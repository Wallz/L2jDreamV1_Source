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
package com.src.gameserver.templates.item;

public enum L2WeaponType
{
	NONE(1, "Shield"),
	SWORD(2, "Sword"),
	BLUNT(3, "Blunt"),
	DAGGER(4, "Dagger"),
	BOW(5, "Bow"),
	POLE(6, "Pole"),
	ETC(7, "Etc"),
	FIST(8, "Fist"),
	DUAL(9, "Dual Sword"),
	DUALFIST(10, "Dual Fist"),
	BIGSWORD(11, "Big Sword"),
	PET(12, "Pet"),
	ROD(13, "Rod"),
	BIGBLUNT(14, "Big Blunt");

	private final int _id;
	private final String _name;

	private L2WeaponType(int id, String name)
	{
		_id = id;
		_name = name;
	}

	public int mask()
	{
		return 1 << _id;
	}

	@Override
	public String toString()
	{
		return _name;
	}

}