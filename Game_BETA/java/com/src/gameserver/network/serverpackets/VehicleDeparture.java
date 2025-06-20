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
package com.src.gameserver.network.serverpackets;

import com.src.gameserver.model.actor.instance.L2BoatInstance;

public class VehicleDeparture extends L2GameServerPacket
{

	private L2BoatInstance _boat;
	private int _speed1;
	private int _speed2;
	private int _x;
	private int _y;
	private int _z;

	public VehicleDeparture(L2BoatInstance boat, int speed1, int speed2, int x, int y, int z)
	{
		_boat = boat;
		_speed1 = speed1;
		_speed2 = speed2;
		_x = x;
		_y = y;
		_z = z;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0x5a);
		writeD(_boat.getObjectId());
		writeD(_speed1);
		writeD(_speed2);
		writeD(_x);
		writeD(_y);
		writeD(_z);
	}

	@Override
	public String getType()
	{
		return "[S] 5A VehicleDeparture";
	}

}