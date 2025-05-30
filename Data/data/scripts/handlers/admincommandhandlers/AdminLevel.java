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

import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.base.Experience;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class AdminLevel implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_add_level",
		"admin_set_level"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		L2Object targetChar = activeChar.getTarget();
		String target = (targetChar != null ? targetChar.getName() : "no-target");
        GMAudit.auditGMAction(activeChar.getName(), command, target, "");

		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken();

		String val = "";

		if(st.countTokens() >= 1)
		{
			val = st.nextToken();
		}

		if(actualCommand.equalsIgnoreCase("admin_add_level"))
		{
			try
			{
				if(targetChar instanceof L2Playable)
				{
					((L2Playable) targetChar).getStat().addLevel(Byte.parseByte(val));
				}
			}
			catch(NumberFormatException e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Wrong Number Format");
			}
		}
		else if(actualCommand.equalsIgnoreCase("admin_set_level"))
		{
			try
			{
				if(targetChar == null || !(targetChar instanceof L2Playable))
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT)); // incorrect
					return false;
				}

				final L2Playable targetPlayer = (L2Playable) targetChar;

				final byte lvl = Byte.parseByte(val);
				int max_level = Experience.MAX_LEVEL;

				if(targetChar instanceof L2PcInstance && ((L2PcInstance) targetPlayer).isSubClassActive())
				{
					max_level = Experience.MAX_SUBCLASS_LEVEL;
				}

				if(lvl >= 1 && lvl <= max_level)
				{
					final long pXp = targetPlayer.getStat().getExp();
					final long tXp = Experience.getExp(lvl);

					if(pXp > tXp)
					{
						targetPlayer.getStat().removeExpAndSp(pXp - tXp, 0);
					}
					else if(pXp < tXp)
					{
						targetPlayer.getStat().addExpAndSp(tXp - pXp, 0);
					}
				}
				else
				{
					activeChar.sendChatMessage(0, 0, "SYS", "You must specify level between 1 and " + Experience.MAX_LEVEL + ".");
					return false;
				}
			}
			catch(final NumberFormatException e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "You must specify level between 1 and " + Experience.MAX_LEVEL + ".");
				return false;
			}
		}
		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}