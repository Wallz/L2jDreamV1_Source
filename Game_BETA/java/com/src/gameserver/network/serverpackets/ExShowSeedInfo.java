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

import com.src.gameserver.managers.CastleManorManager.SeedProduction;
import com.src.gameserver.model.L2Manor;

public class ExShowSeedInfo extends L2GameServerPacket
{
	private static final String _S__FE_1C_EXSHOWSEEDINFO = "[S] FE:1C ExShowSeedInfo";
	private FastList<SeedProduction> _seeds;
	private int _manorId;

	public ExShowSeedInfo(int manorId, FastList<SeedProduction> seeds)
	{
		_manorId = manorId;
		_seeds = seeds;
		if(_seeds == null)
		{
			_seeds = new FastList<SeedProduction>();
		}
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x1C);
		writeC(0);
		writeD(_manorId);
		writeD(0);
		writeD(_seeds.size());
		for(SeedProduction seed : _seeds)
		{
			writeD(seed.getId());
			writeD(seed.getCanProduce());
			writeD(seed.getStartProduce());
			writeD(seed.getPrice());
			writeD(L2Manor.getInstance().getSeedLevel(seed.getId()));
			writeC(1);
			writeD(L2Manor.getInstance().getRewardItemBySeed(seed.getId(), 1));
			writeC(1);
			writeD(L2Manor.getInstance().getRewardItemBySeed(seed.getId(), 2));
		}
	}

	@Override
	public String getType()
	{
		return _S__FE_1C_EXSHOWSEEDINFO;
	}

}