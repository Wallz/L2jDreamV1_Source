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
import com.src.gameserver.datatables.GmListTable;
import com.src.gameserver.managers.CursedWeaponsManager;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.item.L2EtcItemType;
import com.src.gameserver.templates.item.L2Item;
import com.src.gameserver.util.IllegalPlayerAction;
import com.src.gameserver.util.Util;

public final class RequestDropItem extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestDropItem.class.getName());

	private static final String _C__12_REQUESTDROPITEM = "[C] 12 RequestDropItem";

	private int _objectId;
	private int _count;
	private int _x;
	private int _y;
	private int _z;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_count = readD();
		_x = readD();
		_y = readD();
		_z = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if(activeChar == null || activeChar.isDead())
		{
			return;
		}

		L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);

		if(item != null && item.isAugmented())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.AUGMENTED_ITEM_CANNOT_BE_DISCARDED));
			return;
		}

		if(item == null || _count == 0 || !activeChar.validateItemManipulation(_objectId, "drop") || !Config.ALLOW_DISCARDITEM && !activeChar.isGM() || (!item.isDropable() && !(activeChar.isGM() && Config.GM_TRADE_RESTRICTED_ITEMS)))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISCARD_THIS_ITEM));
			return;
		}

		if(item.getItemType() == L2EtcItemType.QUEST && !(activeChar.isGM() && Config.GM_TRADE_RESTRICTED_ITEMS))
		{
			return;
		}

		int itemId = item.getItemId();

		if(CursedWeaponsManager.getInstance().isCursed(itemId))
		{
			return;
		}

		if(activeChar.getActiveEnchantItem() != null)
		{
			sendPacket(SystemMessage.sendString("You can't discard items during enchant."));
			return;
		}

		if(_count > item.getCount())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISCARD_THIS_ITEM));
			return;
		}

		if(Config.PLAYER_SPAWN_PROTECTION > 0 && activeChar.isInvul() && !activeChar.isGM())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISCARD_THIS_ITEM));
			return;
		}

		if(_count <= 0)
		{
			activeChar.setAccessLevel(-1);
			Util.handleIllegalPlayerAction(activeChar, "[RequestDropItem] count <= 0! ban! oid: " + _objectId + " owner: " + activeChar.getName(), IllegalPlayerAction.PUNISH_KICK);
			return;
		}

		if(!item.isStackable() && _count > 1)
		{
			Util.handleIllegalPlayerAction(activeChar, "[RequestDropItem] count > 1 but item is not stackable! ban! oid: " + _objectId + " owner: " + activeChar.getName(), IllegalPlayerAction.PUNISH_KICK);
			return;
		}

		if(!activeChar.getAccessLevel().allowTransaction())
		{
			activeChar.sendMessage("Unsufficient privileges.");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(activeChar.isProcessingTransaction() || activeChar.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE));
			return;
		}

		if(activeChar.isFishing())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DO_WHILE_FISHING_2));
			return;
		}

		if(activeChar.isCastingNow())
		{
			if(activeChar.getCurrentSkill() != null && activeChar.getCurrentSkill().getSkill().getItemConsumeId() == item.getItemId())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISCARD_THIS_ITEM));
				return;
			}
		}

		// Cannot discard item that the skill is consuming
		if (activeChar.isCastingSimultaneouslyNow())
		{
			if (activeChar.getLastSimultaneousSkillCast() != null && activeChar.getLastSimultaneousSkillCast().getItemConsumeId() == item.getItemId())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISCARD_THIS_ITEM));
				return;
			}
		}
				
		if(L2Item.TYPE2_QUEST == item.getItem().getType2() && !activeChar.isGM())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISCARD_EXCHANGE_ITEM));
			return;
		}

		if(!activeChar.isInsideRadius(_x, _y, 150, false) || Math.abs(_z - activeChar.getZ()) > 50)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISCARD_DISTANCE_TOO_FAR));
			return;
		}

		if(item.isEquipped())
		{
			if(item.isAugmented())
			{
				item.getAugmentation().removeBoni(activeChar);
			}

			L2ItemInstance[] unequiped = activeChar.getInventory().unEquipItemInBodySlotAndRecord(item.getItem().getBodyPart());
			InventoryUpdate iu = new InventoryUpdate();

			for(L2ItemInstance element : unequiped)
			{
				activeChar.checkSSMatch(null, element);

				iu.addModifiedItem(element);
			}
			activeChar.sendPacket(iu);
			activeChar.broadcastUserInfo();

			ItemList il = new ItemList(activeChar, true);
			activeChar.sendPacket(il);
		}

		L2ItemInstance dropedItem = activeChar.dropItem("Drop", _objectId, _count, _x, _y, _z, null, false, true);

		if(activeChar.isGM())
		{
			String target = (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target");
			GMAudit.auditGMAction(activeChar.getName(), "drop", target, dropedItem.getItemId() + " - " +dropedItem.getName());
		}

		if(dropedItem != null && dropedItem.getItemId() == 57 && dropedItem.getCount() >= 1000000 && Config.RATE_DROP_ADENA <= 200)
		{
			String msg = "Character (" + activeChar.getName() + ") has dropped (" + dropedItem.getCount() + ")adena at (" + _x + "," + _y + "," + _z + ")";
			_log.warning(msg);
			GmListTable.broadcastMessageToGMs(msg);
		}
	}

	@Override
	public String getType()
	{
		return _C__12_REQUESTDROPITEM;
	}

}