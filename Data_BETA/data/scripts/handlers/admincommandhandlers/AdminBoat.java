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
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.src.Config;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.actor.instance.L2BoatInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class AdminBoat implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_boat"
	};

	private static final Logger _logAudit = Logger.getLogger("gmaudit");

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		if(Config.GMAUDIT)
		{
			LogRecord record = new LogRecord(Level.INFO, command);
			record.setLoggerName("gmaudit");
			record.setParameters(new Object[]
			{
					"GM: " + activeChar.getName(), " to target [" + activeChar.getTarget() + "] "
			});
			_logAudit.log(record);
		}

		L2BoatInstance boat = activeChar.getBoat();

		if(boat == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Usage only possible while riding a boat.");
			return false;
		}

		StringTokenizer st = new StringTokenizer(command);
		st.nextToken();

		if(st.hasMoreTokens())
		{
			String cmd = st.nextToken();
			if(cmd.equals("cycle"))
			{
				if(boat.isInCycle())
				{
					boat.stopCycle();
					activeChar.sendChatMessage(0, 0, "SYS", "Boat cycle stopped.");
				}
				else
				{
					boat.startCycle();
					activeChar.sendChatMessage(0, 0, "SYS", "Boat cycle started.");
				}
			}
			else if(cmd.equals("reload"))
			{
				boat.reloadPath();
				activeChar.sendChatMessage(0, 0, "SYS", "Boat path reloaded.");
			}
			else
			{
				showUsage(activeChar);
			}
		}
		else
		{
			activeChar.sendMessage("====== Boat Information ======");
			activeChar.sendMessage("Name: " + boat.getBoatName() + " (" + boat.getId() + ") ObjId: " + boat.getObjectId());
			activeChar.sendMessage("Cycle: " + boat.isInCycle() + " (" + boat.getCycle() + ")");
			activeChar.sendMessage("Players inside: " + boat.getSizeInside());
			activeChar.sendMessage("Position: " + boat.getX() + " " + boat.getY() + " " + boat.getZ() + " " + boat.getPosition().getHeading());
			activeChar.sendMessage("==============================");
		}

		st = null;
		boat = null;

		return true;
	}

	private void showUsage(L2PcInstance cha)
	{
		cha.sendChatMessage(0, 0, "SYS", "Usage: //boat [cycle|reload]");
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
