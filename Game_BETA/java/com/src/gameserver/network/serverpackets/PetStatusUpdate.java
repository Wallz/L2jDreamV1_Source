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

import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.instance.L2PetInstance;

public class PetStatusUpdate extends L2GameServerPacket
{
	private static final String _S__CE_PETSTATUSSHOW = "[S] B5 PetStatusUpdate";

	private L2Summon _summon;
	private int _maxHp, _maxMp;
	private int _maxFed, _curFed;

	public PetStatusUpdate(L2Summon summon)
	{
		_summon = summon;
		_maxHp = _summon.getMaxHp();
		_maxMp = _summon.getMaxMp();
		if(_summon instanceof L2PetInstance)
		{
			L2PetInstance pet = (L2PetInstance) _summon;
			_curFed = pet.getCurrentFed();
			_maxFed = pet.getMaxFed();
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xb5);
		writeD(_summon.getSummonType());
		writeD(_summon.getObjectId());
		writeD(_summon.getX());
		writeD(_summon.getY());
		writeD(_summon.getZ());
		writeS(_summon.getTitle());
		writeD(_curFed);
		writeD(_maxFed);
		writeD((int) _summon.getCurrentHp());
		writeD(_maxHp);
		writeD((int) _summon.getCurrentMp());
		writeD(_maxMp);
		writeD(_summon.getLevel());
		writeQ(_summon.getStat().getExp());
		writeQ(_summon.getExpForThisLevel());
		writeQ(_summon.getExpForNextLevel());
	}

	@Override
	public String getType()
	{
		return _S__CE_PETSTATUSSHOW;
	}

}