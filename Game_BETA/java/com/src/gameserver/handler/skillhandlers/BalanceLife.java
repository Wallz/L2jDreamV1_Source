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
package com.src.gameserver.handler.skillhandlers;

import com.src.gameserver.handler.ISkillHandler;
import com.src.gameserver.handler.SkillHandler;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.skills.L2SkillType;

public class BalanceLife implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS = { L2SkillType.BALANCE_LIFE };

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		try
		{
			ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(L2SkillType.BUFF);

			if(handler != null)
			{
				handler.useSkill(activeChar, skill, targets);
			}
		}
		catch(Exception e)
		{
		}

		L2Character target = null;

		L2PcInstance player = null;
		if(activeChar instanceof L2PcInstance)
		{
			player = (L2PcInstance) activeChar;
		}

		double fullHP = 0;
		double currentHPs = 0;

		for(L2Object target2 : targets)
		{
			target = (L2Character) target2;

			if(target == null || target.isDead())
			{
				continue;
			}

			if(target != activeChar)
			{
				if(target instanceof L2PcInstance && ((L2PcInstance) target).isCursedWeaponEquiped())
				{
					continue;
				}
				else if(player != null && player.isCursedWeaponEquiped())
				{
					continue;
				}
			}

			player = null;

			fullHP += target.getMaxHp();
			currentHPs += target.getCurrentHp();
		}
		double percentHP = currentHPs / fullHP;

		for(L2Object target2 : targets)
		{
			target = (L2Character) target2;

			if(target == null || target.isDead())
			{
				continue;
			}

			double newHP = target.getMaxHp() * percentHP;
			double totalHeal = newHP - target.getCurrentHp();

			target.setCurrentHp(newHP);

			if(totalHeal > 0)
			{
				target.setLastHealAmount((int) totalHeal);
			}

			StatusUpdate su = new StatusUpdate(target.getObjectId());
			su.addAttribute(StatusUpdate.CUR_HP, (int) target.getCurrentHp());
			target.sendPacket(su);

			target.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("HP of the party has been balanced."));
		}
	}

	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}