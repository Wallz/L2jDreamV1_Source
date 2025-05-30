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
package com.src.gameserver.ai.special;

import javolution.util.FastMap;

import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public class AIExtend implements Runnable
{
	private static FastMap<Integer, AIExtend> _AI = new FastMap<Integer, AIExtend>();
	private int _idCharacter;

	public void addAI(int id)
	{
		if(_AI.get(id) == null)
		{
			_idCharacter = id;
			_AI.put(id, this);
		}
	}

	public static enum Action
	{
		ON_SPELL_FINISHED(true),

		ON_AGGRO_RANGE_ENTER(true),

		ON_SPAWN(true),

		ON_SKILL_USE(true),

		ON_KILL(true),

		ON_ATTACK(true);

		private boolean _isRegistred;

		Action(boolean reg)
		{
			_isRegistred = reg;
		}

		public boolean isRegistred()
		{
			return _isRegistred;
		}
	}

	public static void clearAllAI()
	{
		_AI.clear();
		L2NpcTemplate.clearAI();
	}

	public int getID()
	{
		return _idCharacter;
	}

	public L2NpcTemplate addActionId(int npcId, Action actionType)
	{
		try
		{
			L2NpcTemplate t = NpcTable.getInstance().getTemplate(npcId);

			if(t != null)
			{
				t.addAIEvent(actionType, this);
			}

			return t;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		return null;
	}

	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		return null;
	}

	public String onSkillUse(L2Npc npc, L2PcInstance caster, L2Skill skill)
	{
		return null;
	}

	public String onSpellFinished(L2Npc npc, L2PcInstance player, L2Skill skill)
	{
		return null;
	}

	public String onSpawn(L2Npc npc)
	{
		return null;
	}

	public String onAggroRangeEnter(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		return null;
	}

	public final boolean notifyAggroRangeEnter(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		try
		{
			onAggroRangeEnter(npc, player, isPet);
		}
		catch(Exception e)
		{
			return false;
		}
		return true;
	}

	public final boolean notifySpawn(L2Npc npc)
	{
		try
		{
			onSpawn(npc);
		}
		catch(Exception e)
		{
			return false;
		}
		return true;
	}

	public final boolean notifySkillUse(L2Npc npc, L2PcInstance caster, L2Skill skill)
	{
		try
		{
			onSkillUse(npc, caster, skill);
		}
		catch(Exception e)
		{
			return false;
		}

		return true;
	}

	public final boolean notifySpellFinished(L2Npc npc, L2PcInstance player, L2Skill skill)
	{
		try
		{
			onSpellFinished(npc, player, skill);
		}
		catch(Exception e)
		{
			return false;
		}
		return true;
	}

	public final boolean notifyKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		try
		{
			onKill(npc, killer, isPet);
		}
		catch(Exception e)
		{
			return false;
		}

		return true;
	}

	public final boolean notifyAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		try
		{
			onAttack(npc, attacker, damage, isPet);
		}
		catch(Exception e)
		{
			return false;
		}

		return true;
	}

	@Override
	public void run()
	{
	}

}