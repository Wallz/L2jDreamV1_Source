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

import com.src.Config;
import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.communitybbs.CommunityBoard;
import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.AdminCommandHandler;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2ClassMasterInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2SymbolMakerInstance;
import com.src.gameserver.model.actor.position.L2CharPosition;
import com.src.gameserver.model.entity.olympiad.Olympiad;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.GMViewPledgeInfo;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.util.FloodProtector;

public final class RequestBypassToServer extends L2GameClientPacket
{
	private final static Log _log = LogFactory.getLog(RequestBypassToServer.class);

	private static final String _C__21_REQUESTBYPASSTOSERVER = "[C] 21 RequestBypassToServer";

	private String _command;

	@Override
	protected void readImpl()
	{
		_command = readS();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if(activeChar == null)
		{
			return;
		}

		if(!FloodProtector.getInstance().tryPerformAction(activeChar.getObjectId(), FloodProtector.PROTECTED_BYPASS))
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		try
		{
			if(_command.startsWith("admin_"))
			{
				String command;

				if(_command.indexOf(" ") != -1)
					command = _command.substring(0, _command.indexOf(" "));
				else
					command = _command;

				IAdminCommandHandler ach = AdminCommandHandler.getInstance().getAdminCommandHandler(command);

				if(ach == null)
				{
					if(activeChar.isGM())
					{
						activeChar.sendMessage("The command " + command + " does not exists!");
					}

					_log.warn("No handler registered for admin command '" + command + "'");
					return;
				}

				if(!AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel()))
				{
					activeChar.sendMessage("You don't have the access right to use this command!");
					return;
				}

				ach.useAdminCommand(_command, activeChar);
			}
			else if(_command.equals("come_here") && activeChar.isGM())
				comeHere(activeChar);
			else if(_command.startsWith("player_help "))
				playerHelp(activeChar, _command.substring(12));
			else if(_command.startsWith("npc_"))
			{
				if(!activeChar.validateBypass(_command))
				{
					return;
				}

				int endOfId = _command.indexOf('_', 5);
				String id;

				if(endOfId > 0)
					id = _command.substring(4, endOfId);
				else
					id = _command.substring(4);

				try
				{
					L2Object object = L2World.getInstance().findObject(Integer.parseInt(id));

					if((Config.ALLOW_CLASS_MASTERS && Config.ALLOW_REMOTE_CLASS_MASTERS && object instanceof L2ClassMasterInstance) || (object instanceof L2Npc && endOfId > 0 && activeChar.isInsideRadius(object, L2Npc.INTERACTION_DISTANCE, false, false)))
						((L2Npc) object).onBypassFeedback(activeChar, _command.substring(endOfId + 1));

					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				}
				catch(NumberFormatException nfe)
				{}
			}
			else if(_command.equals("Draw"))
			{
				L2Object object = activeChar.getTarget();
				if(object instanceof L2Npc)
				{
					((L2SymbolMakerInstance) object).onBypassFeedback(activeChar, _command);
				}
			}
			else if(_command.equals("RemoveList"))
			{
				L2Object object = activeChar.getTarget();
				if(object instanceof L2Npc)
				{
					((L2SymbolMakerInstance) object).onBypassFeedback(activeChar, _command);
				}
			}
			else if(_command.equals("Remove "))
			{
				L2Object object = activeChar.getTarget();

				if(object instanceof L2Npc)
				{
					((L2SymbolMakerInstance) object).onBypassFeedback(activeChar, _command);
				}
			}
			else if(_command.startsWith("manor_menu_select?"))
			{
				L2Object object = activeChar.getTarget();
				if(object instanceof L2Npc)
				{
					((L2Npc) object).onBypassFeedback(activeChar, _command);
				}
			}
			else if(_command.startsWith("bbs_"))
			{
				CommunityBoard.getInstance().handleCommands(getClient(), _command);
			}
			else if(_command.startsWith("_bbs"))
			{
				CommunityBoard.getInstance().handleCommands(getClient(), _command);
			}
			else if(_command.startsWith("Quest "))
			{
				if(!activeChar.validateBypass(_command))
				{
					return;
				}

				L2PcInstance player = getClient().getActiveChar();
				if(player == null)
				{
					return;
				}

				String p = _command.substring(6).trim();
				int idx = p.indexOf(' ');

				if(idx < 0)
				{
					player.processQuestEvent(p, "");
				}
				else
				{
					player.processQuestEvent(p.substring(0, idx), p.substring(idx).trim());
				}
			}
			else if (_command.startsWith("show_clan_info "))  
			{  
				activeChar.sendPacket(new GMViewPledgeInfo(ClanTable.getInstance().getClanByName(_command.substring(15)),activeChar));
			}
			else if(_command.equals("rbAnswear"))
			{
				L2PcInstance player = getClient().getActiveChar();
				if(player.getParty().getLeader().isInOlympiadMode())
				{
					return;
				}
				else if(player.isInOlympiadMode())
				{
					return;
				}

				player.teleToLocation(player.getParty().getLeader().getX(), player.getParty().getLeader().getY(), player.getParty().getLeader().getZ());
			}
			else if(_command.equals("rbAnswearDenied"))
			{
				L2PcInstance player = getClient().getActiveChar();
				L2PcInstance target = player.getParty().getLeader();
				target.sendMessage("The invitation was denied by "+player.getName()+".");
				return;
			}
			else if(_command.startsWith("raidbosslevel_"))
			{
				L2PcInstance player = getClient().getActiveChar();
				if(!player.validateBypass(_command))
				{
					return;
				}

				int endOfId = _command.indexOf('_', 5);
				if(endOfId > 0)
				{
					_command.substring(4, endOfId);
				}
				else
				{
					_command.substring(4);
				}
				try
				{
					if(_command.substring(endOfId+1).startsWith("40"))
					{
						player.showRaidbossInfoLevel40();
					}
					else if(_command.substring(endOfId+1).startsWith("45"))
					{
						player.showRaidbossInfoLevel45();
					}
					else if(_command.substring(endOfId+1).startsWith("50"))
					{
						player.showRaidbossInfoLevel50();
					}
					else if(_command.substring(endOfId+1).startsWith("55"))
					{
						player.showRaidbossInfoLevel55();
					}
					else if(_command.substring(endOfId+1).startsWith("60"))
					{
						player.showRaidbossInfoLevel60();
					}
					else if(_command.substring(endOfId+1).startsWith("65"))
					{
						player.showRaidbossInfoLevel65();
					}
					else if(_command.substring(endOfId+1).startsWith("70"))
					{
						player.showRaidbossInfoLevel70();
					}
					else if (_command.startsWith("OlympiadArenaChange"))
					{
						Olympiad.bypassChangeArena(_command, activeChar);
					}
				}
				catch(NumberFormatException nfe)
				{
				}
			}
		}
		catch(Exception e)
		{
			_log.error("Bad RequestBypassToServer", e);
		}
	}

	private void comeHere(L2PcInstance activeChar)
	{
		L2Object obj = activeChar.getTarget();
		if(obj == null)
		{
			return;
		}

		if(obj instanceof L2Npc)
		{
			L2Npc temp = (L2Npc) obj;
			temp.setTarget(activeChar);
			temp.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(activeChar.getX(), activeChar.getY(), activeChar.getZ(), 0));
		}
	}

	private void playerHelp(L2PcInstance activeChar, String path)
	{
		if(path.indexOf("..") != -1)
		{
			return;
		}

		String filename = "data/html/help/" + path;
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(filename);
		activeChar.sendPacket(html);
	}

	@Override
	public String getType()
	{
		return _C__21_REQUESTBYPASSTOSERVER;
	}

}