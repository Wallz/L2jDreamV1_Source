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
package com.src.gameserver.network.clientpackets;

import java.util.logging.Logger;

import com.src.gameserver.datatables.xml.MapRegionTable;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.managers.ClanHallManager;
import com.src.gameserver.model.L2SiegeClan;
import com.src.gameserver.model.Location;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.ClanHall;
import com.src.gameserver.model.entity.siege.Castle;
import com.src.gameserver.network.serverpackets.Revive;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.gameserver.util.IllegalPlayerAction;
import com.src.gameserver.util.Util;

public final class RequestRestartPoint extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestRestartPoint.class.getName());

	private static final String _C__6d_REQUESTRESTARTPOINT = "[C] 6d RequestRestartPoint";

	protected int _requestedPointType;
	protected boolean _continuation;

	@Override
	protected void readImpl()
	{
		_requestedPointType = readD();
	}

	class DeathTask implements Runnable
	{
		L2PcInstance activeChar;

		DeathTask(L2PcInstance _activeChar)
		{
			activeChar = _activeChar;
		}

		@Override
		public void run()
		{
			if(activeChar.isInEvent())
			{
				activeChar.sendMessage("You can't restart in event.");
				return;
			}
			try
			{
				Location loc = null;
				Castle castle = null;

				if(activeChar.isInJail())
				{
					_requestedPointType = 27;
				}
				else if(activeChar.isFestivalParticipant())
				{
					_requestedPointType = 4;
				}

				if(activeChar.isPhoenixBlessed())
				{
					activeChar.stopPhoenixBlessing(null);
				}

				switch(_requestedPointType)
				{
					case 1:
						if(activeChar.getClan() == null || activeChar.getClan().getHasHideout() == 0)
						{
							activeChar.sendMessage("You may not use this respawn point!");
							Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " used respawn cheat.", IllegalPlayerAction.PUNISH_KICK);
							return;
						}
						loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.ClanHall);

						if(ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()) != null && ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()).getFunction(ClanHall.FUNC_RESTORE_EXP) != null)
						{
							activeChar.restoreExp(ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()).getFunction(ClanHall.FUNC_RESTORE_EXP).getLvl());
						}
						break;

					case 2:
						Boolean isInDefense = false;
						castle = CastleManager.getInstance().getCastle(activeChar);
						MapRegionTable.TeleportWhereType teleportWhere = MapRegionTable.TeleportWhereType.Town;

						if(castle != null && castle.getSiege().getIsInProgress())
						{
							if(castle.getSiege().checkIsDefender(activeChar.getClan()))
							{
								isInDefense = true;
							}
						}

						if(activeChar.getClan().getHasCastle() == 0 && !isInDefense)
						{
							activeChar.sendMessage("You may not use this respawn point!");
							Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " used respawn cheat.", IllegalPlayerAction.PUNISH_KICK);
							return;
						}

						if(CastleManager.getInstance().getCastleByOwner(activeChar.getClan()) != null)
						{
							teleportWhere = MapRegionTable.TeleportWhereType.Castle;
						}

						loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, teleportWhere);
						break;

					case 3:
						L2SiegeClan siegeClan = null;
						castle = CastleManager.getInstance().getCastle(activeChar);

						if(castle != null && castle.getSiege().getIsInProgress())
						{
							siegeClan = castle.getSiege().getAttackerClan(activeChar.getClan());
						}

						if(siegeClan == null || siegeClan.getFlag().size() == 0)
						{
							activeChar.sendMessage("You may not use this respawn point!");
							Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " used respawn cheat.", IllegalPlayerAction.PUNISH_KICK);
							return;
						}

						loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.SiegeFlag);
						break;

					case 4:
						if(!activeChar.isGM() && !activeChar.isFestivalParticipant())
						{
							activeChar.sendMessage("You may not use this respawn point!");
							Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " used respawn cheat.", IllegalPlayerAction.PUNISH_KICK);
							return;
						}

						loc = new Location(activeChar.getX(), activeChar.getY(), activeChar.getZ());
						break;

					case 27:
						if(!activeChar.isInJail())
						{
							return;
						}
						loc = new Location(-114356, -249645, -2984);
						break;

					default:
						loc = MapRegionTable.getInstance().getTeleToLocation(activeChar, MapRegionTable.TeleportWhereType.Town);
						break;
				}

				activeChar.setIsIn7sDungeon(false);
				activeChar.setIsPendingRevive(true);
				activeChar.teleToLocation(loc, true);
			}
			catch(Throwable e)
			{
			}
		}
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if(activeChar == null)
		{
			return;
		}

		if(activeChar.isFakeDeath())
		{
			activeChar.stopFakeDeath(null);
			activeChar.broadcastPacket(new Revive(activeChar));
			return;
		}
		else if(!activeChar.isAlikeDead())
		{
			_log.warning("Living player [" + activeChar.getName() + "] called RestartPointPacket! Ban this player!");
			return;
		}

		Castle castle = CastleManager.getInstance().getCastle(activeChar.getX(), activeChar.getY(), activeChar.getZ());
		if(castle != null && castle.getSiege().getIsInProgress())
		{
			if(activeChar.getClan() != null && castle.getSiege().checkIsAttacker(activeChar.getClan()))
			{
				ThreadPoolManager.getInstance().scheduleGeneral(new DeathTask(activeChar), castle.getSiege().getAttackerRespawnDelay());
				activeChar.sendMessage("You will be re-spawned in " + castle.getSiege().getAttackerRespawnDelay() / 1000 + " seconds");
				return;
			}
		}

		new DeathTask(activeChar).run();
	}

	@Override
	public String getType()
	{
		return _C__6d_REQUESTRESTARTPOINT;
	}

}