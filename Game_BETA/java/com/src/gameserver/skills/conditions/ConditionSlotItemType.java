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

public final class ConditionSlotItemType extends ConditionInventory
{
	private final int _mask;

	public ConditionSlotItemType(int slot, int mask)
	{
		super(slot);
		_mask = mask;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(!(env.player instanceof L2PcInstance))
		{
			return false;
		}

		Inventory inv = ((L2PcInstance) env.player).getInventory();
		L2ItemInstance item = inv.getPaperdollItem(_slot);
		if(item == null)
		{
			return false;
		}

		return (item.getItem().getItemMask() & _mask) != 0;
	}

}