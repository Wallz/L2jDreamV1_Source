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
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.templates.skills.L2SkillType;
import com.src.util.random.Rnd;

public class ZakenPlayer implements ISkillHandler
{
	private final static Log _log = LogFactory.getLog(ZakenPlayer.class);

	private static final L2SkillType[] SKILL_IDS = { L2SkillType.ZAKENPLAYER };

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		try
		{
			for(int index = 0; index < targets.length; index++)
			{
				if(!(targets[index] instanceof L2Character))
				{
					continue;
				}

				L2Character target = (L2Character) targets[index];
				int ch = (Rnd.get(14) + 1);
				if(ch == 1)
				{
					target.teleToLocation(55299, 219120, -2952, true);
				}
				else if(ch == 2)
				{
					target.teleToLocation(56363, 218043, -2952, true);
				}
				else if(ch == 3)
				{
					target.teleToLocation(54245, 220162, -2952, true);
				}
				else if(ch == 4)
				{
					target.teleToLocation(56289, 220126, -2952, true);
				}
				else if(ch == 5)
				{
					target.teleToLocation(55299, 219120, -3224, true);
				}
				else if(ch == 6)
				{
					target.teleToLocation(56363, 218043, -3224, true);
				}
				else if(ch == 7)
				{
					target.teleToLocation(54245, 220162, -3224, true);
				}
				else if(ch == 8)
				{
					target.teleToLocation(56289, 220126, -3224, true);
				}
				else if(ch == 9)
				{
					target.teleToLocation(55299, 219120, -3496, true);
				}
				else if(ch == 10)
				{
					target.teleToLocation(56363, 218043, -3496, true);
				}
				else if(ch == 11)
				{
					target.teleToLocation(54245, 220162, -3496, true);
				}
				else if(ch == 12)
				{
					target.teleToLocation(56289, 220126, -3496, true);
				}
				else
				{
					target.teleToLocation(53930, 217760, -2944, true);
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