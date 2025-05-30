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

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;

import com.src.Config;
import com.src.gameserver.datatables.xml.TerritoryTable;
import com.src.gameserver.geo.GeoData;
import com.src.gameserver.idfactory.IdFactory;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.quest.Quest;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.random.Rnd;

public class L2Spawn
{
	protected static final Logger _log = Logger.getLogger(L2Spawn.class.getName());

	private L2NpcTemplate _template;

	private int _id;

	private int _location;

	private int _maximumCount;

	private int _currentCount;

	protected int _scheduledCount;

	private int _locX;

	private int _locY;

	private int _locZ;

	private int _heading;

	private int _respawnDelay;

	private int _respawnMinDelay;

	private int _respawnMaxDelay;

	private Constructor<?> _constructor;

	private boolean _doRespawn;
	
	private boolean _customBossInstance = false;

	private int _instanceId = 0;

	private L2Npc _lastSpawn;
	private static List<SpawnListener> _spawnListeners = new FastList<SpawnListener>();

	class SpawnTask implements Runnable
	{
		private L2Npc _oldNpc;

		public SpawnTask(L2Npc pOldNpc)
		{
			_oldNpc = pOldNpc;
		}

		@Override
		public void run()
		{
			try
			{
				if(hasToRespawn())
				{
					respawnNpc(_oldNpc);
				}
			}
			catch(Exception e)
			{
				_log.log(Level.WARNING, "", e);
			}

			_scheduledCount--;
		}
	}

	public L2Spawn(L2NpcTemplate mobTemplate) throws SecurityException, ClassNotFoundException, NoSuchMethodException
	{
		_template = mobTemplate;

		if(_template == null)
		{
			return;
		}

		String implementationName = _template.type;

		if(mobTemplate.npcId == 30995)
		{
			implementationName = "L2RaceManager";
		}

		if(mobTemplate.npcId >= 31046 && mobTemplate.npcId <= 31053)
		{
			implementationName = "L2SymbolMaker";
		}

		Class<?>[] parameters =
		{
				int.class, L2NpcTemplate.class
		};
		_constructor = Class.forName("com.src.gameserver.model.actor.instance." + implementationName + "Instance").getConstructor(parameters);
		implementationName = null;
	}

	public int getAmount()
	{
		return _maximumCount;
	}

	public int getId()
	{
		return _id;
	}

	public int getLocation()
	{
		return _location;
	}

	public int getLocx()
	{
		return _locX;
	}

	public int getLocy()
	{
		return _locY;
	}

	public int getLocz()
	{
		return _locZ;
	}

	public int getNpcid()
	{
		if(_template == null)
		{
			return -1;
		}

		return _template.npcId;
	}

	public int getHeading()
	{
		return _heading;
	}

	public int getRespawnDelay()
	{
		return _respawnDelay;
	}

	public int getRespawnMinDelay()
	{
		return _respawnMinDelay;
	}

	public int getRespawnMaxDelay()
	{
		return _respawnMaxDelay;
	}

	public void setAmount(int amount)
	{
		_maximumCount = amount;
	}

	public void setId(int id)
	{
		_id = id;
	}

	public void setLocation(int location)
	{
		_location = location;
	}

	public void setRespawnMinDelay(int date)
	{
		_respawnMinDelay = date;
	}

	public void setRespawnMaxDelay(int date)
	{
		_respawnMaxDelay = date;
	}

	public void setLocx(int locx)
	{
		_locX = locx;
	}

	public void setLocy(int locy)
	{
		_locY = locy;
	}

	public void setLocz(int locz)
	{
		_locZ = locz;
	}

	public void setHeading(int heading)
	{
		_heading = heading;
	}

	public void setCustom(boolean custom)
	{
		_customSpawn = custom;
	}

	public boolean isCustom()
	{
		return _customSpawn;
	}

	private boolean _customSpawn;

	public void decreaseCount(L2Npc oldNpc)
	{
		_currentCount--;

		if(_doRespawn && _scheduledCount + _currentCount < _maximumCount)
		{
			_scheduledCount++;

			ThreadPoolManager.getInstance().scheduleGeneral(new SpawnTask(oldNpc), _respawnDelay);
		}
	}

	public int init()
	{
		while(_currentCount < _maximumCount)
		{
			doSpawn();
		}
		_doRespawn = true;

		return _currentCount;
	}

	public L2Npc spawnOne()
	{
		return doSpawn();
	}

	public void stopRespawn()
	{
		_doRespawn = false;
	}

	public void startRespawn()
	{
		_doRespawn = true;
	}

	public L2Npc doSpawn()
	{
		return doSpawn(false);
	}
	
	public L2Npc doSpawn(boolean isSummonSpawn)
	{
		L2Npc mob = null;
		try
		{
			if(_template.type.equalsIgnoreCase("L2Pet") || _template.type.equalsIgnoreCase("L2Minion"))
			{
				_currentCount++;

				return mob;
			}

			Object[] parameters =
			{
					IdFactory.getInstance().getNextId(), _template
			};

			Object tmp = _constructor.newInstance(parameters);

			if (isSummonSpawn && tmp instanceof L2Character)
				((L2Character)tmp).setShowSummonAnimation(isSummonSpawn);
			
			((L2Object) tmp).setInstanceId(_instanceId);

			if(!(tmp instanceof L2Npc))
			{
				return mob;
			}

			mob = (L2Npc) tmp;

			return intializeNpcInstance(mob);
		}
		catch(Exception e)
		{
			_log.log(Level.WARNING, "NPC " + _template.npcId + " class not found", e);
		}

		return mob;
	}

	private L2Npc intializeNpcInstance(L2Npc mob)
	{
		int newlocx, newlocy, newlocz;
		boolean doCorrect = false;

		if(Config.GEODATA > 0)
		{
			switch(Config.GEO_CORRECT_Z)
			{
			case ALL:
				doCorrect = true;
				break;
			case TOWN:
				if(mob instanceof L2Npc)
				{
					doCorrect = true;
				}
				break;
			case MONSTER:
				if(mob instanceof L2Attackable)
				{
					doCorrect = true;
				}
				break;
			default:
				break;
			}
		}

		if(getLocx() == 0 && getLocy() == 0)
		{
			if(getLocation() == 0)
			{
				return mob;
			}

			int p[] = TerritoryTable.getInstance().getRandomPoint(getLocation());

			newlocx = p[0];
			newlocy = p[1];
			newlocz = GeoData.getInstance().getSpawnHeight(newlocx, newlocy, p[2], p[3], this);
		}
		else
		{
			newlocx = getLocx();
			newlocy = getLocy();

			newlocz = doCorrect ? GeoData.getInstance().getSpawnHeight(newlocx, newlocy, getLocz(), getLocz(), this) : getLocz();
		}

		mob.stopAllEffects();
		
		mob.setIsDead(false);
		
		mob.setDecayed(false);
		
		mob.setIsKilledAlready(false);
		
		mob.setCurrentHpMp(mob.getMaxHp(), mob.getMaxMp());

		if(getHeading() == -1)
		{
			mob.setHeading(Rnd.nextInt(61794));
		}
		else
		{
			mob.setHeading(getHeading());
		}
		mob.setSpawn(this);

		mob.spawnMe(newlocx, newlocy, newlocz);

		L2Spawn.notifyNpcSpawned(mob);

		_lastSpawn = mob;

		if(mob.getTemplate().getEventQuests(Quest.QuestEventType.ON_SPAWN) != null)
		{
			for(Quest quest : mob.getTemplate().getEventQuests(Quest.QuestEventType.ON_SPAWN))
			{
				quest.notifySpawn(mob);
			}
		}

		_currentCount++;
		return mob;
	}

	public static void addSpawnListener(SpawnListener listener)
	{
		synchronized (_spawnListeners)
		{
			_spawnListeners.add(listener);
		}
	}

	public static void removeSpawnListener(SpawnListener listener)
	{
		synchronized (_spawnListeners)
		{
			_spawnListeners.remove(listener);
		}
	}

	public static void notifyNpcSpawned(L2Npc npc)
	{
		synchronized (_spawnListeners)
		{
			for(SpawnListener listener : _spawnListeners)
			{
				listener.npcSpawned(npc);
			}
		}
	}

	public void setRespawnDelay(int i)
	{
		if(i < 0)
		{
			_log.warning("respawn delay is negative for spawnId:" + _id);
		}

		if(i < 10)
		{
			i = 10;
		}

		_respawnDelay = i * 1000;
	}

	public L2Npc getLastSpawn()
	{
		return _lastSpawn;
	}

	public void respawnNpc(L2Npc oldNpc)
	{
		if (_doRespawn) 
		{ 
			oldNpc.refreshID(); 
			/*L2NpcInstance instance = */intializeNpcInstance(oldNpc); 
		} 
	}

	public L2NpcTemplate getTemplate()
	{
		return _template;
	}

	public int getInstanceId()
	{
		return _instanceId;
	}

	public void setInstanceId(int instanceId)
	{
		_instanceId = instanceId;
	}

	public boolean hasToRespawn()
	{
	  return _doRespawn;
	}

	public boolean is_customBossInstance()
	{
		return _customBossInstance;
	}

	public void set_customBossInstance(boolean customBossInstance)
	{
		_customBossInstance = customBossInstance;
	}
}