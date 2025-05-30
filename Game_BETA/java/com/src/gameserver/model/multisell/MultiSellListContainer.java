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
package com.src.gameserver.model.multisell;

import java.util.List;

import javolution.util.FastList;

public class MultiSellListContainer
{
	private int _listId;
	private boolean _applyTaxes = false;
	private boolean _isCommunity = false;
	private boolean _maintainEnchantment = false;
	private List<Integer> _npcIds;

	private List<MultiSellEntry> _entriesC;

	public MultiSellListContainer()
	{
		_entriesC = new FastList<MultiSellEntry>();
	}

	public void setListId(int listId)
	{
		_listId = listId;
	}

	public void setApplyTaxes(boolean applyTaxes)
	{
		_applyTaxes = applyTaxes;
	}

	public void setMaintainEnchantment(boolean maintainEnchantment)
	{
		_maintainEnchantment = maintainEnchantment;
	}

	public void addNpcId(int objId)
	{
		_npcIds.add(objId);
	}

	public int getListId()
	{
		return _listId;
	}

	public boolean getApplyTaxes()
	{
		return _applyTaxes;
	}

	public boolean getMaintainEnchantment()
	{
		return _maintainEnchantment;
	}

	public void addEntry(MultiSellEntry e)
	{
		_entriesC.add(e);
	}

	public List<MultiSellEntry> getEntries()
	{
		return _entriesC;
	}

	public boolean checkNpcId(int npcId)
	{
		if(_npcIds == null)
		{
			synchronized (this)
			{
				if(_npcIds == null)
				{
					_npcIds = new FastList<Integer>();
				}
			}

			return false;
		}

		return _npcIds.contains(npcId);
	}
	
    public void setIsCommunity(boolean val)
    {
        _isCommunity = val;
    }

    public boolean getIsCommunity()
    {
        return _isCommunity;
    }
}