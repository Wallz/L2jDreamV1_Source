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

import com.src.gameserver.model.actor.instance.L2RecipeInstance;

public class L2RecipeList
{
	private L2RecipeInstance[] _recipes;
	private int _id;
	private int _level;
	private int _recipeId;
	private String _recipeName;
	private int _successRate;
	private int _mpCost;
	private int _itemId;
	private int _count;
	private boolean _isDwarvenRecipe;

	public L2RecipeList(int id, int level, int recipeId, String recipeName, int successRate, int mpCost, int itemId, int count, boolean isDwarvenRecipe)
	{
		_id = id;
		_recipes = new L2RecipeInstance[0];
		_level = level;
		_recipeId = recipeId;
		_recipeName = recipeName;
		_successRate = successRate;
		_mpCost = mpCost;
		_itemId = itemId;
		_count = count;
		_isDwarvenRecipe = isDwarvenRecipe;
	}

	public void addRecipe(L2RecipeInstance recipe)
	{
		int len = _recipes.length;
		L2RecipeInstance[] tmp = new L2RecipeInstance[len + 1];
		System.arraycopy(_recipes, 0, tmp, 0, len);
		tmp[len] = recipe;
		_recipes = tmp;
		tmp = null;
	}

	public int getId()
	{
		return _id;
	}

	public int getLevel()
	{
		return _level;
	}

	public int getRecipeId()
	{
		return _recipeId;
	}

	public String getRecipeName()
	{
		return _recipeName;
	}

	public int getSuccessRate()
	{
		return _successRate;
	}

	public int getMpCost()
	{
		return _mpCost;
	}

	public boolean isConsumable()
	{
		return _itemId >= 1463 && _itemId <= 1467 || _itemId >= 2509 && _itemId <= 2514 || _itemId >= 3947 && _itemId <= 3952 || _itemId >= 1341 && _itemId <= 1345;
	}

	public int getItemId()
	{
		return _itemId;
	}

	public int getCount()
	{
		return _count;
	}

	public boolean isDwarvenRecipe()
	{
		return _isDwarvenRecipe;
	}

	public L2RecipeInstance[] getRecipes()
	{
		return _recipes;
	}

}