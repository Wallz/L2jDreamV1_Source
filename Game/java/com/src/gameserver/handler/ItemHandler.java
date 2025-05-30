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
package com.src.gameserver.handler;

import java.util.Map;
import java.util.TreeMap;

public class ItemHandler
{
	private static ItemHandler _instance;

	private Map<Integer, IItemHandler> _datatable;

	public static ItemHandler getInstance()
	{
		if(_instance == null)
		{
			_instance = new ItemHandler();
		}

		return _instance;
	}

	public int size()
	{
		return _datatable.size();
	}

	private ItemHandler()
	{
		_datatable = new TreeMap<Integer, IItemHandler>();
	}

	public void registerItemHandler(IItemHandler handler)
	{
		int[] ids = handler.getItemIds();

		for(int id : ids)
		{
			_datatable.put(new Integer(id), handler);
		}
	}

	public IItemHandler getItemHandler(int itemId)
	{
		return _datatable.get(new Integer(itemId));
	}
}