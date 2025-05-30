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

import java.util.Calendar;
import java.util.logging.Logger;

import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.siege.Castle;

public class SiegeInfo extends L2GameServerPacket
{
	private static final String _S__C9_SIEGEINFO = "[S] c9 SiegeInfo";
	private static Logger _log = Logger.getLogger(SiegeInfo.class.getName());
	private Castle _castle;

	public SiegeInfo(Castle castle)
	{
		_castle = castle;
	}

	@Override
	protected final void writeImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if(activeChar == null)
		{
			return;
		}

		writeC(0xc9);
		writeD(_castle.getCastleId());
		writeD(_castle.getOwnerId() == activeChar.getClanId() && activeChar.isClanLeader() ? 0x01 : 0x00);
		writeD(_castle.getOwnerId());
		if(_castle.getOwnerId() > 0)
		{
			L2Clan owner = ClanTable.getInstance().getClan(_castle.getOwnerId());
			if(owner != null)
			{
				writeS(owner.getName());
				writeS(owner.getLeaderName());
				writeD(owner.getAllyId());
				writeS(owner.getAllyName());
			}
			else
			{
				_log.warning("Null owner for castle: " + _castle.getName());
			}
		}
		else
		{
			writeS("NPC");
			writeS("");
			writeD(0);
			writeS("");
		}

		writeD((int) (Calendar.getInstance().getTimeInMillis() / 1000));
		writeD((int) (_castle.getSiege().getSiegeDate().getTimeInMillis() / 1000));
		writeD(0x00);
	}

	@Override
	public String getType()
	{
		return _S__C9_SIEGEINFO;
	}

}