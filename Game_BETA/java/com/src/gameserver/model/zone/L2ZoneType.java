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
package com.src.gameserver.model.zone;

import javolution.util.FastMap;

import org.w3c.dom.Node;

import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.L2GameServerPacket;

public abstract class L2ZoneType
{
	private final int _id;
	protected L2ZoneForm _zone;
	protected FastMap<Integer, L2Character> _characterList;

	private boolean _checkAffected;

	private int _minLvl;
	private int _maxLvl;
	private int[] _race;
	private int[] _class;
	private char _classType;
	private String _target = "L2Character"; // default all chars

	protected L2ZoneType(int id)
	{
		_id = id;
		_characterList = new FastMap<Integer, L2Character>().shared();

		_checkAffected = false;

		_minLvl = 0;
		_maxLvl = 0xFF;

		_classType = 0;

		_race = null;
		_class = null;
	}

	public int getId()
	{
		return _id;
	}

	public void setParameter(String name, String value)
	{
		_checkAffected = true;

		if(name.equals("affectedLvlMin"))
		{
			_minLvl = Integer.parseInt(value);
		}
		else if(name.equals("affectedLvlMax"))
		{
			_maxLvl = Integer.parseInt(value);
		}
		else if(name.equals("affectedRace"))
		{
			if(_race == null)
			{
				_race = new int[1];
				_race[0] = Integer.parseInt(value);
			}
			else
			{
				int[] temp = new int[_race.length + 1];

				int i = 0;

				for(; i < _race.length; i++)
				{
					temp[i] = _race[i];
				}

				temp[i] = Integer.parseInt(value);

				_race = temp;
			}
		}
		else if(name.equals("affectedClassId"))
		{
			if(_class == null)
			{
				_class = new int[1];
				_class[0] = Integer.parseInt(value);
			}
			else
			{
				int[] temp = new int[_class.length + 1];

				int i = 0;

				for(; i < _class.length; i++)
				{
					temp[i] = _class[i];
				}

				temp[i] = Integer.parseInt(value);

				_class = temp;
			}
		}
		else if(name.equals("affectedClassType"))
		{
			if(value.equals("Fighter"))
			{
				_classType = 1;
			}
			else
			{
				_classType = 2;
			}
		}
		else if (name.equals("targetClass"))
		{
			_target = value;
		}
	}

	public void setSpawnLocs(Node node1)
	{
	}

	private boolean isAffected(L2Character character)
	{
		if(character.getLevel() < _minLvl || character.getLevel() > _maxLvl)
		{
			return false;
		}

		// check obj class
		try
		{
			if (!(Class.forName("com.src.gameserver.model.actor." + _target).isInstance(character)))
				return false;
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
				
		if(character instanceof L2PcInstance)
		{
			if(_classType != 0)
			{
				if(((L2PcInstance) character).isMageClass())
				{
					if(_classType == 1)
					{
						return false;
					}
				}
				else if(_classType == 2)
				{
					return false;
				}
			}

			if(_race != null)
			{
				boolean ok = false;

				for(int element : _race)
				{
					if(((L2PcInstance) character).getRace().ordinal() == element)
					{
						ok = true;
						break;
					}
				}

				if(!ok)
				{
					return false;
				}
			}

			if(_class != null)
			{
				boolean ok = false;

				for(int clas : _class)
				{
					if(((L2PcInstance) character).getClassId().ordinal() == clas)
					{
						ok = true;
						break;
					}
				}

				if(!ok)
				{
					return false;
				}
			}
		}

		return true;
	}

	public void setZone(L2ZoneForm zone)
	{
		_zone = zone;
	}

	public L2ZoneForm getZone()
	{
		return _zone;
	}

	public boolean isInsideZone(int x, int y)
	{
		return _zone.isInsideZone(x, y, _zone.getHighZ());
	}

	public boolean isInsideZone(int x, int y, int z)
	{
		return _zone.isInsideZone(x, y, z);
	}

	public boolean isInsideZone(L2Object object)
	{
		return _zone.isInsideZone(object.getX(), object.getY(), object.getZ());
	}

	public double getDistanceToZone(int x, int y)
	{
		return _zone.getDistanceToZone(x, y);
	}

	public double getDistanceToZone(L2Object object)
	{
		return _zone.getDistanceToZone(object.getX(), object.getY());
	}

	public void revalidateInZone(L2Character character)
	{
		if(_checkAffected)
		{
			if(!isAffected(character))
			{
				return;
			}
		}

		if(_zone.isInsideZone(character.getX(), character.getY(), character.getZ()))
		{
			if(!_characterList.containsKey(character.getObjectId()))
			{
				_characterList.put(character.getObjectId(), character);
				onEnter(character);
			}
		}
		else
		{
			if(_characterList.containsKey(character.getObjectId()))
			{
				_characterList.remove(character.getObjectId());
				onExit(character);
			}
		}
	}

	public void removeCharacter(L2Character character)
	{
		if(_characterList.containsKey(character.getObjectId()))
		{
			_characterList.remove(character.getObjectId());
			onExit(character);
		}
	}

	public boolean isCharacterInZone(L2Character character)
	{
		return _characterList.containsKey(character.getObjectId());
	}

	protected abstract void onEnter(L2Character character);

	protected abstract void onExit(L2Character character);

	protected abstract void onDieInside(L2Character character);

	protected abstract void onReviveInside(L2Character character);

	public FastMap<Integer, L2Character> getCharactersInside()
	{
		return _characterList;
	}

	public void setTargetType(String type)
	{
		_target = type;
		_checkAffected = true;
	}
	
	public void broadcastPacket(L2GameServerPacket packet)
	{
		if(_characterList.isEmpty())
		{
			return;
		}

		for(L2Character character : _characterList.values())
		{
			if(character instanceof L2PcInstance)
			{
				character.sendPacket(packet);
			}
		}
	}

}