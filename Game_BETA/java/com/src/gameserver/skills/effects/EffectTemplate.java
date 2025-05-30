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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.model.L2Effect;
import com.src.gameserver.skills.Env;
import com.src.gameserver.skills.conditions.Condition;
import com.src.gameserver.skills.funcs.FuncTemplate;
import com.src.gameserver.skills.funcs.Lambda;
import com.src.gameserver.templates.skills.L2SkillType;

public final class EffectTemplate
{
	private final static Log _log = LogFactory.getLog(EffectTemplate.class);

	private final Class<?> _func;
	private final Constructor<?> _constructor;

	public final Condition attachCond;
	public final Condition applayCond;
	public final Lambda lambda;
	public final int counter;
	public int period;
	public final int abnormalEffect;
	public FuncTemplate[] funcTemplates;
	public boolean showIcon;

	public final String stackType;
	public final float stackOrder;

	public final double effectPower; // to thandle chance
	public final L2SkillType effectType; // to handle resistences etc...

	public EffectTemplate(Condition pAttachCond, Condition pApplayCond, String func, Lambda pLambda, int pCounter, int pPeriod, int pAbnormalEffect, String pStackType, float pStackOrder, int pShowIcon,L2SkillType eType, double ePower)
	{
		attachCond = pAttachCond;
		applayCond = pApplayCond;
		lambda = pLambda;
		counter = pCounter;
		period = pPeriod;
		abnormalEffect = pAbnormalEffect;
		stackType = pStackType;
		stackOrder = pStackOrder;
		showIcon = pShowIcon == 0;
		effectType = eType;
		effectPower = ePower;
		try
		{
			_func = Class.forName("com.src.gameserver.skills.effects.Effect" + func);
		}
		catch(ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
		try
		{
			_constructor = _func.getConstructor(Env.class, EffectTemplate.class);
		}
		catch(NoSuchMethodException e)
		{
			throw new RuntimeException(e);
		}
	}

	public L2Effect getEffect(Env env)
	{
		if(attachCond != null && !attachCond.test(env))
		{
			return null;
		}
		try
		{
			L2Effect effect = (L2Effect) _constructor.newInstance(env, this);

			return effect;
		}
		catch(IllegalAccessException e)
		{
			_log.error("", e);
			return null;
		}
		catch(InstantiationException e)
		{
			_log.error("", e);
			return null;
		}
		catch(InvocationTargetException e)
		{
			_log.error("Error creating new instance of Class " + _func, e);
			return null;
		}
	}

	public void attach(FuncTemplate f)
	{
		if(funcTemplates == null)
		{
			funcTemplates = new FuncTemplate[]
			{
				f
			};
		}
		else
		{
			int len = funcTemplates.length;
			FuncTemplate[] tmp = new FuncTemplate[len + 1];
			System.arraycopy(funcTemplates, 0, tmp, 0, len);
			tmp[len] = f;
			funcTemplates = tmp;
		}
	}

}