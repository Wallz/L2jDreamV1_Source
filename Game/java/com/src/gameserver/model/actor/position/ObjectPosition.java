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
package com.src.gameserver.model.actor.position;

import java.util.logging.Logger;

import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.L2WorldRegion;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.util.object.Point3D;

public class ObjectPosition
{
	private static final Logger _log = Logger.getLogger(ObjectPosition.class.getName());

	private L2Object _activeObject;
	private int _heading = 0;
	private Point3D _worldPosition;
	private L2WorldRegion _worldRegion;

	public ObjectPosition(L2Object activeObject)
	{
		_activeObject = activeObject;
		setWorldRegion(L2World.getInstance().getRegion(getWorldPosition()));
	}

	public final void setXYZ(int x, int y, int z)
	{
		setWorldPosition(x, y, z);

		try
		{
			if(L2World.getInstance().getRegion(getWorldPosition()) != getWorldRegion())
			{
				updateWorldRegion();
			}
		}
		catch(Exception e)
		{
			_log.warning("Object Id at bad coords: (x: " + getX() + ", y: " + getY() + ", z: " + getZ() + ").");

			if(getActiveObject() instanceof L2Character)
			{
				getActiveObject().decayMe();
			}
			else if(getActiveObject() instanceof L2PcInstance)
			{
				((L2PcInstance) getActiveObject()).teleToLocation(0, 0, 0, false);
				((L2PcInstance) getActiveObject()).sendMessage("Error with your coords, Please ask a GM for help!");
			}
		}
	}

	public final void setXYZInvisible(int x, int y, int z)
	{
		if(x > L2World.MAP_MAX_X)
		{
			x = L2World.MAP_MAX_X - 5000;
		}

		if(x < L2World.MAP_MIN_X)
		{
			x = L2World.MAP_MIN_X + 5000;
		}

		if(y > L2World.MAP_MAX_Y)
		{
			y = L2World.MAP_MAX_Y - 5000;
		}

		if(y < L2World.MAP_MIN_Y)
		{
			y = L2World.MAP_MIN_Y + 5000;
		}

		setWorldPosition(x, y, z);
		getActiveObject().setIsVisible(false);
	}

	public void updateWorldRegion()
	{
		if(!getActiveObject().isVisible())
		{
			return;
		}

		L2WorldRegion newRegion = L2World.getInstance().getRegion(getWorldPosition());
		if(newRegion != getWorldRegion())
		{
			getWorldRegion().removeVisibleObject(getActiveObject());

			setWorldRegion(newRegion);

			getWorldRegion().addVisibleObject(getActiveObject());
		}

		newRegion = null;
	}

	public final L2Object getActiveObject()
	{
		return _activeObject;
	}

	public final int getHeading()
	{
		return _heading;
	}

	public final void setHeading(int value)
	{
		_heading = value;
	}

	public final int getX()
	{
		return getWorldPosition().getX();
	}

	public final void setX(int value)
	{
		getWorldPosition().setX(value);
	}

	public final int getY()
	{
		return getWorldPosition().getY();
	}

	public final void setY(int value)
	{
		getWorldPosition().setY(value);
	}

	public final int getZ()
	{
		return getWorldPosition().getZ();
	}

	public final void setZ(int value)
	{
		getWorldPosition().setZ(value);
	}

	public final Point3D getWorldPosition()
	{
		if(_worldPosition == null)
		{
			_worldPosition = new Point3D(0, 0, 0);
		}

		return _worldPosition;
	}

	public final void setWorldPosition(int x, int y, int z)
	{
		getWorldPosition().setXYZ(x, y, z);
	}

	public final void setWorldPosition(Point3D newPosition)
	{
		setWorldPosition(newPosition.getX(), newPosition.getY(), newPosition.getZ());
	}

	public final L2WorldRegion getWorldRegion()
	{
		return _worldRegion;
	}

	public final void setWorldRegion(L2WorldRegion value)
	{
		_worldRegion = value;
	}

}