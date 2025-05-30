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
package com.src.gameserver.model.actor.knownlist;

import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2RaceManagerInstance;
import com.src.gameserver.model.entity.MonsterRace;
import com.src.gameserver.network.serverpackets.DeleteObject;

public class RaceManagerKnownList extends NpcKnownList
{
	public RaceManagerKnownList(L2RaceManagerInstance activeChar)
	{
		super(activeChar);
	}

	@Override
	public boolean addKnownObject(L2Object object)
	{
		return addKnownObject(object, null);
	}

	@Override
	public boolean addKnownObject(L2Object object, L2Character dropper)
	{
		if(!super.addKnownObject(object, dropper))
		{
			return false;
		}

		return true;
	}

	@Override
	public boolean removeKnownObject(L2Object object)
	{
		if(!super.removeKnownObject(object))
		{
			return false;
		}

		if(object instanceof L2PcInstance)
		{
			DeleteObject obj = null;
			for(int i = 0; i < 8; i++)
			{
				obj = new DeleteObject(MonsterRace.getInstance().getMonsters()[i]);
				((L2PcInstance) object).sendPacket(obj);
			}

			obj = null;
		}

		return true;
	}

	@Override
	public L2RaceManagerInstance getActiveChar()
	{
		return (L2RaceManagerInstance) super.getActiveChar();
	}

}