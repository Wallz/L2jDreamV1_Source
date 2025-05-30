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

import com.src.gameserver.handler.IChatHandler;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.CreatureSay;

public class ChatPartyRoomCommander implements IChatHandler
{
	private static final int[] COMMAND_IDS = { 15 };

	public void handleChat(int type, L2PcInstance activeChar, String target, String text)
	{
		if(activeChar.isInParty())
		{
			if(activeChar.getParty().isInCommandChannel() && activeChar.getParty().getCommandChannel().getChannelLeader().equals(activeChar))
			{
				CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
				activeChar.getParty().getCommandChannel().broadcastToChannelMembers(cs);
			}
		}
	}

	public int[] getChatTypeList()
	{
		return COMMAND_IDS;
	}

}