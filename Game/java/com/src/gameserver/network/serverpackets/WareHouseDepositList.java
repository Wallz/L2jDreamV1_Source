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

import javolution.util.FastList;

import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class WareHouseDepositList extends L2GameServerPacket
{
	public static final int PRIVATE = 1;
	public static final int CLAN = 2;
	public static final int CASTLE = 3;
	public static final int FREIGHT = 4;
	private static final String _S__53_WAREHOUSEDEPOSITLIST = "[S] 41 WareHouseDepositList";
	private L2PcInstance _activeChar;
	private int _playerAdena;
	private FastList<L2ItemInstance> _items;
	private int _whType;

	public WareHouseDepositList(L2PcInstance player, int type)
	{
		_activeChar = player;
		_whType = type;
		_playerAdena = _activeChar.getAdena();
		_items = new FastList<L2ItemInstance>();

		for(L2ItemInstance temp : _activeChar.getInventory().getAvailableItems(true))
		{
			_items.add(temp);
		}

		if(_whType == PRIVATE)
		{
			for(L2ItemInstance temp : player.getInventory().getItems())
			{
				if(temp != null && !temp.isEquipped() && (temp.isShadowItem() || temp.isAugmented()))
				{
					_items.add(temp);
				}
			}
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x41);
		writeH(_whType);
		writeD(_playerAdena);
		int count = _items.size();
		writeH(count);

		for(L2ItemInstance item : _items)
		{
			writeH(item.getItem().getType1());
			writeD(item.getObjectId());
			writeD(item.getItemId());
			writeD(item.getCount());
			writeH(item.getItem().getType2());
			writeH(0x00);
			writeD(item.getItem().getBodyPart());
			writeH(item.getEnchantLevel());
			writeH(0x00);
			writeH(0x00);
			writeD(item.getObjectId());
			if(item.isAugmented())
			{
				writeD(0x0000FFFF & item.getAugmentation().getAugmentationId());
				writeD(item.getAugmentation().getAugmentationId() >> 16);
			}
			else
			{
				writeQ(0x00);
			}
		}
	}

	@Override
	public String getType()
	{
		return _S__53_WAREHOUSEDEPOSITLIST;
	}

}