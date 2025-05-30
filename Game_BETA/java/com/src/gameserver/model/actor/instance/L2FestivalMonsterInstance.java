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
package com.src.gameserver.model.actor.instance;

import com.src.gameserver.model.L2Party;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.entity.sevensigns.SevenSignsFestival;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public class L2FestivalMonsterInstance extends L2MonsterInstance
{
	protected int _bonusMultiplier = 1;

	public L2FestivalMonsterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	public void setOfferingBonus(int bonusMultiplier)
	{
		_bonusMultiplier = bonusMultiplier;
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		if(attacker instanceof L2FestivalMonsterInstance)
		{
			return false;
		}

		return true;
	}

	@Override
	public boolean isAggressive()
	{
		return true;
	}

	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}

	@Override
	public void doItemDrop(L2Character lastAttacker)
	{
		L2PcInstance killingChar = null;

		if(!(lastAttacker instanceof L2PcInstance))
		{
			return;
		}

		killingChar = (L2PcInstance) lastAttacker;
		L2Party associatedParty = killingChar.getParty();

		killingChar = null;

		if(associatedParty == null)
		{
			return;
		}

		L2PcInstance partyLeader = associatedParty.getPartyMembers().get(0);
		L2ItemInstance addedOfferings = partyLeader.getInventory().addItem("Sign", SevenSignsFestival.FESTIVAL_OFFERING_ID, _bonusMultiplier, partyLeader, this);

		associatedParty = null;

		InventoryUpdate iu = new InventoryUpdate();

		if(addedOfferings.getCount() != _bonusMultiplier)
		{
			iu.addModifiedItem(addedOfferings);
		}
		else
		{
			iu.addNewItem(addedOfferings);
		}

		addedOfferings = null;

		partyLeader.sendPacket(iu);

		super.doItemDrop(lastAttacker);
	}
}