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

import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.quest.Quest;
import com.src.gameserver.model.quest.QuestState;

public class QuestList extends L2GameServerPacket
{
	private static final String _S__98_QUESTLIST = "[S] 80 QuestList";

	private Quest[] _quests;
	private L2PcInstance _activeChar;

	public QuestList()
	{

	}

	@Override
	public void runImpl()
	{
		if(getClient() != null && getClient().getActiveChar() != null)
		{
			_activeChar = getClient().getActiveChar();
			_quests = _activeChar.getAllActiveQuests();
		}
	}

	@Override
	protected final void writeImpl()
	{
		if(_quests == null || _quests.length == 0)
		{
			writeC(0x80);
			writeH(0);
			writeH(0);
			return;
		}

		writeC(0x80);
		writeH(_quests.length);
		for(Quest q : _quests)
		{
			writeD(q.getQuestIntId());
			QuestState qs = _activeChar.getQuestState(q.getName());
			if(qs == null)
			{
				writeD(0);
				continue;
			}

			int states = qs.getInt("__compltdStateFlags");
			if(states != 0)
			{
				writeD(states);
			}
			else
			{
				writeD(qs.getInt("cond"));
			}
		}
	}

	@Override
	public String getType()
	{
		return _S__98_QUESTLIST;
	}

}