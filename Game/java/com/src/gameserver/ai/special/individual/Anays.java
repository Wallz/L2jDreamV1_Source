package com.src.gameserver.ai.special.individual;

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.managers.GrandBossManager;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.quest.Quest;
import com.src.gameserver.model.quest.State;
import com.src.gameserver.model.zone.type.L2BossZone;

/**
 *  @author TerryXX
 *  AI For raid boss Anays(25517) in Monastery of Silence.
 *  Raid is aggro and start attack to players.
 *  See thought Silent Move.
 *  If leave zone teleporting to home.
 */
public class Anays extends Quest
{
	private static final int ANAYS = 25517;
	private static L2BossZone _Zone;

	public Anays(int questId, String name, String descr)
	{
		super(questId, name, descr);

		setInitialState(new State("Start", this));
		
		_Zone = GrandBossManager.getInstance().getZone(113000, -76000, 200);
		addEventId(ANAYS, Quest.QuestEventType.ON_ATTACK);
		addEventId(ANAYS, Quest.QuestEventType.ON_SPAWN);
		addEventId(ANAYS, Quest.QuestEventType.ON_AGGRO_RANGE_ENTER);
	}

	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		if(npc.getNpcId() == ANAYS && !_Zone.isInsideZone(npc.getX(), npc.getY()))
		{
			((L2Attackable) npc).clearAggroList();
			npc.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			npc.teleToLocation(113000, -76000, 200);
		}

		return super.onAttack(npc, attacker, damage, isPet);
	}

	@Override
	public String onSpawn(L2Npc npc)
	{
		if (npc instanceof L2Attackable)
	        	((L2Attackable)npc).setSeeThroughSilentMove(true);
					return super.onSpawn(npc);
	}

	@Override
	public String onAggroRangeEnter(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		if(npc.getNpcId() == ANAYS && !npc.isInCombat() && npc.getTarget() == null)
		{
			npc.setTarget(player);
			npc.setIsRunning(true);
			((L2Attackable) npc).addDamageHate(player, 0, 999);
			npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, player);
		}
		else if(((L2Attackable)npc).getMostHated() == null)
		{
            return null;
		}

		return super.onAggroRangeEnter(npc, player, isPet);
	}

}