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
package com.src.gameserver.skills.l2skills;

import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.idfactory.IdFactory;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2CubicInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2SiegeSummonInstance;
import com.src.gameserver.model.actor.instance.L2SummonInstance;
import com.src.gameserver.model.base.Experience;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.PetInfo;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.StatsSet;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.util.random.Rnd;

public class L2SkillSummon extends L2Skill
{
	private int _npcId;
	private float _expPenalty;
	private boolean _isCubic;

	public L2SkillSummon(StatsSet set)
	{
		super(set);

		_npcId = set.getInteger("npcId", 0);
		_expPenalty = set.getFloat("expPenalty", 0.f);
		_isCubic = set.getBool("isCubic", false);
	}

	public boolean checkCondition(L2Character activeChar)
	{
		if(activeChar instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) activeChar;
			if(_isCubic)
			{
				if(player.isInCombat())
				{
					player.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_SUMMON_IN_COMBAT));
					return false;
				}

				if(getTargetType() != L2Skill.SkillTargetType.TARGET_SELF)
				{
					return true;
				}

				int mastery = player.getSkillLevel(L2Skill.SKILL_CUBIC_MASTERY);
				if(mastery < 0)
				{
					mastery = 0;
				}

				int count = player.getCubics().size();
				if(count > mastery)
				{
					activeChar.sendMessage("You already have " + count + " cubic(s).");
					return false;
				}
			}
			else
			{
				if(player.inObserverMode())
				{
					return false;
				}
				if(player.getPet() != null)
				{
					activeChar.sendMessage("You already have a pet.");
					return false;
				}
			}
		}
		return super.checkCondition(activeChar, null, false);
	}

	@Override
	public void useSkill(L2Character caster, L2Object[] targets)
	{
		if(caster.isAlikeDead() || !(caster instanceof L2PcInstance))
		{
			return;
		}

		L2PcInstance activeChar = (L2PcInstance) caster;

		if(!checkCondition(activeChar))
		{
			return;
		}

		if(_npcId == 0)
		{
			activeChar.sendMessage("Summon skill " + getId() + " not described yet");
			return;
		}

		if(_isCubic)
		{
			if(targets.length > 1)
			{
				for(L2Object obj : targets)
				{
					if(!(obj instanceof L2PcInstance))
					{
						continue;
					}
					L2PcInstance player = (L2PcInstance) obj;
					int mastery = player.getSkillLevel(L2Skill.SKILL_CUBIC_MASTERY);
					if(mastery < 0)
					{
						mastery = 0;
					}
					if(mastery == 0 && player.getCubics().size() > 0)
					{
						for(L2CubicInstance c : player.getCubics().values())
						{
							c.stopAction();
							c = null;
						}
						player.getCubics().clear();
					}
					if(player.getCubics().size() > mastery)
					{
						continue;
					}
					if(player.getCubics().containsKey(_npcId))
					{
						player.sendMessage("You already have such cubic");
					}
					else
					{

						player.addCubic(_npcId, getLevel());
						player.broadcastUserInfo();
					}
				}
				return;
			}
			else
			{
				int mastery = activeChar.getSkillLevel(L2Skill.SKILL_CUBIC_MASTERY);
				if(mastery < 0)
				{
					mastery = 0;
				}
				if(activeChar.getCubics().size() > mastery)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.CUBIC_SUMMONING_FAILED));
					return;
				}
				if(activeChar.getCubics().containsKey(_npcId))
				{
					activeChar.sendMessage("You already have such cubic");
					return;
				}
				activeChar.addCubic(_npcId, getLevel());
				activeChar.broadcastUserInfo();
				return;
			}
		}

		if(activeChar.getPet() != null || activeChar.isMounted())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		L2SummonInstance summon;
		L2NpcTemplate summonTemplate = NpcTable.getInstance().getTemplate(_npcId);
		if(summonTemplate.type.equalsIgnoreCase("L2SiegeSummon"))
		{
			summon = new L2SiegeSummonInstance(IdFactory.getInstance().getNextId(), summonTemplate, activeChar, this);
		}
		else
		{
			summon = new L2SummonInstance(IdFactory.getInstance().getNextId(), summonTemplate, activeChar, this);
		}

		summon.setName(summonTemplate.name);
		summon.setTitle(activeChar.getName());
		summon.setExpPenalty(_expPenalty);
		if(summon.getLevel() >= Experience.MAX_LEVEL)
		{
			summon.getStat().setExp(Experience.getExp(Experience.MAX_LEVEL - 1));
			_log.warning("Summon (" + summon.getName() + ") NpcID: " + summon.getNpcId() + " has a level above 75. Please rectify.");
		}
		else
		{
			summon.getStat().setExp(Experience.getExp((summon.getLevel() % Experience.MAX_LEVEL)));
		}
		summon.setCurrentHp(summon.getMaxHp());
		summon.setCurrentMp(summon.getMaxMp());
		summon.setHeading(activeChar.getHeading());
		summon.setRunning();
		activeChar.setPet(summon);

		L2World.storeObject(summon);

		if(getTargetType() == SkillTargetType.TARGET_CORPSE_MOB)
		{
			L2Character target = (L2Character) targets[0];
			if(target.isDead() && target instanceof L2Npc)
			{
				summon.spawnMe(target.getX(), target.getY(), target.getZ() + 5);
				((L2Npc) target).endDecayTask();
			}
		}
		else
		{
			summon.spawnMe(activeChar.getX() + Rnd.get(40)-20, activeChar.getY() + Rnd.get(40)-20, activeChar.getZ());
		}

		summon.setFollowStatus(true);
		summon.setShowSummonAnimation(false);
		activeChar.sendPacket(new PetInfo(summon));
	}
	
	public final boolean isCubic()
    {
        return _isCubic;
    }

}