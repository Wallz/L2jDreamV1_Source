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

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.geo.GeoData;
import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.Location;
import com.src.gameserver.model.actor.instance.L2NpcInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2SiegeFlagInstance;
import com.src.gameserver.model.actor.instance.L2SiegeGuardInstance;
import com.src.gameserver.model.actor.instance.L2SiegeSummonInstance;
import com.src.gameserver.model.actor.position.L2CharPosition;
import com.src.gameserver.skills.Env;
import com.src.gameserver.templates.skills.L2EffectType;

final class EffectFear extends L2Effect
{
	public static final int FEAR_RANGE = 500;

	public EffectFear(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.FEAR;
	}

	@Override
	public void onStart()
	{
		if(getEffected().isSleeping())
		{
			getEffected().stopSleeping(null);
		}

		if(!getEffected().isAfraid())
		{
			getEffected().startFear();
			onActionTime();
		}
	}

	@Override
	public void onExit()
	{
		getEffected().stopFear(this);
	}

	@Override
	public boolean onActionTime()
	{
		if(getEffected() instanceof L2PcInstance && getEffector() instanceof L2PcInstance && getSkill().getId() != 1376 && getSkill().getId() != 1169 && getSkill().getId() != 65 && getSkill().getId() != 1092)
		{
			return false;
		}

		if(getEffected() instanceof L2NpcInstance || getEffected() instanceof L2SiegeGuardInstance || getEffected() instanceof L2SiegeFlagInstance || getEffected() instanceof L2SiegeSummonInstance)
		{
			return false;
		}

		int posX = getEffected().getX();
		int posY = getEffected().getY();
		int posZ = getEffected().getZ();

		int signx = -1;
		int signy = -1;

		if(getEffected().getX() > getEffector().getX())
		{
			signx = 1;
		}

		if(getEffected().getY() > getEffector().getY())
		{
			signy = 1;
		}

		posX += signx * FEAR_RANGE;
		posY += signy * FEAR_RANGE;

		Location destiny = GeoData.getInstance().moveCheck(getEffected().getX(), getEffected().getY(), getEffected().getZ(), posX, posY, posZ);
		getEffected().setRunning();
		getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(destiny.getX(),destiny.getY(),destiny.getZ(),0));

		destiny = null;
		return true;
	}
}