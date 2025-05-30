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
package com.src.util.database;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.src.Config;

public class L2DatabaseFactory
{
	private static final Log _log = LogFactory.getLog(L2DatabaseFactory.class);

	public static enum ProviderType
	{
		MySql,
		MsSql
	}

	private static L2DatabaseFactory _instance;
	private ProviderType _providerType;
	private ComboPooledDataSource _source;

	public L2DatabaseFactory() throws SQLException
	{
		try
		{
			if(Config.DATABASE_MAX_CONNECTIONS < 10)
			{
				Config.DATABASE_MAX_CONNECTIONS = 10;
				_log.warn("at least " + Config.DATABASE_MAX_CONNECTIONS + " db connections are required.");
			}
			_source = new ComboPooledDataSource();
			_source.setDebugUnreturnedConnectionStackTraces(false);
			_source.setAutoCommitOnClose(true);
			_source.setInitialPoolSize(10);
			_source.setMinPoolSize(10);
			_source.setMaxPoolSize(Config.DATABASE_MAX_CONNECTIONS);
			_source.setAcquireRetryAttempts(0);
			_source.setAcquireRetryDelay(500);
			_source.setCheckoutTimeout(Config.DATABASE_TIMEOUT);
			_source.setAcquireIncrement(5);
			_source.setAutomaticTestTable("connection_test_table");
			_source.setTestConnectionOnCheckin(false);
			_source.setIdleConnectionTestPeriod(3600);
			_source.setMaxIdleTime(0);
			_source.setMaxStatementsPerConnection(Config.DATABASE_STATEMENT);
			_source.setBreakAfterAcquireFailure(false);
			_source.setDriverClass(Config.DATABASE_DRIVER);
			_source.setJdbcUrl(Config.DATABASE_URL);
			_source.setUser(Config.DATABASE_LOGIN);
			_source.setPassword(Config.DATABASE_PASSWORD);
			_source.getConnection().close();

			if(Config.DATABASE_DRIVER.toLowerCase().contains("microsoft"))
			{
				_providerType = ProviderType.MsSql;
			}
			else
			{
				_providerType = ProviderType.MySql;
			}
		}
		catch(SQLException x)
		{
			throw x;
		}
		catch(Exception e)
		{
			throw new SQLException("could not init DB connection", e);
		}
	}

	public final String prepQuerySelect(String[] fields, String tableName, String whereClause, boolean returnOnlyTopRecord)
	{
		String msSqlTop1 = "";
		String mySqlTop1 = "";
		if(returnOnlyTopRecord)
		{
			if(getProviderType() == ProviderType.MsSql)
			{
				msSqlTop1 = " Top 1 ";
			}

			if(getProviderType() == ProviderType.MySql)
			{
				mySqlTop1 = " Limit 1 ";
			}
		}
		String query = "SELECT " + msSqlTop1 + safetyString(fields) + " FROM " + tableName + " WHERE " + whereClause + mySqlTop1;
		return query;
	}

	public static void close(Connection con)
	{
		if (con == null)
			return;
		
		try
		{
			con.close();
		}
		catch (SQLException e)
		{
			_log.fatal("Failed to close database connection! "+ e);
		}
	}
	
	public void shutdown()
	{
		try
		{
			_source.close();
		}
		catch(Exception e)
		{
			_log.error("", e);
		}

		try
		{
			_source = null;
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
	}

	public final String safetyString(String[] whatToCheck)
	{
		String braceLeft = "`";
		String braceRight = "`";

		if(getProviderType() == ProviderType.MsSql)
		{
			braceLeft = "[";
			braceRight = "]";
		}

		String result = "";

		for(String word : whatToCheck)
		{
			if(result != "")
			{
				result += ", ";
			}
			result += braceLeft + word + braceRight;
		}
		return result;
	}

	public static L2DatabaseFactory getInstance() throws SQLException
	{
		if(_instance == null)
		{
			_instance = new L2DatabaseFactory();
		}
		return _instance;
	}

	public Connection getConnection() throws SQLException
	{
		Connection con = null;

		while(con == null)
		{
			try
			{
				con = _source.getConnection();
			}
			catch(SQLException e)
			{
				_log.error("L2DatabaseFactory: geting connection failed, trying again", e);
			}
		}

		return con;
	}

	public int getBusyConnectionCount() throws SQLException
	{
		return _source.getNumBusyConnectionsDefaultUser();
	}

	public int getIdleConnectionCount() throws SQLException
	{
		return _source.getNumIdleConnectionsDefaultUser();
	}

	public final ProviderType getProviderType()
	{
		return _providerType;
	}

}