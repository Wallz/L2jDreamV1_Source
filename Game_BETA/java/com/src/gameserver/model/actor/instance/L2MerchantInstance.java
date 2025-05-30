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

import javolution.text.TextBuilder;

import com.src.Config;
import com.src.gameserver.TradeController;
import com.src.gameserver.model.L2TradeList;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.multisell.L2Multisell;
import com.src.gameserver.network.L2GameClient;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.BuyList;
import com.src.gameserver.network.serverpackets.MyTargetSelected;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.Ride;
import com.src.gameserver.network.serverpackets.SellList;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.WearList;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public class L2MerchantInstance extends L2NpcInstance
{
	public L2MerchantInstance(int objectId, L2NpcTemplate template)
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

		return "data/html/merchant/" + pom + ".htm";
	}

	private void showWearWindow(L2PcInstance player, int val)
	{
		player.tempInventoryDisable();

		L2TradeList list = TradeController.getInstance().getBuyList(val);

		if(list != null)
		{
			WearList bl = new WearList(list, player.getAdena(), player.getExpertiseIndex());
			player.sendPacket(bl);
			list = null;
			bl = null;
		}
		else
		{
			_log.warning("no buylist with id:" + val);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}

	protected void showBuyWindow(L2PcInstance player, int val)
	{
		double taxRate = 0;

		if(getIsInTown())
		{
			taxRate = getCastle().getTaxRate();
		}

		player.tempInventoryDisable();

		L2TradeList list = TradeController.getInstance().getBuyList(val);

		if (list != null && list.getNpcId().equals(String.valueOf(getNpcId())))
			player.sendPacket(new BuyList(list, player.getAdena(), taxRate));
		else
		{
			_log.warning("possible client hacker: " + player.getName() + " attempting to buy from GM shop! (L2MechantInstance)");
			_log.warning("buylist id:" + val);
		}

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private void showSellWindow(L2PcInstance player)
	{
		player.sendPacket(new SellList(player));

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken();

		if(actualCommand.equalsIgnoreCase("Buy"))
		{
			if(st.countTokens() < 1)
			{
				return;
			}

			int val = Integer.parseInt(st.nextToken());
			showBuyWindow(player, val);
		}
		else if(actualCommand.equalsIgnoreCase("Sell"))
		{
			showSellWindow(player);
		}
		else if(actualCommand.equalsIgnoreCase("RentPet"))
		{
			if(Config.ALLOW_RENTPET)
			{
				if(st.countTokens() < 1)
				{
					showRentPetWindow(player);
				}
				else
				{
					int val = Integer.parseInt(st.nextToken());
					tryRentPet(player, val);
				}
			}
		}
		else if(actualCommand.equalsIgnoreCase("Wear") && Config.ALLOW_WEAR)
		{
			if(st.countTokens() < 1)
			{
				return;
			}

			int val = Integer.parseInt(st.nextToken());
			showWearWindow(player, val);
		}
		else if(actualCommand.equalsIgnoreCase("Multisell"))
		{
			if(st.countTokens() < 1)
			{
				return;
			}

			int val = Integer.parseInt(st.nextToken());
			L2Multisell.getInstance().SeparateAndSend(val, player, ((L2Npc)player.getTarget()).getNpcId(), false, getCastle().getTaxRate(), false);
		}
		else if(actualCommand.equalsIgnoreCase("Exc_Multisell"))
		{
			if(st.countTokens() < 1)
			{
				return;
			}

			int val = Integer.parseInt(st.nextToken());
			L2Multisell.getInstance().SeparateAndSend(val, player, ((L2Npc)player.getTarget()).getNpcId(), true, getCastle().getTaxRate(), false);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
		st = null;
		actualCommand = null;
	}

	public void showRentPetWindow(L2PcInstance player)
	{
		if(!Config.LIST_PET_RENT_NPC.contains(getTemplate().npcId))
		{
			return;
		}

		TextBuilder html1 = new TextBuilder("<html><body>Pet Manager:<br>");
		html1.append("You can rent a wyvern or strider for adena.<br>My prices:<br1>");
		html1.append("<table border=0><tr><td>Ride</td></tr>");
		html1.append("<tr><td>Wyvern</td><td>Strider</td></tr>");
		html1.append("<tr><td><a action=\"bypass -h npc_%objectId%_RentPet 1\">30 sec/1800 adena</a></td><td><a action=\"bypass -h npc_%objectId%_RentPet 11\">30 sec/900 adena</a></td></tr>");
		html1.append("<tr><td><a action=\"bypass -h npc_%objectId%_RentPet 2\">1 min/7200 adena</a></td><td><a action=\"bypass -h npc_%objectId%_RentPet 12\">1 min/3600 adena</a></td></tr>");
		html1.append("<tr><td><a action=\"bypass -h npc_%objectId%_RentPet 3\">10 min/720000 adena</a></td><td><a action=\"bypass -h npc_%objectId%_RentPet 13\">10 min/360000 adena</a></td></tr>");
		html1.append("<tr><td><a action=\"bypass -h npc_%objectId%_RentPet 4\">30 min/6480000 adena</a></td><td><a action=\"bypass -h npc_%objectId%_RentPet 14\">30 min/3240000 adena</a></td></tr>");
		html1.append("</table>");
		html1.append("</body></html>");

		insertObjectIdAndShowChatWindow(player, html1.toString());
		html1 = null;
	}

	public void tryRentPet(L2PcInstance player, int val)
	{
		if(player == null || player.getPet() != null || player.isMounted() || player.isRentedPet())
		{
			return;
		}

		if(!player.disarmWeapons())
		{
			return;
		}

		int petId;
		double price = 1;
		int cost[] =
		{
				1800, 7200, 720000, 6480000
		};
		int ridetime[] =
		{
				30, 60, 600, 1800
		};

		if(val > 10)
		{
			petId = 12526;
			val -= 10;
			price /= 2;
		}
		else
		{
			petId = 12621;
		}

		if(val < 1 || val > 4)
		{
			return;
		}

		price *= cost[val - 1];
		int time = ridetime[val - 1];

		if(!player.reduceAdena("Rent", (int) price, player.getLastFolkNPC(), true))
		{
			return;
		}

		Ride mount = new Ride(player.getObjectId(), Ride.ACTION_MOUNT, petId);
		player.broadcastPacket(mount);

		player.setMountType(mount.getMountType());
		player.startRentPet(time);
		mount = null;
	}

	@Override
	public void onActionShift(L2GameClient client)
	{
		L2PcInstance player = client.getActiveChar();
		if(player == null)
		{
			return;
		}

		if(player.getAccessLevel().isGm())
		{
			player.setTarget(this);
			player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));

			if(isAutoAttackable(player))
			{
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
			}

			NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile("data/html/admin/info/merchantinfo.htm");

			html.replace("%objid%", String.valueOf(getObjectId()));
			html.replace("%class%", getClass().getSimpleName());
			html.replace("%id%",    String.valueOf(getTemplate().npcId));
			html.replace("%lvl%",   String.valueOf(getTemplate().level));
			html.replace("%name%",  String.valueOf(getTemplate().name));
			html.replace("%hp%",    String.valueOf((int)((L2Character)this).getCurrentHp()));
			html.replace("%hpmax%", String.valueOf(((L2Character)this).getMaxHp()));
			html.replace("%mp%",    String.valueOf((int)((L2Character)this).getCurrentMp()));
			html.replace("%mpmax%", String.valueOf(((L2Character)this).getMaxMp()));
			html.replace("%loc%",  String.valueOf(getX()+" "+getY()+" "+getZ()));
			html.replace("%dist%", String.valueOf((int)Math.sqrt(player.getDistanceSq(this))));

			if (getSpawn() != null)
			{
				html.replace("%spawn%", getSpawn().getLocx()+" "+getSpawn().getLocy()+" "+getSpawn().getLocz());
				html.replace("%loc2d%", String.valueOf((int)Math.sqrt(((L2Character)this).getPlanDistanceSq(getSpawn().getLocx(), getSpawn().getLocy()))));
				html.replace("%loc3d%", String.valueOf((int)Math.sqrt(((L2Character)this).getDistanceSq(getSpawn().getLocx(), getSpawn().getLocy(), getSpawn().getLocz()))));
			}
			else
			{
				html.replace("%spawn%", "<font color=FF0000>null</font>");
				html.replace("%loc2d%", "<font color=FF0000>--</font>");
				html.replace("%loc3d%", "<font color=FF0000>--</font>");
			}

			player.sendPacket(html);
		}

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

}