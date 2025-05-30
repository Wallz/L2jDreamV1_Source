package com.src.gameserver.ai.special.individual;

import javolution.util.FastList;

import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.clanhallsiege.FortressOfResistance;
import com.src.gameserver.model.quest.Quest;
import com.src.gameserver.model.quest.State;

/**
 *  @author TerryXX
 */
public class Nurka extends Quest
{
	private int NURKA = 35368;
	private int MESSENGER = 35382;
	private static FastList<String> CLAN_LEADERS = new FastList<String>();
	public Nurka(int questId, String name, String descr)
	{
		super(questId, name, descr);
		
		setInitialState(new State("Start", this));
		
		addEventId(MESSENGER, Quest.QuestEventType.QUEST_START);
		addEventId(MESSENGER, Quest.QuestEventType.QUEST_TALK);
		addEventId(NURKA, Quest.QuestEventType.ON_ATTACK);
		addEventId(NURKA, Quest.QuestEventType.ON_KILL);
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		if(npc.getNpcId() == MESSENGER)
		{
			if(player != null && CLAN_LEADERS.contains(player.getName()))
			{
				return "<html><body>Messenger:<br>You already registered!</body></html>";
			}
			else if(FortressOfResistance.getInstance().Conditions(player))
			{
				CLAN_LEADERS.add(player.getName());
				return "<html><body>Messenger:<br>You have successful registered on a siege!</body></html>";
			}
			else
			{
				return "<html><body>Messenger:<br>Condition are not allow to do that!</body></html>";
			}
		}

		return super.onTalk(npc, player);
	}

	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		int npcId = npc.getNpcId();

		if(attacker != null && npcId == NURKA && CLAN_LEADERS.contains(attacker.getName()));
		{
			FortressOfResistance.getInstance().addSiegeDamage(attacker.getClan(),damage);
		}

		return super.onAttack(npc, attacker, damage, isPet);
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		FortressOfResistance.getInstance().CaptureFinish();
			return super.onKill(npc, killer, isPet);
	}

}