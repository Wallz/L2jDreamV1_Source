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

public class OnVehicleCheckLocation extends L2GameServerPacket
{
	private L2BoatInstance _boat;
	private int _x;
	private int _y;
	private int _z;

	public OnVehicleCheckLocation(L2BoatInstance instance, int x, int y, int z)
	{
		_boat = instance;
		_x = x;
		_y = y;
		_z = z;
	}

	@Override
	protected void writeImpl()
	{

		writeC(0x5b);
		writeD(_boat.getObjectId());
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(_boat.getPosition().getHeading());
	}

	@Override
	public String getType()
	{
		return null;
	}

}