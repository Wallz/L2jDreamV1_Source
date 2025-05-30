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

import java.nio.BufferUnderflowException;

import com.src.Config;
import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.position.L2CharPosition;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.EnchantResult;
import com.src.gameserver.network.serverpackets.PartyMemberPosition;
import com.src.gameserver.network.serverpackets.StopMove;
import com.src.gameserver.templates.item.L2WeaponType;
import com.src.gameserver.thread.TaskPriority;
import com.src.gameserver.util.IllegalPlayerAction;
import com.src.gameserver.util.Util;

public final class MoveBackwardToLocation extends L2GameClientPacket
{
	private static final String _C__01_MOVEBACKWARDTOLOC = "[C] 01 MoveBackwardToLoc";

	private int _targetX;
	private int _targetY;
	private int _targetZ;

	private int _originX;

	private int _originY;

	private int _originZ;

	private int _moveMovement;

	private int _curX;
	private int _curY;

	@SuppressWarnings("unused")
	private int _curZ;

	public TaskPriority getPriority()
	{
		return TaskPriority.PR_HIGH;
	}

	@Override
	protected void readImpl()
	{
		_targetX = readD();
		_targetY = readD();
		_targetZ = readD();
		_originX = readD();
		_originY = readD();
		_originZ = readD();
		try
		{
			_moveMovement = readD();
		}
		catch(BufferUnderflowException e)
		{
			if(Config.L2WALKER_PROTEC)
			{
				L2PcInstance activeChar = getClient().getActiveChar();
				activeChar.sendPacket(SystemMessageId.HACKING_TOOL);
				Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " trying to use l2walker!", IllegalPlayerAction.PUNISH_KICK);
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

        if(activeChar.getPrivateStoreType() != 0)
        {
                getClient().sendPacket(ActionFailed.STATIC_PACKET);
                return;
        }
        
        if(activeChar.isTeleporting())
        {
        	activeChar.sendPacket(ActionFailed.STATIC_PACKET);
        	return;
        }
        if(activeChar.getActiveEnchantItem() != null)
        {
        	activeChar.sendPacket(new EnchantResult(0));
        	activeChar.setActiveEnchantItem(null);
        }
        
        if(Config.PLAYER_MOVEMENT_BLOCK_TIME > 0 && !activeChar.isGM() && activeChar.getNotMoveUntil() > System.currentTimeMillis()) 
        { 
        	activeChar.sendPacket(ActionFailed.STATIC_PACKET); 
        	return; 
        }
        
        if (_targetX == _originX && _targetY == _originY && _targetZ == _originZ) 
        {
                activeChar.sendPacket(new StopMove(activeChar));
                return;
        }

		_curX = activeChar.getX();
		_curY = activeChar.getY();
		_curZ = activeChar.getZ();

		if(activeChar.isInBoat())
		{
			activeChar.setInBoat(false);
		}

		if(activeChar.getTeleMode() > 0)
		{
			if(activeChar.getTeleMode() == 1)
			{
				activeChar.setTeleMode(0);
			}

			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			activeChar.teleToLocation(_targetX, _targetY, _targetZ, false);
			return;
		}
			
		if(_moveMovement == 0 && !Config.ALLOW_USE_CURSOR_FOR_WALK)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else if(activeChar.isAttackingNow() && activeChar.getActiveWeaponItem() != null && activeChar.getActiveWeaponItem().getItemType() == L2WeaponType.BOW)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else
		{
			double dx = _targetX - _curX;
			double dy = _targetY - _curY;

			if(activeChar.isOutOfControl() || dx * dx + dy * dy > 98010000)
			{
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(_targetX, _targetY, _targetZ, 0));

			if(activeChar.getParty() != null)
			{
				activeChar.getParty().broadcastToPartyMembers(activeChar, new PartyMemberPosition(activeChar));
			}
		}
	}

	@Override
	public String getType()
	{
		return _C__01_MOVEBACKWARDTOLOC;
	}

}