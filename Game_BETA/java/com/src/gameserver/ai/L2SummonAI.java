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
package com.src.gameserver.ai;

import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_CAST;
import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_FOLLOW;
import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;
import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_INTERACT;
import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_PICK_UP;

import java.util.concurrent.Future;

import com.src.Config;
import com.src.gameserver.geo.GeoData;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Character.AIAccessor;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.random.Rnd;

public class L2SummonAI extends L2CharacterAI implements Runnable
{
	private static final int AVOID_RADIUS = 70;
	
	private boolean _thinking;
	
	private L2Character _lastAttack = null;
	
	private volatile boolean _startFollow = ((L2Summon) _actor).getFollowStatus();
	
	private volatile boolean _startAvoid = false;
	
	private Future<?> _avoidTask = null;
	
	public L2SummonAI(AIAccessor accessor)
	{
		super(accessor);
	}

	@Override
	protected void onIntentionIdle()
	{
		stopFollow();
		_startFollow = false;
		onIntentionActive();
	}

	@Override
	protected void onIntentionActive()
	{
		L2Summon summon = (L2Summon) _actor;
		if (_startFollow)
			setIntention(AI_INTENTION_FOLLOW, summon.getOwner());
		else
			super.onIntentionActive();
	}

	private void thinkAttack()
	{
		L2Character target = getAttackTarget();
		if (target == null)
			return;
		
		if (checkTargetLostOrDead(target))
		{
			setAttackTarget(null);
			return;
		}
		
		if (maybeMoveToPawn(target, _actor.getPhysicalAttackRange()))
		{
			_actor.breakAttack();
			return;
		}
		
		clientStopMoving(null);
		_accessor.doAttack(target);
	}

	private void thinkCast()
	{
		L2Summon summon = (L2Summon) _actor;
		if (checkTargetLost(getCastTarget()))
		{
			setCastTarget(null);
			return;
		}
		
		boolean val = _startFollow;
		if (maybeMoveToPawn(getCastTarget(), _actor.getMagicalAttackRange(_skill)))
			return;
		
		clientStopMoving(null);
		summon.setFollowStatus(false);
		setIntention(AI_INTENTION_IDLE);
		
		_startFollow = val;
		_accessor.doCast(_skill);
	}

	private void thinkPickUp()
	{
		if(_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			return;
		}

		if(checkTargetLost(getTarget()))
		{
			return;
		}

		if(maybeMoveToPawn(getTarget(), 36))
		{
			return;
		}

		setIntention(AI_INTENTION_IDLE);
		((L2Summon.AIAccessor) _accessor).doPickupItem(getTarget());

		return;
	}

	private void thinkInteract()
	{
		if(_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			return;
		}

		if(checkTargetLost(getTarget()))
		{
			return;
		}

		if(maybeMoveToPawn(getTarget(), 36))
		{
			return;
		}

		setIntention(AI_INTENTION_IDLE);

		return;
	}

	@Override
	protected void onEvtFinishCasting()
	{
		if (_lastAttack == null)
			((L2Summon) _actor).setFollowStatus(_startFollow);
		else
		{
			setIntention(CtrlIntention.AI_INTENTION_ATTACK, _lastAttack);
			_lastAttack = null;
		}
	}
	
	@Override
	protected void onEvtAttacked(L2Character attacker)
	{
		super.onEvtAttacked(attacker);
		
		avoidAttack(attacker);
	}
	
	@Override
	protected void onEvtEvaded(L2Character attacker)
	{
		super.onEvtEvaded(attacker);
		
		avoidAttack(attacker);
	}
	
	@Override
	protected void onEvtThink()
	{
		if(_thinking)
			return;

		_thinking = true;

		try
		{
			if(getIntention() == AI_INTENTION_ATTACK)
			{
				thinkAttack();
			}
			else if(getIntention() == AI_INTENTION_CAST)
			{
				thinkCast();
			}
			else if(getIntention() == AI_INTENTION_PICK_UP)
			{
				thinkPickUp();
			}
			else if(getIntention() == AI_INTENTION_INTERACT)
			{
				thinkInteract();
			}
		}
		finally
		{
			_thinking = false;
		}
	}
	
	private void avoidAttack(L2Character attacker)
	{
		// trying to avoid if summon near owner
		if (((L2Summon) _actor).getOwner() != null && ((L2Summon) _actor).getOwner() != attacker && ((L2Summon) _actor).getOwner().isInsideRadius(_actor, 2 * AVOID_RADIUS, true, false))
			_startAvoid = true;
	}
	
	@Override
	public void run()
	{
		if (_startAvoid)
		{
			_startAvoid = false;
			
			if (!_clientMoving && !_actor.isDead() && !_actor.isMovementDisabled())
			{
				final int ownerX = ((L2Summon) _actor).getOwner().getX();
				final int ownerY = ((L2Summon) _actor).getOwner().getY();
				final double angle = Math.toRadians(Rnd.get(-90, 90)) + Math.atan2(ownerY - _actor.getY(), ownerX - _actor.getX());
				
				final int targetX = ownerX + (int) (AVOID_RADIUS * Math.cos(angle));
				final int targetY = ownerY + (int) (AVOID_RADIUS * Math.sin(angle));
				if (Config.GEODATA == 0 || GeoData.getInstance().canMoveFromToTarget(_actor.getX(), _actor.getY(), _actor.getZ(), targetX, targetY, _actor.getZ()))
					moveTo(targetX, targetY, _actor.getZ());
			}
		}
	}
	
	public void notifyFollowStatusChange()
	{
		_startFollow = !_startFollow;
		switch (getIntention())
		{
			case AI_INTENTION_ACTIVE:
			case AI_INTENTION_FOLLOW:
			case AI_INTENTION_IDLE:
			case AI_INTENTION_MOVE_TO:
			case AI_INTENTION_PICK_UP:
				((L2Summon) _actor).setFollowStatus(_startFollow);
		default:
			break;
		}
	}
	
	public void setStartFollowController(boolean val)
	{
		_startFollow = val;
	}
	
	@Override
	protected void onIntentionCast(L2Skill skill, L2Object target)
	{
		if (getIntention() == AI_INTENTION_ATTACK)
			_lastAttack = getAttackTarget();
		else
			_lastAttack = null;
		super.onIntentionCast(skill, target);
	}
	
	@SuppressWarnings("unused")
	private void startAvoidTask()
	{
		if (_avoidTask == null)
			_avoidTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, 100, 100);
	}
	
	@SuppressWarnings("unused")
	private void stopAvoidTask()
	{
		if (_avoidTask != null)
		{
			_avoidTask.cancel(false);
			_avoidTask = null;
		}
	}

}