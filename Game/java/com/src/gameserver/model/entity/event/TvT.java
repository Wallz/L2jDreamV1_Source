package com.src.gameserver.model.entity.event;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import com.src.Config;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.model.entity.olympiad.Olympiad;
import com.src.gameserver.services.Localization;
import com.src.gameserver.services.Messages;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class TvT extends FunEvent
{
	private final static Logger _log = Logger.getLogger(TvT.class.getName());
	
	@Override
	public void loadConfig() // Configs
	{
		EVENT_ID = 1;
		EVENT_NAME = "TVT";
		EVENT_FULL_NAME = 17;
		EVENT_AUTO_MODE = Config.TVT_AUTO_MODE;
		EVENT_INTERVAL = Config.TVT_EVENT_INTERVAL;
		EVENT_NPC_LOC = (new int[] {Config.TVT_NPC_X, Config.TVT_NPC_Y, Config.TVT_NPC_Z});
		EVENT_NPC_LOC_NAME = Config.TVT_NPC_LOC_NAME;
		EVENT_TEAMS_TYPE = Config.TVT_EVEN_TEAMS;
		EVENT_PLAYER_LEVEL_MIN = Config.TVT_PLAYER_LEVEL_MIN;
		EVENT_PLAYER_LEVEL_MAX = Config.TVT_PLAYER_LEVEL_MAX;
		EVENT_COUNTDOWN_TIME = Config.TVT_COUNTDOWN_TIME;
		EVENT_MIN_PLAYERS = Config.TVT_MIN_PLAYERS;
		EVENT_DOORS_TO_CLOSE = Config.TVT_DOORS_TO_CLOSE;
		EVENT_DOORS_TO_OPEN = Config.TVT_DOORS_TO_OPEN;
	}

	@Override
	public void abortEvent()
	{
		if (_state == State.INACTIVE)
			return;

		if (_state == State.PARTICIPATING)
		{
			unspawnManager();
		}
		else if (_state == State.STARTING)
		{
			teleportPlayersBack();
		}
		else if (_state == State.FIGHTING)
		{
			endFight();
			removeDoors();
			teleportPlayersBack();
		}

		_state = State.INACTIVE;
		clearData();
		autoStart();
	}

	private void loadData()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM z_tvt_teams");
			ResultSet rset = statement.executeQuery();
			Team team;
			int index = 0;

			while (rset.next())
			{
				index++;

				if (index > Config.TVT_TEAMS_NUM)
					break;

				team = new Team();

				team._teamId = index;
				team._teamName = rset.getString("teamName");
				team._teamX = rset.getInt("teamX");
				team._teamY = rset.getInt("teamY");
				team._teamZ = rset.getInt("teamZ");
				team._teamColor = rset.getString("teamColor");

				_teams.put(index, team);
			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("TvTEventEngine[TvT.loadInfo()]: Error while loading TvT Teams data: " + e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	private void clearData()
	{
		if (_sheduleNext != null)
		{
			_sheduleNext.cancel(false);
			_sheduleNext = null;
		}
		for (L2PcInstance player : _players.values(new L2PcInstance[_players.size()]))
		{
			player._eventName = "";
		}
		_players.clear();
		_teams.clear();
	}

	private void startFight()
	{

		for (L2PcInstance player : getAllPlayers())
		{
			Team team = _teams.get(player._eventTeamId);

			if (Config.TVT_ON_START_UNSUMMON_PET)
			{
				if (player.getPet() != null && player.getPet() instanceof L2PetInstance)
					player.getPet().unSummon(player);
			}

			if (Config.TVT_ON_START_REMOVE_ALL_EFFECTS)
			{
				player.stopAllEffects();
				if (player.getPet() != null)
					player.getPet().stopAllEffects();
			}

			if(player.getOlympiadGameId() > 0 || player.isInOlympiadMode() || Olympiad.getInstance().isRegistered(player))
			{
				return;
			}
			
			//Remove player from his party
			if (player.getParty() != null)
				player.getParty().removePartyMember(player);

			if (Config.TVT_AURA && _teams.size() == 2)
				player.setTeam(player._eventTeamId);

			if (player.isMounted())
				player.dismount();

//			player.getAppearance().setVisibleTitle(Localization.getInstance().getString(Config.MULTILANG_DEFAULT, "FunEvent.STRING_KILLS")+": " + player._eventCountKills);
			player.broadcastTitleInfo();
			player.broadcastUserInfo();
			player.teleToLocation(team._teamX, team._teamY, team._teamZ);
			player._eventTeleported = true;
		}
	}

	private void endFight()
	{
		int topteamId = 0;
		int topteamkills = 0;
		int topteams = 0;

		for (Team team : getAllTeams())
		{
			if (team._teamKills > topteamkills)
				topteamkills = team._teamKills;
		}
		for (Team team : getAllTeams())
		{
			if (team._teamKills == topteamkills)
			{
				topteamId = team._teamId;
				topteams++;
			}
		}

		int topplayerId = 0;
		int topplayerPoints = 0;
		int topplayers = 0;

		for (L2PcInstance player : getAllPlayers())
		{
			if (player._eventTeamId != topteamId)
				continue;
			if (player._eventCountKills > topplayerPoints)
				topplayerPoints = player._eventCountKills;
		}
		for (L2PcInstance player : getAllPlayers())
		{
			if (player._eventTeamId != topteamId)
				continue;
			if (player._eventCountKills == topplayerPoints)
			{
				topplayerId = player.getObjectId();
				topplayers++;
			}
		}

		if (topteamkills == 0)
		{
			AnnounceToPlayers(true, new Messages(29, true));

		}
		else if (topteams > 1)
		{
			AnnounceToPlayers(true, new Messages(30, true));

		}
		else
		{
			Messages msg = new Messages(31, true);
			msg.add(_teams.get(topteamId)._teamName);
			msg.add(topteamkills);
			AnnounceToPlayers(true, msg);
			for (L2PcInstance player : getAllPlayers())
			{
				if (player._eventTeamId == topteamId)
				{
					player.sendMessage(Localization.getInstance().getString(player.getLang(), 32));
					if (player.getObjectId() == topplayerId && topplayers == 1)
					{
						for (String reward : Config.TVT_REWARD_TOP)
						{
							String[] rew = reward.split(":");
							player.addItem("TvT Event", Integer.parseInt(rew[0]), Integer.parseInt(rew[1]), null, true);
						}
						Messages topmsg = new Messages(33, true);
						topmsg.add(player.getName());
						topmsg.add(player._eventCountKills);
						AnnounceToPlayers(true, topmsg);
					}
					else
					{
						if (Config.TVT_PRICE_NO_KILLS || player._eventCountKills > 0)
						{
							for (String reward : Config.TVT_REWARD)
							{
								String[] rew = reward.split(":");
								player.addItem("TvT Event", Integer.parseInt(rew[0]), Integer.parseInt(rew[1]), null, true);
							}
						}
					}
				}
			}
		}
	}

	private void teleportPlayersBack()
	{
		kickPlayersFromEvent();
	}

	@Override
	protected void StartNext()
	{
		long delay = 0;

		if (_state == State.WAITING)
		{
			delay = Config.TVT_COUNTDOWN_TIME * 60000;
			_state = State.PARTICIPATING;
			loadData();
			spawnManager();
			countdown();
			sendConfirmDialog();
		}
		else if (_state == State.PARTICIPATING)
		{
			delay = 20000;
			unspawnManager();
			if (checkPlayersCount())
				teleportPlayers();
			else
				{ abortEvent();	return; }
			_state = State.STARTING;
		}
		else if (_state == State.STARTING)
		{
			delay = Config.TVT_FIGHT_TIME * 60000;
			_state = State.FIGHTING;
			startFight();
			makeDoors();
		}
		else if (_state == State.FIGHTING)
		{
			endFight();
			removeDoors();
			teleportPlayersBack();
			clearData();

			_state = State.INACTIVE;
			autoStart();
			return;
		}

		sheduleNext(delay);
	}

	@Override
	public void onPlayerLogin(final L2PcInstance player)
	{
		if (_players.containsKey(player.getObjectId()))
		{
			L2PcInstance member = _players.get(player.getObjectId());
			player._eventName = member._eventName;
			player._eventTeamId = member._eventTeamId;
			if (_state == State.STARTING)
			{
				player._eventOriginalTitle = member._eventOriginalTitle;
				player._eventOriginalNameColor = member._eventOriginalNameColor;
				player._eventOriginalKarma = member._eventOriginalKarma;
				player.getAppearance().setNameColor(member.getAppearance().getNameColor());
				player.setKarma(0);
				player.broadcastUserInfo();
			}
			else if (_state == State.FIGHTING)
			{
				player._eventOriginalTitle = member._eventOriginalTitle;
				player._eventOriginalNameColor = member._eventOriginalNameColor;
				player._eventOriginalKarma = member._eventOriginalKarma;
				player._eventCountKills = member._eventCountKills;
				player.setKarma(0);
				if (Config.TVT_AURA && _teams.size() == 2)
					player.setTeam(player._eventTeamId);
//				player.getAppearance().setVisibleTitle(Localization.getInstance().getString(Config.MULTILANG_DEFAULT, "FunEvent.STRING_KILLS")+": " + player._eventCountKills);
				player.getAppearance().setNameColor(member.getAppearance().getNameColor());
				player.broadcastTitleInfo();
				player.broadcastUserInfo();
				Team team = _teams.get(player._eventTeamId);
				if (!member._eventTeleported)
				{
					if (Config.TVT_ON_START_UNSUMMON_PET)
					{
						if (player.getPet() != null && player.getPet() instanceof L2PetInstance)
							player.getPet().unSummon(player);
					}
					if (Config.TVT_ON_START_REMOVE_ALL_EFFECTS)
					{
						player.stopAllEffects();
						if (player.getPet() != null)
							player.getPet().stopAllEffects();
					}
					if (player.isMounted())
						player.dismount();
					player.teleToLocation(team._teamX, team._teamY, team._teamZ);
					player._eventTeleported = true;
				}
			}
			_players.put(player.getObjectId(), player);
		}
	}

	@Override
	public boolean onPlayerDie(final L2PcInstance player, L2PcInstance killer)
	{
		_teams.get(killer._eventTeamId)._teamKills++;
		killer._eventCountKills++;

//		killer.getAppearance().setVisibleTitle(Localization.getInstance().getString(Config.MULTILANG_DEFAULT, "FunEvent.STRING_KILLS")+": " + killer._eventCountKills);
		//killer.broadcastTitleInfo();
		//killer.broadcastUserInfo();
		//For now uncomment this, better to be died, and then teleported to the team
		player.teleToLocation(Config.TVT_DEAD_X, Config.TVT_DEAD_Y, Config.TVT_DEAD_Z, false);
		Messages msg = new Messages(34, player.getLang());
		msg.add(Config.TVT_RES_TIME);
		player.sendMessage(msg.toString());

		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				Team team = _teams.get(player._eventTeamId);
				player.doRevive();
				player.teleToLocation(team._teamX, team._teamY, team._teamZ, false);
				player.broadcastStatusUpdate();
				player.broadcastUserInfo();
			}
		}, Config.TVT_RES_TIME * 1000);

		return false;
	}
}