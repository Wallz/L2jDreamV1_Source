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

import java.util.StringTokenizer;

import javolution.text.TextBuilder;
import javolution.util.FastList;

import com.src.Config;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.managers.CastleManorManager;
import com.src.gameserver.managers.CastleManorManager.CropProcure;
import com.src.gameserver.managers.CastleManorManager.SeedProduction;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.siege.Castle;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;

public class AdminManor implements IAdminCommandHandler
{
	private static final String[] _adminCommands =
	{
		"admin_manor", 
		"admin_manor_reset",
		"admin_manor_save",
		"admin_manor_disable"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		GMAudit.auditGMAction(activeChar.getName(), command, (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target"), "");

		StringTokenizer st = new StringTokenizer(command);
		command = st.nextToken();

		if(command.equals("admin_manor"))
		{
			showMainPage(activeChar);
		}
		else if(command.equals("admin_manor_reset"))
		{
			int castleId = 0;

			try
			{
				castleId = Integer.parseInt(st.nextToken());
			}
			catch(Exception e)
			{
			}

			if(castleId > 0)
			{
				Castle castle = CastleManager.getInstance().getCastleById(castleId);
				castle.setCropProcure(new FastList<CropProcure>(), CastleManorManager.PERIOD_CURRENT);
				castle.setCropProcure(new FastList<CropProcure>(), CastleManorManager.PERIOD_NEXT);
				castle.setSeedProduction(new FastList<SeedProduction>(), CastleManorManager.PERIOD_CURRENT);
				castle.setSeedProduction(new FastList<SeedProduction>(), CastleManorManager.PERIOD_NEXT);

				if(Config.ALT_MANOR_SAVE_ALL_ACTIONS)
				{
					castle.saveCropData();
					castle.saveSeedData();
				}

				activeChar.sendChatMessage(0, 0, "SYS", "Manor data for " + castle.getName() + " was nulled");
			}
			else
			{
				for(Castle castle : CastleManager.getInstance().getCastles())
				{
					castle.setCropProcure(new FastList<CropProcure>(), CastleManorManager.PERIOD_CURRENT);
					castle.setCropProcure(new FastList<CropProcure>(), CastleManorManager.PERIOD_NEXT);
					castle.setSeedProduction(new FastList<SeedProduction>(), CastleManorManager.PERIOD_CURRENT);
					castle.setSeedProduction(new FastList<SeedProduction>(), CastleManorManager.PERIOD_NEXT);

					if(Config.ALT_MANOR_SAVE_ALL_ACTIONS)
					{
						castle.saveCropData();
						castle.saveSeedData();
					}
				}

				activeChar.sendChatMessage(0, 0, "SYS", "Manor data was nulled");
			}

			showMainPage(activeChar);
		}
		else if(command.equals("admin_manor_save"))
		{
			CastleManorManager.getInstance().save();
			activeChar.sendChatMessage(0, 0, "SYS", "Manor System: all data saved");
			showMainPage(activeChar);
		}
		else if(command.equals("admin_manor_disable"))
		{
			boolean mode = CastleManorManager.getInstance().isDisabled();

			CastleManorManager.getInstance().setDisabled(!mode);

			if(mode)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Manor System: enabled");
			}
			else
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Manor System: disabled");
			}

			showMainPage(activeChar);
		}
		return true;
	}

	public String[] getAdminCommandList()
	{
		return _adminCommands;
	}

	private void showMainPage(L2PcInstance activeChar)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		TextBuilder replyMSG = new TextBuilder("<html><body>");

		replyMSG.append("<center><font color=\"LEVEL\"> [Manor System] </font></center><br>");
		replyMSG.append("<table width=\"100%\"><tr><td>");
		replyMSG.append("Disabled: " + (CastleManorManager.getInstance().isDisabled() ? "yes" : "no") + "</td><td>");
		replyMSG.append("Under Maintenance: " + (CastleManorManager.getInstance().isUnderMaintenance() ? "yes" : "no") + "</td></tr><tr><td>");
		replyMSG.append("<tr><td>Approved: " + (CastleManorManager.APPROVE == 1 ? "yes" : "no") + "</td></tr>");
		replyMSG.append("</table>");
		replyMSG.append("<center><table><tr><td>");
		replyMSG.append("<button value=\"" + (CastleManorManager.getInstance().isDisabled() ? "Enable" : "Disable") + "\" action=\"bypass -h admin_manor_disable\" width=110 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr><tr><td>");
		replyMSG.append("<button value=\"Refresh\" action=\"bypass -h admin_manor\" width=110 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td><td>");
		replyMSG.append("<button value=\"Back\" action=\"bypass -h admin_admin\" width=110 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
		replyMSG.append("</table></center>");
		replyMSG.append("<br><center>Castle Information:<table width=\"100%\">");
		replyMSG.append("<tr><td></td><td>Current Period</td><td>Next Period</td></tr>");

		for(Castle c : CastleManager.getInstance().getCastles())
		{
			replyMSG.append("<tr><td>" + c.getName() + "</td><td>" + c.getManorCost(CastleManorManager.PERIOD_CURRENT) + "a</td>" + "<td>" + c.getManorCost(CastleManorManager.PERIOD_NEXT) + "a</td></tr>");
		}

		replyMSG.append("</table><br>");
		replyMSG.append("</body></html>");

		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
}