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
package com.src.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public final class RequestFriendList extends L2GameClientPacket
{
	private final static Log _log = LogFactory.getLog(RequestFriendList.class);

	private static final String _C__60_REQUESTFRIENDLIST = "[C] 60 RequestFriendList";

	@Override
	protected void readImpl()
	{
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if(activeChar == null)
		{
			return;
		}

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT friend_id, friend_name FROM character_friends WHERE char_id = ?");
			statement.setInt(1, activeChar.getObjectId());

			ResultSet rset = statement.executeQuery();

			activeChar.sendPacket(new SystemMessage(SystemMessageId.FRIEND_LIST_HEADER));

			L2PcInstance friend = null;
			while(rset.next())
			{
				String friendName = rset.getString("friend_name");
				friend = L2World.getInstance().getPlayer(friendName);

				if(friend == null)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_OFFLINE).addString(friendName));
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_ONLINE).addString(friendName));
				}
			}

			activeChar.sendPacket(new SystemMessage(SystemMessageId.FRIEND_LIST_FOOTER));

			rset.close();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("Error in friendlist for " + activeChar, e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	@Override
	public String getType()
	{
		return _C__60_REQUESTFRIENDLIST;
	}
}