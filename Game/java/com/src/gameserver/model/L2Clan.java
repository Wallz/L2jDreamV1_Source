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
package com.src.gameserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.communitybbs.BB.Forum;
import com.src.gameserver.communitybbs.Manager.ForumsBBSManager;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.managers.CrownManager;
import com.src.gameserver.managers.SiegeManager;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.itemcontainer.ItemContainer;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.network.serverpackets.L2GameServerPacket;
import com.src.gameserver.network.serverpackets.MagicSkillUser;
import com.src.gameserver.network.serverpackets.PledgeReceiveSubPledgeCreated;
import com.src.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import com.src.gameserver.network.serverpackets.PledgeShowMemberListAll;
import com.src.gameserver.network.serverpackets.PledgeShowMemberListDeleteAll;
import com.src.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import com.src.gameserver.network.serverpackets.PledgeSkillList;
import com.src.gameserver.network.serverpackets.PledgeSkillListAdd;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.network.serverpackets.UserInfo;
import com.src.gameserver.util.Util;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class L2Clan
{
	private static final Log _log = LogFactory.getLog(L2Clan.class.getName());

	private String _name;
	private int _clanId;
	private L2ClanMember _leader;
	private Map<String, L2ClanMember> _members = new FastMap<String, L2ClanMember>();

	private String _allyName;
	private int _allyId;
	private int _level;
	private int _hasCastle;
	private int _hasHideout;
	private boolean _hasCrest;
	private int _hiredGuards;
	private int _crestId;
	private int _crestLargeId;
	private int _allyCrestId;
	private int _auctionBiddedAt = 0;
	private long _allyPenaltyExpiryTime;
	private int _allyPenaltyType;
	private long _charPenaltyExpiryTime;
	private long _dissolvingExpiryTime;
	public static final int PENALTY_TYPE_CLAN_LEAVED = 1;
	public static final int PENALTY_TYPE_CLAN_DISMISSED = 2;
	public static final int PENALTY_TYPE_DISMISS_CLAN = 3;
	public static final int PENALTY_TYPE_DISSOLVE_ALLY = 4;
	private ItemContainer _warehouse = new ClanWarehouse(this);
	private List<Integer> _atWarWith = new FastList<Integer>();
	private List<Integer> _atWarAttackers = new FastList<Integer>();
	private boolean _hasCrestLarge;
	private Forum _forum;
	private List<L2Skill> _skillList = new FastList<L2Skill>();
	private String _notice;
	private boolean _noticeEnabled = false;
	private static final int MAX_NOTICE_LENGTH = 512;
	public static final int CP_NOTHING = 0;
	public static final int CP_CL_JOIN_CLAN = 2;
	public static final int CP_CL_GIVE_TITLE = 4;
	public static final int CP_CL_VIEW_WAREHOUSE = 8;
	public static final int CP_CL_MANAGE_RANKS = 16;
	public static final int CP_CL_PLEDGE_WAR = 32;
	public static final int CP_CL_DISMISS = 64;
	public static final int CP_CL_REGISTER_CREST = 128;
	public static final int CP_CL_MASTER_RIGHTS = 256;
	public static final int CP_CL_MANAGE_LEVELS = 512;
	public static final int CP_CH_OPEN_DOOR = 1024;
	public static final int CP_CH_OTHER_RIGHTS = 2048;
	public static final int CP_CH_AUCTION = 4096;
	public static final int CP_CH_DISMISS = 8192;
	public static final int CP_CH_SET_FUNCTIONS = 16384;
	public static final int CP_CS_OPEN_DOOR = 32768;
	public static final int CP_CS_MANOR_ADMIN = 65536;
	public static final int CP_CS_MANAGE_SIEGE = 131072;
	public static final int CP_CS_USE_FUNCTIONS = 262144;
	public static final int CP_CS_DISMISS = 524288;
	public static final int CP_CS_TAXES = 1048576;
	public static final int CP_CS_MERCENARIES = 2097152;
	public static final int CP_CS_SET_FUNCTIONS = 4194304;
	public static final int CP_ALL = 8388606;
	public static final int SUBUNIT_ACADEMY = -1;
	public static final int SUBUNIT_ROYAL1 = 100;
	public static final int SUBUNIT_ROYAL2 = 200;
	public static final int SUBUNIT_KNIGHT1 = 1001;
	public static final int SUBUNIT_KNIGHT2 = 1002;
	public static final int SUBUNIT_KNIGHT3 = 2001;
	public static final int SUBUNIT_KNIGHT4 = 2002;
	protected final Map<Integer, L2Skill> _skills = new FastMap<Integer, L2Skill>();
	protected final Map<Integer, RankPrivs> _privs = new FastMap<Integer, RankPrivs>();
	protected final Map<Integer, SubPledge> _subPledges = new FastMap<Integer, SubPledge>();

	private int _reputationScore = 0;
	private int _rank = 0;

	public L2Clan(int clanId)
	{
		_clanId = clanId;
		initializePrivs();
		restore();
		getWarehouse().restore();
	}

	public L2Clan(int clanId, String clanName)
	{
		_clanId = clanId;
		_name = clanName;
		initializePrivs();
	}

	public int getClanId()
	{
		return _clanId;
	}

	public void setClanId(int clanId)
	{
		_clanId = clanId;
	}

	public int getLeaderId()
	{
		return _leader != null ? _leader.getObjectId() : 0;
	}

	public L2ClanMember getLeader()
	{
		return _leader;
	}

	public void setLeader(L2ClanMember leader)
	{
		_leader = leader;
		_members.put(leader.getName(), leader);
	}

	public void setNewLeader(L2ClanMember member,L2PcInstance activeChar)
	{
		if(activeChar.isRiding() || activeChar.isFlying())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(!getLeader().isOnline())
		{
			return;
		}

		if(member == null)
		{
			return;
		}

		if(!member.isOnline())
		{
			return;
		}

		L2PcInstance exLeader = getLeader().getPlayerInstance();

		SiegeManager.getInstance().removeSiegeSkills(exLeader);
		exLeader.setClan(this);
		exLeader.setClanPrivileges(L2Clan.CP_NOTHING);
		exLeader.broadcastUserInfo();

		setLeader(member);
		updateClanInDB();

		exLeader.getClan().getClanMember(exLeader.getObjectId());
		exLeader.setPledgeClass(L2ClanMember.calculatePledgeClass(exLeader));
		exLeader.broadcastUserInfo();

		L2PcInstance newLeader = member.getPlayerInstance();

		newLeader.setClan(this);
		newLeader.setPledgeClass(L2ClanMember.calculatePledgeClass(newLeader));
		newLeader.setClanPrivileges(L2Clan.CP_ALL);

		if(getLevel() >= 4)
		{
			SiegeManager.getInstance().addSiegeSkills(newLeader);
		}

		newLeader.broadcastUserInfo();

		broadcastClanStatus();

		broadcastToOnlineMembers(new SystemMessage(SystemMessageId.CLAN_LEADER_PRIVILEGES_HAVE_BEEN_TRANSFERRED_TO_S1).addString(newLeader.getName()));

		CrownManager.getInstance().checkCrowns(exLeader);
		CrownManager.getInstance().checkCrowns(newLeader);

		exLeader = null;
		newLeader = null;
	}

	public String getLeaderName()
	{
		return _leader != null ? _leader.getName() : "";
	}

	public String getName()
	{
		return _name;
	}

	public void setName(String name)
	{
		_name = name;
	}

	private void addClanMember(L2ClanMember member)
	{
		_members.put(member.getName(), member);
	}

	public void addClanMember(L2PcInstance player)
	{
		L2ClanMember member = new L2ClanMember(this, player.getName(), player.getLevel(), player.getClassId().getId(), player.getObjectId(), player.getPledgeType(), player.getPowerGrade(), player.getTitle());

		addClanMember(member);
		member.setPlayerInstance(player);
		player.setClan(this);
		player.setPledgeClass(L2ClanMember.calculatePledgeClass(player));
		player.sendPacket(new PledgeShowMemberListUpdate(player));
		player.sendPacket(new UserInfo(player));
		addSkillEffects(player);
		player.sendPacket(new PledgeSkillList(this));
		player.rewardSkills();

		member = null;
	}

	public void updateClanMember(L2PcInstance player)
	{
		L2ClanMember member = new L2ClanMember(player);
		addClanMember(member);

		member = null;
	}

	public L2ClanMember getClanMember(String name)
	{
		return _members.get(name);
	}

	public L2ClanMember getClanMember(int objectID)
	{
		for(L2ClanMember temp : _members.values())
		{
			if(temp.getObjectId() == objectID)
			{
				return temp;
			}
		}

		return null;
	}

	public void removeClanMember(String name, long clanJoinExpiryTime)
	{
		L2ClanMember exMember = _members.remove(name);

		if(exMember == null)
		{
			_log.warn("Member " + name + " not found in clan while trying to remove");
			return;
		}

		int leadssubpledge = getLeaderSubPledge(name);

		if(leadssubpledge != 0)
		{
			getSubPledge(leadssubpledge).setLeaderName("");
			updateSubPledgeInDB(leadssubpledge);
		}

		if(exMember.getApprentice() != 0)
		{
			L2ClanMember apprentice = getClanMember(exMember.getApprentice());

			if(apprentice != null)
			{
				if(apprentice.getPlayerInstance() != null)
				{
					apprentice.getPlayerInstance().setSponsor(0);
				}
				else
				{
					apprentice.initApprenticeAndSponsor(0, 0);
				}

				apprentice.saveApprenticeAndSponsor(0, 0);
			}

			apprentice = null;
		}

		if(exMember.getSponsor() != 0)
		{
			L2ClanMember sponsor = getClanMember(exMember.getSponsor());

			if(sponsor != null)
			{
				if(sponsor.getPlayerInstance() != null)
				{
					sponsor.getPlayerInstance().setApprentice(0);
				}
				else
				{
					sponsor.initApprenticeAndSponsor(0, 0);
				}

				sponsor.saveApprenticeAndSponsor(0, 0);
			}

			sponsor = null;
		}

		exMember.saveApprenticeAndSponsor(0, 0);

		if(Config.REMOVE_CASTLE_CIRCLETS)
		{
			CastleManager.getInstance().removeCirclet(exMember, getHasCastle());
		}

		if(exMember.isOnline())
		{
			L2PcInstance player = exMember.getPlayerInstance();

			player.setTitle("");
			player.setApprentice(0);
			player.setSponsor(0);

			if(player.isClanLeader())
			{
				SiegeManager.getInstance().removeSiegeSkills(player);
				player.setClanCreateExpiryTime(System.currentTimeMillis() + Config.ALT_CLAN_CREATE_DAYS * 86400000L);
			}

			for(L2Skill skill : player.getClan().getAllSkills())
			{
				player.removeSkill(skill, false);
			}

			player.setClan(null);

			if(exMember.getPledgeType() != -1)
			{
				player.setClanJoinExpiryTime(clanJoinExpiryTime);
				player.setPledgeClass(L2ClanMember.calculatePledgeClass(player));
			}

			player.broadcastUserInfo();
			player.sendPacket(new PledgeShowMemberListDeleteAll());

			player = null;
		}
		else
		{
			removeMemberInDatabase(exMember, clanJoinExpiryTime, getLeaderName().equalsIgnoreCase(name) ? System.currentTimeMillis() + Config.ALT_CLAN_CREATE_DAYS * 86400000L : 0);
		}

		exMember = null;
	}

	public L2ClanMember[] getMembers()
	{
		return _members.values().toArray(new L2ClanMember[_members.size()]);
	}

	public int getMembersCount()
	{
		return _members.size();
	}

	public int getSubPledgeMembersCount(int subpl)
	{
		int result = 0;

		for(L2ClanMember temp : _members.values())
		{
			if(temp.getPledgeType() == subpl)
			{
				result++;
			}
		}

		return result;
	}

	public int getMaxNrOfMembers(int pledgetype)
	{
		int limit = 0;

		switch(pledgetype)
		{
			case 0:
				switch(getLevel())
				{
					case 4:
						limit = 40;
						break;
					case 3:
						limit = 30;
						break;
					case 2:
						limit = 20;
						break;
					case 1:
						limit = 15;
						break;
					case 0:
						limit = 10;
						break;
					default:
						limit = 40;
						break;
				}
				break;
			case -1:
			case 100:
			case 200:
				limit = 20;
				break;
			case 1001:
			case 1002:
			case 2001:
			case 2002:
				limit = 10;
				break;
			default:
				break;
		}

		return limit;
	}

	public L2PcInstance[] getOnlineMembers(String exclude)
	{
		List<L2PcInstance> result = new FastList<L2PcInstance>();

		for(L2ClanMember temp : _members.values())
		{
			try
			{
				if(temp.isOnline() && !temp.getName().equals(exclude))
				{
					result.add(temp.getPlayerInstance());
				}
			}
			catch(NullPointerException e)
			{
			}
		}

		return result.toArray(new L2PcInstance[result.size()]);

	}

	public int getAllyId()
	{
		return _allyId;
	}

	public String getAllyName()
	{
		return _allyName;
	}

	public void setAllyCrestId(int allyCrestId)
	{
		_allyCrestId = allyCrestId;
	}

	public int getAllyCrestId()
	{
		return _allyCrestId;
	}

	public int getLevel()
	{
		return _level;
	}

	public int getHasCastle()
	{
		return _hasCastle;
	}

	public int getHasHideout()
	{
		return _hasHideout;
	}

	public void setCrestId(int crestId)
	{
		_crestId = crestId;
	}

	public int getCrestId()
	{
		return _crestId;
	}

	public void setCrestLargeId(int crestLargeId)
	{
		_crestLargeId = crestLargeId;
	}

	public int getCrestLargeId()
	{
		return _crestLargeId;
	}

	public void setAllyId(int allyId)
	{
		_allyId = allyId;
	}

	public void setAllyName(String allyName)
	{
		_allyName = allyName;
	}

	public void setHasCastle(int hasCastle)
	{
		_hasCastle = hasCastle;
	}

	public void setHasHideout(int hasHideout)
	{
		_hasHideout = hasHideout;
	}

	public void setLevel(int level)
	{
		_level = level;

		if(_forum == null)
		{
			if(_level >= 2)
			{
				_forum = ForumsBBSManager.getInstance().getForumByName("ClanRoot").getChildByName(_name);

				if(_forum == null)
				{
					_forum = ForumsBBSManager.getInstance().createNewForum(_name, ForumsBBSManager.getInstance().getForumByName("ClanRoot"), Forum.CLAN, Forum.CLANMEMBERONLY, getClanId());
				}
			}
		}
	}

	public boolean isMember(String name)
	{
		return name == null ? false : _members.containsKey(name);
	}

	public void updateClanInDB()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET leader_id = ?, ally_id = ?, ally_name = ?, reputation_score = ?, ally_penalty_expiry_time = ?, ally_penalty_type = ?, char_penalty_expiry_time = ?, dissolving_expiry_time = ? WHERE clan_id = ?");
			statement.setInt(1, getLeaderId());
			statement.setInt(2, getAllyId());
			statement.setString(3, getAllyName());
			statement.setInt(4, getReputationScore());
			statement.setLong(5, getAllyPenaltyExpiryTime());
			statement.setInt(6, getAllyPenaltyType());
			statement.setLong(7, getCharPenaltyExpiryTime());
			statement.setLong(8, getDissolvingExpiryTime());
			statement.setInt(9, getClanId());
			statement.execute();
		}
		catch (Exception e)
		{
			_log.error("Error saving clan.", e);
		}
		finally
		{
			ResourceUtil.closeStatement(statement);
			ResourceUtil.closeConnection(con);
		}
	}

	public void store()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO clan_data (clan_id, clan_name, clan_level, hasCastle, ally_id, ally_name, leader_id, crest_id, crest_large_id, ally_crest_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			statement.setInt(1, getClanId());
			statement.setString(2, getName());
			statement.setInt(3, getLevel());
			statement.setInt(4, getHasCastle());
			statement.setInt(5, getAllyId());
			statement.setString(6, getAllyName());
			statement.setInt(7, getLeaderId());
			statement.setInt(8, getCrestId());
			statement.setInt(9, getCrestLargeId());
			statement.setInt(10, getAllyCrestId());
			statement.execute();
		}
		catch(Exception e)
		{
			_log.error("error while saving new clan to db", e);
		}
		finally
		{
			ResourceUtil.closeStatement(statement);
			ResourceUtil.closeConnection(con);
		}
	}

	private void removeMemberInDatabase(L2ClanMember member, long clanJoinExpiryTime, long clanCreateExpiryTime)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET clanid = 0, title = ?, clan_join_expiry_time = ?, clan_create_expiry_time = ?, clan_privs = 0, wantspeace = 0, subpledge = 0, lvl_joined_academy = 0, apprentice = 0, sponsor = 0 WHERE obj_Id = ?");
			statement.setString(1, "");
			statement.setLong(2, clanJoinExpiryTime);
			statement.setLong(3, clanCreateExpiryTime);
			statement.setInt(4, member.getObjectId());
			statement.execute();
			statement.close();

			statement = con.prepareStatement("UPDATE characters SET apprentice = 0 WHERE apprentice = ?");
			statement.setInt(1, member.getObjectId());
			statement.execute();
			statement.close();

			statement = con.prepareStatement("UPDATE characters SET sponsor = 0 WHERE sponsor = ?");
			statement.setInt(1, member.getObjectId());
			statement.execute();
			statement.close();
			statement = null;
		}
		catch(Exception e)
		{
			_log.error("error while removing clan member in db", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	private void restore()
	{
		Connection con = null;
		try
		{
			L2ClanMember member;

			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT clan_name, clan_level, hasCastle, ally_id, ally_name, leader_id, crest_id, crest_large_id, ally_crest_id, reputation_score, auction_bid_at, ally_penalty_expiry_time, ally_penalty_type, char_penalty_expiry_time, dissolving_expiry_time FROM clan_data WHERE clan_id = ?");
			statement.setInt(1, getClanId());
			ResultSet clanData = statement.executeQuery();

			if(clanData.next())
			{
				setName(clanData.getString("clan_name"));
				setLevel(clanData.getInt("clan_level"));
				setHasCastle(clanData.getInt("hasCastle"));
				setAllyId(clanData.getInt("ally_id"));
				setAllyName(clanData.getString("ally_name"));
				setAllyPenaltyExpiryTime(clanData.getLong("ally_penalty_expiry_time"), clanData.getInt("ally_penalty_type"));

				if(getAllyPenaltyExpiryTime() < System.currentTimeMillis())
				{
					setAllyPenaltyExpiryTime(0, 0);
				}

				setCharPenaltyExpiryTime(clanData.getLong("char_penalty_expiry_time"));

				if(getCharPenaltyExpiryTime() + Config.ALT_CLAN_JOIN_DAYS * 86400000L < System.currentTimeMillis())
				{
					setCharPenaltyExpiryTime(0);
				}

				setDissolvingExpiryTime(clanData.getLong("dissolving_expiry_time"));

				setCrestId(clanData.getInt("crest_id"));

				if(getCrestId() != 0)
				{
					setHasCrest(true);
				}

				setCrestLargeId(clanData.getInt("crest_large_id"));

				if(getCrestLargeId() != 0)
				{
					setHasCrestLarge(true);
				}

				setAllyCrestId(clanData.getInt("ally_crest_id"));
				setReputationScore(clanData.getInt("reputation_score"), false);
				setAuctionBiddedAt(clanData.getInt("auction_bid_at"), false);

				int leaderId = clanData.getInt("leader_id");

				PreparedStatement statement2 = con.prepareStatement("SELECT char_name, level, classid, obj_Id, title, power_grade, subpledge, apprentice, sponsor FROM characters WHERE clanid = ?");
				statement2.setInt(1, getClanId());
				ResultSet clanMembers = statement2.executeQuery();

				while(clanMembers.next())
				{
					member = new L2ClanMember(this, clanMembers.getString("char_name"), clanMembers.getInt("level"), clanMembers.getInt("classid"), clanMembers.getInt("obj_id"), clanMembers.getInt("subpledge"), clanMembers.getInt("power_grade"), clanMembers.getString("title"));

					if(member.getObjectId() == leaderId)
					{
						setLeader(member);
					}
					else
					{
						addClanMember(member);
					}
					member.initApprenticeAndSponsor(clanMembers.getInt("apprentice"), clanMembers.getInt("sponsor"));
				}
				clanMembers.close();
				statement2.close();
			}

			clanData.close();
			statement.close();

			restoreSubPledges();
			restoreRankPrivs();
			restoreSkills();
			restoreNotice();

		}
		catch(Exception e)
		{
			_log.error("error while restoring clan", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	private void restoreSkills()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT skill_id, skill_level FROM clan_skills WHERE clan_id = ?");
			statement.setInt(1, getClanId());

			ResultSet rset = statement.executeQuery();

			while(rset.next())
			{
				int id = rset.getInt("skill_id");
				int level = rset.getInt("skill_level");

				L2Skill skill = SkillTable.getInstance().getInfo(id, level);

				_skills.put(skill.getId(), skill);
			}
			rset.close();
		}
		catch(Exception e)
		{
			_log.error("Could not restore clan skills", e);
		}
		finally
		{
			ResourceUtil.closeStatement(statement);
			ResourceUtil.closeConnection(con);
		}
	}

	private void restoreNotice()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT enabled, notice FROM clan_notices WHERE clan_id = ?");
			statement.setInt(1, getClanId());
			ResultSet noticeData = statement.executeQuery();

			while(noticeData.next())
			{
				_noticeEnabled = noticeData.getBoolean("enabled");
				_notice = noticeData.getString("notice");
			}

			noticeData.close();
		}
		catch(Exception e)
		{
			_log.error("Error restoring clan notice", e);
		}
		finally
		{
			ResourceUtil.closeStatement(statement);
			ResourceUtil.closeConnection(con);
		}
	}

	private void storeNotice(String notice, boolean enabled)
	{
		if(notice == null)
		{
			notice = "";
		}

		if(notice.length() > MAX_NOTICE_LENGTH)
		{
			notice = notice.substring(0, MAX_NOTICE_LENGTH - 1);
		}

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO clan_notices (clan_id, notice, enabled) values (?, ?, ?) ON DUPLICATE KEY UPDATE notice = ?, enabled = ?");
			statement.setInt(1, getClanId());
			statement.setString(2, notice);
			if(enabled)
			{
				statement.setString(3, "true");
			}
			else
			{
				statement.setString(3, "false");
			}

			statement.setString(4, notice);
			if(enabled)
			{
				statement.setString(5, "true");
			}
			else
			{
				statement.setString(5, "false");
			}

			statement.execute();
			statement.close();
		}
		catch(Exception e)
		{
			_log.error("Error could not store clan notice: " + e.getMessage(), e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}

		_notice = notice;
		_noticeEnabled = enabled;
	}

	public void setNoticeEnabled(boolean enabled)
	{
		storeNotice(_notice, enabled);
	}

	public void setNotice(String notice)
	{
		storeNotice(notice, _noticeEnabled);
	}

	public boolean isNoticeEnabled()
	{
		return _noticeEnabled;
	}

	public String getNotice()
	{
		if(_notice == null)
		{
			return "";
		}

		return _notice;
	}

	public final L2Skill[] getAllSkills()
	{
		if(_skills == null)
		{
			return new L2Skill[0];
		}

		return _skills.values().toArray(new L2Skill[_skills.values().size()]);
	}

	public L2Skill addSkill(L2Skill newSkill)
	{
		L2Skill oldSkill = null;

		if(newSkill != null)
		{
			oldSkill = _skills.put(newSkill.getId(), newSkill);
		}

		return oldSkill;
	}

	public L2Skill addNewSkill(L2Skill newSkill)
	{
		L2Skill oldSkill = null;
		Connection con = null;

		if(newSkill != null)
		{
			oldSkill = _skills.put(newSkill.getId(), newSkill);

			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement;

				if(oldSkill != null)
				{
					statement = con.prepareStatement("UPDATE clan_skills SET skill_level = ? WHERE skill_id = ? AND clan_id = ?");
					statement.setInt(1, newSkill.getLevel());
					statement.setInt(2, oldSkill.getId());
					statement.setInt(3, getClanId());
					statement.execute();
					statement.close();
				}
				else
				{
					statement = con.prepareStatement("INSERT INTO clan_skills (clan_id, skill_id, skill_level, skill_name) VALUES (?, ?, ?, ?)");
					statement.setInt(1, getClanId());
					statement.setInt(2, newSkill.getId());
					statement.setInt(3, newSkill.getLevel());
					statement.setString(4, newSkill.getName());
					statement.execute();
					statement.close();

				}
			}
			catch(Exception e)
			{
				_log.error("Error could not store char skills", e);
			}
			finally
			{
				ResourceUtil.closeConnection(con);
			}

			for(L2ClanMember temp : _members.values())
			{
				try
				{
					if(temp.isOnline())
					{
						if(newSkill.getMinPledgeClass() <= temp.getPlayerInstance().getPledgeClass())
						{
							temp.getPlayerInstance().addSkill(newSkill, false);
							temp.getPlayerInstance().sendPacket(new PledgeSkillListAdd(newSkill.getId(), newSkill.getLevel()));
						}
					}
				}
				catch(NullPointerException e)
				{
				}
			}
		}

		return oldSkill;
	}

	public void addSkillEffects()
	{
		for(L2Skill skill : _skills.values())
		{
			for(L2ClanMember temp : _members.values())
			{
				try
				{
					if(temp.isOnline())
					{
						if(skill.getMinPledgeClass() <= temp.getPlayerInstance().getPledgeClass())
						{
							temp.getPlayerInstance().addSkill(skill, false);
						}
					}
				}
				catch(NullPointerException e)
				{
				}
			}
		}
	}

	public void addSkillEffects(L2PcInstance cm)
	{
		if(cm == null)
		{
			return;
		}

		for(L2Skill skill : _skills.values())
		{
			if(skill.getMinPledgeClass() <= cm.getPledgeClass())
			{
				cm.addSkill(skill, false);
			}
		}
	}

	public void broadcastToOnlineAllyMembers(L2GameServerPacket packet)
	{
		if(getAllyId() == 0)
		{
			return;
		}

		for(L2Clan clan : ClanTable.getInstance().getClans())
		{
			if(clan.getAllyId() == getAllyId())
			{
				clan.broadcastToOnlineMembers(packet);
			}
		}
	}

	public void broadcastToOnlineMembers(L2GameServerPacket packet)
	{
		for(L2ClanMember member : _members.values())
		{
			try
			{
				if(member.isOnline())
				{
					member.getPlayerInstance().sendPacket(packet);
				}
			}
			catch(NullPointerException e)
			{
			}
		}
	}

	public void broadcastToOtherOnlineMembers(L2GameServerPacket packet, L2PcInstance player)
	{
		for(L2ClanMember member : _members.values())
		{
			try
			{
				if(member.isOnline() && member.getPlayerInstance() != player)
				{
					member.getPlayerInstance().sendPacket(packet);
				}
			}
			catch(NullPointerException e)
			{
			}
		}
	}

	@Override
	public String toString()
	{
		return getName();
	}

	public boolean hasCrest()
	{
		return _hasCrest;
	}

	public boolean hasCrestLarge()
	{
		return _hasCrestLarge;
	}

	public void setHasCrest(boolean flag)
	{
		_hasCrest = flag;
	}

	public void setHasCrestLarge(boolean flag)
	{
		_hasCrestLarge = flag;
	}

	public ItemContainer getWarehouse()
	{
		return _warehouse;
	}

	public boolean isAtWarWith(Integer id)
	{
		if(_atWarWith != null && _atWarWith.size() > 0)
		{
			if(_atWarWith.contains(id))
			{
				return true;
			}
		}

		return false;
	}

	public boolean isAtWarAttacker(Integer id)
	{
		if(_atWarAttackers != null && _atWarAttackers.size() > 0)
		{
			if(_atWarAttackers.contains(id))
			{
				return true;
			}
		}

		return false;
	}

	public void setEnemyClan(L2Clan clan)
	{
		Integer id = clan.getClanId();
		_atWarWith.add(id);

		id = null;
	}

	public void setEnemyClan(Integer clan)
	{
		_atWarWith.add(clan);
	}

	public void setAttackerClan(L2Clan clan)
	{
		Integer id = clan.getClanId();
		_atWarAttackers.add(id);

		id = null;
	}

	public void setAttackerClan(Integer clan)
	{
		_atWarAttackers.add(clan);
	}

	public void deleteEnemyClan(L2Clan clan)
	{
		Integer id = clan.getClanId();
		_atWarWith.remove(id);

		id = null;
	}

	public void deleteAttackerClan(L2Clan clan)
	{
		Integer id = clan.getClanId();
		_atWarAttackers.remove(id);

		id = null;
	}

	public int getHiredGuards()
	{
		return _hiredGuards;
	}

	public void incrementHiredGuards()
	{
		_hiredGuards++;
	}

	public int isAtWar()
	{
		if(_atWarWith != null && _atWarWith.size() > 0)
		{
			return 1;
		}

		return 0;
	}

	public List<Integer> getWarList()
	{
		return _atWarWith;
	}

	public List<Integer> getAttackerList()
	{
		return _atWarAttackers;
	}

	public void broadcastClanStatus()
	{
		for(L2PcInstance member : getOnlineMembers(""))
		{
			member.sendPacket(new PledgeShowMemberListDeleteAll());
			member.sendPacket(new PledgeShowMemberListAll(this, member));
		}
	}

	public void removeSkill(int id)
	{
		L2Skill deleteSkill = null;

		for(L2Skill sk : _skillList)
		{
			if(sk.getId() == id)
			{
				deleteSkill = sk;
				return;
			}
		}

		_skillList.remove(deleteSkill);

		deleteSkill = null;
	}

	public void removeSkill(L2Skill deleteSkill)
	{
		_skillList.remove(deleteSkill);
	}

	public List<L2Skill> getSkills()
	{
		return _skillList;
	}

	public class SubPledge
	{
		private int _id;
		private String _subPledgeName;
		private String _leaderName;

		public SubPledge(int id, String name, String leaderName)
		{
			_id = id;
			_subPledgeName = name;
			_leaderName = leaderName;
		}

		public int getId()
		{
			return _id;
		}

		public String getName()
		{
			return _subPledgeName;
		}

		 public void setName(String newName)  
		 {  
			 _subPledgeName = newName;  
		 }
		 
		public String getLeaderName()
		{
			return _leaderName;
		}

		public void setLeaderName(String leaderName)
		{
			_leaderName = leaderName;
		}
	}

	public class RankPrivs
	{
		private int _rankId;
		private int _party;
		private int _rankPrivs;

		public RankPrivs(int rank, int party, int privs)
		{
			_rankId = rank;
			_party = party;
			_rankPrivs = privs;
		}

		public int getRank()
		{
			return _rankId;
		}

		public int getParty()
		{
			return _party;
		}

		public int getPrivs()
		{
			return _rankPrivs;
		}

		public void setPrivs(int privs)
		{
			_rankPrivs = privs;
		}
	}

	private void restoreSubPledges()
	{
		Connection con = null;
		PreparedStatement statement = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT sub_pledge_id, name, leader_name FROM clan_subpledges WHERE clan_id = ?");
			statement.setInt(1, getClanId());
			ResultSet rset = statement.executeQuery();

			while(rset.next())
			{
				int id = rset.getInt("sub_pledge_id");

				String name = rset.getString("name");
				String leaderName = rset.getString("leader_name");
				SubPledge pledge = new SubPledge(id, name, leaderName);
				_subPledges.put(id, pledge);
			}
			rset.close();
		}
		catch(Exception e)
		{
			_log.error("Could not restore clan sub-units", e);
		}
		finally
		{
			ResourceUtil.closeStatement(statement);
			ResourceUtil.closeConnection(con);
		}
	}

	public final SubPledge getSubPledge(int pledgeType)
	{
		if(_subPledges == null)
		{
			return null;
		}

		return _subPledges.get(pledgeType);
	}

	public final SubPledge getSubPledge(String pledgeName)
	{
		if(_subPledges == null)
		{
			return null;
		}
		for(SubPledge sp : _subPledges.values())
		{
			if(sp.getName().equalsIgnoreCase(pledgeName))
			{
				return sp;
			}
		}

		return null;
	}

	public final SubPledge[] getAllSubPledges()
	{
		if(_subPledges == null)
		{
			return new SubPledge[0];
		}

		return _subPledges.values().toArray(new SubPledge[_subPledges.values().size()]);
	}

	public SubPledge createSubPledge(L2PcInstance player, int pledgeType, String leaderName, String subPledgeName)
	{
		SubPledge subPledge = null;
		pledgeType = getAvailablePledgeTypes(pledgeType);

		if(pledgeType == 0)
		{
			if(pledgeType == L2Clan.SUBUNIT_ACADEMY)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.CLAN_HAS_ALREADY_ESTABLISHED_A_CLAN_ACADEMY));
			}
			else
			{
				player.sendMessage("You can't create any more sub-units of this type");
			}
			return null;
		}

		if(_leader.getName().equals(leaderName))
		{
			player.sendMessage("Leader is not correct");
			return null;
		}

		if(pledgeType != -1 && (getReputationScore() < 5000 && pledgeType < L2Clan.SUBUNIT_KNIGHT1 || getReputationScore() < 10000 && pledgeType > L2Clan.SUBUNIT_ROYAL2))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_REPUTATION_SCORE_IS_TOO_LOW));

			return null;
		}
		else
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("INSERT INTO clan_subpledges (clan_id, sub_pledge_id, name, leader_name) VALUES (?, ?, ?, ?)");
				statement.setInt(1, getClanId());
				statement.setInt(2, pledgeType);
				statement.setString(3, subPledgeName);

				if(pledgeType != -1)
				{
					statement.setString(4, leaderName);
				}
				else
				{
					statement.setString(4, "");
				}

				statement.execute();
				statement.close();
				statement = null;

				subPledge = new SubPledge(pledgeType, subPledgeName, leaderName);
				_subPledges.put(pledgeType, subPledge);

				if(pledgeType != -1)
				{
					setReputationScore(getReputationScore() - 2500, true);
				}
			}
			catch(Exception e)
			{
				_log.error("error while saving new sub_clan to db", e);
			}
			finally
			{
				ResourceUtil.closeConnection(con);
			}
		}
		broadcastToOnlineMembers(new PledgeShowInfoUpdate(_leader.getClan()));
		broadcastToOnlineMembers(new PledgeReceiveSubPledgeCreated(subPledge));

		return subPledge;
	}

	public int getAvailablePledgeTypes(int pledgeType)
	{
		if(_subPledges.get(pledgeType) != null)
		{
			switch(pledgeType)
			{
				case SUBUNIT_ACADEMY:
					return 0;
				case SUBUNIT_ROYAL1:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_ROYAL2);
					break;
				case SUBUNIT_ROYAL2:
					return 0;
				case SUBUNIT_KNIGHT1:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT2);
					break;
				case SUBUNIT_KNIGHT2:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT3);
					break;
				case SUBUNIT_KNIGHT3:
					pledgeType = getAvailablePledgeTypes(SUBUNIT_KNIGHT4);
					break;
				case SUBUNIT_KNIGHT4:
					return 0;
			}
		}
		return pledgeType;
	}

	public void updateSubPledgeInDB(int pledgeType)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_subpledges SET leader_name = ? WHERE clan_id = ? AND sub_pledge_id = ?");
			statement.setString(1, getSubPledge(pledgeType).getLeaderName());
			statement.setInt(2, getClanId());
			statement.setInt(3, pledgeType);
			statement.execute();
			statement = con.prepareStatement("UPDATE clan_subpledges SET name = ? WHERE clan_id = ? AND sub_pledge_id = ?");  
			statement.setString(1, getSubPledge(pledgeType).getName());  
			statement.setInt(2, getClanId());  
			statement.setInt(3, pledgeType);  
			statement.execute();
		}
		catch(Exception e)
		{
			_log.error("error while saving new clan leader to db", e);
		}
		finally
		{
			ResourceUtil.closeStatement(statement);
			ResourceUtil.closeConnection(con);
		}
	}

	private void restoreRankPrivs()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT privs, rank, party FROM clan_privs WHERE clan_id = ?");
			statement.setInt(1, getClanId());
			ResultSet rset = statement.executeQuery();

			while(rset.next())
			{
				int rank = rset.getInt("rank");
				int privileges = rset.getInt("privs");
				if(rank == -1)
				{
					continue;
				}
				_privs.get(rank).setPrivs(privileges);
			}
			rset.close();
		}
		catch(Exception e)
		{
			_log.error("Could not restore clan privs by rank", e);
		}
		finally
		{
			ResourceUtil.closeStatement(statement);
			ResourceUtil.closeConnection(con);
		}
	}

	public void initializePrivs()
	{
		RankPrivs privs;

		for(int i = 1; i < 10; i++)
		{
			privs = new RankPrivs(i, 0, CP_NOTHING);
			_privs.put(i, privs);
		}

		privs = null;
	}

	public int getRankPrivs(int rank)
	{
		if(_privs.get(rank) != null)
		{
			return _privs.get(rank).getPrivs();
		}
		else
		{
			return CP_NOTHING;
		}
	}

	public void setRankPrivs(int rank, int privs)
	{
		if(_privs.get(rank) != null)
		{
			_privs.get(rank).setPrivs(privs);

			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("INSERT INTO clan_privs (clan_id, rank, party, privs) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE privs = ?");
				statement.setInt(1, getClanId());
				statement.setInt(2, rank);
				statement.setInt(3, 0);
				statement.setInt(4, privs);
				statement.setInt(5, privs);
				statement.execute();
			}
			catch(Exception e)
			{
				_log.error("Could not store clan privs for rank", e);
			}
			finally
			{
				ResourceUtil.closeStatement(statement);
				ResourceUtil.closeConnection(con);
			}
			
			for(L2ClanMember cm : getMembers())
			{
				if(cm.isOnline())
				{
					if(cm.getPowerGrade() == rank)
					{
						if(cm.getPlayerInstance() != null)
						{
							cm.getPlayerInstance().setClanPrivileges(privs);
							cm.getPlayerInstance().sendPacket(new UserInfo(cm.getPlayerInstance()));
						}
					}
				}
			}
			broadcastClanStatus();
		}
		else
		{
			_privs.put(rank, new RankPrivs(rank, 0, privs));

			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("INSERT INTO clan_privs (clan_id, rank, party, privs) VALUES (?, ?, ?, ?)");
				statement.setInt(1, getClanId());
				statement.setInt(2, rank);
				statement.setInt(3, 0);
				statement.setInt(4, privs);
				statement.execute();
			}
			catch(Exception e)
			{
				_log.error("Could not create new rank and store clan privs for rank", e);
			}
			finally
			{
				ResourceUtil.closeStatement(statement);
				ResourceUtil.closeConnection(con);
			}
		}
	}

	public final RankPrivs[] getAllRankPrivs()
	{
		if(_privs == null)
		{
			return new RankPrivs[0];
		}

		return _privs.values().toArray(new RankPrivs[_privs.values().size()]);
	}

	public int getLeaderSubPledge(String name)
	{
		int id = 0;

		for(SubPledge sp : _subPledges.values())
		{
			if(sp.getLeaderName() == null)
			{
				continue;
			}

			if(sp.getLeaderName().equals(name))
			{
				id = sp.getId();
			}
		}

		return id;
	}
	
	public void setReputationScore(int value, boolean save)
	{
		if (_reputationScore >= 0 && value < 0)
		{
			broadcastToOnlineMembers(SystemMessage.getSystemMessage(SystemMessageId.REPUTATION_POINTS_0_OR_LOWER_CLAN_SKILLS_DEACTIVATED));
			L2Skill[] skills = getAllSkills();
			for (L2ClanMember member : _members.values())
			{
				if (member.isOnline() && member.getPlayerInstance() != null)
				{
					for (L2Skill sk : skills)
						member.getPlayerInstance().removeSkill(sk, false);
				}
			}
		}
		else if (_reputationScore < 0 && value >= 0)
		{
			broadcastToOnlineMembers(SystemMessage.getSystemMessage(SystemMessageId.CLAN_SKILLS_WILL_BE_ACTIVATED_SINCE_REPUTATION_IS_0_OR_HIGHER));
			L2Skill[] skills = getAllSkills();
			for (L2ClanMember member : _members.values())
			{
				if (member.isOnline() && member.getPlayerInstance() != null)
				{
					for (L2Skill sk : skills)
					{
						if (sk.getMinPledgeClass() <= member.getPlayerInstance().getPledgeClass())
							member.getPlayerInstance().addSkill(sk, false);
					}
				}
			}
		}
		_reputationScore = value;
		
		if (_reputationScore > 100000000)
			_reputationScore = 100000000;
		
		if (_reputationScore < -100000000)
			_reputationScore = -100000000;
		
		broadcastToOnlineMembers(new PledgeShowInfoUpdate(this));
		
		if (save)
			updateClanInDB();
	}

	public int getReputationScore()
	{
		return _reputationScore;
	}

	public void setRank(int rank)
	{
		_rank = rank;
	}

	public int getRank()
	{
		return _rank;
	}

	public int getAuctionBiddedAt()
	{
		return _auctionBiddedAt;
	}

	public void setAuctionBiddedAt(int id, boolean storeInDb)
	{
		_auctionBiddedAt = id;

		if(storeInDb)
		{
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("UPDATE clan_data SET auction_bid_at = ? WHERE clan_id = ?");
				statement.setInt(1, id);
				statement.setInt(2, getClanId());
				statement.execute();
			}
			catch(Exception e)
			{
				_log.error("Could not store auction for clan", e);
			}
			finally
			{
				ResourceUtil.closeStatement(statement);
				ResourceUtil.closeConnection(con);
			}
		}
	}

	public boolean checkClanJoinCondition(L2PcInstance activeChar, L2PcInstance target, int pledgeType)
	{
		if(activeChar == null)
		{
			return false;
		}

		if((activeChar.getClanPrivileges() & L2Clan.CP_CL_JOIN_CLAN) != L2Clan.CP_CL_JOIN_CLAN)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return false;
		}

		if(target == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET));
			return false;
		}

		if(activeChar.getObjectId() == target.getObjectId())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_INVITE_YOURSELF));
			return false;
		}

		if(getCharPenaltyExpiryTime() > System.currentTimeMillis())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_MUST_WAIT_BEFORE_ACCEPTING_A_NEW_MEMBER).addString(target.getName()));
			return false;
		}

		if(target.getClanId() != 0)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_WORKING_WITH_ANOTHER_CLAN).addString(target.getName()));
			return false;
		}

		if(target.getClanJoinExpiryTime() > System.currentTimeMillis())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_MUST_WAIT_BEFORE_JOINING_ANOTHER_CLAN).addString(target.getName()));
			return false;
		}

		if((target.getLevel() > 40 || target.getClassId().level() >= 2) && pledgeType == -1)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_DOESNOT_MEET_REQUIREMENTS_TO_JOIN_ACADEMY).addString(target.getName()));
			activeChar.sendPacket(new SystemMessage(SystemMessageId.ACADEMY_REQUIREMENTS));
			return false;
		}

		if(getSubPledgeMembersCount(pledgeType) >= getMaxNrOfMembers(pledgeType))
		{
			if(pledgeType == 0)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_CLAN_IS_FULL).addString(getName()));
			}
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.SUBCLAN_IS_FULL));
			}

			return false;
		}

		int leadssubpledge = getLeaderSubPledge(activeChar.getName());

		if(pledgeType != -1 && !activeChar.isClanLeader())
		{
			if(activeChar.getPledgeType() != pledgeType)
			{
				if(leadssubpledge == 0)
				{
					activeChar.sendMessage("Invite to another subunit is not allowed.");
					return false;
				}
				else if(leadssubpledge != pledgeType)
				{
					activeChar.sendMessage("Invite to another subunit is not allowed.");
					return false;
				}
			}
			else if(leadssubpledge != 0)
			{
				activeChar.sendMessage("Invite to another subunit is not allowed.");
				return false;
			}
		}

		return true;
	}

	public boolean checkAllyJoinCondition(L2PcInstance activeChar, L2PcInstance target)
	{
		if(activeChar == null)
		{
			return false;
		}

		if(activeChar.getAllyId() == 0 || !activeChar.isClanLeader() || activeChar.getClanId() != activeChar.getAllyId())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.FEATURE_ONLY_FOR_ALLIANCE_LEADER));
			return false;
		}

		L2Clan leaderClan = activeChar.getClan();

		if(leaderClan.getAllyPenaltyExpiryTime() > System.currentTimeMillis())
		{
			if(leaderClan.getAllyPenaltyType() == PENALTY_TYPE_DISMISS_CLAN)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_INVITE_CLAN_WITHIN_1_DAY));
				return false;
			}
		}

		if(target == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_INVITED_THE_WRONG_TARGET));
			return false;
		}

		if(activeChar.getObjectId() == target.getObjectId())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_INVITE_YOURSELF));
			return false;
		}

		if(target.getClan() == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_MUST_BE_IN_CLAN));
			return false;
		}

		if(!target.isClanLeader())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_IS_NOT_A_CLAN_LEADER).addString(target.getName()));
			return false;
		}

		L2Clan targetClan = target.getClan();

		if(target.getAllyId() != 0)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_CLAN_ALREADY_MEMBER_OF_S2_ALLIANCE).addString(targetClan.getName()).addString(targetClan.getAllyName()));
			return false;
		}

		if(targetClan.getAllyPenaltyExpiryTime() > System.currentTimeMillis())
		{
			if(targetClan.getAllyPenaltyType() == PENALTY_TYPE_CLAN_LEAVED)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_CANT_ENTER_ALLIANCE_WITHIN_1_DAY).addString(target.getClan().getName()).addString(target.getClan().getAllyName()));
				return false;
			}

			if(targetClan.getAllyPenaltyType() == PENALTY_TYPE_CLAN_DISMISSED)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_ENTER_ALLIANCE_WITHIN_1_DAY));
				return false;
			}
		}

		if(activeChar.isInsideZone(L2Character.ZONE_SIEGE) && target.isInsideZone(L2Character.ZONE_SIEGE))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.OPPOSING_CLAN_IS_PARTICIPATING_IN_SIEGE));
			return false;
		}

		if(leaderClan.isAtWarWith(targetClan.getClanId()))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.MAY_NOT_ALLY_CLAN_BATTLE));
			return false;
		}

		int numOfClansInAlly = 0;

		for(L2Clan clan : ClanTable.getInstance().getClans())
		{
			if(clan.getAllyId() == activeChar.getAllyId())
			{
				++numOfClansInAlly;
			}
		}

		if(numOfClansInAlly >= Config.ALT_MAX_NUM_OF_CLANS_IN_ALLY)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_THE_LIMIT));
			return false;
		}

		targetClan = null;
		leaderClan = null;

		return true;
	}

	public long getAllyPenaltyExpiryTime()
	{
		return _allyPenaltyExpiryTime;
	}

	public int getAllyPenaltyType()
	{
		return _allyPenaltyType;
	}

	public void setAllyPenaltyExpiryTime(long expiryTime, int penaltyType)
	{
		_allyPenaltyExpiryTime = expiryTime;
		_allyPenaltyType = penaltyType;
	}

	public long getCharPenaltyExpiryTime()
	{
		return _charPenaltyExpiryTime;
	}

	public void setCharPenaltyExpiryTime(long time)
	{
		_charPenaltyExpiryTime = time;
	}

	public long getDissolvingExpiryTime()
	{
		return _dissolvingExpiryTime;
	}

	public void setDissolvingExpiryTime(long time)
	{
		_dissolvingExpiryTime = time;
	}

	public void createAlly(L2PcInstance player, String allyName)
	{
		if(null == player)
		{
			return;
		}

		if(!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.ONLY_CLAN_LEADER_CREATE_ALLIANCE));
			return;
		}

		if(getAllyId() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.ALREADY_JOINED_ALLIANCE));
			return;
		}

		if(getLevel() < 5)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.TO_CREATE_AN_ALLY_YOU_CLAN_MUST_BE_LEVEL_5_OR_HIGHER));
			return;
		}

		if(getAllyPenaltyExpiryTime() > System.currentTimeMillis())
		{
			if(getAllyPenaltyType() == L2Clan.PENALTY_TYPE_DISSOLVE_ALLY)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.CANT_CREATE_ALLIANCE_10_DAYS_DISOLUTION));
				return;
			}
		}

		if(getDissolvingExpiryTime() > System.currentTimeMillis())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_MAY_NOT_CREATE_ALLY_WHILE_DISSOLVING));
			return;
		}

		if(!Util.isAlphaNumeric(allyName))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_ALLIANCE_NAME));
			return;
		}

		if(allyName.length() > 16 || allyName.length() < 2)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_ALLIANCE_NAME_LENGTH));
			return;
		}

		if(ClanTable.getInstance().isAllyExists(allyName))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.ALLIANCE_ALREADY_EXISTS));
			return;
		}

		setAllyId(getClanId());
		setAllyName(allyName.trim());
		setAllyPenaltyExpiryTime(0, 0);
		updateClanInDB();
		player.sendPacket(new UserInfo(player));
		player.sendMessage("Alliance " + allyName + " has been created.");
	}

	public void dissolveAlly(L2PcInstance player)
	{
		if(getAllyId() == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NO_CURRENT_ALLIANCES));
			return;
		}

		if(!player.isClanLeader() || getClanId() != getAllyId())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.FEATURE_ONLY_FOR_ALLIANCE_LEADER));
			return;
		}

		if(player.isInsideZone(L2Character.ZONE_SIEGE))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISSOLVE_ALLY_WHILE_IN_SIEGE));
			return;
		}

		broadcastToOnlineAllyMembers(new SystemMessage(SystemMessageId.ALLIANCE_DISOLVED));

		long currentTime = System.currentTimeMillis();

		for(L2Clan clan : ClanTable.getInstance().getClans())
		{
			if(clan.getAllyId() == getAllyId() && clan.getClanId() != getClanId())
			{
				clan.setAllyId(0);
				clan.setAllyName(null);
				clan.setAllyPenaltyExpiryTime(0, 0);
				clan.updateClanInDB();
			}
		}

		setAllyId(0);
		setAllyName(null);
		setAllyPenaltyExpiryTime(currentTime + Config.ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED * 86400000L, L2Clan.PENALTY_TYPE_DISSOLVE_ALLY);
		updateClanInDB();

		player.deathPenalty(false);
	}

	public void levelUpClan(L2PcInstance player)
	{
		if(!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return;
		}

		if(System.currentTimeMillis() < getDissolvingExpiryTime())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_RISE_LEVEL_WHILE_DISSOLUTION_IN_PROGRESS));
			return;
		}

		boolean increaseClanLevel = false;

		switch(getLevel())
		{
			case 0:
			{
				if(player.getSp() >= 20000 && player.getAdena() >= 650000)
				{
					if(player.reduceAdena("ClanLvl", 650000, player.getTarget(), true))
					{
						player.setSp(player.getSp() - 20000);
						player.sendPacket(new MagicSkillUser(player, 5103, 1, 1000, 0));
						player.sendPacket(new SystemMessage(SystemMessageId.SP_DECREASED_S1).addNumber(20000));
						increaseClanLevel = true;
					}
				}
				break;
			}
			case 1:
			{
				if(player.getSp() >= 100000 && player.getAdena() >= 2500000)
				{
					if(player.reduceAdena("ClanLvl", 2500000, player.getTarget(), true))
					{
						player.setSp(player.getSp() - 100000);
						player.sendPacket(new MagicSkillUser(player, 5103, 1, 1000, 0));
						player.sendPacket(new SystemMessage(SystemMessageId.SP_DECREASED_S1).addNumber(100000));
						increaseClanLevel = true;
					}
				}
				break;
			}
			case 2:
			{
				if(player.getSp() >= 350000 && player.getInventory().getItemByItemId(1419) != null)
				{
					if(player.destroyItemByItemId("ClanLvl", 1419, 1, player.getTarget(), false))
					{
						player.setSp(player.getSp() - 350000);
						player.sendPacket(new MagicSkillUser(player, 5103, 1, 1000, 0));
						player.sendPacket(new SystemMessage(SystemMessageId.SP_DECREASED_S1).addNumber(350000));
						player.sendPacket(new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(1419).addNumber(1));
						increaseClanLevel = true;
					}
				}
				break;
			}
			case 3:
			{
				if(player.getSp() >= 1000000 && player.getInventory().getItemByItemId(3874) != null)
				{
					if(player.destroyItemByItemId("ClanLvl", 3874, 1, player.getTarget(), false))
					{
						player.setSp(player.getSp() - 1000000);
						player.sendPacket(new MagicSkillUser(player, 5103, 1, 1000, 0));
						player.sendPacket(new SystemMessage(SystemMessageId.SP_DECREASED_S1).addNumber(1000000));
						player.sendPacket(new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(3874).addNumber(1));
						increaseClanLevel = true;
					}
				}
				break;
			}
			case 4:
			{
				if(player.getSp() >= 2500000 && player.getInventory().getItemByItemId(3870) != null)
				{
					if(player.destroyItemByItemId("ClanLvl", 3870, 1, player.getTarget(), false))
					{
						player.setSp(player.getSp() - 2500000);
						player.sendPacket(new MagicSkillUser(player, 5103, 1, 1000, 0));
						player.sendPacket(new SystemMessage(SystemMessageId.SP_DECREASED_S1).addNumber(2500000));
						player.sendPacket(new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(3870).addNumber(1));
						increaseClanLevel = true;
					}
				}
				break;
			}
			case 5:
				if(getReputationScore() >= 10000 && getMembersCount() >= 30)
				{
					setReputationScore(getReputationScore() - 10000, true);
					player.sendPacket(new MagicSkillUser(player, 5103, 1, 1000, 0));
					player.sendPacket(new SystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP).addNumber(10000));
					increaseClanLevel = true;
				}
				break;

			case 6:
				if(getReputationScore() >= 20000 && getMembersCount() >= 80)
				{
					setReputationScore(getReputationScore() - 20000, true);
					player.sendPacket(new MagicSkillUser(player, 5103, 1, 1000, 0));
					player.sendPacket(new SystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP).addNumber(20000));
					increaseClanLevel = true;
				}
				break;
			case 7:
				if(getReputationScore() >= 40000 && getMembersCount() >= 120)
				{
					setReputationScore(getReputationScore() - 40000, true);
					player.sendPacket(new MagicSkillUser(player, 5103, 1, 1000, 0));
					player.sendPacket(new SystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP).addNumber(40000));
					increaseClanLevel = true;
				}
				break;
			default:
				return;
		}

		if(!increaseClanLevel)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.FAILED_TO_INCREASE_CLAN_LEVEL));
			return;
		}

		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.SP, player.getSp());
		player.sendPacket(su);

		ItemList il = new ItemList(player, false);
		player.sendPacket(il);

		changeLevel(getLevel() + 1);
	}

	public void changeLevel(int level)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET clan_level = ? WHERE clan_id = ?");
			statement.setInt(1, level);
			statement.setInt(2, getClanId());
			statement.execute();
		}
		catch(Exception e)
		{
			_log.error("could not increase clan level", e);
		}
		finally
		{
			ResourceUtil.closeStatement(statement);
			ResourceUtil.closeConnection(con);
		}

		setLevel(level);

		if(getLeader().isOnline())
		{
			L2PcInstance leader = getLeader().getPlayerInstance();

			if(3 < level)
			{
				SiegeManager.getInstance().addSiegeSkills(leader);
			}
			else if(4 > level)
			{
				SiegeManager.getInstance().removeSiegeSkills(leader);
			}

			if(4 < level)
			{
				leader.sendPacket(new SystemMessage(SystemMessageId.CLAN_CAN_ACCUMULATE_CLAN_REPUTATION_POINTS));
			}

			leader = null;
		}

		broadcastToOnlineMembers(new SystemMessage(SystemMessageId.CLAN_LEVEL_INCREASED));
		broadcastToOnlineMembers(new PledgeShowInfoUpdate(this));
	}

	public void setAllyCrest(int crestId)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			setAllyCrestId(crestId);
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET ally_crest_id = ? WHERE clan_id = ?");
			statement.setInt(1, crestId);
			statement.setInt(2, getClanId());
			statement.executeUpdate();
		}
		catch(SQLException e)
		{
			_log.error("could not update the ally crest id", e);
		}
		finally
		{
			ResourceUtil.closeStatement(statement);
			ResourceUtil.closeConnection(con);
		}
	}

	public L2PcInstance[] getOnlineMembers(int exclude)
	{
		FastList<L2PcInstance> list = FastList.newInstance();
		for (L2ClanMember temp : _members.values())
		{
			if (temp != null && temp.isOnline() && !(temp.getObjectId() == exclude))
				list.add(temp.getPlayerInstance());
		}
		
		L2PcInstance[] result = list.toArray(new L2PcInstance[list.size()]);
		FastList.recycle(list);
		return result;
	}
}