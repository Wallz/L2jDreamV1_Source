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
package com.src.gameserver.network.clientpackets;

import java.util.logging.Logger;

import com.src.Config;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.itemcontainer.PcInventory;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.item.L2Item;
import com.src.gameserver.util.IllegalPlayerAction;
import com.src.gameserver.util.Util;

public final class RequestCrystallizeItem extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestCrystallizeItem.class.getName());

	private static final String _C__72_REQUESTDCRYSTALLIZEITEM = "[C] 72 RequestCrystallizeItem";

	private int _objectId;
	private int _count;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_count = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if(activeChar == null)
		{
			_log.fine("RequestCrystalizeItem: activeChar was null");
			return;
		}

		if(_count <= 0)
		{
			Util.handleIllegalPlayerAction(activeChar, "[RequestCrystallizeItem] count <= 0! ban! oid: " + _objectId + " owner: " + activeChar.getName(), IllegalPlayerAction.PUNISH_KICK);
			return;
		}

		if(activeChar.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE || activeChar.isInCrystallize())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE));
			return;
		}

		int skillLevel = activeChar.getSkillLevel(L2Skill.SKILL_CRYSTALLIZE);
		if(skillLevel <= 0)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CRYSTALLIZE_LEVEL_TOO_LOW));
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		PcInventory inventory = activeChar.getInventory();
		if(inventory != null)
		{
			L2ItemInstance item = inventory.getItemByObjectId(_objectId);
			if(item == null || item.isWear())
			{
				ActionFailed af = ActionFailed.STATIC_PACKET;
				activeChar.sendPacket(af);
				return;
			}

			int itemId = item.getItemId();

			if(itemId >= 6611 && itemId <= 6621 || itemId == 6842)
			{
				return;
			}

			if(_count > item.getCount())
			{
				_count = activeChar.getInventory().getItemByObjectId(_objectId).getCount();
			}
		}

		L2ItemInstance itemToRemove = activeChar.getInventory().getItemByObjectId(_objectId);

		if(itemToRemove == null || itemToRemove.isWear())
		{
			return;
		}

		if(itemToRemove.fireEvent("CRYSTALLIZE", (Object[]) null) != null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISCARD_THIS_ITEM));
			return;
		}

		if (!activeChar.getInventory().canManipulateWithItemId(itemToRemove.getItemId()))
			return;
		
		if(!itemToRemove.getItem().isCrystallizable() || itemToRemove.getItem().getCrystalCount() <= 0 || itemToRemove.getItem().getCrystalType() == L2Item.CRYSTAL_NONE)
		{
			_log.warning("" + activeChar.getObjectId() + " tried to crystallize " + itemToRemove.getItem().getItemId());
			return;
		}

		if(itemToRemove.getItem().getCrystalType() == L2Item.CRYSTAL_C && skillLevel <= 1)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CRYSTALLIZE_LEVEL_TOO_LOW));
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(itemToRemove.getItem().getCrystalType() == L2Item.CRYSTAL_B && skillLevel <= 2)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CRYSTALLIZE_LEVEL_TOO_LOW));
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(itemToRemove.getItem().getCrystalType() == L2Item.CRYSTAL_A && skillLevel <= 3)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CRYSTALLIZE_LEVEL_TOO_LOW));
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(itemToRemove.getItem().getCrystalType() == L2Item.CRYSTAL_S && skillLevel <= 4)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CRYSTALLIZE_LEVEL_TOO_LOW));
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		activeChar.setInCrystallize(true);

		if(itemToRemove.isEquipped())
		{
			if(itemToRemove.isAugmented())
			{
				itemToRemove.getAugmentation().removeBoni(activeChar);
			}

			L2ItemInstance[] unequiped = activeChar.getInventory().unEquipItemInSlotAndRecord(itemToRemove.getEquipSlot());
			InventoryUpdate iu = new InventoryUpdate();

			for(L2ItemInstance element : unequiped)
			{
				iu.addModifiedItem(element);
			}
			activeChar.sendPacket(iu);
		}

		L2ItemInstance removedItem = activeChar.getInventory().destroyItem("Crystalize", _objectId, _count, activeChar, null);

		int crystalId = itemToRemove.getItem().getCrystalItemId();
		int crystalAmount = itemToRemove.getCrystalCount();
		L2ItemInstance createditem = activeChar.getInventory().addItem("Crystalize", crystalId, crystalAmount, activeChar, itemToRemove);

		activeChar.sendPacket(new SystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(crystalId).addNumber(crystalAmount));

		if(!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate iu = new InventoryUpdate();
			if(removedItem.getCount() == 0)
			{
				iu.addRemovedItem(removedItem);
			}
			else
			{
				iu.addModifiedItem(removedItem);
			}

			if(createditem.getCount() != crystalAmount)
			{
				iu.addModifiedItem(createditem);
			}
			else
			{
				iu.addNewItem(createditem);
			}

			activeChar.sendPacket(iu);
		}
		else
		{
			activeChar.sendPacket(new ItemList(activeChar, false));
		}

		StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
		activeChar.sendPacket(su);

		activeChar.broadcastUserInfo();

		L2World world = L2World.getInstance();
		world.removeObject(removedItem);

		activeChar.setInCrystallize(false);
	}

	@Override
	public String getType()
	{
		return _C__72_REQUESTDCRYSTALLIZEITEM;
	}

}