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
package com.src.gameserver.ai.special.group;

import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.quest.Quest;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.util.Util;

public class VarkaKetraAlly extends Quest
{
	public VarkaKetraAlly(int questId, String name, String descr)
	{
		super(questId, name, descr);
		int[] mobs =
		{
				21324,
				21325,
				21327,
				21328,
				21329,
				21331,
				21332,
				21334,
				21335,
				21336,
				21338,
				21339,
				21340,
				21342,
				21343,
				21344,
				21345,
				21346,
				21347,
				21348,
				21349,

				21350,
				21351,
				21353,
				21354,
				21355,
				21357,
				21358,
				21360,
				21361,
				21362,
				21364,
				21365,
				21366,
				21368,
				21369,
				21370,
				21371,
				21372,
				21373,
				21374,
				21375
		};

		for(int mob : mobs)
		{
			addEventId(mob, Quest.QuestEventType.ON_KILL);
			addEventId(mob, Quest.QuestEventType.ON_ATTACK);
		}
	}

	protected static final int[] ketraAllyMark =
	{
			7211, 7212, 7213, 7214, 7215
	};

	protected static final int[] varkaAllyMark =
	{
			7225, 7224, 7223, 7222, 7221
	};

	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
	{
		if(attacker.getAllianceWithVarkaKetra() != 0)
		{
			if(attacker.isAlliedWithKetra() && npc.getFactionId() == "ketra" || attacker.isAlliedWithVarka() && npc.getFactionId() == "varka")
			{
				L2Skill skill = SkillTable.getInstance().getInfo(4578, 1);
				if(skill != null)
				{
					npc.setTarget(attacker);
					npc.doCast(skill);
				}
			}
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		if(killer.getParty() != null)
		{
			for(L2PcInstance member : killer.getParty().getPartyMembers())
			{
				if(Util.checkIfInRange(6000, killer, member, true))
				{
					decreaseAlly(npc, member);
				}
			}
		}
		else
		{
			decreaseAlly(npc, killer);
		}

		return super.onKill(npc, killer, isPet);
	}

	private void decreaseAlly(L2Npc npc, L2PcInstance player)
	{
		if(player.getAllianceWithVarkaKetra() != 0)
		{
			L2ItemInstance mark = null;

			if(player.isAlliedWithKetra() && npc.getFactionId() == "ketra")
			{
				L2ItemInstance varkasBadgeSoldier = player.getInventory().getItemByItemId(7216);
				L2ItemInstance varkasBadgeOfficer = player.getInventory().getItemByItemId(7217);
				L2ItemInstance varkasBadgeCaptain = player.getInventory().getItemByItemId(7218);
				L2ItemInstance valorTotem = player.getInventory().getItemByItemId(7219);
				L2ItemInstance wisdomTotem = player.getInventory().getItemByItemId(7220);

				int varkasBadgeSoldierCount = varkasBadgeSoldier == null ? 0 : varkasBadgeSoldier.getCount();
				int varkasBadgeOfficerCount = varkasBadgeOfficer == null ? 0 : varkasBadgeOfficer.getCount();
				int varkasBadgeCaptainCount = varkasBadgeCaptain == null ? 0 : varkasBadgeCaptain.getCount();
				int valorTotemCount = valorTotem == null ? 0 : valorTotem.getCount();
				int wisdomTotemCount = wisdomTotem == null ? 0 : wisdomTotem.getCount();

				if(varkasBadgeSoldierCount > 0)
				{
					player.getInventory().destroyItemByItemId("ketrasBadgeSoldier", 7216, varkasBadgeSoldierCount, player, player.getTarget());
				}
				if(varkasBadgeOfficerCount > 0)
				{
					player.getInventory().destroyItemByItemId("ketrasBadgeOfficer", 7217, varkasBadgeOfficerCount, player, player.getTarget());
				}
				if(varkasBadgeCaptainCount > 0)
				{
					player.getInventory().destroyItemByItemId("ketrasBadgeCaptain", 7218, varkasBadgeCaptainCount, player, player.getTarget());
				}
				if(valorTotemCount > 0)
				{
					player.getInventory().destroyItemByItemId("valorTotem", 7219, valorTotemCount, player, player.getTarget());
				}
				if(wisdomTotemCount > 0)
				{
					player.getInventory().destroyItemByItemId("wisdomTotem", 7220, wisdomTotemCount, player, player.getTarget());
				}

				player.getInventory().destroyItemByItemId("Mark", ketraAllyMark[player.getAllianceWithVarkaKetra() - 1], 1, player, player.getTarget());
				player.setAllianceWithVarkaKetra(player.getAllianceWithVarkaKetra() - 1);
				if(player.getAllianceWithVarkaKetra() != 0)
				{
					mark = player.getInventory().addItem("Mark", ketraAllyMark[player.getAllianceWithVarkaKetra() - 1], 1, player, player);
				}
			}

			if(player.isAlliedWithVarka() && npc.getFactionId() == "varka")
			{
				L2ItemInstance ketrasBadgeSoldier = player.getInventory().getItemByItemId(7226);
				L2ItemInstance ketrasBadgeOfficer = player.getInventory().getItemByItemId(7227);
				L2ItemInstance ketrasBadgeCaptain = player.getInventory().getItemByItemId(7228);
				L2ItemInstance featherValor = player.getInventory().getItemByItemId(7229);
				L2ItemInstance featherWisdom = player.getInventory().getItemByItemId(7230);

				int ketrasBadgeSoldierCount = ketrasBadgeSoldier == null ? 0 : ketrasBadgeSoldier.getCount();
				int ketrasBadgeOfficerCount = ketrasBadgeOfficer == null ? 0 : ketrasBadgeOfficer.getCount();
				int ketrasBadgeCaptainCount = ketrasBadgeCaptain == null ? 0 : ketrasBadgeCaptain.getCount();
				int featherValorCount = featherValor == null ? 0 : featherValor.getCount();
				int featherWisdomCount = featherWisdom == null ? 0 : featherWisdom.getCount();

				if(ketrasBadgeSoldierCount > 0)
				{
					player.getInventory().destroyItemByItemId("ketrasBadgeSoldier", 7226, ketrasBadgeSoldierCount, player, player.getTarget());
				}
				if(ketrasBadgeOfficerCount > 0)
				{
					player.getInventory().destroyItemByItemId("ketrasBadgeOfficer", 7227, ketrasBadgeOfficerCount, player, player.getTarget());
				}
				if(ketrasBadgeCaptainCount > 0)
				{
					player.getInventory().destroyItemByItemId("ketrasBadgeCaptain", 7228, ketrasBadgeCaptainCount, player, player.getTarget());
				}
				if(featherValorCount > 0)
				{
					player.getInventory().destroyItemByItemId("featherValor", 7229, featherValorCount, player, player.getTarget());
				}
				if(featherWisdomCount > 0)
				{
					player.getInventory().destroyItemByItemId("featherWisdom", 7230, featherWisdomCount, player, player.getTarget());
				}

				player.getInventory().destroyItemByItemId("Mark", varkaAllyMark[player.getAllianceWithVarkaKetra() + 5], 1, player, player.getTarget());
				player.setAllianceWithVarkaKetra(player.getAllianceWithVarkaKetra() + 1);
				if(player.getAllianceWithVarkaKetra() != 0)
				{
					mark = player.getInventory().addItem("Mark", varkaAllyMark[player.getAllianceWithVarkaKetra() + 5], 1, player, player);
				}
			}

			InventoryUpdate u = new InventoryUpdate();
			u.addNewItem(mark);
			player.sendPacket(u);
		}
	}

}