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

public class Hallate extends Quest
{
	private static final int HALLATE = 25220;
	private static final int z1 = -2150;
	private static final int z2 = -1650;

	public Hallate(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addEventId(HALLATE, Quest.QuestEventType.ON_ATTACK);
	}

	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		int npcId = npc.getNpcId();
		if(npcId == HALLATE)
		{
			int z = npc.getZ();
			if(z > z2 || z < z1)
			{
				npc.teleToLocation(113548, 17061, -2125);
				npc.getStatus().setCurrentHp(npc.getMaxHp());
			}
		}

		return super.onAttack(npc, attacker, damage, isPet);
	}

}