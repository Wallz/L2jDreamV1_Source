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
package com.src.gameserver.model.extender;

import com.src.gameserver.model.L2Object;

public class BaseExtender
{
	public enum EventType
	{
		LOAD("load"),
		STORE("store"),
		CAST("cast"),
		ATTACK("attack"),
		CRAFT("craft"),
		ENCHANT("enchant"),
		SPAWN("spawn"),
		DELETE("delete"),
		SETOWNER("setwoner"),
		DROP("drop"),
		DIE("die"),
		REVIVE("revive"),
		SETINTENTION("setintention");
		public final String name;

		EventType(String name)
		{
			this.name = name;
		}
	}

	public static boolean canCreateFor(L2Object object)
	{
		return true;
	}

	protected L2Object _owner;
	private BaseExtender _next = null;

	public BaseExtender(L2Object owner)
	{
		_owner = owner;
	}

	public L2Object getOwner()
	{
		return _owner;
	}

	public Object onEvent(final String event, Object... params)
	{
		if(_next == null)
		{
			return null;
		}
		else
		{
			return _next.onEvent(event, params);
		}
	}

	public BaseExtender getExtender(final String simpleClassName)
	{
		if(this.getClass().getSimpleName().compareTo(simpleClassName) == 0)
		{
			return this;
		}
		else if(_next != null)
		{
			return _next.getExtender(simpleClassName);
		}
		else
		{
			return null;
		}
	}

	public void removeExtender(BaseExtender ext)
	{
		if(_next != null)
		{
			if(_next==ext)
			{
				_next = _next._next;
			}
			else
			{
				_next.removeExtender(ext);
			}
		}
	}
	public BaseExtender getNextExtender()
	{
		return _next;
	}

	public void addExtender(BaseExtender newExtender)
	{
		if(_next == null)
		{
			_next = newExtender;
		}
		else
		{
			_next.addExtender(newExtender);
		}
	}

}