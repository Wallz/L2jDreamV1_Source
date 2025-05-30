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

import java.util.StringTokenizer;

import com.src.gameserver.datatables.GmListTable;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.CreatureSay;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class AdminGmChat implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
			"admin_gmchat", "admin_snoop", "admin_gmchat_menu"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		String target = (activeChar.getTarget() != null) ? activeChar.getTarget().getName() : "no-target";
        GMAudit.auditGMAction(activeChar.getName(), command, target, "");

		if(command.startsWith("admin_gmchat"))
		{
			handleGmChat(command, activeChar);
		}
		else if(command.startsWith("admin_snoop"))
		{
			snoop(command, activeChar);
		}

		if(command.startsWith("admin_gmchat_menu"))
		{
			AdminHelpPage.showHelpPage(activeChar, "main_menu.htm");
		}
		return true;
	}

	private void snoop(String command, L2PcInstance activeChar)
	{
		StringTokenizer st = new StringTokenizer(command);
		st.nextToken();

		if(!st.hasMoreTokens())
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Usage: //snoop <player_name>");
			return;
		}

		L2PcInstance target = L2World.getInstance().getPlayer(st.nextToken());

		if(command.length() > 12)
		{
			target = L2World.getInstance().getPlayer(command.substring(12));
		}

		if(target == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_CANT_FOUND));
			return;
		}

		if(target.getAccessLevel().getLevel() < activeChar.getAccessLevel().getLevel() && target.getAccessLevel().getLevel() != 0)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			target.sendChatMessage(0, 0, "SYS", activeChar.getName() + " tried to snoop your conversations. Blocked.");
			return;
		}

		target.addSnooper(activeChar);
		activeChar.addSnooped(target);
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void handleGmChat(String command, L2PcInstance activeChar)
	{
		try
		{
			int offset = 0;

			String text;

			if(command.contains("menu"))
			{
				offset = 17;
			}
			else
			{
				offset = 13;
			}

			text = command.substring(offset);
			CreatureSay cs = new CreatureSay(0, 9, activeChar.getName(), text);
			GmListTable.broadcastToGMs(cs);
		}
		catch(StringIndexOutOfBoundsException e)
		{
			
		}
	}
}