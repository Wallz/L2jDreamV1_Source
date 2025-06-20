/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.src.gameserver.model;

import java.util.List;

import javolution.util.FastList;

import com.src.gameserver.model.actor.instance.L2PcInstance;

public class PartyMatchWaitingList
{
	private List<L2PcInstance> _members;

	private PartyMatchWaitingList()
	{
		_members = new FastList<L2PcInstance>();
	}

	public void addPlayer(L2PcInstance player)
	{
		if(!_members.contains(player))
			_members.add(player);
		player.broadcastUserInfo();
	}

	public void removePlayer(L2PcInstance player)
	{
		if(_members.contains(player))
			_members.remove(player);
		player.broadcastUserInfo();
	}

	public List<L2PcInstance> getPlayers()
	{
		return _members;
	}

	public static PartyMatchWaitingList getInstance()
	{
		return SingletonHolder._instance;
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final PartyMatchWaitingList _instance = new PartyMatchWaitingList();
	}

}