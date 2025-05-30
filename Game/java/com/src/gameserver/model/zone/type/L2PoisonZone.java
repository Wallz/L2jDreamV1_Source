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

import java.util.concurrent.Future;

import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2MonsterInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.zone.L2ZoneType;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.random.Rnd;

public class L2PoisonZone extends L2ZoneType
{
	private int _skillId;
	private int _chance;
	private int _initialDelay;
	private int _skillLvl;
	private int _reuse;
	private boolean _enabled;
	private String _target;
	private Future<?> _task;

	public L2PoisonZone(int id)
	{
		super(id);
		_skillId = 4070;
		_skillLvl = 1;
		_chance = 100;
		_initialDelay = 0;
		_reuse = 30000;
		_enabled = true;
		_target = "pc";
	}

	@Override
	public void setParameter(String name, String value)
	{
		if(name.equals("skillId"))
		{
			_skillId = Integer.parseInt(value);
		}
		else if(name.equals("skillLvl"))
		{
			_skillLvl = Integer.parseInt(value);
		}
		else if(name.equals("chance"))
		{
			_chance = Integer.parseInt(value);
		}
		else if(name.equals("initialDelay"))
		{
			_initialDelay = Integer.parseInt(value);
		}
		else if(name.equals("default_enabled"))
		{
			_enabled = Boolean.parseBoolean(value);
		}
		else if(name.equals("target"))
		{
			_target = String.valueOf(value);
		}
		else if(name.equals("reuse"))
		{
			_reuse = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}

	@Override
	protected void onEnter(L2Character character)
	{
		if((character instanceof L2Playable && _target.equalsIgnoreCase("pc") || character instanceof L2PcInstance && _target.equalsIgnoreCase("pc_only") || character instanceof L2MonsterInstance && _target.equalsIgnoreCase("npc")) && _task == null)
		{
			_task = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new ApplySkill(/*this*/), _initialDelay, _reuse);
		}
	}

	@Override
	protected void onExit(L2Character character)
	{
		if(_characterList.isEmpty() && _task != null)
		{
			_task.cancel(true);
			_task = null;
		}
	}

	public L2Skill getSkill()
	{
		return SkillTable.getInstance().getInfo(_skillId, _skillLvl);
	}

	public String getTargetType()
	{
		return _target;
	}

	public boolean isEnabled()
	{
		return _enabled;
	}

	public int getChance()
	{
		return _chance;
	}

	public void setZoneEnabled(boolean val)
	{
		_enabled = val;
	}

	public class ApplySkill implements Runnable
	{
		@Override
		public void run()
		{
			if(isEnabled())
			{
				for(L2Character temp : _characterList.values())
				{
					if(temp != null && !temp.isDead())
					{
						if((temp instanceof L2Playable && getTargetType().equalsIgnoreCase("pc") || temp instanceof L2PcInstance && getTargetType().equalsIgnoreCase("pc_only") || temp instanceof L2MonsterInstance && getTargetType().equalsIgnoreCase("npc")) && Rnd.get(100) < getChance())
						{
							getSkill().getEffects(temp, temp);
						}
					}
				}
			}
		}
	}

	@Override
	public void onDieInside(L2Character l2character)
	{
		
	}

	@Override
	public void onReviveInside(L2Character l2character)
	{
		
	}
}