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

import com.src.gameserver.model.L2World;
import com.src.gameserver.model.PartyMatchRoom;
import com.src.gameserver.model.PartyMatchRoomList;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ExClosePartyRoom;
import com.src.gameserver.network.serverpackets.SystemMessage;

/**
 * format (ch) d
 */
public final class RequestOustFromPartyRoom extends L2GameClientPacket
{
	private static final String _C__D0_01_REQUESTOUSTFROMPARTYROOM = "[C] D0:01 RequestOustFromPartyRoom";

	private int _charid;

	@Override
	protected void readImpl()
	{
		_charid = readD();
	}

	@Override
	protected void runImpl()
	{
		if(getClient().getActiveChar() == null)
			return;

		L2PcInstance activeChar = getClient().getActiveChar();

		L2PcInstance member = L2World.getInstance().getPlayer(_charid);
		if(member == null)
			return;

		PartyMatchRoom _room = PartyMatchRoomList.getInstance().getPlayerRoom(member);
		if(_room == null)
			return;

		if(_room.getOwner() != activeChar)
			return;

		if(activeChar.isInParty() && member.isInParty() && activeChar.getParty().getPartyLeaderOID() == member.getParty().getPartyLeaderOID())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISMISS_PARTY_MEMBER));
		}
		else
		{
			_room.deleteMember(member);
			member.setPartyRoom(0);
			member.sendPacket(new ExClosePartyRoom());
			member.sendPacket(new SystemMessage(SystemMessageId.OUSTED_FROM_PARTY_ROOM));
		}
	}

	@Override
	public String getType()
	{
		return _C__D0_01_REQUESTOUSTFROMPARTYROOM;
	}

}