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

import java.util.List;

import javolution.util.FastList;

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.geo.GeoData;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.MyTargetSelected;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.ValidateLocation;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public class L2ControlTowerInstance extends L2Npc
{

	private List<L2Spawn> _guards;

	public L2ControlTowerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public boolean isAttackable()
	{
		return getCastle() != null && getCastle().getCastleId() > 0 && getCastle().getSiege().getIsInProgress();
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return attacker != null && attacker instanceof L2PcInstance && getCastle() != null && getCastle().getCastleId() > 0 && getCastle().getSiege().getIsInProgress() && getCastle().getSiege().checkIsAttacker(((L2PcInstance) attacker).getClan());
	}

	@Override
	public void onForcedAttack(L2PcInstance player)
	{
		onAction(player);
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
			if(isAutoAttackable(player) && Math.abs(player.getZ() - getZ()) < 100 && GeoData.getInstance().canSeeTarget(player, this))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
		}
	}

	public void onDeath()
	{
		if(getCastle().getSiege().getIsInProgress())
		{
			getCastle().getSiege().killedCT(this);

			if(_guards != null && !_guards.isEmpty())
			{
				for(L2Spawn spawn: _guards)
				{
					if(spawn == null)
					{
						continue;
					}
					try
					{
						spawn.stopRespawn();
					}
					catch(Exception e)
					{
					}
				}
				_guards.clear();
				_guards = null;
			}
		}
	}

	public void registerGuard(L2Spawn guard)
	{
		getGuards().add(guard);
	}

	public final List<L2Spawn> getGuards()
	{
		if(_guards == null)
		{
			synchronized(this)
			{
				if(_guards == null)
				{
					_guards = new FastList<L2Spawn>();
				}
			}
		}

		return _guards;
	}
}