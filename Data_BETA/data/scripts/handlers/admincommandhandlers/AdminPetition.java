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
package handlers.admincommandhandlers;

import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.managers.PetitionManager;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class AdminPetition implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_view_petitions",
		"admin_view_petition",
		"admin_accept_petition",
		"admin_reject_petition",
		"admin_reset_petitions",
		"admin_force_peti",
		"admin_add_peti_chat"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		String target = (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target");
		GMAudit.auditGMAction(activeChar.getName(), command, target, "");

		int petitionId = -1;

		try
		{
			petitionId = Integer.parseInt(command.split(" ")[1]);
		}
		catch(Exception e)
		{
		}

		if(command.equals("admin_view_petitions"))
		{
			PetitionManager.getInstance().sendPendingPetitionList(activeChar);
		}
		else if(command.startsWith("admin_view_petition"))
		{
			PetitionManager.getInstance().viewPetition(activeChar, petitionId);
		}
		else if(command.startsWith("admin_accept_petition"))
		{
			if(PetitionManager.getInstance().isPlayerInConsultation(activeChar))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.ONLY_ONE_ACTIVE_PETITION_AT_TIME));
				return true;
			}

			if(PetitionManager.getInstance().isPetitionInProcess(petitionId))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.PETITION_UNDER_PROCESS));
				return true;
			}

			if(!PetitionManager.getInstance().acceptPetition(activeChar, petitionId))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.NOT_UNDER_PETITION_CONSULTATION));
			}
		}
		else if(command.startsWith("admin_reject_petition"))
		{
			if(!PetitionManager.getInstance().rejectPetition(activeChar, petitionId))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.FAILED_CANCEL_PETITION_TRY_LATER));
			}
		}
		else if (command.startsWith("admin_force_peti"))
		{
			try
			{
				L2Object targetChar = activeChar.getTarget();
				if (targetChar == null || !(targetChar instanceof L2PcInstance))
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return false;
				}
				L2PcInstance targetPlayer = (L2PcInstance) targetChar;
				String val = command.substring(15);
				petitionId = PetitionManager.getInstance().submitPetition(targetPlayer, val, 9);
				PetitionManager.getInstance().acceptPetition(activeChar, petitionId);
			}
			
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //force_peti text");
				return false;
			}
		}
		else if (command.startsWith("admin_add_peti_chat"))
		{
			L2PcInstance player = L2World.getInstance().getPlayer(command.substring(20));
			if (player == null)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME));
				return false;
			}
			petitionId = PetitionManager.getInstance().submitPetition(player, "", 9);
			PetitionManager.getInstance().acceptPetition(activeChar, petitionId);
		}
		else if(command.equals("admin_reset_petitions"))
		{
			if(PetitionManager.getInstance().isPetitionInProcess())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.PETITION_UNDER_PROCESS));
				return false;
			}

			PetitionManager.getInstance().clearPendingPetitions();
		}
		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}