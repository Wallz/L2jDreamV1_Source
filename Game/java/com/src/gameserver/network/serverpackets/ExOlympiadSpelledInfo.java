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

import com.src.gameserver.model.actor.instance.L2PcInstance;

public class ExOlympiadSpelledInfo extends L2GameServerPacket
{
	private static final String _S__FE_2A_OLYMPIADSPELLEDINFO = "[S] FE:2A ExOlympiadSpelledInfo";

	private L2PcInstance _player;
	private List<Effect> _effects;

	private class Effect
	{
		protected int _skillId;
		protected int _dat;
		protected int _duration;

		public Effect(int pSkillId, int pDat, int pDuration)
		{
			_skillId = pSkillId;
			_dat = pDat;
			_duration = pDuration;
		}
	}

	public ExOlympiadSpelledInfo(L2PcInstance player)
	{
		_effects = new FastList<Effect>();
		_player = player;
	}

	public void addEffect(int skillId, int dat, int duration)
	{
		_effects.add(new Effect(skillId, dat, duration));
	}

	@Override
	protected final void writeImpl()
	{
		if(_player == null)
		{
			return;
		}
		writeC(0xfe);
		writeH(0x2a);
		writeD(_player.getObjectId());
		writeD(_effects.size());
		for(Effect temp : _effects)
		{
			writeD(temp._skillId);
			writeH(temp._dat);
			writeD(temp._duration / 1000);
		}
	}

	@Override
	public String getType()
	{
		return _S__FE_2A_OLYMPIADSPELLEDINFO;
	}

}