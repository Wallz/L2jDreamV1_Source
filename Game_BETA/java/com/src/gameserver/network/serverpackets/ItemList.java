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

import java.util.logging.Logger;

import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.templates.item.L2Item;

public class ItemList extends L2GameServerPacket
{
	private static Logger _log = Logger.getLogger(ItemList.class.getName());
	private static final String _S__27_ITEMLIST = "[S] 1b ItemList";
	private L2ItemInstance[] _items;
	private boolean _showWindow;

	public ItemList(L2PcInstance cha, boolean showWindow)
	{
		_items = cha.getInventory().getItems();
		_showWindow = showWindow;
	}

	public ItemList(L2ItemInstance[] items, boolean showWindow)
	{
		_items = items;
		_showWindow = showWindow;
	}

	@SuppressWarnings("unused")
	private void showDebug()
	{
		for(L2ItemInstance temp : _items)
		{
			_log.fine("item:" + temp.getItem().getName() + " type1:" + temp.getItem().getType1() + " type2:" + temp.getItem().getType2());
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x1b);
		writeH(_showWindow ? 0x01 : 0x00);
		writeH(_items.length);
		
		L2Item item;
		for (L2ItemInstance temp : _items)
		{
			if (temp == null || temp.getItem() == null)
				continue;
			
			item = temp.getItem();
			
			writeH(item.getType1());
			writeD(temp.getObjectId());
			writeD(temp.getItemId());
			writeD(temp.getCount());
			writeH(item.getType2());
			writeH(temp.getCustomType1());
			writeH(temp.isEquipped() ? 0x01 : 0x00);
			writeD(item.getBodyPart());
			writeH(temp.getEnchantLevel());
			writeH(temp.getCustomType2());
			writeD((temp.isAugmented()) ? temp.getAugmentation().getAugmentationId() : 0x00);
			writeD(temp.getMana());
		}
	}

	@Override
	public String getType()
	{
		return _S__27_ITEMLIST;
	}

}