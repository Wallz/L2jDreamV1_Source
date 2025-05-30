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
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class AdminTarget implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_target"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		if(command.startsWith("admin_target"))
		{
			handleTarget(command, activeChar);
		}

		return true;
	}

	private void handleTarget(String command, L2PcInstance activeChar)
	{
		try
		{
			String targetName = command.substring(13);
			L2Object obj = L2World.getInstance().getPlayer(targetName);

			if(obj != null && obj instanceof L2PcInstance)
			{
				obj.onAction(activeChar);
			}
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("Player " + targetName + " not found"));
			}
		}
		catch(IndexOutOfBoundsException e)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("Please specify correct name."));
		}
	}
	
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}