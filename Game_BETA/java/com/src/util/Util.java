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
package com.src.util;

import java.io.PrintWriter;
import java.io.StringWriter;

import javolution.text.TextBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.util.object.Memory;

public class Util
{
	private final static Log _log = LogFactory.getLog(Util.class.getName());

	public static String printData(byte[] data, int len)
	{
		TextBuilder result = new TextBuilder();

		int counter = 0;

		for(int i = 0; i < len; i++)
		{
			if(counter % 16 == 0)
			{
				result.append(fillHex(i, 4) + ": ");
			}

			result.append(fillHex(data[i] & 0xff, 2) + " ");
			counter++;
			if(counter == 16)
			{
				result.append("   ");

				int charpoint = i - 15;
				for(int a = 0; a < 16; a++)
				{
					int t1 = data[charpoint++];
					if(t1 > 0x1f && t1 < 0x80)
					{
						result.append((char) t1);
					}
					else
					{
						result.append('.');
					}
				}

				result.append("\n");
				counter = 0;
			}
		}

		int rest = data.length % 16;
		if(rest > 0)
		{
			for(int i = 0; i < 17 - rest; i++)
			{
				result.append("   ");
			}

			int charpoint = data.length - rest;
			for(int a = 0; a < rest; a++)
			{
				int t1 = data[charpoint++];
				if(t1 > 0x1f && t1 < 0x80)
				{
					result.append((char) t1);
				}
				else
				{
					result.append('.');
				}
			}

			result.append("\n");
		}

		return result.toString();
	}

	public static String fillHex(int data, int digits)
	{
		String number = Integer.toHexString(data);

		for(int i = number.length(); i < digits; i++)
		{
			number = "0" + number;
		}

		return number;
	}

	public static void printSection(String s)
	{
		int maxlength = 79;
		s = "=( " + s + " )=-";
		int slen = s.length();
		if(slen > maxlength)
		{
			System.out.println(s);
			return;
		}
		int i;
		for(i = 0; i < maxlength - slen; i++)
		{
			s = "-" + s;
		}
		System.out.println(s);
	}

	public static String printData(byte[] raw)
	{
		return printData(raw, raw.length);
	}

	public static String getStackTrace(Throwable t)
	{
		StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
	
	public static int convertMinutesToMiliseconds(int minutesToConvert)
	{
		return minutesToConvert * 60000;
	}

	/**
	 * Print L2Dream logo
	 */
	public static void team()
	{
		_log.info("              		L2J Dream Interlude Project			               ");
		_log.info("            ##### ##                                                    ");
        _log.info("         /#####  /##                                                    ");
        _log.info("       //    /  / ###                                                   ");
        _log.info("      /     /  /   ###                                                  ");
        _log.info("           /  /     ###                                                 ");
        _log.info("          ## ##      ## ###  /###     /##       /###   ### /### /###    ");
        _log.info("          ## ##      ##  ###/ #### / / ###     / ###  / ##/ ###/ /##  / ");
        _log.info("          ## ##      ##   ##   ###/ /   ###   /   ###/   ##  ###/ ###/  ");
        _log.info("          ## ##      ##   ##       ##    ### ##    ##    ##   ##   ##   ");
        _log.info("          ## ##      ##   ##       ########  ##    ##    ##   ##   ##   ");
        _log.info("          #  ##      ##   ##       #######   ##    ##    ##   ##   ##   ");
        _log.info("             /       /    ##       ##        ##    ##    ##   ##   ##   ");
        _log.info("        /###/       /     ##       ####    / ##    /#    ##   ##   ##   ");
        _log.info("       /   ########/      ###       ######/   ####/ ##   ###  ###  ###  ");
        _log.info("      /       ####         ###       #####     ###   ##   ###  ###  ### ");
        _log.info("      #                                                                 ");
        _log.info("       ##                                                               ");
		_log.info("                                                                        ");
		_log.info("                              				                           ");
	}

	/**
	 * Print basic info about:
	 * <li>Avaible CPUs</li>
	 * <li>Operation System</li>
	 * <li>Free Memory</li>
	 * <li>Maximum Online</li>
	 */
	public static void info()
	{
		_log.info("Processors Identifier: " + System.getenv("PROCESSOR_IDENTIFIER"));
		_log.info("Avaible CPU(s): " + Runtime.getRuntime().availableProcessors());
		_log.info("OS: " + System.getProperty("os.name") + " Build: " + System.getProperty("os.version") + " Arch: " + System.getProperty("os.arch"));
		_log.info("Memory: Free " + Memory.getFreeMemory() + " MB of " + Memory.getTotalMemory() + " MB. Used " + Memory.getUsedMemory() + " MB."); 
		_log.info("Maximum Online: " + Config.MAXIMUM_ONLINE_USERS);
	}

}