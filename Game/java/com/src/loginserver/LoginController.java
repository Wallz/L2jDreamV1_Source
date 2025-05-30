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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;

import javax.crypto.Cipher;

import javolution.util.FastCollection.Record;
import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.datatables.GameServerTable;
import com.src.gameserver.datatables.GameServerTable.GameServerInfo;
import com.src.loginserver.network.gameserverpackets.ServerStatus;
import com.src.loginserver.network.serverpackets.LoginFail.LoginFailReason;
import com.src.util.ResourceUtil;
import com.src.util.Util;
import com.src.util.database.L2DatabaseFactory;
import com.src.util.protection.Base64;
import com.src.util.protection.ScrambledKeyPair;
import com.src.util.random.Rnd;

public class LoginController
{
	private static final Log _log = LogFactory.getLog(LoginController.class);

	private static LoginController _instance;
	private final static int LOGIN_TIMEOUT = 60 * 1000;
	protected FastList<L2LoginClient> _clients = new FastList<L2LoginClient>();
	protected FastMap<String, L2LoginClient> _loginServerClients = new FastMap<String, L2LoginClient>().shared();
	private Map<InetAddress, BanInfo> _bannedIps = new FastMap<InetAddress, BanInfo>().shared();
	private Map<InetAddress, FailedLoginAttempt> _hackProtection;
	protected ScrambledKeyPair[] _keyPairs;
	protected byte[][] _blowfishKeys;
	private static final int BLOWFISH_KEYS = 20;

	private class ConnectionChecker extends Thread
	{
		@Override
		public void run()
		{
			for(;;)
			{
				long now = System.currentTimeMillis();
				if(_stopNow)
				{
					break;
				}
				for(L2LoginClient cl : _clients) try
				{
					if(now - cl.getConnectionStartTime() > Config.SESSION_TTL)
					{
						cl.close(LoginFailReason.REASON_TEMP_PASS_EXPIRED);
					}
				}
				catch(Exception e)
				{
				}
				try
				{
					Thread.sleep(2500);
				}
				catch(Exception e)
				{
				}
			}
		}
	}

	public static void load() throws GeneralSecurityException
	{
		if(_instance == null)
		{
			_instance = new LoginController();
		}
		else
		{
			throw new IllegalStateException("LoginController can only be loaded a single time.");
		}
	}

	public static LoginController getInstance()
	{
		return _instance;
	}

	private LoginController() throws GeneralSecurityException
	{
		Util.printSection("LoginContoller");

		_hackProtection = new FastMap<InetAddress, FailedLoginAttempt>();

		_keyPairs = new ScrambledKeyPair[10];
		KeyPairGenerator keygen = null;

		keygen = KeyPairGenerator.getInstance("RSA");
		RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4);
		keygen.initialize(spec);

		for(int i = 0; i < 10; i++)
		{
			_keyPairs[i] = new ScrambledKeyPair(keygen.generateKeyPair());
		}

		_log.info("Cached 10 KeyPairs for RSA communication");

		testCipher((RSAPrivateKey) _keyPairs[0]._pair.getPrivate());

		generateBlowFishKeys();

		spec = null;
		keygen = null;
		new ConnectionChecker().start();
	}

	private void testCipher(RSAPrivateKey key) throws GeneralSecurityException
	{
		Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
		rsaCipher.init(Cipher.DECRYPT_MODE, key);
		rsaCipher = null;
	}

	private boolean _stopNow = false;
	public void shutdown()
	{
		_stopNow = true;
		try
		{
			Thread.sleep(10000);
		}
		catch(Exception e)
		{
		}
	}

	private void generateBlowFishKeys()
	{
		_blowfishKeys = new byte[BLOWFISH_KEYS][16];

		for(int i = 0; i < BLOWFISH_KEYS; i++)
		{
			for(int j = 0; j < _blowfishKeys[i].length; j++)
			{
				_blowfishKeys[i][j] = (byte) (Rnd.nextInt(255) + 1);
			}
		}
		_log.info("Stored " + _blowfishKeys.length + " keys for Blowfish communication");
	}

	public byte[] getBlowfishKey()
	{
		return _blowfishKeys[(int) (Math.random() * BLOWFISH_KEYS)];
	}

	public void addLoginClient(L2LoginClient client)
	{
		if(_clients.size()>=Config.MAX_LOGINSESSIONS)
		{
			for(L2LoginClient cl : _clients) try
			{
				cl.close(LoginFailReason.REASON_DUAL_BOX);
			}
			catch(Exception e)
			{
			}
		}
		synchronized (_clients)
		{
			_clients.add(client);
		}
	}

	public void removeLoginClient(L2LoginClient client)
	{
		if(_clients.contains(client))
		synchronized (_clients)
		{
			try
			{
				_clients.remove(client);
			}
			catch(Exception e)
			{
			}
		}
	}

	public SessionKey assignSessionKeyToClient(String account, L2LoginClient client)
	{
		SessionKey key;

		key = new SessionKey(Rnd.nextInt(), Rnd.nextInt(), Rnd.nextInt(), Rnd.nextInt());
		_loginServerClients.put(account, client);
		return key;
	}

	public void removeAuthedLoginClient(String account)
	{
		try
		{
			_loginServerClients.remove(account);
		}
		catch(Exception e)
		{}
	}

	public boolean isAccountInLoginServer(String account)
	{
		return _loginServerClients.containsKey(account);
	}

	public L2LoginClient getAuthedClient(String account)
	{
		return _loginServerClients.get(account);
	}

	public static enum AuthLoginResult
	{
		INVALID_PASSWORD,
		ACCOUNT_BANNED,
		ALREADY_ON_LS,
		ALREADY_ON_GS,
		AUTH_SUCCESS
	};

	public AuthLoginResult tryAuthLogin(String account, String password, L2LoginClient client) throws HackingException
	{
		AuthLoginResult ret = AuthLoginResult.INVALID_PASSWORD;

		if(loginValid(account, password, client))
		{
			ret = AuthLoginResult.ALREADY_ON_GS;

			if(!isAccountInAnyGameServer(account))
			{
				ret = AuthLoginResult.ALREADY_ON_LS;

				synchronized(_loginServerClients)
				{
					if(!_loginServerClients.containsKey(account))
					{
						_loginServerClients.put(account, client);
						ret = AuthLoginResult.AUTH_SUCCESS;

						removeLoginClient(client);
					}
				}
			}
		}
		else
		{
			if(client.getAccessLevel() < 0)
			{
				ret = AuthLoginResult.ACCOUNT_BANNED;
			}
		}
		return ret;
	}

	public void addBanForAddress(String address, long expiration) throws UnknownHostException
	{
		InetAddress netAddress = InetAddress.getByName(address);
		_bannedIps.put(netAddress, new BanInfo(netAddress, expiration));
		netAddress = null;
	}

	public void addBanForAddress(InetAddress address, long duration)
	{
		_bannedIps.put(address, new BanInfo(address, System.currentTimeMillis() + duration));
	}

	public boolean isBannedAddress(InetAddress address)
	{
		BanInfo bi = _bannedIps.get(address);
		if(bi != null)
		{
			if(bi.hasExpired())
			{
				_bannedIps.remove(address);
				return false;
			}
			else
			{
				return true;
			}
		}
		bi = null;

		return false;
	}

	public Map<InetAddress, BanInfo> getBannedIps()
	{
		return _bannedIps;
	}

	public boolean removeBanForAddress(InetAddress address)
	{
		return _bannedIps.remove(address) != null;
	}

	public boolean removeBanForAddress(String address)
	{
		try
		{
			return this.removeBanForAddress(InetAddress.getByName(address));
		}
		catch(UnknownHostException e)
		{
			return false;
		}
	}

	public SessionKey getKeyForAccount(String account)
	{
		L2LoginClient client = _loginServerClients.get(account);

		if(client != null)
		{
			return client.getSessionKey();
		}

		client = null;

		return null;
	}

	public int getOnlinePlayerCount(int serverId)
	{
		GameServerInfo gsi = GameServerTable.getInstance().getRegisteredGameServerById(serverId);

		if(gsi != null && gsi.isAuthed())
		{
			return gsi.getCurrentPlayerCount();
		}

		gsi = null;

		return 0;
	}

	public boolean isAccountInAnyGameServer(String account)
	{
		Collection<GameServerInfo> serverList = GameServerTable.getInstance().getRegisteredGameServers().values();

		for(GameServerInfo gsi : serverList)
		{
			GameServerThread gst = gsi.getGameServerThread();

			if(gst != null && gst.hasAccountOnGameServer(account))
			{
				return true;
			}

			gst = null;
		}

		serverList = null;

		return false;
	}

	public GameServerInfo getAccountOnGameServer(String account)
	{
		Collection<GameServerInfo> serverList = GameServerTable.getInstance().getRegisteredGameServers().values();

		for(GameServerInfo gsi : serverList)
		{
			GameServerThread gst = gsi.getGameServerThread();

			if(gst != null && gst.hasAccountOnGameServer(account))
			{
				return gsi;
			}

			gst = null;
		}

		serverList = null;

		return null;
	}

	public int getTotalOnlinePlayerCount()
	{
		int total = 0;
		Collection<GameServerInfo> serverList = GameServerTable.getInstance().getRegisteredGameServers().values();

		for(GameServerInfo gsi : serverList)
		{
			if(gsi.isAuthed())
			{
				total += gsi.getCurrentPlayerCount();
			}
		}

		serverList = null;

		return total;
	}

	public int getMaxAllowedOnlinePlayers(int id)
	{
		GameServerInfo gsi = GameServerTable.getInstance().getRegisteredGameServerById(id);

		if(gsi != null)
		{
			return gsi.getMaxPlayers();
		}

		gsi = null;

		return 0;
	}

	public boolean isLoginPossible(L2LoginClient client, int serverId)
	{
		GameServerInfo gsi = GameServerTable.getInstance().getRegisteredGameServerById(serverId);
		int access = client.getAccessLevel();
		if(gsi != null && gsi.isAuthed())
		{
			boolean loginOk = gsi.getCurrentPlayerCount() < gsi.getMaxPlayers() && gsi.getStatus() != ServerStatus.STATUS_GM_ONLY || access >= 100;
			if(loginOk && client.getLastServer() != serverId)
			{
				Connection con = null;

				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();

					String stmt = "UPDATE accounts SET lastServer = ? WHERE login = ?";
					PreparedStatement statement = con.prepareStatement(stmt);
					statement.setInt(1, serverId);
					statement.setString(2, client.getAccount());
					statement.executeUpdate();
					ResourceUtil.closeStatement(statement);
				}
				catch(Exception e)
				{
					_log.error("Could not set lastServer", e);
				}
				finally
				{
					ResourceUtil.closeConnection(con); 
				}
			}
			return loginOk;
		}
		return false;
	}

	public void setAccountAccessLevel(String account, int banLevel)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			String stmt = "UPDATE accounts SET access_level = ? WHERE login = ?";
			PreparedStatement statement = con.prepareStatement(stmt);
			statement.setInt(1, banLevel);
			statement.setString(2, account);
			statement.executeUpdate();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("Could not set accessLevel", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	public boolean isGM(String user)
	{
		boolean ok = false;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT access_level FROM accounts WHERE login = ?");
			statement.setString(1, user);
			ResultSet rset = statement.executeQuery();

			if(rset.next())
			{
				int accessLevel = rset.getInt(1);

				if(accessLevel >= 100)
				{
					ok = true;
				}
			}

			rset.close();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("could not check gm state", e);
			ok = false;
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
		return ok;
	}

	public ScrambledKeyPair getScrambledRSAKeyPair()
	{
		return _keyPairs[Rnd.nextInt(10)];
	}
	
	@SuppressWarnings("resource")
	public boolean loginValid(String user, String password, L2LoginClient client)
	{
		boolean ok = false;
		InetAddress address = client.getConnection().getInetAddress();

		if(address == null)
		{
			return false;
		}

		Connection con = null;
		try
		{
			MessageDigest md = MessageDigest.getInstance("SHA");
			byte[] raw = password.getBytes("UTF-8");
			byte[] hash = md.digest(raw);

			byte[] expected = null;
			int access = 0;
			int lastServer = 1;

			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT password, access_level, lastServer FROM accounts WHERE login = ?");
			statement.setString(1, user);
			ResultSet rset = statement.executeQuery();

			if(rset.next())
			{
				expected = Base64.decode(rset.getString("password"));
				access = rset.getInt("access_level");
				lastServer = rset.getInt("lastServer");

				if(lastServer <= 0)
				{
					lastServer = 1;
				}
			}

			rset.close();
			ResourceUtil.closeStatement(statement);

			if(expected == null)
			{
				if(Config.AUTO_CREATE_ACCOUNTS)
				{
					if(user.length() >= 2 && user.length() <= 14)
					{
						statement = con.prepareStatement("INSERT INTO accounts (login, password, lastactive, access_level, lastIP) VALUES (?, ?, ?, ?, ?)");
						statement.setString(1, user);
						statement.setString(2, Base64.encodeBytes(hash));
						statement.setLong(3, System.currentTimeMillis());
						statement.setInt(4, 0);
						statement.setString(5, address.getHostAddress());
						statement.execute();
						ResourceUtil.closeStatement(statement);

						_log.info("Created new account : " + user + " on IP : " + address.getHostAddress());
						return true;
					}
					_log.warn("Invalid username creation/use attempt: " + user);
					return false;
				}
				_log.warn("Account missing for user " + user + " IP: " + address.getHostAddress());
				return false;
			}
			else
			{
				if(access < 0)
				{
					client.setAccessLevel(access);
					return false;
				}

				ok = true;
				for(int i = 0; i < expected.length; i++)
				{
					if(hash[i] != expected[i])
					{
						ok = false;
						break;
					}
				}
			}

			if(ok)
			{
				client.setAccessLevel(access);
				client.setLastServer(lastServer);
				statement = con.prepareStatement("UPDATE accounts SET lastactive = ?, lastIP = ? WHERE login = ?");
				statement.setLong(1, System.currentTimeMillis());
				statement.setString(2, address.getHostAddress());
				statement.setString(3, user);
				statement.execute();
				ResourceUtil.closeStatement(statement);
			}
			md = null;
		}
		catch(Exception e)
		{
			_log.error("Could not check password", e);
			ok = false;
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}

		if(!ok)
		{
			//Log.add("'" + user + "' " + address.getHostAddress(), "logins_ip_fails");

			FailedLoginAttempt failedAttempt = _hackProtection.get(address);

			int failedCount;
			if(failedAttempt == null)
			{
				_hackProtection.put(address, new FailedLoginAttempt(address, password));
				failedCount = 1;
			}
			else
			{
				failedAttempt.increaseCounter(password);
				failedCount = failedAttempt.getCount();
			}

			if(failedCount >= Config.LOGIN_TRY_BEFORE_BAN)
			{
				_log.info("Banning '" + address.getHostAddress() + "' for " + Config.LOGIN_BLOCK_AFTER_BAN + " seconds due to " + failedCount + " invalid user/pass attempts");
				this.addBanForAddress(address, Config.LOGIN_BLOCK_AFTER_BAN * 1000);
			}

			failedAttempt = null;
		}
		else
		{
			_hackProtection.remove(address);
			//Log.add("'" + user + "' " + address.getHostAddress(), "logins_ip");
		}

		address = null;

		return ok;
	}

	public boolean loginBanned(String user)
	{
		boolean ok = false;

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT access_level FROM accounts WHERE login = ?");
			statement.setString(1, user);
			ResultSet rset = statement.executeQuery();

			if(rset.next())
			{
				int accessLevel = rset.getInt(1);

				if(accessLevel < 0)
				{
					ok = true;
				}
			}

			rset.close();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("could not check ban state", e);
			ok = false;
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
		return ok;
	}

	class FailedLoginAttempt
	{
		private int _count;
		private long _lastAttempTime;
		private String _lastPassword;

		public FailedLoginAttempt(InetAddress address, String lastPassword)
		{
			_count = 1;
			_lastAttempTime = System.currentTimeMillis();
			_lastPassword = lastPassword;
		}

		public void increaseCounter(String password)
		{
			if(!_lastPassword.equals(password))
			{
				if(System.currentTimeMillis() - _lastAttempTime < 300 * 1000)
				{
					_count++;
				}
				else
				{
					_count = 1;
				}
				_lastPassword = password;
				_lastAttempTime = System.currentTimeMillis();
			}
			else
			{
				_lastAttempTime = System.currentTimeMillis();
			}
		}

		public int getCount()
		{
			return _count;
		}
	}

	class BanInfo
	{
		private InetAddress _ipAddress;

		private long _expiration;

		public BanInfo(InetAddress ipAddress, long expiration)
		{
			_ipAddress = ipAddress;
			_expiration = expiration;
		}

		public InetAddress getAddress()
		{
			return _ipAddress;
		}

		public boolean hasExpired()
		{
			return System.currentTimeMillis() > _expiration && _expiration > 0;
		}
	}

	class PurgeThread extends Thread
	{
		@Override
		public void run()
		{
			for(;;)
			{
				synchronized(_clients)
				{
					for(Record e = _clients.head(), end = _clients.tail(); (e = e.getNext()) != end;)
					{
						L2LoginClient client = _clients.valueOf(e);
						if(client.getConnectionStartTime() + LOGIN_TIMEOUT >= System.currentTimeMillis())
						{
							client.close(LoginFailReason.REASON_ACCESS_FAILED);
						}
						client = null;
					}
				}

				synchronized(_loginServerClients)
				{
					for(FastMap.Entry<String, L2LoginClient> e = _loginServerClients.head(), end = _loginServerClients.tail(); (e = e.getNext()) != end;)
					{
						L2LoginClient client = e.getValue();
						if(client.getConnectionStartTime() + LOGIN_TIMEOUT >= System.currentTimeMillis())
						{
							client.close(LoginFailReason.REASON_ACCESS_FAILED);
						}
						client = null;
					}
				}
				try
				{
					Thread.sleep(2 * LOGIN_TIMEOUT);
				}
				catch(InterruptedException e)
				{
					_log.error("", e);
				}
			}
		}
	}

}