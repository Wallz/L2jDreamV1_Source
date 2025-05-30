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

public class DropItem extends L2GameServerPacket
{
	private static final String _S__16_DROPITEM = "[S] 0c DropItem";

	private L2ItemInstance _item;
	private int _charObjId;

	public DropItem(L2ItemInstance item, int playerObjId)
	{
		_item = item;
		_charObjId = playerObjId;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x0c);
		writeD(_charObjId);
		writeD(_item.getObjectId());
		writeD(_item.getItemId());

		writeD(_item.getX());
		writeD(_item.getY());
		writeD(_item.getZ());

		if(_item.isStackable())
		{
			writeD(0x01);
		}
		else
		{
			writeD(0x00);
		}
		writeD(_item.getCount());

		writeD(1);
	}

	@Override
	public String getType()
	{
		return _S__16_DROPITEM;
	}

}