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

import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.EtcStatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.effects.EffectCharge;
import com.src.gameserver.templates.StatsSet;

public class L2SkillChargeEffect extends L2Skill
{
	final int chargeSkillId;

	public L2SkillChargeEffect(StatsSet set)
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
	public void useSkill(L2Character activeChar, L2Object[] targets)
	{
		if(activeChar.isAlikeDead())
		{
			return;
		}

		EffectCharge effect = (EffectCharge) activeChar.getFirstEffect(chargeSkillId);
		if(effect == null || effect.numCharges < getNumCharges())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(getId()));
			return;
		}

		effect.numCharges -= getNumCharges();

		if(effect.numCharges == 0)
		{
			effect.exit();
		}

		if(hasEffects())
		{
			for(L2Object target : targets)
			{
				getEffects(activeChar, (L2Character) target);
			}
		}
		if(activeChar instanceof L2PcInstance)
		{
			activeChar.sendPacket(new EtcStatusUpdate((L2PcInstance) activeChar));
		}
	}

}