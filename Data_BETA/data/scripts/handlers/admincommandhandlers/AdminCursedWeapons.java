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

import java.util.Collection;
import java.util.StringTokenizer;

import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.managers.CursedWeaponsManager;
import com.src.gameserver.model.CursedWeapon;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.util.StringUtil;

/**
 * This class handles following admin commands:
 * - cw_info = displays cursed weapon status
 * - cw_remove = removes a cursed weapon from the world, item id or name must be provided
 * - cw_goto = teleports GM to the specified cursed weapon
 * - cw_reload = reloads instance manager
 * - cw_add = adds a cursed weapon into the world, item id or name must be provided. Target will be the weilder
 * - admin_cw_info_menu = open the menu in admin panel
 */
public class AdminCursedWeapons implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_cw_info",
		"admin_cw_remove",
		"admin_cw_goto",
		"admin_cw_reload",
		"admin_cw_add",
		"admin_cw_info_menu"
	};

	private int itemId;

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		GMAudit.auditGMAction(activeChar.getName(), command, (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target"), "");

		CursedWeaponsManager cwm = CursedWeaponsManager.getInstance();
		int id = 0;

		StringTokenizer st = new StringTokenizer(command);
		st.nextToken();

		if(command.startsWith("admin_cw_info"))
		{
			if(!command.contains("menu"))
			{
				activeChar.sendChatMessage(0, 0, "SYS", "====== Cursed Weapons: ======");
				for(CursedWeapon cw : cwm.getCursedWeapons())
				{
					activeChar.sendChatMessage(0, 0, "SYS", "> " + cw.getName() + " (" + cw.getItemId() + ")");
					if(cw.isActivated())
					{
						L2PcInstance pl = cw.getPlayer();
						activeChar.sendChatMessage(0, 0, "SYS", "  Player holding: " + (pl == null ? "null" : pl.getName()));
						activeChar.sendChatMessage(0, 0, "SYS", "    Player karma: " + cw.getPlayerKarma());
						activeChar.sendChatMessage(0, 0, "SYS", "    Time Remaining: " + (cw.getTimeLeft() / 60000) + " min.");
						activeChar.sendChatMessage(0, 0, "SYS", "    Kills : " + cw.getNbKills());
					}
					else if(cw.isDropped())
					{
						activeChar.sendChatMessage(0, 0, "SYS", "  Lying on the ground.");
						activeChar.sendChatMessage(0, 0, "SYS", "    Time Remaining: " + (cw.getTimeLeft() / 60000) + " min.");
						activeChar.sendChatMessage(0, 0, "SYS", "    Kills : " + cw.getNbKills());
					}
					else
					{
						activeChar.sendChatMessage(0, 0, "SYS", "  Don't exist in the world.");
					}

					activeChar.sendPacket(new SystemMessage(SystemMessageId.FRIEND_LIST_FOOTER));
				}
			}
			else
			{
				final Collection<CursedWeapon> cws = cwm.getCursedWeapons();
				final StringBuilder replyMSG = new StringBuilder(cws.size() * 300);
				NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
				adminReply.setFile("data/html/admin/main/cwinfo.htm");
				for(CursedWeapon cw : cwm.getCursedWeapons())
				{
					itemId = cw.getItemId();

					StringUtil.append(replyMSG,
							"<table width=280><tr><td>Name:</td><td>",
							cw.getName(),
					"</td></tr>");

					if(cw.isActivated())
					{
						L2PcInstance pl = cw.getPlayer();
						StringUtil.append(replyMSG,
								"<tr><td>Weilder:</td><td>",
								(pl == null ? "null" : pl.getName()),
								"</td></tr>" +
								"<tr><td>Karma:</td><td>",
								String.valueOf(cw.getPlayerKarma()),
								"</td></tr>" +
								"<tr><td>Kills:</td><td>",
								String.valueOf(cw.getPlayerPkKills()),
								"/",
								String.valueOf(cw.getNbKills()),
								"</td></tr>" +
								"<tr><td>Time remaining:</td><td>",
								String.valueOf(cw.getTimeLeft() / 60000),
								" min.</td></tr>" +
								"<tr><td><button value=\"Remove\" action=\"bypass -h admin_cw_remove ",
								String.valueOf(itemId),
								"\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>" +
								"<td><button value=\"Go\" action=\"bypass -h admin_cw_goto ",
								String.valueOf(itemId),
								"\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td></tr>"
						);
					}
					else if(cw.isDropped())
					{
						StringUtil.append(replyMSG,
								"<tr><td>Position:</td><td>Lying on the ground</td></tr>" +
								"<tr><td>Time remaining:</td><td>",
								String.valueOf(cw.getTimeLeft() / 60000),
								" min.</td></tr>" +
								"<tr><td>Kills:</td><td>",
								String.valueOf(cw.getNbKills()),
								"</td></tr>" +
								"<tr><td><button value=\"Remove\" action=\"bypass -h admin_cw_remove ",
								String.valueOf(itemId),
								"\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>" +
								"<td><button value=\"Go\" action=\"bypass -h admin_cw_goto ",
								String.valueOf(itemId),
								"\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td></tr>"
						);
					}
					else
					{
						StringUtil.append(replyMSG,
								"<tr><td>Position:</td><td>Doesn't exist.</td></tr>" +
								"<tr><td><button value=\"Give to Target\" action=\"bypass -h admin_cw_add ",
								String.valueOf(itemId),
								"\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td><td></td></tr>"
						);
					}

					replyMSG.append("</table><br>");
				}
				adminReply.replace("%cwinfo%", replyMSG.toString());
				activeChar.sendPacket(adminReply);
			}
		}
		else if(command.startsWith("admin_cw_reload"))
		{
			cwm.reload();
		}
		else
		{
			CursedWeapon cw = null;
			try
			{
				String parameter = st.nextToken();
				if(parameter!= null && parameter.matches("[0-9]*"))
				{
					id = Integer.parseInt(parameter);
				}
				else
				{
					parameter = parameter.replace('_', ' ');
					for(CursedWeapon cwp : cwm.getCursedWeapons())
					{
						if(cwp.getName().toLowerCase().contains(parameter.toLowerCase()))
						{
							id = cwp.getItemId();
							break;
						}
					}
				}
				cw = cwm.getCursedWeapon(id);
				if(cw == null)
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Unknown cursed weapon ID.");
					return false;
				}
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //cw_remove|//cw_goto|//cw_add <itemid|name>");
			}

			if(command.startsWith("admin_cw_remove "))
			{
				cw.endOfLife();
			}
			else if(command.startsWith("admin_cw_goto "))
			{
				cw.goTo(activeChar);
			}
			else if(command.startsWith("admin_cw_add"))
			{
				if(cw == null)
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Usage: //cw_add <itemid|name>");
					return false;
				}
				else if(cw.isActive())
				{
					activeChar.sendChatMessage(0, 0, "SYS", "This cursed weapon is already active.");
				}
				else
				{
					
                    long endTime = System.currentTimeMillis() + cw.getDuration() * 60000L;
                    cw.setEndTime(endTime);

					L2Object target = activeChar.getTarget();
					if(target != null && target instanceof L2PcInstance)
					{
						((L2PcInstance)target).addItem("AdminCursedWeaponAdd", id, 1, target, true);
					}
					else
					{
						activeChar.addItem("AdminCursedWeaponAdd", id, 1, activeChar, true);
					}
				}
			}
			else
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Unknown command.");
			}
		}

		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

}