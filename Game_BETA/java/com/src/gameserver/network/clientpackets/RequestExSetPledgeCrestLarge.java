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

public final class RequestExSetPledgeCrestLarge extends L2GameClientPacket
{
	private final static Log _log = LogFactory.getLog(RequestExSetPledgeCrestLarge.class);

	private static final String _C__D0_11_REQUESTEXSETPLEDGECRESTLARGE = "[C] D0:11 RequestExSetPledgeCrestLarge";

	private int _size;
	private byte[] _data;

	@Override
	protected void readImpl()
	{
		_size = readD();

		if(_size > 2176)
		{
			return;
		}

		if(_size > 0)
		{
			_data = new byte[_size];
			readB(_data);
		}
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if(activeChar == null)
		{
			return;
		}

		L2Clan clan = activeChar.getClan();

		if(clan == null)
		{
			return;
		}

		if(_data == null)
		{
			CrestCache.getInstance().removePledgeCrestLarge(clan.getCrestId());

			clan.setHasCrestLarge(false);
			activeChar.sendMessage("The insignia has been removed.");

			for(L2PcInstance member : clan.getOnlineMembers(""))
			{
				member.broadcastUserInfo();
			}

			return;
		}

		if(_size > 2176)
		{
			activeChar.sendMessage("The insignia file size is greater than 2176 bytes.");
			return;
		}

		if((activeChar.getClanPrivileges() & L2Clan.CP_CL_REGISTER_CREST) == L2Clan.CP_CL_REGISTER_CREST)
		{
			if(clan.getHasCastle() == 0 && clan.getHasHideout() == 0)
			{
				activeChar.sendMessage("Only a clan that owns a clan hall or a castle can get their emblem displayed on clan related items");
				return;
			}

			CrestCache crestCache = CrestCache.getInstance();

			int newId = IdFactory.getInstance().getNextId();

			if(!crestCache.savePledgeCrestLarge(newId, _data))
			{
				_log.warn("Error loading large crest of clan:" + clan.getName());
				return;
			}

			if(clan.hasCrestLarge())
			{
				crestCache.removePledgeCrestLarge(clan.getCrestLargeId());
			}

			Connection con = null;

			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET crest_large_id = ? WHERE clan_id = ?");
				statement.setInt(1, newId);
				statement.setInt(2, clan.getClanId());
				statement.executeUpdate();
				ResourceUtil.closeStatement(statement);
			}
			catch(SQLException e)
			{
				_log.error("could not update the large crest id", e);
			}
			finally
			{
				ResourceUtil.closeConnection(con); 
			}

			clan.setCrestLargeId(newId);
			clan.setHasCrestLarge(true);

			activeChar.sendPacket(new SystemMessage(SystemMessageId.CLAN_EMBLEM_WAS_SUCCESSFULLY_REGISTERED));

			for(L2PcInstance member : clan.getOnlineMembers(""))
			{
				member.broadcastUserInfo();
			}

		}
	}

	@Override
	public String getType()
	{
		return _C__D0_11_REQUESTEXSETPLEDGECRESTLARGE;
	}
}