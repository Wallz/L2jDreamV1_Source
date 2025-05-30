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
package com.src.gameserver.model;

import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;

import com.src.Config;
import com.src.gameserver.datatables.HeroSkillTable;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.xml.SkillTreeTable;
import com.src.gameserver.geo.GeoData;
import com.src.gameserver.managers.CoupleManager;
import com.src.gameserver.managers.SiegeManager;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.instance.L2ArtefactInstance;
import com.src.gameserver.model.actor.instance.L2ChestInstance;
import com.src.gameserver.model.actor.instance.L2ControlTowerInstance;
import com.src.gameserver.model.actor.instance.L2DoorInstance;
import com.src.gameserver.model.actor.instance.L2MonsterInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.model.actor.instance.L2SummonInstance;
import com.src.gameserver.model.base.ClassId;
import com.src.gameserver.model.entity.Wedding;
import com.src.gameserver.model.entity.siege.Siege;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.EtcStatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.Env;
import com.src.gameserver.skills.Formulas;
import com.src.gameserver.skills.Stats;
import com.src.gameserver.skills.conditions.Condition;
import com.src.gameserver.skills.effects.EffectCharge;
import com.src.gameserver.skills.effects.EffectTemplate;
import com.src.gameserver.skills.funcs.Func;
import com.src.gameserver.skills.funcs.FuncTemplate;
import com.src.gameserver.taskmanager.DecayTaskManager;
import com.src.gameserver.templates.StatsSet;
import com.src.gameserver.templates.skills.L2EffectType;
import com.src.gameserver.templates.skills.L2SkillType;
import com.src.gameserver.util.Util;

public abstract class L2Skill
{
	protected static final Logger _log = Logger.getLogger(L2Skill.class.getName());

	private static final L2Object[] _emptyTargetList = new L2Object[0];
	
	public static final boolean geoEnabled = Config.GEODATA > 0;
	
	public static final int SKILL_CUBIC_MASTERY = 143;
	public static final int SKILL_LUCKY = 194;
	public static final int SKILL_CREATE_COMMON = 1320;
	public static final int SKILL_CREATE_DWARVEN = 172;
	public static final int SKILL_CRYSTALLIZE = 248;
	public static final int SKILL_DIVINE_INSPIRATION = 1405;

	public static final int SKILL_FAKE_INT = 9001;
	public static final int SKILL_FAKE_WIT = 9002;
	public static final int SKILL_FAKE_MEN = 9003;
	public static final int SKILL_FAKE_CON = 9004;
	public static final int SKILL_FAKE_DEX = 9005;
	public static final int SKILL_FAKE_STR = 9006;

	private final int _targetConsumeId;
	private final int _targetConsume;
	
	public static enum SkillOpType
	{
		OP_PASSIVE,
		OP_ACTIVE,
		OP_TOGGLE,
		OP_CHANCE
	}

	public static enum SkillTargetType
	{
		TARGET_NONE,
		TARGET_SELF,
		TARGET_ONE,
		TARGET_PARTY,
		TARGET_ALLY,
		TARGET_CLAN,
		TARGET_PET,
		TARGET_AREA,
		TARGET_AURA,
		TARGET_CORPSE,
		TARGET_UNDEAD,
		TARGET_AREA_UNDEAD,
		TARGET_MULTIFACE,
		TARGET_CORPSE_ALLY,
		TARGET_CORPSE_CLAN,
		TARGET_CORPSE_PLAYER,
		TARGET_CORPSE_PET,
		TARGET_ITEM,
		TARGET_AREA_CORPSE_MOB,
		TARGET_CORPSE_MOB,
		TARGET_UNLOCKABLE,
		TARGET_HOLY,
		TARGET_PARTY_MEMBER,
		TARGET_PARTY_OTHER,
		TARGET_ENEMY_ALLY,
		TARGET_SUMMON,
		TARGET_AREA_SUMMON,
		TARGET_ENEMY_SUMMON,
		TARGET_OWNER_PET,
		TARGET_GROUND,
		TARGET_SIEGE,
		TARGET_TYRANNOSAURUS,
		TARGET_AREA_AIM_CORPSE,
		TARGET_COUPLE,
		TARGET_CLAN_MEMBER
	}

	protected ChanceCondition _chanceCondition = null;

	public final static int ELEMENT_WIND = 1;
	public final static int ELEMENT_FIRE = 2;
	public final static int ELEMENT_WATER = 3;
	public final static int ELEMENT_EARTH = 4;
	public final static int ELEMENT_HOLY = 5;
	public final static int ELEMENT_DARK = 6;

	public final static int COND_RUNNING = 0x0001;
	public final static int COND_WALKING = 0x0002;
	public final static int COND_SIT = 0x0004;
	public final static int COND_BEHIND = 0x0008;
	public final static int COND_CRIT = 0x0010;
	public final static int COND_LOWHP = 0x0020;
	public final static int COND_ROBES = 0x0040;
	public final static int COND_CHARGES = 0x0080;
	public final static int COND_SHIELD = 0x0100;

	private static final Func[] _emptyFunctionSet = new Func[0];
	private static final L2Effect[] _emptyEffectSet = new L2Effect[0];

	private final int _id;
	private final int _level;

	private int _displayId;

	private final String _name;
	private final SkillOpType _operateType;
	private final boolean _magic;
	private final boolean _staticReuse;
	private final boolean _staticHitTime;
	private final int _mpConsume;
	private final int _mpInitialConsume;
	private final int _hpConsume;
	private final int _itemConsume;
	private final int _itemConsumeId;
	private final int _itemConsumeOT;
	private final int _itemConsumeIdOT;
	private final int _itemConsumeSteps;
	private final int _summonTotalLifeTime;
	private final int _summonTimeLostIdle;
	private final int _summonTimeLostActive;

	private final int _itemConsumeTime;
	private final int _castRange;
	private final int _effectRange;

	private final int _hitTime;
	private final int _coolTime;
	private final int _reuseDelay;
	private final int _buffDuration;

	private final SkillTargetType _targetType;

	private final double _power;
	private final int _effectPoints;
	private final int _magicLevel;
	private final String[] _negateStats;
	private final float _negatePower;
	private final int _negateId;
	private final int _levelDepend;

	private final int _skillRadius;

	private final L2SkillType _skillType;
	private final L2SkillType _effectType;
	private final int _effectPower;
	private final int _effectId;
	private final int _effectLvl;

	private final boolean _ispotion;
	private final int _element;
	private final int _savevs;

	private final boolean _isSuicideAttack;

	private final Stats _stat;

	private final int _condition;
	private final int _conditionValue;
	private final boolean _overhit;
	private final int _weaponsAllowed;
	private final int _armorsAllowed;

	private final int _addCrossLearn;
	private final float _mulCrossLearn;
	private final float _mulCrossLearnRace;
	private final float _mulCrossLearnProf;
	private final List<ClassId> _canLearn;
	private final List<Integer> _teachers;
	private final int _minPledgeClass;

	private final boolean _ignoreResists;
	private final boolean _canBeReflected;
	private final boolean _canBeDispeled;
	
	private final boolean _isOffensive;
	private final int _numCharges;
	private final int _triggeredId;
	private final int _triggeredLevel;

	private final boolean _bestowed;

	private final boolean _isHeroSkill;

	private final int _baseCritRate;
	private final int _lethalEffect1;
	private final int _lethalEffect2;
	private final boolean _directHpDmg;
	private final boolean _isDance;
	private final int _nextDanceCost;
	private final float _sSBoost;
	private final int _aggroPoints;

	private final float _pvpMulti;

	private final String _flyType;
	private final int _flyRadius;
	private final float _flyCourse;

	protected Condition _preCondition;
	protected Condition _itemPreCondition;
	protected FuncTemplate[] _funcTemplates;
	public EffectTemplate[] _effectTemplates;
	protected EffectTemplate[] _effectTemplatesSelf;

	private final boolean _nextActionIsAttack;
	private final boolean _isDebuff;
	
	protected L2Skill(StatsSet set)
	{
		_id = set.getInteger("skill_id");
		_level = set.getInteger("level");

		_displayId = set.getInteger("displayId", _id);
		_name = set.getString("name");
		_operateType = set.getEnum("operateType", SkillOpType.class);
		_magic = set.getBool("isMagic", false);
		_staticReuse = set.getBool("staticReuse", false);
		_staticHitTime = set.getBool("staticHitTime", false);
		_ispotion = set.getBool("isPotion", false);
		_mpConsume = set.getInteger("mpConsume", 0);
		_mpInitialConsume = set.getInteger("mpInitialConsume", 0);
		_hpConsume = set.getInteger("hpConsume", 0);
		_itemConsume = set.getInteger("itemConsumeCount", 0);
		_itemConsumeId = set.getInteger("itemConsumeId", 0);
		_itemConsumeOT = set.getInteger("itemConsumeCountOT", 0);
		_itemConsumeIdOT = set.getInteger("itemConsumeIdOT", 0);
		_itemConsumeTime = set.getInteger("itemConsumeTime", 0);
		_itemConsumeSteps = set.getInteger("itemConsumeSteps", 0);
		_summonTotalLifeTime = set.getInteger("summonTotalLifeTime", 1200000);
		_summonTimeLostIdle = set.getInteger("summonTimeLostIdle", 0);
		_summonTimeLostActive = set.getInteger("summonTimeLostActive", 0);

		_castRange = set.getInteger("castRange", 0);
		_effectRange = set.getInteger("effectRange", -1);

		_hitTime = set.getInteger("hitTime", 0);
		_coolTime = set.getInteger("coolTime", 0);
		_reuseDelay = set.getInteger("reuseDelay", 0);
		_buffDuration = set.getInteger("buffDuration", 0);

		_skillRadius = set.getInteger("skillRadius", 80);
		
		_ignoreResists = set.getBool("ignoreResists", false);
		_canBeReflected = set.getBool("canBeReflected", true);
		_canBeDispeled = set.getBool("canBeDispeled", true);
		
		_targetType = set.getEnum("target", SkillTargetType.class);
		_power = set.getFloat("power", 0.f);
		_effectPoints = set.getInteger("effectPoints", 0);
		_negateStats = set.getString("negateStats", "").split(" ");
		_negatePower = set.getFloat("negatePower", 0.f);
		_negateId = set.getInteger("negateId", 0);
		_magicLevel = set.getInteger("magicLvl", SkillTreeTable.getInstance().getMinSkillLevel(_id, _level));
		_levelDepend = set.getInteger("lvlDepend", 0);
		_stat = set.getEnum("stat", Stats.class, null);

		_skillType = set.getEnum("skillType", L2SkillType.class);
		_effectType = set.getEnum("effectType", L2SkillType.class, null);
		_effectPower = set.getInteger("effectPower", 0);
		_effectId = set.getInteger("effectId", 0);
		_effectLvl = set.getInteger("effectLevel", 0);

		_element = set.getInteger("element", 0);
		_savevs = set.getInteger("save", 0);

		_condition = set.getInteger("condition", 0);
		_conditionValue = set.getInteger("conditionValue", 0);
		_overhit = set.getBool("overHit", false);
		_isSuicideAttack = set.getBool("isSuicideAttack", false);
		_weaponsAllowed = set.getInteger("weaponsAllowed", 0);
		_armorsAllowed = set.getInteger("armorsAllowed", 0);

		_addCrossLearn = set.getInteger("addCrossLearn", 1000);
		_mulCrossLearn = set.getFloat("mulCrossLearn", 2.f);
		_mulCrossLearnRace = set.getFloat("mulCrossLearnRace", 2.f);
		_mulCrossLearnProf = set.getFloat("mulCrossLearnProf", 3.f);
		_minPledgeClass = set.getInteger("minPledgeClass", 0);
		_isOffensive = set.getBool("offensive", isSkillTypeOffensive());
		_numCharges = set.getInteger("num_charges", 0);
		_triggeredId = set.getInteger("triggeredId", 0);
		_triggeredLevel = set.getInteger("triggeredLevel", 0);

		_bestowed = set.getBool("bestowed", false);
		
		_targetConsume = set.getInteger("targetConsumeCount", 0);
		_targetConsumeId = set.getInteger("targetConsumeId", 0);

		if(_operateType == SkillOpType.OP_CHANCE)
		{
			_chanceCondition = ChanceCondition.parse(set);
		}

		_isHeroSkill = HeroSkillTable.isHeroSkill(_id);

		_baseCritRate = set.getInteger("baseCritRate", (_skillType == L2SkillType.PDAM  || _skillType == L2SkillType.BLOW) ? 0 : -1);
		_lethalEffect1 = set.getInteger("lethal1", 0);
		_lethalEffect2 = set.getInteger("lethal2", 0);

		_directHpDmg = set.getBool("dmgDirectlyToHp", false);
		_isDance = set.getBool("isDance", false);
		_nextDanceCost = set.getInteger("nextDanceCost", 0);
		_sSBoost = set.getFloat("SSBoost", 0.f);
		_aggroPoints = set.getInteger("aggroPoints", 0);

		_pvpMulti = set.getFloat("pvpMulti", 1.f);

		_nextActionIsAttack = set.getBool("nextActionAttack", false);
		
		_flyType = set.getString("flyType", null);
		_flyRadius = set.getInteger("flyRadius", 0);
		_flyCourse = set.getFloat("flyCourse", 0);

		String canLearn = set.getString("canLearn", null);
		if(canLearn == null)
		{
			_canLearn = null;
		}
		else
		{
			_canLearn = new FastList<ClassId>();
			StringTokenizer st = new StringTokenizer(canLearn, " \r\n\t,;");

			while(st.hasMoreTokens())
			{
				String cls = st.nextToken();
				try
				{
					_canLearn.add(ClassId.valueOf(cls));
				}
				catch(Throwable t)
				{
					_log.log(Level.SEVERE, "Bad class " + cls + " to learn skill", t);
				}
				cls = null;
			}

			st = null;
		}

		canLearn = null;

		String teachers = set.getString("teachers", null);
		if(teachers == null)
		{
			_teachers = null;
		}
		else
		{
			_teachers = new FastList<Integer>();
			StringTokenizer st = new StringTokenizer(teachers, " \r\n\t,;");
			while(st.hasMoreTokens())
			{
				String npcid = st.nextToken();
				try
				{
					_teachers.add(Integer.parseInt(npcid));
				}
				catch(Throwable t)
				{
					_log.log(Level.SEVERE, "Bad teacher id " + npcid + " to teach skill", t);
				}

				npcid = null;
			}

			st = null;
		}

		teachers = null;
		
		_isDebuff = set.getBool("isDebuff", false);
	}

	public abstract void useSkill(L2Character caster, L2Object[] targets);
	
	public boolean isDebuff()
	{
		boolean type_debuff = false;
		
		switch(_skillType){
			case AGGDEBUFF:
			case DEBUFF:
			case STUN:
			case BLEED:
			case CONFUSION:
			case FEAR:
			case PARALYZE:
			case SLEEP:
			case ROOT:
			case WEAKNESS:
				type_debuff = true;
		default:
			break;
				
		}
		
		return _isDebuff || type_debuff;
	}
	
	public final boolean nextActionIsAttack()
	{
		return _nextActionIsAttack;
	}
	
	public final boolean isPotion()
	{
		return _ispotion;
	}

	public final int getArmorsAllowed()
	{
		return _armorsAllowed;
	}

	public final int getConditionValue()
	{
		return _conditionValue;
	}

	public final L2SkillType getSkillType()
	{
		return _skillType;
	}

	public final boolean hasEffectWhileCasting()
	{
		return getSkillType() == L2SkillType.SIGNET_CASTTIME;
	}

	public final int getSavevs()
	{
		return _savevs;
	}

	public final int getElement()
	{
		return _element;
	}

	public final SkillTargetType getTargetType()
	{
		return _targetType;
	}

	public final int getCondition()
	{
		return _condition;
	}

	public final boolean isOverhit()
	{
		return _overhit;
	}

	public final boolean isSuicideAttack()
	{
		return _isSuicideAttack;
	}

	public final double getPower(L2Character activeChar)
	{
    	if (activeChar == null)
    		return _power;

		switch (_skillType)
		{
			case DEATHLINK:
				return _power * Math.pow(1.7165 - activeChar.getCurrentHp()/activeChar.getMaxHp(), 2) * 0.577;
			case FATALCOUNTER:
				return _power * 3.5 * (1 - activeChar.getCurrentHp()/activeChar.getMaxHp());
			default:
				return _power;
		}
	}

	public final double getPower()
	{
		return _power;
	}

	public final int getEffectPoints()
	{
		return _effectPoints;
	}

	public final String[] getNegateStats()
	{
		return _negateStats;
	}

	public final float getNegatePower()
	{
		return _negatePower;
	}

	public final int getNegateId()
	{
		return _negateId;
	}

	public final int getMagicLevel()
	{
		return _magicLevel;
	}

	public final boolean isStaticReuse()
	{
		return _staticReuse;
	}

	public final boolean isStaticHitTime()
	{
		return _staticHitTime;
	}

	public final int getLevelDepend()
	{
		return _levelDepend;
	}

	public final int getEffectPower()
	{
		return _effectPower;
	}

	public final int getEffectId()
	{
		return _effectId;
	}

	public final int getEffectLvl()
	{
		return _effectLvl;
	}

	public final L2SkillType getEffectType()
	{
		return _effectType;
	}

	public final int getBuffDuration()
	{
		return _buffDuration;
	}

	public final int getCastRange()
	{
		return _castRange;
	}

	public final int getEffectRange()
	{
		return _effectRange;
	}

	public final int getHpConsume()
	{
		return _hpConsume;
	}

	public final int getId()
	{
		return _id;
	}

	public int getDisplayId()
	{
		return _displayId;
	}

	public void setDisplayId(int id)
	{
		_displayId = id;
	}

	public int getTriggeredId()
	{
		return _triggeredId;
	}

	public int getTriggeredLevel()
	{
		return _triggeredLevel;
	}

	public final Stats getStat()
	{
		return _stat;
	}

	public final int getItemConsume()
	{
		return _itemConsume;
	}

	public final int getItemConsumeId()
	{
		return _itemConsumeId;
	}

	public final int getItemConsumeOT()
	{
		return _itemConsumeOT;
	}

	public final int getItemConsumeIdOT()
	{
		return _itemConsumeIdOT;
	}

	public final int getItemConsumeSteps()
	{
		return _itemConsumeSteps;
	}

	public final int getTotalLifeTime()
	{
		return _summonTotalLifeTime;
	}

	public final int getTimeLostIdle()
	{
		return _summonTimeLostIdle;
	}

	public final int getTimeLostActive()
	{
		return _summonTimeLostActive;
	}

	public final int getItemConsumeTime()
	{
		return _itemConsumeTime;
	}

	public final int getLevel()
	{
		return _level;
	}

	public final boolean isMagic()
	{
		return _magic;
	}

	public final int getMpConsume()
	{
		return _mpConsume;
	}

	public final int getMpInitialConsume()
	{
		return _mpInitialConsume;
	}

	public final String getName()
	{
		return _name;
	}

	public final int getReuseDelay()
	{
		return _reuseDelay;
	}

	@Deprecated
	public final int getSkillTime()
	{
		return _hitTime;
	}

	public final int getHitTime()
	{
		return _hitTime;
	}

	public final int getCoolTime()
	{
		return _coolTime;
	}

	public final int getSkillRadius()
	{
		return _skillRadius;
	}

	public final boolean isActive()
	{
		return _operateType == SkillOpType.OP_ACTIVE;
	}

	public final boolean isPassive()
	{
		return _operateType == SkillOpType.OP_PASSIVE;
	}

	public final boolean isToggle()
	{
		return _operateType == SkillOpType.OP_TOGGLE;
	}

	public final boolean isChance()
	{
		return _operateType == SkillOpType.OP_CHANCE;
	}

	public ChanceCondition getChanceCondition()
	{
		return _chanceCondition;
	}

	public final boolean isDance()
	{
		return _isDance;
	}

	public final int getNextDanceMpCost()
	{
		return _nextDanceCost;
	}

	public final float getSSBoost()
	{
		return _sSBoost;
	}

	public final int getAggroPoints()
	{
		return _aggroPoints;
	}

	public final float getPvpMulti()
	{
		return _pvpMulti;
	}

	public final boolean useSoulShot()
	{
		return getSkillType() == L2SkillType.PDAM || (getSkillType() == L2SkillType.STUN) || getSkillType() == L2SkillType.CHARGEDAM || getSkillType() == L2SkillType.BLOW;
	}

	public final boolean useSpiritShot()
	{
		return isMagic();
	}

	public final boolean useFishShot()
	{
		return getSkillType() == L2SkillType.PUMPING || getSkillType() == L2SkillType.REELING;
	}

	public final int getWeaponsAllowed()
	{
		return _weaponsAllowed;
	}

	public final int getCrossLearnAdd()
	{
		return _addCrossLearn;
	}

	public final float getCrossLearnMul()
	{
		return _mulCrossLearn;
	}

	public final float getCrossLearnRace()
	{
		return _mulCrossLearnRace;
	}

	public final float getCrossLearnProf()
	{
		return _mulCrossLearnProf;
	}

	public final boolean getCanLearn(ClassId cls)
	{
		return _canLearn == null || _canLearn.contains(cls);
	}

	public final boolean canTeachBy(int npcId)
	{
		return _teachers == null || _teachers.contains(npcId);
	}

	public int getMinPledgeClass()
	{
		return _minPledgeClass;
	}

	public final boolean isPvpSkill()
	{
		switch(_skillType)
		{
			case DOT:
			case AGGREDUCE:
			case AGGDAMAGE:
			case AGGREDUCE_CHAR:
			case CONFUSE_MOB_ONLY:
			case BLEED:
			case CONFUSION:
			case POISON:
			case DEBUFF:
			case AGGDEBUFF:
			case STUN:
			case ROOT:
			case FEAR:
			case SLEEP:
			case MDOT:
			case MANADAM:
			case MUTE:
			case WEAKNESS:
			case PARALYZE:
			case CANCEL:
			case MAGE_BANE:
			case WARRIOR_BANE:
			case FATALCOUNTER:
			case BETRAY:
				return true;
			default:
				return false;
		}
	}

	public final boolean is7Signs()
	{
		if (_id > 4360 && _id < 4367)
			return true;
		return false;
	}
	
	public final boolean isOffensive()
	{
		return _isOffensive;
	}

	public final boolean isHeroSkill()
	{
		return _isHeroSkill;
	}

	public final int getNumCharges()
	{
		return _numCharges;
	}

	public final int getBaseCritRate()
	{
		return _baseCritRate;
	}

	public final boolean ignoreResists()
	{
		return _ignoreResists;
	}
	
	public boolean canBeReflected()
	{
		return _canBeReflected;
	}
	
	public boolean canBeDispeled()
	{
		return _canBeDispeled;
	}
	
	public final int getLethalChance1()
	{
		return _lethalEffect1;
	}

	public final int getLethalChance2()
	{
		return _lethalEffect2;
	}

	public final boolean getDmgDirectlyToHP()
	{
		return _directHpDmg;
	}

	public boolean bestowed()
	{
		return _bestowed;
	}

	public boolean triggerAnotherSkill()
	{
		return _triggeredId > 1;
	}

	public final boolean isSkillTypeOffensive()
	{
		switch(_skillType)
		{
			case PDAM:
			case MDAM:
			case CPDAM:
			case DOT:
			case BLEED:
			case POISON:
			case AGGDAMAGE:
			case DEBUFF:
			case AGGDEBUFF:
			case STUN:
			case ROOT:
			case CONFUSION:
			case ERASE:
			case BLOW:
			case FEAR:
			case DRAIN:
			case SLEEP:
			case CHARGEDAM:
			case CONFUSE_MOB_ONLY:
			case DEATHLINK:
			case DETECT_WEAKNESS:
			case MANADAM:
			case MDOT:
			case MUTE:
			case SOULSHOT:
			case SPIRITSHOT:
			case SPOIL:
			case WEAKNESS:
			case MANA_BY_LEVEL:
			case SWEEP:
			case PARALYZE:
			case DRAIN_SOUL:
			case AGGREDUCE:
			case CANCEL:
			case MAGE_BANE:
			case WARRIOR_BANE:
			case AGGREMOVE:
			case AGGREDUCE_CHAR:
			case FATALCOUNTER:
			case BETRAY:
			case DELUXE_KEY_UNLOCK:
			case SOW:
			case HARVEST:
			case INSTANT_JUMP:
				return true;
			default:
				return false;
		}
	}

	public final boolean getWeaponDependancy(L2Character activeChar)
	{
		if(getWeaponDependancy(activeChar, false))
		{
			return true;
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(this.getId()));
			return false;
		}
	}

	public final boolean getWeaponDependancy(L2Character activeChar, boolean chance)
	{
		int weaponsAllowed = getWeaponsAllowed();
		if(weaponsAllowed == 0)
		{
			return true;
		}

		int mask = 0;
		if(activeChar.getActiveWeaponItem() != null)
		{
			mask |= activeChar.getActiveWeaponItem().getItemType().mask();
		}

		if(activeChar.getSecondaryWeaponItem() != null)
		{
			mask |= activeChar.getSecondaryWeaponItem().getItemType().mask();
		}

		if((mask & weaponsAllowed) != 0)
		{
			return true;
		}

		return false;
	}

	public boolean checkCondition(L2Character activeChar, L2Object target, boolean itemOrWeapon)
	{
		Condition preCondition = _preCondition;

		if(itemOrWeapon)
		{
			preCondition = _itemPreCondition;
		}

		if(preCondition == null)
		{
			return true;
		}

		Env env = new Env();
		env.player = activeChar;
		if(target instanceof L2Character)
		{
			env.target = (L2Character) target;
		}

		env.skill = this;
		if(!preCondition.test(env))
		{
			String msg = preCondition.getMessage();
			if(msg != null)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString(msg));
			}

			msg = null;

			return false;
		}

		env = null;
		preCondition = null;

		return true;
	}

	public final L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst)
	{
		L2Character target = null;

		L2Object objTarget = activeChar.getTarget();
		if(objTarget instanceof L2Character)
		{
			target = (L2Character) objTarget;
		}

		return getTargetList(activeChar, onlyFirst, target);
	}

	public final L2Object[] getTargetList(L2Character activeChar, boolean onlyFirst, L2Character target)
	{
		if( activeChar instanceof L2PcInstance  )
		{ //to avoid attacks during oly start period
			
			if(isOffensive() && (((L2PcInstance)activeChar).isInOlympiadMode() && !((L2PcInstance)activeChar).isOlympiadStart()))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				return null;
			}
			
		}

		List<L2Character> targetList = new FastList<L2Character>();

		if(isPotion())
		{
			
			return new L2Character[]
					{
   					activeChar
   				};
			
		}
		
		// Get the target type of the skill
		// (ex : ONE, SELF, HOLY, PET, AURA, AURA_CLOSE, AREA, MULTIFACE, PARTY, CLAN, CORPSE_PLAYER, CORPSE_MOB, CORPSE_CLAN, UNLOCKABLE, ITEM, UNDEAD)
		SkillTargetType targetType = getTargetType();

		// Get the type of the skill
		// (ex : PDAM, MDAM, DOT, BLEED, POISON, HEAL, HOT, MANAHEAL, MANARECHARGE, AGGDAMAGE, BUFF, DEBUFF, STUN, ROOT, RESURRECT, PASSIVE...)
		L2SkillType skillType = getSkillType();

		switch(targetType)
		{
			// The skill can only be used on the L2Character targeted, or on the caster itself
			case TARGET_ONE:
			{
				boolean canTargetSelf = false;
				switch(skillType)
				{
					case BUFF:
					case HEAL:
					case HOT:
					case HEAL_PERCENT:
					case MANARECHARGE:
					case MANAHEAL:
					case NEGATE:
					case REFLECT:
					case UNBLEED:
					case UNPOISON: //case CANCEL: 
					case SEED:
					case COMBATPOINTHEAL:
					case COMBATPOINTPERCENTHEAL:
					case MAGE_BANE:
					case WARRIOR_BANE:
					case BETRAY:
					case BALANCE_LIFE:
					case FORCE_BUFF:
						canTargetSelf = true;
						break;
				}

				switch(skillType)
				{
					case CONFUSION:
					case DEBUFF:
					case STUN:
					case ROOT:
					case FEAR:
					case SLEEP:
					case MUTE:
					case WEAKNESS:
					case PARALYZE:
					case CANCEL:
					case MAGE_BANE:
					case WARRIOR_BANE:
						if(checkPartyClan(activeChar, target))
						{
							activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
							return null;
						}
						break;
				}

				// Check for null target or any other invalid target
				if(target == null || target.isDead() || target == activeChar && !canTargetSelf)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return null;
				}

				// If a target is found, return it in a table else send a system message TARGET_IS_INCORRECT
				return new L2Character[]
				{
					target
				};
			}
			case TARGET_SELF:
			case TARGET_GROUND:
			{
				return new L2Character[]
				{
					activeChar
				};
			}
			case TARGET_HOLY:
			{
				if(activeChar instanceof L2PcInstance)
				{
					if(activeChar.getTarget() instanceof L2ArtefactInstance)
					{
						return new L2Character[] {(L2ArtefactInstance) activeChar.getTarget()};
					}
				}

				return _emptyTargetList;
			}

			case TARGET_PET:
			{
				target = activeChar.getPet();
				if(target != null && !target.isDead())
				{
					return new L2Character[]
					{
						target
					};
				}

				return _emptyTargetList;
			}
			case TARGET_SUMMON:
			{
				target = activeChar.getPet();
				if (target != null && !target.isDead() && target instanceof L2SummonInstance)
					return new L2Character[]
					{
						target
					};
				
				return _emptyTargetList;
			}
			case TARGET_OWNER_PET:
			{
				if(activeChar instanceof L2Summon)
				{
					target = ((L2Summon) activeChar).getOwner();
					if(target != null && !target.isDead())
					{
						return new L2Character[]
						{
							target
						};
					}
				}

				return _emptyTargetList;
			}
			case TARGET_CORPSE_PET:
			{
				if(activeChar instanceof L2PcInstance)
				{
					target = activeChar.getPet();
					if(target != null && target.isDead())
					{
						return new L2Character[]
						{
							target
						};
					}
				}

				return _emptyTargetList;
			}
			case TARGET_AREA_SUMMON:
			{
				target = activeChar.getPet();
				if (target == null || !(target instanceof L2SummonInstance) || target.isDead())
					return _emptyTargetList;
				
				if (onlyFirst)
					return new L2Character[]
					{
						target
					};
				
				final boolean srcInArena = (activeChar.isInsideZone(L2Character.ZONE_PVP) && !activeChar.isInsideZone(L2Character.ZONE_SIEGE));
				final Collection<L2Character> objs = target.getKnownList().getKnownCharacters();
				final int radius = getSkillRadius();
				
				for (L2Character obj : objs)
				{
					if (obj == null || obj == target || obj == activeChar)
						continue;
					
					if (!Util.checkIfInRange(radius, target, obj, true))
						continue;
					
					if (!(obj instanceof L2Attackable || obj instanceof L2Playable))
						continue;
					
					if (!checkForAreaOffensiveSkills(activeChar, obj, this, srcInArena))
						continue;
					
					targetList.add(obj);
				}
				
				if (targetList.isEmpty())
					return _emptyTargetList;
				
				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_AURA:
			{
				int radius = getSkillRadius();
				boolean srcInArena = activeChar.isInsideZone(L2Character.ZONE_PVP) && !activeChar.isInsideZone(L2Character.ZONE_SIEGE);

				L2PcInstance src = null;
				if(activeChar instanceof L2PcInstance)
				{
					src = (L2PcInstance) activeChar;
				}

				if(activeChar instanceof L2Summon)
				{
					src = ((L2Summon) activeChar).getOwner();
				}

				for(L2Object obj : activeChar.getKnownList().getKnownCharactersInRadius(radius))
				{
					if(src instanceof L2PcInstance && obj != null && (obj instanceof L2Attackable || obj instanceof L2Playable))
					{
						if(obj == activeChar || obj == src)
						{
							continue;
						}

						if(src != null)
						{
							if(!GeoData.getInstance().canSeeTarget(activeChar, obj))
							{
								continue;
							}

							if(obj instanceof L2PcInstance)
							{
								if(((L2PcInstance) obj).isDead())
								{
									continue;
								}

								if(((L2PcInstance) obj).getAppearance().getInvisible())
								{
									continue;
								}

								if(!src.checkPvpSkill(obj, this))
								{
									continue;
								}

								if(src.isInOlympiadMode() && !src.isOlympiadStart())
								{
									continue;
								}

								if(src.getParty() != null && ((L2PcInstance) obj).getParty() != null && src.getParty().getPartyLeaderOID() == ((L2PcInstance) obj).getParty().getPartyLeaderOID())
								{
									continue;
								}

								if(!srcInArena && !(((L2Character) obj).isInsideZone(L2Character.ZONE_PVP) && !((L2Character) obj).isInsideZone(L2Character.ZONE_SIEGE)))
								{
									if(src.getClanId() != 0 && src.getClanId() == ((L2PcInstance) obj).getClanId())
									{
										continue;
									}

									if(src.getAllyId() != 0 && src.getAllyId() == ((L2PcInstance) obj).getAllyId())
									{
										continue;
									}
								}
							}
							if(obj instanceof L2Summon)
							{
								L2PcInstance trg = ((L2Summon) obj).getOwner();
								if(trg == src)
								{
									continue;
								}

								if(!src.checkPvpSkill(trg, this))
								{
									continue;
								}

								if(src.isInOlympiadMode() && !src.isOlympiadStart())
								{
									continue;
								}

								if(src.getParty() != null && trg.getParty() != null && src.getParty().getPartyLeaderOID() == trg.getParty().getPartyLeaderOID())
								{
									continue;
								}

								if(!srcInArena && !(((L2Character) obj).isInsideZone(L2Character.ZONE_PVP) && !((L2Character) obj).isInsideZone(L2Character.ZONE_SIEGE)))
								{
									if(src.getClanId() != 0 && src.getClanId() == trg.getClanId())
									{
										continue;
									}

									if(src.getAllyId() != 0 && src.getAllyId() == trg.getAllyId())
									{
										continue;
									}
								}

								trg = null;
							}
						}
					}

					if(!Util.checkIfInRange(radius, activeChar, obj, true))
					{
						continue;
					}

					if(onlyFirst == false)
					{
						targetList.add((L2Character) obj);
					}
					else
					{
						return new L2Character[]
						{
							(L2Character) obj
						};
					}
				}

				src = null;

				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_AREA:
			{
				if(!(target instanceof L2Attackable || target instanceof L2Playable) || getCastRange() >= 0 && (target == null || target == activeChar || target.isAlikeDead()))
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return null;
				}

				L2Character cha;

				if(getCastRange() >= 0)
				{
					cha = target;

					if(!onlyFirst)
					{
						targetList.add(cha);
					}
					else
					{
						return new L2Character[]
						{
							cha
						};
					}
				}
				else
				{
					cha = activeChar;
				}

				boolean effectOriginIsL2PlayableInstance = cha instanceof L2Playable;

				boolean srcIsSummon = (activeChar instanceof L2Summon);
				
				L2PcInstance src = null;
				if(activeChar instanceof L2PcInstance)
				{
					src = (L2PcInstance) activeChar;
				}
				else if(activeChar instanceof L2Summon)
				{
					src = ((L2Summon) activeChar).getOwner();
				}

				int radius = getSkillRadius();

				boolean srcInArena = activeChar.isInsideZone(L2Character.ZONE_PVP) && !activeChar.isInsideZone(L2Character.ZONE_SIEGE);

				for(L2Object obj : activeChar.getKnownList().getKnownObjects().values())
				{
					if(obj == null)
					{
						continue;
					}

					if(!(obj instanceof L2Attackable || obj instanceof L2Playable))
					{
						continue;
					}

					if(obj == cha)
					{
						continue;
					}
					target = (L2Character) obj;

					if(!GeoData.getInstance().canSeeTarget(activeChar, target))
					{
						continue;
					}

					if(!target.isAlikeDead() && target != activeChar)
					{
						if(!Util.checkIfInRange(radius, obj, cha, true))
						{
							continue;
						}

						if(src != null)
						{

							if(obj instanceof L2PcInstance)
							{
								L2PcInstance trg = (L2PcInstance) obj;
								if(trg == src)
								{
									continue;
								}

								if(((L2PcInstance) obj).getAppearance().getInvisible())
								{
									continue;
								}

								if(src.getParty() != null && trg.getParty() != null && src.getParty().getPartyLeaderOID() == trg.getParty().getPartyLeaderOID())
								{
									continue;
								}

								if(trg.isInsideZone(L2Character.ZONE_PEACE))
								{
									continue;
								}

								if(!srcInArena && !(trg.isInsideZone(L2Character.ZONE_PVP) && !trg.isInsideZone(L2Character.ZONE_SIEGE)))
								{
									if(src.getAllyId() == trg.getAllyId() && src.getAllyId() != 0)
									{
										continue;
									}

									if(src.getClan() != null && trg.getClan() != null)
									{
										if(src.getClan().getClanId() == trg.getClan().getClanId())
										{
											continue;
										}
									}

									if(!src.checkPvpSkill(obj, this, srcIsSummon))
									{
										continue;
									}
								}

								trg = null;
							}
							if(obj instanceof L2Summon)
							{
								L2PcInstance trg = ((L2Summon) obj).getOwner();
								if(trg == src)
								{
									continue;
								}

								if(src.getParty() != null && trg.getParty() != null && src.getParty().getPartyLeaderOID() == trg.getParty().getPartyLeaderOID())
								{
									continue;
								}

								if(!srcInArena && !(trg.isInsideZone(L2Character.ZONE_PVP) && !trg.isInsideZone(L2Character.ZONE_SIEGE)))
								{
									if(src.getAllyId() == trg.getAllyId() && src.getAllyId() != 0)
									{
										continue;
									}

									if(src.getClan() != null && trg.getClan() != null)
									{
										if(src.getClan().getClanId() == trg.getClan().getClanId())
										{
											continue;
										}
									}

									if(!src.checkPvpSkill(trg, this, srcIsSummon))
									{
										continue;
									}
								}

								if(((L2Summon) obj).isInsideZone(L2Character.ZONE_PEACE))
								{
									continue;
								}

								trg = null;
							}
						}
						else
						{
							if(effectOriginIsL2PlayableInstance && !(obj instanceof L2Playable))
							{
								continue;
							}
						}

						targetList.add((L2Character) obj);
					}
				}

				if(targetList.size() == 0)
				{
					return null;
				}

				src = null;

				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_MULTIFACE:
			{
				if(!(target instanceof L2Attackable) && !(target instanceof L2Playable))
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return null;
				}

				if(onlyFirst == false)
				{
					targetList.add(target);
				}
				else
				{
					return new L2Character[]
					{
						target
					};
				}

				int radius = getSkillRadius();

				for(L2Object obj : activeChar.getKnownList().getKnownObjects().values())
				{
					if(obj == null)
					{
						continue;
					}

					if(!Util.checkIfInRange(radius, activeChar, obj, true))
					{
						continue;
					}

					if(obj instanceof L2Attackable && obj != target)
					{
						targetList.add((L2Character) obj);
					}

					if(targetList.size() == 0)
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_CANT_FOUND));
						return null;
					}
				}

				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_PARTY:
			{
				if(onlyFirst)
				{
					return new L2Character[]
					{
						activeChar
					};
				}

				targetList.add(activeChar);

				L2PcInstance player = null;

				if(activeChar instanceof L2Summon)
				{
					player = ((L2Summon) activeChar).getOwner();
					targetList.add(player);
				}
				else if(activeChar instanceof L2PcInstance)
				{
					player = (L2PcInstance) activeChar;
					if(activeChar.getPet() != null)
					{
						targetList.add(activeChar.getPet());
					}
				}

				if(activeChar.getParty() != null)
				{
					List<L2PcInstance> partyList = activeChar.getParty().getPartyMembers();

					for(L2PcInstance partyMember : partyList)
					{
						if(partyMember == null)
						{
							continue;
						}

						if(partyMember == player)
						{
							continue;
						}

						if(!partyMember.isDead() && Util.checkIfInRange(getSkillRadius(), activeChar, partyMember, true))
						{
							targetList.add(partyMember);

							if(partyMember.getPet() != null && !partyMember.getPet().isDead())
							{
								targetList.add(partyMember.getPet());
							}
						}
					}

					partyList = null;
				}

				player = null;

				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_PARTY_MEMBER:
			{
				if(target != null && target == activeChar || target != null && activeChar.getParty() != null && target.getParty() != null && activeChar.getParty().getPartyLeaderOID() == target.getParty().getPartyLeaderOID() || target != null && activeChar instanceof L2PcInstance && target instanceof L2Summon && activeChar.getPet() == target || target != null && activeChar instanceof L2Summon && target instanceof L2PcInstance && activeChar == target.getPet())
				{
					if(!target.isDead())
					{
						return new L2Character[]
						{
							target
						};
					}
					else
					{
						return _emptyTargetList;
					}
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return null;
				}
			}
			case TARGET_PARTY_OTHER:
			{
				if (target != null && target != activeChar && activeChar.isInParty() && target.isInParty() && activeChar.getParty().getPartyLeaderOID() == target.getParty().getPartyLeaderOID())
				{
					if (!target.isDead())
					{
						if (target instanceof L2PcInstance)
						{
							switch (getId())
							{
							// FORCE BUFFS may cancel here but there should be a proper condition
								case 426:
									if (!((L2PcInstance) target).isMageClass())
										return new L2Character[]
										{
											target
										};
									return _emptyTargetList;
									
								case 427:
									if (((L2PcInstance) target).isMageClass())
										return new L2Character[]
										{
											target
										};
									
									return _emptyTargetList;
							}
						}
						return new L2Character[]
						{
							target
						};
					}
					return _emptyTargetList;
				}
				
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				return _emptyTargetList;
			}
			case TARGET_CORPSE_ALLY:
			case TARGET_ALLY:
			{
				if(activeChar instanceof L2PcInstance)
				{
					int radius = getSkillRadius();
					L2PcInstance player = (L2PcInstance) activeChar;
					L2Clan clan = player.getClan();

					if(player.isInOlympiadMode())
					{
						return new L2Character[]
						{
							player
						};
					}

					if(targetType != SkillTargetType.TARGET_CORPSE_ALLY)
					{
						if(onlyFirst == false)
						{
							targetList.add(player);
						}
						else
						{
							return new L2Character[]
							{
								player
							};
						}
					}

					if(clan != null)
					{
						for(L2Object newTarget : activeChar.getKnownList().getKnownObjects().values())
						{
							if(newTarget == null || !(newTarget instanceof L2PcInstance))
							{
								continue;
							}

							if((((L2PcInstance) newTarget).getAllyId() == 0 || ((L2PcInstance) newTarget).getAllyId() != player.getAllyId()) && (((L2PcInstance) newTarget).getClan() == null || ((L2PcInstance) newTarget).getClanId() != player.getClanId()))
							{
								continue;
							}

							if(player.isInDuel() && (player.getDuelId() != ((L2PcInstance) newTarget).getDuelId() || player.getParty() != null && !player.getParty().getPartyMembers().contains(newTarget)))
							{
								continue;
							}

							L2Summon pet = ((L2PcInstance) newTarget).getPet();
							if(pet != null && Util.checkIfInRange(radius, activeChar, pet, true) && !onlyFirst && (targetType == SkillTargetType.TARGET_CORPSE_ALLY && pet.isDead() || targetType == SkillTargetType.TARGET_ALLY && !pet.isDead()) && player.checkPvpSkill(newTarget, this))
							{
								targetList.add(pet);
							}
							pet = null;

							if(targetType == SkillTargetType.TARGET_CORPSE_ALLY)
							{
								if(!((L2PcInstance) newTarget).isDead())
								{
									continue;
								}

								if(getSkillType() == L2SkillType.RESURRECT && ((L2PcInstance) newTarget).isInsideZone(L2Character.ZONE_SIEGE))
								{
									{ 
										// could/should be a more accurate check for siege clans         
										if (!((L2PcInstance) newTarget).getCharmOfCourage() || player.getSiegeState() == 0) 
											continue;        
									}  
								}
							}

							if(!Util.checkIfInRange(radius, activeChar, newTarget, true))
							{
								continue;
							}

							if(!player.checkPvpSkill(newTarget, this))
							{
								continue;
							}

							if(onlyFirst == false)
							{
								targetList.add((L2Character) newTarget);
							}
							else
							{
								return new L2Character[]
								{
									(L2Character) newTarget
								};
							}
						}
					}

					player = null;
					clan = null;
				}
				else if(activeChar instanceof L2Npc)
				{
					final L2Npc npc = (L2Npc) activeChar;
					if(npc.getFactionId() == null || npc.getFactionId().isEmpty())
					{
						return new L2Character[]{activeChar};
					}
					targetList.add(activeChar);
					final Collection<L2Object> objs = activeChar.getKnownList().getKnownObjects().values();
					{
						for(L2Object newTarget : objs)
						{
							if(newTarget instanceof L2Npc && npc.getFactionId().equals(((L2Npc) newTarget).getFactionId()))
							{
								if(!Util.checkIfInRange(getCastRange(), activeChar, newTarget, true))
								{
									continue;
								}

								targetList.add((L2Npc) newTarget);
							}
						}
					}
				}

				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_CORPSE_CLAN:
			case TARGET_CLAN:
			{
				if(activeChar instanceof L2PcInstance)
				{
					int radius = getSkillRadius();
					L2PcInstance player = (L2PcInstance) activeChar;
					L2Clan clan = player.getClan();

					if(player.isInOlympiadMode())
					{
						return new L2Character[]
						{
							player
						};
					}

					if(targetType != SkillTargetType.TARGET_CORPSE_CLAN)
					{
						if(onlyFirst == false)
						{
							targetList.add(player);
						}
						else
						{
							return new L2Character[]
							{
								player
							};
						}
					}

					if(clan != null)
					{
						for(L2ClanMember member : clan.getMembers())
						{
							L2PcInstance newTarget = member.getPlayerInstance();

							if(newTarget == null || newTarget == player)
							{
								continue;
							}

							if(player.isInDuel() && (player.getDuelId() != newTarget.getDuelId() || player.getParty() == null && player.getParty() != newTarget.getParty()))
							{
								continue;
							}

							L2Summon pet = newTarget.getPet();
							if(pet != null && Util.checkIfInRange(radius, activeChar, pet, true) && !onlyFirst && (targetType == SkillTargetType.TARGET_CORPSE_CLAN && pet.isDead() || targetType == SkillTargetType.TARGET_CLAN && !pet.isDead()) && player.checkPvpSkill(newTarget, this))
							{
								targetList.add(pet);
							}

							pet = null;

							if(targetType == SkillTargetType.TARGET_CORPSE_CLAN)
							{
								if(!newTarget.isDead())
								{
									continue;
								}

								if(getSkillType() == L2SkillType.RESURRECT)
								{
									Siege siege = SiegeManager.getInstance().getSiege(newTarget);
									if(siege != null && siege.getIsInProgress())
									{
										continue;
									}

									siege = null;
								}
							}

							if(!Util.checkIfInRange(radius, activeChar, newTarget, true))
							{
								continue;
							}

							if(!player.checkPvpSkill(newTarget, this))
							{
								continue;
							}

							if(!onlyFirst)
							{
								targetList.add(newTarget);
							}
							else
							{
								return new L2Character[]
								{
									newTarget
								};
							}

							newTarget = null;
						}
					}

					player = null;
					clan = null;
				}

				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_CORPSE_PLAYER:
			{
				if(target != null && target.isDead())
				{
					L2PcInstance player = null;

					if(activeChar instanceof L2PcInstance)
					{
						player = (L2PcInstance) activeChar;
					}

					L2PcInstance targetPlayer = null;

					if(target instanceof L2PcInstance)
					{
						targetPlayer = (L2PcInstance) target;
					}

					L2PetInstance targetPet = null;

					if(target instanceof L2PetInstance)
					{
						targetPet = (L2PetInstance) target;
					}

					if(player != null && (targetPlayer != null || targetPet != null))
					{
						boolean condGood = true;

						if(getSkillType() == L2SkillType.RESURRECT)
						{
							if(target.isInsideZone(L2Character.ZONE_SIEGE))
							{
								condGood = false;
								player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE));
							}

							if(targetPlayer != null)
							{
								if(targetPlayer.isReviveRequested())
								{
									if(targetPlayer.isRevivingPet())
									{
										player.sendPacket(new SystemMessage(SystemMessageId.MASTER_CANNOT_RES));
									}
									else
									{
										player.sendPacket(new SystemMessage(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED));
									}
									condGood = false;
								}
							}
							else if (targetPet != null)
							{
								if (targetPet.getOwner() != player)
								{
									if (targetPet.getOwner().isReviveRequested())
									{
										if (targetPet.getOwner().isRevivingPet())
											player.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED); // Resurrection is already been proposed.
										else
											player.sendPacket(SystemMessageId.CANNOT_RES_PET2); // A pet cannot be resurrected while it's owner is in the process of resurrecting.
										condGood = false;
									}
								}
							}
						}

						if(condGood)
						{
							if(onlyFirst == false)
							{
								targetList.add(target);
								return targetList.toArray(new L2Object[targetList.size()]);
							}
							else
							{
								return new L2Character[]
								{
									target
								};
							}
						}
					}

					player = null;
					targetPlayer = null;
					targetPet = null;
				}
				activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));

				return null;
			}
			case TARGET_CORPSE_MOB:
			{
				if(!(target instanceof L2Attackable) || !target.isDead())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return null;
				}

				switch (getSkillType())
				{ 
				case DRAIN: 
				case SUMMON: 
				{ 
					if (DecayTaskManager.getInstance().getTasks().containsKey(target)  
							&& (System.currentTimeMillis() - DecayTaskManager.getInstance().getTasks().get(target)) > DecayTaskManager.ATTACKABLE_DECAY_TIME / 2) 
					{ 
						activeChar.sendPacket(new SystemMessage(SystemMessageId.CORPSE_TOO_OLD_SKILL_NOT_USED)); 
						return null; 
					} 
				}
				default:
					break;
				}
				
				if(onlyFirst == false)
				{
					targetList.add(target);
					return targetList.toArray(new L2Object[targetList.size()]);
				}
				else
				{
					return new L2Character[]
					{
						target
					};
				}
			}
			case TARGET_AREA_CORPSE_MOB:
			{
				if(!(target instanceof L2Attackable) || !target.isDead())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return null;
				}

				if(onlyFirst == false)
				{
					targetList.add(target);
				}
				else
				{
					return new L2Character[]
					{
						target
					};
				}

				boolean srcInArena = activeChar.isInsideZone(L2Character.ZONE_PVP) && !activeChar.isInsideZone(L2Character.ZONE_SIEGE);
				L2PcInstance src = null;

				if(activeChar instanceof L2PcInstance)
				{
					src = (L2PcInstance) activeChar;
				}

				L2PcInstance trg = null;

				int radius = getSkillRadius();

				if(activeChar.getKnownList() != null)
				{
					for(L2Object obj : activeChar.getKnownList().getKnownObjects().values())
					{
						if(obj == null)
						{
							continue;
						}

						if(!(obj instanceof L2Attackable || obj instanceof L2Playable) || ((L2Character) obj).isDead() || (L2Character) obj == activeChar)
						{
							continue;
						}

						if(!Util.checkIfInRange(radius, target, obj, true))
						{
							continue;
						}

						if(!GeoData.getInstance().canSeeTarget(activeChar, obj))
						{
							continue;
						}

						if(obj instanceof L2PcInstance && src != null)
						{
							trg = (L2PcInstance) obj;

							if(src.getParty() != null && trg.getParty() != null && src.getParty().getPartyLeaderOID() == trg.getParty().getPartyLeaderOID())
							{
								continue;
							}

							if(trg.isInsideZone(L2Character.ZONE_PEACE))
							{
								continue;
							}

							if(!srcInArena && !(trg.isInsideZone(L2Character.ZONE_PVP) && !trg.isInsideZone(L2Character.ZONE_SIEGE)))
							{
								if(src.getAllyId() == trg.getAllyId() && src.getAllyId() != 0)
								{
									continue;
								}

								if(src.getClan() != null && trg.getClan() != null)
								{
									if(src.getClan().getClanId() == trg.getClan().getClanId())
									{
										continue;
									}
								}

								if(!src.checkPvpSkill(obj, this))
								{
									continue;
								}
							}
						}
						if(obj instanceof L2Summon && src != null)
						{
							trg = ((L2Summon) obj).getOwner();

							if(src.getParty() != null && trg.getParty() != null && src.getParty().getPartyLeaderOID() == trg.getParty().getPartyLeaderOID())
							{
								continue;
							}

							if(!srcInArena && !(trg.isInsideZone(L2Character.ZONE_PVP) && !trg.isInsideZone(L2Character.ZONE_SIEGE)))
							{
								if(src.getAllyId() == trg.getAllyId() && src.getAllyId() != 0)
								{
									continue;
								}

								if(src.getClan() != null && trg.getClan() != null)
								{
									if(src.getClan().getClanId() == trg.getClan().getClanId())
									{
										continue;
									}
								}

								if(!src.checkPvpSkill(trg, this))
								{
									continue;
								}
							}

							if(((L2Summon) obj).isInsideZone(L2Character.ZONE_PEACE))
							{
								continue;
							}
						}

						targetList.add((L2Character) obj);
					}
				}

				if(targetList.size() == 0)
				{
					return null;
				}

				trg = null;
				src = null;

				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_UNLOCKABLE:
			{
				if(!(target instanceof L2DoorInstance) && !(target instanceof L2ChestInstance))
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return null;
				}

				if(onlyFirst)
				{
					return new L2Character[] { target };
				}

				targetList.add(target);
				return targetList.toArray(new L2Object[targetList.size()]);
			}
			case TARGET_ITEM:
			{
				activeChar.sendMessage("Target type of skill is not currently handled");
				return null;
			}
			case TARGET_UNDEAD:
			{
				if(target instanceof L2Npc || target instanceof L2SummonInstance)
				{
					if(!target.isUndead() || target.isDead())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
						return null;
					}

					if(onlyFirst == false)
					{
						targetList.add(target);
					}
					else
					{
						return new L2Character[]
						{
							target
						};
					}

					return targetList.toArray(new L2Object[targetList.size()]);
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return null;
				}
			}
			case TARGET_AREA_UNDEAD:
			{
				L2Character cha;

				int radius = getSkillRadius();

				if(getCastRange() >= 0 && (target instanceof L2Npc || target instanceof L2SummonInstance) && target.isUndead() && !target.isAlikeDead())
				{
					cha = target;

					if(onlyFirst == false)
					{
						targetList.add(cha);
					}
					else
					{
						return new L2Character[]
						{
							cha
						};
					}
				}
				else
				{
					cha = activeChar;
				}

				if(cha != null && cha.getKnownList() != null)
				{
					for(L2Object obj : cha.getKnownList().getKnownObjects().values())
					{
						if(obj == null)
						{
							continue;
						}

						if(obj instanceof L2Npc)
						{
							target = (L2Npc) obj;
						}
						else if(obj instanceof L2SummonInstance)
						{
							target = (L2SummonInstance) obj;
						}
						else
						{
							continue;
						}

						if(!GeoData.getInstance().canSeeTarget(activeChar, target))
						{
							continue;
						}

						if(!target.isAlikeDead())
						{
							if(!target.isUndead())
							{
								continue;
							}

							if(!Util.checkIfInRange(radius, cha, obj, true))
							{
								continue;
							}

							if(onlyFirst == false)
							{
								targetList.add((L2Character) obj);
							}
							else
							{
								return new L2Character[]
								{
									(L2Character) obj
								};
							}
						}
					}
				}

				if(targetList.size() == 0)
				{
					return null;
				}

				cha = null;

				return targetList.toArray(new L2Character[targetList.size()]);
			}
			case TARGET_ENEMY_ALLY:
			{
				int radius = getSkillRadius();
				L2Character newTarget;

				if(getCastRange() > -1 && target != null)
				{
					newTarget = target;
				}
				else
				{
					newTarget = activeChar;
				}

				if(newTarget != activeChar || isSkillTypeOffensive())
				{
					targetList.add(newTarget);
				}

				for(L2Character obj : activeChar.getKnownList().getKnownCharactersInRadius(radius))
				{
					if(obj == newTarget || obj == activeChar)
					{
						continue;
					}

					if(obj instanceof L2Attackable)
					{
						if(!obj.isAlikeDead())
						{
							if(activeChar instanceof L2PcInstance && !((L2PcInstance)activeChar).checkPvpSkill(obj, this))
							{
								continue;
							}

							if((activeChar instanceof L2PcInstance && obj instanceof L2PcInstance) && (((L2PcInstance)activeChar).getClanId() != ((L2PcInstance)obj).getClanId() || (((L2PcInstance)activeChar).getAllyId() != ((L2PcInstance)obj).getAllyId() && ((((L2PcInstance)activeChar).getParty() != null && ((L2PcInstance)obj).getParty() != null) && ((L2PcInstance)activeChar).getParty().getPartyLeaderOID() != ((L2PcInstance)obj).getParty().getPartyLeaderOID() ))))
							{
								continue;
							}

							targetList.add(obj);
						}
					}
				}
			}
			case TARGET_ENEMY_SUMMON:
			{
				if(target != null && target instanceof L2Summon)
				{
					L2Summon targetSummon = (L2Summon) target;
					if(activeChar instanceof L2PcInstance && activeChar.getPet() != targetSummon && !targetSummon.isDead() && (targetSummon.getOwner().getPvpFlag() != 0 || targetSummon.getOwner().getKarma() > 0) || (targetSummon.getOwner().isInsideZone(L2Character.ZONE_PVP) && ((L2PcInstance)activeChar).isInsideZone(L2Character.ZONE_PVP)) || (targetSummon.getOwner().isInDuel() && ((L2PcInstance)activeChar).isInDuel() && targetSummon.getOwner().getDuelId() == ((L2PcInstance)activeChar).getDuelId()))
					{
						return new L2Character[]
						{
							targetSummon
						};
					}

					targetSummon = null;
				}
				return null;
			}
			case TARGET_SIEGE:
			{
				if(target != null && !target.isDead() && (target instanceof L2DoorInstance || target instanceof L2ControlTowerInstance))
				{
					return new L2Character[]
					{
						target
					};
				}

				return null;
			}
			case TARGET_TYRANNOSAURUS:
			{
				if(target instanceof L2PcInstance)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					return null;
				}

				if(target instanceof L2MonsterInstance && (((L2MonsterInstance) target).getNpcId() == 22217 || ((L2MonsterInstance) target).getNpcId() == 22216 || ((L2MonsterInstance) target).getNpcId() == 22215))
				{
					return new L2Character[]
					{
						target
					};
				}

				return null;
			}
			case TARGET_AREA_AIM_CORPSE:
			{
				if(target != null && target.isDead())
				{
					return new L2Character[]
					{
						target
					};
				}

				return null;
			}
			case TARGET_COUPLE:
			{
				if(target != null && target instanceof L2PcInstance)
				{
					int _chaid = activeChar.getObjectId();
					int targetId = target.getObjectId();
					for(Wedding cl: CoupleManager.getInstance().getCouples())
					{
						if((cl.getPlayer1Id()==_chaid && cl.getPlayer2Id()==targetId) || (cl.getPlayer2Id()==_chaid && cl.getPlayer1Id()==targetId))
						{
							return new L2Character[]{target};
						}
					}
				}

				return null;
			}
			case TARGET_CLAN_MEMBER:
			{
				if(activeChar instanceof L2Npc)
				{
					final L2Npc npc = (L2Npc) activeChar;
					if(npc.getFactionId() == null || npc.getFactionId().isEmpty())
					{
						return new L2Character[]{activeChar};
					}
					final Collection<L2Object> objs = activeChar.getKnownList().getKnownObjects().values();
					for(L2Object newTarget : objs)
					{
						if(newTarget instanceof L2Npc && npc.getFactionId().equals(((L2Npc) newTarget).getFactionId()))
						{
							if(!Util.checkIfInRange(getCastRange(), activeChar, newTarget, true))
							{
								continue;
							}

							if(((L2Npc) newTarget).getFirstEffect(this) != null)
							{
								continue;
							}

							targetList.add((L2Npc) newTarget);
							break;
						}
					}

					if(targetList.isEmpty())
					{
						targetList.add(npc);
					}
				}

				return null;
			}
			default:
			{
				activeChar.sendMessage("Target type of skill is not currently handled");
				return null;
			}
		}
	}

	public final L2Object[] getTargetList(L2Character activeChar)
	{
		return getTargetList(activeChar, false);
	}

	public final L2Object getFirstOfTargetList(L2Character activeChar)
	{
		L2Object[] targets;

		targets = getTargetList(activeChar, true);
		if(targets == null || targets.length == 0)
		{
			return null;
		}
		else
		{
			return targets[0];
		}
	}

	/*
	 * Check if should be target added to the target list false if target is dead, target same as caster, target inside peace zone, target in the same party with caster, caster can see target. Additional checks if not in PvP zones (arena, siege): target in not the same clan and alliance with caster,
	 * and usual skill PvP check. Caution: distance is not checked.
	 */
	public static final boolean checkForAreaOffensiveSkills(L2Character caster, L2Character target, L2Skill skill, boolean sourceInArena)
	{
		if (target == null || target.isDead() || target == caster)
			return false;
		
		final L2PcInstance player = caster.getActingPlayer();
		final L2PcInstance targetPlayer = target.getActingPlayer();
		if (player != null)
		{
			if (targetPlayer != null)
			{
				if (targetPlayer == caster || targetPlayer == player)
					return false;
				
				if (targetPlayer.inObserverMode())
					return false;
				
				if(targetPlayer.getPvpFlag() < 0)
					return false;
				
				if (skill.isOffensive() && player.getSiegeState() > 0 && player.isInsideZone(L2Character.ZONE_SIEGE) && player.getSiegeState() == targetPlayer.getSiegeState())
					return false;
				
				if (target.isInsideZone(L2Character.ZONE_PEACE))
					return false;
				
				if (player.isInParty() && targetPlayer.isInParty())
				{
					// Same party
					if (player.getParty().getPartyLeaderOID() == targetPlayer.getParty().getPartyLeaderOID())
						return false;
					
					// Same commandchannel
					if (player.getParty().getCommandChannel() != null && player.getParty().getCommandChannel() == targetPlayer.getParty().getCommandChannel())
						return false;
				}
				
				if (!sourceInArena && !(targetPlayer.isInsideZone(L2Character.ZONE_PVP) && !targetPlayer.isInsideZone(L2Character.ZONE_SIEGE)))
				{
					if (player.getAllyId() != 0 && player.getAllyId() == targetPlayer.getAllyId())
						return false;
					
					if (player.getClanId() != 0 && player.getClanId() == targetPlayer.getClanId())
						return false;
					
					if (!player.checkPvpSkill(targetPlayer, skill))
						return false;
				}
			}
		}
		else
		{
			// target is mob
			if (targetPlayer == null && target instanceof L2Attackable && caster instanceof L2Attackable)
			{
				String casterEnemyClan = ((L2Attackable) caster).getEnemyClan();
				if (casterEnemyClan == null || casterEnemyClan.isEmpty())
					return false;
				
				String targetClan = ((L2Attackable) target).getClan();
				if (targetClan == null || targetClan.isEmpty())
					return false;
				
				if (!casterEnemyClan.equals(targetClan))
					return false;
			}
		}
		
		if (geoEnabled && !GeoData.getInstance().canSeeTarget(caster, target))
			return false;
		
		return true;
	}

	
	public final Func[] getStatFuncs(L2Effect effect, L2Character player)
	{
		if(!(player instanceof L2PcInstance) && !(player instanceof L2Attackable) && !(player instanceof L2Summon))
		{
			return _emptyFunctionSet;
		}

		if(_funcTemplates == null)
		{
			return _emptyFunctionSet;
		}

		List<Func> funcs = new FastList<Func>();

		for(FuncTemplate t : _funcTemplates)
		{
			Env env = new Env();
			env.player = player;
			env.skill = this;
			Func f = t.getFunc(env, this);

			if(f != null)
			{
				funcs.add(f);
			}
		}

		if(funcs.size() == 0)
		{
			return _emptyFunctionSet;
		}

		return funcs.toArray(new Func[funcs.size()]);
	}

	public boolean hasEffects()
	{
		return _effectTemplates != null && _effectTemplates.length > 0;
	}

	public final L2Effect[] getEffects(L2Character effector, L2Character effected)
	{
		if(isPassive())
		{
			return _emptyEffectSet;
		}

		if(_effectTemplates == null)
		{
			return _emptyEffectSet;
		}

		if(effector != effected && effected.isInvul())
		{
			return _emptyEffectSet;
		}

		List<L2Effect> effects = new FastList<L2Effect>();

		boolean skillMastery = false;

		if(!isToggle() && Formulas.getInstance().calcSkillMastery(effector))
		{
			skillMastery = true;
		}

		if(getSkillType() == L2SkillType.BUFF)
		{
			for(L2Effect ef: effector.getAllEffects())
			{
				if(ef.getSkill().getId() == getId() && ef.getSkill().getLevel() > getLevel())
				{
					return _emptyEffectSet;
				}
			}
		}

		for(EffectTemplate et : _effectTemplates)
		{
			Env env = new Env();
			env.player = effector;
			env.target = effected;
			env.skill = this;
			env.skillMastery = skillMastery;
			L2Effect e = et.getEffect(env);
			if(e != null)
			{
				effects.add(e);
			}
			e = null;
		}

		if(effects.size() == 0)
		{
			return _emptyEffectSet;
		}

		return effects.toArray(new L2Effect[effects.size()]);
	}

	public final L2Effect[] getEffectsSelf(L2Character effector)
	{
		if(isPassive())
		{
			return _emptyEffectSet;
		}

		if(_effectTemplatesSelf == null)
		{
			return _emptyEffectSet;
		}

		List<L2Effect> effects = new FastList<L2Effect>();

		for(EffectTemplate et : _effectTemplatesSelf)
		{
			Env env = new Env();
			env.player = effector;
			env.target = effector;
			env.skill = this;
			L2Effect e = et.getEffect(env);
			if(e != null)
			{
				if(e.getEffectType() == L2EffectType.CHARGE)
				{
					env.skill = SkillTable.getInstance().getInfo(8, effector.getSkillLevel(8));
					EffectCharge effect = (EffectCharge) env.target.getFirstEffect(L2EffectType.CHARGE);
					if(effect != null)
					{
						int effectcharge = effect.getLevel();
						if(effectcharge < _numCharges)
						{
							effectcharge++;
							effect.addNumCharges(effectcharge);
							if(env.target instanceof L2PcInstance)
							{
								env.target.sendPacket(new EtcStatusUpdate((L2PcInstance) env.target));
								env.target.sendPacket(new SystemMessage(SystemMessageId.FORCE_INCREASED_TO_S1).addNumber(effectcharge));
							}
						}
					}
					else
					{
						effects.add(e);
					}
				}
				else
				{
					effects.add(e);
				}
			}

			e = null;
		}

		if(effects.size() == 0)
		{
			return _emptyEffectSet;
		}

		return effects.toArray(new L2Effect[effects.size()]);
	}

	public final void attach(FuncTemplate f)
	{
		if(_funcTemplates == null)
		{
			_funcTemplates = new FuncTemplate[]
			{
				f
			};
		}
		else
		{
			int len = _funcTemplates.length;
			FuncTemplate[] tmp = new FuncTemplate[len + 1];
			System.arraycopy(_funcTemplates, 0, tmp, 0, len);
			tmp[len] = f;
			_funcTemplates = tmp;
			tmp = null;
		}
	}

	public final void attach(EffectTemplate effect)
	{
		if(_effectTemplates == null)
		{
			_effectTemplates = new EffectTemplate[]
			{
				effect
			};
		}
		else
		{
			int len = _effectTemplates.length;
			EffectTemplate[] tmp = new EffectTemplate[len + 1];
			System.arraycopy(_effectTemplates, 0, tmp, 0, len);
			tmp[len] = effect;
			_effectTemplates = tmp;
			tmp = null;
		}
	}

	public final void attachSelf(EffectTemplate effect)
	{
		if(_effectTemplatesSelf == null)
		{
			_effectTemplatesSelf = new EffectTemplate[]
			{
				effect
			};
		}
		else
		{
			int len = _effectTemplatesSelf.length;
			EffectTemplate[] tmp = new EffectTemplate[len + 1];
			System.arraycopy(_effectTemplatesSelf, 0, tmp, 0, len);
			tmp[len] = effect;
			_effectTemplatesSelf = tmp;
			tmp = null;
		}
	}

	public final void attach(Condition c, boolean itemOrWeapon)
	{
		if(itemOrWeapon)
		{
			_itemPreCondition = c;
		}
		else
		{
			_preCondition = c;
		}
	}

	public boolean checkPartyClan(L2Character activeChar, L2Object target)
	{
		if(activeChar instanceof L2PcInstance && target instanceof L2PcInstance)
		{
			L2PcInstance targetChar = (L2PcInstance) target;
			L2PcInstance activeCh = (L2PcInstance) activeChar;

			if(activeCh.isInOlympiadMode() && activeCh.isOlympiadStart() && targetChar.isInOlympiadMode() && targetChar.isOlympiadStart())
			{
				return false;
			}

			if(activeCh.isInDuel() && targetChar.isInDuel() && activeCh.getDuelId() == targetChar.getDuelId())
			{
				return false;
			}

			if(activeCh.getParty() != null && targetChar.getParty() != null && activeCh.getParty().getPartyLeaderOID() == targetChar.getParty().getPartyLeaderOID())
			{
				return true;
			}

			if(activeCh.getClan() != null && targetChar.getClan() != null && activeCh.getClan().getClanId() == targetChar.getClan().getClanId())
			{
				return true;
			}

			targetChar = null;
			activeCh = null;
		}
		return false;
	}

	@Override
	public String toString()
	{
		return "" + _name + "[id=" + _id + ",lvl=" + _level + "]";
	}

	public final String getFlyType()
	{
		return _flyType;
	}

	public final int getFlyRadius()
	{
		return _flyRadius;
	}

	public final float getFlyCourse()
	{
		return _flyCourse;
	}

	public final int getTargetConsumeId()
	{
		return _targetConsumeId;
	}

	public final int getTargetConsume()
	{
		return _targetConsume;
	}
	
	public boolean hasSelfEffects()
	{
		return (_effectTemplatesSelf != null && _effectTemplatesSelf.length > 0);
	}
}