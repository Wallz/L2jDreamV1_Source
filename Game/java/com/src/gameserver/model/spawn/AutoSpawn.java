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
package com.src.gameserver.model.spawn;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.datatables.sql.SpawnTable;
import com.src.gameserver.datatables.xml.MapRegionTable;
import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.idfactory.IdFactory;
import com.src.gameserver.model.Location;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.entity.Announcements;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;
import com.src.util.random.Rnd;

public class AutoSpawn
{
	private static final Log _log = LogFactory.getLog(AutoSpawn.class);

	private static AutoSpawn _instance;

	private static final int DEFAULT_INITIAL_SPAWN = 30000;
	private static final int DEFAULT_RESPAWN = 3600000;
	private static final int DEFAULT_DESPAWN = 3600000;

	protected Map<Integer, AutoSpawnInstance> _registeredSpawns;
	protected Map<Integer, ScheduledFuture<?>> _runningSpawns;

	protected boolean _activeState = true;

	private AutoSpawn()
	{
		_registeredSpawns = new FastMap<Integer, AutoSpawnInstance>();
		_runningSpawns = new FastMap<Integer, ScheduledFuture<?>>();

		restoreSpawnData();
	}

	public static AutoSpawn getInstance()
	{
		if(_instance == null)
		{
			_instance = new AutoSpawn();
		}

		return _instance;
	}

	public final int size()
	{
		return _registeredSpawns.size();
	}

	@SuppressWarnings("unused")
	private void restoreSpawnData()
	{
		int numLoaded = 0;
		Connection con = null;

		try
		{
			PreparedStatement statement = null;
			PreparedStatement statement2 = null;
			ResultSet rs = null;
			ResultSet rs2 = null;

			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("SELECT * FROM random_spawn ORDER BY groupId ASC");
			rs = statement.executeQuery();

			while(rs.next())
			{
				AutoSpawnInstance spawnInst = registerSpawn(rs.getInt("npcId"), rs.getInt("initialDelay"), rs.getInt("respawnDelay"), rs.getInt("despawnDelay"));

				spawnInst.setSpawnCount(rs.getInt("count"));
				spawnInst.setBroadcast(rs.getBoolean("broadcastSpawn"));
				spawnInst.setRandomSpawn(rs.getBoolean("randomSpawn"));
				numLoaded++;

				statement2 = con.prepareStatement("SELECT * FROM random_spawn_loc WHERE groupId = ?");
				statement2.setInt(1, rs.getInt("groupId"));
				rs2 = statement2.executeQuery();

				while(rs2.next())
				{
					spawnInst.addSpawnLocation(rs2.getInt("x"), rs2.getInt("y"), rs2.getInt("z"), rs2.getInt("heading"));
				}

				statement2.close();
				rs2.close();
			}
			ResourceUtil.closeStatement(statement);
			rs.close();
		}
		catch(Exception e)
		{
			_log.error("AutoSpawnHandler: Could not restore spawn data", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	public AutoSpawnInstance registerSpawn(int npcId, int[][] spawnPoints, int initialDelay, int respawnDelay, int despawnDelay)
	{
		if(initialDelay < 0)
		{
			initialDelay = DEFAULT_INITIAL_SPAWN;
		}

		if(respawnDelay < 0)
		{
			respawnDelay = DEFAULT_RESPAWN;
		}

		if(despawnDelay < 0)
		{
			despawnDelay = DEFAULT_DESPAWN;
		}

		AutoSpawnInstance newSpawn = new AutoSpawnInstance(npcId, initialDelay, respawnDelay, despawnDelay);

		if(spawnPoints != null)
		{
			for(int[] spawnPoint : spawnPoints)
			{
				newSpawn.addSpawnLocation(spawnPoint);
			}
		}

		int newId = IdFactory.getInstance().getNextId();
		newSpawn._objectId = newId;
		_registeredSpawns.put(newId, newSpawn);

		setSpawnActive(newSpawn, true);

		return newSpawn;
	}

	public AutoSpawnInstance registerSpawn(int npcId, int initialDelay, int respawnDelay, int despawnDelay)
	{
		return registerSpawn(npcId, null, initialDelay, respawnDelay, despawnDelay);
	}

	public boolean removeSpawn(AutoSpawnInstance spawnInst)
	{
		if(!isSpawnRegistered(spawnInst))
		{
			return false;
		}

		try
		{
			_registeredSpawns.remove(spawnInst);

			ScheduledFuture<?> respawnTask = _runningSpawns.remove(spawnInst._objectId);
			respawnTask.cancel(false);

			respawnTask = null;
		}
		catch(Exception e)
		{
			_log.error("AutoSpawnHandler: Could not auto spawn for NPC ID " + spawnInst._npcId + " (Object ID = " + spawnInst._objectId + ")", e);

			return false;
		}
		return true;
	}

	public void removeSpawn(int objectId)
	{
		removeSpawn(_registeredSpawns.get(objectId));
	}

	public void setSpawnActive(AutoSpawnInstance spawnInst, boolean isActive)
	{
		if(spawnInst == null)
		{
			return;
		}

		int objectId = spawnInst._objectId;

		if(isSpawnRegistered(objectId))
		{
			ScheduledFuture<?> spawnTask = null;

			if(isActive)
			{
				AutoSpawner rs = new AutoSpawner(objectId);

				if(spawnInst._desDelay > 0)
				{
					spawnTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(rs, spawnInst._initDelay, spawnInst._resDelay);
				}
				else
				{
					spawnTask = ThreadPoolManager.getInstance().scheduleEffect(rs, spawnInst._initDelay);
				}

				_runningSpawns.put(objectId, spawnTask);

				rs = null;
			}
			else
			{
				AutoDespawner rd = new AutoDespawner(objectId);
				spawnTask = _runningSpawns.remove(objectId);

				if(spawnTask != null)
				{
					spawnTask.cancel(false);
				}

				ThreadPoolManager.getInstance().scheduleEffect(rd, 0);
			}
			spawnInst.setSpawnActive(isActive);
		}
	}

	public void setAllActive(boolean isActive)
	{
		if(_activeState == isActive)
		{
			return;
		}

		for(AutoSpawnInstance spawnInst : _registeredSpawns.values())
		{
			setSpawnActive(spawnInst, isActive);
		}

		_activeState = isActive;
	}

	public final long getTimeToNextSpawn(AutoSpawnInstance spawnInst)
	{
		if(spawnInst == null)
			return -1;
		
		int objectId = spawnInst.getObjectId();

		ScheduledFuture<?> future_task = _runningSpawns.get(objectId);
		if(future_task!=null)
			return future_task.getDelay(TimeUnit.MILLISECONDS);
		else
		{
			return -1;
		}

	}

	public final AutoSpawnInstance getAutoSpawnInstance(int id, boolean isObjectId)
	{
		if(isObjectId)
		{
			if(isSpawnRegistered(id))
			{
				return _registeredSpawns.get(id);
			}
		}
		else
		{
			for(AutoSpawnInstance spawnInst : _registeredSpawns.values())
			{
				if(spawnInst.getNpcId() == id)
				{
					return spawnInst;
				}
			}
		}
		return null;
	}

	public Map<Integer, AutoSpawnInstance> getAutoSpawnInstances(int npcId)
	{
		Map<Integer, AutoSpawnInstance> spawnInstList = new FastMap<Integer, AutoSpawnInstance>();

		for(AutoSpawnInstance spawnInst : _registeredSpawns.values())
		{
			if(spawnInst.getNpcId() == npcId)
			{
				spawnInstList.put(spawnInst.getObjectId(), spawnInst);
			}
		}
		return spawnInstList;
	}

	public final boolean isSpawnRegistered(int objectId)
	{
		return _registeredSpawns.containsKey(objectId);
	}

	public final boolean isSpawnRegistered(AutoSpawnInstance spawnInst)
	{
		return _registeredSpawns.containsValue(spawnInst);
	}

	private class AutoSpawner implements Runnable
	{
		private int _objectId;

		protected AutoSpawner(int objectId)
		{
			_objectId = objectId;
		}

		@Override
		public void run()
		{
			try
			{
				AutoSpawnInstance spawnInst = _registeredSpawns.get(_objectId);

				if(!spawnInst.isSpawnActive())
				{
					return;
				}

				Location[] locationList = spawnInst.getLocationList();

				if(locationList.length == 0)
				{
					_log.info("AutoSpawnHandler: No location co-ords specified for spawn instance (Object ID = " + _objectId + ").");
					return;
				}

				int locationCount = locationList.length;
				int locationIndex = Rnd.nextInt(locationCount);

				if(!spawnInst.isRandomSpawn())
				{
					locationIndex = spawnInst._lastLocIndex;
					locationIndex++;

					if(locationIndex == locationCount)
					{
						locationIndex = 0;
					}

					spawnInst._lastLocIndex = locationIndex;
				}

				final int x = locationList[locationIndex].getX();
				final int y = locationList[locationIndex].getY();
				final int z = locationList[locationIndex].getZ();
				final int heading = locationList[locationIndex].getHeading();

				L2NpcTemplate npcTemp = NpcTable.getInstance().getTemplate(spawnInst.getNpcId());

				if(npcTemp == null)
				{
					_log.warn("Couldnt find NPC id" + spawnInst.getNpcId() + " Try to update your DP");
					return;
				}

				L2Spawn newSpawn = new L2Spawn(npcTemp);

				newSpawn.setLocx(x);
				newSpawn.setLocy(y);
				newSpawn.setLocz(z);

				if(heading != -1)
				{
					newSpawn.setHeading(heading);
				}

				newSpawn.setAmount(spawnInst.getSpawnCount());

				if(spawnInst._desDelay == 0)
				{
					newSpawn.setRespawnDelay(spawnInst._resDelay);
				}

				SpawnTable.getInstance().addNewSpawn(newSpawn, false);
				L2Npc npcInst = null;

				if(spawnInst._spawnCount == 1)
				{
					npcInst = newSpawn.doSpawn();
					npcInst.setXYZ(npcInst.getX(), npcInst.getY(), npcInst.getZ());
					spawnInst.addNpcInstance(npcInst);
				}
				else
				{
					for(int i = 0; i < spawnInst._spawnCount; i++)
					{
						npcInst = newSpawn.doSpawn();

						npcInst.setXYZ(npcInst.getX() + Rnd.nextInt(50), npcInst.getY() + Rnd.nextInt(50), npcInst.getZ());

						spawnInst.addNpcInstance(npcInst);
					}
				}

				String nearestTown = MapRegionTable.getInstance().getClosestTownName(npcInst);

				if(spawnInst.isBroadcasting())
				{
					Announcements.getInstance().announceToAll("The " + npcInst.getName() + " has spawned near " + nearestTown + "!");
				}

				if(spawnInst.getDespawnDelay() > 0)
				{
					AutoDespawner rd = new AutoDespawner(_objectId);
					ThreadPoolManager.getInstance().scheduleAi(rd, spawnInst.getDespawnDelay() - 1000);
					rd = null;
				}
			}
			catch(Exception e)
			{
				_log.error("AutoSpawnHandler: An error occurred while initializing spawn instance (Object ID = " + _objectId + ")", e);
			}
		}
	}

	private class AutoDespawner implements Runnable
	{
		private int _objectId;

		protected AutoDespawner(int objectId)
		{
			_objectId = objectId;
		}

		@Override
		public void run()
		{
			try
			{
				AutoSpawnInstance spawnInst = _registeredSpawns.get(_objectId);

				if(spawnInst == null)
				{
					_log.info("AutoSpawnHandler: No spawn registered for object ID = " + _objectId + ".");
					return;
				}

				for(L2Npc npcInst : spawnInst.getNPCInstanceList())
				{
					if(npcInst == null)
					{
						continue;
					}

					npcInst.deleteMe();
					spawnInst.removeNpcInstance(npcInst);
				}
			}
			catch(Exception e)
			{
				_log.error("AutoSpawnHandler: An error occurred while despawning spawn (Object ID = " + _objectId + ")", e);
			}
		}
	}

	public class AutoSpawnInstance
	{
		protected int _objectId;

		protected int _spawnIndex;

		protected int _npcId;

		protected int _initDelay;

		protected int _resDelay;

		protected int _desDelay;

		protected int _spawnCount = 1;

		protected int _lastLocIndex = -1;

		private List<L2Npc> _npcList = new FastList<L2Npc>();

		private List<Location> _locList = new FastList<Location>();

		private boolean _spawnActive;

		private boolean _randomSpawn = false;

		private boolean _broadcastAnnouncement = false;

		protected AutoSpawnInstance(int npcId, int initDelay, int respawnDelay, int despawnDelay)
		{
			_npcId = npcId;
			_initDelay = initDelay;
			_resDelay = respawnDelay;
			_desDelay = despawnDelay;
		}

		protected void setSpawnActive(boolean activeValue)
		{
			_spawnActive = activeValue;
		}

		protected boolean addNpcInstance(L2Npc npcInst)
		{
			return _npcList.add(npcInst);
		}

		protected boolean removeNpcInstance(L2Npc npcInst)
		{
			return _npcList.remove(npcInst);
		}

		public int getObjectId()
		{
			return _objectId;
		}

		public int getInitialDelay()
		{
			return _initDelay;
		}

		public int getRespawnDelay()
		{
			return _resDelay;
		}

		public int getDespawnDelay()
		{
			return _desDelay;
		}

		public int getNpcId()
		{
			return _npcId;
		}

		public int getSpawnCount()
		{
			return _spawnCount;
		}

		public Location[] getLocationList()
		{
			return _locList.toArray(new Location[_locList.size()]);
		}

		public L2Npc[] getNPCInstanceList()
		{
			L2Npc[] ret;

			synchronized (_npcList)
			{
				ret = new L2Npc[_npcList.size()];
				_npcList.toArray(ret);
			}

			return ret;
		}

		public L2Spawn[] getSpawns()
		{
			List<L2Spawn> npcSpawns = new FastList<L2Spawn>();

			for(L2Npc npcInst : _npcList)
			{
				npcSpawns.add(npcInst.getSpawn());
			}

			return npcSpawns.toArray(new L2Spawn[npcSpawns.size()]);
		}

		public void setSpawnCount(int spawnCount)
		{
			_spawnCount = spawnCount;
		}

		public void setRandomSpawn(boolean randValue)
		{
			_randomSpawn = randValue;
		}

		public void setBroadcast(boolean broadcastValue)
		{
			_broadcastAnnouncement = broadcastValue;
		}

		public boolean isSpawnActive()
		{
			return _spawnActive;
		}

		public boolean isRandomSpawn()
		{
			return _randomSpawn;
		}

		public boolean isBroadcasting()
		{
			return _broadcastAnnouncement;
		}

		public boolean addSpawnLocation(int x, int y, int z, int heading)
		{
			return _locList.add(new Location(x, y, z, heading));
		}

		public boolean addSpawnLocation(int[] spawnLoc)
		{
			if(spawnLoc.length != 3)
			{
				return false;
			}

			return addSpawnLocation(spawnLoc[0], spawnLoc[1], spawnLoc[2], -1);
		}

		public Location removeSpawnLocation(int locIndex)
		{
			try
			{
				return _locList.remove(locIndex);
			}
			catch(IndexOutOfBoundsException e)
			{
				return null;
			}
		}
	}
}