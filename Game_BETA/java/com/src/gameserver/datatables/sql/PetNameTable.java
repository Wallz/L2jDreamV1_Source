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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.datatables.xml.L2PetDataTable;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class PetNameTable
{
	private static final Log _log = LogFactory.getLog(PetNameTable.class.getName());

	private static PetNameTable _instance;

	public static PetNameTable getInstance()
	{
		if(_instance == null)
		{
			_instance = new PetNameTable();
		}

		return _instance;
	}

	public boolean doesPetNameExist(String name, int petNpcId)
	{
		boolean result = true;
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT name FROM pets p, items i WHERE p.item_obj_id = i.object_id AND name = ? AND i.item_id IN (?)");
			statement.setString(1, name);

			String cond = "";
			for(int it : L2PetDataTable.getPetItemsAsNpc(petNpcId))
			{
				if(cond != "")
				{
					cond += ", ";
				}

				cond += it;
			}
			statement.setString(2, cond);
			ResultSet rset = statement.executeQuery();
			result = rset.next();

			statement.close();
			rset.close();
			statement = null;
			rset = null;
			cond = null;
		}
		catch(SQLException e)
		{
			_log.error("could not check existing petname", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
		return result;
	}

	public boolean isValidPetName(String name)
	{
		boolean result = true;

		if(!isAlphaNumeric(name))
		{
			return result;
		}

		Pattern pattern;
		try
		{
			pattern = Pattern.compile(Config.PET_NAME_TEMPLATE);
		}
		catch(PatternSyntaxException e)
		{
			_log.warn("ERROR: Pet name pattern of config is wrong!");
			pattern = Pattern.compile(".*");
		}

		Matcher regexp = pattern.matcher(name);

		if(!regexp.matches())
		{
			result = false;
		}

		pattern = null;
		regexp = null;

		return result;
	}

	private boolean isAlphaNumeric(String text)
	{
		boolean result = true;
		char[] chars = text.toCharArray();
		for(int i = 0; i < chars.length; i++)
		{
			if(!Character.isLetterOrDigit(chars[i]))
			{
				result = false;
				break;
			}
		}

		chars = null;

		return result;
	}

}