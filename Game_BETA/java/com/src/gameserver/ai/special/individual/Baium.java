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

import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_FOLLOW;
import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;

import java.util.Collection;

import javolution.util.FastList;

import com.src.Config;
import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.geo.GeoData;
import com.src.gameserver.managers.GrandBossManager;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.instance.L2GrandBossInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.itemcontainer.Inventory;
import com.src.gameserver.model.quest.Quest;
import com.src.gameserver.model.quest.QuestTimer;
import com.src.gameserver.model.quest.State;
import com.src.gameserver.model.zone.type.L2BossZone;
import com.src.gameserver.network.clientpackets.Say2;
import com.src.gameserver.network.serverpackets.CreatureSay;
import com.src.gameserver.network.serverpackets.FlyToLocation;
import com.src.gameserver.network.serverpackets.PlaySound;
import com.src.gameserver.network.serverpackets.SocialAction;
import com.src.gameserver.templates.StatsSet;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.gameserver.util.Util;
import com.src.util.random.Rnd;

public class Baium extends Quest
{
	private L2Character _target;
	private L2Skill _skill;
	private L2PcInstance _waker;
	private static final int STONE_BAIUM = 29025;
	private static final int ANGELIC_VORTEX = 31862;
	private static final int LIVE_BAIUM = 29020;
	private static final int ARCHANGEL = 29021;
	private static final int BLOODED_FABRIC = 4295;
	private static FastList<L2Attackable> _Minions = new FastList<L2Attackable>();
	private final static int ANGEL_LOCATION[][] =
	{
		{ 114768, 16569, 10200, 0 },
		{ 115141, 16187, 10200, 0 },
		{ 115606, 16631, 10200, 0 },
		{ 115228, 17014, 10200, 0 },
		{ 114792, 16368, 10200, 0 }
	};

	private static final byte ASLEEP = 0;
	private static final byte AWAKE = 1;
	private static final byte DEAD = 2;

	private static long _LastAttackVsBaiumTime = 0;
	private static L2BossZone _Zone;

	public Baium(int questId, String name, String descr)
	{
		super(questId, name, descr);

		setInitialState(new State("Start", this));
		
		addEventId(LIVE_BAIUM, Quest.QuestEventType.ON_KILL);
		addEventId(LIVE_BAIUM, Quest.QuestEventType.ON_ATTACK);
		addEventId(LIVE_BAIUM, Quest.QuestEventType.ON_SPELL_FINISHED);
		this.addEventId(ARCHANGEL, Quest.QuestEventType.ON_AGGRO_RANGE_ENTER);
		this.addEventId(ARCHANGEL, Quest.QuestEventType.ON_ATTACK);
		addEventId(ANGELIC_VORTEX, Quest.QuestEventType.QUEST_START);
		addEventId(ANGELIC_VORTEX, Quest.QuestEventType.QUEST_TALK);
		addEventId(STONE_BAIUM, Quest.QuestEventType.QUEST_TALK);

		addStartNpc(STONE_BAIUM);
		addStartNpc(ANGELIC_VORTEX);
		addTalkId(STONE_BAIUM);
		addTalkId(ANGELIC_VORTEX);
		_Zone = GrandBossManager.getInstance().getZone(113100, 14500, 10077);
		StatsSet info = GrandBossManager.getInstance().getStatsSet(LIVE_BAIUM);
		int status = GrandBossManager.getInstance().getBossStatus(LIVE_BAIUM);
		if(status == DEAD)
		{
			long temp = info.getLong("respawn_time") - System.currentTimeMillis();
			if(temp > 0)
			{
				startQuestTimer("baium_unlock", temp, null, null);
			}
			else
			{
				addSpawn(STONE_BAIUM, 115996, 17417, 10106, 41740, false, 0);
				GrandBossManager.getInstance().setBossStatus(LIVE_BAIUM, ASLEEP);
			}
		}
		else if(status == AWAKE)
		{
			int loc_x = info.getInteger("loc_x");
			int loc_y = info.getInteger("loc_y");
			int loc_z = info.getInteger("loc_z");
			int heading = info.getInteger("heading");
			final int hp = info.getInteger("currentHP");
			final int mp = info.getInteger("currentMP");
			L2GrandBossInstance baium = (L2GrandBossInstance) addSpawn(LIVE_BAIUM, loc_x, loc_y, loc_z, heading, false, 0);
			GrandBossManager.getInstance().addBoss(baium);
			final L2Npc _baium = baium;
			ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						_baium.setCurrentHpMp(hp,mp);
						_baium.setIsInvul(true);
						_baium.setIsImobilised(true);
						_baium.setRunning();
						_baium.broadcastPacket(new SocialAction(_baium.getObjectId(),2));
						startQuestTimer("baium_wakeup",1500, _baium, null);
						
						// Angels AI
						startQuestTimer("angels_aggro_reconsider", 1000, null, null, true);
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
			}, 100L);
		}
		else
		{
			addSpawn(STONE_BAIUM, 115996, 17417, 10106, 41740, false, 0);
		}
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		if(event.equalsIgnoreCase("baium_unlock"))
		{
			GrandBossManager.getInstance().setBossStatus(LIVE_BAIUM, ASLEEP);
			addSpawn(STONE_BAIUM, 115996, 17417, 10106, 41740, false, 0);
		}
		else if(event.equalsIgnoreCase("skill_range") && npc != null)
		{
			callSkillAI(npc);
			startQuestTimer("skill_range", 500, npc, null);
		}
		else if(event.equalsIgnoreCase("clean_player"))
		{
			_target = getRandomTarget(npc);
		}
		else if(event.equalsIgnoreCase("baium_wakeup") && npc != null)
		{
			if(npc.getNpcId() == LIVE_BAIUM)
			{
				if(_waker != null)
				{
					npc.broadcastPacket(new CreatureSay(npc.getObjectId(), Say2.ALL, npc.getName(), ""+ _waker.getName() + ", Dares to awaken by noise me! Dies"));
				}
				npc.broadcastPacket(new SocialAction(npc.getObjectId(), 1));
				if(_waker != null)
				{
					doThrow(_waker, npc);
				}

				for(int i = 0; i < 5; i++)
				{
					_Minions.add((L2Attackable) addSpawn(ARCHANGEL, ANGEL_LOCATION[i][0], ANGEL_LOCATION[i][1], ANGEL_LOCATION[i][2], ANGEL_LOCATION[i][3], false, 0));
					npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, null);
					npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, null);
					L2Character target = player;
					((L2Attackable) npc).addDamageHate(target, 0, 999);
					npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
				}
				startQuestTimer("angels_aggro_reconsider", 1000, null, null, true);
				_LastAttackVsBaiumTime = System.currentTimeMillis();
				
				if (!npc.getSpawn().is_customBossInstance())
				{
					startQuestTimer("baium_despawn", 60000, npc, null);
				}
				startQuestTimer("skill_range", 500, npc, null);
				final L2Npc baium = npc;
				ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
				{
					@Override
					public void run()
					{
						try
						{
							baium.setIsInvul(false);
							baium.setIsImobilised(false);
						}
						catch(Exception e)
						{
							e.printStackTrace();
						}
					}
				}, 2700L);
			}
		}
		else if(event.equalsIgnoreCase("baium_despawn") && npc != null)
		{
			if(npc.getNpcId() == LIVE_BAIUM)
			{
				startQuestTimer("baium_despawn", 60000, npc, null);
				if(_Zone == null)
				{
					_Zone = GrandBossManager.getInstance().getZone(113100, 14500, 10077);
				}

				if(_LastAttackVsBaiumTime + Config.BAIUM_SLEEP * 1000 < System.currentTimeMillis())
				{
					npc.deleteMe();
					addSpawn(STONE_BAIUM, 115996, 17417, 10106, 41740, false, 0);
					GrandBossManager.getInstance().setBossStatus(LIVE_BAIUM, ASLEEP);
					_Zone.oustAllPlayers();
					cancelQuestTimer("baium_despawn", npc, null);
					for(L2Attackable minion : _Minions)
					{
						if(minion != null)
						{
							minion.deleteMe();
						}
					}
					_Minions.clear();
				}
				else if((_LastAttackVsBaiumTime + 300000 < System.currentTimeMillis()) && npc.getCurrentHp() < ((npc.getMaxHp() * 3) / 4.0))
				{
					npc.setTarget(npc);
					npc.doCast(SkillTable.getInstance().getInfo(4135, 1));
					npc.setIsCastingNow(true);
				}
				else if(!_Zone.isInsideZone(npc))
				{
					npc.teleToLocation(116033, 17447, 10104);
				}
			}
		}
		else if (event.equalsIgnoreCase("angels_aggro_reconsider"))
		{
			boolean updateTarget = false; // Update or no the target
			
			for (L2Npc minion : _Minions)
			{
				L2Attackable angel = ((L2Attackable) minion);
				if (angel == null)
					continue;
				
				L2Character victim = angel.getMostHated();
				
				if (Rnd.get(100) == 0) // Chaos time
					updateTarget = true;
				else
				{
					if (victim != null) // Target is a unarmed player ; clean aggro.
					{
						if (victim instanceof L2PcInstance && victim.getActiveWeaponInstance() == null)
						{
							angel.stopHating(victim); // Clean the aggro number of previous victim.
							updateTarget = true;
						}
					}
					else
						// No target currently.
						updateTarget = true;
				}
				
				if (updateTarget)
				{
					L2Character newVictim = getRandomTarget(minion);
					if (newVictim != null && victim != newVictim)
					{
						angel.addDamageHate(newVictim, 0, 10000);
						
						// Set the intention to the L2Attackable to AI_INTENTION_ACTIVE
						if (angel.getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
							angel.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
					}
				}
			}
		}
		return super.onAdvEvent(event, npc, player);
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		int status = GrandBossManager.getInstance().getBossStatus(LIVE_BAIUM);
		int npcId = npc.getNpcId();
		String htmltext = "";
		if(_Zone == null)
		{
			_Zone = GrandBossManager.getInstance().getZone(113100, 14500, 10077);
		}

		if(_Zone == null)
		{
			return "<html><body>Angelic Vortex:<br>You may not enter while admin disabled this zone</body></html>";
		}

		if(npcId == STONE_BAIUM && GrandBossManager.getInstance().getBossStatus(LIVE_BAIUM) == ASLEEP)
		{
			if(_Zone.isPlayerAllowed(player))
			{
				GrandBossManager.getInstance().setBossStatus(LIVE_BAIUM, AWAKE);
				npc.deleteMe();
				L2GrandBossInstance baium = (L2GrandBossInstance) addSpawn(LIVE_BAIUM, npc);
				GrandBossManager.getInstance().addBoss(baium);
				//player.reduceCurrentHp(player.getCurrentHp(), player);
				final L2Npc _baium = baium;
				ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
				{
					@Override
					public void run()
					{
						try
						{
							_baium.setIsInvul(true);
							_baium.setIsImobilised(true);
							_baium.setRunning();
							_baium.broadcastPacket(new SocialAction(_baium.getObjectId(), 2));
							startQuestTimer("baium_wakeup", 15000, _baium, null);
						}
						catch(Throwable e)
						{
						}
					}
				}, 100L);
				_waker = player;
			}
			else
			{
				htmltext = "Conditions are not right to wake up Baium";
			}
		}
		else if(npcId == ANGELIC_VORTEX)
		{
			if(status == DEAD)
			{
				htmltext = "<html><body><tr><td>You hear something...</td></tr><br><font color=LEVEL>Baium was killed...</font><br>Try another time.</body></html>";
			}

			if(status == ASLEEP)
			{
				if(player.isFlying())
				{
					htmltext = "<html><body>Angelic Vortex:<br>You may not enter while flying a wyvern</body></html>";
				}
				else if(player.getQuestState("baium").getQuestItemsCount(BLOODED_FABRIC) > 0)
				{
					player.getQuestState("baium").takeItems(BLOODED_FABRIC, 1);
					_Zone.allowPlayerEntry(player, 30);
					player.teleToLocation(113100, 14500, 10077);
				}
				else
				{
					htmltext = "<html><body><tr><td>You hear something...</td></tr><br>You need <font color=LEVEL>Blooded Fabric</font> to enter...</body></html>";
				}
			}
			else
			{
				htmltext = "<html><body><tr><td>You hear something...</td></tr><br><font color=LEVEL>Baium is under attack...</font><br>Try another time.</body></html>";
			}
		}

		return htmltext;
	}

	public void doThrow(L2PcInstance player, L2Npc _baium)
	{
		player.setIsImobilised(true);
		try
		{
			Thread.sleep(2000); // Wait 2 seconds then continue
			int[] coord = { 113065, 14487, 10077 };
			_Zone.broadcastPacket(new FlyToLocation(player, coord[0], coord[1], coord[2], FlyToLocation.FlyType.THROW_HORIZONTAL));
			player.reduceCurrentHp(player.getCurrentHp(), player);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			player.setIsImobilised(false);
		}
	}

	@Override
	public String onSpellFinished(L2Npc npc, L2PcInstance player, L2Skill skill)
	{
		if(npc.isInvul())
		{
			npc.getAI().setIntention(AI_INTENTION_IDLE);
			return null;
		}
		else if(npc.getNpcId() == LIVE_BAIUM && !npc.isInvul())
		{
			callSkillAI(npc);
		}

		return super.onSpellFinished(npc, player, skill);
	}

	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		if(!_Zone.isInsideZone(attacker))
		{
			attacker.reduceCurrentHp(attacker.getCurrentHp(), attacker);
			return super.onAttack(npc, attacker, damage, isPet);
		}

		int npcId = npc.getNpcId();
		if(npcId == ARCHANGEL)
		{
			npc.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null, null);
			return super.onAttack(npc, attacker, damage, isPet);
		}

		if(npc.isInvul())
		{
			npc.getAI().setIntention(AI_INTENTION_IDLE);
			return super.onAttack(npc, attacker, damage, isPet);
		}
		else if(npc.getNpcId() == LIVE_BAIUM && !npc.isInvul())
		{
			if(attacker.getMountType() == 1)
			{
				if(attacker.getFirstEffect(4258) == null)
				{
					npc.setTarget(attacker);
					npc.doCast(SkillTable.getInstance().getInfo(4258, 1));
				}
			}

			L2ItemInstance itm = attacker.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			if(itm != null && itm.getItemId() == 4665)
			{
				npc.broadcastPacket(new CreatureSay(npc.getObjectId(), Say2.ALL, npc.getName(),"Who dares to try and steal my noble blood?"));
			}
			_LastAttackVsBaiumTime = System.currentTimeMillis();
			callSkillAI(npc);
			if(_target != null)
			{
				for(L2Attackable angel: _Minions)
				{
					angel.addDamageHate(_target, 0, 1);
				}
			}
		}

		return super.onAttack(npc, attacker, damage, isPet);
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		if(!npc.getSpawn().is_customBossInstance())
		{
			cancelQuestTimer("baium_despawn", npc, null);
			npc.broadcastPacket(new PlaySound(1, "BS01_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
			addSpawn(29055, 115203, 16620, 10078, 0, false, 900000);
			long respawnTime = (Config.BAIUM_RESP_FIRST + Rnd.get(Config.BAIUM_RESP_SECOND)) * 3600000;
			GrandBossManager.getInstance().setBossStatus(LIVE_BAIUM, DEAD);
			startQuestTimer("baium_unlock", respawnTime, null, null);
			StatsSet info = GrandBossManager.getInstance().getStatsSet(LIVE_BAIUM);
			info.set("respawn_time", System.currentTimeMillis() + respawnTime);
			GrandBossManager.getInstance().setStatsSet(LIVE_BAIUM, info);
		}
		if(getQuestTimer("skill_range", npc, null) != null)
		{
			getQuestTimer("skill_range", npc, null).cancel();
		}
		for(L2Attackable minion : _Minions)
		{
			if(minion != null)
			{
				minion.deleteMe();
				_Minions.clear();
			}
		}

		return super.onKill(npc, killer, isPet);
	}

	public L2Character getRandomTarget(L2Npc npc)
	{
		FastList<L2Character> result = new FastList<L2Character>();
		Collection<L2Object> objs = npc.getKnownList().getKnownObjects().values();
		{
			for(L2Object obj : objs)
			{
				if(obj instanceof L2Character)
				{
					if(((L2Character) obj).getZ() < npc.getZ() - 100 && ((L2Character) obj).getZ() > npc.getZ() + 100 || !GeoData.getInstance().canSeeTarget(obj, npc))
					{
						continue;
					}
				}

				if(obj instanceof L2PcInstance)
				{
					if(Util.checkIfInRange(9000, npc, obj, true) && !((L2Character) obj).isDead())
					{
						result.add((L2PcInstance) obj);
					}
				}

				if(obj instanceof L2Summon)
				{
					if(Util.checkIfInRange(9000, npc, obj, true) && !((L2Character) obj).isDead())
					{
						result.add((L2Summon) obj);
					}
				}
			}
		}

		if(!result.isEmpty() && result.size() != 0)
		{
			Object[] characters = result.toArray();
			QuestTimer timer = getQuestTimer("clean_player", npc, null);
			if(timer != null)
			{
				timer.cancel();
			}
			startQuestTimer("clean_player", 20000, npc, null);
			return (L2Character) characters[Rnd.get(characters.length)];
		}

		return null;
	}

	public synchronized void callSkillAI(L2Npc npc)
	{
		if(npc.isInvul() || npc.isCastingNow())
		{
			return;
		}

		if(_target == null || _target.isDead() || !_Zone.isInsideZone(_target))
		{
			_target = getRandomTarget(npc);
			_skill = getRandomSkill(npc);
		}

		L2Character target = _target;
		L2Skill skill = _skill;
		if(target == null || target.isDead() || !_Zone.isInsideZone(target))
		{
			return;
		}

		if(Util.checkIfInRange(skill.getCastRange(), npc, target, true))
		{
			npc.getAI().setIntention(AI_INTENTION_IDLE);
			npc.setTarget(target);
			_target = null;
			npc.doCast(skill);
		}
		else
		{
			npc.getAI().setIntention(AI_INTENTION_FOLLOW, target, null);
		}
	}

	public L2Skill getRandomSkill(L2Npc npc)
	{
		L2Skill skill;
		if(npc.getCurrentHp() > npc.getMaxHp() * 3 / 4)
		{
			if(Rnd.get(100) < 10)
			{
				skill = SkillTable.getInstance().getInfo(4128, 1);
			}
			else if(Rnd.get(100) < 10)
			{
				skill = SkillTable.getInstance().getInfo(4129, 1);
			}
			else
			{
				skill = SkillTable.getInstance().getInfo(4127, 1);
			}
		}
		else if(npc.getCurrentHp() > npc.getMaxHp() * 2 / 4)
		{
			if(Rnd.get(100) < 10)
			{
				skill = SkillTable.getInstance().getInfo(4131, 1);
			}
			else if(Rnd.get(100) < 10)
			{
				skill = SkillTable.getInstance().getInfo(4128, 1);
			}
			else if(Rnd.get(100) < 10)
			{
				skill = SkillTable.getInstance().getInfo(4129, 1);
			}
			else
			{
				skill = SkillTable.getInstance().getInfo(4127, 1);
			}
		}
		else if(npc.getCurrentHp() > npc.getMaxHp() * 1 / 4)
		{
			if(Rnd.get(100) < 10)
			{
				skill = SkillTable.getInstance().getInfo(4130, 1);
			}
			else if(Rnd.get(100) < 10)
			{
				skill = SkillTable.getInstance().getInfo(4131, 1);
			}
			else if(Rnd.get(100) < 10)
			{
				skill = SkillTable.getInstance().getInfo(4128, 1);
			}
			else if(Rnd.get(100) < 10)
			{
				skill = SkillTable.getInstance().getInfo(4129, 1);
			}
			else
			{
				skill = SkillTable.getInstance().getInfo(4127, 1);
			}
		}
		else if(Rnd.get(100) < 10)
		{
			skill = SkillTable.getInstance().getInfo(4130, 1);
		}
		else if(Rnd.get(100) < 10)
		{
			skill = SkillTable.getInstance().getInfo(4131, 1);
		}
		else if(Rnd.get(100) < 10)
		{
			skill = SkillTable.getInstance().getInfo(4128, 1);
		}
		else if(Rnd.get(100) < 10)
		{
			skill = SkillTable.getInstance().getInfo(4129, 1);
		}
		else
		{
			skill = SkillTable.getInstance().getInfo(4127, 1);
		}

		return skill;
	}

	@Override
	public String onSkillUse(L2Npc npc, L2PcInstance caster, L2Skill skill)
	{
		if(npc.isInvul())
		{
			npc.getAI().setIntention(AI_INTENTION_IDLE);
			return null;
		}

		npc.setTarget(caster);
		return super.onSkillUse(npc, caster, skill);
	}

}