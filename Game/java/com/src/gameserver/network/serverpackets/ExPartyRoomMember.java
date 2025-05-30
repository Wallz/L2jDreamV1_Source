package com.src.gameserver.network.serverpackets;

import com.src.gameserver.managers.TownManager;
import com.src.gameserver.model.PartyMatchRoom;
import com.src.gameserver.model.actor.instance.L2PcInstance;

/**
 * Format:(ch) d d [dsdddd]
 */
public class ExPartyRoomMember extends L2GameServerPacket
{
	private final PartyMatchRoom _room;
	private final int _mode;

	public ExPartyRoomMember(L2PcInstance player, PartyMatchRoom room, int mode)
	{
		_room = room;
		_mode = mode;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x0E);

		writeD(_mode);
		writeD(_room.getMembers());
		for(L2PcInstance _member : _room.getPartyMembers())
		{
			writeD(_member.getObjectId());
			writeS(_member.getName());
			writeD(_member.getActiveClass());
			writeD(_member.getLevel());
			writeD(TownManager.getClosestLocation(_member));
			if(_room.getOwner().equals(_member))
				writeD(1);
			else
			{
				if((_room.getOwner().isInParty() && _member.isInParty()) && (_room.getOwner().getParty().getPartyLeaderOID() == _member.getParty().getPartyLeaderOID()))
					writeD(2);
				else
					writeD(0);
			}
		}
	}

	@Override
	public String getType()
	{
		return "[S] FE:0E ExPartyRoomMember";
	}
}