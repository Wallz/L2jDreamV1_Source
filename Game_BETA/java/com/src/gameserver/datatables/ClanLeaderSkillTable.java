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

public class ClanLeaderSkillTable
{
	private static ClanLeaderSkillTable _instance;
	private static L2Skill[] _leaderSkills;

	private ClanLeaderSkillTable()
	{
		_leaderSkills = new L2Skill[8];
		_leaderSkills[0] = SkillTable.getInstance().getInfo(246, 1);
	}

	public static ClanLeaderSkillTable getInstance()
	{
		if(_instance == null)
		{
			_instance = new ClanLeaderSkillTable();
		}

		return _instance;
	}

	public L2Skill[] GetClanLeaderSkills()
	{
		return _leaderSkills;
	}
}