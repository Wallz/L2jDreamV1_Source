/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package com.src.gameserver.model.actor.status;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.managers.DuelManager;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2SummonInstance;
import com.src.gameserver.model.actor.stat.CharStat;
import com.src.gameserver.model.entity.Duel;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.skills.Formulas;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.random.Rnd;

public class CharStatus
{
	protected static final Logger _log = Logger.getLogger(CharStatus.class.getName());

	private L2Character _activeChar;
	private double _currentCp = 0;
	private double _currentHp = 0;
	private double _currentMp = 0;

	private Set<L2Character> _StatusListener;

	private Future<?> _regTask;
	private byte _flagsRegenActive = 0;
	private static final byte REGEN_FLAG_CP = 4;
	private static final byte REGEN_FLAG_HP = 1;
	private static final byte REGEN_FLAG_MP = 2;

	public CharStatus(L2Character activeChar)
	{
		_activeChar = activeChar;
	}

	public final void addStatusListener(L2Character object)
	{
		if(object == getActiveChar())
		{
			return;
		}

		synchronized (getStatusListener())
		{
			getStatusListener().add(object);
		}
	}

	public final void reduceCp(int value)
	{
		if(getCurrentCp() > value)
		{
			setCurrentCp(getCurrentCp() - value);
		}
		else
		{
			setCurrentCp(0);
		}
	}

	public void reduceHp(double value, L2Character attacker)
	{
		reduceHp(value, attacker, true);
	}

	public void reduceHp(double value, L2Character attacker, boolean awake)
	{
		if(getActiveChar().isInvul())
		{
			return;
		}

		if(getActiveChar() instanceof L2PcInstance)
		{
			if(((L2PcInstance) getActiveChar()).isInDuel())
			{
				if(((L2PcInstance) getActiveChar()).getDuelState() == Duel.DUELSTATE_DEAD)
				{
					return;
				}
				else if(((L2PcInstance) getActiveChar()).getDuelState() == Duel.DUELSTATE_WINNER)
				{
					return;
				}

				if(!(attacker instanceof L2SummonInstance) && !(attacker instanceof L2PcInstance && ((L2PcInstance) attacker).getDuelId() == ((L2PcInstance) getActiveChar()).getDuelId()))
				{
					((L2PcInstance) getActiveChar()).setDuelState(Duel.DUELSTATE_INTERRUPTED);
				}
			}

			if(getActiveChar().isDead() && !getActiveChar().isFakeDeath())
			{
				return;
			}
		}
		else
		{
			if(getActiveChar().isDead())
			{
				return;
			}

			if(attacker instanceof L2PcInstance && ((L2PcInstance) attacker).isInDuel() && !(getActiveChar() instanceof L2SummonInstance && ((L2SummonInstance) getActiveChar()).getOwner().getDuelId() == ((L2PcInstance) attacker).getDuelId())) // Duelling player attacks mob
			{
				((L2PcInstance) attacker).setDuelState(Duel.DUELSTATE_INTERRUPTED);
			}
		}

		if(awake && getActiveChar().isSleeping())
		{
			getActiveChar().stopSleeping(null);
		}

		if(awake && getActiveChar().isImmobileUntilAttacked())
		{
			getActiveChar().stopImmobileUntilAttacked(null);
		}

		if(getActiveChar().isAfraid())
		{
			getActiveChar().stopFear(null);
		}

		if(getActiveChar().isStunned() && Rnd.get(10) == 0)
		{
			getActiveChar().stopStunning(null);
		}

		if(getActiveChar() instanceof L2Npc)
		{
			getActiveChar().addAttackerToAttackByList(attacker);
		}

		if(value > 0)
		{
			if(getActiveChar() instanceof L2Attackable)
			{
				if(((L2Attackable) getActiveChar()).isOverhit())
				{
					((L2Attackable) getActiveChar()).setOverhitValues(attacker, value);
				}
				else
				{
					((L2Attackable) getActiveChar()).overhitEnabled(false);
				}
			}

			value = getCurrentHp() - value;

			if(value <= 0)
			{
				if(getActiveChar() instanceof L2PcInstance && ((L2PcInstance) getActiveChar()).isInDuel())
				{
					getActiveChar().disableAllSkills();
					stopHpMpRegeneration();
					attacker.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
					attacker.sendPacket(ActionFailed.STATIC_PACKET);

					DuelManager.getInstance().onPlayerDefeat((L2PcInstance) getActiveChar());
					value = 1;
				}
				else
				{
					value = 0;
				}
			}
			setCurrentHp(value);
		}
		else
		{
			if(getActiveChar() instanceof L2Attackable)
			{
				((L2Attackable) getActiveChar()).overhitEnabled(false);
			}
		}

		if(getActiveChar().isDead())
		{
			getActiveChar().abortAttack();
			getActiveChar().abortCast();

			if(getActiveChar() instanceof L2PcInstance)
			{
				if(((L2PcInstance) getActiveChar()).isInOlympiadMode())
				{
					stopHpMpRegeneration();
					return;
				}
			}
			getActiveChar().doDie(attacker);

			setCurrentHp(0);
		}
		else
		{
			if(getActiveChar() instanceof L2Attackable)
			{
				((L2Attackable) getActiveChar()).overhitEnabled(false);
			}
		}
	}

	public final void reduceMp(double value)
	{
		value = getCurrentMp() - value;

		if(value < 0)
		{
			value = 0;
		}

		setCurrentMp(value);
	}

	public final void removeStatusListener(L2Character object)
	{
		synchronized (getStatusListener())
		{
			getStatusListener().remove(object);
		}
	}

	public synchronized final void startHpMpRegeneration()
	{
		if(_regTask == null && !getActiveChar().isDead())
		{
			int period = Formulas.getInstance().getRegeneratePeriod(getActiveChar());

			_regTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new RegenTask(), period, period);
		}
	}

	public synchronized final void stopHpMpRegeneration()
	{
		if(_regTask != null)
		{
			_regTask.cancel(false);
			_regTask = null;

			_flagsRegenActive = 0;
		}
	}

	public L2Character getActiveChar()
	{
		return _activeChar;
	}

	public final double getCurrentCp()
	{
		return _currentCp;
	}

	public final void setCurrentCp(double newCp)
	{
		setCurrentCp(newCp, true);
	}

	public final void setCurrentCp(double newCp, boolean broadcastPacket)
	{
		synchronized (this)
		{
			int maxCp = getActiveChar().getStat().getMaxCp();

			if(newCp < 0)
			{
				newCp = 0;
			}

			if(newCp >= maxCp)
			{
				_currentCp = maxCp;
				_flagsRegenActive &= ~REGEN_FLAG_CP;

				if(_flagsRegenActive == 0)
				{
					stopHpMpRegeneration();
				}
			}
			else
			{
				_currentCp = newCp;
				_flagsRegenActive |= REGEN_FLAG_CP;

				startHpMpRegeneration();
			}
		}

		if(broadcastPacket)
		{
			getActiveChar().broadcastStatusUpdate();
		}
	}

	public final double getCurrentHp()
	{
		return _currentHp;
	}

	public final void setCurrentHp(double newHp)
	{
		setCurrentHp(newHp, true);
	}

	public final void setCurrentHp(double newHp, boolean broadcastPacket)
	{
		synchronized (this)
		{
			double maxHp = getActiveChar().getStat().getMaxHp();
			
			if(newHp >= maxHp)
			{
				_currentHp = maxHp;
				_flagsRegenActive &= ~REGEN_FLAG_HP;
				getActiveChar().setIsKilledAlready(false);
				
				if(_flagsRegenActive == 0)
				{
					stopHpMpRegeneration();
				}
			}
			else
			{
				_currentHp = newHp;
				_flagsRegenActive |= REGEN_FLAG_HP;
				
				if(!getActiveChar().isDead()) 
				{ 
					getActiveChar().setIsKilledAlready(false); 
				}
				startHpMpRegeneration();
			}
		}

		if(broadcastPacket)
		{
			getActiveChar().broadcastStatusUpdate();
		}
	}

	public final void setCurrentHpMp(double newHp, double newMp)
	{
		setCurrentHp(newHp, false);
		setCurrentMp(newMp, true);
	}

	public final double getCurrentMp()
	{
		return _currentMp;
	}

	public final void setCurrentMp(double newMp)
	{
		setCurrentMp(newMp, true);
	}

	public final void setCurrentMp(double newMp, boolean broadcastPacket)
	{
		synchronized (this)
		{
			int maxMp = getActiveChar().getStat().getMaxMp();

			if(newMp >= maxMp)
			{
				_currentMp = maxMp;
				_flagsRegenActive &= ~REGEN_FLAG_MP;

				if(_flagsRegenActive == 0)
				{
					stopHpMpRegeneration();
				}
			}
			else
			{
				_currentMp = newMp;
				_flagsRegenActive |= REGEN_FLAG_MP;

				startHpMpRegeneration();
			}
		}

		if(broadcastPacket)
		{
			getActiveChar().broadcastStatusUpdate();
		}
	}

	public final Set<L2Character> getStatusListener()
	{
		if(_StatusListener == null)
		{
			_StatusListener = new CopyOnWriteArraySet<L2Character>();
		}

		return _StatusListener;
	}

	class RegenTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				final CharStat charstat = getActiveChar().getStat();

				if(getCurrentCp() < charstat.getMaxCp())
				{
					setCurrentCp(getCurrentCp() + Formulas.getInstance().calcCpRegen(getActiveChar()), false);
				}

				if(getCurrentHp() < charstat.getMaxHp())
				{
					setCurrentHp(getCurrentHp() + Formulas.getInstance().calcHpRegen(getActiveChar()), false);
				}

				if(getCurrentMp() < charstat.getMaxMp())
				{
					setCurrentMp(getCurrentMp() + Formulas.getInstance().calcMpRegen(getActiveChar()), false);
				}

				if(!getActiveChar().isInActiveRegion())
				{
					if(getCurrentCp() == charstat.getMaxCp() && getCurrentHp() == charstat.getMaxHp() && getCurrentMp() == charstat.getMaxMp())
					{
						stopHpMpRegeneration();
					}
				}
				else
				{
					getActiveChar().broadcastStatusUpdate();
				}

			}
			catch(Throwable e)
			{
				_log.log(Level.SEVERE, "RegenTask failed for " + getActiveChar().getName(), e);
			}
		}
	}

}