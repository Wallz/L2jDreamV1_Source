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

import com.src.gameserver.handler.ISkillHandler;
import com.src.gameserver.model.L2Fishing;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.item.L2Weapon;
import com.src.gameserver.templates.item.L2WeaponType;
import com.src.gameserver.templates.skills.L2SkillType;

public class FishingSkill implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.PUMPING,
		L2SkillType.REELING
	};

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if(activeChar == null || !(activeChar instanceof L2PcInstance))
		{
			return;
		}

		L2PcInstance player = (L2PcInstance)activeChar;

		L2Fishing fish = player.GetFishCombat();
		if(fish == null)
		{
			if(skill.getSkillType() == L2SkillType.PUMPING)
			{
			}
			else if(skill.getSkillType() == L2SkillType.REELING)
			{
			}

			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		L2Weapon weaponItem = player.getActiveWeaponItem();
		L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
		if(weaponInst == null || weaponItem == null || weaponItem.getItemType() != L2WeaponType.ROD)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			activeChar.sendPacket(sm);
			sm = null;
			return;
		}

		int SS = 1;
		int pen = 0;

		if(weaponInst != null && weaponInst.getChargedFishshot())
		{
			SS = 2;
		}

		double gradebonus = 1 + weaponItem.getCrystalType() * 0.1;
		int dmg = (int)(skill.getPower()*gradebonus*SS);
		weaponItem = null;
		if(player.getSkillLevel(1315) <= skill.getLevel()-2)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.REELING_PUMPING_3_LEVELS_HIGHER_THAN_FISHING_PENALTY));
			pen = 50;
			int penatlydmg = dmg - pen;
			if(player.isGM())
			{
				player.sendMessage("Dmg w/o penalty = " + dmg);
			}

			dmg = penatlydmg;
		}

		if(SS > 1)
		{
			weaponInst.setChargedFishshot(false);
		}

		if(skill.getSkillType() == L2SkillType.REELING)
		{
			fish.useRealing(dmg, pen);
		}
		else
		{
			fish.usePomping(dmg, pen);
		}
	}

	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}