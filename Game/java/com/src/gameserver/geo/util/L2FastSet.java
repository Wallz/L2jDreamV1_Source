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
package com.src.gameserver.geo.util;

import java.util.Iterator;
import java.util.Set;

import javolution.util.FastCollection.Record;
import javolution.util.FastMap;

@SuppressWarnings("unchecked")
public class L2FastSet<E> extends L2FastCollection<E> implements Set<E>
{
	private static final Object NULL = new Object();

	private final FastMap<E, Object> _map;

	public L2FastSet()
	{
		_map = new FastMap<E, Object>();
	}

	public L2FastSet(int capacity)
	{
		_map = new FastMap<E, Object>(capacity);
	}

	public L2FastSet(Set<? extends E> elements)
	{
		_map = new FastMap<E, Object>(elements.size());

		addAll(elements);
	}

	@SuppressWarnings("deprecation")
	public L2FastSet<E> setShared(boolean isShared)
	{
		_map.setShared(isShared);
		return this;
	}

	public boolean isShared()
	{
		return _map.isShared();
	}

	@Override
	public Record head()
	{
		return _map.head();
	}

	@Override
	public Record tail()
	{
		return _map.tail();
	}

	@Override
	public E valueOf(Record record)
	{
		return ((FastMap.Entry<E, Object>)record).getKey();
	}

	@Override
	public void delete(Record record)
	{
		_map.remove(((FastMap.Entry<E, Object>)record).getKey());
	}

	@Override
	public void delete(Record record, E value)
	{
		_map.remove(value);
	}

	@Override
	public boolean add(E value)
	{
		return _map.put(value, NULL) == null;
	}

	@Override
	public void clear()
	{
		_map.clear();
	}

	@Override
	public boolean contains(Object o)
	{
		return _map.containsKey(o);
	}

	@Override
	public boolean isEmpty()
	{
		return _map.isEmpty();
	}

	@Override
	public Iterator<E> iterator()
	{
		return _map.keySet().iterator();
	}

	@Override
	public boolean remove(Object o)
	{
		return _map.remove(o) != null;
	}

	@Override
	public int size()
	{
		return _map.size();
	}

}