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

import com.src.gameserver.datatables.xml.MapRegionTable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.zone.L2ZoneType;

public class L2ArenaZone extends L2ZoneType
{
	private int[] _spawnLoc;

	public L2ArenaZone(int id)
	{
		super(id);

		_spawnLoc = new int[3];
	}

	@Override
	public void setParameter(String name, String value)
	{
		if(name.equals("spawnX"))
		{
			_spawnLoc[0] = Integer.parseInt(value);
		}
		else if(name.equals("spawnY"))
		{
			_spawnLoc[1] = Integer.parseInt(value);
		}
		else if(name.equals("spawnZ"))
		{
			_spawnLoc[2] = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}

	@Override
	protected void onEnter(L2Character character)
	{
		character.setInsideZone(L2Character.ZONE_PVP, true);
		character.setInsideZone(L2Character.ZONE_NOSUMMONFRIEND, true);
		
	}

	@Override
	protected void onExit(L2Character character)
	{
		character.setInsideZone(L2Character.ZONE_PVP, false);
		character.setInsideZone(L2Character.ZONE_NOSUMMONFRIEND, false);
		
	}
	
	@Override
	protected void onDieInside(L2Character character)
	{
	}

	@Override
	protected void onReviveInside(L2Character character)
	{
		
	}

	public void oustAllPlayers()
	{
		if(_characterList == null)
		{
			return;
		}

		if(_characterList.isEmpty())
		{
			return;
		}

		for(L2Character character : _characterList.values())
		{
			if(character == null)
			{
				continue;
			}

			if(character instanceof L2PcInstance)
			{
				L2PcInstance player = (L2PcInstance) character;

				if(player.isOnline() == 1)
				{
					player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
				}
			}
		}
	}

	public final int[] getSpawnLoc()
	{
		return _spawnLoc;
	}
}