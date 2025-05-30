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

import com.src.gameserver.model.L2Party;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class PartySmallWindowAll extends L2GameServerPacket
{
	private static final String _S__63_PARTYSMALLWINDOWALL = "[S] 4e PartySmallWindowAll";
	private L2Party _party;
	private L2PcInstance _exclude;
	private int _dist, _LeaderOID;

	public PartySmallWindowAll(L2PcInstance exclude, L2Party party)
	{
		_exclude = exclude;
		_party = party;
		_LeaderOID = _party.getPartyLeaderOID();
		_dist = _party.getLootDistribution();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x4e);
		writeD(_LeaderOID);
		writeD(_dist);
		writeD(_party.getMemberCount() - 1);

		for (L2PcInstance member : _party.getPartyMembers())
		{
			if ((member != null) && (member != _exclude))
			{
				writeD(member.getObjectId());
				writeS(member.getName());

				writeD((int) member.getCurrentCp()); //c4
				writeD(member.getMaxCp()); //c4

				writeD((int) member.getCurrentHp());
				writeD(member.getMaxHp());
				writeD((int) member.getCurrentMp());
				writeD(member.getMaxMp());
				writeD(member.getLevel());
				writeD(member.getClassId().getId());
				writeD(0);//writeD(0x01); ??
				writeD(member.getRace().ordinal());
			}
		}
	}

	@Override
	public String getType()
	{
		return _S__63_PARTYSMALLWINDOWALL;
	}

}