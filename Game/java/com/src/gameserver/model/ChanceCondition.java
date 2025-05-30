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
package com.src.gameserver.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.templates.StatsSet;
import com.src.util.random.Rnd;

public final class ChanceCondition
{
	private final static Log _log = LogFactory.getLog(ChanceCondition.class);

	public static final int EVT_HIT = 1;
	public static final int EVT_CRIT = 2;
	public static final int EVT_CAST = 4;
	public static final int EVT_PHYSICAL = 8;
	public static final int EVT_MAGIC = 16;
	public static final int EVT_MAGIC_GOOD = 32;
	public static final int EVT_MAGIC_OFFENSIVE = 64;
	public static final int EVT_ATTACKED = 128;
	public static final int EVT_ATTACKED_HIT = 256;
	public static final int EVT_ATTACKED_CRIT = 512;
	public static final int EVT_HIT_BY_SKILL = 1024;
	public static final int EVT_HIT_BY_OFFENSIVE_SKILL = 2048;
	public static final int EVT_HIT_BY_GOOD_MAGIC = 4096;

	public static enum TriggerType
	{
		ON_HIT(1),
		ON_CRIT(2),
		ON_CAST(4),
		ON_PHYSICAL(8),
		ON_MAGIC(16),
		ON_MAGIC_GOOD(32),
		ON_MAGIC_OFFENSIVE(64),
		ON_ATTACKED(128),
		ON_ATTACKED_HIT(256),
		ON_ATTACKED_CRIT(512),
		ON_HIT_BY_SKILL(1024),
		ON_HIT_BY_OFFENSIVE_SKILL(2048),
		ON_HIT_BY_GOOD_MAGIC(4096);

		private int _mask;

		private TriggerType(int mask)
		{
			_mask = mask;
		}

		public boolean check(int event)
		{
			return (_mask & event) != 0;
		}
	}

	private TriggerType _triggerType;

	private int _chance;

	private ChanceCondition(TriggerType trigger, int chance)
	{
		_triggerType = trigger;
		_chance = chance;
	}

	public static ChanceCondition parse(StatsSet set)
	{
		try
		{
			TriggerType trigger = set.getEnum("chanceType", TriggerType.class);
			int chance = set.getInteger("activationChance", 0);
			if(trigger != null && chance > 0)
			{
				return new ChanceCondition(trigger, chance);
			}
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
		return null;
	}

	public boolean trigger(int event)
	{
		return _triggerType.check(event) && Rnd.get(100) < _chance;
	}

	@Override
	public String toString()
	{
		return "Trigger[" + _chance + ";" + _triggerType.toString() + "]";
	}

}