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


public class Config
{
	public static int MMO_SELECTOR_SLEEP_TIME = 20;
	public static int MMO_MAX_SEND_PER_PASS = 12;
	public static int MMO_MAX_READ_PER_PASS = 12;
	public static int MMO_HELPER_BUFFER_COUNT = 20;
	public static int CLIENT_PACKET_QUEUE_SIZE = 14;
	public static int CLIENT_PACKET_QUEUE_MAX_BURST_SIZE = 13;
	public static int CLIENT_PACKET_QUEUE_MAX_PACKETS_PER_SECOND = 80;
	public static int CLIENT_PACKET_QUEUE_MEASURE_INTERVAL = 5;
	public static int CLIENT_PACKET_QUEUE_MAX_AVERAGE_PACKETS_PER_SECOND = 40;
	public static int CLIENT_PACKET_QUEUE_MAX_FLOODS_PER_MIN = 2;
	public static int CLIENT_PACKET_QUEUE_MAX_OVERFLOWS_PER_MIN = 1;
	public static int CLIENT_PACKET_QUEUE_MAX_UNDERFLOWS_PER_MIN = 1;
	public static int CLIENT_PACKET_QUEUE_MAX_UNKNOWN_PER_MIN = 5;

	private static Config _instance;

	public static Config getInstance()
	{
		if(_instance == null)
		{
			_instance = new Config();
		}

		return _instance;
	}

}