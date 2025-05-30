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

import javolution.util.FastMap;

import com.src.gameserver.datatables.xml.MapRegionTable;
import com.src.gameserver.managers.ClanHallManager;
import com.src.gameserver.model.Location;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.ClanHall;
import com.src.gameserver.model.zone.L2ZoneType;
import com.src.gameserver.network.serverpackets.ClanHallDecoration;

public class L2ClanHallZone extends L2ZoneType
{
	private int _clanHallId;
	private int[] _spawnLoc;

	public L2ClanHallZone(int id)
	{
		super(id);

		_spawnLoc = new int[3];
	}

	@Override
	public void setParameter(String name, String value)
	{
		if(name.equals("clanHallId"))
		{
			_clanHallId = Integer.parseInt(value);
			ClanHallManager.getInstance().getClanHallById(_clanHallId).setZone(this);
		}
		else if(name.equals("spawnX"))
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
		if(character instanceof L2PcInstance)
		{
			character.setInsideZone(L2Character.ZONE_CLANHALL, true);

			ClanHall clanHall = ClanHallManager.getInstance().getClanHallById(_clanHallId);

			if(clanHall == null)
			{
				return;
			}

			ClanHallDecoration deco = new ClanHallDecoration(clanHall);
			((L2PcInstance) character).sendPacket(deco);

			clanHall = null;
		}
	}

	@Override
	protected void onExit(L2Character character)
	{
		if(character instanceof L2PcInstance)
		{
			character.setInsideZone(L2Character.ZONE_CLANHALL, false);
		}
	}

	@Override
	protected void onDieInside(L2Character character)
	{
	}

	@Override
	protected void onReviveInside(L2Character character)
	{
	}

	public void banishForeigners(int owningClanId)
	{
		for(L2Character temp : _characterList.values())
		{
			if(!(temp instanceof L2PcInstance))
			{
				continue;
			}

			if(((L2PcInstance) temp).getClanId() == owningClanId)
			{
				continue;
			}

			((L2PcInstance) temp).teleToLocation(MapRegionTable.TeleportWhereType.Town);
		}
	}

	@Override
	public FastMap<Integer, L2Character> getCharactersInside()
	{
		return _characterList;
	}

	public Location getSpawn()
	{
		return new Location(_spawnLoc[0], _spawnLoc[1], _spawnLoc[2]);
	}

}