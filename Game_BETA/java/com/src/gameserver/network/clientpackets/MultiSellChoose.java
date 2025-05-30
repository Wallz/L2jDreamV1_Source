/* This program is free software; you can redistribute it and/or modify
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
package com.src.gameserver.network.clientpackets;

import java.util.logging.Logger;

import javolution.util.FastList;

import com.src.Config;
import com.src.gameserver.datatables.sql.ItemTable;
import com.src.gameserver.model.L2Augmentation;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.itemcontainer.PcInventory;
import com.src.gameserver.model.multisell.L2Multisell;
import com.src.gameserver.model.multisell.MultiSellEntry;
import com.src.gameserver.model.multisell.MultiSellIngredient;
import com.src.gameserver.model.multisell.MultiSellListContainer;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.item.L2Armor;
import com.src.gameserver.templates.item.L2Item;
import com.src.gameserver.templates.item.L2Weapon;

public final class MultiSellChoose extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(MultiSellChoose.class.getName());

	private static final String _C__A7_MULTISELLCHOOSE = "[C] A7 MultiSellChoose";

	private int _listId;
	private int _entryId;
	private int _amount;
	private int _enchantment;
	private int _transactionTax;

	@Override
	protected void readImpl()
	{
		_listId = readD();
		_entryId = readD();
		_amount = readD();
		_enchantment = _entryId % 100000;
		_entryId = _entryId / 100000;
		_transactionTax = 0;
	}

	@Override
	public void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();

		if(player == null)
		{
			return;
		}

		if(_amount < 1 || _amount > Config.MAX_MULTISELL)
		{
			return;
		}

		L2Npc merchant = player.getTarget() instanceof L2Npc ? (L2Npc) player.getTarget() : null;
		
        final boolean isCommunity = L2Multisell.getInstance().getList(_listId).getIsCommunity();
			
		if(!isCommunity && !player.isInsideRadius(merchant, L2Npc.INTERACTION_DISTANCE, false, false))
		{
			return;
		}

		MultiSellListContainer list = L2Multisell.getInstance().getList(_listId);

		if(list == null)
		{
			return;
		}

		if(player.isCastingNow())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		L2Object target = player.getTarget();
		if(!player.isGM()  && !isCommunity)
		{
			if((target == null || !(target instanceof L2Npc) || !list.checkNpcId(((L2Npc)target).getNpcId()) || !((L2Npc)target).canInteract(player)))
			{
				return;
			}
		}

		for(MultiSellEntry entry : list.getEntries())
		{
			if(entry.getEntryId() == _entryId)
			{
				doExchange(player, entry, list.getApplyTaxes(), list.getMaintainEnchantment(), _enchantment, isCommunity);
				return;
			}
		}
	}

	private void doExchange(L2PcInstance player, MultiSellEntry templateEntry, boolean applyTaxes, boolean maintainEnchantment, int enchantment, boolean isCommunity)
	{
		PcInventory inv = player.getInventory();

		boolean maintainItemFound = false;

		L2Npc merchant = player.getTarget() instanceof L2Npc ? (L2Npc) player.getTarget() : null;
		if (merchant == null && !isCommunity)
		{
			return;
		}
		MultiSellEntry entry = prepareEntry(merchant, templateEntry, applyTaxes, maintainEnchantment, enchantment, isCommunity);

		int slots = 0;
		int weight = 0;
		for(MultiSellIngredient e : entry.getProducts())
		{
			if(e.getItemId() < 0)
			{
				continue;
			}

			L2Item template = ItemTable.getInstance().getTemplate(e.getItemId());
			if(template == null)
			{
				continue;
			}

			if(!template.isStackable())
			{
				slots += e.getItemCount() * _amount;
			}
			else if(player.getInventory().getItemByItemId(e.getItemId()) == null)
			{
				slots++;
			}

			weight += e.getItemCount() * _amount * template.getWeight();
		}

		if(!inv.validateWeight(weight))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.WEIGHT_LIMIT_EXCEEDED));
			return;
		}

		if(!inv.validateCapacity(slots))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
			return;
		}

		FastList<MultiSellIngredient> _ingredientsList = new FastList<MultiSellIngredient>();
		boolean newIng = true;

		for(MultiSellIngredient e : entry.getIngredients())
		{
			newIng = true;

			for(MultiSellIngredient ex : _ingredientsList)
			{
				if(ex.getItemId() == e.getItemId() && ex.getEnchantmentLevel() == e.getEnchantmentLevel())
				{
					if((double) ex.getItemCount() + e.getItemCount() > Integer.MAX_VALUE)
					{
						player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
						_ingredientsList.clear();
						_ingredientsList = null;
						return;
					}

					if(ItemTable.getInstance().createDummyItem(e.getItemId()).isStackable())
					{
						_enchantment = 0;
					}

					ex.setItemCount(ex.getItemCount() + e.getItemCount());
					newIng = false;
				}
			}
			if(newIng)
			{
				// If there is a maintainIngredient, then we do not need to check the enchantment parameter
				// as the enchant level will be checked elsewhere
				if (maintainEnchantment && e.getMantainIngredient())
				{
					maintainItemFound = true;
				}
				if(maintainEnchantment || e.getMantainIngredient())
				{
					maintainItemFound = true;
				}

				_ingredientsList.add(new MultiSellIngredient(e));
			}
		}

		// If there is no maintainIngredient, then we must make sure that the 
		// enchantment is not kept from the client packet, as it may have been forged
		if(!maintainItemFound)
		{
			for (MultiSellIngredient product : entry.getProducts())
			{
				product.setEnchantmentLevel(0);
			}
		}

		for(MultiSellIngredient e : _ingredientsList)
		{
			if((double) e.getItemCount() * _amount > Integer.MAX_VALUE)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
				_ingredientsList.clear();
				_ingredientsList = null;
				return;
			}

			if(e.getItemId() != 65336 && e.getItemId() != 65436)
			{
				if(inv.getInventoryItemCount(e.getItemId(), maintainEnchantment ? e.getEnchantmentLevel() : -1) < (Config.ALT_BLACKSMITH_USE_RECIPES || !e.getMantainIngredient() ? e.getItemCount() * _amount : e.getItemCount()))
				{
					player.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					_ingredientsList.clear();
					_ingredientsList = null;
					return;
				}
			}
			else
			{
				if(e.getItemId() == 65336)
				{
					if(player.getClan() == null)
					{
						player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER));
						return;
					}

					if(!player.isClanLeader())
					{
						player.sendPacket(new SystemMessage(SystemMessageId.ONLY_THE_CLAN_LEADER_IS_ENABLED));
						return;
					}

					if(player.getClan().getReputationScore() < e.getItemCount() * _amount)
					{
						player.sendPacket(new SystemMessage(SystemMessageId.THE_CLAN_REPUTATION_SCORE_IS_TOO_LOW));
						return;
					}
				}
			}
		}

		_ingredientsList.clear();
		_ingredientsList = null;
		FastList<L2Augmentation> augmentation = new FastList<L2Augmentation>();

		for(MultiSellIngredient e : entry.getIngredients())
		{
			if(e.getItemId() != 65336 && e.getItemId() != 65436)
			{
				for(MultiSellIngredient a : entry.getProducts())
				{
					if(player.GetInventoryLimit() < inv.getSize() + _amount && !ItemTable.getInstance().createDummyItem(a.getItemId()).isStackable())
					{
						player.sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
						return;
					}

					if(player.GetInventoryLimit() < inv.getSize() && ItemTable.getInstance().createDummyItem(a.getItemId()).isStackable())
					{
						player.sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
						return;
					}
				}
				L2ItemInstance itemToTake = inv.getItemByItemId(e.getItemId());

				if(itemToTake == null)
				{
					_log.severe("Character: " + player.getName() + " is trying to cheat in multisell, merchant id:" + (merchant != null ? merchant.getNpcId():0));
					return;
				}

				if(itemToTake.isEquipped())
				{
					return;
				}

				if(itemToTake.fireEvent("MULTISELL", (Object[]) null) != null)
				{
					return;
				}

				if(itemToTake.isWear())
				{
					_log.severe("Character: " + player.getName() + " is trying to cheat in multisell with weared item");
					return;
				}

				if(Config.ALT_BLACKSMITH_USE_RECIPES || !e.getMantainIngredient())
				{
					if(itemToTake.isStackable())
					{
						if(!player.destroyItem("Multisell", itemToTake.getObjectId(), (e.getItemCount() * _amount), player.getTarget(), true))
						{
							return;
						}
					}
					else
					{
						if(maintainEnchantment)
						{
							L2ItemInstance[] inventoryContents = inv.getAllItemsByItemId(e.getItemId(), e.getEnchantmentLevel());
							for(int i = 0; i < e.getItemCount() * _amount; i++)
							{
								if(inventoryContents[i].isAugmented())
								{
									augmentation.add(inventoryContents[i].getAugmentation());
								}

								if(inventoryContents[i].isEquipped())
								{
									if(inventoryContents[i].isAugmented())
									{
										inventoryContents[i].getAugmentation().removeBoni(player);
									}
								}

								if(!player.destroyItem("Multisell", inventoryContents[i].getObjectId(), 1, player.getTarget(), true))
								{
									return;
								}
							}
						}
						else
						{
							for(int i = 1; i <= e.getItemCount() * _amount; i++)
							{
								L2ItemInstance[] inventoryContents = inv.getAllItemsByItemId(e.getItemId());

								itemToTake = inventoryContents[0];

								if(itemToTake.getEnchantLevel() > 0)
								{
									for(L2ItemInstance inventoryContent : inventoryContents)
									{
										if(inventoryContent.getEnchantLevel() < itemToTake.getEnchantLevel())
										{
											itemToTake = inventoryContent;

											if(itemToTake.getEnchantLevel() == 0)
											{
												break;
											}
										}
									}
								}

								if(itemToTake.isEquipped())
								{
									if(itemToTake.isAugmented())
									{
										itemToTake.getAugmentation().removeBoni(player);
									}
								}

								if(!player.destroyItem("Multisell", itemToTake.getObjectId(), 1, player.getTarget(), true))
								{
									return;
								}

							}
						}
					}
				}
			}
			else
			{
				if(e.getItemId() == 65336)
				{
					int repCost = player.getClan().getReputationScore() - e.getItemCount();
					player.getClan().setReputationScore(repCost, true);
					player.sendPacket(new SystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP).addNumber(e.getItemCount()));
					player.getClan().broadcastToOnlineMembers(new PledgeShowInfoUpdate(player.getClan()));
					player.sendPacket(new SystemMessage(SystemMessageId.SUCCESSFULLY_TRADED_WITH_NPC));
				}
			}
		}
		for(MultiSellIngredient e : entry.getProducts())
		{
			if(ItemTable.getInstance().createDummyItem(e.getItemId()).isStackable())
			{
				inv.addItem("Multisell["+_listId+"]" , e.getItemId(), (e.getItemCount() * _amount), player, player.getTarget());
			}
			else
			{
				L2ItemInstance product = null;
				for(int i = 0; i < e.getItemCount() * _amount; i++)
				{
					product = inv.addItem("Multisell["+_listId+"]", e.getItemId(), 1, player, player.getTarget());

					if(maintainEnchantment)
					{

						if(i < augmentation.size())
						{
							product.setAugmentation(new L2Augmentation(product, augmentation.get(i).getAugmentationId(), augmentation.get(i).getSkill(), true));
						}

						product.setEnchantLevel(e.getEnchantmentLevel());
					}
				}
			}

			if(e.getItemCount() * _amount > 1)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(e.getItemId()).addNumber(e.getItemCount() * _amount));
				player.sendPacket(new SystemMessage(SystemMessageId.SUCCESSFULLY_TRADED_WITH_NPC));
			}
			else
			{
				if(maintainEnchantment && e.getEnchantmentLevel() > 0)
				{
					player.sendPacket(new SystemMessage(SystemMessageId.ACQUIRED).addNumber(e.getEnchantmentLevel()).addItemName(e.getItemId()));
					player.sendPacket(new SystemMessage(SystemMessageId.SUCCESSFULLY_TRADED_WITH_NPC));
				}
				else
				{
					player.sendPacket(new SystemMessage(SystemMessageId.EARNED_ITEM_S1).addItemName(e.getItemId()));
					player.sendPacket(new SystemMessage(SystemMessageId.SUCCESSFULLY_TRADED_WITH_NPC));
				}
			}
		}
		player.sendPacket(new ItemList(player, false));

		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);

		player.broadcastUserInfo();

		if(merchant != null && merchant.getIsInTown() && merchant.getCastle().getOwnerId() > 0)
		{
			merchant.getCastle().addToTreasury(_transactionTax * _amount);
		}
	}

	private MultiSellEntry prepareEntry(L2Npc merchant, MultiSellEntry templateEntry, boolean applyTaxes, boolean maintainEnchantment, int enchantLevel, boolean isCommunity)
	{
		MultiSellEntry newEntry = new MultiSellEntry();
		newEntry.setEntryId(templateEntry.getEntryId());
		int totalAdenaCount = 0;
		boolean hasIngredient = false;

		for(MultiSellIngredient ing : templateEntry.getIngredients())
		{
			MultiSellIngredient newIngredient = new MultiSellIngredient(ing);

			if(newIngredient.getItemId() == 57 && newIngredient.isTaxIngredient())
			{
				double taxRate = 0.0;

				if(applyTaxes && !isCommunity)
				{
					if(merchant != null && merchant.getIsInTown())
					{
						taxRate = merchant.getCastle().getTaxRate();
					}
				}

				_transactionTax = (int) Math.round(newIngredient.getItemCount() * taxRate);
				totalAdenaCount += _transactionTax;
				continue;
			}
			else if(ing.getItemId() == 57)
			{
				totalAdenaCount += newIngredient.getItemCount();
				continue;
			}
			else if(maintainEnchantment)
			{
				L2Item tempItem = ItemTable.getInstance().createDummyItem(newIngredient.getItemId()).getItem();
				if(tempItem instanceof L2Armor || tempItem instanceof L2Weapon)
				{
					newIngredient.setEnchantmentLevel(enchantLevel);
					hasIngredient = true;
				}
			}
			newEntry.addIngredient(newIngredient);
		}

		if(totalAdenaCount > 0)
		{
			newEntry.addIngredient(new MultiSellIngredient(57, totalAdenaCount, false, false));
		}

		for(MultiSellIngredient ing : templateEntry.getProducts())
		{
			MultiSellIngredient newIngredient = new MultiSellIngredient(ing);

			if(maintainEnchantment && hasIngredient)
			{
				L2Item tempItem = ItemTable.getInstance().createDummyItem(newIngredient.getItemId()).getItem();

				if(tempItem instanceof L2Armor || tempItem instanceof L2Weapon)
				{
					if(enchantLevel==0 && maintainEnchantment)
					{
						enchantLevel = ing.getEnchantmentLevel();
					}
					newIngredient.setEnchantmentLevel(enchantLevel);
				}
			}
			newEntry.addProduct(newIngredient);
		}
		return newEntry;
	}

	@Override
	public String getType()
	{
		return _C__A7_MULTISELLCHOOSE;
	}

}