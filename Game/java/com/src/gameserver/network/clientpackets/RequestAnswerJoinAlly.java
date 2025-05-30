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

import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;

public final class RequestAnswerJoinAlly extends L2GameClientPacket
{
	private static final String _C__83_REQUESTANSWERJOINALLY = "[C] 83 RequestAnswerJoinAlly";

	private int _response;

	@Override
	protected void readImpl()
	{
		_response = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if(activeChar == null)
		{
			return;
		}

		L2PcInstance requestor = activeChar.getRequest().getPartner();

		if(requestor == null)
		{
			return;
		}

		if(_response == 0)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_DID_NOT_RESPOND_TO_ALLY_INVITATION));
			requestor.sendPacket(new SystemMessage(SystemMessageId.NO_RESPONSE_TO_ALLY_INVITATION));
		}
		else
		{
			if(!(requestor.getRequest().getRequestPacket() instanceof RequestJoinAlly))
			{
				return;
			}

			L2Clan clan = requestor.getClan();

			if(clan.checkAllyJoinCondition(requestor, activeChar))
			{
				requestor.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_SUCCEEDED_INVITING_FRIEND));
				activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_ACCEPTED_ALLIANCE));
				activeChar.getClan().setAllyId(clan.getAllyId());
				activeChar.getClan().setAllyName(clan.getAllyName());
				activeChar.getClan().setAllyPenaltyExpiryTime(0, 0);
				activeChar.getClan().setAllyCrest(clan.getAllyCrestId());
				activeChar.getClan().updateClanInDB();
				// Added to set the Alliance Crest when a clan joins an ally.  
				try  
				{   
					activeChar.getClan().setAllyCrestId(requestor.getClan().getAllyCrestId());  
					for (L2PcInstance member : activeChar.getClan().getOnlineMembers(""))  
						member.broadcastUserInfo();  
				}  
				catch(Throwable t)
				{
					
				} 
			}
		}

		activeChar.getRequest().onRequestResponse();
	}

	@Override
	public String getType()
	{
		return _C__83_REQUESTANSWERJOINALLY;
	}

}