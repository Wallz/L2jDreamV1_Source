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
package com.src.gameserver.model;

import java.util.Iterator;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import javolution.util.FastList;

import com.src.gameserver.ai.L2AttackableAI;
import com.src.gameserver.ai.L2SiegeGuardAI;
import com.src.gameserver.datatables.sql.SpawnTable;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.gameserver.model.zone.L2ZoneManager;
import com.src.gameserver.model.zone.L2ZoneType;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.object.L2ObjectSet;

public final class L2WorldRegion
{
	private static Logger _log = Logger.getLogger(L2WorldRegion.class.getName());

	private L2ObjectSet<L2Playable> _allPlayable;

	private L2ObjectSet<L2Object> _visibleObjects;

	private FastList<L2WorldRegion> _surroundingRegions;
	private int _tileX, _tileY;
	private Boolean _active = false;
	private ScheduledFuture<?> _neighborsTask = null;

	private L2ZoneManager _zoneManager;

	public L2WorldRegion(int pTileX, int pTileY)
	{
		_allPlayable = L2ObjectSet.createL2PlayerSet();
		_visibleObjects = L2ObjectSet.createL2ObjectSet();
		_surroundingRegions = new FastList<L2WorldRegion>();

		_tileX = pTileX;
		_tileY = pTileY;
		_active = false;
	}

	public void addZone(L2ZoneType zone)
	{
		if(_zoneManager == null)
		{
			_zoneManager = new L2ZoneManager();
		}
		_zoneManager.registerNewZone(zone);
	}

	public void removeZone(L2ZoneType zone)
	{
		if(_zoneManager == null)
		{
			return;
		}

		_zoneManager.unregisterZone(zone);
	}

	public void revalidateZones(L2Character character)
	{
		if(_zoneManager == null)
		{
			return;
		}

		if(_zoneManager != null)
		{
			_zoneManager.revalidateZones(character);
		}
	}

	public void removeFromZones(L2Character character)
	{
		if(_zoneManager == null)
		{
			return;
		}

		if(_zoneManager != null)
		{
			_zoneManager.removeCharacter(character);
		}
	}

	public void onDeath(L2Character character)
	{
		if(_zoneManager == null)
		{
			return;
		}

		if(_zoneManager != null)
		{
			_zoneManager.onDeath(character);
		}
	}

	public void onRevive(L2Character character)
	{
		if(_zoneManager == null)
		{
			return;
		}

		if(_zoneManager != null)
		{
			_zoneManager.onRevive(character);
		}
	}

	public class NeighborsTask implements Runnable
	{
		private boolean _isActivating;

		public NeighborsTask(boolean isActivating)
		{
			_isActivating = isActivating;
		}

		@Override
		public void run()
		{
			if(_isActivating)
			{
				for(L2WorldRegion neighbor : getSurroundingRegions())
				{
					neighbor.setActive(true);
				}
			}
			else
			{
				if(areNeighborsEmpty())
				{
					setActive(false);
				}

				for(L2WorldRegion neighbor : getSurroundingRegions())
				{
					if(neighbor.areNeighborsEmpty())
					{
						neighbor.setActive(false);
					}
				}
			}
		}
	}

	private void switchAI(Boolean isOn)
	{
		int c = 0;

		if(!isOn)
		{
			for(L2Object o : _visibleObjects)
			{
				if(o instanceof L2Attackable)
				{
					c++;
					L2Attackable mob = (L2Attackable) o;

					// Set target to null and cancel Attack or Cast
					mob.setTarget(null);

					// Stop movement
					mob.stopMove(null);

					// Stop all active skills effects in progress on the L2Character
					mob.stopAllEffects();

					mob.clearAggroList();
					mob.getKnownList().removeAllKnownObjects();

					if(mob.getAI()!=null){
						
						mob.getAI().setIntention(com.src.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE);

						// stop the ai tasks
						if(mob.getAI() instanceof L2AttackableAI)
							((L2AttackableAI) mob.getAI()).stopAITask();
						else if(mob.getAI() instanceof L2SiegeGuardAI)
							((L2SiegeGuardAI) mob.getAI()).stopAITask();

					}
				}
			}
			_log.fine(c + " mobs were turned off");
		}
		else
		{
			for(L2Object o : _visibleObjects)
			{
				if(o instanceof L2Attackable)
				{
					c++;
					((L2Attackable) o).getStatus().startHpMpRegeneration();
				}
				else if(o instanceof L2Npc)
				{
					((L2Npc) o).startRandomAnimationTimer();
				}
			}
			_log.fine(c + " mobs were turned on");
		}
	}

	public Boolean isActive()
	{
		return _active;
	}

	public Boolean areNeighborsEmpty()
	{
		if(isActive() && _allPlayable.size() > 0)
		{
			return false;
		}

		for(L2WorldRegion neighbor : _surroundingRegions)
		{
			if(neighbor.isActive() && neighbor._allPlayable.size() > 0)
			{
				return false;
			}
		}
		return true;
	}

	public void setActive(boolean value)
	{
		if(_active == value)
		{
			return;
		}

		_active = value;

		switchAI(value);

		if(value)
		{
			_log.fine("Starting Grid " + _tileX + "," + _tileY);
		}
		else
		{
			_log.fine("Stoping Grid " + _tileX + "," + _tileY);
		}
	}

	private void startActivation()
	{
		setActive(true);

		if(_neighborsTask != null)
		{
			_neighborsTask.cancel(true);
			_neighborsTask = null;
		}

		_neighborsTask = ThreadPoolManager.getInstance().scheduleGeneral(new NeighborsTask(true), 1000);
	}

	private void startDeactivation()
	{
		if(_neighborsTask != null)
		{
			_neighborsTask.cancel(true);
			_neighborsTask = null;
		}

		_neighborsTask = ThreadPoolManager.getInstance().scheduleGeneral(new NeighborsTask(false), 1000);
	}

	public void addVisibleObject(L2Object object)
	{
		if(object == null)
		{
			return;
		}

		_visibleObjects.put(object);

		if(object instanceof L2Playable)
		{
			_allPlayable.put((L2Playable) object);

			if(_allPlayable.size() == 1)
			{
				startActivation();
			}
		}
	}

	public void removeVisibleObject(L2Object object)
	{
		if(object == null)
		{
			return;
		}

		_visibleObjects.remove(object);

		if(object instanceof L2Playable)
		{
			_allPlayable.remove((L2Playable) object);

			if(_allPlayable.size() == 0)
			{
				startDeactivation();
			}
		}
	}

	public void addSurroundingRegion(L2WorldRegion region)
	{
		_surroundingRegions.add(region);
	}

	public FastList<L2WorldRegion> getSurroundingRegions()
	{
		return _surroundingRegions;
	}

	public Iterator<L2Playable> iterateAllPlayers()
	{
		return _allPlayable.iterator();
	}

	public L2ObjectSet<L2Object> getVisibleObjects()
	{
		return _visibleObjects;
	}

	public String getName()
	{
		return "(" + _tileX + ", " + _tileY + ")";
	}

	public synchronized void deleteVisibleNpcSpawns()
	{
		_log.fine("Deleting all visible NPC's in Region: " + getName());
		for(L2Object obj : _visibleObjects)
		{
			if(obj instanceof L2Npc)
			{
				L2Npc target = (L2Npc) obj;
				target.deleteMe();
				L2Spawn spawn = target.getSpawn();

				if(spawn != null)
				{
					spawn.stopRespawn();
					SpawnTable.getInstance().deleteSpawn(spawn, false);
				}

				_log.finest("Removed NPC " + target.getObjectId());
			}
		}
		_log.info("All visible NPC's deleted in Region: " + getName());
	}
}