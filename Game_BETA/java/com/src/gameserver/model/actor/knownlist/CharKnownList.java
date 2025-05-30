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
package com.src.gameserver.model.actor.knownlist;

import java.util.Collection;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2MonsterInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.util.Util;

public class CharKnownList extends ObjectKnownList
{
	private Map<Integer, L2PcInstance> _knownPlayers;
	private Map<Integer, Integer> _knownRelations;

	public CharKnownList(L2Character activeChar)
	{
		super(activeChar);
	}

	@Override
	public boolean addKnownObject(L2Object object)
	{
		return addKnownObject(object, null);
	}

	@Override
	public boolean addKnownObject(L2Object object, L2Character dropper)
	{
		if(!super.addKnownObject(object, dropper))
		{
			return false;
		}

		if(object instanceof L2PcInstance)
		{
			getKnownPlayers().put(object.getObjectId(), (L2PcInstance) object);
			getKnownRelations().put(object.getObjectId(), -1);
		}

		return true;
	}

	public final boolean knowsThePlayer(L2PcInstance player)
	{
		return getActiveChar() == player || getKnownPlayers().containsKey(player.getObjectId());
	}

	@Override
	public final void removeAllKnownObjects()
	{
		super.removeAllKnownObjects();
		getKnownPlayers().clear();
		getKnownRelations().clear();

		getActiveChar().setTarget(null);

		if(getActiveChar().hasAI())
		{
			getActiveChar().setAI(null);
		}
	}

	@Override
	public boolean removeKnownObject(L2Object object)
	{
		if(!super.removeKnownObject(object))
		{
			return false;
		}

		if(object instanceof L2PcInstance)
		{
			getKnownPlayers().remove(object.getObjectId());
			getKnownRelations().remove(object.getObjectId());
		}

		if(object == getActiveChar().getTarget())
		{
			getActiveChar().setTarget(null);
		}

		return true;
	}

	public L2Character getActiveChar()
	{
		return (L2Character) super.getActiveObject();
	}

	@Override
	public int getDistanceToForgetObject(L2Object object)
	{
		return 0;
	}

	@Override
	public int getDistanceToWatchObject(L2Object object)
	{
		return 0;
	}

	public Collection<L2Character> getKnownCharacters()
	{
		FastList<L2Character> result = new FastList<L2Character>();

		for(L2Object obj : getKnownObjects().values())
		{
			if(obj != null && obj instanceof L2Character)
			{
				result.add((L2Character) obj);
			}
		}

		return result;
	}

	public Collection<L2Character> getKnownCharactersInRadius(long radius)
	{
		FastList<L2Character> result = new FastList<L2Character>();

		for(L2Object obj : getKnownObjects().values())
		{
			if(obj instanceof L2PcInstance)
			{
				if(Util.checkIfInRange((int) radius, getActiveChar(), obj, true))
				{
					result.add((L2PcInstance) obj);
				}
			}
			else if(obj instanceof L2MonsterInstance)
			{
				if(Util.checkIfInRange((int) radius, getActiveChar(), obj, true))
				{
					result.add((L2MonsterInstance) obj);
				}
			}
			else if(obj instanceof L2Npc)
			{
				if(Util.checkIfInRange((int) radius, getActiveChar(), obj, true))
				{
					result.add((L2Npc) obj);
				}
			}
		}

		return result;
	}

	public final Map<Integer, L2PcInstance> getKnownPlayers()
	{
		if(_knownPlayers == null)
		{
			_knownPlayers = new FastMap<Integer, L2PcInstance>().shared();
		}

		return _knownPlayers;
	}

	public final Map<Integer, Integer> getKnownRelations()
	{
		if(_knownRelations == null)
		{
			_knownRelations = new FastMap<Integer, Integer>().shared();
		}

		return _knownRelations;
	}

	public final Collection<L2PcInstance> getKnownPlayersInRadius(long radius)
	{
		FastList<L2PcInstance> result = new FastList<L2PcInstance>();

		for(L2PcInstance player : getKnownPlayers().values())
			if(Util.checkIfInRange((int) radius, getActiveChar(), player, true))
			{
				result.add(player);
			}

		return result;
	}

}