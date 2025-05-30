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

import java.util.StringTokenizer;

import com.src.gameserver.managers.SiegeManager;
import com.src.gameserver.model.entity.olympiad.Olympiad;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public final class L2ObservationInstance extends L2NpcInstance
{
	public L2ObservationInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if(command.startsWith("observeSiege"))
		{
			String val = command.substring(13);
			StringTokenizer st = new StringTokenizer(val);
			st.nextToken();

			if(Olympiad.getInstance().isRegistered(player) || player.isInOlympiadMode())
			{
				player.sendMessage("You already participated in Olympiad!");
				return;
			}

			if(Olympiad.getInstance().isRegisteredInComp(player))
			{
				return;
			}


			if(player.isInCombat() || player.getPvpFlag() > 0)
			{
				player.sendMessage("You are in combat now!");
				return;
			}

			if(SiegeManager.getInstance().getSiege(Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken())) != null)
			{
				doObserve(player, val);
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessageId.ONLY_VIEW_SIEGE));
			}

			val = null;
			st = null;
		}
		else if(command.startsWith("observe"))
		{
			if(Olympiad.getInstance().isRegistered(player) || player.isInOlympiadMode())
			{
				player.sendMessage("You already participated in Olympiad!");
				return;
			}

			if(Olympiad.getInstance().isRegisteredInComp(player))
			{
				return;
			}

			if(player.isInCombat() || player.getPvpFlag() > 0)
			{
				player.sendMessage("You are in combat now!");
				return;
			}

			doObserve(player, command.substring(8));
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		if(val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}

		return "data/html/observation/" + pom + ".htm";
	}

	private void doObserve(L2PcInstance player, String val)
	{
		StringTokenizer st = new StringTokenizer(val);
		int cost = Integer.parseInt(st.nextToken());
		int x = Integer.parseInt(st.nextToken());
		int y = Integer.parseInt(st.nextToken());
		int z = Integer.parseInt(st.nextToken());
		if(player.reduceAdena("Broadcast", cost, this, true))
		{
			player.enterObserverMode(x, y, z);
			ItemList il = new ItemList(player, false);
			player.sendPacket(il);
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
		st = null;
	}

}