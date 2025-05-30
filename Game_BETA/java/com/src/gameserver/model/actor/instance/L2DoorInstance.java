/*
OO * This program is free software; you can redistribute it and/or modify
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

import java.util.Collection;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.ai.L2CharacterAI;
import com.src.gameserver.ai.L2DoorAI;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.L2Territory;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.knownlist.DoorKnownList;
import com.src.gameserver.model.actor.position.L2CharPosition;
import com.src.gameserver.model.actor.stat.DoorStat;
import com.src.gameserver.model.actor.status.DoorStatus;
import com.src.gameserver.model.entity.ClanHall;
import com.src.gameserver.model.entity.clanhallsiege.DevastatedCastle;
import com.src.gameserver.model.entity.siege.Castle;
import com.src.gameserver.network.L2GameClient;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.ConfirmDlg;
import com.src.gameserver.network.serverpackets.DoorInfo;
import com.src.gameserver.network.serverpackets.DoorStatusUpdate;
import com.src.gameserver.network.serverpackets.MyTargetSelected;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.network.serverpackets.ValidateLocation;
import com.src.gameserver.templates.chars.L2CharTemplate;
import com.src.gameserver.templates.item.L2Weapon;
import com.src.gameserver.thread.ThreadPoolManager;

public class L2DoorInstance extends L2Character
{
	protected static final Logger log = Logger.getLogger(L2DoorInstance.class.getName());

	private int _castleIndex = -2;
	private int _mapRegion = -1;
	private Castle _castle;
	private int _rangeXMin = 0;
	private int _rangeYMin = 0;
	private int _rangeZMin = 0;
	private int _rangeXMax = 0;
	private int _rangeYMax = 0;
	private int _rangeZMax = 0;

	private int _A = 0;
	private int _B = 0;
	private int _C = 0;
	private int _D = 0;

	protected final int _doorId;
	protected final String _name;
	private boolean _open;
	private boolean _unlockable;
	private boolean _isAttackableDoor = false; 

	private boolean _isWall = false; // False by default
	private int _upgradeHpRatio = 1;

	private ClanHall _clanHall;

	protected int _autoActionDelay = -1;
	private ScheduledFuture<?> _autoActionTask;

	public final L2Territory pos;

	public class AIAccessor extends L2Character.AIAccessor
	{
		protected AIAccessor()
		{
		}

		@Override
		public L2DoorInstance getActor()
		{
			return L2DoorInstance.this;
		}

		@Override
		public void moveTo(int x, int y, int z, int offset)
		{
		}

		@Override
		public void moveTo(int x, int y, int z)
		{
		}

		@Override
		public void stopMove(L2CharPosition pos)
		{
		}

		@Override
		public void doAttack(L2Character target)
		{
		}

		@Override
		public void doCast(L2Skill skill)
		{
		}
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
					_ai = new L2DoorAI(new AIAccessor());
				}
			}
		}

		return _ai;
	}

	@Override
	public boolean hasAI()
	{
		return _ai != null;
	}

	class CloseTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				onClose();
			}
			catch(Throwable e)
			{
				log.log(Level.SEVERE, "", e);
			}
		}
	}

	class AutoOpenClose implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				if(!getOpen())
				{
					openMe();
				}
				else
				{
					closeMe();
				}
			}
			catch(Exception e)
			{
				log.warning("Could not auto open/close door ID " + _doorId + " (" + _name + ")");
			}
		}
	}

	public L2DoorInstance(int objectId, L2CharTemplate template, int doorId, String name, boolean unlockable)
	{
		super(objectId, template);
		getKnownList();
		getStat();
		getStatus();
		_doorId = doorId;
		_name = name;
		_unlockable = unlockable;
		pos = new L2Territory();
	}

	@Override
	public final DoorKnownList getKnownList()
	{
		if(super.getKnownList() == null || !(super.getKnownList() instanceof DoorKnownList))
		{
			setKnownList(new DoorKnownList(this));
		}

		return (DoorKnownList) super.getKnownList();
	}

	@Override
	public final DoorStat getStat()
	{
		if(super.getStat() == null || !(super.getStat() instanceof DoorStat))
		{
			setStat(new DoorStat(this));
		}

		return (DoorStat) super.getStat();
	}

	@Override
	public final DoorStatus getStatus()
	{
		if(super.getStatus() == null || !(super.getStatus() instanceof DoorStatus))
		{
			setStatus(new DoorStatus(this));
		}

		return (DoorStatus) super.getStatus();
	}

	public final boolean isUnlockable()
	{
		return _unlockable;
	}

	@Override
	public final int getLevel()
	{
		return 1;
	}

	public int getDoorId()
	{
		return _doorId;
	}

	public boolean isOpened()
	{
		return _open;
	}
	
	public boolean getOpen()
	{
		return _open;
	}

	public void setOpen(boolean open)
	{
		_open = open;
	}

	public boolean getIsAttackableDoor() 
	{ 
		return _isAttackableDoor; 
	} 
	
	public void setIsAttackableDoor(boolean val) 
	{ 
		_isAttackableDoor = val; 
	}
 	
	public void setAutoActionDelay(int actionDelay)
	{
		if(_autoActionDelay == actionDelay)
		{
			return;
		}

		if(actionDelay > -1)
		{
			AutoOpenClose ao = new AutoOpenClose();
			ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(ao, actionDelay, actionDelay);
			ao = null;
		}
		else
		{
			if(_autoActionTask != null)
			{
				_autoActionTask.cancel(false);
			}
		}

		_autoActionDelay = actionDelay;
	}

	public int getDamage()
	{
		int dmg = 6 - (int) Math.ceil(getCurrentHp() / getMaxHp() * 6);
		if(dmg > 6)
		{
			return 6;
		}
		if(dmg < 0)
		{
			return 0;
		}
		return dmg;
	}

	public final Castle getCastle()
	{
		if (_castle == null) 
		{ 
			Castle castle = null; 
			
			if (_castleIndex < 0)  
			{ 
				castle = CastleManager.getInstance().getCastle(this); 
				if (castle != null) 
					_castleIndex = castle.getCastleId(); 
			} 
			if (_castleIndex > 0)  
				castle = CastleManager.getInstance().getCastleById(_castleIndex); 
			_castle = castle; 
		} 
		return _castle;
	}

	public void setClanHall(ClanHall clanhall)
	{
		_clanHall = clanhall;
	}

	public ClanHall getClanHall()
	{
		return _clanHall;
	}

	public boolean isEnemy()
	{ 
		if (getCastle() != null && getCastle().getCastleId() > 0 && getCastle().getSiege().getIsInProgress()) 
			return true; 
		return false;
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		if(isUnlockable())
		{
			return true;
		}

		if(attacker == null || !(attacker instanceof L2Playable))
		{
			return false;
		}

		if (getClanHall() != null) 
			return false;

		L2PcInstance activePlayer; 
		if(attacker instanceof L2Summon) 
			activePlayer = ((L2Summon)attacker).getOwner(); 
		else 
			activePlayer = (L2PcInstance)attacker;
		
		boolean isCastle = getCastle() != null && getCastle().getCastleId() > 0 && getCastle().getSiege().getIsInProgress();

		if (isCastle)
		{
			if (attacker instanceof L2SummonInstance)
			{
				L2Clan clan = activePlayer.getClan();
				if (clan != null && clan.getClanId() == getCastle().getOwnerId())
					return false;
			}
			else if (attacker instanceof L2PcInstance)
			{
				L2Clan clan = ((L2PcInstance)attacker).getClan();
				if (clan != null && clan.getClanId() == getCastle().getOwnerId())
					return false;
			}
		}

		return isCastle || DevastatedCastle.getInstance().getIsInProgress();
	}

	public boolean isAttackable(L2Character attacker)
	{
		return isAutoAttackable(attacker);
	}

	@Override
	public void updateAbnormalEffect()
	{
	}

	public int getDistanceToWatchObject(L2Object object)
	{
		if(!(object instanceof L2PcInstance))
		{
			return 0;
		}

		return 3000;
	}

	public int getDistanceToForgetObject(L2Object object)
	{
		if(!(object instanceof L2PcInstance))
		{
			return 0;
		}

		return 4000;
	}

	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getActiveWeaponItem()
	{
		return null;
	}

	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		return null;
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if(player == null)
		{
			return;
		}

		if(this != player.getTarget())
		{
			player.setTarget(this);

			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
			my = null;
			
			if(isAutoAttackable(player)) 
			{ 
				DoorInfo su = new DoorInfo(this, true);
				player.sendPacket(new DoorStatusUpdate(this));
				player.sendPacket(su);
			}
			else
			{
				DoorInfo su = new DoorInfo(this, false);
				player.sendPacket(su);
			}
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			if(isAutoAttackable(player))
			{
				if(Math.abs(player.getZ() - getZ()) < 400)
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
				}
			}
			else if(player.getClan() != null && getClanHall() != null && player.getClanId() == getClanHall().getOwnerId())
			{
				if(!isInsideRadius(player, L2Npc.INTERACTION_DISTANCE, false, false))
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				}
				else
				{
					player.gatesRequest(this);
					if(!getOpen())
						player.sendPacket(new ConfirmDlg(1140));
					else
						player.sendPacket(new ConfirmDlg(1141));
				}
			}
		}

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public void onActionShift(L2GameClient client)
	{
		L2PcInstance player = client.getActiveChar();
		if(player == null)
			return;

		if(player.getAccessLevel().isGm())
		{
			player.setTarget(this);
			player.sendPacket(new MyTargetSelected(getObjectId(), 0)); 

			if(isAutoAttackable(player)) 
			{ 
				player.sendPacket(new DoorStatusUpdate(this)); 
			}
			
			DoorInfo su = new DoorInfo(this, (getCastle() != null));
			player.sendPacket(su);
			
			NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile("data/html/admin/info/doorinfo.htm");

			html.replace("%class%", getClass().getSimpleName());
			html.replace("%hp%", String.valueOf((int) getCurrentHp()));
			html.replace("%hpmax%", String.valueOf(getMaxHp()));
			html.replace("%objid%", String.valueOf(getObjectId()));
			html.replace("%doorid%", String.valueOf(getDoorId()));

			html.replace("%minx%", String.valueOf(getXMin()));
			html.replace("%miny%", String.valueOf(getYMin()));
			html.replace("%minz%", String.valueOf(getZMin()));

			html.replace("%maxx%", String.valueOf(getXMax()));
			html.replace("%maxy%", String.valueOf(getYMax()));
			html.replace("%maxz%", String.valueOf(getZMax()));
			html.replace("%unlock%", isUnlockable() ? "<font color=00FF00>YES<font>" : "<font color=FF0000>NO</font>");

			player.sendPacket(html);
		}

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public void broadcastStatusUpdate()
	{
		Collection<L2PcInstance> knownPlayers = getKnownList().getKnownPlayers().values();

		if(knownPlayers == null || knownPlayers.isEmpty())
		{
			return;
		}

		DoorInfo su = new DoorInfo(this, (getCastle() != null));
		DoorStatusUpdate dsu = new DoorStatusUpdate(this);

		for(L2PcInstance player : knownPlayers)
		{
			if ((getCastle() != null && getCastle().getCastleId() > 0))
				su = new DoorInfo(this, true);
			
			player.sendPacket(su);
			player.sendPacket(dsu);
		}
	}

	public void onOpen()
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new CloseTask(), 60000);
	}

	public void onClose()
	{
		closeMe();
	}

	public final void closeMe()
	{
		synchronized (this)
		{
			if(!getOpen())
			{
				return;
			}

			setOpen(false);
		}

		broadcastStatusUpdate();
	}

	public final void openMe()
	{
		synchronized (this)
		{
			if(getOpen())
			{
				return;
			}

			setOpen(true);
		}

		broadcastStatusUpdate();
	}

	@Override
	public String toString()
	{
		return "door " + _doorId;
	}

	public String getDoorName()
	{
		return _name;
	}

	public int getXMin()
	{
		return _rangeXMin;
	}

	public int getYMin()
	{
		return _rangeYMin;
	}

	public int getZMin()
	{
		return _rangeZMin;
	}

	public int getXMax()
	{
		return _rangeXMax;
	}

	public int getYMax()
	{
		return _rangeYMax;
	}

	public int getZMax()
	{
		return _rangeZMax;
	}

	public void setRange(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax)
	{
		_rangeXMin = xMin;
		_rangeYMin = yMin;
		_rangeZMin = zMin;

		_rangeXMax = xMax;
		_rangeYMax = yMax;
		_rangeZMax = zMax;

		_A = _rangeYMax * (_rangeZMax - _rangeZMin) + _rangeYMin * (_rangeZMin - _rangeZMax);
		_B = _rangeZMin * (_rangeXMax - _rangeXMin) + _rangeZMax * (_rangeXMin - _rangeXMax);
		_C = _rangeXMin * (_rangeYMax - _rangeYMin) + _rangeXMin * (_rangeYMin - _rangeYMax);
		_D = -1 * (_rangeXMin * (_rangeYMax * _rangeZMax - _rangeYMin * _rangeZMax) + _rangeXMax * (_rangeYMin * _rangeZMin - _rangeYMin * _rangeZMax)
				+ _rangeXMin * (_rangeYMin * _rangeZMax - _rangeYMax * _rangeZMin));
	}

	public int getA()
	{
		return _A;
	}

	public int getB()
	{
		return _B;
	}

	public int getC()
	{
		return _C;
	}

	public int getD()
	{
		return _D;
	}

	public int getMapRegion()
	{
		return _mapRegion;
	}

	public void setMapRegion(int region)
	{
		_mapRegion = region;
	}

	public void setIsWall(boolean isWall)
	{
		_isWall = isWall;
	}
	
	public boolean isWall()
	{
		return _isWall;
	}
	
	@Override
	public int getMaxHp()
	{
		return super.getMaxHp() * _upgradeHpRatio;
	}
	
	public void setUpgradeHpRatio(int hpRatio)
	{
		_upgradeHpRatio = hpRatio;
	}
	
	public Collection<L2SiegeGuardInstance> getKnownSiegeGuards()
	{
		FastList<L2SiegeGuardInstance> result = new FastList<L2SiegeGuardInstance>();

		for(L2Object obj : getKnownList().getKnownObjects().values())
		{
			if(obj instanceof L2SiegeGuardInstance)
			{
				result.add((L2SiegeGuardInstance) obj);
			}
		}

		return result;
	}

	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake)
	{
		if(this.isAutoAttackable(attacker) || (attacker instanceof L2PcInstance && ((L2PcInstance) attacker).isGM()))
		{
			super.reduceCurrentHp(damage, attacker, awake);
		}
		else
		{
			super.reduceCurrentHp(0, attacker, awake);
		}
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if(!super.doDie(killer))
		{
			return false;
		}

		boolean isCastle = (getCastle() != null && getCastle().getCastleId() > 0 && getCastle().getSiege().getIsInProgress());

		if(isCastle)
		{
			broadcastPacket(SystemMessage.getSystemMessage((isWall()) ? SystemMessageId.CASTLE_WALL_DAMAGED : SystemMessageId.CASTLE_GATE_BROKEN_DOWN));
		}

		return true;
	}

}