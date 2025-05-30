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

import com.src.Config;
import com.src.gameserver.cache.HtmCache;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2FishermanInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2MerchantInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.util.Util;

public final class RequestSellItem extends L2GameClientPacket
{
	private static final String _C__1E_REQUESTSELLITEM = "[C] 1E RequestSellItem";

	private int _listId;
	private int _count;
	private int[] _items;

	@Override
	protected void readImpl()
	{
		_listId = readD();
		_count = readD();

		if(_count <= 0 || _count * 12 > _buf.remaining() || _count > Config.MAX_ITEM_IN_PACKET)
		{
			_count = 0;
			_items = null;
			return;
		}

		_items = new int[_count * 3];

		for(int i = 0; i < _count; i++)
		{
			int objectId = readD();
			_items[i * 3 + 0] = objectId;
			int itemId = readD();
			_items[i * 3 + 1] = itemId;
			long cnt = readD();

			if(cnt > Integer.MAX_VALUE || cnt <= 0)
			{
				_count = 0;
				_items = null;
				return;
			}
			_items[i * 3 + 2] = (int) cnt;
		}
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();

		if(player == null)
		{
			return;
		}

		if(!Config.KARMA_PLAYER_CAN_SHOP && player.getKarma() > 0)
		{
			return;
		}

		L2Object target = player.getTarget();
		if(!player.isGM() && (target == null || !(target instanceof L2MerchantInstance) || !player.isInsideRadius(target, L2Npc.INTERACTION_DISTANCE, false, false)))
		{
			return;
		}

		boolean ok = true;
		String htmlFolder = "";

		if(target != null)
		{
			if(target instanceof L2MerchantInstance)
			{
				htmlFolder = "merchant";
			}
			else if(target instanceof L2FishermanInstance)
			{
				htmlFolder = "fisherman";
			}
			else
			{
				ok = false;
			}
		}
		else
		{
			ok = false;
		}

		L2Npc merchant = null;

		if(ok)
		{
			merchant = (L2Npc) target;
		}

		if(merchant != null && _listId > 1000000)
		{
			if(merchant.getTemplate().npcId != _listId - 1000000)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}

		long totalPrice = 0;

		for(int i = 0; i < _count; i++)
		{
			int objectId = _items[i * 3 + 0];
			@SuppressWarnings("unused")
			int itemId = _items[i * 3 + 1];
			int count = _items[i * 3 + 2];

			if(count <= 0 || count > Integer.MAX_VALUE)
			{
				sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
				return;
			}

			L2ItemInstance item = player.checkItemManipulation(objectId, count, "sell");

			if(item == null || !item.getItem().isSellable())
			{
				continue;
			}

			long price = item.getReferencePrice() / 2;
			totalPrice += price * count;

			if((Integer.MAX_VALUE / count) < price || totalPrice > Integer.MAX_VALUE)
			{
				sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
				return;
			}

			if(totalPrice <= 0)
			{
				sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
				return;
			}

			item = player.getInventory().destroyItem("Sell", objectId, count, player, null);
		}
		
		if (Util.calculateDistance(player, merchant, true) > 150) 
		{
			if (player.isGM())
			{
				player.addAdena("Sell", (int) totalPrice, merchant, false);
				sendPacket(new SystemMessage(SystemMessageId.SUCCESSFULLY_TRADED_WITH_NPC));
			}
			else
			{
				player.cancelActiveTrade();
				return;
			}
		}
		else
		{
			player.addAdena("Sell", (int) totalPrice, merchant, false);
			player.sendPacket(new SystemMessage(SystemMessageId.SUCCESSFULLY_TRADED_WITH_NPC));
		}

		String html = HtmCache.getInstance().getHtm("data/html/" + htmlFolder + "/" + merchant.getNpcId() + "-sold.htm");

		if(html != null)
		{
			NpcHtmlMessage soldMsg = new NpcHtmlMessage(merchant.getObjectId());
			soldMsg.setHtml(html.replaceAll("%objectId%", String.valueOf(merchant.getObjectId())));
			player.sendPacket(soldMsg);
		}

		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
		player.sendPacket(new ItemList(player, true));
	}

	@Override
	public String getType()
	{
		return _C__1E_REQUESTSELLITEM;
	}

}