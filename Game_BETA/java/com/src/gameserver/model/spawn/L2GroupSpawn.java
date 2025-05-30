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
package com.src.gameserver.model.spawn;

import java.lang.reflect.Constructor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.datatables.xml.TerritoryTable;
import com.src.gameserver.idfactory.IdFactory;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.util.random.Rnd;

public class L2GroupSpawn extends L2Spawn
{
	private static final Log _log = LogFactory.getLog(L2GroupSpawn.class);

	private Constructor<?> _constructor;
	private L2NpcTemplate _template;

	public L2GroupSpawn(L2NpcTemplate mobTemplate) throws SecurityException, ClassNotFoundException, NoSuchMethodException
	{
		super(mobTemplate);
		_constructor = Class.forName("com.src.gameserver.model.actor.instance.L2ControllableMobInstance").getConstructors()[0];
		_template = mobTemplate;

		setAmount(1);
	}

	public L2Npc doGroupSpawn()
	{
		L2Npc mob = null;

		try
		{
			if(_template.type.equalsIgnoreCase("L2Pet") || _template.type.equalsIgnoreCase("L2Minion"))
			{
				return null;
			}

			Object[] parameters =
			{
					IdFactory.getInstance().getNextId(), _template
			};
			Object tmp = _constructor.newInstance(parameters);

			if(!(tmp instanceof L2Npc))
			{
				return null;
			}

			mob = (L2Npc) tmp;

			int newlocx, newlocy, newlocz;

			if(getLocx() == 0 && getLocy() == 0)
			{
				if(getLocation() == 0)
				{
					return null;
				}

				int p[] = TerritoryTable.getInstance().getRandomPoint(getLocation());
				newlocx = p[0];
				newlocy = p[1];
				newlocz = p[2];
			}
			else
			{
				newlocx = getLocx();
				newlocy = getLocy();
				newlocz = getLocz();
			}

			mob.setCurrentHpMp(mob.getMaxHp(), mob.getMaxMp());

			if(getHeading() == -1)
			{
				mob.setHeading(Rnd.nextInt(61794));
			}
			else
			{
				mob.setHeading(getHeading());
			}

			mob.setSpawn(this);
			mob.spawnMe(newlocx, newlocy, newlocz);
			mob.onSpawn();

			return mob;

		}
		catch(Exception e)
		{
			_log.error("NPC class not found", e);
			return null;
		}
	}
}