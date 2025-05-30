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
package com.src.gameserver.model.entity;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import javolution.text.TextBuilder;
import javolution.util.FastList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.cache.HtmCache;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.CreatureSay;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.script.DateRange;
import com.src.gameserver.services.Messages;

public class Announcements
{
	private static final Log _log = LogFactory.getLog(Announcements.class);

	private static Announcements _instance;
	private List<String> _announcements = new FastList<String>();
	private List<List<Object>> _eventAnnouncements = new FastList<List<Object>>();

	public Announcements()
	{
		loadAnnouncements();
	}

	public static Announcements getInstance()
	{
		if(_instance == null)
		{
			_instance = new Announcements();
		}

		return _instance;
	}

	public void loadAnnouncements()
	{
		_announcements.clear();
		File file = new File(Config.DATAPACK_ROOT, "data/announcements.txt");

		if(file.exists())
		{
			readFromDisk(file);
		}
		else
		{
			_log.warn("data/announcements.txt doesn't exist");
		}

		file = null;
	}

	public void showAnnouncements(L2PcInstance activeChar)
	{
		for(int i = 0; i < _announcements.size(); i++)
		{
			CreatureSay cs = new CreatureSay(0, 18, "", "" +_announcements.get(i).replace("%name%", activeChar.getName()));
			activeChar.sendPacket(cs);
			cs = null;
		}

		for(int i = 0; i < _eventAnnouncements.size(); i++)
		{
			List<Object> entry = _eventAnnouncements.get(i);

			DateRange validDateRange = (DateRange) entry.get(0);
			String[] msg = (String[]) entry.get(1);
			Date currentDate = new Date();

			if(!validDateRange.isValid() || validDateRange.isWithinRange(currentDate))
			{
				for(String element : msg)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString(element));
				}
			}

			entry = null;
			validDateRange = null;
			msg = null;
			currentDate = null;
		}
	}

	public void addEventAnnouncement(DateRange validDateRange, String[] msg)
	{
		List<Object> entry = new FastList<Object>();
		entry.add(validDateRange);
		entry.add(msg);
		_eventAnnouncements.add(entry);

		entry = null;
	}

	public void listAnnouncements(L2PcInstance activeChar)
	{
		String content = HtmCache.getInstance().getHtmForce("data/html/admin/main/announce.htm");
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setHtml(content);
		TextBuilder replyMSG = new TextBuilder("<br>");

		for(int i = 0; i < _announcements.size(); i++)
		{
			replyMSG.append("<table width=260><tr><td width=220>" + _announcements.get(i) + "</td><td width=40>");
			replyMSG.append("<button value=\"Delete\" action=\"bypass -h admin_del_announcement " + i + "\" width=60 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr></table>");
		}

		adminReply.replace("%announces%", replyMSG.toString());
		activeChar.sendPacket(adminReply);

		content = null;
		adminReply = null;
		replyMSG = null;
	}

	public void addAnnouncement(String text)
	{
		_announcements.add(text);
		saveToDisk();
	}

	public void delAnnouncement(int line)
	{
		_announcements.remove(line);
		saveToDisk();
	}

	private void readFromDisk(File file)
	{
		LineNumberReader lnr = null;

		try
		{
			int i = 0;

			String line = null;
			lnr = new LineNumberReader(new FileReader(file));

			while((line = lnr.readLine()) != null)
			{
				StringTokenizer st = new StringTokenizer(line, "\n\r");
				if(st.hasMoreTokens())
				{
					String announcement = st.nextToken();
					_announcements.add(announcement);

					i++;
				}
				st = null;
			}
			_log.info("Announcements: Loaded " + i + " announcements.");

			line = null;
		}
		catch(IOException e1)
		{
			_log.error("Error reading announcements", e1);
		}
		finally
		{
			try
			{
				lnr.close();
				lnr = null;
			}
			catch(Exception e2)
			{
			}
		}
	}

	private void saveToDisk()
	{
		File file = new File("data/announcements.txt");
		FileWriter save = null;

		try
		{
			save = new FileWriter(file);
			for(int i = 0; i < _announcements.size(); i++)
			{
				save.write(_announcements.get(i));
				save.write("\r\n");
			}
			save.flush();
			save.close();
		}
		catch(IOException e)
		{
			_log.error("saving the announcements file has failed", e);
		}

		file = null;
		save = null;
	}

	public void announceToPlayers(String message)
	{
		// Get all players
		for (L2PcInstance player : L2World.getInstance().getAllPlayers())
		{
			player.sendMessage(message);
		}
	}
	
	public void announceToAll(String text)
	{
		CreatureSay cs = new CreatureSay(0, 18, "", ""+text);

		for(L2PcInstance player : L2World.getInstance().getAllPlayers())
		{
			player.sendPacket(cs);
		}

		cs = null;
	}

	public void announceToAll(SystemMessage sm)
	{
		for(L2PcInstance player : L2World.getInstance().getAllPlayers())
		{
			player.sendPacket(sm);
		}
	}

	public void handleAnnounce(String command, int lengthToTrim)
	{
		try
		{
			String text = command.substring(lengthToTrim);
			Announcements.getInstance().announceToAll(text);
			text = null;
		}

		catch(StringIndexOutOfBoundsException e)
		{
		}
	}
	
	/**
	 * Used to announce custom messages, also supports multilanguage.
	 */
	public void announceToAll(Messages msg)
	{
		Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers();
		for (L2PcInstance onlinePlayer : pls)
			if (onlinePlayer.isOnline() == 1)
				onlinePlayer.sendPacket(new CreatureSay(0, 18, "", msg.toString(onlinePlayer.getLang())));
	}

}