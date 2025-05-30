/*
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
import com.src.gameserver.datatables.xml.TerritoryTable;
import com.src.gameserver.geo.GeoData;
import com.src.gameserver.managers.DimensionalRiftManager;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.instance.L2DoorInstance;
import com.src.gameserver.model.actor.instance.L2FestivalMonsterInstance;
import com.src.gameserver.model.actor.instance.L2FriendlyMobInstance;
import com.src.gameserver.model.actor.instance.L2GuardInstance;
import com.src.gameserver.model.actor.instance.L2MinionInstance;
import com.src.gameserver.model.actor.instance.L2MonsterInstance;
import com.src.gameserver.model.actor.instance.L2NpcInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PenaltyMonsterInstance;
import com.src.gameserver.model.actor.instance.L2RiftInvaderInstance;
import com.src.gameserver.model.quest.Quest;
import com.src.gameserver.templates.item.L2Weapon;
import com.src.gameserver.templates.item.L2WeaponType;
import com.src.gameserver.templates.skills.L2SkillType;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.random.Rnd;

public class L2AttackableAI extends L2CharacterAI implements Runnable
{
	private static final int RANDOM_WALK_RATE = 30;
	private static final int MAX_ATTACK_TIMEOUT = 1200;

	private Future<?> _aiTask;

	private int _attackTimeout;

	private int _globalAggro;

	private boolean _thinking;
	
	/**
	 * No random Walk NPC's List
	 */
	private final int[] noRandomWalkList = 
	{
		29007, 29008, 29011
	};

	public L2AttackableAI(L2Character.AIAccessor accessor)
	{
		super(accessor);
		_attackTimeout = Integer.MAX_VALUE;
		_globalAggro = -10;
	}

	@Override
	public void run()
	{
		onEvtThink();
	}

	private boolean autoAttackCondition(L2Character target)
	{
		if(target == null || !(_actor instanceof L2Attackable))
		{
			return false;
		}

		if (target instanceof L2DoorInstance || target.isAlikeDead())
			return false;
		
		L2Attackable me = (L2Attackable) _actor;

		if(me == null)
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
		
		if (target instanceof L2NpcInstance || target instanceof L2DoorInstance)
			return false;
		
		if (target instanceof L2PcInstance)
		{	
			if (target.isAlikeDead() || target.isFakeDeath() || !me.isInsideRadius(target, me.getAggroRange(), false, false) || Math.abs(_actor.getZ() - target.getZ()) > 300)
				return false;
			
			// Check if the AI isn't a Raid Boss and the target isn't in silent move mode
			if (!(me.isRaid()) && !(me.canSeeThroughSilentMove()) && ((L2PcInstance) target).isSilentMoving())
				return false;
			
			// Check if the target is a L2PcInstance
			L2PcInstance targetPlayer = target.getActingPlayer();
			if (targetPlayer != null)
			{
				if (targetPlayer.isGM())
				{
					// Check if the target isn't invulnerable ; requires to check GMs specially
					if (target.isInvul())
						return false;
					
					// Don't take the aggro if the GM has the access level below or equal to GM_DONT_TAKE_AGGRO
					if (!targetPlayer.getAccessLevel().canTakeAggro())
						return false;
				}
				
				// Check if player is an ally (comparing mem addr)
				if(me.getFactionId() == "varka" && ((L2PcInstance) target).isAlliedWithVarka())
					return false;

				if(me.getFactionId() == "ketra" && ((L2PcInstance) target).isAlliedWithKetra())
					return false;
				
				// check if the target is within the grace period for JUST getting up from fake death
				if (targetPlayer.isRecentFakeDeath())
					return false;
				
				if (targetPlayer.isInParty() && targetPlayer.getParty().isInDimensionalRift())
				{
					byte riftType = targetPlayer.getParty().getDimensionalRift().getType();
					byte riftRoom = targetPlayer.getParty().getDimensionalRift().getCurrentRoom();
					
					if (me instanceof L2RiftInvaderInstance && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(me.getX(), me.getY(), me.getZ()))
						return false;
				}
			}
		}

		if(target instanceof L2Summon)
		{
			L2PcInstance owner = ((L2Summon) target).getOwner();
			if(owner != null)
			{
				if(owner.isGM() && (owner.isInvul() || !owner.getAccessLevel().canTakeAggro()))
				{
					return false;
				}
				
				if (!me.isInsideRadius(target, me.getAggroRange(), true, false) || Math.abs(_actor.getZ() - target.getZ()) > 300)
					return false;
			}
		}
		if(_actor instanceof L2GuardInstance)
		{
			if(target instanceof L2PcInstance && ((L2PcInstance) target).getKarma() > 0)
			{
				return GeoData.getInstance().canSeeTarget(me, target);
			}

			if((target instanceof L2MonsterInstance) && Config.GUARD_ATTACK_AGGRO_MOB)
			{
				return ((L2MonsterInstance) target).isAggressive() && GeoData.getInstance().canSeeTarget(me, target);
			}

			return false;
		}
		else if(_actor instanceof L2FriendlyMobInstance)
		{
			// the actor is a L2FriendlyMobInstance

			// Check if the target isn't another L2NpcInstance
			if(target instanceof L2NpcInstance)
				return false;

			// Check if the L2PcInstance target has karma (=PK)
			if(target instanceof L2PcInstance && ((L2PcInstance) target).getKarma() > 0)
				// Los Check
				return GeoData.getInstance().canSeeTarget(me, target);
			return false;
		}
		else
		{
			//The actor is a L2MonsterInstance

			// Check if the target isn't another L2NpcInstance
			if(target instanceof L2NpcInstance)
				return false;

			// depending on config, do not allow mobs to attack _new_ players in peacezones,
			// unless they are already following those players from outside the peacezone.
			if(!Config.ALT_MOB_AGRO_IN_PEACEZONE && target.isInsideZone(L2Character.ZONE_PEACE))
				return false;

			// Check if the actor is Aggressive
			return me.isAggressive() && GeoData.getInstance().canSeeTarget(me, target);
		}
	}

	public void startAITask()
	{
		if(_aiTask == null)
		{
			_aiTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, 1000, 1000);
		}
	}

	public void stopAITask()
	{
		if(_aiTask != null)
		{
			_aiTask.cancel(false);
			_aiTask = null;
		}
	}

	@Override
	protected void onEvtDead()
	{
		stopAITask();
		super.onEvtDead();
	}

	@Override
	synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		if(intention == AI_INTENTION_IDLE || intention == AI_INTENTION_ACTIVE)
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
					if (npc.getSpawn() != null) 
					{ 
						final int range = Config.MAX_DRIFT_RANGE; 
						if (!npc.isInsideRadius(npc.getSpawn().getLocx(), npc.getSpawn().getLocy(), npc.getSpawn().getLocz(), range + range, true, false)) 
							intention = AI_INTENTION_ACTIVE; 
					} 
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

		startAITask();
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
			for(L2Object obj : npc.getKnownList().getKnownObjects().values())
			{
				if(obj == null || !(obj instanceof L2Character))
				{
					continue;
				}

				L2Character target = (L2Character) obj;

				if(_actor instanceof L2FestivalMonsterInstance && obj instanceof L2PcInstance)
				{
					L2PcInstance targetPlayer = (L2PcInstance) obj;
					if(!targetPlayer.isFestivalParticipant())
					{
						continue;
					}

					targetPlayer = null;
				}

				if(obj instanceof L2PcInstance || obj instanceof L2Summon)
				{
					if(!((L2Character) obj).isAlikeDead() && !npc.isInsideRadius(obj, npc.getAggroRange(), true, false))
					{
						L2PcInstance targetPlayer = obj instanceof L2PcInstance ? (L2PcInstance) obj : ((L2Summon) obj).getOwner();

						if(npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_AGGRO_RANGE_ENTER) != null)
						{
							for(Quest quest : npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_AGGRO_RANGE_ENTER))
							{
								quest.notifyAggroRangeEnter(npc, targetPlayer, obj instanceof L2Summon);
							}
						}
					}
				}

				if(autoAttackCondition(target))
				{
					int hating = npc.getHating(target);

					if(hating == 0)
					{
						npc.addDamageHate(target, 0, 1);
					}
				}

				target = null;
			}

			L2Character hated;

			if(_actor.isConfused())
			{
				hated = getAttackTarget();
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

					setIntention(CtrlIntention.AI_INTENTION_ATTACK, hated);
				}

				return;
			}

			hated = null;
		}

		if(_actor instanceof L2GuardInstance)
		{
			((L2GuardInstance) _actor).returnHome();
		}

		if(_actor instanceof L2FestivalMonsterInstance)
		{
			return;
		}

		if(!npc.canReturnToSpawnPoint())
		{
			return;
		}

		if(_actor instanceof L2MinionInstance && ((L2MinionInstance) _actor).getLeader() != null)
		{
			int offset;

			if(_actor.isRaid())
			{
				offset = 500;
			}
			else
			{
				offset = 200;
			}

			if(((L2MinionInstance) _actor).getLeader().isRunning())
			{
				_actor.setRunning();
			}
			else
			{
				_actor.setWalking();
			}

			if(_actor.getPlanDistanceSq(((L2MinionInstance) _actor).getLeader()) > offset * offset)
			{
				int x1, y1, z1;

				x1 = ((L2MinionInstance) _actor).getLeader().getX() + Rnd.nextInt((offset - 30) * 2) - (offset - 30);
				y1 = ((L2MinionInstance) _actor).getLeader().getY() + Rnd.nextInt((offset - 30) * 2) - (offset - 30);
				z1 = ((L2MinionInstance) _actor).getLeader().getZ();
				moveTo(x1, y1, z1);
				return;
			}
			else if(Rnd.nextInt(RANDOM_WALK_RATE) == 0)
			{
				for(L2Skill sk : _actor.getAllSkills())
				{
					if(sk != null && sk.getSkillType() == L2SkillType.BUFF)
					{
						if(_actor.getFirstEffect(sk.getId()) == null)
						{
							if(_actor.getStatus().getCurrentMp() < sk.getMpConsume())
								continue;
							if(_actor.isSkillDisabled(sk.getId()))
								continue;
							// no clan buffs here
							if(sk.getTargetType() == L2Skill.SkillTargetType.TARGET_CLAN)
								continue;
							L2Object OldTarget = _actor.getTarget();
							_actor.setTarget(_actor);
							clientStopMoving(null);
							_accessor.doCast(sk);
							// forcing long reuse delay so if cast get interrupted or there would be several buffs, doesn't cast again
							_actor.setTarget(OldTarget);
							break;
						}
					}
				}
			}
		}
		else if(npc.getSpawn() != null && Rnd.nextInt(RANDOM_WALK_RATE) == 0 && !_actor.isNoRndWalk())
		{
			int x1, y1, z1;
			final int range = Config.MAX_DRIFT_RANGE;
			
			for(L2Skill sk : _actor.getAllSkills())
			{
				if(sk != null && sk.getSkillType() == L2SkillType.BUFF)
				{
					if(_actor.getFirstEffect(sk.getId()) == null)
					{
						if(_actor.getStatus().getCurrentMp() < sk.getMpConsume())
							continue;
						if(_actor.isSkillDisabled(sk.getId()))
							continue;
						// no clan buffs here
						if(sk.getTargetType() == L2Skill.SkillTargetType.TARGET_CLAN)
							continue;
						L2Object OldTarget = _actor.getTarget();
						_actor.setTarget(_actor);
						clientStopMoving(null);
						_accessor.doCast(sk);
						// forcing long reuse delay so if cast get interrupted or there would be several buffs, doesn't cast again
						_actor.setTarget(OldTarget);
						break;
					}
				}
			}

			if(npc.getSpawn().getLocx() == 0 && npc.getSpawn().getLocy() == 0)
			{
				if(TerritoryTable.getInstance().getProcMax(npc.getSpawn().getLocation()) > 0)
				{
					return;
				}

				int p[] = TerritoryTable.getInstance().getRandomPoint(npc.getSpawn().getLocation());
				x1 = p[0];
				y1 = p[1];
				z1 = p[2];

				double distance2 = _actor.getPlanDistanceSq(x1, y1);

				if(distance2 > (range + range) * (range + range))
				{
					float delay = (float) Math.sqrt(distance2) / range;
					x1 = _actor.getX() + (int) ((x1 - _actor.getX()) / delay);
					y1 = _actor.getY() + (int) ((y1 - _actor.getY()) / delay);
					npc.setisReturningToSpawnPoint(true);
				}
				
				if (TerritoryTable.getInstance().getProcMax(npc.getSpawn().getLocation()) > 0 && !npc.isReturningToSpawnPoint()) 
					return; 
				
				else
				{
					x1 = npc.getSpawn().getLocx(); 
					y1 = npc.getSpawn().getLocy(); 
					z1 = npc.getSpawn().getLocz();
					
					if (!_actor.isInsideRadius(x1, y1, z1, range + range, true, false)) 
						
					npc.setisReturningToSpawnPoint(true);
				}

			}
			else
			{
				x1 = npc.getSpawn().getLocx() + Rnd.nextInt(range * 2) - range;
				y1 = npc.getSpawn().getLocy() + Rnd.nextInt(range * 2) - range;
				z1 = npc.getZ();
			}

			moveTo(x1, y1, z1);
		}

		if(Config.MONSTER_RETURN_DELAY > 0 && npc instanceof L2MonsterInstance)
		{
			((L2MonsterInstance) _actor).returnHome();
		}

		npc = null;

		return;

	}

	private void thinkAttack()
    {
        if(_actor.isCastingNow())
        {
            return;
        }

        if(getAttackTarget() == null || getAttackTarget().isAlikeDead() || (getAttackTarget() instanceof L2PcInstance && ((L2PcInstance)getAttackTarget()).isOffline()) || _attackTimeout < GameTimeController.getGameTicks())
        {
            if(getAttackTarget() != null)
            {
                L2Attackable npc = (L2Attackable) _actor;
                npc.stopHating(getAttackTarget());
                npc = null;
            }

            setIntention(AI_INTENTION_ACTIVE);

            _actor.setWalking();
            return;
        }

        if(((L2Npc) _actor).getFactionId() != null)
		{
			for(L2Object obj : _actor.getKnownList().getKnownObjects().values())
			{
				if(obj instanceof L2Npc)
				{
					L2Npc npc = (L2Npc) obj;
					String faction_id = ((L2Npc) _actor).getFactionId();

					if(npc == null || getAttackTarget() == null || faction_id != npc.getFactionId() || npc.getFactionRange() == 0)
					{
						faction_id = null;
						continue;
					}

					if(_actor.isInsideRadius(npc, npc.getFactionRange(), true, false) && npc != null && _actor != null && npc.getAI() != null && GeoData.getInstance().canSeeTarget(_actor, npc) && Math.abs(getAttackTarget().getZ() - npc.getZ()) < 600 && _actor.getAttackByList().contains(getAttackTarget()) && (npc.getAI()._intention == CtrlIntention.AI_INTENTION_IDLE || npc.getAI()._intention == CtrlIntention.AI_INTENTION_ACTIVE))
					{
						if(getAttackTarget() instanceof L2PcInstance && getAttackTarget().isInParty() && getAttackTarget().getParty().isInDimensionalRift())
						{
							byte riftType = getAttackTarget().getParty().getDimensionalRift().getType();
							byte riftRoom = getAttackTarget().getParty().getDimensionalRift().getCurrentRoom();

							if(_actor instanceof L2RiftInvaderInstance && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(npc.getX(), npc.getY(), npc.getZ()))
							{
								continue;
							}
						}
						npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, getAttackTarget(), 1);
					}
					if(_actor.isInsideRadius(npc, npc.getFactionRange(), true, false) && npc != null && _actor != null && npc.getAI() != null && GeoData.getInstance().canSeeTarget(_actor, npc) && Math.abs(getAttackTarget().getZ() - npc.getZ()) < 500 && _actor.getAttackByList().contains(getAttackTarget()))
					{
						if(getAttackTarget() instanceof L2PcInstance || getAttackTarget() instanceof L2Summon)
						{
							if(npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_FACTION_CALL) != null)
							{
								L2PcInstance player = getAttackTarget() instanceof L2PcInstance ? (L2PcInstance) getAttackTarget() : ((L2Summon) getAttackTarget()).getOwner();
								for(Quest quest : npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_FACTION_CALL))
								{
									quest.notifyFactionCall(npc, (L2Npc) _actor, player, (getAttackTarget() instanceof L2Summon));
								}
							}
						}
					}
					npc = null;
				}
			}
		}

        if(_actor.isAttackingDisabled())
        {
            return;
        }

        L2Skill[] skills = null;
        double dist2 = 0;
        int range = 0;

        try
        {
            _actor.setTarget(getAttackTarget());
            skills = _actor.getAllSkills();
            dist2 = _actor.getPlanDistanceSq(getAttackTarget().getX(), getAttackTarget().getY());
            range = _actor.getPhysicalAttackRange() + _actor.getTemplate().collisionRadius + getAttackTarget().getTemplate().collisionRadius;
        }
        catch(NullPointerException e)
        {
            setIntention(AI_INTENTION_ACTIVE);
            return;
        }

        L2Weapon weapon = _actor.getActiveWeaponItem();

        if(weapon != null && weapon.getItemType() == L2WeaponType.BOW)
        {
            double distance2 = _actor.getPlanDistanceSq(getAttackTarget().getX(), getAttackTarget().getY());
            if(distance2 <= 10000 && (Config.GEODATA >= 2 ? 12 : 8) >= Rnd.get(100))
            {
                int posX = _actor.getX();
                int posY = _actor.getY();
                int posZ = _actor.getZ();
                double distance = Math.sqrt(distance2); // This way, we only do the sqrt if we need it

                int signx = -1;
                int signy = -1;
                if(_actor.getX() > getAttackTarget().getX())
                {
                    signx = 1;
                }
                if(_actor.getY() > getAttackTarget().getY())
                {
                    signy = 1;
                }

                posX += Math.round((float) (signx * (range / 2 + Rnd.get(range)) - distance));
                posY += Math.round((float) (signy * (range / 2 + Rnd.get(range)) - distance));

                moveTo(posX, posY, posZ);
                return;
            }
        }
        weapon = null;

        L2Character hated;
        if(_actor.isConfused())
        {
            hated = getAttackTarget();
        }
        else
        {
            hated = ((L2Attackable) _actor).getMostHated();
        }

        if(hated == null)
        {
            setIntention(AI_INTENTION_ACTIVE);
            return;
        }

        if(hated != getAttackTarget())
        {
            setAttackTarget(hated);
        }
        dist2 = _actor.getPlanDistanceSq(hated.getX(), hated.getY());

        if(hated.isMoving())
        {
            range += 50;
        }

        if(dist2 > range * range)
        {
            if(!_actor.isMuted() && (_actor instanceof L2MonsterInstance && Rnd.nextInt(100) <= 5))
            {
                for(L2Skill sk : skills)
                {
                    int castRange = sk.getCastRange();

                    if((sk.getSkillType() == L2SkillType.BUFF || sk.getSkillType() == L2SkillType.HEAL || dist2 >= castRange * castRange / 9.0 && dist2 <= castRange * castRange && castRange > 70) && !_actor.isSkillDisabled(sk.getId()) && _actor.getCurrentMp() >= _actor.getStat().getMpConsume(sk) && !sk.isPassive() && Rnd.nextInt(100) <= 5)
                    {
                        boolean useSkillSelf = false;
                        L2Object OldTarget = _actor.getTarget();

                        if(sk.getSkillType() == L2SkillType.BUFF || sk.getSkillType() == L2SkillType.HEAL)
                        {
                            if(sk.getSkillType() == L2SkillType.HEAL && _actor.getCurrentHp() > (int) (_actor.getMaxHp() / 1.5))
                            {
                                continue;
                            }

                            if(sk.getSkillType() == L2SkillType.BUFF)
                            {
                                if(_actor.getFirstEffect(sk.getId()) != null)
                                {
                                    continue;
                                }
                            }

                            _actor.setTarget(_actor);
                            useSkillSelf = true;
                        }

                        if(!useSkillSelf && !GeoData.getInstance().canSeeTarget(_actor, _actor.getTarget()))
                            continue;

                        clientStopMoving(null);
                        _accessor.doCast(sk);
                        _actor.setTarget(OldTarget);
                        OldTarget = null;

                        return;
                    }
                }
            }

            if(hated.isMoving())
            {
                range -= 100;
            }
            if(range < 5)
            {
                range = 5;
            }

            moveToPawn(getAttackTarget(), range);

            return;
        }
        else
        {
            _attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();

            // Check for close combat skills && heal/buff skills
            if(!_actor.isMuted() /*&& _rnd.nextInt(100) <= 5*/)
            {
                for(L2Skill sk : skills)
                {
                    if(/*sk.getCastRange() >= dist && sk.getCastRange() <= 70 && */!sk.isPassive() && _actor.getCurrentMp() >= _actor.getStat().getMpConsume(sk) && !_actor.isSkillDisabled(sk.getId()) && (Rnd.nextInt(100) <= 8 || _actor instanceof L2PenaltyMonsterInstance && Rnd.nextInt(100) <= 20))
                    {
                        boolean useSkillSelf = false;
                        L2Object OldTarget = _actor.getTarget();

                        if(sk.getSkillType() == L2SkillType.BUFF || sk.getSkillType() == L2SkillType.HEAL)
                        {
                            if(sk.getSkillType() == L2SkillType.HEAL && _actor.getCurrentHp() > (int) (_actor.getMaxHp() / 1.5))
                            {
                                continue;
                            }
                            if(sk.getSkillType() == L2SkillType.BUFF)
                            {
                                if(_actor.getFirstEffect(sk.getId()) != null)
                                {
                                        continue;
                                }
                            }

                            _actor.setTarget(_actor);
                            useSkillSelf = true;
                        }

                        // GeoData Los Check here
                        if(!useSkillSelf && !GeoData.getInstance().canSeeTarget(_actor, _actor.getTarget()))
                            continue;

                        clientStopMoving(null);
                        _accessor.doCast(sk);
                        _actor.setTarget(OldTarget);
                        OldTarget = null;

                        return;
                    }
                }
            }

            // Finally, physical attacks
            clientStopMoving(null);
            _accessor.doAttack(hated);
        }

        skills = null;
        hated = null;
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
			setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
		}
		else if(((L2Attackable) _actor).getMostHated() != getAttackTarget())
		{
			setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
		}

		super.onEvtAttacked(attacker);
	}

	@Override
	protected void onEvtAggression(L2Character target, int aggro)
	{
		L2Attackable me = (L2Attackable) _actor;

		if(target != null)
		{
			me.addDamageHate(target, 0, aggro);

			if(getIntention() != CtrlIntention.AI_INTENTION_ATTACK)
			{
				if(!_actor.isRunning())
				{
					_actor.setRunning();
				}

				setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
			}
		}

		me = null;
	}
	
	@SuppressWarnings("unused")
	private boolean containsNoRandomWalk(int npcId)
	{
		for (int i = 0; i < noRandomWalkList.length; i++)
		{
			if (noRandomWalkList[i] == npcId)
				return true;
		}
		return false;
	}

	@Override
	protected void onIntentionActive()
	{
		_attackTimeout = Integer.MAX_VALUE;
		super.onIntentionActive();
	}

	public void setGlobalAggro(int value)
	{
		_globalAggro = value;
	}
}