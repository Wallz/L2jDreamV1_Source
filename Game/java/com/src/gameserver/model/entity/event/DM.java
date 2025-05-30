package com.src.gameserver.model.entity.event;

import com.src.Config;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.services.Localization;
import com.src.gameserver.services.Messages;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.random.Rnd;

public class DM extends FunEvent
{
	@Override
	public void loadConfig() // Configs
	{
		EVENT_ID = 4;
		EVENT_NAME = "DM";
		EVENT_FULL_NAME = 14;
		EVENT_AUTO_MODE = Config.DM_AUTO_MODE;
		EVENT_INTERVAL = Config.DM_EVENT_INTERVAL;
		EVENT_NPC_LOC = (new int[] {Config.DM_NPC_X, Config.DM_NPC_Y, Config.DM_NPC_Z});
		EVENT_NPC_LOC_NAME = Config.DM_NPC_LOC_NAME;
		EVENT_TEAMS_TYPE = Config.DM_EVEN_TEAMS;
		EVENT_PLAYER_LEVEL_MIN = Config.DM_PLAYER_LEVEL_MIN;
		EVENT_PLAYER_LEVEL_MAX = Config.DM_PLAYER_LEVEL_MAX;
		EVENT_COUNTDOWN_TIME = Config.DM_COUNTDOWN_TIME;
		EVENT_MIN_PLAYERS = Config.DM_MIN_PLAYERS;
		EVENT_DOORS_TO_CLOSE = Config.DM_DOORS_TO_CLOSE;
		EVENT_DOORS_TO_OPEN = Config.DM_DOORS_TO_OPEN;
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

			if (Config.DM_ON_START_UNSUMMON_PET)
			{
				if (player.getPet() != null && player.getPet() instanceof L2PetInstance)
					player.getPet().unSummon(player);
			}

			if (Config.DM_ON_START_REMOVE_ALL_EFFECTS)
			{
				player.stopAllEffects();
				if (player.getPet() != null)
					player.getPet().stopAllEffects();
			}

			//Remove player from his party
			if (player.getParty() != null)
				player.getParty().removePartyMember(player);

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
		L2PcInstance winner = null;
		int count = 0;
		for (L2PcInstance player : getAllPlayers())
		{
			if (player.isOnline() == 0)
				continue;
			count++;
			if (!player.isDead())
				winner = player;
		}

		if(count == 1)
		{
			winner.sendMessage(Localization.getInstance().getString(winner.getLang(), 58));
			for (String reward : Config.DM_REWARD)
			{
				String[] rew = reward.split(":");
				winner.addItem("DM Event", Integer.parseInt(rew[0]), Integer.parseInt(rew[1]), null, true);
			}
			Messages msg = new Messages(57, true);
			msg.add(winner.getName());
			msg.add(_teams.get(winner._eventTeamId)._teamKills);
			AnnounceToPlayers(true, msg);
		}
		else
		{
			AnnounceToPlayers(true, new Messages(59, true));
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
			delay = Config.DM_COUNTDOWN_TIME * 60000;
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
			delay = Config.DM_FIGHT_TIME * 60000;
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
			if (_state == State.STARTING)
			{
				L2PcInstance member = _players.get(player.getObjectId());
				player._eventName = member._eventName;
				player._eventTeamId = member._eventTeamId;
				player._eventOriginalTitle = member._eventOriginalTitle;
				player._eventOriginalNameColor = member._eventOriginalNameColor;
				player._eventOriginalKarma = member._eventOriginalKarma;
				//player.getAppearance().setNameColor(member.getAppearance().getNameColor());
				player.setKarma(0);
				player.broadcastUserInfo();
				_players.put(player.getObjectId(), player);
			}
			else if (_state == State.FIGHTING)
			{
				kickPlayerFromEvent(player);
			}
		}
	}

	@Override
	public boolean onPlayerDie(final L2PcInstance player, L2PcInstance killer)
	{
		_teams.get(killer._eventTeamId)._teamKills++;

		killer._eventCountKills++;
//		killer.getAppearance().setVisibleTitle(Localization.getInstance().getString(Config.MULTILANG_DEFAULT, "FunEvent.STRING_KILLS")+": " + killer._eventCountKills);
		//killer.broadcastTitleInfo();
		killer.broadcastUserInfo();
		player.broadcastUserInfo();

		player.sendMessage(Localization.getInstance().getString(player.getLang(), 60));
		kickPlayerFromEvent(player);
        ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
        {
            @Override
			public void run()
            {

        		int count = 0;
        		for (L2PcInstance plr : getAllPlayers())
        		{
        			if (plr.isOnline() == 0)
        				continue;
        			count++;
        		}
        		if (count < 2)
        			abortEvent();
            }
        }, 2000);
    

		return false;
	}

	@Override
	protected void teleportPlayers()
	{
		if (EVENT_TEAMS_TYPE.equals("SHUFFLE"))
		{
			int index = 0;
			for (L2PcInstance player : getAllPlayers())
			{
				if (player.isOnline() == 0)
				{
					_players.remove(player.getObjectId());
					continue;
				}
				index++;
				player._eventName = EVENT_NAME;
				player._eventTeamId = index;
				int offset = 500;
				Team team = new Team();
				team._teamId = index;
				team._teamX = Config.DM_START_LOC_X + Rnd.get(-offset, offset);
				team._teamY = Config.DM_START_LOC_Y + Rnd.get(-offset, offset);
				team._teamZ = Config.DM_START_LOC_Z;
				_teams.put(index, team);
			}
		}

		for (L2PcInstance player : getAllPlayers())
			updatePlayerInfo(player);

		Messages msg = new Messages(61, true);
		msg.add(EVENT_NAME);
		AnnounceToPlayers(false, msg);
	}

	@Override
	public NpcHtmlMessage getChatWindow(L2PcInstance player)
	{
		if (_state != State.PARTICIPATING)
			return null;
		
		String countDownTimer = "";
		int timeLeft = getStartNextTime();
		String lang = player.getLang();

		if (timeLeft > 60)
			 countDownTimer = timeLeft/60 + " "+Localization.getInstance().getString(lang, 8);
		else
			 countDownTimer = timeLeft + " "+Localization.getInstance().getString(lang, 9);

		NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(5);

		if (!_players.containsKey(player.getObjectId()))
		{
			String joiningButtons = "<center><button value=\""+Localization.getInstance().getString(lang, 10)+"\" action=\"bypass -h npc_%objectId%_join 0\" width=204 height=20 back=\"sek.cbui36\" fore=\"sek.cbui75\"></center>";

			npcHtmlMessage.setFile(lang, "data/html/addon/EventManager/DM-joining.htm");
			npcHtmlMessage.replace("%eventName%", EVENT_NAME);
			npcHtmlMessage.replace("%playerLevels%", EVENT_PLAYER_LEVEL_MIN + "-" + EVENT_PLAYER_LEVEL_MAX);
			npcHtmlMessage.replace("%playersCount%", _players.size());
			npcHtmlMessage.replace("%playersCountMin%", EVENT_MIN_PLAYERS);
			npcHtmlMessage.replace("%countdownTime%", countDownTimer);
			npcHtmlMessage.replace("%joiningButtons%", joiningButtons);
		}
		else
		{
			npcHtmlMessage.setFile(lang, "data/html/addon/EventManager/DM-joined.htm");
			npcHtmlMessage.replace("%eventName%", EVENT_NAME);
			npcHtmlMessage.replace("%playersCount%", _players.size());
			npcHtmlMessage.replace("%playersCountMin%", EVENT_MIN_PLAYERS);
			npcHtmlMessage.replace("%countdownTime%", countDownTimer);
		}
		return npcHtmlMessage;
	}
}
