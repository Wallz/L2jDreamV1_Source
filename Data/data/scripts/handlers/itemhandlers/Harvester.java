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
package handlers.itemhandlers;

import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.handler.IItemHandler;
import com.src.gameserver.managers.CastleManorManager;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2MonsterInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class Harvester implements IItemHandler
{

	private static final int[] ITEM_IDS =
	{
		5125
	};
	L2PcInstance _activeChar;
	L2MonsterInstance _target;

	public void useItem(L2Playable playable, L2ItemInstance _item)
	{
		if(!(playable instanceof L2PcInstance))
		{
			return;
		}

		if(CastleManorManager.getInstance().isDisabled())
		{
			return;
		}

		_activeChar = (L2PcInstance) playable;
		if(_activeChar.getTarget() == null || !(_activeChar.getTarget() instanceof L2MonsterInstance))
		{
			_activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
			_activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		_target = (L2MonsterInstance) _activeChar.getTarget();
		if(_target == null || !_target.isDead())
		{
			_activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		L2Skill skill = SkillTable.getInstance().getInfo(2098, 1);
		_activeChar.useMagic(skill, false, false);
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}

}