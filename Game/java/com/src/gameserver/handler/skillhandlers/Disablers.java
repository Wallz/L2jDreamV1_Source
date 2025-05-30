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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.src.Config;
import com.src.gameserver.ai.CtrlEvent;
import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.ai.L2AttackableAI;
import com.src.gameserver.handler.ISkillHandler;
import com.src.gameserver.handler.SkillHandler;
import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.model.actor.instance.L2SiegeSummonInstance;
import com.src.gameserver.model.base.Experience;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.Formulas;
import com.src.gameserver.skills.Stats;
import com.src.gameserver.templates.skills.L2SkillType;
import com.src.util.random.Rnd;

public class Disablers implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.STUN,
		L2SkillType.ROOT,
		L2SkillType.SLEEP,
		L2SkillType.CONFUSION,
		L2SkillType.AGGDAMAGE,
		L2SkillType.AGGREDUCE,
		L2SkillType.AGGREDUCE_CHAR,
		L2SkillType.AGGREMOVE,
		L2SkillType.UNBLEED,
		L2SkillType.UNPOISON,
		L2SkillType.MUTE,
		L2SkillType.FAKE_DEATH,
		L2SkillType.CONFUSE_MOB_ONLY,
		L2SkillType.NEGATE,
		L2SkillType.CANCEL,
		L2SkillType.CANCEL_DEBUFF,
		L2SkillType.PARALYZE,
		L2SkillType.ERASE,
		L2SkillType.MAGE_BANE,
		L2SkillType.WARRIOR_BANE,
		L2SkillType.BETRAY
	};

	protected static final Logger _log = Logger.getLogger(L2Skill.class.getName());
	private String[] _negateStats = null;
	private float _negatePower = 0.f;
	private int _negateId = 0;

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		L2SkillType type = skill.getSkillType();

		boolean ss = false;
		boolean sps = false;
		boolean bss = false;

		L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();

		if(activeChar instanceof L2PcInstance)
		{
			if(weaponInst == null && skill.isOffensive())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("You must equip a weapon before casting a spell."));
				return;
			}
		}

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

		weaponInst = null;

		for(int index = 0; index < targets.length; index++)
		{
			if(!(targets[index] instanceof L2Character))
			{
				continue;
			}

			L2Character target = (L2Character) targets[index];

			if(target == null || target.isDead())
			{
				continue;
			}

			switch(type)
			{
				case BETRAY:
				{
					if(Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
					{
						skill.getEffects(activeChar, target);
					}
					else
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2).addString(target.getName()).addSkillName(skill.getId()));
					}
					break;
				}
				case FAKE_DEATH:
				{
					skill.getEffects(activeChar, target);
					break;
				}
				case STUN:
				{
					if(Formulas.calcPhysicalSkillEvasion(target, skill))
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.ATTACK_FAILED));
						break;
					}

					if(target.vengeanceSkill(skill))
					{
						target = activeChar;
					}

					if(Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
					{
						skill.getEffects(activeChar, target);
					}
					else
					{
						if(activeChar instanceof L2PcInstance)
						{
							activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2).addString(target.getName()).addSkillName(skill.getDisplayId()));
						}
					}

					break;
				}
				case ROOT:
				{
					if(target.reflectSkill(skill))
					{
						target = activeChar;
					}

					if(Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
					{
						skill.getEffects(activeChar, target);
					}
					else
					{
						if(activeChar instanceof L2PcInstance)
						{
							activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2).addString(target.getName()).addSkillName(skill.getDisplayId()));
						}
					}

					break;
				}
				case SLEEP:
				case PARALYZE:
				{
					if(target.reflectSkill(skill))
					{
						target = activeChar;
					}

					if(Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
					{
						skill.getEffects(activeChar, target);
					}
					else
					{
						if(activeChar instanceof L2PcInstance)
						{
							activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2).addString(target.getName()).addSkillName(skill.getDisplayId()));
						}
					}

					break;
				}
				case CONFUSION:
				case MUTE:
				{
					if(target.reflectSkill(skill))
					{
						target = activeChar;
					}

					if(Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
					{
						skill.getEffects(activeChar, target);
					}
					else
					{
						if(activeChar instanceof L2PcInstance)
						{
							activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2).addString(target.getName()).addSkillName(skill.getDisplayId()));
						}
					}
					break;
				}
				case CONFUSE_MOB_ONLY:
				{
					if(Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
					{
						L2Effect[] effects = target.getAllEffects();
						for(L2Effect e : effects)
						{
							if(e.getSkill().getSkillType() == type)
							{
								e.exit();
							}
						}

						skill.getEffects(activeChar, target);
					}
					else
					{
						if(activeChar instanceof L2PcInstance)
						{
							SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
							activeChar.sendPacket(sm);
						}
					}
				}
				case AGGDAMAGE:
				{
					if(target instanceof L2Attackable)
					{
						target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, activeChar, (int) ((150 * skill.getPower()) / (target.getLevel() + 7)));
					}

					skill.getEffects(activeChar, target);
					break;
				}
				case AGGREDUCE:
				{
					if(target instanceof L2Attackable)
					{
						skill.getEffects(activeChar, target);

						double aggdiff = ((L2Attackable) target).getHating(activeChar) - target.calcStat(Stats.AGGRESSION, ((L2Attackable) target).getHating(activeChar), target, skill);

						if(skill.getPower() > 0)
						{
							((L2Attackable) target).reduceHate(null, (int) skill.getPower());
						}
						else if(aggdiff > 0)
						{
							((L2Attackable) target).reduceHate(null, (int) aggdiff);
						}
					}

					break;
				}
				case AGGREDUCE_CHAR:
				{
					if(Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
					{
						if(target instanceof L2Attackable)
						{
							L2Attackable targ = (L2Attackable) target;
							targ.stopHating(activeChar);
							if(targ.getMostHated() == null)
							{
								((L2AttackableAI) targ.getAI()).setGlobalAggro(-25);
								targ.clearAggroList();
								targ.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
								targ.setWalking();
							}
						}

						skill.getEffects(activeChar, target);
					}
					else
					{
						if(activeChar instanceof L2PcInstance)
						{
							activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2).addString(target.getName()).addSkillName(skill.getId()));
						}
					}

					break;
				}
				case AGGREMOVE:
				{
					if(target instanceof L2Attackable && !target.isRaid())
					{
						if(Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss))
						{
							if(skill.getTargetType() == L2Skill.SkillTargetType.TARGET_UNDEAD)
							{
								if(target.isUndead())
								{
									((L2Attackable) target).reduceHate(null, ((L2Attackable) target).getHating(((L2Attackable) target).getMostHated()));
								}
							}
							else
							{
								((L2Attackable) target).reduceHate(null, ((L2Attackable) target).getHating(((L2Attackable) target).getMostHated()));
							}
						}
						else
						{
							if(activeChar instanceof L2PcInstance)
							{
								activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2).addString(target.getName()).addSkillName(skill.getId()));
							}
						}
					}

					break;
				}
				case UNBLEED:
				{
					negateEffect(target, L2SkillType.BLEED, skill.getPower());
					break;
				}
				case UNPOISON:
				{
					negateEffect(target, L2SkillType.POISON, skill.getPower());
					break;
				}
				case ERASE:
				{
					if(Formulas.getInstance().calcSkillSuccess(activeChar, target, skill, ss, sps, bss) && !(target instanceof L2SiegeSummonInstance) && !(target instanceof L2PetInstance))
					{
						L2PcInstance summonOwner = null;
						L2Summon summonPet = null;
						summonOwner = ((L2Summon) target).getOwner();
						summonPet = summonOwner.getPet();
						summonPet.unSummon(summonOwner);
						SystemMessage sm = new SystemMessage(SystemMessageId.LETHAL_STRIKE);
						summonOwner.sendPacket(sm);
					}
					else
					{
						if(activeChar instanceof L2PcInstance)
						{
							activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2).addString(target.getName()).addSkillName(skill.getId()));
						}
					}

					break;
				}
				case MAGE_BANE:
				{
					for(L2Object t : targets)
					{
						L2Character target1 = (L2Character) t;

						if(target1.reflectSkill(skill))
						{
							target1 = activeChar;
						}

						if(!Formulas.getInstance().calcSkillSuccess(activeChar, target1, skill, ss, sps, bss))
						{
							continue;
						}

						L2Effect[] effects = target1.getAllEffects();

						for(L2Effect e : effects)
						{
							if(e.getSkill().getId() == 1059 || e.getSkill().getId() == 1085 || e.getSkill().getId() == 4356 || e.getSkill().getId() == 4355)
							{
								e.exit();
							}
						}
					}

					break;
				}
				case WARRIOR_BANE:
				{
					for(L2Object t : targets)
					{
						L2Character target1 = (L2Character) t;

						if(target1.reflectSkill(skill))
						{
							target1 = activeChar;
						}

						if(!Formulas.getInstance().calcSkillSuccess(activeChar, target1, skill, ss, sps, bss))
						{
							continue;
						}

						L2Effect[] effects = target1.getAllEffects();

						for(L2Effect e : effects)
						{
							if(e.getSkill().getId() == 1204 || e.getSkill().getId() == 1086 || e.getSkill().getId() == 4342  || e.getSkill().getId() == 4357)
							{
								e.exit();
							}
						}
					}

					break;
				}
				case CANCEL_DEBUFF: 
				{ 
					L2Effect[] effects = target.getAllEffects(); 
					
					if (effects.length == 0 || effects == null) break; 
					
					for (L2Effect e : effects) 
					{ 
						if (e.getSkill().isDebuff()) 
							e.exit(); 
						break; 
					} 
					
					break; 
				}
				case CANCEL:
				{
					if(target.reflectSkill(skill))
						target = activeChar;

					if(skill.getId() == 1056)
                    {
                    	if(target.isInvul())
                        {
                            if(activeChar instanceof L2PcInstance)
                            {
                                SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
                                sm.addString(target.getName());
                                sm.addSkillName(skill.getDisplayId());
                                activeChar.sendPacket(sm);
                                sm = null;
                            }
                            break;
                        }
                    	
                    	if(target.isRaid())
						{
							if(activeChar instanceof L2PcInstance)
							{
								SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
								sm.addString(target.getName());
								sm.addSkillName(skill.getDisplayId());
								activeChar.sendPacket(sm);
								sm = null;
							}
							break;
						}
                    	
						int lvlmodifier = 52 + skill.getLevel() * 2;
						if(skill.getLevel() == 12)
						{
							lvlmodifier = (Experience.MAX_LEVEL - 1);
						}

						int landrate = (int) skill.getPower();
						if((target.getLevel() - lvlmodifier) > 0)
						{
							landrate = 90 - 4 * (target.getLevel() - lvlmodifier);
						}

						landrate = (int) target.calcStat(Stats.CANCEL_VULN, landrate, target, null);

						if(Rnd.get(100) < landrate)
						{
							L2Effect[] effects = target.getAllEffects();
							int maxfive = 5;
							for(L2Effect e : effects)
							{
								switch(e.getEffectType())
								{
									case SIGNET_GROUND:
									case SIGNET_EFFECT:
									{
										continue;
									}
								default:
									break;
								}

								if(e.getSkill().getId() != 4082 && e.getSkill().getId() != 4215 && e.getSkill().getId() != 5182 && e.getSkill().getId() != 4515 && e.getSkill().getId() != 110 && e.getSkill().getId() != 111 && e.getSkill().getId() != 1323 && e.getSkill().getId() != 1325 && !skill.isToggle())
								{
									if(e.getSkill().getSkillType() != L2SkillType.BUFF)
									{
										e.exit();
									}
									else
									{
										int rate = 100;
										int level = e.getLevel();
										if(level > 0)
										{
											rate = Integer.valueOf(150 / (1 + level));
										}

										if(rate > 95)
										{
											rate = 95;
										}
										else if(rate < 5)
										{
											rate = 5;
										}

										if(Rnd.get(100) < rate)
										{
											e.exit();
											maxfive--;
											if(maxfive == 0)
											{
												break;
											}
										}
									}
								}
							}
						}
						else
						{
							if(activeChar instanceof L2PcInstance)
							{
								activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2).addString(target.getName()).addSkillName(skill.getDisplayId()));
							}
						}
						break;
					}
					else
					{
						int landrate = (int) skill.getPower();
						landrate = (int) target.calcStat(Stats.CANCEL_VULN, landrate, target, null);
						if(Rnd.get(100) < landrate)
						{
							L2Effect[] effects = target.getAllEffects();
							int maxdisp = (int) skill.getNegatePower();
							if(maxdisp == 0)
							{
								maxdisp = Config.BUFFS_MAX_AMOUNT + Config.DEBUFFS_MAX_AMOUNT + 6;
							}
							for(L2Effect e : effects)
							{
								switch(e.getEffectType())
								{
									case SIGNET_GROUND:
									case SIGNET_EFFECT:
									{
										continue;
									}
								default:
									break;
								}

								if(e.getSkill().getId() != 4082 && e.getSkill().getId() != 4215 && e.getSkill().getId() != 5182 && e.getSkill().getId() != 4515 && e.getSkill().getId() != 110 && e.getSkill().getId() != 111 && e.getSkill().getId() != 1323 && e.getSkill().getId() != 1325)
								{
									if(e.getSkill().getSkillType() == L2SkillType.BUFF)
									{
										int rate = 100;
										int level = e.getLevel();
										if(level > 0)
										{
											rate = Integer.valueOf(150 / (1 + level));
										}

										if(rate > 95)
										{
											rate = 95;
										}
										else if(rate < 5)
										{
											rate = 5;
										}

										if(Rnd.get(100) < rate)
										{
											e.exit();
											maxdisp--;
											if(maxdisp == 0)
											{
												break;
											}
										}
									}
								}
							}
						}
						else
						{
							if(activeChar instanceof L2PcInstance)
							{
								activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2).addString(target.getName()).addSkillName(skill.getDisplayId()));
							}
						}

						break;
					}
				}
				case NEGATE:
				{
					if(skill.getId() == 2275)
					{
						_negatePower = skill.getNegatePower();
						_negateId = skill.getNegateId();
						negateEffect(target, L2SkillType.BUFF, _negatePower, _negateId);
					}
					else
					{
						_negateStats = skill.getNegateStats();
						_negatePower = skill.getNegatePower();

						for(String stat : _negateStats)
						{
							stat = stat.toLowerCase().intern();
							if(stat == "buff")
							{
								int lvlmodifier = 52 + skill.getMagicLevel() * 2;
								if(skill.getMagicLevel() == 12)
								{
									lvlmodifier = (Experience.MAX_LEVEL - 1);
								}

								int landrate = 90;
								if((target.getLevel() - lvlmodifier) > 0)
								{
									landrate = 90 - 4 * (target.getLevel() - lvlmodifier);
								}

								landrate = (int) target.calcStat(Stats.CANCEL_VULN, landrate, target, null);

								if(Rnd.get(100) < landrate)
								{
									negateEffect(target, L2SkillType.BUFF, -1);
								}
							}
							if(stat == "debuff")
							{
								negateEffect(target, L2SkillType.DEBUFF, -1);
							}

							if(stat == "weakness")
							{
								negateEffect(target, L2SkillType.WEAKNESS, -1);
							}

							if(stat == "stun")
							{
								negateEffect(target, L2SkillType.STUN, -1);
							}

							if(stat == "sleep")
							{
								negateEffect(target, L2SkillType.SLEEP, -1);
							}

							if(stat == "mdam")
							{
								negateEffect(target, L2SkillType.MDAM, -1);
							}
							
							if(stat == "confusion")
								negateEffect(target, L2SkillType.CONFUSION, -1);

							if(stat == "mute")
							{
								negateEffect(target, L2SkillType.MUTE, -1);
							}

							if(stat == "fear")
							{
								negateEffect(target, L2SkillType.FEAR, -1);
							}

							if(stat == "poison")
							{
								negateEffect(target, L2SkillType.POISON, _negatePower);
							}

							if(stat == "bleed")
							{
								negateEffect(target, L2SkillType.BLEED, _negatePower);
							}

							if(stat == "paralyze")
							{
								negateEffect(target, L2SkillType.PARALYZE, -1);
							}

							if(stat == "root")
							{
								negateEffect(target, L2SkillType.ROOT, -1);
							}

							if(stat == "heal")
							{
								ISkillHandler Healhandler = SkillHandler.getInstance().getSkillHandler(L2SkillType.HEAL);
								if(Healhandler == null)
								{
									_log.severe("Couldn't find skill handler for HEAL.");
									continue;
								}

								L2Object tgts[] = new L2Object[] { target };
								try
								{
									Healhandler.useSkill(activeChar, skill, tgts);
								}
								catch(IOException e)
								{
									_log.log(Level.WARNING, "", e);
								}
							}
						}
					}
				}
			default:
				break;
			}
			Formulas.calcLethalHit(activeChar, target, skill);
		}

		L2Effect effect = activeChar.getFirstEffect(skill.getId());
		if(effect != null && effect.isSelfEffect())
		{
			effect.exit();
		}

		effect = null;
		skill.getEffectsSelf(activeChar);

	}

	private void negateEffect(L2Character target, L2SkillType type, double power)
	{
		negateEffect(target, type, power, 0);
	}

	private void negateEffect(L2Character target, L2SkillType type, double power, int skillId)
	{
		L2Effect[] effects = target.getAllEffects();
		for(L2Effect e : effects)
		{
			if(e.getSkill()!=null && e.getSkill().getId() == 4215 || e.getSkill().getId() == 4515)
			{
				continue; // skills can't be removed
			}
			else if(power == -1)
			{
				if(e.getSkill().getSkillType() == type || (e.getSkill().getEffectType() != null && e.getSkill().getEffectType() == type))
				{
					if(skillId != 0)
					{
						if(skillId == e.getSkill().getId())
						{
							e.exit();
						}
					}
					else
					{
						e.exit();
					}
				}
			}
			else if((e.getSkill().getSkillType() == type && e.getSkill().getPower() <= power) || (e.getSkill().getEffectType() != null && e.getSkill().getEffectType() == type && e.getSkill().getEffectLvl() <= power))
			{
				if(skillId != 0)
				{
					if(skillId == e.getSkill().getId())
					{
						e.exit();
					}
				}
				else
				{
					e.exit();
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