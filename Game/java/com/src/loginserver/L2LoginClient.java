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

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.interfaces.RSAPrivateKey;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.loginserver.network.serverpackets.L2LoginServerPacket;
import com.src.loginserver.network.serverpackets.LoginFail;
import com.src.loginserver.network.serverpackets.LoginFail.LoginFailReason;
import com.src.loginserver.network.serverpackets.PlayFail;
import com.src.loginserver.network.serverpackets.PlayFail.PlayFailReason;
import com.src.mmocore.MMOClient;
import com.src.mmocore.MMOConnection;
import com.src.mmocore.SendablePacket;
import com.src.util.protection.LoginCrypt;
import com.src.util.protection.ScrambledKeyPair;
import com.src.util.random.Rnd;

public final class L2LoginClient extends MMOClient<MMOConnection<L2LoginClient>>
{
	private final static Log _log = LogFactory.getLog(L2LoginClient.class);

	public static enum LoginClientState
	{
		CONNECTED,
		AUTHED_GG,
		AUTHED_LOGIN
	};

	private LoginClientState _state;
	private LoginCrypt _loginCrypt;
	private ScrambledKeyPair _scrambledPair;
	private byte[] _blowfishKey;

	private String _account="";
	private int _accessLevel;
	private int _lastServer;
	private boolean _usesInternalIP;
	private SessionKey _sessionKey;
	private int _sessionId;
	private boolean _joinedGS;
	private String _ip;
	private long _connectionStartTime;

	public L2LoginClient(final MMOConnection<L2LoginClient> con)
	{
		super(con);
		_state = LoginClientState.CONNECTED;
		final String ip = getConnection().getInetAddress().getHostAddress();
		_ip = ip;
		String[] localip = Config.NETWORK_IP_LIST.split(";");
		for(String oneIp : localip)
		{
			if(ip.startsWith(oneIp) || ip.startsWith("127.0"))
			{
				_usesInternalIP = true;
			}
		}

		_scrambledPair = LoginController.getInstance().getScrambledRSAKeyPair();
		_blowfishKey = LoginController.getInstance().getBlowfishKey();
		_sessionId = Rnd.nextInt(Integer.MAX_VALUE);
		_connectionStartTime = System.currentTimeMillis();
		_loginCrypt = new LoginCrypt();
		_loginCrypt.setKey(_blowfishKey);
		LoginController.getInstance().addLoginClient(this);
	}

	public String getIntetAddress()
	{
		return _ip;
	}

	public boolean usesInternalIP()
	{
		return _usesInternalIP;
	}

	@Override
	public boolean decrypt(ByteBuffer buf, int size)
	{
		boolean ret = false;
		try
		{
			ret = _loginCrypt.decrypt(buf.array(), buf.position(), size);
			_connectionStartTime = System.currentTimeMillis();
		}
		catch(IOException e)
		{
			_log.error("", e);
			super.getConnection().close((SendablePacket<L2LoginClient>)null);
			return false;
		}

		if(!ret)
		{
			byte[] dump = new byte[size];
			System.arraycopy(buf.array(), buf.position(), dump, 0, size);
			_log.warn("Wrong checksum from client: " + toString());
			super.getConnection().close((SendablePacket<L2LoginClient>)null);
			dump = null;
		}

		return ret;
	}

	@Override
	public boolean encrypt(ByteBuffer buf, int size)
	{
		final int offset = buf.position();
		try
		{
			size = _loginCrypt.encrypt(buf.array(), offset, size);
		}
		catch(IOException e)
		{
			_log.error("", e);
			return false;
		}

		buf.position(offset + size);
		return true;
	}

	public LoginClientState getState()
	{
		return _state;
	}

	public void setState(LoginClientState state)
	{
		_state = state;
	}

	public byte[] getBlowfishKey()
	{
		return _blowfishKey;
	}

	public byte[] getScrambledModulus()
	{
		return _scrambledPair._scrambledModulus;
	}

	public RSAPrivateKey getRSAPrivateKey()
	{
		return (RSAPrivateKey) _scrambledPair._pair.getPrivate();
	}

	public String getAccount()
	{
		return _account;
	}

	public void setAccount(String account)
	{
		_account = account;
	}

	public void setAccessLevel(int accessLevel)
	{
		_accessLevel = accessLevel;
	}

	public int getAccessLevel()
	{
		return _accessLevel;
	}

	public void setLastServer(int lastServer)
	{
		_lastServer = lastServer;
	}

	public int getLastServer()
	{
		return _lastServer;
	}

	public int getSessionId()
	{
		return _sessionId;
	}

	public boolean hasJoinedGS()
	{
		return _joinedGS;
	}

	public void setJoinedGS(boolean val)
	{
		_joinedGS = val;
	}

	public void setSessionKey(SessionKey sessionKey)
	{
		_sessionKey = sessionKey;
	}

	public SessionKey getSessionKey()
	{
		return _sessionKey;
	}

	public long getConnectionStartTime()
	{
		return _connectionStartTime;
	}

	public void sendPacket(L2LoginServerPacket lsp)
	{
		getConnection().sendPacket(lsp);
	}

	public void close(LoginFailReason reason)
	{
		getConnection().close(new LoginFail(reason));
	}

	public void close(PlayFailReason reason)
	{
		getConnection().close(new PlayFail(reason));
	}

	public void close(L2LoginServerPacket lsp)
	{
		getConnection().close(lsp);
	}

	@Override
	public void onDisconnection()
	{
		LoginController.getInstance().removeLoginClient(this);
		if(!hasJoinedGS())
		{
			LoginController.getInstance().removeAuthedLoginClient(getAccount());
		}
	}

	@Override
	public String toString()
	{
		InetAddress address = getConnection().getInetAddress();
		if(getState() == LoginClientState.AUTHED_LOGIN)
		{
			return "[" + getAccount() + " (" + (address == null ? "disconnected" : address.getHostAddress()) + ")]";
		}
		else
		{
			return "[" + (address == null ? "disconnected" : address.getHostAddress()) + "]";
		}
	}

	@Override
	protected void onForcedDisconnection()
	{
	}

}