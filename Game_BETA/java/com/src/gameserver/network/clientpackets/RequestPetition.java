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
import com.src.gameserver.datatables.GmListTable;
import com.src.gameserver.managers.PetitionManager;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.PlaySound;
import com.src.gameserver.network.serverpackets.SystemMessage;

public final class RequestPetition extends L2GameClientPacket
{
	private static final String _C__7F_RequestPetition = "[C] 7F RequestPetition";

	private String _content;
	private int _type;

	@Override
	protected void readImpl()
	{
		_content = readS();
		_type = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if(activeChar == null)
		{
			return;
		}

		if(!GmListTable.getInstance().isGmOnline(false))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.NO_GM_PROVIDING_SERVICE_NOW));
			activeChar.sendPacket(new PlaySound("systemmsg_e.702"));
			return;
		}

		if(!PetitionManager.getInstance().isPetitioningAllowed())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.GAME_CLIENT_UNABLE_TO_CONNECT_TO_PETITION_SERVER));
			return;
		}

		if(PetitionManager.getInstance().isPlayerPetitionPending(activeChar))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.ONLY_ONE_ACTIVE_PETITION_AT_TIME));
			return;
		}

		if(PetitionManager.getInstance().getPendingPetitionCount() == Config.MAX_PETITIONS_PENDING)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.PETITION_SYSTEM_CURRENT_UNAVAILABLE));
			return;
		}

		int totalPetitions = PetitionManager.getInstance().getPlayerTotalPetitionCount(activeChar) + 1;

		if(totalPetitions > Config.MAX_PETITIONS_PER_PLAYER)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.WE_HAVE_RECEIVED_S1_PETITIONS_TODAY).addNumber(totalPetitions));
			return;
		}

		if(_content.length() > 255)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.PETITION_MAX_CHARS_255));
			return;
		}

		int petitionId = PetitionManager.getInstance().submitPetition(activeChar, _content, _type);

		activeChar.sendPacket(new SystemMessage(SystemMessageId.PETITION_ACCEPTED_RECENT_NO_S1).addNumber(petitionId));
		activeChar.sendPacket(new SystemMessage(SystemMessageId.SUBMITTED_YOU_S1_TH_PETITION_S2_LEFT).addNumber(totalPetitions).addNumber(Config.MAX_PETITIONS_PER_PLAYER - totalPetitions));
		activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_PETITION_ON_WAITING_LIST).addNumber(PetitionManager.getInstance().getPendingPetitionCount()));
	}

	@Override
	public String getType()
	{
		return _C__7F_RequestPetition;
	}

}