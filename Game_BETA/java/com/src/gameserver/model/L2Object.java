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
package com.src.gameserver.model;

import com.src.Config;
import com.src.gameserver.idfactory.IdFactory;
import com.src.gameserver.managers.ItemsOnGroundManager;
import com.src.gameserver.managers.MercTicketManager;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.knownlist.ObjectKnownList;
import com.src.gameserver.model.actor.poly.ObjectPoly;
import com.src.gameserver.model.actor.position.ObjectPosition;
import com.src.gameserver.model.extender.BaseExtender;
import com.src.gameserver.model.extender.BaseExtender.EventType;
import com.src.gameserver.network.L2GameClient;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.GetItem;

public abstract class L2Object
{
	private boolean _isVisible;
	private ObjectKnownList _knownList;
	private String _name;
	private int _objectId;
	private ObjectPoly _poly;
	private ObjectPosition _position;
	private int _instanceId = 0;
	private BaseExtender _extender = null;

	public L2Object(int objectId)
	{
		_objectId = objectId;
	}

	public void addExtender(BaseExtender newExtender)
	{
		if(_extender == null)
		{
			_extender = newExtender;
		}
		else
		{
			_extender.addExtender(newExtender);
		}
	}

	public BaseExtender getExtender(final String simpleName)
	{
		if(_extender == null)
		{
			return null;
		}
		else
		{
			return _extender.getExtender(simpleName);
		}
	}

	public Object fireEvent(final String event, Object... params)
	{
		if(_extender == null)
		{
			return null;
		}
		else
		{
			return _extender.onEvent(event, params);
		}
	}

	public void removeExtender(BaseExtender ext)
	{
		if(_extender!=null)
		{
			if(_extender==ext)
			{
				_extender = _extender.getNextExtender();
			}
			else
			{
				_extender.removeExtender(ext);
			}
		}
	}

	public void onAction(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	public void onActionShift(L2GameClient client)
	{
		client.getActiveChar().sendPacket(ActionFailed.STATIC_PACKET);
	}

	public void onActionShift(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	public void onForcedAttack(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	public void onSpawn()
	{
		fireEvent(EventType.SPAWN.name, (Object[]) null);
	}

	public final void setXYZ(int x, int y, int z)
	{
		getPosition().setXYZ(x, y, z);
	}

	public final void setXYZInvisible(int x, int y, int z)
	{
		getPosition().setXYZInvisible(x, y, z);
	}

	public final int getX()
	{
		return getPosition().getX();
	}

	public final int getY()
	{
		return getPosition().getY();
	}

	public final int getZ()
	{
		return getPosition().getZ();
	}

	public final void decayMe()
	{
		L2WorldRegion reg = getPosition().getWorldRegion();

		synchronized (this)
		{
			_isVisible = false;
			getPosition().setWorldRegion(null);
		}

		L2World.getInstance().removeVisibleObject(this, reg);
		L2World.getInstance().removeObject(this);

		if(Config.SAVE_DROPPED_ITEM)
		{
			ItemsOnGroundManager.getInstance().removeObject(this);
		}
		fireEvent(EventType.DELETE.name, (Object[]) null);
	}

	public final void pickupMe(L2Character player)
	{
		L2WorldRegion oldregion = getPosition().getWorldRegion();

		GetItem gi = new GetItem((L2ItemInstance) this, player.getObjectId());
		player.broadcastPacket(gi);
		gi = null;

		synchronized (this)
		{
			_isVisible = false;
			getPosition().setWorldRegion(null);
		}

		if(this instanceof L2ItemInstance)
		{
			int itemId = ((L2ItemInstance) this).getItemId();
			if(MercTicketManager.getInstance().getTicketCastleId(itemId) > 0)
			{
				MercTicketManager.getInstance().removeTicket((L2ItemInstance) this);
				ItemsOnGroundManager.getInstance().removeObject(this);
			}
		}

		L2World.getInstance().removeVisibleObject(this, oldregion);

		oldregion = null;
	}

	public void refreshID()
	{
		L2World.getInstance().removeObject(this);
		IdFactory.getInstance().releaseId(getObjectId());
		_objectId = IdFactory.getInstance().getNextId();
	}

	public final void spawnMe()
	{
		synchronized (this)
		{
			_isVisible = true;
			getPosition().setWorldRegion(L2World.getInstance().getRegion(getPosition().getWorldPosition()));

			L2World.storeObject(this);

			getPosition().getWorldRegion().addVisibleObject(this);
		}

		L2World.getInstance().addVisibleObject(this, getPosition().getWorldRegion(), null);

		onSpawn();
	}

	public final void spawnMe(int x, int y, int z)
	{
		synchronized (this)
		{
			_isVisible = true;

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

			getPosition().setWorldPosition(x, y, z);
			getPosition().setWorldRegion(L2World.getInstance().getRegion(getPosition().getWorldPosition()));
		}

		L2World.storeObject(this);

		getPosition().getWorldRegion().addVisibleObject(this);

		L2World.getInstance().addVisibleObject(this, getPosition().getWorldRegion(), null);

		onSpawn();
	}

	public void toggleVisible()
	{
		if(isVisible())
		{
			decayMe();
		}
		else
		{
			spawnMe();
		}
	}

	public boolean isAttackable()
	{
		return false;
	}

	public abstract boolean isAutoAttackable(L2Character attacker);

	public boolean isMarker()
	{
		return false;
	}

	public final boolean isVisible()
	{
		return getPosition().getWorldRegion() != null;
	}

	public final void setIsVisible(boolean value)
	{
		_isVisible = value;

		if(!_isVisible)
		{
			getPosition().setWorldRegion(null);
		}
	}

	public ObjectKnownList getKnownList()
	{
		if(_knownList == null)
		{
			_knownList = new ObjectKnownList(this);
		}

		return _knownList;
	}

	public final void setKnownList(ObjectKnownList value)
	{
		_knownList = value;
	}

	public final String getName()
	{
		return _name;
	}

	public final void setName(String value)
	{
		_name = value;
	}

	public final int getObjectId()
	{
		return _objectId;
	}

	public final ObjectPoly getPoly()
	{
		if(_poly == null)
		{
			_poly = new ObjectPoly(this);
		}

		return _poly;
	}

	public final ObjectPosition getPosition()
	{
		if(_position == null)
		{
			_position = new ObjectPosition(this);
		}

		return _position;
	}

	public L2WorldRegion getWorldRegion()
	{
		return getPosition().getWorldRegion();
	}

	public int getInstanceId()
	{
		return _instanceId;
	}

	public void setInstanceId(int instanceId)
	{
		_instanceId = instanceId;

		if(_isVisible && _knownList != null)
		{
			if(this instanceof L2PcInstance)
			{
			}
			else
			{
				decayMe();
				spawnMe();
			}
		}
	}

	@Override
	public String toString()
	{
		return "" + getObjectId();
	}

	public L2PcInstance getActingPlayer()
	{
		return null;
	}

	public L2PcInstance getPlayer()
	{
		return null;
	}
}