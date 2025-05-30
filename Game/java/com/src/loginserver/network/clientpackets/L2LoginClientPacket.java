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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.loginserver.L2LoginClient;
import com.src.mmocore.ReceivablePacket;

public abstract class L2LoginClientPacket extends ReceivablePacket<L2LoginClient>
{
	protected final static Log _log = LogFactory.getLog(L2LoginClientPacket.class);

	@Override
	protected final boolean read()
	{
		try
		{
			return readImpl();
		}
		catch(Exception e)
		{
			_log.error("error reading", e);
			return false;
		}
	}

	protected abstract boolean readImpl();

}