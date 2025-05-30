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
package com.src.gameserver.model;

import javolution.util.FastMap;

import com.src.gameserver.handler.ISkillHandler;
import com.src.gameserver.handler.SkillHandler;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.network.serverpackets.MagicSkillLaunched;
import com.src.gameserver.network.serverpackets.MagicSkillUser;
import com.src.gameserver.skills.Formulas;

public class ChanceSkillList extends FastMap<L2Skill, ChanceCondition>
{
	private static final long serialVersionUID = -3523525435531L;

	private L2Character _owner;

	@SuppressWarnings("deprecation")
	public ChanceSkillList(L2Character owner)
	{
		super();
		setShared(true);
		_owner = owner;
	}

	public L2Character getOwner()
	{
		return _owner;
	}

	public void setOwner(L2Character owner)
	{
		_owner = owner;
	}

	public void onHit(L2Character target, boolean ownerWasHit, boolean wasCrit)
	{
		int event;
		if(ownerWasHit)
		{
			event = ChanceCondition.EVT_ATTACKED | ChanceCondition.EVT_ATTACKED_HIT;
			if(wasCrit)
			{
				event |= ChanceCondition.EVT_ATTACKED_CRIT;
			}
		}
		else
		{
			event = ChanceCondition.EVT_HIT;
			if(wasCrit)
			{
				event |= ChanceCondition.EVT_CRIT;
			}
		}

		onEvent(event, target);
	}

	public void onSkillHit(L2Character target, boolean ownerWasHit, boolean wasMagic, boolean wasOffensive)
	{
		int event;
		if(ownerWasHit)
		{
			event = ChanceCondition.EVT_HIT_BY_SKILL;
			if(wasOffensive)
			{
				event |= ChanceCondition.EVT_HIT_BY_OFFENSIVE_SKILL;
				event |= ChanceCondition.EVT_ATTACKED;
			}
			else
			{
				event |= ChanceCondition.EVT_HIT_BY_GOOD_MAGIC;
			}
		}
		else
		{
			event = ChanceCondition.EVT_CAST;
			event |= wasMagic ? ChanceCondition.EVT_MAGIC : ChanceCondition.EVT_PHYSICAL;
			event |= wasOffensive ? ChanceCondition.EVT_MAGIC_OFFENSIVE : ChanceCondition.EVT_MAGIC_GOOD;
		}

		onEvent(event, target);
	}

	public static boolean canTriggerByCast(L2Character caster, L2Character target, L2Skill trigger)
	{
		switch(trigger.getSkillType())
		{
			case COMMON_CRAFT:
			case DWARVEN_CRAFT:
				return false;
		default:
			break;
		}

		if(trigger.isToggle() || trigger.isPotion())
		{
			return false;
		}

		if(trigger.getId() == 1320)
		{
			return false;
		}

		if(trigger.isOffensive() && !Formulas.calcMagicSuccess(caster, target, trigger))
		{
			return false;
		}

		return true;
	}

	public void onEvent(int event, L2Character target)
	{
		if(_owner.isDead())
		{
			return;
		}

		for(FastMap.Entry<L2Skill, ChanceCondition> e = head(), end = tail(); (e = e.getNext()) != end;)
		{
			if(e.getValue() != null && e.getValue().trigger(event))
			{
				makeCast(e.getKey(), target);
			}
		}
	}

	private void makeCast(L2Skill skill, L2Character target)
	{
		try
		{
			if(skill.getWeaponDependancy(_owner, true))
			{
				if(skill.triggerAnotherSkill())
				{
					skill = _owner._skills.get(skill.getTriggeredId());
					if(skill == null)
					{
						return;
					}
				}

				ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());
				L2Object[] targets = skill.getTargetList(_owner, false, target);

				_owner.broadcastPacket(new MagicSkillLaunched(_owner, skill.getDisplayId(), skill.getLevel(), targets));
				_owner.broadcastPacket(new MagicSkillUser(_owner, (L2Character) targets[0], skill.getDisplayId(), skill.getLevel(), 0, 0));

				if(handler != null)
				{
					handler.useSkill(_owner, skill, targets);
				}
				else
				{
					skill.useSkill(_owner, targets);
				}
			}
		}
		catch(Exception e)
		{
			
		}
	}
}