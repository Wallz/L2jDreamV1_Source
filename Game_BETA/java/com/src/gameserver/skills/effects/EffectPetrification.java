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
import com.src.gameserver.skills.Env;
import com.src.gameserver.templates.skills.L2EffectType;

public class EffectPetrification extends L2Effect
{
	public EffectPetrification(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.PETRIFICATION;
	}

	@Override
	public void onStart()
	{
		getEffected().startAbnormalEffect(L2Character.ABNORMAL_EFFECT_HOLD_2);
		getEffected().setPetrified(true);
		//getEffected().setIsParalyzed(true);
		//getEffected().setIsInvul(true);
	}

	@Override
	public void onExit()
	{
		getEffected().stopAbnormalEffect(L2Character.ABNORMAL_EFFECT_HOLD_2);
		getEffected().setPetrified(false);
		//getEffected().setIsParalyzed(false);
		//getEffected().setIsInvul(false);
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}
}