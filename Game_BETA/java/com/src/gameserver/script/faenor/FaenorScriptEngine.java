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
package com.src.gameserver.script.faenor;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.script.ScriptContext;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Node;

import com.src.Config;
import com.src.gameserver.script.Parser;
import com.src.gameserver.script.ParserNotCreatedException;
import com.src.gameserver.script.ScriptDocument;
import com.src.gameserver.script.ScriptEngine;
import com.src.gameserver.script.ScriptPackage;
import com.src.gameserver.scripting.L2ScriptEngineManager;

public class FaenorScriptEngine extends ScriptEngine
{
	private final static Log _log = LogFactory.getLog(FaenorScriptEngine.class);

	public final static String PACKAGE_DIRECTORY = "data/faenor/";
	public final static boolean DEBUG = true;

	private LinkedList<ScriptDocument> _scripts;

	public static FaenorScriptEngine getInstance()
	{
		return SingletonHolder._instance;
	}

	private FaenorScriptEngine()
	{
		_scripts = new LinkedList<ScriptDocument>();
		loadPackages();
		parsePackages();

	}

	public void reloadPackages()
	{
		_scripts = new LinkedList<ScriptDocument>();
		parsePackages();
	}

	private void loadPackages()
	{
		File packDirectory = new File(Config.DATAPACK_ROOT, PACKAGE_DIRECTORY);

		FileFilter fileFilter = new FileFilter()
		{
			@Override
			public boolean accept(File file)
			{
				return file.getName().endsWith(".zip");
			}
		};

		File[] files = packDirectory.listFiles(fileFilter);
		if(files == null)
		{
			return;
		}
		ZipFile zipPack;

		for(File file : files)
		{
			try
			{
				zipPack = new ZipFile(file);
			}
			catch(ZipException e)
			{
				_log.error("", e);
				continue;
			}
			catch(IOException e)
			{
				_log.error("", e);
				continue;
			}

			ScriptPackage module = new ScriptPackage(zipPack);

			List<ScriptDocument> scrpts = module.getScriptFiles();
			for(ScriptDocument script : scrpts)
			{
				_scripts.add(script);
			}
			try
			{
				zipPack.close();
			}
			catch(IOException e)
			{
			}
		}
	}

	public void orderScripts()
	{
		if(_scripts.size() > 1)
		{
			for(int i = 0; i < _scripts.size();)
			{
				if(_scripts.get(i).getName().contains("NpcStatData"))
				{
					_scripts.addFirst(_scripts.remove(i));
				}
				else
				{
					i++;
				}
			}
		}
	}

	public void parsePackages()
	{
		L2ScriptEngineManager sem = L2ScriptEngineManager.getInstance();
		ScriptContext context = sem.getScriptContext("beanshell");
		try
		{
			sem.eval("beanshell", "double log1p(double d) { return Math.log1p(d); }");
			sem.eval("beanshell", "double pow(double d, double p) { return Math.pow(d,p); }");

			for(ScriptDocument script : _scripts)
			{
				parseScript(script, context);
			}
		}
		catch(ScriptException e)
		{
			_log.error("", e);
		}
	}

	public void parseScript(ScriptDocument script, ScriptContext context)
	{
		Node node = script.getDocument().getFirstChild();
		String parserClass = "faenor.Faenor" + node.getNodeName() + "Parser";

		Parser parser = null;
		try
		{
			parser = createParser(parserClass);
		}
		catch(ParserNotCreatedException e)
		{
			_log.error("ERROR: No parser registered for Script: " + parserClass, e);
		}

		if(parser == null)
		{
			_log.warn("Unknown Script Type: " + script.getName());
			return;
		}

		try
		{
			parser.parseScript(node, context);
			// This is fucking spam ^_^
			// _log.fine(script.getName() + "Script Sucessfullty Parsed.");
		}
		catch(Exception e)
		{
			_log.error("Script Parsing Failed.", e);
		}
	}

	@Override
	public String toString()
	{
		if(_scripts.isEmpty())
		{
			return "No Packages Loaded.";
		}

		String out = "Script Packages currently loaded:\n";

		for(ScriptDocument script : _scripts)
		{
			out += script;
		}
		return out;
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final FaenorScriptEngine _instance = new FaenorScriptEngine();
	}

}