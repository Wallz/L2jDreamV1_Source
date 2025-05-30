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

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.managers.ClanHallManager;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.entity.ClanHall;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.MyTargetSelected;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.Ride;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.network.serverpackets.ValidateLocation;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public class L2DoormenInstance extends L2NpcInstance
{
	private ClanHall _clanHall;
	private static int COND_ALL_FALSE = 0;
	private static int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	private static int COND_CASTLE_OWNER = 2;
	private static int COND_HALL_OWNER = 3;

	public L2DoormenInstance(int objectID, L2NpcTemplate template)
	{
		super(objectID, template);
	}

	public final ClanHall getClanHall()
	{
		if(_clanHall == null)
		{
			_clanHall = ClanHallManager.getInstance().getNearbyClanHall(getX(), getY(), 500);
		}

		return _clanHall;
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		int condition = validateCondition(player);
		if(condition <= COND_ALL_FALSE)
		{
			return;
		}

		if(condition == COND_BUSY_BECAUSE_OF_SIEGE)
		{
			return;
		}
		else if(condition == COND_CASTLE_OWNER || condition == COND_HALL_OWNER)
		{
			if(command.startsWith("Chat"))
			{
				showMessageWindow(player);
				return;
			}
			else if(command.startsWith("open_doors"))
			{
				if(condition == COND_HALL_OWNER)
				{
					getClanHall().openCloseDoors(true);
					player.sendPacket(new NpcHtmlMessage(getObjectId(), "<html><body>You have <font color=\"LEVEL\">opened</font> the clan hall door.<br>Outsiders may enter the clan hall while the door is open. Please close it when you've finished your business.<br><center><button value=\"Close\" action=\"bypass -h npc_" + getObjectId() + "_close_doors\" width=70 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>"));
				}
				else if(condition == COND_CASTLE_OWNER)
				{
					StringTokenizer st = new StringTokenizer(command.substring(10), ", ");
					st.nextToken();

					while(st.hasMoreTokens())
					{
						getCastle().openDoor(player, Integer.parseInt(st.nextToken()));
					}

					st = null;
					return;
				}

			}

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
						player.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("You Already Have a Pet or Are Mounted."));
						return;
					}
					else
					{
						player.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("Summon your Strider first."));
						return;
					}
				}
				else if(player.getPet().getNpcId() == 12526 || player.getPet().getNpcId() == 12527 || player.getPet().getNpcId() == 12528)
				{
					if(player.getInventory().getItemByItemId(1460) != null && player.getInventory().getItemByItemId(1460).getCount() >= 10)
					{
						if(player.getPet().getLevel() < 55)
						{
							player.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("Your Strider Has not reached the required level."));
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
							player.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("The Wyvern has been summoned successfully!"));
							mount = null;
							return;
						}
					}
					else
					{
						player.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("You need 10 Crystals: B Grade."));
						return;
					}
				}
				else
				{
					player.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("Unsummon your pet."));
					return;
				}
			}
			else if(command.startsWith("close_doors"))
			{
				if(condition == COND_HALL_OWNER)
				{
					getClanHall().openCloseDoors(false);
					player.sendPacket(new NpcHtmlMessage(getObjectId(), "<html><body>You have <font color=\"LEVEL\">closed</font> the clan hall door.<br>Good day!<br><center><button value=\"To Begining\" action=\"bypass -h npc_" + getObjectId() + "_Chat\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>"));
				}
				else if(condition == COND_CASTLE_OWNER)
				{
					StringTokenizer st = new StringTokenizer(command.substring(11), ", ");
					st.nextToken();

					while(st.hasMoreTokens())
					{
						getCastle().closeDoor(player, Integer.parseInt(st.nextToken()));
					}

					st = null;
					return;
				}
			}
		}

		super.onBypassFeedback(player, command);
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

	public void showMessageWindow(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		String filename = "data/html/doormen/" + getTemplate().npcId + "-no.htm";

		int condition = validateCondition(player);
		if(condition == COND_BUSY_BECAUSE_OF_SIEGE)
		{
			filename = "data/html/doormen/" + getTemplate().npcId + "-busy.htm";
		}
		else if(condition == COND_CASTLE_OWNER)
		{
			filename = "data/html/doormen/" + getTemplate().npcId + ".htm";
		}

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		String str;
		if(getClanHall() != null)
		{
			if(condition == COND_HALL_OWNER)
			{
				str = "<html><body>Hello!<br><font color=\"55FFFF\">" + getName() + "</font>, I am honored to serve your clan.<br>How may i assist you?<br>";
				str += "<center><br><button value=\"Open Door\" action=\"bypass -h npc_%objectId%_open_doors\" width=70 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>";
				str += "<button value=\"Close Door\" action=\"bypass -h npc_%objectId%_close_doors\" width=70 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"><br>";
				if(getClanHall().getId() >= 36 && getClanHall().getId() <= 41)
				{
					str += "<button value=\"Wyvern Exchange\" action=\"bypass -h npc_%objectId%_RideWyvern\" width=85 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center></body></html>";
				}
				else
				{
					str += "</center></body></html>";
				}
			}
			else
			{
				L2Clan owner = ClanTable.getInstance().getClan(getClanHall().getOwnerId());
				if(owner != null && owner.getLeader() != null)
				{
					str = "<html><body>Hello there!<br>This clan hall is owned by <font color=\"55FFFF\">" + owner.getLeader().getName() + " who is the Lord of the ";
					str += owner.getName() + "</font> clan.<br>";
					str += "I am sorry, but only the clan members who belong to the <font color=\"55FFFF\">" + owner.getName() + "</font> clan can enter the clan hall.</body></html>";
				}
				else
				{
					str = "<html><body>" + getName() + ":<br1>Clan hall <font color=\"LEVEL\">" + getClanHall().getName() + "</font> have no owner clan.<br>You can rent it at auctioneers..</body></html>";
				}
			}
			html.setHtml(str);
		}
		else
		{
			html.setFile(filename);
		}

		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);

		filename = null;
		html = null;
		str = null;
	}

	private int validateCondition(L2PcInstance player)
	{
		if(player.getClan() != null)
		{
			if(getClanHall() != null)
			{
				if(player.getClanId() == getClanHall().getOwnerId())
				{
					return COND_HALL_OWNER;
				}
				else
				{
					return COND_ALL_FALSE;
				}
			}

			if(getCastle() != null && getCastle().getCastleId() > 0)
			{
				if(getCastle().getOwnerId() == player.getClanId())
				{
					return COND_CASTLE_OWNER;
				}
			}
		}

		return COND_ALL_FALSE;
	}

}