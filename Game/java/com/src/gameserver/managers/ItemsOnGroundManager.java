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
import java.sql.Statement;
import java.util.List;

import javolution.util.FastList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.ItemsAutoDestroy;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.templates.item.L2EtcItemType;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class ItemsOnGroundManager
{
	private static final Log _log = LogFactory.getLog(ItemsOnGroundManager.class);

	private static ItemsOnGroundManager _instance;
	private List<L2ItemInstance> _items = null;

	private ItemsOnGroundManager()
	{
		if(!Config.SAVE_DROPPED_ITEM)
		{
			return;
		}
		_items = new FastList<L2ItemInstance>();

		if(Config.SAVE_DROPPED_ITEM_INTERVAL > 0)
		{
			ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new storeInDb(), Config.SAVE_DROPPED_ITEM_INTERVAL, Config.SAVE_DROPPED_ITEM_INTERVAL);
		}

		load();
	}

	public static final ItemsOnGroundManager getInstance()
	{
		if(_instance == null)
		{
			_instance = new ItemsOnGroundManager();
		}

		return _instance;
	}

	private void load()
	{
		if(!Config.SAVE_DROPPED_ITEM && Config.CLEAR_DROPPED_ITEM_TABLE)
		{
			emptyTable();
		}

		if(!Config.SAVE_DROPPED_ITEM)
		{
			return;
		}

		if(Config.DESTROY_DROPPED_PLAYER_ITEM)
		{
			Connection con = null;
			try
			{
				String str = null;
				if(!Config.DESTROY_EQUIPABLE_PLAYER_ITEM)
				{
					str = "update itemsonground set drop_time=? where drop_time=-1 and equipable=0";
				}
				else if(Config.DESTROY_EQUIPABLE_PLAYER_ITEM)
				{
					str = "update itemsonground set drop_time=? where drop_time=-1";
				}

				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(str);
				statement.setLong(1, System.currentTimeMillis());
				statement.execute();
				statement.close();
				str = null;
				statement = null;
			}
			catch(Exception e)
			{
				_log.error("error while updating table ItemsOnGround", e);
			}
			finally
			{
				ResourceUtil.closeConnection(con); 
			}
		}

		Connection con = null;
		try
		{
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				Statement s = con.createStatement();
				ResultSet result;
				int count = 0;
				result = s.executeQuery("select object_id,item_id,count,enchant_level,x,y,z,drop_time,equipable from itemsonground");
				while(result.next())
				{
					L2ItemInstance item = new L2ItemInstance(result.getInt(1), result.getInt(2));
					L2World.storeObject(item);
					if(item.isStackable() && result.getInt(3) > 1)
					{
						item.setCount(result.getInt(3));
					}

					if(result.getInt(4) > 0)
					{
						item.setEnchantLevel(result.getInt(4));
					}

					item.getPosition().setWorldPosition(result.getInt(5), result.getInt(6), result.getInt(7));
					item.getPosition().setWorldRegion(L2World.getInstance().getRegion(item.getPosition().getWorldPosition()));
					item.getPosition().getWorldRegion().addVisibleObject(item);
					item.setDropTime(result.getLong(8));
					if(result.getLong(8) == -1)
					{
						item.setProtected(true);
					}
					else
					{
						item.setProtected(false);
					}

					item.setIsVisible(true);
					L2World.getInstance().addVisibleObject(item, item.getPosition().getWorldRegion(), null);
					_items.add(item);
					count++;
					if(!Config.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
					{
						if(result.getLong(8) > -1)
						{
							if(Config.AUTODESTROY_ITEM_AFTER > 0 && item.getItemType() != L2EtcItemType.HERB || Config.HERB_AUTO_DESTROY_TIME > 0 && item.getItemType() == L2EtcItemType.HERB)
							{
								ItemsAutoDestroy.getInstance().addItem(item);
							}
						}
					}
					item = null;
				}

				result.close();
				s.close();
				result = null;
				s = null;

				if(count > 0)
				{
					System.out.println("ItemsOnGroundManager: restored " + count + " items.");
				}
			}
			catch(Exception e)
			{
				_log.error("error while loading ItemsOnGround", e);
			}
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
		if(Config.EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD)
		{
			emptyTable();
		}
	}

	public void save(L2ItemInstance item)
	{
		if(!Config.SAVE_DROPPED_ITEM)
		{
			return;
		}
		_items.add(item);
	}

	public void removeObject(L2Object item)
	{
		if(!Config.SAVE_DROPPED_ITEM)
		{
			return;
		}
		_items.remove(item);
	}

	public void saveInDb()
	{
		new storeInDb().run();
	}

	public void cleanUp()
	{
		_items.clear();
	}

	public void emptyTable()
	{
		Connection conn = null;
		try
		{
			conn = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement del = conn.prepareStatement("delete from itemsonground");
			del.execute();
			del.close();
			del = null;
		}
		catch(Exception e1)
		{
			_log.error("error while cleaning table ItemsOnGround", e1);
			e1.printStackTrace();
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch(Exception e)
			{
			}
			conn = null;
		}
	}

	protected class storeInDb extends Thread
	{
		@Override
		public void run()
		{
			if(!Config.SAVE_DROPPED_ITEM)
			{
				return;
			}

			emptyTable();

			if(_items.isEmpty())
				return;
			
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement;

				for(L2ItemInstance item : _items) 
				{
					if(CursedWeaponsManager.getInstance().isCursed(item.getItemId()))
						continue; // Cursed Items not saved to ground, prevent double save

					statement = con.prepareStatement("insert into itemsonground(object_id,item_id,count,enchant_level,x,y,z,drop_time,equipable) values(?,?,?,?,?,?,?,?,?)");

					statement.setInt(1, item.getObjectId());
					statement.setInt(2, item.getItemId());
					statement.setInt(3, item.getCount());
					statement.setInt(4, item.getEnchantLevel());
					statement.setInt(5, item.getX());
					statement.setInt(6, item.getY());
					statement.setInt(7, item.getZ());

					if(item.isProtected())
					{
						statement.setLong(8, -1);
					}
					else
					{
						statement.setLong(8, item.getDropTime());
					}
					if(item.isEquipable())
					{
						statement.setLong(9, 1);
					}
					else
					{
						statement.setLong(9, 0);
					}
					statement.execute();
					statement.close();
				}
			}
			catch(Exception e)
			{
				_log.error("error while inserting into table ItemsOnGround", e);
			}
			finally
			{
				ResourceUtil.closeConnection(con); 
			}
		}
	}
}