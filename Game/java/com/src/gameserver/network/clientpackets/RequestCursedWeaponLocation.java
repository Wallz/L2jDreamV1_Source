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

import java.util.List;

import javolution.util.FastList;

import com.src.gameserver.managers.CursedWeaponsManager;
import com.src.gameserver.model.CursedWeapon;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.network.serverpackets.ExCursedWeaponLocation;
import com.src.gameserver.network.serverpackets.ExCursedWeaponLocation.CursedWeaponInfo;
import com.src.util.object.Point3D;

public final class RequestCursedWeaponLocation extends L2GameClientPacket
{
	private static final String _C__D0_23_REQUESTCURSEDWEAPONLOCATION = "[C] D0:23 RequestCursedWeaponLocation";

	@Override
	protected void readImpl()
	{
	}

	@Override
	protected void runImpl()
	{
		L2Character activeChar = getClient().getActiveChar();
		if(activeChar == null)
		{
			return;
		}

		List<CursedWeaponInfo> list = new FastList<CursedWeaponInfo>();
		for(CursedWeapon cw : CursedWeaponsManager.getInstance().getCursedWeapons())
		{
			if(!cw.isActive())
			{
				continue;
			}

			Point3D pos = cw.getWorldPosition();

			if(pos != null)
			{
				list.add(new CursedWeaponInfo(pos, cw.getItemId(), cw.isActivated() ? 1 : 0));
			}
		}

		if(!list.isEmpty())
		{
			activeChar.sendPacket(new ExCursedWeaponLocation(list));
		}
	}

	@Override
	public String getType()
	{
		return _C__D0_23_REQUESTCURSEDWEAPONLOCATION;
	}

}