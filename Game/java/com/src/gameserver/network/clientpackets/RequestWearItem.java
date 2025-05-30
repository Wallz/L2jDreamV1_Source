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
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.src.Config;
import com.src.gameserver.TradeController;
import com.src.gameserver.datatables.sql.ItemTable;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2TradeList;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2MercManagerInstance;
import com.src.gameserver.model.actor.instance.L2MerchantInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.item.L2Item;
import com.src.gameserver.templates.item.L2Weapon;
import com.src.gameserver.templates.item.L2WeaponType;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.gameserver.util.Util;

public final class RequestWearItem extends L2GameClientPacket
{
	protected static final Logger _log = Logger.getLogger(RequestWearItem.class.getName());

	private static final String _C__C6_REQUESTWEARITEM = "[C] C6 RequestWearItem";

	@SuppressWarnings("unused")
	private int _unknow;

	private int _listId;
	private int _count;
	private int[] _items;

	protected Future<?> _removeWearItemsTask;
	protected L2PcInstance _activeChar;

	class RemoveWearItemsTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				_activeChar.destroyWearedItems("Wear", null, true);
			}
			catch(Throwable e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}

	@Override
	protected void readImpl()
	{
		_activeChar = getClient().getActiveChar();
		_unknow = readD();
		_listId = readD();
		_count = readD();

		if(_count < 0)
		{
			_count = 0;
		}

		if(_count > 100)
		{
			_count = 0;
		}

		_items = new int[_count];

		for(int i = 0; i < _count; i++)
		{
			int itemId = readD();
			_items[i] = itemId;
		}
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if(player == null)
			return;

		if(!Config.ALLOW_WEAR)
		{
			player.sendMessage("Item wear is disabled");
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false BuyList list_id.", Config.DEFAULT_PUNISH);
		}

		L2Weapon curwepe = player.getActiveWeaponItem();
		if(curwepe != null)
		{
			if((curwepe.getItemType() == L2WeaponType.DUAL))
			{
				player.sendMessage("Unequip your weapon and try again.");
				return;
			}
			else if((curwepe.getItemType() == L2WeaponType.BOW))
			{
				player.sendMessage("Unequip your weapon and try again.");
				return;
			}
			else if((curwepe.getItemType() == L2WeaponType.BIGBLUNT))
			{
				player.sendMessage("Unequip your weapon and try again.");
				return;
			}
			else if((curwepe.getItemType() == L2WeaponType.BIGSWORD))
			{
				player.sendMessage("Unequip your weapon and try again.");
				return;
			}
			else if((curwepe.getItemType() == L2WeaponType.POLE))
			{
				player.sendMessage("Unequip your weapon and try again.");
				return;
			}
			else if((curwepe.getItemType() == L2WeaponType.DUALFIST))
			{
				player.sendMessage("Unequip your weapon and try again.");
				return;
			}
			else if((curwepe.getItemType() == L2WeaponType.BLUNT))
			{
				player.sendMessage("Unequip your weapon and try again.");
				return;
			}
			else if((curwepe.getItemType() == L2WeaponType.SWORD))
			{
				player.sendMessage("Unequip your weapon and try again.");
				return;
			}
			else if((curwepe.getItemType() == L2WeaponType.DAGGER))
			{
				player.sendMessage("Unequip your weapon and try again.");
				return;
			}
		}

		if(!Config.KARMA_PLAYER_CAN_SHOP && player.getKarma() > 0)
			return;

		L2Object target = player.getTarget();
		if(!player.isGM() && (target == null || !(target instanceof L2MerchantInstance || target instanceof L2MercManagerInstance) || !player.isInsideRadius(target, L2Npc.INTERACTION_DISTANCE, false, false)))
			return;

		L2TradeList list = null;

		L2MerchantInstance merchant = target != null && target instanceof L2MerchantInstance ? (L2MerchantInstance) target : null;

		List<L2TradeList> lists = TradeController.getInstance().getBuyListByNpcId(merchant.getNpcId());

		if(lists == null)
		{
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false BuyList list_id.", Config.DEFAULT_PUNISH);
			return;
		}

		for(L2TradeList tradeList : lists)
		{
			if(tradeList.getListId() == _listId)
			{
				list = tradeList;
			}
		}

		if(list == null)
		{
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false BuyList list_id.", Config.DEFAULT_PUNISH);
			return;
		}

		_listId = list.getListId();

		if(_count < 1 || _listId >= 1000000)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		long totalPrice = 0;
		int slots = 0;
		int weight = 0;

		for(int i = 0; i < _count; i++)
		{
			int itemId = _items[i];

			if(!list.containsItemId(itemId))
			{
				Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false BuyList list_id.", Config.DEFAULT_PUNISH);
				return;
			}

			L2Item template = ItemTable.getInstance().getTemplate(itemId);
			weight += template.getWeight();
			slots++;

			totalPrice += Config.WEAR_PRICE;
			if(totalPrice > Integer.MAX_VALUE)
			{
				Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to purchase over " + Integer.MAX_VALUE + " adena worth of goods.", Config.DEFAULT_PUNISH);
				return;
			}
		}

		if(!player.getInventory().validateWeight(weight))
		{
			sendPacket(new SystemMessage(SystemMessageId.WEIGHT_LIMIT_EXCEEDED));
			return;
		}

		if(!player.getInventory().validateCapacity(slots))
		{
			sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
			return;
		}

		if(totalPrice < 0 || !player.reduceAdena("Wear", (int) totalPrice, player.getLastFolkNPC(), false))
		{
			sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			return;
		}

		InventoryUpdate playerIU = new InventoryUpdate();
		for(int i = 0; i < _count; i++)
		{
			int itemId = _items[i];

			if(!list.containsItemId(itemId))
			{
				Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false BuyList list_id.", Config.DEFAULT_PUNISH);
				return;
			}

			L2ItemInstance item = player.getInventory().addWearItem("Wear", itemId, player, merchant);

			player.getInventory().equipItemAndRecord(item);
			playerIU.addItem(item);
		}
		player.sendPacket(playerIU);

		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
		player.broadcastUserInfo();

		if(_removeWearItemsTask == null)
		{
			_removeWearItemsTask = ThreadPoolManager.getInstance().scheduleGeneral(new RemoveWearItemsTask(), Config.WEAR_DELAY * 1000);
		}
	}

	@Override
	public String getType()
	{
		return _C__C6_REQUESTWEARITEM;
	}
}