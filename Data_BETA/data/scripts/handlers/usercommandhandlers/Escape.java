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
package handlers.usercommandhandlers;

import com.src.Config;
import com.src.gameserver.GameTimeController;
import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.datatables.xml.MapRegionTable;
import com.src.gameserver.handler.IUserCommandHandler;
import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.MagicSkillUser;
import com.src.gameserver.network.serverpackets.SetupGauge;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.gameserver.util.Broadcast;

public class Escape implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		52
	};

	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		int unstuckTimer = activeChar.getAccessLevel().isGm() ? 1000 : Config.UNSTUCK_INTERVAL * 1000;
		if (activeChar.isCastingNow() || activeChar.isMovementDisabled() || activeChar.isMuted() || activeChar.isAlikeDead() || 
				activeChar.isInOlympiadMode() || activeChar.inObserverMode() || activeChar.isRiding() || activeChar.isDead() || 
				activeChar.isFestivalParticipant() || activeChar.isEnchanting() || activeChar.isTeleporting() || activeChar.isAfraid())  
			return false; 

		if(activeChar.isFestivalParticipant())
		{
			activeChar.sendMessage("You may not use an escape command in a festival.");
			return false;
		}

		if(activeChar.isInEvent())
		{
			activeChar.sendMessage("You may not use an escape skill in event.");
			return false;
		}
		
		if(activeChar.isInJail())
		{
			activeChar.sendMessage("You can not escape from jail.");
			return false;
		}

		if(activeChar.isInFunEvent())
		{
			activeChar.sendMessage("You may not escape from an Event.");
			return false;
		}

		if(activeChar.inObserverMode())
		{
			return false;
		}

		boolean fearActive = false;
		for(L2Effect effect : activeChar.getAllEffects())
		{
			switch(effect.getEffectType())
			{
				case FEAR:
				{
					fearActive = true;
					break;
				}
			default:
				break;
			}
			if(fearActive)
			{
				activeChar.sendMessage("You can't use unstuck when fear is active.");
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}

		activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_S2).addString("You are stuck. You will be transported to the nearest village in " + unstuckTimer / 60000 + " minutes."));

		activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		activeChar.setTarget(activeChar);
		activeChar.disableAllSkills();

		MagicSkillUser msk = new MagicSkillUser(activeChar, 1050, 1, unstuckTimer, 0);
		activeChar.setTarget(null);
		Broadcast.toSelfAndKnownPlayersInRadius(activeChar, msk, 810000);
		SetupGauge sg = new SetupGauge(0, unstuckTimer);
		activeChar.sendPacket(sg);

		EscapeFinalizer ef = new EscapeFinalizer(activeChar);
		activeChar.setSkillCast(ThreadPoolManager.getInstance().scheduleGeneral(ef, unstuckTimer));
		activeChar.forceIsCasting(GameTimeController.getGameTicks() + unstuckTimer / GameTimeController.MILLIS_IN_TICK);
		return true;
	}

	static class EscapeFinalizer implements Runnable
	{
		private L2PcInstance _activeChar;

		EscapeFinalizer(L2PcInstance activeChar)
		{
			_activeChar = activeChar;
		}

		public void run()
		{
			if(_activeChar.isDead() ||  _activeChar.isMovementDisabled())
			{
				return;
			}

			_activeChar.setIsIn7sDungeon(false);
			_activeChar.enableAllSkills();
			_activeChar.setIsCastingNow(false);

			try
			{
				_activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
			}
			catch(Throwable e)
			{
				
			}
		}
	}

	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}