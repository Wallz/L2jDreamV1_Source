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
package com.src.gameserver.datatables.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;

import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class CharNameTable
{
	private static final Log _log = LogFactory.getLog(CharNameTable.class.getName());

	private final Map<Integer, String> _chars;
	private final Map<Integer, Integer> _accessLevels;
	
	private static CharNameTable _instance;

	protected CharNameTable()
	{
		_chars = new FastMap<>();
		_accessLevels = new FastMap<>();
	}
	
	public static CharNameTable getInstance()
	{
		if(_instance == null)
		{
			_instance = new CharNameTable();
		}
		return _instance;
	}

	public synchronized boolean doesCharNameExist(String name)
	{
		boolean result = true;
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT account_name FROM characters WHERE char_name = ?");
			statement.setString(1, name);
			ResultSet rset = statement.executeQuery();
			result = rset.next();

			statement.close();
			rset.close();
			statement = null;
			rset = null;
		}
		catch(SQLException e)
		{
			_log.error("could not check existing charname", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
		return result;
	}

	public final String getNameById(int id)
	{
		if (id <= 0)
			return null;
		
		String name = _chars.get(id);
		if (name != null)
			return name;
		
		int accessLevel = 0;
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT char_name,accesslevel FROM characters WHERE obj_Id=?");
			statement.setInt(1, id);
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				name = rset.getString(1);
				accessLevel = rset.getInt(2);
			}
			rset.close();
			statement.close();
		}
		catch (SQLException e)
		{
			_log.warn(Level.WARNING);
		}
		
		if (name != null && !name.isEmpty())
		{
			_chars.put(id, name);
			_accessLevels.put(id, accessLevel);
			return name;
		}
		
		return null; // not found
	}
	
	public int accountCharNumber(String account)
	{
		Connection con = null;
		int number = 0;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT COUNT(char_name) FROM characters WHERE account_name = ?");
			statement.setString(1, account);
			ResultSet rset = statement.executeQuery();

			while(rset.next())
			{
				number = rset.getInt(1);
			}

			statement.close();
			rset.close();
			statement = null;
			rset = null;
		}
		catch(SQLException e)
		{
			_log.error("could not check existing char number", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}

		return number;
	}

}