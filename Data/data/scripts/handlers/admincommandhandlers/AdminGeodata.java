/* This program is free software; you can redistribute it and/or modify
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

import com.src.Config;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.geo.GeoData;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class AdminGeodata implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
			"admin_geo_z",
			"admin_geo_type",
			"admin_geo_nswe",
			"admin_geo_los",
			"admin_geo_position",
			"admin_geo_bug",
			"admin_geo_load",
			"admin_geo_unload"
	};

	private enum CommandEnum
	{
		admin_geo_z,
		admin_geo_type,
		admin_geo_nswe,
		admin_geo_los,
		admin_geo_position,
		admin_geo_bug,
		admin_geo_load,
		admin_geo_unload
	}

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		String target = (activeChar.getTarget() != null) ? activeChar.getTarget().getName() : "no-target";
        GMAudit.auditGMAction(activeChar.getName(), command, target, "");

		String[] wordList = command.split(" ");
		CommandEnum comm;

		try
		{
			comm = CommandEnum.valueOf(wordList[0]);
		}
		catch(Exception e)
		{
			return false;
		}

		CommandEnum commandEnum = comm;
		switch(commandEnum)
		{
			case admin_geo_z:
			case admin_geo_type:
			case admin_geo_nswe:
			case admin_geo_los:
			case admin_geo_position:
			case admin_geo_bug:
			case admin_geo_unload:
				if(Config.GEODATA == 0)
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Geo Engine is Turned Off!");
					return true;
				}
				break;
		default:
			break;
		}

		switch(commandEnum)
		{
			case admin_geo_z:
				activeChar.sendChatMessage(0, 0, "SYS", "GeoEngine: Geo_Z = " + GeoData.getInstance().getHeight(activeChar.getX(), activeChar.getY(), activeChar.getZ()) + " Loc_Z = " + activeChar.getZ());
				break;

			case admin_geo_type:
				short type = GeoData.getInstance().getType(activeChar.getX(), activeChar.getY());
				activeChar.sendChatMessage(0, 0, "SYS", "GeoEngine: Geo_Type = " + type);

				short height = GeoData.getInstance().getHeight(activeChar.getX(), activeChar.getY(), activeChar.getZ());
				activeChar.sendChatMessage(0, 0, "SYS", "GeoEngine: height = " + height);
				break;

			case admin_geo_nswe:
				String result = "";

				short nswe = GeoData.getInstance().getNSWE(activeChar.getX(), activeChar.getY(), activeChar.getZ());

				if((nswe & 8) == 0)
				{
					result += " N";
				}

				if((nswe & 4) == 0)
				{
					result += " S";
				}

				if((nswe & 2) == 0)
				{
					result += " W";
				}

				if((nswe & 1) == 0)
				{
					result += " E";
				}

				activeChar.sendChatMessage(0, 0, "SYS", "GeoEngine: Geo_NSWE -> " + nswe + "->" + result);
				break;

			case admin_geo_los:
				if(activeChar.getTarget() != null)
				{
					if(GeoData.getInstance().canSeeTargetDebug(activeChar, activeChar.getTarget()))
					{
						activeChar.sendChatMessage(0, 0, "SYS", "GeoEngine: Can See Target!");
					}
					else
					{
						activeChar.sendChatMessage(0, 0, "SYS", "GeoEngine: Can't See Target!");
					}
				}
				else
				{
					activeChar.sendChatMessage(0, 0, "SYS", "None Target!");
				}
				break;

			case admin_geo_position:
				activeChar.sendChatMessage(0, 0, "SYS", "GeoEngine: Your current position: ");
				activeChar.sendChatMessage(0, 0, "SYS", ".... world coords: x: " + activeChar.getX() + " y: " + activeChar.getY() + " z: " + activeChar.getZ());
				activeChar.sendChatMessage(0, 0, "SYS", ".... geo position: " + GeoData.getInstance().geoPosition(activeChar.getX(), activeChar.getY()));
				break;

			case admin_geo_load:
				String[] v = command.substring(15).split(" ");

				if(v.length != 2)
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Usage: //admin_geo_load <regionX> <regionY>");
				}
				else
				{
					try
					{
						byte rx = Byte.parseByte(v[0]);
						byte ry = Byte.parseByte(v[1]);

						boolean result2 = GeoData.getInstance().loadGeodataFile(rx, ry);

						if(result2)
						{
							activeChar.sendChatMessage(0, 0, "SYS", "GeoEngine: File for region [" + rx + "," + ry + "] loaded succesfuly");
						}
						else
						{
							activeChar.sendChatMessage(0, 0, "SYS", "GeoEngine: File for region [" + rx + "," + ry + "] couldn't be loaded");
						}
					}
					catch(Exception e)
					{
						activeChar.sendChatMessage(0, 0, "SYS", "You have to write numbers of regions <regionX> <regionY>");
					}
				}
				break;

			case admin_geo_unload:
				String[] v2 = command.substring(17).split(" ");

				if(v2.length != 2)
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Usage: //admin_geo_unload <regionX> <regionY>");
				}
				else
				{
					try
					{
						byte rx = Byte.parseByte(v2[0]);
						byte ry = Byte.parseByte(v2[1]);

						GeoData.getInstance().unloadGeodata(rx, ry);
						activeChar.sendChatMessage(0, 0, "SYS", "GeoEngine: File for region [" + rx + "," + ry + "] unloaded.");
					}
					catch(Exception e)
					{
						activeChar.sendChatMessage(0, 0, "SYS", "You have to write numbers of regions <regionX> <regionY>");
					}
				}
				break;

			case admin_geo_bug:
				try
				{
					String comment = command.substring(14);
					GeoData.getInstance().addGeoDataBug(activeChar, comment);
				}
				catch(StringIndexOutOfBoundsException e)
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Usage: //admin_geo_bug you coments here");
				}
				break;
		}

		wordList = null;
		comm = null;
		commandEnum = null;

		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

}