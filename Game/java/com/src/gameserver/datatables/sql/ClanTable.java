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
package com.src.gameserver.datatables.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.idfactory.IdFactory;
import com.src.gameserver.managers.SiegeManager;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.L2ClanMember;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.siege.Siege;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.MagicSkillUser;
import com.src.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import com.src.gameserver.network.serverpackets.PledgeShowMemberListAll;
import com.src.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.network.serverpackets.UserInfo;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.gameserver.util.Util;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class ClanTable
{
	private static Log _log = LogFactory.getLog(ClanTable.class);

	private static ClanTable _instance;

	private Map<Integer, L2Clan> _clans;

	public static ClanTable getInstance()
	{
		if(_instance == null)
		{
			_instance = new ClanTable();
		}

		return _instance;
	}

	public L2Clan[] getClans()
	{
		return _clans.values().toArray(new L2Clan[_clans.size()]);
	}

	public int getTopRate(int clan_id)
	{
		L2Clan clan = getClan(clan_id);
		if(clan.getLevel() < 3)
		{
			return 0;
		}
		int i = 1;
		for(L2Clan clans : getClans())
		{
			if(clan != clans)
			{
				if(clan.getLevel() < clans.getLevel())
				{
					i++;
				}
				else if(clan.getLevel() == clans.getLevel())
				{
					if(clan.getReputationScore() <= clans.getReputationScore())
					{
						i++;
					}
				}
			}
		}

		clan = null;
		return i;
	}

	private ClanTable()
	{
		_clans = new FastMap<Integer, L2Clan>();
		L2Clan clan;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT clan_id FROM clan_data");
			ResultSet result = statement.executeQuery();

			int clanCount = 0;

			while(result.next())
			{
				_clans.put(Integer.parseInt(result.getString("clan_id")), new L2Clan(Integer.parseInt(result.getString("clan_id"))));
				clan = getClan(Integer.parseInt(result.getString("clan_id")));
				if(clan.getDissolvingExpiryTime() != 0)
				{
					if(clan.getDissolvingExpiryTime() < System.currentTimeMillis())
					{
						destroyClan(clan.getClanId(), con);
					}
					else
					{
						scheduleRemoveClan(clan.getClanId());
					}
				}
				clanCount++;
			}
			statement.close();
			statement = null;

			_log.info("Clans: Loaded " + clanCount + " clans from the database.");
		}
		catch(Exception e)
		{
			_log.warn("data error on ClanTable: " + e);
			e.printStackTrace();
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}

		restorewars();
	}

	public L2Clan getClan(int clanId)
	{
		L2Clan clan = _clans.get(new Integer(clanId));

		return clan;
	}

	public L2Clan getClanByName(String clanName)
	{
		for(L2Clan clan : getClans())
		{
			if(clan.getName().equalsIgnoreCase(clanName))
			{
				return clan;
			}
		}

		return null;
	}

	public L2Clan createClan(L2PcInstance player, String clanName)
	{
		if(null == player)
		{
			return null;
		}

		if(10 > player.getLevel())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_CLAN));
			return null;
		}

		if(0 != player.getClanId())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.FAILED_TO_CREATE_CLAN));
			return null;
		}

		if(System.currentTimeMillis() < player.getClanCreateExpiryTime())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_MUST_WAIT_XX_DAYS_BEFORE_CREATING_A_NEW_CLAN));
			return null;
		}

		if(!isValidClanName(player, clanName))
		{
			return null;
		}

		L2Clan clan = new L2Clan(IdFactory.getInstance().getNextId(), clanName);
		L2ClanMember leader = new L2ClanMember(clan, player.getName(), player.getLevel(), player.getClassId().getId(), player.getObjectId(), player.getPledgeType(), player.getPowerGrade(), player.getTitle());

		clan.setLeader(leader);
		leader.setPlayerInstance(player);
		clan.store();
		player.setClan(clan);
		player.setPledgeClass(L2ClanMember.calculatePledgeClass(player));
		player.setClanPrivileges(L2Clan.CP_ALL);

		_clans.put(new Integer(clan.getClanId()), clan);

		player.sendPacket(new MagicSkillUser(player, 5103, 1, 1000, 0));
		player.sendPacket(new PledgeShowInfoUpdate(clan));
		player.sendPacket(new PledgeShowMemberListAll(clan, player));
		player.sendPacket(new UserInfo(player));
		player.sendPacket(new PledgeShowMemberListUpdate(player));
		player.sendPacket(new SystemMessage(SystemMessageId.CLAN_CREATED));

		leader = null;

		return clan;
	}

	public boolean isValidClanName(L2PcInstance player, String clanName)
	{
		if(!Util.isAlphaNumeric(clanName) || clanName.length() < 2)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_NAME_INCORRECT));
			return false;
		}

		if(clanName.length() > 16)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_NAME_TOO_LONG));
			return false;
		}

		if(getClanByName(clanName) != null)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.S1_ALREADY_EXISTS).addString(clanName));
			return false;
		}

		Pattern pattern;
		try
		{
			pattern = Pattern.compile(Config.CLAN_NAME_TEMPLATE);
		}
		catch(PatternSyntaxException e)
		{
			_log.warn("ERROR : Clan name pattern of config is wrong!", e);
			pattern = Pattern.compile(".*");
		}

		Matcher match = pattern.matcher(clanName);

		if(!match.matches())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_NAME_INCORRECT));
			return false;
		}

		return true;
	}

	@SuppressWarnings("resource")
	public synchronized void destroyClan(int clanId, Connection con)
	{
		L2Clan clan = getClan(clanId);

		if(clan == null)
		{
			return;
		}

		L2PcInstance leader = null;
		if(clan.getLeader() != null)
		{
			leader = clan.getLeader().getPlayerInstance();
		}

		if(leader != null)
		{
			if(Config.CLAN_LEADER_COLOR_ENABLED && clan.getLevel() >= Config.CLAN_LEADER_COLOR_CLAN_LEVEL && leader != null)
			{
				if(Config.CLAN_LEADER_COLORED == 1)
				{
					leader.getAppearance().setNameColor(0x000000);
				}
				else
				{
					leader.getAppearance().setTitleColor(0xFFFF77);
				}
			}
		}

		clan.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.CLAN_HAS_DISPERSED));

		int castleId = clan.getHasCastle();

		if(castleId == 0)
		{
			for(Siege siege : SiegeManager.getInstance().getSieges())
			{
				siege.removeSiegeClan(clanId);
			}
		}

		L2ClanMember leaderMember = clan.getLeader();

		if(leaderMember == null)
		{
			clan.getWarehouse().destroyAllItems("ClanRemove", null, null);
		}
		else
		{
			clan.getWarehouse().destroyAllItems("ClanRemove", clan.getLeader().getPlayerInstance(), null);
		}

		leaderMember = null;

		for(L2ClanMember member : clan.getMembers())
		{
			clan.removeClanMember(member.getName(), 0);
		}

		int leaderId = clan.getLeaderId();
		int clanLvl = clan.getLevel();

		clan = null;

		_clans.remove(clanId);
		IdFactory.getInstance().releaseId(clanId);

		try
		{
			if(con == null)
			{
				con = L2DatabaseFactory.getInstance().getConnection();
			}
			PreparedStatement statement = con.prepareStatement("DELETE FROM clan_data WHERE clan_id = ?");
			statement.setInt(1, clanId);
			statement.execute();

			statement = con.prepareStatement("DELETE FROM clan_privs WHERE clan_id = ?");
			statement.setInt(1, clanId);
			statement.execute();

			statement = con.prepareStatement("DELETE FROM clan_skills WHERE clan_id = ?");
			statement.setInt(1, clanId);
			statement.execute();

			statement = con.prepareStatement("DELETE FROM clan_subpledges WHERE clan_id = ?");
			statement.setInt(1, clanId);
			statement.execute();

			statement = con.prepareStatement("DELETE FROM clan_wars WHERE clan1 = ? OR clan2 = ?");
			statement.setInt(1, clanId);
			statement.setInt(2, clanId);
			statement.execute();

			if(leader == null && leaderId != 0 && Config.CLAN_LEADER_COLOR_ENABLED && clanLvl >= Config.CLAN_LEADER_COLOR_CLAN_LEVEL)
			{
				if(Config.CLAN_LEADER_COLORED == 1)
				{
					statement = con.prepareStatement("UPDATE characters SET name_color = '000000' WHERE odj_Id = ?");
				}
				else
				{
					statement = con.prepareStatement("UPDATE characters SET title_color = 'FFFF77' WHERE odj_Id = ?");
				}
				statement.setInt(1, leaderId);
				statement.execute();
			}

			if(castleId != 0)
			{
				statement = con.prepareStatement("UPDATE castle SET taxPercent = 0 WHERE id = ?");
				statement.setInt(2, castleId);
				statement.execute();
			}

			statement.close();
		}
		catch(Exception e)
		{
			_log.warn("error while removing clan in db", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	public void scheduleRemoveClan(final int clanId)
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				if(getClan(clanId) == null)
				{
					return;
				}

				if(getClan(clanId).getDissolvingExpiryTime() != 0)
				{
					destroyClan(clanId, null);
				}
			}
		}, getClan(clanId).getDissolvingExpiryTime() - System.currentTimeMillis());
	}

	public boolean isAllyExists(String allyName)
	{
		for(L2Clan clan : getClans())
		{
			if(clan.getAllyName() != null && clan.getAllyName().equalsIgnoreCase(allyName))
			{
				return true;
			}
		}

		return false;
	}

	public void storeclanswars(int clanId1, int clanId2)
	{
		L2Clan clan1 = ClanTable.getInstance().getClan(clanId1);
		L2Clan clan2 = ClanTable.getInstance().getClan(clanId2);

		clan1.setEnemyClan(clan2);
		clan2.setAttackerClan(clan1);
		clan1.broadcastClanStatus();
		clan2.broadcastClanStatus();

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("REPLACE INTO clan_wars (clan1, clan2, wantspeace1, wantspeace2) VALUES (?, ?, ?, ?)");
			statement.setInt(1, clanId1);
			statement.setInt(2, clanId2);
			statement.setInt(3, 0);
			statement.setInt(4, 0);
			statement.execute();
			statement.close();
			statement = null;
		}
		catch(Exception e)
		{
			_log.error("could not store clans wars data", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}

		clan1.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.CLAN_WAR_DECLARED_AGAINST_S1_IF_KILLED_LOSE_LOW_EXP).addString(clan2.getName()));
		clan2.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.CLAN_S1_DECLARED_WAR).addString(clan1.getName()));

		clan1 = null;
		clan2 = null;
	}

	public void deleteclanswars(int clanId1, int clanId2)
	{
		L2Clan clan1 = ClanTable.getInstance().getClan(clanId1);
		L2Clan clan2 = ClanTable.getInstance().getClan(clanId2);

		clan1.deleteEnemyClan(clan2);
		clan2.deleteAttackerClan(clan1);
		clan1.broadcastClanStatus();
		clan2.broadcastClanStatus();

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("DELETE FROM clan_wars WHERE clan1 = ? AND clan2 = ?");
			statement.setInt(1, clanId1);
			statement.setInt(2, clanId2);
			statement.execute();

			statement.close();
			statement = null;
		}
		catch(Exception e)
		{
			_log.warn("could not restore clans wars data", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}

		clan1.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.WAR_AGAINST_S1_HAS_STOPPED).addString(clan2.getName()));
		clan2.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.CLAN_S1_HAS_DECIDED_TO_STOP).addString(clan1.getName()));
	}

	public void checkSurrender(L2Clan clan1, L2Clan clan2)
	{
		int count = 0;

		for(L2ClanMember player : clan1.getMembers())
		{
			if(player != null && player.getPlayerInstance().getWantsPeace() == 1)
			{
				count++;
			}
		}

		if(count == clan1.getMembers().length - 1)
		{
			clan1.deleteEnemyClan(clan2);
			clan2.deleteEnemyClan(clan1);
			deleteclanswars(clan1.getClanId(), clan2.getClanId());
		}
	}

	private void restorewars()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT clan1, clan2, wantspeace1, wantspeace2 FROM clan_wars");
			ResultSet rset = statement.executeQuery();

			while(rset.next())
			{
				getClan(rset.getInt("clan1")).setEnemyClan(rset.getInt("clan2"));
				getClan(rset.getInt("clan2")).setAttackerClan(rset.getInt("clan1"));
			}
			rset.close();
			statement.close();
		}
		catch(Exception e)
		{
			_log.warn("could not restore clan wars data", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}
}