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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.GameTimeController;
import com.src.gameserver.ItemsAutoDestroy;
import com.src.gameserver.RecipeController;
import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.ai.L2CharacterAI;
import com.src.gameserver.ai.L2PlayerAI;
import com.src.gameserver.cache.HtmCache;
import com.src.gameserver.communitybbs.BB.Forum;
import com.src.gameserver.communitybbs.Manager.ForumsBBSManager;
import com.src.gameserver.datatables.AccessLevel;
import com.src.gameserver.datatables.ClanLeaderSkillTable;
import com.src.gameserver.datatables.GmListTable;
import com.src.gameserver.datatables.HeroSkillTable;
import com.src.gameserver.datatables.NobleSkillTable;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.datatables.sql.ItemTable;
import com.src.gameserver.datatables.xml.AccessLevels;
import com.src.gameserver.datatables.xml.CharTemplateTable;
import com.src.gameserver.datatables.xml.FishTable;
import com.src.gameserver.datatables.xml.HennaTable;
import com.src.gameserver.datatables.xml.MapRegionTable;
import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.datatables.xml.RecipeTable;
import com.src.gameserver.datatables.xml.SkillTreeTable;
import com.src.gameserver.geo.GeoData;
import com.src.gameserver.handler.AdminCommandHandler;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.handler.IItemHandler;
import com.src.gameserver.handler.ItemHandler;
import com.src.gameserver.handler.skillhandlers.SiegeFlag;
import com.src.gameserver.handler.skillhandlers.StrSiegeAssault;
import com.src.gameserver.handler.skillhandlers.TakeCastle;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.managers.CoupleManager;
import com.src.gameserver.managers.CursedWeaponsManager;
import com.src.gameserver.managers.DimensionalRiftManager;
import com.src.gameserver.managers.DuelManager;
import com.src.gameserver.managers.GrandBossManager;
import com.src.gameserver.managers.ItemsOnGroundManager;
import com.src.gameserver.managers.QuestManager;
import com.src.gameserver.managers.SiegeManager;
import com.src.gameserver.model.BlockList;
import com.src.gameserver.model.FishData;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.L2ClanMember;
import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.L2Fishing;
import com.src.gameserver.model.L2Macro;
import com.src.gameserver.model.L2ManufactureList;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Party;
import com.src.gameserver.model.L2Radar;
import com.src.gameserver.model.L2RecipeList;
import com.src.gameserver.model.L2Request;
import com.src.gameserver.model.L2ShortCut;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.L2Skill.SkillTargetType;
import com.src.gameserver.model.L2SkillLearn;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.Location;
import com.src.gameserver.model.MacroList;
import com.src.gameserver.model.PartyMatchRoom;
import com.src.gameserver.model.PartyMatchRoomList;
import com.src.gameserver.model.PartyMatchWaitingList;
import com.src.gameserver.model.ShortCuts;
import com.src.gameserver.model.TradeList;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.appearance.PcAppearance;
import com.src.gameserver.model.actor.knownlist.PcKnownList;
import com.src.gameserver.model.actor.stat.PcStat;
import com.src.gameserver.model.actor.status.PcStatus;
import com.src.gameserver.model.base.ClassId;
import com.src.gameserver.model.base.ClassLevel;
import com.src.gameserver.model.base.Experience;
import com.src.gameserver.model.base.PlayerClass;
import com.src.gameserver.model.base.Race;
import com.src.gameserver.model.base.SubClass;
import com.src.gameserver.model.entity.Announcements;
import com.src.gameserver.model.entity.Duel;
import com.src.gameserver.model.entity.clanhallsiege.DevastatedCastle;
import com.src.gameserver.model.entity.olympiad.Olympiad;
import com.src.gameserver.model.entity.sevensigns.SevenSigns;
import com.src.gameserver.model.entity.sevensigns.SevenSignsFestival;
import com.src.gameserver.model.entity.siege.Castle;
import com.src.gameserver.model.entity.siege.Siege;
import com.src.gameserver.model.extender.BaseExtender.EventType;
import com.src.gameserver.model.itemcontainer.Inventory;
import com.src.gameserver.model.itemcontainer.ItemContainer;
import com.src.gameserver.model.itemcontainer.PcFreight;
import com.src.gameserver.model.itemcontainer.PcInventory;
import com.src.gameserver.model.itemcontainer.PcWarehouse;
import com.src.gameserver.model.itemcontainer.PetInventory;
import com.src.gameserver.model.quest.Quest;
import com.src.gameserver.model.quest.QuestState;
import com.src.gameserver.model.zone.type.L2BossZone;
import com.src.gameserver.network.L2GameClient;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.ChangeWaitType;
import com.src.gameserver.network.serverpackets.CharInfo;
import com.src.gameserver.network.serverpackets.ConfirmDlg;
import com.src.gameserver.network.serverpackets.CreatureSay;
import com.src.gameserver.network.serverpackets.EtcStatusUpdate;
import com.src.gameserver.network.serverpackets.ExAutoSoulShot;
import com.src.gameserver.network.serverpackets.ExDuelUpdateUserInfo;
import com.src.gameserver.network.serverpackets.ExFishingEnd;
import com.src.gameserver.network.serverpackets.ExFishingStart;
import com.src.gameserver.network.serverpackets.ExOlympiadMode;
import com.src.gameserver.network.serverpackets.ExOlympiadUserInfo;
import com.src.gameserver.network.serverpackets.ExSetCompassZoneCode;
import com.src.gameserver.network.serverpackets.ExShowScreenMessage;
import com.src.gameserver.network.serverpackets.ExStorageMaxCount;
import com.src.gameserver.network.serverpackets.FriendList;
import com.src.gameserver.network.serverpackets.HennaInfo;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.network.serverpackets.L2GameServerPacket;
import com.src.gameserver.network.serverpackets.LeaveWorld;
import com.src.gameserver.network.serverpackets.MagicSkillUser;
import com.src.gameserver.network.serverpackets.MyTargetSelected;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.ObservationMode;
import com.src.gameserver.network.serverpackets.ObservationReturn;
import com.src.gameserver.network.serverpackets.PartySmallWindowUpdate;
import com.src.gameserver.network.serverpackets.PetInventoryUpdate;
import com.src.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import com.src.gameserver.network.serverpackets.PledgeShowMemberListDelete;
import com.src.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import com.src.gameserver.network.serverpackets.PrivateStoreListBuy;
import com.src.gameserver.network.serverpackets.PrivateStoreListSell;
import com.src.gameserver.network.serverpackets.QuestList;
import com.src.gameserver.network.serverpackets.RecipeShopSellList;
import com.src.gameserver.network.serverpackets.RelationChanged;
import com.src.gameserver.network.serverpackets.Ride;
import com.src.gameserver.network.serverpackets.SendTradeDone;
import com.src.gameserver.network.serverpackets.ServerClose;
import com.src.gameserver.network.serverpackets.SetupGauge;
import com.src.gameserver.network.serverpackets.ShortBuffStatusUpdate;
import com.src.gameserver.network.serverpackets.ShortCutInit;
import com.src.gameserver.network.serverpackets.SkillCoolTime;
import com.src.gameserver.network.serverpackets.SkillList;
import com.src.gameserver.network.serverpackets.Snoop;
import com.src.gameserver.network.serverpackets.SocialAction;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.StopMove;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.network.serverpackets.TargetSelected;
import com.src.gameserver.network.serverpackets.TargetUnselected;
import com.src.gameserver.network.serverpackets.TitleUpdate;
import com.src.gameserver.network.serverpackets.TradePressOtherOk;
import com.src.gameserver.network.serverpackets.TradePressOwnOk;
import com.src.gameserver.network.serverpackets.TradeStart;
import com.src.gameserver.network.serverpackets.UserInfo;
import com.src.gameserver.network.serverpackets.ValidateLocation;
import com.src.gameserver.skills.Formulas;
import com.src.gameserver.skills.Stats;
import com.src.gameserver.skills.effects.EffectCharge;
import com.src.gameserver.skills.l2skills.L2SkillSummon;
import com.src.gameserver.templates.chars.L2PcTemplate;
import com.src.gameserver.templates.item.L2Armor;
import com.src.gameserver.templates.item.L2ArmorType;
import com.src.gameserver.templates.item.L2EtcItem;
import com.src.gameserver.templates.item.L2EtcItemType;
import com.src.gameserver.templates.item.L2Henna;
import com.src.gameserver.templates.item.L2Item;
import com.src.gameserver.templates.item.L2Weapon;
import com.src.gameserver.templates.item.L2WeaponType;
import com.src.gameserver.templates.skills.L2EffectType;
import com.src.gameserver.templates.skills.L2SkillType;
import com.src.gameserver.thread.LoginServerThread;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.gameserver.util.Broadcast;
import com.src.gameserver.util.IllegalPlayerAction;
import com.src.gameserver.util.Util;
import com.src.util.CloseUtil;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;
import com.src.util.object.Point3D;
import com.src.util.protection.nProtect;
import com.src.util.random.Rnd;

public final class L2PcInstance extends L2Playable
{
	// Here is Warehouse Account!
	public boolean hasWarehouseAccount = false;
	public String warehouseAccountId,
	warehouseAccountPwd;
	
	protected static final Log _log = LogFactory.getLog(L2PcInstance.class);
	
	private static final String RESTORE_CHARACTER_HP_MP = "SELECT curHp, curCp, curMp FROM characters WHERE obj_id = ?";

	private static final String RESTORE_SKILLS_FOR_CHAR = "SELECT skill_id, skill_level FROM character_skills WHERE char_obj_id = ? AND class_index = ?";

	private static final String ADD_NEW_SKILL = "INSERT INTO character_skills (char_obj_id, skill_id, skill_level, skill_name, class_index) VALUES (?, ?, ?, ?, ?)";

	private static final String UPDATE_CHARACTER_SKILL_LEVEL = "UPDATE character_skills SET skill_level = ? WHERE skill_id = ? AND char_obj_id = ? AND class_index = ?";

	private static final String DELETE_SKILL_FROM_CHAR = "DELETE FROM character_skills WHERE skill_id = ? AND char_obj_id = ? AND class_index = ?";

	private static final String DELETE_CHAR_SKILLS = "DELETE FROM character_skills WHERE char_obj_id = ? AND class_index = ?";

	private static final String ADD_SKILL_SAVE = "INSERT INTO character_skills_save (char_obj_id, skill_id, skill_level, effect_count, effect_cur_time, reuse_delay, systime, restore_type, class_index, buff_index) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String RESTORE_SKILL_SAVE = "SELECT skill_id, skill_level, effect_count, effect_cur_time, reuse_delay, systime FROM character_skills_save WHERE char_obj_id = ? AND class_index = ? AND restore_type = ? ORDER BY buff_index ASC";

	private static final String DELETE_SKILL_SAVE = "DELETE FROM character_skills_save WHERE char_obj_id = ? AND class_index = ?";

	private static final String UPDATE_CHARACTER = "UPDATE characters SET level = ?, maxHp = ?, curHp = ?, maxCp = ?, curCp = ?, maxMp = ?, curMp = ?, str = ?, con = ?, dex = ?, _int = ?, men = ?, wit = ?, face = ?, hairStyle = ?, hairColor = ?, sex = ?, heading = ?, x = ?, y=?, z = ?, exp = ?, expBeforeDeath = ?, sp = ?, karma = ?, pvpkills = ?, pkkills = ?, rec_have = ?, rec_left = ?, clanid = ?, maxload = ?, race = ?, classid = ?, deletetime = ?, title = ?, accesslevel = ?, online = ?, isin7sdungeon = ?, clan_privs = ?, wantspeace = ?, base_class = ?, onlinetime = ?, punish_level = ?, punish_timer = ?, newbie = ?, nobless = ?, power_grade = ?, subpledge = ?, last_recom_date = ?, lvl_joined_academy = ?, apprentice = ?, sponsor = ?, varka_ketra_ally = ?, clan_join_expiry_time = ?, clan_create_expiry_time = ?, char_name = ?, death_penalty_level = ?, name_color = ?, title_color = ?, first_log = ?, haswhacc = ?, whaccid = ?, whaccpwd = ?, aio = ?, aio_end = ?, vip = ?, vip_end = ? WHERE obj_id = ?";

	private static final String RESTORE_CHARACTER = "SELECT account_name, obj_Id, char_name, level, maxHp, curHp, maxCp, curCp, maxMp, curMp, acc, crit, evasion, mAtk, mDef, mSpd, pAtk, pDef, pSpd, runSpd, walkSpd, str, con, dex, _int, men, wit, face, hairStyle, hairColor, sex, heading, x, y, z, movement_multiplier, attack_speed_multiplier, colRad, colHeight, exp, expBeforeDeath, sp, karma, pvpkills, pkkills, clanid, maxload, race, classid, deletetime, cancraft, title, rec_have, rec_left, accesslevel, online, char_slot, lastAccess, clan_privs, wantspeace, base_class, onlinetime, isin7sdungeon, punish_level, punish_timer, newbie, nobless, power_grade, subpledge, last_recom_date, lvl_joined_academy, apprentice, sponsor, varka_ketra_ally, clan_join_expiry_time, clan_create_expiry_time, death_penalty_level, name_color, title_color, first_log, haswhacc, whaccid, whaccpwd, aio, aio_end, vip, vip_end FROM characters WHERE obj_id = ?";

	private static final String STATUS_DATA_GET = "SELECT hero, noble, donator, hero_end_date FROM characters_custom_data WHERE obj_Id = ?";

	private static final String RESTORE_SKILLS_FOR_CHAR_ALT_SUBCLASS = "SELECT skill_id, skill_level FROM character_skills WHERE char_obj_id = ? ORDER BY (skill_level+0)";

	private static final String RESTORE_CHAR_SUBCLASSES = "SELECT class_id, exp, sp, level, class_index FROM character_subclasses WHERE char_obj_id = ? ORDER BY class_index ASC";

	private static final String ADD_CHAR_SUBCLASS = "INSERT INTO character_subclasses (char_obj_id, class_id, exp, sp, level, class_index) VALUES (?, ?, ?, ?, ?, ?)";

	private static final String UPDATE_CHAR_SUBCLASS = "UPDATE character_subclasses SET exp = ?, sp = ?, level = ?, class_id = ? WHERE char_obj_id = ? AND class_index = ?";

	private static final String DELETE_CHAR_SUBCLASS = "DELETE FROM character_subclasses WHERE char_obj_id = ? AND class_index = ?";

	private static final String RESTORE_CHAR_HENNAS = "SELECT slot, symbol_id FROM character_hennas WHERE char_obj_id = ? AND class_index = ?";

	private static final String ADD_CHAR_HENNA = "INSERT INTO character_hennas (char_obj_id, symbol_id, slot, class_index) VALUES (?, ?, ?, ?)";

	private static final String DELETE_CHAR_HENNA = "DELETE FROM character_hennas WHERE char_obj_id = ? AND slot = ? AND class_index = ?";

	private static final String DELETE_CHAR_HENNAS = "DELETE FROM character_hennas WHERE char_obj_id = ? AND class_index = ?";

	private static final String DELETE_CHAR_SHORTCUTS = "DELETE FROM character_shortcuts WHERE char_obj_id = ? AND class_index = ?";

	private static final String RESTORE_CHAR_RECOMS = "SELECT char_id, target_id FROM character_recommends WHERE char_id = ?";

	private static final String ADD_CHAR_RECOM = "INSERT INTO character_recommends (char_id, target_id) VALUES (?, ?)";

	private static final String DELETE_CHAR_RECOMS = "DELETE FROM character_recommends WHERE char_id = ?";

	public static final int REQUEST_TIMEOUT = 15;

	public static final int STORE_PRIVATE_NONE = 0;
	public static final int STORE_PRIVATE_SELL = 1;
	public static final int STORE_PRIVATE_BUY = 3;
	public static final int STORE_PRIVATE_MANUFACTURE = 5;
	public static final int STORE_PRIVATE_PACKAGE_SELL = 8;

	private static final int[] EXPERTISE_LEVELS =
	{
		SkillTreeTable.getInstance().getExpertiseLevel(0),
		SkillTreeTable.getInstance().getExpertiseLevel(1),
		SkillTreeTable.getInstance().getExpertiseLevel(2),
		SkillTreeTable.getInstance().getExpertiseLevel(3),
		SkillTreeTable.getInstance().getExpertiseLevel(4),
		SkillTreeTable.getInstance().getExpertiseLevel(5),
	};

	private static final int[] COMMON_CRAFT_LEVELS =
	{
		5, 20, 28, 36, 43, 49, 55, 62
	};

	public class AIAccessor extends L2Character.AIAccessor
	{
		protected AIAccessor()
		{}

		public L2PcInstance getPlayer()
		{
			return L2PcInstance.this;
		}

		public void doPickupItem(L2Object object)
		{
			L2PcInstance.this.doPickupItem(object);
		}

		public void doInteract(L2Character target)
		{
			L2PcInstance.this.doInteract(target);
		}

		@Override
		public void doAttack(L2Character target)
		{
			if(isInsidePeaceZone(L2PcInstance.this, target))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			super.doAttack(target);

			getPlayer().setRecentFakeDeath(false);

			if(getPlayer().isSilentMoving())
			{
				L2Effect silentMove = getPlayer().getFirstEffect(L2EffectType.SILENT_MOVE);
				if(silentMove != null)
				{
					silentMove.exit();
				}
			}

			for(L2CubicInstance cubic : getCubics().values())
			{
				if(cubic != null && cubic.getId() != L2CubicInstance.LIFE_CUBIC)
				{
					cubic.doAction(target);
				}
			}
		}

		@Override
		public void doCast(L2Skill skill)
		{
			super.doCast(skill);

			getPlayer().setRecentFakeDeath(false);
			if(skill == null)
			{
				return;
			}

			if(!skill.isOffensive())
			{
				return;
			}

			if(getPlayer().isSilentMoving() && skill.getSkillType() != L2SkillType.AGGDAMAGE)
			{
				L2Effect silentMove = getPlayer().getFirstEffect(L2EffectType.SILENT_MOVE);
				if(silentMove != null)
				{
					silentMove.exit();
				}
			}

			switch(skill.getTargetType())
			{
				case TARGET_GROUND:
					return;
				default:
				{
					L2Object mainTarget = skill.getFirstOfTargetList(L2PcInstance.this);
					if(mainTarget == null || !(mainTarget instanceof L2Character))
					{
						return;
					}
					for(L2CubicInstance cubic : getCubics().values())
					{
						if(cubic.getId() != L2CubicInstance.LIFE_CUBIC)
						{
							cubic.doAction((L2Character) mainTarget);
						}
					}
					mainTarget = null;
				}
				break;
			}
		}
	}

	private L2GameClient _client;

	private String _accountName;
	private long _deleteTimer;

	private boolean _isOnline = false;

	private long _onlineTime;
	private long _onlineBeginTime;
	private long _lastAccess;
	private long _uptime;

	protected int _baseClass;
	protected int _activeClass;
	protected int _classIndex = 0;

	private boolean _first_log;

	private ScheduledFuture<?> _dismountTask;
	
	private Map<Integer, SubClass> _subClasses;

	private PcAppearance _appearance;

	private int _charId = 0x00030b7a;

	private long _expBeforeDeath;

	private long _notMoveUntil = 0;
	
	private int _karma;

	private int _pvpKills;

	private int _pkKills;

	private byte _pvpFlag;

	private byte _siegeState = 0;

	private int _curWeightPenalty = 0;

	private int _lastCompassZone;
	private byte _zoneValidateCounter = 4;

	private boolean _isIn7sDungeon = false;

	public int eventX;
	public int eventY;
	public int eventZ;
	public int eventKarma;
	public int eventPvpKills;
	public int eventPkKills;
	public String eventTitle;
	public List<String> kills = new LinkedList<String>();
	public boolean eventSitForced = false;
	public boolean atEvent = false;

	public int _correctWord = -1;
	public boolean _stopKickBotTask = false;

	public int _originalNameColor, _countKills, _originalKarma, _eventKills;
	public boolean _inEvent = false;

	private boolean _inOlympiadMode = false;
	private boolean _OlympiadStart = false;
	private int[] _OlympiadPosition;
	private int _olympiadGameId = -1;
	private int _olympiadSide = -1;
	public int dmgDealt = 0;

	private boolean _isInDuel = false;
	private int _duelState = Duel.DUELSTATE_NODUEL;
	private int _duelId = 0;

	private boolean _inBoat;
	private L2BoatInstance _boat;
	private Point3D _inBoatPosition;

	private int _mountType;
	private int _mountObjectID = 0;

	public int _telemode = 0;

	private boolean _isSilentMoving = false;

	private boolean _inCrystallize;

	private boolean _inCraftMode;

	private Map<Integer, L2RecipeList> _dwarvenRecipeBook = new FastMap<Integer, L2RecipeList>();
	private Map<Integer, L2RecipeList> _commonRecipeBook = new FastMap<Integer, L2RecipeList>();

	private boolean _waitTypeSitting;

	private boolean _sitdowntask;
	boolean sittingTaskLaunched;
	
	private boolean _relax;
    
	private int _obsX;
	private int _obsY;
	private int _obsZ;
	private boolean _observerMode = false;

	private Location _lastClientPosition = new Location(0, 0, 0);
	private Location _lastServerPosition = new Location(0, 0, 0);

	private int _recomHave;
	private int _recomLeft;
	private long _lastRecomUpdate;
	private List<Integer> _recomChars = new FastList<Integer>();

	private PcInventory _inventory = new PcInventory(this);
	private PcWarehouse _warehouse;
	private PcFreight _freight = new PcFreight(this);

	private int _privatestore;

	private TradeList _activeTradeList;
	private ItemContainer _activeWarehouse;
	private L2ManufactureList _createList;
	private TradeList _sellList;
	private TradeList _buyList;

	private boolean _newbie;
	private SkillDat _currentPetSkill;
	private boolean _noble = false;
	private boolean _hero = false;
	
	/** Aio System */
	private boolean _isAio = false;
	private long _aio_endTime = 0;

    /** Vip System */
    private boolean _isVip = false;
    private long _vip_endTime = 0;
	
	private boolean _heavy_mastery = false;
	private boolean _light_mastery = false;
	private boolean _robe_mastery = false;
	private int _masteryPenalty = 0;
	
	public boolean _clanLeader = false;

	private L2NpcInstance _lastFolkNpc = null;

	private int _questNpcObject = 0;

	private SummonRequest _summonRequest = new SummonRequest();
 	
	public void shortBuffStatusUpdate(int magicId, int level, int time) 
	{ 
		if (_shortBuffTask != null) 
		{ 
			_shortBuffTask.cancel(false); 
			_shortBuffTask = null; 
		} 
		_shortBuffTask = ThreadPoolManager.getInstance().scheduleGeneral(new ShortBuffTask(this), 15000); 
		
		sendPacket(new ShortBuffStatusUpdate(magicId, level, time)); 
	} 
	
	private ScheduledFuture<?> _shortBuffTask = null; 
	
	private class ShortBuffTask implements Runnable 
	{ 
		private L2PcInstance _player = null; 
		
		public ShortBuffTask(L2PcInstance activeChar) 
		{ 
			_player = activeChar; 
		} 
		
		@Override
		public void run() 
		{ 
			if (_player == null) 
				return; 
			
			_player.sendPacket(new ShortBuffStatusUpdate(0, 0, 0)); 
		} 
	}
 	
	private static class SummonRequest
	{
		private L2PcInstance _target = null;
		private L2Skill _skill = null;
		
		public void setTarget(L2PcInstance destination, L2Skill skill)
		{
			_target = destination;
			_skill = skill;
		}
		
		public L2PcInstance getTarget()
		{
			return _target;
		}
		
		public L2Skill getSkill()
		{
			return _skill;
		}
	}
	
	private Map<String, QuestState> _quests = new FastMap<String, QuestState>();

	private ShortCuts _shortCuts = new ShortCuts(this);

	private MacroList _macroses = new MacroList(this);

	private List<L2PcInstance> _snoopListener = new FastList<L2PcInstance>();
	private List<L2PcInstance> _snoopedPlayer = new FastList<L2PcInstance>();

	private ClassId _skillLearningClassId;

	private final L2HennaInstance[] _henna = new L2HennaInstance[3];
	private int _hennaSTR;
	private int _hennaINT;
	private int _hennaDEX;
	private int _hennaMEN;
	private int _hennaWIT;
	private int _hennaCON;

	private L2Summon _summon = null;
	private L2TamedBeastInstance _tamedBeast = null;

	private L2Radar _radar;

	// Party matching
	private int _partyroom = 0;

	// Clan related attributes
	private int _clanId = 0;
	private L2Clan _clan;

	private int _apprentice = 0;
	private int _sponsor = 0;

	public boolean _allowTrade = true;

	private long _clanJoinExpiryTime;
	private long _clanCreateExpiryTime;

	private int _powerGrade = 0;
	private int _clanPrivileges = 0;

	private int _pledgeClass = 0;
	private int _pledgeType = 0;

	private int _lvlJoinedAcademy = 0;

	private int _wantsPeace = 0;

	private int _deathPenaltyBuffLevel = 0;

	private AtomicInteger _charges = new AtomicInteger();
	private ScheduledFuture<?> _chargeTask = null;
	private AccessLevel _accessLevel;

	private boolean _messageRefusal = false;
	private boolean _dietMode = false;
	private boolean _exchangeRefusal = false;

	private L2Party _party;

	private long _lastAttackPacket = 0;
	
	private L2PcInstance _activeRequester;
	private long _requestExpireTime = 0;
	private L2Request _request = new L2Request(this);
	private L2ItemInstance _arrowItem;

	private long _protectEndTime = 0;
	public boolean isSpawnProtected() { return (_protectEndTime > 0); }

	private long _recentFakeDeathEndTime = 0;

	public int _clientX;
	public int _clientY;
	public int _clientZ;
	public int _clientHeading;

	private L2Weapon _fistsWeaponItem;

	private final Map<Integer, String> _chars = new FastMap<Integer, String>();

	private int _expertiseIndex;
	private int _expertisePenalty = 0;

	private boolean _isEnchanting = false;
	private L2ItemInstance _activeEnchantItem = null;

	protected boolean _inventoryDisable = false;

	protected Map<Integer, L2CubicInstance> _cubics = new FastMap<Integer, L2CubicInstance>();

	protected Map<Integer, Integer> _activeSoulShots = new FastMap<Integer, Integer>().shared();

	public final ReentrantLock soulShotLock = new ReentrantLock();

	public Quest dialog = null;

	private int _loto[] = new int[5];

	private int _race[] = new int[2];

	private final BlockList _blockList = new BlockList();
	private boolean _tradeRefusal = false; // Trade refusal

	private int _team = 0;

	private int _alliedVarkaKetra = 0;

	private int _hasCoupon = 0;
	private L2Fishing _fishCombat;
	private boolean _fishing = false;
	private int _fishx = 0;
	private int _fishy = 0;
	private int _fishz = 0;

	private ScheduledFuture<?> _taskRentPet;
	private ScheduledFuture<?> _taskWater;

	private List<String> _validBypass = new FastList<String>();
	private List<String> _validBypass2 = new FastList<String>();
	private List<String> _validLink = new FastList<String>();

	private Forum _forumMail;
	private Forum _forumMemo;

	private SkillDat _currentSkill;
	
	private SkillDat _queuedSkill;

	private boolean _IsWearingFormalWear = false;

	private Point3D _currentSkillWorldPosition;

	private int _cursedWeaponEquipedId = 0;

	private int _reviveRequested = 0;
	private double _revivePower = 0;
	private boolean _revivePet = false;

	private double _cpUpdateIncCheck = .0;
	private double _cpUpdateDecCheck = .0;
	private double _cpUpdateInterval = .0;
	private double _mpUpdateIncCheck = .0;
	private double _mpUpdateDecCheck = .0;
	private double _mpUpdateInterval = .0;

	private boolean isInDangerArea;

	private boolean _isOffline = false;

	private boolean _isTradeOff = false;

	private long _offlineShopStart = 0;

	public int _originalNameColorOffline = 0xFFFFFF;

	private int _herbstask = 0;

	private boolean _expGainOn = true;

	public class HerbTask implements Runnable
	{
		private String _process;
		private int _itemId;
		private int _count;
		private L2Object _reference;
		private boolean _sendMessage;

		HerbTask(String process, int itemId, int count, L2Object reference, boolean sendMessage)
		{
			_process = process;
			_itemId = itemId;
			_count = count;
			_reference = reference;
			_sendMessage = sendMessage;
		}

		@Override
		@SuppressWarnings("synthetic-access")
		public void run()
		{
			try
			{
				addItem(_process, _itemId, _count, _reference, _sendMessage);
			}
			catch(Throwable t)
			{
				_log.error("", t);
			}
		}
	}

	private boolean _married = false;
	private int _marriedType = 0;
	private int _partnerId = 0;
	private int _coupleId = 0;
	private boolean _engagerequest = false;
	private int _engageid = 0;
	private boolean _marryrequest = false;
	private boolean _marryaccepted = false;
	private boolean _isLocked = false;

	public class SkillDat
	{
		private L2Skill _skill;
		private boolean _ctrlPressed;
		private boolean _shiftPressed;

		protected SkillDat(L2Skill skill, boolean ctrlPressed, boolean shiftPressed)
		{
			_skill = skill;
			_ctrlPressed = ctrlPressed;
			_shiftPressed = shiftPressed;
		}

		public boolean isCtrlPressed()
		{
			return _ctrlPressed;
		}

		public boolean isShiftPressed()
		{
			return _shiftPressed;
		}

		public L2Skill getSkill()
		{
			return _skill;
		}

		public int getSkillId()
		{
			return getSkill() != null ? getSkill().getId() : -1;
		}
	}

	public static L2PcInstance create(int objectId, L2PcTemplate template, String accountName, String name, byte hairStyle, byte hairColor, byte face, boolean sex)
	{
		PcAppearance app = new PcAppearance(face, hairColor, hairStyle, sex);
		L2PcInstance player = new L2PcInstance(objectId, template, accountName, app);
		app = null;

		player.setName(name);

		player.setBaseClass(player.getClassId());

		if(Config.ALT_GAME_NEW_CHAR_ALWAYS_IS_NEWBIE)
		{
			player.setNewbie(true);
		}

		boolean ok = player.createDb();

		if(!ok)
		{
			return null;
		}

		return player;
	}

	public static L2PcInstance createDummyPlayer(int objectId, String name)
	{
		L2PcInstance player = new L2PcInstance(objectId);
		player.setName(name);

		return player;
	}

	public String getAccountName()
	{
		if(getClient()!=null)
		{
			return getAccountNamePlayer();
		}

		return getClient().getAccountName();
	}

	public String getAccountNamePlayer()
	{
		return _accountName;
	}

	public Map<Integer, String> getAccountChars()
	{
		return _chars;
	}

	public int getRelation(L2PcInstance target)
	{
		int result = 0;

		if(getPvpFlag() != 0)
		{
			result |= RelationChanged.RELATION_PVP_FLAG;
		}
		if(getKarma() > 0)
		{
			result |= RelationChanged.RELATION_HAS_KARMA;
		}

		if(isClanLeader())
		{
			result |= RelationChanged.RELATION_LEADER;
		}

		if(getSiegeState() != 0)
		{
			result |= RelationChanged.RELATION_INSIEGE;
			if(getSiegeState() != target.getSiegeState())
			{
				result |= RelationChanged.RELATION_ENEMY;
			}
			else
			{
				result |= RelationChanged.RELATION_ALLY;
			}

			if(getSiegeState() == 1)
			{
				result |= RelationChanged.RELATION_ATTACKER;
			}
		}

		if(getClan() != null && target.getClan() != null)
		{
			if(target.getPledgeType() != L2Clan.SUBUNIT_ACADEMY && getPledgeType() != L2Clan.SUBUNIT_ACADEMY && target.getClan().isAtWarWith(getClan().getClanId()))
			{
				result |= RelationChanged.RELATION_1SIDED_WAR;
				if(getClan().isAtWarWith(target.getClan().getClanId()))
				{
					result |= RelationChanged.RELATION_MUTUAL_WAR;
				}
			}
		}

		return result;
	}

	public static L2PcInstance load(int objectId)
	{
		return restore(objectId);
	}

	private void initPcStatusUpdateValues()
	{
		_cpUpdateInterval = getMaxCp() / 352.0;
		_cpUpdateIncCheck = getMaxCp();
		_cpUpdateDecCheck = getMaxCp() - _cpUpdateInterval;
		_mpUpdateInterval = getMaxMp() / 352.0;
		_mpUpdateIncCheck = getMaxMp();
		_mpUpdateDecCheck = getMaxMp() - _mpUpdateInterval;
	}

	private L2PcInstance(int objectId, L2PcTemplate template, String accountName, PcAppearance app)
	{
		super(objectId, template);
		getKnownList();
		getStat();
		getStatus();
		super.initCharStatusUpdateValues();
		initPcStatusUpdateValues();

		_accountName = accountName;
		_appearance = app;

		_ai = new L2PlayerAI(new L2PcInstance.AIAccessor());

		_radar = new L2Radar(this);

		getInventory().restore();
		getWarehouse();
		getFreight().restore();
	}

	private L2PcInstance(int objectId)
	{
		super(objectId, null);
		getKnownList();
		getStat();
		getStatus();
		super.initCharStatusUpdateValues();
		initPcStatusUpdateValues();
	}

	@Override
	public final PcKnownList getKnownList()
	{
		if(super.getKnownList() == null || !(super.getKnownList() instanceof PcKnownList))
		{
			setKnownList(new PcKnownList(this));
		}

		return (PcKnownList) super.getKnownList();
	}

	@Override
	public final PcStat getStat()
	{
		if(super.getStat() == null || !(super.getStat() instanceof PcStat))
		{
			setStat(new PcStat(this));
		}

		return (PcStat) super.getStat();
	}

	@Override
	public final PcStatus getStatus()
	{
		if(super.getStatus() == null || !(super.getStatus() instanceof PcStatus))
		{
			setStatus(new PcStatus(this));
		}

		return (PcStatus) super.getStatus();
	}

	public final PcAppearance getAppearance()
	{
		return _appearance;
	}

	public final L2PcTemplate getBaseTemplate()
	{
		return CharTemplateTable.getInstance().getTemplate(_baseClass);
	}

	@Override
	public final L2PcTemplate getTemplate()
	{
		return (L2PcTemplate) super.getTemplate();
	}

	public void setTemplate(ClassId newclass)
	{
		super.setTemplate(CharTemplateTable.getInstance().getTemplate(newclass));
	}

	@Override
	public L2CharacterAI getAI()
	{
		if(_ai == null)
		{
			synchronized (this)
			{
				if(_ai == null)
				{
					_ai = new L2PlayerAI(new L2PcInstance.AIAccessor());
				}
			}
		}

		return _ai;
	}

	@Override
	public final int getLevel()
	{
		int level = getStat().getLevel();

		if(level == -1)
		{
			L2PcInstance local_char = restore(this.getObjectId());
			if(local_char!=null)
			{
				level = local_char.getLevel();
			}
		}

		if(level<0)
		{
			level = 1;
		}

		return level;
	}

	public boolean isNewbie()
	{
		return _newbie;
	}

	public void setNewbie(boolean isNewbie)
	{
		_newbie = isNewbie;
	}

	public void setBaseClass(int baseClass)
	{
		_baseClass = baseClass;
	}

	public void setBaseClass(ClassId classId)
	{
		_baseClass = classId.ordinal();
	}

	public boolean isInStoreMode()
	{
		return (getPrivateStoreType() > L2PcInstance.STORE_PRIVATE_NONE);
	}

	public boolean isInCraftMode()
	{
		return _inCraftMode;
	}

	public void isInCraftMode(boolean b)
	{
		_inCraftMode = b;
	}

	public void logout()
	{
		if(atEvent)
		{
			sendMessage("A superior power doesn't allow you to leave the event.");
			sendPacket(ActionFailed.STATIC_PACKET);
		}
		closeNetConnection();
	}

	public L2RecipeList[] getCommonRecipeBook()
	{
		return _commonRecipeBook.values().toArray(new L2RecipeList[_commonRecipeBook.values().size()]);
	}

	public L2RecipeList[] getDwarvenRecipeBook()
	{
		return _dwarvenRecipeBook.values().toArray(new L2RecipeList[_dwarvenRecipeBook.values().size()]);
	}

	public void registerCommonRecipeList(L2RecipeList recipe)
	{
		_commonRecipeBook.put(recipe.getId(), recipe);
	}

	public void registerDwarvenRecipeList(L2RecipeList recipe)
	{
		_dwarvenRecipeBook.put(recipe.getId(), recipe);
	}

	public boolean hasRecipeList(int recipeId)
	{
		if(_dwarvenRecipeBook.containsKey(recipeId))
		{
			return true;
		}
		else if(_commonRecipeBook.containsKey(recipeId))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public void unregisterRecipeList(int recipeId)
	{
		if(_dwarvenRecipeBook.containsKey(recipeId))
		{
			_dwarvenRecipeBook.remove(recipeId);
		}
		else if(_commonRecipeBook.containsKey(recipeId))
		{
			_commonRecipeBook.remove(recipeId);
		}
		else
		{
			_log.warn("Attempted to remove unknown RecipeList: " + recipeId);
		}

		L2ShortCut[] allShortCuts = getAllShortCuts();

		for(L2ShortCut sc : allShortCuts)
		{
			if(sc != null && sc.getId() == recipeId && sc.getType() == L2ShortCut.TYPE_RECIPE)
			{
				deleteShortCut(sc.getSlot(), sc.getPage());
			}
		}

		allShortCuts = null;
	}

	public int getLastQuestNpcObject()
	{
		return _questNpcObject;
	}

	public void setLastQuestNpcObject(int npcId)
	{
		_questNpcObject = npcId;
	}

	public QuestState getQuestState(String quest)
	{
		return _quests.get(quest);
	}

	public void setQuestState(QuestState qs)
	{
		_quests.put(qs.getQuestName(), qs);
	}

	public void delQuestState(String quest)
	{
		_quests.remove(quest);
	}

	private QuestState[] addToQuestStateArray(QuestState[] questStateArray, QuestState state)
	{
		int len = questStateArray.length;
		QuestState[] tmp = new QuestState[len + 1];
		for(int i = 0; i < len; i++)
		{
			tmp[i] = questStateArray[i];
		}
		tmp[len] = state;
		return tmp;
	}

	public Quest[] getAllActiveQuests()
	{
		FastList<Quest> quests = new FastList<Quest>();

		for(QuestState qs : _quests.values())
		{
			if(qs != null)
			{
				if(qs.getQuest().getQuestIntId() >= 999)
				{
					continue;
				}

				if(qs.isCompleted())
				{
					continue;
				}

				if(!qs.isStarted())
				{
					continue;
				}

				quests.add(qs.getQuest());
			}
		}

		return quests.toArray(new Quest[quests.size()]);
	}

	public QuestState[] getQuestsForAttacks(L2Npc npc)
	{
		QuestState[] states = null;

		for(Quest quest : npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_ATTACK))
		{
			if(getQuestState(quest.getName()) != null)
			{
				if(states == null)
				{
					states = new QuestState[]
					{
						getQuestState(quest.getName())
					};
				}
				else
				{
					states = addToQuestStateArray(states, getQuestState(quest.getName()));
				}
			}
		}

		return states;
	}

	public QuestState[] getQuestsForKills(L2Npc npc)
	{
		QuestState[] states = null;

		for(Quest quest : npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_KILL))
		{
			if(getQuestState(quest.getName()) != null)
			{
				if(states == null)
				{
					states = new QuestState[]
					{
						getQuestState(quest.getName())
					};
				}
				else
				{
					states = addToQuestStateArray(states, getQuestState(quest.getName()));
				}
			}
		}

		return states;
	}

	public QuestState[] getQuestsForTalk(int npcId)
	{
		QuestState[] states = null;

		Quest[] quests = NpcTable.getInstance().getTemplate(npcId).getEventQuests(Quest.QuestEventType.QUEST_TALK);
		if(quests != null)
		{
			for(Quest quest : quests)
			{
				if(quest != null)
				{
					if(getQuestState(quest.getName()) != null)
					{
						if(states == null)
						{
							states = new QuestState[]
							{
								getQuestState(quest.getName())
							};
						}
						else
						{
							states = addToQuestStateArray(states, getQuestState(quest.getName()));
						}
					}
				}
			}

			quests = null;
		}

		return states;
	}

	public QuestState processQuestEvent(String quest, String event)
	{
		QuestState retval = null;
		if(event == null)
		{
			event = "";
		}

		if(!_quests.containsKey(quest))
		{
			return retval;
		}

		QuestState qs = getQuestState(quest);
		if(qs == null && event.length() == 0)
		{
			return retval;
		}

		if(qs == null)
		{
			Quest q = QuestManager.getInstance().getQuest(quest);
			if(q == null)
			{
				return retval;
			}

			qs = q.newQuestState(this);
		}

		if(qs != null)
		{
			if(getLastQuestNpcObject() > 0)
			{
				L2Object object = L2World.getInstance().findObject(getLastQuestNpcObject());
				if(object instanceof L2Npc && isInsideRadius(object, L2Npc.INTERACTION_DISTANCE, false, false))
				{
					L2Npc npc = (L2Npc) object;
					QuestState[] states = getQuestsForTalk(npc.getNpcId());

					if(states != null)
					{
						for(QuestState state : states)
						{
							if(state.getQuest().getQuestIntId() == qs.getQuest().getQuestIntId() && !qs.isCompleted())
							{
								if(qs.getQuest().notifyEvent(event, npc, this))
								{
									showQuestWindow(quest, qs.getStateId());
								}

								retval = qs;
							}
						}

						sendPacket(new QuestList());
					}
				}
			}

			qs = null;
		}

		return retval;
	}

	private void showQuestWindow(String questId, String stateId)
	{
		String path = "data/scripts/quests/" + questId + "/" + stateId + ".htm";
		String content = HtmCache.getInstance().getHtm(path);

		if(content != null)
		{
			NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
			npcReply.setHtml(content);
			sendPacket(npcReply);
			content = null;
			npcReply = null;
		}

		sendPacket(ActionFailed.STATIC_PACKET);
		path = null;
	}

	public L2ShortCut[] getAllShortCuts()
	{
		return _shortCuts.getAllShortCuts();
	}

	public L2ShortCut getShortCut(int slot, int page)
	{
		return _shortCuts.getShortCut(slot, page);
	}

	public void registerShortCut(L2ShortCut shortcut)
	{
		_shortCuts.registerShortCut(shortcut);
	}

	public void deleteShortCut(int slot, int page)
	{
		_shortCuts.deleteShortCut(slot, page);
	}

	public void registerMacro(L2Macro macro)
	{
		_macroses.registerMacro(macro);
	}

	public void deleteMacro(int id)
	{
		_macroses.deleteMacro(id);
	}

	public MacroList getMacroses()
	{
		return _macroses;
	}

	public void setSiegeState(byte siegeState)
	{
		_siegeState = siegeState;
	}

	public byte getSiegeState()
	{
		return _siegeState;
	}

	public void setPvpFlag(int pvpFlag)
	{
		_pvpFlag = (byte) pvpFlag;
	}

	public byte getPvpFlag()
	{
		return _pvpFlag;
	}

	@Override
	public void updatePvPFlag(int value)
	{
		if(getPvpFlag() == value)
			return;
		setPvpFlag(value);

		sendPacket(new UserInfo(this));

		// If this player has a pet update the pets pvp flag as well
		if(getPet() != null)
		{
			sendPacket(new RelationChanged(getPet(), getRelation(this), false));
		}

		for(L2PcInstance target : getKnownList().getKnownPlayers().values())
		{
			if(target==null)
				continue;
			
			target.sendPacket(new RelationChanged(this, getRelation(this), isAutoAttackable(target)));
			if(getPet() != null)
			{
				target.sendPacket(new RelationChanged(getPet(), getRelation(this), isAutoAttackable(target)));
			}
		}
	}

	public void revalidateZone(boolean force)
	{
		if(getWorldRegion() == null)
		{
			return;
		}

		if(Config.ALLOW_WATER)
		{
			checkWaterState();
		}

		if(force)
		{
			_zoneValidateCounter = 4;
		}
		else
		{
			_zoneValidateCounter--;
			if(_zoneValidateCounter < 0)
			{
				_zoneValidateCounter = 4;
			}
			else
			{
				return;
			}
		}

		getWorldRegion().revalidateZones(this);

		if(isInsideZone(ZONE_SIEGE))
		{
			if(_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2)
			{
				return;
			}
			_lastCompassZone = ExSetCompassZoneCode.SIEGEWARZONE2;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.SIEGEWARZONE2);
			sendPacket(cz);
			cz = null;
		}
		else if(isInsideZone(ZONE_PVP))
		{
			if(_lastCompassZone == ExSetCompassZoneCode.PVPZONE)
			{
				return;
			}
			_lastCompassZone = ExSetCompassZoneCode.PVPZONE;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.PVPZONE);
			sendPacket(cz);
			cz = null;
		}
		else if(isIn7sDungeon())
		{
			if(_lastCompassZone == ExSetCompassZoneCode.SEVENSIGNSZONE)
			{
				return;
			}
			_lastCompassZone = ExSetCompassZoneCode.SEVENSIGNSZONE;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.SEVENSIGNSZONE);
			sendPacket(cz);
			cz = null;
		}
		else if(isInsideZone(ZONE_PEACE))
		{
			if(_lastCompassZone == ExSetCompassZoneCode.PEACEZONE)
			{
				return;
			}
			_lastCompassZone = ExSetCompassZoneCode.PEACEZONE;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.PEACEZONE);
			sendPacket(cz);
			cz = null;
		}
		else
		{
			if(_lastCompassZone == ExSetCompassZoneCode.GENERALZONE)
			{
				return;
			}
			if(_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2)
			{
				updatePvPStatus();
			}
			_lastCompassZone = ExSetCompassZoneCode.GENERALZONE;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.GENERALZONE);
			sendPacket(cz);
			cz = null;
		}
	}

	public boolean hasDwarvenCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_DWARVEN) >= 1;
	}

	public int getDwarvenCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_DWARVEN);
	}

	public boolean hasCommonCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_COMMON) >= 1;
	}

	public int getCommonCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_COMMON);
	}

	public int getPkKills()
	{
		return _pkKills;
	}

	public void setPkKills(int pkKills)
	{
		_pkKills = pkKills;
	}

	public long getDeleteTimer()
	{
		return _deleteTimer;
	}

	public void setDeleteTimer(long deleteTimer)
	{
		_deleteTimer = deleteTimer;
	}

	public int getCurrentLoad()
	{
		return _inventory.getTotalWeight();
	}

	public long getLastRecomUpdate()
	{
		return _lastRecomUpdate;
	}

	public void setLastRecomUpdate(long date)
	{
		_lastRecomUpdate = date;
	}

	public int getRecomHave()
	{
		return _recomHave;
	}

	protected void incRecomHave()
	{
		if(_recomHave < 255)
		{
			_recomHave++;
		}
	}

	public void setRecomHave(int value)
	{
		if(value > 255)
		{
			_recomHave = 255;
		}
		else if(value < 0)
		{
			_recomHave = 0;
		}
		else
		{
			_recomHave = value;
		}
	}

	public int getRecomLeft()
	{
		return _recomLeft;
	}

	protected void decRecomLeft()
	{
		if(_recomLeft > 0)
		{
			_recomLeft--;
		}
	}

	public void giveRecom(L2PcInstance target)
	{
		if(Config.ALT_RECOMMEND)
		{
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement(ADD_CHAR_RECOM);
				statement.setInt(1, getObjectId());
				statement.setInt(2, target.getObjectId());
				statement.execute();
			}
			catch(Exception e)
			{
				_log.error("could not update char recommendations", e);
			}
			finally
			{
				ResourceUtil.closeStatement(statement);
				ResourceUtil.closeConnection(con);
			}
		}
		target.incRecomHave();
		decRecomLeft();
		_recomChars.add(target.getObjectId());
	}

	public boolean canRecom(L2PcInstance target)
	{
		return !_recomChars.contains(target.getObjectId());
	}

	public void setExpBeforeDeath(long exp)
	{
		_expBeforeDeath = exp;
	}

	public long getExpBeforeDeath()
	{
		return _expBeforeDeath;
	}

	public int getKarma()
	{
		return _karma;
	}

	public void setKarma(int karma)
	{
		if(karma < 0)
		{
			karma = 0;
		}

		if(_karma == 0 && karma > 0)
		{
			for(L2Object object : getKnownList().getKnownObjects().values())
			{
				if(object == null || !(object instanceof L2GuardInstance))
				{
					continue;
				}

				if(((L2GuardInstance) object).getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
				{
					((L2GuardInstance) object).getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
				}
			}
		}
		else if(_karma > 0 && karma == 0)
		{
			sendPacket(new UserInfo(this));
			setKarmaFlag(0);
			broadcastRelationsChanges();
		}
		
		sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_KARMA_HAS_BEEN_CHANGED_TO).addNumber(karma));

		_karma = karma;
		broadcastKarma();
	}

	public int getMaxLoad()
	{
		int con = getCON();
		if(con < 1)
		{
			return 31000;
		}

		if(con > 59)
		{
			return 176000;
		}

		double baseLoad = Math.pow(1.029993928, con) * 30495.627366;
		return (int) calcStat(Stats.MAX_LOAD, baseLoad * Config.ALT_WEIGHT_LIMIT, this, null);
	}

	public int getExpertisePenalty()
	{
		return _expertisePenalty;
	}

	public int getMasteryPenalty()
	{
		return _masteryPenalty;
	}
	
	public int getMasteryWeapPenalty()
	{
		return _masteryWeapPenalty;
	}
	
	public int getWeightPenalty()
	{
		if(_dietMode)
			return 0;
		return _curWeightPenalty;
	}

	/**
	 * Update the overloaded status of the L2PcInstance.<BR><BR>
	 */
	public void refreshOverloaded()
	{
		if(Config.DISABLE_WEIGHT_PENALTY)
		{
			setIsOverloaded(false);
		}
		else if(_dietMode)
		{
			setIsOverloaded(false);
			_curWeightPenalty = 0;
			super.removeSkill(getKnownSkill(4270));
			sendPacket(new EtcStatusUpdate(this));
			Broadcast.toKnownPlayers(this, new CharInfo(this));
		}
		else
		{
			int maxLoad = getMaxLoad();
			if(maxLoad > 0)
			{
				setIsOverloaded(getCurrentLoad() > maxLoad);
				int weightproc = getCurrentLoad() * 1000 / maxLoad;
				int newWeightPenalty;

				if(weightproc < 500)
				{
					newWeightPenalty = 0;
				}
				else if(weightproc < 666)
				{
					newWeightPenalty = 1;
				}
				else if(weightproc < 800)
				{
					newWeightPenalty = 2;
				}
				else if(weightproc < 1000)
				{
					newWeightPenalty = 3;
				}
				else
				{
					newWeightPenalty = 4;
				}

				if(_curWeightPenalty != newWeightPenalty)
				{
					_curWeightPenalty = newWeightPenalty;
					if(newWeightPenalty > 0)
					{
						super.addSkill(SkillTable.getInstance().getInfo(4270, newWeightPenalty));
						sendSkillList(); // Fix visual bug
					}
					else
					{
						super.removeSkill(getKnownSkill(4270));
						sendSkillList(); // Fix visual bug
					}

					sendPacket(new EtcStatusUpdate(this));
					Broadcast.toKnownPlayers(this, new CharInfo(this));
				}
			}
		}
		
		sendPacket(new UserInfo(this));
	}

	public void refreshMasteryPenality()
	{
		if (!Config.MASTERY_PENALTY || this.getLevel()<=Config.LEVEL_TO_GET_PENALITY)
			return;
		
		_heavy_mastery = false;
		_light_mastery = false;
		_robe_mastery = false;
		
		L2Skill[] char_skills = this.getAllSkills();
		
		for(L2Skill actual_skill : char_skills){
			
			if(actual_skill.getName().contains("Heavy Armor Mastery")){
				_heavy_mastery = true;
			}
			
			if(actual_skill.getName().contains("Light Armor Mastery")){
				_light_mastery = true;
			}
			
			if(actual_skill.getName().contains("Robe Mastery")){
				_robe_mastery = true;
			}
			
		}
		
		int newMasteryPenalty = 0;
		
		if(_heavy_mastery==false && _light_mastery==false && _light_mastery ==false){ //not completed 1st class transfer or not acquired yet the mastery skills
			
			newMasteryPenalty = 0;
		
		}else{
			
			for(L2ItemInstance item : getInventory().getItems())
			{
				if(item != null && item.isEquipped() && item.getItem() instanceof L2Armor)
				{
					L2Armor armor_item = (L2Armor) item.getItem();
					
					switch(armor_item.getItemType()){
						
						case HEAVY:{
							
							if(!_heavy_mastery)
								newMasteryPenalty++;
						}
						break;
						case LIGHT:{
							
							if(!_light_mastery)
								newMasteryPenalty++;
						}
						break;
						case MAGIC:{
							
							if(!_robe_mastery)
								newMasteryPenalty++;
							
						}
						break;
					default:
						break;

					}
				}
			}

		}
		
		if(_masteryPenalty!=newMasteryPenalty){
			
			int penalties = _masteryWeapPenalty + _expertisePenalty + newMasteryPenalty;
			
			if(penalties > 0)
			{
				super.addSkill(SkillTable.getInstance().getInfo(4267, 1)); // level used to be newPenalty
				sendSkillList(); // Update skill list
			}
			else
			{
				super.removeSkill(getKnownSkill(4267));
				sendSkillList(); // Update skill list
			}
			
			sendPacket(new EtcStatusUpdate(this));
			_masteryPenalty = newMasteryPenalty;
			
		}
		
	}
	
	private boolean _blunt_mastery = false;
	private boolean _pole_mastery = false;
	private boolean _dagger_mastery = false;
	private boolean _sword_mastery = false;
	private boolean _bow_mastery = false;
	private boolean _fist_mastery = false;
	private boolean _dual_mastery = false;
	private boolean _2hands_mastery = false;
	
	private int _masteryWeapPenalty = 0;
	
	public void refreshMasteryWeapPenality()
	{
		if (!Config.MASTERY_WEAPON_PENALTY || this.getLevel() <= Config.LEVEL_TO_GET_WEAPON_PENALITY)
			return;
		
		_blunt_mastery = false;
		_bow_mastery = false;
		_dagger_mastery = false;
		_fist_mastery = false;
		_dual_mastery = false;
		_pole_mastery = false;
		_sword_mastery = false;
		_2hands_mastery = false;
		
		L2Skill[] char_skills = this.getAllSkills();
		
		for(L2Skill actual_skill : char_skills){
			
			if(actual_skill.getName().contains("Blunt Mastery")){
				_blunt_mastery = true;
				continue;
			}
			
			if(actual_skill.getName().contains("Bow Mastery")){
				_bow_mastery = true;
				continue;
			}
			
			if(actual_skill.getName().contains("Dagger Mastery")){
				_dagger_mastery = true;
				continue;
			}

			if(actual_skill.getName().contains("Fist Mastery")){
				_fist_mastery = true;
				continue;
			}

			if(actual_skill.getName().contains("Dual Weapon Mastery")){
				_dual_mastery = true;
				continue;
			}

			if(actual_skill.getName().contains("Polearm Mastery")){
				_pole_mastery = true;
				continue;
			}
			
			if(actual_skill.getName().contains("Sword Blunt Mastery")){
				_sword_mastery = true;
				continue;
			}
			
			if(actual_skill.getName().contains("Two-handed Weapon Mastery")){
				_2hands_mastery = true;
				continue;
			}
		}
		
		int newMasteryPenalty = 0;
		
		if(!_bow_mastery
				&& !_blunt_mastery
				&& !_dagger_mastery 
				&& !_fist_mastery
				&& !_dual_mastery
				&& !_pole_mastery
				&& !_sword_mastery 
				&& !_2hands_mastery){ //not completed 1st class transfer or not acquired yet the mastery skills
			
			newMasteryPenalty = 0;
		
		}
		else
		{
			
			for(L2ItemInstance item : getInventory().getItems())
			{
				if(item != null && item.isEquipped() && item.getItem() instanceof L2Weapon && !isCursedWeaponEquiped())
				{
                    if (item.isCupidBow())
                        continue;
                    
					L2Weapon weap_item = (L2Weapon) item.getItem();
					
					switch(weap_item.getItemType()){
						
						case BIGBLUNT:
						case BIGSWORD:
						{
							
							if(!_2hands_mastery)
								newMasteryPenalty++;
						}
						break;
						case BLUNT:
						{
							
							if(!_blunt_mastery)
								newMasteryPenalty++;
						}
						break;
						case BOW:{
							
							if(!_bow_mastery)
								newMasteryPenalty++;
							
						}
						break;
						case DAGGER:{
							
							if(!_dagger_mastery)
								newMasteryPenalty++;
							
						}
						break;
						case DUAL:{
							
							if(!_dual_mastery)
								newMasteryPenalty++;
							
						}
						break;
						case DUALFIST:
						case FIST:{
							
							if(!_fist_mastery)
								newMasteryPenalty++;
							
						}
						break;
						case POLE:{
							
							if(!_pole_mastery)
								newMasteryPenalty++;
							
						}
						break;
						case SWORD:{
							
							if(!_sword_mastery)
								newMasteryPenalty++;
							
						}
						break;
					default:
						break;
						
					}
				}
			}

		}
		
		if(_masteryWeapPenalty!=newMasteryPenalty){
			
			int penalties = _masteryPenalty + _expertisePenalty + newMasteryPenalty;
			
			if(penalties > 0)
			{
				super.addSkill(SkillTable.getInstance().getInfo(4267, 1)); // level used to be newPenalty
				sendSkillList(); // Update skill list
			}
			else
			{
				super.removeSkill(getKnownSkill(4267));
				sendSkillList(); // Update skill list
			}
			
			sendPacket(new EtcStatusUpdate(this));
			_masteryWeapPenalty = newMasteryPenalty;
			
		}
		
		
	}
	
	public void refreshExpertisePenalty()
	{
		int newPenalty = 0;

		for(L2ItemInstance item : getInventory().getItems())
		{
			if(item != null && item.isEquipped() && ((item.getItemType() != L2EtcItemType.ARROW)))
			{
				int crystaltype = item.getItem().getCrystalType();

				if(crystaltype > newPenalty)
				{
					newPenalty = crystaltype;
				}
			}
		}

		newPenalty = newPenalty - getExpertiseIndex();

		if(newPenalty <= 0 || Config.DISABLE_GRADE_PENALTY)
		{
			newPenalty = 0;
		}

		if(getExpertisePenalty() != newPenalty)
		{
			int penalties = _masteryPenalty + _masteryWeapPenalty + newPenalty;
			
			_expertisePenalty = newPenalty;

			if(penalties > 0)
			{
				super.addSkill(SkillTable.getInstance().getInfo(4267, 1));
				sendSkillList(); // Update skill list
			}
			else
			{
				super.removeSkill(getKnownSkill(4267));
				sendSkillList(); // Update skill list
			}

			sendPacket(new EtcStatusUpdate(this));
			
		}
	}
	
	public void checkIfWeaponIsAllowed()
	{
		if(isGM())
		{
			return;
		}

		for(L2Effect currenteffect : getAllEffects())
		{
			L2Skill effectSkill = currenteffect.getSkill();

			if(currenteffect.getSkill().isToggle() && Config.INTERRUPT_TOGGLE_SKILL_EFFECT)
			{
				currenteffect.exit();
			}
			else if(!effectSkill.isOffensive() && !(effectSkill.getTargetType() == SkillTargetType.TARGET_PARTY && effectSkill.getSkillType() == L2SkillType.BUFF))
			{
				if(!effectSkill.getWeaponDependancy(this))
				{
					sendMessage(effectSkill.getName() + " cannot be used with this weapon.");

					currenteffect.exit();
				}
			}

			continue;
		}
	}

	public void checkSSMatch(L2ItemInstance equipped, L2ItemInstance unequipped)
	{
		if(unequipped == null)
		{
			return;
		}

		if(unequipped.getItem().getType2() == L2Item.TYPE2_WEAPON && (equipped == null ? true : equipped.getItem().getCrystalType() != unequipped.getItem().getCrystalType()))
		{
			for(L2ItemInstance ss : getInventory().getItems())
			{
				int _itemId = ss.getItemId();

				if((_itemId >= 2509 && _itemId <= 2514 || _itemId >= 3947 && _itemId <= 3952 || _itemId <= 1804 && _itemId >= 1808 || _itemId == 5789 || _itemId == 5790 || _itemId == 1835) && ss.getItem().getCrystalType() == unequipped.getItem().getCrystalType())
				{
					sendPacket(new ExAutoSoulShot(_itemId, 0));

					sendPacket(new SystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED).addString(ss.getItemName()));
				}
			}
		}
	}

	/**
	 * Equip or unequip the item.
	 * <UL>
	 * <LI>If item is equipped, shots are applied if automation is on.</LI>
	 * <LI>If item is unequipped, shots are discharged.</LI>
	 * </UL>
	 * @param item The item to charge/discharge.
	 * @param abortAttack If true, the current attack will be aborted in order to equip the item.
	 */
	public void useEquippableItem(L2ItemInstance item, boolean abortAttack)
	{
		L2ItemInstance[] items = null;
		final boolean isEquipped = item.isEquipped();
		final int oldInvLimit = GetInventoryLimit();
		SystemMessage sm = null;
		L2ItemInstance old = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
		
		if (old == null)
			old = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		
		checkSSMatch(item, old);
		
		if (isEquipped)
		{
			if (item.getEnchantLevel() > 0)
				sm = SystemMessage.getSystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED).addNumber(item.getEnchantLevel()).addItemName(item);
			else
				sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISARMED).addItemName(item);
			
			sendPacket(sm);
			
			int slot = getInventory().getSlotFromItem(item);
			items = getInventory().unEquipItemInBodySlotAndRecord(slot);
		}
		else
		{
			items = getInventory().equipItemAndRecord(item);
			
			if (item.isEquipped())
			{
				if (item.getEnchantLevel() > 0)
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1_S2_EQUIPPED).addNumber(item.getEnchantLevel()).addItemName(item);
				else
					sm = SystemMessage.getSystemMessage(SystemMessageId.S1_EQUIPPED).addItemName(item);
				
				sendPacket(sm);
				
				// Consume mana - will start a task if required; returns if item is not a shadow item
				item.decreaseMana(false);
				
				if ((item.getItem().getBodyPart() & L2Item.SLOT_ALLWEAPON) != 0)
					rechargeAutoSoulShot(true, true, false);
			}
			else
				sendPacket(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION);
		}
		refreshExpertisePenalty();
		broadcastUserInfo();
		
		InventoryUpdate iu = new InventoryUpdate();
		iu.addItems(Arrays.asList(items));
		sendPacket(iu);
		
		if (abortAttack)
			abortAttack();
		
		if (GetInventoryLimit() != oldInvLimit)
			sendPacket(new ExStorageMaxCount(this));
	}
	
	public int getPvpKills()
	{
		return _pvpKills;
	}

	public void setPvpKills(int pvpKills)
	{
		_pvpKills = pvpKills;
	}

	public ClassId getClassId()
	{
		return getTemplate().classId;
	}

	public void setClassId(int Id)
	{

		if(getLvlJoinedAcademy() != 0 && _clan != null && PlayerClass.values()[Id].getLevel() == ClassLevel.Third)
		{
			if (getLvlJoinedAcademy() <= 16)
				_clan.setReputationScore(400, true);
			else if (getLvlJoinedAcademy() >= 39)
				_clan.setReputationScore(170, true);
			else
				_clan.setReputationScore((400 - (getLvlJoinedAcademy() - 16) * 10), true);
			
			setLvlJoinedAcademy(0);

			_clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(_clan));
			setLvlJoinedAcademy(0);
			SystemMessage msg = new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_EXPELLED);
			msg.addString(getName());
			_clan.broadcastToOnlineMembers(msg);
			_clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(getName()));
			_clan.removeClanMember(getName(), 0);
			sendPacket(new SystemMessage(SystemMessageId.ACADEMY_MEMBERSHIP_TERMINATED));

			getInventory().addItem("Gift", 8181, 1, this, null);
			getInventory().updateDatabase();
		}

		if(isSubClassActive())
		{
			getSubClasses().get(_classIndex).setClassId(Id);
		}

		broadcastPacket(new MagicSkillUser(this, this, 5103, 1, 1000, 0));
		setClassTemplate(Id);
		
		if (getClassId().level() == 3)
			sendPacket(new SystemMessage(SystemMessageId.THIRD_CLASS_TRANSFER));
		else
			sendPacket(new SystemMessage(SystemMessageId.CLASS_TRANSFER));
		
		// Update class icon in party and clan
		if (isInParty())
			getParty().broadcastToPartyMembers(new PartySmallWindowUpdate(this));
		
		if (getClan() != null)
			getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(this));

		if (Config.AUTO_LEARN_SKILLS)
			rewardSkills();
	}

	public long getExp()
	{
		return getStat().getExp();
	}

	public void setActiveEnchantItem(L2ItemInstance scroll)
	{
		if(scroll == null)
		{
			setIsEnchanting(false);
		}

		_activeEnchantItem = scroll;
	}

	public L2ItemInstance getActiveEnchantItem()
	{
		return _activeEnchantItem;
	}

	public void setFistsWeaponItem(L2Weapon weaponItem)
	{
		_fistsWeaponItem = weaponItem;
	}

	public L2Weapon getFistsWeaponItem()
	{
		return _fistsWeaponItem;
	}

	public L2Weapon findFistsWeaponItem(int classId)
	{
		L2Weapon weaponItem = null;
		if(classId >= 0x00 && classId <= 0x09)
		{
			L2Item temp = ItemTable.getInstance().getTemplate(246);
			weaponItem = (L2Weapon) temp;
			temp = null;
		}
		else if(classId >= 0x0a && classId <= 0x11)
		{
			L2Item temp = ItemTable.getInstance().getTemplate(251);
			weaponItem = (L2Weapon) temp;
			temp = null;
		}
		else if(classId >= 0x12 && classId <= 0x18)
		{
			L2Item temp = ItemTable.getInstance().getTemplate(244);
			weaponItem = (L2Weapon) temp;
			temp = null;
		}
		else if(classId >= 0x19 && classId <= 0x1e)
		{
			L2Item temp = ItemTable.getInstance().getTemplate(249);
			weaponItem = (L2Weapon) temp;
			temp = null;
		}
		else if(classId >= 0x1f && classId <= 0x25)
		{
			L2Item temp = ItemTable.getInstance().getTemplate(245);
			weaponItem = (L2Weapon) temp;
			temp = null;
		}
		else if(classId >= 0x26 && classId <= 0x2b)
		{
			L2Item temp = ItemTable.getInstance().getTemplate(250);
			weaponItem = (L2Weapon) temp;
			temp = null;
		}
		else if(classId >= 0x2c && classId <= 0x30)
		{
			L2Item temp = ItemTable.getInstance().getTemplate(248);
			weaponItem = (L2Weapon) temp;
			temp = null;
		}
		else if(classId >= 0x31 && classId <= 0x34)
		{
			L2Item temp = ItemTable.getInstance().getTemplate(252);
			weaponItem = (L2Weapon) temp;
			temp = null;
		}
		else if(classId >= 0x35 && classId <= 0x39)
		{
			L2Item temp = ItemTable.getInstance().getTemplate(247);
			weaponItem = (L2Weapon) temp;
			temp = null;
		}

		return weaponItem;
	}

	public void rewardSkills()
	{
		int lvl = getLevel();

		if(lvl > 9)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(194, 1);
			skill = removeSkill(skill);

			skill = null;
		}
		for(int i = 0; i < EXPERTISE_LEVELS.length; i++)
		{
			if(lvl >= EXPERTISE_LEVELS[i])
			{
				setExpertiseIndex(i);
			}
		}

		if(getExpertiseIndex() > 0)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(239, getExpertiseIndex());
			addSkill(skill, true);

			skill = null;
		}

		if(getSkillLevel(1321) < 1 && getRace() == Race.dwarf)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(1321, 1);
			addSkill(skill, true);
			skill = null;
		}

		if(getSkillLevel(1322) < 1)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(1322, 1);
			addSkill(skill, true);
			skill = null;
		}
		for(int i = 0; i < COMMON_CRAFT_LEVELS.length; i++)
		{
			if(lvl >= COMMON_CRAFT_LEVELS[i] && getSkillLevel(1320) < i + 1)
			{
				L2Skill skill = SkillTable.getInstance().getInfo(1320, (i + 1));
				addSkill(skill, true);
				skill = null;
			}
		}

		if(Config.AUTO_LEARN_SKILLS)
		{
			if (isCursedWeaponEquipped())
				return;
			giveAvailableSkills();
			sendSkillList();
		}

		if(_clan != null)
		{
			if(_clan.getLevel() > 3 && isClanLeader())
			{
				SiegeManager.getInstance().addSiegeSkills(this);
			}
		}

		refreshOverloaded();
		refreshExpertisePenalty();
		sendSkillList();
	}

	public void regiveTemporarySkills()
	{
		if(isNoble())
		{
			setNoble(true);
		}

		if(isHero())
		{
			setIsHero(true);
		}

		if(isClanLeader())
		{
			setClanLeader(true);
		}

		if(getClan() != null && getClan().getReputationScore() >= 0)
		{
			L2Skill[] skills = getClan().getAllSkills();
			for(L2Skill sk : skills)
			{
				if(sk.getMinPledgeClass() <= getPledgeClass())
				{
					addSkill(sk, false);
				}
			}

			skills = null;
		}

		getInventory().reloadEquippedItems();

	}

	public void giveAvailableSkills()
	{
		int unLearnable = 0;
		int skillCounter = 0;

		L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSkills(this, getClassId());
		while(skills.length > unLearnable)
		{
			unLearnable = 0;
			for(L2SkillLearn s : skills)
			{
				L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
				if(sk == null || (sk.getId() == L2Skill.SKILL_DIVINE_INSPIRATION && !Config.AUTO_LEARN_DIVINE_INSPIRATION))
				{
					unLearnable++;
					continue;
				}

				if(getSkillLevel(sk.getId()) == -1)
				{
					skillCounter++;
				}

				if(sk.isToggle())
				{
					L2Effect toggleEffect = getFirstEffect(sk.getId());
					if(toggleEffect != null)
					{
						toggleEffect.exit();
						sk.getEffects(this, this);
					}
				}

				addSkill(sk, true);
			}

			skills = SkillTreeTable.getInstance().getAvailableSkills(this, getClassId());
		}

		sendMessage("You have learned " + skillCounter + " new skills.");
		skills = null;
	}

	public void setExp(long exp)
	{
		getStat().setExp(exp);
	}

	public Race getRace()
	{
		if(!isSubClassActive())
		{
			return getTemplate().race;
		}

		L2PcTemplate charTemp = CharTemplateTable.getInstance().getTemplate(_baseClass);
		return charTemp.race;
	}

	public L2Radar getRadar()
	{
		return _radar;
	}

	public int getSp()
	{
		return getStat().getSp();
	}

	public void setSp(int sp)
	{
		super.getStat().setSp(sp);
	}

	public boolean isCastleLord(int castleId)
	{
		L2Clan clan = getClan();

		if(clan != null && clan.getLeader().getPlayerInstance() == this)
		{
			Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
			if(castle != null && castle == CastleManager.getInstance().getCastleById(castleId))
			{
				castle = null;
				return true;
			}
		}
		return false;
	}

	public int getClanId()
	{
		return _clanId;
	}

	public int getClanCrestId()
	{
		if(_clan != null && _clan.hasCrest())
		{
			return _clan.getCrestId();
		}

		return 0;
	}

	public int getClanCrestLargeId()
	{
		if(_clan != null && _clan.hasCrestLarge())
		{
			return _clan.getCrestLargeId();
		}

		return 0;
	}

	public long getClanJoinExpiryTime()
	{
		return _clanJoinExpiryTime;
	}

	public void setClanJoinExpiryTime(long time)
	{
		_clanJoinExpiryTime = time;
	}

	public long getClanCreateExpiryTime()
	{
		return _clanCreateExpiryTime;
	}

	public void setClanCreateExpiryTime(long time)
	{
		_clanCreateExpiryTime = time;
	}

	public void setOnlineTime(long time)
	{
		_onlineTime = time;
		_onlineBeginTime = System.currentTimeMillis();
	}

	@Override
	public PcInventory getInventory()
	{
		return _inventory;
	}

	public void removeItemFromShortCut(int objectId)
	{
		_shortCuts.deleteShortCutByObjectId(objectId);
	}

	public boolean isSitting()
	{
		return _waitTypeSitting;
	}

	public void setIsSitting(boolean state)
	{
		_waitTypeSitting = state;
	}

    public void setSitdownTask(boolean act)
    {
        _sitdowntask = act;
    }
    
    public boolean getSitdownTask()
    {
        return _sitdowntask;
    }
    
	public boolean getSittingTask()
	{
		return this.sittingTaskLaunched;
	}

	public void sitDown()
	{
        if(isMoving())
        {
            if(!getSitdownTask())
                setSitdownTask(true);
            else
                setSitdownTask(false);
            return;
        }
        
		if(isCastingNow() && !_relax)
		{
			return;
		}

		if(sittingTaskLaunched)
			return;
		
		if(!_waitTypeSitting && !isAttackingDisabled() && !isOutOfControl() && !isImobilised())
		{
			breakAttack();
			setIsSitting(true);
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_SITTING));
			sittingTaskLaunched = true;
			ThreadPoolManager.getInstance().scheduleGeneral(new SitDownTask(this), 2500);
			setIsParalyzed(true);
		}
	}

	class SitDownTask implements Runnable
	{
		L2PcInstance _player;
		final L2PcInstance this$0;
		
		SitDownTask(L2PcInstance player)
		{
			this$0 = L2PcInstance.this;
			_player = player;
		}

		@Override
		public void run()
		{
			setIsSitting(true);
			_player.setIsParalyzed(false);
			sittingTaskLaunched = false;
			_player.getAI().setIntention(CtrlIntention.AI_INTENTION_REST);
		}
	}

	class StandUpTask implements Runnable
	{
		L2PcInstance _player;

		StandUpTask(L2PcInstance player)
		{
			_player = player;
		}

		@Override
		public void run()
		{
			_player.setIsSitting(false);
			this._player.sittingTaskLaunched = false;
			_player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}
	}

	public void standUp()
	{
        if(sittingTaskLaunched)
        {
            return;
        }
        
		if((this._waitTypeSitting) && (!(this.sittingTaskLaunched)) && (!(isInStoreMode())) && (!(isAlikeDead())))
		{
			if(_relax)
			{
				setRelax(false);
				stopEffects(L2EffectType.RELAXING);
			}

			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STANDING));
			this.sittingTaskLaunched = true;
			ThreadPoolManager.getInstance().scheduleGeneral(new StandUpTask(this), 2500);
		}
	}

	public void setRelax(boolean val)
	{
		_relax = val;
	}

	public PcWarehouse getWarehouse()
	{
		if(_warehouse == null)
		{
			_warehouse = new PcWarehouse(this);
			_warehouse.restore();
		}

		return _warehouse;
	}

	public void clearWarehouse()
	{
		if(_warehouse != null)
		{
			_warehouse.deleteMe();
		}

		_warehouse = null;
	}

	public PcFreight getFreight()
	{
		return _freight;
	}

	public int getCharId()
	{
		return _charId;
	}

	public void setCharId(int charId)
	{
		_charId = charId;
	}

	public int getAdena()
	{
		return _inventory.getAdena();
	}

	public int getAncientAdena()
	{
		return _inventory.getAncientAdena();
	}

	public int getItemCount(int itemId, int enchantLevel)
	{
		return _inventory.getInventoryItemCount(itemId, enchantLevel);
	}

	public void addAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if(count > 0)
		{
			if(_inventory.getAdena() == Integer.MAX_VALUE)
			{
				sendMessage("You have reached the maximum amount of adena, please spend or deposit the adena so you may continue obtaining adena.");
				return;
			}
			else if(_inventory.getAdena() >= Integer.MAX_VALUE - count)
			{
				count = Integer.MAX_VALUE - _inventory.getAdena();
				_inventory.addAdena(process, count, this, reference);
			}
			else if(_inventory.getAdena() < Integer.MAX_VALUE - count)
			{
				_inventory.addAdena(process, count, this, reference);
			}

			if(sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.EARNED_S1_ADENA).addNumber(count));
			}

			if(!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(_inventory.getAdenaInstance());
				sendPacket(iu);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
		}
	}

	public boolean reduceAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if(count > getAdena())
		{
			if(sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			}

			return false;
		}

		if(count > 0)
		{
			L2ItemInstance adenaItem = _inventory.getAdenaInstance();
			_inventory.reduceAdena(process, count, this, reference);

			if(!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(adenaItem);
				sendPacket(iu);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}

			if(sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.DISAPPEARED_ADENA).addNumber(count));
			}
			adenaItem = null;
		}

		return true;
	}

	public void addAncientAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if(sendMessage)
		{
			sendPacket(new SystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(PcInventory.ANCIENT_ADENA_ID).addNumber(count));
		}

		if(count > 0)
		{
			_inventory.addAncientAdena(process, count, this, reference);

			if(!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(_inventory.getAncientAdenaInstance());
				sendPacket(iu);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
		}
	}

	public boolean reduceAncientAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if(count > getAncientAdena())
		{
			if(sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			}

			return false;
		}

		if(count > 0)
		{
			L2ItemInstance ancientAdenaItem = _inventory.getAncientAdenaInstance();
			_inventory.reduceAncientAdena(process, count, this, reference);

			if(!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(ancientAdenaItem);
				sendPacket(iu);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}

			if(sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addNumber(count).addItemName(PcInventory.ANCIENT_ADENA_ID));
			}
			ancientAdenaItem = null;
		}

		return true;
	}

	public void addItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		if(item.getCount() > 0)
		{
			if(sendMessage)
			{
				if(item.getCount() > 1)
				{
					if(item.isStackable() && !Config.MULTIPLE_ITEM_DROP)
					{
						sendPacket(new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1).addItemName(item.getItemId()));
					}
					else
					{
						sendPacket(new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2).addItemName(item.getItemId()).addNumber(item.getCount()));
					}
				}
				else if(item.getEnchantLevel() > 0)
				{
					sendPacket(new SystemMessage(SystemMessageId.YOU_PICKED_UP_A_S1_S2).addNumber(item.getEnchantLevel()).addItemName(item.getItemId()));
				}
				else
				{
					sendPacket(new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1).addItemName(item.getItemId()));
				}
			}

			L2ItemInstance newitem = _inventory.addItem(process, item, this, reference);

			if(!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate playerIU = new InventoryUpdate();
				playerIU.addItem(newitem);
				sendPacket(playerIU);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}

			StatusUpdate su = new StatusUpdate(getObjectId());
			su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
			sendPacket(su);

			if(!isGM() && !_inventory.validateCapacity(0))
			{
				dropItem("InvDrop", newitem, null, true, true);
			}
			else if(CursedWeaponsManager.getInstance().isCursed(newitem.getItemId()))
			{
				CursedWeaponsManager.getInstance().activate(this, newitem);
			}
		}
	}

	public void addItem(String process, int itemId, int count, L2Object reference, boolean sendMessage)
	{
		if(count > 0)
		{
			L2ItemInstance ditem = ItemTable.getInstance().createDummyItem(itemId);
            if (ditem == null)
                return;
			if (sendMessage && (!isCastingNow() && ditem.getItemType() == L2EtcItemType.HERB || ditem.getItemType() != L2EtcItemType.HERB))
			{
				if(count > 1)
				{
					if(process.equalsIgnoreCase("sweep") || process.equalsIgnoreCase("Quest"))
					{
						sendPacket(new SystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(itemId).addNumber(count));
					}
					else
					{
						sendPacket(new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2).addItemName(itemId).addNumber(count));
					}
				}
				else
				{
					if(process.equalsIgnoreCase("sweep") || process.equalsIgnoreCase("Quest"))
					{
						sendPacket(new SystemMessage(SystemMessageId.EARNED_ITEM_S1).addItemName(itemId));
					}
					else
					{
						sendPacket(new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1).addItemName(itemId));
					}
				}
			}

			if(ItemTable.getInstance().createDummyItem(itemId).getItemType() == L2EtcItemType.HERB)
			{
				if(!isCastingNow())
				{
					L2ItemInstance herb = new L2ItemInstance(_charId, itemId);
					IItemHandler handler = ItemHandler.getInstance().getItemHandler(herb.getItemId());

					if (handler != null)
					{
						handler.useItem(this, herb);

						if(_herbstask >= 100)
						{
							_herbstask -= 100;
						}
					}
				}
				else
				{
					_herbstask += 100;
					ThreadPoolManager.getInstance().scheduleAi(new HerbTask(process, itemId, count, reference, sendMessage), _herbstask);
				}
			}
			else
			{
				L2ItemInstance item = _inventory.addItem(process, itemId, count, this, reference);

				if(!Config.FORCE_INVENTORY_UPDATE)
				{
					InventoryUpdate playerIU = new InventoryUpdate();
					playerIU.addItem(item);
					sendPacket(playerIU);
				}
				else
				{
					sendPacket(new ItemList(this, false));
				}

				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
				sendPacket(su);

				if(!isGM() && !_inventory.validateCapacity(0))
				{
					dropItem("InvDrop", item, null, true);
				}
				else if(CursedWeaponsManager.getInstance().isCursed(item.getItemId()))
				{
					CursedWeaponsManager.getInstance().activate(this, item);
				}
			}
		}
	}
	
	public boolean destroyItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		return this.destroyItem(process, item, item.getCount(), reference, sendMessage);
	}

	public boolean destroyItem(String process, L2ItemInstance item, int count, L2Object reference, boolean sendMessage)
	{
		int oldCount = item.getCount();
		item = _inventory.destroyItem(process, item, this, reference);

		if(item == null)
		{
			if(sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}

			return false;
		}

		if(!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}

		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);

		if(sendMessage)
		{
			sendPacket(new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addNumber(oldCount).addItemName(item.getItemId()));
		}

		return true;
	}

	@Override
	public boolean destroyItem(String process, int objectId, int count, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance item = _inventory.getItemByObjectId(objectId);

		if(item == null || item.getCount() < count || _inventory.destroyItem(process, objectId, count, this, reference) == null)
		{
			if(sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}

			return false;
		}

		if(!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}

		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);

		if(sendMessage)
		{
			sendPacket(new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addNumber(count).addItemName(item.getItemId()));
		}

		return true;
	}

	public boolean destroyItemWithoutTrace(String process, int objectId, int count, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance item = _inventory.getItemByObjectId(objectId);

		if(item == null || item.getCount() < count)
		{
			if(sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}

			return false;
		}

		if(item.getCount() > count)
		{
			synchronized (item)
			{
				item.changeCountWithoutTrace(process, -count, this, reference);
				item.setLastChange(L2ItemInstance.MODIFIED);

				if(GameTimeController.getGameTicks() % 10 == 0)
				{
					item.updateDatabase();
				}
				_inventory.refreshWeight();
			}
		}
		else
		{
			_inventory.destroyItem(process, item, this, reference);
		}

		if(!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}

		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);

		if(sendMessage)
		{
			sendPacket(new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addNumber(count).addItemName(item.getItemId()));
		}
		return true;
	}

	@Override
	public boolean destroyItemByItemId(String process, int itemId, int count, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance item = _inventory.getItemByItemId(itemId);

		if(item == null || item.getCount() < count || _inventory.destroyItemByItemId(process, itemId, count, this, reference) == null)
		{
			if(sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}

			return false;
		}

		if(!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}

		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);

		if(sendMessage)
		{
			sendPacket(new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addNumber(count).addItemName(itemId));
		}

		return true;
	}

	public void destroyWearedItems(String process, L2Object reference, boolean sendMessage)
	{
		for(L2ItemInstance item : getInventory().getItems())
		{
			if(item.isWear())
			{
				if(item.isEquipped())
				{
					getInventory().unEquipItemInSlotAndRecord(item.getEquipSlot());
				}

				if(_inventory.destroyItem(process, item, this, reference) == null)
				{
					_log.warn("Player " + getName() + " can't destroy weared item: " + item.getName() + "[ " + item.getObjectId() + " ]");
					continue;
				}

				sendPacket(new SystemMessage(SystemMessageId.S1_DISARMED).addItemName(item.getItemId()));
			}
		}

		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);

		ItemList il = new ItemList(getInventory().getItems(), true);
		sendPacket(il);

		broadcastUserInfo();

		sendMessage("Trying-on mode has ended.");
	}

	public L2ItemInstance transferItem(String process, int objectId, int count, Inventory target, L2Object reference)
	{
		L2ItemInstance oldItem = checkItemManipulation(objectId, count, "transfer");
		if(oldItem == null)
		{
			return null;
		}

		L2ItemInstance newItem = getInventory().transferItem(process, objectId, count, target, this, reference);
		if(newItem == null)
		{
			return null;
		}

		if(!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();

			if(oldItem.getCount() > 0 && oldItem != newItem)
			{
				playerIU.addModifiedItem(oldItem);
			}
			else
			{
				playerIU.addRemovedItem(oldItem);
			}

			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}

		StatusUpdate playerSU = new StatusUpdate(getObjectId());
		playerSU.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(playerSU);

		if(target instanceof PcInventory)
		{
			L2PcInstance targetPlayer = ((PcInventory) target).getOwner();

			if(!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate playerIU = new InventoryUpdate();

				if(newItem.getCount() > count)
				{
					playerIU.addModifiedItem(newItem);
				}
				else
				{
					playerIU.addNewItem(newItem);
				}

				targetPlayer.sendPacket(playerIU);
			}
			else
			{
				targetPlayer.sendPacket(new ItemList(targetPlayer, false));
			}

			playerSU = new StatusUpdate(targetPlayer.getObjectId());
			playerSU.addAttribute(StatusUpdate.CUR_LOAD, targetPlayer.getCurrentLoad());
			targetPlayer.sendPacket(playerSU);
		}
		else if(target instanceof PetInventory)
		{
			PetInventoryUpdate petIU = new PetInventoryUpdate();

			if(newItem.getCount() > count)
			{
				petIU.addModifiedItem(newItem);
			}
			else
			{
				petIU.addNewItem(newItem);
			}

			((PetInventory) target).getOwner().getOwner().sendPacket(petIU);
		}
		return newItem;
	}

	public boolean dropItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage, boolean protectItem)
	{
		item = _inventory.dropItem(process, item, this, reference);

		if(item == null)
		{
			if(sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}

			return false;
		}

		item.dropMe(this, getX() + Rnd.get(50) - 25, getY() + Rnd.get(50) - 25, getZ() + 20);

		if(Config.DESTROY_DROPPED_PLAYER_ITEM && !Config.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
		{
			if(Config.AUTODESTROY_ITEM_AFTER > 0)
			{
				if(item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM || !item.isEquipable())
				{
					ItemsAutoDestroy.getInstance().addItem(item);
					item.setProtected(false);
				}
				else
				{
					item.setProtected(true);
				}
			}
			else
			{
				item.setProtected(true);
			}
		}
		else
		{
			item.setProtected(true);
		}

		if(protectItem)
		{
			item.getDropProtection().protect(this);
		}

		if(!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}

		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);

		if(sendMessage)
		{
			sendPacket(new SystemMessage(SystemMessageId.YOU_DROPPED_S1).addItemName(item.getItemId()));
		}

		return true;
	}

	public boolean dropItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		return dropItem(process, item, reference, sendMessage, false);
	}

	public L2ItemInstance dropItem(String process, int objectId, int count, int x, int y, int z, L2Object reference, boolean sendMessage, boolean protectItem)
	{
		L2ItemInstance invitem = _inventory.getItemByObjectId(objectId);
		L2ItemInstance item = _inventory.dropItem(process, objectId, count, this, reference);

		if(item == null)
		{
			if(sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}

			return null;
		}

		item.dropMe(this, x, y, z);

		if(Config.AUTODESTROY_ITEM_AFTER > 0 && Config.DESTROY_DROPPED_PLAYER_ITEM && !Config.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
		{
			if(item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM || !item.isEquipable())
			{
				ItemsAutoDestroy.getInstance().addItem(item);
			}
		}
		if(Config.DESTROY_DROPPED_PLAYER_ITEM)
		{
			if(!item.isEquipable() || item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM)
			{
				item.setProtected(false);
			}
			else
			{
				item.setProtected(true);
			}
		}
		else
		{
			item.setProtected(true);
		}

		if(protectItem)
		{
			item.getDropProtection().protect(this);
		}

		if(!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(invitem);
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}

		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);

		if(sendMessage)
		{
			sendPacket(new SystemMessage(SystemMessageId.YOU_DROPPED_S1).addItemName(item.getItemId()));
		}
		invitem = null;

		return item;
	}

	public L2ItemInstance checkItemManipulation(int objectId, int count, String action)
	{
		if(L2World.getInstance().findObject(objectId) == null)
		{
			_log.info(getObjectId() + ": player tried to " + action + " item not available in L2World");
			return null;
		}

		L2ItemInstance item = getInventory().getItemByObjectId(objectId);

		if(item == null || item.getOwnerId() != getObjectId())
		{
			_log.info(getObjectId() + ": player tried to " + action + " item he is not owner of");
			return null;
		}

		if(count < 0 || count > 1 && !item.isStackable())
		{
			_log.info(getObjectId() + ": player tried to " + action + " item with invalid count: " + count);
			return null;
		}

		if(count > item.getCount())
		{
			_log.info(getObjectId() + ": player tried to " + action + " more items than he owns");
			return null;
		}

		if(getPet() != null && getPet().getControlItemId() == objectId || getMountObjectID() == objectId)
		{
			return null;
		}

		if(getActiveEnchantItem() != null && getActiveEnchantItem().getObjectId() == objectId)
		{
			return null;
		}

		if(item.isWear())
		{
			return null;
		}

		if(item.isAugmented() && (isCastingNow() || this.isCastingSimultaneouslyNow()))
		{
			return null;
		}

		return item;
	}

	public void setProtection(boolean protect)
	{
		int proTime = Config.PLAYER_SPAWN_PROTECTION;
		if(protect && (proTime == 0 || isInOlympiadMode()))
		{
			return;
		}

		_protectEndTime = protect ? GameTimeController.getGameTicks() + proTime * GameTimeController.TICKS_PER_SECOND : 0;
	}

	public void setRecentFakeDeath(boolean protect)
	{
		_recentFakeDeathEndTime = protect ? GameTimeController.getGameTicks() + Config.PLAYER_FAKEDEATH_UP_PROTECTION * GameTimeController.TICKS_PER_SECOND : 0;
	}

	public boolean isRecentFakeDeath()
	{
		return _recentFakeDeathEndTime > GameTimeController.getGameTicks();
	}

	public L2GameClient getClient()
	{
		return _client;
	}

	public void setClient(L2GameClient client)
	{
		if(client == null && _client != null)
		{
			_client.stopGuardTask();
			nProtect.getInstance().closeSession(_client);
		}

		_client = client;
	}

	public void closeNetConnection()
	{
		if(_client != null)
		{
			_client.close(new LeaveWorld());
			setClient(null);
		}
	}

	public boolean canTarget()
	{
		if (isOutOfControl())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		return true;
	}
	
	@Override
	public void onAction(L2PcInstance player)
	{
		if(player.isOutOfControl())
		{
			return;
		}

		if(player.getTarget() != this)
		{
			player.setTarget(this);

			player.sendPacket(new MyTargetSelected(getObjectId(), 0));
			if(player != this)
			{
				player.sendPacket(new ValidateLocation(this));
			}
		}
		else
		{
			if(player != this)
			{
				player.sendPacket(new ValidateLocation(this));
			}

			if(getPrivateStoreType() != 0)
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				
				if(canInteract(player))
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
				}
			}
			else
			{
				if(isAutoAttackable(player) || player.isInEvent())
				{
					if(player.getLevel() < Config.PLAYER_PROTECTION_LEVEL || getLevel() < Config.PLAYER_PROTECTION_LEVEL)
					{
						player.sendMessage("You can't hit a player that is lower level from You. Target's level: " + String.valueOf(Config.PLAYER_PROTECTION_LEVEL));
						player.sendPacket(ActionFailed.STATIC_PACKET);
					}
					else if(isCursedWeaponEquiped() && player.getLevel() < 21 || player.isCursedWeaponEquiped() && getLevel() < 21)
					{
						player.sendPacket(ActionFailed.STATIC_PACKET);
					}
					else
					{
						if(Config.GEODATA > 0)
						{
							if(GeoData.getInstance().canSeeTarget(player, this))
							{
								player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
								player.onActionRequest();
							}
						}
						else
						{
							player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
							player.onActionRequest();
						}
					}
				}
				else
				{
					// Avoids to stuck when clicking two or more times
					player.sendPacket(ActionFailed.STATIC_PACKET);

					if(Config.GEODATA > 0)
					{
						if(GeoData.getInstance().canSeeTarget(player, this))
						{
							player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
						}
					}
					else
					{
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
					}
				}
			}
		}
	}

	@Override
	public void onActionShift(L2GameClient client)
	{
		L2PcInstance gm = client.getActiveChar();
		if(gm.isGM())
		{
			if(this != gm.getTarget())
			{
				gm.setTarget(this);
				gm.sendPacket(new MyTargetSelected(getObjectId(), 0));
				if(gm != this)
				{
					gm.sendPacket(new ValidateLocation(this));
				}
			}
			else
			{
				IAdminCommandHandler ach = AdminCommandHandler.getInstance().getAdminCommandHandler("admin_character_info");
				if(ach != null)
				{
					ach.useAdminCommand("admin_character_info " + getName(), gm);
				}
			}
		}
		gm.sendPacket(ActionFailed.STATIC_PACKET);

		gm = null;
	}

	private boolean needCpUpdate(int barPixels)
	{
		double currentCp = getCurrentCp();

		if(currentCp <= 1.0 || getMaxCp() < barPixels)
		{
			return true;
		}

		if(currentCp <= _cpUpdateDecCheck || currentCp >= _cpUpdateIncCheck)
		{
			if(currentCp == getMaxCp())
			{
				_cpUpdateIncCheck = currentCp + 1;
				_cpUpdateDecCheck = currentCp - _cpUpdateInterval;
			}
			else
			{
				double doubleMulti = currentCp / _cpUpdateInterval;
				int intMulti = (int) doubleMulti;

				_cpUpdateDecCheck = _cpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_cpUpdateIncCheck = _cpUpdateDecCheck + _cpUpdateInterval;
			}

			return true;
		}

		return false;
	}

	private boolean needMpUpdate(int barPixels)
	{
		double currentMp = getCurrentMp();

		if(currentMp <= 1.0 || getMaxMp() < barPixels)
		{
			return true;
		}

		if(currentMp <= _mpUpdateDecCheck || currentMp >= _mpUpdateIncCheck)
		{
			if(currentMp == getMaxMp())
			{
				_mpUpdateIncCheck = currentMp + 1;
				_mpUpdateDecCheck = currentMp - _mpUpdateInterval;
			}
			else
			{
				double doubleMulti = currentMp / _mpUpdateInterval;
				int intMulti = (int) doubleMulti;

				_mpUpdateDecCheck = _mpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_mpUpdateIncCheck = _mpUpdateDecCheck + _mpUpdateInterval;
			}

			return true;
		}

		return false;
	}

	@Override
	public void broadcastStatusUpdate()
	{
		StatusUpdate su = new StatusUpdate(getObjectId()); 
		su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp()); 
		su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp()); 
		su.addAttribute(StatusUpdate.CUR_CP, (int) getCurrentCp()); 
		su.addAttribute(StatusUpdate.MAX_CP, getMaxCp()); 
		sendPacket(su);

		if(isInParty() && (needCpUpdate(352) || super.needHpUpdate(352) || needMpUpdate(352)))
		{
			PartySmallWindowUpdate update = new PartySmallWindowUpdate(this);
			getParty().broadcastToPartyMembers(this, update);
			update = null;
		}

		if(isInOlympiadMode() && isOlympiadStart())
		{
			if(Olympiad.getInstance().getPlayers(_olympiadGameId) != null)
			{
				for(L2PcInstance player : Olympiad.getInstance().getPlayers(_olympiadGameId))
				{
					if(player != null && player != this)
					{
						player.sendPacket(new ExOlympiadUserInfo(this, 1));
					}
				}
			}

			if(Olympiad.getInstance().getSpectators(_olympiadGameId) != null)
			{
				for(L2PcInstance spectator : Olympiad.getInstance().getSpectators(_olympiadGameId))
				{
					if(spectator == null)
					{
						continue;
					}
					spectator.sendPacket(new ExOlympiadUserInfo(this, getOlympiadSide()));
				}
			}
		}

		if(isInDuel())
		{
			ExDuelUpdateUserInfo update = new ExDuelUpdateUserInfo(this);
			DuelManager.getInstance().broadcastToOppositTeam(this, update);
			update = null;
		}
	}

	public void updatePvPColor()
	{
		if(Config.PVP_COLOR_SYSTEM_ENABLED)
		{
			//Check if the character has GM access and if so, let them be.
			if(isGM())
				return;
			
			Set<Integer> pvpscolors = Config.PVP_COLOR_LIST.keySet();
			for(Integer i : pvpscolors)
			{
				if(getPvpKills() >= i)
				{
					getAppearance().setNameColor(Config.PVP_COLOR_LIST.get(i));
				}
			}
		}
	}
	
	public void updatePkColor()
	{
		if(Config.PK_COLOR_SYSTEM_ENABLED)
		{
			//Check if the character has GM access and if so, let them be, like above.
			if(isGM())
				return;
			
			Set<Integer> pkscolors = Config.PK_COLOR_LIST.keySet();
			for(Integer i : pkscolors)
			{
				if(getPkKills() >= i)
				{
					getAppearance().setTitleColor(Config.PK_COLOR_LIST.get(i));
				}
			}
		}
	}
	
	public final void broadcastUserInfo()
	{
		sendPacket(new UserInfo(this));

		Broadcast.toKnownPlayers(this, new CharInfo(this));
	}

	public final void broadcastTitleInfo()
	{
		sendPacket(new UserInfo(this));

		Broadcast.toKnownPlayers(this, new TitleUpdate(this));
	}

	public int getAllyId()
	{
		if(_clan == null)
		{
			return 0;
		}
		else
		{
			return _clan.getAllyId();
		}
	}

	public int getAllyCrestId()
	{
		if(getClanId() == 0 || getClan()==null)
		{
			return 0;
		}

		if(getClan().getAllyId() == 0)
		{
			return 0;
		}

		return getClan().getAllyCrestId();
	}

	@Override
	protected void onHitTimer(L2Character target, int damage, boolean crit, boolean miss, boolean soulshot, boolean shld)
	{
		super.onHitTimer(target, damage, crit, miss, soulshot, shld);
	}

	@Override
	public void sendPacket(L2GameServerPacket packet)
	{
		if(_client != null)
		{
			_client.sendPacket(packet);
		}
	}

	public void doInteract(L2Character target)
	{
		if(target instanceof L2PcInstance)
		{
			L2PcInstance temp = (L2PcInstance) target;
			sendPacket(ActionFailed.STATIC_PACKET);

			if(temp.getPrivateStoreType() == STORE_PRIVATE_SELL || temp.getPrivateStoreType() == STORE_PRIVATE_PACKAGE_SELL)
			{
				sendPacket(new PrivateStoreListSell(this, temp));
			}
			else if(temp.getPrivateStoreType() == STORE_PRIVATE_BUY)
			{
				sendPacket(new PrivateStoreListBuy(this, temp));
			}
			else if(temp.getPrivateStoreType() == STORE_PRIVATE_MANUFACTURE)
			{
				sendPacket(new RecipeShopSellList(this, temp));
			}

			temp = null;
		}
		else
		{
			if(target != null)
			{
				target.onAction(this);
			}
		}
	}

	public void doAutoLoot(L2Attackable target, L2Attackable.RewardItem item)
	{
		if(isInParty())
			getParty().distributeItem(this, item, false, target);
		else if(item.getItemId() == 57)
			addAdena("AutoLoot", item.getCount(), target, true);
		else
			addItem("AutoLoot", item.getItemId(), item.getCount(), target, true);
	}

	protected void doPickupItem(L2Object object)
	{
		if(isAlikeDead() || isFakeDeath())
		{
			return;
		}

		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);

		if(!(object instanceof L2ItemInstance))
		{
			_log.warn("trying to pickup wrong target." + getTarget());
			return;
		}

		L2ItemInstance target = (L2ItemInstance) object;

		sendPacket(ActionFailed.STATIC_PACKET);

		StopMove sm = new StopMove(getObjectId(), getX(), getY(), getZ(), getHeading());
		sendPacket(sm);
		sm = null;

		synchronized (target)
		{
			if(!target.isVisible())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			if(!target.getDropProtection().tryPickUp(this))
			{
				sendPacket(new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1).addItemName(target));
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			if((isInParty() && getParty().getLootDistribution() == L2Party.ITEM_LOOTER || !isInParty()) && !_inventory.validateCapacity(target))
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
				return;
			}

			if(isInvul() && !isGM())
			{
				sendPacket(new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1).addItemName(target.getItemId()));
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if(target.getOwnerId() != 0 && target.getOwnerId() != getObjectId() && !isInLooterParty(target.getOwnerId()))
			{
				sendPacket(ActionFailed.STATIC_PACKET);

				if(target.getItemId() == 57)
				{
					sendPacket(new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1_ADENA).addNumber(target.getCount()));
				}
				else if(target.getCount() > 1)
				{
					sendPacket(new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S2_S1_S).addItemName(target.getItemId()).addNumber(target.getCount()));
				}
				else
				{
					sendPacket(new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1).addItemName(target.getItemId()));
				}

				return;
			}

			if(target.getItemLootShedule() != null && (target.getOwnerId() == getObjectId() || isInLooterParty(target.getOwnerId())))
			{
				target.resetOwnerTimer();
			}

			target.pickupMe(this);
			target.getDropProtection().unprotect();

			if(Config.SAVE_DROPPED_ITEM)
			{
				ItemsOnGroundManager.getInstance().removeObject(target);
			}

		}

		if(target.getItemType() == L2EtcItemType.HERB)
		{
			IItemHandler handler = ItemHandler.getInstance().getItemHandler(target.getItemId());
			if(handler == null)
			{
				_log.warn("No item handler registered for item ID " + target.getItemId() + ".");
			}
			else
			{
				handler.useItem(this, target);
				ItemTable.getInstance().destroyItem("Consume", target, this, null);
				handler = null;
			}
		}
		else if(CursedWeaponsManager.getInstance().isCursed(target.getItemId()))
		{
			addItem("Pickup", target, null, true);
		}
		else
		{
			if(target.getItemType() instanceof L2ArmorType || target.getItemType() instanceof L2WeaponType)
			{
				if (target.getEnchantLevel() > 0)
				{
					SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.ATTENTION_S1_PICKED_UP_S2_S3);
					msg.addString(getName());
					msg.addNumber(target.getEnchantLevel());
					msg.addItemName(target.getItemId());
					broadcastPacket(msg, 1400);
				}
				else
				{
					SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.ATTENTION_S1_PICKED_UP_S2);
					msg.addString(getName());
					msg.addItemName(target.getItemId());
					broadcastPacket(msg, 1400);
				}
			}

			if(isInParty())
			{
				getParty().distributeItem(this, target);
			}
			else if(target.getItemId() == 57 && getInventory().getAdenaInstance() != null)
			{
				addAdena("Pickup", target.getCount(), null, true);
				ItemTable.getInstance().destroyItem("Pickup", target, this, null);
			}
			else
			{
				addItem("Pickup", target, null, true);
				
				final L2ItemInstance weapon = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
				if (weapon != null)
				{
					final L2EtcItem etcItem = target.getEtcItem();
					if (etcItem != null)
					{
						final L2EtcItemType itemType = etcItem.getItemType();
						if ((weapon.getItemType() == L2WeaponType.BOW) && (itemType == L2EtcItemType.ARROW))
						{
							checkAndEquipArrows();
						}
					}
				}
			}
		}

		target = null;
	}

	@Override
	public void setTarget(L2Object newTarget)
	{
		if(newTarget != null && !newTarget.isVisible())
		{
			newTarget = null;
		}

		if(newTarget != null)
		{
			if(!(newTarget instanceof L2PcInstance) || !isInParty() || !((L2PcInstance) newTarget).isInParty() || getParty().getPartyLeaderOID() != ((L2PcInstance) newTarget).getParty().getPartyLeaderOID())
			{
				if(Math.abs(newTarget.getZ() - getZ()) > Config.DIFFERENT_Z_NEW_MOVE)
				{
					newTarget = null;
				}
			}
		}

		if(!isGM())
		{
			if(newTarget instanceof L2FestivalMonsterInstance && !isFestivalParticipant())
			{
				newTarget = null;
			}
			else if(isInParty() && getParty().isInDimensionalRift())
			{
				byte riftType = getParty().getDimensionalRift().getType();
				byte riftRoom = getParty().getDimensionalRift().getCurrentRoom();

				if(newTarget != null && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(newTarget.getX(), newTarget.getY(), newTarget.getZ()))
				{
					newTarget = null;
				}
			}
		}

		L2Object oldTarget = getTarget();

		if(oldTarget != null)
		{
			if(oldTarget.equals(newTarget))
			{
				return;
			}

			if(oldTarget instanceof L2Character)
			{
				((L2Character) oldTarget).removeStatusListener(this);
			}
		}

		if (newTarget instanceof L2Character)
		{
			((L2Character) newTarget).addStatusListener(this);
			broadcastPacket(new TargetSelected(getObjectId(), newTarget.getObjectId(), getX(), getY(), getZ()));
		}

		if(newTarget == null && getTarget() != null)
		{
			broadcastPacket(new TargetUnselected(this));
		}

		super.setTarget(newTarget);
	}

	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
	}

	@Override
	public L2Weapon getActiveWeaponItem()
	{
		L2ItemInstance weapon = getActiveWeaponInstance();

		if(weapon == null)
		{
			return getFistsWeaponItem();
		}

		return (L2Weapon) weapon.getItem();
	}

	public L2ItemInstance getChestArmorInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
	}

	public L2Armor getActiveChestArmorItem()
	{
		L2ItemInstance armor = getChestArmorInstance();

		if(armor == null)
		{
			return null;
		}

		return (L2Armor) armor.getItem();
	}

	public boolean isWearingHeavyArmor()
	{
		L2ItemInstance armor = getChestArmorInstance();

		if((L2ArmorType) armor.getItemType() == L2ArmorType.HEAVY)
		{
			armor = null;
			return true;
		}

		armor = null;
		return false;
	}

	public boolean isWearingLightArmor()
	{
		L2ItemInstance armor = getChestArmorInstance();

		if((L2ArmorType) armor.getItemType() == L2ArmorType.LIGHT)
		{
			armor = null;
			return true;
		}

		armor = null;
		return false;
	}

	public boolean isWearingMagicArmor()
	{
		L2ItemInstance armor = getChestArmorInstance();

		if((L2ArmorType) armor.getItemType() == L2ArmorType.MAGIC)
		{
			armor = null;
			return true;
		}

		armor = null;
		return false;
	}

	public boolean isWearingFormalWear()
	{
		return _IsWearingFormalWear;
	}

	public void setIsWearingFormalWear(boolean value)
	{
		_IsWearingFormalWear = value;
	}

	public boolean isMarried()
	{
		return _married;
	}

	public void setMarried(boolean state)
	{
		_married = state;
	}

	public int marriedType()
	{
		return _marriedType;
	}

	public void setmarriedType(int type)
	{
		_marriedType = type;
	}

	public boolean isEngageRequest()
	{
		return _engagerequest;
	}

	public void setEngageRequest(boolean state, int playerid)
	{
		_engagerequest = state;
		_engageid = playerid;
	}

	public void setMaryRequest(boolean state)
	{
		_marryrequest = state;
	}

	public boolean isMaryRequest()
	{
		return _marryrequest;
	}

	public void setMarryAccepted(boolean state)
	{
		_marryaccepted = state;
	}

	public boolean isMarryAccepted()
	{
		return _marryaccepted;
	}

	public int getEngageId()
	{
		return _engageid;
	}

	public int getPartnerId()
	{
		return _partnerId;
	}

	public void setPartnerId(int partnerid)
	{
		_partnerId = partnerid;
	}

	public int getCoupleId()
	{
		return _coupleId;
	}

	public void setCoupleId(int coupleId)
	{
		_coupleId = coupleId;
	}

	public void EngageAnswer(int answer)
	{
		if(_engagerequest == false)
		{
			return;
		}
		else if(_engageid == 0)
		{
			return;
		}
		else
		{
			L2PcInstance ptarget = (L2PcInstance) L2World.getInstance().findObject(_engageid);
			setEngageRequest(false, 0);
			if(ptarget != null)
			{
				if(answer == 1)
				{
					CoupleManager.getInstance().createCouple(ptarget, L2PcInstance.this);
					ptarget.sendMessage("Request to Engage has been accepted.");
				}
				else
				{
					ptarget.sendMessage("Request to Engage has been denied!");
				}

				ptarget = null;
			}
		}
	}

	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
	}

	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		L2ItemInstance weapon = getSecondaryWeaponInstance();

		if(weapon == null)
		{
			return getFistsWeaponItem();
		}

		L2Item item = weapon.getItem();

		if(item instanceof L2Weapon)
		{
			return (L2Weapon) item;
		}

		weapon = null;
		return null;
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		boolean charmOfCourage = getCharmOfCourage();
		
		if(!super.doDie(killer))
		{
			return false;
		}

		Castle castle = null;
		if(getClan() != null)
		{
			castle = CastleManager.getInstance().getCastleByOwner(getClan());
			if(castle != null)
			{
				castle.destroyClanGate();
				castle = null;
			}
		}

		if(_summon != null && _summon instanceof L2PetInstance)
		{
			_summon.unSummon(this);
		}

		if(killer != null)
		{
			L2PcInstance pk = null;

			if(killer instanceof L2PcInstance)
			{
				pk = (L2PcInstance) killer;
			}

			if(atEvent && pk != null)
			{
				pk.kills.add(getName());
			}
			
			setExpBeforeDeath(0);

			if(isCursedWeaponEquiped())
			{
				CursedWeaponsManager.getInstance().drop(_cursedWeaponEquipedId, killer);
			}
			else
			{
				if(pk == null || !pk.isCursedWeaponEquiped())
				{
					onDieDropItem(killer);

					if(!(isInsideZone(ZONE_PVP) && !isInsideZone(ZONE_SIEGE)))
					{
						boolean isKillerPc = killer instanceof L2PcInstance;
						if(isKillerPc && ((L2PcInstance) killer).getClan() != null && getClan() != null && !isAcademyMember() && !((L2PcInstance) killer).isAcademyMember() && _clan.isAtWarWith(((L2PcInstance) killer).getClanId()) && ((L2PcInstance) killer).getClan().isAtWarWith(_clan.getClanId()))
						{
							if(getClan().getReputationScore() > 0)
							{
								((L2PcInstance) killer).getClan().setReputationScore(((L2PcInstance) killer).getClan().getReputationScore() + 1, true);
							}

							if(((L2PcInstance) killer).getClan().getReputationScore() > 0)
							{
								_clan.setReputationScore(_clan.getReputationScore() - 1, true);
							}
						}
						
						if(Config.ALT_GAME_DELEVEL)
						{
							if(getSkillLevel(L2Skill.SKILL_LUCKY) < 0 || getStat().getLevel() > 9)
							{
								deathPenalty((pk != null && getClan() != null && pk.getClan() != null && pk.getClan().isAtWarWith(getClanId())), charmOfCourage);
							}
						}
						else
						{
							onDieUpdateKarma();
						}
					}
				}
			}

			pk = null;
		}
		
		//setPvpFlag(0);

		if(_cubics.size() > 0)
		{
			for(L2CubicInstance cubic : _cubics.values())
			{
				cubic.stopAction();
				cubic.cancelDisappear();
			}

			_cubics.clear();
		}

		if(_forceBuff != null)
		{
			abortCast();
		}

		for(L2Character character : getKnownList().getKnownCharacters())
		{
			if(character.getTarget() == this)
			{
				if(character.isCastingNow())
				{
					character.abortCast();
				}
			}
		}

		if(isInParty() && getParty().isInDimensionalRift())
		{
			getParty().getDimensionalRift().getDeadMemberList().add(this);
		}

		calculateDeathPenaltyBuffLevel(killer);
		
		// Icons update in order to get retained buffs list
		updateEffectIcons();
		
		stopRentPet();
		stopWaterTask();
		return true;
	}

	private void onDieDropItem(L2Character killer)
	{
		if(atEvent || killer == null)
		{
			return;
		}

		if(getKarma() <= 0 && killer instanceof L2PcInstance && ((L2PcInstance) killer).getClan() != null && getClan() != null && ((L2PcInstance) killer).getClan().isAtWarWith(getClanId()))
		{
			return;
		}

		if(!isInsideZone(ZONE_PVP) && (!isGM() || Config.KARMA_DROP_GM))
		{
			boolean isKarmaDrop = false;
			boolean isKillerNpc = killer instanceof L2Npc;
			int pkLimit = Config.KARMA_PK_LIMIT;;

			int dropEquip = 0;
			int dropEquipWeapon = 0;
			int dropItem = 0;
			int dropLimit = 0;
			int dropPercent = 0;

			if(getKarma() > 0 && getPkKills() >= pkLimit)
			{
				isKarmaDrop = true;
				dropPercent = Config.KARMA_RATE_DROP;
				dropEquip = Config.KARMA_RATE_DROP_EQUIP;
				dropEquipWeapon = Config.KARMA_RATE_DROP_EQUIP_WEAPON;
				dropItem = Config.KARMA_RATE_DROP_ITEM;
				dropLimit = Config.KARMA_DROP_LIMIT;
			}
			else if(isKillerNpc && getLevel() > 4 && !isFestivalParticipant())
			{
				dropPercent = Config.PLAYER_RATE_DROP;
				dropEquip = Config.PLAYER_RATE_DROP_EQUIP;
				dropEquipWeapon = Config.PLAYER_RATE_DROP_EQUIP_WEAPON;
				dropItem = Config.PLAYER_RATE_DROP_ITEM;
				dropLimit = Config.PLAYER_DROP_LIMIT;
			}

			int dropCount = 0;
			while(dropPercent > 0 && Rnd.get(100) < dropPercent && dropCount < dropLimit)
			{
				int itemDropPercent = 0;
				List<Integer> nonDroppableList = new FastList<Integer>();
				List<Integer> nonDroppableListPet = new FastList<Integer>();

				nonDroppableList = Config.KARMA_LIST_NONDROPPABLE_ITEMS;
				nonDroppableListPet = Config.KARMA_LIST_NONDROPPABLE_PET_ITEMS;

				for(L2ItemInstance itemDrop : getInventory().getItems())
				{
					if(itemDrop.isAugmented() || itemDrop.isShadowItem() || itemDrop.getItemId() == 57 || itemDrop.getItem().getType2() == L2Item.TYPE2_QUEST || nonDroppableList.contains(itemDrop.getItemId()) || nonDroppableListPet.contains(itemDrop.getItemId()) || getPet() != null && getPet().getControlItemId() == itemDrop.getItemId())
					{
						continue;
					}

					if(itemDrop.isEquipped())
					{
						itemDropPercent = itemDrop.getItem().getType2() == L2Item.TYPE2_WEAPON ? dropEquipWeapon : dropEquip;
						getInventory().unEquipItemInSlotAndRecord(itemDrop.getEquipSlot());
					}
					else
					{
						itemDropPercent = dropItem;
					}

					if(Rnd.get(100) < itemDropPercent)
					{
						dropItem("DieDrop", itemDrop, killer, true);

						if(isKarmaDrop)
						{
							_log.warn(getName() + " has karma and dropped id = " + itemDrop.getItemId() + ", count = " + itemDrop.getCount());
						}
						else
						{
							_log.warn(getName() + " dropped id = " + itemDrop.getItemId() + ", count = " + itemDrop.getCount());
						}

						dropCount++;
						break;
					}
				}
			}
		}
	}

	private void onDieUpdateKarma()
	{
		if(getKarma() > 0)
		{
			double karmaLost = Config.KARMA_LOST_BASE;
			karmaLost *= getLevel();
			karmaLost *= getLevel() / 100.0;
			karmaLost = Math.round(karmaLost);
			if ( karmaLost < 0 ) karmaLost = 1;

			setKarma(getKarma() - (int) karmaLost);
		}
	}

	public void onKillUpdatePvPKarma(L2Character target)
	{
		if(target == null)
		{
			return;
		}

		if(!(target instanceof L2Playable))
		{
			return;
		}

		if(isCursedWeaponEquipped())
		{
			CursedWeaponsManager.getInstance().increaseKills(_cursedWeaponEquipedId);
			return;
		}

		L2PcInstance targetPlayer = null;

		if(target instanceof L2PcInstance)
		{
			targetPlayer = (L2PcInstance) target;
		}
		else if(target instanceof L2Summon)
		{
			targetPlayer = ((L2Summon) target).getOwner();
		}
		
		if(targetPlayer == null)
		{
			return;
		}

		if(targetPlayer == this)
		{
			targetPlayer = null;
			return;
		}

		if(isCursedWeaponEquiped())
		{
			CursedWeaponsManager.getInstance().increaseKills(_cursedWeaponEquipedId);
			return;
		}
		
		if(isInDuel() && targetPlayer.isInDuel())
		{
			return;
		}

		if(isInsideZone(ZONE_PVP) || targetPlayer.isInsideZone(ZONE_PVP))
		{
			return;
		}

		if(checkIfPvP(target) && targetPlayer.getPvpFlag() != 0 || isInsideZone(ZONE_PVP) && targetPlayer.isInsideZone(ZONE_PVP))
		{
			if (target instanceof L2PcInstance)
			increasePvpKills();
			if ( target instanceof L2PcInstance && Config.ANNOUNCE_PVP_KILL ) // Announces a PvP kill
				Announcements.getInstance().announceToPlayers("Player "+this.getName()+" hunted Player "+target.getName());
			return;
		}
		else
		{
			if(targetPlayer.getClan() != null && getClan() != null)
			{
				if(getClan().isAtWarWith(targetPlayer.getClanId()) && targetPlayer.getPledgeType() != L2Clan.SUBUNIT_ACADEMY)
				{
					if(targetPlayer.getClan().isAtWarWith(getClanId()) && targetPlayer.getPledgeType() != L2Clan.SUBUNIT_ACADEMY)
					{
						if (target instanceof L2PcInstance)
						increasePvpKills();
						if(target instanceof L2PcInstance && Config.ANNOUNCE_PVP_KILL)
						{
							Announcements.getInstance().announceToAll("Player " + getName() + " hunted Player " + target.getName());
						}
						else if(target instanceof L2PcInstance && Config.ANNOUNCE_ALL_KILL)
						{
							Announcements.getInstance().announceToAll("Player " + getName() + " killed Player " + target.getName());
						}
						return;
					}
				}
			}

			if(targetPlayer.getKarma() > 0)
			{
				if(Config.KARMA_AWARD_PK_KILL)
				{
					if (target instanceof L2PcInstance)
					increasePvpKills();
				}
				
				if(target instanceof L2PcInstance && Config.ANNOUNCE_PVP_KILL)
				{
					Announcements.getInstance().announceToAll("Player " + getName() + " hunted Player " + target.getName());
				}
			}
			else if(targetPlayer.getPvpFlag() == 0)
			{
				increasePkKillsAndKarma(targetPlayer.getLevel(), target instanceof L2PcInstance);
				if ( target instanceof L2PcInstance && Config.ANNOUNCE_PK_KILL ) // Announces a Pk kill
					Announcements.getInstance().announceToPlayers("Player "+this.getName()+" has assassinated Player "+target.getName());
			}
		}
		if ( target instanceof L2PcInstance && Config.ANNOUNCE_ALL_KILL ) // Announces all kill
			Announcements.getInstance().announceToPlayers("Player "+this.getName()+" killed Player "+target.getName());

		targetPlayer = null;
	}
	
	public void increasePvpKills()
	{

		setPvpKills(getPvpKills() + 1);
		
		broadcastUserInfo();
		
		// Update the character's name color if they reached any of the PvP levels.
		if(Config.PVP_COLOR_SYSTEM_ENABLED)
		{
			updatePvPColor();
		}

		sendPacket(new UserInfo(this));
	}

	public void increasePkKillsAndKarma(int targLVL, boolean increasePk)
	{
		int baseKarma = Config.KARMA_MIN_KARMA;
		int newKarma = baseKarma;
		int karmaLimit = Config.KARMA_MAX_KARMA;

		int pkLVL = getLevel();
		int pkPKCount = getPkKills();

		int lvlDiffMulti = 0;
		int pkCountMulti = 0;

		if(pkPKCount > 0)
		{
			pkCountMulti = pkPKCount / 2;
		}
		else
		{
			pkCountMulti = 1;
		}

		if(pkCountMulti < 1)
		{
			pkCountMulti = 1;
		}

		if(pkLVL > targLVL)
		{
			lvlDiffMulti = pkLVL / targLVL;
		}
		else
		{
			lvlDiffMulti = 1;
		}

		if(lvlDiffMulti < 1)
		{
			lvlDiffMulti = 1;
		}

		newKarma *= pkCountMulti;
		newKarma *= lvlDiffMulti;

		if(newKarma < baseKarma)
		{
			newKarma = baseKarma;
		}

		if(newKarma > karmaLimit)
		{
			newKarma = karmaLimit;
		}

		if(getKarma() > Integer.MAX_VALUE - newKarma)
		{
			newKarma = Integer.MAX_VALUE - getKarma();
		}

		setKarma(getKarma() + newKarma);
		if (increasePk) 
			setPkKills(getPkKills() + 1);
			stopPvPFlag();
			
		// Update the character's title color if they reached any of the PK levels.
		if(Config.PK_COLOR_SYSTEM_ENABLED)
		{
			updatePkColor();
		}
		
		broadcastUserInfo();
		sendPacket(new UserInfo(this));
	}

	public int calculateKarmaLost(long exp)
	{
		long expGained = Math.abs(exp);
		expGained /= Config.KARMA_XP_DIVIDER;

		int karmaLost = 0;
		if(expGained > Integer.MAX_VALUE)
		{
			karmaLost = Integer.MAX_VALUE;
		}
		else
		{
			karmaLost = (int) expGained;
		}

		if(karmaLost < Config.KARMA_LOST_BASE)
		{
			karmaLost = Config.KARMA_LOST_BASE;
		}

		if(karmaLost > getKarma())
		{
			karmaLost = getKarma();
		}

		return karmaLost;
	}

	public void updatePvPStatus()
	{
		if (isInsideZone(ZONE_PVP))
		{
			return;
		}
		setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);
		
		if(getPvpFlag() == 0)
			startPvPFlag();
	}

	public void updatePvPStatus(L2Character target)
	{
		L2PcInstance player_target = null;
		if(target instanceof L2PcInstance)
		{
			player_target = (L2PcInstance) target;
		}
		else if(target instanceof L2Summon)
		{
			player_target = ((L2Summon) target).getOwner();
		}

		if(player_target == null)
			return;

		if(isInDuel() && player_target.getDuelId() == getDuelId())
			return;

		if((!isInsideZone(ZONE_PVP) || !player_target.isInsideZone(ZONE_PVP)) && player_target.getKarma() == 0)
		{
			if(checkIfPvP(player_target))
			{
				setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_PVP_TIME);
			}
			else
			{
				setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);
			}
			if(getPvpFlag() == 0)
			{
				startPvPFlag();
			}
		}

		player_target = null;
	}

	public void restoreExp(double restorePercent)
	{
		if(getExpBeforeDeath() > 0)
		{
			getStat().addExp((int) Math.round((getExpBeforeDeath() - getExp()) * restorePercent / 100));
			setExpBeforeDeath(0);
		}
	}

	public void deathPenalty(boolean atwar, boolean charmOfCourage)
	{
		final int lvl = getLevel();

		double percentLost = 4.0;

		if(getLevel() < 20)
		{
			percentLost = 10.0;
		}
		else if(getLevel() >= 20 && getLevel() < 40)
		{
			percentLost = 7.0;
		}
		else if(getLevel() >= 40 && getLevel() < 75)
		{
			percentLost = 4.0;
		}
		else if(getLevel() >= 75 && getLevel() < 81)
		{
			percentLost = 2.0;
		}

		if(getKarma() > 0)
		{
			percentLost *= Config.RATE_KARMA_EXP_LOST;
		}

		if(isFestivalParticipant() || atwar || isInsideZone(ZONE_SIEGE))
		{
			percentLost /= 3.0;
		}

		long lostExp = 0;
		if(!atEvent)
		{
			if(lvl < Experience.MAX_LEVEL)
			{
				lostExp = Math.round((getStat().getExpForLevel(lvl + 1) - getStat().getExpForLevel(lvl)) * percentLost / 100);
			}
			else
			{
				lostExp = Math.round((getStat().getExpForLevel(Experience.MAX_LEVEL) - getStat().getExpForLevel(Experience.MAX_LEVEL - 1)) * percentLost / 100);
			}
		}

		setExpBeforeDeath(getExp());

		if(charmOfCourage)
		{
			if(getSiegeState() > 0 && isInsideZone(ZONE_SIEGE))
			{
				lostExp = 0;
			}

			setCharmOfCourage(false);
		}

		getStat().addExp(-lostExp);
	}

	public void deathPenalty(boolean atwar) 
	{ 
		deathPenalty(atwar, getCharmOfCourage()); 
	}
	
	public boolean isPartyWaiting()
	{
		return PartyMatchWaitingList.getInstance().getPlayers().contains(this);
	}

	public void setPartyRoom(int id)
	{
		_partyroom = id;
	}

	public int getPartyRoom()
	{
		return _partyroom;
	}

	public boolean isInPartyMatchRoom()
	{
		return _partyroom > 0;
	}

	public void increaseLevel()
	{
		setCurrentHpMp(getMaxHp(), getMaxMp());
		setCurrentCp(getMaxCp());
	}

	public void stopAllTimers()
	{
		stopHpMpRegeneration();
		stopWarnUserTakeBreak();
		stopWaterTask();
		stopRentPet();
		stopPvpRegTask();
		stopPunishTask(true);
	}

	@Override
	public L2Summon getPet()
	{
		return _summon;
	}

	public void setPet(L2Summon summon)
	{
		_summon = summon;
	}

	public L2TamedBeastInstance getTrainedBeast()
	{
		return _tamedBeast;
	}

	public void setTrainedBeast(L2TamedBeastInstance tamedBeast)
	{
		_tamedBeast = tamedBeast;
	}

	public L2Request getRequest()
	{
		return _request;
	}

	public synchronized void setActiveRequester(L2PcInstance requester)
	{
		_activeRequester = requester;
	}

	public L2PcInstance getActiveRequester()
	{
		return _activeRequester;
	}

	public boolean isProcessingRequest()
	{
		return _activeRequester != null || _requestExpireTime > GameTimeController.getGameTicks();
	}

	public boolean isProcessingTransaction()
	{
		return _activeRequester != null || _activeTradeList != null || _requestExpireTime > GameTimeController.getGameTicks();
	}

	public void onTransactionRequest(L2PcInstance partner)
	{
		_requestExpireTime = GameTimeController.getGameTicks() + REQUEST_TIMEOUT * GameTimeController.TICKS_PER_SECOND;
		partner.setActiveRequester(this);
	}

	public void onTransactionResponse()
	{
		_requestExpireTime = 0;
	}

	public void setActiveWarehouse(ItemContainer warehouse)
	{
		_activeWarehouse = warehouse;
	}

	public ItemContainer getActiveWarehouse()
	{
		return _activeWarehouse;
	}

	public void setActiveTradeList(TradeList tradeList)
	{
		_activeTradeList = tradeList;
	}

	public TradeList getActiveTradeList()
	{
		return _activeTradeList;
	}

	public void onTradeStart(L2PcInstance partner)
	{
		_activeTradeList = new TradeList(this);
		_activeTradeList.setPartner(partner);

		sendPacket(new SystemMessage(SystemMessageId.BEGIN_TRADE_WITH_S1).addString(partner.getName()));
		sendPacket(new TradeStart(this));
	}

	public void onTradeConfirm(L2PcInstance partner)
	{
		sendPacket(new SystemMessage(SystemMessageId.S1_CONFIRMED_TRADE).addString(partner.getName()));
		partner.sendPacket(TradePressOwnOk.STATIC_PACKET);
		sendPacket(TradePressOtherOk.STATIC_PACKET);
	}

	public void onTradeCancel(L2PcInstance partner)
	{
		if(_activeTradeList == null)
		{
			return;
		}

		_activeTradeList.lock();
		_activeTradeList = null;

		sendPacket(new SendTradeDone(0));
		sendPacket(new SystemMessage(SystemMessageId.S1_CANCELED_TRADE).addString(partner.getName()));
	}

	public void onTradeFinish(boolean successfull)
	{
		_activeTradeList = null;
		sendPacket(new SendTradeDone(1));
		if(successfull)
		{
			sendPacket(new SystemMessage(SystemMessageId.TRADE_SUCCESSFUL));
		}
	}

	public void startTrade(L2PcInstance partner)
	{
		onTradeStart(partner);
		partner.onTradeStart(this);
	}

	public void cancelActiveTrade()
	{
		if(_activeTradeList == null)
		{
			return;
		}

		L2PcInstance partner = _activeTradeList.getPartner();
		if(partner != null)
		{
			partner.onTradeCancel(this);
			partner = null;
		}

		onTradeCancel(this);
	}

	public L2ManufactureList getCreateList()
	{
		return _createList;
	}

	public void setCreateList(L2ManufactureList x)
	{
		_createList = x;
	}

	public TradeList getSellList()
	{
		if(_sellList == null)
		{
			_sellList = new TradeList(this);
		}

		return _sellList;
	}

	public TradeList getBuyList()
	{
		if(_buyList == null)
		{
			_buyList = new TradeList(this);
		}

		return _buyList;
	}

	public void setPrivateStoreType(int type)
	{
		_privatestore = type;

		if(_privatestore == STORE_PRIVATE_NONE && (getClient() == null || isOffline()))
		{
			//getAppearance().setNameColor(this._originalNameColorOffline);
			//this.store();
			if(Config.OFFLINE_DISCONNECT_FINISHED)
			{
				this.store();
				this.deleteMe();

				if(this.getClient() != null)
				{
					this.getClient().setActiveChar(null);
				}
			}
		}
	}

	public int getPrivateStoreType()
	{
		return _privatestore;
	}

	public void setSkillLearningClassId(ClassId classId)
	{
		_skillLearningClassId = classId;
	}

	public ClassId getSkillLearningClassId()
	{
		return _skillLearningClassId;
	}

	public void setClan(L2Clan clan)
	{
		_clan = clan;
		setTitle("");

		if(clan == null)
		{
			_clanId = 0;
			_clanPrivileges = 0;
			_pledgeType = 0;
			_powerGrade = 0;
			_lvlJoinedAcademy = 0;
			_apprentice = 0;
			_sponsor = 0;
			_activeWarehouse = null;
			return;
		}

		if(!clan.isMember(getName()))
		{
			setClan(null);
			return;
		}

		_clanId = clan.getClanId();

		if(isClanLeader())
		{
			setClanLeader(true);
		}

	}

	public L2Clan getClan()
	{
		return _clan;
	}

	public boolean isClanLeader()
	{
		if(getClan() == null)
		{
			return false;
		}
		else
		{
			return getObjectId() == getClan().getLeaderId();
		}
	}

	@Override
	protected void reduceArrowCount()
	{
		L2ItemInstance arrows = getInventory().destroyItem("Consume", getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND), 1, this, null);

		if(arrows == null || arrows.getCount() == 0)
		{
			getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_LHAND);
			_arrowItem = null;

			sendPacket(new ItemList(this, false));
		}
		else
		{
			if(!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(arrows);
				sendPacket(iu);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}

			arrows = null;
		}
	}

	@Override
	protected boolean checkAndEquipArrows()
	{
		if(getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND) == null && getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND).getItemType() == L2WeaponType.BOW )
		{
			_arrowItem = getInventory().findArrowForBow(getActiveWeaponItem());

			if(_arrowItem != null)
			{
				getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, _arrowItem);

				ItemList il = new ItemList(this, false);
				sendPacket(il);
			}
		}
		else
		{
			_arrowItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		}

		return _arrowItem != null;
	}

	public boolean disarmWeapons()
	{
		if(isCursedWeaponEquiped() && !getAccessLevel().isGm())
		{
			return false;
		}

		L2ItemInstance wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if(wpn == null)
		{
			wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
		}

		if(wpn != null)
		{
			if(wpn.isWear())
			{
				return false;
			}

			if(wpn.isAugmented())
			{
				wpn.getAugmentation().removeBoni(this);
			}

			L2ItemInstance[] unequiped = getInventory().unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
			InventoryUpdate iu = new InventoryUpdate();
			for(L2ItemInstance element : unequiped)
			{
				iu.addModifiedItem(element);
			}
			sendPacket(iu);

			abortAttack();
			broadcastUserInfo();

			if(unequiped.length > 0)
			{
				if(unequiped[0].getEnchantLevel() > 0)
				{
					sendPacket(new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED).addNumber(unequiped[0].getEnchantLevel()).addItemName(unequiped[0].getItemId()));
				}
				else
				{
					sendPacket(new SystemMessage(SystemMessageId.S1_DISARMED).addItemName(unequiped[0].getItemId()));
				}
			}

			wpn = null;
			unequiped = null;
		}

		L2ItemInstance sld = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if(sld != null)
		{
			if(sld.isWear())
			{
				return false;
			}

			L2ItemInstance[] unequiped = getInventory().unEquipItemInBodySlotAndRecord(sld.getItem().getBodyPart());
			InventoryUpdate iu = new InventoryUpdate();
			for(L2ItemInstance element : unequiped)
			{
				iu.addModifiedItem(element);
			}
			sendPacket(iu);

			abortAttack();
			broadcastUserInfo();

			if(unequiped.length > 0)
			{
				if(unequiped[0].getEnchantLevel() > 0)
				{
					sendPacket(new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED).addNumber(unequiped[0].getEnchantLevel()).addItemName(unequiped[0].getItemId()));
				}
				else
				{
					sendPacket(new SystemMessage(SystemMessageId.S1_DISARMED).addItemName(unequiped[0].getItemId()));
				}
			}

			sld = null;
			unequiped = null;
		}

		return true;
	}

	@Override
	public boolean isUsingDualWeapon()
	{
		L2Weapon weaponItem = getActiveWeaponItem();
		if(weaponItem == null)
		{
			return false;
		}

		if(weaponItem.getItemType() == L2WeaponType.DUAL)
		{
			return true;
		}
		else if(weaponItem.getItemType() == L2WeaponType.DUALFIST)
		{
			return true;
		}
		else if(weaponItem.getItemId() == 248)
		{
			return true;
		}
		else if(weaponItem.getItemId() == 252)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public void setUptime(long time)
	{
		_uptime = time;
	}

	public long getUptime()
	{
		return System.currentTimeMillis() - _uptime;
	}

	@Override
	public boolean isInvul()
	{
		return _isInvul || _isTeleporting || _protectEndTime > GameTimeController.getGameTicks();
	}

	@Override
	public boolean isInParty()
	{
		return _party != null;
	}

	public void setParty(L2Party party)
	{
		_party = party;
	}

	public void joinParty(L2Party party)
	{
		if(party.getMemberCount()==9){
			sendPacket(new SystemMessage(SystemMessageId.PARTY_FULL));
			return;
		}
		
		if(party != null && party.getMemberCount()<9)
		{
			_party = party;
			party.addPartyMember(this);
		}
	}

	public boolean isGM()
	{
		return getAccessLevel().isGm();
	}

	public boolean isAdministrator()
	{
		return getAccessLevel().getLevel() == AccessLevels._masterAccessLevelNum;
	}

	public boolean isUser()
	{
		return getAccessLevel().getLevel() == AccessLevels._userAccessLevelNum;
	}

	public boolean isNormalGm()
	{
		return !isAdministrator() && !isUser();
	}

	public void leaveParty()
	{
		if(isInParty())
		{
			_party.removePartyMember(this);
			_party = null;
		}
	}

	@Override
	public L2Party getParty()
	{
		return _party;
	}

	public void setFirstLog(int firstlog)
	{
		_first_log = false;
		if(firstlog == 1)
		{
			_first_log = true;
		}
	}

	public void setFirstLog(boolean firstlog)
	{
		_first_log = firstlog;
	}

	public boolean getFirstLog()
	{
		return _first_log;
	}

	/*public void cancelCastMagic()
	{
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		enableAllSkills();
		MagicSkillCanceld msc = new MagicSkillCanceld(getObjectId());
		Broadcast.toSelfAndKnownPlayersInRadius(this, msc, 810000);
		msc = null;
	}*/

	public void setAccessLevel(int level)
	{
		if(level == AccessLevels._masterAccessLevelNum)
		{
			_log.warn("Admin/GM " + getName() + " with access level " + level + " has logged in!");
			_accessLevel = AccessLevels._masterAccessLevel;
		}
		else if(level == AccessLevels._userAccessLevelNum)
		{
			_accessLevel = AccessLevels._userAccessLevel;
		}
		else
		{
			AccessLevel accessLevel = AccessLevels.getInstance().getAccessLevel(level);

			if(accessLevel == null)
			{
				if(level < 0)
				{
					AccessLevels.getInstance().addBanAccessLevel(level);
					_accessLevel = AccessLevels.getInstance().getAccessLevel(level);
				}
				else
				{
					_log.warn("Tried to set unregistered access level " + level + " to character " + getName() + ". Setting access level without privileges!");
					_accessLevel = AccessLevels._userAccessLevel;
				}
			}
			else
			{
				_accessLevel = accessLevel;
			}

			accessLevel = null;
		}

		if(_accessLevel != AccessLevels._userAccessLevel)
		{
			if(getAccessLevel().useNameColor())
			{
				getAppearance().setNameColor(_accessLevel.getNameColor());
			}

			if(getAccessLevel().useTitleColor())
			{
				getAppearance().setTitleColor(_accessLevel.getTitleColor());
			}
			broadcastUserInfo();
		}
	}

	public void setAccountAccesslevel(int level)
	{
		LoginServerThread.getInstance().sendAccessLevel(getAccountName(), level);
	}

	public AccessLevel getAccessLevel()
	{
		if(Config.GM_EVERYBODY)
		{
			return AccessLevels._masterAccessLevel;
		}
		else if(_accessLevel == null)
		{
			setAccessLevel(AccessLevels._userAccessLevelNum);
		}

		return _accessLevel;
	}

	@Override
	public double getLevelMod()
	{
		return (100.0 - 11 + getLevel()) / 100.0;
	}

	public void updateAndBroadcastStatus(int broadcastType)
	{
		refreshOverloaded();
		refreshExpertisePenalty();
		if(broadcastType == 1)
		{
			this.sendPacket(new UserInfo(this));
		}

		if(broadcastType == 2)
		{
			broadcastUserInfo();
		}
	}

	public void setKarmaFlag(int flag)
	{
		sendPacket(new UserInfo(this));
		for(L2PcInstance player : getKnownList().getKnownPlayers().values())
		{
			player.sendPacket(new RelationChanged(this, getRelation(player), isAutoAttackable(player)));
			if (getPet() != null) 
				player.sendPacket(new RelationChanged(getPet(), getRelation(player), isAutoAttackable(player)));
		}
	}

	public void broadcastKarma()
	{
		StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.KARMA, getKarma());
		sendPacket(su);
		
		if (getPet() != null)
			sendPacket(new RelationChanged(getPet(), getRelation(this), false));
		
		broadcastRelationsChanges();
	}

	public void setOnlineStatus(boolean isOnline)
	{
		if(_isOnline != isOnline)
		{
			_isOnline = isOnline;
		}

		updateOnlineStatus();
	}

	public void setIsIn7sDungeon(boolean isIn7sDungeon)
	{
		if(_isIn7sDungeon != isIn7sDungeon)
		{
			_isIn7sDungeon = isIn7sDungeon;
		}

		updateIsIn7sDungeonStatus();
	}

	public void updateOnlineStatus()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE characters SET online = ?, lastAccess = ? WHERE obj_id = ?");
			statement.setInt(1, isOnline());
			statement.setLong(2, System.currentTimeMillis());
			statement.setInt(3, getObjectId());
			statement.execute();
		}
		catch(Exception e)
		{
			_log.error("could not set char online status", e);
		}
		finally
		{
			ResourceUtil.closeStatement(statement);
			ResourceUtil.closeConnection(con);
		}
	}

	public void updateIsIn7sDungeonStatus()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE characters SET isIn7sDungeon = ?, lastAccess = ? WHERE obj_id = ?");
			statement.setInt(1, isIn7sDungeon() ? 1 : 0);
			statement.setLong(2, System.currentTimeMillis());
			statement.setInt(3, getObjectId());
			statement.execute();
		}
		catch(Exception e)
		{
			_log.error("could not set char isIn7sDungeon status", e);
		}
		finally
		{
			ResourceUtil.closeStatement(statement);
			ResourceUtil.closeConnection(con);
		}
	}

	public void updateFirstLog()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE characters SET first_log = ? WHERE obj_id = ?");

			int _fl;
			if(getFirstLog())
			{
				_fl = 1;
			}
			else
			{
				_fl = 0;
			}
			statement.setInt(1, _fl);
			statement.setInt(2, getObjectId());
			statement.execute();
		}
		catch(Exception e)
		{
			_log.warn("Could not set char first login:" + e);
		}
		finally
		{
			ResourceUtil.closeStatement(statement);
			ResourceUtil.closeConnection(con);
		}
	}

	private boolean createDb()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO characters " + "(account_name, obj_Id, char_name, level, maxHp, curHp, maxCp, curCp, maxMp, curMp, " + "acc, crit, evasion, mAtk, mDef, mSpd, pAtk, pDef, pSpd, runSpd, walkSpd, " + "str, con, dex, _int, men, wit, face, hairStyle, hairColor, sex, " + "movement_multiplier, attack_speed_multiplier, colRad, colHeight, " + "exp, sp, karma, pvpkills, pkkills, clanid, maxload, race, classid, deletetime, " + "cancraft, title, accesslevel, online, isin7sdungeon, clan_privs, wantspeace, " + "base_class, newbie, nobless, power_grade, last_recom_date, name_color, title_color, haswhacc, whaccid, whaccpwd, aio, aio_end, vip, vip_end) " + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			statement.setString(1, _accountName);
			statement.setInt(2, getObjectId());
			statement.setString(3, getName());
			statement.setInt(4, getLevel());
			statement.setInt(5, getMaxHp());
			statement.setDouble(6, getCurrentHp());
			statement.setInt(7, getMaxCp());
			statement.setDouble(8, getCurrentCp());
			statement.setInt(9, getMaxMp());
			statement.setDouble(10, getCurrentMp());
			statement.setInt(11, getAccuracy());
			statement.setInt(12, getCriticalHit(null, null));
			statement.setInt(13, getEvasionRate(null));
			statement.setInt(14, getMAtk(null, null));
			statement.setInt(15, getMDef(null, null));
			statement.setInt(16, getMAtkSpd());
			statement.setInt(17, getPAtk(null));
			statement.setInt(18, getPDef(null));
			statement.setInt(19, getPAtkSpd());
			statement.setInt(20, getRunSpeed());
			statement.setInt(21, getWalkSpeed());
			statement.setInt(22, getSTR());
			statement.setInt(23, getCON());
			statement.setInt(24, getDEX());
			statement.setInt(25, getINT());
			statement.setInt(26, getMEN());
			statement.setInt(27, getWIT());
			statement.setInt(28, getAppearance().getFace());
			statement.setInt(29, getAppearance().getHairStyle());
			statement.setInt(30, getAppearance().getHairColor());
			statement.setInt(31, getAppearance().getSex() ? 1 : 0);
			statement.setDouble(32, 1);
			statement.setDouble(33, 1);
			statement.setDouble(34, getTemplate().collisionRadius);
			statement.setDouble(35, getTemplate().collisionHeight);
			statement.setLong(36, getExp());
			statement.setInt(37, getSp());
			statement.setInt(38, getKarma());
			statement.setInt(39, getPvpKills());
			statement.setInt(40, getPkKills());
			statement.setInt(41, getClanId());
			statement.setInt(42, getMaxLoad());
			statement.setInt(43, getRace().ordinal());
			statement.setInt(44, getClassId().getId());
			statement.setLong(45, getDeleteTimer());
			statement.setInt(46, hasDwarvenCraft() ? 1 : 0);
			statement.setString(47, getTitle());
			statement.setInt(48, getAccessLevel().getLevel());
			statement.setInt(49, isOnline());
			statement.setInt(50, isIn7sDungeon() ? 1 : 0);
			statement.setInt(51, getClanPrivileges());
			statement.setInt(52, getWantsPeace());
			statement.setInt(53, getBaseClass());
			statement.setInt(54, isNewbie() ? 1 : 0);
			statement.setInt(55, isNoble() ? 1 : 0);

			statement.setLong(56, 0);
			statement.setLong(57, System.currentTimeMillis());
			statement.setString(58, StringToHex(Integer.toHexString(_originalNameColorOffline).toUpperCase()));
			statement.setString(59, StringToHex(Integer.toHexString(getAppearance().getTitleColor()).toUpperCase()));
			statement.setInt(60, hasWarehouseAccount() ? 1 : 0);
			statement.setString(61, getWarehouseAccountId());
			statement.setString(62, getWarehouseAccountPwd());
			statement.setInt(63, isAio() ? 1 :0);
			statement.setLong(64, 0);
            statement.setInt(65, isVip() ? 1 :0);
            statement.setLong(66, 0);

			statement.executeUpdate();
		}
		catch(Exception e)
		{
			_log.error("Could not insert char data: " + e);
			return false;
		}
		finally
		{
			ResourceUtil.closeStatement(statement);
			ResourceUtil.closeConnection(con);
		}
		_log.info("Created new character : " + getName() + " for account: " + _accountName);
		return true;
	}

	private static L2PcInstance restore(int objectId)
	{
		L2PcInstance player = null;
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			PreparedStatement statement = con.prepareStatement(RESTORE_CHARACTER);
			statement.setInt(1, objectId);
			ResultSet rset = statement.executeQuery();

			while(rset.next())
			{
				final int activeClassId = rset.getInt("classid");
				final boolean female = rset.getInt("sex") != 0;
				final L2PcTemplate template = CharTemplateTable.getInstance().getTemplate(activeClassId);
				PcAppearance app = new PcAppearance(rset.getByte("face"), rset.getByte("hairColor"), rset.getByte("hairStyle"), female);

				player = new L2PcInstance(objectId, template, rset.getString("account_name"), app);
				player.setName(rset.getString("char_name"));
				player._lastAccess = rset.getLong("lastAccess");

				player.getStat().setExp(rset.getLong("exp"));
				player.setExpBeforeDeath(rset.getLong("expBeforeDeath"));
				player.getStat().setLevel(rset.getByte("level"));
				player.getStat().setSp(rset.getInt("sp"));

				player.setWantsPeace(rset.getInt("wantspeace"));

				player.setHeading(rset.getInt("heading"));

				player.setKarma(rset.getInt("karma"));
				player.setPvpKills(rset.getInt("pvpkills"));
				player.setPkKills(rset.getInt("pkkills"));
				player.setOnlineTime(rset.getLong("onlinetime"));
				player.setNewbie(rset.getInt("newbie") == 1);
				player.setNoble(rset.getInt("nobless") == 1);
				player.setClanJoinExpiryTime(rset.getLong("clan_join_expiry_time"));
				player.setFirstLog(rset.getInt("first_log"));
				player.setHasWarehouseAccount(rset.getInt("haswhacc") == 1);
				player.setWarehouseAccountId(rset.getString("whaccid"));
				player.setWarehouseAccountPwd(rset.getString("whaccpwd"));
				app = null;

				if(player.getClanJoinExpiryTime() < System.currentTimeMillis())
				{
					player.setClanJoinExpiryTime(0);
				}
				player.setClanCreateExpiryTime(rset.getLong("clan_create_expiry_time"));
				if(player.getClanCreateExpiryTime() < System.currentTimeMillis())
				{
					player.setClanCreateExpiryTime(0);
				}

				int clanId = rset.getInt("clanid");
				player.setPowerGrade((int) rset.getLong("power_grade"));
				player.setPledgeType(rset.getInt("subpledge"));
				player.setLastRecomUpdate(rset.getLong("last_recom_date"));

				if(clanId > 0)
				{
					player.setClan(ClanTable.getInstance().getClan(clanId));
				}

				if(player.getClan() != null)
				{
					if(player.getClan().getLeaderId() != player.getObjectId())
					{
						if(player.getPowerGrade() == 0)
						{
							player.setPowerGrade(5);
						}
						player.setClanPrivileges(player.getClan().getRankPrivs(player.getPowerGrade()));
					}
					else
					{
						player.setClanPrivileges(L2Clan.CP_ALL);
						player.setPowerGrade(1);
					}
				}
				else
				{
					player.setClanPrivileges(L2Clan.CP_NOTHING);
				}

				player.setDeleteTimer(rset.getLong("deletetime"));

				player.setTitle(rset.getString("title"));
				player.setAccessLevel(rset.getInt("accesslevel"));
				player.setFistsWeaponItem(player.findFistsWeaponItem(activeClassId));
				player.setUptime(System.currentTimeMillis());

				player.setCurrentHp(rset.getDouble("curHp"));
				player.setCurrentCp(rset.getDouble("curCp"));
				player.setCurrentMp(rset.getDouble("curMp"));

				player.checkRecom(rset.getInt("rec_have"), rset.getInt("rec_left"));

				player._classIndex = 0;
				try
				{
					player.setBaseClass(rset.getInt("base_class"));
				}
				catch(Exception e)
				{
					player.setBaseClass(activeClassId);
				}

				if(restoreSubClassData(player))
				{
					if(activeClassId != player.getBaseClass())
					{
						for(SubClass subClass : player.getSubClasses().values())
						{
							if(subClass.getClassId() == activeClassId)
							{
								player._classIndex = subClass.getClassIndex();
							}
						}
					}
				}

				if(player.getClassIndex() == 0 && activeClassId != player.getBaseClass())
				{
					player.setClassId(player.getBaseClass());
					_log.warn("Player " + player.getName() + " reverted to base class. Possibly has tried a relogin exploit while subclassing.");
				}
				else
				{
					player._activeClass = activeClassId;
				}

				player.setApprentice(rset.getInt("apprentice"));
				player.setSponsor(rset.getInt("sponsor"));
				player.setLvlJoinedAcademy(rset.getInt("lvl_joined_academy"));
				player.setIsIn7sDungeon(rset.getInt("isin7sdungeon") == 1 ? true : false);
				player.setPunishLevel(rset.getInt("punish_level"));
                if (player.getPunishLevel() != PunishLevel.NONE)
                	player.setPunishTimer(rset.getLong("punish_timer"));
                else
                	player.setPunishTimer(0);
				try
				{
					player.getAppearance().setNameColor(Integer.decode(new StringBuilder().append("0x").append(rset.getString("name_color")).toString()).intValue());
					player.getAppearance().setTitleColor(Integer.decode(new StringBuilder().append("0x").append(rset.getString("title_color")).toString()).intValue());
				}
				catch(Exception e)
				{
				}

				CursedWeaponsManager.getInstance().checkPlayer(player);

				player.setAllianceWithVarkaKetra(rset.getInt("varka_ketra_ally"));

				player.setDeathPenaltyBuffLevel(rset.getInt("death_penalty_level"));
                player.setAio(rset.getInt("aio") == 1 ? true : false);
                player.setAioEndTime(rset.getLong("aio_end"));
                player.setVip(rset.getInt("vip") == 1 ? true : false);
                player.setVipEndTime(rset.getLong("vip_end"));

				player.setXYZInvisible(rset.getInt("x"), rset.getInt("y"), rset.getInt("z"));

				player.loadVariables();

				PreparedStatement stmt = con.prepareStatement("SELECT obj_Id, char_name FROM characters WHERE account_name = ? AND obj_Id <> ?");
				stmt.setString(1, player._accountName);
				stmt.setInt(2, objectId);
				ResultSet chars = stmt.executeQuery();

				while(chars.next())
				{
					Integer charId = chars.getInt("obj_Id");
					String charName = chars.getString("char_name");
					player._chars.put(charId, charName);
				}

				chars.close();
				stmt.close();
				chars = null;
				stmt = null;

				break;
			}

			rset.close();
			statement.close();
			statement = null;
			rset = null;

			player.restoreCharData();
			player.rewardSkills();

			player.setPet(L2World.getInstance().getPet(player.getObjectId()));
			if(player.getPet() != null)
			{
				player.getPet().setOwner(player);
			}

			player.refreshOverloaded();
			player.fireEvent(EventType.LOAD.name, (Object[]) null);
		}
		catch(Exception e)
		{
			_log.error("Could not restore char data: ");
			e.printStackTrace();
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}

		return player;
	}

	public Forum getMail()
	{
		if(_forumMail == null)
		{
			setMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(getName()));

			if(_forumMail == null)
			{
				ForumsBBSManager.getInstance().createNewForum(getName(), ForumsBBSManager.getInstance().getForumByName("MailRoot"), Forum.MAIL, Forum.OWNERONLY, getObjectId());
				setMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(getName()));
			}
		}

		return _forumMail;
	}

	public void setMail(Forum forum)
	{
		_forumMail = forum;
	}

	public Forum getMemo()
	{
		if(_forumMemo == null)
		{
			setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(_accountName));

			if(_forumMemo == null)
			{
				ForumsBBSManager.getInstance().createNewForum(_accountName, ForumsBBSManager.getInstance().getForumByName("MemoRoot"), Forum.MEMO, Forum.OWNERONLY, getObjectId());
				setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(_accountName));
			}
		}

		return _forumMemo;
	}

	public void setMemo(Forum forum)
	{
		_forumMemo = forum;
	}

	private static boolean restoreSubClassData(L2PcInstance player)
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_SUBCLASSES);
			statement.setInt(1, player.getObjectId());

			ResultSet rset = statement.executeQuery();

			while(rset.next())
			{
				SubClass subClass = new SubClass();
				subClass.setClassId(rset.getInt("class_id"));
				subClass.setLevel(rset.getByte("level"));
				subClass.setExp(rset.getLong("exp"));
				subClass.setSp(rset.getInt("sp"));
				subClass.setClassIndex(rset.getInt("class_index"));

				player.getSubClasses().put(subClass.getClassIndex(), subClass);
			}

			statement.close();
			rset.close();
			rset = null;
			statement = null;
		}
		catch(Exception e)
		{
			_log.error("Could not restore classes for " + player.getName(), e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}

		return true;
	}

	private void restoreCharData()
	{
		restoreSkills();

		_macroses.restore();

		_shortCuts.restore();

		restoreHenna();

		if(Config.ALT_RECOMMEND)
		{
			restoreRecom();
		}

		if(!isSubClassActive())
		{
			restoreRecipeBook();
		}
	}

	private void storeRecipeBook()
	{
		if(isSubClassActive())
		{
			return;
		}

		if(getCommonRecipeBook().length == 0 && getDwarvenRecipeBook().length == 0)
		{
			return;
		}

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_recipebook WHERE char_id = ?");
			statement.setInt(1, getObjectId());
			statement.execute();
			statement.close();
			statement = null;

			L2RecipeList[] recipes = getCommonRecipeBook();

			for(L2RecipeList recipe : recipes)
			{
				statement = con.prepareStatement("INSERT INTO character_recipebook (char_id, id, type) VALUES (?, ?, 0)");
				statement.setInt(1, getObjectId());
				statement.setInt(2, recipe.getId());
				statement.execute();
				statement.close();
				statement = null;
			}

			recipes = getDwarvenRecipeBook();
			for(L2RecipeList recipe : recipes)
			{
				statement = con.prepareStatement("INSERT INTO character_recipebook (char_id, id, type) VALUES (?, ?, 1)");
				statement.setInt(1, getObjectId());
				statement.setInt(2, recipe.getId());
				statement.execute();
				statement.close();
				statement = null;
			}
			recipes = null;
		}
		catch(Exception e)
		{
			_log.error("Could not store recipe book data", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	private void restoreRecipeBook()
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, type FROM character_recipebook WHERE char_id = ?");
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();

			L2RecipeList recipe;
			while(rset.next())
			{
				recipe = RecipeTable.getInstance().getRecipeList(rset.getInt("id") - 1);

				if(rset.getInt("type") == 1)
				{
					registerDwarvenRecipeList(recipe);
				}
				else
				{
					registerCommonRecipeList(recipe);
				}
			}

			rset.close();
			statement.close();
		}
		catch(Exception e)
		{
			_log.error("Could not restore recipe book data", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}
	
	/**
	 * Update L2PcInstance stats in the characters table of the database.<BR>
	 * <BR>
	 */
	public synchronized void store()
	{
		if(isInsideRadius(getClientX(), getClientY(), 1000, true))
		{
			setXYZ(getClientX(), getClientY(), getClientZ());
		}

		storeCharBase();
		storeCharSub();
		storeEffect();
		storeRecipeBook();
		fireEvent(EventType.STORE.name, (Object[]) null);
	}

	private void storeCharBase()
	{
		Connection con = null;

		try
		{
			int currentClassIndex = getClassIndex();
			_classIndex = 0;
			long exp = getStat().getExp();
			int level = getStat().getLevel();
			int sp = getStat().getSp();
			_classIndex = currentClassIndex;

			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;

			statement = con.prepareStatement(UPDATE_CHARACTER);
			statement.setInt(1, level);
			statement.setInt(2, getMaxHp());
			statement.setDouble(3, getCurrentHp());
			statement.setInt(4, getMaxCp());
			statement.setDouble(5, getCurrentCp());
			statement.setInt(6, getMaxMp());
			statement.setDouble(7, getCurrentMp());
			statement.setInt(8, getSTR());
			statement.setInt(9, getCON());
			statement.setInt(10, getDEX());
			statement.setInt(11, getINT());
			statement.setInt(12, getMEN());
			statement.setInt(13, getWIT());
			statement.setInt(14, getAppearance().getFace());
			statement.setInt(15, getAppearance().getHairStyle());
			statement.setInt(16, getAppearance().getHairColor());
			statement.setInt(17, getAppearance().getSex()? 1 : 0);
			statement.setInt(18, getHeading());
			statement.setInt(19, _observerMode ? _obsX : getX());
			statement.setInt(20, _observerMode ? _obsY : getY());
			statement.setInt(21, _observerMode ? _obsZ : getZ());
			statement.setLong(22, exp);
			statement.setLong(23, getExpBeforeDeath());
			statement.setInt(24, sp);
			statement.setInt(25, getKarma());
			statement.setInt(26, getPvpKills());
			statement.setInt(27, getPkKills());
			statement.setInt(28, getRecomHave());
			statement.setInt(29, getRecomLeft());
			statement.setInt(30, getClanId());
			statement.setInt(31, getMaxLoad());
			statement.setInt(32, getRace().ordinal());
			statement.setInt(33, getClassId().getId());
			statement.setLong(34, getDeleteTimer());
			statement.setString(35, getTitle());
			statement.setInt(36, getAccessLevel().getLevel());
			statement.setInt(37, _isOffline ? 0 : isOnline());
			statement.setInt(38, isIn7sDungeon() ? 1 : 0);
			statement.setInt(39, getClanPrivileges());
			statement.setInt(40, getWantsPeace());
			statement.setInt(41, getBaseClass());

			long totalOnlineTime = _onlineTime;

			if(_onlineBeginTime > 0)
			{
				totalOnlineTime += (System.currentTimeMillis() - _onlineBeginTime) / 1000;
			}

			statement.setLong(42, totalOnlineTime);
			statement.setInt(43, getPunishLevel().value());
            statement.setLong(44, getPunishTimer());
			statement.setInt(45, isNewbie() ? 1 : 0);
			statement.setInt(46, isNoble() ? 1 : 0);
			statement.setLong(47, getPowerGrade());
			statement.setInt(48, getPledgeType());
			statement.setLong(49, getLastRecomUpdate());
			statement.setInt(50, getLvlJoinedAcademy());
			statement.setLong(51, getApprentice());
			statement.setLong(52, getSponsor());
			statement.setInt(53, getAllianceWithVarkaKetra());
			statement.setLong(54, getClanJoinExpiryTime());
			statement.setLong(55, getClanCreateExpiryTime());
			statement.setString(56, getName());
			statement.setLong(57, getDeathPenaltyBuffLevel());
			statement.setString(58, StringToHex(Integer.toHexString(getAppearance().getNameColor()).toUpperCase()));
			statement.setString(59, StringToHex(Integer.toHexString(getAppearance().getTitleColor()).toUpperCase()));
			statement.setInt(60, getFirstLog() ? 1 : 0);
			statement.setInt(61, hasWarehouseAccount() ? 1 : 0);
			statement.setString(62, getWarehouseAccountId());
			statement.setString(63, getWarehouseAccountPwd());
			statement.setInt(64, isAio() ? 1 : 0);
			statement.setLong(65, getAioEndTime());
			statement.setInt(66, isVip() ? 1 : 0);
			statement.setLong(67, getVipEndTime());
			statement.setInt(68, getObjectId());
			statement.execute();
			statement.close();
		}
		catch(Exception e)
		{
			_log.error("Could not store char base data", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	private void storeCharSub()
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;

			if(getTotalSubClasses() > 0)
			{
				for(SubClass subClass : getSubClasses().values())
				{
					statement = con.prepareStatement(UPDATE_CHAR_SUBCLASS);
					statement.setLong(1, subClass.getExp());
					statement.setInt(2, subClass.getSp());
					statement.setInt(3, subClass.getLevel());
					statement.setInt(4, subClass.getClassId());
					statement.setInt(5, getObjectId());
					statement.setInt(6, subClass.getClassIndex());
					statement.execute();
					statement.close();
				}
			}
		}
		catch(Exception e)
		{
			_log.error("Could not store sub class data for " + getName(), e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	private void storeEffect()
	{
		if(!Config.STORE_SKILL_COOLTIME)
		{
			return;
		}

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;

			statement = con.prepareStatement(DELETE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.execute();
			statement.close();

			int buff_index = 0;

			for(L2Effect effect : getAllEffects())
			{
				if(effect != null && !effect.isHerbEffect() && effect.getInUse() && !effect.getSkill().isToggle() && effect.getSkill().getSkillType() != L2SkillType.FORCE_BUFF && effect.getSkill().getId() != 426 && effect.getSkill().getId() != 427)
				{
					int skillId = effect.getSkill().getId();
					buff_index++;

					statement = con.prepareStatement(ADD_SKILL_SAVE);
					statement.setInt(1, getObjectId());
					statement.setInt(2, skillId);
					statement.setInt(3, effect.getSkill().getLevel());
					statement.setInt(4, effect.getCount());
					statement.setInt(5, effect.getTime());

					if(_reuseTimeStamps.containsKey(skillId))
					{
						TimeStamp t = _reuseTimeStamps.remove(skillId);
						statement.setLong(6, t.hasNotPassed() ? t.getReuse() : 0);
						statement.setLong(7, t.hasNotPassed() ? t.getStamp() : 0 );
					}
					else
					{
						statement.setLong(6, 0);
						statement.setLong(7, 0);
					}

					statement.setInt(8, 0);
					statement.setInt(9, getClassIndex());
					statement.setInt(10, buff_index);
					statement.execute();
					statement.close();
				}
			}

			for(TimeStamp t : _reuseTimeStamps.values())
			{
				if(t.hasNotPassed())
				{
					buff_index++;
					statement = con.prepareStatement(ADD_SKILL_SAVE);
					statement.setInt(1, getObjectId());
					statement.setInt(2, t.getSkill());
					statement.setInt(3, -1);
					statement.setInt(4, -1);
					statement.setInt(5, -1);
					statement.setLong(6, t.getReuse());
					statement.setLong(7, t.getStamp());
					statement.setInt(8, 1);
					statement.setInt(9, getClassIndex());
					statement.setInt(10, buff_index);
					statement.execute();
					statement.close();
				}
			}
			_reuseTimeStamps.clear();
		}
		catch(Exception e)
		{
			_log.error("Could not store char effect data", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public int isOnline()
	{
		return _isOnline ? 1 : 0;
	}

	public boolean isIn7sDungeon()
	{
		return _isIn7sDungeon;
	}

	public L2Skill addSkill(L2Skill newSkill, boolean store)
	{
		L2Skill oldSkill = super.addSkill(newSkill);

		if(store)
		{
			storeSkill(newSkill, oldSkill, -1);
		}

		return oldSkill;
	}

	public L2Skill removeSkill(L2Skill skill, boolean store)
	{
		if(store)
		{
			return removeSkill(skill);
		}
		else
		{
			return super.removeSkill(skill);
		}
	}

	@Override
	public L2Skill removeSkill(L2Skill skill)
	{
		L2Skill oldSkill = super.removeSkill(skill);

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;

			if(oldSkill != null)
			{
				statement = con.prepareStatement(DELETE_SKILL_FROM_CHAR);
				statement.setInt(1, oldSkill.getId());
				statement.setInt(2, getObjectId());
				statement.setInt(3, getClassIndex());
				statement.execute();
				statement.close();
			}
		}
		catch(Exception e)
		{
			_log.error("Error could not delete skill", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}

		L2ShortCut[] allShortCuts = getAllShortCuts();

		for(L2ShortCut sc : allShortCuts)
		{
			if(sc != null && skill != null && sc.getId() == skill.getId() && sc.getType() == L2ShortCut.TYPE_SKILL)
			{
				deleteShortCut(sc.getSlot(), sc.getPage());
			}
		}
		allShortCuts = null;

		return oldSkill;
	}

	private void storeSkill(L2Skill newSkill, L2Skill oldSkill, int newClassIndex)
	{
		int classIndex = _classIndex;

		if(newClassIndex > -1)
		{
			classIndex = newClassIndex;
		}

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;

			if(oldSkill != null && newSkill != null)
			{
				statement = con.prepareStatement(UPDATE_CHARACTER_SKILL_LEVEL);
				statement.setInt(1, newSkill.getLevel());
				statement.setInt(2, oldSkill.getId());
				statement.setInt(3, getObjectId());
				statement.setInt(4, classIndex);
				statement.execute();
				statement.close();
			}
			else if(newSkill != null)
			{
				statement = con.prepareStatement(ADD_NEW_SKILL);
				statement.setInt(1, getObjectId());
				statement.setInt(2, newSkill.getId());
				statement.setInt(3, newSkill.getLevel());
				statement.setString(4, newSkill.getName());
				statement.setInt(5, classIndex);
				statement.execute();
				statement.close();
			}
			else
			{
				_log.warn("Could not store new skill. its NULL");
			}
		}
		catch(Exception e)
		{
			_log.error("Error could not store char skills", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public void checkPlayerSkills()
	{
		for(int id : _skills.keySet())
		{
			int level = getSkillLevel(id);
			if(level >= 100)
			{
				level = SkillTable.getInstance().getMaxLevel(id, id);
			}

			L2SkillLearn learn = SkillTreeTable.getInstance().getSkillLearnBySkillIdLevel(getClassId(), id, level);
			if(learn == null)
			{
				continue;
			}
			else
			{
				if(getLevel() < (learn.getMinLevel() - 9))
				{
					deacreaseSkillLevel(id);
				}
			}
		}
	}

	private void deacreaseSkillLevel(int id)
	{
		int nextLevel = -1;
		for(L2SkillLearn sl : SkillTreeTable.getInstance().getAllowedSkills(getClassId()))
		{
			if(sl.getId() == id && nextLevel < sl.getLevel() && getLevel() >= (sl.getMinLevel() - 9))
			{
				nextLevel = sl.getLevel();
			}
		}

		if(nextLevel == -1)
		{
			removeSkill(_skills.get(id), true);
		}
		else
		{
			addSkill(SkillTable.getInstance().getInfo(id, nextLevel), true);
		}
	}

	public void checkAllowedSkills()
	{
		boolean foundskill = false;
		if(!isGM())
		{
			Collection<L2SkillLearn> skillTree = SkillTreeTable.getInstance().getAllowedSkills(getClassId());
			for(L2Skill skill : getAllSkills())
			{
				int skillid = skill.getId();

				foundskill = false;
				for(L2SkillLearn temp : skillTree)
				{
					if(temp.getId() == skillid)
					{
						foundskill = true;
					}
				}

				if(isNoble() && skillid >= 325 && skillid <= 397)
				{
					foundskill = true;
				}

				if(isNoble() && skillid >= 1323 && skillid <= 1327)
				{
					foundskill = true;
				}

				if(isHero() && skillid >= 395 && skillid <= 396)
				{
					foundskill = true;
				}

				if(isHero() && skillid >= 1374 && skillid <= 1376)
				{
					foundskill = true;
				}

				if(isCursedWeaponEquiped() && skillid == CursedWeaponsManager.getInstance().getCursedWeapon(_cursedWeaponEquipedId).getSkillId())
				{
					foundskill = true;
				}

				if(getClan() != null && skillid >= 370 && skillid <= 391)
				{
					foundskill = true;
				}

				if(getClan() != null && (skillid == 246 || skillid == 247))
				{
					if(getClan().getLeaderId() == getObjectId())
					{
						foundskill = true;
					}
				}

				if(skillid >= 1312 && skillid <= 1322)
				{
					foundskill = true;
				}

				if(skillid >= 1368 && skillid <= 1373)
				{
					foundskill = true;
				}

				if(skillid >= 3000 && skillid < 7000)
				{
					foundskill = true;
				}

				if(Config.ALLOWED_SKILLS_LIST.contains(skillid))
				{
					foundskill = true;
				}

				if(!foundskill)
				{
					removeSkill(skill);
					sendMessage("Skill " + skill.getName() + " removed and gm informed!");
					_log.warn("Cheater! - Character " + getName() + " of Account " + getAccountName() + " got skill " + skill.getName() + " removed!" + IllegalPlayerAction.PUNISH_KICK);
				}
			}
			skillTree = null;
		}
	}

	public void restoreSkills()
	{
		Connection con = null;

		try
		{
			if(!Config.KEEP_SUBCLASS_SKILLS)
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(RESTORE_SKILLS_FOR_CHAR);
				statement.setInt(1, getObjectId());
				statement.setInt(2, getClassIndex());
				ResultSet rset = statement.executeQuery();

				while(rset.next())
				{
					int id = rset.getInt("skill_id");
					int level = rset.getInt("skill_level");

					if(id > 9000)
					{
						continue;
					}

					L2Skill skill = SkillTable.getInstance().getInfo(id, level);

					super.addSkill(skill);
				}

				rset.close();
				statement.close();
			}
			else
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(RESTORE_SKILLS_FOR_CHAR_ALT_SUBCLASS);
				statement.setInt(1, getObjectId());
				ResultSet rset = statement.executeQuery();

				while(rset.next())
				{
					int id = rset.getInt("skill_id");
					int level = rset.getInt("skill_level");

					if(id > 9000)
					{
						continue;
					}

					L2Skill skill = SkillTable.getInstance().getInfo(id, level);

					super.addSkill(skill);
				}

				rset.close();
				statement.close();
			}

		}
		catch(Exception e)
		{
			_log.error("Could not restore character skills", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public void restoreEffects()
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			ResultSet rset;

			statement = con.prepareStatement(RESTORE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.setInt(3, 0);
			rset = statement.executeQuery();

			while(rset.next())
			{
				int skillId = rset.getInt("skill_id");
				int skillLvl = rset.getInt("skill_level");
				int effectCount = rset.getInt("effect_count");
				int effectCurTime = rset.getInt("effect_cur_time");
				long reuseDelay = rset.getLong("reuse_delay");
				long systime = rset.getLong("systime");

				long remainingTime = systime - System.currentTimeMillis();
				if(skillId == -1 || effectCount == -1 || effectCurTime == -1 || reuseDelay < 0)
				{
					continue;
				}

				L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLvl);

				skill.getEffects(this, this);
				skill = null;

				if(reuseDelay > 10)
				{
					disableSkill(skillId, remainingTime);
					addTimeStamp(new TimeStamp(skillId, reuseDelay, systime));
				}

				for(L2Effect effect : getAllEffects())
				{
					if(effect.getSkill().getId() == skillId)
					{
						effect.setCount(effectCount);
						effect.setFirstTime(effectCurTime);
					}
				}
			}
			rset.close();
			statement.close();
			rset = null;
			statement = null;

			statement = con.prepareStatement(RESTORE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.setInt(3, 1);
			rset = statement.executeQuery();

			while(rset.next())
			{
				int skillId = rset.getInt("skill_id");
				long reuseDelay = rset.getLong("reuse_delay");
				long systime = rset.getLong("systime");

				long remainingTime = systime - System.currentTimeMillis();
				
				if(reuseDelay <= 10)
				{
					continue;
				}

				disableSkill(skillId, remainingTime);
				addTimeStamp(new TimeStamp(skillId, reuseDelay, systime));
			}
			rset.close();
			statement.close();
			rset = null;

			statement = con.prepareStatement(DELETE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.executeUpdate();
			statement.close();
			statement = null;
		}
		catch(Exception e)
		{
			_log.error("Could not restore active effect data", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}

		updateEffectIcons();
	}
	
	public void restoreHpMpOnLoad()
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			ResultSet rset;

			statement = con.prepareStatement(RESTORE_CHARACTER_HP_MP);
			statement.setInt(1, getObjectId());
			rset = statement.executeQuery();

			while(rset.next())
			{
				setCurrentHp(rset.getDouble("curHp"));
				setCurrentCp(rset.getDouble("curCp"));
				setCurrentMp(rset.getDouble("curMp"));
			}

			rset.close();
			statement.close();
			rset = null;
			statement = null;
		}
		catch(Exception e)
		{
			_log.warn("Could not restore active effect data: " + e);
		}
		finally
		{
			try { con.close(); } catch(Exception e) { }
			con = null;
		}
	}

	private void restoreHenna()
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_HENNAS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			ResultSet rset = statement.executeQuery();

			for(int i = 0; i < 3; i++)
			{
				_henna[i] = null;
			}

			while(rset.next())
			{
				int slot = rset.getInt("slot");

				if(slot < 1 || slot > 3)
				{
					continue;
				}

				int symbol_id = rset.getInt("symbol_id");

				L2HennaInstance sym = null;

				if(symbol_id != 0)
				{
					L2Henna tpl = HennaTable.getInstance().getTemplate(symbol_id);

					if(tpl != null)
					{
						sym = new L2HennaInstance(tpl);
						_henna[slot - 1] = sym;
						tpl = null;
						sym = null;
					}
				}
			}

			rset.close();
			statement.close();
		}
		catch(Exception e)
		{
			_log.error("could not restore henna", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}

		recalcHennaStats();
	}

	private void restoreRecom()
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_RECOMS);
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			while(rset.next())
			{
				_recomChars.add(rset.getInt("target_id"));
			}

			rset.close();
			statement.close();
		}
		catch(Exception e)
		{
			_log.error("could not restore recommendations", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public int getHennaEmptySlots()
	{
		int totalSlots = 1 + getClassId().level();

		for(int i = 0; i < 3; i++)
		{
			if(_henna[i] != null)
			{
				totalSlots--;
			}
		}

		if(totalSlots <= 0)
		{
			return 0;
		}

		return totalSlots;
	}

	public boolean removeHenna(int slot)
	{
		if(slot < 1 || slot > 3)
		{
			return false;
		}

		slot--;

		if(_henna[slot] == null)
		{
			return false;
		}

		L2HennaInstance henna = _henna[slot];
		_henna[slot] = null;

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_CHAR_HENNA);
			statement.setInt(1, getObjectId());
			statement.setInt(2, slot + 1);
			statement.setInt(3, getClassIndex());
			statement.execute();
			statement.close();
		}
		catch(Exception e)
		{
			_log.error("could not remove char henna", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}

		recalcHennaStats();

		sendPacket(new HennaInfo(this));

		sendPacket(new UserInfo(this));

		getInventory().addItem("Henna", henna.getItemIdDye(), henna.getAmountDyeRequire() / 2, this, null);

		sendPacket(new SystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(henna.getItemIdDye()).addNumber(henna.getAmountDyeRequire() / 2));

		henna = null;

		return true;
	}

	public boolean addHenna(L2HennaInstance henna)
	{
		if(getHennaEmptySlots() == 0)
		{
			sendMessage("You may not have more than three equipped symbols at a time.");
			return false;
		}

		for(int i = 0; i < 3; i++)
		{
			if(_henna[i] == null)
			{
				_henna[i] = henna;

				recalcHennaStats();

				Connection con = null;

				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement = con.prepareStatement(ADD_CHAR_HENNA);
					statement.setInt(1, getObjectId());
					statement.setInt(2, henna.getSymbolId());
					statement.setInt(3, i + 1);
					statement.setInt(4, getClassIndex());
					statement.execute();
					statement.close();
					statement = null;
				}
				catch(Exception e)
				{
					_log.error("could not save char henna", e);
				}
				finally
				{
					ResourceUtil.closeConnection(con); 
				}

				HennaInfo hi = new HennaInfo(this);
				sendPacket(hi);

				UserInfo ui = new UserInfo(this);
				sendPacket(ui);

				return true;
			}
		}

		return false;
	}

	private void recalcHennaStats()
	{
		_hennaINT = 0;
		_hennaSTR = 0;
		_hennaCON = 0;
		_hennaMEN = 0;
		_hennaWIT = 0;
		_hennaDEX = 0;

		for(int i = 0; i < 3; i++)
		{
			if(_henna[i] == null)
			{
				continue;
			}
			_hennaINT += _henna[i].getStatINT();
			_hennaSTR += _henna[i].getStatSTR();
			_hennaMEN += _henna[i].getStatMEM();
			_hennaCON += _henna[i].getStatCON();
			_hennaWIT += _henna[i].getStatWIT();
			_hennaDEX += _henna[i].getStatDEX();
		}

		if(_hennaINT > 5)
		{
			_hennaINT = 5;
		}

		if(_hennaSTR > 5)
		{
			_hennaSTR = 5;
		}

		if(_hennaMEN > 5)
		{
			_hennaMEN = 5;
		}

		if(_hennaCON > 5)
		{
			_hennaCON = 5;
		}

		if(_hennaWIT > 5)
		{
			_hennaWIT = 5;
		}

		if(_hennaDEX > 5)
		{
			_hennaDEX = 5;
		}
	}

	public L2HennaInstance getHennas(int slot)
	{
		if(slot < 1 || slot > 3)
		{
			return null;
		}

		return _henna[slot - 1];
	}

	public L2HennaInstance getHenna(int slot)
	{
		if (slot < 1 || slot > 3)
			return null;
		
		return _henna[slot - 1];
	}
	
	public L2HennaInstance[] getHennaList()
	{
		return _henna;
	}
	
	public int getHennaStatINT()
	{
		return _hennaINT;
	}

	public int getHennaStatSTR()
	{
		return _hennaSTR;
	}

	public int getHennaStatCON()
	{
		return _hennaCON;
	}

	public int getHennaStatMEN()
	{
		return _hennaMEN;
	}

	public int getHennaStatWIT()
	{
		return _hennaWIT;
	}

	public int getHennaStatDEX()
	{
		return _hennaDEX;
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		if(isInEvent())
		{
			return true;
		}

		if(attacker == this || attacker == getPet())
		{
			return false;
		}

		if(attacker instanceof L2MonsterInstance)
		{
			return true;
		}

		if(getParty() != null && getParty().getPartyMembers().contains(attacker))
		{
			return false;
		}

		if(attacker instanceof L2PcInstance && ((L2PcInstance) attacker).isInOlympiadMode())
		{
			if(isInOlympiadMode() && isOlympiadStart() && ((L2PcInstance) attacker).getOlympiadGameId() == getOlympiadGameId())
			{
				if(isFakeDeath())
				{
					return false;
				}
				else
				{
					return true;
				}
			}
			else
			{
				return false;
			}
		}

		if(getClan() != null && attacker != null && getClan().isMember(attacker.getName()))
		{
			return false;
		}

		if(attacker instanceof L2Playable && isInsideZone(ZONE_PEACE))
		{
			return false;
		}

		if(getKarma() > 0 || getPvpFlag() > 0)
		{
			return true;
		}

		if(attacker instanceof L2PcInstance)
		{
			if(getDuelState() == Duel.DUELSTATE_DUELLING && getDuelId() == ((L2PcInstance) attacker).getDuelId())
			{
				return true;
			}

			if(isInsideZone(ZONE_PVP) && ((L2PcInstance) attacker).isInsideZone(ZONE_PVP))
			{
				return true;
			}

			if(getClan() != null)
			{
				Siege siege = SiegeManager.getInstance().getSiege(getX(), getY(), getZ());
				if(siege != null)
				{
					if(siege.checkIsDefender(((L2PcInstance) attacker).getClan()) && siege.checkIsDefender(getClan()))
					{
						siege = null;
						return false;
					}

					if(siege.checkIsAttacker(((L2PcInstance) attacker).getClan()) && siege.checkIsAttacker(getClan()))
					{
						siege = null;
						return false;
					}
				}

				if(getClan() != null && ((L2PcInstance) attacker).getClan() != null && getClan().isAtWarWith(((L2PcInstance) attacker).getClanId()) && getWantsPeace() == 0 && ((L2PcInstance) attacker).getWantsPeace() == 0 && !isAcademyMember())
				{
					return true;
				}
			}
		}
		else if(attacker instanceof L2SiegeGuardInstance)
		{
			if(getClan() != null)
			{
				Siege siege = SiegeManager.getInstance().getSiege(this);
				return siege != null && siege.checkIsAttacker(getClan()) || DevastatedCastle.getInstance().getIsInProgress();
			}
		}

		return false;
	}

	public void useMagic(L2Skill skill, boolean forceUse, boolean dontMove)
	{
		if(isDead())
		{
			abortCast();
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if(isCastingNow())
        {
			sendPacket(ActionFailed.STATIC_PACKET);
        	return;
        }
		
		if(skill == null)
		{
			abortCast();
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(inObserverMode())
		{
			sendPacket(new SystemMessage(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE));
			abortCast();
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(isSitting() && !skill.isPotion())
		{
			sendPacket(new SystemMessage(SystemMessageId.CANT_MOVE_SITTING));

			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(skill.isToggle())
		{
            if (skill.getId() == 60 && isMounted())
            return;
            
			L2Effect effect = getFirstEffect(skill);

			if(effect != null)
			{
				effect.exit();

				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}

		if((skill.isPassive()) || (skill.isChance()) || (skill.bestowed()))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		int skill_id = skill.getId();
		int curr_skill_id = -1;
		SkillDat current = null;
		if((current = getCurrentSkill()) != null)
		{
			curr_skill_id = current.getSkillId();
		}

		if(_disabledSkills != null && _disabledSkills.contains(skill_id))
		{
			sendPacket(new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE).addSkillName(skill_id, skill.getLevel()));
			return;
		}

		if((skill_id == 13 || skill_id == 299 || skill_id == 448) && !SiegeManager.getInstance().checkIfOkToSummon(this, false))
		{
			return;
		}

		if(curr_skill_id != -1 && (isCastingNow()))
		{
			if(skill_id == curr_skill_id)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			setQueuedSkill(skill, forceUse, dontMove);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(getQueuedSkill() != null)
		{
			setQueuedSkill(null, false, false);
		}

		L2Object target = null;
		SkillTargetType sklTargetType = skill.getTargetType();
		L2SkillType sklType = skill.getSkillType();

		switch(sklTargetType)
		{
			case TARGET_AURA:
				if(isInOlympiadMode() && !isOlympiadStart())
				{
					setTarget(this);
				}
			case TARGET_PARTY:
			case TARGET_ALLY:
			case TARGET_CLAN:
			case TARGET_GROUND:
			case TARGET_CORPSE_ALLY:
			case TARGET_SELF:
				target = this;
				break;
			case TARGET_PET:
				target = getPet();
				break;
			default:
				target = getTarget();
				break;
		}

		if(target == null)
		{
			sendPacket(new SystemMessage(SystemMessageId.TARGET_CANT_FOUND));
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(skill.isOffensive() && target instanceof L2DoorInstance)
		{
			boolean isCastle = ((L2DoorInstance) target).getCastle() != null && ((L2DoorInstance) target).getCastle().getCastleId() > 0 && ((L2DoorInstance) target).getCastle().getSiege().getIsInProgress();

			if(!isCastle)
			{
				return;
			}
		}

		if(isInDuel())
		{
			if(!(target instanceof L2PcInstance && ((L2PcInstance) target).getDuelId() == getDuelId()) && !(target instanceof L2SummonInstance && ((L2Summon) target).getOwner().getDuelId() == getDuelId()))
			{
				sendMessage("You cannot do this while duelling.");
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}

		if(isSkillDisabled(skill_id) && !getAccessLevel().allowPeaceAttack())
		{
			sendPacket(new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE).addSkillName(skill_id));
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(isAllSkillsDisabled() && !getAccessLevel().allowPeaceAttack())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(skill.getSkillType() == L2SkillType.SIGNET || skill.getSkillType() == L2SkillType.SIGNET_CASTTIME)
		{
			if(isInsidePeaceZone(this))
			{
				sendPacket(new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill.getId()));
				return;
			}
		}

		if(getCurrentMp() < getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill))
		{
			sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_MP));
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(getCurrentHp() <= skill.getHpConsume()+1)
		{
			sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_HP));
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(skill.getItemConsume() > 0)
		{
			L2ItemInstance requiredItems = getInventory().getItemByItemId(skill.getItemConsumeId());

			if(requiredItems == null || requiredItems.getCount() < skill.getItemConsume())
			{
				if(sklType == L2SkillType.SUMMON)
				{
					sendPacket(new SystemMessage(SystemMessageId.SUMMONING_SERVITOR_COSTS_S2_S1).addItemName(skill.getItemConsumeId()).addNumber(skill.getItemConsume()));
					return;
				}
				else
				{
					sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					return;
				}
			}
		}

        // Like L2OFF if you are mounted on wyvern you can't use own skills
        if (isFlying())
        {
            if (skill_id != 327 && skill_id != 4289 && !skill.isPotion())
            {
                sendMessage("You cannot use skills while riding a wyvern.");
                return;
            }
        }

        // Like L2OFF if you have a summon you can't summon another one (ignore cubics)
        if(sklType == L2SkillType.SUMMON && skill instanceof L2SkillSummon && !((L2SkillSummon) skill).isCubic())
        {
            if (getPet() != null || isMounted())
            {
                sendPacket(new SystemMessage(SystemMessageId.YOU_ALREADY_HAVE_A_PET));
                return;
            }
        }
        
		EffectCharge effect = (EffectCharge) getFirstEffect(L2EffectType.CHARGE);
		if(skill.getNumCharges() > 0 && skill.getSkillType() != L2SkillType.CHARGE && skill.getSkillType() != L2SkillType.CHARGEDAM && skill.getSkillType() != L2SkillType.CHARGE_EFFECT && skill.getSkillType() != L2SkillType.PDAM)
		{
			if(effect == null || effect.numCharges < skill.getNumCharges())
			{
				sendPacket(new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE));
				return;
			}
			else
			{
				effect.numCharges -= skill.getNumCharges();
				sendPacket(new EtcStatusUpdate(this));

				if(effect.numCharges == 0)
				{
					effect.exit();
				}
			}
		}

		if(!skill.getWeaponDependancy(this))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(effect != null && effect.numCharges >= skill.getNumCharges() && skill.getSkillType() == L2SkillType.CHARGE)
		{
			sendPacket(new SystemMessage(SystemMessageId.FORCE_MAXIMUM));
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(!skill.checkCondition(this, target, false))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(isAlikeDead() && !skill.isPotion() && skill.getSkillType() != L2SkillType.FAKE_DEATH)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(isFishing() && sklType != L2SkillType.PUMPING && sklType != L2SkillType.REELING && sklType != L2SkillType.FISHING)
		{
			sendPacket(new SystemMessage(SystemMessageId.ONLY_FISHING_SKILLS_NOW));
			return;
		}

		if(skill.isOffensive())
		{
			Boolean peace = isInsidePeaceZone(this, target);

            if(peace
                && (skill.getId() != 3261 // Like L2OFF you can use cupid bow skills on peace zone
                && skill.getId() != 3260
                && skill.getId() != 3262 && sklTargetType != SkillTargetType.TARGET_AURA)) // Like L2OFF people can use TARGET_AURE skills on peace zone
            {
                // If L2Character or target is in a peace zone, send a system message TARGET_IN_PEACEZONE a Server->Client packet ActionFailed
                sendPacket(new SystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
                sendPacket(ActionFailed.STATIC_PACKET);
                return;
            }
            
			if(isInsidePeaceZone(this, target) && !getAccessLevel().allowPeaceAttack())
			{
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			if(isInOlympiadMode() && !isOlympiadStart() && sklTargetType != SkillTargetType.TARGET_AURA)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			if(!(target instanceof L2MonsterInstance) && sklType == L2SkillType.CONFUSE_MOB_ONLY)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			if(!target.isAttackable() && !getAccessLevel().allowPeaceAttack())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			// Check if a Forced ATTACK is in progress on non-attackable target
			if(!target.isAutoAttackable(this) && !forceUse)
			{
				switch (sklTargetType)
				{
					case TARGET_AURA:
					case TARGET_CLAN:
					case TARGET_ALLY:
					case TARGET_PARTY:
					case TARGET_SELF:
					case TARGET_GROUND:
					case TARGET_CORPSE_ALLY:
						break;
					default: // Send a Server->Client packet ActionFailed to the L2PcInstance
						sendPacket(ActionFailed.STATIC_PACKET);
						return;
				}
			}

			if(dontMove)
			{
				if(sklTargetType == SkillTargetType.TARGET_GROUND)
				{
					if(!isInsideRadius(getCurrentSkillWorldPosition().getX(), getCurrentSkillWorldPosition().getY(), getCurrentSkillWorldPosition().getZ(), skill.getCastRange() + getTemplate().getCollisionRadius(), false, false))
					{
						sendPacket(SystemMessageId.TARGET_TOO_FAR);
						sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
				}
				else if(skill.getCastRange() > 0 && !isInsideRadius(target, skill.getCastRange() + getTemplate().collisionRadius, false, false))
				{
					sendPacket(new SystemMessage(SystemMessageId.TARGET_TOO_FAR));
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
		}

		// Check if the skill is defensive
		if(!skill.isOffensive() && target instanceof L2MonsterInstance && !forceUse)
		{
			// check if the target is a monster and if force attack is set.. if not then we don't want to cast.
			switch (sklTargetType)
			{
				case TARGET_PET:
				case TARGET_AURA:
				case TARGET_CLAN:
				case TARGET_SELF:
				case TARGET_PARTY:
				case TARGET_ALLY:
				case TARGET_CORPSE_MOB:
				case TARGET_AREA_CORPSE_MOB:
				case TARGET_GROUND:
					break;
				default:
				{
					switch (sklType)
					{
						case BEAST_FEED:
						case DELUXE_KEY_UNLOCK:
						case UNLOCK:
							break;
						default:
							sendPacket(ActionFailed.STATIC_PACKET);
							return;
					}
					break;
				}
			}
		}

		if(sklType == L2SkillType.SPOIL)
		{
			if(!(target instanceof L2MonsterInstance))
			{
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}

		if(sklType == L2SkillType.SWEEP && target instanceof L2Attackable)
		{
			int spoilerId = ((L2Attackable) target).getIsSpoiledBy();

			if(((L2Attackable) target).isDead())
			{
				if(!((L2Attackable) target).isSpoil())
				{
					sendPacket(new SystemMessage(SystemMessageId.SWEEPER_FAILED_TARGET_NOT_SPOILED));
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}

				if(getObjectId() != spoilerId && !isInLooterParty(spoilerId))
				{
					sendPacket(new SystemMessage(SystemMessageId.SWEEP_NOT_ALLOWED));
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
		}

		if(sklType == L2SkillType.DRAIN_SOUL)
		{
			if(!(target instanceof L2MonsterInstance))
			{
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}

		if(sklTargetType == SkillTargetType.TARGET_GROUND)
		{
			final Point3D worldPosition = getCurrentSkillWorldPosition();

			if (worldPosition == null)
			{
				_log.info("WorldPosition is null for skill: " + skill.getName() + ", player: " + getName() + ".");
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			else if(!isInsideRadius(getCurrentSkillWorldPosition().getX(), getCurrentSkillWorldPosition().getY(), getCurrentSkillWorldPosition().getZ(), skill.getCastRange(), false, true))
			{
				// Send a System Message to the caster
				sendPacket(SystemMessageId.TARGET_TOO_FAR);

				// Send a Server->Client packet ActionFailed to the L2PcInstance
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}

		switch(sklTargetType)
		{
			case TARGET_PARTY:
			case TARGET_ALLY:
			case TARGET_CORPSE_ALLY:
			case TARGET_CLAN:
			case TARGET_AURA:
			case TARGET_SELF:
			case TARGET_GROUND:
				break;
			default:
				if(!checkPvpSkill(target, skill) && !getAccessLevel().allowPeaceAttack())
				{
					sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
		}

		if(sklTargetType == SkillTargetType.TARGET_HOLY && !TakeCastle.checkIfOkToCastSealOfRule(this, false))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			abortCast();
			return;
		}

		if(sklType == L2SkillType.SIEGEFLAG && !SiegeFlag.checkIfOkToPlaceFlag(this, false))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			abortCast();
			return;
		}
		else if(sklType == L2SkillType.STRSIEGEASSAULT && !StrSiegeAssault.checkIfOkToUseStriderSiegeAssault(this, false))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			abortCast();
			return;
		}

		if(skill.getCastRange() > 0 && !GeoData.getInstance().canSeeTarget(this, target))
		{
			sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}	
				
		setCurrentSkill(skill, forceUse, dontMove);

		super.useMagic(skill);
	}

	public boolean isInLooterParty(int LooterId)
	{
		L2PcInstance looter = L2World.getInstance().getPlayer(LooterId);

		if(isInParty() && getParty().isInCommandChannel() && looter != null)
		{
			return getParty().getCommandChannel().getMembers().contains(looter);
		}

		if(isInParty() && looter != null)
		{
			return getParty().getPartyMembers().contains(looter);
		}

		looter = null;

		return false;
	}

	/**
	 * Check if the requested casting is a Pc->Pc skill cast and if it's a valid pvp condition
	 * @param target L2Object instance containing the target
	 * @param skill L2Skill instance with the skill being casted
	 * @return False if the skill is a pvpSkill and target is not a valid pvp target
	 */
	public boolean checkPvpSkill(L2Object target, L2Skill skill)
	{
		return checkPvpSkill(target, skill, false);
	}
	
	/**
	 * Check if the requested casting is a Pc->Pc skill cast and if it's a valid pvp condition
	 * @param target L2Object instance containing the target
	 * @param skill L2Skill instance with the skill being casted
	 * @param srcIsSummon is L2Summon - caster?
	 * @return False if the skill is a pvpSkill and target is not a valid pvp target
	 */
	public boolean checkPvpSkill(L2Object target, L2Skill skill, boolean srcIsSummon)
	{
		// check for PC->PC Pvp status
		if (target instanceof L2Summon)
			target = target.getPlayer();
		
		if (target != null && target != this && target instanceof L2PcInstance && !(isInDuel() && ((L2PcInstance) target).getDuelId() == getDuelId()) && !isInsideZone(ZONE_PVP) && !((L2PcInstance) target).isInsideZone(ZONE_PVP))
		{
			SkillDat skilldat = getCurrentSkill();
			SkillDat skilldatpet = getCurrentPetSkill();
			
			if (skill.isPvpSkill()) // pvp skill
			{
				// in clan war player can attack whites even with sleep etc.
				if (getClan() != null && ((L2PcInstance) target).getClan() != null)
				{
					if (getClan().isAtWarWith(((L2PcInstance) target).getClan().getClanId()))
						return true;
				}
				
				// target's pvp flag is not set and target has no karma
				if (((L2PcInstance) target).getPvpFlag() == 0 && ((L2PcInstance) target).getKarma() == 0)
					return false;
			}
			else if ((skilldat != null && !skilldat.isCtrlPressed() && skill.isOffensive() && !srcIsSummon) || (skilldatpet != null && !skilldatpet.isCtrlPressed() && skill.isOffensive() && srcIsSummon))
			{
				// in clan war player can attack whites even with sleep etc.
				if (getClan() != null && ((L2PcInstance) target).getClan() != null)
				{
					if (getClan().isAtWarWith(((L2PcInstance) target).getClan().getClanId()))
						return true;
				}
				
				// target's pvp flag is not set and target has no karma
				if (((L2PcInstance) target).getPvpFlag() == 0 && ((L2PcInstance) target).getKarma() == 0)
					return false;
			}
		}
		return true;
	}

	@Override
	public void consumeItem(int itemConsumeId, int itemCount)
	{
		if(itemConsumeId != 0 && itemCount != 0)
		{
			destroyItemByItemId("Consume", itemConsumeId, itemCount, null, false);
		}
	}

	public boolean isMageClass()
	{
		return getClassId().isMage();
	}

	public boolean isMounted()
	{
		return _mountType > 0;
	}
	
	public boolean checkLandingState()
	{
		if(isInsideZone(ZONE_NOLANDING))
		{
			return true;
		}
		else if(isInsideZone(ZONE_SIEGE) && !(getClan() != null && CastleManager.getInstance().getCastle(this) == CastleManager.getInstance().getCastleByOwner(getClan()) && this == getClan().getLeader().getPlayerInstance()))
		{
			return true;
		}

		return false;
	}

	public boolean setMountType(int mountType)
	{
		if(checkLandingState() && mountType == 2)
		{
			return false;
		}

		switch(mountType)
		{
			case 0:
				setIsFlying(false);
				setIsRiding(false);
				break;
			case 1:
				setIsRiding(true);
				if(isNoble())
				{
					L2Skill striderAssaultSkill = SkillTable.getInstance().getInfo(325, 1);
					addSkill(striderAssaultSkill, false);
				}
				break;
			case 2:
				setIsFlying(true);
				break;
		}

		_mountType = mountType;

		UserInfo ui = new UserInfo(this);
		sendPacket(ui);
		ui = null;
		return true;
	}

	public int getMountType()
	{
		return _mountType;
	}

	@Override
	public void updateAbnormalEffect()
	{
		broadcastUserInfo();
	}

	public void tempInventoryDisable()
	{
		_inventoryDisable = true;

		ThreadPoolManager.getInstance().scheduleGeneral(new InventoryEnable(), 1500);
	}

	public boolean isInventoryDisabled()
	{
		return _inventoryDisable;
	}

	private class InventoryEnable implements Runnable
	{
		@Override
		public void run()
		{
			_inventoryDisable = false;
		}
	}

	public Map<Integer, L2CubicInstance> getCubics()
	{
		return _cubics;
	}

	public void addCubic(int id, int level)
	{
		L2CubicInstance cubic = new L2CubicInstance(this, id, level);
		_cubics.put(id, cubic);
		cubic = null;
	}

	public void delCubic(int id)
	{
		_cubics.remove(id);
	}

	public L2CubicInstance getCubic(int id)
	{
		return _cubics.get(id);
	}

	@Override
	public String toString()
	{
		return "player " + getName();
	}

	public int getEnchantEffect()
	{
		L2ItemInstance wpn = getActiveWeaponInstance();

		if(wpn == null)
		{
			return 0;
		}

		return Math.min(127, wpn.getEnchantLevel());
	}

	public void setLastFolkNPC(L2NpcInstance folkNpc)
	{
		_lastFolkNpc = folkNpc;
	}

	public L2NpcInstance getLastFolkNPC()
	{
		return _lastFolkNpc;
	}

	public void setSilentMoving(boolean flag)
	{
		_isSilentMoving = flag;
	}

	public boolean isSilentMoving()
	{
		return _isSilentMoving;
	}

	public boolean isFestivalParticipant()
	{
		return SevenSignsFestival.getInstance().isPlayerParticipant(this);
	}

	public void addAutoSoulShot(int itemId)
	{
		_activeSoulShots.put(itemId, itemId);
	}

	public void removeAutoSoulShot(int itemId)
	{
		_activeSoulShots.remove(itemId);
	}

	public Map<Integer, Integer> getAutoSoulShot()
	{
		return _activeSoulShots;
	}

	public void rechargeAutoSoulShot(boolean physical, boolean magic, boolean summon)
	{
		L2ItemInstance item;
		IItemHandler handler;

		if(_activeSoulShots == null || _activeSoulShots.size() == 0)
		{
			return;
		}

		for(int itemId : _activeSoulShots.values())
		{
			item = getInventory().getItemByItemId(itemId);

			if(item != null)
			{
				if(magic)
				{
					if(!summon)
					{
						if(itemId == 2509 || itemId == 2510 || itemId == 2511 || itemId == 2512 || itemId == 2513 || itemId == 2514 || itemId == 3947 || itemId == 3948 || itemId == 3949 || itemId == 3950 || itemId == 3951 || itemId == 3952 || itemId == 5790)
						{
							handler = ItemHandler.getInstance().getItemHandler(itemId);

							if(handler != null)
							{
								handler.useItem(this, item);
							}
						}
					}
					else
					{
						if(itemId == 6646 || itemId == 6647)
						{
							handler = ItemHandler.getInstance().getItemHandler(itemId);

							if(handler != null)
							{
								handler.useItem(this, item);
							}
						}
					}
				}

				if(physical)
				{
					if(!summon)
					{
						if(itemId == 1463 || itemId == 1464 || itemId == 1465 || itemId == 1466 || itemId == 1467 || itemId == 1835 || itemId == 5789)
						{
							handler = ItemHandler.getInstance().getItemHandler(itemId);

							if(handler != null)
							{
								handler.useItem(this, item);
							}
						}
					}
					else
					{
						if(itemId == 6645)
						{
							handler = ItemHandler.getInstance().getItemHandler(itemId);

							if(handler != null)
							{
								handler.useItem(this, item);
							}
						}
					}
				}
			}
			else
			{
				removeAutoSoulShot(itemId);
			}
		}

		item = null;
		handler = null;
	}

	private ScheduledFuture<?> _taskWarnUserTakeBreak;

	class WarnUserTakeBreak implements Runnable
	{
		@Override
		public void run()
		{
			if(isOnline() == 1)
			{
				SystemMessage msg = new SystemMessage(SystemMessageId.PLAYING_FOR_LONG_TIME);
				L2PcInstance.this.sendPacket(msg);
				msg = null;
			}
			else
			{
				stopWarnUserTakeBreak();
			}
		}
	}

	class RentPetTask implements Runnable
	{
		@Override
		public void run()
		{
			stopRentPet();
		}
	}

	public ScheduledFuture<?> _taskforfish;

	private boolean _isFlyingMounted = false;

	class WaterTask implements Runnable
	{
		@Override
		public void run()
		{
			double reduceHp = getMaxHp() / 100.0;

			if(reduceHp < 1)
			{
				reduceHp = 1;
			}

			reduceCurrentHp(reduceHp, L2PcInstance.this, false);
			sendPacket(new SystemMessage(SystemMessageId.DROWN_DAMAGE_S1).addNumber((int) reduceHp));
		}
	}

	class LookingForFishTask implements Runnable
	{
		boolean _isNoob, _isUpperGrade;
		int _fishType, _fishGutsCheck, _gutsCheckTime;
		long _endTaskTime;

		protected LookingForFishTask(int fishWaitTime, int fishGutsCheck, int fishType, boolean isNoob, boolean isUpperGrade)
		{
			_fishGutsCheck = fishGutsCheck;
			_endTaskTime = System.currentTimeMillis() + fishWaitTime + 10000;
			_fishType = fishType;
			_isNoob = isNoob;
			_isUpperGrade = isUpperGrade;
		}

		@Override
		public void run()
		{
			if(System.currentTimeMillis() >= _endTaskTime)
			{
				EndFishing(false);
				return;
			}

			if(_fishType == -1)
			{
				return;
			}

			int check = Rnd.get(1000);
			if(_fishGutsCheck > check)
			{
				stopLookingForFishTask();
				StartFishCombat(_isNoob, _isUpperGrade);
			}
		}

	}

	public int getClanPrivileges()
	{
		return _clanPrivileges;
	}

	public void setClanPrivileges(int n)
	{
		_clanPrivileges = n;
	}

	public boolean getAllowTrade()
	{
		return _allowTrade;
	}

	public void setAllowTrade(boolean a)
	{
		_allowTrade = a;
	}

	public void setPledgeClass(int classId)
	{
		_pledgeClass = classId;
	}

	public int getPledgeClass()
	{
		return _pledgeClass;
	}

	public void setPledgeType(int typeId)
	{
		_pledgeType = typeId;
	}

	public int getPledgeType()
	{
		return _pledgeType;
	}

	public int getApprentice()
	{
		return _apprentice;
	}

	public void setApprentice(int apprentice_id)
	{
		_apprentice = apprentice_id;
	}

	public int getSponsor()
	{
		return _sponsor;
	}

	public void setSponsor(int sponsor_id)
	{
		_sponsor = sponsor_id;
	}

	@Override
	public void sendMessage(String message)
	{
		sendPacket(SystemMessage.sendString(message));
	}

	public void enterObserverMode(int x, int y, int z)
	{
		_obsX = getX();
		_obsY = getY();
		_obsZ = getZ();

		if(getPet() != null)
		{
			getPet().unSummon(this);
		}

		if(getCubics().size() > 0)
		{
			for(L2CubicInstance cubic : getCubics().values())
			{
				cubic.stopAction();
				cubic.cancelDisappear();
			}

			getCubics().clear();
		}

		setTarget(null);
		stopMove(null);
		setIsParalyzed(true);
		setIsInvul(true);
		getAppearance().setInvisible();
		setXYZ(x, y, z);
		teleToLocation(x, y, z, false);
		sendPacket(new ObservationMode(x, y, z));
		_observerMode = true;
		broadcastUserInfo();
	}

	public void enterOlympiadObserverMode(int x, int y, int z, int id)
	{
		if(getPet() != null)
		{
			getPet().unSummon(this);
		}

		if(getCubics().size() > 0)
		{
			for(L2CubicInstance cubic : getCubics().values())
			{
				cubic.stopAction();
				cubic.cancelDisappear();
			}

			getCubics().clear();
		}

		_olympiadGameId = id;
		if(isSitting())
		{
			standUp();
		}
		_obsX = getX();
		_obsY = getY();
		_obsZ = getZ();
		setTarget(null);
		setIsInvul(true);
		getAppearance().setInvisible();
		teleToLocation(x, y, z, false);
		sendPacket(new ExOlympiadMode(3));
		_observerMode = true;
		broadcastUserInfo();
	}

	public void leaveObserverMode()
	{
		if(!_observerMode)
		{
			_log.warn("Player " + L2PcInstance.this.getName() + " request leave observer mode when he not use it!");
			Util.handleIllegalPlayerAction(L2PcInstance.this, "Warning!! Character " + L2PcInstance.this.getName() + " tried to cheat in observer mode.", Config.DEFAULT_PUNISH);
		}
		setTarget(null);
		setXYZ(_obsX, _obsY, _obsZ);
		setIsParalyzed(false);
		getAppearance().setVisible();
		setIsInvul(false);

		if(getAI() != null)
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}

		teleToLocation(_obsX, _obsY, _obsZ, false);
		setFalling();
		_observerMode = false;
		sendPacket(new ObservationReturn(this));
		broadcastUserInfo();
	}

	public boolean isFlyingMounted()
	{
		return _isFlyingMounted;
	}

	public void leaveOlympiadObserverMode()
	{
		setTarget(null);
		sendPacket(new ExOlympiadMode(0));
		teleToLocation(_obsX, _obsY, _obsZ, true);
		getAppearance().setVisible();
		setIsInvul(false);
		if(getAI() != null)
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}
		Olympiad.removeSpectator(_olympiadGameId, this);
		_olympiadGameId = -1;
		_observerMode = false;
		broadcastUserInfo();
	}

	public void updateNameTitleColor()
	{
		if(isMarried())
		{
			if(marriedType() == 1);

		}
	}

	public void updateGmNameTitleColor()
	{
		if(isGM() && !hasGmStatusActive())
		{
			getAppearance().setNameColor(0xFFFFFF);
			getAppearance().setTitleColor(0xFFFF77);
		}

		else if(isGM() && hasGmStatusActive())
		{
			if(getAccessLevel().useNameColor())
			{
				if(isNormalGm())
				{
					getAppearance().setNameColor(getAccessLevel().getNameColor());
				}
				else if(isAdministrator())
				{
					getAppearance().setNameColor(Config.GM_NAME_COLOR);
				}
			}
			else
			{
				getAppearance().setNameColor(0xFFFFFF);
			}

			if(getAccessLevel().useTitleColor())
			{
				if(isNormalGm())
				{
					getAppearance().setTitleColor(getAccessLevel().getTitleColor());
				}
				else if(isAdministrator())
				{
					getAppearance().setTitleColor(Config.GM_TITLE_COLOR);
				}
			}
			else
			{
				getAppearance().setTitleColor(0xFFFF77);
			}
		}
	}

	public void setOlympiadSide(int i)
	{
		_olympiadSide = i;
	}

	public int getOlympiadSide()
	{
		return _olympiadSide;
	}

	public void setOlympiadGameId(int id)
	{
		_olympiadGameId = id;
	}

	public int getOlympiadGameId()
	{
		return _olympiadGameId;
	}

	public int getObsX()
	{
		return _obsX;
	}

	public int getObsY()
	{
		return _obsY;
	}

	public int getObsZ()
	{
		return _obsZ;
	}

	public boolean inObserverMode()
	{
		return _observerMode;
	}

	public int getTeleMode()
	{
		return _telemode;
	}

	public void setTeleMode(int mode)
	{
		_telemode = mode;
	}

	public void setLoto(int i, int val)
	{
		_loto[i] = val;
	}

	public int getLoto(int i)
	{
		return _loto[i];
	}

	public void setRace(int i, int val)
	{
		_race[i] = val;
	}

	public int getRace(int i)
	{
		return _race[i];
	}

	/*public void setChatBanned(boolean isBanned)
	{
		_chatBanned = isBanned;

		if(isChatBanned())
		{
			sendMessage("Chat Disabled.");
		}
		else
		{
			sendMessage("Chat Enabled.");
			if(_chatUnbanTask != null)
			{
				_chatUnbanTask.cancel(false);
			}
			_chatUnbanTask = null;
		}

		sendPacket(new EtcStatusUpdate(this));
	}

	public boolean isChatBanned()
	{
		return _chatBanned;
	}

	public void setChatUnbanTask(ScheduledFuture<?> task)
	{
		_chatUnbanTask = task;
	}

	public ScheduledFuture<?> getChatUnbanTask()
	{
		return _chatUnbanTask;
	}*/

	public boolean getMessageRefusal()
	{
		return _messageRefusal;
	}

	public void setMessageRefusal(boolean mode)
	{
		_messageRefusal = mode;
		sendPacket(new EtcStatusUpdate(this));
	}

	public void setDietMode(boolean mode)
	{
		_dietMode = mode;
	}

	public long getNotMoveUntil() 
	{ 
		return _notMoveUntil; 
	} 
	
	public void updateNotMoveUntil() 
	{ 
		_notMoveUntil = System.currentTimeMillis() + Config.PLAYER_MOVEMENT_BLOCK_TIME; 
	}
 	
	public boolean getDietMode()
	{
		return _dietMode;
	}

	public boolean isInRefusalMode()
	{
		return _messageRefusal;
	}
	
	public void setInRefusalMode(boolean mode)
	{
		_messageRefusal = mode;
		sendPacket(new EtcStatusUpdate(this));
	}
	
	public void setTradeRefusal(boolean mode)
	{
		_tradeRefusal = mode;
	}
	
	public boolean getTradeRefusal()
	{
		return _tradeRefusal;
	}
	public void setExchangeRefusal(boolean mode)
	{
		_exchangeRefusal = mode;
	}

	public boolean getExchangeRefusal()
	{
		return _exchangeRefusal;
	}

	public BlockList getBlockList()
	{
		return _blockList;
	}

	public int getCount()
	{

		String HERO_COUNT = "SELECT count FROM heroes WHERE char_name = ?";
		int _count = 0;
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(HERO_COUNT);
			statement.setString(1, getName());
			ResultSet rset = statement.executeQuery();
			while(rset.next())
			{
				_count = rset.getInt("count");
			}

			rset.close();
			statement.close();
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}

		if(_count != 0)
		{
			return _count;
		}
		else
		{
			return 0;
		}
	}

	public void reloadPVPHeroAura()
	{
		sendPacket(new UserInfo(this));
	}

	public void setIsHero(boolean hero)
	{
		if(hero && _baseClass == _activeClass)
		{
			for(L2Skill s : HeroSkillTable.getHeroSkills())
			{
				addSkill(s, false);
			}
		}
		else
		{
			for(L2Skill s : HeroSkillTable.getHeroSkills())
			{
				super.removeSkill(s);
			}
		}

		_hero = hero;

		sendSkillList();
	}
	
	public void setWarehouseAccountId(String id)
	{
	    warehouseAccountId = id;
	}
	public String getWarehouseAccountId()
	{
	    return warehouseAccountId;
	}
	public void setWarehouseAccountPwd(String pwd)
	{
	    warehouseAccountPwd = pwd;
	}
	public String getWarehouseAccountPwd()
	{
	    return warehouseAccountPwd;
	}
	public void setHasWarehouseAccount(boolean i)
    {
	    hasWarehouseAccount = i;
	}
	public boolean hasWarehouseAccount()
	{
	    return hasWarehouseAccount;
	}

	public void setIsInOlympiadMode(boolean b)
	{
		_inOlympiadMode = b;
	}

	public void setIsOlympiadStart(boolean b)
	{
		_OlympiadStart = b;
	}

	public boolean isOlympiadStart()
	{
		return _OlympiadStart;
	}

	public void setOlympiadPosition(int[] pos)
	{
		_OlympiadPosition = pos;
	}

	public int[] getOlympiadPosition()
	{
		return _OlympiadPosition;
	}

	public boolean isHero()
	{
		return _hero;
	}

	public boolean isInOlympiadMode()
	{
		return _inOlympiadMode;
	}

	public boolean isInDuel()
	{
		return _isInDuel;
	}

	public int getDuelId()
	{
		return _duelId;
	}

	public void setDuelState(int mode)
	{
		_duelState = mode;
	}

	public int getDuelState()
	{
		return _duelState;
	}

	public void setCoupon(int coupon)
	{
		if(coupon >= 0 && coupon <= 3)
		{
			_hasCoupon = coupon;
		}
	}

	public void addCoupon(int coupon)
	{
		if(coupon == 1 || coupon == 2 && !getCoupon(coupon - 1))
		{
			_hasCoupon += coupon;
		}
	}

	public boolean getCoupon(int coupon)
	{
		return (_hasCoupon == 1 || _hasCoupon == 3) && coupon == 0 || (_hasCoupon == 2 || _hasCoupon == 3) && coupon == 1;
	}

	public void setIsInDuel(int duelId)
	{
		if(duelId > 0)
		{
			_isInDuel = true;
			_duelState = Duel.DUELSTATE_DUELLING;
			_duelId = duelId;
		}
		else
		{
			if(_duelState == Duel.DUELSTATE_DEAD)
			{
				enableAllSkills();
				getStatus().startHpMpRegeneration();
			}
			_isInDuel = false;
			_duelState = Duel.DUELSTATE_NODUEL;
			_duelId = 0;
		}
	}

	public SystemMessage getNoDuelReason()
	{
		SystemMessage sm = (new SystemMessage(SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL).addString(getName()));
		return sm;
	}

	public boolean canDuel()
	{
		if(isInCombat() || isInJail())
		{
			sendPacket(new SystemMessage(SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_ENGAGED_IN_BATTLE));
			return false;
		}

		if(isDead() || isAlikeDead() || getCurrentHp() < getMaxHp() / 2 || getCurrentMp() < getMaxMp() / 2)
		{
			sendPacket(new SystemMessage(SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1S_HP_OR_MP_IS_BELOW_50_PERCENT));
			return false;
		}

		if(isInDuel())
		{
			sendPacket(new SystemMessage(SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_ALREADY_ENGAGED_IN_A_DUEL));
			return false;
		}

		if(isInOlympiadMode())
		{
			sendPacket(new SystemMessage(SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_PARTICIPATING_IN_THE_OLYMPIAD));
			return false;
		}

		if(isCursedWeaponEquiped())
		{
			sendPacket(new SystemMessage(SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_IN_A_CHAOTIC_STATE));
			return false;
		}

		if(getPrivateStoreType() != STORE_PRIVATE_NONE)
		{
			sendPacket(new SystemMessage(SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_ENGAGED_IN_A_PRIVATE_STORE_OR_MANUFACTURE));
			return false;
		}

		if(isMounted() || isInBoat())
		{
			sendPacket(new SystemMessage(SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_RIDING_A_BOAT_WYVERN_OR_STRIDER));
			return false;
		}

		if(isFishing())
		{
			sendPacket(new SystemMessage(SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_FISHING));
			return false;
		}

		if(isInsideZone(ZONE_PVP) || isInsideZone(ZONE_PEACE) || isInsideZone(ZONE_SIEGE))
		{
			sendPacket(new SystemMessage(SystemMessageId.S1_CANNOT_MAKE_A_CHALLANGE_TO_A_DUEL_BECAUSE_S1_IS_CURRENTLY_IN_A_DUEL_PROHIBITED_AREA));
			return false;
		}

		return true;
	}

	public Map<Integer,L2Skill> returnSkills()
	{
		return _skills;
	}

	public boolean isNoble()
	{
		return _noble;
	}

	public void setNoble(boolean val)
	{
		if(val)
		{
			for(L2Skill s : NobleSkillTable.getInstance().GetNobleSkills())
			{
				addSkill(s, false);
			}
		}
		else
		{
			for(L2Skill s : NobleSkillTable.getInstance().GetNobleSkills())
			{
				super.removeSkill(s);
			}
		}
		_noble = val;

		sendSkillList();
	}

	public void setClanLeader(boolean val)
	{
		if(val)
		{
			for(L2Skill s : ClanLeaderSkillTable.getInstance().GetClanLeaderSkills())
			{
				addSkill(s, false);
			}
		}
		else
		{
			for(L2Skill s : ClanLeaderSkillTable.getInstance().GetClanLeaderSkills())
			{
				super.removeSkill(s);
			}
		}

		_clanLeader = val;

		sendSkillList();
	}

	public void setLvlJoinedAcademy(int lvl)
	{
		_lvlJoinedAcademy = lvl;
	}

	public int getLvlJoinedAcademy()
	{
		return _lvlJoinedAcademy;
	}

	public boolean isAcademyMember()
	{
		return _lvlJoinedAcademy > 0;
	}

	public void setTeam(int team)
	{
		_team = team;
	}

	public int getTeam()
	{
		return _team;
	}

	public void setWantsPeace(int wantsPeace)
	{
		_wantsPeace = wantsPeace;
	}

	public int getWantsPeace()
	{
		return _wantsPeace;
	}

	public boolean isFishing()
	{
		return _fishing;
	}

	public void setFishing(boolean fishing)
	{
		_fishing = fishing;
	}

	public void setAllianceWithVarkaKetra(int sideAndLvlOfAlliance)
	{
		_alliedVarkaKetra = sideAndLvlOfAlliance;
	}

	public int getAllianceWithVarkaKetra()
	{
		return _alliedVarkaKetra;
	}

	public boolean isAlliedWithVarka()
	{
		return _alliedVarkaKetra < 0;
	}

	public boolean isAlliedWithKetra()
	{
		return _alliedVarkaKetra > 0;
	}

	public void sendSkillList()
	{
		sendSkillList(this);
	}

	public void sendSkillList(L2PcInstance player)
	{
		SkillList sl = new SkillList();
		if(player != null)
		{
			for(L2Skill s : player.getAllSkills())
			{
				
				if(s == null)
				{
					continue;
				}

				if(s.getId() > 9000)
				{
					continue;
				}
				
				if(s.bestowed())
				{
					continue;
				}

				if(s.isChance())
				{
					sl.addSkill(s.getId(), s.getLevel(), s.isChance());
				}
				else
				{
					sl.addSkill(s.getId(), s.getLevel(), s.isPassive());
				}
			}
		}

		sendPacket(sl);
		sl = null;
	}

	public synchronized boolean addSubClass(int classId, int classIndex)
	{
		abortAttack();
		abortCast();
		
		if(getTotalSubClasses() == Config.ALLOWED_SUBCLASS || classIndex == 0)
		{
			return false;
		}

		if(getSubClasses().containsKey(classIndex))
		{
			return false;
		}

		startAbnormalEffect(L2Character.ABNORMAL_EFFECT_HOLD_1);
		setIsParalyzed(true);

		SubClass newClass = new SubClass();
		newClass.setClassId(classId);
		newClass.setClassIndex(classIndex);

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(ADD_CHAR_SUBCLASS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, newClass.getClassId());
			statement.setLong(3, newClass.getExp());
			statement.setInt(4, newClass.getSp());
			statement.setInt(5, newClass.getLevel());
			statement.setInt(6, newClass.getClassIndex());
			statement.execute();
			statement.close();
		}
		catch(Exception e)
		{
			_log.error("Could not add character sub class for " + getName(), e);
			return false;
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}

		getSubClasses().put(newClass.getClassIndex(), newClass);

		ClassId subTemplate = ClassId.values()[classId];
		Collection<L2SkillLearn> skillTree = SkillTreeTable.getInstance().getAllowedSkills(subTemplate);
		subTemplate = null;

		if(skillTree == null)
		{
			return true;
		}

		Map<Integer, L2Skill> prevSkillList = new FastMap<Integer, L2Skill>();

		for(L2SkillLearn skillInfo : skillTree)
		{
			if(skillInfo.getMinLevel() <= 40)
			{
				L2Skill prevSkill = prevSkillList.get(skillInfo.getId());
				L2Skill newSkill = SkillTable.getInstance().getInfo(skillInfo.getId(), skillInfo.getLevel());

				if(newSkill== null || prevSkill != null && prevSkill.getLevel() > newSkill.getLevel())
				{
					continue;
				}

				prevSkillList.put(newSkill.getId(), newSkill);
				storeSkill(newSkill, prevSkill, classIndex);
			}
		}
		skillTree = null;
		prevSkillList = null;

		stopAbnormalEffect(L2Character.ABNORMAL_EFFECT_HOLD_1);
		setIsParalyzed(false);

		return true;
	}

	public boolean modifySubClass(int classIndex, int newClassId)
	{
		getSubClasses().get(classIndex).getClassId();

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;

			statement = con.prepareStatement(DELETE_CHAR_HENNAS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, classIndex);
			statement.execute();
			statement.close();

			statement = con.prepareStatement(DELETE_CHAR_SHORTCUTS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, classIndex);
			statement.execute();
			statement.close();

			statement = con.prepareStatement(DELETE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, classIndex);
			statement.execute();
			statement.close();

			statement = con.prepareStatement(DELETE_CHAR_SKILLS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, classIndex);
			statement.execute();
			statement.close();

			statement = con.prepareStatement(DELETE_CHAR_SUBCLASS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, classIndex);
			statement.execute();
			statement.close();
		}
		catch(Exception e)
		{
			_log.warn("Could not modify sub class for " + getName() + " to class index " + classIndex + ": " + e);

			getSubClasses().remove(classIndex);
			return false;
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}

		getSubClasses().remove(classIndex);
		return addSubClass(newClassId, classIndex);
	}

	public boolean isSubClassActive()
	{
		return _classIndex > 0;
	}

	public Map<Integer, SubClass> getSubClasses()
	{
		if(_subClasses == null)
		{
			_subClasses = new FastMap<Integer, SubClass>();
		}

		return _subClasses;
	}

	public int getTotalSubClasses()
	{
		return getSubClasses().size();
	}

	public int getBaseClass()
	{
		return _baseClass;
	}

	public int getActiveClass()
	{
		return _activeClass;
	}

	public int getClassIndex()
	{
		return _classIndex;
	}

	private void setClassTemplate(int classId)
	{
		_activeClass = classId;

		L2PcTemplate t = CharTemplateTable.getInstance().getTemplate(classId);

		if(t == null)
		{
			_log.error("Missing template for classId: " + classId);
			throw new Error();
		}

		setTemplate(t);
		t = null;
	}

	public synchronized boolean setActiveClass(int classIndex)
	{
		if(isInCombat())
		{
			sendMessage("You can not change class while in combat.");
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		L2ItemInstance rhand = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);

		if(rhand != null)
		{
			L2ItemInstance[] unequipped = getInventory().unEquipItemInBodySlotAndRecord(rhand.getItem().getBodyPart());
			InventoryUpdate iu = new InventoryUpdate();

			for(L2ItemInstance element : unequipped)
			{
				iu.addModifiedItem(element);
			}

			sendPacket(iu);
		}

		L2ItemInstance lhand = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);

		if(lhand != null)
		{
			L2ItemInstance[] unequipped = getInventory().unEquipItemInBodySlotAndRecord(lhand.getItem().getBodyPart());
			InventoryUpdate iu = new InventoryUpdate();

			for(L2ItemInstance element : unequipped)
			{
				iu.addModifiedItem(element);
			}

			sendPacket(iu);
		}

		if(_forceBuff != null)
		{
			abortCast();
		}

		store();
		_reuseTimeStamps.clear();
		
		if(classIndex == 0)
		{
			setClassTemplate(getBaseClass());
		}
		else
		{
			try
			{
				setClassTemplate(getSubClasses().get(classIndex).getClassId());
			}
			catch(Exception e)
			{
				_log.error("Could not switch " + getName() + "'s sub class to class index " + classIndex, e);
				return false;
			}
		}
		_classIndex = classIndex;

		if(isInParty())
		{
			getParty().recalculatePartyLevel();
		}

		if(getPet() != null && getPet() instanceof L2SummonInstance)
		{
			getPet().unSummon(this);
		}

		if(getCubics().size() > 0)
		{
			for(L2CubicInstance cubic : getCubics().values())
			{
				cubic.stopAction();
				cubic.cancelDisappear();
			}

			getCubics().clear();
		}

		for(L2Character character : getKnownList().getKnownCharacters())
		{
			if(character.getForceBuff() != null && character.getForceBuff().getTarget() == this)
			{
				character.abortCast();
			}
		}

		for(L2Skill oldSkill : getAllSkills())
		{
			super.removeSkill(oldSkill);
		}

		if(isCursedWeaponEquiped())
		{
			CursedWeaponsManager.getInstance().givePassive(_cursedWeaponEquipedId);
		}

		stopAllEffectsExceptThoseThatLastThroughDeath();
		stopAllEffects();

		if(isSubClassActive())
		{
			_dwarvenRecipeBook.clear();
			_commonRecipeBook.clear();
		}
		else
		{
			restoreRecipeBook();
		}

		restoreDeathPenaltyBuffLevel();

		restoreSkills();
		regiveTemporarySkills();
		rewardSkills();

		// Prevents some issues when changing between subclases that shares skills
		if (_disabledSkills != null && !_disabledSkills.isEmpty())
			_disabledSkills.clear();
		
		restoreEffects();
		updateEffectIcons();
					
		getInventory().reloadEquippedItems();
		
		checkAllowedSkills();
		
		sendPacket(new EtcStatusUpdate(this));

		QuestState st = getQuestState("422_RepentYourSins");

		if(st != null)
		{
			st.exitQuest(true);
			st = null;
		}

		for(int i = 0; i < 3; i++)
		{
			_henna[i] = null;
		}

		restoreHenna();
		sendPacket(new HennaInfo(this));

		if(getCurrentHp() > getMaxHp())
		{
			setCurrentHp(getMaxHp());
		}

		if(getCurrentMp() > getMaxMp())
		{
			setCurrentMp(getMaxMp());
		}

		if(getCurrentCp() > getMaxCp())
		{
			setCurrentCp(getMaxCp());
		}

		broadcastUserInfo();
		refreshOverloaded();
		refreshExpertisePenalty();

		setExpBeforeDeath(0);
		_macroses.restore();
		_macroses.sendUpdate();
		_shortCuts.restore();
		sendPacket(new ShortCutInit(this));
		sendPacket(new SkillCoolTime(this));
		broadcastPacket(new SocialAction(getObjectId(), 15));

		return true;
	}

	public void broadcastClassIcon()
	{
		if(isInParty())
		{
			getParty().broadcastToPartyMembers(new PartySmallWindowUpdate(this));
		}

		if(getClan() != null)
		{
			getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(this));
		}
	}

	public void stopWarnUserTakeBreak()
	{
		if(_taskWarnUserTakeBreak != null)
		{
			_taskWarnUserTakeBreak.cancel(true);
			_taskWarnUserTakeBreak = null;
		}
	}

	public void startWarnUserTakeBreak()
	{
		if(_taskWarnUserTakeBreak == null)
		{
			_taskWarnUserTakeBreak = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new WarnUserTakeBreak(), 7200000, 7200000);
		}
	}

	public void checkAnswer(int id)
	{
		if(id - 100000 == _correctWord)
		{
			_stopKickBotTask = true;
		}
		else
		{
			closeNetConnection();
		}
	}

	public void stopRentPet()
	{
		if(_taskRentPet != null)
		{
			if(checkLandingState() && getMountType() == 2)
			{
				teleToLocation(MapRegionTable.TeleportWhereType.Town);
			}

			if(setMountType(0))
			{
				_taskRentPet.cancel(true);
				Ride dismount = new Ride(getObjectId(), Ride.ACTION_DISMOUNT, 0);
				sendPacket(dismount);
				broadcastPacket(dismount);
				dismount = null;
				_taskRentPet = null;
			}
		}
	}

	public boolean sendPacket(SystemMessageId hacking_tool)
	{
		sendMessage("Please try again after closing unnecessary programs!.");
		return true;
	}

	public void startRentPet(int seconds)
	{
		if(_taskRentPet == null)
		{
			_taskRentPet = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new RentPetTask(), seconds * 1000L, seconds * 1000L);
		}
	}

	public boolean isRentedPet()
	{
		if(_taskRentPet != null)
		{
			return true;
		}

		return false;
	}

	public void stopWaterTask()
	{
		if(_taskWater != null)
		{
			_taskWater.cancel(false);
			_taskWater = null;
			sendPacket(new SetupGauge(2, 0));
			isFalling(0);
			broadcastUserInfo();
		}
	}

	public void startWaterTask()
	{
		broadcastUserInfo();
		if(!isDead() && _taskWater == null)
		{
			int timeinwater = 86000;

			sendPacket(new SetupGauge(2, timeinwater));
			_taskWater = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new WaterTask(), timeinwater, 1000);
		}
	}

	public boolean isInWater()
	{
		if(_taskWater != null)
		{
			return true;
		}

		return false;
	}

	public void checkWaterState()
	{
		if(isInsideZone(128))
		{
			startWaterTask();
		}
		else
		{
			stopWaterTask();
		}
	}

	public void onPlayerEnter()
	{

		startWarnUserTakeBreak();

		if(SevenSigns.getInstance().isSealValidationPeriod() || SevenSigns.getInstance().isCompResultsPeriod())
		{
			if(!isGM() && isIn7sDungeon() && SevenSigns.getInstance().getPlayerCabal(this) != SevenSigns.getInstance().getCabalHighestScore())
			{
				teleToLocation(MapRegionTable.TeleportWhereType.Town);
				setIsIn7sDungeon(false);
				sendMessage("You have been teleported to the nearest town due to the beginning of the Seal Validation period.");
			}
		}
		else
		{
			if(!isGM() && isIn7sDungeon() && SevenSigns.getInstance().getPlayerCabal(this) == SevenSigns.CABAL_NULL)
			{
				teleToLocation(MapRegionTable.TeleportWhereType.Town);
				setIsIn7sDungeon(false);
				sendMessage("You have been teleported to the nearest town because you have not signed for any cabal.");
			}
		}

		updatePunishState();

		if (isGM())
		{
			if(isInvul())
				sendChatMessage(0, 0, "SYS", "Entering world in Invulnerable mode.");
			
			if(getAppearance().getInvisible())
				sendChatMessage(0, 0, "SYS", "Hide is default for builder.");
			
			if(getMessageRefusal())
				sendChatMessage(0, 0, "SYS", "Entering world in Message Refusal mode.");
		}
		
		revalidateZone(true);

		if(Config.CHECK_SKILLS_DELEVEL && !isGM())
		{
			checkPlayerSkills();
		}

		decayMe();
		spawnMe();
		broadcastUserInfo();
	}

	public long getLastAccess()
	{
		return _lastAccess;
	}

	private void checkRecom(int recsHave, int recsLeft)
	{
		Calendar check = Calendar.getInstance();
		check.setTimeInMillis(_lastRecomUpdate);
		check.add(Calendar.DAY_OF_MONTH, 1);

		Calendar min = Calendar.getInstance();

		_recomHave = recsHave;
		_recomLeft = recsLeft;

		if(getStat().getLevel() < 10 || check.after(min))
		{
			return;
		}

		restartRecom();
	}

	public void restartRecom()
	{
		if(Config.ALT_RECOMMEND)
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(DELETE_CHAR_RECOMS);
				statement.setInt(1, getObjectId());
				statement.execute();
				statement.close();

				_recomChars.clear();
			}
			catch(Exception e)
			{
				_log.error("could not clear char recommendations", e);
			}
			finally
			{
				ResourceUtil.closeConnection(con);
			}
		}

		if(getStat().getLevel() < 20)
		{
			_recomLeft = 3;
			_recomHave--;
		}
		else if(getStat().getLevel() < 40)
		{
			_recomLeft = 6;
			_recomHave -= 2;
		}
		else
		{
			_recomLeft = 9;
			_recomHave -= 3;
		}

		if(_recomHave < 0)
		{
			_recomHave = 0;
		}

		Calendar update = Calendar.getInstance();
		if(update.get(Calendar.HOUR_OF_DAY) < 13)
		{
			update.add(Calendar.DAY_OF_MONTH, -1);
		}

		update.set(Calendar.HOUR_OF_DAY, 13);
		_lastRecomUpdate = update.getTimeInMillis();
	}

	@Override
	public void doRevive()
	{
		super.doRevive();
		updateEffectIcons();
		sendPacket(new EtcStatusUpdate(this));
		_reviveRequested = 0;
		_revivePower = 0;

		if(isInParty() && getParty().isInDimensionalRift())
		{
			if(!DimensionalRiftManager.getInstance().checkIfInPeaceZone(getX(), getY(), getZ()))
			{
				getParty().getDimensionalRift().memberRessurected(this);
			}
		}
	}

	@Override
	public void doRevive(double revivePower)
	{
		restoreExp(revivePower);
		doRevive();
	}

	public void reviveRequest(L2PcInstance Reviver, L2Skill skill, boolean Pet)
	{
		if(_reviveRequested == 1)
		{
			if(_revivePet == Pet)
			{
				Reviver.sendPacket(new SystemMessage(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED));
			}
			else
			{
				if(Pet)
				{
					Reviver.sendPacket(new SystemMessage(SystemMessageId.CANNOT_RES_PET2));
				}
				else
				{
					Reviver.sendPacket(new SystemMessage(SystemMessageId.MASTER_CANNOT_RES));
				}
			}
			return;
		}

		if(Pet && getPet() != null && getPet().isDead() || !Pet && isDead())
		{
			_reviveRequested = 1;
			if(isPhoenixBlessed())
			{
				_revivePower = 100;
			}
			else if(skill != null)
			{
				_revivePower = Formulas.getInstance().calculateSkillResurrectRestorePercent(skill.getPower(), Reviver.getWIT());
			}
			else
			{
				_revivePower = 0;
			}
			_revivePet = Pet;
			ConfirmDlg dlg = new ConfirmDlg(SystemMessageId.RESSURECTION_REQUEST.getId());
			dlg.addString(Reviver.getName());
			sendPacket(dlg);
			dlg = null;
		}
	}

	public void reviveAnswer(int answer)
	{
		if(_reviveRequested != 1 || !isDead() && !_revivePet || _revivePet && getPet() != null && !getPet().isDead())
		{
			return;
		}

		if(answer == 0 && ((L2Playable) this).isPhoenixBlessed())
		{
			((L2Playable) this).stopPhoenixBlessing(null);
			stopAllEffects();
		}

		if(answer == 1)
		{
			if(!_revivePet)
			{
				if(_revivePower != 0)
				{
					doRevive(_revivePower);
				}
				else
				{
					doRevive();
				}
			}
			else if(getPet() != null)
			{
				if(_revivePower != 0)
				{
					getPet().doRevive(_revivePower);
				}
				else
				{
					getPet().doRevive();
				}
			}
		}
		_reviveRequested = 0;
		_revivePower = 0;
	}

	public boolean isReviveRequested()
	{
		return _reviveRequested == 1;
	}

	public boolean isRevivingPet()
	{
		return _revivePet;
	}

	public void removeReviving()
	{
		_reviveRequested = 0;
		_revivePower = 0;
	}

	public void onActionRequest()
	{
		if (isSpawnProtected())
			sendMessage("You are no longer under spawn protection.");
		setProtection(false);
	}

	public void setExpertiseIndex(int expertiseIndex)
	{
		_expertiseIndex = expertiseIndex;
	}

	public int getExpertiseIndex()
	{
		return _expertiseIndex;
	}

	@Override
	public final void onTeleported()
	{
		super.onTeleported();

		revalidateZone(true);

		if(Config.PLAYER_SPAWN_PROTECTION > 0)
		{
			setProtection(true);
		}

		if(Config.ALLOW_WATER)
		{
			checkWaterState();
		}

		if(getTrainedBeast() != null)
		{
			getTrainedBeast().getAI().stopFollow();
			getTrainedBeast().teleToLocation(getPosition().getX() + Rnd.get(-100, 100), getPosition().getY() + Rnd.get(-100, 100), getPosition().getZ(), false);
			getTrainedBeast().getAI().startFollow(this);
		}

		broadcastUserInfo();
		
	}

	@Override
	public final boolean updatePosition(int gameTicks)
	{
		if(Config.COORD_SYNCHRONIZE == -1)
		{
			return super.updatePosition(gameTicks);
		}

		MoveData m = _move;

		if(_move == null)
		{
			return true;
		}

		if(!isVisible())
		{
			_move = null;
			return true;
		}

		if(m._moveTimestamp == 0)
		{
			m._moveTimestamp = m._moveStartTime;
		}

		if(m._moveTimestamp == gameTicks)
		{
			return false;
		}

		double dx = m._xDestination - getX();
		double dy = m._yDestination - getY();
		double dz = m._zDestination - getZ();
		int distPassed = (int) getStat().getMoveSpeed() * (gameTicks - m._moveTimestamp) / GameTimeController.TICKS_PER_SECOND;
		double distFraction = distPassed / Math.sqrt(dx * dx + dy * dy + dz * dz);

		if(distFraction > 1)
		{
			super.setXYZ(m._xDestination, m._yDestination, m._zDestination);
		}
		else
		{
			super.setXYZ(getX() + (int) (dx * distFraction + 0.5), getY() + (int) (dy * distFraction + 0.5), getZ() + (int) (dz * distFraction + 0.5));
		}

		m._moveTimestamp = gameTicks;

		revalidateZone(false);

		return distFraction > 1;
	}

	public void setLastClientPosition(int x, int y, int z)
	{
		_lastClientPosition.setXYZ(x, y, z);
	}

	public void setLastClientPosition(Location loc) {
		_lastClientPosition = loc;
	}

	public boolean checkLastClientPosition(int x, int y, int z)
	{
		return _lastClientPosition.equals(x, y, z);
	}

	public int getLastClientDistance(int x, int y, int z)
	{
		double dx = x - _lastClientPosition.getX();
		double dy = y - _lastClientPosition.getY();
		double dz = z - _lastClientPosition.getZ();

		return (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	public void setLastServerPosition(int x, int y, int z)
	{
		_lastServerPosition.setXYZ(x, y, z);
	}

	public void setLastServerPosition(Location loc) {
		_lastServerPosition = loc;
	}

	public boolean checkLastServerPosition(int x, int y, int z)
	{
		return _lastServerPosition.equals(x, y, z);
	}

	public int getLastServerDistance(int x, int y, int z)
	{
		double dx = x - _lastServerPosition.getX();
		double dy = y - _lastServerPosition.getY();
		double dz = z - _lastServerPosition.getZ();

		return (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	@Override
	public void addExpAndSp(long addToExp, int addToSp)
	{
		if(_expGainOn)
		{
			getStat().addExpAndSp(addToExp, addToSp);
		}
		else
		{
			getStat().addExpAndSp(0, 0);
		}
	}

	public void removeExpAndSp(long removeExp, int removeSp)
	{
		getStat().removeExpAndSp(removeExp, removeSp);
	}

	public static boolean checkSummonerStatus(L2PcInstance summonerChar)
	{
		if (summonerChar == null)
			return false;
		
		if (summonerChar.isInOlympiadMode())
		{
			summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
			return false;
		}
		
		if (summonerChar.inObserverMode())
		{
			return false;
		}
		
		if (summonerChar.isInsideZone(L2Character.ZONE_NOSUMMONFRIEND) || summonerChar.isFlying() || summonerChar.isMounted())
		{
			summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
			return false;
		}
		return true;
	}
	
	public static boolean checkSummonTargetStatus(L2Object target, L2PcInstance summonerChar)
	{
		if (target == null || !(target instanceof L2PcInstance))
			return false;
		
		L2PcInstance targetChar = (L2PcInstance) target;
		
		if (targetChar.isAlikeDead())
		{
			return false;
		}
		
		if (targetChar.isInStoreMode())
		{
			return false;
		}
		
		if (targetChar.isRooted() || targetChar.isInCombat())
		{
			return false;
		}
		
		if (targetChar.isInOlympiadMode())
		{
			summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_CANNOT_SUMMON_PLAYERS_WHO_ARE_IN_OLYMPIAD));
			return false;
		}
		
		if (targetChar.isFestivalParticipant() || targetChar.isFlying())
		{
			summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
			return false;
		}
		
		if (targetChar.inObserverMode())
		{
			return false;
		}
		
		if (targetChar.isInCombat())
		{
			summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING));
			return false;
		}
		
		if (targetChar.isInsideZone(L2Character.ZONE_NOSUMMONFRIEND))
		{
			return false;
		}
		
		return true;
	}
	
	private class Dismount implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				L2PcInstance.this.dismount();
			}
			catch (Exception e)
			{
				_log.warn("Exception on dismount(): " + e.getMessage(), e);
			}
		}
	}
	
	public void enteredNoLanding(int delay)
	{
		_dismountTask = ThreadPoolManager.getInstance().scheduleGeneral(new L2PcInstance.Dismount(), delay * 1000);
	}
	
	public void exitedNoLanding()
	{
		if (_dismountTask != null)
		{
			_dismountTask.cancel(true);
			_dismountTask = null;
		}
	}
	
	@Override
	public void reduceCurrentHp(double i, L2Character attacker)
	{
		getStatus().reduceHp(i, attacker);

		if(getTrainedBeast() != null)
		{
			getTrainedBeast().onOwnerGotAttacked(attacker);
		}
	}
	
	/**
	 * Returns the Number of Charges this L2PcInstance got.
	 * @return
	 */
	public int getCharges()
	{
		return _charges.get();
	}
	
	public synchronized void increaseCharges(int count, int max)
	{
		if (_charges.get() >= max)
		{
			sendPacket(SystemMessageId.FORCE_MAXLEVEL_REACHED);
			return;
		}
		else
		{
			// if no charges - start clear task
			if (_charges.get() == 0)
				restartChargeTask();
		}
		
		if (_charges.addAndGet(count) >= max)
		{
			_charges.set(max);
			sendPacket(SystemMessageId.FORCE_MAXLEVEL_REACHED);
		}
		else
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FORCE_INCREASED_TO_S1).addNumber(_charges.get()));
		
		sendPacket(new EtcStatusUpdate(this));
	}
	
	public synchronized boolean decreaseCharges(int count)
	{
		if (_charges.get() < count)
			return false;
		
		if (_charges.addAndGet(-count) == 0)
			stopChargeTask();
		
		sendPacket(new EtcStatusUpdate(this));
		return true;
	}
	
	public void clearCharges()
	{
		_charges.set(0);
		sendPacket(new EtcStatusUpdate(this));
	}
	
	/**
	 * Starts/Restarts the ChargeTask to Clear Charges after 10 Mins.
	 */
	private void restartChargeTask()
	{
		if (_chargeTask != null)
		{
			_chargeTask.cancel(false);
			_chargeTask = null;
		}
		_chargeTask = ThreadPoolManager.getInstance().scheduleGeneral(new ChargeTask(), 600000);
	}
	
	/**
	 * Stops the Charges Clearing Task.
	 */
	public void stopChargeTask()
	{
		if (_chargeTask != null)
		{
			_chargeTask.cancel(false);
			_chargeTask = null;
		}
	}
	
	protected class ChargeTask implements Runnable
	{
		@Override
		public void run()
		{
			L2PcInstance.this.clearCharges();
		}
	}
	
	public boolean teleportRequest(L2PcInstance requester, L2Skill skill)
	{
		if (_summonRequest.getTarget() != null && requester != null)
			return false;
		_summonRequest.setTarget(requester, skill);
		return true;
	}
	
	/** Action teleport **/
	public void teleportAnswer(int answer, int requesterId)
	{
		if (_summonRequest.getTarget() == null)
			return;
		if (answer == 1 && _summonRequest.getTarget().getObjectId() == requesterId)
		{
			teleToTarget(this, _summonRequest.getTarget(), _summonRequest.getSkill());
		}
		_summonRequest.setTarget(null, null);
	}
	
	public static void teleToTarget(L2PcInstance targetChar, L2PcInstance summonerChar, L2Skill summonSkill)
	{
		if (targetChar == null || summonerChar == null || summonSkill == null)
			return;
		
		if (!checkSummonerStatus(summonerChar))
			return;
		if (!checkSummonTargetStatus(targetChar, summonerChar))
			return;
		
		int itemConsumeId = summonSkill.getTargetConsumeId();
		int itemConsumeCount = summonSkill.getTargetConsume();
		if (itemConsumeId != 0 && itemConsumeCount != 0)
		{
			if (targetChar.getInventory().getInventoryItemCount(itemConsumeId, 0) < itemConsumeCount)
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_REQUIRED_FOR_SUMMONING);
				sm.addItemName(summonSkill.getTargetConsumeId());
				targetChar.sendPacket(sm);
				return;
			}
			targetChar.getInventory().destroyItemByItemId("Consume", itemConsumeId, itemConsumeCount, summonerChar, targetChar);
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
			sm.addItemName(summonSkill.getTargetConsumeId());
			targetChar.sendPacket(sm);
		}
		targetChar.teleToLocation(summonerChar.getX(), summonerChar.getY(), summonerChar.getZ(), true);
	}

	@Override
	public void reduceCurrentHp(double value, L2Character attacker, boolean awake)
	{
		getStatus().reduceHp(value, attacker, awake);

		if(getTrainedBeast() != null)
		{
			getTrainedBeast().onOwnerGotAttacked(attacker);
		}
	}

	public void broadcastSnoop(int type, String name, String _text, CreatureSay cs)
	{
		if(_snoopListener.size() > 0)
		{
			Snoop sn = new Snoop(this, type, name, _text);
			for (L2PcInstance pci : _snoopListener)
			{
				if(pci != null)
				{
					pci.sendPacket(cs);
					pci.sendPacket(sn);
				}
			}
		}
	}

	public void addSnooper(L2PcInstance pci)
	{
		if(!_snoopListener.contains(pci))
		{
			_snoopListener.add(pci);
		}
	}

	public void removeSnooper(L2PcInstance pci)
	{
		_snoopListener.remove(pci);
	}

	public void addSnooped(L2PcInstance pci)
	{
		if(!_snoopedPlayer.contains(pci))
		{
			_snoopedPlayer.add(pci);
		}
	}

	public void removeSnooped(L2PcInstance pci)
	{
		_snoopedPlayer.remove(pci);
	}

	public synchronized void addBypass(String bypass)
	{
		if(bypass == null)
		{
			return;
		}
		_validBypass.add(bypass);
	}

	public synchronized void addBypass2(String bypass)
	{
		if(bypass == null)
		{
			return;
		}
		_validBypass2.add(bypass);
	}

	public synchronized boolean validateBypass(String cmd)
	{
		if(!Config.BYPASS_VALIDATION)
		{
			return true;
		}

		for(String bp : _validBypass)
		{
			if(bp == null)
			{
				continue;
			}

			if(bp.equals(cmd))
			{
				return true;
			}
		}

		for(String bp : _validBypass2)
		{
			if(bp == null)
			{
				continue;
			}

			if(cmd.startsWith(bp))
			{
				return true;
			}
		}

		if(cmd.startsWith("npc_") && cmd.endsWith("_SevenSigns 7"))
		{
			return true;
		}

		_log.warn("[L2PcInstance] player [" + getName() + "] sent invalid bypass '" + cmd + "', ban this player!");
		return false;
	}

	public boolean validateItemManipulation(int objectId, String action)
	{
		L2ItemInstance item = getInventory().getItemByObjectId(objectId);

		if(item == null || item.getOwnerId() != getObjectId())
		{
			_log.info(getObjectId() + ": player tried to " + action + " item he is not owner of");
			return false;
		}

		if(getPet() != null && getPet().getControlItemId() == objectId || getMountObjectID() == objectId)
		{
			return false;
		}

		if(getActiveEnchantItem() != null && getActiveEnchantItem().getObjectId() == objectId)
		{
			return false;
		}

		if(CursedWeaponsManager.getInstance().isCursed(item.getItemId()))
		{
			return false;
		}

		if(item.isWear())
		{
			return false;
		}

		item = null;

		return true;
	}

	public synchronized void clearBypass()
	{
		_validBypass.clear();
		_validBypass2.clear();
	}

	public synchronized boolean validateLink(String cmd)
	{
		if(!Config.BYPASS_VALIDATION)
		{
			return true;
		}

		for(String bp : _validLink)
		{
			if(bp == null)
			{
				continue;
			}

			if(bp.equals(cmd))
			{
				return true;
			}
		}
		_log.warn("[L2PcInstance] player ["+getName()+"] sent invalid link '"+cmd+"', ban this player!");
			return false;
	}

	public synchronized void clearLinks()
	{
		_validLink.clear();
	}

	public synchronized void addLink(String link)
	{
		if(link == null) return;
		{
			_validLink.add(link);
		}
	}

	public boolean isInBoat()
	{
		return _inBoat;
	}

	public void setInBoat(boolean inBoat)
	{
		_inBoat = inBoat;
	}

	public L2BoatInstance getBoat()
	{
		return _boat;
	}

	public void setBoat(L2BoatInstance boat)
	{
		_boat = boat;
	}

	public void setInCrystallize(boolean inCrystallize)
	{
		_inCrystallize = inCrystallize;
	}

	public boolean isInCrystallize()
	{
		return _inCrystallize;
	}

	public Point3D getInBoatPosition()
	{
		return _inBoatPosition;
	}

	public void setInBoatPosition(Point3D pt)
	{
		_inBoatPosition = pt;
	}

	public void deleteMe()
	{
		if(inObserverMode())
		{
			setXYZ(_obsX, _obsY, _obsZ);
		}

		Castle castle = null;
		if(getClan() != null)
		{
			castle = CastleManager.getInstance().getCastleByOwner(getClan());
			if(castle != null)
			{
				castle.destroyClanGate();
			}
		}

		try
		{
			setOnlineStatus(false);
		}
		catch(Throwable t)
		{
			_log.error("", t);
		}

		try
		{
			PartyMatchWaitingList.getInstance().removePlayer(this);

			if(_partyroom != 0)
			{
				PartyMatchRoom room = PartyMatchRoomList.getInstance().getRoom(_partyroom);

				if(room != null)
				{
					room.deleteMember(this);
				}
			}
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
		try
		{
			stopAllTimers();
		}
		catch(Throwable t)
		{
			_log.error("", t);
		}

		try
		{
			RecipeController.getInstance().requestMakeItemAbort(this);
		}
		catch(Throwable t)
		{
			_log.error("", t);
		}

		try
		{
			abortAttack();
			abortCast();
			setTarget(null);
		}
		catch(Throwable t)
		{
			_log.error("", t);
		}

		if(getWorldRegion() != null)
		{
			getWorldRegion().removeFromZones(this);
		}

		try
		{
			if(_forceBuff != null)
			{
				abortCast();
			}

			for(L2Character character : getKnownList().getKnownCharacters())
			{
				if(character.getForceBuff() != null && character.getForceBuff().getTarget() == this)
				{
					character.abortCast();
				}
			}
		}
		catch(Throwable t)
		{
			_log.error("", t);
		}

		if(isVisible())
		{
			try
			{
				decayMe();
			}
			catch(Throwable t)
			{
				_log.error("", t);
			}
		}

		if(isInParty())
		{
			try
			{
				leaveParty();
			}
			catch(Throwable t)
			{
				_log.error("", t);
			}
		}

		if(getPet() != null)
		{
			try
			{
				getPet().unSummon(this);
			}
			catch(Throwable t)
			{
				_log.error("", t);
			}
		}

		if(getClanId() != 0 && getClan() != null)
		{
			try
			{
				L2ClanMember clanMember = getClan().getClanMember(getName());
				if(clanMember != null)
				{
					clanMember.setPlayerInstance(null);
				}
				clanMember = null;
			}
			catch(Throwable t)
			{
				_log.error("", t);
			}
		}

		if(getActiveRequester() != null)
		{
			setActiveRequester(null);
		}

		if(getOlympiadGameId() != -1)
		{
			Olympiad.getInstance().removeDisconnectedCompetitor(this);
		}

		if(isGM())
		{
			try
			{
				GmListTable.getInstance().deleteGm(this);
			}
			catch(Throwable t)
			{
				_log.error("", t);
			}
		}

		try
		{
			getInventory().deleteMe();
		}
		catch(Throwable t)
		{
			_log.error("", t);
		}

		try
		{
			clearWarehouse();
		}
		catch(Throwable t)
		{
			_log.error("", t);
		}

		try
		{
			getFreight().deleteMe();
		}
		catch(Throwable t)
		{
			_log.error("", t);
		}

		try
		{
			getKnownList().removeAllKnownObjects();
		}
		catch(Throwable t)
		{
			_log.error("", t);
		}

		closeNetConnection();

		if(getClanId() > 0)
		{
			getClan().broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(this), this);
		}

		for(L2PcInstance player : _snoopedPlayer)
		{
			player.removeSnooper(this);
		}

		for(L2PcInstance player : _snoopListener)
		{
			player.removeSnooped(this);
		}

		if(_chanceSkills != null)
		{
			_chanceSkills.setOwner(null);
			_chanceSkills = null;
		}

		notifyFriends(this);

		L2World.getInstance().removeObject(this);

		try
		{
			setIsTeleporting(false);
			L2World.getInstance().removeFromAllPlayers(this);
		}
		catch(RuntimeException e)
		{
			_log.error("", e);
		}
	}

	public boolean canLogout()
	{
		if (isFlying())
		{
			sendMessage("You cannot leave the game.");
			return false;
		}

		L2Summon summon = getPet();

		if (summon != null && summon instanceof L2PetInstance && !summon.isBetrayed() && summon.isAttackingNow())
		{
			sendPacket(SystemMessageId.PET_CANNOT_SENT_BACK_DURING_BATTLE);
			return false;
		}
		if (isInOlympiadMode() || Olympiad.getInstance().isRegistered(this) || getOlympiadGameId() != -1)
		{
			sendMessage("You cannot leave the game.");
			return false;
		}
		if (isFestivalParticipant())
		{
			sendMessage("You cannot leave the game.");
			return false;
		}

		if (getPrivateStoreType() != 0)
		{
			sendMessage("You cannot leave the game.");
			return false;
		}

		if (getActiveEnchantItem() != null)
		{
			sendMessage("You cannot leave the game.");
			return false;
		}

		return true;
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

			while(rset.next())
			{
				String friendName = rset.getString("friend_name");

				L2PcInstance friend = L2World.getInstance().getPlayer(friendName);

				if(friend != null)
				{
					friend.sendPacket(new FriendList(friend));
					friend.sendMessage("Friend: " + cha.getName() + " has logged off.");
				}
			}

			rset.close();
			statement.close();
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

	private FishData _fish;

	public void startFishing(int _x, int _y, int _z)
	{
		stopMove(null);
		setIsImobilised(true);
		_fishing = true;
		_fishx = _x;
		_fishy = _y;
		_fishz = _z;
		broadcastUserInfo();
		int lvl = GetRandomFishLvl();
		int group = GetRandomGroup();
		int type = GetRandomFishType(group);
		List<FishData> fishs = FishTable.getInstance().getfish(lvl, type, group);
		if(fishs == null || fishs.size() == 0)
		{
			sendMessage("Error - Fishes are not definied");
			EndFishing(false);
			return;
		}
		int check = Rnd.get(fishs.size());
		_fish = new FishData(fishs.get(check));
		fishs.clear();
		fishs = null;
		sendPacket(new SystemMessage(SystemMessageId.CAST_LINE_AND_START_FISHING));
		ExFishingStart efs = null;

		if(!GameTimeController.getInstance().isNowNight() && _lure.isNightLure())
		{
			_fish.setType(-1);
		}

		efs = new ExFishingStart(this, _fish.getType(), _x, _y, _z, _lure.isNightLure());
		broadcastPacket(efs);
		efs = null;
		StartLookingForFishTask();
	}

	public void stopLookingForFishTask()
	{
		if(_taskforfish != null)
		{
			_taskforfish.cancel(false);
			_taskforfish = null;
		}
	}

	public void StartLookingForFishTask()
	{
		if(!isDead() && _taskforfish == null)
		{
			int checkDelay = 0;
			boolean isNoob = false;
			boolean isUpperGrade = false;

			if(_lure != null)
			{
				int lureid = _lure.getItemId();
				isNoob = _fish.getGroup() == 0;
				isUpperGrade = _fish.getGroup() == 2;
				if(lureid == 6519 || lureid == 6522 || lureid == 6525 || lureid == 8505 || lureid == 8508 || lureid == 8511)
				{
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * 1.33));
				}
				else if(lureid == 6520 || lureid == 6523 || lureid == 6526 || lureid >= 8505 && lureid <= 8513 || lureid >= 7610 && lureid <= 7613 || lureid >= 7807 && lureid <= 7809 || lureid >= 8484 && lureid <= 8486)
				{
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * 1.00));
				}
				else if(lureid == 6521 || lureid == 6524 || lureid == 6527 || lureid == 8507 || lureid == 8510 || lureid == 8513)
				{
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * 0.66));
				}
			}
			_taskforfish = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new LookingForFishTask(_fish.getWaitTime(), _fish.getFishGuts(), _fish.getType(), isNoob, isUpperGrade), 10000, checkDelay);
		}
	}

	private int GetRandomGroup()
	{
		switch(_lure.getItemId())
		{
			case 7807:
			case 7808:
			case 7809:
			case 8486:
				return 0;
			case 8485:
			case 8506:
			case 8509:
			case 8512:
				return 2;
			default:
				return 1;
		}
	}

	private int GetRandomFishType(int group)
	{
		int check = Rnd.get(100);
		int type = 1;
		switch(group)
		{
			case 0:
				switch(_lure.getItemId())
				{
					case 7807:
						if(check <= 54)
						{
							type = 5;
						}
						else if(check <= 77)
						{
							type = 4;
						}
						else
						{
							type = 6;
						}
						break;
					case 7808:
						if(check <= 54)
						{
							type = 4;
						}
						else if(check <= 77)
						{
							type = 6;
						}
						else
						{
							type = 5;
						}
						break;
					case 7809:
						if(check <= 54)
						{
							type = 6;
						}
						else if(check <= 77)
						{
							type = 5;
						}
						else
						{
							type = 4;
						}
						break;
					case 8486:
						if(check <= 33)
						{
							type = 4;
						}
						else if(check <= 66)
						{
							type = 5;
						}
						else
						{
							type = 6;
						}
						break;
				}
				break;
			case 1:
				switch(_lure.getItemId())
				{
					case 7610:
					case 7611:
					case 7612:
					case 7613:
						type = 3;
						break;
					case 6519:
					case 8505:
					case 6520:
					case 6521:
					case 8507:
						if(check <= 54)
						{
							type = 1;
						}
						else if(check <= 74)
						{
							type = 0;
						}
						else if(check <= 94)
						{
							type = 2;
						}
						else
						{
							type = 3;
						}
						break;
					case 6522:
					case 8508:
					case 6523:
					case 6524:
					case 8510:
						if(check <= 54)
						{
							type = 0;
						}
						else if(check <= 74)
						{
							type = 1;
						}
						else if(check <= 94)
						{
							type = 2;
						}
						else
						{
							type = 3;
						}
						break;
					case 6525:
					case 8511:
					case 6526:
					case 6527:
					case 8513:
						if(check <= 55)
						{
							type = 2;
						}
						else if(check <= 74)
						{
							type = 1;
						}
						else if(check <= 94)
						{
							type = 0;
						}
						else
						{
							type = 3;
						}
						break;
					case 8484:
						if(check <= 33)
						{
							type = 0;
						}
						else if(check <= 66)
						{
							type = 1;
						}
						else
						{
							type = 2;
						}
						break;
				}
				break;
			case 2:
				switch(_lure.getItemId())
				{
					case 8506:
						if(check <= 54)
						{
							type = 8;
						}
						else if(check <= 77)
						{
							type = 7;
						}
						else
						{
							type = 9;
						}
						break;
					case 8509:
						if(check <= 54)
						{
							type = 7;
						}
						else if(check <= 77)
						{
							type = 9;
						}
						else
						{
							type = 8;
						}
						break;
					case 8512:
						if(check <= 54)
						{
							type = 9;
						}
						else if(check <= 77)
						{
							type = 8;
						}
						else
						{
							type = 7;
						}
						break;
					case 8485:
						if(check <= 33)
						{
							type = 7;
						}
						else if(check <= 66)
						{
							type = 8;
						}
						else
						{
							type = 9;
						}
						break;
				}
		}
		return type;
	}

	private int GetRandomFishLvl()
	{
		L2Effect[] effects = getAllEffects();
		int skilllvl = getSkillLevel(1315);
		for(L2Effect e : effects)
		{
			if(e.getSkill().getId() == 2274)
			{
				skilllvl = (int) e.getSkill().getPower(this);
			}
		}
		if(skilllvl <= 0)
		{
			return 1;
		}
		int randomlvl;
		int check = Rnd.get(100);

		if(check <= 50)
		{
			randomlvl = skilllvl;
		}
		else if(check <= 85)
		{
			randomlvl = skilllvl - 1;
			if(randomlvl <= 0)
			{
				randomlvl = 1;
			}
		}
		else
		{
			randomlvl = skilllvl + 1;
			if(randomlvl > 27)
			{
				randomlvl = 27;
			}
		}
		effects = null;

		return randomlvl;
	}

	public void StartFishCombat(boolean isNoob, boolean isUpperGrade)
	{
		_fishCombat = new L2Fishing(this, _fish, isNoob, isUpperGrade);
	}

	public void EndFishing(boolean win)
	{
		ExFishingEnd efe = new ExFishingEnd(win, this);
		broadcastPacket(efe);
		efe = null;
		_fishing = false;
		_fishx = 0;
		_fishy = 0;
		_fishz = 0;
		broadcastUserInfo();

		if(_fishCombat == null)
		{
			sendPacket(new SystemMessage(SystemMessageId.BAIT_LOST_FISH_GOT_AWAY));
		}

		_fishCombat = null;
		_lure = null;
		sendPacket(new SystemMessage(SystemMessageId.REEL_LINE_AND_STOP_FISHING));
		setIsImobilised(false);
		stopLookingForFishTask();
	}

	public L2Fishing GetFishCombat()
	{
		return _fishCombat;
	}

	public int GetFishx()
	{
		return _fishx;
	}

	public int GetFishy()
	{
		return _fishy;
	}

	public int GetFishz()
	{
		return _fishz;
	}

	public void SetLure(L2ItemInstance lure)
	{
		_lure = lure;
	}

	public L2ItemInstance GetLure()
	{
		return _lure;
	}

	public int GetInventoryLimit()
	{
		int ivlim;
		if(isGM())
		{
			ivlim = Config.INVENTORY_MAXIMUM_GM;
		}
		else if(getRace() == Race.dwarf)
		{
			ivlim = Config.INVENTORY_MAXIMUM_DWARF;
		}
		else
		{
			ivlim = Config.INVENTORY_MAXIMUM_NO_DWARF;
		}
		ivlim += (int) getStat().calcStat(Stats.INV_LIM, 0, null, null);

		return ivlim;
	}

	public int GetWareHouseLimit()
	{
		int whlim;
		if(getRace() == Race.dwarf)
		{
			whlim = Config.WAREHOUSE_SLOTS_DWARF;
		}
		else
		{
			whlim = Config.WAREHOUSE_SLOTS_NO_DWARF;
		}
		whlim += (int) getStat().calcStat(Stats.WH_LIM, 0, null, null);

		return whlim;
	}

	public int GetPrivateSellStoreLimit()
	{
		int pslim;
		if(getRace() == Race.dwarf)
		{
			pslim = Config.MAX_PVTSTORE_SLOTS_DWARF;
		}

		else
		{
			pslim = Config.MAX_PVTSTORE_SLOTS_OTHER;
		}
		pslim += (int) getStat().calcStat(Stats.P_SELL_LIM, 0, null, null);

		return pslim;
	}

	public int GetPrivateBuyStoreLimit()
	{
		int pblim;
		if(getRace() == Race.dwarf)
		{
			pblim = Config.MAX_PVTSTORE_SLOTS_DWARF;
		}
		else
		{
			pblim = Config.MAX_PVTSTORE_SLOTS_OTHER;
		}
		pblim += (int) getStat().calcStat(Stats.P_BUY_LIM, 0, null, null);

		return pblim;
	}

	public int GetFreightLimit()
	{
		return Config.FREIGHT_SLOTS + (int) getStat().calcStat(Stats.FREIGHT_LIM, 0, null, null);
	}

	public int GetDwarfRecipeLimit()
	{
		int recdlim = Config.DWARF_RECIPE_LIMIT;
		recdlim += (int) getStat().calcStat(Stats.REC_D_LIM, 0, null, null);
		return recdlim;
	}

	public int GetCommonRecipeLimit()
	{
		int recclim = Config.COMMON_RECIPE_LIMIT;
		recclim += (int) getStat().calcStat(Stats.REC_C_LIM, 0, null, null);
		return recclim;
	}

	public void setMountObjectID(int newID)
	{
		_mountObjectID = newID;
	}

	public int getMountObjectID()
	{
		return _mountObjectID;
	}

	private L2ItemInstance _lure = null;

	public SkillDat getCurrentSkill()
	{
		return _currentSkill;
	}

	public void setCurrentSkill(L2Skill currentSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if(currentSkill == null)
		{
			_currentSkill = null;
			return;
		}

		_currentSkill = new SkillDat(currentSkill, ctrlPressed, shiftPressed);
	}

	/** 
	 * Get the current pet skill in use or return null.<BR><BR> 
	 *  
	 */ 
	public SkillDat getCurrentPetSkill() 
	{ 
		return _currentPetSkill; 
    } 
    /** 
     * Create a new SkillDat object and set the player _currentPetSkill.<BR><BR> 
     *  
     */ 
	public void setCurrentPetSkill(L2Skill currentSkill, boolean ctrlPressed, boolean shiftPressed) 
	{ 
		if (currentSkill == null) 
		{ 
            if (_log.isDebugEnabled()) 
                _log.info("Setting current skill: NULL for " + getName() + "."); 
 
            _currentPetSkill = null; 
            return; 
        } 
 
        if (_log.isDebugEnabled()) 
            _log.info("Setting current skill: " + currentSkill.getName() + " (ID: " + currentSkill.getId() + ") for " + getName() + "."); 
 
        _currentPetSkill = new SkillDat(currentSkill, ctrlPressed, shiftPressed); 
	}
 	
	public SkillDat getQueuedSkill()
	{
		return _queuedSkill;
	}

	public void setQueuedSkill(L2Skill queuedSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if(queuedSkill == null)
		{
			_queuedSkill = null;
			return;
		}

		_queuedSkill = new SkillDat(queuedSkill, ctrlPressed, shiftPressed);
	}

	/*public void setInJail(boolean state)
	{
		_inJail = state;
	}

	public void setInJail(boolean state, int delayInMinutes)
	{
		_inJail = state;
		_jailTimer = 0;
		stopJailTask(false);

		if(_inJail)
		{
			if(delayInMinutes > 0)
			{
				_jailTimer = delayInMinutes * 60000L;

				_jailTask = ThreadPoolManager.getInstance().scheduleGeneral(new JailTask(this), _jailTimer);
				sendMessage("You are in jail for " + delayInMinutes + " minutes.");
			}

			NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
			String jailInfos = HtmCache.getInstance().getHtm("data/html/jail_in.htm");
			if(jailInfos != null)
			{
				htmlMsg.setHtml(jailInfos);
			}
			else
			{
				htmlMsg.setHtml("<html><body>You have been put in jail by an admin.</body></html>");
			}
			sendPacket(htmlMsg);
			htmlMsg = null;

			teleToLocation(-114356, -249645, -2984, true);
		}
		else
		{
			NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
			String jailInfos = HtmCache.getInstance().getHtm("data/html/jail_out.htm");
			if(jailInfos != null)
			{
				htmlMsg.setHtml(jailInfos);
			}
			else
			{
				htmlMsg.setHtml("<html><body>You are free for now, respect server rules!</body></html>");
			}
			sendPacket(htmlMsg);
			htmlMsg = null;

			teleToLocation(17836, 170178, -3507, true);
		}

		storeCharBase();
	}

	public long getJailTimer()
	{
		return _jailTimer;
	}

	public void setJailTimer(long time)
	{
		_jailTimer = time;
	}

	private void updateJailState()
	{
		if(isInJail())
		{
			if(_jailTimer > 0)
			{
				_jailTask = ThreadPoolManager.getInstance().scheduleGeneral(new JailTask(this), _jailTimer);
				sendMessage("You are still in jail for " + Math.round(_jailTimer / 60000) + " minutes.");
			}

			if(!isInsideZone(ZONE_JAIL))
			{
				teleToLocation(-114356, -249645, -2984, true);
			}
		}
	}

	public void stopJailTask(boolean save)
	{
		if(_jailTask != null)
		{
			if(save)
			{
				long delay = _jailTask.getDelay(TimeUnit.MILLISECONDS);
				if(delay < 0)
				{
					delay = 0;
				}
				setJailTimer(delay);
			}
			_jailTask.cancel(false);
			_jailTask = null;
		}
	}

	private class JailTask implements Runnable
	{
		L2PcInstance _player;


		protected JailTask(L2PcInstance player)
		{
			_player = player;
		}

		@Override
		public void run()
		{
			_player.setInJail(false, 0);
		}
	}*/

	public int getPowerGrade()
	{
		return _powerGrade;
	}

	public void setPowerGrade(int power)
	{
		_powerGrade = power;
	}

	public boolean isCursedWeaponEquiped()
	{
		return _cursedWeaponEquipedId != 0;
	}

	public void setCursedWeaponEquipedId(int value)
	{
		_cursedWeaponEquipedId = value;
	}

	public int getCursedWeaponEquipedId()
	{
		return _cursedWeaponEquipedId;
	}

	private boolean _charmOfCourage = false;

	public boolean getCharmOfCourage()
	{
		return _charmOfCourage;
	}

	public void setCharmOfCourage(boolean val)
	{
		_charmOfCourage = val;
		sendPacket(new EtcStatusUpdate(this));
	}

	public int getDeathPenaltyBuffLevel()
	{
		return _deathPenaltyBuffLevel;
	}

	public void setDeathPenaltyBuffLevel(int level)
	{
		_deathPenaltyBuffLevel = level;
	}

	public void calculateDeathPenaltyBuffLevel(L2Character killer)
	{
		if((getKarma() > 0 || Rnd.get(1,100) <= Config.DEATH_PENALTY_CHANCE) && !(killer instanceof L2PcInstance) && !isGM() && !(getCharmOfLuck() && !isPhoenixBlessed() && (killer instanceof L2GrandBossInstance || killer instanceof L2RaidBossInstance)) && !(isInsideZone(L2Character.ZONE_PVP) || isInsideZone(L2Character.ZONE_SIEGE)))
		{
			increaseDeathPenaltyBuffLevel();
		}
	}

	public void increaseDeathPenaltyBuffLevel()
	{
		if(getDeathPenaltyBuffLevel() >= 15)
		{
			return;
		}

		if(getDeathPenaltyBuffLevel() != 0)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());

			if(skill != null)
			{
				removeSkill(skill, true);
				skill = null;
			}
		}

		_deathPenaltyBuffLevel++;

		addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
		sendPacket(new EtcStatusUpdate(this));
		SystemMessage sm = new SystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED);
		sm.addNumber(getDeathPenaltyBuffLevel());
		sendPacket(sm);
		sm = null;
		sendSkillList();
	}

	public void reduceDeathPenaltyBuffLevel()
	{
		if(getDeathPenaltyBuffLevel() <= 0)
		{
			return;
		}

		L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());

		if(skill != null)
		{
			removeSkill(skill, true);
			skill = null;
			sendSkillList();
		}

		_deathPenaltyBuffLevel--;

		if(getDeathPenaltyBuffLevel() > 0)
		{
			addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
			sendPacket(new EtcStatusUpdate(this));
			sendPacket(new SystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED).addNumber(getDeathPenaltyBuffLevel()));
		}
		else
		{
			sendPacket(new EtcStatusUpdate(this));
			sendPacket(new SystemMessage(SystemMessageId.DEATH_PENALTY_LIFTED));
		}
	}

	public void restoreCustomStatus()
	{
		Connection con = null;

		try
		{

			int hero = 0;
			int noble = 0;
			long hero_end = 0;

			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(STATUS_DATA_GET);
			statement.setInt(1, getObjectId());

			ResultSet rset = statement.executeQuery();

			while(rset.next())
			{
				hero = rset.getInt("hero");
				noble = rset.getInt("noble");
				hero_end = rset.getLong("hero_end_date");
			}
			rset.close();
			statement.close();
			statement = null;
			rset = null;

			if(hero > 0 && (hero_end == 0 || hero_end > System.currentTimeMillis()))
			{
				setIsHero(true);
			}
			else
			{
				destroyItem("HeroEnd", 6842, 1, null, false);
			}

			if(noble > 0)
			{
				setNoble(true);
			}
		}
		catch(Exception e)
		{
			_log.warn("Could not restore char custom data info: " + e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public void restoreDeathPenaltyBuffLevel()
	{
		L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());

		if(skill != null)
		{
			removeSkill(skill, true);
			skill = null;
		}

		if(getDeathPenaltyBuffLevel() > 0)
		{
			addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
			sendPacket(new SystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED).addNumber(getDeathPenaltyBuffLevel()));
		}
		sendPacket(new EtcStatusUpdate(this));
	}

	private final FastMap<Integer, TimeStamp> _reuseTimeStamps = new FastMap<Integer, TimeStamp>().shared();
	
	public Collection<TimeStamp> getReuseTimeStamps()
	{
		return _reuseTimeStamps.values();
	}
	
	public FastMap<Integer, TimeStamp> getReuseTimeStamp()
	{
		return _reuseTimeStamps;
	}
	
	public class TimeStamp
	{
		private int skill;
		private long reuse;
		private long stamp;

		public TimeStamp(int _skill, long _reuse)
		{
			skill = _skill;
			reuse = _reuse;
			stamp = System.currentTimeMillis() + reuse;
		}

		public TimeStamp(int _skill, long _reuse, long _systime)
		{
			skill = _skill;
			reuse = _reuse;
			stamp = _systime;
		}
		
		public int getSkill()
		{
			return skill;
		}

		public long getStamp()
		{
			return stamp;
		}
		
		public long getReuse()
		{
			return reuse;
		}
		
		public long getRemaining()
		{
			return Math.max(stamp - System.currentTimeMillis(), 0);
		}

		public boolean hasNotPassed()
		{
			return System.currentTimeMillis() < stamp;
		}
	}

	@Override
	public void addTimeStamp(int s, int r)
	{
		_reuseTimeStamps.put(s, new TimeStamp(s, r));
	}

	private void addTimeStamp(TimeStamp T)
	{
		_reuseTimeStamps.put(T.getSkill(), T);
	}

	@Override
	public void removeTimeStamp(int s)
	{
		_reuseTimeStamps.remove(s);
	}

	public boolean isInDangerArea()
	{
		return isInDangerArea;
	}

	public void enterDangerArea()
	{
		addSkill(SkillTable.getInstance().getInfo(4268, 1), false);
		isInDangerArea = true;
		sendPacket(new EtcStatusUpdate(this));
		sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("You have entered a danger area."));
	}

	public void exitDangerArea()
	{
		removeSkill(SkillTable.getInstance().getInfo(4268, 1), true);
		isInDangerArea = false;
		sendPacket(new EtcStatusUpdate(this));
		sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("You have left a danger area."));
	}

	@Override
	public final void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
		if(miss)
		{
			sendPacket(new SystemMessage(SystemMessageId.MISSED_TARGET));
			return;
		}

		if(pcrit)
		{
			sendPacket(new SystemMessage(SystemMessageId.CRITICAL_HIT));
			if(Config.SCREEN_CRITICAL)
			{
				sendPacket(new ExShowScreenMessage("Critical Hit! "+damage, 3000));
			}
		}

		if(mcrit)
		{
			sendPacket(new SystemMessage(SystemMessageId.CRITICAL_HIT_MAGIC));
			if(Config.SCREEN_CRITICAL)
			{
				sendPacket(new ExShowScreenMessage("Critical Magic! "+damage, 3000));
			}
		}

		if(isInOlympiadMode() && target instanceof L2PcInstance && ((L2PcInstance) target).isInOlympiadMode() && ((L2PcInstance) target).getOlympiadGameId() == getOlympiadGameId())
		{
			dmgDealt += damage;
		}

		if (target.isInvul() && !(target instanceof L2NpcInstance))
		{
			sendPacket(new SystemMessage(SystemMessageId.ATTACK_WAS_BLOCKED));
		}
		else if (target instanceof L2DoorInstance || target instanceof L2ControlTowerInstance) 
		{ 
			sendPacket(new SystemMessage(SystemMessageId.YOU_DID_S1_DMG).addNumber(damage));
		}
		else
		{
			sendPacket(new SystemMessage(SystemMessageId.YOU_DID_S1_DMG).addNumber(damage));
		}
	}

	public boolean isRequestExpired()
	{
		return !(_requestExpireTime > GameTimeController.getGameTicks());
	}

	boolean _gmStatus = true;

	public void setGmStatusActive(boolean state)
	{
		_gmStatus = state;
	}

	public boolean hasGmStatusActive()
	{
		return _gmStatus;
	}

	/*
	public void setChatBanTimer(long time)
	{
		_chatBanTimer = time;
	}

	private void updateChatBanState()
	{
		if(_chatBanTimer > 0L)
		{
			_chatBanned = true;
			_chatBanTask = ThreadPoolManager.getInstance().scheduleGeneral(new ChatBanTask(this), _chatBanTimer);
			sendPacket(new EtcStatusUpdate(this));
		}
	}

	public void stopChatBanTask(boolean save)
	{
		if(_chatBanTask != null)
		{
			if(save)
			{
				long delay = _chatBanTask.getDelay(TimeUnit.MILLISECONDS);
				if(delay < 0L)
				{
					delay = 0L;
				}
				setChatBanTimer(delay);
			}
			_chatBanTask.cancel(false);
			_chatBanned = false;
			_chatBanTask = null;
			sendPacket(new EtcStatusUpdate(this));
		}
	}

	public void setChatBanned(boolean state, long delayInSec)
	{
		_chatBanned = state;
		_chatBanTimer = 0L;
		stopChatBanTask(false);
		if(_chatBanned && delayInSec > 0)
		{
			if(!isGM())
			{
				_chatBanTimer = delayInSec;
				_chatBanTask = ThreadPoolManager.getInstance().scheduleGeneral(new ChatBanTask(this), _chatBanTimer);
				sendMessage("You are chat banned for " + (_chatBanTimer / 60 / 1000) + " minutes.");
				sendPacket(new EtcStatusUpdate(this));
			}
		}

		storeCharBase();
	}

	public long getChatBanTimer()
	{
		if(_chatBanned && _chatBanTask!= null)
		{
			long delay = _chatBanTask.getDelay(TimeUnit.MILLISECONDS);
			if(delay >= 0L)
			{
				_chatBanTimer = delay;
			}
		}

		return _chatBanTimer;
	}

	private class ChatBanTask implements Runnable
	{
		L2PcInstance _player;


		protected ChatBanTask(L2PcInstance player)
		{
			_player = player;
		}

		@Override
		public void run()
		{
			_player.setChatBanned(false, 0);
		}
	}
*/
	public L2Object _saymode = null;

	public L2Object getSayMode()
	{
		return _saymode;
	}

	public void setSayMode(L2Object say)
	{
		_saymode = say;
	}

	public void saveEventStats()
	{
		_originalNameColor = getAppearance().getNameColor();
		_originalKarma = getKarma();
		_eventKills = 0;
	}

	public void restoreEventStats()
	{
		getAppearance().setNameColor(_originalNameColor);
		setKarma(_originalKarma);
		_eventKills = 0;
	}

	public Point3D getCurrentSkillWorldPosition()
	{
		return _currentSkillWorldPosition;
	}

	public void setCurrentSkillWorldPosition(final Point3D worldPosition)
	{
		_currentSkillWorldPosition = worldPosition;
	}

	public boolean isCursedWeaponEquipped()
	{
		return _cursedWeaponEquipedId != 0;
	}

	public boolean dismount()
	{
		if(setMountType(0))
		{
			if(isFlying())
			{
				removeSkill(SkillTable.getInstance().getInfo(4289, 1));
			}

			Ride dismount = new Ride(getObjectId(), Ride.ACTION_DISMOUNT, 0);
			broadcastPacket(dismount);
			dismount = null;
			setMountObjectID(0);

			broadcastUserInfo();
			return true;
		}

		return false;
	}

	private String StringToHex(String color)
	{
		switch(color.length())
		{
			case 1:
				color = new StringBuilder().append("00000").append(color).toString();
				break;

			case 2:
				color = new StringBuilder().append("0000").append(color).toString();
				break;

			case 3:
				color = new StringBuilder().append("000").append(color).toString();
				break;

			case 4:
				color = new StringBuilder().append("00").append(color).toString();
				break;

			case 5:
				color = new StringBuilder().append("0").append(color).toString();
				break;
		}
		return color;
	}

	public boolean isOffline()
	{
		return _isOffline;
	}

	public void setOffline(boolean set)
	{
		_isOffline = set;
	}

	public boolean isTradeDisabled()
	{
		return _isTradeOff || isCastingNow();
	}

	public void setTradeDisabled(boolean set)
	{
		_isTradeOff = set;
	}

	public void showTeleportHtml()
	{
		TextBuilder text = new TextBuilder();
		text.append("<html>");
		text.append("<title>RaidBoss Manager</title>");
		text.append("<body>");
		text.append("<center>");
		text.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32>");
		text.append("<br><br>");
		text.append("<table width=\"85%\"><tr><td>Your party leader, "+getParty().getLeader().getName()+", requested a group teleport to raidboss. You have 30 seconds from this popup to teleport, or the teleport windows will close</td></tr></table><br>");
		text.append("<a action=\"bypass -h rbAnswear\">Port with my party</a><br>");
		text.append("<a action=\"bypass -h rbAnswearDenied\">Don't port</a><br1>");
		text.append("<center><img src=\"L2UI.SquareGray\" width=\"280\" height=\"1\">");
		text.append("</body>");
		text.append("</html>");

		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setHtml(text.toString());
		sendPacket(html);
	}

	public void showRaidbossInfoLevel40()
	{
		TextBuilder text = new TextBuilder();
		text.append("<html>");
		text.append("<body>");
		text.append("<title>Raidboss Level (40-45)</title>");
		text.append("<center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center>");
		text.append("<br><br>");
		text.append("<center><font color=\"00FF00\">Leto Chief Talkin (40)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Water Spirit Lian (40)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Shaman King Selu (40)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Gwindorr (40)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Icarus Sample 1 (40)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Fafurion's Page Sika (40)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Nakondas (40)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Road Scavenger Leader (40)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Wizard of Storm Teruk (40)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Water Couatle Ateka (40)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Crazy Mechanic Golem (43)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Earth Protector Panathen (43)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Thief Kelbar (44)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Timak Orc Chief Ranger (44)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Rotten Tree Repiro (44)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Dread Avenger Kraven (44)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Biconne of Blue Sky (45)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Evil Spirit Cyrion (45)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Iron Giant Totem (45)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Timak Orc Gosmos (45)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Shacram (45)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Fafurion's Henchman Istary (45)</font></center><br1>");
		text.append("<center><img src=\"L2UI.SquareGray\" width=\"280\" height=\"1\">");
		text.append("</body>");
		text.append("</html>");
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setHtml(text.toString());
		sendPacket(html);
	}

	public void showRaidbossInfoLevel45()
	{
		TextBuilder text = new TextBuilder();
		text.append("<html>");
		text.append("<body>");
		text.append("<title>Raidboss Level (45-50)</title>");
		text.append("<center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center>");
		text.append("<br><br>");
		text.append("<center><font color=\"00FF00\">Necrosentinel Royal Guard (47)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Barion (47)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Orfen's Handmaiden (48)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">King Tarlk (48)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Katu Van Leader Atui (49)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Mirror of Oblivion (49)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Karte (49)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Ghost of Peasant Leader (50)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Cursed Clara (50)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Carnage Lord Gato (50)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Fafurion's Henchman Istary (45)</font></center><br1>");
		text.append("<center><img src=\"L2UI.SquareGray\" width=\"280\" height=\"1\">");
		text.append("</body>");
		text.append("</html>");
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setHtml(text.toString());
		sendPacket(html);
	}

	public void showRaidbossInfoLevel50()
	{
		TextBuilder text = new TextBuilder();
		text.append("<html>");
		text.append("<body>");
		text.append("<title>Raidboss Level (50-55)</title>");
		text.append("<center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center>");
		text.append("<br><br>");
		text.append("<center><font color=\"00FF00\">Verfa (51)</font><br1>");
		text.append("<center><font color=\"00FF00\">Deadman Ereve (51)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Captain of Red Flag Shaka (52)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Grave Robber Kim (52)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Paniel the Unicorn (54)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Bandit Leader Barda (55)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Eva's Spirit Niniel (55)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Beleth's Seer Sephia (55)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Pagan Watcher Cerberon (55)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Shaman King Selu (55)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Black Lily (55)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Ghost Knight Kabed (55)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Sorcerer Isirr (55)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Furious Thieles (55)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Enchanted Forest Watcher Ruell (55)</font></center><br1>");
		text.append("<center><img src=\"L2UI.SquareGray\" width=\"280\" height=\"1\">");
		text.append("</body>");
		text.append("</html>");
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setHtml(text.toString());
		sendPacket(html);
	}

	public void showRaidbossInfoLevel55()
	{
		TextBuilder text = new TextBuilder();
		text.append("<html>");
		text.append("<body>");
		text.append("<title>Raidboss Level (55-60)</title>");
		text.append("<center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center>");
		text.append("<br><br>");
		text.append("<center><font color=\"00FF00\">Fairy Queen Timiniel (56)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Harit Guardian Garangky (56)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Refugee Hopeful Leo (56)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Timak Seer Ragoth (57)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Soulless Wild Boar (59)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Abyss Brukunt (59)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Giant Marpanak (60)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Ghost of the Well Lidia (60)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Guardian of the Statue of Giant Karum (60)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">The 3rd Underwater Guardian (60)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Taik High Prefect Arak (60)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Ancient Weird Drake (60)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Lord Ishka (60)</font></center><br1>");
		text.append("<center><img src=\"L2UI.SquareGray\" width=\"280\" height=\"1\">");
		text.append("</body>");
		text.append("</html>");
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setHtml(text.toString());
		sendPacket(html);
	}

	public void showRaidbossInfoLevel60()
	{
		TextBuilder text = new TextBuilder();
		text.append("<html>");
		text.append("<body>");
		text.append("<title>Raidboss Level (60-65)</title>");
		text.append("<center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center>");
		text.append("<br><br>");
		text.append("<center><font color=\"00FF00\">Roaring Lord Kastor (62)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Gorgolos (64)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Hekaton Prime (65)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Gargoyle Lord Tiphon (65)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Fierce Tiger King Angel (65)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Enmity Ghost Ramdal (65)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Rahha (65)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Shilen's Priest Hisilrome (65)</font></center><br1>");
		text.append("<center><img src=\"L2UI.SquareGray\" width=\"280\" height=\"1\">");
		text.append("</body>");
		text.append("</html>");
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setHtml(text.toString());
		sendPacket(html);
	}

	public void showRaidbossInfoLevel65()
	{
		TextBuilder text = new TextBuilder();
		text.append("<html>");
		text.append("<body>");
		text.append("<title>Raidboss Level (65-70)</title>");
		text.append("<center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center>");
		text.append("<br><br>");
		text.append("<center><font color=\"00FF00\">Demon's Agent Falston (66)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Last Titan utenus (66)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Kernon's Faithful Servant Kelone (67)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Spirit of Andras, the Betrayer (69)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Bloody Priest Rudelto (69)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Shilen's Messenger Cabrio (70)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Anakim's Nemesis Zakaron (70)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Flame of Splendor Barakiel (70)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Roaring Skylancer (70)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Beast Lord Behemoth (70)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Palibati Queen Themis (70)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Fafurion''s Herald Lokness (70)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Meanas Anor (70)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Korim (70)</font></center><br1>");
		text.append("<center><img src=\"L2UI.SquareGray\" width=\"280\" height=\"1\">");
		text.append("</body>");
		text.append("</html>");
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setHtml(text.toString());
		sendPacket(html);
	}

	public void showRaidbossInfoLevel70()
	{
		TextBuilder text = new TextBuilder();
		text.append("<html>");
		text.append("<body>");
		text.append("<title>Raidboss Level (70-75)</title>");
		text.append("<center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center>");
		text.append("<br><br>");
		text.append("<center><font color=\"00FF00\">Immortal Savior Mardil (71)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Vanor Chief Kandra (72)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Water Dragon Seer Sheshark (72)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Doom Blade Tanatos (72)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Death Lord Hallate (73)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Plague Golem (73)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Icicle Emperor Bumbalump (74)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Antharas Priest Cloe (74)</font></center><br1>");
		text.append("<center><font color=\"00FF00\">Krokian Padisha Sobekk (74)</font></center><br1>");
		text.append("<center><img src=\"L2UI.SquareGray\" width=\"280\" height=\"1\">");
		text.append("</body>");
		text.append("</html>");
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setHtml(text.toString());
		sendPacket(html);
	}

	private boolean isintwtown = false;

	public boolean isInsideTWTown()
	{
		if(isintwtown)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public void setInsideTWTown(boolean b)
	{
		isintwtown = true;
	}

	public void removeFromBossZone()
	{
		try
		{
			for(L2BossZone _zone : GrandBossManager.getInstance().getZones())
			{
				_zone.removePlayer(this);
			}
		}
		catch(Exception e)
		{
			_log.warn("Exception on removeFromBossZone(): " + e.getMessage(), e);
		}
	}

	public void setIsEnchanting(boolean val)
	{
		_isEnchanting = val;
	}

	public boolean isEnchanting()
	{
		return _isEnchanting;
	}

	public long getOfflineStartTime()
	{
		return _offlineShopStart;
	}

	public void setOfflineStartTime(long time)
	{
		_offlineShopStart = time;
	}

	public boolean canOpenPrivateStore()
	{
		if(Config.SHOP_MIN_RANGE_FROM_NPC > 0 || Config.SHOP_MIN_RANGE_FROM_PLAYER > 0)
		{
			for(L2Character player : getKnownList().getKnownCharacters())
			{
				if(Util.checkIfInRange(player.getMinShopDistance(), this, player, true))
				{
					sendMessage("You cannot open a Private Workshop here.");
					return false;
				}
			}
		}

		return !isAlikeDead() && !isInOlympiadMode() && !isMounted() && !isCastingNow();
	}

	public boolean canAttackCharacter(L2Character cha)
	{
		if (cha instanceof L2Attackable)
			return true;
		else if (cha instanceof L2Playable)
		{
			if (cha.isInsideZone(L2Character.ZONE_PVP) && !cha.isInsideZone(L2Character.ZONE_SIEGE))
				return true;
			
			L2PcInstance target;
			if (cha instanceof L2Summon)
				target = ((L2Summon) cha).getOwner();
			else
				target = (L2PcInstance) cha;
			
			if (isInDuel() && target.isInDuel() && (target.getDuelId() == getDuelId()))
				return true;
			else if (isInParty() && target.isInParty())
			{
				if (getParty() == target.getParty())
					return false;
				
				if ((getParty().getCommandChannel() != null || target.getParty().getCommandChannel() != null) && (getParty().getCommandChannel() == target.getParty().getCommandChannel()))
					return false;
			}
			else if (getClan() != null && target.getClan() != null)
			{
				if (getClanId() == target.getClanId())
					return false;
				
				if ((getAllyId() > 0 || target.getAllyId() > 0) && (getAllyId() == target.getAllyId()))
					return false;
				
				if (getClan().isAtWarWith(target.getClan().getClanId()) && target.getClan().isAtWarWith(getClan().getClanId()))
					return true;
			}
			else if (getClan() == null || target.getClan() == null)
			{
				if (target.getPvpFlag() == 0 && target.getKarma() == 0)
					return false;
			}
		}
		return true;
	}
	
	@Override
	public int getMinShopDistance()
	{
		return (isSitting()) ? Config.SHOP_MIN_RANGE_FROM_PLAYER : 0;
	}
	
	/** Vip System Start */
	public boolean isVip()
	{
		return _isVip;
	}
	
	public void setVip(boolean val)
	{
		_isVip = val;
		
	}
	
	public void setVipEndTime(long val)
	{
		_vip_endTime = val;
	}
	
	public long getVipEndTime()
	{
		return _vip_endTime;
	}

	/** Aio System Start */
	public boolean isAio()
	{
		return _isAio;
	}
	
	public void setAio(boolean val)
	{
		_isAio = val;
	      
	}
	
	public void rewardAioSkills()
	{
		L2Skill skill;
		for(Integer skillid : Config.AIO_SKILLS.keySet())
		{
			int skilllvl = Config.AIO_SKILLS.get(skillid);
			skill = SkillTable.getInstance().getInfo(skillid,skilllvl);
			if(skill != null)
			{
				addSkill(skill, true);
			}
		}
		sendMessage("Admin give to you Aio's skills");
	}
	
	public void lostAioSkills()
	{
		L2Skill skill;
		for(Integer skillid : Config.AIO_SKILLS.keySet())
		{
			int skilllvl = Config.AIO_SKILLS.get(skillid);
			skill = SkillTable.getInstance().getInfo(skillid,skilllvl);
			if(skill != null)
			{
				removeSkill(skill);
			}
		}
	}
	
	public void setAioEndTime(long val)
	{
		_aio_endTime = val;
	}
		
	public void setEndTime(String process, int val)
	{
		if (val > 0)
		{
			long end_day;
			Calendar calendar = Calendar.getInstance();
			if (val >= 30)
			{
				while(val >= 30)
				{
					if(calendar.get(Calendar.MONTH)== 11)
						calendar.roll(Calendar.YEAR, true);
					calendar.roll(Calendar.MONTH, true);
					val -= 30;
				}
			}
			if (val < 30 && val > 0)
			{
				while(val > 0)
				{
					if(calendar.get(Calendar.DATE)== 28 && calendar.get(Calendar.MONTH) == 1)
						calendar.roll(Calendar.MONTH, true);                    
					if(calendar.get(Calendar.DATE)== 30)
					{
						if(calendar.get(Calendar.MONTH) == 11)
							calendar.roll(Calendar.YEAR, true);
						calendar.roll(Calendar.MONTH, true);
						
					}
					calendar.roll(Calendar.DATE, true);
					val--;
				}
			}
			
			end_day = calendar.getTimeInMillis();
			if(process.equals("aio"))
				_aio_endTime = end_day;
			else if(process.equals("vip"))
				_vip_endTime = end_day;
			else
			{
				System.out.println("process "+ process + "no Known while try set end date");
				return;
			}
			Date dt = new Date(end_day);
			System.out.println(""+process +" end time for player " + getName() + " is " + dt);
		}
		else
		{
			if(process.equals("aio"))
				_aio_endTime = 0;
			else if(process.equals("vip"))
				_vip_endTime = 0;
			else
			{
				System.out.println("process "+ process + "no Known while try set end date");
				return;
			}
		}
	}
	
	public long getAioEndTime()
	{
	return _aio_endTime;
	}

	private static final int FALLING_VALIDATION_DELAY = 10000;
	private long _fallingTimestamp = 0;

	public final boolean isFalling(int z)
	{
		if(isDead() || isFlying() || isInvul() || isInsideZone(ZONE_WATER))
		{
			return false;
		}

		if(isInEvent())
		{
			return false;
		}

		if(System.currentTimeMillis() < _fallingTimestamp)
		{
			return true;
		}

		final int deltaZ = getZ() - z;
		if(deltaZ <= getBaseTemplate().getFallHeight())
		{
			return false;
		}

		final int damage = (int)Formulas.calcFallDam(this, deltaZ);
		if(damage > 0)
		{
			reduceCurrentHp(Math.min(damage, getCurrentHp() - 1), null, false);
			sendPacket(new SystemMessage(SystemMessageId.FALL_DAMAGE_S1).addNumber(damage));
		}

		setFalling();

		return false;
	}

	public final void setFalling()
	{
		_fallingTimestamp = System.currentTimeMillis() + FALLING_VALIDATION_DELAY;
	}

	private Point3D _lastPartyPosition = new Point3D(0, 0, 0);

	public void setLastPartyPosition(int x, int y, int z)
	{
		_lastPartyPosition.setXYZ(x,y,z);
	}

	public int getLastPartyPositionDistance(int x, int y, int z)
	{
		double dx = (x - _lastPartyPosition.getX());
		double dy = (y - _lastPartyPosition.getY());
		double dz = (z - _lastPartyPosition.getZ());

		return (int)Math.sqrt(dx*dx + dy*dy + dz*dz);
	}

	public boolean isLocked()  
    {  
        return _isLocked;  
    }  

    public void setLocked(boolean a)  
    {  
        _isLocked = a;  
    }	
    
    /** The _punish level. */
    private PunishLevel _punishLevel = PunishLevel.NONE;
	
	/** The _punish timer. */
	private long _punishTimer = 0;
	
	/** The _punish task. */
	private ScheduledFuture<?> _punishTask;
    
    /**
     * The Enum PunishLevel.
     */
    public enum PunishLevel
	{
		
		/** The NONE. */
		NONE(0, ""),
		
		/** The CHAT. */
		CHAT(1, "chat banned"),
		
		/** The JAIL. */
		JAIL(2, "jailed"),
		
		/** The CHAR. */
		CHAR(3, "banned"),
		
		/** The ACC. */
		ACC(4, "banned");
		
		/** The pun value. */
		private final int punValue;
		
		/** The pun string. */
		private final String punString;
		
		/**
		 * Instantiates a new punish level.
		 *
		 * @param value the value
		 * @param string the string
		 */
		PunishLevel(int value, String string)
		{
			punValue = value;
			punString = string;
		}
		
		/**
		 * Value.
		 *
		 * @return the int
		 */
		public int value()
		{
			return punValue;
		}
		
		/**
		 * String.
		 *
		 * @return the string
		 */
		public String string()
		{
			return punString;
		}
	}
    
    /**
     * returns punishment level of player.
     *
     * @return the punish level
     */
    public PunishLevel getPunishLevel()
    {
        return _punishLevel;
    }

    /**
     * Checks if is in jail.
     *
     * @return True if player is jailed
     */
    public boolean isInJail()
    {
    	return _punishLevel == PunishLevel.JAIL;
    }
    
    /**
     * Checks if is chat banned.
     *
     * @return True if player is chat banned
     */
    public boolean isChatBanned()
    {
    	return _punishLevel == PunishLevel.CHAT;
    }
    
    /**
     * Sets the punish level.
     *
     * @param state the new punish level
     */
    public void setPunishLevel(int state)
    {
    	switch (state){
    		case 0 :
    		{
    			_punishLevel = PunishLevel.NONE;
    			break;
    		}
    		case 1 :
    		{
    			_punishLevel = PunishLevel.CHAT;
    			break;
    		}
    		case 2 :
    		{
    			_punishLevel = PunishLevel.JAIL;
    			break;
    		}
    		case 3 :
    		{
    			_punishLevel = PunishLevel.CHAR;
    			break;
    		}
    		case 4 :
    		{
    			_punishLevel = PunishLevel.ACC;
    			break;
    		}
    	}
    }

    /**
     * Sets the punish level.
     *
     * @param state the state
     * @param delayInMinutes the delay in minutes
     */
    public void setPunishLevel(PunishLevel state, int delayInMinutes)
    {
    	long delayInMilliseconds = delayInMinutes * 60000L;
    	setPunishLevel(state, delayInMilliseconds);
    	
    }
    
    /**
     * Sets punish level for player based on delay.
     *
     * @param state the state
     * @param delayInMilliseconds 0 - Indefinite
     */
    public void setPunishLevel(PunishLevel state, long delayInMilliseconds)
    {
    	switch (state)
    	{
    		case NONE: // Remove Punishments
	    	{
	    		switch (_punishLevel)
	    		{
	    			case CHAT:
		    		{
		    			_punishLevel = state;
		    			stopPunishTask(true);
		    			sendPacket(new EtcStatusUpdate(this));
			            sendMessage("Your Chat ban has been lifted");
			            break;
		    		}
	    			case JAIL:
		    		{
		    			_punishLevel = state;
		    			// Open a Html message to inform the player
		        		NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
			            String jailInfos = HtmCache.getInstance().getHtm("data/html/jail_out.htm");
			            if (jailInfos != null)
			                htmlMsg.setHtml(jailInfos);
			            else
			                htmlMsg.setHtml("<html><body>You are free for now, respect server rules!</body></html>");
			            sendPacket(htmlMsg);
			            stopPunishTask(true);
			            teleToLocation(17836, 170178, -3507, true);  // Floran
			            break;
		    		}
				default:
					break;
	    		}
	    		break;
	    	}
    		case CHAT: // Chat Ban
	    	{
	    		// not allow player to escape jail using chat ban
	    		if (_punishLevel == PunishLevel.JAIL)
	    			break;
	    		_punishLevel = state;
	    		_punishTimer = 0;
	    		sendPacket(new EtcStatusUpdate(this));
	    		// Remove the task if any
	    		stopPunishTask(false);
	    		
	    		if (delayInMilliseconds > 0)
	    		{
	    			_punishTimer = delayInMilliseconds;
	    			
	    			// start the countdown
	    			int minutes = (int) (delayInMilliseconds/60000);
	    			_punishTask = ThreadPoolManager.getInstance().scheduleGeneral(new PunishTask(this), _punishTimer);
	                sendMessage("You are chat banned for "+minutes+" minutes.");
	    		}
	    		else
	    			sendMessage("You have been chat banned");
	    		break;
	    		
	    	}
    		case JAIL: // Jail Player
	    	{
	    		_punishLevel = state;
	    		_punishTimer = 0;
		        // Remove the task if any
		        stopPunishTask(false);
	
	            if (delayInMilliseconds > 0)
	            {
	                _punishTimer = delayInMilliseconds; // Delay in milliseconds
	
	                // start the countdown
	                _punishTask = ThreadPoolManager.getInstance().scheduleGeneral(new PunishTask(this), _punishTimer);
	                sendMessage("You are in jail for "+delayInMilliseconds/60000+" minutes.");

				}
	            if (Olympiad.getInstance().isRegisteredInComp(this))
	                Olympiad.getInstance().removeDisconnectedCompetitor(this);
	
	            // Open a Html message to inform the player
	            NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
	            String jailInfos = HtmCache.getInstance().getHtm("data/html/jail_in.htm");
	            if (jailInfos != null)
	                htmlMsg.setHtml(jailInfos);
	            else
	                htmlMsg.setHtml("<html><body>You have been put in jail by an admin.</body></html>");
	            sendPacket(htmlMsg);
	            setInstanceId(0);
	            setIsIn7sDungeon(false);
	
	            teleToLocation(-114356, -249645, -2984, false);  // Jail
	            break;
	        }
			case CHAR: // Ban Character
			{
				setAccessLevel(-100);
				logout();
				break;
			}
			case ACC: // Ban Account
			{
				setAccountAccesslevel(-100);
				logout();
				break;
			}
	    	default:
	    	{
	    		_punishLevel = state;
	    		break;
	    	}
    	}

        // store in database
        storeCharBase();
    }

    /**
     * Gets the punish timer.
     *
     * @return the punish timer
     */
    public long getPunishTimer()
    {
        return _punishTimer;
    }

    /**
     * Sets the punish timer.
     *
     * @param time the new punish timer
     */
    public void setPunishTimer(long time)
    {
        _punishTimer = time;
    }

    /**
     * Update punish state.
     */
    private void updatePunishState()
    {
    	if (getPunishLevel() != PunishLevel.NONE)
        {
            // If punish timer exists, restart punishtask.
            if (_punishTimer > 0)
            {
                _punishTask = ThreadPoolManager.getInstance().scheduleGeneral(new PunishTask(this), _punishTimer);
                sendMessage("You are still " + getPunishLevel().string() + " for " + (_punishTimer / 60000) + " minutes.");
            }
            if (getPunishLevel() == PunishLevel.JAIL)
            {
            	// If player escaped, put him back in jail
                if (!isInsideZone(ZONE_JAIL))
                    teleToLocation(-114356,-249645,-2984, true);
            }
        }
    }

    /**
     * Stop punish task.
     *
     * @param save the save
     */
    public void stopPunishTask(boolean save)
    {
        if (_punishTask != null)
        {
            if (save)
            {
            	long delay = _punishTask.getDelay(TimeUnit.MILLISECONDS);
            	if (delay < 0)
            		delay = 0;
            	setPunishTimer(delay);
            }
            _punishTask.cancel(false);
            ThreadPoolManager.getInstance().removeGeneral((Runnable)_punishTask);
            _punishTask = null;
        }
    }

    /**
     * The Class PunishTask.
     */
    private class PunishTask implements Runnable
    {
        
        /** The _player. */
        L2PcInstance _player;
        // protected long _startedAt;

        /**
         * Instantiates a new punish task.
         *
         * @param player the player
         */
        protected PunishTask(L2PcInstance player)
        {
            _player = player;
            // _startedAt = System.currentTimeMillis();
        }

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
		public void run()
        {
            _player.setPunishLevel(PunishLevel.NONE, 0);
        }
    }
    
	public void setExpOn(boolean expOn)
	{
		_expGainOn = expOn;
	}

	public boolean getExpOn()
	{
		return _expGainOn;
	}

	public boolean isInEvent()
	{
		return (atEvent);
	}

	private GatesRequest _gatesRequest = new GatesRequest();

	private static class GatesRequest
	{
		private L2DoorInstance _target = null;
		public void setTarget(L2DoorInstance door)
		{
			_target = door;
		}

		public L2DoorInstance getDoor()
		{
			return _target;
		}
	}

	public void gatesRequest(L2DoorInstance door)
	{
		_gatesRequest.setTarget(door);
	}

	public void gatesAnswer(int answer, int type)
	{
		if(_gatesRequest.getDoor() == null)
		{
			return;
		}

		if(answer == 1 && getTarget() == _gatesRequest.getDoor() && type == 1)
		{
			_gatesRequest.getDoor().openMe();
		}
		else if(answer == 1 && getTarget() == _gatesRequest.getDoor() && type == 0)
		{
			_gatesRequest.getDoor().closeMe();
		}

		_gatesRequest.setTarget(null);
	}

	public String getLang()
	{
		return Config.MULTILANG_ENABLE && getVar("lang@") != null ? getVar("lang@") : "en";
	}

	public void setLang(String lang)
	{
		setVar("lang@", lang);
	}

	HashMap<String, String> user_variables = new HashMap<String, String>();

	public void setVar(String name, Object value)
	{
		setVar(name, String.valueOf(value));
	}

	public void setVar(String name, String value)
	{
		if (user_variables.containsKey(name) && user_variables.get(name).equals(value))
			return;
		user_variables.put(name, value);
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;

			statement = con.prepareStatement("REPLACE INTO character_variables (obj_id, type, name, value) VALUES (?, ?, ?, ?)");
			statement.setInt(1, getObjectId());
			statement.setString(2, "var");
			statement.setString(3, name);
			statement.setString(4, value);
			statement.execute();
			statement.close();
		}
		catch(Exception e)
		{
			_log.warn("Could not stone char_variables data: " + e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public void unsetVar(String name)
	{
		if (name == null)
			return;
		if (user_variables.remove(name) != null)
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement;

				statement = con.prepareStatement("DELETE FROM `character_variables` WHERE `obj_id`= ? AND `type`= ? AND `name`= ? LIMIT 1");
				statement.setInt(1, getObjectId());
				statement.setString(2, "var");
				statement.setString(3, name);
				statement.execute();
				statement.close();
			}
			catch(Exception e)
			{
				_log.warn("Could not unset char_variable: " + e);
			}
			finally
			{
				ResourceUtil.closeConnection(con);
			}
		}
	}

	public String getVar(String name)
	{
		return user_variables.get(name);
	}

	public boolean getVarActive(String name)
	{
		String var = user_variables.get(name);
		return ((var != null) && (!(var.equals("0"))));
	}

	public HashMap<String, String> getVars()
	{
		return user_variables;
	}

	private void loadVariables()
	{
		Connection con = null;
		PreparedStatement offline = null;
		ResultSet rs = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			offline = con.prepareStatement("SELECT * FROM character_variables WHERE obj_id = ?");
			offline.setInt(1, getObjectId());
			rs = offline.executeQuery();
			while (rs.next())
			{
				String name = rs.getString("name");
				String value = rs.getString("value");
				this.user_variables.put(name, value);
			}

			if (getVar("lang@") == null)
				setVar("lang@", Config.MULTILANG_DEFAULT);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}
	
	public void sendChatMessage(int objectId, int messageType, String charName, String text)
	{
		sendPacket(new CreatureSay(objectId, messageType, charName, text));
	}
	
	/**
	* Close the active connection with the client.<BR><BR>
	*/
	public void closeNetConnection(boolean closeClient)
	{
		L2GameClient client = _client;
		if(client != null)
		{
			if(client.isDetached())
			{
				client.cleanMe(true);
			}
			else
			{
				if(!client.getConnection().isClosed())
				{
					if(closeClient)
						client.close(new LeaveWorld());
					else
						client.close(new ServerClose());
				}
			}
		}
	}
	
	public static void setSexDB(L2PcInstance player, int mode)
    {
        Connection con;
        if(player == null)
            return;
        con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("UPDATE characters SET sex=? WHERE obj_Id=?");
            statement.setInt(1, player.getAppearance().getSex() ? 1 : 0);
            statement.setInt(2, player.getObjectId());
            statement.execute();
            statement.close();
        }
        catch(Exception e)
		{
				e.printStackTrace();
			
			_log.warn("SetSex:  Could not store data:" + e);
		}
		finally
		{
			CloseUtil.close(con);
			
		}
    }
	
	public void broadcastRelationsChanges()
	{
		final Collection<L2PcInstance> knownlist = getKnownList().getKnownPlayers().values();
		for (L2PcInstance player : knownlist)
		{
			if (player == null)
				continue;
			
			player.sendPacket(new RelationChanged(this, getRelation(player), isAutoAttackable(player)));
			if (getPet() != null)
				player.sendPacket(new RelationChanged(getPet(), getRelation(player), isAutoAttackable(player)));
		}
	}
	
	public boolean disableAutoShot(int itemId)
	{
		if (getAutoSoulShot().containsKey(itemId))
		{
			removeAutoSoulShot(itemId);
			sendPacket(new ExAutoSoulShot(itemId, 0));
			
			SystemMessage sm = new SystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED);
			sm.addString(ItemTable.getInstance().getTemplate(itemId).getName());
			sendPacket(sm);
			return true;
		}
		else return false;
	}

	@Override  
	public void setIsCastingNow(boolean value)  
	{  
		if (value == false)  
		{  
			_currentSkill = null;  
		}  
		super.setIsCastingNow(value);  
	}
	
	public long getLastAttackPacket()
    {
        return _lastAttackPacket;
    }
    
    	public void setLastAttackPacket()
    {
        _lastAttackPacket = System.currentTimeMillis();
    }    	

}