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

import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.model.entity.siege.Castle;
import com.src.gameserver.network.serverpackets.SiegeAttackerList;

public final class RequestSiegeAttackerList extends L2GameClientPacket
{
	private static final String _C__A2_RequestSiegeAttackerList = "[C] a2 RequestSiegeAttackerList";

	private int _castleId;

	@Override
	protected void readImpl()
	{
		_castleId = readD();
	}

	@Override
	protected void runImpl()
	{
		if(_castleId < 100)
		{
			Castle castle = CastleManager.getInstance().getCastleById(_castleId);

			if(castle == null)
				return;

			SiegeAttackerList sal = new SiegeAttackerList(castle);
			sendPacket(sal);
		}
	}

	@Override
	public String getType()
	{
		return _C__A2_RequestSiegeAttackerList;
	}
}