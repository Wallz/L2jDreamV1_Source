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
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.Env;
import com.src.gameserver.templates.skills.L2EffectType;
import com.src.gameserver.templates.skills.L2SkillType;

public final class EffectDeflectBuff extends L2Effect
{
	public EffectDeflectBuff(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.PREVENT_BUFF;
	}

	@Override
	public boolean onActionTime()
	{
		if(getSkill().getSkillType() != L2SkillType.CONT)
		{
			return false;
		}

		double manaDam = calc();

		if(manaDam > getEffected().getCurrentMp())
		{
			getEffected().sendPacket(new SystemMessage(SystemMessageId.SKILL_REMOVED_DUE_LACK_MP));
			return false;
		}

		getEffected().reduceCurrentMp(manaDam);
		return true;
	}

	@Override
	public void onStart()
	{
		getEffected().setIsBuffProtected(true);
		return;
	}

	@Override
	public void onExit()
	{
		getEffected().setIsBuffProtected(false);
	}
}