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

import com.src.Config;
import com.src.gameserver.Shutdown;
import com.src.gameserver.datatables.sql.HennaTreeTable;
import com.src.gameserver.datatables.xml.HennaTable;
import com.src.gameserver.model.actor.instance.L2HennaInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.item.L2Henna;
import com.src.gameserver.util.Util;
import com.src.util.StringUtil;

public final class RequestHennaEquip extends L2GameClientPacket
{
	private static final String _C__BC_RequestHennaEquip = "[C] bc RequestHennaEquip";

	private int _symbolId;

	@Override
	protected void readImpl()
	{
		_symbolId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if(activeChar == null)
			return;

		if(Shutdown.getCounterInstance() != null)
		{
			showMessageErrorRestart(activeChar);
			return;
		}

		L2Henna template = HennaTable.getInstance().getTemplate(_symbolId);

		if(template == null)
			return;

		L2HennaInstance henna = new L2HennaInstance(template);
		int _count = 0;

		boolean cheater = true;

		for(L2HennaInstance h : HennaTreeTable.getInstance().getAvailableHenna(activeChar.getClassId()))
		{
			if(h.getSymbolId() == henna.getSymbolId())
			{
				cheater = false;
				break;
			}
		}
		if(activeChar.isCastingNow() || activeChar.isFlying() || activeChar.isMounted() || activeChar.isMuted() || activeChar.isInCombat() || activeChar.getActiveEnchantItem() != null || activeChar.getActiveTradeList() != null || activeChar.getActiveWarehouse() != null)
		{
			showMessageErrorEquip(activeChar);
			return;
		}
		if(activeChar.getInventory() != null && activeChar.getInventory().getItemByItemId(henna.getItemIdDye()) != null)
		{
			{
				_count = activeChar.getInventory().getItemByItemId(henna.getItemIdDye()).getCount();
			}

			if(activeChar.getHennaEmptySlots() == 0)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.SYMBOLS_FULL));
			}

		}
		if(!cheater && _count >= henna.getAmountDyeRequire() && activeChar.getAdena() >= henna.getPrice() && activeChar.addHenna(henna))
		{
			activeChar.destroyItemByItemId("Henna", henna.getItemIdDye(), henna.getAmountDyeRequire(), activeChar, true);

			//update inventory
			InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(activeChar.getInventory().getAdenaInstance());
			activeChar.sendPacket(iu);

			activeChar.sendPacket(new SystemMessage(SystemMessageId.SYMBOL_ADDED));
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_DRAW_SYMBOL));

			if(!activeChar.isGM() && cheater)
			{
				Util.handleIllegalPlayerAction(activeChar, "Exploit attempt: Character " + activeChar.getName() + " of account " + activeChar.getAccountName() + " tryed to add a forbidden henna.", Config.DEFAULT_PUNISH);
			}
		}
	}

	private void showMessageErrorEquip(L2PcInstance activeChar)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		final StringBuilder strBuffer = StringUtil.startAppend(3500, "<html><title>Henna</title><body><center>");
		{
			strBuffer.append("<img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32 align=left>");
			strBuffer.append("<font color=\"LEVEL\">%charname%</font> I am sorry but you can't <br>" + "equip your dyes right now!:<br>");
			strBuffer.append("<table width=300>");
			strBuffer.append("</table><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32 align=left>");
		}
		strBuffer.append("</center></body></html>");
		html.setHtml(strBuffer.toString());
		html.replace("%charname%", activeChar.getName());
		activeChar.sendPacket(html);
	}

	private void showMessageErrorRestart(L2PcInstance activeChar)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		final StringBuilder strBuffer = StringUtil.startAppend(3500, "<html><title>Henna</title><body><center>");
		{
			strBuffer.append("<img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32 align=left>");
			strBuffer.append("<font color=\"LEVEL\">%charname%</font> I am sorry but you can't <br>" + "equip when restarting / shutdown of the server!:<br>");
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
		return _C__BC_RequestHennaEquip;
	}
}
