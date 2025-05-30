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
package com.src.gameserver.script;

import java.util.Date;
import java.util.List;

import javolution.util.FastList;

public class EventDroplist
{
	private static EventDroplist _instance;

	private List<DateDrop> _allNpcDateDrops;

	public static EventDroplist getInstance()
	{
		if(_instance == null)
		{
			_instance = new EventDroplist();
		}

		return _instance;
	}

	public class DateDrop
	{
		public DateRange dateRange;
		public int[] items;
		public int min;
		public int max;
		public int chance;
	}

	private EventDroplist()
	{
		_allNpcDateDrops = new FastList<DateDrop>();
	}

	public void addGlobalDrop(int[] items, int[] count, int chance, DateRange range)
	{
		DateDrop date = new DateDrop();

		date.dateRange = range;
		date.items = items;
		date.min = count[0];
		date.max = count[1];
		date.chance = chance;

		_allNpcDateDrops.add(date);
		date = null;
	}

	public List<DateDrop> getAllDrops()
	{
		List<DateDrop> list = new FastList<DateDrop>();

		for(DateDrop drop : _allNpcDateDrops)
		{
			Date currentDate = new Date();

			if(drop.dateRange.isWithinRange(currentDate))
			{
				list.add(drop);
			}
			currentDate = null;
		}
		return list;
	}

}