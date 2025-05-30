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

import com.src.gameserver.managers.DuelManager;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;


public final class RequestDuelAnswerStart extends L2GameClientPacket
{
	private static final String _C__D0_28_REQUESTDUELANSWERSTART = "[C] D0:28 RequestDuelAnswerStart";

	private int _partyDuel;
	@SuppressWarnings("unused")
	private int _unk1;
	private int _response;

	@Override
	protected void readImpl()
	{
		_partyDuel = readD();
		_unk1 = readD();
		_response = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();

		if(player == null)
		{
			return;
		}

		L2PcInstance requestor = player.getActiveRequester();

		if(requestor == null)
		{
			return;
		}

		if(_response == 1)
		{
			if(requestor.isInDuel())
			{
				player.sendPacket(new SystemMessage(SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_ALREADY_ENGAGED_IN_A_DUEL).addString(requestor.getName()));
				return;
			}
			else if(player.isInDuel())
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_UNABLE_TO_REQUEST_A_DUEL_AT_THIS_TIME));
				return;
			}

			if(_partyDuel == 1)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_ACCEPTED_S1S_CHALLENGE_TO_A_PARTY_DUEL_THE_DUEL_WILL_BEGIN_IN_A_FEW_MOMENTS).addString(requestor.getName()));
				requestor.sendPacket(new SystemMessage(SystemMessageId.S1_HAS_ACCEPTED_YOUR_CHALLENGE_TO_DUEL_AGAINST_THEIR_PARTY_THE_DUEL_WILL_BEGIN_IN_A_FEW_MOMENTS).addString(player.getName()));
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_ACCEPTED_S1S_CHALLENGE_TO_A_DUEL_THE_DUEL_WILL_BEGIN_IN_A_FEW_MOMENTS).addString(requestor.getName()));
				requestor.sendPacket(new SystemMessage(SystemMessageId.S1_HAS_ACCEPTED_YOUR_CHALLENGE_TO_A_DUEL_THE_DUEL_WILL_BEGIN_IN_A_FEW_MOMENTS).addString(player.getName()));
			}

			DuelManager.getInstance().addDuel(requestor, player, _partyDuel);
		}
		else
		{
			if(_partyDuel == 1)
			{
				requestor.sendPacket(new SystemMessage(SystemMessageId.THE_OPPOSING_PARTY_HAS_DECLINED_YOUR_CHALLENGE_TO_A_DUEL));
			}
			else
			{
				requestor.sendPacket(new SystemMessage(SystemMessageId.S1_HAS_DECLINED_YOUR_CHALLENGE_TO_A_DUEL).addString(player.getName()));
			}
		}

		player.setActiveRequester(null);
		requestor.onTransactionResponse();
	}

	@Override
	public String getType()
	{
		return _C__D0_28_REQUESTDUELANSWERSTART;
	}

}