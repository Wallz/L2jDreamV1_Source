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
package com.src.gameserver.network.serverpackets;

import com.src.gameserver.network.L2GameClient;
import com.src.mmocore.SendablePacket;

public abstract class L2GameServerPacket extends SendablePacket<L2GameClient>
{
	protected abstract void writeImpl();
	public abstract String getType();

	@Override
	protected void write()
	{
		try
		{
			writeImpl();
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
	}

	public void runImpl()
	{
	}

}