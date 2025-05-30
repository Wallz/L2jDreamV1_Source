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
package com.src.gameserver.model.entity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.datatables.sql.CharNameTable;
import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.olympiad.Olympiad;
import com.src.gameserver.model.entity.siege.Castle;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import com.src.gameserver.network.serverpackets.SocialAction;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.network.serverpackets.UserInfo;
import com.src.gameserver.templates.StatsSet;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.templates.item.L2Item;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class Hero
{
	private static final Log _log = LogFactory.getLog(Hero.class);

	private static Hero _instance;
	private static final String GET_HEROES = "SELECT * FROM heroes WHERE played = 1";
	private static final String GET_ALL_HEROES = "SELECT * FROM heroes";
	private static final String UPDATE_ALL = "UPDATE heroes SET played = 0";
	private static final String INSERT_HERO = "INSERT INTO heroes (char_id, char_name, class_id, count, played, active) VALUES (?, ?, ?, ?,  ?, ?)";
	private static final String UPDATE_HERO = "UPDATE heroes SET count = ?, played = ?, active = ?" + " WHERE char_id = ?";
	private static final String GET_CLAN_ALLY = "SELECT characters.clanid AS clanid, coalesce(clan_data.ally_Id, 0) AS allyId FROM characters LEFT JOIN clan_data ON clan_data.clan_id = characters.clanid " + " WHERE characters.obj_Id = ?";
	private static final String DELETE_ITEMS = "DELETE FROM items WHERE item_id IN " + "(6842, 6611, 6612, 6613, 6614, 6615, 6616, 6617, 6618, 6619, 6620, 6621) " + "AND owner_id NOT IN (SELECT obj_id FROM characters WHERE accesslevel > 0)";

	private static final String GET_DIARIES = "SELECT * FROM  heroes_diary WHERE char_id=? ORDER BY time ASC";
	private static final String UPDATE_DIARIES = "INSERT INTO heroes_diary (char_id, time, action, param) values(?,?,?,?)";
	
	private static final int[] _heroItems =
	{
			6842, 6611, 6612, 6613, 6614, 6615, 6616, 6617, 6618, 6619, 6620, 6621
	};

	private static Map<Integer, StatsSet> _heroes;
	private static Map<Integer, StatsSet> _completeHeroes;
	private static Map<Integer, List<StatsSet>> _herodiary;
	private static List<StatsSet> _diary;
	
	public static final String COUNT = "count";
	public static final String PLAYED = "played";
	public static final String CLAN_NAME = "clan_name";
	public static final String CLAN_CREST = "clan_crest";
	public static final String ALLY_NAME = "ally_name";
	public static final String ALLY_CREST = "ally_crest";
	public static final String ACTIVE = "active";

	public static final int ACTION_RAID_KILLED = 1;
	public static final int ACTION_HERO_GAINED = 2;
	public static final int ACTION_CASTLE_TAKEN = 3;
	
	public static Hero getInstance()
	{
		if(_instance == null)
		{
			_instance = new Hero();
		}

		return _instance;
	}

	public Hero()
	{
		init();
	}

	private void init()
	{
		_heroes = new FastMap<Integer, StatsSet>();
		_completeHeroes = new FastMap<Integer, StatsSet>();

		_herodiary = new FastMap<>();
		
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(GET_HEROES);
			ResultSet rset = statement.executeQuery();

			PreparedStatement statement2;
			ResultSet rset2;

			while(rset.next())
			{
				StatsSet hero = new StatsSet();

				int charId = rset.getInt(Olympiad.CHAR_ID);

				hero.set(Olympiad.CHAR_NAME, rset.getString(Olympiad.CHAR_NAME));
				hero.set(Olympiad.CLASS_ID, rset.getInt(Olympiad.CLASS_ID));
				hero.set(COUNT, rset.getInt(COUNT));
				hero.set(PLAYED, rset.getInt(PLAYED));
				hero.set(ACTIVE, rset.getInt(ACTIVE));

				statement2 = con.prepareStatement(GET_CLAN_ALLY);
				statement2.setInt(1, charId);
				rset2 = statement2.executeQuery();

				if(rset2.next())
				{
					int clanId = rset2.getInt("clanid");
					int allyId = rset2.getInt("allyId");

					String clanName = "";
					String allyName = "";

					int clanCrest = 0;
					int allyCrest = 0;

					if(clanId > 0)
					{
						clanName = ClanTable.getInstance().getClan(clanId).getName();
						clanCrest = ClanTable.getInstance().getClan(clanId).getCrestId();

						if(allyId > 0)
						{
							allyName = ClanTable.getInstance().getClan(clanId).getAllyName();
							allyCrest = ClanTable.getInstance().getClan(clanId).getAllyCrestId();
						}
					}

					hero.set(CLAN_CREST, clanCrest);
					hero.set(CLAN_NAME, clanName);
					hero.set(ALLY_CREST, allyCrest);
					hero.set(ALLY_NAME, allyName);

					clanName = null;
					allyName = null;
				}

				rset2.close();
				statement2.close();
				statement2 = null;
				rset2 = null;

				_heroes.put(charId, hero);

				hero = null;
			}

			rset.close();
			ResourceUtil.closeStatement(statement);

			statement = con.prepareStatement(GET_ALL_HEROES);
			rset = statement.executeQuery();

			while(rset.next())
			{
				StatsSet hero = new StatsSet();

				int charId = rset.getInt(Olympiad.CHAR_ID);

				hero.set(Olympiad.CHAR_NAME, rset.getString(Olympiad.CHAR_NAME));
				hero.set(Olympiad.CLASS_ID, rset.getInt(Olympiad.CLASS_ID));
				hero.set(COUNT, rset.getInt(COUNT));
				hero.set(PLAYED, rset.getInt(PLAYED));
				hero.set(ACTIVE, rset.getInt(ACTIVE));

				statement2 = con.prepareStatement(GET_CLAN_ALLY);
				statement2.setInt(1, charId);
				rset2 = statement2.executeQuery();

				if(rset2.next())
				{
					int clanId = rset2.getInt("clanid");
					int allyId = rset2.getInt("allyId");

					String clanName = "";
					String allyName = "";

					int clanCrest = 0;
					int allyCrest = 0;

					if(clanId > 0)
					{
						clanName = ClanTable.getInstance().getClan(clanId).getName();
						clanCrest = ClanTable.getInstance().getClan(clanId).getCrestId();

						if(allyId > 0)
						{
							allyName = ClanTable.getInstance().getClan(clanId).getAllyName();
							allyCrest = ClanTable.getInstance().getClan(clanId).getAllyCrestId();
						}
					}

					hero.set(CLAN_CREST, clanCrest);
					hero.set(CLAN_NAME, clanName);
					hero.set(ALLY_CREST, allyCrest);
					hero.set(ALLY_NAME, allyName);

					clanName = null;
					allyName = null;
				}

				rset2.close();
				statement2.close();
				statement2 = null;
				rset2 = null;

				_completeHeroes.put(charId, hero);

				hero = null;
			}

			rset.close();
			ResourceUtil.closeStatement(statement);
		}
		catch(SQLException e)
		{
			_log.error("Hero System: Couldnt load Heroes", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}

		_log.info("Hero System: Loaded " + _heroes.size() + " Heroes.");
		_log.info("Hero System: Loaded " + _completeHeroes.size() + " all time Heroes.");
	}

	public Map<Integer, StatsSet> getHeroes()
	{
		return _heroes;
	}

	public synchronized void computeNewHeroes(List<StatsSet> newHeroes)
	{
		updateHeroes(true);
		List<int[]> heroItems = Arrays.asList(_heroItems);
		L2ItemInstance[] items;
		InventoryUpdate iu;

		if(_heroes.size() != 0)
		{
			for(StatsSet hero : _heroes.values())
			{
				String name = hero.getString(Olympiad.CHAR_NAME);

				L2PcInstance player = L2World.getInstance().getPlayer(name);
				name = null;

				if(player == null)
				{
					continue;
				}

				try
				{
					player.setIsHero(false);

					items = player.getInventory().unEquipItemInBodySlotAndRecord(L2Item.SLOT_LR_HAND);
					iu = new InventoryUpdate();

					for(L2ItemInstance item : items)
					{
						iu.addModifiedItem(item);
					}

					player.sendPacket(iu);

					items = player.getInventory().unEquipItemInBodySlotAndRecord(L2Item.SLOT_R_HAND);
					iu = new InventoryUpdate();

					for(L2ItemInstance item : items)
					{
						iu.addModifiedItem(item);
					}

					player.sendPacket(iu);
					iu = null;

					items = player.getInventory().unEquipItemInBodySlotAndRecord(L2Item.SLOT_HAIR);
					iu = new InventoryUpdate();

					for(L2ItemInstance item : items)
					{
						iu.addModifiedItem(item);
					}

					player.sendPacket(iu);
					iu = null;

					items = player.getInventory().unEquipItemInBodySlotAndRecord(L2Item.SLOT_FACE);
					iu = new InventoryUpdate();

					for(L2ItemInstance item : items)
					{
						iu.addModifiedItem(item);
					}

					player.sendPacket(iu);
					iu = null;

					items = player.getInventory().unEquipItemInBodySlotAndRecord(L2Item.SLOT_DHAIR);
					iu = new InventoryUpdate();

					for(L2ItemInstance item : items)
					{
						iu.addModifiedItem(item);
					}

					player.sendPacket(iu);
					iu = null;

					for(L2ItemInstance item : player.getInventory().getAvailableItems(false))
					{
						if(item == null)
						{
							continue;
						}

						if(!heroItems.contains(item.getItemId()))
						{
							continue;
						}

						player.destroyItem("Hero", item, null, true);
						iu = new InventoryUpdate();
						iu.addRemovedItem(item);
						player.sendPacket(iu);
						iu = null;
					}

					player.sendPacket(new UserInfo(player));
					player.broadcastUserInfo();
				}
				catch(NullPointerException e)
				{
				}
			}
		}

		if(newHeroes.size() == 0)
		{
			_heroes.clear();
			return;
		}

		Map<Integer, StatsSet> heroes = new FastMap<Integer, StatsSet>();

		for(StatsSet hero : newHeroes)
		{
			int charId = hero.getInteger(Olympiad.CHAR_ID);

			if(_completeHeroes != null && _completeHeroes.containsKey(charId))
			{
				StatsSet oldHero = _completeHeroes.get(charId);
				int count = oldHero.getInteger(COUNT);
				oldHero.set(COUNT, count + 1);
				oldHero.set(PLAYED, 1);
				oldHero.set(ACTIVE, 0);

				heroes.put(charId, oldHero);
				oldHero = null;
			}
			else
			{
				StatsSet newHero = new StatsSet();
				newHero.set(Olympiad.CHAR_NAME, hero.getString(Olympiad.CHAR_NAME));
				newHero.set(Olympiad.CLASS_ID, hero.getInteger(Olympiad.CLASS_ID));
				newHero.set(COUNT, 1);
				newHero.set(PLAYED, 1);
				newHero.set(ACTIVE, 0);

				heroes.put(charId, newHero);
				newHero = null;
			}
		}

		deleteItemsInDb();

		_heroes.clear();
		_heroes.putAll(heroes);
		heroes.clear();

		updateHeroes(false);

		heroItems = null;
		items = null;
		heroes = null;
	}

	public void loadDiary(int charId)
	{
		_diary = new FastList<>();
		
		int diaryentries = 0;
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(GET_DIARIES);
			statement.setInt(1, charId);
			ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				StatsSet _diaryentry = new StatsSet();
				
				long time = rset.getLong("time");
				int action = rset.getInt("action");
				int param = rset.getInt("param");
				
				String date = (new SimpleDateFormat("yyyy-MM-dd HH")).format(new Date(time));
				_diaryentry.set("date", date);
				
				if (action == ACTION_RAID_KILLED)
				{
					L2NpcTemplate template = NpcTable.getInstance().getTemplate(param);
					if (template != null)
						_diaryentry.set("action", template.getName() + " was defeated");
				}
				else if (action == ACTION_HERO_GAINED)
					_diaryentry.set("action", "Gained Hero status");
				else if (action == ACTION_CASTLE_TAKEN)
				{
					Castle castle = CastleManager.getInstance().getCastleById(param);
					if (castle != null)
						_diaryentry.set("action", castle.getName() + " Castle was successfuly taken");
				}
				_diary.add(_diaryentry);
				diaryentries++;
			}
			rset.close();
			statement.close();
			
			_herodiary.put(charId, _diary);
			
			_log.info("Hero System: Loaded " + diaryentries + " diary entries for Hero: " + CharNameTable.getInstance().getNameById(charId));
		}
		catch (SQLException e)
		{
			_log.warn("Hero System: Couldnt load Hero Diary for char_id: " + charId);
		}
	}
	
	public void updateHeroes(boolean setDefault)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			if(setDefault)
			{
				PreparedStatement statement = con.prepareStatement(UPDATE_ALL);
				statement.execute();
				ResourceUtil.closeStatement(statement);
			}
			else
			{
				PreparedStatement statement;

				for(Integer heroId : _heroes.keySet())
				{
					StatsSet hero = _heroes.get(heroId);

					if(_completeHeroes == null || !_completeHeroes.containsKey(heroId))
					{
						statement = con.prepareStatement(INSERT_HERO);
						statement.setInt(1, heroId);
						statement.setString(2, hero.getString(Olympiad.CHAR_NAME));
						statement.setInt(3, hero.getInteger(Olympiad.CLASS_ID));
						statement.setInt(4, hero.getInteger(COUNT));
						statement.setInt(5, hero.getInteger(PLAYED));
						statement.setInt(6, hero.getInteger(ACTIVE));

						statement.execute();

						PreparedStatement statement2 = con.prepareStatement(GET_CLAN_ALLY);
						statement2.setInt(1, heroId);
						ResultSet rset2 = statement2.executeQuery();

						if(rset2.next())
						{
							int clanId = rset2.getInt("clanid");
							int allyId = rset2.getInt("allyId");

							String clanName = "";
							String allyName = "";

							int clanCrest = 0;
							int allyCrest = 0;

							if(clanId > 0)
							{
								clanName = ClanTable.getInstance().getClan(clanId).getName();
								clanCrest = ClanTable.getInstance().getClan(clanId).getCrestId();

								if(allyId > 0)
								{
									allyName = ClanTable.getInstance().getClan(clanId).getAllyName();
									allyCrest = ClanTable.getInstance().getClan(clanId).getAllyCrestId();
								}
							}

							hero.set(CLAN_CREST, clanCrest);
							hero.set(CLAN_NAME, clanName);
							hero.set(ALLY_CREST, allyCrest);
							hero.set(ALLY_NAME, allyName);
						}

						rset2.close();
						statement2.close();

						_heroes.remove(hero);
						_heroes.put(heroId, hero);

						_completeHeroes.put(heroId, hero);
					}
					else
					{
						statement = con.prepareStatement(UPDATE_HERO);
						statement.setInt(1, hero.getInteger(COUNT));
						statement.setInt(2, hero.getInteger(PLAYED));
						statement.setInt(3, hero.getInteger(ACTIVE));
						statement.setInt(4, heroId);
						statement.execute();
					}
					ResourceUtil.closeStatement(statement);
				}
			}
		}
		catch(SQLException e)
		{
			_log.error("Hero System: Couldnt update Heroes", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public int[] getHeroItems()
	{
		return _heroItems;
	}

	private void deleteItemsInDb()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_ITEMS);
			statement.execute();
			statement.close();
		}
		catch(SQLException e)
		{
			_log.error("", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public boolean isActiveHero(int id)
	{
		if(_heroes == null || _heroes.isEmpty())
		{
			return false;
		}
		if(_heroes.containsKey(id) && _heroes.get(id).getInteger(ACTIVE) == 1)
		{
			return true;
		}
		return false;
	}
	public boolean isInactiveHero(int id)
	{
		if(_heroes == null || _heroes.isEmpty())
		{
			return false;
		}
		if(_heroes.containsKey(id) && _heroes.get(id).getInteger(ACTIVE) == 0)
		{
			return true;
		}
		return false;
	}
	
	public void activateHero(L2PcInstance player)
	{
		StatsSet hero = _heroes.get(player.getObjectId());
		hero.set(ACTIVE, 1);
		_heroes.remove(player.getObjectId());
		_heroes.put(player.getObjectId(), hero);
		
		player.setIsHero(true);
		player.broadcastPacket(new SocialAction(player.getObjectId(), 16));
		player.broadcastUserInfo();
		if(player.getClan() != null && player.getClan().getLevel() >= 5)
		{
			L2Clan clan = player.getClan();
			String name = hero.getString("char_name");
			clan.setReputationScore(clan.getReputationScore() + 1000, true);
			clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
			clan.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_BECAME_HERO_AND_GAINED_S2_REPUTATION_POINTS).addString(name).addNumber(1000));
		}
		else
		{
			player.broadcastUserInfo();
		}
		updateHeroes(false);
	}

	public void setRBkilled(int charId, int npcId)
	{
		setDiaryData(charId, ACTION_RAID_KILLED, npcId);
		
		L2NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);
		
		if (_herodiary.containsKey(charId) && (template != null))
		{
			// Get Data
			List<StatsSet> _list = _herodiary.get(charId);
			
			// Clear old data
			_herodiary.remove(charId);
			
			// Prepare new data
			StatsSet _diaryentry = new StatsSet();
			String date = (new SimpleDateFormat("yyyy-MM-dd HH")).format(new Date(System.currentTimeMillis()));
			_diaryentry.set("date", date);
			_diaryentry.set("action", template.getName() + " was defeated");
			
			// Add to old list
			_list.add(_diaryentry);
			
			// Put new list into diary
			_herodiary.put(charId, _list);
		}
	}
	
	public void setCastleTaken(int charId, int castleId)
	{
		setDiaryData(charId, ACTION_CASTLE_TAKEN, castleId);
		
		Castle castle = CastleManager.getInstance().getCastleById(castleId);
		
		if (_herodiary.containsKey(charId) && (castle != null))
		{
			// Get Data
			List<StatsSet> _list = _herodiary.get(charId);
			
			// Clear old data
			_herodiary.remove(charId);
			
			// Prepare new data
			StatsSet _diaryentry = new StatsSet();
			String date = (new SimpleDateFormat("yyyy-MM-dd HH")).format(new Date(System.currentTimeMillis()));
			_diaryentry.set("date", date);
			_diaryentry.set("action", castle.getName() + " Castle was successfuly taken");
			
			// Add to old list
			_list.add(_diaryentry);
			
			// Put new list into diary
			_herodiary.put(charId, _list);
		}
	}
	
	public void setDiaryData(int charId, int action, int param)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(UPDATE_DIARIES);
			statement.setInt(1, charId);
			statement.setLong(2, System.currentTimeMillis());
			statement.setInt(3, action);
			statement.setInt(4, param);
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
				_log.warn(Level.SEVERE, e);
		}
	}
}