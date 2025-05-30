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
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.managers.MercTicketManager;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.siege.Castle;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class MercTicket implements IItemHandler
{
	private static final String[] MESSAGES =
	{
			"To arms !.", "I am ready to serve my lord when the time will come.", "You summon me."
	};

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		int itemId = item.getItemId();
		L2PcInstance activeChar = (L2PcInstance) playable;
		Castle castle = CastleManager.getInstance().getCastle(activeChar);
		int castleId = -1;

		if(castle != null)
		{
			castleId = castle.getCastleId();
		}

		if(MercTicketManager.getInstance().getTicketCastleId(itemId) != castleId)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.MERCENARIES_CANNOT_BE_POSITIONED_HERE));
			return;
		}

		if(!activeChar.isCastleLord(castleId))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_AUTHORITY_TO_POSITION_MERCENARIES));
			return;
		}

		if(castle.getSiege().getIsInProgress())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE));
			return;
		}

		if(MercTicketManager.getInstance().isAtCasleLimit(item.getItemId()) || MercTicketManager.getInstance().isAtTypeLimit(item.getItemId()))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_MERCENARY_CANNOT_BE_POSITIONED_ANYMORE));
			return;
		}

		if(MercTicketManager.getInstance().isTooCloseToAnotherTicket(activeChar.getX(), activeChar.getY(), activeChar.getZ()))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.POSITIONING_CANNOT_BE_DONE_BECAUSE_DISTANCE_BETWEEN_MERCENARIES_TOO_SHORT));
			return;
		}

		MercTicketManager.getInstance().addTicket(item.getItemId(), activeChar, MESSAGES);
		activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);

		activeChar = null;
		castle = null;
	}

	public int[] getItemIds()
	{
		return MercTicketManager.getInstance().getItemIds();
	}

}