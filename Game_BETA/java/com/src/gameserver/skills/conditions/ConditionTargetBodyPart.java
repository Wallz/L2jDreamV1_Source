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

import com.src.gameserver.model.itemcontainer.Inventory;
import com.src.gameserver.skills.Env;
import com.src.gameserver.templates.item.L2Armor;
import com.src.gameserver.templates.item.L2Item;

public class ConditionTargetBodyPart extends Condition
{
	private L2Armor _armor;

	public ConditionTargetBodyPart(L2Armor armor)
	{
		_armor = armor;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(env.target == null)
		{
			return true;
		}

		int bodypart = env.target.getAttackingBodyPart();
		int armor_part = _armor.getBodyPart();
		switch(bodypart)
		{
			case Inventory.PAPERDOLL_CHEST:
				return (armor_part & (L2Item.SLOT_CHEST | L2Item.SLOT_FULL_ARMOR | L2Item.SLOT_UNDERWEAR)) != 0;
			case Inventory.PAPERDOLL_LEGS:
				return (armor_part & (L2Item.SLOT_LEGS | L2Item.SLOT_FULL_ARMOR)) != 0;
			case Inventory.PAPERDOLL_HEAD:
				return (armor_part & L2Item.SLOT_HEAD) != 0;
			case Inventory.PAPERDOLL_FEET:
				return (armor_part & L2Item.SLOT_FEET) != 0;
			case Inventory.PAPERDOLL_GLOVES:
				return (armor_part & L2Item.SLOT_GLOVES) != 0;
			default:
				return true;
		}
	}

}