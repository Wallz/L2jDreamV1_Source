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

import java.util.logging.Logger;

import com.src.gameserver.TradeController;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2TradeList;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.BuyList;

public class AdminShop implements IAdminCommandHandler
{
	private static Logger _log = Logger.getLogger(AdminShop.class.getName());

	private static final String[] ADMIN_COMMANDS =
	{
		"admin_buy",
		"admin_gmshop"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		if(command.startsWith("admin_buy"))
		{
			try
			{
				handleBuyRequest(activeChar, command.substring(10));
			}
			catch(IndexOutOfBoundsException e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Please specify buylist.");
			}
		}
		else if(command.equals("admin_gmshop"))
		{
			AdminHelpPage.showHelpPage(activeChar, "gm/gmshops.htm");
		}

		String target = (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target");
		GMAudit.auditGMAction(activeChar.getName(), command, target, "");
		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void handleBuyRequest(L2PcInstance activeChar, String command)
	{
		int val = -1;

		try
		{
			val = Integer.parseInt(command);
		}
		catch(Exception e)
		{
			_log.warning("admin buylist failed:" + command);
		}

		L2TradeList list = TradeController.getInstance().getBuyList(val);

		if(list != null)
		{
			activeChar.sendPacket(new BuyList(list, activeChar.getAdena()));
		}
		else
		{
			_log.warning("no buylist with id:" + val);
		}

		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
	}
}