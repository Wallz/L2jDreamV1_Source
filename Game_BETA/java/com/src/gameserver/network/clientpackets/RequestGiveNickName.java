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

import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.L2ClanMember;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class RequestGiveNickName extends L2GameClientPacket
{
	static Logger _log = Logger.getLogger(RequestGiveNickName.class.getName());

	private static final String _C__55_REQUESTGIVENICKNAME = "[C] 55 RequestGiveNickName";

	private String _target;
	private String _title;

	@Override
	protected void readImpl()
	{
		_target = readS();
		_title = readS();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if(activeChar == null)
		{
			return;
		}

		if(activeChar.isNoble() && _target.matches(activeChar.getName()))
		{
			activeChar.setTitle(_title);
			activeChar.sendPacket(new SystemMessage(SystemMessageId.TITLE_CHANGED));
			activeChar.broadcastTitleInfo();
		}

		else if((activeChar.getClanPrivileges() & L2Clan.CP_CL_GIVE_TITLE) == L2Clan.CP_CL_GIVE_TITLE)
		{
			if(activeChar.getClan().getLevel() < 3)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CLAN_LVL_3_NEEDED_TO_ENDOWE_TITLE));
				return;
			}

			L2ClanMember member1 = activeChar.getClan().getClanMember(_target);
			if(member1 != null)
			{
				L2PcInstance member = member1.getPlayerInstance();
				if(member != null)
				{
					member.setTitle(_title);
					member.sendPacket(new SystemMessage(SystemMessageId.TITLE_CHANGED));
					member.broadcastTitleInfo();
				}
				else
				{
					activeChar.sendMessage("Target needs to be online to get a title.");
				}
			}
			else
			{
				activeChar.sendMessage("Target does not belong to your clan.");
			}
		}
	}

	@Override
	public String getType()
	{
		return _C__55_REQUESTGIVENICKNAME;
	}

}