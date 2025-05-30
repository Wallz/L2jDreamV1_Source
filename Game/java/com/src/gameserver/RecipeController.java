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
package com.src.gameserver;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import javolution.util.FastList;

import com.src.Config;
import com.src.gameserver.datatables.xml.RecipeTable;
import com.src.gameserver.model.L2ManufactureItem;
import com.src.gameserver.model.L2RecipeList;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2RecipeInstance;
import com.src.gameserver.model.itemcontainer.Inventory;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.network.serverpackets.MagicSkillUser;
import com.src.gameserver.network.serverpackets.RecipeBookItemList;
import com.src.gameserver.network.serverpackets.RecipeItemMakeInfo;
import com.src.gameserver.network.serverpackets.RecipeShopItemInfo;
import com.src.gameserver.network.serverpackets.SetupGauge;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.Stats;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.gameserver.util.Util;
import com.src.util.random.Rnd;

public class RecipeController
{
	private static final Logger _log = Logger.getLogger(RecipeController.class.getName());

	private static RecipeController _instance;
	private Map<Integer, L2RecipeList> _lists;
	protected static final Map<L2PcInstance, RecipeItemMaker> _activeMakers = Collections.synchronizedMap(new WeakHashMap<L2PcInstance, RecipeItemMaker>());

	public static RecipeController getInstance()
	{
		return _instance == null ? _instance = new RecipeController() : _instance;
	}

	public synchronized void requestBookOpen(L2PcInstance player, boolean isDwarvenCraft)
	{
		RecipeItemMaker maker = null;
		if(Config.ALT_GAME_CREATION)
		{
			maker = _activeMakers.get(player);
		}

		if(maker == null)
		{
			RecipeBookItemList response = new RecipeBookItemList(isDwarvenCraft, player.getMaxMp());
			response.addRecipes(isDwarvenCraft ? player.getDwarvenRecipeBook() : player.getCommonRecipeBook());
			player.sendPacket(response);
			response = null;
			return;
		}

		player.sendPacket(new SystemMessage(SystemMessageId.CANT_ALTER_RECIPEBOOK_WHILE_CRAFTING));

		maker = null;

		return;
	}

	public synchronized void requestMakeItemAbort(L2PcInstance player)
	{
		_activeMakers.remove(player);
	}

	public synchronized void requestManufactureItem(L2PcInstance manufacturer, int recipeListId, L2PcInstance player)
	{
		L2RecipeList recipeList = getValidRecipeList(player, recipeListId);

		if(recipeList == null)
			return;

		List<L2RecipeList> dwarfRecipes = Arrays.asList(manufacturer.getDwarvenRecipeBook());
		List<L2RecipeList> commonRecipes = Arrays.asList(manufacturer.getCommonRecipeBook());

		if(!dwarfRecipes.contains(recipeList) && !commonRecipes.contains(recipeList))
		{
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false recipe id.", Config.DEFAULT_PUNISH);
			commonRecipes = null;
			dwarfRecipes = null;
			return;
		}

		RecipeItemMaker maker;

		if(Config.ALT_GAME_CREATION && (maker = _activeMakers.get(manufacturer)) != null) // check if busy
		{
			player.sendMessage("Manufacturer is busy, please try later.");
			return;
		}

		maker = new RecipeItemMaker(manufacturer, recipeList, player);
		if(maker._isValid)
		{
			if(Config.ALT_GAME_CREATION)
			{
				_activeMakers.put(manufacturer, maker);
				ThreadPoolManager.getInstance().scheduleGeneral(maker, 100);
			}
			else
			{
				maker.run();
			}
		}
		maker = null;
		recipeList = null;
	}

	public synchronized void requestMakeItem(L2PcInstance player, int recipeListId)
	{
		if(player.isInDuel())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANT_CRAFT_DURING_COMBAT));
			return;
		}

		L2RecipeList recipeList = getValidRecipeList(player, recipeListId);

		if(recipeList == null)
		{
			return;
		}

		List<L2RecipeList> dwarfRecipes = Arrays.asList(player.getDwarvenRecipeBook());
		List<L2RecipeList> commonRecipes = Arrays.asList(player.getCommonRecipeBook());

		if(!dwarfRecipes.contains(recipeList) && !commonRecipes.contains(recipeList))
		{
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false recipe id.", Config.DEFAULT_PUNISH);
			dwarfRecipes = null;
			commonRecipes = null;
			return;
		}

		RecipeItemMaker maker;

		if(Config.ALT_GAME_CREATION && (maker = _activeMakers.get(player)) != null)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("You are busy creating ").addItemName(recipeList.getItemId()));
			return;
		}

		maker = new RecipeItemMaker(player, recipeList, player);
		if(maker._isValid)
		{
			if(Config.ALT_GAME_CREATION)
			{
				_activeMakers.put(player, maker);
				ThreadPoolManager.getInstance().scheduleGeneral(maker, 100);
			}
			else
			{
				maker.run();
			}
		}
		maker = null;
		recipeList = null;
	}

	private class RecipeItemMaker implements Runnable
	{
		protected boolean _isValid;
		protected List<TempItem> _items = null;
		protected final L2RecipeList _recipeList;
		protected final L2PcInstance _player;
		protected final L2PcInstance _target;
		protected final L2Skill _skill;
		protected final int _skillId;
		protected final int _skillLevel;
		protected double _creationPasses;
		protected double _manaRequired;
		protected int _price;
		protected int _totalItems;
		@SuppressWarnings("unused")
		protected int _materialsRefPrice;
		protected int _delay;

		public RecipeItemMaker(L2PcInstance pPlayer, L2RecipeList pRecipeList, L2PcInstance pTarget)
		{
			_player = pPlayer;
			_target = pTarget;
			_recipeList = pRecipeList;

			_isValid = false;
			_skillId = _recipeList.isDwarvenRecipe() ? L2Skill.SKILL_CREATE_DWARVEN : L2Skill.SKILL_CREATE_COMMON;
			_skillLevel = _player.getSkillLevel(_skillId);
			_skill = _player.getKnownSkill(_skillId);

			_player.isInCraftMode(true);

			if(_player.isAlikeDead())
			{
				_player.sendMessage("Dead people don't craft.");
				_player.sendPacket(ActionFailed.STATIC_PACKET);
				abort();
				return;
			}

			if(_target.isAlikeDead())
			{
				_target.sendMessage("Dead customers can't use manufacture.");
				_target.sendPacket(ActionFailed.STATIC_PACKET);
				abort();
				return;
			}

			if(_target.isProcessingTransaction())
			{
				_target.sendMessage("You are busy.");
				_target.sendPacket(ActionFailed.STATIC_PACKET);
				abort();
				return;
			}

			if(_player.isProcessingTransaction())
			{
				if(_player != _target)
				{
					_target.sendMessage("Manufacturer " + _player.getName() + " is busy.");
				}
				_player.sendPacket(ActionFailed.STATIC_PACKET);
				abort();
				return;
			}

			if(_recipeList == null || _recipeList.getRecipes().length == 0)
			{
				_player.sendMessage("No such recipe");
				_player.sendPacket(ActionFailed.STATIC_PACKET);
				abort();
				return;
			}

			_manaRequired = _recipeList.getMpCost();

			if(_recipeList.getLevel() > _skillLevel)
			{
				_player.sendMessage("Need skill level " + _recipeList.getLevel());
				_player.sendPacket(ActionFailed.STATIC_PACKET);
				abort();
				return;
			}

			if(_player != _target)
			{
				for(L2ManufactureItem temp : _player.getCreateList().getList())
				{
					if(temp.getRecipeId() == _recipeList.getId())
					{
						_price = temp.getCost();
						if(_target.getAdena() < _price)
						{
							_target.sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
							abort();
							return;
						}
						break;
					}
				}
			}

			if((_items = listItems(false)) == null)
			{
				abort();
				return;
			}

			for(TempItem i : _items)
			{
				_materialsRefPrice += i.getReferencePrice() * i.getQuantity();
				_totalItems += i.getQuantity();
			}

			if(_player.getCurrentMp() < _manaRequired)
			{
				_target.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_MP));
				abort();
				return;
			}

			_creationPasses = _totalItems / _skillLevel + (_totalItems % _skillLevel != 0 ? 1 : 0);

			if(Config.ALT_GAME_CREATION && _creationPasses != 0)
			{
				_manaRequired /= _creationPasses;
			}

			updateMakeInfo(true);
			updateCurMp();
			updateCurLoad();

			_player.isInCraftMode(false);
			_isValid = true;
		}

		@Override
		public void run()
		{
			if(!Config.IS_CRAFTING_ENABLED)
			{
				_target.sendMessage("Item creation is currently disabled.");
				abort();
				return;
			}

			if(_player == null || _target == null)
			{
				_log.warning("player or target == null (disconnected?), aborting" + _target + _player);
				abort();
				return;
			}

			if(_player.isOnline() == 0 || _target.isOnline() == 0)
			{
				_log.warning("player or target is not online, aborting " + _target + _player);
				abort();
				return;
			}

			if(Config.ALT_GAME_CREATION && _activeMakers.get(_player) == null)
			{
				if(_target != _player)
				{
					_target.sendMessage("Manufacture aborted");
					_player.sendMessage("Manufacture aborted");
				}
				else
				{
					_player.sendMessage("Item creation aborted");
				}

				abort();
				return;
			}

			if(Config.ALT_GAME_CREATION && !_items.isEmpty())
			{
				if(!validateMp())
				{
					return;
				}

				_player.reduceCurrentMp(_manaRequired);
				updateCurMp();
				grabSomeItems();

				if(!_items.isEmpty())
				{
					_delay = (int) (Config.ALT_GAME_CREATION_SPEED * _player.getMReuseRate(_skill) * GameTimeController.TICKS_PER_SECOND / Config.RATE_CONSUMABLE_COST) * GameTimeController.MILLIS_IN_TICK;

					MagicSkillUser msk = new MagicSkillUser(_player, _skillId, _skillLevel, _delay, 0);
					_player.broadcastPacket(msk);
					msk = null;

					_player.sendPacket(new SetupGauge(0, _delay));
					ThreadPoolManager.getInstance().scheduleGeneral(this, 100 + _delay);
				}
				else
				{
					_player.sendPacket(new SetupGauge(0, _delay));

					try
					{
						Thread.sleep(_delay);
					}
					catch(InterruptedException e)
					{}
					finally
					{
						finishCrafting();
					}
				}
			}
			else
			{
				finishCrafting();
			}
		}

		private void finishCrafting()
		{
			if(!Config.ALT_GAME_CREATION)
			{
				_player.reduceCurrentMp(_manaRequired);
			}

			if(_target != _player && _price > 0)
			{
				L2ItemInstance adenatransfer = _target.transferItem("PayManufacture", _target.getInventory().getAdenaInstance().getObjectId(), _price, _player.getInventory(), _player);

				if(adenatransfer == null)
				{
					_target.sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
					abort();
					return;
				}
				adenatransfer = null;
			}

			if((_items = listItems(true)) == null)
			{}
			else if(Rnd.get(100) < _recipeList.getSuccessRate())
			{
				rewardPlayer();
				updateMakeInfo(true);
			}
			else
			{
				_player.sendMessage("Item(s) failed to create");
				if(_target != _player)
				{
					_target.sendMessage("Item(s) failed to create");
				}

				updateMakeInfo(false);
			}
			updateCurMp();
			updateCurLoad();
			_activeMakers.remove(_player);
			_player.isInCraftMode(false);
			_target.sendPacket(new ItemList(_target, false));
		}

		private void updateMakeInfo(boolean success)
		{
			if(_target == _player)
			{
				_target.sendPacket(new RecipeItemMakeInfo(_recipeList.getId(), _target, success));
			}
			else
			{
				_target.sendPacket(new RecipeShopItemInfo(_player.getObjectId(), _recipeList.getId()));
			}
		}

		private void updateCurLoad()
		{
			StatusUpdate su = new StatusUpdate(_target.getObjectId());
			su.addAttribute(StatusUpdate.CUR_LOAD, _target.getCurrentLoad());
			_target.sendPacket(su);
		}

		private void updateCurMp()
		{
			StatusUpdate su = new StatusUpdate(_target.getObjectId());
			su.addAttribute(StatusUpdate.CUR_MP, (int) _target.getCurrentMp());
			_target.sendPacket(su);
		}

		private void grabSomeItems()
		{
			int numItems = _skillLevel;

			while(numItems > 0 && !_items.isEmpty())
			{
				TempItem item = _items.get(0);

				int count = item.getQuantity();

				if(count >= numItems)
				{
					count = numItems;
				}

				item.setQuantity(item.getQuantity() - count);
				if(item.getQuantity() <= 0)
				{
					_items.remove(0);
				}
				else
				{
					_items.set(0, item);
				}

				numItems -= count;

				if(_target == _player)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2_EQUIPPED);
					sm.addNumber(count);
					sm.addItemName(item.getItemId());
					_player.sendPacket(sm);
					sm = null;
				}
				else
				{
					_target.sendMessage("Manufacturer " + _player.getName() + " used " + count + " " + item.getItemName());
				}
				item = null;
			}
		}

		private boolean validateMp()
		{
			if(_player.getCurrentMp() < _manaRequired)
			{
				if(Config.ALT_GAME_CREATION)
				{
					_player.sendPacket(new SetupGauge(0, _delay));
					ThreadPoolManager.getInstance().scheduleGeneral(this, 100 + _delay);
				}
				else
				{
					_target.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_MP));
					abort();
				}
				return false;
			}
			return true;
		}

		private List<TempItem> listItems(boolean remove)
		{
			L2RecipeInstance[] recipes = _recipeList.getRecipes();
			Inventory inv = _target.getInventory();
			List<TempItem> materials = new FastList<TempItem>();

			for(L2RecipeInstance recipe : recipes)
			{
				int quantity = _recipeList.isConsumable() ? (int) (recipe.getQuantity() * Config.RATE_CONSUMABLE_COST) : (int) recipe.getQuantity();

				if(quantity > 0)
				{
					L2ItemInstance item = inv.getItemByItemId(recipe.getItemId());

					if(item == null || item.getCount() < quantity)
					{
						_target.sendMessage("You dont have the right elements for making this item" + (_recipeList.isConsumable() && Config.RATE_CONSUMABLE_COST != 1 ? ".\nDue to server rates you need " + Config.RATE_CONSUMABLE_COST + "x more material than listed in recipe" : ""));
						abort();
						return null;
					}

					TempItem temp = new TempItem(item, quantity);
					materials.add(temp);
					temp = null;
				}
			}

			recipes = null;

			if(remove)
			{
				for(TempItem tmp : materials)
				{
					inv.destroyItemByItemId("Manufacture", tmp.getItemId(), tmp.getQuantity(), _target, _player);
				}
			}
			inv = null;

			return materials;
		}

		private void abort()
		{
			updateMakeInfo(false);
			_player.isInCraftMode(false);
			_activeMakers.remove(_player);
		}

		private class TempItem
		{
			private int _itemId;
			private int _quantity;
			private int _referencePrice;
			private String _itemName;

			public TempItem(L2ItemInstance item, int quantity)
			{
				super();
				_itemId = item.getItemId();
				_quantity = quantity;
				_itemName = item.getItem().getName();
				_referencePrice = item.getReferencePrice();
			}

			public int getQuantity()
			{
				return _quantity;
			}

			public void setQuantity(int quantity)
			{
				_quantity = quantity;
			}

			public int getReferencePrice()
			{
				return _referencePrice;
			}

			public int getItemId()
			{
				return _itemId;
			}

			public String getItemName()
			{
				return _itemName;
			}
		}

		private void rewardPlayer()
		{
			int itemId = _recipeList.getItemId();
			int itemCount = _recipeList.getCount();

			L2ItemInstance createdItem = _target.getInventory().addItem("Manufacture", itemId, itemCount, _target, _player);

			if(itemCount > 1)
			{
				_target.sendPacket(new SystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(itemId).addNumber(itemCount));
			}
			else
			{
				_target.sendPacket(new SystemMessage(SystemMessageId.EARNED_ITEM_S1).addItemName(itemId));
			}

			if(_target != _player)
			{
				_player.sendPacket(new SystemMessage(SystemMessageId.EARNED_S1_ADENA).addNumber(_price));
			}

			if(Config.ALT_GAME_CREATION)
			{
				int recipeLevel = _recipeList.getLevel();
				int exp = createdItem.getReferencePrice() * itemCount;

				if(exp < 0)
				{
					exp = 0;
				}

				exp /= recipeLevel;
				for(int i = _skillLevel; i > recipeLevel; i--)
				{
					exp /= 4;
				}

				int sp = exp / 10;

				_player.addExpAndSp((int) _player.calcStat(Stats.EXPSP_RATE, exp * Config.ALT_GAME_CREATION_XP_RATE * Config.ALT_GAME_CREATION_SPEED, null, null), (int) _player.calcStat(Stats.EXPSP_RATE, sp * Config.ALT_GAME_CREATION_SP_RATE * Config.ALT_GAME_CREATION_SPEED, null, null));
			}
			updateMakeInfo(true);
		}
	}

	private L2RecipeList getValidRecipeList(L2PcInstance player, int id)
	{
		L2RecipeList recipeList = RecipeTable.getInstance().getRecipeList(id - 1);

		if(recipeList == null || recipeList.getRecipes().length == 0)
		{
			player.sendMessage("No recipe for: " + id);
			player.isInCraftMode(false);
			return null;
		}
		return recipeList;
	}

	public L2RecipeList getRecipeByItemId(int itemId)
	{
		for(int i = 0; i < _lists.size(); i++)
		{
			L2RecipeList find = _lists.get(new Integer(i));
			if(find.getRecipeId() == itemId)
			{
				return find;
			}
		}
		return null;
	}

}