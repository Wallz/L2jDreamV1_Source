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

public class PetInfo extends L2GameServerPacket
{
	private static final String _S__CA_PETINFO = "[S] b1 PetInfo";
	private L2Summon _summon;
	private int _x, _y, _z, _heading;
	private boolean _isSummoned;
	private int _mAtkSpd, _pAtkSpd;
	private int _runSpd, _walkSpd, _swimRunSpd, _swimWalkSpd, _flRunSpd, _flWalkSpd, _flyRunSpd, _flyWalkSpd;
	private int _maxHp, _maxMp;
	private int _maxFed, _curFed;

	public PetInfo(L2Summon summon)
	{
		_summon = summon;
		_isSummoned = _summon.isShowSummonAnimation();
		_x = _summon.getX();
		_y = _summon.getY();
		_z = _summon.getZ();
		_heading = _summon.getHeading();
		_mAtkSpd = _summon.getMAtkSpd();
		_pAtkSpd = _summon.getPAtkSpd();
		_runSpd = _summon.getRunSpeed();
		_walkSpd = _summon.getWalkSpeed();
		_swimRunSpd = _flRunSpd = _flyRunSpd = _runSpd;
		_swimWalkSpd = _flWalkSpd = _flyWalkSpd = _walkSpd;
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
		writeC(0xb1);
		writeD(_summon.getSummonType());
		writeD(_summon.getObjectId());
		writeD(_summon.getTemplate().idTemplate + 1000000);
		writeD(0);

		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(_heading);
		writeD(0);
		writeD(_mAtkSpd);
		writeD(_pAtkSpd);
		writeD(_runSpd);
		writeD(_walkSpd);
		writeD(_swimRunSpd);
		writeD(_swimWalkSpd);
		writeD(_flRunSpd);
		writeD(_flWalkSpd);
		writeD(_flyRunSpd);
		writeD(_flyWalkSpd);

		writeF(1);
		writeF(1);
		writeF(_summon.getTemplate().collisionRadius);
		writeF(_summon.getTemplate().collisionHeight);
		writeD(0);
		writeD(0);
		writeD(0);
		writeC(1);
		writeC(_summon.isRunning() ? 1 : 0);
		writeC(_summon.isInCombat() ? 1 : 0);
		writeC(_summon.isAlikeDead() ? 1 : 0);
		writeC(_isSummoned ? 2 : 0);
		writeS(_summon.getName());
		writeS(_summon.getTitle());
		writeD(1);
		writeD(_summon.getOwner() != null ? _summon.getOwner().getPvpFlag() : 0);	//0 = white,2= purpleblink, if its greater then karma = purple
		writeD(_summon.getOwner() != null ? _summon.getOwner().getKarma() : 0);  // karma
		writeD(_curFed);
		writeD(_maxFed);
		writeD((int) _summon.getCurrentHp());
		writeD(_maxHp);
		writeD((int) _summon.getCurrentMp());
		writeD(_maxMp);
		writeD(_summon.getStat().getSp());
		writeD(_summon.getLevel());
		writeQ(_summon.getStat().getExp());
		writeQ(_summon.getExpForThisLevel());
		writeQ(_summon.getExpForNextLevel());
		writeD(_summon instanceof L2PetInstance ? _summon.getInventory().getTotalWeight() : 0);
		writeD(_summon.getMaxLoad());
		writeD(_summon.getPAtk(null));
		writeD(_summon.getPDef(null));
		writeD(_summon.getMAtk(null, null));
		writeD(_summon.getMDef(null, null));
		writeD(_summon.getAccuracy());
		writeD(_summon.getEvasionRate(null));
		writeD(_summon.getCriticalHit(null, null));
		writeD(_runSpd);
		writeD(_summon.getPAtkSpd());
		writeD(_summon.getMAtkSpd());

		writeD(0);
		int npcId = _summon.getTemplate().npcId;

		if(npcId >= 12526 && npcId <= 12528)
		{
			writeH(1);
		}
		else
		{
			writeH(0);
		}

		writeC(0);

		writeH(0);
		writeC(0);
		writeD(_summon.getSoulShotsPerHit());
		writeD(_summon.getSpiritShotsPerHit());
	}

	@Override
	public String getType()
	{
		return _S__CA_PETINFO;
	}

}