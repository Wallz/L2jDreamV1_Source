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

import com.src.gameserver.model.actor.instance.L2HennaInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class HennaItemInfo extends L2GameServerPacket
{
	private static final String _S__E3_HennaItemInfo = "[S] E3 HennaItemInfo";

	private L2PcInstance _activeChar;
	private L2HennaInstance _henna;

	public HennaItemInfo(L2HennaInstance henna, L2PcInstance player)
	{
		_henna = henna;
		_activeChar = player;
	}

	@Override
	protected final void writeImpl()
	{

		writeC(0xe3);
		writeD(_henna.getSymbolId());
		writeD(_henna.getItemIdDye());
		writeD(_henna.getAmountDyeRequire());
		writeD(_henna.getPrice());
		writeD(1);
		writeD(_activeChar.getAdena());

		writeD(_activeChar.getINT());
		writeC(_activeChar.getINT() + _henna.getStatINT());
		writeD(_activeChar.getSTR());
		writeC(_activeChar.getSTR() + _henna.getStatSTR());
		writeD(_activeChar.getCON());
		writeC(_activeChar.getCON() + _henna.getStatCON());
		writeD(_activeChar.getMEN());
		writeC(_activeChar.getMEN() + _henna.getStatMEM());
		writeD(_activeChar.getDEX());
		writeC(_activeChar.getDEX() + _henna.getStatDEX());
		writeD(_activeChar.getWIT());
		writeC(_activeChar.getWIT() + _henna.getStatWIT());
	}

	@Override
	public String getType()
	{
		return _S__E3_HennaItemInfo;
	}

}