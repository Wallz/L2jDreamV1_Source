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
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.cache.CrestCache;
import com.src.gameserver.idfactory.IdFactory;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public final class RequestSetPledgeCrest extends L2GameClientPacket
{
	private final static Log _log = LogFactory.getLog(RequestSetPledgeCrest.class);

	private static final String _C__53_REQUESTSETPLEDGECREST = "[C] 53 RequestSetPledgeCrest";

	private int _length;
	private byte[] _data;

	@Override
	protected void readImpl()
	{
		_length = readD();
		if(_length < 0 || _length > 256)
			return;

		_data = new byte[_length];
		readB(_data);
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if(activeChar == null)
			return;

		L2Clan clan = activeChar.getClan();
		if(clan == null)
			return;

		if(clan.getDissolvingExpiryTime() > System.currentTimeMillis())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_SET_CREST_WHILE_DISSOLUTION_IN_PROGRESS));
			return;
		}

		if(_length < 0)
		{
			activeChar.sendMessage("File transfer error.");
			return;
		}

		if(_length > 256)
		{
			activeChar.sendMessage("The clan crest file size was too big (max 256 bytes).");
			return;
		}

		if(_length == 0 || _data.length == 0)
		{
			CrestCache.getInstance().removePledgeCrest(clan.getCrestId());

			clan.setHasCrest(false);
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CLAN_CREST_HAS_BEEN_DELETED));

			for(L2PcInstance member : clan.getOnlineMembers(""))
			{
				member.broadcastUserInfo();
			}

			return;
		}

		if((activeChar.getClanPrivileges() & L2Clan.CP_CL_REGISTER_CREST) == L2Clan.CP_CL_REGISTER_CREST)
		{
			if(clan.getLevel() < 3)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CLAN_LVL_3_NEEDED_TO_SET_CREST));
				return;
			}

			CrestCache crestCache = CrestCache.getInstance();

			int newId = IdFactory.getInstance().getNextId();

			if(clan.hasCrest())
			{
				crestCache.removePledgeCrest(newId);
			}

			if(!crestCache.savePledgeCrest(newId, _data))
			{
				_log.warn("Error loading crest of clan:" + clan.getName());
				return;
			}

			Connection con = null;

			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET crest_id = ? WHERE clan_id = ?");
				statement.setInt(1, newId);
				statement.setInt(2, clan.getClanId());
				statement.executeUpdate();
				ResourceUtil.closeStatement(statement);
			}
			catch(SQLException e)
			{
				_log.error("could not update the crest id", e);
			}
			finally
			{
				ResourceUtil.closeConnection(con); 
			}

			clan.setCrestId(newId);
			clan.setHasCrest(true);

			for(L2PcInstance member : clan.getOnlineMembers(""))
			{
				member.broadcastUserInfo();
			}
		}
	}

	@Override
	public String getType()
	{
		return _C__53_REQUESTSETPLEDGECREST;
	}
}