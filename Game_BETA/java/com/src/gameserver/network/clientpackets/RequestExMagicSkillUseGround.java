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

import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.ValidateLocation;
import com.src.util.object.Point3D;

public final class RequestExMagicSkillUseGround extends L2GameClientPacket
{
	private static final String _C__D0_2F_REQUESTEXMAGICSKILLUSEGROUND = "[C] D0:2F RequestExMagicSkillUseGround";

	private int _x;
	private int _y;
	private int _z;
	private int _skillId;
	private int _ctrlPressed;
	private int _shiftPressed;

	@Override
	protected void readImpl()
	{
		_x = readD();
		_y = readD();
		_z = readD();
		_skillId = readD();
		_ctrlPressed = readD();
		_shiftPressed = readC();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if(activeChar == null)
		{
			return;
		}

		int level = activeChar.getSkillLevel(_skillId);

		if(level <= 0)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		L2Skill skill = SkillTable.getInstance().getInfo(_skillId, level);

		if(skill != null)
		{
			activeChar.setCurrentSkillWorldPosition(new Point3D(_x, _y, _z));
			activeChar.broadcastPacket(new ValidateLocation(activeChar));
			activeChar.useMagic(skill, _ctrlPressed == 1 ? true : false, _shiftPressed == 1 ? true : false);
		}
		else
		{
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}

	@Override
	public String getType()
	{
		return _C__D0_2F_REQUESTEXMAGICSKILLUSEGROUND;
	}

}