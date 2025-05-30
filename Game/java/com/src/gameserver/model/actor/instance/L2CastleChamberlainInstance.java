/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.src.gameserver.model.actor.instance;

import java.util.Calendar;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.managers.CastleManorManager;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.entity.sevensigns.SevenSigns;
import com.src.gameserver.model.entity.siege.Castle;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.ExShowCropInfo;
import com.src.gameserver.network.serverpackets.ExShowCropSetting;
import com.src.gameserver.network.serverpackets.ExShowManorDefaultInfo;
import com.src.gameserver.network.serverpackets.ExShowSeedInfo;
import com.src.gameserver.network.serverpackets.ExShowSeedSetting;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.util.Util;

/**
 * Castle Chamberlains implementation, used for:
 * <ul>
 * <li>Tax rate control</li>
 * <li>Regional manor system control</li>
 * <li>Castle treasure control</li>
 * <li>Siege time modifier</li>
 * <li>Items production</li>
 * <li>Doors management</li>
 * <li>Doors/walls upgrades && traps</li>
 * </ul>
 */
public class L2CastleChamberlainInstance extends L2MerchantInstance
{
	protected static final int COND_ALL_FALSE = 0;
	protected static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	protected static final int COND_OWNER = 2;
	protected static final int COND_CLAN_MEMBER = 3;
	
	private int _preHour = 6;
	
	public L2CastleChamberlainInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		// BypassValidation Exploit plug.
		if (player.getLastFolkNPC().getObjectId() != getObjectId())
			return;
		
		final int cond = validateCondition(player);
		if (cond < COND_OWNER)
		{
			if (cond == COND_BUSY_BECAUSE_OF_SIEGE)
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/chamberlain/busy.htm");
				player.sendPacket(html);
			}
			return;
		}
		
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command
		
		String val = "";
		if (st.hasMoreTokens())
			val = st.nextToken();
		
		final Castle castle = getCastle();
		
		if (actualCommand.equalsIgnoreCase("banish_foreigner"))
		{
			if (!validatePrivileges(player, L2Clan.CP_CS_DISMISS))
				return;
			
			// Move non-clan members off castle area, and send html
			castle.banishForeigners();
			sendFileMessage(player, "data/html/chamberlain/banishafter.htm");
		}
		else if (actualCommand.equalsIgnoreCase("banish_foreigner_show"))
		{
			if (!validatePrivileges(player, L2Clan.CP_CS_DISMISS))
				return;
			
			sendFileMessage(player, "data/html/chamberlain/banishfore.htm");
		}
		else if (actualCommand.equalsIgnoreCase("manage_functions"))
		{
			if (!validatePrivileges(player, L2Clan.CP_CS_SET_FUNCTIONS))
				return;
			
			sendFileMessage(player, "data/html/chamberlain/manage.htm");
		}
		else if (actualCommand.equalsIgnoreCase("products"))
		{
			if (!validatePrivileges(player, L2Clan.CP_CS_USE_FUNCTIONS))
				return;
			
			sendFileMessage(player, "data/html/chamberlain/products.htm");
		}
		else if (actualCommand.equalsIgnoreCase("list_siege_clans"))
		{
			if (!validatePrivileges(player, L2Clan.CP_CS_MANAGE_SIEGE))
				return;
			
			castle.getSiege().listRegisterClan(player); // List current register clan
		}
		else if (actualCommand.equalsIgnoreCase("receive_report"))
		{
			if (cond == COND_CLAN_MEMBER)
				sendFileMessage(player, "data/html/chamberlain/noprivs.htm");
			else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/chamberlain/report.htm");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				L2Clan clan = ClanTable.getInstance().getClan(castle.getOwnerId());
				html.replace("%clanname%", clan.getName());
				html.replace("%clanleadername%", clan.getLeaderName());
				html.replace("%castlename%", castle.getName());
				
				switch (SevenSigns.getInstance().getCurrentPeriod())
				{
					case SevenSigns.PERIOD_COMP_RECRUITING:
						html.replace("%ss_event%", "Quest Event Initialization");
						break;
					
					case SevenSigns.PERIOD_COMPETITION:
						html.replace("%ss_event%", "Competition (Quest Event)");
						break;
					
					case SevenSigns.PERIOD_COMP_RESULTS:
						html.replace("%ss_event%", "Quest Event Results");
						break;
					
					case SevenSigns.PERIOD_SEAL_VALIDATION:
						html.replace("%ss_event%", "Seal Validation");
						break;
				}
				
				switch (SevenSigns.getInstance().getSealOwner(1))
				{
					case SevenSigns.CABAL_NULL:
						html.replace("%ss_avarice%", "Not in Possession");
						break;
					
					case SevenSigns.CABAL_DAWN:
						html.replace("%ss_avarice%", "Lords of Dawn");
						break;
					
					case SevenSigns.CABAL_DUSK:
						html.replace("%ss_avarice%", "Revolutionaries of Dusk");
						break;
				}
				
				switch (SevenSigns.getInstance().getSealOwner(2))
				{
					case SevenSigns.CABAL_NULL:
						html.replace("%ss_gnosis%", "Not in Possession");
						break;
					
					case SevenSigns.CABAL_DAWN:
						html.replace("%ss_gnosis%", "Lords of Dawn");
						break;
					
					case SevenSigns.CABAL_DUSK:
						html.replace("%ss_gnosis%", "Revolutionaries of Dusk");
						break;
				}
				
				switch (SevenSigns.getInstance().getSealOwner(3))
				{
					case SevenSigns.CABAL_NULL:
						html.replace("%ss_strife%", "Not in Possession");
						break;
					
					case SevenSigns.CABAL_DAWN:
						html.replace("%ss_strife%", "Lords of Dawn");
						break;
					
					case SevenSigns.CABAL_DUSK:
						html.replace("%ss_strife%", "Revolutionaries of Dusk");
						break;
				}
				player.sendPacket(html);
			}
		}
		else if (actualCommand.equalsIgnoreCase("items"))
		{
			if (!validatePrivileges(player, L2Clan.CP_CS_USE_FUNCTIONS))
				return;
			
			if (val.isEmpty())
				return;
			
			player.tempInventoryDisable();
			
			showBuyWindow(player, Integer.parseInt(val + "1"));
		}
		else if (actualCommand.equalsIgnoreCase("manage_siege_defender"))
		{
			if (!validatePrivileges(player, L2Clan.CP_CS_MANAGE_SIEGE))
				return;
			
			castle.getSiege().listRegisterClan(player);
		}
		else if (actualCommand.equalsIgnoreCase("manage_vault"))
		{
			if (!validatePrivileges(player, L2Clan.CP_CS_TAXES))
				return;
			
			String filename = "data/html/chamberlain/vault.htm";
			int amount = 0;
			
			if (val.equalsIgnoreCase("deposit"))
			{
				try
				{
					amount = Integer.parseInt(st.nextToken());
				}
				catch (NoSuchElementException e)
				{
				}
				
				if (amount > 0 && castle.getTreasury() + amount < Integer.MAX_VALUE)
				{
					if (player.reduceAdena("Castle", amount, this, true))
						castle.addToTreasuryNoTax(amount);
					else
						sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
				}
			}
			else if (val.equalsIgnoreCase("withdraw"))
			{
				try
				{
					amount = Integer.parseInt(st.nextToken());
				}
				catch (NoSuchElementException e)
				{
				}
				
				if (amount > 0)
				{
					if (castle.getTreasury() < amount)
						filename = "data/html/chamberlain/vault-no.htm";
					else
					{
						if (castle.addToTreasuryNoTax((-1) * amount))
							player.addAdena("Castle", amount, this, true);
					}
				}
			}
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(filename);
			html.replace("%objectId%", String.valueOf(getObjectId()));
			html.replace("%tax_income%", Util.formatAdena(castle.getTreasury()));
			html.replace("%withdraw_amount%", Util.formatAdena(amount));
			player.sendPacket(html);
		}
		else if (actualCommand.equalsIgnoreCase("operate_door")) // door control
		{
			if (!validatePrivileges(player, L2Clan.CP_CS_OPEN_DOOR))
				return;
			
			if (val.isEmpty())
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile("data/html/chamberlain/" + getTemplate().getNpcId() + "-d.htm");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
				return;
			}
			
			boolean open = (Integer.parseInt(val) == 1);
			while (st.hasMoreTokens())
				castle.openCloseDoor(player, Integer.parseInt(st.nextToken()), open);
			
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile((open) ? "data/html/chamberlain/doors-open.htm" : "data/html/chamberlain/doors-close.htm");
			html.replace("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(html);
			
		}
		else if (actualCommand.equalsIgnoreCase("tax_set")) // tax rates control
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			
			if (!validatePrivileges(player, L2Clan.CP_CS_TAXES))
				html.setFile("data/html/chamberlain/tax.htm");
			else
			{
				if (!val.isEmpty())
					castle.setTaxPercent(player, Integer.parseInt(val));
				
				html.setFile("data/html/chamberlain/tax-adjust.htm");
			}
			
			html.replace("%objectId%", String.valueOf(getObjectId()));
			html.replace("%tax%", String.valueOf(castle.getTaxPercent()));
			player.sendPacket(html);
		}
		else if (actualCommand.equalsIgnoreCase("manor"))
		{
			String filename = "";
			if (CastleManorManager.getInstance().isDisabled())
				filename = "data/html/npcdefault.htm";
			else
			{
				int cmd = Integer.parseInt(val);
				switch (cmd)
				{
					case 0:
						filename = "data/html/chamberlain/manor/manor.htm";
						break;
					
					// TODO: correct in html's to 1
					case 4:
						filename = "data/html/chamberlain/manor/manor_help00" + st.nextToken() + ".htm";
						break;
					
					default:
						filename = "data/html/chamberlain/no.htm";
						break;
				}
			}
			
			if (filename.length() != 0)
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(filename);
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
			}
		}
		else if (command.startsWith("manor_menu_select"))
		{
			if ((player.getClanPrivileges() & L2Clan.CP_CS_MANOR_ADMIN) != L2Clan.CP_CS_MANOR_ADMIN) 
			{ 
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId()); 
				html.setFile("data/html/chamberlain/chamberlain-noprivs.htm"); 
				player.sendPacket(html); 
				return; 
			}
			
			if (CastleManorManager.getInstance().isUnderMaintenance())
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				player.sendPacket(new SystemMessage(SystemMessageId.THE_MANOR_SYSTEM_IS_CURRENTLY_UNDER_MAINTENANCE));
				return;
			}
			
			String params = command.substring(command.indexOf("?") + 1);
			StringTokenizer str = new StringTokenizer(params, "&");
			int ask = Integer.parseInt(str.nextToken().split("=")[1]);
			int state = Integer.parseInt(str.nextToken().split("=")[1]);
			int time = Integer.parseInt(str.nextToken().split("=")[1]);
			
			int castleId;
			if (state == -1) // info for current manor
				castleId = castle.getCastleId();
			else
				// info for requested manor
				castleId = state;
			
			switch (ask)
			{
				case 3: // Current seeds (Manor info)
					if (time == 1 && !CastleManager.getInstance().getCastleById(castleId).isNextPeriodApproved())
						player.sendPacket(new ExShowSeedInfo(castleId, null));
					else
						player.sendPacket(new ExShowSeedInfo(castleId, CastleManager.getInstance().getCastleById(castleId).getSeedProduction(time)));
					break;
				
				case 4: // Current crops (Manor info)
					if (time == 1 && !CastleManager.getInstance().getCastleById(castleId).isNextPeriodApproved())
						player.sendPacket(new ExShowCropInfo(castleId, null));
					else
						player.sendPacket(new ExShowCropInfo(castleId, CastleManager.getInstance().getCastleById(castleId).getCropProcure(time)));
					break;
				
				case 5: // Basic info (Manor info)
					player.sendPacket(new ExShowManorDefaultInfo());
					break;
				
				case 7: // Edit seed setup
					if (castle.isNextPeriodApproved())
						player.sendPacket(new SystemMessage(SystemMessageId.A_MANOR_CANNOT_BE_SET_UP_BETWEEN_6_AM_AND_8_PM));
					else
						player.sendPacket(new ExShowSeedSetting(castle.getCastleId()));
					break;
				
				case 8: // Edit crop setup
					if (castle.isNextPeriodApproved())
						player.sendPacket(new SystemMessage(SystemMessageId.A_MANOR_CANNOT_BE_SET_UP_BETWEEN_6_AM_AND_8_PM));
					else
						player.sendPacket(new ExShowCropSetting(castle.getCastleId()));
					break;
			}
		}
		else if (actualCommand.equalsIgnoreCase("siege_change")) // set siege time
		{
			if (!validatePrivileges(player, L2Clan.CP_CS_MANAGE_SIEGE))
				return;
			
			if (castle.getSiege().getTimeRegistrationOverDate().getTimeInMillis() < Calendar.getInstance().getTimeInMillis())
				sendFileMessage(player, "data/html/chamberlain/siegetime1.htm");
			else if (castle.getSiege().getIsTimeRegistrationOver())
				sendFileMessage(player, "data/html/chamberlain/siegetime2.htm");
			else
				sendFileMessage(player, "data/html/chamberlain/siegetime3.htm");
		}
		else if (actualCommand.equalsIgnoreCase("siege_time_set")) // set preDay
		{
			switch (Integer.parseInt(val))
			{
				case 1:
					_preHour = Integer.parseInt(st.nextToken());
					break;
				
				default:
					break;
			}
			
			if (_preHour != 6)
			{
				castle.getSiegeDate().set(Calendar.HOUR_OF_DAY, _preHour + 12);
				
				// now store the changed time and finished next Siege Time registration
				castle.getSiege().endTimeRegistration(false);
				castle.getSiege().setNextSiegeDate();
				sendFileMessage(player, "data/html/chamberlain/siegetime8.htm");
				return;
			}
			
			sendFileMessage(player, "data/html/chamberlain/siegetime6.htm");
		}
		else if (actualCommand.equals("give_crown"))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			
			if (cond == COND_OWNER)
			{
				if (player.getInventory().getItemByItemId(6841) == null)
				{
					player.addItem("Crown", 6841, 1, player, true);
					player.getInventory().updateDatabase();
					/*player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1).addItemName(6841));*/
					
					html.setFile("data/html/chamberlain/gavecrown.htm");
					html.replace("%CharName%", String.valueOf(player.getName()));
					html.replace("%FeudName%", String.valueOf(castle.getName()));
				}
				else
					html.setFile("data/html/chamberlain/hascrown.htm");
			}
			else
				html.setFile("data/html/chamberlain/noprivs.htm");
			
			player.sendPacket(html);
			return;
		}
		else if (actualCommand.equalsIgnoreCase("castle_devices"))
		{
			if (!validatePrivileges(player, L2Clan.CP_CS_SET_FUNCTIONS))
				return;
			
			sendFileMessage(player, "data/html/chamberlain/devices.htm");
		}
		else if (actualCommand.equalsIgnoreCase("doors_update"))
		{
			if (!validatePrivileges(player, L2Clan.CP_CS_SET_FUNCTIONS))
				return;
			
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			if (val.isEmpty())
			{
				html.setFile("data/html/chamberlain/" + getTemplate().getNpcId() + "-gu.htm");
				html.replace("%objectId%", String.valueOf(getObjectId()));
			}
			else
			{
				html.setFile("data/html/chamberlain/doors-update.htm");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				html.replace("%id%", val);
				html.replace("%type%", st.nextToken());
			}
			player.sendPacket(html);
		}
		else if (actualCommand.equalsIgnoreCase("doors_choose_upgrade"))
		{
			if (!validatePrivileges(player, L2Clan.CP_CS_SET_FUNCTIONS))
				return;
			
			int id = Integer.parseInt(val);
			int type = Integer.parseInt(st.nextToken());
			int level = Integer.parseInt(st.nextToken());
			
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile("data/html/chamberlain/doors-confirm.htm");
			html.replace("%objectId%", String.valueOf(getObjectId()));
			html.replace("%id%", String.valueOf(id));
			html.replace("%level%", String.valueOf(level));
			html.replace("%type%", String.valueOf(type));
			html.replace("%price%", String.valueOf(getDoorCost(type, level)));
			player.sendPacket(html);
		}
		else if (actualCommand.equalsIgnoreCase("doors_confirm_upgrade"))
		{
			if (!validatePrivileges(player, L2Clan.CP_CS_SET_FUNCTIONS))
				return;
			
			int id = Integer.parseInt(val);
			int type = Integer.parseInt(st.nextToken());
			int level = Integer.parseInt(st.nextToken());
			int price = getDoorCost(type, level);
			
			if (price == 0)
				return;
			
			if (player.getClan().getWarehouse().getAdena() < price)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ADENA_IN_CWH));
				return;
			}
			
			final int currentHpRatio = castle.getDoorUpgrade(id);
			
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			if (currentHpRatio >= level)
			{
				html.setFile("data/html/chamberlain/doors-already-updated.htm");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				html.replace("%level%", String.valueOf(currentHpRatio * 100));
				player.sendPacket(html);
				return;
			}
			
			player.getClan().getWarehouse().destroyItemByItemId("doors_upgrade", 57, price, player, null);
			castle.upgradeDoor(id, level, true);
			
			html.setFile("data/html/chamberlain/doors-success.htm");
			html.replace("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(html);
		}
		else
			super.onBypassFeedback(player, command);
	}
	
	@Override
	public void showChatWindow(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		String filename = "data/html/chamberlain/no.htm";
		
		int condition = validateCondition(player);
		if (condition > COND_ALL_FALSE)
		{
			if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
				filename = "data/html/chamberlain/busy.htm";
			else if (condition == COND_OWNER)
				filename = "data/html/chamberlain/chamberlain.htm";
		}
		
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}
	
	protected int validateCondition(L2PcInstance player)
	{
		if (getCastle() != null && getCastle().getCastleId() > 0)
		{
			if (player.getClan() != null)
			{
				if (getCastle().getSiege().getIsInProgress())
					return COND_BUSY_BECAUSE_OF_SIEGE;
				
				if (getCastle().getOwnerId() == player.getClanId())
				{
					if (player.isClanLeader())
						return COND_OWNER;
					
					return COND_CLAN_MEMBER;
				}
			}
		}
		return COND_ALL_FALSE;
	}
	
	private boolean validatePrivileges(L2PcInstance player, int privilege)
	{
		if ((player.getClanPrivileges() & privilege) != privilege)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile("data/html/chamberlain/noprivs.htm");
			player.sendPacket(html);
			return false;
		}
		return true;
	}
	
	private void sendFileMessage(L2PcInstance player, String htmlMessage)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(htmlMessage);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));
		html.replace("%time%", String.valueOf(getCastle().getSiegeDate().getTime()));
		player.sendPacket(html);
	}
	
	/**
	 * Retrieve the price of the door, following its type, required level of upgrade and current Seven Signs state.
	 * @param type The type of doors (1: normal gates, 2: metallic gates, 3: walls)
	 * @param level The required level of upgrade (x2, x3 or x5 HPs)
	 * @return The price modified by Seal of Strife state (-20% if Dawn is winning, x3 if Dusk is winning).
	 */
	private static int getDoorCost(int type, int level)
	{
		int price = 0;
		
		switch (type)
		{
			case 1:
				switch (level)
				{
					case 2:
						price = 300000;
						break;
					case 3:
						price = 400000;
						break;
					case 5:
						price = 500000;
						break;
				}
				break;
			
			case 2:
				switch (level)
				{
					case 2:
						price = 750000;
						break;
					case 3:
						price = 900000;
						break;
					case 5:
						price = 1000000;
						break;
				}
				break;
			
			case 3:
				switch (level)
				{
					case 2:
						price = 1600000;
						break;
					case 3:
						price = 1800000;
						break;
					case 5:
						price = 2000000;
						break;
				}
				break;
		}
		
		switch (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
		{
			case SevenSigns.CABAL_DUSK:
				price = price * 3;
				break;
			
			case SevenSigns.CABAL_DAWN:
				price = price * 80 / 100;
				break;
		}
		
		return price;
	}
}