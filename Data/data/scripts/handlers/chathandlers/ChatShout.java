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
import com.src.gameserver.datatables.xml.MapRegionTable;
import com.src.gameserver.handler.IChatHandler;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.CreatureSay;
import com.src.gameserver.util.FloodProtector;

public class ChatShout implements IChatHandler
{
	private static final int[] COMMAND_IDS = { 1 };

	public void handleChat(int type, L2PcInstance activeChar, String target, String text)
	{
		CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);

		if(Config.DEFAULT_GLOBAL_CHAT.equalsIgnoreCase("on") || (Config.DEFAULT_GLOBAL_CHAT.equalsIgnoreCase("gm") && activeChar.isGM()))
		{
			int region = MapRegionTable.getInstance().getMapRegion(activeChar.getX(), activeChar.getY());
			for(L2PcInstance player : L2World.getInstance().getAllPlayers())
			{
				if(region == MapRegionTable.getInstance().getMapRegion(player.getX(),player.getY()))
				{
					player.sendPacket(cs);
				}
			}
		}
		else if(Config.DEFAULT_GLOBAL_CHAT.equalsIgnoreCase("global"))
		{
			if(!FloodProtector.getInstance().tryPerformAction(activeChar.getObjectId(), FloodProtector.PROTECTED_GLOBALCHAT))
			{
				activeChar.sendMessage("You cant speak in the global channel too fast.");
				return;
			}
			for(L2PcInstance player : L2World.getInstance().getAllPlayers())
			{
				player.sendPacket(cs);
			}
		}
	}

	public int[] getChatTypeList()
	{
		return COMMAND_IDS;
	}

}