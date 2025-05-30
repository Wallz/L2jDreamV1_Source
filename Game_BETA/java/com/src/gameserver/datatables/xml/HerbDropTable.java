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
package com.src.gameserver.datatables.xml;


import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.util.logging.Logger;

import javolution.util.FastList;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.src.Config;
import com.src.gameserver.datatables.sql.ItemTable;
import com.src.gameserver.model.L2DropCategory;
import com.src.gameserver.model.L2DropData;
import com.src.util.xmlfactory.XMLDocumentFactory;

public class HerbDropTable
{
	private static Logger _log = Logger.getLogger(HerbDropTable.class.getName());
	
	private TIntObjectHashMap<FastList<L2DropCategory>> _herbGroups;
	
	public static HerbDropTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private HerbDropTable()
	{
		_herbGroups = new TIntObjectHashMap<FastList<L2DropCategory>>();
		restoreData();
	}
	
	private void restoreData()
	{
		try
		{
        	File file = new File(Config.DATAPACK_ROOT + "/data/xml/herbs_droplist.xml");
        	Document doc = XMLDocumentFactory.getInstance().loadDocument(file);

        	Node n = doc.getFirstChild();
        	for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
            {
                if ("group".equalsIgnoreCase(d.getNodeName()))
                {
            		NamedNodeMap attrs = d.getAttributes();
                	int groupId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
                	
    				FastList<L2DropCategory> category;
    				if (_herbGroups.contains(groupId))
    					category = _herbGroups.get(groupId);
    				else
    				{
    					category = new FastList<L2DropCategory>();
    					_herbGroups.put(groupId, category);
    				}
    				
                	L2DropData dropDat = null;
                	int id, chance, categoryType = 0;
                	
                	for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
                    {
                		dropDat = new L2DropData();
                        if ("item".equalsIgnoreCase(cd.getNodeName()))
                        {
                        	attrs = cd.getAttributes();
        					id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
        					categoryType = Integer.parseInt(attrs.getNamedItem("category").getNodeValue());
        					chance = Integer.parseInt(attrs.getNamedItem("chance").getNodeValue());
        					
        					dropDat.setItemId(id);
        					dropDat.setMinDrop(1);
        					dropDat.setMaxDrop(1);
        					dropDat.setChance(chance);
        					
            				if (ItemTable.getInstance().getTemplate(dropDat.getItemId()) == null)
            				{
            					_log.warning("HerbDropTable: Herb data for undefined item template! GroupId: " + groupId+", itemId: " + dropDat.getItemId());
            					continue;
            				}
            				
            				boolean catExists = false;
            				for (L2DropCategory cat : category)
            				{
            					// if the category exists, add the drop to this category.
            					if (cat.getCategoryType() == categoryType)
            					{
            						cat.addDropData(dropDat, false);
            						catExists = true;
            						break;
            					}
            				}
            							
            				// if the category doesn't exit, create it and add the drop
            				if (!catExists)
            				{
            					L2DropCategory cat = new L2DropCategory(categoryType);
            					cat.addDropData(dropDat, false);
            					category.add(cat);
            				}
                        }
                    }
                }
            }
		}
		catch (Exception e)
		{
			_log.warning("HerbDropTable: Error while creating table: " + e);
		}
		_log.info("HerbDropTable: Loaded " + _herbGroups.size() + " herbs groups.");
	}
	
	public FastList<L2DropCategory> getHerbDroplist(int groupId)
	{
		return _herbGroups.get(groupId);
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final HerbDropTable _instance = new HerbDropTable();
	}
}