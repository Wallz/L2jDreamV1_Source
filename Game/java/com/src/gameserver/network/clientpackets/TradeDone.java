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

import com.src.gameserver.model.L2World;
import com.src.gameserver.model.TradeList;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.EnchantResult;
import com.src.gameserver.network.serverpackets.SystemMessage;

public final class TradeDone extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(TradeDone.class.getName());

	private static final String _C__17_TRADEDONE = "[C] 17 TradeDone";

	private int _response;

	@Override
	protected void readImpl()
	{
		_response = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();

		if(player == null)
		{
			return;
		}

		TradeList trade = player.getActiveTradeList();

		if(trade == null)
		{
			_log.warning("player.getTradeList == null in " + getType() + " for player " + player.getName());
			return;
		}

		if(trade.getOwner().getActiveEnchantItem() != null || trade.getPartner().getActiveEnchantItem() != null)
		{
			return;
		}

		if(trade.isLocked())
		{
			return;
		}

		if(player.isCastingNow())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(_response == 1)
		{
			final L2PcInstance owner = trade.getOwner();
			final L2PcInstance partner = trade.getPartner();
			
            if (owner == null || !owner.equals(player))
            {
                return;
            }
            
            if (partner == null || L2World.getInstance().getPlayer(partner.getObjectId()) == null)
			{
				player.cancelActiveTrade();
				player.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME));
				return;
			}

            if (owner.getActiveEnchantItem() != null)
            {
                owner.setActiveEnchantItem(null);
                owner.sendPacket(new EnchantResult(2));
                owner.sendPacket(new SystemMessage(SystemMessageId.ENCHANT_SCROLL_CANCELLED));

            }

            if (partner.getActiveEnchantItem() != null)
            {
                partner.setActiveEnchantItem(null);
                partner.sendPacket(new EnchantResult(2));
                partner.sendPacket(new SystemMessage(SystemMessageId.ENCHANT_SCROLL_CANCELLED));
            }

			if(!player.getAccessLevel().allowTransaction())
			{
				player.sendMessage("Unsufficient privileges.");
				player.cancelActiveTrade();
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			trade.confirm();
		}
		else
		{
			player.cancelActiveTrade();
		}
	}

	@Override
	public String getType()
	{
		return _C__17_TRADEDONE;
	}

}