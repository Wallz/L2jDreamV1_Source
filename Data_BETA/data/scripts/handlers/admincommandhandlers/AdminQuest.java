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
import com.src.gameserver.managers.QuestManager;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class AdminQuest implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_quest_reload"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if(activeChar == null)
		{
			return false;
		}

		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		String target = (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target");
		GMAudit.auditGMAction(activeChar.getName(), command, target, "");

		if(command.startsWith("admin_quest_reload"))
		{
			String[] parts = command.split(" ");

			if(parts.length < 2)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Syntax: //quest_reload <questFolder>.<questSubFolders...>.questName> or //quest_reload <id>");
			}
			else
			{
				try
				{
					int questId = Integer.parseInt(parts[1]);

					if(QuestManager.getInstance().reload(questId))
					{
						activeChar.sendChatMessage(0, 0, "SYS", "Quest Reloaded Successfully.");
					}
					else
					{
						activeChar.sendChatMessage(0, 0, "SYS", "Quest Reloaded Failed");
					}
				}
				catch(NumberFormatException e)
				{
					if(QuestManager.getInstance().reload(parts[1]))
					{
						activeChar.sendChatMessage(0, 0, "SYS", "Quest Reloaded Successfully.");
					}
					else
					{
						activeChar.sendChatMessage(0, 0, "SYS", "Quest Reloaded Failed");
					}
				}
			}
		}
		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}