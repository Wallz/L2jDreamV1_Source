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
package com.src.gameserver.skills.conditions;

import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.itemcontainer.Inventory;
import com.src.gameserver.skills.Env;
import com.src.gameserver.templates.item.L2ArmorType;
import com.src.gameserver.templates.item.L2Item;

public final class ConditionUsingItemType extends Condition
{
	private final boolean _armor;
	private final int _mask;

	public ConditionUsingItemType(int mask)
	{
		_mask = mask;
		_armor = (_mask & (L2ArmorType.MAGIC.mask() | L2ArmorType.LIGHT.mask() | L2ArmorType.HEAVY.mask())) != 0;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(!(env.player instanceof L2PcInstance))
		{
			return false;
		}

		Inventory inv = ((L2PcInstance) env.player).getInventory();
		
		 if (_armor)
         {
			 
                 L2ItemInstance chest = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
                 if (chest == null)
                         return false;
                 int chestMask = chest.getItem().getItemMask();
                 
                 if ((_mask & chestMask) == 0)
                         return false;
                 
                 int chestBodyPart = chest.getItem().getBodyPart();
                 
                 if (chestBodyPart == L2Item.SLOT_FULL_ARMOR)
                         return true;
                 else
                 {
                         L2ItemInstance legs = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
                         if (legs == null)
                                 return false;
                         int legMask = legs.getItem().getItemMask();
                         
                         return (_mask & legMask) != 0;
                 }
         }

		return (_mask & inv.getWearedMask()) != 0;
	}

}