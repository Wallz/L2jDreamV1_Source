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

import com.src.Config;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.templates.skills.L2SkillType;

public final class RequestMagicSkillUse extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestMagicSkillUse.class.getName());

	private static final String _C__2F_REQUESTMAGICSKILLUSE = "[C] 2F RequestMagicSkillUse";

	private int _magicId;
	private boolean _ctrlPressed;
	private boolean _shiftPressed;

	@Override
	protected void readImpl()
	{
		_magicId = readD();
		_ctrlPressed = readD() != 0;
		_shiftPressed = readC() != 0;
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();

		if(activeChar == null)
		{
			return;
		}

		int level = activeChar.getSkillLevel(_magicId);

		if(level <= 0)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(activeChar.isOutOfControl())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		L2Skill skill = SkillTable.getInstance().getInfo(_magicId, level);

		if(skill != null)
		{
			if(skill.getSkillType() == L2SkillType.RECALL && !Config.KARMA_PLAYER_CAN_TELEPORT && activeChar.getKarma() > 0)
			{
				return;
			}

			activeChar.useMagic(skill, _ctrlPressed, _shiftPressed);
		}
		else
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			_log.warning("No skill found!!");
		}
	}

	@Override
	public String getType()
	{
		return _C__2F_REQUESTMAGICSKILLUSE;
	}

}