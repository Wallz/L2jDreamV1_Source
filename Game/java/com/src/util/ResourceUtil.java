package com.src.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ResourceUtil
{
	private static final Logger _log = Logger.getLogger(ResourceUtil.class.getName());
	
	/**
	 * Safely closes <code>connection</code>. Connection can be even null.
	 * If exception is thrown  during closing, it is logged,
	 * but method does not throw any exception.
	 * <br><br>
	 * @param connection
	 */
	public static void closeConnection(final Connection connection)
	{
		if (connection != null)
		{
			try
			{
				connection.close();
			}
			catch (final SQLException ex)
			{
				_log.log(Level.WARNING, "Failed to close connection", ex);
			}
		}
	}
	
	/**
	 * Safely closes <code>statement</code>. 
	 * Statement can be even null. If exception is thrown
	 * during closing, it is logged, but method does not throw any exception.
	 * <br><br>
	 * @param statement
	 */
	public static void closeStatement(final Statement statement)
	{
		if (statement != null)
		{
			try
			{
				statement.close();
			}
			catch (final SQLException ex)
			{
				_log.log(Level.WARNING, "Failed to close statement", ex);
			}
		}
	}
}