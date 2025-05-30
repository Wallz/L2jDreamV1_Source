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
package com.src.gameserver.network.clientpackets;

import java.util.Arrays;

import com.src.Config;
import com.src.gameserver.GameTimeController;
import com.src.gameserver.handler.IItemHandler;
import com.src.gameserver.handler.ItemHandler;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.managers.ClanHallManager;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.itemcontainer.Inventory;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.EtcStatusUpdate;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.network.serverpackets.ShowCalculator;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.network.serverpackets.UserInfo;
import com.src.gameserver.templates.item.L2Item;
import com.src.gameserver.templates.item.L2Weapon;
import com.src.gameserver.templates.item.L2WeaponType;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.gameserver.util.FloodProtector;
import com.src.gameserver.util.Util;

public final class UseItem extends L2GameClientPacket
{
	private static final String _C__14_USEITEM = "[C] 14 UseItem";

	private int _objectId;

	/** Weapon Equip Task */
	public class WeaponEquipTask implements Runnable
	{
		L2ItemInstance	item;
		L2PcInstance	activeChar;

		public WeaponEquipTask(L2ItemInstance it, L2PcInstance character)
		{
			item = it;
			activeChar = character;
		}

		@Override
		public void run()
		{
			activeChar.useEquippableItem(item, false);
		}
	}
	
	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		
		if (activeChar == null)
		{
			return; 
		} 
		
		if (!FloodProtector.getInstance().tryPerformAction(activeChar.getObjectId(), FloodProtector.PROTECTED_USE_ITEM)) 
		{ 
			return;
		}
		
		if (activeChar.isStunned() || activeChar.isConfused() || activeChar.isParalyzed() || activeChar.isSleeping())
		{
			activeChar.sendMessage("You can't use items right now.");
			return;
		}

		if (activeChar.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE));
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (activeChar.isAfraid())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (activeChar.getActiveTradeList() != null)
		{
			activeChar.cancelActiveTrade();
		}

		L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);

		if (item == null)
		{
			return;
		}

		if (item.isWear())
		{
			return;
		}
		
		if (activeChar.isFightingInEvent())
		{
			if (activeChar.getEventName().equals("CTF") && activeChar._CTFHaveFlagOfTeam > 0 && item.isEquipable() && item.isWeapon())
				return;
		}
		
		if (item.getItem().getType2() == L2Item.TYPE2_QUEST)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_USE_QUEST_ITEMS));
			return;
		}

		int itemId = item.getItemId();

		if (!Config.KARMA_PLAYER_CAN_TELEPORT && activeChar.getKarma() > 0 && (itemId == 736 || itemId == 1538 || itemId == 1829 || itemId == 1830 || itemId == 3958 || itemId == 5858 || itemId == 5859 || itemId == 6663 || itemId == 6664 || itemId >= 7117 && itemId <= 7135 || itemId >= 7554 && itemId <= 7559 || itemId == 7618 || itemId == 7619 || itemId == 10129 || itemId == 10130))
		{
			return;
		}

		if (itemId == 57)
		{
			return;
		}

		if ((itemId == 5858) && (ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()) == null))
        {
                activeChar.sendMessage("Blessed Scroll of Escape: Clan Hall cannot be used due to unsuitable terms.");
                return;
        }
        else if((itemId == 5859) && (CastleManager.getInstance().getCastleByOwner(activeChar.getClan()) == null))
        {
                activeChar.sendMessage("Blessed Scroll of Escape: Castle cannot be used due to unsuitable terms.");
                return;
        }

		if (activeChar.isFishing() && (itemId < 6535 || itemId > 6540))
		{
			getClient().getActiveChar().sendPacket(new SystemMessage(SystemMessageId.CANNOT_DO_WHILE_FISHING_3));
			return;
		}

		if (activeChar.getPkKills() > 0 && (itemId >= 7816 && itemId <= 7831))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION));
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_UNABLE_TO_EQUIP_THIS_ITEM_WHEN_YOUR_PK_COUNT_IS_GREATER_THAN_OR_EQUAL_TO_ONE));
			return;
		}

		L2Clan cl = activeChar.getClan();

		if ((cl == null || cl.getHasCastle() == 0) && itemId == 7015 && Config.CASTLE_SHIELD)
		{
			activeChar.sendMessage("You can't equip Castle Shield without clan.");
			return;
		}

		if ((cl == null || cl.getHasHideout() == 0) && itemId == 6902 && Config.CLANHALL_SHIELD)
		{
			activeChar.sendMessage("You can't equip Clan Hall Shield without clan.");
			return;
		}

		if (itemId >= 7860 && itemId <= 7879 && Config.APELLA_ARMORS && (cl == null || activeChar.getPledgeClass() < 5))
		{
			activeChar.sendMessage("You can't equip Appella Armors without Baron or a higher level Aristocrat.");
			return;
		}

		if (itemId >= 7850 && itemId <= 7859 && Config.OATH_ARMORS && cl == null)
		{
			activeChar.sendMessage("You can't equip Clan Oath without clan.");
			return;
		}
		
		if (itemId == 6841 && Config.CASTLE_CROWN && (cl == null || cl.getHasCastle() == 0 || !activeChar.isClanLeader()))
		{
			activeChar.sendMessage("You can't equip Lord's Crown.");
			return;
		}

		if (Config.CASTLE_CIRCLETS && (itemId >= 6834 && itemId <= 6840 || itemId == 8182 || itemId == 8183))
		{
			if (cl == null)
			{
				activeChar.sendMessage("You can't equip that.");
				return;
			}
			else
			{
				int circletId = CastleManager.getInstance().getCircletByCastleId(cl.getHasCastle());
				if (activeChar.getPledgeType() == -1 || circletId != itemId)
				{
					activeChar.sendMessage("You can't equip that.");
					return;
				}
			}
		}

		if (Config.PROTECTED_WEAPONS)
		{
			L2Weapon curwep = activeChar.getActiveWeaponItem();

			if(curwep != null)
			{
				if((curwep.getItemType() == L2WeaponType.DUAL && item.getItemType() == L2WeaponType.NONE) || (curwep.getItemType() == L2WeaponType.BOW && item.getItemType() == L2WeaponType.NONE) || (curwep.getItemType() == L2WeaponType.BIGBLUNT && item.getItemType() == L2WeaponType.NONE) || (curwep.getItemType() == L2WeaponType.BIGBLUNT && item.getItemType() == L2WeaponType.NONE) || (curwep.getItemType() == L2WeaponType.POLE && item.getItemType() == L2WeaponType.NONE) || (curwep.getItemType() == L2WeaponType.DUALFIST && item.getItemType() == L2WeaponType.NONE) || (curwep.getItemType() == L2WeaponType.DAGGER && item.getItemType() == L2WeaponType.DUALFIST) || (curwep.getItemType() == L2WeaponType.DAGGER && item.getItemType() == L2WeaponType.POLE) || (curwep.getItemType() == L2WeaponType.DAGGER && item.getItemType() == L2WeaponType.BIGBLUNT) || (curwep.getItemType() == L2WeaponType.DAGGER && item.getItemType() == L2WeaponType.BIGSWORD) || (curwep.getItemType() == L2WeaponType.DAGGER && item.getItemType() == L2WeaponType.BOW) || (curwep.getItemType() == L2WeaponType.DAGGER && item.getItemType() == L2WeaponType.DUAL) || (curwep.getItemType() == L2WeaponType.SWORD && item.getItemType() == L2WeaponType.DUALFIST) || (curwep.getItemType() == L2WeaponType.SWORD && item.getItemType() == L2WeaponType.POLE) || (curwep.getItemType() == L2WeaponType.SWORD && item.getItemType() == L2WeaponType.BIGBLUNT) || (curwep.getItemType() == L2WeaponType.SWORD && item.getItemType() == L2WeaponType.BIGSWORD) || (curwep.getItemType() == L2WeaponType.SWORD && item.getItemType() == L2WeaponType.BOW) || (curwep.getItemType() == L2WeaponType.SWORD && item.getItemType() == L2WeaponType.DUAL) || (curwep.getItemType() == L2WeaponType.BLUNT && item.getItemType() == L2WeaponType.DUALFIST) || (curwep.getItemType() == L2WeaponType.BLUNT && item.getItemType() == L2WeaponType.POLE) || (curwep.getItemType() == L2WeaponType.BLUNT && item.getItemType() == L2WeaponType.BIGBLUNT) || (curwep.getItemType() == L2WeaponType.BLUNT && item.getItemType() == L2WeaponType.BIGSWORD) || (curwep.getItemType() == L2WeaponType.BLUNT && item.getItemType() == L2WeaponType.BOW) || (curwep.getItemType() == L2WeaponType.BLUNT && item.getItemType() == L2WeaponType.DUAL))
				{
					activeChar.sendMessage("You are not allowed to do this.");
					return;
				}
			}
		}

		if (activeChar.isDead())
		{
			getClient().getActiveChar().sendPacket(new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addItemName(itemId));
			return;
		}

		if (item.getItem().isForWolf() || item.getItem().isForHatchling() || item.getItem().isForStrider() || item.getItem().isForBabyPet())
		{
			getClient().getActiveChar().sendPacket(new SystemMessage(SystemMessageId.CANNOT_EQUIP_PET_ITEM).addItemName(itemId));
			return;
		}

		if (item.isEquipable())
		{
			if (activeChar.isFishing() || activeChar.isStunned() || activeChar.isSleeping() || activeChar.isParalyzed() || activeChar.isAlikeDead())
			{
				activeChar.sendMessage("Your status don't allow you to do that.");
				return;
			}

			if (!activeChar.getInventory().canManipulateWithItemId(item.getItemId()))
				return;
			
			int bodyPart = item.getItem().getBodyPart();

			if ((activeChar.isCastingNow() || activeChar.isMounted()) && ((bodyPart == L2Item.SLOT_LR_HAND) || (bodyPart == L2Item.SLOT_L_HAND) || (bodyPart == L2Item.SLOT_R_HAND)))
			{
				return;
			}

			if (item.isEquipable() && item.getItemType() == L2WeaponType.BOW && activeChar.getClassId().getId() == 90) 
			{ 
				int skill_id = 406; 
				activeChar.stopSkillEffects(skill_id); 
				activeChar.updateAbnormalEffect(); 
				activeChar.updateEffectIcons(); 
			} 
		 	
			if (item.isEquipable() && item.getItemType() == L2WeaponType.BOW && activeChar.getClassId().getId() == 93) 
			{ 
				int skill_id = 445; 
				activeChar.stopSkillEffects(skill_id); 
				activeChar.updateAbnormalEffect(); 
				activeChar.updateEffectIcons(); 
			}
			
			//SECURE FIX - Anti Overenchant Cheat!!
			if (Config.MAX_ITEM_ENCHANT_KICK >0 && !activeChar.isGM() && item.getEnchantLevel() > Config.MAX_ITEM_ENCHANT_KICK)
			{
				activeChar.sendMessage("You have been kicked for using an item overenchanted!");
				Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " has item Overenchanted! Kicked ", Config.DEFAULT_PUNISH);
				return;
	        } 
			
			/*if (activeChar.isWearingFormalWear() && (bodyPart == L2Item.SLOT_LR_HAND || bodyPart == L2Item.SLOT_L_HAND || bodyPart == L2Item.SLOT_R_HAND))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_USE_ITEMS_SKILLS_WITH_FORMALWEAR));
				return;
			}*/

			if (Config.PROTECTED_ENCHANT)
			{
				switch(bodyPart)
				{
					case L2Item.SLOT_LR_HAND:
					case L2Item.SLOT_L_HAND:
					case L2Item.SLOT_R_HAND:
					{
						if ((item.getEnchantLevel() > Config.NORMAL_WEAPON_ENCHANT_LEVEL.size() || item.getEnchantLevel() > Config.BLESS_WEAPON_ENCHANT_LEVEL.size() || item.getEnchantLevel() > Config.CRYTAL_WEAPON_ENCHANT_LEVEL.size()) && !activeChar.isGM())
						{
							activeChar.sendMessage("You have been kicked for using an item wich is over enchanted!");
							Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " has item Overenchanted! ", Config.DEFAULT_PUNISH);
							return;
						}
						break;
					}
					case L2Item.SLOT_CHEST:
					case L2Item.SLOT_BACK:
					case L2Item.SLOT_GLOVES:
					case L2Item.SLOT_FEET:
					case L2Item.SLOT_HEAD:
					case L2Item.SLOT_FULL_ARMOR:
					case L2Item.SLOT_LEGS:
					{
						if ((item.getEnchantLevel() > Config.NORMAL_ARMOR_ENCHANT_LEVEL.size() || item.getEnchantLevel() > Config.BLESS_ARMOR_ENCHANT_LEVEL.size() || item.getEnchantLevel() > Config.CRYSTAL_ARMOR_ENCHANT_LEVEL.size()) && !activeChar.isGM())
						{
							activeChar.sendMessage("You have been kicked for using an item wich is over enchanted!");
							Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " has item Overenchanted! ", Config.DEFAULT_PUNISH);
							return;
						}
						break;
					}
					case L2Item.SLOT_R_EAR:
					case L2Item.SLOT_L_EAR:
					case L2Item.SLOT_NECK:
					case L2Item.SLOT_R_FINGER:
					case L2Item.SLOT_L_FINGER:
					{
						if ((item.getEnchantLevel() > Config.NORMAL_JEWELRY_ENCHANT_LEVEL.size() || item.getEnchantLevel() > Config.BLESS_JEWELRY_ENCHANT_LEVEL.size() || item.getEnchantLevel() > Config.CRYSTAL_JEWELRY_ENCHANT_LEVEL.size()) && !activeChar.isGM())
						{
							activeChar.sendMessage("You have been kicked for using an item wich is over enchanted!");
							Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " has item Overenchanted! ", Config.DEFAULT_PUNISH);
							return;
						}
						break;
					}
				}
			}

			if (activeChar.isCursedWeaponEquiped() && (bodyPart == L2Item.SLOT_LR_HAND || bodyPart == L2Item.SLOT_L_HAND || bodyPart == L2Item.SLOT_R_HAND || itemId == 6408))
			{
				return;
			}

			if (activeChar.isInOlympiadMode() && ((bodyPart == L2Item.SLOT_LR_HAND || bodyPart == L2Item.SLOT_L_HAND || bodyPart == L2Item.SLOT_R_HAND) && (item.getItemId() >= 6611 && item.getItemId() <= 6621 || item.getItemId() == 6842) || Config.LIST_OLY_RESTRICTED_ITEMS.contains(item.getItemId())))
			{
				return;
			}

			if (!activeChar.isHero() && (item.getItemId() >= 6611 && item.getItemId() <= 6621 || item.getItemId() == 6842) && !activeChar.isGM())
			{
				return;
			}

			if (activeChar.isAttackingNow())
			{
				ThreadPoolManager.getInstance().scheduleGeneral(new WeaponEquipTask(item, activeChar), (activeChar.getAttackEndTime() - GameTimeController.getGameTicks()) * GameTimeController.MILLIS_IN_TICK);
				return;
			}

			if (activeChar.isCursedWeaponEquipped() && itemId == 6408)
			{
				return;
			}

			L2ItemInstance[] items = null;
			boolean isEquiped = item.isEquipped();

			L2ItemInstance old = activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
			if (old == null)
			{
				old = activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			}

			activeChar.checkSSMatch(item, old);

			if (old != null && old.isAugmented())
			{
				old.getAugmentation().removeBoni(activeChar);
			}

			if (isEquiped)
			{
				if( item.getEnchantLevel() > 0)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED).addNumber(item.getEnchantLevel()).addItemName(itemId));
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_DISARMED).addItemName(itemId));
				}

				if (item.isAugmented())
				{
					item.getAugmentation().removeBoni(activeChar);
				}

				switch(item.getEquipSlot())
				{
					case 1:
						bodyPart = L2Item.SLOT_L_EAR;
						break;
					case 2:
						bodyPart = L2Item.SLOT_R_EAR;
						break;
					case 4:
						bodyPart = L2Item.SLOT_L_FINGER;
						break;
					case 5:
						bodyPart = L2Item.SLOT_R_FINGER;
						break;
					default:
						break;
				}

				items = activeChar.getInventory().unEquipItemInBodySlotAndRecord(bodyPart);
			}
			else
			{
				int tempBodyPart = item.getItem().getBodyPart();
				L2ItemInstance tempItem = activeChar.getInventory().getPaperdollItemByL2ItemId(tempBodyPart);

				if (tempItem != null && tempItem.isAugmented())
				{
					tempItem.getAugmentation().removeBoni(activeChar);
				}

				if (tempItem != null && tempItem.isWear())
				{
					return;
				}
				else if (tempBodyPart == 0x4000)
				{
					tempItem = activeChar.getInventory().getPaperdollItem(7);
					if (tempItem != null && tempItem.isWear())
					{
						return;
					}

					tempItem = activeChar.getInventory().getPaperdollItem(8);
					if (tempItem != null && tempItem.isWear())
					{
						return;
					}
				}
				else if (tempBodyPart == 0x8000)
				{
					tempItem = activeChar.getInventory().getPaperdollItem(10);
					if (tempItem != null && tempItem.isWear())
					{
						return;
					}

					tempItem = activeChar.getInventory().getPaperdollItem(11);
					if (tempItem != null && tempItem.isWear())
					{
						return;
					}
				}

				if (item.getEnchantLevel() > 0)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_S2_EQUIPPED).addNumber(item.getEnchantLevel()).addItemName(itemId));
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_EQUIPPED).addItemName(itemId));
				}

				if (item.isAugmented())
				{
					item.getAugmentation().applyBoni(activeChar);
				}

				items = activeChar.getInventory().equipItemAndRecord(item);

				item.decreaseMana(false);
				
				if ((item.getItem().getBodyPart() & L2Item.SLOT_ALLWEAPON) != 0)
				{
					activeChar.rechargeAutoSoulShot(true, true, false); 
					item.setChargedSoulshot(L2ItemInstance.CHARGED_NONE); 
					item.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
				}
			}

			if (item.getItem().getType2() == L2Item.TYPE2_WEAPON)
			{
				activeChar.checkIfWeaponIsAllowed();
			}

			activeChar.abortAttack();

			activeChar.sendPacket(new EtcStatusUpdate(activeChar));

			if (!((item.getItem().getBodyPart() & L2Item.SLOT_HEAD) > 0 || (item.getItem().getBodyPart() & L2Item.SLOT_NECK) > 0 || (item.getItem().getBodyPart() & L2Item.SLOT_L_EAR) > 0 || (item.getItem().getBodyPart() & L2Item.SLOT_R_EAR) > 0 || (item.getItem().getBodyPart() & L2Item.SLOT_L_FINGER) > 0 || (item.getItem().getBodyPart() & L2Item.SLOT_R_FINGER) > 0))
			{
				activeChar.broadcastUserInfo();
				InventoryUpdate iu = new InventoryUpdate();
				iu.addItems(Arrays.asList(items));
				activeChar.sendPacket(iu);
			}
			else if ((item.getItem().getBodyPart() & L2Item.SLOT_HEAD) > 0)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addItems(Arrays.asList(items));
				activeChar.sendPacket(iu);
				activeChar.sendPacket(new UserInfo(activeChar));
			}
			else
			{
				activeChar.sendPacket(new ItemList(activeChar, true));
				activeChar.sendPacket(new UserInfo(activeChar));
			}
		}
		else
		{
			L2Weapon weaponItem = activeChar.getActiveWeaponItem();
			int itemid = item.getItemId();

			if (itemid == 4393)
			{
				activeChar.sendPacket(new ShowCalculator(4393));
			}
			else if (weaponItem != null && weaponItem.getItemType() == L2WeaponType.ROD && (itemid >= 6519 && itemid <= 6527 || itemid >= 7610 && itemid <= 7613 || itemid >= 7807 && itemid <= 7809 || itemid >= 8484 && itemid <= 8486 || itemid >= 8505 && itemid <= 8513))
			{
				activeChar.getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, item);
				activeChar.broadcastUserInfo();

				ItemList il = new ItemList(activeChar, false);
				sendPacket(il);
				return;
			}
			else
			{
				IItemHandler handler = ItemHandler.getInstance().getItemHandler(itemId);
				if (handler == null)
				{
				}
				else
				{
					handler.useItem(activeChar, item);
				}
			}
		}
	}

	@Override
	public String getType()
	{
		return _C__14_USEITEM;
	}

}