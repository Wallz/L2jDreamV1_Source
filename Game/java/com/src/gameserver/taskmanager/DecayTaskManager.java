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
package com.src.gameserver.taskmanager;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import javolution.util.FastMap;

import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.thread.ThreadPoolManager;

public class DecayTaskManager
{
	protected static final Logger _log = Logger.getLogger(DecayTaskManager.class.getName());
	protected Map<L2Character, Long> _decayTasks = new FastMap<L2Character, Long>().shared();

	public static final int RAID_BOSS_DECAY_TIME = 30000; 
	public static final int ATTACKABLE_DECAY_TIME = 8500;
	
	private static DecayTaskManager _instance;

	public DecayTaskManager()
	{
		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new DecayScheduler(), 10000, 5000);
	}

	public static DecayTaskManager getInstance()
	{
		if(_instance == null)
		{
			_instance = new DecayTaskManager();
		}

		return _instance;
	}

	public void addDecayTask(L2Character actor)
	{
		_decayTasks.put(actor, System.currentTimeMillis());
	}

	public void addDecayTask(L2Character actor, int interval)
	{
		_decayTasks.put(actor, System.currentTimeMillis() + interval);
	}

	public void cancelDecayTask(L2Character actor)
	{
		try
		{
			_decayTasks.remove(actor);
		}
		catch(NoSuchElementException e)
		{
		}
	}

	private class DecayScheduler implements Runnable
	{
		protected DecayScheduler()
		{
		}

		@Override
		public void run()
		{
			Long current = System.currentTimeMillis();
			int delay;
			try
			{
				if(_decayTasks != null)
				{
					for(L2Character actor : _decayTasks.keySet())
					{
						if(actor.isRaid())  
							delay = RAID_BOSS_DECAY_TIME; 
						else  
							delay = ATTACKABLE_DECAY_TIME;
						
						if(current - _decayTasks.get(actor) > delay)
						{
							actor.onDecay();
							_decayTasks.remove(actor);
						}
					}
				}
			}
			catch(Throwable e)
			{
				_log.warning(e.toString());
			}
		}
	}

	@Override
	public String toString()
	{
		String ret = "============= DecayTask Manager Report ============\r\n";
		ret += "Tasks count: " + _decayTasks.size() + "\r\n";
		ret += "Tasks dump:\r\n";

		Long current = System.currentTimeMillis();
		for(L2Character actor : _decayTasks.keySet())
		{
			ret += "Class/Name: " + actor.getClass().getSimpleName() + "/" + actor.getName() + " decay timer: " + (current - _decayTasks.get(actor)) + "\r\n";
		}

		return ret;
	}
	
	public Map<L2Character, Long> getTasks() 
	{ 
		return _decayTasks; 
	}

}