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

import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;

public final class RequestReplyStartPledgeWar extends L2GameClientPacket
{
	private static final String _C__4e_REQUESTREPLYSTARTPLEDGEWAR = "[C] 4e RequestReplyStartPledgeWar";

	private int _answer;

	@Override
	protected void readImpl()
	{
		@SuppressWarnings("unused")
		String _reqName = readS();
		_answer = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		L2PcInstance requestor = activeChar.getActiveRequester();
		if(requestor == null)
			return;

		if(_answer == 1)
		{
			ClanTable.getInstance().storeclanswars(requestor.getClanId(), activeChar.getClanId());
		}
		else
		{
			requestor.sendPacket(new SystemMessage(SystemMessageId.WAR_PROCLAMATION_HAS_BEEN_REFUSED));
		}

		activeChar.setActiveRequester(null);
		requestor.onTransactionResponse();
	}

	@Override
	public String getType()
	{
		return _C__4e_REQUESTREPLYSTARTPLEDGEWAR;
	}
}