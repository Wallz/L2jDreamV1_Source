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
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.ExPartyRoomMember;
import com.src.gameserver.network.serverpackets.PartyMatchDetail;
import com.src.gameserver.network.serverpackets.PartyMatchList;
import com.src.gameserver.network.serverpackets.SystemMessage;

public final class RequestPartyMatchConfig extends L2GameClientPacket
{
	private static final String _C__6F_REQUESTPARTYMATCHCONFIG = "[C] 6F RequestPartyMatchConfig";

	private int _auto, _loc, _lvl;

	@Override
	protected void readImpl()
	{
		_auto = readD();	// ?
		_loc = readD();		// Location
		_lvl = readD();		// my level
	}

	@Override
	protected void runImpl()
	{
		if(getClient().getActiveChar() == null)
			return;

		L2PcInstance _activeChar = getClient().getActiveChar();

		if( !_activeChar.isInPartyMatchRoom() && _activeChar.getParty() != null && _activeChar.getParty().getLeader() != _activeChar)
		{
			_activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_VIEW_PARTY_ROOMS));
			_activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(_activeChar.isInPartyMatchRoom())
		{
			// If Player is in Room show him room, not list
			PartyMatchRoomList _list = PartyMatchRoomList.getInstance();
			if(_list==null)
				return;

			PartyMatchRoom _room = _list.getPlayerRoom(_activeChar);
			if(_room == null)
				return;

			_activeChar.sendPacket(new PartyMatchDetail(_activeChar,_room));
			_activeChar.sendPacket(new ExPartyRoomMember(_activeChar, _room, 2));

			_activeChar.setPartyRoom(_room.getId());
			_activeChar.broadcastUserInfo();
		}
		else
		{
			// Add to waiting list
			PartyMatchWaitingList.getInstance().addPlayer(_activeChar);

			// Send Room list
			PartyMatchList matchList = new PartyMatchList(_activeChar,_auto,_loc,_lvl);

			_activeChar.sendPacket(matchList);
		}
	}

	@Override
	public String getType()
	{
		return _C__6F_REQUESTPARTYMATCHCONFIG;
	}

}
