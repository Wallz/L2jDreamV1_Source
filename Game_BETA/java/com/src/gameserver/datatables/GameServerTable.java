/* This program is free software; you can redistribute it and/or modify
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
package com.src.gameserver.datatables;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.RSAKeyGenParameterSpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;

import javolution.io.UTF8StreamReader;
import javolution.util.FastMap;
import javolution.xml.stream.XMLStreamConstants;
import javolution.xml.stream.XMLStreamException;
import javolution.xml.stream.XMLStreamReaderImpl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.loginserver.GameServerThread;
import com.src.loginserver.network.gameserverpackets.ServerStatus;
import com.src.util.database.L2DatabaseFactory;
import com.src.util.random.Rnd;

public class GameServerTable
{
	private static Log _log = LogFactory.getLog(GameServerTable.class);

	private static GameServerTable _instance;

	private static Map<Integer, String> _serverNames = new FastMap<Integer, String>();

	private Map<Integer, GameServerInfo> _gameServerTable = new FastMap<Integer, GameServerInfo>().shared();

	private static final int KEYS_SIZE = 10;
	private KeyPair[] _keyPairs;

	public static void load() throws SQLException, GeneralSecurityException
	{
		if(_instance == null)
		{
			_instance = new GameServerTable();
		}
		else
		{
			throw new IllegalStateException("Load can only be invoked a single time.");
		}
	}

	public static GameServerTable getInstance()
	{
		return _instance;
	}

	public GameServerTable() throws SQLException, NoSuchAlgorithmException, InvalidAlgorithmParameterException
	{
		loadServerNames();
		_log.info("Loaded " + _serverNames.size() + " server names");

		loadRegisteredGameServers();
		_log.info("Loaded " + _gameServerTable.size() + " registered Game Servers");

		loadRSAKeys();
		_log.info("Cached " + _keyPairs.length + " RSA keys for Game Server communication.");
	}

	private void loadRSAKeys() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException
	{
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(512, RSAKeyGenParameterSpec.F4);
		keyGen.initialize(spec);

		_keyPairs = new KeyPair[KEYS_SIZE];
		for(int i = 0; i < KEYS_SIZE; i++)
		{
			_keyPairs[i] = keyGen.genKeyPair();
		}
	}

	@SuppressWarnings("resource")
	private void loadServerNames()
	{
		InputStream in = null;
		try
		{
			in = new FileInputStream("config/other/servername.xml");
			XMLStreamReaderImpl xpp = new XMLStreamReaderImpl();
			xpp.setInput(new UTF8StreamReader().setInput(in));

			for(int e = xpp.getEventType(); e != XMLStreamConstants.END_DOCUMENT; e = xpp.next())
			{
				if(e == XMLStreamConstants.START_ELEMENT)
				{
					if(xpp.getLocalName().toString().equals("server"))
					{
						Integer id = new Integer(xpp.getAttributeValue(null, "id").toString());
						String name = xpp.getAttributeValue(null, "name").toString();
						_serverNames.put(id, name);
					}
				}
			}
		}
		catch(FileNotFoundException e)
		{
			_log.warn("servername.xml could not be loaded: file not found");
		}
		catch(XMLStreamException xppe)
		{
			_log.error("", xppe);
		}
		finally
		{
			try
			{
				in.close();
			}
			catch(Exception e)
			{
				
			}
		}
	}

	private void loadRegisteredGameServers() throws SQLException
	{
		Connection con = null;
		PreparedStatement statement = null;

		int id;
		con = L2DatabaseFactory.getInstance().getConnection();
		statement = con.prepareStatement("SELECT * FROM gameservers");
		ResultSet rset = statement.executeQuery();
		GameServerInfo gsi;

		while(rset.next())
		{
			id = rset.getInt("server_id");
			gsi = new GameServerInfo(id, stringToHex(rset.getString("hexid")));
			_gameServerTable.put(id, gsi);
		}

		rset.close();
		statement.close();
		try
		{ 
			con.close();
		} 
		catch(Exception e)
		{ 
			
		}
	}

	public Map<Integer, GameServerInfo> getRegisteredGameServers()
	{
		return _gameServerTable;
	}

	public GameServerInfo getRegisteredGameServerById(int id)
	{
		return _gameServerTable.get(id);
	}

	public boolean hasRegisteredGameServerOnId(int id)
	{
		return _gameServerTable.containsKey(id);
	}

	public boolean registerWithFirstAvaliableId(GameServerInfo gsi)
	{
		synchronized (_gameServerTable)
		{
			for(Entry<Integer, String> entry : _serverNames.entrySet())
			{
				if(!_gameServerTable.containsKey(entry.getKey()))
				{
					_gameServerTable.put(entry.getKey(), gsi);
					gsi.setId(entry.getKey());
					return true;
				}
			}
		}
		return false;
	}

	public boolean register(int id, GameServerInfo gsi)
	{
		synchronized (_gameServerTable)
		{
			if(!_gameServerTable.containsKey(id))
			{
				_gameServerTable.put(id, gsi);
				gsi.setId(id);
				return true;
			}
		}
		return false;
	}

	public void registerServerOnDB(GameServerInfo gsi)
	{
		this.registerServerOnDB(gsi.getHexId(), gsi.getId(), gsi.getExternalHost());
	}

	public void registerServerOnDB(byte[] hexId, int id, String externalHost)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO gameservers (hexid, server_id, host) VALUES (?, ?, ?)");
			statement.setString(1, hexToString(hexId));
			statement.setInt(2, id);
			statement.setString(3, externalHost);
			statement.executeUpdate();
		}
		catch(SQLException e)
		{
			_log.warn("SQL error while saving gameserver: " + e);
		}
		finally
		{
			try
			{
				statement.close();
			}
			catch(SQLException e)
			{
				
			}
			try
			{ 
				con.close();
			} 
			catch(Exception e)
			{ 
				
			}
		}
	}

	public String getServerNameById(int id)
	{
		return getServerNames().get(id);
	}

	public Map<Integer, String> getServerNames()
	{
		return _serverNames;
	}

	public KeyPair getKeyPair()
	{
		return _keyPairs[Rnd.nextInt(10)];
	}

	private byte[] stringToHex(String string)
	{
		return new BigInteger(string, 16).toByteArray();
	}

	private String hexToString(byte[] hex)
	{
		if(hex == null)
			return "null";

		return new BigInteger(hex).toString(16);
	}

	public static class GameServerInfo
	{
		private int _id;
		private byte[] _hexId;
		private boolean _isAuthed;

		private GameServerThread _gst;
		private int _status;

		private String _internalIp;
		private String _externalIp;
		private String _externalHost;
		private int _port;

		private boolean _isPvp = true;
		private boolean _isTestServer;
		private boolean _isShowingClock;
		private boolean _isShowingBrackets;
		private int _maxPlayers;

		public GameServerInfo(int id, byte[] hexId, GameServerThread gst)
		{
			_id = id;
			_hexId = hexId;
			_gst = gst;
			_status = ServerStatus.STATUS_DOWN;
		}

		public GameServerInfo(int id, byte[] hexId)
		{
			this(id, hexId, null);
		}

		public void setId(int id)
		{
			_id = id;
		}

		public int getId()
		{
			return _id;
		}

		public byte[] getHexId()
		{
			return _hexId;
		}

		public void setAuthed(boolean isAuthed)
		{
			_isAuthed = isAuthed;
		}

		public boolean isAuthed()
		{
			return _isAuthed;
		}

		public void setGameServerThread(GameServerThread gst)
		{
			_gst = gst;
		}

		public GameServerThread getGameServerThread()
		{
			return _gst;
		}

		public void setStatus(int status)
		{
			_status = status;
		}

		public int getStatus()
		{
			return _status;
		}

		public int getCurrentPlayerCount()
		{
			if(_gst == null)
			{
				return 0;
			}

			return _gst.getPlayerCount();
		}

		public void setInternalIp(String internalIp)
		{
			_internalIp = internalIp;
		}

		public String getInternalHost()
		{
			return _internalIp;
		}

		public void setExternalIp(String externalIp)
		{
			_externalIp = externalIp;
		}

		public String getExternalIp()
		{
			return _externalIp;
		}

		public void setExternalHost(String externalHost)
		{
			_externalHost = externalHost;
		}

		public String getExternalHost()
		{
			return _externalHost;
		}

		public int getPort()
		{
			return _port;
		}

		public void setPort(int port)
		{
			_port = port;
		}

		public void setMaxPlayers(int maxPlayers)
		{
			_maxPlayers = maxPlayers;
		}

		public int getMaxPlayers()
		{
			return _maxPlayers;
		}

		public boolean isPvp()
		{
			return _isPvp;
		}

		public void setTestServer(boolean val)
		{
			_isTestServer = val;
		}

		public boolean isTestServer()
		{
			return _isTestServer;
		}

		public void setShowingClock(boolean clock)
		{
			_isShowingClock = clock;
		}

		public boolean isShowingClock()
		{
			return _isShowingClock;
		}

		public void setShowingBrackets(boolean val)
		{
			_isShowingBrackets = val;
		}

		public boolean isShowingBrackets()
		{
			return _isShowingBrackets;
		}

		public void setDown()
		{
			setAuthed(false);
			setPort(0);
			setGameServerThread(null);
			setStatus(ServerStatus.STATUS_DOWN);
		}
	}
}