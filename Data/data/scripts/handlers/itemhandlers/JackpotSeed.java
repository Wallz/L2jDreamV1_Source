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

import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.handler.IItemHandler;
import com.src.gameserver.idfactory.IdFactory;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2GourdInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public class JackpotSeed implements IItemHandler
{
	private L2GourdInstance _gourd = null;

	private static int[] _itemIds =
	{
			6389,
			6390
	};

	private static int[] _npcIds =
	{
			12774,
			12777
	};

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		L2PcInstance activeChar = (L2PcInstance) playable;
		L2NpcTemplate template1 = null;
		int itemId = item.getItemId();
		for(int i = 0; i < _itemIds.length; i++)
		{
			if(_itemIds[i] == itemId)
			{
				template1 = NpcTable.getInstance().getTemplate(_npcIds[i]);
				break;
			}
		}

		if(template1 == null)
		{
			return;
		}

		try
		{
			L2Spawn spawn = new L2Spawn(template1);
			spawn.setId(IdFactory.getInstance().getNextId());
			spawn.setLocx(activeChar.getX());
			spawn.setLocy(activeChar.getY());
			spawn.setLocz(activeChar.getZ());
			_gourd = (L2GourdInstance) spawn.spawnOne();
			L2World.getInstance();
			L2World.storeObject(_gourd);
			_gourd.setOwner(activeChar.getName());
			activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("Created " + template1.name + " at x: " + spawn.getLocx() + " y: " + spawn.getLocy() + " z: " + spawn.getLocz()));
		}
		catch(Exception e)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("Target is not ingame."));
		}
		activeChar = null;
		template1 = null;
	}

	public int[] getItemIds()
	{
		return _itemIds;
	}

}