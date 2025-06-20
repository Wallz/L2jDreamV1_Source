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
package com.src.gameserver.datatables;

import java.util.Map;

import javolution.util.FastMap;

import com.src.gameserver.model.L2Skill;
import com.src.gameserver.skills.SkillsEngine;
import com.src.gameserver.templates.item.L2WeaponType;

public class SkillTable
{
	private static SkillTable _instance;

	private Map<Integer, L2Skill> _skills;
	private boolean _initialized = true;

	public static SkillTable getInstance()
	{
		if(_instance == null)
		{
			_instance = new SkillTable();
		}

		return _instance;
	}

	private SkillTable()
	{
		_skills = new FastMap<Integer, L2Skill>();
		SkillsEngine.getInstance().loadAllSkills(_skills);
	}

	public void reload()
	{
		_instance = new SkillTable();
	}

	public boolean isInitialized()
	{
		return _initialized;
	}

	public static int getSkillHashCode(L2Skill skill)
	{
		return SkillTable.getSkillHashCode(skill.getId(), skill.getLevel());
	}

	public static int getSkillHashCode(int skillId, int skillLevel)
	{
		return skillId * 256 + skillLevel;
	}

	public L2Skill getInfo(int skillId, int level)
	{
		return _skills.get(SkillTable.getSkillHashCode(skillId, level));
	}

	public int getMaxLevel(int magicId, int level)
	{
		L2Skill temp;

		while(level < 100)
		{
			level++;
			temp = _skills.get(SkillTable.getSkillHashCode(magicId, level));

			if(temp == null)
			{
				return level - 1;
			}
		}
		return level;
	}

	private static final L2WeaponType[] weaponDbMasks =
	{
		L2WeaponType.ETC,
		L2WeaponType.BOW,
		L2WeaponType.POLE,
		L2WeaponType.DUALFIST,
		L2WeaponType.DUAL,
		L2WeaponType.BLUNT,
		L2WeaponType.SWORD,
		L2WeaponType.DAGGER,
		L2WeaponType.BIGSWORD,
		L2WeaponType.ROD,
		L2WeaponType.BIGBLUNT
	};
	
	public int calcWeaponsAllowed(int mask)
	{
		if(mask == 0)
		{
			return 0;
		}

		int weaponsAllowed = 0;

		for(int i = 0; i < weaponDbMasks.length; i++)
		{
			if((mask & 1 << i) != 0)
			{
				weaponsAllowed |= weaponDbMasks[i].mask();
			}
		}

		return weaponsAllowed;
	}
}