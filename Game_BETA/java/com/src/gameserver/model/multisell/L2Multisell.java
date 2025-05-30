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
package com.src.gameserver.model.multisell;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import javolution.util.FastList;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.src.Config;
import com.src.gameserver.datatables.sql.ItemTable;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.MultiSellList;
import com.src.gameserver.templates.item.L2Armor;
import com.src.gameserver.templates.item.L2Item;
import com.src.gameserver.templates.item.L2Weapon;

public class L2Multisell
{
	private static Logger _log = Logger.getLogger(L2Multisell.class.getName());
	private List<MultiSellListContainer> _entries = new FastList<MultiSellListContainer>();
	private static L2Multisell _instance = new L2Multisell();

	public MultiSellListContainer getList(int id)
	{
		synchronized (_entries)
		{
			for(MultiSellListContainer list : _entries)
			{
				if(list.getListId() == id)
				{
					return list;
				}
			}
		}

		_log.warning("[L2Multisell] can't find list with id: " + id);
		return null;
	}

	public L2Multisell()
	{
		parseData();
	}

	public void reload()
	{
		parseData();
	}

	public static L2Multisell getInstance()
	{
		return _instance;
	}

	private void parseData()
	{
		_entries.clear();
		parse();
	}

	private MultiSellListContainer generateMultiSell(int listId, boolean inventoryOnly, L2PcInstance player, int npcId, double taxRate)
	{
		MultiSellListContainer listTemplate = L2Multisell.getInstance().getList(listId);
		MultiSellListContainer list = new MultiSellListContainer();

		if(listTemplate == null)
		{
			return list;
		}

		list = new MultiSellListContainer();
		list.setListId(listId);

		if(npcId != 0 && !listTemplate.checkNpcId(npcId))
		{
			listTemplate.addNpcId(npcId);
		}

		if(inventoryOnly)
		{
			if(player == null)
			{
				return list;
			}

			L2ItemInstance[] items;

			if(listTemplate.getMaintainEnchantment())
			{
				items = player.getInventory().getUniqueItemsByEnchantLevel(false, false, false);
			}
			else
			{
				items = player.getInventory().getUniqueItems(false, false, false);
			}

			int enchantLevel;
			for(L2ItemInstance item : items)
			{
				if(!item.isWear() && (item.getItem() instanceof L2Armor || item.getItem() instanceof L2Weapon))
				{
					enchantLevel = listTemplate.getMaintainEnchantment() ? item.getEnchantLevel() : 0;
					for(MultiSellEntry ent : listTemplate.getEntries())
					{
						boolean doInclude = false;

						for(MultiSellIngredient ing : ent.getIngredients())
						{
							if(item.getItemId() == ing.getItemId())
							{
								doInclude = true;
								break;
							}
						}

						if(doInclude)
						{
							list.addEntry(prepareEntry(ent, listTemplate.getApplyTaxes(), listTemplate.getMaintainEnchantment(), enchantLevel, taxRate));
						}
					}
				}
			}
		}
		else
		{
			for(MultiSellEntry ent : listTemplate.getEntries())
			{
				list.addEntry(prepareEntry(ent, listTemplate.getApplyTaxes(), false, 0, taxRate));
			}
		}
		list.setIsCommunity(listTemplate.getIsCommunity());
		return list;
	}

	private MultiSellEntry prepareEntry(MultiSellEntry templateEntry, boolean applyTaxes, boolean maintainEnchantment, int enchantLevel, double taxRate)
	{
		MultiSellEntry newEntry = new MultiSellEntry();
		newEntry.setEntryId(templateEntry.getEntryId() * 100000 + enchantLevel);

		int adenaAmount = 0;

		for(MultiSellIngredient ing : templateEntry.getIngredients())
		{
			MultiSellIngredient newIngredient = new MultiSellIngredient(ing);

			if(ing.getItemId() == 57 && ing.isTaxIngredient())
			{
				if(applyTaxes)
				{
					adenaAmount += (int) Math.round(ing.getItemCount() * taxRate);
				}
				continue;
			}
			else if(ing.getItemId() == 57)
			{
				adenaAmount += ing.getItemCount();
				continue;
			}
			else if(maintainEnchantment)
			{
				L2Item tempItem = ItemTable.getInstance().createDummyItem(ing.getItemId()).getItem();
				if(tempItem instanceof L2Armor || tempItem instanceof L2Weapon)
				{
					newIngredient.setEnchantmentLevel(enchantLevel);
				}

				tempItem = null;
			}

			newEntry.addIngredient(newIngredient);
			newIngredient = null;
		}

		if(adenaAmount > 0)
		{
			newEntry.addIngredient(new MultiSellIngredient(57, adenaAmount, 0, false, false));
		}

		for(MultiSellIngredient ing : templateEntry.getProducts())
		{
			MultiSellIngredient newIngredient = new MultiSellIngredient(ing);

			if(maintainEnchantment)
			{
				L2Item tempItem = ItemTable.getInstance().createDummyItem(ing.getItemId()).getItem();

				if(tempItem instanceof L2Armor || tempItem instanceof L2Weapon)
				{
					newIngredient.setEnchantmentLevel(enchantLevel);
				}
			}
			newEntry.addProduct(newIngredient);
		}

		return newEntry;
	}

	public void SeparateAndSend(int listId, L2PcInstance player, int npcId, boolean inventoryOnly, double taxRate, boolean isCommunity)
	{
		MultiSellListContainer list = generateMultiSell(listId, inventoryOnly, player, npcId, taxRate);
		MultiSellListContainer temp = new MultiSellListContainer();
        if (isCommunity && !list.getIsCommunity())
        {
            _log.warning("player "+player.getName()+" try to open not BBS multisell with bypass _bbsmultisell. Ban him!");
            return;
        }
		int page = 1;

		temp.setListId(list.getListId());

		for(MultiSellEntry e : list.getEntries())
		{
			if(temp.getEntries().size() == 40)
			{
				player.sendPacket(new MultiSellList(temp, page++, 0));
				temp = new MultiSellListContainer();
				temp.setListId(list.getListId());
			}

			temp.addEntry(e);
		}

		player.sendPacket(new MultiSellList(temp, page, 1));
	}

	private void hashFiles(String dirname, List<File> hash)
	{
		File dir = new File(Config.DATAPACK_ROOT, "data/" + dirname);

		if(!dir.exists())
		{
			_log.config("Dir " + dir.getAbsolutePath() + " not exists");
			return;
		}

		File[] files = dir.listFiles();

		for(File f : files)
		{
			if(f.getName().endsWith(".xml"))
			{
				hash.add(f);
			}
		}
	}

	private void parse()
	{
		Document doc = null;

		int id = 0;

		List<File> files = new FastList<File>();
		hashFiles("multisell", files);

		for(File f : files)
		{
			id = Integer.parseInt(f.getName().replaceAll(".xml", ""));
			try
			{

				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setValidating(false);
				factory.setIgnoringComments(true);
				doc = factory.newDocumentBuilder().parse(f);
				factory = null;
			}
			catch(Exception e)
			{
				_log.log(Level.SEVERE, "Error loading file " + f, e);
			}
			try
			{
				MultiSellListContainer list = parseDocument(doc);
				list.setListId(id);
				_entries.add(list);
				list = null;
			}
			catch(Exception e)
			{
				_log.log(Level.SEVERE, "Error in file " + f, e);
			}
		}
	}

	protected MultiSellListContainer parseDocument(Document doc)
	{
		MultiSellListContainer list = new MultiSellListContainer();

		for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if("list".equalsIgnoreCase(n.getNodeName()))
			{
				Node attribute;
				attribute = n.getAttributes().getNamedItem("applyTaxes");

				if(attribute == null)
				{
					list.setApplyTaxes(false);
				}
				else
				{
					list.setApplyTaxes(Boolean.parseBoolean(attribute.getNodeValue()));
				}
				
                attribute = n.getAttributes().getNamedItem("isCommunity");
                
                if (attribute == null)
                    list.setIsCommunity(false);
                else
                    list.setIsCommunity(Boolean.parseBoolean(attribute.getNodeValue()));
                
				attribute = n.getAttributes().getNamedItem("maintainEnchantment");

				if(attribute == null)
				{
					list.setMaintainEnchantment(false);
				}
				else
				{
					list.setMaintainEnchantment(Boolean.parseBoolean(attribute.getNodeValue()));
				}

				for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if("item".equalsIgnoreCase(d.getNodeName()))
					{
						MultiSellEntry e = parseEntry(d);
						list.addEntry(e);
					}
				}
			}
			else if("item".equalsIgnoreCase(n.getNodeName()))
			{
				MultiSellEntry e = parseEntry(n);
				list.addEntry(e);
			}
		}

		return list;
	}

	protected MultiSellEntry parseEntry(Node n)
	{
		int entryId = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());

		Node first = n.getFirstChild();
		MultiSellEntry entry = new MultiSellEntry();

		for(n = first; n != null; n = n.getNextSibling())
		{
			if("ingredient".equalsIgnoreCase(n.getNodeName()))
			{
				Node attribute;

				int id = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
				int count = Integer.parseInt(n.getAttributes().getNamedItem("count").getNodeValue());
				boolean isTaxIngredient = false, mantainIngredient = false;

				attribute = n.getAttributes().getNamedItem("isTaxIngredient");

				if(attribute != null)
				{
					isTaxIngredient = Boolean.parseBoolean(attribute.getNodeValue());
				}

				attribute = n.getAttributes().getNamedItem("mantainIngredient");

				if(attribute != null)
				{
					mantainIngredient = Boolean.parseBoolean(attribute.getNodeValue());
				}

				MultiSellIngredient e = new MultiSellIngredient(id, count, isTaxIngredient, mantainIngredient);
				entry.addIngredient(e);
			}
			else if("production".equalsIgnoreCase(n.getNodeName()))
			{
				int id = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
				int count = Integer.parseInt(n.getAttributes().getNamedItem("count").getNodeValue());
				int enchant = 0;

				if(n.getAttributes().getNamedItem("enchant") != null)
				{
					enchant = Integer.parseInt(n.getAttributes().getNamedItem("enchant").getNodeValue());
				}
				MultiSellIngredient e = new MultiSellIngredient(id, count, enchant, false, false);
				entry.addProduct(e);
			}
		}
		entry.setEntryId(entryId);
		return entry;
	}
}