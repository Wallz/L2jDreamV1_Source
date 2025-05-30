/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.src.gameserver.ai.special.group;

import com.src.gameserver.ai.L2AttackableAIScript;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.util.random.Rnd;

public class HotSpringDisease extends L2AttackableAIScript
{
	static final int[] disease1mobs =
	{
		21314,
		21316,
		21317,
		21319,
		21321,
		21322
	}; // Monsters which cast Hot Spring Malaria (4554)
	static final int[] disease2mobs =
	{
		21317,
		21322
	}; // Monsters which cast Hot Springs Flu (4553)
	static final int[] disease3mobs =
	{
		21316,
		21319
	}; // Monsters which cast Hot Springs Cholera (4552)
	static final int[] disease4mobs =
	{
		21314,
		21321
	}; // Monsters which cast Hot Springs Rheumatism (4551)
	
	// Chance to get infected by disease
	private static final int DISEASE_CHANCE = 5;
	
	public HotSpringDisease(int questId, String name, String descr)
	{
		super(questId, name, descr);
		registerMobs(disease1mobs, QuestEventType.ON_ATTACK);
	}
	
	@Override
	public String onAttack(L2Npc npc, L2PcInstance victim, int damage, boolean isPet)
	{
		if (contains(disease1mobs, npc.getNpcId()))
			tryToApplyEffect(npc, victim, 4554);
		
		if (contains(disease2mobs, npc.getNpcId()))
			tryToApplyEffect(npc, victim, 4553);
		
		if (contains(disease3mobs, npc.getNpcId()))
			tryToApplyEffect(npc, victim, 4552);
		
		if (contains(disease4mobs, npc.getNpcId()))
			tryToApplyEffect(npc, victim, 4551);
		
		return super.onAttack(npc, victim, damage, isPet);
	}
	
	private void tryToApplyEffect(L2Npc npc, L2PcInstance victim, int skillId)
	{
		if (skillId != 0)
		{
			if (Rnd.get(100) < DISEASE_CHANCE)
			{
				int level = 0;
				L2Effect[] effects = victim.getAllEffects();
				if (effects.length != 0 || effects != null)
				{
					for (L2Effect e : effects)
					{
						if (e.getSkill().getId() == skillId)
						{
							level = e.getSkill().getLevel();
							e.exit();
						}
					}
				}
				
				// When skill level reaches 10, don't go further (avoid pointless spam).
				if (level == 10)
					return;
				
				++level;
				
				L2Skill tempSkill = SkillTable.getInstance().getInfo(skillId, level);
				tempSkill.getEffects(npc, victim);
			}
		}
	}
}