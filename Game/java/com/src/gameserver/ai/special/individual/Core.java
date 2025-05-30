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
package com.src.gameserver.ai.special.individual;

import java.util.List;

import javolution.util.FastList;

import com.src.Config;
import com.src.gameserver.managers.GrandBossManager;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2GrandBossInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.quest.Quest;
import com.src.gameserver.network.serverpackets.CreatureSay;
import com.src.gameserver.network.serverpackets.PlaySound;
import com.src.gameserver.templates.StatsSet;
import com.src.util.random.Rnd;

public class Core extends Quest
{
	private static final int CORE = 29006;

	private static final byte ALIVE = 0;
	private static final byte DEAD = 1;
	
	private static final int DEATH_KNIGHT = 29007;
	private static final int DOOM_WRAITH = 29008;
	private static final int SUSCEPTOR = 29011;

	private static boolean _FirstAttacked;
	
	/**
	 * Array structure: NpcId, spawnX, spawnY, spawnZ
	 */
	private static int[][] MINIONS_SPAWN = 
	{
		// DEATH KNIGHT
		{ 29007, 17545, 109344, -6482 },
		{ 29007, 17897, 109340, -6508 },
		{ 29007, 18147, 109099, -6482 },
		{ 29007, 18141, 108753, -6482 },
		{ 29007, 17900, 108499, -6508 },
		{ 29007, 17546, 108504, -6482 },
		{ 29007, 17312, 108747, -6482 },
		{ 29007, 17308, 109091, -6508 },
		{ 29007, 17552, 109113, -6476 },
		{ 29007, 17873, 109128, -6476 },

		// DOOM WRAITH
		{ 29008, 17463, 108909, -6476 },
		{ 29008, 17728, 109208, -6476 },
		{ 29008, 17970, 108936, -6502 },

		// SUSCEPTOR
		{ 29011, 17317, 108938, -6482 },
		{ 29011, 17732, 108494, -6508 },
		{ 29011, 18143, 108920, -6508 },
		{ 29011, 17730, 109351, -6482 },
	};

	List<L2Attackable> Minions = new FastList<L2Attackable>();

	public Core(int id, String name, String descr)
	{
		super(id, name, descr);

		int[] mobs =
		{
				CORE, DEATH_KNIGHT, DOOM_WRAITH, SUSCEPTOR
		};

		for(int mob : mobs)
		{
			addEventId(mob, Quest.QuestEventType.ON_KILL);
			addEventId(mob, Quest.QuestEventType.ON_ATTACK);
		}

		_FirstAttacked = false;
		StatsSet info = GrandBossManager.getInstance().getStatsSet(CORE);
		int status = GrandBossManager.getInstance().getBossStatus(CORE);
		if(status == DEAD)
		{
			long temp = info.getLong("respawn_time") - System.currentTimeMillis();
			if(temp > 0)
			{
				startQuestTimer("core_unlock", temp, null, null);
			}
			else
			{
				L2GrandBossInstance core = (L2GrandBossInstance) addSpawn(CORE, 17726, 108915, -6480, 0, false, 0);
				GrandBossManager.getInstance().setBossStatus(CORE, ALIVE);
				spawnBoss(core);
			}
		}
		else
		{
			String test = loadGlobalQuestVar("Core_Attacked");
			if(test.equalsIgnoreCase("true"))
			{
				_FirstAttacked = true;
			}

			L2GrandBossInstance core = (L2GrandBossInstance) addSpawn(CORE, 17726, 108915, -6480, 0, false, 0);
			spawnBoss(core);
		}
	}

	@Override
	public void saveGlobalData()
	{
		String val = "" + _FirstAttacked;
		saveGlobalQuestVar("Core_Attacked", val);
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		if(event.equalsIgnoreCase("core_unlock"))
		{
			L2GrandBossInstance core = (L2GrandBossInstance) addSpawn(CORE, 17726, 108915, -6480, 0, false, 0);
			GrandBossManager.getInstance().setBossStatus(CORE, ALIVE);
			spawnBoss(core);
		}
		else if(event.equalsIgnoreCase("spawn_minion") && GrandBossManager.getInstance().getBossStatus(CORE) == ALIVE)
		{
			Minions.add((L2Attackable) addSpawn(npc.getNpcId(), npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), false, 0));
		}
		else if(event.equalsIgnoreCase("despawn_minions"))
		{
			for(int i = 0; i < Minions.size(); i++)
			{
				L2Attackable mob = Minions.get(i);
				if(mob != null)
				{
					mob.decayMe();
				}
			}

			Minions.clear();
		}

		return super.onAdvEvent(event, npc, player);
	}

	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		if(npc.getNpcId() == CORE)
		{
			if(_FirstAttacked)
			{
				if(Rnd.get(100) == 0)
				{
					npc.broadcastPacket(new CreatureSay(npc.getObjectId(), 0, npc.getName(), "Removing intruders."));
				}
			}
			else
			{
				_FirstAttacked = true;
				npc.broadcastPacket(new CreatureSay(npc.getObjectId(), 0, npc.getName(), "A non-permitted target has been discovered."));
				npc.broadcastPacket(new CreatureSay(npc.getObjectId(), 0, npc.getName(), "Starting intruder removal system."));
			}
		}

		return super.onAttack(npc, attacker, damage, isPet);
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		int npcId = npc.getNpcId();
		String name = npc.getName();
		if(npcId == CORE && !npc.getSpawn().is_customBossInstance())
		{
			int objId = npc.getObjectId();
			npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, objId, npc.getX(), npc.getY(), npc.getZ()));
			npc.broadcastPacket(new CreatureSay(objId, 0, name, "A fatal error has occurred."));
			npc.broadcastPacket(new CreatureSay(objId, 0, name, "System is being shut down..."));
			npc.broadcastPacket(new CreatureSay(objId, 0, name, "......"));
			_FirstAttacked = false;
			addSpawn(31842, 16502, 110165, -6394, 0, false, 900000);
			addSpawn(31842, 18948, 110166, -6397, 0, false, 900000);
			GrandBossManager.getInstance().setBossStatus(CORE, DEAD);
			long respawnTime = (Config.CORE_RESP_FIRST + Rnd.get(Config.CORE_RESP_SECOND)) * 3600000;
			startQuestTimer("core_unlock", respawnTime, null, null);
			StatsSet info = GrandBossManager.getInstance().getStatsSet(CORE);
			info.set("respawn_time", (System.currentTimeMillis() + respawnTime));
			GrandBossManager.getInstance().setStatsSet(CORE, info);
			startQuestTimer("despawn_minions", 20000, null, null);
		}
		else if(GrandBossManager.getInstance().getBossStatus(CORE) == ALIVE && Minions.contains(npc))
		{
			Minions.remove(npc);
			startQuestTimer("spawn_minion", Config.CORE_RESP_MINION * 1000, npc, null);
		}

		return super.onKill(npc, killer, isPet);
	}

	public void spawnBoss(L2GrandBossInstance npc)
	{
		GrandBossManager.getInstance().addBoss(npc);
		npc.broadcastPacket(new PlaySound(1, "BS01_A", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
		
		for (int i=0; i<= MINIONS_SPAWN.length -1; i++)
		{
			Minions.add((L2Attackable) addSpawn(MINIONS_SPAWN[i][0], MINIONS_SPAWN[i][1], MINIONS_SPAWN[i][2], MINIONS_SPAWN[i][3], 280 + Rnd.get(40), false, 0));
		}
	}

}