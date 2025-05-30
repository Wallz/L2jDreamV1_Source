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

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.util.List;

import javolution.util.FastList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class L2Thread extends Thread
{
	private static final Log _log = LogFactory.getLog(L2Thread.class);

	protected abstract void runTurn();
	protected abstract void sleepTurn() throws InterruptedException;

	private volatile boolean _isAlive = true;

	protected L2Thread()
	{
		super();
	}

	protected L2Thread(String name)
	{
		super(name);
	}

	public final void shutdown() throws InterruptedException
	{
		_isAlive = false;

		join();
	}

	@Override
	public final void run()
	{
		try
		{
			while(_isAlive)
			{
				final long begin = System.nanoTime();

				try
				{
					runTurn();
				}
				finally
				{
					RunnableStatsManager.handleStats(getClass(), System.nanoTime() - begin);
				}

				try
				{
					sleepTurn();
				}
				catch(InterruptedException e)
				{
					_log.error("", e);
				}
			}
		}
		finally
		{}
	}

	public static List<String> getStats(Thread t)
	{
		List<String> list = new FastList<String>();

		list.add(t.toString() + " - ID: " + t.getId());
		list.add(" * State: " + t.getState());
		list.add(" * Alive: " + t.isAlive());
		list.add(" * Daemon: " + t.isDaemon());
		list.add(" * Interrupted: " + t.isInterrupted());
		for(ThreadInfo info : ManagementFactory.getThreadMXBean().getThreadInfo(new long[]
		{
			t.getId()
		}, true, true))
		{
			for(MonitorInfo monitorInfo : info.getLockedMonitors())
			{
				list.add("==========");
				list.add(" * Locked monitor: " + monitorInfo);
				list.add("\t[" + monitorInfo.getLockedStackDepth() + ".]: at " + monitorInfo.getLockedStackFrame());
			}

			for(LockInfo lockInfo : info.getLockedSynchronizers())
			{
				list.add("==========");
				list.add(" * Locked synchronizer: " + lockInfo);
			}

			list.add("==========");
			for(StackTraceElement trace : info.getStackTrace())
			{
				list.add("\tat " + trace);
			}
		}
		return list;
	}

}