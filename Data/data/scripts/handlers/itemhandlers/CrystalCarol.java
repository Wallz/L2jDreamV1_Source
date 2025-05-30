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
import com.src.gameserver.network.serverpackets.MagicSkillUser;

public class CrystalCarol implements IItemHandler
{
	private static final int[] ITEM_IDS =
	{
			5562, 5563, 5564, 5565, 5566, 5583, 5584, 5585, 5586, 5587, 4411, 4412, 4413, 4414, 4415, 4416, 4417, 5010, 6903, 7061, 7062, 8555
	};

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(!(playable instanceof L2PcInstance))
		{
			return;
		}

		L2PcInstance activeChar = (L2PcInstance) playable;
		int itemId = item.getItemId();

		if(itemId == 5562)
		{
			MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2140, 1, 1, 0);
			activeChar.broadcastPacket(MSU);
		}
		else if(itemId == 5563)
		{
			MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2141, 1, 1, 0);
			activeChar.broadcastPacket(MSU);
		}
		else if(itemId == 5564)
		{
			MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2142, 1, 1, 0);
			activeChar.broadcastPacket(MSU);
		}
		else if(itemId == 5565)
		{
			MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2143, 1, 1, 0);
			activeChar.broadcastPacket(MSU);
		}
		else if(itemId == 5566)
		{
			MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2144, 1, 1, 0);
			activeChar.broadcastPacket(MSU);
		}
		else if(itemId == 5583)
		{
			MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2145, 1, 1, 0);
			activeChar.broadcastPacket(MSU);
		}
		else if(itemId == 5584)
		{
			MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2146, 1, 1, 0);
			activeChar.broadcastPacket(MSU);
		}
		else if(itemId == 5585)
		{
			MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2147, 1, 1, 0);
			activeChar.broadcastPacket(MSU);
		}
		else if(itemId == 5586)
		{
			MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2148, 1, 1, 0);
			activeChar.broadcastPacket(MSU);
		}
		else if(itemId == 5587)
		{
			MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2149, 1, 1, 0);
			activeChar.broadcastPacket(MSU);
		}
		else if(itemId == 4411)
		{
			MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2069, 1, 1, 0);
			activeChar.broadcastPacket(MSU);
		}
		else if(itemId == 4412)
		{
			MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2068, 1, 1, 0);
			activeChar.broadcastPacket(MSU);
		}
		else if(itemId == 4413)
		{
			MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2070, 1, 1, 0);
			activeChar.broadcastPacket(MSU);
		}
		else if(itemId == 4414)
		{
			MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2072, 1, 1, 0);
			activeChar.broadcastPacket(MSU);
		}
		else if(itemId == 4415)
		{
			MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2071, 1, 1, 0);
			activeChar.broadcastPacket(MSU);
		}
		else if(itemId == 4416)
		{
			MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2073, 1, 1, 0);
			activeChar.broadcastPacket(MSU);
		}
		else if(itemId == 4417)
		{
			MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2067, 1, 1, 0);
			activeChar.broadcastPacket(MSU);
		}
		else if(itemId == 5010)
		{
			MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2066, 1, 1, 0);
			activeChar.broadcastPacket(MSU);
		}
		else if(itemId == 6903)
		{
			MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2187, 1, 1, 0);
			activeChar.broadcastPacket(MSU);
		}
		else if(itemId == 7061)
		{
			MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2073, 1, 1, 0);
			activeChar.broadcastPacket(MSU);
		}
		else if(itemId == 7062)
		{
			MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2230, 1, 1, 0);
			activeChar.broadcastPacket(MSU);
		}
		else if(itemId == 8555)
		{
			MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2272, 1, 1, 0);
			activeChar.broadcastPacket(MSU);
		}
		activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);

		activeChar = null;
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}

}