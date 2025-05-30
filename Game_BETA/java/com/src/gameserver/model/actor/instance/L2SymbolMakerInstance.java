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
package com.src.gameserver.model.actor.instance;

import javolution.text.TextBuilder;

import com.src.gameserver.datatables.sql.HennaTreeTable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.network.serverpackets.HennaEquipList;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public class L2SymbolMakerInstance extends L2NpcInstance
{
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if(command.equals("Draw"))
		{
			player.sendPacket(new HennaEquipList(player, HennaTreeTable.getInstance().getAvailableHenna(player.getClassId())));
			player.sendPacket(new ItemList(player, false));
		}
		else if(command.equals("RemoveList"))
		{
			showRemoveChat(player);
		}
		else if(command.startsWith("Remove "))
		{
			int slot = Integer.parseInt(command.substring(7));
			player.removeHenna(slot);
			player.sendPacket(new ItemList(player, false));
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}

	private void showRemoveChat(L2PcInstance player)
	{
		TextBuilder html1 = new TextBuilder("<html><body>");
		html1.append("Select symbol you would like to remove:<br><br>");
		boolean hasHennas = false;

		for(int i = 1; i <= 3; i++)
		{
			L2HennaInstance henna = player.getHennas(i);

			if(henna != null)
			{
				hasHennas = true;
				html1.append("<a action=\"bypass -h npc_%objectId%_Remove " + i + "\">" + henna.getName() + "</a><br>");
			}
		}
		if(!hasHennas)
		{
			html1.append("You don't have any symbol to remove!");
		}

		html1.append("</body></html>");
		insertObjectIdAndShowChatWindow(player, html1.toString());
		html1 = null;
	}

	public L2SymbolMakerInstance(int objectID, L2NpcTemplate template)
	{
		super(objectID, template);
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		return "data/html/symbolmaker/SymbolMaker.htm";
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}

}