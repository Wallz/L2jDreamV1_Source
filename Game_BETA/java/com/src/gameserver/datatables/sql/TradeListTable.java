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

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.model.L2TradeList;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class TradeListTable
{
	private final static Log _log = LogFactory.getLog(TradeListTable.class.getName());
	private static TradeListTable _instance;

	private int _nextListId;
	private FastMap<Integer, L2TradeList> _lists;

	public class RestoreCount implements Runnable
	{
		private int timer;

		public RestoreCount(int time)
		{
			timer = time;
		}

		@Override
		public void run()
		{
			try
			{
				restoreCount(timer);
				dataTimerSave(timer);
				ThreadPoolManager.getInstance().scheduleGeneral(new RestoreCount(timer), (long) timer * 60 * 60 * 1000);
			}
			catch(Throwable t)
			{
			}
		}
	}

	public static TradeListTable getInstance()
	{
		if(_instance == null)
		{
			_instance = new TradeListTable();
		}

		return _instance;
	}

	private TradeListTable()
	{
		_lists = new FastMap<Integer, L2TradeList>();
		load();
	}

	private void load(boolean custom)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement1 = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString(new String[]
			{
					"shop_id", "npc_id"
			}) + " FROM " + (custom ? "custom_merchant_shopids" : "merchant_shopids"));
			ResultSet rset1 = statement1.executeQuery();

			while(rset1.next())
			{
				PreparedStatement statement = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString(new String[]
				{
						"item_id", "price", "shop_id", "order", "count", "time", "currentCount"
				}) + " FROM " + (custom ? "custom_merchant_buylists" : "merchant_buylists") + " WHERE shop_id=? ORDER BY " + L2DatabaseFactory.getInstance().safetyString(new String[]
				{
					"order"
				}) + " ASC");
				statement.setString(1, String.valueOf(rset1.getInt("shop_id")));
				ResultSet rset = statement.executeQuery();

				L2TradeList buylist = new L2TradeList(rset1.getInt("shop_id"));

				buylist.setNpcId(rset1.getString("npc_id"));
				int _itemId = 0;
				int _itemCount = 0;
				int _price = 0;

				if(!buylist.isGm() && NpcTable.getInstance().getTemplate(rset1.getInt("npc_id")) == null)
				{
					_log.warn("TradeListTable: Merchant id " + rset1.getString("npc_id") + " with " + (custom ? "custom " : "") + "buylist " + buylist.getListId() + " not exist.");
				}

				try
				{
					while(rset.next())
					{
						_itemId = rset.getInt("item_id");
						_price = rset.getInt("price");
						int count = rset.getInt("count");
						int currentCount = rset.getInt("currentCount");
						int time = rset.getInt("time");

						L2ItemInstance buyItem = ItemTable.getInstance().createDummyItem(_itemId);

						if(buyItem == null)
						{
							continue;
						}

						_itemCount++;

						if(count > -1)
						{
							buyItem.setCountDecrease(true);
						}
						buyItem.setPriceToSell(_price);
						buyItem.setTime(time);
						buyItem.setInitCount(count);

						if(currentCount > -1)
						{
							buyItem.setCount(currentCount);
						}
						else
						{
							buyItem.setCount(count);
						}

						buylist.addItem(buyItem);

						if(!buylist.isGm() && buyItem.getReferencePrice() > _price)
						{
							_log.warn("TradeListTable: Reference price of item " + _itemId + " in  " + (custom ? "custom " : "") + "buylist " + buylist.getListId() + " higher then sell price.");
						}
					}
				}
				catch(Exception e)
				{
					_log.warn("TradeListTable: Problem with " + (custom ? "custom " : "") + "buylist " + buylist.getListId() + " item " + _itemId + ".");
				}

				if(_itemCount > 0)
				{
					_lists.put(new Integer(buylist.getListId()), buylist);
					_nextListId = Math.max(_nextListId, buylist.getListId() + 1);
				}
				else
				{
					_log.warn("TradeListTable: Empty " + (custom ? "custom " : "") + " buylist " + buylist.getListId() + ".");
				}

				statement.close();
				rset.close();
				statement = null;
				rset = null;

				buylist = null;
			}
			rset1.close();
			rset1 = null;
			statement1.close();
			statement1 = null;

			_log.info("TradeListTable: Loaded " + _lists.size() + (custom ? "custom " : "") + " Buylists.");
			try
			{
				int time = 0;
				long savetimer = 0;
				long currentMillis = System.currentTimeMillis();
				PreparedStatement statement2 = con.prepareStatement("SELECT DISTINCT time, savetimer FROM " + (custom ? "merchant_buylists" : "merchant_buylists") + " WHERE time <> 0 ORDER BY time");
				ResultSet rset2 = statement2.executeQuery();

				while(rset2.next())
				{
					time = rset2.getInt("time");
					savetimer = rset2.getLong("savetimer");
					if(savetimer - currentMillis > 0)
					{
						ThreadPoolManager.getInstance().scheduleGeneral(new RestoreCount(time), savetimer - System.currentTimeMillis());
					}
					else
					{
						ThreadPoolManager.getInstance().scheduleGeneral(new RestoreCount(time), 0);
					}
				}

				rset2.close();
				rset2 = null;
				statement2.close();
				statement2 = null;
			}
			catch(Exception e)
			{
				_log.warn("TradeController: " + (custom ? "custom " : "") + "Could not restore Timer for Item count.");
				_log.error("", e);
			}
		}
		catch(Exception e)
		{
			_log.warn("TradeListTable: " + (custom ? "custom " : "") + "Buylists could not be initialized.", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	public void load()
	{
		load(false);
		load(true);
	}

	public void reloadAll()
	{
		_lists.clear();

		load();
	}

	public L2TradeList getBuyList(int listId)
	{
		if(_lists.containsKey(listId))
		{
			return _lists.get(listId);
		}

		return null;
	}

	public FastList<L2TradeList> getBuyListByNpcId(int npcId)
	{
		FastList<L2TradeList> lists = new FastList<L2TradeList>();

		for(L2TradeList list : _lists.values())
		{
			if(list.isGm())
			{
				continue;
			}
			lists.add(list);
		}

		return lists;
	}

	protected void restoreCount(int time)
	{
		if(_lists == null)
		{
			return;
		}

		for(L2TradeList list : _lists.values())
		{
			list.restoreCount(time);
		}
	}

	protected void dataTimerSave(int time)
	{
		Connection con = null;
		long timerSave = System.currentTimeMillis() + (long) time * 3600000;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE merchant_buylists SET savetimer = ? WHERE time = ?");
			statement.setLong(1, timerSave);
			statement.setInt(2, time);
			statement.executeUpdate();
			statement.close();
			statement = null;
		}
		catch(Exception e)
		{
			_log.fatal("TradeController: Could not update Timer save in Buylist");
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	public void dataCountStore()
	{
		Connection con = null;
		PreparedStatement statement;

		int listId;

		if(_lists == null)
		{
			return;
		}

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			for(L2TradeList list : _lists.values())
			{
				if(list == null)
				{
					continue;
				}

				listId = list.getListId();

				for(L2ItemInstance Item : list.getItems())
				{
					if(Item.getCount() < Item.getInitCount())
					{
						statement = con.prepareStatement("UPDATE merchant_buylists SET currentCount = ? WHERE item_id = ? AND shop_id = ?");
						statement.setInt(1, Item.getCount());
						statement.setInt(2, Item.getItemId());
						statement.setInt(3, listId);
						statement.executeUpdate();
						statement.close();
						statement = null;
					}
				}
			}
		}
		catch(Exception e)
		{
			_log.fatal("TradeController: Could not store Count Item");
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}
}