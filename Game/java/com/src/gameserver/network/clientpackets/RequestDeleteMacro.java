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

import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;

public final class RequestDeleteMacro extends L2GameClientPacket
{
	private static final String _C__C2_REQUESTDELETEMACRO = "[C] C2 RequestDeleteMacro";

	private int _id;

	@Override
	protected void readImpl()
	{
		_id = readD();
	}

	@Override
	protected void runImpl()
	{
		if(getClient().getActiveChar() == null)
		{
			return;
		}

		getClient().getActiveChar().deleteMacro(_id);

		sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("Delete macro id=" + _id));
	}

	@Override
	public String getType()
	{
		return _C__C2_REQUESTDELETEMACRO;
	}

}