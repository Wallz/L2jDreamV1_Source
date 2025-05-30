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

import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.src.Config;
import com.src.gameserver.GameTimeController;
import com.src.gameserver.Shutdown;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;

public class AdminShutdown implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_server_shutdown",
		"admin_server_restart",
		"admin_server_abort"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		GMAudit.auditGMAction(activeChar.getName(), command, (activeChar.getTarget()!=null ? activeChar.getTarget().getName() : "no-target"), "");

		if(command.startsWith("admin_server_shutdown"))
		{
			try
			{
				int val = Integer.parseInt(command.substring(22));

				serverShutdown(activeChar, val, false);
			}
			catch(StringIndexOutOfBoundsException e)
			{
				sendHtmlForm(activeChar);
			}
		}
		else if(command.startsWith("admin_server_restart"))
		{
			try
			{
				int val = Integer.parseInt(command.substring(21));

				serverShutdown(activeChar, val, true);
			}
			catch(StringIndexOutOfBoundsException e)
			{
				sendHtmlForm(activeChar);
			}
		}
		else if(command.startsWith("admin_server_abort"))
		{
			serverAbort(activeChar);
		}
		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void sendHtmlForm(L2PcInstance activeChar)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

		int t = GameTimeController.getInstance().getGameTime();
		int h = t / 60;
		int m = t % 60;

		SimpleDateFormat format = new SimpleDateFormat("h:mm a");
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, h);
		cal.set(Calendar.MINUTE, m);
		adminReply.setFile("data/html/admin/gm/shutdown.htm");
		adminReply.replace("%count%", String.valueOf(L2World.getInstance().getAllPlayersCount()));
		adminReply.replace("%used%", String.valueOf(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
		adminReply.replace("%xp%", String.valueOf(Config.RATE_XP));
		adminReply.replace("%sp%", String.valueOf(Config.RATE_SP));
		adminReply.replace("%adena%", String.valueOf(Config.RATE_DROP_ADENA));
		adminReply.replace("%drop%", String.valueOf(Config.RATE_DROP_ITEMS));
		adminReply.replace("%time%", String.valueOf(format.format(cal.getTime())));
		activeChar.sendPacket(adminReply);
	}

	private void serverShutdown(L2PcInstance activeChar, int seconds, boolean restart)
	{
		Shutdown.getInstance().startShutdown(activeChar, seconds, restart);
	}

	private void serverAbort(L2PcInstance activeChar)
	{
		Shutdown.getInstance().abort(activeChar);
	}
}