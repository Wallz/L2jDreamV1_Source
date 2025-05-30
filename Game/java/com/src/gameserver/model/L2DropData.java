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

import java.util.Arrays;

public class L2DropData
{
	public static final int MAX_CHANCE = 1000000;

	private int _itemId;
	private int _minDrop;
	private int _maxDrop;
	private int _chance;
	private String _questID = null;
	private String[] _stateID = null;

	public int getItemId()
	{
		return _itemId;
	}

	public void setItemId(int itemId)
	{
		_itemId = itemId;
	}

	public int getMinDrop()
	{
		return _minDrop;
	}

	public int getMaxDrop()
	{
		return _maxDrop;
	}

	public int getChance()
	{
		return _chance;
	}

	public void setMinDrop(int mindrop)
	{
		_minDrop = mindrop;
	}

	public void setMaxDrop(int maxdrop)
	{
		_maxDrop = maxdrop;
	}

	public void setChance(int chance)
	{
		_chance = chance;
	}

	public String[] getStateIDs()
	{
		return _stateID;
	}

	public void addStates(String[] list)
	{
		_stateID = list;
	}

	public String getQuestID()
	{
		return _questID;
	}

	public void setQuestID(String questID)
	{
		_questID = questID;
	}

	public boolean isQuestDrop()
	{
		return _questID != null && _stateID != null;
	}

	@Override
	public String toString()
	{
		String out = "ItemID: " + getItemId() + " Min: " + getMinDrop() + " Max: " + getMaxDrop() + " Chance: " + getChance() / 10000.0 + "%";
		if(isQuestDrop())
		{
			out += " QuestID: " + getQuestID() + " StateID's: " + Arrays.toString(getStateIDs());
		}

		return out;
	}

	@Override
	public boolean equals(Object o)
	{
		if(o instanceof L2DropData)
		{
			L2DropData drop = (L2DropData) o;
			return drop.getItemId() == getItemId();
		}

		return false;
	}

}