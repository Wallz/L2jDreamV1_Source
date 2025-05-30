/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.admincommandhandlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javolution.text.TextBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.model.base.ClassId;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.SetSummonRemainTime;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.util.Util;

public class AdminEditChar implements IAdminCommandHandler
{
	private final static Log _log = LogFactory.getLog(AdminEditChar.class.getName());

	private static String[] ADMIN_COMMANDS =
	{
		"admin_setname",
		"admin_edit_character",
		"admin_current_player",
		"admin_nokarma",
		"admin_setkarma",
		"admin_character_list",
		"admin_character_info",
		"admin_show_characters",
		"admin_find_character",
		"admin_find_dualbox",
		"admin_find_ip",
		"admin_find_account",
		"admin_save_modifications",
		"admin_rec",
		"admin_setclass",
		"admin_settitle",
		"admin_setsex",
		"admin_setcolor",
		"admin_fullfood",
		"admin_remclanwait",
		"admin_setcp",
		"admin_sethp",
		"admin_setmp",
		"admin_setchar_cp",
		"admin_setchar_hp",
		"admin_setchar_mp"
	};

	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		AdminCommandAccessRights.getInstance().hasAccess(command, activeChar.getAccessLevel());

		GMAudit.auditGMAction(activeChar.getName(), command, (activeChar.getTarget() != null?activeChar.getTarget().getName():"no-target"), "");

		if(command.equals("admin_current_player"))
		{
			showCharacterInfo(activeChar, null);
		}
		else if(command.startsWith("admin_character_list") || command.startsWith("admin_character_info"))
		{
			try
			{
				String val = command.substring(21);
				L2PcInstance target = L2World.getInstance().getPlayer(val);

				if(target != null)
				{
					showCharacterInfo(activeChar, target);
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.CHARACTER_DOES_NOT_EXIST));
				}
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //character_info <player_name>");
			}
		}
		else if(command.startsWith("admin_show_characters"))
		{
			try
			{
				String val = command.substring(22);
				int page = Integer.parseInt(val);
				listCharacters(activeChar, page);
			}
			catch(StringIndexOutOfBoundsException e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //show_characters <page_number>");
			}
		}
		else if(command.startsWith("admin_find_character"))
		{
			try
			{
				String val = command.substring(21);
				findCharacter(activeChar, val);
			}
			catch(Exception e)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.GM_S1).addString("You didnt enter a character name to find."));

				listCharacters(activeChar, 0);
			}
		}
		else if(command.startsWith("admin_find_ip"))
		{
			try
			{
				String val = command.substring(14);
				findCharactersPerIp(activeChar, val);
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //find_ip <www.xxx.yyy.zzz>");
				listCharacters(activeChar, 0);
			}
		}
		else if(command.startsWith("admin_find_dualbox"))
		{
			int multibox = 2;
			try
			{
				String val = command.substring(19);
				multibox = Integer.parseInt(val);
			}
			catch(Exception e)
			{
				
			}
			findDualbox(activeChar, multibox);
		}
		else if(command.startsWith("admin_find_account"))
		{
			try
			{
				String val = command.substring(19);
				findCharactersPerAccount(activeChar, val);
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //find_account <player_name>");
				listCharacters(activeChar, 0);
			}
		}
		else if(command.equals("admin_edit_character"))
		{
			editCharacter(activeChar);
		}
		else if(command.equals("admin_nokarma"))
		{
			setTargetKarma(activeChar, 0);
		}
		else if(command.startsWith("admin_setkarma"))
		{
			try
			{
				String val = command.substring(15);
				int karma = Integer.parseInt(val);

				GMAudit.auditGMAction(activeChar.getName(), command, activeChar.getName(), "");
				setTargetKarma(activeChar, karma);
			}
			catch(Exception e)
			{
				
			}
		}
		else if(command.startsWith("admin_save_modifications"))
		{
			try
			{
				String val = command.substring(24);

				GMAudit.auditGMAction(activeChar.getName(), command, activeChar.getName(), "");

				adminModifyCharacter(activeChar, val);
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Error while modifying character.");
				listCharacters(activeChar, 0);
			}
		}
		else if(command.equals("admin_rec"))
		{
			L2Object target = activeChar.getTarget();
			L2PcInstance player = null;

			if(target instanceof L2PcInstance)
			{
				player = (L2PcInstance) target;
			}
			else
			{
				return false;
			}

			player.setRecomHave(player.getRecomHave() + 1);
			player.sendPacket(new SystemMessage(SystemMessageId.GM_S1).addString("You have been recommended by a GM"));
			player.broadcastUserInfo();
		}
		else if(command.startsWith("admin_rec"))
		{
			try
			{
				String val = command.substring(10);

				int recVal = Integer.parseInt(val);

				L2Object target = activeChar.getTarget();
				L2PcInstance player = null;

				if(target instanceof L2PcInstance)
				{
					player = (L2PcInstance) target;
				}
				else
				{
					return false;
				}

				player.setRecomHave(player.getRecomHave() + recVal);
				player.sendPacket(new SystemMessage(SystemMessageId.GM_S1).addString("You have been recommended by a GM"));
				player.broadcastUserInfo();
			}
			catch(NumberFormatException nfe)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "You must specify the number of recommendations to add.");
			}
		}
		else if(command.startsWith("admin_setclass"))
		{
			try
			{
				String val = command.substring(15);

				int classidval = Integer.parseInt(val);

				L2Object target = activeChar.getTarget();
				L2PcInstance player = null;

				if(target instanceof L2PcInstance)
				{
					player = (L2PcInstance) target;
				}
				else
				{
					return false;
				}

				boolean valid = false;

				for(ClassId classid : ClassId.values())
				{
					if(classidval == classid.getId())
					{
						valid = true;
					}
				}
				if(valid && player.getClassId().getId() != classidval)
				{
					player.setClassId(classidval);

					if(!player.isSubClassActive())
					{
						player.setBaseClass(classidval);
					}

					String newclass = player.getTemplate().className;
					player.store();
					player.broadcastUserInfo();

					if(player != activeChar)
					{
						player.sendChatMessage(0, 0, "SYS", "A GM changed your class to " + newclass + ".");
					}

					activeChar.sendChatMessage(0, 0, "SYS", player.getName() + " changed to " + newclass + ".");

					newclass = null;
				}
				else
				{
					activeChar.sendChatMessage(0, 0, "SYS", "Usage: //setclass <valid_new_classid>");
				}
			}
			catch(Exception e)
			{
				AdminHelpPage.showSubMenuPage(activeChar, "game/charclasses.htm");
			}
		}
		else if(command.startsWith("admin_settitle"))
		{
			String val = "";
			StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			L2Object target = activeChar.getTarget();
			L2PcInstance player = null;
			L2Npc npc = null;

			if(target == null)
			{
				player = activeChar;
			}
			else if(target instanceof L2PcInstance)
			{
				player = (L2PcInstance) target;
			}
			else if(target instanceof L2Npc)
			{
				npc = (L2Npc) target;
			}
			else
			{
				return false;
			}

			target = null;

			if(st.hasMoreTokens())
			{
				val = st.nextToken();
			}
			while(st.hasMoreTokens())
			{
				val += " " + st.nextToken();
			}

			st = null;

			if(player != null)
			{
				player.setTitle(val);
				if(player != activeChar)
				{
					player.sendChatMessage(0, 0, "SYS", "Your title has been changed by a GM");
				}
				player.broadcastTitleInfo();
			}
			else if(npc != null)
			{
				npc.setTitle(val);
				npc.updateAbnormalEffect();
			}
		}
		else if(command.startsWith("admin_setname"))
		{
			try
			{
				String val = command.substring(14);
				L2Object target = activeChar.getTarget();
				L2PcInstance player = null;
				
				if (target instanceof L2PcInstance)
					player = (L2PcInstance)target;
				else
					return false;
				
				player.setName(val);
				player.sendChatMessage(0, 0, "SYS", "Your name has been changed by a GM.");
				player.broadcastUserInfo();
				player.store();
			}
			catch (StringIndexOutOfBoundsException e)
			{
				//Case of empty character name
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //setname name");
			}
		}
		else if(command.startsWith("admin_setsex"))
		{
			L2Object target = activeChar.getTarget();
			L2PcInstance player = null;

			if(target instanceof L2PcInstance)
			{
				player = (L2PcInstance) target;
			}
			else
			{
				return false;
			}

			player.getAppearance().setSex(player.getAppearance().getSex() ? false : true);
			L2PcInstance.setSexDB(player, 1);
			player.sendChatMessage(0, 0, "SYS", "Your gender has been changed by a GM");
			player.decayMe();
			player.spawnMe(player.getX(), player.getY(), player.getZ());
			player.broadcastUserInfo();
		}
		else if(command.startsWith("admin_setcolor"))
		{
			L2Object target = activeChar.getTarget();

			if(target == null)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "You have to select a player!");
				return false;
			}

			if(!(target instanceof L2PcInstance))
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Your target is not a player!");
				return false;
			}

			try
			{
				StringTokenizer st = new StringTokenizer(command);
				st.nextToken();
				String val = st.nextToken();
				L2PcInstance player = (L2PcInstance) target;
				player.getAppearance().setNameColor(Integer.decode("0x" + val));
				player.sendChatMessage(0, 0, "SYS", "Your name color has been changed by a GM");
				player.broadcastUserInfo();
			}
			catch(Exception e)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Usage: //setcolor [color in HEX]");
			}
		}
		else if(command.startsWith("admin_fullfood"))
		{
			L2Object target = activeChar.getTarget();

			if(target instanceof L2PetInstance)
			{
				L2PetInstance targetPet = (L2PetInstance) target;
				targetPet.setCurrentFed(targetPet.getMaxFed());
				targetPet.getOwner().sendPacket(new SetSummonRemainTime(targetPet.getMaxFed(), targetPet.getCurrentFed()));
			}
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			}
		}
		else if(command.equals("admin_remclanwait"))
		{
			L2Object target = activeChar.getTarget();
			L2PcInstance player = null;

			if(target instanceof L2PcInstance)
			{
				player = (L2PcInstance) target;
			}
			else
				return false;

			if(player.getClan() == null)
			{
				player.setClanJoinExpiryTime(0);
				player.sendChatMessage(0, 0, "SYS", "A GM Has reset your clan wait time, You may now join another clan.");
				activeChar.sendChatMessage(0, 0, "SYS", "You have reset " + player.getName() + "'s wait time to join another clan.");
			}
			else
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Sorry, but " + player.getName() + " must not be in a clan. Player must leave clan before the wait limit can be reset.");
			}
		}

		StringTokenizer st2 = new StringTokenizer(command, " ");
		String cmand = st2.nextToken();

		if(cmand.equals("admin_setcp"))
		{
			int cp = 0;

			try
			{
				cp = Integer.parseInt(st2.nextToken());

			}
			catch(Exception e)
			{
				_log.error("" + e.getMessage());
			}
			activeChar.getStatus().setCurrentCp(cp);
		}

		if(cmand.equals("admin_sethp"))
		{
			int hp = 0;

			try
			{
				hp = Integer.parseInt(st2.nextToken());
			}
			catch(Exception e)
			{
				_log.error("" + e.getMessage());
			}

			activeChar.getStatus().setCurrentHp(hp);
		}

		if(cmand.equals("admin_setmp"))
		{
			int mp = 0;

			try
			{
				mp = Integer.parseInt(st2.nextToken());

			}
			catch(Exception e)
			{
				_log.error("" + e.getMessage());
			}
			activeChar.getStatus().setCurrentMp(mp);
		}

		if(cmand.equals("admin_setchar_cp"))
		{
			int cp = 0;

			try
			{
				cp = Integer.parseInt(st2.nextToken());
			}
			catch(Exception e)
			{
				_log.error("" + e.getMessage());
			}

			if(activeChar.getTarget() instanceof L2PcInstance)
			{
				L2PcInstance pc = (L2PcInstance) activeChar.getTarget();
				pc.getStatus().setCurrentCp(cp);
			}
			else if(activeChar.getTarget() instanceof L2PetInstance)
			{
				L2PetInstance pet = (L2PetInstance) activeChar.getTarget();
				pet.getStatus().setCurrentCp(cp);
			}
			else
			{
				activeChar.getStatus().setCurrentCp(cp);
			}
		}

		if(cmand.equals("admin_setchar_hp"))
		{
			int hp = 0;

			try
			{
				hp = Integer.parseInt(st2.nextToken());
			}
			catch(Exception e)
			{
				_log.error("" + e.getMessage());
			}

			if(activeChar.getTarget() instanceof L2PcInstance)
			{
				L2PcInstance pc = (L2PcInstance) activeChar.getTarget();
				pc.getStatus().setCurrentHp(hp);
				pc = null;
			}
			else if(activeChar.getTarget() instanceof L2PetInstance)
			{
				L2PetInstance pet = (L2PetInstance) activeChar.getTarget();
				pet.getStatus().setCurrentHp(hp);
				pet = null;
			}
			else
			{
				activeChar.getStatus().setCurrentHp(hp);
			}
		}

		if(cmand.equals("admin_setchar_mp"))
		{
			int mp = 0;

			try
			{
				mp = Integer.parseInt(st2.nextToken());
			}
			catch(Exception e)
			{
				_log.error("" + e.getMessage());
			}

			if(activeChar.getTarget() instanceof L2PcInstance)
			{
				L2PcInstance pc = (L2PcInstance) activeChar.getTarget();
				pc.getStatus().setCurrentMp(mp);
			}
			else if(activeChar.getTarget() instanceof L2PetInstance)
			{
				L2PetInstance pet = (L2PetInstance) activeChar.getTarget();
				pet.getStatus().setCurrentMp(mp);
			}
			else
			{
				activeChar.getStatus().setCurrentMp(mp);
			}
		}

		st2 = null;

		return true;
	}

	private void listCharacters(L2PcInstance activeChar, int page)
	{
		Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers();
		L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);
		allPlayers = null;

		int MaxCharactersPerPage = 20;
		int MaxPages = players.length / MaxCharactersPerPage;

		if(players.length > MaxCharactersPerPage * MaxPages)
		{
			MaxPages++;
		}

		if(page > MaxPages)
		{
			page = MaxPages;
		}

		int CharactersStart = MaxCharactersPerPage * page;
		int CharactersEnd = players.length;

		if(CharactersEnd - CharactersStart > MaxCharactersPerPage)
		{
			CharactersEnd = CharactersStart + MaxCharactersPerPage;
		}

		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/game/charlist.htm");
		TextBuilder replyMSG = new TextBuilder();

		for(int x = 0; x < MaxPages; x++)
		{
			int pagenr = x + 1;
			replyMSG.append("<center><a action=\"bypass -h admin_show_characters " + x + "\">Page " + pagenr + "</a></center>");
		}

		adminReply.replace("%pages%", replyMSG.toString());
		replyMSG.clear();

		for(int i = CharactersStart; i < CharactersEnd; i++)
		{
			replyMSG.append("<tr><td width=80><a action=\"bypass -h admin_character_info " + players[i].getName() + "\">" + players[i].getName() + "</a></td><td width=110>" + players[i].getTemplate().className + "</td><td width=40>" + players[i].getLevel() + "</td></tr>");
		}

		adminReply.replace("%players%", replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	public static void gatherCharacterInfo(L2PcInstance activeChar, L2PcInstance player, String filename)
	{
		String ip = "N/A";
		String account = "N/A";

		try
		{
			StringTokenizer clientinfo = new StringTokenizer(player.getClient().toString(), " ]:-[");
			clientinfo.nextToken();
			clientinfo.nextToken();
			clientinfo.nextToken();
			account = clientinfo.nextToken();
			clientinfo.nextToken();
			ip = clientinfo.nextToken();
			clientinfo = null;
		}
		catch(Exception e)
		{
		}

		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/" + filename);
		adminReply.replace("%name%", player.getName());
		adminReply.replace("%level%", String.valueOf(player.getLevel()));
		adminReply.replace("%clan%", String.valueOf(ClanTable.getInstance().getClan(player.getClanId())));
		adminReply.replace("%xp%", String.valueOf(player.getExp()));
		adminReply.replace("%sp%", String.valueOf(player.getSp()));
		adminReply.replace("%class%", player.getTemplate().className);
		adminReply.replace("%ordinal%", String.valueOf(player.getClassId().ordinal()));
		adminReply.replace("%classid%", String.valueOf(player.getClassId()));
		adminReply.replace("%x%", String.valueOf(player.getX()));
		adminReply.replace("%y%", String.valueOf(player.getY()));
		adminReply.replace("%z%", String.valueOf(player.getZ()));
		adminReply.replace("%currenthp%", String.valueOf((int) player.getCurrentHp()));
		adminReply.replace("%maxhp%", String.valueOf(player.getMaxHp()));
		adminReply.replace("%karma%", String.valueOf(player.getKarma()));
		adminReply.replace("%currentmp%", String.valueOf((int) player.getCurrentMp()));
		adminReply.replace("%maxmp%", String.valueOf(player.getMaxMp()));
		adminReply.replace("%pvpflag%", String.valueOf(player.getPvpFlag()));
		adminReply.replace("%currentcp%", String.valueOf((int) player.getCurrentCp()));
		adminReply.replace("%maxcp%", String.valueOf(player.getMaxCp()));
		adminReply.replace("%pvpkills%", String.valueOf(player.getPvpKills()));
		adminReply.replace("%pkkills%", String.valueOf(player.getPkKills()));
		adminReply.replace("%currentload%", String.valueOf(player.getCurrentLoad()));
		adminReply.replace("%maxload%", String.valueOf(player.getMaxLoad()));
		adminReply.replace("%percent%", String.valueOf(Util.roundTo((float) player.getCurrentLoad() / (float) player.getMaxLoad() * 100, 2)));
		adminReply.replace("%patk%", String.valueOf(player.getPAtk(null)));
		adminReply.replace("%matk%", String.valueOf(player.getMAtk(null, null)));
		adminReply.replace("%pdef%", String.valueOf(player.getPDef(null)));
		adminReply.replace("%mdef%", String.valueOf(player.getMDef(null, null)));
		adminReply.replace("%accuracy%", String.valueOf(player.getAccuracy()));
		adminReply.replace("%evasion%", String.valueOf(player.getEvasionRate(null)));
		adminReply.replace("%critical%", String.valueOf(player.getCriticalHit(null, null)));
		adminReply.replace("%runspeed%", String.valueOf(player.getRunSpeed()));
		adminReply.replace("%patkspd%", String.valueOf(player.getPAtkSpd()));
		adminReply.replace("%matkspd%", String.valueOf(player.getMAtkSpd()));
		adminReply.replace("%access%", String.valueOf(player.getAccessLevel().getLevel()));
		adminReply.replace("%account%", account);
		adminReply.replace("%ip%", ip);
		activeChar.sendPacket(adminReply);
	}

	private void setTargetKarma(L2PcInstance activeChar, int newKarma)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;

		if(target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
		}
		else
		{
			return;
		}

		if(newKarma >= 0)
		{
			int oldKarma = player.getKarma();

			player.setKarma(newKarma);

			StatusUpdate su = new StatusUpdate(player.getObjectId());
			su.addAttribute(StatusUpdate.KARMA, newKarma);
			player.sendPacket(su);
			su = null;

			player.sendPacket(new SystemMessage(SystemMessageId.GM_S1).addString("Admin has changed your karma from " + oldKarma + " to " + newKarma + "."));

			if(player != activeChar)
			{
				activeChar.sendChatMessage(0, 0, "SYS", "Successfully Changed karma for " + player.getName() + " from (" + oldKarma + ") to (" + newKarma + ").");
			}
		}
		else
		{
			activeChar.sendChatMessage(0, 0, "SYS", "You must enter a value for karma greater than or equal to 0.");
		}
	}

	private void adminModifyCharacter(L2PcInstance activeChar, String modifications)
	{
		L2Object target = activeChar.getTarget();

		if(!(target instanceof L2PcInstance))
		{
			return;
		}

		L2PcInstance player = (L2PcInstance) target;
		StringTokenizer st = new StringTokenizer(modifications);

		if(st.countTokens() != 6)
		{
			editCharacter(player);
			return;
		}

		String hp = st.nextToken();
		String mp = st.nextToken();
		String cp = st.nextToken();
		String pvpflag = st.nextToken();
		String pvpkills = st.nextToken();
		String pkkills = st.nextToken();

		int hpval = Integer.parseInt(hp);
		int mpval = Integer.parseInt(mp);
		int cpval = Integer.parseInt(cp);
		int pvpflagval = Integer.parseInt(pvpflag);
		int pvpkillsval = Integer.parseInt(pvpkills);
		int pkkillsval = Integer.parseInt(pkkills);

		player.sendChatMessage(0, 0, "SYS", "Admin has changed your stats." + "  HP: " + hpval + "  MP: " + mpval + "  CP: " + cpval + "  PvP Flag: " + pvpflagval + " PvP/PK " + pvpkillsval + "/" + pkkillsval);
		player.getStatus().setCurrentHp(hpval);
		player.getStatus().setCurrentMp(mpval);
		player.getStatus().setCurrentCp(cpval);
		player.setPvpFlag(pvpflagval);
		player.setPvpKills(pvpkillsval);
		player.updatePvPColor();
		player.setPkKills(pkkillsval);
		player.updatePkColor();

		player.store();

		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_HP, hpval);
		su.addAttribute(StatusUpdate.MAX_HP, player.getMaxHp());
		su.addAttribute(StatusUpdate.CUR_MP, mpval);
		su.addAttribute(StatusUpdate.MAX_MP, player.getMaxMp());
		su.addAttribute(StatusUpdate.CUR_CP, cpval);
		su.addAttribute(StatusUpdate.MAX_CP, player.getMaxCp());
		player.sendPacket(su);

		player.sendChatMessage(0, 0, "SYS", "Changed stats of " + player.getName() + "." + "  HP: " + hpval + "  MP: " + mpval + "  CP: " + cpval + "  PvP: " + pvpflagval + " / " + pvpkillsval);

		showCharacterInfo(activeChar, null);

		player.broadcastUserInfo();
		player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		player.decayMe();
		player.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());
	}

	private void editCharacter(L2PcInstance activeChar)
	{
		L2Object target = activeChar.getTarget();

		if(!(target instanceof L2PcInstance))
		{
			return;
		}

		L2PcInstance player = (L2PcInstance) target;
		gatherCharacterInfo(activeChar, player, "game/charedit.htm");
	}

	private void findCharacter(L2PcInstance activeChar, String CharacterToFind)
	{
		int CharactersFound = 0;

		String name;
		Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers();
		L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);

		allPlayers = null;

		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/charfind.htm");
		TextBuilder replyMSG = new TextBuilder();

		for(L2PcInstance player : players)
		{
			name = player.getName();

			if(name.toLowerCase().contains(CharacterToFind.toLowerCase()))
			{
				CharactersFound = CharactersFound + 1;
				replyMSG.append("<tr><td width=80><a action=\"bypass -h admin_character_list " + name + "\">" + name + "</a></td><td width=110>" + player.getTemplate().className + "</td><td width=40>" + player.getLevel() + "</td></tr>");
			}

			if(CharactersFound > 20)
			{
				break;
			}
		}

		adminReply.replace("%results%", replyMSG.toString());
		replyMSG.clear();

		if(CharactersFound == 0)
		{
			replyMSG.append("s. Please try again.");
		}
		else if(CharactersFound > 20)
		{
			adminReply.replace("%number%", " more than 20");
			replyMSG.append("s.<br>Please refine your search to see all of the results.");
		}
		else if(CharactersFound == 1)
		{
			replyMSG.append(".");
		}
		else
		{
			replyMSG.append("s.");
		}

		adminReply.replace("%number%", String.valueOf(CharactersFound));
		adminReply.replace("%end%", replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private void findDualbox(L2PcInstance activeChar, int multibox) throws IllegalArgumentException
	{
		Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers();
		L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);
		Map<String, List<L2PcInstance>> ipMap = new HashMap<String, List<L2PcInstance>>();

		String ip = "0.0.0.0";

		final Map<String, Integer> dualboxIPs = new HashMap<String, Integer>();

		for(L2PcInstance player : players)
		{
			if(player.getClient() == null || player.getClient().getConnection() == null || player.getClient().getConnection().getInetAddress() == null || player.getClient().getConnection().getInetAddress().getHostAddress() == null)
			{
				continue;
			}

			ip = player.getClient().getConnection().getInetAddress().getHostAddress();

			if(ipMap.get(ip) == null)
			{
				ipMap.put(ip, new ArrayList<L2PcInstance>());
			}

			ipMap.get(ip).add(player);

			if(ipMap.get(ip).size() >= multibox)
			{
				Integer count = dualboxIPs.get(ip);
				if(count == null)
				{
					dualboxIPs.put(ip, 0);
				}
				else
				{
					dualboxIPs.put(ip, count + 1);
				}
			}
		}

		List<String> keys = new ArrayList<String>(dualboxIPs.keySet());
		Collections.sort(keys, new Comparator<String>() {
			public int compare(String left, String right)
			{
				return dualboxIPs.get(left).compareTo(dualboxIPs.get(right));
			}
		});
		Collections.reverse(keys);

		final StringBuilder results = new StringBuilder();
		for(String dualboxIP : keys)
		{
			results.append("<a action=\"bypass -h admin_find_ip " + dualboxIP + "\">" + dualboxIP + "</a><br1>");
		}

		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/game/dualbox.htm");
		adminReply.replace("%multibox%", String.valueOf(multibox));
		adminReply.replace("%results%", results.toString());
		activeChar.sendPacket(adminReply);
	}

	private void findCharactersPerIp(L2PcInstance activeChar, String IpAdress) throws IllegalArgumentException
	{
		if(!IpAdress.matches("^(?:(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2(?:[0-4][0-9]|5[0-5]))\\.){3}(?:[0-9]|[1-9][0-9]|1[0-9][0-9]|2(?:[0-4][0-9]|5[0-5]))$"))
		{
			throw new IllegalArgumentException("Malformed IPv4 number");
		}
		Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers();
		L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);
		allPlayers = null;

		int CharactersFound = 0;

		String name, ip = "0.0.0.0";
		TextBuilder replyMSG = new TextBuilder();
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile("data/html/admin/game/ipfind.htm");

		for(L2PcInstance player : players)
		{
			if(player.getClient() == null || player.getClient().getConnection() == null || player.getClient().getConnection().getInetAddress() == null || player.getClient().getConnection().getInetAddress().getHostAddress() == null)
			{
				continue;
			}

			ip = player.getClient().getConnection().getInetAddress().getHostAddress();

			if(ip.equals(IpAdress))
			{
				name = player.getName();
				CharactersFound = CharactersFound + 1;
				replyMSG.append("<tr><td width=80><a action=\"bypass -h admin_character_list " + name + "\">" + name + "</a></td><td width=110>" + player.getTemplate().className + "</td><td width=40>" + player.getLevel() + "</td></tr>");
			}

			if(CharactersFound > 20)
			{
				break;
			}
		}
		adminReply.replace("%results%", replyMSG.toString());
		replyMSG.clear();

		if(CharactersFound == 0)
		{
			replyMSG.append("s. Maybe they got d/c? :)");
		}
		else if(CharactersFound > 20)
		{
			adminReply.replace("%number%", " more than " + String.valueOf(CharactersFound));
			replyMSG.append("s.<br>In order to avoid you a client crash I won't <br1>display results beyond the 20th character.");
		}
		else if(CharactersFound == 1)
		{
			replyMSG.append(".");
		}
		else
		{
			replyMSG.append("s.");
		}

		adminReply.replace("%ip%", ip);
		adminReply.replace("%number%", String.valueOf(CharactersFound));
		adminReply.replace("%end%", replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private void findCharactersPerAccount(L2PcInstance activeChar, String characterName) throws IllegalArgumentException
	{
		if(characterName.matches(Config.CNAME_TEMPLATE))
		{
			String account = null;
			Map<Integer, String> chars;
			L2PcInstance player = L2World.getInstance().getPlayer(characterName);

			if(player == null)
			{
				throw new IllegalArgumentException("Player doesn't exist");
			}

			chars = player.getAccountChars();
			account = player.getAccountName();

			TextBuilder replyMSG = new TextBuilder();
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			adminReply.setFile("data/html/admin/game/accountinfo.htm");

			for(String charname : chars.values())
			{
				replyMSG.append(charname + "<br1>");
			}

			adminReply.replace("%characters%", replyMSG.toString());
			adminReply.replace("%account%", account);
			adminReply.replace("%player%", characterName);
			activeChar.sendPacket(adminReply);
		}
		else
		{
			throw new IllegalArgumentException("Malformed character name");
		}
	}

	private void showCharacterInfo(L2PcInstance activeChar, L2PcInstance player)
	{
		if(player == null)
		{
			L2Object target = activeChar.getTarget();

			if(target instanceof L2PcInstance)
			{
				player = (L2PcInstance) target;
			}
			else
			{
				return;
			}
		}
		else
		{
			activeChar.setTarget(player);
		}
		gatherCharacterInfo(activeChar, player, "game/charinfo.htm");
	}

	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}