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

import com.src.gameserver.managers.QuestManager;

/**
 * This class merely enumerates the three necessary states for all quests:
 * CREATED: a quest state is created but the quest is not yet accepted.
 * STARTED: the player has accepted the quest.  Quest is currently in progress
 * COMPLETED: the quest has been completed.
 * 
 * In addition, this class defines two functions for lookup and inverse lookup
 * of the state given a name.  This is useful only for saving the state values
 * into the database with a more readable form and then being able to read the
 * string back and remap them to their correct states.
 * 
 * All quests have these and only these states.
 */
public class State
{
	private String _questName;
	private String _name;

	public State(String name, Quest quest)
	{
		_name = name;
		_questName = quest.getName();
		quest.addState(this);
	}

	public void addQuestDrop(int npcId, int itemId, int chance)
	{
		QuestManager.getInstance().getQuest(_questName).registerItem(itemId);
	}

	public String getName()
	{
		return _name;
	}

	@Override
	public String toString()
	{
		return _name;
	}

	public final static byte CREATED = 0;
	public final static byte STARTED = 1;
	public final static byte COMPLETED = 2;

	// discover the string representation of the state, for readable DB storage
	public static String getStateName(byte state)
	{
		switch (state)
		{
			case 1:
				return "Started";
			case 2:
				return "Completed";
			default:
				return "Start";
		}
	}

	// discover the state from its string representation (for reconstruction after DB read)
	public static byte getStateId(String statename)
	{
		if (statename.equals("Started"))
			return 1;
		if (statename.equals("Completed"))
			return 2;
		return 0;
	}
}