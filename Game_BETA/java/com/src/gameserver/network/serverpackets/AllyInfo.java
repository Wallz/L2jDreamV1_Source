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
package com.src.gameserver.network.serverpackets;

import javolution.text.TextBuilder;

import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;

public class AllyInfo extends L2GameServerPacket
{
	private static final String _S__7A_FRIENDLIST = "[S] 7a AllyInfo";

	private static L2PcInstance _cha;

	public AllyInfo(L2PcInstance cha)
	{
		_cha = cha;
	}

	@Override
	protected final void writeImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if(activeChar == null)
		{
			return;
		}

		if(activeChar.getAllyId() == 0)
		{
			_cha.sendPacket(new SystemMessage(SystemMessageId.NO_CURRENT_ALLIANCES));
			return;
		}

		_cha.sendPacket(new SystemMessage(SystemMessageId.ALLIANCE_INFO_HEAD));
		_cha.sendPacket(new SystemMessage(SystemMessageId.ALLIANCE_NAME_S1).addString(_cha.getClan().getAllyName()));

		int online = 0;
		int count = 0;
		int clancount = 0;
		for(L2Clan clan : ClanTable.getInstance().getClans())
		{
			if(clan.getAllyId() == _cha.getAllyId())
			{
				clancount++;
				online += clan.getOnlineMembers("").length;
				count += clan.getMembers().length;
			}
		}

		_cha.sendPacket(new SystemMessage(SystemMessageId.CONNECTION_S1_TOTAL_S2).addString("" + online).addString("" + count));

		L2Clan leaderclan = ClanTable.getInstance().getClan(_cha.getAllyId());

		_cha.sendPacket(new SystemMessage(SystemMessageId.ALLIANCE_LEADER_S2_OF_S1).addString(leaderclan.getName()).addString(leaderclan.getLeaderName()));
		_cha.sendPacket(new SystemMessage(SystemMessageId.ALLIANCE_CLAN_TOTAL_S1).addString("" + clancount));
		_cha.sendPacket(new SystemMessage(SystemMessageId.CLAN_INFO_HEAD));

		for(L2Clan clan : ClanTable.getInstance().getClans())
		{
			if(clan.getAllyId() == _cha.getAllyId())
			{
				_cha.sendPacket(new SystemMessage(SystemMessageId.CLAN_INFO_NAME).addString(clan.getName()));
				_cha.sendPacket(new SystemMessage(SystemMessageId.CLAN_INFO_LEADER).addString(clan.getLeaderName()));
				_cha.sendPacket(new SystemMessage(SystemMessageId.CLAN_INFO_LEVEL).addNumber(clan.getLevel()));
				_cha.sendPacket(new SystemMessage(SystemMessageId.CLAN_INFO_SEPARATOR));
			}
		}

		_cha.sendPacket(new SystemMessage(SystemMessageId.CLAN_INFO_FOOT));
		NpcHtmlMessage adminReply = new NpcHtmlMessage(0);  
		TextBuilder replyMSG = new TextBuilder("<html><title>Alliance Information</title><body>");  
		replyMSG.append("<center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center>");  
		for (L2Clan clan : ClanTable.getInstance().getClans())  
			if (clan.getAllyId() == _cha.getAllyId())  
				replyMSG.append("<br><center><button value=\""+clan.getName()+"\" action=\"bypass -h show_clan_info "+clan.getName()+"\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></center><br>");  
		replyMSG.append("<center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center>");  
		replyMSG.append("</body></html>");  
		adminReply.setHtml(replyMSG.toString());  
		_cha.sendPacket(adminReply);
	}

	@Override
	public String getType()
	{
		return _S__7A_FRIENDLIST;
	}

}