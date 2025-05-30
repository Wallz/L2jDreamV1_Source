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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.sql.SpawnTable;
import com.src.gameserver.datatables.xml.DoorTable;
import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.managers.GrandBossManager;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2DoorInstance;
import com.src.gameserver.model.actor.instance.L2GrandBossInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.position.L2CharPosition;
import com.src.gameserver.model.quest.Quest;
import com.src.gameserver.model.quest.State;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.gameserver.network.serverpackets.MagicSkillUser;
import com.src.gameserver.network.serverpackets.SpecialCamera;
import com.src.gameserver.templates.StatsSet;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.templates.skills.L2EffectType;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;
import com.src.util.random.Rnd;

public class VanHalter extends Quest
{
	private static final Log _log = LogFactory.getLog(VanHalter.class.getName());

	protected Map<Integer, List<L2PcInstance>> _bleedingPlayers = new FastMap<Integer, List<L2PcInstance>>();

	protected Map<Integer, L2Spawn> _monsterSpawn = new FastMap<Integer, L2Spawn>();
	protected List<L2Spawn> _royalGuardSpawn = new FastList<L2Spawn>();
	protected List<L2Spawn> _royalGuardCaptainSpawn = new FastList<L2Spawn>();
	protected List<L2Spawn> _royalGuardHelperSpawn = new FastList<L2Spawn>();
	protected List<L2Spawn> _triolRevelationSpawn = new FastList<L2Spawn>();
	protected List<L2Spawn> _triolRevelationAlive = new FastList<L2Spawn>();
	protected List<L2Spawn> _guardOfAltarSpawn = new FastList<L2Spawn>();
	protected Map<Integer, L2Spawn> _cameraMarkerSpawn = new FastMap<Integer, L2Spawn>();
	protected L2Spawn _ritualOfferingSpawn = null;
	protected L2Spawn _ritualSacrificeSpawn = null;
	protected L2Spawn _vanHalterSpawn = null;

	protected List<L2Npc> _monsters = new FastList<L2Npc>();
	protected List<L2Npc> _royalGuard = new FastList<L2Npc>();
	protected List<L2Npc> _royalGuardCaptain = new FastList<L2Npc>();
	protected List<L2Npc> _royalGuardHepler = new FastList<L2Npc>();
	protected List<L2Npc> _triolRevelation = new FastList<L2Npc>();
	protected List<L2Npc> _guardOfAltar = new FastList<L2Npc>();
	protected Map<Integer, L2Npc> _cameraMarker = new FastMap<Integer, L2Npc>();
	protected List<L2DoorInstance> _doorOfAltar = new FastList<L2DoorInstance>();
	protected List<L2DoorInstance> _doorOfSacrifice = new FastList<L2DoorInstance>();
	protected L2Npc _ritualOffering = null;
	protected L2Npc _ritualSacrifice = null;
	protected L2GrandBossInstance _vanHalter = null;

	protected ScheduledFuture<?> _movieTask = null;
	protected ScheduledFuture<?> _closeDoorOfAltarTask = null;
	protected ScheduledFuture<?> _openDoorOfAltarTask = null;
	protected ScheduledFuture<?> _lockUpDoorOfAltarTask = null;
	protected ScheduledFuture<?> _callRoyalGuardHelperTask = null;
	protected ScheduledFuture<?> _timeUpTask = null;
	protected ScheduledFuture<?> _intervalTask = null;
	protected ScheduledFuture<?> _halterEscapeTask = null;
	protected ScheduledFuture<?> _setBleedTask = null;

	boolean _isLocked = false;
	boolean _isHalterSpawned = false;
	boolean _isSacrificeSpawned = false;
	boolean _isCaptainSpawned = false;
	boolean _isHelperCalled = false;

	private static final byte INTERVAL = 0;
	private static final byte NOTSPAWN = 1;
	private static final byte ALIVE = 2;

	private static VanHalter _instance;

	public static final VanHalter getInstance()
	{
		if(_instance == null)
		{
			_instance = new VanHalter(-1, "vanhalter", "ai_inv");
		}

		return _instance;
	}

	public VanHalter(int questId, String name, String descr)
	{
		super(questId, name, descr);

		setInitialState(new State("Start", this));
		
		int[] mobs =
		{
				29062, 22188, 32058, 32059, 32060, 32061, 32062, 32063, 32064, 32065, 32066
		};

		addEventId(29062, Quest.QuestEventType.ON_ATTACK);
		for(int mob : mobs)
		{
			addEventId(mob, Quest.QuestEventType.ON_KILL);
		}

		_isLocked = false;
		_isCaptainSpawned = false;
		_isHelperCalled = false;
		_isHalterSpawned = false;

		_doorOfAltar.add(DoorTable.getInstance().getDoor(19160014));
		_doorOfAltar.add(DoorTable.getInstance().getDoor(19160015));
		openDoorOfAltar(true);
		_doorOfSacrifice.add(DoorTable.getInstance().getDoor(19160016));
		_doorOfSacrifice.add(DoorTable.getInstance().getDoor(19160017));
		closeDoorOfSacrifice();

		loadRoyalGuard();
		loadTriolRevelation();
		loadRoyalGuardCaptain();
		loadRoyalGuardHelper();
		loadGuardOfAltar();
		loadVanHalter();
		loadRitualOffering();
		loadRitualSacrifice();

		spawnRoyalGuard();
		spawnTriolRevelation();
		spawnVanHalter();
		spawnRitualOffering();

		_cameraMarkerSpawn.clear();
		try
		{
			L2NpcTemplate template1 = NpcTable.getInstance().getTemplate(13014);
			L2Spawn tempSpawn;

			tempSpawn = new L2Spawn(template1);
			tempSpawn.setLocx(-16397);
			tempSpawn.setLocy(-55200);
			tempSpawn.setLocz(-10449);
			tempSpawn.setHeading(16384);
			tempSpawn.setAmount(1);
			tempSpawn.setRespawnDelay(60000);
			SpawnTable.getInstance().addNewSpawn(tempSpawn, false);
			_cameraMarkerSpawn.put(1, tempSpawn);

			tempSpawn = new L2Spawn(template1);
			tempSpawn.setLocx(-16397);
			tempSpawn.setLocy(-55200);
			tempSpawn.setLocz(-10051);
			tempSpawn.setHeading(16384);
			tempSpawn.setAmount(1);
			tempSpawn.setRespawnDelay(60000);
			SpawnTable.getInstance().addNewSpawn(tempSpawn, false);
			_cameraMarkerSpawn.put(2, tempSpawn);

			tempSpawn = new L2Spawn(template1);
			tempSpawn.setLocx(-16397);
			tempSpawn.setLocy(-55200);
			tempSpawn.setLocz(-9741);
			tempSpawn.setHeading(16384);
			tempSpawn.setAmount(1);
			tempSpawn.setRespawnDelay(60000);
			SpawnTable.getInstance().addNewSpawn(tempSpawn, false);
			_cameraMarkerSpawn.put(3, tempSpawn);

			tempSpawn = new L2Spawn(template1);
			tempSpawn.setLocx(-16397);
			tempSpawn.setLocy(-55200);
			tempSpawn.setLocz(-9394);
			tempSpawn.setHeading(16384);
			tempSpawn.setAmount(1);
			tempSpawn.setRespawnDelay(60000);
			SpawnTable.getInstance().addNewSpawn(tempSpawn, false);
			_cameraMarkerSpawn.put(4, tempSpawn);

			tempSpawn = new L2Spawn(template1);
			tempSpawn.setLocx(-16397);
			tempSpawn.setLocy(-55197);
			tempSpawn.setLocz(-8739);
			tempSpawn.setHeading(16384);
			tempSpawn.setAmount(1);
			tempSpawn.setRespawnDelay(60000);
			SpawnTable.getInstance().addNewSpawn(tempSpawn, false);
			_cameraMarkerSpawn.put(5, tempSpawn);
		}
		catch(Exception e)
		{
			_log.error("", e);
		}

		if(_timeUpTask != null)
		{
			_timeUpTask.cancel(false);
		}
		_timeUpTask = ThreadPoolManager.getInstance().scheduleGeneral(new TimeUp(), Config.HPH_ACTIVITYTIMEOFHALTER);

		if(_setBleedTask != null)
		{
			_setBleedTask.cancel(false);
		}
		_setBleedTask = ThreadPoolManager.getInstance().scheduleGeneral(new Bleeding(), 2000);

		int status = GrandBossManager.getInstance().getBossStatus(29062);
		if(status == INTERVAL)
		{
			enterInterval();
		}
		else
		{
			GrandBossManager.getInstance().setBossStatus(29062, NOTSPAWN);
		}
	}

	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		if(npc.getNpcId() == 29062)
		{
			if((int) (npc.getStatus().getCurrentHp() / npc.getMaxHp()) * 100 <= 20)
			{
				callRoyalGuardHelper();
			}
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		int npcId = npc.getNpcId();
		if(npcId == 32058 || npcId == 32059 || npcId == 32060 || npcId == 32061 || npcId == 32062 || npcId == 32063 || npcId == 32064 || npcId == 32065 || npcId == 32066)
		{
			removeBleeding(npcId);
			checkTriolRevelationDestroy();
		}
		if(npcId == 22188)
		{
			checkRoyalGuardCaptainDestroy();
		}
		if(npcId == 29062)
		{
			enterInterval();
		}
		return super.onKill(npc, killer, isPet);
	}

	protected void loadRoyalGuard()
	{
		_royalGuardSpawn.clear();

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay FROM vanhalter_spawnlist WHERE npc_templateid BETWEEN ? AND ? ORDER BY id");
			statement.setInt(1, 22175);
			statement.setInt(2, 22176);
			ResultSet rset = statement.executeQuery();

			L2Spawn spawnDat;
			L2NpcTemplate template1;

			while(rset.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if(template1 != null)
				{
					spawnDat = new L2Spawn(template1);
					spawnDat.setAmount(rset.getInt("count"));
					spawnDat.setLocx(rset.getInt("locx"));
					spawnDat.setLocy(rset.getInt("locy"));
					spawnDat.setLocz(rset.getInt("locz"));
					spawnDat.setHeading(rset.getInt("heading"));
					spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
					SpawnTable.getInstance().addNewSpawn(spawnDat, false);
					_royalGuardSpawn.add(spawnDat);
				}
				else
				{
					_log.warn("VanHalterManager.loadRoyalGuard: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
				}
			}

			rset.close();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("VanHalterManager.loadRoyalGuard: Spawn could not be initialized", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	protected void spawnRoyalGuard()
	{
		if(!_royalGuard.isEmpty())
		{
			deleteRoyalGuard();
		}

		for(L2Spawn rgs : _royalGuardSpawn)
		{
			rgs.startRespawn();
			_royalGuard.add(rgs.doSpawn());
		}
	}

	protected void deleteRoyalGuard()
	{
		for(L2Npc rg : _royalGuard)
		{
			rg.getSpawn().stopRespawn();
			rg.deleteMe();
		}

		_royalGuard.clear();
	}

	protected void loadTriolRevelation()
	{
		_triolRevelationSpawn.clear();

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay FROM vanhalter_spawnlist WHERE npc_templateid BETWEEN ? AND ? ORDER BY id");
			statement.setInt(1, 32058);
			statement.setInt(2, 32068);
			ResultSet rset = statement.executeQuery();

			L2Spawn spawnDat;
			L2NpcTemplate template1;

			while(rset.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if(template1 != null)
				{
					spawnDat = new L2Spawn(template1);
					spawnDat.setAmount(rset.getInt("count"));
					spawnDat.setLocx(rset.getInt("locx"));
					spawnDat.setLocy(rset.getInt("locy"));
					spawnDat.setLocz(rset.getInt("locz"));
					spawnDat.setHeading(rset.getInt("heading"));
					spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
					SpawnTable.getInstance().addNewSpawn(spawnDat, false);
					_triolRevelationSpawn.add(spawnDat);
				}
				else
				{
					_log.warn("VanHalterManager.loadTriolRevelation: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
				}
			}

			rset.close();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("VanHalterManager.loadTriolRevelation: Spawn could not be initialized:", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	protected void spawnTriolRevelation()
	{
		if(!_triolRevelation.isEmpty())
		{
			deleteTriolRevelation();
		}

		for(L2Spawn trs : _triolRevelationSpawn)
		{
			trs.startRespawn();
			_triolRevelation.add(trs.doSpawn());
			if(trs.getNpcid() != 32067 && trs.getNpcid() != 32068)
			{
				_triolRevelationAlive.add(trs);
			}
		}
	}

	protected void deleteTriolRevelation()
	{
		for(L2Npc tr : _triolRevelation)
		{
			tr.getSpawn().stopRespawn();
			tr.deleteMe();
		}
		_triolRevelation.clear();
		_bleedingPlayers.clear();
	}

	protected void loadRoyalGuardCaptain()
	{
		_royalGuardCaptainSpawn.clear();

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay FROM vanhalter_spawnlist WHERE npc_templateid = ? ORDER BY id");
			statement.setInt(1, 22188);
			ResultSet rset = statement.executeQuery();

			L2Spawn spawnDat;
			L2NpcTemplate template1;

			while(rset.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if(template1 != null)
				{
					spawnDat = new L2Spawn(template1);
					spawnDat.setAmount(rset.getInt("count"));
					spawnDat.setLocx(rset.getInt("locx"));
					spawnDat.setLocy(rset.getInt("locy"));
					spawnDat.setLocz(rset.getInt("locz"));
					spawnDat.setHeading(rset.getInt("heading"));
					spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
					SpawnTable.getInstance().addNewSpawn(spawnDat, false);
					_royalGuardCaptainSpawn.add(spawnDat);
				}
				else
				{
					_log.warn("VanHalterManager.loadRoyalGuardCaptain: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
				}
			}

			rset.close();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("VanHalterManager.loadRoyalGuardCaptain: Spawn could not be initialized", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	protected void spawnRoyalGuardCaptain()
	{
		if(!_royalGuardCaptain.isEmpty())
		{
			deleteRoyalGuardCaptain();
		}

		for(L2Spawn trs : _royalGuardCaptainSpawn)
		{
			trs.startRespawn();
			_royalGuardCaptain.add(trs.doSpawn());
		}
		_isCaptainSpawned = true;
	}

	protected void deleteRoyalGuardCaptain()
	{
		for(L2Npc tr : _royalGuardCaptain)
		{
			tr.getSpawn().stopRespawn();
			tr.deleteMe();
		}

		_royalGuardCaptain.clear();
	}

	protected void loadRoyalGuardHelper()
	{
		_royalGuardHelperSpawn.clear();

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay FROM vanhalter_spawnlist WHERE npc_templateid = ? ORDER BY id");
			statement.setInt(1, 22191);
			ResultSet rset = statement.executeQuery();

			L2Spawn spawnDat;
			L2NpcTemplate template1;

			while(rset.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if(template1 != null)
				{
					spawnDat = new L2Spawn(template1);
					spawnDat.setAmount(rset.getInt("count"));
					spawnDat.setLocx(rset.getInt("locx"));
					spawnDat.setLocy(rset.getInt("locy"));
					spawnDat.setLocz(rset.getInt("locz"));
					spawnDat.setHeading(rset.getInt("heading"));
					spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
					SpawnTable.getInstance().addNewSpawn(spawnDat, false);
					_royalGuardHelperSpawn.add(spawnDat);
				}
				else
				{
					_log.warn("VanHalterManager.loadRoyalGuardHelper: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
				}
			}

			rset.close();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("VanHalterManager.loadRoyalGuardHelper: Spawn could not be initialized", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	protected void spawnRoyalGuardHepler()
	{
		for(L2Spawn trs : _royalGuardHelperSpawn)
		{
			trs.startRespawn();
			_royalGuardHepler.add(trs.doSpawn());
		}
	}

	protected void deleteRoyalGuardHepler()
	{
		for(L2Npc tr : _royalGuardHepler)
		{
			tr.getSpawn().stopRespawn();
			tr.deleteMe();
		}
		_royalGuardHepler.clear();
	}

	protected void loadGuardOfAltar()
	{
		_guardOfAltarSpawn.clear();

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay FROM vanhalter_spawnlist WHERE npc_templateid = ? ORDER BY id");
			statement.setInt(1, 32051);
			ResultSet rset = statement.executeQuery();

			L2Spawn spawnDat;
			L2NpcTemplate template1;

			while(rset.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if(template1 != null)
				{
					spawnDat = new L2Spawn(template1);
					spawnDat.setAmount(rset.getInt("count"));
					spawnDat.setLocx(rset.getInt("locx"));
					spawnDat.setLocy(rset.getInt("locy"));
					spawnDat.setLocz(rset.getInt("locz"));
					spawnDat.setHeading(rset.getInt("heading"));
					spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
					SpawnTable.getInstance().addNewSpawn(spawnDat, false);
					_guardOfAltarSpawn.add(spawnDat);
				}
				else
				{
					_log.warn("VanHalterManager.loadGuardOfAltar: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
				}
			}

			rset.close();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("VanHalterManager.loadGuardOfAltar: Spawn could not be initialized", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	protected void spawnGuardOfAltar()
	{
		if(!_guardOfAltar.isEmpty())
		{
			deleteGuardOfAltar();
		}

		for(L2Spawn trs : _guardOfAltarSpawn)
		{
			trs.startRespawn();
			_guardOfAltar.add(trs.doSpawn());
		}
	}

	protected void deleteGuardOfAltar()
	{
		for(L2Npc tr : _guardOfAltar)
		{
			tr.getSpawn().stopRespawn();
			tr.deleteMe();
		}

		_guardOfAltar.clear();
	}

	protected void loadVanHalter()
	{
		_vanHalterSpawn = null;

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay FROM vanhalter_spawnlist WHERE npc_templateid = ? ORDER BY id");
			statement.setInt(1, 29062);
			ResultSet rset = statement.executeQuery();

			L2Spawn spawnDat;
			L2NpcTemplate template1;

			while(rset.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if(template1 != null)
				{
					spawnDat = new L2Spawn(template1);
					spawnDat.setAmount(rset.getInt("count"));
					spawnDat.setLocx(rset.getInt("locx"));
					spawnDat.setLocy(rset.getInt("locy"));
					spawnDat.setLocz(rset.getInt("locz"));
					spawnDat.setHeading(rset.getInt("heading"));
					spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
					SpawnTable.getInstance().addNewSpawn(spawnDat, false);
					_vanHalterSpawn = spawnDat;
				}
				else
				{
					_log.warn("VanHalterManager.loadVanHalter: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
				}
			}

			rset.close();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("VanHalterManager.loadVanHalter: Spawn could not be initialized", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	protected void spawnVanHalter()
	{
		_vanHalter = (L2GrandBossInstance) addSpawn(_vanHalterSpawn.getNpcid(), _vanHalterSpawn.getLocx(), _vanHalterSpawn.getLocy(), _vanHalterSpawn.getLocz(), _vanHalterSpawn.getHeading(), false, 0);
		GrandBossManager.getInstance().addBoss(_vanHalter);
		_vanHalter.setIsInvul(true);
		_isHalterSpawned = true;
	}

	protected void deleteVanHalter()
	{
		_vanHalter.setIsInvul(false);
		_vanHalter.getSpawn().stopRespawn();
		_vanHalter.deleteMe();
	}

	protected void loadRitualOffering()
	{
		_ritualOfferingSpawn = null;

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay FROM vanhalter_spawnlist WHERE npc_templateid = ? ORDER BY id");
			statement.setInt(1, 32038);
			ResultSet rset = statement.executeQuery();

			L2Spawn spawnDat;
			L2NpcTemplate template1;

			while(rset.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if(template1 != null)
				{
					spawnDat = new L2Spawn(template1);
					spawnDat.setAmount(rset.getInt("count"));
					spawnDat.setLocx(rset.getInt("locx"));
					spawnDat.setLocy(rset.getInt("locy"));
					spawnDat.setLocz(rset.getInt("locz"));
					spawnDat.setHeading(rset.getInt("heading"));
					spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
					SpawnTable.getInstance().addNewSpawn(spawnDat, false);
					_ritualOfferingSpawn = spawnDat;
				}
				else
				{
					_log.warn("VanHalterManager.loadRitualOffering: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
				}
			}

			rset.close();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("VanHalterManager.loadRitualOffering: Spawn could not be initialized", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	protected void spawnRitualOffering()
	{
		_ritualOffering = _ritualOfferingSpawn.doSpawn();
		_ritualOffering.setIsInvul(true);
		_ritualOffering.setIsParalyzed(true);
	}

	protected void deleteRitualOffering()
	{
		_ritualOffering.setIsInvul(false);
		_ritualOffering.setIsParalyzed(false);
		_ritualOffering.getSpawn().stopRespawn();
		_ritualOffering.deleteMe();
	}

	protected void loadRitualSacrifice()
	{
		_ritualSacrificeSpawn = null;

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay FROM vanhalter_spawnlist WHERE npc_templateid = ? ORDER BY id");
			statement.setInt(1, 22195);
			ResultSet rset = statement.executeQuery();

			L2Spawn spawnDat;
			L2NpcTemplate template1;

			while(rset.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if(template1 != null)
				{
					spawnDat = new L2Spawn(template1);
					spawnDat.setAmount(rset.getInt("count"));
					spawnDat.setLocx(rset.getInt("locx"));
					spawnDat.setLocy(rset.getInt("locy"));
					spawnDat.setLocz(rset.getInt("locz"));
					spawnDat.setHeading(rset.getInt("heading"));
					spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
					SpawnTable.getInstance().addNewSpawn(spawnDat, false);
					_ritualSacrificeSpawn = spawnDat;
				}
				else
				{
					_log.warn("VanHalterManager.loadRitualSacrifice: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
				}
			}

			rset.close();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("VanHalterManager.loadRitualSacrifice: Spawn could not be initialized", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	protected void spawnRitualSacrifice()
	{
		_ritualSacrifice = _ritualSacrificeSpawn.doSpawn();
		_ritualSacrifice.setIsInvul(true);
		_isSacrificeSpawned = true;
	}

	protected void deleteRitualSacrifice()
	{
		if(!_isSacrificeSpawned)
		{
			return;
		}

		_ritualSacrifice.getSpawn().stopRespawn();
		_ritualSacrifice.deleteMe();
		_isSacrificeSpawned = false;
	}

	protected void spawnCameraMarker()
	{
		_cameraMarker.clear();
		for(int i = 1; i <= _cameraMarkerSpawn.size(); i++)
		{
			_cameraMarker.put(i, _cameraMarkerSpawn.get(i).doSpawn());
			_cameraMarker.get(i).getSpawn().stopRespawn();
		}
	}

	protected void deleteCameraMarker()
	{
		if(_cameraMarker.isEmpty())
		{
			return;
		}

		for(int i = 1; i <= _cameraMarker.size(); i++)
		{
			_cameraMarker.get(i).deleteMe();
		}
		_cameraMarker.clear();
	}

	public void intruderDetection(L2PcInstance intruder)
	{
		if(_lockUpDoorOfAltarTask == null && !_isLocked && _isCaptainSpawned)
		{
			_lockUpDoorOfAltarTask = ThreadPoolManager.getInstance().scheduleGeneral(new LockUpDoorOfAltar(), Config.HPH_TIMEOFLOCKUPDOOROFALTAR);
		}
	}

	private class LockUpDoorOfAltar implements Runnable
	{
		@Override
		public void run()
		{
			closeDoorOfAltar(false);
			_isLocked = true;
			_lockUpDoorOfAltarTask = null;
		}
	}

	protected void openDoorOfAltar(boolean loop)
	{
		for(L2DoorInstance door : _doorOfAltar)
		{
			try
			{
				door.openMe();
			}
			catch(Exception e)
			{
				_log.error("", e);
			}
		}

		if(loop)
		{
			_isLocked = false;

			if(_closeDoorOfAltarTask != null)
			{
				_closeDoorOfAltarTask.cancel(false);
			}
			_closeDoorOfAltarTask = null;
			_closeDoorOfAltarTask = ThreadPoolManager.getInstance().scheduleGeneral(new CloseDoorOfAltar(), Config.HPH_INTERVALOFDOOROFALTER);
		}
		else
		{
			if(_closeDoorOfAltarTask != null)
			{
				_closeDoorOfAltarTask.cancel(false);
			}
			_closeDoorOfAltarTask = null;
		}
	}

	private class OpenDoorOfAltar implements Runnable
	{
		@Override
		public void run()
		{
			openDoorOfAltar(true);
		}
	}

	protected void closeDoorOfAltar(boolean loop)
	{
		for(L2DoorInstance door : _doorOfAltar)
		{
			door.closeMe();
		}

		if(loop)
		{
			if(_openDoorOfAltarTask != null)
			{
				_openDoorOfAltarTask.cancel(false);
			}
			_openDoorOfAltarTask = null;
			_openDoorOfAltarTask = ThreadPoolManager.getInstance().scheduleGeneral(new OpenDoorOfAltar(), Config.HPH_INTERVALOFDOOROFALTER);
		}
		else
		{
			if(_openDoorOfAltarTask != null)
			{
				_openDoorOfAltarTask.cancel(false);
			}
			_openDoorOfAltarTask = null;
		}
	}

	private class CloseDoorOfAltar implements Runnable
	{
		@Override
		public void run()
		{
			closeDoorOfAltar(true);
		}
	}

	protected void openDoorOfSacrifice()
	{
		for(L2DoorInstance door : _doorOfSacrifice)
		{
			try
			{
				door.openMe();
			}
			catch(Exception e)
			{
				_log.error("", e);
			}
		}
	}

	protected void closeDoorOfSacrifice()
	{
		for(L2DoorInstance door : _doorOfSacrifice)
		{
			try
			{
				door.closeMe();
			}
			catch(Exception e)
			{
				_log.error("", e);
			}
		}
	}

	public void checkTriolRevelationDestroy()
	{
		if(_isCaptainSpawned)
		{
			return;
		}

		boolean isTriolRevelationDestroyed = true;
		for(L2Spawn tra : _triolRevelationAlive)
		{
			if(!tra.getLastSpawn().isDead())
			{
				isTriolRevelationDestroyed = false;
			}
		}

		if(isTriolRevelationDestroyed)
		{
			spawnRoyalGuardCaptain();
		}
	}

	public void checkRoyalGuardCaptainDestroy()
	{
		if(!_isHalterSpawned)
		{
			return;
		}

		deleteRoyalGuard();
		deleteRoyalGuardCaptain();
		spawnGuardOfAltar();
		openDoorOfSacrifice();

		_vanHalter.setIsInvul(true);
		spawnCameraMarker();

		if(_timeUpTask != null)
		{
			_timeUpTask.cancel(false);
		}
		_timeUpTask = null;

		_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(1), Config.HPH_APPTIMEOFHALTER);
	}

	protected void combatBeginning()
	{
		if(_timeUpTask != null)
		{
			_timeUpTask.cancel(false);
		}
		_timeUpTask = ThreadPoolManager.getInstance().scheduleGeneral(new TimeUp(), Config.HPH_FIGHTTIMEOFHALTER);

		Map<Integer, L2PcInstance> _targets = new FastMap<Integer, L2PcInstance>();
		int i = 0;

		for(L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
		{
			i++;
			_targets.put(i, pc);
		}

		_vanHalter.reduceCurrentHp(1, _targets.get(Rnd.get(1, i)));
	}

	public void callRoyalGuardHelper()
	{
		if(!_isHelperCalled)
		{
			_isHelperCalled = true;
			_halterEscapeTask = ThreadPoolManager.getInstance().scheduleGeneral(new HalterEscape(), 500);
			_callRoyalGuardHelperTask = ThreadPoolManager.getInstance().scheduleGeneral(new CallRoyalGuardHelper(), 1000);
		}
	}

	private class CallRoyalGuardHelper implements Runnable
	{
		@Override
		public void run()
		{
			spawnRoyalGuardHepler();

			if(_royalGuardHepler.size() <= Config.HPH_CALLROYALGUARDHELPERCOUNT && !_vanHalter.isDead())
			{
				if(_callRoyalGuardHelperTask != null)
				{
					_callRoyalGuardHelperTask.cancel(false);
				}
				_callRoyalGuardHelperTask = ThreadPoolManager.getInstance().scheduleGeneral(new CallRoyalGuardHelper(), Config.HPH_CALLROYALGUARDHELPERINTERVAL);
			}
			else
			{
				if(_callRoyalGuardHelperTask != null)
				{
					_callRoyalGuardHelperTask.cancel(false);
				}
				_callRoyalGuardHelperTask = null;
			}
		}
	}

	private class HalterEscape implements Runnable
	{
		@Override
		public void run()
		{
			if(_royalGuardHepler.size() <= Config.HPH_CALLROYALGUARDHELPERCOUNT && !_vanHalter.isDead())
			{
				if(_vanHalter.isAfraid())
				{
					_vanHalter.stopEffects(L2EffectType.FEAR);
					_vanHalter.setIsAfraid(false);
					_vanHalter.updateAbnormalEffect();
				}
				else
				{
					_vanHalter.startFear();
					if(_vanHalter.getZ() >= -10476)
					{
						L2CharPosition pos = new L2CharPosition(-16397, -53308, -10448, 0);
						if(_vanHalter.getX() == pos.x && _vanHalter.getY() == pos.y)
						{
							_vanHalter.stopEffects(L2EffectType.FEAR);
							_vanHalter.setIsAfraid(false);
							_vanHalter.updateAbnormalEffect();
						}
						else
						{
							_vanHalter.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, pos);
						}
					}
					else if(_vanHalter.getX() >= -16397)
					{
						L2CharPosition pos = new L2CharPosition(-15548, -54830, -10475, 0);
						_vanHalter.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, pos);
					}
					else
					{
						L2CharPosition pos = new L2CharPosition(-17248, -54830, -10475, 0);
						_vanHalter.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, pos);
					}
				}
				if(_halterEscapeTask != null)
				{
					_halterEscapeTask.cancel(false);
				}
				_halterEscapeTask = ThreadPoolManager.getInstance().scheduleGeneral(new HalterEscape(), 5000);
			}
			else
			{
				_vanHalter.stopEffects(L2EffectType.FEAR);
				_vanHalter.setIsAfraid(false);
				_vanHalter.updateAbnormalEffect();
				if(_halterEscapeTask != null)
				{
					_halterEscapeTask.cancel(false);
				}
				_halterEscapeTask = null;
			}
		}
	}

	protected void addBleeding()
	{
		L2Skill bleed = SkillTable.getInstance().getInfo(4615, 12);

		for(L2Npc tr : _triolRevelation)
		{
			if(!tr.getKnownList().getKnownPlayersInRadius(tr.getAggroRange()).iterator().hasNext() || tr.isDead())
			{
				continue;
			}

			List<L2PcInstance> bpc = new FastList<L2PcInstance>();

			for(L2PcInstance pc : tr.getKnownList().getKnownPlayersInRadius(tr.getAggroRange()))
			{
				if(pc.getFirstEffect(bleed) == null)
				{
					bleed.getEffects(tr, pc);
					tr.broadcastPacket(new MagicSkillUser(tr, pc, bleed.getId(), 12, 1, 1));
				}

				bpc.add(pc);
			}
			_bleedingPlayers.remove(tr.getNpcId());
			_bleedingPlayers.put(tr.getNpcId(), bpc);
		}
	}

	public void removeBleeding(int npcId)
	{
		if(_bleedingPlayers.get(npcId) == null)
		{
			return;
		}
		for(L2PcInstance pc : (FastList<L2PcInstance>) _bleedingPlayers.get(npcId))
		{
			if(pc.getFirstEffect(L2EffectType.DMG_OVER_TIME) != null)
			{
				pc.stopEffects(L2EffectType.DMG_OVER_TIME);
			}
		}
		_bleedingPlayers.remove(npcId);
	}

	private class Bleeding implements Runnable
	{
		@Override
		public void run()
		{
			addBleeding();

			if(_setBleedTask != null)
			{
				_setBleedTask.cancel(false);
			}
			_setBleedTask = ThreadPoolManager.getInstance().scheduleGeneral(new Bleeding(), 2000);
		}
	}

	public void enterInterval()
	{
		if(_callRoyalGuardHelperTask != null)
		{
			_callRoyalGuardHelperTask.cancel(false);
		}
		_callRoyalGuardHelperTask = null;

		if(_closeDoorOfAltarTask != null)
		{
			_closeDoorOfAltarTask.cancel(false);
		}
		_closeDoorOfAltarTask = null;

		if(_halterEscapeTask != null)
		{
			_halterEscapeTask.cancel(false);
		}
		_halterEscapeTask = null;

		if(_intervalTask != null)
		{
			_intervalTask.cancel(false);
		}
		_intervalTask = null;

		if(_lockUpDoorOfAltarTask != null)
		{
			_lockUpDoorOfAltarTask.cancel(false);
		}
		_lockUpDoorOfAltarTask = null;

		if(_movieTask != null)
		{
			_movieTask.cancel(false);
		}
		_movieTask = null;

		if(_openDoorOfAltarTask != null)
		{
			_openDoorOfAltarTask.cancel(false);
		}
		_openDoorOfAltarTask = null;

		if(_timeUpTask != null)
		{
			_timeUpTask.cancel(false);
		}
		_timeUpTask = null;

		if(_vanHalter.isDead())
		{
			_vanHalter.getSpawn().stopRespawn();
		}
		else
		{
			deleteVanHalter();
		}
		deleteRoyalGuardHepler();
		deleteRoyalGuardCaptain();
		deleteRoyalGuard();
		deleteRitualOffering();
		deleteRitualSacrifice();
		deleteGuardOfAltar();

		if(_intervalTask != null)
		{
			_intervalTask.cancel(false);
		}

		int status = GrandBossManager.getInstance().getBossStatus(29062);

		if(status != INTERVAL)
		{
			long interval = Rnd.get(Config.HPH_FIXINTERVALOFHALTER, Config.HPH_FIXINTERVALOFHALTER + Config.HPH_RANDOMINTERVALOFHALTER) * 3600000;
			StatsSet info = GrandBossManager.getInstance().getStatsSet(29062);
			info.set("respawn_time", (System.currentTimeMillis() + interval));
			GrandBossManager.getInstance().setStatsSet(29062, info);
			GrandBossManager.getInstance().setBossStatus(29062, INTERVAL);
		}

		StatsSet info = GrandBossManager.getInstance().getStatsSet(29062);
		long temp = info.getLong("respawn_time") - System.currentTimeMillis();
		_intervalTask = ThreadPoolManager.getInstance().scheduleGeneral(new Interval(), temp);
	}

	private class Interval implements Runnable
	{
		@Override
		public void run()
		{
			setupAltar();
		}
	}

	public void setupAltar()
	{
		if(_callRoyalGuardHelperTask != null)
		{
			_callRoyalGuardHelperTask.cancel(false);
		}
		_callRoyalGuardHelperTask = null;

		if(_closeDoorOfAltarTask != null)
		{
			_closeDoorOfAltarTask.cancel(false);
		}
		_closeDoorOfAltarTask = null;

		if(_halterEscapeTask != null)
		{
			_halterEscapeTask.cancel(false);
		}
		_halterEscapeTask = null;

		if(_intervalTask != null)
		{
			_intervalTask.cancel(false);
		}
		_intervalTask = null;

		if(_lockUpDoorOfAltarTask != null)
		{
			_lockUpDoorOfAltarTask.cancel(false);
		}
		_lockUpDoorOfAltarTask = null;

		if(_movieTask != null)
		{
			_movieTask.cancel(false);
		}
		_movieTask = null;

		if(_openDoorOfAltarTask != null)
		{
			_openDoorOfAltarTask.cancel(false);
		}
		_openDoorOfAltarTask = null;

		if(_timeUpTask != null)
		{
			_timeUpTask.cancel(false);
		}
		_timeUpTask = null;

		deleteVanHalter();
		deleteTriolRevelation();
		deleteRoyalGuardHepler();
		deleteRoyalGuardCaptain();
		deleteRoyalGuard();
		deleteRitualSacrifice();
		deleteRitualOffering();
		deleteGuardOfAltar();
		deleteCameraMarker();

		_isLocked = false;
		_isCaptainSpawned = false;
		_isHelperCalled = false;
		_isHalterSpawned = false;

		closeDoorOfSacrifice();
		openDoorOfAltar(true);

		spawnTriolRevelation();
		spawnRoyalGuard();
		spawnRitualOffering();
		spawnVanHalter();

		GrandBossManager.getInstance().setBossStatus(29062, NOTSPAWN);

		if(_timeUpTask != null)
		{
			_timeUpTask.cancel(false);
		}
		_timeUpTask = ThreadPoolManager.getInstance().scheduleGeneral(new TimeUp(), Config.HPH_ACTIVITYTIMEOFHALTER);
	}

	private class TimeUp implements Runnable
	{
		@Override
		public void run()
		{
			enterInterval();
		}
	}

	private class Movie implements Runnable
	{
		private final int _distance = 6502500;
		private final int _taskId;

		public Movie(int taskId)
		{
			_taskId = taskId;
		}

		@Override
		public void run()
		{
			_vanHalter.setHeading(16384);
			_vanHalter.setTarget(_ritualOffering);

			switch(_taskId)
			{
				case 1:
					GrandBossManager.getInstance().setBossStatus(29062, ALIVE);

					for(L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if(pc.getPlanDistanceSq(_vanHalter) <= _distance)
						{
							_vanHalter.broadcastPacket(new SpecialCamera(_vanHalter.getObjectId(), 50, 90, 0, 0, 15000));
						}
					}

					if(_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(2), 16);

					break;

				case 2:
					for(L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if(pc.getPlanDistanceSq(_cameraMarker.get(5)) <= _distance)
						{
							_cameraMarker.get(5).broadcastPacket(new SpecialCamera(_cameraMarker.get(5).getObjectId(), 1842, 100, -3, 0, 15000));
						}
					}

					if(_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(3), 1);

					break;

				case 3:
					for(L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if(pc.getPlanDistanceSq(_cameraMarker.get(5)) <= _distance)
						{
							_cameraMarker.get(5).broadcastPacket(new SpecialCamera(_cameraMarker.get(5).getObjectId(), 1861, 97, -10, 1500, 15000));
						}
					}

					if(_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(4), 1500);

					break;

				case 4:
					for(L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if(pc.getPlanDistanceSq(_cameraMarker.get(4)) <= _distance)
						{
							_cameraMarker.get(4).broadcastPacket(new SpecialCamera(_cameraMarker.get(4).getObjectId(), 1876, 97, 12, 0, 15000));
						}
					}

					if(_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(5), 1);

					break;

				case 5:
					for(L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if(pc.getPlanDistanceSq(_cameraMarker.get(4)) <= _distance)
						{
							_cameraMarker.get(4).broadcastPacket(new SpecialCamera(_cameraMarker.get(4).getObjectId(), 1839, 94, 0, 1500, 15000));
						}
					}

					if(_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(6), 1500);

					break;

				case 6:
					for(L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if(pc.getPlanDistanceSq(_cameraMarker.get(3)) <= _distance)
						{
							_cameraMarker.get(3).broadcastPacket(new SpecialCamera(_cameraMarker.get(3).getObjectId(), 1872, 94, 15, 0, 15000));
						}
					}

					if(_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(7), 1);

					break;

				case 7:
					for(L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if(pc.getPlanDistanceSq(_cameraMarker.get(3)) <= _distance)
						{
							_cameraMarker.get(3).broadcastPacket(new SpecialCamera(_cameraMarker.get(3).getObjectId(), 1839, 92, 0, 1500, 15000));
						}
					}

					if(_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(8), 1500);

					break;

				case 8:
					for(L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if(pc.getPlanDistanceSq(_cameraMarker.get(2)) <= _distance)
						{
							_cameraMarker.get(2).broadcastPacket(new SpecialCamera(_cameraMarker.get(2).getObjectId(), 1872, 92, 15, 0, 15000));
						}
					}

					if(_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(9), 1);

					break;

				case 9:
					for(L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if(pc.getPlanDistanceSq(_cameraMarker.get(2)) <= _distance)
						{
							_cameraMarker.get(2).broadcastPacket(new SpecialCamera(_cameraMarker.get(2).getObjectId(), 1839, 90, 5, 1500, 15000));
						}
					}

					if(_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(10), 1500);

					break;

				case 10:
					for(L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if(pc.getPlanDistanceSq(_cameraMarker.get(1)) <= _distance)
						{
							_cameraMarker.get(1).broadcastPacket(new SpecialCamera(_cameraMarker.get(1).getObjectId(), 1872, 90, 5, 0, 15000));
						}
					}

					if(_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(11), 1);

					break;

				case 11:
					for(L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if(pc.getPlanDistanceSq(_cameraMarker.get(1)) <= _distance)
						{
							_cameraMarker.get(1).broadcastPacket(new SpecialCamera(_cameraMarker.get(1).getObjectId(), 2002, 90, 2, 1500, 15000));
						}
					}

					if(_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(12), 2000);

					break;

				case 12:
					for(L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if(pc.getPlanDistanceSq(_vanHalter) <= _distance)
						{
							_vanHalter.broadcastPacket(new SpecialCamera(_vanHalter.getObjectId(), 50, 90, 10, 0, 15000));
						}
					}

					if(_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(13), 1000);

					break;

				case 13:
					L2Skill skill = SkillTable.getInstance().getInfo(1168, 7);
					_ritualOffering.setIsInvul(false);
					_vanHalter.setTarget(_ritualOffering);
					_vanHalter.doCast(skill);

					if(_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(14), 4700);

					break;

				case 14:
					_ritualOffering.setIsInvul(false);
					_ritualOffering.reduceCurrentHp(_ritualOffering.getMaxHp() + 1, _vanHalter);

					if(_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(15), 4300);

					break;

				case 15:
					spawnRitualSacrifice();
					deleteRitualOffering();

					for(L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if(pc.getPlanDistanceSq(_vanHalter) <= _distance)
						{
							_vanHalter.broadcastPacket(new SpecialCamera(_vanHalter.getObjectId(), 100, 90, 15, 1500, 15000));
						}
					}

					if(_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(16), 2000);

					break;

				case 16:
					for(L2PcInstance pc : _vanHalter.getKnownList().getKnownPlayers().values())
					{
						if(pc.getPlanDistanceSq(_vanHalter) <= _distance)
						{
							_vanHalter.broadcastPacket(new SpecialCamera(_vanHalter.getObjectId(), 5200, 90, -10, 9500, 6000));
						}
					}

					if(_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(17), 6000);

					break;

				case 17:
					deleteRitualSacrifice();
					deleteCameraMarker();
					_vanHalter.setIsInvul(false);

					if(_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
					_movieTask = ThreadPoolManager.getInstance().scheduleGeneral(new Movie(18), 1000);

					break;

				case 18:
					combatBeginning();
					if(_movieTask != null)
					{
						_movieTask.cancel(false);
					}
					_movieTask = null;
			}
		}
	}

}