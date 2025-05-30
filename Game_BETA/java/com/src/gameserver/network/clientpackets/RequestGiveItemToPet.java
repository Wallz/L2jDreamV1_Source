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
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.util.IllegalPlayerAction;
import com.src.gameserver.util.Util;

public final class RequestGiveItemToPet extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestGetItemFromPet.class.getName());

	private static final String REQUESTCIVEITEMTOPET__C__8B = "[C] 8B RequestGiveItemToPet";

	private int _objectId;
	private int _amount;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_amount = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if(player == null || !(player.getPet() instanceof L2PetInstance))
		{
			return;
		}

		if(!Config.KARMA_PLAYER_CAN_TRADE && player.getKarma() > 0)
		{
			return;
		}

		if(player.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE)
		{
			player.sendMessage("Cannot exchange items while trading.");
			return;
		}

		if(player.isCastingNow())
		{
			return;
		}

		if(player.getActiveEnchantItem() != null)
		{
			Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " Tried To Use Enchant Exploit And Got Banned!", IllegalPlayerAction.PUNISH_KICKBAN);
			return;
		}

		L2ItemInstance item = player.getInventory().getItemByObjectId(_objectId);

		if(item == null)
		{
			return;
		}

		if(item.isAugmented())
		{
			return;
		}

		if(!item.isDropable() || !item.isDestroyable() || !item.isTradeable())
		{
			sendPacket(new SystemMessage(SystemMessageId.ITEM_NOT_FOR_PETS));
			return;
		}

		L2PetInstance pet = (L2PetInstance) player.getPet();

		if(pet.isDead())
		{
			sendPacket(new SystemMessage(SystemMessageId.CANNOT_GIVE_ITEMS_TO_DEAD_PET));
			return;
		}

		if(_amount < 0)
		{
			return;
		}

		if(!pet.getInventory().validateCapacity(item))
		{
			pet.getOwner().sendMessage("Your pet can't carry any more items.");
			return;
		}
		
		if (item.isHeroItem())
		{
			player.sendMessage("Can not give Hero Itens to Pet");
			return;
		}
		
		if (pet.isDead())
		{
			sendPacket(new SystemMessage(SystemMessageId.CANNOT_GIVE_ITEMS_TO_DEAD_PET));
			return;
		}
		
		if(_amount < 0)
		{
			return;
		}

		if (player.transferItem("Transfer", _objectId, _amount, pet.getInventory(), pet) == null)
		{
			_log.warning("Invalid item transfer request: " + pet.getName() + "(pet) --> " + player.getName());
		}
	}

	@Override
	public String getType()
	{
		return REQUESTCIVEITEMTOPET__C__8B;
	}

}