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

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.managers.ClanHallManager;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.entity.ClanHall;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.MyTargetSelected;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.Ride;
import com.src.gameserver.network.serverpackets.ValidateLocation;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public class L2WyvernManagerInstance extends L2CastleChamberlainInstance
{
	protected static final int COND_CLAN_OWNER = 3;
	private int _clanHallId = -1;

	public L2WyvernManagerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if(command.startsWith("RideWyvern"))
		{
			if(!player.isClanLeader())
			{
				player.sendMessage("Only clan leaders are allowed.");
				return;
			}

			if(player.getPet() == null)
			{
				if(player.isMounted())
				{
					player.sendMessage("You Already Have a Pet or Are Mounted.");
					return;
				}
				else
				{
					player.sendMessage("Summon your Strider first.");
					return;
				}
			}
			else if(player.getPet().getNpcId() == 12526 || player.getPet().getNpcId() == 12527 || player.getPet().getNpcId() == 12528)
			{
				if(player.getInventory().getItemByItemId(1460) != null && player.getInventory().getItemByItemId(1460).getCount() >= 10)
				{
					if(player.getPet().getLevel() < 55)
					{
						player.sendMessage("Your Strider Has not reached the required level.");
						return;
					}
					else
					{
						if(!player.disarmWeapons())
						{
							return;
						}
						player.getPet().unSummon(player);
						player.getInventory().destroyItemByItemId("Wyvern", 1460, 10, player, player.getTarget());
						Ride mount = new Ride(player.getObjectId(), Ride.ACTION_MOUNT, 12621);
						player.sendPacket(mount);
						player.broadcastPacket(mount);
						player.setMountType(mount.getMountType());
						player.addSkill(SkillTable.getInstance().getInfo(4289, 1));
						player.sendMessage("The Wyvern has been summoned successfully!");
						mount = null;
						return;
					}
				}
				else
				{
					player.sendMessage("You need 10 Crystals: B Grade.");
					return;
				}
			}
			else
			{
				player.sendMessage("Unsummon your pet.");
				return;
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if(!canTarget(player))
		{
			return;
		}

		if(this != player.getTarget())
		{
			player.setTarget(this);

			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
			my = null;

			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			if(!canInteract(player))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				showMessageWindow(player);
			}
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private void showMessageWindow(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		String filename = "data/html/wyvernmanager/wyvernmanager-no.htm";
		if(getClanHall() != null)
		{
			filename = "data/html/wyvernmanager/wyvernmanager-clan-no.htm";
		}

		int condition = validateCondition(player);
		if(condition > COND_ALL_FALSE)
		{
			if(condition == COND_OWNER)
			{
				filename = "data/html/wyvernmanager/wyvernmanager.htm";
			}
			else if(condition == COND_CLAN_OWNER)
			{
				filename = "data/html/wyvernmanager/wyvernmanager-clan.htm";
			}
		}
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
		html = null;
		filename = null;
	}

	public final ClanHall getClanHall()
	{
		if(_clanHallId < 0)
		{
			ClanHall temp = ClanHallManager.getInstance().getNearbyClanHall(getX(), getY(), 500);

			if(temp != null)
			{
				_clanHallId = temp.getId();
				temp = null;
			}

			if(_clanHallId < 0)
			{
				return null;
			}
		}
		return ClanHallManager.getInstance().getClanHallById(_clanHallId);
	}

	@Override
	protected int validateCondition(L2PcInstance player)
	{
		if(getClanHall() != null && player.getClan() != null)
		{
			if(getClanHall().getOwnerId() == player.getClanId() && player.isClanLeader())
			{
				return COND_CLAN_OWNER;
			}
		}
		else if(super.getCastle() != null && super.getCastle().getCastleId() > 0)
		{
			if(player.getClan() != null)
			{
				if(super.isInsideZone(L2Character.ZONE_SIEGE) || super.getCastle().getSiege().getIsInProgress())
				{
					return COND_BUSY_BECAUSE_OF_SIEGE;
				}
				else if(super.getCastle().getOwnerId() == player.getClanId() && player.isClanLeader())
				{
					return COND_OWNER;
				}
			}
		}

		return COND_ALL_FALSE;
	}

}