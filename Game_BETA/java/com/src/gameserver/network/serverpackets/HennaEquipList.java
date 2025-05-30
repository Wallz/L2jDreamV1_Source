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

import com.src.gameserver.model.actor.instance.L2HennaInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class HennaEquipList extends L2GameServerPacket
{
	private static final String _S__E2_HennaEquipList = "[S] E2 HennaEquipList";

	private L2PcInstance _player;
	private L2HennaInstance[] _hennaEquipList;

	public HennaEquipList(L2PcInstance player, L2HennaInstance[] hennaEquipList)
	{
		_player = player;
		_hennaEquipList = hennaEquipList;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xe2);
		writeD(_player.getAdena());
		writeD(3);
		writeD(_hennaEquipList.length);

		for(L2HennaInstance element : _hennaEquipList)
		{
			if(_player.getInventory().getItemByItemId(element.getItemIdDye()) != null)
			{
				writeD(element.getSymbolId());
				writeD(element.getItemIdDye());
				writeD(element.getAmountDyeRequire());
				writeD(element.getPrice());
				writeD(1);
			}
		}
	}

	@Override
	public String getType()
	{
		return _S__E2_HennaEquipList;
	}

}