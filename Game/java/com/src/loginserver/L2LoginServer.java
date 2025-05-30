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
package com.src.loginserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.datatables.GameServerTable;
import com.src.mmocore.SelectorConfig;
import com.src.mmocore.SelectorThread;
import com.src.util.Util;
import com.src.util.database.L2DatabaseFactory;
import com.src.util.database.SqlUtils;
import com.src.util.services.ServerType;

public class L2LoginServer
{
	private static Log _log = LogFactory.getLog(L2LoginServer.class);

	public static final int PROTOCOL_REV = 0x0102;

	private static L2LoginServer _instance;
	private GameServerListener _gameServerListener;
	private SelectorThread<L2LoginClient> _selectorThread;
	private Thread _restartLoginServer;

	long serverLoadStart = System.currentTimeMillis();

	public static void main(String[] args)
	{
		_instance = new L2LoginServer();
	}

	public static L2LoginServer getInstance()
	{
		return _instance;
	}

	public L2LoginServer()
	{
		ServerType.serverMode = ServerType.MODE_LOGINSERVER;

		final String LOG_FOLDER = "log";
		final String LOG_NAME = "./config/other/log.cfg";

		File logFolder = new File(Config.DATAPACK_ROOT, LOG_FOLDER);
		logFolder.mkdir();

		InputStream is = null;
		try
		{
			is = new FileInputStream(new File(LOG_NAME));
			LogManager.getLogManager().readConfiguration(is);
			is.close();
		}
		catch(IOException e)
		{
			_log.error("", e);
		}
		finally
		{
			try
			{
				if(is != null)
				{
					is.close();
				}

				is = null;
			}
			catch(IOException e)
			{
				_log.error("", e);
			}
		}

		Util.printSection("Team");
		Util.team();

		Config.load();

		Util.printSection("Database");
		try
		{
			L2DatabaseFactory.getInstance();
		}
		catch(SQLException e)
		{
			_log.fatal("Failed initializing database.", e);
			System.exit(1);
		}

		try
		{
			LoginController.load();
		}
		catch(GeneralSecurityException e)
		{
			_log.fatal("Failed initializing LoginController.", e);
			System.exit(1);
		}

		try
		{
			GameServerTable.load();
		}
		catch(GeneralSecurityException e)
		{
			_log.fatal("Failed to load GameServerTable.", e);
			System.exit(1);
		}
		catch(SQLException e)
		{
			_log.fatal("Failed to load GameServerTable.", e);
			System.exit(1);
		}
		loadBanFile();
		InetAddress bindAddress = null;
		if(!Config.LOGIN_BIND_ADDRESS.equals("*"))
		{
			try
			{
				bindAddress = InetAddress.getByName(Config.LOGIN_BIND_ADDRESS);
			}
			catch(UnknownHostException e1)
			{
				_log.fatal("The LoginServer bind address is invalid, using all avaliable IPs.", e1);
			}
		}

		final SelectorConfig sc = new SelectorConfig();
		sc.MAX_READ_PER_PASS = com.src.mmocore.Config.MMO_MAX_READ_PER_PASS;
		sc.MAX_SEND_PER_PASS = com.src.mmocore.Config.MMO_MAX_SEND_PER_PASS;
		sc.SLEEP_TIME = com.src.mmocore.Config.MMO_SELECTOR_SLEEP_TIME;
		sc.HELPER_BUFFER_COUNT = com.src.mmocore.Config.MMO_HELPER_BUFFER_COUNT;

		final L2LoginPacketHandler lph = new L2LoginPacketHandler();
		final SelectorHelper sh = new SelectorHelper();
		try
		{
			_selectorThread = new SelectorThread<L2LoginClient>(sc, sh, lph, sh, sh);
		}
		catch(IOException e)
		{
			_log.fatal("Failed to open Selector.", e);
			System.exit(1);
		}

		try
		{
			_gameServerListener = new GameServerListener();
			_gameServerListener.start();
			_log.info("Listening for GameServers on " + Config.GAME_SERVER_LOGIN_HOST + ":" + Config.GAME_SERVER_LOGIN_PORT);
		}
		catch(IOException e)
		{
			_log.fatal("FATAL: Failed to start the Game Server Listener.", e);
			System.exit(1);
		}

		try
		{
			_selectorThread.openServerSocket(bindAddress, Config.PORT_LOGIN);
		}
		catch(IOException e)
		{
			_log.fatal("Failed to open server socket.", e);
			System.exit(1);
		}
		_selectorThread.start();
		_log.info("Login Server ready on " + (bindAddress == null ? "*" : bindAddress.getHostAddress()) + ":" + Config.PORT_LOGIN);

		logFolder = null;
		bindAddress = null;
		_log.info("LoginServer Loaded: " + (System.currentTimeMillis() - serverLoadStart) / 1000 + " seconds");
	}

	public GameServerListener getGameServerListener()
	{
		return _gameServerListener;
	}

	private void loadBanFile()
	{
		File bannedFile = new File("./config/other/banned_ip.cfg");
		if(bannedFile.exists() && bannedFile.isFile())
		{
			FileInputStream fis = null;
			try
			{
				fis = new FileInputStream(bannedFile);
			}
			catch(FileNotFoundException e)
			{
				_log.warn("Failed to load banned IPs file (" + bannedFile.getName() + ") for reading. Reason: " + e.getMessage());
				return;
			}
			LineNumberReader reader = new LineNumberReader(new InputStreamReader(fis));
			String line;
			String[] parts;
			try
			{
				while((line = reader.readLine()) != null)
				{
					line = line.trim();
					if(line.length() > 0 && line.charAt(0) != '#')
					{
						parts = line.split("#", 2);
						line = parts[0];
						parts = line.split(" ");
						String address = parts[0];
						long duration = 0;
						if(parts.length > 1)
						{
							try
							{
								duration = Long.parseLong(parts[1]);
							}
							catch(NumberFormatException e)
							{
								_log.warn("Skipped: Incorrect ban duration (" + parts[1] + ") on (" + bannedFile.getName() + "). Line: " + reader.getLineNumber());
								continue;
							}
						}
						try
						{
							LoginController.getInstance().addBanForAddress(address, duration);
						}
						catch(UnknownHostException e)
						{
							_log.warn("Skipped: Invalid address (" + parts[0] + ") on (" + bannedFile.getName() + "). Line: " + reader.getLineNumber());
						}
					}
				}
			}
			catch(IOException e)
			{
				_log.warn("Error while reading the bans file (" + bannedFile.getName() + "). Details: " + e.getMessage());
			}
			_log.info("Loaded " + LoginController.getInstance().getBannedIps().size() + " IP Bans.");
		}
		else
		{
			_log.info("IP Bans file (" + bannedFile.getName() + ") is missing or is a directory, skipped.");
		}
		
		if (Config.LOGIN_SERVER_SCHEDULE_RESTART)
		{
			_log.info("Scheduled LoginServer restart after " + Config.LOGIN_SERVER_SCHEDULE_RESTART_TIME + " hours");
			_restartLoginServer = new LoginServerRestart();
			_restartLoginServer.setDaemon(true);
			_restartLoginServer.start();
		}
	}

	class LoginServerRestart extends Thread
	{
		public LoginServerRestart()
		{
			setName("LoginServerRestart");
		}
		
		@Override
		public void run()
		{
			while (!isInterrupted())
			{
				try
				{
					Thread.sleep(Config.LOGIN_SERVER_SCHEDULE_RESTART_TIME * 60 * 60 * 1000);
				}
				catch (InterruptedException e)
				{
					return;
				}
				shutdown(true);
			}
		}
	}
	
	public void shutdown(boolean restart)
	{
		LoginController.getInstance().shutdown();
		SqlUtils.OpzLogin();
		System.gc();
		Runtime.getRuntime().exit(restart ? 2 : 0);
	}
}