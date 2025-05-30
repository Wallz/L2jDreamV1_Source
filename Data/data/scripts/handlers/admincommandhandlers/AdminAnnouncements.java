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
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.AutoAnnouncementHandler;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.Announcements;

public class AdminAnnouncements implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_list_announcements",
		"admin_reload_announcements",
		"admin_announce_announcements",
		"admin_add_announcement",
		"admin_del_announcement",
		"admin_announce",
		"admin_announce_menu",
		"admin_list_autoannouncements",
		"admin_add_autoannouncement",
		"admin_del_autoannouncement",
		"admin_autoannounce"
	};

	private enum CommandEnum
	{
		admin_list_announcements,
		admin_reload_announcements,
		admin_announce_announcements,
		admin_add_announcement,
		admin_del_announcement,
		admin_announce,
		admin_announce_menu,
		admin_list_autoannouncements,
		admin_add_autoannouncement,
		admin_del_autoannouncement,
		admin_autoannounce
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
			case admin_list_announcements:
				Announcements.getInstance().listAnnouncements(activeChar);
				break;

			case admin_reload_announcements:
				Announcements.getInstance().loadAnnouncements();
				Announcements.getInstance().listAnnouncements(activeChar);
				break;

			case admin_announce_menu:
				Announcements sys = new Announcements();
				sys.handleAnnounce(command, 20);
				Announcements.getInstance().listAnnouncements(activeChar);
				break;

			case admin_announce_announcements:
				for(L2PcInstance player : L2World.getInstance().getAllPlayers())
				{
					Announcements.getInstance().showAnnouncements(player);
				}

				Announcements.getInstance().listAnnouncements(activeChar);
				break;

			case admin_add_announcement:
				if(!command.equals("admin_add_announcement"))
				{
					try
					{
						String val = command.substring(23);
						Announcements.getInstance().addAnnouncement(val);
						Announcements.getInstance().listAnnouncements(activeChar);
					}
					catch(StringIndexOutOfBoundsException e)
					{
					}
				}
				break;

			case admin_del_announcement:
				try
				{
					int val = new Integer(command.substring(23)).intValue();
					Announcements.getInstance().delAnnouncement(val);
					Announcements.getInstance().listAnnouncements(activeChar);
				}
				catch(StringIndexOutOfBoundsException e)
				{
				}
				break;

			case admin_announce:
				if(Config.GM_ANNOUNCER_NAME)
				{
					command = command + " [ " + activeChar.getName() + " ]";
				}

				Announcements.getInstance().handleAnnounce(command, 15);
				break;

			case admin_list_autoannouncements:
				AutoAnnouncementHandler.getInstance().listAutoAnnouncements(activeChar);
				break;

			case admin_add_autoannouncement:
				if(!command.equals("admin_add_autoannouncement"))
				{
					try
					{
						StringTokenizer st = new StringTokenizer(command.substring(27));
						int delay = Integer.parseInt(st.nextToken().trim());
						String autoAnnounce = st.nextToken();

						if(delay > 30)
						{
							while(st.hasMoreTokens())
							{
								autoAnnounce = autoAnnounce + " " + st.nextToken();
							};

							AutoAnnouncementHandler.getInstance().registerAnnouncment(autoAnnounce, delay);
							AutoAnnouncementHandler.getInstance().listAutoAnnouncements(activeChar);
						}
					}
					catch(StringIndexOutOfBoundsException e)
					{
					}
				}
				break;

			case admin_del_autoannouncement:
				try
				{
					int val = new Integer(command.substring(27)).intValue();
					AutoAnnouncementHandler.getInstance().removeAnnouncement(val);
					AutoAnnouncementHandler.getInstance().listAutoAnnouncements(activeChar);
				}
				catch(StringIndexOutOfBoundsException e)
				{
				}
				break;

			case admin_autoannounce:
				AutoAnnouncementHandler.getInstance().listAutoAnnouncements(activeChar);
				break;
		}
		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}