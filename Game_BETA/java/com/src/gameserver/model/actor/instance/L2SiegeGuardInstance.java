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
import com.src.gameserver.ai.L2CharacterAI;
import com.src.gameserver.ai.L2SiegeGuardAI;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.knownlist.SiegeGuardKnownList;
import com.src.gameserver.model.actor.position.L2CharPosition;
import com.src.gameserver.model.entity.clanhallsiege.DevastatedCastle;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.MyTargetSelected;
import com.src.gameserver.network.serverpackets.SocialAction;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.ValidateLocation;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.util.random.Rnd;

public class L2SiegeGuardInstance extends L2Attackable
{
	private int _homeX;
	private int _homeY;
	private int _homeZ;

	public L2SiegeGuardInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		getKnownList();
	}

	@Override
	public SiegeGuardKnownList getKnownList()
	{
		if(super.getKnownList() == null || !(super.getKnownList() instanceof SiegeGuardKnownList))
		{
			setKnownList(new SiegeGuardKnownList(this));
		}

		return (SiegeGuardKnownList) super.getKnownList();
	}

	@Override
	public L2CharacterAI getAI()
	{
		synchronized (this)
		{
			if(_ai == null)
			{
				_ai = new L2SiegeGuardAI(new AIAccessor());
			}
		}
		return _ai;
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return attacker != null && attacker instanceof L2PcInstance && (getCastle() != null && getCastle().getCastleId() > 0 && getCastle().getSiege().getIsInProgress() && !getCastle().getSiege().checkIsDefender(((L2PcInstance) attacker).getClan()) || DevastatedCastle.getInstance().getIsInProgress());
	}

	@Override
	public boolean hasRandomAnimation()
	{
		return false;
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

	public int getHomeY()
	{
		return _homeY;
	}

	public void returnHome()
	{
		if(!isInsideRadius(_homeX, _homeY, 40, false))
		{
			setisReturningToSpawnPoint(true);
			clearAggroList();

			if(hasAI())
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(_homeX, _homeY, _homeZ, 0));
			}
		}
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if(!canTarget(player))
		{
			return;
		}

		if(this != player.getTarget())
		{
			player.setTarget(this);

			MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			player.sendPacket(my);

			StatusUpdate su = new StatusUpdate(getObjectId());
			su.addAttribute(StatusUpdate.CUR_HP, (int) getStatus().getCurrentHp());
			su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
			player.sendPacket(su);

			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			if(isAutoAttackable(player) && !isAlikeDead())
			{
				if(Math.abs(player.getZ() - getZ()) < 600)
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
				}
				else
				{
					player.sendPacket(ActionFailed.STATIC_PACKET);
				}
			}

			if(!isAutoAttackable(player))
			{
				if(!canInteract(player))
				{
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				}
				else
				{
					SocialAction sa = new SocialAction(getObjectId(), Rnd.nextInt(8));
					broadcastPacket(sa);
					sendPacket(sa);
					showChatWindow(player, 0);
					sa = null;
				}
			}
		}
	}

	@Override
	public void addDamageHate(L2Character attacker, int damage, int aggro)
	{
		if(attacker == null)
		{
			return;
		}

		if(!(attacker instanceof L2SiegeGuardInstance))
		{
			super.addDamageHate(attacker, damage, aggro);
		}
	}

}