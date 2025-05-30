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
import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2RaidBossInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.Formulas;
import com.src.gameserver.templates.skills.L2SkillType;
import com.src.logs.Log;

public class Mdam implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.MDAM,
		L2SkillType.DEATHLINK
	};

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if(activeChar.isAlikeDead())
		{
			return;
		}

		boolean ss = false;
		boolean bss = false;

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

			activeSummon = null;
		}

		weaponInst = null;

		for(L2Object target2 : targets)
		{
			L2Character target = (L2Character) target2;

			if(activeChar instanceof L2PcInstance && target instanceof L2PcInstance && target.isAlikeDead() && target.isFakeDeath())
			{
				target.stopFakeDeath(null);
			}
			else if(target.isAlikeDead())
			{
				if(skill.getTargetType() == L2Skill.SkillTargetType.TARGET_AREA_CORPSE_MOB && target instanceof L2Npc)
				{
					((L2Npc) target).endDecayTask();
				}

				continue;
			}

			boolean mcrit = Formulas.calcMCrit(activeChar.getMCriticalHit(target, skill));

			int damage = (int) Formulas.calcMagicDam(activeChar, target, skill, ss, bss, mcrit);

			if(damage > 5000 && activeChar instanceof L2PcInstance)
			{
				String name = "";
				if(target instanceof L2RaidBossInstance)
				{
					name = "RaidBoss ";
				}

				if(target instanceof L2Npc)
				{
					name += target.getName() + "(" + ((L2Npc) target).getTemplate().npcId + ")";
				}

				if(target instanceof L2PcInstance)
				{
					name = target.getName() + "(" + target.getObjectId() + ") ";
				}

				name += target.getLevel() + " lvl";
				Log.add(activeChar.getName() + "(" + activeChar.getObjectId() + ") " + activeChar.getLevel() + " lvl did damage " + damage + " with skill " + skill.getName() + "(" + skill.getId() + ") to " + name, "damage_mdam");
			}

			if(damage > 0)
			{
				if(!target.isRaid() && Formulas.calcAtkBreak(target, damage))
				{
					target.breakAttack();
					target.breakCast();
				}

				activeChar.sendDamageMessage(target, damage, mcrit, false, false);

				if(skill.hasEffects())
				{
					if(target.reflectSkill(skill))
					{
						activeChar.stopSkillEffects(skill.getId());
						skill.getEffects(null, activeChar);
						activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(skill.getId()));
					}
					else
					{
						if(Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, false, ss, bss))
						{
							target.stopSkillEffects(skill.getId());
							skill.getEffects(activeChar, target);
						}
						else
						{
							activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2).addString(target.getName()).addSkillName(skill.getDisplayId()));
						}
					}
				}

				target.reduceCurrentHp(damage, activeChar);
			}

			Formulas.calcLethalHit(activeChar, target, skill);
		}

		L2Effect effect = activeChar.getFirstEffect(skill.getId());
		if(effect != null && effect.isSelfEffect())
		{
			effect.exit();
		}
		skill.getEffectsSelf(activeChar);

		if(skill.isSuicideAttack())
		{
			activeChar.doDie(null);
			activeChar.setCurrentHp(0);
		}
	}

	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}