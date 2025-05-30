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
package com.src.gameserver.model.actor.instance;

import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;

import java.util.concurrent.Future;

import javolution.util.FastMap;

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.network.serverpackets.NpcInfo;
import com.src.gameserver.network.serverpackets.StopMove;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.templates.skills.L2SkillType;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.object.Point3D;
import com.src.util.random.Rnd;

public final class L2TamedBeastInstance extends L2FeedableBeastInstance
{
	private int _foodSkillId;
	private static final int MAX_DISTANCE_FROM_HOME = 30000;
	private static final int MAX_DISTANCE_FROM_OWNER = 2000;
	private static final int MAX_DURATION = 1200000;
	private static final int DURATION_CHECK_INTERVAL = 60000;
	private static final int DURATION_INCREASE_INTERVAL = 20000;
	private static final int BUFF_INTERVAL = 5000;
	private int _remainingTime = MAX_DURATION;
	private int _homeX, _homeY, _homeZ;
	private L2PcInstance _owner;
	private Future<?> _buffTask = null;
	private Future<?> _durationCheckTask = null;

	public L2TamedBeastInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		setHome(this);
	}

	public L2TamedBeastInstance(int objectId, L2NpcTemplate template, L2PcInstance owner, int foodSkillId, int x, int y, int z)
	{
		super(objectId, template);

		setCurrentHp(getMaxHp());
		setCurrentMp(getMaxMp());
		setOwner(owner);
		setFoodType(foodSkillId);
		setHome(x, y, z);
		this.spawnMe(x, y, z);
	}

	public void onReceiveFood()
	{
		_remainingTime = _remainingTime + DURATION_INCREASE_INTERVAL;
		if(_remainingTime > MAX_DURATION)
		{
			_remainingTime = MAX_DURATION;
		}
	}

	public Point3D getHome()
	{
		return new Point3D(_homeX, _homeY, _homeZ);
	}

	public void setHome(int x, int y, int z)
	{
		_homeX = x;
		_homeY = y;
		_homeZ = z;
	}

	public void setHome(L2Character c)
	{
		setHome(c.getX(), c.getY(), c.getZ());
	}

	public int getRemainingTime()
	{
		return _remainingTime;
	}

	public void setRemainingTime(int duration)
	{
		_remainingTime = duration;
	}

	public int getFoodType()
	{
		return _foodSkillId;
	}

	public void setFoodType(int foodItemId)
	{
		if(foodItemId > 0)
		{
			_foodSkillId = foodItemId;

			if(_durationCheckTask != null)
			{
				_durationCheckTask.cancel(true);
			}

			_durationCheckTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new CheckDuration(this), DURATION_CHECK_INTERVAL, DURATION_CHECK_INTERVAL);
		}
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if(!super.doDie(killer))
		{
			return false;
		}

		getAI().stopFollow();
		_buffTask.cancel(true);
		_durationCheckTask.cancel(true);

		if(_owner != null)
		{
			_owner.setTrainedBeast(null);
		}

		_buffTask = null;
		_durationCheckTask = null;
		_owner = null;
		_foodSkillId = 0;
		_remainingTime = 0;
		return true;
	}

	public L2PcInstance getOwner()
	{
		return _owner;
	}

	public void setOwner(L2PcInstance owner)
	{
		if(owner != null)
		{
			_owner = owner;
			setTitle(owner.getName());
			broadcastPacket(new NpcInfo(this, owner));

			owner.setTrainedBeast(this);

			getAI().startFollow(_owner, 100);

			int totalBuffsAvailable = 0;
			for(L2Skill skill : getTemplate().getSkills().values())
			{
				if(skill.getSkillType() == L2SkillType.BUFF)
				{
					totalBuffsAvailable++;
				}
			}

			if(_buffTask != null)
			{
				_buffTask.cancel(true);
			}
			_buffTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new CheckOwnerBuffs(this, totalBuffsAvailable), BUFF_INTERVAL, BUFF_INTERVAL);
		}
		else
		{
			doDespawn();
		}
	}

	public boolean isTooFarFromHome()
	{
		return !this.isInsideRadius(_homeX, _homeY, _homeZ, MAX_DISTANCE_FROM_HOME, true, true);
	}

	public void doDespawn()
	{
		getAI().stopFollow();
		_buffTask.cancel(true);
		_durationCheckTask.cancel(true);
		stopHpMpRegeneration();

		if(_owner != null)
		{
			_owner.setTrainedBeast(null);
		}
		setTarget(null);
		_buffTask = null;
		_durationCheckTask = null;
		_owner = null;
		_foodSkillId = 0;
		_remainingTime = 0;

		onDecay();
	}

	public void onOwnerGotAttacked(L2Character attacker)
	{
		if(_owner == null || _owner.isOnline() == 0)
		{
			doDespawn();
			return;
		}

		if(!_owner.isInsideRadius(this, MAX_DISTANCE_FROM_OWNER, true, true))
		{
			getAI().startFollow(_owner);
			return;
		}

		if(_owner.isDead())
		{
			return;
		}

		if(isCastingNow())
		{
			return;
		}

		float HPRatio = (float) _owner.getCurrentHp() / _owner.getMaxHp();

		if(HPRatio >= 0.8)
		{
			FastMap<Integer, L2Skill> skills = (FastMap<Integer, L2Skill>) getTemplate().getSkills();

			for(L2Skill skill : skills.values())
			{
				if(skill.getSkillType() == L2SkillType.DEBUFF && Rnd.get(3) < 1 && attacker.getFirstEffect(skill) != null)
				{
					sitCastAndFollow(skill, attacker);
				}
			}
			skills = null;
		}
		else if(HPRatio < 0.5)
		{
			int chance = 1;
			if(HPRatio < 0.25)
			{
				chance = 2;
			}

			FastMap<Integer, L2Skill> skills = (FastMap<Integer, L2Skill>) getTemplate().getSkills();

			for(L2Skill skill : skills.values())
			{
				if(Rnd.get(5) < chance && (skill.getSkillType() == L2SkillType.HEAL || skill.getSkillType() == L2SkillType.HOT || skill.getSkillType() == L2SkillType.BALANCE_LIFE || skill.getSkillType() == L2SkillType.HEAL_PERCENT || skill.getSkillType() == L2SkillType.HEAL_STATIC || skill.getSkillType() == L2SkillType.COMBATPOINTHEAL || skill.getSkillType() == L2SkillType.COMBATPOINTPERCENTHEAL || skill.getSkillType() == L2SkillType.CPHOT || skill.getSkillType() == L2SkillType.MANAHEAL || skill.getSkillType() == L2SkillType.MANA_BY_LEVEL || skill.getSkillType() == L2SkillType.MANAHEAL_PERCENT || skill.getSkillType() == L2SkillType.MANARECHARGE || skill.getSkillType() == L2SkillType.MPHOT))
				{
					sitCastAndFollow(skill, _owner);
					return;
				}
			}
			skills = null;
		}
	}

	protected void sitCastAndFollow(L2Skill skill, L2Character target)
	{
		stopMove(null);
		broadcastPacket(new StopMove(this));
		getAI().setIntention(AI_INTENTION_IDLE);

		setTarget(target);
		doCast(skill);
		getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, _owner);
	}

	private class CheckDuration implements Runnable
	{
		private L2TamedBeastInstance _tamedBeast;

		CheckDuration(L2TamedBeastInstance tamedBeast)
		{
			_tamedBeast = tamedBeast;
		}

		@Override
		public void run()
		{
			int foodTypeSkillId = _tamedBeast.getFoodType();
			L2PcInstance owner = _tamedBeast.getOwner();
			_tamedBeast.setRemainingTime(_tamedBeast.getRemainingTime() - DURATION_CHECK_INTERVAL);

			L2ItemInstance item = null;

			if(foodTypeSkillId == 2188)
			{
				item = owner.getInventory().getItemByItemId(6643);
			}
			else if(foodTypeSkillId == 2189)
			{
				item = owner.getInventory().getItemByItemId(6644);
			}

			if(item != null && item.getCount() >= 1)
			{
				L2Object oldTarget = owner.getTarget();
				owner.setTarget(_tamedBeast);
				L2Object[] targets =
				{
					_tamedBeast
				};

				owner.callSkill(SkillTable.getInstance().getInfo(foodTypeSkillId, 1), targets);
				owner.setTarget(oldTarget);
				oldTarget = null;
				targets = null;
			}
			else
			{
				if(_tamedBeast.getRemainingTime() < MAX_DURATION - 300000)
				{
					_tamedBeast.setRemainingTime(-1);
				}
			}

			if(_tamedBeast.getRemainingTime() <= 0)
			{
				_tamedBeast.doDespawn();
			}

			item = null;
		}
	}

	private class CheckOwnerBuffs implements Runnable
	{
		private L2TamedBeastInstance _tamedBeast;
		private int _numBuffs;

		CheckOwnerBuffs(L2TamedBeastInstance tamedBeast, int numBuffs)
		{
			_tamedBeast = tamedBeast;
			_numBuffs = numBuffs;
		}

		@Override
		public void run()
		{
			L2PcInstance owner = _tamedBeast.getOwner();

			if(owner == null || owner.isOnline() == 0)
			{
				doDespawn();
				return;
			}

			if(!isInsideRadius(owner, MAX_DISTANCE_FROM_OWNER, true, true))
			{
				getAI().startFollow(owner);
				return;
			}

			if(owner.isDead())
			{
				return;
			}

			if(isCastingNow())
			{
				return;
			}

			int totalBuffsOnOwner = 0;
			int i = 0;
			int rand = Rnd.get(_numBuffs);
			L2Skill buffToGive = null;

			FastMap<Integer, L2Skill> skills = (FastMap<Integer, L2Skill>) _tamedBeast.getTemplate().getSkills();

			for(L2Skill skill : skills.values())
			{
				if(skill.getSkillType() == L2SkillType.BUFF)
				{
					if(i == rand)
					{
						buffToGive = skill;
					}

					i++;
					if(owner.getFirstEffect(skill) != null)
					{
						totalBuffsOnOwner++;
					}
				}
			}
			if(_numBuffs * 2 / 3 > totalBuffsOnOwner)
			{
				_tamedBeast.sitCastAndFollow(buffToGive, owner);
			}
			getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, _tamedBeast.getOwner());
			buffToGive = null;
			skills = null;
		}
	}
}