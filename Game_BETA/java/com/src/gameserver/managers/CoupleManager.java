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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javolution.util.FastList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.Wedding;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class CoupleManager
{
	private static final Log _log = LogFactory.getLog(CoupleManager.class.getName());

	private static CoupleManager _instance;

	public static final CoupleManager getInstance()
	{
		if(_instance == null)
		{
			_log.info("Wedding Manager: Active");
			_instance = new CoupleManager();
			_instance.load();
		}

		return _instance;
	}

	private FastList<Wedding> _couples;

	public void reload()
	{
		getCouples().clear();
		load();
	}

	private final void load()
	{
		Connection con = null;
		try
		{
			PreparedStatement statement;
			ResultSet rs;

			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("SELECT id FROM mods_wedding ORDER BY id");
			rs = statement.executeQuery();

			while(rs.next())
			{
				getCouples().add(new Wedding(rs.getInt("id")));
			}

			statement.close();
			statement = null;
			rs.close();
			rs = null;

			_log.info("Wedding Manager: Loaded " + getCouples().size() + " couples.");
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

	public final Wedding getCouple(int coupleId)
	{
		int index = getCoupleIndex(coupleId);
		if(index >= 0)
		{
			return getCouples().get(index);
		}
		return null;
	}

	public void createCouple(L2PcInstance player1, L2PcInstance player2)
	{
		if(player1 != null && player2 != null)
		{
			if(player1.getPartnerId() == 0 && player2.getPartnerId() == 0)
			{
				int _player1id = player1.getObjectId();
				int _player2id = player2.getObjectId();

				Wedding _new = new Wedding(player1, player2);
				getCouples().add(_new);
				player1.setPartnerId(_player2id);
				player2.setPartnerId(_player1id);
				player1.setCoupleId(_new.getId());
				player2.setCoupleId(_new.getId());

				_new = null;
			}
		}
	}

	public void deleteCouple(int coupleId)
	{
		int index = getCoupleIndex(coupleId);
		Wedding wedding = getCouples().get(index);

		if(wedding != null)
		{
			L2PcInstance player1 = (L2PcInstance) L2World.getInstance().findObject(wedding.getPlayer1Id());
			L2PcInstance player2 = (L2PcInstance) L2World.getInstance().findObject(wedding.getPlayer2Id());
			L2ItemInstance item = null;
			if(player1 != null)
			{
				player1.setPartnerId(0);
				player1.setMarried(false);
				player1.setCoupleId(0);
				item = player1.getInventory().getItemByItemId(9140);
				if(player1.isOnline() == 1 && item != null)
				{
					player1.destroyItem("Removing Cupids Bow", item, player1, true);
					player1.getInventory().updateDatabase();
				}
				if(player1.isOnline() == 0 && item != null)
				{
					Integer PlayerId = player1.getObjectId();
					Integer ItemId = 9140;
					Connection con = null;
					try
					{
						con = L2DatabaseFactory.getInstance().getConnection();
						PreparedStatement statement;

						statement = con.prepareStatement("DELETE FROM items WHERE owner_id = ? AND item_id = ?");
						statement.setInt(1, PlayerId);
						statement.setInt(2, ItemId);
						statement.execute();
						statement.close();
						statement = null;
					}
					catch(Exception e)
					{
					}
					finally
					{
						ResourceUtil.closeConnection(con); 
					}
				}
			}
			if(player2 != null)
			{
				player2.setPartnerId(0);
				player2.setMarried(false);
				player2.setCoupleId(0);
				item = player2.getInventory().getItemByItemId(9140);
				if(player2.isOnline() == 1  && item != null)
				{
					player2.destroyItem("Removing Cupids Bow", item, player2, true);
					player2.getInventory().updateDatabase();
				}
				if(player2.isOnline() == 0  && item != null)
				{
					Integer Player2Id = player2.getObjectId();
					Integer Item2Id = 9140;
					Connection con = null;
					try
					{
						con = L2DatabaseFactory.getInstance().getConnection();
						PreparedStatement statement;

						statement = con.prepareStatement("DELETE FROM items WHERE owner_id = ? AND item_id = ?");
						statement.setInt(1, Player2Id);
						statement.setInt(2, Item2Id);
						statement.execute();
						statement.close();
						statement = null;
					}
					catch(Exception e)
					{
					}
					finally
					{
						ResourceUtil.closeConnection(con); 
					}
				}
			}
			wedding.divorce();
			getCouples().remove(index);

			player1 = null;
			player2 = null;
			wedding = null;
		}
	}

	public final int getCoupleIndex(int coupleId)
	{
		Wedding wedding;
		for(int i = 0; i < getCouples().size(); i++)
		{
			wedding = getCouples().get(i);
			if(wedding != null && wedding.getId() == coupleId)
			{
				return i;
			}
		}

		return -1;
	}

	public final FastList<Wedding> getCouples()
	{
		if(_couples == null)
		{
			_couples = new FastList<Wedding>();
		}

		return _couples;
	}
}