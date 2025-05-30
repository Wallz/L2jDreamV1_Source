/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.src.gameserver.skills.conditions;

import javolution.util.FastList;

import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.skills.Env;

public class ConditionPlayerClassIdRestriction extends Condition
{
	private final FastList<Integer> _classIds;

	public ConditionPlayerClassIdRestriction(FastList<Integer> classId)
	{
		_classIds = classId;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(!(env.player instanceof L2PcInstance))
		{
			return false;
		}

		return (_classIds.contains(((L2PcInstance)env.player).getClassId().getId()));
	}

}
