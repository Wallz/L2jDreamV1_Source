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

public class AccessLevel
{
	private int _accessLevel = 0;
	private String _name = null;
	private int _nameColor = 0;
	private int _titleColor = 0;
	private boolean _isGm = false;
	private boolean _allowPeaceAttack = false;
	private boolean _allowFixedRes = false;
	private boolean _allowTransaction = false;
	private boolean _allowAltG = false;
	private boolean _giveDamage = false;
	private boolean _takeAggro = false;
	private boolean _gainExp = false;
	private boolean _useNameColor = true;
	private boolean _useTitleColor = false;
	private boolean _canDisableGmStatus = false;

	public AccessLevel(int accessLevel, String name, int nameColor, int titleColor, boolean isGm, boolean allowPeaceAttack, boolean allowFixedRes, boolean allowTransaction, boolean allowAltG, boolean giveDamage, boolean takeAggro, boolean gainExp, boolean useNameColor, boolean useTitleColor, boolean canDisableGmStatus)
	{
		_accessLevel = accessLevel;
		_name = name;
		_nameColor = nameColor;
		_titleColor = titleColor;
		_isGm = isGm;
		_allowPeaceAttack = allowPeaceAttack;
		_allowFixedRes = allowFixedRes;
		_allowTransaction = allowTransaction;
		_allowAltG = allowAltG;
		_giveDamage = giveDamage;
		_takeAggro = takeAggro;
		_gainExp = gainExp;
		_useNameColor = useNameColor;
		_useTitleColor = useTitleColor;
		_canDisableGmStatus = canDisableGmStatus;
	}

	public int getLevel()
	{
		return _accessLevel;
	}

	public String getName()
	{
		return _name;
	}

	public int getNameColor()
	{
		return _nameColor;
	}

	public int getTitleColor()
	{
		return _titleColor;
	}

	public boolean isGm()
	{
		return _isGm;
	}

	public boolean allowPeaceAttack()
	{
		return _allowPeaceAttack;
	}

	public boolean allowFixedRes()
	{
		return _allowFixedRes;
	}

	public boolean allowTransaction()
	{
		return _allowTransaction;
	}

	public boolean allowAltG()
	{
		return _allowAltG;
	}

	public boolean canGiveDamage()
	{
		return _giveDamage;
	}

	public boolean canTakeAggro()
	{
		return _takeAggro;
	}

	public boolean canGainExp()
	{
		return _gainExp;
	}

	public boolean useNameColor()
	{
		return _useNameColor;
	}

	public boolean useTitleColor()
	{
		return _useTitleColor;
	}

	public boolean canDisableGmStatus()
	{
		return _canDisableGmStatus;
	}
}