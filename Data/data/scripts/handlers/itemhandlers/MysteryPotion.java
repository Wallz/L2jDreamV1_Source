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
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.MagicSkillUser;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.thread.ThreadPoolManager;

public class MysteryPotion implements IItemHandler
{
	private static final int[] ITEM_IDS =
	{
		5234
	};
	private static final int BIGHEAD_EFFECT = 0x2000;
	private static final int MYSTERY_POTION_SKILL = 2103;
	private static final int EFFECT_DURATION = 1200000;

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(!(playable instanceof L2PcInstance))
		{
			return;
		}

		L2PcInstance activeChar = (L2PcInstance) playable;

		MagicSkillUser MSU = new MagicSkillUser(playable, playable, 2103, 1, 0, 0);
		activeChar.sendPacket(MSU);
		activeChar.broadcastPacket(MSU);

		MSU = null;

		activeChar.startAbnormalEffect(BIGHEAD_EFFECT);
		activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);

		activeChar.sendPacket(new SystemMessage(SystemMessageId.USE_S1).addSkillName(MYSTERY_POTION_SKILL));
		activeChar = null;

		MysteryPotionStop mp = new MysteryPotionStop(playable);
		ThreadPoolManager.getInstance().scheduleEffect(mp, EFFECT_DURATION);

		mp = null;
	}

	public class MysteryPotionStop implements Runnable
	{
		private L2Playable _playable;

		public MysteryPotionStop(L2Playable playable)
		{
			_playable = playable;
		}

		public void run()
		{
			try
			{
				if(!(_playable instanceof L2PcInstance))
				{
					return;
				}

				((L2PcInstance) _playable).stopAbnormalEffect(BIGHEAD_EFFECT);
			}
			catch(Throwable t)
			{
			}
		}
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}

}