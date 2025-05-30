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
package com.src.loginserver.network.clientpackets;

import java.net.InetAddress;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.datatables.GameServerTable.GameServerInfo;
import com.src.loginserver.HackingException;
import com.src.loginserver.L2LoginClient;
import com.src.loginserver.L2LoginClient.LoginClientState;
import com.src.loginserver.LoginController;
import com.src.loginserver.LoginController.AuthLoginResult;
import com.src.loginserver.network.serverpackets.AccountKicked;
import com.src.loginserver.network.serverpackets.AccountKicked.AccountKickedReason;
import com.src.loginserver.network.serverpackets.LoginFail.LoginFailReason;
import com.src.loginserver.network.serverpackets.LoginOk;
import com.src.loginserver.network.serverpackets.ServerList;

public class RequestAuthLogin extends L2LoginClientPacket
{
	private static final Log _log = LogFactory.getLog(RequestAuthLogin.class);

	private byte[] _raw = new byte[128];

	private String _user;
	private String _password;
	private int _ncotp;

	public String getPassword()
	{
		return _password;
	}

	public String getUser()
	{
		return _user;
	}

	public int getOneTimePassword()
	{
		return _ncotp;
	}

	@Override
	public boolean readImpl()
	{
		if(super._buf.remaining() >= 128)
		{
			readB(_raw);
			return true;
		}
		else
		{
			return false;
		}
	}

	@Override
	public void run()
	{
		byte[] decrypted = null;
		try
		{
			Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
			rsaCipher.init(Cipher.DECRYPT_MODE, getClient().getRSAPrivateKey());
			decrypted = rsaCipher.doFinal(_raw, 0x00, 0x80);
			rsaCipher = null;
		}
		catch(GeneralSecurityException e)
		{
			_log.error("", e);
			return;
		}

		_user = new String(decrypted, 0x5E, 14).trim();
		_user = _user.toLowerCase();
		_password = new String(decrypted, 0x6C, 16).trim();
		_ncotp = decrypted[0x7c];
		_ncotp |= decrypted[0x7d] << 8;
		_ncotp |= decrypted[0x7e] << 16;
		_ncotp |= decrypted[0x7f] << 24;

		LoginController lc = LoginController.getInstance();
		L2LoginClient client = getClient();
		InetAddress address = getClient().getConnection().getInetAddress();
		if(address == null)
		{
			_log.warn("Socket is not connected: " + client.getAccount());
			client.close(LoginFailReason.REASON_SYSTEM_ERROR);
			return;
		}
		String addhost = address.getHostAddress();
		try
		{
			AuthLoginResult result = lc.tryAuthLogin(_user, _password, getClient());

			switch(result)
			{
				case AUTH_SUCCESS:
					client.setAccount(_user);
					client.setState(LoginClientState.AUTHED_LOGIN);
					client.setSessionKey(lc.assignSessionKeyToClient(_user, client));
					if(Config.SHOW_LICENCE)
					{
						client.sendPacket(new LoginOk(getClient().getSessionKey()));
					}
					else
					{
						getClient().sendPacket(new ServerList(getClient()));
					}
					
					if(Config.ENABLE_DDOS_PROTECTION_SYSTEM)
					{
					String deny_comms = Config.DDOS_COMMAND_BLOCK;
					deny_comms = deny_comms.replace("$IP", addhost);
						try
						{
							Runtime.getRuntime().exec(deny_comms);
							if(Config.ENABLE_DEBUG_DDOS_PROTECTION_SYSTEM)
							{
								_log.info("Accepted IP access GS by "+addhost);
								_log.info("Command is"+deny_comms);
							}
						}
						catch(Exception e)
						{
							_log.warn("Accepts by ip "+addhost+" no allowed");
							_log.warn("Command is"+deny_comms);
						}
					}
					break;
				case INVALID_PASSWORD:
					client.close(LoginFailReason.REASON_USER_OR_PASS_WRONG);
					break;
				case ACCOUNT_BANNED:
					client.close(new AccountKicked(AccountKickedReason.REASON_PERMANENTLY_BANNED));
					break;
				case ALREADY_ON_LS:
					L2LoginClient oldClient;
					if((oldClient = lc.getAuthedClient(_user)) != null)
					{
						oldClient.close(LoginFailReason.REASON_ACCOUNT_IN_USE);
						lc.removeAuthedLoginClient(_user);
					}
					oldClient = null;
					break;
				case ALREADY_ON_GS:
					GameServerInfo gsi;
					if((gsi = lc.getAccountOnGameServer(_user)) != null)
					{
						client.close(LoginFailReason.REASON_ACCOUNT_IN_USE);

						if(gsi.isAuthed())
						{
							gsi.getGameServerThread().kickPlayer(_user);
						}
					}
					gsi = null;
					break;
			}

			result = null;
		}
		catch(HackingException e)
		{
			lc.addBanForAddress(address, Config.LOGIN_BLOCK_AFTER_BAN * 1000);
			_log.warn("Banned (" + address + ") for " + Config.LOGIN_BLOCK_AFTER_BAN + " seconds, due to " + e.getConnects() + " incorrect login attempts.");
		}

		decrypted = null;
		lc = null;
		client = null;
	}

}