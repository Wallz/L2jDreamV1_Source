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

import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.item.L2Item;

public class RequestUnEquipItem extends L2GameClientPacket
{
	private static final String _C__11_REQUESTUNEQUIPITEM = "[C] 11 RequestUnequipItem";

	private int _slot;

	@Override
	protected void readImpl()
	{
		_slot = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}

		L2ItemInstance item = activeChar.getInventory().getPaperdollItemByL2ItemId(_slot);
		if (item != null && item.isWear())
		{
			return;
		}

		if (_slot == L2Item.SLOT_LR_HAND && activeChar.isCursedWeaponEquiped())
		{
			return;
		}

		if (activeChar.isStunned() || activeChar.isConfused() || activeChar.isParalyzed() || activeChar.isSleeping() || activeChar.isAlikeDead())
		{
			activeChar.sendMessage("Your status does not allow you to do that.");
			return;
		}

		if (activeChar.isCastingNow() || activeChar.isCastingSimultaneouslyNow())
		{
			return;
		}
		
		if (!activeChar.getInventory().canManipulateWithItemId(item.getItemId()))
		{
			return;
		}
		
		if (activeChar.isFightingInEvent())
		{
			if (activeChar.getEventName().equals("CTF") && activeChar._CTFHaveFlagOfTeam > 0)
				return;
		}
		
		if (item != null && item.isAugmented())
		{
			item.getAugmentation().removeBoni(activeChar);
		}

		L2ItemInstance[] unequiped = activeChar.getInventory().unEquipItemInBodySlotAndRecord(_slot);

		InventoryUpdate iu = new InventoryUpdate();

		for (L2ItemInstance element : unequiped)
		{
			activeChar.checkSSMatch(null, element);

			iu.addModifiedItem(element);
		}

		activeChar.sendPacket(iu);
		activeChar.broadcastUserInfo();

		if (unequiped.length > 0)
		{
			if(unequiped[0].getEnchantLevel() > 0)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED).addNumber(unequiped[0].getEnchantLevel()).addItemName(unequiped[0].getItemId()));
			}
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_DISARMED).addItemName(unequiped[0].getItemId()));
			}
		}
	}

	@Override
	public String getType()
	{
		return _C__11_REQUESTUNEQUIPITEM;
	}

}