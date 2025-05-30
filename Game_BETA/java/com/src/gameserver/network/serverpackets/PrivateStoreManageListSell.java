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

import com.src.gameserver.model.TradeList;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class PrivateStoreManageListSell extends L2GameServerPacket
{
	private static final String _S__B3_PRIVATESELLLISTSELL = "[S] 9a PrivateSellListSell";
	private L2PcInstance _activeChar;
	private int _playerAdena;
	private boolean _packageSale;
	private TradeList.TradeItem[] _itemList;
	private TradeList.TradeItem[] _sellList;

	public PrivateStoreManageListSell(L2PcInstance player)
	{
		_activeChar = player;
		_playerAdena = _activeChar.getAdena();
		_activeChar.getSellList().updateItems();
		_packageSale = _activeChar.getSellList().isPackaged();
		_itemList = _activeChar.getInventory().getAvailableItems(_activeChar.getSellList());
		_sellList = _activeChar.getSellList().getItems();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x9a);
		writeD(_activeChar.getObjectId());
		writeD(_packageSale ? 1 : 0);
		writeD(_playerAdena);

		writeD(_itemList.length);
		for(TradeList.TradeItem item : _itemList)
		{
			writeD(item.getItem().getType2());
			writeD(item.getObjectId());
			writeD(item.getItem().getItemId());
			writeD(item.getCount());
			writeH(0);
			writeH(item.getEnchant());
			writeH(0);
			writeD(item.getItem().getBodyPart());
			writeD(item.getPrice());
		}

		writeD(_sellList.length);
		for(TradeList.TradeItem item : _sellList)
		{
			writeD(item.getItem().getType2());
			writeD(item.getObjectId());
			writeD(item.getItem().getItemId());
			writeD(item.getCount());
			writeH(0);
			writeH(item.getEnchant());
			writeH(0x00);
			writeD(item.getItem().getBodyPart());
			writeD(item.getPrice());
			writeD(item.getItem().getReferencePrice());
		}
	}

	@Override
	public String getType()
	{
		return _S__B3_PRIVATESELLLISTSELL;
	}

}