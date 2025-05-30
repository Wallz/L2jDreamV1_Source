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
package com.src.gameserver;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import javolution.util.FastList;

import com.src.gameserver.ai.CtrlEvent;
import com.src.gameserver.managers.DayNightSpawnManager;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.thread.ThreadPoolManager;

public class GameTimeController
{
	static final Logger _log = Logger.getLogger(GameTimeController.class.getName());

	public static final int TICKS_PER_SECOND = 10;
	public static final int MILLIS_IN_TICK = 1000 / TICKS_PER_SECOND;

	private static GameTimeController _instance = new GameTimeController();

	protected static int _gameTicks;
	protected static long _gameStartTime;
	protected static boolean _isNight = false;

	private static List<L2Character> _movingObjects = new FastList<L2Character>();

	protected static TimerThread _timer;
	private ScheduledFuture<?> _timerWatcher;

	public static GameTimeController getInstance()
	{
		return _instance;
	}

	private GameTimeController()
	{
		_gameStartTime = System.currentTimeMillis() - 3600000;
		_gameTicks = 3600000 / MILLIS_IN_TICK;

		_timer = new TimerThread();
		_timer.start();

		_timerWatcher = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new TimerWatcher(), 0, 1000);
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new BroadcastSunState(), 0, 600000);

	}

	public boolean isNowNight()
	{
		return _isNight;
	}

	public int getGameTime()
	{
		return _gameTicks / (TICKS_PER_SECOND * 10);
	}

	public static int getGameTicks()
	{
		return _gameTicks;
	}

	public synchronized void registerMovingObject(L2Character cha)
	{
		if(cha == null)
		{
			return;
		}

		if(!_movingObjects.contains(cha))
		{
			_movingObjects.add(cha);
		}
	}

	protected synchronized void moveObjects()
	{
		L2Character[] chars = _movingObjects.toArray(new L2Character[_movingObjects.size()]);

		List<L2Character> ended = null;

		for(L2Character cha : chars)
		{
			boolean end = cha.updatePosition(_gameTicks);

			if(end)
			{
				_movingObjects.remove(cha);
				if(ended == null)
				{
					ended = new FastList<L2Character>();
				}
				ended.add(cha);
			}
		}

		if(ended != null)
		{
			ThreadPoolManager.getInstance().executeTask(new MovingObjectArrived(ended));
		}
	}

	public void stopTimer()
	{
		_timerWatcher.cancel(true);
		_timer.interrupt();
	}

	class TimerThread extends Thread
	{
		protected Exception _error;

		public TimerThread()
		{
			super("GameTimeController");
			setDaemon(true);
			setPriority(MAX_PRIORITY);
			_error = null;
		}

		@Override
		public void run()
		{
			try
			{
				for(;;)
				{
					int _oldTicks = _gameTicks;
					long runtime = System.currentTimeMillis() - _gameStartTime;

					_gameTicks = (int) (runtime / MILLIS_IN_TICK);

					if(_oldTicks != _gameTicks)
					{
						moveObjects();
					}

					runtime = System.currentTimeMillis() - _gameStartTime - runtime;

					int sleepTime = 1 + MILLIS_IN_TICK - (int) runtime % MILLIS_IN_TICK;

					sleep(sleepTime);
				}
			}
			catch(Exception e)
			{
				_error = e;
			}
		}
	}

	class TimerWatcher implements Runnable
	{
		@Override
		public void run()
		{
			if(!_timer.isAlive())
			{
				String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
				_log.warning(time + " TimerThread stop with following error. restart it.");
				if(_timer._error != null)
				{
					_timer._error.printStackTrace();
				}

				_timer = new TimerThread();
				_timer.start();
			}
		}
	}

	class MovingObjectArrived implements Runnable
	{
		private final List<L2Character> _ended;

		MovingObjectArrived(List<L2Character> ended)
		{
			_ended = ended;
		}

		@Override
		public void run()
		{
			for(L2Character cha : _ended)
			{
				try
				{
					cha.getKnownList().updateKnownObjects();
					cha.getAI().notifyEvent(CtrlEvent.EVT_ARRIVED);
				}
				catch(NullPointerException e)
				{}
			}
		}
	}

	class BroadcastSunState implements Runnable
	{
		@Override
		public void run()
		{
			int h = getGameTime() / 60 % 24;
			boolean tempIsNight = h < 6;

			if(tempIsNight != _isNight)
			{
				_isNight = tempIsNight;
				DayNightSpawnManager.getInstance().notifyChangeMode();
			}
		}
	}

}