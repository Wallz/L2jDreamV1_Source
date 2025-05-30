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
package com.src.gameserver.model.actor;

import static com.src.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;

import java.text.DateFormat;
import java.util.List;

import javolution.text.TextBuilder;
import javolution.util.FastList;

import com.src.Config;
import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.cache.HtmCache;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.datatables.sql.ItemTable;
import com.src.gameserver.datatables.sql.SpawnTable;
import com.src.gameserver.datatables.xml.HelperBuffTable;
import com.src.gameserver.idfactory.IdFactory;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.managers.CustomNpcInstanceManager;
import com.src.gameserver.managers.DimensionalRiftManager;
import com.src.gameserver.managers.QuestManager;
import com.src.gameserver.managers.TownManager;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.L2DropCategory;
import com.src.gameserver.model.L2DropData;
import com.src.gameserver.model.L2NpcAIData;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2CastleTeleporterInstance;
import com.src.gameserver.model.actor.instance.L2ControlTowerInstance;
import com.src.gameserver.model.actor.instance.L2CustomNpcInstance;
import com.src.gameserver.model.actor.instance.L2FestivalGuideInstance;
import com.src.gameserver.model.actor.instance.L2FishermanInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2MerchantInstance;
import com.src.gameserver.model.actor.instance.L2NpcInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2TeleporterInstance;
import com.src.gameserver.model.actor.instance.L2WarehouseInstance;
import com.src.gameserver.model.actor.knownlist.NpcKnownList;
import com.src.gameserver.model.actor.stat.NpcStat;
import com.src.gameserver.model.actor.status.NpcStatus;
import com.src.gameserver.model.entity.Hero;
import com.src.gameserver.model.entity.event.Lottery;
import com.src.gameserver.model.entity.olympiad.Olympiad;
import com.src.gameserver.model.entity.sevensigns.SevenSigns;
import com.src.gameserver.model.entity.sevensigns.SevenSignsFestival;
import com.src.gameserver.model.entity.siege.Castle;
import com.src.gameserver.model.multisell.L2Multisell;
import com.src.gameserver.model.quest.Quest;
import com.src.gameserver.model.quest.QuestState;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.gameserver.model.zone.type.L2TownZone;
import com.src.gameserver.network.L2GameClient;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.clientpackets.Say2;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.ExShowVariationCancelWindow;
import com.src.gameserver.network.serverpackets.ExShowVariationMakeWindow;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.MoveToPawn;
import com.src.gameserver.network.serverpackets.MyTargetSelected;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.NpcInfo;
import com.src.gameserver.network.serverpackets.NpcSay;
import com.src.gameserver.network.serverpackets.RadarControl;
import com.src.gameserver.network.serverpackets.SocialAction;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.network.serverpackets.ValidateLocation;
import com.src.gameserver.skills.Stats;
import com.src.gameserver.taskmanager.DecayTaskManager;
import com.src.gameserver.templates.L2HelperBuff;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.templates.item.L2Item;
import com.src.gameserver.templates.item.L2Weapon;
import com.src.gameserver.templates.skills.L2SkillType;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.StringUtil;
import com.src.util.random.Rnd;

public class L2Npc extends L2Character
{
	public static final int INTERACTION_DISTANCE = 150;

	private final L2NpcAIData _staticAIData = getTemplate().getAIDataStatic();
	
	private L2CustomNpcInstance _customNpcInstance;

	private L2Spawn _spawn;

	private boolean _isBusy = false;
	private boolean _hasSpoken = false;
	
	private String _busyMessage = "";

	volatile boolean _isDecayed = false;

	private boolean _isSpoil = false;

	private int _castleIndex = -2;

	public boolean isEventMob = false;

	private boolean _isInTown = false;

	private int _isSpoiledBy = 0;

	protected RandomAnimationTask _rAniTask = null;
	private int _currentLHandId;
	private int _currentRHandId;
	private int _currentCollisionHeight;
	private int _currentCollisionRadius;

	protected class RandomAnimationTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				if(this != _rAniTask)
				{
					return;
				}

				if(isMob())
				{
					if(getAI().getIntention() != AI_INTENTION_ACTIVE)
					{
						return;
					}
				}
				else
				{
					if(!isInActiveRegion())
					{
						return;
					}

					getKnownList().updateKnownObjects();
				}

				if(!(isDead() || isStunned() || isSleeping() || isParalyzed()))
				{
					onRandomAnimation();
				}

				startRandomAnimationTimer();
			}
			catch(Throwable t)
			{
			}
		}
	}

	public void onRandomAnimation()
	{
		int min = _customNpcInstance != null ? 1 : 2;
		int max = _customNpcInstance != null ? 13 : 3;

		SocialAction sa = new SocialAction(getObjectId(), Rnd.get(min, max));
		broadcastPacket(sa);
		sa = null;
	}

	public void startRandomAnimationTimer()
	{
		if(!hasRandomAnimation())
		{
			return;
		}

		int minWait = isMob() ? Config.MIN_MONSTER_ANIMATION : Config.MIN_NPC_ANIMATION;
		int maxWait = isMob() ? Config.MAX_MONSTER_ANIMATION : Config.MAX_NPC_ANIMATION;

		int interval = Rnd.get(minWait, maxWait) * 1000;

		_rAniTask = new RandomAnimationTask();
		ThreadPoolManager.getInstance().scheduleGeneral(_rAniTask, interval);
	}

	public boolean hasRandomAnimation()
	{
		return Config.MAX_NPC_ANIMATION > 0;
	}

	public class destroyTemporalNPC implements Runnable
	{
		private L2Spawn _oldSpawn;

		public destroyTemporalNPC(L2Spawn spawn)
		{
			_oldSpawn = spawn;
		}

		@Override
		public void run()
		{
			try
			{
				_oldSpawn.getLastSpawn().deleteMe();
				_oldSpawn.stopRespawn();
				SpawnTable.getInstance().deleteSpawn(_oldSpawn, false);
			}
			catch(Throwable t)
			{
				t.printStackTrace();
			}
		}
	}

	public class destroyTemporalSummon implements Runnable
	{
		L2Summon _summon;
		L2PcInstance _player;

		public destroyTemporalSummon(L2Summon summon, L2PcInstance player)
		{
			_summon = summon;
			_player = player;
		}

		@Override
		public void run()
		{
			_summon.unSummon(_player);
			_summon = null;
			_player = null;
		}
	}

	public L2Npc(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		getKnownList();
		getStat();
		getStatus();
		initCharStatusUpdateValues();

		_currentLHandId = getTemplate().lhand;
		_currentRHandId = getTemplate().rhand;
		_currentCollisionHeight = getTemplate().collisionHeight;
		_currentCollisionRadius = getTemplate().collisionRadius;

		setName(template.name);

	}

	@Override
	public NpcKnownList getKnownList()
	{
		if(super.getKnownList() == null || !(super.getKnownList() instanceof NpcKnownList))
		{
			setKnownList(new NpcKnownList(this));
		}

		return (NpcKnownList) super.getKnownList();
	}

	@Override
	public NpcStat getStat()
	{
		if(super.getStat() == null || !(super.getStat() instanceof NpcStat))
		{
			setStat(new NpcStat(this));
		}

		return (NpcStat) super.getStat();
	}

	@Override
	public NpcStatus getStatus()
	{
		if(super.getStatus() == null || !(super.getStatus() instanceof NpcStatus))
		{
			setStatus(new NpcStatus(this));
		}

		return (NpcStatus) super.getStatus();
	}

	@Override
	public final L2NpcTemplate getTemplate()
	{
		return (L2NpcTemplate) super.getTemplate();
	}

	public int getNpcId()
	{
		return getTemplate().npcId;
	}

	@Override
	public boolean isAttackable()
	{
		
		return Config.NPC_ATTACKABLE;
	}

	public final String getFactionId()
	{
		return getTemplate().factionId;
	}

	@Override
	public final int getLevel()
	{
		return getTemplate().level;
	}

	public boolean isAggressive()
	{
		return false;
	}

	public int getAggroRange()
	{
		return getTemplate().aggroRange;
	}

	public int getFactionRange()
	{
		return getTemplate().factionRange;
	}

	@Override
	public boolean isUndead()
	{
		return getTemplate().isUndead;
	}

	@Override
	public void updateAbnormalEffect()
	{
		for(L2PcInstance player : getKnownList().getKnownPlayers().values())
		{
			if(player != null)
			{
				player.sendPacket(new NpcInfo(this, player));
			}
		}
	}

	public int getDistanceToWatchObject(L2Object object)
	{
		if(object instanceof L2FestivalGuideInstance)
		{
			return 10000;
		}

		if(object instanceof L2NpcInstance || !(object instanceof L2Character))
		{
			return 0;
		}

		if(object instanceof L2Playable)
		{
			return 1500;
		}

		return 500;
	}

	public int getDistanceToForgetObject(L2Object object)
	{
		return 2 * getDistanceToWatchObject(object);
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}

	public int getLeftHandItem()
	{
		return _currentLHandId;
	}

	public int getRightHandItem()
	{
		return _currentRHandId;
	}

	public boolean isSpoil()
	{
		return _isSpoil;
	}

	public String getEnemyClan()
	{
		return _staticAIData.getEnemyClan();
	}
	
	public String getClan()
	{
		return _staticAIData.getClan();
	}
	
	public void setSpoil(boolean isSpoil)
	{
		_isSpoil = isSpoil;
	}

	public final int getIsSpoiledBy()
	{
		return _isSpoiledBy;
	}

	public final void setIsSpoiledBy(int value)
	{
		_isSpoiledBy = value;
	}

	public final boolean isBusy()
	{
		return _isBusy;
	}

	public void setBusy(boolean isBusy)
	{
		_isBusy = isBusy;
	}

	public final String getBusyMessage()
	{
		return _busyMessage;
	}

	public void setBusyMessage(String message)
	{
		_busyMessage = message;
	}

	protected boolean canTarget(L2PcInstance player)
	{
		if(player.isOutOfControl())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		return true;
	}

	@Override
	public boolean canInteract(L2PcInstance player)
	{
		if(player.isCastingNow())
		{
			return false;
		}

		if(player.isDead() || player.isFakeDeath())
		{
			return false;
		}

		if(player.isSitting())
		{
			return false;
		}

		if(player.getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE)
		{
			return false;
		}

		if(!isInsideRadius(player, INTERACTION_DISTANCE, true, false))
		{
			return false;
		}

		return true;
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if (!player.canTarget())
			return;
		
		// Check if the L2PcInstance already target the L2Npc
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);
			
			// Check if the player is attackable (without a forced attack)
			if (isAutoAttackable(player))
			{
				getAI(); // wake up ai
				
				// Send MyTargetSelected to the L2PcInstance player
				player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));
				
				// Send StatusUpdate of the L2Npc to the L2PcInstance to update its HP bar
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
			}
			// Send MyTargetSelected to the L2PcInstance player
			else
				player.sendPacket(new MyTargetSelected(getObjectId(), 0));
			
			// Send a Server->Client packet ValidateLocation to correct the L2Npc position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Check if the player is attackable (without a forced attack) and isn't dead
			if (isAutoAttackable(player) && !isAlikeDead())
			{
				// Check the height difference
				if (Math.abs(player.getZ() - getZ()) < 400) // this max heigth difference might need some tweaking
				{
					// Set the L2PcInstance Intention to AI_INTENTION_ATTACK
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
				}
				else
				{
					// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
					player.sendPacket(ActionFailed.STATIC_PACKET);
				}
			}
			else if (!isAutoAttackable(player))
			{
				// Calculate the distance between the L2PcInstance and the L2Npc
				if (!canInteract(player))
				{
					// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				}
				else
				{
					// Rotate the player to face the instance
					player.sendPacket(new MoveToPawn(player, this, L2Npc.INTERACTION_DISTANCE));
					
					// Send ActionFailed to the player in order to avoid he stucks
					player.sendPacket(ActionFailed.STATIC_PACKET);
					
					if (hasRandomAnimation())
						onRandomAnimation();
					
					Quest[] qlsa = getTemplate().getEventQuests(Quest.QuestEventType.QUEST_START);
					if ((qlsa != null) && qlsa.length > 0)
						player.setLastQuestNpcObject(getObjectId());
					Quest[] qlst = getTemplate().getEventQuests(Quest.QuestEventType.NPC_FIRST_TALK);
					if ((qlst != null) && qlst.length == 1)
						qlst[0].notifyFirstTalk(this, player);
					else
						showChatWindow(player);
				}
			}
		}
	}

	@Override
	public void onActionShift(L2GameClient client)
	{
		L2PcInstance player = client.getActiveChar();
		if(player == null)
		{
			return;
		}

		if(player.getAccessLevel().isGm())
		{
			player.setTarget(this);
			player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));

			if(isAutoAttackable(player))
			{
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
				su = null;
			}

			NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile("data/html/admin/info/npcinfo.htm");

			html.replace("%objid%", String.valueOf(getObjectId()));
			html.replace("%class%", getClass().getSimpleName());
			html.replace("%id%",    String.valueOf(getTemplate().npcId));
			html.replace("%lvl%",   String.valueOf(getTemplate().level));
			html.replace("%name%",  String.valueOf(getTemplate().name));
			html.replace("%tmplid%",String.valueOf(getTemplate().npcId));
			html.replace("%aggro%", String.valueOf((this instanceof L2Attackable) ? ((L2Attackable) this).getAggroRange() : 0));
			html.replace("%hp%",    String.valueOf((int)((L2Character)this).getCurrentHp()));
			html.replace("%hpmax%", String.valueOf(((L2Character)this).getMaxHp()));
			html.replace("%mp%",    String.valueOf((int)((L2Character)this).getCurrentMp()));
			html.replace("%mpmax%", String.valueOf(((L2Character)this).getMaxMp()));
			html.replace("%patk%", String.valueOf(((L2Character)this).getPAtk(null)));
			html.replace("%matk%", String.valueOf(((L2Character)this).getMAtk(null, null)));
			html.replace("%pdef%", String.valueOf(((L2Character)this).getPDef(null)));
			html.replace("%mdef%", String.valueOf(((L2Character)this).getMDef(null, null)));
			html.replace("%accu%", String.valueOf(((L2Character)this).getAccuracy()));
			html.replace("%evas%", String.valueOf(((L2Character)this).getEvasionRate(null)));
			html.replace("%crit%", String.valueOf(((L2Character)this).getCriticalHit(null, null)));
			html.replace("%rspd%", String.valueOf(((L2Character)this).getRunSpeed()));
			html.replace("%aspd%", String.valueOf(((L2Character)this).getPAtkSpd()));
			html.replace("%cspd%", String.valueOf(((L2Character)this).getMAtkSpd()));
			html.replace("%str%",  String.valueOf(((L2Character)this).getSTR()));
			html.replace("%dex%",  String.valueOf(((L2Character)this).getDEX()));
			html.replace("%con%",  String.valueOf(((L2Character)this).getCON()));
			html.replace("%int%",  String.valueOf(((L2Character)this).getINT()));
			html.replace("%wit%",  String.valueOf(((L2Character)this).getWIT()));
			html.replace("%men%",  String.valueOf(((L2Character)this).getMEN()));
			html.replace("%loc%",  String.valueOf(getX()+" "+getY()+" "+getZ()));
			html.replace("%dist%", String.valueOf((int)Math.sqrt(player.getDistanceSq(this))));

			if (getSpawn() != null)
			{
				html.replace("%spawn%", getSpawn().getLocx()+" "+getSpawn().getLocy()+" "+getSpawn().getLocz());
				html.replace("%loc2d%", String.valueOf((int)Math.sqrt(((L2Character)this).getPlanDistanceSq(getSpawn().getLocx(), getSpawn().getLocy()))));
				html.replace("%loc3d%", String.valueOf((int)Math.sqrt(((L2Character)this).getDistanceSq(getSpawn().getLocx(), getSpawn().getLocy(), getSpawn().getLocz()))));
				html.replace("%resp%",  String.valueOf(getSpawn().getRespawnDelay() / 1000));
			}
			else
			{
				html.replace("%spawn%", "<font color=FF0000>null</font>");
				html.replace("%loc2d%", "<font color=FF0000>--</font>");
				html.replace("%loc3d%", "<font color=FF0000>--</font>");
				html.replace("%resp%",  "<font color=FF0000>--</font>");
			}

			if (hasAI())
			{
				html.replace("%ai_intention%",  "<tr><td><table width=270 border=0><tr><td width=100><font color=FFAA00>Intention:</font></td><td align=right width=170>"+String.valueOf(getAI().getIntention().name())+"</td></tr></table></td></tr>");
				html.replace("%ai%",            "<tr><td><table width=270 border=0><tr><td width=100><font color=FFAA00>AI</font></td><td align=right width=170>"+getAI().getClass().getSimpleName()+"</td></tr></table></td></tr>");
				html.replace("%ai_clan%",       "<tr><td><table width=270 border=0><tr><td width=100><font color=FFAA00>Clan & Range:</font></td><td align=right width=170>"+String.valueOf(getFactionId())+" "+String.valueOf(getFactionRange())+"</td></tr></table></td></tr>");
			}
			else
			{
				html.replace("%ai_intention%",  "");
				html.replace("%ai%",            "");
				html.replace("%ai_clan%",       "");
			}

			player.sendPacket(html);
		}
		else if(Config.ALT_GAME_VIEWNPC)
		{
			player.setTarget(this);

			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);
			my = null;

			if(isAutoAttackable(player))
			{
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
				su = null;
			}

			NpcHtmlMessage html = new NpcHtmlMessage(0);
			TextBuilder html1 = new TextBuilder("<html><body>");

			html1.append("<br><center><font color=\"LEVEL\">[Combat Stats]</font></center>");
			html1.append("<table border=0 width=\"100%\">");
			html1.append("<tr><td>Max.HP</td><td>" + (int) (getMaxHp() / getStat().calcStat(Stats.MAX_HP, 1, this, null)) + "*" + (int) getStat().calcStat(Stats.MAX_HP, 1, this, null) + "</td><td>Max.MP</td><td>" + getMaxMp() + "</td></tr>");
			html1.append("<tr><td>P.Atk.</td><td>" + getPAtk(null) + "</td><td>M.Atk.</td><td>" + getMAtk(null, null) + "</td></tr>");
			html1.append("<tr><td>P.Def.</td><td>" + getPDef(null) + "</td><td>M.Def.</td><td>" + getMDef(null, null) + "</td></tr>");
			html1.append("<tr><td>Accuracy</td><td>" + getAccuracy() + "</td><td>Evasion</td><td>" + getEvasionRate(null) + "</td></tr>");
			html1.append("<tr><td>Critical</td><td>" + getCriticalHit(null, null) + "</td><td>Speed</td><td>" + getRunSpeed() + "</td></tr>");
			html1.append("<tr><td>Atk.Speed</td><td>" + getPAtkSpd() + "</td><td>Cast.Speed</td><td>" + getMAtkSpd() + "</td></tr>");
			html1.append("<tr><td>Race</td><td>" + getTemplate().race + "</td><td></td><td></td></tr>");
			html1.append("</table>");

			html1.append("<br><center><font color=\"LEVEL\">[Basic Stats]</font></center>");
			html1.append("<table border=0 width=\"100%\">");
			html1.append("<tr><td>STR</td><td>" + getSTR() + "</td><td>DEX</td><td>" + getDEX() + "</td><td>CON</td><td>" + getCON() + "</td></tr>");
			html1.append("<tr><td>INT</td><td>" + getINT() + "</td><td>WIT</td><td>" + getWIT() + "</td><td>MEN</td><td>" + getMEN() + "</td></tr>");
			html1.append("</table>");

			html1.append("<br><center><font color=\"LEVEL\">[Drop Info]</font></center>");
			html1.append("Rates legend: <font color=\"ff0000\">50%+</font> <font color=\"00ff00\">30%+</font> <font color=\"0000ff\">less than 30%</font>");
			html1.append("<table border=0 width=\"100%\">");

			for(L2DropCategory cat : getTemplate().getDropData())
			{
				for(L2DropData drop : cat.getAllDrops())
				{
                    if(drop == null || ItemTable.getInstance().getTemplate(drop.getItemId()) == null)
                    {
                        continue;
                    }
                    
					String name = ItemTable.getInstance().getTemplate(drop.getItemId()).getName();

					if(drop.getChance() >= 600000)
					{
						html1.append("<tr><td><font color=\"ff0000\">" + name + "</font></td><td>" + (drop.isQuestDrop() ? "Quest" : cat.isSweep() ? "Sweep" : "Drop") + "</td></tr>");
					}
					else if(drop.getChance() >= 300000)
					{
						html1.append("<tr><td><font color=\"00ff00\">" + name + "</font></td><td>" + (drop.isQuestDrop() ? "Quest" : cat.isSweep() ? "Sweep" : "Drop") + "</td></tr>");
					}
					else
					{
						html1.append("<tr><td><font color=\"0000ff\">" + name + "</font></td><td>" + (drop.isQuestDrop() ? "Quest" : cat.isSweep() ? "Sweep" : "Drop") + "</td></tr>");
					}
				}
			}

			html1.append("</table>");
			html1.append("</body></html>");

			html.setHtml(html1.toString());
			player.sendPacket(html);

			html = null;
			html1 = null;
		}

		player.sendPacket(ActionFailed.STATIC_PACKET);

		player = null;
	}

	public final Castle getCastle()
	{
		if(_castleIndex < 0)
		{
			L2TownZone town = TownManager.getInstance().getTown(getX(), getY(), getZ());

			if(town != null)
			{
				_castleIndex = CastleManager.getInstance().getCastleIndex(town.getTaxById());
			}

			if(_castleIndex < 0)
			{
				_castleIndex = CastleManager.getInstance().findNearestCastlesIndex(this);
			}
			else
			{
				_isInTown = true;
			}
		}

		if(_castleIndex < 0)
		{
			return null;
		}

		return CastleManager.getInstance().getCastles().get(_castleIndex);
	}

	public final boolean getIsInTown()
	{
		if(_castleIndex < 0)
		{
			getCastle();
		}

		return _isInTown;
	}

	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if(isBusy() && getBusyMessage().length() > 0)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);

			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile("/data/html/npcbusy.htm");
			html.replace("%busymessage%", getBusyMessage());
			html.replace("%npcname%", getName());
			html.replace("%playername%", player.getName());
			player.sendPacket(html);
			html = null;
		}
		else if(command.equalsIgnoreCase("TerritoryStatus"))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			{
				if(getCastle().getOwnerId() > 0)
				{
					html.setFile("/data/html/territorystatus.htm");
					L2Clan clan = ClanTable.getInstance().getClan(getCastle().getOwnerId());
					html.replace("%clanname%", clan.getName());
					html.replace("%clanleadername%", clan.getLeaderName());
				}
				else
				{
					html.setFile("/data/html/territorynoclan.htm");
				}
			}
			html.replace("%castlename%", getCastle().getName());
			html.replace("%taxpercent%", "" + getCastle().getTaxPercent());
			html.replace("%objectId%", String.valueOf(getObjectId()));
			{
				if(getCastle().getCastleId() > 6)
				{
					html.replace("%territory%", "The Kingdom of Elmore");
				}
				else
				{
					html.replace("%territory%", "The Kingdom of Aden");
				}
			}
			player.sendPacket(html);
			html = null;
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
		else if(command.startsWith("Chat"))
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
			showChatWindow(player, val);
		}
		else if (command.startsWith("Link"))
		{
			String path = command.substring(5).trim();
			if (path.indexOf("..") != -1)
				return;
			String filename = new StringBuilder().append("data/html/").append(path).toString();
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(filename);
			html.replace("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(html);
			if ((player.isGM()) && (player.getAccessLevel().getLevel() == Config.GM_ACCESS_LEVEL))
				player.sendChatMessage(0, 0, "HTML", filename);
		}
		else if(command.startsWith("NobleTeleport"))
		{
			if(!player.isNoble())
			{
				String filename = "/data/html/teleporter/nobleteleporter-no.htm";
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(filename);
				html.replace("%objectId%", String.valueOf(getObjectId()));
				html.replace("%npcname%", getName());
				player.sendPacket(html);
				html = null;
				filename = null;
				return;
			}
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
			showChatWindow(player, val);
		}
		else if(command.startsWith("Loto"))
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
			if(val == 0)
			{
				for(int i = 0; i < 5; i++)
				{
					player.setLoto(i, 0);
				}
			}
			showLotoWindow(player, val);
		}
		else if(command.startsWith("CPRecovery"))
		{
			makeCPRecovery(player);
		}
		else if(command.startsWith("SupportMagic"))
		{
			makeSupportMagic(player);
		}
		else if(command.startsWith("GiveBlessing"))
		{
			giveBlessingSupport(player);
		}
		else if(command.startsWith("multisell"))
		{
			L2Multisell.getInstance().SeparateAndSend(Integer.parseInt(command.substring(9).trim()), player, ((L2Npc)player.getTarget()).getNpcId(), false, getCastle().getTaxRate(), false);
		}
		else if(command.startsWith("exc_multisell"))
		{
			L2Multisell.getInstance().SeparateAndSend(Integer.parseInt(command.substring(13).trim()), player, ((L2Npc)player.getTarget()).getNpcId(), true, getCastle().getTaxRate(), false);
		}
		else if(command.startsWith("Augment"))
		{
			int cmdChoice = Integer.parseInt(command.substring(8, 9).trim());
			switch(cmdChoice)
			{
				case 1:
					player.sendPacket(new SystemMessage(SystemMessageId.SELECT_THE_ITEM_TO_BE_AUGMENTED));
					player.sendPacket(new ExShowVariationMakeWindow());
					break;
				case 2:
					player.sendPacket(new SystemMessage(SystemMessageId.SELECT_THE_ITEM_FROM_WHICH_YOU_WISH_TO_REMOVE_AUGMENTATION));
					player.sendPacket(new ExShowVariationCancelWindow());
					break;
			}
		}
		else if(command.startsWith("npcfind_byid"))
		{
			try
			{
				L2Spawn spawn = SpawnTable.getInstance().getTemplate(Integer.parseInt(command.substring(12).trim()));

				if(spawn != null)
				{
					player.sendPacket(new RadarControl(0, 1, spawn.getLocx(), spawn.getLocy(), spawn.getLocz()));
					spawn = null;
				}
			}
			catch(NumberFormatException nfe)
			{
				player.sendMessage("Wrong command parameters");
			}
		}
		else if(command.startsWith("EnterRift"))
		{
			try
			{
				Byte b1 = Byte.parseByte(command.substring(10));
				DimensionalRiftManager.getInstance().start(player, b1, this);
				b1 = null;
			}
			catch(Exception e)
			{
			}
		}
		else if(command.startsWith("ChangeRiftRoom"))
		{
			if(player.isInParty() && player.getParty().isInDimensionalRift())
			{
				player.getParty().getDimensionalRift().manualTeleport(player, this);
			}
			else
			{
				DimensionalRiftManager.getInstance().handleCheat(player, this);
			}
		}
		else if(command.startsWith("ExitRift"))
		{
			if(player.isInParty() && player.getParty().isInDimensionalRift())
			{
				player.getParty().getDimensionalRift().manualExitRift(player, this);
			}
			else
			{
				DimensionalRiftManager.getInstance().handleCheat(player, this);
			}
		}
	}

	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getActiveWeaponItem()
	{
		int weaponId = getTemplate().rhand;

		if(weaponId < 1)
		{
			return null;
		}

		L2Item item = ItemTable.getInstance().getTemplate(getTemplate().rhand);

		if(!(item instanceof L2Weapon))
		{
			return null;
		}

		return (L2Weapon) item;
	}

	public void giveBlessingSupport(L2PcInstance player)
	{
		if(player == null)
		{
			return;
		}

		if(player.isCursedWeaponEquiped())
		{
			return;
		}

		int player_level = player.getLevel();
		setTarget(player);
		if(player_level > 39 || player.getClassId().level() >= 2)
		{
			String content = "<html><body>Newbie Guide:<br>I'm sorry, but you are not eligible to receive the protection blessing.<br1>It can only be bestowed on <font color=\"LEVEL\">characters below level 39 who have not made a seccond transfer.</font></body></html>";
			insertObjectIdAndShowChatWindow(player, content);
			content = null;
			return;
		}
		L2Skill skill = SkillTable.getInstance().getInfo(5182, 1);
		doCast(skill);
		skill = null;
	}

	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		int weaponId = getTemplate().lhand;

		if(weaponId < 1)
		{
			return null;
		}

		L2Item item = ItemTable.getInstance().getTemplate(getTemplate().lhand);

		if(!(item instanceof L2Weapon))
		{
			return null;
		}

		return (L2Weapon) item;
	}

	public void insertObjectIdAndShowChatWindow(L2PcInstance player, String content)
	{
		content = content.replaceAll("%objectId%", String.valueOf(getObjectId()));
		NpcHtmlMessage npcReply = new NpcHtmlMessage(getObjectId());
		npcReply.setHtml(content);
		player.sendPacket(npcReply);
		npcReply = null;
	}

	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";

		if(val == 0)
		{
			pom = new StringBuilder().append("").append(npcId).toString();
		}
		else
		{
			pom = new StringBuilder().append(npcId).append("-").append(val).toString();
		}

		String temp = new StringBuilder().append("data/html/default/").append(pom).append(".htm").toString();

		if(!Config.LAZY_CACHE)
		{
			if(HtmCache.getInstance().contains(temp))
			{
				return temp;
			}
		}
		else
		{
			if(HtmCache.getInstance().isLoadable(temp))
			{
				return temp;
			}
		}

		return "data/html/npcdefault.htm";
	}

	public static void showQuestChooseWindow(L2PcInstance player, L2Npc npc, Quest[] quests)
	{
		final StringBuilder sb = StringUtil.startAppend(150, "<html><body>");
		
		for (Quest q : quests)
		{
			if (q == null)
				continue;
			
			StringUtil.append(sb, "<a action=\"bypass -h npc_", String.valueOf(npc.getObjectId()), "_Quest ", q.getName(), "\">[", q.getDescr());
			
			QuestState qs = player.getQuestState(q.getScriptName());
			if (qs != null)
			{
				if (qs.isStarted() && (qs.getInt("cond") > 0))
					sb.append(" (In Progress)");
				else if (qs.isCompleted())
					sb.append(" (Done)");
			}
			sb.append("]</a><br>");
		}
		
		sb.append("</body></html>");
		
		// Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2NpcInstance
		npc.insertObjectIdAndShowChatWindow(player, sb.toString());
	}

	public void showQuestWindow(L2PcInstance player, L2Npc l2Npc, String questId)
    {
        String content = null;

        Quest q = null;
        if (!Config.ALT_DEV_NO_QUESTS)
            q = QuestManager.getInstance().getQuest(questId);

        // Get the state of the selected quest
        QuestState qs = player.getQuestState(questId);

        if (q == null)
        {
            // No quests found
            content = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
        }
        else
        {
            if (player.getWeightPenalty() >= 3 && q.getQuestIntId() >= 1 && q.getQuestIntId() < 1000)
            {
                player.sendPacket(new SystemMessage(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT));
                return;
            }

            if (qs == null)
            {
                if (q.getQuestIntId() >= 1 && q.getQuestIntId() < 20000)
                {
                    Quest[] questList = player.getAllActiveQuests();
                    if (questList.length >= 25) // if too many ongoing quests, don't show window and send message
                    {
                        player.sendMessage("You have too many quests, cannot register");
                        return;
                    }
                }
                // Check for start point
                Quest[] qlst = getTemplate().getEventQuests(Quest.QuestEventType.QUEST_START);

                if (qlst != null && qlst.length > 0)
                {
                    for (Quest temp : qlst)
                    {
                        if (temp == q)
                        {
                            qs = q.newQuestState(player);
                            break;
                        }
                    }
                }
            }
        }

        if (qs != null)
        {
            // If the quest is already started, no need to show a window
            if (!qs.getQuest().notifyTalk(this, qs))
                return;

            questId = qs.getQuest().getName();
            String stateId = qs.getStateId();
            String path = Config.DATAPACK_ROOT + "/data/scripts/quests/" + questId + "/" + stateId + ".htm";
            content = HtmCache.getInstance().getHtm(path);

        }

        // Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2Npc
        if (content != null)
            insertObjectIdAndShowChatWindow(player, content);

        // Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
        player.sendPacket(ActionFailed.STATIC_PACKET);
    }
	
	public void showQuestWindow(L2PcInstance player, L2Npc npc)
    {
        // collect awaiting quests and start points
        List<Quest> options = new FastList<Quest>();

        QuestState[] awaits = player.getQuestsForTalk(getTemplate().npcId);
        Quest[] starts = getTemplate().getEventQuests(Quest.QuestEventType.QUEST_START);

        // Quests are limited between 1 and 999 because those are the quests that are supported by the client.
        // By limitting them there, we are allowed to create custom quests at higher IDs without interfering
        if (awaits != null)
        {
            for (QuestState x : awaits)
            {
                if (!options.contains(x.getQuest()))
                    if (x.getQuest().getQuestIntId() > 0 && x.getQuest().getQuestIntId() < 1000)
                    {
                        options.add(x.getQuest());
                    }
            }
        }

        if (starts != null)
        {
            for (Quest x : starts)
            {
                if (!options.contains(x))
                    if (x.getQuestIntId() > 0 && x.getQuestIntId() < 1000)
                    {
                        options.add(x);
                    }
            }
        }

        // Display a QuestChooseWindow (if several quests are available) or QuestWindow
        if (options.size() > 1)
        {
        	showQuestChooseWindow(player, npc, options.toArray(new Quest[options.size()]));
        }
        else if (options.size() == 1)
        {
        	showQuestWindow(player, npc, options.get(0).getName());
        }
        else
        {
        	showQuestWindow(player, npc, "");
        }
    }

	public void showLotoWindow(L2PcInstance player, int val)
	{
		int npcId = getTemplate().npcId;
		String filename;
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

		if(val == 0)
		{
			filename = getHtmlPath(npcId, 1);
			html.setFile(filename);
		}
		else if(val >= 1 && val <= 21)
		{
			if(!Lottery.getInstance().isStarted())
			{
				player.sendPacket(new SystemMessage(SystemMessageId.NO_LOTTERY_TICKETS_CURRENT_SOLD));
				return;
			}

			if(!Lottery.getInstance().isSellableTickets())
			{
				player.sendPacket(new SystemMessage(SystemMessageId.NO_LOTTERY_TICKETS_AVAILABLE));
				return;
			}

			filename = getHtmlPath(npcId, 5);
			html.setFile(filename);

			int count = 0;
			int found = 0;
			for(int i = 0; i < 5; i++)
			{
				if(player.getLoto(i) == val)
				{
					player.setLoto(i, 0);
					found = 1;
				}
				else if(player.getLoto(i) > 0)
				{
					count++;
				}
			}

			if(count < 5 && found == 0 && val <= 20)
			{
				for(int i = 0; i < 5; i++)
				{
					if(player.getLoto(i) == 0)
					{
						player.setLoto(i, val);
						break;
					}
				}
			}

			count = 0;
			for(int i = 0; i < 5; i++)
			{
				if(player.getLoto(i) > 0)
				{
					count++;
					String button = String.valueOf(player.getLoto(i));
					if(player.getLoto(i) < 10)
					{
						button = "0" + button;
					}
					String search = "fore=\"L2UI.lottoNum" + button + "\" back=\"L2UI.lottoNum" + button + "a_check\"";
					String replace = "fore=\"L2UI.lottoNum" + button + "a_check\" back=\"L2UI.lottoNum" + button + "\"";
					html.replace(search, replace);
				}
			}

			if(count == 5)
			{
				String search = "0\">Return";
				String replace = "22\">The winner selected the numbers above.";
				html.replace(search, replace);
			}
		}
		else if(val == 22)
		{
			if(!Lottery.getInstance().isStarted())
			{
				player.sendPacket(new SystemMessage(SystemMessageId.NO_LOTTERY_TICKETS_CURRENT_SOLD));
				return;
			}

			if(!Lottery.getInstance().isSellableTickets())
			{
				player.sendPacket(new SystemMessage(SystemMessageId.NO_LOTTERY_TICKETS_AVAILABLE));
				return;
			}

			int price = Config.ALT_LOTTERY_TICKET_PRICE;
			int lotonumber = Lottery.getInstance().getId();
			int enchant = 0;
			int type2 = 0;

			for(int i = 0; i < 5; i++)
			{
				if(player.getLoto(i) == 0)
				{
					return;
				}

				if(player.getLoto(i) < 17)
				{
					enchant += Math.pow(2, player.getLoto(i) - 1);
				}
				else
				{
					type2 += Math.pow(2, player.getLoto(i) - 17);
				}
			}
			if(player.getAdena() < price)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
				return;
			}

			if(!player.reduceAdena("Loto", price, this, true))
			{
				return;
			}
			Lottery.getInstance().increasePrize(price);

			player.sendPacket(new SystemMessage(SystemMessageId.ACQUIRED).addNumber(lotonumber).addItemName(4442));

			L2ItemInstance item = new L2ItemInstance(IdFactory.getInstance().getNextId(), 4442);
			item.setCount(1);
			item.setCustomType1(lotonumber);
			item.setEnchantLevel(enchant);
			item.setCustomType2(type2);
			player.getInventory().addItem("Loto", item, player, this);
			item = null;

			InventoryUpdate iu = new InventoryUpdate();
			iu.addItem(item);
			L2ItemInstance adenaupdate = player.getInventory().getItemByItemId(57);
			iu.addModifiedItem(adenaupdate);
			player.sendPacket(iu);

			filename = getHtmlPath(npcId, 3);
			html.setFile(filename);
		}
		else if(val == 23)
		{
			filename = getHtmlPath(npcId, 3);
			html.setFile(filename);
		}
		else if(val == 24)
		{
			filename = getHtmlPath(npcId, 4);
			html.setFile(filename);

			int lotonumber = Lottery.getInstance().getId();
			String message = "";
			for(L2ItemInstance item : player.getInventory().getItems())
			{
				if(item == null)
				{
					continue;
				}

				if(item.getItemId() == 4442 && item.getCustomType1() < lotonumber)
				{
					message = message + "<a action=\"bypass -h npc_%objectId%_Loto " + item.getObjectId() + "\">" + item.getCustomType1() + " Event Number ";
					int[] numbers = Lottery.getInstance().decodeNumbers(item.getEnchantLevel(), item.getCustomType2());
					for(int i = 0; i < 5; i++)
					{
						message += numbers[i] + " ";
					}
					int[] check = Lottery.getInstance().checkTicket(item);
					if(check[0] > 0)
					{
						switch(check[0])
						{
							case 1:
								message += "- 1st Prize";
								break;
							case 2:
								message += "- 2nd Prize";
								break;
							case 3:
								message += "- 3th Prize";
								break;
							case 4:
								message += "- 4th Prize";
								break;
						}
						message += " " + check[1] + "a.";
					}
					message += "</a><br>";
				}
			}
			if(message == "")
			{
				message += "There is no winning lottery ticket...<br>";
			}
			html.replace("%result%", message);
			message = null;
		}
		else if(val > 24)
		{
			int lotonumber = Lottery.getInstance().getId();
			L2ItemInstance item = player.getInventory().getItemByObjectId(val);
			if(item == null || item.getItemId() != 4442 || item.getCustomType1() >= lotonumber)
			{
				return;
			}
			int[] check = Lottery.getInstance().checkTicket(item);

			player.sendPacket(new SystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(4442));

			int adena = check[1];
			if(adena > 0)
			{
				player.addAdena("Loto", adena, this, true);
			}
			player.destroyItem("Loto", item, this, false);
			return;
		}
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%race%", "" + Lottery.getInstance().getId());
		html.replace("%adena%", "" + Lottery.getInstance().getPrize());
		html.replace("%ticket_price%", "" + Config.ALT_LOTTERY_TICKET_PRICE);
		html.replace("%prize5%", "" + Config.ALT_LOTTERY_5_NUMBER_RATE * 100);
		html.replace("%prize4%", "" + Config.ALT_LOTTERY_4_NUMBER_RATE * 100);
		html.replace("%prize3%", "" + Config.ALT_LOTTERY_3_NUMBER_RATE * 100);
		html.replace("%prize2%", "" + Config.ALT_LOTTERY_2_AND_1_NUMBER_PRIZE);
		html.replace("%enddate%", "" + DateFormat.getDateInstance().format(Lottery.getInstance().getEndDate()));
		player.sendPacket(html);

		html = null;

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	public void makeCPRecovery(L2PcInstance player)
	{
		if(getNpcId() != 31225 && getNpcId() != 31226)
		{
			return;
		}

		if(player.isCursedWeaponEquiped())
		{
			player.sendMessage("Go away, you're not welcome here.");
			return;
		}

		int neededmoney = 100;

		if(!player.reduceAdena("RestoreCP", neededmoney, player.getLastFolkNPC(), true))
		{
			return;
		}
		
		L2Skill skill = SkillTable.getInstance().getInfo(4380, 1);
		if (skill != null)
		{
			setTarget(player);
			doCast(skill);
		}
		
		player.setCurrentCp(player.getMaxCp());
		player.sendPacket(new SystemMessage(SystemMessageId.S1_CP_WILL_BE_RESTORED).addString(player.getName()));
	}

	public void makeSupportMagic(L2PcInstance player)
	{
		if(player == null)
		{
			return;
		}

		if(player.isCursedWeaponEquiped())
		{
			return;
		}

		int player_level = player.getLevel();
		int lowestLevel = 0;
		int higestLevel = 0;

		setTarget(player);

		if(player.isMageClass())
		{
			lowestLevel = HelperBuffTable.getInstance().getMagicClassLowestLevel();
			higestLevel = HelperBuffTable.getInstance().getMagicClassHighestLevel();
		}
		else
		{
			lowestLevel = HelperBuffTable.getInstance().getPhysicClassLowestLevel();
			higestLevel = HelperBuffTable.getInstance().getPhysicClassHighestLevel();
		}

		if(player_level > higestLevel || player.isNewbie())
		{
			String content = "<html><body>Newbie Guide:<br>Only a <font color=\"LEVEL\">novice character of level " + higestLevel + " or less</font> can receive my support magic.<br>Your novice character is the first one that you created and " + "raised in this world.</body></html>";
			insertObjectIdAndShowChatWindow(player, content);
			return;
		}

		if(player_level < lowestLevel || player.isNewbie())
		{
			String content = "<html><body>Come back here when you have reached level " + lowestLevel + ". I will give you support magic then.</body></html>";
			insertObjectIdAndShowChatWindow(player, content);
			return;
		}

		L2Skill skill = null;

		for(L2HelperBuff helperBuffItem : HelperBuffTable.getInstance().getHelperBuffTable())
		{
			if(helperBuffItem.isMagicClassBuff() == player.isMageClass())
			{
				if(player_level >= helperBuffItem.getLowerLevel() && player_level <= helperBuffItem.getUpperLevel())
				{
					skill = SkillTable.getInstance().getInfo(helperBuffItem.getSkillID(), helperBuffItem.getSkillLevel());

					if(skill.getSkillType() == L2SkillType.SUMMON)
					{
						player.doSimultaneousCast(skill);
					}
					else
					{
						doCast(skill);
					}
				}
			}
		}
	}

	public void showChatWindow(L2PcInstance player)
	{
		showChatWindow(player, 0);
	}

	private boolean showPkDenyChatWindow(L2PcInstance player, String type)
	{
		String html = HtmCache.getInstance().getHtm("data/html/" + type + "/" + getNpcId() + "-pk.htm");

		if(html != null)
		{
			NpcHtmlMessage pkDenyMsg = new NpcHtmlMessage(getObjectId());
			pkDenyMsg.setHtml(html);
			player.sendPacket(pkDenyMsg);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			html = null;
			return true;
		}

		return false;
	}

	public void showChatWindow(L2PcInstance player, int val)
	{
		if(player.isSitting() || player.isDead() || player.isFakeDeath() || player.getActiveTradeList() != null)
			   return;
		
		if(Config.PLAYER_MOVEMENT_BLOCK_TIME > 0) 
			player.updateNotMoveUntil();
		
		if(player.getKarma() > 0)
		{
			if(!Config.KARMA_PLAYER_CAN_SHOP && this instanceof L2MerchantInstance)
			{
				if(showPkDenyChatWindow(player, "merchant"))
				{
					return;
				}
			}
			else if(!Config.KARMA_PLAYER_CAN_USE_GK && this instanceof L2TeleporterInstance)
			{
				if(showPkDenyChatWindow(player, "teleporter"))
				{
					return;
				}
			}
			else if(!Config.KARMA_PLAYER_CAN_USE_WAREHOUSE && this instanceof L2WarehouseInstance)
			{
				if(showPkDenyChatWindow(player, "warehouse"))
				{
					return;
				}
			}
			else if(!Config.KARMA_PLAYER_CAN_SHOP && this instanceof L2FishermanInstance)
			{
				if(showPkDenyChatWindow(player, "fisherman"))
				{
					return;
				}
			}
		}

		if(getTemplate().type == "L2Auctioneer" && val == 0)
		{
			return;
		}

		int npcId = getTemplate().npcId;

		String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH;
		int sealAvariceOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_AVARICE);
		int sealGnosisOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_GNOSIS);
		int playerCabal = SevenSigns.getInstance().getPlayerCabal(player);
		boolean isSealValidationPeriod = SevenSigns.getInstance().isSealValidationPeriod();
		int compWinner = SevenSigns.getInstance().getCabalHighestScore();

		switch(npcId)
		{
			case 31078:
			case 31079:
			case 31080:
			case 31081:
			case 31082:
			case 31083:
			case 31084:
			case 31168:
			case 31692:
			case 31694:
			case 31997:
				switch(playerCabal)
				{
					case SevenSigns.CABAL_DAWN:
						if(isSealValidationPeriod)
						{
							if(compWinner == SevenSigns.CABAL_DAWN)
							{
								if(compWinner != sealGnosisOwner)
								{
									filename += "dawn_priest_2c.htm";
								}
								else
								{
									filename += "dawn_priest_2a.htm";
								}
							}
							else
							{
								filename += "dawn_priest_2b.htm";
							}
						}
						else
						{
							filename += "dawn_priest_1b.htm";
						}
						break;
					case SevenSigns.CABAL_DUSK:
						if(isSealValidationPeriod)
						{
							filename += "dawn_priest_3b.htm";
						}
						else
						{
							filename += "dawn_priest_3a.htm";
						}
						break;
					default:
						if(isSealValidationPeriod)
						{
							if(compWinner == SevenSigns.CABAL_DAWN)
							{
								filename += "dawn_priest_4.htm";
							}
							else
							{
								filename += "dawn_priest_2b.htm";
							}
						}
						else
						{
							filename += "dawn_priest_1a.htm";
						}
						break;
				}
				break;
			case 31085:
			case 31086:
			case 31087:
			case 31088:
			case 31089:
			case 31090:
			case 31091:
			case 31169:
			case 31693:
			case 31695:
			case 31998:
				switch(playerCabal)
				{
					case SevenSigns.CABAL_DUSK:
						if(isSealValidationPeriod)
						{
							if(compWinner == SevenSigns.CABAL_DUSK)
							{
								if(compWinner != sealGnosisOwner)
								{
									filename += "dusk_priest_2c.htm";
								}
								else
								{
									filename += "dusk_priest_2a.htm";
								}
							}
							else
							{
								filename += "dusk_priest_2b.htm";
							}
						}
						else
						{
							filename += "dusk_priest_1b.htm";
						}
						break;
					case SevenSigns.CABAL_DAWN:
						if(isSealValidationPeriod)
						{
							filename += "dusk_priest_3b.htm";
						}
						else
						{
							filename += "dusk_priest_3a.htm";
						}
						break;
					default:
						if(isSealValidationPeriod)
						{
							if(compWinner == SevenSigns.CABAL_DUSK)
							{
								filename += "dusk_priest_4.htm";
							}
							else
							{
								filename += "dusk_priest_2b.htm";
							}
						}
						else
						{
							filename += "dusk_priest_1a.htm";
						}
						break;
				}
				break;
			case 31095:
			case 31096:
			case 31097:
			case 31098:
			case 31099:
			case 31100:
			case 31101:
			case 31102:
				if(isSealValidationPeriod)
				{
					if(Config.REQUIRE_WIN_7S)
					{
						if(playerCabal != compWinner || sealAvariceOwner != compWinner)
						{
							switch(compWinner)
							{
								case SevenSigns.CABAL_DAWN:
									player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DAWN));
									filename += "necro_no.htm";
									break;
								case SevenSigns.CABAL_DUSK:
									player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DUSK));
									filename += "necro_no.htm";
									break;
								case SevenSigns.CABAL_NULL:
									filename = getHtmlPath(npcId, val);
									break;
							}
						}
						else
						{
							filename = getHtmlPath(npcId, val);
						}
					}
					else
					{
						filename = getHtmlPath(npcId, val);
					}
				}
				else
				{
					if(playerCabal == SevenSigns.CABAL_NULL)
					{
						filename += "necro_no.htm";
					}
					else
					{
						filename = getHtmlPath(npcId, val);
					}
				}
				break;
			case 31114:
			case 31115:
			case 31116:
			case 31117:
			case 31118:
			case 31119:
				if(isSealValidationPeriod)
				{
					if(Config.REQUIRE_WIN_7S)
					{
						if(playerCabal != compWinner || sealGnosisOwner != compWinner)
						{
							switch(compWinner)
							{
								case SevenSigns.CABAL_DAWN:
									player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DAWN));
									filename += "cata_no.htm";
									break;
								case SevenSigns.CABAL_DUSK:
									player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DUSK));
									filename += "cata_no.htm";
									break;
								case SevenSigns.CABAL_NULL:
									filename = getHtmlPath(npcId, val);
									break;
							}
						}
						else
						{
							filename = getHtmlPath(npcId, val);
						}
					}
					else
					{
						filename = getHtmlPath(npcId, val);
					}
				}
				else
				{
					if(playerCabal == SevenSigns.CABAL_NULL)
					{
						filename += "cata_no.htm";
					}
					else
					{
						filename = getHtmlPath(npcId, val);
					}
				}
				break;
			case 31111:
				if(playerCabal == sealAvariceOwner && playerCabal == compWinner)
				{
					switch(sealAvariceOwner)
					{
						case SevenSigns.CABAL_DAWN:
							filename += "spirit_dawn.htm";
							break;
						case SevenSigns.CABAL_DUSK:
							filename += "spirit_dusk.htm";
							break;
						case SevenSigns.CABAL_NULL:
							filename += "spirit_null.htm";
							break;
					}
				}
				else
				{
					filename += "spirit_null.htm";
				}
				break;
			case 31112:
				filename += "spirit_exit.htm";
				break;
			case 31127:
			case 31128:
			case 31129:
			case 31130:
			case 31131:
				filename += "festival/dawn_guide.htm";
				break;
			case 31137:
			case 31138:
			case 31139:
			case 31140:
			case 31141:
				filename += "festival/dusk_guide.htm";
				break;
			case 31092:
				filename += "blkmrkt_1.htm";
				break;
			case 31113:
				if(Config.REQUIRE_WIN_7S)
				{
					switch(compWinner)
					{
						case SevenSigns.CABAL_DAWN:
							if(playerCabal != compWinner || playerCabal != sealAvariceOwner)
							{
								player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DAWN));
								player.sendPacket(ActionFailed.STATIC_PACKET);
								return;
							}
							break;
						case SevenSigns.CABAL_DUSK:
							if(playerCabal != compWinner || playerCabal != sealAvariceOwner)
							{
								player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DUSK));
								player.sendPacket(ActionFailed.STATIC_PACKET);
								return;
							}
							break;
					}
				}
				filename += "mammmerch_1.htm";
				break;
			case 31126:
				if(Config.REQUIRE_WIN_7S)
				{
					switch(compWinner)
					{
						case SevenSigns.CABAL_DAWN:
							if(playerCabal != compWinner || playerCabal != sealGnosisOwner)
							{
								player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DAWN));
								player.sendPacket(ActionFailed.STATIC_PACKET);
								return;
							}
							break;
						case SevenSigns.CABAL_DUSK:
							if(playerCabal != compWinner || playerCabal != sealGnosisOwner)
							{
								player.sendPacket(new SystemMessage(SystemMessageId.CAN_BE_USED_BY_DUSK));
								player.sendPacket(ActionFailed.STATIC_PACKET);
								return;
							}
							break;
					}
				}
				filename += "mammblack_1.htm";
				break;
			case 31132:
			case 31133:
			case 31134:
			case 31135:
			case 31136:
			case 31142:
			case 31143:
			case 31144:
			case 31145:
			case 31146:
				filename += "festival/festival_witch.htm";
				break;
			case 31688:
				if(player.isNoble())
				{
					filename = Olympiad.OLYMPIAD_HTML_FILE + "noble_main.htm";
				}
				else
				{
					filename = getHtmlPath(npcId, val);
				}
				break;
			case 31690:
			case 31769:
			case 31770:
			case 31771:
			case 31772:
				if(player.isHero() || Hero.getInstance().isInactiveHero(player.getObjectId()))
				{
					filename = Olympiad.OLYMPIAD_HTML_FILE + "hero_main.htm";
				}
				else
				{
					filename = getHtmlPath(npcId, val);
				}
				break;
			default:
				if(npcId >= 31865 && npcId <= 31918)
				{
					filename += "rift/GuardianOfBorder.htm";
					break;
				}
				if(npcId >= 31093 && npcId <= 31094 || npcId >= 31172 && npcId <= 31201 || npcId >= 31239 && npcId <= 31254)
				{
					return;
				}
				filename = getHtmlPath(npcId, val);
				break;
		}

		if(this instanceof L2CastleTeleporterInstance)
		{
			((L2CastleTeleporterInstance)this).showChatWindow(player);
			return;
		}

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);

		if(this instanceof L2MerchantInstance)
		{
			if(Config.LIST_PET_RENT_NPC.contains(npcId))
			{
				html.replace("_Quest", "_RentPet\">Rent Pet</a><br><a action=\"bypass -h npc_%objectId%_Quest");
			}
		}

		html.replace("%name%", getName());
		html.replace("%player_name%", player.getName());
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%festivalMins%", SevenSignsFestival.getInstance().getTimeToNextFestivalStart());
		player.sendPacket(html);

		player.sendPacket(ActionFailed.STATIC_PACKET);
		if ((player.isGM()) && (player.getAccessLevel().getLevel() == Config.GM_ACCESS_LEVEL))
			player.sendChatMessage(0, 0, "HTML", filename);
	}

	public void showChatWindow(L2PcInstance player, String filename)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);

		if(Config.PLAYER_MOVEMENT_BLOCK_TIME > 0) 
			player.updateNotMoveUntil();
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	public int getExpReward()
	{
		double rateXp = getStat().calcStat(Stats.MAX_HP, 1, this, null);
		return (int) (getTemplate().rewardExp * rateXp * Config.RATE_XP);
	}

	public int getSpReward()
	{
		double rateSp = getStat().calcStat(Stats.MAX_HP, 1, this, null);
		return (int) (getTemplate().rewardSp * rateSp * Config.RATE_SP);
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if(!super.doDie(killer))
		{
			return false;
		}

		_currentLHandId = getTemplate().lhand;
		_currentRHandId = getTemplate().rhand;
		_currentCollisionHeight = getTemplate().collisionHeight;
		_currentCollisionRadius = getTemplate().collisionRadius;
		DecayTaskManager.getInstance().addDecayTask(this);
		return true;
	}

	public void setSpawn(L2Spawn spawn)
	{
		_spawn = spawn;
		if(_spawn != null)
		{
			if(CustomNpcInstanceManager.getInstance().isThisL2CustomNpcInstance(_spawn.getId(), getNpcId()))
			{
				new L2CustomNpcInstance(this);
			}
		}
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();

	}

	@Override
	public void onDecay()
	{
		if(isDecayed())
		{
			return;
		}

		setDecayed(true);

		if(this instanceof L2ControlTowerInstance)
		{
			((L2ControlTowerInstance) this).onDeath();
		}

		super.onDecay();

		if(_spawn != null)
		{
			_spawn.decreaseCount(this);
		}
	}

	public void deleteMe()
	{
		if(getWorldRegion() != null)
		{
			getWorldRegion().removeFromZones(this);
		}

		try
		{
			decayMe();
		}
		catch(Throwable t)
		{
			_log.severe("deletedMe(): " + t);
		}

		try
		{
			getKnownList().removeAllKnownObjects();
		}
		catch(Throwable t)
		{
			_log.severe("deletedMe(): " + t);
		}

		L2World.getInstance().removeObject(this);
	}

	public L2Spawn getSpawn()
	{
		return _spawn;
	}

	@Override
	public String toString()
	{
		return getTemplate().name;
	}

	public boolean isDecayed()
	{
		return _isDecayed;
	}

	public void setDecayed(boolean decayed)
	{
		_isDecayed = decayed;
	}

	public void endDecayTask()
	{
		if(!isDecayed())
		{
			DecayTaskManager.getInstance().cancelDecayTask(this);
			onDecay();
		}
	}

	public boolean isMob()
	{
		return false;
	}

	public void setLHandId(int newWeaponId)
	{
		_currentLHandId = newWeaponId;
	}

	public void setRHandId(int newWeaponId)
	{
		_currentRHandId = newWeaponId;
	}

	public void setCollisionHeight(int height)
	{
		_currentCollisionHeight = height;
	}

	public void setCollisionRadius(int radius)
	{
		_currentCollisionRadius = radius;
	}

	public int getCollisionHeight()
	{
		return _currentCollisionHeight;
	}

	public int getCollisionRadius()
	{
		return _currentCollisionRadius;
	}

	public L2CustomNpcInstance getCustomNpcInstance()
	{
		return _customNpcInstance;
	}

	public void setCustomNpcInstance(L2CustomNpcInstance arg)
	{
		_customNpcInstance = arg;
	}
	
	private boolean _serverSideTitle = getTemplate().serverSideTitle;
	private boolean _serverSideName = getTemplate().serverSideName;

	public boolean getServerSideTitle()
	{
		return _serverSideTitle;
	}

	public void setServerSideTitle(boolean status)
	{
		_serverSideTitle = status;
	}

	public boolean getServerSideName()
	{
		return _serverSideName;
	}

	public void setServerSideName(boolean status)
	{
		_serverSideName = status;
	}

	@Override
	public int getMinShopDistance()
	{
		return Config.SHOP_MIN_RANGE_FROM_NPC;
	}

	public boolean hasSpoken()
	{
		return _hasSpoken;
	}
	
	/**
	 * Set the speak state of this L2Npc.
	 * @param hasSpoken boolean value.
	 */
	public void setHasSpoken(boolean hasSpoken)
	{
		_hasSpoken = hasSpoken;
	}

	public void broadcastNpcSay(String message)
	{
		broadcastPacket(new NpcSay(getObjectId(), Say2.ALL, getNpcId(), message));
	}
}