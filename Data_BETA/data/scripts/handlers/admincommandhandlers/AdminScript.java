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
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.script.ScriptException;

import com.src.Config;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.scripting.L2ScriptEngineManager;

public class AdminScript implements IAdminCommandHandler
{
	private static final File SCRIPT_FOLDER = new File(Config.DATAPACK_ROOT.getAbsolutePath(), "data/scripts");
	private static final Logger _log = Logger.getLogger(AdminScript.class.getName());

	private static final String[] ADMIN_COMMANDS =
	{
		"admin_load_script"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		String target = (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target");
		GMAudit.auditGMAction(activeChar.getName(), command, target, "");

		if(command.startsWith("admin_load_script"))
		{
			File file;
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			String line = st.nextToken();

			try
			{
				file = new File(SCRIPT_FOLDER, line);

				if(file.isFile())
				{
					try
					{
						L2ScriptEngineManager.getInstance().executeScript(file);
					}
					catch(ScriptException e)
					{
						L2ScriptEngineManager.getInstance().reportScriptFileError(file, e);
					}
				}
				else
				{
					_log.warning("Failed loading: (" + file.getCanonicalPath() + " - Reason: doesnt exists or is not a file.");
				}
			}
			catch(Exception e)
			{
				
			}
		}
		return true;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}