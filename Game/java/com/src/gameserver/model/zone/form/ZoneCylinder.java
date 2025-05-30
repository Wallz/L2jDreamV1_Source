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
package com.src.gameserver.model.zone.form;

import com.src.gameserver.model.zone.L2ZoneForm;

public class ZoneCylinder extends L2ZoneForm
{
	private int _x, _y, _z1, _z2, _rad, _radS;

	public ZoneCylinder(int x, int y, int z1, int z2, int rad)
	{
		_x = x;
		_y = y;
		_z1 = z1;
		_z2 = z2;
		_rad = rad;
		_radS = rad * rad;
	}

	@Override
	public boolean isInsideZone(int x, int y, int z)
	{
		if(Math.pow(_x - x, 2) + Math.pow(_y - y, 2) > _radS || z < _z1 || z > _z2)
		{
			return false;
		}

		return true;
	}

	@Override
	public boolean intersectsRectangle(int ax1, int ax2, int ay1, int ay2)
	{
		if(_x > ax1 && _x < ax2 && _y > ay1 && _y < ay2)
		{
			return true;
		}

		if(Math.pow(ax1 - _x, 2) + Math.pow(ay1 - _y, 2) < _radS)
		{
			return true;
		}

		if(Math.pow(ax1 - _x, 2) + Math.pow(ay2 - _y, 2) < _radS)
		{
			return true;
		}

		if(Math.pow(ax2 - _x, 2) + Math.pow(ay1 - _y, 2) < _radS)
		{
			return true;
		}

		if(Math.pow(ax2 - _x, 2) + Math.pow(ay2 - _y, 2) < _radS)
		{
			return true;
		}

		if(_x > ax1 && _x < ax2)
		{
			if(Math.abs(_y - ay2) < _rad)
			{
				return true;
			}

			if(Math.abs(_y - ay1) < _rad)
			{
				return true;
			}
		}

		if(_y > ay1 && _y < ay2)
		{
			if(Math.abs(_x - ax2) < _rad)
			{
				return true;
			}

			if(Math.abs(_x - ax1) < _rad)
			{
				return true;
			}
		}

		return false;
	}

	@Override
	public double getDistanceToZone(int x, int y)
	{
		if(isInsideZone(x, y, _z1))
		{
			return 0;
		}

		return Math.sqrt((Math.pow(_x - x, 2) + Math.pow(_y - y, 2))) - _rad;
	}

	@Override
	public int getLowZ()
	{
		return _z1;
	}

	@Override
	public int getHighZ()
	{
		return _z2;
	}

}