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

import java.util.Iterator;

import javolution.util.FastList;

import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.siege.Castle;
import com.src.gameserver.model.zone.L2ZoneType;
import com.src.util.random.Rnd;

public class L2CastleTeleportZone extends L2ZoneType
{

	private int _spawnLoc[];
	private int _castleId;
	private Castle _castle;

	public L2CastleTeleportZone(int id)
	{
		super(id);
		_spawnLoc = new int[5];
	}

	@Override
	public void setParameter(String name, String value)
	{
		if(name.equals("castleId"))
		{
			_castleId = Integer.parseInt(value);
			_castle = CastleManager.getInstance().getCastleById(_castleId);
			_castle.setTeleZone(this);
		}
		else if(name.equals("spawnMinX"))
		{
			_spawnLoc[0] = Integer.parseInt(value);
		}
		else if(name.equals("spawnMaxX"))
		{
			_spawnLoc[1] = Integer.parseInt(value);
		}
		else if(name.equals("spawnMinY"))
		{
			_spawnLoc[2] = Integer.parseInt(value);
		}
		else if(name.equals("spawnMaxY"))
		{
			_spawnLoc[3] = Integer.parseInt(value);
		}
		else if(name.equals("spawnZ"))
		{
			_spawnLoc[4] = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}

	@Override
	protected void onEnter(L2Character character)
	{
		character.setInsideZone(4096, true);
		character.setInsideZone(L2Character.ZONE_NOLANDING, true);
		character.setInsideZone(L2Character.ZONE_NOSUMMONFRIEND, true);
	}

	@Override
	protected void onExit(L2Character character)
	{
		character.setInsideZone(4096, false);
		character.setInsideZone(L2Character.ZONE_NOLANDING, false);
		character.setInsideZone(L2Character.ZONE_NOSUMMONFRIEND, false);
	}

	@Override
	public void onDieInside(L2Character l2character)
	{
	}

	@Override
	public void onReviveInside(L2Character l2character)
	{
	}

	public FastList<L2Character> getAllPlayers()
	{
		FastList<L2Character> players = new FastList<L2Character>();
		Iterator<L2Character> i$ = _characterList.values().iterator();

		while(i$.hasNext())
		{
			L2Character temp = i$.next();

			if(temp instanceof L2PcInstance)
			{
				players.add(temp);
			}

			temp = null;
		}

		i$ = null;

		return players;
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

		Iterator<L2Character> i$ = _characterList.values().iterator();
		while(i$.hasNext())
		{
			L2Character character = i$.next();

			if(character != null && character instanceof L2PcInstance)
			{
				L2PcInstance player = (L2PcInstance) character;

				if(player.isOnline() == 1)
				{
					player.teleToLocation(Rnd.get(_spawnLoc[0], _spawnLoc[1]), Rnd.get(_spawnLoc[2], _spawnLoc[3]), _spawnLoc[4]);
				}

				player = null;
			}

			character = null;
		}

		i$ = null;
	}

	public int[] getSpawn()
	{
		return _spawnLoc;
	}

}