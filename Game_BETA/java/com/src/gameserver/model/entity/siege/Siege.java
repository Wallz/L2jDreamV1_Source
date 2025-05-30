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
package com.src.gameserver.model.entity.siege;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import javolution.util.FastList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.datatables.xml.MapRegionTable;
import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.idfactory.IdFactory;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.managers.MercTicketManager;
import com.src.gameserver.managers.SiegeGuardManager;
import com.src.gameserver.managers.SiegeManager;
import com.src.gameserver.managers.SiegeManager.SiegeSpawn;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.L2ClanMember;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2SiegeClan;
import com.src.gameserver.model.L2SiegeClan.SiegeClanType;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2ArtefactInstance;
import com.src.gameserver.model.actor.instance.L2ControlTowerInstance;
import com.src.gameserver.model.actor.instance.L2FlameTowerInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.Announcements;
import com.src.gameserver.model.entity.Hero;
import com.src.gameserver.model.entity.sevensigns.SevenSigns;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.PlaySound;
import com.src.gameserver.network.serverpackets.RelationChanged;
import com.src.gameserver.network.serverpackets.SiegeInfo;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.network.serverpackets.UserInfo;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.gameserver.util.Broadcast;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;
import com.src.util.protection.nProtect;
import com.src.util.protection.nProtect.RestrictionType;

public class Siege
{
	private final static Log _log = LogFactory.getLog(Siege.class);

	public static enum TeleportWhoType
	{
		All,
		Attacker,
		DefenderNotOwner,
		Owner,
		Spectator
	}

	private int _controlTowerCount;
	private int _controlTowerMaxCount;
	private int _flameTowerCount;
	private int _flameTowerMaxCount;

	public int getControlTowerCount()
	{
		return _controlTowerCount;
	}
	
	/**
	 * @return the max count of control type towers.
	 */
	public int getControlTowerMaxCount()
	{
		return _controlTowerMaxCount;
	}
	
	/**
	 * @return the max count of flame type towers.
	 */
	public int getFlameTowerMaxCount()
	{
		return _flameTowerMaxCount;
	}
	
	public void disableTraps()
	{
		_flameTowerCount--;
	}
	
	public boolean isTrapsActive()
	{
		return _flameTowerCount > 0;
	}
	
	public class ScheduleEndSiegeTask implements Runnable
	{
		private Castle _castleInst;

		public ScheduleEndSiegeTask(Castle pCastle)
		{
			_castleInst = pCastle;
		}

		@Override
		public void run()
		{
			if(!getIsInProgress())
				return;

			try
			{
				long timeRemaining = _siegeEndDate.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();

				if(timeRemaining > 3600000)
				{
					announceToPlayer(SystemMessage.getSystemMessage(SystemMessageId.S1_HOURS_UNTIL_SIEGE_CONCLUSION).addNumber(2), true);
					ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndSiegeTask(_castleInst), timeRemaining - 3600000);
				}
				else if(timeRemaining <= 3600000 && timeRemaining > 600000)
				{
					announceToPlayer(SystemMessage.getSystemMessage(SystemMessageId.S1_MINUTES_UNTIL_SIEGE_CONCLUSION).addNumber(Math.round(timeRemaining / 60000)), true);

					ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndSiegeTask(_castleInst), timeRemaining - 600000);
				}
				else if(timeRemaining <= 600000 && timeRemaining > 300000)
				{
					announceToPlayer(SystemMessage.getSystemMessage(SystemMessageId.S1_MINUTES_UNTIL_SIEGE_CONCLUSION).addNumber(Math.round(timeRemaining / 60000)), true);

					ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndSiegeTask(_castleInst), timeRemaining - 300000);
				}
				else if(timeRemaining <= 300000 && timeRemaining > 10000)
				{
					announceToPlayer(SystemMessage.getSystemMessage(SystemMessageId.S1_MINUTES_UNTIL_SIEGE_CONCLUSION).addNumber(Math.round(timeRemaining / 60000)), true);

					ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndSiegeTask(_castleInst), timeRemaining - 10000);
				}
				else if(timeRemaining <= 10000 && timeRemaining > 0)
				{
					announceToPlayer(SystemMessage.getSystemMessage(SystemMessageId.CASTLE_SIEGE_S1_SECONDS_LEFT).addNumber(Math.round(timeRemaining / 1000)), true);

					ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndSiegeTask(_castleInst), timeRemaining);
				}
				else
				{
					_castleInst.getSiege().endSiege();
				}
			}
			catch(Throwable t)
			{
			}
		}
	}

	public class ScheduleStartSiegeTask implements Runnable
	{
		private Castle _castleInst;

		public ScheduleStartSiegeTask(Castle pCastle)
		{
			_castleInst = pCastle;
		}

		@Override
		public void run()
		{
			_scheduledStartSiegeTask.cancel(false);
			if(getIsInProgress())
			{
				return;
			}

			try
			{
				if (!getIsTimeRegistrationOver()) 
				{ 
					long regTimeRemaining = getTimeRegistrationOverDate().getTimeInMillis() - Calendar.getInstance().getTimeInMillis(); 
					if (regTimeRemaining > 0) 
					{ 
						_scheduledStartSiegeTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_castleInst), regTimeRemaining); 
						return; 
					} 
					else 
					{ 
						endTimeRegistration(true); 
					} 
				} 
				
				long timeRemaining = getSiegeDate().getTimeInMillis() - Calendar.getInstance().getTimeInMillis();

				if(timeRemaining > 86400000)
				{
					_scheduledStartSiegeTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_castleInst), timeRemaining - 86400000); // Prepare task for 24 before siege start to end registration 
				}
				else if(timeRemaining <= 86400000 && timeRemaining > 13600000)
				{
					Announcements.getInstance().announceToAll(SystemMessage.getSystemMessage(SystemMessageId.REGISTRATION_TERM_FOR_S1_ENDED).addString(getCastle().getName()));
					_isRegistrationOver = true;
					clearSiegeWaitingClan();

					_scheduledStartSiegeTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_castleInst), timeRemaining - 13600000); // Prepare task for 1 hr left before siege start.
				}
				else if(timeRemaining <= 13600000 && timeRemaining > 600000)
				{

					_scheduledStartSiegeTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_castleInst), timeRemaining - 600000); // Prepare task for 10 minute left.
				}
				else if(timeRemaining <= 600000 && timeRemaining > 300000)
				{

					_scheduledStartSiegeTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_castleInst), timeRemaining - 300000); // Prepare task for 5 minute left.
				}
				else if(timeRemaining <= 300000 && timeRemaining > 10000)
				{

					_scheduledStartSiegeTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_castleInst), timeRemaining - 10000); // Prepare task for 10 seconds count down
				}
				else if(timeRemaining <= 10000 && timeRemaining > 0)
				{

					_scheduledStartSiegeTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartSiegeTask(_castleInst), timeRemaining); // Prepare task for second count down
				}
				else
				{
					_castleInst.getSiege().startSiege();
				}
			}
			catch(Throwable t)
			{
			}
		}
	}

	private List<L2SiegeClan> _attackerClans = new FastList<L2SiegeClan>();

	private List<L2SiegeClan> _defenderClans = new FastList<L2SiegeClan>();
	private List<L2SiegeClan> _defenderWaitingClans = new FastList<L2SiegeClan>();
	private int _defenderRespawnDelayPenalty;

	private List<L2ArtefactInstance> _artifacts = new FastList<L2ArtefactInstance>();
	private List<L2FlameTowerInstance> _flameTowers = new FastList<L2FlameTowerInstance>();
	private List<L2ControlTowerInstance> _controlTowers = new FastList<L2ControlTowerInstance>();
	private Castle[] _castle;
	private boolean _isInProgress = false;
	private boolean _isNormalSide = true;
	protected boolean _isRegistrationOver = false;
	protected Calendar _siegeEndDate;
	private SiegeGuardManager _siegeGuardManager;
	protected ScheduledFuture<?> _scheduledStartSiegeTask = null;

	public Siege(Castle[] castle)
	{
		_castle = castle;
		_siegeGuardManager = new SiegeGuardManager(getCastle());

		startAutoTask();
	}

	public void endSiege()
	{
		if(getIsInProgress())
		{
			Announcements.getInstance().announceToAll(SystemMessage.getSystemMessage(SystemMessageId.SIEGE_OF_S1_HAS_ENDED).addString(getCastle().getName()));
			Broadcast.toAllOnlinePlayers(new PlaySound("systemmsg_e.18"));
			
			if (getCastle().getOwnerId() > 0)
			{
				L2Clan clan = ClanTable.getInstance().getClan(getCastle().getOwnerId());
				Announcements.getInstance().announceToAll(SystemMessage.getSystemMessage(SystemMessageId.CLAN_S1_VICTORIOUS_OVER_S2_S_SIEGE).addString(clan.getName()).addString(getCastle().getName()));
				
				// Delete circlets and crown's leader for initial castle's owner (if one was existing)
				if (getCastle().getInitialCastleOwner() != null && clan != getCastle().getInitialCastleOwner())
				{
					if (Config.REMOVE_CASTLE_CIRCLETS)
						CastleManager.getInstance().removeCirclet(getCastle().getInitialCastleOwner(), getCastle().getCastleId());
					
					for (L2ClanMember member : clan.getMembers())
					{
						if (member != null)
						{
							L2PcInstance player = member.getPlayerInstance();
							if (player != null && player.isNoble())
								Hero.getInstance().setCastleTaken(player.getObjectId(), getCastle().getCastleId());
						}
					}
				}
			}
			else
				Announcements.getInstance().announceToAll(SystemMessage.getSystemMessage(SystemMessageId.SIEGE_S1_DRAW).addString(getCastle().getName()));

			removeFlags();
			
			teleportPlayer(Siege.TeleportWhoType.Attacker, MapRegionTable.TeleportWhereType.Town);

			teleportPlayer(Siege.TeleportWhoType.DefenderNotOwner, MapRegionTable.TeleportWhereType.Town);

			teleportPlayer(Siege.TeleportWhoType.Spectator, MapRegionTable.TeleportWhereType.Town);

			_isInProgress = false;

			updatePlayerSiegeStateFlags(true);

			saveCastleSiege();

			clearSiegeClan();

			removeArtifact();

			removeControlTower();
			removeFlameTower(); // Remove all flame towers from this castle

			_siegeGuardManager.unspawnSiegeGuard();

			if(getCastle().getOwnerId() > 0)
			{
				_siegeGuardManager.removeMercs();
			}

			getCastle().spawnDoor();
			getCastle().getZone().updateZoneStatusForCharactersInside();
		}
	}

	private void removeDefender(L2SiegeClan sc)
	{
		if(sc != null)
		{
			getDefenderClans().remove(sc);
		}
	}

	private void removeAttacker(L2SiegeClan sc)
	{
		if(sc != null)
		{
			getAttackerClans().remove(sc);
		}
	}

	private void addDefender(L2SiegeClan sc, SiegeClanType type)
	{
		if(sc == null)
		{
			return;
		}

		sc.setType(type);
		getDefenderClans().add(sc);
	}

	private void addAttacker(L2SiegeClan sc)
	{
		if(sc == null)
		{
			return;
		}

		sc.setType(SiegeClanType.ATTACKER);
		getAttackerClans().add(sc);
	}

	public void midVictory()
	{
		if(getIsInProgress())
		{
			if(getCastle().getOwnerId() > 0)
			{
				_siegeGuardManager.removeMercs();
			}

			if(getDefenderClans().size() == 0 && getAttackerClans().size() == 1)
			{
				L2SiegeClan sc_newowner = getAttackerClan(getCastle().getOwnerId());
				removeAttacker(sc_newowner);
				addDefender(sc_newowner, SiegeClanType.OWNER);
				endSiege();
				sc_newowner = null;

				return;
			}

			if(getCastle().getOwnerId() > 0)
			{
				int allyId = ClanTable.getInstance().getClan(getCastle().getOwnerId()).getAllyId();

				if(getDefenderClans().size() == 0)
				{
					if(allyId != 0)
					{
						boolean allinsamealliance = true;

						for(L2SiegeClan sc : getAttackerClans())
						{
							if(sc != null)
							{
								if(ClanTable.getInstance().getClan(sc.getClanId()).getAllyId() != allyId)
								{
									allinsamealliance = false;
								}
							}
						}
						if(allinsamealliance)
						{
							L2SiegeClan sc_newowner = getAttackerClan(getCastle().getOwnerId());
							removeAttacker(sc_newowner);
							addDefender(sc_newowner, SiegeClanType.OWNER);
							endSiege();
							sc_newowner = null;

							return;
						}
					}
				}

				for(L2SiegeClan sc : getDefenderClans())
				{
					if(sc != null)
					{
						removeDefender(sc);
						addAttacker(sc);
					}
				}

				L2SiegeClan sc_newowner = getAttackerClan(getCastle().getOwnerId());
				removeAttacker(sc_newowner);
				addDefender(sc_newowner, SiegeClanType.OWNER);
				sc_newowner = null;

				if(allyId != 0)
				{
					L2Clan[] clanList = ClanTable.getInstance().getClans();

					for(L2Clan clan : clanList)
					{
						if(clan.getAllyId() == allyId)
						{
							L2SiegeClan sc = getAttackerClan(clan.getClanId());

							if(sc != null)
							{
								removeAttacker(sc);
								addDefender(sc, SiegeClanType.DEFENDER);
							}
						}
					}
				}

				teleportPlayer(Siege.TeleportWhoType.Attacker, MapRegionTable.TeleportWhereType.SiegeFlag);

				teleportPlayer(Siege.TeleportWhoType.Spectator, MapRegionTable.TeleportWhereType.Town);

				removeDefenderFlags();

				getCastle().removeUpgrade();

				getCastle().spawnDoor(true);

				removeControlTower();
				removeFlameTower();

				_controlTowerCount = 0;
				_controlTowerMaxCount = 0;
				_flameTowerCount = 0;
				_flameTowerMaxCount = 0;

				spawnControlTower(getCastle().getCastleId());
				spawnFlameTower(getCastle().getCastleId());
				updatePlayerSiegeStateFlags(false);
			}
		}
	}

	public void startSiege()
	{
		if(!getIsInProgress())
		{
			if(getAttackerClans().size() <= 0)
			{
				SystemMessage sm;
				if (getCastle().getOwnerId() <= 0)
					sm = SystemMessage.getSystemMessage(SystemMessageId.SIEGE_OF_S1_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_INTEREST);
				else
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1_SIEGE_WAS_CANCELED_BECAUSE_NO_CLANS_PARTICIPATED);
				sm.addString(getCastle().getName());
				Announcements.getInstance().announceToAll(sm);
				saveCastleSiege();
				return;
			}

			_isNormalSide = true;

			_isInProgress = true;

			loadSiegeClan();
			updatePlayerSiegeStateFlags(false);

			teleportPlayer(Siege.TeleportWhoType.Attacker, MapRegionTable.TeleportWhereType.Town);

			_controlTowerCount = 0;
			_controlTowerMaxCount = 0;

			spawnArtifact(getCastle().getCastleId());
			spawnFlameTower(getCastle().getCastleId());
			spawnControlTower(getCastle().getCastleId());

			getCastle().spawnDoor();

			spawnSiegeGuard();

			MercTicketManager.getInstance().deleteTickets(getCastle().getCastleId());

			_defenderRespawnDelayPenalty = 0;

			getCastle().getZone().updateZoneStatusForCharactersInside();

			_siegeEndDate = Calendar.getInstance();
			_siegeEndDate.add(Calendar.MINUTE, SiegeManager.getInstance().getSiegeLength());
			nProtect.getInstance().checkRestriction(null, RestrictionType.RESTRICT_EVENT, new Object[]
			{
					Siege.class, this
			});
			ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndSiegeTask(getCastle()), 1000);

			Announcements.getInstance().announceToAll(SystemMessage.getSystemMessage(SystemMessageId.SIEGE_OF_S1_HAS_STARTED).addString(getCastle().getName()));
			Broadcast.toAllOnlinePlayers(new PlaySound("systemmsg_e.17"));
		}
	}

	/**
	 * Broadcast a string to defenders.
	 * @param message The String of the message to send to player
	 * @param bothSides if true, broadcast too to attackers clans.
	 */
	public void announceToPlayer(SystemMessage message, boolean bothSides)
	{
		for (L2SiegeClan siegeClans : getDefenderClans())
		{
			L2Clan clan = ClanTable.getInstance().getClan(siegeClans.getClanId());
			for (L2PcInstance member : clan.getOnlineMembers(0))
			{
				if (member != null)
					member.sendPacket(message);
			}
		}
		
		if (bothSides)
		{
			for (L2SiegeClan siegeClans : getAttackerClans())
			{
				L2Clan clan = ClanTable.getInstance().getClan(siegeClans.getClanId());
				for (L2PcInstance member : clan.getOnlineMembers(0))
				{
					if (member != null)
						member.sendPacket(message);
				}
			}
		}
	}

	public void updatePlayerSiegeStateFlags(boolean clear)
	{
		L2Clan clan;

		for(L2SiegeClan siegeclan : getAttackerClans())
		{
			clan = ClanTable.getInstance().getClan(siegeclan.getClanId());

			for(L2PcInstance member : clan.getOnlineMembers(""))
			{
				if(clear)
				{
					member.setSiegeState((byte) 0);
				}
				else
				{
					member.setSiegeState((byte) 1);
				}

				member.sendPacket(new UserInfo(member));

				for(L2PcInstance player : member.getKnownList().getKnownPlayers().values())
				{
					player.sendPacket(new RelationChanged(member, member.getRelation(player), member.isAutoAttackable(player)));
					if(member.getPet() != null) 
						player.sendPacket(new RelationChanged(member.getPet(), member.getRelation(player), member.isAutoAttackable(player)));
				}
			}
		}
		for(L2SiegeClan siegeclan : getDefenderClans())
		{
			clan = ClanTable.getInstance().getClan(siegeclan.getClanId());

			for(L2PcInstance member : clan.getOnlineMembers(""))
			{
				if(clear)
				{
					member.setSiegeState((byte) 0);
				}
				else
				{
					member.setSiegeState((byte) 2);
				}

				member.sendPacket(new UserInfo(member));

				for(L2PcInstance player : member.getKnownList().getKnownPlayers().values())
				{
					player.sendPacket(new RelationChanged(member, member.getRelation(player), member.isAutoAttackable(player)));
					if(member.getPet() != null) 
						player.sendPacket(new RelationChanged(member.getPet(), member.getRelation(player), member.isAutoAttackable(player)));
				}
			}
		}
	}

	public void approveSiegeDefenderClan(int clanId)
	{
		if(clanId <= 0)
		{
			return;
		}

		saveSiegeClan(ClanTable.getInstance().getClan(clanId), 0, true);
		loadSiegeClan();
	}

	public boolean checkIfInZone(L2Object object)
	{
		return checkIfInZone(object.getX(), object.getY(), object.getZ());
	}

	public boolean checkIfInZone(int x, int y, int z)
	{
		return getIsInProgress() && getCastle().checkIfInZone(x, y, z);
	}

	public boolean checkIsAttacker(L2Clan clan)
	{
		return getAttackerClan(clan) != null;
	}

	public boolean checkIsDefender(L2Clan clan)
	{
		return getDefenderClan(clan) != null;
	}

	public boolean checkIsDefenderWaiting(L2Clan clan)
	{
		return getDefenderWaitingClan(clan) != null;
	}

	public void clearSiegeClan()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id = ?");
			statement.setInt(1, getCastle().getCastleId());
			statement.execute();
			statement.close();
			statement = null;

			if(getCastle().getOwnerId() > 0)
			{
				PreparedStatement statement2 = con.prepareStatement("DELETE FROM siege_clans WHERE clan_id = ?");
				statement2.setInt(1, getCastle().getOwnerId());
				statement2.execute();
				statement2.close();
				statement2 = null;
			}

			getAttackerClans().clear();
			getDefenderClans().clear();
			getDefenderWaitingClans().clear();
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public void clearSiegeWaitingClan()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id = ? AND type = 2");
			statement.setInt(1, getCastle().getCastleId());
			statement.execute();
			statement.close();
			statement = null;

			getDefenderWaitingClans().clear();
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public List<L2PcInstance> getAttackersInZone()
	{
		List<L2PcInstance> players = new FastList<L2PcInstance>();
		L2Clan clan;

		for(L2SiegeClan siegeclan : getAttackerClans())
		{
			clan = ClanTable.getInstance().getClan(siegeclan.getClanId());

			for(L2PcInstance player : clan.getOnlineMembers(""))
			{
				if(checkIfInZone(player.getX(), player.getY(), player.getZ()))
				{
					players.add(player);
				}
			}
		}
		return players;
	}

	public List<L2PcInstance> getDefendersButNotOwnersInZone()
	{
		List<L2PcInstance> players = new FastList<L2PcInstance>();
		L2Clan clan;

		for(L2SiegeClan siegeclan : getDefenderClans())
		{
			clan = ClanTable.getInstance().getClan(siegeclan.getClanId());

			if(clan.getClanId() == getCastle().getOwnerId())
			{
				continue;
			}

			for(L2PcInstance player : clan.getOnlineMembers(""))
			{
				if(checkIfInZone(player.getX(), player.getY(), player.getZ()))
				{
					players.add(player);
				}
			}
		}
		return players;
	}

	public List<L2PcInstance> getPlayersInZone()
	{
		return getCastle().getZone().getAllPlayers();
	}

	public List<L2PcInstance> getOwnersInZone()
	{
		List<L2PcInstance> players = new FastList<L2PcInstance>();
		L2Clan clan;

		for(L2SiegeClan siegeclan : getDefenderClans())
		{
			clan = ClanTable.getInstance().getClan(siegeclan.getClanId());

			if(clan.getClanId() != getCastle().getOwnerId())
			{
				continue;
			}

			for(L2PcInstance player : clan.getOnlineMembers(""))
			{
				if(checkIfInZone(player.getX(), player.getY(), player.getZ()))
				{
					players.add(player);
				}
			}
		}
		return players;
	}

	public List<L2PcInstance> getSpectatorsInZone()
	{
		List<L2PcInstance> players = new FastList<L2PcInstance>();

		for(L2PcInstance player : L2World.getInstance().getAllPlayers())
		{
			if(!player.isInsideZone(L2Character.ZONE_SIEGE) || player.getSiegeState() != 0)
			{
				continue;
			}

			if(checkIfInZone(player.getX(), player.getY(), player.getZ()))
			{
				players.add(player);
			}
		}
		return players;
	}

	public void killedCT(L2Npc ct)
	{
		_defenderRespawnDelayPenalty += SiegeManager.getInstance().getControlTowerLosePenalty();
		_controlTowerCount--;

		if(_controlTowerCount < 0)
		{
			_controlTowerCount = 0;
		}

		if(_controlTowerMaxCount > 0)
		{
			if(_controlTowerCount == 0)
				_defenderRespawnDelayPenalty = _controlTowerMaxCount * SiegeManager.getInstance().getControlTowerLosePenalty();
			else
				_defenderRespawnDelayPenalty = (_controlTowerMaxCount - _controlTowerCount) / _controlTowerCount * SiegeManager.getInstance().getControlTowerLosePenalty();
		}
		else
		{
			_defenderRespawnDelayPenalty = 0;
		}
	}

	public void killedFlag(L2Npc flag)
	{
		if(flag == null)
		{
			return;
		}

		for(int i = 0; i < getAttackerClans().size(); i++)
		{
			if(getAttackerClan(i).removeFlag(flag))
			{
				return;
			}
		}
	}

	public void listRegisterClan(L2PcInstance player)
	{
		player.sendPacket(new SiegeInfo(getCastle()));
	}

	public void registerAttacker(L2PcInstance player)
	{
		registerAttacker(player, false);
	}

	public void registerAttacker(L2PcInstance player, boolean force)
	{

		if(player.getClan() == null)
		{
			return;
		}

		int allyId = 0;

		if(getCastle().getOwnerId() != 0)
		{
			allyId = ClanTable.getInstance().getClan(getCastle().getOwnerId()).getAllyId();
		}

		if(allyId != 0)
		{
			if(player.getClan().getAllyId() == allyId && !force)
			{
				player.sendMessage("You cannot register as an attacker because your alliance owns the castle.");
				return;
			}
		}

		if(force || checkIfCanRegister(player))
		{
			saveSiegeClan(player.getClan(), 1, false);
		}
	}

	public void registerDefender(L2PcInstance player)
	{
		registerDefender(player, false);
	}

	public void registerDefender(L2PcInstance player, boolean force)
	{
		if(getCastle().getOwnerId() <= 0)
		{
			player.sendMessage("You cannot register as a defender because " + getCastle().getName() + " is owned by NPC.");
		}
		else if(force || checkIfCanRegister(player))
		{
			saveSiegeClan(player.getClan(), 2, false);
		}
	}

	public void removeSiegeClan(int clanId)
	{
		if(clanId <= 0)
		{
			return;
		}

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM siege_clans WHERE castle_id = ? AND clan_id = ?");
			statement.setInt(1, getCastle().getCastleId());
			statement.setInt(2, clanId);
			statement.execute();
			statement.close();
			statement = null;

			loadSiegeClan();
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public void removeSiegeClan(L2Clan clan)
	{
		if(clan == null || clan.getHasCastle() == getCastle().getCastleId() || !SiegeManager.getInstance().checkIsRegistered(clan, getCastle().getCastleId()))
		{
			return;
		}

		removeSiegeClan(clan.getClanId());
	}

	public void removeSiegeClan(L2PcInstance player)
	{
		removeSiegeClan(player.getClan());
	}

	public void startAutoTask()
	{
		correctSiegeDateTime();

		System.out.println("Siege of " + getCastle().getName() + ": " + getCastle().getSiegeDate().getTime());

		loadSiegeClan();

		if (_scheduledStartSiegeTask != null) 
			_scheduledStartSiegeTask.cancel(false); 
		_scheduledStartSiegeTask = ThreadPoolManager.getInstance().scheduleGeneral(new Siege.ScheduleStartSiegeTask(getCastle()), 1000); 
	}

	public void teleportPlayer(TeleportWhoType teleportWho, MapRegionTable.TeleportWhereType teleportWhere)
	{
		List<L2PcInstance> players;
		switch(teleportWho)
		{
			case Owner:
				players = getOwnersInZone();
				break;
			case Attacker:
				players = getAttackersInZone();
				break;
			case DefenderNotOwner:
				players = getDefendersButNotOwnersInZone();
				break;
			case Spectator:
				players = getSpectatorsInZone();
				break;
			default:
				players = getPlayersInZone();
		}

		for(L2PcInstance player : players)
		{
			if(player.isGM() || player.isInJail())
			{
				continue;
			}

			player.teleToLocation(teleportWhere);
		}
	}

	private void addAttacker(int clanId)
	{
		getAttackerClans().add(new L2SiegeClan(clanId, SiegeClanType.ATTACKER));
	}

	private void addDefender(int clanId)
	{
		getDefenderClans().add(new L2SiegeClan(clanId, SiegeClanType.DEFENDER));
	}

	private void addDefender(int clanId, SiegeClanType type)
	{
		getDefenderClans().add(new L2SiegeClan(clanId, type));
	}

	private void addDefenderWaiting(int clanId)
	{
		getDefenderWaitingClans().add(new L2SiegeClan(clanId, SiegeClanType.DEFENDER_PENDING));
	}

	private boolean checkIfCanRegister(L2PcInstance player)
	{
		if(getIsRegistrationOver())
		{
			player.sendMessage("The deadline to register for the siege of " + getCastle().getName() + " has passed.");
		}
		else if(getIsInProgress())
		{
			player.sendMessage("This is not the time for siege registration and so registration and cancellation cannot be done.");
		}
		else if(player.getClan() == null || player.getClan().getLevel() < SiegeManager.getInstance().getSiegeClanMinLevel())
		{
			player.sendMessage("Only clans with Level " + SiegeManager.getInstance().getSiegeClanMinLevel() + " and higher may register for a castle siege.");
		}
		else if(player.getClan().getHasCastle() > 0)
		{
			player.sendMessage("You cannot register because your clan already own a castle.");
		}
		else if(player.getClan().getClanId() == getCastle().getOwnerId())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_THAT_OWNS_CASTLE_IS_AUTOMATICALLY_REGISTERED_DEFENDING));
		}
		else if(checkIfAlreadyRegisteredForAnotherSiege(player.getClan()))
		{
			player.sendMessage("You are already registered in another Siege.");
		}
		else
		{
			for(int i=0; i<10; i++)
			{
				if(SiegeManager.getInstance().checkIsRegistered(player.getClan(), i))
				{
					player.sendMessage("You are already registered in a Siege.");
					return false;
				}
			}
		}
		return true;
	}

	private boolean checkIfAlreadyRegisteredForAnotherSiege(L2Clan clan)
	{
		for(Siege siege : SiegeManager.getInstance().getSieges())
		{
			if(siege == this)
			{
				continue;
			}
			//if(siege.getSiegeDate().get(Calendar.DAY_OF_WEEK) == this.getSiegeDate().get(Calendar.DAY_OF_WEEK))
			//{
				if(siege.checkIsAttacker(clan))
				{
					return true;
				}

				if(siege.checkIsDefender(clan))
				{
					return true;
				}

				if(siege.checkIsDefenderWaiting(clan))
				{
					return true;
				}
			//}
		}
		return false;
	}

	public void correctSiegeDateTime()
	{
		// Siege time has past, or siege is in Seven Signs Seal period.
		if ((getCastle().getSiegeDate().getTimeInMillis() < Calendar.getInstance().getTimeInMillis()) || (!SevenSigns.getInstance().isDateInSealValidPeriod(getCastle().getSiegeDate())))
		{
			setNextSiegeDate();
			saveSiegeDate();
		}
	}

	private void loadSiegeClan()
	{
		Connection con = null;
		try
		{
			getAttackerClans().clear();
			getDefenderClans().clear();
			getDefenderWaitingClans().clear();

			if(getCastle().getOwnerId() > 0)
			{
				addDefender(getCastle().getOwnerId(), SiegeClanType.OWNER);
			}

			PreparedStatement statement = null;
			ResultSet rs = null;

			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("SELECT clan_id, type FROM siege_clans WHERE castle_id = ?");
			statement.setInt(1, getCastle().getCastleId());
			rs = statement.executeQuery();

			int typeId;

			while(rs.next())
			{
				typeId = rs.getInt("type");

				if(typeId == 0)
				{
					addDefender(rs.getInt("clan_id"));
				}
				else if(typeId == 1)
				{
					addAttacker(rs.getInt("clan_id"));
				}
				else if(typeId == 2)
				{
					addDefenderWaiting(rs.getInt("clan_id"));
				}
			}

			statement.close();
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	private void removeArtifact()
	{
		if(_artifacts != null)
		{
			for(L2ArtefactInstance art : _artifacts)
			{
				if(art != null)
				{
					art.decayMe();
				}
			}
			_artifacts = null;
		}
	}

	private void removeControlTower()
	{
		if(_controlTowers != null)
		{
			for(L2ControlTowerInstance ct : _controlTowers)
			{
				if(ct != null)
				{
					ct.decayMe();
				}
			}

			_controlTowers = null;
		}
	}

	/** Remove all flame towers spawned. */
	private void removeFlameTower()
	{
		if (_flameTowers != null && !_flameTowers.isEmpty())
		{
			// Remove all instances of control tower for this castle
			for (L2FlameTowerInstance ct : _flameTowers)
			{
				if (ct != null)
					ct.deleteMe();
			}
			_flameTowers.clear();
			_flameTowers = null;
		}
	}
	
	private void removeFlags()
	{
		for(L2SiegeClan sc : getAttackerClans())
		{
			if(sc != null)
			{
				sc.removeFlags();
			}
		}
		for(L2SiegeClan sc : getDefenderClans())
		{
			if(sc != null)
			{
				sc.removeFlags();
			}
		}
	}

	private void removeDefenderFlags()
	{
		for(L2SiegeClan sc : getDefenderClans())
		{
			if(sc != null)
			{
				sc.removeFlags();
			}
		}
	}

	private void saveCastleSiege()
	{
		setNextSiegeDate();
		// Schedule Time registration end 
		getTimeRegistrationOverDate().setTimeInMillis(Calendar.getInstance().getTimeInMillis()); 
		getTimeRegistrationOverDate().add(Calendar.DAY_OF_MONTH, 1); 
		getCastle().setIsTimeRegistrationOver(false);
		saveSiegeDate();
		startAutoTask();
	}

	public void saveSiegeDate()
	{
		if (_scheduledStartSiegeTask != null) 
		{ 
			_scheduledStartSiegeTask.cancel(true); 
			_scheduledStartSiegeTask = ThreadPoolManager.getInstance().scheduleGeneral(new Siege.ScheduleStartSiegeTask(getCastle()), 1000); 
		}
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("Update castle set siegeDate = ?, regTimeEnd = ?, regTimeOver = ? where id = ?");
			statement.setLong(1, getCastle().getSiegeDate().getTimeInMillis());
			statement.setLong(2, getTimeRegistrationOverDate().getTimeInMillis()); 
			statement.setString(3, String.valueOf(getIsTimeRegistrationOver())); 
			statement.setInt(4, getCastle().getCastleId());
			statement.execute();

			statement.close();
			statement = null;
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	private void saveSiegeClan(L2Clan clan, int typeId, boolean isUpdateRegistration)
	{
		if(clan.getHasCastle() > 0)
		{
			return;
		}

		Connection con = null;
		try
		{
			if(typeId == 0 || typeId == 2 || typeId == -1)
			{
				if(getDefenderClans().size() + getDefenderWaitingClans().size() >= SiegeManager.getInstance().getDefenderMaxClans())
				{
					return;
				}
			}
			else
			{
				if(getAttackerClans().size() >= SiegeManager.getInstance().getAttackerMaxClans())
				{
					return;
				}
			}

			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;

			if(!isUpdateRegistration)
			{
				statement = con.prepareStatement("INSERT INTO siege_clans (clan_id, castle_id, type, castle_owner) VALUES (?, ?, ?, 0)");
				statement.setInt(1, clan.getClanId());
				statement.setInt(2, getCastle().getCastleId());
				statement.setInt(3, typeId);
				statement.execute();
				statement.close();
				statement = null;
			}
			else
			{
				statement = con.prepareStatement("UPDATE siege_clans SET type = ? WHERE castle_id = ? AND clan_id = ?");
				statement.setInt(1, typeId);
				statement.setInt(2, getCastle().getCastleId());
				statement.setInt(3, clan.getClanId());
				statement.execute();
				statement.close();
				statement = null;
			}

			if(typeId == 0 || typeId == -1)
			{
				addDefender(clan.getClanId());
				announceToPlayer(clan.getName() + " has been registered to defend " + getCastle().getName(), false);
			}
			else if(typeId == 1)
			{
				addAttacker(clan.getClanId());
				announceToPlayer(clan.getName() + " has been registered to attack " + getCastle().getName(), false);
			}
			else if(typeId == 2)
			{
				addDefenderWaiting(clan.getClanId());
				announceToPlayer(clan.getName() + " has requested to defend " + getCastle().getName(), false);
			}
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public void setNextSiegeDate()
	{
		// Copy of siege date. All modifications are made on it, then once ended, it is registered.
		Calendar siegeDate = getCastle().getSiegeDate();
		
		// Loop until current time is lower than next siege period.
		while (siegeDate.getTimeInMillis() < Calendar.getInstance().getTimeInMillis())
		{
			// If current day is another than Saturday or Sunday, change it accordingly to castle
			if (siegeDate.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY || siegeDate.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY)
			{
				switch (getCastle().getCastleId())
				{
					case 3:
					case 4:
					case 6:
					case 7:
						siegeDate.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
						break;
					
					default:
						siegeDate.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
						break;
				}
			}
			
			// Set next siege date if siege has passed ; add 14 days (2 weeks).
			siegeDate.add(Calendar.DAY_OF_MONTH, 14);
		}
		
		// If the siege date goes on a Seven Signs seal period, add 7 days (1 week).
		if (!SevenSigns.getInstance().isDateInSealValidPeriod(siegeDate))
			siegeDate.add(Calendar.DAY_OF_MONTH, 7);
		
		// After all modifications are applied on local variable, register the time as siege date of that castle.
		getCastle().getSiegeDate().setTimeInMillis(siegeDate.getTimeInMillis());
		
		// Send message and allow registration for next siege.
		Announcements.getInstance().announceToAll(SystemMessage.getSystemMessage(SystemMessageId.S1_ANNOUNCED_SIEGE_TIME).addString(getCastle().getName()));
		_isRegistrationOver = false;
	}

	private void spawnArtifact(int Id)
	{
		if(_artifacts == null)
		{
			_artifacts = new FastList<L2ArtefactInstance>();
		}

		for(SiegeSpawn _sp : SiegeManager.getInstance().getArtefactSpawnList(Id))
		{
			L2ArtefactInstance art;

			art = new L2ArtefactInstance(IdFactory.getInstance().getNextId(), NpcTable.getInstance().getTemplate(_sp.getNpcId()));
			art.setCurrentHpMp(art.getMaxHp(), art.getMaxMp());
			art.setHeading(_sp.getLocation().getHeading());
			art.spawnMe(_sp.getLocation().getX(), _sp.getLocation().getY(), _sp.getLocation().getZ() + 50);

			_artifacts.add(art);
			art = null;
		}
	}

	/** Spawn flame tower. */
	private void spawnFlameTower(int Id)
	{
		//Set control tower array size if one does not exist
		if (_flameTowers == null)
			_flameTowers = new FastList<L2FlameTowerInstance>();

		for (SiegeSpawn _sp : SiegeManager.getInstance().getFlameTowerSpawnList(Id))
		{
			L2FlameTowerInstance ct;

			L2NpcTemplate template = NpcTable.getInstance().getTemplate(_sp.getNpcId());

			ct = new L2FlameTowerInstance(IdFactory.getInstance().getNextId(), template);

			ct.setCurrentHpMp(_sp.getHp(), ct.getMaxMp());
			ct.spawnMe(_sp.getLocation().getX(), _sp.getLocation().getY(), _sp.getLocation().getZ() + 20);

			_flameTowerCount++;
			_flameTowerMaxCount++;
			_flameTowers.add(ct);
		}

		if (_flameTowerCount == 0) //TODO: temp fix until flame towers are assigned in config
			_flameTowerCount = 1;
	}

	private void spawnControlTower(int Id)
	{
		if(_controlTowers == null)
		{
			_controlTowers = new FastList<L2ControlTowerInstance>();
		}

		for(SiegeSpawn _sp : SiegeManager.getInstance().getControlTowerSpawnList(Id))
		{
			L2ControlTowerInstance ct;

			L2NpcTemplate template = NpcTable.getInstance().getTemplate(_sp.getNpcId());

			template.getStatsSet().set("baseHpMax", _sp.getHp());

			ct = new L2ControlTowerInstance(IdFactory.getInstance().getNextId(), template);

			ct.setCurrentHpMp(ct.getMaxHp(), ct.getMaxMp());
			ct.spawnMe(_sp.getLocation().getX(), _sp.getLocation().getY(), _sp.getLocation().getZ() + 20);
			_controlTowerCount++;
			_controlTowerMaxCount++;
			_controlTowers.add(ct);
		}
	}

	private void spawnSiegeGuard()
	{
		getSiegeGuardManager().spawnSiegeGuard();

		if(getSiegeGuardManager().getSiegeGuardSpawn().size() > 0 && _controlTowers.size() > 0)
		{
			L2ControlTowerInstance closestCt;

			double distance, x, y, z;
			double distanceClosest = 0;

			for(L2Spawn spawn : getSiegeGuardManager().getSiegeGuardSpawn())
			{
				if(spawn == null)
				{
					continue;
				}

				closestCt = null;
				distanceClosest = 0;

				for(L2ControlTowerInstance ct : _controlTowers)
				{
					if(ct == null)
					{
						continue;
					}

					x = spawn.getLocx() - ct.getX();
					y = spawn.getLocy() - ct.getY();
					z = spawn.getLocz() - ct.getZ();

					distance = x * x + y * y + z * z;

					if(closestCt == null || distance < distanceClosest)
					{
						closestCt = ct;
						distanceClosest = distance;
					}
				}

				if(closestCt != null)
				{
					closestCt.registerGuard(spawn);
				}
			}
		}
	}

	public final L2SiegeClan getAttackerClan(L2Clan clan)
	{
		if(clan == null)
		{
			return null;
		}

		return getAttackerClan(clan.getClanId());
	}

	public final L2SiegeClan getAttackerClan(int clanId)
	{
		for(L2SiegeClan sc : getAttackerClans())
		{
			if(sc != null && sc.getClanId() == clanId)
			{
				return sc;
			}
		}

		return null;
	}

	public final List<L2SiegeClan> getAttackerClans()
	{
		if(_isNormalSide)
		{
			return _attackerClans;
		}

		return _defenderClans;
	}

	public final int getAttackerRespawnDelay()
	{
		return SiegeManager.getInstance().getAttackerRespawnDelay();
	}

	public final Castle getCastle()
	{
		if(_castle == null || _castle.length <= 0)
		{
			return null;
		}

		return _castle[0];
	}

	public final L2SiegeClan getDefenderClan(L2Clan clan)
	{
		if(clan == null)
		{
			return null;
		}

		return getDefenderClan(clan.getClanId());
	}

	public final L2SiegeClan getDefenderClan(int clanId)
	{
		for(L2SiegeClan sc : getDefenderClans())
		{
			if(sc != null && sc.getClanId() == clanId)
			{
				return sc;
			}
		}

		return null;
	}

	public final List<L2SiegeClan> getDefenderClans()
	{
		if(_isNormalSide)
		{
			return _defenderClans;
		}

		return _attackerClans;
	}

	public final L2SiegeClan getDefenderWaitingClan(L2Clan clan)
	{
		if(clan == null)
		{
			return null;
		}

		return getDefenderWaitingClan(clan.getClanId());
	}

	public final L2SiegeClan getDefenderWaitingClan(int clanId)
	{
		for(L2SiegeClan sc : getDefenderWaitingClans())
		{
			if(sc != null && sc.getClanId() == clanId)
			{
				return sc;
			}
		}

		return null;
	}

	public final List<L2SiegeClan> getDefenderWaitingClans()
	{
		return _defenderWaitingClans;
	}

	public final int getDefenderRespawnDelay()
	{
		return SiegeManager.getInstance().getDefenderRespawnDelay() + _defenderRespawnDelayPenalty;
	}

	public final boolean getIsInProgress()
	{
		return _isInProgress;
	}

	public final boolean getIsRegistrationOver()
	{
		return _isRegistrationOver;
	}

	public final boolean getIsTimeRegistrationOver()
	{
		return getCastle().getIsTimeRegistrationOver();
	}
	
	public final Calendar getSiegeDate()
	{
		return getCastle().getSiegeDate();
	}

	public final Calendar getTimeRegistrationOverDate()
	{
		return getCastle().getTimeRegistrationOverDate();
	}
	
	public void endTimeRegistration(boolean automatic) 
	{ 
		getCastle().setIsTimeRegistrationOver(true); 
		if (!automatic) 
			saveSiegeDate(); 
	}
	
	public List<L2Npc> getFlag(L2Clan clan)
	{
		if(clan != null)
		{
			L2SiegeClan sc = getAttackerClan(clan);
			if(sc != null)
			{
				return sc.getFlag();
			}
		}
		return null;
	}

	public final SiegeGuardManager getSiegeGuardManager()
	{
		if(_siegeGuardManager == null)
		{
			_siegeGuardManager = new SiegeGuardManager(getCastle());
		}

		return _siegeGuardManager;
	}

	 public void announceToPlayer(String message, boolean inAreaOnly)
	 {
		 if(inAreaOnly)
		 {
			 getCastle().getZone().announceToPlayers(message);
			 return;
		 }
		 
		 for(L2PcInstance player : L2World.getInstance().getAllPlayers())
		 {
			 player.sendMessage(message);
		 }
	 }
}