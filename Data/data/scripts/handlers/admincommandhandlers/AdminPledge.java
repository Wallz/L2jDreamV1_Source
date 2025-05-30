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

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.GMViewPledgeInfo;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class AdminPledge implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_pledge"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;

		if(target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			showMainPage(activeChar);
			return false;
		}

		String name = player.getName();

		if(command.startsWith("admin_pledge"))
		{
			String action = null;
			String parameter = null;
			GMAudit.auditGMAction(activeChar.getName(), command, activeChar.getName(), "");

			StringTokenizer st = new StringTokenizer(command);

			try
			{
				st.nextToken();
				action = st.nextToken();
				parameter = st.nextToken();
			}
			catch(NoSuchElementException nse)
			{
			}

			if(action.equals("create"))
			{
				long cet = player.getClanCreateExpiryTime();

				if(parameter.length() == 0)
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Please, enter clan name.");
					return false;
				}

				player.setClanCreateExpiryTime(0);
				L2Clan clan = ClanTable.getInstance().createClan(player, parameter);

				if(clan != null)
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Clan " + parameter + " created. Leader: " + player.getName());
				}
				else
				{
					player.setClanCreateExpiryTime(cet);
					activeChar.sendChatMessage(0, 0, "SYS", "There was a problem while creating the clan.");
				}
			}
			else if(!player.isClanLeader())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_IS_NOT_A_CLAN_LEADER).addString(name));
				showMainPage(activeChar);
				return false;
			}
			else if(action.equals("dismiss"))
			{
				ClanTable.getInstance().destroyClan(player.getClanId(),null);
				L2Clan clan = player.getClan();

				if(clan == null)
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Clan disbanded.");
				}
				else
				{
					activeChar.sendChatMessage(0, 0, "SYS", "There was a problem while destroying the clan.");
				}
			}
			else if(action.equals("info"))
			{
				activeChar.sendPacket(new GMViewPledgeInfo(player.getClan(), player));
			}
			else if(parameter == null)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //pledge <setlevel|rep> <number>");
			}
			else if(action.equals("setlevel"))
			{
				int level = Integer.parseInt(parameter);

				if(level >= 0 && level < 9)
				{
					player.getClan().changeLevel(level);
					activeChar.sendChatMessage(0, 0, "SYS", "You set level " + level + " for clan " + player.getClan().getName());
				}
				else
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Level incorrect.");
				}
			}
			else if(action.startsWith("rep"))
			{
				try
				{
					int points = Integer.parseInt(parameter);

					L2Clan clan = player.getClan();

					if(clan.getLevel() < 5)
					{
						activeChar.sendChatMessage(0, 0, "SYS", "Only clans of level 5 or above may receive reputation points.");
						showMainPage(activeChar);

						return false;
					}

					clan.setReputationScore(points, true);
					activeChar.sendChatMessage(0, 0, "SYS", "You " + (points > 0 ? "add " : "remove ") + Math.abs(points) + " points " + (points > 0 ? "to " : "from ") + clan.getName() + "'s reputation. Their current score is " + clan.getReputationScore());
					clan = null;
				}
				catch(Exception e)
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Usage: //pledge <rep> <number>");
				}
			}
		}
		showMainPage(activeChar);
		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void showMainPage(L2PcInstance activeChar)
	{
		AdminHelpPage.showHelpPage(activeChar, "game_menu.htm");
	}
}