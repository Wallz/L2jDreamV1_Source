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

import com.src.gameserver.model.L2ShortCut;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class ShortCutInit extends L2GameServerPacket
{
	private static final String _S__57_SHORTCUTINIT = "[S] 45 ShortCutInit";

	private L2ShortCut[] _shortCuts;
	private L2PcInstance _activeChar;

	public ShortCutInit(L2PcInstance activeChar)
	{
		_activeChar = activeChar;

		if(_activeChar == null)
		{
			return;
		}

		_shortCuts = _activeChar.getAllShortCuts();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x45);
		writeD(_shortCuts.length);

		for(L2ShortCut sc : _shortCuts)
		{
			writeD(sc.getType());
			writeD(sc.getSlot() + sc.getPage() * 12);

			switch(sc.getType())
			{
				case L2ShortCut.TYPE_ITEM:
					writeD(sc.getId());
					writeD(0x01);
					writeD(sc.getSharedReuseGroup());
					writeD(0x00);
					writeD(0x00);
					writeH(0x00);
					writeH(0x00);
					break;
				case L2ShortCut.TYPE_SKILL:
					writeD(sc.getId());
					writeD(sc.getLevel());
					writeC(0x00);
					writeD(0x01);
					break;
				case L2ShortCut.TYPE_ACTION:
					writeD(sc.getId());
					writeD(0x01);
					break;
				case L2ShortCut.TYPE_MACRO:
					writeD(sc.getId());
					writeD(0x01);
					break;
				case L2ShortCut.TYPE_RECIPE:
					writeD(sc.getId());
					writeD(0x01);
					break;
				default:
					writeD(sc.getId());
					writeD(0x01);
			}
		}
	}

	@Override
	public String getType()
	{
		return _S__57_SHORTCUTINIT;
	}

}