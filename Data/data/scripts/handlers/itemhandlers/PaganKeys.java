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
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2DoorInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.PlaySound;
import com.src.gameserver.network.serverpackets.SocialAction;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.util.random.Rnd;

public class PaganKeys implements IItemHandler
{
	private static final int[] ITEM_IDS =
	{
			8273, 8274, 8275
	};
	public static final int INTERACTION_DISTANCE = 100;

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

		if(!activeChar.isInsideRadius(door, INTERACTION_DISTANCE, false, false))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.FAILED_TO_UNLOCK_DOOR));
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(activeChar.getAbnormalEffect() > 0 || activeChar.isInCombat())
		{
			activeChar.sendMessage("You cannot use the key now.");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		int openChance = 35;

		if(!playable.destroyItem("Consume", item.getObjectId(), 1, null, false))
			return;

		switch(itemId)
		{
			case 8273:
				if(door.getDoorName().startsWith("Anteroom"))
				{
					if(openChance > 0 && Rnd.get(100) < openChance)
					{
						door.openMe();
						door.onOpen();
						activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 3));
					}
					else
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.FAILED_TO_UNLOCK_DOOR));
						activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 13));
						PlaySound playSound = new PlaySound("interfacesound.system_close_01");
						activeChar.sendPacket(playSound);
					}
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				}
				break;
			case 8274:
				if(door.getDoorName().startsWith("Altar_Entrance"))
				{
					if(openChance > 0 && Rnd.get(100) < openChance)
					{
						door.openMe();
						door.onOpen();
						activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 3));
					}
					else
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.FAILED_TO_UNLOCK_DOOR));
						activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 13));
						PlaySound playSound = new PlaySound("interfacesound.system_close_01");
						activeChar.sendPacket(playSound);
					}
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				}
				break;
			case 8275:
				if(door.getDoorName().startsWith("Door_of_Darkness"))
				{
					if(openChance > 0 && Rnd.get(100) < openChance)
					{
						door.openMe();
						door.onOpen();
						activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 3));
					}
					else
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.FAILED_TO_UNLOCK_DOOR));
						activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 13));
						PlaySound playSound = new PlaySound("interfacesound.system_close_01");
						activeChar.sendPacket(playSound);
					}
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				}
				break;
		}
		activeChar = null;
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}

}