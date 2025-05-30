/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.src.gameserver.communitybbs.Manager;

import java.util.StringTokenizer;

import com.src.Config;
import com.src.gameserver.cache.HtmCache;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.managers.TownManager;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.olympiad.Olympiad;
import com.src.gameserver.taskmanager.AttackStanceTaskManager;

/**
 * @author Matim
 */
public class BuffBBSManager extends BaseBBSManager
{
	@Override
	public void parsecmd(String command, L2PcInstance activeChar)
	{
		if(command.startsWith("_bbsbuff"))
		{
			if(!checkAllowed(activeChar))
				return;

			String val = command.substring(8);
			StringTokenizer st = new StringTokenizer(val, "_");

			String a = st.nextToken();
			int id = Integer.parseInt(a);
			String b = st.nextToken();
			int lvl = Integer.parseInt(b);
			
			L2Skill skill = SkillTable.getInstance().getInfo(id, lvl);
			if(skill != null)
			{
				skill.getEffects(activeChar, activeChar);
			}

			String filename = "data/html/communityboard/custom/buffer.htm";
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
			msg = "You can't use Community Buffer when you sit!";
		else if(activeChar.isCastingNow())
			msg = "You can't use Community Buffer when you cast!";
		else if(activeChar.isDead())
			msg = "You can't use Community Buffer when you dead!";
		else if(activeChar.isInCombat())
			msg = "You can't use Community Buffer when you in combat!";
		else if(Config.COMMUNITY_BUFFER_EXCLUDE_ON.contains("JAIL") && activeChar.isInJail())
			msg = "You can't use Community Buffer when you in jail!";
		else if(Config.COMMUNITY_BUFFER_EXCLUDE_ON.contains("KARMA") && activeChar.getKarma() > 0)
			msg = "You can't use Community Buffer when you have karma!";
		else if(Config.COMMUNITY_BUFFER_EXCLUDE_ON.contains("CURSED") && activeChar.isCursedWeaponEquiped())
			msg = "You can't use Community Buffer when you have Cursed Weapon!"; 
		else if(Config.COMMUNITY_BUFFER_EXCLUDE_ON.contains("ATTACK") && AttackStanceTaskManager.getInstance().getAttackStanceTask(activeChar))
			msg = "You can't use Community Buffer when you Attack!";
		else if(Config.COMMUNITY_BUFFER_EXCLUDE_ON.contains("SEVEN") && activeChar.isIn7sDungeon())
			msg = "You can't use Community Buffer when you on 7 Signs!";
		else if(Config.COMMUNITY_BUFFER_EXCLUDE_ON.contains("RB") && activeChar.isInsideZone(L2Character.ZONE_NOSUMMONFRIEND))
			msg = "You can't use Community Buffer when you on Raid Zone!";
		else if(Config.COMMUNITY_BUFFER_EXCLUDE_ON.contains("PVP") && activeChar.isInsideZone(L2Character.ZONE_PVP))
			msg = "You can't use Community Buffer when you on PvP Zone!";
		else if(Config.COMMUNITY_BUFFER_EXCLUDE_ON.contains("PEACE") && activeChar.isInsideZone(L2Character.ZONE_PEACE))
			msg = "You can't use Community Buffer when you on Peace Zone!";
		else if(Config.COMMUNITY_BUFFER_EXCLUDE_ON.contains("NOTINTOWN") && TownManager.getInstance().getTown(activeChar.getX(), activeChar.getY(), activeChar.getZ()) == null)
			msg = "You can't use Community Buffer when you no in town!";
		else if(Config.COMMUNITY_BUFFER_EXCLUDE_ON.contains("SIEGE") && activeChar.isInsideZone(L2Character.ZONE_SIEGE))
			msg = "You can't use Community Buffer when you on siege!";
		else if(Config.COMMUNITY_BUFFER_EXCLUDE_ON.contains("OLYMPIAD") && (activeChar.isInOlympiadMode() || activeChar.isInsideZone(L2Character.ZONE_OLY) || Olympiad.getInstance().isRegistered(activeChar) || Olympiad.getInstance().isRegisteredInComp(activeChar))) 
			msg = "You can't use Community Buffer when you on olympiad!";
		else if(Config.COMMUNITY_BUFFER_EXCLUDE_ON.contains("EVENT") && activeChar.isFightingInEvent())
			msg = "You can't use Community Buffer when you on event!";

		if(msg!=null)
			activeChar.sendMessage(msg);

		return msg==null;
	}
	
	/**
	 * @return
	 */
	public static BuffBBSManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final BuffBBSManager INSTANCE = new BuffBBSManager();
	}
}