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
package handlers.itemhandlers;

import com.src.gameserver.handler.IItemHandler;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.Dice;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.util.Broadcast;
import com.src.gameserver.util.FloodProtector;
import com.src.util.random.Rnd;

public class RollingDice implements IItemHandler
{
	private static final int[] ITEM_IDS =
	{
			4625, 4626, 4627, 4628
	};

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(!(playable instanceof L2PcInstance))
		{
			return;
		}

		L2PcInstance activeChar = (L2PcInstance) playable;
		int itemId = item.getItemId();

		if(activeChar.isInOlympiadMode())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
			return;
		}

		if(itemId == 4625 || itemId == 4626 || itemId == 4627 || itemId == 4628)
		{
			int number = rollDice(activeChar);
			if(number == 0)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_MAY_NOT_THROW_THE_DICE_AT_THIS_TIME_TRY_AGAIN_LATER));
				return;
			}

			Dice d = new Dice(activeChar.getObjectId(), item.getItemId(), number, activeChar.getX() - 30, activeChar.getY() - 30, activeChar.getZ());
			Broadcast.toSelfAndKnownPlayers(activeChar, d);
			d = null;

			SystemMessage sm = new SystemMessage(SystemMessageId.S1_ROLLED_S2).addString(activeChar.getName()).addNumber(number);
			activeChar.sendPacket(sm);

			if(activeChar.isInsideZone(L2Character.ZONE_PEACE))
			{
				Broadcast.toKnownPlayers(activeChar, sm);
			}
			else if(activeChar.isInParty())
			{
				activeChar.getParty().broadcastToPartyMembers(activeChar, sm);
			}
			sm = null;
		}

		activeChar = null;
	}

	private int rollDice(L2PcInstance player)
	{
		if(!FloodProtector.getInstance().tryPerformAction(player.getObjectId(), FloodProtector.PROTECTED_ROLLDICE))
		{
			return 0;
		}

		return Rnd.get(1, 6);
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}

}