package com.src.gameserver.datatables.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.src.Config;
import com.src.gameserver.model.base.Experience;

public class ExperienceTable
{
	private final static Log _log = LogFactory.getLog(ExperienceTable.class.getName());

	private static ExperienceTable _instance = null;

	public ExperienceTable()
	{
		loadXMLData();
	}

	private void loadXMLData()
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);

		int level = 0;
		long exp = 0;

		File f = new File(Config.DATAPACK_ROOT + "/data/xml/experience.xml");

		if (!f.exists())
		{
			_log.error("[Error][experience.xml]: File doesn`t exist!");
			return;
		}
		try
		{
			InputSource in = new InputSource(new InputStreamReader(new FileInputStream(f), "UTF-8"));
			in.setEncoding("UTF-8");
			Document doc = factory.newDocumentBuilder().parse(in);
			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if (n.getNodeName().equalsIgnoreCase("list"))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if (d.getNodeName().equalsIgnoreCase("exp"))
						{
							level = Integer.valueOf(d.getAttributes().getNamedItem("level").getNodeValue());
							exp = Long.valueOf(d.getAttributes().getNamedItem("expAmmount").getNodeValue());
							Experience.setExp(level, exp);
						}
					}
				}
			}
		}
		catch (SAXException e)
		{
			_log.error("[Error][ExperienceTable.java]: " + e);
		}
		catch (IOException e)
		{
			_log.error("[Error][ExperienceTable.java]: " + e);
		}
		catch (ParserConfigurationException e)
		{
			_log.error("[Error][ExperienceTable.java]: " + e);
		}
	}

	public static ExperienceTable getInstance()
	{
		return _instance == null ? (_instance = new ExperienceTable()) : _instance;
	}
}