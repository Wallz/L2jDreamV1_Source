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
package com.src.gameserver.network.serverpackets;

import java.util.logging.Logger;

import com.src.Config;
import com.src.gameserver.cache.HtmCache;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.clientpackets.RequestBypassToServer;

public class NpcHtmlMessage extends L2GameServerPacket
{
	private static final String _S__1B_NPCHTMLMESSAGE = "[S] 0f NpcHtmlMessage";
	private static Logger _log = Logger.getLogger(RequestBypassToServer.class.getName());
	private int _npcObjId;
	private String _html;
	private boolean _validate = true;

	public NpcHtmlMessage(int npcObjId, String text)
	{
		_npcObjId = npcObjId;
		setHtml(text);
	}

	public NpcHtmlMessage(int npcObjId)
	{
		_npcObjId = npcObjId;
	}

	@Override
	public void runImpl()
	{
		if(Config.BYPASS_VALIDATION && _validate)
		{
			buildBypassCache(getClient().getActiveChar());
			buildLinksCache(getClient().getActiveChar());
		}
	}

	public void setHtml(String text)
	{
		if(text.length() > 8192)
		{
			_log.warning("Html is too long! this will crash the client!");
			_html = "<html><body>Html was too long,<br>Try to use DB for this action</body></html>";
			return;
		}

		_html = text;
	}

	public boolean setFile(String path)
	{
		String content = HtmCache.getInstance().getHtm(path);

		if(content == null)
		{
			setHtml("<html><body>My Text is missing:<br>" + path + "</body></html>");
			_log.warning("missing html page " + path);
			return false;
		}

		setHtml(content);
		return true;
	}
	
	public boolean setFile(String prefix, String path)
	{
		String oriPath = path;
		if (prefix != null && !prefix.equalsIgnoreCase("en"))
		{
			if (path.contains("html/"))
				path = path.replace("html/", "html-" + prefix +"/");
		}
		String content = HtmCache.getInstance().getHtm(path);
		if (content == null && !oriPath.equals(path))
			content = HtmCache.getInstance().getHtm(oriPath);
		if (content == null)
		{
			setHtml("<html><body>No HTML.</body></html>");
			_log.warning("Missing html page: " + path);
			return false;
		}
		setHtml(content);
		return true;
	}

	public void replace(String pattern, String value)
	{
		_html = _html.replaceAll(pattern, value);
	}
	
	public void replace(String pattern, long value)
	{
		replace(pattern, String.valueOf(value));
	}

	private final void buildBypassCache(L2PcInstance activeChar)
	{
		if(activeChar == null)
		{
			return;
		}

		activeChar.clearBypass();
		int len = _html.length();
		for(int i = 0; i < len; i++)
		{
			int start = _html.indexOf("bypass -h", i);
			int finish = _html.indexOf("\"", start);

			if(start < 0 || finish < 0)
			{
				break;
			}

			start += 10;
			i = start;
			int finish2 = _html.indexOf("$", start);
			if(finish2 < finish && finish2 > 0)
			{
				activeChar.addBypass2(_html.substring(start, finish2));
			}
			else
			{
				activeChar.addBypass(_html.substring(start, finish));
			}
		}
	}

	private final void buildLinksCache(L2PcInstance activeChar)
	{
		if(activeChar == null)
		{
			return;
		}

		activeChar.clearLinks();
		int len = _html.length();
		for(int i=0; i<len; i++)
		{
			int start = _html.indexOf("link", i);
			int finish = _html.indexOf("\"", start);

			if(start < 0 || finish < 0)
			{
				break;
			}

			i = start;
			activeChar.addLink(_html.substring(start + 5, finish).trim());
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x0f);
		writeD(_npcObjId);
		writeS(_html);
		writeD(0x00);
	}

	@Override
	public String getType()
	{
		return _S__1B_NPCHTMLMESSAGE;
	}
}