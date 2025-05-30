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
package com.src.gameserver.templates.item;

import com.src.gameserver.model.actor.instance.L2ItemInstance;

public class L2WarehouseItem
{
	private L2Item _item;
	private int _object;
	private int _count;
	private int _owner;
	private int _enchant;
	private int _grade;
	private boolean _isAugmented;
	private int _augmentationId;

	public L2WarehouseItem(L2ItemInstance item)
	{
		_item = item.getItem();
		_object = item.getObjectId();
		_count = item.getCount();
		_owner = item.getOwnerId();
		_enchant = item.getEnchantLevel();
		_grade = item.getItem().getCrystalType();
		if(item.isAugmented())
		{
			_isAugmented = true;
			_augmentationId = item.getAugmentation().getAugmentationId();
		}
		else
		{
			_isAugmented = false;
		}
	}

	public L2Item getItem()
	{
		return _item;
	}

	public final int getObjectId()
	{
		return _object;
	}

	public final int getOwnerId()
	{
		return _owner;
	}

	public final int getCount()
	{
		return _count;
	}

	public final int getType1()
	{
		return _item.getType1();
	}

	public final int getType2()
	{
		return _item.getType2();
	}

	public final Enum<?> getItemType()
	{
		return _item.getItemType();
	}

	public final int getItemId()
	{
		return _item.getItemId();
	}

	public final int getBodyPart()
	{
		return _item.getBodyPart();
	}

	public final int getEnchantLevel()
	{
		return _enchant;
	}

	public final int getItemGrade()
	{
		return _grade;
	}

	public final boolean isWeapon()
	{
		return (_item instanceof L2Weapon);
	}

	public final boolean isArmor()
	{
		return (_item instanceof L2Armor);
	}

	public final boolean isEtcItem()
	{
		return (_item instanceof L2EtcItem);
	}

	public String getItemName()
	{
		return _item.getName();
	}

	public boolean isAugmented()
	{
		return _isAugmented;
	}

	public int getAugmentationId()
	{
		return _augmentationId;
	}

	public String getName()
	{
		return _item.getName();
	}

	@Override
	public String toString()
	{
		return _item.toString();
	}

}