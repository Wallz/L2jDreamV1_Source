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
package com.src.gameserver.geo.pathfinding;


public abstract class Node
{
	private final int _neighborsIdx;
	private Node[] _neighbors;
	private Node _parent;
	private short _cost;

	protected Node(int neighborsIdx)
	{
		_neighborsIdx = neighborsIdx;
	}

	public final void setParent(Node p)
	{
		_parent = p;
	}

	public final void setCost(int cost)
	{
		_cost = (short)cost;
	}

	public final void attachNeighbors()
	{
		_neighbors = PathFinding.getInstance().readNeighbors(this, _neighborsIdx);
	}

	public final Node[] getNeighbors()
	{
		return _neighbors;
	}

	public final Node getParent()
	{
		return _parent;
	}

	public final short getCost()
	{
		return _cost;
	}

	public abstract int getX();

	public abstract int getY();

	public abstract short getZ();

	public abstract void setZ(short z);

	public abstract int getNodeX();

	public abstract int getNodeY();

	@Override
	public final int hashCode()
	{
		return hash((getNodeX() << 20) + (getNodeY() << 8) + getZ());
	}

	@Override
	public final boolean equals(Object obj)
	{
		if(!(obj instanceof Node))
		{
			return false;
		}

		Node n = (Node)obj;

		return getNodeX() == n.getNodeX() && getNodeY() == n.getNodeY() && getZ() == n.getZ();
	}

	public final int hash(int h)
	{
		h ^= (h >>> 20) ^ (h >>> 12);
		return h ^ (h >>> 7) ^ (h >>> 4);
	}

}