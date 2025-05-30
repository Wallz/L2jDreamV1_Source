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

import com.src.gameserver.ai.CtrlEvent;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class PetStatus extends SummonStatus
{
	private int _currentFed = 0;

	public PetStatus(L2PetInstance activeChar)
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
		if(getActiveChar().isDead())
		{
			return;
		}

		super.reduceHp(value, attacker, awake);

		if(attacker != null)
		{
			if(attacker instanceof L2Npc)
			{
				getActiveChar().getOwner().sendPacket(new SystemMessage(SystemMessageId.PET_RECEIVED_S2_DAMAGE_BY_S1).addNpcName(((L2Npc) attacker).getTemplate().idTemplate).addNumber((int) value));
			}
			else
			{
				getActiveChar().getOwner().sendPacket(new SystemMessage(SystemMessageId.PET_RECEIVED_S2_DAMAGE_BY_S1).addString(attacker.getName()).addNumber((int) value));
			}

			getActiveChar().getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, attacker);
		}
	}

	@Override
	public L2PetInstance getActiveChar()
	{
		return (L2PetInstance) super.getActiveChar();
	}

	public int getCurrentFed()
	{
		return _currentFed;
	}

	public void setCurrentFed(int value)
	{
		_currentFed = value;
	}

}