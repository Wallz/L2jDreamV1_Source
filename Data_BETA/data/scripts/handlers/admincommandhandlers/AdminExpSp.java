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
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class AdminExpSp implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_add_exp_sp_to_character",
		"admin_add_exp_sp",
		"admin_remove_exp_sp"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		GMAudit.auditGMAction(activeChar.getName(), command, (activeChar.getTarget()!=null ? activeChar.getTarget().getName() : "no-target"), "");

		if(command.startsWith("admin_add_exp_sp"))
		{
			try
			{
				String val = command.substring(16);

				if(!adminAddExpSp(activeChar, val))
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Usage: //add_exp_sp exp sp");
				}
			}
			catch(StringIndexOutOfBoundsException e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //add_exp_sp exp sp");
			}
		}
		else if(command.startsWith("admin_remove_exp_sp"))
		{
			try
			{
				String val = command.substring(19);

				if(!adminRemoveExpSP(activeChar, val))
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Usage: //remove_exp_sp exp sp");
				}
			}
			catch(StringIndexOutOfBoundsException e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //remove_exp_sp exp sp");
			}
		}
		addExpSp(activeChar);
		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void addExpSp(L2PcInstance activeChar)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;

		if(target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			return;
		}

		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/game/expsp.htm");
		adminReply.replace("%name%", player.getName());
		adminReply.replace("%level%", String.valueOf(player.getLevel()));
		adminReply.replace("%xp%", String.valueOf(player.getExp()));
		adminReply.replace("%sp%", String.valueOf(player.getSp()));
		adminReply.replace("%class%", player.getTemplate().className);
		activeChar.sendPacket(adminReply);
	}

	private boolean adminAddExpSp(L2PcInstance activeChar, String ExpSp)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;

		if(target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			return false;
		}

		StringTokenizer st = new StringTokenizer(ExpSp);

		if(st.countTokens() != 2)
		{
			return false;
		}
		else
		{
			String exp = st.nextToken();
			String sp = st.nextToken();

			long expval = 0;
			int spval = 0;

			try
			{
				expval = Long.parseLong(exp);
				spval = Integer.parseInt(sp);
			}
			catch(Exception e)
			{
				return false;
			}

			if(expval != 0 || spval != 0)
			{
				player.sendChatMessage(0, 0, "SYS", "Admin is adding you " + expval + " xp and " + spval + " sp.");
				player.addExpAndSp(expval, spval);
				activeChar.sendChatMessage(0, 0, "SYS", "Added " + expval + " xp and " + spval + " sp to " + player.getName() + ".");
			}
		}
		return true;
	}

	private boolean adminRemoveExpSP(L2PcInstance activeChar, String ExpSp)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;

		if(target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			return false;
		}
		
		StringTokenizer st = new StringTokenizer(ExpSp);

		if(st.countTokens() != 2)
		{
			return false;
		}
		else
		{
			String exp = st.nextToken();
			String sp = st.nextToken();

			long expval = 0;
			int spval = 0;

			try
			{
				expval = Long.parseLong(exp);
				spval = Integer.parseInt(sp);
			}
			catch(Exception e)
			{
				return false;
			}

			if(expval != 0 || spval != 0)
			{
				player.sendChatMessage(0, 0, "SYS", "Admin is removing you " + expval + " xp and " + spval + " sp.");
				player.removeExpAndSp(expval, spval);
				activeChar.sendChatMessage(0, 0, "SYS", "Removed " + expval + " xp and " + spval + " sp from " + player.getName() + ".");
			}
		}
		return true;
	}
}