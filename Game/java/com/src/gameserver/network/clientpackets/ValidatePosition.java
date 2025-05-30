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

import com.src.Config;
import com.src.gameserver.model.L2Party;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.PartyMemberPosition;
import com.src.gameserver.network.serverpackets.ValidateLocation;
import com.src.gameserver.network.serverpackets.ValidateLocationInVehicle;
import com.src.gameserver.thread.TaskPriority;

public class ValidatePosition extends L2GameClientPacket
{
	private static final String _C__48_VALIDATEPOSITION = "[C] 48 ValidatePosition";

	public TaskPriority getPriority()
	{
		return TaskPriority.PR_HIGH;
	}

	private int _x;
	private int _y;
	private int _z;
	private int _heading;
	@SuppressWarnings("unused")
	private int _data;

	@Override
	protected void readImpl()
	{
		_x = readD();
		_y = readD();
		_z = readD();
		_heading = readD();
		_data = readD();
	}

	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if(activeChar == null || activeChar.isTeleporting() || activeChar.inObserverMode())
		{
			return;
		}

		final int realX = activeChar.getX();
		final int realY = activeChar.getY();
		int realZ = activeChar.getZ();

		if(_x == 0 && _y == 0)
		{
			if(realX != 0)
			{
				return;
			}
		}

		int dx, dy, dz;
		double diffSq;

		if(activeChar.isInBoat())
		{
			dx = _x - realX;
			dy = _y - realY;
			diffSq = dx * dx + dy * dy;

			if((Config.COORD_SYNCHRONIZE & 2) == 2 && diffSq > 10000)
			{
				sendPacket(new ValidateLocationInVehicle(activeChar));
			}
			else if(Config.COORD_SYNCHRONIZE == 4)
			{
				dz = _z - realZ;
				activeChar.setXYZ(activeChar.getBoat().getX(), activeChar.getBoat().getY(), activeChar.getBoat().getZ());
			}
			else if(Config.COORD_SYNCHRONIZE == -1){

				activeChar.setClientX(_x);
				activeChar.setClientY(_y);
				activeChar.setClientZ(_z);
				activeChar.setClientHeading(_heading);

				if(diffSq < 250000)
				{
					activeChar.setXYZ(realX, realY, _z);
				}

				sendPacket(new ValidateLocationInVehicle(activeChar));
			}

			return;
		}

		if(activeChar.isFalling(_z))
		{
			return;
		}

		dx = _x - realX;
		dy = _y - realY;
		dz = _z - realZ;
		diffSq = (dx*dx + dy*dy);

		L2Party party = activeChar.getParty();
		if(party != null && activeChar.getLastPartyPositionDistance(_x, _y, _z) > 150)
		{
			activeChar.setLastPartyPosition(_x, _y, _z);
			party.broadcastToPartyMembers(activeChar,new PartyMemberPosition(activeChar));
		}

		if(activeChar.isFlying() || activeChar.isInsideZone(L2Character.ZONE_WATER))
		{
			activeChar.setXYZ(realX, realY, _z);
			if(diffSq > 90000)
			{
				activeChar.sendPacket(new ValidateLocation(activeChar));
			}

			if(Config.ALLOW_WATER)
			{
				activeChar.checkWaterState();
			}

		}
		else if(diffSq < 360000)
		{
			if(Config.COORD_SYNCHRONIZE == -1)
			{
				activeChar.setXYZ(realX,realY,_z);
				return;
			}

			if(Config.COORD_SYNCHRONIZE == 1)
			{
				if(!activeChar.isMoving() || !activeChar.validateMovementHeading(_heading))
				{
					if(diffSq < 2500)
					{
						activeChar.setXYZ(realX, realY, _z);
					}
					else
					{
						activeChar.setXYZ(_x, _y, _z);
					}
				}
				else
				{
					activeChar.setXYZ(realX, realY, _z);
				}

				activeChar.setHeading(_heading);
				return;
			}

			if(Config.GEODATA > 0 && (diffSq > 250000 || Math.abs(dz) > 200))
			{
				if(Math.abs(dz) > 200 && Math.abs(dz) < 1500 && Math.abs(_z - activeChar.getClientZ()) < 800 )
				{
					activeChar.setXYZ(realX, realY, _z);
					realZ = _z;
				}
				else
				{
					activeChar.sendPacket(new ValidateLocation(activeChar));
				}
			}
		}

		activeChar.setClientX(_x);
		activeChar.setClientY(_y);
		activeChar.setClientZ(_z);
		activeChar.setClientHeading(_heading);
		activeChar.setLastServerPosition(realX, realY, realZ);
	}

	@Override
	public String getType()
	{
		return _C__48_VALIDATEPOSITION;
	}

	@Deprecated
	public boolean equal(ValidatePosition pos)
	{
		return _x == pos._x && _y == pos._y && _z == pos._z && _heading == pos._heading;
	}

}