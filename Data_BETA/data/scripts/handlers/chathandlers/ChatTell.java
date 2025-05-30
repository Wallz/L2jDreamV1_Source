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
package handlers.chathandlers;

import com.src.Config;
import com.src.gameserver.handler.IChatHandler;
import com.src.gameserver.model.BlockList;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.CreatureSay;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class ChatTell implements IChatHandler
{
	private static final int[] COMMAND_IDS = { 2 };

	public void handleChat(int type, L2PcInstance activeChar, String target, String text)
	{
		if(target == null)
		{
			return;
		}

		CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
		L2PcInstance receiver = null;

		receiver = L2World.getInstance().getPlayer(target);

		if(receiver != null && !BlockList.isBlocked(receiver, activeChar) || activeChar.isGM() && receiver != null)
		{
			if(Config.JAIL_DISABLE_CHAT && receiver.isInJail())
			{
				activeChar.sendMessage("Player is in jail.");
				return;
			}

			if(receiver.isChatBanned())
			{
				activeChar.sendMessage("Player is chat banned.");
				return;
			}

			if(receiver.isOffline())
			{
				activeChar.sendMessage("Player is in offline mode.");
				return;
			}

			if(!receiver.getMessageRefusal())
			{
				receiver.sendPacket(cs);
				activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(),  type, "->" + receiver.getName(), text));
			}
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.THE_PERSON_IS_IN_MESSAGE_REFUSAL_MODE));
			}
		}
		else if(receiver != null && BlockList.isBlocked(receiver, activeChar))
        {
                SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_ADDED_YOU_TO_IGNORE_LIST);
                sm.addString(target);
                activeChar.sendPacket(sm);
                sm = null;
        }
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_IS_NOT_ONLINE).addString(target));
		}
	}

	public int[] getChatTypeList()
	{
		return COMMAND_IDS;
	}

}