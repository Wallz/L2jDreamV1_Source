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

import com.src.gameserver.model.actor.L2Character;

public class FinishRotation extends L2GameServerPacket
{
	private static final String _S__78_FINISHROTATION = "[S] 63 FinishRotation";
	private int _heading;
	private int _charObjId;

	public FinishRotation(L2Character cha)
	{
		_charObjId = cha.getObjectId();
		_heading = cha.getHeading();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x63);
		writeD(_charObjId);
		writeD(_heading);
	}

	@Override
	public String getType()
	{
		return _S__78_FINISHROTATION;
	}

}