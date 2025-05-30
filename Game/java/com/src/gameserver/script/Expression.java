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
package com.src.gameserver.script;

import javax.script.ScriptContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.scripting.L2ScriptEngineManager;

public class Expression
{
	private final static Log _log = LogFactory.getLog(Expression.class);

	private final ScriptContext _context;

	public static Object eval(String lang, String code)
	{
		try
		{
			return L2ScriptEngineManager.getInstance().eval(lang, code);
		}
		catch(Exception e)
		{
			_log.error("", e);
			return null;
		}
	}

	public static Object eval(ScriptContext context, String lang, String code)
	{
		try
		{
			return L2ScriptEngineManager.getInstance().eval(lang, code, context);
		}
		catch(Exception e)
		{
			_log.error("", e);
			return null;
		}
	}

	public static Expression create(ScriptContext context)
	{
		try
		{
			return new Expression(context);
		}
		catch(Exception e)
		{
			_log.error("", e);
			return null;
		}
	}

	private Expression(ScriptContext pContext)
	{
		_context = pContext;
	}

	public <T> void addDynamicVariable(String name, T value)
	{
		try
		{
			_context.setAttribute(name, value, ScriptContext.ENGINE_SCOPE);
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
	}

	public void removeDynamicVariable(String name)
	{
		try
		{
			_context.removeAttribute(name, ScriptContext.ENGINE_SCOPE);
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
	}

}