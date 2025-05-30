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
package com.src.gameserver.network.serverpackets;

import java.util.List;

import javolution.util.FastList;

public class AquireSkillList extends L2GameServerPacket
{
	private static final String _S__A3_AQUIRESKILLLIST = "[S] 8a AquireSkillList";

	private List<Skill> _skills;
	private skillType _fishingSkills;

	public enum skillType
	{
		Usual,
		Fishing,
		Clan
	}

	private class Skill
	{
		public int id;
		public int nextLevel;
		public int maxLevel;
		public int spCost;
		public int requirements;

		public Skill(int pId, int pNextLevel, int pMaxLevel, int pSpCost, int pRequirements)
		{
			id = pId;
			nextLevel = pNextLevel;
			maxLevel = pMaxLevel;
			spCost = pSpCost;
			requirements = pRequirements;
		}
	}

	public AquireSkillList(skillType type)
	{
		_skills = new FastList<Skill>();
		_fishingSkills = type;
	}

	public void addSkill(int id, int nextLevel, int maxLevel, int spCost, int requirements)
	{
		_skills.add(new Skill(id, nextLevel, maxLevel, spCost, requirements));
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x8a);
		writeD(_fishingSkills.ordinal());
		writeD(_skills.size());

		for(Skill temp : _skills)
		{
			writeD(temp.id);
			writeD(temp.nextLevel);
			writeD(temp.maxLevel);
			writeD(temp.spCost);
			writeD(temp.requirements);
		}
	}

	@Override
	public String getType()
	{
		return _S__A3_AQUIRESKILLLIST;
	}

}