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
import java.util.List;

import javolution.util.FastList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.datatables.sql.ItemTable;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.itemcontainer.Inventory;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class L2TradeList
{
	private static final Log _log = LogFactory.getLog(L2TradeList.class);

	private List<L2ItemInstance> _items;
	private int _listId;
	private boolean _confirmed;
	private boolean _gm;
	private String _buystorename, _sellstorename;

	private String _npcId;

	public L2TradeList(int listId)
	{
		_items = new FastList<L2ItemInstance>();
		_listId = listId;
		_confirmed = false;
	}

	public void setNpcId(String id)
	{
		_npcId = id;
	}

	public String getNpcId()
	{
		return _npcId;
	}

	public void addItem(L2ItemInstance item)
	{
		_items.add(item);
	}

	public void replaceItem(int itemID, int price)
	{
		for(int i = 0; i < _items.size(); i++)
		{
			L2ItemInstance item = _items.get(i);

			if(item.getItemId() == itemID)
			{
				item.setPriceToSell(price);
			}

			item = null;
		}
	}

	public synchronized boolean decreaseCount(int itemID, int count)
	{

		for(int i = 0; i < _items.size(); i++)
		{
			L2ItemInstance item = _items.get(i);

			if(item.getItemId() == itemID)
			{
				if(item.getCount()<count)
				{
					return false;
				}

				item.setCount(item.getCount() - count);
			}

			item = null;
		}
		return true;
	}

	public void restoreCount(int time)
	{
		for(int i = 0; i < _items.size(); i++)
		{
			L2ItemInstance item = _items.get(i);

			if(item.getCountDecrease() && item.getTime() == time)
			{
				item.restoreInitCount();
			}

			item = null;
		}
	}

	public void removeItem(int itemID)
	{
		for(int i = 0; i < _items.size(); i++)
		{
			L2ItemInstance item = _items.get(i);

			if(item.getItemId() == itemID)
			{
				_items.remove(i);
			}

			item = null;
		}
	}

	public int getListId()
	{
		return _listId;
	}

	public void setSellStoreName(String name)
	{
		_sellstorename = name;
	}

	public String getSellStoreName()
	{
		return _sellstorename;
	}

	public void setBuyStoreName(String name)
	{
		_buystorename = name;
	}

	public String getBuyStoreName()
	{
		return _buystorename;
	}

	public List<L2ItemInstance> getItems()
	{
		return _items;
	}

	public List<L2ItemInstance> getItems(int start, int end)
	{
		return _items.subList(start, end);
	}

	public int getPriceForItemId(int itemId)
	{
		for(int i = 0; i < _items.size(); i++)
		{
			L2ItemInstance item = _items.get(i);

			if(item.getItemId() == itemId)
			{
				return item.getPriceToSell();
			}

			item = null;
		}
		return -1;
	}

	public boolean countDecrease(int itemId)
	{
		for(int i = 0; i < _items.size(); i++)
		{
			L2ItemInstance item = _items.get(i);

			if(item.getItemId() == itemId)
			{
				return item.getCountDecrease();
			}

			item = null;
		}
		return false;
	}

	public boolean containsItemId(int itemId)
	{
		for(L2ItemInstance item : _items)
		{
			if(item.getItemId() == itemId)
			{
				return true;
			}

			item = null;
		}

		return false;
	}

	public L2ItemInstance getItem(int ObjectId)
	{
		for(int i = 0; i < _items.size(); i++)
		{
			L2ItemInstance item = _items.get(i);

			if(item.getObjectId() == ObjectId)
			{
				return item;
			}

			item = null;
		}
		return null;
	}

	public synchronized void setConfirmedTrade(boolean x)
	{
		_confirmed = x;
	}

	public synchronized boolean hasConfirmed()
	{
		return _confirmed;
	}

	public void removeItem(int objId, int count)
	{
		L2ItemInstance temp;

		for(int y = 0; y < _items.size(); y++)
		{
			temp = _items.get(y);

			if(temp.getObjectId() == objId)
			{
				if(count == temp.getCount())
				{
					_items.remove(temp);
				}

				break;
			}
		}

		temp = null;
	}

	public boolean contains(int objId)
	{
		boolean bool = false;

		L2ItemInstance temp;

		for(int y = 0; y < _items.size(); y++)
		{
			temp = _items.get(y);

			if(temp.getObjectId() == objId)
			{
				bool = true;
				break;
			}
		}

		temp = null;

		return bool;
	}

	public boolean validateTrade(L2PcInstance player)
	{
		Inventory playersInv = player.getInventory();
		L2ItemInstance playerItem, temp;

		for(int y = 0; y < _items.size(); y++)
		{
			temp = _items.get(y);
			playerItem = playersInv.getItemByObjectId(temp.getObjectId());

			if(playerItem == null || playerItem.getCount() < temp.getCount())
			{
				return false;
			}
		}

		playersInv = null;
		playerItem = null;
		temp = null;

		return true;
	}

	public void tradeItems(L2PcInstance player, L2PcInstance reciever)
	{
		Inventory playersInv = player.getInventory();
		Inventory recieverInv = reciever.getInventory();
		L2ItemInstance playerItem, recieverItem, temp, newitem;
		InventoryUpdate update = new InventoryUpdate();
		ItemTable itemTable = ItemTable.getInstance();

		for(int y = 0; y < _items.size(); y++)
		{
			temp = _items.get(y);
			playerItem = playersInv.getItemByObjectId(temp.getObjectId());

			if(playerItem == null)
			{
				continue;
			}

			newitem = itemTable.createItem("L2TradeList", playerItem.getItemId(), playerItem.getCount(), player);
			newitem.setEnchantLevel(temp.getEnchantLevel());

			changePetItemObjectId(playerItem.getObjectId(), newitem.getObjectId());

			if(reciever.isGM() || player.isGM())
			{
				L2PcInstance gm;
				L2PcInstance target;
				if(reciever.isGM())
				{
					gm = reciever;
					target = player;
				}
				else
				{
					gm = player;
					target = reciever;
				}
				GMAudit.auditGMAction(gm.getName(), "trade", target.getName(), newitem.getItem().getName()+" - "+newitem.getItemId());
			}

			playerItem = playersInv.destroyItem("!L2TradeList!", playerItem.getObjectId(), temp.getCount(), null, null);
			recieverItem = recieverInv.addItem("!L2TradeList!", newitem, null, null);

			if(playerItem == null)
			{
				_log.warn("L2TradeList: PlayersInv.destroyItem returned NULL!");
				continue;
			}

			if(playerItem.getLastChange() == L2ItemInstance.MODIFIED)
			{
				update.addModifiedItem(playerItem);
			}
			else
			{
				L2World world = L2World.getInstance();
				world.removeObject(playerItem);
				update.addRemovedItem(playerItem);
				world = null;

			}

			player.sendPacket(update);
			update = null;

			update = new InventoryUpdate();
			if(recieverItem.getLastChange() == L2ItemInstance.MODIFIED)
			{
				update.addModifiedItem(recieverItem);
			}
			else
			{
				update.addNewItem(recieverItem);
			}

			reciever.sendPacket(update);
		}

		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
		su = null;

		su = new StatusUpdate(reciever.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, reciever.getCurrentLoad());
		reciever.sendPacket(su);
		su = null;

		playersInv = null;
		recieverInv = null;
		playerItem = null;
		recieverItem = null;
		temp = null;
		newitem = null;
		update = null;
		itemTable = null;
	}

	private void changePetItemObjectId(int oldObjectId, int newObjectId)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE pets SET item_obj_id = ? WHERE item_obj_id = ?");
			statement.setInt(1, newObjectId);
			statement.setInt(2, oldObjectId);
			statement.executeUpdate();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("could not change pet item object id", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	public void updateBuyList(L2PcInstance player, List<TradeItem> list)
	{

		TradeItem temp;
		int count;
		Inventory playersInv = player.getInventory();
		L2ItemInstance temp2;
		count = 0;

		while(count != list.size())
		{
			temp = list.get(count);
			temp2 = playersInv.getItemByItemId(temp.getItemId());

			if(temp2 == null)
			{
				list.remove(count);
				count = count - 1;
			}
			else
			{
				if(temp.getCount() == 0)
				{
					list.remove(count);
					count = count - 1;
				}
			}
			count++;
		}

		temp = null;
		playersInv = null;
		temp2 = null;

	}

	public void updateSellList(L2PcInstance player, List<TradeItem> list)
	{
		Inventory playersInv = player.getInventory();
		TradeItem temp;
		L2ItemInstance temp2;

		int count = 0;
		while(count != list.size())
		{
			temp = list.get(count);
			temp2 = playersInv.getItemByObjectId(temp.getObjectId());

			if(temp2 == null)
			{
				list.remove(count);
				count = count - 1;
			}
			else
			{
				if(temp2.getCount() < temp.getCount())
				{
					temp.setCount(temp2.getCount());
				}

			}
			count++;
		}

		playersInv = null;
		temp = null;
		temp2 = null;

	}

	public synchronized void buySellItems(L2PcInstance buyer, List<TradeItem> buyerslist, L2PcInstance seller, List<TradeItem> sellerslist)
	{
		Inventory sellerInv = seller.getInventory();
		Inventory buyerInv = buyer.getInventory();

		TradeItem temp2 = null;

		L2ItemInstance sellerItem = null;
		L2ItemInstance temp = null;
		L2ItemInstance newitem = null;
		L2ItemInstance adena = null;

		int enchantLevel = 0;

		InventoryUpdate buyerupdate = new InventoryUpdate();
		InventoryUpdate sellerupdate = new InventoryUpdate();

		ItemTable itemTable = ItemTable.getInstance();

		int amount = 0;
		int x = 0;
		int y = 0;

		List<SystemMessage> sysmsgs = new FastList<SystemMessage>();

		for(TradeItem buyerItem : buyerslist)
		{
			for(x = 0; x < sellerslist.size(); x++)
			{
				temp2 = sellerslist.get(x);
				if(temp2.getItemId() == buyerItem.getItemId())
				{
					sellerItem = sellerInv.getItemByItemId(buyerItem.getItemId());
					break;
				}
			}

			if(sellerItem != null)
			{
				if(buyerItem.getCount() > temp2.getCount())
				{
					amount = temp2.getCount();
				}

				if(buyerItem.getCount() > sellerItem.getCount())
				{
					amount = sellerItem.getCount();
				}
				else
				{
					amount = buyerItem.getCount();
				}

				if(buyerItem.getCount() > Integer.MAX_VALUE / buyerItem.getOwnersPrice())
				{
					_log.warn("Integer Overflow on Cost. Possible Exploit attempt between " + buyer.getName() + " and " + seller.getName() + ".");
					return;
				}

				enchantLevel = sellerItem.getEnchantLevel();
				sellerItem = sellerInv.destroyItem("", sellerItem.getObjectId(), amount, null, null);
				newitem = itemTable.createItem("L2TradeList", sellerItem.getItemId(), amount, buyer, seller);
				newitem.setEnchantLevel(enchantLevel);
				temp = buyerInv.addItem("", newitem, null, null);

				if(amount == 1)
				{
					sysmsgs.add(new SystemMessage(SystemMessageId.S1_PURCHASED_S2).addString(buyer.getName()).addItemName(sellerItem.getItemId()));
					sysmsgs.add(new SystemMessage(SystemMessageId.S1_PURCHASED_S2).addString("You").addItemName(sellerItem.getItemId()));
				}
				else
				{
					sysmsgs.add(new SystemMessage(SystemMessageId.S1_PURCHASED_S3_S2_S).addString(buyer.getName()).addItemName(sellerItem.getItemId()).addNumber(amount));
					sysmsgs.add(new SystemMessage(SystemMessageId.S1_PURCHASED_S3_S2_S).addString("You").addItemName(sellerItem.getItemId()).addNumber(amount));
				}

				if(temp2.getCount() == buyerItem.getCount())
				{
					sellerslist.remove(temp2);
					buyerItem.setCount(0);
				}
				else
				{
					if(buyerItem.getCount() < temp2.getCount())
					{
						temp2.setCount(temp2.getCount() - buyerItem.getCount());
					}
					else
					{
						buyerItem.setCount(buyerItem.getCount() - temp2.getCount());
					}
				}

				if(sellerItem.getLastChange() == L2ItemInstance.MODIFIED)
				{
					sellerupdate.addModifiedItem(sellerItem);

				}
				else
				{
					L2World world = L2World.getInstance();
					world.removeObject(sellerItem);
					sellerupdate.addRemovedItem(sellerItem);
					world = null;
				}

				if(temp.getLastChange() == L2ItemInstance.MODIFIED)
				{
					buyerupdate.addModifiedItem(temp);
				}
				else
				{
					buyerupdate.addNewItem(temp);
				}

				sellerItem = null;
			}
		}

		if(newitem != null)
		{
			adena = seller.getInventory().getAdenaInstance();
			adena.setLastChange(L2ItemInstance.MODIFIED);
			sellerupdate.addModifiedItem(adena);
			adena = buyer.getInventory().getAdenaInstance();
			adena.setLastChange(L2ItemInstance.MODIFIED);
			buyerupdate.addModifiedItem(adena);

			seller.sendPacket(sellerupdate);
			buyer.sendPacket(buyerupdate);
			y = 0;

			for(x = 0; x < sysmsgs.size(); x++)
			{

				if(y == 0)
				{
					seller.sendPacket(sysmsgs.get(x));
					y = 1;
				}
				else
				{
					buyer.sendPacket(sysmsgs.get(x));
					y = 0;
				}
			}
		}

		sellerInv = null;
		buyerInv = null;
		temp2 = null;
		sellerItem = null;
		temp = null;
		newitem = null;
		adena = null;
		buyerupdate = null;
		sellerupdate = null;
		itemTable = null;
		sysmsgs = null;
	}

	public boolean isGm()
	{
		return _gm;
	}

}