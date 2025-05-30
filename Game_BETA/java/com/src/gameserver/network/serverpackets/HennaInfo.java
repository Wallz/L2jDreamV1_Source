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

public final class HennaInfo extends L2GameServerPacket
{
	private static final String _S__E4_HennaInfo = "[S] E4 HennaInfo";

	private final L2PcInstance _activeChar;
    private final L2HennaInstance[] _hennas = new L2HennaInstance[3];
    private int _count;

    public HennaInfo(L2PcInstance player)
    {
        _activeChar = player;

        int j = 0;
        for(int i = 0; i < 3; i++)
        {
            L2HennaInstance h = _activeChar.getHennas(i + 1);
            if(h != null)
            {
                _hennas[j++] = h;
            }
        }
        _count = j;
    }

	@Override
	protected final void writeImpl()
	{

		writeC(0xe4);

		writeC(_activeChar.getHennaStatINT());
		writeC(_activeChar.getHennaStatSTR());
		writeC(_activeChar.getHennaStatCON());
		writeC(_activeChar.getHennaStatMEN());
		writeC(_activeChar.getHennaStatDEX());
		writeC(_activeChar.getHennaStatWIT());

		writeD(3);

		writeD(_count); // size
		for (int i = 0; i < _count; i++)
		{
			writeD(_hennas[i].getSymbolId());
			writeD(_hennas[i].getSymbolId());
		}
	}

	@Override
	public String getType()
	{
		return _S__E4_HennaInfo;
	}

}