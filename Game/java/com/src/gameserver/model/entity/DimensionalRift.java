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
package com.src.gameserver.model.entity;

import java.util.Timer;
import java.util.TimerTask;

import javolution.util.FastList;

import com.src.Config;
import com.src.gameserver.managers.DimensionalRiftManager;
import com.src.gameserver.managers.DimensionalRiftManager.DimensionalRiftRoom;
import com.src.gameserver.model.L2Party;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.util.random.Rnd;

public class DimensionalRift
{
	protected byte _type;
	protected L2Party _party;
	protected FastList<Byte> _completedRooms = new FastList<Byte>();
	private static final long seconds_5 = 5000L;
	protected byte jumps_current = 0;

	private Timer teleporterTimer;
	private TimerTask teleporterTimerTask;
	private Timer spawnTimer;
	private TimerTask spawnTimerTask;

	protected byte _choosenRoom = -1;
	private boolean _hasJumped = false;
	protected FastList<L2PcInstance> deadPlayers = new FastList<L2PcInstance>();
	protected FastList<L2PcInstance> revivedInWaitingRoom = new FastList<L2PcInstance>();
	private boolean isBossRoom = false;

	public DimensionalRift(L2Party party, byte type, byte room)
	{
		_type = type;
		_party = party;
		_choosenRoom = room;
		int[] coords = getRoomCoord(room);
		party.setDimensionalRift(this);

		for(L2PcInstance p : party.getPartyMembers())
		{
			p.teleToLocation(coords[0], coords[1], coords[2]);
		}

		createSpawnTimer(_choosenRoom);
		createTeleporterTimer(true);
	}

	public byte getType()
	{
		return _type;
	}

	public byte getCurrentRoom()
	{
		return _choosenRoom;
	}

	protected void createTeleporterTimer(final boolean reasonTP)
	{
		if(teleporterTimerTask != null)
		{
			teleporterTimerTask.cancel();
			teleporterTimerTask = null;
		}

		if(teleporterTimer != null)
		{
			teleporterTimer.cancel();
			teleporterTimer = null;
		}

		teleporterTimer = new Timer();
		teleporterTimerTask = new TimerTask()
		{
			@Override
			public void run()
			{
				if(_choosenRoom > -1)
				{
					DimensionalRiftManager.getInstance().getRoom(_type, _choosenRoom).unspawn();
				}

				if(reasonTP && jumps_current < getMaxJumps() && _party.getMemberCount() > deadPlayers.size())
				{
					jumps_current++;

					_completedRooms.add(_choosenRoom);
					_choosenRoom = -1;

					for(L2PcInstance p : _party.getPartyMembers())
					{
						if(!revivedInWaitingRoom.contains(p))
						{
							teleportToNextRoom(p);
						}
					}

					createTeleporterTimer(true);
					createSpawnTimer(_choosenRoom);
				}
				else
				{
					for(L2PcInstance p : _party.getPartyMembers())
					{
						if(!revivedInWaitingRoom.contains(p))
						{
							teleportToWaitingRoom(p);
						}
					}

					killRift();
					cancel();
				}
			}
		};

		if(reasonTP)
		{
			teleporterTimer.schedule(teleporterTimerTask, calcTimeToNextJump());
		}
		else
		{
			teleporterTimer.schedule(teleporterTimerTask, seconds_5);
		}
	}

	public void createSpawnTimer(final byte room)
	{
		if(spawnTimerTask != null)
		{
			spawnTimerTask.cancel();
			spawnTimerTask = null;
		}

		if(spawnTimer != null)
		{
			spawnTimer.cancel();
			spawnTimer = null;
		}

		final DimensionalRiftRoom riftRoom = DimensionalRiftManager.getInstance().getRoom(_type, room);
		riftRoom.setUsed();

		spawnTimer = new Timer();
		spawnTimerTask = new TimerTask()
		{
			@Override
			public void run()
			{
				riftRoom.spawn();
			}
		};

		spawnTimer.schedule(spawnTimerTask, Config.RIFT_SPAWN_DELAY);
	}

	public void partyMemberInvited()
	{
		createTeleporterTimer(false);
	}

	public void partyMemberExited(L2PcInstance player)
	{
		if(deadPlayers.contains(player))
		{
			deadPlayers.remove(player);
		}

		if(revivedInWaitingRoom.contains(player))
		{
			revivedInWaitingRoom.remove(player);
		}

		if(_party.getMemberCount() < Config.RIFT_MIN_PARTY_SIZE || _party.getMemberCount() == 1)
		{
			for(L2PcInstance p : _party.getPartyMembers())
			{
				teleportToWaitingRoom(p);
			}

			killRift();
		}
	}

	public void manualTeleport(L2PcInstance player, L2Npc npc)
	{
		if(!player.isInParty() || !player.getParty().isInDimensionalRift())
		{
			return;
		}

		if(player.getObjectId() != player.getParty().getPartyLeaderOID())
		{
			DimensionalRiftManager.getInstance().showHtmlFile(player, "data/html/sevensigns/rift/NotPartyLeader.htm", npc);
			return;
		}

		if(_hasJumped)
		{
			DimensionalRiftManager.getInstance().showHtmlFile(player, "data/html/sevensigns/rift/AlreadyTeleported.htm", npc);
			return;
		}
		else
		{
			_hasJumped = true;
		}

		DimensionalRiftManager.getInstance().getRoom(_type, _choosenRoom).unspawn();
		_completedRooms.add(_choosenRoom);
		_choosenRoom = -1;

		for(L2PcInstance p : _party.getPartyMembers())
		{
			teleportToNextRoom(p);
		}

		createSpawnTimer(_choosenRoom);
		createTeleporterTimer(true);
	}

	public void manualExitRift(L2PcInstance player, L2Npc npc)
	{
		if(!player.isInParty() || !player.getParty().isInDimensionalRift())
		{
			return;
		}

		if(player.getObjectId() != player.getParty().getPartyLeaderOID())
		{
			DimensionalRiftManager.getInstance().showHtmlFile(player, "data/html/sevensigns/rift/NotPartyLeader.htm", npc);
			return;
		}

		for(L2PcInstance p : player.getParty().getPartyMembers())
		{
			teleportToWaitingRoom(p);
		}

		killRift();
	}

	protected void teleportToNextRoom(L2PcInstance player)
	{
		if(_choosenRoom == -1)
		{
			do
			{
				_choosenRoom = (byte) Rnd.get(1, 9);
			}
			while(_completedRooms.contains(_choosenRoom) && !DimensionalRiftManager.getInstance().isRoomAvailable(_type, _choosenRoom));
		}

		checkBossRoom(_choosenRoom);

		int[] coords = getRoomCoord(_choosenRoom);

		player.teleToLocation(coords[0], coords[1], coords[2]);
	}

	protected void teleportToWaitingRoom(L2PcInstance player)
	{
		DimensionalRiftManager.getInstance().teleportToWaitingRoom(player);
	}

	public void killRift()
	{
		_completedRooms = null;

		if(_party != null)
		{
			_party.setDimensionalRift(null);
		}

		_party = null;
		revivedInWaitingRoom = null;
		deadPlayers = null;
		DimensionalRiftManager.getInstance().getRoom(_type, _choosenRoom).unspawn();
		DimensionalRiftManager.getInstance().killRift(this);
	}

	public Timer getTeleportTimer()
	{
		return teleporterTimer;
	}

	public TimerTask getTeleportTimerTask()
	{
		return teleporterTimerTask;
	}

	public Timer getSpawnTimer()
	{
		return spawnTimer;
	}

	public TimerTask getSpawnTimerTask()
	{
		return spawnTimerTask;
	}

	public void setTeleportTimer(Timer t)
	{
		teleporterTimer = t;
	}

	public void setTeleportTimerTask(TimerTask tt)
	{
		teleporterTimerTask = tt;
	}

	public void setSpawnTimer(Timer t)
	{
		spawnTimer = t;
	}

	public void setSpawnTimerTask(TimerTask st)
	{
		spawnTimerTask = st;
	}

	private long calcTimeToNextJump()
	{
		int time = Rnd.get(Config.RIFT_AUTO_JUMPS_TIME_MIN, Config.RIFT_AUTO_JUMPS_TIME_MAX) * 1000;

		if(isBossRoom)
		{
			return (long) (time * Config.RIFT_BOSS_ROOM_TIME_MUTIPLY);
		}
		else
		{
			return time;
		}
	}

	public void memberDead(L2PcInstance player)
	{
		if(!deadPlayers.contains(player))
		{
			deadPlayers.add(player);
		}
	}

	public void memberRessurected(L2PcInstance player)
	{
		if(deadPlayers.contains(player))
		{
			deadPlayers.remove(player);
		}
	}

	public void usedTeleport(L2PcInstance player)
	{
		if(!revivedInWaitingRoom.contains(player))
		{
			revivedInWaitingRoom.add(player);
		}

		if(!deadPlayers.contains(player))
		{
			deadPlayers.add(player);
		}

		if(_party.getMemberCount() - revivedInWaitingRoom.size() < Config.RIFT_MIN_PARTY_SIZE)
		{
			for(L2PcInstance p : _party.getPartyMembers())
			{
				if(!revivedInWaitingRoom.contains(p))
				{
					teleportToWaitingRoom(p);
				}
			}

			killRift();
		}
	}

	public FastList<L2PcInstance> getDeadMemberList()
	{
		return deadPlayers;
	}

	public FastList<L2PcInstance> getRevivedAtWaitingRoom()
	{
		return revivedInWaitingRoom;
	}

	public void checkBossRoom(byte room)
	{
		isBossRoom = DimensionalRiftManager.getInstance().getRoom(_type, room).isBossRoom();
	}

	public int[] getRoomCoord(byte room)
	{
		return DimensionalRiftManager.getInstance().getRoom(_type, room).getTeleportCoords();
	}

	public byte getMaxJumps()
	{
		if(Config.RIFT_MAX_JUMPS <= 8 && Config.RIFT_MAX_JUMPS >= 1)
		{
			return (byte) Config.RIFT_MAX_JUMPS;
		}
		else
		{
			return 4;
		}
	}

}