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

import java.util.logging.Logger;

import com.src.Config;
import com.src.gameserver.GameTimeController;
import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.ai.L2AttackableAIScript;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.xml.DoorTable;
import com.src.gameserver.managers.GrandBossManager;
import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2GrandBossInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.zone.type.L2BossZone;
import com.src.gameserver.network.serverpackets.PlaySound;
import com.src.gameserver.templates.StatsSet;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.random.Rnd;

public class Zaken extends L2AttackableAIScript
{
	protected static final Logger log = Logger.getLogger(Zaken.class.getName());
	private int _1001 = 0;
	private int _ai0 = 0;
	private int _ai1 = 0;
	private int _ai2 = 0;
	private int _ai3 = 0;
	private int _ai4 = 0;
	private int _quest0 = 0;
	private int _quest1 = 0;
	private int _quest2 = 0;
	private L2PcInstance c_quest0 = null;
	private L2PcInstance c_quest1 = null;
	private L2PcInstance c_quest2 = null;
	private L2PcInstance c_quest3 = null;
	private L2PcInstance c_quest4 = null;
	private static final int ZAKEN = 29022;
	private static final int doll_blader_b = 29023;
	private static final int vale_master_b = 29024;
	private static final int pirates_zombie_captain_b = 29026;
	private static final int pirates_zombie_b = 29027;
	private static final int[] Xcoords =
		{ 53950, 55980, 54950, 55970, 53930, 55970, 55980, 54960, 53950, 53930, 55970, 55980, 54960, 53950, 53930 };
	private static final int[] Ycoords =
		{ 219860, 219820, 218790, 217770, 217760, 217770, 219920, 218790, 219860, 217760, 217770, 219920, 218790, 219860, 217760 };
	private static final int[] Zcoords =
		{ -3488, -3488, -3488, -3488, -3488, -3216, -3216, -3216, -3216, -3216, -2944, -2944, -2944, -2944, -2944 };
	private static final byte ALIVE = 0;
	private static final byte DEAD = 1;
	private static L2BossZone _zone;

	public Zaken(int questId, String name, String descr)
	{
		super(questId, name, descr);
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					int time = GameTimeController.getInstance().getGameTime();
					int hour = (time/60)%24;
					if(hour == 0)
					{
						DoorTable.getInstance().getDoor(21240006).openMe();
						ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
						{
							@Override
							public void run()
							{
								try
								{
									DoorTable.getInstance().getDoor(21240006).closeMe();
								}
								catch(Throwable e)
								{
									log.warning("Cannot close door ID: 21240006 " + e);
								}
							}
						}, 300000L);
					}
				}
				catch(Throwable e)
				{
					log.warning("Cannot open door ID: 21240006 " + e);
				}
			}
		}, 2000L, 600000L);
		int[] mobs =
			{ ZAKEN, doll_blader_b, vale_master_b, pirates_zombie_captain_b, pirates_zombie_b };
		registerMobs(mobs);
		_zone = GrandBossManager.getInstance().getZone(55312, 219168, -3223);
		StatsSet info = GrandBossManager.getInstance().getStatsSet(ZAKEN);
		int status = GrandBossManager.getInstance().getBossStatus(ZAKEN);
		if(status == DEAD)
		{
			long temp = info.getLong("respawn_time") - System.currentTimeMillis();
			if(temp > 0)
			{
				startQuestTimer("zaken_unlock", temp, null, null);
			}
			else
			{
				L2GrandBossInstance zaken = (L2GrandBossInstance) addSpawn(ZAKEN, 55312, 219168, -3223, 0, false, 0);
				GrandBossManager.getInstance().setBossStatus(ZAKEN, ALIVE);
				spawnBoss(zaken);
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
			L2GrandBossInstance zaken = (L2GrandBossInstance) addSpawn(ZAKEN, loc_x, loc_y, loc_z, heading, false, 0);
			zaken.setCurrentHpMp(hp, mp);
			spawnBoss(zaken);
		}
	}

	public void spawnBoss(L2GrandBossInstance npc)
	{
		if(npc == null)
		{
			log.warning("Zaken AI failed to load, missing Zaken in grandboss_data.sql");
			return;
		}

		GrandBossManager.getInstance().addBoss(npc);
		npc.broadcastPacket(new PlaySound(1, "BS01_A", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
		_ai0 = 0;
		_ai1 = npc.getX();
		_ai2 = npc.getY();
		_ai3 = npc.getZ();
		_quest0 = 0;
		_quest1 = 0;
		_quest2 = 3;
		if(_zone == null)
		{
			log.warning("Zaken AI failed to load, missing zone for Zaken");
			return;
		}

		if(_zone.isInsideZone(npc))
		{
			_ai4 = 1;
			startQuestTimer("1003", 1700, null, null);
		}
		_1001 = 1;
		startQuestTimer("1001", 1000, npc, null);
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		int status = GrandBossManager.getInstance().getBossStatus(ZAKEN);
		if(status == DEAD && !event.equalsIgnoreCase("zaken_unlock"))
		{
			return super.onAdvEvent(event, npc, player);
		}

		if(event.equalsIgnoreCase("1001"))
		{
			if(_1001 == 1)
			{
				_1001 = 0;
				cancelQuestTimer("1001", npc, null);
			}

			int sk_4223 = 0;
			int sk_4227 = 0;
			L2Effect[] effects = npc.getAllEffects();
			if(effects.length != 0 || effects != null)
			{
				for(L2Effect e : effects)
				{
					if(e.getSkill().getId() == 4227)
					{
						sk_4227 = 1;
					}

					if(e.getSkill().getId() == 4223)
					{
						sk_4223 = 1;
					}
				}
			}

			if(getTimeHour() < 5)
			{
				if(sk_4223 == 1)
				{
					npc.setTarget(npc);
					npc.doCast(SkillTable.getInstance().getInfo(4224, 1));
					_ai1 = npc.getX();
					_ai2 = npc.getY();
					_ai3 = npc.getZ();
				}

				if(sk_4227 == 0)
				{
					npc.setTarget(npc);
					npc.doCast(SkillTable.getInstance().getInfo(4227, 1));
				}

				if(npc.getAI().getIntention() == CtrlIntention.AI_INTENTION_ATTACK && _ai0 == 0)
				{
					int i0 = 0;
					int i1 = 1;
					if(((L2Attackable) npc).getMostHated() != null)
					{
						if((((((L2Attackable) npc).getMostHated().getX() - _ai1) * (((L2Attackable) npc).getMostHated().getX() - _ai1)) + ((((L2Attackable) npc).getMostHated().getY() - _ai2) * (((L2Attackable) npc).getMostHated().getY() - _ai2))) > (1500 * 1500))
						{
							i0 = 1;
						}
						else
						{
							i0 = 0;
						}

						if(i0 == 0)
						{
							i1 = 0;
						}

						if(_quest0 > 0)
						{
							if(c_quest0 == null)
							{
								i0 = 0;
							}
							else if((((c_quest0.getX() - _ai1) * (c_quest0.getX() - _ai1)) + ((c_quest0.getY() - _ai2) * (c_quest0.getY() - _ai2))) > (1500 * 1500))
							{
								i0 = 1;
							}
							else
							{
								i0 = 0;
							}

							if(i0 == 0)
							{
								i1 = 0;
							}
						}

						if(_quest0 > 1)
						{
							if(c_quest1 == null)
							{
								i0 = 0;
							}
							else if((((c_quest1.getX() - _ai1) * (c_quest1.getX() - _ai1)) + ((c_quest1.getY() - _ai2) * (c_quest1.getY() - _ai2))) > (1500 * 1500))
							{
								i0 = 1;
							}
							else
							{
								i0 = 0;
							}

							if(i0 == 0)
							{
								i1 = 0;
							}
						}

						if(_quest0 > 2)
						{
							if(c_quest2 == null)
							{
								i0 = 0;
							}
							else if((((c_quest2.getX() - _ai1) * (c_quest2.getX() - _ai1)) + ((c_quest2.getY() - _ai2) * (c_quest2.getY() - _ai2))) > (1500 * 1500))
							{
								i0 = 1;
							}
							else
							{
								i0 = 0;
							}

							if(i0 == 0)
							{
								i1 = 0;
							}
						}

						if(_quest0 > 3)
						{
							if(c_quest3 == null)
							{
								i0 = 0;
							}
							else if((((c_quest3.getX() - _ai1) * (c_quest3.getX() - _ai1)) + ((c_quest3.getY() - _ai2) * (c_quest3.getY() - _ai2))) > (1500 * 1500))
							{
								i0 = 1;
							}
							else
							{
								i0 = 0;
							}

							if(i0 == 0)
							{
								i1 = 0;
							}
						}

						if(_quest0 > 4)
						{
							if(c_quest4 == null)
							{
								i0 = 0;
							}
							else if((((c_quest4.getX() - _ai1) * (c_quest4.getX() - _ai1)) + ((c_quest4.getY() - _ai2) * (c_quest4.getY() - _ai2))) > (1500 * 1500))
							{
								i0 = 1;
							}
							else
							{
								i0 = 0;
							}

							if(i0 == 0)
							{
								i1 = 0;
							}
						}

						if(i1 == 1)
						{
							_quest0 = 0;
							int i2 = Rnd.get(15);
							_ai1 = Xcoords[i2] + Rnd.get(650);
							_ai2 = Ycoords[i2] + Rnd.get(650);
							_ai3 = Zcoords[i2];
							npc.setTarget(npc);
							npc.doCast(SkillTable.getInstance().getInfo(4222, 1));
						}
					}
				}

				if(Rnd.get(20) < 1 && _ai0 == 0)
				{
					_ai1 = npc.getX();
					_ai2 = npc.getY();
					_ai3 = npc.getZ();
				}

				L2Character c_ai0 = null;
				if(npc.getAI().getIntention() == CtrlIntention.AI_INTENTION_ATTACK && _quest1 == 0)
				{
					if(((L2Attackable) npc).getMostHated() != null)
					{
						c_ai0 = ((L2Attackable) npc).getMostHated();
						_quest1 = 1;
					}
				}
				else if(npc.getAI().getIntention() == CtrlIntention.AI_INTENTION_ATTACK && _quest1 != 0)
				{
					if(((L2Attackable) npc).getMostHated() != null)
					{
						if(c_ai0 == ((L2Attackable) npc).getMostHated())
						{
							_quest1 = (_quest1 + 1);
						}
						else
						{
							_quest1 = 1;
							c_ai0 = ((L2Attackable) npc).getMostHated();
						}
					}
				}

				if(npc.getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
				{
					_quest1 = 0;
				}

				if(_quest1 > 5)
				{
					((L2Attackable) npc).stopHating(c_ai0);
					L2Character nextTarget = ((L2Attackable) npc).getMostHated();
					if(nextTarget != null)
					{
						npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, nextTarget);
					}
					_quest1 = 0;
				}
			}
			else if(sk_4223 == 0)
			{
				npc.setTarget(npc);
				npc.doCast(SkillTable.getInstance().getInfo(4223, 1));
				_quest2 = 3;
			}

			if(sk_4227 == 1)
			{
				npc.setTarget(npc);
				npc.doCast(SkillTable.getInstance().getInfo(4242, 1));
			}

			if(Rnd.get(40) < 1)
			{
				int i2 = Rnd.get(15);
				_ai1 = Xcoords[i2] + Rnd.get(650);
				_ai2 = Ycoords[i2] + Rnd.get(650);
				_ai3 = Zcoords[i2];
				npc.setTarget(npc);
				npc.doCast(SkillTable.getInstance().getInfo(4222, 1));
			}
			startQuestTimer("1001", 30000, npc, null);
		}

		if(event.equalsIgnoreCase("1002"))
		{
			_quest0 = 0;
			npc.doCast(SkillTable.getInstance().getInfo(4222, 1));
			_ai0 = 0;
		}

		if(event.equalsIgnoreCase("1003"))
		{
			if(_ai4 == 1)
			{
				int rr = Rnd.get(15);
				addSpawn(pirates_zombie_captain_b, Xcoords[rr] + Rnd.get(650), Ycoords[rr] + Rnd.get(650), Zcoords[rr], Rnd.get(65536), false, 0).setIsRaidMinion(true);
				_ai4 = 2;
			}
			else if(_ai4 == 2)
			{
				int rr = Rnd.get(15);
				addSpawn(doll_blader_b, Xcoords[rr] + Rnd.get(650), Ycoords[rr] + Rnd.get(650), Zcoords[rr], Rnd.get(65536), false, 0).setIsRaidMinion(true);
				_ai4 = 3;
			}
			else if(_ai4 == 3)
			{
				addSpawn(vale_master_b, Xcoords[Rnd.get(15)] + Rnd.get(650), Ycoords[Rnd.get(15)] + Rnd.get(650), Zcoords[Rnd.get(15)], Rnd.get(65536), false, 0).setIsRaidMinion(
				true);
				addSpawn(vale_master_b, Xcoords[Rnd.get(15)] + Rnd.get(650), Ycoords[Rnd.get(15)] + Rnd.get(650), Zcoords[Rnd.get(15)], Rnd.get(65536), false, 0).setIsRaidMinion(
				true);
				_ai4 = 4;
			}
			else if(_ai4 == 4)
			{
				addSpawn(pirates_zombie_b, Xcoords[Rnd.get(15)] + Rnd.get(650), Ycoords[Rnd.get(15)] + Rnd.get(650), Zcoords[Rnd.get(15)], Rnd.get(65536), false, 0)
				.setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, Xcoords[Rnd.get(15)] + Rnd.get(650), Ycoords[Rnd.get(15)] + Rnd.get(650), Zcoords[Rnd.get(15)], Rnd.get(65536), false, 0)
				.setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, Xcoords[Rnd.get(15)] + Rnd.get(650), Ycoords[Rnd.get(15)] + Rnd.get(650), Zcoords[Rnd.get(15)], Rnd.get(65536), false, 0)
				.setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, Xcoords[Rnd.get(15)] + Rnd.get(650), Ycoords[Rnd.get(15)] + Rnd.get(650), Zcoords[Rnd.get(15)], Rnd.get(65536), false, 0)
				.setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, Xcoords[Rnd.get(15)] + Rnd.get(650), Ycoords[Rnd.get(15)] + Rnd.get(650), Zcoords[Rnd.get(15)], Rnd.get(65536), false, 0)
				.setIsRaidMinion(true);
				_ai4 = 5;
			}
			else if(_ai4 == 5)
			{
				addSpawn(doll_blader_b, 52675, 219371, -3290, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(doll_blader_b, 52687, 219596, -3368, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(doll_blader_b, 52672, 219740, -3418, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 52857, 219992, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_captain_b, 52959, 219997, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(vale_master_b, 53381, 220151, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_captain_b, 54236, 220948, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 54885, 220144, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 55264, 219860, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_captain_b, 55399, 220263, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 55679, 220129, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(vale_master_b, 56276, 220783, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(vale_master_b, 57173, 220234, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 56267, 218826, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(doll_blader_b, 56294, 219482, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_captain_b, 56094, 219113, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(doll_blader_b, 56364, 218967, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 57113, 218079, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(doll_blader_b, 56186, 217153, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 55440, 218081, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_captain_b, 55202, 217940, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 55225, 218236, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 54973, 218075, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_captain_b, 53412, 218077, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(vale_master_b, 54226, 218797, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(vale_master_b, 54394, 219067, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 54139, 219253, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(doll_blader_b, 54262, 219480, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				_ai4 = 6;
			}
			else if(_ai4 == 6)
			{
				addSpawn(pirates_zombie_b, 53412, 218077, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(vale_master_b, 54413, 217132, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(doll_blader_b, 54841, 217132, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(doll_blader_b, 55372, 217128, -3343, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(doll_blader_b, 55893, 217122, -3488, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_captain_b, 56282, 217237, -3216, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(vale_master_b, 56963, 218080, -3216, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 56267, 218826, -3216, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(doll_blader_b, 56294, 219482, -3216, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_captain_b, 56094, 219113, -3216, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(doll_blader_b, 56364, 218967, -3216, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(vale_master_b, 56276, 220783, -3216, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(vale_master_b, 57173, 220234, -3216, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 54885, 220144, -3216, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 55264, 219860, -3216, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_captain_b, 55399, 220263, -3216, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 55679, 220129, -3216, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_captain_b, 54236, 220948, -3216, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_captain_b, 54464, 219095, -3216, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(vale_master_b, 54226, 218797, -3216, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(vale_master_b, 54394, 219067, -3216, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 54139, 219253, -3216, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(doll_blader_b, 54262, 219480, -3216, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_captain_b, 53412, 218077, -3216, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 55440, 218081, -3216, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_captain_b, 55202, 217940, -3216, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 55225, 218236, -3216, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 54973, 218075, -3216, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				_ai4 = 7;
			}
			else if(_ai4 == 7)
			{
				addSpawn(pirates_zombie_b, 54228, 217504, -3216, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(vale_master_b, 54181, 217168, -3216, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(doll_blader_b, 54714, 217123, -3168, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(doll_blader_b, 55298, 217127, -3073, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(doll_blader_b, 55787, 217130, -2993, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_captain_b, 56284, 217216, -2944, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(vale_master_b, 56963, 218080, -2944, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 56267, 218826, -2944, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(doll_blader_b, 56294, 219482, -2944, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_captain_b, 56094, 219113, -2944, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(doll_blader_b, 56364, 218967, -2944, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(vale_master_b, 56276, 220783, -2944, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(vale_master_b, 57173, 220234, -2944, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 54885, 220144, -2944, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 55264, 219860, -2944, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_captain_b, 55399, 220263, -2944, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 55679, 220129, -2944, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_captain_b, 54236, 220948, -2944, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_captain_b, 54464, 219095, -2944, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(vale_master_b, 54226, 218797, -2944, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(vale_master_b, 54394, 219067, -2944, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 54139, 219253, -2944, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(doll_blader_b, 54262, 219480, -2944, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_captain_b, 53412, 218077, -2944, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_captain_b, 54280, 217200, -2944, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 55440, 218081, -2944, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_captain_b, 55202, 217940, -2944, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 55225, 218236, -2944, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				addSpawn(pirates_zombie_b, 54973, 218075, -2944, Rnd.get(65536), false, 0).setIsRaidMinion(true);
				_ai4 = 8;
				cancelQuestTimer("1003", null, null);
			}
		}
		else if(event.equalsIgnoreCase("zaken_unlock"))
		{
			L2GrandBossInstance zaken = (L2GrandBossInstance) addSpawn(ZAKEN, 55312, 219168, -3223, 0, false, 0);
			GrandBossManager.getInstance().setBossStatus(ZAKEN, ALIVE);
			spawnBoss(zaken);
		}
		else if(event.equalsIgnoreCase("CreateOnePrivateEx"))
		{
			addSpawn(npc.getNpcId(), npc.getX(), npc.getY(), npc.getZ(), 0, false, 0).setIsRaidMinion(true);
		}

		return super.onAdvEvent(event, npc, player);
	}

	@Override
	public String onFactionCall(L2Npc npc, L2Npc caller, L2PcInstance attacker, boolean isPet)
	{
		if(caller == null || npc == null)
		{
			return super.onFactionCall(npc, caller, attacker, isPet);
		}

		int npcId = npc.getNpcId();
		int callerId = caller.getNpcId();
		if(getTimeHour() < 5 && callerId != ZAKEN && npcId == ZAKEN)
		{
			int damage = 0;
			if(npc.getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE && _ai0 == 0 && damage < 10 && Rnd.get((30 * 15)) < 1)
			{
				_ai0 = 1;
				_ai1 = caller.getX();
				_ai2 = caller.getY();
				_ai3 = caller.getZ();
				startQuestTimer("1002", 300, caller, null);
			}
		}

		return super.onFactionCall(npc, caller, attacker, isPet);
	}

	@Override
	public String onSpellFinished(L2Npc npc, L2PcInstance player, L2Skill skill)
	{
		if(npc.getNpcId() == ZAKEN)
		{
			int skillId = skill.getId();
			if(skillId == 4222)
			{
				npc.teleToLocation(_ai1, _ai2, _ai3);
				npc.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			}
			else if(skillId == 4216)
			{
				int i1 = Rnd.get(15);
				player.teleToLocation(Xcoords[i1] + Rnd.get(650), Ycoords[i1] + Rnd.get(650), Zcoords[i1]);
				((L2Attackable) npc).stopHating(player);
				L2Character nextTarget = ((L2Attackable) npc).getMostHated();
				if(nextTarget != null)
				{
					npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, nextTarget);
				}
			}
			else if(skillId == 4217)
			{
				int i0 = 0;
				int i1 = Rnd.get(15);
				player.teleToLocation(Xcoords[i1] + Rnd.get(650), Ycoords[i1] + Rnd.get(650), Zcoords[i1]);
				((L2Attackable) npc).stopHating(player);
				if(c_quest0 != null && _quest0 > 0 && c_quest0 != player && c_quest0.getZ() > (player.getZ() - 100) && c_quest0.getZ() < (player.getZ() + 100))
				{
					if((((c_quest0.getX() - player.getX()) * (c_quest0.getX() - player.getX())) + ((c_quest0.getY() - player.getY()) * (c_quest0.getY() - player.getY()))) > (250 * 250))
					{
						i0 = 1;
					}
					else
					{
						i0 = 0;
					}

					if(i0 == 0)
					{
						i1 = Rnd.get(15);
						c_quest0.teleToLocation(Xcoords[i1] + Rnd.get(650), Ycoords[i1] + Rnd.get(650), Zcoords[i1]);
						((L2Attackable) npc).stopHating(c_quest0);
					}
				}

				if(c_quest1 != null && _quest0 > 1 && c_quest1 != player && c_quest1.getZ() > (player.getZ() - 100) && c_quest1.getZ() < (player.getZ() + 100))
				{
					if((((c_quest1.getX() - player.getX()) * (c_quest1.getX() - player.getX())) + ((c_quest1.getY() - player.getY()) * (c_quest1.getY() - player.getY()))) > (250 * 250))
					{
						i0 = 1;
					}
					else
					{
						i0 = 0;
					}

					if(i0 == 0)
					{
						i1 = Rnd.get(15);
						c_quest1.teleToLocation(Xcoords[i1] + Rnd.get(650), Ycoords[i1] + Rnd.get(650), Zcoords[i1]);
						((L2Attackable) npc).stopHating(c_quest1);
					}
				}

				if(c_quest2 != null && _quest0 > 2 && c_quest2 != player && c_quest2.getZ() > (player.getZ() - 100) && c_quest2.getZ() < (player.getZ() + 100))
				{
					if((((c_quest2.getX() - player.getX()) * (c_quest2.getX() - player.getX())) + ((c_quest2.getY() - player.getY()) * (c_quest2.getY() - player.getY()))) > (250 * 250))
					{
						i0 = 1;
					}
					else
					{
						i0 = 0;
					}

					if(i0 == 0)
					{
						i1 = Rnd.get(15);
						c_quest2.teleToLocation(Xcoords[i1] + Rnd.get(650), Ycoords[i1] + Rnd.get(650), Zcoords[i1]);
						((L2Attackable) npc).stopHating(c_quest2);
					}
				}

				if(c_quest3 != null && _quest0 > 3 && c_quest3 != player && c_quest3.getZ() > (player.getZ() - 100) && c_quest3.getZ() < (player.getZ() + 100))
				{
					if((((c_quest3.getX() - player.getX()) * (c_quest3.getX() - player.getX())) + ((c_quest3.getY() - player.getY()) * (c_quest3.getY() - player.getY()))) > (250 * 250))
					{
						i0 = 1;
					}
					else
					{
						i0 = 0;
					}

					if(i0 == 0)
					{
						i1 = Rnd.get(15);
						c_quest3.teleToLocation(Xcoords[i1] + Rnd.get(650), Ycoords[i1] + Rnd.get(650), Zcoords[i1]);
						((L2Attackable) npc).stopHating(c_quest3);
					}
				}

				if(c_quest4 != null && _quest0 > 4 && c_quest4 != player && c_quest4.getZ() > (player.getZ() - 100) && c_quest4.getZ() < (player.getZ() + 100))
				{
					if((((c_quest4.getX() - player.getX()) * (c_quest4.getX() - player.getX())) + ((c_quest4.getY() - player.getY()) * (c_quest4.getY() - player.getY()))) > (250 * 250))
					{
						i0 = 1;
					}
					else
					{
						i0 = 0;
					}

					if(i0 == 0)
					{
						i1 = Rnd.get(15);
						c_quest4.teleToLocation(Xcoords[i1] + Rnd.get(650), Ycoords[i1] + Rnd.get(650), Zcoords[i1]);
						((L2Attackable) npc).stopHating(c_quest4);
					}
				}

				L2Character nextTarget = ((L2Attackable) npc).getMostHated();
				if(nextTarget != null)
				{
					npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, nextTarget);
				}
			}
		}

		return super.onSpellFinished(npc, player, skill);
	}

	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		int npcId = npc.getNpcId();
		if(npcId == ZAKEN)
		{
			if(attacker.getMountType() == 1)
			{
				int sk_4258 = 0;
				L2Effect[] effects = attacker.getAllEffects();
				if(effects.length != 0 || effects != null)
				{
					for(L2Effect e : effects)
					{
						if(e.getSkill().getId() == 4258)
						{
							sk_4258 = 1;
						}
					}
				}

				if(sk_4258 == 0)
				{
					npc.setTarget(attacker);
					npc.doCast(SkillTable.getInstance().getInfo(4258, 1));
				}
			}

			L2Character originalAttacker = isPet ? attacker.getPet() : attacker;
			int hate = (int) (((damage / npc.getMaxHp()) / 0.05) * 20000);
			((L2Attackable) npc).addDamageHate(originalAttacker, 0, hate);
			if(Rnd.get(10) < 1)
			{
				int i0 = Rnd.get((15 * 15));
				if(i0 < 1)
				{
					npc.setTarget(attacker);
					npc.doCast(SkillTable.getInstance().getInfo(4216, 1));
				}
				else if(i0 < 2)
				{
					npc.setTarget(attacker);
					npc.doCast(SkillTable.getInstance().getInfo(4217, 1));
				}
				else if(i0 < 4)
				{
					npc.setTarget(attacker);
					npc.doCast(SkillTable.getInstance().getInfo(4219, 1));
				}
				else if(i0 < 8)
				{
					npc.setTarget(attacker);
					npc.doCast(SkillTable.getInstance().getInfo(4218, 1));
				}
				else if(i0 < 15)
				{
					for(L2Character character : npc.getKnownList().getKnownCharactersInRadius(100))
					{
						if(character != attacker)
						{
							continue;
						}

						if(attacker != ((L2Attackable) npc).getMostHated())
						{
							npc.setTarget(attacker);
							npc.doCast(SkillTable.getInstance().getInfo(4221, 1));
						}
					}
				}

				if(Rnd.get(2) < 1)
				{
					if(attacker == ((L2Attackable) npc).getMostHated())
					{
						npc.setTarget(attacker);
						npc.doCast(SkillTable.getInstance().getInfo(4220, 1));
					}
				}
			}

			if(getTimeHour() < 5)
			{
			}
			else if(npc.getCurrentHp() < ((npc.getMaxHp() * _quest2) / 4.0))
			{
				_quest2 = (_quest2 - 1);
				int i2 = Rnd.get(15);
				_ai1 = Xcoords[i2] + Rnd.get(650);
				_ai2 = Ycoords[i2] + Rnd.get(650);
				_ai3 = Zcoords[i2];
				npc.setTarget(npc);
				npc.doCast(SkillTable.getInstance().getInfo(4222, 1));
			}
		}

		return super.onAttack(npc, attacker, damage, isPet);
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		int npcId = npc.getNpcId();
		if(npcId == ZAKEN)
		{
			if(!npc.getSpawn().is_customBossInstance())
			{
				npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
				GrandBossManager.getInstance().setBossStatus(ZAKEN, DEAD);
				long respawnTime = (Config.ZAKEN_RESP_FIRST + Rnd.get(Config.ZAKEN_RESP_SECOND)) * 3600000;
				startQuestTimer("zaken_unlock", respawnTime, null, null);
				cancelQuestTimer("1001", npc, null);
				cancelQuestTimer("1003", npc, null);
				StatsSet info = GrandBossManager.getInstance().getStatsSet(ZAKEN);
				info.set("respawn_time", System.currentTimeMillis() + respawnTime);
				GrandBossManager.getInstance().setStatsSet(ZAKEN, info);
			}
		}
		else if(GrandBossManager.getInstance().getBossStatus(ZAKEN) == ALIVE)
		{
			if(npcId != ZAKEN)
			{
				startQuestTimer("CreateOnePrivateEx", ((30 + Rnd.get(60)) * 1000), npc, null);
			}
		}

		return super.onKill(npc, killer, isPet);
	}

	@Override
	public String onSkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet)
	{
		int npcId = npc.getNpcId();
		if(npcId == ZAKEN)
		{
			if(skill.getAggroPoints() > 0)
			{
				((L2Attackable) npc).addDamageHate(caster, 0, (((skill.getAggroPoints() / npc.getMaxHp()) * 10) * 150));
			}

			if(Rnd.get(12) < 1)
			{
				int i0 = Rnd.get((15 * 15));
				if(i0 < 1)
				{
					npc.setTarget(caster);
					npc.doCast(SkillTable.getInstance().getInfo(4216, 1));
				}
				else if(i0 < 2)
				{
					npc.setTarget(caster);
					npc.doCast(SkillTable.getInstance().getInfo(4217, 1));
				}
				else if(i0 < 4)
				{
					npc.setTarget(caster);
					npc.doCast(SkillTable.getInstance().getInfo(4219, 1));
				}
				else if(i0 < 8)
				{
					npc.setTarget(caster);
					npc.doCast(SkillTable.getInstance().getInfo(4218, 1));
				}
				else if(i0 < 15)
				{
					for(L2Character character : npc.getKnownList().getKnownCharactersInRadius(100))
					{
						if(character != caster)
						{
							continue;
						}

						if(caster != ((L2Attackable) npc).getMostHated())
						{
							npc.setTarget(caster);
							npc.doCast(SkillTable.getInstance().getInfo(4221, 1));
						}
					}
				}

				if(Rnd.get(2) < 1)
				{
					if(caster == ((L2Attackable) npc).getMostHated())
					{
						npc.setTarget(caster);
						npc.doCast(SkillTable.getInstance().getInfo(4220, 1));
					}
				}
			}
		}
		return super.onSkillSee(npc, caster, skill, targets, isPet);
	}

	@Override
	public String onAggroRangeEnter(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		int npcId = npc.getNpcId();
		if(npcId == ZAKEN)
		{
			if(_zone.isInsideZone(npc))
			{
				L2Character target = isPet ? player.getPet() : player;
				((L2Attackable) npc).addDamageHate(target, 1, 200);
			}

			if(player.getZ() > (npc.getZ() - 100) && player.getZ() < (npc.getZ() + 100))
			{
				if(_quest0 < 5 && Rnd.get(3) < 1)
				{
					if(_quest0 == 0)
					{
						c_quest0 = player;
					}
					else if(_quest0 == 1)
					{
						c_quest1 = player;
					}
					else if(_quest0 == 2)
					{
						c_quest2 = player;
					}
					else if(_quest0 == 3)
					{
						c_quest3 = player;
					}
					else if(_quest0 == 4)
					{
						c_quest4 = player;
					}
					_quest0++;
				}

				if(Rnd.get(15) < 1)
				{
					int i0 = Rnd.get((15 * 15));
					if(i0 < 1)
					{
						npc.setTarget(player);
						npc.doCast(SkillTable.getInstance().getInfo(4216, 1));
					}
					else if(i0 < 2)
					{
						npc.setTarget(player);
						npc.doCast(SkillTable.getInstance().getInfo(4217, 1));
					}
					else if(i0 < 4)
					{
						npc.setTarget(player);
						npc.doCast(SkillTable.getInstance().getInfo(4219, 1));
					}
					else if(i0 < 8)
					{
						npc.setTarget(player);
						npc.doCast(SkillTable.getInstance().getInfo(4218, 1));
					}
					else if(i0 < 15)
					{
						for(L2Character character : npc.getKnownList().getKnownCharactersInRadius(100))
						{
							if(character != player)
							{
								continue;
							}

							if(player != ((L2Attackable) npc).getMostHated())
							{
								npc.setTarget(player);
								npc.doCast(SkillTable.getInstance().getInfo(4221, 1));
							}
						}
					}

					if(Rnd.get(2) < 1)
					{
						if(player == ((L2Attackable) npc).getMostHated())
						{
							npc.setTarget(player);
							npc.doCast(SkillTable.getInstance().getInfo(4220, 1));
						}
					}
				}
			}
		}

		return super.onAggroRangeEnter(npc, player, isPet);
	}

	public int getTimeHour()
	{
		return (GameTimeController.getInstance().getGameTime() / 60) % 24;
	}

}