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

public class PledgeShowMemberListDelete extends L2GameServerPacket
{
	private static final String _S__6B_PLEDGESHOWMEMBERLISTDELETE = "[S] 56 PledgeShowMemberListDelete";
	private String _player;

	public PledgeShowMemberListDelete(String playerName)
	{
		_player = playerName;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x56);
		writeS(_player);
	}

	@Override
	public String getType()
	{
		return _S__6B_PLEDGESHOWMEMBERLISTDELETE;
	}

}