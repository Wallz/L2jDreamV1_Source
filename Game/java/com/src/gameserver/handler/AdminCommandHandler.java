/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package com.src.gameserver.handler;


import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.src.util.StringUtil;

public class AdminCommandHandler
{
	private static final Logger _log = Logger.getLogger(AdminCommandHandler.class.getName());
	
	private static final TIntObjectHashMap<IAdminCommandHandler> _datatable = new TIntObjectHashMap<IAdminCommandHandler>();
	
	public static AdminCommandHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private AdminCommandHandler()
	{
		
	}
	
	public void registerAdminCommandHandler(IAdminCommandHandler handler)
	{
		String[] ids = handler.getAdminCommandList();
		for (int i = 0; i < ids.length; i++)
		{
			if (_log.isLoggable(Level.FINE))
			{
				_log.fine(StringUtil.concat("Adding handler for command ", ids[i]));
			}
			_datatable.put(ids[i].hashCode(), handler);
		}
	}
	
	public IAdminCommandHandler getAdminCommandHandler(String adminCommand)
	{
		String command = adminCommand;
		final int sepPos = adminCommand.indexOf(' ');
		
		if (sepPos > -1)
		{
			command = adminCommand.substring(0, sepPos);
		}
		if (_log.isLoggable(Level.FINE))
		{
			_log.fine(StringUtil.concat("getting handler for command: ", command, " -> ", String.valueOf(_datatable.get(command.hashCode()) != null)));
		}
		return _datatable.get(command.hashCode());
	}

	public int size()
	{
		return _datatable.size();
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final AdminCommandHandler INSTANCE = new AdminCommandHandler();
	}
}