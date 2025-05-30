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

import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.Ride;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class AdminRideWyvern implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_ride_wyvern", 
		"admin_ride_strider",
		"admin_unride_wyvern", 
		"admin_unride_strider",
		"admin_unride",
	};
	
	private int _petRideId;

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		if(command.startsWith("admin_ride"))
		{
			if(activeChar.isMounted() || activeChar.getPet() != null)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("Already Have a Pet or Mounted."));

				return false;
			}

			if(command.startsWith("admin_ride_wyvern"))
			{
				_petRideId = 12621;
				
				activeChar.addSkill(SkillTable.getInstance().getInfo(4289, 1));
				activeChar.sendSkillList();
			}
			else if(command.startsWith("admin_ride_strider"))
			{
				_petRideId = 12526;
			}
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("Command '" + command + "' not recognized"));

				return false;
			}

			if(!activeChar.disarmWeapons())
			{
				return false;
			}

			Ride mount = new Ride(activeChar.getObjectId(), Ride.ACTION_MOUNT, _petRideId);
			activeChar.sendPacket(mount);
			activeChar.broadcastPacket(mount);
			activeChar.setMountType(mount.getMountType());
		}
		else if(command.startsWith("admin_unride"))
		{
            if (activeChar.isFlying())
            {
                // Remove skill Wyvern Breath
                activeChar.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
                activeChar.sendSkillList();
            }

			if(activeChar.setMountType(0))
			{
				Ride dismount = new Ride(activeChar.getObjectId(), Ride.ACTION_DISMOUNT, 0);
				activeChar.broadcastPacket(dismount);
			}
		}
		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}