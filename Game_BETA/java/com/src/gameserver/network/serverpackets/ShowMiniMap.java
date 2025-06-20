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

import com.src.gameserver.model.entity.sevensigns.SevenSigns;

public class ShowMiniMap extends L2GameServerPacket
{
	private static final String _S__B6_SHOWMINIMAP = "[S] 9d ShowMiniMap";
	private int _mapId;

	public ShowMiniMap(int mapId)
	{
		_mapId = mapId;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x9d);
		writeD(_mapId);
		writeD(SevenSigns.getInstance().getCurrentPeriod());
	}

	@Override
	public String getType()
	{
		return _S__B6_SHOWMINIMAP;
	}

}