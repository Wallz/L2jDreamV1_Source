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
package com.src.gameserver.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.src.Config;
import com.src.gameserver.model.CursedWeapon;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2FeedableBeastInstance;
import com.src.gameserver.model.actor.instance.L2FestivalMonsterInstance;
import com.src.gameserver.model.actor.instance.L2GrandBossInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2RiftInvaderInstance;
import com.src.gameserver.model.actor.instance.L2SiegeGuardInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class CursedWeaponsManager
{
	private static final Log _log = LogFactory.getLog(CursedWeaponsManager.class);

	private static CursedWeaponsManager _instance;

	public static final CursedWeaponsManager getInstance()
	{
		if(_instance == null)
		{
			_instance = new CursedWeaponsManager();
		}
		return _instance;
	}

	private Map<Integer, CursedWeapon> _cursedWeapons;

	public CursedWeaponsManager()
	{
		_cursedWeapons = new FastMap<Integer, CursedWeapon>();

		if(!Config.ALLOW_CURSED_WEAPONS)
		{
			return;
		}

		load();
		restore();
		controlPlayers();

		_log.info("CursedWeaponsManager: Loaded " + _cursedWeapons.size() + " weapons.");
	}

	public final void reload()
	{
		_instance = new CursedWeaponsManager();
	}

	private final void load()
	{
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			File file = new File(Config.DATAPACK_ROOT + "/data/xml/cursed_weapons.xml");
			if(!file.exists())
			{
				return;
			}

			Document doc = factory.newDocumentBuilder().parse(file);

			factory = null;
			file = null;

			for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if("list".equalsIgnoreCase(n.getNodeName()))
				{
					for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if("item".equalsIgnoreCase(d.getNodeName()))
						{
							NamedNodeMap attrs = d.getAttributes();
							int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							int skillId = Integer.parseInt(attrs.getNamedItem("skillId").getNodeValue());
							String name = attrs.getNamedItem("name").getNodeValue();

							CursedWeapon cw = new CursedWeapon(id, skillId, name);
							name = null;

							int val;
							for(Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
							{
								if("dropRate".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
									cw.setDropRate(val);
								}
								else if("duration".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
									cw.setDuration(val);
								}
								else if("durationLost".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
									cw.setDurationLost(val);
								}
								else if("disapearChance".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
									cw.setDisapearChance(val);
								}
								else if("stageKills".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									val = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
									cw.setStageKills(val);
								}
							}

							_cursedWeapons.put(id, cw);

							attrs = null;
							cw = null;
						}
					}
				}
			}

			doc = null;
		}
		catch(Exception e)
		{
			_log.error("Error parsing cursed weapons file.", e);
			return;
		}
	}

	private final void restore()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			PreparedStatement statement = con.prepareStatement("SELECT itemId, playerId, playerKarma, playerPkKills, nbKills, endTime FROM cursed_weapons");
			ResultSet rset = statement.executeQuery();

			if(rset.next())
			{
				int itemId = rset.getInt("itemId");
				int playerId = rset.getInt("playerId");
				int playerKarma = rset.getInt("playerKarma");
				int playerPkKills = rset.getInt("playerPkKills");
				int nbKills = rset.getInt("nbKills");
				long endTime = rset.getLong("endTime");

				CursedWeapon cw = _cursedWeapons.get(itemId);
				cw.setPlayerId(playerId);
				cw.setPlayerKarma(playerKarma);
				cw.setPlayerPkKills(playerPkKills);
				cw.setNbKills(nbKills);
				cw.setEndTime(endTime);
				cw.reActivate();

				cw = null;
				
				removeFromDb(itemId);
			}

			rset.close();
			statement.close();
			rset = null;
			statement = null;
		}
		catch(Exception e)
		{
			_log.error("Could not restore CursedWeapons data", e);
			return;
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	private final void controlPlayers()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = null;
			ResultSet rset = null;

			for(CursedWeapon cw : _cursedWeapons.values())
			{
				if(cw.isActivated())
				{
					continue;
				}

				int itemId = cw.getItemId();
				try
				{
					statement = con.prepareStatement("SELECT owner_id FROM items WHERE item_id = ?");
					statement.setInt(1, itemId);
					rset = statement.executeQuery();

					if(rset.next())
					{
						int playerId = rset.getInt("owner_id");
						_log.info("PROBLEM : Player " + playerId + " owns the cursed weapon " + itemId + " but he shouldn't.");

						statement = con.prepareStatement("DELETE FROM items WHERE owner_id = ? AND item_id = ?");
						statement.setInt(1, playerId);
						statement.setInt(2, itemId);
						if(statement.executeUpdate() != 1)
						{
							_log.warn("Error while deleting cursed weapon " + itemId + " from userId " + playerId);
						}
						statement.close();

						statement = con.prepareStatement("UPDATE characters SET karma = ?, pkkills = ? WHERE obj_id = ?");
						statement.setInt(1, cw.getPlayerKarma());
						statement.setInt(2, cw.getPlayerPkKills());
						statement.setInt(3, playerId);
						if(statement.executeUpdate() != 1)
						{
							_log.warn("Error while updating karma & pkkills for userId " + cw.getPlayerId());
						}
					}
					rset.close();
					statement.close();
					rset = null;
					statement = null;

				}
				catch(SQLException sqlE)
				{
					_log.error("", sqlE);
				}
			}
		}
		catch(Exception e)
		{
			_log.error("Could not check CursedWeapons data", e);
			return;
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	public synchronized void checkDrop(L2Attackable attackable, L2PcInstance player)
	{
		if(attackable instanceof L2SiegeGuardInstance || attackable instanceof L2RiftInvaderInstance || attackable instanceof L2FestivalMonsterInstance || attackable instanceof L2GrandBossInstance || attackable instanceof L2FeedableBeastInstance)
		{
			return;
		}

		if(player.isCursedWeaponEquiped())
		{
			return;
		}

		for(CursedWeapon cw : _cursedWeapons.values())
		{
			if(cw.isActive())
			{
				continue;
			}

			if(cw.checkDrop(attackable, player))
			{
				break;
			}
		}
	}

	public void activate(L2PcInstance player, L2ItemInstance item)
	{
		CursedWeapon cw = _cursedWeapons.get(item.getItemId());
		if(player.isCursedWeaponEquiped())
		{
			CursedWeapon cw2 = _cursedWeapons.get(player.getCursedWeaponEquipedId());
			cw2.setNbKills(cw2.getStageKills() - 1);
			cw2.increaseKills();

			cw.setPlayer(player);
			cw.endOfLife();
		}
		else
		{
			cw.activate(player, item);
		}

		cw = null;
	}

	public void drop(int itemId, L2Character killer)
	{
		killer.sendPacket(new StatusUpdate(killer.getObjectId()));
		CursedWeapon cw = _cursedWeapons.get(itemId);

		cw.dropIt(killer);
		cw = null;
	}

	public void increaseKills(int itemId)
	{
		CursedWeapon cw = _cursedWeapons.get(itemId);

		cw.increaseKills();
		cw = null;
	}

	public int getLevel(int itemId)
	{
		CursedWeapon cw = _cursedWeapons.get(itemId);

		return cw.getLevel();
	}

	public static void announce(SystemMessage sm)
	{
		for(L2PcInstance player : L2World.getInstance().getAllPlayers())
		{
			if(player == null)
			{
				continue;
			}

			player.sendPacket(sm);
		}
	}

	public void checkPlayer(L2PcInstance player)
	{
		if(player == null)
		{
			return;
		}

		for(CursedWeapon cw : _cursedWeapons.values())
		{
			if(cw.isActive() && player.getObjectId() == cw.getPlayerId())
			{
				cw.setPlayer(player);
				cw.setItem(player.getInventory().getItemByItemId(cw.getItemId()));
				cw.giveSkill();
				player.setCursedWeaponEquipedId(cw.getItemId());

				player.sendPacket(new SystemMessage(SystemMessageId.S2_MINUTE_OF_USAGE_TIME_ARE_LEFT_FOR_S1).addString(cw.getName()).addNumber((int) ((cw.getEndTime() - System.currentTimeMillis()) / 60000)));
				CursedWeaponsManager.announce(new SystemMessage(SystemMessageId.THE_OWNER_OF_S2_HAS_APPEARED_IN_THE_S1_REGION).addZoneName(player.getX(), player.getY(), player.getZ()).addItemName(cw.getItemId()));
			}
		}
	}

	public static void removeFromDb(int itemId)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			PreparedStatement statement = con.prepareStatement("DELETE FROM cursed_weapons WHERE itemId = ?");
			statement.setInt(1, itemId);
			statement.executeUpdate();

			statement.close();
			statement = null;
		}
		catch(SQLException e)
		{
			_log.error("CursedWeaponsManager: Failed to remove data", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	public void saveData()
	{
		for(CursedWeapon cw : _cursedWeapons.values())
		{
			cw.saveData();
		}
	}

	public boolean isCursed(int itemId)
	{
		return _cursedWeapons.containsKey(itemId);
	}

	public Collection<CursedWeapon> getCursedWeapons()
	{
		return _cursedWeapons.values();
	}

	public Set<Integer> getCursedWeaponsIds()
	{
		return _cursedWeapons.keySet();
	}

	public CursedWeapon getCursedWeapon(int itemId)
	{
		return _cursedWeapons.get(itemId);
	}

	public void givePassive(int itemId)
	{
		try
		{
			_cursedWeapons.get(itemId).giveSkill();
		}
		catch(Exception e)
		{
		}
	}

}