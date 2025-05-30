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

import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public class NpcInfoPoly extends L2GameServerPacket
{
	private static final String _S__22_NPCINFO = "[S] 16 NpcInfo";
	private L2Character _activeChar;
	private L2Object _obj;
	private int _x, _y, _z, _heading;
	private int _npcId;
	private boolean _isAttackable, _isSummoned, _isRunning, _isInCombat, _isAlikeDead;
	private int _mAtkSpd, _pAtkSpd;
	private int _runSpd, _walkSpd, _swimRunSpd, _swimWalkSpd, _flRunSpd, _flWalkSpd, _flyRunSpd, _flyWalkSpd;
	private int _rhand, _lhand;
	private String _name, _title;
	private int _abnormalEffect;
	L2NpcTemplate _template;
	private int _collisionRadius;
	private int _collisionHeight;

	public NpcInfoPoly(L2Object obj, L2Character attacker)
	{
		_obj = obj;
		_npcId = obj.getPoly().getPolyId();
		_template = NpcTable.getInstance().getTemplate(_npcId);
		_isAttackable = true;
		_rhand = 0;
		_lhand = 0;
		_isSummoned = false;
		_collisionRadius = _template.collisionRadius;
		_collisionHeight = _template.collisionHeight;
		if(_obj instanceof L2Character)
		{
			_activeChar = (L2Character) obj;
			_isAttackable = obj.isAutoAttackable(attacker);
			_rhand = _template.rhand;
			_lhand = _template.lhand;

		}

		if(_obj instanceof L2ItemInstance)
		{
			_x = _obj.getX();
			_y = _obj.getY();
			_z = _obj.getZ();
			_heading = 0;
			_mAtkSpd = 100;
			_pAtkSpd = 100;
			_runSpd = 120;
			_walkSpd = 80;
			_swimRunSpd = _flRunSpd = _flyRunSpd = _runSpd;
			_swimWalkSpd = _flWalkSpd = _flyWalkSpd = _walkSpd;
			_isRunning = _isInCombat = _isAlikeDead = false;
			_name = "item";
			_title = "polymorphed";
			_abnormalEffect = 0;
		}
		else
		{
			_x = _activeChar.getX();
			_y = _activeChar.getY();
			_z = _activeChar.getZ();
			_heading = _activeChar.getHeading();
			_mAtkSpd = _activeChar.getMAtkSpd();
			_pAtkSpd = _activeChar.getPAtkSpd();
			_runSpd = _activeChar.getRunSpeed();
			_walkSpd = _activeChar.getWalkSpeed();
			_swimRunSpd = _flRunSpd = _flyRunSpd = _runSpd;
			_swimWalkSpd = _flWalkSpd = _flyWalkSpd = _walkSpd;
			_isRunning = _activeChar.isRunning();
			_isInCombat = _activeChar.isInCombat();
			_isAlikeDead = _activeChar.isAlikeDead();
			_name = _activeChar.getName();
			_title = _activeChar.getTitle();
			_abnormalEffect = _activeChar.getAbnormalEffect();

		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x16);
		writeD(_obj.getObjectId());
		writeD(_npcId + 1000000);
		writeD(_isAttackable ? 1 : 0);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(_heading);
		writeD(0x00);
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
		writeF(_collisionRadius);
		writeF(_collisionHeight);
		writeD(_rhand);
		writeD(0);
		writeD(_lhand);
		writeC(1);
		writeC(_isRunning ? 1 : 0);
		writeC(_isInCombat ? 1 : 0);
		writeC(_isAlikeDead ? 1 : 0);
		writeC(_isSummoned ? 2 : 0);
		writeS(_name);
		writeS(_title);
		writeD(0);
		writeD(0);
		writeD(0000);

		writeH(_abnormalEffect);
		writeH(0x00);
		writeD(0000);
		writeD(0000);
		writeD(0000);
		writeD(0000);
		writeC(0000);
	}

	@Override
	public String getType()
	{
		return _S__22_NPCINFO;
	}

}