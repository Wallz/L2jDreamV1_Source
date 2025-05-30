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

import com.src.gameserver.ai.CtrlEvent;
import com.src.gameserver.handler.ISkillHandler;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2MonsterInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.Formulas;
import com.src.gameserver.templates.skills.L2SkillType;

public class Spoil implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS = { L2SkillType.SPOIL };

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if(!(activeChar instanceof L2PcInstance))
		{
			return;
		}

		if(targets == null)
		{
			return;
		}

		for(int index = 0; index < targets.length; index++)
		{
			if(!(targets[index] instanceof L2MonsterInstance))
			{
				continue;
			}

			L2MonsterInstance target = (L2MonsterInstance) targets[index];

			if(target.isSpoil())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.ALREADY_SPOILED));
				continue;
			}

			boolean spoil = false;
			if(target.isDead() == false)
			{
				spoil = Formulas.calcMagicSuccess(activeChar, (L2Character) targets[index], skill);

				if(spoil)
				{
					target.setSpoil(true);
					target.setIsSpoiledBy(activeChar.getObjectId());
					activeChar.sendPacket(new SystemMessage(SystemMessageId.SPOIL_SUCCESS));
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2).addString(target.getName()).addSkillName(skill.getDisplayId()));
				}

				target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);
			}
		}
	}

	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}