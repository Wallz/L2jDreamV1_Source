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
package com.src.gameserver.util;

import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.CharInfo;
import com.src.gameserver.network.serverpackets.L2GameServerPacket;
import com.src.gameserver.network.serverpackets.RelationChanged;

public final class Broadcast
{
	public static void toPlayersTargettingMyself(L2Character character, L2GameServerPacket mov)
	{
		for(L2PcInstance player : character.getKnownList().getKnownPlayers().values())
		{
			if(player == null || player.getTarget() != character)
			{
				continue;
			}

			player.sendPacket(mov);
		}
	}

	public static void toKnownPlayers(L2Character character, L2GameServerPacket mov) 
	{ 
		
		
		for (L2PcInstance player : character.getKnownList().getKnownPlayers().values()) 
		{ 
			try 
			{ 
				player.sendPacket(mov); 
				if (mov instanceof CharInfo && character instanceof L2PcInstance) 
				{ 
					int relation = ((L2PcInstance) character).getRelation(player); 
					if (character.getKnownList().getKnownRelations().get(player.getObjectId()) != null && character.getKnownList().getKnownRelations().get(player.getObjectId()) != relation) 
					{ 
						player.sendPacket(new RelationChanged((L2PcInstance) character, relation, player.isAutoAttackable(character))); 
						if (((L2PcInstance) character).getPet() != null) 
							player.sendPacket(new RelationChanged(((L2PcInstance) character).getPet(), relation, player.isAutoAttackable(character))); 
					} 
				} 
			} 
			catch (NullPointerException e) 
			{ 
			} 
		} 
	}

	public static void toKnownPlayersInRadius(L2Character character, L2GameServerPacket mov, int radius)
	{
		if(radius < 0)
		{
			radius = 1500;
		}
		for(L2PcInstance player : character.getKnownList().getKnownPlayers().values())
		{
			if(player == null)
			{
				continue;
			}

			if(character.isInsideRadius(player, radius, false, false))
			{
				player.sendPacket(mov);
			}
		}
	}

	public static void toSelfAndKnownPlayers(L2Character character, L2GameServerPacket mov)
	{
		if(character instanceof L2PcInstance)
		{
			character.sendPacket(mov);
		}

		toKnownPlayers(character, mov);
	}

	public static void toSelfAndKnownPlayersInRadius(L2Character character, L2GameServerPacket mov, long radiusSq)
	{
		if(radiusSq < 0)
		{
			radiusSq = 360000;
		}

		if(character instanceof L2PcInstance)
		{
			character.sendPacket(mov);
		}
		for(L2PcInstance player : character.getKnownList().getKnownPlayers().values())
		{
			if(player != null && character.getDistanceSq(player) <= radiusSq)
			{
				player.sendPacket(mov);
			}
		}
	}

	public static void toAllOnlinePlayers(L2GameServerPacket mov)
	{
		for(L2PcInstance onlinePlayer : L2World.getInstance().getAllPlayers())
		{
			if(onlinePlayer == null)
			{
				continue;
			}

			onlinePlayer.sendPacket(mov);
		}
	}

}