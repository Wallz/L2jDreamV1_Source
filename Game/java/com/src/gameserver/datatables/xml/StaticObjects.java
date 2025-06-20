/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.src.gameserver.datatables.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.src.Config;
import com.src.gameserver.idfactory.IdFactory;
import com.src.gameserver.model.actor.instance.L2StaticObjectInstance;

public class StaticObjects
{
	private static final Log _log = LogFactory.getLog(StaticObjects.class.getName());

	private static StaticObjects _instance;
	private ConcurrentMap<Integer, L2StaticObjectInstance> _staticObjects;

	public static StaticObjects getInstance()
	{
		if(_instance == null)
		{
			_instance = new StaticObjects();
		}

		return _instance;
	}

	public StaticObjects()
	{
		_staticObjects = new ConcurrentHashMap<Integer, L2StaticObjectInstance>();
		parseData();
		_log.info("StaticObject: Loaded " + _staticObjects.size() + " static object templates.");
	}

	private void parseData()
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		File f = new File(Config.DATAPACK_ROOT + "/data/xml/static_objects.xml");
		if(!f.exists())
		{
			_log.warn("static_objects.xml could not be loaded: file not found");
			return;
		}
		try
		{
			InputSource in = new InputSource(new InputStreamReader(new FileInputStream(f), "UTF-8"));
			in.setEncoding("UTF-8");
			Document doc = factory.newDocumentBuilder().parse(in);
			for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if(n.getNodeName().equalsIgnoreCase("list"))
				{
					for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if(d.getNodeName().equalsIgnoreCase("staticobject"))
						{
							int id = Integer.valueOf(d.getAttributes().getNamedItem("id").getNodeValue());
							int x = Integer.valueOf(d.getAttributes().getNamedItem("x").getNodeValue());
							int y = Integer.valueOf(d.getAttributes().getNamedItem("y").getNodeValue());
							int z = Integer.valueOf(d.getAttributes().getNamedItem("z").getNodeValue());
							int type = Integer.valueOf(d.getAttributes().getNamedItem("type").getNodeValue());
							String texture = String.valueOf(d.getAttributes().getNamedItem("texture").getNodeValue());
							int map_x = Integer.valueOf(d.getAttributes().getNamedItem("map_x").getNodeValue());
							int map_y = Integer.valueOf(d.getAttributes().getNamedItem("map_y").getNodeValue());

							L2StaticObjectInstance obj = new L2StaticObjectInstance(IdFactory.getInstance().getNextId());
							obj.setType(type);
							obj.setStaticObjectId(id);
							obj.setXYZ(x, y, z);
							obj.setMap(texture, map_x, map_y);
							obj.spawnMe();
							_staticObjects.put(obj.getStaticObjectId(), obj);
						}
					}
				}
			}
		}
		catch(SAXException e)
		{
			_log.error("error while creating StaticObjects table", e);
		}
		catch(IOException e)
		{
			_log.error("error while creating StaticObjects table", e);
		}
		catch(ParserConfigurationException e)
		{
			_log.error("error while creating StaticObjects table", e);
		}
	}
}