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
package com.src.gameserver.model.entity;

import java.util.Calendar;
import java.util.logging.Logger;

import javolution.util.FastList;

import com.src.Config;
import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.managers.DuelManager;
import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.olympiad.Olympiad;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.ExDuelEnd;
import com.src.gameserver.network.serverpackets.ExDuelReady;
import com.src.gameserver.network.serverpackets.ExDuelStart;
import com.src.gameserver.network.serverpackets.L2GameServerPacket;
import com.src.gameserver.network.serverpackets.PlaySound;
import com.src.gameserver.network.serverpackets.SocialAction;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.thread.ThreadPoolManager;

public class Duel
{
	protected static final Logger _log = Logger.getLogger(Duel.class.getName());

	public static final int DUELSTATE_NODUEL = 0;
	public static final int DUELSTATE_DUELLING = 1;
	public static final int DUELSTATE_DEAD = 2;
	public static final int DUELSTATE_WINNER = 3;
	public static final int DUELSTATE_INTERRUPTED = 4;

	private int _duelId;
	private L2PcInstance _playerA;
	private L2PcInstance _playerB;
	private boolean _partyDuel;
	private Calendar _duelEndTime;
	private int _surrenderRequest = 0;
	private int _countdown = 4;
	private boolean _finished = false;

	private FastList<PlayerCondition> _playerConditions;

	public static enum DuelResultEnum
	{
		Continue,
		Team1Win,
		Team2Win,
		Team1Surrender,
		Team2Surrender,
		Canceled,
		Timeout
	}

	public Duel(L2PcInstance playerA, L2PcInstance playerB, int partyDuel, int duelId)
	{
		_duelId = duelId;
		_playerA = playerA;
		_playerB = playerB;
		_partyDuel = partyDuel == 1 ? true : false;

		_duelEndTime = Calendar.getInstance();

		if(_partyDuel)
		{
			_duelEndTime.add(Calendar.SECOND, 300);
		}
		else
		{
			_duelEndTime.add(Calendar.SECOND, 120);
		}

		_playerConditions = new FastList<PlayerCondition>();

		setFinished(false);

		if(_partyDuel)
		{
			_countdown++;
			broadcastToTeam1(new SystemMessage(SystemMessageId.IN_A_MOMENT_YOU_WILL_BE_TRANSPORTED_TO_THE_SITE_WHERE_THE_DUEL_WILL_TAKE_PLACE));
			broadcastToTeam2(new SystemMessage(SystemMessageId.IN_A_MOMENT_YOU_WILL_BE_TRANSPORTED_TO_THE_SITE_WHERE_THE_DUEL_WILL_TAKE_PLACE));
		}

		ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleStartDuelTask(this), 3000);
	}

	public class PlayerCondition
	{
		private L2PcInstance _player;
		private double _hp;
		private double _mp;
		private double _cp;
		private boolean _paDuel;
		private int _x, _y, _z;
		private FastList<L2Effect> _debuffs;

		public PlayerCondition(L2PcInstance player, boolean partyDuel)
		{
			if(player == null)
			{
				return;
			}

			_player = player;
			_hp = _player.getCurrentHp();
			_mp = _player.getCurrentMp();
			_cp = _player.getCurrentCp();
			_paDuel = partyDuel;

			if(_paDuel)
			{
				_x = _player.getX();
				_y = _player.getY();
				_z = _player.getZ();
			}
		}

		public void restoreCondition()
		{
			if(_player == null)
			{
				return;
			}

			_player.setCurrentHp(_hp);
			_player.setCurrentMp(_mp);
			_player.setCurrentCp(_cp);

			if(_paDuel)
			{
				teleportBack();
			}

			if(_debuffs != null)
			{
				for(L2Effect temp : _debuffs)
				{
					if(temp != null)
					{
						temp.exit();
					}
				}
			}
		}

		public void registerDebuff(L2Effect debuff)
		{
			if(_debuffs == null)
			{
				_debuffs = new FastList<L2Effect>();
			}

			_debuffs.add(debuff);
		}

		public void teleportBack()
		{
			if(_paDuel)
			{
				_player.teleToLocation(_x, _y, _z);
			}
		}

		public L2PcInstance getPlayer()
		{
			return _player;
		}
	}

	public class ScheduleDuelTask implements Runnable
	{
		private Duel _duel;

		public ScheduleDuelTask(Duel duel)
		{
			_duel = duel;
		}

		@Override
		public void run()
		{
			try
			{
				DuelResultEnum status = _duel.checkEndDuelCondition();

				if(status == DuelResultEnum.Canceled)
				{
					setFinished(true);
					_duel.endDuel(status);
				}
				else if(status != DuelResultEnum.Continue)
				{
					setFinished(true);
					playKneelAnimation();
					ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleEndDuelTask(_duel, status), 5000);
				}
				else
				{
					ThreadPoolManager.getInstance().scheduleGeneral(this, 1000);
				}

				status = null;
			}
			catch(Throwable t)
			{
			}
		}
	}

	public class ScheduleStartDuelTask implements Runnable
	{
		private Duel _duel;

		public ScheduleStartDuelTask(Duel duel)
		{
			_duel = duel;
		}

		@Override
		public void run()
		{
			try
			{
				int count = _duel.countdown();

				if(count == 4)
				{
					_duel.teleportPlayers(Config.DUEL_SPAWN_X, Config.DUEL_SPAWN_Y, Config.DUEL_SPAWN_Z);

					ThreadPoolManager.getInstance().scheduleGeneral(this, 20000);
				}
				else if(count > 0)
				{
					ThreadPoolManager.getInstance().scheduleGeneral(this, 1000);
				}
				else
				{
					_duel.startDuel();
				}
			}
			catch(Throwable t)
			{
			}
		}
	}

	public class ScheduleEndDuelTask implements Runnable
	{
		private Duel _duel;
		private DuelResultEnum _result;

		public ScheduleEndDuelTask(Duel duel, DuelResultEnum result)
		{
			_duel = duel;
			_result = result;
		}

		@Override
		public void run()
		{
			try
			{
				_duel.endDuel(_result);
			}
			catch(Throwable t)
			{
			}
		}
	}

	private void stopFighting()
	{
		ActionFailed af = ActionFailed.STATIC_PACKET;
		L2Summon pet;
		if(_partyDuel)
		{
			for(L2PcInstance temp : _playerA.getParty().getPartyMembers())
			{
				temp.abortCast();
				temp.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				temp.setTarget(null);
				temp.sendPacket(af);
			
				// If it has a pet or summon do the same 
				if (temp.getPet() != null) 
				{ 
					pet = temp.getPet(); 
					pet.abortCast(); 
					pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE); 
					pet.setTarget(null); 
				}
				
			}
			
			for(L2PcInstance temp : _playerB.getParty().getPartyMembers())
			{
				temp.abortCast();
				temp.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				temp.setTarget(null);
				temp.sendPacket(af);
				
				// If it has a pet or summon do the same 
				if (temp.getPet() != null) 
				{ 
					pet = temp.getPet(); 
					pet.abortCast(); 
					pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE); 
					pet.setTarget(null); 
				} 
			} 
		} 
		else 
		{ 
			_playerA.abortCast();
			_playerA.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			_playerA.setTarget(null);
			_playerA.sendPacket(af); 
			
			// If it has a pet or summon do the same 
			if (_playerA.getPet() != null) 
			{ 
				pet = _playerA.getPet(); 
				pet.abortCast(); 
				pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE); 
				pet.setTarget(null); 
			} 
			
			_playerB.abortCast();
			_playerB.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			_playerB.setTarget(null);
			_playerB.sendPacket(af);
			
			// If it has a pet or summon do the same 
			if (_playerB.getPet() != null) 
			{ 
				pet = _playerB.getPet(); 
				pet.abortCast(); 
				pet.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE); 
				pet.setTarget(null); 
			}
		}
		
		af = null;
	}

	public boolean isDuelistInPvp(boolean sendMessage)
	{
		if(_partyDuel)
		{
			return false;
		}
		else if(_playerA.getPvpFlag() != 0 || _playerB.getPvpFlag() != 0)
		{
			if(sendMessage)
			{
				String engagedInPvP = "The duel was canceled because a duelist engaged in PvP combat.";
				_playerA.sendMessage(engagedInPvP);
				_playerB.sendMessage(engagedInPvP);
			}

			return true;
		}

		return false;
	}

	public void startDuel()
	{
		savePlayerConditions();

		if(_playerA == null || _playerB == null || _playerA.isInDuel() || _playerB.isInDuel() ||  Olympiad.getInstance().isRegisteredInComp(_playerA) ||  Olympiad.getInstance().isRegisteredInComp(_playerB) || Olympiad.getInstance().isRegistered(_playerA) || Olympiad.getInstance().isRegistered(_playerB))
		{
			_playerConditions.clear();
			_playerConditions = null;
			DuelManager.getInstance().removeDuel(this);
			return;
		}

		if(_partyDuel)
		{
			for(L2PcInstance temp : _playerA.getParty().getPartyMembers())
			{
				temp.cancelActiveTrade();
				temp.setIsInDuel(_duelId);
				temp.setTeam(1);
				temp.broadcastStatusUpdate();
				temp.broadcastUserInfo();
			}
			for(L2PcInstance temp : _playerB.getParty().getPartyMembers())
			{
				temp.cancelActiveTrade();
				temp.setIsInDuel(_duelId);
				temp.setTeam(2);
				temp.broadcastStatusUpdate();
				temp.broadcastUserInfo();
			}

			ExDuelReady ready = new ExDuelReady(1);
			ExDuelStart start = new ExDuelStart(1);

			broadcastToTeam1(ready);
			broadcastToTeam2(ready);
			broadcastToTeam1(start);
			broadcastToTeam2(start);

			ready = null;
			start = null;
		}
		else
		{
			_playerA.setIsInDuel(_duelId);
			_playerA.setTeam(1);
			_playerB.setIsInDuel(_duelId);
			_playerB.setTeam(2);

			ExDuelReady ready = new ExDuelReady(0);
			ExDuelStart start = new ExDuelStart(0);

			broadcastToTeam1(ready);
			broadcastToTeam2(ready);
			broadcastToTeam1(start);
			broadcastToTeam2(start);

			_playerA.broadcastStatusUpdate();
			_playerB.broadcastStatusUpdate();
			_playerA.broadcastUserInfo();
			_playerB.broadcastUserInfo();

			ready = null;
			start = null;
		}

		PlaySound ps = new PlaySound(1, "B04_S01", 0, 0, 0, 0, 0);
		broadcastToTeam1(ps);
		broadcastToTeam2(ps);

		ps = null;

		ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleDuelTask(this), 1000);
	}

	public void savePlayerConditions()
	{
		if(_partyDuel)
		{
			for(L2PcInstance temp : _playerA.getParty().getPartyMembers())
			{
				_playerConditions.add(new PlayerCondition(temp, _partyDuel));
			}

			for(L2PcInstance temp : _playerB.getParty().getPartyMembers())
			{
				_playerConditions.add(new PlayerCondition(temp, _partyDuel));
			}
		}
		else
		{
			_playerConditions.add(new PlayerCondition(_playerA, _partyDuel));
			_playerConditions.add(new PlayerCondition(_playerB, _partyDuel));
		}
	}

	public void restorePlayerConditions(boolean abnormalDuelEnd)
	{
		if(_partyDuel)
		{
			for(L2PcInstance temp : _playerA.getParty().getPartyMembers())
			{
				temp.setIsInDuel(0);
				temp.setTeam(0);
				temp.broadcastUserInfo();
			}

			for(L2PcInstance temp : _playerB.getParty().getPartyMembers())
			{
				temp.setIsInDuel(0);
				temp.setTeam(0);
				temp.broadcastUserInfo();
			}
		}
		else
		{
			_playerA.setIsInDuel(0);
			_playerA.setTeam(0);
			_playerA.broadcastUserInfo();
			_playerB.setIsInDuel(0);
			_playerB.setTeam(0);
			_playerB.broadcastUserInfo();
		}

		if(abnormalDuelEnd)
		{
			return;
		}

		for(FastList.Node<PlayerCondition> e = _playerConditions.head(), end = _playerConditions.tail(); (e = e.getNext()) != end;)
		{
			e.getValue().restoreCondition();
		}
	}

	public int getId()
	{
		return _duelId;
	}

	public int getRemainingTime()
	{
		return (int) (_duelEndTime.getTimeInMillis() - Calendar.getInstance().getTimeInMillis());
	}

	public L2PcInstance getPlayerA()
	{
		return _playerA;
	}

	public L2PcInstance getPlayerB()
	{
		return _playerB;
	}

	public boolean isPartyDuel()
	{
		return _partyDuel;
	}

	public void setFinished(boolean mode)
	{
		_finished = mode;
	}

	public boolean getFinished()
	{
		return _finished;
	}

	public void teleportPlayers(int x, int y, int z)
	{
		if(!_partyDuel)
		{
			return;
		}

		int offset = 0;

		for(L2PcInstance temp : _playerA.getParty().getPartyMembers())
		{
			temp.teleToLocation(x + offset - 180, y - 150, z);
			offset += 40;
		}

		offset = 0;

		for(L2PcInstance temp : _playerB.getParty().getPartyMembers())
		{
			temp.teleToLocation(x + offset - 180, y + 150, z);
			offset += 40;
		}
	}

	public void broadcastToTeam1(L2GameServerPacket packet)
	{
		if(_playerA == null)
		{
			return;
		}

		if(_partyDuel && _playerA.getParty() != null)
		{
			for(L2PcInstance temp : _playerA.getParty().getPartyMembers())
			{
				temp.sendPacket(packet);
			}
		}
		else
		{
			_playerA.sendPacket(packet);
		}
	}

	public void broadcastToTeam2(L2GameServerPacket packet)
	{
		if(_playerB == null)
		{
			return;
		}

		if(_partyDuel && _playerB.getParty() != null)
		{
			for(L2PcInstance temp : _playerB.getParty().getPartyMembers())
			{
				temp.sendPacket(packet);
			}
		}
		else
		{
			_playerB.sendPacket(packet);
		}
	}

	public L2PcInstance getWinner()
	{
		if(!getFinished() || _playerA == null || _playerB == null)
		{
			return null;
		}

		if(_playerA.getDuelState() == DUELSTATE_WINNER)
		{
			return _playerA;
		}

		if(_playerB.getDuelState() == DUELSTATE_WINNER)
		{
			return _playerB;
		}

		return null;
	}

	public L2PcInstance getLooser()
	{
		if(!getFinished() || _playerA == null || _playerB == null)
		{
			return null;
		}

		if(_playerA.getDuelState() == DUELSTATE_WINNER)
		{
			return _playerB;
		}

		else if(_playerA.getDuelState() == DUELSTATE_WINNER)
		{
			return _playerA;
		}

		return null;
	}

	public void playKneelAnimation()
	{
		L2PcInstance looser = getLooser();

		if(looser == null)
		{
			return;
		}

		if(_partyDuel && looser.getParty() != null)
		{
			for(L2PcInstance temp : looser.getParty().getPartyMembers())
			{
				temp.broadcastPacket(new SocialAction(temp.getObjectId(), 7));
			}
		}
		else
		{
			looser.broadcastPacket(new SocialAction(looser.getObjectId(), 7));
		}

		looser = null;
	}

	public int countdown()
	{
		_countdown--;

		if(_countdown > 3)
		{
			return _countdown;
		}

		if(_countdown > 0)
		{
			broadcastToTeam1(new SystemMessage(SystemMessageId.THE_DUEL_WILL_BEGIN_IN_S1_SECONDS).addNumber(_countdown));
			broadcastToTeam2(new SystemMessage(SystemMessageId.THE_DUEL_WILL_BEGIN_IN_S1_SECONDS).addNumber(_countdown));
		}
		else
		{
			broadcastToTeam1(new SystemMessage(SystemMessageId.LET_THE_DUEL_BEGIN));
			broadcastToTeam2(new SystemMessage(SystemMessageId.LET_THE_DUEL_BEGIN));
		}

		return _countdown;
	}

	public void endDuel(DuelResultEnum result)
	{
		if(_playerA == null || _playerB == null)
		{
			_playerConditions.clear();
			_playerConditions = null;
			DuelManager.getInstance().removeDuel(this);
			return;
		}

		switch(result)
		{
			case Team1Win:
				restorePlayerConditions(false);

				if(_partyDuel)
				{
					broadcastToTeam1(new SystemMessage(SystemMessageId.S1S_PARTY_HAS_WON_THE_DUEL).addString(_playerA.getName()));
					broadcastToTeam2(new SystemMessage(SystemMessageId.S1S_PARTY_HAS_WON_THE_DUEL).addString(_playerA.getName()));
				}
				else
				{
					broadcastToTeam1(new SystemMessage(SystemMessageId.S1_HAS_WON_THE_DUEL).addString(_playerA.getName()));
					broadcastToTeam2(new SystemMessage(SystemMessageId.S1_HAS_WON_THE_DUEL).addString(_playerA.getName()));
				}

				break;
			case Team2Win:
				restorePlayerConditions(false);
				if(_partyDuel)
				{
					broadcastToTeam1(new SystemMessage(SystemMessageId.S1S_PARTY_HAS_WON_THE_DUEL).addString(_playerB.getName()));
					broadcastToTeam2(new SystemMessage(SystemMessageId.S1S_PARTY_HAS_WON_THE_DUEL).addString(_playerB.getName()));
				}
				else
				{
					broadcastToTeam1(new SystemMessage(SystemMessageId.S1_HAS_WON_THE_DUEL).addString(_playerB.getName()));
					broadcastToTeam2(new SystemMessage(SystemMessageId.S1_HAS_WON_THE_DUEL).addString(_playerB.getName()));
				}

				break;
			case Team1Surrender:
				restorePlayerConditions(false);
				if(_partyDuel)
				{
					broadcastToTeam1(new SystemMessage(SystemMessageId.SINCE_S1S_PARTY_WITHDREW_FROM_THE_DUEL_S1S_PARTY_HAS_WON).addString(_playerA.getName()).addString(_playerB.getName()));
					broadcastToTeam2(new SystemMessage(SystemMessageId.SINCE_S1S_PARTY_WITHDREW_FROM_THE_DUEL_S1S_PARTY_HAS_WON).addString(_playerA.getName()).addString(_playerB.getName()));
				}
				else
				{
					broadcastToTeam1(new SystemMessage(SystemMessageId.SINCE_S1_WITHDREW_FROM_THE_DUEL_S2_HAS_WON).addString(_playerA.getName()).addString(_playerB.getName()));
					broadcastToTeam2(new SystemMessage(SystemMessageId.SINCE_S1_WITHDREW_FROM_THE_DUEL_S2_HAS_WON).addString(_playerA.getName()).addString(_playerB.getName()));
				}

				break;
			case Team2Surrender:
				restorePlayerConditions(false);
				if(_partyDuel)
				{
					broadcastToTeam1(new SystemMessage(SystemMessageId.SINCE_S1S_PARTY_WITHDREW_FROM_THE_DUEL_S1S_PARTY_HAS_WON).addString(_playerB.getName()).addString(_playerA.getName()));
					broadcastToTeam2(new SystemMessage(SystemMessageId.SINCE_S1S_PARTY_WITHDREW_FROM_THE_DUEL_S1S_PARTY_HAS_WON).addString(_playerB.getName()).addString(_playerA.getName()));
				}
				else
				{
					broadcastToTeam1(new SystemMessage(SystemMessageId.SINCE_S1_WITHDREW_FROM_THE_DUEL_S2_HAS_WON).addString(_playerB.getName()).addString(_playerA.getName()));
					broadcastToTeam2(new SystemMessage(SystemMessageId.SINCE_S1_WITHDREW_FROM_THE_DUEL_S2_HAS_WON).addString(_playerB.getName()).addString(_playerA.getName()));
				}

				break;
			case Canceled:
				stopFighting();

				restorePlayerConditions(true);

				broadcastToTeam1(new SystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE));
				broadcastToTeam2(new SystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE));

				break;
			case Timeout:
				stopFighting();

				restorePlayerConditions(false);

				broadcastToTeam1(new SystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE));
				broadcastToTeam2(new SystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE));

				break;
		default:
			break;
		}

		if(_partyDuel)
		{
			broadcastToTeam1(new ExDuelEnd(1));
			broadcastToTeam2(new ExDuelEnd(1));
		}
		else
		{
			broadcastToTeam1(new ExDuelEnd(0));
			broadcastToTeam2(new ExDuelEnd(0));
		}

		_playerConditions.clear();
		_playerConditions = null;
		DuelManager.getInstance().removeDuel(this);
	}

	public DuelResultEnum checkEndDuelCondition()
	{
		if(_playerA == null || _playerB == null)
		{
			return DuelResultEnum.Canceled;
		}

		if(_surrenderRequest != 0)
		{
			if(_surrenderRequest == 1)
			{
				return DuelResultEnum.Team1Surrender;
			}
			else
			{
				return DuelResultEnum.Team2Surrender;
			}
		}
		else if(getRemainingTime() <= 0)
		{
			return DuelResultEnum.Timeout;
		}
		else if(_playerA.getDuelState() == DUELSTATE_WINNER)
		{
			stopFighting();
			return DuelResultEnum.Team1Win;
		}
		else if(_playerB.getDuelState() == DUELSTATE_WINNER)
		{
			stopFighting();
			return DuelResultEnum.Team2Win;
		}
		else if(!_partyDuel)
		{
			if(_playerA.getDuelState() == DUELSTATE_INTERRUPTED || _playerB.getDuelState() == DUELSTATE_INTERRUPTED)
			{
				return DuelResultEnum.Canceled;
			}

			if(!_playerA.isInsideRadius(_playerB, 1600, false, false))
			{
				return DuelResultEnum.Canceled;
			}

			if(isDuelistInPvp(true))
			{
				return DuelResultEnum.Canceled;
			}

			if(_playerA.isInsideZone(L2Character.ZONE_PEACE) || _playerB.isInsideZone(L2Character.ZONE_PEACE) || _playerA.isInsideZone(L2Character.ZONE_SIEGE) || _playerB.isInsideZone(L2Character.ZONE_SIEGE) || _playerA.isInsideZone(L2Character.ZONE_PVP) || _playerB.isInsideZone(L2Character.ZONE_PVP))
			{
				return DuelResultEnum.Canceled;
			}
		}

		return DuelResultEnum.Continue;
	}

	public void doSurrender(L2PcInstance player)
	{
		if(_surrenderRequest != 0)
		{
			return;
		}

		stopFighting();

		if(_partyDuel)
		{
			if(_playerA.getParty().getPartyMembers().contains(player))
			{
				_surrenderRequest = 1;

				for(L2PcInstance temp : _playerA.getParty().getPartyMembers())
				{
					temp.setDuelState(DUELSTATE_DEAD);
				}

				for(L2PcInstance temp : _playerB.getParty().getPartyMembers())
				{
					temp.setDuelState(DUELSTATE_WINNER);
				}
			}
			else if(_playerB.getParty().getPartyMembers().contains(player))
			{
				_surrenderRequest = 2;

				for(L2PcInstance temp : _playerB.getParty().getPartyMembers())
				{
					temp.setDuelState(DUELSTATE_DEAD);
				}

				for(L2PcInstance temp : _playerA.getParty().getPartyMembers())
				{
					temp.setDuelState(DUELSTATE_WINNER);
				}

			}
		}
		else
		{
			if(player == _playerA)
			{
				_surrenderRequest = 1;
				_playerA.setDuelState(DUELSTATE_DEAD);
				_playerB.setDuelState(DUELSTATE_WINNER);
			}
			else if(player == _playerB)
			{
				_surrenderRequest = 2;
				_playerB.setDuelState(DUELSTATE_DEAD);
				_playerA.setDuelState(DUELSTATE_WINNER);
			}
		}
	}

	public void onPlayerDefeat(L2PcInstance player)
	{
		player.setDuelState(DUELSTATE_DEAD);

		if(_partyDuel)
		{
			boolean teamdefeated = true;

			for(L2PcInstance temp : player.getParty().getPartyMembers())
			{
				if(temp.getDuelState() == DUELSTATE_DUELLING)
				{
					teamdefeated = false;
					break;
				}
			}

			if(teamdefeated)
			{
				L2PcInstance winner = _playerA;

				if(_playerA.getParty().getPartyMembers().contains(player))
				{
					winner = _playerB;
				}

				for(L2PcInstance temp : winner.getParty().getPartyMembers())
				{
					temp.setDuelState(DUELSTATE_WINNER);
				}

				winner = null;
			}
		}
		else
		{
			if(player != _playerA && player != _playerB)
			{
				_log.warning("Error in onPlayerDefeat(): player is not part of this 1vs1 duel");
			}

			if(_playerA == player)
			{
				_playerB.setDuelState(DUELSTATE_WINNER);
			}
			else
			{
				_playerA.setDuelState(DUELSTATE_WINNER);
			}
		}
	}

	public void onRemoveFromParty(L2PcInstance player)
	{
		if(!_partyDuel)
		{
			return;
		}

		if(player == _playerA || player == _playerB)
		{
			for(FastList.Node<PlayerCondition> e = _playerConditions.head(), end = _playerConditions.tail(); (e = e.getNext()) != end;)
			{
				e.getValue().teleportBack();
				e.getValue().getPlayer().setIsInDuel(0);
			}

			_playerA = null;
			_playerB = null;
		}
		else
		{
			for(FastList.Node<PlayerCondition> e = _playerConditions.head(), end = _playerConditions.tail(); (e = e.getNext()) != end;)
			{
				if(e.getValue().getPlayer() == player)
				{
					e.getValue().teleportBack();
					_playerConditions.remove(e.getValue());
					break;
				}
			}
			player.setIsInDuel(0);
		}
	}

	public void onBuff(L2PcInstance player, L2Effect debuff)
	{
		for(FastList.Node<PlayerCondition> e = _playerConditions.head(), end = _playerConditions.tail(); (e = e.getNext()) != end;)
		{
			if(e.getValue().getPlayer() == player)
			{
				e.getValue().registerDebuff(debuff);
				return;
			}
		}
	}

}
