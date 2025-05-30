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
package com.src.gameserver.model.actor.instance;

import java.util.Map;

import com.src.Config;
import com.src.gameserver.cache.HtmCache;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.itemcontainer.PcFreight;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.EnchantResult;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.PackageToList;
import com.src.gameserver.network.serverpackets.SortedWareHouseWithdrawalList;
import com.src.gameserver.network.serverpackets.SortedWareHouseWithdrawalList.WarehouseListType;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.network.serverpackets.WareHouseDepositList;
import com.src.gameserver.network.serverpackets.WareHouseWithdrawalList;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public final class L2WarehouseInstance extends L2NpcInstance
{
	public L2WarehouseInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		if(val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}
		return "data/html/warehouse/" + pom + ".htm";
	}

	private void showRetrieveWindow(L2PcInstance player, WarehouseListType itemtype, byte sortorder)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		player.setActiveWarehouse(player.getWarehouse());

		if(player.getActiveWarehouse().getSize() == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_DEPOSITED_IN_WH));
			return;
		}

		player.sendPacket(new SortedWareHouseWithdrawalList(player, WareHouseWithdrawalList.PRIVATE, itemtype, sortorder));
	}

	private void showRetrieveWindow(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		player.setActiveWarehouse(player.getWarehouse());

		if(player.getActiveWarehouse().getSize() == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_DEPOSITED_IN_WH));
			return;
		}

		player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.PRIVATE));
	}

	private void showDepositWindow(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		player.setActiveWarehouse(player.getWarehouse());
		player.tempInventoryDisable();

		player.sendPacket(new WareHouseDepositList(player, WareHouseDepositList.PRIVATE));
	}

	private void showDepositWindowClan(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		if(player.getClan() != null)
		{
			if(player.getClan().getLevel() == 0)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE));
			}
			else
			{
				player.setActiveWarehouse(player.getClan().getWarehouse());
				player.tempInventoryDisable();

				WareHouseDepositList dl = new WareHouseDepositList(player, WareHouseDepositList.CLAN);
				player.sendPacket(dl);
				dl = null;
			}
		}
	}

	private void showWithdrawWindowClan(L2PcInstance player, WarehouseListType itemtype, byte sortorder)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		if((player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) != L2Clan.CP_CL_VIEW_WAREHOUSE)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_CLAN_WAREHOUSE));
			return;
		}
		else
		{
			if(player.getClan().getLevel() == 0)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE));
			}
			else
			{
				player.setActiveWarehouse(player.getClan().getWarehouse());
				player.sendPacket(new SortedWareHouseWithdrawalList(player, WareHouseWithdrawalList.CLAN, itemtype, sortorder));
			}
		}
	}

	private void showWithdrawWindowClan(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		if((player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) != L2Clan.CP_CL_VIEW_WAREHOUSE)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_CLAN_WAREHOUSE));
			return;
		}
		else
		{
			if(player.getClan().getLevel() == 0)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE));
			}
			else
			{
				player.setActiveWarehouse(player.getClan().getWarehouse());
				player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.CLAN));
			}
		}
	}

	public void showWithdrawWindowFreight(L2PcInstance player, WarehouseListType itemtype, byte sortorder)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);

		PcFreight freight = player.getFreight();

		if(freight != null)
		{
			if(freight.getSize() > 0)
			{
				if(Config.ALT_GAME_FREIGHTS)
				{
					freight.setActiveLocation(0);
				}
				else
				{
					freight.setActiveLocation(getWorldRegion().hashCode());
				}
				player.setActiveWarehouse(freight);
				player.sendPacket(new SortedWareHouseWithdrawalList(player, WareHouseWithdrawalList.FREIGHT, itemtype, sortorder));
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_DEPOSITED_IN_WH));
			}
		}

		freight = null;
	}

	private void showWithdrawWindowFreight(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);

		PcFreight freight = player.getFreight();

		if(freight != null)
		{
			if(freight.getSize() > 0)
			{
				if(Config.ALT_GAME_FREIGHTS)
				{
					freight.setActiveLocation(0);
				}
				else
				{
					freight.setActiveLocation(getWorldRegion().hashCode());
				}
				player.setActiveWarehouse(freight);
				player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.FREIGHT));
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessageId.NO_ITEM_DEPOSITED_IN_WH));
			}
		}

		freight = null;
	}

	private void showDepositWindowFreight(L2PcInstance player)
	{
		if(player.getAccountChars().size() == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CHARACTER_DOES_NOT_EXIST));
		}
		else
		{
			Map<Integer, String> chars = player.getAccountChars();
			if(chars.size() < 1)
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			player.sendPacket(new PackageToList(chars));
			chars = null;
		}
	}

	private void showDepositWindowFreight(L2PcInstance player, int obj_Id)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		L2PcInstance destChar = L2PcInstance.load(obj_Id);
		if(destChar == null)
		{
			return;
		}

		PcFreight freight = destChar.getFreight();
		if(Config.ALT_GAME_FREIGHTS)
		{
			freight.setActiveLocation(0);
		}
		else
		{
			freight.setActiveLocation(getWorldRegion().hashCode());
		}
		player.setActiveWarehouse(freight);
		player.tempInventoryDisable();
		destChar.deleteMe();
		destChar = null;
		freight = null;

		player.sendPacket(new WareHouseDepositList(player, WareHouseDepositList.FREIGHT));
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if(player.getActiveEnchantItem() != null)
			player.sendPacket(new EnchantResult(0));

		String param[] = command.split("_");

		if(command.startsWith("WithdrawP"))
		{
			if(Config.ENABLE_WAREHOUSESORTING_PRIVATE)
			{
				String htmFile = "data/html/mods/warehouse/whsortedp.htm";
				String content = HtmCache.getInstance().getHtm(htmFile);
				if(content != null)
				{
					NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
					npcHtmlMessage.setHtml(content);
					npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(npcHtmlMessage);
				}
			}
			else
			{
				showRetrieveWindow(player);
			}
		}
		else if(command.startsWith("WithdrawSortedP") && Config.ENABLE_WAREHOUSESORTING_PRIVATE)
		{
			if(param.length > 2)
			{
				showRetrieveWindow(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.getOrder(param[2]));
			}
			else if(param.length > 1)
			{
				showRetrieveWindow(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.A2Z);
			}
			else
			{
				showRetrieveWindow(player, WarehouseListType.ALL, SortedWareHouseWithdrawalList.A2Z);
			}
		}
		else if(command.equals("DepositP"))
		{
			showDepositWindow(player);
		}
		else if(command.equals("WithdrawC"))
		{
			if(Config.ENABLE_WAREHOUSESORTING_CLAN)
			{
				String htmFile = "data/html/mods/warehouse/whsortedc.htm";
				String content = HtmCache.getInstance().getHtm(htmFile);
				if(content != null)
				{
					NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
					npcHtmlMessage.setHtml(content);
					npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(npcHtmlMessage);
				}
			}
			else
			{
				showWithdrawWindowClan(player);
			}
		}
		else if(command.startsWith("WithdrawSortedC") && Config.ENABLE_WAREHOUSESORTING_CLAN)
		{
			if(param.length > 2)
			{
				showWithdrawWindowClan(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.getOrder(param[2]));
			}
			else if(param.length > 1)
			{
				showWithdrawWindowClan(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.A2Z);
			}
			else
			{
				showWithdrawWindowClan(player, WarehouseListType.ALL, SortedWareHouseWithdrawalList.A2Z);
			}
		}
		else if(command.equals("DepositC"))
		{
			showDepositWindowClan(player);
		}
		else if(command.startsWith("WithdrawF"))
		{
			if(Config.ALLOW_FREIGHT)
			{
				showWithdrawWindowFreight(player);
			}
		}
		else if(command.startsWith("DepositF"))
		{
			if(Config.ALLOW_FREIGHT)
			{
				showDepositWindowFreight(player);
			}
		}
		else if(command.startsWith("FreightChar"))
		{
			if(Config.ALLOW_FREIGHT)
			{
				int startOfId = command.lastIndexOf("_") + 1;
				String id = command.substring(startOfId);
				showDepositWindowFreight(player, Integer.parseInt(id));
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
}