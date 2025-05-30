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

import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.quest.Quest;
import com.src.gameserver.network.serverpackets.CreatureSay;
import com.src.util.random.Rnd;

public class OlMahumGeneral extends Quest
{
	private static final int NPC = 20438;
	private static boolean _FirstAttacked;

	public OlMahumGeneral(int questId, String name, String descr)
	{
		super(questId, name, descr);

		_FirstAttacked = false;

		addEventId(NPC, Quest.QuestEventType.ON_ATTACK);
		addEventId(NPC, Quest.QuestEventType.ON_KILL);
	}

	@Override
	public String onAttack (L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		if(npc.getNpcId() == NPC)
		{
			if(_FirstAttacked)
			{
				if(Rnd.get(100) != 0)
				{
					return null;
				}
				npc.broadcastPacket(new CreatureSay(npc.getObjectId(),0,npc.getName(),"Come on, Ill take you on!"));
			}
			else
			{
				_FirstAttacked = true;
				npc.broadcastPacket(new CreatureSay(npc.getObjectId(),0,npc.getName(),"I will definitely repay this humiliation!"));
			}
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		if(npc.getNpcId() == NPC)
		{
			_FirstAttacked = false;
		}
		else if(_FirstAttacked)
		{
			addSpawn(npc.getNpcId(),npc.getX(), npc.getY(), npc.getZ(),npc.getHeading(),true,0);
		}
		return super.onKill(npc,killer,isPet);
	}

}