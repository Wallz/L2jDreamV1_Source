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
package com.src.gameserver.script.faenor;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.script.ScriptContext;
import javax.script.ScriptException;

import javolution.util.FastList;

import com.src.gameserver.model.L2DropCategory;
import com.src.gameserver.model.L2DropData;
import com.src.gameserver.model.L2PetData;
import com.src.gameserver.model.entity.Announcements;
import com.src.gameserver.script.DateRange;
import com.src.gameserver.script.EngineInterface;
import com.src.gameserver.script.EventDroplist;
import com.src.gameserver.script.Expression;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public class FaenorInterface implements EngineInterface
{
	protected static final Logger _log = Logger.getLogger(FaenorInterface.class.getName());

	public static FaenorInterface getInstance()
	{
		return SingletonHolder._instance;
	}

	private FaenorInterface()
	{}

	public List<?> getAllPlayers()
	{
		return null;
	}

	@Override
	public void addQuestDrop(int npcID, int itemID, int min, int max, int chance, String questID, String[] states)
	{
		L2NpcTemplate npc = npcTable.getTemplate(npcID);
		if(npc == null)
		{
			throw new NullPointerException();
		}
		L2DropData drop = new L2DropData();
		drop.setItemId(itemID);
		drop.setMinDrop(min);
		drop.setMaxDrop(max);
		drop.setChance(chance);
		drop.setQuestID(questID);
		drop.addStates(states);
		addDrop(npc, drop, false);
	}

	public void addDrop(int npcID, int itemID, int min, int max, boolean sweep, int chance) throws NullPointerException
	{
		L2NpcTemplate npc = npcTable.getTemplate(npcID);
		if(npc == null)
		{
			throw new NullPointerException();
		}
		L2DropData drop = new L2DropData();
		drop.setItemId(itemID);
		drop.setMinDrop(min);
		drop.setMaxDrop(max);
		drop.setChance(chance);

		addDrop(npc, drop, sweep);
	}

	public void addDrop(L2NpcTemplate npc, L2DropData drop, boolean sweep)
	{
		if(sweep)
		{
			addDrop(npc, drop, -1);
		}
		else
		{
			int maxCategory = -1;

			if(npc.getDropData() != null)
			{
				for(L2DropCategory cat : npc.getDropData())
				{
					if(maxCategory < cat.getCategoryType())
					{
						maxCategory = cat.getCategoryType();
					}
				}
			}
			maxCategory++;
			npc.addDropData(drop, maxCategory);
		}
	}

	public void addDrop(L2NpcTemplate npc, L2DropData drop, int category)
	{
		npc.addDropData(drop, category);
	}

	public List<L2DropData> getQuestDrops(int npcID)
	{
		L2NpcTemplate npc = npcTable.getTemplate(npcID);
		if(npc == null)
		{
			return null;
		}
		List<L2DropData> questDrops = new FastList<L2DropData>();
		if(npc.getDropData() != null)
		{
			for(L2DropCategory cat : npc.getDropData())
			{
				for(L2DropData drop : cat.getAllDrops())
				{
					if(drop.getQuestID() != null)
					{
						questDrops.add(drop);
					}
				}
			}
		}
		return questDrops;
	}

	@Override
	public void addEventDrop(int[] items, int[] count, double chance, DateRange range)
	{
		EventDroplist.getInstance().addGlobalDrop(items, count, (int) (chance * L2DropData.MAX_CHANCE), range);
	}

	@Override
	public void onPlayerLogin(String[] message, DateRange validDateRange)
	{
		Announcements.getInstance().addEventAnnouncement(validDateRange, message);
	}

	public void addPetData(ScriptContext context, int petID, int levelStart, int levelEnd, Map<String, String> stats) throws ScriptException
	{
		L2PetData[] petData = new L2PetData[levelEnd - levelStart + 1];
		int value = 0;
		for(int level = levelStart; level <= levelEnd; level++)
		{
			petData[level - 1] = new L2PetData();
			petData[level - 1].setPetID(petID);
			petData[level - 1].setPetLevel(level);

			context.setAttribute("level", new Double(level), ScriptContext.ENGINE_SCOPE);
			for(String stat : stats.keySet())
			{
				value = ((Number) Expression.eval(context, "beanshell", stats.get(stat))).intValue();
				petData[level - 1].setStat(stat, value);
			}
			context.removeAttribute("level", ScriptContext.ENGINE_SCOPE);
		}
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final FaenorInterface _instance = new FaenorInterface();
	}

}