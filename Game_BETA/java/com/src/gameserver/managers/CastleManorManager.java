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
import java.util.Calendar;

import javolution.util.FastList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.model.ClanWarehouse;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.L2Manor;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.siege.Castle;
import com.src.gameserver.model.itemcontainer.ItemContainer;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;
import com.src.util.random.Rnd;

public class CastleManorManager
{
	private static final Log _log = LogFactory.getLog(CastleManorManager.class);

	private static CastleManorManager _instance;

	public static final int PERIOD_CURRENT = 0;
	public static final int PERIOD_NEXT = 1;
	public static int APPROVE = -1;

	private static final String CASTLE_MANOR_LOAD_PROCURE = "SELECT * FROM castle_manor_procure WHERE castle_id=?";
	private static final String CASTLE_MANOR_LOAD_PRODUCTION = "SELECT * FROM castle_manor_production WHERE castle_id=?";

	private static final int NEXT_PERIOD_APPROVE = Config.ALT_MANOR_APPROVE_TIME;
	private static final int NEXT_PERIOD_APPROVE_MIN = Config.ALT_MANOR_APPROVE_MIN;
	private static final int MANOR_REFRESH = Config.ALT_MANOR_REFRESH_TIME;
	private static final int MANOR_REFRESH_MIN = Config.ALT_MANOR_REFRESH_MIN;
	protected static final long MAINTENANCE_PERIOD = Config.ALT_MANOR_MAINTENANCE_PERIOD / 60000;

	private boolean _underMaintenance;
	private boolean _disabled;

	public static CastleManorManager getInstance()
	{
		if(_instance == null)
		{
			_instance = new CastleManorManager();
		}
		return _instance;
	}

	public class CropProcure
	{
		int _cropId;
		int _buyResidual;
		int _rewardType;
		int _buy;
		int _price;

		public CropProcure(int id)
		{
			_cropId = id;
			_buyResidual = 0;
			_rewardType = 0;
			_buy = 0;
			_price = 0;
		}

		public CropProcure(int id, int amount, int type, int buy, int price)
		{
			_cropId = id;
			_buyResidual = amount;
			_rewardType = type;
			_buy = buy;
			_price = price;
		}

		public int getReward()
		{
			return _rewardType;
		}

		public int getId()
		{
			return _cropId;
		}

		public int getAmount()
		{
			return _buyResidual;
		}

		public int getStartAmount()
		{
			return _buy;
		}

		public int getPrice()
		{
			return _price;
		}

		public void setAmount(int amount)
		{
			_buyResidual = amount;
		}
	}

	public class SeedProduction
	{
		int _seedId;
		int _residual;
		int _price;
		int _sales;

		public SeedProduction(int id)
		{
			_seedId = id;
			_sales = 0;
			_price = 0;
			_sales = 0;
		}

		public SeedProduction(int id, int amount, int price, int sales)
		{
			_seedId = id;
			_residual = amount;
			_price = price;
			_sales = sales;
		}

		public int getId()
		{
			return _seedId;
		}

		public int getCanProduce()
		{
			return _residual;
		}

		public int getPrice()
		{
			return _price;
		}

		public int getStartProduce()
		{
			return _sales;
		}

		public void setCanProduce(int amount)
		{
			_residual = amount;
		}
	}

	private CastleManorManager()
	{
		load();
		init();
		_underMaintenance = false;
		_disabled = !Config.ALLOW_MANOR;
		for(Castle c : CastleManager.getInstance().getCastles())
		{
			c.setNextPeriodApproved(APPROVE == 1 ? true : false);
		}
	}

	private void load()
	{
		Connection con = null;
		ResultSet rs = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			for(Castle castle : CastleManager.getInstance().getCastles())
			{
				FastList<SeedProduction> production = new FastList<SeedProduction>();
				FastList<SeedProduction> productionNext = new FastList<SeedProduction>();
				FastList<CropProcure> procure = new FastList<CropProcure>();
				FastList<CropProcure> procureNext = new FastList<CropProcure>();

				statement = con.prepareStatement(CASTLE_MANOR_LOAD_PRODUCTION);
				statement.setInt(1, castle.getCastleId());
				rs = statement.executeQuery();
				while(rs.next())
				{
					int seedId = rs.getInt("seed_id");
					int canProduce = rs.getInt("can_produce");
					int startProduce = rs.getInt("start_produce");
					int price = rs.getInt("seed_price");
					int period = rs.getInt("period");
					if(period == PERIOD_CURRENT)
					{
						production.add(new SeedProduction(seedId, canProduce, price, startProduce));
					}
					else
					{
						productionNext.add(new SeedProduction(seedId, canProduce, price, startProduce));
					}
				}
				statement.close();
				rs.close();
				statement = null;
				rs = null;

				castle.setSeedProduction(production, PERIOD_CURRENT);
				castle.setSeedProduction(productionNext, PERIOD_NEXT);

				statement = con.prepareStatement(CASTLE_MANOR_LOAD_PROCURE);
				statement.setInt(1, castle.getCastleId());
				rs = statement.executeQuery();
				while(rs.next())
				{
					int cropId = rs.getInt("crop_id");
					int canBuy = rs.getInt("can_buy");
					int startBuy = rs.getInt("start_buy");
					int rewardType = rs.getInt("reward_type");
					int price = rs.getInt("price");
					int period = rs.getInt("period");
					if(period == PERIOD_CURRENT)
					{
						procure.add(new CropProcure(cropId, canBuy, rewardType, startBuy, price));
					}
					else
					{
						procureNext.add(new CropProcure(cropId, canBuy, rewardType, startBuy, price));
					}
				}
				statement.close();
				rs.close();
				statement = null;
				rs = null;

				castle.setCropProcure(procure, PERIOD_CURRENT);
				castle.setCropProcure(procureNext, PERIOD_NEXT);

				if(!procure.isEmpty() || !procureNext.isEmpty() || !production.isEmpty() || !productionNext.isEmpty())
				{
					_log.info(castle.getName() + ": Data loaded");
				}
			}
		}
		catch(Exception e)
		{
			_log.error("Error restoring manor data", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	protected void init()
	{
		if(APPROVE == -1)
		{
			Calendar manorRefresh = Calendar.getInstance();
			manorRefresh.set(Calendar.HOUR_OF_DAY, MANOR_REFRESH);
			manorRefresh.set(Calendar.MINUTE, MANOR_REFRESH_MIN);
			manorRefresh.set(Calendar.SECOND, 0);
			manorRefresh.set(Calendar.MILLISECOND, 0);

			Calendar periodApprove = Calendar.getInstance();
			periodApprove.set(Calendar.HOUR_OF_DAY, NEXT_PERIOD_APPROVE);
			periodApprove.set(Calendar.MINUTE, NEXT_PERIOD_APPROVE_MIN);
			periodApprove.set(Calendar.SECOND, 0);
			periodApprove.set(Calendar.MILLISECOND, 0);
			boolean isApproved = periodApprove.getTimeInMillis() < Calendar.getInstance().getTimeInMillis() && manorRefresh.getTimeInMillis() > Calendar.getInstance().getTimeInMillis();
			APPROVE = isApproved ? 1 : 0;
		}

		Calendar FirstDelay = Calendar.getInstance();
		FirstDelay.set(Calendar.SECOND, 0);
		FirstDelay.set(Calendar.MILLISECOND, 0);
		FirstDelay.add(Calendar.MINUTE, 1);
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new ManorTask(), FirstDelay.getTimeInMillis() - Calendar.getInstance().getTimeInMillis(), 60000);
	}

	public void setNextPeriod()
	{
		for(Castle c : CastleManager.getInstance().getCastles())
		{
			c.setNextPeriodApproved(false);

			if(c.getOwnerId() <= 0)
			{
				continue;
			}

			L2Clan clan = ClanTable.getInstance().getClan(c.getOwnerId());
			if(clan == null)
			{
				continue;
			}

			ItemContainer cwh = clan.getWarehouse();
			if(!(cwh instanceof ClanWarehouse))
			{
				_log.info("Can't get clan warehouse for clan " + ClanTable.getInstance().getClan(c.getOwnerId()));
				return;
			}

			for(CropProcure crop : c.getCropProcure(PERIOD_CURRENT))
			{
				if(crop.getStartAmount() == 0)
				{
					continue;
				}

				if(crop.getStartAmount() > crop.getAmount())
				{
					_log.info("Manor System: Start Amount of Crop" + crop.getStartAmount() + "> Amount of currnt" + crop.getAmount());
					int count = crop.getStartAmount() - crop.getAmount();

					count = count * 90 / 100;
					if(count < 1 && Rnd.get(99) < 90)
					{
						count = 1;
					}

					if(count >= 1)
					{
						cwh.addItem("Manor", L2Manor.getInstance().getMatureCrop(crop.getId()), count, null, null);
					}
				}

				if(crop.getAmount() > 0)
				{
					c.addToTreasuryNoTax(crop.getAmount() * crop.getPrice());
				}
			}

			c.setSeedProduction(c.getSeedProduction(PERIOD_NEXT), PERIOD_CURRENT);
			c.setCropProcure(c.getCropProcure(PERIOD_NEXT), PERIOD_CURRENT);

			int manor_cost = c.getManorCost(PERIOD_CURRENT);
			if(c.getTreasury() < manor_cost)
			{
				c.setSeedProduction(getNewSeedsList(c.getCastleId()), PERIOD_NEXT);
				c.setCropProcure(getNewCropsList(c.getCastleId()), PERIOD_NEXT);
				manor_cost = c.getManorCost(PERIOD_CURRENT);
				if(manor_cost > 0)
				{
					_log.info(c.getName() + "|" + -manor_cost + "|ManorManager Error@setNextPeriod");
				}
			}
			else
			{
				FastList<SeedProduction> production = new FastList<SeedProduction>();
				FastList<CropProcure> procure = new FastList<CropProcure>();
				for(SeedProduction s : c.getSeedProduction(PERIOD_CURRENT))
				{
					s.setCanProduce(s.getStartProduce());
					production.add(s);
				}
				for(CropProcure cr : c.getCropProcure(PERIOD_CURRENT))
				{
					cr.setAmount(cr.getStartAmount());
					procure.add(cr);
				}
				c.setSeedProduction(production, PERIOD_NEXT);
				c.setCropProcure(procure, PERIOD_NEXT);
			}

			if(Config.ALT_MANOR_SAVE_ALL_ACTIONS)
			{
				c.saveCropData();
				c.saveSeedData();
			}

			L2PcInstance clanLeader = null;
			if(clan != null && clan.getLeader() != null && clan.getLeader().getName() != null)
			{
				clanLeader = L2World.getInstance().getPlayer(clan.getLeader().getName());
			}

			if(clanLeader != null)
			{
				clanLeader.sendPacket(new SystemMessage(SystemMessageId.THE_MANOR_INFORMATION_HAS_BEEN_UPDATED));
			}

			c.setNextPeriodApproved(false);

			clanLeader = null;
			clan = null;
		}
	}

	public void approveNextPeriod()
	{
		for(Castle c : CastleManager.getInstance().getCastles())
		{
			if(c.getOwnerId() > 0)
			{
				int manor_cost = c.getManorCost(PERIOD_NEXT);

				if(c.getTreasury() < manor_cost)
				{
					c.setSeedProduction(getNewSeedsList(c.getCastleId()), PERIOD_NEXT);
					c.setCropProcure(getNewCropsList(c.getCastleId()), PERIOD_NEXT);
					manor_cost = c.getManorCost(PERIOD_NEXT);
					if(manor_cost > 0)
					{
						_log.info(c.getName() + "|" + -manor_cost + "|ManorManager Error@approveNextPeriod");
					}
					L2Clan clan = ClanTable.getInstance().getClan(c.getOwnerId());
					L2PcInstance clanLeader = null;
					if(clan != null)
					{
						clanLeader = L2World.getInstance().getPlayer(clan.getLeader().getName());
					}
					if(clanLeader != null)
					{
						clanLeader.sendPacket(SystemMessageId.THE_AMOUNT_IS_NOT_SUFFICIENT_AND_SO_THE_MANOR_IS_NOT_IN_OPERATION);
					}
				}
				else
				{
					c.addToTreasuryNoTax(-manor_cost);
					_log.info(c.getName() + "|" + -manor_cost + "|ManorManager");
				}
			}
			c.setNextPeriodApproved(true);
		}
	}

	private FastList<SeedProduction> getNewSeedsList(int castleId)
	{
		FastList<SeedProduction> seeds = new FastList<SeedProduction>();
		FastList<Integer> seedsIds = L2Manor.getInstance().getSeedsForCastle(castleId);
		for(int sd : seedsIds)
		{
			seeds.add(new SeedProduction(sd));
		}
		return seeds;
	}

	private FastList<CropProcure> getNewCropsList(int castleId)
	{
		FastList<CropProcure> crops = new FastList<CropProcure>();
		FastList<Integer> cropsIds = L2Manor.getInstance().getCropsForCastle(castleId);
		for(int cr : cropsIds)
		{
			crops.add(new CropProcure(cr));
		}
		return crops;
	}

	public boolean isUnderMaintenance()
	{
		return _underMaintenance;
	}

	public void setUnderMaintenance(boolean mode)
	{
		_underMaintenance = mode;
	}

	public boolean isDisabled()
	{
		return _disabled;
	}

	public void setDisabled(boolean mode)
	{
		_disabled = mode;
	}

	public SeedProduction getNewSeedProduction(int id, int amount, int price, int sales)
	{
		return new SeedProduction(id, amount, price, sales);
	}

	public CropProcure getNewCropProcure(int id, int amount, int type, int price, int buy)
	{
		return new CropProcure(id, amount, type, buy, price);
	}

	public void save()
	{
		for(Castle c : CastleManager.getInstance().getCastles())
		{
			c.saveSeedData();
			c.saveCropData();
		}
	}

	private class ManorTask implements Runnable
	{
		@Override
		public void run()
		{
			int H = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
			int M = Calendar.getInstance().get(Calendar.MINUTE);

			if(APPROVE == 1)
			{
				if(H < NEXT_PERIOD_APPROVE || H > MANOR_REFRESH || H == MANOR_REFRESH && M >= MANOR_REFRESH_MIN)
				{
					APPROVE = 0;
					setUnderMaintenance(true);
					_log.info("Manor System: Under maintenance mode started");
				}
			}
			else if(isUnderMaintenance())
			{
				if(H != MANOR_REFRESH || M >= MANOR_REFRESH_MIN + MAINTENANCE_PERIOD)
				{
					setUnderMaintenance(false);
					_log.info("Manor System: Next period started");
					if(isDisabled())
						return;
					setNextPeriod();
					try
					{
						save();
					}
					catch(Exception e)
					{
						_log.error("Manor System: Failed to save manor data", e);
					}
				}
			}
			else
			{
				if(H > NEXT_PERIOD_APPROVE && H < MANOR_REFRESH || H == NEXT_PERIOD_APPROVE && M >= NEXT_PERIOD_APPROVE_MIN)
				{
					APPROVE = 1;
					_log.info("Manor System: Next period approved");
					if(isDisabled())
					{
						return;
					}
					approveNextPeriod();
				}
			}
		}
	}

}