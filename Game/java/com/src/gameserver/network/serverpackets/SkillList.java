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

public class SkillList extends L2GameServerPacket
{
	private static final String _S__6D_SKILLLIST = "[S] 58 SkillList";
	private Skill[] _skills;

	class Skill
	{
		public int id;
		public int level;
		public boolean passive;

		Skill(int pId, int pLevel, boolean pPassive)
		{
			id = pId;
			level = pLevel;
			passive = pPassive;
		}
	}

	public SkillList()
	{
		_skills = new Skill[]
		{};
	}

	public void addSkill(int id, int level, boolean passive)
	{
		Skill sk = new Skill(id, level, passive);
		if(_skills == null || _skills.length == 0)
		{
			_skills = new Skill[]
			{
				sk
			};
		}
		else
		{
			Skill[] ns = new Skill[_skills.length + 1];

			boolean added = false;
			int i = 0;

			for(Skill s : _skills)
			{
				if(sk.id < s.id && !added)
				{
					ns[i] = sk;
					i++;
					ns[i] = s;
					i++;
					added = true;
				}
				else
				{
					ns[i] = s;
					i++;
				}
			}
			if(!added)
			{
				ns[i] = sk;
			}

			_skills = ns;
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x58);
		writeD(_skills.length);

		for(Skill temp : _skills)
		{
			writeD(temp.passive ? 1 : 0);
			writeD(temp.level);
			writeD(temp.id);
			writeC(0x00);
		}
	}

	@Override
	public String getType()
	{
		return _S__6D_SKILLLIST;
	}

}