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
package com.src.gameserver.geo.pathfinding.cellnodes;

import com.src.gameserver.geo.pathfinding.Node;
import com.src.gameserver.model.L2World;

public final class CellNode extends Node
{
	private final int _x;
	private final int _y;
	private short _z;

	public CellNode(int x, int y, short z, int neighborsIdx)
	{
		super(neighborsIdx);

		_x = x;
		_y = y;
		_z = z;
	}

	@Override
	public int getX()
	{
		return (_x << 4) + L2World.MAP_MIN_X;
	}

	@Override
	public int getY()
	{
		return (_y << 4) + L2World.MAP_MIN_Y;
	}

	@Override
	public short getZ()
	{
		return _z;
	}

	@Override
	public void setZ(short z)
	{
		_z = z;
	}

	@Override
	public int getNodeX()
	{
		return _x;
	}

	@Override
	public int getNodeY()
	{
		return _y;
	}

}