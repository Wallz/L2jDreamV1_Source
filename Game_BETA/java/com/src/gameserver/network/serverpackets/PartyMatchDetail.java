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

import com.src.gameserver.model.PartyMatchRoom;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class PartyMatchDetail extends L2GameServerPacket
{
	private static final String _S__97_PARTYMATCHDETAIL = "[S] 97 PartyMatchDetail";

	private PartyMatchRoom _room;

	public PartyMatchDetail(L2PcInstance player, PartyMatchRoom room)
	{
		_room = room;
	}

	@Override
	protected final void writeImpl()
	{
		if(getClient().getActiveChar() == null)
			return;

		writeC(0x97);
		writeD(_room.getId());			//	Room ID
		writeD(_room.getMaxMembers());	//	Max Members
		writeD(_room.getMinLvl());		//	Level Min
		writeD(_room.getMaxLvl());		//	Level Max
		writeD(_room.getLootType());	//	Loot Type
		writeD(_room.getLocation());	//	Room Location
		writeS(_room.getTitle());		//	Room title
	}

	@Override
	public String getType()
	{
		return _S__97_PARTYMATCHDETAIL;
	}

}
