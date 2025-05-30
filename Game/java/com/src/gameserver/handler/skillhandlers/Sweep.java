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
package com.src.gameserver.handler.skillhandlers;

import com.src.Config;
import com.src.gameserver.handler.ISkillHandler;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.skills.L2SkillType;

public class Sweep implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS = { L2SkillType.SWEEP };

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if(!(activeChar instanceof L2PcInstance))
		{
			return;
		}

		L2PcInstance player = (L2PcInstance) activeChar;
		InventoryUpdate iu = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
		boolean send = false;

		for(int index = 0; index < targets.length; index++)
		{
			if(!(targets[index] instanceof L2Attackable))
			{
				continue;
			}

			L2Attackable target = (L2Attackable) targets[index];
			L2Attackable.RewardItem[] items = null;
			boolean isSweeping = false;
			synchronized (target)
			{
				if(target.isSweepActive())
				{
					items = target.takeSweep();
					isSweeping = true;
				}
			}

			if(isSweeping)
			{
				if(items == null || items.length == 0)
				{
					continue;
				}
				for(L2Attackable.RewardItem ritem : items)
				{
					if(player != null && player.isInParty())
					{
						player.getParty().distributeItem(player, ritem, true, target);
					}
					else
					{
						L2ItemInstance item = player.getInventory().addItem("Sweep", ritem.getItemId(), ritem.getCount(), player, target);
						if(iu != null)
						{
							iu.addItem(item);
						}

						send = true;

						if(ritem.getCount() > 1)
						{
							player.sendPacket(new SystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(ritem.getItemId()).addNumber(ritem.getCount()));
						}
						else
						{
							player.sendPacket(new SystemMessage(SystemMessageId.EARNED_ITEM_S1).addItemName(ritem.getItemId()));
						}
					}
				}
			}

			target.endDecayTask();

			if(player != null && send)
			{
				if(iu != null)
				{
					player.sendPacket(iu);
				}
				else
				{
					player.sendPacket(new ItemList(player, false));
				}
			}
		}
	}

	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}