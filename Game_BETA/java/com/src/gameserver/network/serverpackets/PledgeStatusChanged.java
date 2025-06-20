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

import com.src.gameserver.model.L2Clan;

public class PledgeStatusChanged extends L2GameServerPacket
{
	private static final String _S__CD_PLEDGESTATUS_CHANGED = "[S] CD PledgeStatusChanged";
	private L2Clan _clan;

	public PledgeStatusChanged(L2Clan clan)
	{
		_clan = clan;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xcd);
		writeD(_clan.getLeaderId());
		writeD(_clan.getClanId());
		writeD(0);
		writeD(_clan.getLevel());
		writeD(0);
		writeD(0);
		writeD(0);
	}

	@Override
	public String getType()
	{
		return _S__CD_PLEDGESTATUS_CHANGED;
	}

}