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
package com.src.gameserver.managers;

import java.util.Collection;
import java.util.Map;

import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.GameTimeController;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2RaidBossInstance;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class DayNightSpawnManager
{
	private final static Log _log = LogFactory.getLog(DayNightSpawnManager.class);

	private static DayNightSpawnManager _instance;
	private static Map<L2Spawn, L2Npc> _dayCreatures;
	private static Map<L2Spawn, L2Npc> _nightCreatures;
	private static Map<L2Spawn, L2RaidBossInstance> _bosses;

	public static DayNightSpawnManager getInstance()
	{
		if(_instance == null)
		{
			_instance = new DayNightSpawnManager();
		}

		return _instance;
	}

	private DayNightSpawnManager()
	{
		_dayCreatures = new FastMap<L2Spawn, L2Npc>();
		_nightCreatures = new FastMap<L2Spawn, L2Npc>();
		_bosses = new FastMap<L2Spawn, L2RaidBossInstance>();
	}

	public void addDayCreature(L2Spawn spawnDat)
	{
		if(_dayCreatures.containsKey(spawnDat))
		{
			_log.warn("DayNightSpawnManager: Spawn already added into day map");
			return;
		}
		else
		{
			_dayCreatures.put(spawnDat, null);
		}
	}

	public void addNightCreature(L2Spawn spawnDat)
	{
		if(_nightCreatures.containsKey(spawnDat))
		{
			_log.warn("DayNightSpawnManager: Spawn already added into night map");
			return;
		}
		else
		{
			_nightCreatures.put(spawnDat, null);
		}
	}

	public void spawnDayCreatures()
	{
		spawnCreatures(_nightCreatures, _dayCreatures, "night", "day");
	}

	public void spawnNightCreatures()
	{
		spawnCreatures(_dayCreatures, _nightCreatures, "day", "night");
	}

	private void spawnCreatures(Map<L2Spawn, L2Npc> UnSpawnCreatures, Map<L2Spawn, L2Npc> SpawnCreatures, String UnspawnLogInfo, String SpawnLogInfo)
	{
		try
		{
			if(UnSpawnCreatures.size() != 0)
			{
				int i = 0;
				for(L2Npc dayCreature : UnSpawnCreatures.values())
				{
					if(dayCreature == null)
					{
						continue;
					}

					dayCreature.getSpawn().stopRespawn();
					dayCreature.deleteMe();
					i++;
				}
				_log.info("DayNightSpawnManager: Deleted " + i + " " + UnspawnLogInfo + " creatures.");
			}

			int i = 0;
			L2Npc creature = null;

			for(L2Spawn spawnDat : SpawnCreatures.keySet())
			{
				if(SpawnCreatures.get(spawnDat) == null)
				{
					creature = spawnDat.doSpawn();
					if(creature == null)
					{
						continue;
					}

					SpawnCreatures.remove(spawnDat);
					SpawnCreatures.put(spawnDat, creature);
					creature.setCurrentHp(creature.getMaxHp());
					creature.setCurrentMp(creature.getMaxMp());
					creature.getSpawn().startRespawn(); 
					if (creature.isDecayed()) 
						creature.setDecayed(false); 
					if (creature.isDead()) 
						creature.doRevive();
				}
				else
				{
					creature = SpawnCreatures.get(spawnDat);
					if(creature == null)
					{
						continue;
					}

					creature.getSpawn().startRespawn();
					if (creature.isDecayed()) 
						creature.setDecayed(false); 
					if (creature.isDead()) 
						creature.doRevive();
					creature.setCurrentHp(creature.getMaxHp());
					creature.setCurrentMp(creature.getMaxMp());
					creature.spawnMe();
				}

				i++;
			}

			creature = null;

			_log.info("DayNightSpawnManager: Spawning " + i + " " + SpawnLogInfo + " creatures.");
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
	}

	private void changeMode(int mode)
	{
		if(_nightCreatures.size() == 0 && _dayCreatures.size() == 0)
			return;

		switch(mode)
		{
			case 0:
				spawnDayCreatures();
				specialNightBoss(0);
				ShadowSenseMsg(0);
				break;
			case 1:
				spawnNightCreatures();
				specialNightBoss(1);
				ShadowSenseMsg(1);
				break;
			default:
				_log.warn("DayNightSpawnManager: Wrong mode sent");
				break;
		}
	}

	public void notifyChangeMode()
	{
		try
		{
			if(GameTimeController.getInstance().isNowNight())
			{
				changeMode(1);
			}
			else
			{
				changeMode(0);
			}
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
	}

	public void cleanUp()
	{
		_nightCreatures.clear();
		_dayCreatures.clear();
		_bosses.clear();
	}

	private void specialNightBoss(int mode)
	{
		try
		{
			for(L2Spawn spawn : _bosses.keySet())
			{
				L2RaidBossInstance boss = _bosses.get(spawn);

				if(boss == null && mode == 1)
				{
					boss = (L2RaidBossInstance) spawn.doSpawn();
					RaidBossSpawnManager.getInstance().notifySpawnNightBoss(boss);
					_bosses.remove(spawn);
					_bosses.put(spawn, boss);
					continue;
				}

				if(boss == null && mode == 0)
				{
					continue;
				}

				if(boss.getNpcId() == 25328 && boss.getRaidStatus().equals(RaidBossSpawnManager.StatusEnum.ALIVE))
				{
					handleHellmans(boss, mode);
				}

				boss = null;
				return;
			}
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
	}

	private void handleHellmans(L2RaidBossInstance boss, int mode)
	{
		switch(mode)
		{
			case 0:
				boss.deleteMe();
				_log.info("DayNightSpawnManager: Deleting Hellman raidboss");
				break;
			case 1:
				boss.spawnMe();
				_log.info("DayNightSpawnManager: Spawning Hellman raidboss");
				break;
		}
	}

	private void ShadowSenseMsg(int mode)
    {
            final L2Skill skill = SkillTable.getInstance().getInfo(294, 1);
            if (skill == null)
                    return;

            final SystemMessageId msg = (mode == 1 ? SystemMessageId.NIGHT_EFFECT_APPLIES : SystemMessageId.DAY_EFFECT_DISAPPEARS);
            final Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers();
            for (L2PcInstance onlinePlayer : pls)
            {
                    if (onlinePlayer.getRace().ordinal() == 2
                            && onlinePlayer.getSkillLevel(294) > 0)
                    {
                            SystemMessage sm = SystemMessage.getSystemMessage(msg);
                            sm.addSkillName(294);
                            onlinePlayer.sendPacket(sm);
                            sm = null;
                    }
            }
    }

	
	public L2RaidBossInstance handleBoss(L2Spawn spawnDat)
	{
		if(_bosses.containsKey(spawnDat))
		{
			return _bosses.get(spawnDat);
		}

		if(GameTimeController.getInstance().isNowNight())
		{
			L2RaidBossInstance raidboss = (L2RaidBossInstance) spawnDat.doSpawn();
			_bosses.put(spawnDat, raidboss);

			return raidboss;
		}
		else
		{
			_bosses.put(spawnDat, null);
		}

		return null;
	}

}