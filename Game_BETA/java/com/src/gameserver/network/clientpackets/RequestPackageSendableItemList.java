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

import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.network.serverpackets.PackageSendableList;

public final class RequestPackageSendableItemList extends L2GameClientPacket
{
	private static final String _C_9E_REQUESTPACKAGESENDABLEITEMLIST = "[C] 9E RequestPackageSendableItemList";

	private int _objectID;

	@Override
	protected void readImpl()
	{
		_objectID = readD();
	}

	@Override
	public void runImpl()
	{
		L2ItemInstance[] items = getClient().getActiveChar().getInventory().getAvailableItems(true);
		sendPacket(new PackageSendableList(items, _objectID));
	}

	@Override
	public String getType()
	{
		return _C_9E_REQUESTPACKAGESENDABLEITEMLIST;
	}

}