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
package com.src.gameserver.model.itemcontainer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import javolution.util.FastList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.datatables.SkillTable;
import com.src.gameserver.datatables.sql.ItemTable;
import com.src.gameserver.datatables.xml.ArmorSetsTable;
import com.src.gameserver.model.L2ArmorSet;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2Skill;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance.ItemLocation;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.templates.item.L2Armor;
import com.src.gameserver.templates.item.L2EtcItem;
import com.src.gameserver.templates.item.L2EtcItemType;
import com.src.gameserver.templates.item.L2Item;
import com.src.gameserver.templates.item.L2Weapon;
import com.src.gameserver.templates.item.L2WeaponType;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public abstract class Inventory extends ItemContainer
{
	private static final Log _log = LogFactory.getLog(Inventory.class);

	public interface PaperdollListener
	{
		public void notifyEquiped(int slot, L2ItemInstance inst);

		public void notifyUnequiped(int slot, L2ItemInstance inst);
	}

	public static final int PAPERDOLL_UNDER = 0;
	public static final int PAPERDOLL_LEAR = 1;
	public static final int PAPERDOLL_REAR = 2;
	public static final int PAPERDOLL_NECK = 3;
	public static final int PAPERDOLL_LFINGER = 4;
	public static final int PAPERDOLL_RFINGER = 5;
	public static final int PAPERDOLL_HEAD = 6;
	public static final int PAPERDOLL_RHAND = 7;
	public static final int PAPERDOLL_LHAND = 8;
	public static final int PAPERDOLL_GLOVES = 9;
	public static final int PAPERDOLL_CHEST = 10;
	public static final int PAPERDOLL_LEGS = 11;
	public static final int PAPERDOLL_FEET = 12;
	public static final int PAPERDOLL_BACK = 13;
	public static final int PAPERDOLL_LRHAND = 14;
	public static final int PAPERDOLL_FACE = 15;
	public static final int PAPERDOLL_HAIR = 16;
	public static final int PAPERDOLL_DHAIR = 17;

	public static final double MAX_ARMOR_WEIGHT = 12000;

	private final L2ItemInstance[] _paperdoll;
	private final List<PaperdollListener> _paperdollListeners;

	protected int _totalWeight;

	private int _wearedMask;

	final class FormalWearListener implements PaperdollListener
	{
		@Override
		public void notifyUnequiped(int slot, L2ItemInstance item)
		{
			if(!(getOwner() != null && getOwner() instanceof L2PcInstance))
			{
				return;
			}

			L2PcInstance owner = (L2PcInstance) getOwner();
			if(item.getItemId() == 6408)
			{
				owner.setIsWearingFormalWear(false);
			}
		}

		@Override
		public void notifyEquiped(int slot, L2ItemInstance item)
		{
			if(!(getOwner() != null && getOwner() instanceof L2PcInstance))
			{
				return;
			}

			L2PcInstance owner = (L2PcInstance) getOwner();

			if(item.getItemId() == 6408)
			{
				owner.setIsWearingFormalWear(true);
				if(owner.isCastingNow())
				{
					owner.abortCast();
				}
				if(owner.isAttackingNow())
				{
					owner.abortAttack();
				}
				unEquipItemInSlot(PAPERDOLL_RHAND);
				unEquipItemInSlot(PAPERDOLL_LHAND);
				unEquipItemInSlot(PAPERDOLL_LRHAND);
			}
			else
			{
				if(!owner.isWearingFormalWear())
				{
					return;
				}
			}
		}
	}

	public static final class ChangeRecorder implements PaperdollListener
	{
		private final Inventory _inventory;
		private final List<L2ItemInstance> _changed;

		ChangeRecorder(Inventory inventory)
		{
			_inventory = inventory;
			_changed = new FastList<L2ItemInstance>();
			_inventory.addPaperdollListener(this);
		}

		@Override
		public void notifyEquiped(int slot, L2ItemInstance item)
		{
			if(!_changed.contains(item))
			{
				_changed.add(item);
			}
		}

		@Override
		public void notifyUnequiped(int slot, L2ItemInstance item)
		{
			if(!_changed.contains(item))
			{
				_changed.add(item);
			}
		}

		public L2ItemInstance[] getChangedItems()
		{
			return _changed.toArray(new L2ItemInstance[_changed.size()]);
		}
	}

	final class BowListener implements PaperdollListener
	{
		@Override
		public void notifyUnequiped(int slot, L2ItemInstance item)
		{
			if(slot != PAPERDOLL_LRHAND)
			{
				return;
			}

			if(item.getItemType() == L2WeaponType.BOW)
			{
				L2ItemInstance arrow = getPaperdollItem(PAPERDOLL_LHAND);

				if(arrow != null)
				{
					setPaperdollItem(PAPERDOLL_LHAND, null);
				}

				L2PcInstance player = null;
				L2Skill skill = null;
				if(item.getItemId() == 9140)
				{
					if(getOwner() instanceof L2PcInstance)
					{
						player = (L2PcInstance) getOwner();
						skill = SkillTable.getInstance().getInfo(3260, 1);
						player.removeSkill(skill);
						skill = SkillTable.getInstance().getInfo(3261, 1);
						player.removeSkill(skill);
						skill = SkillTable.getInstance().getInfo(3262, 1);
						player.removeSkill(skill);
						player.sendSkillList();
					}
				}
			}
		}

		@Override
		public void notifyEquiped(int slot, L2ItemInstance item)
		{
			if(slot != PAPERDOLL_LRHAND)
			{
				return;
			}

			if(item.getItemType() == L2WeaponType.BOW)
			{
				L2ItemInstance arrow = findArrowForBow(item.getItem());
				if(arrow != null)
				{
					setPaperdollItem(PAPERDOLL_LHAND, arrow);
				}

				L2PcInstance player = null;
				L2Skill skill = null;
				if(item.getItemId() == 9140)
				{
					if(getOwner() instanceof L2PcInstance)
					{
						player = (L2PcInstance) getOwner();
						skill = SkillTable.getInstance().getInfo(3260, 1);
						player.addSkill(skill, false);
						skill = SkillTable.getInstance().getInfo(3261, 1);
						player.addSkill(skill, false);
						skill = SkillTable.getInstance().getInfo(3262, 1);
						player.addSkill(skill, false);
						player.sendSkillList();
					}
				}
			}
		}
	}

	final class StatsListener implements PaperdollListener
	{
		@Override
		public void notifyUnequiped(int slot, L2ItemInstance item)
		{
			if(slot == PAPERDOLL_LRHAND)
			{
				return;
			}

			getOwner().removeStatsOwner(item);
		}

		@Override
		public void notifyEquiped(int slot, L2ItemInstance item)
		{
			if(slot == PAPERDOLL_LRHAND)
			{
				return;
			}
			getOwner().addStatFuncs(item.getStatFuncs(getOwner()));
		}
	}

	final class ItemPassiveSkillsListener implements PaperdollListener
	{
		@Override
		public void notifyUnequiped(int slot, L2ItemInstance item)
		{
			L2PcInstance player;

			if(getOwner() instanceof L2PcInstance)
			{
				player = (L2PcInstance) getOwner();
			}
			else
			{
				return;
			}

			L2Skill passiveSkill = null;
			L2Skill enchant4Skill = null;
			L2Skill skill = null;
			
			L2Item it = item.getItem();
			if(it instanceof L2Weapon)
			{
				passiveSkill = ((L2Weapon) it).getSkill();
				enchant4Skill = ((L2Weapon) it).getEnchant4Skill();
			}
			else if(it instanceof L2Armor)
			{
				passiveSkill = ((L2Armor) it).getSkill();
			}

			if(passiveSkill != null)
			{
				player.removeSkill(passiveSkill, false);
				player.sendSkillList();
			}

			if(enchant4Skill != null)
			{
				player.removeSkill(enchant4Skill, false);
				player.sendSkillList();
			}
			if (item.getItemId() == 8763)
			{
				skill = SkillTable.getInstance().getInfo(3626, 1);
				player.removeSkill(skill);
				skill = SkillTable.getInstance().getInfo(3627, 1);
				player.removeSkill(skill);
				skill = SkillTable.getInstance().getInfo(3628, 1);
				player.removeSkill(skill);
				player.sendSkillList();
			}
		}

		@Override
		public void notifyEquiped(int slot, L2ItemInstance item)
		{
			L2PcInstance player;
			if(getOwner() instanceof L2PcInstance)
			{
				player = (L2PcInstance) getOwner();
			}
			else
			{
				return;
			}

			L2Skill passiveSkill = null;
			L2Skill enchant4Skill = null;
			L2Skill skill = null;
			
			L2Item it = item.getItem();
			if(it instanceof L2Weapon)
			{					
				player.refreshExpertisePenalty();
				player.refreshMasteryWeapPenality();
				if (player.getExpertisePenalty() == 0)
                {	
                    passiveSkill = ((L2Weapon) it).getSkill();
				}

				if(item.getEnchantLevel() >= 4)
				{
					enchant4Skill = ((L2Weapon) it).getEnchant4Skill();
				}
			}
			else if(it instanceof L2Armor)
			{
				player.refreshExpertisePenalty();
				player.refreshMasteryPenality();
				passiveSkill = ((L2Armor) it).getSkill();
			}

			if(passiveSkill != null)
			{
				player.addSkill(passiveSkill, false);
				player.sendSkillList();
			}

			if(enchant4Skill != null)
			{
				player.addSkill(enchant4Skill, false);
				player.sendSkillList();
			}
			
			if (item.getItemId() == 8763)
			{
				skill = SkillTable.getInstance().getInfo(3626, 1);
				player.addSkill(skill, false);
				skill = SkillTable.getInstance().getInfo(3627, 1);
				player.addSkill(skill, false);
				skill = SkillTable.getInstance().getInfo(3628, 1);
				player.addSkill(skill, false);
				player.sendSkillList();
			}
		}
	}

	final class ArmorSetListener implements PaperdollListener
	{
		@Override
		public void notifyEquiped(int slot, L2ItemInstance item)
		{
			if(!(getOwner() instanceof L2PcInstance))
			{
				return;
			}

			L2PcInstance player = (L2PcInstance) getOwner();

			L2ItemInstance chestItem = getPaperdollItem(PAPERDOLL_CHEST);
			if(chestItem == null)
			{
				return;
			}

			L2ArmorSet armorSet = ArmorSetsTable.getInstance().getSet(chestItem.getItemId());
			if(armorSet == null)
			{
				return;
			}

			if(armorSet.containItem(slot, item.getItemId()))
			{
				if(armorSet.containAll(player))
				{
					L2Skill skill = SkillTable.getInstance().getInfo(armorSet.getSkillId(), 1);

					if(skill != null)
					{
						player.addSkill(skill, false);
						player.sendSkillList();
					}
					else
					{
						_log.warn("Inventory.ArmorSetListener: Incorrect skill: " + armorSet.getSkillId() + ".");
					}

					if(armorSet.containShield(player))
					{
						L2Skill skills = SkillTable.getInstance().getInfo(armorSet.getShieldSkillId(), 1);

						if(skills != null)
						{
							player.addSkill(skills, false);
							player.sendSkillList();
						}
						else
						{
							_log.warn("Inventory.ArmorSetListener: Incorrect skill: " + armorSet.getShieldSkillId() + ".");
						}

						skills = null;
					}

					if(armorSet.isEnchanted6(player))
					{
						int skillId = armorSet.getEnchant6skillId();

						if(skillId > 0)
						{
							L2Skill skille = SkillTable.getInstance().getInfo(skillId, 1);

							if(skille != null)
							{
								player.addSkill(skille, false);
								player.sendSkillList();
							}
							else
							{
								_log.warn("Inventory.ArmorSetListener: Incorrect skill: " + armorSet.getEnchant6skillId() + ".");
							}
						}
					}
				}
			}
			else if(armorSet.containShield(item.getItemId()))
			{
				if(armorSet.containAll(player))
				{
					L2Skill skills = SkillTable.getInstance().getInfo(armorSet.getShieldSkillId(), 1);

					if(skills != null)
					{
						player.addSkill(skills, false);
						player.sendSkillList();
					}
					else
					{
						_log.warn("Inventory.ArmorSetListener: Incorrect skill: " + armorSet.getShieldSkillId() + ".");
					}
				}
			}
		}

		@Override
		public void notifyUnequiped(int slot, L2ItemInstance item)
		{
			if(!(getOwner() instanceof L2PcInstance))
			{
				return;
			}

			L2PcInstance player = (L2PcInstance) getOwner();

			boolean remove = false;
			int removeSkillId1 = 0;
			int removeSkillId2 = 0;
			int removeSkillId3 = 0;

			if(slot == PAPERDOLL_CHEST)
			{
				L2ArmorSet armorSet = ArmorSetsTable.getInstance().getSet(item.getItemId());
				if(armorSet == null)
				{
					return;
				}

				remove = true;
				removeSkillId1 = armorSet.getSkillId();
				removeSkillId2 = armorSet.getShieldSkillId();
				removeSkillId3 = armorSet.getEnchant6skillId();

				armorSet = null;
			}
			else
			{
				L2ItemInstance chestItem = getPaperdollItem(PAPERDOLL_CHEST);
				if(chestItem == null)
				{
					return;
				}

				L2ArmorSet armorSet = ArmorSetsTable.getInstance().getSet(chestItem.getItemId());
				if(armorSet == null)
				{
					return;
				}

				if(armorSet.containItem(slot, item.getItemId()))
				{
					remove = true;
					removeSkillId1 = armorSet.getSkillId();
					removeSkillId2 = armorSet.getShieldSkillId();
					removeSkillId3 = armorSet.getEnchant6skillId();
				}
				else if(armorSet.containShield(item.getItemId()))
				{
					remove = true;
					removeSkillId2 = armorSet.getShieldSkillId();
				}
			}

			if(remove)
			{
				if(removeSkillId1 != 0)
				{
					L2Skill skill = SkillTable.getInstance().getInfo(removeSkillId1, 1);

					if(skill != null)
					{
						player.removeSkill(skill);
					}
					else
					{
						_log.warn("Inventory.ArmorSetListener: Incorrect skill: " + removeSkillId1 + ".");
					}

					skill = null;
				}

				if(removeSkillId2 != 0)
				{
					L2Skill skill = SkillTable.getInstance().getInfo(removeSkillId2, 1);

					if(skill != null)
					{
						player.removeSkill(skill);
					}
					else
					{
						_log.warn("Inventory.ArmorSetListener: Incorrect skill: " + removeSkillId2 + ".");
					}

					skill = null;
				}

				if(removeSkillId3 != 0)
				{
					L2Skill skill = SkillTable.getInstance().getInfo(removeSkillId3, 1);

					if(skill != null)
					{
						player.removeSkill(skill);
					}
					else
					{
						_log.warn("Inventory.ArmorSetListener: Incorrect skill: " + removeSkillId3 + ".");
					}
				}
				player.sendSkillList();
			}
		}
	}

	protected Inventory()
	{
		_paperdoll = new L2ItemInstance[0x12];
		_paperdollListeners = new FastList<PaperdollListener>();
		addPaperdollListener(new ArmorSetListener());
		addPaperdollListener(new BowListener());
		addPaperdollListener(new ItemPassiveSkillsListener());
		addPaperdollListener(new StatsListener());
		addPaperdollListener(new FormalWearListener());
	}

	protected abstract ItemLocation getEquipLocation();

	public ChangeRecorder newRecorder()
	{
		return new ChangeRecorder(this);
	}

	public L2ItemInstance dropItem(String process, L2ItemInstance item, L2PcInstance actor, L2Object reference)
	{
		synchronized (item)
		{
			if(!_items.contains(item))
			{
				return null;
			}

			removeItem(item);
			item.setOwnerId(process, 0, actor, reference);
			item.setLocation(ItemLocation.VOID);
			item.setLastChange(L2ItemInstance.REMOVED);

			item.updateDatabase();
			refreshWeight();
		}
		return item;
	}

	public L2ItemInstance dropItem(String process, int objectId, int count, L2PcInstance actor, L2Object reference)
	{
		L2ItemInstance item = getItemByObjectId(objectId);
		if(item == null)
		{
			return null;
		}

		if(item.getCount() > count)
		{
			item.changeCount(process, -count, actor, reference);
			item.setLastChange(L2ItemInstance.MODIFIED);
			item.updateDatabase();

			item = ItemTable.getInstance().createItem(process, item.getItemId(), count, actor, reference);

			item.updateDatabase();
			refreshWeight();

			return item;
		}
		else
		{
			return dropItem(process, item, actor, reference);
		}
	}

	@Override
	protected void addItem(L2ItemInstance item)
	{
		super.addItem(item);
		if(item.isEquipped())
			equipItem(item);
	}

	@Override
	protected void removeItem(L2ItemInstance item)
	{
		for(int i = 0; i < _paperdoll.length; i++)
		{
			if(_paperdoll[i] == item)
			{
				unEquipItemInSlot(i);
			}
		}

		super.removeItem(item);
	}

	public L2ItemInstance getPaperdollItem(int slot)
	{
		return _paperdoll[slot];
	}

	public L2ItemInstance getPaperdollItemByL2ItemId(int slot)
	{
		switch(slot)
		{
			case 0x01:
				return _paperdoll[0];
			case 0x04:
				return _paperdoll[1];
			case 0x02:
				return _paperdoll[2];
			case 0x08:
				return _paperdoll[3];
			case 0x20:
				return _paperdoll[4];
			case 0x10:
				return _paperdoll[5];
			case 0x40:
				return _paperdoll[6];
			case 0x80:
				return _paperdoll[7];
			case 0x0100:
				return _paperdoll[8];
			case 0x0200:
				return _paperdoll[9];
			case 0x0400:
				return _paperdoll[10];
			case 0x0800:
				return _paperdoll[11];
			case 0x1000:
				return _paperdoll[12];
			case 0x2000:
				return _paperdoll[13];
			case 0x4000:
				return _paperdoll[14];
			case 0x040000:
				return _paperdoll[15];
			case 0x010000:
				return _paperdoll[16];
			case 0x080000:
				return _paperdoll[17];
		}
		return null;
	}

	public int getPaperdollItemId(int slot)
	{
		L2ItemInstance item = _paperdoll[slot];

		if(item != null)
		{
			return item.getItemId();
		}
		else if(slot == PAPERDOLL_HAIR)
		{
			item = _paperdoll[PAPERDOLL_DHAIR];

			if(item != null)
			{
				return item.getItemId();
			}
		}
		return 0;
	}

	public int getPaperdollAugmentationId(int slot)
	{
		L2ItemInstance item = _paperdoll[slot];

		if(item != null)
		{
			if(item.getAugmentation() != null)
			{
				return item.getAugmentation().getAugmentationId();
			}
			else
			{
				return 0;
			}
		}
		return 0;
	}

	public int getPaperdollObjectId(int slot)
	{
		L2ItemInstance item = _paperdoll[slot];

		if(item != null)
		{
			return item.getObjectId();
		}
		else if(slot == PAPERDOLL_HAIR)
		{
			item = _paperdoll[PAPERDOLL_DHAIR];

			if(item != null)
			{
				return item.getObjectId();
			}
		}
		return 0;
	}

	public synchronized void addPaperdollListener(PaperdollListener listener)
	{
		_paperdollListeners.add(listener);
	}

	public synchronized void removePaperdollListener(PaperdollListener listener)
	{
		_paperdollListeners.remove(listener);
	}

	public L2ItemInstance setPaperdollItem(int slot, L2ItemInstance item)
	{
		L2ItemInstance old = _paperdoll[slot];

		if(old != item)
		{
			if(old != null)
			{
				_paperdoll[slot] = null;
				old.setLocation(getBaseLocation());
				old.setLastChange(L2ItemInstance.MODIFIED);

				int mask = 0;

				for(int i = 0; i < PAPERDOLL_LRHAND; i++)
				{
					L2ItemInstance pi = _paperdoll[i];

					if(pi != null)
					{
						mask |= pi.getItem().getItemMask();
					}

					pi = null;
				}

				_wearedMask = mask;

				for(PaperdollListener listener : _paperdollListeners)
				{
					if(listener == null)
					{
						continue;
					}

					listener.notifyUnequiped(slot, old);
				}

				if(old.isAugmented())
				{
					if(getOwner() != null && getOwner() instanceof L2PcInstance)
					{
						old.getAugmentation().removeBoni((L2PcInstance)getOwner());
					}
				}

				old.updateDatabase();
			}

			if(item != null)
			{
				_paperdoll[slot] = item;
				item.setLocation(getEquipLocation(), slot);
				item.setLastChange(L2ItemInstance.MODIFIED);
				_wearedMask |= item.getItem().getItemMask();

				for(PaperdollListener listener : _paperdollListeners)
				{
					listener.notifyEquiped(slot, item);
				}

				item.updateDatabase();
			}
		}
		return old;
	}

	public int getWearedMask()
	{
		return _wearedMask;
	}

	public int getSlotFromItem(L2ItemInstance item)
	{
		int slot = -1;
		int location = item.getEquipSlot();

		switch(location)
		{
			case PAPERDOLL_UNDER:
				slot = L2Item.SLOT_UNDERWEAR;
				break;
			case PAPERDOLL_LEAR:
				slot = L2Item.SLOT_L_EAR;
				break;
			case PAPERDOLL_REAR:
				slot = L2Item.SLOT_R_EAR;
				break;
			case PAPERDOLL_NECK:
				slot = L2Item.SLOT_NECK;
				break;
			case PAPERDOLL_RFINGER:
				slot = L2Item.SLOT_R_FINGER;
				break;
			case PAPERDOLL_LFINGER:
				slot = L2Item.SLOT_L_FINGER;
				break;
			case PAPERDOLL_HAIR:
				slot = L2Item.SLOT_HAIR;
				break;
			case PAPERDOLL_FACE:
				slot = L2Item.SLOT_FACE;
				break;
			case PAPERDOLL_DHAIR:
				slot = L2Item.SLOT_DHAIR;
				break;
			case PAPERDOLL_HEAD:
				slot = L2Item.SLOT_HEAD;
				break;
			case PAPERDOLL_RHAND:
				slot = L2Item.SLOT_R_HAND;
				break;
			case PAPERDOLL_LHAND:
				slot = L2Item.SLOT_L_HAND;
				break;
			case PAPERDOLL_GLOVES:
				slot = L2Item.SLOT_GLOVES;
				break;
			case PAPERDOLL_CHEST:
				slot = L2Item.SLOT_CHEST;
				break;
			case PAPERDOLL_LEGS:
				slot = L2Item.SLOT_LEGS;
				break;
			case PAPERDOLL_BACK:
				slot = L2Item.SLOT_BACK;
				break;
			case PAPERDOLL_FEET:
				slot = L2Item.SLOT_FEET;
				break;
			case PAPERDOLL_LRHAND:
				slot = L2Item.SLOT_LR_HAND;
				break;
		}

		return slot;
	}

	public L2ItemInstance[] unEquipItemInBodySlotAndRecord(int slot)
	{
		Inventory.ChangeRecorder recorder = newRecorder();

		try
		{
			unEquipItemInBodySlot(slot);

			if(getOwner() instanceof L2PcInstance)
			{
				((L2PcInstance) getOwner()).refreshExpertisePenalty();
			}
		}
		finally
		{
			removePaperdollListener(recorder);
		}

		return recorder.getChangedItems();
	}

	public L2ItemInstance unEquipItemInSlot(int pdollSlot)
	{
		return setPaperdollItem(pdollSlot, null);
	}

	public L2ItemInstance[] unEquipItemInSlotAndRecord(int slot)
	{
		Inventory.ChangeRecorder recorder = newRecorder();

		try
		{
			unEquipItemInSlot(slot);

			if(getOwner() instanceof L2PcInstance)
			{
				((L2PcInstance) getOwner()).refreshExpertisePenalty();
			}
		}
		finally
		{
			removePaperdollListener(recorder);
		}

		return recorder.getChangedItems();
	}

	private void unEquipItemInBodySlot(int slot)
	{
		int pdollSlot = -1;

		switch(slot)
		{
			case L2Item.SLOT_L_EAR:
				pdollSlot = PAPERDOLL_LEAR;
				break;
			case L2Item.SLOT_R_EAR:
				pdollSlot = PAPERDOLL_REAR;
				break;
			case L2Item.SLOT_NECK:
				pdollSlot = PAPERDOLL_NECK;
				break;
			case L2Item.SLOT_R_FINGER:
				pdollSlot = PAPERDOLL_RFINGER;
				break;
			case L2Item.SLOT_L_FINGER:
				pdollSlot = PAPERDOLL_LFINGER;
				break;
			case L2Item.SLOT_HAIR:
				pdollSlot = PAPERDOLL_HAIR;
				break;
			case L2Item.SLOT_FACE:
				pdollSlot = PAPERDOLL_FACE;
				break;
			case L2Item.SLOT_DHAIR:
				setPaperdollItem(PAPERDOLL_HAIR, null);
				setPaperdollItem(PAPERDOLL_FACE, null);
				pdollSlot = PAPERDOLL_DHAIR;
				break;
			case L2Item.SLOT_HEAD:
				pdollSlot = PAPERDOLL_HEAD;
				break;
			case L2Item.SLOT_R_HAND:
				pdollSlot = PAPERDOLL_RHAND;
				break;
			case L2Item.SLOT_L_HAND:
				pdollSlot = PAPERDOLL_LHAND;
				break;
			case L2Item.SLOT_GLOVES:
				pdollSlot = PAPERDOLL_GLOVES;
				break;
			case L2Item.SLOT_CHEST:
				pdollSlot = PAPERDOLL_CHEST;
				break;
			case L2Item.SLOT_FULL_ARMOR:
				pdollSlot = PAPERDOLL_CHEST;
				break;
			case L2Item.SLOT_LEGS:
				pdollSlot = PAPERDOLL_LEGS;
				break;
			case L2Item.SLOT_BACK:
				pdollSlot = PAPERDOLL_BACK;
				break;
			case L2Item.SLOT_FEET:
				pdollSlot = PAPERDOLL_FEET;
				break;
			case L2Item.SLOT_UNDERWEAR:
				pdollSlot = PAPERDOLL_UNDER;
				break;
			case L2Item.SLOT_LR_HAND:
				setPaperdollItem(PAPERDOLL_LHAND, null);
				setPaperdollItem(PAPERDOLL_RHAND, null);
				pdollSlot = PAPERDOLL_LRHAND;
				break;
		}
		if(pdollSlot >= 0)
		{
			setPaperdollItem(pdollSlot, null);
		}
	}

	public L2ItemInstance[] equipItemAndRecord(L2ItemInstance item)
	{
		Inventory.ChangeRecorder recorder = newRecorder();

		try
		{
			equipItem(item);
		}
		finally
		{
			removePaperdollListener(recorder);
		}

		return recorder.getChangedItems();
	}

	public synchronized void equipItem(L2ItemInstance item)
	{
		if(getOwner() instanceof L2PcInstance && ((L2PcInstance) getOwner()).getPrivateStoreType() != L2PcInstance.STORE_PRIVATE_NONE)
		{
			return;
		}

		if(getOwner() instanceof L2PcInstance)
		{
			L2PcInstance player = (L2PcInstance) getOwner();

			if(!player.isGM())
			{
				if(!player.isHero())
				{
					int itemId = item.getItemId();

					if(itemId >= 6611 && itemId <= 6621 || itemId == 6842)
					{
						return;
					}
				}
			}
		}

		int targetSlot = item.getItem().getBodyPart();
		switch(targetSlot)
		{
			case L2Item.SLOT_LR_HAND:
			{
				if(setPaperdollItem(PAPERDOLL_LHAND, null) != null)
				{
					setPaperdollItem(PAPERDOLL_RHAND, null);
					setPaperdollItem(PAPERDOLL_LHAND, null);
				}
				else
				{
					setPaperdollItem(PAPERDOLL_RHAND, null);
				}

				setPaperdollItem(PAPERDOLL_RHAND, item);
				setPaperdollItem(PAPERDOLL_LRHAND, item);

				break;
			}
			case L2Item.SLOT_L_HAND:
			{
				if(!(item.getItem() instanceof L2EtcItem) || item.getItem().getItemType() != L2EtcItemType.ARROW)
				{
					L2ItemInstance old1 = setPaperdollItem(PAPERDOLL_LRHAND, null);

					if(old1 != null)
					{
						setPaperdollItem(PAPERDOLL_RHAND, null);
					}
				}

				setPaperdollItem(PAPERDOLL_LHAND, null);
				setPaperdollItem(PAPERDOLL_LHAND, item);
				break;
			}
			case L2Item.SLOT_R_HAND:
			{
				if(_paperdoll[PAPERDOLL_LRHAND] != null)
				{
					setPaperdollItem(PAPERDOLL_LRHAND, null);
					setPaperdollItem(PAPERDOLL_LHAND, null);
					setPaperdollItem(PAPERDOLL_RHAND, null);
				}
				else
				{
					setPaperdollItem(PAPERDOLL_RHAND, null);
				}

				setPaperdollItem(PAPERDOLL_RHAND, item);
				break;
			}
			case L2Item.SLOT_L_EAR:
			case L2Item.SLOT_R_EAR:
			case L2Item.SLOT_L_EAR | L2Item.SLOT_R_EAR:
			{
				if(_paperdoll[PAPERDOLL_LEAR] == null)
				{
					setPaperdollItem(PAPERDOLL_LEAR, item);
				}
				else if(_paperdoll[PAPERDOLL_REAR] == null)
				{
					setPaperdollItem(PAPERDOLL_REAR, item);
				}
				else
				{
					setPaperdollItem(PAPERDOLL_LEAR, null);
					setPaperdollItem(PAPERDOLL_LEAR, item);
				}

				break;
			}
			case L2Item.SLOT_L_FINGER:
			case L2Item.SLOT_R_FINGER:
			case L2Item.SLOT_L_FINGER | L2Item.SLOT_R_FINGER:
			{
				if(_paperdoll[PAPERDOLL_LFINGER] == null)
				{
					setPaperdollItem(PAPERDOLL_LFINGER, item);
				}
				else if(_paperdoll[PAPERDOLL_RFINGER] == null)
				{
					setPaperdollItem(PAPERDOLL_RFINGER, item);
				}
				else
				{
					setPaperdollItem(PAPERDOLL_LFINGER, null);
					setPaperdollItem(PAPERDOLL_LFINGER, item);
				}

				break;
			}
			case L2Item.SLOT_NECK:
				setPaperdollItem(PAPERDOLL_NECK, item);
				break;
			case L2Item.SLOT_FULL_ARMOR:
				setPaperdollItem(PAPERDOLL_CHEST, null);
				setPaperdollItem(PAPERDOLL_LEGS, null);
				setPaperdollItem(PAPERDOLL_CHEST, item);
				break;
			case L2Item.SLOT_CHEST:
				setPaperdollItem(PAPERDOLL_CHEST, item);
				break;
			case L2Item.SLOT_LEGS:
			{
				L2ItemInstance chest = getPaperdollItem(PAPERDOLL_CHEST);
				if(chest != null && chest.getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR)
				{
					setPaperdollItem(PAPERDOLL_CHEST, null);
				}

				setPaperdollItem(PAPERDOLL_LEGS, null);
				setPaperdollItem(PAPERDOLL_LEGS, item);
				chest = null;
				break;
			}
			case L2Item.SLOT_FEET:
				setPaperdollItem(PAPERDOLL_FEET, item);
				break;
			case L2Item.SLOT_GLOVES:
				setPaperdollItem(PAPERDOLL_GLOVES, item);
				break;
			case L2Item.SLOT_HEAD:
				setPaperdollItem(PAPERDOLL_HEAD, item);
				break;
			case L2Item.SLOT_HAIR:
				if(setPaperdollItem(PAPERDOLL_DHAIR, null) != null)
				{
					setPaperdollItem(PAPERDOLL_DHAIR, null);
					setPaperdollItem(PAPERDOLL_HAIR, null);
					setPaperdollItem(PAPERDOLL_FACE, null);
				}
				else
				{
					setPaperdollItem(PAPERDOLL_HAIR, null);
				}

				setPaperdollItem(PAPERDOLL_HAIR, item);
				break;
			case L2Item.SLOT_FACE:
				if(setPaperdollItem(PAPERDOLL_DHAIR, null) != null)
				{
					setPaperdollItem(PAPERDOLL_DHAIR, null);
					setPaperdollItem(PAPERDOLL_HAIR, null);
					setPaperdollItem(PAPERDOLL_FACE, null);
				}
				else
				{
					setPaperdollItem(PAPERDOLL_FACE, null);
				}

				setPaperdollItem(PAPERDOLL_FACE, item);
				break;
			case L2Item.SLOT_DHAIR:
				if(setPaperdollItem(PAPERDOLL_HAIR, null) != null)
				{
					setPaperdollItem(PAPERDOLL_HAIR, null);
					setPaperdollItem(PAPERDOLL_FACE, null);
				}
				else
				{
					setPaperdollItem(PAPERDOLL_FACE, null);
				}

				setPaperdollItem(PAPERDOLL_DHAIR, item);
				break;
			case L2Item.SLOT_UNDERWEAR:
				setPaperdollItem(PAPERDOLL_UNDER, item);
				break;
			case L2Item.SLOT_BACK:
				setPaperdollItem(PAPERDOLL_BACK, item);
				break;
			default:
				_log.warn("unknown body slot:" + targetSlot);
		}
	}

	@Override
	protected void refreshWeight()
	{
		int weight = 0;

		for(L2ItemInstance item : _items)
		{
			if(item != null && item.getItem() != null)
			{
				weight += item.getItem().getWeight() * item.getCount();
			}
		}

		_totalWeight = weight;
	}

	public int getTotalWeight()
	{
		return _totalWeight;
	}

	public L2ItemInstance findArrowForBow(L2Item bow)
	{
		if (bow == null) 
		 	return null;
		
		int arrowsId = 0;

		switch(bow.getCrystalType())
		{
			default:
			case L2Item.CRYSTAL_NONE:
				arrowsId = 17;
				break;
			case L2Item.CRYSTAL_D:
				arrowsId = 1341;
				break;
			case L2Item.CRYSTAL_C:
				arrowsId = 1342;
				break;
			case L2Item.CRYSTAL_B:
				arrowsId = 1343;
				break;
			case L2Item.CRYSTAL_A:
				arrowsId = 1344;
				break;
			case L2Item.CRYSTAL_S:
				arrowsId = 1345;
				break;
		}

		return getItemByItemId(arrowsId);
	}

	@Override
	public void restore()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT object_id FROM items WHERE owner_id = ? AND (loc = ? OR loc = ?) " + "ORDER BY object_id DESC");
			statement.setInt(1, getOwnerId());
			statement.setString(2, getBaseLocation().name());
			statement.setString(3, getEquipLocation().name());
			ResultSet inv = statement.executeQuery();

			L2ItemInstance item;

			while(inv.next())
			{
				int getOwnerId = inv.getInt(1);

				item = L2ItemInstance.restoreFromDb(getOwnerId);

				if(item == null)
				{
					continue;
				}

				if(getOwner() instanceof L2PcInstance)
				{
					L2PcInstance player = (L2PcInstance) getOwner();

					if(!player.isGM() && !player.isHero())
					{
						int itemId = item.getItemId();

						if(itemId >= 6611 && itemId <= 6621 || itemId == 6842)
						{
							item.setLocation(ItemLocation.INVENTORY);
						}
					}
				}

				L2World.storeObject(item);

				if(item.isStackable() && getItemByItemId(item.getItemId()) != null)
				{
					addItem("Restore", item, null, getOwner());
				}
				else
				{
					addItem(item);
				}
			}

			inv.close();
			statement.close();
			refreshWeight();
		}
		catch(Exception e)
		{
			_log.error("Could not restore inventory", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	public void reloadEquippedItems()
	{
		L2ItemInstance item;

		int slot;

		for(L2ItemInstance element : _paperdoll)
		{
			item = element;
			if(item == null)
			{
				continue;
			}

			slot = item.getEquipSlot();

			for(PaperdollListener listener : _paperdollListeners)
			{
				if(listener == null)
				{
					continue;
				}

				listener.notifyUnequiped(slot, item);
				listener.notifyEquiped(slot, item);
			}
		}
	}
}