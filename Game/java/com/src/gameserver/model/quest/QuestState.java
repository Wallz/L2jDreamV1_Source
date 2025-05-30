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
package com.src.gameserver.model.quest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.GameTimeController;
import com.src.gameserver.cache.HtmCache;
import com.src.gameserver.datatables.sql.ItemTable;
import com.src.gameserver.managers.QuestManager;
import com.src.gameserver.model.L2DropData;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2MonsterInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ExShowQuestMark;
import com.src.gameserver.network.serverpackets.InventoryUpdate;
import com.src.gameserver.network.serverpackets.ItemList;
import com.src.gameserver.network.serverpackets.PlaySound;
import com.src.gameserver.network.serverpackets.QuestList;
import com.src.gameserver.network.serverpackets.StatusUpdate;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.network.serverpackets.TutorialCloseHtml;
import com.src.gameserver.network.serverpackets.TutorialEnableClientEvent;
import com.src.gameserver.network.serverpackets.TutorialShowHtml;
import com.src.gameserver.network.serverpackets.TutorialShowQuestionMark;
import com.src.gameserver.skills.Stats;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;
import com.src.util.random.Rnd;

public final class QuestState
{
	private static final Log _log = LogFactory.getLog(Quest.class);

	public static final String SOUND_ACCEPT = "ItemSound.quest_accept";
	public static final String SOUND_ITEMGET = "ItemSound.quest_itemget";
	public static final String SOUND_MIDDLE = "ItemSound.quest_middle";
	public static final String SOUND_FINISH = "ItemSound.quest_finish";
	public static final String SOUND_GIVEUP = "ItemSound.quest_giveup";
	public static final String SOUND_JACKPOT = "ItemSound.quest_jackpot";
	public static final String SOUND_FANFARE = "ItemSound.quest_fanfare_2";
	
	private final String _questName;

	private final L2PcInstance _player;

	private State _state;
	
	private boolean _isCompleted;

	private Map<String, String> _vars;

	private boolean _isExitQuestOnCleanUp = false;

	QuestState(Quest quest, L2PcInstance player, State state, boolean completed)
	{
		_questName = quest.getName();
		_player = player;

		getPlayer().setQuestState(this);

		_isCompleted = completed;
		_state = state;
	}

	public String getQuestName()
	{
		return _questName;
	}

	public Quest getQuest()
	{
		return QuestManager.getInstance().getQuest(_questName);
	}

	public L2PcInstance getPlayer()
	{
		return _player;
	}

	public State getState()
	{
		return _state;
	}

	public boolean isCompleted()
	{
		return _isCompleted;
	}

	public boolean isStarted()
	{
		if(getStateId().equals("Start") || getStateId().equals("Completed"))
		{
			return false;
		}

		return true;
	}

	/**
	 * Return state of the quest after its initialization.<BR>
	 * <BR>
	 * <U><I>Actions :</I></U> <LI>Remove drops from previous state</LI> <LI>Set new state of the quest</LI> <LI>Add
	 * drop for new state</LI> <LI>Update information in database</LI> <LI>Send packet QuestList to client</LI>
	 *
	 * @param state
	 * @return object
	 */
	public Object setState(State state)
	{
		// set new state if it is not already in that state
		if(_state != state)
		{
			if(state == null)
			{
				return null;
			}

			if(getStateId().equals("Completed"))
			{
				Quest.createQuestInDb(this);
				_isCompleted = true;
			}
			else
			{
				Quest.createQuestInDb(this);
				_isCompleted = false;
			}
			
			_state = state;
			Quest.updateQuestInDb(this);
			getPlayer().sendPacket(new QuestList());
		}

		return state;
	}

	public String getStateId()
	{
		if(getState()!=null)
		{
			return getState().getName();
		}
		else
		{
			return "Created";
		}
	}

	String setInternal(String var, String val)
	{
		if(_vars == null)
		{
			_vars = new FastMap<String, String>();
		}

		if(val == null)
		{
			val = "";
		}

		_vars.put(var, val);

		return val;
	}

	public String set(String var, String val)
	{
		if(_vars == null)
		{
			_vars = new FastMap<String, String>();
		}

		if(val == null)
		{
			val = "";
		}

		String old = _vars.put(var, val);

		if(old != null)
		{
			Quest.updateQuestVarInDb(this, var, val);
		}
		else
		{
			Quest.createQuestVarInDb(this, var, val);
		}

		if(var == "cond")
		{
			try
			{
				int previousVal = 0;

				try
				{
					previousVal = Integer.parseInt(old);
				}
				catch(Exception ex)
				{
					previousVal = 0;
				}

				setCond(Integer.parseInt(val), previousVal);
			}
			catch(NumberFormatException e)
			{
				_log.warn(getPlayer().getName() + ", " + getQuestName() + " cond [" + val + "] is not an integer.  Value stored, but no packet was sent", e);
			}
		}
		return val;
	}

	private void setCond(int cond, int old)
	{
		int completedStateFlags = 0;

		if(cond == old)
		{
			return;
		}

		if(cond < 3 || cond > 31)
		{
			unset("__compltdStateFlags");
		}
		else
		{
			completedStateFlags = getInt("__compltdStateFlags");
		}

		if(completedStateFlags == 0)
		{
			if(cond > old + 1)
			{
				completedStateFlags = 0x80000001;

				completedStateFlags |= (1 << old) - 1;

				completedStateFlags |= 1 << cond - 1;
				set("__compltdStateFlags", String.valueOf(completedStateFlags));
			}
		}
		else
		{
			if(cond < old)
			{
				completedStateFlags &= (1 << cond) - 1;

				if(completedStateFlags == (1 << cond) - 1)
				{
					unset("__compltdStateFlags");
				}
				else
				{
					completedStateFlags |= 0x80000001;
					set("__compltdStateFlags", String.valueOf(completedStateFlags));
				}
			}
			else
			{
				completedStateFlags |= 1 << cond - 1;
				set("__compltdStateFlags", String.valueOf(completedStateFlags));
			}
		}

		QuestList ql = new QuestList();
		getPlayer().sendPacket(ql);
		ql = null;

		int questId = getQuest().getQuestIntId();

		if(questId > 0 && questId < 19999 && cond > 0)
		{
			getPlayer().sendPacket(new ExShowQuestMark(questId));
		}
	}

	public String unset(String var)
	{
		if(_vars == null)
		{
			return null;
		}

		String old = _vars.remove(var);

		if(old != null)
		{
			Quest.deleteQuestVarInDb(this, var);
		}

		return old;
	}

	public Object get(String var)
	{
		if(_vars == null)
		{
			return null;
		}

		return _vars.get(var);
	}

	public int getInt(String var)
	{
		if(_vars == null)
		{
			return 0;
		}

		final String variable = _vars.get(var);
		if(variable == null || variable.length() == 0)
		{
			return 0;
		}

		int varint = 0;

		try
		{
			varint = Integer.parseInt(_vars.get(var));
		}
		catch(Exception e)
		{
			_log.warn(getPlayer().getName() + ": variable " + var + " isn't an integer: " + varint, e);
			if(Config.AUTODELETE_INVALID_QUEST_DATA)
			{
				exitQuest(true);
			}
		}
		return varint;
	}

	public void addNotifyOfDeath(L2Character character)
	{
		if(character == null)
		{
			return;
		}

		character.addNotifyQuestOfDeath(this);
	}

	public int getQuestItemsCount(int itemId)
	{
		int count = 0;

		if(getPlayer() != null && getPlayer().getInventory() != null && getPlayer().getInventory().getItems() != null)
		{
			for(L2ItemInstance item : getPlayer().getInventory().getItems())
			{
				if(item != null && item.getItemId() == itemId)
				{
					count += item.getCount();
				}
			}
		}
		return count;
	}

	public int getEnchantLevel(int itemId)
	{
		L2ItemInstance enchanteditem = getPlayer().getInventory().getItemByItemId(itemId);

		if(enchanteditem == null)
		{
			return 0;
		}

		return enchanteditem.getEnchantLevel();
	}

	/**
	 * Reward player using quest reward config multiplier's.
	 * @param itemId : the item to reward.
	 * @param count : the amount to reward, before multiplier.
	 */
	public void rewardItems(int itemId, int count)
	{
		if (count <= 0)
			return;
		
		L2ItemInstance _tmpItem = ItemTable.getInstance().createDummyItem(itemId);
		
		if (_tmpItem == null)
			return;
		
		if (itemId == 57)
			count = (int) (count * Config.RATE_QUESTS_REWARD);
		else
			count = (int) (count * Config.RATE_QUESTS_REWARD);
		
		// Add items to player's inventory
		L2ItemInstance item = getPlayer().getInventory().addItem("Quest", itemId, count, getPlayer(), getPlayer().getTarget());
		
		if (item == null)
			return;
		
		// If item for reward is gold, send message of gold reward to client
		if (itemId == 57)
		{
			getPlayer().sendPacket(new SystemMessage(SystemMessageId.EARNED_S1_ADENA).addNumber(count));
		}
		// Otherwise, send message of object reward to client
		else
		{
			if (count > 1)
			{
				getPlayer().sendPacket(new SystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(item.getItemId()).addNumber(count));;
			}
			else
			{
				getPlayer().sendPacket(new SystemMessage(SystemMessageId.EARNED_ITEM_S1).addItemName(item.getItemId()));
			}
		}
		// send packets
		StatusUpdate su = new StatusUpdate(getPlayer());
		su.addAttribute(StatusUpdate.CUR_LOAD, getPlayer().getCurrentLoad());
		getPlayer().sendPacket(su);
	}
	
	public synchronized void giveItems(int itemId, int count)
	{
		giveItems(itemId, count, 0);
	}

	public synchronized void giveItems(int itemId, int count, int enchantlevel)
	{
		if(count <= 0)
		{
			return;
		}

		int questId = getQuest().getQuestIntId();

		if(itemId == 57 && !(questId >= 217 && questId <= 233) && !(questId >= 401 && questId <= 418))
		{
			count = (int) (count * Config.RATE_QUESTS_REWARD);
		}

		L2ItemInstance item = getPlayer().getInventory().addItem("Quest", itemId, count, getPlayer(), getPlayer().getTarget());

		if(item == null)
		{
			return;
		}

		if(getPlayer().isProcessingTransaction())
		{
			getPlayer().cancelActiveTrade();
		}

		if(enchantlevel > 0)
		{
			item.setEnchantLevel(enchantlevel);
		}

		if(itemId == 57)
		{
			getPlayer().sendPacket(new SystemMessage(SystemMessageId.EARNED_S1_ADENA).addNumber(count));
		}
		else
		{
			if(count > 1)
			{
				getPlayer().sendPacket(new SystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(item.getItemId()).addNumber(count));
			}
			else
			{
				getPlayer().sendPacket(new SystemMessage(SystemMessageId.EARNED_ITEM_S1).addItemName(item.getItemId()));
			}
		}
		
		getPlayer().sendPacket(new ItemList(getPlayer(), false));

		StatusUpdate su = new StatusUpdate(getPlayer().getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getPlayer().getCurrentLoad());
		getPlayer().sendPacket(su);
	}

	public boolean dropQuestItems(int itemId, int count, int neededCount, int dropChance, boolean sound)
	{
		return dropQuestItems(itemId, count, count, neededCount, dropChance, sound);
	}

	public boolean dropQuestItems(int itemId, int minCount, int maxCount, int neededCount, int dropChance, boolean sound)
	{
		dropChance *= Config.RATE_DROP_QUEST / (getPlayer().getParty() != null ? getPlayer().getParty().getMemberCount() : 1);

		int currentCount = getQuestItemsCount(itemId);

		if(neededCount > 0 && currentCount >= neededCount)
		{
			return true;
		}

		if(currentCount >= neededCount)
		{
			return true;
		}

		int itemCount = 0;
		int random = Rnd.get(L2DropData.MAX_CHANCE);

		while(random < dropChance)
		{
			if(minCount < maxCount)
			{
				itemCount += Rnd.get(minCount, maxCount);
			}
			else if(minCount == maxCount)
			{
				itemCount += minCount;
			}
			else
			{
				itemCount++;
			}

			dropChance -= L2DropData.MAX_CHANCE;
		}

		if(itemCount > 0)
		{
			if(neededCount > 0 && currentCount + itemCount > neededCount)
			{
				itemCount = neededCount - currentCount;
			}

			if(!getPlayer().getInventory().validateCapacityByItemId(itemId))
			{
				return false;
			}
			try
			{
				Thread.sleep(Rnd.get(3, 5)*1000);
			}
			catch(InterruptedException e)
			{
			}
			getPlayer().addItem("Quest", itemId, itemCount, getPlayer().getTarget(), true);

			if(sound)
			{
				playSound(currentCount + itemCount < neededCount ? "Itemsound.quest_itemget" : "Itemsound.quest_middle");
			}
		}

		return neededCount > 0 && currentCount + itemCount >= neededCount;
	}

	/**
	 * Drop Quest item with 100% luck.<br>
	 * Dropped item amount is always the same.
	 * @param itemId : int Item Identifier of the item to be dropped
	 * @param count : (minCount, maxCount) : Quantity of items to be dropped
	 * @param neededCount : Quantity of items needed for quest
	 * @return boolean indicating whether player has requested number of items
	 */
	public boolean dropAlwaysQuestItems(int itemId, int count, int neededCount)
	{
		return dropAlwaysQuestItems(itemId, count, count, neededCount);
	}
	
	/**
	 * Drop Quest item with 100% luck.
	 * @param itemId : Item Identifier of the item to be dropped
	 * @param minCount : Minimum quantity of items to be dropped
	 * @param maxCount : Maximum quantity of items to be dropped
	 * @param neededCount : Quantity of items needed for quest
	 * @return boolean indicating whether player has requested number of items
	 */
	public boolean dropAlwaysQuestItems(int itemId, int minCount, int maxCount, int neededCount)
	{
		int currentCount = getQuestItemsCount(itemId);
		
		if (neededCount > 0 && currentCount >= neededCount)
			return true;
		
		if (currentCount >= neededCount)
			return true;
		
		int itemCount = 0;
		
		// Get the item quantity dropped
		if (minCount < maxCount)
			itemCount += Rnd.get(minCount, maxCount);
		else if (minCount == maxCount)
			itemCount += minCount;
		else
			itemCount++;
		
		if (itemCount > 0)
		{
			// if over neededCount, just fill the gap
			if (neededCount > 0 && currentCount + itemCount > neededCount)
				itemCount = neededCount - currentCount;
			
			// Inventory slot check
			if (!getPlayer().getInventory().validateCapacityByItemId(itemId))
				return false;
			
			// Give the item to Player
			getPlayer().addItem("Quest", itemId, itemCount, getPlayer().getTarget(), true);
			
			// Play the sound
			playSound((currentCount + itemCount < neededCount) ? SOUND_ITEMGET : SOUND_MIDDLE);
		}
		
		return (neededCount > 0 && currentCount + itemCount >= neededCount);
	}
	
	public void addRadar(int x, int y, int z)
	{
		getPlayer().getRadar().addMarker(x, y, z);
	}

	public void removeRadar(int x, int y, int z)
	{
		getPlayer().getRadar().removeMarker(x, y, z);
	}

	public void clearRadar()
	{
		getPlayer().getRadar().removeAllMarkers();
	}

	public void takeItems(int itemId, int count)
	{
		L2ItemInstance item = getPlayer().getInventory().getItemByItemId(itemId);

		if(item == null)
		{
			return;
		}

		if(count < 0 || count > item.getCount())
		{
			count = item.getCount();
		}

		if(itemId == 57)
		{
			getPlayer().reduceAdena("Quest", count, getPlayer(), true);
		}
		else
		{
			if(item.isEquipped())
			{
				{ 
					L2ItemInstance[] unequiped = getPlayer().getInventory().unEquipItemInBodySlotAndRecord(item.getItem().getBodyPart()); 
					InventoryUpdate iu = new InventoryUpdate(); 
					for (L2ItemInstance itm: unequiped) 
						iu.addModifiedItem(itm); 
					getPlayer().sendPacket(iu); 
					getPlayer().broadcastUserInfo(); 
				}
			}

			getPlayer().destroyItemByItemId("Quest", itemId, count, getPlayer(), true);
		}
	}

	public void playSound(String sound)
	{
		getPlayer().sendPacket(new PlaySound(sound));
	}

	public void addExpAndSp(int exp, int sp)
	{
		getPlayer().addExpAndSp((int) getPlayer().calcStat(Stats.EXPSP_RATE, exp * Config.RATE_QUESTS_REWARD, null, null), (int) getPlayer().calcStat(Stats.EXPSP_RATE, sp * Config.RATE_QUESTS_REWARD, null, null));
	}

	public int getRandom(int max)
	{
		return Rnd.get(max);
	}

	public int getRandom(int min, int max)
	{
		return Rnd.get(min, max);
	}

	public int getItemEquipped(int loc)
	{
		return getPlayer().getInventory().getPaperdollItemId(loc);
	}

	public int getGameTicks()
	{
		return GameTimeController.getGameTicks();
	}

	public final boolean isExitQuestOnCleanUp()
	{
		return _isExitQuestOnCleanUp;
	}

	public void setIsExitQuestOnCleanUp(boolean isExitQuestOnCleanUp)
	{
		_isExitQuestOnCleanUp = isExitQuestOnCleanUp;
	}

	public void startQuestTimer(String name, long time)
	{
		getQuest().startQuestTimer(name, time, null, getPlayer());
	}

	public void startQuestTimer(String name, long time, L2Npc npc)
	{
		getQuest().startQuestTimer(name, time, npc, getPlayer());
	}

	public final QuestTimer getQuestTimer(String name)
	{
		return getQuest().getQuestTimer(name, null, getPlayer());
	}

	public L2Npc addSpawn(int npcId)
	{
		return addSpawn(npcId, getPlayer().getX(), getPlayer().getY(), getPlayer().getZ(), 0, false, 0);
	}

	public L2Npc addSpawn(int npcId, int despawnDelay)
	{
		return addSpawn(npcId, getPlayer().getX(), getPlayer().getY(), getPlayer().getZ(), 0, false, despawnDelay);
	}

	public L2Npc addSpawn(int npcId, int x, int y, int z)
	{
		return addSpawn(npcId, x, y, z, 0, false, 0);
	}

	public L2Npc addSpawn(int npcId, L2Character cha)
	{
		return addSpawn(npcId, cha, true, 0);
	}

	public L2Npc addSpawn(int npcId, L2Character cha, int despawnDelay)
	{
		return addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), true, despawnDelay);
	}

	public L2Npc addSpawn(int npcId, int x, int y, int z, int despawnDelay)
	{
		return addSpawn(npcId, x, y, z, 0, false, despawnDelay);
	}

	public L2Npc addSpawn(int npcId, L2Character cha, boolean randomOffset, int despawnDelay)
	{
		return addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), randomOffset, despawnDelay);
	}

	public L2Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, int despawnDelay)
	{
		return getQuest().addSpawn(npcId, x, y, z, heading, randomOffset, despawnDelay);
	}

	public String showHtmlFile(String fileName)
	{
		return getQuest().showHtmlFile(getPlayer(), fileName);
	}

	public QuestState exitQuest(boolean repeatable)
	{
		if(isCompleted())
		{
			return this;
		}

		_isCompleted = true;

		FastList<Integer> itemIdList = getQuest().getRegisteredItemIds();
		if(itemIdList != null)
		{
			for(FastList.Node<Integer> n = itemIdList.head(), end = itemIdList.tail(); (n = n.getNext()) != end;)
			{
				takeItems(n.getValue().intValue(), -1);
			}
		}

		if(repeatable)
		{
			getPlayer().delQuestState(getQuestName());
			Quest.deleteQuestInDb(this);

			_vars = null;
		}
		else
		{
			if(_vars != null)
			{
				for(String var : _vars.keySet())
				{
					unset(var);
				}
			}

			Quest.updateQuestInDb(this);
		}

		itemIdList = null;

		return this;
	}

	public void showQuestionMark(int number)
	{
		getPlayer().sendPacket(new TutorialShowQuestionMark(number));
	}

	public void playTutorialVoice(String voice)
	{
		getPlayer().sendPacket(new PlaySound(2, voice, 0, 0, getPlayer().getX(), getPlayer().getY(), getPlayer().getZ()));
	}

	public void showTutorialHTML(String html)
	{
		String text = HtmCache.getInstance().getHtm("data/scripts/quests/Q255_Tutorial/" + html);

		if(text == null)
		{
			_log.warn("missing html page data/scripts/quests/Q255_Tutorial/" + html);
			text = "<html><body>File data/scripts/quests/Q255_Tutorial/" + html + " not found or file is empty.</body></html>";
		}

		getPlayer().sendPacket(new TutorialShowHtml(text));
		text = null;
	}

	public void closeTutorialHtml()
	{
		getPlayer().sendPacket(new TutorialCloseHtml());
	}

	public void onTutorialClientEvent(int number)
	{
		getPlayer().sendPacket(new TutorialEnableClientEvent(number));
	}

	public void dropItem(L2MonsterInstance npc, L2PcInstance player, int itemId, int count)
	{
		npc.DropItem(player, itemId, count);
	}
	
	/**
	 * Insert (or Update) in the database variables that need to stay persistant for this player after a reboot.
	 * This function is for storage of values that do not related to a specific quest but are
	 * global for all quests.  For example, player's can get only once the adena and XP reward for
	 * the first class quests, but they can make more than one first class quest.
	 * @param var : String designating the name of the variable for the quest
	 * @param value : String designating the value of the variable for the quest
	 */
	public final void saveGlobalQuestVar(String var, String value)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("REPLACE INTO character_quest_global_data (charId,var,value) VALUES (?,?,?)");
			statement.setInt(1, _player.getObjectId());
			statement.setString(2, var);
			statement.setString(3, value);
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("Could not insert player's global quest variable: " + e.getMessage(), e);
		}
		finally
		{
			ResourceUtil.closeConnection(con);
		}
	}

	public boolean hasQuestItems(int itemId)
	{
		return getPlayer().getInventory().getItemByItemId(itemId) != null;
	}
}