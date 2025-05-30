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
package com.src.gameserver.network.clientpackets;

import com.src.Config;
import com.src.gameserver.datatables.xml.AugmentationData;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ExVariationResult;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.item.L2Item;
import com.src.gameserver.util.Util;

public final class RequestRefine extends L2GameClientPacket
{
	private int _targetItemObjId;
	private int _refinerItemObjId;
	private int _gemstoneItemObjId;
	private int _gemstoneCount;

	@Override
	protected void readImpl()
	{
		_targetItemObjId = readD();
		_refinerItemObjId = readD();
		_gemstoneItemObjId = readD();
		_gemstoneCount = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		L2ItemInstance targetItem = (L2ItemInstance) L2World.getInstance().findObject(_targetItemObjId);
		L2ItemInstance refinerItem = (L2ItemInstance) L2World.getInstance().findObject(_refinerItemObjId);
		L2ItemInstance gemstoneItem = (L2ItemInstance) L2World.getInstance().findObject(_gemstoneItemObjId);

		if(targetItem == null || refinerItem == null || gemstoneItem == null || targetItem.getOwnerId() != activeChar.getObjectId() || refinerItem.getOwnerId() != activeChar.getObjectId() || gemstoneItem.getOwnerId() != activeChar.getObjectId() || activeChar.getLevel() < 46) // must be lvl 46
		{
			activeChar.sendPacket(new ExVariationResult(0, 0, 0));
			activeChar.sendPacket(new SystemMessage(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS));
			return;
		}

		// unequip item
		if(targetItem.isEquipped())
		{
			activeChar.disarmWeapons();
		}

		if(TryAugmentItem(activeChar, targetItem, refinerItem, gemstoneItem))
		{
			int stat12 = 0x0000FFFF & targetItem.getAugmentation().getAugmentationId();
			int stat34 = targetItem.getAugmentation().getAugmentationId() >> 16;
			activeChar.sendPacket(new ExVariationResult(stat12, stat34, 1));
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THE_ITEM_WAS_SUCCESSFULLY_AUGMENTED));
		}
		else
		{
			activeChar.sendPacket(new ExVariationResult(0, 0, 0));
			activeChar.sendPacket(new SystemMessage(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS));
		}
	}

	boolean TryAugmentItem(L2PcInstance player, L2ItemInstance targetItem, L2ItemInstance refinerItem, L2ItemInstance gemstoneItem)
	{
		if(targetItem.isAugmented() || targetItem.isWear())
		{
			player.sendMessage("You can't augment items while you wear it");
			return false;
		}

		if(player.isDead())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_DEAD));
			return false;
		}

		if(player.isSitting())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_SITTING_DOWN));
			return false;
		}

		if(player.isFishing())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_FISHING));
			return false;
		}

		if(player.isParalyzed())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_PARALYZED));
			return false;
		}

		if(player.getActiveTradeList() != null)
		{
			player.sendMessage("You cannot augment while trading");
			return false;
		}

		if(player.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP_IS_IN_OPERATION));
			return false;
		}

		// check for the items to be in the inventory of the owner
		if(player.getInventory().getItemByObjectId(refinerItem.getObjectId()) == null)
		{
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to refine an item with wrong LifeStone-id.", Config.DEFAULT_PUNISH);
			return false;
		}

		if(player.getInventory().getItemByObjectId(targetItem.getObjectId()) == null)
		{
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to refine an item with wrong Weapon-id.", Config.DEFAULT_PUNISH);
			return false;
		}

		if(player.getInventory().getItemByObjectId(gemstoneItem.getObjectId()) == null)
		{
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to refine an item with wrong Gemstone-id.", Config.DEFAULT_PUNISH);
			return false;
		}

		int itemGrade = targetItem.getItem().getItemGrade();
		int itemType = targetItem.getItem().getType2();
		int lifeStoneId = refinerItem.getItemId();
		int gemstoneItemId = gemstoneItem.getItemId();

		// is the refiner Item a life stone?
		if(lifeStoneId < 8723 || lifeStoneId > 8762)
			return false;

		// must be a weapon, must be > d grade
		// TODO: can do better? : currently: using isdestroyable() as a check for hero / cursed weapons
		if(itemGrade < L2Item.CRYSTAL_C || itemType != L2Item.TYPE2_WEAPON || !targetItem.isDestroyable())
			return false;

		// player must be able to use augmentation
		if(player.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE || player.isDead() || player.isParalyzed() || player.isFishing() || player.isSitting())
			return false;

		int modifyGemstoneCount = _gemstoneCount;
		int lifeStoneLevel = getLifeStoneLevel(lifeStoneId);
		int lifeStoneGrade = getLifeStoneGrade(lifeStoneId);
		switch(itemGrade)
		{
			case L2Item.CRYSTAL_C:
				if(player.getLevel() < 46 || gemstoneItemId != 2130)
					return false;

				modifyGemstoneCount = 20;
				break;
			case L2Item.CRYSTAL_B:
				if(lifeStoneLevel < 3 || player.getLevel() < 52 || gemstoneItemId != 2130)
					return false;

				modifyGemstoneCount = 30;
				break;
			case L2Item.CRYSTAL_A:
				if(lifeStoneLevel < 6 || player.getLevel() < 61 || gemstoneItemId != 2131)
					return false;

				modifyGemstoneCount = 20;
				break;
			case L2Item.CRYSTAL_S:
				if(lifeStoneLevel != 10 || player.getLevel() < 76 || gemstoneItemId != 2131)
					return false;

				modifyGemstoneCount = 25;
				break;
		}

		// check if the lifestone is appropriate for this player
		switch(lifeStoneLevel)
		{
			case 1:
				if(player.getLevel() < 46)
					return false;
				break;
			case 2:
				if(player.getLevel() < 49)
					return false;
				break;
			case 3:
				if(player.getLevel() < 52)
					return false;
				break;
			case 4:
				if(player.getLevel() < 55)
					return false;
				break;
			case 5:
				if(player.getLevel() < 58)
					return false;
				break;
			case 6:
				if(player.getLevel() < 61)
					return false;
				break;
			case 7:
				if(player.getLevel() < 64)
					return false;
				break;
			case 8:
				if(player.getLevel() < 67)
					return false;
				break;
			case 9:
				if(player.getLevel() < 70)
					return false;
				break;
			case 10:
				if(player.getLevel() < 76)
					return false;
				break;
		}

        if (gemstoneItem.getCount() - modifyGemstoneCount < 0)
            return false;
        
		// consume the life stone
		if(!player.destroyItem("RequestRefine", refinerItem, null, false))
			return false;

		// consume the gemstones
		player.destroyItem("RequestRefine", _gemstoneItemObjId, modifyGemstoneCount, null, false);

		// generate augmentation
		targetItem.setAugmentation(AugmentationData.getInstance().generateRandomAugmentation(targetItem, lifeStoneLevel, lifeStoneGrade));

		// finish and send the inventory update packet
		InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(targetItem);
		player.sendPacket(iu);

		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);

		return true;
	}

	private int getLifeStoneGrade(int itemId)
	{
		itemId -= 8723;
		if(itemId < 10)
			return 0; // normal grade

		if(itemId < 20)
			return 1; // mid grade

		if(itemId < 30)
			return 2; // high grade

		return 3; // top grade
	}

	private int getLifeStoneLevel(int itemId)
	{
		itemId -= 10 * getLifeStoneGrade(itemId);
		itemId -= 8722;
		return itemId;
	}

	@Override
	public String getType()
	{
		return "[C] D0:2C RequestRefine";
	}
}