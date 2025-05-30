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
package com.src.gameserver.util;

import java.util.List;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import com.src.Config;
import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.idfactory.IdFactory;
import com.src.gameserver.model.L2MinionData;
import com.src.gameserver.model.actor.instance.L2MinionInstance;
import com.src.gameserver.model.actor.instance.L2MonsterInstance;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.util.random.Rnd;

public class MinionList
{
	private final List<L2MinionInstance> minionReferences;
	protected FastMap<Long, Integer> _respawnTasks = new FastMap<Long, Integer>().shared();
	private final L2MonsterInstance master;

	public MinionList(L2MonsterInstance pMaster)
	{
		minionReferences = new FastList<L2MinionInstance>();
		master = pMaster;
	}

	public int countSpawnedMinions()
	{
		synchronized (minionReferences)
		{
			return minionReferences.size();
		}
	}

	public int countSpawnedMinionsById(int minionId)
	{
		int count = 0;
		synchronized (minionReferences)
		{
			for(L2MinionInstance minion : getSpawnedMinions())
			{
				if(minion.getNpcId() == minionId)
				{
					count++;
				}
			}
		}
		return count;
	}

	public boolean hasMinions()
	{
		return getSpawnedMinions().size() > 0;
	}

	public List<L2MinionInstance> getSpawnedMinions()
	{
		return minionReferences;
	}

	public void addSpawnedMinion(L2MinionInstance minion)
	{
		synchronized (minionReferences)
		{
			minionReferences.add(minion);
		}
	}

	public int lazyCountSpawnedMinionsGroups()
	{
		Set<Integer> seenGroups = new FastSet<Integer>();
		for(L2MinionInstance minion : getSpawnedMinions())
		{
			seenGroups.add(minion.getNpcId());
		}
		return seenGroups.size();
	}

	public void removeSpawnedMinion(L2MinionInstance minion)
	{
		synchronized (minionReferences)
		{
			minionReferences.remove(minion);
		}
	}

	public void moveMinionToRespawnList(L2MinionInstance minion)
	{
		Long current = System.currentTimeMillis();
		synchronized (minionReferences)
		{
			minionReferences.remove(minion);
			if(_respawnTasks.get(current) == null)
			{
				_respawnTasks.put(current, minion.getNpcId());
			}
			else
			{
				for(int i = 1; i < 30; i++)
				{
					if(_respawnTasks.get(current + i) == null)
					{
						_respawnTasks.put(current + i, minion.getNpcId());
						break;
					}
				}
			}
		}
	}

	public void clearRespawnList()
	{
		_respawnTasks.clear();
	}

	public void maintainMinions()
	{
		if(master == null || master.isAlikeDead())
		{
			return;
		}

		Long current = System.currentTimeMillis();

		if(_respawnTasks != null)
		{
			for(long deathTime : _respawnTasks.keySet())
			{
				double delay = Config.RAID_MINION_RESPAWN_TIMER;

				if(current - deathTime > delay)
				{
					spawnSingleMinion(_respawnTasks.get(deathTime));
					_respawnTasks.remove(deathTime);
				}
			}
		}
	}

	public void spawnMinions()
	{
		if(master == null || master.isAlikeDead())
		{
			return;
		}

		List<L2MinionData> minions = master.getTemplate().getMinionData();

		synchronized (minionReferences)
		{
			int minionCount, minionId, minionsToSpawn;

			for(L2MinionData minion : minions)
			{
				minionCount = minion.getAmount();
				minionId = minion.getMinionId();

				minionsToSpawn = minionCount - countSpawnedMinionsById(minionId);

				for(int i = 0; i < minionsToSpawn; i++)
				{
					spawnSingleMinion(minionId);
				}
			}
		}
	}

	public void spawnSingleMinion(int minionid)
	{
		L2NpcTemplate minionTemplate = NpcTable.getInstance().getTemplate(minionid);

		L2MinionInstance monster = new L2MinionInstance(IdFactory.getInstance().getNextId(), minionTemplate);

		monster.setCurrentHpMp(monster.getMaxHp(), monster.getMaxMp());
		monster.setHeading(master.getHeading());
		monster.setLeader(master);

		int spawnConstant;
		int randSpawnLim = 170;
		int randPlusMin = 1;
		spawnConstant = Rnd.nextInt(randSpawnLim);

		randPlusMin = Rnd.nextInt(2);
		if(randPlusMin == 1)
		{
			spawnConstant *= -1;
		}

		int newX = master.getX() + Math.round(spawnConstant);
		spawnConstant = Rnd.nextInt(randSpawnLim);

		randPlusMin = Rnd.nextInt(2);

		if(randPlusMin == 1)
		{
			spawnConstant *= -1;
		}

		int newY = master.getY() + Math.round(spawnConstant);

		monster.spawnMe(newX, newY, master.getZ());
	}

}