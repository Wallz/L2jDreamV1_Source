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
package com.src.gameserver.model.actor.instance;

import java.util.concurrent.Future;

import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SetSummonRemainTime;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.thread.ThreadPoolManager;

public class L2SummonInstance extends L2Summon
{
	private float _expPenalty = 0;
	private int _itemConsumeId;
	private int _itemConsumeCount;
	private int _itemConsumeSteps;
	private final int _totalLifeTime;
	private final int _timeLostIdle;
	private final int _timeLostActive;
	private int _timeRemaining;
	private int _nextItemConsumeTime;
	public int lastShowntimeRemaining;

	private Future<?> _summonLifeTask;

	public L2SummonInstance(int objectId, L2NpcTemplate template, L2PcInstance owner, L2Skill skill)
	{
		super(objectId, template, owner);
		setShowSummonAnimation(true);

		if(skill != null)
		{
			_itemConsumeId = skill.getItemConsumeIdOT();
			_itemConsumeCount = skill.getItemConsumeOT();
			_itemConsumeSteps = skill.getItemConsumeSteps();
			_totalLifeTime = skill.getTotalLifeTime();
			_timeLostIdle = skill.getTimeLostIdle();
			_timeLostActive = skill.getTimeLostActive();
		}
		else
		{
			_itemConsumeId = 0;
			_itemConsumeCount = 0;
			_itemConsumeSteps = 0;
			_totalLifeTime = 1200000;
			_timeLostIdle = 1000;
			_timeLostActive = 1000;
		}
		_timeRemaining = _totalLifeTime;
		lastShowntimeRemaining = _totalLifeTime;

		if(_itemConsumeId == 0)
		{
			_nextItemConsumeTime = -1;
		}
		else if(_itemConsumeSteps == 0)
		{
			_nextItemConsumeTime = -1;
		}
		else
		{
			_nextItemConsumeTime = _totalLifeTime - _totalLifeTime / (_itemConsumeSteps + 1);
		}

		int delay = 1000;

		_summonLifeTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new SummonLifetime(getOwner(), this), delay, delay);
	}

	@Override
	public final int getLevel()
	{
		return getTemplate() != null ? getTemplate().level : 0;
	}

	@Override
	public int getSummonType()
	{
		return 1;
	}

	public void setExpPenalty(float expPenalty)
	{
		_expPenalty = expPenalty;
	}

	public float getExpPenalty()
	{
		return _expPenalty;
	}

	public int getItemConsumeCount()
	{
		return _itemConsumeCount;
	}

	public int getItemConsumeId()
	{
		return _itemConsumeId;
	}

	public int getItemConsumeSteps()
	{
		return _itemConsumeSteps;
	}

	public int getNextItemConsumeTime()
	{
		return _nextItemConsumeTime;
	}

	public int getTotalLifeTime()
	{
		return _totalLifeTime;
	}

	public int getTimeLostIdle()
	{
		return _timeLostIdle;
	}

	public int getTimeLostActive()
	{
		return _timeLostActive;
	}

	public int getTimeRemaining()
	{
		return _timeRemaining;
	}

	public void setNextItemConsumeTime(int value)
	{
		_nextItemConsumeTime = value;
	}

	public void decNextItemConsumeTime(int value)
	{
		_nextItemConsumeTime -= value;
	}

	public void decTimeRemaining(int value)
	{
		_timeRemaining -= value;
	}

	public void addExpAndSp(int addToExp, int addToSp)
	{
		getOwner().addExpAndSp(addToExp, addToSp);
	}

	public void reduceCurrentHp(int damage, L2Character attacker)
	{
		super.reduceCurrentHp(damage, attacker);

		if(attacker instanceof L2Npc)
		{
			getOwner().sendPacket(new SystemMessage(SystemMessageId.SUMMON_RECEIVED_DAMAGE_S2_BY_S1).addNpcName(((L2Npc) attacker).getTemplate().npcId).addNumber(damage));
		}
		else
		{
			getOwner().sendPacket(new SystemMessage(SystemMessageId.SUMMON_RECEIVED_DAMAGE_S2_BY_S1).addString(attacker.getName()).addNumber(damage));
		}
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if(!super.doDie(killer))
		{
			return false;
		}

		if(_summonLifeTask != null)
		{
			_summonLifeTask.cancel(true);
			_summonLifeTask = null;
		}
		return true;

	}

	static class SummonLifetime implements Runnable
	{
		private L2PcInstance _activeChar;
		private L2SummonInstance _summon;

		SummonLifetime(L2PcInstance activeChar, L2SummonInstance newpet)
		{
			_activeChar = activeChar;
			_summon = newpet;
		}

		@Override
		public void run()
		{
			try
			{
				double oldTimeRemaining = _summon.getTimeRemaining();
				int maxTime = _summon.getTotalLifeTime();
				double newTimeRemaining;

				if(_summon.isAttackingNow())
				{
					_summon.decTimeRemaining(_summon.getTimeLostActive());
				}
				else
				{
					_summon.decTimeRemaining(_summon.getTimeLostIdle());
				}
				newTimeRemaining = _summon.getTimeRemaining();

				if(newTimeRemaining < 0)
				{
					_summon.unSummon(_activeChar);
				}
				else if(newTimeRemaining <= _summon.getNextItemConsumeTime() && oldTimeRemaining > _summon.getNextItemConsumeTime())
				{
					_summon.decNextItemConsumeTime(maxTime / (_summon.getItemConsumeSteps() + 1));

					if(_summon.getItemConsumeCount() > 0 && _summon.getItemConsumeId() != 0 && !_summon.isDead() && !_summon.destroyItemByItemId("Consume", _summon.getItemConsumeId(), _summon.getItemConsumeCount(), _activeChar, true))
					{
						_summon.unSummon(_activeChar);
					}
				}

				if(_summon.lastShowntimeRemaining - newTimeRemaining > maxTime / 352)
				{
					_summon.getOwner().sendPacket(new SetSummonRemainTime(maxTime, (int) newTimeRemaining));
					_summon.lastShowntimeRemaining = (int) newTimeRemaining;
				}
			}
			catch(Throwable e)
			{
			}
		}
	}

	@Override
	public synchronized void unSummon(L2PcInstance owner)
	{
		if(_summonLifeTask != null)
		{
			_summonLifeTask.cancel(true);
			_summonLifeTask = null;
		}

		super.unSummon(owner);
	}

	@Override
	public boolean destroyItem(String process, int objectId, int count, L2Object reference, boolean sendMessage)
	{
		return getOwner().destroyItem(process, objectId, count, reference, sendMessage);
	}

	@Override
	public boolean destroyItemByItemId(String process, int itemId, int count, L2Object reference, boolean sendMessage)
	{
		return getOwner().destroyItemByItemId(process, itemId, count, reference, sendMessage);
	}

	@Override
	public final void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
		if(miss)
		{
			return;
		}

		if(target.getObjectId() != getOwner().getObjectId())
		{
			if(pcrit || mcrit)
				if (this instanceof L2SummonInstance)
					getOwner().sendPacket(new SystemMessage(SystemMessageId.CRITICAL_HIT_BY_SUMMONED_MOB));
				else
					getOwner().sendPacket(new SystemMessage(SystemMessageId.CRITICAL_HIT_BY_PET));


			if(getOwner().isInOlympiadMode() && target instanceof L2PcInstance && ((L2PcInstance) target).isInOlympiadMode() && ((L2PcInstance) target).getOlympiadGameId() == getOwner().getOlympiadGameId())
			{
				getOwner().dmgDealt += damage;
			}

			if (target.isInvul() && !(target instanceof L2NpcInstance)) 
				getOwner().sendPacket(new SystemMessage(SystemMessageId.ATTACK_WAS_BLOCKED));
			else
			{ 
				if (this instanceof L2SummonInstance) 
					getOwner().sendPacket(new SystemMessage(SystemMessageId.SUMMON_GAVE_DAMAGE_S1).addNumber(damage)); 
				else 
					getOwner().sendPacket(new SystemMessage(SystemMessageId.PET_HIT_FOR_S1_DAMAGE).addNumber(damage)); 
				
			}
		}
	}

}