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

import java.util.List;

import javolution.util.FastList;

import com.src.gameserver.handler.ISkillHandler;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.L2Skill.SkillTargetType;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.Formulas;
import com.src.gameserver.taskmanager.DecayTaskManager;
import com.src.gameserver.templates.skills.L2SkillType;

public class Resurrect implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS = { L2SkillType.RESURRECT };

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		L2PcInstance player = null;
		if(activeChar instanceof L2PcInstance)
		{
			player = (L2PcInstance) activeChar;
		}

		L2Character target = null;
		L2PcInstance targetPlayer;
		List<L2Character> targetToRes = new FastList<L2Character>();

		for(L2Object target2 : targets)
		{
			target = (L2Character) target2;
			if(target instanceof L2PcInstance)
			{
				targetPlayer = (L2PcInstance) target;

				if(skill.getTargetType() == SkillTargetType.TARGET_CORPSE_CLAN)
				{
					if(player != null && player.getClanId() != targetPlayer.getClanId())
					{
						continue;
					}
				}

				targetPlayer = null;
			}

			if(target.isVisible())
			{
				targetToRes.add(target);
			}
		}

		if(targetToRes.size() == 0)
		{
			activeChar.abortCast();
			activeChar.sendPacket(SystemMessage.sendString("No valid target to resurrect"));
		}

		for(L2Character cha : targetToRes)
		{
			if(activeChar instanceof L2PcInstance)
			{
				if(cha instanceof L2PcInstance)
				{
					((L2PcInstance) cha).reviveRequest((L2PcInstance) activeChar, skill, false);
				}
				else if(cha instanceof L2PetInstance)
				{
					if(((L2PetInstance) cha).getOwner() == activeChar)
					{
						cha.doRevive(Formulas.getInstance().calculateSkillResurrectRestorePercent(skill.getPower(), activeChar.getWIT()));
					}
					else
					{
						((L2PetInstance) cha).getOwner().reviveRequest((L2PcInstance) activeChar, skill, true);
					}
				}
				else
				{
					cha.doRevive(Formulas.getInstance().calculateSkillResurrectRestorePercent(skill.getPower(), activeChar.getWIT()));
				}
			}
			else
			{
				DecayTaskManager.getInstance().cancelDecayTask(cha);
				cha.doRevive(Formulas.getInstance().calculateSkillResurrectRestorePercent(skill.getPower(), activeChar.getWIT()));
			}
		}
	}

	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}