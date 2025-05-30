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

import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2SummonInstance;
import com.src.gameserver.model.entity.Duel;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.Stats;
import com.src.gameserver.util.Util;

public class PcStatus extends PlayableStatus
{
	public PcStatus(L2PcInstance activeChar)
	{
		super(activeChar);
	}

	@Override
	public final void reduceHp(double value, L2Character attacker)
	{
		reduceHp(value, attacker, true);
	}

	@Override
	public final void reduceHp(double value, L2Character attacker, boolean awake)
	{
		if(getActiveChar().isInvul() && getActiveChar() != attacker)
		{
			return;
		}

		if(attacker instanceof L2PcInstance)
		{
			if(getActiveChar().isInDuel())
			{
				if(getActiveChar().getDuelState() == Duel.DUELSTATE_DEAD)
				{
					return;
				}
				else if(getActiveChar().getDuelState() == Duel.DUELSTATE_WINNER)
				{
					return;
				}

				if(((L2PcInstance) attacker).getDuelId() != getActiveChar().getDuelId())
				{
					getActiveChar().setDuelState(Duel.DUELSTATE_INTERRUPTED);
				}
			}

			if(getActiveChar().isDead() && !getActiveChar().isFakeDeath())
			{
				return;
			}

			if(getActiveChar().isInStoreMode())
			{
				getActiveChar().setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
				getActiveChar().broadcastUserInfo();
				getActiveChar().standUp();

				if(getActiveChar() == null || getActiveChar().isOffline())
				{
					getActiveChar().logout();
				}
			}
			else if(getActiveChar().isInCraftMode())
			{
				getActiveChar().isInCraftMode(false);
				getActiveChar().broadcastUserInfo();
				getActiveChar().standUp();

				if(getActiveChar() == null || getActiveChar().isOffline())
				{
					getActiveChar().logout();
				}
			}
		}
		else
		{
			if(getActiveChar().isInDuel() && !(attacker instanceof L2SummonInstance))
			{
				getActiveChar().setDuelState(Duel.DUELSTATE_INTERRUPTED);
			}

			if(getActiveChar().isDead())
			{
				return;
			}
		}

		int fullValue = (int) value;

		if(attacker != null && attacker != getActiveChar())
		{
			L2Summon summon = getActiveChar().getPet();

			if(summon != null && summon instanceof L2SummonInstance && Util.checkIfInRange(900, getActiveChar(), summon, true))
			{
				int tDmg = (int) value * (int) getActiveChar().getStat().calcStat(Stats.TRANSFER_DAMAGE_PERCENT, 0, null, null) / 100;

				if(summon.getCurrentHp() < tDmg)
				{
					tDmg = (int) summon.getCurrentHp() - 1;
				}

				if(tDmg > 0)
				{
					summon.reduceCurrentHp(tDmg, attacker);
					value -= tDmg;
					fullValue = (int) value;
				}
			}

			if(attacker instanceof L2Playable)
			{
				if(getCurrentCp() >= value)
				{
					setCurrentCp(getCurrentCp() - value);
					value = 0;
				}
				else
				{
					value -= getCurrentCp();
					setCurrentCp(0);
				}
			}

			summon = null;
		}

		super.reduceHp(value, attacker, awake);

		if(!getActiveChar().isDead() && getActiveChar().isSitting())
		{
			getActiveChar().standUp();
		}

		if(getActiveChar().isFakeDeath())
		{
			getActiveChar().stopFakeDeath(null);
		}

		if(attacker != null && attacker != getActiveChar() && fullValue > 0)
		{
			if(attacker instanceof L2Npc)
			{
				int mobId = ((L2Npc) attacker).getTemplate().idTemplate;

				getActiveChar().sendPacket(new SystemMessage(SystemMessageId.S1_GAVE_YOU_S2_DMG).addNpcName(mobId).addNumber(fullValue));
			}
			else if(attacker instanceof L2Summon)
			{
				int mobId = ((L2Summon) attacker).getTemplate().idTemplate;

				getActiveChar().sendPacket(new SystemMessage(SystemMessageId.S1_GAVE_YOU_S2_DMG).addNpcName(mobId).addNumber(fullValue));
			}
			else
			{
				getActiveChar().sendPacket(new SystemMessage(SystemMessageId.S1_GAVE_YOU_S2_DMG).addString(attacker.getName()).addNumber(fullValue));
			}
		}
	}

	@Override
	public L2PcInstance getActiveChar()
	{
		return (L2PcInstance) super.getActiveChar();
	}

}