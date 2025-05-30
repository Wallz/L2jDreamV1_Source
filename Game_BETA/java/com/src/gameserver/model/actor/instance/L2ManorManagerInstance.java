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

import java.util.StringTokenizer;

import javolution.util.FastList;

import com.src.gameserver.TradeController;
import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.datatables.sql.ItemTable;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.managers.CastleManorManager;
import com.src.gameserver.managers.CastleManorManager.SeedProduction;
import com.src.gameserver.model.L2TradeList;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.BuyList;
import com.src.gameserver.network.serverpackets.BuyListSeed;
import com.src.gameserver.network.serverpackets.ExShowCropInfo;
import com.src.gameserver.network.serverpackets.ExShowManorDefaultInfo;
import com.src.gameserver.network.serverpackets.ExShowProcureCropDetail;
import com.src.gameserver.network.serverpackets.ExShowSeedInfo;
import com.src.gameserver.network.serverpackets.ExShowSellCropList;
import com.src.gameserver.network.serverpackets.MyTargetSelected;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.network.serverpackets.ValidateLocation;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public class L2ManorManagerInstance extends L2MerchantInstance
{
	public L2ManorManagerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if(!canTarget(player))
		{
			return;
		}
		player.setLastFolkNPC(this);

		if(this != player.getTarget())
		{
			player.setTarget(this);

			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
			my = null;

			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			if(!canInteract(player))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				if(CastleManorManager.getInstance().isDisabled())
				{
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile("data/html/npcdefault.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					html.replace("%npcname%", getName());
					player.sendPacket(html);
					html = null;
				}
				else if(!player.isGM() && getCastle() != null && getCastle().getCastleId() > 0 && player.getClan() != null && getCastle().getOwnerId() == player.getClanId() && player.isClanLeader()
				)
				{
					showMessageWindow(player, "manager-lord.htm");
				}
				else
				{
					showMessageWindow(player, "manager.htm");
				}
			}
		}

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private void showBuyWindow(L2PcInstance player, String val)
	{
		double taxRate = 0;
		player.tempInventoryDisable();

		L2TradeList list = TradeController.getInstance().getBuyList(Integer.parseInt(val));

		if(list != null)
		{
			list.getItems().get(0).setCount(1);
			BuyList bl = new BuyList(list, player.getAdena(), taxRate);
			player.sendPacket(bl);
		}
		else
		{
			_log.info("possible client hacker: " + player.getName() + " attempting to buy from GM shop! (L2ManorManagerIntance)");
			_log.info("buylist id:" + val);
		}

		list = null;
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if(player.getLastFolkNPC() == null || player.getLastFolkNPC().getObjectId() != getObjectId())
		{
			return;
		}

		if(command.startsWith("manor_menu_select"))
		{
			if(CastleManorManager.getInstance().isUnderMaintenance())
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				player.sendPacket(new SystemMessage(SystemMessageId.THE_MANOR_SYSTEM_IS_CURRENTLY_UNDER_MAINTENANCE));
				return;
			}

			String params = command.substring(command.indexOf("?") + 1);
			StringTokenizer st = new StringTokenizer(params, "&");
			int ask = Integer.parseInt(st.nextToken().split("=")[1]);
			int state = Integer.parseInt(st.nextToken().split("=")[1]);
			int time = Integer.parseInt(st.nextToken().split("=")[1]);

			int castleId;
			if(state == -1)
			{
				castleId = getCastle().getCastleId();
			}
			else
			{
				castleId = state;
			}

			switch(ask)
			{
				case 1:
					if(castleId != getCastle().getCastleId())
					{
						player.sendPacket(new SystemMessage(SystemMessageId.HERE_YOU_CAN_BUY_ONLY_SEEDS_OF_S1_MANOR));
					}
					else
					{
						L2TradeList tradeList = new L2TradeList(0);
						FastList<SeedProduction> seeds = getCastle().getSeedProduction(CastleManorManager.PERIOD_CURRENT);

						for(SeedProduction s : seeds)
						{
							L2ItemInstance item = ItemTable.getInstance().createDummyItem(s.getId());
							item.setPriceToSell(s.getPrice());
							item.setCount(s.getCanProduce());
							if(item.getCount() > 0 && item.getPriceToSell() > 0)
							{
								tradeList.addItem(item);
							}
						}

						BuyListSeed bl = new BuyListSeed(tradeList, castleId, player.getAdena());
						player.sendPacket(bl);
						tradeList = null;
						bl = null;
						seeds = null;
					}
					break;
				case 2:
					player.sendPacket(new ExShowSellCropList(player, castleId, getCastle().getCropProcure(CastleManorManager.PERIOD_CURRENT)));
					break;
				case 3:
					if(time == 1 && !CastleManager.getInstance().getCastleById(castleId).isNextPeriodApproved())
					{
						player.sendPacket(new ExShowSeedInfo(castleId, null));
					}
					else
					{
						player.sendPacket(new ExShowSeedInfo(castleId, CastleManager.getInstance().getCastleById(castleId).getSeedProduction(time)));
					}
					break;
				case 4:
					if(time == 1 && !CastleManager.getInstance().getCastleById(castleId).isNextPeriodApproved())
					{
						player.sendPacket(new ExShowCropInfo(castleId, null));
					}
					else
					{
						player.sendPacket(new ExShowCropInfo(castleId, CastleManager.getInstance().getCastleById(castleId).getCropProcure(time)));
					}
					break;
				case 5:
					player.sendPacket(new ExShowManorDefaultInfo());
					break;
				case 6:
					showBuyWindow(player, "3" + getNpcId());
					break;
				case 9:
					player.sendPacket(new ExShowProcureCropDetail(state));
					break;
			}
			params = null;
			st = null;
		}
		else if(command.startsWith("help"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			String filename = "manor_client_help00" + st.nextToken() + ".htm";
			showMessageWindow(player, filename);
			st = null;
			filename = null;
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}

	public String getHtmlPath()
	{
		return "data/html/manormanager/";
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		return "data/html/manormanager/manager.htm";
	}

	private void showMessageWindow(L2PcInstance player, String filename)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(getHtmlPath() + filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
		html = null;
	}

}