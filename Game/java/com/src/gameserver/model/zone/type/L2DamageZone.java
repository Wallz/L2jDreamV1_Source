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
package com.src.gameserver.model.zone.type;

import java.util.Collection;
import java.util.concurrent.Future;

import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.siege.Castle;
import com.src.gameserver.model.zone.L2ZoneType;
import com.src.gameserver.network.serverpackets.EtcStatusUpdate;
import com.src.gameserver.thread.ThreadPoolManager;

public class L2DamageZone extends L2ZoneType
{
	private int _damageHpPerSec;
	private Future<?> _task;

	private int _castleId;
	private Castle _castle;
	
	private int _startTask;
	private int _reuseTask;
	
	public L2DamageZone(int id)
	{
		super(id);
		_damageHpPerSec = 100;
		setTargetType("L2Playable"); // default only playable
		
		// Setup default start / reuse time
		_startTask = 10;
		_reuseTask = 5000;
		
		// no castle by default
		_castleId = 0;
		_castle = null;
	}

	@Override
	public void setParameter(String name, String value)
	{
		if(name.equals("dmgSec"))
		{
			_damageHpPerSec = Integer.parseInt(value);
		}
		else if (name.equals("castleId"))
			_castleId = Integer.parseInt(value);
		else if (name.equalsIgnoreCase("initialDelay"))
			_startTask = Integer.parseInt(value);
		else if (name.equalsIgnoreCase("reuse"))
			_reuseTask = Integer.parseInt(value);
		else
		{
			super.setParameter(name, value);
		}
	}

	protected void stopTask()
	{
		if (_task != null)
		{
			_task.cancel(false);
			_task = null;
		}
	}
	
	@Override
	protected void onEnter(L2Character character)
	{
		
		L2PcInstance player = character.getActingPlayer();
		
		// Castle zone, siege and no defender
		if (getCastle() != null)
			if (!(getCastle().getSiege().getIsInProgress() && player != null && player.getSiegeState() != 2))
				return;
		
		synchronized(this) 
		{
			if(_task == null)
			{
				_task = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new ApplyDamage(this), _startTask, _reuseTask);
			}
		}
		
		if (character instanceof L2PcInstance)
		{
			character.setInsideZone(L2Character.ZONE_DANGERAREA, true);
			character.sendPacket(new EtcStatusUpdate((L2PcInstance) character));
		}
	}

	@Override
	protected void onExit(L2Character character)
	{
		if(_characterList.isEmpty())
		{
			_task.cancel(true);
			_task = null;
		}
		
		if (character instanceof L2PcInstance)
		{
			character.setInsideZone(L2Character.ZONE_DANGERAREA, false);
			if (!character.isInsideZone(L2Character.ZONE_DANGERAREA))
				character.sendPacket(new EtcStatusUpdate((L2PcInstance) character));
		}
	}

	protected Collection<L2Character> getCharacterList()
	{
		return _characterList.values();
	}

	protected Castle getCastle()
	{
		if (_castleId > 0 && _castle == null)
			_castle = CastleManager.getInstance().getCastleById(_castleId);
		
		return _castle;
	}
	
	class ApplyDamage implements Runnable
	{
		private L2DamageZone _dmgZone;
		private final Castle _castleZone;

		ApplyDamage(L2DamageZone zone)
		{
			_dmgZone = zone;
			_castleZone = zone.getCastle();
		}

		@Override
		public void run()
		{
			boolean siege = false;
			
			if (_castleZone != null)
			{
				// castle zones active only during siege
				siege = _castleZone.getSiege().getIsInProgress();
				if (!siege)
				{
					_dmgZone.stopTask();
					return;
				}
			}
			
			for(L2Character temp : _dmgZone.getCharacterList())
			{
				if(temp != null && !temp.isDead())
				{
					if (temp instanceof L2Attackable)
						continue;
					
					if (siege)
					{
						// during siege defenders not affected
						final L2PcInstance player = temp.getActingPlayer();
						if (player != null && player.getSiegeState() == 2)
							continue;
					}
					
					if (getHPDamagePerSecond() != 0)
						temp.reduceCurrentHp(_dmgZone.getHPDamagePerSecond(), null);
				}
			}
		}
	}
	
	@Override
	protected void onDieInside(L2Character character)
	{
		
	}

	@Override
	protected void onReviveInside(L2Character character)
	{
		
	}
	
	protected int getHPDamagePerSecond()
	{
		return _damageHpPerSec;
	}
}