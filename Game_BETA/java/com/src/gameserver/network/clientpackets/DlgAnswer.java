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

import com.src.Config;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;

public final class DlgAnswer extends L2GameClientPacket
{
	private static final String _C__C5_DLGANSWER = "[C] C5 DlgAnswer";

	private int _messageId, _answer, _requestId;

	@Override
	protected void readImpl()
	{
		_messageId = readD();
		_answer = readD();
		_requestId = readD();
	}

	@Override
	public void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if(activeChar == null)
		{
			return;
		}

		if(_messageId == SystemMessageId.RESSURECTION_REQUEST.getId())
		{
			activeChar.reviveAnswer(_answer);
		}
		else if (_messageId==SystemMessageId.S1_WISHES_TO_SUMMON_YOU_FROM_S2_DO_YOU_ACCEPT.getId())
		{
			activeChar.teleportAnswer(_answer, _requestId);
		}
		else if(_messageId == 614 && Config.ALLOW_WEDDING)
		{
			activeChar.EngageAnswer(_answer);
		}
		else if(_messageId == SystemMessageId.S1.getId())
		{
			if(activeChar.dialog != null)
			{
				activeChar.dialog.onDlgAnswer(activeChar);
				activeChar.dialog = null;
			}
		}
		else if(_messageId == SystemMessageId.WOULD_YOU_LIKE_TO_OPEN_THE_GATE.getId())
		{
			activeChar.gatesAnswer(_answer, 1);
		}
		else if(_messageId == SystemMessageId.WOULD_YOU_LIKE_TO_CLOSE_THE_GATE.getId())
		{
			activeChar.gatesAnswer(_answer, 0);
		}
	}

	@Override
	public String getType()
	{
		return _C__C5_DLGANSWER;
	}

}