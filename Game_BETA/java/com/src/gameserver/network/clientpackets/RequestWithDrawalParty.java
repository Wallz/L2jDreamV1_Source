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

import com.src.gameserver.model.L2Party;
import com.src.gameserver.model.PartyMatchRoom;
import com.src.gameserver.model.PartyMatchRoomList;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.ExClosePartyRoom;
import com.src.gameserver.network.serverpackets.ExPartyRoomMember;
import com.src.gameserver.network.serverpackets.PartyMatchDetail;

public final class RequestWithDrawalParty extends L2GameClientPacket
{
	private static final String _C__2B_REQUESTWITHDRAWALPARTY = "[C] 2B RequestWithDrawalParty";

	@Override
	protected void readImpl()
	{}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if(player == null)
			return;

		L2Party party = player.getParty();

		if(player.isInParty())
		{
			if(party.isInDimensionalRift() && !party.getDimensionalRift().getRevivedAtWaitingRoom().contains(player))
			{
				player.sendMessage("You can't exit party when you are in Dimensional Rift.");
			}
			else
			{
				party.removePartyMember(player);

				if(player.isInPartyMatchRoom())
				{
					PartyMatchRoom _room = PartyMatchRoomList.getInstance().getPlayerRoom(player);
					if(_room != null)
					{
						player.sendPacket(new PartyMatchDetail(player, _room));
						player.sendPacket(new ExPartyRoomMember(player, _room, 0));
						player.sendPacket(new ExClosePartyRoom());

						_room.deleteMember(player);
					}
					player.setPartyRoom(0);
					player.broadcastUserInfo();
				}
			}
		}
	}

	@Override
	public String getType()
	{
		return _C__2B_REQUESTWITHDRAWALPARTY;
	}
}