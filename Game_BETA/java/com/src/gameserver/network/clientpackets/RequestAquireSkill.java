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
package com.src.gameserver.network.clientpackets;

import java.util.logging.Logger;

import com.src.Config;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.xml.SkillSpellbookTable;
import com.src.gameserver.datatables.xml.SkillTreeTable;
import com.src.gameserver.model.L2PledgeSkillLearn;
import com.src.gameserver.model.L2ShortCut;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.L2SkillLearn;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2FishermanInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2NpcInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2VillageMasterInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ExStorageMaxCount;
import com.src.gameserver.network.serverpackets.PledgeSkillList;
import com.src.gameserver.network.serverpackets.ShortCutRegister;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.util.IllegalPlayerAction;
import com.src.gameserver.util.Util;

public class RequestAquireSkill extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestAquireSkill.class.getName());

	private static final String _C__6C_REQUESTAQUIRESKILL = "[C] 6C RequestAquireSkill";

	private int _id;
	private int _level;
	private int _skillType;

	@Override
	protected void readImpl()
	{
		_id = readD();
		_level = readD();
		_skillType = readD();
	}

	@Override
	protected void runImpl()
	{

		L2PcInstance player = getClient().getActiveChar();

		if(player == null)
		{
			return;
		}

		L2NpcInstance trainer = player.getLastFolkNPC();

		if(trainer == null)
		{
			return;
		}

		int npcid = trainer.getNpcId();

		if(!player.isInsideRadius(trainer, L2Npc.INTERACTION_DISTANCE, false, false) && !player.isGM())
		{
			return;
		}

		if(!Config.ALT_GAME_SKILL_LEARN)
		{
			player.setSkillLearningClassId(player.getClassId());
		}

		if(player.getSkillLevel(_id) >= _level)
		{
			return;
		}

		L2Skill skill = SkillTable.getInstance().getInfo(_id, _level);

		int counts = 0;
		int _requiredSp = 10000000;

		if(_skillType == 0)
		{

			L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSkills(player, player.getSkillLearningClassId());

			for(L2SkillLearn s : skills)
			{
				L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
				if(sk == null || sk != skill || !sk.getCanLearn(player.getSkillLearningClassId()) || !sk.canTeachBy(npcid))
				{
					continue;
				}
				counts++;
				_requiredSp = SkillTreeTable.getInstance().getSkillCost(player, skill);
			}

			if(counts == 0 && !Config.ALT_GAME_SKILL_LEARN)
			{
				player.sendMessage("You are trying to learn skill that u can't.");
				Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to learn skill that he can't!!!", IllegalPlayerAction.PUNISH_KICK);
				return;
			}

			if(player.getSp() >= _requiredSp)
			{
				int spbId = -1;

				if(Config.DIVINE_SP_BOOK_NEEDED && skill.getId() == L2Skill.SKILL_DIVINE_INSPIRATION)
				{
					spbId = SkillSpellbookTable.getInstance().getBookForSkill(skill, _level);
				}
				else if(Config.SP_BOOK_NEEDED && skill.getLevel() == 1)
				{
					spbId = SkillSpellbookTable.getInstance().getBookForSkill(skill);
				}

				if(spbId > -1)
				{
					L2ItemInstance spb = player.getInventory().getItemByItemId(spbId);

					if(spb == null)
					{
						player.sendPacket(new SystemMessage(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL));
						return;
					}

					player.destroyItem("Consume", spb, trainer, true);
				}
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_SP_TO_LEARN_SKILL));
				return;
			}
		}
		else if(_skillType == 1)
		{
			int costid = 0;
			int costcount = 0;

			L2SkillLearn[] skillsc = SkillTreeTable.getInstance().getAvailableSkills(player);

			for(L2SkillLearn s : skillsc)
			{
				L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());

				if(sk == null || sk != skill)
				{
					continue;
				}

				counts++;
				costid = s.getIdCost();
				costcount = s.getCostCount();
				_requiredSp = s.getSpCost();
			}

			if(counts == 0)
			{
				player.sendMessage("You are trying to learn skill that u can't.");
				Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to learn skill that he can't!!!", IllegalPlayerAction.PUNISH_KICK);
				return;
			}

			if(player.getSp() >= _requiredSp)
			{
				if(!player.destroyItemByItemId("Consume", costid, costcount, trainer, false))
				{
					player.sendPacket(new SystemMessage(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL));
					return;
				}

				sendPacket(new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addNumber(costcount).addItemName(costid));
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_SP_TO_LEARN_SKILL));
				return;
			}
		}
		else if(_skillType == 2)
		{
			if(!player.isClanLeader())
			{
				player.sendMessage("This feature is available only for the clan leader.");
				return;
			}

			int itemId = 0;
			int repCost = 100000000;

			L2PledgeSkillLearn[] skills = SkillTreeTable.getInstance().getAvailablePledgeSkills(player);

			for(L2PledgeSkillLearn s : skills)
			{
				L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());

				if(sk == null || sk != skill)
				{
					continue;
				}

				counts++;
				itemId = s.getItemId();
				repCost = s.getRepCost();
			}

			if(counts == 0)
			{
				player.sendMessage("You are trying to learn skill that u can't.");
				Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " tried to learn skill that he can't!!!", IllegalPlayerAction.PUNISH_KICK);
				return;
			}

			if(player.getClan().getReputationScore() >= repCost)
			{
				if(Config.LIFE_CRYSTAL_NEEDED)
				{
					if(!player.destroyItemByItemId("Consume", itemId, 1, trainer, false))
					{
						player.sendPacket(new SystemMessage(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL));
						return;
					}

					sendPacket(new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(itemId).addNumber(1));
				}
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessageId.ACQUIRE_SKILL_FAILED_BAD_CLAN_REP_SCORE));
				return;
			}
			player.getClan().setReputationScore(player.getClan().getReputationScore() - repCost, true);
			player.getClan().addNewSkill(skill);

			player.sendPacket(new SystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP).addNumber(repCost));

			player.sendPacket(new SystemMessage(SystemMessageId.CLAN_SKILL_S1_ADDED).addSkillName(_id));

			player.getClan().broadcastToOnlineMembers(new PledgeSkillList(player.getClan()));

			for(L2PcInstance member : player.getClan().getOnlineMembers(""))
			{
				member.sendSkillList();
			}
			((L2VillageMasterInstance) trainer).showPledgeSkillList(player);

			return;
		}

		else
		{
			_log.warning("Recived Wrong Packet Data in Aquired Skill - unk1:" + _skillType);
			return;
		}

		player.addSkill(skill, true);

		player.setSp(player.getSp() - _requiredSp);

		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.SP, player.getSp());
		player.sendPacket(su);

		sendPacket(new SystemMessage(SystemMessageId.SP_DECREASED_S1).addNumber(_requiredSp));

		player.sendPacket(new SystemMessage(SystemMessageId.LEARNED_SKILL_S1).addSkillName(_id));

		if(_level > 1)
		{
			L2ShortCut[] allShortCuts = player.getAllShortCuts();

			for(L2ShortCut sc : allShortCuts)
			{
				if(sc.getId() == _id && sc.getType() == L2ShortCut.TYPE_SKILL)
				{
					L2ShortCut newsc = new L2ShortCut(sc.getSlot(), sc.getPage(), sc.getType(), sc.getId(), _level, 1);
					player.sendPacket(new ShortCutRegister(newsc));
					player.registerShortCut(newsc);
				}
			}
		}

		if(trainer instanceof L2FishermanInstance)
		{
			((L2FishermanInstance) trainer).showSkillList(player);
		}
		else
		{
			trainer.showSkillList(player, player.getSkillLearningClassId());
		}

		if(_id >= 1368 && _id <= 1372)
		{
			ExStorageMaxCount esmc = new ExStorageMaxCount(player);
			player.sendPacket(esmc);
		}
		
		player.sendSkillList();
	}

	@Override
	public String getType()
	{
		return _C__6C_REQUESTAQUIRESKILL;
	}

}