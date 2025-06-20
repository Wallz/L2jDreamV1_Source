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

public final class L2PledgeSkillLearn
{
	private final int _id;
	private final int _level;
	private final String _name;
	private final int _repCost;
	private final int _baseLvl;
	private final int _itemId;

	public L2PledgeSkillLearn(int id, int lvl, int baseLvl, String name, int cost, int itemId)
	{
		_id = id;
		_level = lvl;
		_baseLvl = baseLvl;
		_name = name.intern();
		_repCost = cost;
		_itemId = itemId;
	}

	public int getId()
	{
		return _id;
	}

	public int getLevel()
	{
		return _level;
	}

	public int getBaseLevel()
	{
		return _baseLvl;
	}

	public String getName()
	{
		return _name;
	}

	public int getRepCost()
	{
		return _repCost;
	}

	public int getItemId()
	{
		return _itemId;
	}

}