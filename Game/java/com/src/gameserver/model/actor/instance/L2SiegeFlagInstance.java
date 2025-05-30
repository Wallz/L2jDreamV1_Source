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
import com.src.gameserver.managers.SiegeManager;
import com.src.gameserver.model.L2SiegeClan;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.entity.siege.Siege;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.MyTargetSelected;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.ValidateLocation;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public class L2SiegeFlagInstance extends L2Npc
{
	private L2PcInstance _player;
	private Siege _siege;

	public L2SiegeFlagInstance(L2PcInstance player, int objectId, L2NpcTemplate template)
	{
		super(objectId, template);

		_player = player;
		_siege = SiegeManager.getInstance().getSiege(_player.getX(), _player.getY(), _player.getZ());
		if(_player.getClan() == null || _siege == null)
		{
			deleteMe();
		}
		else
		{
			L2SiegeClan sc = _siege.getAttackerClan(_player.getClan());
			if(sc == null)
			{
				deleteMe();
			}
			else
			{
				sc.addFlag(this);
			}
			sc = null;
		}
	}

	@Override
	public boolean isAttackable()
	{
		return getCastle() != null && getCastle().getCastleId() > 0 && getCastle().getSiege().getIsInProgress();
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return attacker != null && attacker instanceof L2PcInstance && getCastle() != null && getCastle().getCastleId() > 0 && getCastle().getSiege().getIsInProgress();
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if(!super.doDie(killer))
		{
			return false;
		}

		L2SiegeClan sc = _siege.getAttackerClan(_player.getClan());
		if(sc != null)
		{
			sc.removeFlag(this);
		}
		sc = null;

		return true;
	}

	@Override
	public void onForcedAttack(L2PcInstance player)
	{
		onAction(player);
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if(player == null || !canTarget(player))
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
			if(isAutoAttackable(player) && Math.abs(player.getZ() - getZ()) < 100)
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			}
			else
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
		}
	}

}