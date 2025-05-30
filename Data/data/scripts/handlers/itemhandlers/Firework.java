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
 * [URL]http://www.gnu.org/copyleft/gpl.html[/URL]
 */
package handlers.itemhandlers;

import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.handler.IItemHandler;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.MagicSkillUser;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.util.FloodProtector;

public class Firework implements IItemHandler
{
	private static final int[] ITEM_IDS =
	{
			6403, 6406, 6407
	};

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(!(playable instanceof L2PcInstance))
		{
			return;
		}

		L2PcInstance activeChar = (L2PcInstance) playable;
		int itemId = item.getItemId();

		if(!FloodProtector.getInstance().tryPerformAction(activeChar.getObjectId(), FloodProtector.PROTECTED_FIREWORK))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addItemName(itemId));
			return;
		}

		if(activeChar.isCastingNow())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(activeChar.isInOlympiadMode())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
			return;
		}

		if(activeChar.inObserverMode())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(activeChar.isSitting())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(activeChar.isConfused())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(activeChar.isStunned())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(activeChar.isDead())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(activeChar.isAlikeDead())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(itemId == 6403)
		{
			MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2023, 1, 1, 0);
			activeChar.sendPacket(MSU);
			activeChar.broadcastPacket(MSU);
			MSU = null;
			useFw(activeChar, 2023, 1);
			playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
		}
		else if(itemId == 6406)
		{
			MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2024, 1, 1, 0);
			activeChar.sendPacket(MSU);
			activeChar.broadcastPacket(MSU);
			MSU = null;
			useFw(activeChar, 2024, 1);
			playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
		}
		else if(itemId == 6407)
		{
			MagicSkillUser MSU = new MagicSkillUser(playable, activeChar, 2025, 1, 1, 0);
			activeChar.sendPacket(MSU);
			activeChar.broadcastPacket(MSU);
			MSU = null;
			useFw(activeChar, 2025, 1);
			playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
		}

		activeChar = null;
	}

	public void useFw(L2PcInstance activeChar, int magicId, int level)
	{
		L2Skill skill = SkillTable.getInstance().getInfo(magicId, level);
		if(skill != null)
		{
			activeChar.useMagic(skill, false, false);
		}
		skill = null;
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}

}