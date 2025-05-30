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
import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2SummonInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.Env;
import com.src.gameserver.skills.Formulas;
import com.src.gameserver.skills.Stats;
import com.src.gameserver.skills.funcs.Func;
import com.src.gameserver.templates.item.L2WeaponType;
import com.src.gameserver.templates.skills.L2SkillType;
import com.src.gameserver.util.Util;

public class Blow implements ISkillHandler
{
    private static final L2SkillType[] SKILL_IDS = { L2SkillType.BLOW };

    private int _successChance;

    public static int FRONT = 50;
    public static int SIDE = 60;
    public static int BEHIND = 70;

    public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
    {
        if(Config.BACKSTABRESTRICTION)
        {
            if(skill.getId() == 30)
            {
                FRONT = 0;
                SIDE = 0;
                BEHIND = 70;
            }
        }

        if(activeChar.isAlikeDead())
        {
            return;
        }

        for(L2Object target2 : targets)
        {
            L2Character target = (L2Character) target2;
            if(target.isAlikeDead())
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

            if(activeChar.isBehindTarget())
            {
                _successChance = Config.BLOW_ATTACK_BEHIND;
            }
            else if(activeChar.isFrontTarget())
            {
                _successChance = Config.BLOW_ATTACK_FRONT;
            }
            else
            {
                _successChance = Config.BLOW_ATTACK_SIDE;
            }

            if(((skill.getCondition() & L2Skill.COND_BEHIND) != 0) && _successChance == Config.BLOW_ATTACK_BEHIND || ((skill.getCondition() & L2Skill.COND_CRIT) != 0) && Formulas.getInstance().calcBlow(activeChar, target, _successChance))
            {
                if(skill.hasEffects())
                {
                    if(target.reflectSkill(skill))
                    {
                        activeChar.stopSkillEffects(skill.getId());
                        skill.getEffects(null, activeChar);
                        activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(skill.getId()));
                    }
                }
                L2ItemInstance weapon = activeChar.getActiveWeaponInstance();
                boolean soul = (weapon != null && weapon.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT && weapon.getItemType() == L2WeaponType.DAGGER);
                boolean shld = Formulas.calcShldUse(activeChar, target);

                boolean crit = false;
                if(Formulas.calcCrit(skill.getBaseCritRate() * 10 * Formulas.getInstance().getSTRBonus(activeChar)))
                {
                    crit = true;
                }

                double damage = (int) Formulas.calcBlowDamage(activeChar, target, skill, shld, soul);
                if(crit)
                {
                    damage *= 2;
                    L2Effect vicious = activeChar.getFirstEffect(312);
                    if(vicious != null && damage > 1)
                    {
                        for(Func func : vicious.getStatFuncs())
                        {
                            Env env = new Env();
                            env.player = activeChar;
                            env.target = target;
                            env.skill = skill;
                            env.value = damage;
                            func.calc(env);
                            damage = (int) env.value;
                        }
                    }
                }

                if(soul && weapon != null)
                {
                    weapon.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
                }

                if(skill.getDmgDirectlyToHP() && target instanceof L2PcInstance)
                {
                    L2PcInstance player = (L2PcInstance) target;
                    if(!player.isInvul())
                    {
                        L2Summon summon = player.getPet();
                        if(summon != null && summon instanceof L2SummonInstance && Util.checkIfInRange(900, player, summon, true))
                        {
                            int tDmg = (int) damage * (int) player.getStat().calcStat(Stats.TRANSFER_DAMAGE_PERCENT, 0, null, null) / 100;

                            if(summon.getCurrentHp() < tDmg)
                            {
                                tDmg = (int) summon.getCurrentHp() - 1;
                            }

                            if(tDmg > 0)
                            {
                                summon.reduceCurrentHp(tDmg, activeChar);
                                damage -= tDmg;
                            }
                        }
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
                    player.sendPacket(new SystemMessage(SystemMessageId.S1_GAVE_YOU_S2_DMG).addString(activeChar.getName()).addNumber((int) damage));
                }
                else
                {
                    target.reduceCurrentHp(damage, activeChar);
                }

                if(activeChar instanceof L2PcInstance)
                {
                    activeChar.sendPacket(new SystemMessage(SystemMessageId.CRITICAL_HIT));
                }

                activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_DID_S1_DMG).addNumber((int) damage));
            }

            Formulas.calcLethalHit(activeChar, target, skill);

            L2Effect effect = activeChar.getFirstEffect(skill.getId());

            if(effect != null && effect.isSelfEffect())
            {
                effect.exit();
            }
            skill.getEffectsSelf(activeChar);
        }
    }

    public L2SkillType[] getSkillIds()
    {
        return SKILL_IDS;
    }
}