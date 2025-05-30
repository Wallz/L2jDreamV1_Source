package com.src.gameserver.model.actor.instance;

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.entity.event.CTF;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.MyTargetSelected;
import com.src.gameserver.network.serverpackets.ValidateLocation;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public class L2CustomCTFFlagInstance extends L2Npc
{

	public String						_mode;
	public int							_teamId;
	public CTF							_event;

	public L2CustomCTFFlagInstance(int objectId, L2NpcTemplate template)
    {
		super(objectId, template);
		setServerSideName(true);
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if (!canTarget(player))
			return;

		if (_mode != null && _mode.equals("THRONE"))
		{
			player.sendPacket(new ActionFailed());
			return;
		}

		// Check if the L2PcInstance already target the L2NpcInstance
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);

			// Send a Server->Client packet ValidateLocation to correct the L2NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			if (!canInteract(player))
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this); // Notify the L2PcInstance AI with AI_INTENTION_INTERACT
			else
			{
				if (player._CTFHaveFlagOfTeam > 0 && player._eventTeamId == _teamId)
					_event.onPlayerBringFlag(player);
				else if (player._CTFHaveFlagOfTeam == 0 && player._eventTeamId != _teamId)
					_event.onPlayerTakeFlag(player, _teamId);
			}
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
		return;
	}
}