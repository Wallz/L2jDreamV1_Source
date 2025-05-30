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

import java.util.Map;

import com.src.gameserver.ai.L2CharacterAI;
import com.src.gameserver.ai.L2NpcWalkerAI;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.network.serverpackets.CreatureSay;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public class L2NpcWalkerInstance extends L2Npc
{
	public L2NpcWalkerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		setAI(new L2NpcWalkerAI(new L2NpcWalkerAIAccessor()));
	}

	@Override
	public void setAI(L2CharacterAI newAI)
	{
		if(_ai == null)
		{
			super.setAI(newAI);
		}
	}

	@Override
	public void onSpawn()
	{
		((L2NpcWalkerAI) getAI()).setHomeX(getX());
		((L2NpcWalkerAI) getAI()).setHomeY(getY());
		((L2NpcWalkerAI) getAI()).setHomeZ(getZ());
	}

	public void broadcastChat(String chat)
	{
		Map<Integer, L2PcInstance> _knownPlayers = getKnownList().getKnownPlayers();

		if(_knownPlayers == null)
		{
			return;
		}

		if(_knownPlayers.size() > 0)
		{
			CreatureSay cs = new CreatureSay(getObjectId(), 0, getName(), chat);

			for(L2PcInstance players : _knownPlayers.values())
			{
				players.sendPacket(cs);
			}

			cs = null;
		}
	}

	@Override
	public void reduceCurrentHp(double i, L2Character attacker, boolean awake)
	{
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		return false;
	}

	@Override
	public L2CharacterAI getAI()
	{
		return super.getAI();
	}

	protected class L2NpcWalkerAIAccessor extends L2Character.AIAccessor
	{
		@Override
		public void detachAI()
		{
		}
	}

}