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
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.network.serverpackets.SSQStatus;

public class SevenSignsRecord implements IItemHandler
{
	private static final int[] ITEM_IDS =
	{
		5707
	};

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		L2PcInstance activeChar;

		if(playable instanceof L2PcInstance)
		{
			activeChar = (L2PcInstance) playable;
		}
		else if(playable instanceof L2PetInstance)
		{
			activeChar = ((L2PetInstance) playable).getOwner();
		}
		else
		{
			return;
		}

		SSQStatus ssqs = new SSQStatus(activeChar, 1);
		activeChar.sendPacket(ssqs);

		ssqs = null;
		activeChar = null;
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}

}