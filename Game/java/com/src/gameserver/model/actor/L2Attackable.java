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
package com.src.gameserver.model.actor;

import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.ItemsAutoDestroy;
import com.src.gameserver.ai.CtrlEvent;
import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.ai.L2AttackableAI;
import com.src.gameserver.ai.L2CharacterAI;
import com.src.gameserver.ai.L2SiegeGuardAI;
import com.src.gameserver.datatables.sql.ItemTable;
import com.src.gameserver.managers.CursedWeaponsManager;
import com.src.gameserver.model.L2CommandChannel;
import com.src.gameserver.model.L2DropCategory;
import com.src.gameserver.model.L2DropData;
import com.src.gameserver.model.L2Manor;
import com.src.gameserver.model.L2Party;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.instance.L2DoorInstance;
import com.src.gameserver.model.actor.instance.L2GrandBossInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2MinionInstance;
import com.src.gameserver.model.actor.instance.L2MonsterInstance;
import com.src.gameserver.model.actor.instance.L2NpcInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.model.actor.instance.L2RaidBossInstance;
import com.src.gameserver.model.actor.instance.L2SiegeGuardInstance;
import com.src.gameserver.model.actor.instance.L2SummonInstance;
import com.src.gameserver.model.actor.knownlist.AttackableKnownList;
import com.src.gameserver.model.base.SoulCrystal;
import com.src.gameserver.model.quest.Quest;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.clientpackets.Say2;
import com.src.gameserver.network.serverpackets.CreatureSay;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.script.EventDroplist;
import com.src.gameserver.script.EventDroplist.DateDrop;
import com.src.gameserver.skills.Stats;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.templates.item.L2EtcItemType;
import com.src.gameserver.templates.item.L2Item;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.gameserver.util.Util;
import com.src.util.random.Rnd;

public class L2Attackable extends L2Npc
{
	private static final Log _log = LogFactory.getLog(L2Attackable.class);

	public final class AggroInfo
	{
		protected L2Character _attacker;
		protected int _hate;
		protected int _damage;

		AggroInfo(L2Character pAttacker)
		{
			_attacker = pAttacker;
		}

		@Override
		public boolean equals(Object obj)
		{
			if(this == obj)
			{
				return true;
			}

			if(obj instanceof AggroInfo)
			{
				return ((AggroInfo) obj)._attacker == _attacker;
			}

			return false;
		}

		@Override
		public int hashCode()
		{
			return _attacker.getObjectId();
		}

	}

	protected final class RewardInfo
	{
		protected L2Character _attacker;
		protected int _dmg = 0;

		public RewardInfo(L2Character pAttacker, int pDmg)
		{
			_attacker = pAttacker;
			_dmg = pDmg;
		}

		public void addDamage(int pDmg)
		{
			_dmg += pDmg;
		}

		@Override
		public boolean equals(Object obj)
		{
			if(this == obj)
			{
				return true;
			}

			if(obj instanceof RewardInfo)
			{
				return ((RewardInfo) obj)._attacker == _attacker;
			}

			return false;
		}

		@Override
		public int hashCode()
		{
			return _attacker.getObjectId();
		}
	}

	public final class AbsorberInfo
	{
		protected L2PcInstance _absorber;
		protected int _crystalId;
		protected double _absorbedHP;

		AbsorberInfo(L2PcInstance attacker, int pCrystalId, double pAbsorbedHP)
		{
			_absorber = attacker;
			_crystalId = pCrystalId;
			_absorbedHP = pAbsorbedHP;
		}

		@Override
		public boolean equals(Object obj)
		{
			if(this == obj)
			{
				return true;
			}

			if(obj instanceof AbsorberInfo)
			{
				return ((AbsorberInfo) obj)._absorber == _absorber;
			}

			return false;
		}

		@Override
		public int hashCode()
		{
			return _absorber.getObjectId();
		}
	}

	public final class RewardItem
	{
		protected int _itemId;
		protected int _count;

		public RewardItem(int itemId, int count)
		{
			_itemId = itemId;
			_count = count;
		}

		public int getItemId()
		{
			return _itemId;
		}

		public int getCount()
		{
			return _count;
		}

		public void setCount(int count)
		{
			_count = count;
		}

	}

	private FastMap<L2Character, AggroInfo> _aggroList = new FastMap<L2Character, AggroInfo>().shared();

	public final FastMap<L2Character, AggroInfo> getAggroListRP()
	{
		return _aggroList;
	}

	public final FastMap<L2Character, AggroInfo> getAggroList()
	{
		return _aggroList;
	}

	private boolean _isReturningToSpawnPoint = false;

	public final boolean isReturningToSpawnPoint()
	{
		return _isReturningToSpawnPoint;
	}

	public final void setisReturningToSpawnPoint(boolean value)
	{
		_isReturningToSpawnPoint = value;
	}

	private boolean _canReturnToSpawnPoint = true;

	public final boolean canReturnToSpawnPoint()
	{
		return _canReturnToSpawnPoint;
	}

	public final void setCanReturnToSpawnPoint(boolean value)
	{
		_canReturnToSpawnPoint = value;
	}

	private RewardItem[] _sweepItems;

	private RewardItem[] _harvestItems;
	private boolean _seeded;
	private int _seedType = 0;
	private L2PcInstance _seeder = null;

	private boolean _overhit;

	private double _overhitDamage;

	private L2Character _overhitAttacker;

	private L2CommandChannel _firstCommandChannelAttacked = null;
	private CommandChannelTimer _commandChannelTimer = null;

	private boolean _absorbed;

	private FastMap<L2PcInstance, AbsorberInfo> _absorbersList = new FastMap<L2PcInstance, AbsorberInfo>().shared();

	private boolean _mustGiveExpSp;

	public L2Character _mostHated;
	
	public L2Attackable(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		getKnownList();
		_mustGiveExpSp = true;
	}

	@Override
	public AttackableKnownList getKnownList()
	{
		if(super.getKnownList() == null || !(super.getKnownList() instanceof AttackableKnownList))
		{
			setKnownList(new AttackableKnownList(this));
		}

		return (AttackableKnownList) super.getKnownList();
	}

	@Override
	public L2CharacterAI getAI()
	{
		if(_ai == null)
		{
			synchronized (this)
			{
				if(_ai == null)
				{
					_ai = new L2AttackableAI(new AIAccessor());
				}
			}
		}

		return _ai;
	}

	@Deprecated
	public boolean getCondition2(L2Character target)
	{
		if(target instanceof L2NpcInstance || target instanceof L2DoorInstance)
		{
			return false;
		}

		if(target.isAlikeDead() || !isInsideRadius(target, getAggroRange(), false, false) || Math.abs(getZ() - target.getZ()) > 100)
		{
			return false;
		}

		return !target.isInvul();
	}

	@Override
	public void reduceCurrentHp(double damage, L2Character attacker)
	{
		reduceCurrentHp(damage, attacker, true);
	}

	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake)
	{
		if(_commandChannelTimer == null && isRaid() && attacker != null)
		{
			if (attacker != null && attacker.getParty()!= null && attacker.getParty().isInCommandChannel()&& attacker.getParty().getCommandChannel().meetRaidWarCondition(this))
			{
				_firstCommandChannelAttacked = attacker.getParty().getCommandChannel();
				_commandChannelTimer = new CommandChannelTimer(this, attacker.getParty().getCommandChannel());
				ThreadPoolManager.getInstance().scheduleGeneral(_commandChannelTimer, 300000);
				_firstCommandChannelAttacked.broadcastToChannelMembers(new CreatureSay(0, Say2.PARTYROOM_ALL, "", "You have looting rights!"));
			}
		}

		if(isEventMob)
		{
			return;
		}

		if(attacker != null)
		{
			addDamage(attacker, (int) damage);
		}

		if(this instanceof L2MonsterInstance)
		{
			L2MonsterInstance master = (L2MonsterInstance) this;

			if(this instanceof L2MinionInstance)
			{
				master = ((L2MinionInstance) this).getLeader();

				if(!master.isInCombat() && !master.isDead())
				{
					master.addDamage(attacker, 1);
				}
			}

			if(master.hasMinions())
			{
				master.callMinionsToAssist(attacker);
			}

			master = null;
		}

		super.reduceCurrentHp(damage, attacker, awake);
	}

	public synchronized void setMustRewardExpSp(boolean value)
	{
		_mustGiveExpSp = value;
	}

	public synchronized boolean getMustRewardExpSP()
	{
		return _mustGiveExpSp;
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if(!super.doDie(killer))
		{
			return false;
		}

		try
		{
			levelSoulCrystals(killer);
		}
		catch(Exception e)
		{
			_log.error("", e);
		}

		try
		{
			if(killer instanceof L2PcInstance || killer instanceof L2Summon)
			{
				L2PcInstance player = killer instanceof L2PcInstance ? (L2PcInstance) killer : ((L2Summon) killer).getOwner();

				Quest[] allOnKillQuests = getTemplate().getEventQuests(Quest.QuestEventType.ON_KILL);

				if(allOnKillQuests != null)
				{
					for(Quest quest : allOnKillQuests)
					{
						quest.notifyKill(this, player, killer instanceof L2Summon);
					}
				}

				player = null;
				allOnKillQuests = null;
			}
		}
		catch(Exception e)
		{
			_log.error("", e);
		}

		setChampion(false);

		if(Config.CHAMPION_ENABLE)
		{
			if(!(this instanceof L2GrandBossInstance) && !(this instanceof L2RaidBossInstance) && this instanceof L2MonsterInstance && Config.CHAMPION_FREQUENCY > 0 && getLevel() >= Config.CHAMP_MIN_LVL && getLevel() <= Config.CHAMP_MAX_LVL)
			{
				int random = Rnd.get(100);
				if(random < Config.CHAMPION_FREQUENCY)
				{
					setChampion(true);
				}
			}
		}

		return true;
	}

	class OnKillNotifyTask implements Runnable
	{
		private L2Attackable _attackable;
		private Quest _quest;
		private L2PcInstance _killer;
		private boolean _isPet;

		public OnKillNotifyTask(L2Attackable attackable, Quest quest, L2PcInstance killer, boolean isPet)
		{
			_attackable = attackable;
			_quest = quest;
			_killer = killer;
			_isPet = isPet;
		}

		@Override
		public void run()
		{
			_quest.notifyKill(_attackable, _killer, _isPet);
		}
	}

	@Override
	protected void calculateRewards(L2Character lastAttacker)
	{
		FastMap<L2Character, RewardInfo> rewards = new FastMap<L2Character, RewardInfo>().shared();

		try
		{
			if(getAggroListRP().isEmpty())
				return;

			L2PcInstance maxDealer = null; 
			int maxDamage = 0;

			int damage;

			L2Character attacker, ddealer;
			
			// While Interacting over This Map Removing Object is Not Allowed
			synchronized (getAggroList())
			{
				// Go through the _aggroList of the L2Attackable
				for(AggroInfo info : getAggroListRP().values())
				{
					if(info == null)
					{
						continue;
					}

					// Get the L2Character corresponding to this attacker
					attacker = info._attacker;

					// Get damages done by this attacker
					damage = info._damage;

					// Prevent unwanted behavior
					if(damage > 1)
					{
						if(attacker instanceof L2SummonInstance || attacker instanceof L2PetInstance && ((L2PetInstance) attacker).getPetData().getOwnerExpTaken() > 0)
						{
							ddealer = ((L2Summon) attacker).getOwner();
						}
						else
						{
							ddealer = info._attacker;
						}

						// Check if ddealer isn't too far from this (killed monster)
						if(!Util.checkIfInRange(Config.ALT_PARTY_RANGE, this, ddealer, true))
						{
							continue;
						}

						// Calculate real damages (Summoners should get own damage plus summon's damage)
						RewardInfo reward = rewards.get(ddealer);
						if(reward == null)
						{
							reward = new RewardInfo(ddealer, damage);
						}
						else
						{
							reward.addDamage(damage);
						}

						rewards.put(ddealer, reward);
						
						if (ddealer instanceof L2Playable && ((L2Playable)ddealer).getActingPlayer() != null && reward._dmg > maxDamage) 
						{ 
							maxDealer = ((L2Playable)ddealer).getActingPlayer(); 
							maxDamage = reward._dmg; 
						} 
						
					}
				}
			}

			// Manage Base, Quests and Sweep drops of the L2Attackable
			doItemDrop(maxDealer != null && maxDealer.isOnline() == 1 ? maxDealer : lastAttacker);
			
			// Manage drop of Special Events created by GM for a defined period
			doEventDrop(maxDealer != null && maxDealer.isOnline() == 1 ? maxDealer : lastAttacker);

			if(!getMustRewardExpSP())
				return;

			if(!rewards.isEmpty())
			{
				L2Party attackerParty;
				long exp;
				int levelDiff, partyDmg, partyLvl, sp;
				float partyMul, penalty;
				RewardInfo reward2;
				int[] tmp;

				for(RewardInfo reward : rewards.values())
				{
					if(reward == null)
					{
						continue;
					}

					penalty = 0;
					attacker = reward._attacker;
					damage = reward._dmg;

					if(attacker instanceof L2PetInstance)
					{
						attackerParty = ((L2PetInstance) attacker).getParty();
					}
					else if(attacker instanceof L2PcInstance)
					{
						attackerParty = ((L2PcInstance) attacker).getParty();
					}
					else
						return;

					if(attacker instanceof L2PcInstance && ((L2PcInstance) attacker).getPet() instanceof L2SummonInstance)
					{
						penalty = ((L2SummonInstance) ((L2PcInstance) attacker).getPet()).getExpPenalty();
					}

					if(damage > getMaxHp())
					{
						damage = getMaxHp();
					}

					if(attackerParty == null)
					{
						if(attacker.getKnownList().knowsObject(this))
						{
							levelDiff = attacker.getLevel() - getLevel();

							tmp = calculateExpAndSp(levelDiff, damage);
							exp = tmp[0];
							exp *= 1 - penalty;
							sp = tmp[1];

							if(Config.CHAMPION_ENABLE && isChampion())
							{
								exp *= Config.CHAMPION_REWARDS;
								sp *= Config.CHAMPION_REWARDS;
							}

							if(attacker instanceof L2PcInstance)
							{
								L2PcInstance player = (L2PcInstance) attacker;
								if(isOverhit() && attacker == getOverhitAttacker())
								{
									player.sendPacket(new SystemMessage(SystemMessageId.OVER_HIT));
									exp += calculateOverhitExp(exp);
								}
        						if (player.isVip() && Config.ALLOW_VIP_XPSP)
        						{
        							exp = (long) (exp * Config.VIP_XP);
        							sp = (int) (sp * Config.VIP_SP);
        						}
        						
        						player = null;
							}

							if(!attacker.isDead())
							{
								attacker.addExpAndSp(Math.round(attacker.calcStat(Stats.EXPSP_RATE, exp, null, null)), (int) attacker.calcStat(Stats.EXPSP_RATE, sp, null, null));
							}
						}
					}
					else
					{
						partyDmg = 0;
						partyMul = 1.f;
						partyLvl = 0;

						List<L2Playable> rewardedMembers = new FastList<L2Playable>();

						List<L2PcInstance> groupMembers;

						if(attackerParty.isInCommandChannel())
						{
							groupMembers = attackerParty.getCommandChannel().getMembers();
						}
						else
						{
							groupMembers = attackerParty.getPartyMembers();
						}

						for(L2PcInstance pl : groupMembers)
						{
							if(pl == null || pl.isDead())
							{
								continue;
							}

							reward2 = rewards.get(pl);

							if(reward2 != null)
							{
								if(Util.checkIfInRange(Config.ALT_PARTY_RANGE, this, pl, true))
								{
									partyDmg += reward2._dmg;
									rewardedMembers.add(pl);

									if(pl.getLevel() > partyLvl)
									{
										if(attackerParty.isInCommandChannel())
										{
											partyLvl = attackerParty.getCommandChannel().getLevel();
										}
										else
										{
											partyLvl = pl.getLevel();
										}
									}
								}

								rewards.remove(pl);
							}
							else
							{
								if(Util.checkIfInRange(Config.ALT_PARTY_RANGE, this, pl, true))
								{
									rewardedMembers.add(pl);

									if(pl.getLevel() > partyLvl)
									{
										if(attackerParty.isInCommandChannel())
										{
											partyLvl = attackerParty.getCommandChannel().getLevel();
										}
										else
										{
											partyLvl = pl.getLevel();
										}
									}
								}
							}

							L2Playable summon = pl.getPet();

							if(summon != null && summon instanceof L2PetInstance)
							{
								reward2 = rewards.get(summon);

								if(reward2 != null)
								{
									if(Util.checkIfInRange(Config.ALT_PARTY_RANGE, this, summon, true))
									{
										partyDmg += reward2._dmg;
										rewardedMembers.add(summon);

										if(summon.getLevel() > partyLvl)
										{
											partyLvl = summon.getLevel();
										}
									}

									rewards.remove(summon);
								}
							}

							summon = null;
						}

						if(partyDmg < getMaxHp())
						{
							partyMul = (float) partyDmg / (float) getMaxHp();
						}

						if(partyDmg > getMaxHp())
						{
							partyDmg = getMaxHp();
						}

						levelDiff = partyLvl - getLevel();

						tmp = calculateExpAndSp(levelDiff, partyDmg);
						exp = tmp[0];
						sp = tmp[1];

						if(Config.CHAMPION_ENABLE && isChampion())
						{
							exp *= Config.CHAMPION_REWARDS;
							sp *= Config.CHAMPION_REWARDS;
						}

						exp *= partyMul;
						sp *= partyMul;

						if(attacker instanceof L2PcInstance)
						{
							L2PcInstance player = (L2PcInstance) attacker;

							if(isOverhit() && attacker == getOverhitAttacker())
							{
								player.sendPacket(new SystemMessage(SystemMessageId.OVER_HIT));
								exp += calculateOverhitExp(exp);
							}

							player = null;
						}

						if(partyDmg > 0)
						{
							attackerParty.distributeXpAndSp(exp, sp, rewardedMembers, partyLvl);
						}

						groupMembers = null;
						rewardedMembers = null;
					}
				}

				attackerParty = null;
				reward2 = null;
			}

			rewards = null;
			attacker = null;
			ddealer = null;

		}
		catch(Exception e)
		{
			_log.error("", e);
		}
	}

	public void addDamage(L2Character attacker, int damage)
	{
		addDamageHate(attacker, damage, damage);
	}

	public void addDamageHate(L2Character attacker, int damage, int aggro)
	{
		if(attacker == null)
		{
			return;
		}

		AggroInfo ai = getAggroListRP().get(attacker);

		if(ai == null)
		{
			ai = new AggroInfo(attacker);
			ai._damage = 0;
			ai._hate = 0;
			getAggroListRP().put(attacker, ai);
		}

		if(aggro < 0)
		{
			ai._hate -= aggro * 150 / (getLevel() + 7);
			aggro = -aggro;
		}
		else if(damage == 0)
		{
			ai._hate += aggro;
		}
		else
		{
			ai._hate += aggro * 100 / (getLevel() + 7);
		}

		ai._damage += damage;

		if(aggro > 0 && getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		}

		ai = null;

		if(damage > 0)
		{
			getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, attacker);

			try
			{
				if(attacker instanceof L2PcInstance || attacker instanceof L2Summon)
				{
					L2PcInstance player = attacker instanceof L2PcInstance ? (L2PcInstance) attacker : ((L2Summon) attacker).getOwner();

					if(getTemplate().getEventQuests(Quest.QuestEventType.ON_ATTACK) != null)
					{
						for(Quest quest : getTemplate().getEventQuests(Quest.QuestEventType.ON_ATTACK))
						{
							quest.notifyAttack(this, player, damage, attacker instanceof L2Summon);
						}
					}

					player = null;
				}
			}
			catch(Exception e)
			{
				_log.error("", e);
			}
		}
	}

	public void reduceHate(L2Character target, int amount)
	{
		if(getAI() instanceof L2SiegeGuardAI)
		{
			stopHating(target);
			setTarget(null);
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null, null);
			return;
		}

		if(target == null)
		{
			L2Character mostHated = getMostHated();

			if(mostHated == null)
			{
				((L2AttackableAI) getAI()).setGlobalAggro(-25);
				return;
			}
			else
			{
				for(L2Character aggroed : getAggroListRP().keySet())
				{
					AggroInfo ai = getAggroListRP().get(aggroed);
					if(ai == null)
					{
						return;
					}

					ai._hate -= amount;

					ai = null;
				}
			}

			amount = getHating(mostHated);
			if(amount <= 0)
			{
				((L2AttackableAI) getAI()).setGlobalAggro(-25);
				clearAggroList();
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				setWalking();
			}

			mostHated = null;

			return;
		}

		AggroInfo ai = getAggroListRP().get(target);

		if(ai == null)
		{
			return;
		}

		ai._hate -= amount;

		if(ai._hate <= 0)
		{
			if(getMostHated() == null)
			{
				((L2AttackableAI) getAI()).setGlobalAggro(-25);
				clearAggroList();
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				setWalking();
			}
		}

		ai = null;
	}

	public void stopHating(L2Character target)
	{
		if(target == null)
		{
			return;
		}

		AggroInfo ai = getAggroListRP().get(target);

		if(ai == null)
		{
			return;
		}

		ai._hate = 0;
		ai = null;
	}

	public L2Character getMostHated()
	{
		if(getAggroListRP().isEmpty() || isAlikeDead())
		{
			return null;
		}

		L2Character mostHated = null;

		int maxHate = 0;

		synchronized (getAggroList())
		{
			for(AggroInfo ai : getAggroListRP().values())
			{
				if(ai == null)
				{
					continue;
				}

				if(ai._attacker.isAlikeDead() || !getKnownList().knowsObject(ai._attacker) || !ai._attacker.isVisible())
				{
					ai._hate = 0;
				}

				if(ai._hate > maxHate)
				{
					mostHated = ai._attacker;
					maxHate = ai._hate;
				}
			}
		}

		if(mostHated!=null)
			_mostHated = mostHated;
			
		return mostHated;
	}

	public int getHating(L2Character target)
	{
		if(getAggroListRP().isEmpty())
		{
			return 0;
		}

		if(target == null)
		{
			return 0;
		}

		AggroInfo ai = getAggroListRP().get(target);

		if(ai == null)
		{
			return 0;
		}

		if(ai._attacker instanceof L2PcInstance && (((L2PcInstance) ai._attacker).getAppearance().getInvisible() || ai._attacker.isInvul()))
		{
			getAggroList().remove(target);

			return 0;
		}

		if(!ai._attacker.isVisible())
		{
			getAggroList().remove(target);

			return 0;
		}

		if(ai._attacker.isAlikeDead())
		{
			ai._hate = 0;

			return 0;
		}

		return ai._hate;
	}

	private RewardItem calculateRewardItem(L2PcInstance lastAttacker, L2DropData drop, int levelModifier, boolean isSweep)
	{
		float dropChance = drop.getChance();

		int deepBlueDrop = 1;

		if(Config.DEEPBLUE_DROP_RULES)
		{
			if(levelModifier > 0)
			{
				deepBlueDrop = 3;

				if(drop.getItemId() == 57)
				{
					deepBlueDrop *= isRaid() ? (int) 1 : (int) Config.RATE_DROP_ITEMS;
				}
			}
		}

		if(deepBlueDrop == 0)
		{
			deepBlueDrop = 1;
		}

		if(Config.DEEPBLUE_DROP_RULES)
		{
			dropChance = (drop.getChance() - drop.getChance() * levelModifier / 100) / deepBlueDrop;
		}

		if(drop.getItemId() == 57)
		{
			if(this instanceof L2RaidBossInstance)
			{
				dropChance *= Config.ADENA_RAID;
			}
			else if(this instanceof L2GrandBossInstance)
			{
				dropChance *= Config.ADENA_BOSS;
			}
			else if(this instanceof L2MinionInstance)
			{
				dropChance *= Config.ADENA_MINION;
			}
			else
			{
				dropChance *= Config.RATE_DROP_ADENA;
				if(Config.ADENA_NEWBIE)
				{
					if(lastAttacker.getLevel() <= Config.ADENA_NEWBIE_LVL)
					{
						dropChance *= Config.RATE_DROP_ADENA_NEWBIE;
					}
				}
				if(lastAttacker.isVip())
				{
					dropChance *= Config.VIP_ADENA_RATE;
				}
			}
		}
		else if(drop.getItemId() == 6660 || drop.getItemId() == 6658 || drop.getItemId() == 6661 || drop.getItemId() == 6657 || drop.getItemId() == 6656 || drop.getItemId() == 8191 || drop.getItemId() == 6662 || drop.getItemId() == 6659)
		{
			if(this instanceof L2RaidBossInstance)
			{
				dropChance *= Config.JEWEL_BOSS;
			}
			else if(this instanceof L2GrandBossInstance)
			{
				dropChance *= Config.JEWEL_BOSS;
			}
			else if(this instanceof L2MinionInstance)
			{
				dropChance *= Config.JEWEL_BOSS;
			}
			else
			{
				dropChance *= Config.RATE_DROP_ITEMS;

			}
		}
		else if(isSweep)
		{
			if(this instanceof L2RaidBossInstance)
			{
				dropChance *= Config.SPOIL_RAID;
			}
			else if(this instanceof L2GrandBossInstance)
			{
				dropChance *= Config.SPOIL_BOSS;
			}
			else if(this instanceof L2MinionInstance && this.isRaid())
			{
				dropChance *= Config.SPOIL_MINION;
			}
			else
			{
				dropChance *= Config.RATE_DROP_SPOIL;
				
				if(lastAttacker.isVip())
				{
					dropChance *= Config.VIP_SPOIL_RATE;
				}
			}
		}
		else
		{
			if(this instanceof L2RaidBossInstance)
			{
				dropChance *= Config.ITEMS_RAID;
			}
			else if(this instanceof L2GrandBossInstance)
			{
				dropChance *= Config.ITEMS_BOSS;
			}
			else if(this instanceof L2MinionInstance && this.isRaid())
			{
				dropChance *= Config.ITEMS_MINION;
			}
			else
			{
				dropChance *= Config.RATE_DROP_ITEMS;

				if(lastAttacker.isVip())
				{
					dropChance *= Config.VIP_DROP_RATE;
				}
			}
		}

		if(Config.CHAMPION_ENABLE && isChampion())
		{
			dropChance *= Config.CHAMPION_REWARDS;
		}

		dropChance = Math.round(dropChance);

		if(dropChance < 1)
		{
			dropChance = 1;
		}

		int minCount = drop.getMinDrop();
		int maxCount = drop.getMaxDrop();
		int itemCount = 0;

		if(dropChance > L2DropData.MAX_CHANCE && !Config.PRECISE_DROP_CALCULATION)
		{
			int multiplier = (int) dropChance / L2DropData.MAX_CHANCE;

			if(minCount < maxCount)
			{
				itemCount += Rnd.get(minCount * multiplier, maxCount * multiplier);
			}
			else if(minCount == maxCount)
			{
				itemCount += minCount * multiplier;
			}
			else
			{
				itemCount += multiplier;
			}

			dropChance = dropChance % L2DropData.MAX_CHANCE;
		}

		int random = Rnd.get(L2DropData.MAX_CHANCE);

		while(random < dropChance)
		{
			if(minCount < maxCount)
			{
				itemCount += Rnd.get(minCount, maxCount);
			}
			else if(minCount == maxCount)
			{
				itemCount += minCount;
			}
			else
			{
				itemCount++;
			}

			dropChance -= L2DropData.MAX_CHANCE;
		}
		if(Config.CHAMPION_ENABLE)
		{
			if((drop.getItemId() == 57 || drop.getItemId() >= 6360 && drop.getItemId() <= 6362) && isChampion())
			{
				itemCount *= Config.CHAMPION_ADENAS_REWARDS;
			}
		}

		if(drop.getItemId() >= 6360 && drop.getItemId() <= 6362)
		{
			itemCount *= Config.RATE_DROP_SEAL_STONES;
		}

		if(itemCount > 0)
		{
			return new RewardItem(drop.getItemId(), itemCount);
		}

		return null;
	}

	private RewardItem calculateCategorizedRewardItem(L2PcInstance lastAttacker, L2DropCategory categoryDrops, int levelModifier)
	{

		if(categoryDrops == null)
		{
			return null;
		}

		int basecategoryDropChance = categoryDrops.getCategoryChance();
		int categoryDropChance = basecategoryDropChance;

		int deepBlueDrop = 1;

		if(Config.DEEPBLUE_DROP_RULES)
		{
			if(levelModifier > 0)
			{
				deepBlueDrop = 3;
			}
		}

		if(deepBlueDrop == 0)
		{
			deepBlueDrop = 1;
		}

		if(Config.DEEPBLUE_DROP_RULES)
		{
			categoryDropChance = (categoryDropChance - categoryDropChance * levelModifier / 100) / deepBlueDrop;
		}

		if(this instanceof L2RaidBossInstance)
		{
			categoryDropChance *= Config.ITEMS_RAID;
		}
		else if(this instanceof L2GrandBossInstance)
		{
			categoryDropChance *= Config.ITEMS_BOSS;
		}
		else if(this instanceof L2MinionInstance)
		{
			categoryDropChance *= Config.ITEMS_MINION;
		}
		else
		{
			categoryDropChance *= Config.RATE_DROP_ITEMS;
		}

		if(Config.CHAMPION_ENABLE && isChampion())
		{
			categoryDropChance *= Config.CHAMPION_REWARDS;
		}

		categoryDropChance = Math.round(categoryDropChance);

		if(categoryDropChance < 1)
		{
			categoryDropChance = 1;
		}

		if(Rnd.get(L2DropData.MAX_CHANCE) < categoryDropChance)
		{
			L2DropData drop = categoryDrops.dropOne(isRaid());

			if(drop == null)
			{
				return null;
			}

			int dropChance = drop.getChance();

			if(drop.getItemId() == 57)
			{
				if(this instanceof L2RaidBossInstance)
				{
					dropChance *= Config.ADENA_RAID;
				}
				else if(this instanceof L2GrandBossInstance)
				{
					dropChance *= Config.ADENA_BOSS;
				}
				else if(this instanceof L2MinionInstance)
				{
					dropChance *= Config.ADENA_MINION;
				}
				else
				{
					dropChance *= Config.RATE_DROP_ADENA;
					if(Config.ADENA_NEWBIE)
					{
						if(lastAttacker.getLevel() <= Config.ADENA_NEWBIE_LVL)
						{
							dropChance *= Config.RATE_DROP_ADENA_NEWBIE;
						}
					}
					if(lastAttacker.isVip())
					{
						dropChance *= Config.VIP_ADENA_RATE;
					}
				}
			}
			else
			{
				if(this instanceof L2RaidBossInstance)
				{
					dropChance *= Config.ITEMS_RAID;
				}
				else if(this instanceof L2GrandBossInstance)
				{
					dropChance *= Config.ITEMS_BOSS;
				}
				else if(this instanceof L2MinionInstance)
				{
					dropChance *= Config.ITEMS_MINION;
				}
				else
				{
					dropChance *= Config.RATE_DROP_ITEMS;
					if(lastAttacker.isVip())
					{
						dropChance *= Config.VIP_DROP_RATE;
					}
				}
			}

			if(Config.CHAMPION_ENABLE && isChampion())
			{
				dropChance *= Config.CHAMPION_REWARDS;
			}

			dropChance = Math.round(dropChance);

			if(dropChance < L2DropData.MAX_CHANCE)
			{
				dropChance = L2DropData.MAX_CHANCE;
			}

			int min = drop.getMinDrop();
			int max = drop.getMaxDrop();
			int itemCount = 0;

			if(dropChance > L2DropData.MAX_CHANCE && !Config.PRECISE_DROP_CALCULATION)
			{
				int multiplier = dropChance / L2DropData.MAX_CHANCE;

				if(min < max)
				{
					itemCount += Rnd.get(min * multiplier, max * multiplier);
				}
				else if(min == max)
				{
					itemCount += min * multiplier;
				}
				else
				{
					itemCount += multiplier;
				}

				dropChance = dropChance % L2DropData.MAX_CHANCE;
			}

			int random = Rnd.get(L2DropData.MAX_CHANCE);

			while(random < dropChance)
			{
				if(min < max)
				{
					itemCount += Rnd.get(min, max);
				}
				else if(min == max)
				{
					itemCount += min;
				}
				else
				{
					itemCount++;
				}

				dropChance -= L2DropData.MAX_CHANCE;
			}

			if(Config.CHAMPION_ENABLE)
			{
				if((drop.getItemId() == 57 || drop.getItemId() >= 6360 && drop.getItemId() <= 6362) && isChampion())
				{
					itemCount *= Config.CHAMPION_ADENAS_REWARDS;
				}
			}

			if(drop.getItemId() >= 6360 && drop.getItemId() <= 6362)
			{
				itemCount *= Config.RATE_DROP_SEAL_STONES;
			}

			if(itemCount > 0)
			{
				return new RewardItem(drop.getItemId(), itemCount);
			}

			drop = null;
		}

		return null;
	}

	private int calculateLevelModifierForDrop(L2PcInstance lastAttacker)
	{
		if(Config.DEEPBLUE_DROP_RULES)
		{
			int highestLevel = lastAttacker.getLevel();

			if(getAttackByList() != null && !getAttackByList().isEmpty())
			{
				for(L2Character atkChar : getAttackByList())
				{
					if(atkChar != null && atkChar.getLevel() > highestLevel)
					{
						highestLevel = atkChar.getLevel();
					}
				}
			}

			if(highestLevel - 9 >= getLevel())
			{
				return (highestLevel - (getLevel() + 8)) * 9;
			}
		}

		return 0;
	}

	public void doItemDrop(L2Character mainDamageDealer)
	{
		doItemDrop(getTemplate(), mainDamageDealer);
	}

	public void doItemDrop(L2NpcTemplate npcTemplate, L2Character mainDamageDealer)
	{
		L2PcInstance player = null;

		if(mainDamageDealer instanceof L2PcInstance)
		{
			player = (L2PcInstance) mainDamageDealer;
		}
		else if(mainDamageDealer instanceof L2Summon)
		{
			player = ((L2Summon) mainDamageDealer).getOwner();
		}

		if(player == null)
		{
			return;
		}

		int levelModifier = calculateLevelModifierForDrop(player);

		if(levelModifier == 0 && player.getLevel() > 20)
		{
			CursedWeaponsManager.getInstance().checkDrop(this, player);
		}

		for(L2DropCategory cat : npcTemplate.getDropData())
		{
			RewardItem item = null;
			if(cat.isSweep())
			{
				if(isSpoil())
				{
					FastList<RewardItem> sweepList = new FastList<RewardItem>();

					for(L2DropData drop : cat.getAllDrops())
					{
						item = calculateRewardItem(player, drop, levelModifier, true);

						if(item == null)
						{
							continue;
						}

						sweepList.add(item);

						item = null;
					}

					if(!sweepList.isEmpty())
					{
						_sweepItems = sweepList.toArray(new RewardItem[sweepList.size()]);
					}

					sweepList = null;
				}
			}
			else
			{
				if(isSeeded())
				{
					L2DropData drop = cat.dropSeedAllowedDropsOnly();

					if(drop == null)
					{
						continue;
					}

					item = calculateRewardItem(player, drop, levelModifier, false);
					drop = null;
				}
				else
				{
					item = calculateCategorizedRewardItem(player, cat, levelModifier);
				}

				if(item != null)
				{
					// Check if the autoLoot mode is active
					if ((isRaid() && Config.AUTO_LOOT_BOSS) || (!isRaid() && Config.AUTO_LOOT))
						player.doAutoLoot(this, item); // Give this or these Item(s) to the L2PcInstance that has killed the L2Attackable
					else
						dropItem(player, item); // drop the item on the ground

					if(this instanceof L2RaidBossInstance)
					{
						broadcastPacket(new SystemMessage(SystemMessageId.S1_DIED_DROPPED_S3_S2).addString(getName()).addItemName(item.getItemId()).addNumber(item.getCount()));
					}
				}
			}

			item = null;
		}

		if(Config.CHAMPION_ENABLE && isChampion() && player.getLevel() <= getLevel()+3 && Config.CHAMPION_REWARD_ITEM > 0 && Rnd.get(100) < Config.CHAMPION_REWARD_ITEM)
		{
			int champqty = Rnd.get(Config.CHAMPION_REWARD_ITEM_QTY);
			champqty++; // quantity should actually vary between 1 and whatever admin specified as max, inclusive.
			
			RewardItem item = new RewardItem(Config.CHAMPION_REWARD_ITEM_ID, champqty);
			if (Config.AUTO_LOOT)
				player.addItem("ChampionLoot", item.getItemId(), item.getCount(), this, true);
			else
				dropItem(player, item);
		}

		double rateHp = getStat().calcStat(Stats.MAX_HP, 1, this, null);

		if(rateHp <= Config.HP_RATE_MOBS_HERB && String.valueOf(npcTemplate.type).contentEquals("L2Monster"))
		{
			boolean _hp = false;
			boolean _mp = false;
			boolean _spec = false;

			int random = Rnd.get(1000);

			if(random < Config.RATE_DROP_SPECIAL_HERBS && !_spec)
			{
				RewardItem item = new RewardItem(8612, 1);

				if(Config.AUTO_LOOT && Config.AUTO_LOOT_HERBS)
				{
					L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());

					if(!player.getInventory().validateCapacity(item_templ))
					{
						dropItem(player, item);
					}
					else
					{
						player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
					}
				}
				else
				{
					dropItem(player, item);
				}

				item = null;
				_spec = true;
			}
			else
			{
				for(int i = 0; i < 3; i++)
				{
					random = Rnd.get(100);

					if(random < Config.RATE_DROP_COMMON_HERBS)
					{
						RewardItem item = null;
						if(i == 0)
						{
							item = new RewardItem(8606, 1);
						}
						if(i == 1)
						{
							item = new RewardItem(8608, 1);
						}
						if(i == 2)
						{
							item = new RewardItem(8610, 1);
						}

						if(Config.AUTO_LOOT && Config.AUTO_LOOT_HERBS)
						{
							L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());

							if(!player.getInventory().validateCapacity(item_templ))
							{
								dropItem(player, item);
							}
							else
							{
								player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
							}
						}
						else
						{
							dropItem(player, item);
						}

						item = null;
						break;
					}
				}
			}

			random = Rnd.get(1000);

			if(random < Config.RATE_DROP_SPECIAL_HERBS && !_spec)
			{
				RewardItem item = new RewardItem(8613, 1);

				if(Config.AUTO_LOOT && Config.AUTO_LOOT_HERBS)
				{
					L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());

					if(!player.getInventory().validateCapacity(item_templ))
					{
						dropItem(player, item);
					}
					else
					{
						player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
					}
				}
				else
				{
					dropItem(player, item);
				}

				item = null;
				_spec = true;
			}
			else
			{
				for(int i = 0; i < 2; i++)
				{
					random = Rnd.get(100);

					if(random < Config.RATE_DROP_COMMON_HERBS)
					{
						RewardItem item = null;
						if(i == 0)
						{
							item = new RewardItem(8607, 1);
						}
						if(i == 1)
						{
							item = new RewardItem(8609, 1);
						}

						if(Config.AUTO_LOOT && Config.AUTO_LOOT_HERBS)
						{
							L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());

							if(!player.getInventory().validateCapacity(item_templ))
							{
								dropItem(player, item);
							}
							else
							{
								player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
							}
						}
						else
						{
							dropItem(player, item);
						}

						item = null;
						break;
					}
				}
			}

			random = Rnd.get(1000);

			if(random < Config.RATE_DROP_SPECIAL_HERBS && !_spec)
			{
				RewardItem item = new RewardItem(8614, 1);

				if(Config.AUTO_LOOT && Config.AUTO_LOOT_HERBS)
				{
					L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());

					if(!player.getInventory().validateCapacity(item_templ))
					{
						dropItem(player, item);
					}
					else
					{
						player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
					}
				}
				else
				{
					dropItem(player, item);
				}

				item = null;
				_mp = true;
				_hp = true;
				_spec = true;
			}

			if(!_hp)
			{
				random = Rnd.get(100);
				if(random < Config.RATE_DROP_MP_HP_HERBS)
				{
					RewardItem item = new RewardItem(8600, 1);

					if(Config.AUTO_LOOT && Config.AUTO_LOOT_HERBS)
					{
						L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());

						if(!player.getInventory().validateCapacity(item_templ))
						{
							dropItem(player, item);
						}
						else
						{
							player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
						}
					}
					else
					{
						dropItem(player, item);
					}

					item = null;
					_hp = true;
				}
			}

			if(!_hp)
			{
				random = Rnd.get(100);

				if(random < Config.RATE_DROP_GREATER_HERBS)
				{
					RewardItem item = new RewardItem(8601, 1);

					if(Config.AUTO_LOOT && Config.AUTO_LOOT_HERBS)
					{
						L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());

						if(!player.getInventory().validateCapacity(item_templ))
						{
							dropItem(player, item);
						}
						else
						{
							player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
						}
					}
					else
					{
						dropItem(player, item);
					}

					item = null;
					_hp = true;
				}
			}

			if(!_hp)
			{
				random = Rnd.get(1000);

				if(random < Config.RATE_DROP_SUPERIOR_HERBS)
				{
					RewardItem item = new RewardItem(8602, 1);

					if(Config.AUTO_LOOT && Config.AUTO_LOOT_HERBS)
					{
						L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());

						if(!player.getInventory().validateCapacity(item_templ))
						{
							dropItem(player, item);
						}
						else
						{
							player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
						}
					}
					else
					{
						dropItem(player, item);
					}

					item = null;
				}
			}

			if(!_mp)
			{
				random = Rnd.get(100);

				if(random < Config.RATE_DROP_MP_HP_HERBS)
				{
					RewardItem item = new RewardItem(8603, 1);

					if(Config.AUTO_LOOT && Config.AUTO_LOOT_HERBS)
					{
						L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());

						if(!player.getInventory().validateCapacity(item_templ))
						{
							dropItem(player, item);
						}
						else
						{
							player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
						}
					}
					else
					{
						dropItem(player, item);
					}

					item = null;
					_mp = true;
				}
			}

			if(!_mp)
			{
				random = Rnd.get(100);

				if(random < Config.RATE_DROP_GREATER_HERBS)
				{
					RewardItem item = new RewardItem(8604, 1);

					if(Config.AUTO_LOOT && Config.AUTO_LOOT_HERBS)
					{
						L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());

						if(!player.getInventory().validateCapacity(item_templ))
						{
							dropItem(player, item);
						}
						else
						{
							player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
						}
					}
					else
					{
						dropItem(player, item);
					}

					item = null;
					_mp = true;
				}
			}

			if(!_mp)
			{
				random = Rnd.get(1000);

				if(random < Config.RATE_DROP_SUPERIOR_HERBS)
				{
					RewardItem item = new RewardItem(8605, 1);

					if(Config.AUTO_LOOT && Config.AUTO_LOOT_HERBS)
					{
						L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());

						if(!player.getInventory().validateCapacity(item_templ))
						{
							dropItem(player, item);
						}
						else
						{
							player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
						}
					}
					else
					{
						dropItem(player, item);
					}

					item = null;
				}
			}

			random = Rnd.get(100);

			if(random < Config.RATE_DROP_COMMON_HERBS)
			{
				RewardItem item = new RewardItem(8611, 1);

				if(Config.AUTO_LOOT && Config.AUTO_LOOT_HERBS)
				{
					L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());

					if(!player.getInventory().validateCapacity(item_templ))
					{
						dropItem(player, item);
					}
					else
					{
						player.addItem("Loot", item.getItemId(), item.getCount(), this, true);
					}
				}
				else
				{
					dropItem(player, item);
				}

				item = null;
			}
		}
	}

	public void doEventDrop(L2Character lastAttacker)
	{
		L2PcInstance player = null;

		if(lastAttacker instanceof L2PcInstance)
		{
			player = (L2PcInstance) lastAttacker;
		}
		else if(lastAttacker instanceof L2Summon)
		{
			player = ((L2Summon) lastAttacker).getOwner();
		}

		if(player == null)
		{
			return;
		}

		if(player.getLevel() - getLevel() > 9)
		{
			return;
		}

		for(DateDrop drop : EventDroplist.getInstance().getAllDrops())
		{
			if(Rnd.get(L2DropData.MAX_CHANCE) < drop.chance)
			{
				RewardItem item = new RewardItem(drop.items[Rnd.get(drop.items.length)], Rnd.get(drop.min, drop.max));

				if(Config.AUTO_LOOT)
				{
					L2Item item_templ = ItemTable.getInstance().getTemplate(item.getItemId());

					if(!player.getInventory().validateCapacity(item_templ))
					{
						dropItem(player, item);
					}
					else
					{
						player.doAutoLoot(this, item);
					}
				}
				else
				{
					dropItem(player, item);
				}

				item = null;
			}
		}

		player = null;
	}

	public L2ItemInstance dropItem(L2PcInstance mainDamageDealer, RewardItem item)
	{
		int randDropLim = 70;

		L2ItemInstance ditem = null;

		for(int i = 0; i < item.getCount(); i++)
		{
			int newX = getX() + Rnd.get(randDropLim * 2 + 1) - randDropLim;
			int newY = getY() + Rnd.get(randDropLim * 2 + 1) - randDropLim;
			int newZ = Math.max(getZ(), mainDamageDealer.getZ()) + 20;

			ditem = ItemTable.getInstance().createItem("Loot", item.getItemId(), item.getCount(), mainDamageDealer, this);
			ditem.getDropProtection().protect(mainDamageDealer);
			ditem.dropMe(this, newX, newY, newZ);

			if(!Config.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
			{
				if(Config.AUTODESTROY_ITEM_AFTER > 0 && ditem.getItemType() != L2EtcItemType.HERB || Config.HERB_AUTO_DESTROY_TIME > 0 && ditem.getItemType() == L2EtcItemType.HERB)
				{
					ItemsAutoDestroy.getInstance().addItem(ditem);
				}
			}

			ditem.setProtected(false);

			if(ditem.isStackable() || !Config.MULTIPLE_ITEM_DROP)
			{
				break;
			}
		}

		return ditem;
	}

	public L2ItemInstance DropItem(L2PcInstance lastAttacker, int itemId, int itemCount)
	{
		return dropItem(lastAttacker, new RewardItem(itemId, itemCount));
	}

	public L2ItemInstance getActiveWeapon()
	{
		return null;
	}

	public boolean noTarget()
	{
		return getAggroListRP().isEmpty();
	}

	public boolean containsTarget(L2Character player)
	{
		return getAggroListRP().containsKey(player);
	}

	public void clearAggroList()
	{
		getAggroList().clear();

		_overhit = false;
		_overhitDamage = 0;
		_overhitAttacker = null;
	}

	public boolean isSweepActive()
	{
		return _sweepItems != null;
	}

	public synchronized RewardItem[] takeSweep()
	{
		RewardItem[] sweep = _sweepItems;

		_sweepItems = null;
		return sweep;
	}

	public synchronized RewardItem[] takeHarvest()
	{
		RewardItem[] harvest = _harvestItems;

		_harvestItems = null;
		return harvest;
	}


	public void overhitEnabled(boolean status)
	{
		_overhit = status;
	}

	public void setOverhitValues(L2Character attacker, double damage)
	{
		double overhitDmg = ((getCurrentHp() - damage) * (-1));

		if(overhitDmg < 0)
		{
			overhitEnabled(false);
			_overhitDamage = 0;
			_overhitAttacker = null;
			return;
		}

		overhitEnabled(true);
		_overhitDamage = overhitDmg;
		_overhitAttacker = attacker;
	}

	public L2Character getOverhitAttacker()
	{
		return _overhitAttacker;
	}

	public double getOverhitDamage()
	{
		return _overhitDamage;
	}

	public boolean isOverhit()
	{
		return _overhit;
	}

	public void absorbSoul()
	{
		_absorbed = true;
	}

	public boolean isAbsorbed()
	{
		return _absorbed;
	}

	public void addAbsorber(L2PcInstance attacker, int crystalId)
	{
		if(!(this instanceof L2MonsterInstance))
		{
			return;
		}

		if(attacker == null)
		{
			return;
		}

		if(getAbsorbLevel() == 0)
		{
			return;
		}

		AbsorberInfo ai = _absorbersList.get(attacker);

		if(ai == null)
		{
			ai = new AbsorberInfo(attacker, crystalId, getCurrentHp());
			_absorbersList.put(attacker, ai);
		}
		else
		{
			ai._absorber = attacker;
			ai._crystalId = crystalId;
			ai._absorbedHP = getCurrentHp();
		}

		absorbSoul();

		ai = null;
	}

	private void levelSoulCrystals(L2Character attacker)
	{
		if(!(attacker instanceof L2PcInstance) && !(attacker instanceof L2Summon))
		{
			resetAbsorbList();
			return;
		}

		int maxAbsorbLevel = getAbsorbLevel();
		int minAbsorbLevel = 0;

		if(maxAbsorbLevel == 0)
		{
			resetAbsorbList();
			return;
		}

		if(maxAbsorbLevel > 10)
		{
			minAbsorbLevel = maxAbsorbLevel > 12 ? 12 : 10;
		}

		boolean isSuccess = true;
		boolean doLevelup = true;
		boolean isBossMob = maxAbsorbLevel > 10 ? true : false;

		L2NpcTemplate.AbsorbCrystalType absorbType = getTemplate().absorbType;

		L2PcInstance killer = attacker instanceof L2Summon ? ((L2Summon) attacker).getOwner() : (L2PcInstance) attacker;

		if(!isBossMob)
		{
			if(!isAbsorbed())
			{
				resetAbsorbList();
				return;
			}

			AbsorberInfo ai = _absorbersList.get(killer);
			if(ai == null || ai._absorber.getObjectId() != killer.getObjectId())
			{
				isSuccess = false;
			}

			if(ai != null && ai._absorbedHP > getMaxHp() / 2.0)
			{
				isSuccess = false;
			}

			if(!isSuccess)
			{
				resetAbsorbList();
				return;
			}

			ai = null;
		}

		String[] crystalNFO = null;
		String crystalNME = "";

		int dice = Rnd.get(100);
		int crystalQTY = 0;
		int crystalLVL = 0;
		int crystalOLD = 0;
		int crystalNEW = 0;

		List<L2PcInstance> players = new FastList<L2PcInstance>();

		if(absorbType == L2NpcTemplate.AbsorbCrystalType.FULL_PARTY && killer.isInParty())
		{
			players = killer.getParty().getPartyMembers();
		}
		else if(absorbType == L2NpcTemplate.AbsorbCrystalType.PARTY_ONE_RANDOM && killer.isInParty())
		{
			players.add(killer.getParty().getPartyMembers().get(Rnd.get(killer.getParty().getMemberCount())));
		}
		else
		{
			players.add(killer);
		}

		for(L2PcInstance player : players)
		{
			if(player == null)
			{
				continue;
			}

			crystalQTY = 0;

			L2ItemInstance[] inv = player.getInventory().getItems();
			for(L2ItemInstance item : inv)
			{
				int itemId = item.getItemId();
				for(int id : SoulCrystal.SoulCrystalTable)
				{
					if(id == itemId)
					{
						crystalQTY++;
						if(crystalQTY > 1)
						{
							isSuccess = false;
							break;
						}

						if(id != SoulCrystal.RED_NEW_CRYSTAL && id != SoulCrystal.GRN_NEW_CYRSTAL && id != SoulCrystal.BLU_NEW_CRYSTAL)
						{
							try
							{
								if(item.getItem().getName().contains("Grade"))
								{
									crystalNFO = item.getItem().getName().trim().replace(" Grade ", "-").split("-");
									crystalLVL = 13;
									crystalNME = crystalNFO[0].toLowerCase();
								}
								else
								{
									crystalNFO = item.getItem().getName().trim().replace(" Stage ", "").split("-");
									crystalLVL = Integer.parseInt(crystalNFO[1].trim());
									crystalNME = crystalNFO[0].toLowerCase();
								}

								if(crystalLVL > 9)
								{
									for(int[] element : SoulCrystal.HighSoulConvert)
									{
										if(id == element[0])
										{
											crystalNEW = element[1];
											break;
										}
									}
								}
								else
								{
									crystalNEW = id + 1;
								}
							}
							catch(NumberFormatException nfe)
							{
								_log.warn("An attempt to identify a soul crystal failed, " + "verify the names have not changed in etcitem table.", nfe);

								player.sendMessage("There has been an error handling your soul crystal." + " Please notify your server admin.");

								isSuccess = false;
								break;
							}
							catch(Exception e)
							{
								_log.error("", e);
								isSuccess = false;
								break;
							}
						}
						else
						{
							crystalNME = item.getItem().getName().toLowerCase().trim();
							crystalNEW = id + 1;
						}

						crystalOLD = id;
						break;
					}
				}

				if(!isSuccess)
				{
					break;
				}
			}

			inv = null;

			if(crystalLVL < minAbsorbLevel || crystalLVL >= maxAbsorbLevel)
			{
				doLevelup = false;
			}

			if (crystalQTY != 1 || !isSuccess || !doLevelup)
			{
				if(crystalQTY > 1)
				{
					player.sendPacket(new SystemMessage(SystemMessageId.SOUL_CRYSTAL_ABSORBING_FAILED_RESONATION));
				}
				else if (!doLevelup && crystalQTY > 0)
				{
					player.sendPacket(new SystemMessage(SystemMessageId.SOUL_CRYSTAL_ABSORBING_REFUSED));
				}

				crystalQTY = 0;

				continue;
			}

			int chanceLevelUp = isBossMob ? 70 : SoulCrystal.LEVEL_CHANCE;

			if(absorbType == L2NpcTemplate.AbsorbCrystalType.FULL_PARTY && doLevelup || dice <= chanceLevelUp)
			{
				exchangeCrystal(player, crystalOLD, crystalNEW, false);
			}
			else if(!isBossMob && dice >= 100.0 - SoulCrystal.BREAK_CHANCE)
			{
				if(crystalNME.startsWith("red"))
				{
					exchangeCrystal(player, crystalOLD, SoulCrystal.RED_BROKEN_CRYSTAL, true);
				}
				else if(crystalNME.startsWith("gre"))
				{
					exchangeCrystal(player, crystalOLD, SoulCrystal.GRN_BROKEN_CYRSTAL, true);
				}
				else if(crystalNME.startsWith("blu"))
				{
					exchangeCrystal(player, crystalOLD, SoulCrystal.BLU_BROKEN_CRYSTAL, true);
				}

				resetAbsorbList();
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessageId.SOUL_CRYSTAL_ABSORBING_FAILED));
			}
		}

		killer = null;
		players = null;
		crystalNFO = null;
		crystalNME = null;
	}

	private void exchangeCrystal(L2PcInstance player, int takeid, int giveid, boolean broke)
	{
		L2ItemInstance Item = player.getInventory().destroyItemByItemId("SoulCrystal", takeid, 1, player, this);

		if(Item != null)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addRemovedItem(Item);

			Item = player.getInventory().addItem("SoulCrystal", giveid, 1, player, this);
			playerIU.addItem(Item);

			if(broke)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.SOUL_CRYSTAL_BROKE));
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessageId.SOUL_CRYSTAL_ABSORBING_SUCCEEDED));
			}

			player.sendPacket(new SystemMessage(SystemMessageId.EARNED_ITEM_S1).addItemName(giveid));

			player.sendPacket(playerIU);

			playerIU = null;
		}
	}

	private void resetAbsorbList()
	{
		_absorbed = false;
		_absorbersList.clear();
	}

	private int[] calculateExpAndSp(int diff, int damage)
	{
		double xp;
		double sp;

		if(diff < -5)
		{
			diff = -5;
		}

		xp = (double) getExpReward() * damage / getMaxHp();

		if(Config.ALT_GAME_EXPONENT_XP != 0)
		{
			xp *= Math.pow(2., -diff / Config.ALT_GAME_EXPONENT_XP);
		}

		sp = (double) getSpReward() * damage / getMaxHp();

		if(Config.ALT_GAME_EXPONENT_SP != 0)
		{
			sp *= Math.pow(2., -diff / Config.ALT_GAME_EXPONENT_SP);
		}

		if(Config.ALT_GAME_EXPONENT_XP == 0 && Config.ALT_GAME_EXPONENT_SP == 0)
		{
			if(diff > 5)
			{
				double pow = Math.pow((double) 5 / 6, diff - 5);
				xp = xp * pow;
				sp = sp * pow;
			}

			if(xp <= 0)
			{
				xp = 0;
				sp = 0;
			}
			else if(sp <= 0)
			{
				sp = 0;
			}
		}

		int[] tmp =
		{
				(int) xp, (int) sp
		};

		return tmp;
	}

	public long calculateOverhitExp(long normalExp)
	{
		double overhitPercentage = getOverhitDamage() * 100 / getMaxHp();

		if(overhitPercentage > 25)
		{
			overhitPercentage = 25;
		}

		double overhitExp = overhitPercentage / 100 * normalExp;

		long bonusOverhit = Math.round(overhitExp);

		return bonusOverhit;
	}

	@Override
	public boolean isAttackable()
	{
		return true;
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();
		setSpoil(false);
		clearAggroList();
		_harvestItems = null;
		setSeeded(false);
		overhitEnabled(false);
		_sweepItems = null;
		resetAbsorbList();
		setWalking();

		if(!isInActiveRegion())
		{
			if(this instanceof L2SiegeGuardInstance)
			{
				((L2SiegeGuardAI) getAI()).stopAITask();
			}
			else
			{
				((L2AttackableAI) getAI()).stopAITask();
			}
		}
	}

	public void setSeeded()
	{
		if(_seedType != 0 && _seeder != null)
		{
			setSeeded(_seedType, _seeder.getLevel());
		}
	}

	public void setSeeded(int id, L2PcInstance seeder)
	{
		if(!_seeded)
		{
			_seedType = id;
			_seeder = seeder;
		}
	}

	public void setSeeded(int id, int seederLvl)
	{
		_seeded = true;
		_seedType = id;
		int count = 1;

		Map<Integer, L2Skill> skills = getTemplate().getSkills();

		if(skills != null)
		{
			for(int skillId : skills.keySet())
			{
				switch(skillId)
				{
					case 4303:
						count *= 2;
						break;
					case 4304:
						count *= 3;
						break;
					case 4305:
						count *= 4;
						break;
					case 4306:
						count *= 5;
						break;
					case 4307:
						count *= 6;
						break;
					case 4308:
						count *= 7;
						break;
					case 4309:
						count *= 8;
						break;
					case 4310:
						count *= 9;
						break;
				}
			}
		}

		int diff = getLevel() - (L2Manor.getInstance().getSeedLevel(_seedType) - 5);

		if(diff > 0)
		{
			count += diff;
		}

		FastList<RewardItem> harvested = new FastList<RewardItem>();

		harvested.add(new RewardItem(L2Manor.getInstance().getCropType(_seedType), count * Config.RATE_DROP_MANOR));

		_harvestItems = harvested.toArray(new RewardItem[harvested.size()]);

		skills = null;
		harvested = null;
	}

	public void setSeeded(boolean seeded)
	{
		_seeded = seeded;
	}

	public L2PcInstance getSeeder()
	{
		return _seeder;
	}

	public int getSeedType()
	{
		return _seedType;
	}

	public boolean isSeeded()
	{
		return _seeded;
	}

	private int getAbsorbLevel()
	{
		return getTemplate().absorbLevel;
	}

	@Override
	public boolean hasRandomAnimation()
	{
		return Config.MAX_MONSTER_ANIMATION > 0 && !(this instanceof L2GrandBossInstance);
	}

	@Override
	public boolean isMob()
	{
		return true;
	}

	protected void setCommandChannelTimer(CommandChannelTimer commandChannelTimer)
	{
		_commandChannelTimer = commandChannelTimer;
	}

	public CommandChannelTimer getCommandChannelTimer()
	{
		return _commandChannelTimer;
	}

	public L2CommandChannel getFirstCommandChannelAttacked()
	{
		return _firstCommandChannelAttacked;
	}

	public void setFirstCommandChannelAttacked(L2CommandChannel firstCommandChannelAttacked)
	{
		_firstCommandChannelAttacked = firstCommandChannelAttacked;
	}

	private class CommandChannelTimer implements Runnable
	{
		private L2Attackable _monster;
		private L2CommandChannel _channel;

		public CommandChannelTimer(L2Attackable monster, L2CommandChannel channel)
		{
			_monster = monster;
			_channel = channel;
		}

		@Override
		public void run()
		{
			_monster.setCommandChannelTimer(null);
			_monster.setFirstCommandChannelAttacked(null);

			for(L2Character player : _monster.getAggroListRP().keySet())
			{
				if(player.isInParty() && player.getParty().isInCommandChannel())
				{
					if(player.getParty().getCommandChannel().equals(_channel))
					{
						_monster.setCommandChannelTimer(this);
						_monster.setFirstCommandChannelAttacked(_channel);
						ThreadPoolManager.getInstance().scheduleGeneral(this, 300000);
						break;
					}
				}
			}
		}
	}

	private boolean _seeThroughSilentMove = false;

	public boolean canSeeThroughSilentMove()
	{
		return _seeThroughSilentMove;
	}

	public void setSeeThroughSilentMove(boolean val)
	{
		_seeThroughSilentMove = val;
	}
}