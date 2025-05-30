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
package com.src.util.object;

import java.util.Iterator;
import java.util.Map;

import javolution.util.FastMap;

import com.src.gameserver.model.L2Object;

public class WorldObjectSet<T extends L2Object> extends L2ObjectSet<T>
{
	private Map<Integer, T> _objectMap;

	public WorldObjectSet()
	{
		_objectMap = new FastMap<Integer, T>().shared();
	}

	@Override
	public int size()
	{
		return _objectMap.size();
	}

	@Override
	public boolean isEmpty()
	{
		return _objectMap.isEmpty();
	}

	@Override
	public void clear()
	{
		_objectMap.clear();
	}

	@Override
	public void put(T obj)
	{
		_objectMap.put(obj.getObjectId(), obj);
	}

	@Override
	public void remove(T obj)
	{
		_objectMap.remove(obj.getObjectId());
	}

	@Override
	public boolean contains(T obj)
	{
		return _objectMap.containsKey(obj.getObjectId());
	}

	@Override
	public Iterator<T> iterator()
	{
		return _objectMap.values().iterator();
	}

}