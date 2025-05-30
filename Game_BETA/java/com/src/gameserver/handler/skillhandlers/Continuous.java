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
import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.handler.ISkillHandler;
import com.src.gameserver.managers.DuelManager;
import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.instance.L2DoorInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.Formulas;
import com.src.gameserver.templates.skills.L2SkillType;
import com.src.util.random.Rnd;

public class Continuous implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.BUFF,
		L2SkillType.DEBUFF,
		L2SkillType.DOT,
		L2SkillType.MDOT,
		L2SkillType.POISON,
		L2SkillType.BLEED,
		L2SkillType.HOT,
		L2SkillType.CPHOT,
		L2SkillType.MPHOT,
		L2SkillType.FEAR,
		L2SkillType.CONT,
		L2SkillType.WEAKNESS,
		L2SkillType.REFLECT,
		L2SkillType.UNDEAD_DEFENSE,
		L2SkillType.AGGDEBUFF,
		L2SkillType.FORCE_BUFF
	};

	private L2Skill _skill;

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if(activeChar == null)
		{
			return;
		}

		L2Character target = null;
		L2PcInstance player = null;
		if(activeChar instanceof L2PcInstance)
		{
			player = (L2PcInstance) activeChar;
		}

		if(skill.getEffectId() != 0)
		{
			int skillLevel = skill.getEffectLvl();
			int skillEffectId = skill.getEffectId();
			if(skillLevel == 0)
			{
				_skill = SkillTable.getInstance().getInfo(skillEffectId, 1);
			}
			else
			{
				_skill = SkillTable.getInstance().getInfo(skillEffectId, skillLevel);
			}

			if(_skill != null)
			{
				skill = _skill;
			}
		}

		for(L2Object target2 : targets)
		{
			target = (L2Character) target2;

			if(target == null)
			{
				continue;
			}

			if(target instanceof L2PcInstance && activeChar instanceof L2Playable && skill.isOffensive())
			{
				L2PcInstance _char = (activeChar instanceof L2PcInstance)?(L2PcInstance)activeChar:((L2Summon)activeChar).getOwner();
				L2PcInstance _attacked = (L2PcInstance) target;
				if(_attacked.getClanId() != 0 && _char.getClanId() != 0 && _attacked.getClanId() == _char.getClanId() && _attacked.getPvpFlag() == 0)
				{
					continue;
				}

				if(_attacked.getAllyId() != 0 && _char.getAllyId() != 0 && _attacked.getAllyId() == _char.getAllyId() && _attacked.getPvpFlag() == 0)
				{
					continue;
				}
			}

			if(skill.getSkillType() != L2SkillType.BUFF && skill.getSkillType() != L2SkillType.HOT && skill.getSkillType() != L2SkillType.CPHOT && skill.getSkillType() != L2SkillType.MPHOT && skill.getSkillType() != L2SkillType.UNDEAD_DEFENSE && skill.getSkillType() != L2SkillType.AGGDEBUFF && skill.getSkillType() != L2SkillType.CONT)
			{
				if(target.reflectSkill(skill))
				{
					target = activeChar;
				}
			}

			if(target instanceof L2DoorInstance && (skill.getSkillType() == L2SkillType.BUFF || skill.getSkillType() == L2SkillType.HOT))
			{
				continue;
			}

			if(target != activeChar && target.isBuffProtected() && !skill.isHeroSkill() && (skill.getSkillType() == L2SkillType.BUFF || skill.getSkillType() == L2SkillType.HEAL_PERCENT || skill.getSkillType() == L2SkillType.FORCE_BUFF || skill.getSkillType() == L2SkillType.MANAHEAL_PERCENT || skill.getSkillType() == L2SkillType.COMBATPOINTHEAL || skill.getSkillType() == L2SkillType.REFLECT))
			{
				continue;
			}

			if(skill.getSkillType() == L2SkillType.BUFF)
			{
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
			}

			if(!target.isRaid() && !(target instanceof L2Npc && ((L2Npc) target).getNpcId() == 35062))
			{
				int chance = Rnd.get(100);
				Formulas.getInstance();
				if(skill.getLethalChance2() > 0 && chance < Formulas.calcLethal(activeChar, target, skill.getLethalChance2(), chance))
				{
					if(target instanceof L2Npc)
					{
						target.reduceCurrentHp(target.getCurrentHp() - 1, activeChar);
						activeChar.sendPacket(new SystemMessage(SystemMessageId.LETHAL_STRIKE));
					}
				}
				else
				{
					Formulas.getInstance();
					if(skill.getLethalChance1() > 0 && chance < Formulas.calcLethal(activeChar, target, skill.getLethalChance1(), chance))
					{
						if(target instanceof L2Npc)
						{
							target.reduceCurrentHp(target.getCurrentHp() / 2, activeChar);
							activeChar.sendPacket(new SystemMessage(SystemMessageId.LETHAL_STRIKE));
						}
					}
				}
			}

			if(skill.isOffensive())
			{
				boolean ss = false;
				boolean sps = false;
				boolean bss = false;

				if(player != null)
				{
					L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
					if(weaponInst != null)
					{
						if(skill.isMagic())
						{
							if(weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
							{
								bss = true;
								if(skill.getId() != 1020)
								{
									weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
								}
							}
							else if(weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT)
							{
								sps = true;
								if(skill.getId() != 1020)
								{
									weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
								}
							}
						}
						else if(weaponInst.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT)
						{
							ss = true;
							if(skill.getId() != 1020)
							{
								weaponInst.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
							}
						}
					}
				}
				else if(activeChar instanceof L2Summon)
				{
					L2Summon activeSummon = (L2Summon) activeChar;
					if(skill.isMagic())
					{
						if(activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
						{
							bss = true;
							activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
						}
						else if(activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_SPIRITSHOT)
						{
							sps = true;
							activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
						}
					}
					else if(activeSummon.getChargedSoulShot() == L2ItemInstance.CHARGED_SOULSHOT)
					{
						ss = true;
						activeSummon.setChargedSoulShot(L2ItemInstance.CHARGED_NONE);
					}
				}

				boolean acted = Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss);

				if(!acted)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.ATTACK_FAILED));
					continue;
				}

			}
			else if(skill.getSkillType() == L2SkillType.BUFF)
			{
				if(!Formulas.getInstance().calcBuffSuccess(target, skill))
				{
					if(player!=null)
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2).addString(target.getName()).addSkillName(skill.getDisplayId()));
						continue;
					}
					else
					{
						continue;
					}
				}
			}
			boolean stopped = false;

			L2Effect[] effects = target.getAllEffects();
			if(effects != null)
			{
				for(L2Effect e : effects)
				{
					if(e != null && skill != null)
					{
						if(e.getSkill().getId() == skill.getId())
						{
							e.exit();
							stopped = true;
						}
					}
				}
			}

			if(skill.isToggle() && stopped)
			{
				return;
			}

			if(target == null || target instanceof L2PcInstance && ((L2PcInstance)target).isOnline()==0)
			{
				continue;
			}

			if(target instanceof L2PcInstance && player != null && ((L2PcInstance) target).isInDuel() && (skill.getSkillType() == L2SkillType.DEBUFF || skill.getSkillType() == L2SkillType.BUFF) && player.getDuelId() == ((L2PcInstance) target).getDuelId())
			{
				DuelManager dm = DuelManager.getInstance();
				if(dm != null && skill != null)
				{
					effects = skill.getEffects(activeChar, target);
					if(effects != null)
					{
						for(L2Effect buff : effects)
						{
							if(buff != null)
							{
								dm.onBuff(((L2PcInstance) target), buff);
							}
						}
					}
				}
			}
			else
			{
				skill.getEffects(activeChar, target);
			}

			if(skill.getSkillType() == L2SkillType.AGGDEBUFF)
			{
				if(target instanceof L2Attackable)
				{
					target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, activeChar, (int) skill.getPower());
				}
				else if(target instanceof L2Playable)
				{
					if(target.getTarget() == activeChar)
					{
						target.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, activeChar);
					}
					else
					{
						target.setTarget(activeChar);
					}
				}
			}

			if(target.isDead() && skill.getTargetType() == L2Skill.SkillTargetType.TARGET_AREA_CORPSE_MOB && target instanceof L2Npc)
			{
				((L2Npc) target).endDecayTask();
			}

			Formulas.calcLethalHit(activeChar, target, skill);
		}

		L2Effect effect = activeChar.getFirstEffect(skill.getId());
		if(effect != null && effect.isSelfEffect())
		{
			effect.exit();
		}
		skill.getEffectsSelf(activeChar);
	}

	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}