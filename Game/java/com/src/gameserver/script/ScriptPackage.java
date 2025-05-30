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

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javolution.util.FastList;

public class ScriptPackage
{
	private List<ScriptDocument> _scriptFiles;
	private List<String> _otherFiles;
	private String _name;

	public ScriptPackage(ZipFile pack)
	{
		_scriptFiles = new FastList<ScriptDocument>();
		_otherFiles = new FastList<String>();
		_name = pack.getName();
		addFiles(pack);
	}

	public List<String> getOtherFiles()
	{
		return _otherFiles;
	}

	public List<ScriptDocument> getScriptFiles()
	{
		return _scriptFiles;
	}

	private void addFiles(ZipFile pack)
	{
		for(Enumeration<? extends ZipEntry> e = pack.entries(); e.hasMoreElements();)
		{
			ZipEntry entry = e.nextElement();
			if(entry.getName().endsWith(".xml"))
			{
				try
				{
					ScriptDocument newScript = new ScriptDocument(entry.getName(), pack.getInputStream(entry));
					_scriptFiles.add(newScript);
				}
				catch(IOException e1)
				{
					e1.printStackTrace();
				}
			}
			else if(!entry.isDirectory())
			{
				_otherFiles.add(entry.getName());
			}
		}
	}

	public String getName()
	{
		return _name;
	}

	@Override
	public String toString()
	{
		if(getScriptFiles().isEmpty() && getOtherFiles().isEmpty())
		{
			return "Empty Package.";
		}

		String out = "Package Name: " + getName() + "\n";

		if(!getScriptFiles().isEmpty())
		{
			out += "Xml Script Files...\n";
			for(ScriptDocument script : getScriptFiles())
			{
				out += script.getName() + "\n";
			}
		}

		if(!getOtherFiles().isEmpty())
		{
			out += "Other Files...\n";
			for(String fileName : getOtherFiles())
			{
				out += fileName + "\n";
			}
		}
		return out;
	}

}