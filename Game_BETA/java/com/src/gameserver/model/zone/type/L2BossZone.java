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

import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import com.src.Config;
import com.src.gameserver.GameServer;
import com.src.gameserver.datatables.xml.MapRegionTable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.zone.L2ZoneType;
import com.src.gameserver.network.serverpackets.L2GameServerPacket;
import com.src.util.object.L2FastList;

public class L2BossZone extends L2ZoneType
{
	private String _zoneName;
	private int _timeInvade;
	private boolean _enabled = true;
	private boolean _IsFlyingEnable = true;

	private FastMap<Integer, Long> _playerAllowedReEntryTimes;

	private L2FastList<Integer> _playersAllowed;

	private int _bossId;

	public L2BossZone(int id, int boss_id)
	{
		super(id);
		_bossId = boss_id;
		_playerAllowedReEntryTimes = new FastMap<Integer, Long>();
		_playersAllowed = new L2FastList<Integer>();
	}

	@Override
	public void setParameter(String name, String value)
	{
		if(name.equals("name"))
		{
			_zoneName = value;
		}
		else if(name.equals("InvadeTime"))
		{
			_timeInvade = Integer.parseInt(value);
		}
		else if(name.equals("EnabledByDefault"))
		{
			_enabled = Boolean.parseBoolean(value);
		}
		else if(name.equals("flying"))
		{
			_IsFlyingEnable = Boolean.parseBoolean(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}

	@Override
	protected void onEnter(L2Character character)
	{
		if(_enabled)
		{
			if(character instanceof L2PcInstance)
			{
				L2PcInstance player = (L2PcInstance) character;
				player.setInsideZone(L2Character.ZONE_NOSUMMONFRIEND, true);
				
				if(((L2PcInstance) character).isAio() && Config.ALLOW_AIO_ENTER_IN_BOSS_ZONE)
				{
					((L2PcInstance) character).teleToLocation(MapRegionTable.TeleportWhereType.Town);
					((L2PcInstance) character).sendMessage("Aio buffers cant enter in " + _zoneName + ". You have been teleported to the nearest town.");
				}
				else
				{
				if(player.isGM())
				{
					player.sendMessage("You entered " + _zoneName + ".");
					return;
				}

				if(!player.isGM() && player.isFlying() && !_IsFlyingEnable)
				{
					player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
					return;
				}

				if(_playersAllowed.contains(character.getObjectId()))
				{
					Long expirationTime = _playerAllowedReEntryTimes.get(character.getObjectId());

					if(expirationTime == null)
					{
						long serverStartTime = GameServer.dateTimeServerStarted.getTimeInMillis();

						if(serverStartTime > System.currentTimeMillis() - _timeInvade)
						{
							return;
						}
					}
					else
					{
						_playerAllowedReEntryTimes.remove(character.getObjectId());

						if(expirationTime.longValue() > System.currentTimeMillis())
						{
							return;
						}
					}
					_playersAllowed.remove(_playersAllowed.indexOf(character.getObjectId()));
				}

					player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
				player = null;
				}
				
			}
		}
	}

	@Override
	protected void onExit(L2Character character)
	{
		if(_enabled)
		{
			if(character instanceof L2PcInstance)
			{
				L2PcInstance player = (L2PcInstance) character;

				if(player.isGM())
				{
					player.sendMessage("You left " + _zoneName + ".");
					return;
				}

				if(player.isOnline() == 0 && _playersAllowed.contains(character.getObjectId()))
				{
					_playerAllowedReEntryTimes.put(character.getObjectId(), System.currentTimeMillis() + _timeInvade);
				}

				player = null;
			}
		}
	}

	public void setZoneEnabled(boolean flag)
	{
		if(_enabled != flag)
		{
			oustAllPlayers();
		}

		_enabled = flag;
	}

	public String getZoneName()
	{
		return _zoneName;
	}

	public int getTimeInvade()
	{
		return _timeInvade;
	}

	public void setAllowedPlayers(L2FastList<Integer> players)
	{
		if(players != null)
		{
			_playersAllowed = players;
		}
	}

	public L2FastList<Integer> getAllowedPlayers()
	{
		return _playersAllowed;
	}

	public boolean isPlayerAllowed(L2PcInstance player)
	{
		if(player.isGM())
		{
			return true;
		}
		else if(_playersAllowed.contains(player.getObjectId()))
		{
			return true;
		}
		else
		{
			player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			return false;
		}
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

				player = null;
			}
		}
		_playerAllowedReEntryTimes.clear();
		_playersAllowed.clear();
	}

	public void allowPlayerEntry(L2PcInstance player, int durationInSec)
	{
		if(!player.isGM())
		{
			_playersAllowed.add(player.getObjectId());
			_playerAllowedReEntryTimes.put(player.getObjectId(), System.currentTimeMillis() + durationInSec * 1000);
		}
	}

	public void removePlayer(L2PcInstance player)
	{
		if(!player.isGM())
		{
			_playersAllowed.remove(Integer.valueOf(player.getObjectId()));
			_playerAllowedReEntryTimes.remove(player.getObjectId());
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

	public L2PcInstance[] getAllPlayersInside()
	{
		FastList<L2PcInstance> list = new FastList<L2PcInstance>();

		for(L2Character cha : _characterList.values())
		{
			if(cha instanceof L2PcInstance)
			{
				list.add((L2PcInstance) cha);
			}
		}

		return list.toArray(new L2PcInstance[list.size()]);
	}

	@Override
	public void broadcastPacket(L2GameServerPacket packet)
	{
		if(_characterList == null || _characterList.isEmpty())
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
					player.sendPacket(packet);
				}
			}
		}
	}

	public void updateKnownList(L2Npc npc)
	{
		if(_characterList == null || _characterList.isEmpty())
		{
			return;
		}

		Map<Integer, L2PcInstance> npcKnownPlayers = npc.getKnownList().getKnownPlayers();
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
					npcKnownPlayers.put(player.getObjectId(), player);
				}
			}
		}
	}

	public void movePlayersTo(int x, int y, int z)
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
					player.teleToLocation(x, y, z);
				}
			}
		}
	}

	public int getBossId()
	{
		return _bossId;
	}

}