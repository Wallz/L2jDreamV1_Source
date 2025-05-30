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

public class ExFishingHpRegen extends L2GameServerPacket
{
	private static final String _S__FE_16_EXFISHINGHPREGEN = "[S] FE:16 ExFishingHPRegen";

	private L2Character _activeChar;
	private int _time, _fishHP, _hpMode, _anim, _goodUse, _penalty, _hpBarColor;

	public ExFishingHpRegen(L2Character character, int time, int fishHP, int HPmode, int GoodUse, int anim, int penalty, int hpBarColor)
	{
		_activeChar = character;
		_time = time;
		_fishHP = fishHP;
		_hpMode = HPmode;
		_goodUse = GoodUse;
		_anim = anim;
		_penalty = penalty;
		_hpBarColor = hpBarColor;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x16);

		writeD(_activeChar.getObjectId());
		writeD(_time);
		writeD(_fishHP);
		writeC(_hpMode);
		writeC(_goodUse);
		writeC(_anim);
		writeD(_penalty);
		writeC(_hpBarColor);
	}

	@Override
	public String getType()
	{
		return _S__FE_16_EXFISHINGHPREGEN;
	}

}