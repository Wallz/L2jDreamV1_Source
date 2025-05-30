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
package com.src.gameserver.datatables;

import java.util.List;

import javolution.util.FastList;

public class CrownTable
{
	private static List<Integer> _crownList = new FastList<Integer>();

	public static List<Integer> getCrownList()
	{
		if(_crownList.isEmpty())
		{
			_crownList.add(6841);
			_crownList.add(6834);
			_crownList.add(6835);
			_crownList.add(6836);
			_crownList.add(6837);
			_crownList.add(6838);
			_crownList.add(6839);
			_crownList.add(6840);
			_crownList.add(8182);
			_crownList.add(8183);
		}

		return _crownList;
	}

	public static int getCrownId(int CastleId)
	{
		int CrownId = 0;
		switch(CastleId)
		{
			case 1:
				CrownId = 6838;
				break;
			case 2:
				CrownId = 6835;
				break;
			case 3:
				CrownId = 6839;
				break;
			case 4:
				CrownId = 6837;
				break;
			case 5:
				CrownId = 6840;
				break;
			case 6:
				CrownId = 6834;
				break;
			case 7:
				CrownId = 6836;
				break;
			case 8:
				CrownId = 8182;
				break;
			case 9:
				CrownId = 8183;
				break;
			default:
				CrownId = 0;
				break;
		}
		return CrownId;
	}
}