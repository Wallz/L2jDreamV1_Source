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
package com.src.gameserver.skills;

import java.util.logging.Logger;

import com.src.Config;
import com.src.gameserver.managers.ClanHallManager;
import com.src.gameserver.managers.SiegeManager;
import com.src.gameserver.model.L2SiegeClan;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.instance.L2DoorInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.model.entity.ClanHall;
import com.src.gameserver.model.entity.sevensigns.SevenSigns;
import com.src.gameserver.model.entity.sevensigns.SevenSignsFestival;
import com.src.gameserver.model.entity.siege.Siege;
import com.src.gameserver.model.itemcontainer.Inventory;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.conditions.ConditionPlayerState;
import com.src.gameserver.skills.conditions.ConditionPlayerState.CheckPlayerState;
import com.src.gameserver.skills.conditions.ConditionUsingItemType;
import com.src.gameserver.skills.funcs.Func;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.templates.chars.L2PcTemplate;
import com.src.gameserver.templates.item.L2Armor;
import com.src.gameserver.templates.item.L2Weapon;
import com.src.gameserver.templates.item.L2WeaponType;
import com.src.gameserver.templates.skills.L2SkillType;
import com.src.gameserver.util.Util;
import com.src.util.random.Rnd;

public final class Formulas
{
	protected static final Logger _log = Logger.getLogger(L2Character.class.getName());
	private static final int HP_REGENERATE_PERIOD = 3000;

	public static final byte SHIELD_DEFENSE_FAILED = 0; // no shield defense
	public static final byte SHIELD_DEFENSE_SUCCEED = 1; // normal shield defense
	public static final byte SHIELD_DEFENSE_PERFECT_BLOCK = 2; // perfect block
	
	public static final byte SKILL_REFLECT_FAILED = 0; // no reflect
	public static final byte SKILL_REFLECT_SUCCEED = 1; // normal reflect, some damage reflected some other not
	public static final byte SKILL_REFLECT_VENGEANCE = 2; // 100% of the damage affect both
	
	private static final byte MELEE_ATTACK_RANGE = 40;
	
	public static final int MAX_STAT_VALUE = 100;

	private static final double[] STRCompute = new double[]
	{
		1.036, 34.845
	};
	private static final double[] INTCompute = new double[]
	{
		1.020, 31.375
	};
	private static final double[] DEXCompute = new double[]
	{
		1.009, 19.360
	};
	private static final double[] WITCompute = new double[]
	{
		1.050, 20.000
	};
	private static final double[] CONCompute = new double[]
	{
		1.030, 27.632
	};
	private static final double[] MENCompute = new double[]
	{
		1.010, -0.060
	};

	public static final double[] WITbonus = new double[MAX_STAT_VALUE];
	public static final double[] MENbonus = new double[MAX_STAT_VALUE];
	public static final double[] INTbonus = new double[MAX_STAT_VALUE];
	public static final double[] STRbonus = new double[MAX_STAT_VALUE];
	public static final double[] DEXbonus = new double[MAX_STAT_VALUE];
	public static final double[] CONbonus = new double[MAX_STAT_VALUE];

	static
	{
		for(int i = 0; i < STRbonus.length; i++)
		{
			STRbonus[i] = Math.floor(Math.pow(STRCompute[0], i - STRCompute[1]) * 100 + .5d) / 100;
		}
		for(int i = 0; i < INTbonus.length; i++)
		{
			INTbonus[i] = Math.floor(Math.pow(INTCompute[0], i - INTCompute[1]) * 100 + .5d) / 100;
		}
		for(int i = 0; i < DEXbonus.length; i++)
		{
			DEXbonus[i] = Math.floor(Math.pow(DEXCompute[0], i - DEXCompute[1]) * 100 + .5d) / 100;
		}
		for(int i = 0; i < WITbonus.length; i++)
		{
			WITbonus[i] = Math.floor(Math.pow(WITCompute[0], i - WITCompute[1]) * 100 + .5d) / 100;
		}
		for(int i = 0; i < CONbonus.length; i++)
		{
			CONbonus[i] = Math.floor(Math.pow(CONCompute[0], i - CONCompute[1]) * 100 + .5d) / 100;
		}
		for(int i = 0; i < MENbonus.length; i++)
		{
			MENbonus[i] = Math.floor(Math.pow(MENCompute[0], i - MENCompute[1]) * 100 + .5d) / 100;
		}
	}

	static class FuncAddLevel3 extends Func
	{
		static final FuncAddLevel3[] _instancies = new FuncAddLevel3[Stats.NUM_STATS];

		static Func getInstance(final Stats stat)
		{
			final int pos = stat.ordinal();

			if(_instancies[pos] == null)
			{
				_instancies[pos] = new FuncAddLevel3(stat);
			}
			return _instancies[pos];
		}

		private FuncAddLevel3(final Stats pStat)
		{
			super(pStat, 0x10, null);
		}

		@Override
		public void calc(final Env env)
		{
			env.value += env.player.getLevel() / 3.0;
		}
	}

	static class FuncMultLevelMod extends Func
	{
		static final FuncMultLevelMod[] _instancies = new FuncMultLevelMod[Stats.NUM_STATS];

		static Func getInstance(final Stats stat)
		{
			final int pos = stat.ordinal();

			if(_instancies[pos] == null)
			{
				_instancies[pos] = new FuncMultLevelMod(stat);
			}
			return _instancies[pos];
		}

		private FuncMultLevelMod(final Stats pStat)
		{
			super(pStat, 0x20, null);
		}

		@Override
		public void calc(final Env env)
		{
			env.value *= env.player.getLevelMod();
		}
	}

	static class FuncMultRegenResting extends Func
	{
		static final FuncMultRegenResting[] _instancies = new FuncMultRegenResting[Stats.NUM_STATS];

		static Func getInstance(Stats stat)
		{
			int pos = stat.ordinal();

			if(_instancies[pos] == null)
			{
				_instancies[pos] = new FuncMultRegenResting(stat);
			}

			return _instancies[pos];
		}

		private FuncMultRegenResting(Stats pStat)
		{
			super(pStat, 0x20, null);
			setCondition(new ConditionPlayerState(CheckPlayerState.RESTING, true));
		}

		@Override
		public void calc(Env env)
		{
			if(!cond.test(env))
			{
				return;
			}

			env.value *= 1.45;
		}
	}

	static class FuncPAtkMod extends Func
	{
		static final FuncPAtkMod _fpa_instance = new FuncPAtkMod();

		static Func getInstance()
		{
			return _fpa_instance;
		}

		private FuncPAtkMod()
		{
			super(Stats.POWER_ATTACK, 0x30, null);
		}

		@Override
		public void calc(Env env)
		{
			env.value *= STRbonus[env.player.getSTR()] * env.player.getLevelMod();
		}
	}

	static class FuncMAtkMod extends Func
	{
		static final FuncMAtkMod _fma_instance = new FuncMAtkMod();

		static Func getInstance()
		{
			return _fma_instance;
		}

		private FuncMAtkMod()
		{
			super(Stats.MAGIC_ATTACK, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			double intb = INTbonus[env.player.getINT()];
			double lvlb = env.player.getLevelMod();
			env.value *= lvlb * lvlb * intb * intb;
		}
	}

	static class FuncMDefMod extends Func
	{
		static final FuncMDefMod _fmm_instance = new FuncMDefMod();

		static Func getInstance()
		{
			return _fmm_instance;
		}

		private FuncMDefMod()
		{
			super(Stats.MAGIC_DEFENCE, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			if(env.player instanceof L2PcInstance)
			{
				L2PcInstance p = (L2PcInstance) env.player;
				if(p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LFINGER) != null)
				{
					env.value -= 5;
				}

				if(p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RFINGER) != null)
				{
					env.value -= 5;
				}

				if(p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEAR) != null)
				{
					env.value -= 9;
				}

				if(p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_REAR) != null)
				{
					env.value -= 9;
				}

				if(p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_NECK) != null)
				{
					env.value -= 13;
				}
			}

			env.value *= MENbonus[env.player.getMEN()] * env.player.getLevelMod();
		}
	}

	static class FuncPDefMod extends Func
	{
		static final FuncPDefMod _fmm_instance = new FuncPDefMod();

		static Func getInstance()
		{
			return _fmm_instance;
		}

		private FuncPDefMod()
		{
			super(Stats.POWER_DEFENCE, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			if(env.player instanceof L2PcInstance)
			{
				L2PcInstance p = (L2PcInstance) env.player;
				if(p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HEAD) != null)
				{
					env.value -= 12;
				}

				if(p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST) != null)
				{
					env.value -= p.getClassId().isMage() ? 15 : 31;
				}

				if(p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS) != null)
				{
					env.value -= p.getClassId().isMage() ? 8 : 18;
				}

				if(p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES) != null)
				{
					env.value -= 8;
				}

				if(p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET) != null)
				{
					env.value -= 7;
				}
			}

			env.value *= env.player.getLevelMod();
		}
	}

	static class FuncBowAtkRange extends Func
	{
		private static final FuncBowAtkRange _fbar_instance = new FuncBowAtkRange();

		static Func getInstance()
		{
			return _fbar_instance;
		}

		private FuncBowAtkRange()
		{
			super(Stats.POWER_ATTACK_RANGE, 0x20, null);
			setCondition(new ConditionUsingItemType(L2WeaponType.BOW.mask()));
		}

		@Override
		public void calc(Env env)
		{
			if(!cond.test(env))
			{
				return;
			}
			env.value += 460;
		}
	}

	static class FuncAtkAccuracy extends Func
	{
		static final FuncAtkAccuracy _faa_instance = new FuncAtkAccuracy();

		static Func getInstance()
		{
			return _faa_instance;
		}

		private FuncAtkAccuracy()
		{
			super(Stats.ACCURACY_COMBAT, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Character p = env.player;

			env.value += Math.sqrt(p.getDEX()) * 6;
			env.value += p.getLevel();
			if(p instanceof L2Summon)
			{
				env.value += p.getLevel() < 60 ? 4 : 5;
			}
		}
	}

	static class FuncAtkEvasion extends Func
	{
		static final FuncAtkEvasion _fae_instance = new FuncAtkEvasion();

		static Func getInstance()
		{
			return _fae_instance;
		}

		private FuncAtkEvasion()
		{
			super(Stats.EVASION_RATE, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Character p = env.player;

			env.value += Math.sqrt(p.getDEX()) * 6;
			env.value += p.getLevel();
		}
	}

	static class FuncAtkCritical extends Func
	{
		static final FuncAtkCritical _fac_instance = new FuncAtkCritical();

		static Func getInstance()
		{
			return _fac_instance;
		}

		private FuncAtkCritical()
		{
			super(Stats.CRITICAL_RATE, 0x30, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Character p = env.player;
			if(p instanceof L2Summon)
			{
				env.value = 40;
			}
			else if(p instanceof L2PcInstance && p.getActiveWeaponInstance() == null)
			{
				env.value = 40 * DEXbonus[p.getDEX()];
			}
			else
			{
				env.value *= DEXbonus[p.getDEX()];
				env.value *= 10;
			}

			env.baseValue = env.value;
		}
	}

	static class FuncMAtkCritical extends Func
	{
		static final FuncMAtkCritical _fac_instance = new FuncMAtkCritical();

		static Func getInstance()
		{
			return _fac_instance;
		}

		private FuncMAtkCritical()
		{
			super(Stats.MCRITICAL_RATE, 0x30, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Character p = env.player;
			if(p instanceof L2Summon)
			{
				env.value = 8;
			}
			else if(p instanceof L2PcInstance && p.getActiveWeaponInstance() == null)
			{
				env.value = 8;
			}
			else
			{
				env.value *= WITbonus[p.getWIT()];
			}
		}
	}

	static class FuncMoveSpeed extends Func
	{
		static final FuncMoveSpeed _fms_instance = new FuncMoveSpeed();

		static Func getInstance()
		{
			return _fms_instance;
		}

		private FuncMoveSpeed()
		{
			super(Stats.RUN_SPEED, 0x30, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcInstance p = (L2PcInstance) env.player;
			env.value *= DEXbonus[p.getDEX()];
		}
	}

	static class FuncPAtkSpeed extends Func
	{
		static final FuncPAtkSpeed _fas_instance = new FuncPAtkSpeed();

		static Func getInstance()
		{
			return _fas_instance;
		}

		private FuncPAtkSpeed()
		{
			super(Stats.POWER_ATTACK_SPEED, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcInstance p = (L2PcInstance) env.player;
			env.value *= DEXbonus[p.getDEX()];
		}
	}

	static class FuncMAtkSpeed extends Func
	{
		static final FuncMAtkSpeed _fas_instance = new FuncMAtkSpeed();

		static Func getInstance()
		{
			return _fas_instance;
		}

		private FuncMAtkSpeed()
		{
			super(Stats.MAGIC_ATTACK_SPEED, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcInstance p = (L2PcInstance) env.player;
			env.value *= WITbonus[p.getWIT()];
		}
	}

	static class FuncHennaSTR extends Func
	{
		static final FuncHennaSTR _fh_instance = new FuncHennaSTR();

		static Func getInstance()
		{
			return _fh_instance;
		}

		private FuncHennaSTR()
		{
			super(Stats.STAT_STR, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcInstance pc = (L2PcInstance) env.player;
			if(pc != null)
			{
				env.value += pc.getHennaStatSTR();
			}
		}
	}

	static class FuncHennaDEX extends Func
	{
		static final FuncHennaDEX _fh_instance = new FuncHennaDEX();

		static Func getInstance()
		{
			return _fh_instance;
		}

		private FuncHennaDEX()
		{
			super(Stats.STAT_DEX, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcInstance pc = (L2PcInstance) env.player;
			if(pc != null)
			{
				env.value += pc.getHennaStatDEX();
			}
		}
	}

	static class FuncHennaINT extends Func
	{
		static final FuncHennaINT _fh_instance = new FuncHennaINT();

		static Func getInstance()
		{
			return _fh_instance;
		}

		private FuncHennaINT()
		{
			super(Stats.STAT_INT, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcInstance pc = (L2PcInstance) env.player;
			if(pc != null)
			{
				env.value += pc.getHennaStatINT();
			}
		}
	}

	static class FuncHennaMEN extends Func
	{
		static final FuncHennaMEN _fh_instance = new FuncHennaMEN();

		static Func getInstance()
		{
			return _fh_instance;
		}

		private FuncHennaMEN()
		{
			super(Stats.STAT_MEN, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcInstance pc = (L2PcInstance) env.player;
			if(pc != null)
			{
				env.value += pc.getHennaStatMEN();
			}
		}
	}

	static class FuncHennaCON extends Func
	{
		static final FuncHennaCON _fh_instance = new FuncHennaCON();

		static Func getInstance()
		{
			return _fh_instance;
		}

		private FuncHennaCON()
		{
			super(Stats.STAT_CON, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcInstance pc = (L2PcInstance) env.player;
			if(pc != null)
			{
				env.value += pc.getHennaStatCON();
			}
		}
	}

	static class FuncHennaWIT extends Func
	{
		static final FuncHennaWIT _fh_instance = new FuncHennaWIT();

		static Func getInstance()
		{
			return _fh_instance;
		}

		private FuncHennaWIT()
		{
			super(Stats.STAT_WIT, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcInstance pc = (L2PcInstance) env.player;
			if(pc != null)
			{
				env.value += pc.getHennaStatWIT();
			}
		}
	}

	static class FuncMaxHpAdd extends Func
	{
		static final FuncMaxHpAdd _fmha_instance = new FuncMaxHpAdd();

		static Func getInstance()
		{
			return _fmha_instance;
		}

		private FuncMaxHpAdd()
		{
			super(Stats.MAX_HP, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcTemplate t = (L2PcTemplate) env.player.getTemplate();
			int lvl = env.player.getLevel() - t.classBaseLevel;
			double hpmod = t.lvlHpMod * lvl;
			double hpmax = (t.lvlHpAdd + hpmod) * lvl;
			double hpmin = t.lvlHpAdd * lvl + hpmod;
			env.value += (hpmax + hpmin) / 2;
		}
	}

	static class FuncMaxHpMul extends Func
	{
		static final FuncMaxHpMul _fmhm_instance = new FuncMaxHpMul();

		static Func getInstance()
		{
			return _fmhm_instance;
		}

		private FuncMaxHpMul()
		{
			super(Stats.MAX_HP, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcInstance p = (L2PcInstance) env.player;
			env.value *= CONbonus[p.getCON()];
		}
	}

	static class FuncMaxCpAdd extends Func
	{
		static final FuncMaxCpAdd _fmca_instance = new FuncMaxCpAdd();

		static Func getInstance()
		{
			return _fmca_instance;
		}

		private FuncMaxCpAdd()
		{
			super(Stats.MAX_CP, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcTemplate t = (L2PcTemplate) env.player.getTemplate();
			int lvl = env.player.getLevel() - t.classBaseLevel;
			double cpmod = t.lvlCpMod * lvl;
			double cpmax = (t.lvlCpAdd + cpmod) * lvl;
			double cpmin = t.lvlCpAdd * lvl + cpmod;
			env.value += (cpmax + cpmin) / 2;
		}
	}

	static class FuncMaxCpMul extends Func
	{
		static final FuncMaxCpMul _fmcm_instance = new FuncMaxCpMul();

		static Func getInstance()
		{
			return _fmcm_instance;
		}

		private FuncMaxCpMul()
		{
			super(Stats.MAX_CP, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcInstance p = (L2PcInstance) env.player;
			env.value *= CONbonus[p.getCON()];
		}
	}

	static class FuncMaxMpAdd extends Func
	{
		static final FuncMaxMpAdd _fmma_instance = new FuncMaxMpAdd();

		static Func getInstance()
		{
			return _fmma_instance;
		}

		private FuncMaxMpAdd()
		{
			super(Stats.MAX_MP, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcTemplate t = (L2PcTemplate) env.player.getTemplate();
			int lvl = env.player.getLevel() - t.classBaseLevel;
			double mpmod = t.lvlMpMod * lvl;
			double mpmax = (t.lvlMpAdd + mpmod) * lvl;
			double mpmin = t.lvlMpAdd * lvl + mpmod;
			env.value += (mpmax + mpmin) / 2;
		}
	}

	static class FuncMaxMpMul extends Func
	{
		static final FuncMaxMpMul _fmmm_instance = new FuncMaxMpMul();

		static Func getInstance()
		{
			return _fmmm_instance;
		}

		private FuncMaxMpMul()
		{
			super(Stats.MAX_MP, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcInstance p = (L2PcInstance) env.player;
			env.value *= MENbonus[p.getMEN()];
		}
	}
	
	private static final Formulas _instance = new Formulas();

	public static Formulas getInstance()
	{
		return _instance;
	}

	private Formulas()
	{
	}

	public int getRegeneratePeriod(L2Character cha)
	{
		if(cha instanceof L2DoorInstance)
		{
			return HP_REGENERATE_PERIOD * 100;
		}

		return HP_REGENERATE_PERIOD;
	}

	public Calculator[] getStdNPCCalculators()
	{
		Calculator[] std = new Calculator[Stats.NUM_STATS];

		std[Stats.ACCURACY_COMBAT.ordinal()] = new Calculator();
		std[Stats.ACCURACY_COMBAT.ordinal()].addFunc(FuncAtkAccuracy.getInstance());

		std[Stats.EVASION_RATE.ordinal()] = new Calculator();
		std[Stats.EVASION_RATE.ordinal()].addFunc(FuncAtkEvasion.getInstance());

		return std;
	}

	public void addFuncsToNewCharacter(L2Character cha)
	{
		if(cha instanceof L2PcInstance)
		{
			cha.addStatFunc(FuncMaxHpAdd.getInstance());
			cha.addStatFunc(FuncMaxHpMul.getInstance());
			cha.addStatFunc(FuncMaxCpAdd.getInstance());
			cha.addStatFunc(FuncMaxCpMul.getInstance());
			cha.addStatFunc(FuncMaxMpAdd.getInstance());
			cha.addStatFunc(FuncMaxMpMul.getInstance());
			cha.addStatFunc(FuncBowAtkRange.getInstance());
			cha.addStatFunc(FuncPAtkMod.getInstance());
			cha.addStatFunc(FuncMAtkMod.getInstance());
			cha.addStatFunc(FuncPDefMod.getInstance());
			cha.addStatFunc(FuncMDefMod.getInstance());
			cha.addStatFunc(FuncAtkCritical.getInstance());
			cha.addStatFunc(FuncMAtkCritical.getInstance());
			cha.addStatFunc(FuncAtkAccuracy.getInstance());
			cha.addStatFunc(FuncAtkEvasion.getInstance());
			cha.addStatFunc(FuncPAtkSpeed.getInstance());
			cha.addStatFunc(FuncMAtkSpeed.getInstance());
			cha.addStatFunc(FuncMoveSpeed.getInstance());
			cha.addStatFunc(FuncHennaSTR.getInstance());
			cha.addStatFunc(FuncHennaDEX.getInstance());
			cha.addStatFunc(FuncHennaINT.getInstance());
			cha.addStatFunc(FuncHennaMEN.getInstance());
			cha.addStatFunc(FuncHennaCON.getInstance());
			cha.addStatFunc(FuncHennaWIT.getInstance());
		}
		else if(cha instanceof L2PetInstance)
		{
			cha.addStatFunc(FuncPAtkMod.getInstance());
			cha.addStatFunc(FuncMAtkMod.getInstance());
			cha.addStatFunc(FuncPDefMod.getInstance());
			cha.addStatFunc(FuncMDefMod.getInstance());
		}
		else if(cha instanceof L2Summon)
		{
			cha.addStatFunc(FuncAtkCritical.getInstance());
			cha.addStatFunc(FuncMAtkCritical.getInstance());
			cha.addStatFunc(FuncAtkAccuracy.getInstance());
			cha.addStatFunc(FuncAtkEvasion.getInstance());
		}
	}

	public final double calcHpRegen(L2Character cha)
	{
		double init = cha.getTemplate().baseHpReg;
		double hpRegenMultiplier = cha.isRaid() ? Config.RAID_HP_REGEN_MULTIPLIER : Config.HP_REGEN_MULTIPLIER;
		double hpRegenBonus = 0;

		if(Config.CHAMPION_ENABLE && cha.isChampion())
		{
			hpRegenMultiplier *= Config.CHAMPION_HP_REGEN;
		}

		if(cha instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) cha;

			init += player.getLevel() > 10 ? (player.getLevel() - 1) / 10.0 : 0.5;

			if(SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant())
			{
				hpRegenMultiplier *= calcFestivalRegenModifier(player);
			}
			else
			{
				double siegeModifier = calcSiegeRegenModifer(player);
				if(siegeModifier > 0)
				{
					hpRegenMultiplier *= siegeModifier;
				}
			}

			if(player.isInsideZone(L2Character.ZONE_CLANHALL) && player.getClan() != null)
			{
				int clanHallIndex = player.getClan().getHasHideout();
				if(clanHallIndex > 0)
				{
					ClanHall clansHall = ClanHallManager.getInstance().getClanHallById(clanHallIndex);
					if(clansHall != null)
					{
						if(clansHall.getFunction(ClanHall.FUNC_RESTORE_HP) != null)
						{
							hpRegenMultiplier *= 1 + clansHall.getFunction(ClanHall.FUNC_RESTORE_HP).getLvl() / 100;
						}
					}
				}
			}

			if(player.isInsideZone(L2Character.ZONE_MOTHERTREE))
			{
				hpRegenBonus += 2;
			}

			if(Config.ALLOW_REGEN_SYSTEM && player.isInsideZone(L2Character.ZONE_PEACE))
			{
				hpRegenMultiplier *= (Config.REGEN_SYSTEM_HP);
			}

			if(player.isSitting())
			{
				hpRegenMultiplier *= 1.5;
			}
			else if(!player.isMoving())
			{
				hpRegenMultiplier *= 1.1;
			}
			else if(player.isRunning())
			{
				hpRegenMultiplier *= 0.7;
			}

			init *= cha.getLevelMod() * CONbonus[cha.getCON()];
		}

		if(init < 1)
		{
			init = 1;
		}

		return cha.calcStat(Stats.REGENERATE_HP_RATE, init, null, null) * hpRegenMultiplier + hpRegenBonus;
	}

	public final double calcMpRegen(L2Character cha)
	{
		double init = cha.getTemplate().baseMpReg;
		double mpRegenMultiplier = cha.isRaid() ? Config.RAID_MP_REGEN_MULTIPLIER : Config.MP_REGEN_MULTIPLIER;
		double mpRegenBonus = 0;

		if(cha instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) cha;

			init += 0.3 * (player.getLevel() - 1) / 10.0;

			if(SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant())
			{
				mpRegenMultiplier *= calcFestivalRegenModifier(player);
			}

			if(player.isInsideZone(L2Character.ZONE_MOTHERTREE))
			{
				mpRegenBonus += 1;
			}

			if(Config.ALLOW_REGEN_SYSTEM && player.isInsideZone(L2Character.ZONE_PEACE))
			{
				mpRegenMultiplier *= (Config.REGEN_SYSTEM_MP);
			}

			if(player.isInsideZone(L2Character.ZONE_CLANHALL) && player.getClan() != null)
			{
				int clanHallIndex = player.getClan().getHasHideout();
				if(clanHallIndex > 0)
				{
					ClanHall clansHall = ClanHallManager.getInstance().getClanHallById(clanHallIndex);
					if(clansHall != null)
					{
						if(clansHall.getFunction(ClanHall.FUNC_RESTORE_MP) != null)
						{
							mpRegenMultiplier *= 1 + clansHall.getFunction(ClanHall.FUNC_RESTORE_MP).getLvl() / 100;
						}
					}
				}
			}

			if(player.isSitting())
			{
				mpRegenMultiplier *= 1.5;
			}
			else if(!player.isMoving())
			{
				mpRegenMultiplier *= 1.1;
			}
			else if(player.isRunning())
			{
				mpRegenMultiplier *= 0.7;
			}

			init *= cha.getLevelMod() * MENbonus[cha.getMEN()];
		}

		if(init < 1)
		{
			init = 1;
		}

		return cha.calcStat(Stats.REGENERATE_MP_RATE, init, null, null) * mpRegenMultiplier + mpRegenBonus;
	}

	public final double calcCpRegen(L2Character cha)
	{
		double init = cha.getTemplate().baseHpReg;
		double cpRegenMultiplier = Config.CP_REGEN_MULTIPLIER;
		double cpRegenBonus = 0;

		if(cha instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) cha;

			init += player.getLevel() > 10 ? (player.getLevel() - 1) / 10.0 : 0.5;

			if(Config.ALLOW_REGEN_SYSTEM && player.isInsideZone(L2Character.ZONE_PEACE))
			{
				cpRegenMultiplier *= (Config.REGEN_SYSTEM_CP);
			}

			if(player.isSitting())
			{
				cpRegenMultiplier *= 1.5;
			}
			else if(!player.isMoving())
			{
				cpRegenMultiplier *= 1.1;
			}
			else if(player.isRunning())
			{
				cpRegenMultiplier *= 0.7;
			}
		}
		else
		{
			if(!cha.isMoving())
			{
				cpRegenMultiplier *= 1.1;
			}
			else if(cha.isRunning())
			{
				cpRegenMultiplier *= 0.7;
			}
		}

		init *= cha.getLevelMod() * CONbonus[cha.getCON()];
		if(init < 1)
		{
			init = 1;
		}

		return cha.calcStat(Stats.REGENERATE_CP_RATE, init, null, null) * cpRegenMultiplier + cpRegenBonus;
	}

	@SuppressWarnings("deprecation")
	public final double calcFestivalRegenModifier(L2PcInstance activeChar)
	{
		final int[] festivalInfo = SevenSignsFestival.getInstance().getFestivalForPlayer(activeChar);
		final int oracle = festivalInfo[0];
		final int festivalId = festivalInfo[1];
		int[] festivalCenter;

		if(festivalId < 0)
		{
			return 0;
		}

		if(oracle == SevenSigns.CABAL_DAWN)
		{
			festivalCenter = SevenSignsFestival.FESTIVAL_DAWN_PLAYER_SPAWNS[festivalId];
		}
		else
		{
			festivalCenter = SevenSignsFestival.FESTIVAL_DUSK_PLAYER_SPAWNS[festivalId];
		}

		double distToCenter = activeChar.getDistance(festivalCenter[0], festivalCenter[1]);

		return 1.0 - distToCenter * 0.0005;
	}

	public final double calcSiegeRegenModifer(L2PcInstance activeChar)
	{
		if(activeChar == null || activeChar.getClan() == null)
		{
			return 0;
		}

		Siege siege = SiegeManager.getInstance().getSiege(activeChar.getPosition().getX(), activeChar.getPosition().getY(), activeChar.getPosition().getZ());
		if(siege == null || !siege.getIsInProgress())
		{
			return 0;
		}

		L2SiegeClan siegeClan = siege.getAttackerClan(activeChar.getClan().getClanId());
		if(siegeClan == null || siegeClan.getFlag().size() == 0 || !Util.checkIfInRange(200, activeChar, siegeClan.getFlag().get(0), true))
		{
			return 0;
		}

		return 1.5;
	}

	public static double calcBlowDamage(L2Character attacker, L2Character target, L2Skill skill, boolean shld, boolean crit, boolean ss)
	{
		if((skill.getCondition() & L2Skill.COND_BEHIND) != 0 && !attacker.isBehind(target))
		{
			return 0;
		}
		
		final boolean isPvP = (attacker instanceof L2Playable) && (target instanceof L2Playable);
		double power = skill.getPower();
		double damage = attacker.getPAtk(target);
		double defence = target.getPDef(attacker);
		if(ss)
		{
			damage *= 2.;
		}

		if(shld)
		{
			defence += target.getShldDef();
		}

		if(ss && skill.getSSBoost() > 0)
		{
			power *= skill.getSSBoost();
		}

		damage += 1.5 * attacker.calcStat(Stats.CRITICAL_DAMAGE, damage + power, target, skill);

		if(target instanceof L2Npc)
		{
			damage *= ((L2Npc) target).getTemplate().getVulnerability(Stats.DAGGER_WPN_VULN);
		}

		damage = target.calcStat(Stats.DAGGER_WPN_VULN, damage, target, null);
		damage *= 70. / defence;
		damage += Rnd.get() * attacker.getRandomDamage(target);
		if(target instanceof L2PcInstance)
		{
			L2Armor armor = ((L2PcInstance) target).getActiveChestArmorItem();
			if(armor != null)
			{
				if(((L2PcInstance) target).isWearingHeavyArmor())
				{
					damage /= 2.5;
				}
				if(((L2PcInstance) target).isWearingLightArmor())
				{
					damage /= 2;
				}
				if(((L2PcInstance) target).isWearingMagicArmor())
				{
					damage /= 1.8;
				}
			}
		}
		
		// Dmg bonusses in PvP fight 
		if (isPvP)
			damage *= attacker.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);

		return damage < 1 ? 1. : damage;
	}
	
	public static double calcBlowDamage(L2Character attacker, L2Character target, L2Skill skill, boolean shld, boolean ss)
	{
		if((skill.getCondition() & L2Skill.COND_BEHIND) != 0 && !attacker.isBehind(target))
		{
			return 0;
		}
		
		final boolean isPvP = (attacker instanceof L2Playable) && (target instanceof L2Playable);
		double power = skill.getPower();
		double damage = attacker.getPAtk(target);
		double defence = target.getPDef(attacker);
		if(ss)
		{
			damage *= 2.;
		}

		if(shld)
		{
			defence += target.getShldDef();
		}

		if(ss && skill.getSSBoost() > 0)
		{
			power *= skill.getSSBoost();
		}

		damage += 1.5 * attacker.calcStat(Stats.CRITICAL_DAMAGE, damage + power, target, skill);

		if(target instanceof L2Npc)
		{
			damage *= ((L2Npc) target).getTemplate().getVulnerability(Stats.DAGGER_WPN_VULN);
		}

		damage = target.calcStat(Stats.DAGGER_WPN_VULN, damage, target, null);
		damage *= 70. / defence;
		damage += Rnd.get() * attacker.getRandomDamage(target);
		if(target instanceof L2PcInstance)
		{
			L2Armor armor = ((L2PcInstance) target).getActiveChestArmorItem();
			if(armor != null)
			{
				if(((L2PcInstance) target).isWearingHeavyArmor())
				{
					damage /= 2.5;
				}
				if(((L2PcInstance) target).isWearingLightArmor())
				{
					damage /= 2;
				}
				if(((L2PcInstance) target).isWearingMagicArmor())
				{
					damage /= 1.8;
				}
			}
		}
		
		// Dmg bonusses in PvP fight 
		if (isPvP)
			damage *= attacker.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);

		return damage < 1 ? 1. : damage;
	}

	public final double calcPhysDam(L2Character attacker, L2Character target, L2Skill skill, boolean shld, boolean crit, boolean dual, boolean ss)
	{
		if(attacker instanceof L2PcInstance)
		{
			L2PcInstance pcInst = (L2PcInstance) attacker;
			if(pcInst.isGM() && !pcInst.getAccessLevel().canGiveDamage())
			{
				return 0;
			}
		}

		double damage = attacker.getPAtk(target);
		double defence = target.getPDef(attacker);
		if(ss)
		{
			damage *= 2;
		}

		if(skill != null)
		{
			double skillpower = skill.getPower(attacker);
			float ssboost = skill.getSSBoost();
			if(ssboost <= 0)
			{
				damage += skillpower;
			}
			else if(ssboost > 0)
			{
				if(ss)
				{
					skillpower *= ssboost;
					damage += skillpower;
				}
				else
				{
					damage += skillpower;
				}
			}
		}

		if(attacker instanceof L2Summon && target instanceof L2PcInstance)
		{
			damage *= 0.9;
		}

		if(attacker instanceof L2PcInstance && ((L2PcInstance) attacker).isNoble() && (target instanceof L2PcInstance || target instanceof L2Summon))
		{
			damage *= 1.04;
		}

		L2Weapon weapon = attacker.getActiveWeaponItem();
		Stats stat = null;
		if(weapon != null)
		{
			switch(weapon.getItemType())
			{
				case BOW:
					stat = Stats.BOW_WPN_VULN;
					break;
				case BLUNT:
				case BIGBLUNT:
					stat = Stats.BLUNT_WPN_VULN;
					break;
				case DAGGER:
					stat = Stats.DAGGER_WPN_VULN;
					break;
				case DUAL:
					stat = Stats.DUAL_WPN_VULN;
					break;
				case DUALFIST:
					stat = Stats.DUALFIST_WPN_VULN;
					break;
				case ETC:
					stat = Stats.ETC_WPN_VULN;
					break;
				case FIST:
					stat = Stats.FIST_WPN_VULN;
					break;
				case POLE:
					stat = Stats.POLE_WPN_VULN;
					break;
				case SWORD:
					stat = Stats.SWORD_WPN_VULN;
					break;
				case BIGSWORD:
					stat = Stats.SWORD_WPN_VULN;
					break;
			default:
				break;
			}
		}

		if(crit)
		{
			damage += attacker.getCriticalDmg(target, damage);
		}

		if(shld && !Config.ALT_GAME_SHIELD_BLOCKS)
		{
			defence += target.getShldDef();
		}

		damage = 70 * damage / defence;

		if(stat != null)
		{
			damage = target.calcStat(stat, damage, target, null);
			if(target instanceof L2Npc)
			{
				damage *= ((L2Npc) target).getTemplate().getVulnerability(stat);
			}
		}

		damage += Rnd.nextDouble() * damage / 10;

		if(shld && Config.ALT_GAME_SHIELD_BLOCKS)
		{
			damage -= target.getShldDef();
			if(damage < 0)
			{
				damage = 0;
			}
		}

		if(target instanceof L2PcInstance && weapon != null && weapon.getItemType() == L2WeaponType.DAGGER && skill != null)
		{
			L2Armor armor = ((L2PcInstance) target).getActiveChestArmorItem();
			if(armor != null)
			{
				if(((L2PcInstance) target).isWearingHeavyArmor())
				{
					damage /= 2.5;
				}
				if(((L2PcInstance) target).isWearingLightArmor())
				{
					damage /= 2;
				}
				if(((L2PcInstance) target).isWearingMagicArmor())
				{
					damage /= 1.8;
				}
			}
		}
		
		if(target instanceof L2PcInstance && weapon != null && weapon.getItemType() == L2WeaponType.BOW && skill != null)
		{
			L2Armor armor = ((L2PcInstance) target).getActiveChestArmorItem();
			if(armor != null)
			{
				if(((L2PcInstance) target).isWearingHeavyArmor())
				{
					damage /= 2.5;
				}
				if(((L2PcInstance) target).isWearingLightArmor())
				{
					damage /= 2;
				}
				if(((L2PcInstance) target).isWearingMagicArmor())
				{
					damage /= 1.8;
				}
			}
		}
		
		if(target instanceof L2PcInstance && weapon != null && weapon.getItemType() == L2WeaponType.BLUNT && skill != null)
		{
			L2Armor armor = ((L2PcInstance) target).getActiveChestArmorItem();
			if(armor != null)
			{
				if(((L2PcInstance) target).isWearingHeavyArmor())
				{
					damage /= 2.5;
				}
				if(((L2PcInstance) target).isWearingLightArmor())
				{
					damage /= 2;
				}
				if(((L2PcInstance) target).isWearingMagicArmor())
				{
					damage /= 1.8;
				}
			}
		}
		
		if(target instanceof L2PcInstance && weapon != null && weapon.getItemType() == L2WeaponType.DUALFIST && skill != null)
		{
			L2Armor armor = ((L2PcInstance) target).getActiveChestArmorItem();
			if(armor != null)
			{
				if(((L2PcInstance) target).isWearingHeavyArmor())
				{
					damage /= 2.5;
				}
				if(((L2PcInstance) target).isWearingLightArmor())
				{
					damage /= 2;
				}
				if(((L2PcInstance) target).isWearingMagicArmor())
				{
					damage /= 1.8;
				}
			}
		}
		
		if(target instanceof L2PcInstance && weapon != null && weapon.getItemType() == L2WeaponType.DUAL && skill != null)
		{
			L2Armor armor = ((L2PcInstance) target).getActiveChestArmorItem();
			if(armor != null)
			{
				if(((L2PcInstance) target).isWearingHeavyArmor())
				{
					damage /= 2.5;
				}
				if(((L2PcInstance) target).isWearingLightArmor())
				{
					damage /= 2;
				}
				if(((L2PcInstance) target).isWearingMagicArmor())
				{
					damage /= 1.8;
				}
			}
		}
		
		if(target instanceof L2PcInstance && weapon != null && weapon.getItemType() == L2WeaponType.SWORD && skill != null)
		{
			L2Armor armor = ((L2PcInstance) target).getActiveChestArmorItem();
			if(armor != null)
			{
				if(((L2PcInstance) target).isWearingHeavyArmor())
				{
					damage /= 2.5;
				}
				if(((L2PcInstance) target).isWearingLightArmor())
				{
					damage /= 2;
				}
				if(((L2PcInstance) target).isWearingMagicArmor())
				{
					damage /= 1.8;
				}
			}
		}

		if(target instanceof L2PcInstance && weapon != null && weapon.getItemType() == L2WeaponType.POLE && skill != null)
		{
			L2Armor armor = ((L2PcInstance) target).getActiveChestArmorItem();
			if(armor != null)
			{
				if(((L2PcInstance) target).isWearingHeavyArmor())
				{
					damage /= 2.5;
				}
				if(((L2PcInstance) target).isWearingLightArmor())
				{
					damage /= 2;
				}
				if(((L2PcInstance) target).isWearingMagicArmor())
				{
					damage /= 1.8;
				}
			}
		}

		if(attacker instanceof L2Npc)
		{
			if(((L2Npc) attacker).getTemplate().getRace() == L2NpcTemplate.Race.UNDEAD)
			{
				damage /= attacker.getPDefUndead(target);
			}

			if(((L2Npc) attacker).getTemplate().getRace() == L2NpcTemplate.Race.PLANT)
			{
				damage /= attacker.getPDefPlants(target);
			}

			if(((L2Npc) attacker).getTemplate().getRace() == L2NpcTemplate.Race.BUG)
			{
				damage /= attacker.getPDefInsects(target);
			}

			if(((L2Npc) attacker).getTemplate().getRace() == L2NpcTemplate.Race.ANIMAL)
			{
				damage /= attacker.getPDefAnimals(target);
			}

			if(((L2Npc) attacker).getTemplate().getRace() == L2NpcTemplate.Race.BEAST)
			{
				damage /= attacker.getPDefMonsters(target);
			}

			if(((L2Npc) attacker).getTemplate().getRace() == L2NpcTemplate.Race.DRAGON)
			{
				damage /= attacker.getPDefDragons(target);
			}
		}

		if(target instanceof L2Npc)
		{
			switch(((L2Npc) target).getTemplate().getRace())
			{
				case UNDEAD:
					damage *= attacker.getPAtkUndead(target);
					break;
				case BEAST:
					damage *= attacker.getPAtkMonsters(target);
					break;
				case ANIMAL:
					damage *= attacker.getPAtkAnimals(target);
					break;
				case PLANT:
					damage *= attacker.getPAtkPlants(target);
					break;
				case DRAGON:
					damage *= attacker.getPAtkDragons(target);
					break;
				case BUG:
					damage *= attacker.getPAtkInsects(target);
					break;
				default:
					break;
			}
		}

		if(shld)
		{
			if(100 - Config.ALT_PERFECT_SHLD_BLOCK < Rnd.get(100))
			{
				damage = 1;
				target.sendPacket(new SystemMessage(SystemMessageId.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS));
			}
		}

		if(damage > 0 && damage < 1)
		{
			damage = 1;
		}
		else if(damage < 0)
		{
			damage = 0;
		}

		if((attacker instanceof L2PcInstance || attacker instanceof L2Summon) && (target instanceof L2PcInstance || target instanceof L2Summon))
		{
			if(skill == null)
			{
				damage *= attacker.calcStat(Stats.PVP_PHYSICAL_DMG, 1, null, null);
			}
			else
			{
				damage *= attacker.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);
			}
		}

		return damage;
	}

	public final static double calcMagicDam(L2Character attacker, L2Character target, L2Skill skill, boolean ss, boolean bss, boolean mcrit)
	{
		if (attacker instanceof L2PcInstance)
		{
			L2PcInstance pcInst = (L2PcInstance)attacker;
			if (pcInst.isGM() && !pcInst.getAccessLevel().canGiveDamage())
			{
				return 0;
			}
		}
		
		double mAtk = attacker.getMAtk(target, skill);
		double mDef = target.getMDef(attacker, skill);
		if (bss) mAtk *= 4;
		else if (ss) mAtk *= 2;
		double damage = 91 * Math.sqrt(mAtk) / mDef * skill.getPower(attacker) * calcSkillVulnerability(target, skill);

		/*if(attacker instanceof L2Summon && target instanceof L2PcInstance)
		{
			damage *= 0.9;
		}

		if(attacker instanceof L2PcInstance && ((L2PcInstance) attacker).isNoble() && (target instanceof L2PcInstance || target instanceof L2Summon))
		{
			damage *= 1.04;
		}*/

		if(Config.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(attacker, target, skill))
		{
			if(attacker instanceof L2PcInstance)
			{
				if(calcMagicSuccess(attacker, target, skill) && target.getLevel() - attacker.getLevel() <= 9)
				{
					if(skill.getSkillType() == L2SkillType.DRAIN)
					{
						attacker.sendPacket(new SystemMessage(SystemMessageId.DRAIN_HALF_SUCCESFUL));
					}
					else
					{
						attacker.sendPacket(new SystemMessage(SystemMessageId.ATTACK_FAILED));
					}

					damage /= 2;
				}
				else
				{
					attacker.sendPacket(new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2).addString(target.getName()).addSkillName(skill.getId()));

					damage = 1;
				}
			}

			if(target instanceof L2PcInstance)
			{
				if(skill.getSkillType() == L2SkillType.DRAIN)
				{
					target.sendPacket(new SystemMessage(SystemMessageId.RESISTED_S1_DRAIN).addString(attacker.getName()));
				}
				else
				{
					target.sendPacket(new SystemMessage(SystemMessageId.RESISTED_S1_MAGIC).addString(attacker.getName()));
				}
			}
		}
		else if(mcrit)
		{
			damage *= 3;
		}

		if (attacker instanceof L2Playable && target instanceof L2Playable)
		{
			if (skill.isMagic())
				damage *= attacker.calcStat(Stats.PVP_MAGICAL_DMG, 1, null, null);
			else
				damage *= attacker.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null);
		}

		if(skill != null)
		{
			if((target instanceof L2Playable))
			{
				damage *= skill.getPvpMulti();
			}
		}

		if(skill.getSkillType() == L2SkillType.DEATHLINK)
		{
			damage = damage * (1.0 - attacker.getStatus().getCurrentHp() / attacker.getMaxHp()) * 2.0;
		}
		
		return damage;
	}

	public final static boolean calcCrit(double rate)
	{
		return rate > Rnd.get(1000);
	}

	public final boolean calcBlow(L2Character activeChar, L2Character target, int chance)
	{
		return activeChar.calcStat(Stats.BLOW_RATE, chance * (1.0 + (activeChar.getDEX() - 20) / 100), target, null) > Rnd.get(100);
	}

	public static final double calcLethal(L2Character activeChar, L2Character target, int magiclvl, int baseLethal)
    {
		double chance = 0;
		if (magiclvl > 0)
		{
			int delta = ((magiclvl + activeChar.getLevel()) / 2) - 1 - target.getLevel();
			
			if (delta >= -3)
			{
				chance = (baseLethal * ((double) activeChar.getLevel() / target.getLevel()));
			}
			else if ((delta < -3) && (delta >= -9))
			{
				chance = (-3) * (baseLethal / (delta));
			}
			else
			{
				chance = baseLethal / 15;
			}
		}
		else
		{
			chance = (baseLethal * ((double) activeChar.getLevel() / target.getLevel()));
		}
		
		return 10 * activeChar.calcStat(Stats.LETHAL_RATE, chance, target, null);
    }
	
	public static final boolean calcLethalHit(L2Character activeChar, L2Character target, L2Skill skill)
	{
		if (!target.isRaid() && !(target instanceof L2DoorInstance))
		{
			// If one of following IDs is found, return false (Tyrannosaurus x 3,
			// Headquarters)
			if (target instanceof L2Npc)
			{
				int npcId = ((L2Npc) target).getNpcId();
				switch (npcId)
                {
                case 22215:
                case 22216:
                case 22217:
                case 35062:
                	return false;
                }
			}
			
			// 2nd lethal effect activate (cp,hp to 1 or if target is npc then hp to 1) 
			if ((skill.getLethalChance2() > 0) && (Rnd.get(1000) < calcLethal(activeChar, target, skill.getLethalChance2(), skill.getMagicLevel())))
			{
				if (target instanceof L2Npc)
				{
					target.reduceCurrentHp(target.getCurrentHp() - 1, activeChar);
				}
				else if (target instanceof L2PcInstance) // If is a active player set his
					// HP and CP to 1
				{
					L2PcInstance player = (L2PcInstance) target;
					if (!player.isInvul())
					{
						if (!((activeChar instanceof L2PcInstance) && (((L2PcInstance) activeChar).isGM() && !((L2PcInstance) activeChar).getAccessLevel().canGiveDamage())))
						{
							player.setCurrentHp(1);
							player.setCurrentCp(1);
							player.sendPacket(SystemMessageId.LETHAL_STRIKE);
						}
					}
				}
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.LETHAL_STRIKE_SUCCESSFUL));
			}
			else if ((skill.getLethalChance1() > 0) && (Rnd.get(1000) < calcLethal(activeChar, target, skill.getLethalChance1(), skill.getMagicLevel())))
			{
				if (target instanceof L2PcInstance)
				{
					L2PcInstance player = (L2PcInstance) target;
					if (!player.isInvul())
					{
						if (!((activeChar instanceof L2PcInstance) && (((L2PcInstance) activeChar).isGM() && !((L2PcInstance) activeChar).getAccessLevel().canGiveDamage())))
                        {
							player.setCurrentCp(1); // Set CP to 1
							player.sendPacket(SystemMessageId.LETHAL_STRIKE);
                        }
					}
				}
				else if (target instanceof L2Npc)
				{
					// and after 50% of current hp
					target.reduceCurrentHp(target.getCurrentHp() / 2, activeChar);
				}
				
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.LETHAL_STRIKE_SUCCESSFUL));
			}
			else
			{
				return false;
			}
		}
		else
		{
			return false;
        }
		
		return true;
	}

	public final static boolean calcMCrit(double mRate)
	{
		return mRate > Rnd.get(1000);
	}

	public final static boolean calcAtkBreak(L2Character target, double dmg)
	{
		if(target instanceof L2PcInstance)
		{
			if(((L2PcInstance) target).getForceBuff() != null)
			{
				return true;
			}
		}

		double init = 0;

		if(Config.ALT_GAME_CANCEL_CAST && target.isCastingNow())
		{
			init = 15;
		}

		if(Config.ALT_GAME_CANCEL_BOW && target.isAttackingNow())
		{
			L2Weapon wpn = target.getActiveWeaponItem();
			if(wpn != null && wpn.getItemType() == L2WeaponType.BOW)
			{
				init = 15;
			}
		}

		if(target.isRaid() || target.isInvul() || init <= 0)
		{
			return false;
		}

		init += Math.sqrt(13 * dmg);

		init -= MENbonus[target.getMEN()] * 100 - 100;

		double rate = target.calcStat(Stats.ATTACK_CANCEL, init, null, null);

		if(rate > 99)
		{
			rate = 99;
		}
		else if(rate < 1)
		{
			rate = 1;
		}

		return Rnd.get(100) < rate;
	}

	public final int calcPAtkSpd(L2Character attacker, L2Character target, double rate)
	{
		if(rate < 2)
		{
			return 2700;
		}
		else
		{
			return (int) (470000 / rate);
		}
	}

	public final int calcMAtkSpd(L2Character attacker, L2Character target, L2Skill skill, double skillTime)
	{
		if(skill.isMagic())
		{
			if(!skill.isStaticHitTime())
			{
				return (int) (skillTime * 333 / attacker.getMAtkSpd());
			}
			else
			{
				return (int) (skillTime);
			}
		}

		return (int) (skillTime * 333 / attacker.getPAtkSpd());
	}

	public final int calcMAtkSpd(L2Character attacker, L2Skill skill, double skillTime)
	{
		if(skill.isMagic())
		{
			if(!skill.isStaticHitTime())
			{
				return (int) (skillTime * 333 / attacker.getMAtkSpd());
			}
			else
			{
				return (int) (skillTime);
			}
		}

		return (int) (skillTime * 333 / attacker.getPAtkSpd());
	}

	public boolean calcHitMiss(L2Character attacker, L2Character target)
	{
		int acc_attacker;
		int evas_target;
		acc_attacker = attacker.getAccuracy();
		evas_target = target.getEvasionRate(attacker);
		int d = 85 + acc_attacker - evas_target;
		return d < Rnd.get(100);
	}

	public static boolean calcShldUse(L2Character attacker, L2Character target)
	{
		L2Weapon at_weapon = attacker.getActiveWeaponItem();
		double shldRate = target.calcStat(Stats.SHIELD_RATE, 0, attacker, null) * DEXbonus[target.getDEX()];
		if(shldRate == 0.0)
		{
			return false;
		}

		if(target.getKnownSkill(316) == null && target.getFirstEffect(318) == null)
		{
			if(!target.isFront(attacker))
			{
				return false;
			}
		}

		if(at_weapon != null && at_weapon.getItemType() == L2WeaponType.BOW)
		{
			shldRate *= 1.3;
		}

		return shldRate > Rnd.get(100);
	}

	public boolean calcMagicAffected(L2Character actor, L2Character target, L2Skill skill)
	{
		L2SkillType type = skill.getSkillType();
		double defence = 0;
		if(skill.isActive() && skill.isOffensive())
		{
			defence = target.getMDef(actor, skill);
		}

		double attack = 2 * actor.getMAtk(target, skill) * calcSkillVulnerability(target, skill);
		double d = (attack - defence) / (attack + defence);
		if(target.isRaid() && (type == L2SkillType.CONFUSION || type == L2SkillType.MUTE || type == L2SkillType.PARALYZE || type == L2SkillType.ROOT || type == L2SkillType.FEAR || type == L2SkillType.SLEEP || type == L2SkillType.STUN || type == L2SkillType.DEBUFF || type == L2SkillType.AGGDEBUFF))
		{
			if(d > 0 && Rnd.get(1000) == 1)
			{
				return true;
			}
			else
			{
				return false;
			}
		}

		if (target.calcStat(Stats.DEBUFF_IMMUNITY, 0, null, skill) > 0 && skill.isDebuff())
			return false;
		
		d += 0.5 * Rnd.nextGaussian();
		return d > 0;
	}

	public static double calcSkillVulnerability(L2Character target, L2Skill skill)
	{
		double multiplier = 1;

		if(skill != null)
		{
			Stats stat = skill.getStat();
			if(stat != null)
			{
				switch(stat)
				{
					case AGGRESSION:
						multiplier *= target.getTemplate().baseAggressionVuln;
						break;
					case BLEED:
						multiplier *= target.getTemplate().baseBleedVuln;
						break;
					case POISON:
						multiplier *= target.getTemplate().basePoisonVuln;
						break;
					case STUN:
						multiplier *= target.getTemplate().baseStunVuln;
						break;
					case ROOT:
						multiplier *= target.getTemplate().baseRootVuln;
						break;
					case MOVEMENT:
						multiplier *= target.getTemplate().baseMovementVuln;
						break;
					case CONFUSION:
						multiplier *= target.getTemplate().baseConfusionVuln;
						break;
					case SLEEP:
						multiplier *= target.getTemplate().baseSleepVuln;
						break;
					case FIRE:
						multiplier *= target.getTemplate().baseFireVuln;
						break;
					case WIND:
						multiplier *= target.getTemplate().baseWindVuln;
						break;
					case WATER:
						multiplier *= target.getTemplate().baseWaterVuln;
						break;
					case EARTH:
						multiplier *= target.getTemplate().baseEarthVuln;
						break;
					case HOLY:
						multiplier *= target.getTemplate().baseHolyVuln;
						break;
					case DARK:
						multiplier *= target.getTemplate().baseDarkVuln;
						break;
				default:
					break;
				}
			}

			switch(skill.getElement())
			{
				case L2Skill.ELEMENT_EARTH:
					multiplier = target.calcStat(Stats.EARTH_VULN, multiplier, target, skill);
					break;
				case L2Skill.ELEMENT_FIRE:
					multiplier = target.calcStat(Stats.FIRE_VULN, multiplier, target, skill);
					break;
				case L2Skill.ELEMENT_WATER:
					multiplier = target.calcStat(Stats.WATER_VULN, multiplier, target, skill);
					break;
				case L2Skill.ELEMENT_WIND:
					multiplier = target.calcStat(Stats.WIND_VULN, multiplier, target, skill);
					break;
				case L2Skill.ELEMENT_HOLY:
					multiplier = target.calcStat(Stats.HOLY_VULN, multiplier, target, skill);
					break;
				case L2Skill.ELEMENT_DARK:
					multiplier = target.calcStat(Stats.DARK_VULN, multiplier, target, skill);
					break;
			}

			L2SkillType type = skill.getSkillType();

			if(type != null && (type == L2SkillType.PDAM || type == L2SkillType.MDAM))
			{
				type = skill.getEffectType();
			}

			if(type != null)
			{
				switch(type)
				{
					case BLEED:
						multiplier = target.calcStat(Stats.BLEED_VULN, multiplier, target, null);
						break;
					case POISON:
						multiplier = target.calcStat(Stats.POISON_VULN, multiplier, target, null);
						break;
					case STUN:
						multiplier = target.calcStat(Stats.STUN_VULN, multiplier, target, null);
						break;
					case PARALYZE:
						multiplier = target.calcStat(Stats.PARALYZE_VULN, multiplier, target, null);
						break;
					case ROOT:
						multiplier = target.calcStat(Stats.ROOT_VULN, multiplier, target, null);
						break;
					case SLEEP:
						multiplier = target.calcStat(Stats.SLEEP_VULN, multiplier, target, null);
						break;
					case MUTE:
					case FEAR:
					case BETRAY:
					case AGGREDUCE_CHAR:
						multiplier = target.calcStat(Stats.DERANGEMENT_VULN, multiplier, target, null);
						break;
					case CONFUSION:
						multiplier = target.calcStat(Stats.CONFUSION_VULN, multiplier, target, null);
						break;
					case DEBUFF:
					case WEAKNESS:
						multiplier = target.calcStat(Stats.DEBUFF_VULN, multiplier, target, null);
						break;
					case BUFF:
						multiplier = target.calcStat(Stats.BUFF_VULN, multiplier, target, null);
						break;
					default:
						;
				}
			}

		}

		return multiplier;
	}

	public static double calcSkillStatModifier(L2SkillType type, L2Character target)
	{
		double multiplier = 1;
		if(type == null)
		{
			return multiplier;
		}

		switch(type)
		{
			case STUN:
			case BLEED:
				multiplier = 2 - Math.sqrt(CONbonus[target.getCON()]);
				break;
			case POISON:
			case SLEEP:
			case DEBUFF:
			case WEAKNESS:
			case ERASE:
			case ROOT:
			case MUTE:
			case FEAR:
			case BETRAY:
			case CONFUSION:
			case AGGREDUCE_CHAR:
			case PARALYZE:
				multiplier = 2 - Math.sqrt(MENbonus[target.getMEN()]);
				break;
			default:
				return multiplier;
		}
		if(multiplier < 0)
		{
			multiplier = 0;
		}

		return multiplier;
	}

	public boolean calcSkillSuccess(L2Character attacker, L2Character target, L2Skill skill, boolean ss, boolean sps, boolean bss)
	{
		if(attacker == null)
		{
			return false;
		}
		
		if (target.calcStat(Stats.DEBUFF_IMMUNITY, 0, null, skill) > 0 && skill.isDebuff())
			return false;
		
		L2SkillType type = skill.getSkillType();

		if(target.isRaid() && (type == L2SkillType.CONFUSION || type == L2SkillType.MUTE || type == L2SkillType.PARALYZE || type == L2SkillType.ROOT || type == L2SkillType.FEAR || type == L2SkillType.SLEEP || type == L2SkillType.STUN || type == L2SkillType.DEBUFF || type == L2SkillType.AGGDEBUFF))
		{
			return false;
		}

		if(target.isInvul() && (type == L2SkillType.CONFUSION || type == L2SkillType.MUTE || type == L2SkillType.PARALYZE || type == L2SkillType.ROOT || type == L2SkillType.FEAR || type == L2SkillType.SLEEP || type == L2SkillType.STUN || type == L2SkillType.DEBUFF || type == L2SkillType.CANCEL || type == L2SkillType.NEGATE || type == L2SkillType.WARRIOR_BANE || type == L2SkillType.MAGE_BANE))
		{
			return false;
		}

		int value = (int) skill.getPower();
		int lvlDepend = skill.getLevelDepend();

		if(type == L2SkillType.PDAM || type == L2SkillType.MDAM)
		{
			value = skill.getEffectPower();
			type = skill.getEffectType();
		}

		if(value == 0 || type == null)
		{
			if(skill.getSkillType() == L2SkillType.PDAM)
			{
				value = 50;
				type = L2SkillType.STUN;
			}

			if(skill.getSkillType() == L2SkillType.MDAM)
			{
				value = 30;
				type = L2SkillType.PARALYZE;
			}
		}

		if(value == 0)
		{
			value = type == L2SkillType.PARALYZE ? 50 : type == L2SkillType.FEAR ? 40 : 80;
		}

		if(lvlDepend == 0)
		{
			lvlDepend = type == L2SkillType.PARALYZE || type == L2SkillType.FEAR ? 1 : 2;
		}

		int lvlmodifier = ((skill.getMagicLevel() > 0 ? skill.getMagicLevel() : attacker.getLevel()) - target.getLevel()) * lvlDepend;
		double statmodifier = calcSkillStatModifier(type, target);
		double resmodifier = calcSkillVulnerability(target, skill);

		int ssmodifier = (bss ? 150 : (sps || ss ? 125 : 100));

		int rate = (int) (value * statmodifier + lvlmodifier);
		if(skill.isMagic())
		{
			int mdef = Math.max(1, target.getMDef(target, skill));
			double matk = attacker.getMAtk(target, skill);
			value *= 11 * Math.sqrt((1 + (bss ? 3 : sps ? 1 : 0)) * matk) / mdef;
		}

		value *= calcSkillVulnerability(target, skill);
		
		if(ssmodifier != 100)
		{
			if(rate > 10000 / (100 + ssmodifier))
			{
				rate = 100 - (100 - rate) * 100 / ssmodifier;
			}
			else
			{
				rate = rate * ssmodifier / 100;
			}
		}

		if(rate > 99)
		{
			rate = 99;
		}
		else if(rate < 1)
		{
			rate = 1;
		}

		rate *= resmodifier;

		return Rnd.get(100) < rate;
	}

	public boolean calcBuffSuccess(L2Character target, L2Skill skill)
	{
		int rate = 100 * (int)calcSkillVulnerability(target, skill);
		return Rnd.get(100) < rate;
	}

	public static boolean calcMagicSuccess(L2Character attacker, L2Character target, L2Skill skill)
	{
		double lvlDifference = target.getLevel() - (skill.getMagicLevel() > 0 ? skill.getMagicLevel() : attacker.getLevel());
		int rate = Math.round((float) (Math.pow(1.3, lvlDifference) * 100));

		return Rnd.get(10000) > rate;
	}

	public boolean calculateUnlockChance(L2Skill skill)
	{
		int level = skill.getLevel();
		int chance = 0;
		switch(level)
		{
			case 1:
				chance = 30;
				break;

			case 2:
				chance = 50;
				break;

			case 3:
				chance = 75;
				break;

			case 4:
			case 5:
			case 6:
			case 7:
			case 8:
			case 9:
			case 10:
			case 11:
			case 12:
			case 13:
			case 14:
				chance = 100;
				break;
		}
		if(Rnd.get(120) > chance)
		{
			return false;
		}
		return true;
	}

	public double calcManaDam(L2Character attacker, L2Character target, L2Skill skill, boolean ss, boolean bss)
	{
		double mAtk = attacker.getMAtk(target, skill);
		double mDef = target.getMDef(attacker, skill);
		double mp = target.getMaxMp();
		if(bss)
		{
			mAtk *= 4;
		}
		else if(ss)
		{
			mAtk *= 2;
		}

		double damage = Math.sqrt(mAtk) * skill.getPower(attacker) * mp / 97 / mDef;
		damage *= calcSkillVulnerability(target, skill);
		return damage;
	}

	public double calculateSkillResurrectRestorePercent(double baseRestorePercent, int casterWIT)
	{
		double restorePercent = baseRestorePercent;
		double modifier = WITbonus[casterWIT];

		if(restorePercent != 100 && restorePercent != 0)
		{

			restorePercent = baseRestorePercent * modifier;

			if(restorePercent - baseRestorePercent > 20.0)
			{
				restorePercent = baseRestorePercent + 20.0;
			}
		}

		if(restorePercent > 100)
		{
			restorePercent = 100;
		}

		if(restorePercent < baseRestorePercent)
		{
			restorePercent = baseRestorePercent;
		}

		return restorePercent;
	}

	public double getSTRBonus(L2Character activeChar)
	{
		return STRbonus[activeChar.getSTR()];
	}

	public static boolean calcPhysicalSkillEvasion(L2Character target, L2Skill skill)
	{
		if(skill.isMagic() || skill.getCastRange() > 40)
		{
			return false;
		}

		return Rnd.get(100) < target.calcStat(Stats.P_SKILL_EVASION, 0, null, skill);
	}

	public boolean calcSkillMastery(L2Character actor)
	{
		if(actor == null)
		{
			return false;
		}

		double val = actor.getStat().calcStat(Stats.SKILL_MASTERY, 0, null, null);

		if(actor instanceof L2PcInstance)
		{
			if(((L2PcInstance) actor).isMageClass())
			{
				val *= INTbonus[actor.getINT()];
			}
			else
			{
				val *= STRbonus[actor.getSTR()];
			}
		}

		return Rnd.get(100) < val;
	}

	public static byte calcSkillReflect(L2Character target, L2Skill skill)
	{
		/*
		 * Neither some special skills (like hero debuffs...) or those skills ignoring resistances can be reflected
		 */
		if (skill.ignoreResists() || !skill.canBeReflected())
			return SKILL_REFLECT_FAILED;
		
		// only magic and melee skills can be reflected
		if (!skill.isMagic() && (skill.getCastRange() == -1 || skill.getCastRange() > MELEE_ATTACK_RANGE))
			return SKILL_REFLECT_FAILED;
		
		byte reflect = SKILL_REFLECT_FAILED;
		// check for non-reflected skilltypes, need additional retail check
		switch (skill.getSkillType())
		{
			case BUFF:
			case REFLECT:
			case HEAL_PERCENT:
			case MANAHEAL_PERCENT:
			case HOT:
			case CPHOT:
			case MPHOT:
			case UNDEAD_DEFENSE:
			case AGGDEBUFF:
			case CONT:
				return SKILL_REFLECT_FAILED;
				// these skill types can deal damage
			case PDAM:
			case BLOW:
			case MDAM:
			case DEATHLINK:
			case CHARGEDAM:
				final Stats stat = skill.isMagic() ? Stats.VENGEANCE_SKILL_MAGIC_DAMAGE : Stats.VENGEANCE_SKILL_PHYSICAL_DAMAGE;
				final double venganceChance = target.getStat().calcStat(stat, 0, target, skill);
				if (venganceChance > Rnd.get(100))
					reflect |= SKILL_REFLECT_VENGEANCE;
				break;
		default:
			break;
		}
		
		final double reflectChance = target.calcStat(skill.isMagic() ? Stats.REFLECT_SKILL_MAGIC : Stats.REFLECT_SKILL_PHYSIC, 0, null, skill);
		if (Rnd.get(100) < reflectChance)
			reflect |= SKILL_REFLECT_SUCCEED;
		
		return reflect;
	}
	
	public static double calcFallDam(L2Character cha, int fallHeight)
	{
		if(!Config.ENABLE_FALLING_DAMAGE || fallHeight < 0)
		{
			return 0;
		}

		final double damage = cha.calcStat(Stats.FALL, fallHeight * cha.getMaxHp() / 1000, null, null);
		return damage;
	}
	
}