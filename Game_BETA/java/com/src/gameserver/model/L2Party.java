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
package com.src.gameserver.model;

import java.util.List;

import javolution.util.FastList;

import com.src.Config;
import com.src.gameserver.datatables.sql.ItemTable;
import com.src.gameserver.managers.DuelManager;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.model.actor.instance.L2SummonInstance;
import com.src.gameserver.model.entity.DimensionalRift;
import com.src.gameserver.model.entity.sevensigns.SevenSignsFestival;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.L2GameServerPacket;
import com.src.gameserver.network.serverpackets.PartyMemberPosition;
import com.src.gameserver.network.serverpackets.PartySmallWindowAdd;
import com.src.gameserver.network.serverpackets.PartySmallWindowAll;
import com.src.gameserver.network.serverpackets.PartySmallWindowDelete;
import com.src.gameserver.network.serverpackets.PartySmallWindowDeleteAll;
import com.src.gameserver.network.serverpackets.PartySmallWindowUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.Stats;
import com.src.gameserver.util.Util;
import com.src.util.random.Rnd;

public class L2Party
{
	private static final double[] BONUS_EXP_SP =
	{
		1, 1.30, 1.39, 1.50, 1.54, 1.58, 1.63, 1.67, 1.71
	};

	private List<L2PcInstance> _members = null;
	private int _pendingInvitation = 0;
	private int _partyLvl = 0;
	private int _itemDistribution = 0;
	private int _itemLastLoot = 0;
	private L2CommandChannel _commandChannel = null;

	private DimensionalRift _dr;

	public static final int ITEM_LOOTER = 0;
	public static final int ITEM_RANDOM = 1;
	public static final int ITEM_RANDOM_SPOIL = 2;
	public static final int ITEM_ORDER = 3;
	public static final int ITEM_ORDER_SPOIL = 4;

	public L2Party(L2PcInstance leader, int itemDistribution)
	{
		_itemDistribution = itemDistribution;
		getPartyMembers().add(leader);
		_partyLvl = leader.getLevel();
	}

	public int getMemberCount()
	{
		return getPartyMembers().size();
	}

	public int getPendingInvitationNumber()
	{
		return _pendingInvitation;
	}

	public void decreasePendingInvitationNumber()
	{
		_pendingInvitation--;
	}

	public void increasePendingInvitationNumber()
	{
		_pendingInvitation++;
	}

	public List<L2PcInstance> getPartyMembers()
	{
		if(_members == null)
		{
			_members = new FastList<L2PcInstance>();
		}

		return _members;
	}

	private L2PcInstance getCheckedRandomMember(int ItemId, L2Character target)
	{
		List<L2PcInstance> availableMembers = new FastList<L2PcInstance>();

		for(L2PcInstance member : getPartyMembers())
		{
			if(member.getInventory().validateCapacityByItemId(ItemId) && Util.checkIfInRange(Config.ALT_PARTY_RANGE2, target, member, true))
			{
				availableMembers.add(member);
			}
		}

		if(availableMembers.size() > 0)
		{
			return availableMembers.get(Rnd.get(availableMembers.size()));
		}
		else
		{
			return null;
		}
	}

	private L2PcInstance getCheckedNextLooter(int ItemId, L2Character target)
	{
		for(int i = 0; i < getMemberCount(); i++)
		{
			_itemLastLoot++;
			if(_itemLastLoot >= getMemberCount())
			{
				_itemLastLoot = 0;
			}

			L2PcInstance member;
			try
			{
				member = getPartyMembers().get(_itemLastLoot);
				if(member.getInventory().validateCapacityByItemId(ItemId) && Util.checkIfInRange(Config.ALT_PARTY_RANGE2, target, member, true))
				{
					return member;
				}
			}
			catch(Exception e)
			{
				
			}
		}
		return null;
	}

	private L2PcInstance getActualLooter(L2PcInstance player, int ItemId, boolean spoil, L2Character target)
	{
		L2PcInstance looter = player;

		switch(_itemDistribution)
		{
			case ITEM_RANDOM:
				if(!spoil)
				{
					looter = getCheckedRandomMember(ItemId, target);
				}
				break;
			case ITEM_RANDOM_SPOIL:
				looter = getCheckedRandomMember(ItemId, target);
				break;
			case ITEM_ORDER:
				if(!spoil)
				{
					looter = getCheckedNextLooter(ItemId, target);
				}
				break;
			case ITEM_ORDER_SPOIL:
				looter = getCheckedNextLooter(ItemId, target);
				break;
		}

		if(looter == null)
		{
			looter = player;
		}

		return looter;
	}

	/**
	 * true if player is party leader
	 * @param player
	 * @return
	 */
	public boolean isLeader(L2PcInstance player)
	{
		return getLeader().equals(player);
	}

	public int getPartyLeaderOID()
	{
		return getLeader().getObjectId();
	}

	public void broadcastToPartyMembers(L2GameServerPacket msg)
	{
		for(L2PcInstance member : getPartyMembers())
		{
			if(member != null)
			{
				member.sendPacket(msg);
			}
		}
	}

	public void broadcastToPartyMembersNewLeader()
	{
		for(L2PcInstance member : getPartyMembers())
		{
			if(member != null)
			{
				member.sendPacket(new PartySmallWindowDeleteAll());
				member.sendPacket(new PartySmallWindowAll(member, this));
				member.broadcastUserInfo();
			}
		}
	}

	public void broadcastToPartyMembers(L2PcInstance player, L2GameServerPacket msg)
	{
		for(L2PcInstance member : getPartyMembers())
		{
			if(member != null && !member.equals(player))
			{
				member.sendPacket(msg);
			}
		}
	}

	public void addPartyMember(L2PcInstance player)
	{
		//sends new member party window for all members
		player.sendPacket(new PartySmallWindowAll(player, this));

		player.sendPacket(new SystemMessage(SystemMessageId.YOU_JOINED_S1_PARTY).addString(getLeader().getName()));

		broadcastToPartyMembers(new SystemMessage(SystemMessageId.S1_JOINED_PARTY).addString(player.getName()));
		broadcastToPartyMembers(new PartySmallWindowAdd(player));

		player.sendPacket(new PartyMemberPosition(player));

		broadcastToPartyMembers(player, new PartyMemberPosition(player));

		getPartyMembers().add(player);
		if(player.getLevel() > _partyLvl)
		{
			_partyLvl = player.getLevel();
		}

		for(L2PcInstance member : getPartyMembers())
		{
			member.updateEffectIcons(true);
		}

		if(isInDimensionalRift())
		{
			_dr.partyMemberInvited();
		}
	}

	public void removePartyMember(L2PcInstance player)
	{
		if(getPartyMembers().contains(player))
		{
			boolean isLeader = isLeader(player);
			getPartyMembers().remove(player);
			recalculatePartyLevel();

			if(player.isFestivalParticipant())
			{
				SevenSignsFestival.getInstance().updateParticipants(player, this);
			}

			if(player.isInDuel())
			{
				DuelManager.getInstance().onRemoveFromParty(player);
			}

			player.sendPacket(new SystemMessage(SystemMessageId.YOU_LEFT_PARTY));
			player.sendPacket(new PartySmallWindowDeleteAll());
			player.setParty(null);

			broadcastToPartyMembers(new SystemMessage(SystemMessageId.S1_LEFT_PARTY).addString(player.getName()));
			broadcastToPartyMembers(new PartySmallWindowDelete(player));

			if(isInDimensionalRift())
			{
				_dr.partyMemberExited(player);
			}

			if(isLeader && getPartyMembers().size() > 1)
			{
				broadcastToPartyMembers(new SystemMessage(SystemMessageId.S1_HAS_BECOME_A_PARTY_LEADER).addString(getLeader().getName()));
				broadcastToPartyMembersNewLeader();
			}

			if(getPartyMembers().size() == 1)
			{
				getLeader().setParty(null);

				if(getLeader().isInDuel())
				{
					DuelManager.getInstance().onRemoveFromParty(getLeader());
				}
			}
		}
	}

	public void changePartyLeader(String name)
	{
		L2PcInstance player = getPlayerByName(name);

		if(player != null && !player.isInDuel())
		{
			if(getPartyMembers().contains(player))
			{
				if(isLeader(player))
				{
					player.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_TRANSFER_RIGHTS_TO_YOURSELF));
				}
				else
				{
					L2PcInstance temp;
					int p1 = getPartyMembers().indexOf(player);
					temp = getLeader();
					getPartyMembers().set(0, getPartyMembers().get(p1));
					getPartyMembers().set(p1, temp);

					broadcastToPartyMembers(new SystemMessage(SystemMessageId.S1_HAS_BECOME_A_PARTY_LEADER).addString(getLeader().getName()));
					broadcastToPartyMembersNewLeader();

					if(isInCommandChannel() && temp.equals(_commandChannel.getChannelLeader()))
					{
						_commandChannel.setChannelLeader(getLeader());
						_commandChannel.broadcastToChannelMembers(new SystemMessage(SystemMessageId.COMMAND_CHANNEL_LEADER_NOW_S1).addString(_commandChannel.getChannelLeader().getName()));
					}

					temp = null;

					if(player.isInPartyMatchRoom())
					{
						PartyMatchRoom room = PartyMatchRoomList.getInstance().getPlayerRoom(player);
						room.changeLeader(player);
					}
				}
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_CAN_TRANSFER_RIGHTS_ONLY_TO_ANOTHER_PARTY_MEMBER));
			}
		}
	}

	private L2PcInstance getPlayerByName(String name)
	{
		for(L2PcInstance member : getPartyMembers())
		{
			if(member.getName().equals(name))
			{
				return member;
			}
		}
		return null;
	}

	public void oustPartyMember(L2PcInstance player)
	{
		if(getPartyMembers().contains(player))
		{
			if(isLeader(player))
			{
				removePartyMember(player);

				if(getPartyMembers().size() > 1)
				{
					broadcastToPartyMembers(new SystemMessage(SystemMessageId.S1_HAS_BECOME_A_PARTY_LEADER).addString(getLeader().getName()));
					broadcastToPartyMembers(new PartySmallWindowUpdate(getLeader()));
				}
			}
			else
			{
				removePartyMember(player);
			}

			if(getPartyMembers().size() == 1)
			{
				_members = null;
			}
		}
	}

	public void oustPartyMember(String name)
	{
		L2PcInstance player = getPlayerByName(name);

		if(player != null)
		{
			if(isLeader(player))
			{
				removePartyMember(player);

				if(getPartyMembers().size() > 1)
				{
					broadcastToPartyMembers(new SystemMessage(SystemMessageId.S1_HAS_BECOME_A_PARTY_LEADER).addString(getLeader().getName()));
					broadcastToPartyMembers(new PartySmallWindowUpdate(getLeader()));
				}
			}
			else
			{
				removePartyMember(player);
			}

			if(getPartyMembers().size() == 1)
			{
				_members = null;
			}
		}
	}

	public void distributeItem(L2PcInstance player, L2ItemInstance item)
	{
		if(item.getItemId() == 57)
		{
			distributeAdena(player, item.getCount(), player);
			ItemTable.getInstance().destroyItem("Party", item, player, null);
			return;
		}

		L2PcInstance target = getActualLooter(player, item.getItemId(), false, player);
		target.addItem("Party", item, player, true);

		if(item.getCount() > 1)
		{
			broadcastToPartyMembers(target, (new SystemMessage(SystemMessageId.S1_PICKED_UP_S2_S3).addString(target.getName()).addItemName(item.getItemId()).addNumber(item.getCount())));
		}
		else
		{
			broadcastToPartyMembers(target, (new SystemMessage(SystemMessageId.S1_PICKED_UP_S2).addString(target.getName()).addItemName(item.getItemId())));
		}
	}

	public void distributeItem(L2PcInstance player, L2Attackable.RewardItem item, boolean spoil, L2Attackable target)
	{
		if(item == null)
		{
			return;
		}

		if(item.getItemId() == 57)
		{
			distributeAdena(player, item.getCount(), target);
			return;
		}

		L2PcInstance looter = getActualLooter(player, item.getItemId(), spoil, target);

		looter.addItem(spoil ? "Sweep" : "Party", item.getItemId(), item.getCount(), player, true);

		if(item.getCount() > 1)
		{
			SystemMessage msg = spoil ? new SystemMessage(SystemMessageId.S1_SWEEPED_UP_S2_S3) : new SystemMessage(SystemMessageId.S1_PICKED_UP_S2_S3);
			msg.addString(looter.getName());
			msg.addItemName(item.getItemId());
			msg.addNumber(item.getCount());
			broadcastToPartyMembers(looter, msg);
		}
		else
		{
			SystemMessage msg = spoil ? new SystemMessage(SystemMessageId.S1_SWEEPED_UP_S2) : new SystemMessage(SystemMessageId.S1_PICKED_UP_S2);
			msg.addString(looter.getName());
			msg.addItemName(item.getItemId());
			broadcastToPartyMembers(looter, msg);
		}
	}

	public void distributeAdena(L2PcInstance player, int adena, L2Character target)
	{
		List<L2PcInstance> membersList = getPartyMembers();

		List<L2PcInstance> ToReward = new FastList<L2PcInstance>();

		for(L2PcInstance member : membersList)
		{
			if(!Util.checkIfInRange(Config.ALT_PARTY_RANGE2, target, member, true))
			{
				continue;
			}
			ToReward.add(member);
		}

		if(ToReward == null || ToReward.isEmpty())
		{
			return;
		}

		int count = adena / ToReward.size();

		for(L2PcInstance member : ToReward)
		{
			member.addAdena("Party", count, player, true);
		}
	}

	public void distributeXpAndSp(long xpReward, int spReward, List<L2Playable> rewardedMembers, int topLvl)
	{
		L2SummonInstance summon = null;
		List<L2Playable> validMembers = getValidMembers(rewardedMembers, topLvl);

		float penalty;
		double sqLevel;
		double preCalculation;

		xpReward *= getExpBonus(validMembers.size());
		spReward *= getSpBonus(validMembers.size());

		double sqLevelSum = 0;

		for(L2Playable character : validMembers)
		{
			sqLevelSum += character.getLevel() * character.getLevel();
		}

		synchronized (rewardedMembers)
		{
			for(L2Character member : rewardedMembers)
			{
				if(member.isDead())
				{
					continue;
				}

				penalty = 0;

				if(member.getPet() instanceof L2SummonInstance)
				{
					summon = (L2SummonInstance) member.getPet();
					penalty = summon.getExpPenalty();
				}

				if(member instanceof L2PetInstance)
				{
					if(((L2PetInstance) member).getPetData().getOwnerExpTaken() > 0)
					{
						continue;
					}
					else
					{
						penalty = (float) 0.85;
					}
				}

				if(validMembers.contains(member))
				{
					sqLevel = member.getLevel() * member.getLevel();
					preCalculation = sqLevel / sqLevelSum * (1 - penalty);

					if(!member.isDead())
					{
						member.addExpAndSp(Math.round(member.calcStat(Stats.EXPSP_RATE, xpReward * preCalculation, null, null)), (int) member.calcStat(Stats.EXPSP_RATE, spReward * preCalculation, null, null));
					}
				}
				else
				{
					member.addExpAndSp(0, 0);
				}
			}
		}
	}

	public void recalculatePartyLevel()
	{
		int newLevel = 0;

		for(L2PcInstance member : getPartyMembers())
		{
			if(member.getLevel() > newLevel)
			{
				newLevel = member.getLevel();
			}
		}

		_partyLvl = newLevel;
	}

	private List<L2Playable> getValidMembers(List<L2Playable> members, int topLvl)
	{
		List<L2Playable> validMembers = new FastList<L2Playable>();

		if(Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("level"))
		{
			for(L2Playable member : members)
			{
				if(topLvl - member.getLevel() <= Config.PARTY_XP_CUTOFF_LEVEL)
				{
					validMembers.add(member);
				}
			}
		}
		else if(Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("percentage"))
		{
			int sqLevelSum = 0;

			for(L2Playable member : members)
			{
				sqLevelSum += member.getLevel() * member.getLevel();
			}

			for(L2Playable member : members)
			{
				int sqLevel = member.getLevel() * member.getLevel();

				if(sqLevel * 100 >= sqLevelSum * Config.PARTY_XP_CUTOFF_PERCENT)
				{
					validMembers.add(member);
				}
			}
		}
		else if(Config.PARTY_XP_CUTOFF_METHOD.equalsIgnoreCase("auto"))
		{
			int sqLevelSum = 0;

			for(L2Playable member : members)
			{
				sqLevelSum += member.getLevel() * member.getLevel();
			}

			int i = members.size() - 1;

			if(i < 1)
			{
				return members;
			}

			if(i >= BONUS_EXP_SP.length)
			{
				i = BONUS_EXP_SP.length - 1;
			}

			for(L2Playable member : members)
			{
				int sqLevel = member.getLevel() * member.getLevel();

				if(sqLevel >= sqLevelSum * (1 - 1 / (1 + BONUS_EXP_SP[i] - BONUS_EXP_SP[i - 1])))
				{
					validMembers.add(member);
				}
			}
		}
		return validMembers;
	}

	private double getBaseExpSpBonus(int membersCount)
	{
		int i = membersCount - 1;

		if(i < 1)
		{
			return 1;
		}

		if(i >= BONUS_EXP_SP.length)
		{
			i = BONUS_EXP_SP.length - 1;
		}

		return BONUS_EXP_SP[i];
	}

	private double getExpBonus(int membersCount)
	{
		if(membersCount < 2)
		{
			return getBaseExpSpBonus(membersCount);
		}
		else
		{
			return getBaseExpSpBonus(membersCount) * Config.RATE_PARTY_XP;
		}
	}

	private double getSpBonus(int membersCount)
	{
		if(membersCount < 2)
		{
			return getBaseExpSpBonus(membersCount);
		}
		else
		{
			return getBaseExpSpBonus(membersCount) * Config.RATE_PARTY_SP;
		}
	}

	public int getLevel()
	{
		return _partyLvl;
	}

	public int getLootDistribution()
	{
		return _itemDistribution;
	}

	public boolean isInCommandChannel()
	{
		return _commandChannel != null;
	}

	public L2CommandChannel getCommandChannel()
	{
		return _commandChannel;
	}

	public void setCommandChannel(L2CommandChannel channel)
	{
		_commandChannel = channel;
	}

	public boolean isInDimensionalRift()
	{
		return _dr != null;
	}

	public void setDimensionalRift(DimensionalRift dr)
	{
		_dr = dr;
	}

	public DimensionalRift getDimensionalRift()
	{
		return _dr;
	}

	public L2PcInstance getLeader()
	{
		return getPartyMembers().get(0);
	}
}