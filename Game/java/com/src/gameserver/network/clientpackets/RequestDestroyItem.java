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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.src.Config;
import com.src.gameserver.datatables.xml.L2PetDataTable;
import com.src.gameserver.managers.CursedWeaponsManager;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.util.Util;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public final class RequestDestroyItem extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestDestroyItem.class.getName());

	private static final String _C__59_REQUESTDESTROYITEM = "[C] 59 RequestDestroyItem";

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
			return;
		}

		if(_count <= 0)
		{
			if(_count < 0)
			{
				Util.handleIllegalPlayerAction(activeChar, "[RequestDestroyItem] count < 0! ban! oid: " + _objectId + " owner: " + activeChar.getName(), Config.DEFAULT_PUNISH);
			}
			return;
		}

		int count = _count;

		if(activeChar.isProcessingTransaction() || activeChar.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE));
			return;
		}

		L2ItemInstance itemToRemove = activeChar.getInventory().getItemByObjectId(_objectId);

		if(itemToRemove == null)
		{
			return;
		}

		if(itemToRemove.fireEvent("DESTROY", (Object[]) null) != null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISCARD_THIS_ITEM));
			return;
		}

		if(activeChar.isCastingNow())
		{
			if(activeChar.getCurrentSkill() != null && activeChar.getCurrentSkill().getSkill().getItemConsumeId() == itemToRemove.getItemId())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISCARD_THIS_ITEM));
				return;
			}
		}

		int itemId = itemToRemove.getItemId();

		if(itemToRemove == null || itemToRemove.isWear() || CursedWeaponsManager.getInstance().isCursed(itemId))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISCARD_THIS_ITEM));
			return;
		}

		if(!itemToRemove.isDestroyable() && !activeChar.isGM())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISCARD_THIS_ITEM));
			return;
		}

		if(!itemToRemove.isStackable() && count > 1)
		{
			Util.handleIllegalPlayerAction(activeChar, "[RequestDestroyItem] count > 1 but item is not stackable! oid: " + _objectId + " owner: " + activeChar.getName(), Config.DEFAULT_PUNISH);
			return;
		}

		if (!activeChar.getInventory().canManipulateWithItemId(itemId))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_USE_ON_YOURSELF));
			return;
		}
		
		if(_count > itemToRemove.getCount())
		{
			count = itemToRemove.getCount();
		}

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
				activeChar.checkSSMatch(null, element);
				iu.addModifiedItem(element);
			}

			activeChar.sendPacket(iu);
			activeChar.broadcastUserInfo();
		}

		if(L2PetDataTable.isPetItem(itemId))
		{
			Connection con = null;
			try
			{
				if(activeChar.getPet() != null && activeChar.getPet().getControlItemId() == _objectId)
				{
					activeChar.getPet().unSummon(activeChar);
				}

				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id = ?");
				statement.setInt(1, _objectId);
				statement.execute();
				ResourceUtil.closeStatement(statement);
			}
			catch(Exception e)
			{
				_log.log(Level.WARNING, "could not delete pet objectid: ", e);
			}
			finally
			{
				ResourceUtil.closeConnection(con); 
			}
		}

		L2ItemInstance removedItem = activeChar.getInventory().destroyItem("Destroy", _objectId, count, activeChar, null);

		if(removedItem == null)
		{
			return;
		}

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

			activeChar.sendPacket(iu);
		}
		else
		{
			sendPacket(new ItemList(activeChar, true));
		}

		StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
		activeChar.sendPacket(su);

		L2World world = L2World.getInstance();
		world.removeObject(removedItem);
	}

	@Override
	public String getType()
	{
		return _C__59_REQUESTDESTROYITEM;
	}
}