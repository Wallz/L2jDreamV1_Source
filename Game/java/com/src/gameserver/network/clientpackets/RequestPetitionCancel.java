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
import com.src.gameserver.network.serverpackets.CreatureSay;
import com.src.gameserver.network.serverpackets.SystemMessage;

public final class RequestPetitionCancel extends L2GameClientPacket
{
	private static final String _C__80_REQUEST_PETITIONCANCEL = "[C] 80 RequestPetitionCancel";

	@Override
	protected void readImpl()
	{
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if(activeChar == null)
		{
			return;
		}

		if(PetitionManager.getInstance().isPlayerInConsultation(activeChar))
		{
			if(activeChar.isGM())
			{
				PetitionManager.getInstance().endActivePetition(activeChar);
			}
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.PETITION_UNDER_PROCESS));
			}
		}
		else
		{
			if(PetitionManager.getInstance().isPlayerPetitionPending(activeChar))
			{
				if(PetitionManager.getInstance().cancelActivePetition(activeChar))
				{
					int numRemaining = Config.MAX_PETITIONS_PER_PLAYER - PetitionManager.getInstance().getPlayerTotalPetitionCount(activeChar);

					activeChar.sendPacket(new SystemMessage(SystemMessageId.PETITION_CANCELED_SUBMIT_S1_MORE_TODAY).addString(String.valueOf(numRemaining)));

					String msgContent = activeChar.getName() + " has canceled a pending petition.";
					GmListTable.broadcastToGMs(new CreatureSay(activeChar.getObjectId(), 17, "Petition System", msgContent));
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.FAILED_CANCEL_PETITION_TRY_LATER));
				}
			}
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.PETITION_NOT_SUBMITTED));
			}
		}
	}

	@Override
	public String getType()
	{
		return _C__80_REQUEST_PETITIONCANCEL;
	}

}