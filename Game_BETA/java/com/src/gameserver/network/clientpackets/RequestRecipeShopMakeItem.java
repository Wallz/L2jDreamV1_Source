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

import com.src.gameserver.RecipeController;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.util.Util;

public final class RequestRecipeShopMakeItem extends L2GameClientPacket
{
	private static final String _C__AF_REQUESTRECIPESHOPMAKEITEM = "[C] B6 RequestRecipeShopMakeItem";

	private int _id;
	private int _recipeId;
	@SuppressWarnings("unused")
	private int _unknow;

	@Override
	protected void readImpl()
	{
		_id = readD();
		_recipeId = readD();
		_unknow = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		L2PcInstance manufacturer = (L2PcInstance) L2World.getInstance().findObject(_id);
		if(manufacturer == null)
			return;

		if(activeChar.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE)
		{
			activeChar.sendMessage("Cannot make items while trading");
			return;
		}

		if(manufacturer.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_MANUFACTURE)
			return;

		if(activeChar.isInCraftMode() || manufacturer.isInCraftMode())
		{
			activeChar.sendMessage("Currently in Craft Mode");
			return;
		}

		if(manufacturer.isInDuel() || activeChar.isInDuel())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_CRAFT_DURING_COMBAT));
			return;
		}

		if(Util.checkIfInRange(150, activeChar, manufacturer, true))
		{
			RecipeController.getInstance().requestManufactureItem(manufacturer, _recipeId, activeChar);
		}
	}

	@Override
	public String getType()
	{
		return _C__AF_REQUESTRECIPESHOPMAKEITEM;
	}
}