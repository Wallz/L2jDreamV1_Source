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
package com.src.gameserver.network.clientpackets;

import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.ExListPartyMatchingWaitingRoom;

public class RequestListPartyMatchingWaitingRoom extends L2GameClientPacket
{
	private static int _page;
	private static int _minlvl;
	private static int _maxlvl;
	private static int _mode; // 1 - waitlist 0 - room waitlist

	@Override
	protected void readImpl()
	{
		_page = readD();
		_minlvl = readD();
		_maxlvl = readD();
		_mode	= readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance _activeChar = getClient().getActiveChar();

		if(_activeChar == null)
		{
			return;
		}

		_activeChar.sendPacket(new ExListPartyMatchingWaitingRoom(_activeChar,_page,_minlvl,_maxlvl, _mode));
	}

	@Override
	public String getType()
	{
		return "[C] D0:16 RequestListPartyMatchingWaitingRoom";
	}

}