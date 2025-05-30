/* This program is free software; you can redistribute it and/or modify
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
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ExDuelAskStart;
import com.src.gameserver.network.serverpackets.SystemMessage;

public final class RequestDuelStart extends L2GameClientPacket
{
	private static final String _C__D0_27_REQUESTDUELSTART = "[C] D0:27 RequestDuelStart";

	private String _player;
	private int _partyDuel;

	@Override
	protected void readImpl()
	{
		_player = readS();
		_partyDuel = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		L2PcInstance targetChar = L2World.getInstance().getPlayer(_player);

		if(activeChar == null)
		{
			return;
		}

		if(targetChar == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL));
			return;
		}

		if(activeChar == targetChar)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL));
			return;
		}

		if(!activeChar.canDuel())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_UNABLE_TO_REQUEST_A_DUEL_AT_THIS_TIME));
			return;
		}
		else if(!targetChar.canDuel())
		{
			activeChar.sendPacket(targetChar.getNoDuelReason());
			return;
		}
		else if(!activeChar.isInsideRadius(targetChar, 250, false, false))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_CANNOT_RECEIVE_A_DUEL_CHALLENGE_BECAUSE_S1_IS_TOO_FAR_AWAY).addString(targetChar.getName()));
			return;
		}

		if(_partyDuel == 1)
		{
			if(!activeChar.isInParty() || !(activeChar.isInParty() && activeChar.getParty().isLeader(activeChar)))
			{
				activeChar.sendMessage("You have to be the leader of a party in order to request a party duel.");
				return;
			}
			else if(!targetChar.isInParty())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.SINCE_THE_PERSON_YOU_CHALLENGED_IS_NOT_CURRENTLY_IN_A_PARTY_THEY_CANNOT_DUEL_AGAINST_YOUR_PARTY));
				return;
			}
			else if(activeChar.getParty().getPartyMembers().contains(targetChar))
			{
				activeChar.sendMessage("This player is a member of your own party.");
				return;
			}

			for(L2PcInstance temp : activeChar.getParty().getPartyMembers())
			{
				if(!temp.canDuel())
				{
					activeChar.sendMessage("Not all the members of your party are ready for a duel.");
					return;
				}
			}
			L2PcInstance partyLeader = null;

			for(L2PcInstance temp : targetChar.getParty().getPartyMembers())
			{
				if(partyLeader == null)
				{
					partyLeader = temp;
				}
				if(!temp.canDuel())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.THE_OPPOSING_PARTY_IS_CURRENTLY_UNABLE_TO_ACCEPT_A_CHALLENGE_TO_A_DUEL));
					return;
				}
			}

			if(!partyLeader.isProcessingRequest())
			{
				activeChar.onTransactionRequest(partyLeader);
				partyLeader.sendPacket(new ExDuelAskStart(activeChar.getName(), _partyDuel));
				activeChar.sendPacket(new SystemMessage(SystemMessageId.S1S_PARTY_HAS_BEEN_CHALLENGED_TO_A_DUEL).addString(partyLeader.getName()));
				targetChar.sendPacket(new SystemMessage(SystemMessageId.S1S_PARTY_HAS_CHALLENGED_YOUR_PARTY_TO_A_DUEL).addString(activeChar.getName()));
			}
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER).addString(partyLeader.getName()));
			}
		}
		else
		{
			if(!targetChar.isProcessingRequest())
			{
				activeChar.onTransactionRequest(targetChar);
				targetChar.sendPacket(new ExDuelAskStart(activeChar.getName(), _partyDuel));
				activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_HAS_BEEN_CHALLENGED_TO_A_DUEL).addString(targetChar.getName()));
				targetChar.sendPacket(new SystemMessage(SystemMessageId.S1_HAS_CHALLENGED_YOU_TO_A_DUEL).addString(activeChar.getName()));
			}
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER).addString(targetChar.getName()));
			}
		}
	}

	@Override
	public String getType()
	{
		return _C__D0_27_REQUESTDUELSTART;
	}

}