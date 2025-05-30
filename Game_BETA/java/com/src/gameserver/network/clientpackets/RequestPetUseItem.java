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

import com.src.gameserver.datatables.xml.L2PetDataTable;
import com.src.gameserver.handler.IItemHandler;
import com.src.gameserver.handler.ItemHandler;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.PetInfo;
import com.src.gameserver.network.serverpackets.PetItemList;
import com.src.gameserver.network.serverpackets.SystemMessage;

public final class RequestPetUseItem extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestPetUseItem.class.getName());

	private static final String _C__8A_REQUESTPETUSEITEM = "[C] 8a RequestPetUseItem";

	private int _objectId;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if(activeChar == null)
		{
			return;
		}

		L2PetInstance pet = (L2PetInstance) activeChar.getPet();

		if(pet == null)
		{
			return;
		}

		L2ItemInstance item = pet.getInventory().getItemByObjectId(_objectId);

		if(item == null)
		{
			return;
		}

		if(item.isWear())
		{
			return;
		}

		int itemId = item.getItemId();

		if(activeChar.isAlikeDead() || pet.isDead())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addItemName(item.getItemId()));
			return;
		}

		if(item.isEquipable())
		{
			if(L2PetDataTable.isWolf(pet.getNpcId()) && item.getItem().isForWolf())
			{
				useItem(pet, item, activeChar);
				return;
			}
			else if(L2PetDataTable.isHatchling(pet.getNpcId()) && item.getItem().isForHatchling())
			{
				useItem(pet, item, activeChar);
				return;
			}
			else if(L2PetDataTable.isStrider(pet.getNpcId()) && item.getItem().isForStrider())
			{
				useItem(pet, item, activeChar);
				return;
			}
			else if(L2PetDataTable.isBaby(pet.getNpcId()) && item.getItem().isForBabyPet())
			{
				useItem(pet, item, activeChar);
				return;
			}
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.ITEM_NOT_FOR_PETS));
				return;
			}
		}
		else if(L2PetDataTable.isPetFood(itemId))
		{
			if(L2PetDataTable.isWolf(pet.getNpcId()) && L2PetDataTable.isWolfFood(itemId))
			{
				feed(activeChar, pet, item);
				return;
			}

			if(L2PetDataTable.isSinEater(pet.getNpcId()) && L2PetDataTable.isSinEaterFood(itemId))
			{
				feed(activeChar, pet, item);
				return;
			}
			else if(L2PetDataTable.isHatchling(pet.getNpcId()) && L2PetDataTable.isHatchlingFood(itemId))
			{
				feed(activeChar, pet, item);
				return;
			}
			else if(L2PetDataTable.isStrider(pet.getNpcId()) && L2PetDataTable.isStriderFood(itemId))
			{
				feed(activeChar, pet, item);
				return;
			}
			else if(L2PetDataTable.isWyvern(pet.getNpcId()) && L2PetDataTable.isWyvernFood(itemId))
			{
				feed(activeChar, pet, item);
				return;
			}
			else if(L2PetDataTable.isBaby(pet.getNpcId()) && L2PetDataTable.isBabyFood(itemId))
			{
				feed(activeChar, pet, item);
			}
		}

		IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getItemId());

		if(handler != null)
		{
			useItem(pet, item, activeChar);
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.ITEM_NOT_FOR_PETS));
		}

		return;
	}

	private synchronized void useItem(L2PetInstance pet, L2ItemInstance item, L2PcInstance activeChar)
	{
		if(item.isEquipable())
		{
			if(item.isEquipped())
			{
				pet.getInventory().unEquipItemInSlot(item.getEquipSlot());
			}
			else
			{
				pet.getInventory().equipItem(item);
			}

			PetItemList pil = new PetItemList(pet);
			activeChar.sendPacket(pil);

			PetInfo pi = new PetInfo(pet);
			activeChar.sendPacket(pi);

			pet.updateEffectIcons(true);
		}
		else
		{
			IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getItemId());

			if(handler == null)
			{
				_log.warning("no itemhandler registered for itemId:" + item.getItemId());
			}
			else
			{
				handler.useItem(pet, item);
			}
		}
	}

	private void feed(L2PcInstance player, L2PetInstance pet, L2ItemInstance item)
	{
		if(pet.destroyItem("Feed", item.getObjectId(), 1, pet, false))
		{
			pet.setCurrentFed(pet.getCurrentFed() + 100);
		}

		pet.broadcastStatusUpdate();
	}

	@Override
	public String getType()
	{
		return _C__8A_REQUESTPETUSEITEM;
	}

}