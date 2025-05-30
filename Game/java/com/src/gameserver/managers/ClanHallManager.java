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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.entity.ClanHall;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class ClanHallManager
{
	private static final Log _log = LogFactory.getLog(ClanHallManager.class); 
	
	private ConcurrentMap<Integer, ClanHall> _clanHall; 
	private ConcurrentMap<Integer, ClanHall> _freeClanHall; 
	//private L2FastList<L2ClanHallZone> _zones; 
 
	private static ClanHallManager _instance;

	public static ClanHallManager getInstance()
	{
		if(_instance == null)
		{
			_instance = new ClanHallManager();
		}
		return _instance;
	}

	private ClanHallManager()
	{
		_clanHall = new ConcurrentHashMap<Integer, ClanHall>();
		_freeClanHall = new ConcurrentHashMap<Integer, ClanHall>();
		load();
	}

	private final void load()
	{
		Connection con = null;
		try
		{
			int id, ownerId, lease, grade;
			String Name, Desc, Location;
			long paidUntil;
			boolean paid = false;

			PreparedStatement statement;
			ResultSet rs;
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM clanhall ORDER BY id");
			rs = statement.executeQuery();
			while(rs.next())
			{
				id = rs.getInt("id");
				Name = rs.getString("name");
				ownerId = rs.getInt("ownerId");
				lease = rs.getInt("lease");
				Desc = rs.getString("desc");
				Location = rs.getString("location");
				paidUntil = rs.getLong("paidUntil");
				grade = rs.getInt("Grade");
				paid = rs.getBoolean("paid");

				ClanHall ch = new ClanHall(id, Name, ownerId, lease, Desc, Location, paidUntil, grade, paid);
				if(ownerId == 0)
				{
					_freeClanHall.put(id, ch);
				}
				else
				{
					L2Clan clan = ClanTable.getInstance().getClan(ownerId);
					if(clan != null)
					{
						_clanHall.put(id, ch);
						clan.setHasHideout(id);
					}
					else
					{
						_freeClanHall.put(id, ch);
						ch.free();
						AuctionManager.getInstance().initNPC(id);
					}
					clan = null;
				}
				ch = null;
			}
			statement.close();
			statement = null;
			rs.close();
			rs = null;

			_log.info("ClanHallManager: Loaded " + getClanHalls().size() + " used clan halls.");
			_log.info("ClanHallManager: Loaded " + getFreeClanHalls().size() + " free clan halls.");
		}
		catch(Exception e)
		{
			System.out.println("Exception: ClanHallManager.load(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	public final Map<Integer, ClanHall> getFreeClanHalls()
	{
		return _freeClanHall;
	}

	public final Map<Integer, ClanHall> getClanHalls()
	{
		return _clanHall;
	}

	public final boolean isFree(int chId)
	{
		if(_freeClanHall.containsKey(chId))
		{
			return true;
		}

		return false;
	}

	public final synchronized void setFree(int chId)
	{
		_freeClanHall.put(chId, _clanHall.get(chId));
		ClanTable.getInstance().getClan(_freeClanHall.get(chId).getOwnerId()).setHasHideout(0);
		_freeClanHall.get(chId).free();
		_clanHall.remove(chId);
	}

	public final synchronized void setOwner(int chId, L2Clan clan)
	{
		if(!_clanHall.containsKey(chId))
		{
			_clanHall.put(chId, _freeClanHall.get(chId));
			_freeClanHall.remove(chId);
		}
		else
		{
			_clanHall.get(chId).free();
		}

		ClanTable.getInstance().getClan(clan.getClanId()).setHasHideout(chId);
		_clanHall.get(chId).setOwner(clan);
	}

	public final ClanHall getClanHallById(int clanHallId)
	{
		if(_clanHall.containsKey(clanHallId))
		{
			return _clanHall.get(clanHallId);
		}
		if(_freeClanHall.containsKey(clanHallId))
		{
			return _freeClanHall.get(clanHallId);
		}
		return null;
	}

	public final ClanHall getNearbyClanHall(int x, int y, int maxDist)
	{
		for(ClanHall ch : _clanHall.values()) if(ch.getZone().getDistanceToZone(x, y) < maxDist)
			return ch;
		
		for(ClanHall ch : _freeClanHall.values()) if(ch.getZone().getDistanceToZone(x, y) < maxDist)
			return ch;

		return null;
	}

	public final ClanHall getClanHallByOwner(L2Clan clan)
	{
		for(ClanHall ch : _clanHall.values()) if(clan.getClanId() == ch.getOwnerId())
			return ch;

		return null;
	}
}