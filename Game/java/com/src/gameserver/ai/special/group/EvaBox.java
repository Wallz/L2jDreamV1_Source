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
package com.src.gameserver.ai.special.group;

import com.src.gameserver.datatables.sql.ItemTable;
import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.quest.Quest;
import com.src.util.random.Rnd;

public class EvaBox extends Quest
{
	private final static int[] KISS_OF_EVA = {1073,3141,3252};
	private final static int BOX = 32342;
	private final static int[] REWARDS = {9692,9693};

	public EvaBox(int questId, String name, String descr)
	{
		super(questId,name,descr);

		addEventId(BOX, Quest.QuestEventType.ON_KILL);
	}

	public void dropItem(L2Npc npc, int itemId, int count, L2PcInstance player)
	{
		L2ItemInstance ditem = ItemTable.getInstance().createItem("Loot", itemId, count, player);
		ditem.dropMe(npc, npc.getX(),npc.getY(),npc.getZ());
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		boolean found = false;
		for(L2Effect effect : killer.getAllEffects())
		{
			for(int i=0;i<3;i++)
			{
				if(effect.getSkill().getId() == KISS_OF_EVA[i])
				{
					found = true;
				}
			}
		}

		if(found == true)
		{
			int dropid = Rnd.get(1);
			if(dropid == 1)
			{
				dropItem(npc,REWARDS[dropid],1,killer);
			}
			else if(dropid == 0)
			{
				dropItem(npc,REWARDS[dropid],1,killer);
			}
		}

		return super.onKill(npc,killer,isPet);
	}

}