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
package com.src.gameserver.model;

import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.templates.item.L2Item;

public class ItemInfo
{
	private int _objectId;
	private L2Item _item;
	private int _enchant;
	private int _augmentation;
	private int _count;
	private int _price;
	private int _type1;
	private int _type2;
	private int _equipped;
	private int _change;
	private int _mana;

	public ItemInfo(L2ItemInstance item)
	{
		if(item == null)
		{
			return;
		}

		_objectId = item.getObjectId();
		_item = item.getItem();
		_enchant = item.getEnchantLevel();

		if(item.isAugmented())
		{
			_augmentation = item.getAugmentation().getAugmentationId();
		}
		else
		{
			_augmentation = 0;
		}

		_count = item.getCount();
		_type1 = item.getCustomType1();
		_type2 = item.getCustomType2();
		_equipped = item.isEquipped() ? 1 : 0;

		switch(item.getLastChange())
		{
			case L2ItemInstance.ADDED:
			{
				_change = 1;
				break;
			}
			case L2ItemInstance.MODIFIED:
			{
				_change = 2;
				break;
			}
			case L2ItemInstance.REMOVED:
			{
				_change = 3;
				break;
			}
		}

		_mana = item.getMana();
	}

	public ItemInfo(L2ItemInstance item, int change)
	{
		if(item == null)
		{
			return;
		}

		_objectId = item.getObjectId();
		_item = item.getItem();
		_enchant = item.getEnchantLevel();

		if(item.isAugmented())
		{
			_augmentation = item.getAugmentation().getAugmentationId();
		}
		else
		{
			_augmentation = 0;
		}

		_count = item.getCount();
		_type1 = item.getCustomType1();
		_type2 = item.getCustomType2();
		_equipped = item.isEquipped() ? 1 : 0;
		_change = change;
		_mana = item.getMana();
	}

	public int getObjectId()
	{
		return _objectId;
	}

	public L2Item getItem()
	{
		return _item;
	}

	public int getEnchant()
	{
		return _enchant;
	}

	public int getAugmetationBoni()
	{
		return _augmentation;
	}

	public int getCount()
	{
		return _count;
	}

	public int getPrice()
	{
		return _price;
	}

	public int getCustomType1()
	{
		return _type1;
	}

	public int getCustomType2()
	{
		return _type2;
	}

	public int getEquipped()
	{
		return _equipped;
	}

	public int getChange()
	{
		return _change;
	}

	public int getMana()
	{
		return _mana;
	}

}