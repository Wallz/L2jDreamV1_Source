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

import com.src.Config;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.network.serverpackets.UserInfo;

public final class RequestEvaluate extends L2GameClientPacket
{
	private static final String _C__B9_REQUESTEVALUATE = "[C] B9 RequestEvaluate";

	@SuppressWarnings("unused")
	private int _targetId;

	@Override
	protected void readImpl()
	{
		_targetId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if(activeChar == null)
		{
			return;
		}

		if(!(activeChar.getTarget() instanceof L2PcInstance))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
			return;
		}

		if(activeChar.getLevel() < 10)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.ONLY_LEVEL_SUP_10_CAN_RECOMMEND));
			return;
		}

		if(activeChar.getTarget() == activeChar)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_RECOMMEND_YOURSELF));
			return;
		}

		if(activeChar.getRecomLeft() <= 0)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.NO_MORE_RECOMMENDATIONS_TO_HAVE));
			return;
		}

		L2PcInstance target = (L2PcInstance) activeChar.getTarget();

		if(target.getRecomHave() >= Config.ALT_RECOMMENDATIONS_NUMBER)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOUR_TARGET_NO_LONGER_RECEIVE_A_RECOMMENDATION));
			return;
		}

		if(!activeChar.canRecom(target))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THAT_CHARACTER_IS_RECOMMENDED));
			return;
		}

		activeChar.giveRecom(target);

		activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_RECOMMENDED).addString(target.getName()).addNumber(activeChar.getRecomLeft()));
		target.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_BEEN_RECOMMENDED).addString(activeChar.getName()));

		activeChar.sendPacket(new UserInfo(activeChar));
		target.broadcastUserInfo();
	}

	@Override
	public String getType()
	{
		return _C__B9_REQUESTEVALUATE;
	}

}