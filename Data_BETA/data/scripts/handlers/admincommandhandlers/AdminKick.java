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

import com.src.gameserver.communitybbs.Manager.RegionBBSManager;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.LeaveWorld;

public class AdminKick implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_kick",
		"admin_kick_non_gm"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		String target = (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target");
        GMAudit.auditGMAction(activeChar.getName(), command, target, "");

		if(command.startsWith("admin_kick"))
		{
			StringTokenizer st = new StringTokenizer(command);

			if(activeChar.getTarget() != null)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Type: //kick <name>");
			}

			if(st.countTokens() > 1)
			{
				st.nextToken();

				String player = st.nextToken();
				L2PcInstance plyr = L2World.getInstance().getPlayer(player);

				if(plyr != null)
				{
					plyr.logout();
					activeChar.sendChatMessage(0, 0, "SYS", "You kicked " + plyr.getName() + " from the game.");
					RegionBBSManager.getInstance().changeCommunityBoard();
				}

				if(plyr != null && plyr.isOffline())
				{
					plyr.deleteMe();
					activeChar.sendChatMessage(0, 0, "SYS", "You kicked Offline Player " + plyr.getName() + " from the game.");
					RegionBBSManager.getInstance().changeCommunityBoard();
				}
			}
		}

		if(command.startsWith("admin_kick_non_gm"))
		{
			int counter = 0;

			for(L2PcInstance player : L2World.getInstance().getAllPlayers())
			{
				if(!player.isGM())
				{
					counter++;
					player.sendPacket(new LeaveWorld());
					player.logout();
					RegionBBSManager.getInstance().changeCommunityBoard();
				}
			}

			activeChar.sendChatMessage(0, 0, "SYS", "Kicked " + counter + " players");
		}
		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}