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
package com.src.gameserver.managers;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.Location;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.siege.Castle;
import com.src.gameserver.model.entity.siege.Siege;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;
import com.src.util.services.ConfigFiles;

public class SiegeManager
{
	private static final Log _log = LogFactory.getLog(SiegeManager.class);

	private static SiegeManager _instance;

	public static final SiegeManager getInstance()
	{
		if(_instance == null)
		{
			_instance = new SiegeManager();
		}

		return _instance;
	}

	private int _attackerMaxClans = 500;
	private int _attackerRespawnDelay = 20000;
	private int _defenderMaxClans = 500;
	private int _defenderRespawnDelay = 10000;

	private boolean _siegeEveryWeek = false;

	private FastMap<Integer, FastList<SiegeSpawn>> _artefactSpawnList;
	private FastMap<Integer, FastList<SiegeSpawn>> _flameTowerSpawnList;
	private FastMap<Integer, FastList<SiegeSpawn>> _controlTowerSpawnList;

	private int _controlTowerLosePenalty = 20000;
	private int _flagMaxCount = 1;
	private int _siegeClanMinLevel = 4;
	private int _siegeLength = 120;

	private SiegeManager()
	{
		load();
	}

	public final void addSiegeSkills(L2PcInstance character)
	{
		character.addSkill(SkillTable.getInstance().getInfo(246, 1), false);
		character.addSkill(SkillTable.getInstance().getInfo(247, 1), false);
	}

	public final boolean checkIfOkToSummon(L2Character activeChar, boolean isCheckOnly)
	{
		if(activeChar == null || !(activeChar instanceof L2PcInstance))
		{
			return false;
		}

		SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
		L2PcInstance player = (L2PcInstance) activeChar;
		Castle castle = CastleManager.getInstance().getCastle(player);

		if(castle == null || castle.getCastleId() <= 0)
		{
			sm.addString("You must be on castle ground to summon this.");
		}
		else if(!castle.getSiege().getIsInProgress())
		{
			sm.addString("You can only summon this during a siege.");
		}
		else if(player.getClanId() != 0 && castle.getSiege().getAttackerClan(player.getClanId()) == null)
		{
			sm.addString("You can only summon this as a registered attacker.");
		}
		else
		{
			return true;
		}

		if(!isCheckOnly)
		{
			player.sendPacket(sm);
		}
		sm = null;
		player = null;
		castle = null;

		return false;
	}

	public final boolean checkIsRegisteredInSiege(L2Clan clan)
	{
		for(Castle castle : CastleManager.getInstance().getCastles())
		{
			if(checkIsRegistered(clan, castle.getCastleId()) && castle.getSiege()!=null && castle.getSiege().getIsInProgress())
			{
				return true;
			}
		}

		return false;
	}

	public final boolean checkIsRegistered(L2Clan clan, int castleid)
	{
		if(clan == null)
		{
			return false;
		}

		if(clan.getHasCastle() > 0)
		{
			return true;
		}

		Connection con = null;
		boolean register = false;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT clan_id FROM siege_clans WHERE clan_id = ? AND castle_id = ?");
			statement.setInt(1, clan.getClanId());
			statement.setInt(2, castleid);
			ResultSet rs = statement.executeQuery();

			while(rs.next())
			{
				register = true;
				break;
			}

			rs.close();
			statement.close();
			statement = null;
			rs = null;
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
		return register;
	}

	public final void removeSiegeSkills(L2PcInstance character)
	{
		character.removeSkill(SkillTable.getInstance().getInfo(246, 1));
		character.removeSkill(SkillTable.getInstance().getInfo(247, 1));
	}

	private final void load()
	{
		try
		{
			InputStream is = new FileInputStream(new File(ConfigFiles.SIEGE_INI));
			Properties siegeSettings = new Properties();
			siegeSettings.load(is);
			is.close();
			is = null;

			// Siege setting
			_attackerMaxClans = Integer.decode(siegeSettings.getProperty("AttackerMaxClans", "500"));
			_attackerRespawnDelay = Integer.decode(siegeSettings.getProperty("AttackerRespawn", "30000"));
			_controlTowerLosePenalty = Integer.decode(siegeSettings.getProperty("CTLossPenalty", "20000"));
			_defenderMaxClans = Integer.decode(siegeSettings.getProperty("DefenderMaxClans", "500"));
			_defenderRespawnDelay = Integer.decode(siegeSettings.getProperty("DefenderRespawn", "20000"));
			_flagMaxCount = Integer.decode(siegeSettings.getProperty("MaxFlags", "1"));
			_siegeClanMinLevel = Integer.decode(siegeSettings.getProperty("SiegeClanMinLevel", "4"));
			_siegeLength = Integer.decode(siegeSettings.getProperty("SiegeLength", "120"));
			
			// Siege spawns settings
			_flameTowerSpawnList = new FastMap<Integer, FastList<SiegeSpawn>>();
			_controlTowerSpawnList = new FastMap<Integer, FastList<SiegeSpawn>>();
			_artefactSpawnList = new FastMap<Integer, FastList<SiegeSpawn>>();

			for(Castle castle : CastleManager.getInstance().getCastles())
			{
				FastList<SiegeSpawn> _controlTowersSpawns = new FastList<SiegeSpawn>();

				for(int i = 1; i < 0xFF; i++)
				{
					String _spawnParams = siegeSettings.getProperty(castle.getName() + "ControlTower" + Integer.toString(i), "");

					if(_spawnParams.length() == 0)
					{
						break;
					}

					StringTokenizer st = new StringTokenizer(_spawnParams.trim(), ",");

					_spawnParams = null;

					try
					{
						int x = Integer.parseInt(st.nextToken());
						int y = Integer.parseInt(st.nextToken());
						int z = Integer.parseInt(st.nextToken());
						int npc_id = Integer.parseInt(st.nextToken());
						int hp = Integer.parseInt(st.nextToken());

						_controlTowersSpawns.add(new SiegeSpawn(castle.getCastleId(), x, y, z, 0, npc_id, hp));

						st = null;
					}
					catch(Exception e)
					{
							e.printStackTrace();
						_log.warn("Error while loading control tower(s) for " + castle.getName() + " castle.");
					}
				}

				FastList<SiegeSpawn> _flameTowersSpawns = new FastList<SiegeSpawn>();

				for(int i = 1; i < 0xFF; i++)
				{
					String _spawnParams = siegeSettings.getProperty(castle.getName() + "FlameTower" + Integer.toString(i), "");

					if(_spawnParams.length() == 0)
					{
						break;
					}

					StringTokenizer st = new StringTokenizer(_spawnParams.trim(), ",");

					_spawnParams = null;

					try
					{
						int x = Integer.parseInt(st.nextToken());
						int y = Integer.parseInt(st.nextToken());
						int z = Integer.parseInt(st.nextToken());
						int npc_id = Integer.parseInt(st.nextToken());
						int hp = Integer.parseInt(st.nextToken());

						_flameTowersSpawns.add(new SiegeSpawn(castle.getCastleId(), x, y, z, 0, npc_id, hp));

						st = null;
					}
					catch(Exception e)
					{
						_log.error("Error while loading flame tower(s) for " + castle.getName() + " castle.", e);
					}
				}
				
				FastList<SiegeSpawn> _artefactSpawns = new FastList<SiegeSpawn>();

				for(int i = 1; i < 0xFF; i++)
				{
					String _spawnParams = siegeSettings.getProperty(castle.getName() + "Artefact" + Integer.toString(i), "");

					if(_spawnParams.length() == 0)
					{
						break;
					}

					StringTokenizer st = new StringTokenizer(_spawnParams.trim(), ",");

					_spawnParams = null;

					try
					{
						int x = Integer.parseInt(st.nextToken());
						int y = Integer.parseInt(st.nextToken());
						int z = Integer.parseInt(st.nextToken());
						int heading = Integer.parseInt(st.nextToken());
						int npc_id = Integer.parseInt(st.nextToken());

						st = null;
						_artefactSpawns.add(new SiegeSpawn(castle.getCastleId(), x, y, z, heading, npc_id));
					}
					catch(Exception e)
					{
							e.printStackTrace();
						_log.warn("Error while loading artefact(s) for " + castle.getName() + " castle.");
					}
				}

				_flameTowerSpawnList.put(castle.getCastleId(), _flameTowersSpawns);
				_controlTowerSpawnList.put(castle.getCastleId(), _controlTowersSpawns);
				_artefactSpawnList.put(castle.getCastleId(), _artefactSpawns);

				_artefactSpawns = null;
				_controlTowersSpawns = null;
				_flameTowersSpawns = null;
			}

			siegeSettings = null;

		}
		catch(Exception e)
		{
			//_initialized = false;
			System.err.println("Error while loading siege data.");
			e.printStackTrace();
		}
	}

	public final FastList<SiegeSpawn> getArtefactSpawnList(int _castleId)
	{
		if(_artefactSpawnList.containsKey(_castleId))
		{
			return _artefactSpawnList.get(_castleId);
		}
		else
		{
			return null;
		}
	}

	public final FastList<SiegeSpawn> getFlameTowerSpawnList(int _castleId)
	{
		if(_flameTowerSpawnList.containsKey(_castleId))
		{
			return _flameTowerSpawnList.get(_castleId);
		}
		else
		{
			return null;
		}
	}

	public final FastList<SiegeSpawn> getControlTowerSpawnList(int _castleId)
	{
		if(_controlTowerSpawnList.containsKey(_castleId))
		{
			return _controlTowerSpawnList.get(_castleId);
		}
		else
		{
			return null;
		}
	}

	public final boolean getEveryWeek()
	{
		return _siegeEveryWeek;
	}

	public final int getAttackerMaxClans()
	{
		return _attackerMaxClans;
	}

	public final int getAttackerRespawnDelay()
	{
		return _attackerRespawnDelay;
	}

	public final int getControlTowerLosePenalty()
	{
		return _controlTowerLosePenalty;
	}

	public final int getDefenderMaxClans()
	{
		return _defenderMaxClans;
	}

	public final int getDefenderRespawnDelay()
	{
		return _defenderRespawnDelay;
	}

	public final int getFlagMaxCount()
	{
		return _flagMaxCount;
	}

	public final Siege getSiege(L2Object activeObject)
	{
		return getSiege(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}

	public final Siege getSiege(int x, int y, int z)
	{
		for(Castle castle : CastleManager.getInstance().getCastles())
		{
			if(castle.getSiege().checkIfInZone(x, y, z))
			{
				return castle.getSiege();
			}
		}
		return null;
	}

	public final int getSiegeClanMinLevel()
	{
		return _siegeClanMinLevel;
	}

	public final int getSiegeLength()
	{
		return _siegeLength;
	}

	public final List<Siege> getSieges()
	{
		FastList<Siege> _sieges = new FastList<Siege>();
		for(Castle castle : CastleManager.getInstance().getCastles())
		{
			_sieges.add(castle.getSiege());
		}
		return _sieges;
	}

	public class SiegeSpawn
	{
		Location _location;
		private int _npcId;
		private int _heading;
		private int _castleId;
		private int _hp;

		public SiegeSpawn(int castle_id, int x, int y, int z, int heading, int npc_id)
		{
			_castleId = castle_id;
			_location = new Location(x, y, z, heading);
			_heading = heading;
			_npcId = npc_id;
		}

		public SiegeSpawn(int castle_id, int x, int y, int z, int heading, int npc_id, int hp)
		{
			_castleId = castle_id;
			_location = new Location(x, y, z, heading);
			_heading = heading;
			_npcId = npc_id;
			_hp = hp;
		}

		public int getCastleId()
		{
			return _castleId;
		}

		public int getNpcId()
		{
			return _npcId;
		}

		public int getHeading()
		{
			return _heading;
		}

		public int getHp()
		{
			return _hp;
		}

		public Location getLocation()
		{
			return _location;
		}
	}

}