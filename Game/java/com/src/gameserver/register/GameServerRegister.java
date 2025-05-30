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
package com.src.gameserver.register;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import com.src.Config;
import com.src.gameserver.datatables.GameServerTable;
import com.src.gameserver.thread.LoginServerThread;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;
import com.src.util.services.ServerType;

public class GameServerRegister
{
	private static String _choice;
	private static boolean _choiceOk;

	public static void main(String[] args) throws IOException
	{
		ServerType.serverMode = ServerType.MODE_LOGINSERVER;

		Config.load();

		LineNumberReader _in = new LineNumberReader(new InputStreamReader(System.in));
		try
		{
			GameServerTable.load();
		}
		catch(Exception e)
		{
			System.out.println("FATAL: Failed loading GameServerTable. Reason: "+e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		GameServerTable gameServerTable = GameServerTable.getInstance();
		System.out.println("#--------------------------------------------");
		System.out.println("# Welcome to Dream GameServer Regitering");
		System.out.println("#--------------------------------------------");
		System.out.println("# Type the id of the server you want register");
		System.out.println("# Type 'help' to get a list of ids.");
		System.out.println("# Type 'clean' to unregister all currently"); 
		System.out.println("# registered gameservers on this LoginServer.");
		System.out.println("#--------------------------------------------");
		while(!_choiceOk)
		{
			System.out.println("Your choice:");
			_choice = _in.readLine();
			if(_choice.equalsIgnoreCase("help"))
			{
				for(Map.Entry<Integer, String> entry : gameServerTable.getServerNames().entrySet())
				{
					System.out.println("Server: ID: "+entry.getKey()+"\t- "+entry.getValue()+" - In Use: "+(gameServerTable.hasRegisteredGameServerOnId(entry.getKey()) ? "YES" : "NO"));
				}
				System.out.println("You can also see servername.xml");
			}
			else if(_choice.equalsIgnoreCase("clean"))
			{
				System.out.print("This is going to UNREGISTER ALL servers from this LoginServer. Are you sure? (y/n) ");
				_choice = _in.readLine();
				if(_choice.equals("y"))
				{
					GameServerRegister.cleanRegisteredGameServersFromDB();
					gameServerTable.getRegisteredGameServers().clear();
				}
				else
				{
					System.out.println("ABORTED");
				}
			}
			else
			{
				try
				{
					int id = new Integer(_choice).intValue();
					int size = gameServerTable.getServerNames().size();

					if(size == 0)
					{
						System.out.println("No server names avalible, please make sure that servername.xml is in the LoginServer directory.");
						System.exit(1);
					}

					String name = gameServerTable.getServerNameById(id);
					if(name == null)
					{
						System.out.println("No name for id: "+id);
						continue;
					}
					else
					{
						if(gameServerTable.hasRegisteredGameServerOnId(id))
						{
							System.out.println("This id is not free");
						}
						else
						{
							byte[] hexId = LoginServerThread.generateHex(16);
							gameServerTable.registerServerOnDB(hexId, id, "");
							Config.saveHexid(id, new BigInteger(hexId).toString(16),"hexid.txt");
							System.out.println("Server Registered hexid saved to 'hexid.txt'");
							System.out.println("Put this file in the /config/other/ folder of your gameserver.'");
							return;
						}
					}
				}
				catch(NumberFormatException nfe)
				{
					System.out.println("Please, type a number or 'help'");
				}
			}
		}
	}

	public static void cleanRegisteredGameServersFromDB()
	{
		java.sql.Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM gameservers");
			statement.executeUpdate();
			statement.close();
		}
		catch(SQLException e)
		{
			System.out.println("SQL error while cleaning registered servers: "+e);
		}
		finally
		{
			ResourceUtil.closeStatement(statement);
			ResourceUtil.closeConnection(con);
		}
	}
}