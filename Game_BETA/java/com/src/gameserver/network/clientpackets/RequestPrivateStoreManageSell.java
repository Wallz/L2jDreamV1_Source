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
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.olympiad.Olympiad;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.CreatureSay;
import com.src.gameserver.network.serverpackets.PrivateStoreManageListSell;
import com.src.gameserver.util.Util;

public final class RequestPrivateStoreManageSell extends L2GameClientPacket
{
	private static final String _C__73_REQUESTPRIVATESTOREMANAGESELL = "[C] 73 RequestPrivateStoreManageSell";

	@Override
	protected void readImpl()
	{
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if(player == null)
		{
			return;
		}

        if(!player.isVisible() || player.isLocked())
        {
                Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " try exploit at login with privatestore!", Config.DEFAULT_PUNISH);
                _log.warn("Player " + player.getName() + " try exploit at login with privatestore!");
                return;
        }

		if(player.isAlikeDead())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(player.isInOlympiadMode() || Olympiad.getInstance().isRegistered(player))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(player.getMountType() != 0)
		{
			return;
		}

		if(player.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL || player.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL + 1 || player.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_PACKAGE_SELL)
		{
			player.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
		}

		if(player.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_NONE)
		{
			if(player.isSitting())
			{
				player.standUp();
			}

			if(Config.SELL_BY_ITEM)
			{
				CreatureSay cs11 = new CreatureSay(0, 15, "", "ATTENTION: Store System is not based on Adena, be careful!");
				player.sendPacket(cs11);
			}

			player.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_SELL + 1);
			player.sendPacket(new PrivateStoreManageListSell(player));
		}
	}

	@Override
	public String getType()
	{
		return _C__73_REQUESTPRIVATESTOREMANAGESELL;
	}

}