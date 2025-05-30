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

import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance.ItemLocation;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.templates.item.L2EtcItemType;

public class PetInventory extends Inventory
{
	private final L2PetInstance _owner;

	public PetInventory(L2PetInstance owner)
	{
		_owner = owner;
	}

	@Override
	public L2PetInstance getOwner()
	{
		return _owner;
	}

	public boolean validateCapacity(L2ItemInstance item)
	{
		int slots = 0;

		if(!(item.isStackable() && getItemByItemId(item.getItemId()) != null) && item.getItemType() != L2EtcItemType.HERB)
		{
			slots++;
		}

		return validateCapacity(slots);
	}
	
	@Override
	public boolean validateWeight(int weight)
	{
		if (getOwner() == null)
			return false;
		return (_totalWeight + weight <= getOwner().getMaxLoad());
	}

	@Override
	public boolean validateCapacity(int slots)
	{
		return (_items.size() + slots <= _owner.getInventoryLimit());
	}

	@Override
	protected ItemLocation getBaseLocation()
	{
		return ItemLocation.PET;
	}

	@Override
	public int getOwnerId()
	{
		int id;
		try
		{
			id = _owner.getOwner().getObjectId();
		}
		catch(NullPointerException e)
		{
			return 0;
		}
		return id;
	}

	@Override
	protected ItemLocation getEquipLocation()
	{
		return ItemLocation.PET_EQUIP;
	}
}