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

import com.src.gameserver.templates.StatsSet;

public final class L2EtcItem extends L2Item
{
	
	private final int _sharedReuseGroup;
	
	public L2EtcItem(L2EtcItemType type, StatsSet set)
	{
		super(type, set);
		_sharedReuseGroup = set.getInteger("shared_reuse_group", -1);
	}

	@Override
	public L2EtcItemType getItemType()
	{
		return (L2EtcItemType) super._type;
	}

	@Override
	public final boolean isConsumable()
	{
		return getItemType() == L2EtcItemType.SHOT || getItemType() == L2EtcItemType.POTION;
	}

	@Override
	public int getItemMask()
	{
		return getItemType().mask();
	}

	public int getSharedReuseGroup()
	{
		return _sharedReuseGroup;
	}

}