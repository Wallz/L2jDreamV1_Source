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
package com.src.gameserver.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Loader extends ClassLoader
{
	private HashMap<String, Class<?>> classes = new HashMap<String, Class<?>>();

	private static Loader _instance;

	public static Loader getInstance()
	{
		if(_instance == null)
		{
			_instance = new Loader("config/start.bin");
		}
		return _instance;
	}

	public Loader(String nameZip)
	{
		try
		{
			ZipFile file = new ZipFile(nameZip);

			byte[] data = null;
			Enumeration<?> list = file.entries();

			String name = "";

			while(list.hasMoreElements())
			{
				ZipEntry element = (ZipEntry) list.nextElement();

				if(element.isDirectory())
				{
					name = element.getName().replace("/", "");
				}
				else if(element.getName().startsWith(name))
				{
					byte[] dataAnon = getBytes(file, element);

					String ClassName = element.getName().substring(name.length() + 1).replace(".class", "");

					if(ClassName.compareTo(name) == 0)
					{
						data = dataAnon;
					}
					else
					{
						@SuppressWarnings("unused")
						Class<?> clazz = defineClass(ClassName, dataAnon, 0, dataAnon.length);
						System.out.println("Loading " + ClassName);
					}

					dataAnon = null;
				}

				element = null;
				if(data != null)
				{
					classes.put(name, defineClass(name, data, 0, data.length));
					System.out.println("Loading " + name);
				}
				data = null;
			}

			name = null;
			list = null;
			file = null;
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	public void invokeMethod(String className, String methodName, Object... params) throws Exception
	{
		Class<?> clazz = classes.get(className);

		if(clazz == null)
		{
			throw new Exception("Not found class.");
		}

		Method method;

		Class<?>[] paramClass = new Class<?>[params.length];
		int i = 0;
		for(Object param : params)
		{
			paramClass[i++] = param.getClass();
		}
		method = clazz.getMethod(methodName, paramClass);

		paramClass = null;

		if(method == null)
		{
			throw new Exception("Not found custom method.");
		}

		method.invoke(null, params);

		method = null;
	}

	private byte[] getBytes(ZipFile file, ZipEntry element) throws IOException
	{
		InputStream is;

		if(element.getSize() >= Integer.MAX_VALUE)
		{
			throw new IOException();
		}

		byte[] data = new byte[(int) element.getSize()];

		is = file.getInputStream(element);

		is.read(data);

		is.close();
		is = null;

		return data;
	}

}