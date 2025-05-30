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
package com.src.gameserver.model.actor.instance;

import java.util.Iterator;
import java.util.Set;

import javolution.text.TextBuilder;

import com.src.Config;
import com.src.gameserver.Shutdown;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.datatables.xml.CharTemplateTable;
import com.src.gameserver.datatables.xml.SkillTreeTable;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.managers.SiegeManager;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.L2Clan.SubPledge;
import com.src.gameserver.model.L2ClanMember;
import com.src.gameserver.model.L2PledgeSkillLearn;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.base.ClassId;
import com.src.gameserver.model.base.ClassType;
import com.src.gameserver.model.base.PlayerClass;
import com.src.gameserver.model.base.PlayerRace;
import com.src.gameserver.model.base.SubClass;
import com.src.gameserver.model.entity.olympiad.Olympiad;
import com.src.gameserver.model.entity.siege.Castle;
import com.src.gameserver.model.entity.siege.Siege;
import com.src.gameserver.model.quest.QuestState;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.AquireSkillList;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.PledgeReceiveSubPledgeCreated;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.network.serverpackets.UserInfo;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.util.Util;

public final class L2VillageMasterInstance extends L2NpcInstance
{
	public L2VillageMasterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		String[] commandStr = command.split(" ");
		String actualCommand = commandStr[0];

		String cmdParams = "";
		String cmdParams2 = "";

		if(commandStr.length >= 2)
		{
			cmdParams = commandStr[1];
		}

		if(commandStr.length >= 3)
		{
			cmdParams2 = commandStr[2];
		}

		commandStr = null;
		if(player.isAio() && !Config.ALLOW_AIO_USE_CM)
    	{
    		player.sendMessage("Aio Buffers Can't Speak To Village Masters.");
    		return;
    	}
		if(actualCommand.equalsIgnoreCase("create_clan"))
		{
			if(cmdParams.equals(""))
			{
				return;
			}

			ClanTable.getInstance().createClan(player, cmdParams);
		}
		else if(actualCommand.equalsIgnoreCase("create_academy"))
		{
			if(cmdParams.equals(""))
			{
				return;
			}

			createSubPledge(player, cmdParams, null, L2Clan.SUBUNIT_ACADEMY, 5);
		}
		else if (actualCommand.equalsIgnoreCase("rename_pledge"))
		{
			if (cmdParams.isEmpty() || cmdParams2.isEmpty())
				return;
			
			renameSubPledge(player, cmdParams, actualCommand);
		}
		else if(actualCommand.equalsIgnoreCase("create_royal"))
		{
			if(cmdParams.equals(""))
			{
				return;
			}

			createSubPledge(player, cmdParams, cmdParams2, L2Clan.SUBUNIT_ROYAL1, 6);
		}
		else if (actualCommand.equalsIgnoreCase("assign_subpl_leader"))  
		{ 
			if (cmdParams.equals("")) return;  
			
			assignSubPledgeLeader(player, cmdParams, cmdParams2);  
		}  
		else if (actualCommand.equalsIgnoreCase("rename_royal1") || actualCommand.equalsIgnoreCase("rename_royal2") || actualCommand.equalsIgnoreCase("rename_knights1") || actualCommand.equalsIgnoreCase("rename_knights2") || actualCommand.equalsIgnoreCase("rename_knights3") || actualCommand.equalsIgnoreCase("rename_knights4"))
		{
			if (cmdParams.equals(""))
				return;
			
			renameSubPledge(player, cmdParams, actualCommand);
		}
		else if(actualCommand.equalsIgnoreCase("create_knight"))
		{
			if(cmdParams.equals(""))
			{
				return;
			}

			createSubPledge(player, cmdParams, cmdParams2, L2Clan.SUBUNIT_KNIGHT1, 7);
		}
		else if(actualCommand.equalsIgnoreCase("assign_subpl_leader"))
		{
			if(cmdParams.equals(""))
			{
				return;
			}

			assignSubPledgeLeader(player, cmdParams, cmdParams2);
		}
		else if(actualCommand.equalsIgnoreCase("create_ally"))
		{
			if(cmdParams.equals(""))
			{
				return;
			}
			
			if (player.getClan() == null) 
				return;
				 	
			if(!player.isClanLeader())
			{
				player.sendPacket(new SystemMessage(SystemMessageId.ONLY_CLAN_LEADER_CREATE_ALLIANCE));
				return;
			}
			player.getClan().createAlly(player, cmdParams);
		}
		else if(actualCommand.equalsIgnoreCase("dissolve_ally"))
		{
			if(!player.isClanLeader())
			{
				player.sendPacket(new SystemMessage(SystemMessageId.FEATURE_ONLY_FOR_ALLIANCE_LEADER));
				return;
			}
			player.getClan().dissolveAlly(player);
		}
		else if(actualCommand.equalsIgnoreCase("dissolve_clan"))
		{
			dissolveClan(player, player.getClanId());
		}
		else if(actualCommand.equalsIgnoreCase("change_clan_leader"))
		{
			if(cmdParams.equals(""))
			{
				return;
			}

			changeClanLeader(player, cmdParams);
		}
		else if(actualCommand.equalsIgnoreCase("recover_clan"))
		{
			recoverClan(player, player.getClanId());
		}
		else if(actualCommand.equalsIgnoreCase("increase_clan_level"))
		{
			if(!player.isClanLeader())
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
				return;
			}
			player.getClan().levelUpClan(player);
		}
		else if(actualCommand.equalsIgnoreCase("learn_clan_skills"))
		{
			showPledgeSkillList(player);
		}
		else if(command.startsWith("Subclass"))
		{
			int cmdChoice = Integer.parseInt(command.substring(9, 10).trim());

			if(Shutdown.getCounterInstance() != null)
			{
				player.sendMessage("You can`t change Subclass during server restart/shutdown!");
				return;
			}

			if(player.isCastingNow() || player.isAllSkillsDisabled())
			{
				player.sendPacket(new SystemMessage(SystemMessageId.SUBCLASS_NO_CHANGE_OR_CREATE_WHILE_SKILL_IN_USE));
				return;
			}

			if(player.isInCombat())
			{
				player.sendMessage("You can't change Subclass when you are in combact.");
				return;
			}

			if(player.isCursedWeaponEquiped())
			{
				player.sendMessage("You can`t change Subclass while Cursed weapon equiped!");
				return;
			}

			if(Olympiad.getInstance().isRegisteredInComp(player) || player.getOlympiadGameId() > 0)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_ALREADY_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_AN_EVENT));
				return;
			}

			TextBuilder content = new TextBuilder("<html><body>");
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			Set<PlayerClass> subsAvailable;

			int paramOne = 0;
			int paramTwo = 0;

			try
			{
				int endIndex = command.length();

				if(command.length() > 13)
				{
					endIndex = 13;
					paramTwo = Integer.parseInt(command.substring(13).trim());
				}

				paramOne = Integer.parseInt(command.substring(11, endIndex).trim());
			}
			catch(Exception NumberFormatException)
			{
			}

			switch(cmdChoice)
			{
				case 1:
					if (player.getPet() != null)
					{
						player.sendPacket(SystemMessageId.CANT_SUBCLASS_WITH_SUMMONED_SERVITOR);
						return;
					}
					
					if(player.getTotalSubClasses() == 3)
					{
						html.setFile("data/html/villagemaster/SubClass_Fail.htm");
						break;
					}

					subsAvailable = getAvailableSubClasses(player);

					if(subsAvailable != null && !subsAvailable.isEmpty())
					{
						player.broadcastUserInfo();
						player.abortCast();

						content.append("Add Subclass:<br>Which sub class do you wish to add?<br>");

						for(PlayerClass subClass : subsAvailable)
						{
							content.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 4 " + subClass.ordinal() + "\" msg=\"1268;" + formatClassForDisplay(subClass) + "\">" + formatClassForDisplay(subClass) + "</a><br>");
						}
					}
					else
					{
						player.sendMessage("There are no sub classes available at this time.");
						return;
					}
					break;
				case 2:
					
                    if (Olympiad.getInstance().isRegisteredInComp(player) || player.getOlympiadGameId() > 0)
                    {
                    	player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_ALREADY_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_AN_EVENT));
                    	return;
                    }
					content.append("Change Subclass:<br>");

					final int baseClassId = player.getBaseClass();

					if(player.getSubClasses().isEmpty())
					{
						content.append("You can't change sub classes when you don't have a sub class to begin with.<br>" + "<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 1\">Add subclass.</a>");
					}
					else
					{
						content.append("Which class would you like to switch to?<br>");

						if(baseClassId == player.getActiveClass())
						{
							content.append(CharTemplateTable.getClassNameById(baseClassId) + "&nbsp;<font color=\"LEVEL\">(Base Class)</font><br><br>");
						}
						else
						{
							content.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 5 0\">" + CharTemplateTable.getClassNameById(baseClassId) + "</a>&nbsp;" + "<font color=\"LEVEL\">(Base Class)</font><br><br>");
						}

						for(Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
						{
							SubClass subClass = subList.next();
							int subClassId = subClass.getClassId();

							if(subClassId == player.getActiveClass())
							{
								content.append(CharTemplateTable.getClassNameById(subClassId) + "<br>");
							}
							else
							{
								content.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 5 " + subClass.getClassIndex() + "\">" + CharTemplateTable.getClassNameById(subClassId) + "</a><br>");
							}
						}
					}
					break;
				case 3:
					content.append("Change Subclass:<br>Which of the following sub classes would you like to change?<br>");
					int classIndex = 1;

					for(Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
					{
						SubClass subClass = subList.next();

						content.append("Sub-class " + classIndex + "<br1>");
						content.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 6 " + subClass.getClassIndex() + "\">" + CharTemplateTable.getClassNameById(subClass.getClassId()) + "</a><br>");

						classIndex++;
					}

					content.append("<br>If you change a sub class, you'll start at level 40 after the 2nd class transfer.");
					break;
				case 4:
					boolean allowAddition = true;

					try
					{
						Thread.sleep(2000);
					}
					catch(InterruptedException e1)
					{
						e1.printStackTrace();
					}

					if(Config.CHECK_SKILLS_ON_ENTER && !Config.ALT_GAME_SKILL_LEARN)
					{
						player.checkAllowedSkills();
					}

					if(player.getLevel() < 75)
					{
						player.sendMessage("You may not add a new sub class before you are level 75 on your previous class.");
						allowAddition = false;
					}

					if(player.isCastingNow() || player.isAllSkillsDisabled())
					{
						player.sendPacket(new SystemMessage(SystemMessageId.SUBCLASS_NO_CHANGE_OR_CREATE_WHILE_SKILL_IN_USE));
						return;
					}

					if(Olympiad.getInstance().isRegisteredInComp(player) || player.getOlympiadGameId() > 0)
					{
						player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_ALREADY_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_AN_EVENT));
						return;
					}

					if(allowAddition)
					{
						if(!player.getSubClasses().isEmpty())
						{
							for(Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
							{
								SubClass subClass = subList.next();

								if(subClass.getLevel() < 75)
								{
									player.sendMessage("You may not add a new sub class before you are level 75 on your previous sub class.");
									allowAddition = false;
									break;
								}
							}
						}
					}

					if (allowAddition && !Config.ALT_GAME_SUBCLASS_WITHOUT_QUESTS)
						allowAddition = checkQuests(player);

					if(allowAddition)
					{
						String className = CharTemplateTable.getClassNameById(paramOne);

						if(!player.addSubClass(paramOne, player.getTotalSubClasses() + 1))
						{
							player.sendMessage("The sub class could not be added.");
							return;
						}

						player.setActiveClass(player.getTotalSubClasses());

						content.append("Add Subclass:<br>The sub class of <font color=\"LEVEL\">" + className + "</font> has been added.");
						player.sendPacket(new SystemMessage(SystemMessageId.CLASS_TRANSFER));

						className = null;
					}
					else
					{
						html.setFile("data/html/villagemaster/SubClass_Fail.htm");
					}
					break;
				case 5:
					if(Config.CHECK_SKILLS_ON_ENTER && !Config.ALT_GAME_SKILL_LEARN)
					{
						player.checkAllowedSkills();
					}

					if(Olympiad.getInstance().isRegisteredInComp(player) || player.getOlympiadGameId() > 0)
					{
						player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_ALREADY_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_AN_EVENT));
						return;
					}

					player.setActiveClass(paramOne);

					content.append("Change Subclass:<br>Your active sub class is now a <font color=\"LEVEL\">" + CharTemplateTable.getClassNameById(player.getActiveClass()) + "</font>.");

					player.sendPacket(new SystemMessage(SystemMessageId.SUBCLASS_TRANSFER_COMPLETED));

					if(Config.CHECK_SKILLS_ON_ENTER && !Config.ALT_GAME_SKILL_LEARN)
					{
						player.checkAllowedSkills();
					}
					break;
				case 6:
					content.append("Please choose a sub class to change to. If the one you are looking for is not here, " + "please seek out the appropriate master for that class.<br>" + "<font color=\"LEVEL\">Warning!</font> All classes and skills for this class will be removed.<br><br>");

					subsAvailable = getAvailableSubClasses(player);

					if(subsAvailable != null && !subsAvailable.isEmpty())
					{
						for(PlayerClass subClass : subsAvailable)
						{
							content.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 7 " + paramOne + " " + subClass.ordinal() + "\">" + formatClassForDisplay(subClass) + "</a><br>");
						}
					}
					else
					{
						player.sendMessage("There are no sub classes available at this time.");
						return;
					}
					break;
				case 7:

					try
					{
						Thread.sleep(2000);
					}
					catch(InterruptedException e1)
					{
						e1.printStackTrace();
					}

					if(Config.CHECK_SKILLS_ON_ENTER && !Config.ALT_GAME_SKILL_LEARN)
					{
						player.checkAllowedSkills();
					}

					if(Olympiad.getInstance().isRegisteredInComp(player) || player.getOlympiadGameId() > 0)
					{
						player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_ALREADY_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_AN_EVENT));
						return;
					}

					if(player.modifySubClass(paramOne, paramTwo))
					{
						stopAllEffects();
						player.setActiveClass(paramOne);

						content.append("Change Subclass:<br>Your sub class has been changed to <font color=\"LEVEL\">" + CharTemplateTable.getClassNameById(paramTwo) + "</font>.");

						player.sendPacket(new SystemMessage(SystemMessageId.ADD_NEW_SUBCLASS));

						if(Config.CHECK_SKILLS_ON_ENTER && !Config.ALT_GAME_SKILL_LEARN)
						{
							player.checkAllowedSkills();
						}

					}
					else
					{
						player.setActiveClass(0);

						player.sendMessage("The sub class could not be added, you have been reverted to your base class.");
						return;
					}
					break;
			}

			content.append("</body></html>");

			if(content.length() > 26)
			{
				html.setHtml(content.toString());
			}

			player.sendPacket(html);

			content = null;
			html = null;
			subsAvailable = null;
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
		actualCommand = null;
		cmdParams = null;
		cmdParams2 = null;
	}

	protected boolean checkQuests(L2PcInstance player)
	{
		// Noble players can add subbclasses without quests
		if (player.isNoble())
			return true;
		
		QuestState qs = player.getQuestState("q234_FatesWhisper");
		if (qs == null || !qs.isCompleted())
			return false;
		
		qs = player.getQuestState("q235_MimirsElixir");
		if (qs == null || !qs.isCompleted())
			return false;
		
		return true;
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";

		if(val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}

		return "data/html/villagemaster/" + pom + ".htm";
	}

	public void dissolveClan(L2PcInstance player, int clanId)
	{
		if (player.isFlying())  
		{  
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return;  
		}
	 	
		if(!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return;
		}

		L2Clan clan = player.getClan();
		if(clan.getAllyId() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISPERSE_THE_CLANS_IN_ALLY));
			return;
		}

		if(clan.isAtWar() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_WAR));
			return;
		}

		if(clan.getHasCastle() != 0 || clan.getHasHideout() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISSOLVE_WHILE_OWNING_CLAN_HALL_OR_CASTLE));
			return;
		}

		for(Castle castle : CastleManager.getInstance().getCastles())
		{
			if(SiegeManager.getInstance().checkIsRegistered(clan, castle.getCastleId()))
			{
				player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISSOLVE_CAUSE_CLAN_WILL_PARTICIPATE_IN_CASTLE_SIEGE));
				return;
			}
		}

		if(player.isInsideZone(L2Character.ZONE_SIEGE))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_DISSOLVE_WHILE_IN_SIEGE));
			return;
		}

		if(clan.getDissolvingExpiryTime() > System.currentTimeMillis())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.DISSOLUTION_IN_PROGRESS));
			return;
		}

		if (Config.ALT_CLAN_DISSOLVE_DAYS > 0)
		{
		clan.setDissolvingExpiryTime(System.currentTimeMillis() + Config.ALT_CLAN_DISSOLVE_DAYS * 86400000L);
		clan.updateClanInDB();

		ClanTable.getInstance().scheduleRemoveClan(clan.getClanId());
		}
		else
			ClanTable.getInstance().destroyClan(player.getClanId(), null);
		
		player.deathPenalty(false);

		clan = null;
	}

	public void recoverClan(L2PcInstance player, int clanId)
	{
		if(!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return;
		}
		L2Clan clan = player.getClan();

		clan.setDissolvingExpiryTime(0);
		clan.updateClanInDB();

		clan = null;
	}

	public void changeClanLeader(L2PcInstance player, String target)
	{
		if(!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return;
		}
		
		if(player.isFlying())
		{
			player.sendMessage("You must dismount the wyvern to change the clan leader.");
			return;
		}

		if(player.isMounted())
		{
			player.sendMessage("You must dismount the pet to change the clan leader.");
			return;
		}

		if(player.getName().equalsIgnoreCase(target))
		{
			return;
		}

		L2Clan clan = player.getClan();
		L2ClanMember member = clan.getClanMember(target);
		if(member == null)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_DOES_NOT_EXIST);
			sm.addString(target);
			player.sendPacket(sm);
			sm = null;
			return;
		}

		if(!member.isOnline())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.INVITED_USER_NOT_ONLINE));
			return;
		}

		if(SiegeManager.getInstance().checkIsRegisteredInSiege(clan))
		{
			player.sendMessage("Cannot change clan leader while registered in siege.");
			return;
		}

		clan.setNewLeader(member,player);

		clan = null;
		member = null;
	}

	public void createSubPledge(L2PcInstance player, String clanName, String leaderName, int pledgeType, int minClanLvl)
	{
		if(!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return;
		}

		L2Clan clan = player.getClan();
		if(clan.getLevel() < minClanLvl)
		{
			if(pledgeType == L2Clan.SUBUNIT_ACADEMY)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_CLAN_ACADEMY));
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_MEET_CRITERIA_IN_ORDER_TO_CREATE_A_MILITARY_UNIT));
			}
			return;
		}

		if (!Util.isAlphaNumeric(clanName))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_NAME_INCORRECT));
			return;
		}
		
		if(!Util.isAlphaNumeric(clanName) || 2 > clanName.length())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_NAME_INCORRECT));
			return;
		}

		if(clanName.length() > 16)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_NAME_TOO_LONG));
			return;
		}
		
		if (pledgeType != L2Clan.SUBUNIT_ACADEMY && clan.getClanMember(leaderName) == null) 
			return; 
		
		if(clan.getLeaderSubPledge(leaderName)!=0)  
		{  
			player.sendMessage(leaderName+" is already a sub unit leader.");  
			return;  
		} 
		for(L2Clan tempClan : ClanTable.getInstance().getClans())
		{
			if(tempClan.getSubPledge(clanName) != null)
			{
				if(pledgeType == L2Clan.SUBUNIT_ACADEMY)
				{
					player.sendPacket(new SystemMessage(SystemMessageId.S1_ALREADY_EXISTS).addString(clanName));
				}
				else
				{
					player.sendPacket(new SystemMessage(SystemMessageId.ANOTHER_MILITARY_UNIT_IS_ALREADY_USING_THAT_NAME));
				}
				return;
			}
		}

		if(pledgeType != L2Clan.SUBUNIT_ACADEMY)
		{
			if(clan.getClanMember(leaderName) == null || clan.getClanMember(leaderName).getPledgeType() != 0)
			{
				if(pledgeType >= L2Clan.SUBUNIT_KNIGHT1)
				{
					player.sendPacket(new SystemMessage(SystemMessageId.CAPTAIN_OF_ORDER_OF_KNIGHTS_CANNOT_BE_APPOINTED));
				}
				else if(pledgeType >= L2Clan.SUBUNIT_ROYAL1)
				{
					player.sendPacket(new SystemMessage(SystemMessageId.CAPTAIN_OF_ROYAL_GUARD_CANNOT_BE_APPOINTED));
				}
				return;
			}
		}

		if(clan.createSubPledge(player, pledgeType, leaderName, clanName) == null)
		{
			return;
		}

		SystemMessage sm;
		if(pledgeType == L2Clan.SUBUNIT_ACADEMY)
		{
			sm = (new SystemMessage(SystemMessageId.THE_S1S_CLAN_ACADEMY_HAS_BEEN_CREATED).addString(player.getClan().getName()));
		}
		else if(pledgeType >= L2Clan.SUBUNIT_KNIGHT1)
		{
			sm = (new SystemMessage(SystemMessageId.THE_KNIGHTS_OF_S1_HAVE_BEEN_CREATED).addString(player.getClan().getName()));
		}
		else if(pledgeType >= L2Clan.SUBUNIT_ROYAL1)
		{
			sm = (new SystemMessage(SystemMessageId.THE_ROYAL_GUARD_OF_S1_HAVE_BEEN_CREATED).addString(player.getClan().getName()));
		}
		else
		{
			sm = new SystemMessage(SystemMessageId.CLAN_CREATED);
		}

		player.sendPacket(sm);
		if(pledgeType != L2Clan.SUBUNIT_ACADEMY)
		{
			L2ClanMember leaderSubPledge = clan.getClanMember(leaderName);
			if(leaderSubPledge.getPlayerInstance() == null)
			{
				return;
			}

			leaderSubPledge.getPlayerInstance().setPledgeClass(L2ClanMember.calculatePledgeClass(leaderSubPledge.getPlayerInstance()));
			leaderSubPledge.getPlayerInstance().sendPacket(new UserInfo(leaderSubPledge.getPlayerInstance()));
			try  
			{  
				clan.getClanMember(leaderName).updatePledgeType();  
				for (L2Skill skill : leaderSubPledge.getPlayerInstance().getAllSkills())  
					leaderSubPledge.getPlayerInstance().removeSkill(skill,false);  
				clan.getClanMember(leaderName).getPlayerInstance().setActiveClass(0);  
			}  
			catch(Throwable t){}  
			for (L2ClanMember member : clan.getMembers())  
			{  
				if (member == null || member.getPlayerInstance()==null || member.getPlayerInstance().isOnline()==0)  
					continue;  
				SubPledge[] subPledge = clan.getAllSubPledges();  
				for (SubPledge element : subPledge)   
				{  
					member.getPlayerInstance().sendPacket(new PledgeReceiveSubPledgeCreated(element));  
				}
			}
 
       }
		clan = null;
		sm = null;
	}
	
	public void renameSubPledge(L2PcInstance player, String newName, String command)
	{  
		if (player == null || player.getClan() == null || !player.isClanLeader())  
		{  
			if (player!=null)   
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));  
			return;  
		}  
		L2Clan clan = player.getClan();  
		SubPledge[] subPledge = clan.getAllSubPledges();  
		
		if (subPledge == null)
		{
			player.sendMessage("Pledge doesn't exist.");
			return;
		}
		
		if (!Util.isAlphaNumeric(newName))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_NAME_INCORRECT));
			return;
		}
		
		if (newName.length() < 2 || newName.length() > 16)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_NAME_TOO_LONG));
			return;
		}
		
		for (SubPledge element : subPledge)   
		{  
			switch(element.getId())
			{
			case -1:
				if (command.equalsIgnoreCase("rename_plege"))
				{
					changeSubPledge(clan, element, newName);  
					return;
				}
			case 100: // 1st Royal Guard  
				if (command.equalsIgnoreCase("rename_royal1"))
				{  
					changeSubPledge(clan, element, newName);  
					return;  
				}  
				break;  
			case 200: // 2nd Royal Guard  
				if (command.equalsIgnoreCase("rename_royal2"))
				{  
					changeSubPledge(clan, element, newName);  
					return;  
				}  
				break;                
			case 1001: // 1st Order of Knights  
				if (command.equalsIgnoreCase("rename_knights1"))
				{  
					changeSubPledge(clan, element, newName);  
					return;  
				}  
				break;                
			case 1002: // 2nd Order of Knights  
				if (command.equalsIgnoreCase("rename_knights2"))
				{  
					changeSubPledge(clan, element, newName);  
					return;  
				}  
				break;                
			case 2001: // 3rd Order of Knights  
				if (command.equalsIgnoreCase("rename_knights3"))
				{  
					changeSubPledge(clan, element, newName);  
					return;  
				}  
				break;                
			case 2002: // 4th Order of Knights  
				if (command.equalsIgnoreCase("rename_knights4"))
				{  
					changeSubPledge(clan, element, newName);  
					return;  
				}  
				break;  
			}  
		}  
		player.sendMessage("Sub unit not found.");  
	}  
	
	public void changeSubPledge(L2Clan clan, SubPledge element, String newName)
	{  
		if (newName.length() > 16 || newName.length() < 3)  
		{  
			clan.getLeader().getPlayerInstance().sendPacket(new SystemMessage(SystemMessageId.CLAN_NAME_TOO_LONG));  
			return;  
		}  
		String oldName = element.getName();   
		element.setName(newName);  
		clan.updateSubPledgeInDB(element.getId());  
		for (L2ClanMember member : clan.getMembers())  
		{  
			if (member == null || member.getPlayerInstance()==null || member.getPlayerInstance().isOnline()==0)  
				continue;  
			SubPledge[] subPledge = clan.getAllSubPledges();  
			for (SubPledge sp : subPledge)   
			{  
				member.getPlayerInstance().sendPacket(new PledgeReceiveSubPledgeCreated(sp));  
			}  
			if (member.getPlayerInstance()!= null)  
				member.getPlayerInstance().sendMessage("Clan sub unit " + oldName + "'s name has been changed into " + newName + ".");  
		}  
	}  
      
   public void assignSubPledgeLeader(L2PcInstance player, String subUnitName, String newLeaderName)  
   {  
       if (player.getClan() == null)  
       {  
           player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));  
           return;           
       }  
       String leaderName = "";  
       try  
       {  
           leaderName = player.getClan().getSubPledge(subUnitName).getLeaderName();  
       }  
       catch(Throwable t)  
       {  
    	   _log.warning("could not find sub unit leader name for sub unit: "+subUnitName+" in clan "+player.getClan());  
    	   return;  
       } 
       
		if(!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return;
		}

		if (leaderName.length() > 16 || newLeaderName.length() > 16)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NAMING_CHARNAME_UP_TO_16CHARS));
			return;
		}

		if(player.getName().equals(newLeaderName))  
		{  
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));

		L2Clan clan = player.getClan();
        if(leaderName.equals(newLeaderName) || clan.getLeaderSubPledge(newLeaderName)!=0)  
        {  
            player.sendMessage(newLeaderName+" is already a sub unit leader.");  
            return;  
        }  
        L2PcInstance newLeader = L2World.getInstance().getPlayer(newLeaderName);  
        if (newLeader!=null)  
        {  
            if (newLeader.getClan() == null || newLeader.getClan()!=clan)
            {  
                player.sendMessage(newLeaderName+" is not in your clan!");  
                return;  
            }  
        }  
        int leadssubpledge = clan.getLeaderSubPledge(leaderName);  
  
        if (leadssubpledge == 0)   
        {  
            player.sendMessage(leaderName + " is not a sub unit leader.");  
            return;  
	        }  
        try  
        {  
            clan.getSubPledge(leadssubpledge).setLeaderName(newLeaderName);  
            clan.getClanMember(newLeaderName).updatePledgeType();  
            clan.getClanMember(leaderName).setPledgeType(0);  
            clan.getClanMember(leaderName).updatePledgeType();  
        }  
        catch(Throwable t){}  
        try  
        {  
            clan.getClanMember(leaderName);
			clan.getClanMember(leaderName).getPlayerInstance().setPledgeClass(L2ClanMember.calculatePledgeClass(clan.getClanMember(leaderName).getPlayerInstance()));  
            clan.getClanMember(leaderName).getPlayerInstance().setActiveClass(0);  
        }  
        catch(Throwable t)
        {
        	
        }  
          
        clan.updateSubPledgeInDB(leadssubpledge); 
        for (L2PcInstance member : clan.getOnlineMembers(""))
        {  
            member.sendMessage(newLeaderName+" has been appointed as sub unit leader instead of "+leaderName+".");  
            member.broadcastUserInfo();  
        }  
        for (L2ClanMember member : clan.getMembers())  
        {  
            if (member == null || member.getPlayerInstance()==null || member.getPlayerInstance().isOnline()==0)  
                continue; 
            SubPledge[] subPledge = clan.getAllSubPledges();  
            for (SubPledge element : subPledge)  
            {  
                member.getPlayerInstance().sendPacket(new PledgeReceiveSubPledgeCreated(element));  
            }  
        }  
        L2ClanMember leaderSubPledge = clan.getClanMember(newLeaderName);  
        if (leaderSubPledge.getPlayerInstance() == null) return;  
        leaderSubPledge.getPlayerInstance().setPledgeClass(L2ClanMember.calculatePledgeClass(leaderSubPledge.getPlayerInstance()));  
        leaderSubPledge.getPlayerInstance().sendPacket(new UserInfo(leaderSubPledge.getPlayerInstance()));  
        for (L2Skill skill : leaderSubPledge.getPlayerInstance().getAllSkills())  
            leaderSubPledge.getPlayerInstance().removeSkill(skill,false);  
        leaderSubPledge.getPlayerInstance().setActiveClass(0);
		}
	}

	private final Set<PlayerClass> getAvailableSubClasses(L2PcInstance player)
	{
		int charClassId = player.getBaseClass();

		if(charClassId >= 88)
		{
			charClassId = player.getClassId().getParent().ordinal();
		}

		final PlayerRace npcRace = getVillageMasterRace();
		final ClassType npcTeachType = getVillageMasterTeachType();

		PlayerClass currClass = PlayerClass.values()[charClassId];

		Set<PlayerClass> availSubs = currClass.getAvailableSubclasses(player);

		if(availSubs != null)
		{
			for(PlayerClass availSub : availSubs)
			{
				for(SubClass subClass : player.getSubClasses().values())
				{
					if(subClass.getClassId() == availSub.ordinal())
					{
						availSubs.remove(availSub);
					}
				}
				for(Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext();)
				{
					SubClass prevSubClass = subList.next();
					int subClassId = prevSubClass.getClassId();
					if(subClassId >= 88)
					{
						subClassId = ClassId.values()[subClassId].getParent().getId();
					}

					if(availSub.ordinal() == subClassId || availSub.ordinal() == player.getBaseClass())
					{
						availSubs.remove(PlayerClass.values()[availSub.ordinal()]);
					}
				}

				if(npcRace == PlayerRace.Human || npcRace == PlayerRace.LightElf)
				{
					if(!availSub.isOfType(npcTeachType))
					{
						availSubs.remove(availSub);
					}
					else if(!availSub.isOfRace(PlayerRace.Human) && !availSub.isOfRace(PlayerRace.LightElf))
					{
						availSubs.remove(availSub);
					}
				}
				else
				{
					if(npcRace != PlayerRace.Human && npcRace != PlayerRace.LightElf && !availSub.isOfRace(npcRace))
					{
						availSubs.remove(availSub);
					}
				}
			}
		}

		currClass = null;
		return availSubs;
	}

	public void showPledgeSkillList(L2PcInstance player)
	{
		if(player.getClan() == null || !player.isClanLeader())
		{
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setFile("data/html/villagemaster/noleader.htm");
			player.sendPacket(html);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		AquireSkillList asl = new AquireSkillList(AquireSkillList.skillType.Clan);
		boolean empty = true;

		for (L2PledgeSkillLearn psl : SkillTreeTable.getInstance().getAvailablePledgeSkills(player))
		{
			L2Skill sk = SkillTable.getInstance().getInfo(psl.getId(), psl.getLevel());
			if (sk == null)
				continue;
			
			asl.addSkill(psl.getId(), psl.getLevel(), psl.getLevel(), psl.getRepCost(), 0);
			empty = false;
		}

		if(empty)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(1);

			if(player.getClan().getLevel() < 8)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN).addNumber(player.getClan().getLevel() + 1));
			}
			else
			{
				TextBuilder sb = new TextBuilder();
				sb.append("<html><body>");
				sb.append("You've learned all skills available for your Clan.<br>");
				sb.append("</body></html>");
				html.setHtml(sb.toString());
				player.sendPacket(html);
				html = null;
				sb = null;
			}
		}
		else
		{
			player.sendPacket(asl);
		}

		asl = null;
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private final String formatClassForDisplay(PlayerClass className)
	{
		String classNameStr = className.toString();
		char[] charArray = classNameStr.toCharArray();

		for(int i = 1; i < charArray.length; i++)
		{
			if(Character.isUpperCase(charArray[i]))
			{
				classNameStr = classNameStr.substring(0, i) + " " + classNameStr.substring(i);
			}
		}

		return classNameStr;
	}

	private final PlayerRace getVillageMasterRace()
	{
		String npcClass = getTemplate().getStatsSet().getString("jClass").toLowerCase();

		if(npcClass.indexOf("human") > -1)
		{
			return PlayerRace.Human;
		}

		if(npcClass.indexOf("darkelf") > -1)
		{
			return PlayerRace.DarkElf;
		}

		if(npcClass.indexOf("elf") > -1)
		{
			return PlayerRace.LightElf;
		}

		if(npcClass.indexOf("orc") > -1)
		{
			return PlayerRace.Orc;
		}

		return PlayerRace.Dwarf;
	}

	private final ClassType getVillageMasterTeachType()
	{
		String npcClass = getTemplate().getStatsSet().getString("jClass");

		if(npcClass.indexOf("sanctuary") > -1 || npcClass.indexOf("clergyman") > -1)
		{
			return ClassType.Priest;
		}

		if(npcClass.indexOf("mageguild") > -1 || npcClass.indexOf("patriarch") > -1)
		{
			return ClassType.Mystic;
		}

		return ClassType.Fighter;
	}

	private Iterator<SubClass> iterSubClasses(L2PcInstance player)
	{
		return player.getSubClasses().values().iterator();
	}

}