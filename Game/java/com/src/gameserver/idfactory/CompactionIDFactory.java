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
package com.src.gameserver.idfactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import com.src.Config;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class CompactionIDFactory extends IdFactory
{
	private static Logger _log = Logger.getLogger(CompactionIDFactory.class.getName());
	private int _curOID;
	private int _freeSize;

	protected CompactionIDFactory()
	{
		super();
		_curOID = FIRST_OID;
		_freeSize = 0;

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			int[] tmp_obj_ids = extractUsedObjectIDTable();

			int N = tmp_obj_ids.length;

			for(int idx = 0; idx < N; idx++)
			{
				N = insertUntil(tmp_obj_ids, idx, N, con);
			}

			_curOID++;
			_log.config("IdFactory: Next usable Object ID is: " + _curOID);
			_initialized = true;

			tmp_obj_ids = null;
		}
		catch(Exception e1)
		{
			e1.printStackTrace();
			_log.severe("ID Factory could not be initialized correctly:" + e1);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	private int insertUntil(int[] tmp_obj_ids, int idx, int N, Connection con) throws SQLException
	{
		int id = tmp_obj_ids[idx];

		if(id == _curOID)
		{
			_curOID++;
			return N;
		}

		if(Config.BAD_ID_CHECKING)
		{
			for(String check : ID_CHECKS)
			{
				PreparedStatement ps = con.prepareStatement(check);
				ps.setInt(1, _curOID);
				ps.setInt(2, id);
				ResultSet rs = ps.executeQuery();

				while(rs.next())
				{
					int badId = rs.getInt(1);
					_log.severe("Bad ID " + badId + " in DB found by: " + check);
					throw new RuntimeException();
				}
				rs.close();
				ps.close();
				rs = null;
				ps = null;
			}
		}

		int hole = id - _curOID;

		if(hole > N - idx)
		{
			hole = N - idx;
		}

		for(int i = 1; i <= hole; i++)
		{
			id = tmp_obj_ids[N - i];
			System.out.println("Compacting DB object ID=" + id + " into " + _curOID);

			for(String update : ID_UPDATES)
			{
				PreparedStatement ps = con.prepareStatement(update);
				ps.setInt(1, _curOID);
				ps.setInt(2, id);
				ps.execute();
				ps.close();
				ps = null;
			}
			_curOID++;
		}
		if(hole < N - idx)
		{
			_curOID++;
		}

		return N - hole;
	}

	@Override
	public synchronized int getNextId()
	{
		return _curOID++;
	}

	@Override
	public synchronized void releaseId(int id)
	{
	}

	@Override
	public int size()
	{
		return _freeSize + LAST_OID - FIRST_OID;
	}

}