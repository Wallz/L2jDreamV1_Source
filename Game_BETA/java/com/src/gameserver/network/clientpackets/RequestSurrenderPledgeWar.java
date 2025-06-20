/* This program is free software; you can redistribute it and/or modify
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

import java.util.logging.Logger;

import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.SystemMessage;

public final class RequestSurrenderPledgeWar extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestSurrenderPledgeWar.class.getName());

	private static final String _C__51_REQUESTSURRENDERPLEDGEWAR = "[C] 51 RequestSurrenderPledgeWar";

	private String _pledgeName;
	private L2Clan _clan;
	private L2PcInstance _activeChar;

	@Override
	protected void readImpl()
	{
		_pledgeName = readS();
	}

	@Override
	protected void runImpl()
	{
		_activeChar = getClient().getActiveChar();
		if(_activeChar == null)
			return;

		_clan = _activeChar.getClan();
		if(_clan == null)
			return;

		L2Clan clan = ClanTable.getInstance().getClanByName(_pledgeName);
		if(clan == null)
		{
			_activeChar.sendMessage("No such clan.");
			_activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		_log.info("RequestSurrenderPledgeWar by " + getClient().getActiveChar().getClan().getName() + " with " + _pledgeName);

		if(!_clan.isAtWarWith(clan.getClanId()))
		{
			_activeChar.sendMessage("You aren't at war with this clan.");
			_activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		_activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_SURRENDERED_TO_THE_S1_CLAN).addString(_pledgeName));
		_activeChar.deathPenalty(false);
		ClanTable.getInstance().deleteclanswars(_clan.getClanId(), clan.getClanId());
	}

	@Override
	public String getType()
	{
		return _C__51_REQUESTSURRENDERPLEDGEWAR;
	}
}