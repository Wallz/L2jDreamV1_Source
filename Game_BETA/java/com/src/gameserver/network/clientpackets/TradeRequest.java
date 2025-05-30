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
import com.src.gameserver.model.BlockList;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.SendTradeRequest;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.util.Util;

public final class TradeRequest extends L2GameClientPacket
{
	private static final String TRADEREQUEST__C__15 = "[C] 15 TradeRequest";

	private int _objectId;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();

		if(player == null)
		{
			return;
		}

		if(!player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Transactions are disable for your Access Level");
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		L2Object target = L2World.getInstance().findObject(_objectId);
		if(target == null || !player.getKnownList().knowsObject(target) || !(target instanceof L2PcInstance) || target.getObjectId() == player.getObjectId())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
			return;
		}

		L2PcInstance partner = (L2PcInstance) target;

		if(partner.isInOlympiadMode() || player.isInOlympiadMode())
		{
			player.sendMessage("You or your target can't request trade in Olympiad mode");
			return;
		}
		
		if (partner.isAlikeDead() || partner.isDead())
		{
            player.sendMessage("You or your target cant request trade in Dead player");
            return;
        }

		if(partner.isStunned() || partner.isConfused() || partner.isCastingNow() || partner.isInDuel() || partner.isImobilised() || partner.isParalyzed() || partner.inObserverMode() || partner.isAttackingNow())
		{
			player.sendMessage("You can't request a trade at this time.");
			return;
		}

		if(partner.getActiveEnchantItem() != null)
		{
			player.sendMessage("You can't request a trade when partner enchanting.");
			return;
		}

		if(partner.getPvpFlag() > 0)
		{
			player.sendMessage("You can't Request a Trade when target flagged.");
			return;
		}

		if(player.getDistanceSq(partner) > 22500)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.TARGET_TOO_FAR));
			return;
		}

		if(!Config.KARMA_PLAYER_CAN_TRADE && (player.getKarma() > 0 || partner.getKarma() > 0))
		{
			player.sendMessage("Chaotic players can't use trade.");
			return;
		}

		if(player.getPrivateStoreType() != 0 || partner.getPrivateStoreType() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_TRADE_DISCARD_DROP_ITEM_WHILE_IN_SHOPMODE));
			return;
		}

		if(player.isProcessingTransaction())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.ALREADY_TRADING));
			return;
		}

		if(partner.isProcessingRequest() || partner.isProcessingTransaction())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER).addString(partner.getName()));
			return;
		}

		if(Util.calculateDistance(player, partner, true) > 150)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.TARGET_TOO_FAR));
			return;
		}

		if(partner.getAllowTrade() == false)
		{
			player.sendMessage("Target is not allowed to receive more than one trade request at the same time.");
			return;
		}

		if (partner.getTradeRefusal()) 
		{ 
			player.sendMessage("Target is in trade refusal mode."); 
			return; 
		}
		
        if (BlockList.isBlocked(partner, player))
        {
            SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_ADDED_YOU_TO_IGNORE_LIST).addString(partner.getName());
            player.sendPacket(sm);
            return;
        }
        
		partner.setAllowTrade(false);
		player.setAllowTrade(false);
		player.onTransactionRequest(partner);
		partner.sendPacket(new SendTradeRequest(player.getObjectId()));
		player.sendPacket(new SystemMessage(SystemMessageId.REQUEST_S1_FOR_TRADE).addString(partner.getName()));
	}

	@Override
	public String getType()
	{
		return TRADEREQUEST__C__15;
	}

}