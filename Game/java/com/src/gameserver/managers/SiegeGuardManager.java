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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import javolution.util.FastList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.siege.Castle;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class SiegeGuardManager
{
	private static final Log _log = LogFactory.getLog(SiegeGuardManager.class);

	private Castle _castle;
	private List<L2Spawn> _siegeGuardSpawn = new FastList<L2Spawn>();

	public SiegeGuardManager(Castle castle)
	{
		_castle = castle;
	}

	public void addSiegeGuard(L2PcInstance activeChar, int npcId)
	{
		if(activeChar == null)
		{
			return;
		}

		addSiegeGuard(activeChar.getX(), activeChar.getY(), activeChar.getZ(), activeChar.getHeading(), npcId);
	}

	public void addSiegeGuard(int x, int y, int z, int heading, int npcId)
	{
		saveSiegeGuard(x, y, z, heading, npcId, 0);
	}

	public void hireMerc(L2PcInstance activeChar, int npcId)
	{
		if(activeChar == null)
		{
			return;
		}

		hireMerc(activeChar.getX(), activeChar.getY(), activeChar.getZ(), activeChar.getHeading(), npcId);
	}

	public void hireMerc(int x, int y, int z, int heading, int npcId)
	{
		saveSiegeGuard(x, y, z, heading, npcId, 1);
	}

	public void removeMerc(int npcId, int x, int y, int z)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM castle_siege_guards WHERE npcId = ? AND x = ? AND y = ? AND z = ? AND isHired = 1");
			statement.setInt(1, npcId);
			statement.setInt(2, x);
			statement.setInt(3, y);
			statement.setInt(4, z);
			statement.execute();
			statement.close();
			statement = null;
		}
		catch(Exception e1)
		{
			_log.error("Error deleting hired siege guard at " + x + ',' + y + ',' + z, e1);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	public void removeMercs()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM castle_siege_guards WHERE castleId = ? AND isHired = 1");
			statement.setInt(1, getCastle().getCastleId());
			statement.execute();
			statement.close();
			statement = null;
		}
		catch(Exception e1)
		{
			_log.error("Error deleting hired siege guard for castle " + getCastle().getName(), e1);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	public void spawnSiegeGuard()
	{
		try  
		{  
			int     hiredCount  = 0,   
					hiredMax    = MercTicketManager.getInstance().getMaxAllowedMerc(_castle.getCastleId());   
			boolean isHired     = (getCastle().getOwnerId() > 0) ? true : false;  
			{  
				loadSiegeGuard();  
				for (L2Spawn spawn: getSiegeGuardSpawn())  
				{  
					if (spawn != null)   
					{  
						spawn.init();  
						if (isHired)  
						{  
							hiredCount++;  
							if (hiredCount > hiredMax)  
								return;  
						}  
					}  
				}  
			}  
		}  
		catch (Throwable t)  
		{  
			_log.warn("Error spawning siege guards for castle " + getCastle().getName() + ":" + t.toString());}
	}

	public void unspawnSiegeGuard()
	{
		for(L2Spawn spawn : getSiegeGuardSpawn())
		{
			if(spawn == null)
			{
				continue;
			}

			spawn.stopRespawn();
			spawn.getLastSpawn().doDie(spawn.getLastSpawn());
		}

		getSiegeGuardSpawn().clear();
	}

	private void loadSiegeGuard()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM castle_siege_guards WHERE castleId = ? AND isHired = ?");
			statement.setInt(1, getCastle().getCastleId());
			if(getCastle().getOwnerId() > 0)
			{
				statement.setInt(2, 1);
			}
			else
			{
				statement.setInt(2, 0);
			}
			ResultSet rs = statement.executeQuery();

			L2Spawn spawn1;
			L2NpcTemplate template1;

			while(rs.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rs.getInt("npcId"));
				if(template1 != null)
				{
					spawn1 = new L2Spawn(template1);
					spawn1.setId(rs.getInt("id"));
					spawn1.setAmount(1);
					spawn1.setLocx(rs.getInt("x"));
					spawn1.setLocy(rs.getInt("y"));
					spawn1.setLocz(rs.getInt("z"));
					spawn1.setHeading(rs.getInt("heading"));
					spawn1.setRespawnDelay(rs.getInt("respawnDelay"));
					spawn1.setLocation(0);
					_siegeGuardSpawn.add(spawn1);
					spawn1 = null;
				}
				else
				{
					_log.warn("Missing npc data in npc table for id: " + rs.getInt("npcId"));
				}
				template1 = null;
			}
			rs.close();
			rs = null;
			statement.close();
			statement = null;
		}
		catch(Exception e1)
		{
			_log.error("Error loading siege guard for castle " + getCastle().getName(), e1);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	private void saveSiegeGuard(int x, int y, int z, int heading, int npcId, int isHire)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO castle_siege_guards (castleId, npcId, x, y, z, heading, respawnDelay, isHired) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
			statement.setInt(1, getCastle().getCastleId());
			statement.setInt(2, npcId);
			statement.setInt(3, x);
			statement.setInt(4, y);
			statement.setInt(5, z);
			statement.setInt(6, heading);
			if(isHire == 1)
			{
				statement.setInt(7, 0);
			}
			else
			{
				statement.setInt(7, 600);
			}
			statement.setInt(8, isHire);
			statement.execute();
			statement.close();
			statement = null;
		}
		catch(Exception e1)
		{
			_log.error("Error adding siege guard for castle " + getCastle().getName(), e1);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	public final Castle getCastle()
	{
		return _castle;
	}

	public final List<L2Spawn> getSiegeGuardSpawn()
	{
		return _siegeGuardSpawn;
	}
}