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

public class L2SkillCharge extends L2Skill
{
	final int numCharges;

	public L2SkillCharge(StatsSet set)
	{
		super(set);
		numCharges = set.getInteger("num_charges", getLevel());
	}

	@Override
	public boolean checkCondition(L2Character activeChar, L2Object target, boolean itemOrWeapon)
	{
		if(activeChar instanceof L2PcInstance)
		{
			EffectCharge e = (EffectCharge)activeChar.getFirstEffect(this);
			if((e != null) && (e.numCharges >= this.numCharges))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.FORCE_MAXLEVEL_REACHED));
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

		EffectCharge effect = null;
		if(caster instanceof L2PcInstance)
		{
			effect = ((L2PcInstance) caster).getChargeEffect();
		}
		else
		{
			effect = (EffectCharge) caster.getFirstEffect(this);
		}

		if(effect != null)
		{
			if(effect.numCharges < getNumCharges())
			{
				effect.numCharges++;
				if(caster instanceof L2PcInstance)
				{
					caster.sendPacket(new EtcStatusUpdate((L2PcInstance) caster));
					caster.sendPacket(new SystemMessage(SystemMessageId.FORCE_INCREASED_TO_S1).addNumber(effect.numCharges));
				}
			}
			else
			{
				caster.sendPacket(new SystemMessage(SystemMessageId.FORCE_MAXIMUM));
			}
			return;
		}

		getEffects(caster, caster);
		getEffectsSelf(caster);
	}

}