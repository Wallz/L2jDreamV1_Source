/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.src.gameserver.network.serverpackets;

public class ExClosePartyRoom extends L2GameServerPacket
{
	public ExClosePartyRoom()
	{}

	@Override
	public String getType()
	{
		return "[S] FE:OF ExClosePartyRoom";
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x0F);
	}
}