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

import java.util.List;

import javolution.util.FastList;

import com.src.gameserver.model.L2TradeList;
import com.src.gameserver.model.actor.instance.L2ItemInstance;

public final class BuyListSeed extends L2GameServerPacket
{
	private static final String _S__E8_BUYLISTSEED = "[S] E8 BuyListSeed";

	private int _manorId;
	private List<L2ItemInstance> _list = new FastList<L2ItemInstance>();
	private int _money;

	public BuyListSeed(L2TradeList list, int manorId, int currentMoney)
	{
		_money = currentMoney;
		_manorId = manorId;
		_list = list.getItems();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xE8);

		writeD(_money);
		writeD(_manorId);

		writeH(_list.size());

		for(L2ItemInstance item : _list)
		{
			writeH(0x04);
			writeD(0x00);
			writeD(item.getItemId());
			writeD(item.getCount());
			writeH(0x04);
			writeH(0x00);
			writeD(item.getPriceToSell());
		}
	}

	@Override
	public String getType()
	{
		return _S__E8_BUYLISTSEED;
	}

}