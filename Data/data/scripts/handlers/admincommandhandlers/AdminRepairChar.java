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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.util.database.L2DatabaseFactory;

public class AdminRepairChar implements IAdminCommandHandler
{
	private static Logger _log = Logger.getLogger(AdminRepairChar.class.getName());

	private static final String[] ADMIN_COMMANDS =
	{
		"admin_restore",
		"admin_repair"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		String target = (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target");
        GMAudit.auditGMAction(activeChar.getName(), command, target, "");
		handleRepair(command);
		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void handleRepair(String command)
	{
		String[] parts = command.split(" ");

		if(parts.length != 2)
		{
			return;
		}

		String cmd = "UPDATE characters SET x = -84318, y = 244579, z = -3730 WHERE char_name = ?";
		Connection connection = null;

		try
		{
			connection = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = connection.prepareStatement(cmd);
			statement.setString(1, parts[1]);
			statement.execute();
			statement.close();
			statement = null;

			statement = connection.prepareStatement("SELECT obj_id FROM characters where char_name = ?");
			statement.setString(1, parts[1]);
			ResultSet rset = statement.executeQuery();

			int objId = 0;

			if(rset.next())
			{
				objId = rset.getInt(1);
			}

			rset.close();
			statement.close();
			rset = null;
			statement = null;

			if(objId == 0)
			{
				connection.close();
				return;
			}

			statement = connection.prepareStatement("DELETE FROM character_shortcuts WHERE char_obj_id = ?");
			statement.setInt(1, objId);
			statement.execute();
			statement.close();
			statement = null;

			statement = connection.prepareStatement("UPDATE items SET loc = \"INVENTORY\" WHERE owner_id = ?");
			statement.setInt(1, objId);
			statement.execute();
			statement.close();
		}
		catch(Exception e)
		{
			_log.log(Level.WARNING, "Could not repair char:", e);
		}
		finally
		{
			try
			{
				connection.close();
			}
			catch(Exception e)
			{
				
			}
		}
	}
}