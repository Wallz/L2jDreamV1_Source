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

import com.src.Config;
import com.src.gameserver.handler.IItemHandler;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.ExAutoSoulShot;
import com.src.gameserver.network.serverpackets.MagicSkillUser;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.Stats;
import com.src.gameserver.templates.item.L2Item;
import com.src.gameserver.templates.item.L2Weapon;
import com.src.gameserver.util.Broadcast;

public class SoulShots implements IItemHandler
{
	private static final int[] ITEM_IDS =
	{
			5789, 1835, 1463, 1464, 1465, 1466, 1467
	};
	private static final int[] SKILL_IDS =
	{
			2039, 2150, 2151, 2152, 2153, 2154
	};

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(!(playable instanceof L2PcInstance))
		{
			return;
		}

		L2PcInstance activeChar = (L2PcInstance) playable;
		L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
		L2Weapon weaponItem = activeChar.getActiveWeaponItem();
		int itemId = item.getItemId();

		if(weaponInst == null || weaponItem.getSoulShotCount() == 0)
		{
			if(!activeChar.getAutoSoulShot().containsKey(itemId))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_USE_SOULSHOTS));
			}

			return;
		}

		if(activeChar.isParalyzed())
		{
			activeChar.sendMessage("You can not use this while You are paralyzed.");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		int weaponGrade = weaponItem.getCrystalType();
		if(weaponGrade == L2Item.CRYSTAL_NONE && itemId != 5789 && itemId != 1835 || weaponGrade == L2Item.CRYSTAL_D && itemId != 1463 || weaponGrade == L2Item.CRYSTAL_C && itemId != 1464 || weaponGrade == L2Item.CRYSTAL_B && itemId != 1465 || weaponGrade == L2Item.CRYSTAL_A && itemId != 1466 || weaponGrade == L2Item.CRYSTAL_S && itemId != 1467)
		{
			if(!activeChar.getAutoSoulShot().containsKey(itemId))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.SOULSHOTS_GRADE_MISMATCH));
			}

			return;
		}

		activeChar.soulShotLock.lock();
		try
		{
			if(weaponInst.getChargedSoulshot() != L2ItemInstance.CHARGED_NONE)
			{
				return;
			}

			int saSSCount = (int) activeChar.getStat().calcStat(Stats.SOULSHOT_COUNT, 0, null, null);
			int SSCount = saSSCount == 0 ? weaponItem.getSoulShotCount() : saSSCount;

			weaponItem = null;

			if(!Config.DONT_DESTROY_SS)
			{
				if(!activeChar.destroyItemWithoutTrace("Consume", item.getObjectId(), SSCount, null, false))
				{
					if(activeChar.getAutoSoulShot().containsKey(itemId))
					{
						activeChar.removeAutoSoulShot(itemId);
						activeChar.sendPacket(new ExAutoSoulShot(itemId, 0));

						activeChar.sendPacket(new SystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED).addString(item.getItem().getName()));
					}
					else
					{
						activeChar.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_SOULSHOTS));
					}
					return;
				}
			}

			weaponInst.setChargedSoulshot(L2ItemInstance.CHARGED_SOULSHOT);

			weaponInst = null;
		}
		finally
		{
			activeChar.soulShotLock.unlock();
		}

		activeChar.sendPacket(new SystemMessage(SystemMessageId.ENABLED_SOULSHOT));
		Broadcast.toSelfAndKnownPlayersInRadius(activeChar, new MagicSkillUser(activeChar, activeChar, SKILL_IDS[weaponGrade], 1, 0, 0), 360000);

		activeChar = null;
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}

}