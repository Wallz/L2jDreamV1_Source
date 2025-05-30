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
package com.src.gameserver.thread;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.ArrayUtils;

public final class RunnableStatsManager
{
	private static final Map<Class<?>, ClassStat> _classStats = new HashMap<Class<?>, ClassStat>();

	private static final class ClassStat
	{
		private String[] _methodNames = new String[0];
		private MethodStat[] _methodStats = new MethodStat[0];

		private ClassStat(Class<?> clazz)
		{
			_classStats.put(clazz, this);
		}

		private MethodStat getMethodStat(String methodName, boolean synchronizedAlready)
		{
			for(int i = 0; i < _methodNames.length; i++)
			{
				if(_methodNames[i].equals(methodName))
				{
					return _methodStats[i];
				}
			}

			if(!synchronizedAlready)
			{
				synchronized (this)
				{
					return getMethodStat(methodName, true);
				}
			}

			methodName = methodName.intern();

			final MethodStat methodStat = new MethodStat();

			_methodNames = (String[]) ArrayUtils.add(_methodNames, methodName);
			_methodStats = (MethodStat[]) ArrayUtils.add(_methodStats, methodStat);

			return methodStat;
		}
	}

	private static final class MethodStat
	{
		private final ReentrantLock _lock = new ReentrantLock();

		@SuppressWarnings("unused")
		private long _count;
		@SuppressWarnings("unused")
		private long _total;
		private long _min = Long.MAX_VALUE;
		private long _max = Long.MIN_VALUE;

		private void handleStats(long runTime)
		{
			_lock.lock();
			try
			{
				_count++;
				_total += runTime;
				_min = Math.min(_min, runTime);
				_max = Math.max(_max, runTime);
			}
			finally
			{
				_lock.unlock();
			}
		}
	}

	private static ClassStat getClassStat(Class<?> clazz, boolean synchronizedAlready)
	{
		ClassStat classStat = _classStats.get(clazz);

		if(classStat != null)
		{
			return classStat;
		}

		if(!synchronizedAlready)
		{
			synchronized (RunnableStatsManager.class)
			{
				return getClassStat(clazz, true);
			}
		}

		return new ClassStat(clazz);
	}

	public static void handleStats(Class<? extends Runnable> clazz, long runTime)
	{
		handleStats(clazz, "run()", runTime);
	}

	public static void handleStats(Class<?> clazz, String methodName, long runTime)
	{
		getClassStat(clazz, false).getMethodStat(methodName, false).handleStats(runTime);
	}

}