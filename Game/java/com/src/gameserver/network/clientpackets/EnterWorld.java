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

import java.io.File;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.GameTimeController;
import com.src.gameserver.communitybbs.Manager.RegionBBSManager;
import com.src.gameserver.datatables.GMSkillTable;
import com.src.gameserver.datatables.GmListTable;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.datatables.xml.MapRegionTable;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.managers.ClanHallManager;
import com.src.gameserver.managers.CoupleManager;
import com.src.gameserver.managers.CursedWeaponsManager;
import com.src.gameserver.managers.DimensionalRiftManager;
import com.src.gameserver.managers.FunEventsManager;
import com.src.gameserver.managers.PetitionManager;
import com.src.gameserver.managers.SiegeManager;
import com.src.gameserver.model.CursedWeapon;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.L2ClanMember;
import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2ClassMasterInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.base.ClassLevel;
import com.src.gameserver.model.base.PlayerClass;
import com.src.gameserver.model.entity.Announcements;
import com.src.gameserver.model.entity.ClanHall;
import com.src.gameserver.model.entity.Hero;
import com.src.gameserver.model.entity.Wedding;
import com.src.gameserver.model.entity.olympiad.Olympiad;
import com.src.gameserver.model.entity.sevensigns.SevenSigns;
import com.src.gameserver.model.entity.siege.Castle;
import com.src.gameserver.model.entity.siege.Siege;
import com.src.gameserver.model.quest.Quest;
import com.src.gameserver.model.quest.QuestState;
import com.src.gameserver.network.Disconnection;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ClientSetTime;
import com.src.gameserver.network.serverpackets.Die;
import com.src.gameserver.network.serverpackets.Earthquake;
import com.src.gameserver.network.serverpackets.EtcStatusUpdate;
import com.src.gameserver.network.serverpackets.ExAutoSoulShot;
import com.src.gameserver.network.serverpackets.ExShowScreenMessage;
import com.src.gameserver.network.serverpackets.ExStorageMaxCount;
import com.src.gameserver.network.serverpackets.FriendList;
import com.src.gameserver.network.serverpackets.HennaInfo;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.network.serverpackets.MagicSkillUser;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.PledgeShowMemberListAll;
import com.src.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import com.src.gameserver.network.serverpackets.PledgeSkillList;
import com.src.gameserver.network.serverpackets.PledgeStatusChanged;
import com.src.gameserver.network.serverpackets.QuestList;
import com.src.gameserver.network.serverpackets.ShortCutInit;
import com.src.gameserver.network.serverpackets.SignsSky;
import com.src.gameserver.network.serverpackets.SkillCoolTime;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.network.serverpackets.UserInfo;
import com.src.gameserver.templates.item.L2Item;
import com.src.gameserver.templates.skills.L2EffectType;
import com.src.gameserver.thread.TaskPriority;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.gameserver.util.FloodProtector;
import com.src.gameserver.util.Util;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;
import com.src.util.protection.nProtect;
import com.src.util.protection.nProtect.RestrictionType;

public class EnterWorld extends L2GameClientPacket
{
	private final static Log _log = LogFactory.getLog(EnterWorld.class);

	long _daysleft;
	SimpleDateFormat df = new SimpleDateFormat("dd MM yyyy");
	
	private static final String _C__03_ENTERWORLD = "[C] 03 EnterWorld";

	public TaskPriority getPriority()
	{
		return TaskPriority.PR_URGENT;
	}

	@Override
	protected void readImpl()
	{
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if(activeChar == null)
		{
			_log.warn("EnterWorld failed! activeChar is null...");
			getClient().closeNow();
			return;
		}
		if(!activeChar.isAttackable())
		{
			_log.warn("EnterWorld failed! activeChar is not attackable");
			getClient().closeNow();
			return;
		}

		FloodProtector.getInstance().registerNewPlayer(activeChar.getObjectId());

		if(Config.MAX_ITEM_ENCHANT_KICK > 0)
		{
			for(L2ItemInstance i : activeChar.getInventory().getItems())
			{
				if(!activeChar.isGM())
				{
					if(i.isEquipable())
					{
						if(i.getEnchantLevel() > Config.MAX_ITEM_ENCHANT_KICK)
						{
							activeChar.getInventory().destroyItem(null, i, activeChar, null);
							activeChar.sendMessage("You have over enchanted items you will be kicked from server!");
							activeChar.sendMessage("Respect our server rules.");
							sendPacket(new ExShowScreenMessage(" You have an over enchanted item, you will be kicked from server! ", 6000));
							Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " has Overenchanted  item! Kicked! ", Config.DEFAULT_PUNISH);
							_log.info("#### ATTENTION ####");
							_log.info(i + " item has been removed from " + activeChar);
						}

					}
				}
			}
		}

		if(!activeChar.isGM())
		{
			if(activeChar.getName().length() < 3 || activeChar.getName().length() > 16 || !Util.isAlphaNumeric(activeChar.getName()) || !isValidName(activeChar.getName()))
			{
				_log.warn("Charname: " + activeChar.getName() + " is invalid. EnterWorld failed.");
				getClient().closeNow();
				return;
			}
		}

		activeChar.setOnlineStatus(true);

		activeChar.setRunning();
		activeChar.standUp();
		activeChar.broadcastKarma();
		
		if(Config.DEFAULT_NAME_COLOR)
		{
			if(!activeChar.isGM())
			{
				activeChar.getAppearance().setNameColor(Integer.decode("0x" + "FFFFFF"));
			}
		}
		
		if(activeChar.isCursedWeaponEquiped()) 
		{ 
			SystemMessage msg = new SystemMessage(SystemMessageId.S2_OWNER_HAS_LOGGED_INTO_THE_S1_REGION); 
			msg.addZoneName(activeChar.getX(), activeChar.getY(), activeChar.getZ()); 
			msg.addItemName(activeChar.getCursedWeaponEquipedId()); 
			CursedWeaponsManager.announce(msg); 
			
			CursedWeapon cw = CursedWeaponsManager.getInstance().getCursedWeapon(activeChar.getCursedWeaponEquipedId()); 
			SystemMessage msg2 = new SystemMessage(SystemMessageId.S2_MINUTE_OF_USAGE_TIME_ARE_LEFT_FOR_S1); 
			int timeLeftInHours = (int)(((cw.getTimeLeft()/60000)/60)); 
			msg2.addItemName(activeChar.getCursedWeaponEquipedId()); 
			msg2.addNumber(timeLeftInHours*60); 
			activeChar.sendPacket(msg2); 
		}
		
		FunEventsManager.getInstance().notifyPlayerLogin(activeChar);

		if(Config.ALLOW_WEDDING)
		{
			engage(activeChar);
			notifyPartner(activeChar, activeChar.getPartnerId());
		}
       
		if (Config.ANNOUNCE_CASTLE_LORDS)
		{
			notifyCastleOwner(activeChar);
		}       

	    if (Hero.getInstance().isActiveHero(activeChar.getObjectId()))
	    {
	      activeChar.setIsHero(true);
	    }

		gmEnter(activeChar);

		ColorSystem(activeChar);

		if(!Config.ALT_DEV_NO_QUESTS)
		{
			Quest.playerEnter(activeChar);
			activeChar.sendPacket(new QuestList());
		}

		if(Config.PLAYER_SPAWN_PROTECTION > 0 && !activeChar.isInsideZone(L2Character.ZONE_PEACE))
		{
			activeChar.setProtection(true);
		}

		activeChar.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());

		if(SevenSigns.getInstance().isSealValidationPeriod())
		{
			sendPacket(new SignsSky());
		}
		
        if(Config.ALLOW_VIP_NCOLOR && activeChar.isVip())
        {
        	activeChar.getAppearance().setNameColor(Config.VIP_NCOLOR);
        }
        	 
        if(Config.ALLOW_VIP_TCOLOR && activeChar.isVip())
        {
        	activeChar.getAppearance().setTitleColor(Config.VIP_TCOLOR);
        }

		if(Config.ALLOW_AIO_NCOLOR && activeChar.isAio())
		{
			activeChar.getAppearance().setNameColor(Config.AIO_NCOLOR);
		}
		
		if(Config.ALLOW_AIO_TCOLOR && activeChar.isAio())
		{
			activeChar.getAppearance().setTitleColor(Config.AIO_TCOLOR);
		}
		
		if(activeChar.isAio())
		{
			onEnterAio(activeChar);	
		}

        if(activeChar.isVip())
        {
        	onEnterVip(activeChar);
        }
        
		if(Config.STORE_SKILL_COOLTIME)
		{
			activeChar.restoreEffects();
			activeChar.restoreHpMpOnLoad();
		}

		for(L2ItemInstance item : activeChar.getInventory().getItems())
		{
			if(!activeChar.isHero() && !activeChar.isGM())
			{
				if(item.getItemId() >= 6611 && item.getItemId() <= 6621 || item.getItemId() == 6842)
				{
					activeChar.getInventory().destroyItem(null, item, activeChar, null);
				}
			}
		}

		if(Config.NEWBIE_CHAR_BUFF && activeChar.getLevel() == 1)
		{
			if(activeChar.isMageClass())
			{
				SkillTable.getInstance().getInfo(1204, 1).getEffects(activeChar, activeChar);
				SkillTable.getInstance().getInfo(1040, 1).getEffects(activeChar, activeChar);
				SkillTable.getInstance().getInfo(4338, 1).getEffects(activeChar, activeChar);
				SkillTable.getInstance().getInfo(1048, 1).getEffects(activeChar, activeChar);
				SkillTable.getInstance().getInfo(1085, 1).getEffects(activeChar, activeChar);
				SkillTable.getInstance().getInfo(1078, 1).getEffects(activeChar, activeChar);
				SkillTable.getInstance().getInfo(1059, 1).getEffects(activeChar, activeChar);
			}
			else
			{
				SkillTable.getInstance().getInfo(1204, 1).getEffects(activeChar, activeChar);
				SkillTable.getInstance().getInfo(1040, 1).getEffects(activeChar, activeChar);
				SkillTable.getInstance().getInfo(4338, 1).getEffects(activeChar, activeChar);
				SkillTable.getInstance().getInfo(1045, 1).getEffects(activeChar, activeChar);
				SkillTable.getInstance().getInfo(1268, 1).getEffects(activeChar, activeChar);
				SkillTable.getInstance().getInfo(1044, 1).getEffects(activeChar, activeChar);
				SkillTable.getInstance().getInfo(1086, 1).getEffects(activeChar, activeChar);
			}
		}

		activeChar.sendPacket(new EtcStatusUpdate(activeChar));

		if(activeChar.getAllEffects() != null)
		{
			for(L2Effect e : activeChar.getAllEffects())
			{
				if(e.getEffectType() == L2EffectType.HEAL_OVER_TIME)
				{
					activeChar.stopEffects(L2EffectType.HEAL_OVER_TIME);
					activeChar.removeEffect(e);
				}

				if(e.getEffectType() == L2EffectType.COMBAT_POINT_HEAL_OVER_TIME)
				{
					activeChar.stopEffects(L2EffectType.COMBAT_POINT_HEAL_OVER_TIME);
					activeChar.removeEffect(e);
				}

				if(e.getEffectType() == L2EffectType.BATTLE_FORCE)
				{
					activeChar.stopEffects(L2EffectType.BATTLE_FORCE);
					activeChar.removeEffect(e);
				}

				if(e.getEffectType() == L2EffectType.SPELL_FORCE)
				{
					activeChar.stopEffects(L2EffectType.SPELL_FORCE);
					activeChar.removeEffect(e);
				}
			}
		}

		for(L2ItemInstance temp : activeChar.getInventory().getAugmentedItems())
		{
			if(temp != null && temp.isEquipped())
			{
				temp.getAugmentation().applyBoni(activeChar);
			}
		}

		activeChar.restoreCustomStatus();

		ExStorageMaxCount esmc = new ExStorageMaxCount(activeChar);
		activeChar.sendPacket(esmc);

		activeChar.getMacroses().sendUpdate();

		sendPacket(new ClientSetTime());
		sendPacket(new UserInfo(activeChar));
		sendPacket(new HennaInfo(activeChar));
		sendPacket(new FriendList(activeChar));
		sendPacket(new ItemList(activeChar, false));
		sendPacket(new ShortCutInit(activeChar));

		activeChar.sendSkillList();
		
		activeChar.getInventory().reloadEquippedItems();
		
		sendPacket(new SystemMessage(SystemMessageId.WELCOME_TO_LINEAGE));
		
		activeChar.sendMessage("Thank you for using L2JDream Project.");
		
		SevenSigns.getInstance().sendCurrentPeriodMsg(activeChar);
		Announcements.getInstance().showAnnouncements(activeChar);

		if(activeChar.getRace().ordinal() == 2)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(294, 1);
			if(skill != null && activeChar.getSkillLevel(294) == 1)
			{
				if(GameTimeController.getInstance().isNowNight())
				{
					sendPacket(new SystemMessage(SystemMessageId.NIGHT_EFFECT_APPLIES).addSkillName(294));
				}
				else
				{
					sendPacket(new SystemMessage(SystemMessageId.DAY_EFFECT_DISAPPEARS).addSkillName(294));
				}
			}
		}

		if(!Config.ALT_DEV_NO_QUESTS)
		{
			loadTutorial(activeChar);
		}
		
		if(Config.CHECK_SKILLS_ON_ENTER && !Config.ALT_GAME_SKILL_LEARN)
		{
			if(!activeChar.isAio())
			activeChar.checkAllowedSkills();
		}

		PetitionManager.getInstance().checkPetitionMessages(activeChar);

		if(activeChar.getClanId() != 0 && activeChar.getClan() != null)
		{
			sendPacket(new PledgeShowMemberListAll(activeChar.getClan(), activeChar));
			sendPacket(new PledgeStatusChanged(activeChar.getClan()));
		}

		if(activeChar.isAlikeDead())
			sendPacket(new Die(activeChar));

		if(Config.ALLOW_WATER)
		{
			activeChar.checkWaterState();
		}

		setPledgeClass(activeChar);

		notifyFriends(activeChar);
		notifyClanMembers(activeChar);
		notifySponsorOrApprentice(activeChar);

		activeChar.onPlayerEnter();

		sendPacket(new SkillCoolTime(activeChar));
		
		if(Olympiad.getInstance().playerInStadia(activeChar))
		{
			activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			activeChar.sendMessage("You have been teleported to the nearest town due to you being in an Olympiad Stadium.");
		}

		if(DimensionalRiftManager.getInstance().checkIfInRiftZone(activeChar.getX(), activeChar.getY(), activeChar.getZ(), false))
		{
			DimensionalRiftManager.getInstance().teleportToWaitingRoom(activeChar);
		}

		if(activeChar.getClanJoinExpiryTime() > System.currentTimeMillis())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CLAN_MEMBERSHIP_TERMINATED));
		}

		try  
		{  
			//Sets the apropriate Pledge Class for the clannie (e.g. Viscount, Count, Baron, Marquiz)   
			activeChar.setPledgeClass(L2ClanMember.getCurrentPledgeClass(activeChar));  
			
			//Restores clan skills on world entry (they don't show up for themselves) :)  
			activeChar.getClan().addSkillEffects(activeChar);  
			
		}  
		
		catch(Throwable t)
		{
			
		}
	 	
		if(activeChar.getClan() != null)
		{
			PledgeSkillList psl = new PledgeSkillList(activeChar.getClan());  
			activeChar.sendPacket(psl);

			for(Siege siege : SiegeManager.getInstance().getSieges())
			{
				if(!siege.getIsInProgress())
				{
					continue;
				}

				if(siege.checkIsAttacker(activeChar.getClan()))
				{
					activeChar.setSiegeState((byte) 1);
					break;
				}
				else if(siege.checkIsDefender(activeChar.getClan()))
				{
					activeChar.setSiegeState((byte) 2);
					break;
				}
			}

			ClanHall clanHall = ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan());

			if(clanHall != null)
			{
				if(!clanHall.getPaid())
				{
					clanHall.updateDb();
					activeChar.sendPacket(new SystemMessage(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW));
				}
			}
		}

		if(!activeChar.isGM() && activeChar.getSiegeState() < 2 && activeChar.isInsideZone(L2Character.ZONE_SIEGE))
		{
			activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			activeChar.sendMessage("You have been teleported to the nearest town due to you being in siege zone.");
		}

		RegionBBSManager.getInstance().changeCommunityBoard();

		if(Config.ALLOW_REMOTE_CLASS_MASTERS)
		{
			ClassLevel lvlnow = PlayerClass.values()[activeChar.getClassId().getId()].getLevel();

			if(activeChar.getLevel() >= 20 && lvlnow == ClassLevel.First)
			{
				L2ClassMasterInstance.ClassMaster.onAction(activeChar);
			}
			else if(activeChar.getLevel() >= 40 && lvlnow == ClassLevel.Second)
			{
				L2ClassMasterInstance.ClassMaster.onAction(activeChar);
			}
			else if(activeChar.getLevel() >= 76 && lvlnow == ClassLevel.Third)
			{
				L2ClassMasterInstance.ClassMaster.onAction(activeChar);
			}
		}

		if(!Config.ALLOW_DUALBOX)
		{
			String thisip = activeChar.getClient().getConnection().getInetAddress().getHostAddress();
			Collection<L2PcInstance> allPlayers = L2World.getInstance().getAllPlayers();
			L2PcInstance[] players = allPlayers.toArray(new L2PcInstance[allPlayers.size()]);

			for(L2PcInstance player : players)
			{
				if(player != null)
				{
					String ip = player.getClient().getConnection().getInetAddress().getHostAddress();
					if(thisip.equals(ip) && activeChar != player && player != null)
					{
						activeChar.sendMessage("I'm sorry, but multibox is not allowed here.");
						activeChar.logout();
					}
				}
			}
		}

		Hellows(activeChar);

		if (Config.ALLOW_MESSAGE_ON_ENTER)
			activeChar.sendPacket(new ExShowScreenMessage(Config.MESSAGE_ON_ENTER, 6000, 0x02, true));

        if(Config.PLAYERS_ONLINE_LOGIN)
        {
            activeChar.sendMessage((new StringBuilder()).append("Players online: ").append(L2World.getInstance().getAllPlayers().size() + Config.PLAYERS_ONLINE_TRICK).toString());
        }

		if(!nProtect.getInstance().checkRestriction(activeChar, RestrictionType.RESTRICT_ENTER))
		{
			activeChar.setIsImobilised(true);
			activeChar.disableAllSkills();
			ThreadPoolManager.getInstance().scheduleGeneral(new Disconnection(activeChar), 20000);
		}

		if(Config.AUTO_ACTIVATE_SHOTS)
		{
			verifyAndLoadShots(activeChar);
		}
	}

	private void gmEnter(L2PcInstance activeChar)
	{
		if(activeChar.isGM())
		{
			if(Config.GM_SPECIAL_EFFECT)
			{
				Earthquake eq = new Earthquake(activeChar.getX(), activeChar.getY(), activeChar.getZ(), 50, 4);
				activeChar.broadcastPacket(eq);
			}

			if(Config.GM_GIVE_SPECIAL_SKILLS)
			{
				GMSkillTable.getInstance().addSkills(activeChar);
			}

			if(Config.GM_SHOW_LOGIN)
			{
				String gmname = activeChar.getName();
				String text = "[GM]" + gmname + " has logged on.";
				Announcements.getInstance().announceToAll(text);
			}

			if(Config.GM_STARTUP_INVULNERABLE && AdminCommandAccessRights.getInstance().hasAccess("admin_invul", activeChar.getAccessLevel()))
			{
				activeChar.setIsInvul(true);
			}

			if(Config.GM_SUPER_HASTE)
			{
				SkillTable.getInstance().getInfo(7029, 4).getEffects(activeChar, activeChar);
			}

			if(Config.GM_STARTUP_INVISIBLE && AdminCommandAccessRights.getInstance().hasAccess("admin_invisible", activeChar.getAccessLevel()))
			{
				activeChar.getAppearance().setInvisible();
			}

			if(Config.GM_STARTUP_SILENCE && AdminCommandAccessRights.getInstance().hasAccess("admin_silence", activeChar.getAccessLevel()))
			{
				activeChar.setMessageRefusal(true);
			}

			if(Config.GM_STARTUP_DIET && AdminCommandAccessRights.getInstance().hasAccess("admin_diet", activeChar.getAccessLevel()))
			{
				activeChar.setDietMode(true);
			}

			if(Config.GM_STARTUP_AUTO_LIST && AdminCommandAccessRights.getInstance().hasAccess("admin_gmliston", activeChar.getAccessLevel()))
			{
				GmListTable.getInstance().addGm(activeChar, false);
			}
			else
			{
				GmListTable.getInstance().addGm(activeChar, true);
			}

			activeChar.updateGmNameTitleColor();
		}
	}

	private void Hellows(L2PcInstance activeChar)
	{
		if(activeChar.getFirstLog() && Config.NEW_PLAYER_EFFECT)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(2025,1);
			if(skill != null)
			{
				MagicSkillUser MSU = new MagicSkillUser(activeChar, activeChar, 2025, 1, 1, 0);
				activeChar.sendPacket(MSU);
				activeChar.broadcastPacket(MSU);
				activeChar.useMagic(skill, false, false);
			}
			activeChar.setFirstLog(false);
			activeChar.updateFirstLog();
		}

		if((activeChar.getClan() != null) && activeChar.getClan().isNoticeEnabled())
		{
			String clanNotice = "data/html/communityboard/clannotice.htm";
			File mainText = new File(Config.DATAPACK_ROOT, clanNotice);
			if(mainText.exists())
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile(clanNotice);
				html.replace("%clan_name%", activeChar.getClan().getName());
				html.replace("%notice_text%", activeChar.getClan().getNotice().replaceAll("\r\n", "<br>"));
				sendPacket(html);
			}
		}

		if(Config.GM_WELCOME_HTM && activeChar.isGM() && isValidName(activeChar.getName()))
		{
			String Welcome_Path = "data/html/mods/welcome/welcomegm.htm";
			File mainText = new File(Config.DATAPACK_ROOT, Welcome_Path);
			if(mainText.exists())
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile(Welcome_Path);
				html.replace("%name%", activeChar.getName());
				sendPacket(html);
			}
		}
		else if(Config.WELCOME_HTM && isValidName(activeChar.getName()))
		{
			String Welcome_Path = "data/html/mods/welcome/welcome.htm";
			File mainText = new File(Config.DATAPACK_ROOT, Welcome_Path);
			if(mainText.exists())
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile(Welcome_Path);
				html.replace("%name%", activeChar.getName());
				html.replace("%rate_xp%", String.valueOf(Config.RATE_XP));
				html.replace("%rate_sp%", String.valueOf(Config.RATE_SP));
				html.replace("%rate_party_xp%", String.valueOf(Config.RATE_PARTY_XP));
				html.replace("%rate_party_sp%", String.valueOf(Config.RATE_PARTY_SP));
				html.replace("%rate_adena%", String.valueOf(Config.RATE_DROP_ADENA));
				html.replace("%rate_items%", String.valueOf(Config.RATE_DROP_ITEMS));
				html.replace("%rate_spoil%", String.valueOf(Config.RATE_DROP_SPOIL));
				html.replace("%rate_drop_manor%", String.valueOf(Config.RATE_DROP_MANOR));
				html.replace("%rate_quest_reward%", String.valueOf(Config.RATE_QUESTS_REWARD));
				html.replace("%rate_drop_quest%", String.valueOf(Config.RATE_DROP_QUEST));
				html.replace("%pet_rate_xp%", String.valueOf(Config.PET_XP_RATE));
				html.replace("%sineater_rate_xp%", String.valueOf(Config.SINEATER_XP_RATE));
				html.replace("%pet_food_rate%", String.valueOf(Config.PET_FOOD_RATE));
				sendPacket(html);
			}
		}
	}

	private void ColorSystem(L2PcInstance activeChar)
	{
		if (Config.PVP_COLOR_SYSTEM_ENABLED)
		{
			activeChar.updatePvPColor();
		}
		if (Config.PK_COLOR_SYSTEM_ENABLED)
		{
			activeChar.updatePkColor();
		}
		
		if(activeChar.getClan() != null && activeChar.isClanLeader() && Config.CLAN_LEADER_COLOR_ENABLED && activeChar.getClan().getLevel() >= Config.CLAN_LEADER_COLOR_CLAN_LEVEL)
		{
			if(Config.CLAN_LEADER_COLORED == 1)
			{
				activeChar.getAppearance().setNameColor(Config.CLAN_LEADER_COLOR);
			}
			else
			{
				activeChar.getAppearance().setTitleColor(Config.CLAN_LEADER_COLOR);
			}
		}

		activeChar.updateNameTitleColor();
	}

	private void verifyAndLoadShots(L2PcInstance activeChar)
	{
		int soulId = -1;
		int spiritId = -1;
		int bspiritId = -1;

		if(!activeChar.isDead() && activeChar.getActiveWeaponItem() != null)
		{
			switch(activeChar.getActiveWeaponItem().getCrystalType())
			{
				case L2Item.CRYSTAL_NONE:
					soulId = 1835;
					spiritId = 2509;
					bspiritId = 3947;
					break;
				case L2Item.CRYSTAL_D:
					soulId = 1463;
					spiritId = 2510;
					bspiritId = 3948;
					break;
				case L2Item.CRYSTAL_C:
					soulId = 1464;
					spiritId = 2511;
					bspiritId = 3949;
					break;
				case L2Item.CRYSTAL_B:
					soulId = 1465;
					spiritId = 2512;
					bspiritId = 3950;
					break;
				case L2Item.CRYSTAL_A:
					soulId = 1466;
					spiritId = 2513;
					bspiritId = 3951;
					break;
				case L2Item.CRYSTAL_S:
					soulId = 1467;
					spiritId = 2514;
					bspiritId = 3952;
					break;
			}

			if((soulId > -1) && activeChar.getInventory().getInventoryItemCount(soulId, -1) > Config.AUTO_ACTIVATE_SHOTS_MIN)
			{
				activeChar.addAutoSoulShot(soulId);
				activeChar.sendPacket(new ExAutoSoulShot(soulId, 1));
				activeChar.sendPacket(new SystemMessage(SystemMessageId.USE_OF_S1_WILL_BE_AUTO).addItemName(activeChar.getInventory().getItemByItemId(soulId)));
			}

			if((bspiritId > -1) && activeChar.getInventory().getInventoryItemCount(bspiritId, -1) > Config.AUTO_ACTIVATE_SHOTS_MIN)
			{
				activeChar.addAutoSoulShot(bspiritId);
				activeChar.sendPacket(new ExAutoSoulShot(bspiritId, 1));
				activeChar.sendPacket(new SystemMessage(SystemMessageId.USE_OF_S1_WILL_BE_AUTO).addItemName(activeChar.getInventory().getItemByItemId(bspiritId)));
			}
			else if((spiritId > -1) && activeChar.getInventory().getInventoryItemCount(spiritId, -1) > Config.AUTO_ACTIVATE_SHOTS_MIN)
			{
				activeChar.addAutoSoulShot(spiritId);
				activeChar.sendPacket(new ExAutoSoulShot(spiritId, 1));
				activeChar.sendPacket(new SystemMessage(SystemMessageId.USE_OF_S1_WILL_BE_AUTO).addItemName(activeChar.getInventory().getItemByItemId(spiritId)));
			}

			activeChar.rechargeAutoSoulShot(true, true, false);
		}
	}

	private void engage(L2PcInstance cha)
	{
		int _chaid = cha.getObjectId();

		for(Wedding cl : CoupleManager.getInstance().getCouples())
		{
			if(cl.getPlayer1Id() == _chaid || cl.getPlayer2Id() == _chaid)
			{
				if(cl.getMaried())
				{
					cha.setMarried(true);
				}

				cha.setCoupleId(cl.getId());

				if(cl.getPlayer1Id() == _chaid)
				{
					cha.setPartnerId(cl.getPlayer2Id());
				}
				else
				{
					cha.setPartnerId(cl.getPlayer1Id());
				}
			}
		}
	}

	private void notifyPartner(L2PcInstance cha, int partnerId)
	{
		if(cha.getPartnerId() != 0)
		{
			L2PcInstance partner = null;

			if(L2World.getInstance().findObject(cha.getPartnerId()) instanceof L2PcInstance)
			{
				partner = (L2PcInstance) L2World.getInstance().findObject(cha.getPartnerId());
			}

			if(partner != null)
			{
				partner.sendMessage("Your Partner has logged in.");
			}
		}
	}

	private void notifyFriends(L2PcInstance cha)
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("SELECT friend_name FROM character_friends WHERE char_id = ?");
			statement.setInt(1, cha.getObjectId());
			ResultSet rset = statement.executeQuery();

			L2PcInstance friend;
			String friendName;

			while(rset.next())
			{
				friendName = rset.getString("friend_name");

				friend = L2World.getInstance().getPlayer(friendName);

				if(friend != null)
				{
					friend.sendPacket(new FriendList(friend));
					friend.sendPacket(new SystemMessage(SystemMessageId.FRIEND_S1_HAS_LOGGED_IN).addString(cha.getName()));
				}
			}

			rset.close();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("could not restore friend data", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	private void notifyClanMembers(L2PcInstance activeChar)
	{
		L2Clan clan = activeChar.getClan();
		if(clan != null)
		{
			clan.getClanMember(activeChar.getObjectId()).setPlayerInstance(activeChar);
			clan.broadcastToOtherOnlineMembers((new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_LOGGED_IN).addString(activeChar.getName())), activeChar);
			clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(activeChar), activeChar);
		}
	}

	private void notifySponsorOrApprentice(L2PcInstance activeChar)
	{
		if(activeChar.getSponsor() != 0)
		{
			L2PcInstance sponsor = (L2PcInstance) L2World.getInstance().findObject(activeChar.getSponsor());

			if(sponsor != null)
			{
				sponsor.sendPacket(new SystemMessage(SystemMessageId.YOUR_APPRENTICE_S1_HAS_LOGGED_IN).addString(activeChar.getName()));
			}
		}
		else if(activeChar.getApprentice() != 0)
		{
			L2PcInstance apprentice = (L2PcInstance) L2World.getInstance().findObject(activeChar.getApprentice());

			if(apprentice != null)
			{
				apprentice.sendPacket(new SystemMessage(SystemMessageId.YOUR_SPONSOR_S1_HAS_LOGGED_IN).addString(activeChar.getName()));
			}
		}
	}

	private void loadTutorial(L2PcInstance player)
	{
		QuestState qs = player.getQuestState("Q255_Tutorial");

		if(qs != null)
		{
			qs.getQuest().notifyEvent("UC", null, player);
		}
	}

	private void setPledgeClass(L2PcInstance activeChar)
	{
		int pledgeClass = 0;

		if(activeChar.getClan() != null)
		{
			activeChar.getClan().getClanMember(activeChar.getObjectId());
			pledgeClass = L2ClanMember.calculatePledgeClass(activeChar);
		}

		if(activeChar.isNoble() && pledgeClass < 5)
		{
			pledgeClass = 5;
		}

		if(activeChar.isHero())
		{
			pledgeClass = 8;
		}

		activeChar.setPledgeClass(pledgeClass);
	}

	private boolean isValidName(String text)
	{
		boolean result = true;

		String test = text;
		Pattern pattern;

		try
		{
			pattern = Pattern.compile(Config.CNAME_TEMPLATE);
		}
		catch(PatternSyntaxException e)
		{
			_log.error("", e);
			_log.warn("ERROR : Character name pattern of config is wrong!");
			pattern = Pattern.compile(".*");
		}

		Matcher regexp = pattern.matcher(test);
		if(!regexp.matches())
		{
			result = false;
		}

		return result;
	}
	
	private void onEnterVip(L2PcInstance activeChar)
	{
		long now = Calendar.getInstance().getTimeInMillis();
		long endDay = activeChar.getVipEndTime();
		if(now > endDay)
		{
			activeChar.setVip(false);
			activeChar.setVipEndTime(0);
			activeChar.sendChatMessage(0, 0, "SYS", "Removed your Vip stats... period ends ");
		}
		else
		{
			Date dt = new Date(endDay);
			_daysleft = (endDay - now)/86400000;
			if(_daysleft > 30)
				activeChar.sendMessage("Vip period ends in " + df.format(dt) + ". enjoy the Game");
			else if(_daysleft > 0)
				activeChar.sendMessage("left " + (int)_daysleft + " days for Vip period ends");
			else if(_daysleft < 1)
			{
				long hour = (endDay - now)/3600000;
				activeChar.sendMessage("left " + (int)hour + " hours to Vip period ends");
			}
		}
	}

	private void onEnterAio(L2PcInstance activeChar)
	{
		if (Config.ALLOW_AIO_ITEM && activeChar.isAio())
		{
			if(activeChar.getInventory().getItemByItemId(Config.AIO_ITEMID) == null)
			{
				activeChar.addItem("Aio", Config.AIO_ITEMID, 1, activeChar, true);
				activeChar.getInventory().updateDatabase();
			}
		}
		long now = Calendar.getInstance().getTimeInMillis();
		long endDay = activeChar.getAioEndTime();
		if(now > endDay)
		{
			activeChar.setAio(false);
			activeChar.setAioEndTime(0);
			//activeChar.lostAioSkills();
			if (Config.ALLOW_AIO_ITEM && activeChar.isAio() == false)
			{
				activeChar.getInventory().destroyItemByItemId("", Config.AIO_ITEMID, 1, activeChar, null);
				activeChar.getWarehouse().destroyItemByItemId("", Config.AIO_ITEMID, 1, activeChar, null);
			}
			activeChar.sendChatMessage(0, 0, "SYS", "Removed your Aio stats... period ends ");
				
			for(L2Skill skill : activeChar.getAllSkills())
			{
				activeChar.removeSkill(skill);
			}
		}
		else
		{
			Date dt = new Date(endDay);
			_daysleft = (endDay - now) / 86400000;
			if(_daysleft > 30)
			{
				activeChar.sendMessage("Aio period ends in " + df.format(dt) + ". enjoy the Game");
			}
			else if(_daysleft > 0)
			{
				activeChar.sendMessage("Left " + (int) _daysleft + " for Aio period ends");
			}
			else if(_daysleft < 1)
			{
				long hour = (endDay - now) / 3600000;
				activeChar.sendMessage("Left " + (int) hour + " hours to Aio period ends");
			}
		}
	}

	private void notifyCastleOwner(L2PcInstance activeChar)
	{
		L2Clan clan = activeChar.getClan();
		
		if (clan != null)
		{
			if (clan.getHasCastle() > 0)
			{
				Castle castle = CastleManager.getInstance().getCastleById(clan.getHasCastle());
				if ((castle != null) && (activeChar.getObjectId() == clan.getLeaderId()))
					Announcements.getInstance().announceToAll("Lord " + activeChar.getName() + " Ruler Of " + castle.getName() + " Castle is Now Online!");
			}
		}
	}
	
	@Override
	public String getType()
	{
		return _C__03_ENTERWORLD;
	}
	
}