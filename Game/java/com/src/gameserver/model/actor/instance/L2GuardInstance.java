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
package com.src.gameserver.model.actor.instance;

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.ai.L2AttackableAI;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.L2WorldRegion;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.knownlist.GuardKnownList;
import com.src.gameserver.model.actor.position.L2CharPosition;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.MyTargetSelected;
import com.src.gameserver.network.serverpackets.SocialAction;
import com.src.gameserver.network.serverpackets.ValidateLocation;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.random.Rnd;

public final class L2GuardInstance extends L2Attackable
{
	private int _homeX;
	private int _homeY;
	private int _homeZ;
	private static final int RETURN_INTERVAL = 60000;

	public class ReturnTask implements Runnable
	{
		@Override
		public void run()
		{
			if(getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
			{
				returnHome();
			}
		}
	}

	public L2GuardInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		getKnownList();

		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new ReturnTask(), RETURN_INTERVAL, RETURN_INTERVAL + Rnd.nextInt(60000));
	}

	@Override
	public final GuardKnownList getKnownList()
	{
		if(super.getKnownList() == null || !(super.getKnownList() instanceof GuardKnownList))
		{
			setKnownList(new GuardKnownList(this));
		}

		return (GuardKnownList) super.getKnownList();
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return attacker instanceof L2MonsterInstance;
	}

	public void getHomeLocation()
	{
		_homeX = getX();
		_homeY = getY();
		_homeZ = getZ();
	}

	public int getHomeX()
	{
		return _homeX;
	}

	public void returnHome()
	{
		if(!isInsideRadius(_homeX, _homeY, 150, false))
		{
			clearAggroList();

			getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(_homeX, _homeY, _homeZ, 0));
		}
	}

	@Override
	public void onSpawn()
	{
		setIsNoRndWalk(true);
		super.onSpawn();
		_homeX = getX();
		_homeY = getY();
		_homeZ = getZ();

		L2WorldRegion region = L2World.getInstance().getRegion(getX(), getY());
		if(region != null && !region.isActive())
		{
			((L2AttackableAI) getAI()).stopAITask();
		}

		region = null;
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		if(val == 0)
		{
			pom = "" + npcId;
		}
		else
		{
			pom = npcId + "-" + val;
		}

		return "data/html/guard/" + pom + ".htm";
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if(!canTarget(player))
		{
			return;
		}

		if(getObjectId() != player.getTargetId())
		{
			player.setTarget(this);

			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
			my = null;

			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			if(containsTarget(player))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			}
			else
			{
				if(!canInteract(player))
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				}
				else
				{
					SocialAction sa = new SocialAction(getObjectId(), Rnd.nextInt(8));
					broadcastPacket(sa);
					sa = null;

					showChatWindow(player, 0);
				}
			}
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}