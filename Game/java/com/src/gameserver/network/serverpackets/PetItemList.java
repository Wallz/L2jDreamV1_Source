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


import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.templates.item.L2Item;

public class PetItemList extends L2GameServerPacket
{
	private static final String _S__cb_PETITEMLIST = "[S] b2  PetItemList";
	private L2PetInstance _activeChar;

	public PetItemList(L2PetInstance character)
	{
		_activeChar = character;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xB2);
		
		final L2ItemInstance[] items = _activeChar.getInventory().getItems();
		writeH(items.length);
		
		L2Item item;
		for (L2ItemInstance temp : items)
		{
			if (temp == null || temp.getItem() == null)
				continue;
			
			item = temp.getItem();
			
			writeH(item.getType1());
			writeD(temp.getObjectId());
			writeD(temp.getItemId());
			writeD(temp.getCount());
			writeH(item.getType2());
			writeH(temp.getCustomType1());
			writeH(temp.isEquipped() ? 0x01 : 0x00);
			writeD(item.getBodyPart());
			writeH(temp.getEnchantLevel());
			writeH(temp.getCustomType2());
		}
	}

	@Override
	public String getType()
	{
		return _S__cb_PETITEMLIST;
	}

}