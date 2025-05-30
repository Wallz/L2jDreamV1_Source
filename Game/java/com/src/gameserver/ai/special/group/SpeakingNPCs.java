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
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class SpeakingNPCs extends L2AttackableAIScript
{
	private static final int[] NPC_IDS =
	{
		27016, // Nerkas
		27021, // Kirunak
		27022, // Merkenis
		21104 
	// Delu Lizardman Supplier
	};
	
	public SpeakingNPCs(int questId, String name, String descr)
	{
		super(questId, name, descr);
		this.registerMobs(NPC_IDS, QuestEventType.ON_ATTACK, QuestEventType.ON_KILL);
	}
	
	@Override
	public String onAttack(L2Npc npc, L2PcInstance player, int damage, boolean isPet)
	{
		if (npc.hasSpoken())
			return super.onAttack(npc, player, damage, isPet);
		
		int npcId = npc.getNpcId();
		String message = "";
		
		switch (npcId)
		{
			case 27016:
				message = "...How dare you challenge me!";
				break;
			
			case 27021:
				message = "I will taste your blood!";
				break;
			
			case 27022:
				message = "I shall put you in a never-ending nightmare!";
				break;
				
			case 21104:
				message = "Violates a regulation! We timid fellow result!";
		}
		
		npc.broadcastNpcSay(message);
		npc.setHasSpoken(false); // Make the mob speaks only once, else he will spam.
		
		return super.onAttack(npc, player, damage, isPet);
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		int npcId = npc.getNpcId();
		String message = "";
		
		switch (npcId)
		{
			case 27016:
				message = "May Beleth's power be spread on the whole world...!";
				break;
			
			case 27021:
				message = "I have fulfilled my contract with Trader Creamees.";
				break;
			
			case 27022:
				message = "My soul belongs to Icarus...";
				break;
				
			case 21104:
				message = "Violates a regulation! We timid fellow result!";
		}
		
		npc.broadcastNpcSay(message);
		npc.setHasSpoken(false); // Reset the flag, to unmute the NPC.
		
		return super.onKill(npc, player, isPet);
	}
}