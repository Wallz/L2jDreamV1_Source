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

import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.L2SiegeClan;
import com.src.gameserver.model.entity.siege.Castle;

public class SiegeDefenderList extends L2GameServerPacket
{
	private static final String _S__CA_SiegeDefenderList = "[S] cb SiegeDefenderList";
	private Castle _castle;

	public SiegeDefenderList(Castle castle)
	{
		_castle = castle;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xcb);
		writeD(_castle.getCastleId());
		writeD(0x00);
		writeD(0x01);
		writeD(0x00);
		int size = _castle.getSiege().getDefenderClans().size() + _castle.getSiege().getDefenderWaitingClans().size();
		if(size > 0)
		{
			L2Clan clan;

			writeD(size);
			writeD(size);
			for(L2SiegeClan siegeclan : _castle.getSiege().getDefenderClans())
			{
				clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
				if(clan == null)
				{
					continue;
				}

				writeD(clan.getClanId());
				writeS(clan.getName());
				writeS(clan.getLeaderName());
				writeD(clan.getCrestId());
				writeD(0x00);
				switch(siegeclan.getType())
				{
					case OWNER:
						writeD(0x01);
						break;
					case DEFENDER_PENDING:
						writeD(0x02);
						break;
					case DEFENDER:
						writeD(0x03);
						break;
					default:
						writeD(0x00);
						break;
				}
				writeD(clan.getAllyId());
				writeS(clan.getAllyName());
				writeS("");
				writeD(clan.getAllyCrestId());
			}
			for(L2SiegeClan siegeclan : _castle.getSiege().getDefenderWaitingClans())
			{
				clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
				writeD(clan.getClanId());
				writeS(clan.getName());
				writeS(clan.getLeaderName());
				writeD(clan.getCrestId());
				writeD(0x00);
				writeD(0x02);
				writeD(clan.getAllyId());
				writeS(clan.getAllyName());
				writeS("");
				writeD(clan.getAllyCrestId());
			}
		}
		else
		{
			writeD(0x00);
			writeD(0x00);
		}
	}

	@Override
	public String getType()
	{
		return _S__CA_SiegeDefenderList;
	}

}