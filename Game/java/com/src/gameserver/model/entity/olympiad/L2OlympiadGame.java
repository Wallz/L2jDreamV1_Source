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

import com.src.Config;
import com.src.gameserver.datatables.HeroSkillTable;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.sql.SpawnTable;
import com.src.gameserver.managers.OlympiadStadiaManager;
import com.src.gameserver.model.L2Party;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.instance.L2CubicInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.model.itemcontainer.Inventory;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.clientpackets.Say2;
import com.src.gameserver.network.serverpackets.CreatureSay;
import com.src.gameserver.network.serverpackets.ExAutoSoulShot;
import com.src.gameserver.network.serverpackets.ExOlympiadMode;
import com.src.gameserver.network.serverpackets.ExOlympiadSpelledInfo;
import com.src.gameserver.network.serverpackets.ExOlympiadUserInfoSpectator;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.Ride;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.StatsSet;
import com.src.gameserver.util.Broadcast;
import com.src.util.object.L2FastList;

class L2OlympiadGame extends Olympiad
{
	protected OlympiadType _type;
	public boolean _aborted;
	public boolean _gamestarted;
	public boolean _playerOneDisconnected;
	public boolean _playerTwoDisconnected;
	public String _playerOneName;
	public String _playerTwoName;
	public int _playerOneID = 0;
	public int _playerTwoID = 0;
	protected static final int OLY_MANAGER = 31688;
	public L2PcInstance _playerOne;
	public L2PcInstance _playerTwo;
	private L2FastList<L2PcInstance> _players;
	private int[] _stadiumPort;
	private int x1, y1, z1, x2, y2, z2;
	public int _stadiumID;
	public L2FastList<L2PcInstance> _spectators;
	private SystemMessage _sm;
	private SystemMessage _sm2;
	private SystemMessage _sm3;

	protected L2OlympiadGame(int id, OlympiadType type, L2FastList<L2PcInstance> list, int[] stadiumPort)
	{
		_aborted = false;
		_gamestarted = false;
		_stadiumID = id;
		_playerOneDisconnected = false;
		_playerTwoDisconnected = false;
		_type = type;
		_stadiumPort = stadiumPort;
		_spectators = new L2FastList<L2PcInstance>();

		if(list != null)
		{
			_players = list;
			_playerOne = list.get(0);
			_playerTwo = list.get(1);

			try
			{
				_playerOneName = _playerOne.getName();
				_playerTwoName = _playerTwo.getName();
				_playerOne.setOlympiadGameId(id);
				_playerTwo.setOlympiadGameId(id);
				_playerOneID = _playerOne.getObjectId();
				_playerTwoID = _playerTwo.getObjectId();
			}
			catch(Exception e)
			{
				_aborted = true;
				clearPlayers();
			}

			if(Config.OLYMPIAD_GAME_LOG)
			{
				_log.info("Olympiad System: Game - " + id + ": " + _playerOne.getName() + " Vs " + _playerTwo.getName());
			}
		}
		else
		{
			_aborted = true;
			clearPlayers();
			return;
		}
	}

	protected void clearPlayers()
	{
		_playerOne = null;
		_playerTwo = null;
		_players = null;
		_playerOneName = "";
		_playerTwoName = "";
		_playerOneID = 0;
		_playerTwoID = 0;
	}

	protected void handleDisconnect(L2PcInstance player)
	{
		if(player == _playerOne)
		{
			_playerOneDisconnected = true;
		}
		else if(player == _playerTwo)
		{
			_playerTwoDisconnected = true;
		}
	}

	protected void removals()
	{
		if(_aborted)
		{
			return;
		}

		if(_playerOne == null || _playerTwo == null)
		{
			return;
		}

		if(_playerOneDisconnected || _playerTwoDisconnected)
		{
			return;
		}

		if(_playerOne.isDead())
		{
			_playerOne.doRevive();
		}

		if(_playerTwo.isDead())
		{
			_playerTwo.doRevive();
		}

		for(L2PcInstance player : _players)
		{
			try
			{
				if(player.getClan() != null)
				{
					for(L2Skill skill : player.getClan().getAllSkills())
					{
						player.removeSkill(skill, false);
					}
				}

				if(player.isCastingNow())
				{
					player.abortCast();
				}

				if(player.isHero())
				{
					for(L2Skill skill : HeroSkillTable.getHeroSkills())
					{
						player.removeSkill(skill, false);
					}
				}

				player.removeSkill(SkillTable.getInstance().getInfo(194, 1));

				if(player.isMounted())
				{
					if(player.setMountType(0))
					{
						if(player.isFlying())
						{
							player.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
						}
						Ride dismount = new Ride(player.getObjectId(), 0, 0);
						Broadcast.toSelfAndKnownPlayersInRadius(player, dismount, 810000L);
						player.setMountObjectID(0);
						dismount = null;
					}
				}
				
				player.stopSkillEffects(176);
				player.stopSkillEffects(139);
				player.stopSkillEffects(406);
				player.stopSkillEffects(420);

				player.setCurrentCp(player.getMaxCp());
				player.setCurrentHp(player.getMaxHp());
				player.setCurrentMp(player.getMaxMp());

				if (player.getPvpFlag() > 0)
				{
					player.setPvpFlag(0);
				}
				
				player.stopAllEffects();
				player.setIsBuffProtected(false);

				if(player.getPet() != null)
				{
					L2Summon summon = player.getPet();
					summon.stopAllEffects();

					if(summon instanceof L2PetInstance)
					{
						summon.unSummon(player);
					}
				}

				if(player.getCubics() != null)
				{
					for(L2CubicInstance cubic : player.getCubics().values())
					{
						cubic.stopAction();
						player.delCubic(cubic.getId());
					}
					player.getCubics().clear();
				}

				if(player.getTrainedBeast() != null)
				{
					player.getTrainedBeast().doDespawn();
				}

				if(player.getParty() != null)
				{
					L2Party party = player.getParty();
					party.removePartyMember(player);
				}

				L2ItemInstance wpn;

				if(player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND) != null)
				{
					wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
					checkWeaponArmor(player, wpn);
				}

				if(player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND) != null)
				{
					wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
					checkWeaponArmor(player, wpn);
				}

				if(player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND) != null)
				{
					wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
					checkWeaponArmor(player, wpn);
				}

				if(player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_UNDER) != null)
				{
					wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_UNDER);
					checkWeaponArmor(player, wpn);
				}

				if(player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEAR) != null)
				{
					wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEAR);
					checkWeaponArmor(player, wpn);
				}

				if(player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_REAR) != null)
				{
					wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_REAR);
					checkWeaponArmor(player, wpn);
				}

				if(player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_NECK) != null)
				{
					wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_NECK);
					checkWeaponArmor(player, wpn);
				}

				if(player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LFINGER) != null)
				{
					wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LFINGER);
					checkWeaponArmor(player, wpn);
				}

				if(player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RFINGER) != null)
				{
					wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RFINGER);
					checkWeaponArmor(player, wpn);
				}

				if(player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HEAD) != null)
				{
					wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HEAD);
					checkWeaponArmor(player, wpn);
				}

				if(player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES) != null)
				{
					wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
					checkWeaponArmor(player, wpn);
				}

				if(player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST) != null)
				{
					wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
					checkWeaponArmor(player, wpn);
				}

				if(player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS) != null)
				{
					wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS);
					checkWeaponArmor(player, wpn);
				}

				if(player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET) != null)
				{
					wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET);
					checkWeaponArmor(player, wpn);
				}

				if(player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_BACK) != null)
				{
					wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_BACK);
					checkWeaponArmor(player, wpn);
				}

				if(player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FACE) != null)
				{
					wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FACE);
					checkWeaponArmor(player, wpn);
				}

				if(player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR) != null)
				{
					wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR);
					checkWeaponArmor(player, wpn);
				}

				if(player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_DHAIR) != null)
				{
					wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_DHAIR);
					checkWeaponArmor(player, wpn);
				}

				Map<Integer, Integer> activeSoulShots = player.getAutoSoulShot();
				for(int itemId : activeSoulShots.values())
				{
					player.removeAutoSoulShot(itemId);
					ExAutoSoulShot atk = new ExAutoSoulShot(itemId, 0);
					player.sendPacket(atk);
				}

				player.getActiveWeaponInstance().setChargedSoulshot(L2ItemInstance.CHARGED_NONE); 
				player.getActiveWeaponInstance().setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
			 	
				if(Config.OLY_REFRESH_SKILLS)
				{
					for(L2Skill skill : player.getAllSkills())
					{
						if(skill.getId() != 1324)
						{
							player.enableSkill(skill.getId());
						}
					}

					player.updateEffectIcons();
				}

				player.sendSkillList();
			}
			catch(Exception e)
			{
			}
		}
	}

	protected boolean portPlayersToArena()
	{
		boolean _playerOneCrash = _playerOne == null || _playerOneDisconnected;
		boolean _playerTwoCrash = _playerTwo == null || _playerTwoDisconnected;

		if(_playerOneCrash || _playerTwoCrash || _aborted)
		{
			_playerOne = null;
			_playerTwo = null;
			_aborted = true;
			return false;
		}

		try
		{
			x1 = _playerOne.getX();
			y1 = _playerOne.getY();
			z1 = _playerOne.getZ();

			x2 = _playerTwo.getX();
			y2 = _playerTwo.getY();
			z2 = _playerTwo.getZ();

			OlympiadStadiaManager.getInstance().getStadiumByLoc(_stadiumPort[0], _stadiumPort[1], _stadiumPort[2]).oustAllPlayers();

			if(_playerOne.getPrivateStoreType() == 1 || _playerOne.getPrivateStoreType() == 8 || _playerOne.getPrivateStoreType() == 3 || _playerOne.getPrivateStoreType() == 5 ||_playerOne.isSitting())
			{
				_playerOne.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
				_playerOne.broadcastUserInfo();
				_playerOne.standUp();
			}

			if(_playerTwo.getPrivateStoreType() == 1 || _playerTwo.getPrivateStoreType() == 8 || _playerTwo.getPrivateStoreType() == 3 || _playerTwo.getPrivateStoreType() == 5 || _playerTwo.isSitting())
			{
				_playerTwo.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
				_playerTwo.broadcastUserInfo();
				_playerTwo.standUp();
			}

			_playerOne.setTarget(null);
			_playerTwo.setTarget(null);

			if(_playerOne != null)
			{
				_playerOne.teleToLocation(_stadiumPort[0], _stadiumPort[1], _stadiumPort[2], true);
				_playerOne.setOlympiadPosition(_stadiumPort);
			}

			if(_playerTwo != null)
			{
				_playerTwo.teleToLocation(_stadiumPort[0], _stadiumPort[1], _stadiumPort[2], true);
				_playerTwo.setOlympiadPosition(_stadiumPort);
			}

			_playerOne.sendPacket(new ExOlympiadMode(2));
			_playerTwo.sendPacket(new ExOlympiadMode(2));

			_playerOne.setIsInOlympiadMode(true);
			_playerOne.setIsOlympiadStart(false);
			_playerOne.setOlympiadSide(1);

			_playerTwo.setIsInOlympiadMode(true);
			_playerTwo.setIsOlympiadStart(false);
			_playerTwo.setOlympiadSide(2);

			_gamestarted = true;
		}
		catch(NullPointerException e)
		{
			return false;
		}
		return true;
	}

	protected void sendMessageToPlayers(boolean toBattleBegin, int nsecond)
	{
		if(!toBattleBegin)
		{
			_sm = new SystemMessage(SystemMessageId.YOU_WILL_ENTER_THE_OLYMPIAD_STADIUM_IN_S1_SECOND_S);
		}
		else
		{
			_sm = new SystemMessage(SystemMessageId.THE_GAME_WILL_START_IN_S1_SECOND_S);
		}

		_sm.addNumber(nsecond);
		try
		{
			for(L2PcInstance player : _players)
			{
				player.sendPacket(_sm);
			}
		}
		catch(Exception e)
		{
		}
	}

	protected void portPlayersBack()
	{
		if(_playerOne != null)
		{
			if(x1 != 0 && y1 != 0 && z1 != 0)
			{
				_playerOne.teleToLocation(x1, y1, z1, true);
			}
		}
		else
		{
			_log.info("OlympiadPlayersBack: _playerOne is null!!!");
		}

		if(_playerTwo != null)
		{
			if(x2 != 0 && y2 != 0 && z2 != 0)
			{
				_playerTwo.teleToLocation(x2, y2, z2, true);
			}
		}
		else
		{
			_log.info("OlympiadPlayersBack: _playerTwo is null!!!");
		}
	}

	protected void PlayersStatusBack()
	{
		for(L2PcInstance player : _players)
		{
			try
			{
				player.setIsInOlympiadMode(false);
				player.setIsOlympiadStart(false);
				player.setOlympiadSide(-1);
				player.setOlympiadGameId(-1);
				player.sendPacket(new ExOlympiadMode(0));
				player.getStatus().startHpMpRegeneration();
				player.setCurrentCp(player.getMaxCp());
				player.setCurrentHp(player.getMaxHp());
				player.setCurrentMp(player.getMaxMp());

				if(player.getClan() != null)
				{
					for(L2Skill skill : player.getClan().getAllSkills())
					{
						if(skill.getMinPledgeClass() <= player.getPledgeClass())
						{
							player.addSkill(skill, false);
						}
					}
				}

				player.addSkill(SkillTable.getInstance().getInfo(194, 1));

				if(player.isHero())
				{
					for(L2Skill skill : HeroSkillTable.getHeroSkills())
					{
						player.addSkill(skill, false);
					}
				}

				player.sendSkillList();
			}
			catch(Exception e)
			{
				
			}
		}
	}

	protected boolean haveWinner()
	{
		boolean retval = false;
		if(_aborted || _playerOne == null || _playerTwo == null)
		{
			return true;
		}

		double playerOneHp = 0;

		try
		{
			if(_playerOne != null && _playerOne.getOlympiadGameId() != -1)
			{
				playerOneHp = _playerOne.getCurrentHp();
			}
		}
		catch(Exception e)
		{
			playerOneHp = 0;
		}

		double playerTwoHp = 0;
		try
		{
			if(_playerTwo != null && _playerTwo.getOlympiadGameId() != -1)
			{
				playerTwoHp = _playerTwo.getCurrentHp();
			}
		}
		catch(Exception e)
		{
			playerTwoHp = 0;
		}

		if(playerTwoHp == 0 || playerOneHp == 0)
		{
			if(_playerOne.getPet() != null)
			{
				L2Summon summon = _playerOne.getPet();
				summon.stopAllEffects();
				summon.unSummon(_playerOne);
			}

			if(_playerTwo.getPet() != null)
			{
				L2Summon summon = _playerTwo.getPet();
				summon.stopAllEffects();
				summon.unSummon(_playerTwo);
			}
			return true;
		}
		return retval;
	}

	public void sendPlayersStatus(L2PcInstance spec)
	{
		spec.sendPacket(new ExOlympiadUserInfoSpectator(_playerOne, 1));
		spec.sendPacket(new ExOlympiadUserInfoSpectator(_playerTwo, 2));
		spec.sendPacket(new ExOlympiadSpelledInfo(_playerOne));
		spec.sendPacket(new ExOlympiadSpelledInfo(_playerTwo));
	}
	
	protected void validateWinner()
	{
		if(_aborted || _playerOne == null || _playerTwo == null || _playerOneDisconnected || _playerTwoDisconnected)
		{
			return;
		}

		StatsSet playerOneStat;
		StatsSet playerTwoStat;

		playerOneStat = _nobles.get(_playerOneID);
		playerTwoStat = _nobles.get(_playerTwoID);

		int _div;
		int _gpreward;

		int playerOnePlayed = playerOneStat.getInteger(COMP_DONE);
		int playerTwoPlayed = playerTwoStat.getInteger(COMP_DONE);

		int playerOnePoints = playerOneStat.getInteger(POINTS);
		int playerTwoPoints = playerTwoStat.getInteger(POINTS);

		double playerOneHp = 0;
		try
		{
			if(_playerOne != null && !_playerOneDisconnected)
			{
				if(!_playerOne.isDead())
				{
					playerOneHp = _playerOne.getCurrentHp() + _playerOne.getCurrentCp();
				}
			}
		}
		catch(Exception e)
		{
			playerOneHp = 0;
		}

		double playerTwoHp = 0;
		try
		{
			if(_playerTwo != null && !_playerTwoDisconnected)
			{
				if(!_playerTwo.isDead())
				{
					playerTwoHp = _playerTwo.getCurrentHp() + _playerTwo.getCurrentCp();
				}
			}
		}
		catch(Exception e)
		{
			playerTwoHp = 0;
		}

		_sm = new SystemMessage(SystemMessageId.S1_HAS_WON_THE_GAME);
		_sm2 = new SystemMessage(SystemMessageId.S1_HAS_GAINED_S2_OLYMPIAD_POINTS);
		_sm3 = new SystemMessage(SystemMessageId.S1_HAS_LOST_S2_OLYMPIAD_POINTS);

		String result = "";

		_playerOne = L2World.getInstance().getPlayer(_playerOneName);
		_players.set(0, _playerOne);
		_playerTwo = L2World.getInstance().getPlayer(_playerTwoName);
		_players.set(1, _playerTwo);

		switch(_type)
		{
			case NON_CLASSED:
				_div = 5;
				_gpreward = Config.OLY_NONCLASSED_RITEM_C;
				break;
			default:
				_div = 3;
				_gpreward = Config.OLY_CLASSED_RITEM_C;
				break;
		}

		if(_playerTwo.isOnline() == 0 || playerTwoHp == 0 && playerOneHp != 0 || _playerOne.dmgDealt > _playerTwo.dmgDealt && playerTwoHp != 0 && playerOneHp != 0)
		{
			int pointDiff;
			if(playerOnePoints > playerTwoPoints)
			{
				pointDiff = playerTwoPoints / _div;
			}
			else
			{
				pointDiff = playerOnePoints / _div;
			}
			playerOneStat.set(POINTS, playerOnePoints + pointDiff);
			playerTwoStat.set(POINTS, playerTwoPoints - pointDiff);

			_sm.addString(_playerOneName);
			broadcastMessage(_sm, true);
			_sm2.addString(_playerOneName);
			_sm2.addNumber(pointDiff);
			broadcastMessage(_sm2, true);
			_sm3.addString(_playerTwoName);
			_sm3.addNumber(pointDiff);
			broadcastMessage(_sm3, true);

			try
			{
				result = " (" + playerOneHp + "hp vs " + playerTwoHp + "hp - " + _playerOne.dmgDealt + "dmg vs " + _playerTwo.dmgDealt + "dmg) " + _playerOneName + " win " + pointDiff + " points";
				L2ItemInstance item = _playerOne.getInventory().addItem("Olympiad", Config.OLY_BATTLE_REWARD_ITEM, _gpreward, _playerOne, null);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(item);
				_playerOne.sendPacket(iu);

				_playerOne.sendPacket(new SystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(item.getItemId()).addNumber(_gpreward));
			}
			catch(Exception e)
			{
			}
		}
		else if(_playerOne.isOnline() == 0 || playerOneHp == 0 && playerTwoHp != 0 || _playerTwo.dmgDealt > _playerOne.dmgDealt && playerOneHp != 0 && playerTwoHp != 0)
		{
			int pointDiff;
			if(playerTwoPoints > playerOnePoints)
			{
				pointDiff = playerOnePoints / _div;
			}
			else
			{
				pointDiff = playerTwoPoints / _div;
			}
			playerTwoStat.set(POINTS, playerTwoPoints + pointDiff);
			playerOneStat.set(POINTS, playerOnePoints - pointDiff);

			_sm.addString(_playerTwoName);
			broadcastMessage(_sm, true);
			_sm2.addString(_playerTwoName);
			_sm2.addNumber(pointDiff);
			broadcastMessage(_sm2, true);
			_sm3.addString(_playerOneName);
			_sm3.addNumber(pointDiff);
			broadcastMessage(_sm3, true);

			try
			{
				result = " (" + playerOneHp + "hp vs " + playerTwoHp + "hp - " + _playerOne.dmgDealt + "dmg vs " + _playerTwo.dmgDealt + "dmg) " + _playerTwoName + " win " + pointDiff + " points";
				L2ItemInstance item = _playerTwo.getInventory().addItem("Olympiad", Config.OLY_BATTLE_REWARD_ITEM, _gpreward, _playerTwo, null);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(item);
				_playerTwo.sendPacket(iu);

				_playerTwo.sendPacket(new SystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(item.getItemId()).addNumber(_gpreward));
			}
			catch(Exception e)
			{
			}
		}
		else
		{
			result = " tie";
			_sm = new SystemMessage(SystemMessageId.THE_GAME_ENDED_IN_A_TIE);
			broadcastMessage(_sm, true);
		}

		if(Config.OLYMPIAD_GAME_LOG)
		{
			_log.info("Olympia Result: " + _playerOneName + " vs " + _playerTwoName + " ... " + result);
		}

		playerOneStat.set(COMP_DONE, playerOnePlayed + 1);
		playerTwoStat.set(COMP_DONE, playerTwoPlayed + 1);

		_nobles.remove(_playerOneID);
		_nobles.remove(_playerTwoID);

		_nobles.put(_playerOneID, playerOneStat);
		_nobles.put(_playerTwoID, playerTwoStat);

		for(int i = 20; i > 10; i -= 10)
		{
			_sm = new SystemMessage(SystemMessageId.YOU_WILL_BE_MOVED_TO_TOWN_IN_S1_SECONDS);
			_sm.addNumber(10);
			broadcastMessage(_sm, false);
			try
			{
				Thread.sleep(5000);
			}
			catch(InterruptedException e)
			{
			}
		}
		for(int i = 5; i > 0; i--)
		{
			_sm = new SystemMessage(SystemMessageId.YOU_WILL_BE_MOVED_TO_TOWN_IN_S1_SECONDS);
			_sm.addNumber(i);
			broadcastMessage(_sm, false);
			try
			{
				Thread.sleep(1000);
			}
			catch(InterruptedException e)
			{
			}
		}
	}

	protected void additions()
	{
		for(L2PcInstance player : _players)
		{
			try
			{
				player.dmgDealt = 0;

				L2Skill skill;

				skill = SkillTable.getInstance().getInfo(1204, 2);
				skill.getEffects(player, player);
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(1204));

				if(!player.isMageClass())
				{
					skill = SkillTable.getInstance().getInfo(1086, 1);
					skill.getEffects(player, player);
					player.sendPacket(new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(1086));
				}
				else
				{
					skill = SkillTable.getInstance().getInfo(1085, 1);
					skill.getEffects(player, player);
					player.sendPacket(new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(1085));
				}
				player.setCurrentCp(player.getMaxCp());
				player.setCurrentHp(player.getMaxHp());
				player.setCurrentMp(player.getMaxMp());
			}
			catch(Exception e)
			{
				
			}
		}
	}

	protected boolean makeCompetitionStart()
	{
		if(_aborted)
		{
			return false;
		}

		_sm = new SystemMessage(SystemMessageId.STARTS_THE_GAME);
		broadcastMessage(_sm, true);
		try
		{
			for(L2PcInstance player : _players)
			{
				player.setIsOlympiadStart(true);
			}
		}
		catch(Exception e)
		{
			_aborted = true;
			return false;
		}

		return true;
	}

	protected String getTitle()
	{
		String msg = "";
		msg += _playerOneName + " : " + _playerTwoName;
		return msg;
	}

	protected L2PcInstance[] getPlayers()
	{
		L2PcInstance[] players = new L2PcInstance[2];

		if(_playerOne == null || _playerTwo == null)
		{
			return null;
		}

		players[0] = _playerOne;
		players[1] = _playerTwo;

		return players;
	}

	protected L2FastList<L2PcInstance> getSpectators()
	{
		return _spectators;
	}

	protected void addSpectator(L2PcInstance spec)
	{
		_spectators.add(spec);
	}

	protected void removeSpectator(L2PcInstance spec)
	{
		if(_spectators != null && _spectators.contains(spec))
		{
			_spectators.remove(spec);
		}
	}

	private void broadcastMessage(SystemMessage sm, boolean toAll)
	{
		try
		{
			_playerOne.sendPacket(sm);
			_playerTwo.sendPacket(sm);
		}
		catch(Exception e)
		{
		}

		if(toAll && _spectators != null)
		{
			for(L2PcInstance spec : _spectators)
			{
				try
				{
					spec.sendPacket(sm);
				}
				catch(NullPointerException e)
				{
					
				}
			}
		}
	}
	
	protected void announceGame()
	{
		int objId;
		String npcName;
		String gameType = null;
		switch (_type)
		{
		 	case NON_CLASSED:
		 		gameType = "class-free individual match";
		 		break;
		 		
		 	default:
		 		gameType = "class-specific individual match";
		 		break;
		}
		
		for (L2Spawn manager : SpawnTable.getInstance().getSpawnTable().values())
		{
			if (manager != null && manager.getNpcid() == OLY_MANAGER)
			{
				objId = manager.getLastSpawn().getObjectId();
				npcName = manager.getLastSpawn().getName();
				manager.getLastSpawn().broadcastPacket(new CreatureSay(objId, Say2.SHOUT, npcName, "Olympiad " + gameType + " is going to begin in Arena " + (_stadiumID + 1) + " in a moment."));
			}
		}
	}

	private void checkWeaponArmor(L2PcInstance player, L2ItemInstance wpn)
	{
		if(wpn != null && (wpn.getItemId() >= 6611 && wpn.getItemId() <= 6621 || wpn.getItemId() == 6842 || Config.LIST_OLY_RESTRICTED_ITEMS.contains(wpn.getItemId())))
		{
			L2ItemInstance[] unequiped = player.getInventory().unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
			InventoryUpdate iu = new InventoryUpdate();
			for(L2ItemInstance element : unequiped)
			{
				iu.addModifiedItem(element);
			}
			player.sendPacket(iu);
			player.abortAttack();
			player.broadcastUserInfo();

			if(unequiped.length > 0)
			{
				if(unequiped[0].isWear())
				{
					return;
				}

				if(unequiped[0].getEnchantLevel() > 0)
				{
					player.sendPacket(new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED).addNumber(unequiped[0].getEnchantLevel()).addItemName(unequiped[0].getItemId()));
				}
				else
				{
					player.sendPacket(new SystemMessage(SystemMessageId.S1_DISARMED).addItemName(unequiped[0].getItemId()));
				}
			}
		}
	}
}