/*
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
package com.src;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;

import javolution.text.TypeFormat;
import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.util.object.L2Properties;
import com.src.util.services.ClassMasterSettings;
import com.src.util.services.ConfigFiles;
import com.src.util.services.ServerType;

public final class Config
{
	private static final Log _log = LogFactory.getLog(Config.class);
	//============================================================
	// custom.ini
	//============================================================
	/** ExShowScreenMessage on Enter */
	public static boolean ALLOW_MESSAGE_ON_ENTER;
	public static String MESSAGE_ON_ENTER;
	/** Online */
	public static boolean PLAYERS_ONLINE_LOGIN;
	public static int PLAYERS_ONLINE_TRICK;
	/** Welcome */
	public static boolean WELCOME_HTM;
	/** Spawnlist */
	public static boolean SAVE_GMSPAWN_ON_CUSTOM;
	public static boolean DELETE_GMSPAWN_ON_CUSTOM;
	public static boolean DELETE_SPAWN_ON_SPAWNLIST;
	/** Restrictions */
	public static boolean CASTLE_SHIELD;
	public static boolean CLANHALL_SHIELD;
	public static boolean APELLA_ARMORS;
	public static boolean OATH_ARMORS;
	public static boolean CASTLE_CROWN;
	public static boolean CASTLE_CIRCLETS;
	public static int CRUMA_TOWER_LEVEL_RESTRICT;
	/** Sub Class */
	public static boolean KEEP_SUBCLASS_SKILLS;
	public static int ALLOWED_SUBCLASS;
	/** Character */
	public static boolean DEFAULT_NAME_COLOR;
	public static boolean NEW_PLAYER_EFFECT;
	public static boolean CHAR_TITLE;
	public static String ADD_CHAR_TITLE;
	public static boolean ALLOW_CREATE_LVL;
	public static int CHAR_CREATE_LVL;
	public static boolean SPAWN_CHAR;
	public static int SPAWN_X;
	public static int SPAWN_Y;
	public static int SPAWN_Z;
	public static int PLAYER_PROTECTION_LEVEL;
	public static boolean STORE_ZONE_PEACE;
	public static boolean DISABLE_WEIGHT_PENALTY;
	public static boolean DISABLE_GRADE_PENALTY;
	public static boolean DISABLE_LOST_EXP;
	/** Newbie */
	public static int MAX_LEVEL_NEWBIE;
	public static boolean NEWBIE_CHAR_BUFF;
	public static boolean ADENA_NEWBIE;
	public static int ADENA_NEWBIE_LVL;
	public static float RATE_DROP_ADENA_NEWBIE;
	/** Soul Shots */
	public static boolean DONT_DESTROY_SS;
	public static boolean AUTO_ACTIVATE_SHOTS;
	public static int AUTO_ACTIVATE_SHOTS_MIN;
	/** Raid Bosses */
	public static boolean ALLOW_RAID_BOSS_PUT;
	public static int MONSTER_RETURN_DELAY;
	public static boolean PLAYERS_CAN_HEAL_RB;
	public static int RB_LOCK_RANGE;
	public static int GB_LOCK_RANGE;
	/** Clan Leader */
	public static int CLAN_LEADER_COLOR;
	public static int CLAN_LEADER_COLOR_CLAN_LEVEL;
	public static boolean CLAN_LEADER_COLOR_ENABLED;
	public static int CLAN_LEADER_COLORED;
	public static int MAX_MULTISELL;
	/** Chat Social Actions */
	public static boolean SAY_SOCIAL;
	/** Other Modifications */
	public static boolean ALLOW_REGEN_SYSTEM;
	public static boolean SELL_BY_ITEM;
	public static int SELL_ITEM;
	public static float REGEN_SYSTEM_CP;
	public static float REGEN_SYSTEM_HP;
	public static float REGEN_SYSTEM_MP;
	/** Localization */
	public static boolean MULTILANG_ENABLE;
	public static String MULTILANG_DEFAULT;
	public static ArrayList<String> MULTILANG_ALLOWED = new ArrayList<String>();
	public static boolean BANKING_SYSTEM_ENABLED;
	public static int BANKING_SYSTEM_GOLDBARS;
	public static int BANKING_SYSTEM_ADENA;
	/** Champion Mobs */
	public static boolean CHAMPION_ENABLE;
	public static int CHAMPION_FREQUENCY;
	public static int CHAMP_MIN_LVL;
	public static int CHAMP_MAX_LVL;
	public static int CHAMPION_HP;
	public static float CHAMPION_HP_REGEN;
	public static int CHAMPION_REWARDS;
	public static int CHAMPION_ADENAS_REWARDS;
	public static float CHAMPION_ATK;
	public static float CHAMPION_SPD_ATK;
	public static int CHAMPION_REWARD_ITEM;
	public static int CHAMPION_REWARD_ITEM_ID;
	public static int CHAMPION_REWARD_ITEM_QTY;
	public static String CHAMPION_TITLE;
	/** Offline Trade */
	public static boolean OFFLINE_TRADE_ENABLE;
	public static boolean OFFLINE_CRAFT_ENABLE;
	public static boolean OFFLINE_SET_NAME_COLOR;
	public static int OFFLINE_NAME_COLOR;
	public static boolean OFFLINE_SLEEP_EFFECT;
	public static boolean OFFLINE_RESTORE;
	public static int OFFLINE_MAX_DAYS;
	public static boolean OFFLINE_DISCONNECT_FINISHED;
	/** Wedding System */
	public static boolean ALLOW_WEDDING;
	public static boolean WEDDING_ANNOUNCE;
	public static int WEDDING_PRICE;
	public static boolean WEDDING_PUNISH_INFIDELITY;
	public static boolean WEDDING_TELEPORT;
	public static int WEDDING_TELEPORT_PRICE;
	public static int WEDDING_TELEPORT_DURATION;
	public static boolean WEDDING_SAMESEX;
	public static boolean WEDDING_FORMALWEAR;
	public static int WEDDING_DIVORCE_COSTS;
	public static boolean WEDDING_GIVE_CUPID_BOW;
	public static boolean WEDDING_SAMEIP;
	/** Announce System */
	public static boolean ANNOUNCE_PVP_KILL;
	public static boolean ANNOUNCE_PK_KILL;
	public static boolean ANNOUNCE_ALL_KILL;
	public static boolean ANNOUNCE_CASTLE_LORDS;
	/** PvP & Pk Color System */
	public static boolean PVP_COLOR_SYSTEM_ENABLED;
	public static String PVP_COLOR;
	public static FastMap<Integer, Integer> PVP_COLOR_LIST;	
	public static boolean PK_COLOR_SYSTEM_ENABLED;
	public static String PK_COLOR;
	public static FastMap<Integer, Integer> PK_COLOR_LIST;
	/** Aio System */
	public static boolean ENABLE_AIO_SYSTEM;
	public static Map<Integer, Integer> AIO_SKILLS;
	public static boolean ALLOW_AIO_NCOLOR;
	public static int AIO_NCOLOR;
	public static boolean ALLOW_AIO_TCOLOR;
	public static int AIO_TCOLOR;
	public static boolean ALLOW_AIO_ITEM;
	public static int AIO_ITEMID;
	public static int AIO_ITEM_SKILL;
	public static boolean ALLOW_AIO_USE_GK;
	public static boolean ALLOW_AIO_USE_CM;
	public static boolean ALLOW_AIO_BLOCK_EVENT;
	public static boolean ALLOW_AIO_LEAVE_TOWN;
	public static boolean ALLOW_AIO_ENTER_IN_BOSS_ZONE;
	//public static int AIO_ITEM;
    //public static int AIO_DIAS;
	//public static int AIO_ITEM2;
    //public static int AIO_DIAS2;
    /** Vip system **/
    public static boolean ALLOW_VIP_NCOLOR;
    public static int VIP_NCOLOR;
    public static boolean ALLOW_VIP_TCOLOR;
    public static int VIP_TCOLOR;
    public static boolean ALLOW_VIP_XPSP;
    public static int VIP_XP;
    public static int VIP_SP;
	public static float VIP_ADENA_RATE;
	public static float VIP_DROP_RATE;
	public static float VIP_SPOIL_RATE;
    /*/** Clan Item **/
    //public static boolean CLAN_TITLE;
    //public static String ADD_CLAN_TITLE;
    //public static boolean ENABLE_CLAN_ITEM;
    //public static Map<Integer, Integer> CLAN_ITEM_SKILLS;
    //public static int CLAN_ITEM_ID;
    //public static boolean ALLOW_ITEM_SET_LEVEL;
    //public static byte CLAN_ITEM_LEVEL;
    //public static boolean ALLOW_REPUTATION_ITEM;
    //public static int REPUTATION_ITEM_QUANTITY;
    //public static boolean ALLOW_CLAN_TCOLOR;
    //public static int CLAN_ITEM_TCOLOR;
    
	//============================================================
	public static void CustomConfig()
	{
		try
		{
			L2Properties p = new L2Properties(ConfigFiles.CUSTOM_INI);

			/** ExShowScreenMessage on Enter */
			ALLOW_MESSAGE_ON_ENTER = TypeFormat.parseBoolean(p.getProperty("AllowMessageOnEnter", "False"));
			MESSAGE_ON_ENTER = p.getProperty("MessageOnEnter", "L2Dream Project!");
			/** Online */
			PLAYERS_ONLINE_LOGIN = TypeFormat.parseBoolean(p.getProperty("OnlineOnLogin", "False"));
			PLAYERS_ONLINE_TRICK = TypeFormat.parseInt(p.getProperty("OnlinePlayerAdd", "0"));
			/** Welcome */
			WELCOME_HTM = TypeFormat.parseBoolean(p.getProperty("WelcomeHtm", "False"));
			/** Spawnlist */
			SAVE_GMSPAWN_ON_CUSTOM = Boolean.valueOf(p.getProperty("SaveGmSpawnOnCustom", "True")); 
			DELETE_GMSPAWN_ON_CUSTOM = Boolean.valueOf(p.getProperty("DeleteGmSpawnOnCustom", "True")); 
			DELETE_SPAWN_ON_SPAWNLIST = Boolean.valueOf(p.getProperty("DeleteSpawnOnSpawnlist", "False"));
			/** Restrictions */
			CASTLE_SHIELD = TypeFormat.parseBoolean(p.getProperty("CastleShieldRestriction", "True"));
			CLANHALL_SHIELD = TypeFormat.parseBoolean(p.getProperty("ClanHallShieldRestriction", "True"));
			APELLA_ARMORS = TypeFormat.parseBoolean(p.getProperty("ApellaArmorsRestriction", "True"));
			OATH_ARMORS = TypeFormat.parseBoolean(p.getProperty("OathArmorsRestriction", "True"));
			CASTLE_CROWN = TypeFormat.parseBoolean(p.getProperty("CastleLordsCrownRestriction", "True"));
			CASTLE_CIRCLETS = TypeFormat.parseBoolean(p.getProperty("CastleCircletsRestriction", "True"));
			CRUMA_TOWER_LEVEL_RESTRICT = TypeFormat.parseInt(p.getProperty("CrumaTowerLevelRestrict", "56"));
			/** Sub Class */
			KEEP_SUBCLASS_SKILLS = TypeFormat.parseBoolean(p.getProperty("KeepSubClassSkills", "False"));
			ALLOWED_SUBCLASS = TypeFormat.parseInt(p.getProperty("AllowedSubclass", "3"));
			/** Character */
			DEFAULT_NAME_COLOR = TypeFormat.parseBoolean(p.getProperty("DefaultNameColorOnEnter", "False"));
			NEW_PLAYER_EFFECT = TypeFormat.parseBoolean(p.getProperty("NewPlayerEffect", "False"));
			CHAR_TITLE = TypeFormat.parseBoolean(p.getProperty("CharTitle", "False"));
			ADD_CHAR_TITLE = p.getProperty("CharAddTitle", "Welcome");
			ALLOW_CREATE_LVL = TypeFormat.parseBoolean(p.getProperty("CustomStartingLvl", "False"));
			CHAR_CREATE_LVL = TypeFormat.parseInt(p.getProperty("CharLvl", "80"));
			SPAWN_CHAR = TypeFormat.parseBoolean(p.getProperty("CustomSpawn", "False"));
			SPAWN_X = TypeFormat.parseInt(p.getProperty("SpawnX", ""));
			SPAWN_Y = TypeFormat.parseInt(p.getProperty("SpawnY", ""));
			SPAWN_Z = TypeFormat.parseInt(p.getProperty("SpawnZ", ""));
			PLAYER_PROTECTION_LEVEL = TypeFormat.parseInt(p.getProperty("PlayerProtectionLevel", "0"));
			STORE_ZONE_PEACE = TypeFormat.parseBoolean(p.getProperty("StoreOnlyInPeaceZone", "False"));
			DISABLE_WEIGHT_PENALTY = TypeFormat.parseBoolean(p.getProperty("DisableWeightPenalty", "False"));
			DISABLE_GRADE_PENALTY  = TypeFormat.parseBoolean(p.getProperty("DisableGradePenalty", "False"));
			DISABLE_LOST_EXP  = TypeFormat.parseBoolean(p.getProperty("DisableLostExp", "False"));
			/** Newbie */
			MAX_LEVEL_NEWBIE = TypeFormat.parseInt(p.getProperty("NewbieMaxLevel", "20"));
			NEWBIE_CHAR_BUFF = TypeFormat.parseBoolean(p.getProperty("NewbieBuffCharacter", "False"));
			ADENA_NEWBIE = TypeFormat.parseBoolean(p.getProperty("NewbieAdenaRate", "False"));
			ADENA_NEWBIE_LVL = TypeFormat.parseInt(p.getProperty("NewbieAdenaMaxLvL", "40"));
			RATE_DROP_ADENA_NEWBIE = TypeFormat.parseFloat(p.getProperty("NewbieAdenaeDropRate", "1.00"));
			/** Soul Shots */
			DONT_DESTROY_SS = TypeFormat.parseBoolean(p.getProperty("DontDestroySS", "False"));
			AUTO_ACTIVATE_SHOTS = TypeFormat.parseBoolean(p.getProperty("AutoActivateShotsEnabled", "False"));;
			AUTO_ACTIVATE_SHOTS_MIN = TypeFormat.parseInt(p.getProperty("AutoActivateShotsMin", "200"));
			/** Raid Bosses */
			ALLOW_RAID_BOSS_PUT = TypeFormat.parseBoolean(p.getProperty("AllowRaidBossPetrified", "True"));
			MONSTER_RETURN_DELAY = TypeFormat.parseInt(p.getProperty("MonsterReturnDelay", "0"));
			PLAYERS_CAN_HEAL_RB = TypeFormat.parseBoolean(p.getProperty("PlayersCanHealRb", "True"));
			RB_LOCK_RANGE = TypeFormat.parseInt(p.getProperty("RbLockRange", "5000"));
			GB_LOCK_RANGE = TypeFormat.parseInt(p.getProperty("GbLockRange", "10000"));
			/** Clan Leader */
			CLAN_LEADER_COLOR_ENABLED = TypeFormat.parseBoolean(p.getProperty("ClanLeaderNameColorEnabled", "True"));
			CLAN_LEADER_COLORED = TypeFormat.parseInt(p.getProperty("ClanLeaderColored", "1"));
			CLAN_LEADER_COLOR = Integer.decode("0x" + p.getProperty("ClanLeaderColor", "00FFFF"));
			CLAN_LEADER_COLOR_CLAN_LEVEL = TypeFormat.parseInt(p.getProperty("ClanLeaderColorAtClanLevel", "1"));
			MAX_MULTISELL = TypeFormat.parseInt(p.getProperty("MaxMultisell","5000"));
			/** Chat Social Actions */
			SAY_SOCIAL = TypeFormat.parseBoolean(p.getProperty("ChatSocialEmotions", "True"));
			/** Other Modifications */
			SELL_BY_ITEM = TypeFormat.parseBoolean(p.getProperty("SellByItem", "False"));
			SELL_ITEM = TypeFormat.parseInt(p.getProperty("SellItem", "6392"));
			ALLOW_REGEN_SYSTEM = TypeFormat.parseBoolean(p.getProperty("AllowRegenSystemInTown", "False"));
			REGEN_SYSTEM_CP = TypeFormat.parseFloat(p.getProperty("RegenSystemInTownCP", "1.0"));
			REGEN_SYSTEM_HP = TypeFormat.parseFloat(p.getProperty("RegenSystemInTownHP", "1.0"));
			REGEN_SYSTEM_MP = TypeFormat.parseFloat(p.getProperty("RegenSystemInTownMP", "1.0"));
			/** Localization */
			MULTILANG_ENABLE = TypeFormat.parseBoolean(p.getProperty("AllowMultilanguage", "False"));
			String[] allowed = p.getProperty("AllowedMultiLanguages", "en").split(";");
			MULTILANG_ALLOWED = new ArrayList<String>(allowed.length);
			MULTILANG_ALLOWED.addAll(Arrays.asList(allowed));
			MULTILANG_DEFAULT = p.getProperty("DefaultLanguage", "en");
			if(!MULTILANG_ALLOWED.contains(MULTILANG_DEFAULT))
			{
				_log.warn("MultiLanguage: default language: " + MULTILANG_DEFAULT + " is not in allowed list !");
			}
			/** Banking */
			BANKING_SYSTEM_ENABLED = TypeFormat.parseBoolean(p.getProperty("BankingEnabled", "False"));
			BANKING_SYSTEM_GOLDBARS = TypeFormat.parseInt(p.getProperty("BankingGoldbarCount", "1"));
			BANKING_SYSTEM_ADENA = TypeFormat.parseInt(p.getProperty("BankingAdenaCount", "500000000"));
			/** Champion Mobs */
			CHAMPION_ENABLE = TypeFormat.parseBoolean(p.getProperty("ChampionEnable", "False"));
			CHAMPION_FREQUENCY = TypeFormat.parseInt(p.getProperty("ChampionFrequency", "0"));
			CHAMP_MIN_LVL = TypeFormat.parseInt(p.getProperty("ChampionMinLevel", "20"));
			CHAMP_MAX_LVL = TypeFormat.parseInt(p.getProperty("ChampionMaxLevel", "60"));
			CHAMPION_HP = TypeFormat.parseInt(p.getProperty("ChampionHp", "7"));
			CHAMPION_HP_REGEN = TypeFormat.parseFloat(p.getProperty("ChampionHpRegen", "1.0"));
			CHAMPION_REWARDS = TypeFormat.parseInt(p.getProperty("ChampionRewards", "8"));
			CHAMPION_ADENAS_REWARDS = TypeFormat.parseInt(p.getProperty("ChampionAdenasRewards", "1"));
			CHAMPION_ATK = TypeFormat.parseFloat(p.getProperty("ChampionAtk", "1.0"));
			CHAMPION_SPD_ATK = TypeFormat.parseFloat(p.getProperty("ChampionSpdAtk", "1.0"));
			CHAMPION_REWARD_ITEM = TypeFormat.parseInt(p.getProperty("ChampionRewardItem", "0"));
			CHAMPION_REWARD_ITEM_ID = TypeFormat.parseInt(p.getProperty("ChampionRewardItemID", "6393"));
			CHAMPION_REWARD_ITEM_QTY = TypeFormat.parseInt(p.getProperty("ChampionRewardItemQty", "1"));
			CHAMPION_TITLE = p.getProperty("ChampionTitle", "Champion");
			/** Offline Trade */
			OFFLINE_TRADE_ENABLE = TypeFormat.parseBoolean(p.getProperty("OfflineTradeEnable", "False"));
			OFFLINE_CRAFT_ENABLE = TypeFormat.parseBoolean(p.getProperty("OfflineCraftEnable", "False"));
			OFFLINE_SET_NAME_COLOR = TypeFormat.parseBoolean(p.getProperty("OfflineNameColorEnable", "False"));
			OFFLINE_NAME_COLOR = Integer.decode("0x" + p.getProperty("OfflineNameColor", "FF00FF"));
			OFFLINE_SLEEP_EFFECT = TypeFormat.parseBoolean(p.getProperty("OfflineSleepEffect", "False"));
			OFFLINE_RESTORE = TypeFormat.parseBoolean(p.getProperty("RestoreOffliners", "False"));
			OFFLINE_MAX_DAYS = TypeFormat.parseInt(p.getProperty("OfflineMaxDays", "10"));
			OFFLINE_DISCONNECT_FINISHED = TypeFormat.parseBoolean(p.getProperty("OfflineDisconnectFinished", "True"));
			/** Wedding System */
			ALLOW_WEDDING = TypeFormat.parseBoolean(p.getProperty("AllowWedding", "False"));
			WEDDING_ANNOUNCE = TypeFormat.parseBoolean(p.getProperty("WeddingAnnounce", "True"));
			WEDDING_PRICE = TypeFormat.parseInt(p.getProperty("WeddingPrice", "250000000"));
			WEDDING_PUNISH_INFIDELITY = TypeFormat.parseBoolean(p.getProperty("WeddingPunishInfidelity", "True"));
			WEDDING_TELEPORT = TypeFormat.parseBoolean(p.getProperty("WeddingTeleport", "True"));
			WEDDING_TELEPORT_PRICE = TypeFormat.parseInt(p.getProperty("WeddingTeleportPrice", "50000"));
			WEDDING_TELEPORT_DURATION = TypeFormat.parseInt(p.getProperty("WeddingTeleportDuration", "60"));
			WEDDING_SAMESEX = TypeFormat.parseBoolean(p.getProperty("WeddingAllowSameSex", "False"));
			WEDDING_FORMALWEAR = TypeFormat.parseBoolean(p.getProperty("WeddingFormalWear", "True"));
			WEDDING_DIVORCE_COSTS = TypeFormat.parseInt(p.getProperty("WeddingDivorceCosts", "20"));
			WEDDING_GIVE_CUPID_BOW = TypeFormat.parseBoolean(p.getProperty("WeddingGiveBow", "False"));
			WEDDING_SAMEIP = TypeFormat.parseBoolean(p.getProperty("WeddingAllowSameIP", "False"));
			/** Announce System */
			ANNOUNCE_CASTLE_LORDS = Boolean.parseBoolean(p.getProperty("AnnounceCastleLords", "false"));
			ANNOUNCE_ALL_KILL = Boolean.parseBoolean(p.getProperty("AnnounceAllKill", "False")); // Get the AnnounceAllKill, AnnouncePvpKill and AnnouncePkKill values
			if ( !ANNOUNCE_ALL_KILL )
			{
				ANNOUNCE_PVP_KILL = Boolean.parseBoolean(p.getProperty("AnnouncePvPKill", "False"));
				ANNOUNCE_PK_KILL = Boolean.parseBoolean(p.getProperty("AnnouncePkKill", "False"));
			}
			else
			{
				ANNOUNCE_PVP_KILL = false;
				ANNOUNCE_PK_KILL = false;
			}
			
			/** PvP & Pk Color System */
			PVP_COLOR_SYSTEM_ENABLED = Boolean.parseBoolean(p.getProperty("EnablePvPColorSystem", "false"));
			if(PVP_COLOR_SYSTEM_ENABLED)
			{
				PVP_COLOR = p.getProperty("PvpsColors", "");
				
				PVP_COLOR_LIST = new FastMap<Integer, Integer>();
				
				String[] splitted_pvps_colors = PVP_COLOR.split(";");
				
				for(String iii:splitted_pvps_colors)
				{
					String[] pvps_colors = iii.split(",");
					
					if(pvps_colors.length != 2)
					{
						System.out.println("Invalid properties.");
					}
					else
					{
						PVP_COLOR_LIST.put(Integer.parseInt(pvps_colors[0]), Integer.decode("0x" + pvps_colors[1]));
					}
				}
			}

			PK_COLOR_SYSTEM_ENABLED = Boolean.parseBoolean(p.getProperty("EnablePkColorSystem", "false"));
			if(PK_COLOR_SYSTEM_ENABLED)
			{
				PK_COLOR = p.getProperty("PksColors", "");
				PK_COLOR_LIST = new FastMap<Integer, Integer>();
				
				String[] splitted_pks_colors = PK_COLOR.split(";");
				
				for(String iii:splitted_pks_colors)
				{
					String[] pks_colors = iii.split(",");
					
					if(pks_colors.length != 2)
					{
						System.out.println("Invalid properties.");
					}
					else
					{
						PK_COLOR_LIST.put(Integer.parseInt(pks_colors[0]), Integer.decode("0x" + pks_colors[1]));
					}
				}
			}
			/** Vip System */
            ALLOW_VIP_NCOLOR = Boolean.parseBoolean(p.getProperty("AllowVipNameColor", "True"));
            VIP_NCOLOR = Integer.decode("0x" + p.getProperty("VipNameColor", "0088FF"));
            ALLOW_VIP_TCOLOR = Boolean.parseBoolean(p.getProperty("AllowVipTitleColor", "True"));
            VIP_TCOLOR = Integer.decode("0x" + p.getProperty("VipTitleColor", "0088FF"));
            ALLOW_VIP_XPSP = Boolean.parseBoolean(p.getProperty("AllowVipMulXpSp", "True"));
            VIP_XP = Integer.parseInt(p.getProperty("VipMulXp", "2"));
            VIP_SP = Integer.parseInt(p.getProperty("VipMulSp", "2"));
			VIP_ADENA_RATE = Float.parseFloat(p.getProperty("VipAdenaRate", "1.5"));
			VIP_DROP_RATE = Float.parseFloat(p.getProperty("VipDropRate", "1.5"));
			VIP_SPOIL_RATE = Float.parseFloat(p.getProperty("VipSpoilRate", "1.5"));
			/** Aio System */
			ENABLE_AIO_SYSTEM = Boolean.parseBoolean(p.getProperty("EnableAioSystem", "True"));
			ALLOW_AIO_NCOLOR = Boolean.parseBoolean(p.getProperty("AllowAioNameColor", "True"));
			AIO_NCOLOR = Integer.decode("0x" + p.getProperty("AioNameColor", "88AA88"));
			ALLOW_AIO_TCOLOR = Boolean.parseBoolean(p.getProperty("AllowAioTitleColor", "True"));
			AIO_TCOLOR = Integer.decode("0x" + p.getProperty("AioTitleColor", "88AA88"));
			ALLOW_AIO_ITEM = Boolean.parseBoolean(p.getProperty("AllowAIOItem", "False"));
			AIO_ITEMID = Integer.parseInt(p.getProperty("ItemIdAio", "9945"));
			AIO_ITEM_SKILL = Integer.parseInt(p.getProperty("ItemIdAioSkill", "9945"));
        	ALLOW_AIO_USE_GK = Boolean.parseBoolean(p.getProperty("AllowAioUseGk", "False"));
        	ALLOW_AIO_USE_CM = Boolean.parseBoolean(p.getProperty("AllowAioUseClassMaster", "False"));
        	ALLOW_AIO_BLOCK_EVENT = Boolean.parseBoolean(p.getProperty("AllowAioEvent", "False"));
        	ALLOW_AIO_LEAVE_TOWN = Boolean.parseBoolean(p.getProperty("RestrictAioInTown", "True"));
        	ALLOW_AIO_ENTER_IN_BOSS_ZONE = Boolean.parseBoolean(p.getProperty("RestrictAioEnterInBossZone", "True"));
        	//AIO_ITEM = Integer.parseInt(p.getProperty("AioItem", "4356"));
            //AIO_DIAS = Integer.parseInt(p.getProperty("AioDias", "30"));
           /**   
              ENABLE_CLAN_ITEM = Boolean.parseBoolean(p.getProperty("EnableClanItem", "True"));
              if(ENABLE_CLAN_ITEM)
              {
                      String ItemSkillsSplit[] = p.getProperty("ClanItemSkills", "").split(";");
                      CLAN_ITEM_SKILLS = new FastMap<Integer, Integer>(ItemSkillsSplit.length);
                      String arr[] = ItemSkillsSplit;
                      int len = arr.length;
                      for(int i = 0; i < len; i++)
                      {
                              String skill = arr[i];
                              String skillSplit[] = skill.split(",");
                              if(skillSplit.length != 2)
                              {
                                      System.out.println((new StringBuilder()).append("[Clan Item]: invalid config property in custom.properties -> ClanItemSkills \"").append(skill).append("\"").toString());
                                      continue;
                              }
                              try
                              {
                                      CLAN_ITEM_SKILLS.put(Integer.valueOf(Integer.parseInt(skillSplit[0])), Integer.valueOf(Integer.parseInt(skillSplit[1])));
                                      continue;
                              }
                              catch(NumberFormatException nfe)
                              {
                                      //
                              }
                              if(!skill.equals(""))
                                      System.out.println((new StringBuilder()).append("[Clan Item]: invalid config property in custom.properties -> ClanItemSkills \"").append(skillSplit[0]).append("\"").append(skillSplit[1]).toString());
                      }
              }
              CLAN_ITEM_ID = Integer.parseInt(p.getProperty("ClanItemID", "666"));
              ALLOW_ITEM_SET_LEVEL = TypeFormat.parseBoolean(p.getProperty("AllowClanItemLevel", "True"));
              CLAN_ITEM_LEVEL = Byte.parseByte(p.getProperty("ClanItemSetLevel", "8"));
              ALLOW_REPUTATION_ITEM = TypeFormat.parseBoolean(p.getProperty("AllowClanItemRep", "False"));
              REPUTATION_ITEM_QUANTITY = Integer.parseInt(p.getProperty("ReputationItemScore", "10000"));
              CLAN_TITLE = TypeFormat.parseBoolean(p.getProperty("ClanItemTitle", "False"));
              ADD_CLAN_TITLE = p.getProperty("ClanItemAddTitle", "L2JDream");
              ALLOW_CLAN_TCOLOR = TypeFormat.parseBoolean(p.getProperty("AllowClanItemTitle", "False"));
              CLAN_ITEM_TCOLOR = Integer.decode("0x" + p.getProperty("ClanTitleColor", "88AA88"));
            **/
			if(ENABLE_AIO_SYSTEM)
			{
				String[] AioSkillsSplit = p.getProperty("AioSkills", "").split(";");
				AIO_SKILLS = new FastMap<Integer, Integer>(AioSkillsSplit.length);
				for (String skill : AioSkillsSplit)
                              {
					String[] skillSplit = skill.split(",");
					if (skillSplit.length != 2)
					{
						System.out.println("[Aio System]:Incorrect config property in Custom.ini -> AioSkills \"" + skill + "\"");
                                      }
					else
					{
						try
						{
							AIO_SKILLS.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
                                              }
						catch (NumberFormatException nfe)
						{
							if (!skill.equals(""))
							{
								System.out.println("[Aio System]:Incorrect config property in Custom.ini -> AioSkills \"" + skillSplit[0] + "\"" + skillSplit[1]);
							}
						}
					}
                              }
			}
			p.clear();
		}
		catch(Exception e)
		{
			_log.warn("Failed to load " + ConfigFiles.CUSTOM_INI);
		}
	}	
	//============================================================
	// altsettings.ini
	//============================================================
	/** Drop Alternative */
	public static boolean AUTO_LOOT;
	public static boolean AUTO_LOOT_BOSS;
	public static boolean AUTO_LOOT_HERBS;
	/** Other Alternative */
	public static boolean AUTO_LEARN_SKILLS;
	public static boolean AUTO_LEARN_DIVINE_INSPIRATION;
	public static int ALT_PARTY_RANGE;
	public static int ALT_PARTY_RANGE2;
	public static double ALT_WEIGHT_LIMIT;
	public static boolean ALT_GAME_DELEVEL;
	public static boolean ALT_GAME_MAGICFAILURES;
	public static boolean ALT_GAME_CANCEL_BOW;
	public static boolean ALT_GAME_CANCEL_CAST;
	public static boolean ALT_GAME_SHIELD_BLOCKS;
	public static int ALT_PERFECT_SHLD_BLOCK;
	public static boolean ALT_MOB_AGRO_IN_PEACEZONE;
	public static boolean ALT_GAME_FREIGHTS;
	public static int ALT_GAME_FREIGHT_PRICE;
	public static float ALT_GAME_EXPONENT_XP;
	public static float ALT_GAME_EXPONENT_SP;
	public static boolean ALT_GAME_TIREDNESS;
	public static boolean ALT_GAME_FREE_TELEPORT;
	public static int STANDARD_RESPAWN_DELAY;
	/** Recomendations */
	public static boolean ALT_RECOMMEND;
	public static int ALT_RECOMMENDATIONS_NUMBER;
	/** Class Master */
	public static boolean ALLOW_CLASS_MASTERS;
	public static boolean ALLOW_CLASS_MASTERS_FIRST_CLASS;
	public static boolean ALLOW_CLASS_MASTERS_SECOND_CLASS;
	public static boolean ALLOW_CLASS_MASTERS_THIRD_CLASS;
	public static ClassMasterSettings CLASS_MASTER_SETTINGS;
	public static boolean CLASS_MASTER_STRIDER_UPDATE;
	public static boolean ALLOW_REMOTE_CLASS_MASTERS;
	/** Skills */
	public static boolean LIFE_CRYSTAL_NEEDED;
	public static boolean SP_BOOK_NEEDED;
	public static boolean ES_SP_BOOK_NEEDED;
	public static boolean DIVINE_SP_BOOK_NEEDED;
	public static boolean ALT_GAME_SKILL_LEARN;
	public static boolean ALT_GAME_SUBCLASS_WITHOUT_QUESTS;
	public static boolean ALT_SUB_WITHOUT_FATES;
	public static boolean ALT_GAME_VIEWNPC;
	public static boolean CHECK_SKILLS_DELEVEL;
	public static boolean INTERRUPT_TOGGLE_SKILL_EFFECT;
	/** Buff/Debuff */
	public static byte BUFFS_MAX_AMOUNT;
	public static byte DEBUFFS_MAX_AMOUNT;
	/** Clan */
	public static int ALT_CLAN_JOIN_DAYS;
	public static int ALT_CLAN_CREATE_DAYS;
	public static int ALT_CLAN_DISSOLVE_DAYS;
	public static int ALT_ALLY_JOIN_DAYS_WHEN_LEAVED;
	public static int ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED;
	public static int ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED;
	public static int ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED;
	public static int ALT_MAX_NUM_OF_CLANS_IN_ALLY;
	public static int ALT_CLAN_MEMBERS_FOR_WAR;
	public static boolean ALT_GAME_NEW_CHAR_ALWAYS_IS_NEWBIE;
	public static boolean ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH;
	public static boolean REMOVE_CASTLE_CIRCLETS;
	public static boolean ALLOW_WYVERN_DURING_SIEGE;
	/** Raid Ranking Points */
	public static int RAID_RANKING_1ST;
	public static int RAID_RANKING_2ND;
	public static int RAID_RANKING_3RD;
	public static int RAID_RANKING_4TH;
	public static int RAID_RANKING_5TH;
	public static int RAID_RANKING_6TH;
	public static int RAID_RANKING_7TH;
	public static int RAID_RANKING_8TH;
	public static int RAID_RANKING_9TH;
	public static int RAID_RANKING_10TH;
	public static int RAID_RANKING_UP_TO_50TH;
	public static int RAID_RANKING_UP_TO_100TH;
	/** Manor */
	public static boolean ALLOW_MANOR;
	public static int ALT_MANOR_REFRESH_TIME;
	public static int ALT_MANOR_REFRESH_MIN;
	public static int ALT_MANOR_APPROVE_TIME;
	public static int ALT_MANOR_APPROVE_MIN;
	public static int ALT_MANOR_MAINTENANCE_PERIOD;
	public static boolean ALT_MANOR_SAVE_ALL_ACTIONS;
	public static int ALT_MANOR_SAVE_PERIOD_RATE;
	/** Lottery */
	public static int ALT_LOTTERY_PRICE;
	public static int ALT_LOTTERY_TICKET_PRICE;
	public static float ALT_LOTTERY_5_NUMBER_RATE;
	public static float ALT_LOTTERY_4_NUMBER_RATE;
	public static float ALT_LOTTERY_3_NUMBER_RATE;
	public static int ALT_LOTTERY_2_AND_1_NUMBER_PRIZE;
	/** Dimensional Rift */
	public static int RIFT_MIN_PARTY_SIZE;
	public static int RIFT_SPAWN_DELAY;
	public static int RIFT_MAX_JUMPS;
	public static int RIFT_AUTO_JUMPS_TIME_MIN;
	public static int RIFT_AUTO_JUMPS_TIME_MAX;
	public static float RIFT_BOSS_ROOM_TIME_MUTIPLY;
	public static int RIFT_ENTER_COST_RECRUIT;
	public static int RIFT_ENTER_COST_SOLDIER;
	public static int RIFT_ENTER_COST_OFFICER;
	public static int RIFT_ENTER_COST_CAPTAIN;
	public static int RIFT_ENTER_COST_COMMANDER;
	public static int RIFT_ENTER_COST_HERO;
	/** Crafting */
	public static boolean IS_CRAFTING_ENABLED;
	public static int DWARF_RECIPE_LIMIT;
	public static int COMMON_RECIPE_LIMIT;
	public static boolean ALT_GAME_CREATION;
	public static double ALT_GAME_CREATION_SPEED;
	public static double ALT_GAME_CREATION_XP_RATE;
	public static double ALT_GAME_CREATION_SP_RATE;
	public static boolean ALT_BLACKSMITH_USE_RECIPES;
	/** Npc */
	public static boolean ALT_INVUL_NPC;
	public static boolean NPC_ATTACKABLE;
	public static List<Integer> INVUL_NPC_LIST;
	public static int NPC_RESPAWN_TIME;
	//============================================================
	public static void AltConfig()
	{
		try
		{
			L2Properties p = new L2Properties(ConfigFiles.ALT_INI);

			/** Drop Alternative */
			AUTO_LOOT = p.getProperty("AutoLoot").equalsIgnoreCase("True");
			AUTO_LOOT_BOSS = p.getProperty("AutoLootBoss").equalsIgnoreCase("True");
			AUTO_LOOT_HERBS = p.getProperty("AutoLootHerbs").equalsIgnoreCase("True");
			/** Other Alternative */
			AUTO_LEARN_SKILLS = TypeFormat.parseBoolean(p.getProperty("AutoLearnSkills", "False"));
			AUTO_LEARN_DIVINE_INSPIRATION = TypeFormat.parseBoolean(p.getProperty("AutoLearnDivineInspiration", "False"));
			ALT_PARTY_RANGE = TypeFormat.parseInt(p.getProperty("AltPartyRange", "1600"));
			ALT_PARTY_RANGE2 = TypeFormat.parseInt(p.getProperty("AltPartyRange2", "1400"));
			ALT_WEIGHT_LIMIT = TypeFormat.parseDouble(p.getProperty("AltWeightLimit", "1"));
			ALT_GAME_DELEVEL = TypeFormat.parseBoolean(p.getProperty("Delevel", "True"));
			ALT_GAME_MAGICFAILURES = TypeFormat.parseBoolean(p.getProperty("MagicFailures", "False"));
			ALT_GAME_CANCEL_BOW = p.getProperty("AltGameCancelByHit", "Cast").equalsIgnoreCase("bow") || p.getProperty("AltGameCancelByHit", "Cast").equalsIgnoreCase("all");
			ALT_GAME_CANCEL_CAST = p.getProperty("AltGameCancelByHit", "Cast").equalsIgnoreCase("cast") || p.getProperty("AltGameCancelByHit", "Cast").equalsIgnoreCase("all");
			ALT_GAME_SHIELD_BLOCKS = TypeFormat.parseBoolean(p.getProperty("AltShieldBlocks", "False"));
			ALT_PERFECT_SHLD_BLOCK = TypeFormat.parseInt(p.getProperty("AltPerfectShieldBlockRate", "10"));
			ALT_MOB_AGRO_IN_PEACEZONE = TypeFormat.parseBoolean(p.getProperty("AltMobAgroInPeaceZone", "True"));
			ALT_GAME_FREIGHTS = TypeFormat.parseBoolean(p.getProperty("AltGameFreights", "False"));
			ALT_GAME_FREIGHT_PRICE = TypeFormat.parseInt(p.getProperty("AltGameFreightPrice", "1000"));
			ALT_GAME_EXPONENT_XP = TypeFormat.parseFloat(p.getProperty("AltGameExponentXp", "0."));
			ALT_GAME_EXPONENT_SP = TypeFormat.parseFloat(p.getProperty("AltGameExponentSp", "0."));
			ALT_GAME_TIREDNESS = TypeFormat.parseBoolean(p.getProperty("AltGameTiredness", "False"));
			ALT_GAME_FREE_TELEPORT = TypeFormat.parseBoolean(p.getProperty("AltFreeTeleporting", "False"));
			STANDARD_RESPAWN_DELAY = TypeFormat.parseInt(p.getProperty("StandardRespawnDelay", "180"));
			/** Recomendations */
			ALT_RECOMMEND = TypeFormat.parseBoolean(p.getProperty("AltRecommend", "False"));
			ALT_RECOMMENDATIONS_NUMBER = TypeFormat.parseInt(p.getProperty("AltMaxRecommendationNumber", "255"));
			/** Class Master */
			ALLOW_CLASS_MASTERS = TypeFormat.parseBoolean(p.getProperty("AllowClassMasters", "False"));
			ALLOW_CLASS_MASTERS_FIRST_CLASS = TypeFormat.parseBoolean(p.getProperty("AllowClassMastersFirstClass", "True"));
			ALLOW_CLASS_MASTERS_SECOND_CLASS = TypeFormat.parseBoolean(p.getProperty("AllowClassMastersSecondClass", "True"));
			ALLOW_CLASS_MASTERS_THIRD_CLASS = TypeFormat.parseBoolean(p.getProperty("AllowClassMastersThirdClass", "True"));
			CLASS_MASTER_SETTINGS = new ClassMasterSettings(p.getProperty("ConfigClassMaster"));
			CLASS_MASTER_STRIDER_UPDATE = TypeFormat.parseBoolean(p.getProperty("AllowClassMastersStriderUpdate", "False"));
			ALLOW_REMOTE_CLASS_MASTERS = TypeFormat.parseBoolean(p.getProperty("AllowRemoteClassMasters", "False"));
			/** Skills */
			LIFE_CRYSTAL_NEEDED = TypeFormat.parseBoolean(p.getProperty("LifeCrystalNeeded", "True"));
			SP_BOOK_NEEDED = TypeFormat.parseBoolean(p.getProperty("SpBookNeeded", "True"));
			ES_SP_BOOK_NEEDED = TypeFormat.parseBoolean(p.getProperty("EnchantSkillSpBookNeeded", "True"));
			DIVINE_SP_BOOK_NEEDED = TypeFormat.parseBoolean(p.getProperty("DivineInspirationSpBookNeeded", "True"));
			ALT_GAME_SKILL_LEARN = TypeFormat.parseBoolean(p.getProperty("AltGameSkillLearn", "False"));
			ALT_GAME_SUBCLASS_WITHOUT_QUESTS = TypeFormat.parseBoolean(p.getProperty("AltSubClassWithoutQuests", "False"));
			ALT_SUB_WITHOUT_FATES = Boolean.parseBoolean(p.getProperty("AltSubWithoutFates", "False"));
			ALT_GAME_VIEWNPC = TypeFormat.parseBoolean(p.getProperty("AltGameViewNpc", "False"));
			CHECK_SKILLS_DELEVEL = TypeFormat.parseBoolean(p.getProperty("CheckSkillsOnDelevel", "True"));
			INTERRUPT_TOGGLE_SKILL_EFFECT = TypeFormat.parseBoolean(p.getProperty("InterruptToggleSkillEffect", "True"));
			/** Buff/Debuff */
			BUFFS_MAX_AMOUNT = TypeFormat.parseByte(p.getProperty("MaxBuffAmount", "24"));
			DEBUFFS_MAX_AMOUNT = TypeFormat.parseByte(p.getProperty("MaxDebuffAmount", "6"));
			/** Clan */
			ALT_CLAN_JOIN_DAYS = TypeFormat.parseInt(p.getProperty("DaysBeforeJoinAClan", "5"));
			ALT_CLAN_CREATE_DAYS = TypeFormat.parseInt(p.getProperty("DaysBeforeCreateAClan", "10"));
			ALT_CLAN_DISSOLVE_DAYS = TypeFormat.parseInt(p.getProperty("DaysToPassToDissolveAClan", "7"));
			ALT_ALLY_JOIN_DAYS_WHEN_LEAVED = TypeFormat.parseInt(p.getProperty("DaysBeforeJoinAllyWhenLeaved", "1"));
			ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED = TypeFormat.parseInt(p.getProperty("DaysBeforeJoinAllyWhenDismissed", "1"));
			ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED = TypeFormat.parseInt(p.getProperty("DaysBeforeAcceptNewClanWhenDismissed", "1"));
			ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED = TypeFormat.parseInt(p.getProperty("DaysBeforeCreateNewAllyWhenDissolved", "10"));
			ALT_MAX_NUM_OF_CLANS_IN_ALLY = TypeFormat.parseInt(p.getProperty("AltMaxNumOfClansInAlly", "3"));
			ALT_CLAN_MEMBERS_FOR_WAR = TypeFormat.parseInt(p.getProperty("AltClanMembersForWar", "15"));
			ALT_GAME_NEW_CHAR_ALWAYS_IS_NEWBIE = TypeFormat.parseBoolean(p.getProperty("AltNewCharAlwaysIsNewbie", "False"));
			ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH = TypeFormat.parseBoolean(p.getProperty("AltMembersCanWithdrawFromClanWH", "False"));
			REMOVE_CASTLE_CIRCLETS = TypeFormat.parseBoolean(p.getProperty("RemoveCastleCirclets", "True"));
			ALLOW_WYVERN_DURING_SIEGE = Boolean.parseBoolean(p.getProperty("AllowRideWyvernDuringSiege", "True"));
			/** Raid Ranking Points */
			RAID_RANKING_1ST = TypeFormat.parseInt(p.getProperty("1stRaidRankingPoints", "1250"));
			RAID_RANKING_2ND = TypeFormat.parseInt(p.getProperty("2ndRaidRankingPoints", "900"));
			RAID_RANKING_3RD = TypeFormat.parseInt(p.getProperty("3rdRaidRankingPoints", "700"));
			RAID_RANKING_4TH = TypeFormat.parseInt(p.getProperty("4thRaidRankingPoints", "600"));
			RAID_RANKING_5TH = TypeFormat.parseInt(p.getProperty("5thRaidRankingPoints", "450"));
			RAID_RANKING_6TH = TypeFormat.parseInt(p.getProperty("6thRaidRankingPoints", "350"));
			RAID_RANKING_7TH = TypeFormat.parseInt(p.getProperty("7thRaidRankingPoints", "300"));
			RAID_RANKING_8TH = TypeFormat.parseInt(p.getProperty("8thRaidRankingPoints", "200"));
			RAID_RANKING_9TH = TypeFormat.parseInt(p.getProperty("9thRaidRankingPoints", "150"));
			RAID_RANKING_10TH = TypeFormat.parseInt(p.getProperty("10thRaidRankingPoints", "100"));
			RAID_RANKING_UP_TO_50TH = TypeFormat.parseInt(p.getProperty("UpTo50thRaidRankingPoints", "25"));
			RAID_RANKING_UP_TO_100TH = TypeFormat.parseInt(p.getProperty("UpTo100thRaidRankingPoints", "12"));
			/** Manor */
			ALLOW_MANOR = TypeFormat.parseBoolean(p.getProperty("AllowManor", "True"));
			ALT_MANOR_REFRESH_TIME = TypeFormat.parseInt(p.getProperty("AltManorRefreshTime", "20"));
			ALT_MANOR_REFRESH_MIN = TypeFormat.parseInt(p.getProperty("AltManorRefreshMin", "00"));
			ALT_MANOR_APPROVE_TIME = TypeFormat.parseInt(p.getProperty("AltManorApproveTime", "6"));
			ALT_MANOR_APPROVE_MIN = TypeFormat.parseInt(p.getProperty("AltManorApproveMin", "00"));
			ALT_MANOR_MAINTENANCE_PERIOD = TypeFormat.parseInt(p.getProperty("AltManorMaintenancePeriod", "360000"));
			ALT_MANOR_SAVE_ALL_ACTIONS = TypeFormat.parseBoolean(p.getProperty("AltManorSaveAllActions", "False"));
			ALT_MANOR_SAVE_PERIOD_RATE = TypeFormat.parseInt(p.getProperty("AltManorSavePeriodRate", "2"));
			/** Lottery */
			ALT_LOTTERY_PRICE = TypeFormat.parseInt(p.getProperty("AltLotteryPrice", "50000"));
			ALT_LOTTERY_TICKET_PRICE = TypeFormat.parseInt(p.getProperty("AltLotteryTicketPrice", "2000"));
			ALT_LOTTERY_5_NUMBER_RATE = TypeFormat.parseFloat(p.getProperty("AltLottery5NumberRate", "0.6"));
			ALT_LOTTERY_4_NUMBER_RATE = TypeFormat.parseFloat(p.getProperty("AltLottery4NumberRate", "0.2"));
			ALT_LOTTERY_3_NUMBER_RATE = TypeFormat.parseFloat(p.getProperty("AltLottery3NumberRate", "0.2"));
			ALT_LOTTERY_2_AND_1_NUMBER_PRIZE = TypeFormat.parseInt(p.getProperty("AltLottery2and1NumberPrize", "200"));
			/** Dimensional Rift */
			RIFT_MIN_PARTY_SIZE = TypeFormat.parseInt(p.getProperty("RiftMinPartySize", "5"));
			RIFT_MAX_JUMPS = TypeFormat.parseInt(p.getProperty("MaxRiftJumps", "4"));
			RIFT_SPAWN_DELAY = TypeFormat.parseInt(p.getProperty("RiftSpawnDelay", "10000"));
			RIFT_AUTO_JUMPS_TIME_MIN = TypeFormat.parseInt(p.getProperty("AutoJumpsDelayMin", "480"));
			RIFT_AUTO_JUMPS_TIME_MAX = TypeFormat.parseInt(p.getProperty("AutoJumpsDelayMax", "600"));
			RIFT_BOSS_ROOM_TIME_MUTIPLY = TypeFormat.parseFloat(p.getProperty("BossRoomTimeMultiply", "1.5"));
			RIFT_ENTER_COST_RECRUIT = TypeFormat.parseInt(p.getProperty("RecruitCost", "18"));
			RIFT_ENTER_COST_SOLDIER = TypeFormat.parseInt(p.getProperty("SoldierCost", "21"));
			RIFT_ENTER_COST_OFFICER = TypeFormat.parseInt(p.getProperty("OfficerCost", "24"));
			RIFT_ENTER_COST_CAPTAIN = TypeFormat.parseInt(p.getProperty("CaptainCost", "27"));
			RIFT_ENTER_COST_COMMANDER = TypeFormat.parseInt(p.getProperty("CommanderCost", "30"));
			RIFT_ENTER_COST_HERO = TypeFormat.parseInt(p.getProperty("HeroCost", "33"));
			/** Crafting */
			IS_CRAFTING_ENABLED = TypeFormat.parseBoolean(p.getProperty("CraftingEnabled", "True"));
			DWARF_RECIPE_LIMIT = TypeFormat.parseInt(p.getProperty("DwarfRecipeLimit", "50"));
			COMMON_RECIPE_LIMIT = TypeFormat.parseInt(p.getProperty("CommonRecipeLimit", "50"));
			ALT_GAME_CREATION = TypeFormat.parseBoolean(p.getProperty("AltGameCreation", "False"));
			ALT_GAME_CREATION_SPEED = TypeFormat.parseDouble(p.getProperty("AltGameCreationSpeed", "1"));
			ALT_GAME_CREATION_XP_RATE = TypeFormat.parseDouble(p.getProperty("AltGameCreationRateXp", "1"));
			ALT_GAME_CREATION_SP_RATE = TypeFormat.parseDouble(p.getProperty("AltGameCreationRateSp", "1"));
			ALT_BLACKSMITH_USE_RECIPES = TypeFormat.parseBoolean(p.getProperty("AltBlacksmithUseRecipes", "True"));
			/** Npcs */
			ALT_INVUL_NPC = Boolean.parseBoolean(p.getProperty("AltInvulNPC", "False"));
			NPC_ATTACKABLE = Boolean.parseBoolean(p.getProperty("NpcAttackable", "False"));
			INVUL_NPC_LIST = new FastList<Integer>();
			NPC_RESPAWN_TIME = Integer.parseInt(p.getProperty("NpcRespawnTime", "60"));
			String t = p.getProperty("InvulNpcList", "30001-32132,35092-35103,35142-35146,35176-35187,35218-35232,35261-35278,35308-35319,35352-35367,35382-35407,35417-35427,35433-35469,35497-35513,35544-35587,35600-35617,35623-35628,35638-35640,35644,35645,50007,70010,99999");
			String as[];
			int k = (as = t.split(",")).length;
			for (int j = 0; j < k; j++)
			{
				String t2 = as[j];
				if (t2.contains("-"))
				{
					int a1 = Integer.parseInt(t2.split("-")[0]);
					int a2 = Integer.parseInt(t2.split("-")[1]);
					for (int i = a1; i <= a2; i++)
						INVUL_NPC_LIST.add(Integer.valueOf(i));
				} else
					INVUL_NPC_LIST.add(Integer.valueOf(Integer.parseInt(t2)));
			}
			
			p.clear();
		}
		catch(Exception e)
		{
			_log.warn("Failed to load " + ConfigFiles.ALT_INI);
		}
	}
	//============================================================
	// clanhall.ini
	//============================================================
	/** Clan Halls */
	public static long CH_TELE_FEE_RATIO;
	public static int CH_TELE1_FEE;
	public static int CH_TELE2_FEE;
	public static long CH_ITEM_FEE_RATIO;
	public static int CH_ITEM1_FEE;
	public static int CH_ITEM2_FEE;
	public static int CH_ITEM3_FEE;
	public static long CH_MPREG_FEE_RATIO;
	public static int CH_MPREG1_FEE;
	public static int CH_MPREG2_FEE;
	public static int CH_MPREG3_FEE;
	public static int CH_MPREG4_FEE;
	public static int CH_MPREG5_FEE;
	public static long CH_HPREG_FEE_RATIO;
	public static int CH_HPREG1_FEE;
	public static int CH_HPREG2_FEE;
	public static int CH_HPREG3_FEE;
	public static int CH_HPREG4_FEE;
	public static int CH_HPREG5_FEE;
	public static int CH_HPREG6_FEE;
	public static int CH_HPREG7_FEE;
	public static int CH_HPREG8_FEE;
	public static int CH_HPREG9_FEE;
	public static int CH_HPREG10_FEE;
	public static int CH_HPREG11_FEE;
	public static int CH_HPREG12_FEE;
	public static int CH_HPREG13_FEE;
	public static long CH_EXPREG_FEE_RATIO;
	public static int CH_EXPREG1_FEE;
	public static int CH_EXPREG2_FEE;
	public static int CH_EXPREG3_FEE;
	public static int CH_EXPREG4_FEE;
	public static int CH_EXPREG5_FEE;
	public static int CH_EXPREG6_FEE;
	public static int CH_EXPREG7_FEE;
	public static long CH_SUPPORT_FEE_RATIO;
	public static int CH_SUPPORT1_FEE;
	public static int CH_SUPPORT2_FEE;
	public static int CH_SUPPORT3_FEE;
	public static int CH_SUPPORT4_FEE;
	public static int CH_SUPPORT5_FEE;
	public static int CH_SUPPORT6_FEE;
	public static int CH_SUPPORT7_FEE;
	public static int CH_SUPPORT8_FEE;
	public static long CH_CURTAIN_FEE_RATIO;
	public static int CH_CURTAIN1_FEE;
	public static int CH_CURTAIN2_FEE;
	public static long CH_FRONT_FEE_RATIO;
	public static int CH_FRONT1_FEE;
	public static int CH_FRONT2_FEE;
	/** Devastated Castle */
	public static int DEVASTATED_DAY;
	public static int DEVASTATED_HOUR;
	public static int DEVASTATED_MINUTES;
	/** Partisan Hideout */
	public static int PARTISAN_DAY;
	public static int PARTISAN_HOUR;
	public static int PARTISAN_MINUTES;
	//============================================================
	public static void CHConfig()
	{
		try
		{
			L2Properties p = new L2Properties(ConfigFiles.CLANHALL_INI);

			/** Clan Halls */
			CH_TELE_FEE_RATIO = Long.valueOf(p.getProperty("ClanHallTeleportFunctionFeeRation", "86400000"));
			CH_TELE1_FEE = TypeFormat.parseInt(p.getProperty("ClanHallTeleportFunctionFeeLvl1", "86400000"));
			CH_TELE2_FEE = TypeFormat.parseInt(p.getProperty("ClanHallTeleportFunctionFeeLvl2", "86400000"));
			CH_SUPPORT_FEE_RATIO = Long.valueOf(p.getProperty("ClanHallSupportFunctionFeeRation", "86400000"));
			CH_SUPPORT1_FEE = TypeFormat.parseInt(p.getProperty("ClanHallSupportFeeLvl1", "86400000"));
			CH_SUPPORT2_FEE = TypeFormat.parseInt(p.getProperty("ClanHallSupportFeeLvl2", "86400000"));
			CH_SUPPORT3_FEE = TypeFormat.parseInt(p.getProperty("ClanHallSupportFeeLvl3", "86400000"));
			CH_SUPPORT4_FEE = TypeFormat.parseInt(p.getProperty("ClanHallSupportFeeLvl4", "86400000"));
			CH_SUPPORT5_FEE = TypeFormat.parseInt(p.getProperty("ClanHallSupportFeeLvl5", "86400000"));
			CH_SUPPORT6_FEE = TypeFormat.parseInt(p.getProperty("ClanHallSupportFeeLvl6", "86400000"));
			CH_SUPPORT7_FEE = TypeFormat.parseInt(p.getProperty("ClanHallSupportFeeLvl7", "86400000"));
			CH_SUPPORT8_FEE = TypeFormat.parseInt(p.getProperty("ClanHallSupportFeeLvl8", "86400000"));
			CH_MPREG_FEE_RATIO = Long.valueOf(p.getProperty("ClanHallMpRegenerationFunctionFeeRation", "86400000"));
			CH_MPREG1_FEE = TypeFormat.parseInt(p.getProperty("ClanHallMpRegenerationFeeLvl1", "86400000"));
			CH_MPREG2_FEE = TypeFormat.parseInt(p.getProperty("ClanHallMpRegenerationFeeLvl2", "86400000"));
			CH_MPREG3_FEE = TypeFormat.parseInt(p.getProperty("ClanHallMpRegenerationFeeLvl3", "86400000"));
			CH_MPREG4_FEE = TypeFormat.parseInt(p.getProperty("ClanHallMpRegenerationFeeLvl4", "86400000"));
			CH_MPREG5_FEE = TypeFormat.parseInt(p.getProperty("ClanHallMpRegenerationFeeLvl5", "86400000"));
			CH_HPREG_FEE_RATIO = Long.valueOf(p.getProperty("ClanHallHpRegenerationFunctionFeeRation", "86400000"));
			CH_HPREG1_FEE = TypeFormat.parseInt(p.getProperty("ClanHallHpRegenerationFeeLvl1", "86400000"));
			CH_HPREG2_FEE = TypeFormat.parseInt(p.getProperty("ClanHallHpRegenerationFeeLvl2", "86400000"));
			CH_HPREG3_FEE = TypeFormat.parseInt(p.getProperty("ClanHallHpRegenerationFeeLvl3", "86400000"));
			CH_HPREG4_FEE = TypeFormat.parseInt(p.getProperty("ClanHallHpRegenerationFeeLvl4", "86400000"));
			CH_HPREG5_FEE = TypeFormat.parseInt(p.getProperty("ClanHallHpRegenerationFeeLvl5", "86400000"));
			CH_HPREG6_FEE = TypeFormat.parseInt(p.getProperty("ClanHallHpRegenerationFeeLvl6", "86400000"));
			CH_HPREG7_FEE = TypeFormat.parseInt(p.getProperty("ClanHallHpRegenerationFeeLvl7", "86400000"));
			CH_HPREG8_FEE = TypeFormat.parseInt(p.getProperty("ClanHallHpRegenerationFeeLvl8", "86400000"));
			CH_HPREG9_FEE = TypeFormat.parseInt(p.getProperty("ClanHallHpRegenerationFeeLvl9", "86400000"));
			CH_HPREG10_FEE = TypeFormat.parseInt(p.getProperty("ClanHallHpRegenerationFeeLvl10", "86400000"));
			CH_HPREG11_FEE = TypeFormat.parseInt(p.getProperty("ClanHallHpRegenerationFeeLvl11", "86400000"));
			CH_HPREG12_FEE = TypeFormat.parseInt(p.getProperty("ClanHallHpRegenerationFeeLvl12", "86400000"));
			CH_HPREG13_FEE = TypeFormat.parseInt(p.getProperty("ClanHallHpRegenerationFeeLvl13", "86400000"));
			CH_EXPREG_FEE_RATIO = Long.valueOf(p.getProperty("ClanHallExpRegenerationFunctionFeeRation", "86400000"));
			CH_EXPREG1_FEE = TypeFormat.parseInt(p.getProperty("ClanHallExpRegenerationFeeLvl1", "86400000"));
			CH_EXPREG2_FEE = TypeFormat.parseInt(p.getProperty("ClanHallExpRegenerationFeeLvl2", "86400000"));
			CH_EXPREG3_FEE = TypeFormat.parseInt(p.getProperty("ClanHallExpRegenerationFeeLvl3", "86400000"));
			CH_EXPREG4_FEE = TypeFormat.parseInt(p.getProperty("ClanHallExpRegenerationFeeLvl4", "86400000"));
			CH_EXPREG5_FEE = TypeFormat.parseInt(p.getProperty("ClanHallExpRegenerationFeeLvl5", "86400000"));
			CH_EXPREG6_FEE = TypeFormat.parseInt(p.getProperty("ClanHallExpRegenerationFeeLvl6", "86400000"));
			CH_EXPREG7_FEE = TypeFormat.parseInt(p.getProperty("ClanHallExpRegenerationFeeLvl7", "86400000"));
			CH_ITEM_FEE_RATIO = Long.valueOf(p.getProperty("ClanHallItemCreationFunctionFeeRation", "86400000"));
			CH_ITEM1_FEE = TypeFormat.parseInt(p.getProperty("ClanHallItemCreationFunctionFeeLvl1", "86400000"));
			CH_ITEM2_FEE = TypeFormat.parseInt(p.getProperty("ClanHallItemCreationFunctionFeeLvl2", "86400000"));
			CH_ITEM3_FEE = TypeFormat.parseInt(p.getProperty("ClanHallItemCreationFunctionFeeLvl3", "86400000"));
			CH_CURTAIN_FEE_RATIO = Long.valueOf(p.getProperty("ClanHallCurtainFunctionFeeRation", "86400000"));
			CH_CURTAIN1_FEE = TypeFormat.parseInt(p.getProperty("ClanHallCurtainFunctionFeeLvl1", "86400000"));
			CH_CURTAIN2_FEE = TypeFormat.parseInt(p.getProperty("ClanHallCurtainFunctionFeeLvl2", "86400000"));
			CH_FRONT_FEE_RATIO = Long.valueOf(p.getProperty("ClanHallFrontPlatformFunctionFeeRation", "86400000"));
			CH_FRONT1_FEE = TypeFormat.parseInt(p.getProperty("ClanHallFrontPlatformFunctionFeeLvl1", "86400000"));
			CH_FRONT2_FEE = TypeFormat.parseInt(p.getProperty("ClanHallFrontPlatformFunctionFeeLvl2", "86400000"));
			/** Devastated Castle */
			DEVASTATED_DAY = TypeFormat.parseInt(p.getProperty("DevastatedDay", "1"));
			DEVASTATED_HOUR = TypeFormat.parseInt(p.getProperty("DevastatedHour", "18"));
			DEVASTATED_MINUTES = TypeFormat.parseInt(p.getProperty("DevastatedMinutes", "0"));
			/** Partisan Hideout */
			PARTISAN_DAY = TypeFormat.parseInt(p.getProperty("PartisanDay", "5"));
			PARTISAN_HOUR = TypeFormat.parseInt(p.getProperty("PartisanHour", "21"));
			PARTISAN_MINUTES = TypeFormat.parseInt(p.getProperty("PartisanMinutes", "0"));

			p.clear();
		}
		catch(Exception e)
		{
			_log.warn("Failed to load " + ConfigFiles.CLANHALL_INI);
		}
	}
	//============================================================
	// geodata.ini
	//============================================================
	/** Geodata */
	public static int GEODATA;
	public static boolean ALLOW_PLAYERS_PATHNODE;
	public static boolean FORCE_GEODATA;
	public static boolean GEODATA_CELLFINDING;
	public static enum CorrectSpawnsZ
	{
		TOWN, MONSTER, ALL, NONE
	}
	public static CorrectSpawnsZ GEO_CORRECT_Z;
	public static int COORD_SYNCHRONIZE;
	public static int WORLD_SIZE_MIN_X;
	public static int WORLD_SIZE_MAX_X;
	public static int WORLD_SIZE_MIN_Y;
	public static int WORLD_SIZE_MAX_Y;
	public static int WORLD_SIZE_MIN_Z;
	public static int WORLD_SIZE_MAX_Z;
	public static int DIFFERENT_Z_CHANGE_OBJECT;
	public static int DIFFERENT_Z_NEW_MOVE;
	public static boolean ENABLE_FALLING_DAMAGE;
	//============================================================
	public static void GeodataConfig()
	{
		try
		{
			L2Properties p = new L2Properties(ConfigFiles.GEODATA_INI);

			/** Geodata */
			GEODATA = TypeFormat.parseInt(p.getProperty("GeoData", "0"));
			ALLOW_PLAYERS_PATHNODE	= TypeFormat.parseBoolean(p.getProperty("AllowPlayersPathnode", "False"));
			FORCE_GEODATA = TypeFormat.parseBoolean(p.getProperty("ForceGeoData", "True"));
			GEODATA_CELLFINDING = TypeFormat.parseBoolean(p.getProperty("CellPathFinding", "False"));
			String correctZ = p.getProperty("GeoCorrectZ", "ALL");
			GEO_CORRECT_Z = CorrectSpawnsZ.valueOf(correctZ.toUpperCase());
			COORD_SYNCHRONIZE = TypeFormat.parseInt(p.getProperty("CoordSynchronize", "-1"));
			WORLD_SIZE_MIN_X = TypeFormat.parseInt(p.getProperty("WorldSizeMinX", "-131072"));
			WORLD_SIZE_MAX_X = TypeFormat.parseInt(p.getProperty("WorldSizeMaxX", "228608"));
			WORLD_SIZE_MIN_Y = TypeFormat.parseInt(p.getProperty("WorldSizeMinY", "-262144"));
			WORLD_SIZE_MAX_Y = TypeFormat.parseInt(p.getProperty("WorldSizeMaxY", "262144"));
			WORLD_SIZE_MIN_Z = TypeFormat.parseInt(p.getProperty("WorldSizeMinZ", "-15000"));
			WORLD_SIZE_MAX_Z = TypeFormat.parseInt(p.getProperty("WorldSizeMaxZ", "15000"));
			DIFFERENT_Z_CHANGE_OBJECT = TypeFormat.parseInt(p.getProperty("DifferentZchangeObject", "650"));
			DIFFERENT_Z_NEW_MOVE = TypeFormat.parseInt(p.getProperty("DifferentZnewMove", "1000"));
			String str = p.getProperty("EnableFallingDamage", "auto");
			ENABLE_FALLING_DAMAGE = "auto".equalsIgnoreCase(str) ? GEODATA > 0 : TypeFormat.parseBoolean(str);
			String stg = p.getProperty("EnableWater", "auto");
			ALLOW_WATER = "auto".equalsIgnoreCase(stg) ? GEODATA > 0 : TypeFormat.parseBoolean(stg);

			p.clear();
		}
		catch(Exception e)
		{
			_log.warn("Failed to load " + ConfigFiles.GEODATA_INI);
		}
	}
	//============================================================
	// grandbosses.ini
	//============================================================
	/** Antharas */
	public static int ANTHARAS_DESPAWN_TIME;
	public static int ANTHARAS_WAIT_TIME;
	public static int ANTHARAS_RESP_FIRST;
	public static int ANTHARAS_RESP_SECOND;
	public static boolean ANTHARAS_OLD_TYPE;
	/** Baium */
	public static int BAIUM_SLEEP;
	public static int BAIUM_RESP_FIRST;
	public static int BAIUM_RESP_SECOND;
	/** Core */
	public static int CORE_RESP_MINION;
	public static int CORE_RESP_FIRST;
	public static int CORE_RESP_SECOND;
	/** Frintezza */
	public static int FRINTEZZA_RESP_FIRST;
	public static int FRINTEZZA_RESP_SECOND;
	public static boolean FRINTEZZA_DISABLE_PARTY_CHECK;
	public static int FRINTEZZA_MIN_PARTIES;
	public static int FRINTEZZA_MAX_PARTIES;
	/** Orfen */
	public static int ORFEN_RESP_FIRST;
	public static int ORFEN_RESP_SECOND;
	/** Queen Ant */
	public static int QA_RESP_NURSE;
	public static int QA_RESP_ROYAL;
	public static int QA_RESP_FIRST;
	public static int QA_RESP_SECOND;
	/** Sailren */
	public static int SAILREN_RESP_FIRST;
	public static int SAILREN_RESP_SECOND;
	/** Valakas */
	public static int VALAKAS_WAIT_TIME;
	public static int VALAKAS_RESP_FIRST;
	public static int VALAKAS_RESP_SECOND;
	public static int VALAKAS_DESPAWN_TIME;
	/** High Priestess van Halter */
	public static int HPH_FIXINTERVALOFHALTER;
	public static int HPH_RANDOMINTERVALOFHALTER;
	public static int HPH_APPTIMEOFHALTER;
	public static int HPH_ACTIVITYTIMEOFHALTER;
	public static int HPH_FIGHTTIMEOFHALTER;
	public static int HPH_CALLROYALGUARDHELPERCOUNT;
	public static int HPH_CALLROYALGUARDHELPERINTERVAL;
	public static int HPH_INTERVALOFDOOROFALTER;
	public static int HPH_TIMEOFLOCKUPDOOROFALTAR;
	/** Zaken */
	public static int ZAKEN_RESP_FIRST;
	public static int ZAKEN_RESP_SECOND;
	//============================================================
	public static void GrandBossConfig()
	{
		try
		{
			L2Properties p = new L2Properties(ConfigFiles.GRANDBOSSES_INI);

			/** Antharas */
			ANTHARAS_DESPAWN_TIME = Integer.parseInt(p.getProperty("AntharasDespawnTime", "240"));
			ANTHARAS_WAIT_TIME = TypeFormat.parseInt(p.getProperty("AntharasWaitTime", "30"));
			ANTHARAS_RESP_FIRST = TypeFormat.parseInt(p.getProperty("AntharasRespFirst", "180"));
			ANTHARAS_RESP_SECOND = TypeFormat.parseInt(p.getProperty("AntharasRespSecond", "24"));
			ANTHARAS_OLD_TYPE = TypeFormat.parseBoolean(p.getProperty("AntharasOldType", "False"));
			/** Baium */
			BAIUM_SLEEP = TypeFormat.parseInt(p.getProperty("BaiumSleep", "1800"));
			BAIUM_RESP_FIRST = TypeFormat.parseInt(p.getProperty("BaiumRespFirst", "112"));
			BAIUM_RESP_SECOND = TypeFormat.parseInt(p.getProperty("BaiumRespSecond", "16"));
			/** Core */
			CORE_RESP_MINION = TypeFormat.parseInt(p.getProperty("CoreRespMinion", "60"));
			CORE_RESP_FIRST = TypeFormat.parseInt(p.getProperty("CoreRespFirst", "33"));
			CORE_RESP_SECOND = TypeFormat.parseInt(p.getProperty("CoreRespSecond", "8"));
			/** Frintezza */
			FRINTEZZA_RESP_FIRST = TypeFormat.parseInt(p.getProperty("FrintezzaRespFirst", "48"));
			FRINTEZZA_RESP_SECOND = TypeFormat.parseInt(p.getProperty("FrintezzaRespSecond", "8"));
			FRINTEZZA_DISABLE_PARTY_CHECK = Boolean.valueOf(p.getProperty("FrintezzaDisablePartyCheck", "False"));
			FRINTEZZA_MIN_PARTIES = TypeFormat.parseInt(p.getProperty("FrintezzaMaxParties", "4"));
			FRINTEZZA_MAX_PARTIES = TypeFormat.parseInt(p.getProperty("FrintezzaMaxParties", "5"));
			/** Orfen */
			ORFEN_RESP_FIRST = TypeFormat.parseInt(p.getProperty("OrfenRespFirst", "48"));
			ORFEN_RESP_SECOND = TypeFormat.parseInt(p.getProperty("OrfenRespSecond", "20"));
			/** Queen Ant */
			QA_RESP_NURSE = TypeFormat.parseInt(p.getProperty("QueenAntRespNurse", "60"));
			QA_RESP_ROYAL = TypeFormat.parseInt(p.getProperty("QueenAntRespRoyal", "120"));
			QA_RESP_FIRST = TypeFormat.parseInt(p.getProperty("QueenAntRespFirst", "20"));
			QA_RESP_SECOND = TypeFormat.parseInt(p.getProperty("QueenAntRespSecond", "8"));
			/** Sailren */
			SAILREN_RESP_FIRST = TypeFormat.parseInt(p.getProperty("SailrenRespFirst", "12"));
			SAILREN_RESP_SECOND = TypeFormat.parseInt(p.getProperty("SailrenRespSecond", "24"));
			/** Valakas */
			VALAKAS_WAIT_TIME = TypeFormat.parseInt(p.getProperty("ValakasWaitTime", "30"));
			VALAKAS_RESP_FIRST = TypeFormat.parseInt(p.getProperty("ValakasRespFirst", "192"));
			VALAKAS_RESP_SECOND = TypeFormat.parseInt(p.getProperty("ValakasRespSecond", "44"));
			VALAKAS_DESPAWN_TIME = Integer.parseInt(p.getProperty("ValakasDespawnTime", "15"));
			/** High Priestess van Halter */
			HPH_FIXINTERVALOFHALTER = TypeFormat.parseInt(p.getProperty("FixIntervalOfHalter", "172800"));
			if(HPH_FIXINTERVALOFHALTER < 300 || HPH_FIXINTERVALOFHALTER > 864000)
			{
				HPH_FIXINTERVALOFHALTER = 172800;
			}
			HPH_FIXINTERVALOFHALTER *= 6000;
			HPH_RANDOMINTERVALOFHALTER = TypeFormat.parseInt(p.getProperty("RandomIntervalOfHalter", "86400"));
			if(HPH_RANDOMINTERVALOFHALTER < 300 || HPH_RANDOMINTERVALOFHALTER > 864000)
			{
				HPH_RANDOMINTERVALOFHALTER = 86400;
			}
			HPH_RANDOMINTERVALOFHALTER *= 6000;
			HPH_APPTIMEOFHALTER = TypeFormat.parseInt(p.getProperty("AppTimeOfHalter", "20"));
			if(HPH_APPTIMEOFHALTER < 5 || HPH_APPTIMEOFHALTER > 60)
			{
				HPH_APPTIMEOFHALTER = 20;
			}
			HPH_APPTIMEOFHALTER *= 6000;
			HPH_ACTIVITYTIMEOFHALTER = TypeFormat.parseInt(p.getProperty("ActivityTimeOfHalter", "21600"));
			if(HPH_ACTIVITYTIMEOFHALTER < 7200 || HPH_ACTIVITYTIMEOFHALTER > 86400)
			{
				HPH_ACTIVITYTIMEOFHALTER = 21600;
			}
			HPH_ACTIVITYTIMEOFHALTER *= 1000;
			HPH_FIGHTTIMEOFHALTER = TypeFormat.parseInt(p.getProperty("FightTimeOfHalter", "7200"));
			if(HPH_FIGHTTIMEOFHALTER < 7200 || HPH_FIGHTTIMEOFHALTER > 21600)
			{
				HPH_FIGHTTIMEOFHALTER = 7200;
			}
			HPH_FIGHTTIMEOFHALTER *= 6000;
			HPH_CALLROYALGUARDHELPERCOUNT = TypeFormat.parseInt(p.getProperty("CallRoyalGuardHelperCount", "6"));
			if(HPH_CALLROYALGUARDHELPERCOUNT < 1 || HPH_CALLROYALGUARDHELPERCOUNT > 6)
			{
				HPH_CALLROYALGUARDHELPERCOUNT = 6;
			}
			HPH_CALLROYALGUARDHELPERINTERVAL = TypeFormat.parseInt(p.getProperty("CallRoyalGuardHelperInterval", "10"));
			if(HPH_CALLROYALGUARDHELPERINTERVAL < 1 || HPH_CALLROYALGUARDHELPERINTERVAL > 60)
			{
				HPH_CALLROYALGUARDHELPERINTERVAL = 10;
			}
			HPH_CALLROYALGUARDHELPERINTERVAL *= 6000;
			HPH_INTERVALOFDOOROFALTER = TypeFormat.parseInt(p.getProperty("IntervalOfDoorOfAlter", "5400"));
			if(HPH_INTERVALOFDOOROFALTER < 60 || HPH_INTERVALOFDOOROFALTER > 5400)
			{
				HPH_INTERVALOFDOOROFALTER = 5400;
			}
			HPH_INTERVALOFDOOROFALTER *= 6000;
			HPH_TIMEOFLOCKUPDOOROFALTAR = TypeFormat.parseInt(p.getProperty("TimeOfLockUpDoorOfAltar", "180"));
			if(HPH_TIMEOFLOCKUPDOOROFALTAR < 60 || HPH_TIMEOFLOCKUPDOOROFALTAR > 600)
			{
				HPH_TIMEOFLOCKUPDOOROFALTAR = 180;
			}
			HPH_TIMEOFLOCKUPDOOROFALTAR *= 6000;
			/** Zaken */
			ZAKEN_RESP_FIRST = TypeFormat.parseInt(p.getProperty("ZakenRespFirst", "60"));
			ZAKEN_RESP_SECOND = TypeFormat.parseInt(p.getProperty("ZakenRespSecond", "8"));

			p.clear();
		}
		catch(Exception e)
		{
			_log.warn("Failed to load " + ConfigFiles.GRANDBOSSES_INI);
		}
	}
	//============================================================
	// olympiad.ini
	//============================================================
	/** Olympiad */
	public static int OLY_START_TIME;
	public static int OLY_MIN;
	public static long OLY_CPERIOD;
	public static long OLY_BATTLE;
	public static long OLY_BWAIT;
	public static long OLY_IWAIT;
	public static long OLY_WPERIOD;
	public static long OLY_VPERIOD;
	public static int OLY_CLASSED;
	public static int OLY_NONCLASSED;
	public static int OLY_BATTLE_REWARD_ITEM;
	public static int OLY_CLASSED_RITEM_C;
	public static int OLY_NONCLASSED_RITEM_C;
	public static int OLY_MIN_POINT_FOR_EXCH;
	public static int OLY_COMP_RITEM;
	public static int OLY_GP_PER_POINT;
	public static int OLY_HERO_POINTS;
	public static boolean OLY_SAME_IP;
	public static String OLY_RESTRICTED_ITEMS;
	public static boolean OLY_NORMAL_MODE;
	public static boolean OLY_ANNOUNCE_GAMES;
	public static int OLY_DAYS;
	public static List<Integer> LIST_OLY_RESTRICTED_ITEMS = new FastList<Integer>();
	public static boolean OLY_REFRESH_SKILLS;
	public static boolean OLYMPIAD_GAME_LOG;;
	//============================================================
	public static void OlympiadConfig()
	{
		try
		{
			L2Properties p = new L2Properties(ConfigFiles.OLYMPIAD_INI);

			/** Olympiad */
			OLY_START_TIME = TypeFormat.parseInt(p.getProperty("OlyStartTime", "18"));
			OLY_MIN = TypeFormat.parseInt(p.getProperty("OlyMin", "00"));
			OLY_CPERIOD = TypeFormat.parseLong(p.getProperty("OlyCPeriod", "21600000"));
			OLY_BATTLE = TypeFormat.parseLong(p.getProperty("OlyBattle", "360000"));
			OLY_BWAIT = TypeFormat.parseLong(p.getProperty("OlyBWait", "600000"));
			OLY_IWAIT = TypeFormat.parseLong(p.getProperty("OlyIWait", "300000"));
			OLY_WPERIOD = TypeFormat.parseLong(p.getProperty("OlyWPeriod", "604800000"));
			OLY_VPERIOD = TypeFormat.parseLong(p.getProperty("OlyVPeriod", "86400000"));
			OLY_CLASSED = TypeFormat.parseInt(p.getProperty("OlyClassedParticipants", "5"));
			OLY_NONCLASSED = TypeFormat.parseInt(p.getProperty("OlyNonClassedParticipants", "9"));
			OLY_BATTLE_REWARD_ITEM = TypeFormat.parseInt(p.getProperty("OlyBattleRewItem", "6651"));
			OLY_CLASSED_RITEM_C = TypeFormat.parseInt(p.getProperty("OlyClassedRewItemCount", "50"));
			OLY_NONCLASSED_RITEM_C = TypeFormat.parseInt(p.getProperty("OlyNonClassedRewItemCount", "30"));
			OLY_MIN_POINT_FOR_EXCH = TypeFormat.parseInt(p.getProperty("OlyMinPointForExchange", "50"));
			OLY_COMP_RITEM = TypeFormat.parseInt(p.getProperty("OlyCompRewItem", "6651"));
			OLY_GP_PER_POINT = TypeFormat.parseInt(p.getProperty("OlyGPPerPoint", "1000"));
			OLY_HERO_POINTS = TypeFormat.parseInt(p.getProperty("OlyHeroPoints", "300"));
			OLY_SAME_IP = TypeFormat.parseBoolean(p.getProperty("OlySameIP","False"));
			OLY_RESTRICTED_ITEMS = p.getProperty("OlyRestrictedItems", "0");
			LIST_OLY_RESTRICTED_ITEMS = new FastList<Integer>();
			for(String id : OLY_RESTRICTED_ITEMS.split(","))
			{
				LIST_OLY_RESTRICTED_ITEMS.add(TypeFormat.parseInt(id));
			}
			OLY_NORMAL_MODE = TypeFormat.parseBoolean(p.getProperty("OlyNormalMode", "True"));
			OLY_ANNOUNCE_GAMES = Boolean.parseBoolean(p.getProperty("OlyAnnounceGames", "True"));
			OLY_DAYS = TypeFormat.parseInt(p.getProperty("OlyDays", "14"));
			OLY_REFRESH_SKILLS = TypeFormat.parseBoolean(p.getProperty("OlyRefreshSkills", "False"));
			OLYMPIAD_GAME_LOG = TypeFormat.parseBoolean(p.getProperty("OlyGameLog", "False"));

			p.clear();
		}
		catch(Exception e)
		{
			_log.warn("Failed to load " + ConfigFiles.OLYMPIAD_INI);
		}
	}
	//============================================================
	// options.ini
	//============================================================
	/** Options */
	public static int ZONE_TOWN;
	public static String DEFAULT_GLOBAL_CHAT;
	public static String DEFAULT_TRADE_CHAT;
	public static int MAX_CHAT_LENGTH;
	public static int DEFAULT_PUNISH;
	public static int DEFAULT_PUNISH_PARAM;
	public static int DELETE_DAYS;
	/** Item */
	public static boolean ALLOW_DISCARDITEM;
	public static int AUTODESTROY_ITEM_AFTER;
	public static int HERB_AUTO_DESTROY_TIME;
	public static String PROTECTED_ITEMS;
	public final static FastList<Integer> LIST_PROTECTED_ITEMS = new FastList<Integer>();
	public static boolean DESTROY_DROPPED_PLAYER_ITEM;
	public static boolean DESTROY_EQUIPABLE_PLAYER_ITEM;
	public static boolean SAVE_DROPPED_ITEM;
	public static boolean EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD;
	public static int SAVE_DROPPED_ITEM_INTERVAL;
	public static boolean CLEAR_DROPPED_ITEM_TABLE;
	/** Misc */
	public static int SHOP_MIN_RANGE_FROM_PLAYER;
	public static int SHOP_MIN_RANGE_FROM_NPC;
	public static boolean AUTODELETE_INVALID_QUEST_DATA;
	public static boolean PRECISE_DROP_CALCULATION;
	public static boolean MULTIPLE_ITEM_DROP;
	public static int DROP_PROTECTED_TIME;
	public static boolean FORCE_INVENTORY_UPDATE;
	public static boolean FORCE_COMPLETE_STATUS_UPDATE;
	public static int MAX_DRIFT_RANGE;
	public static int MIN_NPC_ANIMATION;
	public static int MAX_NPC_ANIMATION;
	public static int MIN_MONSTER_ANIMATION;
	public static int MAX_MONSTER_ANIMATION;
	public static boolean SHOW_NPC_LVL;
	public static boolean ALLOW_USE_CURSOR_FOR_WALK;
	/** Warehouse */
	public static boolean ALLOW_WAREHOUSE;
	public static boolean ALLOW_FREIGHT;
	public static boolean ENABLE_WAREHOUSESORTING_CLAN;
	public static boolean ENABLE_WAREHOUSESORTING_PRIVATE;
	/** Wear */
	public static boolean ALLOW_WEAR;
	public static int WEAR_DELAY;
	public static int WEAR_PRICE;
	/** Additionnal features */
	public static boolean ALLOW_LOTTERY;
	public static boolean ALLOW_RACE;
	public static boolean ALLOW_RENTPET;
	public static boolean ALLOW_FISHING;
	public static boolean ALLOW_WATER;
	public static boolean ALLOW_BOAT;
	public static boolean ALLOW_NPC_WALKERS;
	public static boolean ALLOW_CURSED_WEAPONS;
	/** Community board */
	public static String COMMUNITY_TYPE;
	public static String BBS_DEFAULT;
	public static boolean SHOW_LEVEL_COMMUNITYBOARD;
	public static boolean SHOW_STATUS_COMMUNITYBOARD;
	public static int NAME_PAGE_SIZE_COMMUNITYBOARD;
	public static int NAME_PER_ROW_COMMUNITYBOARD;
	public static boolean ALLOW_CUSTOM_COMMUNITY;
	public static List<String> COMMUNITY_BUFFER_EXCLUDE_ON = new FastList<String>();
	public static List<String> COMMUNITY_GATEKEEPER_EXCLUDE_ON = new FastList<String>();
	public static boolean ONLINE_COMMUNITY_BOARD;
	public static boolean COLOR_COMMUNITY_BOARD;
	//============================================================
	public static void OptionsConfig()
	{
		try
		{
			L2Properties p = new L2Properties(ConfigFiles.OPTIONS_INI);

			/** Options */
			ZONE_TOWN = TypeFormat.parseInt(p.getProperty("ZoneTown", "0"));
			DEFAULT_GLOBAL_CHAT = p.getProperty("GlobalChat", "ON");
			DEFAULT_TRADE_CHAT = p.getProperty("TradeChat", "ON");
			MAX_CHAT_LENGTH = TypeFormat.parseInt(p.getProperty("MaxChatLength", "100"));
			DEFAULT_PUNISH = TypeFormat.parseInt(p.getProperty("DefaultPunish", "2"));
			DEFAULT_PUNISH_PARAM = TypeFormat.parseInt(p.getProperty("DefaultPunishParam", "0"));
			DELETE_DAYS = TypeFormat.parseInt(p.getProperty("DeleteCharAfterDays", "7"));
			/** Item */
			ALLOW_DISCARDITEM = TypeFormat.parseBoolean(p.getProperty("AllowDiscardItem", "True"));
			AUTODESTROY_ITEM_AFTER = TypeFormat.parseInt(p.getProperty("AutoDestroyDroppedItemAfter", "0"));
			HERB_AUTO_DESTROY_TIME = TypeFormat.parseInt(p.getProperty("AutoDestroyHerbTime", "15")) * 1000;
			PROTECTED_ITEMS = p.getProperty("ListOfProtectedItems");
			for(String id : PROTECTED_ITEMS.split(","))
			{
				LIST_PROTECTED_ITEMS.add(TypeFormat.parseInt(id));
			}
			DESTROY_DROPPED_PLAYER_ITEM = TypeFormat.parseBoolean(p.getProperty("DestroyPlayerDroppedItem", "False"));
			DESTROY_EQUIPABLE_PLAYER_ITEM = TypeFormat.parseBoolean(p.getProperty("DestroyEquipableItem", "False"));
			SAVE_DROPPED_ITEM = TypeFormat.parseBoolean(p.getProperty("SaveDroppedItem", "False"));
			EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD = TypeFormat.parseBoolean(p.getProperty("EmptyDroppedItemTableAfterLoad", "False"));
			SAVE_DROPPED_ITEM_INTERVAL = TypeFormat.parseInt(p.getProperty("SaveDroppedItemInterval", "0")) * 60000;
			CLEAR_DROPPED_ITEM_TABLE = TypeFormat.parseBoolean(p.getProperty("ClearDroppedItemTable", "False"));
			/** Misc */
			SHOP_MIN_RANGE_FROM_PLAYER  = TypeFormat.parseInt(p.getProperty("ShopMinRangeFromPlayer", "0"));
			SHOP_MIN_RANGE_FROM_NPC  = TypeFormat.parseInt(p.getProperty("ShopMinRangeFromNpc", "0"));
			AUTODELETE_INVALID_QUEST_DATA = TypeFormat.parseBoolean(p.getProperty("AutoDeleteInvalidQuestData", "False"));
			PRECISE_DROP_CALCULATION = TypeFormat.parseBoolean(p.getProperty("PreciseDropCalculation", "True"));
			MULTIPLE_ITEM_DROP = TypeFormat.parseBoolean(p.getProperty("MultipleItemDrop", "True"));
			DROP_PROTECTED_TIME = TypeFormat.parseInt(p.getProperty("DropProtectedTime", "10"));
			FORCE_INVENTORY_UPDATE = TypeFormat.parseBoolean(p.getProperty("ForceInventoryUpdate", "False"));
			FORCE_COMPLETE_STATUS_UPDATE = Boolean.valueOf(p.getProperty("ForceCompletePlayerStatusUpdate", "true"));
			MAX_DRIFT_RANGE = TypeFormat.parseInt(p.getProperty("MaxDriftRange", "300"));
			MIN_NPC_ANIMATION = TypeFormat.parseInt(p.getProperty("MinNPCAnimation", "10"));
			MAX_NPC_ANIMATION = TypeFormat.parseInt(p.getProperty("MaxNPCAnimation", "20"));
			MIN_MONSTER_ANIMATION = TypeFormat.parseInt(p.getProperty("MinMonsterAnimation", "5"));
			MAX_MONSTER_ANIMATION = TypeFormat.parseInt(p.getProperty("MaxMonsterAnimation", "20"));
			SHOW_NPC_LVL = TypeFormat.parseBoolean(p.getProperty("ShowNpcLevel", "False"));
			ALLOW_USE_CURSOR_FOR_WALK = TypeFormat.parseBoolean(p.getProperty("AllowUseCursorForWalk", "False"));
			/** Warehouse */
			ALLOW_WAREHOUSE = TypeFormat.parseBoolean(p.getProperty("AllowWarehouse", "True"));
			ALLOW_FREIGHT = TypeFormat.parseBoolean(p.getProperty("AllowFreight", "True"));
			ENABLE_WAREHOUSESORTING_CLAN = TypeFormat.parseBoolean(p.getProperty("EnableWarehouseSortingClan", "False"));
			ENABLE_WAREHOUSESORTING_PRIVATE = TypeFormat.parseBoolean(p.getProperty("EnableWarehouseSortingPrivate", "False"));
			/** Wear */
			ALLOW_WEAR = TypeFormat.parseBoolean(p.getProperty("AllowWear", "False"));
			WEAR_DELAY = TypeFormat.parseInt(p.getProperty("WearDelay", "5"));
			WEAR_PRICE = TypeFormat.parseInt(p.getProperty("WearPrice", "10"));
			/** Additionnal features */
			ALLOW_LOTTERY = TypeFormat.parseBoolean(p.getProperty("AllowLottery", "False"));
			ALLOW_RACE = TypeFormat.parseBoolean(p.getProperty("AllowRace", "False"));
			ALLOW_RENTPET = TypeFormat.parseBoolean(p.getProperty("AllowRentPet", "False"));
			ALLOW_FISHING = TypeFormat.parseBoolean(p.getProperty("AllowFishing", "False"));
			ALLOW_BOAT = TypeFormat.parseBoolean(p.getProperty("AllowBoat", "False"));
			ALLOW_CURSED_WEAPONS = TypeFormat.parseBoolean(p.getProperty("AllowCursedWeapons", "False"));
			ALLOW_NPC_WALKERS = TypeFormat.parseBoolean(p.getProperty("AllowNpcWalkers", "True"));
			/** Community board */
			COMMUNITY_TYPE = p.getProperty("CommunityType", "old").toLowerCase();
			BBS_DEFAULT = p.getProperty("BBSDefault", "_bbshome");
			SHOW_LEVEL_COMMUNITYBOARD = TypeFormat.parseBoolean(p.getProperty("ShowLevelOnCommunityBoard", "False"));
			SHOW_STATUS_COMMUNITYBOARD = TypeFormat.parseBoolean(p.getProperty("ShowStatusOnCommunityBoard", "True"));
			NAME_PAGE_SIZE_COMMUNITYBOARD = TypeFormat.parseInt(p.getProperty("NamePageSizeOnCommunityBoard", "50"));
			NAME_PER_ROW_COMMUNITYBOARD = TypeFormat.parseInt(p.getProperty("NamePerRowOnCommunityBoard", "5"));
			ALLOW_CUSTOM_COMMUNITY = TypeFormat.parseBoolean(p.getProperty("CustomCommunityBoard", "False"));
			StringTokenizer buff = new StringTokenizer(p.getProperty("CommunityBufferExcludeOn", ""), " ");
			while(buff.hasMoreTokens())
			{
				COMMUNITY_BUFFER_EXCLUDE_ON.add(buff.nextToken());
			}
			StringTokenizer gk = new StringTokenizer(p.getProperty("GatekeeperExcludeOn", ""), " ");
			while(gk.hasMoreTokens())
			{
				COMMUNITY_GATEKEEPER_EXCLUDE_ON.add(gk.nextToken());
			}
			ONLINE_COMMUNITY_BOARD = TypeFormat.parseBoolean(p.getProperty("OnlineCommunityBoard", "False"));
			COLOR_COMMUNITY_BOARD = TypeFormat.parseBoolean(p.getProperty("ColorCommunityBoard", "False"));

			p.clear();
		}
		catch(Exception e)
		{
			_log.warn("Failed to load " + ConfigFiles.OPTIONS_INI);
		}
	}
	//============================================================
	// other.ini
	//============================================================
	/** Other */
	public static int STARTING_ADENA;
	public static int STARTING_AA;
	public static boolean CUSTOM_STARTER_ITEMS_ENABLED;
	public static List<int[]> STARTING_CUSTOM_ITEMS_M = new ArrayList<int[]>();
	public static List<int[]> STARTING_CUSTOM_ITEMS_F = new ArrayList<int[]>();
	public static int WYVERN_SPEED;
	public static int STRIDER_SPEED;
	public static boolean ALLOW_WYVERN_UPGRADER;
	public static boolean EFFECT_CANCELING;
	public static boolean GUARD_ATTACK_AGGRO_MOB;
	public static boolean DEEPBLUE_DROP_RULES;
	public static int INVENTORY_MAXIMUM_NO_DWARF;
	public static int INVENTORY_MAXIMUM_DWARF;
	public static int INVENTORY_MAXIMUM_GM;
	public static int MAX_ITEM_IN_PACKET;
	public static int WAREHOUSE_SLOTS_NO_DWARF;
	public static int WAREHOUSE_SLOTS_DWARF;
	public static int WAREHOUSE_SLOTS_CLAN;
	public static int FREIGHT_SLOTS;
	/** Need to Replace */
	public static double HP_REGEN_MULTIPLIER;
	public static double MP_REGEN_MULTIPLIER;
	public static double CP_REGEN_MULTIPLIER;
	public static double RAID_HP_REGEN_MULTIPLIER;
	public static double RAID_MP_REGEN_MULTIPLIER;
	public static double RAID_P_DEFENCE_MULTIPLIER;
	public static double RAID_M_DEFENCE_MULTIPLIER;
	public static float RAID_MIN_RESPAWN_MULTIPLIER;
	public static float RAID_MAX_RESPAWN_MULTIPLIER;
	public static double RAID_MINION_RESPAWN_TIMER;
	public static int UNSTUCK_INTERVAL;
	public static int PLAYER_SPAWN_PROTECTION;
	public static int PLAYER_FAKEDEATH_UP_PROTECTION;
	/** Party XP Distribution */
	public static String PARTY_XP_CUTOFF_METHOD;
	public static double PARTY_XP_CUTOFF_PERCENT;
	public static int PARTY_XP_CUTOFF_LEVEL;
	public static double RESPAWN_RESTORE_CP;
	public static double RESPAWN_RESTORE_HP;
	public static double RESPAWN_RESTORE_MP;
	public static boolean RESPAWN_RANDOM_ENABLED;
	public static int RESPAWN_RANDOM_MAX_OFFSET;
	public static int MAX_PVTSTORE_SLOTS_DWARF;
	public static int MAX_PVTSTORE_SLOTS_OTHER;
	public static boolean STORE_SKILL_COOLTIME;
	public static String PET_RENT_NPC;
	public static FastList<Integer> LIST_PET_RENT_NPC = new FastList<Integer>();
	public static boolean ANNOUNCE_MAMMON_SPAWN;
	/** Petitions */
	public static boolean PETITIONING_ALLOWED;
	public static int MAX_PETITIONS_PER_PLAYER;
	public static int MAX_PETITIONS_PENDING;
	/** Jail */
	public static boolean JAIL_IS_PVP;
	public static boolean JAIL_DISABLE_CHAT;
	/** Death Penalty */
	public static int DEATH_PENALTY_CHANCE;
	/** Skill Custom */
	public static boolean ENABLE_MODIFY_SKILL_DURATION;
	public static FastMap<Integer, Integer> SKILL_DURATION_LIST;
	/** Say Filter */
	public static boolean USE_SAY_FILTER;
	public static String CHAT_FILTER_CHARS;
	public static String CHAT_FILTER_PUNISHMENT;
	public static int CHAT_FILTER_PUNISHMENT_PARAM1;
	public static int CHAT_FILTER_PUNISHMENT_PARAM2;
	public static ArrayList<String> FILTER_LIST = new ArrayList<String>();
	/** Four Sepulchers */
	public static int FS_TIME_ATTACK;
	public static int FS_TIME_COOLDOWN;
	public static int FS_TIME_ENTRY;
	public static int FS_TIME_WARMUP;
	public static int FS_PARTY_MEMBER_COUNT;
	//============================================================
	public static void OtherConfig()
	{
		try
		{
			L2Properties p = new L2Properties(ConfigFiles.OTHER_INI);

			/** Other */
			STARTING_ADENA = TypeFormat.parseInt(p.getProperty("StartingAdena", "100"));
			STARTING_AA = TypeFormat.parseInt(p.getProperty("StartingAncientAdena", "0"));
			CUSTOM_STARTER_ITEMS_ENABLED = TypeFormat.parseBoolean(p.getProperty("CustomStarterItemsEnabled", "False"));
			if(Config.CUSTOM_STARTER_ITEMS_ENABLED)
			{
				String[] propertySplit = p.getProperty("StartingCustomItemsMage", "57,0").split(";");
				for(String reward : propertySplit)
				{
					String[] rewardSplit = reward.split(",");
					if(rewardSplit.length != 2)
					{
						_log.warn("StartingCustomItemsMage[Config.load()]: invalid config property -> StartingCustomItemsMage \"" + reward + "\"");
					}
					else
					{
						try
						{
							STARTING_CUSTOM_ITEMS_M.add(new int[]{TypeFormat.parseInt(rewardSplit[0]), TypeFormat.parseInt(rewardSplit[1])});
						}
						catch(NumberFormatException nfe)
						{
							if(!reward.isEmpty())
							{
								_log.warn("StartingCustomItemsMage[Config.load()]: invalid config property -> StartingCustomItemsMage \"" + reward + "\"");
							}
						}
					}
				}
				propertySplit = p.getProperty("StartingCustomItemsFighter", "57,0").split(";");
				for(String reward : propertySplit)
				{
					String[] rewardSplit = reward.split(",");
					if(rewardSplit.length != 2)
					{
						_log.warn("StartingCustomItemsFighter[Config.load()]: invalid config property -> StartingCustomItemsFighter \"" + reward + "\"");
					}
					else
					{
						try
						{
							STARTING_CUSTOM_ITEMS_F.add(new int[]{TypeFormat.parseInt(rewardSplit[0]), TypeFormat.parseInt(rewardSplit[1])});
						}
						catch(NumberFormatException nfe)
						{
							if(!reward.isEmpty())
							{
								_log.warn("StartingCustomItemsFighter[Config.load()]: invalid config property -> StartingCustomItemsFighter \"" + reward + "\"");
							}
						}
					}
				}
			}
			WYVERN_SPEED = TypeFormat.parseInt(p.getProperty("WyvernSpeed", "100"));
			STRIDER_SPEED = TypeFormat.parseInt(p.getProperty("StriderSpeed", "80"));
			ALLOW_WYVERN_UPGRADER = TypeFormat.parseBoolean(p.getProperty("AllowWyvernUpgrader", "False"));
			EFFECT_CANCELING = TypeFormat.parseBoolean(p.getProperty("CancelLesserEffect", "True"));
			GUARD_ATTACK_AGGRO_MOB = TypeFormat.parseBoolean(p.getProperty("GuardAttackAggroMob", "False"));	
			DEEPBLUE_DROP_RULES = TypeFormat.parseBoolean(p.getProperty("UseDeepBlueDropRules", "True"));
			INVENTORY_MAXIMUM_NO_DWARF = TypeFormat.parseInt(p.getProperty("MaximumSlotsForNoDwarf", "80"));
			INVENTORY_MAXIMUM_DWARF = TypeFormat.parseInt(p.getProperty("MaximumSlotsForDwarf", "100"));
			INVENTORY_MAXIMUM_GM = TypeFormat.parseInt(p.getProperty("MaximumSlotsForGMPlayer", "250"));
			MAX_ITEM_IN_PACKET = Math.max(INVENTORY_MAXIMUM_NO_DWARF, Math.max(INVENTORY_MAXIMUM_DWARF, INVENTORY_MAXIMUM_GM));
			WAREHOUSE_SLOTS_NO_DWARF = TypeFormat.parseInt(p.getProperty("MaximumWarehouseSlotsForNoDwarf", "100"));
			WAREHOUSE_SLOTS_DWARF = TypeFormat.parseInt(p.getProperty("MaximumWarehouseSlotsForDwarf", "120"));
			WAREHOUSE_SLOTS_CLAN = TypeFormat.parseInt(p.getProperty("MaximumWarehouseSlotsForClan", "150"));
			FREIGHT_SLOTS = TypeFormat.parseInt(p.getProperty("MaximumFreightSlots", "20"));
			/** Need to Replace */
			HP_REGEN_MULTIPLIER = TypeFormat.parseDouble(p.getProperty("HpRegenMultiplier", "100")) / 100;
			MP_REGEN_MULTIPLIER = TypeFormat.parseDouble(p.getProperty("MpRegenMultiplier", "100")) / 100;
			CP_REGEN_MULTIPLIER = TypeFormat.parseDouble(p.getProperty("CpRegenMultiplier", "100")) / 100;
			RAID_HP_REGEN_MULTIPLIER = TypeFormat.parseDouble(p.getProperty("RaidHpRegenMultiplier", "100")) / 100;
			RAID_MP_REGEN_MULTIPLIER = TypeFormat.parseDouble(p.getProperty("RaidMpRegenMultiplier", "100")) / 100;
			RAID_P_DEFENCE_MULTIPLIER = TypeFormat.parseDouble(p.getProperty("RaidPhysicalDefenceMultiplier", "100")) / 100;
			RAID_M_DEFENCE_MULTIPLIER = TypeFormat.parseDouble(p.getProperty("RaidMagicalDefenceMultiplier", "100")) / 100;
			RAID_MIN_RESPAWN_MULTIPLIER = TypeFormat.parseFloat(p.getProperty("RaidMinRespawnMultiplier", "1.0"));
			RAID_MAX_RESPAWN_MULTIPLIER = TypeFormat.parseFloat(p.getProperty("RaidMaxRespawnMultiplier", "1.0"));
			RAID_MINION_RESPAWN_TIMER = TypeFormat.parseInt(p.getProperty("RaidMinionRespawnTime", "300000"));
			UNSTUCK_INTERVAL = TypeFormat.parseInt(p.getProperty("UnstuckInterval", "300"));
			PLAYER_SPAWN_PROTECTION = TypeFormat.parseInt(p.getProperty("PlayerSpawnProtection", "0"));
			PLAYER_FAKEDEATH_UP_PROTECTION = TypeFormat.parseInt(p.getProperty("PlayerFakeDeathUpProtection", "0"));
			/** Party XP Distribution */
			PARTY_XP_CUTOFF_METHOD = p.getProperty("PartyXpCutoffMethod", "percentage");
			PARTY_XP_CUTOFF_PERCENT = TypeFormat.parseDouble(p.getProperty("PartyXpCutoffPercent", "3."));
			PARTY_XP_CUTOFF_LEVEL = TypeFormat.parseInt(p.getProperty("PartyXpCutoffLevel", "30"));
			RESPAWN_RESTORE_CP = TypeFormat.parseDouble(p.getProperty("RespawnRestoreCP", "0")) / 100;
			RESPAWN_RESTORE_HP = TypeFormat.parseDouble(p.getProperty("RespawnRestoreHP", "70")) / 100;
			RESPAWN_RESTORE_MP = TypeFormat.parseDouble(p.getProperty("RespawnRestoreMP", "70")) / 100;
			RESPAWN_RANDOM_ENABLED = TypeFormat.parseBoolean(p.getProperty("RespawnRandomInTown", "False"));
			RESPAWN_RANDOM_MAX_OFFSET = TypeFormat.parseInt(p.getProperty("RespawnRandomMaxOffset", "50"));
			MAX_PVTSTORE_SLOTS_DWARF = TypeFormat.parseInt(p.getProperty("MaxPvtStoreSlotsDwarf", "5"));
			MAX_PVTSTORE_SLOTS_OTHER = TypeFormat.parseInt(p.getProperty("MaxPvtStoreSlotsOther", "4"));
			STORE_SKILL_COOLTIME = TypeFormat.parseBoolean(p.getProperty("StoreSkillCooltime", "True"));
			PET_RENT_NPC = p.getProperty("ListPetRentNpc", "30827");
			LIST_PET_RENT_NPC = new FastList<Integer>();
			for(String id : PET_RENT_NPC.split(","))
			{
				LIST_PET_RENT_NPC.add(TypeFormat.parseInt(id));
			}
			ANNOUNCE_MAMMON_SPAWN = TypeFormat.parseBoolean(p.getProperty("AnnounceMammonSpawn", "True"));
			/** Petitions */
			PETITIONING_ALLOWED = TypeFormat.parseBoolean(p.getProperty("PetitioningAllowed", "True"));
			MAX_PETITIONS_PER_PLAYER = TypeFormat.parseInt(p.getProperty("MaxPetitionsPerPlayer", "5"));
			MAX_PETITIONS_PENDING = TypeFormat.parseInt(p.getProperty("MaxPetitionsPending", "25"));
			/** Jail */
			JAIL_IS_PVP = TypeFormat.parseBoolean(p.getProperty("JailIsPvp", "True"));
			JAIL_DISABLE_CHAT = TypeFormat.parseBoolean(p.getProperty("JailDisableChat", "True"));
			/** Death Penalty */
			DEATH_PENALTY_CHANCE = TypeFormat.parseInt(p.getProperty("DeathPenaltyChance", "20"));
			/** Skill Custom */
			ENABLE_MODIFY_SKILL_DURATION = TypeFormat.parseBoolean(p.getProperty("EnableModifySkillDuration", "False"));
			if(ENABLE_MODIFY_SKILL_DURATION)
			{
				SKILL_DURATION_LIST = new FastMap<Integer, Integer>();

				String[] propertySplit;
				propertySplit = p.getProperty("SkillDurationList", "").split(";");

				for(String skill : propertySplit)
				{
					String[] skillSplit = skill.split(",");
					if(skillSplit.length != 2)
					{
						System.out.println("[SkillDurationList]: invalid config property -> SkillDurationList \"" + skill + "\"");
					}
					else
					{
						try
						{
							SKILL_DURATION_LIST.put(TypeFormat.parseInt(skillSplit[0]), TypeFormat.parseInt(skillSplit[1]));
						}
						catch(NumberFormatException nfe)
						{
							if(!skill.equals(""))
							{
								System.out.println("[SkillDurationList]: invalid config property -> SkillList \"" + skillSplit[0] + "\"" + skillSplit[1]);
							}
						}
					}
				}
			}
			/** Say Filter */
			USE_SAY_FILTER = TypeFormat.parseBoolean(p.getProperty("UseChatFilter", "False"));
			CHAT_FILTER_CHARS = p.getProperty("ChatFilterChars", "^_^");
			CHAT_FILTER_PUNISHMENT = p.getProperty("ChatFilterPunishment", "off");
			CHAT_FILTER_PUNISHMENT_PARAM1 = TypeFormat.parseInt(p.getProperty("ChatFilterPunishmentParam1", "1"));
			CHAT_FILTER_PUNISHMENT_PARAM2 = TypeFormat.parseInt(p.getProperty("ChatFilterPunishmentParam2", "1000"));
			/** Four Sepulchers */
			FS_TIME_ATTACK = TypeFormat.parseInt(p.getProperty("TimeOfAttack", "50"));
			FS_TIME_COOLDOWN = TypeFormat.parseInt(p.getProperty("TimeOfCoolDown", "5"));
			FS_TIME_ENTRY = TypeFormat.parseInt(p.getProperty("TimeOfEntry", "3"));
			FS_TIME_WARMUP = TypeFormat.parseInt(p.getProperty("TimeOfWarmUp", "2"));
			FS_PARTY_MEMBER_COUNT = TypeFormat.parseInt(p.getProperty("NumberOfNecessaryPartyMembers", "4"));
			if(FS_TIME_ATTACK <= 0)
			{
				FS_TIME_ATTACK = 50;
			}

			if(FS_TIME_COOLDOWN <= 0)
			{
				FS_TIME_COOLDOWN = 5;
			}

			if(FS_TIME_ENTRY <= 0)
			{
				FS_TIME_ENTRY = 3;
			}

			if(FS_TIME_WARMUP <= 0)
			{
				FS_TIME_WARMUP = 2;
			}

			if(FS_PARTY_MEMBER_COUNT <= 0)
			{
				FS_PARTY_MEMBER_COUNT = 4;
			}

			p.clear();
		}
		catch(Exception e)
		{
			_log.warn("Failed to load " + ConfigFiles.OTHER_INI);
		}
	}
	//============================================================
	// physics.ini
	//============================================================
	/** Blow Succes Rates */
	public static int BLOW_ATTACK_FRONT;
	public static int BLOW_ATTACK_SIDE;
	public static int BLOW_ATTACK_BEHIND;	
	public static boolean BACKSTABRESTRICTION;
	/** Misc */
	public static int MANA_POTION_RES;
	public static boolean SCREEN_CRITICAL;
	/** Basic Statistic Control */
	public static int MAX_PATK_SPEED;
	public static int MAX_MATK_SPEED;
	public static int MAX_PCRIT_RATE;
	public static int MAX_MCRIT_RATE;
	public static int RUN_SPD_BOOST;
	public static int MAX_RUN_SPEED;
	public static int MCRIT_RATE_MUL;
	//============================================================
	public static void PhysicsConfig()
	{
		try
		{
			L2Properties p = new L2Properties(ConfigFiles.PHYSICS_INI);

			/** Blow Succes Rates */
			BLOW_ATTACK_FRONT = TypeFormat.parseInt(p.getProperty("BlowAttackFront", "50"));
			BLOW_ATTACK_SIDE = TypeFormat.parseInt(p.getProperty("BlowAttackSide", "60"));
			BLOW_ATTACK_BEHIND = TypeFormat.parseInt(p.getProperty("BlowAttackBehind", "70"));
			BACKSTABRESTRICTION = TypeFormat.parseBoolean(p.getProperty("RestrictionBackstab", "False"));
			/** Misc */
			MANA_POTION_RES = Integer.parseInt(p.getProperty("ManaPotionMPRes", "200"));
			SCREEN_CRITICAL = TypeFormat.parseBoolean(p.getProperty("ShowScreenCritical", "False"));
			/** Basic Statistic Control */
			MAX_PATK_SPEED = TypeFormat.parseInt(p.getProperty("MaxPAtkSpeed", "1500"));
			MAX_MATK_SPEED = TypeFormat.parseInt(p.getProperty("MaxMAtkSpeed", "1999"));
			if(MAX_PATK_SPEED < 1)
			{
				MAX_PATK_SPEED = Integer.MAX_VALUE;
			}

			if(MAX_MATK_SPEED < 1)
			{
				MAX_MATK_SPEED = Integer.MAX_VALUE;
			}
			MAX_PCRIT_RATE = TypeFormat.parseInt(p.getProperty("MaxPCritRate", "500"));
			MAX_MCRIT_RATE = TypeFormat.parseInt(p.getProperty("MaxMCritRate", "300"));
			RUN_SPD_BOOST = TypeFormat.parseInt(p.getProperty("RunSpeedBoost", "0"));
			MAX_RUN_SPEED = TypeFormat.parseInt(p.getProperty("MaxRunSpeed", "250"));
			MCRIT_RATE_MUL = TypeFormat.parseInt(p.getProperty("McritMulDif", "4"));

			p.clear();
		}
		catch(Exception e)
		{
			_log.warn("Failed to load " + ConfigFiles.PHYSICS_INI);
		}
	}
	//============================================================
	// pvp.ini
	//============================================================
	/** Karma / Pvp / Pk */
	public static int KARMA_MIN_KARMA;
	public static int KARMA_MAX_KARMA;
	public static int KARMA_XP_DIVIDER;
	public static int KARMA_LOST_BASE;
	public static boolean KARMA_DROP_GM;
	public static String KARMA_NONDROPPABLE_PET_ITEMS;
	public static FastList<Integer> KARMA_LIST_NONDROPPABLE_PET_ITEMS = new FastList<Integer>();
	public static String KARMA_NONDROPPABLE_ITEMS;
	public static FastList<Integer> KARMA_LIST_NONDROPPABLE_ITEMS = new FastList<Integer>();
	public static int KARMA_PK_LIMIT;
	public static boolean KARMA_AWARD_PK_KILL;
	public static int PVP_NORMAL_TIME;
	public static int PVP_PVP_TIME;
	/** Player Killer */
	public static boolean KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE;
	public static boolean KARMA_PLAYER_CAN_SHOP;
	public static boolean KARMA_PLAYER_CAN_TELEPORT;
	public static boolean KARMA_PLAYER_CAN_USE_GK;
	public static boolean KARMA_PLAYER_CAN_TRADE;
	public static boolean KARMA_PLAYER_CAN_USE_WAREHOUSE;
	/** Player vs Player */
	public static boolean FLAGED_PLAYER_CAN_USE_GK;
	public static boolean ALLOW_POTS_IN_PVP;
	public static boolean ALLOW_SOE_IN_PVP;
	/** Party Duel Spawn */
	public static int DUEL_SPAWN_X;
	public static int DUEL_SPAWN_Y;
	public static int DUEL_SPAWN_Z;
	//============================================================
	public static void PvpConfig()
	{
		try
		{
			L2Properties p = new L2Properties(ConfigFiles.PVP_INI);

			/** Karma / Pvp / Pk */
			KARMA_MIN_KARMA = TypeFormat.parseInt(p.getProperty("MinKarma", "240"));
			KARMA_MAX_KARMA = TypeFormat.parseInt(p.getProperty("MaxKarma", "10000"));
			KARMA_XP_DIVIDER = TypeFormat.parseInt(p.getProperty("XPDivider", "260"));
			KARMA_LOST_BASE = TypeFormat.parseInt(p.getProperty("BaseKarmaLost", "0"));
			KARMA_DROP_GM = TypeFormat.parseBoolean(p.getProperty("CanGMDropEquipment", "False"));
			KARMA_NONDROPPABLE_PET_ITEMS = p.getProperty("ListOfPetItems", "2375,3500,3501,3502,4422,4423,4424,4425,6648,6649,6650");
			KARMA_LIST_NONDROPPABLE_PET_ITEMS = new FastList<Integer>();
			for(String id : KARMA_NONDROPPABLE_PET_ITEMS.split(","))
			{
				KARMA_LIST_NONDROPPABLE_PET_ITEMS.add(TypeFormat.parseInt(id));
			}
			KARMA_NONDROPPABLE_ITEMS = p.getProperty("ListOfNonDroppableItems", "57,1147,425,1146,461,10,2368,7,6,2370,2369,6842,6611,6612,6613,6614,6615,6616,6617,6618,6619,6620,6621");
			KARMA_LIST_NONDROPPABLE_ITEMS = new FastList<Integer>();
			for(String id : KARMA_NONDROPPABLE_ITEMS.split(","))
			{
				KARMA_LIST_NONDROPPABLE_ITEMS.add(TypeFormat.parseInt(id));
			}
			KARMA_PK_LIMIT = TypeFormat.parseInt(p.getProperty("MinimumPKRequiredToDrop", "5"));
			KARMA_AWARD_PK_KILL = TypeFormat.parseBoolean(p.getProperty("AwardPKKillPVPPoint", "True"));
			PVP_NORMAL_TIME = TypeFormat.parseInt(p.getProperty("PvPVsNormalTime", "15000"));
			PVP_PVP_TIME = TypeFormat.parseInt(p.getProperty("PvPVsPvPTime", "30000"));
			/** Player Killer */
			KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE = TypeFormat.parseBoolean(p.getProperty("KarmaPlayerCanBeKilledInPeaceZone", "False"));
			KARMA_PLAYER_CAN_SHOP = TypeFormat.parseBoolean(p.getProperty("KarmaPlayerCanShop", "True"));
			KARMA_PLAYER_CAN_TELEPORT = TypeFormat.parseBoolean(p.getProperty("KarmaPlayerCanTeleport", "True"));
			KARMA_PLAYER_CAN_USE_GK = TypeFormat.parseBoolean(p.getProperty("KarmaPlayerCanUseGK", "False"));
			KARMA_PLAYER_CAN_TRADE = TypeFormat.parseBoolean(p.getProperty("KarmaPlayerCanTrade", "True"));
			KARMA_PLAYER_CAN_USE_WAREHOUSE = TypeFormat.parseBoolean(p.getProperty("KarmaPlayerCanUseWareHouse", "True"));
			/** Player vs Player */
			FLAGED_PLAYER_CAN_USE_GK = TypeFormat.parseBoolean(p.getProperty("FlaggedPlayerCanUseGK", "False"));
			ALLOW_POTS_IN_PVP = TypeFormat.parseBoolean(p.getProperty("AllowPotsInPvP", "True"));
			ALLOW_SOE_IN_PVP = TypeFormat.parseBoolean(p.getProperty("AllowSoEInPvP", "False"));
			/** Party Duel Spawn */
			DUEL_SPAWN_X = TypeFormat.parseInt(p.getProperty("DuelSpawnX", "-102495"));
			DUEL_SPAWN_Y = TypeFormat.parseInt(p.getProperty("DuelSpawnY", "-209023"));
			DUEL_SPAWN_Z = TypeFormat.parseInt(p.getProperty("DuelSpawnZ", "-3326"));

			p.clear();
		}
		catch(Exception e)
		{
			_log.warn("Failed to load " + ConfigFiles.PVP_INI);
		}
	}
	//============================================================
	// rates.ini
	//============================================================
	/** Basic Rate Control */
	public static float RATE_XP;
	public static float RATE_SP;
	public static float RATE_PARTY_XP;
	public static float RATE_PARTY_SP;
	public static float RATE_DROP_ADENA;
	public static float RATE_CONSUMABLE_COST;
	public static float RATE_DROP_ITEMS;
	public static float RATE_DROP_SEAL_STONES;
	public static float RATE_DROP_SPOIL;
	public static int RATE_DROP_MANOR;
	public static float RATE_DROP_QUEST;
	public static float RATE_QUESTS_REWARD;
	public static float RATE_KARMA_EXP_LOST;
	public static float RATE_SIEGE_GUARDS_PRICE;
	public static int PLAYER_DROP_LIMIT;
	public static int PLAYER_RATE_DROP;
	public static int PLAYER_RATE_DROP_ITEM;
	public static int PLAYER_RATE_DROP_EQUIP;
	public static int PLAYER_RATE_DROP_EQUIP_WEAPON;
	public static int KARMA_DROP_LIMIT;
	public static int KARMA_RATE_DROP;
	public static int KARMA_RATE_DROP_ITEM;
	public static int KARMA_RATE_DROP_EQUIP;
	public static int KARMA_RATE_DROP_EQUIP_WEAPON;
	public static float PET_XP_RATE;
	public static float SINEATER_XP_RATE;
	public static int PET_FOOD_RATE;
	/** Herbs Rate Control */
	public static float RATE_DROP_COMMON_HERBS;
	public static float RATE_DROP_MP_HP_HERBS;
	public static float RATE_DROP_GREATER_HERBS;
	public static float RATE_DROP_SUPERIOR_HERBS;
	public static float RATE_DROP_SPECIAL_HERBS;
	public static int HP_RATE_MOBS_HERB;
	/** RB Rate Control */
	public static float ADENA_BOSS;
	public static float ADENA_RAID;
	public static float ADENA_MINION;
	public static float JEWEL_BOSS;
	public static float ITEMS_BOSS;
	public static float ITEMS_RAID;
	public static float ITEMS_MINION;
	public static float SPOIL_BOSS;
	public static float SPOIL_RAID;
	public static float SPOIL_MINION;
	/** Normal Scroll */
	public static FastMap<Integer, Integer> NORMAL_WEAPON_ENCHANT_LEVEL = new FastMap<Integer, Integer>();
	public static FastMap<Integer, Integer> NORMAL_ARMOR_ENCHANT_LEVEL = new FastMap<Integer, Integer>();
	public static FastMap<Integer, Integer> NORMAL_JEWELRY_ENCHANT_LEVEL = new FastMap<Integer, Integer>();
	/** Blessed Scroll */
	public static FastMap<Integer, Integer> BLESS_WEAPON_ENCHANT_LEVEL = new FastMap<Integer, Integer>();
	public static FastMap<Integer, Integer> BLESS_ARMOR_ENCHANT_LEVEL = new FastMap<Integer, Integer>();
	public static FastMap<Integer, Integer> BLESS_JEWELRY_ENCHANT_LEVEL = new FastMap<Integer, Integer>();
	/** Crystal Scroll */
	public static FastMap<Integer, Integer> CRYTAL_WEAPON_ENCHANT_LEVEL = new FastMap<Integer, Integer>();
	public static FastMap<Integer, Integer> CRYSTAL_ARMOR_ENCHANT_LEVEL = new FastMap<Integer, Integer>();
	public static FastMap<Integer, Integer> CRYSTAL_JEWELRY_ENCHANT_LEVEL = new FastMap<Integer, Integer>();
	/** Other Enchant Configs */
	public static int ENCHANT_SAFE_MAX;
	public static int ENCHANT_SAFE_MAX_FULL;
	public static int ENCHANT_WEAPON_MAX;
	public static int ENCHANT_ARMOR_MAX;
	public static int ENCHANT_JEWELRY_MAX;
	public static boolean ENCHANT_HERO_WEAPON;
	public static int CUSTOM_ENCHANT_VALUE;
	public static int BREAK_ENCHANT;
	public static int OLY_ENCHANT_LIMIT;
	public static int CRYSTAL_ENCHANT_MAX;
	public static int CRYSTAL_ENCHANT_MIN;
	/** Augumentation */
	public static int AUGMENTATION_NG_SKILL_CHANCE;
	public static int AUGMENTATION_MID_SKILL_CHANCE;
	public static int AUGMENTATION_HIGH_SKILL_CHANCE;
	public static int AUGMENTATION_TOP_SKILL_CHANCE;
	public static int AUGMENTATION_BASESTAT_CHANCE;
	public static int AUGMENTATION_NG_GLOW_CHANCE;
	public static int AUGMENTATION_MID_GLOW_CHANCE;
	public static int AUGMENTATION_HIGH_GLOW_CHANCE;
	public static int AUGMENTATION_TOP_GLOW_CHANCE;
	/** Soul Crystal */
	public static int SOUL_CRYSTAL_BREAK_CHANCE;
	public static int SOUL_CRYSTAL_LEVEL_CHANCE;
	public static int SOUL_CRYSTAL_MAX_LEVEL;
	//============================================================
	public static void RatesConfig()
	{
		try
		{
			L2Properties p = new L2Properties(ConfigFiles.RATES_INI);

			/** Basic Rate Control */
			RATE_XP = TypeFormat.parseFloat(p.getProperty("RateXp", "1.00"));
			RATE_SP = TypeFormat.parseFloat(p.getProperty("RateSp", "1.00"));
			RATE_PARTY_XP = TypeFormat.parseFloat(p.getProperty("RatePartyXp", "1.00"));
			RATE_PARTY_SP = TypeFormat.parseFloat(p.getProperty("RatePartySp", "1.00"));
			RATE_DROP_ADENA = TypeFormat.parseFloat(p.getProperty("RateDropAdena", "1.00"));
			RATE_CONSUMABLE_COST = TypeFormat.parseFloat(p.getProperty("RateConsumableCost", "1.00"));
			RATE_DROP_ITEMS = TypeFormat.parseFloat(p.getProperty("RateDropItems", "1.00"));
			RATE_DROP_SEAL_STONES = TypeFormat.parseFloat(p.getProperty("RateDropSealStones", "1.00"));
			RATE_DROP_SPOIL = TypeFormat.parseFloat(p.getProperty("RateDropSpoil", "1.00"));
			RATE_DROP_MANOR = TypeFormat.parseInt(p.getProperty("RateDropManor", "1.00"));
			RATE_DROP_QUEST = TypeFormat.parseFloat(p.getProperty("RateDropQuest", "1.00"));
			RATE_QUESTS_REWARD = TypeFormat.parseFloat(p.getProperty("RateQuestsReward", "1.00"));
			RATE_KARMA_EXP_LOST = TypeFormat.parseFloat(p.getProperty("RateKarmaExpLost", "1.00"));
			RATE_SIEGE_GUARDS_PRICE = TypeFormat.parseFloat(p.getProperty("RateSiegeGuardsPrice", "1.00"));
			PLAYER_DROP_LIMIT = TypeFormat.parseInt(p.getProperty("PlayerDropLimit", "3"));
			PLAYER_RATE_DROP = TypeFormat.parseInt(p.getProperty("PlayerRateDrop", "5"));
			PLAYER_RATE_DROP_ITEM = TypeFormat.parseInt(p.getProperty("PlayerRateDropItem", "70"));
			PLAYER_RATE_DROP_EQUIP = TypeFormat.parseInt(p.getProperty("PlayerRateDropEquip", "25"));
			PLAYER_RATE_DROP_EQUIP_WEAPON = TypeFormat.parseInt(p.getProperty("PlayerRateDropEquipWeapon", "5"));
			KARMA_DROP_LIMIT = TypeFormat.parseInt(p.getProperty("KarmaDropLimit", "10"));
			KARMA_RATE_DROP = TypeFormat.parseInt(p.getProperty("KarmaRateDrop", "70"));
			KARMA_RATE_DROP_ITEM = TypeFormat.parseInt(p.getProperty("KarmaRateDropItem", "50"));
			KARMA_RATE_DROP_EQUIP = TypeFormat.parseInt(p.getProperty("KarmaRateDropEquip", "40"));
			KARMA_RATE_DROP_EQUIP_WEAPON = TypeFormat.parseInt(p.getProperty("KarmaRateDropEquipWeapon", "10"));
			PET_XP_RATE = TypeFormat.parseFloat(p.getProperty("PetXpRate", "1.00"));
			SINEATER_XP_RATE = TypeFormat.parseFloat(p.getProperty("SinEaterXpRate", "1.00"));
			PET_FOOD_RATE = TypeFormat.parseInt(p.getProperty("PetFoodRate", "1"));
			/** Herbs Rate Control */
			RATE_DROP_COMMON_HERBS = TypeFormat.parseFloat(p.getProperty("RateCommonHerbs", "15.00"));
			RATE_DROP_MP_HP_HERBS = TypeFormat.parseFloat(p.getProperty("RateHpMpHerbs", "10.00"));
			RATE_DROP_GREATER_HERBS = TypeFormat.parseFloat(p.getProperty("RateGreaterHerbs", "4.00"));
			RATE_DROP_SUPERIOR_HERBS = TypeFormat.parseFloat(p.getProperty("RateSuperiorHerbs", "0.80")) * 10;
			RATE_DROP_SPECIAL_HERBS = TypeFormat.parseFloat(p.getProperty("RateSpecialHerbs", "0.20")) * 10;
			HP_RATE_MOBS_HERB = TypeFormat.parseInt(p.getProperty("HpRateMobsHerb", "1"));
			/** RB Rate Control */
			ADENA_BOSS = TypeFormat.parseFloat(p.getProperty("AdenaBoss", "1.00"));
			ADENA_RAID = TypeFormat.parseFloat(p.getProperty("AdenaRaid", "1.00"));
			ADENA_MINION = TypeFormat.parseFloat(p.getProperty("AdenaMinon", "1.00"));
			JEWEL_BOSS = TypeFormat.parseFloat(p.getProperty("JewelBoss", "1.00"));
			ITEMS_BOSS = TypeFormat.parseFloat(p.getProperty("ItemsBoss", "1.00"));
			ITEMS_RAID = TypeFormat.parseFloat(p.getProperty("ItemsRaid", "1.00"));
			ITEMS_MINION = TypeFormat.parseFloat(p.getProperty("ItemsMinon", "1.00"));
			SPOIL_BOSS = TypeFormat.parseFloat(p.getProperty("SpoilBoss", "1.00"));
			SPOIL_RAID = TypeFormat.parseFloat(p.getProperty("SpoilRaid", "1.00"));
			SPOIL_MINION = TypeFormat.parseFloat(p.getProperty("SpoilMinon", "1.00"));
			/** Normal Scroll */
			String[] propertySplit = p.getProperty("NormalWeaponEnchantLevel", "").split(";");
			for(String readData : propertySplit)
			{
				String[] writeData = readData.split(",");
				if(writeData.length != 2)
				{
					System.out.println("Invalid config property");
				}
				else
				{
					try
					{
						NORMAL_WEAPON_ENCHANT_LEVEL.put(TypeFormat.parseInt(writeData[0]), TypeFormat.parseInt(writeData[1]));
					}
					catch(NumberFormatException nfe)
					{
						if(!readData.equals(""))
						{
							System.out.println("Invalid config property");
						}
					}
				}
			}
			propertySplit = p.getProperty("NormalArmorEnchantLevel", "").split(";");
			for(String readData : propertySplit)
			{
				String[] writeData = readData.split(",");
				if(writeData.length != 2)
				{
					System.out.println("Invalid config property");
				}
				else
				{
					try
					{
						NORMAL_ARMOR_ENCHANT_LEVEL.put(TypeFormat.parseInt(writeData[0]), TypeFormat.parseInt(writeData[1]));
					}
					catch(NumberFormatException nfe)
					{
						if(!readData.equals(""))
						{
							System.out.println("Invalid config property");
						}
					}
				}
			}
			propertySplit = p.getProperty("NormalJewelryEnchantLevel", "").split(";");
			for(String readData : propertySplit)
			{
				String[] writeData = readData.split(",");
				if(writeData.length != 2)
				{
					System.out.println("Invalid config property");
				}
				else
				{
					try
					{
						NORMAL_JEWELRY_ENCHANT_LEVEL.put(TypeFormat.parseInt(writeData[0]), TypeFormat.parseInt(writeData[1]));
					}
					catch(NumberFormatException nfe)
					{
						if(!readData.equals(""))
						{
							System.out.println("Invalid config property");
						}
					}
				}
			}
			/** Blessed Scroll */
			propertySplit = p.getProperty("BlessWeaponEnchantLevel", "").split(";");
			for(String readData : propertySplit)
			{
				String[] writeData = readData.split(",");
				if(writeData.length != 2)
				{
					System.out.println("Invalid config property");
				}
				else
				{
					try
					{
						BLESS_WEAPON_ENCHANT_LEVEL.put(TypeFormat.parseInt(writeData[0]), TypeFormat.parseInt(writeData[1]));
					}
					catch(NumberFormatException nfe)
					{
						if(!readData.equals(""))
						{
							System.out.println("Invalid config property");
						}
					}
				}
			}
			propertySplit = p.getProperty("BlessArmorEnchantLevel", "").split(";");
			for(String readData : propertySplit)
			{
				String[] writeData = readData.split(",");
				if(writeData.length != 2)
				{
					System.out.println("Invalid config property");
				}
				else
				{
					try
					{
						BLESS_ARMOR_ENCHANT_LEVEL.put(TypeFormat.parseInt(writeData[0]), TypeFormat.parseInt(writeData[1]));
					}
					catch(NumberFormatException nfe)
					{
						if(!readData.equals(""))
						{
							System.out.println("Invalid config property");
						}
					}
				}
			}
			propertySplit = p.getProperty("BlessJewelryEnchantLevel", "").split(";");
			for(String readData : propertySplit)
			{
				String[] writeData = readData.split(",");
				if(writeData.length != 2)
				{
					System.out.println("Invalid config property");
				}
				else
				{
					try
					{
						BLESS_JEWELRY_ENCHANT_LEVEL.put(TypeFormat.parseInt(writeData[0]), TypeFormat.parseInt(writeData[1]));
					}
					catch(NumberFormatException nfe)
					{
						if(!readData.equals(""))
						{
							System.out.println("Invalid config property");
						}
					}
				}
			}
			/** Crystal Scroll */
			propertySplit = p.getProperty("CrystalWeaponEnchantLevel", "").split(";");
			for(String readData : propertySplit)
			{
				String[] writeData = readData.split(",");
				if(writeData.length != 2)
				{
					System.out.println("Invalid config property");
				}
				else
				{
					try
					{
						CRYTAL_WEAPON_ENCHANT_LEVEL.put(TypeFormat.parseInt(writeData[0]), TypeFormat.parseInt(writeData[1]));
					}
					catch(NumberFormatException nfe)
					{
						if(!readData.equals(""))
						{
							System.out.println("Invalid config property");
						}
					}
				}
			}
			propertySplit = p.getProperty("CrystalArmorEnchantLevel", "").split(";");
			for(String readData : propertySplit)
			{
				String[] writeData = readData.split(",");
				if(writeData.length != 2)
				{
					System.out.println("Invalid config property");
				}
				else
				{
					try
					{
						CRYSTAL_ARMOR_ENCHANT_LEVEL.put(TypeFormat.parseInt(writeData[0]), TypeFormat.parseInt(writeData[1]));
					}
					catch(NumberFormatException nfe)
					{
						if(!readData.equals(""))
						{
							System.out.println("invalid config property");
						}
					}
				}
			}
			propertySplit = p.getProperty("CrystalJewelryEnchantLevel", "").split(";");
			for(String readData : propertySplit)
			{
				String[] writeData = readData.split(",");
				if(writeData.length != 2)
				{
					System.out.println("Invalid config property");
				}
				else
				{
					try
					{
						CRYSTAL_JEWELRY_ENCHANT_LEVEL.put(TypeFormat.parseInt(writeData[0]), TypeFormat.parseInt(writeData[1]));
					}
					catch(NumberFormatException nfe)
					{
						if(!readData.equals(""))
						{
							System.out.println("Invalid config property");
						}
					}
				}
			}
			/** Other Enchant Configs */
			ENCHANT_SAFE_MAX = TypeFormat.parseInt(p.getProperty("EnchantSafeMax", "3"));
			ENCHANT_SAFE_MAX_FULL = TypeFormat.parseInt(p.getProperty("EnchantSafeMaxFull", "4"));
			ENCHANT_WEAPON_MAX = TypeFormat.parseInt(p.getProperty("EnchantWeaponMax", "0"));
			ENCHANT_ARMOR_MAX = TypeFormat.parseInt(p.getProperty("EnchantArmorMax", "0"));
			ENCHANT_JEWELRY_MAX = TypeFormat.parseInt(p.getProperty("EnchantJewelryMax", "0"));
			ENCHANT_HERO_WEAPON = TypeFormat.parseBoolean(p.getProperty("EnableEnchantHeroWeapons", "False"));
			CUSTOM_ENCHANT_VALUE = TypeFormat.parseInt(p.getProperty("CustomEnchantValue", "1"));
			BREAK_ENCHANT = TypeFormat.parseInt(p.getProperty("BreakEnchant", "0"));
			OLY_ENCHANT_LIMIT = TypeFormat.parseInt(p.getProperty("OlyMaxEnchant", "-1"));
			CRYSTAL_ENCHANT_MIN = Integer.parseInt(p.getProperty("CrystalEnchantMin", "0"));
			CRYSTAL_ENCHANT_MAX = Integer.parseInt(p.getProperty("CrystalEnchantMax", "20"));
			/** Augumentation */
			AUGMENTATION_NG_SKILL_CHANCE = TypeFormat.parseInt(p.getProperty("AugmentationNGSkillChance", "15"));
			AUGMENTATION_MID_SKILL_CHANCE = TypeFormat.parseInt(p.getProperty("AugmentationMidSkillChance", "30"));
			AUGMENTATION_HIGH_SKILL_CHANCE = TypeFormat.parseInt(p.getProperty("AugmentationHighSkillChance", "45"));
			AUGMENTATION_TOP_SKILL_CHANCE = TypeFormat.parseInt(p.getProperty("AugmentationTopSkillChance", "60"));
			AUGMENTATION_BASESTAT_CHANCE = TypeFormat.parseInt(p.getProperty("AugmentationBaseStatChance", "1"));
			AUGMENTATION_NG_GLOW_CHANCE = TypeFormat.parseInt(p.getProperty("AugmentationNGGlowChance", "0"));
			AUGMENTATION_MID_GLOW_CHANCE = TypeFormat.parseInt(p.getProperty("AugmentationMidGlowChance", "40"));
			AUGMENTATION_HIGH_GLOW_CHANCE = TypeFormat.parseInt(p.getProperty("AugmentationHighGlowChance", "70"));
			AUGMENTATION_TOP_GLOW_CHANCE = TypeFormat.parseInt(p.getProperty("AugmentationTopGlowChance", "100"));
			/** Soul Crystal */
			SOUL_CRYSTAL_BREAK_CHANCE = TypeFormat.parseInt(p.getProperty("SoulCrystalBreakChance", "10"));
			SOUL_CRYSTAL_LEVEL_CHANCE = TypeFormat.parseInt(p.getProperty("SoulCrystalLevelChance", "32"));
			SOUL_CRYSTAL_MAX_LEVEL = TypeFormat.parseInt(p.getProperty("SoulCrystalMaxLevel", "13"));

			p.clear();
		}
		catch(Exception e)
		{
			_log.warn("Failed to load " + ConfigFiles.RATES_INI);
		}
	}
	//============================================================
	// sevensigns.ini
	//============================================================
	/** Seven Signs */
	public static boolean REQUIRE_CASTLE_DAWN;
	public static boolean REQUIRE_CLAN_CASTLE;
	public static boolean REQUIRE_WIN_7S;
	/** Festival Rules */
	public static int FESTIVAL_MIN_PLAYER;
	public static int MAXIMUM_PLAYER_CONTRIB;
	public static long FESTIVAL_MANAGER_START;
	public static long FESTIVAL_LENGTH;
	public static long FESTIVAL_CYCLE_LENGTH;
	public static long FESTIVAL_FIRST_SPAWN;
	public static long FESTIVAL_FIRST_SWARM;
	public static long FESTIVAL_SECOND_SPAWN;
	public static long FESTIVAL_SECOND_SWARM;
	public static long FESTIVAL_CHEST_SPAWN;
	//============================================================
	public static void SevenSignsConfig()
	{
		try
		{
			L2Properties p = new L2Properties(ConfigFiles.SEVENSIGNS_INI);

			/** Seven Signs */
			REQUIRE_CASTLE_DAWN = TypeFormat.parseBoolean(p.getProperty("RequireCastleForDawn", "False"));
			REQUIRE_CLAN_CASTLE = TypeFormat.parseBoolean(p.getProperty("RequireClanCastle", "False"));
			REQUIRE_WIN_7S = TypeFormat.parseBoolean(p.getProperty("RequireWin7s", "True"));
			/** Festival Rules */
			FESTIVAL_MIN_PLAYER = TypeFormat.parseInt(p.getProperty("FestivalMinPlayer", "5"));
			MAXIMUM_PLAYER_CONTRIB = TypeFormat.parseInt(p.getProperty("MaxPlayerContrib", "1000000"));
			FESTIVAL_MANAGER_START = TypeFormat.parseLong(p.getProperty("FestivalManagerStart", "120000"));
			FESTIVAL_LENGTH = TypeFormat.parseLong(p.getProperty("FestivalLength", "1080000"));
			FESTIVAL_CYCLE_LENGTH = TypeFormat.parseLong(p.getProperty("FestivalCycleLength", "2280000"));
			FESTIVAL_FIRST_SPAWN = TypeFormat.parseLong(p.getProperty("FestivalFirstSpawn", "120000"));
			FESTIVAL_FIRST_SWARM = TypeFormat.parseLong(p.getProperty("FestivalFirstSwarm", "300000"));
			FESTIVAL_SECOND_SPAWN = TypeFormat.parseLong(p.getProperty("FestivalSecondSpawn", "540000"));
			FESTIVAL_SECOND_SWARM = TypeFormat.parseLong(p.getProperty("FestivalSecondSwarm", "720000"));
			FESTIVAL_CHEST_SPAWN = TypeFormat.parseLong(p.getProperty("FestivalChestSpawn", "900000"));

			p.clear();
		}
		catch(Exception e)
		{
			_log.warn("Failed to load " + ConfigFiles.SEVENSIGNS_INI);
		}
	}

	//============================================================
	// banned_ip.cfg
	//============================================================
	public static List<String> BANS = new FastList<String>();
	//============================================================
	public static void BanIPConfig()
	{
		try
		{
			L2Properties p = new L2Properties(ConfigFiles.BANNED_IP);

			BANS = p.getStringList("IP", "", ",");

			p.clear();
		}
		catch(Exception e)
		{
			_log.warn("Failed to load " + ConfigFiles.BANNED_IP);
		}
	}
	//============================================================
	// chatfilter.txt
	//============================================================
	public static void ChatFilter()
	{
		try
		{
			lnr = new LineNumberReader(new BufferedReader(new FileReader(new File(ConfigFiles.FILTER_FILE))));
			String line = null;
			while((line = lnr.readLine()) != null)
			{
				if(line.trim().length() == 0 || line.startsWith("#"))
				{
					continue;
				}
				FILTER_LIST.add(line.trim());
			}
		}
		catch(Exception e)
		{
			_log.warn("Failed to load " + ConfigFiles.FILTER_FILE + " file.");
		}
	}
	//============================================================
	// hexid.txt
	//============================================================
	private static final String HEXID_FILE = ConfigFiles.HEXID_FILE;
	public static int SERVER_ID;
	public static byte[] HEX_ID;
	//============================================================
	public static void Hexid()
	{
		try
		{
			L2Properties p = new L2Properties(ConfigFiles.HEXID_FILE);

			SERVER_ID = TypeFormat.parseInt(p.getProperty("ServerID"));
			HEX_ID = new BigInteger(p.getProperty("HexID"), 16).toByteArray();

			p.clear();
		}
		catch(Exception e)
		{
			_log.warn("Could not load HexID file (" + ConfigFiles.HEXID_FILE + "). Hopefully login will give us one.");
			System.exit(1);
		}
	}

	//============================================================
	// admin.ini
	//============================================================
	/** Game-Master */
	public static boolean GM_EVERYBODY;
	public static int GM_ACCESS_LEVEL;
	public static int GM_NAME_COLOR;
	public static int GM_TITLE_COLOR;
	public static boolean GM_HERO_AURA;
	public static boolean GM_STARTUP_INVISIBLE;
	public static boolean GM_SUPER_HASTE;
	public static boolean GM_GIVE_SPECIAL_SKILLS;
	public static boolean GM_SPECIAL_EFFECT;
	public static boolean GM_STARTUP_SILENCE;
	public static boolean GM_STARTUP_DIET;
	public static boolean GM_STARTUP_AUTO_LIST;
	public static boolean GM_STARTUP_INVULNERABLE;
	public static boolean GM_TRADE_RESTRICTED_ITEMS;
	public static boolean GM_RESTART_FIGHTING;
	public static boolean GM_WELCOME_HTM;
	public static int GM_OVER_ENCHANT;
	public static boolean GM_SHOW_LOGIN;
	public static boolean GM_ANNOUNCER_NAME;
	public static boolean GM_ONLY_ITEMS_FREE;
	/** Developer */
	public static boolean ALT_DEV_NO_QUESTS;
	public static boolean ALT_DEV_NO_SPAWNS;
	public static boolean ALT_DEV_NO_SCRIPT;
	public static boolean ALT_DEV_NO_AI;
	public static boolean ALT_DEV_NO_RB;
	public static boolean SERVER_LIST_TESTSERVER;
	public static boolean SERVER_LIST_BRACKET;
	public static boolean SERVER_LIST_CLOCK;
	public static boolean SERVER_GMONLY;
	/** Character */
	public static String CNAME_TEMPLATE;
	public static String PET_NAME_TEMPLATE;
	public static String CLAN_NAME_TEMPLATE;
	public static int MAX_CHARACTERS_NUMBER_PER_ACCOUNT;
	/** Logs Settings */
	public static boolean GMAUDIT;
	public static boolean LOG_CHAT;
	public static boolean LOG_ITEMS;
	/** Optimization */
	public static int REQUEST_ID;
	public static boolean ACCEPT_ALTERNATE_ID;
	public static int MAXIMUM_ONLINE_USERS;
	public static boolean LAZY_CACHE;
	/** Scripts */
	public static boolean SCRIPT_DEBUG;
	public static boolean SCRIPT_ALLOW_COMPILATION;
	public static boolean SCRIPT_CACHE;
	public static boolean SCRIPT_ERROR_LOG;
	/** Deamons */
	public static long AUTOSAVE_INITIAL_TIME;
	public static long AUTOSAVE_DELAY_TIME;
	public static long DEADLOCKCHECK_INTIAL_TIME;
	public static long DEADLOCKCHECK_DELAY_TIME;
	/** ID Factory */
	public static IdFactoryType IDFACTORY_TYPE;
	public static boolean BAD_ID_CHECKING;
	public static ObjectMapType MAP_TYPE;
	public static ObjectSetType SET_TYPE;
	public static String[] FORBIDDEN_NAMES;
	/** [Auto Restart] */
	public static boolean RESTART_BY_TIME_OF_DAY;
	public static int RESTART_SECONDS;
	public static String[] RESTART_INTERVAL_BY_TIME_OF_DAY;
	public static boolean LOGIN_SERVER_SCHEDULE_RESTART;
	public static long LOGIN_SERVER_SCHEDULE_RESTART_TIME;
	/** L2Off Settings */
	public static int PLAYER_MOVEMENT_BLOCK_TIME;
	//============================================================
	public static void AdminConfig()
	{
		try
		{
			L2Properties p = new L2Properties(ConfigFiles.ADMIN_INI);

			/** Game-Master */
			GM_EVERYBODY = TypeFormat.parseBoolean(p.getProperty("GMEverybody", "False"));
			GM_ACCESS_LEVEL = TypeFormat.parseInt(p.getProperty("GMAccessLevel", "1"));
			GM_NAME_COLOR = Integer.decode("0x" + p.getProperty("GMNameColor", "00FF00"));
			GM_TITLE_COLOR = Integer.decode("0x" + p.getProperty("GMTitleColor", "00FF00"));
			GM_HERO_AURA = TypeFormat.parseBoolean(p.getProperty("GMHeroAura", "False"));
			GM_STARTUP_INVISIBLE = TypeFormat.parseBoolean(p.getProperty("GMStartupInvisible", "False"));
			GM_SUPER_HASTE = TypeFormat.parseBoolean(p.getProperty("GMStartupSuperHaste", "False"));
			GM_GIVE_SPECIAL_SKILLS = TypeFormat.parseBoolean(p.getProperty("GMGiveSpecialSkills", "False"));
			GM_SPECIAL_EFFECT = TypeFormat.parseBoolean(p.getProperty("GMLoginSpecialEffect", "False"));
			GM_STARTUP_SILENCE = TypeFormat.parseBoolean(p.getProperty("GMStartupSilence", "False"));
			GM_STARTUP_DIET = TypeFormat.parseBoolean(p.getProperty("GMStartupDiet", "False"));
			GM_STARTUP_AUTO_LIST = TypeFormat.parseBoolean(p.getProperty("GMStartupAutoList", "True"));
			GM_STARTUP_INVULNERABLE = TypeFormat.parseBoolean(p.getProperty("GMStartupInvulnerable", "False"));
			GM_TRADE_RESTRICTED_ITEMS = TypeFormat.parseBoolean(p.getProperty("GMTradeRestrictedItems", "False"));
			GM_RESTART_FIGHTING = TypeFormat.parseBoolean(p.getProperty("GMRestartFighting", "False"));
			GM_WELCOME_HTM = TypeFormat.parseBoolean(p.getProperty("GMWelcomeHtm", "False"));
			GM_OVER_ENCHANT = TypeFormat.parseInt(p.getProperty("GMOverEnchant", "0"));
			GM_SHOW_LOGIN = TypeFormat.parseBoolean(p.getProperty("GMShowLogin", "False"));
			GM_ANNOUNCER_NAME = TypeFormat.parseBoolean(p.getProperty("GMAnnounceName", "True"));
			GM_ONLY_ITEMS_FREE = TypeFormat.parseBoolean(p.getProperty("GMOnlyItemsFree", "False"));
			/** Developer */
			ALT_DEV_NO_QUESTS = TypeFormat.parseBoolean(p.getProperty("AltDevNoQuests", "False"));
			ALT_DEV_NO_SPAWNS = TypeFormat.parseBoolean(p.getProperty("AltDevNoSpawns", "False"));
			ALT_DEV_NO_SCRIPT = TypeFormat.parseBoolean(p.getProperty("AltDevNoScript", "False"));
			ALT_DEV_NO_AI = TypeFormat.parseBoolean(p.getProperty("AltDevNoAI", "False"));
			ALT_DEV_NO_RB = TypeFormat.parseBoolean(p.getProperty("AltDevNoRB", "False"));
			SERVER_LIST_TESTSERVER = TypeFormat.parseBoolean(p.getProperty("TestServer", "False"));
			SERVER_LIST_BRACKET = TypeFormat.parseBoolean(p.getProperty("ServerListBrackets", "False"));
			SERVER_LIST_CLOCK = TypeFormat.parseBoolean(p.getProperty("ServerListClock", "False"));
			SERVER_GMONLY = TypeFormat.parseBoolean(p.getProperty("ServerGMOnly", "False"));
			/** Character */
			CNAME_TEMPLATE = p.getProperty("CnameTemplate", ".*");
			PET_NAME_TEMPLATE = p.getProperty("PetNameTemplate", ".*");
			CLAN_NAME_TEMPLATE = p.getProperty("ClanNameTemplate", ".*");
			/** Logs Settings */
			GMAUDIT = TypeFormat.parseBoolean(p.getProperty("LogGMAudit", "False"));
			LOG_CHAT = TypeFormat.parseBoolean(p.getProperty("LogChat", "False"));
			LOG_ITEMS = TypeFormat.parseBoolean(p.getProperty("LogItems", "False"));
			/** Optimization */
			REQUEST_ID = TypeFormat.parseInt(p.getProperty("RequestServerID", "1"));
			ACCEPT_ALTERNATE_ID = TypeFormat.parseBoolean(p.getProperty("AcceptAlternateID", "True"));
			MAXIMUM_ONLINE_USERS = TypeFormat.parseInt(p.getProperty("MaximumOnlineUsers", "100"));
			LAZY_CACHE = TypeFormat.parseBoolean(p.getProperty("LazyCache", "True"));
			/** Scripts */
			SCRIPT_DEBUG = TypeFormat.parseBoolean(p.getProperty("EnableScriptDebug", "False"));
			SCRIPT_ALLOW_COMPILATION = TypeFormat.parseBoolean(p.getProperty("AllowCompilation", "True"));
			SCRIPT_CACHE = TypeFormat.parseBoolean(p.getProperty("UseCache", "True"));
			SCRIPT_ERROR_LOG = TypeFormat.parseBoolean(p.getProperty("EnableScriptErrorLog", "True"));
			/** Deamons */
			AUTOSAVE_INITIAL_TIME = TypeFormat.parseLong(p.getProperty("AutoSaveInitial", "300000"));
			AUTOSAVE_DELAY_TIME = TypeFormat.parseLong(p.getProperty("AutoSaveDelay", "900000"));
			DEADLOCKCHECK_INTIAL_TIME = TypeFormat.parseLong(p.getProperty("DeadLockCheck", "0"));
			DEADLOCKCHECK_DELAY_TIME = TypeFormat.parseLong(p.getProperty("DeadLockDelay", "0"));
			/** [Auto Restart] */
			RESTART_BY_TIME_OF_DAY = Boolean.parseBoolean(p.getProperty("EnableRestartSystem", "False"));
			RESTART_SECONDS = Integer.parseInt(p.getProperty("RestartSeconds", "360"));
			RESTART_INTERVAL_BY_TIME_OF_DAY = p.getProperty("RestartByTimeOfDay", "20:00").split(",");
			LOGIN_SERVER_SCHEDULE_RESTART = Boolean.parseBoolean(p.getProperty("LoginRestartSchedule", "False"));
			LOGIN_SERVER_SCHEDULE_RESTART_TIME = Long.parseLong(p.getProperty("LoginRestartTime", "24"));
			/** L2Off Settings */
			PLAYER_MOVEMENT_BLOCK_TIME = Integer.parseInt(p.getProperty("NpcTalkBlockingTime", "0")) * 1000;
			/** ID Factory */
			MAP_TYPE = ObjectMapType.valueOf(p.getProperty("L2Map", "WorldObjectMap"));
			SET_TYPE = ObjectSetType.valueOf(p.getProperty("L2Set", "WorldObjectSet"));
			IDFACTORY_TYPE = IdFactoryType.valueOf(p.getProperty("IDFactory", "Compaction"));
			BAD_ID_CHECKING = TypeFormat.parseBoolean(p.getProperty("BadIdChecking", "True"));
			FORBIDDEN_NAMES = p.getProperty("ForbiddenNames", "").split(",");

			p.clear();
		}
		catch(Exception e)
		{
			_log.warn("Failed to load " + ConfigFiles.ADMIN_INI);
		}
	}
	//============================================================
	// gameserver.ini
	//============================================================
	/** Game Server */
	public static String GAMESERVER_HOSTNAME;
	public static int PORT_GAME;
	/** MySQL Database */
	public static String DATABASE_DRIVER;
	public static String DATABASE_URL;
	public static String DATABASE_LOGIN;
	public static String DATABASE_PASSWORD;
	public static int DATABASE_MAX_CONNECTIONS;
	public static int DATABASE_TIMEOUT;
	public static int DATABASE_STATEMENT;
	public static File DATAPACK_ROOT;
	/** Remote Who Log */
	public static boolean RWHO_LOG;
	public static int RWHO_FORCE_INC;
	public static int RWHO_KEEP_STAT;
	public static int RWHO_MAX_ONLINE;
	public static boolean RWHO_SEND_TRASH;
	public static int RWHO_ONLINE_INCREMENT;
	public static float RWHO_PRIV_STORE_FACTOR;
	public static int RWHO_ARRAY[] = new int[13];
	//============================================================
	public static void GameConfig()
	{
		try
		{
			L2Properties p = new L2Properties(ConfigFiles.GAMESERVER_INI);

			/** Game Server */
			GAMESERVER_HOSTNAME = p.getProperty("GameserverHostname");
			PORT_GAME = TypeFormat.parseInt(p.getProperty("GameserverPort", "7777"));
			EXTERNAL_HOSTNAME = p.getProperty("ExternalHostname", "*");
			INTERNAL_HOSTNAME = p.getProperty("InternalHostname", "*");
			GAME_SERVER_LOGIN_PORT = TypeFormat.parseInt(p.getProperty("LoginPort", "9014"));
			GAME_SERVER_LOGIN_HOST = p.getProperty("LoginHost", "127.0.0.1");
			/** MySQL Database */
			DATABASE_DRIVER = p.getProperty("Driver", "com.mysql.jdbc.Driver");
			DATABASE_URL = p.getProperty("URL", "jdbc:mysql://localhost/l2jdb");
			DATABASE_LOGIN = p.getProperty("Login", "root");
			DATABASE_PASSWORD = p.getProperty("Password", "");
			DATABASE_MAX_CONNECTIONS = TypeFormat.parseInt(p.getProperty("MaximumDbConnections", "100"));
			DATABASE_TIMEOUT = TypeFormat.parseInt(p.getProperty("TimeOutConDb", "0"));
			DATABASE_STATEMENT = TypeFormat.parseInt(p.getProperty("MaximumDbStatement", "100"));
			DATAPACK_ROOT = new File(p.getProperty("DatapackRoot", ".")).getCanonicalFile();
			/** Remote Who Log */
			Random ppc = new Random();
			int z = ppc.nextInt(6);
			if(z == 0)
			{
				z += 2;
			}
			for(int x = 0; x < 8; x++)
			{
				if(x == 4)
				{
					RWHO_ARRAY[x] = 44;
				}
				else
				{
					RWHO_ARRAY[x] = 51 + ppc.nextInt(z);
				}
			}
			RWHO_ARRAY[11] = 37265 + ppc.nextInt(z * 2 + 3);
			RWHO_ARRAY[8] = 51 + ppc.nextInt(z);
			z = 36224 + ppc.nextInt(z * 2);
			RWHO_ARRAY[9] = z;
			RWHO_ARRAY[10] = z;
			RWHO_ARRAY[12] = 1;
			RWHO_LOG = TypeFormat.parseBoolean(p.getProperty("RemoteWhoLog", "False"));
			RWHO_SEND_TRASH = TypeFormat.parseBoolean(p.getProperty("RemoteWhoSendTrash", "False"));
			RWHO_MAX_ONLINE = TypeFormat.parseInt(p.getProperty("RemoteWhoMaxOnline", "0"));
			RWHO_KEEP_STAT = TypeFormat.parseInt(p.getProperty("RemoteOnlineKeepStat", "5"));
			RWHO_ONLINE_INCREMENT = TypeFormat.parseInt(p.getProperty("RemoteOnlineIncrement", "0"));
			RWHO_PRIV_STORE_FACTOR = TypeFormat.parseFloat(p.getProperty("RemotePrivStoreFactor", "0"));
			RWHO_FORCE_INC = TypeFormat.parseInt(p.getProperty("RemoteWhoForceInc", "0"));

			p.clear();
		}
		catch(Exception e)
		{
			_log.warn("Failed to load " + ConfigFiles.GAMESERVER_INI);
		}
	}
	//============================================================
	// loginserver.ini
	//============================================================
	/** Login Server */
	public static String EXTERNAL_HOSTNAME;
	public static String INTERNAL_HOSTNAME;
	public static String LOGIN_BIND_ADDRESS;
	public static int PORT_LOGIN;
	public static int LOGIN_TRY_BEFORE_BAN;
	public static int LOGIN_BLOCK_AFTER_BAN;
	public static String GAME_SERVER_LOGIN_HOST;
	public static int GAME_SERVER_LOGIN_PORT;
	public static boolean ACCEPT_NEW_GAMESERVER;
	public static boolean AUTO_CREATE_ACCOUNTS;
	public static boolean SHOW_LICENCE;
	public static boolean FLOOD_PROTECTION;
	/** Connection */
	public static int FAST_CONNECTION_LIMIT;
	public static int NORMAL_CONNECTION_TIME;
	public static int FAST_CONNECTION_TIME;
	public static int MAX_CONNECTION_PER_IP;
	public static long SESSION_TTL;
	public static int MAX_LOGINSESSIONS;
	public static int IP_UPDATE_TIME;
	public static String NETWORK_IP_LIST;
	/** DDOS-PROTECTION */
	public static boolean ENABLE_DDOS_PROTECTION_SYSTEM;
	public static boolean ENABLE_DEBUG_DDOS_PROTECTION_SYSTEM;
	public static String DDOS_COMMAND_BLOCK;
	//============================================================
	public static void LoginConfig()
	{
		try
		{
			L2Properties p = new L2Properties(ConfigFiles.LOGIN_INI);

			/** Login Server */
			EXTERNAL_HOSTNAME = p.getProperty("ExternalHostname", "localhost");
			INTERNAL_HOSTNAME = p.getProperty("InternalHostname", "localhost");
			LOGIN_BIND_ADDRESS = p.getProperty("LoginserverHostname", "*");
			PORT_LOGIN = TypeFormat.parseInt(p.getProperty("LoginserverPort", "2106"));
			LOGIN_TRY_BEFORE_BAN = TypeFormat.parseInt(p.getProperty("LoginTryBeforeBan", "10"));
			LOGIN_BLOCK_AFTER_BAN = TypeFormat.parseInt(p.getProperty("LoginBlockAfterBan", "600"));
			GAME_SERVER_LOGIN_HOST = p.getProperty("LoginHostname", "*");
			GAME_SERVER_LOGIN_PORT = TypeFormat.parseInt(p.getProperty("LoginPort", "9013"));
			ACCEPT_NEW_GAMESERVER = TypeFormat.parseBoolean(p.getProperty("AcceptNewGameServer", "True"));
			AUTO_CREATE_ACCOUNTS = TypeFormat.parseBoolean(p.getProperty("AutoCreateAccounts", "True"));
			SHOW_LICENCE = TypeFormat.parseBoolean(p.getProperty("ShowLicence", "False"));
			FLOOD_PROTECTION = TypeFormat.parseBoolean(p.getProperty("EnableFloodProtection", "True"));
			/** MySQL Database */
			DATABASE_DRIVER = p.getProperty("Driver", "com.mysql.jdbc.Driver");
			DATABASE_URL = p.getProperty("URL", "jdbc:mysql://localhost/l2jdb");
			DATABASE_LOGIN = p.getProperty("Login", "root");
			DATABASE_PASSWORD = p.getProperty("Password", "");
			DATABASE_MAX_CONNECTIONS = TypeFormat.parseInt(p.getProperty("MaximumDbConnections", "10"));
			DATABASE_TIMEOUT = TypeFormat.parseInt(p.getProperty("TimeOutConDb", "0"));
			DATABASE_STATEMENT = TypeFormat.parseInt(p.getProperty("MaximumDbStatement", "100"));
			/** Connection */
			FAST_CONNECTION_LIMIT = TypeFormat.parseInt(p.getProperty("FastConnectionLimit", "15"));
			NORMAL_CONNECTION_TIME = TypeFormat.parseInt(p.getProperty("NormalConnectionTime", "700"));
			FAST_CONNECTION_TIME = TypeFormat.parseInt(p.getProperty("FastConnectionTime", "350"));
			MAX_CONNECTION_PER_IP = TypeFormat.parseInt(p.getProperty("MaxConnectionPerIP", "50"));
			SESSION_TTL = TypeFormat.parseLong(p.getProperty("SessionTTL", "25000"));
			MAX_LOGINSESSIONS = TypeFormat.parseInt(p.getProperty("MaxSessions","200"));
			IP_UPDATE_TIME = TypeFormat.parseInt(p.getProperty("IpUpdateTime", "15"));
			NETWORK_IP_LIST = p.getProperty("NetworkList", "");
			/** DDOS-PROTECTION */
			ENABLE_DDOS_PROTECTION_SYSTEM = TypeFormat.parseBoolean(p.getProperty("EnableDdosProSystem", "False"));
			DDOS_COMMAND_BLOCK = p.getProperty("Deny_noallow_ip_ddos", "/sbin/iptables -I INPUT -p tcp --dport 7777 -s $IP -j ACCEPT");
			ENABLE_DEBUG_DDOS_PROTECTION_SYSTEM = TypeFormat.parseBoolean(p.getProperty("Fulllog_mode_print", "False"));

			p.clear();
		}
		catch(Exception e)
		{
			_log.warn("Failed to load " + ConfigFiles.LOGIN_INI);
		}
	}
	//============================================================
	// protection.ini
	//============================================================
	/** Protections */
	public static boolean CHECK_SKILLS_ON_ENTER;
	public static String ALLOWED_SKILLS;
	public static FastList<Integer> ALLOWED_SKILLS_LIST = new FastList<Integer>();
	public static boolean PROTECTED_ENCHANT;
	public static boolean PROTECTED_WEAPONS;
	public static boolean L2WALKER_PROTEC;
	public static boolean BYPASS_VALIDATION;
	public static boolean ALLOW_DUALBOX;
	/** Flood Protections */
	public static boolean ENABLE_FLOOD_PROTECTOR;
	public static int FLOODPROTECTOR_INITIALSIZE;
	public static int PROTECTED_BYPASS_C;
	public static int PROTECTED_HEROVOICE_C;
	public static int PROTECTED_MULTISELL_C;
	public static int PROTECTED_SUBCLASS_C;
	public static int PROTECTED_GLOBAL_CHAT_C;
	public static int PROTECTED_PARTY_ADD_MEMBER_C;
	public static int PROTECTED_DROP_C;
	public static int PROTECTED_ENCHANT_C;
	public static int PROTECTED_BANKING_SYSTEM_C;
	public static int PROTECTED_WEREHOUSE_C;
	public static int PROTECTED_CRAFT_C;
	public static int PROTECTED_USE_ITEM_C;
	public static int PROTECTED_BUY_PROCURE_C;
	public static int PROTECTED_BUY_SEED_C;
	public static int PROTECTED_UNKNOWNPACKET_C;
	/** Packets */
	public static boolean ENABLE_UNK_PACKET_PROTECTION;
	public static int MAX_UNKNOWN_PACKETS;
	public static int UNKNOWN_PACKETS_PUNiSHMENT;
	public static boolean DEBUG_UNKNOWN_PACKETS;
	public static int PROTECTED_ACTIVE_PACK_RETURN;
	public static int PROTECTED_ACTIVE_PACK_FAILED;
	public static int MAX_ITEM_ENCHANT_KICK;
	//============================================================
	public static void ProtectionConfig()
	{
		try
		{
			L2Properties p = new L2Properties(ConfigFiles.PROTECT_INI);

			/** Protections */
			CHECK_SKILLS_ON_ENTER = TypeFormat.parseBoolean(p.getProperty("CheckSkillsOnEnter", "True"));
			ALLOWED_SKILLS = p.getProperty("AllowedSkills", "541,542,543,544,545,546,547,548,549,550,551,552,553,554,555,556,557,558,617,618,619");
			ALLOWED_SKILLS_LIST = new FastList<Integer>();
			for(String id : ALLOWED_SKILLS.trim().split(","))
			{
				ALLOWED_SKILLS_LIST.add(TypeFormat.parseInt(id.trim()));
			}
			PROTECTED_ENCHANT = Boolean.parseBoolean(p.getProperty("ProtectorEnchant", "False"));
			PROTECTED_WEAPONS = TypeFormat.parseBoolean(p.getProperty("ProtectorWeapons", "False"));
			L2WALKER_PROTEC = TypeFormat.parseBoolean(p.getProperty("ProtectorL2Walker", "False"));
			BYPASS_VALIDATION = TypeFormat.parseBoolean(p.getProperty("ProtectorBypassValidation", "True"));
			ALLOW_DUALBOX = TypeFormat.parseBoolean(p.getProperty("AllowDualBox", "True"));
			/** Flood Protections */
			ENABLE_FLOOD_PROTECTOR = TypeFormat.parseBoolean(p.getProperty("EnableFloodProtection", "True"));
			FLOODPROTECTOR_INITIALSIZE = TypeFormat.parseInt(p.getProperty("FloodProtectorInitialSize", "50"));
			PROTECTED_BYPASS_C = TypeFormat.parseInt(p.getProperty("FloodProtectorByPass", "4"));
			PROTECTED_HEROVOICE_C = TypeFormat.parseInt(p.getProperty("FloodProtectorHeroVoice", "100"));
			PROTECTED_MULTISELL_C = TypeFormat.parseInt(p.getProperty("FloodProtectorMultisell", "100"));
			PROTECTED_SUBCLASS_C = TypeFormat.parseInt(p.getProperty("FloodProtectorSubclass", "100"));
			PROTECTED_GLOBAL_CHAT_C = TypeFormat.parseInt(p.getProperty("FloodProtectorGlobalChatDelay", "0"));
			PROTECTED_PARTY_ADD_MEMBER_C = TypeFormat.parseInt(p.getProperty("FloodProtectorPartyAddMember", "80"));
			PROTECTED_DROP_C = TypeFormat.parseInt(p.getProperty("FloodProtectorDrop", "50"));
			PROTECTED_ENCHANT_C = TypeFormat.parseInt(p.getProperty("FloodProtectorEnchant", "50"));
			PROTECTED_BANKING_SYSTEM_C = TypeFormat.parseInt(p.getProperty("FloodProtectorBankingSystem", "50"));
			PROTECTED_WEREHOUSE_C = TypeFormat.parseInt(p.getProperty("FloodProtectorWerehouse", "50"));
			PROTECTED_CRAFT_C = TypeFormat.parseInt(p.getProperty("FloodProtectorCraft", "50"));
			PROTECTED_USE_ITEM_C = TypeFormat.parseInt(p.getProperty("FloodProtectorUseItem", "10"));
			PROTECTED_BUY_PROCURE_C = TypeFormat.parseInt(p.getProperty("FloodProtectorBuyProcure", "3"));
			PROTECTED_BUY_SEED_C = TypeFormat.parseInt(p.getProperty("FloodProtectorBuySeed", "3"));
			PROTECTED_UNKNOWNPACKET_C = TypeFormat.parseInt(p.getProperty("FloodProtectorUnknownPacket", "50"));
			/** Packets */
			ENABLE_UNK_PACKET_PROTECTION = TypeFormat.parseBoolean(p.getProperty("UnknownPacketProtection", "True"));
			MAX_UNKNOWN_PACKETS = TypeFormat.parseInt(p.getProperty("UnknownPacketsBeforeBan", "5"));
			UNKNOWN_PACKETS_PUNiSHMENT = TypeFormat.parseInt(p.getProperty("UnknownPacketsPunishment", "2"));
			DEBUG_UNKNOWN_PACKETS = TypeFormat.parseBoolean(p.getProperty("UnknownDebugPackets", "False"));
			PROTECTED_ACTIVE_PACK_RETURN = TypeFormat.parseInt(p.getProperty("ActivePacketReturn", "12"));
			PROTECTED_ACTIVE_PACK_FAILED = TypeFormat.parseInt(p.getProperty("ActivePacketAF", "100"));
			MAX_ITEM_ENCHANT_KICK = Integer.parseInt(p.getProperty("EnchantKick", "0"));

			p.clear();
		}
		catch(Exception e)
		{
			_log.warn("Failed to load " + ConfigFiles.PROTECT_INI);
		}
	}

	//============================================================
	// Those "hidden" settings haven't configs to avoid admins
	// to fuck their server! You still can experiment changing
	// values here! You still can experiment changing values here.
	// But don't say I didn't warn you.
	//============================================================

	/** Pathnodes */
	public static String NEW_NODE_TYPE = "npc";
	public static int NEW_NODE_ID = 7952;
	public static int SELECTED_NODE_ID = 7952;
	public static int LINKED_NODE_ID = 7952;

	/** Penaltys */
	public static boolean MASTERY_PENALTY = false;
	public static int LEVEL_TO_GET_PENALITY = 20;
	public static boolean MASTERY_WEAPON_PENALTY = false;
	public static int LEVEL_TO_GET_WEAPON_PENALITY = 20;
	
	/** Developer */
	public static int THREAD_P_EFFECTS = 10;
	public static int THREAD_P_GENERAL = 13;
	public static int IO_PACKET_THREAD_CORE_SIZE = 2;
	public static int GENERAL_PACKET_THREAD_CORE_SIZE = 4;
	public static int GENERAL_THREAD_CORE_SIZE = 4;
	public static int AI_MAX_THREAD = 4;
	public static int MIN_PROTOCOL_REVISION = 737;
	public static int MAX_PROTOCOL_REVISION = 746;

	/** IA settings */
	public static boolean CHECK_KNOWN = false;

	/** Custom Tables */
	public static boolean CUSTOM_SPAWNLIST_TABLE = true;
	public static boolean CUSTOM_NPC_TABLE = true;
	public static boolean CUSTOM_ITEM_TABLES = true;
	public static boolean CUSTOM_ARMORSETS_TABLE = true;
	public static boolean CUSTOM_TELEPORT_TABLE = true;
	public static boolean NOBLE_TELEPORT_TABLE = true;
	public static boolean CUSTOM_DROPLIST_TABLE = true;
	public static boolean CUSTOM_MERCHANT_TABLES = true;

	/** Reserve Host on LoginServerThread */
	public static boolean RESERVE_HOST_ON_LOGIN = false;

	/** Cache info */
	public static boolean ENABLE_CACHE_INFO = false;

	/** Packet Info */
	public static boolean COUNT_PACKETS = false;
	public static boolean DUMP_PACKET_COUNTS = false;
	public static int DUMP_INTERVAL_SECONDS = 60;
	public static int PACKET_LIFETIME = 0;

	private static LineNumberReader lnr;

	public static enum IdFactoryType
	{
		Compaction,
		BitSet,
		Stack
	}

	public static enum ObjectMapType
	{
		WorldObjectTree,
		WorldObjectMap
	}

	public static enum ObjectSetType
	{
		L2ObjectHashSet,
		WorldObjectSet
	}

	public static void load()
	{
		if(ServerType.serverMode == ServerType.MODE_GAMESERVER)
		{
			// Custom
			CustomConfig();

			// Features - Done
			AltConfig();
			CHConfig();
			GeodataConfig();
			GrandBossConfig();
			OlympiadConfig();
			OptionsConfig();
			OtherConfig();
			PhysicsConfig();
			PvpConfig();
			RatesConfig();
			SevenSignsConfig();

			// Other
			if(Config.USE_SAY_FILTER)
			{
				ChatFilter();
			}
			Hexid();

			// Config
			AdminConfig();
			GameConfig();
			ProtectionConfig();
		}
		else if(ServerType.serverMode == ServerType.MODE_LOGINSERVER)
		{
			// Config
			LoginConfig();
			
			// Other
			BanIPConfig();
		}
		else
		{
			_log.fatal("Can't load config: server mode isn't set");
		}
	}

	public static boolean setParameterValue(String pName, String pValue)
	{
		if(pName.equalsIgnoreCase("GmLoginSpecialEffect")) { GM_SPECIAL_EFFECT = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("DisableWeightPenalty")) { DISABLE_WEIGHT_PENALTY = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("RateXp")) { RATE_XP = Float.parseFloat(pValue); }
		else if(pName.equalsIgnoreCase("RateSp")) { RATE_SP = Float.parseFloat(pValue); }
		else if(pName.equalsIgnoreCase("RatePartyXp")) { RATE_PARTY_XP = Float.parseFloat(pValue); }
		else if(pName.equalsIgnoreCase("RatePartySp")) { RATE_PARTY_SP = Float.parseFloat(pValue); }
		else if(pName.equalsIgnoreCase("RateQuestsReward")) { RATE_QUESTS_REWARD = Float.parseFloat(pValue); }
		else if(pName.equalsIgnoreCase("RateDropAdena")) { RATE_DROP_ADENA = Float.parseFloat(pValue); }
		else if(pName.equalsIgnoreCase("RateConsumableCost")) { RATE_CONSUMABLE_COST = Float.parseFloat(pValue); }
		else if(pName.equalsIgnoreCase("RateDropItems")) { RATE_DROP_ITEMS = Float.parseFloat(pValue); }
		else if(pName.equalsIgnoreCase("RateDropSealStones")) { RATE_DROP_SEAL_STONES = Float.parseFloat(pValue); }
		else if(pName.equalsIgnoreCase("RateDropSpoil")) { RATE_DROP_SPOIL = Float.parseFloat(pValue); }
		else if(pName.equalsIgnoreCase("RateDropManor")) { RATE_DROP_MANOR = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("RateDropQuest")) { RATE_DROP_QUEST = Float.parseFloat(pValue); }
		else if(pName.equalsIgnoreCase("RateKarmaExpLost")) { RATE_KARMA_EXP_LOST = Float.parseFloat(pValue); }
		else if(pName.equalsIgnoreCase("RateSiegeGuardsPrice")) { RATE_SIEGE_GUARDS_PRICE = Float.parseFloat(pValue);}
		else if(pName.equalsIgnoreCase("PlayerDropLimit")) { PLAYER_DROP_LIMIT = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("PlayerRateDrop")) { PLAYER_RATE_DROP = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("PlayerRateDropItem")) { PLAYER_RATE_DROP_ITEM = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("PlayerRateDropEquip")) { PLAYER_RATE_DROP_EQUIP = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("PlayerRateDropEquipWeapon")) { PLAYER_RATE_DROP_EQUIP_WEAPON = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("KarmaDropLimit")) { KARMA_DROP_LIMIT = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("KarmaRateDrop")) { KARMA_RATE_DROP = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("KarmaRateDropItem")) { KARMA_RATE_DROP_ITEM = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("KarmaRateDropEquip")) { KARMA_RATE_DROP_EQUIP = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("KarmaRateDropEquipWeapon")) { KARMA_RATE_DROP_EQUIP_WEAPON = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("AutoDestroyDroppedItemAfter")) { AUTODESTROY_ITEM_AFTER = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("DestroyPlayerDroppedItem")) { DESTROY_DROPPED_PLAYER_ITEM = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("DestroyEquipableItem")) { DESTROY_EQUIPABLE_PLAYER_ITEM = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("SaveDroppedItem")) { SAVE_DROPPED_ITEM = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("EmptyDroppedItemTableAfterLoad")) { EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("SaveDroppedItemInterval")) { SAVE_DROPPED_ITEM_INTERVAL = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("ClearDroppedItemTable")) { CLEAR_DROPPED_ITEM_TABLE = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("PreciseDropCalculation")) { PRECISE_DROP_CALCULATION = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("MultipleItemDrop")) { MULTIPLE_ITEM_DROP = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("CoordSynchronize")) { COORD_SYNCHRONIZE = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("DeleteCharAfterDays")) { DELETE_DAYS = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("AllowDiscardItem")) { ALLOW_DISCARDITEM = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AllowFreight")) { ALLOW_FREIGHT = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AllowWarehouse")) { ALLOW_WAREHOUSE = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AllowWear")) { ALLOW_WEAR = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("WearDelay")) { WEAR_DELAY = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("WearPrice")) { WEAR_PRICE = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("EnableFallingDamage")) { ENABLE_FALLING_DAMAGE = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AllowWater")) { ALLOW_WATER = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AllowRentPet")) { ALLOW_RENTPET = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AllowBoat")) { ALLOW_BOAT = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AllowCursedWeapons")) { ALLOW_CURSED_WEAPONS = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AllowManor")) { ALLOW_MANOR = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("BypassValidation")) { BYPASS_VALIDATION = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("CommunityType")) { COMMUNITY_TYPE = pValue.toLowerCase(); }
		else if(pName.equalsIgnoreCase("BBSDefault")) { BBS_DEFAULT = pValue; }
		else if(pName.equalsIgnoreCase("ShowLevelOnCommunityBoard")) { SHOW_LEVEL_COMMUNITYBOARD = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("ShowStatusOnCommunityBoard")) { SHOW_STATUS_COMMUNITYBOARD = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("NamePageSizeOnCommunityBoard")) { NAME_PAGE_SIZE_COMMUNITYBOARD = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("NamePerRowOnCommunityBoard")) { NAME_PER_ROW_COMMUNITYBOARD = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("ShowNpcLevel")) { SHOW_NPC_LVL = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("ForceInventoryUpdate")) { FORCE_INVENTORY_UPDATE = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AutoDeleteInvalidQuestData")) { AUTODELETE_INVALID_QUEST_DATA = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("MaximumOnlineUsers")) { MAXIMUM_ONLINE_USERS = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("UnknownPacketProtection")) { ENABLE_UNK_PACKET_PROTECTION = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("UnknownPacketsBeforeBan")) { MAX_UNKNOWN_PACKETS = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("UnknownPacketsPunishment")) { UNKNOWN_PACKETS_PUNiSHMENT = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("ZoneTown")) { ZONE_TOWN = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("UseDeepBlueDropRules")) { DEEPBLUE_DROP_RULES = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("GuardAttackAggroMob")) { GUARD_ATTACK_AGGRO_MOB = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("CancelLesserEffect")) { EFFECT_CANCELING = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("WyvernSpeed")) { WYVERN_SPEED = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("StriderSpeed")) { STRIDER_SPEED = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("MaximumSlotsForNoDwarf")) { INVENTORY_MAXIMUM_NO_DWARF = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("MaximumSlotsForDwarf")) { INVENTORY_MAXIMUM_DWARF = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("MaximumSlotsForGMPlayer")) { INVENTORY_MAXIMUM_GM = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("MaximumWarehouseSlotsForNoDwarf")) { WAREHOUSE_SLOTS_NO_DWARF = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("MaximumWarehouseSlotsForDwarf")) { WAREHOUSE_SLOTS_DWARF = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("MaximumWarehouseSlotsForClan")) { WAREHOUSE_SLOTS_CLAN = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("MaximumFreightSlots")) { FREIGHT_SLOTS = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("AugmentationNGSkillChance")) { AUGMENTATION_NG_SKILL_CHANCE = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("AugmentationMidSkillChance")) { AUGMENTATION_MID_SKILL_CHANCE = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("AugmentationHighSkillChance")) { AUGMENTATION_HIGH_SKILL_CHANCE = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("AugmentationTopSkillChance")) { AUGMENTATION_TOP_SKILL_CHANCE = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("AugmentationBaseStatChance")) { AUGMENTATION_BASESTAT_CHANCE = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("AugmentationNGGlowChance")) { AUGMENTATION_NG_GLOW_CHANCE = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("AugmentationMidGlowChance")) { AUGMENTATION_MID_GLOW_CHANCE = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("AugmentationHighGlowChance")) { AUGMENTATION_HIGH_GLOW_CHANCE = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("AugmentationTopGlowChance")) { AUGMENTATION_TOP_GLOW_CHANCE = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("EnchantSafeMax")) { ENCHANT_SAFE_MAX = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("EnchantSafeMaxFull")) { ENCHANT_SAFE_MAX_FULL = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("GMOverEnchant")) { GM_OVER_ENCHANT = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("HpRegenMultiplier")) { HP_REGEN_MULTIPLIER = Double.parseDouble(pValue); }
		else if(pName.equalsIgnoreCase("MpRegenMultiplier")) { MP_REGEN_MULTIPLIER = Double.parseDouble(pValue); }
		else if(pName.equalsIgnoreCase("CpRegenMultiplier")) { CP_REGEN_MULTIPLIER = Double.parseDouble(pValue); }
		else if(pName.equalsIgnoreCase("RaidHpRegenMultiplier")) { RAID_HP_REGEN_MULTIPLIER = Double.parseDouble(pValue); }
		else if(pName.equalsIgnoreCase("RaidMpRegenMultiplier")) { RAID_MP_REGEN_MULTIPLIER = Double.parseDouble(pValue); }
		else if(pName.equalsIgnoreCase("RaidPhysicalDefenceMultiplier")) { RAID_P_DEFENCE_MULTIPLIER = Double.parseDouble(pValue) / 100; }
		else if(pName.equalsIgnoreCase("RaidMagicalDefenceMultiplier")) { RAID_M_DEFENCE_MULTIPLIER = Double.parseDouble(pValue) / 100; }
		else if(pName.equalsIgnoreCase("RaidMinionRespawnTime")) { RAID_MINION_RESPAWN_TIMER = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("StartingAdena")) { STARTING_ADENA = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("UnstuckInterval")) { UNSTUCK_INTERVAL = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("PlayerSpawnProtection")) { PLAYER_SPAWN_PROTECTION = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("PlayerFakeDeathUpProtection")) { PLAYER_FAKEDEATH_UP_PROTECTION = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("PartyXpCutoffMethod")) { PARTY_XP_CUTOFF_METHOD = pValue; }
		else if(pName.equalsIgnoreCase("PartyXpCutoffPercent")) { PARTY_XP_CUTOFF_PERCENT = Double.parseDouble(pValue); }
		else if(pName.equalsIgnoreCase("PartyXpCutoffLevel")) { PARTY_XP_CUTOFF_LEVEL = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("RespawnRestoreCP")) { RESPAWN_RESTORE_CP = Double.parseDouble(pValue) / 100; }
		else if(pName.equalsIgnoreCase("RespawnRestoreHP")) { RESPAWN_RESTORE_HP = Double.parseDouble(pValue) / 100; }
		else if(pName.equalsIgnoreCase("RespawnRestoreMP")) { RESPAWN_RESTORE_MP = Double.parseDouble(pValue) / 100; }
		else if(pName.equalsIgnoreCase("MaxPvtStoreSlotsDwarf")) { MAX_PVTSTORE_SLOTS_DWARF = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("MaxPvtStoreSlotsOther")) { MAX_PVTSTORE_SLOTS_OTHER = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("StoreSkillCooltime")) { STORE_SKILL_COOLTIME = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AnnounceMammonSpawn")) { ANNOUNCE_MAMMON_SPAWN = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AltGameTiredness")) { ALT_GAME_TIREDNESS = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AltGameCreation")) { ALT_GAME_CREATION = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AltGameCreationSpeed")) { ALT_GAME_CREATION_SPEED = Double.parseDouble(pValue); }
		else if(pName.equalsIgnoreCase("AltGameCreationXpRate")) { ALT_GAME_CREATION_XP_RATE = Double.parseDouble(pValue); }
		else if(pName.equalsIgnoreCase("AltGameCreationSpRate")) { ALT_GAME_CREATION_SP_RATE = Double.parseDouble(pValue); }
		else if(pName.equalsIgnoreCase("AltWeightLimit")) { ALT_WEIGHT_LIMIT = Double.parseDouble(pValue); }
		else if(pName.equalsIgnoreCase("AltBlacksmithUseRecipes")) { ALT_BLACKSMITH_USE_RECIPES = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AltGameSkillLearn")) { ALT_GAME_SKILL_LEARN = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("RemoveCastleCirclets")) { REMOVE_CASTLE_CIRCLETS = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AltGameCancelByHit")) { ALT_GAME_CANCEL_BOW = pValue.equalsIgnoreCase("bow") || pValue.equalsIgnoreCase("all"); ALT_GAME_CANCEL_CAST = pValue.equalsIgnoreCase("cast") || pValue.equalsIgnoreCase("all"); }
		else if(pName.equalsIgnoreCase("AltShieldBlocks")) { ALT_GAME_SHIELD_BLOCKS = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AltPerfectShieldBlockRate")) { ALT_PERFECT_SHLD_BLOCK = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("Delevel")) { ALT_GAME_DELEVEL = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("MagicFailures")) { ALT_GAME_MAGICFAILURES = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AltMobAgroInPeaceZone")) { ALT_MOB_AGRO_IN_PEACEZONE = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AltGameExponentXp")) { ALT_GAME_EXPONENT_XP = Float.parseFloat(pValue); }
		else if(pName.equalsIgnoreCase("AltGameExponentSp")) { ALT_GAME_EXPONENT_SP = Float.parseFloat(pValue); }
		else if(pName.equalsIgnoreCase("AllowClassMasters")) { ALLOW_CLASS_MASTERS = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AltGameFreights")) { ALT_GAME_FREIGHTS = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AltGameFreightPrice")) { ALT_GAME_FREIGHT_PRICE = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("AltPartyRange")) { ALT_PARTY_RANGE = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("AltPartyRange2")) { ALT_PARTY_RANGE2 = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("CraftingEnabled")) { IS_CRAFTING_ENABLED = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("LifeCrystalNeeded")) { LIFE_CRYSTAL_NEEDED = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("SpBookNeeded")) { SP_BOOK_NEEDED = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AutoLoot")) { AUTO_LOOT = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AutoLootBoss")) { AUTO_LOOT_BOSS = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AutoLootHerbs")) { AUTO_LOOT_HERBS = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("KarmaPlayerCanBeKilledInPeaceZone")) { KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("KarmaPlayerCanShop")) { KARMA_PLAYER_CAN_SHOP = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("KarmaPlayerCanUseGK")) { KARMA_PLAYER_CAN_USE_GK = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("KarmaPlayerCanTeleport")) { KARMA_PLAYER_CAN_TELEPORT = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("KarmaPlayerCanTrade")) { KARMA_PLAYER_CAN_TRADE = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("KarmaPlayerCanUseWareHouse")) { KARMA_PLAYER_CAN_USE_WAREHOUSE = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AltRequireCastleForDawn")) { REQUIRE_CASTLE_DAWN = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AltRequireClanCastle")) { REQUIRE_CLAN_CASTLE = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AltFreeTeleporting")) { ALT_GAME_FREE_TELEPORT = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AltSubClassWithoutQuests")) { ALT_GAME_SUBCLASS_WITHOUT_QUESTS = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AltNewCharAlwaysIsNewbie")) { ALT_GAME_NEW_CHAR_ALWAYS_IS_NEWBIE = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AltMembersCanWithdrawFromClanWH")) { ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("DwarfRecipeLimit")) { DWARF_RECIPE_LIMIT = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("CommonRecipeLimit")) { COMMON_RECIPE_LIMIT = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("ChampionEnable")) { CHAMPION_ENABLE = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("ChampionFrequency")) { CHAMPION_FREQUENCY = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("ChampionMinLevel")) { CHAMP_MIN_LVL = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("ChampionMaxLevel")) { CHAMP_MAX_LVL = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("ChampionHp")) { CHAMPION_HP = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("ChampionHpRegen")) { CHAMPION_HP_REGEN = Float.parseFloat(pValue); }
		else if(pName.equalsIgnoreCase("ChampionRewards")) { CHAMPION_REWARDS = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("ChampionAdenasRewards")) { CHAMPION_ADENAS_REWARDS = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("ChampionAtk")) { CHAMPION_ATK = Float.parseFloat(pValue); }
		else if(pName.equalsIgnoreCase("ChampionSpdAtk")) { CHAMPION_SPD_ATK = Float.parseFloat(pValue); }
		else if(pName.equalsIgnoreCase("ChampionRewardItem")) { CHAMPION_REWARD_ITEM = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("ChampionRewardItemID")) { CHAMPION_REWARD_ITEM_ID = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("ChampionRewardItemQty")) { CHAMPION_REWARD_ITEM_QTY = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("AllowWedding")) { ALLOW_WEDDING = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("WeddingPrice")) { WEDDING_PRICE = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("WeddingPunishInfidelity")) { WEDDING_PUNISH_INFIDELITY = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("WeddingTeleport")) { WEDDING_TELEPORT = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("WeddingTeleportPrice")) { WEDDING_TELEPORT_PRICE = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("WeddingTeleportDuration")) { WEDDING_TELEPORT_DURATION = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("WeddingAllowSameSex")) { WEDDING_SAMESEX = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("WeddingFormalWear")) { WEDDING_FORMALWEAR = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("WeddingDivorceCosts")) { WEDDING_DIVORCE_COSTS = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("MinKarma")) { KARMA_MIN_KARMA = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("MaxKarma")) { KARMA_MAX_KARMA = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("XPDivider")) { KARMA_XP_DIVIDER = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("BaseKarmaLost")) { KARMA_LOST_BASE = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("CanGMDropEquipment")) { KARMA_DROP_GM = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AwardPKKillPVPPoint")) { KARMA_AWARD_PK_KILL = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("MinimumPKRequiredToDrop")) { KARMA_PK_LIMIT = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("PvPVsNormalTime")) { PVP_NORMAL_TIME = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("PvPVsPvPTime")) { PVP_PVP_TIME = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("GlobalChat")) { DEFAULT_GLOBAL_CHAT = pValue; }
		else if(pName.equalsIgnoreCase("TradeChat")) { DEFAULT_TRADE_CHAT = pValue; }
		else if(pName.equalsIgnoreCase("MaxPAtkSpeed")) { MAX_PATK_SPEED = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("MaxMAtkSpeed")) { MAX_MATK_SPEED = Integer.parseInt(pValue); }
		else if(pName.equalsIgnoreCase("FlagedPlayerCanUseGK")) { FLAGED_PLAYER_CAN_USE_GK = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("CastleShieldRestriction")) { CASTLE_SHIELD = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("ClanHallShieldRestriction")) { CLANHALL_SHIELD = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("ApellaArmorsRestriction")) { APELLA_ARMORS = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("OathArmorsRestriction")) { OATH_ARMORS = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("CastleLordsCrownRestriction")) { CASTLE_CROWN = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("CastleCircletsRestriction")) { CASTLE_CIRCLETS = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AllowRaidBossPetrified")) { ALLOW_RAID_BOSS_PUT = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("AllowPotsInPvP")) { ALLOW_POTS_IN_PVP = Boolean.valueOf(pValue); }
		else if(pName.equalsIgnoreCase("StartingAncientAdena")) { STARTING_AA = Integer.parseInt(pValue); }
		else { return false; }

		return true;
	}

	public static void saveHexid(int serverId, String string)
	{
		Config.saveHexid(serverId, string, HEXID_FILE);
	}

	public static void saveHexid(int serverId, String hexId, String fileName)
	{
		try
		{
			Properties hexSetting = new Properties();
			File file = new File(fileName);
			file.createNewFile();
			OutputStream out = new FileOutputStream(file);
			hexSetting.setProperty("ServerID", String.valueOf(serverId));
			hexSetting.setProperty("HexID", hexId);
			hexSetting.store(out, "the hexID to auth into login");
			out.close();
		}
		catch(Exception e)
		{
			_log.warn("Failed to save hex id to " + fileName + " File.");
		}
	}

	public static void unallocateFilterBuffer()
	{
		_log.info("Cleaning Chat Filter..");
		FILTER_LIST.clear();
	}

}