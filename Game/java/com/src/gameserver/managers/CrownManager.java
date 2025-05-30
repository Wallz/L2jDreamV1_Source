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
package com.src.gameserver.managers;

import com.src.gameserver.datatables.CrownTable;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.L2ClanMember;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.siege.Castle;

public class CrownManager
{
	private static CrownManager _instance;

	public static final CrownManager getInstance()
	{
		if(_instance == null)
		{
			_instance = new CrownManager();
		}
		return _instance;
	}

	public CrownManager()
	{}

	public void checkCrowns(L2Clan clan)
	{
		if(clan == null)
		{
			return;
		}

		for(L2ClanMember member : clan.getMembers())
		{
			if(member != null && member.isOnline() && member.getPlayerInstance() != null)
			{
				checkCrowns(member.getPlayerInstance());
			}
		}
	}

	public void checkCrowns(L2PcInstance activeChar)
	{
		if(activeChar == null)
		{
			return;
		}

		boolean isLeader = false;
		int crownId = -1;

		L2Clan activeCharClan = activeChar.getClan();
		L2ClanMember activeCharClanLeader;

		if(activeCharClan != null)
		{
			activeCharClanLeader = activeChar.getClan().getLeader();
		}
		else
		{
			activeCharClanLeader = null;
		}

		if(activeCharClan != null)
		{
			Castle activeCharCastle = CastleManager.getInstance().getCastleByOwner(activeCharClan);

			if(activeCharCastle != null)
			{
				crownId = CrownTable.getCrownId(activeCharCastle.getCastleId());
			}

			activeCharCastle = null;

			if(activeCharClanLeader != null && activeCharClanLeader.getObjectId() == activeChar.getObjectId())
			{
				isLeader = true;
			}
		}

		activeCharClan = null;
		activeCharClanLeader = null;

		if(crownId > 0)
		{
			if(isLeader && activeChar.getInventory().getItemByItemId(6841) == null)
			{
				activeChar.addItem("Crown", 6841, 1, activeChar, true);
				activeChar.getInventory().updateDatabase();
			}

			if(activeChar.getInventory().getItemByItemId(crownId) == null)
			{
				activeChar.addItem("Crown", crownId, 1, activeChar, true);
				activeChar.getInventory().updateDatabase();
			}
		}

		boolean alreadyFoundCirclet = false;
		boolean alreadyFoundCrown = false;

		for(L2ItemInstance item : activeChar.getInventory().getItems())
		{
			if(CrownTable.getCrownList().contains(item.getItemId()))
			{
				if(crownId > 0)
				{
					if(item.getItemId() == crownId)
					{
						if(!alreadyFoundCirclet)
						{
							alreadyFoundCirclet = true;
							continue;
						}
					}
					else if(item.getItemId() == 6841 && isLeader)
					{
						if(!alreadyFoundCrown)
						{
							alreadyFoundCrown = true;
							continue;
						}
					}
				}

				activeChar.destroyItem("Removing Crown", item, activeChar, true);
				activeChar.getInventory().updateDatabase();
			}
		}
	}

}