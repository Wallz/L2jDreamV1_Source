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
package com.src.gameserver.datatables.xml;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.src.Config;
import com.src.gameserver.RecipeController;
import com.src.gameserver.model.L2RecipeList;
import com.src.gameserver.model.actor.instance.L2RecipeInstance;

public class RecipeTable extends RecipeController
{
	private static final Log _log = LogFactory.getLog(RecipeTable.class.getName());

	private Map<Integer, L2RecipeList> _lists;

	private static RecipeTable instance;

	public static RecipeTable getInstance()
	{
		if(instance == null)
		{
			instance = new RecipeTable();
		}

		return instance;
	}

	private RecipeTable() 
	{
		_lists = new FastMap<Integer, L2RecipeList>();

		try
		{
			this.ParseXML();
			_log.info("RecipeTable: Loaded " + _lists.size() + " recipes.");
		}
		catch (Exception e)
		{
			_log.error("RecipeTable: Failed loading recipe list", e);
		}
	}

    private void ParseXML() throws SAXException, IOException, ParserConfigurationException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setIgnoringComments(true);
        File file = new File(Config.DATAPACK_ROOT + "/data/xml/recipes.xml");
        if (file.exists())
        {
            Document doc = factory.newDocumentBuilder().parse(file);
            List<L2RecipeInstance> recipePartList = new FastList<L2RecipeInstance>();
            
            for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
            {
                if ("list".equalsIgnoreCase(n.getNodeName()))
                {
                    String recipeName;
                    int id;
                    for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
                    {
                        if ("item".equalsIgnoreCase(d.getNodeName()))
                        {
                            recipePartList.clear();
                            NamedNodeMap attrs = d.getAttributes();
                            Node att;
                            att = attrs.getNamedItem("id");
                            if (att == null)
                            {
                                _log.error("RecipeTable: Missing id for recipe item, skipping");
                                continue;
                            }
                            id = Integer.parseInt(att.getNodeValue());
                            
                            att = attrs.getNamedItem("name");
                            if (att == null)
                            {
                                _log.error("RecipeTable: Missing name for recipe item id: "+id+", skipping");
                                continue;
                            }
                            recipeName = att.getNodeValue();
                            
                            int recipeId = -1;
                            int level = -1;
                            boolean isDwarvenRecipe = true;
                            int mpCost = -1;
                            int successRate = -1;
                            int prodId = -1;
                            int count = -1;
                            for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
                            {
                                if ("recipe".equalsIgnoreCase(c.getNodeName()))
                                {
                                    NamedNodeMap atts = c.getAttributes();
                                    
                                    recipeId = Integer.parseInt(atts.getNamedItem("id").getNodeValue());
                                    level = Integer.parseInt(atts.getNamedItem("level").getNodeValue());
                                    isDwarvenRecipe = atts.getNamedItem("type").getNodeValue().equalsIgnoreCase("dwarven");
                                }
                                else if ("mpCost".equalsIgnoreCase(c.getNodeName()))
                                {
                                    mpCost = Integer.parseInt(c.getTextContent());
                                }
                                else if ("successRate".equalsIgnoreCase(c.getNodeName()))
                                {
                                    successRate = Integer.parseInt(c.getTextContent());
                                }
                                else if ("ingredient".equalsIgnoreCase(c.getNodeName()))
                                {
                                    int ingId = Integer.parseInt(c.getAttributes().getNamedItem("id").getNodeValue());
                                    int ingCount = Integer.parseInt(c.getAttributes().getNamedItem("count").getNodeValue());
                                    recipePartList.add(new L2RecipeInstance(ingId, ingCount));
                                }
                                else if ("production".equalsIgnoreCase(c.getNodeName()))
                                {
                                    prodId = Integer.parseInt(c.getAttributes().getNamedItem("id").getNodeValue());
                                    count = Integer.parseInt(c.getAttributes().getNamedItem("count").getNodeValue());
                                }
                            }
                            L2RecipeList recipeList = new L2RecipeList(id, level, recipeId, recipeName, successRate, mpCost, prodId, count, isDwarvenRecipe);
                            for (L2RecipeInstance recipePart : recipePartList)
                                recipeList.addRecipe(recipePart);
                            
                            _lists.put(_lists.size(), recipeList);
                        }
                    }
                }
            }
        }
        else
            _log.error("RecipeTable: recipes.xml is missing in data folder.");
    }

	public int getRecipesCount()
	{
		return _lists.size();
	}

	public L2RecipeList getRecipeList(int listId)
	{
		return _lists.get(listId);
	}

	@Override
	public L2RecipeList getRecipeByItemId(int itemId)
	{
		for(int i = 0; i < _lists.size(); i++)
		{
			L2RecipeList find = _lists.get(new Integer(i));
			if(find.getRecipeId() == itemId)
			{
				return find;
			}
		}
		return null;
	}

	public L2RecipeList getRecipeById(int recId)
	{
		for(int i = 0; i < _lists.size(); i++)
		{
			L2RecipeList find = _lists.get(new Integer(i));
			if(find.getId() == recId)
			{
				return find;
			}
		}
		return null;
	}

}