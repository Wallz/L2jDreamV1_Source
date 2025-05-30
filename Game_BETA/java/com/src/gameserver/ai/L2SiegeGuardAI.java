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
import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;

import java.util.concurrent.Future;

import com.src.Config;
import com.src.gameserver.GameTimeController;
import com.src.gameserver.geo.GeoData;
import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.instance.L2DoorInstance;
import com.src.gameserver.model.actor.instance.L2MonsterInstance;
import com.src.gameserver.model.actor.instance.L2NpcInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2SiegeGuardInstance;
import com.src.gameserver.templates.skills.L2SkillType;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.random.Rnd;

public class L2SiegeGuardAI extends L2CharacterAI implements Runnable
{
	private static final int MAX_ATTACK_TIMEOUT = 300;

	private Future<?> _aiTask;

	private int _attackTimeout;

	private int _globalAggro;

	private boolean _thinking;

	private int _attackRange;

	public L2SiegeGuardAI(L2Character.AIAccessor accessor)
	{
		super(accessor);

		_attackTimeout = Integer.MAX_VALUE;
		_globalAggro = -10;

		_attackRange = ((L2Attackable) _actor).getPhysicalAttackRange();
	}

	@Override
	public void run()
	{
		onEvtThink();
	}

	private boolean autoAttackCondition(L2Character target)
	{
		if(target == null || target instanceof L2SiegeGuardInstance || target instanceof L2NpcInstance || target instanceof L2DoorInstance || target.isAlikeDead())
		{
			return false;
		}

		if(target.isInvul())
		{
			if(target instanceof L2PcInstance && ((L2PcInstance) target).isGM())
			{
				return false;
			}

			if(target instanceof L2Summon && ((L2Summon) target).getOwner().isGM())
			{
				return false;
			}
		}

		if(target instanceof L2Summon)
		{
			L2PcInstance owner = ((L2Summon) target).getOwner();
			if(_actor.isInsideRadius(owner, 1000, true, false))
			{
				target = owner;
			}

			owner = null;
		}

		if(target instanceof L2PcInstance)
		{
			if(((L2PcInstance) target).isSilentMoving() && !_actor.isInsideRadius(target, 250, false, false))
			{
				return false;
			}
		}
		return _actor.isAutoAttackable(target) && GeoData.getInstance().canSeeTarget(_actor, target);

	}

	@Override
	synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		((L2Attackable) _actor).setisReturningToSpawnPoint(false);

		if(intention == AI_INTENTION_IDLE)
		{
			if(!_actor.isAlikeDead())
			{
				L2Attackable npc = (L2Attackable) _actor;

				if(npc.getKnownList().getKnownPlayers().size() > 0)
				{
					intention = AI_INTENTION_ACTIVE;
				}
				else
				{
					intention = AI_INTENTION_IDLE;
				}

				npc = null;
			}

			if(intention == AI_INTENTION_IDLE)
			{
				super.changeIntention(AI_INTENTION_IDLE, null, null);

				if(_aiTask != null)
				{
					_aiTask.cancel(true);
					_aiTask = null;
				}

				_accessor.detachAI();

				return;
			}
		}

		super.changeIntention(intention, arg0, arg1);

		if(_aiTask == null)
		{
			_aiTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, 1000, 1000);
		}
	}

	@Override
	protected void onIntentionAttack(L2Character target)
	{
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();

		super.onIntentionAttack(target);
	}

	private void thinkActive()
	{
		L2Attackable npc = (L2Attackable) _actor;

		if(_globalAggro != 0)
		{
			if(_globalAggro < 0)
			{
				_globalAggro++;
			}
			else
			{
				_globalAggro--;
			}
		}

		if(_globalAggro >= 0)
		{
			for(L2Character target : npc.getKnownList().getKnownCharactersInRadius(_attackRange))
			{
				if(target == null)
				{
					continue;
				}

				if(autoAttackCondition(target))
				{
					int hating = npc.getHating(target);

					if(hating == 0)
					{
						npc.addDamageHate(target, 0, 1);
					}
				}
			}

			L2Character hated;

			if(_actor.isConfused())
			{
				hated = _attackTarget;
			}
			else
			{
				hated = npc.getMostHated();
			}

			if(hated != null)
			{
				int aggro = npc.getHating(hated);

				if(aggro + _globalAggro > 0)
				{
					if(!_actor.isRunning())
					{
						_actor.setRunning();
					}

					setIntention(CtrlIntention.AI_INTENTION_ATTACK, hated, null);
				}

				return;
			}
			hated = null;

		}

		npc = null;

		((L2SiegeGuardInstance) _actor).returnHome();

		return;

	}

	private void attackPrepare()
	{
		L2Skill[] skills = null;
		double dist_2 = 0;
		int range = 0;
		L2SiegeGuardInstance sGuard = (L2SiegeGuardInstance) _actor;

		try
		{
			_actor.setTarget(_attackTarget);
			skills = _actor.getAllSkills();
			dist_2 = _actor.getPlanDistanceSq(_attackTarget.getX(), _attackTarget.getY());
			range = _actor.getPhysicalAttackRange() + _actor.getTemplate().collisionRadius + _attackTarget.getTemplate().collisionRadius;
		}
		catch(NullPointerException e)
		{
			_actor.setTarget(null);
			setIntention(AI_INTENTION_IDLE, null, null);
			return;
		}

		if(_attackTarget instanceof L2PcInstance && sGuard.getCastle().getSiege().checkIsDefender(((L2PcInstance) _attackTarget).getClan()))
		{
			sGuard.stopHating(_attackTarget);
			_actor.setTarget(null);
			setIntention(AI_INTENTION_IDLE, null, null);
			return;
		}

		if(!GeoData.getInstance().canSeeTarget(_actor, _attackTarget))
		{
			sGuard.stopHating(_attackTarget);
			_actor.setTarget(null);
			setIntention(AI_INTENTION_IDLE, null, null);
			return;
		}

		if(!_actor.isMuted() && dist_2 > (range + 20) * (range + 20))
		{
			if(_actor instanceof L2MonsterInstance && Rnd.nextInt(100) <= 5)
			{
				for(L2Skill sk : skills)
				{
					int castRange = sk.getCastRange();

					if((sk.getSkillType() == L2SkillType.BUFF || sk.getSkillType() == L2SkillType.HEAL || dist_2 >= castRange * castRange / 9 && dist_2 <= castRange * castRange && castRange > 70) && !_actor.isSkillDisabled(sk.getId()) && _actor.getCurrentMp() >= _actor.getStat().getMpConsume(sk) && !sk.isPassive())
					{
						L2Object OldTarget = _actor.getTarget();

						if(sk.getSkillType() == L2SkillType.BUFF || sk.getSkillType() == L2SkillType.HEAL)
						{
							boolean useSkillSelf = true;

							if(sk.getSkillType() == L2SkillType.HEAL && _actor.getCurrentHp() > (int) (_actor.getMaxHp() / 1.5))
							{
								useSkillSelf = false;
								break;
							}
							if(sk.getSkillType() == L2SkillType.BUFF)
							{
								L2Effect[] effects = _actor.getAllEffects();

								for(int i = 0; effects != null && i < effects.length; i++)
								{
									L2Effect effect = effects[i];

									if(effect.getSkill() == sk)
									{
										useSkillSelf = false;
										break;
									}
								}

								effects = null;
							}
							if(useSkillSelf)
							{
								_actor.setTarget(_actor);
							}
						}

						clientStopMoving(null);
						_accessor.doCast(sk);
						_actor.setTarget(OldTarget);

						OldTarget = null;

						return;
					}
				}
			}

			if(!(_actor.isAttackingNow()) && (_actor.getRunSpeed() == 0) && (_actor.getKnownList().knowsObject(_attackTarget)))
			{
				_actor.getKnownList().removeKnownObject(_attackTarget);
				_actor.setTarget(null);
				setIntention(AI_INTENTION_IDLE, null, null);
			}
			else
			{
				double dx = _actor.getX() - _attackTarget.getX();
				double dy = _actor.getY() - _attackTarget.getY();
				double dz = _actor.getZ() - _attackTarget.getZ();
				double homeX = _attackTarget.getX() - sGuard.getHomeX();
				double homeY = _attackTarget.getY() - sGuard.getHomeY();

				if(dx * dx + dy * dy > 10000 && homeX * homeX + homeY * homeY > 3240000)
				{
					_actor.getKnownList().removeKnownObject(_attackTarget);
					_actor.setTarget(null);
					setIntention(AI_INTENTION_IDLE, null, null);
				}
				else
				{
					if(dz * dz < 170 * 170)
					{
						moveToPawn(_attackTarget, range);
					}
				}
			}

			return;

		}
		else if(_actor.isMuted() && dist_2 > (range + 20) * (range + 20))
		{
			double dz = _actor.getZ() - _attackTarget.getZ();

			if(dz * dz < 170 * 170)
			{
				moveToPawn(_attackTarget, range);
			}

			return;
		}
		else if(dist_2 <= (range + 20) * (range + 20))
		{
			L2Character hated = null;

			if(_actor.isConfused())
			{
				hated = _attackTarget;
			}
			else
			{
				hated = ((L2Attackable) _actor).getMostHated();
			}

			if(hated == null)
			{
				setIntention(AI_INTENTION_ACTIVE, null, null);
				return;
			}

			if(hated != _attackTarget)
			{
				_attackTarget = hated;
			}

			hated = null;

			_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();

			if(!_actor.isMuted() && Rnd.nextInt(100) <= 5)
			{
				for(L2Skill sk : skills)
				{
					int castRange = sk.getCastRange();

					if(castRange * castRange >= dist_2 && castRange <= 70 && !sk.isPassive() && _actor.getCurrentMp() >= _actor.getStat().getMpConsume(sk) && !_actor.isSkillDisabled(sk.getId()))
					{
						L2Object OldTarget = _actor.getTarget();

						if(sk.getSkillType() == L2SkillType.BUFF || sk.getSkillType() == L2SkillType.HEAL)
						{
							boolean useSkillSelf = true;

							if(sk.getSkillType() == L2SkillType.HEAL && _actor.getCurrentHp() > (int) (_actor.getMaxHp() / 1.5))
							{
								useSkillSelf = false;
								break;
							}

							if(sk.getSkillType() == L2SkillType.BUFF)
							{
								L2Effect[] effects = _actor.getAllEffects();

								for(int i = 0; effects != null && i < effects.length; i++)
								{
									L2Effect effect = effects[i];

									if(effect.getSkill() == sk)
									{
										useSkillSelf = false;
										break;
									}
								}
							}
							if(useSkillSelf)
							{
								_actor.setTarget(_actor);
							}
						}

						clientStopMoving(null);
						_accessor.doCast(sk);
						_actor.setTarget(OldTarget);

						OldTarget = null;

						return;
					}
				}
			}
			_accessor.doAttack(_attackTarget);

			skills = null;
			sGuard = null;
		}
	}

	private void thinkAttack()
	{
		if(_attackTimeout < GameTimeController.getGameTicks())
		{
			if(_actor.isRunning())
			{
				_actor.setWalking();

				_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();
			}
		}

		if(_attackTarget == null || _attackTarget.isAlikeDead() || _attackTimeout < GameTimeController.getGameTicks())
		{
			if(_attackTarget != null)
			{
				L2Attackable npc = (L2Attackable) _actor;

				npc.stopHating(_attackTarget);

				npc = null;
			}

			_attackTimeout = Integer.MAX_VALUE;
			_attackTarget = null;

			setIntention(AI_INTENTION_ACTIVE, null, null);

			_actor.setWalking();
			return;
		}

		attackPrepare();
		factionNotify();
	}

	private final void factionNotify()
	{
		if(((L2Npc) _actor).getFactionId() == null || _attackTarget == null || _actor == null)
		{
			return;
		}

		if(_attackTarget.isInvul())
		{
			return;
		}

		for(L2Character cha : _actor.getKnownList().getKnownCharactersInRadius(1000))
		{
			if(cha == null)
			{
				continue;
			}

			if(!(cha instanceof L2Npc))
			{
				continue;
			}

			L2Npc npc = (L2Npc) cha;

			String faction_id = ((L2Npc) _actor).getFactionId();

			if(faction_id != npc.getFactionId())
			{
				continue;
			}

			faction_id = null;

			if((npc.getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE || npc.getAI().getIntention() == CtrlIntention.AI_INTENTION_ACTIVE) && _actor.isInsideRadius(npc, npc.getFactionRange(), false, true) && npc.getTarget() == null && _attackTarget.isInsideRadius(npc, npc.getFactionRange(), false, true))
			{
				if(Config.GEODATA > 0)
				{
					if(GeoData.getInstance().canSeeTarget(npc, _attackTarget))
					{
						L2CharacterAI ai = npc.getAI();
						if(ai != null)
						{
							ai.notifyEvent(CtrlEvent.EVT_AGGRESSION, _attackTarget, 1);
						}
					}
				}
				else
				{
					if(!npc.isDead() && Math.abs(_attackTarget.getZ() - npc.getZ()) < 600)
					{
						L2CharacterAI ai = npc.getAI();
						if(ai != null)
						{
							ai.notifyEvent(CtrlEvent.EVT_AGGRESSION, _attackTarget, 1);
						}
					}
				}
			}
			npc = null;
		}
	}

	@Override
	protected void onEvtThink()
	{
		if(_thinking)
		{
			return;
		}

		_thinking = true;

		try
		{
			if(getIntention() == AI_INTENTION_ACTIVE)
			{
				thinkActive();
			}
			else if(getIntention() == AI_INTENTION_ATTACK)
			{
				thinkAttack();
			}
		}
		finally
		{
			_thinking = false;
		}
	}

	@Override
	protected void onEvtAttacked(L2Character attacker)
	{
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();

		if(_globalAggro < 0)
		{
			_globalAggro = 0;
		}

		((L2Attackable) _actor).addDamageHate(attacker, 0, 1);

		if(!_actor.isRunning())
		{
			_actor.setRunning();
		}

		if(getIntention() != AI_INTENTION_ATTACK)
		{
			setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker, null);
		}

		super.onEvtAttacked(attacker);
	}

	@Override
	protected void onEvtAggression(L2Character target, int aggro)
	{
		if(_actor == null)
		{
			return;
		}

		L2Attackable me = (L2Attackable) _actor;

		if(target != null)
		{
			me.addDamageHate(target, 0, aggro);

			aggro = me.getHating(target);

			if(aggro <= 0)
			{
				if(me.getMostHated() == null)
				{
					_globalAggro = -25;
					me.clearAggroList();
					setIntention(AI_INTENTION_IDLE, null, null);
				}
				return;
			}

			if(getIntention() != CtrlIntention.AI_INTENTION_ATTACK)
			{
				if(!_actor.isRunning())
				{
					_actor.setRunning();
				}

				L2SiegeGuardInstance sGuard = (L2SiegeGuardInstance) _actor;

				double homeX = target.getX() - sGuard.getHomeX();
				double homeY = target.getY() - sGuard.getHomeY();

				if(homeX * homeX + homeY * homeY < 3240000)
				{
					setIntention(CtrlIntention.AI_INTENTION_ATTACK, target, null);
				}

				sGuard = null;
			}
		}
		else
		{
			if(aggro >= 0)
			{
				return;
			}

			L2Character mostHated = me.getMostHated();
			if(mostHated == null)
			{
				_globalAggro = -25;
				return;
			}
			else
			{
				for(L2Character aggroed : me.getAggroListRP().keySet())
				{
					me.addDamageHate(aggroed, 0, aggro);
				}
			}

			aggro = me.getHating(mostHated);
			if(aggro <= 0)
			{
				_globalAggro = -25;
				me.clearAggroList();
				setIntention(AI_INTENTION_IDLE, null, null);
			}
			mostHated = null;
		}
		me = null;
	}

	@Override
	protected void onEvtDead()
	{
		stopAITask();
		super.onEvtDead();
	}

	public void stopAITask()
	{
		if(_aiTask != null)
		{
			_aiTask.cancel(false);
			_aiTask = null;
		}
		_accessor.detachAI();
	}

}