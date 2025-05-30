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
package com.src.gameserver.model.actor.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.datatables.xml.L2PetDataTable;
import com.src.gameserver.idfactory.IdFactory;
import com.src.gameserver.managers.CursedWeaponsManager;
import com.src.gameserver.managers.ItemsOnGroundManager;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Party;
import com.src.gameserver.model.L2PetData;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.stat.PetStat;
import com.src.gameserver.model.itemcontainer.Inventory;
import com.src.gameserver.model.itemcontainer.PcInventory;
import com.src.gameserver.model.itemcontainer.PetInventory;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.network.serverpackets.MoveToPawn;
import com.src.gameserver.network.serverpackets.MyTargetSelected;
import com.src.gameserver.network.serverpackets.PetInventoryUpdate;
import com.src.gameserver.network.serverpackets.PetItemList;
import com.src.gameserver.network.serverpackets.PetStatusShow;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.StopMove;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.network.serverpackets.ValidateLocation;
import com.src.gameserver.taskmanager.DecayTaskManager;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.templates.item.L2Item;
import com.src.gameserver.templates.item.L2Weapon;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class L2PetInstance extends L2Summon
{
	private static final Log _log = LogFactory.getLog(L2PetInstance.class);

	private int _curFed;
	private PetInventory _inventory;
	private final int _controlItemId;
	private boolean _respawned;
	private boolean _mountable;

	private Future<?> _feedTask;
	private int _feedTime;
	protected boolean _feedMode;

	private L2PetData _data;

	private long _expBeforeDeath = 0;
	private static final int FOOD_ITEM_CONSUME_COUNT = 5;

	private static final int PET_DECAY_DELAY = 86400000; // 24 hours
	
	public final L2PetData getPetData()
	{
		if(_data == null)
		{
			_data = L2PetDataTable.getInstance().getPetData(getTemplate().npcId, getStat().getLevel());
		}

		return _data;
	}

	public final void setPetData(L2PetData value)
	{
		_data = value;
	}

	class FeedTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				if (getOwner() == null || getOwner().getPet() == null || getOwner().getPet().getObjectId() != getObjectId())
				{
					stopFeed();
					return;
				}
				
				if(isAttackingNow())
				{
					if(!_feedMode)
					{
						startFeed(true);
					}
					else if(_feedMode)
					{
						startFeed(false);
					}
				}

				if(getCurrentFed() > FOOD_ITEM_CONSUME_COUNT)
				{
					setCurrentFed(getCurrentFed() - FOOD_ITEM_CONSUME_COUNT);
				}
				else
				{
					setCurrentFed(0);
					stopFeed();
					unSummon(getOwner());
					getOwner().sendMessage("Your pet is too hungry to stay summoned.");
				}

				int foodId = L2PetDataTable.getFoodItemId(getTemplate().npcId);
				if(foodId == 0)
				{
					return;
				}

				L2ItemInstance food = null;
				food = getInventory().getItemByItemId(foodId);

				if(food != null && getCurrentFed() < 0.55 * getMaxFed())
				{
					if(destroyItem("Feed", food.getObjectId(), 1, null, false))
					{
						setCurrentFed(getCurrentFed() + 100);
						if(getOwner() != null)
						{
							getOwner().sendPacket(new SystemMessage(SystemMessageId.PET_TOOK_S1_BECAUSE_HE_WAS_HUNGRY).addItemName(foodId));
						}
					}
				}
				
				if (isRunning() && isHungry()) 
				 	setWalking(); 
				else if (!isHungry() && !isRunning()) 
				 	setRunning(); 

				broadcastStatusUpdate();
			}
			catch(Throwable e)
			{
				
			}
		}
	}

	public synchronized static L2PetInstance spawnPet(L2NpcTemplate template, L2PcInstance owner, L2ItemInstance control)
	{
		if(L2World.getInstance().getPet(owner.getObjectId()) != null)
		{
			return null;
		}

		L2PetInstance pet = restore(control, template, owner);
		if(pet != null)
		{
			pet.setTitle(owner.getName());
			L2World.getInstance().addPet(owner.getObjectId(), pet);
		}
		return pet;
	}

	public L2PetInstance(int objectId, L2NpcTemplate template, L2PcInstance owner, L2ItemInstance control)
	{
		super(objectId, template, owner);
		super.setStat(new PetStat(this));

		_controlItemId = control.getObjectId();

		if(template.npcId == 12564)
		{
			getStat().setLevel((byte) getOwner().getLevel());
		}
		else
		{
			getStat().setLevel(template.level);
		}

		_inventory = new PetInventory(this);
		_inventory.restore();

		int npcId = template.npcId;
		_mountable = L2PetDataTable.isMountable(npcId);
	}

	@Override
	public PetStat getStat()
	{
		if(super.getStat() == null || !(super.getStat() instanceof PetStat))
		{
			setStat(new PetStat(this));
		}

		return (PetStat) super.getStat();
	}

	@Override
	public double getLevelMod()
	{
		return (100.0 - 11 + getLevel()) / 100.0;
	}

	public boolean isRespawned()
	{
		return _respawned;
	}

	@Override
	public int getSummonType()
	{
		return 2;
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		boolean isOwner = player.getObjectId() == getOwner().getObjectId();
		if (isOwner && player != getOwner())
			updateRefOwner(player);
		
		if (player.getTarget() != this)
		{
			player.setTarget(this);
			player.sendPacket(new ValidateLocation(this));
			player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));
		}
		else if (isOwner && player.getTarget() == this)
		{
			// Calculate the distance between the L2PcInstance and the L2Npc
			if(!canInteract(player))
			{
				// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				player.sendPacket(new MoveToPawn(player, this, L2Npc.INTERACTION_DISTANCE));
				player.sendPacket(new PetStatusShow(this));
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
		}
	}
	
	@Override
	public int getControlItemId()
	{
		return _controlItemId;
	}

	public L2ItemInstance getControlItem()
	{
		return getOwner().getInventory().getItemByObjectId(_controlItemId);
	}

	public int getCurrentFed()
	{
		return _curFed;
	}

	public void setCurrentFed(int num)
	{
		_curFed = num > getMaxFed() ? getMaxFed() : num;
	}

	@Override
	public void setPkKills(int pkKills)
	{
		_pkKills = pkKills;
	}

	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		for(L2ItemInstance item : getInventory().getItems())
		{
			if(item.getLocation() == L2ItemInstance.ItemLocation.PET_EQUIP && item.getItem().getBodyPart() == L2Item.SLOT_R_HAND)
			{
				return item;
			}
		}
		return null;
	}

	@Override
	public L2Weapon getActiveWeaponItem()
	{
		L2ItemInstance weapon = getActiveWeaponInstance();

		if(weapon == null)
		{
			return null;
		}
		return (L2Weapon) weapon.getItem();
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
	public PetInventory getInventory()
	{
		return _inventory;
	}

	@Override
	public boolean destroyItem(String process, int objectId, int count, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance item = _inventory.destroyItem(process, objectId, count, getOwner(), reference);

		if(item == null)
		{
			if(sendMessage)
			{
				getOwner().sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}

			return false;
		}

		PetInventoryUpdate petIU = new PetInventoryUpdate();
		petIU.addItem(item);
		getOwner().sendPacket(petIU);

		if(sendMessage)
		{
			getOwner().sendPacket(new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addNumber(count).addItemName(item.getItemId()));
		}
		return true;
	}

	@Override
	public boolean destroyItemByItemId(String process, int itemId, int count, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance item = _inventory.destroyItemByItemId(process, itemId, count, getOwner(), reference);

		if(item == null)
		{
			if(sendMessage)
			{
				getOwner().sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}
			return false;
		}

		PetInventoryUpdate petIU = new PetInventoryUpdate();
		petIU.addItem(item);
		getOwner().sendPacket(petIU);

		if(sendMessage)
		{
			getOwner().sendPacket(new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addNumber(count).addItemName(itemId));
		}
		return true;
	}

	@Override
	protected void doPickupItem(L2Object object)
	{
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		StopMove sm = new StopMove(getObjectId(), getX(), getY(), getZ(), getHeading());

		broadcastPacket(sm);
		sm = null;

		if(!(object instanceof L2ItemInstance))
		{
			_log.warn("trying to pickup wrong target." + object);
			getOwner().sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		L2ItemInstance target = (L2ItemInstance) object;

		if(target.getItemId() > 8599 && target.getItemId() < 8615)
		{
			SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
			smsg.addItemName(target.getItemId());
			getOwner().sendPacket(smsg);
			smsg = null;
			return;
		}

		if(CursedWeaponsManager.getInstance().isCursed(target.getItemId()))
		{
			getOwner().sendPacket(new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1).addItemName(target.getItemId()));
			return;
		}

		synchronized (target)
		{
			if(!target.isVisible())
			{
				getOwner().sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			if (!target.getDropProtection().tryPickUp(this)) 
			{ 
				getOwner().sendPacket(ActionFailed.STATIC_PACKET); 
				SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1); 
				smsg.addItemName(target); 
				getOwner().sendPacket(smsg); 
				return; 
			}
			if(!_inventory.validateCapacity(target))
			{
				getOwner().sendMessage("Your pet can't carry any more items.");
				return;
			}

			if(target.getOwnerId() != 0 && target.getOwnerId() != getOwner().getObjectId() && !getOwner().isInLooterParty(target.getOwnerId()))
			{
				getOwner().sendPacket(ActionFailed.STATIC_PACKET);

				if(target.getItemId() == 57)
				{
					getOwner().sendPacket(new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1_ADENA).addNumber(target.getCount()));
				}
				else if(target.getCount() > 1)
				{
					getOwner().sendPacket(new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S2_S1_S).addItemName(target.getItemId()).addNumber(target.getCount()));
				}
				else
				{
					getOwner().sendPacket(new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1).addItemName(target.getItemId()));
				}

				return;
			}

			if(target.getItemLootShedule() != null && (target.getOwnerId() == getOwner().getObjectId() || getOwner().isInLooterParty(target.getOwnerId())))
			{
				target.resetOwnerTimer();
			}

			if (getOwner().isInParty() && getOwner().getParty().getLootDistribution() != L2Party.ITEM_LOOTER) 
				getOwner().getParty().distributeItem(getOwner(), target); 
			else 
				target.pickupMe(this);

			if(Config.SAVE_DROPPED_ITEM)
			{
				ItemsOnGroundManager.getInstance().removeObject(target);
			}
		}

		getInventory().addItem("Pickup", target, getOwner(), this);
		PetItemList iu = new PetItemList(this);
		getOwner().sendPacket(iu);
		iu = null;

		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);

		if(getFollowStatus())
		{
			followOwner();
		}
	}

	@Override
	public void deleteMe(L2PcInstance owner)
	{
		super.deleteMe(owner);
		destroyControlItem(owner);
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if(!super.doDie(killer, true))
		{
			return false;
		}
		stopFeed();
		DecayTaskManager.getInstance().addDecayTask(this, PET_DECAY_DELAY);
		L2PcInstance owner = getOwner();
		if(owner != null && !owner.isInDuel() && (!isInsideZone(ZONE_PVP) || isInsideZone(ZONE_SIEGE)))
		{
			deathPenalty();
		}
		return true;
	}

	@Override
	public void doRevive()
	{
		getOwner().removeReviving();

		super.doRevive();

		DecayTaskManager.getInstance().cancelDecayTask(this);
		startFeed(false);
		
		if (!isHungry()) 
		 	setRunning();   
		
		getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null); 
	}

	@Override
	public void doRevive(double revivePower)
	{
		restoreExp(revivePower);
		doRevive();
	}

	public L2ItemInstance transferItem(String process, int objectId, int count, Inventory target, L2PcInstance actor, L2Object reference)
	{
		L2ItemInstance oldItem = getInventory().getItemByObjectId(objectId);
		L2ItemInstance newItem = getInventory().transferItem(process, objectId, count, target, actor, reference);

		if(newItem == null)
		{
			return null;
		}

		PetInventoryUpdate petIU = new PetInventoryUpdate();
		if(oldItem.getCount() > 0 && oldItem != newItem)
		{
			petIU.addModifiedItem(oldItem);
		}
		else
		{
			petIU.addRemovedItem(oldItem);
		}

		getOwner().sendPacket(petIU);

		if(target instanceof PcInventory)
		{
			L2PcInstance targetPlayer = ((PcInventory) target).getOwner();
			InventoryUpdate playerUI = new InventoryUpdate();
			if(newItem.getCount() > count)
			{
				playerUI.addModifiedItem(newItem);
			}
			else
			{
				playerUI.addNewItem(newItem);
			}
			targetPlayer.sendPacket(playerUI);

			StatusUpdate playerSU = new StatusUpdate(targetPlayer.getObjectId());
			playerSU.addAttribute(StatusUpdate.CUR_LOAD, targetPlayer.getCurrentLoad());
			targetPlayer.sendPacket(playerSU);
		}
		else if(target instanceof PetInventory)
		{
			petIU = new PetInventoryUpdate();
			if(newItem.getCount() > count)
			{
				petIU.addRemovedItem(newItem);
			}
			else
			{
				petIU.addNewItem(newItem);
			}
			((PetInventory) target).getOwner().getOwner().sendPacket(petIU);
		}
		return newItem;
	}

	@Override
	public void giveAllToOwner()
	{
		try
		{
			Inventory petInventory = getInventory();
			L2ItemInstance[] items = petInventory.getItems();
			petInventory = null;
			for(int i = 0; i < items.length; i++)
			{
				L2ItemInstance giveit = items[i];
				if(giveit.getItem().getWeight() * giveit.getCount() + getOwner().getInventory().getTotalWeight() < getOwner().getMaxLoad())
				{
					giveItemToOwner(giveit);
				}
				else
				{
					dropItemHere(giveit);
				}
			}
		}
		catch(Exception e)
		{
			_log.error("Give all items error", e);
		}
	}

	public void giveItemToOwner(L2ItemInstance item)
	{
		try
		{
			getInventory().transferItem("PetTransfer", item.getObjectId(), item.getCount(), getOwner().getInventory(), getOwner(), this);
			PetInventoryUpdate petiu = new PetInventoryUpdate();
			ItemList PlayerUI = new ItemList(getOwner(), false);
			petiu.addRemovedItem(item);
			getOwner().sendPacket(petiu);
			getOwner().sendPacket(PlayerUI);
		}
		catch(Exception e)
		{
			_log.error("Error while giving item to owner", e);
		}
	}

	public void destroyControlItem(L2PcInstance owner)
	{
		L2World.getInstance().removePet(owner.getObjectId());

		try
		{
			L2ItemInstance removedItem = owner.getInventory().destroyItem("PetDestroy", getControlItemId(), 1, getOwner(), this);

			InventoryUpdate iu = new InventoryUpdate();
			iu.addRemovedItem(removedItem);
			owner.sendPacket(iu);

			StatusUpdate su = new StatusUpdate(owner.getObjectId());
			su.addAttribute(StatusUpdate.CUR_LOAD, owner.getCurrentLoad());
			owner.sendPacket(su);

			owner.broadcastUserInfo();

			L2World world = L2World.getInstance();
			world.removeObject(removedItem);

			removedItem = null;
			world = null;
		}
		catch(Exception e)
		{
			_log.error("Error while destroying control item", e);
		}

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id = ?");
			statement.setInt(1, getControlItemId());
			statement.execute();
			statement.close();
			statement = null;
		}
		catch(Exception e)
		{
			_log.error("could not delete pet", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	public void dropAllItems()
	{
		try
		{
			L2ItemInstance[] items = getInventory().getItems();
			for(int i = 0; i < items.length; i++)
			{
				dropItemHere(items[i]);
			}
		}
		catch(Exception e)
		{
			_log.error("Pet Drop Error", e);
		}
	}

	public void dropItemHere(L2ItemInstance dropit, boolean protect)
	{
		dropit = getInventory().dropItem("Drop", dropit.getObjectId(), dropit.getCount(), getOwner(), this);

		if(dropit != null)
		{
			if(protect)
			{
				dropit.getDropProtection().protect(getOwner());
			}

			_log.trace("Item id to drop: " + dropit.getItemId() + " amount: " + dropit.getCount());
			dropit.dropMe(this, getX(), getY(), getZ() + 100);
		}
	}

	public void dropItemHere(L2ItemInstance dropit)
	{
		dropItemHere(dropit, false);
	}

	@Override
	public boolean isMountable()
	{
		return _mountable;
	}

	private static L2PetInstance restore(L2ItemInstance control, L2NpcTemplate template, L2PcInstance owner)
	{
		Connection con = null;
		try
		{
			L2PetInstance pet;
			if(template.type.compareToIgnoreCase("L2BabyPet") == 0)
			{
				pet = new L2BabyPetInstance(IdFactory.getInstance().getNextId(), template, owner, control);
			}
			else
			{
				pet = new L2PetInstance(IdFactory.getInstance().getNextId(), template, owner, control);
			}

			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT item_obj_id, name, level, curHp, curMp, exp, sp, karma, pkkills, fed FROM pets WHERE item_obj_id = ?");
			statement.setInt(1, control.getObjectId());
			ResultSet rset = statement.executeQuery();
			if(!rset.next())
			{
				rset.close();
				statement.close();
				rset = null;
				statement = null;

				return pet;
			}

			pet._respawned = true;
			pet.setName(rset.getString("name"));

			pet.getStat().setLevel(rset.getByte("level"));
			pet.getStat().setExp(rset.getLong("exp"));
			pet.getStat().setSp(rset.getInt("sp"));

			pet.getStatus().setCurrentHp(rset.getDouble("curHp"));
			pet.getStatus().setCurrentMp(rset.getDouble("curMp"));
			pet.getStatus().setCurrentCp(pet.getMaxCp());

			//pet.setKarma(rset.getInt("karma"));
			pet.setPkKills(rset.getInt("pkkills"));
			pet.setCurrentFed(rset.getInt("fed"));

			rset.close();
			statement.close();
			rset = null;
			statement = null;

			return pet;
		}
		catch(Exception e)
		{
			_log.error("could not restore pet data", e);
			return null;
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	@Override
	public void store()
	{
		if(getControlItemId() == 0)
		{
			return;
		}

		String req;
		if(!isRespawned())
		{
			req = "INSERT INTO pets (name, level, curHp, curMp, exp, sp, karma, pkkills, fed, item_obj_id) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		}
		else
		{
			req = "UPDATE pets SET name = ?, level = ?, curHp = ?, curMp = ?, exp = ?, sp = ?, karma = ?, pkkills = ?, fed = ? " + "WHERE item_obj_id = ?";
		}
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(req);
			statement.setString(1, getName());
			statement.setInt(2, getStat().getLevel());
			statement.setDouble(3, getStatus().getCurrentHp());
			statement.setDouble(4, getStatus().getCurrentMp());
			statement.setLong(5, getStat().getExp());
			statement.setInt(6, getStat().getSp());
			statement.setInt(7, getKarma());
			statement.setInt(8, getPkKills());
			statement.setInt(9, getCurrentFed());
			statement.setInt(10, getControlItemId());
			statement.executeUpdate();
			statement.close();
			statement = null;
			_respawned = true;
		}
		catch(Exception e)
		{
			_log.error("could not store pet data", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}

		L2ItemInstance itemInst = getControlItem();
		if(itemInst != null && itemInst.getEnchantLevel() != getStat().getLevel())
		{
			itemInst.setEnchantLevel(getStat().getLevel());
			itemInst.updateDatabase();
		}
	}

	public synchronized void stopFeed()
	{
		if(_feedTask != null)
		{
			_feedTask.cancel(false);
			_feedTask = null;
		}
	}

	public synchronized void startFeed(boolean battleFeed)
	{
		stopFeed();
		if(!isDead())
		{
			if(battleFeed)
			{
				_feedMode = true;
				_feedTime = _data.getPetFeedBattle();
			}
			else
			{
				_feedMode = false;
				_feedTime = _data.getPetFeedNormal();
			}

			if(_feedTime <= 0)
			{
				_feedTime = 1;
			}

			_feedTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FeedTask(), 60000 / _feedTime, 60000 / _feedTime);
		}
	}

	@Override
	public synchronized void unSummon(L2PcInstance owner)
	{
		stopFeed();
		stopHpMpRegeneration();
		super.unSummon(owner);

		if(!isDead())
		{
			L2World.getInstance().removePet(owner.getObjectId());
		}
	}

	public void restoreExp(double restorePercent)
	{
		if(_expBeforeDeath > 0)
		{
			getStat().addExp(Math.round((_expBeforeDeath - getStat().getExp()) * restorePercent / 100));
			_expBeforeDeath = 0;
		}
	}

	private void deathPenalty()
	{
		int lvl = getStat().getLevel();
		double percentLost = -0.07 * lvl + 6.5;

		long lostExp = Math.round((getStat().getExpForLevel(lvl + 1) - getStat().getExpForLevel(lvl)) * percentLost / 100);

		_expBeforeDeath = getStat().getExp();

		getStat().addExp(-lostExp);
	}

	@Override
	public void addExpAndSp(long addToExp, int addToSp)
	{
		if(getNpcId() == 12564)
		{
			getStat().addExpAndSp(Math.round(addToExp * Config.SINEATER_XP_RATE), addToSp);
		}
		else
		{
			getStat().addExpAndSp(Math.round(addToExp * Config.PET_XP_RATE), addToSp);
		}
	}

	@Override
	public long getExpForThisLevel()
	{
		return getStat().getExpForLevel(getLevel());
	}

	@Override
	public long getExpForNextLevel()
	{
		return getStat().getExpForLevel(getLevel() + 1);
	}

	@Override
	public final int getLevel()
	{
		return getStat().getLevel();
	}

	public int getMaxFed()
	{
		return getStat().getMaxFeed();
	}

	@Override
	public int getAccuracy()
	{
		return getStat().getAccuracy();
	}

	@Override
	public int getCriticalHit(L2Character target, L2Skill skill)
	{
		return getStat().getCriticalHit(target, skill);
	}

	@Override
	public int getEvasionRate(L2Character target)
	{
		return getStat().getEvasionRate(target);
	}

	@Override
	public int getRunSpeed()
	{
		return getStat().getRunSpeed();
	}

	@Override
	public int getPAtkSpd()
	{
		return getStat().getPAtkSpd();
	}

	@Override
	public int getMAtkSpd()
	{
		return getStat().getMAtkSpd();
	}

	@Override
	public int getMAtk(L2Character target, L2Skill skill)
	{
		return getStat().getMAtk(target, skill);
	}

	@Override
	public int getMDef(L2Character target, L2Skill skill)
	{
		return getStat().getMDef(target, skill);
	}

	@Override
	public int getPAtk(L2Character target)
	{
		return getStat().getPAtk(target);
	}

	@Override
	public int getPDef(L2Character target)
	{
		return getStat().getPDef(target);
	}

	@Override
	public final int getSkillLevel(int skillId)
	{
		if(_skills == null || _skills.get(skillId) == null)
		{
			return -1;
		}
		int lvl = getLevel();
		return lvl > 70 ? 7 + (lvl - 70) / 5 : lvl / 10;
	}

	public void updateRefOwner(L2PcInstance owner)
	{
		int oldOwnerId = getOwner().getObjectId();

		setOwner(owner);
		L2World.getInstance().removePet(oldOwnerId);
		L2World.getInstance().addPet(oldOwnerId, this);
	}

	@Override
	public final void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
		if(miss)
		{
			return;
		}

		if(target.getObjectId() != getOwner().getObjectId())
		{
			if(pcrit || mcrit)
			{
				getOwner().sendPacket(new SystemMessage(SystemMessageId.CRITICAL_HIT_BY_PET));
			}

			getOwner().sendPacket(new SystemMessage(SystemMessageId.PET_HIT_FOR_S1_DAMAGE).addNumber(damage));
		}
	}

	public int getInventoryLimit()
	{
		return 12;
	}
	
	/**
	 * If the actual amount of food < 40% of the max, the pet is shown as hungry.
	 * @return
	 */
	public boolean isHungry() 
 	{ 
 		return (getCurrentFed() < (getMaxFed() * 0.40)); 
 	}
}