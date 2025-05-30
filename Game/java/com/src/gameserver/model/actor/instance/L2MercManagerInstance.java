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

import com.src.gameserver.TradeController;
import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.L2TradeList;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.BuyList;
import com.src.gameserver.network.serverpackets.MyTargetSelected;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.ValidateLocation;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public final class L2MercManagerInstance extends L2NpcInstance
{
	private static final int COND_ALL_FALSE = 0;
	private static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	private static final int COND_OWNER = 2;

	public L2MercManagerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if(!canTarget(player))
		{
			return;
		}
		player.setLastFolkNPC(this);

		if(this != player.getTarget())
		{
			player.setTarget(this);

			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
			my = null;

			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			if(!canInteract(player))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				showMessageWindow(player);
			}
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		int condition = validateCondition(player);
		if(condition <= COND_ALL_FALSE)
		{
			return;
		}

		if(condition == COND_BUSY_BECAUSE_OF_SIEGE)
		{
			return;
		}
		else if(condition == COND_OWNER)
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			String actualCommand = st.nextToken();

			String val = "";
			if(st.countTokens() >= 1)
			{
				val = st.nextToken();
			}

			if(actualCommand.equalsIgnoreCase("hire"))
			{
				if(val == "")
				{
					return;
				}

				showBuyWindow(player, Integer.parseInt(val));
				return;
			}
			st = null;
			actualCommand = null;
		}

		super.onBypassFeedback(player, command);
	}

	private void showBuyWindow(L2PcInstance player, int val)
	{
		player.tempInventoryDisable();
		L2TradeList list = TradeController.getInstance().getBuyList(val);
		if(list != null && list.getNpcId().equals(String.valueOf(getNpcId())))
		{
			BuyList bl = new BuyList(list, player.getAdena(), 0);
			player.sendPacket(bl);
			list = null;
			bl = null;
		}
		else
		{
			_log.warning("possible client hacker: " + player.getName() + " attempting to buy from GM shop! (L2MercManagerIntance)");
			_log.warning("buylist id:" + val);
		}
	}

	public void showMessageWindow(L2PcInstance player)
	{
		String filename = "data/html/mercmanager/mercmanager-no.htm";

		int condition = validateCondition(player);
		if(condition == COND_BUSY_BECAUSE_OF_SIEGE)
		{
			filename = "data/html/mercmanager/mercmanager-busy.htm";
		}
		else if(condition == COND_OWNER)
		{
			filename = "data/html/mercmanager/mercmanager.htm";
		}

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
		filename = null;
		html = null;
	}

	private int validateCondition(L2PcInstance player)
	{
		if(getCastle() != null && getCastle().getCastleId() > 0)
		{
			if(player.getClan() != null)
			{
				if(getCastle().getSiege().getIsInProgress())
				{
					return COND_BUSY_BECAUSE_OF_SIEGE;
				}
				else if(getCastle().getOwnerId() == player.getClanId())
				{
					if((player.getClanPrivileges() & L2Clan.CP_CS_MERCENARIES) == L2Clan.CP_CS_MERCENARIES)
					{
						return COND_OWNER;
					}
				}
			}
		}

		return COND_ALL_FALSE;
	}

}