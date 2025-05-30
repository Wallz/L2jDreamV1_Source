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

import com.src.gameserver.managers.QuestManager;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.quest.Quest;
import com.src.gameserver.model.quest.QuestState;
import com.src.gameserver.network.serverpackets.QuestList;

public final class RequestQuestAbort extends L2GameClientPacket
{
	private static final String _C__64_REQUESTQUESTABORT = "[C] 64 RequestQuestAbort";

	private int _questId;

	@Override
	protected void readImpl()
	{
		_questId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		Quest qe = QuestManager.getInstance().getQuest(_questId);
		if(qe != null)
		{
			QuestState qs = activeChar.getQuestState(qe.getName());
			if(qs != null)
			{
				qs.exitQuest(true);
				activeChar.sendMessage("Quest aborted.");
				QuestList ql = new QuestList();
				activeChar.sendPacket(ql);
			}
		}
	}

	@Override
	public String getType()
	{
		return _C__64_REQUESTQUESTABORT;
	}
}