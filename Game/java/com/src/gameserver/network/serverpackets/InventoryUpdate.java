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
import java.util.logging.Logger;

import javolution.util.FastList;

import com.src.gameserver.model.ItemInfo;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.templates.item.L2Item;

public class InventoryUpdate extends L2GameServerPacket
{
	private static Logger _log = Logger.getLogger(InventoryUpdate.class.getName());
	private static final String _S__37_INVENTORYUPDATE = "[S] 27 InventoryUpdate";

	private List<ItemInfo> _items;

	public InventoryUpdate()
	{
		_items = new FastList<ItemInfo>();
	}

	public InventoryUpdate(List<ItemInfo> items)
	{
		_items = items;
	}

	public void addItem(L2ItemInstance item)
	{
		if(item != null)
		{
			_items.add(new ItemInfo(item));
		}
	}

	public void addNewItem(L2ItemInstance item)
	{
		if(item != null)
		{
			_items.add(new ItemInfo(item, 1));
		}
	}

	public void addModifiedItem(L2ItemInstance item)
	{
		if(item != null)
		{
			_items.add(new ItemInfo(item, 2));
		}
	}

	public void addRemovedItem(L2ItemInstance item)
	{
		if(item != null)
		{
			_items.add(new ItemInfo(item, 3));
		}
	}

	public void addItems(List<L2ItemInstance> items)
	{
		if(items != null)
		{
			for(L2ItemInstance item : items)
				if(item != null)
				{
					_items.add(new ItemInfo(item));
				}
		}
	}

	@SuppressWarnings("unused")
	private void showDebug()
	{
		for(ItemInfo item : _items)
		{
			_log.fine("oid:" + Integer.toHexString(item.getObjectId()) + " item:" + item.getItem().getName() + " last change:" + item.getChange());
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x27);
		writeH(_items.size());
		
		L2Item item;
		for (ItemInfo temp : _items)
		{
			if (temp == null || temp.getItem() == null)
				continue;
			
			item = temp.getItem();
			
			writeH(temp.getChange());
			writeH(item.getType1());
			writeD(temp.getObjectId());
			writeD(item.getItemId());
			writeD(temp.getCount());
			writeH(item.getType2());
			writeH(temp.getCustomType1());
			writeH(temp.getEquipped());
			writeD(item.getBodyPart());
			writeH(temp.getEnchant());
			writeH(temp.getCustomType2());
			writeD(temp.getAugmetationBoni());
			writeD(temp.getMana());
		}
		_items.clear();
		_items = null;
	}

	@Override
	public String getType()
	{
		return _S__37_INVENTORYUPDATE;
	}

}