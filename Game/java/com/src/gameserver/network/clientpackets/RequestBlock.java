/* This program is free software; you can redistribute it and/or modify
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

import com.src.gameserver.model.BlockList;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;

public final class RequestBlock extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(L2PcInstance.class.getName());

	private final static int BLOCK = 0;
	private final static int UNBLOCK = 1;
	private final static int BLOCKLIST = 2;
	private final static int ALLBLOCK = 3;
	private final static int ALLUNBLOCK = 4;

	private String _name;
	private Integer _type;
	private L2PcInstance _target;

	@Override
	protected void readImpl()
	{
		_type = readD(); //0x00 - block, 0x01 - unblock, 0x03 - allblock, 0x04 - allunblock

		if(_type == BLOCK || _type == UNBLOCK)
		{
			_name = readS();
			_target = L2World.getInstance().getPlayer(_name);
		}
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if(activeChar == null)
			return;

		switch(_type)
		{
			case BLOCK:
			case UNBLOCK:
				if(_target == null)
				{
					// Incorrect player name.
					activeChar.sendPacket(new SystemMessage(SystemMessageId.FAILED_TO_REGISTER_TO_IGNORE_LIST));
					return;
				}

				if(_target.isGM())
				{
					// Cannot block a GM character.
					activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_MAY_NOT_IMPOSE_A_BLOCK_ON_GM));
					return;
				}

				if(_type == BLOCK)
				{
					BlockList.addToBlockList(activeChar, _target);
				}
				else
				{
					BlockList.removeFromBlockList(activeChar, _target);
				}

				break;
			case BLOCKLIST:
				BlockList.sendListToOwner(activeChar);
				break;
			case ALLBLOCK:
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.MESSAGE_REFUSAL_MODE));//Update by rocknow
				BlockList.setBlockAll(activeChar, true);
				activeChar.setMessageRefusal(true);
				break;
			case ALLUNBLOCK:
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.MESSAGE_ACCEPTANCE_MODE));//Update by rocknow
				BlockList.setBlockAll(activeChar, false);
				activeChar.setMessageRefusal(false);
				break;
			default:
				_log.info("Unknown 0x0a block type: " + _type);
		}
	}

	@Override
	public String getType()
	{
		return "[C] A0 RequestBlock";
	}

}