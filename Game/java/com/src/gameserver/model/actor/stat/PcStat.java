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
package com.src.gameserver.model.actor.stat;

import com.src.Config;
import com.src.gameserver.managers.FunEventsManager;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2ClassMasterInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.model.base.ClassLevel;
import com.src.gameserver.model.base.Experience;
import com.src.gameserver.model.base.PlayerClass;
import com.src.gameserver.model.quest.QuestState;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import com.src.gameserver.network.serverpackets.SocialAction;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.network.serverpackets.UserInfo;

public class PcStat extends PlayableStat
{
	private int _oldMaxHp;
	private int _oldMaxMp;
	private int _oldMaxCp;

	public PcStat(L2PcInstance activeChar)
	{
		super(activeChar);
	}

	@Override
	public boolean addExp(long value)
	{
		L2PcInstance activeChar = getActiveChar();

		if(!getActiveChar().getAccessLevel().canGainExp() && getActiveChar().isInParty())
		{
			return false;
		}

		if(!super.addExp(value))
		{
			return false;
		}

		if(!activeChar.isCursedWeaponEquiped() && activeChar.getKarma() > 0 && (activeChar.isGM() || !activeChar.isInsideZone(L2Character.ZONE_PVP)))
		{
			int karmaLost = activeChar.calculateKarmaLost(value);

			if(karmaLost > 0)
			{
				activeChar.setKarma(activeChar.getKarma() - karmaLost);
			}
		}

		activeChar.sendPacket(new UserInfo(activeChar));

		activeChar = null;

		return true;
	}

	@Override
	public boolean addExpAndSp(long addToExp, int addToSp)
	{
		float ratioTakenByPet = 0;

		L2PcInstance activeChar = getActiveChar();
		if(!activeChar.getAccessLevel().canGainExp() && activeChar.isInParty())
		{
			return false;
		}

		if(activeChar.getPet() instanceof L2PetInstance)
		{
			L2PetInstance pet = (L2PetInstance) activeChar.getPet();
			ratioTakenByPet = pet.getPetData().getOwnerExpTaken();

			if(ratioTakenByPet > 0 && !pet.isDead())
			{
				pet.addExpAndSp((long) (addToExp * ratioTakenByPet), (int) (addToSp * ratioTakenByPet));
			}

			if(ratioTakenByPet > 1)
			{
				ratioTakenByPet = 1;
			}

			addToExp = (long) (addToExp * (1 - ratioTakenByPet));
			addToSp = (int) (addToSp * (1 - ratioTakenByPet));

			pet = null;
		}

		if(!super.addExpAndSp(addToExp, addToSp))
		{
			return false;
		}

		getActiveChar().sendPacket(new SystemMessage(SystemMessageId.YOU_EARNED_S1_EXP_AND_S2_SP).addNumber((int) addToExp).addNumber(addToSp));

		activeChar = null;

		return true;
	}

	@Override
	public boolean removeExpAndSp(long addToExp, int addToSp)
	{
		if(!super.removeExpAndSp(addToExp, addToSp))
		{
			return false;
		}

		getActiveChar().sendPacket(new SystemMessage(SystemMessageId.EXP_DECREASED_BY_S1).addNumber((int) addToExp));

		getActiveChar().sendPacket(new SystemMessage(SystemMessageId.SP_DECREASED_S1).addNumber(addToSp));

		return true;
	}

	@Override
	public final boolean addLevel(byte value)
	{
		if(getLevel() + value > Experience.MAX_LEVEL - 1)
		{
			return false;
		}

		boolean levelIncreased = super.addLevel(value);

		if(Config.ALLOW_REMOTE_CLASS_MASTERS)
		{
			ClassLevel lvlnow = PlayerClass.values()[getActiveChar().getClassId().getId()].getLevel();
			if(getLevel() >= 20 && lvlnow == ClassLevel.First)
			{
				L2ClassMasterInstance.ClassMaster.onAction(getActiveChar());
			}
			else if(getLevel() >= 40 && lvlnow == ClassLevel.Second)
			{
				L2ClassMasterInstance.ClassMaster.onAction(getActiveChar());
			}
			else if(getLevel() >= 76 && lvlnow == ClassLevel.Third)
			{
				L2ClassMasterInstance.ClassMaster.onAction(getActiveChar());
			}

			lvlnow = null;
		}

		if(levelIncreased)
		{
			if(getActiveChar().getLevel() >= 25 && getActiveChar().isNewbie())
			{
				getActiveChar().setNewbie(false);
			}

			QuestState qs = getActiveChar().getQuestState("Q255_Tutorial");

			if(qs != null && qs.getQuest() != null)
			{
				qs.getQuest().notifyEvent("CE40", null, getActiveChar());
			}

			getActiveChar().setCurrentCp(getMaxCp());
			getActiveChar().broadcastPacket(new SocialAction(getActiveChar().getObjectId(), 15));
			getActiveChar().sendPacket(new SystemMessage(SystemMessageId.YOU_INCREASED_YOUR_LEVEL));
			getActiveChar().store();

			qs = null;
		}
		if (getActiveChar().getEventName() != null)
			FunEventsManager.getInstance().notifyLevelChanged(getActiveChar());
		
		getActiveChar().rewardSkills();

		if(getActiveChar().getClan() != null)
		{
			getActiveChar().getClan().updateClanMember(getActiveChar());
			getActiveChar().getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(getActiveChar()));
		}

		if(getActiveChar().isInParty())
		{
			getActiveChar().getParty().recalculatePartyLevel();
		}

		StatusUpdate su = new StatusUpdate(getActiveChar().getObjectId());
		su.addAttribute(StatusUpdate.LEVEL, getLevel());
		su.addAttribute(StatusUpdate.MAX_CP, getMaxCp());
		su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
		su.addAttribute(StatusUpdate.MAX_MP, getMaxMp());
		getActiveChar().sendPacket(su);
		su = null;

		getActiveChar().refreshOverloaded();
		getActiveChar().refreshExpertisePenalty();
		getActiveChar().sendPacket(new UserInfo(getActiveChar()));

		return levelIncreased;
	}

	@Override
	public boolean addSp(int value)
	{
		if(!super.addSp(value))
		{
			return false;
		}

		StatusUpdate su = new StatusUpdate(getActiveChar().getObjectId());
		su.addAttribute(StatusUpdate.SP, getSp());
		getActiveChar().sendPacket(su);
		su = null;

		return true;
	}

	@Override
	public final long getExpForLevel(int level)
	{
		return Experience.getExp(level);
	}

	@Override
	public final L2PcInstance getActiveChar()
	{
		return (L2PcInstance) super.getActiveChar();
	}

	@Override
	public final long getExp()
	{
		try
		{
			if(getActiveChar().isSubClassActive())
				return getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).getExp();
			
			return super.getExp();
		}
		catch(NullPointerException e)
		{
			return -1;
		}
	}

	@Override
	public final void setExp(long value)
	{
		if(getActiveChar().isSubClassActive())
		{
			getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).setExp(value);
		}
		else
		{
			super.setExp(value);
		}
	}

	@Override
	public final int getLevel()
	{
		try
		{
			if(getActiveChar().isSubClassActive())
			{
				return getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).getLevel();
			}
			
			return super.getLevel();
		}
		catch(NullPointerException e)
		{
			return -1;
		}
	}

	@Override
	public final void setLevel(int value)
	{
		if(value > Experience.MAX_LEVEL - 1)
		{
			value = Experience.MAX_LEVEL - 1;
		}

		if(getActiveChar().isSubClassActive())
		{
			getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).setLevel(value);
		}
		else
		{
			super.setLevel(value);
		}
	}

	@Override
	public final int getMaxCp()
	{
		int val = super.getMaxCp();

		if(val != _oldMaxCp)
		{
			_oldMaxCp = val;

			if(getActiveChar().getStatus().getCurrentCp() != val)
			{
				getActiveChar().getStatus().setCurrentCp(getActiveChar().getStatus().getCurrentCp());
			}
		}
		return val;
	}

	@Override
	public final int getMaxHp()
	{
		int val = super.getMaxHp();

		if(val != _oldMaxHp)
		{
			_oldMaxHp = val;

			if(getActiveChar().getStatus().getCurrentHp() != val)
			{
				getActiveChar().getStatus().setCurrentHp(getActiveChar().getStatus().getCurrentHp());
			}
		}

		return val;
	}

	@Override
	public final int getMaxMp()
	{
		int val = super.getMaxMp();

		if(val != _oldMaxMp)
		{
			_oldMaxMp = val;

			if(getActiveChar().getStatus().getCurrentMp() != val)
			{
				getActiveChar().getStatus().setCurrentMp(getActiveChar().getStatus().getCurrentMp());
			}
		}

		return val;
	}

	@Override
	public final int getSp()
	{
		if(getActiveChar().isSubClassActive())
		{
			return getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).getSp();
		}

		return super.getSp();
	}

	@Override
	public final void setSp(int value)
	{
		if(getActiveChar().isSubClassActive())
		{
			getActiveChar().getSubClasses().get(getActiveChar().getClassIndex()).setSp(value);
		}
		else
		{
			super.setSp(value);
		}
	}

}