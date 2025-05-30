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

import java.util.Collection;

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.geo.GeoData;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2MonsterInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.quest.Quest;
import com.src.gameserver.network.clientpackets.Say2;
import com.src.gameserver.network.serverpackets.CreatureSay;

public class GiantScouts extends Quest
{
	final private static int SCOUTS[] = { 20651, 20652 };

	public GiantScouts(int questId, String name, String descr)
	{
		super(questId, name, descr);
		for(int id : SCOUTS)
		{
			addAggroRangeEnterId(id);
		}
	}

	@Override
	public String onAggroRangeEnter(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		final L2Character target = isPet ? player.getPet() : player;
		if(target != null && GeoData.getInstance().canSeeTarget(npc, target) && !player.getAppearance().getInvisible())
		{
			if(!npc.isInCombat() && npc.getTarget() == null)
			{
				npc.broadcastPacket(new CreatureSay(npc.getObjectId(), Say2.SHOUT, npc.getName(), "Oh Giants, an intruder has been discovered."));
			}

			npc.setTarget(target);
			npc.setRunning();
			((L2Attackable) npc).addDamageHate(target, 0, 999);
			npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);

			final Collection<L2Object> objs = npc.getKnownList().getKnownObjects().values();
			for(L2Object obj : objs)
			{
				if(!(obj instanceof L2MonsterInstance))
				{
					continue;
				}

				final L2MonsterInstance monster = (L2MonsterInstance) obj;
				if((npc.getFactionId() != null && monster.getFactionId() != null) && monster.getFactionId().equals(npc.getFactionId()) && GeoData.getInstance().canSeeTarget(npc, monster))
				{
					monster.setTarget(target);
					monster.setRunning();
					monster.addDamageHate(target, 0, 999);
					monster.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
				}
			}
		}

		return super.onAggroRangeEnter(npc, player, isPet);
	}

}