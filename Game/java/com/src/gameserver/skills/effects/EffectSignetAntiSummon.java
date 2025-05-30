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

import com.src.gameserver.ai.CtrlEvent;
import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.instance.L2EffectPointInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.Env;
import com.src.gameserver.templates.skills.L2EffectType;

public final class EffectSignetAntiSummon extends L2Effect
{
	private L2EffectPointInstance _actor;

	public EffectSignetAntiSummon(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.SIGNET_GROUND;
	}

	@Override
	public void onStart()
	{
		_actor = (L2EffectPointInstance) getEffected();
	}

	@Override
	public boolean onActionTime()
	{
		if (getCount() == getTotalCount() - 1)
			return true; // do nothing first time
			
		int mpConsume = getSkill().getMpConsume();
		L2PcInstance caster = (L2PcInstance) getEffector();
		
		for (L2Character cha : _actor.getKnownList().getKnownCharactersInRadius(getSkill().getSkillRadius()))
		{
			if (cha == null)
				continue;
			
			if (cha instanceof L2Playable)
			{
				if (caster.canAttackCharacter(cha))
				{
					L2PcInstance owner = null;
					if (cha instanceof L2Summon)
						owner = ((L2Summon) cha).getOwner();
					else
						owner = (L2PcInstance) cha;
					
					if (owner != null && owner.getPet() != null)
					{
						if (mpConsume > getEffector().getCurrentMp())
						{
							getEffector().sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SKILL_REMOVED_DUE_LACK_MP));
							return false;
						}
						getEffector().reduceCurrentMp(mpConsume);
						
						owner.getPet().unSummon(owner);
						owner.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, getEffector());
					}
				}
			}
		}
		return true;
	}

	@Override
	public void onExit()
	{
		if(_actor != null)
		{
			_actor.deleteMe();
		}
	}
}