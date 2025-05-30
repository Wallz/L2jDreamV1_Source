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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.handler.ISkillHandler;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2DoorInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.siege.Castle;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.Formulas;
import com.src.gameserver.templates.item.L2WeaponType;
import com.src.gameserver.templates.skills.L2SkillType;

public class StrSiegeAssault implements ISkillHandler
{
	private static final Log _log = LogFactory.getLog(StrSiegeAssault.class);

	private static final L2SkillType[] SKILL_IDS = { L2SkillType.STRSIEGEASSAULT };

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if(activeChar == null || !(activeChar instanceof L2PcInstance))
		{
			return;
		}

		L2PcInstance player = (L2PcInstance) activeChar;

		if(!activeChar.isRiding())
		{
			return;
		}

		if(!(player.getTarget() instanceof L2DoorInstance))
		{
			return;
		}

		Castle castle = CastleManager.getInstance().getCastle(player);
		if((castle == null))
		{
			return;
		}

		if(castle != null)
		{
			if(!checkIfOkToUseStriderSiegeAssault(player, castle, true))
			{
				return;
			}
		}

		castle = null;

		try
		{
			L2ItemInstance itemToTake = player.getInventory().getItemByItemId(skill.getItemConsumeId());

			if(!player.destroyItem("Consume", itemToTake.getObjectId(), skill.getItemConsume(), null, true))
			{
				return;
			}

			int damage = 0;

			for(L2Object target2 : targets)
			{
				L2Character target = (L2Character) target2;
				L2ItemInstance weapon = activeChar.getActiveWeaponInstance();
				if(activeChar instanceof L2PcInstance && target instanceof L2PcInstance && target.isAlikeDead() && target.isFakeDeath())
				{
					target.stopFakeDeath(null);
				}
				else if(target.isAlikeDead())
				{
					continue;
				}

				boolean dual = activeChar.isUsingDualWeapon();
				boolean shld = Formulas.calcShldUse(activeChar, target);
				boolean crit = Formulas.calcCrit(activeChar.getCriticalHit(target, skill));
				boolean soul = (weapon != null && weapon.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT && weapon.getItemType() != L2WeaponType.DAGGER);

				if(!crit && (skill.getCondition() & L2Skill.COND_CRIT) != 0)
				{
					damage = 0;
				}
				else
				{
					damage = (int) Formulas.getInstance().calcPhysDam(activeChar, target, skill, shld, crit, dual, soul);
				}

				if(damage > 0)
				{
					target.reduceCurrentHp(damage, activeChar);
					if(soul && weapon != null)
					{
						weapon.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
					}

					activeChar.sendDamageMessage(target, damage, false, false, false);

				}
				else
				{
					activeChar.sendPacket(SystemMessage.sendString(skill.getName() + " failed."));
				}
			}
		}
		catch(Exception e)
		{
			player.sendMessage("Error using siege assault:" + e);
			_log.error("", e);
		}
	}

	public static boolean checkIfOkToUseStriderSiegeAssault(L2Character activeChar, boolean isCheckOnly)
	{
		return checkIfOkToUseStriderSiegeAssault(activeChar, CastleManager.getInstance().getCastle(activeChar), isCheckOnly);
	}

	public static boolean checkIfOkToUseStriderSiegeAssault(L2Character activeChar, Castle castle, boolean isCheckOnly)
	{
		if(activeChar == null || !(activeChar instanceof L2PcInstance))
		{
			return false;
		}

		SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
		L2PcInstance player = (L2PcInstance) activeChar;

		if(castle == null || castle.getCastleId() <= 0)
		{
			sm.addString("You must be on castle ground to use strider siege assault");
		}
		else if(!castle.getSiege().getIsInProgress())
		{
			sm.addString("You can only use strider siege assault during a siege.");
		}
		else if(!(player.getTarget() instanceof L2DoorInstance))
		{
			sm.addString("You can only use strider siege assault on doors and walls.");
		}
		else if(!activeChar.isRiding())
		{
			sm.addString("You can only use strider siege assault when on strider.");
		}
		else
		{
			return true;
		}

		if(!isCheckOnly)
		{
			player.sendPacket(sm);
		}
		return false;
	}

	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}