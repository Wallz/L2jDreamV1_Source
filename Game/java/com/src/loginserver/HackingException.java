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
package com.src.loginserver;

public class HackingException extends Exception
{
	private static final long serialVersionUID = 4050762693478463029L;
	String _ip;
	private int _connects;

	public HackingException(String ip, int connects)
	{
		_ip = ip;
		_connects = connects;
	}

	public String getIP()
	{
		return _ip;
	}

	public int getConnects()
	{
		return _connects;
	}

}