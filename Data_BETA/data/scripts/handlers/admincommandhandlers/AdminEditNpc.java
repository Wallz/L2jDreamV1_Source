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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;

import com.src.gameserver.TradeController;
import com.src.gameserver.cache.HtmCache;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.sql.ItemTable;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2DropCategory;
import com.src.gameserver.model.L2DropData;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.L2TradeList;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.templates.StatsSet;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.templates.item.L2Item;
import com.src.gameserver.templates.skills.L2SkillType;
import com.src.util.database.L2DatabaseFactory;

public class AdminEditNpc implements IAdminCommandHandler
{
	private static Logger _log = Logger.getLogger(AdminEditChar.class.getName());
	private final static int PAGE_LIMIT = 20;

	private static final String[] ADMIN_COMMANDS = 
	{
		"admin_edit_npc",
		"admin_save_npc",
		"admin_show_droplist",
		"admin_edit_drop",
		"admin_add_drop",
		"admin_del_drop",
		"admin_showShop",
		"admin_showShopList",
		"admin_addShopItem",
		"admin_delShopItem",
		"admin_editShopItem",
		"admin_close_window",
		"admin_show_skilllist_npc",
		"admin_add_skill_npc",
		"admin_edit_skill_npc",
		"admin_del_skill_npc"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		GMAudit.auditGMAction(activeChar.getName(), command, (activeChar.getTarget() != null ? activeChar.getTarget().getName() : "no-target"), "");

		if(command.startsWith("admin_showShop "))
		{
			String[] args = command.split(" ");
			if(args.length > 1)
				showShop(activeChar, Integer.parseInt(command.split(" ")[1]));
		}
		else if(command.startsWith("admin_showShopList "))
		{
			String[] args = command.split(" ");
			if(args.length > 2)
				showShopList(activeChar, Integer.parseInt(command.split(" ")[1]), Integer.parseInt(command.split(" ")[2]));
		}
		else if(command.startsWith("admin_edit_npc "))
		{
			try
			{
				String[] commandSplit = command.split(" ");
				int npcId = Integer.parseInt(commandSplit[1]);
				L2NpcTemplate npc = NpcTable.getInstance().getTemplate(npcId);
				showNpcProperty(activeChar, npc);
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Wrong usage: //edit_npc <npcId>");
			}
		}
		else if(command.startsWith("admin_show_droplist "))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			try
			{
				int npcId = Integer.parseInt(st.nextToken());
				int page = 1;
				if(st.hasMoreTokens())
					page = Integer.parseInt(st.nextToken());
				showNpcDropList(activeChar, npcId, page);
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //show_droplist <npc_id> [<page>]");
			}
		}
		else if(command.startsWith("admin_addShopItem "))
		{
			String[] args = command.split(" ");
			if(args.length > 1)
				addShopItem(activeChar, args);
		}
		else if(command.startsWith("admin_delShopItem "))
		{
			String[] args = command.split(" ");
			if(args.length > 2)
				delShopItem(activeChar, args);
		}
		else if(command.startsWith("admin_editShopItem "))
		{
			String[] args = command.split(" ");
			if(args.length > 2)
				editShopItem(activeChar, args);
		}
		else if(command.startsWith("admin_save_npc "))
		{
			try
			{
				saveNpcProperty(activeChar, command);
			}
			catch(StringIndexOutOfBoundsException e)
			{}
		}
		else if(command.startsWith("admin_edit_drop "))
		{
			int npcId = -1, itemId = 0, category = -1000;
			try
			{
				StringTokenizer st = new StringTokenizer(command.substring(16).trim());
				if(st.countTokens() == 3)
				{
					try
					{
						npcId = Integer.parseInt(st.nextToken());
						itemId = Integer.parseInt(st.nextToken());
						category = Integer.parseInt(st.nextToken());
						showEditDropData(activeChar, npcId, itemId, category);
					}
					catch(Exception e)
					{}
				}
				else if(st.countTokens() == 6)
				{
					try
					{
						npcId = Integer.parseInt(st.nextToken());
						itemId = Integer.parseInt(st.nextToken());
						category = Integer.parseInt(st.nextToken());
						int min = Integer.parseInt(st.nextToken());
						int max = Integer.parseInt(st.nextToken());
						int chance = Integer.parseInt(st.nextToken());

						updateDropData(activeChar, npcId, itemId, min, max, category, chance);
					}
					catch(Exception e)
					{
						_log.fine("admin_edit_drop parements error: " + command);
					}
				}
				else
					activeChar.sendChatMessage(0, 0, "SYS", "Usage: //edit_drop <npc_id> <item_id> <category> [<min> <max> <chance>]");
			}
			catch(StringIndexOutOfBoundsException e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //edit_drop <npc_id> <item_id> <category> [<min> <max> <chance>]");
			}
		}
		else if(command.startsWith("admin_add_drop "))
		{
			int npcId = -1;
			try
			{
				StringTokenizer st = new StringTokenizer(command.substring(15).trim());
				if(st.countTokens() == 1)
				{
					try
					{
						String[] input = command.substring(15).split(" ");
						if(input.length < 1)
							return true;
						npcId = Integer.parseInt(input[0]);
					}
					catch(Exception e) {}

					if(npcId > 0)
					{
						L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
						showAddDropData(activeChar, npcData);
					}
				}
				else if(st.countTokens() == 6)
				{
					try
					{
						npcId = Integer.parseInt(st.nextToken());
						int itemId = Integer.parseInt(st.nextToken());
						int category = Integer.parseInt(st.nextToken());
						int min = Integer.parseInt(st.nextToken());
						int max = Integer.parseInt(st.nextToken());
						int chance = Integer.parseInt(st.nextToken());

						addDropData(activeChar, npcId, itemId, min, max, category, chance);
					}
					catch(Exception e)
					{
						_log.fine("admin_add_drop parements error: " + command);
					}
				}
				else
					activeChar.sendChatMessage(0, 0, "SYS", "Usage: //add_drop <npc_id> [<item_id> <category> <min> <max> <chance>]");
			}
			catch(StringIndexOutOfBoundsException e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //add_drop <npc_id> [<item_id> <category> <min> <max> <chance>]");
			}
		}
		else if(command.startsWith("admin_del_drop "))
		{
			int npcId = -1, itemId = -1, category = -1000;
			try
			{
				String[] input = command.substring(15).split(" ");
				if(input.length >= 3)
				{
					npcId = Integer.parseInt(input[0]);
					itemId = Integer.parseInt(input[1]);
					category = Integer.parseInt(input[2]);
				}
			}
			catch(Exception e){}

			if(npcId > 0)
				deleteDropData(activeChar, npcId, itemId, category);
			else
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //del_drop <npc_id> <item_id> <category>");
		}
		else if(command.startsWith("admin_show_skilllist_npc "))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			try
			{
				int npcId = Integer.parseInt(st.nextToken());
				int page = 0;
				if(st.hasMoreTokens())
					page = Integer.parseInt(st.nextToken());
				showNpcSkillList(activeChar, npcId, page);
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //show_skilllist_npc <npc_id> <page>");
			}
		}
		else if(command.startsWith("admin_edit_skill_npc "))
		{
			try
			{
				StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				int npcId = Integer.parseInt(st.nextToken());
				int skillId = Integer.parseInt(st.nextToken());
				if(!st.hasMoreTokens())
					showNpcSkillEdit(activeChar, npcId, skillId);
				else
				{
					int level = Integer.parseInt(st.nextToken());
					updateNpcSkillData(activeChar, npcId, skillId, level);
				}
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //edit_skill_npc <npc_id> <item_id> [<level>]");
			}
		}
		else if(command.startsWith("admin_add_skill_npc "))
		{
			try
			{
				StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				int npcId = Integer.parseInt(st.nextToken());
				if(!st.hasMoreTokens())
				{
					showNpcSkillAdd(activeChar, npcId);
				}
				else
				{
					int skillId = Integer.parseInt(st.nextToken());
					int level = Integer.parseInt(st.nextToken());
					addNpcSkillData(activeChar, npcId, skillId, level);
				}
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //add_skill_npc <npc_id> [<skill_id> <level>]");
			}
		}
		else if(command.startsWith("admin_del_skill_npc "))
		{
			try
			{
				StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				int npcId = Integer.parseInt(st.nextToken());
				int skillId = Integer.parseInt(st.nextToken());
				deleteNpcSkillData(activeChar, npcId, skillId);
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //del_skill_npc <npc_id> <skill_id>");
			}
		}

		return true;
	}

	private void editShopItem(L2PcInstance activeChar, String[] args)
	{
		int tradeListID = Integer.parseInt(args[1]);
		int itemID = Integer.parseInt(args[2]);
		L2TradeList tradeList = TradeController.getInstance().getBuyList(tradeListID);

		L2Item item = ItemTable.getInstance().getTemplate(itemID);
		if(tradeList.getPriceForItemId(itemID) < 0)
		{
			return;
		}

		if(args.length > 3)
		{
			int price = Integer.parseInt(args[3]);
			int order =  findOrderTradeList(itemID, tradeList.getPriceForItemId(itemID), tradeListID);

			tradeList.replaceItem(itemID, Integer.parseInt(args[3]));
			updateTradeList(itemID, price, tradeListID, order);

			activeChar.sendChatMessage(0, 0, "SYS", "Updated price for "+item.getName()+" in Trade List "+tradeListID);
			showShopList(activeChar, tradeListID, 1);
			return;
		}

		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

		TextBuilder replyMSG = new TextBuilder();
		replyMSG.append("<html><title>Merchant Shop Item Edit</title>");
		replyMSG.append("<body>");
		replyMSG.append("<br>Edit an entry in merchantList.");
		replyMSG.append("<br>Editing Item: "+item.getName());
		replyMSG.append("<table>");
		replyMSG.append("<tr><td width=100>Property</td><td width=100>Edit Field</td><td width=100>Old Value</td></tr>");
		replyMSG.append("<tr><td><br></td><td></td></tr>");
		replyMSG.append("<tr><td>Price</td><td><edit var=\"price\" width=80></td><td>"+tradeList.getPriceForItemId(itemID)+"</td></tr>");
		replyMSG.append("</table>");
		replyMSG.append("<center><br><br><br>");
		replyMSG.append("<button value=\"Save\" action=\"bypass -h admin_editShopItem " + tradeListID + " " + itemID + " $price\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("<br><button value=\"Back\" action=\"bypass -h admin_showShopList " + tradeListID +" 1\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("</center>");
		replyMSG.append("</body></html>");

		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private void delShopItem(L2PcInstance activeChar, String[] args)
	{
		int tradeListID = Integer.parseInt(args[1]);
		int itemID = Integer.parseInt(args[2]);
		L2TradeList tradeList = TradeController.getInstance().getBuyList(tradeListID);

		if(tradeList.getPriceForItemId(itemID) < 0)
			return;

		if(args.length > 3)
		{
			int order =  findOrderTradeList(itemID, tradeList.getPriceForItemId(itemID), tradeListID);

			tradeList.removeItem(itemID);
			deleteTradeList(tradeListID, order);

			activeChar.sendChatMessage(0, 0, "SYS", "Deleted "+ItemTable.getInstance().getTemplate(itemID).getName()+" from Trade List "+tradeListID);
			showShopList(activeChar, tradeListID, 1);
			return;
		}

		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

		TextBuilder replyMSG = new TextBuilder();
		replyMSG.append("<html><title>Merchant Shop Item Delete</title>");
		replyMSG.append("<body>");
		replyMSG.append("<br>Delete entry in merchantList.");
		replyMSG.append("<br>Item to Delete: "+ItemTable.getInstance().getTemplate(itemID).getName());
		replyMSG.append("<table>");
		replyMSG.append("<tr><td width=100>Property</td><td width=100>Value</td></tr>");
		replyMSG.append("<tr><td><br></td><td></td></tr>");
		replyMSG.append("<tr><td>Price</td><td>"+tradeList.getPriceForItemId(itemID)+"</td></tr>");
		replyMSG.append("</table>");
		replyMSG.append("<center><br><br><br>");
		replyMSG.append("<button value=\"Confirm\" action=\"bypass -h admin_delShopItem " + tradeListID + " " + itemID + " 1\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("<br><button value=\"Back\" action=\"bypass -h admin_showShopList " + tradeListID +" 1\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("</center>");
		replyMSG.append("</body></html>");

		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private void addShopItem(L2PcInstance activeChar, String[] args)
	{
		int tradeListID = Integer.parseInt(args[1]);

		L2TradeList tradeList = TradeController.getInstance().getBuyList(tradeListID);
		if(tradeList == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "TradeList not found!");
			return;
		}

		if(args.length > 3)
		{
			int order = tradeList.getItems().size() + 1; // last item order + 1
			int itemID = Integer.parseInt(args[2]);
			int price = Integer.parseInt(args[3]);

			L2ItemInstance newItem = ItemTable.getInstance().createDummyItem(itemID);
			newItem.setPriceToSell(price);
			newItem.setCount(-1);
			tradeList.addItem(newItem);
			storeTradeList(itemID, price, tradeListID, order);

			activeChar.sendChatMessage(0, 0, "SYS", "Added "+newItem.getItem().getName()+" to Trade List "+tradeList.getListId());
			showShopList(activeChar, tradeListID, 1);
			return;
		}

		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

		TextBuilder replyMSG = new TextBuilder();
		replyMSG.append("<html><title>Merchant Shop Item Add</title>");
		replyMSG.append("<body>");
		replyMSG.append("<br>Add a new entry in merchantList.");
		replyMSG.append("<table>");
		replyMSG.append("<tr><td width=100>Property</td><td>Edit Field</td></tr>");
		replyMSG.append("<tr><td><br></td><td></td></tr>");
		replyMSG.append("<tr><td>ItemID</td><td><edit var=\"itemID\" width=80></td></tr>");
		replyMSG.append("<tr><td>Price</td><td><edit var=\"price\" width=80></td></tr>");
		replyMSG.append("</table>");
		replyMSG.append("<center><br><br><br>");
		replyMSG.append("<button value=\"Save\" action=\"bypass -h admin_addShopItem " + tradeListID + " $itemID $price\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("<br><button value=\"Back\" action=\"bypass -h admin_showShopList " + tradeListID +" 1\"  width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("</center>");
		replyMSG.append("</body></html>");

		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private void showShopList(L2PcInstance activeChar, int tradeListID, int page)
	{
		L2TradeList tradeList = TradeController.getInstance().getBuyList(tradeListID);
		if(page > tradeList.getItems().size() / PAGE_LIMIT + 1 || page < 1)
			return;

		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		TextBuilder html = itemListHtml(tradeList, page);

		adminReply.setHtml(html.toString());
		activeChar.sendPacket(adminReply);

	}

	private TextBuilder itemListHtml(L2TradeList tradeList, int page)
	{
		TextBuilder replyMSG = new TextBuilder();

		replyMSG.append("<html><title>Merchant Shop List Page: "+page+"</title>");
		replyMSG.append("<body>");
		replyMSG.append("<br>Edit, add or delete entries in a merchantList.");
		replyMSG.append("<table>");
		replyMSG.append("<tr><td width=150>Item Name</td><td width=60>Price</td><td width=40>Delete</td></tr>");
		int start = ((page-1) * PAGE_LIMIT);
		int end = Math.min(((page-1) * PAGE_LIMIT) + (PAGE_LIMIT-1), tradeList.getItems().size() - 1);
		for(L2ItemInstance item : tradeList.getItems(start, end+1))
		{
			replyMSG.append("<tr><td><a action=\"bypass -h admin_editShopItem "+tradeList.getListId()+" "+item.getItemId()+"\">"+item.getItem().getName()+"</a></td>");
			replyMSG.append("<td>"+item.getPriceToSell()+"</td>");
			replyMSG.append("<td><button value=\"Del\" action=\"bypass -h admin_delShopItem "+tradeList.getListId()+" "+item.getItemId()+"\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("</tr>");
		}
		replyMSG.append("<tr>");
		int min = 1;
		int max = tradeList.getItems().size() / PAGE_LIMIT + 1;
		if(page > 1)
		{
			replyMSG.append("<td><button value=\"Page"+(page - 1)+"\" action=\"bypass -h admin_showShopList "+tradeList.getListId()+" "+(page - 1)+"\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		}
		if(page < max)
		{
			if(page <= min)
				replyMSG.append("<td></td>");
			replyMSG.append("<td><button value=\"Page"+(page + 1)+"\" action=\"bypass -h admin_showShopList "+tradeList.getListId()+" "+(page + 1)+"\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		}
		replyMSG.append("</tr><tr><td>.</td></tr>");
		replyMSG.append("</table>");
		replyMSG.append("<center>");
		replyMSG.append("<button value=\"Add\" action=\"bypass -h admin_addShopItem "+tradeList.getListId()+"\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("<button value=\"Close\" action=\"bypass -h admin_close_window\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("</center></body></html>");

		return replyMSG;
	}

	private void showShop(L2PcInstance activeChar, int merchantID)
	{
		List<L2TradeList> tradeLists = getTradeLists(merchantID);
		if(tradeLists == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Unknown npc template ID" + merchantID);
			return ;
		}

		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

		TextBuilder replyMSG = new TextBuilder("<html><title>Merchant Shop Lists</title>");
		replyMSG.append("<body>");
		replyMSG.append("<br>Select a list to view");
		replyMSG.append("<table>");
		replyMSG.append("<tr><td>Mecrchant List ID</td></tr>");

		for(L2TradeList tradeList : tradeLists)
		{
			if(tradeList != null)
				replyMSG.append("<tr><td><a action=\"bypass -h admin_showShopList "+tradeList.getListId()+" 1\">Trade List "+tradeList.getListId()+"</a></td></tr>");
		}

		replyMSG.append("</table>");
		replyMSG.append("<center>");
		replyMSG.append("<button value=\"Close\" action=\"bypass -h admin_close_window\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("</center></body></html>");

		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private void storeTradeList(int itemID, int price, int tradeListID, int order)
	{
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stmt = con.prepareStatement("INSERT INTO merchant_buylists (`item_id`,`price`,`shop_id`,`order`) values ("+itemID+","+price+","+tradeListID+","+order+")");
			stmt.execute();
			stmt.close();
		}
		catch(SQLException esql)
		{
			esql.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(SQLException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void updateTradeList(int itemID, int price, int tradeListID, int order)
	{
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stmt = con.prepareStatement("UPDATE merchant_buylists SET `price`='"+price+"' WHERE `shop_id`='"+tradeListID+"' AND `order`='"+order+"'");
			stmt.execute();
			stmt.close();
		}catch(SQLException esql)
		{
			esql.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(SQLException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void deleteTradeList(int tradeListID, int order)
	{
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stmt = con.prepareStatement("DELETE FROM merchant_buylists WHERE `shop_id`='"+tradeListID+"' AND `order`='"+order+"'");
			stmt.execute();
			stmt.close();
		}
		catch(SQLException esql)
		{
			esql.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(SQLException e)
			{
				e.printStackTrace();
			}
		}
	}

	private int  findOrderTradeList(int itemID, int price, int tradeListID)
	{
		java.sql.Connection con = null;
		int order = 0;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stmt = con.prepareStatement("SELECT * FROM merchant_buylists WHERE `shop_id`='"+tradeListID+"' AND `item_id` ='"+itemID+"' AND `price` = '"+price+"'");
			ResultSet rs = stmt.executeQuery();
			rs.first();

			order = rs.getInt("order");

			stmt.close();
			rs.close();
		}
		catch(SQLException esql)
		{
			esql.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(SQLException e)
			{
				e.printStackTrace();
			}
		}
		return order;
	}

	private List<L2TradeList> getTradeLists(int merchantID)
	{
		String target = "npc_%objectId%_Buy";

		String content = HtmCache.getInstance().getHtm("data/html/merchant/"+merchantID+".htm");

		if(content == null)
		{
			content = HtmCache.getInstance().getHtm("data/html/merchant/30001.htm");
			if(content == null)
				return null;
		}

		List<L2TradeList> tradeLists = new FastList<L2TradeList>();

		String[] lines = content.split("\n");
		int pos = 0;

		for(String line : lines)
		{
			pos = line.indexOf(target);
			if(pos >= 0)
			{
				int tradeListID = Integer.decode((line.substring(pos+target.length()+1)).split("\"")[0]);
				tradeLists.add(TradeController.getInstance().getBuyList(tradeListID));
			}
		}
		return tradeLists;
	}

	public String[] getAdminCommandList() 
	{
		return ADMIN_COMMANDS;
	}

	private void showNpcProperty(L2PcInstance activeChar, L2NpcTemplate npc)
	{
		if(npc.isCustom())
		{
			activeChar.sendChatMessage(0, 0, "SYS", "You are going to modify Custom NPC");
		}

		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		String content = HtmCache.getInstance().getHtm("data/html/admin/game/editnpc.htm");

		if(content != null)
		{
			adminReply.setHtml(content);
			adminReply.replace("%npcId%", String.valueOf(npc.npcId));
			adminReply.replace("%templateId%", String.valueOf(npc.idTemplate));
			adminReply.replace("%name%", npc.name);
			adminReply.replace("%serverSideName%", npc.serverSideName == true ? "yes" : "no");
			adminReply.replace("%title%", npc.title);
			adminReply.replace("%serverSideTitle%", npc.serverSideTitle == true ? "yes" : "no");
			adminReply.replace("%collisionRadius%", String.valueOf(npc.collisionRadius));
			adminReply.replace("%collisionHeight%", String.valueOf(npc.collisionHeight));
			adminReply.replace("%level%", String.valueOf(npc.level));
			adminReply.replace("%sex%", String.valueOf(npc.sex));
			adminReply.replace("%type%", String.valueOf(npc.type));
			adminReply.replace("%attackRange%", String.valueOf(npc.baseAtkRange));
			adminReply.replace("%hp%", String.valueOf(npc.baseHpMax));
			adminReply.replace("%mp%", String.valueOf(npc.baseMpMax));
			adminReply.replace("%hpRegen%", String.valueOf(npc.baseHpReg));
			adminReply.replace("%mpRegen%", String.valueOf(npc.baseMpReg));
			adminReply.replace("%str%", String.valueOf(npc.baseSTR));
			adminReply.replace("%con%", String.valueOf(npc.baseCON));
			adminReply.replace("%dex%", String.valueOf(npc.baseDEX));
			adminReply.replace("%int%", String.valueOf(npc.baseINT));
			adminReply.replace("%wit%", String.valueOf(npc.baseWIT));
			adminReply.replace("%men%", String.valueOf(npc.baseMEN));
			adminReply.replace("%exp%", String.valueOf(npc.rewardExp));
			adminReply.replace("%sp%", String.valueOf(npc.rewardSp));
			adminReply.replace("%pAtk%", String.valueOf(npc.basePAtk));
			adminReply.replace("%pDef%", String.valueOf(npc.basePDef));
			adminReply.replace("%mAtk%", String.valueOf(npc.baseMAtk));
			adminReply.replace("%mDef%", String.valueOf(npc.baseMDef));
			adminReply.replace("%pAtkSpd%", String.valueOf(npc.basePAtkSpd));
			adminReply.replace("%aggro%", String.valueOf(npc.aggroRange));
			adminReply.replace("%mAtkSpd%", String.valueOf(npc.baseMAtkSpd));
			adminReply.replace("%rHand%", String.valueOf(npc.rhand));
			adminReply.replace("%lHand%", String.valueOf(npc.lhand));
			adminReply.replace("%armor%", String.valueOf(npc.armor));
			adminReply.replace("%walkSpd%", String.valueOf(npc.baseWalkSpd));
			adminReply.replace("%runSpd%", String.valueOf(npc.baseRunSpd));
			adminReply.replace("%factionId%", npc.factionId == null ? "" : npc.factionId);
			adminReply.replace("%factionRange%", String.valueOf(npc.factionRange));
			adminReply.replace("%absorbLevel%", String.valueOf(npc.absorbLevel));
		}
		else
			adminReply.setHtml("<html><head><body>File not found: data/html/admin/game/editnpc.htm</body></html>");
		activeChar.sendPacket(adminReply);
	}

	private void saveNpcProperty(L2PcInstance activeChar, String command)
	{
		String[] commandSplit = command.split(" ");

		if(commandSplit.length < 4)
			return;

		StatsSet newNpcData = new StatsSet();

		try
		{
			newNpcData.set("npcId", commandSplit[1]);

			String statToSet = commandSplit[2];
			String value = commandSplit[3];

			if(commandSplit.length > 4)
			{
				for(int i=0;i<commandSplit.length-3;i++)
					value += " " + commandSplit[i+4];
			}

			if(statToSet.equals("templateId"))
				newNpcData.set("idTemplate", Integer.valueOf(value));
			else if(statToSet.equals("name"))
				newNpcData.set("name", value);
			else if(statToSet.equals("serverSideName"))
				newNpcData.set("serverSideName", Integer.valueOf(value));
			else if(statToSet.equals("title"))
				newNpcData.set("title", value);
			else if(statToSet.equals("serverSideTitle"))
				newNpcData.set("serverSideTitle", Integer.valueOf(value) == 1 ? 1 : 0);
			else if(statToSet.equals("collisionRadius"))
				newNpcData.set("collision_radius", Integer.valueOf(value));
			else if(statToSet.equals("collisionHeight"))
				newNpcData.set("collision_height", Integer.valueOf(value));
			else if(statToSet.equals("level"))
				newNpcData.set("level", Integer.valueOf(value));
			else if(statToSet.equals("sex"))
			{
				int intValue = Integer.valueOf(value);
				newNpcData.set("sex", intValue == 0 ? "male" : intValue == 1 ? "female" : "etc");
			}
			else if(statToSet.equals("type"))
			{
				Class.forName("com.src.gameserver.model.actor.instance." + value + "Instance");
				newNpcData.set("type", value);
			}
			else if(statToSet.equals("attackRange"))
				newNpcData.set("attackrange", Integer.valueOf(value));
			else if(statToSet.equals("hp"))
				newNpcData.set("hp", Integer.valueOf(value));
			else if(statToSet.equals("mp"))
				newNpcData.set("mp", Integer.valueOf(value));
			else if(statToSet.equals("hpRegen"))
				newNpcData.set("hpreg", Integer.valueOf(value));
			else if(statToSet.equals("mpRegen"))
				newNpcData.set("mpreg", Integer.valueOf(value));
			else if(statToSet.equals("str"))
				newNpcData.set("str", Integer.valueOf(value));
			else if(statToSet.equals("con"))
				newNpcData.set("con", Integer.valueOf(value));
			else if(statToSet.equals("dex"))
				newNpcData.set("dex", Integer.valueOf(value));
			else if(statToSet.equals("int"))
				newNpcData.set("int", Integer.valueOf(value));
			else if(statToSet.equals("wit"))
				newNpcData.set("wit", Integer.valueOf(value));
			else if(statToSet.equals("men"))
				newNpcData.set("men", Integer.valueOf(value));
			else if(statToSet.equals("exp"))
				newNpcData.set("exp", Integer.valueOf(value));
			else if(statToSet.equals("sp"))
				newNpcData.set("sp", Integer.valueOf(value));
			else if(statToSet.equals("pAtk"))
				newNpcData.set("patk", Integer.valueOf(value));
			else if(statToSet.equals("pDef"))
				newNpcData.set("pdef", Integer.valueOf(value));
			else if(statToSet.equals("mAtk"))
				newNpcData.set("matk", Integer.valueOf(value));
			else if(statToSet.equals("mDef"))
				newNpcData.set("mdef", Integer.valueOf(value));
			else if(statToSet.equals("pAtkSpd"))
				newNpcData.set("atkspd", Integer.valueOf(value));
			else if(statToSet.equals("aggro"))
				newNpcData.set("aggro", Integer.valueOf(value));
			else if(statToSet.equals("mAtkSpd"))
				newNpcData.set("matkspd", Integer.valueOf(value));
			else if(statToSet.equals("rHand"))
				newNpcData.set("rhand", Integer.valueOf(value));
			else if(statToSet.equals("lHand"))
				newNpcData.set("lhand", Integer.valueOf(value));
			else if(statToSet.equals("armor"))
				newNpcData.set("armor", Integer.valueOf(value));
			else if(statToSet.equals("runSpd"))
				newNpcData.set("runspd", Integer.valueOf(value));
			else if(statToSet.equals("factionId"))
				newNpcData.set("faction_id", value);
			else if(statToSet.equals("factionRange"))
				newNpcData.set("faction_range", Integer.valueOf(value));
			else if(statToSet.equals("isUndead"))
				newNpcData.set("isUndead", Integer.valueOf(value) == 1 ? 1 : 0);
			else if(statToSet.equals("absorbLevel"))
			{
				int intVal = Integer.valueOf(value);
				newNpcData.set("absorb_level", intVal < 0 ? 0 : intVal > 12 ? 0 : intVal);
			}
		}
		catch(Exception e)
		{
			_log.warning("Error saving new npc value: " + e);
		}

		int npcId = newNpcData.getInteger("npcId");
		final L2NpcTemplate old = NpcTable.getInstance().getTemplate(npcId);

		if(old.isCustom())
		{
			activeChar.sendChatMessage(0, 0, "SYS", "You are going to save Custom NPC");
		}

		NpcTable.getInstance().saveNpc(newNpcData);
		NpcTable.getInstance().reloadNpc(npcId);
		showNpcProperty(activeChar, NpcTable.getInstance().getTemplate(npcId));
	}

	private void showNpcDropList(L2PcInstance activeChar, int npcId, int page)
	{
		L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
		if(npcData == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Unknown npc template id " + npcId);
			return;
		}

		final StringBuilder replyMSG = new StringBuilder(2900);
		replyMSG.append("<html><title>Show droplist page ");
		replyMSG.append(page);
		replyMSG.append("</title><body><br1><center><font color=\"LEVEL\">");
		replyMSG.append(npcData.name);
		replyMSG.append(" (");
		replyMSG.append(npcId);
		replyMSG.append(")</font></center><br>Drop type legend: <font color=\"3BB9FF\">Drop</font> | <font color=\"00ff00\">Sweep</font> | <font color=\"C12869\">Quest</font><br1><table width=\"100%\" border=0><tr><td width=35>cat.</td><td width=240>item</td><td width=25>del</td></tr>");
		
		int myPage = 1;
		int i = 0;
		int shown = 0;
		boolean hasMore = false;
		if(npcData.getDropData() != null)
		{
			for(L2DropCategory cat : npcData.getDropData())
			{
				
				if(shown == PAGE_LIMIT)
				{
					hasMore = true;
					break;
				}
				for(L2DropData drop : cat.getAllDrops())
				{
					final String color = (drop.isQuestDrop() ? "C12869" : (cat.isSweep() ? "00ff00" : "3BB9FF"));
					
					if(myPage != page)
					{
						i++;
						if(i == PAGE_LIMIT)
						{
							myPage++;
							i = 0;
						}
						continue;
					}
					if(shown == PAGE_LIMIT)
					{
						hasMore = true;
						break;
					}
					
					replyMSG.append("<tr><td><font color=\"");
					replyMSG.append(color);
					replyMSG.append("\">");
					replyMSG.append(cat.getCategoryType());
					replyMSG.append("</td><td><a action=\"bypass -h admin_edit_drop ");
					replyMSG.append(npcId);
					replyMSG.append(" ");
					replyMSG.append(drop.getItemId());
					replyMSG.append(" ");
					replyMSG.append(cat.getCategoryType());
					replyMSG.append("\">");
					replyMSG.append(ItemTable.getInstance().getTemplate(drop.getItemId()).getName());
					replyMSG.append(" (");
					replyMSG.append(drop.getItemId());
					replyMSG.append(")</a></td><td><a action=\"bypass -h admin_del_drop ");
					replyMSG.append(npcId);
					replyMSG.append(" ");
					replyMSG.append(drop.getItemId());
					replyMSG.append(" ");
					replyMSG.append(cat.getCategoryType());
					replyMSG.append("\">del</a></font></td></tr>");
					shown++;
				}
			}
		}
		
		replyMSG.append("</table><table width=300 bgcolor=666666 border=0><tr>");
		
		if(page > 1)
		{
			replyMSG.append("<td width=120><a action=\"bypass -h admin_show_droplist ");
			replyMSG.append(npcId);
			replyMSG.append(" ");
			replyMSG.append(page - 1);
			replyMSG.append("\">Prev Page</a></td>");
			if(!hasMore)
			{
				replyMSG.append("<td width=100>Page ");
				replyMSG.append(page);
				replyMSG.append("</td><td width=70></td></tr>");
			}
		}
		if(hasMore)
		{
			if(page <= 1)
				replyMSG.append("<td width=120></td>");
			replyMSG.append("<td width=100>Page ");
			replyMSG.append(page);
			replyMSG.append("</td><td width=70><a action=\"bypass -h admin_show_droplist ");
			replyMSG.append(npcId);
			replyMSG.append(" ");
			replyMSG.append(page + 1);
			replyMSG.append("\">Next Page</a></td></tr>");
		}
		
		replyMSG.append("</table><center><br><button value=\"Add Drop\" action=\"bypass -h admin_add_drop ");
		replyMSG.append(npcId);
		replyMSG.append("\" width=70 height=21 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\"><button value=\"Close\" action=\"bypass -h admin_close_window\" width=70 height=21 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\"></center></body></html>");
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private void showEditDropData(L2PcInstance activeChar, int npcId, int itemId, int category)
	{
		L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
		if(npcData == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Unknown npc template id " + npcId);
			return;
		}
		
		L2Item itemData = ItemTable.getInstance().getTemplate(itemId);
		if(itemData == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Unknown item template id " + itemId);
			return;
		}
		
		final StringBuilder replyMSG = new StringBuilder();
		replyMSG.append("<html><title>Edit drop data</title><body>");
		
		List<L2DropData> dropDatas = null;
		if(npcData.getDropData() != null)
		{
			for(L2DropCategory dropCat : npcData.getDropData())
			{
				if(dropCat.getCategoryType() == category)
				{
					dropDatas = dropCat.getAllDrops();
					break;
				}
			}
		}
		
		L2DropData dropData = null;
		if(dropDatas != null)
		{
			for(L2DropData drop : dropDatas)
			{
				if(drop.getItemId() == itemId)
				{
					dropData = drop;
					break;
				}
			}
		}
		
		if(dropData != null)
		{
			replyMSG.append("<table width=\"100%\"><tr><td>Npc</td><td>");
			replyMSG.append(npcData.name);
			replyMSG.append(" (");
			replyMSG.append(npcId);
			replyMSG.append(")</td></tr><tr><td>Item</td><td>");
			replyMSG.append(itemData.getName());
			replyMSG.append(" (");
			replyMSG.append(itemId);
			replyMSG.append(")</td></tr><tr><td>Category</td><td>");
			replyMSG.append(((category == -1) ? "-1 (sweep)" : Integer.toString(category)));
			replyMSG.append("</td></tr>");
			replyMSG.append("<tr><td>Min count (");
			replyMSG.append(dropData.getMinDrop());
			replyMSG.append(")</td><td><edit var=\"min\" width=80></td></tr><tr><td>Max count (");
			replyMSG.append(dropData.getMaxDrop());
			replyMSG.append(")</td><td><edit var=\"max\" width=80></td></tr><tr><td>Chance (");
			replyMSG.append(dropData.getChance());
			replyMSG.append(")</td><td><edit var=\"chance\" width=80></td></tr></table><br>");
			
			replyMSG.append("<center><br><button value=\"Save\" action=\"bypass -h admin_edit_drop ");
			replyMSG.append(npcId);
			replyMSG.append(" ");
			replyMSG.append(itemId);
			replyMSG.append(" ");
			replyMSG.append(category);
			replyMSG.append(" $min $max $chance\" width=70 height=21 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\">");
		}
		else
		{
			replyMSG.append("No drop data detail found.<center><br>");
		}
		replyMSG.append("<button value=\"Back\" action=\"bypass -h admin_show_droplist ");
		replyMSG.append(npcId);
		replyMSG.append("\" width=100 height=20 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\"></center>");
		replyMSG.append("</body></html>");
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private void showAddDropData(L2PcInstance activeChar, L2NpcTemplate npcData)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

		TextBuilder replyMSG = new TextBuilder("<html><title>Add dropdata to " + npcData.name + "(" + npcData.npcId + ")</title>");
		replyMSG.append("<body>");
		replyMSG.append("<table>");
		replyMSG.append("<tr><td>Item-Id</td><td><edit var=\"itemId\" width=80></td></tr>");
		replyMSG.append("<tr><td>MIN</td><td><edit var=\"min\" width=80></td></tr>");
		replyMSG.append("<tr><td>MAX</td><td><edit var=\"max\" width=80></td></tr>");
		replyMSG.append("<tr><td width=190>CATEGORY(sweep=-1)</td><td><edit var=\"category\" width=80></td></tr>");
		replyMSG.append("<tr><td>CHANCE(0-1000000)</td><td><edit var=\"chance\" width=80></td></tr>");
		replyMSG.append("</table>");
		replyMSG.append("<center>");
		replyMSG.append("<br><br>");
		replyMSG.append("<button value=\"SAVE\" action=\"bypass -h admin_add_drop " + npcData.npcId + " $itemId $category $min $max $chance\"  width=70 height=21 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\">");
		replyMSG.append("<button value=\"Back\" action=\"bypass -h admin_show_droplist " + npcData.npcId + "\"  width=70 height=21 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\">");
		replyMSG.append("</center>");
		replyMSG.append("</body></html>");

		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private void updateDropData(L2PcInstance activeChar, int npcId, int itemId, int min, int max, int category, int chance)
	{
		java.sql.Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			PreparedStatement statement = con.prepareStatement("UPDATE droplist SET min=?, max=?, chance=? WHERE mobId=? AND itemId=? AND category=?");
			statement.setInt(1, min);
			statement.setInt(2, max);
			statement.setInt(3, chance);
			statement.setInt(4, npcId);
			statement.setInt(5, itemId);
			statement.setInt(6, category);

			statement.execute();
			statement.close();

			PreparedStatement statement2 = con.prepareStatement("SELECT mobId FROM droplist WHERE mobId=? AND itemId=? AND category=?");
			statement2.setInt(1, npcId);
			statement2.setInt(2, itemId);
			statement2.setInt(3, category);

			ResultSet npcIdRs = statement2.executeQuery();
			if(npcIdRs.next()) npcId = npcIdRs.getInt("mobId");
			npcIdRs.close();
			statement2.close();

			if(npcId > 0)
			{
				reLoadNpcDropList(npcId);

				NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
				TextBuilder replyMSG = new TextBuilder("<html><title>Drop data modify complete!</title>");
				replyMSG.append("<body>");
				replyMSG.append("<center><button value=\"DropList\" action=\"bypass -h admin_show_droplist "+ npcId + "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
				replyMSG.append("</body></html>");

				adminReply.setHtml(replyMSG.toString());
				activeChar.sendPacket(adminReply);
			}
			else
				activeChar.sendChatMessage(0, 0, "SYS", "Unknown error!");
		}
		catch(Exception e){ e.printStackTrace(); }
		finally
		{
			try { con.close(); } catch(Exception e) {}
		}
	}

	private void addDropData(L2PcInstance activeChar, int npcId, int itemId, int min, int max, int category, int chance)
	{
		java.sql.Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			PreparedStatement statement = con.prepareStatement("INSERT INTO droplist(mobId, itemId, min, max, category, chance) values(?,?,?,?,?,?)");
			statement.setInt(1, npcId);
			statement.setInt(2, itemId);
			statement.setInt(3, min);
			statement.setInt(4, max);
			statement.setInt(5, category);
			statement.setInt(6, chance);
			statement.execute();
			statement.close();

			reLoadNpcDropList(npcId);

			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			TextBuilder replyMSG = new TextBuilder("<html><title>Add drop data complete!</title>");
			replyMSG.append("<body>");
			replyMSG.append("<center><button value=\"Continue add\" action=\"bypass -h admin_add_drop "+ npcId + "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
			replyMSG.append("<br><br><button value=\"DropList\" action=\"bypass -h admin_show_droplist "+ npcId + "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
			replyMSG.append("</center></body></html>");

			adminReply.setHtml(replyMSG.toString());
			activeChar.sendPacket(adminReply);
		}
		catch(Exception e){}
		finally
		{
			try { con.close(); } catch(Exception e) {}
		}
	}

	private void deleteDropData(L2PcInstance activeChar, int npcId, int itemId, int category)
	{
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			if(npcId > 0)
			{
				PreparedStatement statement2 = con.prepareStatement("DELETE FROM droplist WHERE mobId=? AND itemId=? AND category=?");
				statement2.setInt(1, npcId);
				statement2.setInt(2, itemId);
				statement2.setInt(3, category);
				statement2.execute();
				statement2.close();

				reLoadNpcDropList(npcId);

				NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
				TextBuilder replyMSG = new TextBuilder("<html><title>Delete drop data(" + npcId+", "+ itemId+", "+ category + ")complete</title>");
				replyMSG.append("<body>");
				replyMSG.append("<center><button value=\"DropList\" action=\"bypass -h admin_show_droplist "+ npcId + "\" width=100 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
				replyMSG.append("</body></html>");

				adminReply.setHtml(replyMSG.toString());
				activeChar.sendPacket(adminReply);

			}
		}
		catch(Exception e){}
		finally
		{
			try { con.close(); } catch(Exception e) {}
		}

	}

	private void reLoadNpcDropList(int npcId)
	{
		L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
		if(npcData == null)
			return;

		// reset the drop lists
		npcData.clearAllDropData();

		// get the drops
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			L2DropData dropData = null;

			npcData.getDropData().clear();

			PreparedStatement statement = con.prepareStatement("SELECT " + L2DatabaseFactory.getInstance().safetyString(new String[] {"mobId", "itemId", "min", "max", "category", "chance"}) + " FROM droplist WHERE mobId=?");
			statement.setInt(1, npcId);
			ResultSet dropDataList = statement.executeQuery();

			while(dropDataList.next())
			{
				dropData = new L2DropData();

				dropData.setItemId(dropDataList.getInt("itemId"));
				dropData.setMinDrop(dropDataList.getInt("min"));
				dropData.setMaxDrop(dropDataList.getInt("max"));
				dropData.setChance(dropDataList.getInt("chance"));

				int category = dropDataList.getInt("category");
				npcData.addDropData(dropData, category);
			}
			dropDataList.close();
			statement.close();
		}
		catch(Exception e){}
		finally
		{
			try { con.close(); } catch(Exception e) {}
		}
	}
	
	private void showNpcSkillList(L2PcInstance activeChar, int npcId, int page)
	{
		L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
		if(npcData == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Template id unknown: " + npcId);
			return;
		}
		
		Map<Integer, L2Skill> skills = new FastMap<Integer, L2Skill>();
		if(npcData.getSkills() != null)
			skills = npcData.getSkills();
		
		int _skillsize = skills.size();
		
		int MaxSkillsPerPage = PAGE_LIMIT;
		int MaxPages = _skillsize / MaxSkillsPerPage;
		if(_skillsize > MaxSkillsPerPage * MaxPages)
			MaxPages++;
		
		if(page > MaxPages)
			page = MaxPages;
		
		int SkillsStart = MaxSkillsPerPage * page;
		int SkillsEnd = _skillsize;
		if(SkillsEnd - SkillsStart > MaxSkillsPerPage)
			SkillsEnd = SkillsStart + MaxSkillsPerPage;
		
		StringBuffer replyMSG = new StringBuffer("<html><title>Show NPC Skill List</title><body><center><font color=\"LEVEL\">");
		replyMSG.append(npcData.getName());
		replyMSG.append(" (");
		replyMSG.append(npcData.npcId);
		replyMSG.append("): ");
		replyMSG.append(_skillsize);
		replyMSG.append(" skills</font></center><table width=300 bgcolor=666666><tr>");
		
		for(int x = 0; x < MaxPages; x++)
		{
			int pagenr = x + 1;
			if(page == x)
			{
				replyMSG.append("<td>Page ");
				replyMSG.append(pagenr);
				replyMSG.append("</td>");
			}
			else
			{
				replyMSG.append("<td><a action=\"bypass -h admin_show_skilllist_npc ");
				replyMSG.append(npcData.npcId);
				replyMSG.append(" ");
				replyMSG.append(x);
				replyMSG.append("\"> Page ");
				replyMSG.append(pagenr);
				replyMSG.append(" </a></td>");
			}
		}
		replyMSG.append("</tr></table><table width=\"100%\" border=0><tr><td>Skill name [skill id-skill lvl]</td><td>Delete</td></tr>");
		
		Set<Integer> skillset = skills.keySet();
		Iterator<Integer> skillite = skillset.iterator();
		int skillobj = 0;
		
		for(int i = 0; i < SkillsStart; i++)
		{
			if(skillite.hasNext())
				skillite.next();
		}
		
		int cnt = SkillsStart;
		while (skillite.hasNext())
		{
			cnt++;
			if(cnt > SkillsEnd)
				break;
			
			skillobj = skillite.next();
			replyMSG.append("<tr><td width=240><a action=\"bypass -h admin_edit_skill_npc ");
			replyMSG.append(npcData.npcId);
			replyMSG.append(" ");
			replyMSG.append(skills.get(skillobj).getId());
			replyMSG.append("\">");
			if(skills.get(skillobj).getSkillType() == L2SkillType.NOTDONE)
				replyMSG.append("<font color=\"777777\">"+skills.get(skillobj).getName()+"</font>");
			else
				replyMSG.append(skills.get(skillobj).getName());
			replyMSG.append(" [");
			replyMSG.append(skills.get(skillobj).getId());
			replyMSG.append("-");
			replyMSG.append(skills.get(skillobj).getLevel());
			replyMSG.append("]</a></td><td width=60><a action=\"bypass -h admin_del_skill_npc ");
			replyMSG.append(npcData.npcId);
			replyMSG.append(" ");
			replyMSG.append(skillobj);
			replyMSG.append("\">Delete</a></td></tr>");
		}
		replyMSG.append("</table><br><center><button value=\"Add Skill\" action=\"bypass -h admin_add_skill_npc ");
		replyMSG.append(npcId);
		replyMSG.append("\" width=70 height=21 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\"><button value=\"Close\" action=\"bypass -h admin_close_window\" width=70 height=21 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\"></center></body></html>");
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	private void showNpcSkillEdit(L2PcInstance activeChar, int npcId, int skillId)
	{
		try
		{
			StringBuffer replyMSG = new StringBuffer("<html><title>NPC Skill Edit</title><body>");
			
			L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
			if(npcData == null)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Template id unknown: " + npcId);
				return;
			}
			if(npcData.getSkills() == null)
				return;
			
			L2Skill npcSkill = npcData.getSkills().get(skillId);
			
			if(npcSkill != null)
			{
				replyMSG.append("<table width=\"100%\"><tr><td>NPC: </td><td>");
				replyMSG.append(NpcTable.getInstance().getTemplate(npcId).getName());
				replyMSG.append(" (");
				replyMSG.append(npcId);
				replyMSG.append(")</td></tr><tr><td>Skill: </td><td>");
				replyMSG.append(npcSkill.getName());
				replyMSG.append(" (");
				replyMSG.append(skillId);
				replyMSG.append(")</td></tr><tr><td>Skill Lvl: (");
				replyMSG.append(npcSkill.getLevel());
				replyMSG.append(") </td><td><edit var=\"level\" width=50></td></tr></table><br><center><button value=\"Save\" action=\"bypass -h admin_edit_skill_npc ");
				replyMSG.append(npcId);
				replyMSG.append(" ");
				replyMSG.append(skillId);
				replyMSG.append(" $level\" width=70 height=21 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\"><br1><button value=\"Back\" action=\"bypass -h admin_show_skilllist_npc ");
				replyMSG.append(npcId);
				replyMSG.append("\" width=70 height=21 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\"></center>");
			}
			
			replyMSG.append("</body></html>");
			
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			adminReply.setHtml(replyMSG.toString());
			activeChar.sendPacket(adminReply);
		}
		catch(Exception e)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Could not edit npc skills!");
			_log.warning("Error while editing npc skills (" + npcId + ", " + skillId + "): " + e);
		}
	}
	
	private void updateNpcSkillData(L2PcInstance activeChar, int npcId, int skillId, int level)
	{
		Connection con = null;
		try
		{
			L2Skill skillData = SkillTable.getInstance().getInfo(skillId, level);
			if(skillData == null)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Could not update npc skill: not existing skill id with that level!");
				showNpcSkillEdit(activeChar, npcId, skillId);
				return;
			}
			
			if(skillData.getLevel() != level)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Skill id with requested level doesn't exist! Skill level not changed.");
				showNpcSkillEdit(activeChar, npcId, skillId);
				return;
			}
			
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE `npcskills` SET `level`=? WHERE `npcid`=? AND `skillid`=?");
			statement.setInt(1, level);
			statement.setInt(2, npcId);
			statement.setInt(3, skillId);
				
			statement.execute();
			statement.close();
			
			reloadNpcSkillList(npcId);
			
			showNpcSkillList(activeChar, npcId, 0);
			activeChar.sendChatMessage(0, 0, "SYS", "Updated skill id " + skillId + " for npc id " + npcId + " to level " + level + ".");
		}
		catch(Exception e)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Could not update npc skill!");
			_log.warning("Error while updating npc skill (" + npcId + ", " + skillId + ", " + level + "): " + e);
		}
		finally
		{
			try { con.close(); } catch(Exception e) {}
		}
	}
	
	private void showNpcSkillAdd(L2PcInstance activeChar, int npcId)
	{
		L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
		
		StringBuffer replyMSG = new StringBuffer("<html><title>NPC Skill Add</title><body><table width=\"100%\"><tr><td>NPC: </td><td>");
		replyMSG.append(npcData.getName());
		replyMSG.append(" (");
		replyMSG.append(npcData.npcId);
		replyMSG.append(")</td></tr><tr><td>SkillId: </td><td><edit var=\"skillId\" width=80></td></tr><tr><td>Level: </td><td><edit var=\"level\" width=80></td></tr></table><br><center><button value=\"Add Skill\" action=\"bypass -h admin_add_skill_npc ");
		replyMSG.append(npcData.npcId);
		replyMSG.append(" $skillId $level\"  width=70 height=21 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\"><br1><button value=\"Back\" action=\"bypass -h admin_show_skilllist_npc ");
		replyMSG.append(npcData.npcId);
		replyMSG.append("\" width=70 height=21 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\"></center></body></html>");
		
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
	
	private void addNpcSkillData(L2PcInstance activeChar, int npcId, int skillId, int level)
	{
		Connection con = null;
		try
		{
			// skill check
			L2Skill skillData = SkillTable.getInstance().getInfo(skillId, level);
			if(skillData == null)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Could not add npc skill: not existing skill id with that level!");
				showNpcSkillAdd(activeChar, npcId);
				return;
			}
			
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO `npcskills`(`npcid`, `skillid`, `level`) VALUES(?,?,?)");
			statement.setInt(1, npcId);
			statement.setInt(2, skillId);
			statement.setInt(3, level);
			statement.execute();
			statement.close();
			
			reloadNpcSkillList(npcId);
			
			showNpcSkillList(activeChar, npcId, 0);
			activeChar.sendChatMessage(0, 0, "SYS", "Added skill " + skillId + "-" + level + " to npc id " + npcId + ".");
		}
		catch(Exception e)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Could not add npc skill!");
			_log.warning("Error while adding a npc skill (" + npcId + ", " + skillId + ", " + level + "): " + e);
		}
		finally
		{
			try { con.close(); } catch(Exception e) {}
		}
	}
	
	private void deleteNpcSkillData(L2PcInstance activeChar, int npcId, int skillId)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			
			if(npcId > 0)
			{
				PreparedStatement statement = con.prepareStatement("DELETE FROM `npcskills` WHERE `npcid`=? AND `skillid`=?");
				statement.setInt(1, npcId);
				statement.setInt(2, skillId);
				statement.execute();
				statement.close();
				
				reloadNpcSkillList(npcId);
				
				showNpcSkillList(activeChar, npcId, 0);
				activeChar.sendChatMessage(0, 0, "SYS", "Deleted skill id " + skillId + " from npc id " + npcId + ".");
			}
		}
		catch(Exception e)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Could not delete npc skill!");
			_log.warning("Error while deleting npc skill (" + npcId + ", " + skillId + "): " + e);
		}
		finally
		{
			try { con.close(); } catch(Exception e) {}
		}
	}
	
	private void reloadNpcSkillList(int npcId)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			L2NpcTemplate npcData = NpcTable.getInstance().getTemplate(npcId);
			
			L2Skill skillData = null;
			if(npcData.getSkills() != null)
				npcData.getSkills().clear();
			
			
			// without race
			PreparedStatement statement = con.prepareStatement("SELECT `skillid`, `level` FROM `npcskills` WHERE `npcid`=? AND `skillid` <> 4416");
			statement.setInt(1, npcId);
			ResultSet skillDataList = statement.executeQuery();
			
			while (skillDataList.next())
			{
				int idval = skillDataList.getInt("skillid");
				int levelval = skillDataList.getInt("level");
				skillData = SkillTable.getInstance().getInfo(idval, levelval);
				if(skillData != null)
					npcData.addSkill(skillData);
			}
			skillDataList.close();
			statement.close();
		}
		catch(Exception e)
		{
			_log.warning("Error while reloading npc skill list (" + npcId + "): " + e);
		}
		finally
		{
			try { con.close(); } catch(Exception e) {}
		}
	}
}