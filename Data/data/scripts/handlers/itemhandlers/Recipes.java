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
package handlers.itemhandlers;

import com.src.gameserver.datatables.xml.RecipeTable;
import com.src.gameserver.handler.IItemHandler;
import com.src.gameserver.model.L2RecipeList;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class Recipes implements IItemHandler
{
	private final int[] ITEM_IDS;

	public Recipes()
	{
		RecipeTable rc = RecipeTable.getInstance();
		ITEM_IDS = new int[rc.getRecipesCount()];
		for(int i = 0; i < rc.getRecipesCount(); i++)
		{
			ITEM_IDS[i] = rc.getRecipeList(i).getRecipeId();
		}
	}

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(!(playable instanceof L2PcInstance))
		{
			return;
		}

		L2PcInstance activeChar = (L2PcInstance) playable;
		L2RecipeList rp = RecipeTable.getInstance().getRecipeByItemId(item.getItemId());
		if(activeChar.hasRecipeList(rp.getId()))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.RECIPE_ALREADY_REGISTERED));
		}
		else
		{
			if(rp.isDwarvenRecipe())
			{
				if(activeChar.hasDwarvenCraft())
				{
					if(rp.getLevel() > activeChar.getDwarvenCraft())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.CREATE_LVL_TOO_LOW_TO_REGISTER));
					}
					else if(activeChar.getDwarvenRecipeBook().length >= activeChar.GetDwarfRecipeLimit())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.UP_TO_S1_RECIPES_CAN_REGISTER).addNumber(activeChar.GetDwarfRecipeLimit()));
					}
					else
					{
						activeChar.registerDwarvenRecipeList(rp);
						activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
						activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("Added recipe \"" + rp.getRecipeName() + "\" to Dwarven RecipeBook"));
					}
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_REGISTER_NO_ABILITY_TO_CRAFT));
				}
			}
			else
			{
				if(activeChar.hasCommonCraft())
				{
					if(rp.getLevel() > activeChar.getCommonCraft())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.CREATE_LVL_TOO_LOW_TO_REGISTER));
					}
					else if(activeChar.getCommonRecipeBook().length >= activeChar.GetCommonRecipeLimit())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.UP_TO_S1_RECIPES_CAN_REGISTER).addNumber(activeChar.GetCommonRecipeLimit()));
					}
					else
					{
						activeChar.registerCommonRecipeList(rp);
						activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
						activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("Added recipe \"" + rp.getRecipeName() + "\" to Common RecipeBook"));
					}
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_REGISTER_NO_ABILITY_TO_CRAFT));
				}
			}
		}
		activeChar = null;
		rp = null;
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}

}