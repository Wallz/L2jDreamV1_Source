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
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.skills.L2SkillType;

public class CombatPointHeal implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS = { L2SkillType.COMBATPOINTHEAL, L2SkillType.COMBATPOINTPERCENTHEAL };

	@Override
	public void useSkill(L2Character actChar, L2Skill skill, L2Object[] targets)
	{
		try
		{
			ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(L2SkillType.BUFF);

			if(handler != null)
			{
				handler.useSkill(actChar, skill, targets);
			}

			handler = null;
		}
		catch(Exception e)
		{
		}

		L2Character target = null;

		for(L2Object target2 : targets)
		{
			target = (L2Character) target2;

			double cp = skill.getPower();
			if(skill.getSkillType() == L2SkillType.COMBATPOINTPERCENTHEAL)
			{
				cp = target.getMaxCp() * cp / 100.0;
			}

			target.sendPacket(new SystemMessage(SystemMessageId.S1_CP_WILL_BE_RESTORED).addNumber((int) cp));

			target.setCurrentCp(cp + target.getCurrentCp());
			StatusUpdate sump = new StatusUpdate(target.getObjectId());
			sump.addAttribute(StatusUpdate.CUR_CP, (int) target.getCurrentCp());
			target.sendPacket(sump);
		}
	}

	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}