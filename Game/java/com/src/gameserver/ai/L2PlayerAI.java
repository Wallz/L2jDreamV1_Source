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
import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;
import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_INTERACT;
import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_PICK_UP;
import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_REST;

import java.util.EmptyStackException;
import java.util.Stack;

import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Character.AIAccessor;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2StaticObjectInstance;
import com.src.gameserver.model.actor.knownlist.ObjectKnownList.KnownListAsynchronousUpdateTask;
import com.src.gameserver.model.actor.position.L2CharPosition;
import com.src.gameserver.thread.ThreadPoolManager;

public class L2PlayerAI extends L2CharacterAI
{
	private boolean _thinking;

	public class IntentionCommand
	{
		protected CtrlIntention _crtlIntention;
		protected Object _arg0, _arg1;

		protected IntentionCommand(CtrlIntention pIntention, Object pArg0, Object pArg1)
		{
			_crtlIntention = pIntention;
			_arg0 = pArg0;
			_arg1 = pArg1;
		}
	}

	private Stack<IntentionCommand> _interuptedIntentions = new Stack<IntentionCommand>();

	public L2PlayerAI(AIAccessor accessor)
	{
		super(accessor);
	}

	@Override
	synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		if(intention != AI_INTENTION_CAST)
		{
			super.changeIntention(intention, arg0, arg1);
			return;
		}

		if(intention == _intention && arg0 == _intentionArg0 && arg1 == _intentionArg1)
		{
			super.changeIntention(intention, arg0, arg1);
			return;
		}

		_interuptedIntentions.push(new IntentionCommand(_intention, _intentionArg0, _intentionArg1));
		super.changeIntention(intention, arg0, arg1);
	}

	@Override
	protected void onEvtFinishCasting()
	{
		if(_skill != null && _skill.isOffensive())
		{
			_interuptedIntentions.clear();
		}

		if(getIntention() == AI_INTENTION_CAST)
		{
			if(!_interuptedIntentions.isEmpty())
			{
				IntentionCommand cmd = null;
				try
				{
					cmd = _interuptedIntentions.pop();
				}
				catch(EmptyStackException ese)
				{
				}

				if(cmd != null && cmd._crtlIntention != AI_INTENTION_CAST)
				{
					setIntention(cmd._crtlIntention, cmd._arg0, cmd._arg1);
					cmd = null;
				}
				else
				{
					setIntention(AI_INTENTION_IDLE);
				}

				cmd = null;
			}
			else
			{
				setIntention(AI_INTENTION_IDLE);
			}
		}
	}

	@Override
	protected void onIntentionRest()
	{
		if(getIntention() != AI_INTENTION_REST)
		{
			changeIntention(AI_INTENTION_REST, null, null);
			setTarget(null);

			if(getAttackTarget() != null)
			{
				setAttackTarget(null);
			}

			clientStopMoving(null);
		}
	}

	@Override
    protected void clientStopMoving(L2CharPosition pos)
    {
    	super.clientStopMoving(pos);
        L2PcInstance _player = (L2PcInstance)_actor;
        if(_player.getSitdownTask())
        {
            _player.setSitdownTask(false);
            _player.sitDown();
        }
    }
    
	@Override
	protected void onIntentionActive()
	{
		setIntention(AI_INTENTION_IDLE);
	}

	@Override
	protected void clientNotifyDead()
	{
		_clientMovingToPawnOffset = 0;
		_clientMoving = false;

		super.clientNotifyDead();
	}

	private void thinkAttack()
	{
		L2Character target = getAttackTarget();
		if(target == null)
		{
			return;
		}

		if(checkTargetLostOrDead(target))
		{
			if(target != null)
			{
				setAttackTarget(null);
			}
			return;
		}

		if(maybeMoveToPawn(target, _actor.getPhysicalAttackRange()))
		{
			return;
		}

		_accessor.doAttack(target);
		target = null;
		return;
	}

	private void thinkCast()
	{
		
		L2Character target = getCastTarget();
		
		if(checkTargetLost(target))
		{
			if(_skill.isOffensive() && getAttackTarget() != null)
			{
				setCastTarget(null);
			}
			_actor.setIsCastingNow(false);
			return;
		}
		
		if(target != null)
		{
			if(maybeMoveToPawn(target, _actor.getMagicalAttackRange(_skill)))
			{
				_actor.setIsCastingNow(false);
				return;
			}
		}
		
		if(_skill.getHitTime() > 50)
		{
			clientStopMoving(null);
		}
		
		L2Object oldTarget = _actor.getTarget();
		if (oldTarget != null && target != null && oldTarget != target)
		{
			// Replace the current target by the cast target
			_actor.setTarget(getCastTarget());
			// Launch the Cast of the skill
			_accessor.doCast(_skill);
			// Restore the initial target
			_actor.setTarget(oldTarget);
		}
		else
			_accessor.doCast(_skill);
		
		target = null;
		oldTarget = null;
		
		return;
	}

	private void thinkPickUp()
	{
		if(_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			return;
		}

		L2Object target = getTarget();
		if(checkTargetLost(target))
		{
			return;
		}

		if(maybeMoveToPawn(target, 36))
		{
			return;
		}

		setIntention(AI_INTENTION_IDLE);
		((L2PcInstance.AIAccessor) _accessor).doPickupItem(target);

		target = null;

		return;
	}

	private void thinkInteract()
	{
		if(_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			return;
		}

		L2Object target = getTarget();
		if(checkTargetLost(target))
		{
			return;
		}

		if(maybeMoveToPawn(target, 36))
		{
			return;
		}

		if(!(target instanceof L2StaticObjectInstance))
		{
			((L2PcInstance.AIAccessor) _accessor).doInteract((L2Character) target);
		}

		target = null;

		setIntention(AI_INTENTION_IDLE);
		return;
	}

	@Override
	protected void onEvtThink()
	{
		if(_thinking && getIntention() != AI_INTENTION_CAST)
		{
			return;
		}

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

	@Override
	protected void onEvtArrivedRevalidate()
	{
		ThreadPoolManager.getInstance().executeTask(new KnownListAsynchronousUpdateTask(_actor));
		super.onEvtArrivedRevalidate();
	}

}