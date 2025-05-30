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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.src.gameserver.network.serverpackets.CharDeleteFail;
import com.src.gameserver.network.serverpackets.CharDeleteOk;
import com.src.gameserver.network.serverpackets.CharSelectInfo;

public final class CharacterDelete extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(CharacterDelete.class.getName());

	private static final String _C__0C_CHARACTERDELETE = "[C] 0C CharacterDelete";

	private int _charSlot;

	@Override
	protected void readImpl()
	{
		_charSlot = readD();
	}

	@Override
	protected void runImpl()
	{
		try
		{
			byte answer = getClient().markToDeleteChar(_charSlot);

			switch(answer)
			{
				default:
				case -1:
					break;
				case 0:
					sendPacket(new CharDeleteOk());
					break;
				case 1:
					sendPacket(new CharDeleteFail(CharDeleteFail.REASON_YOU_MAY_NOT_DELETE_CLAN_MEMBER));
					break;
				case 2:
					sendPacket(new CharDeleteFail(CharDeleteFail.REASON_CLAN_LEADERS_MAY_NOT_BE_DELETED));
					break;
			}
		}
		catch(Exception e)
		{
			_log.log(Level.SEVERE, "Error:", e);

		}
		
		CharSelectInfo cl = new CharSelectInfo(getClient().getAccountName(), getClient().getSessionId().playOkID1, 0);
		sendPacket(cl);
		getClient().setCharSelection(cl.getCharInfo());
	}

	@Override
	public String getType()
	{
		return _C__0C_CHARACTERDELETE;
	}

}