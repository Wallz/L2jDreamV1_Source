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
package com.src.gameserver.network.clientpackets;

import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;

import com.src.Config;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2NpcInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.itemcontainer.ItemContainer;
import com.src.gameserver.model.itemcontainer.PcFreight;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.item.L2EtcItemType;

public final class RequestPackageSend extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestPackageSend.class.getName());

	private static final String _C_9F_REQUESTPACKAGESEND = "[C] 9F RequestPackageSend";

	private List<Item> _items = new FastList<Item>();
	private int _objectID;
	private int _count;

	@Override
	protected void readImpl()
	{
		_objectID = readD();
		_count = readD();

		if(_count < 0 || _count > 500)
		{
			_count = -1;
			return;
		}

		for(int i = 0; i < _count; i++)
		{
			int id = readD();
			int count = readD();
			_items.add(new Item(id, count));
		}
	}

	@Override
	protected void runImpl()
	{
		if(_count == -1 || _items == null)
		{
			return;
		}

		L2PcInstance player = getClient().getActiveChar();

		if(player == null)
		{
			return;
		}

		if(player.getObjectId() == _objectID)
		{
			return;
		}

		L2PcInstance target = L2PcInstance.load(_objectID);

		if(player.getAccountChars().size() < 1)
		{
			return;
		}
		else if(!player.getAccountChars().containsKey(_objectID))
		{
			return;
		}

		if(L2World.getInstance().getPlayer(_objectID) != null)
		{
			return;
		}

		PcFreight freight = target.getFreight();
		player.setActiveWarehouse(freight);
		target.deleteMe();
		ItemContainer warehouse = player.getActiveWarehouse();

		if(warehouse == null)
		{
			return;
		}

		L2NpcInstance manager = player.getLastFolkNPC();

		if((manager == null || !player.isInsideRadius(manager, L2Npc.INTERACTION_DISTANCE, false, false)) && !player.isGM())
		{
			return;
		}

		if(warehouse instanceof PcFreight && !player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Unsufficient privileges.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(!Config.KARMA_PLAYER_CAN_USE_WAREHOUSE && player.getKarma() > 0)
		{
			return;
		}

		int fee = _count * Config.ALT_GAME_FREIGHT_PRICE;
		int currentAdena = player.getAdena();
		int slots = 0;

		for(Item i : _items)
		{
			int objectId = i.id;
			int count = i.count;

			L2ItemInstance item = player.checkItemManipulation(objectId, count, "deposit");

			if(item == null)
			{
				_log.warning("Error depositing a warehouse object for char " + player.getName() + " (validity check)");
				i.id = 0;
				i.count = 0;
				continue;
			}

			if(item.isAugmented())
			{
				_log.warning("Error depositing a warehouse object for char " + player.getName() + " (item is augmented)");
				return;
			}

			if(!item.isTradeable() || item.getItemType() == L2EtcItemType.QUEST)
			{
				return;
			}

			if(item.getItemId() == 57)
			{
				currentAdena -= count;
			}

			if(!item.isStackable())
			{
				slots += count;
			}
			else if(warehouse.getItemByItemId(item.getItemId()) == null)
			{
				slots++;
			}
		}

		if(!warehouse.validateCapacity(slots))
		{
			sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
			return;
		}

		if(currentAdena < fee || !player.reduceAdena("Warehouse", fee, player.getLastFolkNPC(), false))
		{
			sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			return;
		}

		InventoryUpdate playerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
		for(Item i : _items)
		{
			int objectId = i.id;
			int count = i.count;

			if(objectId == 0 && count == 0)
			{
				continue;
			}

			L2ItemInstance oldItem = player.getInventory().getItemByObjectId(objectId);

			if(oldItem == null)
			{
				_log.warning("Error depositing a warehouse object for char " + player.getName() + " (olditem == null)");
				continue;
			}

			int itemId = oldItem.getItemId();

			if(itemId >= 6611 && itemId <= 6621 || itemId == 6842)
			{
				continue;
			}

			L2ItemInstance newItem = player.getInventory().transferItem("Warehouse", objectId, count, warehouse, player, player.getLastFolkNPC());

			if(newItem == null)
			{
				_log.warning("Error depositing a warehouse object for char " + player.getName() + " (newitem == null)");
				continue;
			}

			if(playerIU != null)
			{
				if(oldItem.getCount() > 0 && oldItem != newItem)
				{
					playerIU.addModifiedItem(oldItem);
				}
				else
				{
					playerIU.addRemovedItem(oldItem);
				}
			}
		}

		if(playerIU != null)
		{
			player.sendPacket(playerIU);
		}
		else
		{
			player.sendPacket(new ItemList(player, false));
		}

		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);

		player.setActiveWarehouse(null);
	}

	private class Item
	{
		public int id;
		public int count;

		public Item(int i, int c)
		{
			id = i;
			count = c;
		}
	}

	@Override
	public String getType()
	{
		return _C_9F_REQUESTPACKAGESEND;
	}

}