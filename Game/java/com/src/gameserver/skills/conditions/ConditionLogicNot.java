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
package com.src.gameserver.skills.conditions;

import com.src.gameserver.skills.Env;

public class ConditionLogicNot extends Condition
{
	private Condition _condition;

	public ConditionLogicNot(Condition condition)
	{
		_condition = condition;
		if(getListener() != null)
		{
			_condition.setListener(this);
		}
	}

	@Override
	void setListener(ConditionListener listener)
	{
		if(listener != null)
		{
			_condition.setListener(this);
		}
		else
		{
			_condition.setListener(null);
		}
		super.setListener(listener);
	}

	@Override
	public boolean testImpl(Env env)
	{
		return !_condition.test(env);
	}

}