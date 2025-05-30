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

import com.src.gameserver.model.L2Manor;

public class ExShowManorDefaultInfo extends L2GameServerPacket
{
	private static final String _S__FE_1C_EXSHOWSEEDINFO = "[S] FE:1E ExShowManorDefaultInfo";

	private FastList<Integer> _crops = null;

	public ExShowManorDefaultInfo()
	{
		_crops = L2Manor.getInstance().getAllCrops();
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x1E);
		writeC(0);
		writeD(_crops.size());
		for(int cropId : _crops)
		{
			writeD(cropId);
			writeD(L2Manor.getInstance().getSeedLevelByCrop(cropId));
			writeD(L2Manor.getInstance().getSeedBasicPriceByCrop(cropId));
			writeD(L2Manor.getInstance().getCropBasicPrice(cropId));
			writeC(1);
			writeD(L2Manor.getInstance().getRewardItem(cropId, 1));
			writeC(1);
			writeD(L2Manor.getInstance().getRewardItem(cropId, 2));
		}
	}

	@Override
	public String getType()
	{
		return _S__FE_1C_EXSHOWSEEDINFO;
	}

}