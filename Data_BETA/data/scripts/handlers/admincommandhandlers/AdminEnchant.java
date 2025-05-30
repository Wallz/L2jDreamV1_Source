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
package handlers.admincommandhandlers;

import com.src.Config;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.itemcontainer.Inventory;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.CharInfo;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.network.serverpackets.UserInfo;

public class AdminEnchant implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_seteh",
		"admin_setec",
		"admin_seteg",
		"admin_setel",
		"admin_seteb",
		"admin_setew",
		"admin_setes",
		"admin_setle",
		"admin_setre",
		"admin_setlf",
		"admin_setrf",
		"admin_seten",
		"admin_setun",
		"admin_setba",
		"admin_enchant"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		if(command.equals("admin_enchant"))
		{
			showMainPage(activeChar);
		}
		else
		{
			int armorType = -1;

			if(command.startsWith("admin_seteh"))
			{
				armorType = Inventory.PAPERDOLL_HEAD;
			}
			else if(command.startsWith("admin_setec"))
			{
				armorType = Inventory.PAPERDOLL_CHEST;
			}
			else if(command.startsWith("admin_seteg"))
			{
				armorType = Inventory.PAPERDOLL_GLOVES;
			}
			else if(command.startsWith("admin_seteb"))
			{
				armorType = Inventory.PAPERDOLL_FEET;
			}
			else if(command.startsWith("admin_setel"))
			{
				armorType = Inventory.PAPERDOLL_LEGS;
			}
			else if(command.startsWith("admin_setew"))
			{
				armorType = Inventory.PAPERDOLL_RHAND;
			}
			else if(command.startsWith("admin_setes"))
			{
				armorType = Inventory.PAPERDOLL_LHAND;
			}
			else if(command.startsWith("admin_setle"))
			{
				armorType = Inventory.PAPERDOLL_LEAR;
			}
			else if(command.startsWith("admin_setre"))
			{
				armorType = Inventory.PAPERDOLL_REAR;
			}
			else if(command.startsWith("admin_setlf"))
			{
				armorType = Inventory.PAPERDOLL_LFINGER;
			}
			else if(command.startsWith("admin_setrf"))
			{
				armorType = Inventory.PAPERDOLL_RFINGER;
			}
			else if(command.startsWith("admin_seten"))
			{
				armorType = Inventory.PAPERDOLL_NECK;
			}
			else if(command.startsWith("admin_setun"))
			{
				armorType = Inventory.PAPERDOLL_UNDER;
			}
			else if(command.startsWith("admin_setba"))
			{
				armorType = Inventory.PAPERDOLL_BACK;
			}

			if(armorType != -1)
			{
				try
				{
					int ench = Integer.parseInt(command.substring(12));

					if(ench < 0 || ench > 65535)
					{
						activeChar.sendChatMessage(0, 0, "SYS", "You must set the enchant level to be between 0-65535.");
					}
					else
					{
						setEnchant(activeChar, ench, armorType);
					}
				}
				catch(StringIndexOutOfBoundsException e)
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Please specify a new enchant value.");
				}
				catch(NumberFormatException e)
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Please specify a valid new enchant value.");
				}
			}
			showMainPage(activeChar);
		}
		return true;
	}

	private void setEnchant(L2PcInstance activeChar, int ench, int armorType)
	{
		L2Object target = activeChar.getTarget();
		if(target == null)
		{
			target = activeChar;
		}

		L2PcInstance player = null;

		if(target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			return;
		}

		target = null;

		int curEnchant = 0;

		L2ItemInstance itemInstance = null;

		L2ItemInstance parmorInstance = player.getInventory().getPaperdollItem(armorType);

		if(parmorInstance != null && parmorInstance.getEquipSlot() == armorType)
		{
			itemInstance = parmorInstance;
		}
		else
		{
			parmorInstance = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);

			if(parmorInstance != null && parmorInstance.getEquipSlot() == Inventory.PAPERDOLL_LRHAND)
			{
				itemInstance = parmorInstance;
			}
		}

		parmorInstance = null;

		if(itemInstance != null)
		{
			curEnchant = itemInstance.getEnchantLevel();

			if(Config.GM_OVER_ENCHANT != 0 && ench > Config.GM_OVER_ENCHANT && !player.isGM())
			{
				player.sendChatMessage(0, 0, "SYS", "A GM tried to over enchant you. Action blocked and Admin informed!");
				activeChar.sendChatMessage(0, 0, "SYS", "You tried to over enchant somebody. Action blocked and Admin informed!");
				return;
			}
			else
			{
				player.getInventory().unEquipItemInSlotAndRecord(armorType);
				curEnchant = itemInstance.getEnchantLevel();
				itemInstance.setEnchantLevel(ench);
				player.getInventory().equipItemAndRecord(itemInstance);

				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(itemInstance);
				player.sendPacket(iu);
				player.broadcastPacket(new CharInfo(player));
				player.sendPacket(new UserInfo(player));

				iu = null;

				activeChar.sendChatMessage(0, 0, "SYS", "Changed enchantment of " + player.getName() + "'s " + itemInstance.getItem().getName() + " from " + curEnchant + " to " + ench + ".");
				player.sendChatMessage(0, 0, "SYS", "Admin has changed the enchantment of your " + itemInstance.getItem().getName() + " from " + curEnchant + " to " + ench + ".");
			}
		}
		
		player = null;
		itemInstance = null;
	}

	private void showMainPage(L2PcInstance activeChar)
	{
		AdminHelpPage.showHelpPage(activeChar, "gm/enchant.htm");
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}