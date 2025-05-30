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
package com.src.gameserver.ai.special.group;

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.quest.Quest;

public class SummonOnAttack extends Quest implements Runnable
{
	private static final int[] mobs =
	{
			20965, 20966, 20967, 20968, 20969, 20970, 20971, 20972, 20973
	};

	public SummonOnAttack(int questId, String name, String descr)
	{
		super(questId, name, descr);
		for(int id : mobs)
		{
			addAttackId(id);
		}
	}

	@Override
	public String onAttack(L2Npc npc, L2PcInstance player, int damage, boolean isPet)
	{
		L2Character attacker = isPet ? player.getPet().getOwner() : player;

		npc.setIsRunning(true);
		((L2Attackable) npc).addDamageHate(attacker, 0, 999);
		npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);

		return super.onAttack(npc, player, damage, isPet);
	}

	@Override
	public void run()
	{
	}

}