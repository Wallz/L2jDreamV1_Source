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
package com.src.gameserver.model.entity.olympiad;

import com.src.Config;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ExOlympiadUserInfo;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.StatsSet;

class OlympiadGameTask extends Olympiad implements Runnable
{
	private final L2OlympiadGame _game;
	private SystemMessage _sm;
	private SystemMessage _sm2;

	private boolean _terminated = false;

	public boolean isTerminated()
	{
		return _terminated;
	}

	private boolean _started = false;

	public boolean isStarted()
	{
		return _started;
	}

	public OlympiadGameTask(L2OlympiadGame game)
	{
		_game = game;
	}

	protected L2OlympiadGame getGame()
	{
		return _game;
	}

	protected boolean checkBattleStatus()
	{
		boolean _pOneCrash = _game._playerOne == null || _game._playerOneDisconnected;
		boolean _pTwoCrash = _game._playerTwo == null || _game._playerTwoDisconnected;
		if(_pOneCrash || _pTwoCrash || _game._aborted)
		{
			return false;
		}

		return true;
	}

	protected boolean checkStatus()
	{
		boolean _pOneCrash = _game._playerOne == null || _game._playerOneDisconnected;
		boolean _pTwoCrash = _game._playerTwo == null || _game._playerTwoDisconnected;

		StatsSet playerOneStat;
		StatsSet playerTwoStat;

		playerOneStat = _nobles.get(_game._playerOneID);
		playerTwoStat = _nobles.get(_game._playerTwoID);

		int playerOnePlayed = playerOneStat.getInteger(COMP_DONE);
		int playerTwoPlayed = playerTwoStat.getInteger(COMP_DONE);

		if(Config.OLY_SAME_IP && _game._playerOne.getClient().getConnection().getInetAddress().getHostAddress().equals(_game._playerTwo.getClient().getConnection().getInetAddress().getHostAddress()) || _game._aborted)
		{
			_game._playerOne.sendChatMessage(0, 0, "SYS", "Match interrupted because of both parties have the same IP-address.");
			_game._playerTwo.sendChatMessage(0, 0, "SYS", "Match interrupted because of both parties have the same IP-address.");

			_terminated = true;
			_game._gamestarted = false;
			_game.PlayersStatusBack();
			try
			{
				_game.portPlayersBack();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			return false;
		}
		else if(_pOneCrash || _pTwoCrash || _game._aborted)
		{
			if(_pOneCrash && !_pTwoCrash)
			{
				try
				{
					int playerOnePoints = playerOneStat.getInteger(POINTS);
					int playerTwoPoints = playerTwoStat.getInteger(POINTS);
					int transferPoints;
					if(playerTwoPoints > playerOnePoints)
					{
						transferPoints = playerOnePoints / 5;
					}
					else
					{
						transferPoints = playerTwoPoints / 5;
					}
					playerOneStat.set(POINTS, playerOnePoints - transferPoints);
					if(Config.OLYMPIAD_GAME_LOG)
					{
						_log.info("Olympia Result: " + _game._playerOneName + " vs " + _game._playerTwoName + " ... " + _game._playerOneName + " lost " + transferPoints + " points for crash");
					}

					playerTwoStat.set(POINTS, playerTwoPoints + transferPoints);
					if(Config.OLYMPIAD_GAME_LOG)
					{
						_log.info("Olympia Result: " + _game._playerOneName + " vs " + _game._playerTwoName + " ... " + _game._playerTwoName + " Win " + transferPoints + " points");
					}

					_sm = new SystemMessage(SystemMessageId.S1_HAS_WON_THE_GAME);
					_sm2 = new SystemMessage(SystemMessageId.S1_HAS_GAINED_S2_OLYMPIAD_POINTS);
					_sm.addString(_game._playerTwoName);
					broadcastMessage(_sm, true);
					_sm2.addString(_game._playerTwoName);
					_sm2.addNumber(transferPoints);
					broadcastMessage(_sm2, true);
				}
				catch(Exception e)
				{
					_log.error("", e);
				}
			}

			if(_pTwoCrash && !_pOneCrash)
			{
				try
				{
					int playerTwoPoints = playerTwoStat.getInteger(POINTS);
					int playerOnePoints = playerOneStat.getInteger(POINTS);
					int transferPoints;
					if(playerOnePoints > playerTwoPoints)
					{
						transferPoints = playerTwoPoints / 5;
					}
					else
					{
						transferPoints = playerOnePoints / 5;
					}
					playerTwoStat.set(POINTS, playerTwoPoints - transferPoints);
					if(Config.OLYMPIAD_GAME_LOG)
					{
						_log.info("Olympia Result: " + _game._playerTwoName + " vs " + _game._playerOneName + " ... " + _game._playerTwoName + " lost " + transferPoints + " points for crash");
					}

					playerOneStat.set(POINTS, playerOnePoints + transferPoints);
					if(Config.OLYMPIAD_GAME_LOG)
					{
						_log.info("Olympia Result: " + _game._playerTwoName + " vs " + _game._playerOneName + " ... " + _game._playerOneName + " Win " + transferPoints + " points");
					}

					_sm = new SystemMessage(SystemMessageId.S1_HAS_WON_THE_GAME);
					_sm2 = new SystemMessage(SystemMessageId.S1_HAS_GAINED_S2_OLYMPIAD_POINTS);
					_sm.addString(_game._playerOneName);
					broadcastMessage(_sm, true);
					_sm2.addString(_game._playerOneName);
					_sm2.addNumber(transferPoints);
					broadcastMessage(_sm2, true);
				}
				catch(Exception e)
				{
					_log.error("", e);
				}
			}
			playerOneStat.set(COMP_DONE, playerOnePlayed + 1);
			playerTwoStat.set(COMP_DONE, playerTwoPlayed + 1);

			_terminated = true;
			_game._gamestarted = false;
			_game.PlayersStatusBack();
			try
			{
				_game.portPlayersBack();
			}
			catch(Exception e)
			{
				_log.error("", e);
			}

			return false;
		}

		return true;
	}

	@Override
	public void run()
	{
		_started = true;
		if(_game != null)
		{
			if(_game._playerOne != null && _game._playerTwo != null)
			{
				for(int i = 45; i > 10; i -= 5)
				{
					switch(i)
					{
						case 45:
						case 30:
						case 15:
							_game.sendMessageToPlayers(false, i);
							break;
					}
					try
					{
						Thread.sleep(5000);
					}
					catch(InterruptedException e)
					{
					}
					if(!checkStatus())
					{
						return;
					}
				}
				for(int i = 5; i > 0; i--)
				{
					_game.sendMessageToPlayers(false, i);
					try
					{
						Thread.sleep(1000);
					}
					catch(InterruptedException e)
					{
					}
				}

				if(!checkStatus())
				{
					return;
				}

				_game.portPlayersToArena();
				_game.removals();
	            if (Config.OLY_ANNOUNCE_GAMES)
	            	 _game.announceGame();
				try
				{
					Thread.sleep(5000);
				}
				catch(InterruptedException e)
				{
				}
				_game.removals();

				synchronized (this)
				{
					if(!_battleStarted)
					{
						_battleStarted = true;
					}
				}
				for(int i = 60; i > 10; i -= 10)
				{
					_game.sendMessageToPlayers(true, i);
					try
					{
						Thread.sleep(10000);
					}
					catch(InterruptedException e)
					{
					}
					if(i == 20)
					{
						_game.sendMessageToPlayers(true, 10);
						try
						{
							Thread.sleep(5000);
						}
						catch(InterruptedException e)
						{
						}
					}
				}
				_game.additions();
				for(int i = 5; i > 0; i--)
				{
					_game.sendMessageToPlayers(true, i);
					try
					{
						Thread.sleep(1000);
					}
					catch(InterruptedException e)
					{
					}
				}

				if(!checkStatus())
				{
					return;
				}

				_game._playerOne.sendPacket(new ExOlympiadUserInfo(_game._playerTwo, 1));
				_game._playerTwo.sendPacket(new ExOlympiadUserInfo(_game._playerOne, 1));
				if(_game._spectators != null)
				{
					for(L2PcInstance spec : _game.getSpectators())
					{
						try
						{
							spec.sendPacket(new ExOlympiadUserInfo(_game._playerTwo, 2));
							spec.sendPacket(new ExOlympiadUserInfo(_game._playerOne, 1));
						}
						catch(NullPointerException e)
						{
						}
					}
				}
				_game.makeCompetitionStart();

				for(int i = 0; i < BATTLE_PERIOD; i += 5000)
				{
					try
					{
						Thread.sleep(5000);
						if(_game.haveWinner() || !checkBattleStatus())
						{
							break;
						}
					}
					catch(InterruptedException e)
					{
					}
				}

				if(!checkStatus())
				{
					return;
				}
				_terminated = true;
				_game._gamestarted = false;
				try
				{
					_game.validateWinner();
					_game.portPlayersBack();
					_game.PlayersStatusBack();
					_game.removals();
				}
				catch(Exception e)
				{
					_log.error("", e);
				}

				saveNobleData();
			}
		}
	}

	private void broadcastMessage(SystemMessage sm, boolean toAll)
	{
		try
		{
			_game._playerOne.sendPacket(sm);
			_game._playerTwo.sendPacket(sm);
		}
		catch(Exception e)
		{
		}

		if(toAll && _game._spectators != null)
		{
			for(L2PcInstance spec : _game._spectators)
			{
				try
				{
					spec.sendPacket(sm);
				}
				catch(NullPointerException e)
				{
				}
			}
		}
	}

}