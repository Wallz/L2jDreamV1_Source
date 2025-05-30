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

import com.src.gameserver.model.L2ManufactureItem;
import com.src.gameserver.model.L2ManufactureList;
import com.src.gameserver.model.L2RecipeList;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class RecipeShopManageList extends L2GameServerPacket
{

	private static final String _S__D8_RecipeShopManageList = "[S] d8 RecipeShopManageList";
	private L2PcInstance _seller;
	private boolean _isDwarven;
	private L2RecipeList[] _recipes;

	public RecipeShopManageList(L2PcInstance seller, boolean isDwarven)
	{
		_seller = seller;
		_isDwarven = isDwarven;

		if(_isDwarven && _seller.hasDwarvenCraft())
		{
			_recipes = _seller.getDwarvenRecipeBook();
		}
		else
		{
			_recipes = _seller.getCommonRecipeBook();
		}

		if(_seller.getCreateList() != null)
		{
			L2ManufactureList list = _seller.getCreateList();
			for(L2ManufactureItem item : list.getList())
			{
				if(item.isDwarven() != _isDwarven)
				{
					list.getList().remove(item);
				}
			}
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xd8);
		writeD(_seller.getObjectId());
		writeD(_seller.getAdena());
		writeD(_isDwarven ? 0x00 : 0x01);

		if(_recipes == null)
		{
			writeD(0);
		}
		else
		{
			writeD(_recipes.length);

			for(int i = 0; i < _recipes.length; i++)
			{
				L2RecipeList temp = _recipes[i];
				writeD(temp.getId());
				writeD(i + 1);
			}
		}

		if(_seller.getCreateList() == null)
		{
			writeD(0);
		}
		else
		{
			L2ManufactureList list = _seller.getCreateList();
			writeD(list.size());

			for(L2ManufactureItem item : list.getList())
			{
				writeD(item.getRecipeId());
				writeD(0x00);
				writeD(item.getCost());
			}
		}
	}

	@Override
	public String getType()
	{
		return _S__D8_RecipeShopManageList;
	}

}