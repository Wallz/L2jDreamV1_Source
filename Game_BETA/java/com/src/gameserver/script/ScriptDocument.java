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
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class ScriptDocument
{
	private final static Log _log = LogFactory.getLog(ScriptDocument.class);

	private Document _document;
	private String _name;

	public ScriptDocument(String name, InputStream input)
	{
		_name = name;

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try
		{
			DocumentBuilder builder = factory.newDocumentBuilder();
			_document = builder.parse(input);

		}
		catch(SAXException e)
		{
			_log.error("", e);
		}
		catch(ParserConfigurationException e)
		{
			_log.error("", e);
		}
		catch(IOException e)
		{
			_log.error("", e);
		}
	}

	public Document getDocument()
	{
		return _document;
	}

	public String getName()
	{
		return _name;
	}

	@Override
	public String toString()
	{
		return _name;
	}

}