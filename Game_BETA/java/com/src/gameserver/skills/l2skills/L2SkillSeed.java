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
package com.src.gameserver.skills.l2skills;

import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.skills.effects.EffectSeed;
import com.src.gameserver.templates.StatsSet;
import com.src.gameserver.templates.skills.L2EffectType;

public class L2SkillSeed extends L2Skill
{
	public L2SkillSeed(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(L2Character caster, L2Object[] targets)
	{
		if(caster.isAlikeDead())
		{
			return;
		}

		for(L2Object target2 : targets)
		{
			L2Character target = (L2Character) target2;
			if(target.isAlikeDead() && getTargetType() != SkillTargetType.TARGET_CORPSE_MOB)
			{
				continue;
			}

			EffectSeed oldEffect = (EffectSeed) target.getFirstEffect(getId());
			if(oldEffect == null)
			{
				getEffects(caster, target);
			}
			else
			{
				oldEffect.increasePower();
			}

			L2Effect[] effects = target.getAllEffects();
			for(L2Effect effect : effects)
			{
				if(effect.getEffectType() == L2EffectType.SEED)
				{
					effect.rescheduleEffect();
				}
			}
		}
	}
}