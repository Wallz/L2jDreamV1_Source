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
package com.src.gameserver.network.clientpackets;

import com.src.gameserver.model.PartyMatchRoom;
import com.src.gameserver.model.PartyMatchRoomList;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class RequestDismissPartyRoom extends L2GameClientPacket
{
	private static final String _C__D0_02_REQUESTDISMISSPARTYROOM = "[C] D0:02 RequestDismissPartyRoom";

	private int _roomid;
	@SuppressWarnings("unused")
	private int _data2;

	@Override
	protected void readImpl()
	{
		_roomid = readD();
		_data2 = readD();
	}

	@Override
	protected void runImpl()
	{
		final L2PcInstance _activeChar = getClient().getActiveChar();

		if(_activeChar == null)
		{
			return;
		}

		PartyMatchRoom _room = PartyMatchRoomList.getInstance().getRoom(_roomid);

		if(_room == null)
		{
			return;
		}

		PartyMatchRoomList.getInstance().deleteRoom(_roomid);
	}

	@Override
	public String getType()
	{
		return _C__D0_02_REQUESTDISMISSPARTYROOM;
	}

}