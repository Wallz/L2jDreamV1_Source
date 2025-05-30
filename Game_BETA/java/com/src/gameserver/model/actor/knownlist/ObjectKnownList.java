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
package com.src.gameserver.model.actor.knownlist;

import java.util.Collection;
import java.util.Map;

import javolution.util.FastMap;

import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2BoatInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.util.Util;

public class ObjectKnownList
{
	private L2Object _activeObject;
	private Map<Integer, L2Object> _knownObjects;

	public ObjectKnownList(L2Object activeObject)
	{
		_activeObject = activeObject;
	}

	public boolean addKnownObject(L2Object object)
	{
		return addKnownObject(object, null);
	}

	public boolean addKnownObject(L2Object object, L2Character dropper)
	{
		if(object == null || object == getActiveObject())
		{
			return false;
		}

		if(knowsObject(object))
		{
			if(!object.isVisible())
			{
				removeKnownObject(object);
			}
			return false;
		}

		if(!Util.checkIfInRange(getDistanceToWatchObject(object), getActiveObject(), object, true))
		{
			return false;
		}

		return getKnownObjects().put(object.getObjectId(), object) == null;
	}

	public final boolean knowsObject(L2Object object)
	{
		if(object == null)
		{
			return false;
		}

		return getActiveObject() == object || getKnownObjects().containsKey(object.getObjectId());
	}

	public void removeAllKnownObjects()
	{
		getKnownObjects().clear();
	}

	public boolean removeKnownObject(L2Object object)
	{
		if(object == null)
		{
			return false;
		}

		return getKnownObjects().remove(object.getObjectId()) != null;
	}

	public final synchronized void updateKnownObjects()
	{
		if(getActiveObject() instanceof L2Character)
		{
			findCloseObjects();
			forgetObjects();
		}
	}

	private final void findCloseObjects()
	{
		boolean isActiveObjectPlayable = getActiveObject() instanceof L2Playable;

		if(isActiveObjectPlayable)
		{
			Collection<L2Object> objects = L2World.getInstance().getVisibleObjects(getActiveObject());

			if(objects == null)
			{
				return;
			}
			for(L2Object object : objects)
			{
				if(object == null)
				{
					continue;
				}

				addKnownObject(object);

				if(object instanceof L2Character)
				{
					object.getKnownList().addKnownObject(getActiveObject());
				}
			}

			objects = null;
		}
		else
		{
			Collection<L2Playable> playables = L2World.getInstance().getVisiblePlayable(getActiveObject());

			if(playables == null)
			{
				return;
			}
			for(L2Object playable : playables)
			{
				if(playable == null)
				{
					continue;
				}

				addKnownObject(playable);
			}

			playables = null;
		}
	}

	public final void forgetObjects()
	{
		Collection<L2Object> knownObjects = getKnownObjects().values();

		if(knownObjects == null || knownObjects.size() == 0)
		{
			return;
		}

		for(L2Object object : knownObjects)
		{
			if(object == null)
			{
				continue;
			}

			if(!object.isVisible() || !Util.checkIfInRange(getDistanceToForgetObject(object), getActiveObject(), object, true))
			{
				if(object instanceof L2BoatInstance && getActiveObject() instanceof L2PcInstance)
				{
					if(((L2BoatInstance) object).getVehicleDeparture() == null)
					{
					}
					else if(((L2PcInstance) getActiveObject()).isInBoat())
					{
						if(((L2PcInstance) getActiveObject()).getBoat() == object)
						{
						}
						else
						{
							removeKnownObject(object);
						}
					}
					else
					{
						removeKnownObject(object);
					}
				}
				else
				{
					removeKnownObject(object);
				}
			}
		}

		knownObjects = null;
	}

	public L2Object getActiveObject()
	{
		return _activeObject;
	}

	public int getDistanceToForgetObject(L2Object object)
	{
		return 0;
	}

	public int getDistanceToWatchObject(L2Object object)
	{
		return 0;
	}

	public final Map<Integer, L2Object> getKnownObjects()
	{
		if(_knownObjects == null)
		{
			_knownObjects = new FastMap<Integer, L2Object>().shared();
		}

		return _knownObjects;
	}

	public static class KnownListAsynchronousUpdateTask implements Runnable
	{
		private L2Object _obj;

		public KnownListAsynchronousUpdateTask(L2Object obj)
		{
			_obj = obj;
		}

		@Override
		public void run()
		{
			if(_obj != null)
			{
				_obj.getKnownList().updateKnownObjects();
			}
		}
	}

}