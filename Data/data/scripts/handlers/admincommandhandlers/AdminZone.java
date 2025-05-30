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

import com.src.gameserver.datatables.GmListTable;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.datatables.xml.MapRegionTable;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.Location;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class AdminZone implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_zone_check",
		"admin_zone_reload"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if(activeChar == null)
		{
			return false;
		}

		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		GMAudit.auditGMAction(activeChar.getName(), command, (activeChar.getTarget() != null ? activeChar.getTarget().getName() : "no-target"), "");

		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken();

		if(actualCommand.equalsIgnoreCase("admin_zone_check"))
		{
			if(activeChar.isInsideZone(L2Character.ZONE_PVP))
			{
				activeChar.sendChatMessage(0, 0, "SYS", "This is a PvP zone.");
			}
			else
			{
				activeChar.sendChatMessage(0, 0, "SYS", "This is NOT a PvP zone.");
			}

			if(activeChar.isInsideZone(L2Character.ZONE_NOLANDING))
			{
				activeChar.sendChatMessage(0, 0, "SYS", "This is a no landing zone.");
			}
			else
			{
				activeChar.sendChatMessage(0, 0, "SYS", "This is NOT a no landing zone.");
			}

			activeChar.sendChatMessage(0, 0, "SYS", "MapRegion: x:" + MapRegionTable.getInstance().getMapRegionX(activeChar.getX()) + " y:" + MapRegionTable.getInstance().getMapRegionX(activeChar.getY()));

			activeChar.sendChatMessage(0, 0, "SYS", "Closest Town: " + MapRegionTable.getInstance().getClosestTownName(activeChar));

			Location loc;

			loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.Castle);
			activeChar.sendChatMessage(0, 0, "SYS", "TeleToLocation (Castle): x:" + loc.getX() + " y:" + loc.getY() + " z:" + loc.getZ());

			loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.ClanHall);
			activeChar.sendChatMessage(0, 0, "SYS", "TeleToLocation (ClanHall): x:" + loc.getX() + " y:" + loc.getY() + " z:" + loc.getZ());

			loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.SiegeFlag);
			activeChar.sendChatMessage(0, 0, "SYS", "TeleToLocation (SiegeFlag): x:" + loc.getX() + " y:" + loc.getY() + " z:" + loc.getZ());

			loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.Town);
			activeChar.sendChatMessage(0, 0, "SYS", "TeleToLocation (Town): x:" + loc.getX() + " y:" + loc.getY() + " z:" + loc.getZ());

			loc = null;
		}
		else if(actualCommand.equalsIgnoreCase("admin_zone_reload"))
		{
			GmListTable.broadcastMessageToGMs("Zones can not be reloaded in this version.");
		}
		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}