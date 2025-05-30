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

import com.src.gameserver.model.actor.L2Npc;

public class MonRaceInfo extends L2GameServerPacket
{
	private static final String _S__DD_MonRaceInfo = "[S] dd MonRaceInfo";
	private int _unknown1;
	private int _unknown2;
	private L2Npc[] _monsters;
	private int[][] _speeds;

	public MonRaceInfo(int unknown1, int unknown2, L2Npc[] monsters, int[][] speeds)
	{
		_unknown1 = unknown1;
		_unknown2 = unknown2;
		_monsters = monsters;
		_speeds = speeds;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xdd);

		writeD(_unknown1);
		writeD(_unknown2);
		writeD(8);

		for(int i = 0; i < 8; i++)
		{
			writeD(_monsters[i].getObjectId());
			writeD(_monsters[i].getTemplate().npcId + 1000000);
			writeD(14107);
			writeD(181875 + 58 * (7 - i));
			writeD(-3566);
			writeD(12080);
			writeD(181875 + 58 * (7 - i));
			writeD(-3566);
			writeF(_monsters[i].getTemplate().collisionHeight);
			writeF(_monsters[i].getTemplate().collisionRadius);
			writeD(120);
			for(int j = 0; j < 20; j++)
			{
				if(_unknown1 == 0)
				{
					writeC(_speeds[i][j]);
				}
				else
				{
					writeC(0);
				}
			}

			writeD(0);
		}
	}

	@Override
	public String getType()
	{
		return _S__DD_MonRaceInfo;
	}

}