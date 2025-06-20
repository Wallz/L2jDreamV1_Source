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
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.datatables.xml.HennaTable;
import com.src.gameserver.model.actor.instance.L2HennaInstance;
import com.src.gameserver.model.base.ClassId;
import com.src.gameserver.templates.item.L2Henna;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class HennaTreeTable
{
	private static final Log _log = LogFactory.getLog(HennaTreeTable.class.getName());

	private static final HennaTreeTable _instance = new HennaTreeTable();
	private Map<ClassId, List<L2HennaInstance>> _hennaTrees;
	private boolean _initialized = true;

	public static HennaTreeTable getInstance()
	{
		return _instance;
	}

	private HennaTreeTable()
	{
		_hennaTrees = new FastMap<ClassId, List<L2HennaInstance>>();
		int classId = 0;
		int count = 0;

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT class_name, id, parent_id FROM class_list ORDER BY id");
			ResultSet classlist = statement.executeQuery();
			List<L2HennaInstance> list;

			while(classlist.next())
			{
				list = new FastList<L2HennaInstance>();
				classId = classlist.getInt("id");
				PreparedStatement statement2 = con.prepareStatement("SELECT class_id, symbol_id FROM henna_trees WHERE class_id = ? ORDER BY symbol_id");
				statement2.setInt(1, classId);
				ResultSet hennatree = statement2.executeQuery();

				while(hennatree.next())
				{
					int id = hennatree.getInt("symbol_id");
					L2Henna template = HennaTable.getInstance().getTemplate(id);

					if(template == null)
					{
						hennatree.close();
						statement2.close();
						classlist.close();
						statement.close();
						return;
					}

					L2HennaInstance temp = new L2HennaInstance(template);
					temp.setSymbolId(id);
					temp.setItemIdDye(template.getDyeId());
					temp.setAmountDyeRequire(template.getAmountDyeRequire());
					temp.setPrice(template.getPrice());
					temp.setStatINT(template.getStatINT());
					temp.setStatSTR(template.getStatSTR());
					temp.setStatCON(template.getStatCON());
					temp.setStatMEM(template.getStatMEM());
					temp.setStatDEX(template.getStatDEX());
					temp.setStatWIT(template.getStatWIT());

					list.add(temp);

					temp = null;
					template = null;
				}
				_hennaTrees.put(ClassId.values()[classId], list);

				hennatree.close();
				hennatree = null;
				statement2.close();
				statement2 = null;

				count += list.size();
				
				// This will be usefull for debug, put for your own risk
				// _log.info("Henna Tree for Class: " + classId + " has " + list.size() + " Henna Templates.");
			}

			list = null;
			classlist.close();
			classlist = null;
			statement.close();
			statement = null;

		}
		catch(Exception e)
		{
			_log.warn("error while creating henna tree for classId " + classId + "  " + e);
			e.printStackTrace();
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}

		_log.info("HennaTreeTable: Loaded " + count + " henna tree templates.");

	}

	public L2HennaInstance[] getAvailableHenna(ClassId classId)
	{
		List<L2HennaInstance> result = new FastList<L2HennaInstance>();
		List<L2HennaInstance> henna = _hennaTrees.get(classId);
		if(henna == null)
		{
			_log.warn("Hennatree for class " + classId + " is not defined !");

			return new L2HennaInstance[0];
		}

		for(L2HennaInstance temp : henna)
		{
			result.add(temp);
		}

		return result.toArray(new L2HennaInstance[result.size()]);
	}

	public boolean isInitialized()
	{
		return _initialized;
	}

}