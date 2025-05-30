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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

public class Log
{
	private static final Logger _log = Logger.getLogger(Log.class.getName());

	public static final void add(String text, String cat)
	{
		String date = new SimpleDateFormat("yy.MM.dd H:mm:ss").format(new Date());

		new File("log/game").mkdirs();

		try
		{
			File file = new File("log/game/" + (cat != null ? cat : "_all") + ".txt");

			FileWriter save = new FileWriter(file, true);
			String out = "[" + date + "] '---': " + text + "\n";
			save.write(out);
			save.flush();
			save.close();
			save = null;
			file = null;
			out = null;
		}
		catch(IOException e)
		{
			_log.warning("saving chat log failed: " + e);
			e.printStackTrace();
		}

		if(cat != null)
		{
			add(text, null);
		}

		date = null;
	}

	public static final void Assert(boolean exp, String cmt)
	{
		if(exp)
		{
			return;
		}

		_log.info("Assertion error [" + cmt + "]");
		Thread.dumpStack();
	}

}