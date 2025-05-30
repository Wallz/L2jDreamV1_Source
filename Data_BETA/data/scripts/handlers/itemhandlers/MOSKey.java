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
package handlers.itemhandlers;

import com.src.gameserver.datatables.xml.DoorTable;
import com.src.gameserver.handler.IItemHandler;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2DoorInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.SocialAction;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class MOSKey implements IItemHandler
{
	private static final int[] ITEM_IDS =
	{
		8056
	};
	public static final int INTERACTION_DISTANCE = 150;
	public static long LAST_OPEN = 0;

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		int itemId = item.getItemId();

		if(!(playable instanceof L2PcInstance))
		{
			return;
		}

		L2PcInstance activeChar = (L2PcInstance) playable;
		L2Object target = activeChar.getTarget();

		if(!(target instanceof L2DoorInstance))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		L2DoorInstance door = (L2DoorInstance) target;

		target = null;
		
		if (door.getOpen())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(!activeChar.isInsideRadius(door, INTERACTION_DISTANCE, false, false))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED));
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(activeChar.getAbnormalEffect() > 0 || activeChar.isInCombat())
		{
			activeChar.sendMessage("You are currently enganged in combat.");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(LAST_OPEN + 1800000 > System.currentTimeMillis())
		{
			activeChar.sendMessage("You cannot use the key now.");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(!playable.destroyItem("Consume", item.getObjectId(), 1, null, false))
		{
			return;
		}

		if(itemId == 8056)
		{
			if(door.getDoorId() == 23150003 || door.getDoorId() == 23150004)
			{
				DoorTable.getInstance().getDoor(23150003).openMe();
				DoorTable.getInstance().getDoor(23150004).openMe();
				DoorTable.getInstance().getDoor(23150003).onOpen();
				DoorTable.getInstance().getDoor(23150004).onOpen();
				activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 3));
				LAST_OPEN = System.currentTimeMillis();
			}
		}
		activeChar = null;
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}

}