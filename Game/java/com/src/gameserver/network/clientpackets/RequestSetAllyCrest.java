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
import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.idfactory.IdFactory;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public final class RequestSetAllyCrest extends L2GameClientPacket
{
	private final static Log _log = LogFactory.getLog(RequestSetAllyCrest.class);

	private static final String _C__87_REQUESTSETALLYCREST = "[C] 87 RequestSetAllyCrest";

	private int _length;
	private byte[] _data;

	@Override
	protected void readImpl()
	{
		_length = readD();
		if(_length < 0 || _length > 192)
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

		if(_length < 0)
		{
			activeChar.sendMessage("File transfer error.");
			return;
		}

		if(_length > 192)
		{
			activeChar.sendMessage("The crest file size was too big (max 192 bytes).");
			return;
		}

		if(activeChar.getAllyId() != 0)
		{
			L2Clan leaderclan = ClanTable.getInstance().getClan(activeChar.getAllyId());

			if(activeChar.getClanId() != leaderclan.getClanId() || !activeChar.isClanLeader())
				return;

			CrestCache crestCache = CrestCache.getInstance();

			int newId = IdFactory.getInstance().getNextId();

			if(!crestCache.saveAllyCrest(newId, _data))
			{
				_log.warn("Error loading crest of ally:" + leaderclan.getAllyName());
				return;
			}

			if(leaderclan.getAllyCrestId() != 0)
			{
				crestCache.removeAllyCrest(leaderclan.getAllyCrestId());
			}

			Connection con = null;

			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET ally_crest_id = ? WHERE ally_id = ?");
				statement.setInt(1, newId);
				statement.setInt(2, leaderclan.getAllyId());
				statement.executeUpdate();
				ResourceUtil.closeStatement(statement);
			}
			catch(SQLException e)
			{
				_log.error("could not update the ally crest id", e);
			}
			finally
			{
				ResourceUtil.closeConnection(con); 
			}

			for(L2Clan clan : ClanTable.getInstance().getClans())
			{
				if(clan.getAllyId() == activeChar.getAllyId())
				{
					clan.setAllyCrestId(newId);
					for(L2PcInstance member : clan.getOnlineMembers(""))
					{
						member.broadcastUserInfo();
					}
				}
			}
		}
	}

	@Override
	public String getType()
	{
		return _C__87_REQUESTSETALLYCREST;
	}
}