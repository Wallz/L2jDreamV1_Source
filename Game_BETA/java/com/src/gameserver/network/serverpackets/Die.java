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
package com.src.gameserver.network.serverpackets;

import com.src.gameserver.datatables.AccessLevel;
import com.src.gameserver.datatables.xml.AccessLevels;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.L2SiegeClan;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.siege.Castle;

public class Die extends L2GameServerPacket
{
	private static final String _S__0B_DIE = "[S] 06 Die";

	private int _charObjId;
	private boolean _fake;
	private boolean _sweepable;
	private boolean _canTeleport;
	private AccessLevel _access = AccessLevels._userAccessLevel;
	private L2Clan _clan;
	L2Character _activeChar;

	public Die(L2Character cha)
	{
		_activeChar = cha;
		if(cha instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) cha;
			_access = player.getAccessLevel();
			_clan = player.getClan();
			_canTeleport = player.isPendingRevive();
		}

		_charObjId = cha.getObjectId();
		_fake = !cha.isDead();
		if(cha instanceof L2Attackable)
		{
			_sweepable = ((L2Attackable) cha).isSweepActive();
		}
	}

	@Override
	protected final void writeImpl()
	{
		if(_fake)
			return;

		writeC(0x06);
		writeD(_charObjId);
		writeD(_canTeleport ? 0x01 : 0);

		if(_canTeleport && _clan != null)
		{
			L2SiegeClan siegeClan = null;
			Boolean isInDefense = false;
			Castle castle = CastleManager.getInstance().getCastle(_activeChar);

			if(castle != null && castle.getSiege().getIsInProgress())
			{
				siegeClan = castle.getSiege().getAttackerClan(_clan);
				if(siegeClan == null && castle.getSiege().checkIsDefender(_clan))
				{
					isInDefense = true;
				}
			}

			writeD(_clan.getHasHideout() > 0 ? 0x01 : 0x00);
			writeD(_clan.getHasCastle() > 0 || isInDefense ? 0x01 : 0x00);
			writeD(siegeClan != null && !isInDefense && siegeClan.getFlag().size() > 0 ? 0x01 : 0x00);
		}
		else
		{
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
		}
		
		writeD(_sweepable ? 0x01 : 0x00);
		writeD(_access.allowFixedRes() ? 0x01 : 0x00);
	}

	@Override
	public String getType()
	{
		return _S__0B_DIE;
	}

}