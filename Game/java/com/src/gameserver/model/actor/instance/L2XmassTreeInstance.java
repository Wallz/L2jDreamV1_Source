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

import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.network.serverpackets.MagicSkillUser;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.random.Rnd;

public class L2XmassTreeInstance extends L2Npc
{
	private ScheduledFuture<?> _aiTask;

	class XmassAI implements Runnable
	{
		private L2XmassTreeInstance _caster;

		protected XmassAI(L2XmassTreeInstance caster)
		{
			_caster = caster;
		}

		@Override
		public void run()
		{
			for(L2PcInstance player : getKnownList().getKnownPlayers().values())
			{
				int i = Rnd.nextInt(3);
				handleCast(player, (4262 + i));
			}
		}

		private boolean handleCast(L2PcInstance player, int skillId)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(skillId, 1);

			if(player.getFirstEffect(skill) == null)
			{
				setTarget(player);
				doCast(skill);

				MagicSkillUser msu = new MagicSkillUser(_caster, player, skill.getId(), 1, skill.getHitTime(), 0);
				broadcastPacket(msu);
				skill = null;
				return true;
			}
			skill = null;
			return false;
		}
	}

	public L2XmassTreeInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		_aiTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new XmassAI(this), 3000, 3000);
	}

	@Override
	public void deleteMe()
	{
		if(_aiTask != null)
		{
			_aiTask.cancel(true);
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