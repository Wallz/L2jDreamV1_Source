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

import java.text.SimpleDateFormat;

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.managers.ClanHallManager;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.L2ClanMember;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.entity.ClanHall;
import com.src.gameserver.model.entity.clanhallsiege.BanditStrongholdSiege;
import com.src.gameserver.model.entity.clanhallsiege.WildBeastFarmSiege;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.MyTargetSelected;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.ValidateLocation;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public class L2ClanHallSiegeInInstance extends L2Npc
{
	public L2ClanHallSiegeInInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
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
				showMessageWindow(player, 0);
			}
		}

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if(command.startsWith("Chat"))
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch(IndexOutOfBoundsException ioobe)
			{
			}
			catch(NumberFormatException nfe)
			{
			}
			showMessageWindow(player, val);
		}
		else if(command.startsWith("Quest"))
		{
			String quest = "";
			try
			{
				quest = command.substring(5).trim();
			}
			catch(IndexOutOfBoundsException ioobe)
			{
			}

			if(quest.length() == 0)
			{
				showQuestWindow(player, this);
			}
			else
			{
				showQuestWindow(player, this, quest);
			}
		}
		else if(command.startsWith("Registration"))
		{
			L2Clan playerClan = player.getClan();
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			String str;
			str = "<html><body>Newspaper!<br>";

			switch(getTemplate().getNpcId())
			{
				case 35437:
					if(!BanditStrongholdSiege.getInstance().isRegistrationPeriod())
					{
						showMessageWindow(player, 3);
						return;
					}
					if(playerClan == null || playerClan.getLeaderName() != player.getName() || playerClan.getLevel() < 4)
					{
						showMessageWindow(player, 1);
						return;
					}
					if(BanditStrongholdSiege.getInstance().clanhall.getOwnerClan() == playerClan)
					{
						str += "Your clan is already registered for the siege, what more do you want from me?<br>";
						str += "<a action=\"bypass -h npc_%objectId%_PlayerList\">Add / remove a member of the siege</a><br>";
					}
					else
					{
						if(BanditStrongholdSiege.getInstance().isClanOnSiege(playerClan))
						{
							str += "Your clan is already registered for the siege, what more do you want from me?<br>";
							str += "<a action=\"bypass -h npc_%objectId%_UnRegister\">Unsubscribe</a><br>";
							str += "<a action=\"bypass -h npc_%objectId%_PlayerList\">Add / remove a member of the siege</a><br>";
						}
						else
						{
							int res = BanditStrongholdSiege.getInstance().registerClanOnSiege(player, playerClan);
							if(res == 0)
							{
								str += "Your clan : <font color=\"LEVEL\">" + player.getClan().getName() + "</font>, successfully registered for the siege clan hall.<br>";
								str += "Now you need to select no more than 18 igokov who will take part in the siege, a member of your clan.<br>";
								str += "<a action=\"bypass -h npc_%objectId%_PlayerList\">Select members of the siege</a><br>";
							}
							else if(res == 1)
							{
								str += "You have not passed the test and did not qualify for participation in the siege of Robbers<br>";
								str += "Come back when you're done.";
							}
							else if(res == 2)
							{
								str += "Unfortunately, you are late. Five tribal leaders have already filed an application for registration.<br>";
								str += "Next time be more powerful";
							}
						}
					}
					break;
				case 35627:
					if(!WildBeastFarmSiege.getInstance().isRegistrationPeriod())
					{
						showMessageWindow(player, 3);
						return;
					}
					if(playerClan == null || playerClan.getLeaderName() != player.getName() || playerClan.getLevel() < 4)
					{
						showMessageWindow(player, 1);
						return;
					}
					if(WildBeastFarmSiege.getInstance().clanhall.getOwnerClan() == playerClan)
					{
						str += "Your clan is already registered for the siege, what more do you want from me?<br>";
						str += "<a action=\"bypass -h npc_%objectId%_PlayerList\">Add / remove a member of the siege</a><br>";
					}
					else
					{
						if(WildBeastFarmSiege.getInstance().isClanOnSiege(playerClan))
						{
							str += "Your clan is already registered for the siege, what more do you want from me?<br>";
							str += "<a action=\"bypass -h npc_%objectId%_UnRegister\">Unsubscribe</a><br>";
							str += "<a action=\"bypass -h npc_%objectId%_PlayerList\">Add / remove a member of the siege</a><br>";
						}
						else
						{
							int res = WildBeastFarmSiege.getInstance().registerClanOnSiege(player, playerClan);
							if(res == 0)
							{
								str += "Your clan : <font color=\"LEVEL\">" + player.getClan().getName() + "</font>, successfully registered for the siege clan hall.<br>";
								str += "Now you need to select no more than 18 igokov who will take part in the siege, a member of your clan.<br>";
								str += "<a action=\"bypass -h npc_%objectId%_PlayerList\">Select members of the siege</a><br>";
							}
							else if(res == 1)
							{
								str += "You have not passed the test and did not qualify for participation in the siege of Robbers<br>";
								str += "Come back when you're done.";
							}
							else if(res == 2)
							{
								str += "Unfortunately, you are late. Five tribal leaders have already filed an application for registration.<br>";
								str += "Next time be more raztoropny.";
							}
						}
					}
					break;
			}

			str += "</body></html>";
			html.setHtml(str);
			html.replace("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(html);
		}
		else if(command.startsWith("UnRegister"))
		{
			L2Clan playerClan = player.getClan();
			if(playerClan == null || playerClan.getLeaderName() != player.getName() || playerClan.getLevel() < 4)
			{
				_log.warning("Attention!!! player " + player.getName() + " use packet hack, try unregister clan.");
				return;
			}
			if(!BanditStrongholdSiege.getInstance().isRegistrationPeriod())
			{
				showMessageWindow(player, 3);
				return;
			}
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			String str;
			if(BanditStrongholdSiege.getInstance().isClanOnSiege(playerClan))
			{
				if(BanditStrongholdSiege.getInstance().unRegisterClan(playerClan))
				{
					str = "<html><body>Newspaper!<br>";
					str += "Your clan : <font color=\"LEVEL\">" + player.getClan().getName() + "</font>, successfully removed from the register at the siege clan hall.<br>";
					str += "</body></html>";
					html.setHtml(str);
					html.replace("%objectId%", String.valueOf(getObjectId()));
					player.sendPacket(html);
				}
			}
			else
			{
				_log.warning("Attention!!! player " + player.getName() + " use packet hack, try unregister clan.");
			}
		}
		else if(command.startsWith("PlayerList"))
		{
			L2Clan playerClan = player.getClan();
			if(playerClan == null || playerClan.getLeaderName() != player.getName() || playerClan.getLevel() < 4)
			{
				return;
			}

			if(!BanditStrongholdSiege.getInstance().isRegistrationPeriod())
			{
				showMessageWindow(player, 3);
				return;
			}

			if(BanditStrongholdSiege.getInstance().isClanOnSiege(playerClan))
			{
				showPlayersList(playerClan, player);
			}
		}
		else if(command.startsWith("addPlayer"))
		{
			L2Clan playerClan = player.getClan();
			if(playerClan == null || playerClan.getLeaderName() != player.getName() || playerClan.getLevel() < 4)
			{
				return;
			}

			if(!BanditStrongholdSiege.getInstance().isRegistrationPeriod())
			{
				showMessageWindow(player, 3);
				return;
			}

			String val = command.substring(10);
			if(playerClan.getClanMember(val) == null)
			{
				return;
			}

			BanditStrongholdSiege.getInstance().addPlayer(playerClan, val);
			if(BanditStrongholdSiege.getInstance().isClanOnSiege(playerClan))
			{
				showPlayersList(playerClan, player);
			}
		}
		else if(command.startsWith("removePlayer"))
		{
			L2Clan playerClan = player.getClan();
			if(playerClan == null || playerClan.getLeaderName() != player.getName() || playerClan.getLevel() < 4)
			{
				return;
			}

			if(!BanditStrongholdSiege.getInstance().isRegistrationPeriod())
			{
				showMessageWindow(player, 3);
				return;
			}

			String val = command.substring(13);
			if(playerClan.getClanMember(val) != null)
			{
				BanditStrongholdSiege.getInstance().removePlayer(playerClan, val);
			}

			if(BanditStrongholdSiege.getInstance().isClanOnSiege(playerClan))
			{
				showPlayersList(playerClan, player);
			}
		}
	}

	public void showPlayersList(L2Clan playerClan, L2PcInstance player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		String str;
		str = "<html><body>Newspaper!<br>";
		str += "Your clan : <font color=\"LEVEL\">" + player.getClan().getName() + "</font>. select participants for the siege.<br><br>";
		str += "<img src=\"L2UI.SquareWhite\" width=280 height=1>";
		str += "<table width=280 border=0 bgcolor=\"000000\"><tr><td width=170 align=center>Register bathrooms</td><td width=110 align=center>Action</td></tr></table>";
		str += "<img src=\"L2UI.SquareWhite\" width=280 height=1>";
		str += "<table width=280 border=0>";
		for(String temp : BanditStrongholdSiege.getInstance().getRegisteredPlayers(playerClan))
		{
			str += "<tr><td width=170>" + temp + "</td><td width=110 align=center><a action=\"bypass -h npc_%objectId%_removePlayer " + temp + "\"> Remove</a></td></tr>";
		}
		str += "</table>";
		str += "<img src=\"L2UI.SquareWhite\" width=280 height=1>";
		str += "<table width=280 border=0 bgcolor=\"000000\"><tr><td width=170 align=center>Clan Members</td><td width=110 align=center>Action</td></tr></table>";
		str += "<img src=\"L2UI.SquareWhite\" width=280 height=1>";
		str += "<table width=280 border=0>";
		for(L2ClanMember temp : playerClan.getMembers())
		{
			if(!BanditStrongholdSiege.getInstance().getRegisteredPlayers(playerClan).contains(temp.getName()))
			{
				str += "<tr><td width=170>" + temp.getName() + "</td><td width=110 align=center><a action=\"bypass -h npc_%objectId%_addPlayer " + temp.getName() + "\"> Add</a></td></tr>";
			}
		}
		str += "</table>";
		str += "</body></html>";
		html.setHtml(str);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}

	public void showMessageWindow(L2PcInstance player, int val)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		long startSiege = 0;
		int npcId = getTemplate().getNpcId();
		String filename;
		if(val == 0)
		{
			filename = "data/html/default/" + npcId + ".htm";
		}
		else
		{
			filename = "data/html/default/" + npcId + "-" + val + ".htm";
		}
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		if(npcId == 35382)
		{
		}
		else if(npcId == 35437 || npcId == 35627)
		{
			ClanHall clanhall = null;
			String clans = "";
			clans += "<table width=280 border=0>";
			int clanCount = 0;

			switch(npcId)
			{
				case 35437:
					clanhall = ClanHallManager.getInstance().getClanHallById(35);
					startSiege = BanditStrongholdSiege.getInstance().getSiegeDate().getTimeInMillis();
					for(String a : BanditStrongholdSiege.getInstance().getRegisteredClans())
					{
						clanCount++;
						clans += "<tr><td><font color=\"LEVEL\">" + a + "</font>  (Number :" + BanditStrongholdSiege.getInstance().getPlayersCount(a) + "people.)</td></tr>";
					}
					break;
			}
			while(clanCount < 5)
			{
				clans += "<tr><td><font color=\"LEVEL\">**Not logged**</font>  (Quantity : people.)</td></tr>";
				clanCount++;
			}
			clans += "</table>";
			html.replace("%clan%", String.valueOf(clans));
			L2Clan clan = clanhall.getOwnerClan();
			String clanName;
			if(clan == null)
			{
				clanName = "NPC";
			}
			else
			{
				clanName = clan.getName();
			}
			html.replace("%clanname%", String.valueOf(clanName));
		}

		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		html.replace("%SiegeDate%", String.valueOf(format.format(startSiege)));
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}

}