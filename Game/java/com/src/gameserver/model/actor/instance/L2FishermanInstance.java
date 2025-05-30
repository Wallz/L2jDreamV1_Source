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

import java.util.StringTokenizer;

import javolution.text.TextBuilder;

import com.src.gameserver.TradeController;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.xml.SkillTreeTable;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.L2SkillLearn;
import com.src.gameserver.model.L2TradeList;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.AquireSkillList;
import com.src.gameserver.network.serverpackets.BuyList;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.SellList;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public class L2FishermanInstance extends L2NpcInstance
{
	public L2FishermanInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
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

		return "data/html/fisherman/" + pom + ".htm";
	}

	private void showBuyWindow(L2PcInstance player, int val)
	{
		double taxRate = 0;
		if(getIsInTown())
		{
			taxRate = getCastle().getTaxRate();
		}
		player.tempInventoryDisable();
		L2TradeList list = TradeController.getInstance().getBuyList(val);

		if(list != null && list.getNpcId().equals(String.valueOf(getNpcId())))
		{
			BuyList bl = new BuyList(list, player.getAdena(), taxRate);
			player.sendPacket(bl);
			list = null;
			bl = null;
		}
		else
		{
			_log.warning("possible client hacker: " + player.getName() + " attempting to buy from GM shop! (L2FishermanInstance)");
			_log.warning("buylist id:" + val);
		}

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private void showSellWindow(L2PcInstance player)
	{
		player.sendPacket(new SellList(player));
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if(command.startsWith("FishSkillList"))
		{
			player.setSkillLearningClassId(player.getClassId());
			showSkillList(player);
		}

		StringTokenizer st = new StringTokenizer(command, " ");
		String command2 = st.nextToken();

		if(command2.equalsIgnoreCase("Buy"))
		{
			if(st.countTokens() < 1)
			{
				return;
			}

			int val = Integer.parseInt(st.nextToken());
			showBuyWindow(player, val);
		}
		else if(command2.equalsIgnoreCase("Sell"))
		{
			showSellWindow(player);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
		st = null;
		command2 = null;
	}

	public void showSkillList(L2PcInstance player)
	{
		L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSkills(player);
		AquireSkillList asl = new AquireSkillList(AquireSkillList.skillType.Fishing);

		int counts = 0;

		for(L2SkillLearn s : skills)
		{
			L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());

			if(sk == null)
			{
				continue;
			}

			counts++;
			asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), s.getSpCost(), 1);
		}

		if(counts == 0)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			int minlevel = SkillTreeTable.getInstance().getMinLevelForNewSkill(player);

			if(minlevel > 0)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN).addNumber(minlevel));
			}
			else
			{
				TextBuilder sb = new TextBuilder();
				sb.append("<html><head><body>");
				sb.append("You've learned all skills.<br>");
				sb.append("</body></html>");
				html.setHtml(sb.toString());
				player.sendPacket(html);
				sb = null;
				html = null;
			}
		}
		else
		{
			player.sendPacket(asl);
		}

		skills = null;
		asl = null;

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

}