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

import com.src.gameserver.model.L2RecipeList;

public class RecipeBookItemList extends L2GameServerPacket
{
	private static final String _S__D6_RECIPEBOOKITEMLIST = "[S] D6 RecipeBookItemList";
	private L2RecipeList[] _recipes;
	private boolean _isDwarvenCraft;
	private int _maxMp;

	public RecipeBookItemList(boolean isDwarvenCraft, int maxMp)
	{
		_isDwarvenCraft = isDwarvenCraft;
		_maxMp = maxMp;
	}

	public void addRecipes(L2RecipeList[] recipeBook)
	{
		_recipes = recipeBook;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xD6);

		writeD(_isDwarvenCraft ? 0x00 : 0x01);
		writeD(_maxMp);

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
	}

	@Override
	public String getType()
	{
		return _S__D6_RECIPEBOOKITEMLIST;
	}

}