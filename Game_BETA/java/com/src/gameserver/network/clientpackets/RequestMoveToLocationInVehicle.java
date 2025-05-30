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
import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.managers.BoatManager;
import com.src.gameserver.model.actor.instance.L2BoatInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.position.L2CharPosition;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.templates.item.L2WeaponType;
import com.src.gameserver.thread.TaskPriority;
import com.src.util.object.Point3D;

public final class RequestMoveToLocationInVehicle extends L2GameClientPacket
{
	private final Point3D _pos = new Point3D(0, 0, 0);
	private final Point3D _origin_pos = new Point3D(0, 0, 0);
	private int _boatId;

	public TaskPriority getPriority()
	{
		return TaskPriority.PR_HIGH;
	}

	@Override
	protected void readImpl()
	{
		int _x, _y, _z;
		_boatId = readD();
		_x = readD();
		_y = readD();
		_z = readD();
		_pos.setXYZ(_x, _y, _z);
		_x = readD();
		_y = readD();
		_z = readD();
		_origin_pos.setXYZ(_x, _y, _z);
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if(activeChar == null)
		{
			return;
		}
		
		if(Config.PLAYER_MOVEMENT_BLOCK_TIME > 0 && !activeChar.isGM() && activeChar.getNotMoveUntil() > System.currentTimeMillis()) 
		{ 
			activeChar.sendMessage("You cannot move while speaking to an NPC. One moment please"); 
			activeChar.sendPacket(ActionFailed.STATIC_PACKET); 
			return; 
		}
		
		else if(activeChar.isAttackingNow() && activeChar.getActiveWeaponItem() != null && activeChar.getActiveWeaponItem().getItemType() == L2WeaponType.BOW)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else
		{
			L2BoatInstance boat = BoatManager.getInstance().GetBoat(_boatId);

			if(boat == null)
			{
				return;
			}

			activeChar.setBoat(boat);
			activeChar.setInBoat(true);
			activeChar.setInBoatPosition(_pos);
			activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO_IN_A_BOAT, new L2CharPosition(_pos.getX(), _pos.getY(), _pos.getZ(), 0), new L2CharPosition(_origin_pos.getX(), _origin_pos.getY(), _origin_pos.getZ(), 0));
		}
	}

	@Override
	public String getType()
	{
		return null;
	}

}