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

import com.src.gameserver.geo.GeoData;
import com.src.gameserver.geo.pathfinding.Node;
import com.src.gameserver.geo.pathfinding.PathFinding;
import com.src.gameserver.model.L2World;

public final class CellPathFinding extends PathFinding
{
	private static final class SingletonHolder
	{
		private static final CellPathFinding INSTANCE = new CellPathFinding();
	}

	public static CellPathFinding getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	@Override
	public Node[] findPath(int x, int y, int z, int tx, int ty, int tz)
	{
		int gx = x - L2World.MAP_MIN_X >> 4;
		int gy = y - L2World.MAP_MIN_Y >> 4;
		if(!GeoData.getInstance().hasGeo(x, y))
		{
			return null;
		}
		short gz = GeoData.getInstance().getHeight(x, y, z);
		int gtx = tx - L2World.MAP_MIN_X >> 4;
		int gty = ty - L2World.MAP_MIN_Y >> 4;
		if(!GeoData.getInstance().hasGeo(tx, ty))
		{
			return null;
		}
		short gtz = GeoData.getInstance().getHeight(tx, ty, tz);
		Node start = readNode(gx, gy, gz);
		Node end = readNode(gtx, gty, gtz);
		return searchByClosest(start, end);
	}

	@Override
	public Node[] readNeighbors(Node n, int idx)
	{
		return GeoData.getInstance().getNeighbors(n);
	}

	public Node readNode(int gx, int gy, short z)
	{
		return new CellNode(gx, gy, z, 0);
	}

	private CellPathFinding()
	{
	}

}