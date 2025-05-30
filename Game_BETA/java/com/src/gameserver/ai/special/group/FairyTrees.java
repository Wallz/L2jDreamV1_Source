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

import java.util.ArrayList;

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.quest.Quest;
import com.src.util.random.Rnd;

public class FairyTrees extends Quest
{
	private ArrayList<mobs> _mobs = new ArrayList<mobs>();

	private static class mobs
	{
		private int _id;

		private mobs(int id)
		{
			_id = id;
		}

		private int getId()
		{
			return _id;
		}
	}

	public FairyTrees(int questId, String name, String descr)
	{
		super(questId, name, descr);

		_mobs.add(new mobs(27185));
		_mobs.add(new mobs(27186));
		_mobs.add(new mobs(27187));
		_mobs.add(new mobs(27188));

		int[] mobsKill =
		{
				27185, 27186, 27187, 27188
		};

		for(int mob : mobsKill)
		{
			addEventId(mob, Quest.QuestEventType.ON_KILL);
		}
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		int npcId = npc.getNpcId();
		for(mobs monster : _mobs)
		{
			if(npcId == monster.getId())
			{
				for(int i = 0; i < 20; i++)
				{
					L2Attackable newNpc = (L2Attackable) addSpawn(27189, npc.getX(), npc.getY(), npc.getZ(), 0, false, 30000);
					L2Character originalKiller = isPet ? killer.getPet() : killer;
					newNpc.setRunning();
					newNpc.addDamageHate(originalKiller, 0, 999);
					newNpc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, originalKiller);
					if(Rnd.get(1, 2) == 1)
					{
						L2Skill skill = SkillTable.getInstance().getInfo(4243, 1);
						if(skill != null && originalKiller != null)
						{
							skill.getEffects(newNpc, originalKiller);
						}
					}
				}
			}
		}

		return super.onKill(npc, killer, isPet);
	}

}