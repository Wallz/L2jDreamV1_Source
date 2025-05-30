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

import com.src.Config;
import com.src.gameserver.handler.ISkillHandler;
import com.src.gameserver.handler.SkillHandler;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.instance.L2DoorInstance;
import com.src.gameserver.model.actor.instance.L2GrandBossInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2RaidBossInstance;
import com.src.gameserver.model.actor.instance.L2SiegeFlagInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.Stats;
import com.src.gameserver.templates.skills.L2SkillType;

public class Heal implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS = { L2SkillType.HEAL, L2SkillType.HEAL_PERCENT, L2SkillType.HEAL_STATIC };

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

			handler = null;
		}
		catch(Exception e)
		{
		}

		L2Character target = null;
		L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();

		L2PcInstance player = null;
		if(activeChar instanceof L2PcInstance)
		{
			player = (L2PcInstance) activeChar;
		}
		boolean clearSpiritShot = false;

		for(L2Object target2 : targets)
		{
			target = (L2Character) target2;

			if(target == null || target.isDead() || target.isInvul())
			{
				continue;
			}

			if(target instanceof L2DoorInstance || target instanceof L2SiegeFlagInstance)
			{
				continue;
			}

			if(!Config.PLAYERS_CAN_HEAL_RB && player != null && !player.isGM() && target instanceof L2RaidBossInstance)
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				continue;
			}

			if(!Config.PLAYERS_CAN_HEAL_RB && player != null && !player.isGM() && target instanceof L2GrandBossInstance)
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
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

			double hp = skill.getPower();

			if(skill.getSkillType() == L2SkillType.HEAL_PERCENT)
			{
				hp = target.getMaxHp() * hp / 100.0;
			}
			else
			{
				if(weaponInst != null)
				{
					if(weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
					{
						hp *= 1.5;
						clearSpiritShot = true;
					}
					else if(weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT)
					{
						hp *= 1.3;
						clearSpiritShot = true;
					}
				}

				else if(activeChar instanceof L2Summon)
				{
					L2Summon activeSummon = (L2Summon) activeChar;

					if(activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
					{
						hp *= 1.5;
						clearSpiritShot = true;
					}
					else if(activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_SPIRITSHOT)
					{
						hp *= 1.3;
						clearSpiritShot = true;
					}

					activeSummon = null;
				}
			}

			if(skill.getSkillType() == L2SkillType.HEAL_STATIC)
			{
				hp = skill.getPower();
			}
			else if(skill.getSkillType() != L2SkillType.HEAL_PERCENT)
			{
				hp *= target.calcStat(Stats.HEAL_EFFECTIVNESS, 100, null, null) / 100;
			}

			if (target.getCurrentHp()+hp >= target.getMaxHp()) 
			{ 
				hp = target.getMaxHp()- target.getCurrentHp(); 
			} 
			
			if (hp < 0) 
			{ 
				hp = 0;  
			}
				 	
			target.setCurrentHp(hp + target.getCurrentHp());
			target.setLastHealAmount((int) hp);
			StatusUpdate su = new StatusUpdate(target.getObjectId());
			su.addAttribute(StatusUpdate.CUR_HP, (int) target.getCurrentHp());
			target.sendPacket(su);

			if(target instanceof L2PcInstance)
			{
				if(skill.getId() == 4051)
				{
					target.sendPacket(new SystemMessage(SystemMessageId.REJUVENATING_HP));
				}
				else
				{
					if(activeChar instanceof L2PcInstance && activeChar != target)
					{
						target.sendPacket(new SystemMessage(SystemMessageId.S2_HP_RESTORED_BY_S1).addString(activeChar.getName()).addNumber((int) hp));
					}
					else
					{
						target.sendPacket(new SystemMessage(SystemMessageId.S1_HP_RESTORED).addNumber((int) hp));
					}
				}
			}
		}
		if(clearSpiritShot)
		{
			if(activeChar instanceof L2Summon)
			{
				L2Summon activeSummon = (L2Summon) activeChar;
				activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
				activeSummon = null;
			}
			else
			{
				if(weaponInst != null)
				{
					weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
				}
			}
		}
	}

	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}