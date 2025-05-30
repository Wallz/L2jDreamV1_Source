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

import java.util.logging.Logger;

import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.L2WorldRegion;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.thread.ThreadPoolManager;

public class KnownListUpdateTaskManager
{
	protected static final Logger _log = Logger.getLogger(DecayTaskManager.class.getName());

	private static KnownListUpdateTaskManager _instance;

	public KnownListUpdateTaskManager()
	{
		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new KnownListUpdate(), 1000, 750);
	}

	public static KnownListUpdateTaskManager getInstance()
	{
		if(_instance == null)
		{
			_instance = new KnownListUpdateTaskManager();
		}

		return _instance;
	}

	private class KnownListUpdate implements Runnable
	{
		boolean toggle = false;
		boolean fullUpdate = true;

		protected KnownListUpdate()
		{}

		@Override
		public void run()
		{
			try
			{
				for(L2WorldRegion regions[] : L2World.getInstance().getAllWorldRegions())
				{
					for(L2WorldRegion r : regions)
					{
						if(r.isActive())
						{
							updateRegion(r, fullUpdate, toggle);
						}
					}
				}
				if(toggle)
				{
					toggle = false;
				}
				else
				{
					toggle = true;
				}
				if(fullUpdate)
				{
					fullUpdate = false;
				}
			}
			catch(Throwable e)
			{
				_log.warning(e.toString());
			}
		}
	}

	public void updateRegion(L2WorldRegion region, boolean fullUpdate, boolean forgetObjects)
	{
		for(L2Object object : region.getVisibleObjects())
		{
			if(!object.isVisible())
			{
				continue;
			}

			if(forgetObjects)
			{
				object.getKnownList().forgetObjects();
				continue;
			}
			if(object instanceof L2Playable || fullUpdate)
			{
				for(L2WorldRegion regi : region.getSurroundingRegions())
				{
					for(L2Object _object : regi.getVisibleObjects())
					{
						if(_object != object)
						{
							object.getKnownList().addKnownObject(_object);
						}
					}
				}
			}
			else if(object instanceof L2Character)
			{
				for(L2WorldRegion regi : region.getSurroundingRegions())
				{
					if(regi.isActive())
					{
						for(L2Object _object : regi.getVisibleObjects())
						{
							if(_object != object)
							{
								object.getKnownList().addKnownObject(_object);
							}
						}
					}
				}
			}
		}
	}

}