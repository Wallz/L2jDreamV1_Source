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

import com.src.Config;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.util.IllegalPlayerAction;
import com.src.gameserver.util.Util;

public final class RequestGetItemFromPet extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestGetItemFromPet.class.getName());

	private static final String REQUESTGETITEMFROMPET__C__8C = "[C] 8C RequestGetItemFromPet";

	private int _objectId;
	private int _amount;
	@SuppressWarnings("unused")
	private int _unknown;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_amount = readD();
		_unknown = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();

		if(player == null || player.getPet() == null || !(player.getPet() instanceof L2PetInstance))
		{
			return;
		}

		L2PetInstance pet = (L2PetInstance) player.getPet();

		if(player.getActiveEnchantItem() != null)
		{
			Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " Tried To Use Enchant Exploit , And Got Banned!", IllegalPlayerAction.PUNISH_KICKBAN);
			return;
		}

		if(_amount < 0)
		{
			player.setAccessLevel(-1);
			Util.handleIllegalPlayerAction(player, "[RequestGetItemFromPet] count < 0! ban! oid: " + _objectId + " owner: " + player.getName(), Config.DEFAULT_PUNISH);
			return;
		}
		else if(_amount == 0)
		{
			return;
		}

		if(player.getDistanceSq(pet) > 40000)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.TARGET_TOO_FAR));
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(pet.transferItem("Transfer", _objectId, _amount, player.getInventory(), player, pet) == null)
		{
			_log.warning("Invalid item transfer request: " + pet.getName() + "(pet) --> " + player.getName());
		}

		player.sendPacket(new ItemList(player, true));
	}

	@Override
	public String getType()
	{
		return REQUESTGETITEMFROMPET__C__8C;
	}

}