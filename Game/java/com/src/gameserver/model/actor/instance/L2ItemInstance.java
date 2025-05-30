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
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.src.Config;
import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.datatables.sql.ItemTable;
import com.src.gameserver.geo.GeoData;
import com.src.gameserver.managers.ItemsOnGroundManager;
import com.src.gameserver.managers.MercTicketManager;
import com.src.gameserver.model.DropProtection;
import com.src.gameserver.model.L2Augmentation;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.Location;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.knownlist.NullKnownList;
import com.src.gameserver.model.extender.BaseExtender.EventType;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.funcs.Func;
import com.src.gameserver.templates.item.L2Armor;
import com.src.gameserver.templates.item.L2EtcItem;
import com.src.gameserver.templates.item.L2Item;
import com.src.gameserver.templates.item.L2Weapon;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public final class L2ItemInstance extends L2Object
{
	private static final Logger _log = Logger.getLogger(L2ItemInstance.class.getName());
	private static final Logger _logItems = Logger.getLogger("item");

	public static enum ItemLocation
	{
		VOID,
		INVENTORY,
		PAPERDOLL,
		WAREHOUSE,
		CLANWH,
		PET,
		PET_EQUIP,
		LEASE,
		FREIGHT
	}

	private int _ownerId;

	private int _count;
	private int _initCount;
	private int _time;
	private boolean _decrease = false;

	private final int _itemId;

	private final L2Item _item;

	private ItemLocation _loc;

	private int _locData;

	private int _enchantLevel;

	private int _priceSell;

	private int _priceBuy;

	private boolean _wear;

	private L2Augmentation _augmentation = null;

	private int _mana = -1;
	private boolean _consumingMana = false;
	private static final int MANA_CONSUMPTION_RATE = 60000;

	private int _type1;
	private int _type2;

	private long _dropTime;

	public static final int CHARGED_NONE = 0;
	public static final int CHARGED_SOULSHOT = 1;
	public static final int CHARGED_SPIRITSHOT = 1;
	public static final int CHARGED_BLESSED_SOULSHOT = 2;
	public static final int CHARGED_BLESSED_SPIRITSHOT = 2;

	private int _chargedSoulshot = CHARGED_NONE;
	private int _chargedSpiritshot = CHARGED_NONE;

	private boolean _chargedFishtshot = false;

	private boolean _protected;

	public static final int UNCHANGED = 0;
	public static final int ADDED = 1;
	public static final int REMOVED = 3;
	public static final int MODIFIED = 2;
	private int _lastChange = 2;
	private boolean _existsInDb;
	private boolean _storedInDb;

	private ScheduledFuture<?> itemLootShedule = null;

	private final DropProtection _dropProtection = new DropProtection();

	public L2ItemInstance(int objectId, int itemId)
	{
		super(objectId);
		super.setKnownList(new NullKnownList(this));

		_itemId = itemId;
		_item = ItemTable.getInstance().getTemplate(itemId);

		if(_itemId == 0 || _item == null)
		{
			throw new IllegalArgumentException();
		}

		super.setName(_item.getName());
		_count = 1;
		_loc = ItemLocation.VOID;
		_type1 = 0;
		_type2 = 0;
		_dropTime = 0;
		_mana = _item.getDuration();
	}

	public L2ItemInstance(int objectId, L2Item item)
	{
		super(objectId);
		super.setKnownList(new NullKnownList(this));

		_itemId = item.getItemId();
		_item = item;

		if(_itemId == 0 || _item == null)
		{
			throw new IllegalArgumentException();
		}

		_count = 1;
		_loc = ItemLocation.VOID;
		_mana = _item.getDuration();
	}

	public void setOwnerId(String process, int owner_id, L2PcInstance creator, L2Object reference)
	{
		int oldOwner = _ownerId;
		setOwnerId(owner_id);
		fireEvent(EventType.SETOWNER.name, new Object[]
		{
				process, oldOwner
		});
		if(Config.LOG_ITEMS)
		{
			LogRecord record = new LogRecord(Level.INFO, "CHANGE:" + process);
			record.setLoggerName("item");
			record.setParameters(new Object[]
			{
					this, creator, reference
			});
			_logItems.log(record);
			record = null;
		}
	}

	public void setOwnerId(int owner_id)
	{
		if(owner_id == _ownerId)
		{
			return;
		}

		_ownerId = owner_id;
		_storedInDb = false;
	}

	public int getOwnerId()
	{
		return _ownerId;
	}

	public void setLocation(ItemLocation loc)
	{
		setLocation(loc, 0);
	}

	public void setLocation(ItemLocation loc, int loc_data)
	{
		if(loc == _loc && loc_data == _locData)
		{
			return;
		}
		_loc = loc;
		_locData = loc_data;
		_storedInDb = false;
	}

	public ItemLocation getLocation()
	{
		return _loc;
	}

	public int getCount()
	{
		return _count;
	}

	public void changeCount(String process, int count, L2PcInstance creator, L2Object reference)
	{
		if(count == 0)
		{
			return;
		}

		if(count > 0 && _count > Integer.MAX_VALUE - count)
		{
			_count = Integer.MAX_VALUE;
		}
		else
		{
			_count += count;
		}

		if(_count < 0)
		{
			_count = 0;
		}

		_storedInDb = false;

		if(Config.LOG_ITEMS)
		{
			LogRecord record = new LogRecord(Level.INFO, "CHANGE:" + process);
			record.setLoggerName("item");
			record.setParameters(new Object[]
			{
					this, creator, reference
			});
			_logItems.log(record);
			record = null;
		}
	}

	public void changeCountWithoutTrace(String process, int count, L2PcInstance creator, L2Object reference)
	{
		if(count == 0)
		{
			return;
		}

		if(count > 0 && _count > Integer.MAX_VALUE - count)
		{
			_count = Integer.MAX_VALUE;
		}
		else
		{
			_count += count;
		}

		if(_count < 0)
		{
			_count = 0;
		}
		_storedInDb = false;
	}

	public void setCount(int count)
	{
		if(_count == count)
		{
			return;
		}

		_count = count >= -1 ? count : 0;
		_storedInDb = false;
	}

	public boolean isEquipable()
	{
		return !(_item.getBodyPart() == 0 || _item instanceof L2EtcItem);
	}

	public boolean isEquipped()
	{
		return _loc == ItemLocation.PAPERDOLL || _loc == ItemLocation.PET_EQUIP;
	}

	public int getEquipSlot()
	{
		return _locData;
	}

	public L2Item getItem()
	{
		return _item;
	}

	public int getCustomType1()
	{
		return _type1;
	}

	public int getCustomType2()
	{
		return _type2;
	}

	public void setCustomType1(int newtype)
	{
		_type1 = newtype;
	}

	public void setCustomType2(int newtype)
	{
		_type2 = newtype;
	}

	public void setDropTime(long time)
	{
		_dropTime = time;
	}

	public long getDropTime()
	{
		return _dropTime;
	}

	public boolean isWear()
	{
		return _wear;
	}

	public void setWear(boolean newwear)
	{
		_wear = newwear;
	}

	public Enum<?> getItemType()
	{
		return _item.getItemType();
	}

	public int getItemId()
	{
		return _itemId;
	}

	public boolean isEtcItem()
	{
		return(_item instanceof L2EtcItem);
	}

	public boolean isWeapon()
	{
		return(_item instanceof L2Weapon);
	}

	public boolean isArmor()
	{
		return(_item instanceof L2Armor);
	}

	public L2EtcItem getEtcItem()
	{
		if(_item instanceof L2EtcItem)
		{
			return(L2EtcItem)_item;
		}

		return null;
	}

	public L2Weapon getWeaponItem()
	{
		if(_item instanceof L2Weapon)
		{
			return(L2Weapon)_item;
		}

		return null;
	}

	public L2Armor getArmorItem()
	{
		if(_item instanceof L2Armor)
		{
			return(L2Armor)_item;
		}

		return null;
	}

	public final int getCrystalCount()
	{
		return _item.getCrystalCount(_enchantLevel);
	}

	public int getReferencePrice()
	{
		return _item.getReferencePrice();
	}

	public String getItemName()
	{
		return _item.getName();
	}

	public int getPriceToSell()
	{
		return isConsumable() ? (int) (_priceSell * Config.RATE_CONSUMABLE_COST) : _priceSell;
	}

	public void setPriceToSell(int price)
	{
		_priceSell = price;
		_storedInDb = false;
	}

	public int getPriceToBuy()
	{
		return isConsumable() ? (int) (_priceBuy * Config.RATE_CONSUMABLE_COST) : _priceBuy;
	}

	public void setPriceToBuy(int price)
	{
		_priceBuy = price;
		_storedInDb = false;
	}

	public int getLastChange()
	{
		return _lastChange;
	}

	public void setLastChange(int lastChange)
	{
		_lastChange = lastChange;
	}

	public boolean isStackable()
	{
		return _item.isStackable();
	}

	public boolean isDropable()
	{
		return isAugmented() ? false : _item.isDropable();
	}

	public boolean isDestroyable()
	{
		return _item.isDestroyable();
	}

	public boolean isTradeable()
	{
		return isAugmented() ? false : _item.isTradeable();
	}

	public boolean isConsumable()
	{
		return _item.isConsumable();
	}

	public boolean isAvailable(L2PcInstance player, boolean allowAdena)
	{
		return !isEquipped() && getItem().getType2() != 3 && (getItem().getType2() != 4 || getItem().getType1() != 1) && (player.getPet() == null || getObjectId() != player.getPet().getControlItemId()) && player.getActiveEnchantItem() != this && (allowAdena || getItemId() != 57) && (player.getCurrentSkill() == null || player.getCurrentSkill().getSkill().getItemConsumeId() != getItemId())&& (!player.isCastingSimultaneouslyNow() || player.getLastSimultaneousSkillCast() == null || player.getLastSimultaneousSkillCast().getItemConsumeId() != getItemId()) && isTradeable();
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		final int castleId = MercTicketManager.getInstance().getTicketCastleId((this).getItemId());

		if(castleId > 0 && (!player.isCastleLord(castleId) || player.isInParty() || player.isGM()))
		{
			if(player.isInParty())
			{
				player.sendMessage("You cannot pickup mercenaries while in a party.");
			}
			else
			{
				player.sendMessage("Only the castle lord can pickup mercenaries.");
			}

			player.setTarget(this);
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else if(!player.isFlying())
		{
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_PICK_UP, this);
		}
	}

	public int getEnchantLevel()
	{
		return _enchantLevel;
	}

	public void setEnchantLevel(int enchantLevel)
	{
		if(_enchantLevel == enchantLevel)
		{
			return;
		}
		_enchantLevel = enchantLevel;
		_storedInDb = false;
	}

	public int getPDef()
	{
		if(_item instanceof L2Armor)
		{
			return ((L2Armor) _item).getPDef();
		}

		return 0;
	}

	public boolean isAugmented()
	{
		return _augmentation == null ? false : true;
	}

	public L2Augmentation getAugmentation()
	{
		return _augmentation;
	}

	public boolean setAugmentation(L2Augmentation augmentation)
	{
		if(_augmentation != null)
		{
			return false;
		}
		_augmentation = augmentation;
		return true;
	}

	public void removeAugmentation()
	{
		if(_augmentation == null)
		{
			return;
		}
		_augmentation.deleteAugmentationData();
		_augmentation = null;
	}

	public class ScheduleConsumeManaTask implements Runnable
	{
		private L2ItemInstance _shadowItem;

		public ScheduleConsumeManaTask(L2ItemInstance item)
		{
			_shadowItem = item;
		}

		@Override
		public void run()
		{
			try
			{
				if(_shadowItem != null)
				{
					_shadowItem.decreaseMana(true);
				}
			}
			catch(Throwable t)
			{
			}
		}
	}

	public boolean isShadowItem()
	{
		return _mana >= 0;
	}

	public void setMana(int mana)
	{
		_mana = mana;
	}

	public int getMana()
	{
		return _mana;
	}

	public void decreaseMana(boolean resetConsumingMana)
	{
		if(!isShadowItem())
		{
			return;
		}

		if(_mana > 0)
		{
			_mana--;
		}

		if(_storedInDb)
		{
			_storedInDb = false;
		}

		if(resetConsumingMana)
		{
			_consumingMana = false;
		}

		L2PcInstance player = (L2PcInstance) L2World.getInstance().findObject(getOwnerId());
		if(player != null)
		{
			switch(_mana)
			{
				case 10:
					player.sendPacket(new SystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_10).addString(getItemName()));
					break;
				case 5:
					player.sendPacket(new SystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_5).addString(getItemName()));
					break;
				case 1:
					player.sendPacket(new SystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_1).addString(getItemName()));
					break;
			}

			if(_mana == 0)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_0).addString(getItemName()));

				if(isEquipped())
				{
					L2ItemInstance[] unequiped = player.getInventory().unEquipItemInSlotAndRecord(getEquipSlot());
					InventoryUpdate iu = new InventoryUpdate();

					for(L2ItemInstance element : unequiped)
					{
						player.checkSSMatch(null, element);
						iu.addModifiedItem(element);
					}

					player.sendPacket(iu);

					unequiped = null;
					iu = null;
				}

				if(getLocation() != ItemLocation.WAREHOUSE)
				{
					player.getInventory().destroyItem("L2ItemInstance", this, player, null);

					InventoryUpdate iu = new InventoryUpdate();
					iu.addRemovedItem(this);
					player.sendPacket(iu);

					StatusUpdate su = new StatusUpdate(player.getObjectId());
					su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
					player.sendPacket(su);
				}
				else
				{
					player.getWarehouse().destroyItem("L2ItemInstance", this, player, null);
				}

				L2World.getInstance().removeObject(this);
			}
			else
			{
				if(!_consumingMana && isEquipped())
				{
					scheduleConsumeManaTask();
				}

				if(getLocation() != ItemLocation.WAREHOUSE)
				{
					InventoryUpdate iu = new InventoryUpdate();
					iu.addModifiedItem(this);
					player.sendPacket(iu);
					iu = null;
				}
			}
		}

		player = null;
	}

	private void scheduleConsumeManaTask()
	{
		_consumingMana = true;
		ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleConsumeManaTask(this), MANA_CONSUMPTION_RATE);
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}

	public int getChargedSoulshot()
	{
		return _chargedSoulshot;
	}

	public int getChargedSpiritshot()
	{
		return _chargedSpiritshot;
	}

	public boolean getChargedFishshot()
	{
		return _chargedFishtshot;
	}

	public void setChargedSoulshot(int type)
	{
		_chargedSoulshot = type;
	}

	public void setChargedSpiritshot(int type)
	{
		_chargedSpiritshot = type;
	}

	public void setChargedFishshot(boolean type)
	{
		_chargedFishtshot = type;
	}

	public Func[] getStatFuncs(L2Character player)
	{
		return getItem().getStatFuncs(this, player);
	}

	public void updateDatabase()
	{
		if(isWear())
		{
			return;
		}

		if(_existsInDb)
		{
			if(_ownerId == 0 || _loc == ItemLocation.VOID || _count == 0 && _loc != ItemLocation.LEASE)
			{
				removeFromDb();
			}
			else
			{
				updateInDb();
			}
		}
		else
		{
			if(_count == 0 && _loc != ItemLocation.LEASE)
			{
				return;
			}

			if(_loc == ItemLocation.VOID || _ownerId == 0)
			{
				return;
			}

			insertIntoDb();
		}
	}

	public static L2ItemInstance restoreFromDb(int objectId)
	{
		L2ItemInstance inst = null;
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT owner_id, object_id, item_id, count, enchant_level, loc, loc_data, price_sell, price_buy, custom_type1, custom_type2, mana_left FROM items WHERE object_id = ?");
			statement.setInt(1, objectId);
			ResultSet rs = statement.executeQuery();

			if(rs.next())
			{
				int owner_id = rs.getInt("owner_id");
				int item_id = rs.getInt("item_id");
				int count = rs.getInt("count");

				ItemLocation loc = ItemLocation.valueOf(rs.getString("loc"));

				int loc_data = rs.getInt("loc_data");
				int enchant_level = rs.getInt("enchant_level");
				int custom_type1 = rs.getInt("custom_type1");
				int custom_type2 = rs.getInt("custom_type2");
				int price_sell = rs.getInt("price_sell");
				int price_buy = rs.getInt("price_buy");
				int manaLeft = rs.getInt("mana_left");

				L2Item item = ItemTable.getInstance().getTemplate(item_id);

				if(item == null)
				{
					_log.severe("Item item_id=" + item_id + " not known, object_id=" + objectId);
					rs.close();
					statement.close();
					return null;
				}

				inst = new L2ItemInstance(objectId, item);
				inst._existsInDb = true;
				inst._storedInDb = true;
				inst._ownerId = owner_id;
				inst._count = count;
				inst._enchantLevel = enchant_level;
				inst._type1 = custom_type1;
				inst._type2 = custom_type2;
				inst._loc = loc;
				inst._locData = loc_data;
				inst._priceSell = price_sell;
				inst._priceBuy = price_buy;

				inst._mana = manaLeft;

				if(inst._mana > 0 && inst.getLocation() == ItemLocation.PAPERDOLL)
				{
					inst.decreaseMana(false);
				}

				if(inst._mana == 0)
				{
					inst.removeFromDb();

					rs.close();
					statement.close();

					return null;
				}
				else if(inst._mana > 0 && inst.getLocation() == ItemLocation.PAPERDOLL)
				{
					inst.scheduleConsumeManaTask();
				}

				loc = null;
				item = null;
			}
			else
			{
				_log.severe("Item object_id=" + objectId + " not found");

				rs.close();
				statement.close();

				return null;
			}

			rs.close();
			statement.close();

			statement = con.prepareStatement("SELECT attributes, skill, level FROM augmentations WHERE item_id = ?");
			statement.setInt(1, objectId);
			rs = statement.executeQuery();

			if(rs.next())
			{
				inst._augmentation = new L2Augmentation(inst, rs.getInt("attributes"), rs.getInt("skill"), rs.getInt("level"), false);
			}

			rs.close();
			statement.close();
			inst.fireEvent(EventType.LOAD.name, new Object[]
			{
				con
			});

			rs.close();
			statement.close();
			rs = null;
			statement = null;
		}
		catch(Exception e)
		{
			_log.log(Level.SEVERE, "Could not restore item " + objectId + " from DB:", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}

		return inst;
	}

	public final void dropMe(L2Character dropper, int x, int y, int z)
	{
		if(Config.GEODATA > 0 && dropper != null)
		{
			Location dropDest = GeoData.getInstance().moveCheck(dropper.getX(), dropper.getY(), dropper.getZ(), x, y, z);
			if(dropDest != null && dropDest.getX() != 0 && dropDest.getY() != 0)
			{
				x = dropDest.getX();
				y = dropDest.getY();
				z = dropDest.getZ();
			}

			dropDest = null;
		}

		synchronized (this)
		{
			setIsVisible(true);
			getPosition().setWorldPosition(x, y, z);
			getPosition().setWorldRegion(L2World.getInstance().getRegion(getPosition().getWorldPosition()));

			getPosition().getWorldRegion().addVisibleObject(this);
		}

		setDropTime(System.currentTimeMillis());

		L2World.getInstance().addVisibleObject(this, getPosition().getWorldRegion(), dropper);

		if(Config.SAVE_DROPPED_ITEM)
		{
			ItemsOnGroundManager.getInstance().save(this);
		}
	}

	private void updateInDb()
	{
		if(_wear)
		{
			return;
		}

		if(_storedInDb)
		{
			return;
		}

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE items SET owner_id = ?, count = ?, loc = ?, loc_data = ?, enchant_level = ?, price_sell = ?, price_buy = ?, custom_type1 = ?, custom_type2 = ?, mana_left = ? " + "WHERE object_id = ?");
			statement.setInt(1, _ownerId);
			statement.setInt(2, getCount());
			statement.setString(3, _loc.name());
			statement.setInt(4, _locData);
			statement.setInt(5, getEnchantLevel());
			statement.setInt(6, _priceSell);
			statement.setInt(7, _priceBuy);
			statement.setInt(8, getCustomType1());
			statement.setInt(9, getCustomType2());
			statement.setInt(10, getMana());
			statement.setInt(11, getObjectId());
			statement.executeUpdate();
			_existsInDb = true;
			_storedInDb = true;
			statement.close();
			statement = null;
			fireEvent(EventType.STORE.name, new Object[]
			{
				con
			});
		}
		catch(Exception e)
		{
			_log.log(Level.SEVERE, "Could not update item " + getObjectId() + " in DB: Reason: " + "Duplicate itemId");
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	private void insertIntoDb()
	{
		if(_wear)
		{
			return;
		}

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO items (owner_id, item_id, count, loc, loc_data, enchant_level, price_sell, price_buy, object_id, custom_type1, custom_type2, mana_left) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			statement.setInt(1, _ownerId);
			statement.setInt(2, _itemId);
			statement.setInt(3, getCount());
			statement.setString(4, _loc.name());
			statement.setInt(5, _locData);
			statement.setInt(6, getEnchantLevel());
			statement.setInt(7, _priceSell);
			statement.setInt(8, _priceBuy);
			statement.setInt(9, getObjectId());
			statement.setInt(10, _type1);
			statement.setInt(11, _type2);
			statement.setInt(12, getMana());

			statement.executeUpdate();
			_existsInDb = true;
			_storedInDb = true;
			statement.close();
			statement = null;
		}
		catch(Exception e)
		{
			_log.log(Level.SEVERE, "Could not insert item " + getObjectId() + " into DB: Reason: " + "Duplicate itemId");
			e.printStackTrace();
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	private void removeFromDb()
	{
		if(_wear)
		{
			return;
		}

		if(isAugmented())
		{
			_augmentation.deleteAugmentationData();
		}

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM items WHERE object_id = ?");
			statement.setInt(1, getObjectId());
			statement.executeUpdate();
			_existsInDb = false;
			_storedInDb = false;
			statement.close();
			statement = null;
			fireEvent(EventType.DELETE.name, new Object[]
			{
				con
			});
		}
		catch(Exception e)
		{
			_log.log(Level.SEVERE, "Could not delete item " + getObjectId() + " in DB:", e);
			e.printStackTrace();
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	@Override
	public String toString()
	{
		return "" + _item;
	}

	public void resetOwnerTimer()
	{
		if(itemLootShedule != null)
		{
			itemLootShedule.cancel(true);
		}
		itemLootShedule = null;
	}

	public void setItemLootShedule(ScheduledFuture<?> sf)
	{
		itemLootShedule = sf;
	}

	public ScheduledFuture<?> getItemLootShedule()
	{
		return itemLootShedule;
	}

	public void setProtected(boolean is_protected)
	{
		_protected = is_protected;
	}

	public boolean isProtected()
	{
		return _protected;
	}

	public boolean isNightLure()
	{
		return _itemId >= 8505 && _itemId <= 8513 || _itemId == 8485;
	}

	public void setCountDecrease(boolean decrease)
	{
		_decrease = decrease;
	}

	public boolean getCountDecrease()
	{
		return _decrease;
	}

	public void setInitCount(int InitCount)
	{
		_initCount = InitCount;
	}

	public int getInitCount()
	{
		return _initCount;
	}

	public void restoreInitCount()
	{
		if(_decrease)
		{
			_count = _initCount;
		}
	}

	public void setTime(int time)
	{
		if(time > 0)
		{
			_time = time;
		}
		else
		{
			_time = 0;
		}
	}

	public int getTime()
	{
		return _time;
	}

	public int getLocationSlot()
	{
		return _locData;
	}

	public final DropProtection getDropProtection()
	{
		return _dropProtection;
	}

	public boolean isVarkaKetraAllyQuestItem()
	{
		if((this.getItemId() >= 7211 && this.getItemId() <= 7215)
				|| (this.getItemId() >= 7221 && this.getItemId() <= 7225)){
			return true;
		}
		return false;
	}

	public boolean isCupidBow()
    {
        if (getItemId() == 9140 || getItemId() == 9141)
            return true;
        return false;
    }
	
	public boolean isHeroItem()
	{
		return ((_itemId >= 6611 && _itemId <= 6621) || (_itemId >= 9388 && _itemId <= 9390) || _itemId == 6842);
	}

	public boolean isCommonItem() 
	{ 
		return ((_itemId >= 12006 && _itemId <= 12361) || (_itemId >= 11605 && _itemId <= 12308)); 
	} 
	
}