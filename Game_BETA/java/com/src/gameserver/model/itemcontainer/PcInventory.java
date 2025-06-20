/* This program is free software; you can redistribute it and/or modify
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
package com.src.gameserver.model.itemcontainer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import javolution.util.FastList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.TradeList;
import com.src.gameserver.model.TradeList.TradeItem;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance.ItemLocation;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.templates.item.L2EtcItemType;
import com.src.gameserver.templates.item.L2Item;
import com.src.gameserver.util.Util;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class PcInventory extends Inventory
{
	private static final Log _log = LogFactory.getLog(PcInventory.class);

	public static final int ADENA_ID = 57;
	public static final int ANCIENT_ADENA_ID = 5575;

	private final L2PcInstance _owner;
	private L2ItemInstance _adena;
	private L2ItemInstance _ancientAdena;
	
	private int[] _blockItems = null;

	private int _blockMode = -1;
	
	public PcInventory(L2PcInstance owner)
	{
		_owner = owner;
	}

	@Override
	public L2PcInstance getOwner()
	{
		return _owner;
	}
	
	/*boolean b = Config.FORCE_INVENTORY_UPDATE;*/

	@Override
	protected ItemLocation getBaseLocation()
	{
		return ItemLocation.INVENTORY;
	}

	@Override
	protected ItemLocation getEquipLocation()
	{
		return ItemLocation.PAPERDOLL;
	}

	public L2ItemInstance getAdenaInstance()
	{
		return _adena;
	}

	@Override
	public int getAdena()
	{
		return _adena != null ? _adena.getCount() : 0;
	}

	public L2ItemInstance getAncientAdenaInstance()
	{
		return _ancientAdena;
	}

	public int getAncientAdena()
	{
		return _ancientAdena != null ? _ancientAdena.getCount() : 0;
	}

	public L2ItemInstance[] getUniqueItems(boolean allowAdena, boolean allowAncientAdena)
	{
		return getUniqueItems(allowAdena, allowAncientAdena, true);
	}

	public L2ItemInstance[] getUniqueItems(boolean allowAdena, boolean allowAncientAdena, boolean onlyAvailable)
	{
		List<L2ItemInstance> list = new FastList<L2ItemInstance>();

		for(L2ItemInstance item : _items)
		{
			if(!allowAdena && item.getItemId() == 57)
			{
				continue;
			}

			if(!allowAncientAdena && item.getItemId() == 5575)
			{
				continue;
			}

			boolean isDuplicate = false;

			for(L2ItemInstance litem : list)
			{
				if(litem.getItemId() == item.getItemId())
				{
					isDuplicate = true;
					break;
				}
			}
			if(!isDuplicate && (!onlyAvailable || item.getItem().isSellable() && item.isAvailable(getOwner(), false)))
			{
				list.add(item);
			}
		}

		return list.toArray(new L2ItemInstance[list.size()]);
	}

	public L2ItemInstance[] getUniqueItemsByEnchantLevel(boolean allowAdena, boolean allowAncientAdena)
	{
		return getUniqueItemsByEnchantLevel(allowAdena, allowAncientAdena, true);
	}

	public L2ItemInstance[] getUniqueItemsByEnchantLevel(boolean allowAdena, boolean allowAncientAdena, boolean onlyAvailable)
	{
		List<L2ItemInstance> list = new FastList<L2ItemInstance>();

		for(L2ItemInstance item : _items)
		{
			if(!allowAdena && item.getItemId() == 57)
			{
				continue;
			}

			if(!allowAncientAdena && item.getItemId() == 5575)
			{
				continue;
			}

			boolean isDuplicate = false;

			for(L2ItemInstance litem : list)
			{
				if(litem.getItemId() == item.getItemId() && litem.getEnchantLevel() == item.getEnchantLevel())
				{
					isDuplicate = true;
					break;
				}
			}

			if(!isDuplicate && (!onlyAvailable || item.getItem().isSellable() && item.isAvailable(getOwner(), false)))
			{
				list.add(item);
			}
		}

		return list.toArray(new L2ItemInstance[list.size()]);
	}

	public L2ItemInstance[] getAllItemsByItemId(int itemId)
	{
		List<L2ItemInstance> list = new FastList<L2ItemInstance>();

		for(L2ItemInstance item : _items)
		{
			if(item.getItemId() == itemId)
			{
				list.add(item);
			}
		}

		return list.toArray(new L2ItemInstance[list.size()]);
	}

	public L2ItemInstance[] getAllItemsByItemId(int itemId, int enchantment)
	{
		List<L2ItemInstance> list = new FastList<L2ItemInstance>();

		for(L2ItemInstance item : _items)
		{
			if(item.getItemId() == itemId && item.getEnchantLevel() == enchantment)
			{
				list.add(item);
			}
		}

		return list.toArray(new L2ItemInstance[list.size()]);
	}

	public L2ItemInstance[] getAvailableItems(boolean allowAdena)
	{
		List<L2ItemInstance> list = new FastList<L2ItemInstance>();

		for(L2ItemInstance item : _items)
		{
			if(item != null && item.isAvailable(getOwner(), allowAdena) && canManipulateWithItemId(item.getItemId()))
			{
				list.add(item);
			}
		}

		return list.toArray(new L2ItemInstance[list.size()]);
	}

	public L2ItemInstance[] getAugmentedItems()
	{
		List<L2ItemInstance> list = new FastList<L2ItemInstance>();

		for(L2ItemInstance item : _items)
		{
			if(item != null && item.isAugmented())
			{
				list.add(item);
			}
		}

		return list.toArray(new L2ItemInstance[list.size()]);
	}

	public TradeList.TradeItem[] getAvailableItems(TradeList tradeList)
	{
		List<TradeList.TradeItem> list = new FastList<TradeList.TradeItem>();

		for(L2ItemInstance item : _items)
		{
			if(item.isAvailable(getOwner(), false))
			{
				TradeList.TradeItem adjItem = tradeList.adjustAvailableItem(item);
				if(adjItem != null)
				{
					list.add(adjItem);
				}
			}
		}

		return list.toArray(new TradeList.TradeItem[list.size()]);
	}

	public void adjustAvailableItem(TradeItem item)
	{
		for(L2ItemInstance adjItem : _items)
		{
			if(adjItem.getItemId() == item.getItem().getItemId())
			{
				item.setObjectId(adjItem.getObjectId());
				item.setEnchant(adjItem.getEnchantLevel());

				if(adjItem.getCount() < item.getCount())
				{
					item.setCount(adjItem.getCount());
				}

				return;
			}
		}

		item.setCount(0);
	}

	public void addAdena(String process, int count, L2PcInstance actor, L2Object reference)
	{
		if(count > 0)
		{
			addItem(process, ADENA_ID, count, actor, reference);
		}
	}

	public void reduceAdena(String process, int count, L2PcInstance actor, L2Object reference)
	{
		if(count > 0)
		{
			destroyItemByItemId(process, ADENA_ID, count, actor, reference);
		}
	}

	/**
     * Adds specified amount of ancient adena to player inventory.
	 * @param process : String Identifier of process triggering this action
     * @param count : int Quantity of adena to be added
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
     */
	public void addAncientAdena(String process, int count, L2PcInstance actor, L2Object reference)
	{
		if(count > 0)
		{
			addItem(process, ANCIENT_ADENA_ID, count, actor, reference);
		}
	}

    /**
     * Removes specified amount of ancient adena from player inventory.
	 * @param process : String Identifier of process triggering this action
     * @param count : int Quantity of adena to be removed
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
     */
	public void reduceAncientAdena(String process, int count, L2PcInstance actor, L2Object reference)
	{
		if(count > 0)
		{
			destroyItemByItemId(process, ANCIENT_ADENA_ID, count, actor, reference);
		}
	}

	/**
	 * Adds item in inventory and checks _adena and _ancientAdena
	 * 
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be added
	 * @param actor : L2PcInstance Player requesting the item add
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in
	 *            transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance addItem(String process, L2ItemInstance item, L2PcInstance actor, L2Object reference)
	{
		item = super.addItem(process, item, actor, reference);

		if(item != null && item.getItemId() == ADENA_ID && !item.equals(_adena))
		{
			_adena = item;
		}

		if(item != null && item.getItemId() == ANCIENT_ADENA_ID && !item.equals(_ancientAdena))
		{
			_ancientAdena = item;
		}

		return item;
	}

	/**
	 * Adds item in inventory and checks _adena and _ancientAdena
	 * 
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item Identifier of the item to be added
	 * @param count : int Quantity of items to be added
	 * @param actor : L2PcInstance Player requesting the item creation
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in
	 *            transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance addItem(String process, int itemId, int count, L2PcInstance actor, L2Object reference)
	{
		L2ItemInstance item = super.addItem(process, itemId, count, actor, reference);

		if(item != null && item.getItemId() == ADENA_ID && !item.equals(_adena))
		{
			_adena = item;
		}

		if(item != null && item.getItemId() == ANCIENT_ADENA_ID && !item.equals(_ancientAdena))
		{
			_ancientAdena = item;
		}

		return item;
	}

    /**
     * Transfers item to another inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
     * @param itemId : int Item Identifier of the item to be transfered
     * @param count : int Quantity of items to be transfered
	 * @param actor : L2PcInstance Player requesting the item transfer
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @return L2ItemInstance corresponding to the new item or the updated item in inventory
     */
	@Override
	public L2ItemInstance transferItem(String process, int objectId, int count, ItemContainer target, L2PcInstance actor, L2Object reference)
	{
		L2ItemInstance item = super.transferItem(process, objectId, count, target, actor, reference);

		if(_adena != null && (_adena.getCount() <= 0 || _adena.getOwnerId() != getOwnerId()))
		{
			_adena = null;
		}

		if(_ancientAdena != null && (_ancientAdena.getCount() <= 0 || _ancientAdena.getOwnerId() != getOwnerId()))
		{
			_ancientAdena = null;
		}

		return item;
	}

	/**
	 * Destroy item from inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be destroyed
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance destroyItem(String process, L2ItemInstance item, L2PcInstance actor, L2Object reference)
	{
		item = super.destroyItem(process, item, actor, reference);

		if(_adena != null && _adena.getCount() <= 0)
		{
			_adena = null;
		}

		if(_ancientAdena != null && _ancientAdena.getCount() <= 0)
		{
			_ancientAdena = null;
		}

		return item;
	}

	/**
	 * Destroys item from inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance destroyItem(String process, int objectId, int count, L2PcInstance actor, L2Object reference)
	{
		L2ItemInstance item = super.destroyItem(process, objectId, count, actor, reference);

		if(_adena != null && _adena.getCount() <= 0)
		{
			_adena = null;
		}

		if(_ancientAdena != null && _ancientAdena.getCount() <= 0)
		{
			_ancientAdena = null;
		}

		return item;
	}

	/**
	 * Destroy item from inventory by using its <B>itemId</B> and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
     * @param itemId : int Item identifier of the item to be destroyed
     * @param count : int Quantity of items to be destroyed
	 * @param actor : L2PcInstance Player requesting the item destroy
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance destroyItemByItemId(String process, int itemId, int count, L2PcInstance actor, L2Object reference)
	{
		L2ItemInstance item = super.destroyItemByItemId(process, itemId, count, actor, reference);

		if(_adena != null && _adena.getCount() <= 0)
		{
			_adena = null;
		}

		if(_ancientAdena != null && _ancientAdena.getCount() <= 0)
		{
			_ancientAdena = null;
		}

		return item;
	}

	/**
	 * Drop item from inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
     * @param item : L2ItemInstance to be dropped
	 * @param actor : L2PcInstance Player requesting the item drop
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance dropItem(String process, L2ItemInstance item, L2PcInstance actor, L2Object reference)
	{
		item = super.dropItem(process, item, actor, reference);

		if(_adena != null && (_adena.getCount() <= 0 || _adena.getOwnerId() != getOwnerId()))
		{
			_adena = null;
		}

		if(_ancientAdena != null && (_ancientAdena.getCount() <= 0 || _ancientAdena.getOwnerId() != getOwnerId()))
		{
			_ancientAdena = null;
		}

		return item;
	}


	/**
	 * Drop item from inventory by using its <B>objectID</B> and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
     * @param objectId : int Item Instance identifier of the item to be dropped
     * @param count : int Quantity of items to be dropped
	 * @param actor : L2PcInstance Player requesting the item drop
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public L2ItemInstance dropItem(String process, int objectId, int count, L2PcInstance actor, L2Object reference)
	{
		L2ItemInstance item = super.dropItem(process, objectId, count, actor, reference);

		if(_adena != null && (_adena.getCount() <= 0 || _adena.getOwnerId() != getOwnerId()))
		{
			_adena = null;
		}

		if(_ancientAdena != null && (_ancientAdena.getCount() <= 0 || _ancientAdena.getOwnerId() != getOwnerId()))
		{
			_ancientAdena = null;
		}

		return item;
	}

	@Override
	protected void removeItem(L2ItemInstance item)
	{
		getOwner().removeItemFromShortCut(item.getObjectId());

		if(item.equals(getOwner().getActiveEnchantItem()))
		{
			getOwner().setActiveEnchantItem(null);
		}

		if(item.getItemId() == ADENA_ID)
		{
			_adena = null;
		}
		else if(item.getItemId() == ANCIENT_ADENA_ID)
		{
			_ancientAdena = null;
		}

		super.removeItem(item);
	}

	@Override
	public void refreshWeight()
	{
		super.refreshWeight();
		getOwner().refreshOverloaded();
	}

	@Override
	public void restore()
	{
		super.restore();
		_adena = getItemByItemId(ADENA_ID);
		_ancientAdena = getItemByItemId(ANCIENT_ADENA_ID);
	}

	public static int[][] restoreVisibleInventory(int objectId)
	{
		int[][] paperdoll = new int[0x12][3];
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement2 = con.prepareStatement("SELECT object_id, item_id, loc_data, enchant_level FROM items WHERE owner_id = ? AND loc = 'PAPERDOLL'");
			statement2.setInt(1, objectId);
			ResultSet invdata = statement2.executeQuery();

			while(invdata.next())
			{
				int slot = invdata.getInt("loc_data");
				paperdoll[slot][0] = invdata.getInt("object_id");
				paperdoll[slot][1] = invdata.getInt("item_id");
				paperdoll[slot][2] = invdata.getInt("enchant_level");
			}

			invdata.close();
			statement2.close();
		}
		catch(Exception e)
		{
			_log.error("could not restore inventory", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
		return paperdoll;
	}

	public boolean validateCapacity(L2ItemInstance item)
	{
		int slots = 0;

		if(!(item.isStackable() && getItemByItemId(item.getItemId()) != null) && item.getItemType() != L2EtcItemType.HERB)
		{
			slots++;
		}
		return validateCapacity(slots);
	}

	public boolean validateCapacity(List<L2ItemInstance> items)
	{
		int slots = 0;

		for(L2ItemInstance item : items)
		{
			if(!(item.isStackable() && getItemByItemId(item.getItemId()) != null))
			{
				slots++;
			}
		}
		return validateCapacity(slots);
	}

	public boolean validateCapacityByItemId(int ItemId)
	{
		int slots = 0;

		L2ItemInstance invItem = getItemByItemId(ItemId);

		if(!(invItem != null && invItem.isStackable()))
		{
			slots++;
		}
		return validateCapacity(slots);
	}

	@Override
	public boolean validateCapacity(int slots)
	{
		return _items.size() + slots <= _owner.GetInventoryLimit();
	}

	@Override
	public boolean validateWeight(int weight)
	{
		if(_owner.isGM() && _owner.getAccessLevel().allowTransaction())
		{
			return true;
		}

		return _totalWeight + weight <= _owner.getMaxLoad();
	}

	public boolean validateCapacity(L2Item item)
	{
		int slots = 0;

		if(!(item.isStackable() && getItemByItemId(item.getItemId()) != null) && item.getItemType() != L2EtcItemType.HERB)
		{
			slots++;
		}
		return validateCapacity(slots);
	}
	
	public boolean checkIfEquipped(int item_id){
		
		final L2ItemInstance[] items = getAllItemsByItemId(item_id);
		
		if(items == null
				|| items.length == 0){
			
			return false;
			
		}
		
		for(final L2ItemInstance item:items){
			
			if(item.isEquipped())
				return true;
			
		}
		
		return false;
		
	}
	
	public void setInventoryBlock(int[] items, int mode)
	{
		_blockMode = mode;
		_blockItems = items;
		
		_owner.sendPacket(new ItemList(_owner, false));
	}
	
	/**
	 * Unblock blocked itemIds
	 */
	public void unblock()
	{
		_blockMode = -1;
		_blockItems = null;
		
		_owner.sendPacket(new ItemList(_owner, false));
	}
	
	/**
	 * Check if player inventory is in block mode.
	 * @return true if some itemIds blocked
	 */
	public boolean hasInventoryBlock()
	{
		return (_blockMode > -1 && _blockItems != null && _blockItems.length > 0);
	}
	
	/**
	 * Return block mode
	 * @return int {@link PcInventory#_blockMode}
	 */
	public int getBlockMode()
	{
		return _blockMode;
	}
	
	/**
	 * Return TIntArrayList with blocked item ids
	 * @return TIntArrayList
	 */
	public int[] getBlockItems()
	{
		return _blockItems;
	}
	
	public boolean canManipulateWithItemId(int itemId)
	{
		if ((_blockMode == 0 && Util.contains(_blockItems, itemId)) || (_blockMode == 1 && !Util.contains(_blockItems, itemId)))
			return false;
		return true;
	}
	
	
	public int checkHowManyEquipped(int item_id){
		
		final L2ItemInstance[] items = getAllItemsByItemId(item_id);
		
		if(items == null
				|| items.length == 0){
			
			return 0;
			
		}
		
		int count = 0;
		for(final L2ItemInstance item:items){
			
			if(item.isEquipped())
				count++;
			
		}
		
		return count;
		
	}
	
}