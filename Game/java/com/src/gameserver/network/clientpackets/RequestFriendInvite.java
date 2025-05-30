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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.AskJoinFriend;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.util.Util;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public final class RequestFriendInvite extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestFriendInvite.class.getName());

	private static final String _C__5E_REQUESTFRIENDINVITE = "[C] 5E RequestFriendInvite";

	private String _name;

	@Override
	protected void readImpl()
	{
		_name = readS();
	}

	@Override
	protected void runImpl()
	{
		Connection con = null;
		L2PcInstance activeChar = getClient().getActiveChar();

		if(activeChar == null)
		{
			return;
		}

		L2PcInstance friend = L2World.getInstance().getPlayer(_name);
		_name = Util.capitalizeFirst(_name);

		if(friend == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THE_USER_YOU_REQUESTED_IS_NOT_IN_GAME));
			return;
		}
		else if(friend == activeChar)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_ADD_YOURSELF_TO_OWN_FRIEND_LIST));
			return;
		}
		if(activeChar.isInCombat() || friend.isInCombat())
        {
			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER));
            return;
        }
		else if(friend.isInOlympiadMode())
		{
			activeChar.sendMessage("Your friend is in the Olympiad now.");
			return;
		}

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT char_id FROM character_friends WHERE char_id = ? AND friend_id = ?");
			statement.setInt(1, activeChar.getObjectId());
			statement.setInt(2, friend.getObjectId());
			ResultSet rset = statement.executeQuery();

			if(rset.next())
			{
				friend.sendPacket(new SystemMessage(SystemMessageId.S1_ALREADY_IN_FRIENDS_LIST).addString(_name));
			}
			else
			{
				if(!friend.isProcessingRequest())
				{
					activeChar.onTransactionRequest(friend);
					friend.sendPacket(new SystemMessage(SystemMessageId.S1_REQUESTED_TO_BECOME_FRIENDS).addString(_name));
					friend.sendPacket(new AskJoinFriend(activeChar.getName()));
				}
				else
				{
					friend.sendPacket(new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER));
				}
			}

			rset.close();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.log(Level.WARNING, "could not add friend objectid: ", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	@Override
	public String getType()
	{
		return _C__5E_REQUESTFRIENDINVITE;
	}
}