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
package com.src.gameserver.model.actor.instance;

import java.util.concurrent.Future;

import javolution.util.FastMap;

import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.templates.skills.L2SkillType;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.random.Rnd;

public final class L2BabyPetInstance extends L2PetInstance
{
	protected L2Skill _weakHeal;
	protected L2Skill _strongHeal;
	private Future<?> _healingTask;

	public L2BabyPetInstance(int objectId, L2NpcTemplate template, L2PcInstance owner, L2ItemInstance control)
	{
		super(objectId, template, owner, control);

		FastMap<Integer, L2Skill> skills = (FastMap<Integer, L2Skill>) getTemplate().getSkills();
		L2Skill skill1 = null;
		L2Skill skill2 = null;

		for(L2Skill skill : skills.values())
		{
			if(skill.isActive() && skill.getTargetType() == L2Skill.SkillTargetType.TARGET_OWNER_PET && (skill.getSkillType() == L2SkillType.HEAL || skill.getSkillType() == L2SkillType.HOT || skill.getSkillType() == L2SkillType.BALANCE_LIFE || skill.getSkillType() == L2SkillType.HEAL_PERCENT || skill.getSkillType() == L2SkillType.HEAL_STATIC || skill.getSkillType() == L2SkillType.COMBATPOINTHEAL || skill.getSkillType() == L2SkillType.COMBATPOINTPERCENTHEAL || skill.getSkillType() == L2SkillType.CPHOT || skill.getSkillType() == L2SkillType.MANAHEAL || skill.getSkillType() == L2SkillType.MANA_BY_LEVEL || skill.getSkillType() == L2SkillType.MANAHEAL_PERCENT || skill.getSkillType() == L2SkillType.MANARECHARGE || skill.getSkillType() == L2SkillType.MPHOT))
			{
				if(skill1 == null)
				{
					skill1 = skill;
				}
				else
				{
					skill2 = skill;
					break;
				}
			}
		}
		skills = null;
		if(skill1 != null)
		{
			if(skill2 == null)
			{
				_weakHeal = skill1;
				_strongHeal = skill1;
			}
			else
			{
				if(skill1.getPower() > skill2.getPower())
				{
					_weakHeal = skill2;
					_strongHeal = skill1;
				}
				else
				{
					_weakHeal = skill1;
					_strongHeal = skill2;
				}
				skill2 = null;
			}

			_healingTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Heal(this), 0, 1000);

			skill1 = null;
		}
	}

	@Override
	public boolean doDie(L2Character killer)
	{

		if(!super.doDie(killer))
		{
			return false;
		}

		if(_healingTask != null)
		{
			_healingTask.cancel(false);
			_healingTask = null;
		}
		return true;
	}

	@Override
	public synchronized void unSummon(L2PcInstance owner)
	{
		super.unSummon(owner);

		if(_healingTask != null)
		{
			_healingTask.cancel(false);
			_healingTask = null;
		}
	}

	@Override
	public void doRevive()
	{
		super.doRevive();
		if(_healingTask == null)
		{
			_healingTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new Heal(this), 0, 1000);
		}
	}

	private class Heal implements Runnable
	{
		private L2BabyPetInstance _baby;

		public Heal(L2BabyPetInstance baby)
		{
			_baby = baby;
		}

		@Override
		public void run()
		{
			L2PcInstance owner = _baby.getOwner();

			if(!owner.isDead() && !_baby.isCastingNow() && !_baby.isBetrayed())
			{
				boolean previousFollowStatus = _baby.getFollowStatus();

				if(owner.getCurrentHp() / owner.getMaxHp() < 0.2 && Rnd.get(4) < 3)
				{
					_baby.useMagic(_strongHeal, false, false);
				}
				else if(owner.getCurrentHp() / owner.getMaxHp() < 0.8 && Rnd.get(4) < 1)
				{
					_baby.useMagic(_weakHeal, false, false);
				}

				if(previousFollowStatus != _baby.getFollowStatus())
				{
					setFollowStatus(previousFollowStatus);
				}
			}
			owner = null;
		}
	}

}