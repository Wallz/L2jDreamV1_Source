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
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.handler.IItemHandler;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.model.entity.siege.Castle;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.SystemMessage;

public class ScrollOfResurrection implements IItemHandler
{
	private static final int[] ITEM_IDS =
	{
			737, 3936, 3959, 6387
	};

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(!(playable instanceof L2PcInstance))
		{
			return;
		}

		L2PcInstance activeChar = (L2PcInstance) playable;

		if(activeChar.isSitting())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_MOVE_SITTING));
			return;
		}

		if(activeChar.isInOlympiadMode() || activeChar.inObserverMode())
		{
			activeChar.sendMessage("This item can not be used in olympiad games.");
		}
		if (activeChar.isFightingInEvent())
		{
			if ((activeChar.getEventName().equals("TVT") && !Config.TVT_ALLOW_SCROLL)
					|| (activeChar.getEventName().equals("CTF") && !Config.CTF_ALLOW_SCROLL)
					|| (activeChar.getEventName().equals("BW") && !Config.BW_ALLOW_SCROLL)
					|| (activeChar.getEventName().equals("DM") && !Config.DM_ALLOW_SCROLL))
			 	{
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			 		return;
			 	}
		}

		if(activeChar.isMovementDisabled())
		{
			return;
		}

		int itemId = item.getItemId();
		boolean humanScroll = itemId == 3936 || itemId == 3959 || itemId == 737;
		boolean petScroll = itemId == 6387 || itemId == 737;

		L2Character target = (L2Character) activeChar.getTarget();

		if(target != null && target.isDead())
		{
			L2PcInstance targetPlayer = null;
			if(target instanceof L2PcInstance)
			{
				targetPlayer = (L2PcInstance) target;
			}

			L2PetInstance targetPet = null;
			if(target instanceof L2PetInstance)
			{
				targetPet = (L2PetInstance) target;
			}

			target = null;

			if(targetPlayer != null || targetPet != null)
			{
				boolean condGood = true;

				Castle castle = null;

				if(targetPlayer != null)
				{
					castle = CastleManager.getInstance().getCastle(targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ());
				}
				else
				{
					castle = CastleManager.getInstance().getCastle(targetPet.getX(), targetPet.getY(), targetPet.getZ());
				}

				if(castle != null && castle.getSiege().getIsInProgress())
				{
					condGood = false;
					activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE));
				}

				castle = null;

				if(targetPet != null)
				{
					if(targetPet.getOwner() != activeChar)
					{
						if(targetPet.getOwner().isReviveRequested())
						{
							if(targetPet.getOwner().isRevivingPet())
							{
								activeChar.sendPacket(new SystemMessage(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED));
							}
							else
							{
								activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_RES_PET2));
							}
							condGood = false;
						}
					}
					else if(!petScroll)
					{
						condGood = false;
						activeChar.sendMessage("You do not have the correct scroll");
					}
				}
				else
				{
					if(targetPlayer.isFestivalParticipant())
					{
						condGood = false;
						activeChar.sendPacket(SystemMessage.sendString("You may not resurrect participants in a festival."));
					}

					if(targetPlayer.isReviveRequested())
					{
						if(targetPlayer.isRevivingPet())
						{
							activeChar.sendPacket(new SystemMessage(SystemMessageId.MASTER_CANNOT_RES));
						}
						else
						{
							activeChar.sendPacket(new SystemMessage(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED));
						}
						condGood = false;
					}
					else if(!humanScroll)
					{
						condGood = false;
						activeChar.sendMessage("You do not have the correct scroll");
					}
				}

				if(condGood)
				{
					if(!activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false))
					{
						return;
					}

					int skillId = 0;
					int skillLevel = 1;

					switch(itemId)
					{
						case 737:
							skillId = 2014;
							break;
						case 3936:
							skillId = 2049;
							break;
						case 3959:
							skillId = 2062;
							break;
						case 6387:
							skillId = 2179;
							break;
						case 9157:
							skillId = 2321;
							break;
					}

					if(skillId != 0)
					{
						L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
						activeChar.useMagic(skill, true, true);
						skill = null;

						activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_DISAPPEARED).addItemName(itemId));
					}
				}
			}
		}
		else
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
		}

		activeChar = null;
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}

}