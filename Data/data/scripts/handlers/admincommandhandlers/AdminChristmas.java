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
import com.src.gameserver.managers.ChristmasManager;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class AdminChristmas implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_christmas_start",
		"admin_christmas_end"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		GMAudit.auditGMAction(activeChar.getName(), command, (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target"), "");

		if(command.equals("admin_christmas_start"))
		{
			startChristmas(activeChar);
		}
		else if(command.equals("admin_christmas_end"))
		{
			endChristmas(activeChar);
		}
		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void startChristmas(L2PcInstance activeChar)
	{
		ChristmasManager.getInstance().init(activeChar);
	}

	private void endChristmas(L2PcInstance activeChar)
	{
		ChristmasManager.getInstance().end(activeChar);
	}
}