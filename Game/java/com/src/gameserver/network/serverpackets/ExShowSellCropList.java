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
import javolution.util.FastMap;

import com.src.gameserver.managers.CastleManorManager.CropProcure;
import com.src.gameserver.model.L2Manor;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class ExShowSellCropList extends L2GameServerPacket
{
	private static final String _S__FE_21_EXSHOWSELLCROPLIST = "[S] FE:21 ExShowSellCropList";

	private int _manorId = 1;
	private FastMap<Integer, L2ItemInstance> _cropsItems;
	private FastMap<Integer, CropProcure> _castleCrops;

	public ExShowSellCropList(L2PcInstance player, int manorId, FastList<CropProcure> crops)
	{
		_manorId = manorId;
		_castleCrops = new FastMap<Integer, CropProcure>();
		_cropsItems = new FastMap<Integer, L2ItemInstance>();

		FastList<Integer> allCrops = L2Manor.getInstance().getAllCrops();
		for(int cropId : allCrops)
		{
			L2ItemInstance item = player.getInventory().getItemByItemId(cropId);
			if(item != null)
			{
				_cropsItems.put(cropId, item);
			}
		}

		for(CropProcure crop : crops)
		{
			if(_cropsItems.containsKey(crop.getId()) && crop.getAmount() > 0)
			{
				_castleCrops.put(crop.getId(), crop);
			}
		}
	}

	@Override
	public void runImpl()
	{
	}

	@Override
	public void writeImpl()
	{
		writeC(0xFE);
		writeH(0x21);

		writeD(_manorId);
		writeD(_cropsItems.size());

		for(L2ItemInstance item : _cropsItems.values())
		{
			writeD(item.getObjectId());
			writeD(item.getItemId());
			writeD(L2Manor.getInstance().getSeedLevelByCrop(item.getItemId()));
			writeC(1);
			writeD(L2Manor.getInstance().getRewardItem(item.getItemId(), 1));
			writeC(1);
			writeD(L2Manor.getInstance().getRewardItem(item.getItemId(), 2));

			if(_castleCrops.containsKey(item.getItemId()))
			{
				CropProcure crop = _castleCrops.get(item.getItemId());
				writeD(_manorId);
				writeD(crop.getAmount());
				writeD(crop.getPrice());
				writeC(crop.getReward());
			}
			else
			{
				writeD(0xFFFFFFFF);
				writeD(0);
				writeD(0);
				writeC(0);
			}
			writeD(item.getCount());
		}
	}

	@Override
	public String getType()
	{
		return _S__FE_21_EXSHOWSELLCROPLIST;
	}

}