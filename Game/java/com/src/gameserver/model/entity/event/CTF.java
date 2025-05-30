package com.src.gameserver.model.entity.event;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import com.src.Config;
import com.src.gameserver.datatables.sql.ItemTable;
import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.idfactory.IdFactory;
import com.src.gameserver.model.actor.instance.L2CustomCTFFlagInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.model.itemcontainer.Inventory;
import com.src.gameserver.network.clientpackets.Say2;
import com.src.gameserver.network.serverpackets.CreatureSay;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.network.serverpackets.SocialAction;
import com.src.gameserver.services.Localization;
import com.src.gameserver.services.Messages;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class CTF extends FunEvent
{

	private int											_CTFFlagId			= 97005;
	private TIntObjectHashMap<L2CustomCTFFlagInstance>	_flagSpawns			= new TIntObjectHashMap<L2CustomCTFFlagInstance>();
	private int											_CTFThroneId		= 97006;
	private TIntObjectHashMap<L2CustomCTFFlagInstance>	_throneSpawns		= new TIntObjectHashMap<L2CustomCTFFlagInstance>();
	private int											_CTFFlagInHandId	= 6718;
	private final static Logger _log = Logger.getLogger(CTF.class.getName());
	@Override
	public void loadConfig() // Configs
	{
		EVENT_ID = 2;
		EVENT_NAME = "CTF";
		EVENT_FULL_NAME = 16;
		EVENT_AUTO_MODE = Config.CTF_AUTO_MODE;
		EVENT_INTERVAL = Config.CTF_EVENT_INTERVAL;
		EVENT_NPC_LOC = (new int[] {Config.CTF_NPC_X, Config.CTF_NPC_Y, Config.CTF_NPC_Z});
		EVENT_NPC_LOC_NAME = Config.CTF_NPC_LOC_NAME;
		EVENT_TEAMS_TYPE = Config.CTF_EVEN_TEAMS;
		EVENT_PLAYER_LEVEL_MIN = Config.CTF_PLAYER_LEVEL_MIN;
		EVENT_PLAYER_LEVEL_MAX = Config.CTF_PLAYER_LEVEL_MAX;
		EVENT_COUNTDOWN_TIME = Config.CTF_COUNTDOWN_TIME;
		EVENT_MIN_PLAYERS = Config.CTF_MIN_PLAYERS;
		EVENT_DOORS_TO_CLOSE = Config.CTF_DOORS_TO_CLOSE;
		EVENT_DOORS_TO_OPEN = Config.CTF_DOORS_TO_OPEN;
	}

	@Override
	protected CTFTeam getTeam(int team)
	{
		return (CTFTeam) _teams.get(team);
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
			unspawnFlags();
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
			PreparedStatement statement = con.prepareStatement("SELECT * FROM z_ctf_teams");
			ResultSet rset = statement.executeQuery();
			CTFTeam team = null;
			int index = 0;

			while (rset.next())
			{
				index++;

				if (index > Config.CTF_TEAMS_NUM)
					break;

				team = new CTFTeam();

				team._teamId = index;
				team._teamName = rset.getString("teamName");
				team._teamX = rset.getInt("teamX");
				team._teamY = rset.getInt("teamY");
				team._teamZ = rset.getInt("teamZ");
				team._flagX = rset.getInt("flagX");
				team._flagY = rset.getInt("flagY");
				team._flagZ = rset.getInt("flagZ");
				team._teamColor = rset.getString("teamColor");

				_teams.put(index, team);
			}

			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("CTFEventEngine[CTF.loadInfo()]: Error while loading CTF Teams data: "+e);
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
			_sheduleNext.cancel(true);
			_sheduleNext = null;
		}
		_Manager = null;
		_flagSpawns.clear();
		_throneSpawns.clear();
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
			CTFTeam team = getTeam(player._eventTeamId);

			if (Config.CTF_ON_START_UNSUMMON_PET)
			{
				if (player.getPet() != null && player.getPet() instanceof L2PetInstance)
					player.getPet().unSummon(player);
			}

			if (Config.CTF_ON_START_REMOVE_ALL_EFFECTS)
			{
				player.stopAllEffects();
				if (player.getPet() != null)
					player.getPet().stopAllEffects();
			}

			//Remove player from his party
			if (player.getParty() != null)
				player.getParty().removePartyMember(player);

			if (Config.CTF_AURA && _teams.size() == 2)
				player.setTeam(player._eventTeamId);


			if (player.isMounted())
				player.dismount();

//			player.getAppearance().setVisibleTitle(Localization.getInstance().getString(Config.MULTILANG_DEFAULT, "CTF.STRING_FLAGS")+": " + player._CTFCountFlags);
			player.broadcastTitleInfo();
			player.broadcastUserInfo();
			player.teleToLocation(team._teamX, team._teamY, team._teamZ);
			player._eventTeleported = true;
		}

	}

	private void spawnFlags()
	{
		try
		{
			for (Team teams : getAllTeams())
			{
				CTFTeam team = (CTFTeam) teams;
				L2CustomCTFFlagInstance CTFFlag = new L2CustomCTFFlagInstance(IdFactory.getInstance().getNextId(), NpcTable.getInstance().getTemplate(_CTFFlagId));
				CTFFlag.setHeading(100);
				CTFFlag._teamId = team._teamId;
				CTFFlag._event = this;
				CTFFlag._mode = "FLAG";
				CTFFlag.setTitle(team._teamName + "'s Flag");
				CTFFlag.getStatus().setCurrentHpMp(CTFFlag.getMaxHp(), CTFFlag.getMaxMp());
				CTFFlag.spawnMe(team._flagX, team._flagY, team._flagZ + 50);
				CTFFlag.doRevive();
				CTFFlag.setIsDead(false);
				CTFFlag.setIsKilledAlready(false);
				_flagSpawns.put(team._teamId, CTFFlag);

				L2CustomCTFFlagInstance CTFThrone = new L2CustomCTFFlagInstance(IdFactory.getInstance().getNextId(), NpcTable.getInstance().getTemplate(_CTFThroneId));
				CTFThrone.setHeading(100);
				CTFThrone._event = this;
				CTFThrone._mode = "THRONE";
				CTFThrone.setName(" ");
				CTFThrone.getStatus().setCurrentHpMp(CTFThrone.getMaxHp(), CTFThrone.getMaxMp());
				CTFThrone.spawnMe(team._flagX, team._flagY, team._flagZ + 50);
				CTFThrone.doRevive();
				CTFFlag.setIsDead(false);
				CTFFlag.setIsKilledAlready(false);
				_throneSpawns.put(team._teamId, CTFThrone);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void unspawnFlags()
	{
		try
		{
			if (_flagSpawns != null && !_flagSpawns.isEmpty())
				for (L2CustomCTFFlagInstance CTFFlag : _flagSpawns.values(new L2CustomCTFFlagInstance[_flagSpawns.size()]))
					CTFFlag.deleteMe();

			if (_throneSpawns != null && !_throneSpawns.isEmpty())
				for (L2CustomCTFFlagInstance CTFThrone : _throneSpawns.values(new L2CustomCTFFlagInstance[_throneSpawns.size()]))
					CTFThrone.deleteMe();
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
	}

	private void checkFlagsLoop()
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				if (_state == State.FIGHTING)
				{
					checkFlags();
					checkFlagsLoop();
				}
			}
		}, 60000);
	}

	private void checkFlags()
	{
		for (L2PcInstance player : getAllPlayers())
		{
			if (player == null)
				continue;

			if (player.isOnline() == 0 && player._CTFHaveFlagOfTeam != 0)
			{
				Messages msg = new Messages(41, true);
				msg.add(player.getName());
				AnnounceToPlayers(false, msg);
				spawnFlag(player._CTFHaveFlagOfTeam);
				takeFlag(player);
			}
		}
	}

	private void spawnFlag(int teamId)
	{
		if (_flagSpawns.containsKey(teamId))
			return;

		CTFTeam team = getTeam(teamId);
		L2CustomCTFFlagInstance CTFFlag = new L2CustomCTFFlagInstance(IdFactory.getInstance().getNextId(), NpcTable.getInstance().getTemplate(_CTFFlagId));
		CTFFlag.setHeading(100);
		CTFFlag._teamId = team._teamId;
		CTFFlag._event = this;
		CTFFlag._mode = "FLAG";
		CTFFlag.setTitle(team._teamName + "'s Flag");
		CTFFlag.spawnMe(team._flagX, team._flagY, team._flagZ + 50);
		CTFFlag.doRevive();
		CTFFlag.setIsDead(false);
		CTFFlag.setIsKilledAlready(false);
		_flagSpawns.put(team._teamId, CTFFlag);
	}

	private void unspawnFlag(int teamId)
	{
		_flagSpawns.get(teamId).deleteMe();
		_flagSpawns.remove(teamId);
	}

	private void endFight()
	{
		int topteamId = 0;
		int topteamflags = 0;
		int topteams = 0;

		for (Team teams : getAllTeams())
		{
			CTFTeam team = (CTFTeam) teams;
			if (team._teamFlags > topteamflags)
				topteamflags = team._teamFlags;
		}
		for (Team teams : getAllTeams())
		{
			CTFTeam team = (CTFTeam) teams;
			if (team._teamFlags == topteamflags)
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
			if (player._CTFCountFlags > topplayerPoints)
				topplayerPoints = player._CTFCountFlags;
		}
		for (L2PcInstance player : getAllPlayers())
		{
			if (player._eventTeamId != topteamId)
				continue;
			if (player._CTFCountFlags == topplayerPoints)
			{
				topplayerId = player.getObjectId();
				topplayers++;
			}
		}

		if (topteamflags == 0)
		{
			AnnounceToPlayers(true, new Messages(42, true));
		}
		else if (topteams > 1)
		{
			AnnounceToPlayers(true, new Messages(43, true));

		}
		else
		{
			Messages msg = new Messages(44, true);
			msg.add(_teams.get(topteamId)._teamName);
			msg.add(topteamflags);
			AnnounceToPlayers(true, msg);
			for (L2PcInstance player : getAllPlayers())
			{
				if (player._eventTeamId == topteamId)
				{
					player.sendMessage(Localization.getInstance().getString(player.getLang(), 45));
					if (player.getObjectId() == topplayerId && topplayers == 1)
					{
						for (String reward : Config.CTF_REWARD_TOP)
						{
							String[] rew = reward.split(":");
							player.addItem("CTF Event", Integer.parseInt(rew[0]), Integer.parseInt(rew[1]), null, true);
						}
						Messages topmsg = new Messages(46, true);
						topmsg.add(player.getName());
						topmsg.add(player._CTFCountFlags);
						AnnounceToPlayers(true, topmsg);
					}
					else
					{
						if (Config.CTF_PRICE_NO_KILLS || player._eventCountKills > 0)
						{
							for (String reward : Config.CTF_REWARD)
							{
								String[] rew = reward.split(":");
								player.addItem("CTF Event", Integer.parseInt(rew[0]), Integer.parseInt(rew[1]), null, true);
							}
						}
					}
				}
			}
		}
	}

	private void teleportPlayersBack()
	{
		for (L2PcInstance player : getAllPlayers())
		{
			if (player.isOnline() != 0)
			{
				if (player._CTFHaveFlagOfTeam != 0)
					takeFlag(player);
				//player.getAppearance().setNameColor(player._eventOriginalNameColor);
//				player.getAppearance().setVisibleTitle(player._eventOriginalTitle);
				player.setKarma(player._eventOriginalKarma);
				player.setTeam(0);
				player._eventName = "";
				player._eventTeamId = 0;
				player._CTFHaveFlagOfTeam = 0;
				player._CTFCountFlags = 0;
				player.doRevive();
				player.getStatus().setCurrentHp(player.getMaxHp());
				player.getStatus().setCurrentMp(player.getMaxMp());
				player.getStatus().setCurrentCp(player.getMaxCp());
				player.teleToLocation(Config.CTF_NPC_X, Config.CTF_NPC_Y, Config.CTF_NPC_Z, false);
				player.broadcastTitleInfo();
				player.broadcastUserInfo();
			}
			else
			{
				Connection con = null;
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();

					PreparedStatement statement = con.prepareStatement("UPDATE characters SET x=?, y=?, z=?, karma=? WHERE char_name=?");
					statement.setInt(1, Config.CTF_NPC_X);
					statement.setInt(2, Config.CTF_NPC_Y);
					statement.setInt(3, Config.CTF_NPC_Z);
					statement.setInt(4, player._eventOriginalKarma);
					statement.setString(5, player.getName());
					statement.execute();
					statement.close();
				}
				catch (Exception e)
				{
					_log.warning("CTFEventEngine[CTF.endFight()]: Error while updating player's " + player.getName() + " data: "+e);
				}
				finally
				{
					ResourceUtil.closeConnection(con);
				}
			}
		}
	}

	@Override
	protected void StartNext()
	{
		long delay = 0;

		if (_state == State.WAITING)
		{
			delay = Config.CTF_COUNTDOWN_TIME * 60000;
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
			delay = Config.CTF_FIGHT_TIME * 60000;
			_state = State.FIGHTING;
			startFight();
			makeDoors();
			spawnFlags();
			checkFlagsLoop();
		}
		else if (_state == State.FIGHTING)
		{
			unspawnFlags();
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
				//player.getAppearance().setNameColor(member.getAppearance().getNameColor());
				player.setKarma(0);
				player.broadcastUserInfo();
			}
			else if (_state == State.FIGHTING)
			{
				player._eventOriginalTitle = member._eventOriginalTitle;
				player._eventOriginalNameColor = member._eventOriginalNameColor;
				player._eventOriginalKarma = member._eventOriginalKarma;
				player._CTFCountFlags = member._CTFCountFlags;
				player.setKarma(0);
				if (Config.CTF_AURA && _teams.size() == 2)
					player.setTeam(player._eventTeamId);

//				player.getAppearance().setVisibleTitle(Localization.getInstance().getString(Config.MULTILANG_DEFAULT, "CTF.STRING_FLAGS")+": " + player._CTFCountFlags);
				//layer.getAppearance().setNameColor(member.getAppearance().getNameColor());
				player.broadcastTitleInfo();
				player.broadcastUserInfo();
				CTFTeam team = getTeam(player._eventTeamId);
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
	public void onPlayerLogout(final L2PcInstance player)
	{
		if (player._CTFHaveFlagOfTeam != 0)
		{
			spawnFlag(player._CTFHaveFlagOfTeam);
			Messages msg = new Messages(47, true);
			msg.add(getTeam(player._CTFHaveFlagOfTeam)._teamName);
			AnnounceToPlayers(false, msg);

			for (L2PcInstance plr : getAllPlayers())
			{
				if (plr._eventTeamId == player._CTFHaveFlagOfTeam)
				{
					CreatureSay cs = new CreatureSay(plr.getObjectId(), Say2.PARTYROOM_COMMANDER, ":", Localization.getInstance().getString(plr.getLang(), 48)); // 8D
					plr.sendPacket(cs);
				}
			}

			takeFlag(player);
		}
	}

	public void onPlayerTakeFlag(L2PcInstance player, int teamId)
	{
		unspawnFlag(teamId);
		giveFlag(player, teamId);
		if (player.getAppearance().getInvisible())
		{

				player.sendMessage(Localization.getInstance().getString(player.getLang(), 49));
				player.getAppearance().setVisible();
		}

		Messages msg = new Messages(50, true);
		msg.add(getTeam(teamId)._teamName);
		AnnounceToPlayers(false, msg);

		for (L2PcInstance plr : getAllPlayers())
		{
			if (plr._eventTeamId == teamId)
			{
				CreatureSay cs = new CreatureSay(plr.getObjectId(), Say2.PARTYROOM_COMMANDER, ":", Localization.getInstance().getString(plr.getLang(), 51)); // 8D
				plr.sendPacket(cs);
			}
		}
	}

	public void onPlayerBringFlag(L2PcInstance player)
	{
		Messages msg = new Messages(52, true);
		msg.add(getTeam(player._eventTeamId)._teamName);
		msg.add(getTeam(player._CTFHaveFlagOfTeam)._teamName);
		AnnounceToPlayers(false, msg);

		getTeam(player._eventTeamId)._teamFlags++;
		player._CTFCountFlags++;


		//player.getAppearance().setVisibleTitle(Localization.getInstance().getString(Config.MULTILANG_DEFAULT, "CTF.STRING_FLAGS")+": " + player._CTFCountFlags);
		player.broadcastTitleInfo();
		player.broadcastUserInfo();

		spawnFlag(player._CTFHaveFlagOfTeam);

		for (L2PcInstance plr : getAllPlayers())
		{
			if (plr._eventTeamId == player._CTFHaveFlagOfTeam)
			{
				CreatureSay cs = new CreatureSay(plr.getObjectId(), Say2.PARTYROOM_COMMANDER, ":", Localization.getInstance().getString(plr.getLang(), 53)); // 8D
				plr.sendPacket(cs);
			}
		}

		takeFlag(player);
	}

	@Override
	public boolean onPlayerDie(final L2PcInstance player, L2PcInstance killer)
	{
		if (player._CTFHaveFlagOfTeam != 0)
		{
			spawnFlag(player._CTFHaveFlagOfTeam);
			Messages msg = new Messages(54, true);
			msg.add(getTeam(player._CTFHaveFlagOfTeam)._teamName);
			AnnounceToPlayers(false, msg);

			for (L2PcInstance plr : getAllPlayers())
			{
				if (plr._eventTeamId == player._CTFHaveFlagOfTeam)
				{
					CreatureSay cs = new CreatureSay(plr.getObjectId(), Say2.PARTYROOM_COMMANDER, ":", Localization.getInstance().getString(plr.getLang(), 53)); // 8D
					plr.sendPacket(cs);
				}
			}

			takeFlag(player);
		}
		//For now uncomment this, better to be died, and then teleported to the team
		//player.teleToLocation(Config.CTF_DEAD_X, Config.CTF_DEAD_Y, Config.CTF_DEAD_Z, false);
		//player.doRevive();
		//player.broadcastStatusUpdate();
		//player.broadcastUserInfo();
		Messages msg = new Messages(55, player.getLang());
		msg.add(Config.CTF_RES_TIME);
		player.sendMessage(msg.toString());

		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				CTFTeam team = getTeam(player._eventTeamId);
				player.doRevive();
				player.teleToLocation(team._teamX, team._teamY, team._teamZ, false);
				player.broadcastStatusUpdate();
				player.broadcastUserInfo();
			}
		}, Config.CTF_RES_TIME * 1000);

		return false;
	}

	public void giveFlag(L2PcInstance player, int teamId)
	{
		//remove items from the player hands (right, left, both)
		// This is NOT a BUG, I don't want them to see the icon they have 8D
		L2ItemInstance wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (wpn == null)
		{
			wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			if (wpn != null)
				player.getInventory().unEquipItemInBodySlotAndRecord(Inventory.PAPERDOLL_RHAND);
		}
		else
		{
			player.getInventory().unEquipItemInBodySlotAndRecord(Inventory.PAPERDOLL_RHAND);
			wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
			if (wpn != null)
				player.getInventory().unEquipItemInBodySlotAndRecord(Inventory.PAPERDOLL_LHAND);
		}
		//add the flag in his hands
		player.getInventory().equipItem(ItemTable.getInstance().createItem("", _CTFFlagInHandId, 1, player, null));
		player.broadcastPacket(new SocialAction(player.getObjectId(), 16)); //amazing glow
		player._CTFHaveFlagOfTeam = teamId;
		player.broadcastUserInfo();
		CreatureSay cs = new CreatureSay(player.getObjectId(), Say2.PARTYROOM_COMMANDER, ":", Localization.getInstance().getString(player.getLang(), 56)); // 8D
		player.sendPacket(cs);
	}

	public void takeFlag(L2PcInstance player)
	{
		L2ItemInstance wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		player._CTFHaveFlagOfTeam = 0;
		if (wpn != null)
		{
			L2ItemInstance[] unequiped = player.getInventory().unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
			player.getInventory().destroyItemByItemId("", _CTFFlagInHandId, 1, player, null);
			InventoryUpdate iu = new InventoryUpdate();
			for (L2ItemInstance element : unequiped)
				iu.addModifiedItem(element);
			player.sendPacket(iu);
			player.sendPacket(new ItemList(player, true)); // get your weapon back now ...
			player.abortAttack();
			player.broadcastUserInfo();
		}
		else
		{
			player.getInventory().destroyItemByItemId("", _CTFFlagInHandId, 1, player, null);
			player.sendPacket(new ItemList(player, true)); // get your weapon back now ...
			player.abortAttack();
			player.broadcastUserInfo();
		}
	}

	public class CTFTeam extends Team
	{
		public int 			_flagX;
		public int			_flagY;
		public int			_flagZ;
		public int			_teamFlags;
	}
}
