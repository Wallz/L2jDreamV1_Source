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

import java.util.Collection;

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.ai.L2CharacterAI;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2NpcInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class AttackableKnownList extends NpcKnownList
{
	public AttackableKnownList(L2Attackable activeChar)
	{
		super(activeChar);
	}

	@Override
	public boolean removeKnownObject(L2Object object)
	{
		if(!super.removeKnownObject(object))
		{
			return false;
		}

		if(object != null && object instanceof L2Character)
		{
			getActiveChar().getAggroList().remove(object);
		}

		Collection<L2PcInstance> known = getKnownPlayers().values();

		L2CharacterAI ai = getActiveChar().getAI();
		if(ai != null && (known == null || known.isEmpty()))
		{
			ai.setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}

		ai = null;

		return true;
	}

	@Override
	public L2Attackable getActiveChar()
	{
		return (L2Attackable) super.getActiveChar();
	}

	@Override
	public int getDistanceToForgetObject(L2Object object)
	{
		if(getActiveChar().getAggroListRP() != null)
		{
			if(getActiveChar().getAggroListRP().get(object) != null)
			{
				return 3000;
			}
		}

		return Math.min(2200, 2 * getDistanceToWatchObject(object));
	}

	@Override
	public int getDistanceToWatchObject(L2Object object)
	{
		if(object instanceof L2NpcInstance || !(object instanceof L2Character))
		{
			return 0;
		}

		if(object instanceof L2Playable)
		{
			return 1500;
		}

		if(getActiveChar().getAggroRange() > getActiveChar().getFactionRange())
		{
			return getActiveChar().getAggroRange();
		}

		if(getActiveChar().getFactionRange() > 200)
		{
			return getActiveChar().getFactionRange();
		}

		return 200;
	}

}