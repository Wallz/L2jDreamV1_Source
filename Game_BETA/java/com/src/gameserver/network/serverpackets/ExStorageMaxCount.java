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

import com.src.gameserver.model.actor.instance.L2PcInstance;

public class ExStorageMaxCount extends L2GameServerPacket
{
	private static final String _S__FE_2E_EXSTORAGEMAXCOUNT = "[S] FE:2E ExStorageMaxCount";
	private L2PcInstance _activeChar;
	private int _inventory;
	private int _warehouse;
	private int _freight;
	private int _privateSell;
	private int _privateBuy;
	private int _receipeD;
	private int _recipe;

	public ExStorageMaxCount(L2PcInstance character)
	{
		_activeChar = character;
		_inventory = _activeChar.GetInventoryLimit();
		_warehouse = _activeChar.GetWareHouseLimit();
		_privateSell = _activeChar.GetPrivateSellStoreLimit();
		_privateBuy = _activeChar.GetPrivateBuyStoreLimit();
		_freight = _activeChar.GetFreightLimit();
		_receipeD = _activeChar.GetDwarfRecipeLimit();
		_recipe = _activeChar.GetCommonRecipeLimit();
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x2e);

		writeD(_inventory);
		writeD(_warehouse);
		writeD(_freight);
		writeD(_privateSell);
		writeD(_privateBuy);
		writeD(_receipeD);
		writeD(_recipe);
	}

	@Override
	public String getType()
	{
		return _S__FE_2E_EXSTORAGEMAXCOUNT;
	}

}