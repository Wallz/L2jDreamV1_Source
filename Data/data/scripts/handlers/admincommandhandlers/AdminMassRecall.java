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

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.L2Party;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class AdminMassRecall implements IAdminCommandHandler
{
	private static String[] _adminCommands =
	{
		"admin_recallclan",
		"admin_recallparty", 
		"admin_recallally"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		GMAudit.auditGMAction(activeChar.getName(), command, (activeChar.getTarget() != null ? activeChar.getTarget().getName() : "no-target"), "");

		if(command.startsWith("admin_recallclan"))
		{
			try
			{
				String val = command.substring(17).trim();

				L2Clan clan = ClanTable.getInstance().getClanByName(val);

				if(clan == null)
				{
					activeChar.sendChatMessage(0, 0, "SYS", "This clan doesn't exists.");
					return true;
				}

				L2PcInstance[] m = clan.getOnlineMembers("");

				for(L2PcInstance element : m)
				{
					Teleport(element, activeChar.getX(), activeChar.getY(), activeChar.getZ(), "Admin is teleporting you");
				}
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Error in recallclan command.");
			}
		}
		else if(command.startsWith("admin_recallally"))
		{
			try
			{
				String val = command.substring(17).trim();
				L2Clan clan = ClanTable.getInstance().getClanByName(val);

				if(clan == null)
				{
					activeChar.sendChatMessage(0, 0, "SYS", "This clan doesn't exists.");
					return true;
				}

				int ally = clan.getAllyId();

				if(ally == 0)
				{

					L2PcInstance[] m = clan.getOnlineMembers("");

					for(L2PcInstance element : m)
					{
						Teleport(element, activeChar.getX(), activeChar.getY(), activeChar.getZ(), "Admin is teleporting you");
					}

					m = null;
				}
				else
				{
					for(L2Clan aclan : ClanTable.getInstance().getClans())
					{
						if(aclan.getAllyId() == ally)
						{
							L2PcInstance[] m = aclan.getOnlineMembers("");

							for(L2PcInstance element : m)
							{
								Teleport(element, activeChar.getX(), activeChar.getY(), activeChar.getZ(), "Admin is teleporting you");
							}
						}
					}
				}
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Error in recallally command.");
			}
		}
		else if(command.startsWith("admin_recallparty"))
		{
			try
			{
				String val = command.substring(18).trim();
				L2PcInstance player = L2World.getInstance().getPlayer(val);

				if(player == null)
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Target error.");
					return true;
				}

				if(!player.isInParty())
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Player is not in party.");
					return true;
				}

				L2Party p = player.getParty();

				for(L2PcInstance ppl : p.getPartyMembers())
				{
					Teleport(ppl, activeChar.getX(), activeChar.getY(), activeChar.getZ(), "Admin is teleporting you");
				}
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Error in recallparty command.");
			}
		}
		return true;
	}

	private void Teleport(L2PcInstance player, int X, int Y, int Z, String Message)
	{
		player.sendMessage(Message);
		player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		player.teleToLocation(X, Y, Z, true);
	}

	public String[] getAdminCommandList()
	{
		return _adminCommands;
	}
}