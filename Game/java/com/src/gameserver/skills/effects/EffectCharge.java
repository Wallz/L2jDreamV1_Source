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
package com.src.gameserver.skills.effects;

import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.EtcStatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.Env;
import com.src.gameserver.templates.skills.L2EffectType;

public class EffectCharge extends L2Effect
{
	public int numCharges;

	public EffectCharge(Env env, EffectTemplate template)
	{
		super(env, template);
		numCharges = 1;
		if(env.target instanceof L2PcInstance)
		{
			env.target.sendPacket(new EtcStatusUpdate((L2PcInstance) env.target));
			getEffected().sendPacket(new SystemMessage(SystemMessageId.FORCE_INCREASED_TO_S1).addNumber(numCharges));
		}
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.CHARGE;
	}

	@Override
	public boolean onActionTime()
	{
		return true;
	}

	@Override
	public int getLevel()
	{
		return numCharges;
	}

	public void addNumCharges(int i)
	{
		numCharges = numCharges + i;
	}
}