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

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import com.src.Config;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.MagicSkillUser;
import com.src.gameserver.thread.ThreadPoolManager;

public class AdminTest implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_test",
		"admin_stats",
		"admin_skill_test",
		"admin_st",
		"admin_mp",
		"admin_known"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		String target = (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target");
        GMAudit.auditGMAction(activeChar.getName(), command, target, "");

		if(command.equals("admin_stats"))
		{
			for(String line : ThreadPoolManager.getInstance().getStats())
			{
				activeChar.sendMessage(line);
			}
		}
		else if(command.startsWith("admin_skill_test") || command.startsWith("admin_st"))
		{
			try
			{
				StringTokenizer st = new StringTokenizer(command);
				st.nextToken();

				int id = Integer.parseInt(st.nextToken());

				adminTestSkill(activeChar, id);
			}
			catch(NumberFormatException e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Command format is //skill_test <ID>");
			}
			catch(NoSuchElementException nsee)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Command format is //skill_test <ID>");
			}
		}
		else if(command.equals("admin_mp on"))
		{
			activeChar.sendChatMessage(0, 0, "SYS", "command not working");
		}
		else if(command.equals("admin_mp off"))
		{
			activeChar.sendChatMessage(0, 0, "SYS", "command not working");
		}
		else if(command.equals("admin_mp dump"))
		{
			activeChar.sendChatMessage(0, 0, "SYS", "command not working");
		}
		else if(command.equals("admin_known on"))
		{
			Config.CHECK_KNOWN = true;
		}
		else if(command.equals("admin_known off"))
		{
			Config.CHECK_KNOWN = false;
		}
		return true;
	}

	private void adminTestSkill(L2PcInstance activeChar, int id)
	{
		L2Character player;
		L2Object target = activeChar.getTarget();

		if(target == null || !(target instanceof L2Character))
		{
			player = activeChar;
		}
		else
		{
			player = (L2Character) target;
		}
		player.broadcastPacket(new MagicSkillUser(activeChar, player, id, 1, 1, 1));
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}