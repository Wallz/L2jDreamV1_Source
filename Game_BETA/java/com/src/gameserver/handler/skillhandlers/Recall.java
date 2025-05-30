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

import javolution.util.FastList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.datatables.xml.MapRegionTable;
import com.src.gameserver.handler.ISkillHandler;
import com.src.gameserver.managers.GrandBossManager;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2GrandBossInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2RaidBossInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.skills.L2SkillType;

public class Recall implements ISkillHandler
{
	private final static Log _log = LogFactory.getLog(Recall.class);

	private static final L2SkillType[] SKILL_IDS = { L2SkillType.RECALL };

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		try
		{
			if(activeChar instanceof L2PcInstance)
			{
				if(((L2PcInstance) activeChar).isInOlympiadMode())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
					return;
				}

				if(activeChar.isInsideZone(L2Character.ZONE_SIEGE))
				{
					((L2PcInstance) activeChar).sendMessage("You cannot summon in siege zone.");
					return;
				}

				if(activeChar.isInsideZone(L2Character.ZONE_PVP))
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_SUMMON_IN_COMBAT));
					return;
				}

				if(GrandBossManager.getInstance().getZone(activeChar) != null && !((L2PcInstance) activeChar).isGM())
				{
					((L2PcInstance) activeChar).sendMessage("You may not use Summon Friend Skill inside a Boss Zone.");
					return;
				}

				FastList<L2Object> objects = L2World.getInstance().getVisibleObjects(activeChar, 5000);
				if(objects != null)
				{
					for(L2Object object : objects)
					{
						if(skill.getId() != 1050)
						{
							if(object instanceof L2RaidBossInstance || object instanceof L2GrandBossInstance)
							{
								activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION));
								return;
							}
						}
					}
				}
			}

			for(int index = 0; index < targets.length; index++)
			{
				if(!(targets[index] instanceof L2Character))
				{
					continue;
				}

				L2Character target = (L2Character) targets[index];

				if(target instanceof L2PcInstance)
				{
					L2PcInstance targetChar = (L2PcInstance) target;

					if(targetChar.isAio())
					{
						activeChar.sendPacket(SystemMessage.sendString("You cannot summon Aio Buffers."));
						continue;
					}
					
					if(targetChar.isFestivalParticipant())
					{
						targetChar.sendPacket(SystemMessage.sendString("You can't use escape skill in a festival."));
						continue;
					}

					if(targetChar.isInEvent())
					{
						targetChar.sendMessage("You can't use escape skill in Event.");
						continue;
					}

					if(targetChar.isInJail())
					{
						targetChar.sendPacket(SystemMessage.sendString("You can't escape from jail."));
						continue;
					}

					if(targetChar.isInDuel())
					{
						targetChar.sendPacket(SystemMessage.sendString("You can't use escape skills during a duel."));
						continue;
					}

					if(targetChar.isAlikeDead())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_IS_DEAD_AT_THE_MOMENT_AND_CANNOT_BE_SUMMONED).addString(targetChar.getName()));
						continue;
					}

					if(targetChar.isInStoreMode())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_CURRENTLY_TRADING_OR_OPERATING_PRIVATE_STORE_AND_CANNOT_BE_SUMMONED).addString(targetChar.getName()));
						continue;
					}

					if(targetChar.isRooted() || targetChar.isInCombat())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_IS_ENGAGED_IN_COMBAT_AND_CANNOT_BE_SUMMONED).addString(targetChar.getName()));
						continue;
					}

					if(GrandBossManager.getInstance().getZone(targetChar) != null && !targetChar.isGM())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
						continue;
					}

					if(targetChar.isInOlympiadMode())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_SUMMON_PLAYERS_WHO_ARE_IN_OLYMPIAD));
						continue;
					}

					if(targetChar.isInsideZone(L2Character.ZONE_PVP))
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
						continue;
					}
				}

				target.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			}
		}
		catch(Throwable e)
		{
			_log.error("", e);
		}
	}

	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}