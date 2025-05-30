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

import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.SendTradeDone;
import com.src.gameserver.network.serverpackets.SystemMessage;

public final class AnswerTradeRequest extends L2GameClientPacket
{
	private static final String _C__40_ANSWERTRADEREQUEST = "[C] 40 AnswerTradeRequest";
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

		if(!player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Unsufficient privileges.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		L2PcInstance partner = player.getActiveRequester();

		if(partner == null || L2World.getInstance().findObject(partner.getObjectId()) == null)
		{
			player.sendPacket(new SendTradeDone(0));
			player.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME));
			player.setActiveRequester(null);
			player.setAllowTrade(true);
			partner.setAllowTrade(true);
			return;
		}

		if(_response == 1 && !partner.isRequestExpired())
		{
			player.startTrade(partner);
			partner.setAllowTrade(true);
			player.setAllowTrade(true);
		}
		else
		{
			partner.sendPacket(new SystemMessage(SystemMessageId.S1_DENIED_TRADE_REQUEST).addString(player.getName()));
			player.setAllowTrade(true);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}

		player.setActiveRequester(null);
		partner.onTransactionResponse();
	}

	@Override
	public String getType()
	{
		return _C__40_ANSWERTRADEREQUEST;
	}

}