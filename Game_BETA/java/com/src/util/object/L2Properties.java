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
package com.src.util.object;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class L2Properties extends Properties
{
	private static final long serialVersionUID = -4599023842346938325L;
	private static final Log _log = LogFactory.getLog(L2Properties.class);
	private boolean _warn = false;

	public L2Properties()
	{}

	public L2Properties setLog(boolean warn)
	{
		_warn = warn;

		return this;
	}

	public L2Properties(String name) throws IOException
	{
		load(new FileInputStream(name));
	}

	public L2Properties(File file) throws IOException
	{
		load(new FileInputStream(file));
	}

	public L2Properties(InputStream inStream) throws IOException
	{
		load(inStream);
	}

	public L2Properties(Reader reader) throws IOException
	{
		load(reader);
	}

	public void load(String name) throws IOException
	{
		load(new FileInputStream(name));
	}

	public void load(File file) throws IOException
	{
		load(new FileInputStream(file));
	}

	@Override
	public synchronized void load(InputStream inStream) throws IOException
	{
		try
		{
			super.load(inStream);
		}
		finally
		{
			inStream.close();
		}
	}

	@Override
	public synchronized void load(Reader reader) throws IOException
	{
		try
		{
			super.load(reader);
		}
		finally
		{
			reader.close();
		}
	}

	@Override
	public String getProperty(String key)
	{
		String property = super.getProperty(key);

		if(property == null)
		{
			if(_warn)
			{
				_log.warn("L2Properties: Missing property for key - " + key);
			}
			return null;
		}
		return property.trim();
	}

	@Override
	public String getProperty(String key, String defaultValue)
	{
		String property = super.getProperty(key, defaultValue);

		if(property == null)
		{
			if(_warn)
			{
				_log.warn("L2Properties: Missing defaultValue for key - " + key);
			}
			return null;
		}
		return property.trim();
	}

	public List<String> getStringList(String key, String defaultValue, String regex)
	{
		List<String> list = new ArrayList<String>();
		String value = getProperty(key, defaultValue);

		if(value.equals(""))
		{
			return list;
		}

		list.addAll(Arrays.asList(value.split(regex)));
		return list;
	}
}