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
package com.src.gameserver.model;

import javolution.util.FastList;

import com.src.Config;
import com.src.util.random.Rnd;

public class L2DropCategory
{
	private FastList<L2DropData> _drops;
	private int _categoryChance;
	private int _categoryBalancedChance;
	private int _categoryType;

	public L2DropCategory(int categoryType)
	{
		_categoryType = categoryType;
		_drops = new FastList<L2DropData>(0);
		_categoryChance = 0;
		_categoryBalancedChance = 0;
	}

	public void addDropData(L2DropData drop, boolean raid)
	{
		_drops.add(drop);
		_categoryChance += drop.getChance();
		_categoryBalancedChance += Math.min((drop.getChance() * (raid ? 1 : Config.RATE_DROP_ITEMS)), L2DropData.MAX_CHANCE);
	}

	public FastList<L2DropData> getAllDrops()
	{
		return _drops;
	}

	public void clearAllDrops()
	{
		_drops.clear();
	}

	public boolean isSweep()
	{
		return getCategoryType() == -1;
	}

	public int getCategoryChance()
	{
		if(getCategoryType() >= 0)
		{
			return _categoryChance;
		}
		else
		{
			return L2DropData.MAX_CHANCE;
		}
	}

	public int getCategoryBalancedChance()
	{
		if(getCategoryType() >= 0)
		{
			return _categoryBalancedChance;
		}
		else
		{
			return L2DropData.MAX_CHANCE;
		}
	}

	public int getCategoryType()
	{
		return _categoryType;
	}

	public synchronized L2DropData dropSeedAllowedDropsOnly()
	{
		FastList<L2DropData> drops = new FastList<L2DropData>();
		int subCatChance = 0;
		for(L2DropData drop : getAllDrops())
		{
			if(drop.getItemId() == 57 || drop.getItemId() == 6360 || drop.getItemId() == 6361 || drop.getItemId() == 6362)
			{
				drops.add(drop);
				subCatChance += drop.getChance();
			}
		}

		int randomIndex = Rnd.get(subCatChance);
		int sum = 0;
		for(L2DropData drop : drops)
		{
			sum += drop.getChance();

			if(sum > randomIndex)
			{
				drops.clear();
				drops = null;
				return drop;
			}
		}

		drops = null;

		return null;
	}

	public synchronized L2DropData dropOne(boolean raid)
	{
		int randomIndex = Rnd.get(getCategoryBalancedChance());
		int sum = 0;
		for(L2DropData drop : getAllDrops())
		{
			sum += Math.min((drop.getChance() * (raid ? 1 : Config.RATE_DROP_ITEMS)), L2DropData.MAX_CHANCE);

			if(sum >= randomIndex)
			{
				return drop;
			}
		}
		return null;
	}

}