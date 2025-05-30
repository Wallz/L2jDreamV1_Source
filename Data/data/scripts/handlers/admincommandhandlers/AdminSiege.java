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

import javolution.text.TextBuilder;

import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.managers.AuctionManager;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.managers.ClanHallManager;
import com.src.gameserver.managers.SiegeManager;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.ClanHall;
import com.src.gameserver.model.entity.siege.Castle;
import com.src.gameserver.model.zone.type.L2ClanHallZone;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class AdminSiege implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
			"admin_siege",
			"admin_add_attacker",
			"admin_add_defender",
			"admin_add_guard",
			"admin_list_siege_clans",
			"admin_clear_siege_list",
			"admin_move_defenders",
			"admin_spawn_doors",
			"admin_endsiege",
			"admin_startsiege",
			"admin_setcastle",
			"admin_removecastle",
			"admin_clanhall",
			"admin_clanhallset",
			"admin_clanhalldel",
			"admin_clanhallopendoors",
			"admin_clanhallclosedoors",
			"admin_clanhallteleportself"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		GMAudit.auditGMAction(activeChar.getName(), command, (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target"), "");

		StringTokenizer st = new StringTokenizer(command, " ");
		command = st.nextToken();

		Castle castle = null;
		ClanHall clanhall = null;

		if(command.startsWith("admin_clanhall"))
		{
			clanhall = ClanHallManager.getInstance().getClanHallById(Integer.parseInt(st.nextToken()));
		}
		else if(st.hasMoreTokens())
		{
			castle = CastleManager.getInstance().getCastle(st.nextToken());
		}

		String val = "";

		if(st.hasMoreTokens())
		{
			val = st.nextToken();
		}

		if((castle == null || castle.getCastleId() < 0) && clanhall == null)
		{
			showCastleSelectPage(activeChar);
		}
		else
		{
			L2Object target = activeChar.getTarget();
			L2PcInstance player = null;

			if(target instanceof L2PcInstance)
			{
				player = (L2PcInstance) target;
			}

			if(command.equalsIgnoreCase("admin_add_attacker"))
			{
				if(player == null)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				}
				else if(SiegeManager.getInstance().checkIsRegistered(player.getClan(), castle.getCastleId()))
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Clan is already registered!");
				}
				else
				{
					castle.getSiege().registerAttacker(player, true);
				}
			}
			else if(command.equalsIgnoreCase("admin_add_defender"))
			{
				if(player == null)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				}
				else
				{
					castle.getSiege().registerDefender(player, true);
				}
			}
			else if(command.equalsIgnoreCase("admin_add_guard"))
			{
				try
				{
					int npcId = Integer.parseInt(val);
					castle.getSiege().getSiegeGuardManager().addSiegeGuard(activeChar, npcId);
				}
				catch(Exception e)
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Usage: //add_guard npcId");
				}
			}
			else if(command.equalsIgnoreCase("admin_clear_siege_list"))
			{
				castle.getSiege().clearSiegeClan();
			}
			else if(command.equalsIgnoreCase("admin_endsiege"))
			{
				castle.getSiege().endSiege();
			}
			else if(command.equalsIgnoreCase("admin_list_siege_clans"))
			{
				castle.getSiege().listRegisterClan(activeChar);

				return true;
			}
			else if(command.equalsIgnoreCase("admin_move_defenders"))
			{
				activeChar.sendPacket(SystemMessage.sendString("Not implemented yet."));
			}
			else if(command.equalsIgnoreCase("admin_setcastle"))
			{
				if(player == null || player.getClan() == null)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				}
				else
				{
					if (player.getClan().getHasCastle() == 0)
					{
						castle.setOwner(player.getClan());
					}
					else
					{
						activeChar.sendChatMessage(0, 0, "SYS", "This clan has castle already!");
					}
				}
			}
			else if(command.equalsIgnoreCase("admin_removecastle"))
			{
				L2Clan clan = ClanTable.getInstance().getClan(castle.getOwnerId());

				if(clan != null)
				{
					castle.removeOwner(clan);
				}
				else
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Unable to remove castle");
				}
			}
			else if(command.equalsIgnoreCase("admin_clanhallset"))
			{
				if(player == null || player.getClan() == null)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				}
				else if(!ClanHallManager.getInstance().isFree(clanhall.getId()))
				{
					activeChar.sendChatMessage(0, 0, "SYS", "This ClanHall isn't free!");
				}
				else if(player.getClan().getHasHideout() == 0)
				{
					ClanHallManager.getInstance().setOwner(clanhall.getId(), player.getClan());

					if(AuctionManager.getInstance().getAuction(clanhall.getId()) != null)
					{
						AuctionManager.getInstance().getAuction(clanhall.getId()).deleteAuctionFromDB();
					}
				}
				else
				{
					activeChar.sendChatMessage(0, 0, "SYS", "You have already a ClanHall!");
				}
			}
			else if(command.equalsIgnoreCase("admin_clanhalldel"))
			{
				if(!ClanHallManager.getInstance().isFree(clanhall.getId()))
				{
					ClanHallManager.getInstance().setFree(clanhall.getId());
					AuctionManager.getInstance().initNPC(clanhall.getId());
				}
				else
				{
					activeChar.sendChatMessage(0, 0, "SYS", "This ClanHall is already Free!");
				}
			}
			else if(command.equalsIgnoreCase("admin_clanhallopendoors"))
			{
				clanhall.openCloseDoors(true);
			}
			else if(command.equalsIgnoreCase("admin_clanhallclosedoors"))
			{
				clanhall.openCloseDoors(false);
			}
			else if(command.equalsIgnoreCase("admin_clanhallteleportself"))
			{
				L2ClanHallZone zone = clanhall.getZone();

				if(zone != null)
				{
					activeChar.teleToLocation(zone.getSpawn(), true);
				}
			}
			else if(command.equalsIgnoreCase("admin_spawn_doors"))
			{
				castle.spawnDoor();
			}
			else if(command.equalsIgnoreCase("admin_startsiege"))
			{
				castle.getSiege().startSiege();
			}

			if(clanhall != null)
			{
				showClanHallPage(activeChar, clanhall);
			}
			else
			{
				showSiegePage(activeChar, castle.getName());
			}
		}
		return true;
	}

	private void showCastleSelectPage(L2PcInstance activeChar)
	{
		int i = 0;

		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/game/castles.htm");
		TextBuilder cList = new TextBuilder();

		for(Castle castle : CastleManager.getInstance().getCastles())
		{
			if(castle != null)
			{
				String name = castle.getName();
				cList.append("<td fixwidth=90><a action=\"bypass -h admin_siege " + name + "\">" + name + "</a></td>");
				i++;
				name = null;
			}

			if(i > 2)
			{
				cList.append("</tr><tr>");
				i = 0;
			}
		}

		adminReply.replace("%castles%", cList.toString());
		cList.clear();
		i = 0;

		for(ClanHall clanhall : ClanHallManager.getInstance().getClanHalls().values())
		{
			if(clanhall != null)
			{
				cList.append("<td fixwidth=134><a action=\"bypass -h admin_clanhall " + clanhall.getId() + "\">");
				cList.append(clanhall.getName() + "</a></td>");
				i++;
			}

			if(i > 1)
			{
				cList.append("</tr><tr>");
				i = 0;
			}
		}

		adminReply.replace("%clanhalls%", cList.toString());
		cList.clear();
		i = 0;

		for(ClanHall clanhall : ClanHallManager.getInstance().getFreeClanHalls().values())
		{
			if(clanhall != null)
			{
				cList.append("<td fixwidth=134><a action=\"bypass -h admin_clanhall " + clanhall.getId() + "\">");
				cList.append(clanhall.getName() + "</a></td>");
				i++;
			}

			if(i > 1)
			{
				cList.append("</tr><tr>");
				i = 0;
			}
		}
		adminReply.replace("%freeclanhalls%", cList.toString());
		activeChar.sendPacket(adminReply);
	}

	private void showSiegePage(L2PcInstance activeChar, String castleName)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/game/castle.htm");
		adminReply.replace("%castleName%", castleName);
		activeChar.sendPacket(adminReply);
	}

	private void showClanHallPage(L2PcInstance activeChar, ClanHall clanhall)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/game/clanhall.htm");
		adminReply.replace("%clanhallName%", clanhall.getName());
		adminReply.replace("%clanhallId%", String.valueOf(clanhall.getId()));
		L2Clan owner = ClanTable.getInstance().getClan(clanhall.getOwnerId());

		if(owner == null)
		{
			adminReply.replace("%clanhallOwner%", "None");
		}
		else
		{
			adminReply.replace("%clanhallOwner%", owner.getName());
		}

		activeChar.sendPacket(adminReply);
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}