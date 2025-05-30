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
package com.src.gameserver.model;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.datatables.GmListTable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.network.L2GameClient;
import com.src.util.object.L2ObjectMap;
import com.src.util.object.Point3D;

public final class L2World
{
	private static final Log _log = LogFactory.getLog(L2World.class.getName());

	public static final int SHIFT_BY = 12;

	public static final int MAP_MIN_X = Config.WORLD_SIZE_MIN_X;
	public static final int MAP_MAX_X = Config.WORLD_SIZE_MAX_X;
	public static final int MAP_MIN_Y = Config.WORLD_SIZE_MIN_Y;
	public static final int MAP_MAX_Y = Config.WORLD_SIZE_MAX_Y;

	public static final int OFFSET_X = Math.abs(MAP_MIN_X >> SHIFT_BY);
	public static final int OFFSET_Y = Math.abs(MAP_MIN_Y >> SHIFT_BY);

	private static final int REGIONS_X = (MAP_MAX_X >> SHIFT_BY) + OFFSET_X;
	private static final int REGIONS_Y = (MAP_MAX_Y >> SHIFT_BY) + OFFSET_Y;

	private Map<String, L2PcInstance> _allPlayers;

	private static L2ObjectMap<L2Object> _allObjects;

	private FastMap<Integer, L2PetInstance> _petsInstance;

	private static final L2World _instance = new L2World();

	private Map<Integer, L2Character> _allCharacters;
	
	private L2WorldRegion[][] _worldRegions;

	private L2World()
	{
		_allPlayers = new FastMap<String, L2PcInstance>().shared();
		_allCharacters = new ConcurrentHashMap<Integer, L2Character>();
		_petsInstance = new FastMap<Integer, L2PetInstance>().shared();
		_allObjects = L2ObjectMap.createL2ObjectMap();

		initRegions();
	}

	public static L2World getInstance()
	{
		return _instance;
	}

	public static void storeObject(L2Object object)
	{
		if(_allObjects.get(object.getObjectId()) != null)
		{
			return;
		}

		_allObjects.put(object);
	}

	public long timeStoreObject(L2Object object)
	{
		long time = System.currentTimeMillis();
		_allObjects.put(object);
		time -= System.currentTimeMillis();

		return time;
	}

	public void removeObject(L2Object object)
	{
		_allObjects.remove(object);
	}

	public void removeObjects(List<L2Object> list)
	{
		for(L2Object o : list)
		{
			_allObjects.remove(o);
		}
	}

	public void removeObjects(L2Object[] objects)
	{
		for(L2Object o : objects)
		{
			_allObjects.remove(o);
		}
	}

	public long timeRemoveObject(L2Object object)
	{
		long time = System.currentTimeMillis();
		_allObjects.remove(object);
		time -= System.currentTimeMillis();

		return time;
	}

	public L2Object findObject(int oID)
	{
		return _allObjects.get(oID);
	}

	public L2PcInstance findPlayer(int objectId)
	{
		L2Object obj = _allObjects.get(objectId);

		if (obj instanceof L2PcInstance)
			return (L2PcInstance)obj;

		return null;
	}
	
	public long timeFindObject(int objectID)
	{
		long time = System.currentTimeMillis();
		_allObjects.get(objectID);
		time -= System.currentTimeMillis();

		return time;
	}

	@Deprecated
	public final L2ObjectMap<L2Object> getAllVisibleObjects()
	{
		return _allObjects;
	}

	public final int getAllVisibleObjectsCount()
	{
		return _allObjects.size();
	}

	/**
	 * Return the L2Character object that belongs to an ID or null if no object found.<BR><BR>
	 *
	 * @param oID Identifier of the L2Character
	 */
	public L2Character findCharacter(int oID)
	{
		return _allCharacters.get(oID);
	}
	
	public FastList<L2PcInstance> getAllGMs()
	{
		return GmListTable.getInstance().getAllGms(true);
	}

	public Collection<L2PcInstance> getAllPlayers()
	{
		return _allPlayers.values();
	}

	public L2Character[] getAllCharacters()
	{
		return _allCharacters.values().toArray(new L2Character[0]);
	}
	
	public int getAllPlayersCount()
	{
		return _allPlayers.size();
	}

	public L2PcInstance getPlayer(String name)
	{
		return _allPlayers.get(name.toLowerCase());
	}

	public L2PcInstance getPlayer(int playerObjId)
	{
		for(L2PcInstance actual:_allPlayers.values())
		{
			if(actual.getObjectId() == playerObjId)
			{
				return actual;
			}
		}
		return null;
	}

	public Collection<L2PetInstance> getAllPets()
	{
		return _petsInstance.values();
	}

	public L2PetInstance getPet(int ownerId)
	{
		return _petsInstance.get(new Integer(ownerId));
	}

	public L2PetInstance addPet(int ownerId, L2PetInstance pet)
	{
		return _petsInstance.put(new Integer(ownerId), pet);
	}

	public void removePet(int ownerId)
	{
		_petsInstance.remove(new Integer(ownerId));
	}

	public void removePet(L2PetInstance pet)
	{
		_petsInstance.values().remove(pet);
	}

	public void addVisibleObject(L2Object object, L2WorldRegion newRegion, L2Character dropper)
	{
		if(object instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) object;
			L2PcInstance tmp = _allPlayers.get(player.getName().toLowerCase());

			if(tmp != null && tmp != player)
			{
				if((Config.OFFLINE_TRADE_ENABLE || Config.OFFLINE_CRAFT_ENABLE) && tmp.isOffline())
				{
					_log.warn("Offline: Duplicate character!? Closing offline character (" + tmp.getName() + ")");
					if(tmp._originalNameColorOffline!=0)
					{
						tmp.getAppearance().setNameColor(tmp._originalNameColorOffline);
					}
					tmp.store();
					tmp.logout();

					if(tmp.getClient() != null)
					{
						tmp.getClient().setActiveChar(null);
					}
				}
				else
				{
					_log.warn("EnterWorld: Duplicate character!? Closing both characters (" + player.getName() + ")");
					L2GameClient client = player.getClient();
					player.store();
					player.deleteMe();
					client.setActiveChar(null);
					client = tmp.getClient();
					tmp.store();
					tmp.deleteMe();

					if(client != null)
					{
						client.setActiveChar(null);
					}
					return;
				}
			}

			if(!player.isTeleporting())
			{
				if(tmp != null)
				{
					_log.warn("Duplicate character!? Closing both characters (" + player.getName() + ")");
					player.closeNetConnection();
					tmp.closeNetConnection();
					return;
				}
				_allPlayers.put(player.getName().toLowerCase(), player);
			}

			_allPlayers.put(player.getName().toLowerCase(), player);
		}

		FastList<L2Object> visibles = getVisibleObjects(object, 2000);

		for(L2Object visible : visibles)
		{
			visible.getKnownList().addKnownObject(object, dropper);
			object.getKnownList().addKnownObject(visible, dropper);
		}
	}

	public void addToAllPlayers(L2PcInstance cha)
	{
		_allPlayers.put(cha.getName().toLowerCase(), cha);
	}

	public void removeFromAllPlayers(L2PcInstance cha)
	{
		if(cha != null && !cha.isTeleporting())
		{
			_allPlayers.remove(cha.getName().toLowerCase());
		}
	}

	public void removeVisibleObject(L2Object object, L2WorldRegion oldRegion)
	{
		if(object == null)
		{
			return;
		}

		if(oldRegion != null)
		{
			oldRegion.removeVisibleObject(object);

			for(L2WorldRegion reg : oldRegion.getSurroundingRegions())
			{
				for(L2Object obj : reg.getVisibleObjects())
				{
					if(obj != null && obj.getKnownList() != null)
					{
						obj.getKnownList().removeKnownObject(object);
					}

					if(object.getKnownList() != null)
					{
						object.getKnownList().removeKnownObject(obj);
					}
				}
			}

			object.getKnownList().removeAllKnownObjects();

			if(object instanceof L2PcInstance)
			{
				if(!((L2PcInstance) object).isTeleporting())
				{
					removeFromAllPlayers((L2PcInstance) object);
				}
			}
		}
	}

	public FastList<L2Object> getVisibleObjects(L2Object object)
	{
		if(object == null)
		{
			return null;
		}

		L2WorldRegion reg = object.getWorldRegion();

		if(reg == null)
		{
			return null;
		}

		FastList<L2Object> result = new FastList<L2Object>();

		FastList<L2WorldRegion> regions = reg.getSurroundingRegions();

		for(int i = 0; i < regions.size(); i++)
		{
			for(L2Object _object : regions.get(i).getVisibleObjects())
			{
				if(_object == null)
				{
					continue;
				}

				if(_object.equals(object))
				{
					continue;
				}

				if(!_object.isVisible())
				{
					continue;
				}

				result.add(_object);
			}
		}
		return result;
	}

	public FastList<L2Object> getVisibleObjects(L2Object object, int radius)
	{
		if(object == null || !object.isVisible())
		{
			return new FastList<L2Object>();
		}

		int x = object.getX();
		int y = object.getY();
		int sqRadius = radius * radius;

		FastList<L2Object> result = new FastList<L2Object>();

		FastList<L2WorldRegion> regions = object.getWorldRegion().getSurroundingRegions();

		for(int i = 0; i < regions.size(); i++)
		{
			for(L2Object _object : regions.get(i).getVisibleObjects())
			{
				if(_object == null)
				{
					continue;
				}

				if(_object.equals(object))
				{
					continue;
				}

				int x1 = _object.getX();
				int y1 = _object.getY();

				double dx = x1 - x;
				double dy = y1 - y;

				if(dx * dx + dy * dy < sqRadius)
				{
					result.add(_object);
				}
			}
		}
		return result;
	}

	public FastList<L2Object> getVisibleObjects3D(L2Object object, int radius)
	{
		if(object == null || !object.isVisible())
		{
			return new FastList<L2Object>();
		}

		int x = object.getX();
		int y = object.getY();
		int z = object.getZ();
		int sqRadius = radius * radius;

		FastList<L2Object> result = new FastList<L2Object>();

		FastList<L2WorldRegion> regions = object.getWorldRegion().getSurroundingRegions();

		for(int i = 0; i < regions.size(); i++)
		{
			for(L2Object _object : regions.get(i).getVisibleObjects())
			{
				if(_object == null)
				{
					continue;
				}

				if(_object.equals(object))
				{
					continue;
				}

				int x1 = _object.getX();
				int y1 = _object.getY();
				int z1 = _object.getZ();

				long dx = x1 - x;
				long dy = y1 - y;
				long dz = z1 - z;

				if(dx * dx + dy * dy + dz * dz < sqRadius)
				{
					result.add(_object);
				}
			}
		}
		return result;
	}

	public FastList<L2Playable> getVisiblePlayable(L2Object object)
	{
		L2WorldRegion reg = object.getWorldRegion();

		if(reg == null)
		{
			return null;
		}

		FastList<L2Playable> result = new FastList<L2Playable>();

		FastList<L2WorldRegion> regions = reg.getSurroundingRegions();

		for(int i = 0; i < regions.size(); i++)
		{
			Iterator<L2Playable> playables = regions.get(i).iterateAllPlayers();

			while(playables.hasNext())
			{
				L2Playable _object = playables.next();

				if(_object == null)
				{
					continue;
				}

				if(_object.equals(object))
				{
					continue;
				}

				if(!_object.isVisible())
				{
					continue;
				}

				result.add(_object);

				_object = null;
			}

			playables = null;
		}
		return result;
	}

	public L2WorldRegion getRegion(Point3D point)
	{
		return _worldRegions[(point.getX() >> SHIFT_BY) + OFFSET_X][(point.getY() >> SHIFT_BY) + OFFSET_Y];
	}

	public L2WorldRegion getRegion(int x, int y)
	{
		return _worldRegions[(x >> SHIFT_BY) + OFFSET_X][(y >> SHIFT_BY) + OFFSET_Y];
	}

	public L2WorldRegion[][] getAllWorldRegions()
	{
		return _worldRegions;
	}

	private boolean validRegion(int x, int y)
	{
		return x >= 0 && x <= REGIONS_X && y >= 0 && y <= REGIONS_Y;
	}

	private void initRegions()
	{
		_worldRegions = new L2WorldRegion[REGIONS_X + 1][REGIONS_Y + 1];

		for(int i = 0; i <= REGIONS_X; i++)
		{
			for(int j = 0; j <= REGIONS_Y; j++)
			{
				_worldRegions[i][j] = new L2WorldRegion(i, j);
			}
		}

		for(int x = 0; x <= REGIONS_X; x++)
		{
			for(int y = 0; y <= REGIONS_Y; y++)
			{
				for(int a = -1; a <= 1; a++)
				{
					for(int b = -1; b <= 1; b++)
					{
						if(validRegion(x + a, y + b))
						{
							_worldRegions[x + a][y + b].addSurroundingRegion(_worldRegions[x][y]);
						}
					}
				}
			}
		}

		_log.info("L2World: (" + REGIONS_X + " by " + REGIONS_Y + ") World Region Grid set up.");
	}

	public synchronized void deleteVisibleNpcSpawns()
	{
		_log.info("Deleting all visible NPC's.");

		for(int i = 0; i <= REGIONS_X; i++)
		{
			for(int j = 0; j <= REGIONS_Y; j++)
			{
				_worldRegions[i][j].deleteVisibleNpcSpawns();
			}
		}
		_log.info("All visible NPC's deleted.");
	}

	public FastList<L2PcInstance> getAccountPlayers(String account_name)
	{
		FastList<L2PcInstance> players_for_account = new FastList<L2PcInstance>();

		for(L2PcInstance actual:_allPlayers.values())
		{
			if(actual.getAccountName().equals(account_name))
			{
				players_for_account.add(actual);
			}
		}

		return players_for_account;
	}
}