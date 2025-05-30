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
package com.src.gameserver.scripting;

import java.io.File;
import java.io.FileNotFoundException;

import javax.script.ScriptException;

public abstract class ManagedScript
{
	private File _scriptFile;
	private long _lastLoadTime;
	private boolean _isActive;

	public abstract String getScriptName();
	public abstract ScriptManager<?> getScriptManager();
	public abstract boolean unload();

	public ManagedScript()
	{
		_scriptFile = L2ScriptEngineManager.getInstance().getCurrentLoadScript();
		setLastLoadTime(System.currentTimeMillis());
	}

	public boolean reload()
	{
		try
		{
			L2ScriptEngineManager.getInstance().executeScript(getScriptFile());
			return true;
		}
		catch(FileNotFoundException e)
		{
			return false;
		}
		catch(ScriptException e)
		{
			return false;
		}
	}

	public void setActive(boolean status)
	{
		_isActive = status;
	}

	public boolean isActive()
	{
		return _isActive;
	}

	public File getScriptFile()
	{
		return _scriptFile;
	}

	protected void setLastLoadTime(long lastLoadTime)
	{
		_lastLoadTime = lastLoadTime;
	}

	protected long getLastLoadTime()
	{
		return _lastLoadTime;
	}

}