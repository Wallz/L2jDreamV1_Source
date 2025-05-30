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
package com.src.gameserver.network.serverpackets;

import javolution.util.FastList;

import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.managers.CastleManorManager;
import com.src.gameserver.managers.CastleManorManager.SeedProduction;
import com.src.gameserver.model.L2Manor;
import com.src.gameserver.model.entity.siege.Castle;

public class ExShowSeedSetting extends L2GameServerPacket
{
	private static final String _S__FE_1F_EXSHOWSEEDSETTING = "[S] FE:1F ExShowSeedSetting";

	private int _manorId;
	private int _count;
	private int[] _seedData;

	@Override
	public void runImpl()
	{}

	public ExShowSeedSetting(int manorId)
	{
		_manorId = manorId;
		Castle c = CastleManager.getInstance().getCastleById(_manorId);
		FastList<Integer> seeds = L2Manor.getInstance().getSeedsForCastle(_manorId);
		_count = seeds.size();
		_seedData = new int[_count * 12];
		int i = 0;
		for(int s : seeds)
		{
			_seedData[i * 12 + 0] = s;
			_seedData[i * 12 + 1] = L2Manor.getInstance().getSeedLevel(s);
			_seedData[i * 12 + 2] = L2Manor.getInstance().getRewardItemBySeed(s, 1);
			_seedData[i * 12 + 3] = L2Manor.getInstance().getRewardItemBySeed(s, 2);
			_seedData[i * 12 + 4] = L2Manor.getInstance().getSeedSaleLimit(s);
			_seedData[i * 12 + 5] = L2Manor.getInstance().getSeedBuyPrice(s);
			_seedData[i * 12 + 6] = L2Manor.getInstance().getSeedBasicPrice(s) * 60 / 100;
			_seedData[i * 12 + 7] = L2Manor.getInstance().getSeedBasicPrice(s) * 10;
			SeedProduction seedPr = c.getSeed(s, CastleManorManager.PERIOD_CURRENT);
			if(seedPr != null)
			{
				_seedData[i * 12 + 8] = seedPr.getStartProduce();
				_seedData[i * 12 + 9] = seedPr.getPrice();
			}
			else
			{
				_seedData[i * 12 + 8] = 0;
				_seedData[i * 12 + 9] = 0;
			}

			seedPr = c.getSeed(s, CastleManorManager.PERIOD_NEXT);
			if(seedPr != null)
			{
				_seedData[i * 12 + 10] = seedPr.getStartProduce();
				_seedData[i * 12 + 11] = seedPr.getPrice();
			}
			else
			{
				_seedData[i * 12 + 10] = 0;
				_seedData[i * 12 + 11] = 0;
			}
			i++;
		}
	}

	@Override
	public void writeImpl()
	{
		writeC(0xFE);
		writeH(0x1F);

		writeD(_manorId);
		writeD(_count);

		for(int i = 0; i < _count; i++)
		{
			writeD(_seedData[i * 12 + 0]);
			writeD(_seedData[i * 12 + 1]);
			writeC(1);
			writeD(_seedData[i * 12 + 2]);
			writeC(1);
			writeD(_seedData[i * 12 + 3]);

			writeD(_seedData[i * 12 + 4]);
			writeD(_seedData[i * 12 + 5]);
			writeD(_seedData[i * 12 + 6]);
			writeD(_seedData[i * 12 + 7]);

			writeD(_seedData[i * 12 + 8]);
			writeD(_seedData[i * 12 + 9]);
			writeD(_seedData[i * 12 + 10]);
			writeD(_seedData[i * 12 + 11]);
		}
	}

	@Override
	public String getType()
	{
		return _S__FE_1F_EXSHOWSEEDSETTING;
	}

}