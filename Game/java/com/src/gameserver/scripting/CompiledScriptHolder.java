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
import java.io.Serializable;

import javax.script.CompiledScript;

public class CompiledScriptHolder implements Serializable
{
	private static final long serialVersionUID = 1L;

	private long _lastModified;
	private long _size;
	private CompiledScript _compiledScript;

	public CompiledScriptHolder(CompiledScript compiledScript, long lastModified, long size)
	{
		_compiledScript = compiledScript;
		_lastModified = lastModified;
		_size = size;
	}

	public CompiledScriptHolder(CompiledScript compiledScript, File scriptFile)
	{
		this(compiledScript, scriptFile.lastModified(), scriptFile.length());
	}

	public long getLastModified()
	{
		return _lastModified;
	}

	public void setLastModified(long lastModified)
	{
		_lastModified = lastModified;
	}

	public long getSize()
	{
		return _size;
	}

	public void setSize(long size)
	{
		_size = size;
	}

	public CompiledScript getCompiledScript()
	{
		return _compiledScript;
	}

	public void setCompiledScript(CompiledScript compiledScript)
	{
		_compiledScript = compiledScript;
	}

	public boolean matches(File f)
	{
		return f.lastModified() == getLastModified() && f.length() == getSize();
	}

}