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
package com.src.loginserver.network.gameserverpackets;

import java.util.logging.Logger;

import com.src.loginserver.network.clientpackets.ClientBasePacket;

public class GameServerAuth extends ClientBasePacket
{
	protected static Logger _log = Logger.getLogger(GameServerAuth.class.getName());
	private byte[] _hexId;
	private int _desiredId;
	private boolean _hostReserved;
	private boolean _acceptAlternativeId;
	private int _maxPlayers;
	private int _port;
	private String _externalHost;
	private String _internalHost;

	public GameServerAuth(byte[] decrypt)
	{
		super(decrypt);

		_desiredId = readC();
		_acceptAlternativeId = readC() == 0 ? false : true;
		_hostReserved = readC() == 0 ? false : true;
		_externalHost = readS();
		_internalHost = readS();
		_port = readH();
		_maxPlayers = readD();

		int size = readD();

		_hexId = readB(size);
	}

	public byte[] getHexID()
	{
		return _hexId;
	}

	public boolean getHostReserved()
	{
		return _hostReserved;
	}

	public int getDesiredID()
	{
		return _desiredId;
	}

	public boolean acceptAlternateID()
	{
		return _acceptAlternativeId;
	}

	public int getMaxPlayers()
	{
		return _maxPlayers;
	}

	public String getExternalHost()
	{
		return _externalHost;
	}

	public String getInternalHost()
	{
		return _internalHost;
	}

	public int getPort()
	{
		return _port;
	}

}