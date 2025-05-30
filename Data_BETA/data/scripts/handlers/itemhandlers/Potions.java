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

import java.util.logging.Logger;

import com.src.Config;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.handler.IItemHandler;
import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.model.actor.instance.L2SummonInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.MagicSkillUser;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.skills.L2EffectType;

public class Potions implements IItemHandler
{
	protected static final Logger _log = Logger.getLogger(Potions.class.getName());
	
	private static final int[] ITEM_IDS =
	{
		65, 725, 726, 727, 728, 733, 734, 735, 1060, 1061, 1062, 1073,
		1374, 1375, 1539, 1540, 4667, 4679, 4680, 5283, 5591, 5592, 6035, 6036,
		6652, 6653, 6654, 6655, 8193, 8194, 8195, 8196, 8197, 8198,
		8199, 8200, 8201, 8202, 8600, 8601, 8602, 8603, 8604, 8605,
		8606, 8607, 8608, 8609, 8610, 8611, 8612, 8613, 8614, 8622,
		8623, 8624, 8625, 8626, 8627, 8628, 8629, 8630, 8631, 8632,
		8633, 8634, 8635, 8636, 8637, 8638, 8639, 8786, 8787, 9206, 9207
	};

	public synchronized void useItem(L2Playable playable, L2ItemInstance item)
	{
		L2PcInstance activeChar;
		boolean res = false;

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
		if (activeChar.isFightingInEvent())
		{
			if ((activeChar.getEventName().equals("TVT") && !Config.TVT_ALLOW_POTIONS)
					|| (activeChar.getEventName().equals("BW") && !Config.BW_ALLOW_POTIONS)
					|| (activeChar.getEventName().equals("CTF") && !Config.CTF_ALLOW_POTIONS)
					|| (activeChar.getEventName().equals("DM") && !Config.DM_ALLOW_POTIONS))
				{
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
		}

		if(activeChar.isAllSkillsDisabled())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if(!Config.ALLOW_POTS_IN_PVP && (activeChar.isInDuel() || activeChar.getPvpFlag() != 0))
		{
			activeChar.sendMessage("You can not use potions when You are flaged!");
			return;
		}

		int itemId = item.getItemId();
		switch(itemId)
		{
			case 726:
				if(!isEffectReplaceable(activeChar, L2EffectType.MANA_HEAL_OVER_TIME, itemId))
				{
					return;
				}
				res = usePotion(activeChar, 2003, 1);
				break;
			case 728:
				activeChar.setCurrentMp((double)Config.MANA_POTION_RES + activeChar.getCurrentMp());
				StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
				su.addAttribute(11, (int)activeChar.getCurrentMp());
				activeChar.sendPacket(su);
				MagicSkillUser MSU = new MagicSkillUser(activeChar, activeChar, 2005, 1, 0, 0);
				activeChar.broadcastPacket(MSU);
				SystemMessage sm1 = new SystemMessage(SystemMessageId.USE_S1);
				sm1.addItemName(itemId);
				activeChar.sendPacket(sm1);
				activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
				activeChar.sendMessage("restored " + Config.MANA_POTION_RES + " MP." );
				break;
			case 65:
				res = usePotion(activeChar, 2001, 1);
				break;
			case 725:
				if(!isEffectReplaceable(activeChar, L2EffectType.HEAL_OVER_TIME, itemId))
				{
					return;
				}
				res = usePotion(activeChar, 2002, 1);
				break;
			case 727:
				if(!isEffectReplaceable(activeChar, L2EffectType.HEAL_OVER_TIME, itemId))
				{
					return;
				}
				res = usePotion(activeChar, 2032, 1);
				break;
            case 733: // endeavor_potion
                usePotion(activeChar, 2010, 1);
                break;
			case 734:
				res = usePotion(activeChar, 2011, 1);
				break;
			case 735:
				res = usePotion(activeChar, 2012, 1);
				break;
			case 1060:
			case 1073:
				if(!isEffectReplaceable(activeChar, L2EffectType.HEAL_OVER_TIME, itemId))
				{
					return;
				}
				res = usePotion(activeChar, 2031, 1);
				break;
			case 1061:
				if(!isEffectReplaceable(activeChar, L2EffectType.HEAL_OVER_TIME, itemId))
					return;
				res = usePotion(activeChar, 2032, 1);
				break;
			case 1062:
				res = usePotion(activeChar, 2011, 1);
				break;
			case 1374:
				res = usePotion(activeChar, 2034, 1);
				break;
			case 1375:
				res = usePotion(activeChar, 2035, 1);
				break;
			case 1539:
				if(!isEffectReplaceable(activeChar, L2EffectType.HEAL_OVER_TIME, itemId))
				{
					return;
				}
				res = usePotion(activeChar, 2037, 1);
				break;
			case 1540:
				res = usePotion(activeChar, 2038, 1);
				break;
            case 4667: // potion_of_critical_escape
                usePotion(activeChar, 2074, 1);
                break;
			case 4679:
				if(!isEffectReplaceable(activeChar, L2EffectType.HEAL_OVER_TIME, itemId))
				{
					return;
				}
				res = usePotion(activeChar, 2076, 1);
				break;
            case 4680: // rsk_damage_shield_potion
                usePotion(activeChar, 2077, 1);
                break;
			case 5283:
				if(!isEffectReplaceable(activeChar, L2EffectType.HEAL_OVER_TIME, itemId))
				{
					return;
				}
				res = usePotion(activeChar, 2136, 1);
				break;
			case 5591:
				res = usePotion(activeChar, 2166, 1);
				break;
			case 5592:
				res = usePotion(activeChar, 2166, 2);
				break;
			case 6035:
				res = usePotion(activeChar, 2169, 1);
				break;
			case 6036:
				res = usePotion(activeChar, 2169, 2);
				break;
			case 8622:
			case 8623:
			case 8624:
			case 8625:
			case 8626:
			case 8627:
				if((itemId == 8622 && activeChar.getExpertiseIndex() == 0) || (itemId == 8623 && activeChar.getExpertiseIndex() == 1) || (itemId == 8624 && activeChar.getExpertiseIndex() == 2) || (itemId == 8625 && activeChar.getExpertiseIndex() == 3) || (itemId == 8626 && activeChar.getExpertiseIndex() == 4) || (itemId == 8627 && activeChar.getExpertiseIndex() == 5))
				{
					res = usePotion(activeChar, 2287, (activeChar.getExpertiseIndex() + 1));
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.INCOMPATIBLE_ITEM_GRADE).addItemName(itemId));
					return;
				}
				break;
			case 8628:
			case 8629:
			case 8630:
			case 8631:
			case 8632:
			case 8633:
				if((itemId == 8628 && activeChar.getExpertiseIndex() == 0) || (itemId == 8629 && activeChar.getExpertiseIndex() == 1) || (itemId == 8630 && activeChar.getExpertiseIndex() == 2) || (itemId == 8631 && activeChar.getExpertiseIndex() == 3) || (itemId == 8632 && activeChar.getExpertiseIndex() == 4) || (itemId == 8633 && activeChar.getExpertiseIndex() == 5))
				{
					res = usePotion(activeChar, 2288, (activeChar.getExpertiseIndex() + 1));
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.INCOMPATIBLE_ITEM_GRADE).addItemName(itemId));
					return;
				}
				break;
			case 8634:
			case 8635:
			case 8636:
			case 8637:
			case 8638:
			case 8639:
				if((itemId == 8634 && activeChar.getExpertiseIndex() == 0) || (itemId == 8635 && activeChar.getExpertiseIndex() == 1) || (itemId == 8636 && activeChar.getExpertiseIndex() == 2) || (itemId == 8637 && activeChar.getExpertiseIndex() == 3) || (itemId == 8638 && activeChar.getExpertiseIndex() == 4) || (itemId == 8639 && activeChar.getExpertiseIndex() == 5))
				{
					res = usePotion(activeChar, 2289, (activeChar.getExpertiseIndex() + 1));
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessageId.INCOMPATIBLE_ITEM_GRADE).addItemName(itemId));
					return;
				}
				break;
			case 6652:
				res = usePotion(activeChar, 2231, 1);
				break;
			case 6653:
				res = usePotion(activeChar, 2233, 1);
				break;
			case 6654:
				res = usePotion(activeChar, 2233, 1);
				break;
			case 6655:
				res = usePotion(activeChar, 2232, 1);
				break;
			case 8600:
				res = usePotion(activeChar, 2278, 1);
				break;
			case 8601:
				res = usePotion(activeChar, 2278, 2);
				break;
			case 8602:
				res = usePotion(activeChar, 2278, 3);
				break;
			case 8603:
				res = usePotion(activeChar, 2279, 1);
				break;
			case 8604:
				res = usePotion(activeChar, 2279, 2);
				break;
			case 8605:
				res = usePotion(activeChar, 2279, 3);
				break;
			case 8606:
				res = usePotion(activeChar, 2280, 1);
				break;
			case 8607:
				res = usePotion(activeChar, 2281, 1);
				break;
			case 8608:
				res = usePotion(activeChar, 2282, 1);
				break;
			case 8609:
				res = usePotion(activeChar, 2283, 1);
				break;
			case 8610:
				res = usePotion(activeChar, 2284, 1);
				break;
			case 8611:
				res = usePotion(activeChar, 2285, 1);
				break;
			case 8612:
				res = usePotion(activeChar, 2280, 1);
				res = usePotion(activeChar, 2282, 1);
				res = usePotion(activeChar, 2284, 1);
				break;
			case 8613:
				res = usePotion(activeChar, 2281, 1);
				res = usePotion(activeChar, 2283, 1);
				break;
			case 8614:
				res = usePotion(activeChar, 2278, 3);
				res = usePotion(activeChar, 2279, 3);
				break;
			case 8193:
				if(activeChar.getSkillLevel(1315) <= 3)
				{
					playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
					playable.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
					return;
				}
				res = usePotion(activeChar, 2274, 1);
				break;
			case 8194:
				if(activeChar.getSkillLevel(1315) <= 6)
				{
					playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
					playable.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
					return;
				}
				res = usePotion(activeChar, 2274, 2);
				break;
			case 8195:
				if(activeChar.getSkillLevel(1315) <= 9)
				{
					playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
					playable.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
					return;
				}
				res = usePotion(activeChar, 2274, 3);
				break;
			case 8196:
				if(activeChar.getSkillLevel(1315) <= 12)
				{
					playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
					playable.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
					return;
				}
				res = usePotion(activeChar, 2274, 4);
				break;
			case 8197:
				if(activeChar.getSkillLevel(1315) <= 15)
				{
					playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
					playable.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
					return;
				}
				res = usePotion(activeChar, 2274, 5);
				break;
			case 8198:
				if(activeChar.getSkillLevel(1315) <= 18)
				{
					playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
					playable.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
					return;
				}
				res = usePotion(activeChar, 2274, 6);
				break;
			case 8199:
				if(activeChar.getSkillLevel(1315) <= 21)
				{
					playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
					playable.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
					return;
				}
				res = usePotion(activeChar, 2274, 7);
				break;
			case 8200:
				if(activeChar.getSkillLevel(1315) <= 24)
				{
					playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
					playable.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
					return;
				}
				res = usePotion(activeChar, 2274, 8);
				break;
			case 8201:
				res = usePotion(activeChar, 2274, 9);
				break;
			case 8202:
				res = usePotion(activeChar, 2275, 1);
				break;
            case 8786:
                usePotion(playable, 2305, 1);
                break;
            case 8787:
                usePotion(playable, 2305, 1);
                break;
            case 9206:
                if (!isEffectReplaceable(activeChar, L2EffectType.HEAL_OVER_TIME, itemId))
                {
                    return;
                }
                res = usePotion(playable, 2037, 1);
                break;
            case 9207:
                if (!isEffectReplaceable(activeChar, L2EffectType.HEAL_OVER_TIME, itemId))
                {
                    return;
                }
                res = usePotion(playable, 2038, 1);
                break;
			default:
		}

		activeChar = null;

		if(res)
		{
			playable.destroyItem("Consume", item.getObjectId(), 1, null, false);
		}
	}

	private boolean isEffectReplaceable(L2PcInstance activeChar, Enum<L2EffectType> effectType, int itemId)
	{
		L2Effect[] effects = activeChar.getAllEffects();

		if(effects == null)
		{
			return true;
		}

		for(L2Effect e : effects)
		{
			if(e.getEffectType() == effectType)
			{
				if(e.getSkill().isPotion())
				{
					if(e.getTaskTime() > e.getSkill().getBuffDuration() * 67 / 100000)
					{
						return true;
					}
					activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE).addItemName(itemId));

					return false;
				}
			}
		}
		return true;
	}

	public boolean usePotion(L2Playable activeChar, int magicId, int level)
	{
		
		L2Skill skill = SkillTable.getInstance().getInfo(magicId, level);
			
		if (skill != null)
		{
			// Return false if potion is in reuse
			// so is not destroyed from inventory
			if (activeChar.isSkillDisabled(skill.getId()))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE).addSkillName(skill.getId(),skill.getLevel()));
				
				return false;
			}
				
			activeChar.doSimultaneousCast(skill);
				
			if (activeChar instanceof L2PcInstance)
			{
				L2PcInstance player = (L2PcInstance)activeChar;
				//only for Heal potions
				if (magicId == 2031 || magicId == 2032 || magicId == 2037)
				{
					player.shortBuffStatusUpdate(magicId, level, 15);
				}
				// Summons should be affected by herbs too, self time effect is handled at L2Effect constructor 
				else if (((magicId > 2277 && magicId < 2286) || (magicId >= 2512 && magicId <= 2514))
					&& (player.getPet() != null && player.getPet() instanceof L2SummonInstance))
				{
					player.getPet().doSimultaneousCast(skill);
				}
				
				if (!(player.isSitting() && !skill.isPotion()))
					return true;
			}
			else if (activeChar instanceof L2PetInstance)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.PET_USES_S1);
				sm.addString(skill.getName());
				((L2PetInstance)(activeChar)).getOwner().sendPacket(sm);
			}
		}
		return false;
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}