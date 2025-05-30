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

import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ExVariationCancelResult;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.item.L2Item;

public final class RequestRefineCancel extends L2GameClientPacket
{
	private static final String _C__D0_2E_REQUESTREFINECANCEL = "[C] D0:2E RequestRefineCancel";

	private int _targetItemObjId;

	@Override
	protected void readImpl()
	{
		_targetItemObjId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		L2ItemInstance targetItem = (L2ItemInstance) L2World.getInstance().findObject(_targetItemObjId);

		if(activeChar == null)
			return;

		if(targetItem == null)
		{
			activeChar.sendPacket(new ExVariationCancelResult(0));
			return;
		}

		if(!targetItem.isAugmented())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM));
			activeChar.sendPacket(new ExVariationCancelResult(0));
			return;
		}

		int price = 0;
		switch(targetItem.getItem().getCrystalType())
		{
			case L2Item.CRYSTAL_C:
				if(targetItem.getCrystalCount() < 1720)
				{
					price = 95000;
				}
				else if(targetItem.getCrystalCount() < 2452)
				{
					price = 150000;
				}
				else
				{
					price = 210000;
				}
				break;
			case L2Item.CRYSTAL_B:
				if(targetItem.getCrystalCount() < 1746)
				{
					price = 240000;
				}
				else
				{
					price = 270000;
				}
				break;
			case L2Item.CRYSTAL_A:
				if(targetItem.getCrystalCount() < 2160)
				{
					price = 330000;
				}
				else if(targetItem.getCrystalCount() < 2824)
				{
					price = 390000;
				}
				else
				{
					price = 420000;
				}
				break;
			case L2Item.CRYSTAL_S:
				price = 480000;
				break;
			default:
				activeChar.sendPacket(new ExVariationCancelResult(0));
				return;
		}

		if(!activeChar.reduceAdena("RequestRefineCancel", price, null, true))
			return;

		if(targetItem.isEquipped())
		{
			activeChar.disarmWeapons();
		}

		targetItem.removeAugmentation();
		activeChar.sendPacket(new ExVariationCancelResult(1));

		InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(targetItem);
		activeChar.sendPacket(iu);

		activeChar.sendPacket(new SystemMessage(SystemMessageId.AUGMENTATION_HAS_BEEN_SUCCESSFULLY_REMOVED_FROM_YOUR_S1).addString(targetItem.getItemName()));
	}

	@Override
	public String getType()
	{
		return _C__D0_2E_REQUESTREFINECANCEL;
	}
}