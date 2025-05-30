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

import com.src.Config;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.L2ClanMember;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.PledgeShowMemberListDelete;
import com.src.gameserver.network.serverpackets.SystemMessage;

public final class RequestOustPledgeMember extends L2GameClientPacket
{
	static Logger _log = Logger.getLogger(RequestOustPledgeMember.class.getName());

	private static final String _C__27_REQUESTOUSTPLEDGEMEMBER = "[C] 27 RequestOustPledgeMember";

	private String _target;

	@Override
	protected void readImpl()
	{
		_target = readS();
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
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER));
			return;
		}

		if((activeChar.getClanPrivileges() & L2Clan.CP_CL_DISMISS) != L2Clan.CP_CL_DISMISS)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return;
		}

		if(activeChar.getName().equalsIgnoreCase(_target))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_DISMISS_YOURSELF));
			return;
		}

		L2Clan clan = activeChar.getClan();

		L2ClanMember member = clan.getClanMember(_target);

		if(member == null)
		{
			_log.warning("Target (" + _target + ") is not member of the clan");
			return;
		}

		if(member.isOnline() && member.getPlayerInstance().isInCombat())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CLAN_MEMBER_CANNOT_BE_DISMISSED_DURING_COMBAT));
			return;
		}

		clan.removeClanMember(_target, 0);
		clan.setCharPenaltyExpiryTime(System.currentTimeMillis() + Config.ALT_CLAN_JOIN_DAYS * 86400000L);
		clan.updateClanInDB();

		clan.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_EXPELLED).addString(member.getName()));

		activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_SUCCEEDED_IN_EXPELLING_CLAN_MEMBER));
		activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_MUST_WAIT_BEFORE_ACCEPTING_A_NEW_MEMBER));

		clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(_target));
		if(member.isOnline())
		{
			L2PcInstance player = member.getPlayerInstance();
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_MEMBERSHIP_TERMINATED));
			player.setActiveWarehouse(null);
		}
	}

	@Override
	public String getType()
	{
		return _C__27_REQUESTOUSTPLEDGEMEMBER;
	}

}