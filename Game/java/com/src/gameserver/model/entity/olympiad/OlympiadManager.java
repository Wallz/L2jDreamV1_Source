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

import java.util.Map;

import javolution.util.FastMap;

import com.src.Config;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.util.object.L2FastList;
import com.src.util.random.Rnd;

class OlympiadManager extends Olympiad implements Runnable
{
	private Map<Integer, L2OlympiadGame> _olympiadInstances;

	public OlympiadManager()
	{
		_olympiadInstances = new FastMap<Integer, L2OlympiadGame>();
		_manager = this;
	}

	protected static final OlympiadStadia[] STADIUMS = {
		new OlympiadStadia(-20814, -21189, -3030),
		new OlympiadStadia(-120324, -225077, -3331),
		new OlympiadStadia(-102495, -209023, -3331),
		new OlympiadStadia(-120156, -207378, -3331),
		new OlympiadStadia(-87628, -225021, -3331),
		new OlympiadStadia(-81705, -213209, -3331),
		new OlympiadStadia(-87593, -207339, -3331),
		new OlympiadStadia(-93709, -218304, -3331),
		new OlympiadStadia(-77157, -218608, -3331),
		new OlympiadStadia(-69682, -209027, -3331),
		new OlympiadStadia(-76887, -201256, -3331),
		new OlympiadStadia(-109985, -218701, -3331),
		new OlympiadStadia(-126367, -218228, -3331),
		new OlympiadStadia(-109629, -201292, -3331),
		new OlympiadStadia(-87523, -240169, -3331),
		new OlympiadStadia(-81748, -245950, -3331),
		new OlympiadStadia(-77123, -251473, -3331),
		new OlympiadStadia(-69778, -241801, -3331),
		new OlympiadStadia(-76754, -234014, -3331),
		new OlympiadStadia(-93742, -251032, -3331),
		new OlympiadStadia(-87466, -257752, -3331),
		new OlympiadStadia(-114413, -213241, -3331)};
	
	@Override
	public synchronized void run()
	{
		_cycleTerminated = false;
		if(isOlympiadEnd())
		{
			_scheduledManagerTask.cancel(true);
			_cycleTerminated = true;
			return;
		}
		Map<Integer, OlympiadGameTask> _gamesQueue = new FastMap<Integer, OlympiadGameTask>();
		while(inCompPeriod())
		{
			if(_nobles.size() == 0)
			{
				try
				{
					wait(60000);
				}
				catch(InterruptedException ex)
				{
				}
				continue;
			}

			boolean classBasedCanStart = false;
			for(L2FastList<L2PcInstance> classList : _classBasedRegisters.values())
			{
				if(classList.size() >= Config.OLY_CLASSED)
				{
					classBasedCanStart = true;
					break;
				}
			}
			while(((_gamesQueue.size() > 0) || (classBasedCanStart) || (_nonClassBasedRegisters.size() >= Config.OLY_NONCLASSED)) && (inCompPeriod()))
			{
				int _gamesQueueSize = 0;
				_gamesQueueSize = _gamesQueue.size();
				for(int i = 0; i < _gamesQueueSize; i++)
				{
					if(_gamesQueue.get(i) == null || _gamesQueue.get(i).isTerminated() || _gamesQueue.get(i).getGame() == null)
					{
						if(_gamesQueue.containsKey(i))
						{
							try
							{
								_olympiadInstances.remove(i);
								_gamesQueue.remove(i);
								STADIUMS[i].setStadiaFree();
							}
							catch(Exception e)
							{
								_log.error("", e);
							}
						}
						else
						{
							_gamesQueueSize = _gamesQueueSize + 1;
						}
					}
					else if(_gamesQueue.get(i) != null && !_gamesQueue.get(i).isStarted())
					{
						Thread T = new Thread(_gamesQueue.get(i));
						T.start();
					}
				}
				for(int i = 0; i < STADIUMS.length; i++)
				{
					if(!existNextOpponents(_nonClassBasedRegisters) && !existNextOpponents(getRandomClassList(_classBasedRegisters)))
					{
						break;
					}

					if(STADIUMS[i].isFreeToUse())
					{
						if(existNextOpponents(_nonClassBasedRegisters))
						{
							try
							{
								_olympiadInstances.put(i, new L2OlympiadGame(i, OlympiadType.NON_CLASSED, nextOpponents(_nonClassBasedRegisters), STADIUMS[i].getCoordinates()));
								_gamesQueue.put(i, new OlympiadGameTask(_olympiadInstances.get(i)));
								STADIUMS[i].setStadiaBusy();
							}
							catch(Exception ex)
							{
								if(_olympiadInstances.get(i) != null)
								{
									for(L2PcInstance player : _olympiadInstances.get(i).getPlayers())
									{
										player.sendMessage("Your olympiad registration was canceled due to an error.");
										player.setIsInOlympiadMode(false);
										player.setIsOlympiadStart(false);
										player.setOlympiadSide(-1);
										player.setOlympiadGameId(-1);
									}
									_olympiadInstances.remove(i);
								}

								if(_gamesQueue.get(i) != null)
								{
									_gamesQueue.remove(i);
								}
								STADIUMS[i].setStadiaFree();

								i--;
							}
						}
						else if(existNextOpponents(getRandomClassList(_classBasedRegisters)))
						{
							try
							{
								_olympiadInstances.put(i, new L2OlympiadGame(i, OlympiadType.CLASSED, nextOpponents(getRandomClassList(_classBasedRegisters)), STADIUMS[i].getCoordinates()));
								_gamesQueue.put(i, new OlympiadGameTask(_olympiadInstances.get(i)));
								STADIUMS[i].setStadiaBusy();
							}
							catch(Exception ex)
							{
								if(_olympiadInstances.get(i) != null)
								{
									for(L2PcInstance player : _olympiadInstances.get(i).getPlayers())
									{
										player.sendMessage("Your olympiad registration was canceled due to an error.");
										player.setIsInOlympiadMode(false);
										player.setIsOlympiadStart(false);
										player.setOlympiadSide(-1);
										player.setOlympiadGameId(-1);
									}
									_olympiadInstances.remove(i);
								}

								if(_gamesQueue.get(i) != null)
								{
									_gamesQueue.remove(i);
								}
								STADIUMS[i].setStadiaFree();

								i--;
							}
						}
					}
				}
				try
				{
					wait(30000);
				}
				catch(InterruptedException e)
				{
				}
			}
			try
			{
				wait(30000);
			}
			catch(InterruptedException e)
			{
			}
		}

		boolean allGamesTerminated = false;
		while(!allGamesTerminated)
		{
			try
			{
				wait(30000);
			}
			catch(InterruptedException e)
			{
			}
			if(_gamesQueue.size() == 0)
			{
				allGamesTerminated = true;
			}
			else
			{
				for(OlympiadGameTask game : _gamesQueue.values())
				{
					allGamesTerminated = allGamesTerminated || game.isTerminated();
				}
			}
		}
		_cycleTerminated = true;
		_gamesQueue.clear();
		_olympiadInstances.clear();
		_classBasedRegisters.clear();
		_nonClassBasedRegisters.clear();

		_battleStarted = false;
	}

	protected L2OlympiadGame getOlympiadInstance(int index)
	{
		if(_olympiadInstances != null && _olympiadInstances.size() > 0)
		{
			return _olympiadInstances.get(index);
		}

		return null;
	}

	@Override
	public Map<Integer, L2OlympiadGame> getOlympiadGames()
	{
		return _olympiadInstances == null ? null : _olympiadInstances;
	}

	private L2FastList<L2PcInstance> getRandomClassList(Map<Integer, L2FastList<L2PcInstance>> list)
	{
		if(list.size() == 0)
		{
			return null;
		}

		Map<Integer, L2FastList<L2PcInstance>> tmp = new FastMap<Integer, L2FastList<L2PcInstance>>();
		int tmpIndex = 0;
		for(L2FastList<L2PcInstance> l : list.values())
		{
			if(list.size() >= Config.OLY_CLASSED)
			{
				tmp.put(tmpIndex, l);
				tmpIndex++;
			}
		}

		L2FastList<L2PcInstance> rndList = new L2FastList<L2PcInstance>();
		int classIndex = 0;
		if(tmp.size() == 1)
		{
			classIndex = 0;
		}
		else
		{
			classIndex = Rnd.nextInt(tmp.size());
		}

		rndList = tmp.get(classIndex);
		return rndList;
	}

	private L2FastList<L2PcInstance> nextOpponents(L2FastList<L2PcInstance> list)
	{
		L2FastList<L2PcInstance> opponents = new L2FastList<L2PcInstance>();
		if(list.size() == 0)
		{
			return opponents;
		}
		int loopCount = list.size() / 2;

		int first;
		int second;

		if(loopCount < 1)
		{
			return opponents;
		}

		first = Rnd.nextInt(list.size());
		opponents.add(list.get(first));
		list.remove(first);

		second = Rnd.nextInt(list.size());
		opponents.add(list.get(second));
		list.remove(second);

		return opponents;
	}

	private boolean existNextOpponents(L2FastList<L2PcInstance> list)
	{
		if(list == null)
		{
			return false;
		}

		if(list.size() == 0)
		{
			return false;
		}

		int loopCount = list.size() >> 1;

		if(loopCount < 1)
		{
			return false;
		}
		else
		{
			return true;
		}
	}

	protected FastMap<Integer, String> getAllTitles()
	{
		FastMap<Integer, String> titles = new FastMap<Integer, String>();
		
		for (L2OlympiadGame instance : _olympiadInstances.values())
		{
			if (instance._gamestarted != true)
				continue;

			titles.put(instance._stadiumID, instance.getTitle());
		}
		
		return titles;
	}

}