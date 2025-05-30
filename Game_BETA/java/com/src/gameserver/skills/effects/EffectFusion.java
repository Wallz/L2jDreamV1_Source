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

import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.model.L2Effect;
import com.src.gameserver.skills.Env;
import com.src.gameserver.templates.skills.L2EffectType;

public final class EffectFusion extends L2Effect
{
	public int _effect;
	public int _maxEffect;

	public EffectFusion(Env env, EffectTemplate template)
	{
		super(env, template);
		_effect = getSkill().getLevel();
		_maxEffect = 10;
	}

	@Override
	public boolean onActionTime()
	{
		return true;
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.FUSION;
	}

	public void increaseEffect()
	{
		if(_effect < _maxEffect)
		{
			_effect++;
			updateBuff();
		}
	}

	public void decreaseForce()
	{
		_effect--;
		if(_effect < 1)
		{
			exit();
		}
		else
		{
			updateBuff();
		}
	}

	private void updateBuff()
	{
		exit();
		SkillTable.getInstance().getInfo(getSkill().getId(), _effect).getEffects(getEffector(), getEffected());
	}
}