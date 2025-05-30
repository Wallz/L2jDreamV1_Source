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
package com.src.gameserver.network.serverpackets;

import com.src.Config;
import com.src.gameserver.model.TradeList;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class PrivateStoreManageListBuy extends L2GameServerPacket
{
	private static final String _S__D0_PRIVATESELLLISTBUY = "[S] b7 PrivateSellListBuy";
	private L2PcInstance _activeChar;
	private int _playerAdena;
	private L2ItemInstance[] _itemList;
	private TradeList.TradeItem[] _buyList;

	public PrivateStoreManageListBuy(L2PcInstance player)
	{
		_activeChar = player;

		if(Config.SELL_BY_ITEM)
		{
			_playerAdena = _activeChar.getItemCount(Config.SELL_ITEM, -1);
		}
		else
		{
			_playerAdena = _activeChar.getAdena();
		}

		_itemList = _activeChar.getInventory().getUniqueItems(false, true);
		_buyList = _activeChar.getBuyList().getItems();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xb7);
		writeD(_activeChar.getObjectId());
		writeD(_playerAdena);

		writeD(_itemList.length);
		for(L2ItemInstance item : _itemList)
		{
			writeD(item.getItemId());
			writeH(item.getEnchantLevel()); //show enchant lvl as 0, as you can't buy enchanted weapons
			writeD(item.getCount());
			writeD(item.getReferencePrice());
			writeH(0x00);
			writeD(item.getItem().getBodyPart());
			writeH(item.getItem().getType2());
		}

		writeD(_buyList.length);
		for(TradeList.TradeItem item : _buyList)
		{
			writeD(item.getItem().getItemId());
			writeH(item.getEnchant());
			writeD(item.getCount());
			writeD(item.getItem().getReferencePrice());
			writeH(0x00);
			writeD(item.getItem().getBodyPart());
			writeH(item.getItem().getType2());
			writeD(item.getPrice());
			writeD(item.getItem().getReferencePrice());
		}
	}

	@Override
	public String getType()
	{
		return _S__D0_PRIVATESELLLISTBUY;
	}

}