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
package com.src.gameserver.thread;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;

import com.src.Config;
import com.src.gameserver.GameTimeController;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.L2GameClient;
import com.src.gameserver.network.L2GameClient.GameClientState;
import com.src.gameserver.network.gameserverpackets.AuthRequest;
import com.src.gameserver.network.gameserverpackets.BlowFishKey;
import com.src.gameserver.network.gameserverpackets.ChangeAccessLevel;
import com.src.gameserver.network.gameserverpackets.GameServerBasePacket;
import com.src.gameserver.network.gameserverpackets.PlayerAuthRequest;
import com.src.gameserver.network.gameserverpackets.PlayerInGame;
import com.src.gameserver.network.gameserverpackets.PlayerLogout;
import com.src.gameserver.network.gameserverpackets.ServerStatus;
import com.src.gameserver.network.loginserverpackets.AuthResponse;
import com.src.gameserver.network.loginserverpackets.InitLS;
import com.src.gameserver.network.loginserverpackets.KickPlayer;
import com.src.gameserver.network.loginserverpackets.LoginServerFail;
import com.src.gameserver.network.loginserverpackets.PlayerAuthResponse;
import com.src.gameserver.network.serverpackets.AuthLoginFail;
import com.src.gameserver.network.serverpackets.CharSelectInfo;
import com.src.util.Util;
import com.src.util.protection.NewCrypt;
import com.src.util.random.Rnd;

public class LoginServerThread extends Thread
{
	protected static final Logger _log = Logger.getLogger(LoginServerThread.class.getName());

	private static LoginServerThread _instance;

	private static final int REVISION = 0x0102;
	private RSAPublicKey _publicKey;
	private String _hostname;
	private int _port;
	private int _gamePort;
	private Socket _loginSocket;
	private InputStream _in;
	private OutputStream _out;
	private NewCrypt _blowfish;
	private byte[] _blowfishKey;
	private byte[] _hexID;
	private boolean _acceptAlternate;
	private int _requestID;
	private int _serverID;
	private boolean _reserveHost;
	private int _maxPlayer;
	private List<WaitingClient> _waitingClients;
	private Map<String, L2GameClient> _accountsInGameServer;
	private int _status;
	private String _serverName;
	private String _gameExternalHost;
	private String _gameInternalHost;

	public LoginServerThread()
	{
		super("LoginServerThread");
		_port = Config.GAME_SERVER_LOGIN_PORT;
		_gamePort = Config.PORT_GAME;
		_hostname = Config.GAME_SERVER_LOGIN_HOST;
		_hexID = Config.HEX_ID;
		if(_hexID == null)
		{
			_requestID = Config.REQUEST_ID;
			_hexID = generateHex(16);
		}
		else
		{
			_requestID = Config.SERVER_ID;
		}
		_acceptAlternate = Config.ACCEPT_ALTERNATE_ID;
		_reserveHost = Config.RESERVE_HOST_ON_LOGIN;
		_gameExternalHost = Config.EXTERNAL_HOSTNAME;
		_gameInternalHost = Config.INTERNAL_HOSTNAME;
		_waitingClients = new FastList<WaitingClient>();
		_accountsInGameServer = new FastMap<String, L2GameClient>().shared();
		_maxPlayer = Config.MAXIMUM_ONLINE_USERS;
	}

	public static LoginServerThread getInstance()
	{
		if(_instance == null)
		{
			_instance = new LoginServerThread();
		}
		return _instance;
	}

	@Override
	public void run()
	{
		while(true)
		{
			int lengthHi = 0;
			int lengthLo = 0;
			int length = 0;
			boolean checksumOk = false;
			try
			{
				_log.info("Connect: Login on " + _hostname + ":" + _port);
				_loginSocket = new Socket(_hostname, _port);
				_in = _loginSocket.getInputStream();
				_out = new BufferedOutputStream(_loginSocket.getOutputStream());
				_blowfishKey = generateHex(40);
				_blowfish = new NewCrypt("_;v.]05-31!|+-%xT!^[$\00");
				while(true)
				{
					lengthLo = _in.read();
					lengthHi = _in.read();
					length = lengthHi * 256 + lengthLo;

					if(lengthHi < 0)
					{
						_log.finer("LoginServerThread: Login terminated the connection.");
						break;
					}

					byte[] incoming = new byte[length];
					incoming[0] = (byte) lengthLo;
					incoming[1] = (byte) lengthHi;

					int receivedBytes = 0;
					int newBytes = 0;
					while(newBytes != -1 && receivedBytes < length - 2)
					{
						newBytes = _in.read(incoming, 2, length - 2);
						receivedBytes = receivedBytes + newBytes;
					}

					if(receivedBytes != length - 2)
					{
						_log.warning("Incomplete Packet is sent to the server, closing connection.(LS)");
						break;
					}

					byte[] decrypt = new byte[length - 2];
					System.arraycopy(incoming, 2, decrypt, 0, decrypt.length);

					decrypt = _blowfish.decrypt(decrypt);
					checksumOk = NewCrypt.verifyChecksum(decrypt);

					if(!checksumOk)
					{
						_log.warning("Incorrect packet checksum, ignoring packet (LS)");
						break;
					}

					int packetType = decrypt[0] & 0xff;
					switch(packetType)
					{
						case 00:
							InitLS init = new InitLS(decrypt);
							if(init.getRevision() != REVISION)
							{
								_log.warning("/!\\ Revision mismatch between LS and GS /!\\");
								break;
							}
							try
							{
								KeyFactory kfac = KeyFactory.getInstance("RSA");
								BigInteger modulus = new BigInteger(init.getRSAKey());
								RSAPublicKeySpec kspec1 = new RSAPublicKeySpec(modulus, RSAKeyGenParameterSpec.F4);
								_publicKey = (RSAPublicKey) kfac.generatePublic(kspec1);
							}

							catch(GeneralSecurityException e)
							{
								_log.warning("Troubles while init the public key send by login");
								break;
							}
							BlowFishKey bfk = new BlowFishKey(_blowfishKey, _publicKey);
							sendPacket(bfk);

							_blowfish = new NewCrypt(_blowfishKey);
							AuthRequest ar = new AuthRequest(_requestID, _acceptAlternate, _hexID, _gameExternalHost, _gameInternalHost, _gamePort, _reserveHost, _maxPlayer);
							sendPacket(ar);
							break;
						case 01:
							LoginServerFail lsf = new LoginServerFail(decrypt);
							_log.info("Damn! Registeration Failed: " + lsf.getReasonString());
							break;
						case 02:
							AuthResponse aresp = new AuthResponse(decrypt);
							_serverID = aresp.getServerId();
							_serverName = aresp.getServerName();
							Config.saveHexid(_serverID, hexToString(_hexID));
							_log.info("Register: Server " + _serverID + " - " + _serverName);
							Util.printSection("Live");
							ServerStatus st = new ServerStatus();
							if(Config.SERVER_LIST_BRACKET)
							{
								st.addAttribute(ServerStatus.SERVER_LIST_SQUARE_BRACKET, ServerStatus.ON);
							}
							else
							{
								st.addAttribute(ServerStatus.SERVER_LIST_SQUARE_BRACKET, ServerStatus.OFF);
							}
							if(Config.SERVER_LIST_CLOCK)
							{
								st.addAttribute(ServerStatus.SERVER_LIST_CLOCK, ServerStatus.ON);
							}
							else
							{
								st.addAttribute(ServerStatus.SERVER_LIST_CLOCK, ServerStatus.OFF);
							}
							if(Config.SERVER_LIST_TESTSERVER)
							{
								st.addAttribute(ServerStatus.TEST_SERVER, ServerStatus.ON);
							}
							else
							{
								st.addAttribute(ServerStatus.TEST_SERVER, ServerStatus.OFF);
							}
							if(Config.SERVER_GMONLY)
							{
								st.addAttribute(ServerStatus.SERVER_LIST_STATUS, ServerStatus.STATUS_GM_ONLY);
							}
							else
							{
								st.addAttribute(ServerStatus.SERVER_LIST_STATUS, ServerStatus.STATUS_AUTO);
							}
							sendPacket(st);
							if(L2World.getInstance().getAllPlayersCount() > 0)
							{
								FastList<String> playerList = new FastList<String>();
								for(L2PcInstance player : L2World.getInstance().getAllPlayers())
								{
									if((player == null) || (player.getClient() == null))
									{
										continue;
									}
									playerList.add(player.getAccountName());
								}
								PlayerInGame pig = new PlayerInGame(playerList);
								sendPacket(pig);
							}
							break;
						case 03:
							PlayerAuthResponse par = new PlayerAuthResponse(decrypt);
							String account = par.getAccount();
							WaitingClient wcToRemove = null;
							synchronized (_waitingClients)
							{
								for(WaitingClient wc : _waitingClients)
								{
									if(wc.account.equals(account))
									{
										wcToRemove = wc;
									}
								}
							}
							if(wcToRemove != null)
							{
								if(par.isAuthed())
								{
									PlayerInGame pig = new PlayerInGame(par.getAccount());
									sendPacket(pig);
									pig = null;

									wcToRemove.gameClient.setState(GameClientState.AUTHED);
									wcToRemove.gameClient.setSessionId(wcToRemove.session);

									CharSelectInfo cl = new CharSelectInfo(wcToRemove.account, wcToRemove.gameClient.getSessionId().playOkID1);
									wcToRemove.gameClient.getConnection().sendPacket(cl);
									wcToRemove.gameClient.setCharSelection(cl.getCharInfo());
									cl = null;
								}
								else
								{
									_log.warning("session key is not correct. closing connection");
									wcToRemove.gameClient.getConnection().sendPacket(new AuthLoginFail(1));
									wcToRemove.gameClient.closeNow();
								}
								_waitingClients.remove(wcToRemove);
							}
							break;
						case 04:
							KickPlayer kp = new KickPlayer(decrypt);
							doKickPlayer(kp.getAccount());
							kp = null;
							break;
					}
				}
			}
			catch(UnknownHostException e)
			{
			}
			catch(IOException e)
			{
				_log.info("Deconnected from Login, Trying to reconnect:");
				_log.info(e.toString());
			}
			finally
			{
				try
				{
					_loginSocket.close();
				}
				catch(Exception e)
				{
				}
			}
			try
			{
				Thread.sleep(5000);
			}
			catch(InterruptedException e)
			{
			}
		}
	}

	public void addWaitingClientAndSendRequest(String acc, L2GameClient client, SessionKey key)
	{
		WaitingClient wc = new WaitingClient(acc, client, key);

		synchronized (_waitingClients)
		{
			_waitingClients.add(wc);
		}

		PlayerAuthRequest par = new PlayerAuthRequest(acc, key);

		try
		{
			sendPacket(par);
		}
		catch(IOException e)
		{
			_log.warning("Error while sending player auth request");
		}

		par = null;
	}

	public void removeWaitingClient(L2GameClient client)
	{
		WaitingClient toRemove = null;

		synchronized (_waitingClients)
		{
			for(WaitingClient c : _waitingClients)
			{
				if(c.gameClient == client)
				{
					toRemove = c;
				}
			}

			if(toRemove != null)
			{
				_waitingClients.remove(toRemove);
			}
		}

		toRemove = null;
	}

	public void sendLogout(String account)
	{
		PlayerLogout pl = new PlayerLogout(account);
		try
		{
			sendPacket(pl);
		}
		catch(IOException e)
		{
			_log.warning("Error while sending logout packet to login");
		}

		pl = null;
	}

	public void addGameServerLogin(String account, L2GameClient client)
	{
		_accountsInGameServer.put(account, client);
	}

	public void sendAccessLevel(String account, int level)
	{
		ChangeAccessLevel cal = new ChangeAccessLevel(account, level);
		try
		{
			sendPacket(cal);
		}
		catch(IOException e)
		{
		}

		cal = null;
	}

	private String hexToString(byte[] hex)
	{
		return new BigInteger(hex).toString(16);
	}

	public void doKickPlayer(String account)
	{
		if(_accountsInGameServer.get(account) != null)
		{
			_accountsInGameServer.get(account).closeNow();
			LoginServerThread.getInstance().sendLogout(account);
		}
	}

	public static byte[] generateHex(int size)
	{
		byte[] array = new byte[size];
		Rnd.nextBytes(array);
		return array;
	}

	private void sendPacket(GameServerBasePacket sl) throws IOException
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
	}

	public void setMaxPlayer(int maxPlayer)
	{
		sendServerStatus(ServerStatus.MAX_PLAYERS, maxPlayer);
		_maxPlayer = maxPlayer;
	}

	public int getMaxPlayer()
	{
		return _maxPlayer;
	}

	public void sendServerStatus(int id, int value)
	{
		ServerStatus ss = new ServerStatus();
		ss.addAttribute(id, value);
		try
		{
			sendPacket(ss);
		}
		catch(IOException e)
		{
		}

		ss = null;
	}

	public String getStatusString()
	{
		return ServerStatus.STATUS_STRING[_status];
	}

	public boolean isClockShown()
	{
		return Config.SERVER_LIST_CLOCK;
	}

	public boolean isBracketShown()
	{
		return Config.SERVER_LIST_BRACKET;
	}

	public String getServerName()
	{
		return _serverName;
	}

	public void setServerStatus(int status)
	{
		switch(status)
		{
			case ServerStatus.STATUS_AUTO:
				sendServerStatus(ServerStatus.SERVER_LIST_STATUS, ServerStatus.STATUS_AUTO);
				_status = status;
				break;
			case ServerStatus.STATUS_DOWN:
				sendServerStatus(ServerStatus.SERVER_LIST_STATUS, ServerStatus.STATUS_DOWN);
				_status = status;
				break;
			case ServerStatus.STATUS_FULL:
				sendServerStatus(ServerStatus.SERVER_LIST_STATUS, ServerStatus.STATUS_FULL);
				_status = status;
				break;
			case ServerStatus.STATUS_GM_ONLY:
				sendServerStatus(ServerStatus.SERVER_LIST_STATUS, ServerStatus.STATUS_GM_ONLY);
				_status = status;
				break;
			case ServerStatus.STATUS_GOOD:
				sendServerStatus(ServerStatus.SERVER_LIST_STATUS, ServerStatus.STATUS_GOOD);
				_status = status;
				break;
			case ServerStatus.STATUS_NORMAL:
				sendServerStatus(ServerStatus.SERVER_LIST_STATUS, ServerStatus.STATUS_NORMAL);
				_status = status;
				break;
			default:
				throw new IllegalArgumentException("Status does not exists:" + status);
		}
	}

	public static class SessionKey
	{
		public int playOkID1;
		public int playOkID2;
		public int loginOkID1;
		public int loginOkID2;
		public int clientKey = -1;

		public SessionKey(int loginOK1, int loginOK2, int playOK1, int playOK2)
		{
			playOkID1 = playOK1;
			playOkID2 = playOK2;
			loginOkID1 = loginOK1;
			loginOkID2 = loginOK2;
		}

		@Override
		public String toString()
		{
			return "PlayOk: " + playOkID1 + " " + playOkID2 + " LoginOk:" + loginOkID1 + " " + loginOkID2;
		}
	}

	private class WaitingClient
	{
		public String account;
		public L2GameClient gameClient;
		public SessionKey session;

		public WaitingClient(String acc, L2GameClient client, SessionKey key)
		{
			account = acc;
			GameTimeController.getGameTicks();
			gameClient = client;
			session = key;
		}
	}

	public String getExHost()
	{
		return _gameExternalHost;
	}

}