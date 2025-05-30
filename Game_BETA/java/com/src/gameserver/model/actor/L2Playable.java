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
package com.src.gameserver.model.actor;

import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.knownlist.PlayableKnownList;
import com.src.gameserver.model.actor.stat.PlayableStat;
import com.src.gameserver.model.actor.status.PlayableStatus;
import com.src.gameserver.templates.chars.L2CharTemplate;
import com.src.gameserver.templates.skills.L2EffectType;

public abstract class L2Playable extends L2Character
{
	private boolean _isNoblesseBlessed = false;
	private boolean _getCharmOfLuck = false;
	private boolean _isPhoenixBlessed = false;
	private boolean _ProtectionBlessing = false;

	public L2Playable(int objectId, L2CharTemplate template)
	{
		super(objectId, template);
		getKnownList();
		getStat();
		getStatus();
	}

	@Override
	public PlayableKnownList getKnownList()
	{
		if(super.getKnownList() == null || !(super.getKnownList() instanceof PlayableKnownList))
		{
			setKnownList(new PlayableKnownList(this));
		}

		return (PlayableKnownList) super.getKnownList();
	}

	@Override
	public PlayableStat getStat()
	{
		if(super.getStat() == null || !(super.getStat() instanceof PlayableStat))
		{
			setStat(new PlayableStat(this));
		}

		return (PlayableStat) super.getStat();
	}

	@Override
	public PlayableStatus getStatus()
	{
		if(super.getStatus() == null || !(super.getStatus() instanceof PlayableStatus))
		{
			setStatus(new PlayableStatus(this));
		}

		return (PlayableStatus) super.getStatus();
	}

	@Override
	public boolean doDie(L2Character killer)
	{	
		// Set target to null and cancel Attack or Cast
		setTarget(null);
		
		// Stop movement
		stopMove(null);
		
		// Stop HP/MP/CP Regeneration task
		getStatus().stopHpMpRegeneration();
		
		if(!super.doDie(killer))
		{
			return false;
		}

		if(killer != null)
		{
			L2PcInstance player = null;
			if(killer instanceof L2PcInstance)
			{
				player = (L2PcInstance) killer;
			}

			if(player != null)
			{
				player.onKillUpdatePvPKarma(this);
				player = null;
			}
		}

		return true;
	}

	public boolean checkIfPvP(L2Character target)
	{
		if(target == null)
		{
			return false;
		}

		if(target == this)
		{
			return false;
		}

		if(!(target instanceof L2Playable))
		{
			return false;
		}

		L2PcInstance player = null;
		if(this instanceof L2PcInstance)
		{
			player = (L2PcInstance) this;
		}
		else if(this instanceof L2Summon)
		{
			player = ((L2Summon) this).getOwner();
		}

		if(player == null)
		{
			return false;
		}

		if(player.getKarma() != 0)
		{
			return false;
		}

		L2PcInstance targetPlayer = null;
		if(target instanceof L2PcInstance)
		{
			targetPlayer = (L2PcInstance) target;
		}
		else if(target instanceof L2Summon)
		{
			targetPlayer = ((L2Summon) target).getOwner();
		}

		if(targetPlayer == null)
		{
			return false;
		}

		if(targetPlayer == this)
		{
			return false;
		}

		if(targetPlayer.getKarma() != 0)
		{
			return false;
		}

		if(targetPlayer.getPvpFlag() == 0)
		{
			return false;
		}

		player = null;
		targetPlayer = null;

		return true;
	}

	@Override
	public boolean isAttackable()
	{
		return true;
	}

	public final boolean isNoblesseBlessed()
	{
		return _isNoblesseBlessed;
	}

	public final void setIsNoblesseBlessed(boolean value)
	{
		_isNoblesseBlessed = value;
	}

	public final void startNoblesseBlessing()
	{
		setIsNoblesseBlessed(true);
		updateAbnormalEffect();
	}

	public final boolean getProtectionBlessing()
	{
		return _ProtectionBlessing;
	}

	public final void setProtectionBlessing(boolean value)
	{
		_ProtectionBlessing = value;
	}

	public void startProtectionBlessing()
	{
		setProtectionBlessing(true);
		updateAbnormalEffect();
	}

	public void stopProtectionBlessing(L2Effect effect)
	{
		if(effect == null)
		{
			stopEffects(L2EffectType.PROTECTION_BLESSING);
		}
		else
		{
			removeEffect(effect);
		}

		setProtectionBlessing(false);
		updateAbnormalEffect();
	}

	public final boolean isPhoenixBlessed()
	{
		return _isPhoenixBlessed;
	}

	public final void setIsPhoenixBlessed(boolean value)
	{
		_isPhoenixBlessed = value;
	}

	public final void startPhoenixBlessing()
	{
		setIsPhoenixBlessed(true);
		updateAbnormalEffect();
	}

	public final void stopPhoenixBlessing(L2Effect effect)
	{
		if(effect == null)
		{
			stopEffects(L2EffectType.PHOENIX_BLESSING);
		}
		else
		{
			removeEffect(effect);
		}

		setIsPhoenixBlessed(false);
		updateAbnormalEffect();
	}

	public final void stopNoblesseBlessing(L2Effect effect)
	{
		if(effect == null)
		{
			stopEffects(L2EffectType.NOBLESSE_BLESSING);
		}
		else
		{
			removeEffect(effect);
		}

		setIsNoblesseBlessed(false);
		updateAbnormalEffect();
	}

	@Override
	public abstract boolean destroyItemByItemId(String process, int itemId, int count, L2Object reference, boolean sendMessage);

	public abstract boolean destroyItem(String process, int objectId, int count, L2Object reference, boolean sendMessage);

	public final boolean getCharmOfLuck()
	{
		return _getCharmOfLuck;
	}

	public final void setCharmOfLuck(boolean value)
	{
		_getCharmOfLuck = value;
	}

	public final void startCharmOfLuck()
	{
		setCharmOfLuck(true);
		updateAbnormalEffect();
	}

	public final void stopCharmOfLuck(L2Effect effect)
	{
		if(effect == null)
		{
			stopEffects(L2EffectType.CHARM_OF_LUCK);
		}
		else
		{
			removeEffect(effect);
		}

		setCharmOfLuck(false);
		updateAbnormalEffect();
	}
}