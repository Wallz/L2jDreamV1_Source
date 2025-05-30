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
package handlers.admincommandhandlers;

import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.MonsterRace;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.DeleteObject;
import com.src.gameserver.network.serverpackets.MonRaceInfo;
import com.src.gameserver.network.serverpackets.PlaySound;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.thread.ThreadPoolManager;

public class AdminMonsterRace implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_mons"
	};

	protected static int state = -1;

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		if(command.equalsIgnoreCase("admin_mons"))
		{
			handleSendPacket(activeChar);
		}

		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void handleSendPacket(L2PcInstance activeChar)
	{
		int[][] codes =
		{
				{
						-1, 0
				},
				{
						0, 15322
				},
				{
						13765, -1
				},
				{
						-1, 0
				}
		};
		MonsterRace race = MonsterRace.getInstance();

		if(state == -1)
		{
			state++;
			race.newRace();
			race.newSpeeds();
			MonRaceInfo spk = new MonRaceInfo(codes[state][0], codes[state][1], race.getMonsters(), race.getSpeeds());
			activeChar.sendPacket(spk);
			activeChar.broadcastPacket(spk);
			spk = null;
		}
		else if(state == 0)
		{
			state++;

			activeChar.sendPacket(new SystemMessage(SystemMessageId.MONSRACE_RACE_START).addNumber(0));

			PlaySound SRace = new PlaySound(1, "S_Race", 0, 0, 0, 0, 0);
			activeChar.sendPacket(SRace);
			activeChar.broadcastPacket(SRace);

			PlaySound SRace2 = new PlaySound(0, "ItemSound2.race_start", 1, 121209259, 12125, 182487, -3559);
			activeChar.sendPacket(SRace2);
			activeChar.broadcastPacket(SRace2);

			MonRaceInfo spk = new MonRaceInfo(codes[state][0], codes[state][1], race.getMonsters(), race.getSpeeds());
			activeChar.sendPacket(spk);
			activeChar.broadcastPacket(spk);

			ThreadPoolManager.getInstance().scheduleGeneral(new RunRace(codes, activeChar), 5000);
		}
	}

	class RunRace implements Runnable
	{

		private int[][] codes;
		private L2PcInstance activeChar;

		public RunRace(int[][] pCodes, L2PcInstance pActiveChar)
		{
			codes = pCodes;
			activeChar = pActiveChar;
		}

		public void run()
		{
			MonRaceInfo spk = new MonRaceInfo(codes[2][0], codes[2][1], MonsterRace.getInstance().getMonsters(), MonsterRace.getInstance().getSpeeds());
			activeChar.sendPacket(spk);
			activeChar.broadcastPacket(spk);
			ThreadPoolManager.getInstance().scheduleGeneral(new RunEnd(activeChar), 30000);
		}
	}

	class RunEnd implements Runnable
	{
		private L2PcInstance activeChar;

		public RunEnd(L2PcInstance pActiveChar)
		{
			activeChar = pActiveChar;
		}

		public void run()
		{
			DeleteObject obj = null;

			for(int i = 0; i < 8; i++)
			{
				obj = new DeleteObject(MonsterRace.getInstance().getMonsters()[i]);
				activeChar.sendPacket(obj);
				activeChar.broadcastPacket(obj);
				obj = null;
			}
			state = -1;
		}
	}
}