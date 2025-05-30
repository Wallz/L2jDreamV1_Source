package handlers.admincommandhandlers;

import java.util.StringTokenizer;

import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.util.StringUtil;

/**
 * @author Vhalior
 */
public class AdminBuffs implements IAdminCommandHandler
{
	private final static int PAGE_LIMIT = 20;

	private static final String[] ADMIN_COMMANDS =
	{
		"admin_getbuffs",
		"admin_stopbuff",
		"admin_stopallbuffs",
		"admin_areacancel"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		GMAudit.auditGMAction(activeChar.getName(), command, (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target"), "");

		if(command.startsWith("admin_getbuffs"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			command = st.nextToken();

			if(st.hasMoreTokens())
			{
				L2PcInstance player = null;
				String playername = st.nextToken();

				try
				{
					player = L2World.getInstance().getPlayer(playername);
				}
				catch(Exception e)
				{
					// Null
				}

				if(player != null)
				{
					int page = 1;
					if(st.hasMoreTokens())
					{
						page = Integer.parseInt(st.nextToken());
					}

					showBuffs(activeChar, player, page);
					return true;
				}
				else
				{
					activeChar.sendChatMessage(0, 0, "SYS", new StringBuilder().append("The player ").append(playername).append(" is not online.").toString());
					return false;
				}
			}
			else if((activeChar.getTarget() != null) && (activeChar.getTarget() instanceof L2Character))
			{
				showBuffs(activeChar, (L2Character) activeChar.getTarget(), 1);
				return true;
			}
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				return false;
			}
		}
		else if(command.startsWith("admin_stopbuff"))
		{
			try
			{
				StringTokenizer st = new StringTokenizer(command, " ");
				
				st.nextToken();
				int objectId = Integer.parseInt(st.nextToken());
				int skillId = Integer.parseInt(st.nextToken());
				
				removeBuff(activeChar, objectId, skillId);
				return true;
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", new StringBuilder().append("Failed removing effect: ").append(e.getMessage()).append(".").toString());
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //stopbuff <playername> [skillId].");
				return false;
			}
		}
		else if(command.startsWith("admin_stopallbuffs"))
		{
			try
			{
				StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				int objectId = Integer.parseInt(st.nextToken());
				removeAllBuffs(activeChar, objectId);
				return true;
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Failed removing all effects: " + e.getMessage());
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //stopallbuffs <objectId>");
				return false;
			}
		}
		else if(command.startsWith("admin_areacancel"))
		{
			try
			{
				StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				String val = st.nextToken();
				int radius = Integer.parseInt(val);

				for(L2Character knownChar : activeChar.getKnownList().getKnownCharactersInRadius(radius))
				{
					if((knownChar instanceof L2PcInstance) && !(knownChar.equals(activeChar)))
						knownChar.stopAllEffects();
				}

				activeChar.sendChatMessage(0, 0, "SYS", new StringBuilder().append("All effects canceled within raidus ").append(radius).toString());
				return true;
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //areacancel <radius>");
				return false;
			}
		}
		else
		{
			return true;
		}
	}

	public void showBuffs(L2PcInstance activeChar, L2Character target, int page)
	{
		final L2Effect[] effects = target.getAllEffects();

		if(page > effects.length / PAGE_LIMIT + 1 || page < 1)
		{
			return;
		}

		int max = effects.length / PAGE_LIMIT;
		if(effects.length > PAGE_LIMIT * max)
		{
			max++;
		}

		final StringBuilder html = StringUtil.startAppend(500 + effects.length * 200,
		"<html><title>Buff Panel</title><body><center><table width=240 bgcolor=\"12334\"><tr><td><button value=\"Main\" action=\"bypass -h admin_admin\" width=65 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td><td><button value=\"Game\" action=\"bypass -h admin_admin2\" width=65 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td><td><button value=\"Effects\" action=\"bypass -h admin_admin3\" width=65 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td><td><button value=\"GM\" action=\"bypass -h admin_admin4\" width=65 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\"></td></tr></table><br1><font color=\"333333\" align=\"center\">_______________________________________</font><br><table width=\"100%\"><tr><td width=160>Skill</td><td width=60>Time Left</td><td width=60>Action</td></tr>");

		int start = ((page - 1) * PAGE_LIMIT);
		int end = Math.min(((page - 1) * PAGE_LIMIT) + PAGE_LIMIT, effects.length);

		for(int i = start; i < end; i++)
		{
			L2Effect e = effects[i];
			if(e != null)
			{
				StringUtil.append(html,
					"<tr><td>", e.getSkill().getName(),"</td><td>",
					e.getSkill().isToggle() ? "toggle" : e.getPeriod() - e.getTime() + "s",
					"</td><td><a action=\"bypass -h admin_stopbuff ",
					Integer.toString(target.getObjectId()), " ", String.valueOf(e.getSkill().getId()),
				"\">Remove</a></td></tr>");
			}
		}

		html.append("</table><br><table width=\"100%\" bgcolor=444444><tr>");
		for(int x = 0; x < max; x++)
		{
			int pagenr = x + 1;
			if(page == pagenr)
			{
				html.append("<td>Page ");
				html.append(pagenr);
				html.append("</td>");
			}
			else
			{
				html.append("<td><a action=\"bypass -h admin_getbuffs ");
				html.append(target.getName());
				html.append(" ");
				html.append(x + 1);
				html.append("\"> Page ");
				html.append(pagenr);
				html.append(" </a></td>");
			}
		}

		html.append("</tr></table>");

		StringUtil.append(html, "<br><center><button value=\"Remove All\" action=\"bypass -h admin_stopallbuffs ",
		Integer.toString(target.getObjectId()),
		"\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"><br><font color=\"333333\" align=\"center\">_______________________________________</font><br1><font color=\"3399ff\">Server Software created by L2JSky.</font></center></body></html>");

		NpcHtmlMessage ms = new NpcHtmlMessage(1);
		ms.setHtml(html.toString());
		activeChar.sendPacket(ms);
	}

	private void removeBuff(L2PcInstance activeChar, int objId, int skillId)
	{
		L2Character target = null;
		try
		{
			target = (L2Character) L2World.getInstance().findObject(objId);
		}
		catch(Exception e)
		{
			// Null
		}

		if((target != null) && (skillId > 0))
		{
			L2Effect[] effects = target.getAllEffects();

			for(L2Effect e : effects)
			{
				if((e != null) && (e.getSkill().getId() == skillId))
				{
					e.exit();
					activeChar.sendChatMessage(0, 0, "SYS", new StringBuilder().append("Removed ").append(e.getSkill().getName()).append(" level ").append(e.getSkill().getLevel()).append(" from ").append(objId).toString());
				}
			}

			showBuffs(activeChar, target, 1);
		}
	}

	private void removeAllBuffs(L2PcInstance activeChar, int objId)
	{
		L2Character target = null;
		try
		{
			target = (L2Character) L2World.getInstance().findObject(objId);
		}
		catch(Exception e)
		{
			// Null
		}

		if(target != null)
		{
			target.stopAllEffects();
			activeChar.sendChatMessage(0, 0, "SYS", new StringBuilder().append("Removed all effects from ").append(objId).toString());
			showBuffs(activeChar, target, 1);
		}
		else
		{
			activeChar.sendChatMessage(0, 0, "SYS", new StringBuilder().append("The player ").append(objId).append(" is not online.").toString());
		}
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

}