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

import java.util.logging.Logger;

import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.PledgeInfo;

public final class RequestPledgeInfo extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestPledgeInfo.class.getName());

	private static final String _C__66_REQUESTPLEDGEINFO = "[C] 66 RequestPledgeInfo";

	private int _clanId;

	@Override
	protected void readImpl()
	{
		_clanId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		L2Clan clan = ClanTable.getInstance().getClan(_clanId);

		if(activeChar == null)
		{
			return;
		}

		if(clan == null)
		{
			_log.warning("Clan data for clanId " + _clanId + " is missing for player " + activeChar.getName());
			return;
		}

		PledgeInfo pc = new PledgeInfo(clan);
		if(activeChar != null)
		{
			activeChar.sendPacket(pc);
		}
	}

	@Override
	public String getType()
	{
		return _C__66_REQUESTPLEDGEINFO;
	}

}