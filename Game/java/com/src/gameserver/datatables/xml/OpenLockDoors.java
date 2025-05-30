package com.src.gameserver.datatables.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.src.Config;

/**
 * @author Matim
 * @version 1.0
 * <br><br>
 * Useful XML parser, parsing xml containing
 * ID`s od doors which should be locked/opened on GS startup.
 */
public class OpenLockDoors
{
	private final static Log _log = LogFactory.getLog(OpenLockDoors.class.getName());

	private Map<Integer, String> _openLockDoors = new FastMap<Integer, String>();

	private static OpenLockDoors _instance = null;

	public OpenLockDoors()
	{
		loadXMLData();
		openLockDoors();
	}

	private void loadXMLData()
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);

		String modifierFunction = null;
		int doorID = 0;

		File f = new File(Config.DATAPACK_ROOT + "/data/xml/modifiers/door_modifiers.xml");

		if (!f.exists())
		{
			_log.error("[Error][door_modifiers.xml]: File doesn`t exist!");
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
						if(d.getNodeName().equalsIgnoreCase("door"))
						{
							modifierFunction = String.valueOf(d.getAttributes().getNamedItem("function").getNodeValue());
							doorID = Integer.valueOf(d.getAttributes().getNamedItem("doorID").getNodeValue());
							_openLockDoors.put(doorID, modifierFunction);
						}
					}
				}
			}
		}
		catch (SAXException e)
		{
			_log.error("[Error][OpenLockDoors.java]: " + e);
		}
		catch (IOException e)
		{
			_log.error("[Error][OpenLockDoors.java]: " + e);
		}
		catch (ParserConfigurationException e)
		{
			_log.error("[Error][OpenLockDoors.java]: " + e);
		}
	}

	private void openLockDoors()
	{
		DoorTable doorTable = DoorTable.getInstance();

		for (Integer doorID : _openLockDoors.keySet())
		{
		    String value = _openLockDoors.get(doorID);

		    if (value.equals("open"))
		    {
		    	doorTable.getDoor(doorID).openMe();
		    }
		    else
		    {
		    	doorTable.getDoor(doorID).closeMe();
		    }
		}

		doorTable.checkAutoOpen();
	}

	public static OpenLockDoors getInstance()
	{
		return _instance == null ? (_instance = new OpenLockDoors()) : _instance;
	}
}