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
package com.src.gameserver.templates.chars;

import java.util.List;

import javolution.util.FastList;

import com.src.gameserver.model.base.ClassId;
import com.src.gameserver.model.base.Race;
import com.src.gameserver.templates.StatsSet;

public class L2PcTemplate extends L2CharTemplate
{
	public final Race race;
	public final ClassId classId;

	public final int _currentCollisionRadius;
	public final int _currentCollisionHeight;
	public final String className;

	public final int spawnX;
	public final int spawnY;
	public final int spawnZ;

	public final int classBaseLevel;
	public final float lvlHpAdd;
	public final float lvlHpMod;
	public final float lvlCpAdd;
	public final float lvlCpMod;
	public final float lvlMpAdd;
	public final float lvlMpMod;

	private final List<PcTemplateItem> _items = new FastList<PcTemplateItem>();

	public L2PcTemplate(StatsSet set)
	{
		super(set);
		classId = ClassId.values()[set.getInteger("classId")];
		race = Race.values()[set.getInteger("raceId")];
		className = set.getString("className");
		_currentCollisionRadius = set.getInteger("collision_radius");
		_currentCollisionHeight = set.getInteger("collision_height");

		spawnX = set.getInteger("spawnX");
		spawnY = set.getInteger("spawnY");
		spawnZ = set.getInteger("spawnZ");

		classBaseLevel = set.getInteger("classBaseLevel");
		lvlHpAdd = set.getFloat("lvlHpAdd");
		lvlHpMod = set.getFloat("lvlHpMod");
		lvlCpAdd = set.getFloat("lvlCpAdd");
		lvlCpMod = set.getFloat("lvlCpMod");
		lvlMpAdd = set.getFloat("lvlMpAdd");
		lvlMpMod = set.getFloat("lvlMpMod");
	}

	public void addItem(int itemId, int amount, boolean equipped)
	{
		_items.add(new PcTemplateItem(itemId, amount, equipped));
	}

	public List<PcTemplateItem> getItems()
	{
		return _items;
	}

	@Override
	public int getCollisionRadius()
	{
		return _currentCollisionRadius;
	}

	@Override
	public int getCollisionHeight()
	{
		return _currentCollisionHeight;
	}

	public int getBaseFallSafeHeight(boolean female)
	{
		if(classId.getRace() == Race.darkelf || classId.getRace() == Race.elf)
		{
			return classId.isMage() ? (female ? 330 : 300) : female ? 380 : 350;
		}
		else if(classId.getRace() == Race.dwarf)
		{
			return female ? 200 : 180;
		}
		else if(classId.getRace() == Race.human)
		{
			return classId.isMage() ? (female ? 220 : 200) : female ? 270 : 250;
		}
		else if(classId.getRace() == Race.orc)
		{
			return classId.isMage() ? (female ? 280 : 250) : female ? 220 : 200;
		}

		return 400;
	}

	public final int getFallHeight()
	{
		return 333;
	}

	public static final class PcTemplateItem
	{
		private final int _itemId;
		private final int _amount;
		private final boolean _equipped;

		public PcTemplateItem(int itemId, int amount, boolean equipped)
		{
			_itemId = itemId;
			_amount = amount;
			_equipped = equipped;
		}

		public int getItemId()
		{
			return _itemId;
		}

		public int getAmount()
		{
			return _amount;
		}

		public boolean isEquipped()
		{
			return _equipped;
		}
	}

}