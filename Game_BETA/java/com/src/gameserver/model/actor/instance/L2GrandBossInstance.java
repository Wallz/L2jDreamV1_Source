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
import com.src.gameserver.managers.GrandBossManager;
import com.src.gameserver.managers.RaidBossPointsManager;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.PlaySound;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.random.Rnd;

public final class L2GrandBossInstance extends L2MonsterInstance
{
	private static final int BOSS_MAINTENANCE_INTERVAL = 10000;

	public L2GrandBossInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	protected int getMaintenanceInterval()
	{
		return BOSS_MAINTENANCE_INTERVAL;
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

		return true;
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();
		setIsNoRndWalk(true);
		if(!this.getSpawn().is_customBossInstance())
			GrandBossManager.getInstance().addBoss(this);
	}

	@Override
	protected void startMaintenanceTask()
	{
		_minionList.spawnMinions();
		_maintenanceTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Runnable()
		{
			@Override
			public void run()
			{
				L2Spawn bossSpawn = getSpawn();
				if(!isInsideRadius(bossSpawn.getLocx(), bossSpawn.getLocy(), bossSpawn.getLocz(), Config.GB_LOCK_RANGE, true, false))
				{
					teleToLocation(bossSpawn.getLocx(), bossSpawn.getLocy(), bossSpawn.getLocz(), true);
					healFull();
				}
				_minionList.maintainMinions();
			}
		}, 60000, getMaintenanceInterval() + Rnd.get(Config.GB_LOCK_RANGE));
	}

	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake)
	{
		super.reduceCurrentHp(damage, attacker, awake);
	}

	@Override
	public boolean isRaid()
	{
		return true;
	}

	public void healFull()
	{
		super.setCurrentHp(super.getMaxHp());
		super.setCurrentMp(super.getMaxMp());
	}

}