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

import java.util.List;

import javolution.util.FastList;

public class AquireSkillInfo extends L2GameServerPacket
{
	private static final String _S__A4_AQUIRESKILLINFO = "[S] 8b AquireSkillInfo";

	private List<Req> _reqs;
	private int _id, _level, _spCost, _mode;

	private class Req
	{
		public int itemId;
		public int count;
		public int type;
		public int unk;

		public Req(int pType, int pItemId, int pCount, int pUnk)
		{
			itemId = pItemId;
			type = pType;
			count = pCount;
			unk = pUnk;
		}
	}

	public AquireSkillInfo(int id, int level, int spCost, int mode)
	{
		_reqs = new FastList<Req>();
		_id = id;
		_level = level;
		_spCost = spCost;
		_mode = mode;
	}

	public void addRequirement(int type, int id, int count, int unk)
	{
		_reqs.add(new Req(type, id, count, unk));
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x8b);
		writeD(_id);
		writeD(_level);
		writeD(_spCost);
		writeD(_mode);
		writeD(_reqs.size());

		for(Req temp : _reqs)
		{
			writeD(temp.type);
			writeD(temp.itemId);
			writeD(temp.count);
			writeD(temp.unk);
		}
	}

	@Override
	public String getType()
	{
		return _S__A4_AQUIRESKILLINFO;
	}

}