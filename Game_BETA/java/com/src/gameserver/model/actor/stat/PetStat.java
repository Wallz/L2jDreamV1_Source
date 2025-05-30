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

import com.src.gameserver.datatables.xml.L2PetDataTable;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.model.base.Experience;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.PetInfo;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.Stats;

public class PetStat extends SummonStat
{
	public PetStat(L2PetInstance activeChar)
	{
		super(activeChar);
	}

	public boolean addExp(int value)
	{
		if(!super.addExp(value))
		{
			return false;
		}

		getActiveChar().broadcastPacket(new PetInfo(getActiveChar()));
		getActiveChar().updateEffectIcons(true);

		return true;
	}

	@Override
	public boolean addExpAndSp(long addToExp, int addToSp)
	{
		if(!super.addExpAndSp(addToExp, addToSp))
		{
			return false;
		}

		getActiveChar().getOwner().sendPacket(new SystemMessage(SystemMessageId.PET_EARNED_S1_EXP).addNumber((int) addToExp));

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

		if(getExp() > getExpForLevel(getLevel() + 1) || getExp() < getExpForLevel(getLevel()))
		{
			setExp(Experience.getExp(getLevel()));
		}

		if(levelIncreased)
		{
			getActiveChar().getOwner().sendMessage("Your pet has increased it's level.");
		}

		StatusUpdate su = new StatusUpdate(getActiveChar().getObjectId());
		su.addAttribute(StatusUpdate.LEVEL, getLevel());
		su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
		su.addAttribute(StatusUpdate.MAX_MP, getMaxMp());
		getActiveChar().broadcastPacket(su);
		su = null;

		getActiveChar().getOwner().sendPacket(new PetInfo(getActiveChar()));
		getActiveChar().updateEffectIcons(true);

		if(getActiveChar().getControlItem() != null)
		{
			getActiveChar().getControlItem().setEnchantLevel(getLevel());
		}

		return levelIncreased;
	}

	@Override
	public final long getExpForLevel(int level)
	{
		return L2PetDataTable.getInstance().getPetData(getActiveChar().getNpcId(), level).getPetMaxExp();
	}

	@Override
	public L2PetInstance getActiveChar()
	{
		return (L2PetInstance) super.getActiveChar();
	}

	public final int getFeedBattle()
	{
		return getActiveChar().getPetData().getPetFeedBattle();
	}

	public final int getFeedNormal()
	{
		return getActiveChar().getPetData().getPetFeedNormal();
	}

	@Override
	public void setLevel(int value)
	{
		getActiveChar().stopFeed();
		super.setLevel(value);

		getActiveChar().setPetData(L2PetDataTable.getInstance().getPetData(getActiveChar().getTemplate().npcId, getLevel()));
		getActiveChar().startFeed(false);

		if(getActiveChar().getControlItem() != null)
		{
			getActiveChar().getControlItem().setEnchantLevel(getLevel());
		}
	}

	public final int getMaxFeed()
	{
		return getActiveChar().getPetData().getPetMaxFeed();
	}

	@Override
	public int getMaxHp()
	{
		return (int) calcStat(Stats.MAX_HP, getActiveChar().getPetData().getPetMaxHP(), null, null);
	}

	@Override
	public int getMaxMp()
	{
		return (int) calcStat(Stats.MAX_MP, getActiveChar().getPetData().getPetMaxMP(), null, null);
	}

	@Override
	public int getMAtk(L2Character target, L2Skill skill)
	{
		double attack = getActiveChar().getPetData().getPetMAtk();
		Stats stat = skill == null ? null : skill.getStat();

		if(stat != null)
		{
			switch(stat)
			{
				case AGGRESSION:
					attack += getActiveChar().getTemplate().baseAggression;
					break;
				case BLEED:
					attack += getActiveChar().getTemplate().baseBleed;
					break;
				case POISON:
					attack += getActiveChar().getTemplate().basePoison;
					break;
				case STUN:
					attack += getActiveChar().getTemplate().baseStun;
					break;
				case ROOT:
					attack += getActiveChar().getTemplate().baseRoot;
					break;
				case MOVEMENT:
					attack += getActiveChar().getTemplate().baseMovement;
					break;
				case CONFUSION:
					attack += getActiveChar().getTemplate().baseConfusion;
					break;
				case SLEEP:
					attack += getActiveChar().getTemplate().baseSleep;
					break;
				case FIRE:
					attack += getActiveChar().getTemplate().baseFire;
					break;
				case WIND:
					attack += getActiveChar().getTemplate().baseWind;
					break;
				case WATER:
					attack += getActiveChar().getTemplate().baseWater;
					break;
				case EARTH:
					attack += getActiveChar().getTemplate().baseEarth;
					break;
				case HOLY:
					attack += getActiveChar().getTemplate().baseHoly;
					break;
				case DARK:
					attack += getActiveChar().getTemplate().baseDark;
					break;
			default:
				break;
			}
		}

		if(skill != null)
		{
			attack += skill.getPower();
		}

		stat = null;

		return (int) calcStat(Stats.MAGIC_ATTACK, attack, target, skill);
	}

	@Override
	public int getMDef(L2Character target, L2Skill skill)
	{
		double defence = getActiveChar().getPetData().getPetMDef();

		return (int) calcStat(Stats.MAGIC_DEFENCE, defence, target, skill);
	}

	@Override
	public int getPAtk(L2Character target)
	{
		return (int) calcStat(Stats.POWER_ATTACK, getActiveChar().getPetData().getPetPAtk(), target, null);
	}

	@Override
	public int getPDef(L2Character target)
	{
		return (int) calcStat(Stats.POWER_DEFENCE, getActiveChar().getPetData().getPetPDef(), target, null);
	}

	@Override
	public int getAccuracy()
	{
		return (int) calcStat(Stats.ACCURACY_COMBAT, getActiveChar().getPetData().getPetAccuracy(), null, null);
	}

	@Override
	public int getCriticalHit(L2Character target, L2Skill skill)
	{
		return (int) calcStat(Stats.CRITICAL_RATE, getActiveChar().getPetData().getPetCritical(), target, null);
	}

	@Override
	public int getEvasionRate(L2Character target)
	{
		return (int) calcStat(Stats.EVASION_RATE, getActiveChar().getPetData().getPetEvasion(), target, null);
	}

	@Override
	public int getRunSpeed()
	{
		final double multiplier = getActiveChar().isInsideZone(L2Character.ZONE_WATER) ? 0.5 : 1.0;
		return (int) (multiplier * calcStat(Stats.RUN_SPEED, getActiveChar().getPetData().getPetSpeed(), null, null));
	}

	@Override
	public int getPAtkSpd()
	{
		return (int) calcStat(Stats.POWER_ATTACK_SPEED, getActiveChar().getPetData().getPetAtkSpeed(), null, null);
	}

	@Override
	public int getMAtkSpd()
	{
		return (int) calcStat(Stats.MAGIC_ATTACK_SPEED, getActiveChar().getPetData().getPetCastSpeed(), null, null);
	}

}