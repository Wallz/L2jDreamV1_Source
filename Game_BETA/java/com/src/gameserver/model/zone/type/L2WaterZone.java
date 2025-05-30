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
package com.src.gameserver.model.zone.type;

import java.util.Collection;

import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.zone.L2ZoneType;
import com.src.gameserver.network.serverpackets.NpcInfo;

public class L2WaterZone extends L2ZoneType
{
	public L2WaterZone(int id)
	{
		super(id);
	}

	@Override
	protected void onEnter(L2Character character)
	{
		character.setInsideZone(L2Character.ZONE_WATER, true);

		if(character instanceof L2PcInstance)
		{
			((L2PcInstance) character).broadcastUserInfo();
		}
		else if(character instanceof L2Npc)
		{
			Collection<L2PcInstance> plrs = character.getKnownList().getKnownPlayers().values();
			{
				for(L2PcInstance player : plrs)
				{
					player.sendPacket(new NpcInfo((L2Npc) character, player));
				}
			}
		}
	}

	@Override
	protected void onExit(L2Character character)
	{
		character.setInsideZone(L2Character.ZONE_WATER, false);

		if(character instanceof L2PcInstance)
		{
			((L2PcInstance) character).broadcastUserInfo();
		}
		else if(character instanceof L2Npc)
		{
			Collection<L2PcInstance> plrs = character.getKnownList().getKnownPlayers().values();
			{
				for(L2PcInstance player : plrs)
				{
					player.sendPacket(new NpcInfo((L2Npc) character, player));
				}
			}
		}
	}

	@Override
	public void onDieInside(L2Character character)
	{
	}

	@Override
	public void onReviveInside(L2Character character)
	{
	}

	public int getWaterZ()
	{
		return getZone().getHighZ();
	}
}