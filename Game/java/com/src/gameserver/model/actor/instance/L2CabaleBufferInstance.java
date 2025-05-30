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

import java.util.concurrent.ScheduledFuture;

import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.entity.sevensigns.SevenSigns;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.MagicSkillUser;
import com.src.gameserver.network.serverpackets.MyTargetSelected;
import com.src.gameserver.network.serverpackets.SocialAction;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.network.serverpackets.ValidateLocation;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.random.Rnd;

public class L2CabaleBufferInstance extends L2Npc
{
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

			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
			my = null;

			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			if(!canInteract(player))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				SocialAction sa = new SocialAction(getObjectId(), Rnd.get(8));
				broadcastPacket(sa);
				sa = null;
			}
		}

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private ScheduledFuture<?> _aiTask;

	private class CabalaAI implements Runnable
	{
		private L2CabaleBufferInstance _caster;

		protected CabalaAI(L2CabaleBufferInstance caster)
		{
			_caster = caster;
		}

		@Override
		public void run()
		{
			boolean isBuffAWinner = false;
			boolean isBuffALoser = false;

			final int winningCabal = SevenSigns.getInstance().getCabalHighestScore();
			int losingCabal = SevenSigns.CABAL_NULL;

			if(winningCabal == SevenSigns.CABAL_DAWN)
			{
				losingCabal = SevenSigns.CABAL_DUSK;
			}
			else if(winningCabal == SevenSigns.CABAL_DUSK)
			{
				losingCabal = SevenSigns.CABAL_DAWN;
			}

			for(L2PcInstance player : getKnownList().getKnownPlayers().values())
			{
				if (player == null || player.isInvul())
					continue;
				
				final int playerCabal = SevenSigns.getInstance().getPlayerCabal(player);

				if(playerCabal == winningCabal && playerCabal != SevenSigns.CABAL_NULL && _caster.getNpcId() == SevenSigns.ORATOR_NPC_ID)
				{
					if(!player.isMageClass())
					{
						if(handleCast(player, 4364))
						{
							isBuffAWinner = true;
							continue;
						}
					}
					else
					{
						if(handleCast(player, 4365))
						{
							isBuffAWinner = true;
							continue;
						}
					}
				}
				else if(playerCabal == losingCabal && playerCabal != SevenSigns.CABAL_NULL && _caster.getNpcId() == SevenSigns.PREACHER_NPC_ID)
				{
					if(!player.isMageClass())
					{
						if(handleCast(player, 4361))
						{
							isBuffALoser = true;
							continue;
						}
					}
					else
					{
						if(handleCast(player, 4362))
						{
							isBuffALoser = true;
							continue;
						}
					}
				}

				if(isBuffAWinner && isBuffALoser)
				{
					break;
				}
			}
		}

		private boolean handleCast(L2PcInstance player, int skillId)
		{
			int skillLevel = player.getLevel() > 40 ? 1 : 2;

			if(player.isDead() || !player.isVisible() || !isInsideRadius(player, getDistanceToWatchObject(player), false, false))
			{
				return false;
			}

			L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
			if(player.getFirstEffect(skill) == null)
			{
				skill.getEffects(_caster, player);
				broadcastPacket(new MagicSkillUser(_caster, player, skill.getId(), skillLevel, skill.getHitTime(), 0));
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(skillId));
				skill = null;
				return true;
			}
			skill = null;

			return false;
		}
	}

	public L2CabaleBufferInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);

		if(_aiTask != null)
		{
			_aiTask.cancel(true);
		}

		_aiTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new CabalaAI(this), 3000, 3000);
	}

	@Override
	public void deleteMe()
	{
		if(_aiTask != null)
		{
			_aiTask.cancel(true);
			_aiTask = null;
		}

		super.deleteMe();
	}

	@Override
	public int getDistanceToWatchObject(L2Object object)
	{
		return 900;
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}

}