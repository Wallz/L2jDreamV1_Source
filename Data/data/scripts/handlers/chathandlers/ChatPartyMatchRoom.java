package handlers.chathandlers;

import com.src.gameserver.handler.IChatHandler;
import com.src.gameserver.model.PartyMatchRoom;
import com.src.gameserver.model.PartyMatchRoomList;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.CreatureSay;

public class ChatPartyMatchRoom implements IChatHandler
{
	private static final int[] COMMAND_IDS =
	{
		14
	};
	
	public void handleChat(int type, L2PcInstance activeChar, String target, String text)
	{
		if (activeChar.isInPartyMatchRoom())
		{
			PartyMatchRoom _room = PartyMatchRoomList.getInstance().getPlayerRoom(activeChar);
			if(_room != null)
			{
				CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
				for(L2PcInstance _member : _room.getPartyMembers())
				{
					_member.sendPacket(cs);
				}
			}
		}
	}

	public int[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
	
	public static void main(String[] args)
	{
		new ChatPartyMatchRoom();
	}
}