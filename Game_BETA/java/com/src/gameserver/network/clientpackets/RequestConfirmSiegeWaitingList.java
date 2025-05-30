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
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.siege.Castle;
import com.src.gameserver.network.serverpackets.SiegeDefenderList;

public final class RequestConfirmSiegeWaitingList extends L2GameClientPacket
{
	private static final String _C__A5_RequestConfirmSiegeWaitingList = "[C] a5 RequestConfirmSiegeWaitingList";

	private int _approved;
	private int _castleId;
	private int _clanId;

	@Override
	protected void readImpl()
	{
		_castleId = readD();
		_clanId = readD();
		_approved = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if(activeChar == null)
		{
			return;
		}

		if(activeChar.getClan() == null)
		{
			return;
		}

		Castle castle = CastleManager.getInstance().getCastleById(_castleId);
		if(castle == null)
		{
			return;
		}

		if(castle.getOwnerId() != activeChar.getClanId() || !activeChar.isClanLeader())
		{
			return;
		}

		L2Clan clan = ClanTable.getInstance().getClan(_clanId);
		if(clan == null)
		{
			return;
		}

		if(!castle.getSiege().getIsRegistrationOver())
		{
			if(_approved == 1)
			{
				if(castle.getSiege().checkIsDefenderWaiting(clan))
				{
					castle.getSiege().approveSiegeDefenderClan(_clanId);
				}
				else
					return;
			}
			else
			{
				if(castle.getSiege().checkIsDefenderWaiting(clan) || castle.getSiege().checkIsDefender(clan))
				{
					castle.getSiege().removeSiegeClan(_clanId);
				}
			}
		}

		activeChar.sendPacket(new SiegeDefenderList(castle));
	}

	@Override
	public String getType()
	{
		return _C__A5_RequestConfirmSiegeWaitingList;
	}

}