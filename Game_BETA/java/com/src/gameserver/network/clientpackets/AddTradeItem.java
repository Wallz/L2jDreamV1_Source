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

import com.src.gameserver.model.L2World;
import com.src.gameserver.model.TradeList;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.itemcontainer.ItemContainer;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.network.serverpackets.TradeOtherAdd;
import com.src.gameserver.network.serverpackets.TradeOwnAdd;
import com.src.gameserver.network.serverpackets.TradeUpdate;

public final class AddTradeItem extends L2GameClientPacket
{
	private static final String _C__16_ADDTRADEITEM = "[C] 16 AddTradeItem";
	private static Logger _log = Logger.getLogger(AddTradeItem.class.getName());

	private int _tradeId;
	private int _objectId;
	private int _count;

	public AddTradeItem()
	{}

	@Override
	protected void readImpl()
	{
		_tradeId = readD();
		_objectId = readD();
		_count = readD();
	}

	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();

		if(player == null)
		{
			return;
		}

		if(player.getInventory().getItemByObjectId(_objectId) == null || _count <= 0)
		{
			_log.warning("Character:" + player.getName() + " requested invalid trade object");
			return;
		}

		final TradeList trade = player.getActiveTradeList();

		if(trade == null)
		{
			_log.warning("Character: " + player.getName() + " requested item:" + _objectId + " add without active tradelist:" + _tradeId);
			return;
		}

		if(trade.getPartner() == null || L2World.getInstance().findObject(trade.getPartner().getObjectId()) == null)
		{
			if(trade.getPartner() != null)
			{
				_log.warning("Character:" + player.getName() + " requested invalid trade object: " + _objectId);
			}

			player.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME));
			player.cancelActiveTrade();
			return;
		}

		if(!player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Transactions are disable for your Access Level.");
			player.cancelActiveTrade();
			return;
		}

		if (trade.isConfirmed() || trade.getPartner().getActiveTradeList().isConfirmed()) 
	    {
	      player.sendPacket(new SystemMessage(SystemMessageId.MAY_NO_LONGER_ADJUST_ITEMS_BECAUSE_TRADE_CONFIRMED));
	      return;
	    }
		
		L2ItemInstance _tmpitem = ItemContainer.getItemByObjectId(_objectId, player.getInventory());
		if (_tmpitem == null) 
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
			return;
		}
		
		if(!player.validateItemManipulation(_objectId, "trade"))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
			return;
		}
		
		final TradeList.TradeItem item = trade.addItem(_objectId, _count);

		if(item == null)
		{
			return;
		}
		if(item.isAugmented())
		{
			return;
		}
		
		if(item != null)
		{
			player.sendPacket(new TradeOwnAdd(item));
			player.sendPacket(new TradeUpdate(trade, player));
			trade.getPartner().sendPacket(new TradeOtherAdd(item));
		}
	}

	@Override
	public String getType()
	{
		return _C__16_ADDTRADEITEM;
	}

}