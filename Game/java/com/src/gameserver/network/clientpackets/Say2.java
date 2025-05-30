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
 * 02111-1307, USA.ó
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package com.src.gameserver.network.clientpackets;

import java.nio.BufferUnderflowException;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.src.Config;
import com.src.gameserver.handler.ChatHandler;
import com.src.gameserver.handler.IChatHandler;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance.PunishLevel;
import com.src.gameserver.network.SystemChatChannelId;
import com.src.gameserver.network.serverpackets.SocialAction;
import com.src.gameserver.util.Util;

public final class Say2 extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(Say2.class.getName());
	private static Logger _logChat = Logger.getLogger("chat");

	private static final String _C__38_SAY2 = "[C] 38 Say2";

	public final static int ALL = 0;
	public final static int SHOUT = 1;
	public final static int TELL = 2;
	public final static int PARTY = 3;
	public final static int CLAN = 4;
	public final static int GM = 5;
	public final static int PETITION_PLAYER = 6;
	public final static int PETITION_GM = 7;
	public final static int TRADE = 8;
	public final static int ALLIANCE = 9;
	public final static int ANNOUNCEMENT = 10;
	public final static int BOAT = 11;
	public final static int L2FRIEND = 12;
	public final static int MSNCHAT = 13;
	public final static int PARTYMATCH_ROOM = 14;
	public final static int PARTYROOM_COMMANDER = 15;
	public final static int PARTYROOM_ALL = 16;
	public final static int HERO_VOICE = 17;

	private final static String[] CHAT_NAMES =
	{
			"ALL", "SHOUT", "TELL", "PARTY", "CLAN", "GM", "PETITION_PLAYER", "PETITION_GM", "TRADE", "ALLIANCE", "ANNOUNCEMENT",
			"WILLCRASHCLIENT:)",
			"FAKEALL?",
			"FAKEALL?",
			"FAKEALL?",
			"PARTYROOM_ALL",
			"PARTYROOM_COMMANDER",
			"HERO_VOICE"
	};

	private static final String[] WALKER_COMMAND_LIST =
	{
			"USESKILL",
			"USEITEM",
			"BUYITEM",
			"SELLITEM",
			"SAVEITEM",
			"LOADITEM",
			"MSG",
			"SET",
			"DELAY",
			"LABEL",
			"JMP",
			"CALL",
			"RETURN",
			"MOVETO",
			"NPCSEL",
			"NPCDLG",
			"DLGSEL",
			"CHARSTATUS",
			"POSOUTRANGE",
			"POSINRANGE",
			"GOHOME",
			"SAY",
			"EXIT",
			"PAUSE",
			"STRINDLG",
			"STRNOTINDLG",
			"CHANGEWAITTYPE",
			"FORCEATTACK",
			"ISMEMBER",
			"REQUESTJOINPARTY",
			"REQUESTOUTPARTY",
			"QUITPARTY",
			"MEMBERSTATUS",
			"CHARBUFFS",
			"ITEMCOUNT",
			"FOLLOWTELEPORT"
	};

	private final static HashSet<String> WALKER_COMMAND_LIST_SET = new HashSet<String>(301);
	private String _text;
	private int _type;
	private SystemChatChannelId _type2Check;
	private String _target;
	static
      {
         for (String WalkerCmdlist : WALKER_COMMAND_LIST)
            WALKER_COMMAND_LIST_SET.add(WalkerCmdlist);
      }

	@Override
	protected void readImpl()
	{
		_text = readS();
		try
		{
			_type = readD();
			_type2Check = SystemChatChannelId.getChatType(_type);
		}
		catch(BufferUnderflowException e)
		{
			_type = CHAT_NAMES.length;
		}

		_target = (_type == TELL) ? readS() : null;
	}

	@Override
	protected void runImpl()
	{
		if(_type < 0 || _type >= CHAT_NAMES.length)
		{
			_log.warning("Say2: Invalid type: " + _type);
			return;
		}

		L2PcInstance activeChar = getClient().getActiveChar();

		if(_type2Check == SystemChatChannelId.CHAT_NONE || _type2Check == SystemChatChannelId.CHAT_ANNOUNCE || _type2Check == SystemChatChannelId.CHAT_CRITICAL_ANNOUNCE || _type2Check == SystemChatChannelId.CHAT_SYSTEM || _type2Check == SystemChatChannelId.CHAT_CUSTOM || (_type2Check == SystemChatChannelId.CHAT_GM_PET && !activeChar.isGM()))
		{
			_log.warning("[Anti-PHX Announce] Illegal Chat channel was used by character: [" + activeChar.getName() + "]");
			return;
		}

		if(activeChar == null)
		{
			return;
		}

		if(Config.SAY_SOCIAL)
		{
			if((_text.equalsIgnoreCase("hello") || _text.equalsIgnoreCase("hey") || _text.equalsIgnoreCase("aloha") || _text.equalsIgnoreCase("alo") || _text.equalsIgnoreCase("ciao") || _text.equalsIgnoreCase("hi")) && (!activeChar.isRunning() || !activeChar.isAttackingNow() || !activeChar.isCastingNow()))
			{
				if(activeChar.isAlikeDead())
				{
					return;
				}

				if(_type == ALL)
				{
					activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 2));
				}
			}

			if((_text.equalsIgnoreCase("lol") || _text.equalsIgnoreCase("haha") || _text.equalsIgnoreCase("xaxa") || _text.equalsIgnoreCase("ghgh") || _text.equalsIgnoreCase("jaja")) && (!activeChar.isRunning() || !activeChar.isAttackingNow() || !activeChar.isCastingNow()))
			{
				if(activeChar.isAlikeDead())
				{
					return;
				}

				if(_type == ALL)
				{
					activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 10));
				}
			}

			if((_text.equalsIgnoreCase("yes") || _text.equalsIgnoreCase("si") || _text.equalsIgnoreCase("yep")) && (!activeChar.isRunning() || !activeChar.isAttackingNow() || !activeChar.isCastingNow()))
			{
				if(activeChar.isAlikeDead())
				{
					return;
				}

				if(_type == ALL)
				{
					activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 6));
				}
			}

			if((_text.equalsIgnoreCase("no") || _text.equalsIgnoreCase("nop") || _text.equalsIgnoreCase("nope")) && (!activeChar.isRunning() || !activeChar.isAttackingNow() || !activeChar.isCastingNow()))
			{
				if(activeChar.isAlikeDead())
				{
					return;
				}

				if(_type == ALL)
				{
					activeChar.broadcastPacket(new SocialAction(activeChar.getObjectId(), 5));
				}
			}
		}

		if(activeChar.isCursedWeaponEquiped() && (_type == TRADE || _type == SHOUT))
		{
			activeChar.sendMessage("Shout and trade chatting cannot be used while possessing a cursed weapon.");
			return;
		}

		if(activeChar.isChatBanned() && !activeChar.isGM())
		{
			activeChar.sendMessage("You may not chat while a chat ban is in effect.");
			return;
		}

		if(!activeChar.isGM() && _type == ANNOUNCEMENT)
		{
			activeChar.sendMessage("You may not announce anything without rights!");
			return;
		}

		if(activeChar.isInJail() && Config.JAIL_DISABLE_CHAT && !activeChar.isGM())
		{
			if(_type == TELL || _type == SHOUT || _type == TRADE || _type == HERO_VOICE)
			{
				activeChar.sendMessage("You can't chat with players outside of the jail.");
				return;
			}
		}

		if(_type == PETITION_PLAYER && activeChar.isGM())
		{
			_type = PETITION_GM;
		}

		if(Config.L2WALKER_PROTEC && _type == TELL && checkBot(_text))
		{
			Util.handleIllegalPlayerAction(activeChar, "Client Emulator Detect: Player " + activeChar.getName() + " using l2walker.", Config.DEFAULT_PUNISH);
			return;
		}
		String WalkerCmdlist = "";
		if (Config.L2WALKER_PROTEC && _type == TELL && WALKER_COMMAND_LIST_SET.contains(WalkerCmdlist))
			_text = _text.replaceAll("\\\\n", "");
			
		if(_text.length() > Config.MAX_CHAT_LENGTH)
		{
			_text = _text.substring(0, Config.MAX_CHAT_LENGTH);
		}

		if(Config.LOG_CHAT)
		{
			LogRecord record = new LogRecord(Level.INFO, _text);
			record.setLoggerName("chat");

			if(_type == TELL)
			{
				record.setParameters(new Object[]
				{
						CHAT_NAMES[_type], "[" + activeChar.getName() + " to " + _target + "]"
				});
			}
			else
			{
				record.setParameters(new Object[]
				{
						CHAT_NAMES[_type], "[" + activeChar.getName() + "]"
				});
			}

			_logChat.log(record);
		}

		_text = _text.replaceAll("\\\\n", "");

		if(Config.USE_SAY_FILTER)
		{
			checkText(activeChar);
		}

		IChatHandler handler = ChatHandler.getInstance().getChatHandler(_type);
	 	if(handler != null)
		{
			handler.handleChat(_type, activeChar, _target, _text);
		}
	}

	private void checkText(L2PcInstance activeChar)
	{
		if(Config.USE_SAY_FILTER)
		{
			String filteredText = _text.toLowerCase();

			for(String pattern : Config.FILTER_LIST)
			{
				filteredText = filteredText.replaceAll("(?i)" + pattern, Config.CHAT_FILTER_CHARS);
			}

			if(!filteredText.equalsIgnoreCase(_text))
			{
				if(Config.CHAT_FILTER_PUNISHMENT.equalsIgnoreCase("chat"))
				{
					activeChar.setPunishLevel(PunishLevel.CHAT, Config.CHAT_FILTER_PUNISHMENT_PARAM1);
					activeChar.sendMessage("Administrator banned you chat from " + Config.CHAT_FILTER_PUNISHMENT_PARAM1 + " minutes");
				}
				else if(Config.CHAT_FILTER_PUNISHMENT.equalsIgnoreCase("karma"))
				{
					activeChar.setKarma(Config.CHAT_FILTER_PUNISHMENT_PARAM2);
					activeChar.sendMessage("You have get " + Config.CHAT_FILTER_PUNISHMENT_PARAM2 + " karma for bad words");
				}
				else if(Config.CHAT_FILTER_PUNISHMENT.equalsIgnoreCase("jail"))
				{
					activeChar.setPunishLevel(L2PcInstance.PunishLevel.JAIL, Config.DEFAULT_PUNISH_PARAM);
				}
				_text = filteredText;
			}
		}
	}

	private boolean checkBot(String text)
	{
		for(String botCommand : WALKER_COMMAND_LIST)
		{
			if(text.startsWith(botCommand))
			{
				return true;
			}
		}

		return false;
	}

	@Override
	public String getType()
	{
		return _C__38_SAY2;
	}

}