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

import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.SystemMessage;

public final class Action extends L2GameClientPacket
{
	private static final String ACTION__C__04 = "[C] 04 Action";
	private static Logger _log = Logger.getLogger(Action.class.getName());

	private int _objectId;
	@SuppressWarnings("unused")
	private int _originX;
	@SuppressWarnings("unused")
	private int _originY;
	@SuppressWarnings("unused")
	private int _originZ;
	private int _actionId;

	private boolean _removeSpawnProtection = false;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_originX = readD();
		_originY = readD();
		_originZ = readD();
		_actionId = readC();
	}

	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();

		if(activeChar == null)
		{
			return;
		}

		if (activeChar.inObserverMode())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE));
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		final L2Object obj;

		if(activeChar.getTargetId() == _objectId)
		{
			obj = activeChar.getTarget();
			_removeSpawnProtection = true;
		}
		else
		{
			obj = L2World.getInstance().findObject(_objectId);
		}

		if(obj == null)
		{
			getClient().sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(activeChar.getPrivateStoreType() == 0 && activeChar.getActiveRequester() == null)
		{
			switch(_actionId)
			{
				case 0:
					obj.onAction(activeChar);
					break;
				case 1:
					if(obj instanceof L2Character && ((L2Character) obj).isAlikeDead())
					{
						obj.onAction(activeChar);
					}
					else
					{
						obj.onActionShift(getClient());
					}
					break;
				default:
					_log.warning("Character: " + activeChar.getName() + " requested invalid action: " + _actionId);
					getClient().sendPacket(ActionFailed.STATIC_PACKET);
					break;
			}
		}
		else
		{
			getClient().sendPacket(ActionFailed.STATIC_PACKET);
			
			activeChar.broadcastStatusUpdate();
		}
	}

	protected boolean triggersOnActionRequest()
	{
		return _removeSpawnProtection;
	}

	@Override
	public String getType()
	{
		return ACTION__C__04;
	}

}