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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.managers.CursedWeaponsManager;
import com.src.gameserver.managers.FunEventsManager;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.itemcontainer.Inventory;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.Earthquake;
import com.src.gameserver.network.serverpackets.ExRedSky;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.network.serverpackets.Ride;
import com.src.gameserver.network.serverpackets.SocialAction;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.item.L2Item;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;
import com.src.util.object.Point3D;
import com.src.util.random.Rnd;

public class CursedWeapon
{
	private static final Log _log = LogFactory.getLog(CursedWeaponsManager.class);

	private final String _name;
	private final int _itemId;
	private final int _skillId;
	private final int _skillMaxLevel;
	private int _dropRate;
	private int _duration;
	private int _durationLost;
	private int _disapearChance;
	private int _stageKills;

	private boolean _isDropped = false;
	private boolean _isActivated = false;
	private ScheduledFuture<?> _removeTask;

	private int _nbKills = 0;
	private long _endTime = 0;

	private int _playerId = 0;
	private L2PcInstance _player = null;
	private L2ItemInstance _item = null;
	private int _playerKarma = 0;
	private int _playerPkKills = 0;

	public CursedWeapon(int itemId, int skillId, String name)
	{
		_name = name;
		_itemId = itemId;
		_skillId = skillId;
		_skillMaxLevel = SkillTable.getInstance().getMaxLevel(_skillId, 0);
	}

	public void endOfLife()
	{
		if(_isActivated)
		{
			if(_player != null && _player.isOnline() == 1)
			{
				_log.info(_name + " being removed online.");

				_player.abortAttack();

				_player.setKarma(_playerKarma);
				_player.setPkKills(_playerPkKills);
				_player.setCursedWeaponEquipedId(0);
				removeSkill();

				_player.getInventory().unEquipItemInBodySlotAndRecord(L2Item.SLOT_LR_HAND);
				_player.getInventory().destroyItemByItemId("", _itemId, 1, _player, null);
				_player.store();

				_player.sendPacket(new ItemList(_player, true));
				
				_player.broadcastUserInfo();
			}
			else
			{
				_log.info(_name + " being removed offline.");

				Connection con = null;
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();

					PreparedStatement statement = con.prepareStatement("DELETE FROM items WHERE owner_id = ? AND item_id = ?");
					statement.setInt(1, _playerId);
					statement.setInt(2, _itemId);

					if(statement.executeUpdate() != 1)
					{
						_log.warn("Error while deleting itemId " + _itemId + " from userId " + _playerId);
					}

					statement.close();
					statement = null;

					statement = con.prepareStatement("UPDATE characters SET karma = ?, pkkills = ? WHERE obj_id = ?");
					statement.setInt(1, _playerKarma);
					statement.setInt(2, _playerPkKills);
					statement.setInt(3, _playerId);

					if(statement.executeUpdate() != 1)
					{
						_log.warn("Error while updating karma & pkkills for userId " + _playerId);
					}
					ResourceUtil.closeStatement(statement);
				}
				catch(Exception e)
				{
					_log.error("Could not delete", e);
				}
				finally
				{
					ResourceUtil.closeConnection(con); 
				}
			}
		}
		else
		{
			if(_player != null && _player.getInventory().getItemByItemId(_itemId) != null)
			{
                L2ItemInstance rhand = _player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
                if (rhand != null)
                {    
                    _player.getInventory().unEquipItemInSlotAndRecord(rhand.getEquipSlot());
                }

                _player.getInventory().destroyItemByItemId("", _itemId, 1, _player, null);
                _player.store();
                
                _player.sendPacket(new ItemList(_player, true));
                
				_player.broadcastUserInfo();
			}
			else if(_item != null)
			{
				_item.decayMe();
				L2World.getInstance().removeObject(_item);
				_log.info(_name + " item has been removed from World.");
			}
		}

		CursedWeaponsManager.announce(new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED).addItemName(_itemId));

		cancelTask();
		_isActivated = false;
		_isDropped = false;
		_endTime = 0;
		_player = null;
		_playerId = 0;
		_playerKarma = 0;
		_playerPkKills = 0;
		_item = null;
		_nbKills = 0;
	}

	private void cancelTask()
	{
		if(_removeTask != null)
		{
			_removeTask.cancel(true);
			_removeTask = null;
		}
	}

	private class RemoveTask implements Runnable
	{
		protected RemoveTask()
		{
		}

		@Override
		public void run()
		{
			if(System.currentTimeMillis() >= getEndTime())
			{
				endOfLife();
			}
		}
	}

	private void dropIt(L2Attackable attackable, L2PcInstance player)
	{
		dropIt(attackable, player, null, true);
	}

	public void dropIt(L2Attackable attackable, L2PcInstance player, L2Character killer, boolean fromMonster)
	{
		_isActivated = false;

		SystemMessage sm = new SystemMessage(SystemMessageId.S2_WAS_DROPPED_IN_THE_S1_REGION);
		sm.addItemName(_itemId);
		
		if(fromMonster)
		{
			_item = attackable.DropItem(player, _itemId, 1);
			_item.setDropTime(0);

			ExRedSky packet = new ExRedSky(10);
			Earthquake eq = new Earthquake(player.getX(), player.getY(), player.getZ(), 14, 3);

			for(L2PcInstance aPlayer : L2World.getInstance().getAllPlayers())
			{
				aPlayer.sendPacket(packet);
				aPlayer.sendPacket(eq);
			}
			
			sm.addZoneName(attackable.getX(), attackable.getY(), attackable.getZ());
			
			packet = null;
			eq = null;
			
			cancelTask();
			_endTime = 0;
			
		}
		else
		{
			_player.abortAttack();
			_player.setKarma(_playerKarma);
			_player.setPkKills(_playerPkKills);
			_player.setCursedWeaponEquipedId(0);
			removeSkill();

			_player.getInventory().unEquipItemInBodySlotAndRecord(L2Item.SLOT_LR_HAND);
			_player.dropItem("DieDrop", _item, killer, true, true);
			_player.store();
			
			_player.sendPacket(new ItemList(_player, false));
			_player.broadcastUserInfo();
			
			sm.addZoneName(_player.getX(), _player.getY(), _player.getZ());
		}
		
		_player = null;
		_playerId = 0;
		_playerKarma = 0;
		_playerKarma = 0;
		_playerPkKills = 0;
		_nbKills = 0;
		_isDropped = true;
		
		CursedWeaponsManager.announce(sm);
	}

	public void giveSkill()
	{
		int level = 1 + _nbKills / _stageKills;

		if(level > _skillMaxLevel)
		{
			level = _skillMaxLevel;
		}

		L2Skill skill = SkillTable.getInstance().getInfo(_skillId, level);
		_player.addSkill(skill, false);

		skill = SkillTable.getInstance().getInfo(3630, 1);
		_player.addSkill(skill, false);
		skill = SkillTable.getInstance().getInfo(3631, 1);
		_player.addSkill(skill, false);

		_player.sendSkillList();
	}

	public void removeSkill()
	{
		_player.removeSkill(SkillTable.getInstance().getInfo(_skillId, _player.getSkillLevel(_skillId)), false);
		_player.removeSkill(SkillTable.getInstance().getInfo(3630, 1), false);
		_player.removeSkill(SkillTable.getInstance().getInfo(3631, 1), false);
		_player.sendSkillList();
	}

	public void reActivate()
	{
		_isActivated = true;

		if(_endTime - System.currentTimeMillis() <= 0)
		{
			endOfLife();
		}
		else
		{
			_removeTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new RemoveTask(), _durationLost * 12000L, _durationLost * 12000L);
		}
	}

	public boolean checkDrop(L2Attackable attackable, L2PcInstance player)
	{
		if(Rnd.get(100000) < _dropRate)
		{
			
			dropIt(attackable, player);

			_endTime = System.currentTimeMillis() + _duration * 60000L;
			
			_removeTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new RemoveTask(), _durationLost * 12000L, _durationLost * 12000L);

			return true;
		}
		return false;
	}

	public void activate(L2PcInstance player, L2ItemInstance item)
	{
		_player = player;

		if(player.isMounted())
		{
			if(_player.setMountType(0))
			{
				Ride dismount = new Ride(_player.getObjectId(), Ride.ACTION_DISMOUNT, 0);
				_player.broadcastPacket(dismount);
				_player.setMountObjectID(0);
				dismount = null;
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1).addItemName(_item.getItemId())); 
				return;
			}
		}
		if (player.getEventName() != null)
			FunEventsManager.getInstance().notifyJoinCursed(player);

		_isActivated = true;

		_playerId = _player.getObjectId();
		_playerKarma = _player.getKarma();
		_playerPkKills = _player.getPkKills();
		saveData();

		_player.setCursedWeaponEquipedId(_itemId);
		_player.setKarma(9999999);
		_player.setPkKills(0);

		if(_player.isInParty())
		{
			_player.getParty().oustPartyMember(_player);
		}

		if(_player.isWearingFormalWear())
		{
			_player.getInventory().unEquipItemInSlot(10);
		}

		giveSkill();

		_item = item;

		_player.getInventory().equipItemAndRecord(_item);

		_player.sendPacket(new SystemMessage(SystemMessageId.S1_EQUIPPED).addItemName(_item.getItemId()));

		_player.setCurrentHpMp(_player.getMaxHp(), _player.getMaxMp());
		_player.setCurrentCp(_player.getMaxCp());

		_player.sendPacket(new ItemList(_player, false));

		_player.broadcastUserInfo();

		SocialAction atk = new SocialAction(_player.getObjectId(), 17);

		_player.broadcastPacket(atk);

		CursedWeaponsManager.announce(new SystemMessage(SystemMessageId.THE_OWNER_OF_S2_HAS_APPEARED_IN_THE_S1_REGION).addZoneName(_player.getX(), _player.getY(), _player.getZ()).addItemName(_item.getItemId()));
	}

	public void saveData()
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			PreparedStatement statement = con.prepareStatement("DELETE FROM cursed_weapons WHERE itemId = ?");
			statement.setInt(1, _itemId);
			statement.executeUpdate();

			if(_isActivated)
			{
				statement = con.prepareStatement("INSERT INTO cursed_weapons (itemId, playerId, playerKarma, playerPkKills, nbKills, endTime) VALUES (?, ?, ?, ?, ?, ?)");
				statement.setInt(1, _itemId);
				statement.setInt(2, _playerId);
				statement.setInt(3, _playerKarma);
				statement.setInt(4, _playerPkKills);
				statement.setInt(5, _nbKills);
				statement.setLong(6, _endTime);
				statement.executeUpdate();
			}
			ResourceUtil.closeStatement(statement);
		}
		catch(SQLException e)
		{
			_log.error("CursedWeapon: Failed to save data", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	public void dropIt(L2Character killer)
	{
		if(Rnd.get(100) <= _disapearChance)
		{
			endOfLife();
		}
		else
		{
			dropIt(null, null, killer, false);
		}
	}

	public void increaseKills()
	{
		_nbKills++;

		if (_player != null && _player.isOnline() > 0) 
		{ 
			_player.setPkKills(_nbKills); 
			_player.broadcastUserInfo(); 
			
			if (_nbKills % _stageKills == 0 && _nbKills <= _stageKills*(_skillMaxLevel-1)) 
			{ 
				giveSkill(); 
			} 
		}

		_endTime -= _durationLost * 60000L;
		saveData();
	}

	public void setDisapearChance(int disapearChance)
	{
		_disapearChance = disapearChance;
	}

	public void setDropRate(int dropRate)
	{
		_dropRate = dropRate;
	}

	public void setDuration(int duration)
	{
		_duration = duration;
	}

	public void setDurationLost(int durationLost)
	{
		_durationLost = durationLost;
	}

	public void setStageKills(int stageKills)
	{
		_stageKills = stageKills;
	}

	public void setNbKills(int nbKills)
	{
		_nbKills = nbKills;
	}

	public void setPlayerId(int playerId)
	{
		_playerId = playerId;
	}

	public void setPlayerKarma(int playerKarma)
	{
		_playerKarma = playerKarma;
	}

	public void setPlayerPkKills(int playerPkKills)
	{
		_playerPkKills = playerPkKills;
	}

	public void setActivated(boolean isActivated)
	{
		_isActivated = isActivated;
	}

	public void setDropped(boolean isDropped)
	{
		_isDropped = isDropped;
	}

	public void setEndTime(long endTime)
	{
		_endTime = endTime;
	}

	public void setPlayer(L2PcInstance player)
	{
		_player = player;
	}

	public void setItem(L2ItemInstance item)
	{
		_item = item;
	}

	public boolean isActivated()
	{
		return _isActivated;
	}

	public boolean isDropped()
	{
		return _isDropped;
	}

	public int getDuration()
	{
		return _duration;
	}
	
	public long getEndTime()
	{
		return _endTime;
	}

	public String getName()
	{
		return _name;
	}

	public int getItemId()
	{
		return _itemId;
	}

	public int getSkillId()
	{
		return _skillId;
	}

	public int getPlayerId()
	{
		return _playerId;
	}

	public L2PcInstance getPlayer()
	{
		return _player;
	}

	public int getPlayerKarma()
	{
		return _playerKarma;
	}

	public int getPlayerPkKills()
	{
		return _playerPkKills;
	}

	public int getNbKills()
	{
		return _nbKills;
	}

	public int getStageKills()
	{
		return _stageKills;
	}

	public boolean isActive()
	{
		return _isActivated || _isDropped;
	}

	public int getLevel()
	{
		if(_nbKills > _stageKills * _skillMaxLevel)
			return _skillMaxLevel;
		else
			return _nbKills / _stageKills;
	}

	public long getTimeLeft()
	{
		return _endTime - System.currentTimeMillis();
	}

	public void goTo(L2PcInstance player)
	{
		if(player == null)
		{
			return;
		}

		if(_isActivated)
		{
			player.teleToLocation(_player.getX(), _player.getY(), _player.getZ() + 20, true);
		}
		else if(_isDropped)
		{
			player.teleToLocation(_item.getX(), _item.getY(), _item.getZ() + 20, true);
		}
		else
		{
			player.sendMessage(_name + " isn't in the World.");
		}
	}

	public Point3D getWorldPosition()
	{
		if(_isActivated && _player != null)
		{
			return _player.getPosition().getWorldPosition();
		}

		if(_isDropped && _item != null)
		{
			return _item.getPosition().getWorldPosition();
		}

		return null;
	}
}