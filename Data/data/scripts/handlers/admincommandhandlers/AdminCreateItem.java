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

import java.util.Collection;
import java.util.StringTokenizer;

import com.src.gameserver.datatables.sql.ItemTable;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.templates.item.L2Item;

public class AdminCreateItem implements IAdminCommandHandler
{	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_itemcreate",
		"admin_create_item",
		"admin_create_coin",
		"admin_give_item_target",
		"admin_give_item_to_all",
		"admin_clean_inventory"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		GMAudit.auditGMAction(activeChar.getName(), command, (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target"), "");

		if(command.equals("admin_itemcreate"))
		{
			AdminHelpPage.showHelpPage(activeChar, "main/itemcreation.htm");
		}
		else if(command.startsWith("admin_create_item"))
		{
			try
			{
				String val = command.substring(17);
				StringTokenizer st = new StringTokenizer(val);

				if(st.countTokens() == 2)
				{
					String id = st.nextToken();
					int idval = Integer.parseInt(id);
					String num = st.nextToken();
					int numval = Integer.parseInt(num);
					createItem(activeChar, activeChar, idval, numval);
				}
				else if (st.countTokens() == 1)
				{
					String id = st.nextToken();
					int idval = Integer.parseInt(id);
					createItem(activeChar, activeChar, idval, 1);
				}
			}
			catch(StringIndexOutOfBoundsException e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //create_item <itemId> [amount]");
			}
			catch(NumberFormatException nfe)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Specify a valid number.");
			}

			AdminHelpPage.showHelpPage(activeChar, "main/itemcreation.htm");
		}
		else if(command.startsWith("clean_inventory"))
		{
			L2PcInstance target;
			if (activeChar.getTarget() instanceof L2PcInstance)
			{
				target = (L2PcInstance) activeChar.getTarget();
				cleanInventory(activeChar, target);
			}
			else
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Invalid target.");
				return false;
			}
			AdminHelpPage.showHelpPage(activeChar, "main/itemcreation.htm");
		}
		else if (command.startsWith("admin_create_coin"))
		{
			try
			{
				String val = command.substring(17);
				StringTokenizer st = new StringTokenizer(val);
				if (st.countTokens() == 2)
				{
					String name = st.nextToken();
					int idval = getCoinId(name);
					if(idval > 0)
					{
						String num = st.nextToken();
						int numval = Integer.parseInt(num);
						createItem(activeChar, activeChar, idval, numval);
					}
				}
				else if (st.countTokens() == 1)
				{
					String name = st.nextToken();
					int idval = getCoinId(name);
					createItem(activeChar, activeChar, idval, 1);
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //create_coin <name> [amount]");
			}
			catch (NumberFormatException nfe)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Specify a valid number.");
			}
			AdminHelpPage.showHelpPage(activeChar, "main/itemcreation.htm");
		}
		else if (command.startsWith("admin_give_item_target"))
		{
			try
			{
				L2PcInstance target;
				if (activeChar.getTarget() instanceof L2PcInstance)
					target = (L2PcInstance) activeChar.getTarget();
				else
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Invalid target.");
					return false;
				}
				
				String val = command.substring(22);
				StringTokenizer st = new StringTokenizer(val);
				if (st.countTokens() == 2)
				{
					String id = st.nextToken();
					int idval = Integer.parseInt(id);
					String num = st.nextToken();
					int numval = Integer.parseInt(num);
					createItem(activeChar, target, idval, numval);
				}
				else if (st.countTokens() == 1)
				{
					String id = st.nextToken();
					int idval = Integer.parseInt(id);
					createItem(activeChar, target, idval, 1);
				}
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //give_item_target <itemId> [amount]");
			}
			catch (NumberFormatException nfe)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Specify a valid number.");
			}
			AdminHelpPage.showHelpPage(activeChar, "main/itemcreation.htm");
		}
		else if (command.startsWith("admin_give_item_to_all"))
		{
			String val = command.substring(22);
			StringTokenizer st = new StringTokenizer(val);
			int idval = 0;
			int numval = 0;
			if (st.countTokens() == 2)
			{
				String id = st.nextToken();
				idval = Integer.parseInt(id);
				String num = st.nextToken();
				numval = Integer.parseInt(num);
			}
			else if (st.countTokens() == 1)
			{
				String id = st.nextToken();
				idval = Integer.parseInt(id);
				numval = 1;
			}
			int counter = 0;
			L2Item template = ItemTable.getInstance().getTemplate(idval);
			if (template == null)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "This item doesn't exist.");
				return false;
			}
			if (numval > 10 && !template.isStackable())
			{
				activeChar.sendChatMessage(0, 0, "SYS", "This item does not stack - Creation aborted.");
				return false;
			}
			Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers();
			{
				for (L2PcInstance onlinePlayer : pls)
				{
					if (activeChar != onlinePlayer && onlinePlayer.isOnline() == 1 && (onlinePlayer.getClient() != null))
					{
						onlinePlayer.getInventory().addItem("Admin", idval, numval, onlinePlayer, activeChar);
						onlinePlayer.sendChatMessage(0, 0, "SYS", "Admin spawned "+numval+" "+template.getName()+" in your inventory.");
						onlinePlayer.sendPacket(new ItemList(onlinePlayer, false));
						counter++;
					}
				}
			}
			activeChar.sendChatMessage(0, 0, "SYS", counter +" players rewarded with " + template.getName());
		}
		else if (command.startsWith("admin_clean_inventory"))
		{
			L2PcInstance target; 
		 	if (activeChar.getTarget() instanceof L2PcInstance) 
		 	{ 
		 		target = (L2PcInstance) activeChar.getTarget(); 
		 		cleanInventory(activeChar, target); 
		 	} 
		 	else 
		 	{ 
		 		activeChar.sendChatMessage(0, 0, "SYS", "Invalid target."); 
		 		return false; 
			} 
		 	AdminHelpPage.showHelpPage(activeChar, "main/itemcreation.htm"); 
		}
		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void createItem(L2PcInstance activeChar, L2PcInstance target, int id, int num)
	{
		L2Item template = ItemTable.getInstance().getTemplate(id);
		if (template == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "This item doesn't exist.");
			return;
		}
		if (num > 10 && !template.isStackable())
		{
			activeChar.sendChatMessage(0, 0, "SYS", "This item does not stack - Creation aborted.");
			return;
		}
		
		target.getInventory().addItem("Admin", id, num, activeChar, null);
		target.sendPacket(new ItemList(target, false));

		if (activeChar != target)
			target.sendChatMessage(0, 0, "SYS", "Admin spawned " + num + " " + template.getName() + " in your inventory!");
		activeChar.sendChatMessage(0, 0, "SYS", "You have spawned " + num + " " + template.getName() + "(" + id + ") in " + target.getName() + " inventory.");
	}
	
	private void cleanInventory(L2PcInstance activeChar, L2PcInstance target)
	{
		for (L2ItemInstance item : target.getInventory().getItems())
		{
			if (item.getLocation() == L2ItemInstance.ItemLocation.INVENTORY)
			{
				target.getInventory().destroyItem("Destroy", item.getObjectId(), item.getCount(), activeChar, null);
			}
		}
		
		target.sendPacket(new ItemList(target, false));
		
		if (activeChar != target)
			target.sendChatMessage(0, 0, "SYS", "Admin cleaned your Inventory!");
		activeChar.sendChatMessage(0, 0, "SYS", "You have cleaned " + target.getName() + " inventory!");
	}
	
	private int getCoinId(String name)
	{
		int id;
		if (name.equalsIgnoreCase("adena"))
			id = 57;
		else if (name.equalsIgnoreCase("ancientadena"))
			id = 5575;
		else if (name.equalsIgnoreCase("festivaladena"))
			id = 6673;
		else id = 0;
		
		return id;
	}
}