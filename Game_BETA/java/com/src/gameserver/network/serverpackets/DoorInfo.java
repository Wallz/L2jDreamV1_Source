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

import com.src.gameserver.model.actor.instance.L2DoorInstance;

public class DoorInfo extends L2GameServerPacket
{
	private static final String _S__60_DOORINFO = "[S] 4c DoorInfo";

	private L2DoorInstance		_door;
	private final boolean _showHp;

	public DoorInfo(L2DoorInstance door, boolean showHp)
	{
		_door = door;
		_showHp = showHp;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x4c);
		writeD(_door.getObjectId());
		writeD(_door.getDoorId());
		writeD(1);
		writeD(1);
		writeD(_door.isEnemy() ? 1 : 0);
		writeD((int)_door.getCurrentHp());
		writeD(_door.getMaxHp());
		writeD(_showHp ? 1 : 0);
		writeD(_door.getDamage());
	}

	@Override
	public String getType()
	{
		return _S__60_DOORINFO;
	}

}