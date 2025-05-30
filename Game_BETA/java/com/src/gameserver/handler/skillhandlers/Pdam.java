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

import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.handler.ISkillHandler;
import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2RaidBossInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.EtcStatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.Formulas;
import com.src.gameserver.skills.effects.EffectCharge;
import com.src.gameserver.templates.item.L2WeaponType;
import com.src.gameserver.templates.skills.L2EffectType;
import com.src.gameserver.templates.skills.L2SkillType;
import com.src.logs.Log;

public class Pdam implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.PDAM,
		L2SkillType.FATALCOUNTER
	};

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if(activeChar.isAlikeDead())
		{
			return;
		}

		int damage = 0;

		for(L2Object target2 : targets)
		{
			L2Character target = (L2Character) target2;

			L2ItemInstance weapon = activeChar.getActiveWeaponInstance();

			if(activeChar instanceof L2PcInstance && target instanceof L2PcInstance && target.isAlikeDead() && target.isFakeDeath())
			{
				target.stopFakeDeath(null);
			}
			else if(target.isAlikeDead())
			{
				continue;
			}

			if(target.isInvul())
			{
				continue;
			}

			Formulas.getInstance();
			if(Formulas.calcPhysicalSkillEvasion(target, skill))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.ATTACK_FAILED));
				continue;
			}

			if(target.vengeanceSkill(skill))
			{
				target = activeChar;
			}

			boolean dual = activeChar.isUsingDualWeapon();
			boolean shld = Formulas.calcShldUse(activeChar, target);
			boolean crit = false;
			if(skill.getBaseCritRate() > 0)
			{
				crit = Formulas.calcCrit(skill.getBaseCritRate() * 10 * Formulas.getInstance().getSTRBonus(activeChar));
			}

			boolean soul = (weapon != null && weapon.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT && weapon.getItemType() != L2WeaponType.DAGGER);

			if(!crit && (skill.getCondition() & L2Skill.COND_CRIT) != 0)
			{
				damage = 0;
			}
			else
			{
				damage = (int) Formulas.getInstance().calcPhysDam(activeChar, target, skill, shld, false, dual, soul);
			}

			if(crit)
			{
				damage *= 2;
			}

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
				Log.add(activeChar.getName() + "(" + activeChar.getObjectId() + ") " + activeChar.getLevel() + " lvl did damage " + damage + " with skill " + skill.getName() + "(" + skill.getId() + ") to " + name, "damage_pdam");
			}

			if(soul && weapon != null)
			{
				weapon.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
			}

			if(damage > 0)
			{
				activeChar.sendDamageMessage(target, damage, false, crit, false);

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
						if(Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, soul, false, false))
						{
							target.stopSkillEffects(skill.getId());
							skill.getEffects(activeChar, target);
							activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(skill.getId()));
						}
						else
						{
							activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2).addString(target.getName()).addSkillName(skill.getDisplayId()));
						}
					}
				}

				boolean lethal = Formulas.calcLethalHit(activeChar, target, skill);

				if(!lethal && skill.getDmgDirectlyToHP() || !(activeChar instanceof L2Playable))
				{
					if(target instanceof L2PcInstance)
					{
						L2PcInstance player = (L2PcInstance)target;
						if(!player.isInvul())
						{
							if(damage >= player.getCurrentHp())
							{
								if(player.isInDuel())
								{
									player.setCurrentHp(1);
								}
								else
								{
									player.setCurrentHp(0);
									if(player.isInOlympiadMode())
									{
										player.abortAttack();
										player.abortCast();
										player.getStatus().stopHpMpRegeneration();
									}
									else
									{
										player.doDie(activeChar);
									}
								}
							}
							else
							{
								player.setCurrentHp(player.getCurrentHp() - damage);
							}
						}

						player.sendPacket(new SystemMessage(SystemMessageId.S1_GAVE_YOU_S2_DMG).addString(activeChar.getName()).addNumber(damage));
					}
					else
					{
						target.reduceCurrentHp(damage, activeChar);
					}
				}
				else
				{
					target.reduceCurrentHp(damage, activeChar);
				}
			}
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.ATTACK_FAILED));
			}

			if(skill.getId() == 345 || skill.getId() == 346)
			{
				EffectCharge effect = (EffectCharge) activeChar.getFirstEffect(L2EffectType.CHARGE);
				if(effect != null)
				{
					int effectcharge = effect.getLevel();
					if(effectcharge < 7)
					{
						effectcharge++;
						effect.addNumCharges(1);
						if(activeChar instanceof L2PcInstance)
						{
							activeChar.sendPacket(new EtcStatusUpdate((L2PcInstance) activeChar));
							activeChar.sendPacket(new SystemMessage(SystemMessageId.FORCE_INCREASED_TO_S1).addNumber(effectcharge));
						}
					}
					else
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.FORCE_MAXLEVEL_REACHED));
					}
				}
				else
				{
					if(skill.getId() == 345)
					{
						L2Skill dummy = SkillTable.getInstance().getInfo(8, 7);
						dummy.getEffects(activeChar, activeChar);
						dummy = null;
					}
					else if(skill.getId() == 346)
					{
						L2Skill dummy = SkillTable.getInstance().getInfo(50, 7);
						dummy.getEffects(activeChar, activeChar);
						dummy = null;
					}
				}
			}
			L2Effect effect = activeChar.getFirstEffect(skill.getId());
			if(effect != null && effect.isSelfEffect())
			{
				effect.exit();
			}

			skill.getEffectsSelf(activeChar);
		}

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