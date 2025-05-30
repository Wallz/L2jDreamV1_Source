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

import com.src.gameserver.model.actor.L2Character;

public class ExFishingStartCombat extends L2GameServerPacket
{
	private static final String _S__FE_15_EXFISHINGSTARTCOMBAT = "[S] FE:15 ExFishingStartCombat";

	private L2Character _activeChar;
	private int _time, _hp;
	private int _lureType, _deceptiveMode, _mode;

	public ExFishingStartCombat(L2Character character, int time, int hp, int mode, int lureType, int deceptiveMode)
	{
		_activeChar = character;
		_time = time;
		_hp = hp;
		_mode = mode;
		_lureType = lureType;
		_deceptiveMode = deceptiveMode;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x15);

		writeD(_activeChar.getObjectId());
		writeD(_time);
		writeD(_hp);
		writeC(_mode);
		writeC(_lureType);
		writeC(_deceptiveMode);
	}

	@Override
	public String getType()
	{
		return _S__FE_15_EXFISHINGSTARTCOMBAT;
	}

}