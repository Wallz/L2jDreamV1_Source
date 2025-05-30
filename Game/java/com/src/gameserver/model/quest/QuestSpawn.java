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
package com.src.gameserver.model.quest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.random.Rnd;

public final class QuestSpawn
{
	private final static Log _log = LogFactory.getLog(QuestSpawn.class);

	private static QuestSpawn instance;

	public static QuestSpawn getInstance()
	{
		if(instance == null)
		{
			instance = new QuestSpawn();
		}

		return instance;
	}

	public class DeSpawnScheduleTimerTask implements Runnable
	{
		L2Npc _npc = null;

		public DeSpawnScheduleTimerTask(L2Npc npc)
		{
			_npc = npc;
		}

		@Override
		public void run()
		{
			_npc.onDecay();
		}
	}

	public L2Npc addSpawn(int npcId, L2Character cha)
	{
		return addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), false, 0);
	}

	public L2Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, int despawnDelay)
	{
		L2Npc result = null;

		try
		{
			L2NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);

			if(template != null)
			{
				if(x == 0 && y == 0)
				{
					_log.warn("Failed to adjust bad locks for quest spawn! Spawn aborted!");
					return null;
				}

				if(randomOffset)
				{
					int offset;

					offset = Rnd.get(2);
					if(offset == 0)
					{
						offset = -1;
					}

					offset *= Rnd.get(50, 100);
					x += offset;

					offset = Rnd.get(2);
					if(offset == 0)
					{
						offset = -1;
					}

					offset *= Rnd.get(50, 100);
					y += offset;
				}
				L2Spawn spawn = new L2Spawn(template);
				spawn.setHeading(heading);
				spawn.setLocx(x);
				spawn.setLocy(y);
				spawn.setLocz(z + 20);
				spawn.stopRespawn();
				result = spawn.spawnOne();
				spawn = null;

				if(despawnDelay > 0)
				{
					ThreadPoolManager.getInstance().scheduleGeneral(new DeSpawnScheduleTimerTask(result), despawnDelay);
				}

				return result;
			}
		}
		catch(Exception e1)
		{
			_log.error("Could not spawn Npc " + npcId, e1);
		}

		return null;
	}
}