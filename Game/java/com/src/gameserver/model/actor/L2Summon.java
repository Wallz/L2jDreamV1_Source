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

import com.src.Config;
import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.ai.L2CharacterAI;
import com.src.gameserver.ai.L2SummonAI;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.geo.GeoData;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Party;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.L2Skill.SkillTargetType;
import com.src.gameserver.model.actor.instance.L2DoorInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.knownlist.SummonKnownList;
import com.src.gameserver.model.actor.stat.SummonStat;
import com.src.gameserver.model.actor.status.SummonStatus;
import com.src.gameserver.model.base.Experience;
import com.src.gameserver.model.itemcontainer.PetInventory;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.MyTargetSelected;
import com.src.gameserver.network.serverpackets.NpcInfo;
import com.src.gameserver.network.serverpackets.PetDelete;
import com.src.gameserver.network.serverpackets.PetInfo;
import com.src.gameserver.network.serverpackets.PetStatusShow;
import com.src.gameserver.network.serverpackets.PetStatusUpdate;
import com.src.gameserver.network.serverpackets.RelationChanged;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.taskmanager.DecayTaskManager;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.templates.item.L2Weapon;

public abstract class L2Summon extends L2Playable
{
	protected int _pkKills;
	private L2PcInstance _owner;
	private int _attackRange = 36;
	private boolean _follow = true;
	private boolean _previousFollowStatus = true;
	private int _maxLoad;
	private int _chargedSoulShot;
	private int _chargedSpiritShot;
	private int _soulShotsPerHit = 1;
	private int _spiritShotsPerHit = 1;
	protected boolean _showSummonAnimation;

	public class AIAccessor extends L2Character.AIAccessor
	{
		protected AIAccessor()
		{
		}

		public L2Summon getSummon()
		{
			return L2Summon.this;
		}

		public boolean isAutoFollow()
		{
			return getFollowStatus();
		}

		public void doPickupItem(L2Object object)
		{
			L2Summon.this.doPickupItem(object);
		}
	}

	public L2Summon(int objectId, L2NpcTemplate template, L2PcInstance owner)
	{
		super(objectId, template);
		getKnownList();
		getStat();
		getStatus();

		_showSummonAnimation = true;
		_owner = owner;
		_ai = new L2SummonAI(new L2Summon.AIAccessor());

		setXYZInvisible(owner.getX() + 50, owner.getY() + 100, owner.getZ() + 100);
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		this.setFollowStatus(true);
		this.setShowSummonAnimation(false);
		
		this.getOwner().sendPacket(new PetInfo(this));
		
		getOwner().sendPacket(new RelationChanged(this, getOwner().getRelation(getOwner()), false));
		
		broadcastRelationsChanges();
	}
	
	public void broadcastRelationsChanges()
	{
		for (L2PcInstance player : getOwner().getKnownList().getKnownPlayersInRadius(800))
		{
			if (player != null)
				player.sendPacket(new RelationChanged(this, getOwner().getRelation(player), isAutoAttackable(player)));
		}
	}
	
	@Override
	public final SummonKnownList getKnownList()
	{
		if(super.getKnownList() == null || !(super.getKnownList() instanceof SummonKnownList))
		{
			setKnownList(new SummonKnownList(this));
		}

		return (SummonKnownList) super.getKnownList();
	}

	@Override
	public SummonStat getStat()
	{
		if(super.getStat() == null || !(super.getStat() instanceof SummonStat))
		{
			setStat(new SummonStat(this));
		}

		return (SummonStat) super.getStat();
	}

	@Override
	public SummonStatus getStatus()
	{
		if(super.getStatus() == null || !(super.getStatus() instanceof SummonStatus))
		{
			setStatus(new SummonStatus(this));
		}

		return (SummonStatus) super.getStatus();
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
					_ai = new L2SummonAI(new L2Summon.AIAccessor());
				}
			}
		}

		return _ai;
	}

	@Override
	public L2NpcTemplate getTemplate()
	{
		return (L2NpcTemplate) super.getTemplate();
	}

	public abstract int getSummonType();

	@Override
	public void updateAbnormalEffect()
	{
		for(L2PcInstance player : getKnownList().getKnownPlayers().values())
		{
			player.sendPacket(new NpcInfo(this, player));
		}
	}

	public boolean isMountable()
	{
		return false;
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if(player == _owner && player.getTarget() == this)
		{
			player.sendPacket(new PetStatusShow(this));
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else if(player.getTarget() != this)
		{
			player.setTarget(this);
			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);

			StatusUpdate su = new StatusUpdate(getObjectId());
			su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
			su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
			su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
			su.addAttribute(StatusUpdate.MAX_MP, getMaxMp());
			player.sendPacket(su);
		}
		else if(player.getTarget() == this)
		{
			if(isAutoAttackable(player))
			{
				if(Config.GEODATA > 0)
				{
					if(GeoData.getInstance().canSeeTarget(player, this))
					{
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
						player.onActionRequest();
					}
				}
				else
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
					player.onActionRequest();
				}
			}
			else
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);

				if(Config.GEODATA > 0)
				{
					if(GeoData.getInstance().canSeeTarget(player, this))
					{
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
					}
				}
				else
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
				}
			}
		}
	}

	public long getExpForThisLevel()
	{
		if(getLevel() >= Experience.MAX_LEVEL)
		{
			return 0;
		}

		return Experience.getExp(getLevel());
	}

	public long getExpForNextLevel()
	{
		if(getLevel() >= Experience.MAX_LEVEL - 1)
		{
			return 0;
		}

		return Experience.getExp(getLevel() + 1);
	}

	public final int getKarma()
	{
		return getOwner()!= null ? getOwner().getKarma() : 0;
	}

	public final L2PcInstance getOwner()
	{
		return _owner;
	}

	public final int getNpcId()
	{
		return getTemplate().npcId;
	}

	@Override
	protected void doAttack(L2Character target)
	{
		if(getOwner() != null && getOwner() == target && !getOwner().isBetrayed())
		{
			sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
			return;
		}

		if(isInsidePeaceZone(this, target))
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			return;
		}

		if(!target.isAttackable())
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			return;
		}

		super.doAttack(target);
	}
	
	public byte getPvpFlag()
	{
		return getOwner()!= null ? getOwner().getPvpFlag() : 0;
	}

	public void setPkKills(int pkKills)
	{
		_pkKills = pkKills;
	}

	public final int getPkKills()
	{
		return _pkKills;
	}

	public final int getMaxLoad()
	{
		return _maxLoad;
	}

	public void setMaxLoad(int maxLoad)
	{
		_maxLoad = maxLoad;
	}

	public final int getSoulShotsPerHit()
	{
		return _soulShotsPerHit;
	}

	public final int getSpiritShotsPerHit()
	{
		return _spiritShotsPerHit;
	}

	public final void increaseUsedSoulShots(int numShots)  
	{  
		_soulShotsPerHit += numShots;  
	}  
     
	public final void increaseUsedSpiritShots(int numShots)  
	{  
		_spiritShotsPerHit += numShots; 
	}
 	
	public void setChargedSoulShot(int shotType)
	{
		_chargedSoulShot = shotType;
	}

	public void setChargedSpiritShot(int shotType)
	{
		_chargedSpiritShot = shotType;
	}

	public void followOwner()
	{
		setFollowStatus(true);
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if(!super.doDie(killer))
		{
			return false;
		}
		
		DecayTaskManager.getInstance().addDecayTask(this);
		return true;
	}

	public boolean doDie(L2Character killer, boolean decayed)
	{
		if(!super.doDie(killer))
		{
			return false;
		}

		if(!decayed)
		{
			DecayTaskManager.getInstance().addDecayTask(this);
		}

		return true;
	}

	public void stopDecay()
	{
		DecayTaskManager.getInstance().cancelDecayTask(this);
	}

	@Override
	public void onDecay()
	{
		deleteMe(_owner);
	}

	@Override
	public void broadcastStatusUpdate()
	{
		super.broadcastStatusUpdate();

		if(getOwner() != null && isVisible())
		{
			getOwner().sendPacket(new PetStatusUpdate(this));
		}
	}

	public void deleteMe(L2PcInstance owner)
	{
		stopAllEffects();
		getAI().stopFollow();
		abortCast();
		owner.sendPacket(new PetDelete(getObjectId(), 2));
		giveAllToOwner();
		decayMe();
		getKnownList().removeAllKnownObjects();
		owner.setPet(null);
	}

	public synchronized void unSummon(L2PcInstance owner)
	{
		if(isVisible() && !isDead())
		{
			stopAllEffects();
			getAI().stopFollow();
			abortCast();
			owner.sendPacket(new PetDelete(getObjectId(), 2));

			if(getWorldRegion() != null)
			{
				getWorldRegion().removeFromZones(this);
			}

			store();

			giveAllToOwner();
			decayMe();
			getKnownList().removeAllKnownObjects();
			owner.setPet(null);
			setTarget(null);
			for (int itemId : owner.getAutoSoulShot().keySet()) 
			{ 
				String handler = owner.getInventory().getItemByItemId(itemId).getEtcItem().getName(); 
				if (handler.contains("Beast")) 
					owner.disableAutoShot(itemId); 
             }
		}
	}

	public int getAttackRange()
	{
		return _attackRange;
	}

	public void setAttackRange(int range)
	{
		if(range < 36)
		{
			range = 36;
		}
		_attackRange = range;
	}

	public void setFollowStatus(boolean state)
	{
		_follow = state;

		if(_follow)
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, getOwner());
		}
		else
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null);
		}
	}

	public boolean getFollowStatus()
	{
		return _follow;
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return _owner.isAutoAttackable(attacker);
	}

	public int getChargedSoulShot()
	{
		return _chargedSoulShot;
	}

	public int getChargedSpiritShot()
	{
		return _chargedSpiritShot;
	}

	public int getControlItemId()
	{
		return 0;
	}

	public L2Weapon getActiveWeapon()
	{
		return null;
	}

	@Override
	public PetInventory getInventory()
	{
		return null;
	}

	protected void doPickupItem(L2Object object)
	{
		return;
	}

	public void giveAllToOwner()
	{
		return;
	}

	public void store()
	{
		return;
	}

	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getActiveWeaponItem()
	{
		return null;
	}

	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		return null;
	}

	@Override
	public boolean isInvul()
	{
		return _isInvul  || _isTeleporting ||  getOwner().isSpawnProtected();
	}

	@Override
	public L2Party getParty()
	{
		if(_owner == null)
		{
			return null;
		}
		else
		{
			return _owner.getParty();
		}
	}

	@Override
	public boolean isInParty()
	{
		if(_owner == null)
		{
			return false;
		}
		else
		{
			return _owner.getParty() != null;
		}
	}

	public void useMagic(L2Skill skill, boolean forceUse, boolean dontMove)
	{
		if(skill == null || isDead())
		{
			return;
		}

		if(skill.isPassive())
		{
			return;
		}

		if(isCastingNow())
		{
			return;
		}
		
		getOwner().setCurrentSkill(skill, forceUse, dontMove);

		L2Object target = null;

		switch(skill.getTargetType())
		{
			case TARGET_OWNER_PET:
				target = getOwner();
				break;
			case TARGET_PARTY:
			case TARGET_AURA:
			case TARGET_SELF:
				target = this;
				break;
			default:
				target = skill.getFirstOfTargetList(this);
				break;
		}

		if (target == null)
		{
			if (getOwner() != null)
				getOwner().sendPacket(SystemMessageId.TARGET_CANT_FOUND);
			return;
		}

		if(isSkillDisabled(skill.getId()) && getOwner() != null)
		{
			getOwner().sendPacket(new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE).addString(skill.getName()));
			return;
		}

		if(isAllSkillsDisabled() && getOwner() != null)
		{
			return;
		}

		if(getCurrentMp() < getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill))
		{
			if(getOwner() != null)
			{
				getOwner().sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_MP));
			}

			return;
		}

		if(getCurrentHp() <= skill.getHpConsume())
		{
			if(getOwner() != null)
			{
				getOwner().sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_HP));
			}

			return;
		}

		// Check if the spell consummes an Item
		if(skill.getItemConsume() > 0)
		{
			if(getOwner() != null)
			{
				// Get the L2ItemInstance consummed by the spell
				L2ItemInstance requiredItems = getOwner().getInventory().getItemByItemId(skill.getItemConsumeId());

				// Check if the caster owns enought consummed Item to cast
				if(requiredItems == null || requiredItems.getCount() < skill.getItemConsume())
				{
					// Send a System Message to the caster
					getOwner().sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
			}
		}

		if(skill.isOffensive())
		{
			if(getOwner() != null && getOwner() == target && !getOwner().isBetrayed()) 
			{ 
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT)); 
				return; 
			} 

			if(isInsidePeaceZone(this, target) && getOwner() != null && !getOwner().getAccessLevel().allowPeaceAttack())
			{
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
				return;
			}

			if(getOwner() != null && getOwner().isInOlympiadMode() && !getOwner().isOlympiadStart())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			if(target instanceof L2DoorInstance)
			{
				if(!((L2DoorInstance) target).isAttackable(getOwner()))
				{
					return;
				}
			}
			else
			{
				if(!target.isAttackable() && getOwner() != null && getOwner().getAccessLevel().allowPeaceAttack())
				{
					return;
				}

				if(!target.isAutoAttackable(this) && !forceUse && skill.getTargetType() != SkillTargetType.TARGET_AURA && skill.getTargetType() != SkillTargetType.TARGET_CLAN && skill.getTargetType() != SkillTargetType.TARGET_ALLY && skill.getTargetType() != SkillTargetType.TARGET_PARTY && skill.getTargetType() != SkillTargetType.TARGET_SELF)
				{
					return;
				}
			}
		}

		getOwner().setCurrentPetSkill(skill, forceUse, dontMove);
		getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, skill, target);

		target = null;
		skill = null;
	}

	@Override
	public void setIsImobilised(boolean value)
	{
		super.setIsImobilised(value);

		if(value)
		{
			_previousFollowStatus = getFollowStatus();

			if(_previousFollowStatus)
			{
				setFollowStatus(false);
			}
		}
		else
		{
			setFollowStatus(_previousFollowStatus);
		}
	}

	public void setOwner(L2PcInstance newOwner)
	{
		_owner = newOwner;
	}

	@Override
	public boolean isShowSummonAnimation()
	{
		return _showSummonAnimation;
	}

	@Override
	public void setShowSummonAnimation(boolean showSummonAnimation)
	{
		_showSummonAnimation = showSummonAnimation;
	}

	@Override
	public void doCast(L2Skill skill)
	{
		int petLevel = getLevel();
		int skillLevel = petLevel / 10;

		if(petLevel >= 70)
		{
			skillLevel += (petLevel - 65) / 10;
		}

		if(skillLevel < 1)
		{
			skillLevel = 1;
		}

		L2Skill skillToCast = SkillTable.getInstance().getInfo(skill.getId(), skillLevel);

		if(skillToCast != null)
		{
			super.doCast(skillToCast);
		}
		else
		{
			super.doCast(skill);
		}

		skillToCast = null;
	}
}