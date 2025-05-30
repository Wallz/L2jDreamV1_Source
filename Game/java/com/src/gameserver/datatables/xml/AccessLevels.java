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
import com.src.gameserver.datatables.AccessLevel;

public class AccessLevels
{
	private final static Log _log = LogFactory.getLog(AccessLevels.class.getName());

	public static final int _masterAccessLevelNum = Config.GM_ACCESS_LEVEL;
	
	private static AccessLevels _instance = null;
	
	public static final int _userAccessLevelNum = 0;
	public static AccessLevel _masterAccessLevel = new AccessLevel(_masterAccessLevelNum, "Master Access", Config.GM_NAME_COLOR, Config.GM_TITLE_COLOR, true, true, true, true, true, true, true, true, true, true, true);
	public static AccessLevel _userAccessLevel = new AccessLevel(_userAccessLevelNum, "User", Integer.decode("0xFFFFFF"), Integer.decode("0xFFFFFF"), false, false, false, true, false, true, true, true, true, true, false);

	private FastMap<Integer, AccessLevel> _accessLevels = new FastMap<Integer, AccessLevel>();

	private AccessLevels()
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		File f = new File(Config.DATAPACK_ROOT + "/data/xml/access_levels.xml");
		if(!f.exists())
		{
			_log.error("access_levels.xml could not be loaded: file not found");
			return;
		}
		try
		{
			int accessLevel = 0;
			String name = null;
			int nameColor = 0;
			int titleColor = 0;
			boolean isGm = false;
			boolean allowPeaceAttack = false;
			boolean allowFixedRes = false;
			boolean allowTransaction = false;
			boolean allowAltG = false;
			boolean giveDamage = false;
			boolean takeAggro = false;
			boolean gainExp = false;
			boolean useNameColor = true;
			boolean useTitleColor = false;
			boolean canDisableGmStatus = true;

			InputSource in = new InputSource(new InputStreamReader(new FileInputStream(f), "UTF-8"));
			in.setEncoding("UTF-8");
			Document doc = factory.newDocumentBuilder().parse(in);
			for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if(n.getNodeName().equalsIgnoreCase("list"))
				{
					for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if(d.getNodeName().equalsIgnoreCase("acessLevel"))
						{
							accessLevel = Integer.valueOf(d.getAttributes().getNamedItem("level").getNodeValue());
							name = String.valueOf(d.getAttributes().getNamedItem("name").getNodeValue());

							if(accessLevel == _userAccessLevelNum)
							{
								//_log.warn("AccessLevels: Access level with name " + name + " is using reserved user access level " + _userAccessLevelNum + ". Ignoring it!");
								continue;
							}
							else if(accessLevel == _masterAccessLevelNum)
							{
								//_log.warn("AccessLevels: Access level with name " + name + " is using reserved master access level " + _masterAccessLevelNum + ". Ignoring it!");
								continue;
							}
							else if(accessLevel < 0)
							{
								//_log.warn("AccessLevels: Access level with name " + name + " is using banned access level state(below 0). Ignoring it!");
								continue;
							}

							try
							{
								nameColor = Integer.decode("0x" + String.valueOf(d.getAttributes().getNamedItem("nameColor").getNodeValue()));
							}
							catch(NumberFormatException nfe)
							{
								try
								{
									nameColor = Integer.decode("0xFFFFFF");
								}
								catch(NumberFormatException nfe2)
								{}
							}

							try
							{
								titleColor = Integer.decode("0x" + String.valueOf(d.getAttributes().getNamedItem("titleColor").getNodeValue()));

							}
							catch(NumberFormatException nfe)
							{
								try
								{
									titleColor = Integer.decode("0x77FFFF");
								}
								catch(NumberFormatException nfe2)
								{}
							}

							isGm = Boolean.valueOf(d.getAttributes().getNamedItem("isGm").getNodeValue());
							allowPeaceAttack = Boolean.valueOf(d.getAttributes().getNamedItem("allowPeaceAttack").getNodeValue());
							allowFixedRes = Boolean.valueOf(d.getAttributes().getNamedItem("allowFixedRes").getNodeValue());
							allowTransaction = Boolean.valueOf(d.getAttributes().getNamedItem("allowTransaction").getNodeValue());
							allowAltG = Boolean.valueOf(d.getAttributes().getNamedItem("allowAltg").getNodeValue());
							giveDamage = Boolean.valueOf(d.getAttributes().getNamedItem("giveDamage").getNodeValue());
							takeAggro = Boolean.valueOf(d.getAttributes().getNamedItem("takeAggro").getNodeValue());
							gainExp = Boolean.valueOf(d.getAttributes().getNamedItem("gainExp").getNodeValue());
							useNameColor = Boolean.valueOf(d.getAttributes().getNamedItem("useNameColor").getNodeValue());
							useTitleColor = Boolean.valueOf(d.getAttributes().getNamedItem("useTitleColor").getNodeValue());
							canDisableGmStatus = Boolean.valueOf(d.getAttributes().getNamedItem("canDisableGmStatus").getNodeValue());

							_accessLevels.put(accessLevel, new AccessLevel(accessLevel, name, nameColor, titleColor, isGm, allowPeaceAttack, allowFixedRes, allowTransaction, allowAltG, giveDamage, takeAggro, gainExp, useNameColor, useTitleColor, canDisableGmStatus));
						}
					}
				}
			}
		}
		catch(SAXException e)
		{
			_log.error("Error while loading admin command data: ", e);
		}
		catch(IOException e)
		{
			_log.error("Error while loading admin command data: ", e);
		}
		catch(ParserConfigurationException e)
		{
			_log.error("Error while loading admin command data: ", e);
		}

		_log.info("AccessLevels: Loaded " + _accessLevels.size() + " access from database.");
	}

	public static AccessLevels getInstance()
	{
		return _instance == null ? (_instance = new AccessLevels()) : _instance;
	}

	public AccessLevel getAccessLevel(int accessLevelNum)
	{
		AccessLevel accessLevel = null;

		synchronized(_accessLevels)
		{
			accessLevel = _accessLevels.get(accessLevelNum);
		}

		return accessLevel;
	}

	public void addBanAccessLevel(int accessLevel)
	{
		synchronized (_accessLevels)
		{
			if(accessLevel > -1)
			{
				return;
			}

			_accessLevels.put(accessLevel, new AccessLevel(accessLevel, "Banned", Integer.decode("0x000000"), Integer.decode("0x000000"), false, false, false, false, false, false, false, false, false, false, false));
		}
	}
}