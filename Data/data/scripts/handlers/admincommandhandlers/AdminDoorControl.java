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
import com.src.gameserver.datatables.xml.DoorTable;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.instance.L2DoorInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.siege.Castle;

public class AdminDoorControl implements IAdminCommandHandler
{
	private static DoorTable _doorTable;
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_open",
		"admin_close",
		"admin_openall",
		"admin_closeall"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		_doorTable = DoorTable.getInstance();

		L2Object target2 = null;

		if(command.startsWith("admin_close "))
		{
			try
			{
				int doorId = Integer.parseInt(command.substring(12));

				if(_doorTable.getDoor(doorId) != null)
				{
					_doorTable.getDoor(doorId).closeMe();
				}
				else
				{
					for(Castle castle : CastleManager.getInstance().getCastles())
					{
						if(castle.getDoor(doorId) != null)
						{
							castle.getDoor(doorId).closeMe();
						}
					}
				}
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Wrong ID door.");
				e.printStackTrace();
				return false;
			}
		}
		else if(command.equals("admin_close"))
		{
			target2 = activeChar.getTarget();

			if(target2 instanceof L2DoorInstance)
			{
				((L2DoorInstance) target2).closeMe();
			}
			else
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Incorrect target.");
			}
		}
		else if(command.startsWith("admin_open "))
		{
			try
			{
				int doorId = Integer.parseInt(command.substring(11));

				if(_doorTable.getDoor(doorId) != null)
				{
					_doorTable.getDoor(doorId).openMe();
				}
				else
				{
					for(Castle castle : CastleManager.getInstance().getCastles())
					{
						if(castle.getDoor(doorId) != null)
						{
							castle.getDoor(doorId).openMe();
						}
					}
				}
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Wrong ID door.");
				e.printStackTrace();
				return false;
			}
		}
		else if(command.equals("admin_open"))
		{
			target2 = activeChar.getTarget();

			if(target2 instanceof L2DoorInstance)
			{
				((L2DoorInstance) target2).openMe();
			}
			else
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Incorrect target.");
			}
		}

		else if(command.equals("admin_closeall"))
		{
			try
			{
				for(L2DoorInstance door : _doorTable.getDoors())
				{
					door.closeMe();
				}

				for(Castle castle : CastleManager.getInstance().getCastles())
				{
					for(L2DoorInstance door : castle.getDoors())
					{
						door.closeMe();
					}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				return false;
			}
		}
		else if(command.equals("admin_openall"))
		{
			try
			{
				for(L2DoorInstance door : _doorTable.getDoors())
				{
					door.openMe();
				}

				for(Castle castle : CastleManager.getInstance().getCastles())
				{
					for(L2DoorInstance door : castle.getDoors())
					{
						door.openMe();
					}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				return false;
			}
		}

		String target = (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target");
        GMAudit.auditGMAction(activeChar.getName(), command, target, "");
		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}