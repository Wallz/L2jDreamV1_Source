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
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ExAutoSoulShot;
import com.src.gameserver.network.serverpackets.MagicSkillUser;
import com.src.gameserver.network.serverpackets.PetInfo;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.util.Broadcast;

public class BeastSoulShot implements IItemHandler
{
    // All the item IDs that this handler knows.
    private static final int[] ITEM_IDS = { 6645 };
    
    public void useItem(L2Playable playable, L2ItemInstance item)
    {
    	if (playable == null) return;
    	
        L2PcInstance activeOwner = null;
        if (playable instanceof L2Summon)
        {
            activeOwner = ((L2Summon)playable).getOwner();
            activeOwner.sendPacket(new SystemMessage(SystemMessageId.PET_CANNOT_USE_ITEM));
            return;
        } else if (playable instanceof L2PcInstance)
        {
        	activeOwner = (L2PcInstance)playable;
        }
        
        if (activeOwner == null)
        	return;
        L2Summon activePet = activeOwner.getPet();
        
        if (activePet == null)
        {
            activeOwner.sendPacket(new SystemMessage(SystemMessageId.PETS_ARE_NOT_AVAILABLE_AT_THIS_TIME));
            return;
        }
        
        if (activePet.isDead())
        {
            activeOwner.sendPacket(new SystemMessage(SystemMessageId.SOULSHOTS_AND_SPIRITSHOTS_ARE_NOT_AVAILABLE_FOR_A_DEAD_PET));
            return;
        }
        
        int itemId = 6645;
        int shotConsumption = 1;
        
        if (!(item.getCount() > shotConsumption))
		{
			// Not enough Soulshots to use.
			if (!activeOwner.disableAutoShot(itemId))
				activeOwner.sendPacket(SystemMessageId.NOT_ENOUGH_SOULSHOTS_FOR_PET);
			return;
		}
		
		L2ItemInstance weaponInst = null;
		
		if (activePet instanceof L2PetInstance)
			weaponInst = ((L2PetInstance) activePet).getActiveWeaponInstance();
		
		if (weaponInst == null)
		{
			if (activePet.getChargedSoulShot() != L2ItemInstance.CHARGED_NONE)
				return;
			
			activePet.setChargedSoulShot(L2ItemInstance.CHARGED_SOULSHOT);
		}
		else
		{
			// SoulShots are already active.
			if (weaponInst.getChargedSoulshot() != L2ItemInstance.CHARGED_NONE)
				return;
			
			weaponInst.setChargedSoulshot(L2ItemInstance.CHARGED_SOULSHOT);
		}
        
        // If the player doesn't have enough beast soulshot remaining, remove any auto soulshot task.
        if (!activeOwner.destroyItemWithoutTrace("Consume", item.getObjectId(), shotConsumption, null, false))
        {
            if (activeOwner.getAutoSoulShot().containsKey(itemId))
            {
                activeOwner.removeAutoSoulShot(itemId);
                activeOwner.sendPacket(new ExAutoSoulShot(itemId, 0));
                
                SystemMessage sm = new SystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED); 
                sm.addString(item.getItem().getName());
                activeOwner.sendPacket(sm);
                return;
            }

            activeOwner.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_SOULSHOTS));
            return;     
        }

        // Pet uses the power of spirit.
        activePet.increaseUsedSoulShots(1);
        activeOwner.sendPacket(new PetInfo(activePet));
        activeOwner.sendPacket(new SystemMessage(SystemMessageId.PET_USE_THE_POWER_OF_SPIRIT));
        
        Broadcast.toSelfAndKnownPlayersInRadius(activeOwner, new MagicSkillUser(activePet, activePet, 2033, 1, 0, 0), 360000/*600*/);
    }
    
    public int[] getItemIds()
    {
        return ITEM_IDS;
    }
}