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
package com.src.gameserver.templates.item;

import java.io.IOException;
import java.util.List;

import javolution.util.FastList;

import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.handler.ISkillHandler;
import com.src.gameserver.handler.SkillHandler;
import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.quest.Quest;
import com.src.gameserver.skills.Env;
import com.src.gameserver.skills.conditions.ConditionGameChance;
import com.src.gameserver.skills.funcs.Func;
import com.src.gameserver.skills.funcs.FuncTemplate;
import com.src.gameserver.templates.StatsSet;
import com.src.gameserver.templates.skills.L2SkillType;

public final class L2Weapon extends L2Item
{
	private final int _soulShotCount;
	private final int _spiritShotCount;
	private final int _pDam;
	private final int _rndDam;
	private final int _critical;
	private final double _hitModifier;
	private final int _avoidModifier;
	private final int _shieldDef;
	private final double _shieldDefRate;
	private final int _atkSpeed;
	private final int _atkReuse;
	private final int _mpConsume;
	private final int _mDam;
	private L2Skill _itemSkill = null;
	private L2Skill _enchant4Skill = null;
	protected L2Skill[] _skillsOnCast;
	protected L2Skill[] _skillsOnCrit;

	public L2Weapon(L2WeaponType type, StatsSet set)
	{
		super(type, set);
		_soulShotCount = set.getInteger("soulshots");
		_spiritShotCount = set.getInteger("spiritshots");
		_pDam = set.getInteger("p_dam");
		_rndDam = set.getInteger("rnd_dam");
		_critical = set.getInteger("critical");
		_hitModifier = set.getDouble("hit_modify");
		_avoidModifier = set.getInteger("avoid_modify");
		_shieldDef = set.getInteger("shield_def");
		_shieldDefRate = set.getDouble("shield_def_rate");
		_atkSpeed = set.getInteger("atk_speed");
		_atkReuse = set.getInteger("atk_reuse", type == L2WeaponType.BOW ? 1500 : 0);
		_mpConsume = set.getInteger("mp_consume");
		_mDam = set.getInteger("m_dam");

		int sId = set.getInteger("item_skill_id");
		int sLv = set.getInteger("item_skill_lvl");
		if(sId > 0 && sLv > 0)
		{
			_itemSkill = SkillTable.getInstance().getInfo(sId, sLv);
		}

		sId = set.getInteger("enchant4_skill_id");
		sLv = set.getInteger("enchant4_skill_lvl");
		if(sId > 0 && sLv > 0)
		{
			_enchant4Skill = SkillTable.getInstance().getInfo(sId, sLv);
		}

		sId = set.getInteger("onCast_skill_id");
		sLv = set.getInteger("onCast_skill_lvl");
		int sCh = set.getInteger("onCast_skill_chance");
		if(sId > 0 && sLv > 0 && sCh > 0)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(sId, sLv);
			skill.attach(new ConditionGameChance(sCh), true);
			attachOnCast(skill);
		}

		sId = set.getInteger("onCrit_skill_id");
		sLv = set.getInteger("onCrit_skill_lvl");
		sCh = set.getInteger("onCrit_skill_chance");
		if(sId > 0 && sLv > 0 && sCh > 0)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(sId, sLv);
			skill.attach(new ConditionGameChance(sCh), true);
			attachOnCrit(skill);
		}
	}

	@Override
	public L2WeaponType getItemType()
	{
		return (L2WeaponType) super._type;
	}

	@Override
	public int getItemMask()
	{
		return getItemType().mask();
	}

	public int getSoulShotCount()
	{
		return _soulShotCount;
	}

	public int getSpiritShotCount()
	{
		return _spiritShotCount;
	}

	public int getPDamage()
	{
		return _pDam;
	}

	public int getRandomDamage()
	{
		return _rndDam;
	}

	public int getAttackSpeed()
	{
		return _atkSpeed;
	}

	public int getAttackReuseDelay()
	{
		return _atkReuse;
	}

	public int getAvoidModifier()
	{
		return _avoidModifier;
	}

	public int getCritical()
	{
		return _critical;
	}

	public double getHitModifier()
	{
		return _hitModifier;
	}

	public int getMDamage()
	{
		return _mDam;
	}

	public int getMpConsume()
	{
		return _mpConsume;
	}

	public int getShieldDef()
	{
		return _shieldDef;
	}

	public double getShieldDefRate()
	{
		return _shieldDefRate;
	}

	public L2Skill getSkill()
	{
		return _itemSkill;
	}

	public L2Skill getEnchant4Skill()
	{
		return _enchant4Skill;
	}

	@Override
	public Func[] getStatFuncs(L2ItemInstance instance, L2Character player)
	{
		List<Func> funcs = new FastList<Func>();
		if(_funcTemplates != null)
		{
			for(FuncTemplate t : _funcTemplates)
			{
				Env env = new Env();
				env.player = player;
				env.item = instance;
				Func f = t.getFunc(env, instance);
				if(f != null)
				{
					funcs.add(f);
				}
			}
		}
		return funcs.toArray(new Func[funcs.size()]);
	}

	public L2Effect[] getSkillEffects(L2Character caster, L2Character target, boolean crit)
	{
		if(_skillsOnCrit == null || !crit)
		{
			return _emptyEffectSet;
		}
		List<L2Effect> effects = new FastList<L2Effect>();

		for(L2Skill skill : _skillsOnCrit)
		{
			if(target.isRaid() && (skill.getSkillType() == L2SkillType.CONFUSION || skill.getSkillType() == L2SkillType.MUTE || skill.getSkillType() == L2SkillType.PARALYZE || skill.getSkillType() == L2SkillType.ROOT))
			{
				continue;
			}

			if(!skill.checkCondition(caster, target, true))
			{
				continue;
			}

			if(target.getFirstEffect(skill.getId()) != null)
			{
				target.getFirstEffect(skill.getId()).exit();
			}
			for(L2Effect e : skill.getEffects(caster, target))
			{
				effects.add(e);
			}
		}
		if(effects.size() == 0)
		{
			return _emptyEffectSet;
		}
		return effects.toArray(new L2Effect[effects.size()]);
	}

	public L2Effect[] getSkillEffects(L2Character caster, L2Character target, L2Skill trigger)
	{
		if(_skillsOnCast == null)
		{
			return _emptyEffectSet;
		}
		List<L2Effect> effects = new FastList<L2Effect>();

		for(L2Skill skill : _skillsOnCast)
		{
			if(trigger.isOffensive() != skill.isOffensive())
			{
				continue;
			}

			if(trigger.getId() >= 1320 && trigger.getId() <= 1322)
			{
				continue;
			}

			if(trigger.isPotion())
			{
				continue;
			}

			if(target.isRaid() && (skill.getSkillType() == L2SkillType.CONFUSION || skill.getSkillType() == L2SkillType.MUTE || skill.getSkillType() == L2SkillType.PARALYZE || skill.getSkillType() == L2SkillType.ROOT))
			{
				continue;
			}

			if(trigger.isToggle() && skill.getSkillType() == L2SkillType.BUFF)
			{
				continue;
			}

			if(!skill.checkCondition(caster, target, true))
			{
				continue;
			}

			try
			{
				ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());

				L2Character[] targets = new L2Character[1];
				targets[0] = target;

				if(handler != null)
				{
					handler.useSkill(caster, skill, targets);
				}
				else
				{
					skill.useSkill(caster, targets);
				}

				if(caster instanceof L2PcInstance && target instanceof L2Npc)
				{
					Quest[] quests = ((L2Npc) target).getTemplate().getEventQuests(Quest.QuestEventType.ON_SKILL_USE);
					if(quests != null)
					{
						for(Quest quest : quests)
						{
							quest.notifySkillUse((L2Npc) target, (L2PcInstance) caster, skill);
						}
					}
				}
			}
			catch(IOException e)
			{
			}
		}
		if(effects.size() == 0)
		{
			return _emptyEffectSet;
		}
		return effects.toArray(new L2Effect[effects.size()]);
	}

	public void attachOnCrit(L2Skill skill)
	{
		if(_skillsOnCrit == null)
		{
			_skillsOnCrit = new L2Skill[]
			{
				skill
			};
		}
		else
		{
			int len = _skillsOnCrit.length;
			L2Skill[] tmp = new L2Skill[len + 1];

			System.arraycopy(_skillsOnCrit, 0, tmp, 0, len);
			tmp[len] = skill;
			_skillsOnCrit = tmp;
		}
	}

	public void attachOnCast(L2Skill skill)
	{
		if(_skillsOnCast == null)
		{
			_skillsOnCast = new L2Skill[]
			{
				skill
			};
		}
		else
		{
			int len = _skillsOnCast.length;
			L2Skill[] tmp = new L2Skill[len + 1];

			System.arraycopy(_skillsOnCast, 0, tmp, 0, len);
			tmp[len] = skill;
			_skillsOnCast = tmp;
		}
	}
}