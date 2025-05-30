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

import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_FOLLOW;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;

import com.src.Config;
import com.src.gameserver.GameTimeController;
import com.src.gameserver.ai.CtrlEvent;
import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.ai.L2AttackableAI;
import com.src.gameserver.ai.L2CharacterAI;
import com.src.gameserver.datatables.HeroSkillTable;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.xml.MapRegionTable;
import com.src.gameserver.datatables.xml.MapRegionTable.TeleportWhereType;
import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.geo.GeoData;
import com.src.gameserver.geo.pathfinding.Node;
import com.src.gameserver.geo.pathfinding.PathFinding;
import com.src.gameserver.geo.util.Door;
import com.src.gameserver.handler.ISkillHandler;
import com.src.gameserver.handler.SkillHandler;
import com.src.gameserver.managers.DimensionalRiftManager;
import com.src.gameserver.managers.GrandBossManager;
import com.src.gameserver.managers.RaidBossSpawnManager;
import com.src.gameserver.managers.TownManager;
import com.src.gameserver.model.ChanceSkillList;
import com.src.gameserver.model.CharEffectList;
import com.src.gameserver.model.ForceBuff;
import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Party;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.L2Skill.SkillTargetType;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.L2WorldRegion;
import com.src.gameserver.model.Location;
import com.src.gameserver.model.actor.instance.L2BoatInstance;
import com.src.gameserver.model.actor.instance.L2ControlTowerInstance;
import com.src.gameserver.model.actor.instance.L2DoorInstance;
import com.src.gameserver.model.actor.instance.L2EffectPointInstance;
import com.src.gameserver.model.actor.instance.L2FlameTowerInstance;
import com.src.gameserver.model.actor.instance.L2FriendlyMobInstance;
import com.src.gameserver.model.actor.instance.L2GrandBossInstance;
import com.src.gameserver.model.actor.instance.L2GuardInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2MinionInstance;
import com.src.gameserver.model.actor.instance.L2MonsterInstance;
import com.src.gameserver.model.actor.instance.L2NpcInstance;
import com.src.gameserver.model.actor.instance.L2NpcWalkerInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance.SkillDat;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.model.actor.instance.L2RaidBossInstance;
import com.src.gameserver.model.actor.instance.L2RiftInvaderInstance;
import com.src.gameserver.model.actor.instance.L2SiegeFlagInstance;
import com.src.gameserver.model.actor.instance.L2SiegeGuardInstance;
import com.src.gameserver.model.actor.instance.L2SiegeSummonInstance;
import com.src.gameserver.model.actor.instance.L2SummonInstance;
import com.src.gameserver.model.actor.knownlist.CharKnownList;
import com.src.gameserver.model.actor.knownlist.ObjectKnownList.KnownListAsynchronousUpdateTask;
import com.src.gameserver.model.actor.position.L2CharPosition;
import com.src.gameserver.model.actor.stat.CharStat;
import com.src.gameserver.model.actor.status.CharStatus;
import com.src.gameserver.model.entity.Duel;
import com.src.gameserver.model.entity.olympiad.Olympiad;
import com.src.gameserver.model.extender.BaseExtender.EventType;
import com.src.gameserver.model.itemcontainer.Inventory;
import com.src.gameserver.model.quest.Quest;
import com.src.gameserver.model.quest.QuestState;
import com.src.gameserver.model.zone.type.L2BossZone;
import com.src.gameserver.network.Disconnection;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.Attack;
import com.src.gameserver.network.serverpackets.BeginRotation;
import com.src.gameserver.network.serverpackets.ChangeMoveType;
import com.src.gameserver.network.serverpackets.ChangeWaitType;
import com.src.gameserver.network.serverpackets.CharInfo;
import com.src.gameserver.network.serverpackets.CharMoveToLocation;
import com.src.gameserver.network.serverpackets.ExOlympiadSpelledInfo;
import com.src.gameserver.network.serverpackets.FlyToLocation;
import com.src.gameserver.network.serverpackets.FlyToLocation.FlyType;
import com.src.gameserver.network.serverpackets.L2GameServerPacket;
import com.src.gameserver.network.serverpackets.MagicEffectIcons;
import com.src.gameserver.network.serverpackets.MagicSkillCanceld;
import com.src.gameserver.network.serverpackets.MagicSkillLaunched;
import com.src.gameserver.network.serverpackets.MagicSkillUser;
import com.src.gameserver.network.serverpackets.NpcInfo;
import com.src.gameserver.network.serverpackets.PartySpelled;
import com.src.gameserver.network.serverpackets.PetInfo;
import com.src.gameserver.network.serverpackets.RelationChanged;
import com.src.gameserver.network.serverpackets.Revive;
import com.src.gameserver.network.serverpackets.SetupGauge;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.StopMove;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.network.serverpackets.TargetUnselected;
import com.src.gameserver.network.serverpackets.TeleportToLocation;
import com.src.gameserver.network.serverpackets.ValidateLocationInVehicle;
import com.src.gameserver.skills.Calculator;
import com.src.gameserver.skills.Formulas;
import com.src.gameserver.skills.Stats;
import com.src.gameserver.skills.effects.EffectCharge;
import com.src.gameserver.skills.funcs.Func;
import com.src.gameserver.taskmanager.AttackStanceTaskManager;
import com.src.gameserver.templates.StatsSet;
import com.src.gameserver.templates.chars.L2CharTemplate;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.templates.item.L2Weapon;
import com.src.gameserver.templates.item.L2WeaponType;
import com.src.gameserver.templates.skills.L2EffectType;
import com.src.gameserver.templates.skills.L2SkillType;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.gameserver.util.Broadcast;
import com.src.gameserver.util.Util;
import com.src.util.random.Rnd;

public abstract class L2Character extends L2Object
{
	protected static final Logger _log = Logger.getLogger(L2Character.class.getName());

	private List<L2Character> _attackByList;
	private volatile boolean _isCastingNow = false;
	private volatile boolean _isCastingSimultaneouslyNow = false;
	private L2Skill _lastSkillCast;
	private L2Skill _lastSimultaneousSkillCast;
	private boolean _isAfraid = false;
	private boolean _isConfused = false;
	private boolean _isBuffProtected = false;
	private boolean _isFakeDeath = false;
	private boolean _isFlying = false;
	private boolean _isMuted = false;
	private boolean _isPsychicalMuted = false;
	private boolean _isKilledAlready = false;
	private boolean _isImobilised = false;
	private boolean _isOverloaded = false;
	private boolean _isParalyzed = false;
	private boolean _isRiding = false;
	private boolean _isPendingRevive = false;
	private boolean _isRooted = false;
	private boolean _isRunning = false;
	private boolean _isNoRndWalk = false;
	private boolean _isImmobileUntilAttacked = false;
	private boolean _isSleeping = false;
	private boolean _isStunned = false;
	private boolean _isBetrayed = false;
	protected boolean _showSummonAnimation = false;
	protected boolean _isTeleporting = false;
	protected boolean _isInvul = false;
	private int _lastHealAmount = 0;
	private CharStat _stat;
	private CharStatus _status;
	private L2CharTemplate _template;
	private String _title;
	private String _aiClass = "default";
	private double _hpUpdateIncCheck = .0;
	private double _hpUpdateDecCheck = .0;
	private double _hpUpdateInterval = .0;
	private boolean _champion = false;
	private Calculator[] _calculators;

	public final Map<Integer, L2Skill> _skills;

	protected ChanceSkillList _chanceSkills;

	protected ForceBuff _forceBuff;

	private boolean _blocked;
	private boolean _meditated;

	public static final int ZONE_PVP = 1;
	public static final int ZONE_PEACE = 2;
	public static final int ZONE_SIEGE = 4;
	public static final int ZONE_MOTHERTREE = 8;
	public static final int ZONE_CLANHALL = 16;
	public static final int ZONE_UNUSED = 32;
	public static final int ZONE_NOLANDING = 64;
	public static final byte ZONE_NOSTORE = 12;
	public static final int ZONE_WATER = 128;
	public static final int ZONE_JAIL = 256;
	public static final int ZONE_MONSTERTRACK = 512;
	public static final int ZONE_SWAMP = 1024;
	public static final int ZONE_NOSUMMONFRIEND = 2048;
	public static final int ZONE_OLY = 4096;
	public static final int ZONE_NOHQ = 8192;
	public static final byte ZONE_DANGERAREA = 17;
	public static final byte ZONE_CASTONARTIFACT = 18;
	public static final byte ZONE_NORESTART = 19;
	public static final byte ZONE_TOWN = 13;
	public static final byte ZONE_SCRIPT = 14;
	
	private static final int[] SKILL_IDS =
	{
		2039, 2150, 2151, 2152, 2153, 2154
	};

	private int _currentZones = 0;

	public boolean isInsideZone(int zone)
	{
		return (_currentZones & zone) != 0;
	}

	public void setInsideZone(int zone, boolean state)
	{
		if(state)
		{
			_currentZones |= zone;
		}
		else if(isInsideZone(zone))
		{
			_currentZones ^= zone;
		}
	}

	private L2Character _debugger = null;

	/**
	 * @return True if debugging is enabled for this L2Character
	 */
	public boolean isDebug()
	{
		return _debugger != null;
	}

	/**
	 * Sets L2Character instance, to which debug packets will be send
	 * 
	 * @param d
	 */
	public void setDebug(L2Character d)
	{
		_debugger = d;
	}

	/**
	 * Send debug packet.
	 * 
	 * @param pkt
	 */
	public void sendDebugPacket(L2GameServerPacket pkt)
	{
		if(_debugger != null)
		{
			_debugger.sendPacket(pkt);
		}
	}

	/**
	 * Send debug text string
	 * 
	 * @param msg
	 */
	public void sendDebugMessage(String msg)
	{
		if(_debugger != null)
		{
			_debugger.sendMessage(msg);
		}
	}
	
	public boolean charIsGM()
	{
		if(this instanceof L2PcInstance)
		{
			if(((L2PcInstance)this).isGM())
			{
				return true;
			}
		}

		return false;
	}

	public L2Character(int objectId, L2CharTemplate template)
	{
		super(objectId);
		getKnownList();

		_template = template;

		if(template != null && this instanceof L2Npc)
		{
			_calculators = NPC_STD_CALCULATOR;

			_skills = ((L2NpcTemplate) template).getSkills();

			if(_skills != null)
			{
				for(Map.Entry<Integer, L2Skill> skill : _skills.entrySet())
				{
					addStatFuncs(skill.getValue().getStatFuncs(null, this));
				}
			}
			
		}
		else
		{
			_skills = new FastMap<Integer, L2Skill>().shared();

			_calculators = new Calculator[Stats.NUM_STATS];
			Formulas.getInstance().addFuncsToNewCharacter(this);
		}

		if (Config.ALT_INVUL_NPC)
		{
			if (!(this instanceof L2Attackable) && !(this instanceof L2PcInstance) && !(this instanceof L2GuardInstance) && !(this instanceof L2SiegeGuardInstance) && !(this instanceof L2ControlTowerInstance) && !(this instanceof L2FlameTowerInstance) && !(this instanceof L2DoorInstance) && !(this instanceof L2FriendlyMobInstance) && !(this instanceof L2SiegeSummonInstance) && !(this instanceof L2PetInstance) && !(this instanceof L2SummonInstance) && !(this instanceof L2SiegeFlagInstance) && !(this instanceof L2EffectPointInstance))
		
			setIsInvul(true);
		}
	}

	
	
	protected void initCharStatusUpdateValues()
	{
		_hpUpdateInterval = getMaxHp() / 352.0;
		_hpUpdateIncCheck = getMaxHp();
		_hpUpdateDecCheck = getMaxHp() - _hpUpdateInterval;
	}

	public void onDecay()
	{
		L2WorldRegion reg = getWorldRegion();

		if(reg != null)
		{
			reg.removeFromZones(this);
		}

		decayMe();

		reg = null;
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();
		revalidateZone();
	}

	public void onTeleported()
	{
		if(!isTeleporting())
		{
			return;
		}

		if(this instanceof L2Summon)
 		{
 			((L2Summon)this).getOwner().sendPacket(new TeleportToLocation(this, getPosition().getX(), getPosition().getY(), getPosition().getZ()));
 		}

		spawnMe();

		setIsTeleporting(false);

		if(_isPendingRevive)
		{
			doRevive();
		}

		if(getPet() != null)
		{
			getPet().setFollowStatus(false);
			getPet().teleToLocation(getPosition().getX() + Rnd.get(-100, 100), getPosition().getY() + Rnd.get(-100, 100), getPosition().getZ(), false);
			getPet().setFollowStatus(true);
		}

	}

	public void addAttackerToAttackByList(L2Character player)
	{
		if(player == null || player == this || getAttackByList() == null || getAttackByList().contains(player))
		{
			return;
		}

		getAttackByList().add(player);
	}

	protected byte _startingRotationCounter = 4;

	public synchronized boolean isStartingRotationAllowed()
	{
		_startingRotationCounter--;
		if(_startingRotationCounter < 0)
		{
			_startingRotationCounter = 4;
		}

		if(_startingRotationCounter == 4)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public final void broadcastPacket(L2GameServerPacket mov)
	{
		if(!(mov instanceof CharInfo))
		{
			sendPacket(mov);
		}

		if(mov instanceof BeginRotation  && !isStartingRotationAllowed())
		{
			return;
		}

		for(L2PcInstance player : getKnownList().getKnownPlayers().values())
		{
			if(player!=null)
			{
				try
				{
					player.sendPacket(mov);

					if(mov instanceof CharInfo && this instanceof L2PcInstance)
					{
						{
							int relation = ((L2PcInstance) this).getRelation(player);

							if(getKnownList().getKnownRelations().get(player.getObjectId()) != null && getKnownList().getKnownRelations().get(player.getObjectId()) != relation)
							{
								player.sendPacket(new RelationChanged((L2PcInstance) this, relation, player.isAutoAttackable(this)));
							}
						}
					}
				}
				catch(NullPointerException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	public void broadcastPacket(L2GameServerPacket mov, int radiusInKnownlist) 
 	{ 
		Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values(); 
 		for (L2PcInstance player : plrs) 
 		{ 
 			if (player != null && isInsideRadius(player, radiusInKnownlist, false, false)) 
 				player.sendPacket(mov); 
 		}
 	}

	protected boolean needHpUpdate(int barPixels)
	{
		double currentHp = getCurrentHp();

		if(currentHp <= 1.0 || getMaxHp() < barPixels)
		{
			return true;
		}

		if(currentHp <= _hpUpdateDecCheck || currentHp >= _hpUpdateIncCheck)
		{
			if(currentHp == getMaxHp())
			{
				_hpUpdateIncCheck = currentHp + 1;
				_hpUpdateDecCheck = currentHp - _hpUpdateInterval;
			}
			else
			{
				double doubleMulti = currentHp / _hpUpdateInterval;
				int intMulti = (int) doubleMulti;

				_hpUpdateDecCheck = _hpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_hpUpdateIncCheck = _hpUpdateDecCheck + _hpUpdateInterval;
			}

			return true;
		}

		return false;
	}

	public void broadcastStatusUpdate()
	{
		if(getStatus().getStatusListener().isEmpty())
		{
			return;
		}

		if(!needHpUpdate(352))
		{
			return;
		}

		StatusUpdate su = new StatusUpdate(getObjectId()); 
		su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp()); 
		su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp()); 

		for(L2Character temp : getStatus().getStatusListener())
		{
			if(temp != null)
			{
				temp.sendPacket(su);
			}
		}

		su = null;
	}

	public void sendPacket(L2GameServerPacket mov)
	{
	}

	public void teleToLocation(int x, int y, int z, boolean allowRandomOffset)
	{
		stopMove(null, false);
		abortAttack();
		abortCast();
		setIsTeleporting(true);
		setTarget(null);

		if(getWorldRegion() != null)
		{
			getWorldRegion().removeFromZones(this);
		}

		getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);

		if(Config.RESPAWN_RANDOM_ENABLED && allowRandomOffset)
		{
			x += Rnd.get(-Config.RESPAWN_RANDOM_MAX_OFFSET, Config.RESPAWN_RANDOM_MAX_OFFSET);
			y += Rnd.get(-Config.RESPAWN_RANDOM_MAX_OFFSET, Config.RESPAWN_RANDOM_MAX_OFFSET);
		}

		z += 5;

		broadcastPacket(new TeleportToLocation(this, x, y, z));

		getPosition().setXYZ(x, y, z);

		decayMe();

		getPosition().setWorldPosition(x, y, z);

		if(!(this instanceof L2PcInstance))
		{
			onTeleported();
		}
	}

	public void teleToLocation(int x, int y, int z)
	{
		teleToLocation(x, y, z, false);
	}

	public void teleToLocation(Location loc, boolean allowRandomOffset)
	{
		int x = loc.getX();
		int y = loc.getY();
		int z = loc.getZ();

		if(this instanceof L2PcInstance && DimensionalRiftManager.getInstance().checkIfInRiftZone(getX(), getY(), getZ(), true))
		{
			L2PcInstance player = (L2PcInstance) this;
			player.sendMessage("You have been sent to the waiting room.");

			if(player.isInParty() && player.getParty().isInDimensionalRift())
			{
				player.getParty().getDimensionalRift().usedTeleport(player);
			}

			int[] newCoords = DimensionalRiftManager.getInstance().getRoom((byte) 0, (byte) 0).getTeleportCoords();

			x = newCoords[0];
			y = newCoords[1];
			z = newCoords[2];

			player = null;
		}
		teleToLocation(x, y, z, allowRandomOffset);
	}

	public void teleToLocation(TeleportWhereType teleportWhere)
	{
		teleToLocation(MapRegionTable.getInstance().getTeleToLocation(this, teleportWhere), true);
	}

	protected void doAttack(L2Character target)
	{
		if(target == null)
		{
			return;
		}

		if(isAlikeDead())
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);

			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(this instanceof L2Npc && target.isAlikeDead())
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);

			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(this instanceof L2PcInstance && target.isDead() && !target.isFakeDeath())
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);

			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(!getKnownList().knowsObject(target))
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);

			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(this instanceof L2PcInstance && isDead())
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);

			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(target instanceof L2PcInstance && ((L2PcInstance) target).getDuelState() == Duel.DUELSTATE_DEAD)
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);

			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(target instanceof L2DoorInstance && !((L2DoorInstance) target).isAttackable(this))
		{
			return;
		}

		if(isAttackingDisabled())
		{
			return;
		}

		if(this instanceof L2PcInstance)
		{
			if(((L2PcInstance) this).inObserverMode())
			{
				sendPacket(new SystemMessage(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE));
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			if(target instanceof L2PcInstance)
			{
				if(((L2PcInstance) target).isCursedWeaponEquiped() && ((L2PcInstance) this).getLevel() <= Config.MAX_LEVEL_NEWBIE)
				{
					((L2PcInstance) this).sendMessage("Can't attack a cursed player when under level 21.");
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}

				if(((L2PcInstance) this).isCursedWeaponEquiped() && ((L2PcInstance) target).getLevel() <= Config.MAX_LEVEL_NEWBIE)
				{
					((L2PcInstance) this).sendMessage("Can't attack a newbie player using a cursed weapon.");
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}

			if(getObjectId() == target.getObjectId())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}

		L2ItemInstance weaponInst = getActiveWeaponInstance();

		L2Weapon weaponItem = getActiveWeaponItem();

		if(weaponItem != null && weaponItem.getItemType() == L2WeaponType.ROD)
		{
			((L2PcInstance) this).sendPacket(new SystemMessage(SystemMessageId.CANNOT_ATTACK_WITH_FISHING_POLE));
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);

			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(!GeoData.getInstance().canSeeTarget(this, target))
		{
			sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(weaponItem != null && weaponItem.getItemType() == L2WeaponType.BOW)
		{
			if(!checkAndEquipArrows())
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);

				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ARROWS));
				return;
			}

			if(this instanceof L2PcInstance)
			{
				if(target.isInsidePeaceZone((L2PcInstance) this))
				{
					getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}

				if(_disableBowAttackEndTime <= GameTimeController.getGameTicks())
				{
					int saMpConsume = (int) getStat().calcStat(Stats.MP_CONSUME, 0, null, null);
					int mpConsume = saMpConsume == 0 ? weaponItem.getMpConsume() : saMpConsume;

					if(getCurrentMp() < mpConsume)
					{
						ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(CtrlEvent.EVT_READY_TO_ACT), 1000);

						sendPacket(ActionFailed.STATIC_PACKET);
						sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_MP));
						return;
					}
					getStatus().reduceMp(mpConsume);

					_disableBowAttackEndTime = 5 * GameTimeController.TICKS_PER_SECOND + GameTimeController.getGameTicks();
				}
				else
				{
					ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(CtrlEvent.EVT_READY_TO_ACT), 1000);

					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			else if(this instanceof L2Npc)
			{
				if(_disableBowAttackEndTime > GameTimeController.getGameTicks())
				{
					return;
				}
			}
		}

		target.getKnownList().addKnownObject(this);

		if(Config.ALT_GAME_TIREDNESS)
		{
			setCurrentCp(getCurrentCp() - 10);
		}

		if(this instanceof L2PcInstance)
		{
			((L2PcInstance) this).rechargeAutoSoulShot(true, false, false);
		}
		else if(this instanceof L2Summon)
		{
			((L2Summon) this).getOwner().rechargeAutoSoulShot(true, false, true);
		}

		boolean wasSSCharged;

		if(this instanceof L2Summon && !(this instanceof L2PetInstance))
		{
			wasSSCharged = ((L2Summon) this).getChargedSoulShot() != L2ItemInstance.CHARGED_NONE;
		}
		else if(this instanceof L2MonsterInstance && ((L2MonsterInstance) this).getTemplate().getRace() == L2NpcTemplate.Race.HUMANOID && !this.isRaid())
		{
			if(this.getLevel() > 20 && this.getLevel() >= Rnd.get(100))
			{
				wasSSCharged = weaponItem != null;
				if(wasSSCharged)
				{
					Broadcast.toSelfAndKnownPlayersInRadius(this, new MagicSkillUser(this, this, SKILL_IDS[weaponItem.getCrystalType()], 1, 0, 0), 360000);
				}
			}
			else
			{
				wasSSCharged = false;
			}
		}
		else
		{
			wasSSCharged = weaponInst != null && weaponInst.getChargedSoulshot() != L2ItemInstance.CHARGED_NONE;
		}

		int timeAtk = calculateTimeBetweenAttacks(target, weaponItem);
		int timeToHit = timeAtk / 2;
		_attackEndTime = GameTimeController.getGameTicks();
		_attackEndTime += timeAtk / GameTimeController.MILLIS_IN_TICK;
		_attackEndTime -= 1;

		int ssGrade = 0;

		if(weaponItem != null)
		{
			ssGrade = weaponItem.getCrystalType();
		}

		Attack attack = new Attack(this, wasSSCharged, ssGrade);

		boolean hitted;

		setAttackingBodypart();

		int reuse = calculateReuseTime(target, weaponItem);

		if(weaponItem == null)
		{
			hitted = doAttackHitSimple(attack, target, timeToHit);
		}
		else if(weaponItem.getItemType() == L2WeaponType.BOW)
		{
			hitted = doAttackHitByBow(attack, target, timeAtk, reuse);
		}
		else if(weaponItem.getItemType() == L2WeaponType.POLE)
		{
			hitted = doAttackHitByPole(attack, timeToHit);
		}
		else if(isUsingDualWeapon())
		{
			hitted = doAttackHitByDual(attack, target, timeToHit);
		}
		else
		{
			hitted = doAttackHitSimple(attack, target, timeToHit);
		}

		L2PcInstance player = getActingPlayer();

		if(this instanceof L2PcInstance)
		{
			player = (L2PcInstance) this;
		}
		else if(this instanceof L2Summon)
		{
			player = ((L2Summon) this).getOwner();
		}

		if(player != null)
		{
			AttackStanceTaskManager.getInstance().addAttackStanceTask(player);
			
			if (player.getPet() != target)
				player.updatePvPStatus(target);
		}

		if(!hitted)
		{
			sendPacket(new SystemMessage(SystemMessageId.MISSED_TARGET));
			abortAttack();
		}
		else
		{
			if(this instanceof L2Summon && !(this instanceof L2PetInstance))
			{
				((L2Summon) this).setChargedSoulShot(L2ItemInstance.CHARGED_NONE);
			}
			else if(weaponInst != null)
			{
				weaponInst.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
			}

			if(player != null)
			{
				if(player.isCursedWeaponEquiped())
				{
					if(!target.isInvul())
					{
						target.setCurrentCp(0);
					}
				}
				else if(player.isHero())
				{
					if(target instanceof L2PcInstance && ((L2PcInstance) target).isCursedWeaponEquiped())
					{
						target.setCurrentCp(0);
					}
				}
			}

			weaponInst = null;
			weaponItem = null;
		}

		if(attack.hasHits())
		{
			broadcastPacket(attack);
			fireEvent(EventType.ATTACK.name, new Object[]
			{
				getTarget()
			});
		}
		
		ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(CtrlEvent.EVT_READY_TO_ACT), timeAtk + reuse);

		attack = null;
		player = null;
	}

	private boolean doAttackHitByBow(Attack attack, L2Character target, int sAtk, int reuse)
	{
		int damage1 = 0;
		boolean shld1 = false;
		boolean crit1 = false;

		boolean miss1 = Formulas.getInstance().calcHitMiss(this, target);

		reduceArrowCount();

		_move = null;

		if(!miss1)
		{
			shld1 = Formulas.calcShldUse(this, target);
			crit1 = Formulas.calcCrit(getStat().getCriticalHit(target, null));
			damage1 = (int) Formulas.getInstance().calcPhysDam(this, target, null, shld1, crit1, false, attack.soulshot);
		}

		if(this instanceof L2PcInstance)
		{
			sendPacket(new SystemMessage(SystemMessageId.GETTING_READY_TO_SHOOT_AN_ARROW));
			SetupGauge sg = new SetupGauge(SetupGauge.RED, sAtk + reuse);
			sendPacket(sg);
			sg = null;
		}

		ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, attack.soulshot, shld1), sAtk);

		_disableBowAttackEndTime = (sAtk + reuse) / GameTimeController.MILLIS_IN_TICK + GameTimeController.getGameTicks();

		attack.addHit(target, damage1, miss1, crit1, shld1);

		return !miss1;
	}

	private boolean doAttackHitByDual(Attack attack, L2Character target, int sAtk)
	{
		int damage1 = 0;
		int damage2 = 0;
		boolean shld1 = false;
		boolean shld2 = false;
		boolean crit1 = false;
		boolean crit2 = false;

		boolean miss1 = Formulas.getInstance().calcHitMiss(this, target);
		boolean miss2 = Formulas.getInstance().calcHitMiss(this, target);

		if(!miss1)
		{
			shld1 = Formulas.calcShldUse(this, target);

			crit1 = Formulas.calcCrit(getStat().getCriticalHit(target, null));

			damage1 = (int) Formulas.getInstance().calcPhysDam(this, target, null, shld1, crit1, true, attack.soulshot);
			damage1 /= 2;
		}

		if(!miss2)
		{
			shld2 = Formulas.calcShldUse(this, target);
			crit2 = Formulas.calcCrit(getStat().getCriticalHit(target, null));
			damage2 = (int) Formulas.getInstance().calcPhysDam(this, target, null, shld2, crit2, true, attack.soulshot);
			damage2 /= 2;
		}

		ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, attack.soulshot, shld1), sAtk / 2);

		ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage2, crit2, miss2, attack.soulshot, shld2), sAtk);

		attack.addHit(target, damage1, miss1, crit1, shld1);
		attack.addHit(target, damage2, miss2, crit2, shld2);

		return !miss1 || !miss2;
	}

	private boolean doAttackHitByPole(Attack attack, int sAtk)
	{
		boolean hitted = false;

		double angleChar, angleTarget;
		int maxRadius = (int) getStat().calcStat(Stats.POWER_ATTACK_RANGE, 66, null, null);
		int maxAngleDiff = (int) getStat().calcStat(Stats.POWER_ATTACK_ANGLE, 120, null, null);

		if(getTarget() == null)
		{
			return false;
		}

		angleTarget = Util.calculateAngleFrom(this, getTarget());
		setHeading((int) (angleTarget / 9.0 * 1610.0));

		angleChar = Util.convertHeadingToDegree(getHeading());
		double attackpercent = 85;
		int attackcountmax = (int) getStat().calcStat(Stats.ATTACK_COUNT_MAX, 3, null, null);
		int attackcount = 0;

		if(angleChar <= 0)
		{
			angleChar += 360;
		}

		L2Character target;
		for(L2Object obj : getKnownList().getKnownObjects().values())
		{
			if(obj instanceof L2Character)
			{
				if(obj instanceof L2PetInstance && this instanceof L2PcInstance && ((L2PetInstance) obj).getOwner() == (L2PcInstance) this)
				{
					continue;
				}

				if(!Util.checkIfInRange(maxRadius, this, obj, false))
				{
					continue;
				}

				if(Math.abs(obj.getZ() - getZ()) > Config.DIFFERENT_Z_CHANGE_OBJECT)
				{
					continue;
				}

				angleTarget = Util.calculateAngleFrom(this, obj);

				if(Math.abs(angleChar - angleTarget) > maxAngleDiff && Math.abs(angleChar + 360 - angleTarget) > maxAngleDiff && Math.abs(angleChar - (angleTarget + 360)) > maxAngleDiff)
				{
					continue;
				}

				target = (L2Character) obj;

				if(!target.isAlikeDead())
				{
					attackcount += 1;

					if(attackcount <= attackcountmax)
					{
						if(target == getAI().getAttackTarget() || target.isAutoAttackable(this))
						{

							hitted |= doAttackHitSimple(attack, target, attackpercent, sAtk);
							attackpercent /= 1.15;
						}
					}
				}
			}
		}

		target = null;

		return hitted;
	}

	private boolean doAttackHitSimple(Attack attack, L2Character target, int sAtk)
	{
		return doAttackHitSimple(attack, target, 100, sAtk);
	}

	private boolean doAttackHitSimple(Attack attack, L2Character target, double attackpercent, int sAtk)
	{
		int damage1 = 0;
		boolean shld1 = false;
		boolean crit1 = false;
		boolean miss1 = Formulas.getInstance().calcHitMiss(this, target);

		if(!miss1)
		{
			shld1 = Formulas.calcShldUse(this, target);
			crit1 = Formulas.calcCrit(getStat().getCriticalHit(target, null));
			damage1 = (int) Formulas.getInstance().calcPhysDam(this, target, null, shld1, crit1, false, attack.soulshot);

			if(attackpercent != 100)
			{
				damage1 = (int) (damage1 * attackpercent / 100);
			}
		}

		ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, attack.soulshot, shld1), sAtk);

		attack.addHit(target, damage1, miss1, crit1, shld1);

		return !miss1;
	}

	public void doCast(L2Skill skill)
	{
		beginCast(skill, false); 
	} 
	public void doSimultaneousCast(L2Skill skill) 
	{ 
		beginCast(skill, true); 
	} 
	
	private void beginCast(L2Skill skill, boolean simultaneously)
	{
		L2Character activeChar = this;
		if (skill == null) 
		{ 
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL); 
			return; 
		}
	
		if ((isSkillDisabled(skill.getId())))
		{
			if(activeChar instanceof L2PcInstance && !(skill.getId() == 2166))
				{
					sendPacket(new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE).addSkillName(skill.getId(), skill.getLevel()));
				}
				else if(activeChar instanceof L2PcInstance && (skill.getId() == 2166))
				{
					if (skill.getLevel() == 2)
					((L2PcInstance) activeChar).sendMessage("Greater CP Potion is not available at this time: being prepared for reuse.");
					else if (skill.getLevel() == 1)
					((L2PcInstance) activeChar).sendMessage("CP Potion is not available at this time: being prepared for reuse.");	
				}
				
				return;
			}

			if (!skill.isPotion())
			{
				// Check if the skill is a magic spell and if the L2Character is not muted
				if (skill.isMagic())
				{
					if (isMuted())
					{
						// Send a Server->Client packet ActionFailed to the L2PcInstance
						sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
				}
				else
				{
					// Check if the skill is physical and if the L2Character is not physical_muted
					if (isPsychicalMuted())
					{
						// Send a Server->Client packet ActionFailed to the L2PcInstance
						sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
				}
			}

			if(this instanceof L2PcInstance && ((L2PcInstance) this).isInOlympiadMode() && (skill.isHeroSkill() || skill.getSkillType() == L2SkillType.RESURRECT))
			{
				sendPacket(new SystemMessage(SystemMessageId.THIS_SKILL_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
			}
			
			if(skill.useSoulShot())
			{
				if(this instanceof L2PcInstance)
				{
					((L2PcInstance) this).rechargeAutoSoulShot(true, false, false);
				}
				else if(this instanceof L2Summon)
				{
					((L2Summon) this).getOwner().rechargeAutoSoulShot(true, false, true);
				}
			}
			else if(skill.useSpiritShot())
			{
				if(this instanceof L2PcInstance)
				{
					((L2PcInstance) this).rechargeAutoSoulShot(false, true, false);
				}
				else if(this instanceof L2Summon)
				{
					((L2Summon) this).getOwner().rechargeAutoSoulShot(false, true, true);
				}
			}
			
			if (skill.getItemConsume() > 0 && getInventory() != null) 
			{ 
				// Get the L2ItemInstance consumed by the spell 
				L2ItemInstance requiredItems = getInventory().getItemByItemId(skill.getItemConsumeId()); 
				
				// Check if the caster owns enough consumed Item to cast 
				if (requiredItems == null || requiredItems.getCount() < skill.getItemConsume()) 
				{ 
					// Checked: when a summon skill failed, server show required consume item count 
					if (skill.getSkillType() == L2SkillType.SUMMON) 
					{ 
						SystemMessage sm = new SystemMessage(SystemMessageId.SUMMONING_SERVITOR_COSTS_S2_S1); 
						sm.addItemName(skill.getItemConsumeId()); 
						sm.addNumber(skill.getItemConsume()); 
						sendPacket(sm); 
						return; 
					} 
					else 
					{ 
						// Send a System Message to the caster 
						sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS)); 
						return; 
					} 
				} 
			}			

		L2Object[] targets = skill.getTargetList(this);
		L2Character target = null;


		if(skill.getTargetType() == SkillTargetType.TARGET_AURA || skill.getTargetType() == SkillTargetType.TARGET_GROUND || skill.isPotion())
		{
			target = this;
		}
		else if(targets == null || targets.length == 0)
		{
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return;
		}
		else if((skill.getSkillType() == L2SkillType.BUFF || skill.getSkillType() == L2SkillType.HEAL || skill.getSkillType() == L2SkillType.COMBATPOINTHEAL || skill.getSkillType() == L2SkillType.COMBATPOINTPERCENTHEAL || skill.getSkillType() == L2SkillType.MANAHEAL || skill.getSkillType() == L2SkillType.REFLECT || skill.getSkillType() == L2SkillType.SEED || skill.getTargetType() == L2Skill.SkillTargetType.TARGET_SELF || skill.getTargetType() == L2Skill.SkillTargetType.TARGET_PET || skill.getTargetType() == L2Skill.SkillTargetType.TARGET_PARTY || skill.getTargetType() == L2Skill.SkillTargetType.TARGET_CLAN || skill.getTargetType() == L2Skill.SkillTargetType.TARGET_ALLY) && !skill.isPotion())
		{
			target = (L2Character) targets[0];
		}
		else
		{
			target = (L2Character) getTarget();
		}

		if(target == null)
		{
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return;
		}

		int magicId = skill.getId();
		int displayId = skill.getDisplayId();
		int level = skill.getLevel();

		if(level < 1)
		{
			level = 1;
		}

		int hitTime = skill.getHitTime();
		int coolTime = skill.getCoolTime();
		final boolean effectWhileCasting = skill.hasEffectWhileCasting();
		boolean forceBuff = skill.getSkillType() == L2SkillType.FORCE_BUFF && target instanceof L2PcInstance;

		if(!effectWhileCasting && !forceBuff && !skill.isStaticHitTime())
		{
			hitTime = Formulas.getInstance().calcMAtkSpd(this, skill, hitTime);

			if(coolTime > 0)
			{
				coolTime = Formulas.getInstance().calcMAtkSpd(this, skill, coolTime);
			}
		}

		L2ItemInstance weaponInst = getActiveWeaponInstance();

		if(weaponInst != null && skill.isMagic() && !forceBuff && skill.getTargetType() != SkillTargetType.TARGET_SELF && !skill.isStaticHitTime())
		{
			if(weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT || weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT)
			{
				hitTime = (int) (0.70 * hitTime);
				coolTime = (int) (0.70 * coolTime);

				if(skill.getSkillType() == L2SkillType.BUFF || skill.getSkillType() == L2SkillType.MANAHEAL || skill.getSkillType() == L2SkillType.RESURRECT || skill.getSkillType() == L2SkillType.RECALL || skill.getSkillType() == L2SkillType.DOT)
				{
					weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
				}
			}
		}

		weaponInst = null;
		
		// queue herbs and potions 
		if (isCastingSimultaneouslyNow() && simultaneously) 
		{ 
			ThreadPoolManager.getInstance().scheduleAi(new UsePotionTask(this, skill), 100 ); 
			return; 
		} 
	 	
		if (simultaneously)
			setIsCastingSimultaneouslyNow(true);
		else
			setIsCastingNow(true);
		//_castEndTime = 10 + GameTimeController.getGameTicks() + (coolTime + hitTime) / GameTimeController.MILLIS_IN_TICK;
		if (!simultaneously)
		{
			_castInterruptTime = -2 + GameTimeController.getGameTicks() + hitTime / GameTimeController.MILLIS_IN_TICK;
			setLastSkillCast(skill);
		}
		else
			setLastSimultaneousSkillCast(skill);

		int reuseDelay = skill.getReuseDelay();

		if(this instanceof L2PcInstance && Formulas.getInstance().calcSkillMastery(this))
		{
			reuseDelay = 0;
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE);
			sm.addSkillName(skill);
			sendPacket(sm);
		}
		else if(!skill.isStaticReuse() && !skill.isPotion())
		{
			if(skill.isMagic())
			{
				reuseDelay *= getStat().getMReuseRate(skill);
			}
			else
			{
				reuseDelay *= getStat().getPReuseRate(skill);
			}

			reuseDelay *= 333.0 / (skill.isMagic() ? getMAtkSpd() : getPAtkSpd());
		}
		
		if(effectWhileCasting)
		{
			callSkill(skill, targets);
		}

		broadcastPacket(new MagicSkillUser(this, target, displayId, level, hitTime, reuseDelay));

		if(this instanceof L2PcInstance && magicId != 1312)
		{
			sendPacket(new SystemMessage(SystemMessageId.USE_S1).addSkillName(magicId, skill.getLevel()));
			
			if (!effectWhileCasting && skill.getItemConsumeId() > 0)
			{
				if (!destroyItemByItemId("Consume", skill.getItemConsumeId(), skill.getItemConsume(), null, true))
				{
					getActingPlayer().sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
					abortCast();
					return;
				}
			}
		}

		if(reuseDelay > 30000)
		{
			addTimeStamp(skill.getId(), reuseDelay);
		}

		int initmpcons = getStat().getMpInitialConsume(skill);

		if(initmpcons > 0)
		{
			StatusUpdate su = new StatusUpdate(getObjectId());

			if(skill.isDance())
			{
				getStatus().reduceMp(calcStat(Stats.DANCE_MP_CONSUME_RATE, initmpcons, null, null));
			}
			else if(skill.isMagic())
			{
				getStatus().reduceMp(calcStat(Stats.MAGICAL_MP_CONSUME_RATE, initmpcons, null, null));
			}
			else
			{
				getStatus().reduceMp(calcStat(Stats.PHYSICAL_MP_CONSUME_RATE, initmpcons, null, null));
			}

			su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
			sendPacket(su);
			su = null;
		}

		if(reuseDelay > 10)
		{
			disableSkill(skill.getId(), reuseDelay);
		}

		if(forceBuff)
		{
			startForceBuff(target, skill);
		}

		if(skill.getFlyType() != null && (this instanceof L2PcInstance))
			ThreadPoolManager.getInstance().scheduleEffect(new FlyToLocationTask(this, target, skill), 50);

		if (hitTime > 210)
		{
			// Send a Server->Client packet SetupGauge with the color of the gauge and the casting time
			if (this instanceof L2PcInstance && !effectWhileCasting)
			{
				SetupGauge sg = new SetupGauge(SetupGauge.BLUE, hitTime);
				sendPacket(sg);
			}

			if (simultaneously)
			{
				if (_skillCast2 != null)
				{
					_skillCast2.cancel(true);
					_skillCast2 = null;
				}
				 // Create a task MagicUseTask to launch the MagicSkill at the end of the casting time (hitTime)
				// For client animation reasons (party buffs especially) 200 ms before!
				if (effectWhileCasting)
					_skillCast2 = ThreadPoolManager.getInstance().scheduleEffect(new MagicUseTask(targets, skill, coolTime, 2, simultaneously), hitTime);
				else
					_skillCast2 = ThreadPoolManager.getInstance().scheduleEffect(new MagicUseTask(targets, skill, coolTime, 1, simultaneously), hitTime-200); 
				
			}
			else
				{
				if (_skillCast != null)
				{
					_skillCast.cancel(true);
					_skillCast = null;
				}
				// Create a task MagicUseTask to launch the MagicSkill at the end of the casting time (hitTime)
				// For client animation reasons (party buffs especially) 200 ms before! 
				if (effectWhileCasting)
					_skillCast = ThreadPoolManager.getInstance().scheduleEffect(new MagicUseTask(targets, skill, coolTime, 2, simultaneously), hitTime);
				else
					_skillCast = ThreadPoolManager.getInstance().scheduleEffect(new MagicUseTask(targets, skill, coolTime, 1, simultaneously), hitTime-200);
				
				}
		}
		else
		{
			onMagicLaunchedTimer(targets, skill, coolTime, true, simultaneously);
		}
	}
	
	public void addTimeStamp(int s, int r)
	{
	}

	public void removeTimeStamp(int s)
	{
	}

	public void startForceBuff(L2Character target, L2Skill skill)
	{
		if(skill.getSkillType() != L2SkillType.FORCE_BUFF)
		{
			return;
		}

		if(_forceBuff == null)
		{
			_forceBuff = new ForceBuff(this, target, skill);
		}
	}

	public boolean doDie(L2Character killer)
	{
		synchronized (this)
		{
			if(isKilledAlready())
				return false;
			
			setIsKilledAlready(true);
		}

		setTarget(null);
		stopMove(null);
		getStatus().stopHpMpRegeneration();

		// Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
		broadcastStatusUpdate();
		
		// Notify L2Character AI
		getAI().notifyEvent(CtrlEvent.EVT_DEAD, null);
			
		if (getWorldRegion() != null)
			getWorldRegion().onDeath(this);
				
		if(this instanceof L2Playable && ((L2Playable) this).isPhoenixBlessed())
		{
			if(((L2Playable) this).isNoblesseBlessed())
			{
				((L2Playable) this).stopNoblesseBlessing(null);
			}

			if(((L2Playable) this).getCharmOfLuck())
			{
				((L2Playable) this).stopCharmOfLuck(null);
			}
		}
		else if(this instanceof L2Playable && ((L2Playable) this).isNoblesseBlessed())
		{
			((L2Playable) this).stopNoblesseBlessing(null);

			if(((L2Playable) this).getCharmOfLuck())
			{
				((L2Playable) this).stopCharmOfLuck(null);
			}
		}
		else
		{
			stopAllEffectsExceptThoseThatLastThroughDeath();
		}

		L2Character mostHated = null;
		if(this instanceof L2Attackable)
		{
			mostHated = ((L2Attackable)this)._mostHated;
		}
		
		if(mostHated!=null && isInsideRadius(mostHated, 200, false, false))
		{
			calculateRewards(mostHated);
		}
		else
		{
			calculateRewards(killer);
		}
		
		broadcastStatusUpdate();
		getAI().notifyEvent(CtrlEvent.EVT_DEAD, null);

		if(getWorldRegion() != null)
		{
			getWorldRegion().onDeath(this);
		}

		for(QuestState qs : getNotifyQuestOfDeath())
		{
			qs.getQuest().notifyDeath((killer == null ? this : killer), this, qs);
		}

		getNotifyQuestOfDeath().clear();

		getAttackByList().clear();

		if(this instanceof L2Playable && ((L2Playable) this).isPhoenixBlessed())
		{
			((L2PcInstance) this).reviveRequest(((L2PcInstance) this), null, false);
		}
		fireEvent(EventType.DIE.name, new Object[]
		{
			killer
		});
		
		updateEffectIcons();
		return true;
	}

	protected void calculateRewards(L2Character killer)
	{
	}


	public void doRevive()
	{
		if(!isTeleporting())
		{
			setIsPendingRevive(false);

			if(this instanceof L2Playable && ((L2Playable) this).isPhoenixBlessed())
			{
				((L2Playable) this).stopPhoenixBlessing(null);
				
				// Like L2OFF Soul of The Phoenix and Salvation restore all hp,cp,mp.
				_status.setCurrentCp(getMaxCp());
				_status.setCurrentHp(getMaxHp());
				_status.setCurrentMp(getMaxMp());
			}
			_status.setCurrentCp(getMaxCp() * Config.RESPAWN_RESTORE_CP);
			_status.setCurrentHp(getMaxHp() * Config.RESPAWN_RESTORE_HP);
		}
		// Start broadcast status
		broadcastPacket(new Revive(this));

		if(getWorldRegion() != null)
		{
			getWorldRegion().onRevive(this);
		}
		else
		{
			setIsPendingRevive(true);
		}
		fireEvent(EventType.REVIVE.name, (Object[]) null);
	}

	public void doRevive(double revivePower)
	{
		doRevive();
	}

	protected void useMagic(L2Skill skill)
	{
		if(skill == null || isDead())
		{
			return;
		}

		if(!skill.isPotion() && isAllSkillsDisabled())
		{
			return;
		}

		if(skill.isPassive() || skill.isChance())
		{
			return;
		}

		L2Object target = null;    
		
		switch(skill.getTargetType())
		{
			case TARGET_AURA:
			case TARGET_SELF:
				target = this;
				break;
			default:
				target = skill.getFirstOfTargetList(this);
				break;
		}

		getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, skill, target);		

		target = null;
	}

	public L2CharacterAI getAI()
	{
		if(_ai == null)
		{
			synchronized (this)
			{
				if(_ai == null)
				{
					_ai = new L2CharacterAI(new AIAccessor());
				}
			}
		}

		return _ai;
	}

	public void setAI(L2CharacterAI newAI)
	{
		L2CharacterAI oldAI = getAI();

		if(oldAI != null && oldAI != newAI && oldAI instanceof L2AttackableAI)
		{
			((L2AttackableAI) oldAI).stopAITask();
		}
		_ai = newAI;

		oldAI = null;
	}

	public boolean hasAI()
	{
		return _ai != null;
	}

	public boolean isRaid()
	{
		return false;
	}

	public boolean isNpc()
	{
		return false;
	}

	public final List<L2Character> getAttackByList()
	{
		if(_attackByList == null)
		{
			_attackByList = new FastList<L2Character>();
		}

		return _attackByList;
	}

	public final L2Skill getLastSimultaneousSkillCast()
	{
		return _lastSimultaneousSkillCast;
	} 
	
	public void setLastSimultaneousSkillCast(L2Skill skill)
	{
		_lastSimultaneousSkillCast = skill;
	} 
 	
	public final L2Skill getLastSkillCast()
	{
		return _lastSkillCast;
	}

	public void setLastSkillCast(L2Skill skill)
	{
		_lastSkillCast = skill;
	}
	
	public final boolean isNoRndWalk()
	{
		return _isNoRndWalk;
	}

	public final void setIsNoRndWalk(boolean value)
	{
		_isNoRndWalk = value;
	}

	public final boolean isAfraid()
	{
		return _isAfraid;
	}

	public final void setIsAfraid(boolean value)
	{
		_isAfraid = value;
	}

	public final boolean isAlikeDead()
	{
		return isFakeDeath() || !(getCurrentHp() > 0.01);
	}

	public final boolean isAllSkillsDisabled()
	{
		return _allSkillsDisabled || isImmobileUntilAttacked() || isStunned() || isSleeping() || isParalyzed();
	}

	public boolean isAttackingDisabled()
	{
		return isImmobileUntilAttacked() || isStunned() || isSleeping() || _attackEndTime > GameTimeController.getGameTicks() || isFakeDeath() || isParalyzed();
	}

	public final Calculator[] getCalculators()
	{
		return _calculators;
	}

	public final boolean isConfused()
	{
		return _isConfused;
	}

	public final void setIsConfused(boolean value)
	{
		_isConfused = value;
	}

	public final boolean isDead()
	{
		return !isFakeDeath() && (getCurrentHp() < 0.5);
	}

	public final boolean isFakeDeath()
	{
		return _isFakeDeath;
	}

	public final void setIsFakeDeath(boolean value)
	{
		_isFakeDeath = value;
	}

	public final boolean isFlying()
	{
		return _isFlying;
	}

	public final void setIsFlying(boolean mode)
	{
		_isFlying = mode;
	}

	public boolean isImobilised()
	{
		return _isImobilised;
	}

	public void setIsImobilised(boolean value)
	{
		_isImobilised = value;
	}

	public final boolean setIsDead(boolean value)
	{ 
		return !isFakeDeath() && (getCurrentHp() < 0.5);
	}
	
	public final boolean isKilledAlready()
	{
		return _isKilledAlready;
	}

	public final void setIsKilledAlready(boolean value)
	{
		_isKilledAlready = value;
	}

	public final boolean isMuted()
	{
		return _isMuted;
	}

	public final void setIsMuted(boolean value)
	{
		_isMuted = value;
	}

	public final boolean isPsychicalMuted()
	{
		return _isPsychicalMuted;
	}

	public final void setIsPsychicalMuted(boolean value)
	{
		_isPsychicalMuted = value;
	}

	public boolean isMovementDisabled()
	{
		return isStunned() || isRooted() || isSleeping() || isOverloaded() || isParalyzed() || isImobilised() || isFakeDeath() || isTeleporting();
	}

	public final boolean isOutOfControl()
	{
		return isConfused() || isAfraid() || isBlocked();
	}

	public final boolean isOverloaded()
	{
		return _isOverloaded;
	}

	public final void setIsOverloaded(boolean value)
	{
		_isOverloaded = value;
	}

	public final boolean isParalyzed()
	{
		return _isParalyzed;
	}

	public final void setIsParalyzed(boolean value)
	{
		if(_petrified)
			return;

		_isParalyzed = value;
	}

	public final boolean isPendingRevive()
	{
		return isDead() && _isPendingRevive;
	}

	public final void setIsPendingRevive(boolean value)
	{
		_isPendingRevive = value;
	}

	public L2Summon getPet()
	{
		return null;
	}

	public final boolean isRiding()
	{
		return _isRiding;
	}

	public final void setIsRiding(boolean mode)
	{
		_isRiding = mode;
	}

	public final boolean isRooted()
	{
		return _isRooted;
	}

	public final void setIsRooted(boolean value)
	{
		_isRooted = value;
	}

	public final boolean isRunning()
	{
		return _isRunning;
	}

	public final void setIsRunning(boolean value)
	{
		_isRunning = value;
		broadcastPacket(new ChangeMoveType(this));
	}

	public final void setRunning()
	{
		if(!isRunning())
		{
			setIsRunning(true);
		}
	}

	public final boolean isImmobileUntilAttacked()
	{
		return _isImmobileUntilAttacked;
	}

	public final void setIsImmobileUntilAttacked(boolean value)
	{
		_isImmobileUntilAttacked = value;
	}

	public final boolean isSleeping()
	{
		return _isSleeping;
	}

	public final void setIsSleeping(boolean value)
	{
		_isSleeping = value;
	}

	public final boolean isStunned()
	{
		return _isStunned;
	}

	public final void setIsStunned(boolean value)
	{
		_isStunned = value;
	}

	public final boolean isBetrayed()
	{
		return _isBetrayed;
	}

	public final void setIsBetrayed(boolean value)
	{
		_isBetrayed = value;
	}

	public final boolean isTeleporting()
	{
		return _isTeleporting;
	}

	public final void setIsTeleporting(boolean value)
	{
		_isTeleporting = value;
	}

	public void setIsInvul(boolean b)
	{
		if(_petrified)
			return;

		_isInvul = b;
	}

	public boolean isInvul()
	{
		return _isInvul || _isTeleporting;
	}

	public boolean isUndead()
	{
		return _template.isUndead;
	}

	@Override
	public CharKnownList getKnownList()
	{
		if(super.getKnownList() == null || !(super.getKnownList() instanceof CharKnownList))
		{
			setKnownList(new CharKnownList(this));
		}

		return (CharKnownList) super.getKnownList();
	}

	public CharStat getStat()
	{
		if(_stat == null)
		{
			_stat = new CharStat(this);
		}

		return _stat;
	}

	public final void setStat(CharStat value)
	{
		_stat = value;
	}

	public CharStatus getStatus()
	{
		if(_status == null)
		{
			_status = new CharStatus(this);
		}

		return _status;
	}

	public final void setStatus(CharStatus value)
	{
		_status = value;
	}

	public L2CharTemplate getTemplate()
	{
		return _template;
	}

	protected final void setTemplate(L2CharTemplate template)
	{
		_template = template;
	}

	public final String getTitle()
	{
		return _title;
	}

	public final void setTitle(String value)
	{
		if (value == null)
			_title = "";
		else
			_title = value.length() > 16 ? value.substring(0, 15) : value;
	}

	public final void setWalking()
	{
		if(isRunning())
		{
			setIsRunning(false);
		}
	}

	class EnableSkill implements Runnable
	{
		int _skillId;

		public EnableSkill(int skillId)
		{
			_skillId = skillId;
		}

		@Override
		public void run()
		{
			try
			{
				enableSkill(_skillId);
			}
			catch(Throwable e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}

	class HitTask implements Runnable
	{
		L2Character _hitTarget;
		int _damage;
		boolean _crit;
		boolean _miss;
		boolean _shld;
		boolean _soulshot;

		public HitTask(L2Character target, int damage, boolean crit, boolean miss, boolean soulshot, boolean shld)
		{
			_hitTarget = target;
			_damage = damage;
			_crit = crit;
			_shld = shld;
			_miss = miss;
			_soulshot = soulshot;
		}

		@Override
		public void run()
		{
			try
			{
				onHitTimer(_hitTarget, _damage, _crit, _miss, _soulshot, _shld);
			}
			catch(Throwable e)
			{
				_log.severe(e.toString());
			}
		}
	}

	class MagicUseTask implements Runnable
	{
		L2Object[] _targets;
		L2Skill _skill;
		int _coolTime;
		int _phase;

		boolean _simultaneously;
		
		public MagicUseTask(L2Object[] targets, L2Skill skill, int coolTime, int phase, boolean simultaneously)
		{
			_targets = targets;
			_skill = skill;
			_coolTime = coolTime;
			_phase = phase;
			_simultaneously = simultaneously;
		}

		@Override
		public void run()
		{
			try
			{
				switch(_phase)
				{
					case 1:
						onMagicLaunchedTimer(_targets, _skill, _coolTime, false, _simultaneously);
						break;
					case 2:
						onMagicHitTimer(_targets, _skill, _coolTime, false, _simultaneously);
						break;
					case 3:
						onMagicFinalizer(_targets, _skill, _simultaneously);
						break;
					default:
						break;
				}
			}
			catch(Throwable e)
			{
				_log.log(Level.SEVERE, "", e);
				if (_simultaneously)
					setIsCastingSimultaneouslyNow(false);
				else
					setIsCastingNow(false);
			}
		}
	}

	class QueuedMagicUseTask implements Runnable
	{
		L2PcInstance _currPlayer;
		L2Skill _queuedSkill;
		boolean _isCtrlPressed;
		boolean _isShiftPressed;

		public QueuedMagicUseTask(L2PcInstance currPlayer, L2Skill queuedSkill, boolean isCtrlPressed, boolean isShiftPressed)
		{
			_currPlayer = currPlayer;
			_queuedSkill = queuedSkill;
			_isCtrlPressed = isCtrlPressed;
			_isShiftPressed = isShiftPressed;
		}

		@Override
		public void run()
		{
			try
			{
				_currPlayer.useMagic(_queuedSkill, _isCtrlPressed, _isShiftPressed);
			}
			catch(Throwable e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}

	public class NotifyAITask implements Runnable
	{
		private final CtrlEvent _evt;

		NotifyAITask(CtrlEvent evt)
		{
			_evt = evt;
		}

		@Override
		public void run()
		{
			try
			{
				getAI().notifyEvent(_evt, null);
			}
			catch(Throwable t)
			{
				_log.log(Level.WARNING, "", t);
			}
		}
	}

	/** Task lauching the magic skill phases */
	class FlyToLocationTask implements Runnable
	{
		private final L2Object _tgt;
		private final L2Character _actor;
		private final L2Skill _skill;

		public FlyToLocationTask(L2Character actor, L2Object target, L2Skill skill)
		{
			_actor = actor;
			_tgt = target;
			_skill = skill;
		}

		@Override
		public void run()
		{
			try
			{
				FlyType _flyType;

				_flyType = FlyType.valueOf(_skill.getFlyType());

				broadcastPacket(new FlyToLocation(_actor,_tgt,_flyType));
				setXYZ(_tgt.getX(), _tgt.getY(), _tgt.getZ());
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "Failed executing FlyToLocationTask.", e);
			}
		}
	}

	class PvPFlag implements Runnable
	{
		public PvPFlag()
		{
		}

		@Override
		public void run()
		{
			try
			{
				if(System.currentTimeMillis() > getPvpFlagLasts())
				{
					stopPvPFlag();
					
				}
				else if(System.currentTimeMillis() > getPvpFlagLasts() - 5000)
				{
					updatePvPFlag(2);
				}
				else
				{
					updatePvPFlag(1);
				}
			}
			catch(Exception e)
			{
				_log.log(Level.WARNING, "error in pvp flag task:", e);
			}
		}
	}

	private int _AbnormalEffects;

	private CharEffectList _effects = new CharEffectList(this);

	public static final int ABNORMAL_EFFECT_BLEEDING = 0x000001;
	public static final int ABNORMAL_EFFECT_POISON = 0x000002;
	public static final int ABNORMAL_EFFECT_REDCIRCLE = 0x000004;
	public static final int ABNORMAL_EFFECT_ICE = 0x000008;
	public static final int ABNORMAL_EFFECT_WIND = 0x0000010;
	public static final int ABNORMAL_EFFECT_FEAR = 0x0000020;
	public static final int ABNORMAL_EFFECT_STUN = 0x000040;
	public static final int ABNORMAL_EFFECT_SLEEP = 0x000080;
	public static final int ABNORMAL_EFFECT_MUTED = 0x000100;
	public static final int ABNORMAL_EFFECT_ROOT = 0x000200;
	public static final int ABNORMAL_EFFECT_HOLD_1 = 0x000400;
	public static final int ABNORMAL_EFFECT_HOLD_2 = 0x000800;
	public static final int ABNORMAL_EFFECT_UNKNOWN_13 = 0x001000;
	public static final int ABNORMAL_EFFECT_BIG_HEAD = 0x002000;
	public static final int ABNORMAL_EFFECT_FLAME = 0x004000;
	public static final int ABNORMAL_EFFECT_UNKNOWN_16 = 0x008000;
	public static final int ABNORMAL_EFFECT_GROW = 0x010000;
	public static final int ABNORMAL_EFFECT_FLOATING_ROOT = 0x020000;
	public static final int ABNORMAL_EFFECT_DANCE_STUNNED = 0x040000;
	public static final int ABNORMAL_EFFECT_FIREROOT_STUN = 0x080000;
	public static final int ABNORMAL_EFFECT_STEALTH = 0x100000;
	public static final int ABNORMAL_EFFECT_IMPRISIONING_1 = 0x200000;
	public static final int ABNORMAL_EFFECT_IMPRISIONING_2 = 0x400000;
	public static final int ABNORMAL_EFFECT_MAGIC_CIRCLE = 0x800000;
	public static final int ABNORMAL_EFFECT_CONFUSED = 0x0020;
	public static final int ABNORMAL_EFFECT_AFRAID = 0x0010;

	public final void addEffect(L2Effect newEffect)
	{
		_effects.addEffect(newEffect);
		// Update active skills in progress (In Use and Not In Use because stacked) icones on client
		updateEffectIcons();
	}

	public final void removeEffect(L2Effect effect)
	{
		_effects.removeEffect(effect);
		updateEffectIcons();
	}

	public final void startAbnormalEffect(int mask)
	{
		_AbnormalEffects |= mask;
		updateAbnormalEffect();
	}

	public final void startImmobileUntilAttacked()
	{
		setIsImmobileUntilAttacked(true);
		abortAttack();
		abortCast();
		getAI().notifyEvent(CtrlEvent.EVT_SLEEPING);
		updateAbnormalEffect();
	}

	public final void startConfused()
	{
		setIsConfused(true);
		getAI().notifyEvent(CtrlEvent.EVT_CONFUSED);
		updateAbnormalEffect();
	}

	public final void startFakeDeath()
	{
		setIsFakeDeath(true);
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_FAKE_DEATH, null);
		broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_START_FAKEDEATH));
	}

	public final void startFear()
	{
		setIsAfraid(true);
		getAI().notifyEvent(CtrlEvent.EVT_AFFRAID);
		updateAbnormalEffect();
	}

	public final void startMuted()
	{
		setIsMuted(true);
		abortCast();
		getAI().notifyEvent(CtrlEvent.EVT_MUTED);
		updateAbnormalEffect();
	}

	public final void startPsychicalMuted()
	{
		setIsPsychicalMuted(true);
		getAI().notifyEvent(CtrlEvent.EVT_MUTED);
		updateAbnormalEffect();
	}

	public final void startRooted()
	{
		setIsRooted(true);
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_ROOTED, null);
		updateAbnormalEffect();
	}

	public final void startSleeping()
	{
		setIsSleeping(true);
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_SLEEPING, null);
		updateAbnormalEffect();
	}

	public final void startStunning()
	{
		setIsStunned(true);
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_STUNNED, null);
		updateAbnormalEffect();
	}

	public final void startBetray()
	{
		setIsBetrayed(true);
		getAI().notifyEvent(CtrlEvent.EVT_BETRAYED, null);
		updateAbnormalEffect();
	}

	public final void stopBetray()
	{
		stopEffects(L2EffectType.BETRAY);
		setIsBetrayed(false);
		updateAbnormalEffect();
	}

	public final void stopAbnormalEffect(int mask)
	{
		_AbnormalEffects &= ~mask;
		updateAbnormalEffect();
	}

	public final void stopAllEffects()
	{
		_effects.stopAllEffects();

		if(this instanceof L2PcInstance)
		{
			((L2PcInstance) this).updateAndBroadcastStatus(2);
		}
	}

	public final void stopAllEffectsExceptThoseThatLastThroughDeath() 
	{ 
		_effects.stopAllEffectsExceptThoseThatLastThroughDeath(); 
		if (this instanceof L2PcInstance) ((L2PcInstance)this).updateAndBroadcastStatus(2); 
	} 
	
	public final void stopImmobileUntilAttacked(L2Effect effect)
	{
		if(effect == null)
		{
			stopEffects(L2EffectType.IMMOBILEUNTILATTACKED);
		}
		else
		{
			removeEffect(effect);
			stopSkillEffects(effect.getSkill().getNegateId());
		}

		setIsImmobileUntilAttacked(false);
		getAI().notifyEvent(CtrlEvent.EVT_THINK);
		updateAbnormalEffect();
	}

	public final void stopConfused(L2Effect effect)
	{
		if(effect == null)
		{
			stopEffects(L2EffectType.CONFUSION);
		}
		else
		{
			removeEffect(effect);
		}

		setIsConfused(false);
		getAI().notifyEvent(CtrlEvent.EVT_THINK, null);
		updateAbnormalEffect();
	}

	public final void stopSkillEffects(int skillId)
	{
		_effects.stopSkillEffects(skillId);
	}

	public final void stopEffects(L2EffectType type)
	{
		_effects.stopEffects(type);
	}

	public final void stopFakeDeath(L2Effect effect)
	{
		if(effect == null)
		{
			stopEffects(L2EffectType.FAKE_DEATH);
		}
		else
		{
			removeEffect(effect);
		}

		setIsFakeDeath(false);

		if(this instanceof L2PcInstance)
		{
			((L2PcInstance) this).setRecentFakeDeath(true);
		}

		ChangeWaitType revive = new ChangeWaitType(this, ChangeWaitType.WT_STOP_FAKEDEATH);
		broadcastPacket(revive);
		broadcastPacket(new Revive(this));
		getAI().notifyEvent(CtrlEvent.EVT_THINK, null);

		revive = null;
	}

	public final void stopFear(L2Effect effect)
	{
		if(effect == null)
		{
			stopEffects(L2EffectType.FEAR);
		}
		else
		{
			removeEffect(effect);
		}

		setIsAfraid(false);
		updateAbnormalEffect();
	}

	public final void stopMuted(L2Effect effect)
	{
		if(effect == null)
		{
			stopEffects(L2EffectType.MUTE);
		}
		else
		{
			removeEffect(effect);
		}

		setIsMuted(false);
		updateAbnormalEffect();
	}

	public final void stopPsychicalMuted(L2Effect effect)
	{
		if(effect == null)
		{
			stopEffects(L2EffectType.PSYCHICAL_MUTE);
		}
		else
		{
			removeEffect(effect);
		}

		setIsPsychicalMuted(false);
		updateAbnormalEffect();
	}

	public final void stopRooting(L2Effect effect)
	{
		if(effect == null)
		{
			stopEffects(L2EffectType.ROOT);
		}
		else
		{
			removeEffect(effect);
		}

		setIsRooted(false);
		getAI().notifyEvent(CtrlEvent.EVT_THINK, null);
		updateAbnormalEffect();
	}

	public final void stopSleeping(L2Effect effect)
	{
		if(effect == null)
		{
			stopEffects(L2EffectType.SLEEP);
		}
		else
		{
			removeEffect(effect);
		}

		setIsSleeping(false);
		getAI().notifyEvent(CtrlEvent.EVT_THINK, null);
		updateAbnormalEffect();
	}

	public final void stopStunning(L2Effect effect)
	{
		if(effect == null)
		{
			stopEffects(L2EffectType.STUN);
		}
		else
		{
			removeEffect(effect);
		}

		setIsStunned(false);
		getAI().notifyEvent(CtrlEvent.EVT_THINK, null);
		updateAbnormalEffect();
	}

	public abstract void updateAbnormalEffect();

	public final void updateEffectIcons()
	{
		updateEffectIcons(false);
	}

	public final void updateEffectIcons(boolean partyOnly)
	{
		L2PcInstance player = null;

		if(this instanceof L2PcInstance)
		{
			player = (L2PcInstance) this;
		}

		L2Summon summon = null;
		if(this instanceof L2Summon)
		{
			summon = (L2Summon) this;
			player = summon.getOwner();
			summon.getOwner().sendPacket(new PetInfo(summon));
		}

		MagicEffectIcons mi = null;
		if(!partyOnly)
		{
			mi = new MagicEffectIcons();
		}

		PartySpelled ps = null;
		if(summon != null)
		{
			ps = new PartySpelled(summon);
		}
		else if(player != null && player.isInParty())
		{
			ps = new PartySpelled(player);
		}

		ExOlympiadSpelledInfo os = null;
		if(player != null && player.isInOlympiadMode())
		{
			os = new ExOlympiadSpelledInfo(player);
		}

		if(mi == null && ps == null && os == null)
		{
			return;
		}

		L2Effect[] effects = getAllEffects();
		if(effects != null && effects.length > 0)
		{
			for(L2Effect effect2 : effects)
			{
				L2Effect effect = effect2;

				if(effect == null)
				{
					continue;
				}

				if(effect.getEffectType() == L2EffectType.CHARGE && player != null)
				{
					continue;
				}

				if(effect.getInUse())
				{
					if(mi != null)
					{
						effect.addIcon(mi);
					}
					if(ps != null)
					{
						effect.addPartySpelledIcon(ps);
					}
					if(os != null)
					{
						effect.addOlympiadSpelledIcon(os);
					}
				}

				effect = null;
			}
		}

		effects = null;

		if(mi != null)
		{
			sendPacket(mi);
		}

		if(ps != null && player != null)
		{
			if(player.isInParty() && summon == null)
			{
				player.getParty().broadcastToPartyMembers(player, ps);
			}
			else
			{
				player.sendPacket(ps);
			}
		}

		if(os != null)
		{
			if(Olympiad.getInstance().getSpectators(player.getOlympiadGameId()) != null)
			{
				for(L2PcInstance spectator : Olympiad.getInstance().getSpectators(player.getOlympiadGameId()))
				{
					if(spectator == null)
					{
						continue;
					}

					spectator.sendPacket(os);
				}
			}
		}

		player = null;
		mi = null;
		os = null;
		ps = null;
		summon = null;
	}

	public int getAbnormalEffect()
	{
		int ae = _AbnormalEffects;

		if(isStunned())
		{
			ae |= ABNORMAL_EFFECT_STUN;
		}

		if(isRooted())
		{
			ae |= ABNORMAL_EFFECT_ROOT;
		}

		if(isSleeping())
		{
			ae |= ABNORMAL_EFFECT_SLEEP;
		}

		if(isConfused())
		{
			ae |= ABNORMAL_EFFECT_CONFUSED;
		}

		if(isMuted())
		{
			ae |= ABNORMAL_EFFECT_MUTED;
		}

		if(isAfraid())
		{
			ae |= ABNORMAL_EFFECT_AFRAID;
		}

		if(isPsychicalMuted())
		{
			ae |= ABNORMAL_EFFECT_MUTED;
		}

		return ae;
	}

	public final L2Effect[] getAllEffects()
	{
		return _effects.getAllEffects();
	}

	public final L2Effect getFirstEffect(int index)
	{
		return _effects.getFirstEffect(index);
	}

	public final L2Effect getFirstEffect(L2Skill skill)
	{
		return _effects.getFirstEffect(skill);
	}

	public final L2Effect getFirstEffect(L2EffectType tp)
	{
		return _effects.getFirstEffect(tp);
	}

	public EffectCharge getChargeEffect()
	{
		return _effects.getChargeEffect();
	}

	public class AIAccessor
	{
		public AIAccessor()
		{
		}

		public L2Character getActor()
		{
			return L2Character.this;
		}

		public void moveTo(int x, int y, int z, int offset)
		{
			moveToLocation(x, y, z, offset);
		}

		public void moveTo(int x, int y, int z)
		{
			moveToLocation(x, y, z, 0);
		}

		public void stopMove(L2CharPosition pos)
		{
			L2Character.this.stopMove(pos);
		}

		public void doAttack(L2Character target)
		{
			L2Character.this.doAttack(target);
		}

		public void doCast(L2Skill skill)
		{
			L2Character.this.doCast(skill);
		}

		public NotifyAITask newNotifyTask(CtrlEvent evt)
		{
			return new NotifyAITask(evt);
		}

		public void detachAI()
		{
			_ai = null;
		}
	}

	public static class MoveData
	{
		public int _moveStartTime;
		public int _moveTimestamp;
		public int _xDestination;
		public int _yDestination;
		public int _zDestination;
		public double _xAccurate;
		public double _yAccurate;
		public double _zAccurate;
		public int _heading;
		public boolean disregardingGeodata;
		public int onGeodataPathIndex;
		public Node[] geoPath;
		public int geoPathAccurateTx;
		public int geoPathAccurateTy;
		public int geoPathGtx;
		public int geoPathGty;
	}

	protected List<Integer> _disabledSkills;
	private boolean _allSkillsDisabled;
	protected MoveData _move;
	private int _heading;
	private L2Object _target;
	private int _castInterruptTime;
	private int _attackEndTime;
	private int _attacking;
	private int _disableBowAttackEndTime;
	private static final Calculator[] NPC_STD_CALCULATOR;
	static
	{
		NPC_STD_CALCULATOR = Formulas.getInstance().getStdNPCCalculators();
	}

	protected L2CharacterAI _ai;
	protected Future<?> _skillCast;
	protected Future<?> _skillCast2;
	private int _clientX;
	private int _clientY;
	private int _clientZ;
	private int _clientHeading;
	private List<QuestState> _NotifyQuestOfDeathList = new FastList<QuestState>();

	public void addNotifyQuestOfDeath(QuestState qs)
	{
		if(qs == null || _NotifyQuestOfDeathList.contains(qs))
		{
			return;
		}

		_NotifyQuestOfDeathList.add(qs);
	}

	public final List<QuestState> getNotifyQuestOfDeath()
	{
		if(_NotifyQuestOfDeathList == null)
		{
			_NotifyQuestOfDeathList = new FastList<QuestState>();
		}

		return _NotifyQuestOfDeathList;
	}

	public final synchronized void addStatFunc(Func f)
	{
		if(f == null)
		{
			return;
		}

		if(_calculators == NPC_STD_CALCULATOR)
		{
			_calculators = new Calculator[Stats.NUM_STATS];

			for(int i = 0; i < Stats.NUM_STATS; i++)
			{
				if(NPC_STD_CALCULATOR[i] != null)
				{
					_calculators[i] = new Calculator(NPC_STD_CALCULATOR[i]);
				}
			}
		}

		int stat = f.stat.ordinal();

		if(_calculators[stat] == null)
		{
			_calculators[stat] = new Calculator();
		}

		_calculators[stat].addFunc(f);

	}

	public final synchronized void addStatFuncs(Func[] funcs)
	{

		FastList<Stats> modifiedStats = new FastList<Stats>();

		for(Func f : funcs)
		{
			modifiedStats.add(f.stat);
			addStatFunc(f);
		}

		broadcastModifiedStats(modifiedStats);

		modifiedStats = null;
	}

	public final synchronized void removeStatFunc(Func f)
	{
		if(f == null)
		{
			return;
		}

		int stat = f.stat.ordinal();

		if(_calculators[stat] == null)
		{
			return;
		}

		_calculators[stat].removeFunc(f);

		if(_calculators[stat].size() == 0)
		{
			_calculators[stat] = null;
		}

		if(this instanceof L2Npc)
		{
			int i = 0;

			for(; i < Stats.NUM_STATS; i++)
			{
				if(!Calculator.equalsCals(_calculators[i], NPC_STD_CALCULATOR[i]))
				{
					break;
				}
			}

			if(i >= Stats.NUM_STATS)
			{
				_calculators = NPC_STD_CALCULATOR;
			}
		}
	}

	public final synchronized void removeStatFuncs(Func[] funcs)
	{

		FastList<Stats> modifiedStats = new FastList<Stats>();

		for(Func f : funcs)
		{
			modifiedStats.add(f.stat);
			removeStatFunc(f);
		}

		broadcastModifiedStats(modifiedStats);

		modifiedStats = null;
	}

	public final void removeStatsOwner(Object owner)
	{
		FastList<Stats> modifiedStats = null;

		int i = 0;
		synchronized (_calculators)
		{
			for(Calculator calc : _calculators)
			{
				if(calc != null)
				{
					if(modifiedStats != null)
					{
						modifiedStats.addAll(calc.removeOwner(owner));
					}
					else
					{
						modifiedStats = calc.removeOwner(owner);
					}

					if(calc.size() == 0)
					{
						_calculators[i] = null;
					}
				}
				i++;
			}

			if(this instanceof L2Npc)
			{
				i = 0;
				for(; i < Stats.NUM_STATS; i++)
				{
					if(!Calculator.equalsCals(_calculators[i], NPC_STD_CALCULATOR[i]))
					{
						break;
					}
				}

				if(i >= Stats.NUM_STATS)
				{
					_calculators = NPC_STD_CALCULATOR;
				}
			}

			if(owner instanceof L2Effect && !((L2Effect) owner).preventExitUpdate)
			{
				broadcastModifiedStats(modifiedStats);
			}
		}

		modifiedStats = null;
	}

	private void broadcastModifiedStats(FastList<Stats> stats)
	{
		if(stats == null || stats.isEmpty())
		{
			return;
		}

		boolean broadcastFull = false;
		boolean otherStats = false;
		StatusUpdate su = null;

		for(Stats stat : stats)
		{
			if(stat == Stats.POWER_ATTACK_SPEED)
			{
				if(su == null)
				{
					su = new StatusUpdate(getObjectId());
				}

				su.addAttribute(StatusUpdate.ATK_SPD, getPAtkSpd());
			}
			else if(stat == Stats.MAGIC_ATTACK_SPEED)
			{
				if(su == null)
				{
					su = new StatusUpdate(getObjectId());
				}

				su.addAttribute(StatusUpdate.CAST_SPD, getMAtkSpd());
			}
			else if(stat == Stats.MAX_CP)
			{
				if(this instanceof L2PcInstance)
				{
					if(su == null)
					{
						su = new StatusUpdate(getObjectId());
					}

					su.addAttribute(StatusUpdate.MAX_CP, getMaxCp());
				}
			}
			else if(stat == Stats.RUN_SPEED)
			{
				broadcastFull = true;
			}
			else
			{
				otherStats = true;
			}
		}

		if(this instanceof L2PcInstance)
		{
			if(broadcastFull)
			{
				((L2PcInstance) this).updateAndBroadcastStatus(2);
			}
			else
			{
				if(otherStats)
				{
					((L2PcInstance) this).updateAndBroadcastStatus(1);
					if(su != null)
					{
						for(L2PcInstance player : getKnownList().getKnownPlayers().values())
						{
							try
							{
								player.sendPacket(su);
							}
							catch(NullPointerException e)
							{
							}
						}
					}
				}
				else if(su != null)
				{
					broadcastPacket(su);
				}
			}
		}
		else if(this instanceof L2Npc)
		{
			if(broadcastFull)
			{
				for(L2PcInstance player : getKnownList().getKnownPlayers().values())
				{
					if(player != null)
					{
						player.sendPacket(new NpcInfo((L2Npc) this, player));
					}
				}
			}
			else if(su != null)
			{
				broadcastPacket(su);
			}
		}
		else if(this instanceof L2Summon)
		{
			if(broadcastFull)
			{
				for(L2PcInstance player : getKnownList().getKnownPlayers().values())
					if(player != null)
					{
						player.sendPacket(new NpcInfo((L2Summon) this, player));
					}
			}
			else if(su != null)
			{
				broadcastPacket(su);
			}
		}
		else if(su != null)
		{
			broadcastPacket(su);
		}

		su = null;
	}

	public final int getHeading()
	{
		return _heading;
	}

	public final void setHeading(int heading)
	{
		if(heading > 65535)
		{
			_heading = 65335;
			return;
		}

		_heading = heading;
	}

	public final int getClientX()
	{
		return _clientX;
	}

	public final int getClientY()
	{
		return _clientY;
	}

	public final int getClientZ()
	{
		return _clientZ;
	}

	public final int getClientHeading()
	{
		return _clientHeading;
	}

	public final void setClientX(int val)
	{
		_clientX = val;
	}

	public final void setClientY(int val)
	{
		_clientY = val;
	}

	public final void setClientZ(int val)
	{
		_clientZ = val;
	}

	public final void setClientHeading(int val)
	{
		_clientHeading = val;
	}

	public final int getXdestination()
	{
		MoveData m = _move;

		if(m != null)
		{
			return m._xDestination;
		}

		return getX();
	}

	public final int getYdestination()
	{
		MoveData m = _move;

		if(m != null)
		{
			return m._yDestination;
		}

		return getY();
	}

	public final int getZdestination()
	{
		MoveData m = _move;

		if(m != null)
		{
			return m._zDestination;
		}

		return getZ();
	}

	public final boolean isInCombat()
	{
		return getAI().getAttackTarget() != null;
	}

	public final boolean isMoving()
	{
		return _move != null;
	}

	public final boolean isOnGeodataPath()
	{
		final MoveData move = _move;

		if(move == null)
		{
			return false;
		}

		try
		{
			if(move.onGeodataPathIndex == -1)
			{
				return false;
			}

			if(move.onGeodataPathIndex == _move.geoPath.length - 1)
			{
				return false;
			}
		}
		catch(NullPointerException e)
		{
			return false;
		}

		return true;
	}

	public final boolean isCastingNow()
	{
		return _isCastingNow;
	}
	
	public void setIsCastingNow(boolean value)
	{
		_isCastingNow = value;
	}

	public final boolean isCastingSimultaneouslyNow() 
	{
		return _isCastingSimultaneouslyNow;
	}
	
	public void setIsCastingSimultaneouslyNow(boolean value)
	{
		_isCastingSimultaneouslyNow = value;
	}
 	
	public final boolean canAbortCast()
	{
		return _castInterruptTime > GameTimeController.getGameTicks();
	}

	public final boolean isAttackingNow()
	{
		return _attackEndTime > GameTimeController.getGameTicks();
	}

	public final boolean isAttackAborted()
	{
		return _attacking <= 0;
	}

	public final void abortAttack()
	{
		if(isAttackingNow())
		{
			_attacking = 0;
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}

	public final int getAttackingBodyPart()
	{
		return _attacking;
	}

	public final void abortCast()
	{
		if(isCastingNow() || isCastingSimultaneouslyNow())
		{
			_castInterruptTime = 0;

			if(_skillCast != null)
			{
				_skillCast.cancel(true);
				_skillCast = null;
			}
			if (_skillCast2 != null) 
			{ 
				try  
				{ 
					_skillCast2.cancel(true); 
				} 
				catch (NullPointerException e) {} 
				
				_skillCast2 = null; 
			} 
			
			if(getForceBuff() != null)
			{
				getForceBuff().onCastAbort();
			}

			L2Effect mog = getFirstEffect(L2EffectType.SIGNET_GROUND);
		 	if(mog != null)
			{
				_skillCast = null;
				mog.exit();
			}

		 	if (_allSkillsDisabled) enableAllSkills(); // this remains for forced skill use, e.g. scroll of escape 
		 	setIsCastingNow(false); 
		 	setIsCastingSimultaneouslyNow(false); 
		 	// safeguard for cannot be interrupt any more 
		 	_castInterruptTime = 0;
			if(this instanceof L2PcInstance)
			{
				getAI().notifyEvent(CtrlEvent.EVT_FINISH_CASTING);
			}

			broadcastPacket(new MagicSkillCanceld(getObjectId()));
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}

	public boolean updatePosition(int gameTicks)
	{
		MoveData m = _move;

		if(m == null)
		{
			return true;
		}

		if(!isVisible())
		{
			_move = null;
			return true;
		}

		if(m._moveTimestamp == 0)
		{
			m._moveTimestamp = m._moveStartTime;
			m._xAccurate = getX();
			m._yAccurate = getY();
		}

		if(m._moveTimestamp == gameTicks)
		{
			return false;
		}

		int xPrev = getX();
		int yPrev = getY();
		int zPrev = getZ();

		double dx, dy, dz, distFraction;
		if(Config.COORD_SYNCHRONIZE == 1)
		{
			dx = m._xDestination - xPrev;
			dy = m._yDestination - yPrev;
		}
		else
		{
			dx = m._xDestination - m._xAccurate;
			dy = m._yDestination - m._yAccurate;
		}
		if(Config.GEODATA > 0 && Config.COORD_SYNCHRONIZE == 2 && !isFlying() && !isInsideZone(L2Character.ZONE_WATER) && !m.disregardingGeodata && GameTimeController.getGameTicks() % 10 == 0 && !(this instanceof L2BoatInstance))
		{
			short geoHeight = GeoData.getInstance().getSpawnHeight(xPrev, yPrev, zPrev - 30, zPrev + 30, null);
			dz = m._zDestination - geoHeight;
			if(this instanceof L2PcInstance && Math.abs(((L2PcInstance) this).getClientZ() - geoHeight) > 200 && Math.abs(((L2PcInstance) this).getClientZ() - geoHeight) < 1500)
			{
				dz = m._zDestination - zPrev;
			}
			else if(isInCombat() && Math.abs(dz) > 200 && dx * dx + dy * dy < 40000)
			{
				dz = m._zDestination - zPrev;
			}
			else
			{
				zPrev = geoHeight;
			}
		}
		else
		{
			dz = m._zDestination - zPrev;
		}

		float speed;
		if(this instanceof L2BoatInstance)
		{
			speed = ((L2BoatInstance) this).boatSpeed;
		}
		else
		{
			speed = getStat().getMoveSpeed();
		}

		double distPassed = speed * (gameTicks - m._moveTimestamp) / GameTimeController.TICKS_PER_SECOND;
		if(dx * dx + dy * dy < 10000 && dz * dz > 2500)
		{
			distFraction = distPassed / Math.sqrt(dx * dx + dy * dy);
		}
		else
		{
			distFraction = distPassed / Math.sqrt(dx * dx + dy * dy + dz * dz);
		}

		if(distFraction > 1)
		{
			super.getPosition().setXYZ(m._xDestination, m._yDestination, m._zDestination);
			if(this instanceof L2BoatInstance)
			{
				((L2BoatInstance) this).updatePeopleInTheBoat(m._xDestination, m._yDestination, m._zDestination);
			}
			else
			{
				revalidateZone();
			}
		}
		else
		{
			m._xAccurate += dx * distFraction;
			m._yAccurate += dy * distFraction;

			super.getPosition().setXYZ((int) m._xAccurate, (int) m._yAccurate, zPrev + (int) (dz * distFraction + 0.5));
			if(this instanceof L2BoatInstance)
			{
				((L2BoatInstance) this).updatePeopleInTheBoat((int) m._xAccurate, (int) m._yAccurate, zPrev + (int) (dz * distFraction + 0.5));
			}
			else
			{
				revalidateZone();
			}
		}

		m._moveTimestamp = gameTicks;

		return distFraction > 1;
	}

	public void revalidateZone()
	{
		if(getWorldRegion() == null)
		{
			return;
		}

		getWorldRegion().revalidateZones(this);
	}

	public void stopMove(L2CharPosition pos)
	{
		stopMove(pos, true);
	}

	public void stopMove(L2CharPosition pos, boolean updateKnownObjects)
	{
		_move = null;

		if(pos != null)
		{
			getPosition().setXYZ(pos.x, pos.y, GeoData.getInstance().getHeight(pos.x, pos.y, pos.z));
			setHeading(pos.heading);

			if(this instanceof L2PcInstance)
			{
				((L2PcInstance) this).revalidateZone(true);

				if(((L2PcInstance)this).isInBoat())
				{
					broadcastPacket(new ValidateLocationInVehicle(this));
				}
			}
		}

		broadcastPacket(new StopMove(this));

		if(updateKnownObjects)
		{
			ThreadPoolManager.getInstance().executeTask(new KnownListAsynchronousUpdateTask(this));
		}
	}

	public boolean isShowSummonAnimation()
	{
		return _showSummonAnimation;
	}

	public void setShowSummonAnimation(boolean showSummonAnimation)
	{
		_showSummonAnimation = showSummonAnimation;
	}

	public void setTarget(L2Object object)
	{
		if(object != null && !object.isVisible())
		{
			object = null;
		}

		if(object != null && object != _target)
		{
			getKnownList().addKnownObject(object);
			object.getKnownList().addKnownObject(this);
		}

		if(object == null)
		{
			if(_target != null)
			{
				broadcastPacket(new TargetUnselected(this));
			}
		}

		_target = object;
	}

	public final int getTargetId()
	{
		if(_target != null)
		{
			return _target.getObjectId();
		}

		return -1;
	}

	public final L2Object getTarget()
	{
		return _target;
	}

	protected void moveToLocation(int x, int y, int z, int offset)
	{
		if(this instanceof L2PcInstance)
		{
            ((L2PcInstance)this).setSitdownTask(false);
        }
		
		float speed = getStat().getMoveSpeed();

		if(speed <= 0 || isMovementDisabled())
		{
			return;
		}

		final int curX = super.getX();
		final int curY = super.getY();
		final int curZ = super.getZ();
		double dx = x - curX;
		double dy = y - curY;
		double dz = z - curZ;
		double distance = Math.sqrt(dx * dx + dy * dy);

		if(Config.GEODATA > 0 && isInsideZone(ZONE_WATER) && distance > 700)
		{
			double divider = 700 / distance;
			x = curX + (int) (divider * dx);
			y = curY + (int) (divider * dy);
			z = curZ + (int) (divider * dz);
			dx = x - curX;
			dy = y - curY;
			dz = z - curZ;
			distance = Math.sqrt(dx * dx + dy * dy);
		}

		double cos;
		double sin;

		if(offset > 0 || distance < 1)
		{
			offset -= Math.abs(dz);

			if(offset < 5)
			{
				offset = 5;
			}

			if(distance < 1 || distance - offset <= 0)
			{
				sin = 0;
				cos = 1;
				distance = 0;
				x = curX;
				y = curY;

				getAI().notifyEvent(CtrlEvent.EVT_ARRIVED, null);

				return;
			}
			sin = dy / distance;
			cos = dx / distance;

			distance -= offset - 5;

			x = curX + (int) (distance * cos);
			y = curY + (int) (distance * sin);

		}
		else
		{
			sin = dy / distance;
			cos = dx / distance;
		}

		MoveData m = new MoveData();

		m.onGeodataPathIndex = -1;
		m.disregardingGeodata = false;

		if(Config.GEODATA > 0 && !isFlying() && (!isInsideZone(ZONE_WATER) || isInsideZone(ZONE_SIEGE)) && !(this instanceof L2NpcWalkerInstance))
		{
			double originalDistance = distance;
			int originalX = x;
			int originalY = y;
			int originalZ = z;
			int gtx = originalX - L2World.MAP_MIN_X >> 4;
			int gty = originalY - L2World.MAP_MIN_Y >> 4;

			if(Config.GEODATA == 2 && !(this instanceof L2Attackable && ((L2Attackable) this).isReturningToSpawnPoint()) || this instanceof L2PcInstance || this instanceof L2Summon && !(getAI().getIntention() == AI_INTENTION_FOLLOW) || this instanceof L2RiftInvaderInstance || isAfraid())
			{
				if(isOnGeodataPath())
				{
					try
					{
						if(gtx == _move.geoPathGtx && gty == _move.geoPathGty)
						{
							return;
						}
						else
						{
							_move.onGeodataPathIndex = -1;
						}
					}
					catch(NullPointerException e)
					{
					}
				}

				if(curX < L2World.MAP_MIN_X || curX > L2World.MAP_MAX_X || curY < L2World.MAP_MIN_Y || curY > L2World.MAP_MAX_Y)
				{
					_log.warning("Character " + getName() + " outside world area, in coordinates x:" + curX + " y:" + curY);
					getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
					if(this instanceof L2PcInstance)
					{
						((L2PcInstance) this).deleteMe();
					}
					else
					{
						onDecay();
					}

					return;
				}
				Location destiny = GeoData.getInstance().moveCheck(curX, curY, curZ, x, y, z);
				x = destiny.getX();
				y = destiny.getY();
				z = destiny.getZ();
				distance = Math.sqrt((x - curX) * (x - curX) + (y - curY) * (y - curY));
			}

			// Pathfinding checks. Only when geodata setting is 2, the LoS check gives shorter result
			// than the original movement was and the LoS gives a shorter distance than 2000
			// This way of detecting need for pathfinding could be changed.
			if( ((this instanceof L2PcInstance) && Config.ALLOW_PLAYERS_PATHNODE || !(this instanceof L2PcInstance)) && Config.GEODATA == 2 && originalDistance - distance > 30 && distance < 2000 && !isAfraid())
			{
				// Path calculation
				// Overrides previous movement check
				if(this instanceof L2Playable || isInCombat() || this instanceof L2MinionInstance)
				{
					//int gx = (curX - L2World.MAP_MIN_X) >> 4;
					//int gy = (curY - L2World.MAP_MIN_Y) >> 4;

					m.geoPath = PathFinding.getInstance().findPath(curX, curY, curZ, originalX, originalY, originalZ);
					if(m.geoPath == null || m.geoPath.length < 2) // No path found
					{
						// Even though there's no path found (remember geonodes aren't perfect), 
						// the mob is attacking and right now we set it so that the mob will go
						// after target anyway, is dz is small enough. Summons will follow their masters no matter what.
						if(Config.ALLOW_PLAYERS_PATHNODE && (this instanceof L2PcInstance)
								|| (!(this instanceof L2PcInstance) && Math.abs(z - curZ) > 140) 
								|| (this instanceof L2Summon && !((L2Summon) this).getFollowStatus()))
						{
							getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
							return;
						}
						else
						{
							m.disregardingGeodata = true;
							x = originalX;
							y = originalY;
							z = originalZ;
							distance = originalDistance;
						}
					}
					else
					{
						m.onGeodataPathIndex = 0; // on first segment
						m.geoPathGtx = gtx;
						m.geoPathGty = gty;
						m.geoPathAccurateTx = originalX;
						m.geoPathAccurateTy = originalY;

						x = m.geoPath[m.onGeodataPathIndex].getX();
						y = m.geoPath[m.onGeodataPathIndex].getY();
						z = m.geoPath[m.onGeodataPathIndex].getZ();

						// check for doors in the route
						if(Door.getInstance().checkIfDoorsBetween(curX, curY, curZ, x, y, z))
						{
							m.geoPath = null;
							getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
							return;
						}

						for(int i = 0; i < m.geoPath.length - 1; i++)
						{
							if(Door.getInstance().checkIfDoorsBetween(m.geoPath[i], m.geoPath[i+1]))
							{
								m.geoPath = null;
								getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
								return;
							}
						}

						dx = x - curX;
						dy = y - curY;
						distance = Math.sqrt(dx * dx + dy * dy);
						sin = dy / distance;
						cos = dx / distance;
					}
				}
			}
			// If no distance to go through, the movement is canceled
			if(((this instanceof L2PcInstance) && Config.ALLOW_PLAYERS_PATHNODE || !(this instanceof L2PcInstance)) && distance < 1 && (Config.GEODATA == 2 || this instanceof L2Playable || this instanceof L2RiftInvaderInstance || isAfraid()))
			{
				if(this instanceof L2Summon)
				{
					((L2Summon) this).setFollowStatus(false);
				}

				//getAI().notifyEvent(CtrlEvent.EVT_ARRIVED, null);
				getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);

				return;
			}
		}

		// Caclulate the Nb of ticks between the current position and the destination
		// One tick added for rounding reasons
		int ticksToMove = 1 + (int) (GameTimeController.TICKS_PER_SECOND * distance / speed);

		// Calculate and set the heading of the L2Character
		setHeading((int) (Math.atan2(-sin, -cos) * 10430.37835) + 32768);

		m._xDestination = x;
		m._yDestination = y;
		m._zDestination = z; // this is what was requested from client
		m._heading = 0;

		m._moveStartTime = GameTimeController.getGameTicks();

		// Set the L2Character _move object to MoveData object
		_move = m;

		// Add the L2Character to movingObjects of the GameTimeController
		// The GameTimeController manage objects movement
		GameTimeController.getInstance().registerMovingObject(this);

		// Create a task to notify the AI that L2Character arrives at a check point of the movement
		if(ticksToMove * GameTimeController.MILLIS_IN_TICK > 3000)
		{
			ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(CtrlEvent.EVT_ARRIVED_REVALIDATE), 2000);
		}

		// the CtrlEvent.EVT_ARRIVED will be sent when the character will actually arrive
		// to destination by GameTimeController

		m = null;
	}

	public boolean moveToNextRoutePoint()
	{
		if(!isOnGeodataPath())
		{
			_move = null;
			return false;
		}

		float speed = getStat().getMoveSpeed();

		if(speed <= 0 || isMovementDisabled())
		{
			_move = null;
			return false;
		}

		MoveData md = _move;
		if(md == null)
		{
			return false;
		}

		MoveData m = new MoveData();

		m.onGeodataPathIndex = md.onGeodataPathIndex + 1;
		m.geoPath = md.geoPath;
		m.geoPathGtx = md.geoPathGtx;
		m.geoPathGty = md.geoPathGty;
		m.geoPathAccurateTx = md.geoPathAccurateTx;
		m.geoPathAccurateTy = md.geoPathAccurateTy;

		if(md.onGeodataPathIndex == md.geoPath.length - 2)
		{
			m._xDestination = md.geoPathAccurateTx;
			m._yDestination = md.geoPathAccurateTy;
			m._zDestination = md.geoPath[m.onGeodataPathIndex].getZ();
		}
		else
		{
			m._xDestination = md.geoPath[m.onGeodataPathIndex].getX();
			m._yDestination = md.geoPath[m.onGeodataPathIndex].getY();
			m._zDestination = md.geoPath[m.onGeodataPathIndex].getZ();
		}

		double dx = m._xDestination - super.getX();
		double dy = m._yDestination - super.getY();
		double distance = Math.sqrt(dx * dx + dy * dy);
		double sin = dy / distance;
		double cos = dx / distance;
		int ticksToMove = 1 + (int) (GameTimeController.TICKS_PER_SECOND * distance / speed);

		setHeading((int) (Math.atan2(-sin, -cos) * 10430.37835) + 32768);
		m._heading = 0; // ?

		m._moveStartTime = GameTimeController.getGameTicks();

		_move = m;

		GameTimeController.getInstance().registerMovingObject(this);

		if(ticksToMove * GameTimeController.MILLIS_IN_TICK > 3000)
		{
			ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(CtrlEvent.EVT_ARRIVED_REVALIDATE), 2000);
		}

		CharMoveToLocation msg = new CharMoveToLocation(this);
		broadcastPacket(msg);

		msg = null;
		m = null;
		md = null;

		return true;
	}

	public boolean validateMovementHeading(int heading)
	{
		MoveData md = _move;

		if(md == null)
		{
			return true;
		}

		boolean result = true;

		if(md._heading != heading)
		{
			result = md._heading == 0;
			md._heading = heading;
		}

		md = null;

		return result;
	}

	@Deprecated
	public final double getDistance(int x, int y)
	{
		double dx = x - getX();
		double dy = y - getY();

		return Math.sqrt(dx * dx + dy * dy);
	}

	@Deprecated
	public final double getDistance(int x, int y, int z)
	{
		double dx = x - getX();
		double dy = y - getY();
		double dz = z - getZ();

		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	public final double getDistanceSq(L2Object object)
	{
		return getDistanceSq(object.getX(), object.getY(), object.getZ());
	}

	public final double getDistanceSq(int x, int y, int z)
	{
		double dx = x - getX();
		double dy = y - getY();
		double dz = z - getZ();

		return dx * dx + dy * dy + dz * dz;
	}

	public final double getPlanDistanceSq(L2Object object)
	{
		return getPlanDistanceSq(object.getX(), object.getY());
	}

	public final double getPlanDistanceSq(int x, int y)
	{
		double dx = x - getX();
		double dy = y - getY();

		return dx * dx + dy * dy;
	}

	public final boolean isInsideRadius(L2Object object, int radius, boolean checkZ, boolean strictCheck)
	{
		if(object != null)
		return isInsideRadius(object.getX(), object.getY(), object.getZ(), radius, checkZ, strictCheck);
		else
			return false;
	}

	public final boolean isInsideRadius(int x, int y, int radius, boolean strictCheck)
	{
		return isInsideRadius(x, y, 0, radius, false, strictCheck);
	}

	public final boolean isInsideRadius(int x, int y, int z, int radius, boolean checkZ, boolean strictCheck)
	{
		double dx = x - getX();
		double dy = y - getY();
		double dz = z - getZ();

		if(strictCheck)
		{
			if(checkZ)
			{
				return dx * dx + dy * dy + dz * dz < radius * radius;
			}
			else
			{
				return dx * dx + dy * dy < radius * radius;
			}
		}
		else
		{
			if(checkZ)
			{
				return dx * dx + dy * dy + dz * dz <= radius * radius;
			}
			else
			{
				return dx * dx + dy * dy <= radius * radius;
			}
		}
	}

	public float getWeaponExpertisePenalty()
	{
		return 1.f;
	}

	public float getArmourExpertisePenalty()
	{
		return 1.f;
	}

	public void setAttackingBodypart()
	{
		_attacking = Inventory.PAPERDOLL_CHEST;
	}

	protected boolean checkAndEquipArrows()
	{
		return true;
	}

	public void addExpAndSp(long addToExp, int addToSp)
	{
	}

	public abstract L2ItemInstance getActiveWeaponInstance();

	public abstract L2Weapon getActiveWeaponItem();

	public abstract L2ItemInstance getSecondaryWeaponInstance();

	public abstract L2Weapon getSecondaryWeaponItem();

	protected void onHitTimer(L2Character target, int damage, boolean crit, boolean miss, boolean soulshot, boolean shld)
	{
		if(target == null || isAlikeDead() || this instanceof L2Npc && ((L2Npc) this).isEventMob)
		{
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return;
		}

		if(this instanceof L2Npc && target.isAlikeDead() || target.isDead() || !getKnownList().knowsObject(target) && !(this instanceof L2DoorInstance))
		{
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);

			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(miss)
		{
			if(target instanceof L2PcInstance)
			{
				if(this instanceof L2Summon)
				{
					int mobId = ((L2Summon) this).getTemplate().npcId;
					((L2PcInstance) target).sendPacket(new SystemMessage(SystemMessageId.AVOIDED_S1S_ATTACK).addNpcName(mobId));
				}
				else
				{
					((L2PcInstance) target).sendPacket(new SystemMessage(SystemMessageId.AVOIDED_S1S_ATTACK).addString(getName()));
				}
			}
		}

		// If attack isn't aborted, send a message system (critical hit, missed...) to attacker/target if they are L2PcInstance
				if(!isAttackAborted())
				{
					if(Config.ALLOW_RAID_BOSS_PUT && (this instanceof L2PcInstance || this instanceof L2Summon)) // Check if option is True Or False. 
					{				
						boolean to_be_cursed = false;
						
						//check on BossZone raid lvl
						if(!(target instanceof L2Playable) && !(target instanceof L2SummonInstance) )
						{ //this must work just on mobs/raids
							
							if( (target.isRaid() && getLevel() > target.getLevel() + 8) || (!(target instanceof L2PcInstance) && ( target.getTarget()!= null && target.getTarget() instanceof L2RaidBossInstance && getLevel() > ((L2RaidBossInstance) target.getTarget()).getLevel() + 8)) || (!(target instanceof L2PcInstance) && ( target.getTarget()!=null && target.getTarget() instanceof L2GrandBossInstance && getLevel() > ((L2GrandBossInstance) target.getTarget()).getLevel() + 8)))									
							{
								to_be_cursed = true;
							}
							
							//advanced check too if not already cursed
							if(!to_be_cursed)
							{
								
								int boss_id = -1;
								L2NpcTemplate boss_template = null;
								L2BossZone boss_zone = GrandBossManager.getInstance().getZone(this);
								
								if(boss_zone!=null)
								{
									boss_id = boss_zone.getBossId();
								}
								
								//boolean alive = false;
								
								if(boss_id != -1)
								{									
									boss_template = NpcTable.getInstance().getTemplate(boss_id);
									
									if(boss_template != null && getLevel() > boss_template.getLevel()  + 8)
									{
										
										L2MonsterInstance boss_instance = null;
										
										if(boss_template.type.equals("L2RaidBoss"))
										{											
											StatsSet actual_boss_stat=RaidBossSpawnManager.getInstance().getStatsSet(boss_id);
											if(actual_boss_stat!=null)
											{
												
												//alive = actual_boss_stat.getLong("respawnTime") == 0;
												boss_instance = RaidBossSpawnManager.getInstance().getBoss(boss_id);												
											}
											
										}
										else if(boss_template.type.equals("L2GrandBoss"))
										{
											
											StatsSet actual_boss_stat=GrandBossManager.getInstance().getStatsSet(boss_id);
											if(actual_boss_stat!=null)
											{												
												//alive = actual_boss_stat.getLong("respawn_time") == 0;
												boss_instance = GrandBossManager.getInstance().getBoss(boss_id);
												
											}
										}
												
										//max allowed rage into take cursed is 3000
										if(boss_instance!=null/* && alive*/ && boss_instance.isInsideRadius(this, 3000, false, false))
										{
											to_be_cursed = true;
										}
									}
									
								}
								
							}
							
						}

				if(to_be_cursed)
				{
					L2Skill skill = SkillTable.getInstance().getInfo(4515, 1);

					if(skill != null)
					{
						abortAttack();
						abortCast();
						getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
						skill.getEffects(target, this);
					}
					else
					{
						_log.warning("Skill 4515 at level 1 is missing in DP.");
					}

					skill = null;

					if(target instanceof L2MinionInstance)
					{
						((L2MinionInstance) target).getLeader().stopHating(this);

						List<L2MinionInstance> spawnedMinions = ((L2MinionInstance) target).getLeader().getSpawnedMinions();
						if(spawnedMinions != null && spawnedMinions.size() > 0)
						{
							Iterator<L2MinionInstance> itr = spawnedMinions.iterator();
							L2MinionInstance minion;
							while(itr.hasNext())
							{
								minion = itr.next();
								if(((L2MinionInstance) target).getLeader().getMostHated() == null)
								{
									((L2AttackableAI) minion.getAI()).setGlobalAggro(-25);
									minion.clearAggroList();
									minion.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
									minion.setWalking();
								}
								if(minion != null && !minion.isDead())
								{
									((L2AttackableAI) minion.getAI()).setGlobalAggro(-25);
									minion.clearAggroList();
									minion.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
									minion.addDamage(((L2MinionInstance) target).getLeader().getMostHated(), 100);
								}
							}
							itr = null;
							spawnedMinions = null;
							minion = null;
						}
					}
					else
					{
						((L2Attackable) target).stopHating(this);
						List<L2MinionInstance> spawnedMinions = ((L2MonsterInstance) target).getSpawnedMinions();
						if(spawnedMinions != null && spawnedMinions.size() > 0)
						{
							Iterator<L2MinionInstance> itr = spawnedMinions.iterator();
							L2MinionInstance minion;
							while(itr.hasNext())
							{
								minion = itr.next();
								if(((L2Attackable) target).getMostHated() == null)
								{
									((L2AttackableAI) minion.getAI()).setGlobalAggro(-25);
									minion.clearAggroList();
									minion.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
									minion.setWalking();
								}
								if(minion != null && !minion.isDead())
								{
									((L2AttackableAI) minion.getAI()).setGlobalAggro(-25);
									minion.clearAggroList();
									minion.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
									minion.addDamage(((L2Attackable) target).getMostHated(), 100);
								}
							}
							itr = null;
							spawnedMinions = null;
							minion = null;
						}
					}

					damage = 0;
				}
			}

			sendDamageMessage(target, damage, false, crit, miss);

			if(target instanceof L2PcInstance)
			{
				L2PcInstance enemy = (L2PcInstance) target;

				if(shld)
				{
					enemy.sendPacket(new SystemMessage(SystemMessageId.SHIELD_DEFENCE_SUCCESSFULL));
				}

				enemy = null;
			}
			else if(target instanceof L2Summon)
			{
				((L2Summon) target).getOwner().sendPacket(new SystemMessage(SystemMessageId.PET_RECEIVED_S2_DAMAGE_BY_S1).addString(getName()).addNumber(damage));
			}

			if(!miss && damage > 0)
			{
				L2Weapon weapon = getActiveWeaponItem();
				boolean isBow = weapon != null && weapon.getItemType().toString().equalsIgnoreCase("Bow");

				if(!isBow)
				{
					double absorbPercent = getStat().calcStat(Stats.ABSORB_DAMAGE_PERCENT, 0, null, null);

					if(absorbPercent > 0)
					{
						int maxCanAbsorb = (int) (getMaxHp() - getCurrentHp());
						int absorbDamage = (int) (absorbPercent / 100. * damage);

						if(absorbDamage > maxCanAbsorb)
						{
							absorbDamage = maxCanAbsorb;
						}

						if(absorbDamage > 0)
						{
							setCurrentHp(getCurrentHp() + absorbDamage);
						}
					}

					double reflectPercent = target.getStat().calcStat(Stats.REFLECT_DAMAGE_PERCENT, 0, null, null);

					if(reflectPercent > 0)
					{
						int reflectedDamage = (int) (reflectPercent / 100. * damage);
						damage -= reflectedDamage;

						if(reflectedDamage > target.getMaxHp())
						{
							reflectedDamage = target.getMaxHp();
						}

						if((reflectedDamage > getCurrentHp()) && (getCurrentHp() >= 1))
						{
							reflectedDamage = (int)(getCurrentHp() - 1);
						}

						getStatus().reduceHp(reflectedDamage, target, true);
					}
				}

				// Reduce target HPs
				target.reduceCurrentHp(damage, this);

				// Notify AI with EVT_ATTACKED
				target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this);
				
				getAI().clientStartAutoAttack();
				
				if(!target.isRaid() && Formulas.calcAtkBreak(target, damage))
				{
					target.breakAttack();
					target.breakCast();
				}

				// Maybe launch chance skills on us
				if (_chanceSkills != null)
				{
					_chanceSkills.onHit(target, false, crit);
				}

				if(target.getChanceSkills() != null)
				{
					target.getChanceSkills().onHit(this, true, crit);
				}

				weapon = null;
			}

			L2Weapon activeWeapon = getActiveWeaponItem();

			if(activeWeapon != null)
			{
				activeWeapon.getSkillEffects(this, target, crit);
			}

			activeWeapon = null;

			return;
		}

		if (!isCastingNow() && !isCastingSimultaneouslyNow())
		getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
	}

	public void breakAttack()
	{
		if(isAttackingNow())
		{
			abortAttack();

			if(this instanceof L2PcInstance)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(new SystemMessage(SystemMessageId.ATTACK_FAILED));
			}
		}
	}

	public void breakCast()
	{
		if(isCastingNow() && canAbortCast() && getLastSkillCast() != null && getLastSkillCast().isMagic())
		{
			abortCast();

			if(this instanceof L2PcInstance)
			{
				sendPacket(new SystemMessage(SystemMessageId.CASTING_INTERRUPTED));
			}
		}
	}

	protected void reduceArrowCount()
	{
	}

	@Override
	public void onForcedAttack(L2PcInstance player)
	{
		if(player.getTarget() == null || !(player.getTarget() instanceof L2Character))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(isInsidePeaceZone(player))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(player.isInOlympiadMode() && player.getTarget() != null && player.getTarget() instanceof L2Playable)
		{
			L2PcInstance target;

			if(player.getTarget() instanceof L2Summon)
			{
				target = ((L2Summon) player.getTarget()).getOwner();
			}
			else
			{
				target = (L2PcInstance) player.getTarget();
			}

			if(target.isInOlympiadMode() && !player.isOlympiadStart() && player.getOlympiadGameId() == target.getOlympiadGameId())
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			target = null;
		}

		if(!player.isAttackable() && !player.getAccessLevel().allowPeaceAttack() && !isInFunEvent())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(player.isConfused() || player.isBlocked())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(!GeoData.getInstance().canSeeTarget(player, this))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
	}

	public boolean canInteract(L2PcInstance player)
	{
		// Can't interact while casting a spell.
		if (player.isCastingNow())
			return false;
		
		// Can't interact while died.
		if (player.isDead() || player.isFakeDeath())
			return false;
		
		// Can't interact sitted.
		if (player.isSitting())
			return false;
		
		// Can't interact in shop mode, or during a transaction or a request.
		if (player.getPrivateStoreType() != 0 || player.isProcessingTransaction())
			return false;
		
		// Can't interact if regular distance doesn't match.
		if (!isInsideRadius(player, L2Npc.INTERACTION_DISTANCE, true, false))
			return false;
		
		return true;
	}
	
	public boolean isInsidePeaceZone(L2PcInstance attacker)
	{
		if(!isInFunEvent() || !attacker.isInFunEvent())
		{
			return isInsidePeaceZone(attacker, this);
		}

		return false;
	}

	public static boolean isInsidePeaceZone(L2PcInstance attacker, L2Object target)
	{
		return !attacker.getAccessLevel().allowPeaceAttack() && isInsidePeaceZone((L2Object) attacker, target);
	}

	public static boolean isInsidePeaceZone(L2Object attacker, L2Object target)
	{
		if(target == null)
		{
			return false;
		}

		if(target instanceof L2MonsterInstance || attacker instanceof L2MonsterInstance)
		{
			return false;
		}

		if(target instanceof L2GuardInstance || attacker instanceof L2GuardInstance)
		{
			return false;
		}

		if (target instanceof L2NpcInstance || attacker instanceof L2NpcInstance)
		{
			return false;
		}
		
		if(Config.KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE)
		{
			if(target instanceof L2PcInstance && ((L2PcInstance) target).getKarma() > 0)
			{
				return false;
			}

			if(target instanceof L2Summon && ((L2Summon) target).getOwner().getKarma() > 0)
			{
				return false;
			}

			if(attacker instanceof L2PcInstance && ((L2PcInstance) attacker).getKarma() > 0)
			{
				if(target instanceof L2PcInstance && ((L2PcInstance) target).getPvpFlag() > 0)
				{
					return false;
				}

				if(target instanceof L2Summon && ((L2Summon) target).getOwner().getPvpFlag() > 0)
				{
					return false;
				}
			}

			if(attacker instanceof L2Summon && ((L2Summon) attacker).getOwner().getKarma() > 0)
			{
				if(target instanceof L2PcInstance && ((L2PcInstance) target).getPvpFlag() > 0)
				{
					return false;
				}

				if(target instanceof L2Summon && ((L2Summon) target).getOwner().getPvpFlag() > 0)
				{
					return false;
				}
			}
		}

		if(attacker instanceof L2PcInstance && target instanceof L2PcInstance)
		{
			L2PcInstance src = (L2PcInstance) attacker;
			L2PcInstance dst = (L2PcInstance) target;

			if(src.isInOlympiadMode() && src.isOlympiadStart() && dst.isInOlympiadMode() && dst.isOlympiadStart())
			{
				return false;
			}
		}

		if(attacker instanceof L2Character && target instanceof L2Character)
		{
			return ((L2Character) target).isInsideZone(ZONE_PEACE) || ((L2Character) attacker).isInsideZone(ZONE_PEACE);
		}

		if(attacker instanceof L2Character)
		{
			return TownManager.getInstance().getTown(target.getX(), target.getY(), target.getZ()) != null || ((L2Character) attacker).isInsideZone(ZONE_PEACE);
		}

		return TownManager.getInstance().getTown(target.getX(), target.getY(), target.getZ()) != null || TownManager.getInstance().getTown(attacker.getX(), attacker.getY(), attacker.getZ()) != null;
	}

	public Boolean isInActiveRegion()
	{
		try
		{
			L2WorldRegion region = L2World.getInstance().getRegion(getX(), getY());
			return region != null && region.isActive();
		}
		catch(Exception e)
		{
			if(this instanceof L2PcInstance)
			{
				_log.warning("Player " + getName() + " at bad coords: (x: " + getX() + ", y: " + getY() + ", z: " + getZ() + ").");

				((L2PcInstance) this).sendMessage("Error with your coordinates! Please reboot your game fully!");
				((L2PcInstance) this).teleToLocation(80753, 145481, -3532, false);
			}
			else
			{
				_log.warning("Object " + getName() + " at bad coords: (x: " + getX() + ", y: " + getY() + ", z: " + getZ() + ").");
				decayMe();
			}
			return false;
		}
	}

	public boolean isInParty()
	{
		return false;
	}

	public L2Party getParty()
	{
		return null;
	}

	public int calculateTimeBetweenAttacks(L2Character target, L2Weapon weapon)
	{
		double atkSpd = 0;
		if(weapon != null)
		{
			switch(weapon.getItemType())
			{
				case BOW:
					atkSpd = getStat().getPAtkSpd();
					return (int) (1500 * 345 / atkSpd);
				case DAGGER:
					atkSpd = getStat().getPAtkSpd();
					break;
				default:
					atkSpd = getStat().getPAtkSpd();
			}
		}
		else
		{
			atkSpd = getPAtkSpd();
		}

		return Formulas.getInstance().calcPAtkSpd(this, target, atkSpd);
	}

	public int calculateReuseTime(L2Character target, L2Weapon weapon)
	{
		if(weapon == null)
			return 0;

		int reuse = weapon.getAttackReuseDelay();

		if(reuse == 0)
		{
			return 0;
		}

		reuse *= getStat().getReuseModifier(target);

		double atkSpd = getStat().getPAtkSpd();

		switch(weapon.getItemType())
		{
			case BOW:
				return (int) (reuse * 345 / atkSpd);
			default:
				return (int) (reuse * 312 / atkSpd);
		}
	}

	public boolean isUsingDualWeapon()
	{
		return false;
	}

	public L2Skill addSkill(L2Skill newSkill)
	{
		L2Skill oldSkill = null;

		if(newSkill != null)
		{
			oldSkill = _skills.put(newSkill.getId(), newSkill);

			if(oldSkill != null)
			{
				if(oldSkill.triggerAnotherSkill())
				{
					removeSkill(oldSkill.getTriggeredId(), true);
				}
				removeStatsOwner(oldSkill);
				stopSkillEffects(oldSkill.getId());
			}

			addStatFuncs(newSkill.getStatFuncs(null, this));

			if(oldSkill != null && _chanceSkills != null)
			{
				removeChanceSkill(oldSkill.getId());
			}
			if(newSkill.isChance())
			{
				addChanceSkill(newSkill);
			}

			if(newSkill.isChance() && newSkill.triggerAnotherSkill())
			{
				L2Skill triggeredSkill = SkillTable.getInstance().getInfo(newSkill.getTriggeredId(), newSkill.getTriggeredLevel());
				addSkill(triggeredSkill);
			}
		}

		return oldSkill;
	}

	public void addChanceSkill(L2Skill skill)
	{
		synchronized (this)
		{
			if(_chanceSkills == null)
			{
				_chanceSkills = new ChanceSkillList(this);
			}

			_chanceSkills.put(skill, skill.getChanceCondition());
		}
	}

	public void removeChanceSkill(int id)
	{
		synchronized (this)
		{
			for(L2Skill skill : _chanceSkills.keySet())
			{
				if(skill.getId() == id)
				{
					_chanceSkills.remove(skill);
				}
			}

			if(_chanceSkills.size() == 0)
			{
				_chanceSkills = null;
			}
		}
	}

	public L2Skill removeSkill(L2Skill skill)
	{
		if(skill == null)
		{
			return null;
		}

		return removeSkill(skill.getId());
	}

	public L2Skill removeSkill(int skillId)
	{
		return removeSkill(skillId, true);
	}

	public L2Skill removeSkill(int skillId, boolean cancelEffect)
	{
		L2Skill oldSkill = _skills.remove(skillId);
		if(oldSkill != null)
		{
			if(oldSkill.triggerAnotherSkill())
			{
				removeSkill(oldSkill.getTriggeredId(), true);
			}

			if(getLastSkillCast() != null && isCastingNow())
			{
				if(oldSkill.getId() == getLastSkillCast().getId())
				{
					abortCast();
				}
			}
			if (getLastSimultaneousSkillCast() != null && isCastingSimultaneouslyNow()) 
			{ 
				if (oldSkill.getId() == getLastSimultaneousSkillCast().getId()) 
					abortCast(); 
			} 
			if (this instanceof L2PcInstance) 
			{ 
				if (((L2PcInstance)this).getCurrentSkill() != null && isCastingNow()) 
				{ 
					if (oldSkill.getId() == ((L2PcInstance)this).getCurrentSkill().getSkillId()) 
						abortCast(); 
				} 
			}
			if(cancelEffect || oldSkill.isToggle())
			{
				L2Effect e = getFirstEffect(oldSkill);
				if(e == null)
				{
					removeStatsOwner(oldSkill);
					stopSkillEffects(oldSkill.getId());
				}
			}

			if(oldSkill.isChance() && _chanceSkills != null)
			{
				removeChanceSkill(oldSkill.getId());
			}
			removeStatsOwner(oldSkill);
		}
		return oldSkill;
	}

	public final L2Skill[] getAllSkills()
	{
		if(_skills == null)
		{
			return new L2Skill[0];
		}

		try
		{
			return _skills.values().toArray(new L2Skill[_skills.values().size()]);
		}
		catch(UnsupportedOperationException e)
		{
			if(this instanceof L2PcInstance)
			{
				new Disconnection((L2PcInstance) this);
			}
		}
		return new L2Skill[0];
	}

	public ChanceSkillList getChanceSkills()
	{
		return _chanceSkills;
	}

	public int getSkillLevel(int skillId)
	{
		if(_skills == null)
		{
			return -1;
		}

		L2Skill skill = _skills.get(skillId);

		if(skill == null)
		{
			return -1;
		}

		return skill.getLevel();
	}

	public final L2Skill getKnownSkill(int skillId)
	{
		if(_skills == null)
		{
			return null;
		}

		return _skills.get(skillId);
	}

	public int getBuffCount() 
	{ 
		return _effects.getBuffCount(); 
	} 
	
	public int getDanceCount() 
	{ 
		return _effects.getDanceCount(); 
	}

	public int getMaxBuffCount()
	{
		return Config.BUFFS_MAX_AMOUNT + Math.max(0, getSkillLevel(L2Skill.SKILL_DIVINE_INSPIRATION));
	}
	
	public void onMagicLaunchedTimer(L2Object[] targets, L2Skill skill, int coolTime, boolean instant, boolean simultaneously)
	{
		if((skill == null || targets == null))
		{
			abortCast();
			return;
		}

		if (targets.length == 0)
		{
			switch (skill.getTargetType())
			{
			// only AURA-type skills can be cast without target
				case TARGET_AURA:
					break;
				default:
					abortCast();
					return;
			}
		}
		
		// Escaping from under skill's radius and peace zone check. First version, not perfect in AoE skills.
		int escapeRange = 0;
		if (skill.getEffectRange() > escapeRange)
			escapeRange = skill.getEffectRange();
		else if (skill.getCastRange() < 0 && skill.getSkillRadius() > 80)
			escapeRange = skill.getSkillRadius();
		
		if (targets.length > 0 && escapeRange > 0)
		{
			int _skiprange = 0;
			int _skipgeo = 0;
			int _skippeace = 0;
			List<L2Character> targetList = new FastList<L2Character>(targets.length);
			for (L2Object target : targets)
			{
				if (target instanceof L2Character)
				{
					if (!Util.checkIfInRange(escapeRange, this, target, true))
					{
						_skiprange++;
						continue;
					}
					if (skill.getSkillRadius() > 0 && skill.isOffensive() && Config.GEODATA > 0 && !GeoData.getInstance().canSeeTarget(this, target))
					{
						_skipgeo++;
						continue;
					}
					if (skill.isOffensive())
					{
						if (this instanceof L2PcInstance)
						{
							if (((L2Character) target).isInsidePeaceZone((L2PcInstance) this))
							{
								_skippeace++;
								continue;
							}
						}
						else
						{
							if (isInsidePeaceZone(this, target))
							{
								_skippeace++;
								continue;
							}
						}
					}
					targetList.add((L2Character) target);
				}
			}
			if (targetList.isEmpty())
			{
				if (this instanceof L2PcInstance)
				{
					if (_skiprange > 0)
						sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED));
					else if (_skipgeo > 0)
						sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANT_SEE_TARGET));
					else if (_skippeace > 0)
						sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
				}
				abortCast();
				return;
			}
			targets = targetList.toArray(new L2Character[targetList.size()]);
		}

		if ((simultaneously && !isCastingSimultaneouslyNow())  
				|| (!simultaneously && !isCastingNow())  
				|| (isAlikeDead() && !skill.isPotion())) 
		{
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return;
		}

		int magicId = skill.getDisplayId();

		int level = getSkillLevel(skill.getId());

		if(level < 1)
		{
			level = 1;
		}

		if(!skill.isPotion())
		{
			broadcastPacket(new MagicSkillLaunched(this, magicId, level, targets));
		}

		if(instant)
		{
			onMagicHitTimer(targets, skill, coolTime, true, simultaneously);
		}
		else
		{
			_skillCast = ThreadPoolManager.getInstance().scheduleEffect(new MagicUseTask(targets, skill, coolTime, 2, simultaneously), 200);
		}

	}

	public void onMagicHitTimer(L2Object[] targets, L2Skill skill, int coolTime, boolean instant, boolean simultaneously)
	{
		if((skill == null || targets == null || targets.length <= 0) && skill.getTargetType() != SkillTargetType.TARGET_AURA)
		{
			abortCast();
			return;
		}

		if(getForceBuff() != null)
		{
			if (simultaneously) 
			{ 
				_skillCast2 = null; 
				setIsCastingSimultaneouslyNow(false); 
			} 
			else 
			{ 
				_skillCast = null; 
				setIsCastingNow(false); 
			} 
			getForceBuff().onCastAbort();
			notifyQuestEventSkillFinished(skill, targets[0]);
			return;
		}

		L2Effect mog = getFirstEffect(L2EffectType.SIGNET_GROUND);
		if(mog != null)
		{
			if (simultaneously) 
			{ 
				_skillCast2 = null; 
				setIsCastingSimultaneouslyNow(false); 
			} 
			else 
			{ 
				_skillCast = null; 
				setIsCastingNow(false); 
			}
			mog.exit();
			notifyQuestEventSkillFinished(skill, targets[0]);
			return;
		}

		final L2Object[] targets2 = targets;
		try
		{
			if(targets2 != null && targets2.length!=0){
				
				// Go through targets table
				for(int i=0;i<targets2.length;i++)//L2Object target2 : targets)
				{
					L2Object target2 = targets2[i];
					if(target2==null){
						continue;
					}
					
					if(target2 instanceof L2Playable)
					{
						L2Character target = (L2Character) target2;

						if(skill.getSkillType() == L2SkillType.BUFF || skill.getSkillType() == L2SkillType.SEED)
						{
							SystemMessage smsg = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
							smsg.addString(skill.getName());
							target.sendPacket(smsg);
							smsg = null;
						}

						if(this instanceof L2PcInstance && target instanceof L2Summon)
						{
							((L2Summon) target).getOwner().sendPacket(new PetInfo((L2Summon) target));
							sendPacket(new NpcInfo((L2Summon) target, this));

							// The PetInfo packet wipes the PartySpelled (list of active spells' icons).  Re-add them
							((L2Summon) target).updateEffectIcons(true);
						}

						target = null;
					}
				}
				
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
			
			
		try{
			
			StatusUpdate su = new StatusUpdate(getObjectId());
			boolean isSendStatus = false;

			// Consume MP of the L2Character and Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
			double mpConsume = getStat().getMpConsume(skill);

			if(mpConsume > 0)
			{
				if(skill.isDance())
				{
					getStatus().reduceMp(calcStat(Stats.DANCE_MP_CONSUME_RATE, mpConsume, null, null));
				}
				else if(skill.isMagic())
				{
					getStatus().reduceMp(calcStat(Stats.MAGICAL_MP_CONSUME_RATE, mpConsume, null, null));
				}
				else
				{
					getStatus().reduceMp(calcStat(Stats.PHYSICAL_MP_CONSUME_RATE, mpConsume, null, null));
				}

				su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
				isSendStatus = true;
			}

			// Consume HP if necessary and Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
			if(skill.getHpConsume() > 0)
			{
				double consumeHp;

				consumeHp = calcStat(Stats.HP_CONSUME_RATE, skill.getHpConsume(), null, null);

				if(consumeHp + 1 >= getCurrentHp())
				{
					consumeHp = getCurrentHp() - 1.0;
				}

				getStatus().reduceHp(consumeHp, this);

				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				isSendStatus = true;
			}

			// Send a Server->Client packet StatusUpdate with MP modification to the L2PcInstance
			if(isSendStatus)
			{
				sendPacket(su);
			}

			// Consume Items if necessary and Send the Server->Client packet InventoryUpdate with Item modification to all the L2Character
			if(skill.getItemConsume() > 0)
			{
				consumeItem(skill.getItemConsumeId(), skill.getItemConsume());
			}

			// Launch the magic skill in order to calculate its effects
			callSkill(skill, targets);

			su = null;
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		if(instant || coolTime == 0)
		{
			onMagicFinalizer(targets, skill, simultaneously);
		}
		else
		{ 
			if (simultaneously) 
				_skillCast2 = ThreadPoolManager.getInstance().scheduleEffect(new MagicUseTask(targets, skill, coolTime, 3, simultaneously), coolTime); 
			else 
				_skillCast = ThreadPoolManager.getInstance().scheduleEffect(new MagicUseTask(targets, skill, coolTime, 3, simultaneously), coolTime); 
		} 
	}

	public void onMagicFinalizer(L2Object[] targets, L2Skill skill, boolean simultaneously)
	{
		if (simultaneously) 
		{ 
			_skillCast2 = null; 
			setIsCastingSimultaneouslyNow(false); 
			return; 
		} 
		else 
		{ 
			_skillCast = null; 
			setIsCastingNow(false); 
			_castInterruptTime = 0; 
		}
		enableAllSkills();

        // Notify the AI of the L2Character with EVT_FINISH_CASTING
		getAI().notifyEvent(CtrlEvent.EVT_FINISH_CASTING);
		
		final L2Object target = targets.length > 0 ? targets[0] : null;
		
		if(skill.nextActionIsAttack() && getTarget() instanceof L2Character && getTarget() != this && getTarget() == target && target.isAttackable())
		{
			if (getAI() == null || getAI().getNextIntention() == null || getAI().getNextIntention().getCtrlIntention() != CtrlIntention.AI_INTENTION_MOVE_TO)
				getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, getTarget());
		}

		if(skill.isOffensive() && !(skill.getSkillType() == L2SkillType.UNLOCK) && !(skill.getSkillType() == L2SkillType.DELUXE_KEY_UNLOCK))
				getAI().clientStartAutoAttack();
		
		notifyQuestEventSkillFinished(skill, getTarget());
		
		if(this instanceof L2PcInstance)
		{
			L2PcInstance currPlayer = (L2PcInstance) this;
			SkillDat queuedSkill = currPlayer.getQueuedSkill();
			
			if(skill.isPotion())
			{
				queuedSkill = currPlayer.getCurrentSkill();
			}

			currPlayer.setCurrentSkill(null, false, false);

			if(queuedSkill != null)
			{
				currPlayer.setQueuedSkill(null, false, false);

				ThreadPoolManager.getInstance().executeTask(new QueuedMagicUseTask(currPlayer, queuedSkill.getSkill(), queuedSkill.isCtrlPressed(), queuedSkill.isShiftPressed()));
			}

			currPlayer = null;
			queuedSkill = null;
			
		}
		
	}

	private void notifyQuestEventSkillFinished(L2Skill skill, L2Object target)
	{
		if(this instanceof L2NpcInstance)
		{
			try
			{
				if(((L2NpcTemplate) getTemplate()).getEventQuests(Quest.QuestEventType.ON_SPELL_FINISHED) != null)
				{
					L2PcInstance player=null;
					if(target instanceof L2SummonInstance)
					{
						player = ((L2SummonInstance) target).getOwner();
					}
					else if(target instanceof L2PcInstance)
					{
						player = (L2PcInstance) target;
					}

					if(player!=null)
						for(Quest quest : ((L2NpcTemplate) getTemplate()).getEventQuests(Quest.QuestEventType.ON_SPELL_FINISHED))
						{
							quest.notifySpellFinished(((L2NpcInstance) this), player, skill);
						}
				}
			}
			catch(Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}

	public void consumeItem(int itemConsumeId, int itemCount)
	{
	}

	public void enableSkill(int skillId)
	{
		if(_disabledSkills == null)
		{
			return;
		}

		_disabledSkills.remove(new Integer(skillId));

		if(this instanceof L2PcInstance)
		{
			removeTimeStamp(skillId);
		}
	}

	public void disableSkill(int skillId)
	{
		if(_disabledSkills == null)
		{
			_disabledSkills = Collections.synchronizedList(new FastList<Integer>());
		}

		_disabledSkills.add(skillId);
	}

	public void disableSkill(int skillId, long delay)
	{
		disableSkill(skillId);

		if(delay > 10)
		{
			ThreadPoolManager.getInstance().scheduleAi(new EnableSkill(skillId), delay);
		}
	}

	public boolean isSkillDisabled(int skillId)
	{
		L2Skill skill = SkillTable.getInstance().getInfo(skillId, 1);

		if(isAllSkillsDisabled() && !skill.isPotion())
			return true;

		if(this instanceof L2PcInstance)
		{
			L2PcInstance activeChar = (L2PcInstance) this;

			if((skill.getSkillType()==L2SkillType.FISHING || skill.getSkillType()==L2SkillType.REELING || skill.getSkillType()==L2SkillType.PUMPING) && !activeChar.isFishing() && (activeChar.getActiveWeaponItem() != null && activeChar.getActiveWeaponItem().getItemType()!=L2WeaponType.ROD))
			{
				if(skill.getSkillType()==L2SkillType.PUMPING)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.CAN_USE_PUMPING_ONLY_WHILE_FISHING));
				}
				else if(skill.getSkillType()==L2SkillType.REELING)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.CAN_USE_REELING_ONLY_WHILE_FISHING));
				}
				else if(skill.getSkillType()==L2SkillType.FISHING)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.FISHING_POLE_NOT_EQUIPPED));
				}
				
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
				sm.addString(skill.getName());
				activeChar.sendPacket(sm);
				return true;
			}

			if((skill.getSkillType()==L2SkillType.FISHING || skill.getSkillType()==L2SkillType.REELING || skill.getSkillType()==L2SkillType.PUMPING) && activeChar.getActiveWeaponItem() == null)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
				sm.addString(skill.getName());
				activeChar.sendPacket(sm);
				return true;
			}

			if((skill.getSkillType()==L2SkillType.REELING || skill.getSkillType()==L2SkillType.PUMPING) && !activeChar.isFishing() && (activeChar.getActiveWeaponItem() != null && activeChar.getActiveWeaponItem().getItemType()==L2WeaponType.ROD))
			{
				if(skill.getSkillType()==L2SkillType.PUMPING)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.CAN_USE_PUMPING_ONLY_WHILE_FISHING));
				}
				else if(skill.getSkillType()==L2SkillType.REELING)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.CAN_USE_REELING_ONLY_WHILE_FISHING));
				}
				
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
				sm.addString(skill.getName());
				activeChar.sendPacket(sm);
				return true;
			}
			
			if(activeChar.isHero() && HeroSkillTable.isHeroSkill(skillId) && activeChar.isInOlympiadMode() && activeChar.isOlympiadStart())
			{
				activeChar.sendMessage("You can't use Hero skills during Olympiad match.");
				return true;
			}
		}

		if(_disabledSkills == null)
			return false;

		return _disabledSkills.contains(skillId);
	}

	public void disableAllSkills()
	{
		_allSkillsDisabled = true;
	}

	public void enableAllSkills()
	{
		_allSkillsDisabled = false;
	}

	public void callSkill(L2Skill skill, L2Object[] targets)
	{
		try
		{
			if(skill.isToggle() && getFirstEffect(skill.getId()) != null)
			{
				return;
			}

			if(skill.getId() >= 3080 && skill.getId() < 3260 && _skills.get(skill.getId()) == null)
				return;

			for(L2Object target : targets)
			{
				if(target instanceof L2Character)
				{
					L2Character player = (L2Character) target;
					
					L2Weapon activeWeapon = getActiveWeaponItem();
					if(activeWeapon != null && !((L2Character) target).isDead())
					{
						if(activeWeapon.getSkillEffects(this, player, skill).length > 0 && this instanceof L2PcInstance)
						{
							sendPacket(SystemMessage.sendString("Target affected by weapon special ability!"));
						}
					}

					if(target instanceof L2Character)
					{
						L2Character targ = (L2Character) target;

						if(ChanceSkillList.canTriggerByCast(this, targ, skill))
						{
							if(_chanceSkills != null)
							{
								_chanceSkills.onSkillHit(targ, false, skill.isMagic(), skill.isOffensive());
							}
							if(targ.getChanceSkills() != null && target != this)
							{
								targ.getChanceSkills().onSkillHit(this, true, skill.isMagic(), skill.isOffensive());
							}
						}
					}

					if(Config.ALLOW_RAID_BOSS_PUT && (this instanceof L2PcInstance || this instanceof L2Summon))
					{
						boolean to_be_cursed = false;

						if(!(target instanceof L2Playable) && !(target instanceof L2SummonInstance))
						{
							if((player.isRaid() && getLevel() > player.getLevel() + 8) || (!(player instanceof L2PcInstance) && ( player.getTarget() != null && player.getTarget() instanceof L2RaidBossInstance && getLevel() > ((L2RaidBossInstance) player.getTarget()).getLevel() + 8)) || (!(player instanceof L2PcInstance) && ( player.getTarget() != null && player.getTarget() instanceof L2GrandBossInstance && getLevel() > ((L2GrandBossInstance) player.getTarget()).getLevel() + 8)))
							{
								to_be_cursed = true;
							}

							if(!to_be_cursed)
							{
								int boss_id = -1;
								L2NpcTemplate boss_template = null;
								L2BossZone boss_zone = GrandBossManager.getInstance().getZone(this);

								if(boss_zone != null)
								{
									boss_id = boss_zone.getBossId();
								}

								if(boss_id != -1)
								{
									boss_template = NpcTable.getInstance().getTemplate(boss_id);

									if(boss_template != null && getLevel() > boss_template.getLevel() + 8)
									{
										L2MonsterInstance boss_instance = null;

										if(boss_template.type.equals("L2RaidBoss"))
										{
											StatsSet actual_boss_stat=RaidBossSpawnManager.getInstance().getStatsSet(boss_id);
											if(actual_boss_stat != null)
											{
												boss_instance = RaidBossSpawnManager.getInstance().getBoss(boss_id);
											}
										}
										else if(boss_template.type.equals("L2GrandBoss"))
										{
											StatsSet actual_boss_stat=GrandBossManager.getInstance().getStatsSet(boss_id);
											if(actual_boss_stat!=null)
											{
												boss_instance = GrandBossManager.getInstance().getBoss(boss_id);
											}
										}

										if(boss_instance != null && boss_instance.isInsideRadius(this, 3000, false, false))
										{
											to_be_cursed = true;
										}
									}
								}
							}
						}

						if(to_be_cursed)
						{
							if(skill.isMagic())
							{
								L2Skill tempSkill = SkillTable.getInstance().getInfo(4215, 1);
								if(tempSkill != null)
								{
									abortAttack();
									abortCast();
									getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
									tempSkill.getEffects(player, this);
								}
								else
								{
									_log.warning("Skill 4215 at level 1 is missing in DP.");
								}

								tempSkill = null;
							}
							else
							{
								L2Skill tempSkill = SkillTable.getInstance().getInfo(4515, 1);
								if(tempSkill != null)									
								{ 
									abortAttack(); 
									abortCast(); 
									getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
									tempSkill.getEffects(player, this);
								}
								else
								{
									_log.warning("Skill 4515 at level 1 is missing in DP.");
								}

								tempSkill = null;

								if(player instanceof L2MinionInstance)
								{
									((L2MinionInstance) player).getLeader().stopHating(this);
									List<L2MinionInstance> spawnedMinions = ((L2MonsterInstance) player).getSpawnedMinions();
									if(spawnedMinions != null && spawnedMinions.size() > 0)
									{
										Iterator<L2MinionInstance> itr = spawnedMinions.iterator();
										L2MinionInstance minion;
										while(itr.hasNext())
										{
											minion = itr.next();
											if(((L2Attackable) player).getMostHated() == null)
											{
												((L2AttackableAI) minion.getAI()).setGlobalAggro(-25);
												minion.clearAggroList();
												minion.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
												minion.setWalking();
											}

											if((minion == null) || (minion.isDead()))
											{
												continue;
											}

											((L2AttackableAI) minion.getAI()).setGlobalAggro(-25);
											minion.clearAggroList();
											minion.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
											minion.addDamage(((L2Attackable) player).getMostHated(), 100);
										}
										itr = null;
										spawnedMinions = null;
										minion = null;
									}
								}
								else
								{
									((L2Attackable) player).stopHating(this);
									List<L2MinionInstance> spawnedMinions = ((L2MonsterInstance) player).getSpawnedMinions();
									if(spawnedMinions != null && spawnedMinions.size() > 0)
									{
										Iterator<L2MinionInstance> itr = spawnedMinions.iterator();
										L2MinionInstance minion;
										while(itr.hasNext())
										{
											minion = itr.next();
											if(((L2Attackable) player).getMostHated() == null)
											{
												((L2AttackableAI) minion.getAI()).setGlobalAggro(-25);
												minion.clearAggroList();
												minion.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
												minion.setWalking();
											}

											if((minion == null) || (minion.isDead()))
											{
												continue;
											}

											((L2AttackableAI) minion.getAI()).setGlobalAggro(-25);
											minion.clearAggroList();
											minion.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
											minion.addDamage(((L2Attackable) player).getMostHated(), 100);
										}
										itr = null;
										spawnedMinions = null;
										minion = null;
									}
								}
							}
							return;
						}
					}

					L2PcInstance activeChar = null;

					if(this instanceof L2PcInstance)
					{
						activeChar = (L2PcInstance) this;
					}
					else if(this instanceof L2Summon)
					{
						activeChar = ((L2Summon) this).getOwner();
					}

					if(activeChar != null)
					{
						if(skill.isOffensive())
						{
							if (player instanceof L2PcInstance || player instanceof L2Summon)
							{
								if(skill.getSkillType() != L2SkillType.SIGNET && skill.getSkillType() != L2SkillType.SIGNET_CASTTIME)
								{
									player.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, activeChar);

									// attack of the own pet does not flag player 
									// triggering trap not flag trap owner 
									if (player.getPet() != target) 
										activeChar.updatePvPStatus(player); 
								}
							}
							else if(player instanceof L2Attackable)
							{
								switch(skill.getSkillType())
								{
									case AGGREDUCE:
									case AGGREDUCE_CHAR:
									case AGGREMOVE:
										break;
									default:
										((L2Character) target).addAttackerToAttackByList(this);
										((L2Character) target).getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this);
										break;
								}
							}
						}
						else
						{
							if(player instanceof L2PcInstance)
							{
								if(!player.equals(this) && (((L2PcInstance) player).getPvpFlag() > 0 || ((L2PcInstance) player).getKarma() > 0))
								{
									activeChar.updatePvPStatus();
								}
							}
							else if(player instanceof L2Attackable && !(skill.getSkillType() == L2SkillType.SUMMON) && !(skill.getSkillType() == L2SkillType.BEAST_FEED) && !(skill.getSkillType() == L2SkillType.UNLOCK) && !(skill.getSkillType() == L2SkillType.DELUXE_KEY_UNLOCK))
							{
								activeChar.updatePvPStatus(this);
							}
						}
						player = null;
						//activeWeapon = null;
					}
					activeChar = null;
				}
				if(target instanceof L2MonsterInstance)
				{
					if(!skill.isOffensive() && skill.getSkillType() != L2SkillType.UNLOCK && skill.getSkillType() != L2SkillType.SUMMON && skill.getSkillType() != L2SkillType.DELUXE_KEY_UNLOCK && skill.getSkillType() != L2SkillType.BEAST_FEED)
					{
						L2PcInstance activeChar = null;

						if(this instanceof L2PcInstance)
						{
							activeChar = (L2PcInstance) this;
							activeChar.updatePvPStatus(activeChar);
						}
						else if(this instanceof L2Summon)
						{
							activeChar = ((L2Summon) this).getOwner();
						}
					}
				}
			}
			
			ISkillHandler handler = null;

			if(skill.isToggle())
			{
				if(getFirstEffect(skill.getId()) != null)
				{
					handler = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());

					if(handler != null)
					{
						handler.useSkill(this, skill, targets);
					}
					else
					{
						skill.useSkill(this, targets);
					}

					return;
				}
			}
			
			if(skill.isOverhit())
			{
				for(L2Object target : targets)
				{
					L2Character player = (L2Character) target;
					if(player instanceof L2Attackable)
					{
						((L2Attackable) player).overhitEnabled(true);
					}

					player = null;
				}
			}

			handler = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());

			if(handler != null)
			{
				handler.useSkill(this, skill, targets);
			}
			else
			{
				skill.useSkill(this, targets);
			}
			
			if(this instanceof L2PcInstance || this instanceof L2Summon)
			{
				L2PcInstance caster = this instanceof L2PcInstance ? (L2PcInstance) this : ((L2Summon) this).getOwner();
				for(L2Object target : targets)
				{
					if(target instanceof L2Npc)
					{
						L2Npc npc = (L2Npc) target;

						if(npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_SKILL_USE) != null)
						{
							for(Quest quest : npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_SKILL_USE))
							{
								quest.notifySkillUse(npc, caster, skill);
							}
						}

						npc = null;
					}
				}
				
				if(skill.getAggroPoints() > 0)
				{
					for(L2Object spMob : caster.getKnownList().getKnownObjects().values())
					{
						if(spMob instanceof L2Npc)
						{
							L2Npc npcMob = (L2Npc) spMob;

							if(npcMob.isInsideRadius(caster, 1000, true, true) && npcMob.hasAI() && npcMob.getAI().getIntention() == AI_INTENTION_ATTACK)
							{
								L2Object npcTarget = npcMob.getTarget();

								for(L2Object target : targets)
								{
									if(npcTarget == target || npcMob == target)
									{
										npcMob.seeSpell(caster, target, skill);
									}
								}

								npcTarget = null;
							}

							npcMob = null;
						}
					}
				}

				caster = null;
			}

			handler = null;
		}
		catch(Exception e)
		{
			_log.log(Level.WARNING, "", e);
		}
	}

	public void seeSpell(L2PcInstance caster, L2Object target, L2Skill skill)
	{
		if(this instanceof L2Attackable)
		{
			((L2Attackable) this).addDamageHate(caster, 0, -skill.getAggroPoints());
		}
	}

	public boolean isBehind(L2Object target)
    {
        if(target != null && target instanceof L2Character)
        {
            int head = getHeadingTo(target, true);
            return head != -1 && (head <= 10430 || head >= 55105);
        }
        return false;
    }
	
	public static final double HEADINGS_IN_PI = 10430.378350470452724949566316381;

	public int getHeadingTo(L2Object target, boolean toChar)
	{
		if(target == null || target == this)
			return -1;
		int dx = target.getX() - getX();
		int dy = target.getY() - getY();
		int heading = (int) (Math.atan2(-dy, -dx) * HEADINGS_IN_PI + 32768);
		heading = toChar ? target.getPosition().getHeading() - heading : getPosition().getHeading() - heading;
		if(heading < 0)
			heading = heading + 1 + Integer.MAX_VALUE & 0xFFFF;
		else if(heading > 0xFFFF)
			heading &= 0xFFFF;
		return heading;
	}
	
	/**public boolean isBehind(L2Object target)
	{
        double angleChar, angleTarget, angleDiff, maxAngleDiff = 45;

        if(target == null)
			return false;

		if (target instanceof L2Character)
		{
			L2Character target1 = (L2Character) target;
            angleChar = Util.calculateAngleFrom(this, target1);
            angleTarget = Util.convertHeadingToDegree(target1.getHeading());
            angleDiff = angleChar - angleTarget;
            if (angleDiff <= -360 + maxAngleDiff) angleDiff += 360;
            if (angleDiff >= 360 - maxAngleDiff) angleDiff -= 360;
            if (Math.abs(angleDiff) <= maxAngleDiff)
            {
                return true;
            }
		}
		else
		{
			_log.fine("isBehindTarget's target not an L2 Character.");
		}
		return false;
	}*/

	public boolean isBehindTarget()
	{
		return isBehind(getTarget());
	}

	public boolean isFront(L2Object target)
	{
		double angleChar, angleTarget, angleDiff, maxAngleDiff = 45;

		if(target == null)
		{
			return false;
		}

		if(target instanceof L2Character)
		{
			L2Character target1 = (L2Character) target;
			angleChar = Util.calculateAngleFrom(target1, this);
			angleTarget = Util.convertHeadingToDegree(target1.getHeading());
			angleDiff = angleChar - angleTarget;

			if(angleDiff <= -180 + maxAngleDiff)
			{
				angleDiff += 180;
			}

			if(angleDiff >= 180 - maxAngleDiff)
			{
				angleDiff -= 180;
			}

			if(Math.abs(angleDiff) <= maxAngleDiff)
			{
				return true;
			}

			target1 = null;
		}
		else
		{
			_log.fine("isSideTarget's target not an L2 Character.");
		}
		return false;
	}

	public boolean isFrontTarget()
	{
		return isFront(getTarget());
	}

	public double getLevelMod()
	{
		return 1;
	}

	public final void setSkillCast(Future<?> newSkillCast)
	{
		_skillCast = newSkillCast;
	}

	
	public final void forceIsCasting(int newSkillCastEndTick)
	{
		setIsCastingNow(true);
		_castInterruptTime = newSkillCastEndTick - 4;
	}

	/** The _ pvp reg task. */
	private Future<?> _PvPRegTask;

	/** The _pvp flag lasts. */
	private long _pvpFlagLasts;

	/**
	 * Sets the pvp flag lasts.
	 *
	 * @param time the new pvp flag lasts
	 */
	public void setPvpFlagLasts(long time)
	{
		_pvpFlagLasts = time;
	}

	/**
	 * Gets the pvp flag lasts.
	 *
	 * @return the pvp flag lasts
	 */
	public long getPvpFlagLasts()
	{
		return _pvpFlagLasts;
	}

	/**
	 * Start pvp flag.
	 */
	public void startPvPFlag()
	{
		updatePvPFlag(1);

		_PvPRegTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new PvPFlag(), 1000, 1000);
	}

	/**
	 * Stop pvp reg task.
	 */
	public void stopPvpRegTask()
	{
		if(_PvPRegTask != null)
		{
			_PvPRegTask.cancel(true);
		}
	}

	/**
	 * Stop pvp flag.
	 */
	public void stopPvPFlag()
	{
		stopPvpRegTask();

		updatePvPFlag(0);

		_PvPRegTask = null;
	}

	public void updatePvPFlag(int value)
	{
    }

	public final int getRandomDamage(L2Character target)
	{
		L2Weapon weaponItem = getActiveWeaponItem();

		if(weaponItem == null)
		{
			return 5 + (int) Math.sqrt(getLevel());
		}

		return weaponItem.getRandomDamage();
	}

	@Override
	public String toString()
	{
		return "mob " + getObjectId();
	}

	public int getAttackEndTime()
	{
		return _attackEndTime;
	}

	public abstract int getLevel();

	public final double calcStat(Stats stat, double init, L2Character target, L2Skill skill)
	{
		return getStat().calcStat(stat, init, target, skill);
	}

	public int getAccuracy()
	{
		return getStat().getAccuracy();
	}

	public final float getAttackSpeedMultiplier()
	{
		return getStat().getAttackSpeedMultiplier();
	}

	public int getCON()
	{
		return getStat().getCON();
	}

	public int getDEX()
	{
		return getStat().getDEX();
	}

	public final double getCriticalDmg(L2Character target, double init)
	{
		return getStat().getCriticalDmg(target, init);
	}

	public int getCriticalHit(L2Character target, L2Skill skill)
	{
		return getStat().getCriticalHit(target, skill);
	}

	public int getEvasionRate(L2Character target)
	{
		return getStat().getEvasionRate(target);
	}

	public int getINT()
	{
		return getStat().getINT();
	}

	public final int getMagicalAttackRange(L2Skill skill)
	{
		return getStat().getMagicalAttackRange(skill);
	}

	public final int getMaxCp()
	{
		return getStat().getMaxCp();
	}

	public int getMAtk(L2Character target, L2Skill skill)
	{
		return getStat().getMAtk(target, skill);
	}

	public int getMAtkSpd()
	{
		return getStat().getMAtkSpd();
	}

	public int getMaxMp()
	{
		return getStat().getMaxMp();
	}

	public int getMaxHp()
	{
		return getStat().getMaxHp();
	}

	public final int getMCriticalHit(L2Character target, L2Skill skill)
	{
		return getStat().getMCriticalHit(target, skill);
	}

	public int getMDef(L2Character target, L2Skill skill)
	{
		return getStat().getMDef(target, skill);
	}

	public int getMEN()
	{
		return getStat().getMEN();
	}

	public double getMReuseRate(L2Skill skill)
	{
		return getStat().getMReuseRate(skill);
	}

	public float getMovementSpeedMultiplier()
	{
		return getStat().getMovementSpeedMultiplier();
	}

	public int getPAtk(L2Character target)
	{
		return getStat().getPAtk(target);
	}

	public double getPAtkAnimals(L2Character target)
	{
		return getStat().getPAtkAnimals(target);
	}

	public double getPAtkDragons(L2Character target)
	{
		return getStat().getPAtkDragons(target);
	}

	public double getPAtkInsects(L2Character target)
	{
		return getStat().getPAtkInsects(target);
	}

	public double getPAtkMonsters(L2Character target)
	{
		return getStat().getPAtkMonsters(target);
	}

	public double getPAtkPlants(L2Character target)
	{
		return getStat().getPAtkPlants(target);
	}

	public int getPAtkSpd()
	{
		return getStat().getPAtkSpd();
	}

	public double getPAtkUndead(L2Character target)
	{
		return getStat().getPAtkUndead(target);
	}

	public double getPDefUndead(L2Character target)
	{
		return getStat().getPDefUndead(target);
	}

	public double getPDefPlants(L2Character target)
	{
		return getStat().getPDefPlants(target);
	}

	public double getPDefInsects(L2Character target)
	{
		return getStat().getPDefInsects(target);
	}

	public double getPDefAnimals(L2Character target)
	{
		return getStat().getPDefAnimals(target);
	}

	public double getPDefMonsters(L2Character target)
	{
		return getStat().getPDefMonsters(target);
	}

	public double getPDefDragons(L2Character target)
	{
		return getStat().getPDefDragons(target);
	}

	public int getPDef(L2Character target)
	{
		return getStat().getPDef(target);
	}

	public final int getPhysicalAttackRange()
	{
		return getStat().getPhysicalAttackRange();
	}

	public int getRunSpeed()
	{
		return getStat().getRunSpeed();
	}

	public final int getShldDef()
	{
		return getStat().getShldDef();
	}

	public int getSTR()
	{
		return getStat().getSTR();
	}

	public final int getWalkSpeed()
	{
		return getStat().getWalkSpeed();
	}

	public int getWIT()
	{
		return getStat().getWIT();
	}

	public void addStatusListener(L2Character object)
	{
		getStatus().addStatusListener(object);
	}

	public void reduceCurrentHp(double i, L2Character attacker)
	{
		reduceCurrentHp(i, attacker, true);
	}

	public void reduceCurrentHp(double i, L2Character attacker, boolean awake)
	{		
		if(Config.CHAMPION_ENABLE && isChampion() && Config.CHAMPION_HP != 0)
		{
			getStatus().reduceHp(i / Config.CHAMPION_HP, attacker, awake);
		}
		else
		{
			getStatus().reduceHp(i, attacker, awake);
		}
	}

	public void reduceCurrentMp(double i)
	{
		getStatus().reduceMp(i);
	}

	public void removeStatusListener(L2Character object)
	{
		getStatus().removeStatusListener(object);
	}

	protected void stopHpMpRegeneration()
	{
		getStatus().stopHpMpRegeneration();
	}

	public final double getCurrentCp()
	{
		return getStatus().getCurrentCp();
	}

	public final void setCurrentCp(Double newCp)
	{
		setCurrentCp((double) newCp);
	}

	public final void setCurrentCp(double newCp)
	{
		getStatus().setCurrentCp(newCp);
	}

	public final double getCurrentHp()
	{
		return getStatus().getCurrentHp();
	}

	public final void setCurrentHp(double newHp)
	{
		getStatus().setCurrentHp(newHp);
	}

	public final void setCurrentHpMp(double newHp, double newMp)
	{
		getStatus().setCurrentHpMp(newHp, newMp);
	}

	public final double getCurrentMp()
	{
		return getStatus().getCurrentMp();
	}

	public final void setCurrentMp(Double newMp)
	{
		setCurrentMp((double) newMp);
	}

	public final void setCurrentMp(double newMp)
	{
		getStatus().setCurrentMp(newMp);
	}

	public void setAiClass(String aiClass)
	{
		_aiClass = aiClass;
	}

	public String getAiClass()
	{
		return _aiClass;
	}

	public void setChampion(boolean champ)
	{
		_champion = champ;
	}

	public boolean isChampion()
	{
		return _champion;
	}

	public int getLastHealAmount()
	{
		return _lastHealAmount;
	}

	public void setLastHealAmount(int hp)
	{
		_lastHealAmount = hp;
	}

	public boolean reflectSkill(L2Skill skill)
	{
		double reflect = calcStat(skill.isMagic() ? Stats.REFLECT_SKILL_MAGIC : Stats.REFLECT_SKILL_PHYSIC, 0, null, null);

		if(Rnd.get(100) < reflect)
		{
			if (this instanceof L2PcInstance) 
				((L2PcInstance)this).sendMessage("You reflected " + skill.getName() + "!"); 
			else if (this instanceof L2Summon) 
				((L2Summon)this).getOwner().sendMessage("Your summon reflected " + skill.getName() + "!"); 
			return true;
		}

		return false;
	}

	public boolean vengeanceSkill(L2Skill skill)
	{
		if(!skill.isMagic() && skill.getCastRange() <= 40)
		{
			final double venganceChance = calcStat(Stats.VENGEANCE_SKILL_PHYSICAL_DAMAGE, 0, null, skill);
			if(venganceChance > Rnd.get(100))
			{
				return true;
			}
		}

		return false;
	}

	public void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
	}

	public ForceBuff getForceBuff()
	{
		return _forceBuff;
	}

	public void setForceBuff(ForceBuff fb)
	{
		_forceBuff = fb;
	}

	public boolean isFearImmune()
	{
		return false;
	}

	public void restoreHPMP()
	{
		getStatus().setCurrentHpMp(getMaxHp(), getMaxMp());
	}

	public void restoreCP()
	{
		getStatus().setCurrentCp(getMaxCp());
	}

	public void block()
	{
		_blocked = true;
	}

	public void unblock()
	{
		_blocked = false;
	}

	public boolean isBlocked()
	{
		return _blocked;
	}

	public boolean isMeditated()
	{
		return _meditated;
	}

	public void setMeditated(boolean meditated)
	{
		_meditated = meditated;
	}

	public void disableCoreAI(boolean val)
	{
	}

	public Inventory getInventory()
	{
		return null;
	}

	public boolean destroyItemByItemId(String process, int itemId, int count, L2Object reference, boolean sendMessage)
	{
		return true;
	}
	
	public int getMinShopDistance()
	{
		return 0;
	}

	public final void setIsBuffProtected(boolean value)
	{
		_isBuffProtected = value;
	}

	public boolean isBuffProtected()
	{
		return _isBuffProtected;
	}

	public void setIsRaidMinion(boolean val)
	{
		
	}

	public void sendMessage(String message)
	{
		sendPacket(SystemMessage.sendString(message));
	}

	private boolean _petrified = false;

	/**
	 * @return the petrified
	 */
	public boolean isPetrified()
	{
		return _petrified;
	}

	/**
	 * @param petrified the petrified to set
	 */
	public void setPetrified(boolean petrified)
	{
		if(petrified)
		{
			setIsParalyzed(petrified);
			setIsInvul(petrified);
			_petrified = petrified;
		}
		else
		{
			_petrified = petrified;
			setIsParalyzed(petrified);
			setIsInvul(petrified);
		}
	}

	public boolean isInFrontOf(L2Character target)
	{
		double angleChar, angleTarget, angleDiff, maxAngleDiff = 60;
		if (target == null)
			return false;
		
		angleTarget = Util.calculateAngleFrom(target, this);
		angleChar = Util.convertHeadingToDegree(target.getHeading());
		angleDiff = angleChar - angleTarget;
		
		if (angleDiff <= -360 + maxAngleDiff)
			angleDiff += 360;
		
		if (angleDiff >= 360 - maxAngleDiff)
			angleDiff -= 360;
		
		if (Math.abs(angleDiff) <= maxAngleDiff)
			return true;
		
		return false;
	}
	
	public boolean checkBss(){

        boolean bss = false;

        L2ItemInstance weaponInst = this.getActiveWeaponInstance();

        if (weaponInst != null)
        {
            if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
            {
                bss = true;
                //ponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
            }

        }
        // If there is no weapon equipped, check for an active summon.
        else if (this instanceof L2Summon)
        {
            L2Summon activeSummon = (L2Summon)this;

            if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
            {
                bss = true;
                //activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
            }

        }

        return bss;
    }

    public void removeBss(){

        L2ItemInstance weaponInst = this.getActiveWeaponInstance();

        if (weaponInst != null)
        {
            if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
            {
                weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
            }

        }
        // If there is no weapon equipped, check for an active summon.
        else if (this instanceof L2Summon)
        {
            L2Summon activeSummon = (L2Summon)this;

            if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT)
            {
                activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
            }

        }

    }

    public boolean checkSps(){

        boolean ss = false;

        L2ItemInstance weaponInst = this.getActiveWeaponInstance();

        if (weaponInst != null)
        {
            if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT)
            {
                ss = true;
                //weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
            }
        }
        // If there is no weapon equipped, check for an active summon.
        else if (this instanceof L2Summon)
        {
            L2Summon activeSummon = (L2Summon)this;

            if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_SPIRITSHOT)
            {
                ss = true;
                //activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
            }
        }

        return ss;

    }

    public void removeSps(){

        L2ItemInstance weaponInst = this.getActiveWeaponInstance();

        if (weaponInst != null)
        {
            if (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT)
            {
                weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
            }
        }
        // If there is no weapon equipped, check for an active summon.
        else if (this instanceof L2Summon)
        {
            L2Summon activeSummon = (L2Summon)this;

            if (activeSummon.getChargedSpiritShot() == L2ItemInstance.CHARGED_SPIRITSHOT)
            {
                activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
            }
        }

    }

    public boolean checkSs(){

        boolean ss = false;

        L2ItemInstance weaponInst = this.getActiveWeaponInstance();

        if (weaponInst != null)
        {
            if (weaponInst.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT)
            {
                ss = true;
                //weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
            }
        }
        // If there is no weapon equipped, check for an active summon.
        else if (this instanceof L2Summon)
        {
            L2Summon activeSummon = (L2Summon)this;

            if (activeSummon.getChargedSoulShot() == L2ItemInstance.CHARGED_SOULSHOT)
            {
                ss = true;
                //activeSummon.setChargedSpiritShot(L2ItemInstance.CHARGED_NONE);
            }
        }

        return ss;

    }

    public void removeSs(){

        L2ItemInstance weaponInst = this.getActiveWeaponInstance();

        if (weaponInst != null)
        {
            if (weaponInst.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT)
            {
                weaponInst.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
            }
        }
        // If there is no weapon equipped, check for an active summon.
        else if (this instanceof L2Summon)
        {
            L2Summon activeSummon = (L2Summon)this;

            if (activeSummon.getChargedSoulShot() == L2ItemInstance.CHARGED_SOULSHOT)
            {
                activeSummon.setChargedSoulShot(L2ItemInstance.CHARGED_NONE);
            }
        }

    }
    
	private class UsePotionTask implements Runnable
	{
		private L2Character _activeChar;
		private L2Skill _skill;
		
		UsePotionTask(L2Character activeChar, L2Skill skill)
		{
			_activeChar = activeChar;
			_skill = skill;
		}
		
		@Override
		public void run()
		{
			try
			{
				_activeChar.doSimultaneousCast(_skill);
			}
			catch (Throwable t)
			{
				_log.log(Level.WARNING, "", t);
			}
		}
	}
}