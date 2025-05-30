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

import com.src.gameserver.model.actor.instance.L2PcInstance;

public class PartySmallWindowAdd extends L2GameServerPacket
{
	private static final String _S__64_PARTYSMALLWINDOWADD = "[S] 4f PartySmallWindowAdd";

	private L2PcInstance _member;

	public PartySmallWindowAdd(L2PcInstance member)
	{
		_member = member;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x4f);
		L2PcInstance player = getClient().getActiveChar();
		writeD(player.getObjectId());
		writeD(0);
		writeD(_member.getObjectId());
		writeS(_member.getName());

		writeD((int) _member.getCurrentCp());
		writeD(_member.getMaxCp());

		writeD((int) _member.getCurrentHp());
		writeD(_member.getMaxHp());
		writeD((int) _member.getCurrentMp());
		writeD(_member.getMaxMp());
		writeD(_member.getLevel());
		writeD(_member.getClassId().getId());
		writeD(0);
		writeD(0);
	}

	@Override
	public String getType()
	{
		return _S__64_PARTYSMALLWINDOWADD;
	}

}