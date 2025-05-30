package com.src.gameserver.model.actor.instance;

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.entity.event.BW;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.MyTargetSelected;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.ValidateLocation;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public class L2CustomBWBaseInstance extends L2Npc
{
	public int							_teamId;
	public BW							_event;

	public L2CustomBWBaseInstance(int objectId, L2NpcTemplate template)
    {
		super(objectId, template);
		setServerSideName(true);
	}

	public boolean canAttack(L2PcInstance player)
	{
		if (player._eventTeamId != 0 && player._eventTeamId != _teamId)
			return true;
		return false;
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player))
			return;

		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);

			StatusUpdate su = new StatusUpdate(getObjectId());
			su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
			su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
			player.sendPacket(su);
			
			// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			if (!isAlikeDead() && Math.abs(player.getZ() - getZ()) < 400 && canAttack(player))
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			else
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
		}
		player.sendPacket(new ActionFailed());
	}

	@Override
	public void onForcedAttack(L2PcInstance player)
	{
		onAction(player);
	}

	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake)
	{
		L2PcInstance player = null;
		if (attacker instanceof L2PcInstance)
			player = (L2PcInstance) attacker;
		else if (attacker instanceof L2Summon)
			player = ((L2Summon) attacker).getOwner();
		else
			return;

		if (!canAttack(player))
			return;

		player.updatePvPStatus();

		if (damage < getStatus().getCurrentHp())
			getStatus().setCurrentHp(getStatus().getCurrentHp() - damage);
		else
			doDie(attacker);
	}
	@Override
	public boolean isAttackable()
	{
		return true;
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return true;
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		L2PcInstance player = null;
		if (killer instanceof L2PcInstance)
			player = (L2PcInstance) killer;
		else if (killer instanceof L2Summon)
			player = ((L2Summon) killer).getOwner();
		else
			return false;

		if (!super.doDie(killer))
			return false;

		_event.onPlayerKillBase(player, _teamId);
		return true;
	}
}