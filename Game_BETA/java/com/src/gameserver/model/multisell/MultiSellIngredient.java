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

public class MultiSellIngredient
{
	private int _itemId, _itemCount, _enchantmentLevel;
	private boolean _isTaxIngredient, _mantainIngredient;

	public MultiSellIngredient(int itemId, int itemCount, boolean isTaxIngredient, boolean mantainIngredient)
	{
		this(itemId, itemCount, 0, isTaxIngredient, mantainIngredient);
	}

	public MultiSellIngredient(int itemId, int itemCount, int enchantmentLevel, boolean isTaxIngredient, boolean mantainIngredient)
	{
		setItemId(itemId);
		setItemCount(itemCount);
		setEnchantmentLevel(enchantmentLevel);
		setIsTaxIngredient(isTaxIngredient);
		setMantainIngredient(mantainIngredient);
	}

	public MultiSellIngredient(MultiSellIngredient e)
	{
		_itemId = e.getItemId();
		_itemCount = e.getItemCount();
		_enchantmentLevel = e.getEnchantmentLevel();
		_isTaxIngredient = e.isTaxIngredient();
		_mantainIngredient = e.getMantainIngredient();
	}

	public void setItemId(int itemId)
	{
		_itemId = itemId;
	}

	public int getItemId()
	{
		return _itemId;
	}

	public void setItemCount(int itemCount)
	{
		_itemCount = itemCount;
	}

	public int getItemCount()
	{
		return _itemCount;
	}

	public void setEnchantmentLevel(int enchantmentLevel)
	{
		_enchantmentLevel = enchantmentLevel;
	}

	public int getEnchantmentLevel()
	{
		return _enchantmentLevel;
	}

	public void setIsTaxIngredient(boolean isTaxIngredient)
	{
		_isTaxIngredient = isTaxIngredient;
	}

	public boolean isTaxIngredient()
	{
		return _isTaxIngredient;
	}

	public void setMantainIngredient(boolean mantainIngredient)
	{
		_mantainIngredient = mantainIngredient;
	}

	public boolean getMantainIngredient()
	{
		return _mantainIngredient;
	}
}