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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.thread.ThreadPoolManager;

public final class L2CastleTeleporterInstance extends L2Npc
{
	public static final Logger _log = Logger.getLogger(L2CastleTeleporterInstance.class.getName());

	private boolean _currentTask = false;

	public L2CastleTeleporterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken();

		if(actualCommand.equalsIgnoreCase("tele"))
		{
			int delay;
			if(!getTask())
			{
				if(getCastle().getSiege().getIsInProgress() && getCastle().getSiege().getControlTowerCount() == 0)
				{
					delay = 480000;
				}
				else
				{
					delay = 30000;
				}

				setTask(true);
				ThreadPoolManager.getInstance().scheduleGeneral(new oustAllPlayers(), delay );
			}

			String filename = "data/html/castleteleporter/mass-gk-1.htm";
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(filename);
			player.sendPacket(html);
			return;
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}

	@Override
	public void showChatWindow(L2PcInstance player)
	{
		String filename;
		if(!getTask())
		{
			if(getCastle().getSiege().getIsInProgress() && getCastle().getSiege().getControlTowerCount() == 0)
			{
				filename = "data/html/castleteleporter/mass-gk-2.htm";
			}
			else
			{
				filename = "data/html/castleteleporter/mass-gk.htm";
			}
		}
		else
		{
			filename = "data/html/castleteleporter/mass-gk-1.htm";
		}

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}

	void oustAllPlayers()
	{
		getCastle().oustAllPlayers();
	}

	class oustAllPlayers implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				oustAllPlayers();
				setTask(false);
			}
			catch(NullPointerException e)
			{
				_log.log(Level.WARNING, "" + e.getMessage(), e);
			}
		}
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