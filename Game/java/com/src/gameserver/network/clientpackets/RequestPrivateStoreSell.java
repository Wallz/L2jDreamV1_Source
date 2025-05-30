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
import com.src.gameserver.model.ItemRequest;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.TradeList;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.util.Util;

public final class RequestPrivateStoreSell extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestPrivateStoreSell.class.getName());

	private static final String _C__96_REQUESTPRIVATESTORESELL = "[C] 96 RequestPrivateStoreSell";

	private int _storePlayerId;
	private int _count;
	private int _price;
	private ItemRequest[] _items;

	@Override
	protected void readImpl()
	{
		_storePlayerId = readD();
		_count = readD();

		if(_count < 0 || _count * 20 > _buf.remaining() || _count > Config.MAX_ITEM_IN_PACKET)
		{
			_count = 0;
		}
		_items = new ItemRequest[_count];

		long priceTotal = 0;
		for(int i = 0; i < _count; i++)
		{
			int objectId = readD();
			int itemId = readD();
			readH();
			readH();
			long count = readD();
			int price = readD();

			if(count > Integer.MAX_VALUE || count < 0)
			{
				String msgErr = "[RequestPrivateStoreSell] player " + getClient().getActiveChar().getName() + " tried an overflow exploit, ban this player!";
				Util.handleIllegalPlayerAction(getClient().getActiveChar(), msgErr, Config.DEFAULT_PUNISH);
				_count = 0;
				_items = null;
				return;
			}
			_items[i] = new ItemRequest(objectId, itemId, (int) count, price);
			priceTotal += price * count;
		}

		if(priceTotal < 0 || priceTotal > Integer.MAX_VALUE)
		{
			String msgErr = "[RequestPrivateStoreSell] player " + getClient().getActiveChar().getName() + " tried an overflow exploit, ban this player!";
			Util.handleIllegalPlayerAction(getClient().getActiveChar(), msgErr, Config.DEFAULT_PUNISH);
			_count = 0;
			_items = null;
			return;
		}

		_price = (int) priceTotal;
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if(player == null)
		{
			return;
		}

		L2Object object = L2World.getInstance().findObject(_storePlayerId);
		if(object == null || !(object instanceof L2PcInstance))
		{
			return;
		}

		if(player.isCursedWeaponEquiped()) 
			return;
		
		L2PcInstance storePlayer = (L2PcInstance) object;
		if(storePlayer.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_BUY)
		{
			return;
		}

		TradeList storeList = storePlayer.getBuyList();
		if(storeList == null)
		{
			return;
		}

		if(!player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Unsufficient privileges.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(Config.SELL_BY_ITEM)
		{
			if(storePlayer.getItemCount(Config.SELL_ITEM, -1) < _price)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(SystemMessage.sendString("You have not enough items to buy, canceling PrivateBuy"));
				storePlayer.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
				storePlayer.broadcastUserInfo();
				return;
			}
		}
		else
		{
			if(storePlayer.getAdena() < _price)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				storePlayer.sendMessage("You have not enough adena, canceling PrivateBuy.");
				storePlayer.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
				storePlayer.broadcastUserInfo();
				return;
			}
		}

		if(!storeList.PrivateStoreSell(player, _items, _price))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			_log.warning("PrivateStore sell has failed due to invalid list or request. Player: " + player.getName() + ", Private store of: " + storePlayer.getName());
			return;
		}
		
		if(storeList.getItemCount() == 0)
		{
			storePlayer.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
			storePlayer.broadcastUserInfo();
		}
	}

	@Override
	public String getType()
	{
		return _C__96_REQUESTPRIVATESTORESELL;
	}

}