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
package com.src.gameserver.script.faenor;

import java.util.Date;

import javax.script.ScriptContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Node;

import com.src.gameserver.script.DateRange;
import com.src.gameserver.script.IntList;
import com.src.gameserver.script.Parser;
import com.src.gameserver.script.ParserFactory;
import com.src.gameserver.script.ScriptEngine;
import com.src.gameserver.thread.ThreadPoolManager;

public class FaenorEventParser extends FaenorParser
{
	private final static Log _log = LogFactory.getLog(FaenorEventParser.class);

	private DateRange _eventDates = null;

	@Override
	public void parseScript(final Node eventNode, ScriptContext context)
	{
		String ID = attribute(eventNode, "ID");

		_eventDates = DateRange.parse(attribute(eventNode, "Active"), DATE_FORMAT);

		Date currentDate = new Date();
		if(_eventDates.getEndDate().before(currentDate))
		{
			_log.info("Event ID: (" + ID + ") has passed... Ignored.");
			return;
		}

		if(_eventDates.getStartDate().after(currentDate))
		{
			_log.info("Event ID: (" + ID + ") is not active yet... Ignored.");
			ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {
				@Override
				public void run()
				{
					parseEventDropAndMessage(eventNode);
				}
			}, _eventDates.getStartDate().getTime() - currentDate.getTime());
			return;
		}

		parseEventDropAndMessage(eventNode);
	}

	protected void parseEventDropAndMessage(Node eventNode)
	{
		for(Node node = eventNode.getFirstChild(); node != null; node = node.getNextSibling())
		{

			if(isNodeName(node, "DropList"))
			{
				parseEventDropList(node);
			}
			else if(isNodeName(node, "Message"))
			{
				parseEventMessage(node);
			}
		}
	}

	private void parseEventMessage(Node sysMsg)
	{
		try
		{
			String type = attribute(sysMsg, "Type");
			String[] message = attribute(sysMsg, "Msg").split("\n");

			if(type.equalsIgnoreCase("OnJoin"))
			{
				_bridge.onPlayerLogin(message, _eventDates);
			}
		}
		catch(Exception e)
		{
			_log.error("Error in event parser.", e);
		}
	}

	private void parseEventDropList(Node dropList)
	{
		for(Node node = dropList.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if(isNodeName(node, "AllDrop"))
			{
				parseEventDrop(node);
			}
		}
	}

	private void parseEventDrop(Node drop)
	{
		try
		{
			int[] items = IntList.parse(attribute(drop, "Items"));
			int[] count = IntList.parse(attribute(drop, "Count"));
			double chance = getPercent(attribute(drop, "Chance"));

			_bridge.addEventDrop(items, count, chance, _eventDates);
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
	}

	static class FaenorEventParserFactory extends ParserFactory
	{
		@Override
		public Parser create()
		{
			return new FaenorEventParser();
		}
	}

	static
	{
		ScriptEngine.parserFactories.put(getParserName("Event"), new FaenorEventParserFactory());
	}

}