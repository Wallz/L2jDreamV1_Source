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
import com.src.gameserver.managers.PetitionManager;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class ChatPetition implements IChatHandler
{
	private static final int[] COMMAND_IDS = { 6, 7 };

	public void handleChat(int type, L2PcInstance activeChar, String target, String text)
	{
		if(!PetitionManager.getInstance().isPlayerInConsultation(activeChar))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_IN_PETITION_CHAT));
			return;
		}

		PetitionManager.getInstance().sendActivePetitionMessage(activeChar, text);
	}

	public int[] getChatTypeList()
	{
		return COMMAND_IDS;
	}

}