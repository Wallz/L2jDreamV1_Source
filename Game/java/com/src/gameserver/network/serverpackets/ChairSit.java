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

import com.src.gameserver.model.actor.instance.L2PcInstance;

public class ChairSit extends L2GameServerPacket
{
	private static final String _S__e1_CHAIRSIT = "[S] e1 ChairSit";

	private L2PcInstance _activeChar;
	private int _staticObjectId;

	public ChairSit(L2PcInstance player, int staticObjectId)
	{
		_activeChar = player;
		_staticObjectId = staticObjectId;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xe1);
		writeD(_activeChar.getObjectId());
		writeD(_staticObjectId);
	}

	@Override
	public String getType()
	{
		return _S__e1_CHAIRSIT;
	}

}