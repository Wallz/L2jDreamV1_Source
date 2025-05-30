package com.src.gameserver.model.entity.event;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import com.src.Config;
import com.src.gameserver.datatables.xml.DoorTable;
import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.idfactory.IdFactory;
import com.src.gameserver.managers.FunEventsManager;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2CustomEventManagerInstance;
import com.src.gameserver.model.actor.instance.L2DoorInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.Announcements;
import com.src.gameserver.model.entity.olympiad.Olympiad;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.clientpackets.Say2;
import com.src.gameserver.network.serverpackets.ConfirmDlg;
import com.src.gameserver.network.serverpackets.CreatureSay;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.services.Localization;
import com.src.gameserver.services.Messages;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;
import com.src.util.random.Rnd;

public abstract class FunEvent
{
	protected static final Logger _log = Logger.getLogger(FunEvent.class.getName());

	protected TIntObjectHashMap<L2PcInstance>	_players			= new TIntObjectHashMap<L2PcInstance>();
	protected TIntObjectHashMap<Team>			_teams				= new TIntObjectHashMap<Team>();
	protected State								_state				= State.INACTIVE;
	protected Future<?>							_sheduleNext;
	protected long								_startNextTime 		= 0;
	protected countdownTask						_countdownTask;
	protected final int							_ManagerId			= 97001;
	protected L2CustomEventManagerInstance		_Manager;
	protected ArrayList<L2DoorInstance>			_doors				= new ArrayList<L2DoorInstance>();
	protected Future<?>					        _checkActivityTask;

	public int EVENT_ID;
	public String EVENT_NAME;
	public int EVENT_FULL_NAME;
	protected boolean EVENT_AUTO_MODE;
	protected String[] EVENT_INTERVAL;
	protected int[] EVENT_NPC_LOC;
	protected String EVENT_NPC_LOC_NAME;
	public boolean EVENT_JOIN_CURSED = true;
	protected String EVENT_TEAMS_TYPE;
	public int EVENT_PLAYER_LEVEL_MIN;
	public int EVENT_PLAYER_LEVEL_MAX;
	protected int EVENT_COUNTDOWN_TIME;
	protected int EVENT_MIN_PLAYERS;
	protected ArrayList<Integer> EVENT_DOORS_TO_CLOSE;
	protected ArrayList<Integer> EVENT_DOORS_TO_OPEN;

	public enum State
	{
		INACTIVE,
		WAITING,
		PARTICIPATING,
		STARTING,
		FIGHTING
	}

	public FunEvent()
	{
		loadConfig();
	}

	public State getState()
	{
		return _state;
	}

	protected Team getTeam(int team)
	{
		return _teams.get(team);
	}

	protected Team[] getAllTeams()
	{
		return _teams.values(new Team[_teams.size()]);
	}

	protected L2PcInstance[] getAllPlayers()
	{
		return _players.values(new L2PcInstance[_players.size()]);
	}

	public int getStartNextTime()
	{
		long currentTime = Calendar.getInstance().getTimeInMillis();
		return (_startNextTime > currentTime) ? (int) ((_startNextTime - currentTime)/1000) : 0;
	}

	protected void spawnManager()
	{
		try
		{
			_Manager = new L2CustomEventManagerInstance(IdFactory.getInstance().getNextId(), NpcTable.getInstance().getTemplate(_ManagerId));
			_Manager._event = this;
			_Manager.setTitle(EVENT_NAME + " Manager");
			_Manager.spawnMe(EVENT_NPC_LOC[0], EVENT_NPC_LOC[1], EVENT_NPC_LOC[2]);
			_Manager.doRevive();
			_Manager.setIsDead(false);
			_Manager.setIsKilledAlready(false);
			_Manager.setCurrentHpMp(_Manager.getMaxHp(), _Manager.getMaxMp());
			Messages msg = new Messages(1, true);
			msg.add(EVENT_NAME);
			msg.add(_Manager.getName());
			msg.add(EVENT_NPC_LOC_NAME);
			AnnounceToPlayers(true, msg);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	protected void unspawnManager()
	{
		try
		{
			if (_Manager == null)
				return;

			_Manager.deleteMe();
			_Manager = null;
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
	}

	public void startEvent()
	{
		loadConfig();
		if (_state != State.INACTIVE || EVENT_AUTO_MODE)
			return;

		_state = State.WAITING;
		sheduleNext(0);
	}
	
	public void requestAbortEvent()
	{	
		if (FunEventsManager.getInstance().getActiveEvents().contains(this))
		{
			Messages msg = new Messages(63, true);
			msg.add(EVENT_NAME);
			AnnounceToPlayers(true, msg);
			abortEvent();
			FunEventsManager.getInstance().getActiveEvents().remove(this);
		}
	}

	public void autoStart()
	{
		loadConfig();
		if (_state != State.INACTIVE || !EVENT_AUTO_MODE)
			return;

		try
		{
			Calendar currentTime = Calendar.getInstance();
			Calendar nextStartTime = null;
			Calendar testStartTime;
			for (String timeOfDay : EVENT_INTERVAL)
			{
				// Creating a Calendar object from the specified interval value
				testStartTime = Calendar.getInstance();
				testStartTime.setLenient(true);
				String[] splitTimeOfDay = timeOfDay.split(":");
				testStartTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(splitTimeOfDay[0]));
				testStartTime.set(Calendar.MINUTE, Integer.parseInt(splitTimeOfDay[1]));
				// If the date is in the past, make it the next day (Example: Checking for "1:00", when the time is 23:57.)
				if (testStartTime.getTimeInMillis() < currentTime.getTimeInMillis())
				{
					testStartTime.add(Calendar.DAY_OF_MONTH, 1);
				}
				// Check for the test date to be the minimum (smallest in the specified list)
				if (nextStartTime == null || testStartTime.getTimeInMillis() < nextStartTime.getTimeInMillis())
				{
					nextStartTime = testStartTime;
				}
			}
			if (nextStartTime != null)
			{
				_log.info(EVENT_NAME+"EventEngine["+EVENT_NAME+".autoStart()]: "+EVENT_NAME+" AUTOSTART in " + (nextStartTime.getTimeInMillis() - currentTime.getTimeInMillis()) + " ms.");
				_state = State.WAITING;
				sheduleNext(nextStartTime.getTimeInMillis() - currentTime.getTimeInMillis());
			}
		}
		catch (Exception e)
		{
			_log.warning(EVENT_NAME+"EventEngine["+EVENT_NAME+".autoStart()]: Error figuring out a start time. Check "+EVENT_NAME+"EventInterval in config file.");
		}
	}

	public void AnnounceToPlayers(Boolean toall, String announce)
	{
		if (toall)
			Announcements.getInstance().announceToAll(announce);
		else
		{
			CreatureSay cs = new CreatureSay(0, Say2.ANNOUNCEMENT, "", announce);
			if (_players != null && !_players.isEmpty())
			{
				for (L2PcInstance player : _players.values(new L2PcInstance[_players.size()]))
				{
					if (player != null && player.isOnline() != 0)
						player.sendPacket(cs);
				}
			}
		}
	}
	
	public void AnnounceToPlayers(Boolean toall, Messages msg)
	{
		if (toall)
			Announcements.getInstance().announceToAll(msg);
		else
		{
			if (_players != null && !_players.isEmpty())
			{
				for (L2PcInstance player : _players.values(new L2PcInstance[_players.size()]))
				{
					CreatureSay cs = new CreatureSay(0, Say2.ANNOUNCEMENT, "", msg.toString(player.getLang()));
					if (player.isOnline() != 0)
						player.sendPacket(cs);
				}
			}
		}
	}

	public void addPlayer(L2PcInstance player, int joinTeamId)
	{
		String lang = player.getLang();
		if (player.getLevel() < EVENT_PLAYER_LEVEL_MIN || player.getLevel() > EVENT_PLAYER_LEVEL_MAX)
		{
			player.sendMessage(Localization.getInstance().getString(lang, 2));
			return;
		}
		if (!EVENT_JOIN_CURSED && player.isCursedWeaponEquipped())
		{
			player.sendMessage(Localization.getInstance().getString(lang, 3));
			return;
		}
		
		if(player.getOlympiadGameId() > 0 || player.isInOlympiadMode() || Olympiad.getInstance().isRegistered(player))
		{
			return;
		}
		
		if (!Config.ALLOW_DUALBOX_EVENT)
		{
		   String _IPADD1;
		   String _IPADD2 = player.getClient().getConnection().getInetAddress().getHostAddress();
		   
		   	// get all player from FunEvent and check if ip addres already exists
		   	for (L2PcInstance playr : getAllPlayers())
		   	{
			   _IPADD1 = playr.getClient().getConnection().getInetAddress().getHostAddress();
			   if (_IPADD1.equals(_IPADD2))
			   {
				   player.sendMessage("No dual box allowed in event!");
				   return;
			   }    
		   	}
		}
		if (EVENT_TEAMS_TYPE.equals("SHUFFLE"))
		{
			player._eventName = EVENT_NAME;
			_players.put(player.getObjectId(), player);
		}
		else if (EVENT_TEAMS_TYPE.equals("BALANCE"))
		{
			int topPlayersCount = 0;
			int topPlayersCountTeam = 0;
			int minPlayersCount = 0;
			int minPlayersCountTeam = 0;

			for (Team team : _teams.values(new Team[_teams.size()]))
			{
				if (team._playersCount <= minPlayersCount)
				{
					minPlayersCount = team._playersCount;
					minPlayersCountTeam = team._teamId;
				}
				if (team._playersCount >= topPlayersCount)
				{
					topPlayersCount = team._playersCount;
					topPlayersCountTeam = team._teamId;
				}
			}

			if (topPlayersCountTeam == minPlayersCountTeam || joinTeamId != topPlayersCountTeam)
			{
				player._eventName = EVENT_NAME;
				player._eventTeamId = joinTeamId;
				_teams.get(joinTeamId)._playersCount++;
				_players.put(player.getObjectId(), player);
			}
			else
			{
				player.sendMessage(Localization.getInstance().getString(lang, 4));
			}
		}
	}

	public void removePlayer(L2PcInstance player)
	{
		if (EVENT_TEAMS_TYPE.equals("SHUFFLE"))
		{
			if (_players.containsKey(player.getObjectId()))
			{
				player._eventTeamId = 0;
				_players.remove(player.getObjectId());
			}
		}
		else if (EVENT_TEAMS_TYPE.equals("BALANCE"))
		{
			if (_players.containsKey(player.getObjectId()))
			{
				_teams.get(player._eventTeamId)._playersCount--;
				player._eventTeamId = 0;
				_players.remove(player.getObjectId());
			}
		}
	}

	protected void sheduleNext(long delay)
	{
		_startNextTime = Calendar.getInstance().getTimeInMillis() + delay;
		_sheduleNext = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				StartNext();
			}
		}, delay);
	}

	protected abstract void loadConfig();
	public abstract void abortEvent();
	protected abstract void StartNext();

	public boolean onPlayerDie(final L2PcInstance player, final L2PcInstance killer)
	{
		return true;
	}

	public void onPlayerLogin(final L2PcInstance player) {}
	public void onPlayerLogout(final L2PcInstance player) {}

	public NpcHtmlMessage getChatWindow(L2PcInstance player)
	{
		if (_state != State.PARTICIPATING)
			return null;

		String joinType = "";
		String teamsInfo = "";
		String countDownTimer;
		int timeLeft = getStartNextTime();

		String lang = player.getLang();

		if (EVENT_TEAMS_TYPE.equals("SHUFFLE"))
			joinType = Localization.getInstance().getString(lang, 5);
		else if (EVENT_TEAMS_TYPE.equals("BALANCE"))
			joinType = Localization.getInstance().getString(lang, 6);

		if (timeLeft > 60)
			 countDownTimer = timeLeft/60 + " "+Localization.getInstance().getString(lang, 8);
		else
			 countDownTimer = timeLeft + " "+Localization.getInstance().getString(lang, 9);

		NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(5);

		if (!_players.containsKey(player.getObjectId()))
		{
			String joiningButtons = "";

			if (EVENT_TEAMS_TYPE.equals("SHUFFLE"))
			{
				joiningButtons = "<center><button value=\""+Localization.getInstance().getString(lang, 10)+"\" action=\"bypass -h npc_%objectId%_join 0\" width=204 height=20 back=\"sek.cbui36\" fore=\"sek.cbui75\"></center>";
			}
			else if (EVENT_TEAMS_TYPE.equals("BALANCE"))
			{
				teamsInfo += "<br>"+Localization.getInstance().getString(lang, 11)+":<br>";
				for (Team team : _teams.values(new Team[_teams.size()]))
					teamsInfo += "<font color=" + team._teamColor + ">" + team._teamName + "</font>: " + team._playersCount + "<br>";

				joiningButtons += "<br>";
				for (Team team : _teams.values(new Team[_teams.size()]))
					joiningButtons += "<center><button value=\""+Localization.getInstance().getString(lang, 12)+" " + team._teamName + "\" action=\"bypass -h npc_%objectId%_join " + team._teamId + "\" width=204 height=20 back=\"sek.cbui36\" fore=\"sek.cbui75\"></center>";
			}

			npcHtmlMessage.setFile(lang, "data/html/addon/EventManager/joining.htm");
			npcHtmlMessage.replace("%eventName%", EVENT_NAME);
			npcHtmlMessage.replace("%joinType%", joinType);
			npcHtmlMessage.replace("%playerLevels%", EVENT_PLAYER_LEVEL_MIN + "-" + EVENT_PLAYER_LEVEL_MAX);
			npcHtmlMessage.replace("%teamsCount%", _teams.size());
			npcHtmlMessage.replace("%playersCount%", _players.size());
			npcHtmlMessage.replace("%playersCountMin%", EVENT_MIN_PLAYERS);
			npcHtmlMessage.replace("%teamsInfo%", teamsInfo);
			npcHtmlMessage.replace("%countdownTime%", countDownTimer);
			npcHtmlMessage.replace("%joiningButtons%", joiningButtons);
		}
		else
		{
			if (EVENT_TEAMS_TYPE.equals("BALANCE"))
			{
				teamsInfo += "<br>"+Localization.getInstance().getString(lang, 11)+":<br>";
				for (Team team : _teams.values(new Team[_teams.size()]))
					teamsInfo += "<font color=" + team._teamColor + ">" + team._teamName + "</font>: " + team._playersCount + "<br>";
			}
			Team team = _teams.get(player._eventTeamId);
			String playerTeamName  = (team == null) ? Localization.getInstance().getString(lang, 13) : team._teamName;
			String playerTeamColor = (team == null) ? "LEVEL" : team._teamColor;

			npcHtmlMessage.setFile(lang, "data/html/addon/EventManager/joined.htm");
			npcHtmlMessage.replace("%eventName%", EVENT_NAME);
			npcHtmlMessage.replace("%joinType%", joinType);
			npcHtmlMessage.replace("%playerTeamName%", playerTeamName);
			npcHtmlMessage.replace("%playerTeamColor%", playerTeamColor);
			npcHtmlMessage.replace("%teamsCount%", _teams.size());
			npcHtmlMessage.replace("%playersCount%", _players.size());
			npcHtmlMessage.replace("%playersCountMin%", EVENT_MIN_PLAYERS);
			npcHtmlMessage.replace("%teamsInfo%", teamsInfo);
			npcHtmlMessage.replace("%countdownTime%", countDownTimer);
		}
		return npcHtmlMessage;
	}

	public String getInfo(String lang)
	{
		String name = EVENT_NAME + " ("+Localization.getInstance().getString(lang, EVENT_FULL_NAME)+")";
		String info = "";
		String state = null;
		int timer = 0;
		int timeLeft = getStartNextTime();

		switch (_state)
		{
			case INACTIVE:
				state = Localization.getInstance().getString(lang, 18);
				break;
			case WAITING:
				state = Localization.getInstance().getString(lang, 19);
				timer = timeLeft;
				break;
			case PARTICIPATING:
				state = Localization.getInstance().getString(lang, 20);
				timer = timeLeft;
				break;
			case STARTING:
				state = Localization.getInstance().getString(lang, 21);
				break;
			case FIGHTING:
				state = Localization.getInstance().getString(lang, 22);
				timer = timeLeft;
				break;
		}

		info += "<br>"+Localization.getInstance().getString(lang, 23)+":&nbsp;<font color=\"LEVEL\">" + name + "</font><br1>";
		info += Localization.getInstance().getString(lang, 24)+"State:&nbsp;<font color=\"LEVEL\">" + state + "</font><br1>";

		String left = Localization.getInstance().getString(lang, 25);
		if (timer > 0)
		{
			if (timer > 3600)
				info += left+":&nbsp;<font color=\"LEVEL\">> " + timer/3600 + "</font> "+Localization.getInstance().getString(lang, 7)+"<br1>";
			else if (timer > 60)
				info += left+":&nbsp;<font color=\"LEVEL\">" + timer/60 + "</font> "+Localization.getInstance().getString(lang, 8)+"<br1>";
			else
				info += left+":&nbsp;<font color=\"LEVEL\">" + timer + "</font> "+Localization.getInstance().getString(lang, 9)+"<br1>";
		}

		return info;
	}

	protected boolean checkPlayersCount()
	{
		L2PcInstance[] players = getAllPlayers();
		for (L2PcInstance player : players)
		{
			if (player.isOnline() != 1)
				_players.remove(player.getObjectId());
		}
		if (_players == null || _players.isEmpty() || _players.size() < EVENT_MIN_PLAYERS)
		{
			Messages msg = new Messages(26, true);
			msg.add(EVENT_MIN_PLAYERS);
			msg.add(_players.size());
			AnnounceToPlayers(true, msg);
			return false;
		}

		return true;
	}

	protected void teleportPlayers()
	{
		L2PcInstance[] players = getAllPlayers();
		if (EVENT_TEAMS_TYPE.equals("SHUFFLE"))
		{
			int index = 1;

			for (L2PcInstance player : players)
			{
				player._eventName = EVENT_NAME;
				player._eventTeamId = index;
				if (index < _teams.size())
					index++;
				else
					index = 1;
				
				if(player.getOlympiadGameId() > 0 || player.isInOlympiadMode() || Olympiad.getInstance().isRegistered(player))
				{
					return;
				}
			}
		}
		
		for (L2PcInstance player : players)
			updatePlayerInfo(player);

		Messages msg = new Messages(27, true);
		msg.add(EVENT_NAME);
		AnnounceToPlayers(false, msg);
	}

	protected void updatePlayerInfo(L2PcInstance player)
	{
			// Not used just make warrnings
			// Team team = _teams.get(player._eventTeamId);

			player._eventOriginalTitle = player.getTitle();
			player._eventOriginalKarma = player.getKarma();
			//player._eventOriginalNameColor = player.getAppearance().getNameColor();
			//if (!team._teamColor.equals("None"))
				//player.getAppearance().setNameColor(team._teamColor);
			player.setKarma(0);
			player.broadcastUserInfo();
	}

	protected void kickPlayerFromEvent(final L2PcInstance player)
	{
		int x = EVENT_NPC_LOC[0] + Rnd.get(-50, 50);
		int y = EVENT_NPC_LOC[1] + Rnd.get(-50, 50);
		int z = EVENT_NPC_LOC[2];
		if (player.isOnline() != 0)
		{
			//player.getAppearance().setNameColor(player._eventOriginalNameColor);
			//player.getAppearance().setVisibleTitle(player._eventOriginalTitle);
			player.setKarma(player._eventOriginalKarma);
			player.setTeam(0);
			player.teleToLocation(x, y, z, true);
			if(player.isDead())
				player.doRevive();
			else
			{
				player.getStatus().setCurrentHp(player.getMaxHp());
				player.getStatus().setCurrentMp(player.getMaxMp());
				player.getStatus().setCurrentCp(player.getMaxCp());
			}
			player._eventName = "";
			player._eventTeamId = 0;
			player._eventCountKills = 0;
	        player.broadcastTitleInfo();
	        player.broadcastStatusUpdate();
	        player.broadcastUserInfo();
	        _players.remove(player.getObjectId());
			player._eventTeleported = false;
		}
		else
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();

				PreparedStatement statement = con.prepareStatement("UPDATE characters SET x=?, y=?, z=?, karma=? WHERE char_name=?");
				statement.setInt(1, x);
				statement.setInt(2, y);
				statement.setInt(3, z);
				statement.setInt(4, player._eventOriginalKarma);
				statement.setString(5, player.getName());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warning(EVENT_NAME + "EventEngine["+EVENT_NAME+".kickPlayerFromEvent()]: Error while updating player's " + player.getName() + " data: " + e);
			}
			finally
			{
				ResourceUtil.closeConnection(con);
			}
		}
	}

	protected void kickPlayersFromEvent()
	{
		for (L2PcInstance player : _players.values(new L2PcInstance[_players.size()]))
		{
			kickPlayerFromEvent(player);
		}
	}

	protected void countdown()
	{
		_countdownTask = new countdownTask(EVENT_COUNTDOWN_TIME * 60); // in seconds
		ThreadPoolManager.getInstance().scheduleGeneral(_countdownTask, 1000);
	}

	protected class countdownTask implements Runnable
	{
		private boolean _firstmessage = true;
		public long _countdownTime;

		public countdownTask(long time)
		{
			_countdownTime = time;
		}

		@Override
		public void run()
		{
			if (_state != State.PARTICIPATING)
					return;

			if (_countdownTime == 3600 || _countdownTime == 1800 || _countdownTime == 600 || _countdownTime == 60 || _firstmessage)
			{
				Messages msg = new Messages(62, true);
				msg.add(EVENT_NAME);
				msg.add(_countdownTime/60);
				AnnounceToPlayers(true, msg);
				_firstmessage = false;
			}

			_countdownTime--;

			if (_countdownTime > 0)
				ThreadPoolManager.getInstance().scheduleGeneral(this, 1000);
		}
	}


	protected void makeDoors()
	{

			for (int doorId : EVENT_DOORS_TO_OPEN)
			{
				if (doorId == 0)
					break;
				L2DoorInstance doorInstance = DoorTable.getInstance().getDoor(doorId);
				if (doorInstance != null)
					doorInstance.openMe();
			}
			for (int doorId : EVENT_DOORS_TO_CLOSE)
			{
				if (doorId == 0)
					break;
				L2DoorInstance doorInstance = DoorTable.getInstance().getDoor(doorId);
				if (doorInstance != null)
					doorInstance.closeMe();
			}
	}

	protected void removeDoors()
	{
			for (int doorId : EVENT_DOORS_TO_OPEN)
			{
				if (doorId == 0)
					break;
				L2DoorInstance doorInstance = DoorTable.getInstance().getDoor(doorId);
				if (doorInstance != null)
					doorInstance.closeMe();
			}
			for (int doorId : EVENT_DOORS_TO_CLOSE)
			{
				if (doorId == 0)
					break;
				L2DoorInstance doorInstance = DoorTable.getInstance().getDoor(doorId);
				if (doorInstance != null)
					doorInstance.openMe();
			}
	}

	public void sendConfirmDialog()
	{
		if (Config.EVENT_SHOW_JOIN_DIALOG && EVENT_TEAMS_TYPE.equals("SHUFFLE"))
		{
			for (L2PcInstance onlinePlayer : L2World.getInstance().getAllPlayers())
			{
				if (onlinePlayer.isOnline() == 1 && onlinePlayer.getEventName().equals(""))
				{
					Messages msg = new Messages(28, onlinePlayer.getLang());
					msg.add(EVENT_NAME + " (" + Localization.getInstance().getString(onlinePlayer.getLang(), EVENT_FULL_NAME) + ")");
					//ConfirmDlg dlg = new ConfirmDlg(614);
					ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.S1.getId());
					dlg.addString(msg.toString());
					dlg.addTime(30000);
					dlg.addRequesterId(EVENT_ID);
					onlinePlayer.sendPacket(dlg);
				}
			}
		}
	}

	public void recieveConfirmDialog(L2PcInstance player, int answer)
	{
		if (EVENT_TEAMS_TYPE.equals("SHUFFLE") && answer == 1)
			addPlayer(player, 0);
	}

	protected class Team
	{
		public int 			_teamId;
		public String 		_teamName;
		public int 			_teamX;
		public int			_teamY;
		public int			_teamZ;
		public String		_teamColor = "None";
		public int			_teamKills;
		public int			_playersCount;
	}
	
	public String getName()
	{
		return EVENT_NAME;
	}
}