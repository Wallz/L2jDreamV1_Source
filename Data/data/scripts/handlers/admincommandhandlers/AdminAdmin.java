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

import com.src.Config;
import com.src.gameserver.datatables.GmListTable;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.olympiad.Olympiad;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class AdminAdmin implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_admin",
		"admin_admin1",
		"admin_admin2",
		"admin_admin3",
		"admin_admin4",
		"admin_gmliston",
		"admin_gmlistoff",
		"admin_silence",
		"admin_diet",
		"admin_set",
		"admin_set_menu",
		"admin_set_mod",
		"admin_saveolymp",
		"admin_manualhero"
	};

	private enum CommandEnum
	{
		admin_admin,
		admin_admin2,
		admin_admin3,
		admin_admin4,
		admin_gmliston,
		admin_gmlistoff,
		admin_silence,
		admin_diet,
		admin_set,
		admin_set_menu,
		admin_set_mod,
		admin_saveolymp,
		admin_manualhero
	}

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		GMAudit.auditGMAction(activeChar.getName(), command, (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target"), "");

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
			case admin_admin:
			case admin_admin2:
			case admin_admin3:
			case admin_admin4:
				showMainPage(activeChar, command);
				break;

			case admin_gmliston:
				GmListTable.getInstance().showGm(activeChar);
				activeChar.sendChatMessage(0, 0, "SYS", "Registerd into GM list");
				break;

			case admin_gmlistoff:
				GmListTable.getInstance().hideGm(activeChar);
				activeChar.sendChatMessage(0, 0, "SYS", "Removed from GM list");
				break;

			case admin_silence:
				if(activeChar.getMessageRefusal())
				{
					activeChar.setMessageRefusal(false);
					activeChar.sendPacket(new SystemMessage(SystemMessageId.MESSAGE_ACCEPTANCE_MODE));
				}
				else
				{
					activeChar.setMessageRefusal(true);
					activeChar.sendPacket(new SystemMessage(SystemMessageId.MESSAGE_REFUSAL_MODE));
				}
				break;

			case admin_saveolymp:
				try
				{
					Olympiad.getInstance().save();
				}

				catch(Exception e)
				{
					e.printStackTrace();
				}

				activeChar.sendChatMessage(0, 0, "SYS", "Olympiad system saved.");
				break;

			case admin_manualhero:
				try
				{
					Olympiad.getInstance().manualSelectHeroes();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}

				activeChar.sendChatMessage(0, 0, "SYS", "Heroes formed.");
				break;

			case admin_diet:
				try
				{
					StringTokenizer st = new StringTokenizer(command);
					st.nextToken();

					if(st.nextToken().equalsIgnoreCase("on"))
					{
						activeChar.setDietMode(true);
						activeChar.sendChatMessage(0, 0, "SYS", "Diet mode on.");
					}
					else if(st.nextToken().equalsIgnoreCase("off"))
					{
						activeChar.setDietMode(false);
						activeChar.sendChatMessage(0, 0, "SYS", "Diet mode off.");
					}
				}
				catch(Exception ex)
				{
					if(activeChar.getDietMode())
					{
						activeChar.setDietMode(false);
						activeChar.sendChatMessage(0, 0, "SYS", "Diet mode off.");
					}
					else
					{
						activeChar.setDietMode(true);
						activeChar.sendChatMessage(0, 0, "SYS", "Diet mode on.");
					}
				}
				finally
				{
					activeChar.refreshOverloaded();
				}
				break;

			case admin_set:
				StringTokenizer st = new StringTokenizer(command);
				String[] cmd = st.nextToken().split("_");

				try
				{
					String[] parameter = st.nextToken().split("=");
					String pName = parameter[0].trim();
					String pValue = parameter[1].trim();

					if(Config.setParameterValue(pName, pValue))
					{
						activeChar.sendChatMessage(0, 0, "SYS", "parameter " + pName + " succesfully set to " + pValue + ".");
					}
					else
					{
						activeChar.sendChatMessage(0, 0, "SYS", "Invalid parameter!");
					}
				}
				catch(Exception e)
				{
					if(cmd.length == 2)
					{
						activeChar.sendChatMessage(0, 0, "SYS", "Usage: //set parameter=value .");
					}
				}
				finally
				{
					st = null;

					if(cmd.length == 3)
					{
						if(cmd[2].equalsIgnoreCase("menu"))
						{
							AdminHelpPage.showHelpPage(activeChar, "settings.htm");
						}
						else if(cmd[2].equalsIgnoreCase("mod"))
						{
							AdminHelpPage.showHelpPage(activeChar, "mods_menu.htm");
						}
					}
				}
				break;
		default:
			break;
		}
		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void showMainPage(L2PcInstance activeChar, String command)
	{
		int mode = 0;
		String filename = null;

		try
		{
			mode = Integer.parseInt(command.substring(11));
		}

		catch(Exception e)
		{}

		switch(mode)
		{
			case 1:
				filename = "main";
				break;
			case 2:
				filename = "game";
				break;
			case 3:
				filename = "effects";
				break;
			case 4:
				filename = "gm";
				break;
			default:
				filename = "main";
				break;
		}

		AdminHelpPage.showHelpPage(activeChar, filename + "_menu.htm");

		filename = null;
	}
}