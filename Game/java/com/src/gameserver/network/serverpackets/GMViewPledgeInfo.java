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
import com.src.gameserver.model.L2ClanMember;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class GMViewPledgeInfo extends L2GameServerPacket
{
	private static final String _S__A9_GMVIEWPLEDGEINFO = "[S] 90 GMViewPledgeInfo";
	private L2Clan _clan;
	private L2PcInstance _activeChar;

	public GMViewPledgeInfo(L2Clan clan, L2PcInstance activeChar)
	{
		_clan = clan;
		_activeChar = activeChar;
	}

	@Override
	protected final void writeImpl()
	{
		int TOP = ClanTable.getInstance().getTopRate(_clan.getClanId());
		writeC(0x90);
		writeS(_activeChar.getName());
		writeD(_clan.getClanId());
		writeD(0x00);
		writeS(_clan.getName());
		writeS(_clan.getLeaderName());
		writeD(_clan.getCrestId());
		writeD(_clan.getLevel());
		writeD(_clan.getHasCastle());
		writeD(_clan.getHasHideout());
		writeD(TOP);
		writeD(_clan.getReputationScore());
		writeD(0);
		writeD(0);

		writeD(_clan.getAllyId());
		writeS(_clan.getAllyName());
		writeD(_clan.getAllyCrestId());
		writeD(_clan.isAtWar());

		L2ClanMember[] members = _clan.getMembers();
		writeD(members.length);

		for(L2ClanMember member : members)
		{
			writeS(member.getName());
			writeD(member.getLevel());
			writeD(member.getClassId());
			writeD(0);
			writeD(1);
			writeD(member.isOnline() ? member.getObjectId() : 0);
			writeD(0);
		}
	}

	@Override
	public String getType()
	{
		return _S__A9_GMVIEWPLEDGEINFO;
	}

}