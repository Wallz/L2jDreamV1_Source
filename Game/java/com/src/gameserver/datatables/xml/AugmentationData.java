/* This program is free software; you can redistribute it and/or modify
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
package com.src.gameserver.datatables.xml;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilderFactory;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.src.Config;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.model.L2Augmentation;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.skills.Stats;
import com.src.util.random.Rnd;

public class AugmentationData
{
	private static final Log _log = LogFactory.getLog(AugmentationData.class.getName());

	private static AugmentationData _instance;

	public static final AugmentationData getInstance()
	{
		if(_instance == null)
		{
			_instance = new AugmentationData();
		}

		return _instance;
	}

	private static final int STAT_START = 1;
	private static final int STAT_END = 14560;
	private static final int STAT_BLOCKSIZE = 3640;
	private static final int STAT_SUBBLOCKSIZE = 91;

	private static final int BLUE_START = 14561;
	private static final int SKILLS_BLOCKSIZE = 178;

	private static final int BASESTAT_STR = 16341;
	private static final int BASESTAT_CON = 16342;
	private static final int BASESTAT_INT = 16343;
	private static final int BASESTAT_MEN = 16344;

	private FastList<augmentationStat> _augmentationStats[];
	private Map<Integer, FastList<augmentationSkill>> _blueSkills;
	private Map<Integer, FastList<augmentationSkill>> _purpleSkills;
	private Map<Integer, FastList<augmentationSkill>> _redSkills;

	@SuppressWarnings("unchecked")
	public AugmentationData()
	{
		_augmentationStats = new FastList[4];
		_augmentationStats[0] = new FastList<augmentationStat>();
		_augmentationStats[1] = new FastList<augmentationStat>();
		_augmentationStats[2] = new FastList<augmentationStat>();
		_augmentationStats[3] = new FastList<augmentationStat>();

		_blueSkills = new FastMap<Integer, FastList<augmentationSkill>>();
		_purpleSkills = new FastMap<Integer, FastList<augmentationSkill>>();
		_redSkills = new FastMap<Integer, FastList<augmentationSkill>>();
		for(int i = 1; i <= 10; i++)
		{
			_blueSkills.put(i, new FastList<augmentationSkill>());
			_purpleSkills.put(i, new FastList<augmentationSkill>());
			_redSkills.put(i, new FastList<augmentationSkill>());
		}

		load();

		_log.info("AugmentationData: Loaded " + _augmentationStats[0].size() * 4 + " augmentation stats.");
	}

	public class augmentationSkill
	{
		private int _skillId;
		private int _maxSkillLevel;
		private int _augmentationSkillId;

		public augmentationSkill(int skillId, int maxSkillLevel, int augmentationSkillId)
		{
			_skillId = skillId;
			_maxSkillLevel = maxSkillLevel;
			_augmentationSkillId = augmentationSkillId;
		}

		public L2Skill getSkill(int level)
		{
			if(level > _maxSkillLevel)
			{
				return SkillTable.getInstance().getInfo(_skillId, _maxSkillLevel);
			}

			return SkillTable.getInstance().getInfo(_skillId, level);
		}

		public int getAugmentationSkillId()
		{
			return _augmentationSkillId;
		}
	}

	public class augmentationStat
	{
		private Stats _stat;
		private int _singleSize;
		private int _combinedSize;
		private float _singleValues[];
		private float _combinedValues[];

		public augmentationStat(Stats stat, float sValues[], float cValues[])
		{
			_stat = stat;
			_singleSize = sValues.length;
			_singleValues = sValues;
			_combinedSize = cValues.length;
			_combinedValues = cValues;
		}

		public int getSingleStatSize()
		{
			return _singleSize;
		}

		public int getCombinedStatSize()
		{
			return _combinedSize;
		}

		public float getSingleStatValue(int i)
		{
			if(i >= _singleSize || i < 0)
			{
				return _singleValues[_singleSize - 1];
			}

			return _singleValues[i];
		}

		public float getCombinedStatValue(int i)
		{
			if(i >= _combinedSize || i < 0)
			{
				return _combinedValues[_combinedSize - 1];
			}

			return _combinedValues[i];
		}

		public Stats getStat()
		{
			return _stat;
		}
	}

	private final void load()
	{
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			int badAugmantData = 0;

			File file = new File(Config.DATAPACK_ROOT + "/data/stats/augmentation/augmentation_skillmap.xml");
			if(!file.exists())
			{
				return;
			}

			Document doc = factory.newDocumentBuilder().parse(file);

			for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if("list".equalsIgnoreCase(n.getNodeName()))
				{
					for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if("augmentation".equalsIgnoreCase(d.getNodeName()))
						{
							NamedNodeMap attrs = d.getAttributes();
							int skillId = 0, augmentationId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							String type = "blue";
							int skillLvL = 0;

							for(Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
							{
								if("skillId".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									skillId = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
								}
								else if("skillLevel".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									skillLvL = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
								}
								else if("type".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									type = attrs.getNamedItem("val").getNodeValue();
								}
							}

							if(skillId == 0)
							{
								badAugmantData++;
								continue;
							}
							else if(skillLvL == 0)
							{
								badAugmantData++;
								continue;
							}

							int k = 1;
							while(augmentationId - k * SKILLS_BLOCKSIZE >= BLUE_START)
							{
								k++;
							}

							if(type.equalsIgnoreCase("blue"))
							{
								_blueSkills.get(k).add(new augmentationSkill(skillId, skillLvL, augmentationId));
							}
							else if(type.equalsIgnoreCase("purple"))
							{
								_purpleSkills.get(k).add(new augmentationSkill(skillId, skillLvL, augmentationId));
							}
							else
							{
								_redSkills.get(k).add(new augmentationSkill(skillId, skillLvL, augmentationId));
							}

							attrs = null;
						}
					}
				}
			}

			if(badAugmantData != 0)
			{
				_log.info("AugmentationData: " + badAugmantData + " bad skill(s) were skipped.");
			}

			doc = null;
			file = null;
		}
		catch(Exception e)
		{
			_log.error("Error parsing augmentation_skillmap.xml.", e);

			return;
		}

		for(int i = 1; i < 5; i++)
		{
			try
			{
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setValidating(false);
				factory.setIgnoringComments(true);

				File file = new File(Config.DATAPACK_ROOT + "/data/stats/augmentation/augmentation_stats" + i + ".xml");

				if(!file.exists())
				{
					return;
				}

				Document doc = factory.newDocumentBuilder().parse(file);

				for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
				{
					if("list".equalsIgnoreCase(n.getNodeName()))
					{
						for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if("stat".equalsIgnoreCase(d.getNodeName()))
							{
								NamedNodeMap attrs = d.getAttributes();
								String statName = attrs.getNamedItem("name").getNodeValue();

								float soloValues[] = null, combinedValues[] = null;

								for(Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
								{
									if("table".equalsIgnoreCase(cd.getNodeName()))
									{
										attrs = cd.getAttributes();
										String tableName = attrs.getNamedItem("name").getNodeValue();

										StringTokenizer data = new StringTokenizer(cd.getFirstChild().getNodeValue());
										List<Float> array = new FastList<Float>();

										while(data.hasMoreTokens())
										{
											array.add(Float.parseFloat(data.nextToken()));
										}

										if(tableName.equalsIgnoreCase("#soloValues"))
										{
											soloValues = new float[array.size()];
											int x = 0;

											for(float value : array)
											{
												soloValues[x++] = value;
											}
										}
										else
										{
											combinedValues = new float[array.size()];
											int x = 0;

											for(float value : array)
											{
												combinedValues[x++] = value;
											}
										}

										tableName = null;
									}
								}

								_augmentationStats[(i - 1)].add(new augmentationStat(Stats.valueOfXml(statName), soloValues, combinedValues));

								statName = null;
								attrs = null;
							}
						}
					}
				}

				doc = null;
				file = null;
				factory = null;
			}
			catch(Exception e)
			{
				_log.error("Error parsing augmentation_stats" + i + ".xml.", e);
				return;
			}
		}
	}

	public L2Augmentation generateRandomAugmentation(L2ItemInstance item, int lifeStoneLevel, int lifeStoneGrade)
	{
		int skill_Chance = 0;
		int stat34 = 0;
		boolean generateSkill = false;
		int resultColor = 0;
		boolean generateGlow = false;

		switch(lifeStoneGrade)
		{
			case 0:
				skill_Chance = Config.AUGMENTATION_NG_SKILL_CHANCE;

				if(Rnd.get(1, 100) <= Config.AUGMENTATION_NG_GLOW_CHANCE)
				{
					generateGlow = true;
				}
				break;

			case 1:
				skill_Chance = Config.AUGMENTATION_MID_SKILL_CHANCE;

				if(Rnd.get(1, 100) <= Config.AUGMENTATION_MID_GLOW_CHANCE)
				{
					generateGlow = true;
				}
				break;

			case 2:
				skill_Chance = Config.AUGMENTATION_HIGH_SKILL_CHANCE;

				if(Rnd.get(1, 100) <= Config.AUGMENTATION_HIGH_GLOW_CHANCE)
				{
					generateGlow = true;
				}
				break;

			case 3:
				skill_Chance = Config.AUGMENTATION_TOP_SKILL_CHANCE;

				if(Rnd.get(1, 100) <= Config.AUGMENTATION_TOP_GLOW_CHANCE)
				{
					generateGlow = true;
				}
		}

		if(Rnd.get(1, 100) <= skill_Chance)
		{
			generateSkill = true;
		}
		else if(Rnd.get(1, 100) <= Config.AUGMENTATION_BASESTAT_CHANCE)
		{
			stat34 = Rnd.get(BASESTAT_STR, BASESTAT_MEN);
		}

		if(stat34 == 0 && !generateSkill)
		{
			resultColor = Rnd.get(0, 100);

			if(resultColor <= 15 * lifeStoneGrade + 40)
			{
				resultColor = 1;
			}
			else
			{
				resultColor = 0;
			}
		}
		else
		{
			resultColor = Rnd.get(0, 100);

			if(resultColor <= 10 * lifeStoneGrade + 5 || stat34 != 0)
			{
				resultColor = 3;
			}
			else if(resultColor <= 10 * lifeStoneGrade + 10)
			{
				resultColor = 1;
			}
			else
			{
				resultColor = 2;
			}
		}

		int stat12 = 0;

		if(stat34 == 0 && !generateSkill)
		{
			int temp = Rnd.get(2, 3);
			int colorOffset = resultColor * 10 * STAT_SUBBLOCKSIZE + temp * STAT_BLOCKSIZE + 1;
			int offset = (lifeStoneLevel - 1) * STAT_SUBBLOCKSIZE + colorOffset;

			stat34 = Rnd.get(offset, offset + STAT_SUBBLOCKSIZE - 1);

			if(generateGlow && lifeStoneGrade >= 2)
			{
				offset = (lifeStoneLevel - 1) * STAT_SUBBLOCKSIZE + (temp - 2) * STAT_BLOCKSIZE + lifeStoneGrade * 10 * STAT_SUBBLOCKSIZE + 1;
			}
			else
			{
				offset = (lifeStoneLevel - 1) * STAT_SUBBLOCKSIZE + (temp - 2) * STAT_BLOCKSIZE + Rnd.get(0, 1) * 10 * STAT_SUBBLOCKSIZE + 1;
			}
			stat12 = Rnd.get(offset, offset + STAT_SUBBLOCKSIZE - 1);
		}
		else
		{
			int offset;

			if(!generateGlow)
			{
				offset = (lifeStoneLevel - 1) * STAT_SUBBLOCKSIZE + Rnd.get(0, 1) * STAT_BLOCKSIZE + 1;
			}
			else
			{
				offset = (lifeStoneLevel - 1) * STAT_SUBBLOCKSIZE + Rnd.get(0, 1) * STAT_BLOCKSIZE + (lifeStoneGrade + resultColor) / 2 * 10 * STAT_SUBBLOCKSIZE + 1;
			}
			stat12 = Rnd.get(offset, offset + STAT_SUBBLOCKSIZE - 1);
		}

		L2Skill skill = null;
		if(generateSkill)
		{
			augmentationSkill temp = null;
			switch(resultColor)
			{
				case 1:
					temp = _blueSkills.get(lifeStoneLevel).get(Rnd.get(0, _blueSkills.get(lifeStoneLevel).size() - 1));
					skill = temp.getSkill(lifeStoneLevel);
					stat34 = temp.getAugmentationSkillId();
					break;
				case 2:
					temp = _purpleSkills.get(lifeStoneLevel).get(Rnd.get(0, _purpleSkills.get(lifeStoneLevel).size() - 1));
					skill = temp.getSkill(lifeStoneLevel);
					stat34 = temp.getAugmentationSkillId();
					break;
				case 3:
					temp = _redSkills.get(lifeStoneLevel).get(Rnd.get(0, _redSkills.get(lifeStoneLevel).size() - 1));
					skill = temp.getSkill(lifeStoneLevel);
					stat34 = temp.getAugmentationSkillId();
					break;
			}
		}

		return new L2Augmentation(item, ((stat34 << 16) + stat12), skill, true);
	}

	public class AugStat
	{
		private Stats _stat;
		private float _value;

		public AugStat(Stats stat, float value)
		{
			_stat = stat;
			_value = value;
		}

		public Stats getStat()
		{
			return _stat;
		}

		public float getValue()
		{
			return _value;
		}
	}

	public FastList<AugStat> getAugStatsById(int augmentationId)
	{
		FastList<AugStat> temp = new FastList<AugStat>();
		int stats[] = new int[2];
		stats[0] = 0x0000FFFF & augmentationId;
		stats[1] = augmentationId >> 16;

		for(int i = 0; i < 2; i++)
		{
			if(stats[i] >= STAT_START && stats[i] <= STAT_END)
			{
				int block = 0;

				while(stats[i] > STAT_BLOCKSIZE)
				{
					stats[i] -= STAT_BLOCKSIZE;
					block++;
				}

				int subblock = 0;

				while(stats[i] > STAT_SUBBLOCKSIZE)
				{
					stats[i] -= STAT_SUBBLOCKSIZE;
					subblock++;
				}

				if(stats[i] < 14)
				{
					augmentationStat as = _augmentationStats[block].get((stats[i] - 1));
					temp.add(new AugStat(as.getStat(), as.getSingleStatValue(subblock)));
				}
				else
				{
					stats[i] -= 13;

					int x = 12;
					int rescales = 0;

					while(stats[i] > x)
					{
						stats[i] -= x;
						x--;
						rescales++;
					}

					augmentationStat as = _augmentationStats[block].get(rescales);
					if(rescales == 0)
					{
						temp.add(new AugStat(as.getStat(), as.getCombinedStatValue(subblock)));
					}
					else
					{
						temp.add(new AugStat(as.getStat(), as.getCombinedStatValue(subblock * 2 + 1)));
					}

					as = _augmentationStats[block].get(rescales + stats[i]);
					if(as.getStat() == Stats.CRITICAL_DAMAGE)
					{
						temp.add(new AugStat(as.getStat(), as.getCombinedStatValue(subblock)));
					}
					else
					{
						temp.add(new AugStat(as.getStat(), as.getCombinedStatValue(subblock * 2)));
					}

					as = null;
				}
			}
			else if(stats[i] >= BASESTAT_STR && stats[i] <= BASESTAT_MEN)
			{
				switch(stats[i])
				{
					case BASESTAT_STR:
						temp.add(new AugStat(Stats.STAT_STR, 1.0f));
						break;
					case BASESTAT_CON:
						temp.add(new AugStat(Stats.STAT_CON, 1.0f));
						break;
					case BASESTAT_INT:
						temp.add(new AugStat(Stats.STAT_INT, 1.0f));
						break;
					case BASESTAT_MEN:
						temp.add(new AugStat(Stats.STAT_MEN, 1.0f));
						break;
				}
			}
		}

		return temp;
	}

}