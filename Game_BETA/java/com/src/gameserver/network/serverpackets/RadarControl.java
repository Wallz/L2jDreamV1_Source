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

/**
 * 0xEB RadarControl ddddd
 */
public class RadarControl extends L2GameServerPacket
{
	private static final String S_EB_RadarControl = "[S] EB RadarControl";

	private int _showRadar;
	private int _type;
	private int _x, _y, _z;

	public RadarControl(int showRadar, int type, int x, int y, int z)
	{
		_showRadar = showRadar;
		_type = type;
		_x = x;
		_y = y;
		_z = z;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xEB);
		writeD(_showRadar);
		writeD(_type);
		writeD(_x);
		writeD(_y);
		writeD(_z);
	}

	@Override
	public String getType()
	{
		return S_EB_RadarControl;
	}

}