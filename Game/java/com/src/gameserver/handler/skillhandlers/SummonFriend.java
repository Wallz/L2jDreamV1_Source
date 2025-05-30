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

import com.src.Config;
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
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.ConfirmDlg;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.skills.L2SkillType;
import com.src.gameserver.util.Util;

public class SummonFriend implements ISkillHandler
{
	private final static Log _log = LogFactory.getLog(SummonFriend.class);

	private static final L2SkillType[] SKILL_IDS = { L2SkillType.SUMMON_FRIEND };

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if(!(activeChar instanceof L2PcInstance))
		{
			return;
		}

		L2PcInstance activePlayer = (L2PcInstance) activeChar;
		
        if (!L2PcInstance.checkSummonerStatus(activePlayer))
            return;

		if(activePlayer.isInOlympiadMode())
		{
			activePlayer.sendPacket(new SystemMessage(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
			return;
		}

		if(activePlayer.isInEvent())
		{
			activePlayer.sendMessage("You can't use this skill in Event.");
			return;
		}

		if(activeChar.isInsideZone(L2Character.ZONE_SIEGE))
		{
			((L2PcInstance) activeChar).sendMessage("You cannot summon in siege zone.");
			return;
		}
		if (activePlayer.isFightingInEvent())
		{
			if ((activePlayer.getEventName().equals("TVT") && !Config.TVT_ALLOW_SCROLL)
					|| (activePlayer.getEventName().equals("CTF") && !Config.CTF_ALLOW_SCROLL)
					|| (activePlayer.getEventName().equals("BW") && !Config.BW_ALLOW_SCROLL)
					|| (activePlayer.getEventName().equals("DM") && !Config.DM_ALLOW_SCROLL))
			 	{
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			 		return;
			 	}
		}

		if(activePlayer.isInsideZone(L2Character.ZONE_PVP))
		{
			activePlayer.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_SUMMON_IN_COMBAT));
			return;
		}

		if(GrandBossManager.getInstance().getZone(activePlayer) != null && !activePlayer.isGM())
		{
			activePlayer.sendMessage("You may not use Summon Friend Skill inside a Boss Zone.");
			return;
		}

		FastList<L2Object> objects = L2World.getInstance().getVisibleObjects(activeChar, 5000);
		if(objects != null)
		{
			for(L2Object object : objects)
			{
				if(object instanceof L2RaidBossInstance || object instanceof L2GrandBossInstance)
				{
					activePlayer.sendPacket(new SystemMessage(SystemMessageId.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION));
					return;
				}
			}
		}

		try
		{
			for(int index = 0; index < targets.length; index++)
			{
				if(!(targets[index] instanceof L2Character))
				{
					continue;
				}

				L2Character target = (L2Character) targets[index];

				if(activeChar == target)
				{
					continue;
				}

				if(target instanceof L2PcInstance)
				{
					L2PcInstance targetChar = (L2PcInstance) target;

                    if (!L2PcInstance.checkSummonTargetStatus(targetChar, activePlayer))
                        continue;

					if(targetChar.isAlikeDead())
					{
						activePlayer.sendPacket(new SystemMessage(SystemMessageId.S1_IS_DEAD_AT_THE_MOMENT_AND_CANNOT_BE_SUMMONED).addString(targetChar.getName()));
						continue;
					}

					if(targetChar.isAio())
					{
						activeChar.sendPacket(SystemMessage.sendString("You cannot summon Aio Buffers."));
						return;
					}
					
					if(targetChar.isInEvent())
					{
						targetChar.sendMessage("You cannot use this skill in a Event.");
						return;
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

					if(targetChar.isFestivalParticipant())
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
						continue;
					}

					if(targetChar.isInsideZone(L2Character.ZONE_PVP))
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
						continue;
					}

					if((targetChar.getInventory().getItemByItemId(8615) == null) && (skill.getId() != 1429))
					{
						((L2PcInstance) activeChar).sendMessage("Your target cannot be summoned while he hasn't got a Summoning Crystal");
						targetChar.sendMessage("You cannot be summoned while you haven't got a Summoning Crystal");
						continue;
					}

                    if(!Util.checkIfInRange(0, activeChar, target, false))
                    {       
                            // Check already summon
                            if(!targetChar.teleportRequest((L2PcInstance) activeChar, skill))
                            {
                                    SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_ALREADY_SUMMONED);
                                    sm.addString(target.getName());
                                    activeChar.sendPacket(sm);
                                    continue;
                            }
                            
                            // Summon friend
                            if (skill.getId() == 1403)
                            {
                                    // Send message
                                    ConfirmDlg confirm = new ConfirmDlg(SystemMessageId.S1_WISHES_TO_SUMMON_YOU_FROM_S2_DO_YOU_ACCEPT.getId());
                                    confirm.addString(activeChar.getName());
                                    confirm.addZoneName(activeChar.getX(), activeChar.getY(), activeChar.getZ());
                                    confirm.addTime(30000);
                                    confirm.addRequesterId(activeChar.getObjectId());
                                    targetChar.sendPacket(confirm);
                            }
                            else
                            {
                                    L2PcInstance.teleToTarget(targetChar, (L2PcInstance) activeChar, skill);
                                    targetChar.teleportRequest(null, null);
                            }
                    }

				}
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