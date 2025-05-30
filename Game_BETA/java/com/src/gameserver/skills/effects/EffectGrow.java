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

import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.skills.Env;
import com.src.gameserver.templates.skills.L2EffectType;

public class EffectGrow extends L2Effect
{
	public EffectGrow(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.BUFF;
	}

	@Override
	public void onStart()
	{
		if(getEffected() instanceof L2Npc)
		{
			L2Npc npc = (L2Npc) getEffected();
			npc.setCollisionHeight((int) (npc.getCollisionHeight() * 1.24));
			npc.setCollisionRadius((int) (npc.getCollisionRadius() * 1.19));

			getEffected().startAbnormalEffect(L2Character.ABNORMAL_EFFECT_GROW);
		}
	}

	@Override
	public boolean onActionTime()
	{
		if(getEffected() instanceof L2Npc)
		{
			L2Npc npc = (L2Npc) getEffected();
			npc.setCollisionHeight(npc.getTemplate().collisionHeight);
			npc.setCollisionRadius(npc.getTemplate().collisionRadius);

			getEffected().stopAbnormalEffect(L2Character.ABNORMAL_EFFECT_GROW);
		}

		return false;
	}

	@Override
	public void onExit()
	{
		if(getEffected() instanceof L2Npc)
		{
			L2Npc npc = (L2Npc) getEffected();
			npc.setCollisionHeight(npc.getTemplate().collisionHeight);
			npc.setCollisionRadius(npc.getTemplate().collisionRadius);

			getEffected().stopAbnormalEffect(L2Character.ABNORMAL_EFFECT_GROW);
		}
	}
}