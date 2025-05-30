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
package com.src.gameserver.model;

import java.util.List;

import javolution.util.FastList;

import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.instance.L2GrandBossInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2RaidBossInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ExCloseMPCC;
import com.src.gameserver.network.serverpackets.ExOpenMPCC;
import com.src.gameserver.network.serverpackets.L2GameServerPacket;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class L2CommandChannel
{
	private List<L2Party> _partys = null;
	private L2PcInstance _commandLeader = null;
	private int _channelLvl;

	public L2CommandChannel(L2PcInstance leader)
	{
		_commandLeader = leader;
		_partys = new FastList<L2Party>();
		_partys.add(leader.getParty());
		_channelLvl = leader.getParty().getLevel();
		leader.getParty().setCommandChannel(this);
		leader.getParty().broadcastToPartyMembers(new ExOpenMPCC());
	}

	public void addParty(L2Party party)
	{
		if(party == null)
		{
			return;
		}

		_partys.add(party);

		if(party.getLevel() > _channelLvl)
		{
			_channelLvl = party.getLevel();
		}

		party.setCommandChannel(this);
		party.broadcastToPartyMembers(new SystemMessage(SystemMessageId.JOINED_COMMAND_CHANNEL));
		party.broadcastToPartyMembers(new ExOpenMPCC());
	}

	public void removeParty(L2Party party)
	{
		if(party == null)
		{
			return;
		}

		_partys.remove(party);
		_channelLvl = 0;

		for(L2Party pty : _partys)
		{
			if(pty.getLevel() > _channelLvl)
			{
				_channelLvl = pty.getLevel();
			}
		}

		party.setCommandChannel(null);
		party.broadcastToPartyMembers(new ExCloseMPCC());

		if(_partys.size() < 2)
		{
			broadcastToChannelMembers(new SystemMessage(SystemMessageId.COMMAND_CHANNEL_DISBANDED));
			disbandChannel();
		}
	}

	public void disbandChannel()
	{
		if(_partys != null)
		{
			for(L2Party party : _partys)
			{
				if(party != null)
				{
					removeParty(party);
				}
			}
		}

		_partys.clear();
	}

	public int getMemberCount()
	{
		int count = 0;

		for(L2Party party : _partys)
		{
			if(party != null)
			{
				count += party.getMemberCount();
			}
		}
		return count;
	}

	public void broadcastToChannelMembers(L2GameServerPacket gsp)
	{
		if(_partys != null && !_partys.isEmpty())
		{
			for(L2Party party : _partys)
			{
				if(party != null)
				{
					party.broadcastToPartyMembers(gsp);
				}
			}
		}
	}

	public List<L2Party> getPartys()
	{
		return _partys;
	}

	public List<L2PcInstance> getMembers()
	{
		List<L2PcInstance> members = new FastList<L2PcInstance>();
		for(L2Party party : getPartys())
		{
			members.addAll(party.getPartyMembers());
		}

		return members;
	}

	public int getLevel()
	{
		return _channelLvl;
	}

	public void setChannelLeader(L2PcInstance leader)
	{
		_commandLeader = leader;
	}

	public L2PcInstance getChannelLeader()
	{
		return _commandLeader;
	}

	public boolean meetRaidWarCondition(L2Object obj)
	{
		if(!(obj instanceof L2RaidBossInstance) || !(obj instanceof L2GrandBossInstance))
		{
			return false;
		}

		int npcId = ((L2Attackable) obj).getNpcId();

		switch(npcId)
		{
			case 29001:
			case 29006:
			case 29014:
			case 29022:
				return getMemberCount() > 36;
			case 29020:
				return getMemberCount() > 56;
			case 29019:
				return getMemberCount() > 225;
			case 29028:
				return getMemberCount() > 99;
			default:
				return getMemberCount() > 18;
		}
	}

}