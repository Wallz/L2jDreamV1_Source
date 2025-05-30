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
package com.src.util.object;

import java.util.Iterator;

import com.src.Config;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.L2Playable;

public abstract class L2ObjectSet<T extends L2Object> implements Iterable<T>
{
	public abstract int size();
	public abstract boolean isEmpty();
	public abstract void clear();
	public abstract void put(T obj);
	public abstract void remove(T obj);
	public abstract boolean contains(T obj);
	@Override
	public abstract Iterator<T> iterator();

	public static L2ObjectSet<L2Object> createL2ObjectSet()
	{
		switch(Config.SET_TYPE)
		{
			case WorldObjectSet:
			{
				return new WorldObjectSet<L2Object>();
			}
			default:
			{
				return new L2ObjectHashSet<L2Object>();
			}
		}
	}

	public static L2ObjectSet<L2Playable> createL2PlayerSet()
	{
		switch(Config.SET_TYPE)
		{
			case WorldObjectSet:
			{
				return new WorldObjectSet<L2Playable>();
			}
			default:
			{
				return new L2ObjectHashSet<L2Playable>();
			}
		}
	}

}