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
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.ValidateLocation;
import com.src.gameserver.templates.skills.L2SkillType;
import com.src.util.random.Rnd;

public class GetPlayer implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS = { L2SkillType.GET_PLAYER };

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if(activeChar.isAlikeDead())
		{
			return;
		}

		for(L2Object target : targets)
		{
			if(target instanceof L2PcInstance)
			{
				L2PcInstance trg = (L2PcInstance) target;
				if(trg.isAlikeDead())
				{
					continue;
				}

				trg.setXYZ(activeChar.getX() + Rnd.get(-10, 10), activeChar.getY() + Rnd.get(-10, 10), activeChar.getZ());
				trg.sendPacket(new ValidateLocation(trg));
			}
		}
	}

	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}