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
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class AdminInvul implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_invul",
		"admin_setinvul"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		if(command.equals("admin_invul"))
		{
			handleInvul(activeChar);
		}

		if(command.equals("admin_setinvul"))
		{
			L2Object target = activeChar.getTarget();

			if(target instanceof L2PcInstance)
			{
				handleInvul((L2PcInstance) target);
			}
		}
		return true;
	}

	private void handleInvul(L2PcInstance activeChar)
	{
		String text;

		if(activeChar.isInvul())
		{
			activeChar.setIsInvul(false);
			text = activeChar.getName() + " is now mortal";
		}
		else
		{
			activeChar.setIsInvul(true);
			text = activeChar.getName() + " is now invulnerable";
		}
		activeChar.sendMessage(text);
	}
	
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}