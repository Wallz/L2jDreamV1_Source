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

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.Env;
import com.src.gameserver.templates.skills.L2EffectType;

class EffectRelax extends L2Effect
{
	public EffectRelax(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.RELAXING;
	}

	@Override
	public void onStart()
	{
		if(getEffected() instanceof L2PcInstance)
		{
			setRelax(true);
			((L2PcInstance) getEffected()).sitDown();
		}
		else
		{
			getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_REST);
		}

		super.onStart();
	}

	@Override
	public void onExit()
	{
		setRelax(false);
		super.onExit();
	}

	@Override
	public boolean onActionTime()
	{
		boolean retval = true;
		if(getEffected().isDead())
		{
			retval = false;
		}

		if(getEffected() instanceof L2PcInstance)
		{
			if(!((L2PcInstance) getEffected()).isSitting())
			{
				retval = false;
			}
		}

		if(getEffected().getCurrentHp() + 1 > getEffected().getMaxHp())
		{
			if(getSkill().isToggle())
			{
				getEffected().sendPacket(new SystemMessage(SystemMessageId.SKILL_DEACTIVATED_HP_FULL));
				retval = false;
			}
		}

		double manaDam = calc();

		if(manaDam > getEffected().getCurrentMp())
		{
			if(getSkill().isToggle())
			{
				getEffected().sendPacket(new SystemMessage(SystemMessageId.SKILL_REMOVED_DUE_LACK_MP));
				retval = false;
			}
		}

		if(!retval)
		{
			setRelax(retval);
		}
		else
		{
			getEffected().reduceCurrentMp(manaDam);
		}

		return retval;
	}

	private void setRelax(boolean val)
	{
		if(getEffected() instanceof L2PcInstance)
		{
			((L2PcInstance) getEffected()).setRelax(val);
		}
	}
}