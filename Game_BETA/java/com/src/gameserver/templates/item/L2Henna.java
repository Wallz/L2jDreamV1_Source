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
package com.src.gameserver.templates.item;

import com.src.gameserver.templates.StatsSet;

public class L2Henna
{
	public final int symbolId;
	public final String symbolName;
	public final int dye;
	public final int price;
	public final int amount;
	public final int statINT;
	public final int statSTR;
	public final int statCON;
	public final int statMEM;
	public final int statDEX;
	public final int statWIT;

	public L2Henna(StatsSet set)
	{
		symbolId = set.getInteger("symbol_id");
		symbolName = "";
		dye = set.getInteger("dye");
		price = set.getInteger("price");
		amount = set.getInteger("amount");
		statINT = set.getInteger("stat_INT");
		statSTR = set.getInteger("stat_STR");
		statCON = set.getInteger("stat_CON");
		statMEM = set.getInteger("stat_MEM");
		statDEX = set.getInteger("stat_DEX");
		statWIT = set.getInteger("stat_WIT");
	}

	public int getSymbolId()
	{
		return symbolId;
	}

	public int getDyeId()
	{
		return dye;
	}

	public int getPrice()
	{
		return price;
	}

	public int getAmountDyeRequire()
	{
		return amount;
	}

	public int getStatINT()
	{
		return statINT;
	}

	public int getStatSTR()
	{
		return statSTR;
	}

	public int getStatCON()
	{
		return statCON;
	}

	public int getStatMEM()
	{
		return statMEM;
	}

	public int getStatDEX()
	{
		return statDEX;
	}

	public int getStatWIT()
	{
		return statWIT;
	}

}