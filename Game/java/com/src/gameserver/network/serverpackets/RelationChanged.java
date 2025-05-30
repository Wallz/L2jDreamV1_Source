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

import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2SummonInstance;

public class RelationChanged extends L2GameServerPacket
{
	public static final int RELATION_PVP_FLAG = 0x00002;
	public static final int RELATION_HAS_KARMA = 0x00004;
	public static final int RELATION_LEADER = 0x00080;
	public static final int RELATION_INSIEGE = 0x00200;
	public static final int RELATION_ATTACKER = 0x00400;
	public static final int RELATION_ALLY = 0x00800;
	public static final int RELATION_ENEMY = 0x01000;
	public static final int RELATION_MUTUAL_WAR = 0x08000;
	public static final int RELATION_1SIDED_WAR = 0x10000;

	private static final String _S__CE_RELATIONCHANGED = "[S] CE RelationChanged";

	private int _objId, _relation, _autoAttackable, _karma, _pvpFlag;

	public RelationChanged(L2Playable activeChar, int relation, boolean autoattackable)
	{
		_objId = activeChar.getObjectId();
		_relation = relation;
		_autoAttackable = autoattackable ? 1 : 0;
		if(activeChar instanceof L2PcInstance)
		{
			_karma = ((L2PcInstance) activeChar).getKarma();
			_pvpFlag = ((L2PcInstance) activeChar).getPvpFlag();
		}
		else if(activeChar instanceof L2SummonInstance)
		{
			_karma = ((L2SummonInstance)activeChar).getOwner().getKarma();
			_pvpFlag = ((L2SummonInstance) activeChar).getOwner().getPvpFlag();
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xce);
		writeD(_objId);
		writeD(_relation);
		writeD(_autoAttackable);
		writeD(_karma);
		writeD(_pvpFlag);
	}

	@Override
	public String getType()
	{
		return _S__CE_RELATIONCHANGED;
	}

}