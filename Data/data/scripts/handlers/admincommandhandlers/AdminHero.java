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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.datatables.GmListTable;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.SocialAction;
import com.src.util.database.L2DatabaseFactory;

public class AdminHero implements IAdminCommandHandler
{
	private static String[] ADMIN_COMMANDS =
	{
		"admin_sethero"
	};

	private final static Log _log = LogFactory.getLog(AdminHero.class.getName());

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if(activeChar == null)
			return false;

		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		GMAudit.auditGMAction(activeChar.getName(), command, (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target"), "");

		if(command.startsWith("admin_sethero"))
		{
			String[] cmdParams = command.split(" ");

			long heroTime = 0;
			if(cmdParams.length > 1)
			{
				try
				{
					heroTime = Integer.parseInt(cmdParams[1]) * 24L * 60L * 60L * 1000L;
				}
				catch(NumberFormatException nfe)
				{
					//None
				}
			}
		
			L2Object target = activeChar.getTarget();

			if(target instanceof L2PcInstance)
			{
				L2PcInstance targetPlayer = (L2PcInstance) target;
				boolean newHero = !targetPlayer.isHero();

				if(newHero)
				{
					targetPlayer.setIsHero(true);
					targetPlayer.sendMessage("You are now a hero.");
					updateDatabase(targetPlayer, true, heroTime);
					sendMessages(true, targetPlayer, activeChar, true, true);
					targetPlayer.broadcastPacket(new SocialAction(targetPlayer.getObjectId(), 16));
					targetPlayer.broadcastUserInfo();
				}
				else
				{
					targetPlayer.setIsHero(false);
					targetPlayer.sendMessage("You are no longer a hero.");
					updateDatabase(targetPlayer, false, 0);
					sendMessages(false, targetPlayer, activeChar, true, true);
					targetPlayer.broadcastUserInfo();
				}

				targetPlayer = null;
			}
			else
			{
				activeChar.sendMessage("Impossible to set a non Player Target as hero.");
				_log.info("GM: " + activeChar.getName() + " is trying to set a non Player Target as hero.");

				return false;
			}

			target = null;
		}
		return true;
	}

	private void sendMessages(boolean fornewHero, L2PcInstance player, L2PcInstance gm, boolean announce, boolean notifyGmList)
	{
		if(fornewHero)
		{
			player.sendMessage(gm.getName() + " has granted Hero Status for you!");
			gm.sendMessage("You've granted Hero Status for " + player.getName());

			if(notifyGmList)
			{
				GmListTable.broadcastMessageToGMs("Warn: " + gm.getName() + " has set " + player.getName() + " as Hero !");
			}
		}
		else
		{
			player.sendMessage(gm.getName() + " has revoked Hero Status from you!");
			gm.sendMessage("You've revoked Hero Status from " + player.getName());

			if(notifyGmList)
			{
				GmListTable.broadcastMessageToGMs("Warn: " + gm.getName() + " has removed Hero Status of player" + player.getName());
			}
		}
	}

	/**
	 * @param activeChar
	 * @param newHero
	 */
	private void updateDatabase(L2PcInstance player, boolean newHero, long heroTime)
	{
		Connection con = null;
		try
		{
			if(player == null)
				return;

			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM characters_custom_data WHERE obj_Id=?");
			statement.setInt(1, player.getObjectId());
			ResultSet result = statement.executeQuery();

			if(result.next())
			{
				PreparedStatement stmt = con.prepareStatement(newHero ? UPDATE_DATA : DEL_DATA);
				if(newHero)
				{
					stmt.setLong(1, heroTime == 0 ? 0 : System.currentTimeMillis() + heroTime);
					stmt.setInt(2, player.getObjectId());
					stmt.execute();
				}
				else
				{
					stmt.setInt(1, player.getObjectId());
					stmt.execute();
				}
				stmt.close();
				stmt = null;
			}
			else
			{
				if(newHero)
				{
					PreparedStatement stmt = con.prepareStatement(INSERT_DATA);
					stmt.setInt(1, player.getObjectId());
					stmt.setString(2, player.getName());
					stmt.setInt(3, 1);
					stmt.setInt(4, 1);
					stmt.setLong(5, heroTime == 0 ? 0 : System.currentTimeMillis() + heroTime);
					stmt.execute();
					stmt.close();
					stmt = null;
				}
			}
			result.close();
			statement.close();

			result = null;
			statement = null;
		}
		catch(Exception e)
		{
			_log.error("Error: could not update database: ", e);
		}
		finally
		{
			try { con.close(); } catch(Exception e) { }
			con = null;
		}
	}

	// Updates That Will be Executed by MySQL
	// ----------------------------------------
	String INSERT_DATA= "INSERT INTO characters_custom_data (obj_Id, char_name, noble, hero, hero_end_date) VALUES (?,?,?,?,?)";
	String UPDATE_DATA = "UPDATE characters_custom_data SET noble=1, hero=1, hero_end_date=? WHERE obj_Id=?";
	String DEL_DATA = "UPDATE characters_custom_data SET hero = 0, hero_end_date=0 WHERE obj_Id=?";

	/**
	 * @return
	 */
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

}