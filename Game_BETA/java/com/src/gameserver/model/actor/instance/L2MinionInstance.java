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

import com.src.gameserver.ai.L2AttackableAI;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.L2WorldRegion;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public final class L2MinionInstance extends L2MonsterInstance
{
	private L2MonsterInstance _master;

	public L2MinionInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public boolean isRaid()
	{
		return getLeader() instanceof L2RaidBossInstance;
	}

	public L2MonsterInstance getLeader()
	{
		return _master;
	}

	@Override
	public void onSpawn()
	{
		setIsNoRndWalk(true);
		super.onSpawn();
		getLeader().notifyMinionSpawned(this);

		L2WorldRegion region = L2World.getInstance().getRegion(getX(), getY());
		if(region != null && !region.isActive())
		{
			((L2AttackableAI) getAI()).stopAITask();
		}
		region = null;
	}

	public void setLeader(L2MonsterInstance leader)
	{
		_master = leader;
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if(!super.doDie(killer))
		{
			return false;
		}

		_master.notifyMinionDied(this);
		return true;
	}

}