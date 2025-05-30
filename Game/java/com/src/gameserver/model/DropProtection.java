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

import java.util.concurrent.ScheduledFuture;

import com.src.Config;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.thread.ThreadPoolManager;

public class DropProtection implements Runnable
{
	private volatile boolean _isProtected = false;
	private L2PcInstance _owner = null;
	private ScheduledFuture<?> _task = null;

	private static final long PROTECTED_MILLIS_TIME = Config.DROP_PROTECTED_TIME * 1000;

	@Override
	public synchronized void run()
	{
		_isProtected = false;
		_owner = null;
		_task = null;
	}

	public boolean isProtected()
	{
		return _isProtected;
	}

	public L2PcInstance getOwner()
	{
		return _owner;
	}

	public synchronized boolean tryPickUp(L2PcInstance actor)
	{
		if(!_isProtected)
		{
			return true;
		}

		if(_owner == actor)
		{
			return true;
		}

		if(_owner.getParty() != null && _owner.getParty() == actor.getParty())
		{
			return true;
		}

		if(_owner.getClan() != null && _owner.getClan() == actor.getClan())
		{
			return true;
		}

		return false;
	}

	public boolean tryPickUp(L2PetInstance pet)
	{
		return tryPickUp(pet.getOwner());
	}

	public synchronized void unprotect()
	{
		if(_task != null)
		{
			_task.cancel(false);
		}
		_isProtected = false;
		_owner = null;
		_task = null;
	}

	public synchronized void protect(L2PcInstance player)
	{
		unprotect();

		_isProtected = true;

		if((_owner = player) == null)
		{
			throw new NullPointerException("Trying to protect dropped item to null owner");
		}

		_task = ThreadPoolManager.getInstance().scheduleGeneral(this, PROTECTED_MILLIS_TIME);
	}

}