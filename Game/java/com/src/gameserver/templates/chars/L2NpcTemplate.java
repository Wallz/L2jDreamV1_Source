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
package com.src.gameserver.templates.chars;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;

import com.src.gameserver.ai.special.AIExtend;
import com.src.gameserver.model.L2DropCategory;
import com.src.gameserver.model.L2DropData;
import com.src.gameserver.model.L2MinionData;
import com.src.gameserver.model.L2NpcAIData;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.base.ClassId;
import com.src.gameserver.model.quest.Quest;
import com.src.gameserver.skills.Stats;
import com.src.gameserver.templates.StatsSet;

public final class L2NpcTemplate extends L2CharTemplate
{
	protected static final Logger _log = Logger.getLogger(Quest.class.getName());

	public final int npcId;
	public final int idTemplate;
	public final String type;
	public final String name;
	public final boolean serverSideName;
	public final String title;
	public final boolean serverSideTitle;
	public final String sex;
	public final byte level;
	public final int rewardExp;
	public final int rewardSp;
	public final int aggroRange;
	public final int rhand;
	public final int lhand;
	public final int armor;
	public final String factionId;
	public final int factionRange;
	public final int absorbLevel;
	public final AbsorbCrystalType absorbType;
	public Race race;

	private boolean _custom;

	public static enum AbsorbCrystalType
	{
		LAST_HIT,
		FULL_PARTY,
		PARTY_ONE_RANDOM
	}

	public static enum Race
	{
		UNDEAD,
		MAGICCREATURE,
		BEAST,
		ANIMAL,
		PLANT,
		HUMANOID,
		SPIRIT,
		ANGEL,
		DEMON,
		DRAGON,
		GIANT,
		BUG,
		FAIRIE,
		HUMAN,
		ELVE,
		DARKELVE,
		ORC,
		DWARVE,
		OTHER,
		NONLIVING,
		SIEGEWEAPON,
		DEFENDINGARMY,
		MERCENARIE,
		UNKNOWN
	}

	private final StatsSet _npcStatsSet;
	private L2NpcAIData _AIdataStatic = new L2NpcAIData();
	private final FastList<L2DropCategory> _categories = new FastList<L2DropCategory>();

	private final List<L2MinionData> _minions = new FastList<L2MinionData>(0);

	private List<ClassId> _teachInfo;
	private Map<Integer, L2Skill> _skills;
	private Map<Stats, Double> _vulnerabilities;
	private Map<Quest.QuestEventType, Quest[]> _questEvents;
	private static FastMap<AIExtend.Action, AIExtend[]> _aiEvents;

	public L2NpcTemplate(StatsSet set, boolean custom)
	{
		super(set);
		npcId = set.getInteger("npcId");
		idTemplate = set.getInteger("idTemplate");
		type = set.getString("type");
		name = set.getString("name");
		serverSideName = set.getBool("serverSideName");
		title = set.getString("title");
		serverSideTitle = set.getBool("serverSideTitle");
		sex = set.getString("sex");
		level = set.getByte("level");
		rewardExp = set.getInteger("rewardExp");
		rewardSp = set.getInteger("rewardSp");
		aggroRange = set.getInteger("aggroRange");
		rhand = set.getInteger("rhand");
		lhand = set.getInteger("lhand");
		armor = set.getInteger("armor");
		String f = set.getString("factionId", null);
		if(f == null)
		{
			factionId = null;
		}
		else
		{
			factionId = f.intern();
		}
		factionRange = set.getInteger("factionRange", 0);
		absorbLevel = set.getInteger("absorb_level", 0);
		absorbType = AbsorbCrystalType.valueOf(set.getString("absorb_type"));
		race = null;
		_npcStatsSet = set;
		_teachInfo = null;
		_custom = custom;
	}

	public void addTeachInfo(ClassId classId)
	{
		if(_teachInfo == null)
		{
			_teachInfo = new FastList<ClassId>();
		}
		_teachInfo.add(classId);
	}

	public ClassId[] getTeachInfo()
	{
		if(_teachInfo == null)
		{
			return null;
		}
		return _teachInfo.toArray(new ClassId[_teachInfo.size()]);
	}

	public boolean canTeach(ClassId classId)
	{
		if(_teachInfo == null)
		{
			return false;
		}

		if(classId.getId() >= 88)
		{
			return _teachInfo.contains(classId.getParent());
		}

		return _teachInfo.contains(classId);
	}

	public void addDropData(L2DropData drop, int categoryType)
	{
		if(drop.isQuestDrop())
		{
		}
		else
		{
			synchronized (_categories)
			{
				boolean catExists = false;
				for(L2DropCategory cat : _categories)
				{
					if(cat.getCategoryType() == categoryType)
					{
						cat.addDropData(drop, type.equalsIgnoreCase("L2RaidBoss") || type.equalsIgnoreCase("L2GrandBoss"));
						catExists = true;
						break;
					}
				}
				if(!catExists)
				{
					L2DropCategory cat = new L2DropCategory(categoryType);
					cat.addDropData(drop, type.equalsIgnoreCase("L2RaidBoss") || type.equalsIgnoreCase("L2GrandBoss"));
					_categories.add(cat);
				}
			}
		}
	}

	public void addRaidData(L2MinionData minion)
	{
		_minions.add(minion);
	}

	public void addSkill(L2Skill skill)
	{
		if(_skills == null)
		{
			_skills = new FastMap<Integer, L2Skill>();
		}
		_skills.put(skill.getId(), skill);
	}

	public void addVulnerability(Stats id, double vuln)
	{
		if(_vulnerabilities == null)
		{
			_vulnerabilities = new FastMap<Stats, Double>();
		}
		_vulnerabilities.put(id, new Double(vuln));
	}

	public double getVulnerability(Stats id)
	{
		if(_vulnerabilities == null || _vulnerabilities.get(id) == null)
		{
			return 1;
		}
		return _vulnerabilities.get(id);
	}

	public double removeVulnerability(Stats id)
	{
		return _vulnerabilities.remove(id);
	}

	public FastList<L2DropCategory> getDropData()
	{
		return _categories;
	}

	public List<L2DropData> getAllDropData()
	{
		List<L2DropData> lst = new FastList<L2DropData>();
		for(L2DropCategory tmp : _categories)
		{
			lst.addAll(tmp.getAllDrops());
		}
		return lst;
	}

	public synchronized void clearAllDropData()
	{
		while(_categories.size() > 0)
		{
			_categories.getFirst().clearAllDrops();
			_categories.removeFirst();
		}
		_categories.clear();
	}

	public List<L2MinionData> getMinionData()
	{
		return _minions;
	}

	public Map<Integer, L2Skill> getSkills()
	{
		return _skills;
	}

	public void addQuestEvent(Quest.QuestEventType EventType, Quest q)
	{
		if(_questEvents == null)
		{
			_questEvents = new FastMap<Quest.QuestEventType, Quest[]>();
		}

		if(_questEvents.get(EventType) == null)
		{
			_questEvents.put(EventType, new Quest[]
			{
				q
			});
		}
		else
		{
			Quest[] _quests = _questEvents.get(EventType);
			int len = _quests.length;

			if(!EventType.isMultipleRegistrationAllowed())
			{
				if(_quests[0].getName().equals(q.getName()))
				{
					_quests[0] = q;
				}
				else
				{
					_log.warning("Quest event not allowed in multiple quests.  Skipped addition of Event Type \"" + EventType + "\" for NPC \"" + name + "\" and quest \"" + q.getName() + "\".");
				}
			}
			else
			{
				Quest[] tmp = new Quest[len + 1];

				for(int i = 0; i < len; i++)
				{
					if(_quests[i].getName().equals(q.getName()))
					{
						_quests[i] = q;
						return;
					}
					tmp[i] = _quests[i];
				}
				tmp[len] = q;
				_questEvents.put(EventType, tmp);
			}
		}
	}

	public Quest[] getEventQuests(Quest.QuestEventType EventType)
	{
		if(_questEvents == null)
		{
			return null;
		}
		return _questEvents.get(EventType);
	}

	public void addAIEvent(AIExtend.Action actionType, AIExtend ai)
	{
		if(_aiEvents == null)
		{
			_aiEvents = new FastMap<AIExtend.Action, AIExtend[]>();
		}

		if(_aiEvents.get(actionType) == null)
		{
			_aiEvents.put(actionType, new AIExtend[]
			{
				ai
			});
		}
		else
		{
			AIExtend[] _ai = _aiEvents.get(actionType);
			int len = _ai.length;

			if(!actionType.isRegistred())
			{
				if(_ai[0].getID() == ai.getID())
				{
					_ai[0] = ai;
				}
				else
				{
					_log.warning("Skipped AI: \"" + ai.getID() + "\".");
				}
			}
			else
			{
				AIExtend[] tmp = new AIExtend[len + 1];

				for(int i = 0; i < len; i++)
				{
					if(_ai[i].getID() == ai.getID())
					{
						_ai[i] = ai;
						return;
					}
					tmp[i] = _ai[i];
				}

				tmp[len] = ai;
				_aiEvents.put(actionType, tmp);
			}
		}
	}

	public static void clearAI()
	{
		if(_aiEvents != null)
		{
			_aiEvents.clear();
		}
	}

	public StatsSet getStatsSet()
	{
		return _npcStatsSet;
	}

	public void setRace(int raceId)
	{
		switch(raceId)
		{
			case 1:
				race = L2NpcTemplate.Race.UNDEAD;
				break;
			case 2:
				race = L2NpcTemplate.Race.MAGICCREATURE;
				break;
			case 3:
				race = L2NpcTemplate.Race.BEAST;
				break;
			case 4:
				race = L2NpcTemplate.Race.ANIMAL;
				break;
			case 5:
				race = L2NpcTemplate.Race.PLANT;
				break;
			case 6:
				race = L2NpcTemplate.Race.HUMANOID;
				break;
			case 7:
				race = L2NpcTemplate.Race.SPIRIT;
				break;
			case 8:
				race = L2NpcTemplate.Race.ANGEL;
				break;
			case 9:
				race = L2NpcTemplate.Race.DEMON;
				break;
			case 10:
				race = L2NpcTemplate.Race.DRAGON;
				break;
			case 11:
				race = L2NpcTemplate.Race.GIANT;
				break;
			case 12:
				race = L2NpcTemplate.Race.BUG;
				break;
			case 13:
				race = L2NpcTemplate.Race.FAIRIE;
				break;
			case 14:
				race = L2NpcTemplate.Race.HUMAN;
				break;
			case 15:
				race = L2NpcTemplate.Race.ELVE;
				break;
			case 16:
				race = L2NpcTemplate.Race.DARKELVE;
				break;
			case 17:
				race = L2NpcTemplate.Race.ORC;
				break;
			case 18:
				race = L2NpcTemplate.Race.DWARVE;
				break;
			case 19:
				race = L2NpcTemplate.Race.OTHER;
				break;
			case 20:
				race = L2NpcTemplate.Race.NONLIVING;
				break;
			case 21:
				race = L2NpcTemplate.Race.SIEGEWEAPON;
				break;
			case 22:
				race = L2NpcTemplate.Race.DEFENDINGARMY;
				break;
			case 23:
				race = L2NpcTemplate.Race.MERCENARIE;
				break;
			default:
				race = L2NpcTemplate.Race.UNKNOWN;
				break;
		}
	}

	public L2NpcTemplate.Race getRace()
	{
		if(race == null)
		{
			race = L2NpcTemplate.Race.UNKNOWN;
		}

		return race;
	}

	public byte getLevel()
	{
		return level;
	}

	public String getName()
	{
		return name;
	}

	public int getNpcId()
	{
		return npcId;
	}

	public final boolean isCustom()
	{
		return _custom;
	}

	public void setAIData(L2NpcAIData aidata)
	{
		_AIdataStatic = aidata;
	}
	
	public L2NpcAIData getAIDataStatic()
	{
		return _AIdataStatic;
	}
	
}