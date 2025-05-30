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
package com.src.gameserver.model;

import com.src.Config;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.sql.ItemTable;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class CombatFlag
{
	protected L2PcInstance _player = null;
	public int playerId = 0;
	private L2ItemInstance _item = null;

	private Location _location;
	public L2ItemInstance itemInstance;

	private int _itemId;

	public CombatFlag(int x, int y, int z, int heading, int item_id)
	{
		_location = new Location(x, y, z, heading);
		_itemId = item_id;
	}

	public synchronized void spawnMe()
	{
		L2ItemInstance i;

		i = ItemTable.getInstance().createItem("Combat", _itemId, 1, null, null);
		i.spawnMe(_location.getX(), _location.getY(), _location.getZ());
		itemInstance = i;
		i = null;
	}

	public synchronized void unSpawnMe()
	{
		if(_player != null)
		{
			dropIt();
		}

		if(itemInstance != null)
		{
			itemInstance.decayMe();
		}
	}

	public void activate(L2PcInstance player, L2ItemInstance item)
	{
		if(player.isMounted())
		{
			if(!player.dismount())
			{
				player.sendMessage("You may not pick up this item while riding in this territory.");
				return;
			}
		}

		_player = player;
		playerId = _player.getObjectId();
		itemInstance = null;

		giveSkill();

		_item = item;
		_player.getInventory().equipItemAndRecord(_item);

		_player.sendPacket(new SystemMessage(SystemMessageId.S1_EQUIPPED).addItemName(_item.getItemId()));

		if(!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate iu = new InventoryUpdate();
			iu.addItem(_item);
			_player.sendPacket(iu);
		}
		else
		{
			_player.sendPacket(new ItemList(_player, false));
		}

		_player.broadcastUserInfo();
	}

	public void dropIt()
	{
		removeSkill();
		_player.destroyItem("DieDrop", _item, null, false);
		_item = null;
		_player.broadcastUserInfo();
		_player = null;
		playerId = 0;
	}

	public void giveSkill()
	{
		_player.addSkill(SkillTable.getInstance().getInfo(3318, 1), false);
		_player.addSkill(SkillTable.getInstance().getInfo(3358, 1), false);
		_player.sendSkillList();
	}

	public void removeSkill()
	{
		_player.removeSkill(SkillTable.getInstance().getInfo(3318, 1), false);
		_player.removeSkill(SkillTable.getInstance().getInfo(3358, 1), false);
		_player.sendSkillList();
	}
}