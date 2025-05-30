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
package com.src.logs;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import javolution.text.TextBuilder;

import com.src.gameserver.model.actor.instance.L2ItemInstance;

public class ItemLogFormatter extends Formatter
{
	private static final String CRLF = "\r\n";
	private SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMM H:mm:ss");

	@Override
	public String format(LogRecord record)
	{
		Object[] params = record.getParameters();
		TextBuilder output = new TextBuilder();

		output.append("[" + dateFmt.format(new Date(record.getMillis())) + "] ");
		output.append(record.getMessage() + " ");

		if(params != null)
		{
			for(Object p : params)
			{
				if(p == null)
				{
					continue;
				}
				output.append("| ");
				if(p instanceof L2ItemInstance)
				{
					L2ItemInstance item = (L2ItemInstance) p;
					output.append("item " + item.getObjectId() + ": ");
					if(item.getEnchantLevel() > 0)
					{
						output.append("+" + item.getEnchantLevel() + " ");
					}
					output.append(item.getItem().getName() + "(" + item.getCount() + ")");
				}
				else
				{
					output.append(p.toString());
				}
			}
		}
		output.append(CRLF);
		return output.toString();
	}

}