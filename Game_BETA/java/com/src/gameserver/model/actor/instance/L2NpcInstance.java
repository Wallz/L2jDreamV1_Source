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

import javolution.text.TextBuilder;

import com.src.Config;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.xml.SkillTreeTable;
import com.src.gameserver.model.L2EnchantSkillLearn;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.L2SkillLearn;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.base.ClassId;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.AquireSkillList;
import com.src.gameserver.network.serverpackets.ExEnchantSkillList;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public class L2NpcInstance extends L2Npc
{
	private final ClassId[] _classesToTeach;

	public L2NpcInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		_classesToTeach = template.getTeachInfo();
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		player.setLastFolkNPC(this);
		super.onAction(player);
	}

	public void showSkillList(L2PcInstance player, ClassId classId)
	{
		int npcId = getTemplate().npcId;

		if(_classesToTeach == null)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			TextBuilder sb = new TextBuilder();
			sb.append("<html><body>");
			sb.append("I cannot teach you. My class list is empty.<br> Ask admin to fix it. Need add my npcid and classes to skill_learn.sql.<br>NpcId:" + npcId + ", Your classId:" + player.getClassId().getId() + "<br>");
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);
			return;
		}

		if(!getTemplate().canTeach(classId))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			TextBuilder sb = new TextBuilder();
			sb.append("<html><body>");
			sb.append("I cannot teach you any skills.<br> You must find your current class teachers.");
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);
			return;
		}

		L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSkills(player, classId);
		AquireSkillList asl = new AquireSkillList(AquireSkillList.skillType.Usual);
		int counts = 0;

		for(L2SkillLearn s : skills)
		{
			L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());

			if(sk == null || !sk.getCanLearn(player.getClassId()) || !sk.canTeachBy(npcId))
			{
				continue;
			}

			int cost = SkillTreeTable.getInstance().getSkillCost(player, sk);
			counts++;

			asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), cost, 0);
		}

		if(counts == 0)
		{
			int minlevel = SkillTreeTable.getInstance().getMinLevelForNewSkill(player, classId);

			if(minlevel > 0)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN).addNumber(minlevel));
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessageId.NO_MORE_SKILLS_TO_LEARN));
			}
		}
		else
		{
			player.sendPacket(asl);
		}

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	public void showEnchantSkillList(L2PcInstance player, ClassId classId)
	{
		int npcId = getTemplate().npcId;

		if(_classesToTeach == null)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			TextBuilder sb = new TextBuilder();
			sb.append("<html><body>");
			sb.append("I cannot teach you. My class list is empty.<br> Ask admin to fix it. Need add my npcid and classes to skill_learn.sql.<br>NpcId:" + npcId + ", Your classId:" + player.getClassId().getId() + "<br>");
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);
			html = null;
			sb = null;
			return;
		}

		if(!getTemplate().canTeach(classId))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			TextBuilder sb = new TextBuilder();
			sb.append("<html><body>");
			sb.append("I cannot teach you any skills.<br> You must find your current class teachers.");
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);
			html = null;
			sb = null;
			return;
		}

		if(player.getClassId().getId() < 88)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			TextBuilder sb = new TextBuilder();
			sb.append("<html><body>");
			sb.append("You must have 3rd class change quest completed.");
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);
			html = null;
			sb = null;
			return;
		}

		L2EnchantSkillLearn[] skills = SkillTreeTable.getInstance().getAvailableEnchantSkills(player);
		ExEnchantSkillList esl = new ExEnchantSkillList();
		int counts = 0;

		for(L2EnchantSkillLearn s : skills)
		{
			L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
			if(sk == null)
			{
				continue;
			}
			counts++;
			esl.addSkill(s.getId(), s.getLevel(), s.getSpCost(), s.getExp());
		}
		if(counts == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.THERE_IS_NO_SKILL_THAT_ENABLES_ENCHANT));
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			int level = player.getLevel();

			if(level < 74)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN).addNumber(level));
			}
			else
			{
				TextBuilder sb = new TextBuilder();
				sb.append("<html><body>");
				sb.append("You've learned all skills for your class.<br>");
				sb.append("</body></html>");
				html.setHtml(sb.toString());
				player.sendPacket(html);
				html = null;
				sb = null;
			}
		}
		else
		{
			player.sendPacket(esl);
		}
		skills = null;
		esl = null;

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if(command.startsWith("SkillList"))
		{
			if(Config.ALT_GAME_SKILL_LEARN)
			{
				String id = command.substring(9).trim();

				if(id.length() != 0)
				{
					player.setSkillLearningClassId(ClassId.values()[Integer.parseInt(id)]);
					showSkillList(player, ClassId.values()[Integer.parseInt(id)]);
				}
				else
				{
					boolean own_class = false;

					if(_classesToTeach != null)
					{
						for(ClassId cid : _classesToTeach)
						{
							if(cid.equalsOrChildOf(player.getClassId()))
							{
								own_class = true;
								break;
							}
						}
					}

					String text = "<html><body><center>Skill learning:</center><br>";

					if(!own_class)
					{
						String mages = player.getClassId().isMage() ? "fighters" : "mages";
						text += "Skills of your class are the easiest to learn.<br>" + "Skills of another class are harder.<br>" + "Skills for another race are even more hard to learn.<br>" + "You can also learn skills of " + mages + ", and they are" + " the hardest to learn!<br>" + "<br>";
					}

					if(_classesToTeach != null)
					{
						int count = 0;
						ClassId classCheck = player.getClassId();

						while(count == 0 && classCheck != null)
						{
							for(ClassId cid : _classesToTeach)
							{
								if(cid.level() != classCheck.level())
								{
									continue;
								}

								if(SkillTreeTable.getInstance().getAvailableSkills(player, cid).length == 0)
								{
									continue;
								}

								text += "<a action=\"bypass -h npc_%objectId%_SkillList " + cid.getId() + "\">Learn " + cid + "'s class Skills</a><br>\n";
								count++;
							}
							classCheck = classCheck.getParent();
						}
						classCheck = null;
					}
					else
					{
						text += "No Skills.<br>";
					}

					text += "</body></html>";

					insertObjectIdAndShowChatWindow(player, text);
					player.sendPacket(ActionFailed.STATIC_PACKET);
					text = null;
				}
			}
			else
			{
				player.setSkillLearningClassId(player.getClassId());
				showSkillList(player, player.getClassId());
			}
		}
		else if(command.startsWith("EnchantSkillList"))
		{
			showEnchantSkillList(player, player.getClassId());
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
}