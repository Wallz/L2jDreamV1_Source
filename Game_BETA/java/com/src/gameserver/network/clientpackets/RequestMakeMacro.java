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
package com.src.gameserver.network.clientpackets;

import com.src.gameserver.model.L2Macro;
import com.src.gameserver.model.L2Macro.L2MacroCmd;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;

public final class RequestMakeMacro extends L2GameClientPacket
{
	private static final String _C__C1_REQUESTMAKEMACRO = "[C] C1 RequestMakeMacro";

	private static final int MAX_MACRO_LENGTH = 12;

	private L2Macro _macro;
	private int _commandsLenght = 0;

	@Override
	protected void readImpl()
	{
		int _id = readD();
		String _name = readS();
		String _desc = readS();
		String _acronym = readS();
		int _icon = readC();
		int _count = readC();

		if(_count < 0)
		{
			_count = 0;
			return;
		}

		if(_count > MAX_MACRO_LENGTH)
		{
			_count = MAX_MACRO_LENGTH;
		}

		L2MacroCmd[] commands = new L2MacroCmd[_count];

		for(int i = 0; i < _count; i++)
		{
			int entry = readC();
			int type = readC();
			int d1 = readD();
			int d2 = readC();
			String command = readS();
			_commandsLenght += command.length();
			commands[i] = new L2MacroCmd(entry, type, d1, d2, command);
		}
		_macro = new L2Macro(_id, _icon, _name, _desc, _acronym, commands);
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();

		if(player == null)
		{
			return;
		}

		if(_commandsLenght > 255)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.INVALID_MACRO));
			return;
		}

		if(player.getMacroses().getAllMacroses().length > 24)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_MAY_CREATE_UP_TO_24_MACROS));
			return;
		}

		if(_macro.name.length() == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.ENTER_THE_MACRO_NAME));
			return;
		}

		if(_macro.descr.length() > 32)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.MACRO_DESCRIPTION_MAX_32_CHARS));
			return;
		}
		for(L2MacroCmd command:_macro.commands)
		{
			if(!checkSecurityOnCommand(command))
			{
				player.sendPacket(new SystemMessage(SystemMessageId.INVALID_MACRO));
				player.sendMessage("SecurityCheck: not more then 2x ',' or 2x ';' in the same command.");
				return;
			}
		}

		player.registerMacro(_macro);
	}

	private boolean checkSecurityOnCommand(L2MacroCmd cmd)
	{
		if(cmd.cmd != null && cmd.cmd.split(";").length>2)
		{
			return false;
		}

		if(cmd.cmd != null && cmd.cmd.split(",").length>2)
		{
			return false;
		}

		return true;
	}

	@Override
	public String getType()
	{
		return _C__C1_REQUESTMAKEMACRO;
	}

}