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

import com.src.gameserver.model.BlockList;
import com.src.gameserver.model.L2Party;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.AskJoinParty;
import com.src.gameserver.network.serverpackets.SystemMessage;

public final class RequestJoinParty extends L2GameClientPacket
{
	private static final String _C__29_REQUESTJOINPARTY = "[C] 29 RequestJoinParty";

	private String _name;
	private int _itemDistribution;

	@Override
	protected void readImpl()
	{
		_name = readS();
		_itemDistribution = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance requestor = getClient().getActiveChar();
		L2PcInstance target = L2World.getInstance().getPlayer(_name);

		if(requestor == null)
		{
			return;
		}

		if(target == null)
		{
			requestor.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
			return;
		}

		if(requestor.isInCombat() || target.isInCombat() )
        {
            requestor.sendMessage("Sorry, you are in combat now.");
            return;
        }
		
		if(target.isInParty())
		{
			requestor.sendPacket(new SystemMessage(SystemMessageId.S1_IS_ALREADY_IN_PARTY).addString(target.getName()));
			return;
		}
		
		if(target == requestor)
		{
			requestor.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			return;
		}

		if(target.isCursedWeaponEquiped() || requestor.isCursedWeaponEquiped())
		{
			requestor.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			return;
		}

		if(target.getAppearance().getInvisible())
		{
			requestor.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
			return;
		}

		if(target.isInJail() || requestor.isInJail())
		{
			requestor.sendMessage("Player is in Jail.");
			return;
		}

		if(BlockList.isBlocked(target, requestor))
		{
			requestor.sendPacket(new SystemMessage(SystemMessageId.S1_HAS_ADDED_YOU_TO_IGNORE_LIST).addString(target.getName()));
			return;
		}

		if(target.isOffline())
		{
			requestor.sendMessage("Player is in offline mode.");
			return;
		}

		if(target.isInOlympiadMode() || requestor.isInOlympiadMode())
		{
			return;
		}

		if(target.isInDuel() || requestor.isInDuel())
		{
			return;
		}

		if(!requestor.isInParty())
		{
			createNewParty(target, requestor);
		}
		else
		{
			if(requestor.getParty().isInDimensionalRift())
			{
				requestor.sendMessage("You can't invite a player when in Dimensional Rift.");
			}
			else
			{
				addTargetToParty(target, requestor);
			}
		}
	}

	private void addTargetToParty(L2PcInstance target, L2PcInstance requestor)
	{
		if(requestor.getParty().getMemberCount() + requestor.getParty().getPendingInvitationNumber() >= 9)
		{
			requestor.sendPacket(new SystemMessage(SystemMessageId.PARTY_FULL));
			return;
		}

		if(!requestor.getParty().isLeader(requestor))
		{
			requestor.sendPacket(new SystemMessage(SystemMessageId.ONLY_LEADER_CAN_INVITE));
			return;
		}

		if(!target.isProcessingRequest())
		{
			requestor.onTransactionRequest(target);
			target.sendPacket(new AskJoinParty(requestor.getName(), _itemDistribution));
			requestor.getParty().increasePendingInvitationNumber();

			requestor.sendPacket(new SystemMessage(SystemMessageId.YOU_INVITED_S1_TO_PARTY).addString(target.getName()));
		}
		else
		{
			requestor.sendPacket(new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER));
		}

	}

	private void createNewParty(L2PcInstance target, L2PcInstance requestor)
	{
		if(!target.isProcessingRequest())
		{
			requestor.setParty(new L2Party(requestor, _itemDistribution));

			requestor.onTransactionRequest(target);
			target.sendPacket(new AskJoinParty(requestor.getName(), _itemDistribution));
			requestor.getParty().increasePendingInvitationNumber();

			requestor.sendPacket(new SystemMessage(SystemMessageId.YOU_INVITED_S1_TO_PARTY).addString(target.getName()));
		}
		else
		{
			requestor.sendPacket(new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER).addString(target.getName()));
		}
	}

	@Override
	public String getType()
	{
		return _C__29_REQUESTJOINPARTY;
	}

}