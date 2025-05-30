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

import java.io.File;

import com.src.Config;
import com.src.gameserver.cache.CrestCache;
import com.src.gameserver.cache.HtmCache;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class AdminCache implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_cache_htm_rebuild",
		"admin_cache_htm_reload",
		"admin_cache_reload_path",
		"admin_cache_reload_file",
		"admin_cache_crest_rebuild",
		"admin_cache_crest_reload",
		"admin_cache_crest_fix"
	};

	private enum CommandEnum
	{
		admin_cache_htm_rebuild,
		admin_cache_htm_reload,
		admin_cache_reload_path,
		admin_cache_reload_file,
		admin_cache_crest_rebuild,
		admin_cache_crest_reload,
		admin_cache_crest_fix
	}

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		String[] wordList = command.split(" ");
		CommandEnum comm;

		try
		{
			comm = CommandEnum.valueOf(wordList[0]);
		}
		catch(Exception e)
		{
			return false;
		}

		CommandEnum commandEnum = comm;

		switch(commandEnum)
		{
			case admin_cache_htm_reload:
				HtmCache.getInstance().reload(Config.DATAPACK_ROOT);
				activeChar.sendChatMessage(0, 0, "SYS", "Cache[HTML]: " + HtmCache.getInstance().getMemoryUsage() + " MB on " + HtmCache.getInstance().getLoadedFiles() + " file(s) loaded.");
				break;

			case admin_cache_reload_path:
				try
				{
					String path = command.split(" ")[1];
					HtmCache.getInstance().reloadPath(new File(Config.DATAPACK_ROOT, path));
					activeChar.sendChatMessage(0, 0, "SYS","Cache[HTML]: " + HtmCache.getInstance().getMemoryUsage() + " MB in " + HtmCache.getInstance().getLoadedFiles() + " file(s) loaded.");
				}
				catch(Exception e)
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Usage: //cache_reload_path <path>");
				}
				break;

			case admin_cache_reload_file:
				try
				{
					String path = command.split(" ")[1];
					if(HtmCache.getInstance().loadFile(new File(Config.DATAPACK_ROOT, path)) != null)
					{
						activeChar.sendChatMessage(0, 0, "SYS", "Cache[HTML]: file was loaded");
					}
					else
					{
						activeChar.sendChatMessage(0, 0, "SYS", "Cache[HTML]: file can't be loaded");
					}
				}
				catch(Exception e)
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Usage: //cache_reload_file <relative_path/file>");
				}
				break;

			case admin_cache_crest_rebuild:
				CrestCache.getInstance().reload();
				activeChar.sendChatMessage(0, 0, "SYS", "Cache[Crest]: " + String.format("%.3f", CrestCache.getInstance().getMemoryUsage()) + " megabytes on " + CrestCache.getInstance().getLoadedFiles() + " files loaded");
				break;

			case admin_cache_crest_fix:
				CrestCache.getInstance().convertOldPedgeFiles();
				activeChar.sendChatMessage(0, 0, "SYS", "Cache[Crest]: crests fixed");
				break;
		default:
			break;
		}

		String target = (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target");
		GMAudit.auditGMAction(activeChar.getName(), command, target, "");
		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}