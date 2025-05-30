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
package com.src.gameserver.model.quest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.cache.HtmCache;
import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.managers.QuestManager;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Party;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ConfirmDlg;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.scripting.ManagedScript;
import com.src.gameserver.scripting.ScriptManager;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.ResourceUtil;
import com.src.util.Util;
import com.src.util.database.L2DatabaseFactory;
import com.src.util.random.Rnd;

public class Quest extends ManagedScript
{
	protected static final Log _log = LogFactory.getLog(Quest.class);

	private static Map<String, Quest> _allEventsS = new FastMap<String, Quest>();
	private Map<String, FastList<QuestTimer>> _allEventTimers = new FastMap<String, FastList<QuestTimer>>();
	private final ReentrantReadWriteLock _rwLock = new ReentrantReadWriteLock();

	private final int _questId;
	private final String _name;
	private final String _prefixPath;
	private final String _descr;
	private State _initialState;
	private Map<String, State> _states;
	private FastList<Integer> _questItemIds;

	private static final String DEFAULT_NO_QUEST_MSG = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
	private static final String DEFAULT_ALREADY_COMPLETED_MSG = "<html><body>This quest has already been completed.</body></html>";

	public static Collection<Quest> findAllEvents()
	{
		return _allEventsS.values();
	}

	public Quest(int questId, String name, String descr)
	{
		_questId = questId;
		_name = name;
		_descr = descr;
		_states = new FastMap<String, State>();

		StringBuffer temp = new StringBuffer(getClass().getCanonicalName());
		temp.delete(0, temp.indexOf(".scripts.") + 9);
		temp.delete(temp.indexOf(getClass().getSimpleName()), temp.length());
		_prefixPath = temp.toString();

		if(questId != 0)
		{
			QuestManager.getInstance().addQuest(Quest.this);
		}
		else
		{
			_allEventsS.put(name, this);
		}

		init_LoadGlobalData();
	}

	protected void init_LoadGlobalData()
	{
		
	}

	public void saveGlobalData()
	{
		
	}

	public static enum QuestEventType
	{
		NPC_FIRST_TALK(false), // control the first dialog shown by NPCs when they are clicked (some quests must override the default npc action)
		QUEST_START(true), // onTalk action from start npcs
		QUEST_TALK(true), // onTalk action from npcs participating in a quest
		ON_FACTION_CALL(true), // NPC or Mob saw a person casting a skill (regardless what the target is).
		ON_SPELL_FINISHED(true), // on spell finished action when npc finish casting skill
		ON_AGGRO_RANGE_ENTER(true), // a person came within the Npc/Mob's range
		ON_SPAWN(true), // onSpawn action triggered when an NPC is spawned or respawned.
		ON_KILL(true), // onKill action triggered when a mob gets killed.
		ON_SKILL_SEE(true), // NPC or Mob saw a person casting a skill (regardless what the target is).
		ON_ATTACK(true), // onAttack action triggered when a mob gets attacked by someone
		ON_ATTACK_ACT(true),
		ON_ENTER_ZONE(true), // on zone enter
		ON_EXIT_ZONE(true), // on zone exit
		ON_SKILL_USE(true),

		@Deprecated
		MOBGOTATTACKED(true),
		@Deprecated
		MOBKILLED(true),
		@Deprecated
		MOB_TARGETED_BY_SKILL(true);

		// control whether this event type is allowed for the same npc template in multiple quests
		// or if the npc must be registered in at most one quest for the specified event
		private boolean _allowMultipleRegistration;

		QuestEventType(boolean allowMultipleRegistration)
		{
			_allowMultipleRegistration = allowMultipleRegistration;
		}

		public boolean isMultipleRegistrationAllowed()
		{
			return _allowMultipleRegistration;
		}

	}

	/**
	 * Return ID of the quest
	 * @return int
	 */
	public int getQuestIntId()
	{
		return _questId;
	}

	public void setInitialState(State state)
	{
		_initialState = state;
	}

	/**
	 * Add a new QuestState to the database and return it.
	 * @param player
	 * @return QuestState : QuestState created
	 */
	public QuestState newQuestState(L2PcInstance player)
	{
		if(getInitialState() == null)
			_initialState = new State("Start", this);

		QuestState qs = new QuestState(this, player, getInitialState(), false);
		Quest.createQuestInDb(qs);
		return qs;
	}

	/**
	 * Return initial state of the quest
	 * @return State
	 */
	public State getInitialState()
	{
		return _initialState;
	}

	/**
	 * Return name of the quest
	 * @return String
	 */
	public String getName()
	{
		return _name;
	}

	public String getPrefixPath()
	{
		return _prefixPath;
	}

	/**
	 * Return description of the quest
	 * @return String
	 */
	public String getDescr()
	{
		return _descr;
	}

	public State addState(State state)
	{
		_states.put(state.getName(), state);
		return state;
	}

	/**
	 * Add a timer to the quest, if it doesn't exist already
	 * @param name: name of the timer (also passed back as "event" in onAdvEvent)
	 * @param time: time in ms for when to fire the timer
	 * @param npc:  npc associated with this timer (can be null)
	 * @param player: player associated with this timer (can be null)
	 */
	public void startQuestTimer(String name, long time, L2Npc npc, L2PcInstance player)
	{
		startQuestTimer(name, time, npc, player, false);
	}

	/**
	 * Add a timer to the quest, if it doesn't exist already.  If the timer is repeatable,
	 * it will auto-fire automatically, at a fixed rate, until explicitly canceled.
	 * @param name: name of the timer (also passed back as "event" in onAdvEvent)
	 * @param time: time in ms for when to fire the timer
	 * @param npc:  npc associated with this timer (can be null)
	 * @param player: player associated with this timer (can be null)
	 * @param repeatable: indicates if the timer is repeatable or one-time.
	 */
	public void startQuestTimer(String name, long time, L2Npc npc, L2PcInstance player, boolean repeating)
	{
		// Add quest timer if timer doesn't already exist
		FastList<QuestTimer> timers = getQuestTimers(name);

		if(timers == null)
		{
			timers = new FastList<QuestTimer>();
			timers.add(new QuestTimer(this, name, time, npc, player, repeating));
			_allEventTimers.put(name, timers);
		}
		else
		{
			if(getQuestTimer(name, npc, player) == null)
			{
				timers.add(new QuestTimer(this, name, time, npc, player, repeating));
			}
		}
	}

	public QuestTimer getQuestTimer(String name, L2Npc npc, L2PcInstance player)
	{
		FastList<QuestTimer> qt = _allEventTimers.get(name);

		if(qt == null || qt.isEmpty())
		{
			return null;
		}

		for(QuestTimer timer : qt)
		{
			if(timer != null)
			{
				if(timer.isMatch(this, name, npc, player))
				{
					return timer;
				}
			}
		}

		return null;
	}

	public FastList<QuestTimer> getQuestTimers(String name)
	{
		return _allEventTimers.get(name);
	}

	public void cancelQuestTimers(String name)
	{
		FastList<QuestTimer> timers = getQuestTimers(name);
		if(timers == null)
		{
			return;
		}
		try
		{
			_rwLock.writeLock().lock();
			for(QuestTimer timer : timers)
			{
				if(timer != null)
				{
					timer.cancel();
				}
			}
		}
		finally
		{
			_rwLock.writeLock().unlock();
		}
	}

	public void cancelQuestTimer(String name, L2Npc npc, L2PcInstance player)
	{
		QuestTimer timer = getQuestTimer(name, npc, player);

		if(timer != null)
		{
			timer.cancel();
		}
	}

	public void removeQuestTimer(QuestTimer timer)
	{
		if(timer == null)
		{
			return;
		}

		FastList<QuestTimer> timers = getQuestTimers(timer.getName());

		if(timers == null)
		{
			return;
		}

		timers.remove(timer);
	}

	public final boolean notifyAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		String res = null;

		try
		{
			res = onAttack(npc, attacker, damage, isPet);
		}
		catch(Exception e)
		{
			return showError(attacker, e);
		}

		return showResult(attacker, res);
	}

	public final boolean notifyDeath(L2Character killer, L2Character victim, QuestState qs)
	{
		String res = null;

		try
		{
			res = onDeath(killer, victim, qs);
		}
		catch(Exception e)
		{
			return showError(qs.getPlayer(), e);
		}

		return showResult(qs.getPlayer(), res);
	}

	public final boolean notifyEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String res = null;

		try
		{
			res = onAdvEvent(event, npc, player);
		}
		catch(Exception e)
		{
			return showError(player, e);
		}

		return showResult(player, res);
	}

	public final boolean notifyKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		String res = null;

		try
		{
			res = onKill(npc, killer, isPet);
		}
		catch(Exception e)
		{
			return showError(killer, e);
		}

		return showResult(killer, res);
	}

	public final boolean notifySkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet)
	{
		ThreadPoolManager.getInstance().executeAi(new tmpOnSkillSee(npc, caster, skill, targets, isPet));
		return true;
	}

	public final boolean notifyTalk(L2Npc npc, QuestState qs)
	{
		String res = null;

		try
		{
			res = onTalk(npc, qs.getPlayer());
		}
		catch(Exception e)
		{
			return showError(qs.getPlayer(), e);
		}

		qs.getPlayer().setLastQuestNpcObject(npc.getObjectId());

		return showResult(qs.getPlayer(), res);
	}

	public final boolean notifyFirstTalk(L2Npc npc, L2PcInstance player)
	{
		String res = null;

		try
		{
			res = onFirstTalk(npc, player);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return showError(player, e);
		}

		player.setLastQuestNpcObject(npc.getObjectId());

		if(res != null && res.length() > 0)
		{
			return showResult(player, res);
		}

		npc.showChatWindow(player);

		return true;
	}

	public final boolean notifySkillUse(L2Npc npc, L2PcInstance caster, L2Skill skill)
	{
		String res = null;

		try
		{
			res = onSkillUse(npc, caster, skill);
		}
		catch(Exception e)
		{
			return showError(caster, e);
		}

		return showResult(caster, res);
	}

	public final boolean notifySpellFinished(L2Npc npc, L2PcInstance player, L2Skill skill)
	{
		String res = null;
		try
		{
			res = onSpellFinished(npc, player, skill);
		}
		catch(Exception e)
		{
			return showError(player, e);
		}

		return showResult(player, res);
	}

	public final boolean notifyFactionCall(L2Npc npc, L2Npc caller, L2PcInstance attacker, boolean isPet)
	{
		String res = null;
		try
		{
			res = onFactionCall(npc, caller, attacker, isPet);
		}
		catch(Exception e)
		{
			return showError(attacker, e);
		}

		return showResult(attacker, res);
	}

	public final boolean notifyAttackAct(L2Npc npc, L2PcInstance victim)
	{
		String res = null;
		try
		{
			res = onAttackAct(npc, victim);
		}
		catch (Exception e)
		{
			return showError(victim, e);
		}
		return showResult(victim, res);
	}
	
	public class TmpOnAggroEnter implements Runnable
	{
		private final L2Npc _npc;
		private final L2PcInstance _pc;
		private final boolean _isPet;
		
		public TmpOnAggroEnter(L2Npc npc, L2PcInstance pc, boolean isPet)
		{
			_npc = npc;
			_pc = pc;
			_isPet = isPet;
		}
		
		@Override
		public void run()
		{
			String res = null;
			try
			{
				res = onAggroRangeEnter(_npc, _pc, _isPet);
			}
			catch (Exception e)
			{
				showError(_pc, e);
			}
			showResult(_pc, res);
			
		}
	}
	
	public final boolean notifyAggroRangeEnter(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		ThreadPoolManager.getInstance().executeAi(new TmpOnAggroEnter(npc, player, isPet));
		return true;
	}

	public final boolean notifySpawn(L2Npc npc)
	{
		String res = null;
		try
		{
			res = onSpawn(npc);
		}
		catch(Exception e)
		{
			_log.error("", e);
			return true;
		}

		return showResult(npc, res);
	}

	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		return null;
	}

	public String onAttackAct(L2Npc npc, L2PcInstance victim)
	{
		return null;
	}
	
	
	public String onDeath(L2Character killer, L2Character victim, QuestState qs)
	{
		if(killer instanceof L2Npc)
		{
			return onAdvEvent("", (L2Npc) killer, qs.getPlayer());
		}
		else
		{
			return onAdvEvent("", null, qs.getPlayer());
		}
	}

	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		if(player == null)
		{
			return null;
		}

		QuestState qs = player.getQuestState(getName());

		if(qs != null)
		{
			return onEvent(event, qs);
		}

		return null;
	}

	public void sendDlgMessage(String text, L2PcInstance player)
	{
		player.dialog = this;
		ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.S1.getId());
		dlg.addString(text);
		player.sendPacket(dlg);
	}

	public void onDlgAnswer(L2PcInstance player)
	{
	}

	public String onEvent(String event, QuestState qs)
	{
		return null;
	}

	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		return null;
	}

	public String onSkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet)
	{
		return null;
	}

	public String onTalk(L2Npc npc, L2PcInstance talker)
	{
		return null;
	}

	public String onFirstTalk(L2Npc npc, L2PcInstance player)
	{
		return null;
	}

	public String onSkillUse(L2Npc npc, L2PcInstance caster, L2Skill skill)
	{
		return null;
	}

	public String onSpellFinished(L2Npc npc, L2PcInstance player, L2Skill skill)
	{
		return null;
	}

	public String onFactionCall(L2Npc npc, L2Npc caller, L2PcInstance attacker, boolean isPet)
	{
		return null;
	}

	public String onAggroRangeEnter(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		return null;
	}

	public String onSpawn(L2Npc npc)
	{
		return null;
	}
	
	public boolean showError(L2PcInstance player, Throwable t)
	{
		_log.error(getScriptFile().getAbsolutePath(), t);
		if (t.getMessage() == null)
			t.printStackTrace();
		if (player != null && player.getAccessLevel().isGm())
		{
			String res = "<html><body><title>Script error</title>" + Util.getStackTrace(t) + "</body></html>";
			return showResult(player, res);
		}
		return false;
	}

	private boolean showResult(L2Character object, String res)
	{
		if(res == null)
		{
			return true;
		}

		if(object instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) object;

			if(res.endsWith(".htm"))
			{
				showHtmlFile(player, res);
			}
			else if(res.startsWith("<html>"))
			{
				NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
				npcReply.setHtml(res);
				npcReply.replace("%playername%", player.getName());
				player.sendPacket(npcReply);
				npcReply = null;
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString(res));
			}

			player = null;
		}

		return false;
	}

	public L2NpcTemplate addStartNpc(int npcId)
	{
		return addEventId(npcId, Quest.QuestEventType.QUEST_START);
	}

	public L2NpcTemplate addFirstTalkId(int npcId)
	{
		return addEventId(npcId, Quest.QuestEventType.NPC_FIRST_TALK);
	}

	public L2NpcTemplate addAttackId(int attackId)
	{
		return addEventId(attackId, Quest.QuestEventType.ON_ATTACK);
	}

	public L2NpcTemplate addKillId(int killId)
	{
		return addEventId(killId, Quest.QuestEventType.ON_KILL);
	}

	public L2NpcTemplate addTalkId(int talkId)
	{
		return addEventId(talkId, Quest.QuestEventType.QUEST_TALK);
	}

	public L2NpcTemplate addFactionCallId(int npcId)
	{
		return addEventId(npcId, Quest.QuestEventType.ON_FACTION_CALL);
	}

	public L2NpcTemplate addSkillUseId(int npcId)
	{
		return addEventId(npcId, Quest.QuestEventType.ON_SKILL_USE);
	}

	public L2NpcTemplate addSpellFinishedId(int npcId)
	{
		return addEventId(npcId, Quest.QuestEventType.ON_SPELL_FINISHED);
	}

	public L2NpcTemplate addAggroRangeEnterId(int npcId)
	{
		return addEventId(npcId, Quest.QuestEventType.ON_AGGRO_RANGE_ENTER);
	}

	public L2NpcTemplate addSpawnId(int npcId)
	{
		return addEventId(npcId, Quest.QuestEventType.ON_SPAWN);
	}

	public L2NpcTemplate addEventId(int npcId, QuestEventType eventType)
	{
		try
		{
			L2NpcTemplate t = NpcTable.getInstance().getTemplate(npcId);

			if(t != null)
			{
				t.addQuestEvent(eventType, this);
			}

			return t;
		}
		catch(Exception e)
		{
			_log.error("", e);
			return null;
		}
	}

	public final static void playerEnter(L2PcInstance player)
	{
		
		if(Config.ALT_DEV_NO_QUESTS)
			return;
		
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;

			PreparedStatement invalidQuestData = con.prepareStatement("DELETE FROM character_quests WHERE char_id = ? and name = ?");
			PreparedStatement invalidQuestDataVar = con.prepareStatement("DELETE FROM character_quests WHERE char_id = ? and name = ? and var = ?");

			statement = con.prepareStatement("SELECT name, value FROM character_quests WHERE char_id = ? AND var = ?");
			statement.setInt(1, player.getObjectId());
			statement.setString(2, "<state>");
			ResultSet rs = statement.executeQuery();

			while(rs.next())
			{
				String questId = rs.getString("name");
				String stateId = rs.getString("value");

				Quest q = QuestManager.getInstance().getQuest(questId);

				if(q == null)
				{
					_log.warn("Unknown quest " + questId + " for player " + player.getName());
					if(Config.AUTODELETE_INVALID_QUEST_DATA)
					{
						invalidQuestData.setInt(1, player.getObjectId());
						invalidQuestData.setString(2, questId);
						invalidQuestData.executeUpdate();
					}

					continue;
				}

				boolean completed = false;

				if(stateId.equals("Completed"))
				{
					completed = true;
				}

				State state = q._states.get(stateId);
				if(state == null)
				{
					_log.warn("Unknown state in quest " + questId + " for player " + player.getName());
					if(Config.AUTODELETE_INVALID_QUEST_DATA)
					{
						invalidQuestData.setInt(1, player.getObjectId());
						invalidQuestData.setString(2, questId);
						invalidQuestData.executeUpdate();
					}
					continue;
				}
				QuestState qs = new QuestState(q, player, state, completed);
				player.setQuestState(qs);
			}

			rs.close();
			invalidQuestData.close();
			ResourceUtil.closeStatement(statement);

			statement = con.prepareStatement("SELECT name, var, value FROM character_quests WHERE char_id = ?");
			statement.setInt(1, player.getObjectId());
			rs = statement.executeQuery();

			while(rs.next())
			{
				String questId = rs.getString("name");
				String var = rs.getString("var");
				String value = rs.getString("value");

				QuestState qs = player.getQuestState(questId);

				if(qs == null)
				{
					_log.warn("Lost variable " + var + " in quest " + questId + " for player " + player.getName());

					if(Config.AUTODELETE_INVALID_QUEST_DATA)
					{
						invalidQuestDataVar.setInt(1, player.getObjectId());
						invalidQuestDataVar.setString(2, questId);
						invalidQuestDataVar.setString(3, var);
						invalidQuestDataVar.executeUpdate();
					}
					continue;
				}
				qs.setInternal(var, value);
			}

			rs.close();
			invalidQuestDataVar.close();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("could not insert char quest", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}

		for(String name : _allEventsS.keySet())
		{
			player.processQuestEvent(name, "enter");
		}
	}

	public final void saveGlobalQuestVar(String var, String value)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("REPLACE INTO quest_global_data (quest_name, var, value) VALUES (?, ?, ?)");
			statement.setString(1, getName());
			statement.setString(2, var);
			statement.setString(3, value);
			statement.executeUpdate();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("could not insert global quest variable", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public final String loadGlobalQuestVar(String var)
	{
		String result = "";
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("SELECT value FROM quest_global_data WHERE quest_name = ? AND var = ?");
			statement.setString(1, getName());
			statement.setString(2, var);
			ResultSet rs = statement.executeQuery();

			if(rs.first())
			{
				result = rs.getString(1);
			}

			rs.close();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("could not load global quest variable", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
		return result;
	}

	public final void deleteGlobalQuestVar(String var)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("DELETE FROM quest_global_data WHERE quest_name = ? AND var = ?");
			statement.setString(1, getName());
			statement.setString(2, var);
			statement.executeUpdate();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("could not delete global quest variable", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public final void deleteAllGlobalQuestVars()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("DELETE FROM quest_global_data WHERE quest_name = ?");
			statement.setString(1, getName());
			statement.executeUpdate();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("could not delete global quest variable", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public static void createQuestVarInDb(QuestState qs, String var, String value)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("INSERT INTO character_quests (char_id, name, var, value) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE value=?");
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuestName());
			statement.setString(3, var);
			statement.setString(4, value);
			statement.setString(5, value);
			statement.executeUpdate();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("could not insert char quest", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public static void updateQuestVarInDb(QuestState qs, String var, String value)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("UPDATE character_quests SET value = ? WHERE char_id = ? AND name = ? AND var = ?");
			statement.setString(1, value);
			statement.setInt(2, qs.getPlayer().getObjectId());
			statement.setString(3, qs.getQuestName());
			statement.setString(4, var);
			statement.executeUpdate();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("could not update char quest", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public static void deleteQuestVarInDb(QuestState qs, String var)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("DELETE FROM character_quests WHERE char_id = ? AND name = ? AND var = ?");
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuestName());
			statement.setString(3, var);
			statement.executeUpdate();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("could not delete char quest", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public static void deleteQuestInDb(QuestState qs)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("DELETE FROM character_quests WHERE char_id = ? AND name = ?");
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuestName());
			statement.executeUpdate();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("could not delete char quest", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public static void createQuestInDb(QuestState qs)
	{
		createQuestVarInDb(qs, "<state>", qs.getStateId());
	}

	/**
	 * Update informations regarding quest in database.<BR>
	 * <U><I>Actions :</I></U><BR>
	 * <LI>Get ID state of the quest recorded in object qs</LI>
	 * <LI>Test if quest is completed. If true, add a star (*) before the ID state</LI>
	 * <LI>Save in database the ID state (with or without the star) for the variable called "&lt;state&gt;" of the quest</LI>
	 * @param qs : QuestState
	 */
	public static void updateQuestInDb(QuestState qs)
	{
		String val = qs.getStateId();
		updateQuestVarInDb(qs, "<state>", val);
		val = null;
	}

	/**
	 * Return default html page "You are either not on a quest that involves this NPC.."
	 * @param player
	 * @return
	 */
	public static String getNoQuestMsg(L2PcInstance player)
	{
		final String result = HtmCache.getInstance().getHtm("data/html/noquest.htm");
		if(result != null && result.length() > 0)
			return result;

		return DEFAULT_NO_QUEST_MSG;
	}

	/**
	 * Return default html page "This quest has already been completed."
	 * @param player
	 * @return
	 */
	public static String getAlreadyCompletedMsg(L2PcInstance player)
	{
		final String result = HtmCache.getInstance().getHtm("data/html/alreadycompleted.htm");
		if(result != null && result.length() > 0)
			return result;

		return DEFAULT_ALREADY_COMPLETED_MSG;
	}

	public L2PcInstance getRandomPartyMember(L2PcInstance player)
	{
		if(player == null)
		{
			return null;
		}

		if(player.getParty() == null || player.getParty().getPartyMembers().size() == 0)
		{
			return player;
		}

		L2Party party = player.getParty();

		return party.getPartyMembers().get(Rnd.get(party.getPartyMembers().size()));
	}

	public L2PcInstance getRandomPartyMember(L2PcInstance player, String value)
	{
		return getRandomPartyMember(player, "cond", value);
	}

	public L2PcInstance getRandomPartyMember(L2PcInstance player, String var, String value)
	{
		if(player == null)
		{
			return null;
		}

		if(var == null)
		{
			return getRandomPartyMember(player);
		}

		QuestState temp = null;
		L2Party party = player.getParty();

		if(party == null || party.getPartyMembers().size() == 0)
		{
			temp = player.getQuestState(getName());
			if(temp != null && temp.get(var) != null && ((String) temp.get(var)).equalsIgnoreCase(value))
			{
				return player;
			}

			return null;
		}

		FastList<L2PcInstance> candidates = new FastList<L2PcInstance>();

		L2Object target = player.getTarget();
		if(target == null)
		{
			target = player;
		}

		for(L2PcInstance partyMember : party.getPartyMembers())
		{
			temp = partyMember.getQuestState(getName());
			if(temp != null && temp.get(var) != null && ((String) temp.get(var)).equalsIgnoreCase(value) && partyMember.isInsideRadius(target, 1500, true, false))
			{
				candidates.add(partyMember);
			}
		}

		if(candidates.size() == 0)
		{
			return null;
		}

		return candidates.get(Rnd.get(candidates.size()));
	}

	public L2PcInstance getRandomPartyMemberState(L2PcInstance player, State state)
	{
		if(player == null)
		{
			return null;
		}

		if(state == null)
		{
			return getRandomPartyMember(player);
		}

		QuestState temp = null;
		L2Party party = player.getParty();
		if(party == null || party.getPartyMembers().size() == 0)
		{
			temp = player.getQuestState(getName());
			if(temp != null && temp.getState() == state)
			{
				return player;
			}

			return null;
		}

		FastList<L2PcInstance> candidates = new FastList<L2PcInstance>();

		L2Object target = player.getTarget();
		if(target == null)
		{
			target = player;
		}

		for(L2PcInstance partyMember : party.getPartyMembers())
		{
			temp = partyMember.getQuestState(getName());

			if(temp != null && temp.getState() == state && partyMember.isInsideRadius(target, 1500, true, false))
			{
				candidates.add(partyMember);
			}
		}

		if(candidates.size() == 0)
		{
			return null;
		}
		return candidates.get(Rnd.get(candidates.size()));
	}

	public String showHtmlFile(L2PcInstance player, String fileName)
	{
		String questId = getName();

		String directory = getDescr().toLowerCase();
		String content = HtmCache.getInstance().getHtm("data/scripts/" + directory + "/" + questId + "/" + fileName);

		if(content == null)
		{
			content = HtmCache.getInstance().getHtmForce("data/scripts/quests/" + questId + "/" + fileName);
		}

		if(player != null && player.getTarget() != null)
		{
			content = content.replaceAll("%objectId%", String.valueOf(player.getTarget().getObjectId()));
		}

		if(content != null)
		{
			NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
			npcReply.setHtml(content);
			npcReply.replace("%playername%", player.getName());
			player.sendPacket(npcReply);
			npcReply = null;
		}

		if ((player.isGM()) && (player.getAccessLevel().getLevel() == Config.GM_ACCESS_LEVEL))
			player.sendChatMessage(0, 0, "HTML", "scripts/" + directory + "/" + questId + "/" + fileName);
		return content;
	}

	public L2Npc addSpawn(int npcId, L2Character cha)
	{
		return QuestSpawn.getInstance().addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), false, 0);
	}

	public L2Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, int despawnDelay)
	{
		return QuestSpawn.getInstance().addSpawn(npcId, x, y, z, heading, randomOffset, despawnDelay);
	}

	public void registerItem(int itemId)
	{
		if(_questItemIds == null)
		{
			_questItemIds = new FastList<Integer>();
		}

		_questItemIds.add(itemId);
	}

	public FastList<Integer> getRegisteredItemIds()
	{
		return _questItemIds;
	}

	@Override
	public ScriptManager<?> getScriptManager()
	{
		return QuestManager.getInstance();
	}

	@Override
	public boolean unload()
	{
		saveGlobalData();
		for(FastList<QuestTimer> timers : _allEventTimers.values())
		{
			for(QuestTimer timer : timers)
			{
				timer.cancel();
			}
		}
		_allEventTimers.clear();
		return QuestManager.getInstance().removeQuest(this);
	}

	@Override
	public boolean reload()
	{
		unload();
		return super.reload();
	}

	@Override
	public String getScriptName()
	{
		return getName();
	}

	@SuppressWarnings("synthetic-access")
	public class tmpOnSkillSee implements Runnable
	{
		private final L2Npc _npc;
		private final L2PcInstance _caster;
		private final L2Skill _skill;
		private final L2Object[] _targets;
		private final boolean _isPet;

		public tmpOnSkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet)
		{
			_npc = npc;
			_caster = caster;
			_skill = skill;
			_targets = targets;
			_isPet = isPet;
		}

		@Override
		public void run()
		{
			String res = null;
			try
			{
				res = onSkillSee(_npc, _caster, _skill, _targets, _isPet);
			}
			catch(Exception e)
			{
				showError(_caster, e);
			}
			showResult(_caster, res);
		}
	}

	public void registerMobs(int[] mobs)
	{
		for(int id : mobs)
		{
			addEventId(id, QuestEventType.ON_ATTACK);
			addEventId(id, QuestEventType.ON_KILL);
			addEventId(id, QuestEventType.ON_SPAWN);
			addEventId(id, QuestEventType.ON_SPELL_FINISHED);
			addEventId(id, QuestEventType.ON_FACTION_CALL);
			addEventId(id, QuestEventType.ON_AGGRO_RANGE_ENTER);
		}
	}

	public void registerMobs(int[] mobs, QuestEventType... types)
	{
		for(int id : mobs)
		{
			for(QuestEventType type : types)
			{
				addEventId(id, type);
			}
		}
	}

	public static <T> boolean contains(T[] array, T obj)
	{
		for(int i = 0; i < array.length; i++)
		{
			if(array[i] == obj)
			{
				return true;
			}
		}
		return false;
	}

	public static boolean contains(int[] array, int obj)
	{
		for(int i = 0; i < array.length; i++)
		{
			if(array[i] == obj)
			{
				return true;
			}
		}
		return false;
	}

}