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
package com.src.gameserver.model.entity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javolution.util.FastList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.datatables.xml.DoorTable;
import com.src.gameserver.managers.AuctionManager;
import com.src.gameserver.managers.ClanHallManager;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.actor.instance.L2DoorInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.zone.type.L2ClanHallZone;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class ClanHall
{
	private static final Log _log = LogFactory.getLog(ClanHall.class);

	private int _clanHallId;
	private List<L2DoorInstance> _doors = new FastList<L2DoorInstance>();;
	private List<String> _doorDefault = new FastList<String>();;
	private String _name;
	private int _ownerId;
	private L2Clan _ownerClan;
	private int _lease;
	private String _desc;
	private String _location;
	protected long _paidUntil;
	private L2ClanHallZone _zone;
	private int _grade;
	protected final int _chRate = 604800000;
	protected boolean _isFree = true;
	private Map<Integer, ClanHallFunction> _functions;
	protected boolean _paid;

	public static final int FUNC_TELEPORT = 1;
	public static final int FUNC_ITEM_CREATE = 2;
	public static final int FUNC_RESTORE_HP = 3;
	public static final int FUNC_RESTORE_MP = 4;
	public static final int FUNC_RESTORE_EXP = 5;
	public static final int FUNC_SUPPORT = 6;
	public static final int FUNC_DECO_FRONTPLATEFORM = 7;
	public static final int FUNC_DECO_CURTAINS = 8;

	public class ClanHallFunction
	{
		private int _type;
		private int _lvl;
		protected int _fee;
		protected int _tempFee;
		private long _rate;
		private long _endDate;
		protected boolean _inDebt;

		public ClanHallFunction(int type, int lvl, int lease, int tempLease, long rate, long time)
		{
			_type = type;
			_lvl = lvl;
			_fee = lease;
			_tempFee = tempLease;
			_rate = rate;
			_endDate = time;
			initializeTask();
		}

		public int getType()
		{
			return _type;
		}

		public int getLvl()
		{
			return _lvl;
		}

		public int getLease()
		{
			return _fee;
		}

		public long getRate()
		{
			return _rate;
		}

		public long getEndTime()
		{
			return _endDate;
		}

		public void setLvl(int lvl)
		{
			_lvl = lvl;
		}

		public void setLease(int lease)
		{
			_fee = lease;
		}

		public void setEndTime(long time)
		{
			_endDate = time;
		}

		private void initializeTask()
		{
			if(_isFree)
			{
				return;
			}

			long currentTime = System.currentTimeMillis();

			if(_endDate > currentTime)
			{
				ThreadPoolManager.getInstance().scheduleGeneral(new FunctionTask(), _endDate - currentTime);
			}
			else
			{
				ThreadPoolManager.getInstance().scheduleGeneral(new FunctionTask(), 0);
			}
		}

		private class FunctionTask implements Runnable
		{
			public FunctionTask()
			{}

			@Override
			public void run()
			{
				try
				{
					if(_isFree)
					{
						return;
					}

					if(getOwnerClan().getWarehouse().getAdena() >= _fee)
					{
						int fee = _fee;
						boolean newfc = true;

						if(getEndTime() == 0 || getEndTime() == -1)
						{
							if(getEndTime() == -1)
							{
								newfc = false;
								fee = _tempFee;
							}
						}
						else
						{
							newfc = false;
						}

						setEndTime(System.currentTimeMillis() + getRate());
						dbSave(newfc);
						getOwnerClan().getWarehouse().destroyItemByItemId("CH_function_fee", 57, fee, null, null);

						ThreadPoolManager.getInstance().scheduleGeneral(new FunctionTask(), getRate());
					}
					else
					{
						removeFunction(getType());
					}
				}
				catch(Throwable t)
				{
					_log.error("", t);
				}
			}
		}

		public void dbSave(boolean newFunction)
		{
			Connection con = null;
			try
			{
				PreparedStatement statement;

				con = L2DatabaseFactory.getInstance().getConnection();
				if(newFunction)
				{
					statement = con.prepareStatement("INSERT INTO clanhall_functions (hall_id, type, lvl, lease, rate, endTime) VALUES (?, ?, ?, ?, ?, ?)");
					statement.setInt(1, getId());
					statement.setInt(2, getType());
					statement.setInt(3, getLvl());
					statement.setInt(4, getLease());
					statement.setLong(5, getRate());
					statement.setLong(6, getEndTime());
				}
				else
				{
					statement = con.prepareStatement("UPDATE clanhall_functions SET lvl = ?, lease = ?, endTime = ? WHERE hall_id = ? AND type = ?");
					statement.setInt(1, getLvl());
					statement.setInt(2, getLease());
					statement.setLong(3, getEndTime());
					statement.setInt(4, getId());
					statement.setInt(5, getType());
				}
				statement.execute();
				ResourceUtil.closeStatement(statement);
			}
			catch(Exception e)
			{
				_log.error("", e);
			}
			finally
			{
				ResourceUtil.closeConnection(con);
			}
		}
	}

	public ClanHall(int clanHallId, String name, int ownerId, int lease, String desc, String location, long paidUntil, int Grade, boolean paid)
	{
		_clanHallId = clanHallId;
		_name = name;
		_ownerId = ownerId;

		_lease = lease;
		_desc = desc;
		_location = location;
		_paidUntil = paidUntil;
		_grade = Grade;
		_paid = paid;
		loadDoor();
		_functions = new ConcurrentHashMap<Integer, ClanHallFunction>();

		if(ownerId != 0)
		{
			_isFree = false;

			initialyzeTask(false);
			loadFunctions();
		}
	}

	public final boolean getPaid()
	{
		return _paid;
	}

	public final int getId()
	{
		return _clanHallId;
	}

	public final String getName()
	{
		return _name;
	}

	public final int getOwnerId()
	{
		return _ownerId;
	}

	public final int getLease()
	{
		return _lease;
	}

	public final String getDesc()
	{
		return _desc;
	}

	public final String getLocation()
	{
		return _location;
	}

	public final long getPaidUntil()
	{
		return _paidUntil;
	}

	public final int getGrade()
	{
		return _grade;
	}

	public final List<L2DoorInstance> getDoors()
	{
		return _doors;
	}

	public final L2DoorInstance getDoor(int doorId)
	{
		if(doorId <= 0)
		{
			return null;
		}

		for(int i = 0; i < getDoors().size(); i++)
		{
			L2DoorInstance door = getDoors().get(i);

			if(door.getDoorId() == doorId)
			{
				return door;
			}

			door = null;
		}

		return null;
	}

	public ClanHallFunction getFunction(int type)
	{
		if(_functions.get(type) != null)
		{
			return _functions.get(type);
		}

		return null;
	}

	public void setZone(L2ClanHallZone zone)
	{
		_zone = zone;
	}

	public L2ClanHallZone getZone()
	{
		return _zone;
	}

	public void free()
	{
		_ownerId = 0;
		_isFree = true;

		for(Map.Entry<Integer, ClanHallFunction> fc : _functions.entrySet())
		{
			removeFunction(fc.getKey());
		}

		_functions.clear();
		_paidUntil = 0;
		_paid = false;
		updateDb();
	}

	public void setOwner(L2Clan clan)
	{
		if(_ownerId > 0 || clan == null)
		{
			return;
		}

		_ownerId = clan.getClanId();
		_isFree = false;
		_paidUntil = System.currentTimeMillis();
		initialyzeTask(true);

		clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
		updateDb();
	}

	public L2Clan getOwnerClan()
	{
		if(_ownerId == 0)
		{
			return null;
		}

		if(_ownerClan == null)
		{
			_ownerClan = ClanTable.getInstance().getClan(getOwnerId());
		}

		return _ownerClan;
	}

	public void spawnDoor()
	{
		for(int i = 0; i < getDoors().size(); i++)
		{
			L2DoorInstance door = getDoors().get(i);

			if(door.getCurrentHp() <= 0)
			{
				door.decayMe();
				door = DoorTable.parseList(_doorDefault.get(i));
				DoorTable.getInstance().putDoor(door);
				door.spawnMe(door.getX(), door.getY(), door.getZ());
				getDoors().set(i, door);
			}
			else if(door.getOpen())
			{
				door.closeMe();
			}

			door.setCurrentHp(door.getMaxHp());

			door = null;
		}
	}

	public void openCloseDoor(L2PcInstance activeChar, int doorId, boolean open)
	{
		if(activeChar != null && activeChar.getClanId() == getOwnerId())
		{
			openCloseDoor(doorId, open);
		}
	}

	public void openCloseDoor(int doorId, boolean open)
	{
		openCloseDoor(getDoor(doorId), open);
	}

	public void openCloseDoor(L2DoorInstance door, boolean open)
	{
		if(door != null)
		{
			if(open)
			{
				door.openMe();
			}
			else
			{
				door.closeMe();
			}
		}
	}

	public void openCloseDoors(L2PcInstance activeChar, boolean open)
	{
		if(activeChar != null && activeChar.getClanId() == getOwnerId())
		{
			openCloseDoors(open);
		}
	}

	public void openCloseDoors(boolean open)
	{
		for(L2DoorInstance door : getDoors())
		{
			if(door != null)
			{
				if(open)
				{
					door.openMe();
				}
				else
				{
					door.closeMe();
				}
			}
		}
	}

	public void banishForeigners()
	{
		_zone.banishForeigners(getOwnerId());
	}

	private void loadFunctions()
	{
		Connection con = null;
		try
		{
			PreparedStatement statement;
			ResultSet rs;
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM clanhall_functions WHERE hall_id = ?");
			statement.setInt(1, getId());
			rs = statement.executeQuery();

			while(rs.next())
			{
				_functions.put(rs.getInt("type"), new ClanHallFunction(rs.getInt("type"), rs.getInt("lvl"), rs.getInt("lease"), 0, rs.getLong("rate"), rs.getLong("endTime")));
			}

			rs.close();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public void removeFunction(int functionType)
	{
		_functions.remove(functionType);
		Connection con = null;
		try
		{
			PreparedStatement statement;
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM clanhall_functions WHERE hall_id = ? AND type = ?");
			statement.setInt(1, getId());
			statement.setInt(2, functionType);
			statement.execute();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public boolean updateFunctions(int type, int lvl, int lease, long rate, boolean addNew)
	{
		if(addNew)
		{
			if(ClanTable.getInstance().getClan(getOwnerId()).getWarehouse().getAdena() < lease)
			{
				return false;
			}
			_functions.put(type, new ClanHallFunction(type, lvl, lease, 0, rate, 0));
		}
		else
		{
			if(lvl == 0 && lease == 0)
			{
				removeFunction(type);
			}
			else
			{
				int diffLease = lease - _functions.get(type).getLease();

				if(diffLease > 0)
				{
					if(ClanTable.getInstance().getClan(_ownerId).getWarehouse().getAdena() < diffLease)
					{
						return false;
					}

					_functions.remove(type);
					_functions.put(type, new ClanHallFunction(type, lvl, lease, diffLease, rate, -1));
				}
				else
				{
					_functions.get(type).setLease(lease);
					_functions.get(type).setLvl(lvl);
					_functions.get(type).dbSave(false);
				}
			}
		}
		return true;
	}

	public void updateDb()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;

			statement = con.prepareStatement("UPDATE clanhall SET ownerId = ?, paidUntil = ?, paid = ? WHERE id = ?");
			statement.setInt(1, _ownerId);
			statement.setLong(2, _paidUntil);
			statement.setInt(3, _paid ? 1 : 0);
			statement.setInt(4, _clanHallId);
			statement.execute();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	private void initialyzeTask(boolean forced)
	{
		long currentTime = System.currentTimeMillis();

		if(_paidUntil > currentTime)
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), _paidUntil - currentTime);
		}
		else if(!_paid && !forced)
		{
			if(System.currentTimeMillis() + 1000 * 60 * 60 * 24 <= _paidUntil + _chRate)
			{
				ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), System.currentTimeMillis() + 1000 * 60 * 60 * 24);
			}
			else
			{
				ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), _paidUntil + _chRate - System.currentTimeMillis());
			}
		}
		else
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), 3000);
		}
	}

	private class FeeTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				if(_isFree)
				{
					return;
				}

				L2Clan Clan = ClanTable.getInstance().getClan(getOwnerId());

				if(ClanTable.getInstance().getClan(getOwnerId()).getWarehouse().getAdena() >= getLease())
				{
					if(_paidUntil != 0)
					{
						while(_paidUntil <= System.currentTimeMillis())
						{
							_paidUntil += _chRate;
						}
					}
					else
					{
						_paidUntil = System.currentTimeMillis() + _chRate;
					}

					ClanTable.getInstance().getClan(getOwnerId()).getWarehouse().destroyItemByItemId("CH_rental_fee", 57, getLease(), null, null);

					ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), _paidUntil - System.currentTimeMillis());
					_paid = true;
					updateDb();
				}
				else
				{
					_paid = false;
					if(System.currentTimeMillis() > _paidUntil + _chRate)
					{
						AuctionManager.getInstance().initNPC(getId()); 
						ClanHallManager.getInstance().setFree(getId());
						Clan.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.THE_CLAN_HALL_FEE_IS_ONE_WEEK_OVERDUE_THEREFORE_THE_CLAN_HALL_OWNERSHIP_HAS_BEEN_REVOKED));
					}
					else
					{
						updateDb();
						Clan.broadcastToOnlineMembers(new SystemMessage(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW).addNumber(getLease()));

						if(System.currentTimeMillis() + 1000 * 60 * 60 * 24 <= _paidUntil + _chRate)
						{
							ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), System.currentTimeMillis() + 1000 * 60 * 60 * 24);
						}
						else
						{
							ThreadPoolManager.getInstance().scheduleGeneral(new FeeTask(), _paidUntil + _chRate - System.currentTimeMillis());
						}

					}
				}
			}
			catch(Exception t)
			{
				_log.error("", t);
			}
		}
	}

	private void loadDoor()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM castle_door WHERE castleId = ?");
			statement.setInt(1, getId());
			ResultSet rs = statement.executeQuery();

			while(rs.next())
			{
				_doorDefault.add(rs.getString("name") + ";" + rs.getInt("id") + ";" + rs.getInt("x") + ";" + rs.getInt("y") + ";" + rs.getInt("z") + ";" + rs.getInt("range_xmin") + ";" + rs.getInt("range_ymin") + ";" + rs.getInt("range_zmin") + ";" + rs.getInt("range_xmax") + ";" + rs.getInt("range_ymax") + ";" + rs.getInt("range_zmax") + ";" + rs.getInt("hp") + ";" + rs.getInt("pDef") + ";" + rs.getInt("mDef"));

				L2DoorInstance door = DoorTable.parseList(_doorDefault.get(_doorDefault.size() - 1));
				door.spawnMe(door.getX(), door.getY(), door.getZ());
				_doors.add(door);
				DoorTable.getInstance().putDoor(door);
			}

			rs.close();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}
}