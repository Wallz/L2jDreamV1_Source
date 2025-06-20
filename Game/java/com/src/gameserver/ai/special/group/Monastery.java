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

import java.util.Collection;

import javolution.util.FastList;

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.ai.L2AttackableAIScript;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.templates.skills.L2SkillType;
import com.src.gameserver.util.Util;
import com.src.util.random.Rnd;

public class Monastery extends L2AttackableAIScript
{
	static final int[] mobs1 = {22124, 22125, 22126, 22127, 22129};
	static final int[] mobs2 = {22134, 22135};

	public Monastery(int questId, String name, String descr)
	{
		super(questId, name, descr);
		registerMobs(mobs1, QuestEventType.ON_AGGRO_RANGE_ENTER, QuestEventType.ON_SPAWN, QuestEventType.ON_SPELL_FINISHED);
		registerMobs(mobs2, QuestEventType.ON_SPELL_FINISHED);
	}

	@Override
	public String onAggroRangeEnter(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		if(contains(mobs1,npc.getNpcId()) && !npc.isInCombat() && npc.getTarget() == null)
		{
			if(player.getActiveWeaponInstance() != null && !player.isSilentMoving() && !player.isGM() && !player.getAppearance().getInvisible())
			{
				npc.setTarget(player);
				npc.broadcastNpcSay("Brother " + player.getName() + ", move your weapon away!!");
				switch (npc.getNpcId())
				{
					case 22124:
					case 22126:
					{
						npc.doCast(SkillTable.getInstance().getInfo(4589, 8));
						break;
					}
					default:
					{
						npc.setIsRunning(true);
						((L2Attackable) npc).addDamageHate(player, 0, 999);
						npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, player);
						break;
					}
				}
			}
			else if(((L2Attackable)npc).getMostHated() == null)
			{
				return null;
			}
		}
		return super.onAggroRangeEnter(npc, player, isPet);
	}

	@Override
	public String onSkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet)
	{
		if(contains(mobs2,npc.getNpcId()))
		{
			if(skill.getSkillType() == L2SkillType.AGGDAMAGE && targets.length != 0)
			{
				for(L2Object obj : targets)
				{
					if(obj.equals(npc))
					{
						if(Rnd.get(100) < 50)
							npc.broadcastNpcSay("Brother " + caster.getName() + ", move your weapon away!!");
						((L2Attackable) npc).addDamageHate(caster, 0, 999);
						npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, caster);
						break;
					}
				}
			}
		}
		return super.onSkillSee(npc, caster, skill, targets, isPet);
	}

	@Override
	public String onSpawn(L2Npc npc)
	{
		if(contains(mobs1,npc.getNpcId()))
		{
			FastList<L2Playable> result = new FastList<L2Playable>();
			Collection<L2Object> objs = npc.getKnownList().getKnownObjects().values();
			for(L2Object obj : objs)
			{
				if(obj instanceof L2PcInstance || obj instanceof L2PetInstance)
				{
					if(Util.checkIfInRange(npc.getAggroRange(), npc, obj, true) && !((L2Character) obj).isDead())
					{
						result.add((L2Playable) obj);
					}
				}
			}
			if(!result.isEmpty() && result.size() != 0)
			{
				Object[] characters = result.toArray();
				for(Object obj : characters)
				{
					L2Playable target = (L2Playable) (obj instanceof L2PcInstance ? obj : ((L2Summon) obj).getOwner());

					if(target.getActiveWeaponInstance() == null || (target instanceof L2PcInstance && ((L2PcInstance)target).isSilentMoving()) || (target instanceof L2Summon && ((L2Summon)target).getOwner().isSilentMoving()))
					{
						continue;
					}

					if(target.getActiveWeaponInstance() != null && !npc.isInCombat() && npc.getTarget() == null)
					{
						npc.setTarget(target);
						if(Rnd.get(100) < 50)
							npc.broadcastNpcSay("Brother " + target.getName() + ", move your weapon away!!");
						switch(npc.getNpcId())
						{
							case 22124:
							case 22126:
							case 22127:
							{
								L2Skill skill = SkillTable.getInstance().getInfo(4589,8);
								npc.doCast(skill);
								break;
							}
							default:
							{
								npc.setIsRunning(true);
								((L2Attackable) npc).addDamageHate(target, 0, 999);
								npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
								break;
							}
						}
					}
				}
			}
		}
		return super.onSpawn(npc);
	}

	@Override
	public String onSpellFinished(L2Npc npc, L2PcInstance player, L2Skill skill)
	{
		if(contains(mobs1,npc.getNpcId()) && skill.getId() == 4589)
		{
			npc.setIsRunning(true);
			((L2Attackable) npc).addDamageHate(player, 0, 999);
			npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, player);
		}
		return super.onSpellFinished(npc, player, skill);
	}

}