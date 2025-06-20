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

public class ChangeMoveType extends L2GameServerPacket
{
	private static final String _S__3E_CHANGEMOVETYPE = "[S] 3E ChangeMoveType";

	public static final int WALK = 0;
	public static final int RUN = 1;

	private int _charObjId;
	private boolean _running;

	public ChangeMoveType(L2Character character)
	{
		_charObjId = character.getObjectId();
		_running = character.isRunning();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x2e);
		writeD(_charObjId);
		writeD(_running ? RUN : WALK);
		writeD(0);
	}

	@Override
	public String getType()
	{
		return _S__3E_CHANGEMOVETYPE;
	}

}