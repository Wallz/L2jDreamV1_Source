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
package handlers.itemhandlers;

import com.src.Config;
import com.src.gameserver.GameTimeController;
import com.src.gameserver.ai.CtrlIntention;
import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.xml.MapRegionTable;
import com.src.gameserver.handler.IItemHandler;
import com.src.gameserver.managers.CastleManager;
import com.src.gameserver.managers.ClanHallManager;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.actor.L2Playable;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.MagicSkillUser;
import com.src.gameserver.network.serverpackets.SetupGauge;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.thread.ThreadPoolManager;

public class ScrollOfEscape implements IItemHandler
{
	private static final int[] ITEM_IDS =
	{
		736,
		1830,
		1829,
		1538,
		3958,
		5858,
		5859,
		6663,
		6664,
		7117,
		7118,
		7119,
		7120,
		7121,
		7122,
		7123,
		7124,
		7125,
		7126,
		7127,
		7128,
		7129,
		7130,
		7131,
		7132,
		7133,
		7134,
		7135,
		7554,
		7555,
		7556,
		7557,
		7558,
		7559,
		7618,
		7619,
		9156
	};

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if(!(playable instanceof L2PcInstance))
		{
			return;
		}

		L2PcInstance activeChar = (L2PcInstance) playable;

		if(checkConditions(activeChar))
		{
			return;
		}

		if(activeChar.isSitting())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_MOVE_SITTING));
			return;
		}

		if(activeChar.isInEvent())
		{
			activeChar.sendMessage("You can not use an escape skill in event.");
			return;
		}
		if (activeChar.isFightingInEvent())
		{
			if ((activeChar.getEventName().equals("TVT") && !Config.TVT_ALLOW_SCROLL)
					|| (activeChar.getEventName().equals("CTF") && !Config.CTF_ALLOW_SCROLL)
					|| (activeChar.getEventName().equals("BW") && !Config.BW_ALLOW_SCROLL)
					|| (activeChar.getEventName().equals("DM") && !Config.DM_ALLOW_SCROLL))
			 	{
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			 		return;
			 	}
		}

		if(activeChar.isInOlympiadMode() || activeChar.inObserverMode())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
			return;
		}

		if(!Config.ALLOW_SOE_IN_PVP && activeChar.getPvpFlag() != 0)
		{
			activeChar.sendMessage("You can not use an escape skill when You are flaged.");
			return;
		}

		if(activeChar.isFestivalParticipant())
		{
			activeChar.sendPacket(SystemMessage.sendString("You may not use an escape skill in a festival."));
			return;
		}

		if(activeChar.isInJail())
		{
			activeChar.sendPacket(SystemMessage.sendString("You can not escape from jail."));
			return;
		}

		if(activeChar.isInDuel())
		{
			activeChar.sendPacket(SystemMessage.sendString("You can not use escape skills during a duel."));
			return;
		}

		activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		int itemId = item.getItemId();
		
		SystemMessage sm3 = new SystemMessage(SystemMessageId.USE_S1);
		sm3.addItemName(itemId);
		activeChar.sendPacket(sm3);
		
		int escapeSkill = itemId == 1538 || itemId == 5858 || itemId == 5859 || itemId == 3958 || itemId == 10130 ? 2036 : 2013;

		if(!activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false))
		{
			return;
		}

		activeChar.disableAllSkills();

		L2Object oldtarget = activeChar.getTarget();
		activeChar.setTarget(activeChar);

		L2Skill skill = SkillTable.getInstance().getInfo(escapeSkill, 1);
		MagicSkillUser msu = new MagicSkillUser(activeChar, escapeSkill, 1, skill.getHitTime(), 0);
		activeChar.broadcastPacket(msu);
		activeChar.setTarget(oldtarget);
		SetupGauge sg = new SetupGauge(0, skill.getHitTime());
		activeChar.sendPacket(sg);
		oldtarget = null;
		sg = null;

		activeChar.setTarget(null);
		
		activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_DISAPPEARED).addItemName(itemId));

		EscapeFinalizer ef = new EscapeFinalizer(activeChar, itemId);
		activeChar.setSkillCast(ThreadPoolManager.getInstance().scheduleEffect(ef, skill.getHitTime()));
		activeChar.forceIsCasting(GameTimeController.getGameTicks() + skill.getHitTime() / GameTimeController.MILLIS_IN_TICK);

		ef = null;
		activeChar = null;
	}

	static class EscapeFinalizer implements Runnable
	{
		private L2PcInstance _activeChar;
		private int _itemId;

		EscapeFinalizer(L2PcInstance activeChar, int itemId)
		{
			_activeChar = activeChar;
			_itemId = itemId;
		}

		public void run()
		{
			if(_activeChar.isDead())
			{
				return;
			}

			_activeChar.enableAllSkills();
			_activeChar.setIsCastingNow(false);
			_activeChar.setIsIn7sDungeon(false);

			try
			{
				if((_itemId == 1830 || _itemId == 5859))
				{
					if (CastleManager.getInstance().getCastleByOwner(_activeChar.getClan()) != null)
						_activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Castle);
					else
						_activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
				}
				else if((_itemId == 1829 || _itemId == 5858) && _activeChar.getClan() != null && ClanHallManager.getInstance().getClanHallByOwner(_activeChar.getClan()) != null) // escape to clan hall if own's one
				{
					_activeChar.teleToLocation(MapRegionTable.TeleportWhereType.ClanHall);
				}
				else if(_itemId == 5858)
				{
					_activeChar.sendPacket(new SystemMessage(SystemMessageId.CLAN_HAS_NO_CLAN_HALL));
					return;
				}
				else
				{
					if(_itemId < 7117)
					{
						_activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
					}
					else
					{
						switch(_itemId)
						{
							case 7117:
								_activeChar.teleToLocation(-84318, 244579, -3730, true);
								break;
							case 7554:
								_activeChar.teleToLocation(-84318, 244579, -3730, true);
								break;
							case 7118:
								_activeChar.teleToLocation(46934, 51467, -2977, true);
								break;
							case 7555:
								_activeChar.teleToLocation(46934, 51467, -2977, true);
								break;
							case 7119:
								_activeChar.teleToLocation(9745, 15606, -4574, true);
								break;
							case 7556:
								_activeChar.teleToLocation(9745, 15606, -4574, true);
								break;
							case 7120:
								_activeChar.teleToLocation(-44836, -112524, -235, true);
								break;
							case 7557:
								_activeChar.teleToLocation(-44836, -112524, -235, true);
								break;
							case 7121:
								_activeChar.teleToLocation(115113, -178212, -901, true);
								break;
							case 7558:
								_activeChar.teleToLocation(115113, -178212, -901, true);
								break;
							case 7122:
								_activeChar.teleToLocation(-80826, 149775, -3043, true);
								break;
							case 7123:
								_activeChar.teleToLocation(-12678, 122776, -3116, true);
								break;
							case 7124:
								_activeChar.teleToLocation(15670, 142983, -2705, true);
								break;
							case 7125:
								_activeChar.teleToLocation(17836, 170178, -3507, true);
								break;
							case 7126:
								_activeChar.teleToLocation(83400, 147943, -3404, true);
								break;
							case 7559:
								_activeChar.teleToLocation(83400, 147943, -3404, true);
								break;
							case 7127:
								_activeChar.teleToLocation(105918, 109759, -3207, true);
								break;
							case 7128:
								_activeChar.teleToLocation(111409, 219364, -3545, true);
								break;
							case 7129:
								_activeChar.teleToLocation(82956, 53162, -1495, true);
								break;
							case 7130:
								_activeChar.teleToLocation(85348, 16142, -3699, true);
								break;
							case 7131:
								_activeChar.teleToLocation(116819, 76994, -2714, true);
								break;
							case 7132:
								_activeChar.teleToLocation(146331, 25762, -2018, true);
								break;
							case 7133:
								_activeChar.teleToLocation(147928, -55273, -2734, true);
								break;
							case 7134:
								_activeChar.teleToLocation(43799, -47727, -798, true);
								break;
							case 7135:
								_activeChar.teleToLocation(87331, -142842, -1317, true);
								break;
							case 7618:
								_activeChar.teleToLocation(149864, -81062, -5618, true);
								break;
							case 7619:
								_activeChar.teleToLocation(108275, -53785, -2524, true);
								break;
							default:
								_activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
								break;
						}
					}
				}
			}
			catch(Throwable e)
			{
			}
		}
	}

	private static boolean checkConditions(L2PcInstance actor)
	{
		return actor.isStunned() || actor.isSleeping() || actor.isParalyzed() || actor.isFakeDeath() || actor.isTeleporting() || actor.isMuted() || actor.isAlikeDead() || actor.isAllSkillsDisabled() || actor.isCastingNow();
	}

	public int[] getItemIds()
	{
		return ITEM_IDS;
	}

}