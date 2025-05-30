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

import java.util.logging.Logger;

import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class AdminUnblockIp implements IAdminCommandHandler
{
	private static final Logger _log = Logger.getLogger(AdminTeleport.class.getName());

	private static final String[] ADMIN_COMMANDS =
	{
		"admin_unblockip"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		String target = (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target");
        GMAudit.auditGMAction(activeChar.getName(), command, target, "");

		if(command.startsWith("admin_unblockip "))
		{
			try
			{
				String ipAddress = command.substring(16);

				if(unblockIp(ipAddress, activeChar))
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("Removed IP " + ipAddress + " from blocklist!"));
				}
			}
			catch(StringIndexOutOfBoundsException e)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("Usage mode: //unblockip <ip>"));
			}
		}
		return true;
	}

	private boolean unblockIp(String ipAddress, L2PcInstance activeChar)
	{
		_log.warning("IP removed by GM " + activeChar.getName());
		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}