package com.src.gameserver.model.zone.type;

import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.xml.MapRegionTable;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.zone.L2ZoneType;

/**
 * @author Matim
 * @version 1.0
 * <br><br>
 * Custom Multi conditions Zone, such us:
 * - player level
 * - is player hero
 * - is player noble
 */
public class L2MultiConditionZone extends L2ZoneType
{
	private String _msgOnEnter = "";
	private String _msgOnExit = "";
	private boolean _heroOnly = false;
	private boolean _nobleOnly = false;
	private boolean _nobleOnRevive = false;
	private int _minLevel = 1;
	private int _maxLevel = 80;
	
	public L2MultiConditionZone(int id) 
	{
		super(id);
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("msgOnEnter"))
		{
			_msgOnEnter = value;
		}
		else if (name.equals("msgOnExit"))
		{
			_msgOnExit = value;
		}
		else if (name.equals("heroOnly"))
		{
			_heroOnly = Boolean.parseBoolean(value);
		}
		else if (name.equals("nobleOnly"))
		{
			_nobleOnly = Boolean.parseBoolean(value);
		}
		else if (name.equals("nobleOnRevive"))
		{
			_nobleOnRevive = Boolean.parseBoolean(value);
		}
		else if (name.equals("minPlayerLevel"))
		{
			_minLevel = Integer.parseInt(value);
		}
		else if (name.equals("maxPlayerLevel"))
		{
			_maxLevel = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}

	@Override
	protected void onEnter(L2Character character) 
	{
		if(character instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) character;
			
			if (player.isFlying())
			{
				return;
			}
			else
			{
				if (_heroOnly)
				{
					if (!player.isHero())
					{
						player.sendMessage("You are not hero, you can not enter this zone!");
						player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
					}
				}
				else if (_nobleOnly)
				{
					if (!player.isHero())
					{
						player.sendMessage("You are not noble, you can not enter this zone!");
						player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
					}
				}
				else if (player.getLevel() < _minLevel)
				{
					player.sendMessage("Your level is to low, you can not enter this zone!");
					player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
				}
				else if (player.getLevel() > _maxLevel)
				{
					player.sendMessage("Your level is to high, you can not enter this zone!");
					player.teleToLocation(MapRegionTable.TeleportWhereType.Town);
				}
				else if (!_msgOnEnter.equals(""))
				{
					player.sendMessage(_msgOnEnter);
				}
			}
		}
	}

	@Override
	protected void onExit(L2Character character) 
	{
		if(character instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) character;
			
			if (!_msgOnExit.equals(""))
			{
				if (player != null)
				{
					player.sendMessage(_msgOnExit);
				}
			}
		}
	}

	@Override
	protected void onReviveInside(L2Character character)
	{
		if(character instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) character;
			
			if (_nobleOnRevive)
			{
				if (player != null)
				{
					L2Skill skill = SkillTable.getInstance().getInfo(1323, 1);
					skill.getEffects(player, player);
				}
			}
		}
	}
	
	@Override
	protected void onDieInside(L2Character character)
	{
		
	}
}