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

public class TradeOwnAdd extends L2GameServerPacket
{
	private static final String _S__30_TRADEOWNADD = "[S] 20 TradeOwnAdd";
	private TradeList.TradeItem _item;

	public TradeOwnAdd(TradeList.TradeItem item)
	{
		_item = item;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x20);

		writeH(1);

		writeH(_item.getItem().getType1());
		writeD(_item.getObjectId());
		writeD(_item.getItem().getItemId());
		writeD(_item.getCount());
		writeH(_item.getItem().getType2());
		writeH(0x00);

		writeD(_item.getItem().getBodyPart());
		writeH(_item.getEnchant());
		writeH(0x00);
		writeH(0x00);
	}

	@Override
	public String getType()
	{
		return _S__30_TRADEOWNADD;
	}

}