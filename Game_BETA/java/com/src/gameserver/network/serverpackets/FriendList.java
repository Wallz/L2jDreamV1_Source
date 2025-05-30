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
package com.src.gameserver.network.serverpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class FriendList extends L2GameServerPacket
{
	private final static Log _log = LogFactory.getLog(FriendList.class);

	private static final String _S__FA_FRIENDLIST = "[S] FA FriendList";

	private L2PcInstance _activeChar;

	public FriendList(L2PcInstance character)
	{
		_activeChar = character;
	}

	@Override
	protected final void writeImpl()
	{
		if(_activeChar == null)
		{
			return;
		}

		Connection con = null;

		try
		{
			String sqlQuery = "SELECT friend_id, friend_name FROM character_friends WHERE " + "char_id=" + _activeChar.getObjectId() + " ORDER BY friend_name ASC";

			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(sqlQuery);
			ResultSet rset = statement.executeQuery(sqlQuery);

			rset.last();

			if(rset.getRow() > 0)
			{

				writeC(0xfa);
				writeH(rset.getRow());

				rset.beforeFirst();

				while(rset.next())
				{
					int friendId = rset.getInt("friend_id");
					String friendName = rset.getString("friend_name");

					if(friendId == _activeChar.getObjectId())
					{
						continue;
					}

					L2PcInstance friend = L2World.getInstance().getPlayer(friendName);

					writeH(0); // ??
					writeD(friendId);
					writeS(friendName);

					if(friend == null)
					{
						writeD(0);
					}
					else
					{
						writeD(1);
					}

					writeH(0);
				}
			}

			rset.close();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("Error found in " + _activeChar.getName() + "'s", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	@Override
	public String getType()
	{
		return _S__FA_FRIENDLIST;
	}
}