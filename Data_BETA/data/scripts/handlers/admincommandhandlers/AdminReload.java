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

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

import com.src.Config;
import com.src.gameserver.TradeController;
import com.src.gameserver.cache.HtmCache;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.sql.ItemTable;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.datatables.xml.NpcWalkerRoutesTable;
import com.src.gameserver.datatables.xml.TeleportLocationTable;
import com.src.gameserver.datatables.xml.ZoneData;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.managers.DatatablesManager;
import com.src.gameserver.managers.Manager;
import com.src.gameserver.managers.QuestManager;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.multisell.L2Multisell;
import com.src.gameserver.script.faenor.FaenorScriptEngine;
import com.src.gameserver.scripting.CompiledScriptCache;
import com.src.gameserver.scripting.L2ScriptEngineManager;

public class AdminReload implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_reload"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		String target = (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target");
		GMAudit.auditGMAction(activeChar.getName(), command, target, "");

		if(command.startsWith("admin_reload"))
		{
			sendReloadPage(activeChar);
			StringTokenizer st = new StringTokenizer(command);
			st.nextToken();

			try
			{
				String type = st.nextToken();

				if(type.equals("multisell"))
				{
					L2Multisell.getInstance().reload();
					sendReloadPage(activeChar);
					activeChar.sendChatMessage(0, 0, "SYS", "Multisell reloaded.");
				}
				else if(type.startsWith("teleport"))
				{
					TeleportLocationTable.getInstance().reloadAll();
					sendReloadPage(activeChar);
					activeChar.sendChatMessage(0, 0, "SYS", "Teleport location table reloaded.");
				}
				else if(type.startsWith("zone"))
				{
					ZoneData.getInstance().reload();
					sendReloadPage(activeChar);
					activeChar.sendChatMessage(0, 0, "SYS", "Zones reloaded.");
				}
				else if(type.startsWith("skill"))
				{
					SkillTable.getInstance().reload();
					sendReloadPage(activeChar);
					activeChar.sendChatMessage(0, 0, "SYS", "Skills reloaded.");
				}
				else if(type.equals("npc"))
				{
					NpcTable.getInstance().reloadAllNpc();
					sendReloadPage(activeChar);
					activeChar.sendChatMessage(0, 0, "SYS", "Npcs reloaded.");
				}
				else if(type.startsWith("htm"))
				{
					HtmCache.getInstance().reload();
					sendReloadPage(activeChar);
					activeChar.sendChatMessage(0, 0, "SYS", "Cache[HTML]: " + HtmCache.getInstance().getMemoryUsage() + " megabytes on " + HtmCache.getInstance().getLoadedFiles() + " files loaded");
				}
				else if(type.startsWith("item"))
				{
					ItemTable.getInstance().reload();
					sendReloadPage(activeChar);
					activeChar.sendChatMessage(0, 0, "SYS", "Item templates reloaded");
				}
				else if(type.startsWith("instancemanager"))
				{
					Manager.reloadAll();
					sendReloadPage(activeChar);
					activeChar.sendChatMessage(0, 0, "SYS", "All instance manager has been reloaded");
				}
				else if(type.startsWith("npcwalkers"))
				{
					NpcWalkerRoutesTable.getInstance().load();
					sendReloadPage(activeChar);
					activeChar.sendChatMessage(0, 0, "SYS", "All NPC walker routes have been reloaded");
				}
				else if(type.startsWith("quests"))
				{
					String folder = "quests";
					QuestManager.getInstance().reload(folder);
					sendReloadPage(activeChar);
					activeChar.sendChatMessage(0, 0, "SYS", "Quests Reloaded.");
				}
				else if(type.startsWith("npcbuffers"))
				{
					DatatablesManager.reloadAll();
					sendReloadPage(activeChar);
					activeChar.sendChatMessage(0, 0, "SYS", "All Buffer skills tables have been reloaded");
				}
				else if(type.equals("configs"))
				{
					Config.load();
					sendReloadPage(activeChar);
					activeChar.sendChatMessage(0, 0, "SYS", "Server Config Reloaded.");
				}
				else if(type.equals("tradelist"))
				{
					TradeController.reload();
					sendReloadPage(activeChar);
					activeChar.sendChatMessage(0, 0, "SYS", "TradeList Table reloaded.");
				}
				else if(type.equals("dbs"))
				{
					DatatablesManager.reloadAll();
					sendReloadPage(activeChar);
					activeChar.sendChatMessage(0, 0, "SYS", "BufferSkillsTable reloaded.");
					activeChar.sendChatMessage(0, 0, "SYS", "NpcBufferSkillIdsTable reloaded.");
					activeChar.sendChatMessage(0, 0, "SYS", "AccessLevels reloaded.");
					activeChar.sendChatMessage(0, 0, "SYS", "AdminCommandAccessRights reloaded.");
					activeChar.sendChatMessage(0, 0, "SYS", "GmListTable reloaded.");
					activeChar.sendChatMessage(0, 0, "SYS", "ClanTable reloaded.");
					activeChar.sendChatMessage(0, 0, "SYS", "AugmentationData reloaded.");
					activeChar.sendChatMessage(0, 0, "SYS", "HelperBuffTable reloaded.");
				}
				else if(type.startsWith("scripts"))
				{
						try
						{
								File scripts = new File(Config.DATAPACK_ROOT + "/data/scripts/scripts.cfg");
								if(!Config.ALT_DEV_NO_QUESTS)
								{
									L2ScriptEngineManager.getInstance().executeScriptsList(scripts);
								}
						}
						catch(IOException ioe)
						{
								activeChar.sendChatMessage(0, 0, "SYS", "Failed loading scripts.cfg, no script going to be loaded");
								ioe.printStackTrace();
						}
						try
						{
								CompiledScriptCache compiledScriptCache = L2ScriptEngineManager.getInstance().getCompiledScriptCache();
								if(compiledScriptCache == null)
								{
									activeChar.sendChatMessage(0, 0, "SYS", "Compiled Scripts Cache is disabled.");
								}
								else
								{
										compiledScriptCache.purge();
										if(compiledScriptCache.isModified())
										{
											compiledScriptCache.save();
											activeChar.sendChatMessage(0, 0, "SYS", "Compiled Scripts Cache was saved.");
										}
										else
										{
											activeChar.sendChatMessage(0, 0, "SYS", "Compiled Scripts Cache is up-to-date.");
										}
								}
						}
						catch(IOException e)
						{
							activeChar.sendChatMessage(0, 0, "SYS", "Failed to store Compiled Scripts Cache.");
							e.printStackTrace();
						}
						QuestManager.getInstance().reloadAllQuests();
						QuestManager.getInstance().report();
						FaenorScriptEngine.getInstance().reloadPackages();
				}
				activeChar.sendChatMessage(0, 0, "SYS", "WARNING: There are several known issues regarding this feature. Reloading server data during runtime is STRONGLY NOT RECOMMENDED for live servers, just for developing environments.");
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage:  //reload <type>");
			}
		}
		return true;
	}

	private void sendReloadPage(L2PcInstance activeChar)
	{
		AdminHelpPage.showSubMenuPage(activeChar, "reload_menu.htm");
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}