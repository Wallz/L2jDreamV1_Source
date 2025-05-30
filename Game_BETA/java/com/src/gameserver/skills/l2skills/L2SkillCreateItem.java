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
package com.src.gameserver.skills.l2skills;

import com.src.gameserver.idfactory.IdFactory;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.StatsSet;
import com.src.util.random.Rnd;

public class L2SkillCreateItem extends L2Skill
{
	private final int[] _createItemId;
	private final int _createItemCount;
	private final int _randomCount;

	public L2SkillCreateItem(StatsSet set)
	{
		super(set);
		_createItemId = set.getIntegerArray("create_item_id");
		_createItemCount = set.getInteger("create_item_count", 0);
		_randomCount = set.getInteger("random_count", 1);
	}

	@Override
	public void useSkill(L2Character activeChar, L2Object[] targets)
	{
		if(activeChar.isAlikeDead())
		{
			return;
		}

		if(_createItemId == null || _createItemCount == 0)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE));
			return;
		}

		L2PcInstance player = (L2PcInstance) activeChar;
		if(activeChar instanceof L2PcInstance)
		{
			int rnd = Rnd.nextInt(_randomCount) + 1;
			int count = _createItemCount * rnd;
			int rndid = Rnd.nextInt(_createItemId.length);
			giveItems(player, _createItemId[rndid], count);
		}
	}

	public void giveItems(L2PcInstance activeChar, int itemId, int count)
	{
		L2ItemInstance item = new L2ItemInstance(IdFactory.getInstance().getNextId(), itemId);

		item.setCount(count);
		activeChar.getInventory().addItem("Skill", item, activeChar, activeChar);

		if(count > 1)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(item.getItemId()).addNumber(count));
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.EARNED_ITEM_S1).addItemName(item.getItemId()));
		}

		ItemList il = new ItemList(activeChar, false);
		activeChar.sendPacket(il);
	}

}