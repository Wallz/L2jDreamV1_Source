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
package com.src.gameserver.model.actor.knownlist;

import com.src.Config;
import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2GuardInstance;
import com.src.gameserver.model.actor.instance.L2MonsterInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class GuardKnownList extends AttackableKnownList
{
	public GuardKnownList(L2GuardInstance activeChar)
	{
		super(activeChar);
	}

	@Override
	public boolean addKnownObject(L2Object object)
	{
		return addKnownObject(object, null);
	}

	@Override
	public boolean addKnownObject(L2Object object, L2Character dropper)
	{
		if(!super.addKnownObject(object, dropper))
		{
			return false;
		}

		if(getActiveChar().getHomeX() == 0)
		{
			getActiveChar().getHomeLocation();
		}

		if(object instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) object;

			if(player.getKarma() > 0)
			{
				if(getActiveChar().getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
				{
					getActiveChar().getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
				}
			}

			player = null;
		}
		else if(Config.GUARD_ATTACK_AGGRO_MOB && object instanceof L2MonsterInstance)
		{
			L2MonsterInstance mob = (L2MonsterInstance) object;

			if(mob.isAggressive())
			{
				if(getActiveChar().getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
				{
					getActiveChar().getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
				}
			}
		}
		return true;
	}

	@Override
	public boolean removeKnownObject(L2Object object)
	{
		if(!super.removeKnownObject(object))
		{
			return false;
		}

		if(getActiveChar().noTarget())
		{
			if (getActiveChar().hasAI()) 
				getActiveChar().getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);
		}

		return true;
	}

	@Override
	public final L2GuardInstance getActiveChar()
	{
		return (L2GuardInstance) super.getActiveChar();
	}
}