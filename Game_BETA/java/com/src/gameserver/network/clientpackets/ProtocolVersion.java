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
package com.src.gameserver.network.clientpackets;

import java.util.logging.Logger;

import com.src.Config;
import com.src.gameserver.network.serverpackets.KeyPacket;
import com.src.gameserver.network.serverpackets.L2GameServerPacket;
import com.src.gameserver.network.serverpackets.SendStatus;

public final class ProtocolVersion extends L2GameClientPacket
{
	static Logger _log = Logger.getLogger(ProtocolVersion.class.getName());

	private static final String _C__00_PROTOCOLVERSION = "[C] 00 ProtocolVersion";

	private int _version;

	@Override
	protected void readImpl()
	{
		_version = readH();
	}

	@Override
	protected void runImpl()
	{
		if(_version == 65534 || _version == -2)
		{
			getClient().close((L2GameServerPacket) null);
		}
		else if(_version == 65533 || _version == -3)
		{
			if(Config.RWHO_LOG)
			{
				_log.info(getClient().toString() + " RWHO received");
			}
			getClient().close(new SendStatus());
		}
		else if(_version < Config.MIN_PROTOCOL_REVISION || _version > Config.MAX_PROTOCOL_REVISION)
		{
			_log.info("Client: " + getClient().toString() + " -> Protocol Revision: " + _version + " is invalid. Minimum is " + Config.MIN_PROTOCOL_REVISION + " and Maximum is " + Config.MAX_PROTOCOL_REVISION + " are supported. Closing connection.");
			_log.warning("Wrong Protocol Version " + _version);
			getClient().close((L2GameServerPacket) null);
		}
		else
		{
			KeyPacket pk = new KeyPacket(getClient().enableCrypt());
			getClient().sendPacket(pk);
		}
	}

	@Override
	public String getType()
	{
		return _C__00_PROTOCOLVERSION;
	}

}