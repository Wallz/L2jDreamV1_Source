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
package com.src.gameserver.model.entity.siege;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.datatables.xml.DoorTable;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.managers.CastleManorManager;
import com.src.gameserver.managers.CastleManorManager.CropProcure;
import com.src.gameserver.managers.CastleManorManager.SeedProduction;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.L2Manor;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.instance.L2DoorInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.sevensigns.SevenSigns;
import com.src.gameserver.model.zone.type.L2CastleTeleportZone;
import com.src.gameserver.model.zone.type.L2CastleZone;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.PlaySound;
import com.src.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.gameserver.updaters.CastleUpdater;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class Castle
{
	private final static Log _log = LogFactory.getLog(Castle.class);

	private FastList<CropProcure> _procure = new FastList<CropProcure>();
	private FastList<SeedProduction> _production = new FastList<SeedProduction>();
	private FastList<CropProcure> _procureNext = new FastList<CropProcure>();
	private FastList<SeedProduction> _productionNext = new FastList<SeedProduction>();
	private boolean _isNextPeriodApproved = false;

	private static final String CASTLE_MANOR_DELETE_PRODUCTION = "DELETE FROM castle_manor_production WHERE castle_id=?;";

	private static final String CASTLE_MANOR_DELETE_PRODUCTION_PERIOD = "DELETE FROM castle_manor_production WHERE castle_id=? AND period=?;";

	private static final String CASTLE_MANOR_DELETE_PROCURE = "DELETE FROM castle_manor_procure WHERE castle_id=?;";

	private static final String CASTLE_MANOR_DELETE_PROCURE_PERIOD = "DELETE FROM castle_manor_procure WHERE castle_id=? AND period=?;";

	private static final String CASTLE_UPDATE_CROP = "UPDATE castle_manor_procure SET can_buy=? WHERE crop_id=? AND castle_id=? AND period=?";

	private static final String CASTLE_UPDATE_SEED = "UPDATE castle_manor_production SET can_produce=? WHERE seed_id=? AND castle_id=? AND period=?";

	private int _castleId = 0;
	private List<L2DoorInstance> _doors = new FastList<L2DoorInstance>();
	private final Map<Integer, Integer> _doorUpgrades = new FastMap<>();
	private List<String> _doorDefault = new FastList<String>();
	private String _name = "";
	private int _ownerId = 0;
	private Siege _siege = null;
	private Calendar _siegeDate;
	private boolean _isTimeRegistrationOver = true;
	private Calendar _siegeTimeRegistrationEndDate;
	private int _taxPercent = 0;
	private double _taxRate = 0;
	private int _treasury = 0;
	private L2CastleZone _zone;
	private L2CastleTeleportZone _teleZone;
	private L2Clan _formerOwner = null;
	private int _nbArtifact = 1;
	private final int[] _gate =
	{
			Integer.MIN_VALUE, 0, 0
	};
	private Map<Integer, Integer> _engrave = new FastMap<Integer, Integer>();


	public Castle(int castleId)
	{
		_castleId = castleId;

		if(_castleId == 7 || castleId == 9)
		{
			_nbArtifact = 2;
		}
		load();
		loadDoor();
	}

	public void Engrave(L2Clan clan, int objId)
	{
		_engrave.put(objId, clan.getClanId());

		if(_engrave.size() == _nbArtifact)
		{
			boolean rst = true;

			for(int id : _engrave.values())
			{
				if(id != clan.getClanId())
				{
					rst = false;
				}
			}

			if(rst)
			{
				_engrave.clear();
				setOwner(clan);
			}
			else
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.CLAN_S1_ENGRAVED_RULER);
				sm.addString(clan.getName());
				getSiege().announceToPlayer(sm, true);
			}
		}
		else
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.CLAN_S1_ENGRAVED_RULER);
			sm.addString(clan.getName());
			getSiege().announceToPlayer(sm, true);
		}
	}

	public void addToTreasury(int amount)
	{
		if(getOwnerId() <= 0)
		{
			return;
		}

		if(_name.equalsIgnoreCase("Schuttgart") || _name.equalsIgnoreCase("Goddard"))
		{
			Castle rune = CastleManager.getInstance().getCastle("rune");
			if(rune != null)
			{
				int runeTax = (int) (amount * rune.getTaxRate());

				if(rune.getOwnerId() > 0)
				{
					rune.addToTreasury(runeTax);
				}

				amount -= runeTax;
			}
		}

		if(!_name.equalsIgnoreCase("aden") && !_name.equalsIgnoreCase("Rune") && !_name.equalsIgnoreCase("Schuttgart") && !_name.equalsIgnoreCase("Goddard"))
		{
			Castle aden = CastleManager.getInstance().getCastle("aden");

			if(aden != null)
			{
				int adenTax = (int) (amount * aden.getTaxRate());

				if(aden.getOwnerId() > 0)
				{
					aden.addToTreasury(adenTax);
				}

				amount -= adenTax;
			}
		}

		addToTreasuryNoTax(amount);
	}

	public boolean addToTreasuryNoTax(int amount)
	{
		if(getOwnerId() <= 0)
		{
			return false;
		}

		if(amount < 0)
		{
			amount *= -1;

			if(_treasury < amount)
			{
				return false;
			}

			_treasury -= amount;
		}
		else
		{
			if((long) _treasury + amount > Integer.MAX_VALUE)
			{
				_treasury = Integer.MAX_VALUE;
			}
			else
			{
				_treasury += amount;
			}
		}

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE castle SET treasury = ? WHERE id = ?");
			statement.setInt(1, getTreasury());
			statement.setInt(2, getCastleId());
			statement.execute();
			statement.close();
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
		return true;
	}

	public void banishForeigners()
	{
		_zone.banishForeigners(getOwnerId());
	}

	public boolean checkIfInZone(int x, int y, int z)
	{
		return _zone.isInsideZone(x, y, z);
	}

	public void setZone(L2CastleZone zone)
	{
		_zone = zone;
	}

	public L2CastleZone getZone()
	{
		return _zone;
	}

	public void setTeleZone(L2CastleTeleportZone zone)
	{
		_teleZone = zone;
	}

	public L2CastleTeleportZone getTeleZone()
	{
		return _teleZone;
	}

	public double getDistance(L2Object obj)
	{
		return _zone.getDistanceToZone(obj);
	}

	public void closeDoor(L2PcInstance activeChar, int doorId)
	{
		openCloseDoor(activeChar, doorId, false);
	}

	public void openDoor(L2PcInstance activeChar, int doorId)
	{
		openCloseDoor(activeChar, doorId, true);
	}

	public void openCloseDoor(L2PcInstance activeChar, int doorId, boolean open)
	{
		if(activeChar.getClanId() != getOwnerId())
		{
			return;
		}

		L2DoorInstance door = getDoor(doorId);
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

	public void removeUpgrade()
	{
		removeDoorUpgrade();
	}

	public void setOwner(L2Clan clan)
	{
		if(getOwnerId() > 0 && (clan == null || clan.getClanId() != getOwnerId()))
		{
			L2Clan oldOwner = ClanTable.getInstance().getClan(getOwnerId());

			if(oldOwner != null)
			{
				if(_formerOwner == null)
				{
					_formerOwner = oldOwner;
					if(Config.REMOVE_CASTLE_CIRCLETS)
					{
						CastleManager.getInstance().removeCirclet(_formerOwner, getCastleId());
					}
					L2PcInstance oldLeader = oldOwner.getLeader().getPlayerInstance();
					if (oldLeader != null)
					{
						if (oldLeader.getMountType() == 2)
							oldLeader.dismount();
					}
				}
				oldOwner.setHasCastle(0);
			}

			oldOwner = null;
		}

		updateOwnerInDB(clan);
		
		if(getSiege().getIsInProgress())
		{
			getSiege().midVictory();
			
			getSiege().announceToPlayer(SystemMessage.getSystemMessage(SystemMessageId.NEW_CASTLE_LORD), true);
		}

		updateClansReputation();
	}

	public void removeOwner(L2Clan clan)
	{
		if(clan != null)
		{
			_formerOwner = clan;

			if(Config.REMOVE_CASTLE_CIRCLETS)
			{
				CastleManager.getInstance().removeCirclet(_formerOwner, getCastleId());
			}

			clan.setHasCastle(0);
			clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
		}

		updateOwnerInDB(null);

		if(getSiege().getIsInProgress())
		{
			getSiege().midVictory();
		}

		updateClansReputation();
	}

	public void setTaxPercent(L2PcInstance activeChar, int taxPercent)
	{
		int maxTax;

		switch(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
		{
			case SevenSigns.CABAL_DAWN:
				maxTax = 25;
				break;
			case SevenSigns.CABAL_DUSK:
				maxTax = 5;
				break;
			default:
				maxTax = 15;
		}

		if(taxPercent < 0 || taxPercent > maxTax)
		{
			activeChar.sendMessage("Tax value must be between 0 and " + maxTax + ".");
			return;
		}

		setTaxPercent(taxPercent);
		activeChar.sendMessage(getName() + " castle tax changed to " + taxPercent + "%.");
	}

	public void setTaxPercent(int taxPercent)
	{
		_taxPercent = taxPercent;
		_taxRate = _taxPercent / 100.0;

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE castle SET taxPercent = ? WHERE id = ?");
			statement.setInt(1, taxPercent);
			statement.setInt(2, getCastleId());
			statement.execute();
			statement.close();
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

	public void spawnDoor()
	{
		spawnDoor(false);
	}

	public void spawnDoor(boolean isDoorWeak)
	{
		for(int i = 0; i < getDoors().size(); i++)
		{
			L2DoorInstance door = getDoors().get(i);
			if(door.getCurrentHp() <= 0)
			{
				door.decayMe();
				door = DoorTable.parseList(_doorDefault.get(i));

				if(isDoorWeak)
				{
					door.setCurrentHp(door.getMaxHp() / 2);
				}
				door.spawnMe(door.getX(), door.getY(), door.getZ());
				getDoors().set(i, door);
			}
			else if(door.getOpen())
			{
				door.closeMe();
			}
		}
		loadDoorUpgrade();
	}

	public void upgradeDoor(int doorId, int hp, boolean db)
	{
		L2DoorInstance door = getDoor(doorId);
		if (door == null)
			return;
		
		door.setUpgradeHpRatio(hp);
		door.setCurrentHp(door.getMaxHp());
		
		if (db)
			saveDoorUpgrade(doorId, hp);
		
		_doorUpgrades.put(doorId, hp);
	}

	public Integer getDoorUpgrade(int doorId)
	{
		if (_doorUpgrades.containsKey(doorId))
			return _doorUpgrades.get(doorId);
		
		return 1;
	}
	
	private void load()
	{
		Connection con = null;
		try
		{
			PreparedStatement statement;
			ResultSet rs;

			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("SELECT * FROM castle WHERE id = ?");
			statement.setInt(1, getCastleId());
			rs = statement.executeQuery();

			while(rs.next())
			{
				_name = rs.getString("name");

				_siegeDate = Calendar.getInstance();
				_siegeDate.setTimeInMillis(rs.getLong("siegeDate"));
				_siegeTimeRegistrationEndDate = Calendar.getInstance();
				_siegeTimeRegistrationEndDate.setTimeInMillis(rs.getLong("regTimeEnd"));
				_isTimeRegistrationOver = rs.getBoolean("regTimeOver");

				_taxPercent = rs.getInt("taxPercent");
				_treasury = rs.getInt("treasury");
			}

			rs.close();
			statement.close();
			statement = null;
			rs = null;

			_taxRate = _taxPercent / 100.0;

			statement = con.prepareStatement("SELECT clan_id FROM clan_data WHERE hasCastle = ?");
			statement.setInt(1, getCastleId());
			rs = statement.executeQuery();

			while(rs.next())
			{
				_ownerId = rs.getInt("clan_id");
			}

			if(getOwnerId() > 0)
			{
				L2Clan clan = ClanTable.getInstance().getClan(getOwnerId());
				ThreadPoolManager.getInstance().scheduleGeneral(new CastleUpdater(clan, 1), 3600000);
				clan = null;
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
	}

	private void loadDoor()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM castle_door WHERE castleId = ?");
			statement.setInt(1, getCastleId());
			ResultSet rs = statement.executeQuery();

			while(rs.next())
			{
				_doorDefault.add(rs.getString("name") + ";" + rs.getInt("id") + ";" + rs.getInt("x") + ";" + rs.getInt("y") + ";" + rs.getInt("z") + ";" + rs.getInt("range_xmin") + ";" + rs.getInt("range_ymin") + ";" + rs.getInt("range_zmin") + ";" + rs.getInt("range_xmax") + ";" + rs.getInt("range_ymax") + ";" + rs.getInt("range_zmax") + ";" + rs.getInt("hp") + ";" + rs.getInt("pDef") + ";" + rs.getInt("mDef"));

				L2DoorInstance door = DoorTable.parseList(_doorDefault.get(_doorDefault.size() - 1));
				door.spawnMe(door.getX(), door.getY(), door.getZ());
				_doors.add(door);
				DoorTable.getInstance().putDoor(door);

				door = null;
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
	}

	public void loadDoorUpgrade()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			StringBuilder sb = new StringBuilder(200);
			for (L2DoorInstance door : _doors)
				sb.append(door.getDoorId()).append(',');
			sb.deleteCharAt(sb.length() - 1);
			
			PreparedStatement statement = con.prepareStatement("SELECT * FROM castle_doorupgrade WHERE doorId IN (" + sb.toString() + ")");
			ResultSet rs = statement.executeQuery();
			
			while (rs.next())
				upgradeDoor(rs.getInt("doorId"), rs.getInt("hp"), false);
			
			rs.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.error(Level.WARNING);
		}
	}

	private void removeDoorUpgrade()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM castle_doorupgrade WHERE doorId IN (SELECT id FROM castle_door WHERE castleId = ?)");
			statement.setInt(1, getCastleId());
			statement.execute();
			statement.close();
			statement = null;
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

	private static void saveDoorUpgrade(int doorId, int hp)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("REPLACE INTO castle_doorupgrade (doorId, hp) VALUES (?,?)");
			statement.setInt(1, doorId);
			statement.setInt(2, hp);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.error(Level.WARNING, e);
		}
	}

	private void updateOwnerInDB(L2Clan clan)
	{
		if(clan != null)
		{
			_ownerId = clan.getClanId();
		}
		else
		{
			_ownerId = 0;
			resetManor();
		}

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;

			statement = con.prepareStatement("UPDATE clan_data SET hasCastle = 0 WHERE hasCastle = ?");
			statement.setInt(1, getCastleId());
			statement.execute();
			statement.close();
			statement = null;

			statement = con.prepareStatement("UPDATE clan_data SET hasCastle = ? WHERE clan_id = ?");
			statement.setInt(1, getCastleId());
			statement.setInt(2, getOwnerId());
			statement.execute();
			statement.close();
			statement = null;

			if(clan != null)
			{
				clan.setHasCastle(_castleId); // Set has castle flag for new owner
				clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
				clan.broadcastToOnlineMembers(new PlaySound(1, "Siege_Victory", 0, 0, 0, 0, 0));
				ThreadPoolManager.getInstance().scheduleGeneral(new CastleUpdater(clan, 1), 3600000);
			}
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

	public final int getCastleId()
	{
		return _castleId;
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
		}
		return null;
	}

	public final List<L2DoorInstance> getDoors()
	{
		return _doors;
	}

	public final String getName()
	{
		return _name;
	}

	public final int getOwnerId()
	{
		return _ownerId;
	}

	public final Siege getSiege()
	{
		if(_siege == null)
		{
			_siege = new Siege(new Castle[]
			{
				this
			});
		}
		return _siege;
	}

	public final Calendar getSiegeDate()
	{
		return _siegeDate;
	}

	public boolean getIsTimeRegistrationOver()
	{
		return _isTimeRegistrationOver;
	}
	
	public void setIsTimeRegistrationOver(boolean val)
	{
		_isTimeRegistrationOver = val;
	}
	
	public Calendar getTimeRegistrationOverDate()
	{
		if (_siegeTimeRegistrationEndDate == null)
			_siegeTimeRegistrationEndDate = Calendar.getInstance();
		return _siegeTimeRegistrationEndDate;
	}

	public final int getTaxPercent()
	{
		return _taxPercent;
	}

	public final double getTaxRate()
	{
		return _taxRate;
	}

	public final int getTreasury()
	{
		return _treasury;
	}

	public FastList<SeedProduction> getSeedProduction(int period)
	{
		return period == CastleManorManager.PERIOD_CURRENT ? _production : _productionNext;
	}

	public FastList<CropProcure> getCropProcure(int period)
	{
		return period == CastleManorManager.PERIOD_CURRENT ? _procure : _procureNext;
	}

	public void setSeedProduction(FastList<SeedProduction> seed, int period)
	{
		if(period == CastleManorManager.PERIOD_CURRENT)
		{
			_production = seed;
		}
		else
		{
			_productionNext = seed;
		}
	}

	public void setCropProcure(FastList<CropProcure> crop, int period)
	{
		if(period == CastleManorManager.PERIOD_CURRENT)
		{
			_procure = crop;
		}
		else
		{
			_procureNext = crop;
		}
	}

	public synchronized SeedProduction getSeed(int seedId, int period)
	{
		for(SeedProduction seed : getSeedProduction(period))
		{
			if(seed.getId() == seedId)
			{
				return seed;
			}
		}
		return null;
	}

	public synchronized CropProcure getCrop(int cropId, int period)
	{
		for(CropProcure crop : getCropProcure(period))
		{
			if(crop.getId() == cropId)
			{
				return crop;
			}
		}
		return null;
	}

	public int getManorCost(int period)
	{
		FastList<CropProcure> procure;
		FastList<SeedProduction> production;

		if(period == CastleManorManager.PERIOD_CURRENT)
		{
			procure = _procure;
			production = _production;
		}
		else
		{
			procure = _procureNext;
			production = _productionNext;
		}

		int total = 0;

		if(production != null)
		{
			for(SeedProduction seed : production)
			{
				total += L2Manor.getInstance().getSeedBuyPrice(seed.getId()) * seed.getStartProduce();
			}
		}

		if(procure != null)
		{
			for(CropProcure crop : procure)
			{
				total += crop.getPrice() * crop.getStartAmount();
			}
		}
		return total;
	}

	public void saveSeedData()
	{
		Connection con = null;
		PreparedStatement statement;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement(CASTLE_MANOR_DELETE_PRODUCTION);
			statement.setInt(1, getCastleId());

			statement.execute();
			statement.close();
			statement = null;

			if(_production != null)
			{
				int count = 0;

				String query = "INSERT INTO castle_manor_production VALUES ";
				String values[] = new String[_production.size()];

				for(SeedProduction s : _production)
				{
					values[count] = "(" + getCastleId() + "," + s.getId() + "," + s.getCanProduce() + "," + s.getStartProduce() + "," + s.getPrice() + "," + CastleManorManager.PERIOD_CURRENT + ")";
					count++;
				}

				if(values.length > 0)
				{
					query += values[0];
					for(int i = 1; i < values.length; i++)
					{
						query += "," + values[i];
					}
					statement = con.prepareStatement(query);
					statement.execute();
					statement.close();
					statement = null;
				}
			}

			if(_productionNext != null)
			{
				int count = 0;

				String query = "INSERT INTO castle_manor_production VALUES ";
				String values[] = new String[_productionNext.size()];

				for(SeedProduction s : _productionNext)
				{
					values[count] = "(" + getCastleId() + "," + s.getId() + "," + s.getCanProduce() + "," + s.getStartProduce() + "," + s.getPrice() + "," + CastleManorManager.PERIOD_NEXT + ")";
					count++;
				}

				if(values.length > 0)
				{
					query += values[0];

					for(int i = 1; i < values.length; i++)
					{
						query += "," + values[i];
					}

					statement = con.prepareStatement(query);
					statement.execute();
					statement.close();
					statement = null;
				}
			}
		}
		catch(Exception e)
		{
			_log.error("Error adding seed production data for castle " + getName(), e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public void saveSeedData(int period)
	{
		Connection con = null;
		PreparedStatement statement;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement(CASTLE_MANOR_DELETE_PRODUCTION_PERIOD);
			statement.setInt(1, getCastleId());
			statement.setInt(2, period);
			statement.execute();
			statement.close();
			statement = null;

			FastList<SeedProduction> prod = null;
			prod = getSeedProduction(period);

			if(prod != null)
			{
				int count = 0;

				String query = "INSERT INTO castle_manor_production VALUES ";
				String values[] = new String[prod.size()];

				for(SeedProduction s : prod)
				{
					values[count] = "(" + getCastleId() + "," + s.getId() + "," + s.getCanProduce() + "," + s.getStartProduce() + "," + s.getPrice() + "," + period + ")";
					count++;
				}
				if(values.length > 0)
				{
					query += values[0];
					for(int i = 1; i < values.length; i++)
					{
						query += "," + values[i];
					}
					statement = con.prepareStatement(query);
					statement.execute();
					statement.close();
					statement = null;
				}
			}
		}
		catch(Exception e)
		{
			_log.error("Error adding seed production data for castle " + getName(), e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public void saveCropData()
	{
		Connection con = null;
		PreparedStatement statement;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement(CASTLE_MANOR_DELETE_PROCURE);
			statement.setInt(1, getCastleId());
			statement.execute();
			statement.close();
			statement = null;

			if(_procure != null)
			{
				int count = 0;

				String query = "INSERT INTO castle_manor_procure VALUES ";
				String values[] = new String[_procure.size()];

				for(CropProcure cp : _procure)
				{
					values[count] = "(" + getCastleId() + "," + cp.getId() + "," + cp.getAmount() + "," + cp.getStartAmount() + "," + cp.getPrice() + "," + cp.getReward() + "," + CastleManorManager.PERIOD_CURRENT + ")";
					count++;
				}

				if(values.length > 0)
				{
					query += values[0];

					for(int i = 1; i < values.length; i++)
					{
						query += "," + values[i];
					}

					statement = con.prepareStatement(query);
					statement.execute();
					statement.close();
					statement = null;
				}
			}

			if(_procureNext != null)
			{
				int count = 0;

				String query = "INSERT INTO castle_manor_procure VALUES ";
				String values[] = new String[_procureNext.size()];

				for(CropProcure cp : _procureNext)
				{
					values[count] = "(" + getCastleId() + "," + cp.getId() + "," + cp.getAmount() + "," + cp.getStartAmount() + "," + cp.getPrice() + "," + cp.getReward() + "," + CastleManorManager.PERIOD_NEXT + ")";
					count++;
				}

				if(values.length > 0)
				{
					query += values[0];
					for(int i = 1; i < values.length; i++)
					{
						query += "," + values[i];
					}
					statement = con.prepareStatement(query);
					statement.execute();
					statement.close();
				}
			}
		}
		catch(Exception e)
		{
			_log.error("Error adding crop data for castle " + getName(), e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public void saveCropData(int period)
	{
		Connection con = null;
		PreparedStatement statement;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement(CASTLE_MANOR_DELETE_PROCURE_PERIOD);
			statement.setInt(1, getCastleId());
			statement.setInt(2, period);
			statement.execute();
			statement.close();
			statement = null;

			FastList<CropProcure> proc = null;
			proc = getCropProcure(period);

			if(proc != null)
			{
				int count = 0;

				String query = "INSERT INTO castle_manor_procure VALUES ";
				String values[] = new String[proc.size()];

				for(CropProcure cp : proc)
				{
					values[count] = "(" + getCastleId() + "," + cp.getId() + "," + cp.getAmount() + "," + cp.getStartAmount() + "," + cp.getPrice() + "," + cp.getReward() + "," + period + ")";
					count++;
				}

				if(values.length > 0)
				{
					query += values[0];

					for(int i = 1; i < values.length; i++)
					{
						query += "," + values[i];
					}

					statement = con.prepareStatement(query);
					statement.execute();
					statement.close();
					statement = null;
				}
			}
		}
		catch(Exception e)
		{
			_log.error("Error adding crop data for castle " + getName(), e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public void updateCrop(int cropId, int amount, int period)
	{
		Connection con = null;
		PreparedStatement statement;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement(CASTLE_UPDATE_CROP);
			statement.setInt(1, amount);
			statement.setInt(2, cropId);
			statement.setInt(3, getCastleId());
			statement.setInt(4, period);
			statement.execute();
			statement.close();
			statement = null;
		}
		catch(Exception e)
		{
			_log.error("Error adding crop data for castle " + getName(), e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public void updateSeed(int seedId, int amount, int period)
	{
		Connection con = null;
		PreparedStatement statement;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement(CASTLE_UPDATE_SEED);
			statement.setInt(1, amount);
			statement.setInt(2, seedId);
			statement.setInt(3, getCastleId());
			statement.setInt(4, period);
			statement.execute();
			statement.close();
			statement = null;
		}
		catch(Exception e)
		{
			_log.error("Error adding seed production data for castle " + getName(), e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public boolean isNextPeriodApproved()
	{
		return _isNextPeriodApproved;
	}

	public void setNextPeriodApproved(boolean val)
	{
		_isNextPeriodApproved = val;
	}

	public void updateClansReputation()
	{
		SystemMessage msg;
		
		if (_formerOwner != null)
		{
			if (_formerOwner != ClanTable.getInstance().getClan(getOwnerId()))
			{
				int maxreward = Math.max(0, _formerOwner.getReputationScore());
				_formerOwner.setReputationScore(_formerOwner.getReputationScore() - 1000, true);
				
				// Defenders fail
				msg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_WAS_DEFEATED_IN_SIEGE_AND_LOST_S1_REPUTATION_POINTS);
				msg.addNumber(1000);
				_formerOwner.broadcastToOnlineMembers(msg);
				
				L2Clan owner = ClanTable.getInstance().getClan(getOwnerId());
				if (owner != null)
				{
					owner.setReputationScore(owner.getReputationScore() + Math.min(1000, maxreward), true);
					
					// Attackers succeed over defenders
					msg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_VICTORIOUS_IN_SIEGE_AND_GAINED_S1_REPUTATION_POINTS);
					msg.addNumber(1000);
					owner.broadcastToOnlineMembers(msg);
				}
			}
			else
			{
				_formerOwner.setReputationScore(_formerOwner.getReputationScore() + 500, true); 
				
				// Draw
				msg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_VICTORIOUS_IN_SIEGE_AND_GAINED_S1_REPUTATION_POINTS);
				msg.addNumber(500);
				_formerOwner.broadcastToOnlineMembers(msg);
			}
		}
		else
		{
			L2Clan owner = ClanTable.getInstance().getClan(getOwnerId());
			if (owner != null)
			{
				owner.setReputationScore(owner.getReputationScore() + 1000, true);
				
				// Attackers win over NPCs
				msg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_VICTORIOUS_IN_SIEGE_AND_GAINED_S1_REPUTATION_POINTS);
				msg.addNumber(1000);
				owner.broadcastToOnlineMembers(msg);
			}
		}
	}

	public void createClanGate(int x, int y, int z)
	{
		_gate[0] = x;
		_gate[1] = y;
		_gate[2] = z;
	}

	public void destroyClanGate()
	{
		_gate[0] = Integer.MIN_VALUE;
	}

	public boolean isGateOpen()
	{
		return _gate[0] != Integer.MIN_VALUE;
	}

	public int getGateX()
	{
		return _gate[0];
	}

	public int getGateY()
	{
		return _gate[1];
	}

	public int getGateZ()
	{
		return _gate[2];
	}

	public void resetManor()
	{
		setCropProcure(new FastList<CropProcure>(), CastleManorManager.PERIOD_CURRENT);
		setCropProcure(new FastList<CropProcure>(), CastleManorManager.PERIOD_NEXT);
		setSeedProduction(new FastList<SeedProduction>(), CastleManorManager.PERIOD_CURRENT);
		setSeedProduction(new FastList<SeedProduction>(), CastleManorManager.PERIOD_NEXT);
		if(Config.ALT_MANOR_SAVE_ALL_ACTIONS)
		{
			saveCropData();
			saveSeedData();
		}
	}

	public L2Clan getInitialCastleOwner()
	{
		return _formerOwner;
	}
	
	public void oustAllPlayers()
	{
		getTeleZone().oustAllPlayers();
	}

}