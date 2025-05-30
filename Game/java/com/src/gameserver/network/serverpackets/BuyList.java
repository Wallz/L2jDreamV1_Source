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

import com.src.Config;
import com.src.gameserver.model.L2TradeList;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.templates.item.L2Item;

public final class BuyList extends L2GameServerPacket
{
	private static final String _S__1D_BUYLIST = "[S] 11 BuyList";

	private int _listId;
	private L2ItemInstance[] _list;
	private int _money;
	private double _taxRate = 0;

	public BuyList(L2TradeList list, int currentMoney)
	{
		_listId = list.getListId();
		List<L2ItemInstance> lst = list.getItems();
		_list = lst.toArray(new L2ItemInstance[lst.size()]);
		_money = currentMoney;
	}

	public BuyList(L2TradeList list, int currentMoney, double taxRate)
	{
		_listId = list.getListId();
		List<L2ItemInstance> lst = list.getItems();
		_list = lst.toArray(new L2ItemInstance[lst.size()]);
		_money = currentMoney;
		_taxRate = taxRate;
	}

	public BuyList(List<L2ItemInstance> lst, int listId, int currentMoney)
	{
		_listId = listId;
		_list = lst.toArray(new L2ItemInstance[lst.size()]);
		_money = currentMoney;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x11);
		writeD(_money);
		writeD(_listId);

		writeH(_list.length);

		for(L2ItemInstance item : _list)
		{
			if(item.getCount() > 0 || item.getCount() == -1)
			{
				writeH(item.getItem().getType1());
				writeD(item.getObjectId());
				writeD(item.getItemId());
				if(item.getCount() < 0)
				{
					writeD(0x00);
				}
				else
				{
					writeD(item.getCount());
				}
				writeH(item.getItem().getType2());
				writeH(0x00);

				if(item.getItem().getType1() != L2Item.TYPE1_ITEM_QUESTITEM_ADENA)
				{
					writeD(item.getItem().getBodyPart());
					writeH(item.getEnchantLevel());
					writeH(0x00);
					writeH(0x00);
				}
				else
				{
					writeD(0x00);
					writeH(0x00);
					writeH(0x00);
					writeH(0x00);
				}

				if(item.getItemId() >= 3960 && item.getItemId() <= 4026)
				{
					writeD((int) (item.getPriceToSell() * Config.RATE_SIEGE_GUARDS_PRICE * (1 + _taxRate)));
				}
				else
				{
					writeD((int) (item.getPriceToSell() * (1 + _taxRate)));
				}
			}
		}
	}

	@Override
	public String getType()
	{
		return _S__1D_BUYLIST;
	}

}