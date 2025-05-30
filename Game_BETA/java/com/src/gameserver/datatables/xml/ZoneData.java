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
package com.src.gameserver.datatables.xml;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.xml.parsers.DocumentBuilderFactory;

import javolution.util.FastList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.src.Config;
import com.src.gameserver.managers.ArenaManager;
import com.src.gameserver.managers.FishingZoneManager;
import com.src.gameserver.managers.GrandBossManager;
import com.src.gameserver.managers.OlympiadStadiaManager;
import com.src.gameserver.managers.TownManager;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.L2WorldRegion;
import com.src.gameserver.model.zone.L2ZoneType;
import com.src.gameserver.model.zone.form.ZoneCuboid;
import com.src.gameserver.model.zone.form.ZoneNPoly;
import com.src.gameserver.model.zone.type.L2ArenaZone;
import com.src.gameserver.model.zone.type.L2BigheadZone;
import com.src.gameserver.model.zone.type.L2BossZone;
import com.src.gameserver.model.zone.type.L2CastleTeleportZone;
import com.src.gameserver.model.zone.type.L2CastleZone;
import com.src.gameserver.model.zone.type.L2ClanHallZone;
import com.src.gameserver.model.zone.type.L2CustomZone;
import com.src.gameserver.model.zone.type.L2DamageZone;
import com.src.gameserver.model.zone.type.L2DerbyTrackZone;
import com.src.gameserver.model.zone.type.L2FishingZone;
import com.src.gameserver.model.zone.type.L2JailZone;
import com.src.gameserver.model.zone.type.L2MotherTreeZone;
import com.src.gameserver.model.zone.type.L2MultiConditionZone;
import com.src.gameserver.model.zone.type.L2NoHqZone;
import com.src.gameserver.model.zone.type.L2NoLandingZone;
import com.src.gameserver.model.zone.type.L2OlympiadStadiumZone;
import com.src.gameserver.model.zone.type.L2PeaceZone;
import com.src.gameserver.model.zone.type.L2PoisonZone;
import com.src.gameserver.model.zone.type.L2SkillZone;
import com.src.gameserver.model.zone.type.L2SwampZone;
import com.src.gameserver.model.zone.type.L2TownZone;
import com.src.gameserver.model.zone.type.L2WaterZone;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class ZoneData
{
	private static final Log _log = LogFactory.getLog(ZoneData.class.getName());

	private static ZoneData _instance;

	public static final ZoneData getInstance()
	{
		if(_instance == null)
		{
			_instance = new ZoneData();
		}

		return _instance;
	}

	public ZoneData()
	{
		load();
	}

	public void reload()
	{
		synchronized (_instance)
		{
			_instance = null;
			_instance = new ZoneData();
		}
	}

	private final void load()
	{
		Connection con = null;
		int zoneCount = 0;

		L2WorldRegion[][] worldRegions = L2World.getInstance().getAllWorldRegions();

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			File file = new File(Config.DATAPACK_ROOT + "/data/xml/zone.xml");
			if(!file.exists())
			{
				_log.info("The zone.xml file is missing.");
				return;
			}

			Document doc = factory.newDocumentBuilder().parse(file);
			factory = null;
			file = null;

			for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if("list".equalsIgnoreCase(n.getNodeName()))
				{
					for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if("zone".equalsIgnoreCase(d.getNodeName()))
						{
							NamedNodeMap attrs = d.getAttributes();

							int zoneId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							int minZ = Integer.parseInt(attrs.getNamedItem("minZ").getNodeValue());
							int maxZ = Integer.parseInt(attrs.getNamedItem("maxZ").getNodeValue());

							String zoneType = attrs.getNamedItem("type").getNodeValue();
							String zoneShape = attrs.getNamedItem("shape").getNodeValue();

							L2ZoneType temp = null;

							if(zoneType.equals("FishingZone"))
							{
								temp = new L2FishingZone(zoneId);
							}
							else if(zoneType.equals("ClanHallZone"))
							{
								temp = new L2ClanHallZone(zoneId);
							}
							else if(zoneType.equals("PeaceZone"))
							{
								temp = new L2PeaceZone(zoneId);
							}
							else if(zoneType.equals("Town"))
							{
								temp = new L2TownZone(zoneId);
							}
							else if(zoneType.equals("OlympiadStadium"))
							{
								temp = new L2OlympiadStadiumZone(zoneId);
							}
							else if(zoneType.equals("CastleZone"))
							{
								temp = new L2CastleZone(zoneId);
							}
							else if(zoneType.equals("DamageZone"))
							{
								temp = new L2DamageZone(zoneId);
							}
							else if(zoneType.equals("Arena"))
							{
								temp = new L2ArenaZone(zoneId);
							}
							else if(zoneType.equals("MotherTree"))
							{
								temp = new L2MotherTreeZone(zoneId);
							}
							else if(zoneType.equals("BigheadZone"))
							{
								temp = new L2BigheadZone(zoneId);
							}
							else if(zoneType.equals("NoLandingZone"))
							{
								temp = new L2NoLandingZone(zoneId);
							}
							else if(zoneType.equals("JailZone"))
							{
								temp = new L2JailZone(zoneId);
							}
							else if(zoneType.equals("DerbyTrackZone"))
							{
								temp = new L2DerbyTrackZone(zoneId);
							}
							else if(zoneType.equals("WaterZone"))
							{
								temp = new L2WaterZone(zoneId);
							}
							else if(zoneType.equals("NoHqZone"))
							{
								temp = new L2NoHqZone(zoneId);
							}
							else if(zoneType.equals("BossZone"))
							{
								int boss_id = -1;
								try
								{
									boss_id = Integer.parseInt(attrs.getNamedItem("bossId").getNodeValue());
								}
								catch(IllegalArgumentException e)
								{
									e.printStackTrace();
								}
								temp = new L2BossZone(zoneId, boss_id);
							}
							else if(zoneType.equals("SkillZone"))
							{
								temp = new L2SkillZone(zoneId);
							}
							else if(zoneType.equals("PoisonZone"))
							{
								temp = new L2PoisonZone(zoneId);
							}
							else if(zoneType.equals("CastleTeleportZone"))
							{
								temp = new L2CastleTeleportZone(zoneId);
							}
							else if(zoneType.equals("CustomZone"))
							{
								temp = new L2CustomZone(zoneId);
							}
							else if(zoneType.equals("SwampZone"))
							{
								temp = new L2SwampZone(zoneId);
							}
							else if(zoneType.equals("MultiCondition"))
							{
								temp = new L2MultiConditionZone(zoneId);
							}

							if(temp == null)
							{
								_log.warn("ZoneData: No such zone type: " + zoneType);
								continue;
							}

							zoneType = null;

							try
							{
								PreparedStatement statement = null;

								statement = con.prepareStatement("SELECT x,y FROM zone_vertices WHERE id = ? ORDER BY 'order' ASC ");

								statement.setInt(1, zoneId);
								ResultSet rset = statement.executeQuery();

								if(zoneShape.equals("Cuboid"))
								{
									int[] x = {0, 0};
									int[] y = {0, 0};
									boolean successfulLoad = true;

									for(int i = 0; i < 2; i++)
									{
										if(rset.next())
										{
											x[i] = rset.getInt("x");
											y[i] = rset.getInt("y");
										}
										else
										{
											_log.warn("ZoneData: Missing cuboid vertex in sql data for zone: " + zoneId);
											statement.close();
											rset.close();
											successfulLoad = false;
											break;
										}
									}

									if(successfulLoad)
									{
										temp.setZone(new ZoneCuboid(x[0], x[1], y[0], y[1], minZ, maxZ));
									}
									else
									{
										continue;
									}
								}
								else if(zoneShape.equals("NPoly"))
								{
									FastList<Integer> fl_x = new FastList<Integer>(), fl_y = new FastList<Integer>();

									while(rset.next())
									{
										fl_x.add(rset.getInt("x"));
										fl_y.add(rset.getInt("y"));
									}

									if(fl_x.size() == fl_y.size() && fl_x.size() > 2)
									{
										int[] aX = new int[fl_x.size()];
										int[] aY = new int[fl_y.size()];

										for(int i = 0; i < fl_x.size(); i++)
										{
											aX[i] = fl_x.get(i);
											aY[i] = fl_y.get(i);
										}

										temp.setZone(new ZoneNPoly(aX, aY, minZ, maxZ));
									}
									else
									{
										_log.warn("ZoneData: Bad sql data for zone: " + zoneId);
										statement.close();
										rset.close();
										continue;
									}

									fl_x = null;
								}
								else
								{
									_log.warn("ZoneData: Unknown shape: " + zoneShape);
									statement.close();
									rset.close();
									continue;
								}

								statement.close();
								rset.close();
								statement = null;
								rset = null;
							}
							catch(Exception e)
							{
								_log.error("ZoneData: Failed to load zone coordinates", e);
							}

							zoneShape = null;

							for(Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
							{
								if("stat".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									String name = attrs.getNamedItem("name").getNodeValue();
									String val = attrs.getNamedItem("val").getNodeValue();
									attrs = null;

									temp.setParameter(name, val);
									name = null;
									val = null;
								}
								if("spawn".equalsIgnoreCase(cd.getNodeName()))
								{
									temp.setSpawnLocs(cd);
								}
							}

							if(temp instanceof L2FishingZone)
							{
								FishingZoneManager.getInstance().addFishingZone((L2FishingZone) temp);
								continue;
							}

							if(temp instanceof L2WaterZone)
							{
								FishingZoneManager.getInstance().addWaterZone((L2WaterZone) temp);
							}

							int ax, ay, bx, by;

							for(int x = 0; x < worldRegions.length; x++)
							{
								for(int y = 0; y < worldRegions[x].length; y++)
								{
									ax = x - L2World.OFFSET_X << L2World.SHIFT_BY;
									bx = x + 1 - L2World.OFFSET_X << L2World.SHIFT_BY;
									ay = y - L2World.OFFSET_Y << L2World.SHIFT_BY;
									by = y + 1 - L2World.OFFSET_Y << L2World.SHIFT_BY;

									if(temp.getZone().intersectsRectangle(ax, bx, ay, by))
									{
										worldRegions[x][y].addZone(temp);
									}
								}
							}

							if(temp instanceof L2ArenaZone)
							{
								ArenaManager.getInstance().addArena((L2ArenaZone) temp);
							}
							else if(temp instanceof L2TownZone)
							{
								TownManager.getInstance().addTown((L2TownZone) temp);
							}
							else if(temp instanceof L2OlympiadStadiumZone)
							{
								OlympiadStadiaManager.getInstance().addStadium((L2OlympiadStadiumZone) temp);
							}
							else if(temp instanceof L2BossZone)
							{
								GrandBossManager.getInstance().addZone((L2BossZone) temp);
							}

							zoneCount++;

							temp = null;
							attrs = null;
						}
					}
				}
			}

			doc = null;
		}
		catch(Exception e)
		{
			_log.error("Error while loading zones.", e);
			return;
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
		GrandBossManager.getInstance().initZones();

		_log.info("ZoneData: Loaded " + zoneCount + " zones.");
	}
}