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
import java.util.logging.Logger;

import javolution.util.FastList;

import com.src.Config;
import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.managers.GrandBossManager;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2GrandBossInstance;
import com.src.gameserver.model.actor.instance.L2MonsterInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.quest.Quest;
import com.src.gameserver.model.zone.type.L2BossZone;
import com.src.gameserver.network.serverpackets.PlaySound;
import com.src.gameserver.network.serverpackets.SocialAction;
import com.src.gameserver.templates.StatsSet;
import com.src.util.random.Rnd;

public class QueenAnt extends Quest
{
	private static final int QUEEN = 29001;
	private static final int LARVA = 29002;
	private static final int NURSE = 29003;
	private static final int GUARD = 29004;
	private static final int ROYAL = 29005;

	//QUEEN Status Tracking :
	private static final byte ALIVE = 0; //Queen Ant is spawned.
	private static final byte DEAD = 1; //Queen Ant has been killed.

	private static L2BossZone _Zone;
	private static List<L2Attackable> _Minions = new FastList<L2Attackable>();
	private L2MonsterInstance _larva = null;
	protected static final Logger _log = Logger.getLogger(QueenAnt.class.getName());

	public QueenAnt(int questId, String name, String descr)
	{
		super(questId, name, descr);

		int[] mobs =
		{
				QUEEN, LARVA, NURSE, GUARD, ROYAL
		};
		for(int mob : mobs)
		{
			addEventId(mob, Quest.QuestEventType.ON_KILL);
			addEventId(mob, Quest.QuestEventType.ON_ATTACK);
			addEventId(mob, Quest.QuestEventType.ON_FACTION_CALL);
		}

		_Zone = GrandBossManager.getInstance().getZone(-21610, 181594, -5734);

		StatsSet info = GrandBossManager.getInstance().getStatsSet(QUEEN);
		int status = GrandBossManager.getInstance().getBossStatus(QUEEN);
		if(status == DEAD)
		{
			// load the unlock date and time for queen ant from DB
			long temp = info.getLong("respawn_time") - System.currentTimeMillis();
			// if queen ant is locked until a certain time, mark it so and start the unlock timer
			// the unlock time has not yet expired.
			if(temp > 0)
			{
				startQuestTimer("queen_unlock", temp, null, null);
			}
			else
			{
				// the time has already expired while the server was offline. Immediately spawn queen ant.
				L2GrandBossInstance queen = (L2GrandBossInstance) addSpawn(QUEEN, -21610, 181594, -5734, 0, false, 0);
				GrandBossManager.getInstance().setBossStatus(QUEEN, ALIVE);
				spawnBoss(queen);
				if(_larva != null) {
				_larva.deleteMe();
				_larva = null;
				}
			}
		}
		else
		{
			int loc_x = info.getInteger("loc_x");
			int loc_y = info.getInteger("loc_y");
			int loc_z = info.getInteger("loc_z");
			int heading = info.getInteger("heading");
			int hp = info.getInteger("currentHP");
			int mp = info.getInteger("currentMP");
			L2GrandBossInstance queen = (L2GrandBossInstance) addSpawn(QUEEN, loc_x, loc_y, loc_z, heading, false, 0);
			queen.setCurrentHpMp(hp, mp);
			spawnBoss(queen);
			if(_larva != null) {
			_larva.deleteMe();
			_larva = null;
			}
		}
	}

	public void spawnBoss(L2GrandBossInstance npc)
	{
		GrandBossManager.getInstance().addBoss(npc);
		/*if (Rnd.get(100) < 33)
		    _Zone.movePlayersTo(-19480,187344,-5600);
		else if (Rnd.get(100) < 50)
		    _Zone.movePlayersTo(-17928,180912,-5520);
		else
		    _Zone.movePlayersTo(-23808,182368,-5600);*/

		startQuestTimer("action", 10000, npc, null);
		npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
		//Spawn minions
		_larva = (L2MonsterInstance)addSpawn(LARVA, -21600, 179482, -5846, Rnd.get(360), false, 0);
		addSpawn(NURSE, -22000, 179482, -5846, 0, false, 0);
		addSpawn(NURSE, -21200, 179482, -5846, 0, false, 0);
		int radius = 400;
		for(int i = 0; i < 6; i++)
		{
			int x = (int) (radius * Math.cos(i * 1.407)); //1.407~2pi/6
			int y = (int) (radius * Math.sin(i * 1.407));
			addSpawn(NURSE, npc.getX() + x, npc.getY() + y, npc.getZ(), 0, false, 0);
		}
		for(int i = 0; i < 8; i++)
		{
			int x = (int) (radius * Math.cos(i * .7854)); //.7854~2pi/8
			int y = (int) (radius * Math.sin(i * .7854));
			_Minions.add((L2Attackable) addSpawn(ROYAL, npc.getX() + x, npc.getY() + y, npc.getZ(), 0, false, 0));
		}
		startQuestTimer("check_royal__Zone", 30000, npc, null);
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		if(event.equalsIgnoreCase("action") && npc != null)
		{
			if(!_Zone.isInsideZone(npc))
			{
				((L2Attackable) npc).clearAggroList();
				npc.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				npc.teleToLocation(-21610, 181594, -5740);
			}
			else if(Rnd.get(3) == 0)
			{
				if(Rnd.get(2) == 0)
				{
					npc.broadcastPacket(new SocialAction(npc.getObjectId(), 3));
				}
				else
				{
					npc.broadcastPacket(new SocialAction(npc.getObjectId(), 4));
				}
			}
			
			for(L2Object obj : npc.getKnownList().getKnownCharactersInRadius(1500))
				{
					if(obj != null && obj instanceof L2PcInstance && ((L2PcInstance) obj).getLevel() > 49 && !((L2PcInstance) obj).isGM())
					{
						if (Rnd.get(100) < 33)
							((L2PcInstance) obj).teleToLocation(-19480,187344,-5600);
						else if (Rnd.get(100) < 50)
							((L2PcInstance) obj).teleToLocation(-17928,180912,-5520);
						else
							((L2PcInstance) obj).teleToLocation(-23808,182368,-5600);
					}
				}
			startQuestTimer("action", 10000, npc, null);
		}
		else if(event.equalsIgnoreCase("queen_unlock"))
		{
			L2GrandBossInstance queen = (L2GrandBossInstance) addSpawn(QUEEN, -21610, 181594, -5734, 0, false, 0);
			GrandBossManager.getInstance().setBossStatus(QUEEN, ALIVE);
			spawnBoss(queen);
			// unspawn larva todo
		}
		else if(event.equalsIgnoreCase("check_royal__Zone") && npc != null)
		{
			for(int i = 0; i < _Minions.size(); i++)
			{
				L2Attackable mob = _Minions.get(i);
				if(mob != null && !_Zone.isInsideZone(mob))
				{
					mob.teleToLocation(npc.getX(), npc.getY(), npc.getZ());
				}
			}
			startQuestTimer("check_royal__Zone", 30000, npc, null);
		}
		else if(event.equalsIgnoreCase("despawn_royals"))
		{
			for(int i = 0; i < _Minions.size(); i++)
			{
				L2Attackable mob = _Minions.get(i);
				if(mob != null)
				{
					mob.decayMe();
				}
			}
			_Minions.clear();
		}
		else if(event.equalsIgnoreCase("spawn_royal") && GrandBossManager.getInstance().getBossStatus(QUEEN) == ALIVE)
		{
			_Minions.add((L2Attackable) addSpawn(ROYAL, npc.getX(), npc.getY(), npc.getZ(), 0, true, 0));
		}
		else if(event.equalsIgnoreCase("spawn_nurse") && GrandBossManager.getInstance().getBossStatus(QUEEN) == ALIVE)
		{
			addSpawn(NURSE, npc.getX(), npc.getY(), npc.getZ(), 0, true, 0);
		}
		return super.onAdvEvent(event, npc, player);
	}

	@Override
	public String onFactionCall(L2Npc npc, L2Npc caller, L2PcInstance attacker, boolean isPet)
	{
		if(caller == null || npc == null)
			return super.onFactionCall(npc, caller, attacker, isPet);
		int npcId = npc.getNpcId();
		int callerId = caller.getNpcId();
		if(npcId == NURSE)
		{
			if(callerId == LARVA)
			{
				npc.setTarget(caller);
				npc.doCast(SkillTable.getInstance().getInfo(4020, 1));
				npc.doCast(SkillTable.getInstance().getInfo(4024, 1));
				return null;
			}
			else if(callerId == QUEEN)
			{
				if(npc.getTarget() != null && npc.getTarget() instanceof L2Npc)
				{
					if(((L2Npc) npc.getTarget()).getNpcId() == LARVA)
						return null;
				}
				int percentage = caller.getMaxHp() * 2 / 3;
				if(caller.getCurrentHp() < percentage) {
				npc.setTarget(caller);
				npc.doCast(SkillTable.getInstance().getInfo(4020, 1));
				}
				return null;
			}
		}
		return super.onFactionCall(npc, caller, attacker, isPet);
	}

	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		int npcId = npc.getNpcId();
		if(attacker.getLevel() > (npc.getLevel()+9))
		{
			damage = 0;
			L2Skill skill = SkillTable.getInstance().getInfo(4515, 1);
			skill.getEffects(npc, attacker);
			return null;
		}
		if(npcId == NURSE)
		{
			npc.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null, null);
			//return null;
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		int npcId = npc.getNpcId();
		if(npcId == QUEEN && !npc.getSpawn().is_customBossInstance())
		{
			npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
			GrandBossManager.getInstance().setBossStatus(QUEEN, DEAD);
			//time is 36hour	+/- 17hour
			long randomizeMinutes = Rnd.get(60)*60000; // get random minutes in hours
			long respawnTime = (Config.QA_RESP_FIRST + Rnd.get(Config.QA_RESP_SECOND)) * 3600000 + randomizeMinutes;
			startQuestTimer("queen_unlock", respawnTime, null, null);
			cancelQuestTimer("action", npc, null);
			cancelQuestTimer("check_royal__Zone", npc, null);
			// also save the respawn time so that the info is maintained past reboots
			StatsSet info = GrandBossManager.getInstance().getStatsSet(QUEEN);
			info.set("respawn_time", System.currentTimeMillis() + respawnTime);
			GrandBossManager.getInstance().setStatsSet(QUEEN, info);
			startQuestTimer("despawn_royals", 20000, null, null);
			//this.cancelQuestTimers("spawn_minion");
		}
		else if(GrandBossManager.getInstance().getBossStatus(QUEEN) == ALIVE)
		{
			if(npcId == ROYAL)
			{
				_Minions.remove(npc);
				startQuestTimer("spawn_royal", (Config.QA_RESP_ROYAL + Rnd.get(40)) * 1000, npc, null);
			}
			else if(npcId == NURSE)
			{
				startQuestTimer("spawn_nurse", Config.QA_RESP_NURSE * 1000, npc, null);
			}
		}
		return super.onKill(npc, killer, isPet);
	}
}
