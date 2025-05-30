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
 * [URL]http://www.gnu.org/copyleft/gpl.html[/URL]
 */
package handlers.admincommandhandlers;

import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class AdminHeal implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_heal"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		GMAudit.auditGMAction(activeChar.getName(), command, (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target"), "");

		if(command.equals("admin_heal"))
		{
			handleRes(activeChar);
		}
		else if(command.startsWith("admin_heal"))
		{
			try
			{
				String healTarget = command.substring(11);
				handleRes(activeChar, healTarget);
				healTarget = null;
			}
			catch(StringIndexOutOfBoundsException e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Incorrect target/radius specified.");
			}
		}
		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void handleRes(L2PcInstance activeChar)
	{
		handleRes(activeChar, null);
	}

	private void handleRes(L2PcInstance activeChar, String player)
	{
		L2Object obj = activeChar.getTarget();

		if(player != null)
		{
			L2PcInstance plyr = L2World.getInstance().getPlayer(player);

			if(plyr != null)
			{
				obj = plyr;
			}
			else
			{
				try
				{
					int radius = Integer.parseInt(player);
					for(L2Object object : activeChar.getKnownList().getKnownObjects().values())
					{
						if(object instanceof L2Character)
						{
							L2Character character = (L2Character) object;
							character.setCurrentHpMp(character.getMaxHp(), character.getMaxMp());

							if(object instanceof L2PcInstance)
							{
								character.setCurrentCp(character.getMaxCp());
							}
						}
					}
					activeChar.sendChatMessage(0, 0, "SYS", "Healed within " + radius + " unit radius.");
					return;
				}
				catch(NumberFormatException nbe)
				{
				}
			}
		}

		if(obj == null)
		{
			obj = activeChar;
		}

		if(obj != null && obj instanceof L2Character)
		{
			L2Character target = (L2Character) obj;
			target.setCurrentHpMp(target.getMaxHp(), target.getMaxMp());

			if(target instanceof L2PcInstance)
			{
				target.setCurrentCp(target.getMaxCp());
			}
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
		}
	}
}