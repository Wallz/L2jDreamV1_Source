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

import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_CAST;
import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_FOLLOW;
import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;
import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_INTERACT;
import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_MOVE_TO;
import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_PICK_UP;
import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_REST;

import com.src.Config;
import com.src.gameserver.geo.GeoData;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.instance.L2BoatInstance;
import com.src.gameserver.model.actor.instance.L2DoorInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance.ItemLocation;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.position.L2CharPosition;
import com.src.gameserver.network.serverpackets.AutoAttackStop;
import com.src.gameserver.taskmanager.AttackStanceTaskManager;
import com.src.util.object.Point3D;

public class L2CharacterAI extends AbstractAI
{
	private static final int ZONE_PVP = 1;
	protected boolean _sitDownAfterAction = false;

	public static class IntentionCommand
	{
		protected final CtrlIntention _crtlIntention;
		protected final Object _arg0, _arg1;
		
		protected IntentionCommand(CtrlIntention pIntention, Object pArg0, Object pArg1)
		{
			_crtlIntention = pIntention;
			_arg0 = pArg0;
			_arg1 = pArg1;
		}
		
		public CtrlIntention getCtrlIntention()
		{
			return _crtlIntention;
		}
	}
	
	@Override
	protected void onEvtAttacked(L2Character attacker)
	{
		clientStartAutoAttack();
	}
	
	protected void onEvtEvaded(L2Character attacker)
	{
		// do nothing
	}

	public L2CharacterAI(L2Character.AIAccessor accessor)
	{
		super(accessor);
	}

	@Override
	protected void onIntentionIdle()
	{
		changeIntention(AI_INTENTION_IDLE, null, null);

		setCastTarget(null);
		setAttackTarget(null);

		clientStopMoving(null);

		clientStopAutoAttack();
	}

	protected void onIntentionActive(L2Character target)
	{
		if(target instanceof L2PcInstance && _actor instanceof L2PcInstance)
		{
			if(((L2PcInstance) _actor).getKarma() > 0 && _actor.getLevel() - target.getLevel() >= 10 && ((L2Playable) target).getProtectionBlessing() && !target.isInsideZone(ZONE_PVP))
			{
				clientActionFailed();
				return;
			}
		}

		if(getIntention() != AI_INTENTION_ACTIVE)
		{
			changeIntention(AI_INTENTION_ACTIVE, null, null);

			setCastTarget(null);
			setAttackTarget(null);

			clientStopMoving(null);

			clientStopAutoAttack();

			if(_actor instanceof L2Attackable)
			{
				((L2Npc) _actor).startRandomAnimationTimer();
			}

			onEvtThink();
		}
	}

	@Override
	protected void onIntentionRest()
	{
		setIntention(AI_INTENTION_IDLE);
	}

	@Override
	protected void onIntentionAttack(L2Character target)
	{
		if(target == null)
		{
			clientActionFailed();
			return;
		}

		if(getIntention() == AI_INTENTION_REST)
		{
			clientActionFailed();
			return;
		}

		if(_actor.isAllSkillsDisabled() || _actor.isCastingNow() || _actor.isAfraid())
		{
			clientActionFailed();
			return;
		}

		if(getIntention() == AI_INTENTION_ATTACK)
		{
			if(getAttackTarget() != target)
			{
				setAttackTarget(target);

				stopFollow();

				notifyEvent(CtrlEvent.EVT_THINK, null);

			}
			else
			{
				clientActionFailed();
			}
		}
		else
		{
			changeIntention(AI_INTENTION_ATTACK, target, null);

			setAttackTarget(target);

			stopFollow();

			notifyEvent(CtrlEvent.EVT_THINK, null);
		}
	}

	@Override
	protected void onIntentionCast(L2Skill skill, L2Object target)
	{
		if(getIntention() == AI_INTENTION_REST && skill.isMagic())
		{
			clientActionFailed();
			return;
		}

		if(_actor.isAllSkillsDisabled())
		{
			clientActionFailed();
			return;
		}

		if(_actor.isMuted() && skill.isMagic())
		{
			clientActionFailed();
			return;
		}

		if(target instanceof L2PcInstance && _actor instanceof L2PcInstance)
		{
			if(((L2PcInstance) _actor).getKarma() > 0 && _actor.getLevel() - ((L2PcInstance) target).getLevel() >= 10 && ((L2Playable) target).getProtectionBlessing() && !((L2Character) target).isInsideZone(ZONE_PVP))
			{
				clientActionFailed();
				return;
			}
		}

		setCastTarget((L2Character) target);

		if(skill.getHitTime() > 50)
		{
			_actor.abortAttack();
		}

		_skill = skill;

		changeIntention(AI_INTENTION_CAST, skill, target);

		setSitDownAfterAction(false);

		notifyEvent(CtrlEvent.EVT_THINK, null);
	}

	@Override
	protected void onIntentionMoveTo(L2CharPosition pos)
	{
		if(getIntention() == AI_INTENTION_REST)
		{
			clientActionFailed();
			return;
		}

		if(_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			clientActionFailed();
			return;
		}

		changeIntention(AI_INTENTION_MOVE_TO, pos, null);

		clientStopAutoAttack();

		_actor.abortAttack();

		moveTo(pos.x, pos.y, pos.z);
	}

	@Override
	protected void onIntentionMoveToInABoat(L2CharPosition destination, L2CharPosition origin)
	{
		if(getIntention() == AI_INTENTION_REST)
		{
			clientActionFailed();
			return;
		}

		if(_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			clientActionFailed();
			return;
		}

		clientStopAutoAttack();

		_actor.abortAttack();

		moveToInABoat(destination, origin);
	}

	@Override
	protected void onIntentionFollow(L2Character target)
	{
		if(getIntention() == AI_INTENTION_REST)
		{
			clientActionFailed();
			return;
		}

		if(_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			clientActionFailed();
			return;
		}

		if(_actor.isImobilised() || _actor.isRooted())
		{
			clientActionFailed();
			return;
		}

		if(_actor.isDead())
		{
			clientActionFailed();
			return;
		}

		if(_actor == target)
		{
			clientActionFailed();
			return;
		}

		clientStopAutoAttack();

		changeIntention(AI_INTENTION_FOLLOW, target, null);

		startFollow(target);
	}

	@Override
	protected void onIntentionPickUp(L2Object object)
	{
		if(getIntention() == AI_INTENTION_REST)
		{
			clientActionFailed();
			return;
		}

		if(_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			clientActionFailed();
			return;
		}

		if(object.getX() == 0 && object.getY() == 0)
		{
			if(object instanceof L2ItemInstance && ((L2ItemInstance) object).getLocation() != L2ItemInstance.ItemLocation.VOID) 
			{ 
				L2PcInstance player = null; 
				if(getActor() instanceof L2PcInstance) 
					player = (L2PcInstance) getActor(); 
				else if(getActor() instanceof L2Summon) 
					player = ((L2Summon) getActor()).getOwner(); 

				if(player != null) 
				{ 
					player.sendMessage("Sorry, but this item is buged, you can`t pick up it.");
					_log.warn("Item coordinates is 0! :: Item location is: " + ((L2ItemInstance) object).getLocation() + " :: player name: " + player.getName());
					return;
				}
			}

			object.setXYZ(getActor().getX(), getActor().getY(), getActor().getZ() + 5);
		}

		if(object instanceof L2ItemInstance && ((L2ItemInstance)object).getLocation() != ItemLocation.VOID)
		{
			return;
		}

		clientStopAutoAttack();

		changeIntention(AI_INTENTION_PICK_UP, object, null);

		setTarget(object);

		moveToPawn(object, 20);
	}

	@Override
	protected void onIntentionInteract(L2Object object)
	{
		if(getIntention() == AI_INTENTION_REST)
		{
			clientActionFailed();
			return;
		}

		if(_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			clientActionFailed();
			return;
		}

		clientStopAutoAttack();

		if(getIntention() != AI_INTENTION_INTERACT)
		{
			changeIntention(AI_INTENTION_INTERACT, object, null);
			setTarget(object);
			moveToPawn(object, 60);
		}
	}

	@Override
	protected void onEvtThink()
	{
	}

	@Override
	protected void onEvtAggression(L2Character target, int aggro)
	{
	}

	@Override
	protected void onEvtStunned(L2Character attacker)
	{
		_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
		if(AttackStanceTaskManager.getInstance().getAttackStanceTask(_actor))
		{
			AttackStanceTaskManager.getInstance().removeAttackStanceTask(_actor);
		}
		setAutoAttacking(false);
		clientStopMoving(null);
		onEvtAttacked(attacker);
	}

	@Override
	protected void onEvtSleeping(L2Character attacker)
	{
		_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
		if(AttackStanceTaskManager.getInstance().getAttackStanceTask(_actor))
		{
			AttackStanceTaskManager.getInstance().removeAttackStanceTask(_actor);
		}
		setAutoAttacking(false);
		clientStopMoving(null);
	}

	@Override
	protected void onEvtRooted(L2Character attacker)
	{
		clientStopMoving(null);
		onEvtAttacked(attacker);
	}

	@Override
	protected void onEvtConfused(L2Character attacker)
	{
		clientStopMoving(null);
		onEvtAttacked(attacker);
	}

	@Override
	protected void onEvtMuted(L2Character attacker)
	{
		onEvtAttacked(attacker);
	}

	@Override
	protected void onEvtReadyToAct()
	{
		onEvtThink();
	}

	@Override
	protected void onEvtUserCmd(Object arg0, Object arg1)
	{
	}

	@Override
	protected void onEvtArrived()
	{
		if(_accessor.getActor() instanceof L2PcInstance)
		{
			((L2PcInstance) _accessor.getActor()).revalidateZone(true);
		}
		else
		{
			_accessor.getActor().revalidateZone();
		}

		if(_accessor.getActor().moveToNextRoutePoint())
		{
			return;
		}

		if(_accessor.getActor() instanceof L2Attackable)
		{
			((L2Attackable) _accessor.getActor()).setisReturningToSpawnPoint(false);
		}

		clientStoppedMoving();

		if(getIntention() == AI_INTENTION_MOVE_TO)
		{
			if((this._sitDownAfterAction) && (this._actor instanceof L2PcInstance))
			{
				((L2PcInstance)this._actor).sitDown();
				this._sitDownAfterAction = false;
			}

			setIntention(AI_INTENTION_ACTIVE);
		}

		onEvtThink();

		if(_actor instanceof L2BoatInstance)
		{
			((L2BoatInstance) _actor).evtArrived();
		}
	}

	@Override
	protected void onEvtArrivedRevalidate()
	{
		onEvtThink();
	}

	@Override
	protected void onEvtArrivedBlocked(L2CharPosition blocked_at_pos)
	{
		clientStopMoving(blocked_at_pos);

		if(getIntention() == AI_INTENTION_MOVE_TO || getIntention() == AI_INTENTION_CAST)
		{
			setIntention(AI_INTENTION_ACTIVE);
		}

		onEvtThink();
	}

	@Override
	protected void onEvtForgetObject(L2Object object)
	{
		if(getTarget() == object)
		{
			setTarget(null);

			if(getIntention() == AI_INTENTION_INTERACT)
			{
				setIntention(AI_INTENTION_ACTIVE);
			}
			else if(getIntention() == AI_INTENTION_PICK_UP)
			{
				setIntention(AI_INTENTION_ACTIVE);
			}
		}

		if(getAttackTarget() == object)
		{
			setAttackTarget(null);
			setIntention(AI_INTENTION_ACTIVE);
		}

		if(getCastTarget() == object)
		{
			setCastTarget(null);
			setIntention(AI_INTENTION_ACTIVE);
		}

		if(getFollowTarget() == object)
		{
			clientStopMoving(null);
			stopFollow();
			setIntention(AI_INTENTION_ACTIVE);
		}

		if(_actor == object)
		{
			setTarget(null);
			setAttackTarget(null);
			setCastTarget(null);
			stopFollow();
			clientStopMoving(null);
			changeIntention(AI_INTENTION_IDLE, null, null);
		}
	}

	@Override
	protected void onEvtCancel()
	{
		_actor.abortCast();
		stopFollow();

		if(!AttackStanceTaskManager.getInstance().getAttackStanceTask(_actor))
		{
			_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
		}

		onEvtThink();
	}

	@Override
	protected void onEvtDead()
	{
		stopFollow();
		clientNotifyDead();
		if(!(_actor instanceof L2PcInstance))
		{
			_actor.setWalking();
		}
	}

	@Override
	protected void onEvtFakeDeath()
	{
		stopFollow();
		clientStopMoving(null);
		_intention = AI_INTENTION_IDLE;
		setTarget(null);
		setCastTarget(null);
		setAttackTarget(null);
	}

	@Override
	protected void onEvtFinishCasting()
	{
	}

	protected boolean maybeMoveToPosition(Point3D worldPosition, int offset)
	{
		if (worldPosition == null)
		{
			_log.warn("maybeMoveToPosition: worldPosition == NULL!");
			return false;
		}
		
		if (offset < 0)
			return false; // skill radius -1
			
		if (!_actor.isInsideRadius(worldPosition.getX(), worldPosition.getY(), offset + _actor.getTemplate().getCollisionRadius(), false))
		{
			if (_actor.isMovementDisabled())
				return true;
			
			if (!_actor.isRunning() && !(this instanceof L2PlayerAI) && !(this instanceof L2SummonAI))
				_actor.setRunning();
			
			stopFollow();
			
			int x = _actor.getX();
			int y = _actor.getY();
			
			double dx = worldPosition.getX() - x;
			double dy = worldPosition.getY() - y;
			
			double dist = Math.sqrt(dx * dx + dy * dy);
			
			double sin = dy / dist;
			double cos = dx / dist;
			
			dist -= offset - 5;
			
			x += (int) (dist * cos);
			y += (int) (dist * sin);
			
			moveTo(x, y, worldPosition.getZ());
			return true;
		}
		
		if (getFollowTarget() != null)
			stopFollow();
		
		return false;
	}
	
	protected boolean maybeMoveToPawn(L2Object target, int offset)
	{
		if(target == null)
		{
			return false;
		}

		if(offset < 0)
		{
			return false;
		}

		offset += _actor.getTemplate().collisionRadius;

		if(target instanceof L2Character)
		{
			offset += ((L2Character) target).getTemplate().collisionRadius;
		}

		if(!_actor.isInsideRadius(target, offset, false, false))
		{
			if(getFollowTarget() != null)
			{
				if(getAttackTarget() != null && _actor instanceof L2Playable && target instanceof L2Playable)
				{
					if(getAttackTarget() == getFollowTarget())
					{
						boolean isGM = _actor instanceof L2PcInstance ? ((L2PcInstance) _actor).isGM() : false;
						if(L2Character.isInsidePeaceZone(_actor, target) && !isGM)
						{
							stopFollow();
							setIntention(AI_INTENTION_IDLE);
							return true;
						}
					}
				}

				if(!_actor.isInsideRadius(target, 2000, false, false))
				{
					stopFollow();
					setIntention(AI_INTENTION_IDLE);
					return true;
				}

				if(!_actor.isInsideRadius(target, offset + 100, false, false))
				{
					return true;
				}

				stopFollow();
				return false;
			}

			if(_actor.isMovementDisabled())
			{
				return true;
			}

			if(!_actor.isRunning() && !(this instanceof L2PlayerAI))
			{
				_actor.setRunning();
			}

			stopFollow();

			if(target instanceof L2Character && !(target instanceof L2DoorInstance))
			{
				if(((L2Character) target).isMoving())
				{
					offset -= 100;
				}
				if(offset < 5)
				{
					offset = 5;
				}

				startFollow((L2Character) target, offset);
			}
			else
			{
				moveToPawn(target, offset);
			}
			return true;
		}

		if(getFollowTarget() != null)
		{
			stopFollow();
		}

		return false;
	}

	protected boolean checkTargetLostOrDead(L2Character target)
	{
		if(target == null || target.isAlikeDead())
		{
			if(target != null && target.isFakeDeath())
			{
				target.stopFakeDeath(null);
				return false;
			}

			setIntention(AI_INTENTION_ACTIVE);

			return true;
		}
		return false;
	}

	protected boolean checkTargetLost(L2Object target)
	{
		if(target instanceof L2PcInstance)
		{
			L2PcInstance target2 = (L2PcInstance) target;

			if(target2.isFakeDeath())
			{
				target2.stopFakeDeath(null);
				return false;
			}
			target2 = null;
		}

		if(target == null)
		{
			setIntention(AI_INTENTION_ACTIVE);
			return true;
		}

		if(_actor != null && _skill != null && _skill.isOffensive() && _skill.getSkillRadius() > 0 && Config.GEODATA > 0 && !GeoData.getInstance().canSeeTarget(_actor, target))
		{
			setIntention(AI_INTENTION_ACTIVE);
			return true;
		}

		return false;
	}

	@Override
	protected void onIntentionActive()
	{
		if(getIntention() != AI_INTENTION_ACTIVE)
		{
			changeIntention(AI_INTENTION_ACTIVE, null, null);
			setCastTarget(null);
			setAttackTarget(null);
			clientStopMoving(null);
			clientStopAutoAttack();

			if(_actor instanceof L2Attackable)
			{
				((L2Npc) _actor).startRandomAnimationTimer();
			}

			onEvtThink();
		}
	}

	public void setSitDownAfterAction(boolean val)
	{
		this._sitDownAfterAction = val;
	}

	public IntentionCommand getNextIntention()
	{
		return null;
	}
}