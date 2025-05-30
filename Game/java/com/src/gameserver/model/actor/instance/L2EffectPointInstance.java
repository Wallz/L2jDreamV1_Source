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
package com.src.gameserver.model.actor.instance;

import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public class L2EffectPointInstance extends L2Npc
{
	private final L2Character _owner;

	public L2EffectPointInstance(int objectId, L2NpcTemplate template, L2Character owner)
	{
		super(objectId, template);
		_owner = owner;
	}

	public L2Character getOwner()
	{
		return _owner;
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public void onActionShift(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

}