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

import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.handler.IItemHandler;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2ChestInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class ChestKey implements IItemHandler
{
	public static final int INTERACTION_DISTANCE = 100;

	private static final int[] ITEM_IDS =
	{
			6665, 6666, 6667, 6668, 6669, 6670, 6671, 6672
	};

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(!(playable instanceof L2PcInstance))
		{
			return;
		}

		L2PcInstance activeChar = (L2PcInstance) playable;
		int itemId = item.getItemId();
		L2Skill skill = SkillTable.getInstance().getInfo(2229, itemId - 6664);
		L2Object target = activeChar.getTarget();

		if(!(target instanceof L2ChestInstance) || target == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else
		{
			L2ChestInstance chest = (L2ChestInstance) target;
			if(chest.isDead() || chest.isInteracted())
			{
				activeChar.sendMessage("The chest Is empty.");
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);

				return;
			}
			activeChar.useMagic(skill, false, false);
			chest = null;
		}

		activeChar = null;
		skill = null;
		target = null;
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}

}