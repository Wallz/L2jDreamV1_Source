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

import java.util.logging.Logger;

import com.src.Config;
import com.src.gameserver.model.ClanWarehouse;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2NpcInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.itemcontainer.ItemContainer;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.EnchantResult;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;

public final class SendWareHouseWithDrawList extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(SendWareHouseWithDrawList.class.getName());

	private static final String _C__32_SENDWAREHOUSEWITHDRAWLIST = "[C] 32 SendWareHouseWithDrawList";

	private int _count;
	private int[] _items;

	@Override
	protected void readImpl()
	{
		_count = readD();

		if(_count < 0 || _count * 8 > _buf.remaining() || _count > Config.MAX_ITEM_IN_PACKET)
		{
			_count = 0;
			_items = null;
			return;
		}

		_items = new int[_count * 2];

		for(int i = 0; i < _count; i++)
		{
			int objectId = readD();
			_items[i * 2 + 0] = objectId;
			long cnt = readD();

			if(cnt > Integer.MAX_VALUE || cnt < 0)
			{
				_count = 0;
				_items = null;
				return;
			}

			_items[i * 2 + 1] = (int) cnt;
		}
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if(player == null)
			return;

        if(player.getActiveTradeList() != null)
        {
                player.sendMessage("You can't withdraw items when you are trading.");
                player.sendPacket(ActionFailed.STATIC_PACKET);
                return;
        }

		ItemContainer warehouse = player.getActiveWarehouse();
		if(warehouse == null)
			return;

		L2NpcInstance manager = player.getLastFolkNPC();
		if((manager == null || !player.isInsideRadius(manager, L2Npc.INTERACTION_DISTANCE, false, false)) && !player.isGM())
			return;

		if(warehouse instanceof ClanWarehouse && !player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Unsufficient privileges.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(!Config.KARMA_PLAYER_CAN_USE_WAREHOUSE && player.getKarma() > 0)
			return;

		if(Config.ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH)
		{
			if(warehouse instanceof ClanWarehouse && (player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) != L2Clan.CP_CL_VIEW_WAREHOUSE)
				return;
		}
		else
		{
			if(warehouse instanceof ClanWarehouse && !player.isClanLeader())
			{
				player.sendPacket(new SystemMessage(SystemMessageId.ONLY_CLAN_LEADER_CAN_RETRIEVE_ITEMS_FROM_CLAN_WAREHOUSE));
				return;
			}
		}

		int weight = 0;
		int slots = 0;

		for(int i = 0; i < _count; i++)
		{
			int objectId = _items[i * 2 + 0];
			int count = _items[i * 2 + 1];

			L2ItemInstance item = warehouse.getItemByObjectId(objectId);
			if(item == null)
			{
				continue;
			}
			weight += count * item.getItem().getWeight();
			if(!item.isStackable())
			{
				slots += count;
			}
			else if(player.getInventory().getItemByItemId(item.getItemId()) == null)
			{
				slots++;
			}
		}

		if(!player.getInventory().validateCapacity(slots))
		{
			sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
			return;
		}

        if(player.getActiveEnchantItem() != null)
        {
                sendPacket(new SystemMessage(SystemMessageId.ENCHANT_SCROLL_CANCELLED));
                player.sendPacket(new EnchantResult(0));
                player.sendPacket(ActionFailed.STATIC_PACKET);
                return;
        }

		if(!player.getInventory().validateWeight(weight))
		{
			sendPacket(new SystemMessage(SystemMessageId.WEIGHT_LIMIT_EXCEEDED));
			return;
		}

		InventoryUpdate playerIU = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
		for(int i = 0; i < _count; i++)
		{
			int objectId = _items[i * 2 + 0];
			int count = _items[i * 2 + 1];

			L2ItemInstance oldItem = warehouse.getItemByObjectId(objectId);
			if(oldItem == null || oldItem.getCount() < count)
			{
				player.sendMessage("Can't withdraw requested item" + (count > 1 ? "s" : ""));
			}
			L2ItemInstance newItem = warehouse.transferItem("Warehouse", objectId, count, player.getInventory(), player, player.getLastFolkNPC());
			if(newItem == null)
			{
				_log.warning("Error withdrawing a warehouse object for char " + player.getName());
				continue;
			}

			if(playerIU != null)
			{
				if(newItem.getCount() > count)
				{
					playerIU.addModifiedItem(newItem);
				}
				else
				{
					playerIU.addNewItem(newItem);
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
	}

	@Override
	public String getType()
	{
		return _C__32_SENDWAREHOUSEWITHDRAWLIST;
	}
}