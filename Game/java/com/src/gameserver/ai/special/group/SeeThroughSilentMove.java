/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.src.gameserver.ai.special.group;

import com.src.gameserver.ai.L2AttackableAIScript;
import com.src.gameserver.datatables.sql.SpawnTable;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.gameserver.util.Util;

public class SeeThroughSilentMove extends L2AttackableAIScript
{
	private static final int[] MOBIDS = {18001,18002,22199,22215,22216,22217,29009,29010,29011,29012,29013};
	
	public SeeThroughSilentMove(int questId, String name, String descr)
	{
		super(questId, name, descr);
		for (L2Spawn npc : SpawnTable.getInstance().getSpawnTable().values())
			if (Util.contains(MOBIDS,npc.getNpcid()) && npc.getLastSpawn() != null && npc.getLastSpawn() instanceof L2Attackable)
				((L2Attackable)npc.getLastSpawn()).setSeeThroughSilentMove(true);
		for (int npcId : MOBIDS)
			this.addSpawnId(npcId);
	}
	
	@Override
	public String onSpawn(L2Npc npc)
	{
		if (npc instanceof L2Attackable)
			((L2Attackable)npc).setSeeThroughSilentMove(true);
		return super.onSpawn(npc);
	}
	
	public static void main(String[] args)
	{
		new SeeThroughSilentMove(-1, "SeeThroughSilentMove", "ai");
	}
}