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

import java.util.Arrays;
import java.util.Iterator;

@SuppressWarnings("unchecked")
public class LookupTable<T> implements Iterable<T>
{
	private static final Object[] EMPTY_ARRAY = new Object[0];

	private Object[] _array = EMPTY_ARRAY;

	private int _offset = 0;

	private int _size = 0;

	public int size()
	{
		return _size;
	}

	public boolean isEmpty()
	{
		return _size == 0;
	}

	public void clear(boolean force)
	{
		if(force)
			_array = EMPTY_ARRAY;
		else
			Arrays.fill(_array, null);

		_offset = 0;
		_size = 0;
	}

	public T get(final int key)
	{
		final int index = key + _offset;

		if(index < 0 || _array.length <= index)
		{
			return null;
		}

		return (T)_array[index];
	}

	public void set(final int key, final T newValue)
	{
		final int index = key + _offset;

		if(0 <= index && index < _array.length)
		{
			final T oldValue = (T)_array[index];

			_array[index] = newValue;

			if(oldValue != null && oldValue != newValue)
			{
				replacedValue(key, oldValue, newValue);
			}

			if(oldValue == null)
			{
				if(newValue != null)
				{
					_size++;
				}
			}
			else
			{
				if(newValue == null)
				{
					_size--;
				}
			}

			return;
		}

		_size++;

		if(_array.length == 0)
		{
			_array = new Object[] { newValue };
			_offset = -1 * key;
			return;
		}

		final int minimumKey = Math.min(0 - _offset, key);
		final int maximumKey = Math.max((_array.length - 1) - _offset, key);

		final Object[] newArray = new Object[maximumKey - minimumKey + 1];
		final int newOffset = -1 * minimumKey;

		System.arraycopy(_array, 0, newArray, newOffset - _offset, _array.length);

		_array = newArray;
		_offset = newOffset;

		_array[key + _offset] = newValue;
	}

	protected void replacedValue(final int key, final T oldValue, final T newValue)
	{
	}

	@Override
	public Iterator<T> iterator()
	{
		return L2Arrays.iterator(_array, false);
	}

}