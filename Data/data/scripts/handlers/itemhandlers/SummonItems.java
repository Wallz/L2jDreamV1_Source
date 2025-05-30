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
import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.datatables.xml.SummonItemsData;
import com.src.gameserver.handler.IItemHandler;
import com.src.gameserver.idfactory.IdFactory;
import com.src.gameserver.model.L2SummonItem;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.MagicSkillLaunched;
import com.src.gameserver.network.serverpackets.MagicSkillUser;
import com.src.gameserver.network.serverpackets.PetInfo;
import com.src.gameserver.network.serverpackets.Ride;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.gameserver.util.Broadcast;
import com.src.util.random.Rnd;

public class SummonItems implements IItemHandler
{
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

		if(activeChar.isParalyzed())
		{
			activeChar.sendMessage("You can not use this while You are paralyzed.");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		if (activeChar.isFightingInEvent())
		{
			if ((activeChar.getEventName().equals("TVT") && !Config.TVT_ALLOW_SUMMON)
					|| (activeChar.getEventName().equals("CTF") && !Config.CTF_ALLOW_SUMMON)
					|| (activeChar.getEventName().equals("BW") && !Config.BW_ALLOW_SUMMON)
					|| (activeChar.getEventName().equals("DM") && !Config.DM_ALLOW_SUMMON))
			 	{
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			 		return;
			 	}
		}

		if(activeChar.inObserverMode())
		{
			return;
		}

		if(activeChar.isInOlympiadMode())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
			return;
		}

		L2SummonItem sitem = SummonItemsData.getInstance().getSummonItem(item.getItemId());

		if((activeChar.getPet() != null || activeChar.isMounted()) && sitem.isPetSummon())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_ALREADY_HAVE_A_PET));
			return;
		}

		if(activeChar.isAttackingNow())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_SUMMON_IN_COMBAT));
			return;
		}

		if(activeChar.isCursedWeaponEquiped() && sitem.isPetSummon())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE));
			return;
		}

		int npcID = sitem.getNpcId();

		if(npcID == 0)
		{
			return;
		}

		L2NpcTemplate npcTemplate = NpcTable.getInstance().getTemplate(npcID);

		if(npcTemplate == null)
		{
			return;
		}

		switch(sitem.getType())
		{
			case 0:
				try
				{
					L2Spawn spawn = new L2Spawn(npcTemplate);
					spawn.setId(IdFactory.getInstance().getNextId());
					spawn.setLocx(activeChar.getX());
					spawn.setLocy(activeChar.getY());
					spawn.setLocz(activeChar.getZ());
					L2World.storeObject(spawn.spawnOne());
					activeChar.destroyItem("Summon", item.getObjectId(), 1, null, false);
					activeChar.sendMessage("Created " + npcTemplate.name + " at x: " + spawn.getLocx() + " y: " + spawn.getLocy() + " z: " + spawn.getLocz());
					spawn = null;
				}
				catch(Exception e)
				{
					activeChar.sendMessage("Target is not ingame.");
				}

				break;
			case 1:
				L2PetInstance petSummon = L2PetInstance.spawnPet(npcTemplate, activeChar, item);

				if(petSummon == null)
				{
					break;
				}

				petSummon.setTitle(activeChar.getName());

				if(!petSummon.isRespawned())
				{
					petSummon.setCurrentHp(petSummon.getMaxHp());
					petSummon.setCurrentMp(petSummon.getMaxMp());
					petSummon.getStat().setExp(petSummon.getExpForThisLevel());
					petSummon.setCurrentFed(petSummon.getMaxFed());
				}

				petSummon.setRunning();

				if(!petSummon.isRespawned())
				{
					petSummon.store();
				}

				activeChar.setPet(petSummon);

				Broadcast.toSelfAndKnownPlayers(activeChar, new MagicSkillUser(activeChar, 2046, 1, 5000, 0));
				activeChar.sendPacket(new SystemMessage(SystemMessageId.SUMMON_A_PET));
				L2World.storeObject(petSummon);
				petSummon.spawnMe(activeChar.getX() + Rnd.get(40)-20, activeChar.getY() + Rnd.get(40)-20, activeChar.getZ());
				activeChar.sendPacket(new PetInfo(petSummon));
				petSummon.startFeed(false);
				item.setEnchantLevel(petSummon.getLevel());

				ThreadPoolManager.getInstance().scheduleGeneral(new PetSummonFinalizer(activeChar, petSummon), 5000);

				if(petSummon.getCurrentFed() <= 0)
				{
					ThreadPoolManager.getInstance().scheduleGeneral(new PetSummonFeedWait(activeChar, petSummon), 60000);
				}
				else
				{
					petSummon.startFeed(false);
				}

				petSummon = null;

				break;
			case 2:
				if(!activeChar.disarmWeapons())
				{
					return;
				}

				Ride mount = new Ride(activeChar.getObjectId(), Ride.ACTION_MOUNT, sitem.getNpcId());
				activeChar.sendPacket(mount);
				activeChar.broadcastPacket(mount);
				activeChar.setMountType(mount.getMountType());
				activeChar.setMountObjectID(item.getObjectId());
		}

		activeChar = null;
		sitem = null;
		npcTemplate = null;
	}

	static class PetSummonFeedWait implements Runnable
	{
		private L2PcInstance _activeChar;
		private L2PetInstance _petSummon;

		PetSummonFeedWait(L2PcInstance activeChar, L2PetInstance petSummon)
		{
			_activeChar = activeChar;
			_petSummon = petSummon;
		}

		public void run()
		{
			try
			{
				if(_petSummon.getCurrentFed() <= 0)
				{
					_petSummon.unSummon(_activeChar);
				}
				else
				{
					_petSummon.startFeed(false);
				}
			}
			catch(Throwable e)
			{
			}
		}
	}

	static class PetSummonFinalizer implements Runnable
	{
		private L2PcInstance _activeChar;
		private L2PetInstance _petSummon;

		PetSummonFinalizer(L2PcInstance activeChar, L2PetInstance petSummon)
		{
			_activeChar = activeChar;
			_petSummon = petSummon;
		}

		public void run()
		{
			try
			{
				_activeChar.sendPacket(new MagicSkillLaunched(_activeChar, 2046, 1));
				_petSummon.setFollowStatus(true);
				_petSummon.setShowSummonAnimation(false);
			}
			catch(Throwable e)
			{
			}
		}
	}

	public int[] getItemIds()
	{
		return SummonItemsData.getInstance().itemIDs();
	}

}