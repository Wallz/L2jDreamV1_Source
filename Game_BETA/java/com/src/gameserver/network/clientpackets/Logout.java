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

import com.src.Config;
import com.src.gameserver.communitybbs.Manager.RegionBBSManager;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.model.L2Party;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.olympiad.Olympiad;
import com.src.gameserver.model.entity.sevensigns.SevenSignsFestival;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.taskmanager.AttackStanceTaskManager;

public final class Logout extends L2GameClientPacket
{
	private static final String _C__09_LOGOUT = "[C] 09 Logout";

	@Override
	protected void readImpl()
	{
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();

		if(player == null)
		{
			return;
		}

		player.getInventory().updateDatabase();

		if(AttackStanceTaskManager.getInstance().getAttackStanceTask(player) && !(player.isGM() && Config.GM_RESTART_FIGHTING))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANT_LOGOUT_WHILE_FIGHTING));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(player.atEvent)
		{
			player.sendPacket(SystemMessage.sendString("A superior power doesn't allow you to leave the event"));
			return;
		}

		if(player.isInOlympiadMode() || Olympiad.getInstance().isRegistered(player))
		{
			player.sendMessage("You can't logout in olympiad mode");
			return;
		}

		if(player.isCastingNow())
		{
			player.abortCast();
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}

		if(player.isFestivalParticipant())
		{
			if(SevenSignsFestival.getInstance().isFestivalInitialized())
			{
				player.sendMessage("You can't log out while you are a participant in a festival.");
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

		if(player.isInStoreMode() && Config.OFFLINE_TRADE_ENABLE || player.isInCraftMode() && Config.OFFLINE_CRAFT_ENABLE)
		{
			player.closeNetConnection();
			if(player.getOfflineStartTime() == 0)
			{
				player.setOfflineStartTime(System.currentTimeMillis());
			}

			return;
		}

		RegionBBSManager.getInstance().changeCommunityBoard();

		player.removeFromBossZone();
		player.deleteMe();
	}

	@Override
	public String getType()
	{
		return _C__09_LOGOUT;
	}

}