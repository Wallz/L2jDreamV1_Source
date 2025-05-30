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

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.handler.ISkillHandler;
import com.src.gameserver.model.L2Manor;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2MonsterInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.PlaySound;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.skills.L2SkillType;
import com.src.util.random.Rnd;

public class Sow implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS = { L2SkillType.SOW };

	private L2PcInstance _activeChar;
	private L2MonsterInstance _target;
	private int _seedId;

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if(!(activeChar instanceof L2PcInstance))
		{
			return;
		}

		_activeChar = (L2PcInstance) activeChar;

		L2Object[] targetList = skill.getTargetList(activeChar);
		if(targetList == null)
		{
			return;
		}

		for(int index = 0; index < targetList.length; index++)
		{
			if(!(targetList[0] instanceof L2MonsterInstance))
			{
				continue;
			}

			_target = (L2MonsterInstance) targetList[0];
			if(_target.isSeeded())
			{
				_activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				continue;
			}

			if(_target.isDead())
			{
				_activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				continue;
			}

			if(_target.getSeeder() != _activeChar)
			{
				_activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				continue;
			}

			_seedId = _target.getSeedType();
			if(_seedId == 0)
			{
				_activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				continue;
			}

			L2ItemInstance item = _activeChar.getInventory().getItemByItemId(_seedId);
			if(item == null)
			{
				_activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			 	break;
			}

			_activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
			item = null;

			SystemMessage sm = null;
			if(calcSuccess())
			{
				_activeChar.sendPacket(new PlaySound("Itemsound.quest_itemget"));
				_target.setSeeded();
				sm = new SystemMessage(SystemMessageId.THE_SEED_WAS_SUCCESSFULLY_SOWN);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.THE_SEED_WAS_NOT_SOWN);
			}

			if(_activeChar.getParty() == null)
			{
				_activeChar.sendPacket(sm);
			}
			else
			{
				_activeChar.getParty().broadcastToPartyMembers(sm);
			}
			sm = null;
			_target.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);

			_target = null;
		}
	}

	private boolean calcSuccess()
	{
		if(_activeChar== null || _target == null)
			return false;
		
		int basicSuccess = (L2Manor.getInstance().isAlternative(_seedId) ? 20 : 90);
		int minlevelSeed = 0;
		int maxlevelSeed = 0;
		minlevelSeed = L2Manor.getInstance().getSeedMinLevel(_seedId);
		maxlevelSeed = L2Manor.getInstance().getSeedMaxLevel(_seedId);

		int levelPlayer = _activeChar.getLevel();
		int levelTarget = _target.getLevel();

		if(levelTarget < minlevelSeed)
		{
			basicSuccess -= 5;
		}
		if(levelTarget > maxlevelSeed)
		{
			basicSuccess -= 5;
		}

		int diff = (levelPlayer - levelTarget);
		if(diff < 0)
		{
			diff = -diff;
		}

		if(diff > 5)
		{
			basicSuccess -= 5 * (diff - 5);
		}

		if(basicSuccess < 1)
		{
			basicSuccess = 1;
		}

		int rate = Rnd.nextInt(99);

		return (rate < basicSuccess);
	}

	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}