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
package com.src.gameserver.datatables;

import javolution.util.FastList;
import javolution.util.FastMap;

import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.L2GameServerPacket;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class GmListTable
{
	private static GmListTable _instance;

	private FastMap<L2PcInstance, Boolean> _gmList;

	public static GmListTable getInstance()
	{
		if(_instance == null)
		{
			_instance = new GmListTable();
		}

		return _instance;
	}

	public FastList<L2PcInstance> getAllGms(boolean includeHidden)
	{
		FastList<L2PcInstance> tmpGmList = new FastList<L2PcInstance>();

		for(FastMap.Entry<L2PcInstance, Boolean> n = _gmList.head(), end = _gmList.tail(); (n = n.getNext()) != end;)
		{
			if(includeHidden || !n.getValue())
			{
				tmpGmList.add(n.getKey());
			}
		}
		return tmpGmList;
	}

	public FastList<String> getAllGmNames(boolean includeHidden)
	{
		FastList<String> tmpGmList = new FastList<String>();

		for(FastMap.Entry<L2PcInstance, Boolean> n = _gmList.head(), end = _gmList.tail(); (n = n.getNext()) != end;)
		{
			if(!n.getValue())
			{
				tmpGmList.add(n.getKey().getName());
			}
			else if(includeHidden)
			{
				tmpGmList.add(n.getKey().getName() + " (invis)");
			}
		}
		return tmpGmList;
	}

	private GmListTable()
	{
		_gmList = new FastMap<L2PcInstance, Boolean>().shared();
	}

	public void addGm(L2PcInstance player, boolean hidden)
	{
		_gmList.put(player, hidden);
	}

	public void deleteGm(L2PcInstance player)
	{
		_gmList.remove(player);
	}

	public void showGm(L2PcInstance player)
	{
		FastMap.Entry<L2PcInstance, Boolean> gm = _gmList.getEntry(player);

		if(gm != null)
		{
			gm.setValue(false);
		}
	}

	public void hideGm(L2PcInstance player)
	{
		FastMap.Entry<L2PcInstance, Boolean> gm = _gmList.getEntry(player);

		if(gm != null)
		{
			gm.setValue(true);
		}
	}

	public boolean isGmOnline(boolean includeHidden)
	{
		for(boolean b : _gmList.values())
		{
			if(includeHidden || !b)
			{
				return true;
			}
		}

		return false;
	}

	public void sendListToPlayer(L2PcInstance player)
	{
		if(isGmOnline(player.isGM()))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.GM_LIST));

			for(String name : getAllGmNames(player.isGM()))
			{
				player.sendPacket(new SystemMessage(SystemMessageId.GM_S1).addString(name));
			}
		}
		else
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NO_GM_PROVIDING_SERVICE_NOW));
		}
	}

	public static void broadcastToGMs(L2GameServerPacket packet)
	{
		for(L2PcInstance gm : getInstance().getAllGms(true))
		{
			gm.sendPacket(packet);
		}
	}

	public static void broadcastMessageToGMs(String message)
	{
		for(L2PcInstance gm : getInstance().getAllGms(true))
		{
			if(gm != null)
			{
				gm.sendPacket(SystemMessage.sendString(message));
			}
		}
	}
}