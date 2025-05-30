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

import java.util.logging.Logger;

import com.src.gameserver.model.L2Effect;
import com.src.gameserver.skills.Env;
import com.src.gameserver.templates.skills.L2EffectType;

public class EffectImmobileUntilAttacked extends L2Effect
{
	static final Logger _log = Logger.getLogger(EffectImmobileUntilAttacked.class.getName());

	public EffectImmobileUntilAttacked(final Env env, final EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.IMMOBILEUNTILATTACKED;
	}

	@Override
	public boolean onActionTime()
	{
		getEffected().stopImmobileUntilAttacked(this);
		return false;
	}

	@Override
	public void onExit()
	{
		super.onExit();
		getEffected().stopImmobileUntilAttacked(this);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		getEffected().startImmobileUntilAttacked();
	}
}