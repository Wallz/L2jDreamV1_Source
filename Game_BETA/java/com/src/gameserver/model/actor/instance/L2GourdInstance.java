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

import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.taskmanager.DecayTaskManager;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public final class L2GourdInstance extends L2MonsterInstance
{
	private String _name;
	private byte _nectar = 0;
	private byte _good = 0;

	public L2GourdInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		DecayTaskManager.getInstance().addDecayTask(this, 180000);
	}

	public void setOwner(String name)
	{
		_name = name;
	}

	public String getOwner()
	{
		return _name;
	}

	public void addNectar()
	{
		_nectar++;
	}

	public byte getNectar()
	{
		return _nectar;
	}

	public void addGood()
	{
		_good++;
	}

	public byte getGood()
	{
		return _good;
	}

	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake)
	{
		if(attacker.getName() != getOwner())
		{
			damage = 0;
		}

		if(getTemplate().npcId == 12778 || getTemplate().npcId == 12779)
		{
			if(attacker.getActiveWeaponInstance().getItemId() == 4202 || attacker.getActiveWeaponInstance().getItemId() == 5133 || attacker.getActiveWeaponInstance().getItemId() == 5817 || attacker.getActiveWeaponInstance().getItemId() == 7058)
			{
				super.reduceCurrentHp(damage, attacker, awake);
			}
			else if(damage > 0)
			{
				damage = 0;
			}
		}
		super.reduceCurrentHp(damage, attacker, awake);
	}

}