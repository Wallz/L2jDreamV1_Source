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
import com.src.gameserver.templates.item.L2Item;
import com.src.gameserver.templates.item.L2Weapon;
import com.src.gameserver.util.Broadcast;

public class SpiritShot implements IItemHandler
{
	private static final int[] ITEM_IDS =
	{
			5790, 2509, 2510, 2511, 2512, 2513, 2514
	};
	private static final int[] SKILL_IDS =
	{
			2061, 2155, 2156, 2157, 2158, 2159
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

		if(weaponInst == null || weaponItem.getSpiritShotCount() == 0)
		{
			if(!activeChar.getAutoSoulShot().containsKey(itemId))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_USE_SPIRITSHOTS));
			}
			return;
		}

		if(activeChar.isParalyzed())
		{
			activeChar.sendMessage("You can not use this while You are paralyzed.");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(weaponInst.getChargedSpiritshot() != L2ItemInstance.CHARGED_NONE)
		{
			return;
		}

		int weaponGrade = weaponItem.getCrystalType();
		if(weaponGrade == L2Item.CRYSTAL_NONE && itemId != 5790 && itemId != 2509 || weaponGrade == L2Item.CRYSTAL_D && itemId != 2510 || weaponGrade == L2Item.CRYSTAL_C && itemId != 2511 || weaponGrade == L2Item.CRYSTAL_B && itemId != 2512 || weaponGrade == L2Item.CRYSTAL_A && itemId != 2513 || weaponGrade == L2Item.CRYSTAL_S && itemId != 2514)
		{
			if(!activeChar.getAutoSoulShot().containsKey(itemId))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.SPIRITSHOTS_GRADE_MISMATCH));
			}
			return;
		}

		if(!Config.DONT_DESTROY_SS)
		{
			if(!activeChar.destroyItemWithoutTrace("Consume", item.getObjectId(), weaponItem.getSpiritShotCount(), null, false))
			{
				if(activeChar.getAutoSoulShot().containsKey(itemId))
				{
					activeChar.removeAutoSoulShot(itemId);
					activeChar.sendPacket(new ExAutoSoulShot(itemId, 0));

					activeChar.sendPacket(new SystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED).addString(item.getItem().getName()));
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_SPIRITSHOTS));
				}
				return;
			}
		}
		weaponItem = null;

		weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_SPIRITSHOT);

		weaponInst = null;

		activeChar.sendPacket(new SystemMessage(SystemMessageId.ENABLED_SPIRITSHOT));
		Broadcast.toSelfAndKnownPlayersInRadius(activeChar, new MagicSkillUser(activeChar, activeChar, SKILL_IDS[weaponGrade], 1, 0, 0), 360000);

		activeChar = null;
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}

}