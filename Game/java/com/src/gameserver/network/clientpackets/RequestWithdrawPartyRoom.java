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
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ExClosePartyRoom;
import com.src.gameserver.network.serverpackets.SystemMessage;

/**
 * Format (ch) dd
 */
public final class RequestWithdrawPartyRoom extends L2GameClientPacket
{
	private static final String _C__D0_02_REQUESTWITHDRAWPARTYROOM = "[C] D0:02 RequestWithdrawPartyRoom";

	private int _roomid;
	@SuppressWarnings("unused")
	private int _unk1;

	@Override
	protected void readImpl()
	{
		_roomid = readD();
		_unk1 = readD();
	}

	@Override
	protected void runImpl()
	{
		if(getClient().getActiveChar() == null)
			return;

		L2PcInstance _activeChar = getClient().getActiveChar();

		PartyMatchRoom _room = PartyMatchRoomList.getInstance().getRoom(_roomid);
		if(_room == null)
			return;

		if((_activeChar.isInParty() && _room.getOwner().isInParty()) && (_activeChar.getParty().getPartyLeaderOID() == _room.getOwner().getParty().getPartyLeaderOID()))
		{
			_activeChar.broadcastUserInfo();
		}
		else
		{
			_room.deleteMember(_activeChar);
			
			_activeChar.setPartyRoom(0);
			
			_activeChar.sendPacket(new ExClosePartyRoom());
			_activeChar.sendPacket(new SystemMessage(SystemMessageId.PARTY_ROOM_EXITED));
		}
	}

	@Override
	public String getType()
	{
		return _C__D0_02_REQUESTWITHDRAWPARTYROOM;
	}

}
