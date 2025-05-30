/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.src.gameserver.geo.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

@SuppressWarnings("unchecked")
public final class L2Collections
{
	private static final Object[] EMPTY_ARRAY = new Object[0];

	private static final class EmptyListIterator implements ListIterator<Object>
	{
		private static final ListIterator<Object> INSTANCE = new EmptyListIterator();

		@Override
		public boolean hasNext()
		{
			return false;
		}

		@Override
		public Object next()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasPrevious()
		{
			return false;
		}

		@Override
		public Object previous()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public int nextIndex()
		{
			return 0;
		}

		@Override
		public int previousIndex()
		{
			return -1;
		}

		@Override
		public void add(Object obj)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void set(Object obj)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}

	private static class EmptyCollection implements Collection<Object>
	{
		private static final Collection<Object> INSTANCE = new EmptyCollection();

		@Override
		public boolean add(Object e)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(Collection<? extends Object> c)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear()
		{
		}

		@Override
		public boolean contains(Object o)
		{
			return false;
		}

		@Override
		public boolean containsAll(Collection<?> c)
		{
			return false;
		}

		@Override
		public boolean isEmpty()
		{
			return true;
		}

		@Override
		public Iterator<Object> iterator()
		{
			return emptyListIterator();
		}

		@Override
		public boolean remove(Object o)
		{
			return false;
		}

		@Override
		public boolean removeAll(Collection<?> c)
		{
			return false;
		}

		@Override
		public boolean retainAll(Collection<?> c)
		{
			return false;
		}

		@Override
		public int size()
		{
			return 0;
		}

		@Override
		public Object[] toArray()
		{
			return EMPTY_ARRAY;
		}

		@Override
		public <T> T[] toArray(T[] a)
		{
			if(a.length != 0)
			{
				a = (T[])Array.newInstance(a.getClass().getComponentType(), 0);
			}

			return a;
		}
	}

	private static final class EmptySet extends EmptyCollection implements Set<Object>
	{
		private static final Set<Object> INSTANCE = new EmptySet();
	}

	private static final class EmptyList extends EmptyCollection implements List<Object>
	{
		private static final List<Object> INSTANCE = new EmptyList();

		@Override
		public void add(int index, Object element)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(int index, Collection<? extends Object> c)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Object get(int index)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public int indexOf(Object o)
		{
			return -1;
		}

		@Override
		public int lastIndexOf(Object o)
		{
			return -1;
		}

		@Override
		public ListIterator<Object> listIterator()
		{
			return emptyListIterator();
		}

		@Override
		public ListIterator<Object> listIterator(int index)
		{
			return emptyListIterator();
		}

		@Override
		public Object remove(int index)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Object set(int index, Object element)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public List<Object> subList(int fromIndex, int toIndex)
		{
			throw new UnsupportedOperationException();
		}
	}

	private static final class EmptyMap implements Map<Object, Object>
	{
		private static final Map<Object, Object> INSTANCE = new EmptyMap();

		@Override
		public void clear()
		{
		}

		@Override
		public boolean containsKey(Object key)
		{
			return false;
		}

		@Override
		public boolean containsValue(Object value)
		{
			return false;
		}

		@Override
		public Set<Map.Entry<Object, Object>> entrySet()
		{
			return emptySet();
		}

		@Override
		public Object get(Object key)
		{
			return null;
		}

		@Override
		public boolean isEmpty()
		{
			return true;
		}

		@Override
		public Set<Object> keySet()
		{
			return emptySet();
		}

		@Override
		public Object put(Object key, Object value)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void putAll(Map<? extends Object, ? extends Object> m)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Object remove(Object key)
		{
			return null;
		}

		@Override
		public int size()
		{
			return 0;
		}

		@Override
		public Collection<Object> values()
		{
			return emptyCollection();
		}
	}

	private static final class EmptyBunch implements Bunch<Object>
	{
		private static final Bunch<Object> INSTANCE = new EmptyBunch();

		@Override
		public Bunch<Object> add(Object e)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Bunch<Object> addAll(Iterable<? extends Object> c)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Bunch<Object> addAll(Object[] array)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear()
		{
		}

		@Override
		public boolean contains(Object o)
		{
			return false;
		}

		@Override
		public Object get(int index)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isEmpty()
		{
			return true;
		}

		@Override
		public Object[] moveToArray()
		{
			return EMPTY_ARRAY;
		}

		@Override
		public <T> T[] moveToArray(T[] array)
		{
			if(array.length != 0)
				array = (T[])Array.newInstance(array.getClass().getComponentType(), 0);

			return array;
		}

		@Override
		public <T> T[] moveToArray(Class<T> clazz)
		{
			return (T[])Array.newInstance(clazz, 0);
		}

		@Override
		public List<Object> moveToList(List<Object> list)
		{
			return list;
		}

		@Override
		public Bunch<Object> remove(Object o)
		{
			return this;
		}

		@Override
		public Object remove(int index)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Object set(int index, Object value)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public int size()
		{
			return 0;
		}

		@Override
		public Bunch<Object> cleanByFilter(Filter<Object> filter)
		{
			return this;
		}
	}

	private static <T> ListIterator<T> emptyListIterator()
	{
		return (ListIterator<T>)EmptyListIterator.INSTANCE;
	}

	private static <T> Collection<T> emptyCollection()
	{
		return (Collection<T>)EmptyCollection.INSTANCE;
	}

	public static <T> Set<T> emptySet()
	{
		return (Set<T>)EmptySet.INSTANCE;
	}

	public static <T> List<T> emptyList()
	{
		return (List<T>)EmptyList.INSTANCE;
	}

	public static <K, V> Map<K, V> emptyMap()
	{
		return (Map<K, V>)EmptyMap.INSTANCE;
	}

	public static <T> Bunch<T> emptyBunch()
	{
		return (Bunch<T>)EmptyBunch.INSTANCE;
	}

	public static <T> Iterable<T> filteredIterable(Class<T> clazz, Iterable<? super T> iterable)
	{
		return filteredIterable(clazz, iterable, null);
	}

	public static <T> Iterable<T> filteredIterable(Class<T> clazz, Iterable<? super T> iterable, Filter<T> filter)
	{
		return new FilteredIterable<T>(clazz, iterable, filter);
	}

	public static <T> Iterator<T> filteredIterator(Class<T> clazz, Iterable<? super T> iterable)
	{
		return filteredIterator(clazz, iterable, null);
	}

	public static <T> Iterator<T> filteredIterator(Class<T> clazz, Iterable<? super T> iterable, Filter<T> filter)
	{
		return new FilteredIterator<T>(clazz, iterable, filter);
	}

	public interface Filter<E>
	{
		public boolean accept(E element);
	}

	private static final class FilteredIterable<E> implements Iterable<E>
	{
		private final Iterable<? super E> _iterable;
		private final Filter<E> _filter;
		private final Class<E> _clazz;

		private FilteredIterable(Class<E> clazz, Iterable<? super E> iterable, Filter<E> filter)
		{
			_iterable = iterable;
			_filter = filter;
			_clazz = clazz;
		}

		@Override
		public Iterator<E> iterator()
		{
			return filteredIterator(_clazz, _iterable, _filter);
		}
	}

	private static final class FilteredIterator<E> implements Iterator<E>
	{
		private final Iterator<? super E> _iterator;
		private final Filter<E> _filter;
		private final Class<E> _clazz;

		private E _next;

		private FilteredIterator(Class<E> clazz, Iterable<? super E> iterable, Filter<E> filter)
		{
			_iterator = iterable.iterator();
			_filter = filter;
			_clazz = clazz;

			step();
		}

		@Override
		public boolean hasNext()
		{
			return _next != null;
		}

		@Override
		public E next()
		{
			if(!hasNext())
			{
				throw new NoSuchElementException();
			}

			E next = _next;

			step();

			return next;
		}

		private void step()
		{
			while(_iterator.hasNext())
			{
				Object next = _iterator.next();

				if(next == null || !_clazz.isInstance(next))
				{
					continue;
				}

				if(_filter == null || _filter.accept((E)next))
				{
					_next = (E)next;
					return;
				}
			}

			_next = null;
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}

	public static <S, T> Iterable<T> convertingIterable(Iterable<? extends S> iterable, Converter<S, T> converter)
	{
		return new ConvertingIterable<S, T>(iterable, converter);
	}

	public static <S, T> Iterator<T> convertingIterator(Iterable<? extends S> iterable, Converter<S, T> converter)
	{
		return new ConvertingIterator<S, T>(iterable, converter);
	}

	public interface Converter<S, T>
	{
		public T convert(S src);
	}

	private static final class ConvertingIterable<S, T> implements Iterable<T>
	{
		private final Iterable<? extends S> _iterable;
		private final Converter<S, T> _converter;

		private ConvertingIterable(Iterable<? extends S> iterable, Converter<S, T> converter)
		{
			_iterable = iterable;
			_converter = converter;
		}

		@Override
		public Iterator<T> iterator()
		{
			return convertingIterator(_iterable, _converter);
		}
	}

	private static final class ConvertingIterator<S, T> implements Iterator<T>
	{
		private final Iterator<? extends S> _iterator;
		private final Converter<S, T> _converter;

		private T _next;

		private ConvertingIterator(Iterable<? extends S> iterable, Converter<S, T> converter)
		{
			_iterator = iterable.iterator();
			_converter = converter;

			step();
		}

		@Override
		public boolean hasNext()
		{
			return _next != null;
		}

		@Override
		public T next()
		{
			if(!hasNext())
			{
				throw new NoSuchElementException();
			}

			T next = _next;

			step();

			return next;
		}

		private void step()
		{
			while(_iterator.hasNext())
			{
				S src = _iterator.next();

				if(src == null)
				{
					continue;
				}

				T next = _converter.convert(src);

				if(next != null)
				{
					_next = next;
					return;
				}
			}

			_next = null;
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}

	public static <T> Iterable<T> concatenatedIterable(Iterable<? extends T> iterable1, Iterable<? extends T> iterable2)
	{
		return new ConcatenatedIterable<T>(iterable1, iterable2);
	}

	public static <T> Iterable<T> concatenatedIterable(Iterable<? extends T> iterable1, Iterable<? extends T> iterable2, Iterable<? extends T> iterable3)
	{
		return new ConcatenatedIterable<T>(iterable1, iterable2, iterable3);
	}

	public static <T> Iterable<T> concatenatedIterable(Iterable<? extends T>... iterables)
	{
		return new ConcatenatedIterable<T>(iterables);
	}

	public static <T> Iterator<T> concatenatedIterator(Iterable<? extends T> iterable1, Iterable<? extends T> iterable2)
	{
		return new ConcatenatedIterator<T>(iterable1, iterable2);
	}

	public static <T> Iterator<T> concatenatedIterator(Iterable<? extends T> iterable1, Iterable<? extends T> iterable2, Iterable<? extends T> iterable3)
	{
		return new ConcatenatedIterator<T>(iterable1, iterable2, iterable3);
	}

	public static <T> Iterator<T> concatenatedIterator(Iterable<? extends T>... iterables)
	{
		return new ConcatenatedIterator<T>(iterables);
	}

	private static final class ConcatenatedIterable<E> implements Iterable<E>
	{
		private final Iterable<? extends E>[] _iterables;

		private ConcatenatedIterable(Iterable<? extends E>... iterables)
		{
			_iterables = iterables;
		}

		@Override
		public Iterator<E> iterator()
		{
			return concatenatedIterator(_iterables);
		}
	}

	private static final class ConcatenatedIterator<E> implements Iterator<E>
	{
		private final Iterable<? extends E>[] _iterables;

		private Iterator<? extends E> _iterator;
		private int _index = -1;

		private ConcatenatedIterator(Iterable<? extends E>... iterables)
		{
			_iterables = iterables;

			validateIterator();
		}

		@Override
		public boolean hasNext()
		{
			validateIterator();

			return _iterator != null && _iterator.hasNext();
		}

		@Override
		public E next()
		{
			if(!hasNext())
			{
				throw new NoSuchElementException();
			}

			return _iterator.next();
		}

		private void validateIterator()
		{
			while(_iterator == null || !_iterator.hasNext())
			{
				_index++;

				if(_index >= _iterables.length)
				{
					return;
				}

				_iterator = _iterables[_index].iterator();
			}
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}

	@SuppressWarnings("rawtypes")
	private static final ObjectPool<ArrayList> ARRAY_LISTS = new ObjectPool<ArrayList>()
	{
		@Override
		protected void reset(ArrayList list)
		{
			list.clear();
		}

		@Override
		protected ArrayList create()
		{
			return new ArrayList();
		}
	};

	@SuppressWarnings("rawtypes")
	private static final ObjectPool<L2FastSet> L2_FAST_SETS = new ObjectPool<L2FastSet>()
	{
		@Override
		protected void reset(L2FastSet list)
		{
			list.clear();
		}

		@Override
		protected L2FastSet create()
		{
			return new L2FastSet();
		}
	};

	public static <T> ArrayList<T> newArrayList()
	{
		return ARRAY_LISTS.get();
	}

	@SuppressWarnings("rawtypes")
	public static void recycle(ArrayList arrayList)
	{
		ARRAY_LISTS.store(arrayList);
	}

	public static <T> L2FastSet<T> newL2FastSet()
	{
		return L2_FAST_SETS.get();
	}

	@SuppressWarnings("rawtypes")
	public static void recycle(L2FastSet l2FastSet)
	{
		L2_FAST_SETS.store(l2FastSet);
	}
}