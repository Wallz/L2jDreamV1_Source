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

import com.src.gameserver.handler.IItemHandler;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ChooseInventoryItem;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class EnchantScrolls implements IItemHandler
{
	private static final int[] ITEM_IDS =
	{
			729, 730, 731, 732, 6569, 6570,
			947,
			948,
			949,
			950,
			6571,
			6572,
			951,
			952,
			953,
			954,
			6573,
			6574,
			955,
			956,
			957,
			958,
			6575,
			6576,
			959,
			960,
			961,
			962,
			6577,
			6578
	};

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(!(playable instanceof L2PcInstance))
		{
			return;
		}

		L2PcInstance activeChar = (L2PcInstance) playable;
		if(activeChar.isCastingNow())
		{
			return;
		}

		activeChar.setActiveEnchantItem(item);
		activeChar.sendPacket(new SystemMessage(SystemMessageId.SELECT_ITEM_TO_ENCHANT));
		activeChar.sendPacket(new ChooseInventoryItem(item.getItemId()));
		return;
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}

}