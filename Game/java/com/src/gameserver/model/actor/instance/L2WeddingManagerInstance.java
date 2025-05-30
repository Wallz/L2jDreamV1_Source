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

import com.src.Config;
import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.managers.CoupleManager;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.entity.Announcements;
import com.src.gameserver.model.entity.Wedding;
import com.src.gameserver.model.itemcontainer.Inventory;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.MagicSkillUser;
import com.src.gameserver.network.serverpackets.MyTargetSelected;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.ValidateLocation;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public class L2WeddingManagerInstance extends L2Npc
{
	public L2WeddingManagerInstance(int objectId, L2NpcTemplate template)
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

			player.sendPacket(new MyTargetSelected(getObjectId(), 0));

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
		String filename = "data/html/mods/weddings/wedding_start.htm";
		String replace = String.valueOf(Config.WEDDING_PRICE);

		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%replace%", replace);
		html.replace("%npcname%", getName());
		player.sendPacket(html);
		filename = null;
		replace = null;
		html = null;
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		String filename = "data/html/mods/weddings/wedding_start.htm";
		String replace = "";

		if(player.getPartnerId() == 0)
		{
			filename = "data/html/mods/weddings/wedding_nopartner.htm";
			sendHtmlMessage(player, filename, replace);
			return;
		}
		else
		{
			L2PcInstance ptarget = (L2PcInstance) L2World.getInstance().findObject(player.getPartnerId());
			if(ptarget == null || ptarget.isOnline() == 0)
			{
				filename = "data/html/mods/weddings/wedding_notfound.htm";
				sendHtmlMessage(player, filename, replace);
				return;
			}
			else
			{
				if(player.isMarried())
				{
					filename = "data/html/mods/weddings/wedding_already.htm";
					sendHtmlMessage(player, filename, replace);
					return;
				}
				else if(player.isMarryAccepted())
				{
					filename = "data/html/mods/weddings/wedding_waitforpartner.htm";
					sendHtmlMessage(player, filename, replace);
					return;
				}
				else if(command.startsWith("AcceptWedding"))
				{
					player.setMarryAccepted(true);
					Wedding wedding = CoupleManager.getInstance().getCouple(player.getCoupleId());
					wedding.marry();

					player.sendMessage("Congratulations you are married!");
					player.setMarried(true);
					player.setMaryRequest(false);
					ptarget.sendMessage("Congratulations you are married!");
					ptarget.setMarried(true);
					ptarget.setMaryRequest(false);

					if(Config.WEDDING_GIVE_CUPID_BOW)
					{
						player.addItem("Cupids Bow", 9140, 1, player, true);
						player.getInventory().updateDatabase();
						ptarget.addItem("Cupids Bow", 9140, 1, ptarget, true);
						ptarget.getInventory().updateDatabase();
						player.sendSkillList();
						ptarget.sendSkillList();
					}

					MagicSkillUser MSU = new MagicSkillUser(player, player, 2230, 1, 1, 0);
					player.broadcastPacket(MSU);
					MSU = new MagicSkillUser(ptarget, ptarget, 2230, 1, 1, 0);
					ptarget.broadcastPacket(MSU);
					MSU = null;

					L2Skill skill = SkillTable.getInstance().getInfo(2025, 1);
					if(skill != null)
					{
						MSU = new MagicSkillUser(player, player, 2025, 1, 1, 0);
						player.sendPacket(MSU);
						player.broadcastPacket(MSU);
						player.useMagic(skill, false, false);
						MSU = null;

						MSU = new MagicSkillUser(ptarget, ptarget, 2025, 1, 1, 0);
						ptarget.sendPacket(MSU);
						ptarget.broadcastPacket(MSU);
						ptarget.useMagic(skill, false, false);
						MSU = null;

						skill = null;
					}

					if(Config.WEDDING_ANNOUNCE)
					{
						Announcements.getInstance().announceToAll("Congratulations to " + player.getName() + " and " + ptarget.getName() + "! They have been married.");
					}

					MSU = null;

					filename = "data/html/mods/weddings/wedding_accepted.htm";
					replace = ptarget.getName();
					sendHtmlMessage(ptarget, filename, replace);
					return;
				}
				else if(command.startsWith("DeclineWedding"))
				{
					player.setMaryRequest(false);
					ptarget.setMaryRequest(false);
					player.setMarryAccepted(false);
					ptarget.setMarryAccepted(false);
					player.getAppearance().setNameColor(0xFFFFFF);
					ptarget.getAppearance().setNameColor(0xFFFFFF);
					player.sendMessage("You declined");
					ptarget.sendMessage("Your partner declined");
					replace = ptarget.getName();
					filename = "data/html/mods/wedding_declined.htm";
					sendHtmlMessage(ptarget, filename, replace);
					return;
				}
				else if(player.isMaryRequest())
				{
					if(Config.WEDDING_FORMALWEAR)
					{
						Inventory inv3 = player.getInventory();
						L2ItemInstance item3 = inv3.getPaperdollItem(10);
						if(item3 == null)
						{
							player.setIsWearingFormalWear(false);
						}
						else
						{
							String frmWear = Integer.toString(6408);
							String strItem = null;
							strItem = Integer.toString(item3.getItemId());

							if(null != strItem && strItem.equals(frmWear))
							{
								player.setIsWearingFormalWear(true);
							}
							else
							{
								player.setIsWearingFormalWear(false);
							}
							frmWear = null;
							strItem = null;
						}
						inv3 = null;
						item3 = null;
					}

					if(Config.WEDDING_FORMALWEAR && !player.isWearingFormalWear())
					{
						filename = "data/html/mods/weddings/wedding_noformal.htm";
						sendHtmlMessage(player, filename, replace);
						return;
					}

					filename = "data/html/mods/wedding_ask.htm";
					player.setMaryRequest(false);
					ptarget.setMaryRequest(false);
					replace = ptarget.getName();
					sendHtmlMessage(player, filename, replace);
					return;
				}
				else if(command.startsWith("AskWedding"))
				{
					if(Config.WEDDING_FORMALWEAR)
					{
						Inventory inv3 = player.getInventory();
						L2ItemInstance item3 = inv3.getPaperdollItem(10);

						if(item3 == null)
						{
							player.setIsWearingFormalWear(false);
						}
						else
						{
							String frmWear = Integer.toString(6408);
							String strItem = null;
							strItem = Integer.toString(item3.getItemId());

							if(null != strItem && strItem.equals(frmWear))
							{
								player.setIsWearingFormalWear(true);
							}
							else
							{
								player.setIsWearingFormalWear(false);
							}
							frmWear = null;
							strItem = null;
						}
						inv3 = null;
						item3 = null;
					}

					if(Config.WEDDING_FORMALWEAR && !player.isWearingFormalWear())
					{
						filename = "data/html/mods/weddings/wedding_noformal.htm";
						sendHtmlMessage(player, filename, replace);
						return;
					}
					else if(player.getAdena() < Config.WEDDING_PRICE)
					{
						filename = "data/html/mods/weddings/wedding_adena.htm";
						replace = String.valueOf(Config.WEDDING_PRICE);
						sendHtmlMessage(player, filename, replace);
						return;
					}
					else
					{
						player.setMarryAccepted(true);
						ptarget.setMaryRequest(true);
						replace = ptarget.getName();
						filename = "data/html/mods/wedding_requested.htm";
						player.getInventory().reduceAdena("Wedding", Config.WEDDING_PRICE, player, player.getLastFolkNPC());
						sendHtmlMessage(player, filename, replace);
						return;
					}
				}
			}
			ptarget = null;
		}
		sendHtmlMessage(player, filename, replace);
		filename = null;
		replace = null;
	}

	private void sendHtmlMessage(L2PcInstance player, String filename, String replace)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%replace%", replace);
		html.replace("%npcname%", getName());
		player.sendPacket(html);
		html = null;
	}

}