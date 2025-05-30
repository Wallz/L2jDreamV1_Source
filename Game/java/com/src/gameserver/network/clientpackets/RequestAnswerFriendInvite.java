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
import com.src.gameserver.network.serverpackets.FriendList;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public final class RequestAnswerFriendInvite extends L2GameClientPacket
{
	private final static Log _log = LogFactory.getLog(RequestAnswerFriendInvite.class);

	private static final String _C__5F_REQUESTANSWERFRIENDINVITE = "[C] 5F RequestAnswerFriendInvite";

	private int _response;

	@Override
	protected void readImpl()
	{
		_response = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if(player != null)
		{
			L2PcInstance requestor = player.getActiveRequester();
			if(requestor == null)
			{
				return;
			}

			if(_response == 1)
			{
				Connection con = null;
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement = con.prepareStatement("INSERT INTO character_friends (char_id, friend_id, friend_name) VALUES (?, ?, ?), (?, ?, ?)");
					statement.setInt(1, requestor.getObjectId());
					statement.setInt(2, player.getObjectId());
					statement.setString(3, player.getName());
					statement.setInt(4, player.getObjectId());
					statement.setInt(5, requestor.getObjectId());
					statement.setString(6, requestor.getName());
					statement.execute();
					statement.close();
					requestor.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_SUCCEEDED_INVITING_FRIEND));

					requestor.sendPacket(new SystemMessage(SystemMessageId.S1_ADDED_TO_FRIENDS).addString(player.getName()));

					player.sendPacket(new SystemMessage(SystemMessageId.S1_JOINED_AS_FRIEND).addString(requestor.getName()));;

					notifyFriends(player);
					notifyFriends(requestor);
					player.sendPacket(new FriendList(player));
					requestor.sendPacket(new FriendList(requestor));
				}
				catch(Exception e)
				{
					_log.error("could not add friend objectid", e);
				}
				finally
				{
					ResourceUtil.closeConnection(con); 
				}
			}
			else
			{
				requestor.sendPacket(new SystemMessage(SystemMessageId.FAILED_TO_INVITE_A_FRIEND));
			}

			player.setActiveRequester(null);
			requestor.onTransactionResponse();
		}
	}

	private void notifyFriends(L2PcInstance cha)
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("SELECT friend_name FROM character_friends WHERE char_id = ?");
			statement.setInt(1, cha.getObjectId());
			ResultSet rset = statement.executeQuery();
			L2PcInstance friend;
			String friendName;

			while(rset.next())
			{
				friendName = rset.getString("friend_name");
				friend = L2World.getInstance().getPlayer(friendName);

				if(friend != null)
				{
					friend.sendPacket(new FriendList(friend));
				}
			}

			rset.close();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("could not restore friend data", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	@Override
	public String getType()
	{
		return _C__5F_REQUESTANSWERFRIENDINVITE;
	}
}