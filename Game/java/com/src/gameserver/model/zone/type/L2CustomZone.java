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
package com.src.gameserver.model.zone.type;

import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.zone.L2ZoneType;

public class L2CustomZone extends L2ZoneType
{

	public L2CustomZone(int id)
	{
		super(id);
		_IsFlyingEnable = true;
	}

	@Override
	protected void onDieInside(L2Character l2character)
	{
	}

	@Override
	protected void onReviveInside(L2Character l2character)
	{
	}

	@Override
	public void setParameter(String name, String value)
	{
		if(name.equals("name"))
		{
			_zoneName = value;
		}
		else if(name.equals("flying"))
		{
			_IsFlyingEnable = Boolean.parseBoolean(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}

	@Override
	protected void onEnter(L2Character character)
	{
		if(character instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) character;
			if(!player.isGM() && player.isFlying() && !player.isInJail() && !_IsFlyingEnable)
			{
				player.teleToLocation(com.src.gameserver.datatables.xml.MapRegionTable.TeleportWhereType.Town);
			}

			if(_zoneName.equalsIgnoreCase("tradeoff"))
			{
				player.sendMessage("Trade restrictions are involved.");
				player.setTradeDisabled(true);
			}
		}
	}

	@Override
	protected void onExit(L2Character character)
	{
		if(character instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) character;

			if(_zoneName.equalsIgnoreCase("tradeoff"))
			{
				player.sendMessage("Trade restrictions removed.");
				player.setTradeDisabled(false);
			}
		}
	}

	public String getZoneName()
	{
		return _zoneName;
	}

	public boolean isFlyingEnable()
	{
		return _IsFlyingEnable;
	}

	private String _zoneName;
	private boolean _IsFlyingEnable;

}