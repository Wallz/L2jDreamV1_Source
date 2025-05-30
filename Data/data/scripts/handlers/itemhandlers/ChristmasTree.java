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
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.thread.ThreadPoolManager;

public class ChristmasTree implements IItemHandler
{
	private static final int[] ITEM_IDS =
	{
			5560,
			5561
	};

	private static final int[] NPC_IDS =
	{
			13006,
			13007
	};

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		L2PcInstance activeChar = (L2PcInstance) playable;

		L2NpcTemplate template1 = null;

		int itemId = item.getItemId();
		for(int i = 0; i < ITEM_IDS.length; i++)
		{
			if(ITEM_IDS[i] == itemId)
			{
				template1 = NpcTable.getInstance().getTemplate(NPC_IDS[i]);
				break;
			}
		}

		if(template1 == null)
		{
			return;
		}

		L2Object target = activeChar.getTarget();
		if(target == null)
		{
			target = activeChar;
		}

		try
		{
			L2Spawn spawn = new L2Spawn(template1);
			spawn.setId(IdFactory.getInstance().getNextId());
			spawn.setLocx(target.getX());
			spawn.setLocy(target.getY());
			spawn.setLocz(target.getZ());
			L2Npc result = spawn.spawnOne();

			activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);

			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_S2));

			ThreadPoolManager.getInstance().scheduleGeneral(new DeSpawn(result), 3600000);

			spawn = null;
		}
		catch(Exception e)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("Target is not ingame."));
		}

		activeChar = null;
		template1 = null;
		target = null;
	}

	public class DeSpawn implements Runnable
	{
		L2Npc _npc = null;

		public DeSpawn(L2Npc npc)
		{
			_npc = npc;
		}

		public void run()
		{
			_npc.onDecay();
		}
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}

}