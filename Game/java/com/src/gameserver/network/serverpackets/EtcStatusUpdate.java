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

import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.skills.effects.EffectCharge;
import com.src.gameserver.templates.skills.L2EffectType;

public class EtcStatusUpdate extends L2GameServerPacket
{
	private static final String _S__F3_ETCSTATUSUPDATE = "[S] F3 EtcStatusUpdate";

	private L2PcInstance _activeChar;
	private EffectCharge _effect;

	public EtcStatusUpdate(L2PcInstance activeChar)
	{
		_activeChar = activeChar;
		_effect = (EffectCharge) _activeChar.getFirstEffect(L2EffectType.CHARGE);
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xF3);
		if(_effect != null)
		{
			writeD(_effect.getLevel());
		}
		else
		{
			writeD(0x00);
		}
		writeD(_activeChar.getWeightPenalty());
		writeD(_activeChar.getMessageRefusal() || _activeChar.isChatBanned() ? 1 : 0);
		writeD(0x00);
		writeD(Math.min(_activeChar.getExpertisePenalty(), 1));
		writeD(_activeChar.getCharmOfCourage() ? 1 : 0);
		writeD(_activeChar.getDeathPenaltyBuffLevel());
	}

	@Override
	public String getType()
	{
		return _S__F3_ETCSTATUSUPDATE;
	}

}