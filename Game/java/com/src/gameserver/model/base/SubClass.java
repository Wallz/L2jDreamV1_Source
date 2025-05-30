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
package com.src.gameserver.model.base;

public final class SubClass
{
	private PlayerClass _class;
	private long _exp = Experience.getExp(40);
	private int _sp = 0;
	private int _level = 40;
	private int _classIndex = 1;

	public SubClass(int classId, long exp, int sp, byte level, int classIndex)
	{
		_class = PlayerClass.values()[classId];
		_exp = exp;
		_sp = sp;
		_level = level;
		_classIndex = classIndex;
	}

	public SubClass(int classId, int classIndex)
	{
		_class = PlayerClass.values()[classId];
		_classIndex = classIndex;
	}

	public SubClass()
	{
	}

	public PlayerClass getClassDefinition()
	{
		return _class;
	}

	public int getClassId()
	{
		return _class.ordinal();
	}

	public long getExp()
	{
		return _exp;
	}

	public int getSp()
	{
		return _sp;
	}

	public int getLevel()
	{
		return _level;
	}

	public int getClassIndex()
	{
		return _classIndex;
	}

	public void setClassId(int classId)
	{
		_class = PlayerClass.values()[classId];
	}

	public void setExp(long expValue)
	{
		if(expValue > Experience.getExp(Experience.MAX_LEVEL))
		{
			expValue = Experience.getExp(Experience.MAX_LEVEL);
		}

		_exp = expValue;
	}

	public void setSp(int spValue)
	{
		_sp = spValue;
	}

	public void setClassIndex(int classIndex)
	{
		_classIndex = classIndex;
	}

	public void setLevel(int levelValue)
	{
		if(levelValue > Experience.MAX_LEVEL - 1)
		{
			levelValue = Experience.MAX_LEVEL - 1;
		}
		else if(levelValue < 40)
		{
			levelValue = 40;
		}

		_level = levelValue;
	}

	public void incLevel()
	{
		if(getLevel() == Experience.MAX_LEVEL - 1)
		{
			return;
		}

		_level++;
		setExp(Experience.getExp(getLevel()));
	}

	public void decLevel()
	{
		if(getLevel() == 40)
		{
			return;
		}

		_level--;
		setExp(Experience.getExp(getLevel()));
	}

}