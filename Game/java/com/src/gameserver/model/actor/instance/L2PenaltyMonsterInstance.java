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

import com.src.gameserver.ai.CtrlEvent;
import com.src.gameserver.datatables.sql.SpawnTable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.gameserver.network.clientpackets.Say2;
import com.src.gameserver.network.serverpackets.CreatureSay;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.util.random.Rnd;

public class L2PenaltyMonsterInstance extends L2MonsterInstance
{
	private L2PcInstance _ptk;

	public L2PenaltyMonsterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public L2Character getMostHated()
	{
		return _ptk;
	}

	@Deprecated
	public void notifyPlayerDead()
	{
		deleteMe();

		L2Spawn spawn = getSpawn();
		if(spawn != null)
		{
			spawn.stopRespawn();
			SpawnTable.getInstance().deleteSpawn(spawn, false);
			spawn = null;
		}
	}

	public void setPlayerToKill(L2PcInstance ptk)
	{
		if(Rnd.nextInt(100) <= 80)
		{
			CreatureSay cs = new CreatureSay(getObjectId(), Say2.ALL, getName(), "mmm your bait was delicious");
			this.broadcastPacket(cs);
			cs = null;
		}
		_ptk = ptk;
		addDamageHate(ptk, 10, 10);
		getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, ptk);
		addAttackerToAttackByList(ptk);
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if(!super.doDie(killer))
		{
			return false;
		}

		if(Rnd.nextInt(100) <= 75)
		{
			CreatureSay cs = new CreatureSay(getObjectId(), Say2.ALL, getName(), "I will tell fishes not to take your bait");
			this.broadcastPacket(cs);
			cs = null;
		}
		return true;
	}

}