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
package com.src.gameserver.network;

public enum SystemChatChannelId
{
	CHAT_NORMAL("ALL"),
	CHAT_SHOUT("SHOUT"),
	CHAT_TELL("WHISPER"),
	CHAT_PARTY("PARTY"),
	CHAT_CLAN("CLAN"),
	CHAT_SYSTEM("EMOTE"),
	CHAT_USER_PET("USERPET"),
	CHAT_GM_PET("GMPET"),
	CHAT_MARKET("TRADE"),
	CHAT_ALLIANCE("ALLIANCE"),
	CHAT_ANNOUNCE("ANNOUNCE"),
	CHAT_CUSTOM("CRASH"),
	CHAT_L2FRIEND("L2FRIEND"),
	CHAT_MSN("MSN"),
	CHAT_PARTY_ROOM("PARTYROOM"),
	CHAT_COMMANDER("COMMANDER"),
	CHAT_INNER_PARTYMASTER("INNERPARTYMASTER"),
	CHAT_HERO("HERO"),
	CHAT_CRITICAL_ANNOUNCE("CRITANNOUNCE"),
	CHAT_UNKNOWN("UNKNOWN"),
	CHAT_BATTLEFIELD("BATTLEFIELD"),
	CHAT_NONE("NONE");

	private String _channelName;

	private SystemChatChannelId(String channelName)
	{
		_channelName = channelName;
	}

	public int getId()
	{
		return this.ordinal();
	}

	public String getName()
	{
		return _channelName;
	}

	public static SystemChatChannelId getChatType(int channelId)
	{
		for(SystemChatChannelId channel : SystemChatChannelId.values())
		{
			if(channel.getId() == channelId)
			{
				return channel;
			}
		}

		return SystemChatChannelId.CHAT_NONE;
	}

}