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

import com.src.Config;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.gameserverpackets.ServerStatus;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.thread.LoginServerThread;

public class AdminLogin implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_server_gm_only",
		"admin_server_all",
		"admin_server_max_player",
		"admin_server_list_clock",
		"admin_server_login"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		GMAudit.auditGMAction(activeChar.getName(), command, (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target"), "");

		if(command.equals("admin_server_gm_only"))
		{
			gmOnly();
			activeChar.sendChatMessage(0, 0, "SYS", "Server is now GM only");
			showMainPage(activeChar);
		}
		else if(command.equals("admin_server_all"))
		{
			allowToAll();
			activeChar.sendChatMessage(0, 0, "SYS", "Server is not GM only anymore");
			showMainPage(activeChar);
		}
		else if(command.startsWith("admin_server_max_player"))
		{
			StringTokenizer st = new StringTokenizer(command);
			if(st.countTokens() > 1)
			{
				st.nextToken();
				String number = st.nextToken();
				try
				{
					LoginServerThread.getInstance().setMaxPlayer(new Integer(number).intValue());
					activeChar.sendChatMessage(0, 0, "SYS", "maxPlayer set to " + new Integer(number).intValue());
					showMainPage(activeChar);
				}
				catch(NumberFormatException e)
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Max players must be a number.");
				}
			}
			else
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Format is server_max_player <max>");
			}
		}
		else if(command.startsWith("admin_server_list_clock"))
		{
			StringTokenizer st = new StringTokenizer(command);

			if(st.countTokens() > 1)
			{
				st.nextToken();
				String mode = st.nextToken();

				if(mode.equals("on"))
				{
					LoginServerThread.getInstance().sendServerStatus(ServerStatus.SERVER_LIST_CLOCK, ServerStatus.ON);
					activeChar.sendChatMessage(0, 0, "SYS", "A clock will now be displayed next to the server name");
					Config.SERVER_LIST_CLOCK = true;
					showMainPage(activeChar);
				}
				else if(mode.equals("off"))
				{
					LoginServerThread.getInstance().sendServerStatus(ServerStatus.SERVER_LIST_CLOCK, ServerStatus.OFF);
					Config.SERVER_LIST_CLOCK = false;
					activeChar.sendChatMessage(0, 0, "SYS", "The clock will not be displayed");
					showMainPage(activeChar);
				}
				else
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Format is server_list_clock <on/off>");
				}
			}
			else
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Format is server_list_clock <on/off>");
			}
		}
		else if(command.equals("admin_server_login"))
		{
			showMainPage(activeChar);
		}
		return true;
	}

	private void showMainPage(L2PcInstance activeChar)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile("data/html/admin/gm/login.htm");
		html.replace("%server_name%", LoginServerThread.getInstance().getServerName());
		html.replace("%status%", LoginServerThread.getInstance().getStatusString());
		html.replace("%clock%", String.valueOf(Config.SERVER_LIST_CLOCK));
		html.replace("%brackets%", String.valueOf(Config.SERVER_LIST_BRACKET));
		html.replace("%max_players%", String.valueOf(LoginServerThread.getInstance().getMaxPlayer()));
		activeChar.sendPacket(html);
	}

	private void allowToAll()
	{
		LoginServerThread.getInstance().setServerStatus(ServerStatus.STATUS_AUTO);
		Config.SERVER_GMONLY = false;
	}

	private void gmOnly()
	{
		LoginServerThread.getInstance().setServerStatus(ServerStatus.STATUS_GM_ONLY);
		Config.SERVER_GMONLY = true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}