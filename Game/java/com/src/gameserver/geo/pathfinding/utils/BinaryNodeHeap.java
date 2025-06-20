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
package com.src.gameserver.geo.pathfinding.utils;

import java.util.Arrays;

import com.src.gameserver.geo.pathfinding.Node;
import com.src.gameserver.geo.util.L2FastSet;
import com.src.gameserver.geo.util.ObjectPool;

public final class BinaryNodeHeap
{
	private final Node[] _list = new Node[800 + 1];
	private final L2FastSet<Node> _set = new L2FastSet<Node>();
	private int _size = 0;

	private BinaryNodeHeap()
	{
	}

	public void add(Node n)
	{
		_size++;
		int pos = _size;
		_list[pos] = n;
		_set.add(n);
		while(pos != 1)
		{
			int p2 = pos / 2;
			if(_list[pos].getCost() <= _list[p2].getCost())
			{
				Node temp = _list[p2];
				_list[p2] = _list[pos];
				_list[pos] = temp;
				pos = p2;
			}
			else
			{
				break;
			}
		}
	}

	public Node removeFirst()
	{
		Node first = _list[1];
		_list[1] = _list[_size];
		_list[_size] = null;
		_size--;
		int pos = 1;
		int cpos;
		int dblcpos;
		Node temp;
		while(true)
		{
			cpos = pos;
			dblcpos = cpos * 2;
			if((dblcpos + 1) <= _size)
			{
				if(_list[cpos].getCost() >= _list[dblcpos].getCost())
				{
					pos = dblcpos;
				}
				if(_list[pos].getCost() >= _list[dblcpos + 1].getCost())
				{
					pos = dblcpos + 1;
				}
			}
			else if(dblcpos <= _size)
			{
				if(_list[cpos].getCost() >= _list[dblcpos].getCost())
				{
					pos = dblcpos;
				}
			}

			if(cpos != pos)
			{
				temp = _list[cpos];
				_list[cpos] = _list[pos];
				_list[pos] = temp;
			}
			else
			{
				break;
			}
		}
		_set.remove(first);
		return first;
	}

	public boolean contains(Node n)
	{
		if(_size == 0)
		{
			return false;
		}

		return _set.contains(n);
	}

	public boolean isEmpty()
	{
		return _size == 0;
	}

	public static BinaryNodeHeap newInstance()
	{
		return POOL.get();
	}

	public static void recycle(BinaryNodeHeap heap)
	{
		POOL.store(heap);
	}

	private static final ObjectPool<BinaryNodeHeap> POOL = new ObjectPool<BinaryNodeHeap>()
	{
		@Override
		protected void reset(BinaryNodeHeap heap)
		{
			Arrays.fill(heap._list, null);
			heap._set.clear();
			heap._size = 0;
		}

		@Override
		protected BinaryNodeHeap create()
		{
			return new BinaryNodeHeap();
		}
	};

}