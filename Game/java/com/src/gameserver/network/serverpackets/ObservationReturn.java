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

import com.src.gameserver.model.actor.instance.L2PcInstance;

public class ObservationReturn extends L2GameServerPacket
{
	private static final String _S__E0_OBSERVRETURN = "[S] E0 ObservationReturn";
	private L2PcInstance _activeChar;

	public ObservationReturn(L2PcInstance observer)
	{
		_activeChar = observer;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xe0);
		writeD(_activeChar.getObsX());
		writeD(_activeChar.getObsY());
		writeD(_activeChar.getObsZ());
	}

	@Override
	public String getType()
	{
		return _S__E0_OBSERVRETURN;
	}

}