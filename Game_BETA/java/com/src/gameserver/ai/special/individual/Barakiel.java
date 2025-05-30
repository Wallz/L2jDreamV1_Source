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
package com.src.gameserver.ai.special.individual;

import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.quest.Quest;

public class Barakiel extends Quest
{
	private static final int BARAKIEL = 25325;
	private static final int x1 = 89800;
	private static final int x2 = 93200;
	private static final int y1 = -87038;

	public Barakiel(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addEventId(BARAKIEL, Quest.QuestEventType.ON_ATTACK);
	}

	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		int npcId = npc.getNpcId();
		if(npcId == BARAKIEL)
		{
			int x = npc.getX();
			int y = npc.getY();
			if(x < x1 || x > x2 || y < y1)
			{
				npc.teleToLocation(91008, -85904, -2736);
				npc.getStatus().setCurrentHp(npc.getMaxHp());
			}
		}

		return super.onAttack(npc, attacker, damage, isPet);
	}

}