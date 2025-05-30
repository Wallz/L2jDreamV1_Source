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

public class L2LvlupData
{
	private int _classid;
	private int _classLvl;
	private float _classHpAdd;
	private float _classHpBase;
	private float _classHpModifier;
	private float _classCpAdd;
	private float _classCpBase;
	private float _classCpModifier;
	private float _classMpAdd;
	private float _classMpBase;
	private float _classMpModifier;

	@Deprecated
	public float getClassHpAdd()
	{
		return _classHpAdd;
	}

	public void setClassHpAdd(float hpAdd)
	{
		_classHpAdd = hpAdd;
	}

	@Deprecated
	public float getClassHpBase()
	{
		return _classHpBase;
	}

	public void setClassHpBase(float hpBase)
	{
		_classHpBase = hpBase;
	}

	@Deprecated
	public float getClassHpModifier()
	{
		return _classHpModifier;
	}

	public void setClassHpModifier(float hpModifier)
	{
		_classHpModifier = hpModifier;
	}

	@Deprecated
	public float getClassCpAdd()
	{
		return _classCpAdd;
	}

	public void setClassCpAdd(float cpAdd)
	{
		_classCpAdd = cpAdd;
	}

	@Deprecated
	public float getClassCpBase()
	{
		return _classCpBase;
	}

	public void setClassCpBase(float cpBase)
	{
		_classCpBase = cpBase;
	}

	@Deprecated
	public float getClassCpModifier()
	{
		return _classCpModifier;
	}

	public void setClassCpModifier(float cpModifier)
	{
		_classCpModifier = cpModifier;
	}

	public int getClassid()
	{
		return _classid;
	}

	public void setClassid(int pClassid)
	{
		_classid = pClassid;
	}

	@Deprecated
	public int getClassLvl()
	{
		return _classLvl;
	}

	public void setClassLvl(int lvl)
	{
		_classLvl = lvl;
	}

	@Deprecated
	public float getClassMpAdd()
	{
		return _classMpAdd;
	}

	public void setClassMpAdd(float mpAdd)
	{
		_classMpAdd = mpAdd;
	}

	@Deprecated
	public float getClassMpBase()
	{
		return _classMpBase;
	}

	public void setClassMpBase(float mpBase)
	{
		_classMpBase = mpBase;
	}

	@Deprecated
	public float getClassMpModifier()
	{
		return _classMpModifier;
	}

	public void setClassMpModifier(float mpModifier)
	{
		_classMpModifier = mpModifier;
	}
}