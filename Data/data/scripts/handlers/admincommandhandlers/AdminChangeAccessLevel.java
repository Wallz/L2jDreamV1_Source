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
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class AdminChangeAccessLevel implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_changelvl"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		handleChangeLevel(command, activeChar);

		String target = (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target");
		GMAudit.auditGMAction(activeChar.getName(), command, target, "");

		return true;
	}

	private void handleChangeLevel(String command, L2PcInstance activeChar)
	{
		if(activeChar == null)
		{
			return;
		}

		String[] parts = command.split(" ");

		if(parts.length == 2)
		{
			int lvl = Integer.parseInt(parts[1]);

			if(activeChar.getTarget() instanceof L2PcInstance)
			{
				((L2PcInstance) activeChar.getTarget()).setAccessLevel(lvl);
				activeChar.sendChatMessage(0, 0, "SYS", "You have changed the access level of player " + activeChar.getTarget().getName() + " to " + lvl + ".");
			}
		}
		else if(parts.length == 3)
		{
			int lvl = Integer.parseInt(parts[2]);

			L2PcInstance player = L2World.getInstance().getPlayer(parts[1]);

			if(player != null)
			{
				player.setAccessLevel(lvl);
				activeChar.sendChatMessage(0, 0, "SYS", "You have changed the access level of player " + activeChar.getTarget().getName() + " to " + lvl + ".");
			}
		}
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}