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
package com.src.gameserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.src.Config;
import com.src.gameserver.ai.special.AILoader;
import com.src.gameserver.cache.CrestCache;
import com.src.gameserver.cache.HtmCache;
import com.src.gameserver.communitybbs.Manager.ForumsBBSManager;
import com.src.gameserver.datatables.GMSkillTable;
import com.src.gameserver.datatables.GmListTable;
import com.src.gameserver.datatables.HeroSkillTable;
import com.src.gameserver.datatables.NobleSkillTable;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.csv.ExtractableItemsData;
import com.src.gameserver.datatables.sql.CharNameTable;
import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.datatables.sql.CustomArmorSetsTable;
import com.src.gameserver.datatables.sql.HennaTreeTable;
import com.src.gameserver.datatables.sql.ItemTable;
import com.src.gameserver.datatables.sql.OfflineTradersTable;
import com.src.gameserver.datatables.sql.SpawnTable;
import com.src.gameserver.datatables.xml.AccessLevels;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.datatables.xml.ArmorSetsTable;
import com.src.gameserver.datatables.xml.AugmentationData;
import com.src.gameserver.datatables.xml.CharTemplateTable;
import com.src.gameserver.datatables.xml.DoorTable;
import com.src.gameserver.datatables.xml.FishTable;
import com.src.gameserver.datatables.xml.HelperBuffTable;
import com.src.gameserver.datatables.xml.HennaTable;
import com.src.gameserver.datatables.xml.L2PetDataTable;
import com.src.gameserver.datatables.xml.LevelUpData;
import com.src.gameserver.datatables.xml.MapRegionTable;
import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.datatables.xml.NpcWalkerRoutesTable;
import com.src.gameserver.datatables.xml.OpenLockDoors;
import com.src.gameserver.datatables.xml.RecipeTable;
import com.src.gameserver.datatables.xml.SkillSpellbookTable;
import com.src.gameserver.datatables.xml.SkillTreeTable;
import com.src.gameserver.datatables.xml.StaticObjects;
import com.src.gameserver.datatables.xml.SummonItemsData;
import com.src.gameserver.datatables.xml.TeleportLocationTable;
import com.src.gameserver.datatables.xml.TerritoryTable;
import com.src.gameserver.datatables.xml.ZoneData;
import com.src.gameserver.geo.GeoData;
import com.src.gameserver.geo.pathfinding.PathFinding;
import com.src.gameserver.handler.AdminCommandHandler;
import com.src.gameserver.handler.AutoAnnouncementHandler;
import com.src.gameserver.handler.ChatHandler;
import com.src.gameserver.handler.ItemHandler;
import com.src.gameserver.handler.SkillHandler;
import com.src.gameserver.handler.UserCommandHandler;
import com.src.gameserver.handler.VoicedCommandHandler;
import com.src.gameserver.idfactory.IdFactory;
import com.src.gameserver.managers.AuctionManager;
import com.src.gameserver.managers.BoatManager;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.managers.CastleManorManager;
import com.src.gameserver.managers.ClanHallManager;
import com.src.gameserver.managers.CoupleManager;
import com.src.gameserver.managers.CrownManager;
import com.src.gameserver.managers.CursedWeaponsManager;
import com.src.gameserver.managers.DatatablesManager;
import com.src.gameserver.managers.DayNightSpawnManager;
import com.src.gameserver.managers.DimensionalRiftManager;
import com.src.gameserver.managers.DuelManager;
import com.src.gameserver.managers.FourSepulchersManager;
import com.src.gameserver.managers.FunEventsManager;
import com.src.gameserver.managers.GrandBossManager;
import com.src.gameserver.managers.ItemsOnGroundManager;
import com.src.gameserver.managers.MercTicketManager;
import com.src.gameserver.managers.PetitionManager;
import com.src.gameserver.managers.QuestManager;
import com.src.gameserver.managers.RaidBossPointsManager;
import com.src.gameserver.managers.RaidBossSpawnManager;
import com.src.gameserver.managers.SiegeManager;
import com.src.gameserver.model.AutoChatHandler;
import com.src.gameserver.model.L2Manor;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.PartyMatchRoomList;
import com.src.gameserver.model.PartyMatchWaitingList;
import com.src.gameserver.model.entity.Announcements;
import com.src.gameserver.model.entity.Hero;
import com.src.gameserver.model.entity.MonsterRace;
import com.src.gameserver.model.entity.clanhallsiege.BanditStrongholdSiege;
import com.src.gameserver.model.entity.clanhallsiege.DevastatedCastle;
import com.src.gameserver.model.entity.clanhallsiege.FortressOfResistance;
import com.src.gameserver.model.entity.clanhallsiege.WildBeastFarmSiege;
import com.src.gameserver.model.entity.market.Market;
import com.src.gameserver.model.entity.olympiad.Olympiad;
import com.src.gameserver.model.entity.sevensigns.SevenSigns;
import com.src.gameserver.model.entity.sevensigns.SevenSignsFestival;
import com.src.gameserver.model.spawn.AutoSpawn;
import com.src.gameserver.network.L2GameClient;
import com.src.gameserver.network.L2GamePacketHandler;
import com.src.gameserver.script.EventDroplist;
import com.src.gameserver.script.faenor.FaenorScriptEngine;
import com.src.gameserver.scripting.CompiledScriptCache;
import com.src.gameserver.scripting.L2ScriptEngineManager;
import com.src.gameserver.taskmanager.TaskManager;
import com.src.gameserver.thread.LoginServerThread;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.gameserver.thread.daemons.DeadlockDetector;
import com.src.gameserver.util.FloodProtector;
import com.src.gameserver.util.sql.SQLQueue;
import com.src.mmocore.SelectorConfig;
import com.src.mmocore.SelectorThread;
import com.src.util.Util;
import com.src.util.database.L2DatabaseFactory;
import com.src.util.protection.IPv4Filter;
import com.src.util.protection.nProtect;
import com.src.util.services.CreateFolders;
import com.src.util.services.Restart;
import com.src.util.services.ServerType;

public class GameServer
{
	private static Logger _log = Logger.getLogger("Loader");
	private static SelectorThread<L2GameClient> _selectorThread;
	private static LoginServerThread _loginThread;
	private static L2GamePacketHandler _gamePacketHandler;

	public static final Calendar dateTimeServerStarted = Calendar.getInstance();

	public static void main(String[] args) throws Exception
	{
		ServerType.serverMode = ServerType.MODE_GAMESERVER;
		long serverLoadStart = System.currentTimeMillis();

		CreateFolders.createFolders();

		InputStream is = new FileInputStream(new File("./config/other/log.cfg"));
		LogManager.getLogManager().readConfiguration(is);
		is.close();

		Util.printSection("Team");
		Util.team();

		Config.load();
		loadDatabase();
		loadScriptEngines();
		loadChache();
		loadL2World();
		loadAnnouncements();
		loadIdFactory();
		loadTeleports();
		loadSkils();
		loadItems();
		loadNpcs();
		loadCharacters();
		loadHenna();	
		loadHelperBuff();
		loadGeoData();
		loadTradeAndRecipes();
		loadProperty();
		loadCustomMods();
		loadSpawnLists();
		loadZones();
		loadRift();
		loadAugmention();
		loadCursedWeapons();
		loadManoor();
		loadPets();
		loadDoors();
		loadSevenSignsAnd4S();
		loadOlympiadAndHero();
		loadAccessLevel();
		loadHandlers();
		loadConquerableHalls();
		loadQuests();
		loadAi();

		Util.printSection("Scripts");
		try
		{
			File scripts = new File(Config.DATAPACK_ROOT + "/data/scripts/scripts.cfg");
			L2ScriptEngineManager.getInstance().executeScriptsList(scripts);
		}
		catch(IOException ioe)
		{
			_log.warning("Failed loading scripts.cfg, no script going to be loaded");
		}
		try
		{
			CompiledScriptCache compiledScriptCache = L2ScriptEngineManager.getInstance().getCompiledScriptCache();
			if(compiledScriptCache == null)
			{
				_log.info("Compiled Scripts Cache is disabled.");
			}
			else
			{
				compiledScriptCache.purge();
				if(compiledScriptCache.isModified())
				{
					compiledScriptCache.save();
					_log.info("Compiled Scripts Cache was saved.");
				}
				else
				{
					_log.info("Compiled Scripts Cache is up-to-date.");
				}
			}
		}
		catch(IOException e)
		{
			_log.warning("Failed to store Compiled Scripts Cache." + e);
		}

		QuestManager.getInstance().report();

		if(!Config.ALT_DEV_NO_SCRIPT)
		{
			FaenorScriptEngine.getInstance();
		}
		else
		{
			System.out.println("Script: disable load.");
		}

		Runtime.getRuntime().addShutdownHook(Shutdown.getInstance());

		Util.printSection("Info");
		Util.info();
		_log.info("GameServer: Loaded " + (System.currentTimeMillis() - serverLoadStart) / 1000 + " seconds");

		Util.printSection("Status");
		System.gc();

		_loginThread = LoginServerThread.getInstance();
		_loginThread.start();

		final SelectorConfig sc = new SelectorConfig();
		sc.MAX_READ_PER_PASS = com.src.mmocore.Config.MMO_MAX_READ_PER_PASS;
		sc.MAX_SEND_PER_PASS = com.src.mmocore.Config.MMO_MAX_SEND_PER_PASS;
		sc.SLEEP_TIME = com.src.mmocore.Config.MMO_SELECTOR_SLEEP_TIME;
		sc.HELPER_BUFFER_COUNT = com.src.mmocore.Config.MMO_HELPER_BUFFER_COUNT;

		_gamePacketHandler = new L2GamePacketHandler();

		_selectorThread = new SelectorThread<L2GameClient>(sc, _gamePacketHandler, _gamePacketHandler, _gamePacketHandler, new IPv4Filter());

		InetAddress bindAddress = null;
		if(!Config.GAMESERVER_HOSTNAME.equals("*"))
		{
			try
			{
				bindAddress = InetAddress.getByName(Config.GAMESERVER_HOSTNAME);
			}
			catch(UnknownHostException e1)
			{
				_log.log(Level.SEVERE, "WARNING: The GameServer bind address is invalid, using all avaliable IPs. Reason: " + e1.getMessage(), e1);
			}
		}

		try
		{
			_selectorThread.openServerSocket(bindAddress, Config.PORT_GAME);
		}
		catch(IOException e)
		{
			_log.log(Level.SEVERE, "FATAL: Failed to open server socket. Reason: " + e.getMessage(), e);
			System.exit(1);
		}

		_selectorThread.start();
	}
	
	private static void loadDatabase() throws Exception
	{
		Util.printSection("Database");
		L2DatabaseFactory.getInstance();
		DatatablesManager.LoadSTS();
		if(Config.DEADLOCKCHECK_INTIAL_TIME > 0)
		{
			ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(DeadlockDetector.getInstance(), Config.DEADLOCKCHECK_INTIAL_TIME, Config.DEADLOCKCHECK_DELAY_TIME);
		}
		SQLQueue.getInstance();
	}
	
	private static void loadScriptEngines()
	{
		Util.printSection("Script Engines");
		L2ScriptEngineManager.getInstance();
		ThreadPoolManager.getInstance();
		nProtect.getInstance();
	}

	private static void loadChache()
	{
		Util.printSection("Cache");
		HtmCache.getInstance();
		CrestCache.getInstance();
	}
	
	private static void loadL2World()
	{
		Util.printSection("World");
		L2World.getInstance();
		MapRegionTable.getInstance();
		TerritoryTable.getInstance();
		GameTimeController.getInstance();
		DuelManager.getInstance();
		CharNameTable.getInstance();
		PartyMatchWaitingList.getInstance();
		PartyMatchRoomList.getInstance();
	}
	
	private static void loadAnnouncements()
	{
		Util.printSection("Announcements");
		Announcements.getInstance();
		AutoAnnouncementHandler.getInstance();
	}
	
	private static void loadSkils()
	{
		Util.printSection("Skills");
		SkillTable.getInstance();
		SkillTreeTable.getInstance();
		SkillSpellbookTable.getInstance();
		NobleSkillTable.getInstance();
		GMSkillTable.getInstance();
		HeroSkillTable.getInstance();
	}
	
	private static void loadItems()
	{
		Util.printSection("Items");
		ItemTable.getInstance();
		ArmorSetsTable.getInstance();
		
		if(Config.CUSTOM_ARMORSETS_TABLE)
			CustomArmorSetsTable.getInstance();
		
		ExtractableItemsData.getInstance();
		SummonItemsData.getInstance();
		StaticObjects.getInstance();
		
		if(Config.ALLOW_FISHING)
			FishTable.getInstance();
		if(Config.SAVE_DROPPED_ITEM)
			ItemsOnGroundManager.getInstance();
		if(Config.AUTODESTROY_ITEM_AFTER > 0 || Config.HERB_AUTO_DESTROY_TIME > 0)
			ItemsAutoDestroy.getInstance();
	}
	
	private static void loadNpcs()
	{
		Util.printSection("Npc");
		NpcWalkerRoutesTable.getInstance().load();
		NpcTable.getInstance();
	}
	
	private static void loadCharacters()
	{
		Util.printSection("Characters");
		ClanTable.getInstance();
		CharTemplateTable.getInstance();
		LevelUpData.getInstance();
	}
	
	private static void loadCustomMods()
	{
		Util.printSection("Custom Mods");
		
		if (Config.ENABLE_FLOOD_PROTECTOR)
		{
			FloodProtector.getInstance();
		}
		else
		{
			_log.info("[Flood Protection] System is Disabled !");
		}

		if(Config.RESTART_BY_TIME_OF_DAY)
		{
			Restart.getInstance().StartCalculationOfNextRestartTime();
		}
		else
		{
			_log.info("[Auto Restart] System is Disabled !");
		}
     
     System.gc();
	
		if(Config.COMMUNITY_TYPE.equals("full"))
			ForumsBBSManager.getInstance().initRoot();

		if((Config.OFFLINE_TRADE_ENABLE || Config.OFFLINE_CRAFT_ENABLE) && Config.OFFLINE_RESTORE)
			OfflineTradersTable.restoreOfflineTraders();

		Market.getInstance();

		if(Config.ALLOW_WEDDING)
			CoupleManager.getInstance();
		else
			_log.info("Wedding Manager: Disabled");
		
		FunEventsManager.getInstance().autoStartEvents();
	}
	
	private static void loadGeoData()
	{
		Util.printSection("Geodata");
		GeoData.getInstance();
		if(Config.GEODATA == 2)
			PathFinding.getInstance();
	}
	
	private static void loadIdFactory()
	{
		Util.printSection("ID Factory");
		IdFactory.getInstance();
	}
	
	private static void loadHenna()
	{
		Util.printSection("Henna");
		HennaTable.getInstance();
		HennaTreeTable.getInstance();
	}
	
	private static void loadTeleports()
	{
		Util.printSection("Teleport");
		TeleportLocationTable.getInstance();
	}
	
	private static void loadHelperBuff()
	{
		Util.printSection("Helper Buff");
		HelperBuffTable.getInstance();
	}
	
	private static void loadDoors()
	{
		Util.printSection("Doors");
		DoorTable.getInstance().parseData();
		OpenLockDoors.getInstance();
		
		try
		{
			DoorTable doorTable = DoorTable.getInstance();
			doorTable.getDoor(19160010).openMe();
			doorTable.getDoor(19160011).openMe();
			doorTable.getDoor(19160012).openMe();
			doorTable.getDoor(19160013).openMe();
			doorTable.getDoor(19160014).openMe();
			doorTable.getDoor(19160015).openMe();
			doorTable.getDoor(19160016).openMe();
			doorTable.getDoor(19160017).openMe();
			doorTable.getDoor(24190001).openMe();
			doorTable.getDoor(24190002).openMe();
			doorTable.getDoor(24190003).openMe();
			doorTable.getDoor(24190004).openMe();
			doorTable.getDoor(23180001).openMe();
			doorTable.getDoor(23180002).openMe();
			doorTable.getDoor(23180003).openMe();
			doorTable.getDoor(23180004).openMe();
			doorTable.getDoor(23180005).openMe();
			doorTable.getDoor(23180006).openMe();
			doorTable.checkAutoOpen();
			doorTable = null;
		}
		catch (NullPointerException e)
		{
		}
	}
	
	private static void loadProperty()
	{
		Util.printSection("Castle");
		CastleManager.getInstance();
		SiegeManager.getInstance();
		CrownManager.getInstance();

		Util.printSection("Clan Halls");
		ClanHallManager.getInstance();

		Util.printSection("Auction");
		AuctionManager.getInstance();
	}
	
	private static void loadTradeAndRecipes()
	{
		Util.printSection("Trade & Recipe");
		TradeController.getInstance();
		RecipeController.getInstance();
		RecipeTable.getInstance();
	}
	
	private static void loadSpawnLists()
	{
		Util.printSection("Spawnlist");
		if(!Config.ALT_DEV_NO_SPAWNS)
			SpawnTable.getInstance();
		else
			_log.info("Spawn: Disable load.");

		if(!Config.ALT_DEV_NO_RB)
		{
			RaidBossSpawnManager.getInstance();
			GrandBossManager.getInstance();
			RaidBossPointsManager.init();
		}
		else
		{
			_log.info("RaidBoss: Disable load.");
		}
		DayNightSpawnManager.getInstance().notifyChangeMode();
	}
	
	private static void loadHandlers()
	{
		Util.printSection("Handlers");
		AutoSpawn.getInstance();
		AutoChatHandler.getInstance();
		ChatHandler.getInstance();
		ItemHandler.getInstance();
		SkillHandler.getInstance();
		AdminCommandHandler.getInstance();
		UserCommandHandler.getInstance();
		VoicedCommandHandler.getInstance();
		
		_log.info("AutoChatHandler: Loaded " + AutoChatHandler.getInstance().size() + " handlers.");
		_log.info("SpawnHandler: Loaded " + AutoSpawn.getInstance().size() + " handlers.");
	}
	
	private static void loadSevenSignsAnd4S()
	{
		Util.printSection("Seven Signs");
		SevenSigns.getInstance();
		SevenSignsFestival.getInstance();
		EventDroplist.getInstance();
		MonsterRace.getInstance();
		TaskManager.getInstance();
		
		Util.printSection("Four Sepulchers");
		FourSepulchersManager.getInstance().init();
	}
	
	private static void loadOlympiadAndHero() throws Exception
	{
		Util.printSection("Olympiad System");
		Olympiad.getInstance().load();

		Util.printSection("Hero System");
		Hero.getInstance();
	}
	
	private static void loadAccessLevel()
	{
		Util.printSection("Access Levels");
		AccessLevels.getInstance();
		AdminCommandAccessRights.getInstance();
		GmListTable.getInstance();
	}
	
	private static void loadConquerableHalls()
	{
		Util.printSection("Conquerable Halls");
		BanditStrongholdSiege.getInstance();
		WildBeastFarmSiege.getInstance();
		DevastatedCastle.getInstance();
		FortressOfResistance.getInstance();
	}
	
	private static void loadZones()
	{
		Util.printSection("Zone");
		ZoneData.getInstance();
	}
	
	private static void loadAugmention()
	{
		Util.printSection("Augmention");
		AugmentationData.getInstance();
	}
	
	private static void loadRift()
	{
		Util.printSection("Dimensional Rift");
		DimensionalRiftManager.getInstance();
	}
	
	private static void loadCursedWeapons()
	{
		Util.printSection("Cursed Weapons");
		CursedWeaponsManager.getInstance();
	}
	
	private static void loadManoor()
	{
		Util.printSection("Manor");
		L2Manor.getInstance();
		CastleManorManager.getInstance();
		MercTicketManager.getInstance();
		PetitionManager.getInstance();

		// Boats
		BoatManager.getInstance();
	}
	
	private static void loadPets()
	{
		Util.printSection("Pets");
		L2PetDataTable.getInstance().loadPetsData();
	}
	
	private static void loadQuests()
	{
		Util.printSection("Quests");
		if(!Config.ALT_DEV_NO_QUESTS)
			QuestManager.getInstance();
		else
			_log.info("Quest: disable load.");
	}
	
	private static void loadAi()
	{
		Util.printSection("AI");
		if(!Config.ALT_DEV_NO_AI)
			AILoader.init();
		else
			_log.info("AI: disable load.");
	}

	public static SelectorThread<L2GameClient> getSelectorThread()
	{
		return _selectorThread;
	}
	
	public static void stopGameServer()
	{
		return;
	}
}