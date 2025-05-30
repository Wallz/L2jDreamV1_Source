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
package com.src.gameserver.model.itemcontainer;

import java.util.List;

import javolution.util.FastList;

import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance.ItemLocation;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class PcFreight extends ItemContainer
{
	private L2PcInstance _owner;
	private int _activeLocationId;

	public PcFreight(L2PcInstance owner)
	{
		_owner = owner;
	}

	@Override
	public L2PcInstance getOwner()
	{
		return _owner;
	}

	@Override
	public ItemLocation getBaseLocation()
	{
		return ItemLocation.FREIGHT;
	}

	public void setActiveLocation(int locationId)
	{
		_activeLocationId = locationId;
	}

	public int getactiveLocation()
	{
		return _activeLocationId;
	}

	@Override
	public int getSize()
	{
		int size = 0;

		for(L2ItemInstance item : _items)
		{
			if(item.getEquipSlot() == 0 || _activeLocationId == 0 || item.getEquipSlot() == _activeLocationId)
			{
				size++;
			}
		}

		return size;
	}

	@Override
	public L2ItemInstance[] getItems()
	{
		List<L2ItemInstance> list = new FastList<L2ItemInstance>();

		for(L2ItemInstance item : _items)
		{
			if(item.getEquipSlot() == 0 || item.getEquipSlot() == _activeLocationId)
			{
				list.add(item);
			}
		}

		return list.toArray(new L2ItemInstance[list.size()]);
	}

	@Override
	public L2ItemInstance getItemByItemId(int itemId)
	{
		for(L2ItemInstance item : _items)
		{
			if(item.getItemId() == itemId && (item.getEquipSlot() == 0 || _activeLocationId == 0 || item.getEquipSlot() == _activeLocationId))
			{
				return item;
			}
		}

		return null;
	}

	@Override
	protected void addItem(L2ItemInstance item)
	{
		super.addItem(item);
		if(_activeLocationId > 0)
		{
			item.setLocation(item.getLocation(), _activeLocationId);
		}
	}

	@Override
	public void restore()
	{
		int locationId = _activeLocationId;
		_activeLocationId = 0;
		super.restore();
		_activeLocationId = locationId;
	}

	@Override
	public boolean validateCapacity(int slots)
	{
		return getSize() + slots <= _owner.GetFreightLimit();
	}
}