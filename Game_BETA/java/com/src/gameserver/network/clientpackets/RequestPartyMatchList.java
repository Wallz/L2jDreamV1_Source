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

import java.util.logging.Logger;

import com.src.gameserver.model.PartyMatchRoom;
import com.src.gameserver.model.PartyMatchRoomList;
import com.src.gameserver.model.PartyMatchWaitingList;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ExPartyRoomMember;
import com.src.gameserver.network.serverpackets.PartyMatchDetail;
import com.src.gameserver.network.serverpackets.SystemMessage;

/**
 * Packetformat Rev650 cdddddS
 */
public class RequestPartyMatchList extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestPartyMatchList.class.getName());

	private static final String _C__70_REQUESTPARTYMATCHLIST = "[C] 70 RequestPartyMatchList";

	private int _roomid;
	private int _membersmax;
	private int _lvlmin;
	private int _lvlmax;
	private int _loot;
	private String _roomtitle;

	@Override
	protected void readImpl()
	{
		_roomid = readD();
		_membersmax = readD();
		_lvlmin = readD();
		_lvlmax = readD();
		_loot = readD();
		_roomtitle = readS();
	}

	@Override
	protected void runImpl()
	{
		if(getClient().getActiveChar() == null)
			return;

		L2PcInstance _activeChar = getClient().getActiveChar();

		if(_roomid  > 0)
		{
			PartyMatchRoom _room = PartyMatchRoomList.getInstance().getRoom(_roomid);
			if(_room != null)
			{
				_log.info("PartyMatchRoom #" + _room.getId() + " changed by "+_activeChar.getName());
				_room.setMaxMembers(_membersmax);
				_room.setMinLvl(_lvlmin);
				_room.setMaxLvl(_lvlmax);
				_room.setLootType(_loot);
				_room.setTitle(_roomtitle);

				for(L2PcInstance _member : _room.getPartyMembers())
				{
					if(_member == null)
						continue;

					_member.sendPacket(new PartyMatchDetail(_activeChar, _room));
					_member.sendPacket(new SystemMessage(SystemMessageId.PARTY_ROOM_REVISED));
				}
			}
		}
		else
		{
			int _maxid = PartyMatchRoomList.getInstance().getMaxId();

			PartyMatchRoom _room = new PartyMatchRoom(_maxid, _roomtitle, _loot, _lvlmin, _lvlmax, _membersmax, _activeChar);

			_log.info("PartyMatchRoom #" + _maxid + " created by " + _activeChar.getName());
			// Remove from waiting list
			PartyMatchWaitingList.getInstance().removePlayer(_activeChar);

			PartyMatchRoomList.getInstance().addPartyMatchRoom(_maxid, _room);

			if(_activeChar.isInParty())
			{
				for(L2PcInstance ptmember : _activeChar.getParty().getPartyMembers())
				{
					if(ptmember == null)
						continue;

					if(ptmember == _activeChar)
						continue;

					ptmember.setPartyRoom(_maxid);
					//ptmember.setPartyMatching(1);

					_room.addMember(ptmember);
				}
			}

			_activeChar.sendPacket(new PartyMatchDetail(_activeChar, _room));
			_activeChar.sendPacket(new ExPartyRoomMember(_activeChar, _room, 1));

			_activeChar.sendPacket(new SystemMessage(SystemMessageId.PARTY_ROOM_CREATED));

			_activeChar.setPartyRoom(_maxid);
			//_activeChar.setPartyMatching(1);
			_activeChar.broadcastUserInfo();
		}
	}

	@Override
	public String getType()
	{
		return _C__70_REQUESTPARTYMATCHLIST;
	}

}