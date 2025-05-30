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
import java.util.ConcurrentModificationException;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.src.gameserver.thread.L2Thread;

public abstract class ObjectPool<E>
{
	private static final WeakHashMap<ObjectPool<?>, Object> POOLS = new WeakHashMap<ObjectPool<?>, Object>();

	static
	{
		new L2Thread(ObjectPool.class.getName())
		{
			@Override
			protected void runTurn()
			{
				try
				{
					for(ObjectPool<?> pool : POOLS.keySet())
					{
						if(pool != null)
						{
							pool.purge();
						}
					}
				}
				catch(ConcurrentModificationException e)
				{
				}
			}

			@Override
			protected void sleepTurn() throws InterruptedException
			{
				Thread.sleep(60000);
			}
		}.start();
	}

	private final ReentrantLock _lock = new ReentrantLock();

	private Object[] _elements = new Object[0];
	private long[] _access = new long[0];
	private int _size = 0;

	protected ObjectPool()
	{
		POOLS.put(this, POOLS);
	}

	public int getCurrentSize()
	{
		_lock.lock();
		try
		{
			return _size;
		}
		finally
		{
			_lock.unlock();
		}
	}

	protected int getMaximumSize()
	{
		return Integer.MAX_VALUE;
	}

	protected long getMaxLifeTime()
	{
		return 120000;
	}

	public void clear()
	{
		_lock.lock();
		try
		{
			_elements = new Object[0];
			_access = new long[0];
			_size = 0;
		}
		finally
		{
			_lock.unlock();
		}
	}

	public void store(E e)
	{
		if(getCurrentSize() >= getMaximumSize())
		{
			return;
		}

		reset(e);

		_lock.lock();
		try
		{
			if(_size == _elements.length)
			{
				_elements = Arrays.copyOf(_elements, _elements.length + 10);
				_access = Arrays.copyOf(_access, _access.length + 10);
			}

			_elements[_size] = e;
			_access[_size] = System.currentTimeMillis();

			_size++;
		}
		finally
		{
			_lock.unlock();
		}
	}

	protected void reset(E e)
	{
	}

	@SuppressWarnings("unchecked")
	public E get()
	{
		Object obj = null;

		_lock.lock();
		try
		{
			if(_size > 0)
			{
				_size--;

				obj = _elements[_size];

				_elements[_size] = null;
				_access[_size] = 0;
			}
		}
		finally
		{
			_lock.unlock();
		}

		return obj == null ? create() : (E)obj;
	}

	protected abstract E create();

	public void purge()
	{
		_lock.lock();
		try
		{
			int newIndex = 0;
			for(int oldIndex = 0; oldIndex < _elements.length; oldIndex++)
			{
				final Object obj = _elements[oldIndex];
				final long time = _access[oldIndex];

				_elements[oldIndex] = null;
				_access[oldIndex] = 0;

				if(obj == null || time + getMaxLifeTime() < System.currentTimeMillis())
				{
					continue;
				}

				_elements[newIndex] = obj;
				_access[newIndex] = time;

				newIndex++;
			}

			_elements = Arrays.copyOf(_elements, newIndex);
			_access = Arrays.copyOf(_access, newIndex);
			_size = newIndex;
		}
		finally
		{
			_lock.unlock();
		}
	}

}