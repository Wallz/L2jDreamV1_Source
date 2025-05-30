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

import com.src.gameserver.model.actor.instance.L2ItemInstance;

public class GetItem extends L2GameServerPacket
{
	private static final String _S__17_GETITEM = "[S] 0d GetItem";
	private L2ItemInstance _item;
	private int _playerId;

	public GetItem(L2ItemInstance item, int playerId)
	{
		_item = item;
		_playerId = playerId;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x0d);
		writeD(_playerId);
		writeD(_item.getObjectId());

		writeD(_item.getX());
		writeD(_item.getY());
		writeD(_item.getZ());
	}

	@Override
	public String getType()
	{
		return _S__17_GETITEM;
	}

}