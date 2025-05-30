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
package com.src.gameserver.managers;

import javolution.util.FastList;

import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.zone.type.L2ArenaZone;

public class ArenaManager
{
	private static ArenaManager _instance;

	public static final ArenaManager getInstance()
	{
		if(_instance == null)
		{
			_instance = new ArenaManager();
		}
		return _instance;
	}

	private FastList<L2ArenaZone> _arenas;

	public ArenaManager()
	{}

	public void addArena(L2ArenaZone arena)
	{
		if(_arenas == null)
		{
			_arenas = new FastList<L2ArenaZone>();
		}

		_arenas.add(arena);
	}

	public final L2ArenaZone getArena(L2Character character)
	{
		if(_arenas != null)
		{
			for(L2ArenaZone temp : _arenas)
			{
				if(temp.isCharacterInZone(character))
				{
					return temp;
				}
			}
		}

		return null;
	}

	public final L2ArenaZone getArena(int x, int y, int z)
	{
		if(_arenas != null)
		{
			for(L2ArenaZone temp : _arenas)
			{
				if(temp.isInsideZone(x, y, z))
				{
					return temp;
				}
			}
		}

		return null;
	}

}