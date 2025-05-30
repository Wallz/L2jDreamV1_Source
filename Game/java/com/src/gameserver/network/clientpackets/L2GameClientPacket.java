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
package com.src.gameserver.network.clientpackets;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.network.L2GameClient;
import com.src.gameserver.network.serverpackets.L2GameServerPacket;
import com.src.mmocore.ReceivablePacket;

public abstract class L2GameClientPacket extends ReceivablePacket<L2GameClient>
{
	protected static final Log _log = LogFactory.getLog(L2GameClientPacket.class);

	@Override
	protected boolean read()
	{
		try
		{
			readImpl();
			return true;
		}
		catch(BufferOverflowException e)
		{
			if(getClient() != null)
			{
				getClient().closeNow();
			}

			_log.warn("Client: " + getClient().toString() + " - Buffer overflow and has been kicked");
		}
		catch(BufferUnderflowException e)
		{
			getClient().onBufferUnderflow();
		}
		catch(Throwable t)
		{
			_log.warn("Client: " + getClient().toString() + " - Failed reading: " + getType() + " ; " + t.getMessage(), t);
		}

		return false;
	}

	protected abstract void readImpl();

	@Override
	public void run()
	{
		try
		{
			runImpl();

			if(this instanceof MoveBackwardToLocation || this instanceof AttackRequest || this instanceof RequestMagicSkillUse)
			{
				if(getClient().getActiveChar() != null)
				{
					getClient().getActiveChar().onActionRequest();
				}
			}
		}
		catch(Throwable t)
		{
			_log.warn("Client: " + getClient().toString() + " - Failed reading: " + getType() + " ; " + t.getMessage(), t);

			if(this instanceof EnterWorld)
			{
				getClient().closeNow();
			}
		}
	}

	protected abstract void runImpl();

	protected final void sendPacket(L2GameServerPacket gsp)
	{
		getClient().sendPacket(gsp);
	}

	public abstract String getType();

}