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
import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_FOLLOW;
import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;

import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.GameTimeController;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.position.L2CharPosition;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.AutoAttackStart;
import com.src.gameserver.network.serverpackets.AutoAttackStop;
import com.src.gameserver.network.serverpackets.CharMoveToLocation;
import com.src.gameserver.network.serverpackets.Die;
import com.src.gameserver.network.serverpackets.MoveToLocationInVehicle;
import com.src.gameserver.network.serverpackets.MoveToPawn;
import com.src.gameserver.network.serverpackets.StopMove;
import com.src.gameserver.network.serverpackets.StopRotation;
import com.src.gameserver.taskmanager.AttackStanceTaskManager;
import com.src.gameserver.thread.ThreadPoolManager;

abstract class AbstractAI implements Ctrl
{
	protected static final Log _log = LogFactory.getLog(AbstractAI.class);

	private NextAction _nextAction;
	
	/**
	 * @return the _nextAction
	 */
	public NextAction getNextAction()
	{
		return _nextAction;
	}
	
	/**
	 * @param nextAction the _nextAction to set
	 */
	public void setNextAction(NextAction nextAction)
	{
		_nextAction = nextAction;
	}
	
	class FollowTask implements Runnable
	{
		protected int _range = 60;
		protected boolean newtask = true;

		public FollowTask()
		{
		}

		public FollowTask(int range)
		{
			_range = range;
		}

		@Override
		public void run()
		{
			try
			{
				if(_followTask == null)
				{
					return;
				}

				if(_followTarget == null)
				{
					stopFollow();
					return;
				}

				if(!_actor.isInsideRadius(_followTarget, _range, true, false))
				{
					moveToPawn(_followTarget, _range);
				}
				else if(_followTarget instanceof L2Character && newtask)
				{
					newtask = false;
					_actor.broadcastPacket(new MoveToPawn(_actor, _followTarget, _range));
				}
			}
			catch(Throwable t)
			{
				_log.warn("", t);
			}
		}
	}

	protected final L2Character _actor;

	protected final L2Character.AIAccessor _accessor;

	protected CtrlIntention _intention = AI_INTENTION_IDLE;
	protected Object _intentionArg0 = null;
	protected Object _intentionArg1 = null;

	protected boolean _clientMoving;
	protected boolean _clientAutoAttacking;
	protected int _clientMovingToPawnOffset;

	private L2Object _target;
	private L2Character _castTarget;
	protected L2Character _attackTarget;
	protected L2Character _followTarget;

	L2Skill _skill;

	private int _moveToPawnTimeout;

	protected Future<?> _followTask = null;
	private static final int FOLLOW_INTERVAL = 1000;
	private static final int ATTACK_FOLLOW_INTERVAL = 500;

	protected AbstractAI(L2Character.AIAccessor accessor)
	{
		_accessor = accessor;

		_actor = accessor.getActor();
	}

	@Override
	public L2Character getActor()
	{
		return _actor;
	}

	@Override
	public CtrlIntention getIntention()
	{
		return _intention;
	}

	protected synchronized void setCastTarget(L2Character target)
	{
		_castTarget = target;
	}

	public L2Character getCastTarget()
	{
		return _castTarget;
	}

	protected synchronized void setAttackTarget(L2Character target)
	{
		_attackTarget = target;
	}

	@Override
	public L2Character getAttackTarget()
	{
		return _attackTarget;
	}

	synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		_intention = intention;
		_intentionArg0 = arg0;
		_intentionArg1 = arg1;
	}

	@Override
	public final void setIntention(CtrlIntention intention)
	{
		setIntention(intention, null, null);
	}

	@Override
	public final void setIntention(CtrlIntention intention, Object arg0)
	{
		setIntention(intention, arg0, null);
	}

	@Override
	public final void setIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		if((!_actor.isVisible() && !_actor.isTeleporting()) || !_actor.hasAI())
		{
			return;
		}

		if(intention != AI_INTENTION_FOLLOW && intention != AI_INTENTION_ATTACK)
		{
			stopFollow();
		}

		switch(intention)
		{
			case AI_INTENTION_IDLE:
				onIntentionIdle();
				break;
			case AI_INTENTION_ACTIVE:
				onIntentionActive();
				break;
			case AI_INTENTION_REST:
				onIntentionRest();
				break;
			case AI_INTENTION_ATTACK:
				onIntentionAttack((L2Character) arg0);
				break;
			case AI_INTENTION_CAST:
				onIntentionCast((L2Skill) arg0, (L2Object) arg1);
				break;
			case AI_INTENTION_MOVE_TO:
				onIntentionMoveTo((L2CharPosition) arg0);
				break;
			case AI_INTENTION_MOVE_TO_IN_A_BOAT:
				onIntentionMoveToInABoat((L2CharPosition) arg0, (L2CharPosition) arg1);
				break;
			case AI_INTENTION_FOLLOW:
				onIntentionFollow((L2Character) arg0);
				break;
			case AI_INTENTION_PICK_UP:
				onIntentionPickUp((L2Object) arg0);
				break;
			case AI_INTENTION_INTERACT:
				onIntentionInteract((L2Object) arg0);
				break;
		}		
		if (_nextAction != null)
			if (_nextAction.getIntentions().contains(intention))
				_nextAction = null;
	}

	@Override
	public final void notifyEvent(CtrlEvent evt)
	{
		notifyEvent(evt, null, null);
	}

	@Override
	public final void notifyEvent(CtrlEvent evt, Object arg0)
	{
		notifyEvent(evt, arg0, null);
	}

	@Override
	public final void notifyEvent(CtrlEvent evt, Object arg0, Object arg1)
	{
		if(!_actor.isVisible() || !_actor.hasAI())
		{
			return;
		}

		switch(evt)
		{
			case EVT_THINK:
				onEvtThink();
				break;
			case EVT_ATTACKED:
				onEvtAttacked((L2Character) arg0);
				break;
			case EVT_AGGRESSION:
				onEvtAggression((L2Character) arg0, ((Number) arg1).intValue());
				break;
			case EVT_STUNNED:
				onEvtStunned((L2Character) arg0);
				break;
			case EVT_SLEEPING:
				onEvtSleeping((L2Character) arg0);
				break;
			case EVT_ROOTED:
				onEvtRooted((L2Character) arg0);
				break;
			case EVT_CONFUSED:
				onEvtConfused((L2Character) arg0);
				break;
			case EVT_MUTED:
				onEvtMuted((L2Character) arg0);
				break;
			case EVT_READY_TO_ACT:
				if (!_actor.isCastingNow() && !_actor.isCastingSimultaneouslyNow())
				onEvtReadyToAct();
				break;
			case EVT_USER_CMD:
				onEvtUserCmd(arg0, arg1);
				break;
			case EVT_ARRIVED:
				if (!_actor.isCastingNow() && !_actor.isCastingSimultaneouslyNow())
				onEvtArrived();
				break;
			case EVT_ARRIVED_REVALIDATE:
				if (_actor.isMoving())
				onEvtArrivedRevalidate();
				break;
			case EVT_ARRIVED_BLOCKED:
				onEvtArrivedBlocked((L2CharPosition) arg0);
				break;
			case EVT_FORGET_OBJECT:
				onEvtForgetObject((L2Object) arg0);
				break;
			case EVT_CANCEL:
				onEvtCancel();
				break;
			case EVT_DEAD:
				onEvtDead();
				break;
			case EVT_FAKE_DEATH:
				onEvtFakeDeath();
				break;
			case EVT_FINISH_CASTING:
				onEvtFinishCasting();
				break;
		default:
			break;
		}
		// Do next action.
				if (_nextAction != null)
					if (_nextAction.getEvents().contains(evt))
						_nextAction.doAction();
	}

	protected abstract void onIntentionIdle();

	protected abstract void onIntentionActive();

	protected abstract void onIntentionRest();

	protected abstract void onIntentionAttack(L2Character target);

	protected abstract void onIntentionCast(L2Skill skill, L2Object target);

	protected abstract void onIntentionMoveTo(L2CharPosition destination);

	protected abstract void onIntentionMoveToInABoat(L2CharPosition destination, L2CharPosition origin);

	protected abstract void onIntentionFollow(L2Character target);

	protected abstract void onIntentionPickUp(L2Object item);

	protected abstract void onIntentionInteract(L2Object object);

	protected abstract void onEvtThink();

	protected abstract void onEvtAttacked(L2Character attacker);

	protected abstract void onEvtAggression(L2Character target, int aggro);

	protected abstract void onEvtStunned(L2Character attacker);

	protected abstract void onEvtSleeping(L2Character attacker);

	protected abstract void onEvtRooted(L2Character attacker);

	protected abstract void onEvtConfused(L2Character attacker);

	protected abstract void onEvtMuted(L2Character attacker);

	protected abstract void onEvtReadyToAct();

	protected abstract void onEvtUserCmd(Object arg0, Object arg1);

	protected abstract void onEvtArrived();

	protected abstract void onEvtArrivedRevalidate();

	protected abstract void onEvtArrivedBlocked(L2CharPosition blocked_at_pos);

	protected abstract void onEvtForgetObject(L2Object object);

	protected abstract void onEvtCancel();

	protected abstract void onEvtDead();

	protected abstract void onEvtFakeDeath();

	protected abstract void onEvtFinishCasting();

	protected void clientActionFailed()
	{
		if(_actor instanceof L2PcInstance)
		{
			_actor.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}

	protected void moveToPawn(L2Object pawn, int offset)
	{
		if(!_actor.isMovementDisabled())
		{
			if(offset < 10)
			{
				offset = 10;
			}

			boolean sendPacket = true;
			if(_clientMoving && _target == pawn)
			{
				if(_clientMovingToPawnOffset == offset)
				{
					if(GameTimeController.getGameTicks() < _moveToPawnTimeout)
					{
						return;
					}

					sendPacket = false;
				}
				else if(_actor.isOnGeodataPath())
				{
					if(GameTimeController.getGameTicks() < _moveToPawnTimeout + 10)
					{
						return;
					}
				}
			}

			_clientMoving = true;
			_clientMovingToPawnOffset = offset;
			_target = pawn;
			_moveToPawnTimeout = GameTimeController.getGameTicks();
			_moveToPawnTimeout += 200 / GameTimeController.MILLIS_IN_TICK;

			if(pawn == null || _accessor == null)
			{
				return;
			}

			_accessor.moveTo(pawn.getX(), pawn.getY(), pawn.getZ(), offset);

			if(!_actor.isMoving())
			{
				_actor.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			if(pawn instanceof L2Character)
			{
				if(_actor.isOnGeodataPath())
				{
					_actor.broadcastPacket(new CharMoveToLocation(_actor));
					_clientMovingToPawnOffset = 0;
				}
				else if(sendPacket)
				{
					_actor.broadcastPacket(new MoveToPawn(_actor, (L2Character) pawn, offset));
				}
			}
			else
			{
				_actor.broadcastPacket(new CharMoveToLocation(_actor));
			}
		}
		else
		{
			_actor.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}

	protected void moveTo(int x, int y, int z)
	{
		moveTo(x, y, z, 0);
	}
	
	/**
	 * Move the actor to Location (x,y,z,offset) server side AND client side by sending Server->Client packet CharMoveToLocation <I>(broadcast)</I>.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Low level function, used by AI subclasses</B></FONT><BR>
	 * <BR>
	 * @param x
	 * @param y
	 * @param z
	 * @param offset
	 */
	protected void moveTo(int x, int y, int z, int offset)
	{
		// Check if actor can move
		if (_actor.isMovementDisabled())
		{
			_actor.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Set AI movement data
		_clientMoving = true;
		
		if (_accessor == null)
			return;
		
		// Calculate movement data for a move to location action and add the actor to movingObjects of GameTimeController
		_accessor.moveTo(x, y, z, offset);
		
		if (!_actor.isMoving())
		{
			_actor.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Send a Server->Client packet CharMoveToLocation to the actor and all L2PcInstance in its _knownPlayers
		_actor.broadcastPacket(new CharMoveToLocation(_actor));
	}

	protected void moveToInABoat(L2CharPosition destination, L2CharPosition origin)
	{
		if(!_actor.isMovementDisabled())
		{
			if(((L2PcInstance) _actor).getBoat() != null)
			{
				MoveToLocationInVehicle msg = new MoveToLocationInVehicle(_actor, destination, origin);
				_actor.broadcastPacket(msg);
				msg = null;
			}

		}
		else
		{
			_actor.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}

	protected void clientStopMoving(L2CharPosition pos)
	{
		if(_actor.isMoving())
		{
			_accessor.stopMove(pos);
		}

		_clientMovingToPawnOffset = 0;

		if(_clientMoving || pos != null)
		{
			_clientMoving = false;

			StopMove msg = new StopMove(_actor);
			_actor.broadcastPacket(msg);
			msg = null;

			if(pos != null)
			{
				StopRotation sr = new StopRotation(_actor, pos.heading, 0);
				_actor.sendPacket(sr);
				_actor.broadcastPacket(sr);
				sr = null;
			}
		}
	}

	protected void clientStoppedMoving()
	{
		if(_clientMovingToPawnOffset > 0)
		{
			_clientMovingToPawnOffset = 0;
			StopMove msg = new StopMove(_actor);
			_actor.broadcastPacket(msg);
			msg = null;
		}
		_clientMoving = false;
	}

	public boolean isAutoAttacking()
	{
		return _clientAutoAttacking;
	}

	public void setAutoAttacking(boolean isAutoAttacking)
	{
		_clientAutoAttacking = isAutoAttacking;
	}

	public void clientStartAutoAttack()
	{
		if(_actor instanceof L2Summon)
		{
			L2Summon summon = (L2Summon) _actor;
			if(summon.getOwner() != null)
			{
				summon.getOwner().getAI().clientStartAutoAttack();
			}

			return;
		}

		if(!isAutoAttacking())
		{
			if(_actor instanceof L2PcInstance && ((L2PcInstance)_actor).getPet() != null)
			{
				((L2PcInstance)_actor).getPet().broadcastPacket(new AutoAttackStart(((L2PcInstance)_actor).getPet().getObjectId()));
			}

			_actor.broadcastPacket(new AutoAttackStart(_actor.getObjectId()));
			setAutoAttacking(true);
		}

		AttackStanceTaskManager.getInstance().addAttackStanceTask(_actor);
	}

	public void clientStopAutoAttack()
	{
		if(_actor instanceof L2Summon)
		{
			L2Summon summon = (L2Summon) _actor;
			if(summon.getOwner() != null)
			{
				summon.getOwner().getAI().clientStopAutoAttack();
			}

			return;
		}

		if(_actor instanceof L2PcInstance)
		{
			if(!AttackStanceTaskManager.getInstance().getAttackStanceTask(_actor) && isAutoAttacking())
			{
				AttackStanceTaskManager.getInstance().addAttackStanceTask(_actor);
			}
		}
		else if(isAutoAttacking())
		{
			_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
			setAutoAttacking(false);
		}
	}

	protected void clientNotifyDead()
	{
		Die msg = new Die(_actor);
		_actor.broadcastPacket(msg);
		msg = null;

		_intention = AI_INTENTION_IDLE;
		_target = null;
		_castTarget = null;
		_attackTarget = null;

		stopFollow();
	}

	public void describeStateToPlayer(L2PcInstance player)
	{
		if(_clientMoving)
		{
			if(_clientMovingToPawnOffset != 0 && _followTarget != null)
			{
				MoveToPawn msg = new MoveToPawn(_actor, _followTarget, _clientMovingToPawnOffset);
				player.sendPacket(msg);
				msg = null;
			}
			else
			{
				CharMoveToLocation msg = new CharMoveToLocation(_actor);
				player.sendPacket(msg);
				msg = null;
			}
		}
	}

	public synchronized void startFollow(L2Character target)
	{
		if(_followTask != null)
		{
			_followTask.cancel(false);
			_followTask = null;
		}

		_followTarget = target;
		_followTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new FollowTask(), 5, FOLLOW_INTERVAL);
	}

	public synchronized void startFollow(L2Character target, int range)
	{
		if(_followTask != null)
		{
			_followTask.cancel(false);
			_followTask = null;
		}

		_followTarget = target;
		_followTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new FollowTask(range), 5, ATTACK_FOLLOW_INTERVAL);
	}

	public synchronized void stopFollow()
	{
		if(_followTask != null)
		{
			_followTask.cancel(false);
			_followTask = null;
		}
		_followTarget = null;
	}

	protected L2Character getFollowTarget()
	{
		return _followTarget;
	}

	protected L2Object getTarget()
	{
		return _target;
	}

	protected synchronized void setTarget(L2Object target)
	{
		_target = target;
	}

}