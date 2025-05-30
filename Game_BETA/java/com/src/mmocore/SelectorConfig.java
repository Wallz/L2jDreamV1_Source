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
package com.src.mmocore;

public final class SelectorConfig
{
	public int READ_BUFFER_SIZE = 64 * 1024;
	public int WRITE_BUFFER_SIZE = 64 * 1024;
	public int HELPER_BUFFER_COUNT = 20;
	public int HELPER_BUFFER_SIZE = 64 * 1024;
	public int MAX_SEND_PER_PASS = 10;
	public int MAX_READ_PER_PASS = 10;
	public int SLEEP_TIME = 10;
}