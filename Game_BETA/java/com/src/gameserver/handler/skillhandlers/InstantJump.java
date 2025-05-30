/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.src.gameserver.handler.skillhandlers;

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.handler.ISkillHandler;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.FlyToLocation;
import com.src.gameserver.network.serverpackets.FlyToLocation.FlyType;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.network.serverpackets.ValidateLocation;
import com.src.gameserver.skills.Formulas;
import com.src.gameserver.templates.skills.L2SkillType;
import com.src.gameserver.util.Util;

public class InstantJump implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS =
	{
		L2SkillType.INSTANT_JUMP
	};
	
	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		L2Character target = (L2Character) targets[0];
		
		if (Formulas.calcPhysicalSkillEvasion(target, skill))
		{
			if (activeChar instanceof L2PcInstance)
				((L2PcInstance) activeChar).sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DODGES_ATTACK).addString(target.getName()));
			
			return;
		}
		
		int x = 0, y = 0, z = 0;
		
		int px = target.getX();
		int py = target.getY();
		double ph = Util.convertHeadingToDegree(target.getHeading());
		
		ph += 180;
		
		if (ph > 360)
			ph -= 360;
		
		ph = (Math.PI * ph) / 180;
		
		x = (int) (px + (25 * Math.cos(ph)));
		y = (int) (py + (25 * Math.sin(ph)));
		z = target.getZ();
		
		activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		activeChar.broadcastPacket(new FlyToLocation(activeChar, x, y, z, FlyType.DUMMY));
		activeChar.abortAttack();
		activeChar.abortCast();
		
		activeChar.setXYZ(x, y, z);
		activeChar.broadcastPacket(new ValidateLocation(activeChar));
	}
	
	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}