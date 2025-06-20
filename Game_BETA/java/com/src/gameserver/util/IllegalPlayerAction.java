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

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.src.Config;
import com.src.gameserver.datatables.GmListTable;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public final class IllegalPlayerAction implements Runnable
{
	private static Logger _logAudit = Logger.getLogger(IllegalPlayerAction.class.getName());

	private String _message;
	private int _punishment;
	private L2PcInstance _actor;

	public static final int PUNISH_BROADCAST = 1;
	public static final int PUNISH_KICK = 2;
	public static final int PUNISH_KICKBAN = 3;
	public static final int PUNISH_JAIL = 4;

	public IllegalPlayerAction(L2PcInstance actor, String message, int punishment)
	{
		_message = message;
		_punishment = punishment;
		_actor = actor;

		switch(punishment)
		{
			case PUNISH_KICK:
				_actor.sendMessage("You will be kicked for illegal action, GM informed.");
				_actor.closeNetConnection();
				break;
			case PUNISH_KICKBAN:
				_actor.setAccessLevel(-100);
				_actor.setAccountAccesslevel(-100);
				_actor.sendMessage("You are banned for illegal action, GM informed.");
				break;
			case PUNISH_JAIL:
				_actor.sendMessage("Illegal action performed!");
				_actor.sendMessage("You will be teleported to GM Consultation Service area and jailed.");
				break;
		}
	}

	@Override
	public void run()
	{
		LogRecord record = new LogRecord(Level.INFO, "AUDIT:" + _message);
		record.setLoggerName("audit");
		record.setParameters(new Object[]
		{
				_actor, _punishment
		});
		_logAudit.log(record);

		GmListTable.broadcastMessageToGMs(_message);

		switch(_punishment)
		{
			case PUNISH_BROADCAST:
				return;
			case PUNISH_KICK:
				_actor.logout();
				break;
			case PUNISH_KICKBAN:
				_actor.logout();
				break;
			case PUNISH_JAIL:
				_actor.setPunishLevel(L2PcInstance.PunishLevel.JAIL, Config.DEFAULT_PUNISH_PARAM);
				break;
		}
	}
}