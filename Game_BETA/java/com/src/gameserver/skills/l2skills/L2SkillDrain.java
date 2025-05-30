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
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.Formulas;
import com.src.gameserver.templates.StatsSet;

public class L2SkillDrain extends L2Skill
{
	private float _absorbPart;
	private int _absorbAbs;

	public L2SkillDrain(StatsSet set)
	{
		super(set);

		_absorbPart = set.getFloat("absorbPart", 0.f);
		_absorbAbs = set.getInteger("absorbAbs", 0);
	}

	@Override
	public void useSkill(L2Character activeChar, L2Object[] targets)
	{
		if(activeChar.isAlikeDead())
		{
			return;
		}

		boolean ss = false;
		boolean bss = false;

		for(L2Object target2 : targets)
		{
			L2Character target = (L2Character) target2;
			if(target.isAlikeDead() && getTargetType() != SkillTargetType.TARGET_CORPSE_MOB)
			{
				continue;
			}

			if(activeChar != target && target.isInvul())
			{
				continue;
			}

			L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();

			if(weaponInst != null)
			{
				if(weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
				{
					bss = true;
					weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
				}
				else if(weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT)
				{
					ss = true;
					weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
				}
			}
			else if(activeChar instanceof L2Summon)
			{
				L2Summon activeSummon = (L2Summon) activeChar;

				if(activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
				{
					bss = true;
					activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
				}
				else if(activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_SPIRITSHOT)
				{
					ss = true;
					activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
				}
			}

			boolean mcrit = Formulas.calcMCrit(activeChar.getMCriticalHit(target, this));
			int damage = (int) Formulas.calcMagicDam(activeChar, target, this, ss, bss, mcrit);

			int _drain = 0;
			int _cp = (int) target.getStatus().getCurrentCp();
			int _hp = (int) target.getStatus().getCurrentHp();

			if(_cp > 0)
			{
				if(damage < _cp)
				{
					_drain = 0;
				}
				else
				{
					_drain = damage - _cp;
				}
			}
			else if(damage > _hp)
			{
				_drain = _hp;
			}
			else
			{
				_drain = damage;
			}

			double hpAdd = _absorbAbs + _absorbPart * _drain;
			double hp = activeChar.getCurrentHp() + hpAdd > activeChar.getMaxHp() ? activeChar.getMaxHp() : activeChar.getCurrentHp() + hpAdd;

			activeChar.setCurrentHp(hp);

			StatusUpdate suhp = new StatusUpdate(activeChar.getObjectId());
			suhp.addAttribute(StatusUpdate.CUR_HP, (int) hp);
			activeChar.sendPacket(suhp);

			if(damage > 0 && (!target.isDead() || getTargetType() != SkillTargetType.TARGET_CORPSE_MOB))
			{
				if(!target.isRaid() && Formulas.calcAtkBreak(target, damage))
				{
					target.breakAttack();
					target.breakCast();
				}

				activeChar.sendDamageMessage(target, damage, mcrit, false, false);

				if(hasEffects() && getTargetType() != SkillTargetType.TARGET_CORPSE_MOB)
				{
					if(target.reflectSkill(this))
					{
						activeChar.stopSkillEffects(getId());
						getEffects(null, activeChar);
						activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(getId()));
					}
					else
					{
						target.stopSkillEffects(getId());
						if(Formulas.getInstance().calcSkillSuccess(activeChar, target, this, false, ss, bss))
						{
							getEffects(activeChar, target);
						}
						else
						{
							activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2).addString(target.getName()).addSkillName(getDisplayId()));
						}
					}
				}
				target.reduceCurrentHp(damage, activeChar);
			}

			if(target.isDead() && getTargetType() == SkillTargetType.TARGET_CORPSE_MOB && target instanceof L2Npc)
			{
				((L2Npc) target).endDecayTask();
			}
		}
		L2Effect effect = activeChar.getFirstEffect(getId());
		if(effect != null && effect.isSelfEffect())
		{
			effect.exit();
		}
		getEffectsSelf(activeChar);
	}

}