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
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.EtcStatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.Formulas;
import com.src.gameserver.skills.effects.EffectCharge;
import com.src.gameserver.templates.StatsSet;
import com.src.gameserver.templates.item.L2WeaponType;

public class L2SkillChargeDmg extends L2Skill
{
	final int chargeSkillId;

	public L2SkillChargeDmg(StatsSet set)
	{
		super(set);
		chargeSkillId = set.getInteger("charge_skill_id");
	}

	@Override
	public boolean checkCondition(L2Character activeChar, L2Object target, boolean itemOrWeapon)
	{
		if(activeChar instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) activeChar;
			EffectCharge e = (EffectCharge) player.getFirstEffect(chargeSkillId);
			if(e == null || e.numCharges < getNumCharges())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(getId()));
				return false;
			}
		}

		return super.checkCondition(activeChar, target, itemOrWeapon);
	}

	@Override
	public void useSkill(L2Character caster, L2Object[] targets)
	{
		if(caster.isAlikeDead())
		{
			return;
		}

		EffectCharge effect = (EffectCharge) caster.getFirstEffect(chargeSkillId);
		if(effect == null || effect.numCharges < getNumCharges())
		{
			caster.sendPacket(new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(getId()));
			return;
		}

		double modifier = 0;

		modifier = 0.8 + 0.201 * effect.numCharges;

		if(getTargetType() != SkillTargetType.TARGET_AREA && getTargetType() != SkillTargetType.TARGET_MULTIFACE)
		{
			effect.numCharges -= getNumCharges();
		}

		if(caster instanceof L2PcInstance)
		{
			caster.sendPacket(new EtcStatusUpdate((L2PcInstance) caster));
		}

		if(effect.numCharges == 0)
		{
			effect.exit();
		}
		for(L2Object target2 : targets)
		{
			L2ItemInstance weapon = caster.getActiveWeaponInstance();
			L2Character target = (L2Character) target2;
			if(target.isAlikeDead())
			{
				continue;
			}

			if(Formulas.calcPhysicalSkillEvasion(target, this))
			{
				caster.sendPacket(new SystemMessage(SystemMessageId.ATTACK_FAILED));
			}
			else
			{
				if(target.vengeanceSkill(this))
				{
					target = caster;
				}

				boolean shld = Formulas.calcShldUse(caster, target);
				boolean crit = Formulas.calcCrit(caster.getCriticalHit(target, this));
				boolean soul = weapon != null && weapon.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT && weapon.getItemType() != L2WeaponType.DAGGER;

				int damage = (int) Formulas.getInstance().calcPhysDam(caster, target, this, shld, false, false, soul);
				if(crit)
				{
					damage *= 2;
				}

				if(damage > 0)
				{
					double finalDamage = damage * modifier;
					target.reduceCurrentHp(finalDamage, caster);

					caster.sendDamageMessage(target, (int) finalDamage, false, crit, false);

					if(soul && weapon != null)
					{
						weapon.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
					}
				}
				else
				{
					caster.sendDamageMessage(target, 0, false, false, true);
				}
			}
		}

		L2Effect seffect = caster.getFirstEffect(getId());
		if(seffect != null && seffect.isSelfEffect())
		{
			seffect.exit();
		}

		getEffectsSelf(caster);
	}

}