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

package handlers.usercommandhandlers;

import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.geo.GeoData;
import com.src.gameserver.handler.IUserCommandHandler;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.itemcontainer.Inventory;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.Ride;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.util.Broadcast;

public class Mount implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		61
	};

	public synchronized boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		if(id != COMMAND_IDS[0])
		{
			return false;
		}

		L2Summon pet = activeChar.getPet();

		if(pet != null && pet.isMountable() && !activeChar.isMounted())
		{
			if(activeChar.isDead())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_DEAD));
				return false;
			}
			else if(pet.isDead())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.DEAD_STRIDER_CANT_BE_RIDDEN));
				return false;
			}
			else if(pet.isInCombat() || activeChar.getPvpFlag() != 0)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.STRIDER_IN_BATLLE_CANT_BE_RIDDEN));
				return false;
			}
			else if(activeChar.isInCombat())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE));
				return false;
			}
			else if(!activeChar.isInsideRadius(pet, 60, true, false))
			{
				activeChar.sendMessage("Too far away from strider to mount.");
				return false;
			}
			else if(!GeoData.getInstance().canSeeTarget(activeChar, pet))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
				return false;
			}
			else if(activeChar.isSitting() || activeChar.isMoving())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.STRIDER_CAN_BE_RIDDEN_ONLY_WHILE_STANDING));
				return false;
			}
			else if(!pet.isDead() && !activeChar.isMounted())
			{
				if(!activeChar.disarmWeapons())
				{
					return false;
				}

				Ride mount = new Ride(activeChar.getObjectId(), Ride.ACTION_MOUNT, pet.getTemplate().npcId);
				Broadcast.toSelfAndKnownPlayersInRadius(activeChar, mount, 810000);
				activeChar.setMountType(mount.getMountType());
				activeChar.setMountObjectID(pet.getControlItemId());
				pet.unSummon(activeChar);

				if(activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND) != null || activeChar.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND) != null)
				{
					if(activeChar.setMountType(0))
					{
						if(activeChar.isFlying())
						{
							activeChar.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
						}

						Ride dismount = new Ride(activeChar.getObjectId(), Ride.ACTION_DISMOUNT, 0);
						Broadcast.toSelfAndKnownPlayers(activeChar, dismount);
						activeChar.setMountObjectID(0);
					}
				}
			}
		}
		else if(activeChar.isRentedPet())
		{
			activeChar.stopRentPet();
		}
		else if(activeChar.isMounted())
		{
			if(activeChar.setMountType(0))
			{
				if(activeChar.isFlying())
				{
					activeChar.removeSkill(SkillTable.getInstance().getInfo(4289, 1));
				}

				Ride dismount = new Ride(activeChar.getObjectId(), Ride.ACTION_DISMOUNT, 0);
				Broadcast.toSelfAndKnownPlayers(activeChar, dismount);
				activeChar.setMountObjectID(0);
				dismount = null;
			}
		}
		return true;
	}

	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}