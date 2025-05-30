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

import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class TradeStart extends L2GameServerPacket
{
	private static final String _S__2E_TRADESTART = "[S] 1E TradeStart";
	private L2PcInstance _activeChar;
	private L2ItemInstance[] _itemList;

	public TradeStart(L2PcInstance player)
	{
		_activeChar = player;
		_itemList = _activeChar.getInventory().getAvailableItems(true);
	}

	@Override
	protected final void writeImpl()
	{
		if(_activeChar.getActiveTradeList() == null || _activeChar.getActiveTradeList().getPartner() == null)
		{
			return;
		}

		writeC(0x1E);
		writeD(_activeChar.getActiveTradeList().getPartner().getObjectId());

		writeH(_itemList.length);
		for(L2ItemInstance item : _itemList)
		{
			writeH(item.getItem().getType1());
			writeD(item.getObjectId());
			writeD(item.getItemId());
			writeD(item.getCount());
			writeH(item.getItem().getType2());
			writeH(0x00);

			writeD(item.getItem().getBodyPart());
			writeH(item.getEnchantLevel());
			writeH(0x00);
			writeH(0x00);
		}
	}


	@Override
	public String getType()
	{
		return _S__2E_TRADESTART;
	}

}