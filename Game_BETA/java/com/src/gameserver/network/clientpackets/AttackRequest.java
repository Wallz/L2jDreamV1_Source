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

import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.SystemMessage;

public final class AttackRequest extends L2GameClientPacket
{
	private int _objectId;

	@SuppressWarnings("unused")
	private int _originX, _originY, _originZ;

	@SuppressWarnings("unused")
	private int _attackId;

	private static final String _C__0A_ATTACKREQUEST = "[C] 0A AttackRequest";

	@Override
	protected void readImpl()
	{
		_objectId = readD();
		_originX = readD();
		_originY = readD();
		_originZ = readD();
		_attackId = readC();
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if(activeChar == null)
		{
			return;
		}
		
		if (System.currentTimeMillis() - activeChar.getLastAttackPacket() < 500)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		activeChar.setLastAttackPacket();
		
		if (activeChar.inObserverMode())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE));
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		L2Object target;

		if(activeChar.getTargetId() == _objectId)
		{
			target = activeChar.getTarget();
		}
		else
		{
			target = L2World.getInstance().findObject(_objectId);
		}

		if(target == null)
		{
			return;
		}
		if(target instanceof L2PcInstance && ((L2PcInstance) target).getAppearance().getInvisible() && !activeChar.isGM())
			return;
		
		if(activeChar.getTarget() != target)
		{
			target.onAction(activeChar);
		}
		else
		{
			if(target.getObjectId() != activeChar.getObjectId() & activeChar.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_NONE && activeChar.getActiveRequester() == null)
			{
				target.onForcedAttack(activeChar);
			}
			else
			{
				sendPacket(ActionFailed.STATIC_PACKET);
			}
		}
	}

	@Override
	public String getType()
	{
		return _C__0A_ATTACKREQUEST;
	}

}