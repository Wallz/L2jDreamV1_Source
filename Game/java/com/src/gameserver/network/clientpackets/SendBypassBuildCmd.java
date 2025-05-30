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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.AdminCommandHandler;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public final class SendBypassBuildCmd extends L2GameClientPacket
{
	private final static Log _log = LogFactory.getLog(SendBypassBuildCmd.class.getName());

	private static final String _C__5B_SENDBYPASSBUILDCMD = "[C] 5b SendBypassBuildCmd";

	public final static int GM_MESSAGE = 9;
	public final static int ANNOUNCEMENT = 10;

	private String _command;

	@Override
	protected void readImpl()
	{
		_command = "admin_" + readS().trim();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		if(!AdminCommandAccessRights.getInstance().hasAccess(_command, activeChar.getAccessLevel()))
		{
			activeChar.sendMessage("You don't have the access right to use this command!");
			_log.warn("Character " + activeChar.getName() + " tried to use admin command " + _command + ", but doesn't have access to it!");
			return;
		}

		IAdminCommandHandler ach = AdminCommandHandler.getInstance().getAdminCommandHandler(_command);

		if(ach != null)
		{
			ach.useAdminCommand(_command, activeChar);
		}
		else
		{
			activeChar.sendMessage("The command " + _command + " doesn't exists!");
			_log.warn("No handler registered for admin command '" + _command + "'");
			return;
		}
	}

	@Override
	public String getType()
	{
		return _C__5B_SENDBYPASSBUILDCMD;
	}
}