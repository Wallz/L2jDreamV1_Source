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

import com.src.gameserver.model.zone.type.L2FishingZone;
import com.src.gameserver.model.zone.type.L2WaterZone;

public class FishingZoneManager
{
	private static FishingZoneManager _instance;

	public static final FishingZoneManager getInstance()
	{
		if(_instance == null)
		{
			_instance = new FishingZoneManager();
		}
		return _instance;
	}

	private FastList<L2FishingZone> _fishingZones;
	private FastList<L2WaterZone> _waterZones;

	public FishingZoneManager()
	{}

	public void addFishingZone(L2FishingZone fishingZone)
	{
		if(_fishingZones == null)
		{
			_fishingZones = new FastList<L2FishingZone>();
		}

		_fishingZones.add(fishingZone);
	}

	public void addWaterZone(L2WaterZone waterZone)
	{
		if(_waterZones == null)
		{
			_waterZones = new FastList<L2WaterZone>();
		}

		_waterZones.add(waterZone);
	}

	public final L2FishingZone isInsideFishingZone(int x, int y, int z)
	{
		for(L2FishingZone temp : _fishingZones)
		{
			if(temp.isInsideZone(x, y, temp.getWaterZ() - 10))
			{
				return temp;
			}
		}
		return null;
	}

	public final L2WaterZone isInsideWaterZone(int x, int y, int z)
	{
		for(L2WaterZone temp : _waterZones)
		{
			if(temp.isInsideZone(x, y, temp.getWaterZ()))
			{
				return temp;
			}
		}
		return null;
	}

}