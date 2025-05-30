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
 * [URL]http://www.gnu.org/copyleft/gpl.html[/URL]
 */
package handlers.admincommandhandlers;

import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2ControllableMobInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.taskmanager.DecayTaskManager;

public class AdminRes implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_res", 
		"admin_res_monster"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		String target = (activeChar.getTarget() != null) ? activeChar.getTarget().getName() : "no-target";
        GMAudit.auditGMAction(activeChar.getName(), command, target, "");

		if(command.startsWith("admin_res "))
		{
			handleRes(activeChar, command.split(" ")[1]);
		}
		else if(command.equals("admin_res"))
		{
			handleRes(activeChar);
		}
		else if(command.startsWith("admin_res_monster "))
		{
			handleNonPlayerRes(activeChar, command.split(" ")[1]);
		}
		else if(command.equals("admin_res_monster"))
		{
			handleNonPlayerRes(activeChar);
		}
		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void handleRes(L2PcInstance activeChar)
	{
		handleRes(activeChar, null);
	}

	private void handleRes(L2PcInstance activeChar, String resParam)
	{
		L2Object obj = activeChar.getTarget();

		if(resParam != null)
		{
			L2PcInstance plyr = L2World.getInstance().getPlayer(resParam);

			if(plyr != null)
			{
				obj = plyr;
			}
			else
			{
				try
				{
					int radius = Integer.parseInt(resParam);

					for(L2PcInstance knownPlayer : activeChar.getKnownList().getKnownPlayersInRadius(radius))
					{
						doResurrect(knownPlayer);
					}

					activeChar.sendChatMessage(0, 0, "SYS", "Resurrected all players within a " + radius + " unit radius.");
					return;
				}
				catch(NumberFormatException e)
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Enter a valid player name or radius.");
					return;
				}
			}
		}

		if(obj == null)
		{
			obj = activeChar;
		}

		if(obj instanceof L2ControllableMobInstance)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			return;
		}
		doResurrect((L2Character) obj);
	}

	private void handleNonPlayerRes(L2PcInstance activeChar)
	{
		handleNonPlayerRes(activeChar, "");
	}

	private void handleNonPlayerRes(L2PcInstance activeChar, String radiusStr)
	{
		L2Object obj = activeChar.getTarget();

		try
		{
			int radius = 0;

			if(!radiusStr.equals(""))
			{
				radius = Integer.parseInt(radiusStr);

				for(L2Character knownChar : activeChar.getKnownList().getKnownCharactersInRadius(radius))
					if(!(knownChar instanceof L2PcInstance) && !(knownChar instanceof L2ControllableMobInstance))
					{
						doResurrect(knownChar);
					}

				activeChar.sendChatMessage(0, 0, "SYS", "Resurrected all non-players within a " + radius + " unit radius.");
			}
		}
		catch(NumberFormatException e)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Enter a valid radius.");
			return;
		}

		if(obj == null || obj instanceof L2PcInstance || obj instanceof L2ControllableMobInstance)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			return;
		}
		doResurrect((L2Character) obj);
	}

	private void doResurrect(L2Character targetChar)
	{
		if(!targetChar.isDead())
		{
			return;
		}

		if(targetChar instanceof L2PcInstance)
		{
			((L2PcInstance) targetChar).restoreExp(100.0);
		}
		else
		{
			DecayTaskManager.getInstance().cancelDecayTask(targetChar);
		}
		targetChar.doRevive();
	}
}