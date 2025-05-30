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
package com.src.gameserver.handler;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastMap;

import com.src.util.StringUtil;

public class UserCommandHandler
{
	private static final Logger _log = Logger.getLogger(UserCommandHandler.class.getName());
	
	private static final Map<Integer, IUserCommandHandler> _datatable = new FastMap<Integer, IUserCommandHandler>();
	
	public static UserCommandHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private UserCommandHandler()
	{
	}
	
	public void registerUserCommandHandler(IUserCommandHandler handler)
	{
		int[] ids = handler.getUserCommandList();
		for (int i = 0; i < ids.length; i++)
		{
			if (_log.isLoggable(Level.FINE))
			{
				_log.fine(StringUtil.concat("Adding handler for user command ", String.valueOf(ids[i])));
			}
			_datatable.put(Integer.valueOf(ids[i]), handler);
		}
	}
	
	public IUserCommandHandler getUserCommandHandler(int userCommand)
	{
		if (_log.isLoggable(Level.FINE))
		{
			_log.fine(StringUtil.concat("getting handler for user command: ", String.valueOf(userCommand)));
		}
		return _datatable.get(Integer.valueOf(userCommand));
	}
	
	public int size()
	{
		return _datatable.size();
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final UserCommandHandler INSTANCE = new UserCommandHandler();
	}
}