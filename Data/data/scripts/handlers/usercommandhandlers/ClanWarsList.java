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
package handlers.usercommandhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.src.gameserver.handler.IUserCommandHandler;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.util.database.L2DatabaseFactory;

public class ClanWarsList implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		88, 89, 90
	};

	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		if(id != COMMAND_IDS[0] && id != COMMAND_IDS[1] && id != COMMAND_IDS[2])
		{
			return false;
		}

		L2Clan clan = activeChar.getClan();

		if(clan == null)
		{
			activeChar.sendMessage("You are not in a clan.");
			return false;
		}
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;

			if(id == 88)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CLANS_YOU_DECLARED_WAR_ON));
				statement = con.prepareStatement("SELECT clan_name, clan_id, ally_id, ally_name FROM clan_data, clan_wars WHERE clan1 = ? AND clan_id = clan2 AND clan2 NOT IN (SELECT clan1 FROM clan_wars WHERE clan2 = ?)");
				statement.setInt(1, clan.getClanId());
				statement.setInt(2, clan.getClanId());
			}
			else if(id == 89)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CLANS_THAT_HAVE_DECLARED_WAR_ON_YOU));
				statement = con.prepareStatement("SELECT clan_name, clan_id, ally_id, ally_name FROM clan_data, clan_wars WHERE clan2 = ? AND clan_id = clan1 AND clan1 NOT IN (SELECT clan2 FROM clan_wars WHERE clan1 = ?)");
				statement.setInt(1, clan.getClanId());
				statement.setInt(2, clan.getClanId());
			}
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.WAR_LIST));
				statement = con.prepareStatement("SELECT clan_name, clan_id, ally_id, ally_name FROM clan_data, clan_wars WHERE clan1 = ? AND clan_id = clan2 AND clan2 IN (SELECT clan1 FROM clan_wars WHERE clan2 = ?)");
				statement.setInt(1, clan.getClanId());
				statement.setInt(2, clan.getClanId());
			}

			ResultSet rset = statement.executeQuery();
			while(rset.next())
			{
				String clanName = rset.getString("clan_name");
				int ally_id = rset.getInt("ally_id");

				if(ally_id > 0)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_S2_ALLIANCE).addString(clanName).addString(rset.getString("ally_name")));
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_NO_ALLI_EXISTS).addString(clanName));
				}
			}

			activeChar.sendPacket(new SystemMessage(SystemMessageId.FRIEND_LIST_FOOTER));

			rset.close();
			statement.close();
		}
		catch(Exception e)
		{
			
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(Exception e)
			{
				
			}
		}
		return true;
	}

	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}