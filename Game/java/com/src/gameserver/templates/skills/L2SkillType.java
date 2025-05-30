/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.src.gameserver.templates.skills;

import java.lang.reflect.Constructor;

import com.src.gameserver.model.L2Skill;
import com.src.gameserver.skills.l2skills.L2SkillCharge;
import com.src.gameserver.skills.l2skills.L2SkillChargeDmg;
import com.src.gameserver.skills.l2skills.L2SkillChargeEffect;
import com.src.gameserver.skills.l2skills.L2SkillCreateItem;
import com.src.gameserver.skills.l2skills.L2SkillDefault;
import com.src.gameserver.skills.l2skills.L2SkillDrain;
import com.src.gameserver.skills.l2skills.L2SkillSeed;
import com.src.gameserver.skills.l2skills.L2SkillSignet;
import com.src.gameserver.skills.l2skills.L2SkillSignetCasttime;
import com.src.gameserver.skills.l2skills.L2SkillSummon;
import com.src.gameserver.templates.StatsSet;

/**
 * @author  nBd
 */
public enum L2SkillType
{
	PDAM,
	MDAM,
	CPDAM,
	MANADAM,
	DOT,
	MDOT,
	DRAIN_SOUL,
	DRAIN(L2SkillDrain.class),
	DEATHLINK,
	FATALCOUNTER,
	BLOW,

	BLEED,
	POISON,
	STUN,
	ROOT,
	CONFUSION,
	FEAR,
	SLEEP,
	CONFUSE_MOB_ONLY,
	MUTE,
	PARALYZE,
	WEAKNESS,

	HEAL,
	HOT,
	BALANCE_LIFE,
	HEAL_PERCENT,
	HEAL_STATIC,
	COMBATPOINTHEAL,
	COMBATPOINTPERCENTHEAL,
	CPHOT,
	MANAHEAL,
	MANA_BY_LEVEL,
	MANAHEAL_PERCENT,
	MANARECHARGE,
	MPHOT,

	AGGDAMAGE,
	AGGREDUCE,
	AGGREMOVE,
	AGGREDUCE_CHAR,
	AGGDEBUFF,

	FISHING,
	PUMPING,
	REELING,

	UNLOCK,
	UNLOCK_SPECIAL,
	ENCHANT_ARMOR,
	ENCHANT_WEAPON,
	SOULSHOT,
	SPIRITSHOT,
	SIEGEFLAG,
	TAKECASTLE,
	DELUXE_KEY_UNLOCK,
	SOW,
	HARVEST,
	GET_PLAYER,
	DUMMY,
	INSTANT_JUMP,

	COMMON_CRAFT,
	DWARVEN_CRAFT,
	CREATE_ITEM(L2SkillCreateItem.class),
	SUMMON_TREASURE_KEY,

	SUMMON(L2SkillSummon.class),
	FEED_PET,
	DEATHLINK_PET,
	STRSIEGEASSAULT,
	ERASE,
	BETRAY,

	CANCEL,
	CANCEL_DEBUFF,
	MAGE_BANE,
	WARRIOR_BANE,
	NEGATE,

	BUFF,
	DEBUFF,
	PASSIVE,
	CONT,
	SIGNET(L2SkillSignet.class),
	SIGNET_CASTTIME(L2SkillSignetCasttime.class),

	RESURRECT,
	CHARGE(L2SkillCharge.class),
	CHARGE_EFFECT(L2SkillChargeEffect.class),
	CHARGEDAM(L2SkillChargeDmg.class),
	MHOT,
	DETECT_WEAKNESS,
	LUCK,
	RECALL,
	SUMMON_FRIEND,
	REFLECT,
	SPOIL,
	SWEEP,
	FAKE_DEATH,
	UNBLEED,
	UNPOISON,
	UNDEAD_DEFENSE,
	SEED(L2SkillSeed.class),
	BEAST_FEED,
	FORCE_BUFF,
	CLAN_GATE,
	GIVE_SP,
	COREDONE,
	ZAKENPLAYER,
	ZAKENSELF,

	NOTDONE;

	private final Class<? extends L2Skill> _class;

	public L2Skill makeSkill(StatsSet set)
	{
		try
		{
			Constructor<? extends L2Skill> c = _class.getConstructor(StatsSet.class);
			
			return c.newInstance(set);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	private L2SkillType()
	{
		_class = L2SkillDefault.class;
	}

	private L2SkillType(Class<? extends L2Skill> classType)
	{
		_class = classType;
	}
}