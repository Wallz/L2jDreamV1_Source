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
package com.src.gameserver.datatables.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.Item;
import com.src.gameserver.datatables.xml.L2PetDataTable;
import com.src.gameserver.idfactory.IdFactory;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.instance.L2GrandBossInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2ItemInstance.ItemLocation;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2RaidBossInstance;
import com.src.gameserver.skills.SkillsEngine;
import com.src.gameserver.templates.StatsSet;
import com.src.gameserver.templates.item.L2Armor;
import com.src.gameserver.templates.item.L2ArmorType;
import com.src.gameserver.templates.item.L2EtcItem;
import com.src.gameserver.templates.item.L2EtcItemType;
import com.src.gameserver.templates.item.L2Item;
import com.src.gameserver.templates.item.L2Weapon;
import com.src.gameserver.templates.item.L2WeaponType;
import com.src.gameserver.thread.ThreadPoolManager;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class ItemTable
{
	private static final Log _log = LogFactory.getLog(ItemTable.class.getName());

	private static Logger _logItems = Logger.getLogger("item");

	private static final Map<String, Integer> _materials = new FastMap<String, Integer>();
	private static final Map<String, Integer> _crystalTypes = new FastMap<String, Integer>();
	private static final Map<String, L2WeaponType> _weaponTypes = new FastMap<String, L2WeaponType>();
	private static final Map<String, L2ArmorType> _armorTypes = new FastMap<String, L2ArmorType>();
	private static final Map<String, Integer> _slots = new FastMap<String, Integer>();

	private L2Item[] _allTemplates;
	private Map<Integer, L2EtcItem> _etcItems;
	private Map<Integer, L2Armor> _armors;
	private Map<Integer, L2Weapon> _weapons;

	private boolean _initialized = true;

	static
	{
		_materials.put("paper", L2Item.MATERIAL_PAPER);
		_materials.put("wood", L2Item.MATERIAL_WOOD);
		_materials.put("liquid", L2Item.MATERIAL_LIQUID);
		_materials.put("cloth", L2Item.MATERIAL_CLOTH);
		_materials.put("leather", L2Item.MATERIAL_LEATHER);
		_materials.put("horn", L2Item.MATERIAL_HORN);
		_materials.put("bone", L2Item.MATERIAL_BONE);
		_materials.put("bronze", L2Item.MATERIAL_BRONZE);
		_materials.put("fine_steel", L2Item.MATERIAL_FINE_STEEL);
		_materials.put("cotton", L2Item.MATERIAL_FINE_STEEL);
		_materials.put("mithril", L2Item.MATERIAL_MITHRIL);
		_materials.put("silver", L2Item.MATERIAL_SILVER);
		_materials.put("gold", L2Item.MATERIAL_GOLD);
		_materials.put("adamantaite", L2Item.MATERIAL_ADAMANTAITE);
		_materials.put("steel", L2Item.MATERIAL_STEEL);
		_materials.put("oriharukon", L2Item.MATERIAL_ORIHARUKON);
		_materials.put("blood_steel", L2Item.MATERIAL_BLOOD_STEEL);
		_materials.put("crystal", L2Item.MATERIAL_CRYSTAL);
		_materials.put("damascus", L2Item.MATERIAL_DAMASCUS);
		_materials.put("chrysolite", L2Item.MATERIAL_CHRYSOLITE);
		_materials.put("scale_of_dragon", L2Item.MATERIAL_SCALE_OF_DRAGON);
		_materials.put("dyestuff", L2Item.MATERIAL_DYESTUFF);
		_materials.put("cobweb", L2Item.MATERIAL_COBWEB);
		_materials.put("seed", L2Item.MATERIAL_SEED);

		_crystalTypes.put("s", L2Item.CRYSTAL_S);
		_crystalTypes.put("a", L2Item.CRYSTAL_A);
		_crystalTypes.put("b", L2Item.CRYSTAL_B);
		_crystalTypes.put("c", L2Item.CRYSTAL_C);
		_crystalTypes.put("d", L2Item.CRYSTAL_D);
		_crystalTypes.put("none", L2Item.CRYSTAL_NONE);

		_weaponTypes.put("blunt", L2WeaponType.BLUNT);
		_weaponTypes.put("bow", L2WeaponType.BOW);
		_weaponTypes.put("dagger", L2WeaponType.DAGGER);
		_weaponTypes.put("dual", L2WeaponType.DUAL);
		_weaponTypes.put("dualfist", L2WeaponType.DUALFIST);
		_weaponTypes.put("etc", L2WeaponType.ETC);
		_weaponTypes.put("fist", L2WeaponType.FIST);
		_weaponTypes.put("none", L2WeaponType.NONE);
		_weaponTypes.put("pole", L2WeaponType.POLE);
		_weaponTypes.put("sword", L2WeaponType.SWORD);
		_weaponTypes.put("bigsword", L2WeaponType.BIGSWORD);
		_weaponTypes.put("pet", L2WeaponType.PET);
		_weaponTypes.put("rod", L2WeaponType.ROD);
		_weaponTypes.put("bigblunt", L2WeaponType.BIGBLUNT);
		_armorTypes.put("none", L2ArmorType.NONE);
		_armorTypes.put("light", L2ArmorType.LIGHT);
		_armorTypes.put("heavy", L2ArmorType.HEAVY);
		_armorTypes.put("magic", L2ArmorType.MAGIC);
		_armorTypes.put("pet", L2ArmorType.PET);

		_slots.put("chest", L2Item.SLOT_CHEST);
		_slots.put("fullarmor", L2Item.SLOT_FULL_ARMOR);
		_slots.put("head", L2Item.SLOT_HEAD);
		_slots.put("hair", L2Item.SLOT_HAIR);
		_slots.put("face", L2Item.SLOT_FACE);
		_slots.put("dhair", L2Item.SLOT_DHAIR);
		_slots.put("underwear", L2Item.SLOT_UNDERWEAR);
		_slots.put("back", L2Item.SLOT_BACK);
		_slots.put("neck", L2Item.SLOT_NECK);
		_slots.put("legs", L2Item.SLOT_LEGS);
		_slots.put("feet", L2Item.SLOT_FEET);
		_slots.put("gloves", L2Item.SLOT_GLOVES);
		_slots.put("chest,legs", L2Item.SLOT_CHEST | L2Item.SLOT_LEGS);
		_slots.put("rhand", L2Item.SLOT_R_HAND);
		_slots.put("lhand", L2Item.SLOT_L_HAND);
		_slots.put("lrhand", L2Item.SLOT_LR_HAND);
		_slots.put("rear,lear", L2Item.SLOT_R_EAR | L2Item.SLOT_L_EAR);
		_slots.put("rfinger,lfinger", L2Item.SLOT_R_FINGER | L2Item.SLOT_L_FINGER);
		_slots.put("none", L2Item.SLOT_NONE);
		_slots.put("wolf", L2Item.SLOT_WOLF);
		_slots.put("hatchling", L2Item.SLOT_HATCHLING);
		_slots.put("strider", L2Item.SLOT_STRIDER);
		_slots.put("babypet", L2Item.SLOT_BABYPET);
	}

	private static ItemTable _instance;

	private static final String[] SQL_ITEM_SELECTS =
	{
			"SELECT item_id, name, crystallizable, item_type, weight, consume_type, material, crystal_type, duration, price, crystal_count, sellable, dropable, destroyable, tradeable FROM etcitem",

			"SELECT item_id, name, bodypart, crystallizable, armor_type, weight," + " material, crystal_type, avoid_modify, duration, p_def, m_def, mp_bonus," + " price, crystal_count, sellable, dropable, destroyable, tradeable, item_skill_id, item_skill_lvl FROM armor",

			"SELECT item_id, name, bodypart, crystallizable, weight, soulshots, spiritshots," + " material, crystal_type, p_dam, rnd_dam, weaponType, critical, hit_modify, avoid_modify," + " shield_def, shield_def_rate, atk_speed, mp_consume, m_dam, duration, price, crystal_count," + " sellable, dropable, destroyable, tradeable, item_skill_id, item_skill_lvl,enchant4_skill_id,enchant4_skill_lvl, onCast_skill_id, onCast_skill_lvl," + " onCast_skill_chance, onCrit_skill_id, onCrit_skill_lvl, onCrit_skill_chance FROM weapon"
	};

	private static final String[] SQL_CUSTOM_ITEM_SELECTS =
	{
			"SELECT item_id, name, crystallizable, item_type, weight, consume_type, material, crystal_type, duration, price, crystal_count, sellable, dropable, destroyable, tradeable FROM custom_etcitem",

			"SELECT item_id, name, bodypart, crystallizable, armor_type, weight," + " material, crystal_type, avoid_modify, duration, p_def, m_def, mp_bonus," + " price, crystal_count, sellable, dropable, destroyable, tradeable, item_skill_id, item_skill_lvl FROM custom_armor",

			"SELECT item_id, name, bodypart, crystallizable, weight, soulshots, spiritshots," + " material, crystal_type, p_dam, rnd_dam, weaponType, critical, hit_modify, avoid_modify," + " shield_def, shield_def_rate, atk_speed, mp_consume, m_dam, duration, price, crystal_count," + " sellable, dropable, destroyable, tradeable, item_skill_id, item_skill_lvl,enchant4_skill_id,enchant4_skill_lvl, onCast_skill_id, onCast_skill_lvl," + " onCast_skill_chance, onCrit_skill_id, onCrit_skill_lvl, onCrit_skill_chance FROM custom_weapon"
	};

	private static final Map<Integer, Item> itemData = new FastMap<Integer, Item>();
	private static final Map<Integer, Item> weaponData = new FastMap<Integer, Item>();
	private static final Map<Integer, Item> armorData = new FastMap<Integer, Item>();

	public static ItemTable getInstance()
	{
		if(_instance == null)
		{
			_instance = new ItemTable();
		}
		return _instance;
	}

	public Item newItem()
	{
		return new Item();
	}

	public ItemTable()
	{
		_etcItems = new FastMap<Integer, L2EtcItem>();
		_armors = new FastMap<Integer, L2Armor>();
		_weapons = new FastMap<Integer, L2Weapon>();

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			for(String selectQuery : SQL_ITEM_SELECTS)
			{
				PreparedStatement statement = con.prepareStatement(selectQuery);
				ResultSet rset = statement.executeQuery();

				while(rset.next())
				{
					if(selectQuery.endsWith("etcitem"))
					{
						Item newItem = readItem(rset);
						itemData.put(newItem.id, newItem);
						newItem = null;
					}
					else if(selectQuery.endsWith("armor"))
					{
						Item newItem = readArmor(rset);
						armorData.put(newItem.id, newItem);
						newItem = null;
					}
					else if(selectQuery.endsWith("weapon"))
					{
						Item newItem = readWeapon(rset);
						weaponData.put(newItem.id, newItem);
						newItem = null;
					}
				}

				statement.close();
				rset.close();
				statement = null;
				rset = null;
			}
		}
		catch(Exception e)
		{
			_log.error("data error on item: ", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}

		if(Config.CUSTOM_ITEM_TABLES)
		{
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				for(String selectQuery : SQL_CUSTOM_ITEM_SELECTS)
				{
					PreparedStatement statement = con.prepareStatement(selectQuery);
					ResultSet rset = statement.executeQuery();

					while(rset.next())
					{
						if(selectQuery.endsWith("etcitem"))
						{
							Item newItem = readItem(rset);

							if(itemData.containsKey(newItem.id))
							{
								itemData.remove(newItem.id);
							}

							itemData.put(newItem.id, newItem);
							newItem = null;
						}
						else if(selectQuery.endsWith("armor"))
						{
							Item newItem = readArmor(rset);

							if(armorData.containsKey(newItem.id))
							{
								armorData.remove(newItem.id);
							}

							armorData.put(newItem.id, newItem);
							newItem = null;
						}
						else if(selectQuery.endsWith("weapon"))
						{
							Item newItem = readWeapon(rset);

							if(weaponData.containsKey(newItem.id))
							{
								weaponData.remove(newItem.id);
							}

							weaponData.put(newItem.id, newItem);
							newItem = null;
						}
					}

					statement.close();
					rset.close();
					statement = null;
					rset = null;
				}
			}
			catch(Exception e)
			{
				_log.error("data error on custom_item: ", e);
			}
			finally
			{
				ResourceUtil.closeConnection(con); 
			}
		}

		for(L2Armor armor : SkillsEngine.getInstance().loadArmors(armorData))
		{
			_armors.put(armor.getItemId(), armor);
		}
		_log.info("ItemTable: Loaded " + _armors.size() + " Armors.");

		for(L2EtcItem item : SkillsEngine.getInstance().loadItems(itemData))
		{
			_etcItems.put(item.getItemId(), item);
		}
		_log.info("ItemTable: Loaded " + _etcItems.size() + " Items.");

		for(L2Weapon weapon : SkillsEngine.getInstance().loadWeapons(weaponData))
		{
			_weapons.put(weapon.getItemId(), weapon);
		}
		_log.info("ItemTable: Loaded " + _weapons.size() + " Weapons.");

		buildFastLookupTable();
	}

	private Item readWeapon(ResultSet rset) throws SQLException
	{
		Item item = new Item();
		item.set = new StatsSet();
		item.type = _weaponTypes.get(rset.getString("weaponType"));
		item.id = rset.getInt("item_id");
		item.name = rset.getString("name");

		item.set.set("item_id", item.id);
		item.set.set("name", item.name);

		if(item.type == L2WeaponType.NONE)
		{
			item.set.set("type1", L2Item.TYPE1_SHIELD_ARMOR);
			item.set.set("type2", L2Item.TYPE2_SHIELD_ARMOR);
		}
		else
		{
			item.set.set("type1", L2Item.TYPE1_WEAPON_RING_EARRING_NECKLACE);
			item.set.set("type2", L2Item.TYPE2_WEAPON);
		}

		item.set.set("bodypart", _slots.get(rset.getString("bodypart")));
		item.set.set("material", _materials.get(rset.getString("material")));
		item.set.set("crystal_type", _crystalTypes.get(rset.getString("crystal_type")));
		item.set.set("crystallizable", Boolean.valueOf(rset.getString("crystallizable")).booleanValue());
		item.set.set("weight", rset.getInt("weight"));
		item.set.set("soulshots", rset.getInt("soulshots"));
		item.set.set("spiritshots", rset.getInt("spiritshots"));
		item.set.set("p_dam", rset.getInt("p_dam"));
		item.set.set("rnd_dam", rset.getInt("rnd_dam"));
		item.set.set("critical", rset.getInt("critical"));
		item.set.set("hit_modify", rset.getDouble("hit_modify"));
		item.set.set("avoid_modify", rset.getInt("avoid_modify"));
		item.set.set("shield_def", rset.getInt("shield_def"));
		item.set.set("shield_def_rate", rset.getInt("shield_def_rate"));
		item.set.set("atk_speed", rset.getInt("atk_speed"));
		item.set.set("mp_consume", rset.getInt("mp_consume"));
		item.set.set("m_dam", rset.getInt("m_dam"));
		item.set.set("duration", rset.getInt("duration"));
		item.set.set("price", rset.getInt("price"));
		item.set.set("crystal_count", rset.getInt("crystal_count"));
		item.set.set("sellable", Boolean.valueOf(rset.getString("sellable")));
		item.set.set("dropable", Boolean.valueOf(rset.getString("dropable")));
		item.set.set("destroyable", Boolean.valueOf(rset.getString("destroyable")));
		item.set.set("tradeable", Boolean.valueOf(rset.getString("tradeable")));

		item.set.set("item_skill_id", rset.getInt("item_skill_id"));
		item.set.set("item_skill_lvl", rset.getInt("item_skill_lvl"));

		item.set.set("enchant4_skill_id", rset.getInt("enchant4_skill_id"));
		item.set.set("enchant4_skill_lvl", rset.getInt("enchant4_skill_lvl"));

		item.set.set("onCast_skill_id", rset.getInt("onCast_skill_id"));
		item.set.set("onCast_skill_lvl", rset.getInt("onCast_skill_lvl"));
		item.set.set("onCast_skill_chance", rset.getInt("onCast_skill_chance"));

		item.set.set("onCrit_skill_id", rset.getInt("onCrit_skill_id"));
		item.set.set("onCrit_skill_lvl", rset.getInt("onCrit_skill_lvl"));
		item.set.set("onCrit_skill_chance", rset.getInt("onCrit_skill_chance"));

		if(item.type == L2WeaponType.PET)
		{
			item.set.set("type1", L2Item.TYPE1_WEAPON_RING_EARRING_NECKLACE);

			if(item.set.getInteger("bodypart") == L2Item.SLOT_WOLF)
			{
				item.set.set("type2", L2Item.TYPE2_PET_WOLF);
			}
			else if(item.set.getInteger("bodypart") == L2Item.SLOT_HATCHLING)
			{
				item.set.set("type2", L2Item.TYPE2_PET_HATCHLING);
			}
			else if(item.set.getInteger("bodypart") == L2Item.SLOT_BABYPET)
			{
				item.set.set("type2", L2Item.TYPE2_PET_BABY);
			}
			else
			{
				item.set.set("type2", L2Item.TYPE2_PET_STRIDER);
			}

			item.set.set("bodypart", L2Item.SLOT_R_HAND);
		}

		return item;
	}

	private Item readArmor(ResultSet rset) throws SQLException
	{
		Item item = new Item();
		item.set = new StatsSet();
		item.type = _armorTypes.get(rset.getString("armor_type"));
		item.id = rset.getInt("item_id");
		item.name = rset.getString("name");

		item.set.set("item_id", item.id);
		item.set.set("name", item.name);
		int bodypart = _slots.get(rset.getString("bodypart"));
		item.set.set("bodypart", bodypart);
		item.set.set("crystallizable", Boolean.valueOf(rset.getString("crystallizable")));
		item.set.set("crystal_count", rset.getInt("crystal_count"));
		item.set.set("sellable", Boolean.valueOf(rset.getString("sellable")));
		item.set.set("dropable", Boolean.valueOf(rset.getString("dropable")));
		item.set.set("destroyable", Boolean.valueOf(rset.getString("destroyable")));
		item.set.set("tradeable", Boolean.valueOf(rset.getString("tradeable")));
		item.set.set("item_skill_id", rset.getInt("item_skill_id"));
		item.set.set("item_skill_lvl", rset.getInt("item_skill_lvl"));

		if(bodypart == L2Item.SLOT_NECK || bodypart == L2Item.SLOT_HAIR || bodypart == L2Item.SLOT_FACE || bodypart == L2Item.SLOT_DHAIR || (bodypart & L2Item.SLOT_L_EAR) != 0 || (bodypart & L2Item.SLOT_L_FINGER) != 0)
		{
			item.set.set("type1", L2Item.TYPE1_WEAPON_RING_EARRING_NECKLACE);
			item.set.set("type2", L2Item.TYPE2_ACCESSORY);
		}
		else
		{
			item.set.set("type1", L2Item.TYPE1_SHIELD_ARMOR);
			item.set.set("type2", L2Item.TYPE2_SHIELD_ARMOR);
		}

		item.set.set("weight", rset.getInt("weight"));
		item.set.set("material", _materials.get(rset.getString("material")));
		item.set.set("crystal_type", _crystalTypes.get(rset.getString("crystal_type")));
		item.set.set("avoid_modify", rset.getInt("avoid_modify"));
		item.set.set("duration", rset.getInt("duration"));
		item.set.set("p_def", rset.getInt("p_def"));
		item.set.set("m_def", rset.getInt("m_def"));
		item.set.set("mp_bonus", rset.getInt("mp_bonus"));
		item.set.set("price", rset.getInt("price"));

		if(item.type == L2ArmorType.PET)
		{
			item.set.set("type1", L2Item.TYPE1_SHIELD_ARMOR);

			if(item.set.getInteger("bodypart") == L2Item.SLOT_WOLF)
			{
				item.set.set("type2", L2Item.TYPE2_PET_WOLF);
			}
			else if(item.set.getInteger("bodypart") == L2Item.SLOT_HATCHLING)
			{
				item.set.set("type2", L2Item.TYPE2_PET_HATCHLING);
			}
			else if(item.set.getInteger("bodypart") == L2Item.SLOT_BABYPET)
			{
				item.set.set("type2", L2Item.TYPE2_PET_BABY);
			}
			else
			{
				item.set.set("type2", L2Item.TYPE2_PET_STRIDER);
			}

			item.set.set("bodypart", L2Item.SLOT_CHEST);
		}

		return item;
	}

	private Item readItem(ResultSet rset) throws SQLException
	{
		Item item = new Item();
		item.set = new StatsSet();
		item.id = rset.getInt("item_id");

		item.set.set("item_id", item.id);
		item.set.set("crystallizable", Boolean.valueOf(rset.getString("crystallizable")));
		item.set.set("type1", L2Item.TYPE1_ITEM_QUESTITEM_ADENA);
		item.set.set("type2", L2Item.TYPE2_OTHER);
		item.set.set("bodypart", 0);
		item.set.set("crystal_count", rset.getInt("crystal_count"));
		item.set.set("sellable", Boolean.valueOf(rset.getString("sellable")));
		item.set.set("dropable", Boolean.valueOf(rset.getString("dropable")));
		item.set.set("destroyable", Boolean.valueOf(rset.getString("destroyable")));
		item.set.set("tradeable", Boolean.valueOf(rset.getString("tradeable")));
		String itemType = rset.getString("item_type");

		if(itemType.equals("none"))
		{
			item.type = L2EtcItemType.OTHER;
		}
		else if(itemType.equals("castle_guard"))
		{
			item.type = L2EtcItemType.SCROLL;
		}
		else if(itemType.equals("material"))
		{
			item.type = L2EtcItemType.MATERIAL;
		}
		else if(itemType.equals("pet_collar"))
		{
			item.type = L2EtcItemType.PET_COLLAR;
		}
		else if(itemType.equals("potion"))
		{
			item.type = L2EtcItemType.POTION;
		}
		else if(itemType.equals("recipe"))
		{
			item.type = L2EtcItemType.RECEIPE;
		}
		else if(itemType.equals("scroll"))
		{
			item.type = L2EtcItemType.SCROLL;
		}
		else if(itemType.equals("seed"))
		{
			item.type = L2EtcItemType.SEED;
		}
		else if(itemType.equals("shot"))
		{
			item.type = L2EtcItemType.SHOT;
		}
		else if(itemType.equals("spellbook"))
		{
			item.type = L2EtcItemType.SPELLBOOK;
		}
		else if(itemType.equals("herb"))
		{
			item.type = L2EtcItemType.HERB;
		}
		else if(itemType.equals("arrow"))
		{
			item.type = L2EtcItemType.ARROW;
			item.set.set("bodypart", L2Item.SLOT_L_HAND);
		}
		else if(itemType.equals("quest"))
		{
			item.type = L2EtcItemType.QUEST;
			item.set.set("type2", L2Item.TYPE2_QUEST);
		}
		else if(itemType.equals("lure"))
		{
			item.type = L2EtcItemType.OTHER;
			item.set.set("bodypart", L2Item.SLOT_L_HAND);
		}
		else
		{
			_log.trace("unknown etcitem type:" + itemType);
			item.type = L2EtcItemType.OTHER;
		}
		itemType = null;

		String consume = rset.getString("consume_type");
		if(consume.equals("asset"))
		{
			item.type = L2EtcItemType.MONEY;
			item.set.set("stackable", true);
			item.set.set("type2", L2Item.TYPE2_MONEY);
		}
		else if(consume.equals("stackable"))
		{
			item.set.set("stackable", true);
		}
		else
		{
			item.set.set("stackable", false);
		}
		consume = null;

		int material = _materials.get(rset.getString("material"));
		item.set.set("material", material);

		int crystal = _crystalTypes.get(rset.getString("crystal_type"));
		item.set.set("crystal_type", crystal);

		int weight = rset.getInt("weight");
		item.set.set("weight", weight);
		item.name = rset.getString("name");
		item.set.set("name", item.name);

		item.set.set("duration", rset.getInt("duration"));
		item.set.set("price", rset.getInt("price"));

		return item;
	}

	public boolean isInitialized()
	{
		return _initialized;
	}

	private void buildFastLookupTable()
	{
		int highestId = 0;

		for(Integer id : _armors.keySet())
		{
			L2Armor item = _armors.get(id);
			if(item.getItemId() > highestId)
			{
				highestId = item.getItemId();
			}
			id = null;
			item = null;
		}

		for(Integer id : _weapons.keySet())
		{

			L2Weapon item = _weapons.get(id);
			if(item.getItemId() > highestId)
			{
				highestId = item.getItemId();
			}
			id = null;
			item = null;
		}

		for(Integer id : _etcItems.keySet())
		{
			L2EtcItem item = _etcItems.get(id);
			if(item.getItemId() > highestId)
			{
				highestId = item.getItemId();
			}
			id = null;
			item = null;
		}

		_allTemplates = new L2Item[highestId + 1];

		for(Integer id : _armors.keySet())
		{
			L2Armor item = _armors.get(id);
			assert _allTemplates[id.intValue()] == null;
			_allTemplates[id.intValue()] = item;
			id = null;
			item = null;
		}

		for(Integer id : _weapons.keySet())
		{
			L2Weapon item = _weapons.get(id);
			assert _allTemplates[id.intValue()] == null;
			_allTemplates[id.intValue()] = item;
			id = null;
			item = null;
		}

		for(Integer id : _etcItems.keySet())
		{
			L2EtcItem item = _etcItems.get(id);
			assert _allTemplates[id.intValue()] == null;
			_allTemplates[id.intValue()] = item;
			id = null;
			item = null;
		}
	}


	public L2Item getTemplate(int id)
	{
		if(id > _allTemplates.length)
		{
			return null;
		}
		else
		{
			return _allTemplates[id];
		}
	}

	public L2ItemInstance createItem(String process, int itemId, int count, L2PcInstance actor, L2Object reference)
	{
		L2ItemInstance item = new L2ItemInstance(IdFactory.getInstance().getNextId(), itemId);

		if(process.equalsIgnoreCase("loot"))
		{
			ScheduledFuture<?> itemLootShedule;
			long delay = 0;
			if(reference != null && reference instanceof L2GrandBossInstance || reference instanceof L2RaidBossInstance)
			{
				if(((L2Attackable) reference).getFirstCommandChannelAttacked() != null && ((L2Attackable) reference).getFirstCommandChannelAttacked().meetRaidWarCondition(reference))
				{
					item.setOwnerId(((L2Attackable) reference).getFirstCommandChannelAttacked().getChannelLeader().getObjectId());
					delay = 300000;
				}
				else
				{
					delay = 15000;
					item.setOwnerId(actor.getObjectId());
				}
			}
			else
			{
				item.setOwnerId(actor.getObjectId());
				delay = 15000;
			}
			itemLootShedule = ThreadPoolManager.getInstance().scheduleGeneral(new resetOwner(item), delay);
			item.setItemLootShedule(itemLootShedule);
			itemLootShedule = null;
		}

		L2World.storeObject(item);

		if(item.isStackable() && count > 1)
		{
			item.setCount(count);
		}

		if(Config.LOG_ITEMS)
		{
			LogRecord record = new LogRecord(Level.INFO, "CREATE:" + process);
			record.setLoggerName("item");
			record.setParameters(new Object[]
			{
					item, actor, reference
			});
			_logItems.log(record);
			record = null;
		}

		return item;
	}

	public L2ItemInstance createItem(String process, int itemId, int count, L2PcInstance actor)
	{
		return createItem(process, itemId, count, actor, null);
	}

	public L2ItemInstance createDummyItem(int itemId)
	{
		L2Item item = getTemplate(itemId);

		if(item == null)
		{
			return null;
		}

		L2ItemInstance temp = new L2ItemInstance(0, item);
		item = null;

		try
		{
			temp = new L2ItemInstance(0, itemId);
		}
		catch(ArrayIndexOutOfBoundsException e)
		{
		}

		if(temp.getItem() == null)
		{
			_log.warn("ItemTable: Item Template missing for Id: " + itemId);
		}

		return temp;
	}

	public void destroyItem(String process, L2ItemInstance item, L2PcInstance actor, L2Object reference)
	{
		synchronized (item)
		{
			item.setCount(0);
			item.setOwnerId(0);
			item.setLocation(ItemLocation.VOID);
			item.setLastChange(L2ItemInstance.REMOVED);

			L2World.getInstance().removeObject(item);
			IdFactory.getInstance().releaseId(item.getObjectId());

			if(Config.LOG_ITEMS)
			{
				LogRecord record = new LogRecord(Level.INFO, "DELETE:" + process);
				record.setLoggerName("item");
				record.setParameters(new Object[]
				{
						item, actor, reference
				});
				_logItems.log(record);
			}

			if(L2PetDataTable.isPetItem(item.getItemId()))
			{
				Connection con = null;
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id = ?");
					statement.setInt(1, item.getObjectId());
					statement.execute();
					statement.close();
					statement = null;
				}
				catch(Exception e)
				{
					_log.error("could not delete pet objectid:", e);
				}
				finally
				{
					ResourceUtil.closeConnection(con); 
				}
			}
		}
	}

	public void reload()
	{
		synchronized (_instance)
		{
			_instance = null;
			_instance = new ItemTable();
		}
	}

	protected class resetOwner implements Runnable
	{
		L2ItemInstance _item;

		public resetOwner(L2ItemInstance item)
		{
			_item = item;
		}

		@Override
		public void run()
		{
			_item.setOwnerId(0);
			_item.setItemLootShedule(null);
		}
	}
}