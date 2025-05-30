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

import com.src.gameserver.handler.IItemHandler;
import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.MagicSkillUser;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.skills.L2EffectType;
import com.src.gameserver.templates.skills.L2SkillType;

public class Remedy implements IItemHandler
{
	private static int[] ITEM_IDS =
	{
		1831, 1832, 1833, 1834, 3889
	};

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		L2PcInstance activeChar;

		if(playable instanceof L2PcInstance)
		{
			activeChar = (L2PcInstance) playable;
		}
		else if(playable instanceof L2PetInstance)
		{
			activeChar = ((L2PetInstance) playable).getOwner();
		}
		else
		{
			return;
		}

		if(activeChar.isInOlympiadMode())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
			return;
		}

		int itemId = item.getItemId();
		if(itemId == 1831)
		{
			L2Effect[] effects = activeChar.getAllEffects();
			for(L2Effect e : effects)
			{
				if(e.getSkill().getSkillType() == L2SkillType.POISON && e.getSkill().getLevel() <= 3)
				{
					e.exit();
					break;
				}
			}
			effects = null;
			MagicSkillUser MSU = new MagicSkillUser(playable, playable, 2042, 1, 0, 0);
			activeChar.sendPacket(MSU);
			activeChar.broadcastPacket(MSU);
			MSU = null;
			playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
		}
		else if(itemId == 1832)
		{
			L2Effect[] effects = activeChar.getAllEffects();
			for(L2Effect e : effects)
			{
				if(e.getSkill().getSkillType() == L2SkillType.POISON && e.getSkill().getLevel() <= 7)
				{
					e.exit();
					break;
				}
			}
			effects = null;
			MagicSkillUser MSU = new MagicSkillUser(playable, playable, 2043, 1, 0, 0);
			activeChar.sendPacket(MSU);
			activeChar.broadcastPacket(MSU);
			MSU = null;
			playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
		}
		else if(itemId == 1833)
		{
			L2Effect[] effects = activeChar.getAllEffects();
			for(L2Effect e : effects)
			{
				if(e.getSkill().getSkillType() == L2SkillType.BLEED && e.getSkill().getLevel() <= 3)
				{
					e.exit();
					break;
				}
			}
			effects = null;
			MagicSkillUser MSU = new MagicSkillUser(playable, playable, 34, 1, 0, 0);
			activeChar.sendPacket(MSU);
			activeChar.broadcastPacket(MSU);
			MSU = null;
			playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
		}
		else if(itemId == 1834)
		{
			L2Effect[] effects = activeChar.getAllEffects();
			for(L2Effect e : effects)
			{
				if(e.getSkill().getSkillType() == L2SkillType.BLEED && e.getSkill().getLevel() <= 7)
				{
					e.exit();
					break;
				}
			}
			effects = null;
			MagicSkillUser MSU = new MagicSkillUser(playable, playable, 2045, 1, 0, 0);
			activeChar.sendPacket(MSU);
			activeChar.broadcastPacket(MSU);
			MSU = null;
			playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
		}
		else if(itemId == 3889)
		{
			L2Effect[] effects = activeChar.getAllEffects();
			for(L2Effect e : effects)
			{
				if(e.getSkill().getId() == 4082)
				{
					e.exit();
				}
			}

			effects = null;
			activeChar.setIsImobilised(false);

			if(activeChar.getFirstEffect(L2EffectType.ROOT) == null)
			{
				activeChar.stopRooting(null);
			}

			MagicSkillUser MSU = new MagicSkillUser(playable, playable, 2042, 1, 0, 0);
			activeChar.sendPacket(MSU);
			activeChar.broadcastPacket(MSU);
			MSU = null;
			playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
		}

		activeChar = null;
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}