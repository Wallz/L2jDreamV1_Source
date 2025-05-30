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

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.ai.L2AttackableAIScript;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2ChestInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.util.Util;
import com.src.util.random.Rnd;

/**
 * Chest AI implementation.
 * @author Fulminus
 */
public class Chests extends L2AttackableAIScript
{
	private static final int SKILL_DELUXE_KEY = 2229;
	
	// Base chance for BOX to be opened.
	private static final int BASE_CHANCE = 100;
	
	// Percent to decrease base chance when grade of DELUXE key not match.
	private static final int LEVEL_DECREASE = 40;
	
	// Chance for a chest to actually be a BOX (as opposed to being a mimic).
	private static final int IS_BOX = 40;
	
	private static final int[] NPC_IDS =
	{
		18265,
		18266,
		18267,
		18268,
		18269,
		18270,
		18271,
		18272,
		18273,
		18274,
		18275,
		18276,
		18277,
		18278,
		18279,
		18280,
		18281,
		18282,
		18283,
		18284,
		18285,
		18286,
		18287,
		18288,
		18289,
		18290,
		18291,
		18292,
		18293,
		18294,
		18295,
		18296,
		18297,
		18298,
		21671,
		21694,
		21717,
		21740,
		21763,
		21786,
		21801,
		21802,
		21803,
		21804,
		21805,
		21806,
		21807,
		21808,
		21809,
		21810,
		21811,
		21812,
		21813,
		21814,
		21815,
		21816,
		21817,
		21818,
		21819,
		21820,
		21821,
		21822
	};
	
	public Chests(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		registerMobs(NPC_IDS, QuestEventType.ON_ATTACK, QuestEventType.ON_SKILL_SEE);
	}
	
	@Override
	public String onSkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet)
	{
		if (npc instanceof L2ChestInstance)
		{
			// This behavior is only run when the target of skill is the passed npc.
			if (!Util.contains(targets, npc))
				return super.onSkillSee(npc, caster, skill, targets, isPet);
			
			final L2ChestInstance chest = ((L2ChestInstance) npc);
			
			// If this chest has already been interacted, no further AI decisions are needed.
			if (!chest.isInteracted())
			{
				chest.setInteracted();
				
				// If it's the first interaction, check if this is a box or mimic.
				if (Rnd.get(100) < IS_BOX)
				{
					if (skill.getId() == SKILL_DELUXE_KEY)
					{
						// check the chance to open the box
						int keyLevelNeeded = (chest.getLevel() / 10) - skill.getLevel();
						if (keyLevelNeeded < 0)
							keyLevelNeeded *= -1;
						
						final int chance = BASE_CHANCE - keyLevelNeeded * LEVEL_DECREASE;
						
						// Success, die with rewards.
						if (Rnd.get(100) < chance)
						{
							chest.setSpecialDrop();
							chest.doDie(caster);
						}
						// Used a key but failed to open: disappears with no rewards.
						else
							chest.deleteMe(); // TODO: replace for a better system (as chests attack once before decaying)
					}
					else
						chest.doCast(SkillTable.getInstance().getInfo(4143, Math.min(10, Math.round(npc.getLevel() / 10))));
				}
				// Mimic behavior : attack the caster.
				else
				{
					final L2Character originalCaster = isPet ? caster.getPet() : caster;
					
					chest.setRunning();
					chest.addDamageHate(originalCaster, 0, 999);
					chest.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, originalCaster);
				}
			}
		}
		return super.onSkillSee(npc, caster, skill, targets, isPet);
	}
	
	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		if (npc instanceof L2ChestInstance)
		{
			final L2ChestInstance chest = ((L2ChestInstance) npc);
			
			// If this has already been interacted, no further AI decisions are needed.
			if (!chest.isInteracted())
			{
				chest.setInteracted();
				
				// If it was a box, cast a suicide type skill.
				if (Rnd.get(100) < IS_BOX)
					chest.doCast(SkillTable.getInstance().getInfo(4143, Math.min(10, Math.round(npc.getLevel() / 10))));
				// Mimic behavior : attack the caster.
				else
				{
					final L2Character originalAttacker = isPet ? attacker.getPet() : attacker;
					
					chest.setRunning();
					chest.addDamageHate(originalAttacker, 0, (damage * 100) / (chest.getLevel() + 7));
					chest.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, originalAttacker);
				}
			}
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}

}