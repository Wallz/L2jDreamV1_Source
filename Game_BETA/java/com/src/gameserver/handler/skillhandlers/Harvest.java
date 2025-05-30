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
import com.src.gameserver.model.actor.instance.L2MonsterInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.skills.L2SkillType;
import com.src.util.random.Rnd;

public class Harvest implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS = { L2SkillType.HARVEST };

	private L2PcInstance _activeChar;
	private L2MonsterInstance _target;

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if(!(activeChar instanceof L2PcInstance))
		{
			return;
		}

		_activeChar = (L2PcInstance) activeChar;

		L2Object[] targetList = skill.getTargetList(activeChar);

		InventoryUpdate iu = Config.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();

		if(targetList == null)
		{
			return;
		}

		for(int index = 0; index < targetList.length; index++)
		{
			if(!(targetList[index] instanceof L2MonsterInstance))
			{
				continue;
			}

			_target = (L2MonsterInstance) targetList[index];

			if(_activeChar != _target.getSeeder())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_HARVEST));
				continue;
			}

			boolean send = false;
			int total = 0;
			int cropId = 0;

			if(_target.isSeeded())
			{
				if(calcSuccess())
				{
					L2Attackable.RewardItem[] items = _target.takeHarvest();
					if(items != null && items.length > 0)
					{
						for(L2Attackable.RewardItem ritem : items)
						{
							cropId = ritem.getItemId();
							if(_activeChar.isInParty())
							{
								_activeChar.getParty().distributeItem(_activeChar, ritem, true, _target);
							}
							else
							{
								L2ItemInstance item = _activeChar.getInventory().addItem("Manor", ritem.getItemId(), ritem.getCount(), _activeChar, _target);
								if(iu != null)
								{
									iu.addItem(item);
								}
								send = true;
								total += ritem.getCount();
								item = null;
							}
						}
						if(send)
						{
							_activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2).addNumber(total).addItemName(cropId));

							if(_activeChar.getParty() != null)
							{
								_activeChar.getParty().broadcastToPartyMembers(new SystemMessage(SystemMessageId.S1_HARVESTED_S3_S2S).addString(_activeChar.getName()).addNumber(total).addItemName(cropId));
							}

							if(iu != null)
							{
								_activeChar.sendPacket(iu);
							}
							else
							{
								_activeChar.sendPacket(new ItemList(_activeChar, false));
							}
						}
					}
				}
				else
				{
					_activeChar.sendPacket(new SystemMessage(SystemMessageId.THE_HARVEST_HAS_FAILED));
				}
			}
			else
			{
				_activeChar.sendPacket(new SystemMessage(SystemMessageId.THE_HARVEST_FAILED_BECAUSE_THE_SEED_WAS_NOT_SOWN));
			}
		}
	}

	private boolean calcSuccess()
	{
		int basicSuccess = 100;
		int levelPlayer = _activeChar.getLevel();
		int levelTarget = _target.getLevel();

		int diff = (levelPlayer - levelTarget);
		if(diff < 0)
		{
			diff = -diff;
		}

		if(diff > 5)
		{
			basicSuccess -= (diff - 5) * 5;
		}

		if(basicSuccess < 1)
		{
			basicSuccess = 1;
		}

		int rate = Rnd.nextInt(99);

		if(rate < basicSuccess)
		{
			return true;
		}

		return false;
	}

	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}