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

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.util.StringUtil;

public class ThreadPoolManager
{
	private static final Log _log = LogFactory.getLog(ThreadPoolManager.class);

	private static final class RunnableWrapper implements Runnable
	{
		private final Runnable _r;

		public RunnableWrapper(final Runnable r)
		{
			_r = r;
		}

		@Override
		public final void run()
		{
			try
			{
				_r.run();
			}
			catch(final Throwable e)
			{
				e.printStackTrace();
				
				final Thread t = Thread.currentThread();
				final UncaughtExceptionHandler h = t.getUncaughtExceptionHandler();
				if(h != null)
				{
					h.uncaughtException(t, e);
				}
			}
		}
	}

	private ScheduledThreadPoolExecutor _effectsScheduledThreadPool;
	private ScheduledThreadPoolExecutor _generalScheduledThreadPool;
	private ScheduledThreadPoolExecutor _aiScheduledThreadPool;

	private ThreadPoolExecutor _generalPacketsThreadPool;
	private ThreadPoolExecutor _ioPacketsThreadPool;
	private ThreadPoolExecutor _generalThreadPool;

	private static final long MAX_DELAY = Long.MAX_VALUE / 1000000 / 2;

	private boolean _shutdown;

	public static ThreadPoolManager getInstance()
	{
		return SingletonHolder._instance;
	}

	private ThreadPoolManager()
	{
		_effectsScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.THREAD_P_EFFECTS, new PriorityThreadFactory("EffectsSTPool", Thread.NORM_PRIORITY));
		_generalScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.THREAD_P_GENERAL, new PriorityThreadFactory("GeneralSTPool", Thread.NORM_PRIORITY));
		_ioPacketsThreadPool = new ThreadPoolExecutor(Config.IO_PACKET_THREAD_CORE_SIZE, Integer.MAX_VALUE, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("I/O Packet Pool", Thread.NORM_PRIORITY + 1));
		_generalPacketsThreadPool = new ThreadPoolExecutor(Config.GENERAL_PACKET_THREAD_CORE_SIZE, Config.GENERAL_PACKET_THREAD_CORE_SIZE + 2, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("Normal Packet Pool", Thread.NORM_PRIORITY + 1));
		_generalThreadPool = new ThreadPoolExecutor(Config.GENERAL_THREAD_CORE_SIZE, Config.GENERAL_THREAD_CORE_SIZE + 2, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory("General Pool", Thread.NORM_PRIORITY));
		_aiScheduledThreadPool = new ScheduledThreadPoolExecutor(Config.AI_MAX_THREAD, new PriorityThreadFactory("AISTPool", Thread.NORM_PRIORITY));

		scheduleGeneralAtFixedRate(new PurgeTask(), 10*60*1000l, 5*60*1000l);
	}

	public static long validateDelay(long delay)
	{
		if(delay < 0)
		{
			delay = 0;
		}
		else if(delay > MAX_DELAY)
		{
			delay = MAX_DELAY;
		}

		return delay;
	}

	public ScheduledFuture<?> scheduleEffect(Runnable r, long delay)
	{
		try
		{
			delay = ThreadPoolManager.validateDelay(delay);
			return _effectsScheduledThreadPool.schedule(new RunnableWrapper(r), delay, TimeUnit.MILLISECONDS);
		}
		catch(RejectedExecutionException e)
		{
			e.printStackTrace();
			
			return null;
		}
	}

	public ScheduledFuture<?> scheduleEffectAtFixedRate(Runnable r, long initial, long delay)
	{
		try
		{
			delay = ThreadPoolManager.validateDelay(delay);
			initial = ThreadPoolManager.validateDelay(initial);
			return _effectsScheduledThreadPool.scheduleAtFixedRate(new RunnableWrapper(r), initial, delay, TimeUnit.MILLISECONDS);
		}
		catch(RejectedExecutionException e)
		{
			e.printStackTrace();
			
			return null;
		}
	}

	//@Deprecated
	public boolean removeGeneral(Runnable r)
	{
		return _effectsScheduledThreadPool.remove(r);
	}

	public ScheduledFuture<?> scheduleGeneral(Runnable r, long delay)
	{
		try
		{
			delay = ThreadPoolManager.validateDelay(delay);
			return _generalScheduledThreadPool.schedule(new RunnableWrapper(r), delay, TimeUnit.MILLISECONDS);
		}
		catch(RejectedExecutionException e)
		{
			e.printStackTrace();
			
			return null;
		}
	}

	public ScheduledFuture<?> scheduleGeneralAtFixedRate(Runnable r, long initial, long delay)
	{
		try
		{
			delay = ThreadPoolManager.validateDelay(delay);
			initial = ThreadPoolManager.validateDelay(initial);
			return _generalScheduledThreadPool.scheduleAtFixedRate(new RunnableWrapper(r), initial, delay, TimeUnit.MILLISECONDS);
		}
		catch(RejectedExecutionException e)
		{
			e.printStackTrace();
			
			return null;
		}
	}

	@Deprecated
	public boolean removeGeneral(RunnableScheduledFuture<?> r)
	{
		return _generalScheduledThreadPool.remove(r);
	}

	public ScheduledFuture<?> scheduleAi(Runnable r, long delay)
	{
		try
		{
			delay = ThreadPoolManager.validateDelay(delay);
			return _aiScheduledThreadPool.schedule(new RunnableWrapper(r), delay, TimeUnit.MILLISECONDS);
		}
		catch(RejectedExecutionException e)
		{
			e.printStackTrace();
			
			return null;
		}
	}

	public ScheduledFuture<?> scheduleAiAtFixedRate(Runnable r, long initial, long delay)
	{
		try
		{
			delay = ThreadPoolManager.validateDelay(delay);
			initial = ThreadPoolManager.validateDelay(initial);
			return _aiScheduledThreadPool.scheduleAtFixedRate(new RunnableWrapper(r), initial, delay, TimeUnit.MILLISECONDS);
		}
		catch(RejectedExecutionException e)
		{
			e.printStackTrace();
			
			return null;
		}
	}

	public void executePacket(Runnable pkt)
	{
		if(!_generalPacketsThreadPool.isShutdown())
		{
			_generalPacketsThreadPool.execute(pkt);
		}
	}

	public void executeCommunityPacket(Runnable r)
	{
		if(!_generalPacketsThreadPool.isShutdown())
		{
			_generalPacketsThreadPool.execute(r);
		}
	}

	public void executeIOPacket(Runnable pkt)
	{
		if(!_ioPacketsThreadPool.isShutdown())
		{
			_ioPacketsThreadPool.execute(pkt);
		}
	}

	public void executeTask(Runnable r)
	{
		if(!_aiScheduledThreadPool.isShutdown())
		{
			_aiScheduledThreadPool.execute(new RunnableWrapper(r));
		}
	}

	public void executeAi(Runnable r)
	{
		if(!_aiScheduledThreadPool.isShutdown())
		{
			_aiScheduledThreadPool.execute(new RunnableWrapper(r));
		}
	}

	public String[] getStats()
	{
		return new String[]
		{
				"STP:",
				" + Effects:",
				" |- ActiveThreads:   " + _effectsScheduledThreadPool.getActiveCount(),
				" |- getCorePoolSize: " + _effectsScheduledThreadPool.getCorePoolSize(),
				" |- PoolSize:        " + _effectsScheduledThreadPool.getPoolSize(),
				" |- MaximumPoolSize: " + _effectsScheduledThreadPool.getMaximumPoolSize(),
				" |- CompletedTasks:  " + _effectsScheduledThreadPool.getCompletedTaskCount(),
				" |- ScheduledTasks:  " + (_effectsScheduledThreadPool.getTaskCount() - _effectsScheduledThreadPool.getCompletedTaskCount()),
				" | -------",
				" + General:",
				" |- ActiveThreads:   " + _generalScheduledThreadPool.getActiveCount(),
				" |- getCorePoolSize: " + _generalScheduledThreadPool.getCorePoolSize(),
				" |- PoolSize:        " + _generalScheduledThreadPool.getPoolSize(),
				" |- MaximumPoolSize: " + _generalScheduledThreadPool.getMaximumPoolSize(),
				" |- CompletedTasks:  " + _generalScheduledThreadPool.getCompletedTaskCount(),
				" |- ScheduledTasks:  " + (_generalScheduledThreadPool.getTaskCount() - _generalScheduledThreadPool.getCompletedTaskCount()),
				" | -------",
				" + AI:",
				" |- ActiveThreads:   " + _aiScheduledThreadPool.getActiveCount(),
				" |- getCorePoolSize: " + _aiScheduledThreadPool.getCorePoolSize(),
				" |- PoolSize:        " + _aiScheduledThreadPool.getPoolSize(),
				" |- MaximumPoolSize: " + _aiScheduledThreadPool.getMaximumPoolSize(),
				" |- CompletedTasks:  " + _aiScheduledThreadPool.getCompletedTaskCount(),
				" |- ScheduledTasks:  " + (_aiScheduledThreadPool.getTaskCount() - _aiScheduledThreadPool.getCompletedTaskCount()),
				"TP:",
				" + Packets:",
				" |- ActiveThreads:   " + _generalPacketsThreadPool.getActiveCount(),
				" |- getCorePoolSize: " + _generalPacketsThreadPool.getCorePoolSize(),
				" |- MaximumPoolSize: " + _generalPacketsThreadPool.getMaximumPoolSize(),
				" |- LargestPoolSize: " + _generalPacketsThreadPool.getLargestPoolSize(),
				" |- PoolSize:        " + _generalPacketsThreadPool.getPoolSize(),
				" |- CompletedTasks:  " + _generalPacketsThreadPool.getCompletedTaskCount(),
				" |- QueuedTasks:     " + _generalPacketsThreadPool.getQueue().size(),
				" | -------",
				" + I/O Packets:",
				" |- ActiveThreads:   " + _ioPacketsThreadPool.getActiveCount(),
				" |- getCorePoolSize: " + _ioPacketsThreadPool.getCorePoolSize(),
				" |- MaximumPoolSize: " + _ioPacketsThreadPool.getMaximumPoolSize(),
				" |- LargestPoolSize: " + _ioPacketsThreadPool.getLargestPoolSize(),
				" |- PoolSize:        " + _ioPacketsThreadPool.getPoolSize(),
				" |- CompletedTasks:  " + _ioPacketsThreadPool.getCompletedTaskCount(),
				" |- QueuedTasks:     " + _ioPacketsThreadPool.getQueue().size(),
				" | -------",
				" + General Tasks:",
				" |- ActiveThreads:   " + _generalThreadPool.getActiveCount(),
				" |- getCorePoolSize: " + _generalThreadPool.getCorePoolSize(),
				" |- MaximumPoolSize: " + _generalThreadPool.getMaximumPoolSize(),
				" |- LargestPoolSize: " + _generalThreadPool.getLargestPoolSize(),
				" |- PoolSize:        " + _generalThreadPool.getPoolSize(),
				" |- CompletedTasks:  " + _generalThreadPool.getCompletedTaskCount(),
				" |- QueuedTasks:     " + _generalThreadPool.getQueue().size(),
				" | -------",
				" + Javolution stats:",
				" |- FastList:        " + FastList.report(),
				" |- FastMap:         " + FastMap.report(),
				" |- FastSet:         " + FastSet.report(),
				" | -------"
		};
	}

	private static class PriorityThreadFactory implements ThreadFactory
	{
		private int _prio;
		private String _name;
		private AtomicInteger _threadNumber = new AtomicInteger(1);
		private ThreadGroup _group;
		
		public PriorityThreadFactory(String name, int prio)
		{
			_prio = prio;
			_name = name;
			_group = new ThreadGroup(_name);
		}

		@Override
		public Thread newThread(Runnable r)
		{
			Thread t = new Thread(_group, r);
			t.setName(_name + "-" + _threadNumber.getAndIncrement());
			t.setPriority(_prio);
			return t;
		}

		public ThreadGroup getGroup()
		{
			return _group;
		}
	}

	public void shutdown()
	{
		_shutdown = true;
		try
		{
			_effectsScheduledThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_generalScheduledThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_generalPacketsThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_ioPacketsThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_generalThreadPool.awaitTermination(1, TimeUnit.SECONDS);
			_effectsScheduledThreadPool.shutdown();
			_generalScheduledThreadPool.shutdown();
			_generalPacketsThreadPool.shutdown();
			_ioPacketsThreadPool.shutdown();
			_generalThreadPool.shutdown();
			_log.info("All ThreadPools are now stopped.");

		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
			
			_log.error("", e);
		}
	}

	public boolean isShutdown()
	{
		return _shutdown;
	}

	public void purge()
	{
		_effectsScheduledThreadPool.purge();
		_generalScheduledThreadPool.purge();
		_aiScheduledThreadPool.purge();
		_ioPacketsThreadPool.purge();
		_generalPacketsThreadPool.purge();
		_generalThreadPool.purge();
	}

	public String getPacketStats()
	{
		final StringBuilder sb = new StringBuilder(1000);
		ThreadFactory tf = _generalPacketsThreadPool.getThreadFactory();
		if(tf instanceof PriorityThreadFactory)
		{
			PriorityThreadFactory ptf = (PriorityThreadFactory) tf;

			int count = ptf.getGroup().activeCount();

			Thread[] threads = new Thread[count + 2];

			ptf.getGroup().enumerate(threads);

			StringUtil.append(sb, "General Packet Thread Pool:\r\n" + "Tasks in the queue: ", String.valueOf(_generalPacketsThreadPool.getQueue().size()), "\r\n" + "Showing threads stack trace:\r\n" + "There should be ", String.valueOf(count), " Threads\r\n");

			for(Thread t : threads)
			{
				if(t == null)
				{
					continue;
				}

				StringUtil.append(sb, t.getName(), "\r\n");

				for(StackTraceElement ste : t.getStackTrace())
				{
					StringUtil.append(sb, ste.toString(), "\r\n");
				}
			}
		}

		sb.append("Packet Tp stack traces printed.\r\n");

		return sb.toString();
	}

	public String getIOPacketStats()
	{
		final StringBuilder sb = new StringBuilder(1000);
		ThreadFactory tf = _ioPacketsThreadPool.getThreadFactory();

		if(tf instanceof PriorityThreadFactory)
		{
			PriorityThreadFactory ptf = (PriorityThreadFactory) tf;

			int count = ptf.getGroup().activeCount();

			Thread[] threads = new Thread[count + 2];

			ptf.getGroup().enumerate(threads);

			StringUtil.append(sb, "I/O Packet Thread Pool:\r\n" + "Tasks in the queue: ", String.valueOf(_ioPacketsThreadPool.getQueue().size()), "\r\n" + "Showing threads stack trace:\r\n" + "There should be ", String.valueOf(count), " Threads\r\n");

			for(Thread t : threads)
			{
				if(t == null)
				{
					continue;
				}

				StringUtil.append(sb, t.getName(), "\r\n");

				for(StackTraceElement ste : t.getStackTrace())
				{
					StringUtil.append(sb, ste.toString(), "\r\n");
				}
			}
		}

		sb.append("Packet Tp stack traces printed.\r\n");

		return sb.toString();
	}

	public String getGeneralStats()
	{
		final StringBuilder sb = new StringBuilder(1000);
		ThreadFactory tf = _generalThreadPool.getThreadFactory();

		if(tf instanceof PriorityThreadFactory)
		{
			PriorityThreadFactory ptf = (PriorityThreadFactory) tf;

			int count = ptf.getGroup().activeCount();

			Thread[] threads = new Thread[count + 2];
			ptf.getGroup().enumerate(threads);

			StringUtil.append(sb, "General Thread Pool:\r\n" + "Tasks in the queue: ", String.valueOf(_generalThreadPool.getQueue().size()), "\r\n" + "Showing threads stack trace:\r\n" + "There should be ", String.valueOf(count), " Threads\r\n");

			for(Thread t : threads)
			{
				if(t == null)
				{
					continue;
				}

				StringUtil.append(sb, t.getName(), "\r\n");

				for(StackTraceElement ste : t.getStackTrace())
				{
					StringUtil.append(sb, ste.toString(), "\r\n");
				}
			}
		}
 
		sb.append("Packet Tp stack traces printed.\r\n");

		return sb.toString();
	}

	private class PurgeTask implements Runnable
	{
		@Override
		public void run()
		{
			_effectsScheduledThreadPool.purge();
			_generalScheduledThreadPool.purge();
			_aiScheduledThreadPool.purge();
		}
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final ThreadPoolManager _instance = new ThreadPoolManager();
	}
}