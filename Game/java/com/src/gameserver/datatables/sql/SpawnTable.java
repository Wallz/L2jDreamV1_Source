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
package com.src.gameserver.datatables.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.managers.DayNightSpawnManager;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class SpawnTable
{
	private static final Log _log = LogFactory.getLog(SpawnTable.class.getName());

	private static final SpawnTable _instance = new SpawnTable();

	private Map<Integer, L2Spawn> _spawntable = new FastMap<Integer, L2Spawn>().shared();
	@SuppressWarnings("unused")
	private int _npcSpawnCount;
	private int _customSpawnCount;

	private int _highestId;

	public static SpawnTable getInstance()
	{
		return _instance;
	}

	private SpawnTable()
	{
		if(!Config.ALT_DEV_NO_SPAWNS)
		{
			fillSpawnTable();
		}
	}

	public Map<Integer, L2Spawn> getSpawnTable()
	{
		return _spawntable;
	}

	private void fillSpawnTable()
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;

			if(!Config.DELETE_SPAWN_ON_SPAWNLIST)
			{
				statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay, loc_id, periodOfDay FROM spawnlist WHERE id NOT IN ( SELECT id FROM custom_notspawned WHERE isCustom = false ) ORDER BY id");
			}
			else
			{
				statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay, loc_id, periodOfDay FROM spawnlist ORDER BY id");
			}

			ResultSet rset = statement.executeQuery();

			L2Spawn spawnDat;
			L2NpcTemplate template1;

			while(rset.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if(template1 != null)
				{
					if(template1.type.equalsIgnoreCase("L2SiegeGuard"))
					{
					}
					else if(template1.type.equalsIgnoreCase("L2RaidBoss"))
					{
					}
					else if(!Config.ALLOW_CLASS_MASTERS && template1.type.equals("L2ClassMaster"))
					{
					}
					else
					{
						spawnDat = new L2Spawn(template1);
						spawnDat.setId(rset.getInt("id"));
						spawnDat.setAmount(rset.getInt("count"));
						spawnDat.setLocx(rset.getInt("locx"));
						spawnDat.setLocy(rset.getInt("locy"));
						spawnDat.setLocz(rset.getInt("locz"));
						spawnDat.setHeading(rset.getInt("heading"));
						spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));

						int loc_id = rset.getInt("loc_id");

						spawnDat.setLocation(loc_id);

						template1 = null;

						switch(rset.getInt("periodOfDay"))
						{
							case 0:
								_npcSpawnCount += spawnDat.init();
								break;
							case 1:
								DayNightSpawnManager.getInstance().addDayCreature(spawnDat);
								_npcSpawnCount++;
								break;
							case 2:
								DayNightSpawnManager.getInstance().addNightCreature(spawnDat);
								_npcSpawnCount++;
								break;
						}

						_spawntable.put(spawnDat.getId(), spawnDat);
						if(spawnDat.getId() > _highestId)
						{
							_highestId = spawnDat.getId();
						}

						spawnDat = null;
					}
				}
				else
				{
					_log.warn("SpawnTable: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
				}
			}
			statement.close();
			rset.close();
			statement = null;
			rset = null;
		}
		catch(Exception e)
		{
			_log.error("SpawnTable: Spawn could not be initialized", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}

		_log.info("SpawnTable: Loaded " + _spawntable.size() + " Npc Spawn Locations.");

		if(Config.CUSTOM_SPAWNLIST_TABLE)
		{
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement;

				if(!Config.DELETE_GMSPAWN_ON_CUSTOM)
				{
					statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay, loc_id, periodOfDay FROM custom_spawnlist where id NOT in ( select id from custom_notspawned where isCustom = false ) ORDER BY id");
				}
				else
				{
					statement = con.prepareStatement("SELECT id, count, npc_templateid, locx, locy, locz, heading, respawn_delay, loc_id, periodOfDay FROM custom_spawnlist ORDER BY id");
				}

				ResultSet rset = statement.executeQuery();

				L2Spawn spawnDat;
				L2NpcTemplate template1;

				while(rset.next())
				{
					template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));

					if(template1 != null)
					{
						if(template1.type.equalsIgnoreCase("L2SiegeGuard"))
						{
						}
						else if(template1.type.equalsIgnoreCase("L2RaidBoss"))
						{
						}
						else if(!Config.ALLOW_CLASS_MASTERS && template1.type.equals("L2ClassMaster"))
						{
						}
						else
						{
							spawnDat = new L2Spawn(template1);
							spawnDat.setId(rset.getInt("id"));
							spawnDat.setAmount(rset.getInt("count"));
							spawnDat.setLocx(rset.getInt("locx"));
							spawnDat.setLocy(rset.getInt("locy"));
							spawnDat.setLocz(rset.getInt("locz"));
							spawnDat.setHeading(rset.getInt("heading"));
							spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));

							spawnDat.setCustom(true);
							int loc_id = rset.getInt("loc_id");

							spawnDat.setLocation(loc_id);

							template1 = null;

							switch(rset.getInt("periodOfDay"))
							{
								case 0:
									_customSpawnCount += spawnDat.init();
									break;
								case 1:
									DayNightSpawnManager.getInstance().addDayCreature(spawnDat);
									_customSpawnCount++;
									break;
								case 2:
									DayNightSpawnManager.getInstance().addNightCreature(spawnDat);
									_customSpawnCount++;
									break;
							}

							_spawntable.put(spawnDat.getId(), spawnDat);
							if(spawnDat.getId() > _highestId)
							{
								_highestId = spawnDat.getId();
							}

							template1 = null;
						}
					}
					else
					{
						_log.warn("CustomSpawnTable: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
					}
				}
				statement.close();
				rset.close();
				statement = null;
				rset = null;
			}
			catch(Exception e)
			{
				_log.error("SpawnTable: Spawn could not be initialized", e);
			}
			finally
			{
				ResourceUtil.closeConnection(con); 
			}

			_log.info("CustomSpawnTable: Loaded " + _customSpawnCount + " Npc Spawn Locations.");
		}
	}

	public L2Spawn getTemplate(int id)
	{
		return _spawntable.get(id);
	}

	public void addNewSpawn(L2Spawn spawn, boolean storeInDb)
	{
		_highestId++;
		spawn.setId(_highestId);
		_spawntable.put(_highestId, spawn);

		if(storeInDb)
		{
			Connection con = null;

			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("INSERT INTO " + (spawn.isCustom() ? "custom_spawnlist" : "spawnlist") + "(id, count, npc_templateid, locx,locy, locz, heading, respawn_delay, loc_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
				statement.setInt(1, spawn.getId());
				statement.setInt(2, spawn.getAmount());
				statement.setInt(3, spawn.getNpcid());
				statement.setInt(4, spawn.getLocx());
				statement.setInt(5, spawn.getLocy());
				statement.setInt(6, spawn.getLocz());
				statement.setInt(7, spawn.getHeading());
				statement.setInt(8, spawn.getRespawnDelay() / 1000);
				statement.setInt(9, spawn.getLocation());
				statement.execute();
				statement.close();
				statement = null;
			}
			catch(Exception e)
			{
				_log.error("SpawnTable: Could not store spawn in the DB", e);
			}
			finally
			{
				ResourceUtil.closeConnection(con); 
			}
		}
	}

	public void deleteSpawn(L2Spawn spawn, boolean updateDb)
	{
		if(_spawntable.remove(spawn.getId()) == null)
		{
			return;
		}

		if(updateDb)
		{
			Connection con = null;

			if((spawn.isCustom() && !Config.DELETE_GMSPAWN_ON_CUSTOM) || (!spawn.isCustom() && !Config.DELETE_SPAWN_ON_SPAWNLIST))
			{
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement = con.prepareStatement("REPLACE INTO custom_notspawned VALUES (?, ?)");
					statement.setInt(1, spawn.getId());
					statement.setBoolean(2, spawn.isCustom());
					statement.execute();
					statement.close();
					statement = null;
				}
				catch(Exception e)
				{
					_log.error("SpawnTable: Spawn " + spawn.getId() + " could not be removed from DB", e);
				}
				finally
				{
					ResourceUtil.closeConnection(con); 
				}
			}
			else
			{
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement = con.prepareStatement("DELETE FROM " + (spawn.isCustom() ? "custom_spawnlist" : "spawnlist") + " WHERE id = ?");
					statement.setInt(1, spawn.getId());
					statement.execute();
					statement.close();
					statement = null;
				}
				catch(Exception e)
				{
					_log.error("SpawnTable: Spawn " + spawn.getId() + " could not be removed from DB", e);
				}
				finally
				{
					ResourceUtil.closeConnection(con); 
				}
			}
		}
	}

	public void reloadAll()
	{
		fillSpawnTable();
	}

	public void findNPCInstances(L2PcInstance activeChar, int npcId, int teleportIndex)
	{
		int index = 0;
		for(L2Spawn spawn : _spawntable.values())
		{
			if(npcId == spawn.getNpcid())
			{
				index++;

				if(teleportIndex > -1)
				{
					if(teleportIndex == index)
					{
						activeChar.teleToLocation(spawn.getLocx(), spawn.getLocy(), spawn.getLocz(), true);
					}
				}
				else
				{
					activeChar.sendMessage(index + " - " + spawn.getTemplate().name + " (" + spawn.getId() + "): " + spawn.getLocx() + " " + spawn.getLocy() + " " + spawn.getLocz());
				}
			}
		}

		if(index == 0)
		{
			activeChar.sendMessage("No current spawns found.");
		}
	}

	public Map<Integer, L2Spawn> getAllTemplates()
	{
		return _spawntable;
	}

}