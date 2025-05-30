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

import java.util.List;
import java.util.concurrent.ScheduledFuture;

import com.src.Config;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.knownlist.MonsterKnownList;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.gameserver.util.MinionList;
import com.src.util.random.Rnd;

public class L2MonsterInstance extends L2Attackable
{
	protected final MinionList _minionList; 
	
	protected ScheduledFuture<?> _maintenanceTask = null; 
	
	private static final int MONSTER_MAINTENANCE_INTERVAL = 1000;
	
	public L2MonsterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		getKnownList();
		if (getTemplate().getMinionData() != null) 
			_minionList  = new MinionList(this); 
		else 
			_minionList = null; 
	} 
	
	@Override
	public final MonsterKnownList getKnownList()
	{
		if(super.getKnownList() == null || !(super.getKnownList() instanceof MonsterKnownList))
		{
			setKnownList(new MonsterKnownList(this));
		}
		
		return (MonsterKnownList) super.getKnownList();
	}

	public void returnHome()
	{
		ThreadPoolManager.getInstance().scheduleAi(new Runnable()
		{
			@Override
			public void run()
			{
				L2Spawn mobSpawn = getSpawn();
				if(!isInCombat() && !isAlikeDead() && !isDead() && mobSpawn != null && !isInsideRadius(mobSpawn.getLocx(), mobSpawn.getLocy(), Config.MAX_DRIFT_RANGE, false))
				{
					teleToLocation(mobSpawn.getLocx(), mobSpawn.getLocy(), mobSpawn.getLocz(), false);
				}
				mobSpawn = null;
			}
		}, Config.MONSTER_RETURN_DELAY * 1000);
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		if(attacker instanceof L2MonsterInstance)
		{
			return false;
		}

		return !isEventMob;
	}

	@Override
	public boolean isAggressive()
	{
		return getTemplate().aggroRange > 0 && !isEventMob;
	}

	@Override
	public void onSpawn()
	{ 
		super.onSpawn(); 
		
		if (_minionList != null) 
		{ 
			try 
			{ 
				for (L2MinionInstance minion : getSpawnedMinions()) 
				{ 
					if (minion == null) continue; 
					getSpawnedMinions().remove(minion); 
					minion.deleteMe(); 
				} 
				_minionList.clearRespawnList(); 
			} 
			catch ( NullPointerException e ) 
			{ 
			} 
		} 
		startMaintenanceTask(); 
	} 
	
	protected int getMaintenanceInterval() 
	{ 
		return MONSTER_MAINTENANCE_INTERVAL; 
	} 

	/** 
	 * Spawn all minions at a regular interval 
	 * 
	 */ 
	protected void startMaintenanceTask() 
	{ 
		// maintenance task now used only for minions spawn 
		if (_minionList == null) 
			return; 
		
		_maintenanceTask = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() { 
			@Override
			public void run() 
			{ 
				_minionList.spawnMinions(); 
			} 
		}, getMaintenanceInterval() + Rnd.get(1000)); 
	} 
	
	public void callMinions() 
	{ 
		if (hasMinions()) 
		{ 
			for (L2MinionInstance minion : _minionList.getSpawnedMinions()) 
			{ 
				if (minion == null || minion.isDead() || minion.isMovementDisabled()) 
					continue; 
				
				// Get actual coords of the minion and check to see if it's too far away from this L2MonsterInstance 
				if (!isInsideRadius(minion, 200, false, false)) 
				{ 
					// Calculate a new random coord for the minion based on the master's coord 
					// but with minimum distance from master = 30 
					int minionX = Rnd.nextInt(340); 
					int minionY = Rnd.nextInt(340); 
					
					if (minionX < 171) 
						minionX = getX() + minionX + 30; 
					else 
						minionX = getX() - minionX + 140; 
					
					if (minionY < 171) 
						minionY = getY() + minionY + 30; 
					else 
						minionY = getY() - minionY + 140; 
					
					// Move the minion to the new coords 
					if (!minion.isInCombat() && !minion.isDead() && !minion.isMovementDisabled()) 
						minion.moveToLocation(minionX, minionY, getZ(), 0); 
				} 
			} 
		} 
	} 
	
	public void callMinionsToAssist(L2Character attacker) 
	{ 
		if (hasMinions()) 
		{ 
			for (L2MinionInstance minion : _minionList.getSpawnedMinions()) 
			{ 
				if (minion == null || minion.isDead()) 
					continue; 
				
				// Trigger the aggro condition of the minion 
				if(this instanceof L2RaidBossInstance) 
					minion.addDamage(attacker, 100); 
				else minion.addDamage(attacker, 1); 
			} 
		} 
	} 

	@Override
	public boolean doDie(L2Character killer)
	{ 
		if (!super.doDie(killer)) 
			return false; 
		
		if (_maintenanceTask != null) 
			_maintenanceTask.cancel(true); // doesn't do it? 
		
		if (hasMinions() && isRaid()) 
			deleteSpawnedMinions(); 
		return true; 
	} 
     
	public List<L2MinionInstance> getSpawnedMinions() 
	{
		if (_minionList == null) 
			return null; 
		return _minionList.getSpawnedMinions(); 
	} 
     
	public int getTotalSpawnedMinionsInstances() 
	{ 
		if (_minionList == null) 
			return 0; 
		return _minionList.countSpawnedMinions(); 
	} 
	
	public int getTotalSpawnedMinionsGroups() 
	{ 
		if (_minionList == null) 
			return 0; 
		return _minionList.lazyCountSpawnedMinionsGroups(); 
	} 
	
	public void notifyMinionDied(L2MinionInstance minion) 
	{ 
		_minionList.moveMinionToRespawnList(minion); 
	} 
     
	public void notifyMinionSpawned(L2MinionInstance minion) 
	{ 
		_minionList.addSpawnedMinion(minion); 
	} 
     
	public boolean hasMinions() 
	{ 
		if (_minionList == null) 
			return false; 
		return _minionList.hasMinions(); 
	}
     
	@Override
	public void addDamageHate(L2Character attacker, int damage, int aggro)
	{
		if(!(attacker instanceof L2MonsterInstance))
		{
			super.addDamageHate(attacker, damage, aggro);
		}
	}

	@Override
	public void deleteMe()
	{
		if (hasMinions())
		{
			if (_maintenanceTask != null)
				_maintenanceTask.cancel(true);
			
			deleteSpawnedMinions();
		}
		super.deleteMe();
	}

	public void deleteSpawnedMinions()
	{
		for(L2MinionInstance minion : getSpawnedMinions())
		{
			if(minion == null)
			{
				continue;
			}
			minion.abortAttack();
			minion.abortCast();
			minion.deleteMe();
			getSpawnedMinions().remove(minion);
		}
		_minionList.clearRespawnList();
	}

}