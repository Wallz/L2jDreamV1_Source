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

import javolution.text.TextBuilder;

import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.datatables.xml.SkillTreeTable;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.L2SkillLearn;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.PledgeSkillList;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class AdminSkill implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_show_skills",
		"admin_remove_skills",
		"admin_skill_list",
		"admin_skill_index",
		"admin_add_skill",
		"admin_remove_skill",
		"admin_get_skills",
		"admin_reset_skills",
		"admin_give_all_skills",
		"admin_remove_all_skills",
		"admin_add_clan_skill"
	};

	private static L2Skill[] adminSkills;

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		GMAudit.auditGMAction(activeChar.getName(), command, (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target"), "");

		if(command.equals("admin_show_skills"))
		{
			showMainPage(activeChar);
		}
		else if(command.startsWith("admin_remove_skills"))
		{
			try
			{
				String val = command.substring(20);
				removeSkillsPage(activeChar, Integer.parseInt(val));
			}
			catch(StringIndexOutOfBoundsException e)
			{
			}
		}
		else if(command.startsWith("admin_skill_list"))
		{
			AdminHelpPage.showHelpPage(activeChar, "game/skills.htm");
		}
		else if(command.startsWith("admin_skill_index"))
		{
			try
			{
				String val = command.substring(18);
				AdminHelpPage.showHelpPage(activeChar, "game/skills/" + val + ".htm");
			}
			catch(StringIndexOutOfBoundsException e)
			{
			}
		}
		else if(command.startsWith("admin_add_skill"))
		{
			try
			{
				String val = command.substring(15);

				if(activeChar == activeChar.getTarget() || activeChar.getAccessLevel().isGm())
				{
					adminAddSkill(activeChar, val);
				}
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //add_skill <skill_id> <level>");
			}
		}
		else if(command.startsWith("admin_remove_skill"))
		{
			try
			{
				String id = command.substring(19);

				int idval = Integer.parseInt(id);

				if(activeChar == activeChar.getTarget() || activeChar.getAccessLevel().isGm())
				{
					adminRemoveSkill(activeChar, idval);
				}
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //remove_skill <skill_id>");
			}
		}
		else if(command.equals("admin_get_skills"))
		{
			adminGetSkills(activeChar);
		}
		else if(command.equals("admin_reset_skills"))
		{
			if(activeChar == activeChar.getTarget() || activeChar.getAccessLevel().isGm())
			{
				adminResetSkills(activeChar);
			}
		}
		else if(command.equals("admin_give_all_skills"))
		{
			if(activeChar == activeChar.getTarget() || activeChar.getAccessLevel().isGm())
			{
				adminGiveAllSkills(activeChar);
			}
		}
		else if(command.equals("admin_remove_all_skills"))
		{
			if(activeChar.getTarget() instanceof L2PcInstance)
			{
				L2PcInstance player = (L2PcInstance) activeChar.getTarget();

				for(L2Skill skill : player.getAllSkills())
				{
					player.removeSkill(skill);
				}

				activeChar.sendChatMessage(0, 0, "SYS", "You removed all skills from " + player.getName());
				player.sendChatMessage(0, 0, "SYS", "Admin removed all skills from you.");
				player.sendSkillList();
			}
		}
		else if(command.startsWith("admin_add_clan_skill"))
		{
			try
			{
				String[] val = command.split(" ");

				if(activeChar == activeChar.getTarget() || activeChar.getAccessLevel().isGm())
				{
					adminAddClanSkill(activeChar, Integer.parseInt(val[1]), Integer.parseInt(val[2]));
				}
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //add_clan_skill <skill_id> <level>");
			}
		}

		return true;
	}

	private void adminGiveAllSkills(L2PcInstance activeChar)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;

		if(target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			return;
		}

		boolean countUnlearnable = true;
		int unLearnable = 0;
		int skillCounter = 0;

		L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSkills(player, player.getClassId());

		while(skills.length > unLearnable)
		{
			for(L2SkillLearn s : skills)
			{
				L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());

				if(sk == null || !sk.getCanLearn(player.getClassId()))
				{
					if(countUnlearnable)
					{
						unLearnable++;
					}

					continue;
				}

				if(player.getSkillLevel(sk.getId()) == -1)
				{
					skillCounter++;
				}

				player.addSkill(sk, true);
			}

			countUnlearnable = false;
			skills = SkillTreeTable.getInstance().getAvailableSkills(player, player.getClassId());
		}

		player.sendChatMessage(0, 0, "SYS", "A GM gave you " + skillCounter + " skills.");
		activeChar.sendChatMessage(0, 0, "SYS", "You gave " + skillCounter + " skills to " + player.getName());
		player.sendSkillList();
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private void removeSkillsPage(L2PcInstance activeChar, int page)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;

		if(target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
			return;
		}

		L2Skill[] skills = player.getAllSkills();

		int MaxSkillsPerPage = 10;
		int MaxPages = skills.length / MaxSkillsPerPage;

		if(skills.length > MaxSkillsPerPage * MaxPages)
		{
			MaxPages++;
		}

		if(page > MaxPages)
		{
			page = MaxPages;
		}

		int SkillsStart = MaxSkillsPerPage * page;
		int SkillsEnd = skills.length;

		if(SkillsEnd - SkillsStart > MaxSkillsPerPage)
		{
			SkillsEnd = SkillsStart + MaxSkillsPerPage;
		}

		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		TextBuilder replyMSG = new TextBuilder("<html><body>");
		replyMSG.append("<table width=260><tr>");
		replyMSG.append("<td width=40><button value=\"Main\" action=\"bypass -h admin_admin\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=180><center>Character Selection Menu</center></td>");
		replyMSG.append("<td width=40><button value=\"Back\" action=\"bypass -h admin_show_skills\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table>");
		replyMSG.append("<br><br>");
		replyMSG.append("<center>Editing <font color=\"LEVEL\">" + player.getName() + "</font></center>");
		replyMSG.append("<br><table width=270><tr><td>Lv: " + player.getLevel() + " " + player.getTemplate().className + "</td></tr></table>");
		replyMSG.append("<br><table width=270><tr><td>Note: Dont forget that modifying players skills can</td></tr>");
		replyMSG.append("<tr><td>ruin the game...</td></tr></table>");
		replyMSG.append("<br><center>Click on the skill you wish to remove:</center>");
		replyMSG.append("<br>");
		String pages = "<center><table width=270><tr>";

		for(int x = 0; x < MaxPages; x++)
		{
			int pagenr = x + 1;
			pages += "<td><a action=\"bypass -h admin_remove_skills " + x + "\">Page " + pagenr + "</a></td>";
		}

		pages += "</tr></table></center>";
		replyMSG.append(pages);
		replyMSG.append("<br><table width=270>");
		replyMSG.append("<tr><td width=80>Name:</td><td width=60>Level:</td><td width=40>Id:</td></tr>");

		for(int i = SkillsStart; i < SkillsEnd; i++)
		{
			replyMSG.append("<tr><td width=80><a action=\"bypass -h admin_remove_skill " + skills[i].getId() + "\">" + skills[i].getName() + "</a></td><td width=60>" + skills[i].getLevel() + "</td><td width=40>" + skills[i].getId() + "</td></tr>");
		}

		replyMSG.append("</table>");
		replyMSG.append("<br><center><table>");
		replyMSG.append("Remove skill by ID :");
		replyMSG.append("<tr><td>Id: </td>");
		replyMSG.append("<td><edit var=\"id_to_remove\" width=110></td></tr>");
		replyMSG.append("</table></center>");
		replyMSG.append("<center><button value=\"Remove skill\" action=\"bypass -h admin_remove_skill $id_to_remove\" width=110 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center>");
		replyMSG.append("<br><center><button value=\"Back\" action=\"bypass -h admin_current_player\" width=40 height=15></center>");
		replyMSG.append("</body></html>");
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private void showMainPage(L2PcInstance activeChar)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;

		if(target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			return;
		}

		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/game/charskills.htm");
		adminReply.replace("%name%", player.getName());
		adminReply.replace("%level%", String.valueOf(player.getLevel()));
		adminReply.replace("%class%", player.getTemplate().className);
		activeChar.sendPacket(adminReply);
	}

	private void adminGetSkills(L2PcInstance activeChar)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;

		if(target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			return;
		}

		if(player.getName().equals(activeChar.getName()))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_USE_ON_YOURSELF));
		}
		else
		{
			L2Skill[] skills = player.getAllSkills();
			adminSkills = activeChar.getAllSkills();

			for(L2Skill adminSkill : adminSkills)
			{
				activeChar.removeSkill(adminSkill);
			}

			for(L2Skill skill : skills)
			{
				activeChar.addSkill(skill, true);
			}

			activeChar.sendChatMessage(0, 0, "SYS", "You now have all the skills of " + player.getName() + ".");
			activeChar.sendSkillList();
		}

		showMainPage(activeChar);
	}

	private void adminResetSkills(L2PcInstance activeChar)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;

		if(target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			return;
		}

		if(adminSkills == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "You must get the skills of someone in order to do this.");
		}
		else
		{
			L2Skill[] skills = player.getAllSkills();

			for(L2Skill skill : skills)
			{
				player.removeSkill(skill);
			}

			for(int i = 0; i < activeChar.getAllSkills().length; i++)
			{
				player.addSkill(activeChar.getAllSkills()[i], true);
			}

			for(L2Skill skill : skills)
			{
				activeChar.removeSkill(skill);
			}

			for(L2Skill adminSkill : adminSkills)
			{
				activeChar.addSkill(adminSkill, true);
			}

			player.sendChatMessage(0, 0, "SYS", "[GM]" + activeChar.getName() + " updated your skills.");
			activeChar.sendChatMessage(0, 0, "SYS", "You now have all your skills back.");
			adminSkills = null;
			activeChar.sendSkillList();
		}
		showMainPage(activeChar);
	}

	private void adminAddSkill(L2PcInstance activeChar, String val)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;

		if(target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			showMainPage(activeChar);
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			return;
		}

		StringTokenizer st = new StringTokenizer(val);

		if(st.countTokens() != 2)
		{
			showMainPage(activeChar);
		}
		else
		{
			L2Skill skill = null;

			try
			{
				String id = st.nextToken();
				String level = st.nextToken();

				int idval = Integer.parseInt(id);
				int levelval = Integer.parseInt(level);

				skill = SkillTable.getInstance().getInfo(idval, levelval);
			}
			catch(Exception e)
			{
			}

			if(skill != null)
			{
				String name = skill.getName();
				player.sendChatMessage(0, 0, "SYS", "Admin gave you the skill " + name + ".");
				player.addSkill(skill, true);
				activeChar.sendChatMessage(0, 0, "SYS", "You gave the skill " + name + " to " + player.getName() + ".");

				activeChar.sendSkillList();
			}
			else
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Error: there is no such skill.");
			}
			showMainPage(activeChar);
		}
	}

	private void adminRemoveSkill(L2PcInstance activeChar, int idval)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;

		if(target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			return;
		}

		L2Skill skill = SkillTable.getInstance().getInfo(idval, player.getSkillLevel(idval));

		if(skill != null)
		{
			String skillname = skill.getName();
			player.sendChatMessage(0, 0, "SYS", "Admin removed the skill " + skillname + " from your skills list.");
			player.removeSkill(skill);
			activeChar.sendChatMessage(0, 0, "SYS", "You removed the skill " + skillname + " from " + player.getName() + ".");

			activeChar.sendSkillList();
		}
		else
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Error: there is no such skill.");
		}
		removeSkillsPage(activeChar, 0);
	}

	private void adminAddClanSkill(L2PcInstance activeChar, int id, int level)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;

		if(target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			showMainPage(activeChar);
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			return;
		}

		if(!player.isClanLeader())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_IS_NOT_A_CLAN_LEADER).addString(player.getName()));
			showMainPage(activeChar);
			return;
		}

		if(id < 370 || id > 391 || level < 1 || level > 3)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Usage: //add_clan_skill <skill_id> <level>");
			showMainPage(activeChar);
			return;
		}
		else
		{
			L2Skill skill = SkillTable.getInstance().getInfo(id, level);

			if(skill != null)
			{
				String skillname = skill.getName();
				player.getClan().broadcastToOnlineMembers(new SystemMessage(SystemMessageId.CLAN_SKILL_S1_ADDED).addSkillName(id));
				player.getClan().addNewSkill(skill);
				activeChar.sendChatMessage(0, 0, "SYS", "You gave the Clan Skill: " + skillname + " to the clan " + player.getClan().getName() + ".");

				activeChar.getClan().broadcastToOnlineMembers(new PledgeSkillList(activeChar.getClan()));

				for(L2PcInstance member : activeChar.getClan().getOnlineMembers(""))
				{
					member.sendSkillList();
				}
				showMainPage(activeChar);
				return;
			}
			else
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Error: there is no such skill.");
				return;
			}
		}
	}
}