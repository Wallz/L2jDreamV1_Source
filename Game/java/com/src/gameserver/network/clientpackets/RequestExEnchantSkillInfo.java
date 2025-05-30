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
package com.src.gameserver.network.clientpackets;

import com.src.Config;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.xml.SkillTreeTable;
import com.src.gameserver.model.L2EnchantSkillLearn;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2NpcInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.ExEnchantSkillInfo;

public final class RequestExEnchantSkillInfo extends L2GameClientPacket
{
	private static final String _C__D0_06_REQUESTEXENCHANTSKILLINFO = "[C] D0:06 RequestExEnchantSkillInfo";

	private int _skillId;
	private int _skillLvl;

	@Override
	protected void readImpl()
	{
		_skillId = readD();
		_skillLvl = readD();
	}

	@Override
	protected void runImpl()
	{
		if(_skillId <= 0 || _skillLvl <= 0)
		{
			return;
		}

		L2PcInstance activeChar = getClient().getActiveChar();

		if(activeChar == null)
		{
			return;
		}

		if(activeChar.getLevel() < 76)
		{
			return;
		}

		L2NpcInstance trainer = activeChar.getLastFolkNPC();

		if((trainer == null || !activeChar.isInsideRadius(trainer, L2Npc.INTERACTION_DISTANCE, false, false)) && !activeChar.isGM())
		{
			return;
		}

		L2Skill skill = SkillTable.getInstance().getInfo(_skillId, _skillLvl);

		boolean canteach = false;

		if(skill == null || skill.getId() != _skillId)
		{
			return;
		}

		if(!trainer.getTemplate().canTeach(activeChar.getClassId()))
		{
			return;
		}

		L2EnchantSkillLearn[] skills = SkillTreeTable.getInstance().getAvailableEnchantSkills(activeChar);

		for(L2EnchantSkillLearn s : skills)
		{
			if(s.getId() == _skillId && s.getLevel() == _skillLvl)
			{
				canteach = true;
				break;
			}
		}

		if(!canteach)
		{
			return;
		}

		int requiredSp = SkillTreeTable.getInstance().getSkillSpCost(activeChar, skill);
		int requiredExp = SkillTreeTable.getInstance().getSkillExpCost(activeChar, skill);
		byte rate = SkillTreeTable.getInstance().getSkillRate(activeChar, skill);
		ExEnchantSkillInfo asi = new ExEnchantSkillInfo(skill.getId(), skill.getLevel(), requiredSp, requiredExp, rate);

		if(Config.ES_SP_BOOK_NEEDED && (skill.getLevel() == 101 || skill.getLevel() == 141))
		{
			int spbId = 6622;
			asi.addRequirement(4, spbId, 1, 0);
		}
		sendPacket(asi);

	}

	@Override
	public String getType()
	{
		return _C__D0_06_REQUESTEXENCHANTSKILLINFO;
	}

}