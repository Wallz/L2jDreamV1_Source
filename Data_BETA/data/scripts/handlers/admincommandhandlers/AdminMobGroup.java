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

import com.src.gameserver.datatables.MobGroupTable;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.MobGroup;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.MagicSkillUser;
import com.src.gameserver.network.serverpackets.SetupGauge;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.util.Broadcast;

public class AdminMobGroup implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
			"admin_mobmenu",
			"admin_mobgroup_list",
			"admin_mobgroup_create",
			"admin_mobgroup_remove",
			"admin_mobgroup_delete",
			"admin_mobgroup_spawn",
			"admin_mobgroup_unspawn",
			"admin_mobgroup_kill",
			"admin_mobgroup_idle",
			"admin_mobgroup_attack",
			"admin_mobgroup_rnd",
			"admin_mobgroup_return",
			"admin_mobgroup_follow",
			"admin_mobgroup_casting",
			"admin_mobgroup_nomove",
			"admin_mobgroup_attackgrp",
			"admin_mobgroup_invul",
			"admin_mobinst"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		GMAudit.auditGMAction(activeChar.getName(), command, "", "");

		if(command.equals("admin_mobmenu"))
		{
			showMainPage(activeChar, command);
			return true;
		}
		else if(command.equals("admin_mobinst"))
		{
			showMainPage(activeChar, command);
			return true;
		}
		else if(command.equals("admin_mobgroup_list"))
		{
			showGroupList(activeChar);
		}
		else if(command.startsWith("admin_mobgroup_create"))
		{
			createGroup(command, activeChar);
		}
		else if(command.startsWith("admin_mobgroup_delete") || command.startsWith("admin_mobgroup_remove"))
		{
			removeGroup(command, activeChar);
		}
		else if(command.startsWith("admin_mobgroup_spawn"))
		{
			spawnGroup(command, activeChar);
		}
		else if(command.startsWith("admin_mobgroup_unspawn"))
		{
			unspawnGroup(command, activeChar);
		}
		else if(command.startsWith("admin_mobgroup_kill"))
		{
			killGroup(command, activeChar);
		}
		else if(command.startsWith("admin_mobgroup_attackgrp"))
		{
			attackGrp(command, activeChar);
		}
		else if(command.startsWith("admin_mobgroup_attack"))
		{
			if(activeChar.getTarget() instanceof L2Character)
			{
				L2Character target = (L2Character) activeChar.getTarget();
				attack(command, activeChar, target);
				target = null;
			}
		}
		else if(command.startsWith("admin_mobgroup_rnd"))
		{
			setNormal(command, activeChar);
		}
		else if(command.startsWith("admin_mobgroup_idle"))
		{
			idle(command, activeChar);
		}
		else if(command.startsWith("admin_mobgroup_return"))
		{
			returnToChar(command, activeChar);
		}
		else if(command.startsWith("admin_mobgroup_follow"))
		{
			follow(command, activeChar, activeChar);
		}
		else if(command.startsWith("admin_mobgroup_casting"))
		{
			setCasting(command, activeChar);
		}
		else if(command.startsWith("admin_mobgroup_nomove"))
		{
			noMove(command, activeChar);
		}
		else if(command.startsWith("admin_mobgroup_invul"))
		{
			invul(command, activeChar);
		}
		else if(command.startsWith("admin_mobgroup_teleport"))
		{
			teleportGroup(command, activeChar);
		}

		showMainPage(activeChar, command);

		return true;
	}

	private void showMainPage(L2PcInstance activeChar, String command)
	{
		String filename = "effects/mobgroup.htm";

		if(command.contains("mobinst"))
		{
			filename = "effects/mobgrouphelp.htm";
		}

		AdminHelpPage.showHelpPage(activeChar, filename);

		filename = null;
	}

	private void returnToChar(String command, L2PcInstance activeChar)
	{
		int groupId;

		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
		}
		catch(Exception e)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Incorrect command arguments.");
			return;
		}

		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);

		if(group == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Invalid group specified.");
			return;
		}

		group.returnGroup(activeChar);

		group = null;
	}

	private void idle(String command, L2PcInstance activeChar)
	{
		int groupId;

		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
		}
		catch(Exception e)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Incorrect command arguments.");
			return;
		}

		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);

		if(group == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Invalid group specified.");
			return;
		}

		group.setIdleMode();

		group = null;
	}

	private void setNormal(String command, L2PcInstance activeChar)
	{
		int groupId;

		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
		}
		catch(Exception e)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Incorrect command arguments.");
			return;
		}

		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);

		if(group == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Invalid group specified.");
			return;
		}

		group.setAttackRandom();

		group = null;
	}

	private void attack(String command, L2PcInstance activeChar, L2Character target)
	{
		int groupId;

		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
		}
		catch(Exception e)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Incorrect command arguments.");
			return;
		}

		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);

		if(group == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Invalid group specified.");
			return;
		}

		group.setAttackTarget(target);

		group = null;
	}

	private void follow(String command, L2PcInstance activeChar, L2Character target)
	{
		int groupId;

		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
		}
		catch(Exception e)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Incorrect command arguments.");
			return;
		}

		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);

		if(group == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Invalid group specified.");
			return;
		}

		group.setFollowMode(target);
		group = null;
	}

	private void createGroup(String command, L2PcInstance activeChar)
	{
		int groupId;
		int templateId;
		int mobCount;

		try
		{
			String[] cmdParams = command.split(" ");

			groupId = Integer.parseInt(cmdParams[1]);
			templateId = Integer.parseInt(cmdParams[2]);
			mobCount = Integer.parseInt(cmdParams[3]);
			cmdParams = null;
		}
		catch(Exception e)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Usage: //mobgroup_create <group> <npcid> <count>");
			return;
		}

		if(MobGroupTable.getInstance().getGroup(groupId) != null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Mob group " + groupId + " already exists.");
			return;
		}

		L2NpcTemplate template = NpcTable.getInstance().getTemplate(templateId);

		if(template == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Invalid NPC ID specified.");
			return;
		}

		MobGroup group = new MobGroup(groupId, template, mobCount);
		MobGroupTable.getInstance().addGroup(groupId, group);

		activeChar.sendChatMessage(0, 0, "SYS", "Mob group " + groupId + " created.");

		template = null;
		group = null;
	}

	private void removeGroup(String command, L2PcInstance activeChar)
	{
		int groupId;

		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
		}
		catch(Exception e)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Usage: //mobgroup_remove <groupId>");
			return;
		}

		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);

		if(group == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Invalid group specified.");
			return;
		}

		doAnimation(activeChar);
		group.unspawnGroup();

		if(MobGroupTable.getInstance().removeGroup(groupId))
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Mob group " + groupId + " unspawned and removed.");
		}

		group = null;
	}

	private void spawnGroup(String command, L2PcInstance activeChar)
	{
		int groupId;
		boolean topos = false;
		int posx = 0;
		int posy = 0;
		int posz = 0;

		try
		{
			String[] cmdParams = command.split(" ");
			groupId = Integer.parseInt(cmdParams[1]);

			try
			{
				posx = Integer.parseInt(cmdParams[2]);
				posy = Integer.parseInt(cmdParams[3]);
				posz = Integer.parseInt(cmdParams[4]);
				topos = true;
			}
			catch(Exception e)
			{
			}

			cmdParams = null;
		}
		catch(Exception e)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Usage: //mobgroup_spawn <group> [ x y z ]");
			return;
		}

		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);

		if(group == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Invalid group specified.");
			return;
		}

		doAnimation(activeChar);

		if(topos)
		{
			group.spawnGroup(posx, posy, posz);
		}
		else
		{
			group.spawnGroup(activeChar);
		}

		activeChar.sendChatMessage(0, 0, "SYS", "Mob group " + groupId + " spawned.");

		group = null;
	}

	private void unspawnGroup(String command, L2PcInstance activeChar)
	{
		int groupId;

		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
		}
		catch(Exception e)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Usage: //mobgroup_unspawn <groupId>");
			return;
		}

		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);

		if(group == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Invalid group specified.");
			return;
		}

		doAnimation(activeChar);
		group.unspawnGroup();

		activeChar.sendChatMessage(0, 0, "SYS", "Mob group " + groupId + " unspawned.");

		group = null;
	}

	private void killGroup(String command, L2PcInstance activeChar)
	{
		int groupId;

		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
		}
		catch(Exception e)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Usage: //mobgroup_kill <groupId>");
			return;
		}

		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);

		if(group == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Invalid group specified.");
			return;
		}

		doAnimation(activeChar);
		group.killGroup(activeChar);

		group = null;
	}

	private void setCasting(String command, L2PcInstance activeChar)
	{
		int groupId;

		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
		}
		catch(Exception e)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Usage: //mobgroup_casting <groupId>");
			return;
		}

		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);

		if(group == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Invalid group specified.");
			return;
		}

		group.setCastMode();

		group = null;
	}

	private void noMove(String command, L2PcInstance activeChar)
	{
		int groupId;

		String enabled;

		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
			enabled = command.split(" ")[2];
		}
		catch(Exception e)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Usage: //mobgroup_nomove <groupId> <on|off>");
			return;
		}

		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);

		if(group == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Invalid group specified.");
			return;
		}

		if(enabled.equalsIgnoreCase("on") || enabled.equalsIgnoreCase("true"))
		{
			group.setNoMoveMode(true);
		}
		else if(enabled.equalsIgnoreCase("off") || enabled.equalsIgnoreCase("false"))
		{
			group.setNoMoveMode(false);
		}
		else
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Incorrect command arguments.");
		}

		enabled = null;
	}

	private void doAnimation(L2PcInstance activeChar)
	{
		Broadcast.toSelfAndKnownPlayersInRadius(activeChar, new MagicSkillUser(activeChar, 1008, 1, 4000, 0), 2250000/*1500*/);
		activeChar.sendPacket(new SetupGauge(0, 4000));
	}

	private void attackGrp(String command, L2PcInstance activeChar)
	{
		int groupId;
		int othGroupId;

		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
			othGroupId = Integer.parseInt(command.split(" ")[2]);
		}
		catch(Exception e)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Usage: //mobgroup_attackgrp <groupId> <TargetGroupId>");
			return;
		}

		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);

		if(group == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Invalid group specified.");
			return;
		}

		MobGroup othGroup = MobGroupTable.getInstance().getGroup(othGroupId);

		if(othGroup == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Incorrect target group.");
			return;
		}

		group.setAttackGroup(othGroup);

		group = null;
		othGroup = null;
	}

	private void invul(String command, L2PcInstance activeChar)
	{
		int groupId;

		String enabled;

		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
			enabled = command.split(" ")[2];
		}
		catch(Exception e)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Usage: //mobgroup_invul <groupId> <on|off>");
			return;
		}

		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);

		if(group == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Invalid group specified.");
			return;
		}

		if(enabled.equalsIgnoreCase("on") || enabled.equalsIgnoreCase("true"))
		{
			group.setInvul(true);
		}
		else if(enabled.equalsIgnoreCase("off") || enabled.equalsIgnoreCase("false"))
		{
			group.setInvul(false);
		}
		else
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Incorrect command arguments.");
		}

		group = null;
		enabled = null;
	}

	private void teleportGroup(String command, L2PcInstance activeChar)
	{
		int groupId;
		String targetPlayerStr = null;
		L2PcInstance targetPlayer = null;

		try
		{
			groupId = Integer.parseInt(command.split(" ")[1]);
			targetPlayerStr = command.split(" ")[2];

			if(targetPlayerStr != null)
			{
				targetPlayer = L2World.getInstance().getPlayer(targetPlayerStr);
			}

			if(targetPlayer == null)
			{
				targetPlayer = activeChar;
			}
		}
		catch(Exception e)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Usage: //mobgroup_teleport <groupId> [playerName]");
			return;
		}

		MobGroup group = MobGroupTable.getInstance().getGroup(groupId);

		if(group == null)
		{
			activeChar.sendChatMessage(0, 0, "SYS", "Invalid group specified.");
			return;
		}

		group.teleportGroup(activeChar);

		group = null;
		targetPlayer = null;
		targetPlayerStr = null;
	}

	private void showGroupList(L2PcInstance activeChar)
	{
		MobGroup[] mobGroupList = MobGroupTable.getInstance().getGroups();

		activeChar.sendChatMessage(0, 0, "SYS", "======= <Mob Groups> =======");

		for(MobGroup mobGroup : mobGroupList)
		{
			activeChar.sendChatMessage(0, 0, "SYS", mobGroup.getGroupId() + ": " + mobGroup.getActiveMobCount() + " alive out of " + mobGroup.getMaxMobCount() + " of NPC ID " + mobGroup.getTemplate().npcId + " (" + mobGroup.getStatus() + ")");
		}

		activeChar.sendPacket(new SystemMessage(SystemMessageId.FRIEND_LIST_FOOTER));

		mobGroupList = null;
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

}