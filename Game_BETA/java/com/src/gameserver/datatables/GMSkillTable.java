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

import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class GMSkillTable
{
	private static final L2Skill[] _gmSkills = new L2Skill[24];
	private static final int[] _gmSkillsId =
	{
		7041, 7042, 7043, 7044, 7045, 7046, 7047, 7048, 7049, 7050, 7051, 7052,
		7053, 7054, 7055, 7056, 7057, 7058, 7059, 7060, 7061, 7062, 7063, 7064
	};

	private GMSkillTable()
	{
		for(int i = 0; i < _gmSkillsId.length; i++)
		{
			_gmSkills[i] = SkillTable.getInstance().getInfo(_gmSkillsId[i], 1);
		}
	}

	public static GMSkillTable getInstance()
	{
		return SingletonHolder._instance;
	}

	public static L2Skill[] getGMSkills()
	{
		return _gmSkills;
	}

	public static boolean isGMSkill(int skillid)
	{
		for(int id : _gmSkillsId)
		{
			if(id == skillid)
			{
				return true;
			}
		}

		return false;
	}

	public void addSkills(L2PcInstance gmchar)
	{
		for(L2Skill s : getGMSkills())
		{
			gmchar.addSkill(s, false);
		}
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final GMSkillTable _instance = new GMSkillTable();
	}
}