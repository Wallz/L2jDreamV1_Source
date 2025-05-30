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
import com.src.gameserver.communitybbs.Manager.RegionBBSManager;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.model.L2Party;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.olympiad.Olympiad;
import com.src.gameserver.model.entity.sevensigns.SevenSignsFestival;
import com.src.gameserver.model.itemcontainer.Inventory;
import com.src.gameserver.network.L2GameClient;
import com.src.gameserver.network.L2GameClient.GameClientState;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.CharSelectInfo;
import com.src.gameserver.network.serverpackets.RestartResponse;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.taskmanager.AttackStanceTaskManager;

public final class RequestRestart extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestRestart.class.getName());

	private static final String _C__46_REQUESTRESTART = "[C] 46 RequestRestart";

	@Override
	protected void readImpl()
	{}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		
		if(player == null)
		{
			_log.warning("[RequestRestart] activeChar null!?");
			return;
		}

		if (!player.canLogout())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if(player.getOlympiadGameId() > 0 || player.isInOlympiadMode() || Olympiad.getInstance().isRegistered(player))
		{
			player.sendMessage("You can't logout while in Olympiad.");
			sendPacket(RestartResponse.valueOf(false));
			return;
		}
		
		if(player.inObserverMode())
		{
			player.sendMessage("You can't logout in Observer Olympiad mode");
			return;
		}
		
		if(player.getActiveEnchantItem() != null)
		{
			sendPacket(RestartResponse.valueOf(false));
			return;
		}

		player.getInventory().updateDatabase();

		if(player.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE)
		{
			player.sendMessage("Cannot restart while trading.");
			sendPacket(RestartResponse.valueOf(false));
			return;
		}

		if(AttackStanceTaskManager.getInstance().getAttackStanceTask(player) && !(player.isGM() && Config.GM_RESTART_FIGHTING))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANT_RESTART_WHILE_FIGHTING));
			sendPacket(RestartResponse.valueOf(false));
			return;
		}

		if(player.isFestivalParticipant())
		{
			if(SevenSignsFestival.getInstance().isFestivalInitialized())
			{
				player.sendPacket(SystemMessage.sendString("You can't logout while you are a participant in a festival."));
				player.sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(RestartResponse.valueOf(false));
				return;
			}

			L2Party playerParty = player.getParty();
			if(playerParty != null)
			{
				player.getParty().broadcastToPartyMembers(SystemMessage.sendString(player.getName() + " has been removed from the upcoming festival."));
			}
		}

		if(player.isFlying())
		{
			player.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
		}

		if(player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND) != null && player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND).isAugmented())
		{
			player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND).getAugmentation().removeBoni(player);
		}

		if(player.isInEvent())
		{
			player.sendMessage("You can't logout in event.");
			sendPacket(RestartResponse.valueOf(false));
			return;
		}

        if(player.getActiveRequester() != null)
        {
                player.getActiveRequester().onTradeCancel(player);
                player.onTradeCancel(player.getActiveRequester());
        }

		L2GameClient client = getClient();

		player.setClient(null);

		RegionBBSManager.getInstance().changeCommunityBoard();

		player.removeFromBossZone();

		player.deleteMe();
		
		client.getActiveChar().store();

		getClient().setActiveChar(null);

		client.setState(GameClientState.AUTHED);
		
		sendPacket(RestartResponse.valueOf(true));
		
		CharSelectInfo cl = new CharSelectInfo(client.getAccountName(), client.getSessionId().playOkID1);
		sendPacket(cl);
		client.setCharSelection(cl.getCharInfo());
	}

	@Override
	public String getType()
	{
		return _C__46_REQUESTRESTART;
	}

}