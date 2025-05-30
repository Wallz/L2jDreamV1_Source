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

import com.src.gameserver.datatables.sql.SpawnTable;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.managers.RaidBossSpawnManager;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class AdminDelete implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_delete"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		if(command.equals("admin_delete"))
		{
			handleDelete(activeChar);
		}

		String target = (activeChar.getTarget() != null ? activeChar.getTarget().getName() : "no-target");
        GMAudit.auditGMAction(activeChar.getName(), command, target, "");
		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void handleDelete(L2PcInstance activeChar)
	{
		L2Object obj = activeChar.getTarget();

		if(obj != null && obj instanceof L2Npc)
		{
			L2Npc target = (L2Npc) obj;
			target.deleteMe();

			L2Spawn spawn = target.getSpawn();
			if(spawn != null)
			{
				spawn.stopRespawn();

				if(RaidBossSpawnManager.getInstance().isDefined(spawn.getNpcid()) && !spawn.is_customBossInstance())
				{
					RaidBossSpawnManager.getInstance().deleteSpawn(spawn, true);
				}
				else
				{
					SpawnTable.getInstance().deleteSpawn(spawn, true);
				}
			}
			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("Deleted " + target.getName() + " from " + target.getObjectId() + "."));
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("Incorrect target."));
		}
	}

}