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

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.datatables.xml.MapRegionTable;
import com.src.gameserver.model.L2World;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.CreatureSay;
import com.src.gameserver.network.serverpackets.MyTargetSelected;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.SocialAction;
import com.src.gameserver.network.serverpackets.ValidateLocation;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.random.Rnd;


/**
 * @author Kerberos
 */
public final class L2MassGatekeeperInstance extends L2NpcInstance
{
	private boolean	_currentTask	= false;

	/**
	* @param template
	*/
	public L2MassGatekeeperInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command

		if (actualCommand.equalsIgnoreCase("tele"))
		{
			int delay;
			if (!getTask())
			{
				if (getCastle().getSiege().getIsInProgress())
					delay = getCastle().getSiege().getDefenderRespawnDelay();
				else
					delay = 30000;

				setTask(true);
				ThreadPoolManager.getInstance().scheduleGeneral(new oustAllPlayers(), delay);
			}

			String filename = "data/html/castleteleporter/mass-gk-1.htm";
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(filename);
			player.sendPacket(html);
		}
		else
			super.onBypassFeedback(player, command);
	}

	@Override
	public void showChatWindow(L2PcInstance player)
	{
		String filename;
		if (!getTask())
		{
			if (getCastle().getSiege().getIsInProgress() && getCastle().getSiege().getDefenderRespawnDelay() > 0)
				filename = "data/html/castleteleporter/mass-gk-2.htm";
			else
				filename = "data/html/castleteleporter/mass-gk.htm";
		}
		else
			filename = "data/html/castleteleporter/mass-gk-1.htm";

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}

	void oustAllPlayers()
	{
		getCastle().getTeleZone().oustAllPlayers();
	}

	class oustAllPlayers implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				CreatureSay cs = new CreatureSay(getObjectId(), 1, getName(), "Castle defenders of " + getCastle().getName() + " will be move to throne hall.");
				int region = MapRegionTable.getInstance().getMapRegion(getX(), getY());
				{
					for (L2PcInstance player : L2World.getInstance().getAllPlayers())
					{
						if (region == MapRegionTable.getInstance().getMapRegion(player.getX(), player.getY()))
							player.sendPacket(cs);
					}
				}
				oustAllPlayers();
				setTask(false);
			}
			catch (NullPointerException e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	* this is called when a player interacts with this NPC
	* @param player
	*/
	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player))
			return;

		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);

			// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			player.sendPacket(new ValidateLocation(this));
			// Calculate the distance between the L2PcInstance and the L2NpcInstance
			if (!canInteract(player))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				// Send a Server->Client packet SocialAction to the all L2PcInstance on the _knownPlayer of the L2NpcInstance
				// to display a social action of the L2NpcInstance on their client
				SocialAction sa = new SocialAction(getObjectId(), Rnd.get(8));
				broadcastPacket(sa);
				sa = null;

				showChatWindow(player);
			}
		}
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	public boolean getTask()
	{
		return _currentTask;
	}

	public void setTask(boolean state)
	{
		_currentTask = state;
	}
}