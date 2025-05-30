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

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.MyTargetSelected;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.ValidateLocation;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public class L2CastleBlacksmithInstance extends L2NpcInstance
{
	protected static final int COND_ALL_FALSE = 0;
	protected static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	protected static final int COND_OWNER = 2;

	public L2CastleBlacksmithInstance(int objectId, L2NpcTemplate template)
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
				showMessageWindow(player, 0);
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
			if(command.startsWith("Chat"))
			{
				int val = 0;
				try
				{
					val = Integer.parseInt(command.substring(5));
				}
				catch(IndexOutOfBoundsException ioobe)
				{
				}
				catch(NumberFormatException nfe)
				{
				}
				showMessageWindow(player, val);
			}
			else
			{
				super.onBypassFeedback(player, command);
			}
		}
	}

	private void showMessageWindow(L2PcInstance player, int val)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		String filename = "data/html/castleblacksmith/castleblacksmith-no.htm";

		int condition = validateCondition(player);
		if(condition > COND_ALL_FALSE)
		{
			if(condition == COND_BUSY_BECAUSE_OF_SIEGE)
			{
				filename = "data/html/castleblacksmith/castleblacksmith-busy.htm";
			}
			else if(condition == COND_OWNER)
			{
				if(val == 0)
				{
					filename = "data/html/castleblacksmith/castleblacksmith.htm";
				}
				else
				{
					filename = "data/html/castleblacksmith/castleblacksmith-" + val + ".htm";
				}
			}
		}

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		html.replace("%castleid%", Integer.toString(getCastle().getCastleId()));
		player.sendPacket(html);
		filename = null;
		html = null;
	}

	protected int validateCondition(L2PcInstance player)
	{
		if(player.isGM())
		{
			return COND_OWNER;
		}

		if(getCastle() != null && getCastle().getCastleId() > 0)
		{
			if(player.getClan() != null)
			{
				if(getCastle().getSiege().getIsInProgress())
				{
					return COND_BUSY_BECAUSE_OF_SIEGE;
				}
				else if(getCastle().getOwnerId() == player.getClanId() && (player.getClanPrivileges() & L2Clan.CP_CS_MANOR_ADMIN) == L2Clan.CP_CS_MANOR_ADMIN)
				{
					return COND_OWNER;
				}
			}
		}
		return COND_ALL_FALSE;
	}

}