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
import com.src.gameserver.network.serverpackets.JoinPledge;
import com.src.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import com.src.gameserver.network.serverpackets.PledgeShowMemberListAdd;
import com.src.gameserver.network.serverpackets.PledgeShowMemberListAll;
import com.src.gameserver.network.serverpackets.SystemMessage;

public final class RequestAnswerJoinPledge extends L2GameClientPacket
{
	private static final String _C__25_REQUESTANSWERJOINPLEDGE = "[C] 25 RequestAnswerJoinPledge";

	private int _answer;

	@Override
	protected void readImpl()
	{
		_answer = readD();
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

		if(_answer == 0)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_DID_NOT_RESPOND_TO_S1_CLAN_INVITATION).addString(requestor.getName()));
			requestor.sendPacket(new SystemMessage(SystemMessageId.S1_DID_NOT_RESPOND_TO_CLAN_INVITATION).addString(activeChar.getName()));
		}
		else
		{
			if(!(requestor.getRequest().getRequestPacket() instanceof RequestJoinPledge))
			{
				return;
			}

			RequestJoinPledge requestPacket = (RequestJoinPledge) requestor.getRequest().getRequestPacket();
			L2Clan clan = requestor.getClan();

			if(clan.checkClanJoinCondition(requestor, activeChar, requestPacket.getPledgeType()))
			{
				JoinPledge jp = new JoinPledge(requestor.getClanId());
				activeChar.sendPacket(jp);

				activeChar.setPledgeType(requestPacket.getPledgeType());

				if(requestPacket.getPledgeType() == L2Clan.SUBUNIT_ACADEMY)
				{
					activeChar.setPowerGrade(9);
					activeChar.setLvlJoinedAcademy(activeChar.getLevel());
				}
				else
				{
					activeChar.setPowerGrade(5);
				}

				clan.addClanMember(activeChar);
				activeChar.setClanPrivileges(activeChar.getClan().getRankPrivs(activeChar.getPowerGrade()));

				activeChar.sendPacket(new SystemMessage(SystemMessageId.ENTERED_THE_CLAN));

				clan.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.S1_HAS_JOINED_CLAN).addString(activeChar.getName()));
				clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListAdd(activeChar), activeChar);
				clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));

				activeChar.sendPacket(new PledgeShowMemberListAll(clan, activeChar));
				activeChar.setClanJoinExpiryTime(0);
				activeChar.broadcastUserInfo();
				activeChar.regiveTemporarySkills();
			}
		}

		activeChar.getRequest().onRequestResponse();
	}

	@Override
	public String getType()
	{
		return _C__25_REQUESTANSWERJOINPLEDGE;
	}

}