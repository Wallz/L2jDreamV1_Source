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
package com.src.gameserver.model.actor.knownlist;

import com.src.Config;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.instance.L2BoatInstance;
import com.src.gameserver.model.actor.instance.L2DoorInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2PetInstance;
import com.src.gameserver.model.actor.instance.L2StaticObjectInstance;
import com.src.gameserver.network.serverpackets.CharInfo;
import com.src.gameserver.network.serverpackets.DeleteObject;
import com.src.gameserver.network.serverpackets.DoorInfo;
import com.src.gameserver.network.serverpackets.DropItem;
import com.src.gameserver.network.serverpackets.GetOnVehicle;
import com.src.gameserver.network.serverpackets.NpcInfo;
import com.src.gameserver.network.serverpackets.PetInfo;
import com.src.gameserver.network.serverpackets.PetItemList;
import com.src.gameserver.network.serverpackets.PrivateStoreMsgBuy;
import com.src.gameserver.network.serverpackets.PrivateStoreMsgSell;
import com.src.gameserver.network.serverpackets.RecipeShopMsg;
import com.src.gameserver.network.serverpackets.RelationChanged;
import com.src.gameserver.network.serverpackets.SpawnItem;
import com.src.gameserver.network.serverpackets.SpawnItemPoly;
import com.src.gameserver.network.serverpackets.StaticObject;
import com.src.gameserver.network.serverpackets.VehicleInfo;

public class PcKnownList extends PlayableKnownList
{
	public PcKnownList(L2PcInstance activeChar)
	{
		super(activeChar);
	}

	@Override
	public boolean addKnownObject(L2Object object)
	{
		return addKnownObject(object, null);
	}

	@Override
	public boolean addKnownObject(L2Object object, L2Character dropper)
	{
		if(!super.addKnownObject(object, dropper))
		{
			return false;
		}

		if(object.getPoly().isMorphed() && object.getPoly().getPolyType().equals("item"))
		{
			getActiveChar().sendPacket(new SpawnItemPoly(object));
		}
		else
		{
			if(object instanceof L2ItemInstance)
			{
				if(dropper != null)
				{
					getActiveChar().sendPacket(new DropItem((L2ItemInstance) object, dropper.getObjectId()));
				}
				else
				{
					getActiveChar().sendPacket(new SpawnItem((L2ItemInstance) object));
				}
			}
			else if(object instanceof L2DoorInstance)
			{
				getActiveChar().sendPacket(new DoorInfo((L2DoorInstance) object, false));
			}
			else if(object instanceof L2BoatInstance)
			{
				if(!getActiveChar().isInBoat())
				{
					if(object != getActiveChar().getBoat())
					{
						getActiveChar().sendPacket(new VehicleInfo((L2BoatInstance) object));
						((L2BoatInstance) object).sendVehicleDeparture(getActiveChar());
					}
				}
			}
			else if(object instanceof L2StaticObjectInstance)
			{
				getActiveChar().sendPacket(new StaticObject((L2StaticObjectInstance) object));
			}
			else if(object instanceof L2Npc)
			{
				if(Config.CHECK_KNOWN)
				{
					getActiveChar().sendMessage("Added NPC: " + ((L2Npc) object).getName());
				}

				getActiveChar().sendPacket(new NpcInfo((L2Npc) object, getActiveChar()));
			}
			else if(object instanceof L2Summon)
			{
				L2Summon summon = (L2Summon) object;

				if(getActiveChar().equals(summon.getOwner()))
				{
					getActiveChar().sendPacket(new PetInfo(summon));
					summon.updateEffectIcons(true);

					if(summon instanceof L2PetInstance)
					{
						getActiveChar().sendPacket(new PetItemList((L2PetInstance) summon));
					}
				}
				else
				{
					getActiveChar().sendPacket(new NpcInfo(summon, getActiveChar()));
				}

				summon = null;
			}
			else if(object instanceof L2PcInstance)
			{
				L2PcInstance otherPlayer = (L2PcInstance) object;
				if(otherPlayer.isInBoat())
				{
					otherPlayer.getPosition().setWorldPosition(otherPlayer.getBoat().getPosition().getWorldPosition());
					getActiveChar().sendPacket(new CharInfo(otherPlayer));

					int relation = otherPlayer.getRelation(getActiveChar());

					if(otherPlayer.getKnownList().getKnownRelations().get(getActiveChar().getObjectId()) != null && otherPlayer.getKnownList().getKnownRelations().get(getActiveChar().getObjectId()) != relation)
					{
						getActiveChar().sendPacket(new RelationChanged(otherPlayer, relation, getActiveChar().isAutoAttackable(otherPlayer)));
					}

					getActiveChar().sendPacket(new GetOnVehicle(otherPlayer, otherPlayer.getBoat(), otherPlayer.getInBoatPosition().getX(), otherPlayer.getInBoatPosition().getY(), otherPlayer.getInBoatPosition().getZ()));

				}
				else
				{
					getActiveChar().sendPacket(new CharInfo(otherPlayer));

					int relation = otherPlayer.getRelation(getActiveChar());

					if(otherPlayer.getKnownList().getKnownRelations().get(getActiveChar().getObjectId()) != null && otherPlayer.getKnownList().getKnownRelations().get(getActiveChar().getObjectId()) != relation)
					{
						getActiveChar().sendPacket(new RelationChanged(otherPlayer, relation, getActiveChar().isAutoAttackable(otherPlayer)));
					}
				}

				if(otherPlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL)
				{
					getActiveChar().sendPacket(new PrivateStoreMsgSell(otherPlayer));
				}
				else if(otherPlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_BUY)
				{
					getActiveChar().sendPacket(new PrivateStoreMsgBuy(otherPlayer));
				}
				else if(otherPlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_MANUFACTURE)
				{
					getActiveChar().sendPacket(new RecipeShopMsg(otherPlayer));
				}

				otherPlayer = null;
			}

			if(object instanceof L2Character)
			{
				L2Character obj = (L2Character) object;

				if(obj.getAI() != null)
				{
					obj.getAI().describeStateToPlayer(getActiveChar());
				}

				obj = null;
			}
		}

		return true;
	}

	@Override
	public boolean removeKnownObject(L2Object object)
	{
		if(!super.removeKnownObject(object))
		{
			return false;
		}

		getActiveChar().sendPacket(new DeleteObject(object));

		if(Config.CHECK_KNOWN && object instanceof L2Npc)
		{
			getActiveChar().sendMessage("Removed NPC: " + ((L2Npc) object).getName());
		}

		return true;
	}

	@Override
	public final L2PcInstance getActiveChar()
	{
		return (L2PcInstance) super.getActiveChar();
	}

	@Override
	public int getDistanceToForgetObject(L2Object object)
	{
		int knownlistSize = getKnownObjects().size();

		if(knownlistSize <= 25)
		{
			return 4200;
		}

		if(knownlistSize <= 35)
		{
			return 3600;
		}

		if(knownlistSize <= 70)
		{
			return 2910;
		}

		else
		{
			return 2310;
		}
	}

	@Override
	public int getDistanceToWatchObject(L2Object object)
	{
		int knownlistSize = getKnownObjects().size();

		if(knownlistSize <= 25)
		{
			return 3500;
		}

		if(knownlistSize <= 35)
		{
			return 2900;
		}

		if(knownlistSize <= 70)
		{
			return 2300;
		}

		else
		{
			return 1700;
		}
	}

}