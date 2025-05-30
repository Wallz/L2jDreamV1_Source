package handlers.admincommandhandlers;

import java.util.StringTokenizer;

import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.managers.FunEventsManager;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;

public class AdminEvents implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_event", 
		"admin_start", 
		"admin_abort"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		if (command.startsWith("admin_event"))
		{
			showEventWindow(activeChar);
		}
		else if (command.startsWith("admin_start"))
		{
			try
			{
				StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				
				String event = st.nextToken().toUpperCase();
				
				if (FunEventsManager.getInstance().getEvents().containsKey(event))
				{
					FunEventsManager.getInstance().startEvent(event);
				}
				else
				{
					activeChar.sendChatMessage(0, 0, "SYS", "No such event registered!");
				}
			}
			catch (Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Command ussage: //start <EventName>");
			}
		}
		else if (command.startsWith("admin_abort"))
		{
			try
			{
				StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				
				String event = st.nextToken().toUpperCase();
				
				if (FunEventsManager.getInstance().getEvents().containsKey(event))
				{
					FunEventsManager.getInstance().abortEvent(event);
				}
				else
				{
					activeChar.sendChatMessage(0, 0, "SYS", "No such event registered!");
				}
			}
			catch (Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Command ussage: //abort <EventName>");
			}
		}
		
		showEventWindow(activeChar);
		return true;
	}
	
	/**
	 * Generate event list
	 */
	private String eventList()
	{
		String list = "";
				
		for (String event: FunEventsManager.getInstance().getEvents().keySet())
		{	
			list += "<table width=230 border=0><tr><td width=270 align=\"left\">" + event 
			        + "</td><td width=100 align=\"right\">" 
			        + "<a action=\"bypass -h admin_start " + event.toLowerCase() + "\">Start</a></td>" 
			        + "<td width=100 align=\"right\">" 
			        + "<a action=\"bypass -h admin_abort " + event.toLowerCase() + "\">Abort</a></td></tr></table>";
		}
		return list;
	}
	
	private void showEventWindow(L2PcInstance activeChar)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/gm/event.htm");
		adminReply.replace("%events%", eventList());
		activeChar.sendPacket(adminReply);
	}

	/**
	 * sends config reload page
	 * 
	 * @param admin private void sendConfigReloadPage(L2PcInstance activeChar) {
	 *            AdminHelpPage.showSubMenuPage(activeChar, "config_reload_menu.htm"); }
	 **/
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}