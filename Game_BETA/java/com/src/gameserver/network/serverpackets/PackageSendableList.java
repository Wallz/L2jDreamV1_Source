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

public class PackageSendableList extends L2GameServerPacket
{
	private static final String _S__C3_PACKAGESENDABLELIST = "[S] C3 PackageSendableList";
	private L2ItemInstance[] _items;
	private int _playerObjId;

	public PackageSendableList(L2ItemInstance[] items, int playerObjId)
	{
		_items = items;
		_playerObjId = playerObjId;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xC3);

		writeD(_playerObjId);
		writeD(getClient().getActiveChar().getAdena());
		writeD(_items.length);
		for(L2ItemInstance item : _items)
		{
			writeH(item.getItem().getType1());
			writeD(item.getObjectId());
			writeD(item.getItemId());
			writeD(item.getCount());
			writeH(item.getItem().getType2());
			writeH(0x00);
			writeD(item.getItem().getBodyPart());
			writeH(item.getEnchantLevel());
			writeH(0x00);
			writeH(0x00);
			writeD(item.getObjectId());
		}
	}

	@Override
	public String getType()
	{
		return _S__C3_PACKAGESENDABLELIST;
	}

}