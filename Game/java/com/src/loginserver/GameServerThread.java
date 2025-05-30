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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javolution.util.FastSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.datatables.GameServerTable;
import com.src.gameserver.datatables.GameServerTable.GameServerInfo;
import com.src.loginserver.network.gameserverpackets.BlowFishKey;
import com.src.loginserver.network.gameserverpackets.ChangeAccessLevel;
import com.src.loginserver.network.gameserverpackets.GameServerAuth;
import com.src.loginserver.network.gameserverpackets.PlayerAuthRequest;
import com.src.loginserver.network.gameserverpackets.PlayerInGame;
import com.src.loginserver.network.gameserverpackets.PlayerLogout;
import com.src.loginserver.network.gameserverpackets.ServerStatus;
import com.src.loginserver.network.loginserverpackets.AuthResponse;
import com.src.loginserver.network.loginserverpackets.InitLS;
import com.src.loginserver.network.loginserverpackets.KickPlayer;
import com.src.loginserver.network.loginserverpackets.LoginServerFail;
import com.src.loginserver.network.loginserverpackets.PlayerAuthResponse;
import com.src.loginserver.network.serverpackets.ServerBasePacket;
import com.src.util.protection.NewCrypt;

public class GameServerThread extends Thread
{
	private final static Log _log = LogFactory.getLog(GameServerThread.class);

	private Socket _connection;
	private InputStream _in;
	private OutputStream _out;
	private RSAPublicKey _publicKey;
	private RSAPrivateKey _privateKey;
	private NewCrypt _blowfish;
	private byte[] _blowfishKey;

	private String _connectionIp;
	private GameServerInfo _gsi;
	private Set<String> _accountsOnGameServer = new FastSet<String>();
	private String _connectionIPAddress;

	@Override
	public void run()
	{
		boolean checkTime = true;
		long time = System.currentTimeMillis();
		_connectionIPAddress = _connection.getInetAddress().getHostAddress();
		if(GameServerThread.isBannedGameserverIP(_connectionIPAddress))
		{
			_log.info("GameServerRegistration: IP Address " + _connectionIPAddress + " is on Banned IP list.");
			forceClose(LoginServerFail.REASON_IP_BANNED);
			return;
		}

		InitLS startPacket = new InitLS(_publicKey.getModulus().toByteArray());
		try
		{
			sendPacket(startPacket);

			int lengthHi = 0;
			int lengthLo = 0;
			int length = 0;
			boolean checksumOk = false;
			while(true)
			{
				if(time - System.currentTimeMillis() > 10000 && checkTime)
				{
					_connection.close();
					break;
				}

				lengthLo = _in.read();
				lengthHi = _in.read();
				length = lengthHi * 256 + lengthLo;

				if(lengthHi < 0 || _connection.isClosed())
				{
					_log.info("LoginServerThread: Login terminated the connection.");
					break;
				}

				byte[] data = new byte[length - 2];

				int receivedBytes = 0;
				int newBytes = 0;

				while(newBytes != -1 && receivedBytes < length - 2)
				{
					newBytes = _in.read(data, 0, length - 2);
					receivedBytes = receivedBytes + newBytes;
				}

				if(receivedBytes != length - 2)
				{
					_log.warn("Incomplete Packet is sent to the server, closing connection.(LS)");
					break;
				}

				data = _blowfish.decrypt(data);
				checksumOk = NewCrypt.verifyChecksum(data);

				if(!checksumOk)
				{
					_log.warn("Incorrect packet checksum, closing connection (LS)");
					return;
				}

				int packetType = data[0] & 0xff;
				switch(packetType)
				{
					case 00:
						checkTime = false;
						onReceiveBlowfishKey(data);
						break;
					case 01:
						onGameServerAuth(data);
						break;
					case 02:
						onReceivePlayerInGame(data);
						break;
					case 03:
						onReceivePlayerLogOut(data);
						break;
					case 04:
						onReceiveChangeAccessLevel(data);
						break;
					case 05:
						onReceivePlayerAuthRequest(data);
						break;
					case 06:
						onReceiveServerStatus(data);
						break;
					default:
						_log.warn("Unknown Opcode (" + Integer.toHexString(packetType).toUpperCase() + ") from GameServer, closing connection.");
						forceClose(LoginServerFail.NOT_AUTHED);
				}
			}
		}
		catch(IOException e)
		{
			String serverName = getServerId() != -1 ? "[" + getServerId() + "] " + GameServerTable.getInstance().getServerNameById(getServerId()) : "(" + _connectionIPAddress + ")";
			_log.error("GameServer " + serverName + ": Connection lost");
			serverName = null;
		}
		finally
		{
			if(isAuthed())
			{
				_gsi.setDown();
				_log.info("Server [" + getServerId() + "] " + GameServerTable.getInstance().getServerNameById(getServerId()) + " is now set as disconnected");
			}

			L2LoginServer.getInstance().getGameServerListener().removeGameServer(this);
			L2LoginServer.getInstance().getGameServerListener().removeFloodProtection(_connectionIp);
		}
		startPacket = null;
	}

	private void onReceiveBlowfishKey(byte[] data)
	{
		BlowFishKey bfk = new BlowFishKey(data, _privateKey);
		_blowfishKey = bfk.getKey();
		_blowfish = new NewCrypt(_blowfishKey);

		bfk = null;
	}

	private void onGameServerAuth(byte[] data) throws IOException
	{
		GameServerAuth gsa = new GameServerAuth(data);

		handleRegProcess(gsa);

		if(isAuthed())
		{
			AuthResponse ar = new AuthResponse(getGameServerInfo().getId());
			sendPacket(ar);

			ar = null;
		}

		gsa = null;
	}

	private void onReceivePlayerInGame(byte[] data)
	{
		if(isAuthed())
		{
			PlayerInGame pig = new PlayerInGame(data);
			List<String> newAccounts = pig.getAccounts();

			for(String account : newAccounts)
			{
				_accountsOnGameServer.add(account);
			}

			pig = null;
			newAccounts = null;

		}
		else
		{
			forceClose(LoginServerFail.NOT_AUTHED);
		}
	}

	private void onReceivePlayerLogOut(byte[] data)
	{
		if(isAuthed())
		{
			PlayerLogout plo = new PlayerLogout(data);
			_accountsOnGameServer.remove(plo.getAccount());

			plo = null;
		}
		else
		{
			forceClose(LoginServerFail.NOT_AUTHED);
		}
	}

	private void onReceiveChangeAccessLevel(byte[] data)
	{
		if(isAuthed())
		{
			ChangeAccessLevel cal = new ChangeAccessLevel(data);
			LoginController.getInstance().setAccountAccessLevel(cal.getAccount(), cal.getLevel());
			_log.info("Changed " + cal.getAccount() + " access level to " + cal.getLevel());
			cal = null;
		}
		else
		{
			forceClose(LoginServerFail.NOT_AUTHED);
		}
	}

	private void onReceivePlayerAuthRequest(byte[] data) throws IOException
	{
		if(isAuthed())
		{
			PlayerAuthRequest par = new PlayerAuthRequest(data);
			PlayerAuthResponse authResponse;

			SessionKey key = LoginController.getInstance().getKeyForAccount(par.getAccount());

			if(key != null && key.equals(par.getKey()))
			{
				LoginController.getInstance().removeAuthedLoginClient(par.getAccount());
				authResponse = new PlayerAuthResponse(par.getAccount(), true);
			}
			else
			{
				authResponse = new PlayerAuthResponse(par.getAccount(), false);
			}
			sendPacket(authResponse);

			par = null;
			authResponse = null;
			key = null;
		}
		else
		{
			forceClose(LoginServerFail.NOT_AUTHED);
		}
	}

	private void onReceiveServerStatus(byte[] data)
	{
		if(isAuthed())
		{
			new ServerStatus(data, getServerId());
		}
		else
		{
			forceClose(LoginServerFail.NOT_AUTHED);
		}
	}

	private void handleRegProcess(GameServerAuth gameServerAuth)
	{
		GameServerTable gameServerTable = GameServerTable.getInstance();

		int id = gameServerAuth.getDesiredID();
		byte[] hexId = gameServerAuth.getHexID();

		GameServerInfo gsi = gameServerTable.getRegisteredGameServerById(id);

		if(gsi != null)
		{
			if(Arrays.equals(gsi.getHexId(), hexId))
			{
				synchronized(gsi)
				{
					if(gsi.isAuthed())
					{
						forceClose(LoginServerFail.REASON_ALREADY_LOGGED8IN);
					}
					else
					{
						attachGameServerInfo(gsi, gameServerAuth);
					}
				}
			}
			else
			{
				if(Config.ACCEPT_NEW_GAMESERVER && gameServerAuth.acceptAlternateID())
				{
					gsi = new GameServerInfo(id, hexId, this);

					if(gameServerTable.registerWithFirstAvaliableId(gsi))
					{
						attachGameServerInfo(gsi, gameServerAuth);
						gameServerTable.registerServerOnDB(gsi);
					}
					else
					{
						forceClose(LoginServerFail.REASON_NO_FREE_ID);
					}
				}
				else
				{
					forceClose(LoginServerFail.REASON_WRONG_HEXID);
				}
			}
		}
		else
		{
			if(Config.ACCEPT_NEW_GAMESERVER)
			{
				gsi = new GameServerInfo(id, hexId, this);

				if(gameServerTable.register(id, gsi))
				{
					attachGameServerInfo(gsi, gameServerAuth);
					gameServerTable.registerServerOnDB(gsi);
				}
				else
				{
					forceClose(LoginServerFail.REASON_ID_RESERVED);
				}
			}
			else
			{
				forceClose(LoginServerFail.REASON_WRONG_HEXID);
			}
		}
		gameServerTable = null;
		gsi = null;
	}

	public boolean hasAccountOnGameServer(String account)
	{
		return _accountsOnGameServer.contains(account);
	}

	public int getPlayerCount()
	{
		return _accountsOnGameServer.size();
	}

	private void attachGameServerInfo(GameServerInfo gsi, GameServerAuth gameServerAuth)
	{
		setGameServerInfo(gsi);
		gsi.setGameServerThread(this);
		gsi.setPort(gameServerAuth.getPort());
		setGameHosts(gameServerAuth.getExternalHost(), gameServerAuth.getInternalHost());
		gsi.setMaxPlayers(gameServerAuth.getMaxPlayers());
		gsi.setAuthed(true);
	}

	private void forceClose(int reason)
	{
		LoginServerFail lsf = new LoginServerFail(reason);

		try
		{
			sendPacket(lsf);
		}
		catch(IOException e)
		{
			_log.error("GameServerThread: Failed kicking banned server.", e);
		}

		try
		{
			_connection.close();
		}
		catch(IOException e)
		{
			_log.error("GameServerThread: Failed disconnecting banned server, server already disconnected.");
		}

		lsf = null;
	}

	public static boolean isBannedGameserverIP(String ipAddress)
	{
		return false;
	}

	public GameServerThread(Socket con)
	{
		_connection = con;
		_connectionIp = con.getInetAddress().getHostAddress();
		try
		{
			_in = _connection.getInputStream();
			_out = new BufferedOutputStream(_connection.getOutputStream());
		}
		catch(IOException e)
		{
			_log.error("", e);
		}

		KeyPair pair = GameServerTable.getInstance().getKeyPair();
		_privateKey = (RSAPrivateKey) pair.getPrivate();
		_publicKey = (RSAPublicKey) pair.getPublic();
		_blowfish = new NewCrypt("_;v.]05-31!|+-%xT!^[$\00");
		start();
		pair = null;
	}

	public void sendPacket(ServerBasePacket sl) throws IOException
	{
		byte[] data = sl.getContent();
		NewCrypt.appendChecksum(data);
		data = _blowfish.crypt(data);

		int len = data.length + 2;
		synchronized (_out)
		{
			_out.write(len & 0xff);
			_out.write(len >> 8 & 0xff);
			_out.write(data);
			_out.flush();
		}

		data = null;
	}

	public void kickPlayer(String account)
	{
		KickPlayer kp = new KickPlayer(account);
		try
		{
			sendPacket(kp);
		}
		catch(IOException e)
		{
			_log.error("", e);
		}

		kp = null;
	}

	public void setGameHosts(String gameExternalHost, String gameInternalHost)
	{
		String oldInternal = _gsi.getInternalHost();
		String oldExternal = _gsi.getExternalHost();

		_gsi.setExternalHost(gameExternalHost);
		_gsi.setInternalIp(gameInternalHost);

		if(!gameExternalHost.equals("*"))
		{
			try
			{
				_gsi.setExternalIp(InetAddress.getByName(gameExternalHost).getHostAddress());
			}
			catch(UnknownHostException e)
			{
				_log.warn("Couldn't resolve hostname \"" + gameExternalHost + "\"");
			}
		}
		else
		{
			_gsi.setExternalIp(_connectionIp);
		}

		if(!gameInternalHost.equals("*"))
		{
			try
			{
				_gsi.setInternalIp(InetAddress.getByName(gameInternalHost).getHostAddress());
			}
			catch(UnknownHostException e)
			{
				_log.warn("Couldn't resolve hostname \"" + gameInternalHost + "\"");
			}
		}
		else
		{
			_gsi.setInternalIp(_connectionIp);
		}

		_log.info("Updated Gameserver [" + getServerId() + "] " + GameServerTable.getInstance().getServerNameById(getServerId()) + " IP's:");

		if(oldInternal == null || !oldInternal.equalsIgnoreCase(gameInternalHost))
		{
			_log.info("InternalIP: " + gameInternalHost);
		}

		if(oldExternal == null || !oldExternal.equalsIgnoreCase(gameExternalHost))
		{
			_log.info("ExternalIP: " + gameExternalHost);
		}

		oldInternal = null;
		oldExternal = null;
	}

	public boolean isAuthed()
	{
		if(getGameServerInfo() == null)
		{
			return false;
		}

		return getGameServerInfo().isAuthed();
	}

	public void setGameServerInfo(GameServerInfo gsi)
	{
		_gsi = gsi;
	}

	public GameServerInfo getGameServerInfo()
	{
		return _gsi;
	}

	public String getConnectionIpAddress()
	{
		return _connectionIPAddress;
	}

	private int getServerId()
	{
		if(getGameServerInfo() != null)
		{
			return getGameServerInfo().getId();
		}

		return -1;
	}

}