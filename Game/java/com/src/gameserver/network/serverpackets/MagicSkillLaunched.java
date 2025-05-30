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

import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.L2Character;

public class MagicSkillLaunched extends L2GameServerPacket
{
	private static final String _S__8E_MAGICSKILLLAUNCHED = "[S] 8E MagicSkillLaunched";
	private int _charObjId;
	private int _skillId;
	private int _skillLevel;
	private int _numberOfTargets;
	private L2Object[] _targets;
	private int _singleTargetId;

	public MagicSkillLaunched(L2Character cha, int skillId, int skillLevel, L2Object[] targets)
	{
		_charObjId = cha.getObjectId();
		_skillId = skillId;
		_skillLevel = skillLevel;

		if (targets != null)
		{
			_numberOfTargets = targets.length;
			_targets = targets;
		}
		else
		{
			_numberOfTargets = 1;
			L2Object[] objs = {cha};
			_targets = objs;
		}

		_singleTargetId = 0;
	}

	public MagicSkillLaunched(L2Character cha, int skillId, int skillLevel)
	{
		_charObjId = cha.getObjectId();
		_skillId = skillId;
		_skillLevel = skillLevel;
		_numberOfTargets = 1;
		_singleTargetId = cha.getTargetId();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x76);
		writeD(_charObjId);
		writeD(_skillId);
		writeD(_skillLevel);
		writeD(_numberOfTargets);
		if(_singleTargetId != 0 || _numberOfTargets == 0)
		{
			writeD(_singleTargetId);
		}
		else
		{
			for(L2Object target : _targets)
			{
				try
				{
					writeD(target.getObjectId());
				}
				catch(NullPointerException e)
				{
					writeD(0);
				}
			}
		}
	}

	@Override
	public String getType()
	{
		return _S__8E_MAGICSKILLLAUNCHED;
	}

}