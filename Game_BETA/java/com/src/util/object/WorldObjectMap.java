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

public class WorldObjectMap<T extends L2Object> extends L2ObjectMap<T>
{
	Map<Integer, T> _objectMap = new FastMap<Integer, T>().shared();

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
		if(obj != null)
		{
			_objectMap.put(obj.getObjectId(), obj);
		}
	}

	@Override
	public void remove(T obj)
	{
		if(obj != null)
		{
			_objectMap.remove(obj.getObjectId());
		}
	}

	@Override
	public T get(int id)
	{
		return _objectMap.get(id);
	}

	@Override
	public boolean contains(T obj)
	{
		if(obj == null)
		{
			return false;
		}

		return _objectMap.get(obj.getObjectId()) != null;
	}

	@Override
	public Iterator<T> iterator()
	{
		return _objectMap.values().iterator();
	}

}