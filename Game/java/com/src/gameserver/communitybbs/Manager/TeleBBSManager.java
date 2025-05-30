package com.src.gameserver.communitybbs.Manager;

import java.util.StringTokenizer;

import com.src.Config;
import com.src.gameserver.cache.HtmCache;
import com.src.gameserver.datatables.xml.TeleportLocationTable;
import com.src.gameserver.managers.TownManager;
import com.src.gameserver.model.L2TeleportLocation;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.olympiad.Olympiad;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.taskmanager.AttackStanceTaskManager;

/**
 * @author Matim
 * @version 1.0
 */
public class TeleBBSManager extends BaseBBSManager
{
	@Override
	public void parsecmd(String command, L2PcInstance activeChar)
	{
		if(command.startsWith("_bbstele;"))
		{
			if(!checkAllowed(activeChar))
				return;

			StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			int id = Integer.parseInt(st.nextToken());
			
			doTeleport(activeChar, id);
			
			String filename = "data/html/communityboard/custom/gk.htm";
			String content = HtmCache.getInstance().getHtm(filename);
			
			separateAndSend(content, activeChar);
		}
		
	}

	@Override
	public void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
	{
		
	}
	
	/**
	 * @param player
	 * @return
	 * <br><br>
	 * Check if player may use additional Community board functions.
	 * Such as buffer, gatekeeper.
	 */
	public boolean checkAllowed(L2PcInstance activeChar)
	{
		String msg = null;

		if(activeChar.isSitting())
			msg = "You can't use Community Gatekeeper when you sit!";
		else if(activeChar.isCastingNow())
			msg = "You can't use Community Gatekeeper when you cast!";
		else if(activeChar.isDead())
			msg = "You can't use Community Gatekeeper when you dead!";
		else if(activeChar.isInCombat())
			msg = "You can't use Community Gatekeeper when you in combat!";
		else if(Config.COMMUNITY_GATEKEEPER_EXCLUDE_ON.contains("JAIL") && activeChar.isInJail())
			msg = "You can't use Community Gatekeeper when you in jail!";
		else if(Config.COMMUNITY_GATEKEEPER_EXCLUDE_ON.contains("KARMA") && activeChar.getKarma() > 0)
			msg = "You can't use Community Gatekeeper when you have karma!";
		else if(Config.COMMUNITY_GATEKEEPER_EXCLUDE_ON.contains("CURSED") && activeChar.isCursedWeaponEquiped())
			msg = "You can't use Community Gatekeeper when you have Cursed Weapon!"; 
		else if(Config.COMMUNITY_GATEKEEPER_EXCLUDE_ON.contains("ATTACK") && AttackStanceTaskManager.getInstance().getAttackStanceTask(activeChar))
			msg = "You can't use Community Gatekeeper when you Attack!";
		else if(Config.COMMUNITY_GATEKEEPER_EXCLUDE_ON.contains("SEVEN") && activeChar.isIn7sDungeon())
			msg = "You can't use Community Gatekeeper when you on 7 Signs!";
		else if(Config.COMMUNITY_GATEKEEPER_EXCLUDE_ON.contains("RB") && activeChar.isInsideZone(L2Character.ZONE_NOSUMMONFRIEND))
			msg = "You can't use Community Gatekeeper when you on Raid Zone!";
		else if(Config.COMMUNITY_GATEKEEPER_EXCLUDE_ON.contains("PVP") && activeChar.isInsideZone(L2Character.ZONE_PVP))
			msg = "You can't use Community Gatekeeper when you on PvP Zone!";
		else if(Config.COMMUNITY_GATEKEEPER_EXCLUDE_ON.contains("PEACE") && activeChar.isInsideZone(L2Character.ZONE_PEACE))
			msg = "You can't use Community Gatekeeper when you on Peace Zone!";
		else if(Config.COMMUNITY_GATEKEEPER_EXCLUDE_ON.contains("NOTINTOWN") && TownManager.getInstance().getTown(activeChar.getX(), activeChar.getY(), activeChar.getZ()) == null)
			msg = "You can't use Community Gatekeeper when you no in town!";
		else if(Config.COMMUNITY_GATEKEEPER_EXCLUDE_ON.contains("SIEGE") && activeChar.isInsideZone(L2Character.ZONE_SIEGE))
			msg = "You can't use Community Gatekeeper when you on siege!";
		else if(Config.COMMUNITY_GATEKEEPER_EXCLUDE_ON.contains("OLYMPIAD") && (activeChar.isInOlympiadMode() || activeChar.isInsideZone(L2Character.ZONE_OLY) || Olympiad.getInstance().isRegistered(activeChar) || Olympiad.getInstance().isRegisteredInComp(activeChar))) 
			msg = "You can't use Community Gatekeeper when you on olympiad!";
		else if(Config.COMMUNITY_GATEKEEPER_EXCLUDE_ON.contains("EVENT") && activeChar.isFightingInEvent())
			msg = "You can't use Community Gatekeeper when you on event!";

		if(msg!=null)
			activeChar.sendMessage(msg);

		return msg==null;
	}
	
	private void doTeleport(L2PcInstance player, int val)
	{
		L2TeleportLocation list = TeleportLocationTable.getInstance().getTemplate(val);
		if (list != null)
		{
			player.teleToLocation(list.getLocX(), list.getLocY(), list.getLocZ(), true);
		}
		else
			System.out.println("No teleport destination with id:" + val);
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public static TeleBBSManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final TeleBBSManager INSTANCE = new TeleBBSManager();
	}
}