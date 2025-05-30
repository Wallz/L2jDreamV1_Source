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

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;

import javolution.util.FastList;

@SuppressWarnings("unchecked")
public final class L2Arrays
{
	private L2Arrays()
	{
	}

	public static int countNull(Object[] array)
	{
		if(array == null)
		{
			return 0;
		}

		int nullCount = 0;

		for(Object obj : array)
		{
			if(obj == null)
			{
				nullCount++;
			}
		}

		return nullCount;
	}

	public static int countNotNull(Object[] array)
	{
		return array == null ? 0 : array.length - countNull(array);
	}

	public static <T> T[] compact(T[] array)
	{
		final int newSize = countNotNull(array);

		if(array.length == newSize)
		{
			return array;
		}

		final T[] result = (T[])Array.newInstance(array.getClass().getComponentType(), newSize);

		int index = 0;

		for(T t : array)
		{
			if(t != null)
			{
				result[index++] = t;
			}
		}

		return result;
	}

	public static <T> List<T> asForeachSafeList(T... array)
	{
		return asForeachSafeList(true, array);
	}

	public static <T> List<T> asForeachSafeList(boolean allowAddition, T... array)
	{
		final int newSize = countNotNull(array);

		if(newSize == 0 && !allowAddition)
		{
			return L2Collections.emptyList();
		}

		if(newSize <= 8)
		{
			return new CopyOnWriteArrayList<T>(compact(array));
		}

		final List<T> result = new FastList<T>(newSize);

		for(T t : array)
		{
			if(t != null)
			{
				result.add(t);
			}
		}

		return result;
	}

	public static <T> Iterable<T> iterable(Object[] array)
	{
		return new NullFreeArrayIterable<T>(array);
	}

	public static <T> Iterable<T> iterable(Object[] array, boolean allowNull)
	{
		if(allowNull)
		{
			return new ArrayIterable<T>(array);
		}
		else
		{
			return new NullFreeArrayIterable<T>(array);
		}
	}

	private static class ArrayIterable<T> implements Iterable<T>
	{
		protected final Object[] _array;

		private ArrayIterable(Object[] array)
		{
			_array = array;
		}

		@Override
		public Iterator<T> iterator()
		{
			return new ArrayIterator<T>(_array);
		}
	}

	private static final class NullFreeArrayIterable<T> extends ArrayIterable<T>
	{
		private NullFreeArrayIterable(Object[] array)
		{
			super(array);
		}

		@Override
		public Iterator<T> iterator()
		{
			return new NullFreeArrayIterator<T>(_array);
		}
	}

	public static <T> Iterator<T> iterator(Object[] array)
	{
		return new NullFreeArrayIterator<T>(array);
	}

	public static <T> Iterator<T> iterator(Object[] array, boolean allowNull)
	{
		if(allowNull)
		{
			return new ArrayIterator<T>(array);
		}
		else
		{
			return new NullFreeArrayIterator<T>(array);
		}
	}

	private static class ArrayIterator<T> implements Iterator<T>
	{
		private final Object[] _array;

		private int _index;

		private ArrayIterator(Object[] array)
		{
			_array = array;
		}

		boolean allowElement(Object obj)
		{
			return true;
		}

		@Override
		public final boolean hasNext()
		{
			for(;;)
			{
				if(_array.length <= _index)
				{
					return false;
				}

				if(allowElement(_array[_index]))
				{
					return true;
				}

				_index++;
			}
		}

		@Override
		public final T next()
		{
			if(!hasNext())
			{
				throw new NoSuchElementException();
			}

			return (T)_array[_index++];
		}

		@Override
		public final void remove()
		{
			throw new UnsupportedOperationException();
		}
	}

	private static final class NullFreeArrayIterator<T> extends ArrayIterator<T>
	{
		private NullFreeArrayIterator(Object[] array)
		{
			super(array);
		}

		@Override
		boolean allowElement(Object obj)
		{
			return obj != null;
		}
	}

}