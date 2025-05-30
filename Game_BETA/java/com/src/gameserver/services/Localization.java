package com.src.gameserver.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.src.Config;

public class Localization
{
	private static final Logger _log = Logger.getLogger(Localization.class.getName());

	private HashMap<String, HashMap<Integer, String>> _sysmessages = new HashMap<String, HashMap<Integer, String>>();

	private Localization()
	{
		reload();
	}

	public String getString(String lang, Integer id)
	{
		if (lang == null)
			lang = "en";
		if (_sysmessages.get(lang) == null)
			return "";
		if (_sysmessages.get(lang).get(id) == null)
			return "";
		return _sysmessages.get(lang).get(id);
	}

	private void reload()
	{
		File dir = new File(Config.DATAPACK_ROOT, "data/localization/");
		for (File file : dir.listFiles())
		{
			if (file.isDirectory() && !file.isHidden())
			{
				String lang = file.getName();
				HashMap<Integer, String> map = new HashMap<Integer, String>();
				readFromDisk(map, lang);
				_sysmessages.put(lang, map);
			}
		}
	}

	private void readFromDisk(HashMap<Integer, String> map, String lang)
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		File f = new File(Config.DATAPACK_ROOT, "data/localization/"+lang+"/messages.xml");
		if(!f.exists()) {
			_log.warning("File not found, path: data/localization/"+lang+"/messages.xml");
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
						if(d.getNodeName().equalsIgnoreCase("message"))
						{
							int id = Integer.valueOf(d.getAttributes().getNamedItem("id").getNodeValue());
							String content = String.valueOf(d.getAttributes().getNamedItem("content").getNodeValue());
							map.put(id, content);
						}
					}
				}
			}
		}
		catch(SAXException e)
		{
			_log.warning("Localization: Error while creating table: " + e.getMessage() + "\n" + e);
		}
		catch(IOException e)
		{
			_log.warning("Localization: Error while creating table: " + e.getMessage() + "\n" + e);
		}
		catch(ParserConfigurationException e)
		{
			_log.warning("Localization: Error while creating table: " + e.getMessage() + "\n" + e);
		}
	}

	public static Localization getInstance()
	{
		return SingletonHolder._instance;
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final Localization _instance = new Localization();
	}

}
