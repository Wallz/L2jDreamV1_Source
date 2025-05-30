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

import com.src.Config;
import com.src.gameserver.model.PartyMatchRoom;
import com.src.gameserver.model.PartyMatchRoomList;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ExManagePartyRoomMember;
import com.src.gameserver.network.serverpackets.JoinParty;
import com.src.gameserver.network.serverpackets.SystemMessage;

public final class RequestAnswerJoinParty extends L2GameClientPacket
{
	private static final String _C__2A_REQUESTANSWERPARTY = "[C] 2A RequestAnswerJoinParty";

	private int _response;

	@Override
	protected void readImpl()
	{
		_response = readD();
	}

	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if(player == null)
		{
			return;
		}

		final L2PcInstance requestor = player.getActiveRequester();
		if(requestor == null)
		{
			return;
		}
		if(player.isCursedWeaponEquiped() || requestor.isCursedWeaponEquiped())
        {
                requestor.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
                return;
        }

		if((requestor.isFightingInEvent() || player.isFightingInEvent()) && !requestor.isInSameEvent(player) && !requestor.isGM())
		{
			if(((requestor.getEventName().equals("TVT") || player.getEventName().equals("TVT")) && !Config.TVT_ALLOW_INTERFERENCE)
				|| ((requestor.getEventName().equals("CTF") || player.getEventName().equals("CTF")) && !Config.CTF_ALLOW_INTERFERENCE)
				|| ((requestor.getEventName().equals("BW") || player.getEventName().equals("BW")) && !Config.BW_ALLOW_INTERFERENCE)
				|| ((requestor.getEventName().equals("DM") || player.getEventName().equals("DM")) && !Config.DM_ALLOW_INTERFERENCE))
				return;
		}
		requestor.sendPacket(new JoinParty(_response));

		if(_response == 1)
		{
			if(requestor.isInParty())
			{
				if(requestor.getParty().getMemberCount() >= 9)
				{
					player.sendPacket(new SystemMessage(SystemMessageId.PARTY_FULL));
					requestor.sendPacket(new SystemMessage(SystemMessageId.PARTY_FULL));
					return;
				}
			}

			if(requestor.isGM())
			{
				if(requestor.getAppearance().getInvisible())
				{
					requestor.sendMessage("You can't invite invisible GameMaster!");
					return;
				}
			}

			player.joinParty(requestor.getParty());
			if(requestor.isInPartyMatchRoom() && player.isInPartyMatchRoom())
			{
				PartyMatchRoomList list = PartyMatchRoomList.getInstance();
				if(list != null && (list.getPlayerRoomId(requestor) == list.getPlayerRoomId(player)))
				{
					PartyMatchRoom room = list.getPlayerRoom(requestor);
					if(room != null)
					{
						ExManagePartyRoomMember packet = new ExManagePartyRoomMember(player, room, 1);
						for(L2PcInstance member : room.getPartyMembers())
						{
							if(member != null)
								member.sendPacket(packet);
						}
					}
				}
			}
			else if(requestor.isInPartyMatchRoom() && !player.isInPartyMatchRoom())
			{
				PartyMatchRoomList list = PartyMatchRoomList.getInstance();
				if(list != null)
				{
					PartyMatchRoom room = list.getPlayerRoom(requestor);
					if(room != null)
					{
						room.addMember(player);
						ExManagePartyRoomMember packet = new ExManagePartyRoomMember(player, room, 1);
						for(L2PcInstance member : room.getPartyMembers())
						{
							if(member != null)
								member.sendPacket(packet);
						}
						player.setPartyRoom(room.getId());
						player.broadcastUserInfo();
					}
				}
			}
		}
		else
		{
			requestor.sendPacket(new SystemMessage(SystemMessageId.PLAYER_DECLINED));

			if(requestor.getParty() != null && requestor.getParty().getMemberCount() == 1)
			{
				requestor.setParty(null);
			}
		}

		if(requestor.isInParty())
		{
			requestor.getParty().decreasePendingInvitationNumber();
		}

		player.setActiveRequester(null);
		requestor.onTransactionResponse();
	}

	@Override
	public String getType()
	{
		return _C__2A_REQUESTANSWERPARTY;
	}

}