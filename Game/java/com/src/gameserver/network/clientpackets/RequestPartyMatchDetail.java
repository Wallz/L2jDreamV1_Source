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
import com.src.gameserver.model.PartyMatchWaitingList;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ExManagePartyRoomMember;
import com.src.gameserver.network.serverpackets.ExPartyRoomMember;
import com.src.gameserver.network.serverpackets.PartyMatchDetail;
import com.src.gameserver.network.serverpackets.SystemMessage;

public final class RequestPartyMatchDetail extends L2GameClientPacket
{
	private static final String _C__71_REQUESTPARTYMATCHDETAIL = "[C] 71 RequestPartyMatchDetail";

	private int _roomid;
	@SuppressWarnings("unused")
	private int _unk1;
	@SuppressWarnings("unused")
	private int _unk2;
	@SuppressWarnings("unused")
	private int _unk3;

	@Override
	protected void readImpl()
	{
		_roomid = readD();
		_unk1 = readD();
		_unk2 = readD();
		_unk3 = readD();
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

		if((_activeChar.getLevel() >= _room.getMinLvl()) && (_activeChar.getLevel() <= _room.getMaxLvl()))
		{
			// Remove from waiting list
			PartyMatchWaitingList.getInstance().removePlayer(_activeChar);

			_activeChar.setPartyRoom(_roomid);

			_activeChar.sendPacket(new PartyMatchDetail(_activeChar, _room));
			_activeChar.sendPacket(new ExPartyRoomMember(_activeChar, _room, 0));

			for(L2PcInstance _member : _room.getPartyMembers())
			{
				if(_member == null)
					continue;

				_member.sendPacket(new ExManagePartyRoomMember(_activeChar, _room, 0));
				_member.sendPacket(new SystemMessage(SystemMessageId.S1_ENTERED_PARTY_ROOM).addString(_activeChar.getName()));
			}

			_room.addMember(_activeChar);

			// Info Broadcast
			_activeChar.broadcastUserInfo();
		}
		else
		{
			_activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_ENTER_PARTY_ROOM));
		}
	}

	@Override
	public String getType()
	{
		return _C__71_REQUESTPARTYMATCHDETAIL;
	}

}
