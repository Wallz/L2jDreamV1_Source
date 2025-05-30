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
package com.src.gameserver.skills.effects;

import java.util.List;

import javolution.util.FastList;

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.skills.Env;
import com.src.gameserver.templates.skills.L2EffectType;
import com.src.util.random.Rnd;

final class EffectConfuseMob extends L2Effect
{
	public EffectConfuseMob(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.CONFUSE_MOB_ONLY;
	}

	@Override
	public void onStart()
	{
		getEffected().startConfused();
		onActionTime();
	}

	@Override
	public void onExit()
	{
		getEffected().stopConfused(this);
	}

	@Override
	public boolean onActionTime()
	{
		List<L2Character> targetList = new FastList<L2Character>();

		for(L2Object obj : getEffected().getKnownList().getKnownObjects().values())
		{
			if(obj == null)
			{
				continue;
			}

			if(obj instanceof L2Attackable && obj != getEffected())
			{
				targetList.add((L2Character) obj);
			}
		}

		if(targetList.size() == 0)
		{
			return true;
		}

		int nextTargetIdx = Rnd.nextInt(targetList.size());
		L2Object target = targetList.get(nextTargetIdx);

		getEffected().setTarget(target);
		getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);

		return true;
	}
}