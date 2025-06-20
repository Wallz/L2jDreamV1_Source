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
package handlers.voicedcommandhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.GameTimeController;
import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.handler.IVoicedCommandHandler;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.managers.CoupleManager;
import com.src.gameserver.managers.GrandBossManager;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.sevensigns.SevenSigns;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ConfirmDlg;
import com.src.gameserver.network.serverpackets.MagicSkillUser;
import com.src.gameserver.network.serverpackets.SetupGauge;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.gameserver.util.Broadcast;
import com.src.util.database.L2DatabaseFactory;

public class Wedding implements IVoicedCommandHandler
{
	static final Log _log = LogFactory.getLog(Wedding.class);

	private static String[] _voicedCommands =
	{
		"divorce", "engage", "gotolove"
	};

	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
	{
        if(activeChar.isInFunEvent() || activeChar.isInOlympiadMode()){
            activeChar.sendMessage("Sorry, you are in event now.");
            return false;
        }
		if(command.startsWith("engage"))
		{
			return Engage(activeChar);
		}
		else if(command.startsWith("divorce"))
		{
			return Divorce(activeChar);
		}
		else if(command.startsWith("gotolove"))
		{
			return GoToLove(activeChar);
		}
		return false;
	}

	public boolean Divorce(L2PcInstance activeChar)
	{

		if(activeChar.getPartnerId() == 0)
		{
			return false;
		}

		int _partnerId = activeChar.getPartnerId();
		int _coupleId = activeChar.getCoupleId();
		int AdenaAmount = 0;

		if(activeChar.isMarried())
		{
			activeChar.sendMessage("You are now divorced.");
			AdenaAmount = (activeChar.getAdena() / 100) * Config.WEDDING_DIVORCE_COSTS;
			activeChar.getInventory().reduceAdena("Wedding", AdenaAmount, activeChar, null);
		}
		else
		{
			activeChar.sendMessage("You have broken up as a couple.");
		}

		L2PcInstance partner;
		partner = (L2PcInstance) L2World.getInstance().findObject(_partnerId);

		if(partner != null)
		{
			partner.setPartnerId(0);
			if(partner.isMarried())
			{
				partner.sendMessage("Your spouse has decided to divorce you.");
			}
			else
			{
				partner.sendMessage("Your fiance has decided to break the engagement with you.");
			}

			if(AdenaAmount > 0)
			{
				partner.addAdena("WEDDING", AdenaAmount, null, false);
			}
		}
		CoupleManager.getInstance().deleteCouple(_coupleId);
		return true;
	}

	public boolean Engage(L2PcInstance activeChar)
	{
		if(activeChar.getTarget() == null)
		{
			activeChar.sendMessage("You have no one targeted.");
			return false;
		}

		if(!(activeChar.getTarget() instanceof L2PcInstance))
		{
			activeChar.sendMessage("You can only ask another player to engage you.");
			return false;
		}

		L2PcInstance ptarget = (L2PcInstance) activeChar.getTarget();

		if(activeChar.getPartnerId() != 0)
		{
			activeChar.sendMessage("You are already engaged.");

			if(Config.WEDDING_PUNISH_INFIDELITY)
			{
				activeChar.startAbnormalEffect((short) 0x2000);
				int skillId;
				int skillLevel = 1;

				if(activeChar.getLevel() > 40)
				{
					skillLevel = 2;
				}

				if(activeChar.isMageClass())
				{
					skillId = 4361;
				}
				else
				{
					skillId = 4362;
				}

				L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
				if(activeChar.getFirstEffect(skill) == null)
				{
					skill.getEffects(activeChar, activeChar);
					activeChar.sendPacket(new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(skillId));
				}
			}
			return false;
		}

		if(ptarget.getObjectId() == activeChar.getObjectId())
		{
			activeChar.sendMessage("Is there something wrong with you, are you trying to go out with youre self?");
			return false;
		}

		if(ptarget.isMarried())
		{
			activeChar.sendMessage("Player already married.");
			return false;
		}

		if(ptarget.getClient().getConnection().getInetAddress().getHostAddress() == activeChar.getClient().getConnection().getInetAddress().getHostAddress() && !Config.WEDDING_SAMEIP)
		{
			activeChar.sendMessage("You can't ask someone of the same IP for engagement.");
			return false;
		}

		if(ptarget.isEngageRequest())
		{
			activeChar.sendMessage("Player already asked by someone else.");
			return false;
		}

		if(ptarget.getPartnerId() != 0)
		{
			activeChar.sendMessage("Player already engaged with someone else.");
			return false;
		}

		if(ptarget.getAppearance().getSex() == activeChar.getAppearance().getSex() && !Config.WEDDING_SAMESEX)
		{
			activeChar.sendMessage("Gay marriage is not allowed on this server!");
			return false;
		}

		boolean FoundOnFriendList = false;
		int objectId;
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("SELECT friend_id FROM character_friends WHERE char_id = ?");
			statement.setInt(1, ptarget.getObjectId());

			ResultSet rset = statement.executeQuery();
			while(rset.next())
			{
				objectId = rset.getInt("friend_id");
				if(objectId == activeChar.getObjectId())
				{
					FoundOnFriendList = true;
				}
			}
		}
		catch(Exception e)
		{
			_log.warn("could not read friend data:" + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(Exception e)
			{
				
			}
		}

		if(!FoundOnFriendList)
		{
			activeChar.sendMessage("The player you want to ask is not on your friends list, you must first be on each others friends list before you choose to engage.");
			return false;
		}

		ptarget.setEngageRequest(true, activeChar.getObjectId());
		ConfirmDlg dlg = new ConfirmDlg(614);
		dlg.addString(activeChar.getName() + " asking you to engage. Do you want to start a new relationship?");
		ptarget.sendPacket(dlg);
		return true;
	}

	public boolean GoToLove(L2PcInstance activeChar)
	{
		if(!activeChar.isMarried())
		{
			activeChar.sendMessage("You're not married.");
			return false;
		}

		if(activeChar.isInFunEvent())
		{
			activeChar.sendMessage("You're partener is in a Fun Event.");
			return false;
		}

		if(activeChar.getPartnerId() == 0)
		{
			activeChar.sendMessage("Couldn't find your fiance in the Database - Inform a Gamemaster.");
			_log.error("Married but couldn't find parter for " + activeChar.getName());
			return false;
		}

		if(GrandBossManager.getInstance().getZone(activeChar) != null)
		{
			activeChar.sendMessage("You're partener is in a Grand boss zone.");
			return false;
		}

		L2PcInstance partner;
		partner = (L2PcInstance) L2World.getInstance().findObject(activeChar.getPartnerId());
		if(partner == null)
		{
			activeChar.sendMessage("Your partner is not online.");
			return false;
		}
		else if(partner.isInJail())
		{
			activeChar.sendMessage("Your partner is in Jail.");
			return false;
		}
		else if(partner.isInOlympiadMode())
		{
			activeChar.sendMessage("Your partner is in the Olympiad now.");
			return false;
		}
		else if(partner.atEvent)
		{
			activeChar.sendMessage("Your partner is in an event.");
			return false;
		}
		else if(partner.isInDuel())
		{
			activeChar.sendMessage("Your partner is in a duel.");
			return false;
		}
		else if(partner.isFestivalParticipant())
		{
			activeChar.sendMessage("Your partner is in a festival.");
			return false;
		}
		else if(GrandBossManager.getInstance().getZone(partner) != null)
		{
			activeChar.sendMessage("Your partner is inside a Boss Zone.");
			return false;
		}
		else if(partner.isInParty() && partner.getParty().isInDimensionalRift())
		{
			activeChar.sendMessage("Your partner is in dimensional rift.");
			return false;
		}
		else if(partner.inObserverMode())
		{
			activeChar.sendMessage("Your partner is in the observation.");
			return false;
		}
		else if(partner.getClan() != null && CastleManager.getInstance().getCastleByOwner(partner.getClan()) != null && CastleManager.getInstance().getCastleByOwner(partner.getClan()).getSiege().getIsInProgress())
		{
			activeChar.sendMessage("Your partner is in siege, you can't go to your partner.");
			return false;
		}
		else if(activeChar.isInJail())
		{
			activeChar.sendMessage("You are in Jail!");
			return false;
		}
		else if(activeChar.isInOlympiadMode())
		{
			activeChar.sendMessage("You are in the Olympiad now.");
			return false;
		}
		else if(activeChar.atEvent)
		{
			activeChar.sendMessage("You are in an event.");
			return false;
		}
		else if(activeChar.isInEvent())
		{
			activeChar.sendMessage("You may not use go to love when you are in event.");
			return false;
		}
		else if(activeChar.isInDuel())
		{
			activeChar.sendMessage("You are in a duel!");
			return false;
		}
		else if(activeChar.inObserverMode())
		{
			activeChar.sendMessage("You are in the observation.");
			return false;
		}
		else if(activeChar.getClan() != null && CastleManager.getInstance().getCastleByOwner(activeChar.getClan()) != null && CastleManager.getInstance().getCastleByOwner(activeChar.getClan()).getSiege().getIsInProgress())
		{
			activeChar.sendMessage("You are in siege, you can't go to your partner.");
			return false;
		}
		else if(activeChar.isFestivalParticipant())
		{
			activeChar.sendMessage("You are in a festival.");
			return false;
		}
		else if(activeChar.isInParty() && activeChar.getParty().isInDimensionalRift())
		{
			activeChar.sendMessage("You are in the dimensional rift.");
			return false;
		}
		else if(activeChar.isCursedWeaponEquiped())
		{
			activeChar.sendMessage("You have a cursed weapon, you can't go to your partner.");
			return false;
		}
		else if(activeChar.isInsideZone(L2Character.ZONE_NOSUMMONFRIEND))
		{
			activeChar.sendMessage("You are in area which blocks summoning.");
			return false;
		}
		else if (partner.isIn7sDungeon() && !activeChar.isIn7sDungeon()) 
		{ 
			int playerCabal = SevenSigns.getInstance().getPlayerCabal(activeChar); 
			boolean isSealValidationPeriod = SevenSigns.getInstance().isSealValidationPeriod(); 
			int compWinner = SevenSigns.getInstance().getCabalHighestScore(); 
			
			if (isSealValidationPeriod) 
			{ 
				if (playerCabal != compWinner) 
				{ 
					activeChar.sendMessage("Your Partner is in a Seven Signs Dungeon and you are not in the winner Cabal!"); 
					return false; 
				} 
			} 
			else 
			{ 
				if (playerCabal == SevenSigns.CABAL_NULL) 
				{ 
					activeChar.sendMessage("Your Partner is in a Seven Signs Dungeon and you are not registered!"); 
					return false; 
				} 
			} 
		}
		
		int teleportTimer = Config.WEDDING_TELEPORT_DURATION * 1000;

		activeChar.sendMessage("After " + teleportTimer / 60000 + " min. you will be teleported to your fiance.");
		activeChar.getInventory().reduceAdena("Wedding", Config.WEDDING_TELEPORT_PRICE, activeChar, null);

		activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		activeChar.setTarget(activeChar);
		activeChar.disableAllSkills();

		MagicSkillUser msk = new MagicSkillUser(activeChar, 1050, 1, teleportTimer, 0);
		Broadcast.toSelfAndKnownPlayersInRadius(activeChar, msk, 810000);
		SetupGauge sg = new SetupGauge(0, teleportTimer);
		activeChar.sendPacket(sg);

		EscapeFinalizer ef = new EscapeFinalizer(activeChar, partner.getX(), partner.getY(), partner.getZ(), partner.isIn7sDungeon());
		activeChar.setSkillCast(ThreadPoolManager.getInstance().scheduleGeneral(ef, teleportTimer));
		activeChar.forceIsCasting(GameTimeController.getGameTicks() + teleportTimer / GameTimeController.MILLIS_IN_TICK);
		return true;
	}

	static class EscapeFinalizer implements Runnable
	{
		private L2PcInstance _activeChar;
		private int _partnerx;
		private int _partnery;
		private int _partnerz;
		private boolean _to7sDungeon;

		EscapeFinalizer(L2PcInstance activeChar, int x, int y, int z, boolean to7sDungeon)
		{
			_activeChar = activeChar;
			_partnerx = x;
			_partnery = y;
			_partnerz = z;
			_to7sDungeon = to7sDungeon;
		}

		public void run()
		{
			if(_activeChar.isDead())
			{
				return;
			}

			_activeChar.setIsIn7sDungeon(_to7sDungeon);
			_activeChar.enableAllSkills();
			_activeChar.setIsCastingNow(false);

			try
			{
				_activeChar.teleToLocation(_partnerx, _partnery, _partnerz);
			}
			catch(Throwable e)
			{
				_log.error(e.getMessage(), e);
			}
		}
	}

	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}