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
import com.src.gameserver.model.actor.instance.L2ArtefactInstance;
import com.src.gameserver.model.actor.instance.L2ControlTowerInstance;
import com.src.gameserver.model.actor.instance.L2EffectPointInstance;
import com.src.gameserver.model.actor.instance.L2NpcInstance;
import com.src.gameserver.model.actor.instance.L2SiegeFlagInstance;
import com.src.gameserver.model.actor.instance.L2SiegeSummonInstance;
import com.src.gameserver.network.serverpackets.BeginRotation;
import com.src.gameserver.network.serverpackets.StopRotation;
import com.src.gameserver.skills.Env;
import com.src.gameserver.templates.skills.L2EffectType;

public class EffectBluff extends L2Effect
{
	public EffectBluff(final Env env, final EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.BLUFF;
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}

	@Override
	public void onStart()
	{
		if(getEffected().isDead() || getEffected().isAfraid())
		{
			return;
		}

		if(getEffected() instanceof L2NpcInstance || getEffected() instanceof L2ControlTowerInstance || getEffected() instanceof L2ArtefactInstance || getEffected() instanceof L2EffectPointInstance || getEffected() instanceof L2SiegeFlagInstance || getEffected() instanceof L2SiegeSummonInstance)
		{
			return;
		}

		super.onStart();

		getEffected().setTarget(null);
		getEffected().breakCast();
		getEffected().breakAttack();
		getEffected().getAI().stopFollow();
		getEffected().getAI().clientStopAutoAttack();

		getEffected().broadcastPacket(new BeginRotation(getEffected(), getEffected().getHeading(), 1, 65535));
		getEffected().broadcastPacket(new StopRotation(getEffected(), getEffector().getHeading(), 65535));
		getEffected().setHeading(getEffector().getHeading());
		onActionTime();
	}
}