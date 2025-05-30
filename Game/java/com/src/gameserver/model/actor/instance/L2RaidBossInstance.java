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

import com.src.Config;
import com.src.gameserver.managers.RaidBossPointsManager;
import com.src.gameserver.managers.RaidBossSpawnManager;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.PlaySound;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.random.Rnd;

public final class L2RaidBossInstance extends L2MonsterInstance
{
	private static final int RAIDBOSS_MAINTENANCE_INTERVAL = 30000;

	private RaidBossSpawnManager.StatusEnum _raidStatus;

	public L2RaidBossInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onSpawn()
	{
		setIsNoRndWalk(true);
		super.onSpawn();
	}
	
	@Override
	public boolean isRaid()
	{
		return true;
	}

	@Override
	protected int getMaintenanceInterval()
	{
		return RAIDBOSS_MAINTENANCE_INTERVAL;
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if(!super.doDie(killer))
		{
			return false;
		}

		L2PcInstance player = null;

		if(killer instanceof L2PcInstance)
		{
			player = (L2PcInstance) killer;
		}
		else if(killer instanceof L2Summon)
		{
			player = ((L2Summon) killer).getOwner();
		}

		if(player != null)
		{
			broadcastPacket(new SystemMessage(SystemMessageId.RAID_WAS_SUCCESSFUL));
			broadcastPacket(new PlaySound("systemmsg_e.1209"));
			if(player.getParty() != null)
			{
				for(L2PcInstance member : player.getParty().getPartyMembers())
				{
					RaidBossPointsManager.addPoints(member, getNpcId(), (getLevel() / 2) + Rnd.get(-5, 5));
				}
			}
			else
			{
				RaidBossPointsManager.addPoints(player, getNpcId(), (getLevel() / 2) + Rnd.get(-5, 5));
			}
		}

		if(!getSpawn().is_customBossInstance())
		RaidBossSpawnManager.getInstance().updateStatus(this, true);
		return true;
	}

	@Override
	protected void startMaintenanceTask() 
	{ 
		if (_minionList != null) 
			_minionList.spawnMinions(); 
		
		_maintenanceTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Runnable() { 
			@Override
			public void run() 
			{ 
				checkAndReturnToSpawn(); 
				
				if (_minionList != null) 
					_minionList.maintainMinions(); 
			} 
		}, 60000, getMaintenanceInterval()+Rnd.get(5000)); 
	} 
	
	protected void checkAndReturnToSpawn() 
	{ 
		if (isDead() || isMovementDisabled()) 
			return; 
		
		// Gordon does not have permanent spawn 
		if (getNpcId() == 29095) 
			return; 
		
		final L2Spawn spawn = getSpawn(); 
		if (spawn == null) 
			return; 
		
		final int spawnX = spawn.getLocx(); 
		final int spawnY = spawn.getLocy(); 
		final int spawnZ = spawn.getLocz(); 
		
		if (!isInCombat() && !isMovementDisabled()) 
		{ 
			if (!isInsideRadius(spawnX, spawnY, spawnZ, Math.max(Config.MAX_DRIFT_RANGE, 200), true, false)) 
				teleToLocation(spawnX, spawnY, spawnZ, false); 
		} 
	}

	public void setRaidStatus(RaidBossSpawnManager.StatusEnum status)
	{
		_raidStatus = status;
	}

	public RaidBossSpawnManager.StatusEnum getRaidStatus()
	{
		return _raidStatus;
	}

	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake)
	{
		super.reduceCurrentHp(damage, attacker, awake);
	}

	public void healFull()
	{
		super.setCurrentHp(super.getMaxHp());
		super.setCurrentMp(super.getMaxMp());
	}

}