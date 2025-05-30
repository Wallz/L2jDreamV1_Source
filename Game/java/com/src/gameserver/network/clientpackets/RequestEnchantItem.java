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

import java.util.logging.Logger;

import com.src.Config;
import com.src.gameserver.Shutdown;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.olympiad.Olympiad;
import com.src.gameserver.model.itemcontainer.Inventory;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.EnchantResult;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.item.L2Item;
import com.src.gameserver.templates.item.L2WeaponType;
import com.src.gameserver.util.FloodProtector;
import com.src.gameserver.util.IllegalPlayerAction;
import com.src.gameserver.util.Util;
import com.src.util.StringUtil;
import com.src.util.random.Rnd;

public final class RequestEnchantItem extends L2GameClientPacket
{
	protected static final Logger _log = Logger.getLogger(Inventory.class.getName());

	private static final String _C__58_REQUESTENCHANTITEM = "[C] 58 RequestEnchantItem";

	private static final int[] CRYSTAL_SCROLLS =
	{
		731, 732, 949, 950, 953, 954, 957, 958, 961, 962
	};

	private static final int[] NORMAL_WEAPON_SCROLLS =
	{
		729, 947, 951, 955, 959
	};

	private static final int[] BLESSED_WEAPON_SCROLLS =
	{
		6569, 6571, 6573, 6575, 6577
	};

	private static final int[] CRYSTAL_WEAPON_SCROLLS =
	{
		731, 949, 953, 957, 961
	};

	private static final int[] NORMAL_ARMOR_SCROLLS =
	{
		730, 948, 952, 956, 960
	};

	private static final int[] BLESSED_ARMOR_SCROLLS =
	{
		6570, 6572, 6574, 6576, 6578
	};

	private static final int[] CRYSTAL_ARMOR_SCROLLS =
	{
		732, 950, 954, 958, 962
	};

	private int _objectId;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if(activeChar == null || _objectId == 0)
		{
			return;
		}

		if(Shutdown.getCounterInstance() != null)
		{
			showMessageErrorRestart(activeChar);
			return;
		}
		
		if(activeChar.getActiveTradeList() != null)
		{
			activeChar.cancelActiveTrade();
			activeChar.sendMessage("Your trade canceled.");
			return;
		}

		if (activeChar.isInOlympiadMode() || Olympiad.getInstance().isRegistered(activeChar) || activeChar.getOlympiadGameId() != -1)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION));
			return;
		}
		
		if(activeChar.isProcessingTransaction())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION));
			activeChar.setActiveEnchantItem(null);
			return;
		}

		if(activeChar.isOnline() == 0)
		{
			activeChar.setActiveEnchantItem(null);
			return;
		}

		L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);
		L2ItemInstance scroll = activeChar.getActiveEnchantItem();
		activeChar.setActiveEnchantItem(null);

		if(item == null || scroll == null)
		{
			activeChar.setActiveEnchantItem(null);
			return;
		}

		if(item.getItem().getItemType() == L2WeaponType.ROD || item.isShadowItem() || item.isCommonItem())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION));
			activeChar.setActiveEnchantItem(null);
			return;
		}

		if(!Config.ENCHANT_HERO_WEAPON && item.getItemId() >= 6611 && item.getItemId() <= 6621)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION));
			activeChar.setActiveEnchantItem(null);
			return;
		}

		if(!FloodProtector.getInstance().tryPerformAction(activeChar.getObjectId(), FloodProtector.PROTECTED_ENCHANT))
		{
			activeChar.setActiveEnchantItem(null);
			activeChar.sendMessage("Flood Protection: You can't enchant item so fast!");
			return;
		}

		if(item.isWear())
		{
			activeChar.setActiveEnchantItem(null);
			Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " tried to enchant a weared Item", IllegalPlayerAction.PUNISH_KICK);
			return;
		}

		int itemType2 = item.getItem().getType2();
		boolean enchantItem = false;
		boolean blessedScroll = false;
		boolean crystalScroll = false;
		int crystalId = 0;

		switch(item.getItem().getCrystalType())
		{
			case L2Item.CRYSTAL_A:
				crystalId = 1461;
				switch(scroll.getItemId())
				{
					case 729:
					case 731:
					case 6569:
						if(itemType2 == L2Item.TYPE2_WEAPON)
						{
							enchantItem = true;
						}
						break;
					case 730:
					case 732:
					case 6570:
						if(itemType2 == L2Item.TYPE2_SHIELD_ARMOR || itemType2 == L2Item.TYPE2_ACCESSORY)
						{
							enchantItem = true;
						}
						break;
				}
				break;
			case L2Item.CRYSTAL_B:
				crystalId = 1460;
				switch(scroll.getItemId())
				{
					case 947:
					case 949:
					case 6571:
						if(itemType2 == L2Item.TYPE2_WEAPON)
						{
							enchantItem = true;
						}
						break;
					case 948:
					case 950:
					case 6572:
						if(itemType2 == L2Item.TYPE2_SHIELD_ARMOR || itemType2 == L2Item.TYPE2_ACCESSORY)
						{
							enchantItem = true;
						}
						break;
				}
				break;
			case L2Item.CRYSTAL_C:
				crystalId = 1459;
				switch(scroll.getItemId())
				{
					case 951:
					case 953:
					case 6573:
						if(itemType2 == L2Item.TYPE2_WEAPON)
						{
							enchantItem = true;
						}
						break;
					case 952:
					case 954:
					case 6574:
						if(itemType2 == L2Item.TYPE2_SHIELD_ARMOR || itemType2 == L2Item.TYPE2_ACCESSORY)
						{
							enchantItem = true;
						}
						break;
				}
				break;
			case L2Item.CRYSTAL_D:
				crystalId = 1458;
				switch(scroll.getItemId())
				{
					case 955:
					case 957:
					case 6575:
						if(itemType2 == L2Item.TYPE2_WEAPON)
						{
							enchantItem = true;
						}
						break;
					case 956:
					case 958:
					case 6576:
						if(itemType2 == L2Item.TYPE2_SHIELD_ARMOR || itemType2 == L2Item.TYPE2_ACCESSORY)
						{
							enchantItem = true;
						}
						break;
				}
				break;
			case L2Item.CRYSTAL_S:
				crystalId = 1462;
				switch(scroll.getItemId())
				{
					case 959:
					case 961:
					case 6577:
						if(itemType2 == L2Item.TYPE2_WEAPON)
						{
							enchantItem = true;
						}
						break;
					case 960:
					case 962:
					case 6578:
						if(itemType2 == L2Item.TYPE2_SHIELD_ARMOR || itemType2 == L2Item.TYPE2_ACCESSORY)
						{
							enchantItem = true;
						}
						break;
				}
				break;
		}

		if(!enchantItem)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION));
			return;
		}

		if(scroll.getItemId() >= 6569 && scroll.getItemId() <= 6578)
		{
			blessedScroll = true;
		}
		else
		{
			for(int crystalscroll : CRYSTAL_SCROLLS)
			{
				if(scroll.getItemId() == crystalscroll)
				{
					crystalScroll = true;
					break;
				}
			}
		}

		int chance = 0;
		int maxEnchantLevel = 0;
		int minEnchantLevel = 0;

		SystemMessage sm;
		
		if(item.getItem().getType2() == L2Item.TYPE2_WEAPON)
		{
			for(int normalweaponscroll : NORMAL_WEAPON_SCROLLS)
			{
				if(scroll.getItemId() == normalweaponscroll)
				{
					if(item.getEnchantLevel() + 1 > Config.NORMAL_WEAPON_ENCHANT_LEVEL.size())
					{
						chance = Config.NORMAL_WEAPON_ENCHANT_LEVEL.get(Config.NORMAL_WEAPON_ENCHANT_LEVEL.size());
					}
					else
					{
						chance = Config.NORMAL_WEAPON_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
					}
					maxEnchantLevel = Config.ENCHANT_WEAPON_MAX;
				}
			}
			for(int blessedweaponscroll : BLESSED_WEAPON_SCROLLS)
			{
				if(scroll.getItemId() == blessedweaponscroll)
				{
					if(item.getEnchantLevel() + 1 > Config.BLESS_WEAPON_ENCHANT_LEVEL.size())
					{
						chance = Config.BLESS_WEAPON_ENCHANT_LEVEL.get(Config.BLESS_WEAPON_ENCHANT_LEVEL.size());
					}
					else
					{
						chance = Config.BLESS_WEAPON_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
					}
					maxEnchantLevel = Config.ENCHANT_WEAPON_MAX;
				}
			}
			for(int crystalweaponscroll : CRYSTAL_WEAPON_SCROLLS)
			{
				if(scroll.getItemId() == crystalweaponscroll)
				{
					if(item.getEnchantLevel() + 1 > Config.CRYTAL_WEAPON_ENCHANT_LEVEL.size())
					{
						chance = Config.CRYTAL_WEAPON_ENCHANT_LEVEL.get(Config.CRYTAL_WEAPON_ENCHANT_LEVEL.size());
					}
					else
					{
						chance = Config.CRYTAL_WEAPON_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
					}
					minEnchantLevel = Config.CRYSTAL_ENCHANT_MIN;
					maxEnchantLevel = Config.ENCHANT_WEAPON_MAX;
				}
			}
		}
		else if(item.getItem().getType2() == L2Item.TYPE2_SHIELD_ARMOR)
		{
			for(int normalarmorscroll : NORMAL_ARMOR_SCROLLS)
			{
				if(scroll.getItemId() == normalarmorscroll)
				{
					if(item.getEnchantLevel() + 1 > Config.NORMAL_ARMOR_ENCHANT_LEVEL.size())
					{
						chance = Config.NORMAL_ARMOR_ENCHANT_LEVEL.get(Config.NORMAL_ARMOR_ENCHANT_LEVEL.size());
					}
					else
					{
						chance = Config.NORMAL_ARMOR_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
					}
					maxEnchantLevel = Config.ENCHANT_ARMOR_MAX;
				}
			}
			for(int blessedarmorscroll : BLESSED_ARMOR_SCROLLS)
			{
				if(scroll.getItemId() == blessedarmorscroll)
				{
					if(item.getEnchantLevel() + 1 > Config.BLESS_ARMOR_ENCHANT_LEVEL.size())
					{
						chance = Config.BLESS_ARMOR_ENCHANT_LEVEL.get(Config.BLESS_ARMOR_ENCHANT_LEVEL.size());
					}
					else
					{
						chance = Config.BLESS_ARMOR_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
					}
					maxEnchantLevel = Config.ENCHANT_ARMOR_MAX;
				}
			}
			for(int crystalarmorscroll : CRYSTAL_ARMOR_SCROLLS)
			{
				if(scroll.getItemId() == crystalarmorscroll)
				{
					if(item.getEnchantLevel() + 1 > Config.CRYSTAL_ARMOR_ENCHANT_LEVEL.size())
					{
						chance = Config.CRYSTAL_ARMOR_ENCHANT_LEVEL.get(Config.CRYSTAL_ARMOR_ENCHANT_LEVEL.size());
					}
					else
					{
						chance = Config.CRYSTAL_ARMOR_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
					}
					minEnchantLevel = Config.CRYSTAL_ENCHANT_MIN;
					maxEnchantLevel = Config.ENCHANT_ARMOR_MAX;
				}
			}
		}
		else if(item.getItem().getType2() == L2Item.TYPE2_ACCESSORY)
		{
			for(int normaljewelscroll : NORMAL_ARMOR_SCROLLS)
			{
				if(scroll.getItemId() == normaljewelscroll)
				{
					if(item.getEnchantLevel() + 1 > Config.NORMAL_JEWELRY_ENCHANT_LEVEL.size())
					{
						chance = Config.NORMAL_JEWELRY_ENCHANT_LEVEL.get(Config.NORMAL_JEWELRY_ENCHANT_LEVEL.size());
					}
					else
					{
						chance = Config.NORMAL_JEWELRY_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
					}
					maxEnchantLevel = Config.ENCHANT_JEWELRY_MAX;
				}
			}
			for(int blessedjewelscroll : BLESSED_ARMOR_SCROLLS)
			{
				if(scroll.getItemId() == blessedjewelscroll)
				{
					if(item.getEnchantLevel() + 1 > Config.BLESS_JEWELRY_ENCHANT_LEVEL.size())
					{
						chance = Config.BLESS_JEWELRY_ENCHANT_LEVEL.get(Config.BLESS_JEWELRY_ENCHANT_LEVEL.size());
					}
					else
					{
						chance = Config.BLESS_JEWELRY_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
					}
					maxEnchantLevel = Config.ENCHANT_JEWELRY_MAX;
				}
			}
			for(int crystaljewelscroll : CRYSTAL_ARMOR_SCROLLS)
			{
				if(scroll.getItemId() == crystaljewelscroll)
				{
					if(item.getEnchantLevel() + 1 > Config.CRYSTAL_JEWELRY_ENCHANT_LEVEL.size())
					{
						chance = Config.CRYSTAL_JEWELRY_ENCHANT_LEVEL.get(Config.CRYSTAL_JEWELRY_ENCHANT_LEVEL.size());
					}
					else
					{
						chance = Config.CRYSTAL_JEWELRY_ENCHANT_LEVEL.get(item.getEnchantLevel() + 1);
					}
					minEnchantLevel = Config.CRYSTAL_ENCHANT_MIN;
					maxEnchantLevel = Config.ENCHANT_JEWELRY_MAX;
				}
			}
		}

		if(maxEnchantLevel != 0 && item.getEnchantLevel() >= maxEnchantLevel || item.getEnchantLevel() < minEnchantLevel)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION));
			return;
		}

		scroll = activeChar.getInventory().destroyItem("Enchant", scroll, activeChar, item);
		if(scroll == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " tried to enchant with a scroll he doesnt have", Config.DEFAULT_PUNISH);
			return;
		}

		if(item.getEnchantLevel() < Config.ENCHANT_SAFE_MAX || item.getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR && item.getEnchantLevel() < Config.ENCHANT_SAFE_MAX_FULL)
		{
			chance = 100;
		}

		int rndValue = Rnd.get(100);

		Object aChance = item.fireEvent("calcEnchantChance", new Object[chance]);
		if(aChance != null)
		{
			chance = (Integer) aChance;
		}
		synchronized(item)
		{
			if(rndValue < chance)
			{
				if(item.getOwnerId() != activeChar.getObjectId())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION));
					return;
				}

				if(item.getLocation() != L2ItemInstance.ItemLocation.INVENTORY && item.getLocation() != L2ItemInstance.ItemLocation.PAPERDOLL)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION));
					return;
				}

				if(item.getEnchantLevel() == 0)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_SUCCESSFULLY_ENCHANTED).addItemName(item.getItemId()));
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_S2_SUCCESSFULLY_ENCHANTED).addNumber(item.getEnchantLevel()).addItemName(item.getItemId()));
				}

				item.setEnchantLevel(item.getEnchantLevel() + Config.CUSTOM_ENCHANT_VALUE);
				item.updateDatabase();
			}
			else
			{
				if(crystalScroll)
				{
					sm = SystemMessage.sendString("Failed in Crystal Enchant. The enchant value of the item become " + Config.CRYSTAL_ENCHANT_MIN);
					activeChar.sendPacket(sm);
                }
				else if(blessedScroll)

				{
					if(item.getEnchantLevel() > 0)
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.ENCHANTMENT_FAILED_S1_S2_EVAPORATED).addNumber(item.getEnchantLevel()).addItemName(item.getItemId()));
					}
					else
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.ENCHANTMENT_FAILED_S1_EVAPORATED).addItemName(item.getItemId()));
					}
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.BLESSED_ENCHANT_FAILED));
				}

				if(!blessedScroll && !crystalScroll)
				{
					if(item.getEnchantLevel() > 0)
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED).addNumber(item.getEnchantLevel()).addItemName(item.getItemId()));
					}
					else
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_DISARMED).addItemName(item.getItemId()));
					}

					if(item.isEquipped())
					{
						if(item.isAugmented())
						{
							item.getAugmentation().removeBoni(activeChar);
						}

						L2ItemInstance[] unequiped = activeChar.getInventory().unEquipItemInSlotAndRecord(item.getEquipSlot());

						InventoryUpdate iu = new InventoryUpdate();
						for(L2ItemInstance element : unequiped)
						{
							iu.addModifiedItem(element);
						}
						activeChar.sendPacket(iu);

						activeChar.broadcastUserInfo();
					}

					int count = item.getCrystalCount() - (item.getItem().getCrystalCount() + 1) / 2;

					if(count < 1)
					{
						count = 1;
					}

					if(item.fireEvent("enchantFail", new Object[]
					{}) != null)
						return;
					L2ItemInstance destroyItem = activeChar.getInventory().destroyItem("Enchant", item, activeChar, null);
					if(destroyItem == null)
					{
						return;
					}

					L2ItemInstance crystals = activeChar.getInventory().addItem("Enchant", crystalId, count, activeChar, destroyItem);

					activeChar.sendPacket(new SystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(crystals.getItemId()).addNumber(count));

					if(!Config.FORCE_INVENTORY_UPDATE)
					{
						InventoryUpdate iu = new InventoryUpdate();
						if(destroyItem.getCount() == 0)
						{
							iu.addRemovedItem(destroyItem);
						}
						else
						{
							iu.addModifiedItem(destroyItem);
						}
						iu.addItem(crystals);

						activeChar.sendPacket(iu);
					}
					else
					{
						activeChar.sendPacket(new ItemList(activeChar, true));
					}

					StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
					su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
					activeChar.sendPacket(su);

					activeChar.broadcastUserInfo();

					L2World world = L2World.getInstance();
					world.removeObject(destroyItem);
				}
				else
				{
					if(blessedScroll)
					{
                        item.setEnchantLevel(Config.BREAK_ENCHANT);
                        item.updateDatabase();
                    }
					else if(crystalScroll)
					{
                        item.setEnchantLevel(Config.CRYSTAL_ENCHANT_MIN);
                        item.updateDatabase();

				}
			}
		}

		StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
		activeChar.sendPacket(su);

		activeChar.sendPacket(new EnchantResult(item.getEnchantLevel()));
		activeChar.sendPacket(new ItemList(activeChar, false));
		activeChar.broadcastUserInfo();
		}
	}

	private void showMessageErrorRestart(L2PcInstance activeChar)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		final StringBuilder strBuffer = StringUtil.startAppend(3500, "<html><title>Enchant ! </title><body><center>");
		{
			strBuffer.append("<img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32 align=left>");
			strBuffer.append("<font color=\"LEVEL\">%charname%</font> I am sorry but you can't <br>" + "enchant when restarting / shutdown of the server!:<br>");
			strBuffer.append("<table width=300>");
			strBuffer.append("</table><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32 align=left>");
		}
		strBuffer.append("</center></body></html>");
		html.setHtml(strBuffer.toString());
		html.replace("%charname%", activeChar.getName());
		activeChar.sendPacket(html);
	}
	
	@Override
	public String getType()
	{
		return _C__58_REQUESTENCHANTITEM;
	}
}