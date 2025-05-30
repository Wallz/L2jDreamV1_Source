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
package com.src.gameserver.util;

import java.util.logging.Logger;

import javolution.util.FastMap;
import javolution.util.FastMap.Entry;

import com.src.Config;
import com.src.gameserver.GameTimeController;

public class FloodProtector
{
	private static final Logger _log = Logger.getLogger(FloodProtector.class.getName());
	private static FloodProtector _instance;
	private static FastMap<Integer, Integer[]> _floodClient;

	public static final FloodProtector getInstance()
	{
		if(_instance == null)
		{
			_instance = new FloodProtector();
		}

		return _instance;
	}

	private static final int[] REUSEDELAY = new int[]
	{
			Config.PROTECTED_BYPASS_C,
			42,
			42,
			Config.PROTECTED_HEROVOICE_C,
			Config.PROTECTED_MULTISELL_C,
			Config.PROTECTED_SUBCLASS_C,
			Config.PROTECTED_UNKNOWNPACKET_C,
			Config.PROTECTED_GLOBAL_CHAT_C,
			Config.PROTECTED_PARTY_ADD_MEMBER_C,
			Config.PROTECTED_DROP_C,
			Config.PROTECTED_ENCHANT_C,
			Config.PROTECTED_BANKING_SYSTEM_C,
			Config.PROTECTED_WEREHOUSE_C,
			Config.PROTECTED_CRAFT_C,
			Config.PROTECTED_ACTIVE_PACK_RETURN,
			Config.PROTECTED_ACTIVE_PACK_FAILED,
			Config.PROTECTED_USE_ITEM_C,
			Config.PROTECTED_BUY_PROCURE_C,
			Config.PROTECTED_BUY_SEED_C
	};

	public static final int PROTECTED_BYPASS = 0;
	public static final int PROTECTED_ROLLDICE = 1;
	public static final int PROTECTED_FIREWORK = 2;
	public static final int PROTECTED_HEROVOICE = 3;
	public static final int PROTECTED_MULTISELL = 4;
	public static final int PROTECTED_SUBCLASS = 5;
	public static final int PROTECTED_UNKNOWNPACKET = 6;
	public static final int PROTECTED_GLOBALCHAT = 7;
	public static final int PROTECTED_PARTY_ADD_MEMBER = 8;
	public static final int PROTECTED_DROP = 9;
	public static final int PROTECTED_ENCHANT = 10;
	public static final int PROTECTED_BANKING_SYSTEM = 11;
	public static final int PROTECTED_WEREHOUSE = 12;
	public static final int PROTECTED_CRAFT = 13;
	public static final int PROTECTED_ACTIVE_PACKETS = 14;
	public static final int PROTECTED_ACTIVE_PACKETS2 = 15;
	public static final int PROTECTED_USE_ITEM = 16;
	public static final int PROTECTED_BUY_PROCURE = 17;
	public static final int PROTECTED_BUY_SEED = 18;

	private FloodProtector()
	{
		_log.info("[Flood Protection] Active");
		_floodClient = new FastMap<Integer, Integer[]>(Config.FLOODPROTECTOR_INITIALSIZE).shared();
	}

	public void registerNewPlayer(int playerObjId)
	{
		Integer[] array = new Integer[REUSEDELAY.length];
		for(int i = 0; i < array.length; i++)
		{
			array[i] = 0;
		}

		_floodClient.put(playerObjId, array);
	}

	public void removePlayer(int playerObjId)
	{
		_floodClient.remove(playerObjId);
	}

	public int getSize()
	{
		return _floodClient.size();
	}

	public boolean tryPerformAction(int playerObjId, int action)
	{
		Entry<Integer, Integer[]> entry = _floodClient.getEntry(playerObjId);

		if(entry != null)
		{
			Integer[] value = entry.getValue();

			if(value[action] < GameTimeController.getGameTicks())
			{
				value[action] = GameTimeController.getGameTicks() + REUSEDELAY[action];
				entry.setValue(value);

				return true;
			}
		}
		return false;
	}
}