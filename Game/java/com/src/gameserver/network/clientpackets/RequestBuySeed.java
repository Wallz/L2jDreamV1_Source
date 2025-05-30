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

import com.src.Config;
import com.src.gameserver.datatables.sql.ItemTable;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.managers.CastleManorManager;
import com.src.gameserver.managers.CastleManorManager.SeedProduction;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2ManorManagerInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.siege.Castle;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.item.L2Item;
import com.src.gameserver.util.Util;

public class RequestBuySeed extends L2GameClientPacket
{
	private static final String _C__C4_REQUESTBUYSEED = "[C] C4 RequestBuySeed";

	private int _count;
	private int _manorId;
	private int[] _items;

	@Override
	protected void readImpl()
	{
		_manorId = readD();
		_count = readD();

		if(_count > 500 || _count * 12 < _buf.remaining() || _count < 1)
		{
			_count = 0;
			return;
		}

		_items = new int[_count * 2];

		for(int i = 0; i < _count; i++)
		{
			_items[i * 2 + 0] = readD();
			long cnt = readD();

			if(cnt > Integer.MAX_VALUE || cnt < 1)
			{
				_count = 0;
				_items = null;
				return;
			}

			_items[i * 2 + 1] = (int) cnt;
		}
	}

	@Override
	protected void runImpl()
	{
		long totalPrice = 0;
		int slots = 0;
		int totalWeight = 0;

		L2PcInstance player = getClient().getActiveChar();
		if(player == null)
		{
			return;
		}

		if(_count < 1)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		L2Object target = player.getTarget();

		if(!(target instanceof L2ManorManagerInstance))
		{
			target = player.getLastFolkNPC();
		}

		if(!(target instanceof L2ManorManagerInstance))
		{
			return;
		}

		Castle castle = CastleManager.getInstance().getCastleById(_manorId);

		for(int i = 0; i < _count; i++)
		{
			int seedId = _items[i * 2 + 0];
			int count = _items[i * 2 + 1];
			int price = 0;
			int residual = 0;

			SeedProduction seed = castle.getSeed(seedId, CastleManorManager.PERIOD_CURRENT);
			price = seed.getPrice();
			residual = seed.getCanProduce();

			if(price <= 0)
			{
				return;
			}

			if(residual < count)
			{
				return;
			}

			totalPrice += count * price;

			L2Item template = ItemTable.getInstance().getTemplate(seedId);
			totalWeight += count * template.getWeight();

			if(!template.isStackable())
			{
				slots += count;
			}
			else if(player.getInventory().getItemByItemId(seedId) == null)
			{
				slots++;
			}
		}

		if(totalPrice > Integer.MAX_VALUE)
		{
			Util.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to purchase over " + Integer.MAX_VALUE + " adena limit.", Config.DEFAULT_PUNISH);
			return;
		}

		if(!player.getInventory().validateWeight(totalWeight))
		{
			sendPacket(new SystemMessage(SystemMessageId.WEIGHT_LIMIT_EXCEEDED));
			return;
		}

		if(!player.getInventory().validateCapacity(slots))
		{
			sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
			return;
		}

		if(totalPrice < 0 || !player.reduceAdena("Buy", (int) totalPrice, target, false))
		{
			sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			return;
		}

		castle.addToTreasuryNoTax((int) totalPrice);

		InventoryUpdate playerIU = new InventoryUpdate();
		for(int i = 0; i < _count; i++)
		{
			int seedId = _items[i * 2 + 0];
			int count = _items[i * 2 + 1];

			if(count < 0)
			{
				count = 0;
			}

			SeedProduction seed = castle.getSeed(seedId, CastleManorManager.PERIOD_CURRENT);
			seed.setCanProduce(seed.getCanProduce() - count);

			if(Config.ALT_MANOR_SAVE_ALL_ACTIONS)
			{
				CastleManager.getInstance().getCastleById(_manorId).updateSeed(seed.getId(), seed.getCanProduce(), CastleManorManager.PERIOD_CURRENT);
			}

			L2ItemInstance item = player.getInventory().addItem("Buy", seedId, count, player, target);

			if(item.getCount() > count)
			{
				playerIU.addModifiedItem(item);
			}
			else
			{
				playerIU.addNewItem(item);
			}

			player.sendPacket(new SystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(seedId).addNumber(count));
		}
		player.sendPacket(playerIU);

		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
	}

	@Override
	public String getType()
	{
		return _C__C4_REQUESTBUYSEED;
	}

}