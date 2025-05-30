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

import java.util.StringTokenizer;

import com.src.Config;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2ControllableMobInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class AdminKill implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_kill",
		"admin_kill_monster"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		String target = (activeChar.getTarget() != null) ? activeChar.getTarget().getName() : "no-target";
		GMAudit.auditGMAction(activeChar.getName(), command, target, "");

		if(command.startsWith("admin_kill"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();

			if(st.hasMoreTokens())
			{
				String firstParam = st.nextToken();
				L2PcInstance plyr = L2World.getInstance().getPlayer(firstParam);

				if(plyr != null)
				{
					if(st.hasMoreTokens())
					{
						try
						{
							int radius = Integer.parseInt(st.nextToken());

							for(L2Character knownChar : plyr.getKnownList().getKnownCharactersInRadius(radius))
							{
								if(knownChar == null || knownChar instanceof L2ControllableMobInstance || knownChar.equals(activeChar))
								{
									continue;
								}

								kill(activeChar, knownChar);
							}

							activeChar.sendChatMessage(0, 0, "SYS", "Killed all characters within a " + radius + " unit radius.");

							return true;
						}
						catch(NumberFormatException e)
						{
							activeChar.sendChatMessage(0, 0, "SYS", "Invalid radius.");
							return false;
						}
					}
					else
					{
						kill(activeChar, plyr);
					}
				}
				else
				{
					try
					{
						int radius = Integer.parseInt(firstParam);

						for(L2Character knownChar : activeChar.getKnownList().getKnownCharactersInRadius(radius))
						{
							if(knownChar == null || knownChar instanceof L2ControllableMobInstance || knownChar.equals(activeChar))
							{
								continue;
							}

							kill(activeChar, knownChar);
						}

						activeChar.sendChatMessage(0, 0, "SYS", "Killed all characters within a " + radius + " unit radius.");

						return true;
					}
					catch(NumberFormatException e)
					{
						activeChar.sendChatMessage(0, 0, "SYS", "Usage: //kill <player_name | radius>");
						return false;
					}
				}
			}
			else
			{
				L2Object obj = activeChar.getTarget();

				if(obj == null || obj instanceof L2ControllableMobInstance || !(obj instanceof L2Character))
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
				}
				else
				{
					kill(activeChar, (L2Character) obj);
				}
			}
		}
		return true;
	}

	private void kill(L2PcInstance activeChar, L2Character target)
	{
		if(target instanceof L2PcInstance)
		{
			if(!((L2PcInstance) target).isGM())
			{
				target.stopAllEffects();
			}

			target.reduceCurrentHp(target.getMaxHp() + target.getMaxCp() + 1, activeChar);
		}
		else if(Config.CHAMPION_ENABLE && target.isChampion())
		{
			target.reduceCurrentHp(target.getMaxHp() * Config.CHAMPION_HP + 1, activeChar);
		}
		else
		{
			if (target.isInvul()) target.setIsInvul(false);
			target.reduceCurrentHp(target.getMaxHp() + 1, activeChar);
		}
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}